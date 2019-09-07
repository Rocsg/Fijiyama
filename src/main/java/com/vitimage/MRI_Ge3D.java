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
import java.util.Date;
import java.util.stream.Collectors;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;


/*
 * Flip z PUIS 
[ 0,00618   0,99997  -0,00418   0,93675]
[-0,99997   0,00619   0,00373  16,85129]
[ 0,00376   0,00416   0,99998  -13,36324]
[ 0,00000   0,00000   0,00000   1,00000]
Recaler approximativement les deux
*/

public class MRI_Ge3D extends Acquisition {	
	private double Te;
	private double Tr;
	private double fieldPower;
	private boolean oldBehaviour=false;//if true : old way to export data in directories, until end 2018
	private double flipAngle;
	private static String []StandardMetadataLookup= { "Study Date", "Study Time", "Study Description" , "Slice Thickness", "Repetition Time" ,
			"Echo Time" , "Image Position (Patient)" , "Image Orientation (Patient)"  , "Rows" , "Columns" ,
										   	   "Pixel Spacing" , "Bits Allocated" };
			
	/**
	 * Main test sequences
	 */
	public static void main (String []args) {
		ImageJ ij=new ImageJ();
		System.out.println("Test procedure start...");
		MRI_Ge3D mri=new MRI_Ge3D("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B099_PCH/Source_data/J133/Source_data/MRI_GE3D",
									Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,"B041_DS_J218_MRI_T1",ComputingType.COMPUTE_ALL);		
		mri.start();
		VitimageUtils.detectAxis(mri,15);
		return;
	}


	public MRI_Ge3D(String sourcePath,Capillary cap, SupervisionLevel supervisionLevel,String title,ComputingType computingType) {
		super(AcquisitionType.MRI_GE3D, sourcePath,cap, supervisionLevel);
		this.computingType=computingType;
		this.title=title;
		this.hyperSize=1;
		this.computedData=new ImagePlus[1];
	}

	public void start() {
		this.printStartMessage();
		quickStartFromFile();
		while(nextStep());
	}


	
	public boolean nextStep(){
		int a=readStep();
		System.out.println("MRI_T1_SEQ "+this.getTitle()+"------> "+ " next step : "+a+" "+this.getTitle());
		switch(a) {
		case -1:
			IJ.showMessage("Critical fail, no directory found Source_data in the current directory");
			return false;
		case 0: // rien --> exit
			if(this.supervisionLevel != SupervisionLevel.AUTONOMOUS)IJ.log("No data in this directory");
			return false;			
		case 1: //data are read. Time to compute hyperimage
			if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) return false;
			this.computeMask();
			this.computeNormalizedHyperImage();
			writeHyperImage();
			break;
		case 2:
			System.out.println("MRI_GE3D Computation finished for "+this.getTitle());
			return false;
		}
		writeStep(a+1);	
		return true;
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
        } catch (IOException ex) { ex.printStackTrace();  strFile="None\nNone";    	  }	
		String[]strLines=strFile.split("\n");
		String st=strLines[0].split("=")[1];
		int a=Integer.valueOf(st);
		return(a);		
	}


	
	
	
	
	
	
	
	
	/**
	 * I/O writers/readers helpers functions
	 */
	@Override
	public void quickStartFromFile() {
		//Gather the path to source data
		System.out.println("Quick start : Ge3d sequence hosted in ");
		System.out.println(this.sourcePath.substring(0,this.sourcePath.length()/2));
		System.out.println(this.sourcePath.substring(this.sourcePath.length()/2+1));

		//Explore the path, look for STEPS_DONE.tag and DATA_PARAMETERS.tag
		File fStep=new File(this.sourcePath+slash+"STEPS_DONE.tag");
		File fParam=new File(this.sourcePath+slash+"DATA_PARAMETERS.tag");

		if(fStep.exists() && fParam.exists() ) {
			//read the actual step use it to open in memory all necessary datas			
			System.out.println("It's a match ! The tag files tells me that data have already been processed here");
			System.out.println("Start reading parameters");
			readParametersFromHardDisk();//Read parameters, path and load data in memory
			System.out.println("Start reading processed images");
			readProcessedImages(readStep());
			setImageForRegistration();
			System.out.println("Reading done...");
		}
		else {		
			//look for a directory Source_data
			System.out.println("No match with previous analysis ! Starting new analysis...");
			File directory = new File(this.sourcePath,"Source_data");
			File pathlink = new File(this.sourcePath,"DATA_PATH.tag");
			if(! directory.exists() && ! pathlink.exists()) {
				writeStep(-1);
				return;
			}
			File dir = new File(this.sourcePath+slash+"Computed_data");
			dir.mkdirs();
			writeStep(0);
			System.out.println("Reading source data...");
			readSourceData();
			setImageForRegistration();
			System.out.println("Writing parameters file");
			writeParametersToHardDisk();
			System.out.println("Writing stacks");
			writeStackedSourceData();
			this.computeMask();
			writeMask();
			System.out.println("Writing step");
			writeStep(1);
		}
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
		this.acquisitionType=VitimageUtils.stringToAcquisitionType(strFile[7]);
		this.flipAngle=Double.valueOf(strFile[8]);
		this.Te=Double.valueOf(strFile[9]);
		this.Tr=Double.valueOf(strFile[10]);
		this.acquisitionDate=VitimageUtils.getDateFromString(strFile[11]);
		this.acquisitionTime=strFile[12];
		this.operator=strFile[13];
		this.acquisitionPlace=strFile[14];
		this.fieldPower=Double.valueOf(strFile[15]);
		this.acquisitionDuration=Double.valueOf(strFile[16]);
		this.setDimensions(Integer.valueOf(strFile[17]) , Integer.valueOf(strFile[18]) , Integer.valueOf(strFile[19]) );
		this.unit=strFile[20];
		this.setVoxelSizes(Double.valueOf(strFile[21]), Double.valueOf(strFile[22]), Double.valueOf(strFile[23]));
		this.acquisitionPosition=strFile[24];
		this.acquisitionOrientation=strFile[25];
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
			out.write("TimeSerieDay="+this.getTimeSerieDay()+"\n");
			out.write("Project="+this.getProjectName() +"\n");
			out.write("AcquisitionType="+ this.getAcquisitionType()+"\n");
			out.write("FlipAngle="+this.flipAngle+"\n");
			out.write("Te(ms)="+this.Te+"\n" );
			out.write("Tr(ms)="+this.Tr+"\n");
			out.write("AcquisitionDate="+this.acquisitionDate+"\n");
			out.write("AcquisitionTime="+this.acquisitionTime+"\n");
			out.write("Operator="+this.operator+"\n");
			out.write("AcquisitionPlace="+this.acquisitionPlace+"\n");
			out.write("Field(T)="+this.fieldPower +"\n");
			out.write("AcquisitionDuration(s)="+this.acquisitionDuration+"\n" );
			out.write("DimX(pix)="+this.dimX() +"\n");
			out.write("DimY(pix)="+this.dimY() +"\n");
			out.write("DimZ(pix)="+this.dimZ()+"\n" );
			out.write("Unit="+this.unit +"\n");
			out.write("VoxSX="+this.voxSX() +"\n");
			out.write("VoxSY="+this.voxSY() +"\n");
			out.write("VoxSZ="+this.voxSZ() +"\n");
			out.write("ObjectPosition="+this.acquisitionPosition +"\n");
			out.write("ObjectOrientation="+this.acquisitionOrientation +"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write transformation to file: "+fParam.getAbsolutePath()+"error: "+e);}	
	}
	
	
	
	
	
	/**
	 * I/O images helper functions
	 */
	public void readSourceData() {
		if( ! this.oldBehaviour) {
			//Chercher un dossier SL_RAW dans le dossier SourceData
			File pathlink = new File(this.sourcePath,"DATA_PATH.tag");
			File directory = new File(this.sourcePath,"Source_data");
			File f=null;
			if(directory.exists())f=new File(this.sourcePath, "Source_data"+slash+"SL_RAW");
			else {
				try {
					 String str= Files.lines(Paths.get(pathlink.getAbsolutePath()) ).collect(Collectors.joining("\n"));
					 String strFile=str.split("=")[1];
					 System.out.println("Open data directory : "+strFile);
					 f=new File(strFile); 
				} catch (IOException ex) {        ex.printStackTrace();   }
			}
			
			if(! f.exists()) {IJ.showMessage("Source path not found : "+this.sourcePath+slash+"Source_data"+slash+"SL_RAW");return;}
			String strPath=f.getAbsolutePath();
			f=new File(strPath,"TR000100");
			strPath=f.getAbsolutePath();
			f=new File(strPath,"TE000005");
			strPath=f.getAbsolutePath();
			String[] strTR=f.list();
		
			//Extraire les parametres généraux depuis une des coupes	
			File fiTr=new File(strPath,strTR[0]);
			ImagePlus imgPar=IJ.openImage(fiTr.getAbsolutePath());
			this.setParamsFromDCM(imgPar);
			
			
			//Construire les tableaux en fonction du nombre d echos
			sourceData=new ImagePlus[1];
			System.out.println("Importation de la sequence");
			System.out.println("path="+fiTr.getAbsolutePath());
			sourceData[0] = FolderOpener.open(strPath, "");
			IJ.run(sourceData[0], "Flip Z", "");
			IJ.run(sourceData[0], "Rotate 90 Degrees Left", "");
			VitimageUtils.imageCheckingFast(sourceData[0],"Source data["+0+"]");
			this.setDimZ(sourceData[0].getStackSize());
			System.out.println("");
		}
	}

	public String[] getParamsFromDCM(ImagePlus imgPar,String []paramsWanted,String debug){
		if(imgPar==null) IJ.showMessage("Warning : image used for parameters detection is detected as null image. Computation will fail");
		String paramGlob=imgPar.getInfoProperty();
		String[]paramLines=paramGlob.split("\n");
		String[]ret=new String[paramsWanted.length];
		for(int i=0;i<paramsWanted.length;i++) {
			ret [i]="NOT DETECTED";
			for(int j=0;j<paramLines.length ;j++) {
				if(paramLines[j].split(": ")[0].indexOf(paramsWanted[i])>=0)ret[i]=paramLines[j].split(": ")[1];
			}
		}
		return ret;
	}
		
	
	public void setParamsFromDCM(ImagePlus imgPar) {
		String[]params=getParamsFromDCM(imgPar,MRI_Ge3D.StandardMetadataLookup,"Called from setParams");
		
		this.acquisitionDate=VitimageUtils.getDateFromString(params[0]);
		this.acquisitionTime=params[1];	
		this.setTitle(params[2]);
		this.setVoxSZ(Double.valueOf(params[3]));
		this.Te=Double.valueOf(params[5]);
		this.acquisitionPosition=params[6];
		this.acquisitionOrientation=params[7];
		this.setDimX(Integer.valueOf(params[8]));
		this.setDimY(Integer.valueOf(params[9]));
		
		this.setVoxSX(Double.valueOf( params[10].split("\\W")[0]+"."+params[10].split("\\W")[1] ) );
		this.setVoxSY(Double.valueOf( params[10].split("\\W")[2]+"."+params[10].split("\\W")[3] ) );
	}
	
	
	public void readStackedSourceData() {
		File f=new File(this.sourcePath, "Computed_data"+slash+"0_Stacks");
		String strPath=f.getAbsolutePath();
		System.out.println(strPath);
		sourceData=new ImagePlus[1];
		sourceData[0]=IJ.openImage(strPath+slash+"ge3d.tif");
	}			
	
	
	public void writeStackedSourceData() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Stacks");
		dir.mkdirs();
		IJ.saveAsTiff(this.sourceData[0],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"ge3d.tif");
	}


	public void readMask() {
		this.mask=IJ.openImage(this.sourcePath + slash + "Computed_data" + slash + "0_Stacks" + slash + "mask.tif");
	}
	
	public void writeMask() {
		IJ.saveAsTiff(this.mask,this.sourcePath + slash + "Computed_data" + slash + "0_Stacks" + slash + "mask.tif");
	}
	


	public void readHyperImage() {
		this.normalizedHyperImage =IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		//this.imageForRegistration=this.normalizedHyperImage;
	}
	
	public void writeHyperImage() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"3_HyperImage");
		dir.mkdirs();
		IJ.saveAsTiff(this.normalizedHyperImage,this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
	}
	

	
	public void readProcessedImages(int step){
		if(step <1) IJ.log("Error : read process images, but step is lower than 1");
		if(step>=1) {readStackedSourceData();}
		if(step>=2) {readMask();readHyperImage();}
	}
	
	
	public void computeMask() {
		System.out.println("\nMask computation : gaussian filtering, and 3D connected component research (~1 mn)...");
		this.mask=VitimageUtils.areaOfPertinentMRIMapComputation (sourceData[sourceData.length-1],sigmaGaussMapInPixels);
		System.out.println("Mask computation ok.");
		VitimageUtils.imageCheckingFast(this.mask,"T1 Mask");
	}
	

	public void processData() {
		this.start();
	}
	


	public ImagePlus computeNormalizedHyperImage() {
		this.computedData[0]=new Duplicator().run(this.sourceData[0]);
		if(this.capillary == Capillary.HAS_CAPILLARY)this.computeNormalisationValues();
		else this.computedValuesNormalisationFactor=defaultNormalisationValues();
		System.out.println("Normalisation factor. "+this.computedValuesNormalisationFactor[0]);
		IJ.run(this.computedData[0],"32-bit","");
		this.normalizedHyperImage=VitimageUtils.multiplyImage(this.computedData[0], 1.0/this.computedValuesNormalisationFactor[0]);
		this.normalizedHyperImage =VitimageUtils.normalizeMeanAndVarianceAlongZ(this.normalizedHyperImage);
		this.normalizedHyperImage.getProcessor().setMinAndMax(0,1);
		return this.normalizedHyperImage;
	}

	

	public double[]defaultNormalisationValues(){
		return new double[] {20000};
	}
	
	public void setImageForRegistration() {
		this.imageForRegistration=new Duplicator().run(this.sourceData[this.sourceData.length-1]);
		this.valMinForCalibration=this.imageForRegistration.getProcessor().getMin();
		this.valMaxForCalibration=this.imageForRegistration.getProcessor().getMax();
		this.valMedForThresholding=VitimageUtils.getOtsuThreshold(this.imageForRegistration)*0.8;
	}

	public void setTe(double Te) {
		this.Te=Te;
	}

	public void setTr(double tr) {
		this.Tr=tr;
	}

	
}
