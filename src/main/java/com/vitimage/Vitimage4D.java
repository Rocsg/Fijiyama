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
import java.util.Date;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import com.sun.tools.extcheck.Main;
import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.OptimizerType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.TransformUtils.Geometry;
import com.vitimage.TransformUtils.Misalignment;
import com.vitimage.VitimageUtils.Capillary;
import com.vitimage.VitimageUtils.ComputingType;
import com.vitimage.VitimageUtils.SupervisionLevel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import math3d.Point3d;

public class Vitimage4D implements VitiDialogs,TransformUtils,VitimageUtils{
	public enum VineType{
		GRAFTED_VINE,
		VINE,
		CUTTING
	}
	public final static String slash=File.separator;
	public ComputingType computingType;
	public String title="--";
	public String sourcePath="--";
	public String dataPath="--";
	public boolean T2AndT1SameGeometry=true;
	public SupervisionLevel supervisionLevel=SupervisionLevel.GET_INFORMED;
	public VineType vineType=VineType.CUTTING;
	public final int dayAfterExperience;
	private ArrayList<Acquisition> acquisition;//Observations goes there
	private ArrayList<Geometry> geometry;
	private ArrayList<Misalignment> misalignment;
	private ArrayList<ItkTransform> transformation;
	private ArrayList<Capillary> capillary;
	private int[] referenceImageSize;
	private double[] referenceVoxelSize;
	private AcquisitionType acquisitionStandardReference=AcquisitionType.MRI_T1_SEQ;
	private ImagePlus normalizedHyperImage;
	private ImagePlus mask;
	ImagePlus imageForRegistration;
	private int timeSerieDay=0;
	private String projectName="VITIMAGE";
	private String unit="mm";
	private int hyperSize=0;
	public static final int targetHyperSize=7;
	/**
	 *  Test sequence for the class
	 */
	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		String subject="B001_PAL";
		String day="J218";
		Vitimage4D viti = new Vitimage4D(VineType.CUTTING,0,"/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+subject+"/Source_data/"+day,
								subject+"_"+day,ComputingType.COMPUTE_ALL);			
		viti.start();
		viti.normalizedHyperImage.show();
	}


	
	/**
	 * Top level functions
	 */
	public void start() {
		printStartMessage();
		//if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS)writeStep(1);
		quickStartFromFile();
		while(nextStep());
	}
	
	public void printStartMessage() {
		System.out.println("");
		System.out.println("");
		System.out.println("#######################################################################################################################");
		System.out.println("######## Starting new vitimage4D ");
		String str=""+this.sourcePath+"";
		System.out.println("######## "+str+"");
		System.out.println("#######################################################################################################################");		
	}

	
	public boolean nextStep(){
		int a=readStep();
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
			for (Acquisition acq : this.acquisition) {System.out.println("\nVitimage4D, step1 start a new acquisition");acq.start();}
			this.setImageForRegistration();
			this.writeImageForRegistration();
			this.writeParametersToHardDisk();
			if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) {writeStep(a+1);	return false;}
			break;
		case 2: //individual computations are done. Time to register acquisitions
			System.out.println("\nVitimage4D, step2, start major misalignements");
			this.handleMajorMisalignments();
			this.writeTransforms();
			break;
		case 3: //individual computations are done. Time to register acquisitions
			System.out.println("\nVitimage4D, step3, centrage des axes");
			this.centerAxes();
			this.writeTransforms();
			break;
		case 4: //individual computations are done. Time to register acquisitions
			System.out.println("\nVitimage4D, step4, detection des points d inoculation");
			if(vineType==VineType.CUTTING) detectInoculationPoints();
			this.writeTransforms();
			break;
		case 5: //individual computations are done. Time to register acquisitions
			System.out.println("\nVitimage4D, step5, recalage fin");
			automaticFineRegistration();
			this.writeTransforms();
			break;
		case 6: //data are registered. Time to share data (i.e. masks of non-computable data...)
			//this.computeMask();
			//this.writeMask();
			break;
		case 7: //Data are shared. Time to compute hyperimage
			System.out.println("\nVitimage4D, step7, compute normalized hyperimage");
			this.computeNormalizedHyperImage();
			writeHyperImage();
			break;
		case 8:
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
		if(step>=3)readTransforms();
		if(step>=4) readMask();
		if(step>=5) readHyperImage();
	}
	
	
	public Vitimage4D(VineType vineType, int dayAfterExperience,String sourcePath,String title,ComputingType computingType) {
		this.computingType=computingType;
		this.title=title;
		this.sourcePath=sourcePath;
		this.dayAfterExperience=dayAfterExperience;
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
		System.out.println("Looking for a Vitimage4D hosted in "+this.sourcePath);

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
		File dataSourceDir = new File(this.sourcePath,"Source_data");
		//Lire le contenu, et chercher des dossiers RX MRI_T1_SEQ MRI_T2_SEQ
		File t1SourceDir=new File(this.sourcePath+slash+"Source_data"+slash+"MRI_T1_SEQ");
		if(t1SourceDir.exists()) {
			//A ajouter : extraire des données la geometrie generale
			this.addAcquisition(AcquisitionType.MRI_T1_SEQ,this.sourcePath+slash+"Source_data"+slash+"MRI_T1_SEQ",
					Geometry.REFERENCE,
					Misalignment.LIGHT_RIGID,Capillary.HAS_CAPILLARY,this.supervisionLevel);
		}
		
		
		File t2SourceDir=new File(this.sourcePath+slash+"Source_data"+slash+"MRI_T2_SEQ");
		if(t2SourceDir.exists()) {
			//A ajouter : extraire des données la geometrie generale
			this.addAcquisition(AcquisitionType.MRI_T2_SEQ,this.sourcePath+slash+"Source_data"+slash+"MRI_T2_SEQ",
					Geometry.QUASI_REFERENCE,
					Misalignment.LIGHT_RIGID,Capillary.HAS_CAPILLARY,this.supervisionLevel);
		}

		
		File rxSourceDir=new File(this.sourcePath+slash+"Source_data"+slash+"RX");
		if(rxSourceDir.exists()) {
			//A ajouter : extraire des données la geometrie generale
			this.addAcquisition(AcquisitionType.RX,this.sourcePath+slash+"Source_data"+slash+"RX",
					Geometry.QUASI_REFERENCE,
					Misalignment.LIGHT_RIGID,Capillary.HAS_NO_CAPILLARY,this.supervisionLevel);
		}
		System.out.println("Number of acquisitions detected : "+acquisition.size());
			
	}
	
	public void addAcquisition(Acquisition.AcquisitionType acq, String path,Geometry geom,Misalignment mis,Capillary cap,SupervisionLevel sup){
		
		switch(acq) {
		case MRI_T1_SEQ: this.acquisition.add(new MRI_T1_Seq(path,cap,sup,this.title+"_T1",this.computingType));this.hyperSize+=3;break;
		case MRI_T2_SEQ: this.acquisition.add(new MRI_T2_Seq(path,cap,sup,this.title+"_T2",this.computingType));this.hyperSize+=3;break;
		case MRI_DIFF_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case MRI_FLIPFLOP_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case MRI_SE_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case MRI_GE3D_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case RX: this.acquisition.add(new RX(path,cap,sup,this.title+"_RX"));this.hyperSize+=1;break;
		case HISTOLOGY: VitiDialogs.notYet("FlipFlop");break;
		case PHOTOGRAPH: VitiDialogs.notYet("FlipFlop");break;
		case TERAHERTZ: VitiDialogs.notYet("FlipFlop");break;
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
		this.timeSerieDay=Integer.valueOf(strFile[5]);
		this.projectName=strFile[6];
		this.vineType=VitimageUtils.stringToVineType(strFile[7]);
		this.setDimensions(Integer.valueOf(strFile[9]) , Integer.valueOf(strFile[10]) , Integer.valueOf(strFile[11]) );
		this.unit=strFile[12];
		this.setVoxelSizes(Double.valueOf(strFile[13]), Double.valueOf(strFile[14]), Double.valueOf(strFile[15]));
		this.hyperSize=Integer.valueOf(strFile[16]);
	}
	
	public void writeParametersToHardDisk() {
		File fParam=new File(this.sourcePath,"DATA_PARAMETERS.tag");
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fParam)));
			out.write("############# DATA PARAMETERS TAG FILE ###########\n");
			out.write("DayOfWritingTagFile="+(new Date()) +"\n");
			out.write("LocalSourcePath="+this.sourcePath+"\n");
			out.write("LocalDataPath="+this.dataPath+"\n");
			out.write("Title="+this.getTitle()+"\n");
			out.write("TimeSerieDay="+this.timeSerieDay+"\n");
			out.write("Project="+this.projectName +"\n");
			out.write("VineType="+ this.vineType+"\n");
			out.write("NumberOfAcquisitions="+this.acquisition.size()+"\n");
			out.write("DimX(pix)="+this.dimX() +"\n");
			out.write("DimY(pix)="+this.dimY() +"\n");
			out.write("DimZ(pix)="+this.dimZ()+"\n" );
			out.write("Unit="+this.unit +"\n");
			out.write("VoxSX="+this.voxSX() +"\n");
			out.write("VoxSY="+this.voxSY() +"\n");
			out.write("VoxSZ="+this.voxSZ() +"\n");
			out.write("HyperSize="+this.hyperSize +"\n");
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
		this.imageForRegistration=new Duplicator().run(this.acquisition.get(0).getImageForRegistration());
		this.setDimensions(this.imageForRegistration.getWidth(), this.imageForRegistration.getHeight(), this.imageForRegistration.getStackSize());
		this.setVoxelSizes(this.imageForRegistration.getCalibration().pixelWidth, this.imageForRegistration.getCalibration().pixelHeight, this.imageForRegistration.getCalibration().pixelDepth);
	}
	

	public void readImageForRegistration() {
		this.imageForRegistration=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"imageForRegistration.tif");
		this.setDimensions(this.imageForRegistration.getWidth(), this.imageForRegistration.getHeight(), this.imageForRegistration.getStackSize());
		this.setVoxelSizes(this.imageForRegistration.getCalibration().pixelWidth, this.imageForRegistration.getCalibration().pixelHeight, this.imageForRegistration.getCalibration().pixelDepth);
	}
	
	public void writeImageForRegistration() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		dir.mkdirs();
		IJ.saveAsTiff(this.imageForRegistration,this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"imageForRegistration.tif");
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
			this.transformation.get(i).writeToFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+".txt");
		}
	}

	public void writeRegisteringTransforms(String registrationStep) {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Registration");
		dir.mkdirs();
		for(int i=0;i<this.transformation.size() ; i++) {
			this.transformation.get(i).writeToFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_"+registrationStep+".txt");
		}
	}
	
	public void readTransforms() {
		System.out.println("Reading transforms at step "+this.readStep());
		for(int i=0;i<this.transformation.size() ; i++) {
			if(this.readStep()==4) {	
				this.transformation.set(i,ItkTransform.readFromFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_afterAxisAlignment.txt"));
			}
			else if(this.readStep()==5) {	
				this.transformation.set(i,ItkTransform.readFromFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_afterIPalignment.txt"));
			}
			else if(this.readStep()>=6) {
				this.transformation.set(i,ItkTransform.readFromFile(this.sourcePath+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+i+"_step_afterItkRegistration.txt"));
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
	
	
	public void handleMajorMisalignments() {
		System.out.println("");
		System.out.println("########## Handling major misalignments for vitimage 4D "+title);
		for(int i=0;i<acquisition.size();i++) {
			System.out.println("-- Acquisition "+i);
			if(geometry.get(i)==Geometry.REFERENCE) continue;
			if(geometry.get(i)==Geometry.UNKNOWN) {}
			switch(geometry.get(i)){
			case REFERENCE: 	
				break;
			case UNKNOWN: 
				VitiDialogs.notYet("Geometry.UNKNOWN");
				break;
			case MIRROR_X:
				VitiDialogs.notYet("Geometry.MIRROR_X");
				break;
			case MIRROR_Y:
				VitiDialogs.notYet("Geometry.MIRROR_Y");
				break;
			case MIRROR_Z:
				VitiDialogs.notYet("Geometry.MIRROR_Z");
				break;
			case SWITCH_XY:
				VitiDialogs.notYet("Geometry.SWITCH_XY");
				break;
			case SWITCH_XZ:
				VitiDialogs.notYet("Geometry.SWITCH_XZ");
				break;
			case SWITCH_YZ:
				VitiDialogs.notYet("Geometry.SWITCH_YZ");
				break;
			}			
		}
	}
	
	public void centerAxes() {
		System.out.println("");
		System.out.println("########## Centering axes for vitimage 4D "+title);
		double refCenterX=acquisition.get(0).dimX()*acquisition.get(0).voxSX()/2.0;
		double refCenterY=acquisition.get(0).dimY()*acquisition.get(0).voxSY()/2.0;
		double refCenterZ=acquisition.get(0).dimZ()*acquisition.get(0).voxSZ()/2.0;
		for(int i=0;i<acquisition.size();i++) {
			System.out.println("-- Acquisition "+i);
			Acquisition acq=acquisition.get(i);
			acq.imageForRegistration.setTitle("i="+i+" before all");
			if(T2AndT1SameGeometry && this.acquisition.get(i).acquisitionType == AcquisitionType.MRI_T2_SEQ) {
				this.transformation.set(i,this.transformation.get(0));
				transformation.set(i,transformation.get(i).simplify());
				continue;
			}			
			Point3d[]pInit=new Point3d[3];
			Point3d[]pFin=new Point3d[3];
			pFin=VitimageUtils.detectAxis(acq,15);//in the order : center of object along its axis , center + daxis , center + dvect Orthogonal to axis 				
			pInit[0]=new Point3d(refCenterX, refCenterY     , refCenterZ     );//origine
			pInit[1]=new Point3d(refCenterX, refCenterY     , 1 + refCenterZ );//origine + dZ
			pInit[2]=new Point3d(refCenterX, 1 + refCenterY , refCenterZ     );//origine + dY
			System.out.println("Image local basis 0 / 0Z / 0Y : \n"+pFin[0]+"\n"+pFin[1]+"\n"+pFin[2]+"");
			ItkTransform trAdd=ItkTransform.estimateBestRigid3D(pFin, pInit);
			transformation.get(i).addTransform(trAdd);
			transformation.set(i,transformation.get(i).simplify());
		}
		writeRegisteringTransforms("afterAxisAlignment");
		writeRegisteringImages("afterAxisAlignment");
	}
	
	public void detectInoculationPoints(){
		VitimageUtils.soundAlert("Inoculation");
		VitimageUtils.waitFor(5000);
		System.out.println("");
		System.out.println("########## Detect inoculation points for vitimage 4D "+title);
		double refCenterX=acquisition.get(0).dimX()*acquisition.get(0).voxSX()/2.0;
		double refCenterY=acquisition.get(0).dimY()*acquisition.get(0).voxSY()/2.0;
		double refCenterZ=acquisition.get(0).dimZ()*acquisition.get(0).voxSZ()/2.0;
		for(int i=0;i<acquisition.size();i++) {
			Acquisition acq=acquisition.get(i);
			System.out.println("Processing data number "+i+" , "+acq.acquisitionType);
			Point3d[]pFin;
			if(T2AndT1SameGeometry && this.acquisition.get(i).acquisitionType == AcquisitionType.MRI_T2_SEQ) {
				this.transformation.set(i,new ItkTransform(this.transformation.get(0)));
				transformation.set(i,transformation.get(i).simplify());
				continue;
			}			
			
			ImagePlus tempImgDetect=this.transformation.get(i).transformImage( 
					this.acquisition.get(0).getImageForRegistration() ,
					this.acquisition.get(i).getImageForRegistration());
			ImagePlus tempImgOutline=this.transformation.get(i).transformImage( 
					this.acquisition.get(0).getImageForRegistration() , 
					this.acquisition.get(i).getImageForRegistration());
			tempImgOutline=	VitimageUtils.smoothContourOfPlant(tempImgOutline,tempImgOutline.getStackSize()/2);
			pFin=VitimageUtils.detectInoculationPointGuidedByAutomaticComputedOutline(tempImgDetect,tempImgOutline);
			
			//Compute and store the inoculation Point, in the coordinates of the original image
			Point3d inoculationPoint=ItkImagePlusInterface.vectorDoubleToPoint3d(
					acq.getTransform().transformPoint(ItkImagePlusInterface.doubleArrayToVectorDouble(
							new double[] {pFin[3].x,pFin[3].y,pFin[3].z})));
			acq.setInoculationPoint(inoculationPoint);

			
			//Compute the transformation that align the inoculation point to the Y+, with the axis already aligned
			Point3d[]pInit=new Point3d[3];
			pInit[0]=new Point3d( refCenterX  , refCenterY     , refCenterZ      );
			pInit[1]=new Point3d( refCenterX  , refCenterY     , refCenterZ + 1  );
			pInit[2]=new Point3d( refCenterX  , refCenterY + 1 , refCenterZ      );

			Point3d[] pFinTrans=new Point3d[] { pFin[0] , pFin[1] , pFin[2] };
			ItkTransform trAdd=ItkTransform.estimateBestRigid3D(pFinTrans,pInit);
			transformation.get(i).addTransform(trAdd);
			transformation.set(i,transformation.get(i).simplify());
			//acq.setImageForRegistration();				
		}
		writeRegisteringTransforms("afterIPalignment");
		writeRegisteringImages("afterIPalignment");	
	}
	
	public void automaticFineRegistrationITK() {
		ImagePlus imgRef= this.transformation.get(0).transformImage(
				this.acquisition.get(0).imageForRegistration,
				this.acquisition.get(0).imageForRegistration);
		imgRef.getProcessor().resetMinAndMax();
		imgRef=VitimageUtils.removeCapillaryFromRandomMriImage(imgRef); 
		imgRef=VitimageUtils.convertFloatToShortWithoutDynamicChanges(imgRef);
		for (int i=0;i<this.acquisition.size();i++) {
			if(i==0)continue;
			if(T2AndT1SameGeometry && this.acquisition.get(i).acquisitionType == AcquisitionType.MRI_T2_SEQ)this.transformation.set(i,new ItkTransform(this.transformation.get(0)));

			ItkRegistrationManager manager=new ItkRegistrationManager();

			//Preparation of moving image
			ImagePlus imgMov=null;
			if(this.acquisition.get(i).capillary == Capillary.HAS_CAPILLARY) {
				imgMov= this.transformation.get(i).transformImage(
					this.acquisition.get(0).imageForRegistration,
					VitimageUtils.removeCapillaryFromRandomMriImage(this.acquisition.get(i).imageForRegistration));
					imgMov=VitimageUtils.convertFloatToShortWithoutDynamicChanges(imgMov);
			}
			else{
				imgMov= this.transformation.get(i).transformImage(			
					this.acquisition.get(0).imageForRegistration,
					this.acquisition.get(i).imageForRegistration);
			}
			imgMov.getProcessor().resetMinAndMax();

			System.out.println("Automatic registration intermodal");
			System.out.println("Ref="+this.acquisition.get(0).getTitle());
			System.out.println("Mov="+this.acquisition.get(i).getTitle());
			imgRef.getProcessor().resetMinAndMax();
			imgMov.getProcessor().resetMinAndMax();
				this.transformation.get(i).addTransform(manager.runScenarioInterModal(
							new ItkTransform(),imgRef,imgMov, (this.acquisition.get(i).acquisitionType == AcquisitionType.MRI_T2_SEQ) ));
			this.transformation.set(i,this.transformation.get(i).simplify());
		}
		this.transformation.set(0,this.transformation.get(0).simplify());
		writeRegisteringImages("afterItkRegistration");	
		writeRegisteringTransforms("afterItkRegistration");
	}
	
	///NEW VERSION
	//USAGE OF A BLOCK MATCHING MODEL
	public void automaticFineRegistration() {
		ImagePlus imgRef= this.transformation.get(0).transformImage(
				this.acquisition.get(0).imageForRegistration,
				this.acquisition.get(0).imageForRegistration);
		imgRef.getProcessor().resetMinAndMax();
		imgRef=VitimageUtils.removeCapillaryFromRandomMriImage(imgRef); 
		imgRef=VitimageUtils.convertFloatToShortWithoutDynamicChanges(imgRef);
		for (int i=0;i<this.acquisition.size();i++) {
			if(i==0)continue;
			if(T2AndT1SameGeometry && this.acquisition.get(i).acquisitionType == AcquisitionType.MRI_T2_SEQ)this.transformation.set(i,new ItkTransform(this.transformation.get(0)));

			
			//Preparation of moving image
			ImagePlus imgMov=null;
			if(this.acquisition.get(i).capillary == Capillary.HAS_CAPILLARY) {
				imgMov= this.transformation.get(i).transformImage(
					this.acquisition.get(0).imageForRegistration,
					VitimageUtils.removeCapillaryFromRandomMriImage(this.acquisition.get(i).imageForRegistration));
					imgMov=VitimageUtils.convertFloatToShortWithoutDynamicChanges(imgMov);
			}
			else{
				imgMov= this.transformation.get(i).transformImage(			
					this.acquisition.get(0).imageForRegistration,
					this.acquisition.get(i).imageForRegistration);
			}
			imgMov.getProcessor().resetMinAndMax();

			System.out.println("Automatic registration intermodal");
			System.out.println("Ref="+this.acquisition.get(0).getTitle());
			System.out.println("Mov="+this.acquisition.get(i).getTitle());
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
					this.acquisition.get(i).getImageForRegistration());
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
			hyp=VitimageUtils.stacksFromHyperstack(acq.normalizedHyperImage,acq.hyperSize);
			switch(acq.acquisitionType) {
			case RX:imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[0]));break;
			case MRI_T1_SEQ:imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[0]));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[1]));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[2]));break;
			case MRI_T2_SEQ:imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[0]));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[1]));
							imgList.add( transformation.get(i).transformImage( acquisition.get(0).imageForRegistration ,hyp[2]));break;			
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
