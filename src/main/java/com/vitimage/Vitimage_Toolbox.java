package com.vitimage;
import ij.IJ;
import ij.plugin.filter.Binary;
import ij.plugin.filter.ThresholdToSelection;
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
import ij.plugin.ImageCalculator;
import ij.plugin.Memory;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.StackProcessor;
import ij.plugin.RGBStackMerge;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

//import net.imglib2.type.numeric.RealType;
import org.itk.simple.*;
//import org.scijava.ItemIO;
//import org.scijava.Priority;
//import org.scijava.plugin.Parameter;
//import org.scijava.plugin.Plugin;
import org.itk.simple.ImageRegistrationMethod.MetricSamplingStrategyType;

import com.sun.tools.extcheck.Main;
import com.vitimage.MRUtils;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.VitimageUtils.AcquisitionType;

import imagescience.transform.Transform;
import math3d.JacobiDouble;
import math3d.Point3d;
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
 * 1-check safety for the vx/2 stuff (see VitimageUtils.detectIP(), end of function)      . Same for TransformUtils line 62 (axis), where I removed it
 * 2-Avoid all uses of reech3D
 * 
 * @author Romain Fernandez
 *
 */


public class Vitimage_Toolbox implements PlugIn,ItkImagePlusInterface,VitiDialogs,VitimageUtils {
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
	
	
	public static void testRecKhalifa() {
		String path="/home/fernandr/Bureau/Test/TestKhalifa/";
		ImagePlus imgRef=IJ.openImage(path+"RX_auto-1.tif");
		ImagePlus imgMov=IJ.openImage(path+"IRM_auto-2.tif");	
		ImagePlus masque=IJ.openImage(path+"masque.tif");
		ItkRegistrationManager it=new ItkRegistrationManager();
		ItkTransform res=it.runScenarioKhalifa2(new ItkTransform(),imgRef,imgMov,true,null);
		ImagePlus result=res.transformImage(imgRef, imgMov);
		result.show();
		IJ.save(result,"/home/fernandr/Bureau/Test/TestKhalifa/test1.tif");
		res.writeToFile("/home/fernandr/Bureau/Test/TestKhalifa/test1.txt");
		//ImagePlus mask=VitimageUtils.restrictionMaskForFadingHandling(imgRef, 10);
		//mask.show();
		//imgRef.show();
		//imgMov.show();
		//VitimageUtils.waitFor(100000);
		//recGrad : -0,896554
		//recGradMask : -0,901105
		//recGradCut : -0,901103
		//recAMOEB : -0,916413
		//recAmoebMask : -0,923018
		//recAmoebCut : -0,922170
		//rec
	
	}
	
	public static void testRemCapHyper() {
		ImagePlus testIn=IJ.openImage("/home/fernandr/Bureau/Test/Hyper/test.tif");
		VitimageUtils.imageChecking(testIn,"TestIn");
		ImagePlus testOut=VitimageUtils.removeCapillaryFromHyperImageForRegistration(testIn);
		VitimageUtils.imageChecking(testOut,"TestOut");
	}
	
	public static void testRecAvecMasque() {
		String path="/home/fernandr/Bureau/Test/RecMasque";
		ImagePlus imgRef=IJ.openImage(path+"/Recovery_3.tif");
		ImagePlus imgMov=IJ.openImage(path+"/Recovery_1.tif");	
		ItkRegistrationManager it=new ItkRegistrationManager();
		ImagePlus[] res=it.runScenarioInterEchoes(imgRef,imgMov);
		IJ.save(res[0], "/home/fernandr/Bureau/Test/RecMasque/recBLEACH2.tif");
		//ImagePlus mask=VitimageUtils.restrictionMaskForFadingHandling(imgRef, 10);
		//mask.show();
		//imgRef.show();
		//imgMov.show();
		//VitimageUtils.waitFor(100000);
		//recGrad : -0,896554
		//recGradMask : -0,901105
		//recGradCut : -0,901103
		//recAMOEB : -0,916413
		//recAmoebMask : -0,923018
		//recAmoebCut : -0,922170
		//rec
	
	}
	
	
	public static void main(String[] args) {
		ImageJ imageJ = new ImageJ();
		Vitimage_Toolbox viti=new Vitimage_Toolbox();
		//testSegmentationAndDetectionStrategyUponVariousMRI();
		//testMask();
		

		testRemCapHyper();
		
		
		
		
		
		
		//				ImagePlus imgUp=IJ.openImage("/home/fernandr/Bureau/pouet.tif");
		//		System.out.println("Est nulle image ?"+VitimageUtils.isNullImage(imgUp));
		//	imgUp.show();
		//			VitimageUtils.waitFor(10000);
		//		VitimageUtils.imageChecking(result);
		System.exit(0);
		

		//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/VITIMAGE4D/Source_data/MRI_T1_SEQ/Computed_data/3_HyperImage/hyperImage.tif");
		
		
		
		
		
		
		
		

		ItkTransform itkTrans=ItkTransform.readFromFile("/home/fernandr/Bureau/Test/VITIMAGE4D/Computed_data/0_Registration/transformation_2_step_afterAxisAlignment.txt");
		System.out.println("itkTrans = "+itkTrans);
		ItkTransform itkTrans2=itkTrans.simplify();
		System.out.println("itkTrans2 = "+itkTrans2);
		
		Transform transIj=new Transform(1,2,3,4,5,6,7,8,9,10,11,12);
		ItkTransform transItk=ItkTransform.ijTransformToItkTransform(transIj);
		transItk.addTransform(transItk);
		transItk.writeToFile("/home/fernandr/Bureau/Test/testWrite.txt");
		ItkTransform trans2=ItkTransform.readFromFile("/home/fernandr/Bureau/Test/testWrite.txt");
		System.out.println("Et en effet :");
		System.out.println("Transfo 1 ="+transItk);
		System.out.println("Transfo 2 ="+trans2);
		
		//		viti.startPlugin(false);//Basic behaviour is making debugging test
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
		VitimageUtils.imageChecking(resultTemp,0,1000,4,"Deuxieme partie executee : axe aligné",5,false);
		//......That shouldnt move among the tests.......
	}
	
	/** 
	 * Common testing go there, then stored in Test_codes.java. Waiting for setting up a test automat
	 * */
	@SuppressWarnings("unused")
	private void runSpecificTesting() {
		//MRI_T1_Seq.main(null); 
		
		//ItkRegistrationManager.runTestSequence();
		//ItkRegistrationManager manager=new ItkRegistrationManager();
		//manager.runInterfaceTestSequence();
		//manager.runTestSequence();
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
				imgTab=VitiDialogs.chooseTwoImagesUI("Choose moving image and reference image\n\n","Moving image","Reference image");
				if(imgTab ==null)return;
				titleMov=imgTab[0].getShortTitle();
				titleRef=imgTab[1].getShortTitle();
				globalTransform=VitiDialogs.chooseTransformsUI("Matrix path from moving to reference",SUPERVISED);
				imgsOut[0]=globalTransform.transformImage(imgTab[0],imgTab[1]);
				if(VitiDialogs.getYesNoUI("Compute mask ?")) {
					ImagePlus imTemp=new Duplicator().run(imgTab[0]);
					imTemp.getProcessor().fill();
					imgsOut[1]=globalTransform.transformImage(imTemp,imgTab[1]);
				}
				if(VitiDialogs.getYesNoUI("Save the transformation and its inverse ?")) {
					VitiDialogs.saveTransformUI(globalTransform,"Save transformation from moving to reference space ?",SUPERVISED,"","transformation_from_"+titleMov+"_to_"+titleRef);
					VitiDialogs.saveTransformUI(new ItkTransform(globalTransform.getInverse()),"Save inverse transformation from reference to moving space ?",SUPERVISED,"","transformation_from_"+titleRef+"_to_"+titleMov);
				}
				if(VitiDialogs.getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {VitiDialogs.saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}
				resultTemp=imgsOut[0];
				break;
			case TR3D_2_MAT_TR:
				globalTransform=VitiDialogs.chooseTransformsUI("Matrix path from moving to reference",SUPERVISED);
				VitiDialogs.saveTransformUI(globalTransform,"Save transformation from moving to reference space ?",SUPERVISED,"","globalTransfo");
				VitiDialogs.saveTransformUI(new ItkTransform(globalTransform.getInverse()),"Save inverse transformation from reference to moving space ?",SUPERVISED,"","globalTransfoInverse");				
				break;
			case TR3D_3_MANALIGN_TR:
				imgTab=new ImagePlus[] {IJ.getImage()};
				titleMov=imgTab[0].getShortTitle();
				if (imgTab[0]==null) {System.out.println("No image to be aligned here");return;}
				//Wait for 4 points, given in real coordinates
				System.out.println("Waiting for the four points");
				inoculationPointCoordinates=VitiDialogs.waitForPointsUI(4,imgTab[0],true);
								
				//Compute the transformation, according to reference geometry, and the given 4 points. Resample image
				//globalTransform=ItkTransform.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),false,inoculationPointCoordinates,false);
				//imgsOut[0]=globalTransform.transformImage(imgTab[0],imgTab[0]);			

				//if(VitiDialogs.getYesNoUI("Save the transformation and its inverse ?")) {
				//	VitiDialogs.saveTransformUI(globalTransform,"Save transformation from moving to reference space ?",SUPERVISED,"","transformation_from_"+titleMov+"_to_z_axis_reference");
				//	VitiDialogs.saveTransformUI(new ItkTransform(globalTransform.getInverse()),"Save inverse transformation from reference to moving space ?",SUPERVISED,"","transformation_from_z_axis_to_"+titleMov);
				//}
				
				if(VitiDialogs.getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {VitiDialogs.saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}				
				resultTemp=imgsOut[0];
				break;
			case TR3D_4_AUTOALIGN_TR:
				imgTab=new ImagePlus[] {IJ.getImage()};
				anna.notifyStep("Entree de case","");
				for(int i=0;i<imgTab.length ; i++)anna.storeImage(imgTab[i],"image "+i);
				titleMov=imgTab[0].getShortTitle();

				/////Compute a first alignment along Z axis
				//globalTransform=ItkTransform.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),true,null,true);
				//imgsOut[0]=globalTransform.transformImage(imgTab[0],imgTab[0]);			
				anna.storeImage(imgsOut[0],"Image reechantillonnee apres aligment suivant Z");
				resultTemp=imgsOut[0];
				break;
			case TR3D_5_MANCAP_TR:VitiDialogs.notYet("Vitimage_Toolbox : TR3D_5_MANCAP_TR");break;
			case TR3D_6_AUTOCAP_TR:VitiDialogs.notYet("Vitimage_Toolbox : TR3D_6_AUTOCAP_TR");break;
		}
		if(VitiDialogs.getYesNoUI("Le processus est terminé. \nVoulez-vous continuer à utiliser la tool box ?")){release();} 
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
				imgTab=VitiDialogs.chooseTwoImagesUI("Choose MRI T1 sequence (varying Tr) and MRI T2 sequence (varying Te)","T1 sequence : ","T2 sequence : ");
				if(imgTab ==null)return;
				titles=new String[] {imgTab[0].getShortTitle(),imgTab[1].getShortTitle()};
				sizeT1=VitiDialogs.chooseSizeUI(imgTab[0],"",AUTOMATIC);
				sizeT2=VitiDialogs.chooseSizeUI(imgTab[1],"",AUTOMATIC);
				voxSizeT1=VitiDialogs.chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				voxSizeT2=VitiDialogs.chooseVoxSizeUI(imgTab[1],"",AUTOMATIC);
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

	
	
	
	
	/** Utility functions for tool 2*/
	
	
	
	
	

	
	
	/** Utility functions for tool 3*/

	
	
	
	public static void testFiles() {
		String folder="/home/fernandr/Bureau/Test";
	    File directory = new File(folder);
	    File[] contents = directory.listFiles();
	    for(int i=0;i<5;i++) {
	    	File f=contents[i];
	    	System.out.println("\nAffichage nouvel element");
	    	System.out.println(f.getAbsolutePath());
	    	System.out.println("Is directory ?"+f.isDirectory());
	    }
	}
	
	
	
	
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

	
	
	
	
	
	

	public static ImagePlus detectAxisAndSetItVertical(ImagePlus img,AcquisitionType acqType) {
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;

		double refCenterX=xMax*vX/2.0;
		double refCenterY=yMax*vY/2.0;
		double refCenterZ=zMax*vZ/2.0;
		Point3d[]pInit=new Point3d[3];
		Point3d[]pFin=new Point3d[3];
		pFin=VitimageUtils.detectAxis(img,acqType);//in the order : center of object along its axis , center + daxis , center + dvect Orthogonal to axis 				
		pInit[0]=new Point3d(refCenterX, refCenterY     , refCenterZ     );//origine
		pInit[1]=new Point3d(refCenterX, refCenterY     , 1 + refCenterZ );//origine + dZ
		pInit[2]=new Point3d(refCenterX, 1 + refCenterY , refCenterZ     );//origine + dY
		System.out.println("Image local basis 0 / 0Z / 0Y : \n"+pFin[0]+"\n"+pFin[1]+"\n"+pFin[2]+"");
		ItkTransform trAdd=ItkTransform.estimateBestRigid3D(pFin, pInit);
		ImagePlus test=trAdd.transformImage(img,img);
		
		return(test);
	}
	
	public static void testDetectAxis(){

		
		for(int i=1;i<31;i++) {
			for(int T=1;T<2;T++) {
				String path="/home/fernandr/Bureau/Test/testMasks/imageT"+T+"_"+i+".tif";
				System.out.println("\n\nTest sur image T"+T+" :\n"+path);
				ImagePlus img=IJ.openImage(path);
				ImagePlus result=detectAxisAndSetItVertical(img,AcquisitionType.MRI_T1_SEQ); 
				IJ.save(result,"/home/fernandr/Bureau/Test/testMasks/imageT"+T+"_"+i+"_aligned.tif");
				img.close();
			//	VitimageUtils.imageChecking(img,"Initial image : ind"+i+"-T"+T);
				//VitimageUtils.imageCheckingFast(result,"Cutting resampled along Z axis: ind"+i+"-T"+T);
			}
		}
			
		
		for(int i=1;i<8;i++) {
			String path="/home/fernandr/Bureau/Test/testMasks/imageRX_"+i+".tif";
			System.out.println("\n\nTest sur image RX T"+i+" :\n"+path);
			ImagePlus img=IJ.openImage(path);
			ImagePlus result=detectAxisAndSetItVertical(img,AcquisitionType.RX); 
			IJ.save(result,"/home/fernandr/Bureau/Test/testMasks/imageRX_"+i+"_aligned.tif");
			img.close();
			//	VitimageUtils.imageChecking(img,"Initial image : ind"+i+"-T"+T);
				//VitimageUtils.imageCheckingFast(result,"Cutting resampled along Z axis: ind"+i+"-T"+T);
		}
		
	}
	
	
	
	
	public static void testOutlineAndInocPoint(){		
		for(int i=18;i<31;i++) {
			for(int T=1;T<2;T++) {
				String path="/home/fernandr/Bureau/Test/testMasks/imageT"+T+"_"+i+"_aligned.tif";
				System.out.println("\n\nTest sur image T"+T+" :\n"+path);
				ImagePlus img=IJ.openImage(path);
				Point3d[]pFin=VitimageUtils.detectInoculationPointGuidedByAutomaticComputedOutline(img, VitimageUtils.smoothContourOfPlant(img, 20));
			img.close();
			}
		}
	}

	

	public static ImagePlus areaOfPertinentComputation2 (ImagePlus img2){
		double voxVolume=VitimageUtils.getVoxelVolume(img2);
		int nbThreshObjects=100;
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*0.5,vX*0.5,vX*0.5);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		ImagePlus imgMask0=VitimageUtils.getBinaryMask(img,thresh);
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,255, nbThreshObjects*voxVolume,10E10, 26, 0,false);//L objet
		ImagePlus imgMask2=VitimageUtils.getBinaryMask(imgMask1,1);
		return imgMask2;
	}
	
	public static void testMask() {
		for(int i=0;i<10;i++) {
			for(int T=1;T<3;T++) {
				String path="/home/fernandr/Bureau/Test/testMasks/imageT"+T+"_"+i+".tif";
				System.out.println("\n\nTest sur image T"+T+" :\n"+path);
				ImagePlus img=IJ.openImage(path);
				ImagePlus areaCompute1=VitimageUtils.areaOfPertinentMRIMapComputation(img,MRI_T1_Seq.sigmaGaussMapInPixels);
				ImagePlus areaCompute2=areaOfPertinentComputation2(img);
				VitimageUtils.imageChecking(img,"Initial image : ind"+i+"-T"+T);
				VitimageUtils.imageChecking(areaCompute1,"Initial image : ind"+i+"-T"+T);
				VitimageUtils.imageChecking(areaCompute2,"Initial image : ind"+i+"-T"+T);
			}
		}
		
	}
	
	

	/*
	public static int[] detectCapillaryPositionOld(ImagePlus img2,int Z) {
		//Prepare data
		double vX=img2.getCalibration().pixelWidth;
		int zMax=img2.getStackSize();
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+2*sigma;

		//VitimageUtils.imageChecking(img2,"Img avant");
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*2,vX*2,vX*2);
		//VitimageUtils.imageChecking(img,"Filtered");
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );

		//Extraction de la composante connexe 2
		ImagePlus imgMask0=VitimageUtils.getBinaryMask(img,thresh);
		//VitimageUtils.imageChecking(imgMask0,"Binarized");
		//imgMask0.show();

		//Protection to ensure non-even connections at the top or the back of the image, due to bias in magnet field
		imgMask0.getStack().getProcessor(1).set(0);
		imgMask0.getStack().getProcessor(2).set(0);
		imgMask0.getStack().getProcessor(3).set(0);
		imgMask0.getStack().getProcessor(4).set(0);
		imgMask0.getStack().getProcessor(zMax).set(0);
		imgMask0.getStack().getProcessor(zMax-1).set(0);
		imgMask0.getStack().getProcessor(zMax-2).set(0);
		imgMask0.getStack().getProcessor(zMax-3).set(0);
		ImagePlus imgMask2=VitimageUtils.connexe(imgMask0,1,255, 0,10E10, 26, 2,false);//le cap
		//VitimageUtils.imageChecking(imgMask2,"Connected");
		IJ.run(imgMask2,"8-bit","");
		ImagePlus imgSlice=new Duplicator().run(imgMask2,Z,Z);
		//VitimageUtils.imageChecking(imgSlice,"Duplicated");

		//Retrait du bouzin pas beau
		IJ.run(imgSlice,"Erode","");
		IJ.run(imgSlice,"Erode","");
		//VitimageUtils.imageChecking(imgSlice,"Eroded");
		
		//Preparation seuillage
		IJ.setThreshold(imgSlice, 255,255);
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		Roi capArea=new ThresholdToSelection().convert(imgSlice.getProcessor());	
		rm.add(imgSlice, capArea, 0);							
		Rectangle rect=capArea.getFloatPolygon().getBounds();
		System.out.println("Capillary position detected is :"+(rect.getX() + rect.getWidth()/2.0)+" , "+
															 (rect.getY() + rect.getHeight()/2.0)+" , "+
														 	 Z);
		rm.close();
		return new int[] {(int) (rect.getX() + rect.getWidth()/2.0) , (int) (rect.getY() + rect.getHeight()/2.0) , Z , (int)rect.getWidth(),(int)rect.getHeight()};  
	}
*/
	
	
	public static int[] detectCapillaryPosition(ImagePlus img2,int Z) {
		int zMax=img2.getStackSize();
		int tailleMorpho=4;
		ImagePlus imgMask=areaOfPertinentComputation2(img2);
		IJ.run(imgMask,"8-bit","");
		//Protection to ensure non-even connections at the top or the back of the image, due to bias in magnet field
		imgMask.getStack().getProcessor(1).set(0);
		imgMask.getStack().getProcessor(2).set(0);
		imgMask.getStack().getProcessor(3).set(0);
		imgMask.getStack().getProcessor(4).set(0);
		imgMask.getStack().getProcessor(zMax).set(0);
		imgMask.getStack().getProcessor(zMax-1).set(0);
		imgMask.getStack().getProcessor(zMax-2).set(0);
		imgMask.getStack().getProcessor(zMax-3).set(0);
		for(int t =0;t<tailleMorpho;t++) IJ.run(imgMask,"Dilate","stack");
		for(int t =0;t<tailleMorpho;t++) IJ.run(imgMask,"Erode","stack");
		ImagePlus imgCap=VitimageUtils.connexe(imgMask,1,255,0,10E10, 6, 2,false);//Le cap
		IJ.run(imgCap,"8-bit","");

		//Mise en slice et Retrait du bouzin pas beau
		imgCap=new Duplicator().run(imgCap,Z,Z);
		IJ.run(imgCap,"Erode","");
		IJ.run(imgCap,"Erode","");
		
		
		//Preparation seuillage
		IJ.setThreshold(imgCap, 255,255);
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		Roi capArea=new ThresholdToSelection().convert(imgCap.getProcessor());	
		rm.add(imgCap, capArea, 0);							
		Rectangle rect=capArea.getFloatPolygon().getBounds();
		System.out.println("Capillary position detected is :"+(rect.getX() + rect.getWidth()/2.0)+" , "+
															 (rect.getY() + rect.getHeight()/2.0)+" , "+
														 	 Z);
		rm.close();
		return new int[] {(int) (rect.getX() + rect.getWidth()/2.0) , (int) (rect.getY() + rect.getHeight()/2.0) , Z , (int)rect.getWidth(),(int)rect.getHeight()};  
	}

	
	
	
	
	
	
	
	
	
	
	public static void testDetCapillary() {
		int tailleMorpho=4;
		//Test T1 et T2 seuillage à Mu+3*sigma		
		for(int i=1;i<31;i++) {
			for(int T=2;T<3;T++) {
				String path="/home/fernandr/Bureau/Test/testMasks/imageT"+T+"_"+i+".tif";
				System.out.println("\n\nTest sur image T"+T+" :\n"+path);
				ImagePlus img=IJ.openImage(path);
				//img1.show();
				//VitimageUtils.waitFor(10000);
	//			VitimageUtils.imageChecking(img,"Image initiale, test="+i+" T"+T);


			
				int[] coords=detectCapillaryPosition(img,20);				
				img.getProcessor().resetMinAndMax();
				ImagePlus img2=new Duplicator().run(img);
				IJ.run(img2,"8-bit","");
				ImagePlus capCenterImg=VitimageUtils.drawCircleInImage(img2, 0.7, coords[0], coords[1], coords[2],255);
				//VitimageUtils.detectInoculationPointGuidedByAutomaticComputedOutline(img,contourImg);
				
				ImagePlus res3=VitimageUtils.compositeOf(img,capCenterImg,"Capillary exact position");

				
				VitimageUtils.imageChecking(res3,18,23,2,"Capillary exact position",3,false);
				IJ.save(res3,"/home/fernandr/Bureau/Test/testMasks/Cap_"+i+".tif");
			}
		}
		
		
		
		
		
	}
	
	public static void testViewCap(){
		for(int i=26;i<27;i++) {
			for(int T=1;T<2;T++) {
				ImagePlus res3=IJ.openImage("/home/fernandr/Bureau/Test/testMasks/Cap_"+i+".tif");
				VitimageUtils.imageChecking(res3,18,23,2,"Cap_"+i,3,false);
				res3.show();
				VitimageUtils.waitFor(100000);
			}
		}
	}
	
	
	
	
	
}  
