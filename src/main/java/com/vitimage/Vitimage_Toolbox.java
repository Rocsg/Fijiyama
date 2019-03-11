package com.vitimage;
import ij.IJ;
import ij.plugin.filter.Binary;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.plugin.RGBStackMerge;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

//import net.imglib2.type.numeric.RealType;
import org.itk.simple.*;
//import org.scijava.ItemIO;
//import org.scijava.Priority;
//import org.scijava.plugin.Parameter;
//import org.scijava.plugin.Plugin;
import org.itk.simple.ImageRegistrationMethod.MetricSamplingStrategyType;

import com.vitimage.MRUtils;
import imagescience.transform.Transform;
import math3d.JacobiDouble;
import vib.FastMatrix;


/**
 * Vitimage_Toolbox is the toolbox starting point. It handles Gui through ImageJ utilities and Dialog, and manage the necessary process
 * 
 * This toolbox provides three main tools :
 * 1) Transform 3D, the tools for image transformation, matrix composition, axis alignment, capillary substraction
 * 2) MRI Explorer, tool for curve fitting and modeling, T1, T2, M0 map extraction
 * 3) Multimodal time-series assistant, a tool to assist management of high throughput experiments on multi-modal times series
 *
 * @author Romain Fernandez
 */

/**
 * TODO 
 * 1-Change all prototypes
 * 2-Avoid all uses of reech3D
 * 
 * @author Romain Fernandez
 *
 */


public class Vitimage_Toolbox implements PlugIn,ItkImagePlusInterface {
	private final int NO_AUTO_CHOICE=-10;
	private final int TOOL_NOTHING=-1;
	private final int TOOL_1_TRANSFORM_3D=0;
	private final int TOOL_2_MRI_WATER_TRACKER=1;
	private final int TOOL_3_MULTIMODAL_ASSISTANT=2;
	private final int TR3D_0_MANUAL_TR=0;
	private final int TR3D_1_AUTO_TR=1;
	private final int TR3D_2_MAT_TR=2;
	private final int TR3D_3_MANALIGN_TR=3;
	private final int TR3D_4_AUTOALIGN_TR=4;
	private final int TR3D_5_MANCAP_TR=5;
	private final int TR3D_6_AUTOCAP_TR=6;
	private final int MRI_0_EXPLORER=0;
	private final int MRI_1_T1_CALCULATION=1;
	private final int MRI_2_T2_CALCULATION=2;	
	private final boolean SUPERVISED=false;
	private final boolean AUTOMATIC=true;
	private final String OS_SEPARATOR=System.getProperties().getProperty("file.separator");
	private final String[]toolsStr= {"Tool 1 : Transform 3D ","Tool 2 : MRI Water tracker", "Tool 3 : Multimodal Timeseries assistant"};
	private final String[]tr3DToolsStr= {"Tool 1-1 : Image transformation by #N registration matrices",
									  	 "Tool 1-2 : Automatic image transformation by one matrix (for scripting purpose) ", 
									     "Tool 1-3 : Matrix compositions tool",
									     "Tool 1-4 : Bouture Alignment along z axis",
									     "Tool 1-5 : User-assisted capillary removal",
									     "Tool 1-6 : Automated capillary removal"};
	private final String[]mriToolsStr= {"Tool 2-1 : MRI Explorer",
			                            "Tool 2-2 : T1 sequence calculation",
			                            "Tool 2-3 : T2 sequence calculation"};
	public static Analyze anna;
	private ImagePlus resultTemp;
		
	
	/**
	 * The two launching methods are facility that leads to the same behaviour (i.e. the third method).
	 * --> The "main" is the entry point when debugging (from eclipse for example)
	 * --> The "run" is the entry point when using it from ImageJ GUI
	 */	
	public Vitimage_Toolbox(){
		this.anna=new Analyze("Vitimage toolbox");
	}
	public static void main(String[] args) {
		ImageJ imageJ = new ImageJ();
		Vitimage_Toolbox viti=new Vitimage_Toolbox();
		
		
		viti.startPlugin(false);//Basic behaviour is making debugging test
		//viti.analyse();
	}	
	public void run(String arg) {
		this.startPlugin(true);//Basic behaviour is making release test
	}

	/**
	 * Common entry Point
	 */
	public void startPlugin(boolean isRelease) {
		Vitimage_Toolbox vitiTool=new Vitimage_Toolbox();
		boolean isDebuggingReleaseConfiguration=false;//Switcher, for debugging phases and specific testing, go on false
		boolean release=(isRelease ? true : isDebuggingReleaseConfiguration);
		if(release)vitiTool.release();
		else vitiTool.testAlgos();	
	}

	/**
	 * Consumer entry point
	 */
	public void release() {
		int tool=chooseToolUI();//FOR RUNNING PURPOSE
		switch(tool) {
			case TOOL_1_TRANSFORM_3D: runTransform3D(NO_AUTO_CHOICE);break;
			case TOOL_2_MRI_WATER_TRACKER: runMRIWaterTracker(NO_AUTO_CHOICE);break;
			case TOOL_3_MULTIMODAL_ASSISTANT: runMultimodalAssistant();break;
		}
	}

	/**
	 * Backend developer entry point
	 */
	public void testAlgos() {
		System.out.println("Algos");
		//......That shouldnt move among the tests.......
		Date date=new Date();
		long diff1 = System.currentTimeMillis();
		///...............................................		
		runSpecificTesting();//FOR TESTING PURPOSE
		//......That shouldnt move among the tests.......
		long diff2 = System.currentTimeMillis();
		long diff0=(diff2-diff1)/1000;
		System.out.println("Duree totale du processus : "+diff0+" secondes");
		anna.remember("Le processus total a duré ",""+diff0+" secondes");
		Vitimage_Toolbox.imageChecking(resultTemp,0,1000,4,"Deuxieme partie executee : axe aligné",5);
		//......That shouldnt move among the tests.......
	}
	
	/** 
	 * Common testing go there, then stored in Test_codes.java. Waiting for setting up a test automat
	 * */
	@SuppressWarnings("unused")
	private void runSpecificTesting() {

		
		//ItkRegistrationManager.runTestSequence();
		ItkRegistrationManager manager=new ItkRegistrationManager();
		//manager.runInterfaceTestSequence();
		manager.runTestSequence();
	//	manager.debugTest();
	}
	
	
	
	
	
	/**
	/* Tool chooser
	 * */
	private int chooseToolUI(){
	        GenericDialog gd= new GenericDialog("Select mode");
	        gd.addChoice("Tool ", toolsStr,toolsStr[0]);
			gd.showDialog();
	        if (gd.wasCanceled()) return -1;
	        else return (gd.getNextChoiceIndex());

	}

	/**
	/* Tool 1 Transform 3D
	 * This tool let the user apply or compose transformation matrices to an image, and to resample it in custom or computed dimensions
	 * This tool also give facilities like : 
	 *    --> matrices composition tool
	 *    --> capillary detection and removal (useful before an automated registration) 
	 *    --> user defined longitudinal alignment
	 *    --> automated longitudinal alignment
	 *    --> connected components extraction
	 */
	private void runTransform3D(int initChoice) {
		ItkTransform globalTransform;
		int choice;
		ImagePlus imgComp;
		ImagePlus []imgsOut=new ImagePlus[2];
		ImagePlus []imgTab;
		double [] outVoxSize;
		double [] inVoxSize;
		int [] outImgSize;
		double[][]inoculationPointCoordinates=null;
		String outUnit="mm";
		String titleMov,titleRef;
		if(initChoice==NO_AUTO_CHOICE) {
			GenericDialog gd= new GenericDialog("Transform 3D tools");
	        gd.addChoice("Transform 3D mode ", tr3DToolsStr,tr3DToolsStr[0]);gd.showDialog();
			if (gd.wasCanceled()) {choice=-1;IJ.log("Processus annulé. Fin.");return;}
	        else choice=gd.getNextChoiceIndex();
		}
		else choice=initChoice;
		switch(choice) {
			case TR3D_0_MANUAL_TR:
				imgTab=chooseTwoImagesUI("Choose moving image and reference image\n\n","Moving image","Reference image");
				if(imgTab ==null)return;
				titleMov=imgTab[0].getShortTitle();
				titleRef=imgTab[1].getShortTitle();
				globalTransform=chooseTransformsUI("Matrix path from moving to reference",SUPERVISED);
				imgsOut[0]=globalTransform.transformImage(imgTab[0],imgTab[1]);
				if(getYesNoUI("Compute mask ?")) {
					ImagePlus imTemp=new Duplicator().run(imgTab[0]);
					imTemp.getProcessor().fill();
					imgsOut[1]=globalTransform.transformImage(imTemp,imgTab[1]);
				}
				if(getYesNoUI("Save the transformation and its inverse ?")) {
					saveTransformUI(globalTransform,"Save transformation from moving to reference space ?",SUPERVISED,"","transformation_from_"+titleMov+"_to_"+titleRef);
					saveTransformUI(new ItkTransform(globalTransform.getInverse()),"Save inverse transformation from reference to moving space ?",SUPERVISED,"","transformation_from_"+titleRef+"_to_"+titleMov);
				}
				if(getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}
				resultTemp=imgsOut[0];
				break;
			case TR3D_2_MAT_TR:
				globalTransform=chooseTransformsUI("Matrix path from moving to reference",SUPERVISED);
				saveTransformUI(globalTransform,"Save transformation from moving to reference space ?",SUPERVISED,"","globalTransfo");
				saveTransformUI(new ItkTransform(globalTransform.getInverse()),"Save inverse transformation from reference to moving space ?",SUPERVISED,"","globalTransfoInverse");				
				break;
			case TR3D_3_MANALIGN_TR:
				imgTab=new ImagePlus[] {IJ.getImage()};
				titleMov=imgTab[0].getShortTitle();
				if (imgTab[0]==null) {System.out.println("No image to be aligned here");return;}
				//Wait for 4 points, given in real coordinates
				System.out.println("Waiting for the four points");
				inoculationPointCoordinates=waitForPointsUI(4,imgTab[0],true);
								
				//Compute the transformation, according to reference geometry, and the given 4 points. Resample image
				globalTransform=ItkTransform.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),false,inoculationPointCoordinates,false);
				imgsOut[0]=globalTransform.transformImage(imgTab[0],imgTab[0]);			

				if(getYesNoUI("Save the transformation and its inverse ?")) {
					saveTransformUI(globalTransform,"Save transformation from moving to reference space ?",SUPERVISED,"","transformation_from_"+titleMov+"_to_z_axis_reference");
					saveTransformUI(new ItkTransform(globalTransform.getInverse()),"Save inverse transformation from reference to moving space ?",SUPERVISED,"","transformation_from_z_axis_to_"+titleMov);
				}
				
				if(getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}				
				resultTemp=imgsOut[0];
				break;
			case TR3D_4_AUTOALIGN_TR:
				imgTab=new ImagePlus[] {IJ.getImage()};
				anna.notifyStep("Entree de case","");
				for(int i=0;i<imgTab.length ; i++)anna.storeImage(imgTab[i],"image "+i);
				titleMov=imgTab[0].getShortTitle();

				/////Compute a first alignment along Z axis
				globalTransform=ItkTransform.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),true,null,true);
				imgsOut[0]=globalTransform.transformImage(imgTab[0],imgTab[0]);			
				anna.storeImage(imgsOut[0],"Image reechantillonnee apres aligment suivant Z");
				resultTemp=imgsOut[0];
				break;
			case TR3D_5_MANCAP_TR:notYet("Vitimage_Toolbox : TR3D_5_MANCAP_TR");break;
			case TR3D_6_AUTOCAP_TR:notYet("Vitimage_Toolbox : TR3D_6_AUTOCAP_TR");break;
		}
		if(getYesNoUI("Le processus est terminé. \nVoulez-vous continuer à utiliser la tool box ?")){release();} 
	}

	/** Tool 2*/
	private void runMRIWaterTracker(int initChoice) {
		MRUtils mrUt=new MRUtils();
		int choice;
		ImagePlus []imgTab;
		ImagePlus []imgsT1;
		ImagePlus []imgsT2;
		double []tabTe;
		double []tabTr;
		double sigma=mrUt.stdSigmaIRM;
		double te=11;
		int nTe=16;
		int nTr=3;
		double [] voxSizeT1=mrUt.getT1Times(nTr);
		double [] voxSizeT2=mrUt.getProportionalTimes(te, nTe*te,te);
		int [] sizeT1;
		int [] sizeT2;
		String outUnit="mm";
		String []titles;
		
		if(initChoice==NO_AUTO_CHOICE) {
			GenericDialog gd= new GenericDialog("MRI 3D tools");
	        gd.addChoice("MRI 3D mode ", mriToolsStr,mriToolsStr[0]);
			gd.showDialog();
			if (gd.wasCanceled()) choice=-1;
	        else choice=gd.getNextChoiceIndex();
		}
		else choice=initChoice;
		// 0 choice (explorer) : Ask for T1 and T2 images, then plot a point that can be moved by the user
		// 1 choice (T1 calculation) : Ask for T1, then compute the T1 Map, according to rice bias estimation, using a monocomponent exponential
		// 2 choice (T2 calculation) : Ask for T2, then compute the T2 and M0 Maps, according to rice bias estimation, using a monocomponent exponential
		switch(choice) {
			case MRI_0_EXPLORER:
				//Open data
				imgTab=chooseTwoImagesUI("Choose MRI T1 sequence (varying Tr) and MRI T2 sequence (varying Te)","T1 sequence : ","T2 sequence : ");
				if(imgTab ==null)return;
				titles=new String[] {imgTab[0].getShortTitle(),imgTab[1].getShortTitle()};
				sizeT1=chooseSizeUI(imgTab[0],"",AUTOMATIC);
				sizeT2=chooseSizeUI(imgTab[1],"",AUTOMATIC);
				voxSizeT1=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				voxSizeT2=chooseVoxSizeUI(imgTab[1],"",AUTOMATIC);
				MRICurveExplorerWindow explorer=new MRICurveExplorerWindow(imgTab[0],imgTab[1]);
				break;

			case 10:
				int a=1;
				break;
		}
	}


	/** Tool 3*/
	private void runMultimodalAssistant() {
		ItkRegistrationManager manager=new ItkRegistrationManager();
		//manager.runInterfaceTestSequence();
		manager.runTestSequence();
		//notYet("Vitimage_Toolbox : runMultimodalAssistant");	
	}

	
	
	
	
	
	/** 
	 * UI interfaces for the tools
	 * */
	public static ImagePlus[] chooseTwoImagesUI(String strGuess,String strImg1, String strImg2) {
			ImagePlus[]imgRet=new ImagePlus[2];
			String open="* Je vais choisir une image dans l'explorateur de fichiers *";
			int index1,index2;
			int[] wList = WindowManager.getIDList();
			String[] titles=(wList==null) ? new String[1] : new String[wList.length+1] ;
			titles[0]=open;
			if (wList!=null) {
		        for (int i=0; i<wList.length; i++) {
		        	ImagePlus imp = WindowManager.getImage(wList[i]);
		            titles[i+1] = imp!=null?imp.getTitle():"";
		        }
	        }
	        GenericDialog gd= new GenericDialog(strGuess);
	        gd.addChoice(strImg1, titles,open);
	        gd.addChoice(strImg2, titles,open);
			gd.showDialog();
	        if (gd.wasCanceled()) return null;
	       	index1 = gd.getNextChoiceIndex()-1;
	       	index2 = gd.getNextChoiceIndex()-1;

	       	if(index1 < 0) {
	       		OpenDialog od1=new OpenDialog("Select "+strImg1);
	       		imgRet[0]=IJ.openImage(od1.getPath());
	       	}
	       	else imgRet[0]=WindowManager.getImage(wList[index1]);
	      
	       	if(index2 < 0) {
	       		OpenDialog od2=new OpenDialog("Select "+strImg2);
	       		imgRet[1]=IJ.openImage(od2.getPath());
	       	}
	       	else imgRet[1]=WindowManager.getImage(wList[index2]);
	   	       	
	       	anna.remember("Choix image 1 : ",imgRet[0].getShortTitle());
	       	anna.remember("Choix image 2 : ",imgRet[1].getShortTitle());
        	return imgRet;
	}
		
	public double[] chooseVoxSizeUI(ImagePlus img,String strGuess,boolean autonomyLevel) {
		 double[]tabRet=new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
		 if(autonomyLevel==AUTOMATIC)return tabRet;
		 else {
				GenericDialog gd = new GenericDialog(strGuess);
		        gd.addNumericField("Vx suggestion :",tabRet[0], 5);
		        gd.addNumericField("Vy suggestion :",tabRet[1], 5);
		        gd.addNumericField("Vz suggestion :",tabRet[2], 5);
		        gd.showDialog();
		        if (gd.wasCanceled()) {System.out.println("Warning : vox sizes set by default 1.0 1.0 1.0"); return new double[] {1.0,1.0,1.0};}
		 
		        tabRet[0] = gd.getNextNumber();
		        tabRet[1] = gd.getNextNumber();
		        tabRet[2] = gd.getNextNumber();
			 return tabRet;
		 }
	}

	public static boolean getYesNoUI(String strGuess) {
        GenericDialog gd=new GenericDialog(strGuess);
        gd.addMessage(strGuess);
        gd.enableYesNoCancel("Yes", "No");
        gd.showDialog();
    	return (gd.wasOKed());
	}

	public int[] chooseSizeUI(ImagePlus img,String strGuess,boolean autonomyLevel) {
		 int[]tabRet=new int[] {img.getWidth(),img.getHeight(),img.getStack().getSize()};
		 if(autonomyLevel==AUTOMATIC)return tabRet;
		 else {
				GenericDialog gd = new GenericDialog(strGuess);
		        gd.addNumericField("Dim_X suggestion :",tabRet[0],1);
		        gd.addNumericField("Dim_Y suggestion :",tabRet[1],1);
		        gd.addNumericField("Dim_Z suggestion :",tabRet[2],1);
		        gd.showDialog();
		        if (gd.wasCanceled()) {System.out.println("Warning : Dims set by default 100.0 100.0 100.0"); return new int[] {100,100,100};}
		 
		        tabRet[0] = (int) gd.getNextNumber();
		        tabRet[1] = (int) gd.getNextNumber();
		        tabRet[2] = (int) gd.getNextNumber();
			 return tabRet;
		 }
	}

	
	public ItkTransform chooseTransformsUI(String strGuess,boolean autonomyLevel){
		ItkTransform globalTransform = null;
		int iTr=-1;
		boolean oneAgain=true;
		GenericDialog gd2;
		do {
			iTr++;
			OpenDialog od=new OpenDialog("Select_transformation_#"+(iTr)+"");
			if(iTr==0)globalTransform=ItkTransform.readTransformFromFile(od.getPath());
			else globalTransform.addTransform(ItkTransform.readTransformFromFile(od.getPath()));
			gd2 = new GenericDialog("Encore une transformation ?");
	        gd2.addMessage("One again ?");
	        gd2.enableYesNoCancel("Yes", "No");
	        gd2.showDialog();
	    	oneAgain=gd2.wasOKed();
		} while(oneAgain);
		iTr++;
		return (globalTransform.simplify());
	 }
		
	public void saveTransformUI(ItkTransform tr,String strGuess,boolean autonomyLevel,String path,String title){
		if(autonomyLevel==AUTOMATIC) {
			String pathSave=path+""+title;
			tr.writeTransform(pathSave);
		}
		else {
			SaveDialog sd=new SaveDialog(strGuess,title,".itktr");
			if(sd.getDirectory()==null ||  sd.getFileName()==null)return;
			String pathSave=sd.getDirectory()+""+sd.getFileName();
			tr.writeTransform(pathSave);
		}
	}

	public void saveImageUI(ImagePlus img,String strGuess,boolean autonomyLevel,String path,String title) {
		if(autonomyLevel==AUTOMATIC) {
			String pathSave=path+title;
			IJ.saveAsTiff(img,pathSave);
		}
		else {
			SaveDialog sd=new SaveDialog(strGuess,title,".tif");
			if(sd.getDirectory()==null ||  sd.getFileName()==null)return;
			String pathSave=sd.getDirectory()+""+sd.getFileName();
			IJ.saveAsTiff(img,pathSave);
		}
	
	}
	
	public double[][] waitForPointsUI(int nbWantedPoints,ImagePlus img,boolean realCoordinates){
		double[][]tabRet=new double[nbWantedPoints][3];
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		boolean finished =false;
		getYesNoUI("Identification of the four corners \nof the inoculation point with ROI points\nAre you ready  ?");
		do {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(rm.getCount()==nbWantedPoints && getYesNoUI("Confirm points ?"))finished=true;
			System.out.println("Waiting "+nbWantedPoints+". Current number="+rm.getCount());
		}while (!finished);	
		for(int indP=0;indP<nbWantedPoints;indP++){
			tabRet[indP][0]=rm.getRoi(indP).getXBase();
			tabRet[indP][1]=rm.getRoi(indP).getYBase();
			tabRet[indP][2]=rm.getRoi(indP).getZPosition();
			if(realCoordinates) {
				tabRet[indP][0]=(tabRet[indP][0]+0.5)*(img.getCalibration().pixelWidth);
				tabRet[indP][1]=(tabRet[indP][1]+0.5)*(img.getCalibration().pixelHeight);
				tabRet[indP][2]=(tabRet[indP][2]+0.5)*(img.getCalibration().pixelDepth);
			}	
			System.out.println("Point retenu numéro "+indP+" : {"+tabRet[indP][0]+","+tabRet[indP][1]+","+tabRet[indP][2]+"}");
		}
		return tabRet;
	}
	
	public static void imageChecking(ImagePlus imgInit,double sliMin,double sliMax,int periods,String message,double totalDuration) {
		int minFrameRateForVisualConfort=33;
		int maxDurationForVisualConfort=1000/minFrameRateForVisualConfort;
		if (imgInit==null)return;
		ImagePlus img=new Duplicator().run(imgInit,1,imgInit.getStackSize());
		String titleOld=img.getTitle();
		String str;
		if (message.compareTo("")==0)str=titleOld;
		else str=message;
		img.setTitle(str);
		int sliceMin=(int)Math.round(sliMin);
		int sliceMax=(int)Math.round(sliMax);
		int miniDuration=0;
		if(periods<1)periods=1;
		if(sliceMin<1)sliceMin=1;
		if(sliceMin>img.getStackSize())sliceMin=img.getStackSize();
		if(sliceMax<1)sliceMax=1;
		if(sliceMax>img.getStackSize())sliceMax=img.getStackSize();
		if(sliceMin>sliceMax)sliceMin=sliceMax;
		img.show();
		if(img.getType() != ImagePlus.COLOR_RGB)IJ.run(img,"Fire","");
		if(sliceMin==sliceMax) {
			miniDuration=(int)Math.round(1000.0*totalDuration/periods);
			img.setSlice(sliceMin);
			for(int i=0;i<periods;i++)waitFor(miniDuration);
			return;
		}
		else {
			miniDuration=(int)Math.round(totalDuration*1000.0/periods/(sliceMax-sliceMin+10));
			while(miniDuration>maxDurationForVisualConfort) {
				periods++;
				miniDuration=(int)Math.round(totalDuration*1000.0/periods/(sliceMax-sliceMin+10));				
			}
			int curSlice=(sliceMin+sliceMax)/2;
			for(int j=0;j<5 ;j++)waitFor(miniDuration);
			img.setSlice((sliceMin+sliceMax/2));
			for(int i=0;i<periods;i++) {
				while (curSlice>sliceMin) {
					img.setSlice(--curSlice);
					waitFor(miniDuration);
				}
				for(int j=0;j<5 ;j++)waitFor(miniDuration);
				while (curSlice<sliceMax) {
					img.setSlice(++curSlice);
					waitFor(miniDuration);
				}
				for(int j=0;j<5 ;j++)waitFor(miniDuration);
			}
		}
		img.close();
		System.out.println("Miniduration="+miniDuration);
	}
	public static void imageChecking(ImagePlus img,String message,double totalDuration) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration);
		else imageChecking(img,0,img.getStackSize()-1,1,message,totalDuration);
	}
	public static void imageChecking(ImagePlus img,String message) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),4);
		else imageChecking(img,0,img.getStackSize()-1,1,message,4);
	}
	public static void imageChecking(ImagePlus img,double totalDuration) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration);
	}
	public static void imageChecking(ImagePlus img) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),3);
	}
	

	
	/** Utility functions for tool 2*/
	
	
	
	
	

	
	
	/** Utility functions for tool 3*/

	
	
	
	
	
	
	
	
	/**
	 * Other utilities and test code
	 */
	public void testAnna(){
		ImagePlus imgJ70=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
		ImagePlus imgT2=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_2-1_T2.tif");
		ImagePlus imgT1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_2-1_T1.tif");
		anna.notifyAlgo("Testing","pour voir si tu marches, Anna","");
		anna.notifyStep("Une premiere etape","donnees inexistantes");
		anna.notifyStep("Une seconde etape","donnees toujours pas");
		anna.notifyStep("Une troisieme etape","donnees encore toujours pas");

		anna.notifyAlgo("Connexe, pour voir","Detecter les composantes connexes pour trouver la moelle","image entrée : IRM J0");
		anna.notifyStep("Coupure en deux stacks","Taille des stacks : "+5);
		anna.storeImage(imgT1,"Stack numero 1, partie up");
		anna.storeImage(imgT2,"Stack numero 2, partie up");
		anna.notifyStep("Extraction composantes","Nombre de composantes : "+12);
		anna.notifyStep("Resolution groupes","groupes elimines : "+15+" sur la base de leurs volumes, inferieurs à "+200);

		anna.notifyAlgo("Seuillage","Separer capillaire","image entrée : IRM J0 connexe");
		anna.storeImage(imgJ70,"Image donnee en entree");
		anna.notifyStep("Recherche seuil","seuil identifié : "+89+" alors que min ="+12+" et max = "+45);
		anna.storeImage(imgJ70,"Seuil valeur 1="+70);
		anna.storeImage(imgJ70,"Seuil valeur 2="+80);
		anna.notifyStep("Calcul volume","valeur : "+15000);
		anna.notifyStep("Calcul contour par outlining","");
		anna.storeImage(imgJ70,"Image pour mettre le bazar");
		anna.storeImage(imgJ70,"Autre Image pour mettre le bazar");
		Scanner sc = new Scanner(System.in);
		while(true) {
			String str = sc.nextLine();
			System.out.println(anna.talk(str,false));
		}
		
		
	}
	
	public static void waitFor(int n) {
		try {
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public static void closeEverything() {
		WindowManager.closeAllWindows();
		IJ.getInstance().quit();
	}
	
	class VolumeComparator implements java.util.Comparator {
		   public int compare(Object o1, Object o2) {
		      return ((Double) ((Double[]) o1)[0]).compareTo(((Double[]) o2)[0]);
		   }
		}
	
	public void analyse(){
		Scanner sc = new Scanner(System.in);
		while(true) {
			String str = sc.nextLine();
			System.out.println(anna.talk(str,false));
		}
	}
	
	public static ImagePlus compositeOf(ImagePlus img1,ImagePlus img2){
	//	return(RGBStackMerge.mergeChannels(new ImagePlus[] {img1,img2},false));
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		return new ImagePlus("Composite",is);
	}
	
	public static ImagePlus compositeOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}
	
	public static void notYet(String message) {
		IJ.log(message+"\nCette fonctionnalité est en cours de développement.\nD'avance, nous vous remercions de votre patience.\nNos équipes font le meilleur chaque jour pour satisfaire vos besoins.\nLe cas échéant, vous pouvez envoyer une requête de fonctionnalité à \n\n    romainfernandez06@gmail.com");
	}
	
	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize) {
		ImagePlus ret=new Duplicator().run(img);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(10*img.getWidth()*1.0/512,10*img.getWidth()*1.0/512, text, font);
		roi.setStrokeColor(Color.white);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			IJ.run(ret, "Draw", "stack");
			ret.setRoi((Roi)null);
		}
		return ret;
	}
	
	public static void putThatImageInThatOther(ImagePlus source,ImagePlus dest) {
		int dimX= source.getWidth(); int dimY= source.getHeight(); int dimZ= source.getStackSize();
		for(int z=0;z<dimZ;z++) {
			int []tabDest=(int[])dest.getStack().getProcessor(z+1).getPixels();
			int []tabSource=(int[])source.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					tabDest[dimX*y+x]=tabSource[dimX*y+x];
				}
			}
		}
	}

		
	
}  
