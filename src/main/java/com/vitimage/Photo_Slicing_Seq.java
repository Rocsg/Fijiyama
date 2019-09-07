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
import com.vitimage.VitimageUtils.AcquisitionType;
import com.vitimage.VitimageUtils.Capillary;
import com.vitimage.VitimageUtils.ComputingType;
import com.vitimage.VitimageUtils.SupervisionLevel;

import Hough_Package.Hough_Circle;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.plugin.ImageCalculator;
import ij.process.FloatProcessor;
import ij.process.StackProcessor;
import math3d.Point3d;


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
	private double[][] wrongAngles;
	private ItkTransform[]currentGlobalTransform;
	private double[] center=new double[] {174,89.3};
	private ImagePlus maskForAutoRegistration;
	private ItkTransform[] tabTransInit;
	private ImagePlus roughSegmentation;
	
	
	/**
	 * Constructors, factory and top level functions
	 */
	public Photo_Slicing_Seq(String sourcePath,Capillary cap,SupervisionLevel sup,String title,ComputingType computingType) {
		super(AcquisitionType.PHOTOGRAPH, sourcePath,cap,sup);
		this.computingType=computingType;
		this.title=title;
		this.hyperSize=4;//Gray, R, G, B
	}

	
	public static void main (String[]args) {
		ImageJ ij=new ImageJ();
		System.out.println("Sequence de test de Photo_slicing");
		Photo_Slicing_Seq photo=new Photo_Slicing_Seq("/home/fernandr/Bureau/Traitements/Cep5D/CEP019_S3_Martyr",Capillary.HAS_NO_CAPILLARY,SupervisionLevel.AUTONOMOUS,"CEP019_S3_Martyr",ComputingType.COMPUTE_ALL);
		photo.start(10);
	}
	
	public static Photo_Slicing_Seq Photo_Slicing_SeqFactory(String sourcePath,Capillary cap,SupervisionLevel sup,String dataPath) {
		Photo_Slicing_Seq photo=new Photo_Slicing_Seq(sourcePath,cap,sup,"factory",ComputingType.COMPUTE_ALL);
		photo.start(100);
		return photo;
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
			//firstRoughRegistration();
			break;
		case 3: //data are registered. Time to compute Maps
			if(this.computingType==ComputingType.EVERYTHING_UNTIL_MAPS) return false;
			System.out.println(this.title+" step 3 : pruning segmentation");
			secondFineIterativeRegistrationWithOneReference();
			System.exit(0);
			break;
		case 4:
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
		System.out.println("Quick start : PHOTO sequence hosted in ");
		System.out.println(this.sourcePath.substring(0,this.sourcePath.length()/2));
		System.out.println(this.sourcePath.substring(this.sourcePath.length()/2+1));

		//Explore the path, look for STEPS_DONE.tag and DATA_PARAMETERS.tag
		File fStep=new File(this.sourcePath+slash+"STEPS_DONE.tag");
		File fParam=new File(this.sourcePath+slash+"DATA_PARAMETERS.tag");
		File pathlink = new File(this.sourcePath+slash+"DATA_PATH.tag");

		
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
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms");
			dir.mkdirs();
			dir = new File(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks");
			dir.mkdirs();

			writeStep(0);
			System.out.println("Reading source data...");
			readAndProcessSourceData();
			System.out.println("Writing stacks");
			writeStackedSourceData();
			writeStep(1);
		}
	}

	private void writeMask() {
		// TODO Auto-generated method stub
		
	}

	private void computeMask() {
		// TODO Auto-generated method stub
		
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
		if(readStep>=4) {
			System.out.println("Chargement des transformations de la deuxieme etape");
			for(int i=0;i<this.dimZ();i++) {
				this.currentGlobalTransform[i]=ItkTransform.readTransformFromFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_"+i+".txt");
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
		
		if(strFile.length>=16+this.depthIndications.length) {
			this.wrongAngles=new double[Integer.valueOf(strFile[15+this.depthIndications.length+1].split("=")[1])][2];
			for(int i=0;i<this.wrongAngles.length;i++) {
				String[] sT=strFile[15+this.depthIndications.length+2+i].split(" ");
				this.wrongAngles[i][0]=Double.parseDouble(sT[0]);
				this.wrongAngles[i][1]=Double.parseDouble(sT[1]);
				System.out.println("Lecture d'une indication d angle incorrect : slice="+this.wrongAngles[i][0]+", angle Z to apply="+this.wrongAngles[i][1]);
			}
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
		double voxZ=0.5*(this.depthIndications[0][1]-this.depthIndications[this.depthIndications.length-1][1])*1.0/(this.depthIndications[this.depthIndications.length-1][0]-this.depthIndications[0][0]);
		System.out.println("Estimated overall voxel size="+voxZ);
		VitimageUtils.adjustImageCalibration(sourceData[0], new double[] {this.voxSX()*4,this.voxSY()*4,voxZ},"mm");

		//Faire miroir aux verso
		System.out.println("Mirror all recto along X axis");
		for(int i=0;i<this.dimZ()/2;i++) {
			int sli=2*i;
			this.sourceData[0].setSlice(sli+1);
			IJ.run(this.sourceData[0], "Flip Vertically", "slice");
		}
		VitimageUtils.imageChecking(sourceData[0],"Image apres miroir des recto");

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
	
	public double[] deployWrongAngles() {
		double[]ret=new double[this.dimZ()];
		for(int i=0;i<wrongAngles.length;i++) {
			ret[(int)Math.round(wrongAngles[i][0]-1)]=wrongAngles[i][1];
			System.out.println("A la coupe i="+wrongAngles[i][0]+" application de la correction angulaire d angle "+this.wrongAngles[i][1]);
		}
		return ret;
	}
	
	
	public void computeRoughSegmentation() {
		this.roughSegmentation=VitimageUtils.makeWekaSegmentation(this.sourceData[1],"/home/fernandr/Bureau/Traitements/Cep5D/Weka_hybride/classifier_v2_structure.model");
		IJ.saveAsTiff(this.roughSegmentation,this.sourcePath+slash+"Computed_data"+slash+"0_Stacks"+slash+"roughSegmentation.tif");
	}

	
	public ItkTransform[] detectSlicePoseFromPVC(ImagePlus seg,ImagePlus init,String pathToWriteResult) {
		ImagePlus imgSeg=new Duplicator().run(seg);
		VitimageUtils.adjustImageCalibration(imgSeg, seg);
		ImagePlus imgInit=new Duplicator().run(init);
		VitimageUtils.adjustImageCalibration(imgInit, init);
		imgSeg=VitimageUtils.thresholdFloatImage(imgSeg, 3,4);
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


		///////////DETECTION TUBE		
		imgSeg.show();
		Hough_Circle hough=new Hough_Circle();
		hough.setParameters(radiusTube-1,radiusTube+1, 1, /*int radiusMin, int radiusMax, int radiusInc, */
				1,1,0.1,   /*int minCircles, int maxCircles, double thresholdRatio,*/
				5000,1,10,10, /*int resolution, double ratio, int searchBand, int searchRadius*/
				true,false,false,false,/*boolean reduce, boolean local, boolean houghSeries, boolean showCircles, */
				false,false,true,false /*boolean showID, boolean showScores, boolean results, boolean isGUI*/);	  
		hough.execute();
		while(rt.getCounter()<(zMax) ) {VitimageUtils.waitFor(20);i++; }
		imgSeg.hide();
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
			initTab[z]=trTab[z].transformImage(initTab[z],initTab[z]);
		}
		ImagePlus result=Concatenator.run(initTab);
		VitimageUtils.imageChecking(result,"Apres reduction de la translation");
		
		
		///////////DETECTION INDICATEUR PVC		
		//Preparation image pour tiny
		ImagePlus imgTiny=new Duplicator().run(result);
		VitimageUtils.adjustImageCalibration(imgTiny,imgInit);
		IJ.run(imgTiny, "8-bit", "");
		IJ.run(imgTiny, "Find Edges", "stack");
		imgTiny=VitimageUtils.thresholdByteImage(imgTiny,150,256);
		imgTiny.show();
		Hough_Circle hough2=new Hough_Circle();
		hough2.setParameters(radiusTiny,radiusTiny, 1, /*int radiusMin, int radiusMax, int radiusInc, */
					1,1,0.1,   /*int minCircles, int maxCircles, double thresholdRatio,*/
					5000,1,10,10, /*int resolution, double ratio, int searchBand, int searchRadius*/
					true,false,true,false,/*boolean reduce, boolean local, boolean houghSeries, boolean showCircles, */
					false,false,true,false /*boolean showID, boolean showScores, boolean results, boolean isGUI*/);	  
		hough2.execute();
		i=0;
		while(rt.getCounter()<(2*zMax)) {VitimageUtils.waitFor(20);	i++; }
		System.out.println("Detection des centres des indicateurs ok. Calcul effectué en "+(i*20)+" ms");


		for(int z=0;z<zMax;z++) {
			tinyCenters[z][0]=rt.getValue(1,zMax+z);
			tinyCenters[z][1]=rt.getValue(2,zMax+z);
			tinyCenters[z][2]=z;
			if(z==0) {
				tetaZero=TransformUtils.calculateAngle(tinyCenters[z][0]-imageCenter[0],tinyCenters[z][1]-imageCenter[1]);
				System.out.println("Angle tetaZero="+tetaZero);
			}
			deltaTeta=TransformUtils.diffTeta(TransformUtils.calculateAngle(tinyCenters[z][0]-imageCenter[0],tinyCenters[z][1]-imageCenter[1]),tetaZero);
			System.out.println("Z="+z+"   Tube : ( "+imageCenter[0]+" , "+imageCenter[1]+" )   ,   indicateur : ( "+tinyCenters[z][0]+" , "+tinyCenters[z][1]+" ).  Dteta="+deltaTeta);
			imgInit.setSlice(z+1);
			initTab[z] = imgInit.crop();
			trTab[z].addTransform(ItkTransform.getRigidTransform(imageCenter, new double[] {0,0,deltaTeta/180.0*Math.PI},new double[] {0,0,0}));
			initTab[z]=trTab[z].transformImage(initTab[z],initTab[z]);
		}	
		imgTiny.hide();
		result=Concatenator.run(initTab);
		VitimageUtils.imageChecking(result);
		if(pathToWriteResult!=null)IJ.saveAsTiff(result,pathToWriteResult);
		return trTab;
	}

	public void firstRoughRegistrationV2() { 	
		this.currentGlobalTransform=detectSlicePoseFromPVC(this.roughSegmentation,this.sourceData[1],this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"img_low_after_step_1.tif");
		for(int i=0;i<this.dimZ();i++) this.currentGlobalTransform[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_1_"+i+".txt");
	}
	
	public void firstRoughRegistration() {
		int zFinal=this.dimZ();
		System.out.println("Step 2 : first rough registration");
		///Sur le masque fond, mesurer les hauteurs, les comparer à la hauteur attendue, ce qui donne le delta Y
		//Integrer les eventuelles rotations en construisant une premiere transfo pour chaque slice
		ImagePlus maskIn=IJ.openImage(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"maskOutFond.tif");
		double[][]centers=VitimageUtils.getCentersOfSlicesFromMask(maskIn);
		double[]rotations=deployWrongAngles();
		this.tabTransInit=new ItkTransform[zFinal];
		this.currentGlobalTransform=new ItkTransform[zFinal];
		ImagePlus[]tabSlices=new ImagePlus[this.dimZ()];
		ImagePlus[]tabSlicesMask=new ImagePlus[this.dimZ()];
		for(int i=0;i<zFinal;i++) {
			System.out.println("Calcul transfo initiale slice "+i);
			double[]tabTmp=new double[] {center[0],centers[i][1],centers[i][2]};//le centre de rotation ne doit pas dépendre du centre de la zone exploitable suivant X
			tabTransInit[i]=ItkTransform.getRigidTransform(centers[i], new double[] {0,0,rotations[i]},new double[] {0,centers[i][1]-center[1],0});
			tabTransInit[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_1_"+i+".txt");
			this.currentGlobalTransform[i]=tabTransInit[i];
			System.out.println("There");
			this.sourceData[1].setSlice(i+1);
			tabSlices[i]= this.sourceData[1].crop();
			tabSlices[i]=tabTransInit[i].transformImage(tabSlices[i],tabSlices[i]);
			this.maskForAutoRegistration.setSlice(i+1);
			tabSlicesMask[i]= this.maskForAutoRegistration.crop();
			tabSlicesMask[i]=tabTransInit[i].transformImage(tabSlicesMask[i],tabSlicesMask[i]);
		}
		ImagePlus imgRes=Concatenator.run(tabSlices);
		ImagePlus imgResMask=Concatenator.run(tabSlicesMask);
		VitimageUtils.adjustImageCalibration(imgRes, this.sourceData[1]);
		IJ.saveAsTiff(imgRes, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"Stack_low_after_step_1.tif");
		VitimageUtils.adjustImageCalibration(imgResMask, this.sourceData[1]);
		IJ.saveAsTiff(imgResMask, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"Mask_low_after_step_1.tif");
	}	
		
		
	
	
	public void secondFineIterativeRegistrationWithOneReference() {
		int sliceRef=0;
		System.out.println("Here we go !");
		int nIterations=50;

	
		
		
		
		
		///INITIALISATION
		int zFinal=this.dimZ();
		ImagePlus[]tabSlices=new ImagePlus[zFinal];
		ImagePlus[]tabSlicesMask=new ImagePlus[zFinal];
		
		ImagePlus[]tabSlicesRGB=new ImagePlus[zFinal];
		ItkTransform[]tabTransInd=new ItkTransform[zFinal];
		ItkTransform[]tabTransGlob=new ItkTransform[zFinal];
		System.out.println("");
		System.out.println("Lecture transfos");
		for(int i=0;i<zFinal;i++) {
			tabTransGlob[i]=this.currentGlobalTransform[i];
		}

		for(int i=0;i<100;i++)System.out.println("\n");
		System.out.println("\n-------------------\n ITERATION one-pass\n-----------------\n\n");
		//Application transformation calculée à l'étape precedente
		for(int i=0;i<zFinal;i++) {
			System.out.println("Lecture slices et prepa recalage "+i);
			this.sourceData[1].setSlice(i+1);
			tabSlicesRGB[i] = this.sourceData[1].crop();
			System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(sourceData[1]),"vox init"));
			System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlicesRGB[i]),"vox RGB slice"));
			tabSlicesRGB[i]=tabTransGlob[i].transformImage(tabSlicesRGB[i],tabSlicesRGB[i]);
			System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlicesRGB[i]),"vox RGB slice 2"));
			tabSlices[i]=new Duplicator().run(tabSlicesRGB[i]);
			System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlices[i]), "Vox i"));
			VitimageUtils.adjustImageCalibration(this.sourceData[1], tabSlices[i]);
			System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(this.sourceData[1]), "->ref i"));
			this.sourceData[1].show();
			System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlices[i]), "->Vox i"));
			IJ.run(tabSlices[i],"8-bit","");
		
			this.maskForAutoRegistration.setSlice(i+1);
			tabSlicesMask[i] = this.maskForAutoRegistration.crop();
			tabSlicesMask[i]=VitimageUtils.thresholdByteImage(tabTransGlob[i].transformImage(tabSlicesMask[i],tabSlicesMask[i]),128,256);
		}
		ImagePlus imgRes=Concatenator.run(tabSlicesRGB);
		VitimageUtils.adjustImageCalibration(imgRes, this.sourceData[1]);
		IJ.saveAsTiff(imgRes, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"Stack_low_before_step_2_iter_0.tif");
			
		
			//Recalage des chaque image sur la sliceReference et amendement des transformation en fonction des nouveaux calculs
		tabTransInd[0]=new ItkTransform();
		ItkTransform trToTmpRef=new ItkTransform();
		for(int i=0;i<zFinal;i++) {
			int tmpRef=((i-1)/20)*20;
			System.out.println("\nRecalage de "+i+" sur "+(tmpRef));
			tabTransInd[i]=BlockMatchingRegistration.testSetupAndRunStandardBlockMatchingWithoutFineParameterizationPhoto2D(tabSlices[tmpRef],tabSlices[i],tabSlicesMask[tmpRef], null, true,true,"Pass=one-pass slice="+i+"->"+tmpRef).simplify();
			tabTransInd[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_ind_iter_0_"+i+".txt");
			//System.out.println("calcul de tabTransInd["+i+"]");
			//System.out.println("tabGlob["+i+"].addTransform(trToTmpRef)");
			//System.out.println("tabGlob["+i+"].addTransform(tabTransInd["+i+"])");
			tabTransGlob[i].addTransform(trToTmpRef);
			tabTransGlob[i].addTransform(tabTransInd[i]);
			tabTransGlob[i]=tabTransGlob[i].simplify();
			if(i%20==0) {
				//System.out.println("trToTmpRef.addTransform(tabTransInd["+i+"])");
				trToTmpRef.addTransform(tabTransInd[i]);
			}
		}
		
		
		//Enregistrement des nouvelles transformations
		for(int i=0;i<zFinal;i++) {
			System.out.println("Saving registered slice"+i);
			this.sourceData[1].setSlice(i+1);
			tabSlicesRGB[i] = this.sourceData[1].crop();
			tabSlicesRGB[i]=tabTransGlob[i].transformImage(tabSlicesRGB[i],tabSlicesRGB[i]);
			tabTransGlob[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_iter_0_"+i+".txt");
			tabTransGlob[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_"+i+".txt");
		}

			
		
		//Application finale transformation calculée à l'étape precedente
		for(int i=0;i<zFinal;i++) {
			System.out.println("Lecture slices et prepa recalage "+i);
			this.sourceData[1].setSlice(i+1);
			tabSlicesRGB[i] = this.sourceData[1].crop();
			tabSlicesRGB[i]=tabTransGlob[i].transformImage(tabSlicesRGB[i],tabSlicesRGB[i]);
		}
		imgRes=Concatenator.run(tabSlicesRGB);
		VitimageUtils.adjustImageCalibration(imgRes, this.sourceData[1]);
		IJ.saveAsTiff(imgRes, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"Stack_low_after_step_2.tif");
	}

	
	
	
	
	
	
	public void secondFineIterativeRegistration() {
		System.out.println("Here we go !");
		int nIterations=50;

		///INITIALISATION

		int zFinal=this.dimZ();
		ImagePlus[]tabSlices=new ImagePlus[zFinal];
		ImagePlus[]tabSlicesMask=new ImagePlus[zFinal];
		
		ImagePlus[]tabSlicesRGB=new ImagePlus[zFinal];
		ItkTransform[]tabTransInd=new ItkTransform[zFinal];
		ItkTransform[]tabTransGlob=new ItkTransform[zFinal];
		System.out.println("");
		System.out.println("Lecture transfos");
		for(int i=0;i<zFinal;i++) {
			tabTransGlob[i]=this.currentGlobalTransform[i];
		}

		for(int iter=0;iter<nIterations;iter++) {
			for(int i=0;i<100;i++)System.out.println("\n");
			System.out.println("\n-------------------\n ITERATION "+iter+"\n-----------------\n\n");
			//Application transformation calculée à l'étape precedente
			for(int i=0;i<zFinal;i++) {
				System.out.println("Lecture slices et prepa recalage "+i);
				this.sourceData[1].setSlice(i+1);
				tabSlicesRGB[i] = this.sourceData[1].crop();
				System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(sourceData[1]),"vox init"));
				System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlicesRGB[i]),"vox RGB slice"));
				tabSlicesRGB[i]=tabTransGlob[i].transformImage(tabSlicesRGB[i],tabSlicesRGB[i]);
				System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlicesRGB[i]),"vox RGB slice 2"));
				tabSlices[i]=new Duplicator().run(tabSlicesRGB[i]);
				System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlices[i]), "Vox i"));
				VitimageUtils.adjustImageCalibration(this.sourceData[1], tabSlices[i]);
				System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(this.sourceData[1]), "->ref i"));
				this.sourceData[1].show();
				System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(tabSlices[i]), "->Vox i"));
				IJ.run(tabSlices[i],"8-bit","");
			
				this.maskForAutoRegistration.setSlice(i+1);
				tabSlicesMask[i] = this.maskForAutoRegistration.crop();
				tabSlicesMask[i]=VitimageUtils.thresholdByteImage(tabTransGlob[i].transformImage(tabSlicesMask[i],tabSlicesMask[i]),128,256);
			}
			ImagePlus imgRes=Concatenator.run(tabSlicesRGB);
			VitimageUtils.adjustImageCalibration(imgRes, this.sourceData[1]);
			IJ.saveAsTiff(imgRes, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"Stack_low_before_step_2_iter_"+iter+".tif");
			
		
			//Recalage des images déja recalées, deux par deux, et amendement des transformation en fonction des nouveaux calculs
			for(int i=1;i<zFinal;i++) {
				System.out.println("Recalage de "+i+" sur "+(i-1));
				tabTransInd[i]=BlockMatchingRegistration.testSetupAndRunStandardBlockMatchingWithoutFineParameterizationPhoto2D(tabSlices[i-1],tabSlices[i],tabSlicesMask[i-1], null, true,true,"Pass="+iter+" slice="+i).simplify();
				tabSlicesMask[i-1].hide();
				tabTransInd[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_ind_iter_"+iter+"_"+i+".txt");
				for(int j=i;j<zFinal;j++)tabTransGlob[j].addTransform(tabTransInd[i]);
				tabTransGlob[i]=tabTransGlob[i].simplify();
			}
			
			//Enregistrement des nouvelles transformations
			for(int i=0;i<zFinal;i++) {
				System.out.println("Saving registered slice"+i);
				this.sourceData[1].setSlice(i+1);
				tabSlicesRGB[i] = this.sourceData[1].crop();
				tabSlicesRGB[i]=tabTransGlob[i].transformImage(tabSlicesRGB[i],tabSlicesRGB[i]);
				tabTransGlob[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_iter_"+iter+"_"+i+".txt");
				tabTransGlob[i].writeMatrixTransformToFile(this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Transforms"+slash+"trans_step_2_"+i+".txt");
			}

			
		}
		
		//Application finale transformation calculée à l'étape precedente
		for(int i=0;i<zFinal;i++) {
			System.out.println("Lecture slices et prepa recalage "+i);
			this.sourceData[1].setSlice(i+1);
			tabSlicesRGB[i] = this.sourceData[1].crop();
			tabSlicesRGB[i]=tabTransGlob[i].transformImage(tabSlicesRGB[i],tabSlicesRGB[i]);
		}
		ImagePlus imgRes=Concatenator.run(tabSlicesRGB);
		VitimageUtils.adjustImageCalibration(imgRes, this.sourceData[1]);
		IJ.saveAsTiff(imgRes, this.sourcePath+slash+"Computed_data"+slash+"1_Registration"+slash+"Stacks"+slash+"Stack_low_after_step_2.tif");
	}

	
	public void computePVCcenters() {
		//Segmenter le PVC
		ImagePlus imgPVC=VitimageUtils.makeWekaSegmentation(this.imageForRegistration,"/home/fernandr/Bureau/Test/TestPhotoStack/classifierPVC.model");
		
		//Dans chaque slice, detecter les trois barres, et leur centre. Estimer une premiere transformation rigide à partir de ça
		Point3d[][]coordsTubes=new Point3d[imgPVC.getStackSize()][3];
		ImagePlus imgPVCBin=VitimageUtils.thresholdFloatImage(imgPVC,0.5, 1);
		ImagePlus[]imgTubes=new ImagePlus[3];
		for(int tub=0;tub<3;tub++) {
			imgTubes[tub]=VitimageUtils.connexe(imgPVCBin,0.5,1.001,threshLowPVC[tub+1]*VitimageUtils.getVoxelVolume(imgPVC), threshHighPVC[tub+1]*VitimageUtils.getVoxelVolume(imgPVC), 4, 1, true);
			imgTubes[tub]=VitimageUtils.thresholdShortImage(imgTubes[tub], 1,10E10);
		}
		
		for(int z=0;z<imgPVC.getStackSize();z++){
			for(int tub=0;tub<3;tub++)coordsTubes[z][tub]=VitimageUtils.coordinatesOfObjectInSlice(imgTubes[tub],z,true);
		}
		
	}
		
	public void computeFirstAlignement() {
		//ItkTransform []trs=new ItkTransform[imgPVC.getStackSize()];
		///trs[0]=new ItkTransform();
		
	//	for(int z=1;z<imgPVC.getStackSize();z++){
			///ItkTransform tr=ItkTransform.estimateBestRigid3D(coordsTubes[z-1], coordsTubes[z]);
			
		///}
		
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
	
	
}
