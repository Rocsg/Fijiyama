package com.vitimage.fijiyama;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.vitimage.common.VitimageUtils;
import com.vitimage.registration.OptimizerType;
import com.vitimage.registration.Transform3DType;

import ij.IJ;
import ij.ImagePlus;

public class RegistrationAction implements Serializable{
	private static final long serialVersionUID = 6L;
	private boolean isDone=false;
	public String nameAction;
	public String nameSubject;
	public int refTime=0;
	public int refMod=0;
	public int movTime=0;
	public int movMod=0;
	public int typeAction=0;
	public static final int TYPEACTION_MAN=0;
	public static final int TYPEACTION_AUTO=1;
	public static final int TYPEACTION_ALIGN=2;
	public static final int TYPEACTION_VIEW=3;
	public static final int TYPEACTION_SAVE=4;
	public static final int TYPEACTION_EXPORT=5;
	public static final int TYPEACTION_EVALUATE=6;
	public static final int STD_BM_ITER=12;
	public static final int STD_ITK_ITER=200;
	public OptimizerType typeOpt=OptimizerType.BLOCKMATCHING;
	public int typeAutoDisplay=0;
	public int typeManViewer=0;	
	public static int VIEWER_3D=0;
	public static int VIEWER_2D=1;
	public int estimatedTime=0;
	public int step=0;
	public Transform3DType typeTrans=Transform3DType.RIGID;;
	public double sigmaResampling=0;
	public double sigmaDense=0;
	public int levelMinLinear=2;
	public int levelMinDense=2;
	public int levelMaxDense=2;
	public int levelMaxLinear=2;
	public int higherAcc=0;
	public int iterationsBMLin=STD_BM_ITER;
	public int iterationsBMDen=STD_BM_ITER/2;
	public int iterationsITK=STD_ITK_ITER;
	public int neighX=2;
	public int neighY=2;
	public int neighZ=0;
	public int bhsX=7;
	public int bhsY=7;
	public int bhsZ=1;
	public int strideX=3;
	public int strideY=3;
	public int strideZ=3;
	public int selectScore=95;
	public int selectLTS=80;
	public int selectRandom=100;
	public int subsampleZ=0;
	public OptimizerType itkOptimizerType=OptimizerType.ITK_AMOEBA;
	public double learningRate=0.3;

	public RegistrationAction(Fijiyama_GUI fijiyamaGui,RegistrationManager regManager) {
		adjustSettings(fijiyamaGui,regManager);
	}
	
	public int getStep() {
		return step;
	}
	
	public RegistrationAction() {}

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

	public boolean isDone() {
		return isDone;
	}
	
	public void setDone() {
		isDone=true;
	}
	
	public void setUndone() {
		isDone=false;
	}
	
	public boolean isAbortable() {
		return (this.isTransformationAction() || this.typeAction==TYPEACTION_VIEW);
	}
	
	public String toString() {
		return readableString();
	}
	
	public RegistrationAction setStepTo(int step) {
		this.step=step;
		return this;
	}
	
	public boolean isTransformationAction() {
		return ( (typeAction==TYPEACTION_ALIGN) || (typeAction==TYPEACTION_AUTO) || (typeAction==TYPEACTION_MAN) );
	}
	
	public String readableString() {
		return readableString(true);
	}

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


	public static RegistrationAction createRegistrationAction(ImagePlus imgRef, ImagePlus imgMov, Fijiyama_GUI fijiyamaGui,RegistrationManager regManager,int typeAction2) {
		RegistrationAction reg=new RegistrationAction(fijiyamaGui,regManager);
		reg.defineSettingsFromTwoImages(imgRef, imgMov, regManager, false);
		reg.typeAction=typeAction2;
		return reg;
	}


	public static RegistrationAction copyWithModifiedElements(RegistrationAction registrationAction, int refTime2,int refMod2, int movTime2, int movMod2, int step2) {
		RegistrationAction reg=new RegistrationAction(registrationAction);
		reg.refTime=refTime2;
		reg.movTime=movTime2;
		reg.refMod=refMod2;
		reg.movMod=movMod2;
		reg.step=step2;
		return reg;
	}
	
	public RegistrationAction setActionTo(int typeAction2) {
		this.typeAction=typeAction2;
		return this;
	}
	
	
	
	

	// Serialization  
	public void writeToFile(String path) {
	    try    {    
	        FileOutputStream file = new FileOutputStream(path); 
	        ObjectOutputStream out = new ObjectOutputStream(file); 
	        out.writeObject(this); 
	        out.close(); 
	        file.close(); 
	    }    catch(IOException ex) {         IJ.log("IOException has been caught during writing to "+path);     } 
	}

	public static RegistrationAction readFromFile(String path) {
		RegistrationAction reg=null;
		try    {    
	        FileInputStream file = new FileInputStream(path); 
	        ObjectInputStream in = new ObjectInputStream(file); 
	        reg = (RegistrationAction)in.readObject();        
	        in.close(); 
	        file.close(); 
	    }       catch(IOException ex)     {      IJ.log("IOException has been caught during reading from "+path);    } 
	    		catch(ClassNotFoundException ex)     {        IJ.log("ClassNotFoundException has been caught during reading from "+path);    } 
	    return reg;
	}
	
	

	// Serialization in text file  
	public void writeToTxtFile(String path) {
		String str="#\n";
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
		str+="#SigmaResampling="+sigmaResampling+"\n";
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
		VitimageUtils.writeStringInFile(str,path);
	}

	public static RegistrationAction readFromTxtFile(String path) {
		RegistrationAction reg=new RegistrationAction();
		String str=VitimageUtils.readStringFromFile(path);
		String[]lines=str.split("\n");
		for(int indStr=1 ;  indStr<lines.length;indStr++)lines[indStr]=lines[indStr].split("=")[1];
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
		reg.sigmaResampling=Double.parseDouble(lines[15]);
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
		reg.learningRate=Double.parseDouble(lines[36]);
		reg.levelMaxLinear=Integer.parseInt(lines[18]);
		reg.levelMaxDense=Integer.parseInt(lines[37]);
		reg.iterationsBMLin=Integer.parseInt(lines[20]);
		reg.iterationsBMDen=Integer.parseInt(lines[38]);
		reg.levelMinDense=Integer.parseInt(lines[39]);
		return reg;
	}
	
	
	public int getLevelMax() {
		if(typeTrans==Transform3DType.DENSE) return levelMaxDense;
		else return levelMaxLinear;
	}
	
	public int getLevelMin() {
		if(typeTrans==Transform3DType.DENSE) return levelMinDense;
		else return levelMinLinear;
	}
	
	

	public int getIterationsBM() {
		if(typeTrans==Transform3DType.DENSE) return iterationsBMDen;
		else return iterationsBMLin;
	}
	
	
	
	public void setLevelMax(int lev) {
		if(typeTrans==Transform3DType.DENSE) {
			levelMaxDense=lev;
		}
		else {
			levelMaxLinear=lev;
		}
	}
	
	
	
	public void setLevelMin(int lev) {
		if(typeTrans==Transform3DType.DENSE) {
			levelMinDense=lev;
		}
		else {
			levelMinLinear=lev;
		}
	}
	

	public void setIterationsBM(int it) {
		if(typeTrans==Transform3DType.DENSE) iterationsBMDen=it;
		else iterationsBMLin=it;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
}
