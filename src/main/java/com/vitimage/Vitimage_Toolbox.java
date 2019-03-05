package com.vitimage;
import ij.IJ;
import ij.plugin.filter.Binary;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import com.vitimage.MRUtils;
import imagescience.transform.Transform;
/**
 * The entry point for the whole toolbox. This toolbox provides three main tools :
 * 1) Transform 3D, the tools for image transformation, matrix composition, axis alignment, capillary substraction
 * 2) MRI Explorer, tool for curve fitting and modeling, T1, T2, M0 map extraction
 * 3) Multimodal time-series assistant, a tool to assist management of high throughput experiment on multi-modal-times series
 *
 * @author Romain Fernandez
 */

/**
 * TODO 
 * 1-CAPILLARY TOOL --> todo
 * 2-AXIS ALIGNMENT TOOL  --> make a sort upon volumes before release the connected components image
 * 3-MRI COMPUTATION
 * 
 * @author fernandr
 *
 */



public class Vitimage_Toolbox implements PlugIn {
	private final int TOOL_NOTHING=-1;
	private final int TOOL_0_TESTING=0;
	private final int TOOL_1_TRANSFORM_3D=1;
	private final int TOOL_2_MRI_WATER_TRACKER=2;
	private final int TOOL_3_MULTIMODAL_ASSISTANT=3;
	private final boolean SUPERVISED=false;
	private final boolean AUTOMATIC=true;
	private final String OS_SEPARATOR=System.getProperties().getProperty("file.separator");
	private final String[]toolsStr= {"TOOool 0 : testing","Tool 1 : Transform 3D ","Tool 2 : MRI Water tracker",
									 "Tool 2 : MRI Water tracker", "Tool 3 : Multimodal Timeseries assistant"};
	public static Analyze anna;
	private final int NO_AUTO_CHOICE=-10;
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
	
	private final String[]tr3DToolsStr= {"Tool 1-1 : Image transformation by #N registration matrices","Tool 1-2 : Image transformation by one matrix (for scripts) ", 
									 "Tool 1-3 : Matrix compositions tool","Tool 1-4 : Bouture Alignment along z axis","Tool 1-5 : User-assisted capillary removal",
									 "Tool 1-6 : Automated capillary removal"};
	private final String[]mriToolsStr= {"Tool 2-1 : MRI Explorer","Tool 2-2 : T1 sequence calculation", "Tool 2-3 : T2 sequence calculation"};
	
	
	public Vitimage_Toolbox(Analyze anna){
		this.anna=anna;
	}

	
	/**These two launching methods are facility that leads to the same behaviour.
	 * --> The "main" is the entry point when debugging (from eclipse for example)
	 * --> The "run" is the entry point when using it from ImageJ GUI
	 */	
	public static void main(String[] args) {
		Analyze anna=new Analyze("Vitimage toolbox");
		Vitimage_Toolbox vitiTool=new Vitimage_Toolbox(anna);
		vitiTool.runSpecificTesting();
		analyse();
	}	

	public void run(String arg) {
		//mrUt.runCurveExplorer();
/*		int chosenTool=chooseToolUI();
		switch(chosenTool) {
			case TOOL_NOTHING:System.out.println("Nothing to do, then. See you !");break;
			case TOOL_0_TESTING:System.out.println("Testing mode !");runTesting();break;
			case TOOL_1_TRANSFORM_3D:System.out.println("Transform 3D !");runTransform3D(NO_AUTO_CHOICE);break;
			case TOOL_2_MRI_WATER_TRACKER:System.out.println("MRI Water tracker !");runMRIWaterTracker(MRI_0_EXPLORER);break;
			case TOOL_3_MULTIMODAL_ASSISTANT:System.out.println("Multimodal timeseries assistant !");runMultimodalAssistant();break;
		}
		*/
	}

	private int chooseToolUI(){
	        GenericDialog gd= new GenericDialog("Select mode");
	        gd.addChoice("Tool ", toolsStr,toolsStr[0]);
			gd.showDialog();
	        if (gd.wasCanceled()) return -1;
	        else return (gd.getNextChoiceIndex());

	}

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
			System.out.println(anna.talkWithAnna(str,false));
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
	
	/** REGRESSION TESTS
	 * TR3D_0_MANUAL_TR Regression test : level0=no difference, level1=salt and pepper of value1 (obviously conversion artifact)
	 * TR3D_1_AUTO_TR Regression test : give the same 
	 * 
	 * */
	@SuppressWarnings("unused")
	private void runSpecificTesting() {

		
		
		if(false) {//Test imageChecking
			//ImageJ thisImageJ=new ImageJ();
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/RX_J218_to_ref.tif");
			imageChecking(img,200,250,4,"Test affichage de RX",2);
			img.show();
		}
		
		if(false) {//Test AutoAlign
			//System.out.println(TransformUtils.calculateAngle(0,10));//
//EASY			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
//MEDIUM			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/RX_J218_to_ref.tif");
//HARD			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1_echo1_axis_aligned.tif");
			
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/T1_J35.tif");//Official test
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/ge3D_J35.tif");//Official test OK
			//			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/T2_J35.tif");//Official test
			anna.notifyAlgo("Lancement alignement image","Pour la horde !","");
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/RX_J210.tif");//Official test
			anna.notifyStep("Ouverture image entree","RX_J210.tif");
			anna.storeImage(img,"Image test");
			img.show();
			runTransform3D(TR3D_4_AUTOALIGN_TR);
		}
		
		
		
		
		if(false) {//Test detection inoculation point
			//ImageJ thisImageJ=new ImageJ();
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1_echo1_axis_aligned.tif");
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/RX_J218_to_ref.tif");
			
			img.show();
			ImagePlus img2 = new Duplicator().run(img, 1,img.getStackSize());
			new TransformUtils().detectInoculationPoint(img2);
			//img3.show();
		
		
		}
		
		
		if(false) {//Test MRI Curve explorer
			//ImageJ thisImageJ=new ImageJ();
			ImagePlus imgT1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_2-1_T1.tif");
				imgT1.show();
				ImagePlus imgT2=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_2-1_T2.tif");
				imgT2.show();
				MRUtils mrUt=new MRUtils();
				new MRICurveExplorerWindow(imgT1,imgT2);
		}

		
		if(false) {//Test Remove capillary
			//ImageJ thisImageJ=new ImageJ();
			MRUtils mrUt=new MRUtils();
			ImagePlus img1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_1-4_capToRemove.tif");
			img1.show();
			ImagePlus img2=mrUt.removeCapillary(img1,true);
			img2.show();
		}

		if(false) {//Test manual Alignment  TODO : probleme, on dirait : mal aligné avec Z
			//ImageJ thisImageJ=new ImageJ();
			MRUtils mrUt=new MRUtils();
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
			imageChecking(img);
			img.show();
			runTransform3D(TR3D_3_MANALIGN_TR);
		}
	}
	
	public static void analyse(){
		Scanner sc = new Scanner(System.in);
		while(true) {
			String str = sc.nextLine();
			System.out.println(anna.talkWithAnna(str,false));
		}
	}
		
//FAIRE DU SEUILLAGE		
//		IJ.setAutoThreshold(img1, "Otsu dark stack");
//		ImagePlus imgSeuillee1=new ImagePlus("seuillee1",img1.createThresholdMask());
//		imgSeuillee1.show();

		
		
		
		
	//	waitFor(20000);
	//	closeEverything();
		
		
	

	private void testAsTheFutureBuild() {
		Class<?> clazz = Vitimage_Toolbox.class;
		String url = clazz.getResource(OS_SEPARATOR + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		new ImageJ();
		ImagePlus img1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/imgMoving.tif");
		ImagePlus img2=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/imgRef.tif");
		img1.show();
		//img2.show();
		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
		
	}
	
	/*
	/* Tool 1
	 * */
	private void runTransform3D(int initChoice) {
		TransformUtils trUt	=new TransformUtils();
		int choice;
		ImagePlus []imgsOut;
		ImagePlus []imgTab;
		double [][] matTransforms;
		double [][] matTransforms2;
		double [] matTransformFinal;
		Transform[] matricesToCompose;
		double [] outVoxSize;
		double [] inVoxSize;
		int [] outImgSize;
		String outUnit="mm";
		String titleMov,titleRef;
		double[][]inoculationPointCoordinates=null;
		if(initChoice==NO_AUTO_CHOICE) {
			GenericDialog gd= new GenericDialog("Transform 3D tools");
	        gd.addChoice("Transform 3D mode ", tr3DToolsStr,tr3DToolsStr[0]);
			gd.showDialog();
			if (gd.wasCanceled()) choice=-1;
	        else choice=gd.getNextChoiceIndex();
		}
		else choice=initChoice;
		// 0 choice (full manual) : Initial behaviour, ask for init and final espace, ask #matrices, ask for matrices, ask for output dimensions
		// 1 choice (full automatic) : Ask for init image, ask for a matrix, ask for output dimensions
		// 2 choice (matrix computation) : Ask for #matrices, matrices to compose, and output matrix file
		// 3 choice (bouture alignment) : Ask for a point, output the transformed image and the corresponding matrix
		// 4 choice (manual capillary removal) : ask for a point, output the transformed image
		// 5 choice (automatic capillary removal) : ask for a point, output the transformed image
		switch(choice) {
			case TR3D_0_MANUAL_TR:
				imgTab=chooseTwoImagesUI("Choose moving image and reference image\n\n","Moving image","Reference image");
				if(imgTab ==null)return;
				titleMov=imgTab[0].getShortTitle();
				titleRef=imgTab[1].getShortTitle();
				outImgSize=chooseSizeUI(imgTab[1],"Output image size :",SUPERVISED);
				outVoxSize=chooseVoxSizeUI(imgTab[1],"Output voxel size :",SUPERVISED);
				inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				matricesToCompose=chooseMatricesUI("Matrix path from moving to reference",SUPERVISED,trUt);

				matTransforms=trUt.composeMatrices(matricesToCompose);
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],outVoxSize);

				imgsOut=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_registered on_"+titleRef,getYesNoUI("Compute mask ?"));
				for(int  i=0;i<imgsOut.length;i++) {trUt.adjustImageCalibration(imgsOut[i],outVoxSize,outUnit);imgsOut[i].show();}
				if(getYesNoUI("Save the matrix and its inverse ?")) {
					saveMatrixUI(matTransforms[0],"Save transformation from moving to reference space ?",SUPERVISED,"","mat_"+titleMov+"_to_"+titleRef,trUt);
					saveMatrixUI(matTransforms[1],"Save inverse transformation from reference to moving space ?",SUPERVISED,"","mat_"+titleRef+"_to_"+titleMov,trUt);
				}
				if(getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}
				break;

			case TR3D_1_AUTO_TR:
				imgTab=chooseTwoImagesUI("Choose moving image and reference image\n\n","Moving image","Reference image");
				if(imgTab ==null)return;
				titleMov=imgTab[0].getShortTitle();
				titleRef=imgTab[1].getShortTitle();
				outImgSize=chooseSizeUI(imgTab[1],"Output image size :",AUTOMATIC);
				outVoxSize=chooseVoxSizeUI(imgTab[1],"Output voxel size :",AUTOMATIC);
				inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				matricesToCompose=chooseMatricesUI("Matrix path from moving to reference",AUTOMATIC,trUt);

				matTransforms=trUt.composeMatrices(matricesToCompose);
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],outVoxSize);

				imgsOut=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_registered on_"+titleRef,getYesNoUI("Compute mask ?"));
				for(int  i=0;i<imgsOut.length;i++) {trUt.adjustImageCalibration(imgsOut[i],outVoxSize,outUnit);imgsOut[i].show();}
				if(getYesNoUI("Save the matrix and its inverse ?")) {
					saveMatrixUI(matTransforms[0],"Save transformation from moving to reference space ?",SUPERVISED,"","mat_"+titleMov+"_to_"+titleRef,trUt);
					saveMatrixUI(matTransforms[1],"Save inverse transformation from reference to moving space ?",SUPERVISED,"","mat_"+titleRef+"_to_"+titleMov,trUt);
				}
				if(getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}
				break;
			case TR3D_2_MAT_TR:
				matricesToCompose=chooseMatricesUI("Matrix path from moving to reference",AUTOMATIC,trUt);
				matTransforms=trUt.composeMatrices(matricesToCompose);
				saveMatrixUI(matTransforms[0],"Save transformation from moving to reference space ?",SUPERVISED,"","mat_forward",trUt);
				saveMatrixUI(matTransforms[1],"Save inverse transformation from reference to moving space ?",SUPERVISED,"","mat_backward",trUt);
				
				break;
			case TR3D_3_MANALIGN_TR:
				//Get the opened image
				imgTab=new ImagePlus[] {IJ.getImage()};
				titleMov=imgTab[0].getShortTitle();
				inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outImgSize=chooseSizeUI(imgTab[0],"Output image size :",AUTOMATIC);
				if (imgTab[0]==null) {System.out.println("No image to be aligned here");return;}
				System.out.println("Selected image : "+imgTab[0].getShortTitle());

				//Wait for 4 points, given in real coordinates
				System.out.println("Waiting for the four points");
				inoculationPointCoordinates=waitForPointsUI(4,imgTab[0],true);
				System.out.println("I got them");
								
				//Compute the transformation, according to reference geometry, and the given 4 points
				matTransforms=trUt.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),false,inoculationPointCoordinates);
						
				//Resample and show input image
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],inVoxSize);
				imgsOut=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_axis_aligned",getYesNoUI("Compute mask ?"));
				for(int  i=0;i<imgsOut.length;i++) {trUt.adjustImageCalibration(imgsOut[i],outVoxSize,outUnit);imgsOut[i].show();}
										
				//Propose to save the transformation
				if(getYesNoUI("Save the matrix and its inverse ?")) {
					saveMatrixUI(matTransforms[0],"Save transformation from moving to reference space ?",SUPERVISED,"","mat_"+titleMov+"_to_local_reference",trUt);
					saveMatrixUI(matTransforms[1],"Save inverse transformation from reference to moving space ?",SUPERVISED,"","mat_local_reference_to_"+titleMov,trUt);
				}
				
				//Propose to save the image
				if(getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}
				
				break;
			case TR3D_4_AUTOALIGN_TR:
				//Get the opened image
				imgTab=new ImagePlus[] {IJ.getImage()};
				anna.notifyStep("Entree de case","");
				for(int i=0;i<imgTab.length ; i++)anna.storeImage(imgTab[i],"image "+i);
				anna.storeImage(imgTab[1],"image2");
				
				//imageChecking(imgTab[0],imgTab[0].getStackSize()*0.3,imgTab[0].getStackSize()*0.7,2,"Image source for AUTO-ALIGN process",5);
				titleMov=imgTab[0].getShortTitle();
				inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outImgSize=chooseSizeUI(imgTab[0],"Output image size :",AUTOMATIC);
				if (imgTab[0]==null) {System.out.println("No image to be aligned here");return;}
				System.out.println("Selected image : "+imgTab[0].getShortTitle());

				/////Compute a first alignment along Z axis
				matTransforms= trUt.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),true,null);
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],inVoxSize);
				ImagePlus []imgTempZ=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_axis_auto_aligned",false);
				trUt.adjustImageCalibration(imgTempZ[0],outVoxSize,outUnit);
				imgTempZ[0].getProcessor().setMinAndMax(0,8000);
				imgTempZ[0].show();
				//Vitimage_Toolbox.imageChecking(imgTempZ[0],0,1000,4,"Premiere partie executee : axe aligné",4);
				
				
				/////Look for the inoculation point
				double[]inoculationPoint=trUt.detectInoculationPoint(imgTempZ[0]);
						
				matTransforms2=trUt.computeTransformationForBoutureAlignment(new Duplicator().run(imgTempZ[0]),false,new double[][] {inoculationPoint});
				//Resample and show input image
				matTransforms=trUt.composeMatrices(new Transform[] {trUt.arrayToTransform(matTransforms[0]),trUt.arrayToTransform(matTransforms2[0])});
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],outVoxSize);

				imgsOut=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_axis_and_IP_auto_aligned_",false);
				for(int  i=0;i<imgsOut.length;i++) {trUt.adjustImageCalibration(imgsOut[i],outVoxSize,outUnit);imgsOut[i].show();}
				imgsOut[0].getProcessor().setMinAndMax(0,8000);
				Vitimage_Toolbox.imageChecking(imgsOut[0],0,1000,4,"Deuxieme partie executee : axe aligné",10);
				imgsOut[0].show();
				/*if(getYesNoUI("Save the matrix and its inverse ?")) {
					saveMatrixUI(matTransforms[0],"Save transformation from moving to reference space ?",SUPERVISED,"","mat_"+titleMov+"_to_local_reference",trUt);
					saveMatrixUI(matTransforms[1],"Save inverse transformation from reference to moving space ?",SUPERVISED,"","mat_local_reference_to_"+titleMov,trUt);
				}
				if(getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}
				*/
				break;
			case TR3D_5_MANCAP_TR:System.out.println("Not implemented yet");break;
			case TR3D_6_AUTOCAP_TR:System.out.println("Not implemented yet");break;
		}
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
				//mrUt.runCurveExplorer();
				break;

			case 10:
				int a=1;
				break;
		}
	}


	
	
	
	
	
	
	
	/** Tool 3*/
	private void runMultimodalAssistant() {
		
	}

	
	/** Utility functions for tool 1*/
	public ImagePlus[] chooseTwoImagesUI(String strGuess,String strImg1, String strImg2) {
			String none="*None*";
			int index1,index2;
			int[] wList = WindowManager.getIDList();
	        if (wList==null) {IJ.error("No images are open.");return null;}
	        String[] titles = new String[wList.length];
	        for (int i=0; i<wList.length; i++) {
	        	ImagePlus imp = WindowManager.getImage(wList[i]);
	            titles[i] = imp!=null?imp.getTitle():"";
	        }
	 
	        GenericDialog gd= new GenericDialog(strGuess);
	        gd.addChoice(strImg1, titles,none);
	        gd.addChoice(strImg2, titles,none);
			gd.showDialog();
	        if (gd.wasCanceled()) return null;
	       	index1 = gd.getNextChoiceIndex();
	       	index2 = gd.getNextChoiceIndex();
	        System.out.println("Chosen "+strImg1+" "+WindowManager.getImage(wList[index1]).getShortTitle());
	        System.out.println("Chosen "+strImg2+" "+WindowManager.getImage(wList[index2]).getShortTitle());
        	return new ImagePlus[] {WindowManager.getImage(wList[index1]),WindowManager.getImage(wList[index2])};
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

	public boolean getYesNoUI(String strGuess) {
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
		        gd.addNumericField("Dim_X suggestion :",tabRet[0], 5);
		        gd.addNumericField("Dim_Y suggestion :",tabRet[1], 5);
		        gd.addNumericField("Dim_Z suggestion :",tabRet[2], 5);
		        gd.showDialog();
		        if (gd.wasCanceled()) {System.out.println("Warning : Dims set by default 100.0 100.0 100.0"); return new int[] {100,100,100};}
		 
		        tabRet[0] = (int) gd.getNextNumber();
		        tabRet[1] = (int) gd.getNextNumber();
		        tabRet[2] = (int) gd.getNextNumber();
			 return tabRet;
		 }
	}

	
	public Transform[] chooseMatricesUI(String strGuess,boolean autonomyLevel,TransformUtils trUt){
		boolean oneAgain=true;
		Transform transInd;
		Transform []transTab;
		int iTr=-1;
		ArrayList<Transform> tabRet = new ArrayList<Transform>();
		GenericDialog gd2;
		String []pathSplit;
		String pathLog;
		
		do {
			iTr++;
			OpenDialog od=new OpenDialog("Select_transformation_#"+(iTr)+"");
			transInd=trUt.readMatrixFromFile(od.getPath(),true);
			if(autonomyLevel==SUPERVISED) {
				GenericDialog gdn= new GenericDialog("Confirm_transformation_#"+(iTr)+"");
 				gdn.addNumericField("R11"+iTr, transInd.get(0,0),5);
				gdn.addNumericField("R12"+iTr, transInd.get(0,1),5);
				gdn.addNumericField("R13"+iTr, transInd.get(0,2),5);
				gdn.addNumericField("Tx"+iTr, transInd.get(0,3),5);
				gdn.addNumericField("R21"+iTr, transInd.get(1,0),5);
				gdn.addNumericField("R22"+iTr, transInd.get(1,1),5);
				gdn.addNumericField("R23"+iTr, transInd.get(1,2),5);
				gdn.addNumericField("Ty"+iTr, transInd.get(1,3),5);
				gdn.addNumericField("R31"+iTr, transInd.get(2,0),5);
				gdn.addNumericField("R32"+iTr, transInd.get(2,1),5);
				gdn.addNumericField("R33"+iTr, transInd.get(2,2),5);
				gdn.addNumericField("Tz"+iTr, transInd.get(2,3),5);
		        gdn.showDialog();
		        if (gdn.wasCanceled()) return null;
				for(int j=0;j<12;j++)transInd.set(j/4,j%4,gdn.getNextNumber());
				pathSplit=(od.getPath()).split(OS_SEPARATOR);
				pathLog="";
				for(int i=0;i<iTr;i++)pathLog="--";
				pathLog=pathLog+"-> Tr "+iTr+" "+pathSplit[pathSplit.length-1];
				System.out.println(pathLog);				
            }	

			tabRet.add(transInd);
			
			gd2 = new GenericDialog("One again ?\n(yes=one more transformation, no=path is over)");
	        gd2.addMessage("One again ?");
	        gd2.enableYesNoCancel("Yes", "No");
	        gd2.showDialog();
	    	oneAgain=gd2.wasOKed();
		} while(oneAgain);
		iTr++;
		transTab=new Transform[iTr];
		for(int i=0;i<transTab.length;i++)transTab[i]=tabRet.get(i);
		return transTab;
	 }
		

	public void saveMatrixUI(double []matrix,String strGuess,boolean autonomyLevel,String path,String title,TransformUtils trUt) {
		if(autonomyLevel==AUTOMATIC) {
			String pathSave=path+OS_SEPARATOR+title;
			trUt.writeMatrixToFile(pathSave,trUt.arrayToTransform(matrix));
		}
		else {
			SaveDialog sd=new SaveDialog(strGuess,title,".mat");
			if(sd.getDirectory()==null ||  sd.getFileName()==null)return;
			String pathSave=sd.getDirectory()+""+sd.getFileName();
			trUt.writeMatrixToFile(pathSave,trUt.arrayToTransform(matrix));
		}
	}

	public void saveImageUI(ImagePlus img,String strGuess,boolean autonomyLevel,String path,String title) {
		if(autonomyLevel==AUTOMATIC) {
			String pathSave=path+OS_SEPARATOR+title;
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
		int nbP=0;
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
		IJ.run(img,"Fire","");
		if(sliceMin==sliceMax) {
			miniDuration=(int)Math.round(1000.0*totalDuration/periods);
			img.setSlice(sliceMin);
			for(int i=0;i<periods;i++)waitFor(miniDuration);
			return;
		}
		else {
			miniDuration=(int)Math.round(totalDuration*1000.0/periods/(sliceMax-sliceMin+10));
			int curSlice=(sliceMin+sliceMax)/2;
			img.show();
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
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),5);
	}
	
	/** Utility functions for tool 2*/

	
	
	/** Utility functions for tool 3*/

	
}

	
	
	
	
	
	
	
	
	
	
	
