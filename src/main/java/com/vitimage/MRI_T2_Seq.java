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
import ij.process.FloatProcessor;

public class MRI_T2_Seq extends Acquisition{
	private final double sigmaGaussMapInPixels=0.5;
	private boolean removeOutliers=true;
	private double thresholdOutlier=1.5;
	private double Te[];
	private double Tr;
	private double fieldPower;
	private boolean oldBehaviour=false;//if true : old way to export data in directories, until end 2018
	private static String []StandardMetadataLookup= { "Study Date", "Study Time", "Study Description" , "Slice Thickness", "Repetition Time" ,
											   "Echo Time" , "Image Position (Patient)" , "Image Orientation (Patient)"  , "Rows" , "Columns" ,
										   	   "Pixel Spacing" , "Bits Allocated" };

	/**
	 * Test sequences
	 */
	public static void main (String []args) {
		ImageJ ij=new ImageJ();
		System.out.println("Test procedure start...");
		MRI_T2_Seq mri=new MRI_T2_Seq("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B001_PAL/Source_data/J000/Source_data/MRI_T2_SEQ",Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,"Test_T2");
		mri.start();//
	}


	
	
	/**
	 * Constructors, factory and top level functions
	 */

	public MRI_T2_Seq(String sourcePath,Capillary cap,SupervisionLevel sup,String title) {
		super(AcquisitionType.MRI_T2_SEQ, sourcePath,cap,sup);
		this.title=title;
		this.hyperSize=3;
	}

	public static MRI_T2_Seq MRI_T2_SeqFactory(String sourcePath,Capillary cap,SupervisionLevel sup,String dataPath) {
		MRI_T2_Seq mri=new MRI_T2_Seq(sourcePath,cap,sup,"Factory");
		mri.start();
		return mri;
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
		case 1: //data are read. Time to compute Maps
			this.computeMaps();
			writeMaps();
			break;
		case 2: //data are registered. Time to compute Maps
			this.computeNormalizedHyperImage();
			writeHyperImage();
			break;
		case 3:
			System.out.println("MRI_T2_SEQ Computation finished for "+this.getTitle());
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
			correctWeirdStuffInTheFirstSlices();
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
		this.Te=new double[Integer.valueOf(strFile[8])];
		this.sourceData=new ImagePlus[Integer.valueOf(strFile[8])];
		this.Tr=Double.valueOf(strFile[10]);
		String []strTr=strFile[9].split(" ");
		for(int i=0;i<this.Te.length ; i++)Te[i]=Double.valueOf(strTr[i]);
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
			out.write("NumberOfSuccessiveTe="+this.sourceData.length+"\n");
			out.write("Te(ms)="+VitimageUtils.writableArray(this.Te)+"\n");
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
	
	
	
	public void correctWeirdStuffInTheFirstSlices() {
		int []coordinates=this.detectCapillaryPosition(this.dimZ()/2);
		for(int ech=0;ech<this.sourceData.length ; ech++) {
			ImagePlus img=sourceData[ech];
			double valueSlice1=VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],0,2);
			double valueSlice2=VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],1,2);
			double valueOthers= 0.25 * ( VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],1,3) + 
										 VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],1,4) +
										 VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],1,5) + 
										 VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],1,6) );
			double factorSlice1=valueOthers/valueSlice1;
			double factorSlice2=valueOthers/valueSlice2;
			img.setSlice(1);
			IJ.run(img, "Multiply...", "value="+factorSlice1+" slice");
			img.setSlice(2);
			IJ.run(img, "Multiply...", "value="+factorSlice2+" slice");
		}
		this.setImageForRegistration();
	}
	
	/**
	 * I/O images helper functions
	 */
	public void readSourceData() {
		if( ! this.oldBehaviour) {
			
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
			String strMainPath=f.getAbsolutePath();
			String[] strTr=f.list();
			strTr=VitimageUtils.stringArraySort(strTr);
			int nbTr=strTr.length;
			int seqIndex=-1;
			
			//Identify the directory hosting the manipulation in T2
			for(int i=nbTr-1;i>0;i--) {
				System.out.println("Tr LU : i="+i+" : "+strTr[i]);
				if(strTr[i].equals("TR010000"))seqIndex=i;
			}
		
			//Extraire les parametres généraux depuis une des coupes	
			File fiTr=new File(strMainPath,strTr[seqIndex]);
			String strTrPath=fiTr.getAbsolutePath();
			
			String[]strEchoes=VitimageUtils.stringArraySort(fiTr.list());
			int nEch=strEchoes.length;
			File fiEc=new File(fiTr.getAbsolutePath(),strEchoes[0]);
			String[]strSlices=fiEc.list();

			ImagePlus imgPar=IJ.openImage(fiEc.getAbsolutePath()+slash+strSlices[0]);
			System.out.println("Parameters auto detection from : "+fiEc.getAbsolutePath()+slash+strSlices[0]);
			System.out.println("Appel à setParams");
			this.setParamsFromDCM(imgPar);
			System.out.println("Ok.");
		
			
			//Construire les tableaux en fonction du nombre d echos
			sourceData=new ImagePlus[nEch];
			Te=new double[nEch];

			System.out.println("T2 Sequences importation ("+nEch+" echos) from "+fiTr.getAbsolutePath());
		
			//Pour chaque Te
			for(int i=0;i<nEch;i++) {
				System.out.print(i+" ");
				fiEc=new File(fiTr.getAbsolutePath(),strEchoes[i]);
				strSlices=fiEc.list();
				
				//Lire la Te et les Tr
				imgPar=IJ.openImage(fiEc.getAbsolutePath()+slash+strSlices[0]);
				System.out.println("Lecture parametres echo "+i+" depuis fichier="+fiEc.getAbsolutePath()+slash+strSlices[0]);
				String[]params=getParamsFromDCM(imgPar,MRI_T2_Seq.StandardMetadataLookup,"En deuxieme partie, effectué pour chaque Tr, ici i="+i);
				this.Te[i]=Double.valueOf(params[5]);
				this.Tr=Double.valueOf(params[4]);				
				
				//Importer la sequence
				String str=fiEc.getAbsolutePath();
				sourceData[i] = FolderOpener.open(str, "");
				if (i%4==0)VitimageUtils.imageCheckingFast(sourceData[i],"Source data["+i+"]");
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
		System.out.println("Entree dans setParams (image phase 1 viewing)");
		String[]params=getParamsFromDCM(imgPar,MRI_T2_Seq.StandardMetadataLookup,"Called from setParams");
		
		this.acquisitionDate=VitimageUtils.getDateFromString(params[0]);
		this.acquisitionTime=params[1];	
		this.setTitle(params[2]);
		this.setVoxSZ(Double.valueOf(params[3]));
		this.Tr=Double.valueOf(params[4]);
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
		for(int i=0;i<Te.length ; i++) {
			sourceData[i]=IJ.openImage(strPath+slash+"Echo_"+(i+1)+".tif");
		}			
	}
	
	public void writeStackedSourceData() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Stacks");
		boolean isCreated = dir.mkdirs();
		for(int i=0;i<this.Te.length ; i++) {
			IJ.saveAsTiff(this.sourceData[i],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"Echo_"+(i+1)+".tif");
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
		this.computedData[0]=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_Maps"+slash+"MapEcho1.tif");
		this.computedData[1]=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_Maps"+slash+"MapM0.tif");
		this.computedData[2]=IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"2_Maps"+slash+"MapT2.tif");
	}
	
	public void writeMaps() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"2_Maps");
		boolean isCreated = dir.mkdirs();
		IJ.saveAsTiff(this.computedData[0],this.sourcePath+slash+"Computed_data"+slash+"2_Maps"+slash+"MapEcho1.tif");
		IJ.saveAsTiff(this.computedData[1],this.sourcePath+slash+"Computed_data"+slash+"2_Maps"+slash+"MapM0.tif");
		IJ.saveAsTiff(this.computedData[2],this.sourcePath+slash+"Computed_data"+slash+"2_Maps"+slash+"MapT2.tif");
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
		if(step>=2) readMaps();
		if(step>=3) readHyperImage();
	}
	
	
	public void computeMask() {
		System.out.println("Mask computation : gaussian filtering, and 3D connected component research (~1 mn)...");
		ImagePlus img=new Duplicator().run(sourceData[0]);
		img=VitimageUtils.gaussianFiltering(img,2*this.voxSX(),2*this.voxSY(),2*this.voxSX());
		double val=(this.capillary==Capillary.HAS_CAPILLARY ? this.getCapillaryValue(img) : this.defaultNormalisationValues()[0]);
		ImagePlus imgConObject=VitimageUtils.connexe(img,val/15, val*2,0,10E10,6,1,true);
		ImagePlus imgConCap=VitimageUtils.connexe(img,val/5, val*2,0,10E10,6,2,true);
		IJ.run(imgConObject,"8-bit","");
		IJ.run(imgConCap,"8-bit","");
		ImageCalculator ic=new ImageCalculator();
		this.mask = ic.run("OR create stack", imgConObject,imgConCap);
		System.out.println("Ok.");
		VitimageUtils.imageCheckingFast(this.mask,"T2 Mask");
	}
	

	

	
	
	
	
	
	
	


	public void computeMaps() {
		if(this.sourceData[0].getType() != ImagePlus.GRAY16) {VitiDialogs.notYet("sourceData.getType != 16 in computeMaps in MRI_T2_Seq : "+this.sourceData[0].getType());return;}
		final int nEch=this.sourceData.length;
		for(int i=0;i<sourceData.length ; i++) {
			sourceData[i]=VitimageUtils.convertFloatToShortWithoutDynamicChanges(
					VitimageUtils.gaussianFiltering(
							sourceData[i],this.voxSX()*sigmaGaussMapInPixels,this.voxSY()*sigmaGaussMapInPixels,this.voxSX()*sigmaGaussMapInPixels));//It's no error : no "big smoothing" over Z, due to misalignment
		}
		computedData=new ImagePlus[3];
		computedData[0]=VitimageUtils.convertShortToFloatWithoutDynamicChanges(this.sourceData[0]);
		computedData[1]=VitimageUtils.convertShortToFloatWithoutDynamicChanges(this.sourceData[0]);
		computedData[2]=VitimageUtils.convertShortToFloatWithoutDynamicChanges(this.sourceData[0]);
		this.computedData[1].getProcessor().set(0);
		this.computedData[2].getProcessor().set(0);
		double factorSeconds=8E-5;
		final int fitType=MRUtils.T2_RELAX_RICE;
		final int algType=MRUtils.SIMPLEX;
		final int X=this.dimX();
		final int Y=this.dimY();
		final int Z=this.dimZ();
		final double []threadTe=this.Te;
		this.caracterizeBackground();
		double sigma=this.computeRiceSigmaFromBackgroundValues();System.out.println("T2 map computation. sigma Rice="+sigma);
		System.out.println("T2 map computation. Background : mean="+this.meanBackground+" , sigma="+this.sigmaBackground);

		
		final short[][][]tabData=new short[nEch][Z][];
		final byte[][]tabMask=new byte[Z][];
		for(int i=0;i<nEch ;i++)for(int z=0;z<Z ; z++) tabData[i][z]=(short[])this.sourceData[i].getStack().getProcessor(z+1).getPixels();
		for(int z=0;z<Z ; z++)tabMask[z]=(byte[])this.mask.getStack().getProcessor(z+1).getPixels();
		final FloatProcessor[] tempComputedT2=new FloatProcessor[Z];
		final FloatProcessor[] tempComputedM0=new FloatProcessor[Z];
		final FloatProcessor[] tempComputedEcho1=new FloatProcessor[Z];
		final AtomicInteger incrZ = new AtomicInteger(0);
		final AtomicInteger incrProcess = new AtomicInteger(0);
		final int totLines=Y*Z;
		
			
		System.out.println("Multi-threaded T2 map computation. start fit on "+(X*Y*Z)+" voxels.\n Estimated time @2.5Ghz x 1 core ="+VitimageUtils.dou(factorSeconds*(X*Y*Z)));
		final Thread[] threads = VitimageUtils.newThreadArray(Z);    
		for (int ithread = 0; ithread < Z; ithread++) {  
			threads[ithread] = new Thread() {  { setPriority(Thread.NORM_PRIORITY); }  
			public void run() {  
				FloatProcessor threadCompT2=new FloatProcessor(X,Y);
				FloatProcessor threadCompM0=new FloatProcessor(X,Y);
				float[]tabThreadT2=(float[])threadCompT2.getPixels();
				float[]tabThreadM0=(float[])threadCompM0.getPixels();
				
				double[]echoesForThisVoxel=new double[nEch];		
				double[]estimatedParams;
				int z = incrZ.getAndIncrement();
				
				for(int y=0;y<Y;y++) {
					int incrTot=incrProcess.getAndIncrement();
					if(incrTot%500==0)System.out.println("Processing map, "+VitimageUtils.dou(incrTot*100.0/totLines)+" %");
					for(int x=0;x<X;x++) {
						int index=y*X+x;
						for(int ech=0;ech<nEch;ech++)echoesForThisVoxel[ech]= tabData[ech][z][index];
						estimatedParams=MRUtils.makeFit(threadTe, echoesForThisVoxel,fitType,algType,100,sigma);
						if((int)((byte)tabMask[z][index] &0xff) >0 ) {
							tabThreadT2[index]=(float)estimatedParams[1];
							tabThreadM0[index]=(float)estimatedParams[0];
						}
					}
				}
				tempComputedT2[z]=threadCompT2;
				tempComputedM0[z]=threadCompM0;
			}};  
		}  		

		VitimageUtils.startAndJoin(threads);  
		for (int z=0; z< Z; z++) {  
			this.computedData[1].getStack().setProcessor(tempComputedM0[z], z+1);
			this.computedData[2].getStack().setProcessor(tempComputedT2[z], z+1);
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
		System.out.println("Normalisation factor. First echo image="+this.computedValuesNormalisationFactor[0]);
		System.out.println("Normalisation factor. M0 image="+this.computedValuesNormalisationFactor[1]);
		System.out.println("Normalisation factor. T2 image="+this.computedValuesNormalisationFactor[2]);
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
					for(int i=0;i<computedData.length;i++)tabComputed[i][z][index]/=this.computedValuesNormalisationFactor[i];
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
		return new double[] {7000,7000,200};
	}


	public void setImageForRegistration() {
		this.imageForRegistration=new Duplicator().run(this.sourceData[0]);
		this.valMinForCalibration=this.imageForRegistration.getProcessor().getMin();
		this.valMaxForCalibration=this.imageForRegistration.getProcessor().getMax();
		this.valMedForThresholding=VitimageUtils.getOtsuThreshold(this.imageForRegistration);
		this.valMedForThresholding=Math.max(500,this.valMedForThresholding/2 );
	}


	public void setTe(double []te) {
		if(te.length != sourceData.length) IJ.showMessage("Warning  in MRI T2: Te length and number of images does not match :"+
				te.length+" and "+sourceData.length+" . Computation set to the minimum of two");
		this.Te=new double[te.length];
		for(int i=0;i<te.length ; i++)this.Te[i]=te[i];
	}

	public void setTr(double Tr) {
		this.Tr=Tr;
	}

	public void processData() {
		this.start();
	}
}
