/*
 * 
 */
package net.imagej.fijiyama.fijiyamaplugin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.fijiyama.common.VitimageUtils;
import net.imagej.fijiyama.fijiyamaplugin.Fijiyama_GUI;
import net.imagej.fijiyama.fijiyamaplugin.RegistrationAction;
import net.imagej.fijiyama.fijiyamaplugin.RegistrationManager;
import net.imagej.fijiyama.registration.OptimizerType;
import net.imagej.fijiyama.registration.Transform3DType;

// TODO: Auto-generated Javadoc
/**
 * The Class RegistrationAction.
 */
public class RegistrationAction implements Serializable{
	
	/** The Constant serialFelicityFicus. */
	private static final long serialFelicityFicus = 600L;
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = serialFelicityFicus;
	
	/** The current serial version UID. */
	private long currentSerialVersionUID = serialFelicityFicus;
	
	/** The is done. */
	private boolean isDone=false;
	
	/** The name action. */
	public String nameAction;
	
	/** The name subject. */
	public String nameSubject;
	
	/** The ref time. */
	public int refTime=0;
	
	/** The ref mod. */
	public int refMod=0;
	
	/** The mov time. */
	public int movTime=0;
	
	/** The mov mod. */
	public int movMod=0;
	
	/** The type action. */
	public int typeAction=0;
	
	/** The Constant TYPEACTION_MAN. */
	public static final int TYPEACTION_MAN=0;
	
	/** The Constant TYPEACTION_AUTO. */
	public static final int TYPEACTION_AUTO=1;
	
	/** The Constant TYPEACTION_ALIGN. */
	public static final int TYPEACTION_ALIGN=2;
	
	/** The Constant TYPEACTION_VIEW. */
	public static final int TYPEACTION_VIEW=3;
	
	/** The Constant TYPEACTION_SAVE. */
	public static final int TYPEACTION_SAVE=4;
	
	/** The Constant TYPEACTION_EXPORT. */
	public static final int TYPEACTION_EXPORT=5;
	
	/** The Constant TYPEACTION_EVALUATE. */
	public static final int TYPEACTION_EVALUATE=6;
	
	/** The Constant STD_BM_ITER. */
	public static final int STD_BM_ITER=12;
	
	/** The Constant STD_ITK_ITER. */
	public static final int STD_ITK_ITER=200;
	
	/** The type opt. */
	public OptimizerType typeOpt=OptimizerType.BLOCKMATCHING;
	
	/** The type auto display. */
	public int typeAutoDisplay=0;
	
	/** The type man viewer. */
	public int typeManViewer=0;	
	
	/** The viewer 3d. */
	public static int VIEWER_3D=0;
	
	/** The viewer 2d. */
	public static int VIEWER_2D=1;
	
	/** The estimated time. */
	public int estimatedTime=0;
	
	/** The step. */
	public int step=0;
	
	/** The type trans. */
	public Transform3DType typeTrans=Transform3DType.RIGID;;
	
	/** The sigma resampling. */
	public double sigmaResampling=0;
	
	/** The sigma dense. */
	public double sigmaDense=0;
	
	/** The level min linear. */
	public int levelMinLinear=2;
	
	/** The level min dense. */
	public int levelMinDense=2;
	
	/** The level max dense. */
	public int levelMaxDense=2;
	
	/** The level max linear. */
	public int levelMaxLinear=2;
	
	/** The higher acc. */
	public int higherAcc=0;
	
	/** The iterations BM lin. */
	public int iterationsBMLin=STD_BM_ITER;
	
	/** The iterations BM den. */
	public int iterationsBMDen=STD_BM_ITER/2;
	
	/** The iterations ITK. */
	public int iterationsITK=STD_ITK_ITER;
	
	/** The neigh X. */
	public int neighX=2;
	
	/** The neigh Y. */
	public int neighY=2;
	
	/** The neigh Z. */
	public int neighZ=0;
	
	/** The bhs X. */
	public int bhsX=7;
	
	/** The bhs Y. */
	public int bhsY=7;
	
	/** The bhs Z. */
	public int bhsZ=1;
	
	/** The stride X. */
	public int strideX=3;
	
	/** The stride Y. */
	public int strideY=3;
	
	/** The stride Z. */
	public int strideZ=3;
	
	/** The select score. */
	public int selectScore=95;
	
	/** The select LTS. */
	public int selectLTS=80;
	
	/** The select random. */
	public int selectRandom=100;
	
	/** The subsample Z. */
	public int subsampleZ=0;
	
	/** The itk optimizer type. */
	public OptimizerType itkOptimizerType=OptimizerType.ITK_AMOEBA;
	
	/** The learning rate. */
	public double learningRate=0.3;
	
	/** The param opt. */
	public double paramOpt=8;

	/**
	 * Instantiates a new registration action.
	 *
	 * @param fijiyamaGui the fijiyama gui
	 * @param regManager the reg manager
	 */
	public RegistrationAction(Fijiyama_GUI fijiyamaGui,RegistrationManager regManager) {
		adjustSettings(fijiyamaGui,regManager);
	}
	
	/**
	 * Gets the step.
	 *
	 * @return the step
	 */
	public int getStep() {
		return step;
	}
	
	/**
	 * Instantiates a new registration action.
	 */
	public RegistrationAction() {}

	
	
	/**
	 * Gets the number.
	 *
	 * @param t the t
	 * @return the number
	 */
	public static int getNumber(Transform3DType t) {
		switch (t) {
		case TRANSLATION : return 1;
		case EULER2D : return 2;
		case VERSOR : return 3;
		case EULER : return 4;
		case RIGID : return 5;
		case SIMILARITY : return 6;
		case AFFINE : return 7;
		case DENSE : return 8;
		default : return 9;
		}
	}
	
	/**
	 * Checks if is valid order.
	 *
	 * @param r1 the r 1
	 * @param r2 the r 2
	 * @return true, if is valid order
	 */
	public static boolean isValidOrder(RegistrationAction r1, RegistrationAction r2) {
		if(r1.typeAction > r2.typeAction)return false;
		if(r1.typeAction < r2.typeAction)return true;
		else return getNumber(r1.typeTrans)<=getNumber(r2.typeTrans);
	}
	
	
	/**
	 * Checks if is valid order.
	 *
	 * @param t1 the t 1
	 * @param t2 the t 2
	 * @return true, if is valid order
	 */
	public static boolean isValidOrder(Transform3DType t1, Transform3DType t2) {
		return getNumber(t1)<=getNumber(t2);
	}
	
	
	/**
	 * Instantiates a new registration action.
	 *
	 * @param regAct the reg act
	 */
	public RegistrationAction(RegistrationAction regAct) {
		nameAction=regAct.nameAction;
		nameSubject=regAct.nameSubject;
		refTime=regAct.refTime;
		refMod=regAct.refMod;
		movTime=regAct.movTime;
		movMod=regAct.movMod;
		typeAction=regAct.typeAction;
		typeOpt=regAct.typeOpt;
		typeAutoDisplay=regAct.typeAutoDisplay;
		typeManViewer=regAct.typeManViewer;
		estimatedTime=regAct.estimatedTime;
		step=regAct.step;
		typeTrans=regAct.typeTrans;
		sigmaResampling=regAct.sigmaResampling;
		sigmaDense=regAct.sigmaDense;
		levelMinLinear=regAct.levelMinLinear;
		levelMinDense=regAct.levelMinDense;
		levelMaxDense=regAct.levelMaxDense;
		levelMaxLinear=regAct.levelMaxLinear;
		higherAcc=regAct.higherAcc;
		iterationsBMLin=regAct.iterationsBMLin;
		iterationsBMDen=regAct.iterationsBMDen;
		iterationsITK=regAct.iterationsITK;
		neighX=regAct.neighX;
		neighY=regAct.neighY;
		neighZ=regAct.neighZ;
		bhsX=regAct.bhsX;
		bhsY=regAct.bhsY;
		bhsZ=regAct.bhsZ;
		strideX=regAct.strideX;
		strideY=regAct.strideY;
		strideZ=regAct.strideZ;
		selectScore=regAct.selectScore;
		selectLTS=regAct.selectLTS;
		selectRandom=regAct.selectRandom;
		subsampleZ=regAct.subsampleZ;
		itkOptimizerType=regAct.itkOptimizerType;
		learningRate=regAct.learningRate;
	}

	
	
	/**
	 * Adjust settings.
	 *
	 * @param fijiyamaGui the fijiyama gui
	 * @param regManager the reg manager
	 */
	public void adjustSettings(Fijiyama_GUI fijiyamaGui,RegistrationManager regManager) {
		this.typeAction=fijiyamaGui.boxTypeAction.getSelectedIndex();
		this.typeTrans=fijiyamaGui.boxTypeTrans.getSelectedIndex()==0 ? Transform3DType.RIGID : fijiyamaGui.boxTypeTrans.getSelectedIndex()==1 ? Transform3DType.SIMILARITY : Transform3DType.DENSE;
		this.typeOpt=fijiyamaGui.boxOptimizer.getSelectedIndex()==0 ? OptimizerType.BLOCKMATCHING : OptimizerType.ITK_AMOEBA;
		this.typeAutoDisplay=fijiyamaGui.boxDisplay.getSelectedIndex();
		this.typeManViewer=fijiyamaGui.boxDisplayMan.getSelectedIndex();
		this.refMod=regManager.refMod;
		this.movMod=regManager.movMod;
		this.refTime=regManager.refTime;
		this.movTime=regManager.movTime;
		if(this.levelMaxLinear>regManager.maxAcceptableLevel)this.levelMaxLinear=regManager.maxAcceptableLevel;
		if(this.levelMaxDense>regManager.maxAcceptableLevel)this.levelMaxDense=regManager.maxAcceptableLevel;
		if(this.typeTrans==Transform3DType.DENSE && this.levelMinDense>this.levelMaxDense) this.levelMinDense=this.levelMaxDense;
		if(( this.typeTrans!=Transform3DType.DENSE ) && this.levelMinLinear>this.levelMaxLinear) this.levelMinLinear=this.levelMaxLinear;			
	}	
	
	/**
	 * Define settings from two images.
	 *
	 * @param imgRef the img ref
	 * @param imgMov the img mov
	 * @return the registration action
	 */
	public RegistrationAction defineSettingsFromTwoImages(ImagePlus imgRef,ImagePlus imgMov) {
		return defineSettingsFromTwoImages(imgRef,imgMov,null,false);
	}
	
	/**
	 * Define settings from two images.
	 *
	 * @param imgRef the img ref
	 * @param imgMov the img mov
	 * @param regManager the reg manager
	 * @param modifyMaxLevelOfManager the modify max level of manager
	 * @return the registration action
	 */
	public RegistrationAction defineSettingsFromTwoImages(ImagePlus imgRef,ImagePlus imgMov,RegistrationManager regManager,boolean modifyMaxLevelOfManager) {
		this.selectScore=95;
		this.selectLTS=80;
		this.selectRandom=100;
		int nbStrideAtMaxLevel=30;
		double minSubResolutionImageSizeLog2=5.0;//In power of two : min resolution=64;
		double maxSubResolutionImageSizeLog2=7.0;//In power of two : max resolution=256
		int strideMinZ=3;

		int[]dimsTemp=VitimageUtils.getDimensions(imgRef);
		double[]voxsTemp=VitimageUtils.getVoxelSizes(imgRef);
		double[]sizesTemp=new double[] {dimsTemp[0]*voxsTemp[0],dimsTemp[1]*voxsTemp[1],dimsTemp[2]*voxsTemp[2]};				
		sigmaDense=sizesTemp[0]/12;//Default : gaussian kernel for dense field estimation is 12 times smaller than image
		double anisotropyVox=voxsTemp[2]/Math.max(voxsTemp[1],voxsTemp[0]);
		this.levelMinLinear=0;
		this.levelMinDense=0;
		this.levelMaxLinear=0;
		this.levelMaxDense=0;
		boolean subZ=false;
		iterationsBMLin=STD_BM_ITER;
		iterationsBMDen=STD_BM_ITER/2;
		iterationsITK=STD_ITK_ITER;
		neighX=2;
		neighY=2;
		neighZ=2;
		if((dimsTemp[2]>=5) && (anisotropyVox<3)) {//Cas 3D pur
			subZ=true;
			int []dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-minSubResolutionImageSizeLog2),
						              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-minSubResolutionImageSizeLog2),
					              	  (int)Math.floor(Math.log(dimsTemp[2])/Math.log(2)-minSubResolutionImageSizeLog2)};
			levelMaxLinear=Math.min(Math.min(dimsLog2[0], dimsLog2[1]), dimsLog2[2]);
			dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-maxSubResolutionImageSizeLog2),
		              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-maxSubResolutionImageSizeLog2),
	              	  (int)Math.floor(Math.log(dimsTemp[2])/Math.log(2)-maxSubResolutionImageSizeLog2)};
			levelMinLinear=Math.max(Math.max(dimsLog2[0], dimsLog2[1]), dimsLog2[2]);	
			if(levelMinLinear>levelMaxLinear)levelMinLinear=levelMaxLinear;
			subsampleZ=1;
			higherAcc=levelMinLinear<1 ? 1 : 0;
			levelMinLinear=levelMinLinear<1 ? 1 : levelMinLinear;
			levelMaxLinear= levelMaxLinear<levelMinLinear ? levelMinLinear : levelMaxLinear;
		}
		else {	
			//If dimZ<5, case 2D --> no subsampleZ, levelMin and max defined using dimX and dimY, neighZ=0 BHSZ=0 strideZ=1;
			//else if anisotropyVox>3 -> no subsampleZ levelMin and max defined using dimX and dimY, neighZ=3 BHSZ=prop strideZ=prop;
			int []dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-minSubResolutionImageSizeLog2),
		              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-minSubResolutionImageSizeLog2) };
			levelMaxLinear=Math.min(dimsLog2[0], dimsLog2[1]);
			dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-maxSubResolutionImageSizeLog2),
			    (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-maxSubResolutionImageSizeLog2)};
			levelMinLinear=Math.max(dimsLog2[0], dimsLog2[1]);	
			if(levelMinLinear>levelMaxLinear)levelMinLinear=levelMaxLinear;
			subsampleZ=0;
			higherAcc=levelMinLinear<1 ? 1 : 0;
			levelMinLinear=levelMinLinear<1 ? 1 : levelMinLinear;
			levelMaxLinear= levelMaxLinear<levelMinLinear ? levelMinLinear : levelMaxLinear;
		}
		int subFactorMin=(int)Math.round(Math.pow(2, -1+Math.max(1,levelMinLinear)));
		int []targetDimsLevelMin=new int[] {dimsTemp[0]/subFactorMin,dimsTemp[1]/subFactorMin,dimsTemp[2]/(subZ ? subFactorMin : 1)};

		int[]strides=new int[] { (int) Math.round(Math.max(1,Math.ceil(targetDimsLevelMin[0]/nbStrideAtMaxLevel))),
								 (int) Math.round(Math.max(1,Math.ceil(targetDimsLevelMin[1]/nbStrideAtMaxLevel))),
								 (int) Math.round(Math.max(strideMinZ,Math.ceil(targetDimsLevelMin[2]/nbStrideAtMaxLevel))) };
		strideX=strides[0];
		strideY=strides[1];
		strideZ=strides[2];
		bhsX=(int) Math.round(Math.min(7,Math.max(strides[0],3)));
		bhsY=(int) Math.round(Math.min(7,Math.max(strides[1],3)));
		bhsZ=(int) Math.round(Math.min(7,Math.max(strides[2],3)));
		if(dimsTemp[2]<5) {//cas 2D
			neighZ=0;
			bhsZ=0;
			strideZ=1;
		}
		if(modifyMaxLevelOfManager){
			regManager.maxAcceptableLevel=levelMaxLinear;			
			if(this.levelMaxLinear>regManager.maxAcceptableLevel)this.levelMaxLinear=regManager.maxAcceptableLevel;
			if(this.levelMinLinear>this.levelMaxLinear)this.levelMinLinear=this.levelMaxLinear;
		}
		if(this.levelMaxLinear>1)this.levelMaxDense=this.levelMaxLinear-1;
		else this.levelMaxDense=1;
		this.levelMinDense=this.levelMinLinear;
		return this;
	}

	
	/**
	 * Define settings for RSML.
	 *
	 * @param imgRef the img ref
	 * @return the registration action
	 */
	public static RegistrationAction defineSettingsForRSML(ImagePlus imgRef) {
		RegistrationAction regAct=new RegistrationAction();
		int X=imgRef.getWidth();
		int levelImage=(X>=1024 ? 1 : 0);
		regAct.selectScore=80;
		regAct.selectLTS=80;
		regAct.selectRandom=100;
	    regAct.setActionTo(1);
	    regAct.typeTrans=Transform3DType.DENSE;
	    regAct.typeAutoDisplay=0;
	    regAct.setLevelMinNonLinear(0);
	    regAct.setLevelMaxNonLinear(levelImage);
	    regAct.strideX=2+levelImage;
	    regAct.strideY=2+levelImage;
	    regAct.strideZ=1;
	    regAct.neighX=3;
	    regAct.neighY=3;
	    regAct.neighZ=0;
	    regAct.bhsX=3+2*levelImage;
	    regAct.bhsY=3+2*levelImage;
	    regAct.bhsZ=0;
	    regAct.setIterationsBMNonLinear(20);
	    regAct.sigmaDense=VitimageUtils.getVoxelSizes(imgRef)[0]/100.0*imgRef.getWidth();
	    return regAct;
	}
	
	
	
	/**
	 * Define settings simply from two images.
	 *
	 * @param imgRef the img ref
	 * @param imgMov the img mov
	 * @return the registration action
	 */
	public RegistrationAction defineSettingsSimplyFromTwoImages(ImagePlus imgRef,ImagePlus imgMov) {
		this.selectScore=95;
		this.selectLTS=80;
		this.selectRandom=100;
		int nbStrideAtMaxLevel=30;
		double minSubResolutionImageSizeLog2=5.0;//In power of two : min resolution=64;
		double maxSubResolutionImageSizeLog2=7.0;//In power of two : max resolution=256
		int strideMinZ=3;

		int[]dimsTemp=VitimageUtils.getDimensions(imgRef);
		double[]voxsTemp=VitimageUtils.getVoxelSizes(imgRef);
		double[]sizesTemp=new double[] {dimsTemp[0]*voxsTemp[0],dimsTemp[1]*voxsTemp[1],dimsTemp[2]*voxsTemp[2]};				
		sigmaDense=sizesTemp[0]/12;//Default : gaussian kernel for dense field estimation is 12 times smaller than image
		double anisotropyVox=voxsTemp[2]/Math.max(voxsTemp[1],voxsTemp[0]);
		this.levelMinLinear=0;
		this.levelMinDense=0;
		this.levelMaxLinear=0;
		this.levelMaxDense=0;
		boolean subZ=false;
		iterationsBMLin=STD_BM_ITER;
		iterationsBMDen=STD_BM_ITER/2;
		iterationsITK=STD_ITK_ITER;
		neighX=2;
		neighY=2;
		neighZ=2;
		if((dimsTemp[2]>=5) && (anisotropyVox<3)) {//Cas 3D pur
			subZ=true;
			int []dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-minSubResolutionImageSizeLog2),
						              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-minSubResolutionImageSizeLog2),
					              	  (int)Math.floor(Math.log(dimsTemp[2])/Math.log(2)-minSubResolutionImageSizeLog2)};
			levelMaxLinear=Math.min(Math.min(dimsLog2[0], dimsLog2[1]), dimsLog2[2]);
			dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-maxSubResolutionImageSizeLog2),
		              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-maxSubResolutionImageSizeLog2),
	              	  (int)Math.floor(Math.log(dimsTemp[2])/Math.log(2)-maxSubResolutionImageSizeLog2)};
			levelMinLinear=Math.max(Math.max(dimsLog2[0], dimsLog2[1]), dimsLog2[2]);	
			if(levelMinLinear>levelMaxLinear)levelMinLinear=levelMaxLinear;
			subsampleZ=1;
			higherAcc=levelMinLinear<1 ? 1 : 0;
			levelMinLinear=levelMinLinear<1 ? 1 : levelMinLinear;
			levelMaxLinear= levelMaxLinear<levelMinLinear ? levelMinLinear : levelMaxLinear;
		}
		else {	
			//If dimZ<5, case 2D --> no subsampleZ, levelMin and max defined using dimX and dimY, neighZ=0 BHSZ=0 strideZ=1;
			//else if anisotropyVox>3 -> no subsampleZ levelMin and max defined using dimX and dimY, neighZ=3 BHSZ=prop strideZ=prop;
			int []dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-minSubResolutionImageSizeLog2),
		              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-minSubResolutionImageSizeLog2) };
			levelMaxLinear=Math.min(dimsLog2[0], dimsLog2[1]);
			dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-maxSubResolutionImageSizeLog2),
			    (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-maxSubResolutionImageSizeLog2)};
			levelMinLinear=Math.max(dimsLog2[0], dimsLog2[1]);	
			if(levelMinLinear>levelMaxLinear)levelMinLinear=levelMaxLinear;
			subsampleZ=0;
			higherAcc=levelMinLinear<1 ? 1 : 0;
			levelMinLinear=levelMinLinear<1 ? 1 : levelMinLinear;
			levelMaxLinear= levelMaxLinear<levelMinLinear ? levelMinLinear : levelMaxLinear;
		}
		int subFactorMin=(int)Math.round(Math.pow(2, -1+Math.max(1,levelMinLinear)));
		int []targetDimsLevelMin=new int[] {dimsTemp[0]/subFactorMin,dimsTemp[1]/subFactorMin,dimsTemp[2]/(subZ ? subFactorMin : 1)};

		int[]strides=new int[] { (int) Math.round(Math.max(1,Math.ceil(targetDimsLevelMin[0]/nbStrideAtMaxLevel))),
								 (int) Math.round(Math.max(1,Math.ceil(targetDimsLevelMin[1]/nbStrideAtMaxLevel))),
								 (int) Math.round(Math.max(strideMinZ,Math.ceil(targetDimsLevelMin[2]/nbStrideAtMaxLevel))) };
		strideX=strides[0];
		strideY=strides[1];
		strideZ=strides[2];
		bhsX=(int) Math.round(Math.min(7,Math.max(strides[0],3)));
		bhsY=(int) Math.round(Math.min(7,Math.max(strides[1],3)));
		bhsZ=(int) Math.round(Math.min(7,Math.max(strides[2],3)));
		if(dimsTemp[2]<5) {//cas 2D
			neighZ=0;
			bhsZ=0;
			strideZ=1;
		}
		if(this.levelMaxLinear>1)this.levelMaxDense=this.levelMaxLinear-1;
		else this.levelMaxDense=1;
		this.levelMinDense=this.levelMinLinear;
		return this;
	}

	
	
	
	
	
	/**
	 * Update fields from boxes.
	 *
	 * @param actionSelectedIndex the action selected index
	 * @param transSelectedIndex the trans selected index
	 * @param optimizerSelectedIndex the optimizer selected index
	 * @param displaySelectedIndex the display selected index
	 * @param viewerManSelectedIndex the viewer man selected index
	 * @param modeWindow the mode window
	 */
	public void updateFieldsFromBoxes(int actionSelectedIndex,int transSelectedIndex,int optimizerSelectedIndex,int displaySelectedIndex,int viewerManSelectedIndex,int modeWindow) {
		this.typeAction=actionSelectedIndex;
		this.typeTrans=(transSelectedIndex==0 ? Transform3DType.RIGID : transSelectedIndex==1 ? Transform3DType.SIMILARITY : Transform3DType.DENSE);
		this.typeOpt=(optimizerSelectedIndex==0 ? OptimizerType.BLOCKMATCHING : OptimizerType.ITK_AMOEBA);
		this.typeAutoDisplay=displaySelectedIndex;
		this.typeManViewer=viewerManSelectedIndex;
		if(modeWindow==Fijiyama_GUI.WINDOWTWOIMG) {
			if(this.typeAction==TYPEACTION_ALIGN) {
				this.movMod=0;
				this.movTime=0;
				this.refMod=0;
				this.refTime=0;
			}
			else{
				this.movMod=1;
				this.movTime=0;
				this.refMod=0;
				this.refTime=0;
			}
		}
	}

	/**
	 * Checks if is done.
	 *
	 * @return true, if is done
	 */
	public boolean isDone() {
		return isDone;
	}
	
	/**
	 * Sets the done.
	 */
	public void setDone() {
		isDone=true;
	}
	
	/**
	 * Sets the undone.
	 */
	public void setUndone() {
		isDone=false;
	}
	
	/**
	 * Checks if is abortable.
	 *
	 * @return true, if is abortable
	 */
	public boolean isAbortable() {
		return (this.isTransformationAction() || this.typeAction==TYPEACTION_VIEW);
	}
	
	/**
	 * To string.
	 *
	 * @return the string
	 */
	public String toString() {
		return readableString();
	}
	
	/**
	 * Sets the step to.
	 *
	 * @param step the step
	 * @return the registration action
	 */
	public RegistrationAction setStepTo(int step) {
		this.step=step;
		return this;
	}
	
	/**
	 * Checks if is transformation action.
	 *
	 * @return true, if is transformation action
	 */
	public boolean isTransformationAction() {
		return ( (typeAction==TYPEACTION_ALIGN) || (typeAction==TYPEACTION_AUTO) || (typeAction==TYPEACTION_MAN) );
	}
	
	/**
	 * Readable string.
	 *
	 * @return the string
	 */
	public String readableString() {
		return readableString(true);
	}

	/**
	 * Readable string.
	 *
	 * @param withTodoDone the with todo done
	 * @return the string
	 */
	public String readableString(boolean withTodoDone) {
		String str="";
		if(withTodoDone)str+="<"+(isDone ? " Done   " : " To do ")+"> ";
		str+="Step "+step+"   ";
		switch(typeAction) {
		case TYPEACTION_MAN: str+="Manual reg."  ;break;
		case TYPEACTION_AUTO: str+="Auto. reg.  ";break;
		case TYPEACTION_ALIGN: str+="Align. axis  ";break;
		case TYPEACTION_VIEW: str+="View results  ";break;
		case TYPEACTION_SAVE: str+="Save actions and results  ";break;
		case TYPEACTION_EXPORT: str+="Export results  ";break;
		case TYPEACTION_EVALUATE: str+="Evaluate alignment  ";break;
		default : str+="Unknown operation  ";break;
		}
		if (this.isTransformationAction()) {
			str+="Transform="+typeTrans+" Mov=t"+movTime+"_mod"+movMod+" -> Ref=t"+refTime+"_mod"+refMod+"   ";
		}
		if (typeAction==TYPEACTION_MAN || typeAction==TYPEACTION_ALIGN) {
			str+=(typeManViewer==VIEWER_3D ? "in 3d-viewer" : "in 2d-viewer");
		}
		if (typeAction==TYPEACTION_AUTO) {
			str+=( (typeOpt==OptimizerType.BLOCKMATCHING)?" BM" : " ITK");
			str+=" levs="+(typeTrans==Transform3DType.DENSE ? (levelMaxDense+"->"+(higherAcc==1 ? -1 : levelMinDense)) : (levelMaxLinear+"->"+(higherAcc==1 ? -1 : levelMinLinear))); 
			if (typeTrans==Transform3DType.DENSE) str+=" sigma="+sigmaDense;
			if (typeOpt==OptimizerType.BLOCKMATCHING) {
				str+=" it="+(typeTrans==Transform3DType.DENSE ? iterationsBMDen: iterationsBMLin)+" bh="+bhsX+" nei="+neighX+" strd="+strideX;
			}
			else {
				str+=" it="+iterationsITK+" lr="+learningRate;
			}
			str+=typeAutoDisplay==0 ? " noDisp" : typeAutoDisplay==1 ? "DispFus" : "DispAll"; 
		}
		return str;
	}


	/**
	 * Creates the registration action.
	 *
	 * @param imgRef the img ref
	 * @param imgMov the img mov
	 * @param fijiyamaGui the fijiyama gui
	 * @param regManager the reg manager
	 * @param typeAction2 the type action 2
	 * @return the registration action
	 */
	public static RegistrationAction createRegistrationAction(ImagePlus imgRef, ImagePlus imgMov, Fijiyama_GUI fijiyamaGui,RegistrationManager regManager,int typeAction2) {
		RegistrationAction reg=new RegistrationAction(fijiyamaGui,regManager);
		reg.defineSettingsFromTwoImages(imgRef, imgMov, regManager, false);
		reg.typeAction=typeAction2;
		return reg;
	}


	/**
	 * Copy with modified elements.
	 *
	 * @param registrationAction the registration action
	 * @param refTime2 the ref time 2
	 * @param refMod2 the ref mod 2
	 * @param movTime2 the mov time 2
	 * @param movMod2 the mov mod 2
	 * @param step2 the step 2
	 * @return the registration action
	 */
	public static RegistrationAction copyWithModifiedElements(RegistrationAction registrationAction, int refTime2,int refMod2, int movTime2, int movMod2, int step2) {
		RegistrationAction reg=new RegistrationAction(registrationAction);
		reg.refTime=refTime2;
		reg.movTime=movTime2;
		reg.refMod=refMod2;
		reg.movMod=movMod2;
		reg.step=step2;
		return reg;
	}
	
	/**
	 * Sets the action to.
	 *
	 * @param typeAction2 the type action 2
	 * @return the registration action
	 */
	public RegistrationAction setActionTo(int typeAction2) {
		this.typeAction=typeAction2;
		if(typeAction2==RegistrationAction.TYPEACTION_ALIGN)this.typeManViewer=VIEWER_3D;
		return this;
	}
	
	
	
	/**
	 * Full length description.
	 *
	 * @return the string
	 */
	public String fullLengthDescription() {
		String str="#Version="+this.currentSerialVersionUID+"\n";
		str+="#IsDone="+(isDone ? 1 : 0)+"\n";
		str+="#Step="+step+"\n";
		str+="#NameAction="+nameAction+"\n";
		str+="#NameSubject="+nameSubject+"\n";
		str+="#RefTime="+refTime+"\n";
		str+="#RefMod="+refMod+"\n";
		str+="#MovTime="+movTime+"\n";
		str+="#MovMod="+movMod+"\n";
		str+="#TypeAction="+typeAction+"\n";
		str+="#OptimizerType="+(typeOpt==OptimizerType.BLOCKMATCHING ? 0 : 1)+"\n";
		str+="#TypeAutoDisplay="+typeAutoDisplay+"\n";
		str+="#TypeManViewer="+typeManViewer+"\n";
		str+="#EstimatedTime="+estimatedTime+"\n";
		str+="#TypeTrans="+(typeTrans==Transform3DType.RIGID ? 0 : typeTrans==Transform3DType.SIMILARITY ? 1 : 2)+"\n";
		str+="#ParamOpt="+paramOpt+"\n";
		str+="#SigmaDense="+sigmaDense+"\n";
		str+="#LevelMinLinear="+levelMinLinear+"\n";
		str+="#LevelMaxLinear="+levelMaxLinear+"\n";
		str+="#HigherAcc="+higherAcc+"\n";
		str+="#IterationsBMLinear="+iterationsBMLin+"\n";
		str+="#IterationsITK="+iterationsITK+"\n";
		str+="#NeighX="+neighX+"\n";
		str+="#NeighY="+neighY+"\n";
		str+="#NeighZ="+neighZ+"\n";
		str+="#BhsX="+bhsX+"\n";
		str+="#BhsY="+bhsY+"\n";
		str+="#BhsZ="+bhsZ+"\n";
		str+="#StrideX="+strideX+"\n";
		str+="#StrideY="+strideY+"\n";
		str+="#StrideZ="+strideZ+"\n";
		str+="#SelectScore="+selectScore+"\n";
		str+="#SelectLTS="+selectLTS+"\n";
		str+="#SelectRandom="+selectRandom+"\n";
		str+="#SubsampleZ="+subsampleZ+"\n";
		str+="#ItkOptimizerType="+(itkOptimizerType==OptimizerType.ITK_AMOEBA ? 0 : 1)+"\n";
		str+="#LearningRate="+learningRate+"\n";
		str+="#LevelMaxDense="+levelMaxDense+"\n";
		str+="#IterationsBMDense="+iterationsBMDen+"\n";
		str+="#LevelMinDense="+levelMinDense+"\n";
		return str;
	}


	/**
	 * Write to txt file.
	 *
	 * @param path the path
	 */
	// Serialization in text file  
	public void writeToTxtFile(String path) {
		String str=fullLengthDescription();
		VitimageUtils.writeStringInFile(str,path);
	}

	/**
	 * Read from txt file.
	 *
	 * @param path the path
	 * @return the registration action
	 */
	public static RegistrationAction readFromTxtFile(String path) {
		RegistrationAction reg=new RegistrationAction();
		String str=VitimageUtils.readStringFromFile(path);
		String[]lines=str.split("\n");
		for(int indStr=1 ;  indStr<lines.length;indStr++)lines[indStr]=lines[indStr].split("=")[1];
		reg.currentSerialVersionUID=500;
		if(lines.length<40) IJ.showMessage("Warning : reading a deprecated registration file. Unexpected behaviour can happen, possibly a crash");
		else reg.currentSerialVersionUID=600;
		if( lines[0].length()>1)reg.currentSerialVersionUID=Integer.parseInt(lines[0].split("=")[1]);
		reg.isDone=Integer.parseInt(lines[1])==1;
		reg.step=Integer.parseInt(lines[2]);
		reg.nameAction=lines[3];
		reg.nameSubject=lines[4];
		reg.refTime=Integer.parseInt(lines[5]);
		reg.refMod=Integer.parseInt(lines[6]);
		reg.movTime=Integer.parseInt(lines[7]);
		reg.movMod=Integer.parseInt(lines[8]);
		reg.typeAction=Integer.parseInt(lines[9]);
		reg.typeOpt= Integer.parseInt(lines[10])==0 ? OptimizerType.BLOCKMATCHING : OptimizerType.ITK_AMOEBA;
		reg.typeAutoDisplay =Integer.parseInt(lines[11]);
		reg.typeManViewer=Integer.parseInt(lines[12]);
		reg.estimatedTime=Integer.parseInt(lines[13]);
		reg.typeTrans=Integer.parseInt(lines[14])==0 ? Transform3DType.RIGID : Integer.parseInt(lines[14])==1 ? Transform3DType.SIMILARITY : Transform3DType.DENSE;
		reg.paramOpt=Double.parseDouble(lines[15]);
		reg.sigmaDense=Double.parseDouble(lines[16]);
		reg.levelMinLinear =Integer.parseInt(lines[17]);
		reg.higherAcc =Integer.parseInt(lines[19]);
		reg.iterationsITK  =Integer.parseInt(lines[21]);
		reg.neighX  =Integer.parseInt(lines[22]);
		reg.neighY  =Integer.parseInt(lines[23]);
		reg.neighZ =Integer.parseInt(lines[24]);
		reg.bhsX =Integer.parseInt(lines[25]);
		reg.bhsY =Integer.parseInt(lines[26]);
		reg.bhsZ =Integer.parseInt(lines[27]);
		reg.strideX  =Integer.parseInt(lines[28]);
		reg.strideY =Integer.parseInt(lines[29]);
		reg.strideZ =Integer.parseInt(lines[30]);
		reg.selectScore  =Integer.parseInt(lines[31]);
		reg.selectLTS =Integer.parseInt(lines[32]);
		reg.selectRandom =Integer.parseInt(lines[33]);
		reg.subsampleZ  =Integer.parseInt(lines[34]);
		reg.itkOptimizerType=Integer.parseInt(lines[35])==0 ? OptimizerType.ITK_AMOEBA : null;
		reg.levelMaxLinear=Integer.parseInt(lines[18]);
		reg.iterationsBMLin=Integer.parseInt(lines[20]);
		if(reg.currentSerialVersionUID>=RegistrationAction.serialFelicityFicus) {
			reg.learningRate=Double.parseDouble(lines[36]);
			reg.levelMaxDense=Integer.parseInt(lines[37]);
			reg.iterationsBMDen=Integer.parseInt(lines[38]);
			reg.levelMinDense=Integer.parseInt(lines[39]);
		}
		else {
			reg.learningRate=0.3;
			reg.levelMaxDense=2;
			reg.iterationsBMDen=6;
			reg.levelMinDense=1;
		}
		return reg;
	}
	
	
	/**
	 * Gets the level max.
	 *
	 * @return the level max
	 */
	public int getLevelMax() {
		if(typeTrans==Transform3DType.DENSE) return levelMaxDense;
		else return levelMaxLinear;
	}
	
	/**
	 * Gets the level min.
	 *
	 * @return the level min
	 */
	public int getLevelMin() {
		if(typeTrans==Transform3DType.DENSE) return levelMinDense;
		else return levelMinLinear;
	}
	
	

	/**
	 * Gets the iterations BM.
	 *
	 * @return the iterations BM
	 */
	public int getIterationsBM() {
		if(typeTrans==Transform3DType.DENSE) return iterationsBMDen;
		else return iterationsBMLin;
	}
	
	/**
	 * Gets the iterations BM linear.
	 *
	 * @return the iterations BM linear
	 */
	public int getIterationsBMLinear() {
		return iterationsBMLin;
	}
	
	/**
	 * Gets the iterations BM non linear.
	 *
	 * @return the iterations BM non linear
	 */
	public int getIterationsBMNonLinear() {
		return iterationsBMDen;
	}
	
	/**
	 * Sets the level max.
	 *
	 * @param lev the new level max
	 */
	public void setLevelMax(int lev) {
		if(typeTrans==Transform3DType.DENSE) {
			levelMaxDense=lev;
		}
		else {
			levelMaxLinear=lev;
		}
	}
	
	/**
	 * Sets the level max linear.
	 *
	 * @param lev the new level max linear
	 */
	public void setLevelMaxLinear(int lev) {
		levelMaxLinear=lev;
	}

	/**
	 * Sets the level max non linear.
	 *
	 * @param lev the new level max non linear
	 */
	public void setLevelMaxNonLinear(int lev) {
		levelMaxDense=lev;
	}

	/**
	 * Sets the level min linear.
	 *
	 * @param lev the new level min linear
	 */
	public void setLevelMinLinear(int lev) {
		levelMinLinear=lev;
	}

	/**
	 * Sets the level min non linear.
	 *
	 * @param lev the new level min non linear
	 */
	public void setLevelMinNonLinear(int lev) {
		levelMinDense=lev;
	}
	
	/**
	 * Sets the level min.
	 *
	 * @param lev the new level min
	 */
	public void setLevelMin(int lev) {
		if(typeTrans==Transform3DType.DENSE) {
			levelMinDense=lev;
		}
		else {
			levelMinLinear=lev;
		}
	}
	

	/**
	 * Sets the iterations BM.
	 *
	 * @param it the new iterations BM
	 */
	public void setIterationsBM(int it) {
		if(typeTrans==Transform3DType.DENSE) iterationsBMDen=it;
		else iterationsBMLin=it;
	}
	
	
	/**
	 * Sets the iterations BM linear.
	 *
	 * @param it the new iterations BM linear
	 */
	public void setIterationsBMLinear(int it) {
		iterationsBMLin=it;
	}
	
	
	/**
	 * Sets the iterations BM non linear.
	 *
	 * @param it the new iterations BM non linear
	 */
	public void setIterationsBMNonLinear(int it) {
		iterationsBMDen=it;
	}
	
	

	
	
	
	
	
	
	
	
	
	
}
