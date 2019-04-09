/**
 * This class is an implementation of Acquisition, in the case of a MRI T1 sequence. These sequences are successive 3D observations,
 * obtained at varying Trecovery(0.6 s, 1.2s, 2.4s, ...), with a constant Techo.These helps to fit the parameter T1, according to 
 * the equation of the spin echo sequence.
 */

package com.vitimage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.itk.simple.ResampleImageFilter;

import com.vitimage.ItkImagePlusInterface.OptimizerType;
import com.vitimage.VitimageUtils.AcquisitionType;
import com.vitimage.VitimageUtils.Capillary;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.plugin.ImageCalculator;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import imagescience.shape.Point;
import math3d.Point3d;



/**
 * @author fernandr
 *
 */
public class MRI_T1_Seq extends Acquisition implements Fit,ItkImagePlusInterface{
	
	private boolean removeOutliers=true;
	public boolean computeOnlyAcquisitionsAndMaps=false;
	private double thresholdOutlier=1.5;
	private double Te;
	private double Tr[];
	private double fieldPower;
	private boolean oldBehaviour=false;//if true : old way to export data in directories, until end 2018
	private static String []StandardMetadataLookup= { "Study Date", "Study Time", "Study Description" , "Slice Thickness", "Repetition Time" ,
			"Echo Time" , "Image Position (Patient)" , "Image Orientation (Patient)"  , "Rows" , "Columns" ,
										   	   "Pixel Spacing" , "Bits Allocated" };
			
	/**
	 * Main test sequences
	 */
	public static void main (String []args) {
		ImageJ ij=new ImageJ();
		System.out.println("Test procedure start...");
		MRI_T1_Seq mri=new MRI_T1_Seq("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B051_CT/Source_data/J218/Source_data/MRI_T1_SEQ",
									Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,"Test_MRI_T1",ComputingType.COMPUTE_ALL);
		mri.start();
		return;
	}

	
	
	/**
	 * Constructors, factory and top level functions
	 */
	public MRI_T1_Seq(String sourcePath,Capillary cap,SupervisionLevel sup,String title,ComputingType computingType) {
		super(AcquisitionType.MRI_T1_SEQ, sourcePath,cap,sup);
		this.computingType=computingType;
		this.title=title;
		this.hyperSize=3;
	}

	public static MRI_T1_Seq MRI_T1_SeqFactory(String sourcePath,Capillary cap,SupervisionLevel sup,String dataPath) {
		MRI_T1_Seq mri=new MRI_T1_Seq(sourcePath,cap,sup,"factory",ComputingType.COMPUTE_ALL);
		mri.start();
		return mri;
	}
	
	public void start() {
		this.printStartMessage();
		//if(this.computingType==ComputingType.EVERYTHING_AFTER_MAPS)writeStep(3);
		//if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS)writeStep(1);
		quickStartFromFile();
		while(nextStep());
	}
	
	public boolean nextStep(){
		int a=readStep();
		System.out.println("MRI_T1_SEQ "+this.getTitle()+"------> "+ " next step : "+a+" "+this.getTitle());
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
		case 1: //data are read. Time to register
			if(this.computingType!=ComputingType.EVERYTHING_AFTER_MAPS) {
				this.registerEchoes();
				writeRegisteredSourceData();
			};break;			
		case 2: //data are registered. Time to compute Maps
			if(this.computingType!=ComputingType.EVERYTHING_AFTER_MAPS) {
				this.computeMask();
				writeMask();
				this.computeMaps();
				writeMaps();
				if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) {writeStep(a+1);	return false;}
			};break;
		case 3: //data are registered. Time to compute Maps
			if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) return false;
			this.computeNormalizedHyperImage();
			writeHyperImage();
			break;
		case 4:
			if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) return false;
			System.out.println("MRI_T1_Seq Computation finished for "+this.getTitle());
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
		System.out.println("Quick start : T1 sequence hosted in ");
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
		this.Tr=new double[Integer.valueOf(strFile[8])];
		this.sourceData=new ImagePlus[Integer.valueOf(strFile[8])];
		this.Te=Double.valueOf(strFile[9]);
		String []strTr=strFile[10].split(" ");
		for(int i=0;i<this.Tr.length ; i++)Tr[i]=Double.valueOf(strTr[i]);
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
			out.write("NumberOfSuccessiveTr="+this.sourceData.length+"\n");
			out.write("Te(ms)="+this.Te+"\n" );
			out.write("Tr(ms)="+VitimageUtils.writableArray(this.Tr)+"\n");
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
			String[] strEchoes=f.list();
			strEchoes=VitimageUtils.stringArraySort(strEchoes);
			int nbEch=strEchoes.length;
			
			//Compter le nombre d echos : verifier l'eventuelle presence d'une sequence T2 et la virer
			for(int i=nbEch-1;i>=0;i--) {
				if(strEchoes[i].equals("TR010000"))nbEch--;
				else System.out.println("Available recovery time : i="+i+" : "+strEchoes[i]);
			}
		
			//Extraire les parametres généraux depuis une des coupes	
			File fiTr=new File(strPath,strEchoes[0]);
			String[] strSlices=fiTr.list();
			fiTr=new File(fiTr.getAbsolutePath(),strSlices[0]);
			strSlices=fiTr.list();

			ImagePlus imgPar=IJ.openImage(fiTr.getAbsolutePath()+slash+strSlices[0]);
			this.setParamsFromDCM(imgPar);
			
			
			//Construire les tableaux en fonction du nombre d echos
			sourceData=new ImagePlus[nbEch];
			Tr=new double[nbEch];
			System.out.println("T1 Sequences importation ("+nbEch+" Tr) from "+fiTr.getAbsolutePath());

			//Pour chaque Tr
			for(int i=0;i<nbEch;i++) {
				System.out.print("Recovery time "+i+" ");
				fiTr=new File(strPath,strEchoes[i]);
				strSlices=fiTr.list();
				fiTr=new File(fiTr.getAbsolutePath(),strSlices[0]);
				strSlices=fiTr.list();
				//Lire la Te et les Tr
				imgPar=IJ.openImage(fiTr.getAbsolutePath()+slash+strSlices[0]);
				String[]params=getParamsFromDCM(imgPar,MRI_T1_Seq.StandardMetadataLookup,"En deuxieme partie, effectué pour chaque Tr, ici i="+i);
				this.Tr[i]=Double.valueOf(params[4]);
				this.Te=Double.valueOf(params[5]);				
				
				//Importer la sequence
				String str=fiTr.getAbsolutePath();
				System.out.println("Importation de la sequence");
				System.out.println("path="+str);
				sourceData[i] = FolderOpener.open(str, "");
				VitimageUtils.imageCheckingFast(sourceData[i],"Source data["+i+"]");
			}
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
		String[]params=getParamsFromDCM(imgPar,MRI_T1_Seq.StandardMetadataLookup,"Called from setParams");
		
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
		for(int i=0;i<Tr.length ; i++) {
			sourceData[i]=IJ.openImage(strPath+slash+"Recovery_"+(i+1)+".tif");
		}			
	}
	
	public void writeStackedSourceData() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Stacks");
		boolean isCreated = dir.mkdirs();
		for(int i=0;i<this.Tr.length ; i++) {
			IJ.saveAsTiff(this.sourceData[i],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"Recovery_"+(i+1)+".tif");
		}
	}

	
	public void readRegisteredSourceData() {
		String strPath=this.sourcePath+slash+"Computed_data"+slash+"1_RegisteredStacks";
		for(int i=0;i<this.Tr.length ; i++) {
			this.sourceData[i]=IJ.openImage(strPath+slash+"Recovery_"+(i+1)+".tif");
		}		
		setImageForRegistration();
	}
	
	public void writeRegisteredSourceData() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_RegisteredStacks");
		boolean isCreated = dir.mkdirs();
		for(int i=0;i<this.Tr.length ; i++) {
			IJ.saveAsTiff(this.sourceData[i],this.sourcePath+slash+"Computed_data"+slash+"1_RegisteredStacks"+slash+"Recovery_"+(i+1)+".tif");
		}			
	}


	public void readMask() {
		this.mask=IJ.openImage(this.sourcePath + slash + "Computed_data" + slash + "0_Stacks" + slash + "mask.tif");
	}
	
	public void writeMask() {
		IJ.saveAsTiff(this.mask,this.sourcePath + slash + "Computed_data" + slash + "0_Stacks" + slash + "mask.tif");
	}
	
	public void readMaps() {
		this.computedData=new ImagePlus[3];
		this.computedData[0]=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_Maps"+slash+"MapTrN.tif");
		this.computedData[1]=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_Maps"+slash+"MapM0.tif");
		this.computedData[2]=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_Maps"+slash+"MapT1.tif");
	}
	
	public void writeMaps() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"2_Maps");
		boolean isCreated = dir.mkdirs();
		IJ.saveAsTiff(this.computedData[0],this.sourcePath+slash+"Computed_data"+slash+"2_Maps"+slash+"MapTrN.tif");
		IJ.saveAsTiff(this.computedData[1],this.sourcePath+slash+"Computed_data"+slash+"2_Maps"+slash+"MapM0.tif");
		IJ.saveAsTiff(this.computedData[2],this.sourcePath+slash+"Computed_data"+slash+"2_Maps"+slash+"MapT1.tif");
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
		if(step>=1) readStackedSourceData();
		if(step>=2) readRegisteredSourceData();		
		if(step>=3) {readMask();readMaps();}
		if(step>=4) readHyperImage();
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
	
	
	public void registerEchoes() {
		for(int i=0;i<this.sourceData.length-1;i++) {
			System.out.println("Recalage sequence numero "+i+" sur sequence numero "+(sourceData.length-1));
			ItkRegistrationManager manager=new ItkRegistrationManager();
			ImagePlus[] result=manager.runScenarioInterEchoes(sourceData[sourceData.length-1], sourceData[i]);
			System.out.println("Recalage sequence numero "+i+" ok");
			sourceData[i]=VitimageUtils.convertFloatToShortWithoutDynamicChanges(result[0]);
			System.out.println("Conversion "+i+" ok");
			sourceData[i].setTitle("Echo i ="+i);
			System.out.println("Conversion accomplie"+i+" ok");
			//result[1].show();
		}
	}



	
	public void computeMaps() {
		ImagePlus []smoothCapSourceData=this.getSourceDataWithSmoothedCapillary();
		System.out.println("");
		System.out.println("Calcul de la carte "+this.title);
		if(smoothCapSourceData[0].getType() != ImagePlus.GRAY16) {VitiDialogs.notYet("smoothCapsourceData.getType != 16 in computeMaps in MRI_T1_Seq : "+smoothCapSourceData[0].getType());return;}
		final int nEch=smoothCapSourceData.length;
		for(int i=0;i<smoothCapSourceData.length ; i++) {
			smoothCapSourceData[i]=VitimageUtils.convertFloatToShortWithoutDynamicChanges(
					VitimageUtils.gaussianFiltering(
							smoothCapSourceData[i],this.voxSX()*sigmaGaussMapInPixels,this.voxSY()*sigmaGaussMapInPixels,this.voxSX()*sigmaGaussMapInPixels));//It's no error : no "big smoothing" over Z, due to misalignment
		}
		computedData=new ImagePlus[3];
		computedData[0]=VitimageUtils.convertShortToFloatWithoutDynamicChanges(this.sourceData[this.sourceData.length-1]);
		computedData[1]=VitimageUtils.convertShortToFloatWithoutDynamicChanges(smoothCapSourceData[0]);
		computedData[2]=VitimageUtils.convertShortToFloatWithoutDynamicChanges(smoothCapSourceData[0]);
			
		this.computedData[1].getProcessor().set(0);
		this.computedData[2].getProcessor().set(0);
		double factorSeconds=8E-6;
		final int fitType=MRUtils.T1_RECOVERY_RICE;
		final int algType=MRUtils.SIMPLEX;
		final int X=this.dimX();
		final int Y=this.dimY();
		final int Z=this.dimZ();
		final double []threadTr=this.Tr;
		this.caracterizeBackground(); 
		double sigma=this.computeRiceSigmaFromBackgroundValues();System.out.println("T1 map computation. sigma Rice="+sigma);
		System.out.println("T1 map computation. Background : mean="+this.meanBackground+" , sigma="+this.sigmaBackground);

		
		final short[][][]tabData=new short[nEch][Z][];
		final byte[][]tabMask=new byte[Z][];
		for(int i=0;i<nEch ;i++)for(int z=0;z<Z ; z++) tabData[i][z]=(short[])smoothCapSourceData[i].getStack().getProcessor(z+1).getPixels();
		for(int z=0;z<Z ; z++)tabMask[z]=(byte[])this.mask.getStack().getProcessor(z+1).getPixels();
		final FloatProcessor[] tempComputedT1=new FloatProcessor[Z];
		final FloatProcessor[] tempComputedM0=new FloatProcessor[Z];
		final AtomicInteger incrZ = new AtomicInteger(0);
		final AtomicInteger incrProcess = new AtomicInteger(0);
		final int totLines=Y*Z;

			
		System.out.println("Multi-threaded T1 map computation. start fit on "+(X*Y*Z)+" voxels.\n--> Estimated time  @2.5Ghz @12 cores : "+VitimageUtils.dou(factorSeconds*(X*Y*Z) )+" s");
		final Thread[] threads = VitimageUtils.newThreadArray(Z);    
		for (int ithread = 0; ithread < Z; ithread++) {  
			threads[ithread] = new Thread() {  { setPriority(Thread.NORM_PRIORITY); }  
			public void run() {  
				FloatProcessor threadCompT1=new FloatProcessor(X,Y);
				FloatProcessor threadCompM0=new FloatProcessor(X,Y);
				float[]tabThreadT1=(float[])threadCompT1.getPixels();
				float[]tabThreadM0=(float[])threadCompM0.getPixels();
				
				double[]echoesForThisVoxel=new double[nEch];		
				double[]estimatedParams;
				int z = incrZ.getAndIncrement();

				for(int y=0;y<Y;y++) {
					int incrTot=incrProcess.getAndIncrement();
					if(incrTot%500==0)System.out.println("Processing map :, "+VitimageUtils.dou(incrTot*100.0/totLines)+" %");
					for(int x=0;x<X;x++) {
						int index=y*X+x;
						for(int ech=0;ech<nEch;ech++)echoesForThisVoxel[ech]= tabData[ech][z][index];
						estimatedParams=MRUtils.makeFit(threadTr, echoesForThisVoxel,fitType,algType,100,sigma);
						if((int)((byte)tabMask[z][index] &0xff) >0 ) {
							tabThreadT1[index]=(float)estimatedParams[1];
							tabThreadM0[index]=(float)estimatedParams[0];
						}
					}
				}
				tempComputedT1[z]=threadCompT1;
				tempComputedM0[z]=threadCompM0;
			}};  
		}  		

		VitimageUtils.startAndJoin(threads);  
		for (int z=0; z< Z; z++) {  
			this.computedData[1].getStack().setProcessor(tempComputedM0[z], z+1);
			this.computedData[2].getStack().setProcessor(tempComputedT1[z], z+1);
		}  
		
		for (int z=0; z< Z; z++) {
			float []tabEcho=(float[])this.computedData[0].getStack().getProcessor(z+1).getPixels();		
			for(int y=0;y<Y;y++) {
				for(int x=0;x<X;x++) {
					int index=y*X+x;
					if((int)((byte)tabMask[z][index] &0xff) == 0 ) tabEcho[index]=0;
				}
			}
		}
					
		this.computedData[0].getProcessor().setMinAndMax(0, defaultNormalisationValues()[0]);
		this.computedData[1].getProcessor().setMinAndMax(0, defaultNormalisationValues()[1]);
		this.computedData[2].getProcessor().setMinAndMax(0, defaultNormalisationValues()[2]);
	}  


	public ImagePlus computeNormalizedHyperImage() {
		if(this.capillary == Capillary.HAS_CAPILLARY)this.computeNormalisationValues();
		else this.computedValuesNormalisationFactor=defaultNormalisationValues();
		IJ.saveAsTiff(this.computedData[1],"/home/fernandr/Bureau/debugM0MapOfT1.tif");
		IJ.saveAsTiff(this.computedData[0],"/home/fernandr/Bureau/debugT1MapOfT1.tif");
		System.out.println("Normalisation factor. Last Tr image="+this.computedValuesNormalisationFactor[0]);
		System.out.println("Normalisation factor. M0 image="+this.computedValuesNormalisationFactor[1]);
		System.out.println("Normalisation factor. T1 image="+this.computedValuesNormalisationFactor[2]);
		int X=this.dimX();
		int Y=this.dimY();
		int Z=this.dimZ();
		int index;

		float[][][]tabComputed=new float[3][Z][];
		for(int z=0;z<Z;z++) {
			tabComputed[0][z]=(float[])this.computedData[0].getStack().getProcessor(z+1).getPixels();
			tabComputed[1][z]=(float[])this.computedData[1].getStack().getProcessor(z+1).getPixels();
			tabComputed[2][z]=(float[])this.computedData[2].getStack().getProcessor(z+1).getPixels();
		}
		for(int z=0;z<Z;z++) {
			for(int y=0;y<Y;y++) {
				for(int x=0;x<X;x++) {
					index=y*X+x;
					for(int i=0;i<this.computedData.length;i++)tabComputed[i][z][index]/=this.computedValuesNormalisationFactor[i];
					if(this.removeOutliers) {
						if( (tabComputed[1][z][index]>this.thresholdOutlier) || (tabComputed[2][z][index]>this.thresholdOutlier) ) {tabComputed[1][z][index]=0;tabComputed[2][z][index]=0;}						
					}
				}
			}
		}
		this.normalizedHyperImage=Concatenator.run(computedData);
		this.normalizedHyperImage.getProcessor().setMinAndMax(0,1);
		return this.normalizedHyperImage;
	}





	

	public double[]defaultNormalisationValues(){
		return new double[] {10000,10000,3500};
	}
	
	public void setImageForRegistration() {
		this.imageForRegistration=new Duplicator().run(this.sourceData[this.sourceData.length-1]);
		this.valMinForCalibration=this.imageForRegistration.getProcessor().getMin();
		this.valMaxForCalibration=this.imageForRegistration.getProcessor().getMax();
		this.valMedForThresholding=VitimageUtils.getOtsuThreshold(this.imageForRegistration)*0.8;
//		this.valMedForThresholding=Math.max(500,this.valMedForThresholding/2 );
	}

	public void setTe(double Te) {
		this.Te=Te;
	}

	public void setTr(double[]tr) {
		if(tr.length != sourceData.length) IJ.showMessage("Warning : Tr length and number of images does not match :"+
				tr.length+" and "+sourceData.length+" . Computation set to the minimum of two");
		this.Tr=new double[tr.length];
		for(int i=0;i<tr.length ; i++)this.Tr[i]=tr[i];
	}


	
}

