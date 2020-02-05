/**
 * This class is an implementation of Acquisition, in the case of a MRI T1 sequence. These sequences are successive 3D observations,
 * obtained at varying Trecovery(0.6 s, 1.2s, 2.4s, ...), with a constant Techo.These helps to fit the parameter T1, according to 
 * the equation of the spin echo sequence.
 */

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
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.vitimage.MetricType;
import com.vitimage.Transform3DType;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.process.FloatProcessor;
import math3d.Point3d;



/**
 * @author fernandr
 *
 */
public class MRI_Clinical extends Acquisition implements Fit,ItkImagePlusInterface{
	public static String[]lookupStr=new String[] {"3DT1", "3DT2", "AxDPdixon_F" , "AxDif_ADC1", "AxDPdixon_W","AxDPdixon_in", "AxDPdixon_opp" , };
	public static int[]normalizationValues=new int[] {240,151,750,3500,900,900,900};
	public static int N_DATA=7;
	public double[]valBG=new double[3];
	public double[]valCap=new double[3];
	private double field;
	private String[] units;
	private double[] Te;
	private double[] Tr;
	private double[][] voxelSizeSource;
	private double[][] vectX;
	private int[] dimsY;
	private int[] dimsX;
	private int[] dimsZ;
	private double[][] vectY;
	private double[] voxsX;
	private double[] voxsY;
	private double[] voxsZ;
	private String[] titles;
	private double[][] translation;
	private double[][] vectZ;
	private ItkTransform[] transformations;
	private ImagePlus normalizedHyperImageAxial;
	private static String []StandardMetadataLookup= { "Acquisition Date", "Slice Thickness", "Repetition Time" , "Echo Time", "Magnetic Field Strength" ,
			"Image Position (Patient)" , "Image Orientation (Patient)" , "Rows" , "Columns" ,
										   	   "Pixel Spacing" };
			
	public static void main (String []args) {
		ImageJ ij=new ImageJ();
		System.out.println("Test procedure start...");
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3" ,"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(String spec : specimen) {
			File f=new File("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/MRI_CLINICAL/STEPS_DONE.tag");
			//f.delete();
			MRI_Clinical mri=new MRI_Clinical("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/MRI_CLINICAL",
										Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,spec,ComputingType.COMPUTE_ALL);
			mri.start(2);
		}
		return;
	}


	
	
	/**
	 * Constructors, factory and top level functions
	 */
	public MRI_Clinical(String sourcePath,Capillary cap,SupervisionLevel sup,String title,ComputingType computingType) {
		super(AcquisitionType.MRI_CLINICAL, sourcePath,cap,sup);
		this.titles=new String[N_DATA];
		this.sourceData=new ImagePlus[N_DATA];
		this.units=new String[N_DATA];	 
		this.Tr=new double[N_DATA];	 
		this.Te=new double[N_DATA];	
		
		this.translation=new double[N_DATA][3];
		this.vectX=new double[N_DATA][3];
		this.vectY=new double[N_DATA][3];
		this.vectZ=new double[N_DATA][3];
		this.transformations=new ItkTransform[N_DATA];
		
		this.dimsY=new int[N_DATA];	 
		this.dimsX=new int[N_DATA];	 
		this.dimsZ=new int[N_DATA];	 
		
		this.voxsX=new double[N_DATA];	 
		this.voxsY=new double[N_DATA];	 
		this.voxsZ=new double[N_DATA];	 
		this.voxelSizeSource=new double[N_DATA][3];
		this.computingType=computingType;
		this.title=title;
		this.hyperSize=N_DATA;
		this.operator="Samuel";
		this.acquisitionPlace="Clinique du parc";
	}
	
	public void start(int endStep) {
		this.printStartMessage();
		quickStartFromFile();
		while(nextStep(endStep));
	}
	
	public boolean nextStep(int endStep){
		int a=readStep();
		System.out.println("MRI_CLINICAL "+this.getTitle()+"------> "+ " next step : "+a+" "+this.getTitle());
		if(a>=endStep) {
			System.out.println("Last step. Exit");
			return false;
		}
		switch(a) {
		case -1:
			IJ.showMessage("Critical fail, no directory found Source_data in the current directory");
			return false;
		case 0: // rien --> exit
			IJ.log("No data in this directory");
			return false;
		case 1: //data are read. Time to register
			this.computeNormalizedHyperImage();
			writeHyperImage();
			break;
		case 3:
			System.out.println("MRI_T1_Seq Computation finished for "+this.getTitle());
			return false;
		}
		writeStep(a+1);	
		return true;
	}

	

	public void readProcessedImages(int step){
		if(step <1) IJ.log("Error : read process images, but step is lower than 1");
		if(step>=1) readStackedSourceData();
		if(step>=1) readTransformations();
		if(step>=4) readHyperImage();
	}
	
		
	
	
	/**
	 * I/O writers/readers helpers functions
	 */
	@Override
	public void quickStartFromFile() {
		//Gather the path to source data
		System.out.println("Quick start : MRI Clinical hosted in ");
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
			System.out.println("Writing parameters file");
			writeParametersToHardDisk();
			System.out.println("Writing stacks and transformations");
			writeStackedSourceData();
			writeTransformations();
//			setImageForRegistration();
//			this.computeMask();
//			writeMask();
			System.out.println("Writing step 1");
			writeStep(1);
		}
	}

	
	
	
	/**
	 * I/O images helper functions
	 */
	public void readSourceData() {
		File directory = new File(this.sourcePath,"Source_data");//Le dossier d output

		//Localiser et oouvrir le dossier d input. Il ouvre sur les dossiers 
		File pathlink = new File(this.sourcePath,"DATA_PATH.tag");
		File f=null;
		try {
			 String str= Files.lines(Paths.get(pathlink.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 String strFile=str.split("=")[1];
			 System.out.println("Open data directory : "+strFile);
			 f=new File(strFile); 
		} catch (IOException ex) {        ex.printStackTrace();   }
		if(! f.exists()) {IJ.showMessage("Source path not found : "+this.sourcePath+slash+"Source_data"+slash+"SL_RAW");return;}

		String strPath=f.getAbsolutePath();
		for(int seq=0;seq<N_DATA; seq++) {
			String sequence=lookupStr[seq];
			this.titles[seq]=sequence;
			System.out.println("\nLooking for sequence "+sequence);
			File fseq=new File(strPath,sequence);
			
			//Get the first slice to take parameters out
			String[] lsdir=fseq.list();
			fseq=new File(fseq.getAbsolutePath(),"DICOM");		lsdir=fseq.list();	
			fseq=new File(fseq.getAbsolutePath(),lsdir[0]);     lsdir=fseq.list();
			fseq=new File(fseq.getAbsolutePath(),lsdir[0]);
			System.out.println("Dossier d images localise : "+fseq.getAbsolutePath());
			lsdir=fseq.list();
			ImagePlus imgPar1=IJ.openImage(fseq.getAbsolutePath()+slash+lsdir[0]);
			ImagePlus imgParN=IJ.openImage(fseq.getAbsolutePath()+slash+lsdir[lsdir.length-1]);
			
			//Import the stack and set parameters
			sourceData[seq] = FolderOpener.open(fseq.getAbsolutePath(), "");
			if(seq>=2)IJ.run(sourceData[seq] , "Flip Z", "");
			this.setParamsOfSourceStack(imgPar1,imgParN,seq,sourceData[seq].getStackSize());
		}
	}

	public String[] getParamsFromSlice(ImagePlus imgPar,String []paramsWanted,String debug){
		if(imgPar==null) IJ.showMessage("Warning : image used for parameters detection is detected as null image. Computation will fail");
		String paramGlob=imgPar.getInfoProperty();
		String[]paramLines=paramGlob.split("\n");
		String[]ret=new String[paramsWanted.length];
		for(int i=0;i<paramsWanted.length;i++) {
			ret [i]="NOT DETECTED";
			for(int j=0;j<paramLines.length ;j++) {
				if(paramLines[j].split(": ").length==2 && paramLines[j].split(": ")[0].indexOf(paramsWanted[i])>=0)ret[i]=paramLines[j].split(": ")[1];
			}
		}
		return ret;
	}
	
	
	
	public void setParamsOfSourceStack(ImagePlus imgPar1,ImagePlus imgParN,int i,int Z) {
		String[]ret=getParamsFromSlice(imgPar1,MRI_Clinical.StandardMetadataLookup,"Called from setParams");
		String[]retN=getParamsFromSlice(imgParN,MRI_Clinical.StandardMetadataLookup,"Called from setParams");
		this.acquisitionDate=new Date(Integer.parseInt(ret[0].substring(0, 4)),Integer.parseInt(ret[0].substring(4, 6)),Integer.parseInt(ret[0].substring(6, 8)));		//Study Date 
		this.Tr[i]=Double.parseDouble(ret[2]);		//Tr 
		this.Te[i]=Double.parseDouble(ret[3]);		//Te
		this.field=Double.parseDouble(ret[4]);		//Field 
	
		//Translation vers premiere slice
		String[]coords=(i<2 ? ret[5].split("\\\\") : retN[5].split("\\\\"));		
		this.translation[i]=new double[] {Double.parseDouble(coords[0]),Double.parseDouble(coords[1]),Double.parseDouble(coords[2])};

		//Matrice d orientation 
		coords=ret[6].split("\\\\");		
		this.vectX[i]=new double[] {Double.parseDouble(coords[0]),Double.parseDouble(coords[1]),Double.parseDouble(coords[2])};
		this.vectY[i]=new double[] {(i<0 ? -1 : 1)*Double.parseDouble(coords[3]),(i<0 ? -1 : 1)*Double.parseDouble(coords[4]),(i<0 ? -1 : 1)*Double.parseDouble(coords[5])};
		this.vectZ[i]=TransformUtils.vectorialProduct(this.vectX[i], this.vectY[i]);
		if(i<2)this.transformations[i]=new ItkTransform(ItkTransform.itkTransformFromDICOMVectorsNOAXIAL(this.vectX[i],this.vectY[i],this.vectZ[i],this.translation[i]));
		else this.transformations[i]=new ItkTransform(ItkTransform.itkTransformFromDICOMVectorsNOAXIAL(this.vectX[i],this.vectY[i],this.vectZ[i],this.translation[i]));
		System.out.println("Matrice "+i+" : "+this.transformations[i].drawableString());
		
		//Stack dimensions
		this.dimsY[i]=Integer.parseInt(ret[7]); 
		this.dimsX[i]=Integer.parseInt(ret[8]);
		this.dimsZ[i]=Z;
		this.units[i]="mm";

		//Voxel sizes X et Y
		coords=ret[9].split("\\\\");		
		this.voxsX[i]=Double.parseDouble(coords[0]);
		this.voxsY[i]=Double.parseDouble(coords[1]);

		//Voxel size Z
		coords=(i<=1) ? retN[5].split("\\\\") : ret[5].split("\\\\");		
		this.vectZ[i]=new double[]{1.0/(Z-1)*(Double.parseDouble(coords[0])-this.translation[i][0]),1.0/(Z-1)*(Double.parseDouble(coords[1])-this.translation[i][1]),1.0/(Z-1)*(Double.parseDouble(coords[2])-this.translation[i][2])};
		this.voxsZ[i]=TransformUtils.norm(this.vectZ[i]);
		System.out.println("Et voxsZ["+i+"="+voxsZ[i]);
		this.voxelSizeSource[i]=new double[] {voxsX[i],voxsY[i],voxsZ[i]};
		VitimageUtils.adjustImageCalibration(this.sourceData[i],this.voxelSizeSource[i],this.units[i]);
	}
	
	
	public ImagePlus computeNormalizedHyperImage() {
		boolean makeRegistration=false;
		boolean makeNormalization=false;
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"3_HyperImage");
		boolean isCreated = dir.mkdirs();
		ImagePlus []tabSeqReg=new ImagePlus[N_DATA];
		ImagePlus []tabSeqReg2=new ImagePlus[N_DATA];
		tabSeqReg[0]=new Duplicator().run(this.sourceData[0]);
		if(makeNormalization)tabSeqReg[0].setDisplayRange(valBG[0], valCap[0]);
		else tabSeqReg[0].setDisplayRange(0,this.normalizationValues[0]);
		IJ.run(tabSeqReg[0],"8-bit","");
		VitimageUtils.adjustImageCalibration(tabSeqReg[0],this.sourceData[0]);
		tabSeqReg2[0]=VitimageUtils.writeTextOnImage("", tabSeqReg[0],15,0);
		tabSeqReg[0]=VitimageUtils.writeTextOnImage(this.titles[0], tabSeqReg[0],15,0);
		IJ.saveAsTiff(tabSeqReg2[0],this.sourcePath+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_0.tif");

		ItkTransform trDeCote=null;
		for(int i=1;i<N_DATA;i++) {
			System.out.println("Recalage sequence "+i+" sur sequence 0");
			/////////////CALCUL TRANSFORMATION COMPOSEE	
			ItkTransform trTemp=new ItkTransform(this.transformations[i].getInverse());
			trTemp.addTransform(this.transformations[0]);
			trTemp.writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations"+slash+"trans_rig"+i+".txt");
			if(i<3) {
				tabSeqReg[i]=VitimageUtils.imageCopy(sourceData[i]);
				if(makeNormalization)tabSeqReg[i].setDisplayRange(valBG[i], valCap[i]);
				else tabSeqReg[i].setDisplayRange(0,this.normalizationValues[i]);
				IJ.run(tabSeqReg[i],"8-bit","");			
				if(i==1) {
					if(makeRegistration) {
						trTemp=registerT1T2(this.sourceData[0],this.sourceData[i],trTemp,i);
						trTemp=trTemp.flattenDenseField(VitimageUtils.Sub222(this.sourceData[0]));
						trTemp.writeAsDenseField(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations"+slash+"trans_dense"+i+".tif", VitimageUtils.Sub222(this.sourceData[0]));
					}
					else trTemp=ItkTransform.readAsDenseField(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations"+slash+"trans_dense"+i+".tif");
				}
				if(i==2) {
					if(makeRegistration) {
						ImagePlus temp=VitimageUtils.makeOperationBetweenTwoImages(tabSeqReg[1],tabSeqReg[0],VitimageUtils.OP_MEAN, false);
						temp.setDisplayRange(0,255);
						IJ.run(temp,"8-bit","");
						trTemp=registerT1T2(temp,tabSeqReg[i],trTemp,i);
						trTemp=trTemp.flattenDenseField(VitimageUtils.Sub222(this.sourceData[0]));
						trTemp.writeAsDenseField(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations"+slash+"trans_dense"+i+".tif", VitimageUtils.Sub222(this.sourceData[0]));
					}
					else {
						trTemp=ItkTransform.readAsDenseField(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations"+slash+"trans_dense"+i+".tif");
					}
				}
			}	
			/////////////TRANSFORMATIONS
			tabSeqReg[i]=trTemp.transformImage(this.sourceData[0],this.sourceData[i],false);
			if(i<3 && makeNormalization) tabSeqReg[i].setDisplayRange(valBG[i], valCap[i]);
			else tabSeqReg[i].setDisplayRange(0,this.normalizationValues[i]);
			
			tabSeqReg[i].setSlice(100);			
			IJ.run(tabSeqReg[i],"8-bit","");
			tabSeqReg2[i]=VitimageUtils.writeTextOnImage("", tabSeqReg[i],15,0);
			tabSeqReg[i]=VitimageUtils.writeTextOnImage(this.titles[i], tabSeqReg[i],15,0);
			IJ.saveAsTiff(tabSeqReg2[i],this.sourcePath+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_"+i+".tif");
		}
		this.normalizedHyperImage=Concatenator.run(tabSeqReg);
		
		for(int i=0;i<N_DATA;i++) {
			tabSeqReg2[i]=VitimageUtils.switchAxis(tabSeqReg2[i],2);			
			tabSeqReg2[i]=VitimageUtils.writeTextOnImage(this.titles[i], tabSeqReg2[i],15,0);
			IJ.run(tabSeqReg2[i], "Flip Z", "");
		}
		
		this.normalizedHyperImageAxial=Concatenator.run(tabSeqReg2);
		return this.normalizedHyperImageAxial;
	}
		
		
	public void setImageForRegistration() {
		this.imageForRegistration=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_0.tif");
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	public void readParametersFromHardDisk() {
		File fParam=new File(this.sourcePath,"DATA_PARAMETERS.tag");
		String[]strFile=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
       } catch (IOException ex) {        ex.printStackTrace();   }
		for(int i=1;i<strFile.length ; i++) {
			String []tab=strFile[i].split("=");
			strFile[i]=tab.length>=2 ? tab[1] : "";
		}
		this.sourcePath=strFile[2];
		this.dataPath=strFile[3];
		this.setTitle(strFile[4]);
		this.projectName=strFile[5];
		this.acquisitionDate=VitimageUtils.getDateFromString(strFile[6]);
		this.acquisitionTime=strFile[7];
		this.operator=strFile[8];
		this.acquisitionPlace=strFile[9];
		this.field=Double.parseDouble(strFile[10]);
		for(int seq=0;seq<N_DATA;seq++) {
			int start=11+seq*9+Math.min(3, seq)*2;
			this.titles[seq]=strFile[start+1];
			this.dimsX[seq]=Integer.valueOf(strFile[start+2]);
			this.dimsY[seq]=Integer.valueOf(strFile[start+3]);
			this.dimsZ[seq]=Integer.valueOf(strFile[start+4]);
			this.units[seq]=strFile[start+5];
			this.voxsX[seq]=Double.parseDouble(strFile[start+6]);
			this.voxsY[seq]=Double.parseDouble(strFile[start+7]);
			this.voxsZ[seq]=Double.parseDouble(strFile[start+8]);	
			if(seq<3) {
				this.valBG[seq]=Double.parseDouble(strFile[start+9]);
				this.valCap[seq]=Double.parseDouble(strFile[start+10]);
			}
		}
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
			out.write("Project="+this.getProjectName() +"\n");
			out.write("AcquisitionDate="+this.acquisitionDate+"\n");
			out.write("AcquisitionTime="+this.acquisitionTime+"\n");
			out.write("Operator="+this.operator+"\n");
			out.write("AcquisitionPlace="+this.acquisitionPlace+"\n");
			out.write("Field="+this.field+"\n");
			for(int seq=0;seq<N_DATA;seq++) {
				out.write("########\n");
				out.write("Sequence="+seq+" "+this.titles[seq]+"\n");				
				out.write("DimX(pix)="+this.dimsX[seq] +"\n");
				out.write("DimY(pix)="+this.dimsY[seq] +"\n");
				out.write("DimZ(pix)="+this.dimsZ[seq]+"\n" );
				out.write("Unit="+this.units[seq] +"\n");
				out.write("VoxSX="+this.voxsX[seq] +"\n");
				out.write("VoxSY="+this.voxsY[seq] +"\n");
				out.write("VoxSZ="+this.voxsZ[seq] +"\n");
				if(seq<3)out.write("BG="+valBG[seq]+"\nCap="+valCap[seq]);
			}
			out.close();
		} catch (Exception e) {IJ.error("Unable to write transformation to file: "+fParam.getAbsolutePath()+"error: "+e);}	
	}
	
	

	
	public void readStackedSourceData() {
		File f=new File(this.sourcePath, "Computed_data"+slash+"0_Stacks");
		String strPath=f.getAbsolutePath();
		for(int i=0;i<N_DATA ; i++) {
			sourceData[i]=IJ.openImage(strPath+slash+"Sequence_"+(i)+".tif");
		}			
	}
	
	public void writeStackedSourceData() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Stacks");
		boolean isCreated = dir.mkdirs();
		for(int i=0;i<N_DATA; i++) {
			IJ.saveAsTiff(this.sourceData[i],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"Sequence_"+(i)+".tif");
		}
	}

	public void readTransformations() {
		File f=new File(this.sourcePath, "Computed_data"+slash+"0_Stacks");
		String strPath=f.getAbsolutePath();
		for(int i=0;i<N_DATA ; i++) {
			transformations[i]=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations"+slash+"transformation_"+i+".txt");
		}			
	}
	
	public void writeTransformations() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations");
		boolean isCreated = dir.mkdirs();
		for(int i=0;i<N_DATA ; i++) {
			this.transformations[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Transformations"+slash+"transformation_"+i+".txt");
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
		this.normalizedHyperImageAxial =IJ.openImage(this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImageAxial.tif");
	}
	
	public void writeHyperImage() {
		IJ.saveAsTiff(this.normalizedHyperImage,this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		IJ.saveAsTiff(this.normalizedHyperImageAxial,this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImageAxial.tif");
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


	
	
	public static ItkTransform registerT1T2(ImagePlus imgT1,ImagePlus imgT2,ItkTransform trInit,int mod) {
		ImagePlus imgRef=VitimageUtils.Sub222(imgT1);
		ImagePlus imgMov=VitimageUtils.Sub222(imgT2);
		double sigma=12;
		int levelMax=2;
		int levelMin=2;
		int viewSlice=78;
		int nbIterations=mod==1 ? 5 : 3 ;
		int neighXY=2;	int neighZ=2;
		int bSXY=6;		int bSZ=6;
		int strideXY=2;		int strideZ=2;
		ItkTransform transRet=null;
		BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
					0,sigma,levelMin,levelMax,nbIterations,viewSlice,null,neighXY,neighXY,neighZ,bSXY,neighXY,bSZ,strideXY,strideXY,strideZ);
		bmRegistration.displayRegistration=0;
		bmRegistration.displayR2=false;
		bmRegistration.minBlockVariance=20;
		bmRegistration.minBlockScore=0.20;
		transRet=bmRegistration.runBlockMatching(trInit);
//		bmRegistration.closeLastImages();
		bmRegistration.freeMemory();
		return transRet;
	}

		



	
}

