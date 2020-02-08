package com.vitimage.ndimaging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;

import com.vitimage.common.TransformUtils;
import com.vitimage.common.VineType;
import com.vitimage.common.VitiDialogs;
import com.vitimage.common.VitimageUtils;
import com.vitimage.registration.BlockMatchingRegistration;
import com.vitimage.registration.ItkTransform;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;

public class Cep4D implements VitiDialogs,TransformUtils,VitimageUtils{
	public static final int UNTIL_END=1000;
	public final static String slash=File.separator;
	public ComputingType computingType;
	public String title="--";
	public String sourcePath="--";
	public String dataPath="--";
	public VineType vineType=VineType.VINE;
	protected ArrayList<Acquisition> acquisition;//Observations goes there
	protected ArrayList<Geometry> geometry;
	protected ArrayList<Misalignment> misalignment;
	protected ArrayList<ItkTransform> transformation;
	protected ArrayList<Capillary> capillary;
	private int[] referenceImageSize;
	private double[] referenceVoxelSize;
	protected AcquisitionType acquisitionStandardReference=AcquisitionType.RX;
	protected ImagePlus normalizedHyperImage;
	private ImagePlus mask;
	ImagePlus imageForRegistration;
	private String projectName="VITIMAGE";
	private String unit="mm";
	private int hyperSize=0;
	public static final int targetHyperSize=7;
	/**
	 *  Test sequence for the class
	 */
	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		String subject="CEP011_AS1";
		Cep4D cep = new Cep4D(VineType.VINE,0,"/home/fernandr/Bureau/Traitements/Cep5D/"+subject+"/Source_data/",
								subject,ComputingType.COMPUTE_ALL);			
		cep.start(Cep4D.UNTIL_END);
		cep.normalizedHyperImage.show();
	}


	
	/**
	 * Top level functions
	 */
	public void start(int ending) {
		printStartMessage();
		//if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS)writeStep(1);
		quickStartFromFile();
		while(nextStep(ending));
	}
	
	public void printStartMessage() {
		System.out.println("");
		System.out.println("");
		System.out.println("#######################################################################################################################");
		System.out.println("######## Starting new Cep4D ");
		String str=""+this.sourcePath+"";
		System.out.println("######## "+str+"");
		System.out.println("#######################################################################################################################");		
	}

	
	public boolean nextStep(int ending){
		int a=readStep();
		if(a>=ending)return false;
		if (this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS && a>1) {return false;}
		switch(a) {
		case -1:
			if(this.computingType!=ComputingType.EVERYTHING_AFTER_MAPS) {
				IJ.showMessage("Critical fail, no directory found Source_data in the current directory");
				return false;
			};break;
		case 0: // rien --> exit
				IJ.log("No data in this directory");
				return false;		
		case 1://data are read. Time to compute individual calculus for RX and MRI
			for (int i=0;i<2;i++) {System.out.println("\nCep4D, step1 start a new acquisition");this.acquisition.get(i).start();}
			break;
		case 2: //individual computations are done. Time to do photo
			System.out.println("\nCep4D, step2, make photo");
			this.acquisition.get(2).start();
			break;
		case 3: //individual computations are done. Time to register acquisitions
			System.out.println("Vitimage 4D, Computation finished for "+this.getTitle());
			for (Acquisition acq : this.acquisition) {
				long lThis=this.getHyperImageModificationTime();
				long lAcq=acq.getHyperImageModificationTime();
				if(lAcq>lThis) {
					System.out.println("Vit4D HyperImage update : at least one source acquisition hyper image has been modified since last hyperimage modification : " +acq.getTitle());
					writeStep(7);
					return true;
				}			
			}
			return false;
		}
		writeStep(a+1);	
		return true;
	}
	
	
	public void freeMemory(){
		for(Acquisition acq : acquisition) {
			acq.freeMemory();
			acq=null;
		}
		imageForRegistration=null;
		normalizedHyperImage=null;
		mask=null;			
		System.gc();
	}

	public void writeStep(int st) {
		File f = new File(this.getSourcePath(),"STEPS_DONE.tag");
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			out.write("Step="+(st)+"\n");
			out.write("# Last execution time : "+(new Date())+"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write transformation to file: "+f.getAbsolutePath()+"error: "+e);}
	}
	
	public int readStep() {
		String strFile="";
		String line;
		File f = new File(this.getSourcePath(),"STEPS_DONE.tag");
		try {
			BufferedReader in=new BufferedReader(new FileReader(this.getSourcePath()+slash+"STEPS_DONE.tag"));
			while ((line = in.readLine()) != null) {
				strFile+=line+"\n";
			}
        } catch (IOException ex) { ex.printStackTrace();  strFile="None\nNone";        }	
		String[]strLines=strFile.split("\n");
		String st=strLines[0].split("=")[1];
		int a=Integer.valueOf(st);
		return(a);		
	}


	public void readProcessedImages(int step){
		if(step <1) IJ.log("Error : read process images, but step is lower than 1");
//		if(step>=2) {for (Acquisition acq : this.acquisition)acq.start();readImageForRegistration();}
//		if(step>3)readTransforms();
		//		if(step>=4) readMask();
		//if(step>=5) readHyperImage();
	}
	
	
	public Cep4D(VineType vineType, int dayAfterExperience,String sourcePath,String title,ComputingType computingType) {
		this.computingType=computingType;
		this.title=title;
		this.sourcePath=sourcePath;
		this.vineType=vineType;
		acquisition=new ArrayList<Acquisition>();//Observations goes there
		geometry=new ArrayList<Geometry>();
		misalignment=new ArrayList<Misalignment>();
		capillary=new ArrayList<Capillary> ();
		imageForRegistration=null;
		transformation=new ArrayList<ItkTransform>();
		transformation.add(new ItkTransform());//No transformation for the first image, that is the reference image
	}
	
	
	
	/**
	 * Medium level functions
	 */
	public void quickStartFromFile() {
		//Acquisitions auto-detect
		//Detect T1 sequence
		//Gather the path to source data
		System.out.println("Looking for a Cep4D hosted in "+this.sourcePath);
		//Explore the path, look for STEPS_DONE.tag and DATA_PARAMETERS.tag
		File fStep=new File(this.sourcePath+slash+"STEPS_DONE.tag");
		File fParam=new File(this.sourcePath+slash+"DATA_PARAMETERS.tag");

		if(fStep.exists() && fParam.exists() ) {
			//read the actual step use it to open in memory all necessary datas			
			System.out.println("It's a match ! The tag files tells me that data have already been processed here");
			System.out.println("Start reading acquisitions");
			readAcquisitions();			
			for (Acquisition acq : this.acquisition) {System.out.println("");acq.start();}
			readParametersFromHardDisk();//Read parameters, path and load data in memory
			this.setImageForRegistration();
			readParametersFromHardDisk();//Read parameters, path and load data in memory
			writeParametersToHardDisk();//In the case that a data appears since last time
			System.out.println("Start reading parameters");
			System.out.println("Start reading processed images");
			this.readProcessedImages(readStep());
			System.out.println("Reading done...");
		}
		else {		
			//look for a directory Source_data
			System.out.println("No match with previous analysis ! Starting new analysis...");
			File directory = new File(this.sourcePath,"Source_data");
			if(! directory.exists()) {
				writeStep(-1);
				return;
			}
			File dir = new File(this.sourcePath+slash+"Computed_data");
			dir.mkdirs();
			writeStep(0);
			System.out.println("Exploring Source_data...");
			readAcquisitions();
			System.out.println("Writing parameters file");
			writeStep(1);
		}
	}
		
	public void readAcquisitions() {
		File rxSourceDir=new File(this.sourcePath+slash+"Source_data"+slash+"RX");
		if(rxSourceDir.exists()) {
			this.addAcquisition(AcquisitionType.RX,this.sourcePath+slash+"Source_data"+slash+"RX",Geometry.REFERENCE,
					Misalignment.LIGHT_RIGID,Capillary.HAS_NO_CAPILLARY,SupervisionLevel.AUTONOMOUS);
		}

		File irmSourceDir=new File(this.sourcePath+slash+"Source_data"+slash+"IRM");
		if(irmSourceDir.exists()) {
			this.addAcquisition(AcquisitionType.MRI_CLINICAL,this.sourcePath+slash+"Source_data"+slash+"MRI_CLINICAL",Geometry.UNKNOWN,
					Misalignment.LIGHT_RIGID,Capillary.HAS_CAPILLARY,SupervisionLevel.AUTONOMOUS);
		}

		File photoSourceDir = new File(this.sourcePath,"Source_data"+slash+"PHOTO");
		if(photoSourceDir.exists()) {
			this.addAcquisition(AcquisitionType.PHOTOGRAPH,this.sourcePath+slash+"Source_data"+slash+"PHOTOGRAPH",Geometry.UNKNOWN,
					Misalignment.LIGHT_RIGID,Capillary.HAS_NO_CAPILLARY,SupervisionLevel.AUTONOMOUS);
		}
		System.out.println("Number of acquisitions detected : "+acquisition.size());
	}
	
	public void addAcquisition(Acquisition.AcquisitionType acq, String path,Geometry geom,Misalignment mis,Capillary cap,SupervisionLevel sup){
		switch(acq) {
		case MRI_CLINICAL: this.acquisition.add(new MRI_Clinical(path,cap,sup,this.title+"_MRI",this.computingType));this.hyperSize+=4;break;
		case RX: this.acquisition.add(new RX(path, cap, sup, this.title+"_RX", VineType.VINE));this.hyperSize+=1;break;
		case PHOTOGRAPH: this.acquisition.add(new Photo_Slicing_Seq(path,cap,SupervisionLevel.AUTONOMOUS,this.title+"_RX", ComputingType.COMPUTE_ALL));this.hyperSize+=1;break;
		}
		this.geometry.add(geom);
		this.misalignment.add(mis);
		this.capillary.add(cap);
		int indexCur=this.acquisition.size()-1;
		if(geom==Geometry.REFERENCE) {
			this.referenceImageSize=new int[] {this.acquisition.get(indexCur).dimX(),
					this.acquisition.get(indexCur).dimY(),
					this.acquisition.get(indexCur).dimZ()};
			this.referenceVoxelSize=new double[] {this.acquisition.get(indexCur).voxSX(),
					this.acquisition.get(indexCur).voxSY(),
					this.acquisition.get(indexCur).voxSZ()};
		}
		this.transformation.add(new ItkTransform());
	}
	


	public void readParametersFromHardDisk() {
		
	}
	
	public void writeParametersToHardDisk() {

	}
	
	
	public void setDimensions(int dimX,int dimY,int dimZ) {
		this.referenceImageSize=new int[] {dimX,dimY,dimZ};
	}

	public void setVoxelSizes(double voxSX,double voxSY,double voxSZ) {
		this.referenceVoxelSize=new double[] {voxSX,voxSY,voxSZ};
	}

	public void setImageForRegistration() {
	}
	

	public void readImageForRegistration() {
	}
	
	public void writeImageForRegistration() {
	}

	public long getHyperImageModificationTime() {
		File f=new File(this.sourcePath+slash+ "Computed_data"+slash+"2_HyperImage"+slash+"hyperImage.tif");
		long val=0;
		if(f.exists())val=f.lastModified();
		return val;		
	}
	
	
	
	public void readHyperImage() {
		this.normalizedHyperImage =IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_HyperImage"+slash+"hyperImage.tif");
	}
	
	public void writeHyperImage() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"2_HyperImage");
		dir.mkdirs();
		IJ.saveAsTiff(this.normalizedHyperImage,this.sourcePath+slash+ "Computed_data"+slash+"2_HyperImage"+slash+"hyperImage.tif");
	}

	public void readMask() {
		this.mask=IJ.openImage(this.sourcePath + slash + "Computed_data" + slash + "0_Mask" + slash + "mask.tif");
	}
	
	public void writeMask() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Mask");
		dir.mkdirs();
		IJ.saveAsTiff(this.mask,this.sourcePath + slash + "Computed_data" + slash + "1_Mask" + slash + "mask.tif");
	}

	public void writeTransforms() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		dir.mkdirs();
		for(int i=0;i<this.transformation.size() ; i++) {
			this.transformation.get(i).writeMatrixTransformToFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+".txt");
		}
	}

	public void writeRegisteringTransforms(String registrationStep) {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		dir.mkdirs();
		for(int i=0;i<this.transformation.size() ; i++) {
			this.transformation.get(i).writeMatrixTransformToFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_"+registrationStep+".txt");
		}
	}
	
	public void readTransforms() {
		System.out.println("Reading transforms at step "+this.readStep());
		for(int i=0;i<this.transformation.size() ; i++) {
			if(this.readStep()==4) {	
				this.transformation.set(i,ItkTransform.readTransformFromFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_afterAxisAlignment.txt"));
			}
			else if(this.readStep()==5) {	
				this.transformation.set(i,ItkTransform.readTransformFromFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_afterIPalignment.txt"));
			}
			else if(this.readStep()>=6) {
				this.transformation.set(i,ItkTransform.readTransformFromFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_afterItkRegistration.txt"));
			}
			else {
				IJ.showMessage("Don't understand : read transform at a moment where no transforms had been computed");
			}
		}

	
	
	
	
	
	}

	public void computeMask() {
		//VitiDialogs.notYet("Compute Mask in Vitimage4D");
	}
	

	
	public Acquisition getAcquisition(int i) {
		return this.acquisition.get(i);
	}
	

	public void centerMassCenters() {
		
	
	}
	
	
	///NEW VERSION
	//USAGE OF A BLOCK MATCHING MODEL
	public void automaticFineRegistration() {
		ImagePlus imgRef= this.transformation.get(0).transformImage(
				this.acquisition.get(0).imageForRegistration,
				this.acquisition.get(0).imageForRegistration,false);
		imgRef.getProcessor().resetMinAndMax();
		imgRef=VitimageUtils.removeCapillaryFromRandomMriImage(imgRef); 
		imgRef=VitimageUtils.convertFloatToShortWithoutDynamicChanges(imgRef);
		//		for (int i=this.acquisition.size()-1;i>0;i--) {
		for (int i=0;i<this.acquisition.size();i++) {
			if(i==0)continue;
			if(this.acquisition.get(i).acquisitionType == AcquisitionType.MRI_T2_SEQ)this.transformation.set(i,new ItkTransform(this.transformation.get(0)));

			
			//Preparation of moving image
			ImagePlus imgMov=null;
			if(this.acquisition.get(i).capillary == Capillary.HAS_CAPILLARY) {
				imgMov= this.transformation.get(i).transformImage(
					this.acquisition.get(0).imageForRegistration,
					VitimageUtils.removeCapillaryFromRandomMriImage(this.acquisition.get(i).imageForRegistration),false);
					imgMov=VitimageUtils.convertFloatToShortWithoutDynamicChanges(imgMov);
			}
			else{
				imgMov= this.transformation.get(i).transformImage(			
					this.acquisition.get(0).imageForRegistration,
					this.acquisition.get(i).imageForRegistration,false);
			}
			imgMov.getProcessor().resetMinAndMax();

			System.out.println("Automatic registration intermodal");
			imgRef.getProcessor().resetMinAndMax();
			imgMov.getProcessor().resetMinAndMax();
			ImagePlus imgMask=new Duplicator().run(imgRef);
			IJ.run(imgMask,"8-bit","");
			imgMask=VitimageUtils.set8bitToValue(imgMask,255);
			this.transformation.get(i).addTransform( BlockMatchingRegistration.setupAndRunStandardBlockMatchingWithoutFineParameterization(
										imgRef,imgMov,imgMask,true,false,true)  );
			this.transformation.set(i,this.transformation.get(i).simplify());
		}
		this.transformation.set(0,this.transformation.get(0).simplify());
		writeRegisteringImages("afterItkRegistration");	
		writeRegisteringTransforms("afterItkRegistration");
	}
	
	
	public void writeRegisteringImages(String registrationStep) {
		for(int i=0;i<this.acquisition.size() ; i++) {
			ImagePlus tempView=this.transformation.get(i).transformImage(
					this.acquisition.get(0).getImageForRegistration(),
					this.acquisition.get(i).getImageForRegistration(),false);
			tempView.getProcessor().resetMinAndMax();
			IJ.saveAsTiff(tempView, this.sourcePath+slash+"Computed_data"+slash+"0_Registration"+slash+"imgRegistration_acq_"+i+"_step_"+registrationStep+".tif");
		}
	}
	
	public void computeNormalizedHyperImage() {
		
		ArrayList<ImagePlus> imgList=new ArrayList<ImagePlus>();
		ImagePlus[] hyp;
		
		for(int i=0;i<acquisition.size();i++) {
			Acquisition acq=acquisition.get(i);
			System.out.println("Titre="+this.getTitle()+"  et i="+i+"  c est a dire "+ acq.getTitle());
			System.out.println("Showing hyperimage");
			acq.normalizedHyperImage.show();
			VitimageUtils.waitFor(10000);
			acq.normalizedHyperImage.hide();
			hyp=VitimageUtils.stacksFromHyperstack(acq.normalizedHyperImage,acq.hyperSize);
			System.out.println("Showing hyperimage substack 0");
			hyp[0].show();
			VitimageUtils.waitFor(10000);
			hyp[0].hide();
			
			switch(acq.acquisitionType) {
			case RX:imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[0],false));break;
			case MRI_T1_SEQ:imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[0],false));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[1],false));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[2],false));break;
			case MRI_T2_SEQ:imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[0],false));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[1],false));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[2],false));break;			
			case MRI_GE3D:imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[0],false));break;			
			}
		}
		if(imgList.size()<targetHyperSize) {
			for(int i=imgList.size();i<targetHyperSize;i++) {
				ImagePlus add=ij.gui.NewImage.createImage("", dimX(),dimY(), dimZ(),imgList.get(i-1).getBitDepth(),ij.gui.NewImage.FILL_BLACK);
				imgList.add(add);
			}
		}
		ImagePlus[]tabRet=new ImagePlus[imgList.size()];
		for(int i=0;i<imgList.size() ;i++) tabRet[i]=imgList.get(i);

		this.normalizedHyperImage=Concatenator.run(tabRet);
	}

	
	
	
	/**
	 * Minor functions
	 */
	public String getSourcePath() {
		return this.sourcePath;
	}
	
	public void setSourcePath(String path) {
		this.sourcePath=path;
	}

	public String getTitle(){
		return title;
	}
	
	public void setTitle(String title){
		this.title=title;
	}
	

	public int dimX() {
		return this.referenceImageSize==null ? this.acquisition.get(0).dimX() : this.referenceImageSize[0];
	}

	public int dimY() {
		return this.referenceImageSize==null ? this.acquisition.get(0).dimY() : this.referenceImageSize[1];
	}

	public int dimZ() {
		return this.referenceImageSize==null ? this.acquisition.get(0).dimZ() : this.referenceImageSize[2];
	}
	
	public double voxSX() {
		return this.referenceVoxelSize==null ? this.acquisition.get(0).voxSX() : this.referenceVoxelSize[0];
	}

	public double voxSY() {
		return this.referenceVoxelSize==null ? this.acquisition.get(0).voxSY() : this.referenceVoxelSize[1];
	}

	public double voxSZ() {
		return this.referenceVoxelSize==null ? this.acquisition.get(0).voxSZ() : this.referenceVoxelSize[2];
	}
	
	public int getHyperSize() {
		return hyperSize;
	}
	
	public ItkTransform getTransformation(int i) {
		return this.transformation.get(i);
	}
	
	public ImagePlus getNormalizedHyperImage() {
		return normalizedHyperImage;
	}
}
