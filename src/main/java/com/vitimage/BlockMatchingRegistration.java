package com.vitimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.File;
import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.ResampleImageFilter;

import com.sun.tools.javac.code.Attribute.Array;
import com.vitimage.TransformUtils.AngleComparator;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.LUT;
import math3d.Point3d;

public class BlockMatchingRegistration  implements ItkImagePlusInterface{
	boolean bmIsInterruptedSucceeded=false;
	public Thread[] threads;
	public Thread mainThread;
	boolean timingMeasurement=true;
	boolean debug=false;
	double[]refRange=new double[] {-1,-1};
	double[]movRange=new double[] {-1,-1};
	boolean flagRange=false;
	public boolean bmIsInterrupted;
	public boolean computeSummary=false;
	int maxIter=1000;
	int fontSize=12;
	int percentageBlocksSelectedByLTS=70;
	public Point3d[][] correspondanceProvidedAtStart;
	public double[]globalR2Values=new double[maxIter];
	public double[]blockR2ValuesBef=new double[maxIter];
	public double[]blockR2ValuesAft=new double[maxIter];
	public int incrIter=0;
	public int strideMoving=2;
	public int displayRegistration=1;
	public boolean displayR2=true;
	public boolean consoleOutputActivated=true;
	public boolean imageJOutputActivated=true;
	public int defaultCoreNumber=12;
	public ResampleImageFilter resampler;
	public ImagePlus imgRef;
	public ImagePlus imgMov;
	public double denseFieldSigma;//in mm
	public double smoothingSigmaInPixels;
	public int levelMin;
	public int levelMax;
	public boolean noSubScaleZ=true;
	public int percentageBlocksSelectedByVariance=50;//15;
	public double minBlockVariance=0.05;
	public double minBlockScore=0.1;
	public int percentageBlocksSelectedRandomly=100;//50;
	public int percentageBlocksSelectedByScore=80;//95;
	public int blockSizeHalfX;
	public int blockSizeHalfY;
	public int blockSizeHalfZ;
	public int blockSizeX;
	public int blockSizeY;
	public int blockSizeZ;
	public int blocksStrideX=2;
	public int blocksStrideY=2;
	public int blocksStrideZ=2;
	public int neighbourhoodSizeX=4;
	public int neighbourhoodSizeY=4;
	public int neighbourhoodSizeZ=1;
	public double imgMovDefaultValue;
	public double imgRefDefaultValue;
	public int nbLevels;
	public int nbIterations;
	Image []currentField;
	int indField;
	public int []subScaleFactors;
	public double []successiveStepFactors;
	public int [][]successiveDimensions;
	public double [][]successiveVoxSizes;
	public double []successiveSmoothingSigma;
	public double []successiveDenseFieldSigma;
	public double rejectionThreshold=3;//for outlier rejection : when computing Sigma_X/Y/Z,Mu_X/Y/Z of the transformation coordinates (X,Y,Z) in the neighbourhood (x0+-sigma,y0+-sigma,z0+-sigma),
	//reject the vectors which one at least of their coordinate X (respectively Y,Z) is out of the interval Mu_X (respectively Y,Z)  +- rejectionThreshold*Sigma_X (respectively Y,Z) 
	public ItkTransform currentTransform=new ItkTransform();
	public Transform3DType transformationType;
	public MetricType metricType;
	public ImagePlus sliceRef;
	public ImagePlus sliceMov;
	public ImagePlus sliceFuse;
	public ImagePlus sliceGrid;
	public ImagePlus sliceCorr;
	public ImagePlus sliceJacobian;
	public ImagePlus summary;
	public ArrayList<ImagePlus> summaryArray=new ArrayList<ImagePlus>();
	public ImagePlus gridSummary;
	public ArrayList<ImagePlus> gridSummaryArray=new ArrayList<ImagePlus>();
	public ImagePlus correspondancesSummary;
	public ArrayList<ImagePlus> correspondancesSummaryArray=new ArrayList<ImagePlus>();
	public ImagePlus jacobianSummary;
	public ArrayList<ImagePlus> jacobianSummaryArray=new ArrayList<ImagePlus>();
	public int sliceInt;
	public int sliceIntCorr;
	public double zoomFactor=1;
	public int viewWidth;
	public int viewHeight;
	public double lastValueCorr=0;
	public double lastValueBlocksCorr=0;
	public ImagePlus mask=null;
	private boolean flagSingleView=false;
	private String info="";
	public static void callDebugDef() {
		System.out.println("Debug production method. Start...");
		//Lecture image initiale et champ et construction grille
		ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Test/TestBM/imgMov_Ttest.tif");
		ItkTransform trans=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Test/TestBM/testField.tif");
		ImagePlus grid=VitimageUtils.getBinaryGrid(imgMov, 10);

		
		//Deformation image et grille
		ImagePlus imgTrans=trans.transformImage(imgMov,imgMov,false);
		ImagePlus gridTrans=trans.transformImage(imgMov,grid,false);

		
		//Affichage du tout
		imgMov.show();
		imgMov.setTitle("TESTBefore");
		imgTrans.show();
		imgTrans.setTitle("TESTAfter");
		grid.show();
		grid.setTitle("TESTGrid Before");
		gridTrans.show();	
		gridTrans.setTitle("TESTGrid After");
		VitimageUtils.waitFor(100000);
	}
	
	/** Main for testing, and constructor
	 * 
	 */
	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		int viewSlice=19;
		///////////TEST 1/////////////
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/imgRef_Ttest.tif");
		ImagePlus imgMov=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/imgMov_Ttest.tif");
		ImagePlus imgMask=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/mask_little.tif");

		///CALCUL DU BLOCKMATCHING
		
		//COMPUTE THE REGISTRATION, RIGID-BODY, then DENSE
		System.out.println("Running registration. ");
		
		ItkTransform tr1=BlockMatchingRegistration.testSetupAndRunStandardBlockMatchingWithoutFineParameterization(
												VitimageUtils.removeCapillaryFromHyperImageForRegistration(imgRef),
												VitimageUtils.removeCapillaryFromHyperImageForRegistration(imgMov),
												imgMask,true,true,true,true,true,true);
		
		

		///SAUVEGARDE DE LA TRANSFORMATION RESULTAT
		ItkTransform testField=tr1.flattenDenseField(imgRef);
		testField.writeAsDenseField("/home/fernandr/Bureau/Test/BM_subvoxel/testField.tif",imgRef);

		///CALCUL DE LA NORME		
		ImagePlus normField=testField.normOfDenseField(imgRef);
		normField.setTitle("Distance_map");
		normField.setDisplayRange(0,0.1);
		IJ.run(normField,"Fire","");
		normField.show();
		ImagePlus testNorm=testField.transformImage(imgRef, imgRef,false);	
		testNorm.setTitle("Validation_after");
		testNorm.show();
		IJ.run(normField, "Merge Channels...", "c1=Distance_map c2=Validation_after create");
		normField=IJ.getImage();
		normField.setTitle("Distance map in final transform");
		normField.setSlice(viewSlice);
		
		
		//CALCUL DE L IMAGE AVANT/APRES
		ImagePlus imgInit=new ItkTransform().transformImage(imgRef,imgMov,false);
		ImagePlus imgResampled=testField.transformImage(imgRef,imgMov,false);
		imgResampled.getProcessor().setMinAndMax(imgMov.getProcessor().getMin(), imgMov.getProcessor().getMax());
		imgInit.getProcessor().setMinAndMax(imgMov.getProcessor().getMin(), imgMov.getProcessor().getMax());
		ImagePlus viewAfter=VitimageUtils.compositeNoAdjustOf(imgRef, imgResampled,"After");
		ImagePlus viewBefore=VitimageUtils.compositeNoAdjustOf(imgRef, imgInit,"Before");
		viewBefore.show();
		viewBefore.setSlice(viewSlice);
		viewAfter.show();
		viewAfter.setSlice(viewSlice);
		
	
	}

	public BlockMatchingRegistration(ImagePlus imgRef,ImagePlus imgMov,Transform3DType transformationType,MetricType metricType,
			double smoothingSigmaInPixels, double denseFieldSigma,int levelMin,int levelMax,int nbIterations,int sliceInt,ImagePlus mask,
			int neighbourXY,int neighbourZ,int blockHalfSizeXY,int blockHalfSizeZ,int strideXY,int strideZ) {
		if(imgRef.getWidth()<imgRef.getStackSize()*4)noSubScaleZ=false;
		this.resampler=new ResampleImageFilter();
		this.imgRef=VitimageUtils.imageCopy(imgRef);
		this.imgMov=VitimageUtils.imageCopy(imgMov);
		if(this.imgRef.getType() != 32)IJ.run(imgRef,"32-bit","");
		if(this.imgMov.getType() != 32)IJ.run(imgMov,"32-bit","");
		this.metricType=metricType;
		this.transformationType=transformationType;
		this.levelMin=levelMin;
		this.levelMax=levelMax;
		this.nbLevels=levelMax-levelMin+1;
		this.denseFieldSigma=denseFieldSigma;
		this.nbIterations=nbIterations;
		this.smoothingSigmaInPixels=smoothingSigmaInPixels;
		this.subScaleFactors=new int[nbLevels];
		this.subScaleFactors=subSamplingFactorsAtSuccessiveLevels(levelMin,levelMax,false,2);
		this.successiveStepFactors=successiveStepFactorsAtSuccessiveLevels(levelMin,levelMax,false,2);
		this.successiveDimensions=imageDimsAtSuccessiveLevels(new int[] {imgRef.getWidth(),imgRef.getHeight(),imgRef.getStackSize()},subScaleFactors,this.noSubScaleZ);
		this.successiveVoxSizes=imageVoxSizesAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),subScaleFactors);
		this.successiveSmoothingSigma=sigmaFactorsAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),this.subScaleFactors,smoothingSigmaInPixels); 
		this.successiveDenseFieldSigma=sigmaDenseFieldAtSuccessiveLevels(this.subScaleFactors,denseFieldSigma); 
		this.imgMovDefaultValue=(Acquisition.caracterizeBackgroundOfImage(imgMov))[0];
		this.imgRefDefaultValue=(Acquisition.caracterizeBackgroundOfImage(imgRef))[0];
		int[]dims=VitimageUtils.getDimensions(imgRef);
		this.fontSize=Math.min(dims[0]/40, dims[1]/40);
		if(this.fontSize<5)this.fontSize=0;
		this.minBlockVariance=VitimageUtils.statistics1D(VitimageUtils.valuesOfBlock(imgRef, 0, 0, dims[2]/2, dims[0]-1, dims[1]-1, dims[2]/2))[0]/10.0;
		this.sliceInt=sliceInt;
		this.sliceIntCorr=sliceInt;
		this.mask=mask;
		this.noSubScaleZ=noSubScaleZ;
		this.blockSizeHalfX=blockHalfSizeXY;
		this.blockSizeHalfY=blockHalfSizeXY;
		this.blockSizeHalfZ=blockHalfSizeZ;
		this.blockSizeX=2*this.blockSizeHalfX+1;
		this.blockSizeY=2*this.blockSizeHalfY+1;
		this.blockSizeZ=2*this.blockSizeHalfZ+1;
		indField=0;
		currentField=new Image[nbLevels*nbIterations];
		this.neighbourhoodSizeX=neighbourXY;
		this.neighbourhoodSizeY=neighbourXY;
		this.neighbourhoodSizeZ=neighbourZ;
		this.blocksStrideX=strideXY;
		this.blocksStrideY=strideXY;
		this.blocksStrideZ=strideZ;
		if(dims[0]<512)this.zoomFactor=(int)Math.round(800/dims[0]);
		this.viewHeight=(int)(this.imgRef.getHeight()*zoomFactor);
		this.viewWidth=(int)(this.imgRef.getWidth()*zoomFactor);
		System.out.println("Min block variance="+this.minBlockVariance);
		System.out.println("Min block score="+this.minBlockScore);

	}

	
	
	
	
	/** Helper to estimate computation time, and eventually adjust parameters before launch
	 * 
	 */
	public static double []estimateBlocksNumberPerLevel(int[]dims,int viewRegistrationLevel,int levelMin,int levelMax,int nbIter,Transform3DType transformType,													
		int []blockHalfSizes,int []strides,int []neighbourhoods,int percentageScoreSelect,int percentageVarianceSelect,int percentageRandomSelect,boolean subsampleZ) {
		double[]tabNb= new double[levelMax-levelMin+1];
		int factorDiv=1;
		for(int lev=levelMax;lev>=levelMin;lev--) {
			if(lev>1)factorDiv=(int)Math.round(Math.pow(2, lev-1));
			System.out.println("factorDiv="+factorDiv);
			int nbBlocksX=(dims[0]>1) ? 1+(dims[0]/factorDiv-blockHalfSizes[0]-2*neighbourhoods[0]*strides[0])/strides[0] : 1;
			int nbBlocksY=(dims[1]>1) ? 1+(dims[1]/factorDiv-blockHalfSizes[1]-2*neighbourhoods[1]*strides[1])/strides[1] : 1;
			int nbBlocksZ=(dims[2]>1) ? 1+(dims[2]/(subsampleZ ? factorDiv : 1) -blockHalfSizes[2]-2*neighbourhoods[2]*strides[2])/strides[2] : 1;
			int nbBlocksTotal=nbBlocksX*nbBlocksY*nbBlocksZ;			
			tabNb[levelMax-lev]=nbBlocksTotal*1.0*percentageVarianceSelect/(1.0*1E2);
		}
		return tabNb;
	}

	
	
	public static double estimateRegistrationDuration(int[]dims,int viewRegistrationLevel,int maxExpectedTime,int levelMin,int levelMax,int nIterations,Transform3DType transformType,
													int []blockHalfSizes,int []strides,int []neighbourhoods,int nCpu,int percentageScoreSelect,int percentageVarianceSelect,int percentageRandomSelect,boolean subsampleZ,int higherAccuracy) {
		if(higherAccuracy==1)levelMin=-1;
		System.out.println("DEBUG\nDEBUG ESTIMATION");
		int nVoxels=dims[0]*dims[1]*dims[2];
		int nNeighbours=(1+2*neighbourhoods[0])*(1+2*neighbourhoods[1])*(1+2*neighbourhoods[2]);
		int blockSize=(1+2*blockHalfSizes[0])*(1+2*blockHalfSizes[1])*(1+2*blockHalfSizes[2]);
		double []nBlocksPerLevel=estimateBlocksNumberPerLevel(dims,viewRegistrationLevel,levelMin,levelMax,nIterations,
				transformType,blockHalfSizes,strides,neighbourhoods,percentageScoreSelect,percentageVarianceSelect,percentageRandomSelect,subsampleZ);
		int nLevels=nBlocksPerLevel.length;
		double nImgUpdateView=viewRegistrationLevel;
		if(viewRegistrationLevel==2)nImgUpdateView=3.5*0.57;
		double sumLevels=0;
		double sumLevels2=0;
		double sumNbBlocks=0;
		System.out.println("Nb blocks="+TransformUtils.stringVectorN(nBlocksPerLevel,""));
		for(int lev=levelMax;lev>=levelMin;lev--){
			sumLevels+= (lev<=1) ? 1 : (1.0/lev);
			sumLevels2+= (lev<=1) ? 1 : (1.0/Math.sqrt(lev+1));
			sumNbBlocks+=nBlocksPerLevel[levelMax-lev];
		}
		System.out.println("Levmax="+levelMax);
		System.out.println("Levmin="+levelMin);
		System.out.println("N voxels of image="+nVoxels);
		System.out.println("N neighbours="+nNeighbours);
		System.out.println("nLevel="+nLevels);
		System.out.println("nIterations="+nIterations);
		System.out.println("sumLevels="+sumLevels);
		System.out.println("blocksize="+blockSize);
		System.out.println("blocks per level="+TransformUtils.stringVectorN(nBlocksPerLevel, ""));
		
		double alphaTrans=1E-7;
		double alphaGlob=1E-7;
		double alphaLev=1E-7;
		double alphaIter=1E-7;
		double alphaProdDense=1E-7;
		double alphaComp=300;
		double alphaBm=0.9;
		double alphaVar=0.14;
		double factorTrans=1E-7;
		double factorVarBlocks=2.5E-8;
		double factorCompBlocks=1.6E-8;
		double factorProdDense=1E-7;
		
		double transTime=alphaTrans+nVoxels*factorTrans; // time for a transformation = constante + factor * N voxels
		double timeUpdatesView=transTime*nImgUpdateView*1.125*(1.9+1.52*nLevels*nIterations); //time for the first update, and one for each iteration. Each updated image lies 4 transformations
		double timeTransRefAndMov=0.625*sumLevels2*transTime*(1+nIterations); // One transfo per level for ref, one transfo per iteration for mov. 
		double timeVariance=nIterations*(alphaVar+0.12*factorVarBlocks*sumNbBlocks*blockSize);
		double timeBlockMatching=(factorCompBlocks*sumNbBlocks*(alphaComp+0.455*blockSize)*nNeighbours*(1.0/nCpu)+alphaBm)*nIterations;
		double timeTransfoProduction=transTime*1.4*(1.9)*(nLevels*nIterations); // production of dense field		
		double timeBonus=timeUpdatesView+timeVariance;
		double totalTime=timeUpdatesView+timeTransRefAndMov+timeVariance+timeBlockMatching+(transformType==Transform3DType.DENSE ? timeTransfoProduction : 0)+timeBonus;

		System.out.println("Trans time="+transTime);
		System.out.println("timeUpdatesView="+ timeUpdatesView);
		System.out.println("timeTransRefAndMov="+ timeTransRefAndMov);
		System.out.println("timeVariance="+ timeVariance);
		System.out.println("timeBlockMatching="+ timeBlockMatching);
		System.out.println("timeTransfoProduction="+ timeTransfoProduction);
		System.out.println("timeBonus="+ timeBonus);
		System.out.println("Total time="+totalTime);
		System.out.println("DEBUG\nDEBUG ESTIMATION");
		
		return totalTime;
		/**Pas detail :
		***************Global : 
		* First update view : 2.24 s   prop to (alpha + nvox image) * nbimagesconsideredfor debug
		***************Level :
		* trans ref : 0.35  0.6      prop to nvox image and target resolution (level)
		*
		***************Iteration
		*trans mov : 0.32  0.54     prop to nvox image and target resolution (level)
		*compute var : 0.23 0.87    prop to nb blocks at level dot (alpha+ block size)
		*bm : 2.72  8.4            prop nblocks dot nneighbour dot (alpha + block size)
		*compute trans if dense
		*update view : 1.613  1.7  prop to (alpha + nvox image) 

		*
		* Transtime=alphatrans * nvoximage
		* Global : alphaGlob + NimgDisplay * 4 * transtime 
		*         
		* Levels : Sigma_over_levels  (alphaLev + transTime / level) 
		* Iter  : N iter * (alphaIter + transTime / level + factorBlocks*nbBlocksatlevel*blocksize + factorBisBlocks*nbBlocks* nb neighbour * (alpha + blocksize) / nbcpu  + 3*Nimgdisplay*transtime) 
		*  
		*  inconnues : nbblocksatlevel, alphatrans, alphaglob, alphaLev, alphaIter, factorBlocks, factorBisBlocks 
		*/
	}
		

	
	
		

	
	/** Run algorithm from an initial situation (trInit), and return the final transform (including trInit)
 */
	@SuppressWarnings("unchecked")
	public ItkTransform runBlockMatching(ItkTransform trInit) {
		double[]timesGlob=new double[20];
		double[][]timesLev=new double[nbLevels][20];
		double[][][]timesIter=new double[nbLevels][nbIterations][20];
		long t0= System.currentTimeMillis();
		timesGlob[0]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
		handleOutput("Absolute time at start="+t0);
		double progress=0;IJ.showProgress(progress);
		this.currentTransform=trInit;
		if(this.currentTransform==null)this.currentTransform=new ItkTransform();
		handleOutput(new Date().toString());
		//Initialize various artifacts
		ImagePlus imgRefTemp;
		ImagePlus imgMovTemp;
		handleOutput("------------------------------");
		handleOutput("| Block Matching registration|");
		handleOutput("------------------------------");
		handleOutput(" ");
		handleOutput(" ");
		handleOutput("   .-------.          ______");
		handleOutput("  /       /|         /\\     \\");
		handleOutput(" /_______/ |        /  \\     \\");
		handleOutput(" |       | |  -->  /    \\_____\\");
		handleOutput(" |       | /       \\    /     /");
		handleOutput(" |       |/         \\  /     /");
		handleOutput(" .-------.           \\/____ /");
		handleOutput("");
		handleOutput("Parameters Summary");
		handleOutput(" |  ");
		handleOutput(" |--* Transformation type = "+this.transformationType);
		handleOutput(" |--* Metric type = "+this.metricType);
		handleOutput(" |--* Min block variance = "+this.minBlockVariance);
		
		handleOutput(" |  ");
		handleOutput(" |--* Reference image initial size = "+this.imgRef.getWidth()+" X "+this.imgRef.getHeight()+" X "+this.imgRef.getStackSize()+
		"   with voxel size = "+VitimageUtils.dou(this.imgRef.getCalibration().pixelWidth)+" X "+VitimageUtils.dou(this.imgRef.getCalibration().pixelHeight)+" X "+VitimageUtils.dou(this.imgRef.getCalibration().pixelDepth)+"  , unit="+this.imgRef.getCalibration().getUnit()+" . Mean background value="+this.imgRefDefaultValue);
		handleOutput(" |--* Moving image initial size = "+this.imgMov.getWidth()+" X "+this.imgMov.getHeight()+" X "+this.imgMov.getStackSize()+
		"   with voxel size = "+VitimageUtils.dou(this.imgMov.getCalibration().pixelWidth)+" X "+VitimageUtils.dou(this.imgMov.getCalibration().pixelHeight)+" X "+VitimageUtils.dou(this.imgMov.getCalibration().pixelDepth)+"  , unit="+this.imgMov.getCalibration().getUnit()+" . Mean background value="+this.imgMovDefaultValue);
		handleOutput(" |--* Block sizes(pix) = [ "+this.blockSizeX+" X "+this.blockSizeY+" X "+this.blockSizeZ+" ] . Block neigbourhood(pix) = "+this.neighbourhoodSizeX+" X "+this.neighbourhoodSizeY+" X "+this.neighbourhoodSizeZ+" . Stride active, select one block every "+this.blocksStrideX+" X "+this.blocksStrideY+" X "+this.blocksStrideZ+" pix");
		handleOutput(" |  ");
		handleOutput(" |--* Blocks selected by variance sorting = "+this.percentageBlocksSelectedByVariance+" %");	
		handleOutput(" |--* Blocks selected randomly = "+this.percentageBlocksSelectedRandomly+" %");	
		handleOutput(" |--* Blocks selected by score = "+this.percentageBlocksSelectedByScore+" %");	
		handleOutput(" |  ");
		handleOutput(" |--* Iterations for each level = "+this.nbIterations);
		handleOutput(" |--* Successive "+TransformUtils.stringVectorN(this.subScaleFactors, "subscale factors"));
		handleOutput(" |--* Successive "+TransformUtils.stringVectorN(this.successiveStepFactors, "step factors (in pixels)"));
		handleOutput(" |--* Successive sigma for dense field interpolation = "+TransformUtils.stringVectorN(this.successiveDenseFieldSigma, ""));
		handleOutput(" |--* Successive sigma for image resampling = "+TransformUtils.stringVectorN(this.successiveSmoothingSigma, ""));
		if(this.displayR2) {
			handleOutput(" |--* Beginning matching. Initial superposition with global R^2 = "+getGlobalRsquareWithActualTransform() +"\n\n");		
		}
		this.updateViews(0,0,0,this.transformationType==Transform3DType.DENSE ? null : this.currentTransform.drawableString());
		timesGlob[1]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
		int nbProc=this.defaultCoreNumber;
		double timeFactor=0.000000003;
		ImagePlus imgMaskTemp=null;
		progress=0.05;IJ.showProgress(progress);

		
		//for each scale
		timesGlob[2]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
		for(int lev=0;lev<nbLevels;lev++) {
			timesLev[lev][0]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
			handleOutput("");
			int[]curDims=successiveDimensions[lev];
			double[]curVoxSizes=successiveVoxSizes[lev];
			int []subSamplingFactors=new int[] {(int)Math.round(this.imgRef.getWidth()*1.0/curDims[0]),(int)Math.round(this.imgRef.getHeight()*1.0/curDims[1]),(int)Math.round(this.imgRef.getStackSize()*1.0/curDims[2])};
			double stepFactorN=this.successiveStepFactors[lev];
			double voxMin=Math.min(curVoxSizes[0],Math.min(curVoxSizes[1],curVoxSizes[2]));
			double stepFactorX=stepFactorN*voxMin/curVoxSizes[0];
			double stepFactorY=stepFactorN*voxMin/curVoxSizes[1];
			double stepFactorZ=stepFactorN*voxMin/curVoxSizes[2];
			handleOutput("--> Level "+(lev+1)+"/"+nbLevels+" . Dims=("+curDims[0]+"x"+curDims[1]+"x"+curDims[2]+"), search step factors =("+stepFactorX+","+stepFactorY+","+stepFactorZ+")"+" pixels. Subsample factors="+subSamplingFactors[0]+","+subSamplingFactors[1]+","+subSamplingFactors[2]);

			// blocks from fixed
			int nbBlocksX=1+(curDims[0]-this.blockSizeX-2*this.neighbourhoodSizeX*strideMoving)/this.blocksStrideX;
			int nbBlocksY=1+(curDims[1]-this.blockSizeY-2*this.neighbourhoodSizeY*strideMoving)/this.blocksStrideY;
			int nbBlocksZ=1;
			if(curDims[2]>1)nbBlocksZ=1+(curDims[2]-this.blockSizeZ-2*this.neighbourhoodSizeZ*strideMoving)/this.blocksStrideZ;
			int nbBlocksTotal=nbBlocksX*nbBlocksY*nbBlocksZ;			
			double nbBlocksActually=nbBlocksTotal*1.0*this.percentageBlocksSelectedByScore*this.percentageBlocksSelectedByVariance*this.percentageBlocksSelectedRandomly/(1.0*1E6);
			long lo1=((int)nbBlocksActually)*(this.neighbourhoodSizeX*2+1)*(this.neighbourhoodSizeY*2+1)*(this.neighbourhoodSizeZ*2+1)*nbIterations;
			double d01=lo1/1000.0;	
			double d02=d01*this.blockSizeX*this.blockSizeY*this.blockSizeZ/1000.0;
			double levelTime=(1.0/nbProc)*(this.neighbourhoodSizeX*2+1)*(this.neighbourhoodSizeY*2+1)*(this.neighbourhoodSizeZ*2+1)*this.blockSizeX*this.blockSizeY*this.blockSizeZ*
					nbBlocksActually*timeFactor*nbIterations;
			handleOutput("    At this level : # Blocks comparison="+VitimageUtils.dou(d01/1000.0)+" Mega-Ops.     # of voxelwise operations="+VitimageUtils.dou(d02/1000.0)+" Giga-Ops.    Expected computation time="+VitimageUtils.dou(levelTime)+" seconds.");		

			
			final double voxSX=curVoxSizes[0];
			final double voxSY=curVoxSizes[1];
			final double voxSZ=curVoxSizes[2];
			final int bSX=this.blockSizeX;
			final int bSY=this.blockSizeY;
			final int bSZ=this.blockSizeZ;
			final int bSXHalf=this.blockSizeHalfX;
			final int bSYHalf=this.blockSizeHalfY;
			final int bSZHalf=this.blockSizeHalfZ;
			final int nSX=this.neighbourhoodSizeX;
			final int nSY=this.neighbourhoodSizeY;
			final int nSZ=this.neighbourhoodSizeZ;
 			
			//resample and smooth the fixed image, at the scale and with the smoothing sigma chosen 
			this.resampler.setDefaultPixelValue(this.imgRefDefaultValue);
			this.resampler.setTransform(new ItkTransform());
			this.resampler.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(this.successiveVoxSizes[lev]));
			this.resampler.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(this.successiveDimensions[lev]));
			imgRefTemp=VitimageUtils.gaussianFilteringIJ(this.imgRef, this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev]);
			timesLev[lev][1]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
			imgRefTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgRefTemp)));
			timesLev[lev][2]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

			//resample the mask image
			if(this.mask !=null) {
				this.resampler.setDefaultPixelValue(255);
				imgMaskTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(this.mask)));
			}
			timesLev[lev][3]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
			//for each iteration
			for(int iter=0;iter<nbIterations;iter++) {
				timesLev[lev][4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				timesIter[lev][iter][0]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				progress=0.1+0.9*(lev*1.0/nbLevels+(iter*1.0/nbIterations)*1.0/nbLevels);IJ.showProgress(progress);
				handleOutput("\n   --> Iteration "+(iter+1) +"/"+this.nbIterations);
				
				this.resampler.setTransform(this.currentTransform);
				this.resampler.setDefaultPixelValue(this.imgMovDefaultValue);
				imgMovTemp=VitimageUtils.gaussianFilteringIJ(this.imgMov, this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev]);
				imgMovTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgMovTemp)));
				timesIter[lev][iter][1]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

				

				//Prepare a coordinate summary tabs for this blocks, compute and store their sigma
				int indexTab=0;
				nbBlocksTotal=nbBlocksX*nbBlocksY*nbBlocksZ;
				double[][] blocksRefTmp=new double[nbBlocksTotal][4];
				handleOutput("       # blocks before trimming="+nbBlocksTotal);
				
				
				
				timesIter[lev][iter][2]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				for(int blX=0;blX<nbBlocksX ;blX++) {
					for(int blY=0;blY<nbBlocksY ;blY++) {
						for(int blZ=0;blZ<nbBlocksZ ;blZ++) {
							double[]valsBlock=VitimageUtils.valuesOfBlock(imgRefTemp,
									blX*this.blocksStrideX+this.neighbourhoodSizeX*strideMoving,                blY*this.blocksStrideY+this.neighbourhoodSizeY*strideMoving,                blZ*this.blocksStrideZ+this.neighbourhoodSizeZ*strideMoving,
									blX*this.blocksStrideX+this.blockSizeX+this.neighbourhoodSizeX*strideMoving-1,blY*this.blocksStrideY+this.blockSizeY+this.neighbourhoodSizeY*strideMoving-1,blZ*this.blocksStrideZ+this.blockSizeZ+this.neighbourhoodSizeZ*strideMoving-1);
							double[]stats=VitimageUtils.statistics1D(valsBlock);
							//if(stats[0]>0.001)System.out.println("On en tient un : block "+blX+" , "+blY+" , "+blZ+" et vals="+stats[0]+","+stats[1]);
							blocksRefTmp[indexTab++]=new double[] {stats[1],blX*this.blocksStrideX+this.neighbourhoodSizeX*strideMoving,       blY*this.blocksStrideY+this.neighbourhoodSizeY*strideMoving,     blZ*this.blocksStrideZ+this.neighbourhoodSizeZ*strideMoving};
						}
					}
				}
				timesIter[lev][iter][3]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

				
				//Sort by variance
				double meanVar=0;
				for(int i=0;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
				Arrays.sort(blocksRefTmp,new VarianceComparator());
				timesIter[lev][iter][4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				
				//Keep this.percentageBlocksSelectedByVariance
				int lastRemoval=(nbBlocksTotal*(100-this.percentageBlocksSelectedByVariance))/100;
				for(int bl=0;bl<lastRemoval;bl++)blocksRefTmp[bl][0]=-1;
				meanVar=0;
				for(int i=lastRemoval;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
				handleOutput("Tri par variance : elimination des blocs de 0 à  "+lastRemoval+" / "+blocksRefTmp.length);
				nbBlocksTotal=0;
				for(int bl=0;bl<blocksRefTmp.length;bl++)if(blocksRefTmp[bl][0]>=this.minBlockVariance)nbBlocksTotal++;
				double[][] blocksRef=new double[nbBlocksTotal][4];
				nbBlocksTotal=0;
				for(int bl=0;bl<blocksRefTmp.length;bl++)if(blocksRefTmp[bl][0]>=this.minBlockVariance) {
					blocksRef[nbBlocksTotal][3]=blocksRefTmp[bl][0];
					blocksRef[nbBlocksTotal][0]=blocksRefTmp[bl][1];
					blocksRef[nbBlocksTotal][1]=blocksRefTmp[bl][2];
					blocksRef[nbBlocksTotal++][2]=blocksRefTmp[bl][3];
				}
				handleOutput("       # blocks after trimming="+nbBlocksTotal);
				timesIter[lev][iter][5]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
			
				//Trim the ones outside the mask
				if(this.mask !=null)blocksRef=this.trimUsingMask(blocksRef,imgMaskTemp,bSX,bSY,bSZ);
				this.correspondanceProvidedAtStart=null;
				nbBlocksTotal=blocksRef.length;
				
				// Multi-threaded exectution of the algorithm core (a block-matching)
				final ImagePlus imgRefTempThread;
				final ImagePlus imgMovTempThread;
				final double minBS=this.minBlockScore;
				final double [][][][]correspondances=new double[nbProc][][][];
				final double [][][]blocksProp=createBlockPropsFromBlockList(blocksRef,nbProc);
				imgRefTempThread=imgRefTemp.duplicate();
				imgMovTempThread=imgMovTemp.duplicate();
				AtomicInteger atomNumThread=new AtomicInteger(0);
				AtomicInteger curProcessedBlock=new AtomicInteger(0);
				AtomicInteger flagAlert=new AtomicInteger(0);
				final int nbTotalBlock=blocksRef.length;
				this.threads = VitimageUtils.newThreadArray(nbProc);    
				timesIter[lev][iter][6]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				for (int ithread = 0; ithread < nbProc; ithread++) {  
					this.threads[ithread] = new Thread() {  { setPriority(Thread.NORM_PRIORITY); }  

					
					public void run() {  
					
						try {
							int numThread=atomNumThread.getAndIncrement();
							double[][]blocksPropThread=blocksProp[numThread];
							double[][][]correspondancesThread=new double[blocksProp[numThread].length][][];
							//for each fixed block
							for(int fixBl=0;fixBl<blocksProp[numThread].length && !interrupted();fixBl++) {
								//extract ref block data in moving image
								int x0=(int)Math.round(blocksPropThread[fixBl][0]);
								int y0=(int)Math.round(blocksPropThread[fixBl][1]);
								int z0=(int)Math.round(blocksPropThread[fixBl][2]);
								int x1=x0+bSX-1;
								int y1=y0+bSY-1;
								int z1=z0+bSZ-1;
								double []valsFixedBlock=VitimageUtils.valuesOfBlock(imgRefTempThread,x0,y0,z0,x1,y1,z1);
								double scoreMax=-10E100;
								double distMax=0;
								int xMax=0;
								int yMax=0;
								int zMax=0;
								//for each moving block
								int numBl=curProcessedBlock.getAndIncrement();
								if(nbTotalBlock>1000 && (numBl%(nbTotalBlock/20)==0))handleOutputNoNewline((" "+((numBl*100)/nbTotalBlock)+"%"));
								if(nbTotalBlock>1000 && (numBl%(nbTotalBlock/3)==0))handleOutput((" "+((numBl*100)/nbTotalBlock)+"%"));
								for(int xPlus=-nSX*strideMoving;xPlus<=nSX*strideMoving;xPlus+=strideMoving) {
									for(int yPlus=-nSY*strideMoving;yPlus<=nSY*strideMoving;yPlus+=strideMoving) {
										for(int zPlus=-nSZ*strideMoving;zPlus<=nSZ*strideMoving;zPlus+=strideMoving) {
											//compute similarity between blocks, according to the metric
											
											double []valsMovingBlock=curDims[2]==1 ? 
															VitimageUtils.valuesOfBlockDoubleSlice(imgMovTempThread,x0+xPlus*stepFactorX,y0+yPlus*(stepFactorY),x1+xPlus*(stepFactorX),y1+yPlus*(stepFactorY) ) :
														    VitimageUtils.valuesOfBlockDouble(imgMovTempThread,x0+xPlus*stepFactorX,y0+yPlus*(stepFactorY),z0+zPlus*(stepFactorZ),x1+xPlus*(stepFactorX),y1+yPlus*(stepFactorY),z1+zPlus*(stepFactorZ) );
												    		
											double score=computeBlockScore(valsFixedBlock,valsMovingBlock);
											double distance=Math.sqrt( (xPlus*voxSX*stepFactorX *  (xPlus*voxSX*stepFactorX) +  
													(yPlus*voxSY*stepFactorY) *  (yPlus*voxSY*stepFactorZ) +
													(zPlus*voxSZ*stepFactorZ) *  (zPlus*voxSZ*stepFactorZ)     )  );
												
											if(Math.abs(score)>10E10) {
												final int flagA=flagAlert.getAndIncrement();
												if(flagA<1) {
													handleOutput("THREAD ALERT");
													handleOutput("SCORE > 10E20 between ("+x0+","+y0+","+z0+") and ("+(x0+xPlus*stepFactorX)+","+(y0+yPlus*stepFactorY)+","+(z0+zPlus*stepFactorZ)+")");
													System.out.println("En effet, corr="+correlationCoefficient(valsFixedBlock, valsMovingBlock));
													handleOutput(TransformUtils.stringVectorN(valsFixedBlock, "Vals fixed"));
													handleOutput(TransformUtils.stringVectorN(valsMovingBlock, "Vals moving"));
													System.exit(0);//
													//VitimageUtils.waitFor(10000);
												}
											}
											//keep the best one
											if( (score>scoreMax) || ((score==scoreMax) && (distance < distMax)) ) {
												xMax=xPlus;yMax=yPlus;zMax=zPlus;scoreMax=score;distMax=distance;}
										}
									}
								}
								correspondancesThread[fixBl]=new double[][] {							
										new double[] {blocksPropThread[fixBl][0]+bSXHalf , blocksPropThread[fixBl][1]+bSYHalf , blocksPropThread[fixBl][2]+bSZHalf} ,
										new double[] {blocksPropThread[fixBl][0]+bSXHalf + xMax*stepFactorX, blocksPropThread[fixBl][1]+bSYHalf  + yMax*stepFactorZ, blocksPropThread[fixBl][2]+bSZHalf  + zMax*stepFactorZ},
										new double[] {scoreMax,1} 	};
							}
							int nbKeep=0;
							for(int i=0;i<correspondancesThread.length;i++)if(correspondancesThread[i][2][0]>=minBS)nbKeep++;
							double[][][]correspondancesThread2=new double[nbKeep][][];
							nbKeep=0;
							for(int i=0;i<correspondancesThread.length;i++){
								if(correspondancesThread[i][2][0]>=minBS) {
									correspondancesThread2[nbKeep]=new double[][] {{0,0,0},{0,0,0},{0,0}};
									for(int l=0;l<3;l++)for(int c=0;c<(l==2 ? 2 : 3) ; c++ )correspondancesThread2[nbKeep][l][c]=correspondancesThread[i][l][c];
									nbKeep++;
								}
							}
							if(numThread==0)handleOutput("Tri par score > "+minBS+" , before="+nbProc*correspondancesThread.length+" and after="+nbProc*nbKeep);
	
							correspondances[numThread]=correspondancesThread2; 
						} catch(Exception ie) {}
						} 
						public void cancel() {interrupt();}
					};  		
				}				
				timesIter[lev][iter][7]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				VitimageUtils.startAndJoin(threads);
				if(bmIsInterrupted) {
					bmIsInterruptedSucceeded=true;
					VitimageUtils.waitFor(1000);
					int nbAlive=1;
					while(nbAlive>0) {
						nbAlive=0;
						for(int th=0;th<this.threads.length;th++)if(this.threads[th].isAlive())nbAlive++;
						handleOutput("Trying to stop blockmatching. There is still "+nbAlive+" threads running over "+this.threads.length);
					}
					this.closeLastImages();
					this.freeMemory();					
					return null;
				}
				timesIter[lev][iter][8]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				handleOutput("");
				//Convert the correspondance from each thread correspondance list to a main list for the whole image
				ArrayList<double[][]> listCorrespondances=new ArrayList<double[][]>();
				for(int i=0;i<correspondances.length;i++) {
					for(int j=0;j<correspondances[i].length;j++){
						listCorrespondances.add(correspondances[i][j]);	
					}
				}
				timesIter[lev][iter][9]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				
				
				// Selection step 1 : select correspondances by score
				ItkTransform transEstimated=null;
				int nbPts1=listCorrespondances.size();
				Object[]ret=getCorrespondanceListAsTrimmedPointArray(listCorrespondances,this.successiveVoxSizes[lev],this.percentageBlocksSelectedByScore,100,transEstimated);
				Point3d[][]correspondancePoints=(Point3d[][])ret[0];
				listCorrespondances=(ArrayList<double[][]>) ret[2];
				int nbPts2=listCorrespondances.size();
				this.lastValueBlocksCorr=(Double)ret[1];
				timesIter[lev][iter][10]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

				//if affine
				if(this.transformationType!=Transform3DType.DENSE) {					
					switch(this.transformationType) {
						case VERSOR:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
						case AFFINE:transEstimated=ItkTransform.estimateBestAffine3D(correspondancePoints[1],correspondancePoints[0]);break;
						case SIMILARITY:transEstimated=ItkTransform.estimateBestSimilarity3D(correspondancePoints[1],correspondancePoints[0]);break;
						default:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
					}
					timesIter[lev][iter][11]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					if(correspondancePoints[1].length<5) {
						System.out.println("Warning : less than 5 correspondance points. Setting up identity transform in replacement");
						transEstimated=new ItkTransform();
					}
					
					else {
						ret=getCorrespondanceListAsTrimmedPointArray(listCorrespondances,this.successiveVoxSizes[lev],100,this.percentageBlocksSelectedByLTS,transEstimated);
						timesIter[lev][iter][12]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
						correspondancePoints=(Point3d[][])ret[0];
						listCorrespondances=(ArrayList<double[][]>) ret[2];
						this.lastValueBlocksCorr=(Double)ret[1];
						int nbPts3=listCorrespondances.size();
						System.out.println("Nb correspondances : "+nbPts1 +" , after score selection : "+nbPts2+" , after LTS selection : "+nbPts3);
						
						transEstimated=null;
						switch(this.transformationType) {
							case VERSOR:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
							case AFFINE:transEstimated=ItkTransform.estimateBestAffine3D(correspondancePoints[1],correspondancePoints[0]);break;
							case SIMILARITY:transEstimated=ItkTransform.estimateBestSimilarity3D(correspondancePoints[1],correspondancePoints[0]);break;
							default:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
						}
						timesIter[lev][iter][13]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
						
						if(displayRegistration==2) {
							Object [] obj=VitimageUtils.getCorrespondanceListAsImagePlus(imgRef,listCorrespondances,curVoxSizes,this.sliceInt,						
								this.blocksStrideX*subSamplingFactors[0],this.blocksStrideY*subSamplingFactors[1],this.blocksStrideZ*subSamplingFactors[2],
								blockSizeHalfX*subSamplingFactors[0],blockSizeHalfY*subSamplingFactors[1],blockSizeHalfZ*subSamplingFactors[2],false);
							this.correspondancesSummary=(ImagePlus) obj[0];
							this.sliceIntCorr=1+(int) obj[1];	
						}
						timesIter[lev][iter][14]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					}
					//Finally, add it to the current stack of transformations
					handleOutput("  Update to transform = \n"+transEstimated.drawableString());
					if(!transEstimated.isIdentityAffineTransform(1E-6,0.05*Math.min(Math.min(voxSX,voxSY),voxSZ) ) ){
						this.currentTransform.addTransform(transEstimated);
						handleOutput("Global transform after this step =\n"+this.currentTransform.drawableString());
					}
					else {
						System.out.println("Last transformation computed was identity. Convergence seems to be attained. Going to next level");
						iter=nbIterations;
						continue;
					}
					timesIter[lev][iter][15]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				}
				
				else {
					handleOutput("       Field interpolation from "+correspondancePoints[0].length+" correspondances with sigma="+this.successiveDenseFieldSigma[lev]+" "+imgRefTemp.getCalibration().getUnit()+
							" ( "+((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSX)))+" voxSX , " +((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSY)))+" voxSY , " +((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSZ)))+" voxSZ )"  );
					
					//compute the field
					this.currentField[indField++]=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(correspondancePoints,imgRefTemp,this.successiveDenseFieldSigma[lev],false);
					timesIter[lev][iter][11]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					timesIter[lev][iter][12]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					timesIter[lev][iter][13]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					timesIter[lev][iter][14]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					
					//Finally, add it to the current stack of transformations
					this.currentTransform.addTransform(new ItkTransform(new DisplacementFieldTransform( this.currentField[indField-1])));
					timesIter[lev][iter][15]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				}
				if(displayR2) {
					globalR2Values[incrIter]=getGlobalRsquareWithActualTransform();
					timesIter[lev][iter][16]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					this.lastValueCorr=globalR2Values[incrIter];
					handleOutput("Global R^2 after iteration="+globalR2Values[incrIter++]);
				}
				this.updateViews(lev,iter,(this.levelMax-lev)>=1 ? 0 : (1-this.levelMax+lev),this.transformationType==Transform3DType.DENSE ? null : this.currentTransform.drawableString());
				timesIter[lev][iter][17]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
			}// Back for another iteration
			timesLev[lev][4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
		} // Back for another level
		timesGlob[3]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
		handleOutput(new Date().toString());

		if(this.transformationType!=Transform3DType.DENSE)handleOutput("\nMatrice finale block matching : \n"+this.currentTransform.drawableString());
		if(false && this.transformationType==Transform3DType.DENSE)this.sliceJacobian.hide();

		
		if(displayR2) {
			handleOutput("Successive R2 values :");
			for(int i=0;i<incrIter;i++)handleOutput(" -> "+globalR2Values[i]);
		}
		timesGlob[4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

		
		
		if(this.timingMeasurement) {
			System.out.println("\n\n\n\n\n###################################################\n\nDebrief timing");
			System.out.println("Parametres : ");
			handleOutput(" |--* Transformation type = "+this.transformationType);
			handleOutput(" |--* Metric type = "+this.metricType);
			handleOutput(" |--* Min block variance = "+this.minBlockVariance);
			
			handleOutput(" |  ");
			handleOutput(" |--* Reference image initial size = "+this.imgRef.getWidth()+" X "+this.imgRef.getHeight()+" X "+this.imgRef.getStackSize()+
			"   with voxel size = "+VitimageUtils.dou(this.imgRef.getCalibration().pixelWidth)+" X "+VitimageUtils.dou(this.imgRef.getCalibration().pixelHeight)+" X "+VitimageUtils.dou(this.imgRef.getCalibration().pixelDepth)+"  , unit="+this.imgRef.getCalibration().getUnit()+" . Mean background value="+this.imgRefDefaultValue);
			handleOutput(" |--* Moving image initial size = "+this.imgMov.getWidth()+" X "+this.imgMov.getHeight()+" X "+this.imgMov.getStackSize()+
			"   with voxel size = "+VitimageUtils.dou(this.imgMov.getCalibration().pixelWidth)+" X "+VitimageUtils.dou(this.imgMov.getCalibration().pixelHeight)+" X "+VitimageUtils.dou(this.imgMov.getCalibration().pixelDepth)+"  , unit="+this.imgMov.getCalibration().getUnit()+" . Mean background value="+this.imgMovDefaultValue);
			handleOutput(" |--* Block sizes(pix) = [ "+this.blockSizeX+" X "+this.blockSizeY+" X "+this.blockSizeZ+" ] . Block neigbourhood(pix) = "+this.neighbourhoodSizeX+" X "+this.neighbourhoodSizeY+" X "+this.neighbourhoodSizeZ+" . Stride active, select one block every "+this.blocksStrideX+" X "+this.blocksStrideY+" X "+this.blocksStrideZ+" pix");
			handleOutput(" |  ");
			handleOutput(" |--* Blocks selected by variance sorting = "+this.percentageBlocksSelectedByVariance+" %");	
			handleOutput(" |--* Blocks selected randomly = "+this.percentageBlocksSelectedRandomly+" %");	
			handleOutput(" |--* Blocks selected by score = "+this.percentageBlocksSelectedByScore+" %");	
			handleOutput(" |  ");
			handleOutput(" |--* Iterations for each level = "+this.nbIterations);
			handleOutput(" |--* Successive "+TransformUtils.stringVectorN(this.subScaleFactors, "subscale factors"));
			handleOutput(" |--* Successive "+TransformUtils.stringVectorN(this.successiveStepFactors, "step factors (in pixels)"));
			handleOutput(" |--* Successive sigma for dense field interpolation = "+TransformUtils.stringVectorN(this.successiveDenseFieldSigma, ""));
			handleOutput(" |--* Successive sigma for image resampling = "+TransformUtils.stringVectorN(this.successiveSmoothingSigma, ""));
			handleOutput(" |--* Successive sigma for image resampling = "+ TransformUtils.stringVectorN(this.successiveSmoothingSigma, ""));
			System.out.println("\n\n");
			System.out.println("Times globaux : start="+timesGlob[0]+"  fin update view="+timesGlob[1]+"  fin prepa="+timesGlob[2]+"  fin levels="+timesGlob[3]+"  fin return="+timesGlob[3] );
			for(int lev=0;lev<this.nbLevels;lev++) {
				System.out.println("    Times level "+lev+" : start="+timesLev[lev][0]+"  fin gaussRef="+timesLev[lev][1]+"  fin transRef="+timesLev[lev][2]+"  fin prepa3="+timesLev[lev][3]+"fin iters="+timesLev[lev][4] );
				for(int it=0;it<this.nbIterations;it++) {
					System.out.println("          Times iter "+it+" : start="+timesIter[lev][it][0]+"  fin trmov="+timesIter[lev][it][1]+"  fin maketab="+timesIter[lev][it][2] +
												"fin compvar="+timesIter[lev][it][3]+"  fin sortvar="+timesIter[lev][it][4]+"  fin trimvar="+timesIter[lev][it][5]); 
					System.out.println("          Times iter "+it+" : fin prepbm="+timesIter[lev][it][6]+"  fin prejoin="+timesIter[lev][it][7]+"  fin join="+timesIter[lev][it][8] +
							"fin buildcortab="+timesIter[lev][it][9]+"  fin trimscore="+timesIter[lev][it][10]+"  fin firstestimate="+timesIter[lev][it][11]); 
					System.out.println("          Times iter "+it+" : fin LTS="+timesIter[lev][it][12]+"  fin second estimate="+timesIter[lev][it][13]+"  fin correspImage="+timesIter[lev][it][14] +
							"fin addtransform="+timesIter[lev][it][15]+"  fin R2="+timesIter[lev][it][16]+"  fin update view="+timesIter[lev][it][17]); 
				}
			}
			
			System.out.println("Bilan time");
			double d=0;double dSum=0;
			d+=(timesGlob[1]-timesGlob[0]);
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][17]-timesIter[lev][it][14]);
			System.out.println("timeUpdatesView="+d);

			d=0;
			for(int lev=0;lev<this.nbLevels;lev++)			d+=(timesLev[lev][2]-timesLev[lev][1]);
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][1]-timesIter[lev][it][0]);
			System.out.println("timeTransRefAndMov="+d);dSum+=d;
	
			d=0;
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][3]-timesIter[lev][it][2]);
			System.out.println("timeVariance="+d);dSum+=d;

			d=0;
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][8]-timesIter[lev][it][7]);
			System.out.println("timeBlockMatching="+d);dSum+=d;
			
			
			d=timesGlob[3]-timesGlob[0]-dSum;
			System.out.println("timeBonus="+d);
			
			System.out.println("Total time="+(timesGlob[3]-timesGlob[0]));
			
		}
		//glob       0               1            2               3
//		  st    prep         levels          return   
//		timesLev     0            1           2            3          4             5                6                7              8                9                10                 11 
//				     st   prep        tr ref      iters       

		//			timesIter     0            1           2            3          4             5                6                7              8                9                10                 11 
		//                        st   tr mov     maketab      compvar      sortvar     trimvar          prep bm             prejoin       join          buildcorrtab	  trim score
		//                         10                    11                    12                         13                        14                  15          16  
		//                               firstestimate           LTS                 second estimate             correspImage               add              R2
		return new ItkTransform(this.currentTransform);
	}
	
				
	

	
	
	/**	Helper functions for trimming the correspondances
*
*/
	Object[] getCorrespondanceListAsTrimmedPointArray(ArrayList<double[][]>list,double[]voxSizes,int percentageKeepScore,int percentageKeepLTS,ItkTransform transform){
		//Convert voxel space correspondances in real space vectors
		boolean isLTS = (transform!=null);
		int n=list.size();
		int  ind=0;
		double sumWay0=0;
		double sumWay1=0;
		double distance=0;
		for(int i=0;i<n;i++)if(list.get(i)[2][1]<0)n--;
		Point3d[][]tabPt=new Point3d[3][n];
		for(int i=0;i<n;i++)if(list.get(i)[2][1]>0) {
			double [][]tabInit=list.get(i);
			tabPt[0][ind]=new Point3d((tabInit[0][0])*voxSizes[0],(tabInit[0][1])*voxSizes[1],(tabInit[0][2])*voxSizes[2]);
			tabPt[1][ind]=new Point3d((tabInit[1][0])*voxSizes[0],(tabInit[1][1])*voxSizes[1] ,(tabInit[1][2])*voxSizes[2]);
			//If Least Trimmed Squared selection activated, compute distance from reference to moving using transform
			if(isLTS) {
				Point3d pt0Trans=transform.transformPoint(tabPt[0][ind]);
				Point3d pt1Trans=transform.transformPoint(tabPt[1][ind]);
				distance=TransformUtils.norm(new double[] {pt0Trans.x-tabPt[1][ind].x, pt0Trans.y-tabPt[1][ind].y, pt0Trans.z-tabPt[1][ind].z});
			}
			tabPt[2][ind]=new Point3d((tabInit[2][0]),tabInit[2][1],-distance);
			ind++;
		}
		//Compute mean val of the selection variable, before selecting
		double meanVar=0;
		for(int i=0;i<tabPt[0].length;i++)meanVar+=(isLTS ? tabPt[2][i].z : tabPt[2][i].x);
		double meanBef=meanVar/tabPt[0].length;

		
		//Reorganize tab for the sorting
		Point3d[][]tmp=new Point3d[tabPt[0].length][3];
		for(int i=0;i<tabPt.length;i++)		for(int j=0;j<tabPt[0].length;j++)tmp[j][i]=tabPt[i][j];

		//Sort and reorganize back
		if(isLTS)Arrays.sort(tmp, new PointTabComparatorByDistanceLTS());
		else Arrays.sort(tmp, new PointTabComparatorByScore());
		for(int i=0;i<tabPt.length;i++)		for(int j=0;j<tabPt[0].length;j++)tabPt[i][j]=tmp[j][i];
		
		//Keep this.percentageBlocksSelectedByVariance
		int lastRemoval=(int)Math.round(tabPt[0].length*((100-(isLTS ? percentageKeepLTS : percentageKeepScore))/100.0));
		Point3d[][]ret=new Point3d[3][tabPt[0].length-lastRemoval];
		ArrayList<double[][]>listRet=new ArrayList();
		for(int bl=lastRemoval;bl<tabPt[0].length;bl++) {
			ret[0][bl-lastRemoval]=tabPt[0][bl];
			ret[1][bl-lastRemoval]=tabPt[1][bl];
			ret[2][bl-lastRemoval]=tabPt[2][bl];
			listRet.add(new double[][]{  { ret[0][bl-lastRemoval].x/voxSizes[0], ret[0][bl-lastRemoval].y/voxSizes[1], ret[0][bl-lastRemoval].z/voxSizes[2]} , 
										{ ret[1][bl-lastRemoval].x/voxSizes[0], ret[1][bl-lastRemoval].y/voxSizes[1], ret[1][bl-lastRemoval].z/voxSizes[2]} ,
										{ ret[2][bl-lastRemoval].x, ret[2][bl-lastRemoval].y, ret[2][bl-lastRemoval].z} } );								
		}
		
		//Compute mean val of the selection variable, after selecting
		meanVar=0;
		for(int i=0;i<ret[0].length;i++) {
			meanVar+=(isLTS ? ret[2][i].z : ret[2][i].x);
		}
		if(isLTS) handleOutput("   Mean resulting distance between transformed corresponding points before trimming / after trimming : "+VitimageUtils.dou(-meanBef)+" / "+VitimageUtils.dou(-meanVar/(ret[0].length)));
		else handleOutput("   Mean correspondance score before / after : "+VitimageUtils.dou(meanBef)+" / "+VitimageUtils.dou(meanVar/(ret[0].length)));
		return new Object[] {ret,meanVar/(ret[0].length),listRet};
	}
		
	public double[][][]createBlockPropsFromBlockList(double[][]blockList,int nbProc){
		double[][][]ret=new double[nbProc][][];
		int nbTot=blockList.length;
		int ind0,ind1,nbPr,nbParPr;
		nbParPr=(int)Math.ceil(1.0*nbTot/nbProc);
		for(int pr=0;pr<nbProc;pr++) {
			ind0=0+(pr*(nbParPr));
			ind1=nbParPr+(pr*(nbParPr));
			if(ind0>nbTot-1)ind0=nbTot;
			if(ind1>nbTot)ind1=nbTot;
			nbPr=ind1-ind0;
			ret[pr]=new double[nbPr][4];
			for(int bl=ind0;bl<ind1;bl++) {
				ret[pr][bl-ind0][0]=blockList[bl][0];
				ret[pr][bl-ind0][1]=blockList[bl][1];
				ret[pr][bl-ind0][2]=blockList[bl][2];
				ret[pr][bl-ind0][3]=blockList[bl][3];
			}
		}
		return ret;
	}

	public double[][] trimUsingMask(double [][]tabIn,ImagePlus imgMaskAtScale,int bSX,int bSY,int bSZ){
		double epsilon=10E-4;
		int[]isOut=new int[tabIn.length];
		int n=tabIn.length;
		int n0=n;
		boolean debug=false;
		for(int i=0;i<tabIn.length;i++) {
			debug=false;
			double []vals=VitimageUtils.valuesOfBlock(imgMaskAtScale,	(int)tabIn[i][0], (int)tabIn[i][1], (int)tabIn[i][2], (int)tabIn[i][0]+bSX,  (int)tabIn[i][1]+bSY,  (int)tabIn[i][2]+bSZ);
			for(int j=0;j<vals.length;j++) {
				if(vals[j]<255-epsilon)isOut[i]=1;
			}
			if(isOut[i]==1)n--;
		}
		double[][]ret=new double[n][4];

		n=0;
		for(int i=0;i<tabIn.length;i++) {
			if(isOut[i]==0) {
				ret[n][0]=tabIn[i][0];
				ret[n][1]=tabIn[i][1];
				ret[n][2]=tabIn[i][2];
				n++;
			}
		}
		handleOutput("    Masking : selected "+n+" over "+n0);
		return ret;
	}
	

	
	

	/** display registration and informations during execution.  
 * 	
 */
	public void updateViewsSingle() {
		this.sliceMov=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(this.imgRef,this.imgMov,false)),this.sliceInt);
		if(sliceRef==null) {
			System.out.print("Starting graphical following tool...");
			this.sliceRef=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.imgRef),this.sliceInt);
			ImagePlus tempImg=new Duplicator().run(imgRef);
			IJ.run(tempImg,"32-bit","");
			tempImg.getProcessor().set(0);
			this.sliceCorr=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(tempImg),this.sliceIntCorr);
			this.sliceFuse=VitimageUtils.compositeRGBDouble(this.sliceRef,this.sliceMov,this.sliceCorr,1,1,1,"Registration is running. Red=Reference, Green=moving, Gray=score");
			this.sliceFuse.show();
			this.sliceFuse.getWindow().setSize(this.viewWidth,this.viewHeight);
			this.sliceFuse.getCanvas().fitToWindow();
			
	
		}
		else {
			sliceCorr=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.correspondancesSummary),this.sliceIntCorr);
			this.sliceRef.show();
			this.sliceMov.show();
			this.sliceCorr.show();
			this.sliceCorr.setSlice(this.sliceIntCorr);
			VitimageUtils.waitFor(3000);
			this.sliceRef.hide();
			this.sliceMov.hide();
			this.sliceCorr.hide();
			
			ImagePlus tempImg=VitimageUtils.compositeRGBDouble(this.sliceRef,this.sliceMov,this.sliceCorr,1,1,1,"Registration is running. Red=Reference, Green=moving, Blue=score");
			tempImg.show();
			IJ.run(tempImg, "Select All", "");
			tempImg.copy();
			this.sliceFuse.paste();
			tempImg.close();

		}
		
		
		this.summaryArray.add(this.sliceFuse.duplicate());
		handleOutput("Added the slice number "+this.summaryArray.size());

	}

	public void updateViews(int level,int iteration,int subpixellic,String textTrans) {
		System.out.println("Levelmax="+this.levelMax);
		System.out.println("Levelmin="+this.levelMin);
		System.out.println("Level="+level);
		System.out.println("Subpixellic="+subpixellic);
		String textIter=String.format("Niveau=%1d/%1d - Iter=%3d/%3d - %s",
				level+1,this.levelMax-this.levelMin+1,
				iteration+1,this.nbIterations,subpixellic>0 ? ("subpixellic 1/"+(1+subpixellic)+" pixel") :""
				);

		if(displayRegistration==0) {
			if(this.summary!=null)this.summary.hide();
			return;
		}
		System.out.print("Updating the views...");
		this.sliceMov=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(this.imgRef,this.imgMov,false)),this.sliceInt);
		if(flagRange)this.sliceMov.setDisplayRange(movRange[0], movRange[1]);
		IJ.run(this.sliceMov,"8-bit","");
		this.sliceMov=VitimageUtils.writeTextOnImage(textIter,this.sliceMov,(this.fontSize*4)/3,0);
		if(textTrans!=null)this.sliceMov=VitimageUtils.writeTextOnImage(textTrans,this.sliceMov,this.fontSize,1);

		if(sliceRef==null) {
			System.out.print("Starting graphical following tool...");
			this.sliceRef=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.imgRef),this.sliceInt);
			if(flagRange)this.sliceRef.setDisplayRange(refRange[0], refRange[1]);
			IJ.run(this.sliceRef,"8-bit","");
			ImagePlus temp=VitimageUtils.writeTextOnImage(textIter,this.sliceRef,(this.fontSize*4)/3,0);
			if(textTrans!=null)temp=VitimageUtils.writeTextOnImage(textTrans,temp,this.fontSize,1);
			if(flagSingleView)this.sliceFuse=(flagRange ? VitimageUtils.compositeNoAdjustOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving, Gray=score. Level=0 Iter=0 "+this.info) : 
				VitimageUtils.compositeOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving, Gray=score. Level=0 Iter=0 "+this.info));
			else this.sliceFuse=flagRange ? VitimageUtils.compositeNoAdjustOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info) : 
				VitimageUtils.compositeOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info);
			this.sliceFuse.show();
			this.sliceFuse.getWindow().setSize(this.viewWidth,this.viewHeight);
			this.sliceFuse.getCanvas().fitToWindow();
			this.sliceFuse.setSlice(this.sliceInt);
			RegistrationManager.adjustImageOnScreen(this.sliceFuse,0,0);
			
			if(displayRegistration>1) {
				ImagePlus tempImg=VitimageUtils.getBinaryGrid(this.imgRef, 10);
				this.sliceGrid=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(tempImg,tempImg,false)),this.sliceInt);
				this.sliceGrid.show();
				this.sliceGrid.setTitle("Field visualization on a uniform 3D grid");
				this.sliceGrid.getWindow().setSize(this.viewWidth,this.viewHeight);
				this.sliceGrid.getCanvas().fitToWindow();
				this.sliceGrid.setSlice(this.sliceInt);
				RegistrationManager.adjustImageOnScreenRelative(this.sliceGrid,this.sliceFuse,2,0);
	
				tempImg=new Duplicator().run(imgRef);
				IJ.run(tempImg,"32-bit","");
				tempImg.getProcessor().set(0);
				this.sliceCorr=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(tempImg),this.sliceIntCorr);
				this.sliceCorr.show();
				this.sliceCorr.setTitle("Correspondances points");
				this.sliceCorr.getWindow().setSize(this.viewWidth,this.viewHeight);
				this.sliceCorr.getCanvas().fitToWindow();
				this.sliceCorr.getProcessor().setMinAndMax(0,1);
				this.sliceCorr.setSlice(this.sliceIntCorr);
				IJ.selectWindow("Correspondances points");
				IJ.run("Fire","");
				RegistrationManager.adjustImageOnScreenRelative(this.sliceCorr,this.sliceGrid,2,0);
			}
			if(flagSingleView)IJ.selectWindow("Registration is running. Red=Reference, Green=moving, Gray=score. Level=0 Iter=0"+" "+this.info);
			else IJ.selectWindow("Registration is running. Red=Reference, Green=moving. Level=0 Iter=0"+" "+this.info);
		}
		else {
			ImagePlus tempImg=null;
			ImagePlus temp=VitimageUtils.writeTextOnImage(textIter,this.sliceRef,(this.fontSize*4)/3,0);
			if(textTrans!=null)temp=VitimageUtils.writeTextOnImage(textTrans,temp,this.fontSize,1);

			if(this.flagSingleView)tempImg=VitimageUtils.compositeRGBDoubleJet(temp,this.sliceMov,this.sliceCorr,"Registration is running. Red=Reference, Green=moving, Gray=score. Level="+level+" Iter="+iteration+" "+this.info,true,1);
			else tempImg=VitimageUtils.compositeOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info);
			VitimageUtils.actualizeData(tempImg,this.sliceFuse);
			if(this.flagSingleView)this.sliceFuse.setTitle("Registration is running. Red=Reference, Green=moving, Gray=score. Level="+level+" Iter="+iteration+" "+this.info);
			else this.sliceFuse.setTitle("Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info);
			this.sliceFuse.setSlice(this.sliceIntCorr);

			if(displayRegistration>1) {
				tempImg=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.correspondancesSummary),this.sliceIntCorr);
				VitimageUtils.actualizeData(tempImg,this.sliceCorr);
				this.sliceCorr.setSlice(this.sliceIntCorr);
	
				
				tempImg=VitimageUtils.getBinaryGrid(this.imgRef, 10);
				tempImg=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(tempImg,tempImg,false)),this.sliceInt);
				VitimageUtils.actualizeData(tempImg,this.sliceGrid);
				this.sliceGrid.setSlice(this.sliceIntCorr);
			}
		}
		
		
		if(this.computeSummary) {
			this.summaryArray.add(this.sliceFuse.duplicate());
			if(displayRegistration>1) {
				this.gridSummaryArray.add(this.sliceGrid.duplicate());
				this.correspondancesSummaryArray.add(this.sliceCorr.duplicate());
				if(false)this.jacobianSummaryArray.add(this.sliceJacobian.duplicate());
			}
			handleOutput("Added the slice number "+this.summaryArray.size());
		}
	}
	
	public void computeSummaries() {
		if(this.displayRegistration==0)return;
		ImagePlus []tabSum=new ImagePlus[summaryArray.size()];
		for(int i =0;i<summaryArray.size() ; i++) {
			tabSum[i]=summaryArray.get(i);
		}

		this.summary=Concatenator.run(tabSum);
		this.summary.setTitle("Summary of alignement VS iterations");
		ImagePlus imgTemp=new Duplicator().run(this.summary);
		IJ.run(imgTemp,"32-bit","");

		ImagePlus []tabGrid=new ImagePlus[gridSummaryArray.size()];
		for(int i =0;i<gridSummaryArray.size() ; i++) {
			tabGrid[i]=gridSummaryArray.get(i);
		}
		ImagePlus []tabCorr=new ImagePlus[correspondancesSummaryArray.size()];
		for(int i =0;i<correspondancesSummaryArray.size() ; i++) {
			tabCorr[i]=correspondancesSummaryArray.get(i);
		}

		
		this.gridSummary=Concatenator.run(tabGrid);
		this.gridSummary.setTitle("Summary of field evolution VS iterations");
		this.gridSummary=VitimageUtils.compositeOf(this.gridSummary, imgTemp);

		this.correspondancesSummary=Concatenator.run(tabCorr);
		this.correspondancesSummary.setTitle("Correspondance points VS iterations");
	}
	
	public void closeLastImages() {
		if(this.displayRegistration>0) {
			this.sliceFuse.changes=false;
			this.sliceFuse.close();
			if(this.computeSummary) {
				this.summary.close();
			}
		}
		if(this.displayRegistration==2) {
			this.sliceGrid.changes=false;
			this.sliceGrid.close();
			this.sliceCorr.changes=false;
			this.sliceCorr.close();
			if(this.computeSummary) {
				this.gridSummary.close();
				this.correspondancesSummary.close();
			}
		}
	}
	
	public void temporarySummariesSave() {
		long lo=new Date().getTime();
		File f=new File("/home/fernandr");
		if(! f.exists())return;
		IJ.saveAsTiff(this.gridSummary, "/mnt/DD_COMMON/Data_VITIMAGE/Temp/Recalage_memories/BM"+lo+"imageGrid.tif");
		IJ.saveAsTiff(this.summary, "/mnt/DD_COMMON/Data_VITIMAGE/Temp/Recalage_memories/BM"+lo+"imageSummary.tif");
		if(displayRegistration>0)this.summary.hide();
		if(displayRegistration==2)this.gridSummary.hide();
	}
	
	public void handleOutput(String s) {
		if (consoleOutputActivated)System.out.println(s);
		if (imageJOutputActivated)IJ.log(s);
	}

	public void handleOutputNoNewline(String s) {
		if (consoleOutputActivated)System.out.print(s);
	}


	
	
	
	
	
	
	
	/** Local and global score computation for block comparison
 * 
 */
	double computeBlockScore(double[]valsFixedBlock,double[]valsMovingBlock) {
		//if(valsFixedBlock.length!=valsMovingBlock.length)return -10E10;
		double measure=0;
		switch(this.metricType) {
		case CORRELATION :return correlationCoefficient(valsFixedBlock,valsMovingBlock);
		case SQUARED_CORRELATION :double score=correlationCoefficient(valsFixedBlock,valsMovingBlock);return (score*score);
		case MEANSQUARE :return -1*meanSquareDifference(valsFixedBlock,valsMovingBlock);
		default:return -10E10;
		}
	}

	double meanSquareDifference(double X[], double Y[]) {
		if(X.length !=Y.length )IJ.log("In meanSquareDifference in BlockMatching, blocks length does not match");
		double sum=0;
		double diff;
		int n=X.length;
		for (int i = 0; i < n; i++) { 
			diff=X[i]-Y[i];
			sum+=(diff*diff);
		}
		return (sum/n);
	}
	
	public double correlationCoefficient(double X[], double Y[]) { 
		//System.out.println("En effet, X.length="+X.length);
		//System.out.println("En effet, Y.length="+Y.length);
		double epsilon=10E-20;
		if(X.length !=Y.length )IJ.log("In correlationCoefficient in BlockMatching, blocks length does not match");
		int n=X.length;
		boolean flag=false;
		double sum_X = 0, sum_Y = 0, sum_XY = 0; 
		double squareSum_X = 0, squareSum_Y = 0; 	
		for (int i = 0; i < n; i++) { 
			sum_X = sum_X + X[i]; 		
			sum_Y = sum_Y + Y[i]; 
			sum_XY = sum_XY + X[i] * Y[i]; 
			squareSum_X = squareSum_X + X[i] * X[i]; 
			squareSum_Y = squareSum_Y + Y[i] * Y[i]; 
		} 
		if(squareSum_X<epsilon || squareSum_Y<epsilon )return 0;
		// use formula for calculating correlation  
		// coefficient. 
		double result=(n * sum_XY - sum_X * sum_Y)/ (Math.sqrt((n * squareSum_X - sum_X * sum_X) * (n * squareSum_Y - sum_Y * sum_Y)));
		if(Math.abs((n * squareSum_X - sum_X * sum_X))<10E-10)return 0; //cas Infinity
		if(Math.abs((n * squareSum_Y - sum_Y * sum_Y))<10E-10)return 0; //cas Infinity
		return result;
	} 
		
	public double getGlobalRsquareWithActualTransform() {
		ImagePlus imgResampled;
		imgResampled=this.currentTransform.transformImage(imgRef,imgMov,false);
		int[]dims=VitimageUtils.getDimensions(imgRef);
		double[]refData=VitimageUtils.valuesOfBlock(imgRef,0,0,0,dims[0],dims[1],dims[2]);
		double[]movData=VitimageUtils.valuesOfBlock(imgResampled,0,0,0,dims[0],dims[1],dims[2]);
		double val=correlationCoefficient(refData,movData);
		return (val*val);
	}
	//TODO : should be elsewhere, in VitiDialogs for example
	public Point3d[][] getRegistrationLandmarks(int minimumNbWantedPointsPerImage){
		ImagePlus imgRefBis=imgRef.duplicate();		imgRefBis.getProcessor().resetMinAndMax();		imgRefBis.show();		imgRefBis.setTitle("Reference image");
		ImagePlus imgMovBis=imgMov.duplicate();	imgMovBis.getProcessor().resetMinAndMax();		imgMovBis.show();		imgMovBis.setTitle("Moving image");
		Point3d []pRef=new Point3d[1000];
		Point3d []pMov=new Point3d[1000];
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		IJ.showMessage("Examine images and click on reference points");
		boolean finished =false;
		do {
			if(rm.getCount()>=minimumNbWantedPointsPerImage*2 ) finished=true;
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}while (!finished);
		int nbCouples=0;
		for(int indP=0;indP<rm.getCount()/2;indP++){
			pRef[indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			pMov[indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			pRef[indP]=TransformUtils.convertPointToRealSpace(pRef[indP],imgRef);
			pMov[indP]=TransformUtils.convertPointToRealSpace(pMov[indP],imgRef);
			nbCouples++;
		}
		System.out.println("Number of correspondance pairs = "+nbCouples);
		imgRefBis.close();
		imgMovBis.close();
		Point3d []pRefRet=new Point3d[nbCouples];		Point3d []pMovRet=new Point3d[nbCouples];
		for(int i=0;i<pRefRet.length;i++) {
			pRefRet[i]=pRef[i];
			pMovRet[i]=pMov[i];
		}
		return new Point3d[][] {pRefRet,pMovRet};
	}
	
	//TODO : should be elsewhere, in VitiUtils
	double[][]getDoubleArray2DCopy(double[][]in){
		double[][]ret=new double[in.length][];
		for(int i=0;i<in.length;i++) {
			ret[i]=new double[in[i].length];
			for(int j=0;j<in[i].length;j++) {
				ret[i][j]=in[i][j];
			}
		}
		return ret;
	}
	

	
	
	
	
	/**Helper functions to determine initial factors from given data and parameters 
	 * 
	 */
	public int[] subSamplingFactorsAtSuccessiveLevels(int levelMin,int levelMax,boolean doubleIterAtCoarsestLevels,double basis){
		if(levelMax<levelMin)levelMax=levelMin;
		int nbLevels=levelMax-levelMin+1;
		int nbDouble=doubleIterAtCoarsestLevels ? (nbLevels>2 ? nbLevels-2 : 0) : 0;
		int nbTot=nbLevels+nbDouble;
		int []ret=new int[nbTot];
		double []ret2=new double[nbTot];
		for(int i=0;i<2 && i<nbTot;i++) {
			ret[nbTot-1-i]=(int)Math.round(Math.pow(basis,(levelMin-1+i)));
			if(ret[nbTot-1-i]<1) {
				ret[nbTot-1-i]=1;
				ret2[nbTot-1-i]=Math.pow(basis,(levelMin-1+i));
			}
			else ret2[nbTot-1-i]=1;
		}
		if(doubleIterAtCoarsestLevels) {
			for(int i=0;i<(nbTot-2)/2;i++) {
				ret[nbTot-1-(2+i*2)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
				ret[nbTot-1-(2+i*2+1)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
				if(ret[nbTot-1-(2+i*2)]<1) {
					ret2[nbTot-1-(2+i*2)]=Math.pow(basis,(levelMin-1+i+2));
					ret[nbTot-1-(2+i*2)]=1;
				}
				else ret2[nbTot-1-(2+i*2)]=1;
				if(ret[nbTot-1-(2+i*2+1)]<1) {
					ret2[nbTot-1-(2+i*2+1)]=Math.pow(basis,(levelMin-1+i+2));
					ret[nbTot-1-(2+i*2+1)]=1;
				}
				else ret2[nbTot-1-(2+i*2+1)]=1;
			}
		}
		else {
			for(int i=2;i<nbTot;i++) {
				ret[nbTot-1-i]=(int)Math.round(Math.pow(basis,(levelMin-1+i)));
				if(ret[nbTot-1-i]<1) {
					ret2[nbTot-1-i]=ret[nbTot-1-i];
					ret[nbTot-1-i]=1;
				}
				else ret2[nbTot-1-i]=1;
			}

		}		
		return ret;
	}

	public int [][] imageDimsAtSuccessiveLevels(int []dimsImg,int []subFactors,boolean noSubScaleZ) {		
		if (subFactors==null)return null;
		int n=subFactors.length;
		int [][]ret=new int[n][3];
		if(noSubScaleZ)for(int i=0;i<n;i++) {
			for(int j=0;j<2;j++)ret[i][j]=(int)Math.ceil(dimsImg[j]/subFactors[i]);
			ret[i][2]=dimsImg[2];
		}
		else for(int i=0;i<n;i++) for(int j=0;j<3;j++)ret[i][j]=(int)Math.ceil(dimsImg[j]/subFactors[i]);
		return ret;
	}

	public double [][] imageVoxSizesAtSuccessiveLevels(double []voxSizes,int []subFactors) {		
		if (subFactors==null)return null;
		int n=subFactors.length;
		double [][]ret=new double[n][3];
		if(noSubScaleZ) {
			for(int i=0;i<n;i++) for(int j=0;j<2;j++)ret[i][j]=voxSizes[j]*subFactors[i];
			for(int i=0;i<n;i++) for(int j=2;j<3;j++)ret[i][j]=voxSizes[j];
		}
		else for(int i=0;i<n;i++) for(int j=0;j<3;j++)ret[i][j]=voxSizes[j]*subFactors[i];
		return ret;
	}

	public double[] sigmaDenseFieldAtSuccessiveLevels(int[]subFactors,double rapportSigma) {
		if (subFactors==null)return null;
		double[]ret=new double[subFactors.length];
//		for(int i=0;i<subFactors.length;i++)ret[i]=subFactors[i]*rapportSigma;
		for(int i=0;i<subFactors.length;i++)ret[i]=rapportSigma;
		return ret;
	}
	
	public double[] sigmaFactorsAtSuccessiveLevels(double []voxSize,int[]subFactors,double rapportSigma) {
		if (subFactors==null)return null;
		double voxSizeInit=Math.min(Math.min(voxSize[0],voxSize[1]),voxSize[2]);
		double[]ret=new double[subFactors.length];
		for(int i=0;i<subFactors.length;i++)ret[i]=voxSizeInit*subFactors[i]*rapportSigma;
		return ret;
	}

	public double[] successiveStepFactorsAtSuccessiveLevels(int levelMin,int levelMax,boolean doubleIterAtCoarsestLevels,double basis){
		if(levelMax<levelMin)levelMax=levelMin;
		int nbLevels=levelMax-levelMin+1;
		int nbDouble=doubleIterAtCoarsestLevels ? (nbLevels>2 ? nbLevels-2 : 0) : 0;
		int nbTot=nbLevels+nbDouble;
		int []ret=new int[nbTot];
		double []ret2=new double[nbTot];
		for(int i=0;i<2 && i<nbTot;i++) {
			
			ret[nbTot-1-i]=(int)Math.round(-0.0001+Math.pow(basis,(levelMin-1+i)));//0.0001 is in order that 0.5 is not rounded to 1
			if(ret[nbTot-1-i]<1) {
				ret[nbTot-1-i]=1;
				ret2[nbTot-1-i]=Math.pow(basis,(levelMin-1+i));
			}
			else ret2[nbTot-1-i]=1;

		//	handleOutput("Eval "+i+"ret2["+(nbTot-1-i)+"]="+ret2[nbTot-1-i]+", car "+Math.pow(basis,(levelMin-1+i)));
		}
		if(doubleIterAtCoarsestLevels) {
			for(int i=0;i<(nbTot-2)/2;i++) {
				ret[nbTot-1-(2+i*2)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
				ret[nbTot-1-(2+i*2+1)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
				if(ret[nbTot-1-(2+i*2)]<1) {
					ret2[nbTot-1-(2+i*2)]=Math.pow(basis,(levelMin-1+i+2));
					ret[nbTot-1-(2+i*2)]=1;
				}
				else ret2[nbTot-1-(2+i*2)]=1;
				if(ret[nbTot-1-(2+i*2+1)]<1) {
					ret2[nbTot-1-(2+i*2+1)]=Math.pow(basis,(levelMin-1+i+2));
					ret[nbTot-1-(2+i*2+1)]=1;
				}
				else ret2[nbTot-1-(2+i*2+1)]=1;
		//		handleOutput("CoarEval "+i+"ret2["+(nbTot-1-(2+i*2))+"]="+ret2[nbTot-1-(2+i*2)] );
			//	handleOutput("CoarEval "+i+"ret2["+(nbTot-1-(2+i*2+1))+"]="+ret2[nbTot-1-(2+i*2+1)] );
			}
		}
		else {
			for(int i=2;i<nbTot;i++) {
				ret[nbTot-1-i]=(int)Math.round(Math.pow(basis,(levelMin-1+i)));
				if(ret[nbTot-1-i]<1) {
					ret2[nbTot-1-i]=ret[nbTot-1-i];
					ret[nbTot-1-i]=1;
				}
				else ret2[nbTot-1-i]=1;
			//	handleOutput("CoarEval "+i+"ret2["+(nbTot-1-i)+"]="+ret2[nbTot-1-i] );

			}

		}		
		return ret2;
	}

	
	



	/** Common test scenarios, and command-line calls
	 * 
	 */
	public static ItkTransform setupAndRunRoughBlockMatchingWithoutFineParameterization(
			ImagePlus imgRef,ImagePlus imgMov,ImagePlus imgMask,Transform3DType transType,MetricType metr,
			int levelMax,int levelMin,int blockSize,int neighSize,double varMin,double sigma,int duration,boolean displayRegistration,boolean displayR2,int percentageScoreKeep,boolean correspondanceUserDefined)
	{
	double sigmaSmoothingInPixels=0.0;
	double denseFieldSigmaInMM=sigma;
	double[]voxS=VitimageUtils.getVoxelSizes(imgRef);
	int nbIterations=5+3*duration;
	int viewSlice=imgRef.getStackSize()/2;
	int neighXY=neighSize;
	int neighZ=(int)Math.round(Math.max(1,neighXY*voxS[0]/voxS[2]));
	int bSXY=blockSize;
	int bSZ=(int)Math.round(Math.max(1,bSXY*voxS[0]/voxS[2]));
	int strideXY=bSXY;
	int strideZ=bSZ;
	ItkTransform transRet=null;
	BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,transType,metr,
				sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMin,levelMax,nbIterations,
				viewSlice,imgMask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
	ItkTransform trInit=null;
	if(correspondanceUserDefined) {
		trInit=new ItkTransform(new DisplacementFieldTransform(ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(VitiDialogs.registrationPointsUI(50, imgRef, imgMov, true),imgRef,denseFieldSigmaInMM,true)));			
	}
	
	bmRegistration.percentageBlocksSelectedByScore=percentageScoreKeep;
	bmRegistration.minBlockVariance=varMin;
	bmRegistration.displayRegistration=displayRegistration ? 2 : 0;
	bmRegistration.displayR2=displayR2;
	transRet=bmRegistration.runBlockMatching(trInit);
	
	if(transType==Transform3DType.DENSE ) transRet=transRet.flattenDenseField(imgRef);
	else transRet=transRet.simplify();
	if(displayRegistration) {
		bmRegistration.computeSummaries();		
		bmRegistration.temporarySummariesSave();
		bmRegistration.closeLastImages();
	}
	bmRegistration.freeMemory();
	return transRet;
	}
	
	public static ItkTransform testSetupAndRunStandardBlockMatchingWithoutFineParameterization(ImagePlus imgRef,ImagePlus imgMov,ImagePlus mask,boolean doRigidPart,boolean doDensePart,boolean doCoarserRigid,boolean doCoarserDense,boolean doThinnerRigid,boolean doThinnerDense) {
	double sigmaSmoothingInPixels=0.0;
	double denseFieldSigmaInMM=1.2;
	int levelMinRig=doThinnerRigid ? -1 : 0;
	int levelMaxRig=doCoarserRigid ? 2 : 1;
	int levelMinDen=doThinnerDense ? 0 : 1;
	int levelMaxDen=doCoarserDense ? 2 : 1;
	int viewSlice=imgRef.getStackSize()/2;
	int nbIterations=8;
	int nbIterationsRig=8;
	int neighXY=2;
	int neighZ=1;
	int bSXY=7;
	int bSZ=1;
	int strideXY=3;
	int strideZ=1;
	ItkTransform transRet=null;
	if(doRigidPart) {
		BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.VERSOR,MetricType.SQUARED_CORRELATION,
				sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinRig,levelMaxRig,nbIterationsRig,
				viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
		transRet=bmRegistration.runBlockMatching(null);
		if( !doDensePart) {
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
		else {
			bmRegistration.transformationType=Transform3DType.DENSE;
			bmRegistration.levelMin=levelMinDen;
			bmRegistration.levelMax=levelMaxDen;
			bmRegistration.nbLevels=bmRegistration.levelMax-bmRegistration.levelMin+1;
			bmRegistration.subScaleFactors=bmRegistration.subSamplingFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
			bmRegistration.successiveStepFactors=bmRegistration.successiveStepFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
			bmRegistration.successiveDimensions=bmRegistration.imageDimsAtSuccessiveLevels(new int[] {imgRef.getWidth(),imgRef.getHeight(),imgRef.getStackSize()},bmRegistration.subScaleFactors,bmRegistration.noSubScaleZ);
			bmRegistration.successiveVoxSizes=bmRegistration.imageVoxSizesAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors);
			bmRegistration.successiveSmoothingSigma=bmRegistration.sigmaFactorsAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors,bmRegistration.smoothingSigmaInPixels); 
			bmRegistration.successiveDenseFieldSigma=bmRegistration.sigmaDenseFieldAtSuccessiveLevels(bmRegistration.subScaleFactors,bmRegistration.denseFieldSigma);
			transRet=bmRegistration.runBlockMatching(transRet);
			transRet=transRet.flattenDenseField(imgRef);
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
	}
	
	else {
		if(doDensePart) {			
			BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
												sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinDen,levelMaxDen,nbIterations,
												viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
			transRet=bmRegistration.runBlockMatching(null);
			transRet=transRet.flattenDenseField(imgRef);
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
		else {
			return null;
		}
	}
	}
	
	public static ItkTransform testSetupAndRunStandardBlockMatchingWithoutFineParameterizationFromRigidStart(ImagePlus imgRef,ImagePlus imgMov,ImagePlus mask,ItkTransform rigidTrans,boolean doDensePart,boolean doCoarserRigid,boolean doCoarserDense,boolean doThinnerRigid,boolean doThinnerDense) {
	double sigmaSmoothingInPixels=0.0;
	double denseFieldSigmaInMM=1.2;
	int levelMinDen=doThinnerDense ? 0 : 1;
	int levelMaxDen=doCoarserDense ? 2 : 1;
	int viewSlice=imgRef.getStackSize()/2;
	int nbIterations=7;
	int nbIterationsRig=8;
	int neighXY=2;
	int neighZ=1;
	int bSXY=7;
	int bSZ=1;
	int strideXY=3;
	int strideZ=1;
	ItkTransform transRet=rigidTrans;
	BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.VERSOR,MetricType.SQUARED_CORRELATION,
			sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinDen,levelMaxDen,nbIterationsRig,
			viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
	bmRegistration.transformationType=Transform3DType.DENSE;
	bmRegistration.levelMin=levelMinDen;
	bmRegistration.levelMax=levelMaxDen;
	bmRegistration.nbLevels=bmRegistration.levelMax-bmRegistration.levelMin+1;
	bmRegistration.subScaleFactors=bmRegistration.subSamplingFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
	bmRegistration.successiveStepFactors=bmRegistration.successiveStepFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
	bmRegistration.successiveDimensions=bmRegistration.imageDimsAtSuccessiveLevels(new int[] {imgRef.getWidth(),imgRef.getHeight(),imgRef.getStackSize()},bmRegistration.subScaleFactors,bmRegistration.noSubScaleZ);
	bmRegistration.successiveVoxSizes=bmRegistration.imageVoxSizesAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors);
	bmRegistration.successiveSmoothingSigma=bmRegistration.sigmaFactorsAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors,bmRegistration.smoothingSigmaInPixels); 
	bmRegistration.successiveDenseFieldSigma=bmRegistration.sigmaDenseFieldAtSuccessiveLevels(bmRegistration.subScaleFactors,bmRegistration.denseFieldSigma);
	bmRegistration.minBlockScore=0.3;///////////////////////////////////////////////////
	transRet=bmRegistration.runBlockMatching(transRet);
	transRet=transRet.flattenDenseField(imgRef);
	bmRegistration.computeSummaries();
	bmRegistration.temporarySummariesSave();
	bmRegistration.closeLastImages();
	bmRegistration.freeMemory();
	return transRet;
	}
	
	public static Object[] testSetupAndRunStandardBlockMatchingWithoutFineParameterizationPhoto2D(ImagePlus imgRef,ImagePlus imgMov,ImagePlus mask,ItkTransform trInit,boolean doCoarser,boolean doThinner,String info) {
	double sigmaSmoothingInPixels=0.0;
	int levelMinRig=doThinner ? 0 : 0;
	int levelMaxRig=doCoarser ? 1 : 1;
	int viewSlice=0;
	int nbIterations=8;
	int neighXY=5;
	int neighZ=0;
	int bSXY=6;
	int bSZ=0;
	int strideXY=2;
	int strideZ=1;
	ItkTransform transRet=null;
	BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.VERSOR,MetricType.SQUARED_CORRELATION,
			sigmaSmoothingInPixels,0,levelMinRig,levelMaxRig,nbIterations,
			viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
	bmRegistration.info=info;
	bmRegistration.displayRegistration=0;
	bmRegistration.flagSingleView=false;
	bmRegistration.minBlockVariance=4;
	bmRegistration.consoleOutputActivated=false;
	transRet=bmRegistration.runBlockMatching(trInit);
	bmRegistration.closeLastImages();
	bmRegistration.freeMemory();
	return (new Object[] {transRet,new Double(bmRegistration.lastValueBlocksCorr)});
	}
	
	public static ItkTransform registerRXOnSlices(ImagePlus imgSlice,ImagePlus imgRX,ItkTransform transRX,int slice,boolean accurate) {
	int levelMin=accurate ? 1 : 2;	
	int levelMax=3;
	int viewSlice=slice;
	int nbIterations=8;//14
	int neighXY=2;	int neighZ=2;
	int bSXY=9;		int bSZ=0;
	int strideXY=3;		int strideZ=1;
	ItkTransform transRet=null;
	BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgSlice,imgRX,Transform3DType.VERSOR,MetricType.SQUARED_CORRELATION,
				0,0,levelMin,levelMax,nbIterations,viewSlice,null,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
	bmRegistration.flagSingleView=true;
	bmRegistration.displayRegistration=0;
	bmRegistration.displayR2=false;
	transRet=bmRegistration.runBlockMatching(transRX);
	bmRegistration.closeLastImages();
	bmRegistration.freeMemory();
	return transRet;
	}
	
	public static ItkTransform registerMRIonRX(ImagePlus imgRefH,ImagePlus imgMovH,ItkTransform transMov,int slice,ImagePlus mask) {
	ImagePlus imgRef=VitimageUtils.Sub222(imgRefH);
	ImagePlus imgMov=VitimageUtils.Sub222(imgMovH);
	int levelMinRig=2;		int levelMax=3;int levelMinDen=2;
	int viewSlice=slice;
	int nbIterations=13;//14
	int neighXY=2;	int neighZ=2;
	int bSXY=5;		int bSZ=5;
	int strideXY=3;		int strideZ=3;
	ItkTransform transRet=null;
	BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.VERSOR,MetricType.SQUARED_CORRELATION,
				0,0,levelMinRig,levelMax,nbIterations,viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
	bmRegistration.flagSingleView=true;
	bmRegistration.displayRegistration=0;
	bmRegistration.displayR2=false;
	transRet=bmRegistration.runBlockMatching(transMov);
	bmRegistration.closeLastImages();
	bmRegistration.freeMemory();
	//bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,ItkTransform.Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
	//		0,50,levelMinDen,levelMax,nbIterations,viewSlice,null,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
				//		bmRegistration.flagSingleView=true;
				//bmRegistration.displayRegistration=true;
				//bmRegistration.displayR2=false;
	//transRet=bmRegistration.runBlockMatching(transRet);
	//bmRegistration.closeLastImages();
	//bmRegistration.freeMemory();
	return transRet;
	}
	
	public static ItkTransform blockMatchingRegistrationCepMRIData(ImagePlus imgRef,ImagePlus imgMov,ImagePlus mask,boolean doRigidPart,boolean doDensePart,boolean doCoarserRigid,boolean doCoarserDense,double sigma) {
	double sigmaSmoothingInPixels=0.0;
	double denseFieldSigmaInMM=sigma;
	int levelMinRig=1;
	int levelMaxRig=doCoarserRigid ? 3 : 1;
	int levelMinDen=1;
	int levelMaxDen=doCoarserDense ? 2 : 1;
	int viewSlice=60;//imgRef.getStackSize()/2;
	int nbIterations=7;//14
	int nbIterationsRig=10;//14
	int neighXY=2;
	int neighZ=2;
	int bSXY=9;
	int bSZ=9;
	int strideXY=3;
	int strideZ=1;
	ItkTransform transRet=null;
	if(doRigidPart) {
		BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.VERSOR,MetricType.SQUARED_CORRELATION,
				sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinRig,levelMaxRig,nbIterationsRig,
				viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
		bmRegistration.flagSingleView=true;
		transRet=bmRegistration.runBlockMatching(null);
		if( !doDensePart) {
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
		else {
			bmRegistration.transformationType=Transform3DType.DENSE;
			bmRegistration.levelMin=levelMinDen;
			bmRegistration.levelMax=levelMaxDen;
			bmRegistration.nbLevels=bmRegistration.levelMax-bmRegistration.levelMin+1;
			bmRegistration.subScaleFactors=bmRegistration.subSamplingFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
			bmRegistration.successiveStepFactors=bmRegistration.successiveStepFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
			bmRegistration.successiveDimensions=bmRegistration.imageDimsAtSuccessiveLevels(new int[] {imgRef.getWidth(),imgRef.getHeight(),imgRef.getStackSize()},bmRegistration.subScaleFactors,bmRegistration.noSubScaleZ);
			bmRegistration.successiveVoxSizes=bmRegistration.imageVoxSizesAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors);
			bmRegistration.successiveSmoothingSigma=bmRegistration.sigmaFactorsAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors,bmRegistration.smoothingSigmaInPixels); 
			bmRegistration.successiveDenseFieldSigma=bmRegistration.sigmaDenseFieldAtSuccessiveLevels(bmRegistration.subScaleFactors,bmRegistration.denseFieldSigma);
			bmRegistration.flagSingleView=true;
	
			transRet=bmRegistration.runBlockMatching(transRet);
			transRet=transRet.flattenDenseField(imgRef);
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
	}
	
	else {
		if(doDensePart) {			
			BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
												sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinDen,levelMaxDen,nbIterations,
												viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
			bmRegistration.flagSingleView=true;
	
			transRet=bmRegistration.runBlockMatching(null);
			transRet=transRet.flattenDenseField(imgRef);
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
		else {
			return null;
		}
	}
	}
	
	public static ItkTransform setupAndRunStandardBlockMatchingWithoutFineParameterization(ImagePlus imgRef,ImagePlus imgMov,ImagePlus mask,boolean doRigidPart,boolean doDensePart,boolean doCoarserRigid) {
	double sigmaSmoothingInPixels=0.0;
	double denseFieldSigmaInMM=0.9;
	int levelMinRig=1;
	int levelMaxRig=doCoarserRigid ? 2 : 1;
	int levelMinDen=1;
	int levelMaxDen=1;
	int viewSlice=imgRef.getStackSize()/2;
	int nbIterations=5;//14
	int nbIterationsRig=14;//14
	int neighXY=3;
	int neighZ=1;
	int bSXY=9;
	int bSZ=2;
	int strideXY=3;
	int strideZ=1;
	ItkTransform transRet=null;
	if(doRigidPart) {
		BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.VERSOR,MetricType.SQUARED_CORRELATION,
				sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinRig,levelMaxRig,nbIterationsRig,
				viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
		transRet=bmRegistration.runBlockMatching(null);
		if( !doDensePart) {
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
		else {
			bmRegistration.transformationType=Transform3DType.DENSE;
			bmRegistration.levelMin=levelMinDen;
			bmRegistration.levelMax=levelMaxDen;
			bmRegistration.nbLevels=bmRegistration.levelMax-bmRegistration.levelMin+1;
			bmRegistration.subScaleFactors=bmRegistration.subSamplingFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
			bmRegistration.successiveStepFactors=bmRegistration.successiveStepFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
			bmRegistration.successiveDimensions=bmRegistration.imageDimsAtSuccessiveLevels(new int[] {imgRef.getWidth(),imgRef.getHeight(),imgRef.getStackSize()},bmRegistration.subScaleFactors,bmRegistration.noSubScaleZ);
			bmRegistration.successiveVoxSizes=bmRegistration.imageVoxSizesAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors);
			bmRegistration.successiveSmoothingSigma=bmRegistration.sigmaFactorsAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors,bmRegistration.smoothingSigmaInPixels); 
			bmRegistration.successiveDenseFieldSigma=bmRegistration.sigmaDenseFieldAtSuccessiveLevels(bmRegistration.subScaleFactors,bmRegistration.denseFieldSigma);
			transRet=bmRegistration.runBlockMatching(transRet);
			transRet=transRet.flattenDenseField(imgRef);
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
	}
	
	else {
		if(doDensePart) {			
			BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
												sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinDen,levelMaxDen,nbIterations,
												viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
			transRet=bmRegistration.runBlockMatching(null);
			transRet=transRet.flattenDenseField(imgRef);
			bmRegistration.computeSummaries();
			bmRegistration.temporarySummariesSave();
			bmRegistration.closeLastImages();
			bmRegistration.freeMemory();
			return transRet;
		}
		else {
			return null;
		}
	}
	}

	/**Memory free and management
	 * 
	 */
	public void freeMemory() {
		this.resampler=null;
		this.imgRef=null;
		this.imgMov=null;
		for(int i=0;i<currentField.length ;i++)this.currentField[i]=null;
		this.currentField=null;
		this.currentTransform=null;
		this.sliceRef=null;;
		this.sliceMov=null;;
		this.sliceFuse=null;;
		this.sliceGrid=null;;
		this.sliceCorr=null;;
		this.sliceJacobian=null;
		this.summaryArray.clear();
		this.summary=null;
		this.gridSummaryArray.clear();
		this.gridSummary=null;
		this.correspondancesSummaryArray.clear();
		this.correspondancesSummary=null;
		this.jacobianSummaryArray.clear();
		this.jacobianSummary=null;
		this.mask=null;
		System.gc();
	}
	
	
}


/** Comparators used for sorting data when trimming by score, variance or distance to computed transform
 * 
 */
class PointTabComparatorByScore implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((Point3d[]) o1)[2].x)).compareTo(new Double(((Point3d[]) o2)[2].x));
   }
}

class PointTabComparatorByDistanceLTS implements java.util.Comparator {
	   public int compare(Object o1, Object o2) {
		   return (new Double(((Point3d[]) o1)[2].z)).compareTo(new Double(((Point3d[]) o2)[2].z));
	   }
	}

class VarianceComparator implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((double[]) o1)[0])).compareTo(new Double(((double[]) o2)[0]));
   }
}

class ScoreComparator implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((double[][]) o1)[2][0])).compareTo(new Double(((double[][]) o2)[2][0]));
   }
}











