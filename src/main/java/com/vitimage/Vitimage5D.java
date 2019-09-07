package com.vitimage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.OptimizerType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.TransformUtils.AngleComparator;
import com.vitimage.TransformUtils.Geometry;
import com.vitimage.TransformUtils.Misalignment;
import com.vitimage.Vitimage4D.VineType;
import com.vitimage.VitimageUtils.Capillary;
import com.vitimage.VitimageUtils.ComputingType;
import com.vitimage.VitimageUtils.SupervisionLevel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import math3d.Point3d;

public class Vitimage5D implements VitiDialogs,TransformUtils,VitimageUtils{
	public boolean ge3dComputation=true;
	public ComputingType computingType;
	public final static String slash=File.separator;
	public String title="--";
	public String sourcePath="--";
	public String dataPath="--";
	public SupervisionLevel supervisionLevel=SupervisionLevel.GET_INFORMED;
	public VineType vineType=VineType.CUTTING;
	private int[]successiveTimePoints;
	private ArrayList<Vitimage4D> vitimage4D;//Observations goes there
	private ArrayList<ItkTransform> transformation;
	private int[] referenceImageSize;
	private double[] referenceVoxelSize;
	private ImagePlus normalizedHyperImage;
	private ImagePlus mask;
	ImagePlus imageForRegistration;
	private String projectName="VITIMAGE";
	private String unit="mm";
	private int nbDaysInTimeCourse=1;
	
	/**
	 *  Test sequence for the class
	 */
	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		String subject="B099_PCH";
		Vitimage5D viti = new Vitimage5D(VineType.CUTTING,"/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+subject,subject,ComputingType.COMPUTE_ALL);			
		//viti.useGe3dForTimeRegistration=false;
		viti.start(10);
		ImagePlus norm=new Duplicator().run(viti.normalizedHyperImage);
		norm.show();
		viti.freeMemory();
	}


	
	/**
	 * Top level functions
	 */
	public void start(int ending) {
		quickStartFromFile();
		if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS)writeStep(1);
		while(nextStep(ending));
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
			if(this.computingType!=ComputingType.EVERYTHING_AFTER_MAPS) {
				if(this.supervisionLevel != SupervisionLevel.AUTONOMOUS)IJ.log("No data in this directory");
				return false;
			};break;			
		case 1://data are read. Time to compute individual calculus for each acquisition
			this.writeParametersToHardDisk();
			this.setImageForRegistration();
			if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) {writeStep(a+1);	return false;}
			break;
		case 2: //individual computations are done. Time to register acquisitions
			System.out.println("\n\nVitimage 5D : step2, start automatic fine rigid registration");
			automaticRigidFineRegistration();
			break;
		case 3: //individual computations are done. Time to register acquisitions
			System.out.println("\n\nVitimage 5D : step3, start automatic fine dense registration");
			automaticDenseFineRegistration();
			break;
		case 4: //Data are shared. Time to compute hyperimage
			System.out.println("\n\nVitimage 5D : step3, start Normalized hyperimage computation");
			this.computeNormalizedHyperImage(true);
			writeHyperImage();
			System.out.println("HYPERIMAGE WRITTEN");
			break;
		case 5:
			for (Vitimage4D viti : this.vitimage4D) {
				long lThis=this.getHyperImageModificationTime();
				long lVit4D=viti.getHyperImageModificationTime();
				if(lVit4D>lThis) {
					System.out.println("Vit5D HyperImage update : at least one source vitimage4D hyper image has been modified since last hyperimage modification : " +viti.getTitle());
					System.out.println("Creation date of vitimage4D="+lVit4D);
					System.out.println("Creation date of vitimage5D="+lThis);
					System.out.println("And this time ="+new Date().getTime());
					writeStep(3);
					return true;
				}			
			}
		case 6:
			if(this.ge3dComputation)computeTransfosForGe3dAlignement();saveTransfosForGe3dAlignement();
			return true;
		case 7:
			if(this.ge3dComputation)computeGe3dHyperImage();
			return true;
		case 8:			
			System.out.println("Vitimage 5D, Computation finished for "+this.getTitle());
			return false;
		}
		writeStep(a+1);	
		return true;
	}
	

	public ImagePlus getNormalizedHyperImage() {
		return this.normalizedHyperImage;
	}
	
	public void freeMemory(){
		for(Vitimage4D viti : vitimage4D) {
			viti.freeMemory();
			viti=null;
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
		try {
			BufferedReader in=new BufferedReader(new FileReader(this.getSourcePath()+slash+"STEPS_DONE.tag"));
			while ((line = in.readLine()) != null) {
				strFile+=line+"\n";
			}
			in.close();
        } catch (IOException ex) { ex.printStackTrace();  strFile="None\nNone";        }	
		String[]strLines=strFile.split("\n");
		String st=strLines[0].split("=")[1];
		int a=Integer.valueOf(st);
		return(a);		
	}


	public void readProcessedImages(int step){
		if(step <1) IJ.log("Error : read process images, but step is lower than 1");
		if(step>=2)this.readImageForRegistration();
		if(step>=4)readDenseTransforms();
		if(step>=5) readHyperImage();
	}
	
	
	public Vitimage5D(VineType vineType,String sourcePath,String title,ComputingType computingType) {
		this.computingType=computingType;
		this.title=title;
		this.sourcePath=sourcePath;
		this.vineType=vineType;
		vitimage4D=new ArrayList<Vitimage4D>();//Observations goes there
		imageForRegistration=null;
		transformation=new ArrayList<ItkTransform>();
	}
	
	/**
	 * Medium level functions
	 */
	public void quickStartFromFile() {
		//Acquisitions auto-detect
		//Detect T1 sequence
		//Gather the path to source data
		System.out.println("Looking for a Vitimage5D hosted in "+this.sourcePath);

		//Explore the path, look for STEPS_DONE.tag and DATA_PARAMETERS.tag
		File fStep=new File(this.sourcePath+slash+"STEPS_DONE.tag");
		File fParam=new File(this.sourcePath+slash+"DATA_PARAMETERS.tag");

		if(fStep.exists() && fParam.exists() ) {
			//read the actual step use it to open in memory all necessary datas			
			System.out.println("It's a match ! The tag files tells me that data have already been processed here");
			System.out.println("Start reading acquisitions");
			readVitimages();			
			for (Vitimage4D viti4d : this.vitimage4D) {System.out.println("");viti4d.start(Vitimage4D.UNTIL_END);}
			System.out.println("Start reading parameters");
			readParametersFromHardDisk();//Read parameters, path and load data in memory
			writeParametersToHardDisk();//In the case that a data appears since last time
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
			readVitimages();
			for (Vitimage4D vit4d : this.vitimage4D) {System.out.println("\nVitimage5D : step1, start a new Vitimage4D");vit4d.start(Vitimage4D.UNTIL_END);}
			System.out.println("Writing parameters file");

			writeStep(1);
		}
	}
		
	public void readVitimages() {
		File dataSourceDir = new File(this.sourcePath,"Source_data");
		//Lire le contenu, et chercher des dossiers Jquelque chose
		String[] strDays=dataSourceDir.list();
		this.successiveTimePoints=new int[strDays.length];
		for(int day=0;day<strDays.length ;day++) {
			String dayStr=strDays[day].split("J")[1];
			System.out.println("Detection d'une Vitimage4D à J"+dayStr);
			int dayVal=Integer.parseInt(dayStr);
			this.addVitimage4D(this.sourcePath+slash+"Source_data"+slash+strDays[day],this.supervisionLevel,dayVal,this.title+" - D0 + "+dayStr+" days ");
			this.transformation.set(day,new ItkTransform());
		}
		System.out.println("Number of vitimages4D detected : "+vitimage4D.size());
		System.out.println("Sorting data");
		System.out.print("Before : ");
		for(int i=0;i<vitimage4D.size();i++)System.out.println(vitimage4D.get(i).dayAfterExperience);
		vitimage4D.sort(new Vitimage4DComparator());
		System.out.print("After : ");
		for(int i=0;i<vitimage4D.size();i++) {
			System.out.println(vitimage4D.get(i).dayAfterExperience);
			this.successiveTimePoints[i]=vitimage4D.get(i).dayAfterExperience;
		}
	}
	public void addVitimage4D(String path,SupervisionLevel sup,int dayOfTimeCourse,String title){		
		this.vitimage4D.add(new Vitimage4D(vineType,dayOfTimeCourse,path,title,this.computingType));
		this.successiveTimePoints[this.vitimage4D.size()-1]=dayOfTimeCourse;
		this.transformation.add(new ItkTransform());
	}
	
	public void readParametersFromHardDisk() {
		File fParam=new File(this.sourcePath,"DATA_PARAMETERS.tag");
		String[]strFile=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
       } catch (IOException ex) {        ex.printStackTrace();   }
		for(int i=1;i<strFile.length ; i++) {
			strFile[i]=strFile[i].split("=")[1];
		}
		this.sourcePath=strFile[2];
		this.dataPath=strFile[3];
		this.setTitle(strFile[4]);
		this.nbDaysInTimeCourse=Integer.valueOf(strFile[5]);this.successiveTimePoints=new int[this.nbDaysInTimeCourse];
		String []strDays=strFile[6].split(" ");
		for(int i=0;i<this.nbDaysInTimeCourse ; i++)this.successiveTimePoints[i]=Integer.valueOf(strDays[i]);
		this.projectName=strFile[7];
		this.vineType=VitimageUtils.stringToVineType(strFile[8]);
		this.setDimensions(Integer.valueOf(strFile[9]) , Integer.valueOf(strFile[10]) , Integer.valueOf(strFile[11]) );
		this.unit=strFile[12];
		this.setVoxelSizes(Double.valueOf(strFile[13]), Double.valueOf(strFile[14]), Double.valueOf(strFile[15]));
	}
	
	public void writeParametersToHardDisk() {
		System.out.println("Here is the writing of parameters, with dimX="+this.dimX());
		File fParam=new File(this.sourcePath,"DATA_PARAMETERS.tag");
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fParam)));
			out.write("############# DATA PARAMETERS TAG FILE ###########\n");
			out.write("DayOfWritingTagFile="+(new Date()) +"\n");
			out.write("LocalSourcePath="+this.sourcePath+"\n");
			out.write("LocalDataPath="+this.dataPath+"\n");
			out.write("Title="+this.getTitle()+"\n");
			out.write("NbDays="+this.successiveTimePoints.length+"\n");
			out.write("TimeSerieDays="+VitimageUtils.writableArray(this.successiveTimePoints)+"\n");
			out.write("Project="+this.projectName +"\n");
			out.write("VineType="+ this.vineType+"\n");
			out.write("DimX(pix)="+this.dimX() +"\n");
			out.write("DimY(pix)="+this.dimY() +"\n");
			out.write("DimZ(pix)="+this.dimZ()+"\n" );
			out.write("Unit="+this.unit +"\n");
			out.write("VoxSX="+this.voxSX() +"\n");
			out.write("VoxSY="+this.voxSY() +"\n");
			out.write("VoxSZ="+this.voxSZ() +"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write transformation to file: "+fParam.getAbsolutePath()+"error: "+e);}	
	}
	
	
	public void setDimensions(int dimX,int dimY,int dimZ) {
		this.referenceImageSize=new int[] {dimX,dimY,dimZ};
	}

	public void setVoxelSizes(double voxSX,double voxSY,double voxSZ) {
		this.referenceVoxelSize=new double[] {voxSX,voxSY,voxSZ};
	}

	public void setImageForRegistration() {
		this.imageForRegistration=new Duplicator().run(this.vitimage4D.get(0).imageForRegistration);
		this.setDimensions(this.imageForRegistration.getWidth(), this.imageForRegistration.getHeight(), this.imageForRegistration.getStackSize());
		this.setVoxelSizes(this.imageForRegistration.getCalibration().pixelWidth, this.imageForRegistration.getCalibration().pixelHeight, this.imageForRegistration.getCalibration().pixelDepth);
		this.writeImageForRegistration();
	}
	

	public void readImageForRegistration() {
		this.imageForRegistration=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"imageForRegistration.tif");
		System.out.println("Lecture de l'image :");
		System.out.println(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"imageForRegistration.tif");
		this.setDimensions(this.imageForRegistration.getWidth(), this.imageForRegistration.getHeight(), this.imageForRegistration.getStackSize());
		this.setVoxelSizes(this.imageForRegistration.getCalibration().pixelWidth, this.imageForRegistration.getCalibration().pixelHeight, this.imageForRegistration.getCalibration().pixelDepth);
	}
	
	public void writeImageForRegistration() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		dir.mkdirs();
		IJ.saveAsTiff(this.imageForRegistration,this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"imageForRegistration.tif");
	}

	public long getHyperImageModificationTime() {
		File f=new File(this.sourcePath+slash+ "Computed_data"+slash+"2_HyperImage"+slash+this.getTitle()+"_HyperImage.tif");
		long val=0;
		if(f.exists())val=f.lastModified();
		return val;		
	}

	
	public void readHyperImage() {
		this.normalizedHyperImage =IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_HyperImage"+slash+this.title+"_HyperImage.tif");
	}
	
	public void writeHyperImage() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"2_HyperImage");
		dir.mkdirs();
		IJ.saveAsTiff(this.normalizedHyperImage,this.sourcePath+slash+ "Computed_data"+slash+"2_HyperImage"+slash+this.title+"_HyperImage.tif");
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
			this.transformation.get(i).writeAsDenseField(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+".tif",this.imageForRegistration);
		}
	}

	public void writeRegisteringTransforms(String registrationStep) {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		dir.mkdirs();
		for(int i=0;i<this.transformation.size() ; i++) {
			System.out.println("Ecriture de la transformation :");
			System.out.println(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_"+registrationStep+".tif");
			this.transformation.set(i,this.transformation.get(i).flattenDenseField(this.imageForRegistration));
			this.transformation.get(i).writeAsDenseField(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_"+registrationStep+".tif",this.imageForRegistration);
		}
	}

	public void writeRigidTransform(int i) {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		if(!dir.exists())dir.mkdirs();
		this.transformation.get(i).writeMatrixTransformToFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"trans_rig_with_flo_"+i+".txt");
	}

	public void writeDenseTransform(int i) {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		if(!dir.exists())dir.mkdirs();
		this.transformation.get(i).writeAsDenseField(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"trans_dense_with_flo_"+i+".tif",this.imageForRegistration);
	}
	
	
	public void readRigidTransforms() {
		this.transformation.set(0,new ItkTransform());
		for(int i=1;i<this.transformation.size() ; i++) {
			System.out.println("Lecture de la transformation :");
			this.transformation.set(i,ItkTransform.readTransformFromFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"trans_rig_with_flo_"+i+".txt"));
		}
	}

	public void readDenseTransforms() {
		this.transformation.set(0,new ItkTransform());
		for(int i=1;i<this.transformation.size() ; i++) {
			System.out.println("Lecture de la transformation :");
			this.transformation.set(i,ItkTransform.readAsDenseField(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"trans_rig_and_dense_with_flo_"+i+".tif"));
		}
	}

	
	public void readTransforms() {
		for(int i=0;i<this.transformation.size() ; i++) {
			System.out.println("Lecture de la transformation :");
			System.out.println(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+".tif");
			this.transformation.set(i,ItkTransform.readAsDenseField(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+".tif"));
		}
	}

	public void automaticRigidFineRegistration() {
		int startsAt=1;		
		for (int i=startsAt;i<this.vitimage4D.size();i++) {
			//UNSTACK HYPERIMAGE 4D REF TO GET IMAGE FOR REGISTRATION
			ImagePlus concatTab=vitimage4D.get(i-1).getNormalizedHyperImage();
			ImagePlus []imgTab=VitimageUtils.stacksFromHyperstack(concatTab, Vitimage4D.targetHyperSize);
			ImagePlus imgRef=imgTab[0];
			imgRef.getProcessor().resetMinAndMax();
			VitimageUtils.imageCheckingFast(imgRef,"Reference image, "+"J"+vitimage4D.get(i-1).dayAfterExperience);
			
			//UNSTACK HYPERIMAGE 4D MOV	TO GET IMAGE FOR REGISTRATION		
			concatTab=vitimage4D.get(i).getNormalizedHyperImage();
			imgTab=VitimageUtils.stacksFromHyperstack(concatTab, Vitimage4D.targetHyperSize);
			ImagePlus imgMov=imgTab[0];
			imgMov.getProcessor().resetMinAndMax();
			VitimageUtils.imageChecking(imgMov,"Moving image, "+"J"+vitimage4D.get(i).dayAfterExperience
					);


			//COMPUTE THE MASK IMAGE
			ImagePlus imgMask=IJ.openImage("/home/fernandr/Bureau/Test/BM_subvoxel/mask_little.tif");
			
			//COMPUTE THE REGISTRATION, RIGID-BODY, then DENSE
			System.out.println("\n\n\n\n\n\n\nRunning rigid registration step  ");
			System.out.println("Ref="+this.vitimage4D.get(i-1).getTitle());
			System.out.println("Mov="+this.vitimage4D.get(i).getTitle());
			ItkTransform transDayItoIminus1=null;
			transDayItoIminus1=BlockMatchingRegistration.testSetupAndRunStandardBlockMatchingWithoutFineParameterization(
					VitimageUtils.removeCapillaryFromHyperImageForRegistration(imgRef),
					VitimageUtils.removeCapillaryFromHyperImageForRegistration(imgMov),
					imgMask,true,false,true,true,true,true);
			this.transformation.set(i,transDayItoIminus1);
			writeRigidTransform(i);
			writeRegisteredImage(i,"rigid",imgMov);
			writeReferenceImage(i,"rigidreference",imgRef);
		}
	}
	
	
	
	public void automaticDenseFineRegistration() {
		int startsAt=1;
		startsAt=1;
		this.readRigidTransforms();
		
		for (int i=startsAt;i<this.vitimage4D.size();i++) {
			
			//UNSTACK HYPERIMAGE 4D REF TO GET IMAGE FOR REGISTRATION
			ImagePlus concatTab=vitimage4D.get(i-1).getNormalizedHyperImage();
			ImagePlus []imgTab=VitimageUtils.stacksFromHyperstack(concatTab, Vitimage4D.targetHyperSize);
			ImagePlus imgRef=imgTab[0];
			imgRef.getProcessor().resetMinAndMax();
			VitimageUtils.imageCheckingFast(imgRef,"Reference image, "+"J"+vitimage4D.get(i-1).dayAfterExperience);
			
			//UNSTACK HYPERIMAGE 4D MOV	TO GET IMAGE FOR REGISTRATION		
			concatTab=vitimage4D.get(i).getNormalizedHyperImage();
			imgTab=VitimageUtils.stacksFromHyperstack(concatTab, Vitimage4D.targetHyperSize);
			ImagePlus imgMov=imgTab[0];
			imgMov.getProcessor().resetMinAndMax();
			VitimageUtils.imageChecking(imgMov,"Moving image, "+"J"+vitimage4D.get(i).dayAfterExperience);


			//COMPUTE THE MASK IMAGE
			ImagePlus imgMask=IJ.openImage("/home/fernandr/Bureau/Test/BM_subvoxel/mask_little.tif");
			
			//COMPUTE THE REGISTRATION
			System.out.println("\n\n\n\n\n\n\nRunning registration step  ");
			System.out.println("Ref="+this.vitimage4D.get(i-1).getTitle());
			System.out.println("Mov="+this.vitimage4D.get(i).getTitle());
			ItkTransform transDayItoIminus1=null;
			if((i==4) && this.title.equals("B001_PAL")) {
				transDayItoIminus1=this.transformation.get(i);
			}
			else transDayItoIminus1=BlockMatchingRegistration.testSetupAndRunStandardBlockMatchingWithoutFineParameterizationFromRigidStart(
					VitimageUtils.removeCapillaryFromHyperImageForRegistration(imgRef),
					VitimageUtils.removeCapillaryFromHyperImageForRegistration(imgMov),
					imgMask,this.transformation.get(i),true,true,true,true,true);
			this.transformation.set(i, transDayItoIminus1);
			System.out.print("Saving transfo computed between "+(i-1)+" and "+i);
			transDayItoIminus1.writeAsDenseField(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"trans_rig_and_dense_with_flo_"+i+".tif",imageForRegistration);
			System.gc();
		
			System.out.println();
			writeRegisteredImage(i,"dense",imgMov);
			writeReferenceImage(i,"densereference",imgRef);
		}
	}	
	
	
	public void writeRegisteredImage(int i,String step,ImagePlus imgMov) {
		ImagePlus tempView=this.transformation.get(i).transformImage(
				this.vitimage4D.get(0).getAcquisition(0).getImageForRegistration(),
				imgMov);
		tempView.getProcessor().resetMinAndMax();
		IJ.saveAsTiff(tempView, this.sourcePath+slash+"Computed_data"+slash+"0_Registration"+slash+"imgRegistration_J"+this.successiveTimePoints[i]+"_step_"+step+".tif");
	}

	public void writeReferenceImage(int i,String step,ImagePlus imgRef) {
		ImagePlus tempView=imgRef;
		tempView.getProcessor().resetMinAndMax();
		IJ.saveAsTiff(tempView, this.sourcePath+slash+"Computed_data"+slash+"0_Registration"+slash+"imgRegistration_J"+this.successiveTimePoints[i]+"_step_"+step+".tif");
	}

	
	public void writeRegisteringImages(String registrationStep) {
		for(int i=0;i<this.vitimage4D.size() ; i++) {
			ImagePlus tempView=this.transformation.get(i).transformImage(
					this.vitimage4D.get(0).getAcquisition(0).getImageForRegistrationWithoutCapillary(),
					this.vitimage4D.get(i).getAcquisition(0).getImageForRegistrationWithoutCapillary());
			tempView.getProcessor().resetMinAndMax();
			IJ.saveAsTiff(tempView, this.sourcePath+slash+"Computed_data"+slash+"0_Registration"+slash+
							"imgRegistration_J"+this.successiveTimePoints[i]+"_step_"+registrationStep+".tif");
		}
	}
	
	public void computeNormalizedHyperImage(boolean useDenseTransfo) {
		//Calcul des transformations composees
		System.out.println("Hyperimage : lecture transfo denses");
//		this.readDenseTransforms();
		ItkTransform[]transTemp=new ItkTransform[this.transformation.size()];
		for(int i=0;i<transTemp.length;i++) transTemp[i]=new ItkTransform();
		for(int i=0;i<transTemp.length;i++) {
			System.out.println("Hyperimage : composition pour transfo i="+i);
			for(int j=i;j<transTemp.length;j++) {
				transTemp[j].addTransform(this.transformation.get(i));
			}
		}
		for(int i=0;i<transTemp.length;i++) {
			System.out.println("Hyperimage : flatten transfo i="+i);
			this.transformation.set(i,transTemp[i].flattenDenseField(this.imageForRegistration));
		}
		
		System.out.println("Calcul de l'hyperimage 5D");
		ImagePlus []concatTab=new ImagePlus[vitimage4D.size()];
		for(int i=0;i<vitimage4D.size();i++) {
			System.out.println("Add vitimage 4D "+i+" corresponding to day"+this.successiveTimePoints[i]+" that is"+
					vitimage4D.get(i).dayAfterExperience+" from path="+vitimage4D.get(i).sourcePath);			
			concatTab[i]=vitimage4D.get(i).getNormalizedHyperImage();
			concatTab[i]=this.transformation.get(i).transformHyperImage4D(concatTab[0],concatTab[i], Vitimage4D.targetHyperSize);

			ImagePlus []tempTab=VitimageUtils.stacksFromHyperstack(concatTab[i], Vitimage4D.targetHyperSize);
			tempTab[0]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T1-weighted", tempTab[0],15,0);
			tempTab[1]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- M0 map from T1 seq", tempTab[1],15,0);
			tempTab[2]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T1 map from T1 seq", tempTab[2],15,0);
			tempTab[3]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T2-weighted", tempTab[3],15,0);
			tempTab[4]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- M0 map from T2 seq", tempTab[4],15,0);
			tempTab[5]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T2 map from T2 seq", tempTab[5],15,0);
			tempTab[6]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- RX ", tempTab[6],15,0);
			concatTab[i]=Concatenator.run(tempTab);
		}
		Concatenator con=new Concatenator();
		con.setIm5D(true);
		ImagePlus concatImage=con.concatenate(concatTab,true);
		System.out.println("vitimage4D.size()"+vitimage4D.size());
		System.out.println(" vitimage4D.get(0).dimZ()"+ vitimage4D.get(0).dimZ());
		System.out.println(" vitimage4D.get(0).getHyperSize()"+ vitimage4D.get(0).getHyperSize());
		this.normalizedHyperImage=HyperStackConverter.toHyperStack(concatImage, Vitimage4D.targetHyperSize, vitimage4D.get(0).dimZ(),this.successiveTimePoints.length,"xyzct","Grayscale");
		
		//IJ.run(concatImage,"Open Series as Image5D", "3rd=z 4th=ch 3rd_dimension_size=" + dimZ() + " 4th_dimension_size="+vitimage4D.get(0).getHyperSize());
//        ImagePlus hyper = WindowManager.getImage(WindowManager.getImageCount());
	}

	
	
	
	
	public void computeNormalizedHyperImageGe3d() {
		//Calcul des transformations composees
		this.readRigidTransforms();
		ItkTransform[]transTemp=new ItkTransform[this.transformation.size()];
		for(int i=0;i<transTemp.length;i++) transTemp[i]=new ItkTransform();
		for(int i=0;i<transTemp.length;i++) {
			for(int j=i;j<transTemp.length;j++) {
				transTemp[j].addTransform(this.transformation.get(i));
			}
		}
		for(int i=0;i<transTemp.length;i++) this.transformation.set(i,transTemp[i]);
		

		
		System.out.println("Calcul de l'hyperimage 5D");
		ImagePlus []concatTab=new ImagePlus[vitimage4D.size()];
		for(int i=0;i<vitimage4D.size();i++) {
			System.out.println("Add vitimage 4D "+i+" GE3d corresponding to day"+this.successiveTimePoints[i]+" that is"+
					vitimage4D.get(i).dayAfterExperience+" from path="+vitimage4D.get(i).sourcePath);			
			//concatTab[i]=vitimage4D.get(i).getNormalizedGe3dAndM0();
			concatTab[i]=this.transformation.get(i).transformHyperImage4D(concatTab[0],concatTab[i], Vitimage4D.targetHyperSize);

			ImagePlus []tempTab=VitimageUtils.stacksFromHyperstack(concatTab[i], Vitimage4D.targetHyperSize);
			tempTab[0]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T1-weighted", tempTab[0],15,0);
			tempTab[1]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- M0 map from T1 seq", tempTab[1],15,0);
			tempTab[2]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T1 map from T1 seq", tempTab[2],15,0);
			tempTab[3]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T2-weighted", tempTab[3],15,0);
			tempTab[4]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- M0 map from T2 seq", tempTab[4],15,0);
			tempTab[5]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T2 map from T2 seq", tempTab[5],15,0);
			tempTab[6]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- RX ", tempTab[6],15,0);
			concatTab[i]=Concatenator.run(tempTab);
		}
		Concatenator con=new Concatenator();
		con.setIm5D(true);
		ImagePlus concatImage=con.concatenate(concatTab,true);
		System.out.println("vitimage4D.size()"+vitimage4D.size());
		System.out.println(" vitimage4D.get(0).dimZ()"+ vitimage4D.get(0).dimZ());
		System.out.println(" vitimage4D.get(0).getHyperSize()"+ vitimage4D.get(0).getHyperSize());
		this.normalizedHyperImage=HyperStackConverter.toHyperStack(concatImage, Vitimage4D.targetHyperSize, vitimage4D.get(0).dimZ(),this.successiveTimePoints.length,"xyzct","Grayscale");
		
		//IJ.run(concatImage,"Open Series as Image5D", "3rd=z 4th=ch 3rd_dimension_size=" + dimZ() + " 4th_dimension_size="+vitimage4D.get(0).getHyperSize());
//        ImagePlus hyper = WindowManager.getImage(WindowManager.getImageCount());
	}

	
	
	
	
//TO REFACTOR A LITTLE BIT
	public void computeTransfosForGe3dAlignement() {
		System.out.println("What s next ?");
		System.exit(0);

		//Load reference image
		ImagePlus imgRef=vitimage4D.get(0).getAcquisition(2).getImageForRegistration();
		ItkTransform transTemp=vitimage4D.get(0).transformation.get(2);
		transTemp.transformImage(imgRef, imgRef);
		VitimageUtils.imageCheckingFast(imgRef,"Reference image Ge3d at J0");
		

		//Compute registrations and store them for movie computing
		
		
	}
	
	public void saveTransfosForGe3dAlignement() {
	}
	
	
	
	public void computeGe3dHyperImage() {
		System.out.println("Calcul de l'hyperimage 5D basée sur ge3d");
		ImagePlus []concatTab=new ImagePlus[vitimage4D.size()];
		for(int i=0;i<vitimage4D.size();i++) {
			System.out.println("Add vitimage 4D "+i+" corresponding to day"+this.successiveTimePoints[i]+" that is"+
					vitimage4D.get(i).dayAfterExperience+" from path="+vitimage4D.get(i).sourcePath);			
			concatTab[i]=vitimage4D.get(i).getNormalizedHyperImage();
			concatTab[i]=this.transformation.get(i).transformHyperImage4D(concatTab[0],concatTab[i], Vitimage4D.targetHyperSize);

			ImagePlus []tempTab=VitimageUtils.stacksFromHyperstack(concatTab[i], Vitimage4D.targetHyperSize);
			tempTab[0]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T1-weighted", tempTab[0],15,0);
			tempTab[1]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- M0 map from T1 seq", tempTab[1],15,0);
			tempTab[2]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T1 map from T1 seq", tempTab[2],15,0);
			tempTab[3]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T2-weighted", tempTab[3],15,0);
			tempTab[4]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- M0 map from T2 seq", tempTab[4],15,0);
			tempTab[5]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- T2 map from T2 seq", tempTab[5],15,0);
			tempTab[6]=VitimageUtils.writeTextOnImage(vitimage4D.get(i).title+"- RX ", tempTab[6],15,0);
			concatTab[i]=Concatenator.run(tempTab);
		}
		Concatenator con=new Concatenator();
		con.setIm5D(true);
		ImagePlus concatImage=con.concatenate(concatTab,true);
		System.out.println("vitimage4D.size()"+vitimage4D.size());
		System.out.println(" vitimage4D.get(0).dimZ()"+ vitimage4D.get(0).dimZ());
		System.out.println(" vitimage4D.get(0).getHyperSize()"+ vitimage4D.get(0).getHyperSize());
		this.normalizedHyperImage=HyperStackConverter.toHyperStack(concatImage, Vitimage4D.targetHyperSize, vitimage4D.get(0).dimZ(),this.successiveTimePoints.length,"xyzct","Grayscale");
		
		//IJ.run(concatImage,"Open Series as Image5D", "3rd=z 4th=ch 3rd_dimension_size=" + dimZ() + " 4th_dimension_size="+vitimage4D.get(0).getHyperSize());
//        ImagePlus hyper = WindowManager.getImage(WindowManager.getImageCount());
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
		return this.referenceImageSize==null ? this.vitimage4D.get(0).dimX() : this.referenceImageSize[0];
	}

	public int dimY() {
		return this.referenceImageSize==null ? this.vitimage4D.get(0).dimY() : this.referenceImageSize[1];
	}

	public int dimZ() {
		return this.referenceImageSize==null ? this.vitimage4D.get(0).dimZ() : this.referenceImageSize[2];
	}
	
	public double voxSX() {
		return this.referenceVoxelSize==null ? this.vitimage4D.get(0).voxSX() : this.referenceVoxelSize[0];
	}

	public double voxSY() {
		return this.referenceVoxelSize==null ? this.vitimage4D.get(0).voxSY() : this.referenceVoxelSize[1];
	}

	public double voxSZ() {
		return this.referenceVoxelSize==null ? this.vitimage4D.get(0).voxSZ() : this.referenceVoxelSize[2];
	}
	
	
}