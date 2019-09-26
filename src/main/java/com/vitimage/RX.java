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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.vitimage.Vitimage4D.VineType;

import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.OptimizerType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.VitimageUtils.Capillary;
import com.vitimage.VitimageUtils.SupervisionLevel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.plugin.ImageCalculator;
import ij.plugin.Scaler;
import ij.plugin.Slicer;
import ij.process.FloatProcessor;
import ij.process.StackProcessor;

public class RX extends Acquisition{
	private static String []StandardMetadataLookup= {};
	private double standardVoxSize;
	public static final double sliceThicknessForRegistration=0.6;
	private ImagePlus imageFullSize;
	private double voxSXFull;
	private double voxSYFull;
	private double voxSZFull;
	private int dimXFull;
	private int dimYFull;
	private int dimZFull;
	public VineType vineType=VineType.VINE;

	/**
	 * Test sequences
	 */
	public static void main (String []args) {
		ImageJ ij=new ImageJ();
		String []specimen= {"CEP017_S1","CEP011_AS1","CEP012_AS2","CEP013_AS3" ,"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(String spec : specimen) {
			File f=new File("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/RX/STEPS_DONE.tag");
			//f.delete();
			System.out.println("Test procedure start...");
			RX rx=new RX("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/RX", Capillary.HAS_NO_CAPILLARY, SupervisionLevel.AUTONOMOUS, spec, null);
			//,Capillary.HAS_NO_CAPILLARY,SupervisionLevel.GET_INFORMED,spec,VineType.VINE);
			rx.start();//
			rx.freeMemory();
			rx=null;
		}
	}

	
	
	/**
	 * Constructors, factory and top level functions
	 */

	public RX(String sourcePath,Capillary cap,SupervisionLevel sup,String title,com.vitimage.Vitimage4D.VineType cutting) {
		super(AcquisitionType.RX, sourcePath,cap,sup);
		this.vineType=vineType;
		this.title=title;
	}


	public void start() {
		this.printStartMessage();
		quickStartFromFile();
		while(nextStep());
	}
	
	public boolean nextStep(){
		int a=readStep();
		System.out.println("------> "+ " next step : "+a+" "+this.getTitle());
		switch(a) {
		case -1:
			IJ.showMessage("Critical fail, no directory found Source_data in the current directory");
			return false;
		case 0: // rien --> exit
			if(this.supervisionLevel != SupervisionLevel.AUTONOMOUS)IJ.log("No data in this directory");
			return false;
		case 1: //Time to compute hyperimage
			this.computeMask();
			this.writeMask();
			this.computeNormalizedHyperImage();
			writeHyperImage();
			break;
		case 2:
			System.out.println("RX Computation finished for "+this.getTitle());
			return false;
		}
		writeStep(a+1);	
		return true;
	}

	public void freeMemory() {
		imageForRegistration=null;
		normalizedHyperImage=null;
		for(int i=0;i<sourceData.length;i++) sourceData[i]=null;
		if(computedData!=null)for(int i=0;i<computedData.length;i++) computedData[i]=null;
		sourceData=null;
		computedData=null;
		mask=null;
		imageFullSize=null;
		System.gc();		
	}

	

	
	/**
	 * I/O writers/readers helpers functions
	 */
	@Override
	public void quickStartFromFile() {
		//Gather the path to source data
		System.out.println("Quick start : RX sequence hosted in ");
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
			boolean isCreated = dir.mkdirs();
			writeStep(0);
			System.out.println("Reading source data...");
			readSourceData();
			setImageForRegistration();
			System.out.println("Writing parameters file");
			writeParametersToHardDisk();
			System.out.println("Writing stacks");
			writeStackedSourceData();
			//this.computeMask();
			//writeMask();
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
		this.acquisitionDate=VitimageUtils.getDateFromString(strFile[8]);
		this.acquisitionTime=strFile[9];
		this.operator=strFile[10];
		this.acquisitionPlace=strFile[11];
		this.acquisitionDuration=Double.valueOf(strFile[12]);
		this.setDimensions(Integer.valueOf(strFile[13]) , Integer.valueOf(strFile[14]) , Integer.valueOf(strFile[15]) );
		this.setFullDimensions(Integer.valueOf(strFile[16]) , Integer.valueOf(strFile[17]) , Integer.valueOf(strFile[18]) );
		this.unit=strFile[19];
		this.setVoxelSizes(Double.valueOf(strFile[20]), Double.valueOf(strFile[21]), Double.valueOf(strFile[22]));
		this.setFullVoxelSizes(Double.valueOf(strFile[23]), Double.valueOf(strFile[24]), Double.valueOf(strFile[25]));
		this.acquisitionPosition=strFile[20];
		this.acquisitionOrientation=strFile[21];
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
			out.write("AcquisitionDate="+this.acquisitionDate+"\n");
			out.write("AcquisitionTime="+this.acquisitionTime+"\n");
			out.write("Operator="+this.operator+"\n");
			out.write("AcquisitionPlace="+this.acquisitionPlace+"\n");
			out.write("AcquisitionDuration(s)="+this.acquisitionDuration+"\n" );
			out.write("DimX(pix)="+this.dimX() +"\n");
			out.write("DimY(pix)="+this.dimY() +"\n");
			out.write("DimZ(pix)="+this.dimZ()+"\n" );
			out.write("DimFULLX(pix)="+this.dimXFull() +"\n");
			out.write("DimFULLY(pix)="+this.dimYFull() +"\n");
			out.write("DimFULLZ(pix)="+this.dimZFull()+"\n" );
			out.write("Unit="+this.unit +"\n");
			out.write("VoxSX="+this.voxSX() +"\n");
			out.write("VoxSY="+this.voxSY() +"\n");
			out.write("VoxSZ="+this.voxSZ() +"\n");
			out.write("VoxFULLSX="+this.voxSXFull() +"\n");
			out.write("VoxFULLSY="+this.voxSYFull() +"\n");
			out.write("VoxFULLSZ="+this.voxSZFull() +"\n");
			out.write("ObjectPosition="+this.acquisitionPosition +"\n");
			out.write("ObjectOrientation="+this.acquisitionOrientation +"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write transformation to file: "+fParam.getAbsolutePath()+"error: "+e);}	
	}
	

	
	
	
	public double parseVoxSizeFromXML(String path,boolean feelSafeAboutOfficialSize){
		String str="";
		try {
			File f=new File(path);
			str= Files.lines(Paths.get(f.getAbsolutePath()) ).collect(Collectors.joining("\n"));
		}  catch (IOException ex) {        ex.printStackTrace();   }
		String[]lines=str.split("\n");
		if(feelSafeAboutOfficialSize) {
			System.out.println("Procedure when not feeling safe about provided size");
			String[] voxStr=lines[5].split("\\\"");
			double voxSX=Double.parseDouble(voxStr[1]);
			double voxSY=Double.parseDouble(voxStr[3]);
			double voxSZ=Double.parseDouble(voxStr[5]);
			System.out.println("Read : voxel size init=[ "+voxSX+" x "+voxSY+" x "+voxSZ+" ]");
			return voxSX;
		}
		else{
			System.out.println("Procedure when feeling safe about provided size");
			String voxStr=lines[0].split(" ")[1];
			double voxSX=Double.parseDouble(voxStr.split("x")[0]);
			double voxSY=Double.parseDouble(voxStr.split("x")[1]);
			double voxSZ=Double.parseDouble(voxStr.split("x")[2]);
			System.out.println("Read : voxel size init=[ "+voxSX+" x "+voxSY+" x "+voxSZ+" ]");
			return voxSX;
		}
	}
	
	
	/**
	 * I/O images helper functions
	 */
	public void readSourceData() {
		File pathlink = new File(this.sourcePath,"DATA_PATH.tag");
		File directory = new File(this.sourcePath,"Source_data");
		File f=null;
		double voxS=0;
		if(directory.exists())f=new File(this.sourcePath, "Source_data"+slash+"SlicesY");
		else {
			try {
				 String str= Files.lines(Paths.get(pathlink.getAbsolutePath()) ).collect(Collectors.joining("\n"));
				 String strFile=str.split("=")[1];
				 f=new File(strFile+slash+"SlicesY");
				 voxS=parseVoxSizeFromXML(strFile+slash+"unireconstruction.xml",true);
				 } catch (IOException ex) {        ex.printStackTrace();   }
		}
		
		if(! f.exists()) {IJ.showMessage("Source path not found : "+f.getAbsolutePath());return;}
		this.standardVoxSize=voxS;

		//Importer la sequence
		String strMainPath=f.getAbsolutePath();	
		String str=strMainPath;
		System.out.println("Importation de la sequence");
		System.out.println("path="+str);
		sourceData=new ImagePlus[2];
		sourceData[1] = FolderOpener.open(str, "");
		
		if(sourceData[1]==null)System.out.println("NULL IMPORT ! De : "+str);
		//Lire / appliquer les parametres
		VitimageUtils.adjustImageCalibration(sourceData[1], new double[] {standardVoxSize, standardVoxSize,standardVoxSize},"mm");
		System.out.println(TransformUtils.stringVector(VitimageUtils.getDimensions(sourceData[1]), "Dims="));
		System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(sourceData[1]), "VoxS="));
		//VitimageUtils.imageChecking(sourceData[1]);
//		VitimageUtils.imageChecking(sourceData[1]);
		//La subsampler pour produire la deuxieme image source		
		double factorZ=standardVoxSize/sliceThicknessForRegistration;
		System.out.println("Reduction d'un facteur : "+factorZ);
		int zEnd=(int)Math.round(sourceData[1].getStackSize()*factorZ);
		int xEnd=(int)Math.round(sourceData[1].getWidth()*factorZ);
		int yEnd=(int)Math.round(sourceData[1].getHeight()*factorZ);
		System.out.println("Final number of slices : "+zEnd);
		IJ.run(sourceData[1], "Scale...", "x="+factorZ+" y="+factorZ+" z="+factorZ+" width="+xEnd+" height="+yEnd+" depth="+zEnd+" interpolation=Bilinear average process create");
		this.sourceData[0]=IJ.getImage();
		VitimageUtils.adjustImageCalibration(this.sourceData[0],new double[] {sliceThicknessForRegistration,sliceThicknessForRegistration,sliceThicknessForRegistration},"mm");
		this.sourceData[0].hide();
		sourceData[0]=VitimageUtils.switchAxis(sourceData[0],2 );
		sourceData[0]=VitimageUtils.switchAxis(sourceData[0],1 );
		IJ.run(sourceData[0], "Flip Horizontally", "stack");
		this.setDimensions(sourceData[0].getWidth(),sourceData[0].getHeight(),sourceData[0].getStackSize());
		this.setVoxelSizes(sourceData[0].getCalibration().pixelWidth, sourceData[0].getCalibration().pixelHeight , sourceData[0].getCalibration().pixelDepth);
		this.setFullDimensions(sourceData[1].getWidth(),sourceData[1].getHeight(),sourceData[1].getStackSize());
		this.setFullVoxelSizes(sourceData[1].getCalibration().pixelWidth, sourceData[1].getCalibration().pixelHeight , sourceData[1].getCalibration().pixelDepth);
		System.out.println("Voxel size for registration ="+this.voxSX()+" , " +this.voxSY()+" , " +this.voxSZ());
		System.out.println("Voxel size of initial image="+this.voxSXFull()+" , " +this.voxSYFull()+" , " +this.voxSZFull());

	}

	
	public void readStackedSourceData() {
		boolean memoryEconomy=true;
		File f=new File(this.sourcePath, "Computed_data"+slash+"0_Stacks");
		String strPath=f.getAbsolutePath();
		if(! memoryEconomy) {
			sourceData=new ImagePlus[2];
			sourceData[0]=IJ.openImage(strPath+slash+"RX.tif");
			sourceData[1]=IJ.openImage(strPath+slash+"RXfull.tif");
			setImageForRegistration();	
		}
		else {
			sourceData=new ImagePlus[1];
			sourceData[0]=IJ.openImage(strPath+slash+"RX.tif");
			setImageForRegistration();	
			
		}
	}
	
	
	public void writeStackedSourceData() {
		boolean memoryEconomy=true;
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Stacks");
		boolean isCreated = dir.mkdirs();
		IJ.saveAsTiff(this.sourceData[0],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"RX.tif");
		if(! memoryEconomy) {IJ.saveAsTiff(this.sourceData[1],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"RXfull.tif");}
		setImageForRegistration();	
	}

	

	public void readMask() {
		this.mask=IJ.openImage(this.sourcePath + slash + "Computed_data" + slash + "0_Stacks" + slash + "mask.tif");
	}
	
	public void writeMask() {
		IJ.saveAsTiff(this.mask,this.sourcePath + slash + "Computed_data" + slash + "0_Stacks" + slash + "mask.tif");
	}
	


	public void readHyperImage() {
		this.normalizedHyperImage =IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
	}
	
	public void writeHyperImage() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"3_HyperImage");
		boolean isCreated = dir.mkdirs();
		IJ.saveAsTiff(this.normalizedHyperImage,this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
	}
	
	
	public void readProcessedImages(int step){
		if(step <1) IJ.log("Error : read process images, but step is lower than 1");
		if(step>=1) {readStackedSourceData();readMask();}
		if(step>=2) readHyperImage();
	}
	
	public void computeMask() {
		ImagePlus img2=VitimageUtils.maskForRemovingThinStructuresInIsotropicImage(sourceData[0],6000,4);
		img2=VitimageUtils.connexe(img2,1,256, 1,10E8, 6,1, false);
		this.mask=VitimageUtils.thresholdShortImage(img2,1,10E8);
	}
	
	public ImagePlus computeNormalizedHyperImage() {
		if(this.capillary == Capillary.HAS_CAPILLARY)this.computeNormalisationValues();
		else this.computedValuesNormalisationFactor=defaultNormalisationValues();
		System.out.println("Normalisation factor. RX image="+this.computedValuesNormalisationFactor[0]);
		int X=this.dimX();
		int Y=this.dimY();
		int Z=this.dimZ();
		int index;
		computedData=new ImagePlus[1];
		if(this.vineType!=VineType.VINE) {
			computedData[0]=VitimageUtils.convertShortToFloatWithoutDynamicChanges(sourceData[0]);		
			float[][][]tabComputed=new float[1][Z][];
			for(int z=0;z<Z;z++) {
				tabComputed[0][z]=(float[])this.computedData[0].getStack().getProcessor(z+1).getPixels();
			}
			for(int z=0;z<Z;z++) {
				for(int y=0;y<Y;y++) {
					for(int x=0;x<X;x++) {
						index=y*X+x;
						for(int i=0;i<computedData.length;i++)tabComputed[i][z][index]/=this.computedValuesNormalisationFactor[i];
					}
				}
			}
			this.normalizedHyperImage=Concatenator.run(computedData);
			this.normalizedHyperImage.getProcessor().setMinAndMax(0,1);
			return this.normalizedHyperImage;
		}
		else {
			this.normalizedHyperImage=VitimageUtils.makeOperationOnOneImage(this.mask,VitimageUtils.OP_DIV, 255, false);
			this.normalizedHyperImage=VitimageUtils.makeOperationBetweenTwoImages(this.mask,this.sourceData[0],VitimageUtils.OP_MULT,true);
			this.normalizedHyperImage.setDisplayRange(0, 65535);
			IJ.run(this.normalizedHyperImage,"8-bit","");
			return this.normalizedHyperImage;
		}
	}

	public double[]defaultNormalisationValues(){
		return new double[] {60000};
	}

	public void setImageForRegistration() {
		this.imageForRegistration=new Duplicator().run(this.sourceData[0]);
		this.valMinForCalibration=this.imageForRegistration.getProcessor().getMin();
		this.valMaxForCalibration=this.imageForRegistration.getProcessor().getMax();
		this.valMedForThresholding=VitimageUtils.getOtsuThreshold(this.imageForRegistration);
	}

	public void processData() {
		this.start();
	}
	public void setFullDimensions(int x,int y,int z) {
		this.dimXFull=x;
		this.dimYFull=y;
		this.dimZFull=z;
	}
	public void setFullVoxelSizes(double x,double y,double z) {
		this.voxSXFull=x;
		this.voxSYFull=y;
		this.voxSZFull=z;
	}
	public int dimXFull() {
		return this.dimXFull;
	}	
	public int dimYFull() {
		return this.dimYFull;
	}
	public int dimZFull() {
		return this.dimZFull;
	}
	public double voxSXFull() {
		return this.voxSXFull;
	}	
	public double voxSYFull() {
		return this.voxSYFull;
	}
	public double voxSZFull() {
		return this.voxSZFull;
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

}
	
		

