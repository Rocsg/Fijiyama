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
	public static final double standardVoxSize=0.0209;//Variable due to ommission of metadata copyin during acquisitions gathering. There should be an improvement there : keep the metadata right in place.
	public static final double sliceThicknessForRegistration=0.25;
	private ImagePlus imageFullSize;
	private double voxSXFull;
	private double voxSYFull;
	private double voxSZFull;
	private int dimXFull;
	private int dimYFull;
	private int dimZFull;
	/**
	 * Test sequences
	 */
	public static void main (String []args) {
		ImageJ ij=new ImageJ();
		System.out.println("Test procedure start...");
		RX rx=new RX("/home/fernandr/Bureau/Test/RX",Capillary.HAS_NO_CAPILLARY,SupervisionLevel.GET_INFORMED,"Test_RX");
		rx.start();//
	}


	
	
	/**
	 * Constructors, factory and top level functions
	 */

	public RX(String sourcePath,Capillary cap,SupervisionLevel sup,String title) {
		super(AcquisitionType.RX, sourcePath,cap,sup);
		this.title=title;
	}

	public static RX RXFactory(String sourcePath,Capillary cap,SupervisionLevel sup,String dataPath) {
		RX rx=new RX(sourcePath,cap,sup,"factory");
		rx.start();
		return rx;
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
			this.computeMask();
			writeMask();
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
	
	
	
	public void makeSubresolution() {
		
	}
	
	/**
	 * I/O images helper functions
	 */
	public void readSourceData() {
		File pathlink = new File(this.sourcePath,"DATA_PATH.tag");
		File directory = new File(this.sourcePath,"Source_data");
		File f=null;
		if(directory.exists())f=new File(this.sourcePath, "Source_data"+slash+"SlicesY");
		else {
			try {
				 String str= Files.lines(Paths.get(pathlink.getAbsolutePath()) ).collect(Collectors.joining("\n"));
				 String strFile=str.split("=")[1];
				 f=new File(strFile+slash+"SlicesY");
	       } catch (IOException ex) {        ex.printStackTrace();   }
		}
		
		if(! f.exists()) {IJ.showMessage("Source path not found : "+f.getAbsolutePath());return;}
		String strMainPath=f.getAbsolutePath();
		String[] strSlices=f.list();
		strSlices=VitimageUtils.stringArraySort(strSlices);
		int nbTr=strSlices.length;
		
		//Extraire les parametres généraux depuis une des coupes	
		ImagePlus imgPar=IJ.openImage(strMainPath+slash+strSlices[0]);
		System.out.println("Parameters auto detection from : "+strMainPath+slash+strSlices[0]);
		System.out.println("Appel à setParams");
		System.out.println("Ok.");
	
			
		//Importer la sequence
		String str=strMainPath;
		System.out.println("Importation de la sequence");
		System.out.println("path="+str);
		sourceData=new ImagePlus[2];
		sourceData[1] = FolderOpener.open(str, "");

		//Lire / appliquer les parametres
		VitimageUtils.adjustImageCalibration(sourceData[1], new double[] {standardVoxSize, standardVoxSize,standardVoxSize},"mm");

		System.out.println("Mirror along X axis");
		VitimageUtils.imageChecking(sourceData[1]);
		new StackProcessor(sourceData[1].getStack()).flipHorizontal();
		VitimageUtils.imageChecking(sourceData[1]);
		//La subsampler pour produire la deuxieme image source		
		double factorZ=standardVoxSize/sliceThicknessForRegistration;
		System.out.println("Reduction d'un facteur : "+factorZ);
		double nSlicesFinal=(int)Math.ceil(sourceData[1].getStackSize()*factorZ);
		System.out.println("Final number of slices : "+nSlicesFinal);
		IJ.run(sourceData[1], "Scale...", "x=1.0 y=1.0 z="+factorZ+" width="+sourceData[1].getWidth()+" height="+sourceData[1].getHeight()+" depth="+nSlicesFinal+" interpolation=Bilinear average process create");
		this.sourceData[0]=IJ.getImage();
		this.sourceData[0].hide();
		this.setDimensions(sourceData[0].getWidth(),sourceData[0].getHeight(),sourceData[0].getStackSize());
		this.setVoxelSizes(sourceData[0].getCalibration().pixelWidth, sourceData[0].getCalibration().pixelHeight , sourceData[0].getCalibration().pixelDepth);
		this.setFullDimensions(sourceData[1].getWidth(),sourceData[1].getHeight(),sourceData[1].getStackSize());
		this.setFullVoxelSizes(sourceData[1].getCalibration().pixelWidth, sourceData[1].getCalibration().pixelHeight , sourceData[1].getCalibration().pixelDepth);
		System.out.println("Voxel size for registration ="+this.voxSX()+" , " +this.voxSY()+" , " +this.voxSZ());
		System.out.println("Voxel size of initial image="+this.voxSXFull()+" , " +this.voxSYFull()+" , " +this.voxSZFull());
		VitimageUtils.imageCheckingFast(this.sourceData[0],"RX SourceData[0]");
		VitimageUtils.imageCheckingFast(this.sourceData[1],"RX SourceData[1]");
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
		System.out.println("Mask computation : gaussian filtering, and 3D connected component research (~1 mn)...");
		ImagePlus img=new Duplicator().run(sourceData[0]);
		System.out.println("Gaussian filtering");
		img=VitimageUtils.gaussianFiltering(img,2*this.voxSX(),2*this.voxSY(),2*this.voxSX());
		System.out.println("Connected components extraction");
		double val=(this.capillary==Capillary.HAS_CAPILLARY ? this.getCapillaryValue(img) : this.defaultNormalisationValues()[0]);
		ImagePlus imgConObject=VitimageUtils.connexe(img,val/15, 65500,0,10E10,6,1,true);//Here 65500 for the scotch stuff
		IJ.run(imgConObject,"8-bit","");
		this.mask = imgConObject;
		VitimageUtils.imageCheckingFast(this.mask,"RX mask");
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


}
	
		

