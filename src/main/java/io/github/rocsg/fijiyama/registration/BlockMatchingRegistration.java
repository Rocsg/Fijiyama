/*
 * 
 */
package io.github.rocsg.fijiyama.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.Toolkit;
import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.ResampleImageFilter;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.StackConverter;
import io.github.rocsg.fijiyama.common.ItkImagePlusInterface;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationAction;
import io.github.rocsg.fijiyama.registration.BlockMatchingRegistration;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.MetricType;
import io.github.rocsg.fijiyama.registration.PointTabComparatorByDistanceLTS;
import io.github.rocsg.fijiyama.registration.PointTabComparatorByScore;
import io.github.rocsg.fijiyama.registration.Transform3DType;
import io.github.rocsg.fijiyama.registration.VarianceComparator;
import io.github.rocsg.fijiyama.rsml.*;
import math3d.Point3d;

// TODO: Auto-generated Javadoc
/**
 * The Class BlockMatchingRegistration.
 */
public class BlockMatchingRegistration  implements ItkImagePlusInterface{
	
	/** The old behaviour. */
	public boolean OLD_BEHAVIOUR=false;
	
	/** The return composed transformation including the initial transformation given. */
	public boolean returnComposedTransformationIncludingTheInitialTransformationGiven=true;
	
	/** The view fuse bigger. */
	boolean viewFuseBigger=true;
	
	/** The bm is interrupted succeeded. */
	public volatile boolean bmIsInterruptedSucceeded=false;
	
	/** The threads. */
	public Thread[] threads;
	
	/** The main thread. */
	public Thread mainThread;
	
	/** The timing measurement. */
	public boolean timingMeasurement=false;
	
	/** The debug. */
	boolean debug=false;
	
	/** The wait before start. */
	boolean waitBeforeStart=false;
	
	/** The ref range. */
	private double[]refRange=new double[] {-1,-1};
	
	/** The mov range. */
	private double[]movRange=new double[] {-1,-1};
	
	/** The flag range. */
	public boolean flagRange=false;
	
	/** The bm is interrupted. */
	public volatile boolean bmIsInterrupted;
	
	/** The compute summary. */
	public boolean computeSummary=false;
	
	/** The max iter. */
	int maxIter=1000;
	
	/** The font size. */
	int fontSize=12;
	
	/** The percentage blocks selected by LTS. */
	public int percentageBlocksSelectedByLTS=70;
	
	/** The correspondance provided at start. */
	public Point3d[][] correspondanceProvidedAtStart;
	
	/** The global R 2 values. */
	public double[]globalR2Values=new double[maxIter];
	
	/** The block R 2 values bef. */
	public double[]blockR2ValuesBef=new double[maxIter];
	
	/** The block R 2 values aft. */
	public double[]blockR2ValuesAft=new double[maxIter];
	
	/** The incr iter. */
	public int incrIter=0;
	
	/** The stride moving. */
	public int strideMoving=1;
	
	/** The display registration. */
	public int displayRegistration=1;
	
	/** The display R 2. */
	public boolean displayR2=true;
	
	/** The console output activated. */
	public boolean consoleOutputActivated=true;
	
	/** The image J output activated. */
	public boolean imageJOutputActivated=true;
	
	/** The default core number. */
	public int defaultCoreNumber=12;
	
	/** The resampler. */
	public ResampleImageFilter resampler;
	
	/** The img ref. */
	public ImagePlus imgRef;
	
	/** The img mov. */
	public ImagePlus imgMov;
	
	/** The dense field sigma. */
	public double denseFieldSigma;//in mm
	
	/** The smoothing sigma in pixels. */
	public double smoothingSigmaInPixels;
	
	/** The level min. */
	public int levelMin;
	
	/** The level max. */
	public int levelMax;
	
	/** The no sub scale Z. */
	public boolean noSubScaleZ=true;
	
	/** The percentage blocks selected by variance. */
	public int percentageBlocksSelectedByVariance=50;//15;
	
	/** The min block variance. */
	public double minBlockVariance=0.05;
	
	/** The min block score. */
	public double minBlockScore=0.1;
	
	/** The percentage blocks selected randomly. */
	public int percentageBlocksSelectedRandomly=100;//50;
	
	/** The percentage blocks selected by score. */
	public int percentageBlocksSelectedByScore=80;//95;
	
	/** The block size half X. */
	public int blockSizeHalfX;
	
	/** The block size half Y. */
	public int blockSizeHalfY;
	
	/** The block size half Z. */
	public int blockSizeHalfZ;
	
	/** The block size X. */
	public int blockSizeX;
	
	/** The block size Y. */
	public int blockSizeY;
	
	/** The block size Z. */
	public int blockSizeZ;
	
	/** The blocks stride X. */
	public int blocksStrideX=2;
	
	/** The blocks stride Y. */
	public int blocksStrideY=2;
	
	/** The blocks stride Z. */
	public int blocksStrideZ=2;
	
	/** The neighbourhood size X. */
	public int neighbourhoodSizeX=4;
	
	/** The neighbourhood size Y. */
	public int neighbourhoodSizeY=4;
	
	/** The neighbourhood size Z. */
	public int neighbourhoodSizeZ=1;
	
	/** The img mov default value. */
	public double imgMovDefaultValue;
	
	/** The img ref default value. */
	public double imgRefDefaultValue;
	
	/** The nb levels. */
	public int nbLevels;
	
	/** The nb iterations. */
	public int nbIterations;
	
	/** The current field. */
	Image []currentField;
	
	/** The ind field. */
	int indField;
	
	/** The sub scale factors. */
	public int []subScaleFactors;
	
	/** The successive step factors. */
	public double []successiveStepFactors;
	
	/** The successive dimensions. */
	public int [][]successiveDimensions;
	
	/** The successive vox sizes. */
	public double [][]successiveVoxSizes;
	
	/** The successive smoothing sigma. */
	public double []successiveSmoothingSigma;
	
	/** The successive dense field sigma. */
	public double []successiveDenseFieldSigma;
	
	/** The rejection threshold. */
	public double rejectionThreshold=3;//for outlier rejection : when computing Sigma_X/Y/Z,Mu_X/Y/Z of the transformation coordinates (X,Y,Z) in the neighbourhood (x0+-sigma,y0+-sigma,z0+-sigma),
	
	/** The current transform. */
	//reject the vectors which one at least of their coordinate X (respectively Y,Z) is out of the interval Mu_X (respectively Y,Z)  +- rejectionThreshold*Sigma_X (respectively Y,Z) 
	public ItkTransform currentTransform=new ItkTransform();
	
	/** The transformation type. */
	public Transform3DType transformationType;
	
	/** The metric type. */
	public MetricType metricType;
	
	/** The slice ref. */
	public ImagePlus sliceRef;
	
	/** The slice mov. */
	public ImagePlus sliceMov;
	
	/** The slice fuse. */
	public ImagePlus sliceFuse;
	
	/** The slice grid. */
	public ImagePlus sliceGrid;
	
	/** The slice corr. */
	public ImagePlus sliceCorr;
	
	/** The slice jacobian. */
	public ImagePlus sliceJacobian;
	
	/** The summary. */
	public ImagePlus summary;
	
	/** The grid summary. */
	public ImagePlus gridSummary;
	
	/** The correspondances summary. */
	public ImagePlus correspondancesSummary;
	
	/** The jacobian summary. */
	public ImagePlus jacobianSummary;
	
	/** The slice int. */
	public int sliceInt;
	
	/** The slice int corr. */
	public int sliceIntCorr;
	
	/** The zoom factor. */
	public double zoomFactor=1;
	
	/** The view width. */
	public int viewWidth;
	
	/** The view height. */
	public int viewHeight;
	
	/** The last value corr. */
	public double lastValueCorr=0;
	
	/** The last value blocks corr. */
	public double lastValueBlocksCorr=0;
	
	/** The mask. */
	public ImagePlus mask=null;
	
	/** The flag single view. */
	public boolean flagSingleView=false;
	
	/** The info. */
	private String info="";
	
	/** The Constant EPSILON. */
	private static final double EPSILON=1E-8;
	
	/** The is rsml. */
	public boolean isRsml=false;
	
	/** The root model. */
	private RootModel rootModel;
	
	/**
	 * Sets the single view.
	 */
	public void setSingleView() {flagSingleView=true;}
	
	/**
	 *  Starting points.
	 *
	 * @param imgReff the img reff
	 * @param imgMovv the img movv
	 * @param transformationType the transformation type
	 * @param metricType the metric type
	 * @param smoothingSigmaInPixels the smoothing sigma in pixels
	 * @param denseFieldSigma the dense field sigma
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param nbIterations the nb iterations
	 * @param sliceInt the slice int
	 * @param maskk the maskk
	 * @param neighbourX the neighbour X
	 * @param neighbourY the neighbour Y
	 * @param neighbourZ the neighbour Z
	 * @param blockHalfSizeX the block half size X
	 * @param blockHalfSizeY the block half size Y
	 * @param blockHalfSizeZ the block half size Z
	 * @param strideX the stride X
	 * @param strideY the stride Y
	 * @param strideZ the stride Z
	 * @param displayReg the display reg
	 */
	public BlockMatchingRegistration(ImagePlus imgReff,ImagePlus imgMovv,Transform3DType transformationType,MetricType metricType,
			double smoothingSigmaInPixels, double denseFieldSigma,int levelMin,int levelMax,int nbIterations,int sliceInt,ImagePlus maskk,
			int neighbourX,int neighbourY,int neighbourZ,int blockHalfSizeX,int blockHalfSizeY,int blockHalfSizeZ,int strideX,int strideY,int strideZ,int displayReg) {
		refRange=VitimageUtils.getDoubleSidedRangeForContrastMoreIntelligent(imgReff,1,1,imgReff.getNSlices()/2,99,1.0); 
		movRange=VitimageUtils.getDoubleSidedRangeForContrastMoreIntelligent(imgMovv,1,1,imgReff.getNSlices()/2,99,1.0); 
		if(imgReff.getWidth()<imgReff.getStackSize()*4)noSubScaleZ=false;
		this.displayRegistration=displayReg;
		this.resampler=new ResampleImageFilter();
		this.imgRef=VitimageUtils.imageCopy(imgReff);
		this.imgMov=VitimageUtils.imageCopy(imgMovv);
		int aaa=1;
		int bbb=2;
		
		if(this.imgRef.getType() != 32)imgRef=VitimageUtils.convertToFloat(imgRef);
		if(this.imgMov.getType() != 32)imgMov=VitimageUtils.convertToFloat(imgMov);
		this.metricType=metricType;
		this.transformationType=transformationType;
		this.levelMin=levelMin;
		this.levelMax=levelMax;
		if(this.levelMin>this.levelMax)this.levelMin=this.levelMax;
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
		this.imgMovDefaultValue=(VitimageUtils.caracterizeBackgroundOfImage(imgMov))[0];
		this.imgRefDefaultValue=(VitimageUtils.caracterizeBackgroundOfImage(imgRef))[0];
		int[]dims=VitimageUtils.getDimensions(imgRef);
		this.fontSize=Math.min(dims[0]/40, dims[1]/40);
		if(this.fontSize<5)this.fontSize=0;
		this.minBlockVariance=VitimageUtils.statistics1D(VitimageUtils.valuesOfBlock(imgRef, 0, 0, dims[2]/2, dims[0]-1, dims[1]-1, dims[2]/2))[0]/10.0;
		this.sliceInt=sliceInt;
		this.sliceIntCorr=sliceInt;
		this.mask=maskk;
		this.blockSizeHalfX=blockHalfSizeX;
		this.blockSizeHalfY=blockHalfSizeY;
		this.blockSizeHalfZ=blockHalfSizeZ;
		this.blockSizeX=2*this.blockSizeHalfX+1;
		this.blockSizeY=2*this.blockSizeHalfY+1;
		this.blockSizeZ=2*this.blockSizeHalfZ+1;
		indField=0;
		currentField=new Image[nbLevels*nbIterations];
		this.neighbourhoodSizeX=neighbourX;
		this.neighbourhoodSizeY=neighbourY;
		this.neighbourhoodSizeZ=neighbourZ;
		this.blocksStrideX=strideX;
		this.blocksStrideY=strideY;
		this.blocksStrideZ=strideZ;
		int screenHeight=Toolkit.getDefaultToolkit().getScreenSize().height;
		int screenWidth=Toolkit.getDefaultToolkit().getScreenSize().width;
		zoomFactor=  (dims[0]<280 ? 0.8 : 1)*Math.min((screenHeight/2)/dims[1]  ,  (screenWidth/2)/dims[0]) ; 
		this.viewHeight=(int)(this.imgRef.getHeight()*zoomFactor);
		this.viewWidth=(int)(this.imgRef.getWidth()*zoomFactor);
		
	}
		
	/**
	 * Instantiates a new block matching registration.
	 */
	public BlockMatchingRegistration() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Setup block matching registration.
	 *
	 * @param imgRef the img ref
	 * @param imgMov the img mov
	 * @param regAct the reg act
	 * @return the block matching registration
	 */
	public static BlockMatchingRegistration setupBlockMatchingRegistration(ImagePlus imgRef,ImagePlus imgMov,RegistrationAction regAct) {
		return new BlockMatchingRegistration(imgRef,imgMov,regAct.typeTrans,MetricType.SQUARED_CORRELATION,
				regAct.sigmaResampling,regAct.sigmaDense , regAct.higherAcc==1 ? -1 : (regAct.typeTrans==Transform3DType.DENSE ? regAct.levelMinDense : regAct.levelMinLinear),
				regAct.typeTrans==Transform3DType.DENSE ? regAct.levelMaxDense : regAct.levelMaxLinear,regAct.typeTrans==Transform3DType.DENSE ? regAct.iterationsBMDen : regAct.iterationsBMLin,
				imgRef.getStackSize()/2,null  ,			regAct.neighX,regAct.neighY,regAct.neighZ,			regAct.bhsX,regAct.bhsY,regAct.bhsZ, 			regAct.strideX,regAct.strideY,regAct.strideZ,regAct.typeAutoDisplay);
	}


	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
//		setupAndRunRsmlBlockMatchingRegistration("/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Train/20200826-AC-PIP_azote_Seq 6_Boite 00005_IdentificationFailed-Visu.jpg");
		String dir="/home/rfernandez/Bureau/DATA/Data_Morgan/myData/train/";
		String img="20200826-AC-PIP_azote_Seq 4_Boite 00109_IdentificationFailed-Visu.jpg";
		setupAndRunRsmlBlockMatchingRegistration(dir+img,true,true);
	}
	
	/**
	 * Setup and run rsml block matching registration.
	 *
	 * @param pathToImgRef the path to img ref
	 * @param display the display
	 * @param multiRsml the multi rsml
	 * @return the root model
	 */
	public static RootModel setupAndRunRsmlBlockMatchingRegistration(String pathToImgRef,boolean display,boolean multiRsml) {
		ImagePlus imgRef=IJ.openImage(pathToImgRef);
		RootModel rootModel = new RootModel(VitimageUtils.withoutExtension(pathToImgRef)+".rsml");
		rootModel.refineDescription(10);
		rootModel.attachLatToPrime();
		ImagePlus imgMov=plongement(imgRef,rootModel,false);
	    RegistrationAction regAct=RegistrationAction.defineSettingsForRSML(imgRef);    
	    BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef,imgMov,regAct);
	    if(!display)bm.imageJOutputActivated=false;
	    //bm.waitBeforeStart=false;
	    // bm.updateViews(0, 0, 0, "Start");
	    bm.displayRegistration=display ? 2 : 0;
	    bm.minBlockVariance=0.05;
	    bm.minBlockScore=0.01;
	    bm.isRsml=true;
	    bm.rootModel=rootModel;
	    bm.adjustZoomFactor(512.0/imgRef.getWidth());
	    bm.defaultCoreNumber=multiRsml ? 1 : VitimageUtils.getNbCores()/2;
	    bm.runBlockMatching(null, false);
	    bm.closeLastImages();
	    bm.freeMemory();
	    RootModel rt=bm.rootModel;
	    bm=null;
	    imgRef=null;
	    imgMov=null;
	    regAct=null;
	    return rt ;
	}


	
	
	
	
	
	/**
	 *  Run algorithm from an initial situation (trInit), and return the final transform (including trInit).
	 *
	 * @param trInit the tr init
	 * @param stressTest the stress test
	 * @return the itk transform
	 */
	@SuppressWarnings("unchecked")
	public ItkTransform runBlockMatching(ItkTransform trInit,boolean stressTest) {
		double[]timesGlob=new double[20];
		double[][]timesLev=new double[nbLevels][20];
		double[][][]timesIter=new double[nbLevels][nbIterations][20];
		long t0= System.currentTimeMillis();
		timesGlob[0]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
		handleOutput("Absolute time at start="+t0);
//		double progress=0;IJ.showProgress(progress);
		if(trInit==null)this.currentTransform=new ItkTransform();
		else this.currentTransform=new ItkTransform(trInit);
		handleOutput(new Date().toString());
		//Initialize various artifacts
		ImagePlus imgRefTemp=null;
		ImagePlus imgMovTemp=null;
		if(stressTest) {
			handleOutput("BlockMatching preparation stress test");
		}
		else {
			handleOutput("Standard blockMatching preparation");
		}
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
		if(waitBeforeStart)VitimageUtils.waitFor(20000);
		timesGlob[1]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
		int nbProc=this.defaultCoreNumber;
		double timeFactor=0.000000003;
		ImagePlus imgMaskTemp=null;
//		progress=0.05;IJ.showProgress(progress);

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
			
			int levelStrideX=(int)Math.round(Math.max(1, -EPSILON+this.blocksStrideX/Math.pow(subSamplingFactors[0],1.0/3)));
			int levelStrideY=(int)Math.round(Math.max(1, -EPSILON+this.blocksStrideY/Math.pow(subSamplingFactors[1],1.0/3)));
			int levelStrideZ=(int)Math.round(Math.max(1, -EPSILON+this.blocksStrideZ/Math.pow(subSamplingFactors[2],1.0/3)));
			handleOutput("--> Level "+(lev+1)+"/"+nbLevels+" . Dims=("+curDims[0]+"x"+curDims[1]+"x"+curDims[2]+
					"), search step factors =("+stepFactorX+","+stepFactorY+","+stepFactorZ+")"+" pixels."+
					" Subsample factors="+subSamplingFactors[0]+","+subSamplingFactors[1]+","+subSamplingFactors[2]+" Stride="+levelStrideX+","+levelStrideY+","+levelStrideZ);

			// blocks from fixed
			int nbBlocksX=1+(curDims[0]-this.blockSizeX-2*this.neighbourhoodSizeX*strideMoving)/levelStrideX;
			int nbBlocksY=1+(curDims[1]-this.blockSizeY-2*this.neighbourhoodSizeY*strideMoving)/levelStrideY;
			int nbBlocksZ=1;
			if(curDims[2]>1)nbBlocksZ=1+(curDims[2]-this.blockSizeZ-2*this.neighbourhoodSizeZ*strideMoving)/levelStrideZ;
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
				this.resampler.setDefaultPixelValue(1);
				imgMaskTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(this.mask)));
			}
			timesLev[lev][3]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

		
			//for each iteration
			System.out.println("ImageType");
			for(int iter=0;iter<nbIterations;iter++) {
				IJ.showProgress((nbIterations*lev+iter)/(1.0*nbIterations*nbLevels));
				timesLev[lev][4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				timesIter[lev][iter][0]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
//				progress=0.1+0.9*(lev*1.0/nbLevels+(iter*1.0/nbIterations)*1.0/nbLevels);IJ.showProgress(progress);
				handleOutput("\n   --> Iteration "+(iter+1) +"/"+this.nbIterations);
				
				this.resampler.setTransform(this.currentTransform);
				this.resampler.setDefaultPixelValue(this.imgMovDefaultValue);

				if(this.isRsml)imgMovTemp=plongement(this.imgRef,this.rootModel,false);
				else {
					imgMovTemp=VitimageUtils.gaussianFilteringIJ(this.imgMov, this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev]);
					imgMovTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgMovTemp)));
				}
				timesIter[lev][iter][1]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

				//Prepare a coordinate summary tabs for this blocks, compute and store their sigma
				int indexTab=0;
				nbBlocksTotal=nbBlocksX*nbBlocksY*nbBlocksZ;
				if(nbBlocksTotal<0) {
					IJ.showMessage("Bad parameters. Nb blocks=0. nbBlocksX="+nbBlocksX+" , nbBlocksY="+nbBlocksY+" , nbBlocksZ="+nbBlocksZ);
					if(this.returnComposedTransformationIncludingTheInitialTransformationGiven) return this.currentTransform;
					else return new ItkTransform();
				}
				double[][] blocksRefTmp=new double[nbBlocksTotal][4];
				handleOutput("       # blocks before trimming="+nbBlocksTotal);
				
				
				
				timesIter[lev][iter][2]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				for(int blX=0;blX<nbBlocksX ;blX++) {
					for(int blY=0;blY<nbBlocksY ;blY++) {
						for(int blZ=0;blZ<nbBlocksZ ;blZ++) {
							double[]valsBlock=VitimageUtils.valuesOfBlock(imgRefTemp,
									blX*levelStrideX+this.neighbourhoodSizeX*strideMoving,                blY*levelStrideY+this.neighbourhoodSizeY*strideMoving,                blZ*levelStrideZ+this.neighbourhoodSizeZ*strideMoving,
									blX*levelStrideX+this.blockSizeX+this.neighbourhoodSizeX*strideMoving-1,blY*levelStrideY+this.blockSizeY+this.neighbourhoodSizeY*strideMoving-1,blZ*levelStrideZ+this.blockSizeZ+this.neighbourhoodSizeZ*strideMoving-1);
							double[]stats=VitimageUtils.statistics1D(valsBlock);
							blocksRefTmp[indexTab++]=new double[] {stats[1],blX*levelStrideX+this.neighbourhoodSizeX*strideMoving,       blY*levelStrideY+this.neighbourhoodSizeY*strideMoving,     blZ*levelStrideZ+this.neighbourhoodSizeZ*strideMoving};
						}
					}
				}
				timesIter[lev][iter][3]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

	
				
				double[][] blocksRef;
				if(OLD_BEHAVIOUR) {
					//Sort by variance
					//double meanVar=0;
					//for(int i=0;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
					Arrays.sort(blocksRefTmp,new VarianceComparator());
					timesIter[lev][iter][4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					
					//Keep this.percentageBlocksSelectedByVariance
					int lastRemoval=(nbBlocksTotal*(100-this.percentageBlocksSelectedByVariance))/100;
					for(int bl=0;bl<lastRemoval;bl++)blocksRefTmp[bl][0]=-1;
					//meanVar=0;
					//for(int i=lastRemoval;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
					handleOutput("Sorting blocks using variance : eliminating blocks from 0 to  "+lastRemoval+" / "+blocksRefTmp.length);
					nbBlocksTotal=0;
					for(int bl=0;bl<blocksRefTmp.length;bl++)if(blocksRefTmp[bl][0]>=this.minBlockVariance)nbBlocksTotal++;
					blocksRef=new double[nbBlocksTotal][4];
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
					if(this.mask !=null)blocksRef=this.trimUsingMaskOLD(blocksRef,imgMaskTemp,bSX,bSY,bSZ);
					this.correspondanceProvidedAtStart=null;
					nbBlocksTotal=blocksRef.length;
				}
				else {
					//Trim the ones outside the mask
					if(this.mask !=null)blocksRefTmp=this.trimUsingMaskNEW(blocksRefTmp,imgMaskTemp,bSX,bSY,bSZ);
					nbBlocksTotal=blocksRefTmp.length;
					//Sort by variance
					//double meanVar=0;
					//for(int i=0;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
					Arrays.sort(blocksRefTmp,new VarianceComparator());
					timesIter[lev][iter][4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					
					//Keep this.percentageBlocksSelectedByVariance
					int lastRemoval=(nbBlocksTotal*(100-this.percentageBlocksSelectedByVariance))/100;
					for(int bl=0;bl<lastRemoval;bl++)blocksRefTmp[bl][0]=-1;
					//meanVar=0;
					//for(int i=lastRemoval;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
					handleOutput("Sorting blocks using variance : eliminating blocks from 0 to  "+lastRemoval+" / "+blocksRefTmp.length);
					nbBlocksTotal=0;
					for(int bl=0;bl<blocksRefTmp.length;bl++)if(blocksRefTmp[bl][0]>=this.minBlockVariance)nbBlocksTotal++;
					blocksRef=new double[nbBlocksTotal][4];
					nbBlocksTotal=0;
					for(int bl=0;bl<blocksRefTmp.length;bl++)if(blocksRefTmp[bl][0]>=this.minBlockVariance) {
						blocksRef[nbBlocksTotal][3]=blocksRefTmp[bl][0];
						blocksRef[nbBlocksTotal][0]=blocksRefTmp[bl][1];
						blocksRef[nbBlocksTotal][1]=blocksRefTmp[bl][2];
						blocksRef[nbBlocksTotal++][2]=blocksRefTmp[bl][3];
					}
					handleOutput("       # blocks after trimming="+nbBlocksTotal);
					timesIter[lev][iter][5]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				
				
					this.correspondanceProvidedAtStart=null;
					nbBlocksTotal=blocksRef.length;
				}
				
				
				
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
								if(fixBl==0 && numThread==0) {
									
								}
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
//								if(nbTotalBlock>1000 && (numBl%(nbTotalBlock/20)==0))handleOutputNoNewline((" "+((numBl*100)/nbTotalBlock)+"%"+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)));
								if(nbTotalBlock>1000 && (numBl%(nbTotalBlock/10)==0))handleOutput((" "+((numBl*100)/nbTotalBlock)+"%"+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)));
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
													handleOutput("Corr="+correlationCoefficient(valsFixedBlock, valsMovingBlock));
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
							if(numThread==0)handleOutput("Sorting blocks using correspondance score. Threshold= "+minBS+" . Nb blocks before="+nbProc*correspondancesThread.length+" and after="+nbProc*nbKeep);
	
							correspondances[numThread]=correspondancesThread2; 
						} catch(Exception ie) {}
						} 
						@SuppressWarnings("unused")
						public void cancel() {interrupt();}
					};  		
				}				
				timesIter[lev][iter][7]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				if(stressTest) {
					System.out.println("Stress test area");
					VitimageUtils.startNoJoin(threads);
					VitimageUtils.waitFor(10000);
					for(int th=0;th<this.threads.length;th++)this.threads[th].interrupt();     
					VitimageUtils.waitFor(200);
					bmIsInterrupted=true;
					handleOutput("Stress test passed.\n");
					System.out.println("Out from stress test area");
				}
				else VitimageUtils.startAndJoin(threads);
				
				if(bmIsInterrupted) {
					System.out.println("BM Is INt zone");
					bmIsInterruptedSucceeded=true;
					VitimageUtils.waitFor(200);
					int nbAlive=1;
					while(nbAlive>0) {
						nbAlive=0;
						for(int th=0;th<this.threads.length;th++)if(this.threads[th].isAlive())nbAlive++;
						handleOutput("Trying to stop blockmatching. There is still "+nbAlive+" threads running over "+this.threads.length);
					}
					this.closeLastImages();
					this.freeMemory();					
					System.out.println("UNTIL THERE");
					return null;
				}
				for(int i=0;i<threads.length;i++)threads[i]=null;
				threads=null;
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
						case TRANSLATION:transEstimated=ItkTransform.estimateBestTranslation3D(correspondancePoints[1],correspondancePoints[0]);break;
						default:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
					}
					timesIter[lev][iter][11]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					if(correspondancePoints[1].length<5) {
						handleOutput("Warning : less than 5 correspondance points. Setting up identity transform in replacement");
						transEstimated=new ItkTransform();
					}
					
					else {
						ret=getCorrespondanceListAsTrimmedPointArray(listCorrespondances,this.successiveVoxSizes[lev],100,this.percentageBlocksSelectedByLTS,transEstimated);
						timesIter[lev][iter][12]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
						correspondancePoints=(Point3d[][])ret[0];
						listCorrespondances=(ArrayList<double[][]>) ret[2];
						this.lastValueBlocksCorr=(Double)ret[1];
						int nbPts3=listCorrespondances.size();
						handleOutput("Nb pairs : "+nbPts1 +" , after score selection : "+nbPts2+" , after LTS selection : "+nbPts3);
				
						transEstimated=null;
						switch(this.transformationType) {
							case VERSOR:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
							case AFFINE:transEstimated=ItkTransform.estimateBestAffine3D(correspondancePoints[1],correspondancePoints[0]);break;
							case SIMILARITY:transEstimated=ItkTransform.estimateBestSimilarity3D(correspondancePoints[1],correspondancePoints[0]);break;
							case TRANSLATION:transEstimated=ItkTransform.estimateBestTranslation3D(correspondancePoints[1],correspondancePoints[0]);break;
							default:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
						}
						timesIter[lev][iter][13]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
						
						if(displayRegistration==2) {
							Object [] obj=VitimageUtils.getCorrespondanceListAsImagePlus(imgRef,listCorrespondances,curVoxSizes,this.sliceInt,						
								levelStrideX*subSamplingFactors[0],levelStrideY*subSamplingFactors[1],levelStrideZ*subSamplingFactors[2],
								blockSizeHalfX*subSamplingFactors[0],blockSizeHalfY*subSamplingFactors[1],blockSizeHalfZ*subSamplingFactors[2],false);
							this.correspondancesSummary=(ImagePlus) obj[0];
							this.sliceIntCorr=1+(int) obj[1];	
						}
						timesIter[lev][iter][14]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					}
					//Finally, add it to the current stack of transformations
					handleOutput("  Update to transform = \n"+transEstimated.drawableString());
					if(!transEstimated.isIdentityAffineTransform(1E-6,0.05*Math.min(Math.min(voxSX,voxSY),voxSZ) ) ){
						this.currentTransform.addTransform(new ItkTransform(transEstimated));
						//this.additionalTransform.addTransform(new ItkTransform(transEstimated));
						handleOutput("Global transform after this step =\n"+this.currentTransform.drawableString());
					}
					else {
						handleOutput("Last transformation computed was identity. Convergence seems to be attained. Going to next level");
						iter=nbIterations;
						continue;
					}
					timesIter[lev][iter][15]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
				}
				
				else {
					handleOutput("       Field interpolation from "+correspondancePoints[0].length+" correspondances with sigma="+this.successiveDenseFieldSigma[lev]+" "+imgRefTemp.getCalibration().getUnit()+
							" ( "+((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSX)))+" voxSX , " +((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSY)))+" voxSY , " +((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSZ)))+" voxSZ )"  );
					
					//compute the field
					this.currentField[this.indField++]=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(correspondancePoints,imgRefTemp,this.successiveDenseFieldSigma[lev],false);
					timesIter[lev][iter][11]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					timesIter[lev][iter][12]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					timesIter[lev][iter][13]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					timesIter[lev][iter][14]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					
					//Finally, add it to the current stack of transformations
					if(this.isRsml) {
						//System.out.println("YE"+(this.indField-1)+"\n"+this.currentField[this.indField-1]);
						
						ItkTransform tr=new ItkTransform(new DisplacementFieldTransform( new Image(this.currentField[this.indField-1])));
						System.out.println("Mean distance after trans="+tr.meanDistanceAfterTrans(imgRefTemp,100,100,1,true)[0]);
						rootModel.applyTransformToGeometry(tr);
						this.currentTransform.addTransform(tr);
					}
					else{
						this.currentTransform.addTransform(new ItkTransform(new DisplacementFieldTransform( this.currentField[this.indField-1])));
					}
					timesIter[lev][iter][15]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);
					
				}
				if(displayRegistration==2) {
					Object [] obj=VitimageUtils.getCorrespondanceListAsImagePlus(imgRef,listCorrespondances,curVoxSizes,this.sliceInt,						
						levelStrideX*subSamplingFactors[0],levelStrideY*subSamplingFactors[1],levelStrideZ*subSamplingFactors[2],
						blockSizeHalfX*subSamplingFactors[0],blockSizeHalfY*subSamplingFactors[1],blockSizeHalfZ*subSamplingFactors[2],false);
					this.correspondancesSummary=(ImagePlus) obj[0];
					
					this.sliceIntCorr=1+(int) obj[1];	
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
		handleOutput("Block matching finished, date="+new Date().toString());
		if(this.transformationType!=Transform3DType.DENSE)handleOutput("\nMatrice finale block matching : \n"+this.currentTransform.drawableString());
		
		if(displayR2) {
			handleOutput("Successive R2 values :");
			for(int i=0;i<incrIter;i++)handleOutput(" -> "+globalR2Values[i]);
		}
		timesGlob[4]=VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0);

		
		
		if(this.timingMeasurement) {
			handleOutput("\n\n\n\n\n###################################################\n\nDebrief timing");
			handleOutput("Parametres : ");
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
			handleOutput("\n\n");
			handleOutput("Times globaux : start="+timesGlob[0]+"  fin update view="+timesGlob[1]+"  fin prepa="+timesGlob[2]+"  fin levels="+timesGlob[3]+"  fin return="+timesGlob[3] );
			for(int lev=0;lev<this.nbLevels;lev++) {
				handleOutput("    Times level "+lev+" : start="+timesLev[lev][0]+"  fin gaussRef="+timesLev[lev][1]+"  fin transRef="+timesLev[lev][2]+"  fin prepa3="+timesLev[lev][3]+"fin iters="+timesLev[lev][4] );
				
			}
			
			handleOutput("Summary computation times for Block matching");
			double d=0;double dSum=0;
			d+=(timesGlob[1]-timesGlob[0]);
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][17]-timesIter[lev][it][14]);
			handleOutput("time used for view updating (s)="+VitimageUtils.dou(d));

			d=0;
			for(int lev=0;lev<this.nbLevels;lev++)			d+=(timesLev[lev][2]-timesLev[lev][1]);
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][1]-timesIter[lev][it][0]);
			handleOutput("time used for resampling reference (one time), and moving (at each iteration) (s)="+VitimageUtils.dou(d));dSum+=d;
	
			d=0;
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][3]-timesIter[lev][it][2]);
			handleOutput("time used to compute blocks variances (s)="+VitimageUtils.dou(d));dSum+=d;

			d=0;
			for(int lev=0;lev<this.nbLevels;lev++) 				for(int it=0;it<this.nbIterations;it++) 			d+=(timesIter[lev][it][8]-timesIter[lev][it][7]);
			handleOutput("time used to compute correspondences between blocks (s)="+VitimageUtils.dou(d));dSum+=d;
			
			
			d=timesGlob[3]-timesGlob[0]-dSum;
			handleOutput("time used for side events (s) ="+VitimageUtils.dou(d));
			
			handleOutput("Total time (s)="+VitimageUtils.dou(timesGlob[3]-timesGlob[0]));	
		}
		//glob       0               1            2               3
//		  st    prep         levels          return   
//		timesLev     0            1           2            3          4             5                6                7              8                9                10                 11 
//				     st   prep        tr ref      iters       

		//			timesIter     0            1           2            3          4             5                6                7              8                9                10                 11 
		//                        st   tr mov     maketab      compvar      sortvar     trimvar          prep bm             prejoin       join          buildcorrtab	  trim score
		//                         10                    11                    12                         13                        14                  15          16  
		//                               firstestimate           LTS                 second estimate             correspImage               add              R2
		if(this.returnComposedTransformationIncludingTheInitialTransformationGiven) return this.currentTransform;
		else {
			if(trInit.isDense()) {
				ItkTransform trInv=(trInit.getFlattenDenseField(imgRefTemp).getInverseOfDenseField());
				return new ItkTransform(trInv.addTransform(this.currentTransform));
			}
			else return new ItkTransform(trInit.getInverse().addTransform(this.currentTransform));
		}
	}
	
				
	
	
	
	
	/**
	 *  Helper to estimate computation time, and eventually adjust parameters before launch.
	 *
	 * @param dims the dims
	 * @param viewRegistrationLevel the view registration level
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param nbIter the nb iter
	 * @param transformType the transform type
	 * @param blockHalfSizes the block half sizes
	 * @param strides the strides
	 * @param neighbourhoods the neighbourhoods
	 * @param percentageScoreSelect the percentage score select
	 * @param percentageVarianceSelect the percentage variance select
	 * @param percentageRandomSelect the percentage random select
	 * @param subsampleZ the subsample Z
	 * @return the double[]
	 */
	public static double []estimateBlocksNumberPerLevel(int[]dims,int viewRegistrationLevel,int levelMin,int levelMax,int nbIter,Transform3DType transformType,													
		int []blockHalfSizes,int []strides,int []neighbourhoods,int percentageScoreSelect,int percentageVarianceSelect,int percentageRandomSelect,boolean subsampleZ) {
		double[]tabNb= new double[levelMax-levelMin+1];
		int []factorDiv;
		double []factorDivStride;
		int []strideLev;
		for(int lev=levelMax;lev>=levelMin;lev--) {
			factorDiv=new int[] {1,1,1};
			factorDivStride=new double[] {1,1,1};
			strideLev=new int[] {strides[0],strides[1],strides[2]};
			if(lev>1) {
				factorDiv=new int[] {(int)Math.round(Math.pow(2, lev-1)),(int)Math.round(Math.pow(2, lev-1)),subsampleZ ? (int)Math.round(Math.pow(2, lev-1)) : 1};
				factorDivStride=new double[] {(Math.pow(factorDiv[0], 1/3.0)),(Math.pow(factorDiv[1], 1/3.0)),(Math.pow(factorDiv[2], 1/3.0))};
				strideLev=new int[] {(int)Math.round(Math.max(1,-EPSILON+strides[0]/factorDivStride[0])),(int)Math.round(Math.max(1,-EPSILON+strides[1]/factorDivStride[1])),(int)Math.round(Math.max(1,-EPSILON+strides[2]/factorDivStride[2]))};
			}
			int nbBlocksX=1;int nbBlocksY=1;int nbBlocksZ=1;			
			if(dims[0]>1) nbBlocksX=(int)Math.round( 1+(dims[0]/factorDiv[0]-blockHalfSizes[0]-2*neighbourhoods[0]*strideLev[0])/strideLev[0]);
			if(dims[1]>1) nbBlocksY=(int)Math.round( 1+(dims[1]/factorDiv[1]-blockHalfSizes[1]-2*neighbourhoods[1]*strideLev[1])/strideLev[1]);
			if(dims[2]>1) nbBlocksZ=(int)Math.round( 1+(dims[2]/factorDiv[2]-blockHalfSizes[2]-2*neighbourhoods[2]*strideLev[2])/strideLev[2]);
			int nbBlocksTotal=nbBlocksX*nbBlocksY*nbBlocksZ;			
			tabNb[levelMax-lev]=nbBlocksTotal*1.0*percentageVarianceSelect/(1.0*1E2);
		}
		return tabNb;
	}

	/**
	 * Estimate registration duration.
	 *
	 * @param dims the dims
	 * @param viewRegistrationLevel the view registration level
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param nIterations the n iterations
	 * @param transformType the transform type
	 * @param blockHalfSizes the block half sizes
	 * @param strides the strides
	 * @param neighbourhoods the neighbourhoods
	 * @param nCpu the n cpu
	 * @param percentageScoreSelect the percentage score select
	 * @param percentageVarianceSelect the percentage variance select
	 * @param percentageRandomSelect the percentage random select
	 * @param subsampleZ the subsample Z
	 * @param higherAccuracy the higher accuracy
	 * @return the double
	 */
	public static double estimateRegistrationDuration(int[]dims,int viewRegistrationLevel,int levelMin,int levelMax,int nIterations,Transform3DType transformType,
													int []blockHalfSizes,int []strides,int []neighbourhoods,int nCpu,int percentageScoreSelect,int percentageVarianceSelect,int percentageRandomSelect,boolean subsampleZ,int higherAccuracy) {
		if(higherAccuracy==1)levelMin=-1;
		int nVoxels=dims[0]*dims[1]*dims[2];
		int nNeighbours=(1+2*neighbourhoods[0])*(1+2*neighbourhoods[1])*(1+2*neighbourhoods[2]);
		int blockSize=(1+2*blockHalfSizes[0])*(1+2*blockHalfSizes[1])*(1+2*blockHalfSizes[2]);
		double []nBlocksPerLevel=estimateBlocksNumberPerLevel(dims,viewRegistrationLevel,levelMin,levelMax,nIterations,
				transformType,blockHalfSizes,strides,neighbourhoods,percentageScoreSelect,percentageVarianceSelect,percentageRandomSelect,subsampleZ);
		int nLevels=nBlocksPerLevel.length;
		double nImgUpdateView=viewRegistrationLevel;
		if(viewRegistrationLevel==2)nImgUpdateView=3.5*0.57;
		//double sumLevels=0;
		double sumLevels2=0;
		double sumNbBlocks=0;
		for(int lev=levelMax;lev>=levelMin;lev--){
			//sumLevels+= (lev<=1) ? 1 : (1.0/lev);
			sumLevels2+= (lev<=1) ? 1 : (1.0/Math.sqrt(lev+1));
			sumNbBlocks+=nBlocksPerLevel[levelMax-lev];
		}
		double alphaTrans=1E-7;
		//double alphaGlob=1E-7;
		//double alphaLev=1E-7;
		//double alphaIter=1E-7;
		//double alphaProdDense=1E-7;
		double alphaComp=300;
		double alphaBm=0.9;
		double alphaVar=0.14;
		double factorTrans=1E-7;
		double factorVarBlocks=2.5E-8;
		double factorCompBlocks=1.6E-8;
		//double factorProdDense=1E-7;
		
		double transTime=alphaTrans+nVoxels*factorTrans; // time for a transformation = constante + factor * N voxels
		double timeUpdatesView=transTime*nImgUpdateView*1.125*(1.9+1.52*nLevels*nIterations); //time for the first update, and one for each iteration. Each updated image lies 4 transformations
		double timeTransRefAndMov=0.625*sumLevels2*transTime*(1+nIterations); // One transfo per level for ref, one transfo per iteration for mov. 
		double timeVariance=nIterations*(alphaVar+0.12*factorVarBlocks*sumNbBlocks*blockSize);
		double timeBlockMatching=(factorCompBlocks*sumNbBlocks*(alphaComp+0.455*blockSize)*nNeighbours*(1.0/nCpu)+alphaBm)*nIterations;
		double timeTransfoProduction=transTime*1.4*(1.9)*(nLevels*nIterations); // production of dense field		
		double timeBonus=timeUpdatesView+timeVariance;
		double totalTime=timeUpdatesView+timeTransRefAndMov+timeVariance+timeBlockMatching+(transformType==Transform3DType.DENSE ? timeTransfoProduction : 0)+timeBonus;
		return totalTime*4;
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
		

	
	
	/**
	 * Sets the ref range.
	 *
	 * @param newRange the new ref range
	 */
	public void setRefRange(double[]newRange) {
		this.refRange=new double[] {newRange[0],newRange[1]};		
	}
	
	/**
	 * Sets the mov range.
	 *
	 * @param newRange the new mov range
	 */
	public void setMovRange(double[]newRange) {
		this.movRange=new double[] {newRange[0],newRange[1]};		
	}

	
	
	/**
	 * 	Helper functions for trimming the correspondences.
	 *
	 * @param list the list
	 * @param voxSizes the vox sizes
	 * @param percentageKeepScore the percentage keep score
	 * @param percentageKeepLTS the percentage keep LTS
	 * @param transform the transform
	 * @return the correspondance list as trimmed point array
	 */
	@SuppressWarnings("unchecked")
	Object[] getCorrespondanceListAsTrimmedPointArray(ArrayList<double[][]>list,double[]voxSizes,int percentageKeepScore,int percentageKeepLTS,ItkTransform transform){
		//Convert voxel space correspondances in real space vectors
		boolean isLTS = (transform!=null);
		int n=list.size();
		int  ind=0;
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
		for(int i=0;i<tabPt.length;i++)		for(int j=0;j<tabPt[0].length	;j++)tabPt[i][j]=tmp[j][i];
		
		//Keep this.percentageBlocksSelectedByVariance
		int lastRemoval=(int)Math.round(tabPt[0].length*((100-(isLTS ? percentageKeepLTS : percentageKeepScore))/100.0));
		Point3d[][]ret=new Point3d[3][tabPt[0].length-lastRemoval];
		ArrayList<double[][]>listRet=new ArrayList<double[][]>();
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
		
	/**
	 * Creates the block props from block list.
	 *
	 * @param blockList the block list
	 * @param nbProc the nb proc
	 * @return the double[][][]
	 */
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

/**
 * Trim using mask NEW.
 *
 * @param tabIn the tab in
 * @param imgMaskAtScale the img mask at scale
 * @param bSX the b SX
 * @param bSY the b SY
 * @param bSZ the b SZ
 * @return the double[][]
 */
/*
  	blocksRef[nbBlocksTotal][3]=blocksRefTmp[bl][0];
	blocksRef[nbBlocksTotal][0]=blocksRefTmp[bl][1];
	blocksRef[nbBlocksTotal][1]=blocksRefTmp[bl][2];
	blocksRef[nbBlocksTotal++][2]=blocksRefTmp[bl][3];
*/
	public double[][] trimUsingMaskNEW(double [][]tabIn,ImagePlus imgMaskAtScale,int bSX,int bSY,int bSZ){
		double epsilon=10E-4;
		int[]isOut=new int[tabIn.length];
		int n=tabIn.length;
		int n0=n;
		for(int i=0;i<tabIn.length;i++) {
			double []vals=VitimageUtils.valuesOfBlock(imgMaskAtScale,	(int)tabIn[i][1], (int)tabIn[i][2], (int)tabIn[i][3], (int)tabIn[i][1]+bSX,  (int)tabIn[i][2]+bSY,  (int)tabIn[i][3]+bSZ);
			for(int j=0;j<vals.length;j++) {
				if(vals[j]<1-epsilon)isOut[i]=1;
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
				ret[n][3]=tabIn[i][3];
				n++;
			}
		}
		handleOutput("    Masking : selected "+n+" over "+n0);
		return ret;
	}
	
	/**
	 * Trim using mask OLD.
	 *
	 * @param tabIn the tab in
	 * @param imgMaskAtScale the img mask at scale
	 * @param bSX the b SX
	 * @param bSY the b SY
	 * @param bSZ the b SZ
	 * @return the double[][]
	 */
	public double[][] trimUsingMaskOLD(double [][]tabIn,ImagePlus imgMaskAtScale,int bSX,int bSY,int bSZ){
		double epsilon=10E-4;
		int[]isOut=new int[tabIn.length];
		int n=tabIn.length;
		int n0=n;
		for(int i=0;i<tabIn.length;i++) {
			double []vals=VitimageUtils.valuesOfBlock(imgMaskAtScale,	(int)tabIn[i][0], (int)tabIn[i][1], (int)tabIn[i][2], (int)tabIn[i][0]+bSX,  (int)tabIn[i][1]+bSY,  (int)tabIn[i][2]+bSZ);
			for(int j=0;j<vals.length;j++) {
				if(vals[j]<1-epsilon)isOut[i]=1;
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
	
	
	

	/**
	 * Plongement.
	 *
	 * @param ref the ref
	 * @param rootModel the root model
	 * @param addCrosses the add crosses
	 * @return the image plus
	 */
	public static ImagePlus plongement(ImagePlus ref, RootModel rootModel, boolean addCrosses) {
		return rootModel.createGrayScaleImage(ref,1,false,addCrosses,1); 
	}
	

	/**
	 *  display registration and informations during execution.  
	 *
	 * @param level the level
	 * @param iteration the iteration
	 * @param subpixellic the subpixellic
	 * @param textTrans the text trans
	 */

	public void updateViews(int level,int iteration,int subpixellic,String textTrans) {
		String textIter=String.format("Level=%1d/%1d - Iter=%3d/%3d - %s",
				level+1,this.levelMax-this.levelMin+1,
				iteration+1,this.nbIterations,subpixellic>0 ? ("subpixellic 1/"+((int)Math.pow(2,subpixellic))+" pixel") :""
				);

		if(displayRegistration==0) {
			if(this.summary!=null)this.summary.hide();
			return;
		}
		handleOutput("Updating the views...");
		if(this.isRsml)this.sliceMov=plongement(this.imgRef,this.rootModel,true);
		else this.sliceMov=ItkImagePlusInterface.itkImageToImagePlusStack(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(this.imgRef,this.imgMov,false)),this.sliceInt);
		if(flagRange)this.sliceMov.setDisplayRange(movRange[0], movRange[1]);
		else this.sliceMov.resetDisplayRange();
		new StackConverter(sliceMov).convertToGray8();

		this.sliceMov=VitimageUtils.writeTextOnImage(textIter,this.sliceMov,(this.fontSize*4)/3,0);
		if(textTrans!=null)this.sliceMov=VitimageUtils.writeTextOnImage(textTrans,this.sliceMov,this.fontSize,1);

		
		//VitimageUtils.waitFor(2000);
		if(sliceRef==null) {
			handleOutput("Starting graphical following tool...");
			if(this.mask!=null) {
				handleOutput("Starting mask...");
				this.mask.setTitle("Mask in use for image ref");
				this.mask.show();
			}
			this.sliceRef=this.imgRef.duplicate();
			this.sliceRef.setSlice(this.sliceInt);
			if(flagRange)this.sliceRef.setDisplayRange(refRange[0], refRange[1]);
			else this.sliceRef.resetDisplayRange();
			new StackConverter(sliceRef).convertToGray8();

			ImagePlus temp=VitimageUtils.writeTextOnImage(textIter,this.sliceRef,(this.fontSize*4)/3,0);
			if(textTrans!=null)temp=VitimageUtils.writeTextOnImage(textTrans,temp,this.fontSize,1);
			if(flagSingleView)this.sliceFuse=(flagRange ? VitimageUtils.compositeNoAdjustOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving, Gray=score. Level=0 Iter=0 "+this.info) : 
				VitimageUtils.compositeOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving, Gray=score. Level=0 Iter=0 "+this.info));
			else this.sliceFuse=flagRange ? VitimageUtils.compositeNoAdjustOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info) : 
				VitimageUtils.compositeOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info);
			this.sliceFuse.show();
			this.sliceFuse.getWindow().setSize(this.viewWidth*(viewFuseBigger?2:1),this.viewHeight*(viewFuseBigger?2:1));
			this.sliceFuse.getCanvas().fitToWindow();
			this.sliceFuse.setSlice(this.sliceInt);
			VitimageUtils.adjustImageOnScreen(this.sliceFuse,0,0);

			if(displayRegistration>1) {
				ImagePlus tempImg=VitimageUtils.getBinaryGrid(this.imgRef, 10);
				if(this.isRsml) {
					if(this.indField==0) {this.sliceGrid=tempImg.duplicate();}
					
				}
				else this.sliceGrid=this.currentTransform.transformImage(tempImg,tempImg,false);
				this.sliceGrid.setSlice(this.sliceInt);
				this.sliceGrid.show();
				this.sliceGrid.setTitle("Transform visualization on a uniform 3D grid");
				this.sliceGrid.getWindow().setSize(this.viewWidth,this.viewHeight);
				this.sliceGrid.getCanvas().fitToWindow();
				this.sliceGrid.setSlice(this.sliceInt);
				VitimageUtils.adjustImageOnScreenRelative(this.sliceGrid,this.sliceFuse,2,0,10);
	
				tempImg=new Duplicator().run(imgRef);
				tempImg=VitimageUtils.convertToFloat(tempImg);
				tempImg.getProcessor().set(0);
				this.sliceCorr=tempImg;
				this.sliceCorr.setSlice(this.sliceIntCorr);
				this.sliceCorr.show();
				this.sliceCorr.setTitle("Similarity heatmap");
				this.sliceCorr.getWindow().setSize(this.viewWidth,this.viewHeight);
				this.sliceCorr.getCanvas().fitToWindow();
				this.sliceCorr.getProcessor().setMinAndMax(0,1);
				this.sliceCorr.setSlice(this.sliceIntCorr);
				IJ.selectWindow("Similarity heatmap");
				IJ.run("Fire","");
				VitimageUtils.adjustImageOnScreenRelative(this.sliceCorr,this.sliceFuse,2,2,10);
			}
			if(flagSingleView)IJ.selectWindow("Registration is running. Red=Reference, Green=moving, Gray=score. Level=0 Iter=0"+" "+this.info);
			else IJ.selectWindow("Registration is running. Red=Reference, Green=moving. Level=0 Iter=0"+" "+this.info);
		}
		else {
			handleOutput("Updating graphical following tool...");
			ImagePlus tempImg=null;
			ImagePlus temp=VitimageUtils.writeTextOnImage(textIter,this.sliceRef,(this.fontSize*4)/3,0);
			if(textTrans!=null)temp=VitimageUtils.writeTextOnImage(textTrans,temp,this.fontSize,1);

			if(this.flagSingleView)tempImg=VitimageUtils.compositeRGBDoubleJet(temp,this.sliceMov,this.sliceCorr,"Registration is running. Red=Reference, Green=moving, Gray=score. Level="+level+" Iter="+iteration+" "+this.info,true,1);
			else tempImg=VitimageUtils.compositeOf(temp,this.sliceMov,"Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info);
			VitimageUtils.actualizeData(tempImg,this.sliceFuse);
				if(this.flagSingleView)this.sliceFuse.setTitle("Registration is running. Red=Reference, Green=moving, Gray=score. Level="+level+" Iter="+iteration+" "+this.info);
			else this.sliceFuse.setTitle("Registration is running. Red=Reference, Green=moving. Level="+level+" Iter="+iteration+" "+this.info);
			//this.sliceFuse.setSlice(this.sliceIntCorr);

			if(displayRegistration>1) {
				tempImg=this.correspondancesSummary.duplicate();//setSlice
				VitimageUtils.actualizeData(tempImg,this.sliceCorr);//TODO : do it using the reaffectation of the pixel value pointer. See in VitimageUtils.actualizeData
				//this.sliceCorr.setSlice(this.sliceIntCorr);

				
				tempImg=VitimageUtils.getBinaryGrid(this.imgRef, 10);
				if(!isRsml || (this.indField==0)) {
					tempImg=this.currentTransform.transformImage(tempImg,tempImg,false);
					//if(isRsml)IJ.run(tempImg,"RGB Color","");
				}
				
				else {
					tempImg=new ItkTransform(new DisplacementFieldTransform( new Image(this.currentField[this.indField-1]))).transformImage(tempImg,tempImg,false);
					tempImg=displayDistanceMapOnGrid(new ItkTransform(new DisplacementFieldTransform( new Image(this.currentField[this.indField-1]) ) ),tempImg );
					
				}
				VitimageUtils.actualizeData(tempImg,this.sliceGrid);//TODO : do it using the reaffectation of the pixel value pointer
				if(isRsml)IJ.run(this.sliceGrid,"Fire","");
			}
		}
	}
	
	
	/**
	 * Display distance map on grid.
	 *
	 * @param tr the tr
	 * @param grid the grid
	 * @return the image plus
	 */
	public ImagePlus displayDistanceMapOnGrid(ItkTransform tr,ImagePlus grid){
		ImagePlus distMap=tr.distanceMap(imgRef,true);//goes from 0 to 1, with a lot between 0 and 0.1
		double factorMult=70;
		double factorAdd=7.5;
		IJ.run(distMap,"Log","");//goes from -inf to 0, with a lot between -inf and -2
		IJ.run(distMap, "Add...", "value="+factorAdd);//goes from -inf to 10, with a lot between -inf and 8
		IJ.run(distMap, "Multiply...", "value="+factorMult);//goes from -inf to 250, with a lot between -inf and 200
		distMap.setDisplayRange(0, 255);
		distMap=VitimageUtils.convertToFloat(distMap);
		distMap=VitimageUtils.convertFloatToByteWithoutDynamicChanges(distMap);
		distMap=VitimageUtils.convertToFloat(distMap);

		ImagePlus gridTemp=grid.duplicate();
		gridTemp=VitimageUtils.convertToFloat(gridTemp);
		
		ImagePlus maskGrid=VitimageUtils.makeOperationOnOneImage(gridTemp, 2,1/255.0,true);
		ImagePlus gridResidual=VitimageUtils.makeOperationOnOneImage(gridTemp, 2,1/15.0,true);
		ImagePlus test=VitimageUtils.makeOperationBetweenTwoImages(maskGrid,distMap,2,true);
		ImagePlus maskRoot=VitimageUtils.getBinaryMask(plongement(this.imgRef,this.rootModel,false), 10);
		ImagePlus maskOutRoot=VitimageUtils.gaussianFiltering(maskRoot, 10,10,0);
		maskOutRoot=VitimageUtils.getBinaryMaskUnary(maskOutRoot, 4);
		
//		maskRoot.duplicate().show();
//		VitimageUtils.waitFor(10000);
		test=VitimageUtils.makeOperationBetweenTwoImages(test, maskRoot, 1, true);
		test=VitimageUtils.makeOperationBetweenTwoImages(test, maskOutRoot, 2, true);
		test=VitimageUtils.makeOperationBetweenTwoImages(test, gridResidual, 1, true);
		test.setDisplayRange(0, 255);
		test=VitimageUtils.convertToFloat(test);
		test=VitimageUtils.convertFloatToByteWithoutDynamicChanges(test);
		IJ.run(test,"Fire","");
		distMap.setDisplayRange(0, 255);
		ImagePlus maskOutGrid=VitimageUtils.invertBinaryMask(maskGrid);
		ImagePlus finalMapOfGrid=VitimageUtils.makeOperationBetweenTwoImages(maskGrid, gridTemp, 2, true);
		ImagePlus finalMapOfOutGrid=VitimageUtils.makeOperationBetweenTwoImages(maskOutGrid, distMap, 2, true);
		distMap=VitimageUtils.makeOperationBetweenTwoImages(finalMapOfGrid,finalMapOfOutGrid, 1, true);
		/*finalMapOfOutGrid.duplicate().show();
		finalMapOfGrid.duplicate().show();
		VitimageUtils.waitFor(20000000);*/
//		ImagePlus maskOutGrid=VitimageUtils.getBinaryMaskUnary(gridTemp, 1);
		
		distMap.setDisplayRange(0, 255);
		distMap=VitimageUtils.convertToFloat(distMap);
		distMap=VitimageUtils.convertFloatToByteWithoutDynamicChanges(distMap);
		IJ.run(distMap,"Fire","");
		//distMap.duplicate().show();
		//VitimageUtils.waitFor(50000);
		
		return test/*distMap*/;
	}
	
	/**
	 * Close last images.
	 */
	public void closeLastImages() {
		if(this.displayRegistration>0) {
			if(this.sliceFuse!=null)this.sliceFuse.changes=false;
			if(this.sliceFuse!=null)this.sliceFuse.close();
			if(this.mask!=null)this.mask.close();
		}
		if(this.displayRegistration==2) {
			if(this.sliceGrid!=null)this.sliceGrid.changes=false;
			if(this.sliceGrid!=null)this.sliceGrid.close();
			if(this.sliceCorr!=null)this.sliceCorr.changes=false;
			if(this.sliceCorr!=null)this.sliceCorr.close();
		}
	}
	
	
	/**
	 * Handle output.
	 *
	 * @param s the s
	 */
	public void handleOutput(String s) {
		if (consoleOutputActivated)System.out.println(s);
		if (imageJOutputActivated)IJ.log(s);
	}

	/**
	 * Handle output no newline.
	 *
	 * @param s the s
	 */
	public void handleOutputNoNewline(String s) {
		if (consoleOutputActivated)System.out.print(s);
	}


	
	
	
	
	
	
	
	/**
	 *  Local and global score computation for block comparison.
	 *
	 * @param valsFixedBlock the vals fixed block
	 * @param valsMovingBlock the vals moving block
	 * @return the double
	 */
	double computeBlockScore(double[]valsFixedBlock,double[]valsMovingBlock) {
		//if(valsFixedBlock.length!=valsMovingBlock.length)return -10E10;
		switch(this.metricType) {
		case CORRELATION :return correlationCoefficient(valsFixedBlock,valsMovingBlock);
		case SQUARED_CORRELATION :double score=correlationCoefficient(valsFixedBlock,valsMovingBlock);return (score*score);
		case MEANSQUARE :return -1*meanSquareDifference(valsFixedBlock,valsMovingBlock);
		default:return -10E10;
		}
	}

	/**
	 * Mean square difference.
	 *
	 * @param X the x
	 * @param Y the y
	 * @return the double
	 */
	double meanSquareDifference(double X[], double Y[]) {
		if(X.length !=Y.length ) {IJ.log("In meanSquareDifference in BlockMatching, blocks length does not match");return 1E8;}
		double sum=0;
		double diff;
		int n=X.length;
		for (int i = 0; i < n; i++) { 
			diff=X[i]-Y[i];
			sum+=(diff*diff);
		}
		return (sum/n);
	}
	
	/**
	 * Correlation coefficient.
	 *
	 * @param X the x
	 * @param Y the y
	 * @return the double
	 */
	public double correlationCoefficient(double X[], double Y[]) { 
		double epsilon=10E-20;
		if(X.length !=Y.length ) {return 0;}
		int n=X.length;
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
		
	/**
	 * Gets the global rsquare with actual transform.
	 *
	 * @return the global rsquare with actual transform
	 */
	public double getGlobalRsquareWithActualTransform() {
		ImagePlus imgResampled;
		imgResampled=this.currentTransform.transformImage(imgRef,imgMov,false);
		int[]dims=VitimageUtils.getDimensions(imgRef);
		double[]refData=VitimageUtils.valuesOfBlock(imgRef,0,0,0,dims[0],dims[1],dims[2]);
		double[]movData=VitimageUtils.valuesOfBlock(imgResampled,0,0,0,dims[0],dims[1],dims[2]);
		double val=correlationCoefficient(refData,movData);
		return (val*val);
	}
	
	
	
	
	
	
	
	/**
	 * Helper functions to determine initial factors from given data and parameters .
	 *
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param doubleIterAtCoarsestLevels the double iter at coarsest levels
	 * @param basis the basis
	 * @return the int[]
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

	/**
	 * Image dims at successive levels.
	 *
	 * @param dimsImg the dims img
	 * @param subFactors the sub factors
	 * @param noSubScaleZ the no sub scale Z
	 * @return the int[][]
	 */
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

	/**
	 * Image vox sizes at successive levels.
	 *
	 * @param voxSizes the vox sizes
	 * @param subFactors the sub factors
	 * @return the double[][]
	 */
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

	/**
	 * Sigma dense field at successive levels.
	 *
	 * @param subFactors the sub factors
	 * @param rapportSigma the rapport sigma
	 * @return the double[]
	 */
	public double[] sigmaDenseFieldAtSuccessiveLevels(int[]subFactors,double rapportSigma) {
		if (subFactors==null)return null;
		double[]ret=new double[subFactors.length];
//		for(int i=0;i<subFactors.length;i++)ret[i]=subFactors[i]*rapportSigma;
		for(int i=0;i<subFactors.length;i++)ret[i]=rapportSigma;
		return ret;
	}
	
	/**
	 * Sigma factors at successive levels.
	 *
	 * @param voxSize the vox size
	 * @param subFactors the sub factors
	 * @param rapportSigma the rapport sigma
	 * @return the double[]
	 */
	public double[] sigmaFactorsAtSuccessiveLevels(double []voxSize,int[]subFactors,double rapportSigma) {
		if (subFactors==null)return null;
		double voxSizeInit=Math.min(Math.min(voxSize[0],voxSize[1]),voxSize[2]);
		double[]ret=new double[subFactors.length];
		for(int i=0;i<subFactors.length;i++)ret[i]=voxSizeInit*subFactors[i]*rapportSigma;
		return ret;
	}

	/**
	 * Successive step factors at successive levels.
	 *
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param doubleIterAtCoarsestLevels the double iter at coarsest levels
	 * @param basis the basis
	 * @return the double[]
	 */
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

	
	




	/**
	 * Memory free and management.
	 */
	public void freeMemory() {
		this.resampler=null;
		this.imgRef=null;
		this.imgMov=null;
		if(currentField!=null && currentField.length>0)for(int i=0;i<currentField.length ;i++) this.currentField[i]=null;
		this.currentField=null;
		this.currentTransform=null;
		this.sliceRef=null;;
		this.sliceMov=null;;
		this.sliceFuse=null;;
		this.sliceGrid=null;;
		this.sliceCorr=null;;
		this.sliceJacobian=null;
		this.summary=null;
		this.gridSummary=null;
		this.correspondancesSummary=null;
		this.jacobianSummary=null;
		this.mask=null;
		System.gc();
	}
	
	/**
	 * Adjust zoom factor.
	 *
	 * @param newZoom the new zoom
	 */
	public void adjustZoomFactor(double newZoom) {
		this.zoomFactor=newZoom;
		this.viewHeight=(int)(this.imgRef.getHeight()*zoomFactor);
		this.viewWidth=(int)(this.imgRef.getWidth()*zoomFactor);		
	}
	

}


/** Comparators used for sorting data when trimming by score, variance or distance to computed transform
 * 
 */
@SuppressWarnings("rawtypes")
class PointTabComparatorByScore implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((Point3d[]) o1)[2].x)).compareTo(new Double(((Point3d[]) o2)[2].x));
   }
}

@SuppressWarnings("rawtypes")
class PointTabComparatorByDistanceLTS implements java.util.Comparator {
	   public int compare(Object o1, Object o2) {
		   return (new Double(((Point3d[]) o1)[2].z)).compareTo(new Double(((Point3d[]) o2)[2].z));
	   }
	}

@SuppressWarnings("rawtypes")
class VarianceComparator implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((double[]) o1)[0])).compareTo(new Double(((double[]) o2)[0]));
   }
}

@SuppressWarnings("rawtypes")
class ScoreComparator implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((double[][]) o1)[2][0])).compareTo(new Double(((double[][]) o2)[2][0]));
   }
}











