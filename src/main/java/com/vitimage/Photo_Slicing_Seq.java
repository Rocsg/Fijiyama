package com.vitimage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.stream.Collectors;

import com.vitimage.MetricType;
import com.vitimage.Transform3DType;
import com.vitimage.Vitimage4D.VineType;
import Hough_Package.Hough_Circle;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.plugin.ImageCalculator;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;


public class Photo_Slicing_Seq extends Acquisition implements Fit,ItkImagePlusInterface {
	public static final int UNTIL_END=1000;
	public static final double epsilon=0.00000001;
	double []surfacePVC= {160*3.2,15*15,10*10,6*6};//Surface du tube, de la petite tige, de la moyenne tige, de l epaisse tige
	double []threshHighPVC= {surfacePVC[0]*2,(surfacePVC[0]+surfacePVC[1])/2+epsilon,(surfacePVC[1]+surfacePVC[2])/2+epsilon,(surfacePVC[2]+surfacePVC[3])/2+epsilon};
	double []threshLowPVC= {(surfacePVC[0]+surfacePVC[1])/2,(surfacePVC[1]+surfacePVC[2])/2,(surfacePVC[2]+surfacePVC[3])/2,surfacePVC[3]/2};
	public final double stdVSX=0.1; //in mm
	public final double stdVSY=0.1; //in mm
	public final double stdVSZ=5; //in mm
	private String healthStatus;
	private VineType vineType;
	private String sourceField;
	private String variety;
	private Integer nbSlices;
	private int nbSlicesLost;
	private int[] indexOfLostSlices;
	private int[][] depthIndications;
	private ItkTransform[]currentGlobalTransform;
	private double[] center=new double[] {174,89.3};
	private ImagePlus maskForAutoRegistration;
	private ItkTransform[] tabTransInit;
	private ImagePlus roughSegmentation;
	private int[] wrongAngles;
	public int[]missingAngles;
	private ImagePlus roughSegmentationHighRes;
	private boolean handleFlip=true;
	
	/**
	 * Constructors, factory and top level functions
	 */
	public Photo_Slicing_Seq(String sourcePath,Capillary cap,SupervisionLevel sup,String title,ComputingType computingType) {
		super(AcquisitionType.PHOTOGRAPH, sourcePath,cap,sup);
		this.computingType=computingType;
		this.title=title;
		this.hyperSize=4;//Gray, R, G, B
	}

	
	public void freeMemory() {
		this.sourceData=null;
		this.roughSegmentationHighRes=null;
		this. maskForAutoRegistration=null;
	}
	
	public static void main (String[]args) {
		ImageJ ij=new ImageJ();//
		System.out.println("Sequence de test de Photo_slicing");//,"
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};//,
//		String []specimen= {"CEP020_APO1"};//,
		for(String spec : specimen) {
			Photo_Slicing_Seq photo=new Photo_Slicing_Seq("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH",Capillary.HAS_NO_CAPILLARY,
					SupervisionLevel.AUTONOMOUS,spec,ComputingType.COMPUTE_ALL);
			photo.start(13);
			photo.freeMemory();
			System.gc();
		}
		System.out.println("FINI PHOTOS !");
		return;
	}
	

	public void start(int endStep) {
		this.printStartMessage();
		quickStartFromFile();//Build the initial stack
		while(nextStep(endStep));
	}
	
	public boolean nextStep(int endStep){
		int a=readStep();
		if(a>=endStep) {
			System.out.println("Step maximale atteinte : "+a);
			return false;
		}
		System.out.println("PHOTO_SLICING_SEQ "+this.getTitle()+"------> "+ " next step : "+a+" "+this.getTitle());
		switch(a) {
		case -1:
			return false;
		case 0: // rien --> exit
			if(this.computingType!=ComputingType.EVERYTHING_AFTER_MAPS) {
				if(this.supervisionLevel != SupervisionLevel.AUTONOMOUS)IJ.log("No data in this directory");
				return false;
			};break;			
		case 1: //data are read. Time to mirror recto and write data
			System.out.println(this.title+" step 1 : calcul Weka segmentation");
			this.computeRoughSegmentation();
			break;
		case 2: //data are registered. Time to compute Maps
			System.out.println(this.title+" step 2 : first alignment");
			firstRoughRegistrationV2();
			break;
		case 3: //Use the first transform to visualize result on higher res
			System.out.println(this.title+" step 3 : export high res data");
			extendRoughRegistrationToHighRes();
			break;
		case 4: //Segment wood
			System.out.println(this.title+" step 4 : wood area extraction");
			extractWood();
			break;
		case 5: //recover weird parts in segmentation
			System.out.println(this.title+" step 5 : clean wood");
			cleanWood();
			break;			
		case 6: //run poly matching between slices and RX
			System.out.println(this.title+" step 6 : equalize wood");
			this.equalizeWood();
			break;			
		case 7: //run poly matching between slices and RX
			System.out.println(this.title+" step 7 : manual RX registration");
			manualRXregistration();
			break;			
		case 8: //run poly matching between slices and RX
			System.out.println(this.title+" step 8 : automatic RX-slices registration");
			registerWithRX();
			break;			
		case 9:
			System.out.println(this.title+" step 9 : refine RX registration");
			refineRXregistration();
			break;
		case 10:
			System.out.println(this.title+" step 10 : manual RX-MRI registration");
			manualMRIregistration();
			break;
		case 11:
			System.out.println(this.title+" step 11 : automatic RX-MRI registration");
			automaticMRIregistration();
			break;
			
		case 12:
			System.out.println(this.title+" step 12 : compute high res data for expertise");			
			computeDataForExpertise();
			break;

		case 13:
			System.out.println(this.title+" step 12 : compute high res data for expertise");			
			extendRegistrationToHigherResolutions();
			break;
		
		case 20:
			if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) return false;
			System.out.println(this.title+" step 4 : high_res_combination and hyper image");
			this.computeNormalizedHyperImage();
			//writeHyperImage();
			System.out.println("MRI_T1_Seq Computation finished for "+this.getTitle());
			return false;
		}
		writeStep(a+1);	
		return true;
	}


	/**
	 * I/O writers/readers helpers functions
	 */
	@Override
	public void quickStartFromFile() {
		//Gather the path to source data
		System.out.println("Quick start : PHOTO sequence hosted in ");
		System.out.println(this.sourcePath.substring(0,this.sourcePath.length()/2));
		System.out.println(this.sourcePath.substring(this.sourcePath.length()/2+1));

		//Explore the path, look for STEPS_DONE.tag and DATA_PARAMETERS.tag
		File fStep=new File(this.sourcePath+slash+"STEPS_DONE.tag");
		File fParam=new File(this.sourcePath+slash+"DATA_PARAMETERS.tag");
		File pathlink = new File(this.sourcePath+slash+"DATA_PATH.tag");
		System.out.println("File touch "+(fParam.exists())+" "+pathlink.exists()+sourcePath);
		
		if(! fParam.exists() || ! pathlink.exists()) {
			IJ.log("No parameters found. There should be two files in the main directory : DATA_PARAMETERS.tag and DATA_PATH.tag. Build them, then run again");
			System.exit(0);
		}
		System.out.println("It's a match ! The tag files tells me that data have already been processed here");
		System.out.println("Start reading parameters");
		readParametersFromHardDisk();//Read parameters and path
		System.out.println("Start reading processed images");
		
		if(fStep.exists()) {
			System.out.println("Quickstart reads already processed images");
			readProcessedImages(readStep());
		}
		else {		
			//look for a directory Source_data
			System.out.println("No match with previous analysis ! Starting new analysis...");
			File dir = new File(this.sourcePath+slash+"Computed_data");
			boolean isCreated = dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Registration");dir.mkdirs();
			dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation");dir.mkdirs();
			dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms");
			dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks");
			dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2");
			dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2");
			dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage");dir.mkdirs();
			dir.mkdirs();

			writeStep(0);
			System.out.println("Reading source data...");
			readAndProcessSourceData();
			System.out.println("Writing stacks");
			writeStackedSourceData();
			writeStep(1);
		}
	}

	
	
	private void readProcessedImages(int readStep) {
		// TODO Auto-generated method stub
		if(readStep>0) {
			this.readStackedSourceData();
			this.setDimensions(this.sourceData[0].getWidth(), this.sourceData[0].getHeight(),this.sourceData[0].getStackSize());
			System.out.println("Starting from step "+readStep+" an experience with dimensions = "+TransformUtils.stringVector(VitimageUtils.getDimensions(this.sourceData[0]), "")+" with vox="+
					TransformUtils.stringVector(VitimageUtils.getVoxelSizes(this.sourceData[0]), ""));
		}
		if(readStep>=2) {
			this.roughSegmentation=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"roughSegmentation.tif");
			System.out.println("Read : segmentation of dimensions = "+TransformUtils.stringVector(VitimageUtils.getDimensions(this.roughSegmentation), ""));
		}
		if(readStep>=3)this.readCurrentGlobalTransforms(readStep);
	}

	private void readCurrentGlobalTransforms(int readStep) {
		if(readStep==3) {
			this.currentGlobalTransform=new ItkTransform[this.dimZ()];
			System.out.println("Chargement des transformations de la premiere etape");
			for(int i=0;i<this.dimZ();i++) {
				this.currentGlobalTransform[i]=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_1_"+i+".txt");
			}
		}
		if(readStep>=8) {
			System.out.println("Chargement des transformations de la deuxieme etape");
			for(int i=0;i<this.dimZ();i++) {
				//this.currentGlobalTransform[i]=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_"+i+".txt");
			}
		}
	}
	
	
	public void readParametersFromHardDisk() {
		File fParam=new File(this.sourcePath,"DATA_PARAMETERS.tag");
		String[]strFile=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
       } catch (IOException ex) {        ex.printStackTrace();   }
		for(int i=1;i<16 ; i++) {
			strFile[i]=strFile[i].split("=")[1];
		}
		this.sourcePath=strFile[1];
		this.dataPath=strFile[2];
		this.setTitle(strFile[4]);
		this.healthStatus=strFile[5];
		this.projectName=strFile[6];
		
		this.vineType=VitimageUtils.stringToVineType(strFile[7]);
		this.sourceField=strFile[8];
		this.variety=strFile[9];
		this.nbSlices=Integer.valueOf(strFile[13]);

		//Gestion des coupes disparues
		String lostSlicesIndexes=strFile[14];
		String []losli=lostSlicesIndexes.split(" ");
		this.nbSlicesLost=losli.length-1;
		if(this.nbSlicesLost>0) {
			this.indexOfLostSlices=new int[this.nbSlicesLost];
			System.out.println("Importation : gestion des slices disparues, au nombre de "+this.nbSlicesLost);
			for(int i=0;i<this.nbSlicesLost;i++) {
				this.indexOfLostSlices[i]=Integer.parseInt(losli[i+1]);
			}
		}
		
		this.depthIndications=new int[Integer.valueOf(strFile[15])][2];
		for(int i=0;i<this.depthIndications.length;i++) {
			String[] sT=strFile[16+i].split(" ");
			this.depthIndications[i][0]=Integer.parseInt(sT[0]);
			this.depthIndications[i][1]=Integer.parseInt(sT[1]);
		}

		double voxZ=0.5*(this.depthIndications[0][1]-this.depthIndications[this.depthIndications.length-1][1])*1.0/(this.depthIndications[this.depthIndications.length-1][0]-this.depthIndications[0][0]);
		System.out.println("Estimated overall voxel size="+voxZ);
		this.setVoxelSizes(new double[] {4*Double.parseDouble(strFile[10]),4*Double.parseDouble(strFile[11]),voxZ });

		
		
		
		int nWrong=Integer.valueOf(strFile[15+this.depthIndications.length+1].split("=")[1]);
		this.wrongAngles=new int[this.nbSlices*2];
		for(int i=0;i<nWrong;i++) {
			String[] sT=strFile[15+this.depthIndications.length+2+i].split(" ");
			int slice=Integer.parseInt(sT[0]);
			this.wrongAngles[slice-1]=Integer.parseInt(sT[1]);
			System.out.println("Lecture d'une indication d angle incorrect : slice="+(slice)+", angle Z to apply="+this.wrongAngles[slice-1]);
		}
		
		this.missingAngles=new int[this.nbSlices*2];
		System.out.println("En effet, dimZ="+dimZ());
		int nMissing=Integer.parseInt(strFile[15+this.depthIndications.length+2+nWrong].split("=")[1]);
		System.out.println("Read : "+nMissing+" missing angles");
		for(int n=0;n<this.nbSlices*2;n++)missingAngles[n]=-1;
		for(int n=0;n<nMissing;n++) {
			String[] sT=strFile[15+this.depthIndications.length+3+n+nWrong].split(" ");
			int slice=Integer.parseInt(sT[0]);
			this.missingAngles[slice-1]=Integer.parseInt(sT[1]);
			System.out.println("Lecture d'une indication d angle manquant (PVC tombé) : slice="+(slice)+", angle ="+this.missingAngles[slice-1]);
		}		
	}

	
	/**
	 * I/O images helper functions
	 */
	public void readAndProcessSourceData() {
		//Import des images
		File pathlink = new File(this.dataPath);
		File directoryOut = new File(this.sourcePath,"Computed_data");
		if(! pathlink.exists()) {IJ.showMessage("Source path not found : "+pathlink.getAbsolutePath());return;}
		sourceData=new ImagePlus[2];
		sourceData[0] = FolderOpener.open(pathlink.getAbsolutePath(), "");

		//Reduction taille de l'image
		IJ.run(this.sourceData[0], "Scale...", "x=0.25 y=0.25 z=1 width="+sourceData[0].getWidth()/4+" height="+sourceData[0].getHeight()/4+" depth="+this.dimZ()+" interpolation=Bilinear average process create");
		this.sourceData[0]=IJ.getImage();
		this.sourceData[0].hide();
		VitimageUtils.imageChecking(sourceData[0],"Image empilee apres reduction des donnees d'un facteur 4");		
		this.setDimensions(sourceData[0].getWidth(),sourceData[0].getHeight(), sourceData[0].getStackSize());
		System.out.println("Image lue et sous echantillonnee, constituée de "+sourceData[0].getStackSize()+" photos, en resolution  "+this.dimX()+" x "+this.dimY()+" correspondant à "+this.nbSlices);
		VitimageUtils.adjustImageCalibration(sourceData[0], new double[] {this.voxSX(),this.voxSY(),this.voxSZ()},"mm");

		//Faire miroir aux verso
		System.out.println("Mirror all recto along X axis");
		for(int i=0;i<this.dimZ()/2;i++) {
			int sli=2*i;
			this.sourceData[0].setSlice(sli+(handleFlip ? 2 : 1));
			IJ.run(this.sourceData[0], "Flip Vertically", "slice");
		}
		VitimageUtils.imageChecking(sourceData[0],"Image apres miroir des recto");
		//if(this.handleFlip)IJ.run(this.sourceData[0], "Flip Horizontally", "stack");

		//Faire la balance des couleurs
		sourceData[0]=VitimageUtils.balanceColors(sourceData[0],25);
		VitimageUtils.imageChecking(sourceData[0]);

		//Produire l'image vraiment sous-echantillonnée	
		System.out.println("Reduction d'un facteur 4");
		IJ.run(this.sourceData[0], "Scale...", "x=0.25 y=0.25 z=1 width="+sourceData[0].getWidth()/4+" height="+sourceData[0].getHeight()/4+" depth="+this.dimZ()+" interpolation=Bilinear average process create");
		this.sourceData[1]=IJ.getImage();
		this.sourceData[1].hide();
	}

	
	public void readStackedSourceData() {
		File f=new File(this.sourcePath, "Computed_data"+slash+"0_Stacks");
		String strPath=f.getAbsolutePath();
		sourceData=new ImagePlus[2];
		sourceData[0]=IJ.openImage(strPath+slash+"Stack_high_res.tif");
		sourceData[1]=IJ.openImage(strPath+slash+"Stack_low_res.tif");
		setImageForRegistration();	
	}
	
	public void writeStackedSourceData() {
		File dir = new File(this.sourcePath+slash+"Computed_data"+slash+"0_Stacks");
		boolean isCreated = dir.mkdirs();
		IJ.saveAsTiff(this.sourceData[0],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"Stack_high_res.tif");
		IJ.saveAsTiff(this.sourceData[1],this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"Stack_low_res.tif");
		setImageForRegistration();	
	}

	//Identify areas where non-consistent information lies, such as saw cutting marks, or cylinder shade on paper, or red marker line 
	public void computeMaskForAutoRegistration() {
		System.out.println("Extraction masque du fond, ombre et trait rouge");
		ImagePlus maskOutFond=VitimageUtils.makeWekaSegmentation(this.sourceData[1],"/home/fernandr/Bureau/Traitements/Cep5D/CEP019_S3_Martyr/classifier_fond_ombre_et_rouge.model");
		IJ.run(maskOutFond, "8-bit", "");
		IJ.run(maskOutFond, "Median...", "radius=2 stack");//Apres ça, result est le masque en positif de la région d'interêt
		IJ.saveAsTiff(maskOutFond,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"maskOutFond.tif");
		IJ.run(maskOutFond, "Divide...", "value=255.000");

		System.out.println("Extraction masque des traits de coupe laissés sur la mousse");
		ImagePlus maskOutTraces=VitimageUtils.makeWekaSegmentation(this.sourceData[1],"/home/fernandr/Bureau/Traitements/Cep5D/CEP019_S3_Martyr/classifier_traces_decoupe.model");
		IJ.run(maskOutTraces, "8-bit", "");
		IJ.saveAsTiff(maskOutTraces,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"maskOutTraces.tif");

		System.out.println("Extraction masque du bois");
		ImagePlus maskOutBois=VitimageUtils.makeWekaSegmentation(this.sourceData[1],"/home/fernandr/Bureau/Traitements/Cep5D/CEP019_S3_Martyr/classifier_bois.model");
		IJ.run(maskOutBois, "8-bit", "");
		IJ.run(maskOutBois, "Median...", "radius=2 stack");//Apres ça, result est le masque en positif de la région d'interêt
		IJ.saveAsTiff(maskOutBois,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"maskOutBois.tif");
		IJ.run(maskOutBois, "Divide...", "value=255.000");

		System.out.println("Composition des masques");
		this.maskForAutoRegistration = new ImageCalculator().run("Multiply create stack",maskOutFond, maskOutTraces);
		this.maskForAutoRegistration = new ImageCalculator().run("Multiply create stack",this.maskForAutoRegistration, maskOutBois);
		VitimageUtils.adjustImageCalibration(this.maskForAutoRegistration, this.sourceData[1]);
		IJ.run(this.maskForAutoRegistration, "Dilate", "stack");
		IJ.saveAsTiff(this.maskForAutoRegistration,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"maskForAutoRegistration.tif");		
	}
	
	
	public void readMaskForAutoRegistration() {
		this.maskForAutoRegistration=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"maskForAutoRegistration.tif");
	}
	

	/*----------------------- STEP 1 -----------------------------------------------*/
	public void computeRoughSegmentation() {
		this.roughSegmentation=VitimageUtils.makeWekaSegmentation(this.sourceData[1],"/home/fernandr/Bureau/Traitements/Cep5D/Weka_hybride/classifier_v2_structure.model");
		this.roughSegmentation.setDisplayRange(0, 255);
		IJ.run(this.roughSegmentation,"8-bit","");
		IJ.run(this.roughSegmentation,"Fire","");
		this.roughSegmentation.setDisplayRange(0, 4);
		
		IJ.saveAsTiff(this.roughSegmentation,this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"roughSegmentation.tif");
	}

	
	
	/*----------------------- STEP 2 -----------------------------------------------*/
	public ItkTransform[] detectSlicePoseFromPVC(ImagePlus seg,ImagePlus init,String pathToWriteResult) {
		System.out.println("VOX="+this.voxSX());
		ImagePlus imgSeg=new Duplicator().run(seg);
		System.out.println("SEG VOX="+TransformUtils.stringVector(VitimageUtils.getVoxelSizes(seg),""));
		System.out.println("INIT VOX="+TransformUtils.stringVector(VitimageUtils.getVoxelSizes(init),""));
		VitimageUtils.adjustImageCalibration(imgSeg, seg);
		ImagePlus imgInit=new Duplicator().run(init);
		VitimageUtils.adjustImageCalibration(imgInit, init);
		imgSeg=VitimageUtils.thresholdByteImage(imgSeg, 3,4);
		double[][]tubeCenters =new double[imgInit.getStackSize()][3];
		double[][]tinyCenters =new double[imgInit.getStackSize()][3];
		double[]tetas =new double[imgInit.getStackSize()];
		int radiusTube=110;
		int radiusTiny=11;
		int []dims=VitimageUtils.getDimensions(imgInit);
		double []voxS=VitimageUtils.getVoxelSizes(imgInit);
		double deltaX=0,deltaY=0,deltaTeta=0;
		double []imageCenter=new double[] {0.5*dims[0]*voxS[0],0.5*dims[1]*voxS[1],0};
		double tetaZero=0;
		int i=0;
		int zMax=imgSeg.getStackSize();
		ImagePlus[] segTab=new ImagePlus[zMax];
		ImagePlus[] initTab=new ImagePlus[zMax];
		ItkTransform[]trTab=new ItkTransform[zMax];
		ResultsTable rt=ResultsTable.getResultsTable();
		ImagePlus temp;

		///////////DETECTION TUBE		
		imgSeg.show();
		Hough_Circle hough=new Hough_Circle();
		hough.setParameters(radiusTube-2,radiusTube+2, 1, /*int radiusMin, int radiusMax, int radiusInc, */
				1,1,0.1,   /*int minCircles, int maxCircles, double thresholdRatio,*/
				5000,1,10,10, /*int resolution, double ratio, int searchBand, int searchRadius*/
				true,false,true,false,/*boolean reduce, boolean local, boolean houghSeries, boolean showCircles, */
				false,false,true,false /*boolean showID, boolean showScores, boolean results, boolean isGUI*/);	  
		hough.execute();
		while(rt.getCounter()<(zMax) ) {VitimageUtils.waitFor(20);i++; }
		VitimageUtils.waitFor(5000);
		temp=IJ.getImage();
		temp.changes=false;
		System.out.println("Titre0="+temp.getTitle());
		if(! temp.getTitle().contains("Hough")) {
			temp.close();
			temp=IJ.getImage();
			temp.changes=false;
		}
		IJ.saveAsTiff(temp,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"hough_tube.tif");
		temp.close();
		System.out.println("Detection des centres des tubes ok. Calcul effectué en "+(i*20)+" ms");
		for(int z=0;z<zMax;z++) {
			tubeCenters[z][0]=rt.getValue(1,z);
			tubeCenters[z][1]=rt.getValue(2,z);
			tubeCenters[z][2]=z;
			deltaX=tubeCenters[z][0]-imageCenter[0];
			deltaY=tubeCenters[z][1]-imageCenter[1];
			System.out.println("Z="+z+"   Tube : ( "+tubeCenters[z][0]+" , "+tubeCenters[z][1]+" )   ,   indicateur : ( "+tinyCenters[z][0]+" , "+tinyCenters[z][1]+" ).  Trans=["+deltaX+" , "+deltaY+"]");
			imgInit.setSlice(z+1);
			initTab[z] = imgInit.crop();
			trTab[z]=ItkTransform.getRigidTransform(tubeCenters[z], new double[] {0,0,0},new double[] {deltaX,deltaY,0});
			trTab[z].addTransform(ItkTransform.getRigidTransform(imageCenter, new double[] {0,0,(this.handleFlip ? -1.0 : 1.0)*wrongAngles[z]/180.0*Math.PI},new double[] {0,0,0}));
			initTab[z]=trTab[z].transformImage(initTab[z],initTab[z],false);
		}
		ImagePlus result=Concatenator.run(initTab);
		VitimageUtils.imageChecking(result,"Apres reduction de la translation");
		if(pathToWriteResult!=null)IJ.saveAsTiff(result,pathToWriteResult+".partialTransform.tif");

		
		///////////DETECTION INDICATEUR PVC		
		//Preparation image pour tiny
		ImagePlus imgTiny=new Duplicator().run(result);
		VitimageUtils.adjustImageCalibration(imgTiny,imgInit);
		IJ.run(imgTiny, "8-bit", "");
		IJ.run(imgTiny, "Find Edges", "stack");
		imgTiny=VitimageUtils.thresholdByteImage(imgTiny,150,256);
		
		
		///DETECTION PREMIER POINT
		imgTiny.setSlice(1);
		ImagePlus imgTemp=imgTiny.crop();
		imgTemp.show();
		Hough_Circle houghINT=new Hough_Circle();
		houghINT.setParameters(radiusTiny,radiusTiny, 1, /*int radiusMin, int radiusMax, int radiusInc, */
					1,1,0.1,   /*int minCircles, int maxCircles, double thresholdRatio,*/
					5000,1,10,10, /*int resolution, double ratio, int searchBand, int searchRadius*/
					true,false,true,false,/*boolean reduce, boolean local, boolean houghSeries, boolean showCircles, */
					false,false,true,false /*boolean showID, boolean showScores, boolean results, boolean isGUI*/);	  
		houghINT.execute();
		i=0;
		while(rt.getCounter()<(zMax+1)) {VitimageUtils.waitFor(20);	i++; }
		tinyCenters[0][0]=rt.getValue(1,zMax);
		tinyCenters[0][1]=rt.getValue(2,zMax);
		tinyCenters[0][2]=0;
		ImagePlus imgBlack=new Duplicator().run(imgTiny);
		VitimageUtils.adjustImageCalibration(imgBlack, imgTiny);
		imgBlack=VitimageUtils.set8bitToValue(imgBlack, 0);
		System.out.println("Calcul cercles.");
		System.out.println("XC="+(int)Math.round(imageCenter[0]/voxS[0]));
		System.out.println("YC="+(int)Math.round(imageCenter[0]/voxS[0]));
		ImagePlus maskNeighbour=VitimageUtils.drawCylinderInImage(imgBlack, 40*voxS[0], (int)Math.round(tinyCenters[0][0]/voxS[0]), (int)Math.round(tinyCenters[0][1]/voxS[1]),1);		
		ImagePlus maskGrandCircle=VitimageUtils.drawCylinderInImage(imgBlack, radiusTube*1.03*voxS[0], (int)Math.round(imageCenter[0]/voxS[0]), (int)Math.round(imageCenter[1]/voxS[1]),1);
		ImagePlus maskLittleCircle=VitimageUtils.drawCylinderInImage(imgBlack, radiusTube*0.63*voxS[0], (int)Math.round(imageCenter[0]/voxS[0]), (int)Math.round(imageCenter[1]/voxS[1]),1);
		ImagePlus maskBand = new ImageCalculator().run("Subtract create stack", maskGrandCircle,maskLittleCircle);
		ImagePlus maskArea = new ImageCalculator().run("Multiply create stack", maskBand,maskNeighbour);
		ImagePlus imgTiny2 = new ImageCalculator().run("Multiply create stack", maskArea,imgTiny);
		VitimageUtils.adjustImageCalibration(imgTiny2, imgTiny);
		VitimageUtils.imageChecking(imgTiny2,"Angle indicator over Z");
		imgTiny2.setTitle("tiny");
		imgTiny2.show();
		VitimageUtils.waitFor(100);
		IJ.saveAsTiff(maskArea,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"maskArea.tif");
		IJ.saveAsTiff(imgTiny,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"gradArea.tif");
		System.out.println("Let s go");
		IJ.selectWindow("tiny");
		
		Hough_Circle hough2=new Hough_Circle();
		hough2.setParameters(radiusTiny,radiusTiny, 1, /*int radiusMin, int radiusMax, int radiusInc, */
					1,1,0.1,   /*int minCircles, int maxCircles, double thresholdRatio,*/
					5000,1,10,10, /*int resolution, double ratio, int searchBand, int searchRadius*/
					true,false,true,false,/*boolean reduce, boolean local, boolean houghSeries, boolean showCircles, */
					false,false,true,false /*boolean showID, boolean showScores, boolean results, boolean isGUI*/);	  
		hough2.execute();
		i=0;
		while(rt.getCounter()<(2*zMax+1)) {VitimageUtils.waitFor(20);	i++; }
		System.out.println("Detection des centres des indicateurs ok. Calcul effectué en "+(i*20)+" ms");
		VitimageUtils.waitFor(5000);
		temp=IJ.getImage();
		temp.changes=false;
		System.out.println("Titre0="+temp.getTitle());
		if(! temp.getTitle().contains("Hough")) {
			temp.close();
			temp=IJ.getImage();
			temp.changes=false;
		}
		IJ.saveAsTiff(temp,this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"hough_indicateur.tif");
		temp.close();


		for(int z=0;z<zMax;z++) {
			tinyCenters[z][0]=rt.getValue(1,zMax+z+1);
			tinyCenters[z][1]=rt.getValue(2,zMax+z+1);
			tinyCenters[z][2]=z;
			if(z==0) {
				tetaZero=TransformUtils.calculateAngle(tinyCenters[z][0]-imageCenter[0],tinyCenters[z][1]-imageCenter[1]);
				System.out.println("Angle tetaZero="+tetaZero);
			}
			if(missingAngles[z]==-1)deltaTeta=TransformUtils.diffTeta(TransformUtils.calculateAngle(tinyCenters[z][0]-imageCenter[0],tinyCenters[z][1]-imageCenter[1]),tetaZero);
			else {
				double ttemp=missingAngles[z];
				if(this.handleFlip) {
					ttemp=360-ttemp;
				}
				deltaTeta=TransformUtils.diffTeta(ttemp,tetaZero);
				System.out.println("A la slice z="+z+" correction effectuee sur la base d un angle fourni par l experimentateur. Dteta calculé="+deltaTeta );
			}
			System.out.println("Z="+z+"/"+zMax+"   Tube : ( "+imageCenter[0]+" , "+imageCenter[1]+" )   ,   indicateur : ( "+tinyCenters[z][0]+" , "+tinyCenters[z][1]+" ).  Dteta="+deltaTeta);
			imgInit.setSlice(z+1);
			initTab[z] = imgInit.crop();
			trTab[z].addTransform(ItkTransform.getRigidTransform(imageCenter, new double[] {0,0,deltaTeta/180.0*Math.PI},new double[] {0,0,0}));
			initTab[z]=trTab[z].transformImage(initTab[z],initTab[z],false);
		}	
		imgTiny.hide();
		result=Concatenator.run(initTab);
		VitimageUtils.imageChecking(result);
		if(pathToWriteResult!=null)IJ.saveAsTiff(result,pathToWriteResult);
		imgSeg.changes=false;
		imgSeg.close();
		
		while(rt.getCounter()>0)rt.deleteRow(0);
		int n=WindowManager.getImageCount();
		int []ids=WindowManager.getIDList();
		for(int im=n-1;im>=0;im--)WindowManager.getImage(ids[im]).close();
		this.readProcessedImages(2);
		return trTab;
	}

	public void firstRoughRegistrationV2() { 	
		this.currentGlobalTransform=detectSlicePoseFromPVC(this.roughSegmentation,this.sourceData[1],this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"img_low_after_step_1.tif");
		for(int i=0;i<this.dimZ();i++) this.currentGlobalTransform[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_1_"+i+".txt");
	}
	
	
	/*----------------------- STEP 3 -----------------------------------------------*/	
	public void extendRoughRegistrationToHighRes() {
//		IJ.run(this.roughSegmentation, "Scale...", "x=4 y=4 z=1 width="+sourceData[0].getWidth()+" height="+sourceData[0].getHeight()+" depth="+this.dimZ()+" interpolation=None average process create");
//		this.roughSegmentationHighRes=IJ.getImage();
//		this.roughSegmentationHighRes.hide();
		int zMax=this.roughSegmentation.getStackSize();
		ImagePlus []segTab=new ImagePlus[zMax];
		ImagePlus []segHighTab=new ImagePlus[zMax];
		ImagePlus []imgTab=new ImagePlus[zMax];
		for(int z=0;z<zMax;z++) {
			System.out.println("Processing slice "+z);
			this.roughSegmentation.setSlice(z+1);
			segTab[z]=this.roughSegmentation.crop();
			this.sourceData[0].setSlice(z+1);
			imgTab[z]=this.sourceData[0].crop();
			
			segTab[z]=this.currentGlobalTransform[z].transformImageSegmentationByte(segTab[z], segTab[z],0,4);
			segHighTab[z]=this.currentGlobalTransform[z].transformImageSegmentationByte(imgTab[z], segTab[z],0,4);
			//imgTab[z]=this.currentGlobalTransform[z].transformImage(imgTab[z], imgTab[z]);
		}
		ImagePlus resultSeg=Concatenator.run(segTab);
		resultSeg.setDisplayRange(0,255);
		IJ.run(resultSeg,"8-bit","");
		IJ.run(resultSeg,"Fire","");
		resultSeg.setDisplayRange(0,4);

		ImagePlus resultHighSeg=Concatenator.run(segHighTab);
		resultHighSeg.setDisplayRange(0,255);
		IJ.run(resultHighSeg,"8-bit","");
		IJ.run(resultHighSeg,"Fire","");
		resultHighSeg.setDisplayRange(0,4);
		
		//ImagePlus resultImg=Concatenator.run(imgTab);

		IJ.saveAsTiff(resultSeg, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"rough_segmentation_low_res_after_step_1.tif");
		IJ.saveAsTiff(resultHighSeg, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"rough_segmentation_high_res_after_step_1.tif");
		//IJ.saveAsTiff(resultImg, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"img_high_res_after_step_1.tif");
	}
	

	
	
	/*----------------------- STEP 4 -----------------------------------------------*/
	public void extractWood() {
		//get low res registered seg
		ImagePlus imgLow=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"img_low_after_step_1.tif");
		ImagePlus imgSeg=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"rough_segmentation_low_res_after_step_1.tif");
		//most represented 1 and 2 then save
		ImagePlus segFiltered=VitimageUtils.mostRepresentedFilteringWithRadius(imgSeg,4,true,6,true);
		IJ.saveAsTiff(segFiltered, this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"seg_low_radius_4.tif");

		ImagePlus segWood=VitimageUtils.thresholdByteImage(segFiltered, 2,3);
		IJ.saveAsTiff(segWood, this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"seg_low_wood_radius_4.tif");
		
		ImagePlus woodOnlyRGB=VitimageUtils.maskRGB(imgLow, segWood);
		IJ.saveAsTiff(woodOnlyRGB, this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"wood_only_rgb_radius_4.tif");
		//binary then save
		//Fill holes then save	
		
	}
	


	/*----------------------- STEP 5 -----------------------------------------------*/
	public void cleanWood() {
		ImagePlus segWood=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"seg_low_wood_radius_4.tif");
		ImagePlus imgLow=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"img_low_after_step_1.tif");
	 // create structuring element (cube of radius 'radius')
		Strel3D str=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(2,2, 1);
		ImagePlus img =new ImagePlus("",Morphology.closing(segWood.getImageStack(),str));
		str=inra.ijpb.morphology.strel.SquareStrel.fromDiameter(3);
		img =new ImagePlus("",Morphology.erosion(img.getImageStack(),str));
		IJ.saveAsTiff(img, this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"seg_low_wood_radius_4_pruned.tif");
		ImagePlus woodOnlyRGB=VitimageUtils.maskRGB(imgLow, img);
		IJ.saveAsTiff(woodOnlyRGB, this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"wood_only_rgb_radius_4_pruned.tif");
	}
	

	
	/*----------------------- STEP 6 -----------------------------------------------*/
	public void equalizeWood() {
		ImagePlus woodOnlyRGB=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"wood_only_rgb_radius_4_pruned.tif");
		ImagePlus equalizedRGB=VitimageUtils.normalizationSliceRGB(woodOnlyRGB,6,6,1,1,false);
		IJ.saveAsTiff(equalizedRGB, this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"wood_only_rgb_equalized.tif");		
	}


	/*----------------------- STEP 7 -----------------------------------------------*/
	public void manualRXregistration() {
		ImagePlus imgMov=IJ.openImage(this.sourcePath+slash+".."+slash+"RX"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		ImagePlus imgRef=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"wood_only_rgb_equalized.tif");
		ItkTransform firstRXtransform=VitimageUtils.manualRegistrationIn3D(imgRef,imgMov);
		firstRXtransform.writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_MAN.txt");
	}
	
	
	
	/*----------------------- STEP 8 -----------------------------------------------*/
	public ImagePlus []generateThinnedImage(ImagePlus[]slices,ItkTransform[]trSlices,int[]depthSlices) {
		int []dims=VitimageUtils.getDimensions(slices[0]);
		double[]voxs=VitimageUtils.getVoxelSizes(slices[0]);
		double[]voxsThin=new double[] {voxs[0],voxs[1],1};
		int valTmp;
		int X=dims[0];int Y=dims[1];int Z=slices.length;int ZT=depthSlices[Z-1]+1;
		System.out.println("Generating thin stacks... Generating a stack of "+ZT+" slices");
		ImagePlus []tabRet=new ImagePlus[ZT];
		ImagePlus []tabRetThick=new ImagePlus[Z];
		ImagePlus imgZero=VitimageUtils.makeOperationOnOneImage(slices[0],VitimageUtils.OP_MULT, 0, true);
		for(int z=0;z<ZT;z++)tabRet[z]=imgZero;
		for(int z=0;z<Z;z++) {
			if(z%10==0)System.out.print(" z="+z+"/"+Z);
			valTmp=depthSlices[z];
			if(valTmp>=ZT)valTmp=ZT-1;
			if(valTmp<0)valTmp=0;
			tabRet[valTmp]=trSlices[z].transformImage(slices[z],slices[z],false);
			tabRetThick[z]=trSlices[z].transformImage(slices[z],slices[z],false);
		}
		ImagePlus ret=Concatenator.run(tabRet);
		VitimageUtils.adjustImageCalibration(ret,voxsThin,"mm");		
		ImagePlus ret2=new Duplicator().run(ret);
		VitimageUtils.adjustImageCalibration(ret2, ret);
		IJ.run(ret2,"8-bit","");
		ret2.setDisplayRange(0, 255);

		ImagePlus ret3=Concatenator.run(tabRetThick);
		VitimageUtils.adjustImageCalibration(ret3,voxs,"mm");
		
		System.out.println(" Ok.");
		return new ImagePlus[] {ret,ret2,ret3};
	}
	
	
	public ImagePlus produceDebugCompositeImage(ImagePlus imgRX,ImagePlus imgPhoto,int []depthSlices,double[]voxThick,int iteration) {
		System.out.println("Starting composite fusion for iteration "+iteration);
		int Z=depthSlices.length;
		ImagePlus[]tabSliceRX=new ImagePlus[Z];
		ImagePlus[]tabSlicePhoto=new ImagePlus[Z];
		ImagePlus[]tabHyper=new ImagePlus[3];
		ImagePlus[]slicesRX=VitimageUtils.getImagePlusStackAsImagePlusTab(imgRX);
		ImagePlus[]slicesPhoto=VitimageUtils.getImagePlusStackAsImagePlusTab(imgPhoto);
		for(int z=0;z<Z;z++) {
			tabSliceRX[z]=slicesRX[depthSlices[z]];
			tabSlicePhoto[z]=slicesPhoto[depthSlices[z]];
		}
		ImagePlus fuseRX=Concatenator.run(tabSliceRX);
		ImagePlus fusePH=Concatenator.run(tabSlicePhoto);
		ImagePlus nullImage=VitimageUtils.setImageToValue(fuseRX.duplicate(),0);
		tabHyper[1]=VitimageUtils.compositeOf(fuseRX,fusePH);
		tabHyper[0]=VitimageUtils.compositeOf(fuseRX,nullImage);
		tabHyper[2]=VitimageUtils.compositeOf(nullImage,fusePH);
		ImagePlus imgFus=Concatenator.run(tabHyper);
		VitimageUtils.adjustImageCalibration(imgFus, voxThick,"mm");
		imgFus.setTitle("Fusion a l iteration "+iteration);
		return imgFus;
	}


	public int[] computeDepthApproximation() {
		int nSlices=this.sourceData[1].getStackSize()/2;
		int nDepthDef=this.depthIndications.length;
		int []depthUp=new int[nSlices];
		int []depthDown=new int[nSlices];
		int []depthReal=new int[nSlices*2];
		int[]sliDef=new int[nDepthDef];
		int[]depthDef=new int[nDepthDef];
		for(int i=0;i<nDepthDef;i++) {sliDef[i]=this.depthIndications[i][0];depthDef[i]=this.depthIndications[i][1];}
		double thickMoy=(depthDef[0]-depthDef[nDepthDef-1])*1.0/(sliDef[nDepthDef-1]-sliDef[0]);

		for(int pt=0;pt<nDepthDef-1;pt++) {
			int curSlice=sliDef[pt]-1;
			int curDepth=depthDef[pt];
			int nextSlice=sliDef[pt+1]-1;
			int nextDepth=depthDef[pt+1];
			double thickBetween=(curDepth-nextDepth)*1.0/(nextSlice-curSlice);
			for(int sl=curSlice;sl<nextSlice;sl++) {
				depthUp[sl]=(int)Math.round(  curDepth- thickBetween*(sl-curSlice));
			}
		}
		depthUp[nSlices-1]=depthDef[nDepthDef-1];
		for(int pt=1;pt<nSlices;pt++)depthDown[pt-1]=depthUp[pt]+1;
		depthDown[nSlices-1]=depthUp[nSlices-1]-(int)Math.round(thickMoy);			

	
		for(int i=0;i<nSlices;i++) {
			depthReal[2*i]=depthUp[i];
			depthReal[2*i+1]=depthDown[i];
		}
		for(int i=1;i<nSlices*2;i++) {
			depthReal[i]=depthReal[0]-depthReal[i];
		}
		depthReal[0]=0;
		return depthReal;
	}
		
	public void registerWithRX() {
		//Parameters handling :
		ImagePlus imgRX=IJ.openImage(this.sourcePath+slash+".."+slash+"RX"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		ItkTransform transRX=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_MAN.txt");
		int []dims=VitimageUtils.getDimensions(sourceData[1]);
		double[]voxsThick=VitimageUtils.getVoxelSizes(sourceData[1]);
		int X=dims[0];int Y=dims[1];int Z=dims[2];
		ImagePlus []tabSlices=new ImagePlus[Z];
		ImagePlus[]tabSlicesRX;
		ItkTransform[]tabTrans=new ItkTransform[Z];
		int nbIterMax=3;
		int amplitudeZ=0;
		int[]depthSlicesInit=computeDepthApproximation();
		int[]depthSlices=computeDepthApproximation();
		ImagePlus img=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"wood_only_rgb_equalized.tif");
		System.out.println("Construction du tableau de slices et initialisation transformations");
		for(int z=0;z<Z;z++) { img.setSlice(z+1);   tabSlices[z] =  img.crop(); VitimageUtils.adjustImageCalibration(tabSlices[z],voxsThick,"mm");tabTrans[z]=new ItkTransform();}
		double [][][]scores=new double[nbIterMax][Z][3];
		
	
		
		//BOUCLE PRINCIPALE
		boolean testOnlySlices=false;
		for(int it=0;it<nbIterMax;it++) {
			System.out.println("\n\n ITERATION "+it);
			
			//Preparer les piles thin
			ImagePlus []imgsSliceTab=generateThinnedImage(tabSlices,tabTrans,depthSlices); 
			IJ.saveAsTiff(imgsSliceTab[0], this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_SL_0_start_iter_"+it+".tif");
			IJ.saveAsTiff(imgsSliceTab[1], this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_SL_1_start_iter_"+it+".tif");
			IJ.saveAsTiff(imgsSliceTab[2], this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_SL_thick_start_iter_"+it+".tif");
			ImagePlus imgSlice=imgsSliceTab[1];
			int ZT=imgSlice.getStackSize();
			tabSlicesRX=new ImagePlus[ZT];
			VitimageUtils.adjustImageCalibration(imgRX, new double[] {0.6,0.6,0.6},"mm");
			System.exit(0);
			
			//Recaler la pile RX, dans le domaine thin et produire l'image debug composite
			if(false ){
				transRX=BlockMatchingRegistration.registerRXOnSlices(imgSlice,imgRX,transRX,93,false);
				transRX.writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_iter_"+it+".txt");
				transRX.writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_last.txt");
			}
			else transRX=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_iter_0.txt");
			ImagePlus res=transRX.transformImage(imgsSliceTab[2], imgRX,false);
			res.setDisplayRange(0,255);
			IJ.run(res,"8-bit","");
			IJ.saveAsTiff(res, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_RX_thick_start_iter_"+it+".tif");

			res=transRX.transformImage(imgSlice, imgRX,false);
			res.setDisplayRange(0,255);
			IJ.run(res,"8-bit","");
			IJ.saveAsTiff(res, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_RX_thin_start_iter_"+it+".tif");
			ImagePlus resViewComposite=produceDebugCompositeImage(res,imgsSliceTab[1],depthSlices,voxsThick,it);
			resViewComposite.setTitle("Composite beginning iteration "+it);
			IJ.saveAsTiff(resViewComposite,  this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"composite_start_iter_"+it+".tif");
			
			
			//Recaler les slices photo
			for(int z=0;z<ZT;z++) {res.setSlice(z+1);tabSlicesRX[z]=res.crop();}
			for(int z=0;z<Z;z++) { 
				System.out.println("ITER "+it+" traitement slice "+z+"/"+Z+" , situee a la profondeur "+depthSlices[z]);
				ImagePlus imgMov=new Duplicator().run(tabSlices[z]);
				VitimageUtils.adjustImageCalibration(imgMov,tabSlices[z]);
				IJ.run(imgMov,"8-bit","");
				int zRefMin=Math.max(0,depthSlices[z]-amplitudeZ);
				int zRefMax=Math.min(ZT-1,depthSlices[z]+amplitudeZ);
				double[]result=new double[zRefMax-zRefMin+1];
				ItkTransform[]resTrans=new ItkTransform[zRefMax-zRefMin+1];
				for(int zRef=zRefMin;zRef<=zRefMax;zRef++){
					System.out.println("Recalage slice "+z+"/"+Z+" sur RX "+zRef+" entre "+zRefMin+" et "+zRefMax );
					ImagePlus imgRef=tabSlicesRX[zRef];
					imgRef.setDisplayRange(0, 255);
					IJ.run(imgRef,"8-bit","");
					Object[]ret=BlockMatchingRegistration.testSetupAndRunStandardBlockMatchingWithoutFineParameterizationPhoto2D(
							imgRef,imgMov,null,tabTrans[z],false,false,"|Sl "+z+" -> Rx "+zRef+"|");
					
					if(Double.isNaN((Double)ret[1])){result[zRef-zRefMin]=0;resTrans[zRef-zRefMin]=tabTrans[z];}
					else {result[zRef-zRefMin]=(Double)ret[1];resTrans[zRef-zRefMin]=(ItkTransform)ret[0];}
				}
				for(int i=0;i<2;i++)System.out.println("");
				System.out.println("Slice "+z+"/"+Z+" , prof= "+depthSlices[z]);
				double max=-10;int itMax=0;
				result[depthSlices[z]-zRefMin]+=0.0000000001;
				for(int i=0;i<result.length;i++) {System.out.println("  i="+i+" prof="+(zRefMin+i)+"  res="+result[i]);if(result[i]>max) {max=result[i];itMax=i;}}
				System.out.println();
				System.out.println("Passe de "+depthSlices[z]+" a "+(zRefMin+itMax));
				System.out.println();
				System.out.println();
				scores[it][z][0]=zRefMin+itMax;
				scores[it][z][1]=max;
				scores[it][z][2]=(int)(Math.round(zRefMin+itMax-depthSlicesInit[z]));
				depthSlices[z]=zRefMin+itMax;
				tabTrans[z]=resTrans[itMax];
				tabTrans[z].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"slice_"+z+"_iter_"+it+".txt");
				for(int i=0;i<3;i++)System.out.println("###########################################");
			}	
			res.hide();

			
			//Generer les informations de debug
			String recap="Recap\n";
			double []means=new double[it+1];
			for(int z=0;z<Z;z++) {
				recap+="Slice "+z+"  ";
				recap+="||||it=-1|depth="+depthSlicesInit[z]+" "+" (D=0.0)|score=-1.0";
				for(int itt=0;itt<=it;itt++) {
					recap+="||||it="+itt+"|depth="+scores[itt][z][0]+" "+" (D="+scores[itt][z][2]+")|score="+scores[itt][z][1];
					means[itt]+=scores[itt][z][1];
				}
				recap+="\n";
			}
			recap+="Score moy = ";
			for(int itt=0;itt<=it;itt++)recap+="||it="+itt+"|||mean="+(means[itt]/Z);
			
			System.out.println(recap);
			PrintWriter out;
			try {
				out = new PrintWriter(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"RECAP_iter_"+it);
				out.println(recap);
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println(recap);
			VitimageUtils.waitFor(10000);
			
		}
	}
	
	
	
	/*----------------------- STEP 9 -----------------------------------------------*/
	public void refineRXregistration() {
		int iterRef=1;
		int []dims=VitimageUtils.getDimensions(sourceData[1]);int Z=dims[2];
		double[]voxsThick=VitimageUtils.getVoxelSizes(sourceData[1]);
		ImagePlus imgRX=IJ.openImage(this.sourcePath+slash+".."+slash+"RX"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		ImagePlus img=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"wood_only_rgb_equalized.tif");
		ImagePlus []tabSlices=new ImagePlus[Z];
		int[]depthSlices=computeDepthApproximation();
		ItkTransform[]tabTrans=new ItkTransform[Z];
		for(int z=0;z<Z;z++) {
			tabTrans[z]=ItkTransform.readTransformFromFile(
					this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"slice_"+z+"_iter_"+iterRef+".txt");
			img.setSlice(z+1);   tabSlices[z] =  img.crop(); VitimageUtils.adjustImageCalibration(tabSlices[z],voxsThick,"mm");
		}
		
		ImagePlus imgSlice=generateThinnedImage(tabSlices,tabTrans,depthSlices)[1]; 
		ItkTransform transRX=ItkTransform.readTransformFromFile(
				this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_iter_"+iterRef+".txt");
		transRX=BlockMatchingRegistration.registerRXOnSlices(imgSlice,imgRX,transRX,93,false);
		transRX.writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_last.txt");
	}
	


	/*----------------------- STEP 10 -----------------------------------------------*/
	public void manualMRIregistration() {

		//Build reference RX image
		//This will have its "MRI shape"
		ImagePlus imgRX=IJ.openImage(this.sourcePath+slash+".."+slash+"RX"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		ItkTransform transRX=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_iter_1.txt");
		ImagePlus rxModel=VitimageUtils.switchAxis(imgRX, 2);
		System.out.println(this.title);
		rxModel=VitimageUtils.uncropImageByte(rxModel, 0, 0, 0, rxModel.getWidth()+200+((this.title.equals("CEP016_RES3"))?100 : 0), rxModel.getHeight(),rxModel.getStackSize());
		ImagePlus imgRef=transRX.transformImage(rxModel, imgRX,false);
		imgRef.setDisplayRange(0,255);
		IJ.run(imgRef,"8-bit","");
		IJ.saveAsTiff(imgRef, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"imgRef_RXForMRItif.tif");


		
		ImagePlus imgMov=IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_0.tif");
		
		//ItkTransform firstMRItransform=VitimageUtils.manualRegistrationIn3D(imgRef,imgMov);
		//firstMRItransform.writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_MAN.txt");
		//ItkTransform firstMRItransform=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_MAN.txt");
		ItkTransform transMRI=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_MAN.txt");
		ImagePlus res=transMRI.transformImage(imgRef, imgMov,false);
		IJ.saveAsTiff(res, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"imgMov_MRI_registeredMan.tif");
	}
	
	
	/*----------------------- STEP 11 -----------------------------------------------*/
	public void automaticMRIregistration() {
		//PHASE 1 : Recalage rigide
		//Build reference RX image
		//This will have its "MRI shape"
		boolean doRigid=false;
		ImagePlus imgRef=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"imgRef_RXForMRItif.tif");
		ItkTransform transMRI=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_MAN.txt");
		ImagePlus imgMov=IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_0.tif");
		ImagePlus imgMov2=VitimageUtils.makeOperationBetweenTwoImages(
				IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_0.tif"),
				IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_1.tif"),VitimageUtils.OP_MEAN,false);
		ImagePlus imgCir=VitimageUtils.makeOperationOnOneImage(imgRef, VitimageUtils.OP_MULT, 0, true);
		int[]dims=VitimageUtils.getDimensions(imgCir);
		imgCir=VitimageUtils.drawCircleInImage(imgCir, 125, dims[0]/2, dims[1]/2, dims[2]/2);
		imgCir=VitimageUtils.thresholdByteImage(imgCir, 100,256);
		ImagePlus res=null;
		if(doRigid) {
		transMRI=BlockMatchingRegistration.registerMRIonRX(imgRef,imgMov2,transMRI,180,imgCir);
		transMRI.writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_AUTO.txt");
		}
		else	transMRI=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_AUTO.txt");
		res=transMRI.transformImage(imgRef, imgMov,false);
		res.setDisplayRange(0, 255);
		IJ.run(res,"8-bit","");
		if(doRigid) IJ.saveAsTiff(res, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"imgMov_MRI_registeredAuto.tif");


		//Phase 2 : Recalage dense
		
		//Amener IRM dans geometrie RX, en passant par field
		//--> vers maching O field O from machine O to RX 
		ItkTransform field=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/champ_0_SIG10.tif");
		ItkTransform transMRIToRXWithField=new ItkTransform((ItkTransform.readTransformFromFile(
				this.sourcePath+slash+".."+slash+"MRI_CLINICAL/Computed_data/1_Transformations/transformation_0.txt")).getInverse());
		transMRIToRXWithField.addTransform(ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,-175,0,1,0,-175,0,0,1,-153.6}));
		transMRIToRXWithField.addTransform(field);
		transMRIToRXWithField.addTransform(ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,-175,0,1,0,-175,0,0,1,-153.6} ).getInverse( ));
		transMRIToRXWithField.addTransform(new ItkTransform(ItkTransform.readTransformFromFile(
				this.sourcePath+slash+".."+slash+"MRI_CLINICAL/Computed_data/1_Transformations/transformation_0.txt")));
		transMRIToRXWithField.addTransform(transMRI);

	
		transMRIToRXWithField=registerMRandRXFinal(imgRef,imgMov,transMRIToRXWithField);
		transMRIToRXWithField=transMRIToRXWithField.flattenDenseField(VitimageUtils.Sub222(imgRef));
		transMRIToRXWithField.writeAsDenseField(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_AUTO_DENSE_MERGE.tif",VitimageUtils.Sub222(imgRef));
		res=transMRIToRXWithField.transformImage(imgRef, imgMov,false);
		res.setDisplayRange(0, 255);
		IJ.run(res,"8-bit","");
		IJ.saveAsTiff(res, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"imgMov_MRI_registeredAutoDenseMerge.tif");

	}
	
	
	
	
	/*----------------------- STEP 12 -----------------------------------------------*/
	public void computeDataForExpertise() {
		boolean doThick=true;
		boolean doThin=true;
		System.out.println("Parameters loading");
		//Donnees necessaires : 
		//Img slice final low res
		//Img seg low res IJ.saveAsTiff(segWood, this.sourcePath+slash+"Computed_data"+slash+"2_Segmentation"+slash+"seg_low_wood_radius_4.tif");

		//##########################################################################""
		//A) Construire hyperimage niveaux de gris, photo-RX-T1-T2-M0-Diff
		//##########################################################################""
		int iterRef=0;
		int []dimsThick=VitimageUtils.getDimensions(sourceData[1]);int Z=dimsThick[2];
		double[]voxsThick=VitimageUtils.getVoxelSizes(sourceData[1]);
		int[]depthSlices=computeDepthApproximation();
		VitimageUtils.writeIntArray1DInFile(depthSlices, this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"depthSlicesTab.txt");
		ImagePlus []regsThick=new ImagePlus[9];
		ImagePlus []regsThin=new ImagePlus[9];
		ImagePlus [][]slicesThick=new ImagePlus[9][Z];
		ImagePlus imgPhotoThin=IJ.openImage(
				this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_SL_0_start_iter_"+iterRef+".tif");
		regsThin[0]=IJ.openImage(
				this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_SL_0_start_iter_"+iterRef+".tif");
		ImagePlus imgPhotoThick=IJ.openImage(
				this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_SL_thick_start_iter_"+iterRef+".tif");
		regsThick[0]=IJ.openImage(
				this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks2"+slash+"img_SL_thick_start_iter_"+iterRef+".tif");
		ImagePlus []tempTab=VitimageUtils.decomposeRGBImage(regsThin[0]);
		regsThin[6]=tempTab[0];
		regsThin[7]=tempTab[1];
		regsThin[8]=tempTab[2];
		tempTab=VitimageUtils.decomposeRGBImage(regsThick[0]);
		regsThick[6]=tempTab[0];
		regsThick[7]=tempTab[1];
		regsThick[8]=tempTab[2];
		IJ.run(regsThin[0],"8-bit","");
		IJ.run(regsThick[0],"8-bit","");
		double[]voxsThin=VitimageUtils.getVoxelSizes(imgPhotoThin);
		int []dimsThin=VitimageUtils.getDimensions(imgPhotoThin);int Zthin=dimsThin[2];
		ImagePlus [][]slicesThin=new ImagePlus[9][Zthin];

		
		//     Lire RX dans geometrie de photo thick
		System.out.println("RX reading");
		ImagePlus imgRX=IJ.openImage(this.sourcePath+slash+".."+slash+"RX"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		ItkTransform transRX=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_RX_last.txt");
		regsThin[1]=transRX.transformImage(imgPhotoThin, imgRX,false);

		//     Lire IRM T1 dans geometrie de photo thick
		System.out.println("MRI transformation, phase 1");
		ItkTransform transMRI=null;
		transMRI=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"trans_MRI_AUTO_DENSE_MERGE.tif");
		regsThin[2]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_0.tif"),false);
		regsThin[3]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_1.tif"),false);
		regsThin[4]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_2.tif"),false);
		regsThin[5]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_3.tif"),false);

		regsThin[2]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_0.tif"),false);
		regsThin[3]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_1.tif"),false);
		regsThin[4]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_2.tif"),false);
		regsThin[5]=transMRI.transformImage(imgPhotoThin, IJ.openImage(this.sourcePath+slash+".."+slash+"MRI_CLINICAL"+slash+"Computed_data"+slash+"3_HyperImage"+slash+"stack_3.tif"),false);


		if(doThick) {
			//     Selectionner les slices correspondantes aux photos
			System.out.println("MRI slice selection thick");
			for(int mod=0;mod<6;mod++) {
				System.out.print(" "+mod);
				for(int z=0;z<Z;z++) {
					System.out.println("z="+z+"/"+Z+" <- "+depthSlices[z]+"/"+Zthin);
					regsThin[mod].setSlice(depthSlices[z]+1);
					
					slicesThick[mod][z]=VitimageUtils.imageCopy(regsThin[mod].crop());
				}
				regsThick[mod]=Concatenator.run(slicesThick[mod]);
				VitimageUtils.adjustImageCalibration(regsThick[mod],voxsThick,"mm");
			}
			System.out.print("Save mods");
			IJ.saveAsTiff(regsThick[0],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_0_low_photo_bnw.tif");
			IJ.saveAsTiff(regsThick[1],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_1_low_rx.tif");
			IJ.saveAsTiff(regsThick[2],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_2_low_mri_t1.tif");
			IJ.saveAsTiff(regsThick[3],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_3_low_mri_t2.tif");
			IJ.saveAsTiff(regsThick[4],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_4_low_mri_m0.tif");
			IJ.saveAsTiff(regsThick[5],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_5_low_mri_diff.tif");
			IJ.saveAsTiff(regsThick[6],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_6_low_photo_Red.tif");
			IJ.saveAsTiff(regsThick[7],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_7_low_photo_Green.tif");
			IJ.saveAsTiff(regsThick[8],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_8_low_photo_Blue.tif");
			IJ.saveAsTiff(imgPhotoThick,this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_9_low_photo_rgb.tif");
			
			regsThick[0]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - Lum", regsThick[0],15,0);
			//regsThick[1]=VitimageUtils.writeTextOnImage(this.title+" RX", regsThick[1],15,0);
			//regsThick[2]=VitimageUtils.writeTextOnImage(this.title+" MRI-T1", regsThick[2],15,0);
			//regsThick[3]=VitimageUtils.writeTextOnImage(this.title+" MRI-T2", regsThick[3],15,0);
			//regsThick[4]=VitimageUtils.writeTextOnImage(this.title+" MRI-DP", regsThick[4],15,0);
			regsThick[5]=VitimageUtils.writeTextOnImage(this.title+" MRI-DIFF", regsThick[5],15,0);
			regsThick[6]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - R", regsThick[6],15,0);
			regsThick[7]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - G", regsThick[7],15,0);
			regsThick[8]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - B", regsThick[8],15,0);
	
			System.out.print("Build hyperimage");
			ImagePlus hyperimage=Concatenator.run(regsThick);
			
			IJ.run(hyperimage,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+Z+" frames=9 display=Grayscale");
			IJ.saveAsTiff(hyperimage,this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"hyperimage.tif");
		}	
		if(doThin) {
			//     Selectionner les slices correspondantes aux photos
			System.out.println("RGB duplicate slices");
			int[]modsToDo=new int[] {0,6,7,8};
			for(int mod : modsToDo) {
				System.out.print(" "+mod);
				int[]isSet=new int[Zthin];//set to 0
				for(int z=0;z<Z;z++) {
					System.out.print("Traitement z="+z+"/"+Z+" ");
					regsThin[mod].setSlice(depthSlices[z]+1);
					for(int zz=(Math.max(0,depthSlices[z]-(z%2==0 ? 0 : 2)));zz<(Math.min(Zthin,depthSlices[z]+(z%2==0 ? 3 : 1)) ) ;zz++) {
						 System.out.print(" -> "+zz);
						 slicesThin[mod][zz]=VitimageUtils.imageCopy(regsThin[mod].crop());
						 isSet[zz]=1;
					}
					System.out.println();
				}
				for(int zz=0;zz<Zthin;zz++)if(isSet[zz]<1) {
					System.out.println("slice vide ="+zz+" va etre remplacee par "+(zz-1));
					slicesThin[mod][zz]=VitimageUtils.imageCopy(slicesThin[mod][zz-1]);
				}
				regsThin[mod]=Concatenator.run(slicesThin[mod]);
				
				VitimageUtils.adjustImageCalibration(regsThin[mod],voxsThin,"mm");

			}
			System.out.print("Save mods");
			IJ.saveAsTiff(regsThin[0],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_0_THIN_low_photo_bnw.tif");
			IJ.saveAsTiff(regsThin[1],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_1_THIN_low_rx.tif");
			IJ.saveAsTiff(regsThin[2],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_2_THIN_low_mri_t1.tif");
			IJ.saveAsTiff(regsThin[3],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_3_THIN_low_mri_t2.tif");
			IJ.saveAsTiff(regsThin[4],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_4_THIN_low_mri_m0.tif");
			IJ.saveAsTiff(regsThin[5],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_5_THIN_low_mri_diff.tif");
			IJ.saveAsTiff(regsThin[6],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_6_THIN_low_photo_Red.tif");
			IJ.saveAsTiff(regsThin[7],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_7_THIN_low_photo_Green.tif");
			IJ.saveAsTiff(regsThin[8],this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_8_THIN_low_photo_Blue.tif");
			imgPhotoThin=VitimageUtils.compositeRGBByte(regsThin[6],regsThin[7],regsThin[8], 1, 1, 1);
			IJ.saveAsTiff(imgPhotoThin,this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"Mod_9_THIN_low_photo_rgb.tif");
			regsThin[0]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - Lum", regsThin[0],15,0);
			//regsThin[1]=VitimageUtils.writeTextOnImage(this.title+" RX", regsThin[1],15,0);
			//regsThin[2]=VitimageUtils.writeTextOnImage(this.title+" MRI-T1", regsThin[2],15,0);
			//regsThin[3]=VitimageUtils.writeTextOnImage(this.title+" MRI-T2", regsThin[3],15,0);
			//regsThin[4]=VitimageUtils.writeTextOnImage(this.title+" MRI-DP", regsThin[4],15,0);
			regsThin[5]=VitimageUtils.writeTextOnImage(this.title+" MRI-DIFF", regsThin[5],15,0);
			regsThin[6]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - R", regsThin[6],15,0);
			regsThin[7]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - G", regsThin[7],15,0);
			regsThin[8]=VitimageUtils.writeTextOnImage(this.title+" PHOTO - B", regsThin[8],15,0);
	
			System.out.print("Build hyperimage");
			ImagePlus hyperimage=Concatenator.run(regsThin);
			
			IJ.run(hyperimage,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+Zthin+" frames=9 display=Grayscale");
			IJ.saveAsTiff(hyperimage,this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"hyperimage_THIN.tif");		
		}
	}

	/*----------------------- STEP 13 -----------------------------------------------*/
	public void extendRegistrationToHigherResolutions() {
		int []dims=VitimageUtils.getDimensions(sourceData[1]);int Z=dims[2];
		//##########################################################################""
		//B) Construire photo med res RGB
		//##########################################################################""
		System.out.println("PARTIE B : construction photo med res RGB");
		ImagePlus []tabSlices=new ImagePlus[Z];
		int iterRef=0;
		for(int z=0;z<Z;z++) {
			if(z%10==0)System.out.println("z="+z);
			sourceData[0].setSlice(z+1);
			tabSlices[z]=sourceData[0].crop();
			ItkTransform trans=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_1_"+z+".txt");
			trans.addTransform(ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms2"+slash+"slice_"+z+"_iter_"+iterRef+".txt"));
			tabSlices[z]=trans.transformImage(tabSlices[z], tabSlices[z],false);
		}
		ImagePlus imgPhotoHighRes=Concatenator.run(tabSlices);
		VitimageUtils.adjustImageCalibration(imgPhotoHighRes,sourceData[0]);
		IJ.saveAsTiff(imgPhotoHighRes,this.sourcePath+slash+"Computed_data"+slash+"3_Hyperimage"+slash+"photo_high.tif");
		
		
		//##########################################################################""
		//C) Construire rx med res
		//##########################################################################""
		//Open RXHigh
		
		//Subsample photothin pour atteindre le VX et VY de imgPhotoHighRes
		//Transform RXHigh using its transformation
		//Select interesting slices
		//Concatenate them
		//Write them
		
		
		
		

		//##########################################################################""
		//D) Construire photo high res
		//##########################################################################""
/*		String rep="/media/fernandr/TOSHIBA_EXT/Temp/High_res_photos_registered";
		VitiDialogs.getYesNoUI("Please connect TOSHIBA Hard drive, wait for ubuntu mounting it, then click ok");
		File f=new File(rep);
		while(! f.exists()) {
			VitiDialogs.getYesNoUI("Directory not found : /media/fernandr/TOSHIBA_EXT/Temp/High_res_photos_registered\n Please create it.");
			VitimageUtils.waitFor(1000);
		}
*/
	}

	
	
	
	
	
	
	
	

	@Override
	public ImagePlus computeNormalizedHyperImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setImageForRegistration() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processData() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void start() {
		// TODO Auto-generated method stub
		
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

	private void writeMask() {
		// TODO Auto-generated method stub
		
	}

	private void computeMask() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	public ItkTransform registerMRandRXFinal(ImagePlus imgRX,ImagePlus imgMRI,ItkTransform trInit) {
		ImagePlus imgRef=VitimageUtils.Sub222(imgRX);
		ImagePlus imgMov=VitimageUtils.Sub222(imgMRI);

		double sigma=12;
		int levelMax=1;
		int levelMin=1;
		imgRef.show();
		int viewSlice=252;
		int nbIterations=6;
		int neighXY=1;	int neighZ=1;
		int bSXY=6;		int bSZ=1;
		int strideXY=2;		int strideZ=2;
		ItkTransform transRet=null;
		BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
					0,sigma,levelMin,levelMax,nbIterations,viewSlice,null,neighXY,neighXY,neighZ,bSXY,bSXY,bSZ,strideXY,strideXY,strideZ);
		bmRegistration.displayRegistration=0;
		bmRegistration.displayR2=false;
		bmRegistration.minBlockVariance=15;
		bmRegistration.minBlockScore=0.15;
		transRet=bmRegistration.runBlockMatching(trInit);
		bmRegistration.closeLastImages();
		bmRegistration.freeMemory();
		return transRet;
	}

		
}
