package com.vitimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.ResampleImageFilter;

import com.sun.tools.javac.code.Attribute.Array;
import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.TransformUtils.AngleComparator;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.process.FloatProcessor;
import ij.process.LUT;
import math3d.Point3d;

public class BlockMatchingRegistration  implements ItkImagePlusInterface{
	public boolean displayRegistration=true;
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
	public int percentageBlocksSelectedByVariance=30;//15;
	public double minBlockVariance=0.05;
	public double minBlockScore=0.1;
	public int percentageBlocksSelectedRandomly=90;//50;
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
	public Transformation3DType transformationType;
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
	public double zoomFactor=1;
	public int viewWidth;
	public int viewHeight;
	public ImagePlus mask=null;
	public static void callDebugDef() {
		System.out.println("Debug production method. Start...");
		//Lecture image initiale et champ et construction grille
		ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Test/TestBM/imgMov_Ttest.tif");
		ItkTransform trans=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Test/TestBM/testField.tif");
		ImagePlus grid=VitimageUtils.getBinaryGrid(imgMov, 10);

		
		//Deformation image et grille
		ImagePlus imgTrans=trans.transformImage(imgMov,imgMov);
		ImagePlus gridTrans=trans.transformImage(imgMov,grid);

		
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
	
	
	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		int viewSlice=19;
		///////////TEST 1/////////////
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Test/BM_subvoxel/imgRef_Ttest.tif");
		ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Test/BM_subvoxel/imgMov_Ttest.tif");
		ImagePlus imgMask=IJ.openImage("/home/fernandr/Bureau/Test/BM_subvoxel/mask_little.tif");

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
		ImagePlus testNorm=testField.transformImage(imgRef, imgRef);	
		testNorm.setTitle("Validation_after");
		testNorm.show();
		IJ.run(normField, "Merge Channels...", "c1=Distance_map c2=Validation_after create");
		normField=IJ.getImage();
		normField.setTitle("Distance map in final transform");
		normField.setSlice(viewSlice);
		
		
		//CALCUL DE L IMAGE AVANT/APRES
		ImagePlus imgInit=new ItkTransform().transformImage(imgRef,imgMov);
		ImagePlus imgResampled=testField.transformImage(imgRef,imgMov);
		imgResampled.getProcessor().setMinAndMax(imgMov.getProcessor().getMin(), imgMov.getProcessor().getMax());
		imgInit.getProcessor().setMinAndMax(imgMov.getProcessor().getMin(), imgMov.getProcessor().getMax());
		ImagePlus viewAfter=VitimageUtils.compositeNoAdjustOf(imgRef, imgResampled,"After");
		ImagePlus viewBefore=VitimageUtils.compositeNoAdjustOf(imgRef, imgInit,"Before");
		viewBefore.show();
		viewBefore.setSlice(viewSlice);
		viewAfter.show();
		viewAfter.setSlice(viewSlice);
		
	
	}

	
	
	public static ItkTransform setupAndRunRoughBlockMatchingWithoutFineParameterization(
				ImagePlus imgRef,ImagePlus imgMov,ImagePlus imgMask,Transformation3DType transType,MetricType metr,
				int levelMax,int levelMin,int blockSize,int neighSize,double varMin,double sigma,int duration,boolean displayRegistration,boolean displayR2)
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
		bmRegistration.displayRegistration=displayRegistration;
		bmRegistration.displayR2=displayR2;
		System.out.println("C est parti");
		transRet=bmRegistration.runMultiThreaded(new ItkTransform());
		System.out.println("C est fini");
		
		if(transType==Transformation3DType.DENSE ) transRet=transRet.flattenDenseField(imgRef);
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
		double denseFieldSigmaInMM=0.8;
		int levelMinRig=doThinnerRigid ? -1 : 0;
		int levelMaxRig=doCoarserRigid ? 2 : 1;
		int levelMinDen=doThinnerDense ? -1 : 0;
		int levelMaxDen=doCoarserDense ? 2 : 1;
		int viewSlice=imgRef.getStackSize()/2;
		int nbIterations=8;
		int nbIterationsRig=6;
		int neighXY=2;
		int neighZ=1;
		int bSXY=7;
		int bSZ=1;
		int strideXY=3;
		int strideZ=1;
		ItkTransform transRet=null;
		if(doRigidPart) {
			BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transformation3DType.VERSOR,MetricType.SQUARED_CORRELATION,
					sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinRig,levelMaxRig,nbIterationsRig,
					viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
			transRet=bmRegistration.runMultiThreaded(new ItkTransform());
			if( !doDensePart) {
				bmRegistration.computeSummaries();
				bmRegistration.temporarySummariesSave();
				bmRegistration.closeLastImages();
				bmRegistration.freeMemory();
				return transRet;
			}
			else {
				bmRegistration.transformationType=Transformation3DType.DENSE;
				bmRegistration.levelMin=levelMinDen;
				bmRegistration.levelMax=levelMaxDen;
				bmRegistration.nbLevels=bmRegistration.levelMax-bmRegistration.levelMin+1;
				bmRegistration.subScaleFactors=bmRegistration.subSamplingFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
				bmRegistration.successiveStepFactors=bmRegistration.successiveStepFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
				bmRegistration.successiveDimensions=bmRegistration.imageDimsAtSuccessiveLevels(new int[] {imgRef.getWidth(),imgRef.getHeight(),imgRef.getStackSize()},bmRegistration.subScaleFactors,bmRegistration.noSubScaleZ);
				bmRegistration.successiveVoxSizes=bmRegistration.imageVoxSizesAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors);
				bmRegistration.successiveSmoothingSigma=bmRegistration.sigmaFactorsAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors,bmRegistration.smoothingSigmaInPixels); 
				bmRegistration.successiveDenseFieldSigma=bmRegistration.sigmaDenseFieldAtSuccessiveLevels(bmRegistration.subScaleFactors,bmRegistration.denseFieldSigma);
				transRet=bmRegistration.runMultiThreaded(transRet);
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
				BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transformation3DType.DENSE,MetricType.SQUARED_CORRELATION,
													sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinDen,levelMaxDen,nbIterations,
													viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
				transRet=bmRegistration.runMultiThreaded(new ItkTransform());
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
	

	
	
	
	
	public void closeLastImages() {
		if(this.displayRegistration) {
			this.summary.close();
			this.gridSummary.close();
			this.correspondancesSummary.close();
			this.sliceFuse.changes=false;
			this.sliceGrid.changes=false;
			this.sliceCorr.changes=false;
			this.sliceFuse.close();
			this.sliceGrid.close();
			this.sliceCorr.close();
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
		int nbIterations=14;
		int nbIterationsRig=14;
		int neighXY=3;
		int neighZ=1;
		int bSXY=9;
		int bSZ=2;
		int strideXY=3;
		int strideZ=1;
		ItkTransform transRet=null;
		if(doRigidPart) {
			BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transformation3DType.VERSOR,MetricType.SQUARED_CORRELATION,
					sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinRig,levelMaxRig,nbIterationsRig,
					viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
			transRet=bmRegistration.runMultiThreaded(new ItkTransform());
			if( !doDensePart) {
				bmRegistration.computeSummaries();
				bmRegistration.temporarySummariesSave();
				bmRegistration.closeLastImages();
				bmRegistration.freeMemory();
				return transRet;
			}
			else {
				bmRegistration.transformationType=Transformation3DType.DENSE;
				bmRegistration.levelMin=levelMinDen;
				bmRegistration.levelMax=levelMaxDen;
				bmRegistration.nbLevels=bmRegistration.levelMax-bmRegistration.levelMin+1;
				bmRegistration.subScaleFactors=bmRegistration.subSamplingFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
				bmRegistration.successiveStepFactors=bmRegistration.successiveStepFactorsAtSuccessiveLevels(bmRegistration.levelMin,bmRegistration.levelMax,false,2);
				bmRegistration.successiveDimensions=bmRegistration.imageDimsAtSuccessiveLevels(new int[] {imgRef.getWidth(),imgRef.getHeight(),imgRef.getStackSize()},bmRegistration.subScaleFactors,bmRegistration.noSubScaleZ);
				bmRegistration.successiveVoxSizes=bmRegistration.imageVoxSizesAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors);
				bmRegistration.successiveSmoothingSigma=bmRegistration.sigmaFactorsAtSuccessiveLevels(VitimageUtils.getVoxelSizes(imgRef),bmRegistration.subScaleFactors,bmRegistration.smoothingSigmaInPixels); 
				bmRegistration.successiveDenseFieldSigma=bmRegistration.sigmaDenseFieldAtSuccessiveLevels(bmRegistration.subScaleFactors,bmRegistration.denseFieldSigma);
				transRet=bmRegistration.runMultiThreaded(transRet);
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
				BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transformation3DType.DENSE,MetricType.SQUARED_CORRELATION,
													sigmaSmoothingInPixels,denseFieldSigmaInMM,levelMinDen,levelMaxDen,nbIterations,
													viewSlice,mask,neighXY,neighZ,bSXY,bSZ,strideXY,strideZ);
				transRet=bmRegistration.runMultiThreaded(new ItkTransform());
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
	

	public void temporarySummariesSave() {
		long lo=new Date().getTime();
		IJ.saveAsTiff(this.gridSummary, "/home/fernandr/Temp/BM"+lo+"imageGrid.tif");
		IJ.saveAsTiff(this.summary, "/home/fernandr/Temp/BM"+lo+"imageSummary.tif");
		this.summary.hide();
	}
	
	
	public BlockMatchingRegistration(ImagePlus imgRef,ImagePlus imgMov,Transformation3DType transformationType,MetricType metricType,
			double smoothingSigmaInPixels, double denseFieldSigma,int levelMin,int levelMax,int nbIterations,int sliceInt,ImagePlus mask,
			int neighbourXY,int neighbourZ,int blockHalfSizeXY,int blockHalfSizeZ,int strideXY,int strideZ) {
		if(imgRef.getWidth()<imgRef.getStackSize()*4)noSubScaleZ=false;
		this.resampler=new ResampleImageFilter();
		this.imgRef=new Duplicator().run(imgRef);
		this.imgMov=new Duplicator().run(imgMov);
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
		this.minBlockVariance=VitimageUtils.statistics1D(VitimageUtils.valuesOfBlock(imgRef, 0, 0, dims[2]/2-1, dims[0]-1, dims[1]-1, dims[2]/2-1))[0]/3.0;
		this.sliceInt=sliceInt;
		this.viewHeight=(int)(this.imgRef.getHeight()*zoomFactor);
		this.viewWidth=(int)(this.imgRef.getWidth()*zoomFactor);
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
	}

	public void handleOutput(String s) {
		if (consoleOutputActivated)System.out.println(s);
		if (imageJOutputActivated)IJ.log(s);
	}

	public void handleOutputNoNewline(String s) {
		if (consoleOutputActivated)System.out.print(s);
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
		System.out.print("    Masking : selected "+n+" over "+n0);
		return ret;
	}
	
/**
 * Main game is here
 * @param trInit
 * @return
 */
	@SuppressWarnings("unchecked")
	public ItkTransform runMultiThreaded(ItkTransform trInit) {
		double progress=0;IJ.showProgress(progress);
		this.currentTransform=trInit;
		handleOutput(new Date().toString());
		//Initialize various artifacts
		ImagePlus imgRefTemp;
		ImagePlus imgMovTemp;
		handleOutput("------------------------------");
		handleOutput("| Block Matching registration|");
		handleOutput("------------------------------");
		handleOutput(" ");
		handleOutput(" ");
		System.out.println("   .-------.          ______");
		System.out.println("  /       /|         /\\     \\");
		System.out.println(" /_______/ |        /  \\     \\");
		System.out.println(" |       | |  -->  /    \\_____\\");
		System.out.println(" |       | /       \\    /     /");
		System.out.println(" |       |/         \\  /     /");
		System.out.println(" .-------.           \\/____ /");
		System.out.println("");
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
		handleOutput(" |--* Beginning matching. Initial superposition with global R^2 = "+getGlobalRsquareWithActualTransform() +"\n\n");		
		this.updateViews();
		int nbProc=this.defaultCoreNumber;
		double timeFactor=0.000000003;
		ImagePlus imgMaskTemp=null;
		progress=0.05;IJ.showProgress(progress);

		//for each scale
		for(int lev=0;lev<nbLevels;lev++) {
			handleOutput("");
			int[]curDims=successiveDimensions[lev];
			double[]curVoxSizes=successiveVoxSizes[lev];
			double stepFactor=this.successiveStepFactors[lev];
			boolean subVoxel=(stepFactor<0.99);
			handleOutput("--> Level "+(lev+1)+"/"+nbLevels+" . Dims=("+curDims[0]+"x"+curDims[1]+"x"+curDims[2]+"), search step factor ="+stepFactor+" pixels. ");

			// blocks from fixed
			int nbBlocksX=1+(curDims[0]-this.blockSizeX-2*this.neighbourhoodSizeX)/this.blocksStrideX;
			int nbBlocksY=1+(curDims[1]-this.blockSizeY-2*this.neighbourhoodSizeY)/this.blocksStrideY;
			int nbBlocksZ=1+(curDims[2]-this.blockSizeZ-2*this.neighbourhoodSizeZ)/this.blocksStrideZ;
			handleOutput("Debug");
			handleOutput(TransformUtils.stringVectorN(successiveVoxSizes[lev], "Successive vox sizes"));
			handleOutput(TransformUtils.stringVectorN(curDims, "curDims"));
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
			imgRefTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgRefTemp)));

			//resample the mask image
			if(this.mask !=null) {
				this.resampler.setDefaultPixelValue(255);
				imgMaskTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(this.mask)));
			}
			//for each iteration
			for(int iter=0;iter<nbIterations;iter++) {
				progress=0.1+0.9*(lev*1.0/nbLevels+(iter*1.0/nbIterations)*1.0/nbLevels);IJ.showProgress(progress);
				handleOutput("\n   --> Iteration "+(iter+1) +"/"+this.nbIterations);
				
				//resample the moving image with currentTransform, in the fixed image space, at the scale
				this.resampler.setTransform(this.currentTransform);
				this.resampler.setDefaultPixelValue(this.imgMovDefaultValue);
				imgMovTemp=VitimageUtils.gaussianFilteringIJ(this.imgMov, this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev], this.successiveSmoothingSigma[lev]);
				imgMovTemp=ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgMovTemp)));

				

				//Prepare a coordinate summary tabs for this blocks, compute and store their sigma
				int indexTab=0;
				nbBlocksTotal=nbBlocksX*nbBlocksY*nbBlocksZ;
				double[][] blocksRefTmp=new double[nbBlocksTotal][4];
				handleOutput("       # blocks before trimming="+nbBlocksTotal);

				
				
				for(int blX=0;blX<nbBlocksX ;blX++) {
					for(int blY=0;blY<nbBlocksY ;blY++) {
						for(int blZ=0;blZ<nbBlocksZ ;blZ++) {
							double[]valsBlock=VitimageUtils.valuesOfBlock(imgRefTemp,
									blX*this.blocksStrideX+this.neighbourhoodSizeX,                blY*this.blocksStrideY+this.neighbourhoodSizeY,                blZ*this.blocksStrideZ+this.neighbourhoodSizeZ,
									blX*this.blocksStrideX+this.blockSizeX+this.neighbourhoodSizeX-1,blY*this.blocksStrideY+this.blockSizeY+this.neighbourhoodSizeY-1,blZ*this.blocksStrideZ+this.blockSizeZ+this.neighbourhoodSizeZ-1);
							double[]stats=VitimageUtils.statistics1D(valsBlock);
							blocksRefTmp[indexTab++]=new double[] {stats[1],blX*this.blocksStrideX+this.neighbourhoodSizeX,       blY*this.blocksStrideY+this.neighbourhoodSizeY,     blZ*this.blocksStrideZ+this.neighbourhoodSizeZ};
						}
					}
				}

				//Sort by variance
				double meanVar=0;
				for(int i=0;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
				Arrays.sort(blocksRefTmp,new VarianceComparator());
				
				//Keep this.percentageBlocksSelectedByVariance
				int lastRemoval=(nbBlocksTotal*(100-this.percentageBlocksSelectedByVariance))/100;
				for(int bl=0;bl<lastRemoval;bl++)blocksRefTmp[bl][0]=-1;
				meanVar=0;
				for(int i=lastRemoval;i<blocksRefTmp.length;i++)meanVar+=blocksRefTmp[i][0];
				
				
			
				
				//Keep randomly this.percentageBlocksSelectedRandomly
				lastRemoval=0;
				double ratio=this.percentageBlocksSelectedRandomly/100.0;
				for(int bl=0;bl<nbBlocksTotal;bl++)if(Math.random() > ratio ) {lastRemoval++;blocksRefTmp[bl][0]=-1;}
				//Copy only the both selected blocks
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
				System.out.print("       # blocks after trimming="+nbBlocksTotal);
			
				
				//Trim the ones outside the mask
				if(this.mask !=null)blocksRef=this.trimUsingMask(blocksRef,imgMaskTemp,bSX,bSY,bSZ);
				nbBlocksTotal=blocksRef.length;
				
				// Multi-threaded exectution of the algorithm core (a block-matching)
				final ImagePlus []imgRefTempThread=new ImagePlus[nbProc];
				final ImagePlus []imgMovTempThread=new ImagePlus[nbProc];
				final double minBS=this.minBlockScore;
				final double [][][][]correspondances=new double[nbProc][][][];
				final double [][][]blocksProp=createBlockPropsFromBlockList(blocksRef,nbProc);
				for(int pr=0;pr<nbProc;pr++) {
					imgRefTempThread[pr]=imgRefTemp.duplicate();
					imgMovTempThread[pr]=imgMovTemp.duplicate();
				}
				AtomicInteger atomNumThread=new AtomicInteger(0);
				AtomicInteger curProcessedBlock=new AtomicInteger(0);
				final int nbTotalBlock=blocksRef.length;
				final Thread[] threads = VitimageUtils.newThreadArray(nbProc);    
				for (int ithread = 0; ithread < nbProc; ithread++) {  
					threads[ithread] = new Thread() {  { setPriority(Thread.NORM_PRIORITY); }  
					public void run() {  
						int numThread=atomNumThread.getAndIncrement();
						double[][]blocksPropThread=blocksProp[numThread];
						double[][][]correspondancesThread=new double[blocksProp[numThread].length][][];
						//for each fixed block
						for(int fixBl=0;fixBl<blocksProp[numThread].length;fixBl++) {
							//extract ref block data in moving image
							int x0=(int)Math.round(blocksPropThread[fixBl][0]);
							int y0=(int)Math.round(blocksPropThread[fixBl][1]);
							int z0=(int)Math.round(blocksPropThread[fixBl][2]);
							int x1=x0+bSX-1;
							int y1=y0+bSY-1;
							int z1=z0+bSZ-1;
							double []valsFixedBlock=VitimageUtils.valuesOfBlock(imgRefTempThread[numThread],x0,y0,z0,x1,y1,z1);
							double scoreMax=-10E100;
							double distMax=0;
							int xMax=0;
							int yMax=0;
							int zMax=0;
							//for each moving block
							int numBl=curProcessedBlock.getAndIncrement();
							if(nbTotalBlock>1000 && (numBl%(nbTotalBlock/30.0)==0))handleOutputNoNewline((" "+((numBl*100)/nbTotalBlock)+"%"));
							for(int xPlus=-nSX;xPlus<=nSX;xPlus++) {
								for(int yPlus=-nSY;yPlus<=nSY;yPlus++) {
									for(int zPlus=-nSZ;zPlus<=nSZ;zPlus++) {
										//compute similarity between blocks, according to the metric
										double []valsMovingBlock=(subVoxel? 
												VitimageUtils.valuesOfBlockDouble(imgMovTempThread[numThread],x0+xPlus*stepFactor,y0+yPlus*(stepFactor),z0+zPlus*(stepFactor),x1+xPlus*(stepFactor),y1+yPlus*(stepFactor),z1+zPlus*(stepFactor) ) :
												VitimageUtils.valuesOfBlock(imgMovTempThread[numThread],x0+xPlus,y0+yPlus,z0+zPlus,x1+xPlus,y1+yPlus,z1+zPlus ) )	;
										double score=computeBlockScore(valsFixedBlock,valsMovingBlock);
										double distance=Math.sqrt( (xPlus*voxSX*(subVoxel?stepFactor:1)) *  (xPlus*voxSX*(subVoxel?stepFactor:1)) +  
												(yPlus*voxSY*(subVoxel?stepFactor:1)) *  (yPlus*voxSY*(subVoxel?stepFactor:1)) +
												(zPlus*voxSZ*(subVoxel?stepFactor:1)) *  (zPlus*voxSZ*(subVoxel?stepFactor:1))     );
											
										if(Math.abs(score)>10E10)handleOutput("THREAD ALERT");
										//keep the best one
										if( (score>scoreMax) || ((score==scoreMax) && (distance < distMax)) ) {
											xMax=xPlus;yMax=yPlus;zMax=zPlus;scoreMax=score;distMax=distance;}
									}
								}
							}
							correspondancesThread[fixBl]=new double[][] {							
									new double[] {blocksPropThread[fixBl][0]+bSXHalf , blocksPropThread[fixBl][1]+bSYHalf , blocksPropThread[fixBl][2]+bSZHalf} ,
									new double[] {blocksPropThread[fixBl][0]+bSXHalf + xMax*(subVoxel?stepFactor:1), blocksPropThread[fixBl][1]+bSYHalf  + yMax*(subVoxel?stepFactor:1), blocksPropThread[fixBl][2]+bSZHalf  + zMax*(subVoxel?stepFactor:1)},
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
					
						correspondances[numThread]=correspondancesThread2;	
							
					} };  		
				}				
				VitimageUtils.startAndJoin(threads);  
				handleOutput("");
				//Convert the correspondance from each thread correspondance list to a main list for the whole image
				ArrayList<double[][]> listCorrespondances=new ArrayList<double[][]>();
				for(int i=0;i<correspondances.length;i++) {
					for(int j=0;j<correspondances[i].length;j++){
						listCorrespondances.add(correspondances[i][j]);	
					}
				}
				
				
				
				Point3d[][]correspondancePoints=getCorrespondanceListAsTrimmedPointArray(listCorrespondances,this.successiveVoxSizes[lev],this.percentageBlocksSelectedByScore);///////CHANGER ICI UN JOUR SIGMA
				this.correspondancesSummary=getCorrespondanceListAsImagePlus(imgRef,listCorrespondances,curVoxSizes);
				
				//if affine
				if(this.transformationType!=Transformation3DType.DENSE) {
					
					//compute the affine transform that lies with these couples
					ItkTransform transEstimated=null;
					switch(this.transformationType) {
					//case EULER:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[0],correspondancePoints[1]);break;
					case VERSOR:transEstimated=ItkTransform.estimateBestRigid3D(correspondancePoints[1],correspondancePoints[0]);break;
					case AFFINE:transEstimated=ItkTransform.estimateBestAffine3D(correspondancePoints[1],correspondancePoints[0]);break;
					case SIMILARITY:transEstimated=ItkTransform.estimateBestSimilarity3D(correspondancePoints[1],correspondancePoints[0]);break;
					default:transEstimated=ItkTransform.estimateBestSimilarity3D(correspondancePoints[1],correspondancePoints[0]);break;
					}
					//Finally, add it to the current stack of transformations
					this.currentTransform.addTransform(transEstimated);
					handleOutput("Global transform after this step =\n"+this.currentTransform.drawableString());
				}		
				else {
					handleOutput("       Field interpolation from "+correspondancePoints[0].length+" correspondances with sigma="+this.successiveDenseFieldSigma[lev]+" "+imgRefTemp.getCalibration().getUnit()+
							" ( "+((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSX)))+" voxSX , " +((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSY)))+" voxSY , " +((int)(Math.round(this.successiveDenseFieldSigma[lev]/voxSZ)))+" voxSZ )"  );
					
					//compute the field
					this.currentField[indField++]=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(correspondancePoints,imgRefTemp,this.successiveDenseFieldSigma[lev]);
					
				//Finally, add it to the current stack of transformations
				this.currentTransform.addTransform(new ItkTransform(new DisplacementFieldTransform( this.currentField[indField-1])));
				}
				if(displayR2)handleOutput("Global R^2 after iteration="+getGlobalRsquareWithActualTransform());
				this.updateViews();

			}
		}
		handleOutput(new Date().toString());

		if(this.transformationType!=Transformation3DType.DENSE)handleOutput("\nMatrice finale block matching : \n"+this.currentTransform.drawableString());
		if(false && this.transformationType==Transformation3DType.DENSE)this.sliceJacobian.hide();

		return new ItkTransform(this.currentTransform);
	}
	
				
	
	public ImagePlus getCorrespondanceListAsImagePlus(ImagePlus imgRef,ArrayList<double[][]>tabCorr,double[]curVoxSize) {
		int [] dims=VitimageUtils.getDimensions(imgRef);		
		ImagePlus ret=IJ.createImage("corr", dims[0], dims[1], dims[2], 32);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		double [] voxS=VitimageUtils.getVoxelSizes(imgRef);
		float[][]dataRet=new float[dims[2]][];
		for(int z=0;z<dims[2];z++) {
			dataRet[z]=(float[])ret.getStack().getProcessor(z+1).getPixels();
		}

		for(int cor=0;cor<tabCorr.size();cor++) {
			int x=(int)Math.round( tabCorr.get(cor)[0][0]/voxS[0]*curVoxSize[0]  );
			int y=(int)Math.round( tabCorr.get(cor)[0][1]/voxS[1]*curVoxSize[1]  );
			int z=(int)Math.round( tabCorr.get(cor)[0][2]/voxS[2]*curVoxSize[2]  );
			int index=dims[0]*y+x;
			float score=(float)tabCorr.get(cor)[2][0];
			dataRet[z][index]=score;
		}
		return ret;		
	}
	
	
	
	
				
	Point3d[][] getCorrespondanceListAsTrimmedPointArray(ArrayList<double[][]>list,double[]voxSizes,int percentageKeep){
		int n=list.size();
		int  ind=0;
		for(int i=0;i<n;i++)if(list.get(i)[2][1]<0)n--;
		Point3d[][]ret=new Point3d[3][n];
		for(int i=0;i<n;i++)if(list.get(i)[2][1]>0) {
			double [][]tabInit=list.get(i);
			ret[0][ind]=new Point3d(tabInit[0][0]*voxSizes[0],tabInit[0][1]*voxSizes[1],tabInit[0][2]*voxSizes[2]);
			ret[1][ind]=new Point3d(tabInit[1][0]*voxSizes[0],tabInit[1][1]*voxSizes[1] ,tabInit[1][2]*voxSizes[2]);
			ret[2][ind]=new Point3d(tabInit[2][0],tabInit[2][1],0);
			ind++;
		}
		if(percentageKeep==100)return ret;
		else return trimPoint3dTabBasedOnScores(ret,percentageKeep);
	}
	
	
	
	Point3d[][]trimPoint3dTabBasedOnScores(Point3d[][] tabIn,int percentageKeep){
		double meanVar=0;
		for(int i=0;i<tabIn[0].length;i++)meanVar+=tabIn[2][i].x;
		double meanBef=meanVar/tabIn[0].length;

		Point3d[][]tmp=new Point3d[tabIn[0].length][3];
		for(int i=0;i<tabIn.length;i++)		for(int j=0;j<tabIn[0].length;j++)tmp[j][i]=tabIn[i][j];
		Arrays.sort(tmp, new PointTabComparator());
		for(int i=0;i<tabIn.length;i++)		for(int j=0;j<tabIn[0].length;j++)tabIn[i][j]=tmp[j][i];
		
		//Keep this.percentageBlocksSelectedByVariance
		int lastRemoval=(tabIn[0].length*(100-percentageKeep))/100;
		Point3d[][]ret=new Point3d[3][tabIn[0].length-lastRemoval];
		for(int bl=lastRemoval;bl<tabIn[0].length;bl++) {
			ret[0][bl-lastRemoval]=tabIn[0][bl];
			ret[1][bl-lastRemoval]=tabIn[1][bl];
			ret[2][bl-lastRemoval]=tabIn[2][bl];
		}

		meanVar=0;
		for(int i=0;i<ret[0].length;i++) {
			meanVar+=ret[2][i].x;
		}
		handleOutput("   Mean correspondance score before / after : "+VitimageUtils.dou(meanBef)+" / "+VitimageUtils.dou(meanVar/(ret[0].length)));
		return ret;
	}
	
	


	public void updateViews() {
		if(!displayRegistration) {
			if(this.summary!=null)this.summary.hide();
			return;
		}
		System.out.print("Updating the views...");
		this.sliceMov=ItkImagePlusInterface.itkImageToImagePlusSlice(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(this.imgRef,this.imgMov)),this.sliceInt);
		if(sliceRef==null) {
			System.out.print("Starting graphical following tool...");
			this.sliceRef=ItkImagePlusInterface.itkImageToImagePlusSlice(ItkImagePlusInterface.imagePlusToItkImage(this.imgRef),this.sliceInt);
			this.sliceFuse=VitimageUtils.compositeOf(this.sliceRef,this.sliceMov,"Registration is running. Red=Reference, Green=moving");
			this.sliceFuse.show();
			this.sliceFuse.getWindow().setSize(this.viewWidth,this.viewHeight);
			this.sliceFuse.getCanvas().fitToWindow();
			
			ImagePlus tempImg=VitimageUtils.getBinaryGrid(this.imgRef, 10);
			this.sliceGrid=ItkImagePlusInterface.itkImageToImagePlusSlice(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(tempImg,tempImg)),this.sliceInt);
			this.sliceGrid.show();
			this.sliceGrid.setTitle("Field visualization on a uniform 3D grid");
			this.sliceGrid.getWindow().setSize(this.viewWidth,this.viewHeight);
			this.sliceGrid.getCanvas().fitToWindow();

			tempImg=new Duplicator().run(imgRef);
			tempImg.getProcessor().set(0);
			this.sliceCorr=ItkImagePlusInterface.itkImageToImagePlusSlice(ItkImagePlusInterface.imagePlusToItkImage(tempImg),this.sliceInt);
			this.sliceCorr.show();
			this.sliceCorr.setTitle("Correspondances points");
			this.sliceCorr.getWindow().setSize(this.viewWidth,this.viewHeight);
			this.sliceCorr.getCanvas().fitToWindow();
			this.sliceCorr.getProcessor().setMinAndMax(0,1);
			IJ.selectWindow("Correspondances points");
			IJ.run("Fire","");

			
			if(false && this.transformationType==Transformation3DType.DENSE) {
				this.sliceJacobian=sliceGrid.duplicate();this.sliceJacobian.getProcessor().set(1);IJ.run(this.sliceJacobian,"32-bit","");
				this.sliceJacobian.setTitle("Field jacobian determinant");
				this.sliceJacobian.show();
				this.sliceJacobian.getWindow().setSize(this.viewWidth,this.viewHeight);
				this.sliceJacobian.getCanvas().fitToWindow();
				IJ.run(this.sliceJacobian, "Fire","");
				this.sliceJacobian.setDisplayRange(0.99, 1.01);
			}
		}
		else {
			ImagePlus tempImg=VitimageUtils.compositeOf(this.sliceRef,this.sliceMov,"Registration is running. Red=Reference, Green=moving");
			IJ.run(tempImg, "Select All", "");
			tempImg.copy();
			this.sliceFuse.paste();

			tempImg=VitimageUtils.getBinaryGrid(this.imgRef, 10);
			tempImg=ItkImagePlusInterface.itkImageToImagePlusSlice(ItkImagePlusInterface.imagePlusToItkImage(this.currentTransform.transformImage(tempImg,tempImg)),this.sliceInt);
			IJ.run(tempImg, "Select All", "");
			tempImg.copy();
			this.sliceGrid.paste();

			tempImg=ItkImagePlusInterface.itkImageToImagePlusSlice(ItkImagePlusInterface.imagePlusToItkImage(this.correspondancesSummary),this.sliceInt);
			IJ.run(tempImg, "Select All", "");
			tempImg.copy();
			this.sliceCorr.paste();

			if(false && this.transformationType==Transformation3DType.DENSE) {
				tempImg=ItkTransform.getJacobian(this.currentTransform,this.imgRef);
				tempImg=ItkImagePlusInterface.itkImageToImagePlusSlice(ItkImagePlusInterface.imagePlusToItkImage(tempImg),this.sliceInt);
				

				IJ.run(tempImg, "Select All", "");
				tempImg.copy();
				this.sliceJacobian.paste();
				IJ.run(this.sliceJacobian, "Fire","");
				this.sliceJacobian.setDisplayRange(0.99, 1.01);	
			}
		}
		
		
		this.summaryArray.add(this.sliceFuse.duplicate());
		this.gridSummaryArray.add(this.sliceGrid.duplicate());
		this.correspondancesSummaryArray.add(this.sliceCorr.duplicate());
		if(false)this.jacobianSummaryArray.add(this.sliceJacobian.duplicate());
		handleOutput("Added the slice number "+this.summaryArray.size());
	}
	
	public double getGlobalRsquareWithActualTransform() {
		ImagePlus imgResampled=this.currentTransform.transformImage(imgRef,imgMov);
		int[]dims=VitimageUtils.getDimensions(imgRef);
		double[]refData=VitimageUtils.valuesOfBlock(imgRef,0,0,0,dims[0],dims[1],dims[2]);
		double[]movData=VitimageUtils.valuesOfBlock(imgResampled,0,0,0,dims[0],dims[1],dims[2]);
		double val=correlationCoefficient(refData,movData);
		return (val*val);
	}
	
	public void computeSummaries() {
		if(! this.displayRegistration)return;
		ImagePlus []tabGrid=new ImagePlus[gridSummaryArray.size()];
		for(int i =0;i<gridSummaryArray.size() ; i++) {
			tabGrid[i]=gridSummaryArray.get(i);
		}
		ImagePlus []tabCorr=new ImagePlus[correspondancesSummaryArray.size()];
		for(int i =0;i<correspondancesSummaryArray.size() ; i++) {
			tabCorr[i]=correspondancesSummaryArray.get(i);
		}
		ImagePlus []tabSum=new ImagePlus[summaryArray.size()];
		for(int i =0;i<summaryArray.size() ; i++) {
			tabSum[i]=summaryArray.get(i);
		}

		this.summary=Concatenator.run(tabSum);
		this.summary.setTitle("Summary of alignement VS iterations");
		ImagePlus imgTemp=new Duplicator().run(this.summary);
		IJ.run(imgTemp,"32-bit","");

		this.gridSummary=Concatenator.run(tabGrid);
		this.gridSummary.setTitle("Summary of field evolution VS iterations");
		this.gridSummary=VitimageUtils.compositeOf(this.gridSummary, imgTemp);

		this.correspondancesSummary=Concatenator.run(tabCorr);
		this.correspondancesSummary.setTitle("Correspondance points VS iterations");
		
	}
	
	
	
	
	
	
	
	
	
	
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
	
	double computeBlockScore(double[]valsFixedBlock,double[]valsMovingBlock) {
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

	
	
	double correlationCoefficient(double X[], double Y[]) { 
		double epsilon=10E-20;
		if(X.length !=Y.length )IJ.log("In correlationCoefficient in BlockMatching, blocks length does not match");
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
		return (  (n * sum_XY - sum_X * sum_Y)/ (Math.sqrt((n * squareSum_X - sum_X * sum_X) * (n * squareSum_Y - sum_Y * sum_Y)))); 
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


	
	
	
	
	
	
}


class PointTabComparator implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((Point3d[]) o1)[2].x)).compareTo(new Double(((Point3d[]) o2)[2].x));
   }
}





class VarianceComparator implements java.util.Comparator {
   public int compare(Object o1, Object o2) {
	   return (new Double(((double[]) o1)[0])).compareTo(new Double(((double[]) o2)[0]));
   }
}
















