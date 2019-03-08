package com.vitimage;
import ij.IJ;
import ij.plugin.filter.Binary;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
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
import ij.plugin.RGBStackMerge;

import java.awt.FileDialog;
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
 * 1-CAPILLARY TOOL --> ok
 * 2-AXIS ALIGNMENT TOOL  --> verify if iterate upon the tool improve the result. If so --> weird...
 * 3-MRI COMPUTATION --> T1 and T2 computation
 * 
 * @author Romain Fernandez
 *
 */



public class Vitimage_Toolbox implements PlugIn {
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
		
	/**The two launching methods are facility that leads to the same behaviour (i.e. the third method).
	 * --> The "main" is the entry point when debugging (from eclipse for example)
	 * --> The "run" is the entry point when using it from ImageJ GUI
	 */	
	public Vitimage_Toolbox(){
		this.anna=new Analyze("Vitimage toolbox");
	}
	public static void main(String[] args) {
		//new ImageJ();
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
	
	
	
	/** Regression tests
	 * TR3D_0_MANUAL_TR Regression test : level0=no difference, level1=salt and pepper of value1 (obviously conversion artifact)
	 * TR3D_1_AUTO_TR Regression test : give the same 
	 * 
	 * */
	@SuppressWarnings("unused")
	private void runSpecificTesting() {

		ImagePlus imgT1_J70=IJ.openImage("/home/fernandr/Bureau/Test/ITK/4_INTERTIME/T1_J70_to_ref.tif");
		ImagePlus imgT1_J35=IJ.openImage("/home/fernandr/Bureau/Test/ITK/4_INTERTIME/T1_J35_to_ref.tif");
		ImagePlus imgT1_J0_600=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_600_32bit.tif");
		ImagePlus imgT1_J0_1200=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
		ImagePlus imgT1_J0_2400=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
		ImagePlus imgT2_J0_10000=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T2_J0_10000_32bit.tif");
		ImagePlus imgT2_J0_10000_rot=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T2_J0_10000_32bit_rotate_3degZ.tif");
		int levelMin=2;
		int levelMax=5;
		boolean coarsestDouble=true;
		double basis=2;
		int []levelsMin=new int[] {0,2,0};
		int []levelsMax=new int[] {0,5,0};
//		registerImages(imgT1_J0_2400,imgT2_J0_10000,levelsMin,levelsMax,true,2,4,5,100,true);
		registerImages(imgT1_J35,imgT1_J70,levelsMin,levelsMax,true,2,4,2,100,false);
			
		/*
		
		if(false){//Test create sampling factors
			int levelMin=2;
			int levelMax=6;
			boolean coarsestDouble=true;
			double basis=2;
			int []levelsMin=new int[] {3,3,3};
			int []levelsMax=new int[] {5,5,5};
			int[]tabDims=new int[] {128,128,40};
			double voxSizeInit=1;
			int []tabSampl=subSamplingFactorsAtSuccessiveLevels(levelMin,levelMax,coarsestDouble,basis);
			int [][]tabSuccDims=imageDimsAtSuccessiveLevels(tabDims,tabSampl);
			double []tabSig=sigmaFactorsAtSuccessiveLevels(voxSizeInit,tabSampl);
	
			System.out.println("Test avec levelMin="+levelMin+" levelMax="+levelMax+" doubleIterationsAtCoarsestLevels="+coarsestDouble+" basis="+basis);
			System.out.println( TransformUtils.stringVectorN(tabSampl, "Tab Subsample Factors")   );
			System.out.println( TransformUtils.stringMatrixN( tabSuccDims, "Tab Successive Dims")   );
			System.out.println(  TransformUtils.stringVectorN( tabSig, "Tab Sigma Factors")  );
		*/		
		if(false) {//Test remove capillary
			ImageJ ij=new ImageJ();
			ImagePlus img1_J0_600=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_600_32bit.tif");
			ImagePlus img1_J0_1200=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
			ImagePlus img1_J0_2400=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
			ImagePlus img2_J0_10000=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T2_J0_10000_32bit.tif");
			ImagePlus imgInTest=img2_J0_10000;
			ImagePlus imgTest;
			imgTest=MRUtils.removeCapillary(imgInTest,false);
			imgInTest.show();
			imgTest.show();
		}
		
		if(false) {//Test MRI CUrve explorer
			ImagePlus img1=IJ.openImage("/home/fernandr/Bureau/Test/IRM/T1seq.tif");//T1_seq_aligned.tif
			ImagePlus img2=IJ.openImage("/home/fernandr/Bureau/Test/IRM/T2seq.tif");//T2_seq_aligned.tif
			MRICurveExplorerWindow explorer=new MRICurveExplorerWindow(img1,img2);
		}
		if(false) {//Test AutoAlign
			//System.out.println(TransformUtils.calculateAngle(0,10));//
//EASYImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
//MEDIUMImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/RX_J218_to_ref.tif");
//HARD		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1_echo1_axis_aligned.tif");
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/RX_J210.tif");//Official test
			img.show();
			
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/T1_J35.tif");//Official test
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/ge3D_J35.tif");//Official test OK
			//			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/T2_J35.tif");//Official test
			anna.notifyAlgo("Lancement alignement image","Pour la horde !","");
			anna.notifyStep("Ouverture image entree","RX_J210.tif");
			anna.storeImage(img,"Image test");
			runTransform3D(TR3D_4_AUTOALIGN_TR);
		}
		
		if(false) {//Test detection inoculation point
			//ImageJ thisImageJ=new ImageJ();
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1_echo1_axis_aligned.tif");
			ImagePlus imgT1=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
			//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/RX_J218_to_ref.tif");
			imgT1.show();
			ImagePlus imgTT2 = new Duplicator().run(imgT1, 1,imgT1.getStackSize());
			new TransformUtils().detectInoculationPoint(imgTT2);
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
			ImagePlus im1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_1-4_capToRemove.tif");
			im1.show();
			ImagePlus im2=mrUt.removeCapillary(im1,true);
			im2.show();
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
		TransformUtils trUt	=new TransformUtils();
		int choice;
		ImagePlus imgComp;
		ImagePlus []imgsOut;
		ImagePlus []imgTab;
		Transform[] matricesToCompose;
		double [][] matTransforms;
		double [][] matTransforms2;
		double [] matTransformFinal;
		double [] outVoxSize;
		double [] inVoxSize;
		int [] outImgSize;
		double[][]inoculationPointCoordinates=null;
		String outUnit="mm";
		String titleMov,titleRef;
		if(initChoice==NO_AUTO_CHOICE) {
			GenericDialog gd= new GenericDialog("Transform 3D tools");
	        gd.addChoice("Transform 3D mode ", tr3DToolsStr,tr3DToolsStr[0]);
			gd.showDialog();
			if (gd.wasCanceled()) {
				choice=-1;
				IJ.log("Processus annulé. Fin.");
			}
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
				anna.notifyAlgo("TR3D 0", "Alignement d'une image avec une autre", "");
				anna.storeEntryImage(imgTab[0],"Image flottante");
				anna.storeEntryImage(imgTab[1],"Image reference ");
				imageChecking(imgTab[0],"Verification image flottante (qui va être alignée)",2);
				imageChecking(imgTab[1],"Verification image reference",2);
				imgComp=compositeOf(imgTab[0],imgTab[1]);
				imageChecking(imgComp,"Inspection de la qualité de superposition avant recalage.",4);
				titleMov=imgTab[0].getShortTitle();
				titleRef=imgTab[1].getShortTitle();
				outImgSize=chooseSizeUI(imgTab[1],"Output image size :",SUPERVISED);
				outVoxSize=chooseVoxSizeUI(imgTab[1],"Output voxel size :",SUPERVISED);
				inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				matricesToCompose=chooseMatricesUI("Matrix path from moving to reference",SUPERVISED,trUt);

				matTransforms=trUt.composeMatrices(matricesToCompose);
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],outVoxSize);

				imgsOut=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_registered on_"+titleRef,getYesNoUI("Compute mask ?"));
				for(int  i=0;i<imgsOut.length;i++) {
					trUt.adjustImageCalibration(imgsOut[i],outVoxSize,outUnit);
					anna.storeImage(imgsOut[i],"Image sortie de reech3D");
					imageChecking(imgsOut[i],(i==0 ? "Image resultat de l algorithme" : "Masque de l image reechantillonnee"),2);
				}
				imgComp=compositeOf(imgsOut[0],imgTab[1]);
				imageChecking(imgComp,"Inspection de la qualité de superposition après recalage.",10);
				anna.rememberMatrix("Matrice flo vers ref",trUt.stringMatrix("",matTransforms[0]));
				anna.rememberMatrix("Matrice ref vers flo",trUt.stringMatrix("",matTransforms[1]));
				anna.storeImage(imgComp,"Image composite");
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
				matTransforms=trUt.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),false,inoculationPointCoordinates,false);
						
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
				
				//imageChecking(imgTab[0],imgTab[0].getStackSize()*0.3,imgTab[0].getStackSize()*0.7,2,"Image source for AUTO-ALIGN process",5);
				titleMov=imgTab[0].getShortTitle();
				inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outImgSize=chooseSizeUI(imgTab[0],"Output image size :",AUTOMATIC);
				if (imgTab[0]==null) {System.out.println("No image to be aligned here");return;}
				System.out.println("Selected image : "+imgTab[0].getShortTitle());

				/////Compute a first alignment along Z axis
				matTransforms= trUt.computeTransformationForBoutureAlignment(new Duplicator().run(imgTab[0]),true,null,true);
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],inVoxSize);
				ImagePlus []imgTempZ=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_axis_auto_aligned",false);
				trUt.adjustImageCalibration(imgTempZ[0],outVoxSize,outUnit);
				imgTempZ[0].getProcessor().setMinAndMax(0,8000);
				anna.storeImage(imgTempZ[0],"Image reechantillonnee apres aligment suivant Z");
				//Vitimage_Toolbox.imageChecking(imgTempZ[0],0,1000,4,"Premiere partie executee : axe aligné",4);
				
				
				/////Look for the inoculation point
				double[]inoculationPoint=trUt.detectInoculationPoint(imgTempZ[0]);
						
				matTransforms2=trUt.computeTransformationForBoutureAlignment(new Duplicator().run(imgTempZ[0]),false,new double[][] {inoculationPoint},true);
				//Resample and show input image
				matTransforms=trUt.composeMatrices(new Transform[] {trUt.arrayToTransform(matTransforms[0]),trUt.arrayToTransform(matTransforms2[0])});
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],outVoxSize);

				imgsOut=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_axis_and_IP_auto_aligned_",false);
				for(int  i=0;i<imgsOut.length;i++) {trUt.adjustImageCalibration(imgsOut[i],outVoxSize,outUnit);}
				imgsOut[0].getProcessor().setMinAndMax(0,8000);
				anna.storeImage(imgsOut[0],"Image reechantillonnee apres alignement suivant Z et suivant le point d'inoculation");
				resultTemp=imgsOut[0];
				System.out.println("Processus terminé");
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
		notYet();	
	}

	
	
	
	
	
	/** 
	 * UI interfaces for the tools
	 * */
	public ImagePlus[] chooseTwoImagesUI(String strGuess,String strImg1, String strImg2) {
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

	public Transform[] chooseMatricesUI(String strGuess,boolean autonomyLevel,TransformUtils trUt){
		boolean oneAgain=true;
		Transform transInd;
		Transform []transTab;
		int iTr=-1;
		ArrayList<Transform> tabRet = new ArrayList<Transform>();
		GenericDialog gd2;
		String pathSplit;
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
				pathSplit=(od.getPath());
				pathLog="";
				for(int i=0;i<iTr;i++)pathLog="--";
				pathLog=pathLog+"-> Tr "+iTr+" "+pathSplit;
				System.out.println(pathLog);				
            }	

			tabRet.add(transInd);
			
			gd2 = new GenericDialog("Encore une transformation ? (yes=encore une s'il te plait, no=chemin terminé)");
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
			String pathSave=path+""+title;
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
	
	public ImagePlus compositeOf(ImagePlus img1,ImagePlus img2){
	//	return(RGBStackMerge.mergeChannels(new ImagePlus[] {img1,img2},false));
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		return new ImagePlus("Composite",is);
	}
	
	public ImagePlus compositeOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}
	
	public static void notYet() {
		IJ.log("Cette fonctionnalité est en cours de développement.\nD'avance, nous vous remercions de votre patience.\nNos équipes font le meilleur chaque jour pour satisfaire vos besoins.\nLe cas échéant, vous pouvez envoyer une requête de fonctionnalité à \n\n    romainfernandez06@gmail.com");
	}
	  
	
	
	
	public org.itk.simple.Transform toItkTransform(imagescience.transform.Transform transIJ){
		return null;		
	}
	
	
	
	public void register(ImagePlus imgRef,ImagePlus imgFlo,org.itk.simple.Transform transformInit,int method) {
		
		//That should better match in a class like ITKRegistrationManager
		//And I should first pass all my code under the Transform way of ImageScience
	}
	
	
	
	
    /**
     * Tests to do :
     * Test the capability of this framework against Medinria performance. If so, let's go !
     */
	  

	 public void notifyOptimizerIteration() {
		 
	 }
	 
	 
	 public int nbTransformInComposite(org.itk.simple.Transform transfo) {
		 int nb=(transfo.toString().split("<<<<<<<<<<")[0].split(">>>>>>>>>").length-1);
		 if(nb==0)nb=1;
		 return nb;
	 }
	  
	 public String itkTransformToString(org.itk.simple.Transform transfo,String title) {
		 String ret="";
		 int nb=nbTransformInComposite(transfo);
		 ret=transfo.toString();
		 if(nb==1) {
			 return("Transformation "+title+"\nDe type transformation ITK à "+nb+" composante\n>>>>>>>> Composante 1 >>>>>>>>>>>>>>>>>>>>>>>>\n"+(itkTransfoStepToString(ret.substring(ret.indexOf("\n")))));
		 }
		 else {
			 String []tab1=ret.split("<<<<<<<<<<");
  			 String[] tabActives=(tab1[1].split("\n")[2]).split("[ ]{1,}");
 			 String[] transforms=tab1[0].split(">>>>>>>>>");
			 ret="Transformation "+title+"\nDe type transformation ITK à "+nb+" composantes\n";
			 for(int i=1;i<transforms.length;i++) {
				 ret+=">>>>>>>> Composante "+i+" ("+(tabActives[i].equals("1") ? "libre" : "fixée")+
					  ") >>>>>>>>>>>>>>>>\n"+itkTransfoStepToString(transforms[i]) ;
			 }
			 return ret;
		 }
	 }
	 
	 public String itkTransfoStepToString(String step){		 
		double[]mat= itkTransformStepToArray(step);
		String strType=(((step).split("\n"))[1]).split("[ ]{1,}")[1];
		if(strType.equals("Euler3DTransform")) {
			String angles=(((step).split("\n"))[21]).split(":")[1];
			strType+=angles;
		}
		return (TransformUtils.stringMatrix("Categorie geometrique : "+strType,mat));
	 }
	 
	 public double[] itkTransformStepToArray(String step) {
		String strInit=step;
		String []tabLign=strInit.split("\n");
		String detectIdentity=tabLign[1].split("[ ]{1,}")[1];
		if(detectIdentity.equals("IdentityTransform")) {
			return (new double[] {1,0,0,0,0,1,0,0,0,0,1,0});
		}
		else if(detectIdentity.equals("TranslationTransform")) {
			String valsTrans[]=tabLign[9].split("\\[")[1].split("\\]")[0].split(", ");
			return (new double[] {1,0,0,Double.valueOf(valsTrans[0]),0,1,0,Double.valueOf(valsTrans[1]),0,0,1,Double.valueOf(valsTrans[2])});
		}
		String []vals123=tabLign[10].split("[ ]{1,}");
		String []vals456=tabLign[11].split("[ ]{1,}");
		String []vals789=tabLign[12].split("[ ]{1,}");
		String []valsT=((((tabLign[13].split("\\[")[1]).split("\\]"))[0]).split(", "));
		return(new double[] {Double.parseDouble(vals123[1]),Double.parseDouble(vals123[2]),Double.parseDouble(vals123[3]),Double.parseDouble(valsT[0]),
				Double.parseDouble(vals456[1]),Double.parseDouble(vals456[2]),Double.parseDouble(vals456[3]),Double.parseDouble(valsT[1]),
				Double.parseDouble(vals789[1]),Double.parseDouble(vals789[2]),Double.parseDouble(vals789[3]),Double.parseDouble(valsT[2]) } );		
	 }
	 

	 public double[] itkTransformToArray(org.itk.simple.Transform transfo) {
		 double [][]ret;
		 double[] temp;
		 int nb=nbTransformInComposite(transfo);
		 String str=transfo.toString();
		 if(nb==1) {
			 return(itkTransformStepToArray(str.substring(str.indexOf("\n"))));
		 }
		 else {
			 String []tab1=str.split("<<<<<<<<<<");
 			 String[] transforms=tab1[0].split(">>>>>>>>>");
 			 double[][]tabMat=new double[transforms.length-1][12];
 			 for(int i=1;i<transforms.length;i++) {
				tabMat[i-1]=itkTransformStepToArray(transforms[i]) ;
			 }
 			 ret=TransformUtils.composeMatrices(tabMat);
			 return ret[0];
		 }
	 }

	 
	 
	 
 
	 
	 @SuppressWarnings("deprecation")
	public void testITK_V0(){ 
//			imgRegMethod.setMetricSamplingPercentage(percentageUsefulPixels);
			//		imgRegMethod.setOptimizerAsPowell();
			//		R.SetOptimizerAsGradientDescentLineSearch(learningRate=1,numberOfIterations=100)
			//		imgRegMethod.setOptimizerAsConjugateGradientLineSearch(0.05,numberOfIterations);

			//		imgRegMethod.setInitialTransform( new TranslationTransform( imgRef.getDimension() ) );
			//		imgRegMethod.setInitialTransform( new VersorRigid3DTransform() );
			//imgRegMethod.setInterpolator( InterpolatorEnum.sitkLinear );

			//		initialTransform = 
//			imgRegMethod.setInterpolator(InterpolatorEnum.sitkLinear);
		/**
		 * Successive used resolutions for pyramidal behaviour
		 */
		 int []dimsImg=new int[] {128,128,40};
		 
		int []tabShrink1= new int[] {8,8,4,4,2,1};
		double []tabSigma1= new double[] {4,4,2,2,1,0.5};// on the python tutorials, they say to use 2*shrink factor, but that seems heavy for our data
		int [][]tabSizes1=new int [tabShrink1.length][3];
		for(int i = 0 ;i<tabShrink1.length;i++)for(int j = 0 ;j<3;j++)tabSizes1[i][j]=(int)Math.ceil(1.0*dimsImg[j]/tabShrink1[i]);
		
		double []tabSigma2= new double[] {2,1};
		int []tabShrink2= new int[] {2,1};// on the python tutorials, they say to use 2*shrink factor, but that seems heavy for our data
		int [][]tabSizes2=new int [tabShrink2.length][3];
		for(int i = 0 ;i<tabShrink2.length;i++)for(int j = 0 ;j<3;j++)tabSizes2[i][j]=(int)Math.ceil(1.0*dimsImg[j]/tabShrink2[i]);
		
		 
		VectorUInt32  vectResolutionsStep1=new VectorUInt32(tabShrink1.length) ;
		VectorDouble vectSigmaStep1=new VectorDouble(tabShrink1.length);
		for(int i = 0 ;i<tabShrink1.length;i++) {
			vectResolutionsStep1.set(i,tabShrink1[i]);
			vectSigmaStep1.set(i,tabSigma1[i]);
		}

		VectorUInt32  vectResolutionsStep2=new VectorUInt32(tabShrink2.length) ;
		VectorDouble vectSigmaStep2=new VectorDouble(tabShrink2.length);
		for(int i = 0 ;i<tabShrink2.length;i++) {
			vectResolutionsStep1.set(i,tabShrink2[i]);
			vectSigmaStep2.set(i,tabSigma2[i]);
		}


		/**
		 * entry parameters
		 */	  
		double maxStep = 0.05;
		double minStep = 0.0000000001;
		int numberOfIterations = 40;
		double relaxationFactor = 0.5;
		long imageDimension=3;
		final int JOINT=1;
		final int CORRELATION=2;
		final int MATTES=3;
		final int MEANSQUARE=4;
		final int similarity=MATTES;
		
		//Banc d'essai : objet identique mais rotation grande (0.2,0.2,0.2): 3 et 4 marchent parfaitement, 1 est hyper pourri, et 2 fait un peu le taf
		//                mean square finit par decrocher avant MATTES, lorsqu'on ajoute en plus une translation
		//                enfin, MATTES finit par decrocher lorsqu'on augmente la rotation au dessus de 0.3 radiant suivant chaque axe.
		//
		//               Cependant les resultats de MATTES et MEAN SQUARE sont toujours impeccables lorsqu'on ne fait qu'un angle. JOINT reste
		//                 hyper pourri, et CORRELATION est bien
		String strImgRef="/home/fernandr/Bureau/Test/ITK/TestITKRef.tif";

		
		/**
		 * setup main actors
		 */	  
		ImageFileWriter imgW=new ImageFileWriter();
		ImageRegistrationMethod imgRegMethod = new ImageRegistrationMethod();	
		RescaleIntensityImageFilter imgRescale=new RescaleIntensityImageFilter();
		ResampleImageFilter resampler = new ResampleImageFilter();
		IterationUpdate cmd;
		ImageFileReader reader = new ImageFileReader();
		reader.setFileName(strImgRef);
		Image imgRef = reader.execute();
		resampler.setReferenceImage(imgRef);
		resampler.setDefaultPixelValue(0);

		/*reader = new ImageFileReader();
		reader.setFileName(strImgMov);
		Image imgMov = reader.execute();
		 */
		
		/**
		 * Transformation building for robustness testing
		 */	  
		double txTest=0;
		double tyTest=0;
		double tzTest=0;
		double rotXTest=0.0;
		double rotYTest=0.5;
		double rotZTest=0.0;
	
		VectorDouble vectTrans=new VectorDouble(3);
		vectTrans.set(0,txTest);
		vectTrans.set(1,tyTest);
		vectTrans.set(2,tzTest);
		TranslationTransform trans=new TranslationTransform(imageDimension, vectTrans);

		VectorDouble vectCenterEuler=new VectorDouble(3);
		vectCenterEuler.set(0,64);
		vectCenterEuler.set(1,64);
		vectCenterEuler.set(2,20);
		Euler3DTransform trans2=new Euler3DTransform();
		trans2.setCenter(vectCenterEuler);
		trans2.setRotation(rotXTest,rotYTest,rotZTest);
		trans2.setTranslation(vectTrans);
		System.out.println(trans2);
		resampler.setTransform(trans2);
		Image imgMov=resampler.execute(imgRef);
		imgW.execute(imgMov,"/home/fernandr/Bureau/Test/ITK/TestITKMov.tif", false);

	 
		


		/**
		 * Setup and execution of the rigid registration
		 */	  
		switch(similarity) {
			case JOINT:imgRegMethod.setMetricAsJointHistogramMutualInformation();break;
			case MEANSQUARE:imgRegMethod.setMetricAsMeanSquares();break;
			case CORRELATION:imgRegMethod.setMetricAsCorrelation();break;
			case MATTES:imgRegMethod.setMetricAsMattesMutualInformation();break;
		}
		
		imgRegMethod.setOptimizerAsRegularStepGradientDescent( maxStep,minStep,numberOfIterations,relaxationFactor);
//		imgRegMethod.setOptimizerAsGradientDescentLineSearch(0.1, numberOfIterations, 0.01, 10);
		Euler3DTransform initialTransform = new Euler3DTransform();
		imgRegMethod.setInitialTransform( initialTransform );
		System.out.println(itkTransformToString( initialTransform,"initialisée"));
		CenteredTransformInitializerFilter filtre=new CenteredTransformInitializerFilter();
		filtre.momentsOn();
		org.itk.simple.Transform preparedTransform=filtre.execute(imgRef,imgMov,initialTransform);
		System.out.println(itkTransformToString(preparedTransform,"preparee sur la base du centrage des moments"));
		imgRegMethod.setOptimizerScalesFromIndexShift();
		imgRegMethod.setShrinkFactorsPerLevel(vectResolutionsStep1);
		imgRegMethod.setSmoothingSigmasPerLevel(vectSigmaStep1);
		imgRegMethod.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
		imgRegMethod.setMetricSamplingStrategy(MetricSamplingStrategyType.REGULAR);
		imgRegMethod.setInitialTransform(preparedTransform);
		cmd = new IterationUpdate(imgRegMethod,tabShrink1,tabSizes1,tabSigma1);
		imgRegMethod.removeAllCommands();
		imgRegMethod.addCommand( EventEnum.sitkIterationEvent, cmd);
		org.itk.simple.Transform transfoStep1 = imgRegMethod.execute( imgRef, imgMov );
		System.out.println(itkTransformToString(transfoStep1,"resultat du recalage etape 1 rigide"));

		
		/**
		 * Setup and execution of an eventual second step
		  */
		preparedTransform.addTransform(new Euler3DTransform());
		imgRegMethod.setOptimizerAsRegularStepGradientDescent( maxStep,minStep,numberOfIterations,relaxationFactor);
//		imgRegMethod.setOptimizerAsGradientDescentLineSearch(1,100);
		imgRegMethod.setOptimizerScalesFromIndexShift();
		imgRegMethod.setShrinkFactorsPerLevel(vectResolutionsStep1);
		imgRegMethod.setSmoothingSigmasPerLevel(vectSigmaStep1);
		imgRegMethod.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
		imgRegMethod.setMetricSamplingStrategy(MetricSamplingStrategyType.REGULAR);
		imgRegMethod.setInitialTransform(preparedTransform);
		cmd = new IterationUpdate(imgRegMethod,tabShrink1,tabSizes1,tabSigma1);
		imgRegMethod.removeAllCommands();
		imgRegMethod.addCommand( EventEnum.sitkIterationEvent, cmd);
		org.itk.simple.Transform transfoStep2 = imgRegMethod.execute( imgRef, imgMov );
		System.out.println(itkTransformToString(transfoStep2,"resultat du recalage etape 2 rigide"));

		
		/**
		 * Display result informations
		 */	  
		System.out.println("-----------------------------------------");
		System.out.println("-----------------------------------------");
		System.out.println("------        FIN DU RECALAGE     -------");
		System.out.println("-----------------------------------------");
		System.out.format("Optimizer stop condition: %s\n", imgRegMethod.getOptimizerStopConditionDescription());
		System.out.format(" Iteration: %d\n", imgRegMethod.getOptimizerIteration());
		System.out.format(" Metric value: %f\n", imgRegMethod.getMetricValue());
		//transfoStep2.writeTransform("/home/fernandr/Bureau/Test/MATITK.txt");


		/**
		 * Apply the transformation to the moving image, and export result in a file
		 */	  
		resampler.setTransform(transfoStep1);
		Image imgResITKStep1=resampler.execute(imgMov);
		imgW.execute(imgResITKStep1,"/home/fernandr/Bureau/Test/ITK/TestITK_mov_to_ref_step_1.tif", false);

		
		resampler.setTransform(preparedTransform);
		Image imgResITKStep2=resampler.execute(imgMov);
		imgW.execute(imgResITKStep2,"/home/fernandr/Bureau/Test/ITK/TestITK_mov_to_ref_step_2.tif", false);

		new ImageJ();
		ImagePlus imRes1=IJ.openImage("/home/fernandr/Bureau/Test/ITK/TestITK_mov_to_ref_step_1.tif");
		ImagePlus imRes2=IJ.openImage("/home/fernandr/Bureau/Test/ITK/TestITK_mov_to_ref_step_2.tif");
		ImagePlus imRef=IJ.openImage("/home/fernandr/Bureau/Test/ITK/TestITKRef.tif");
		ImagePlus imMov=IJ.openImage("/home/fernandr/Bureau/Test/ITK/TestITKMov.tif");
		imRes1.getProcessor().setMinAndMax(0,255);
		imRes2.getProcessor().setMinAndMax(0,255);
		imRef.getProcessor().setMinAndMax(0,255);
		imMov.getProcessor().setMinAndMax(0,255);
		ImagePlus testAvant=compositeOf(imRef,imMov,"Position initiale");
		ImagePlus testApresStep1=compositeOf(imRef,imRes1,"Apres etape 1");
		ImagePlus testApresStep2=compositeOf(imRef,imRes2,"Apres etape 2");

		testAvant.show();
		testAvant.getWindow().setSize(500,500);
		testAvant.getCanvas().fitToWindow();
		testAvant.setSlice(20);

		testApresStep1.show();
		testApresStep1.getWindow().setSize(500,500);
		testApresStep1.getCanvas().fitToWindow();
		testApresStep1.setSlice(20);
		
		testApresStep2.show();
		testApresStep2.getWindow().setSize(500,500);
		testApresStep2.getCanvas().fitToWindow();
		testApresStep2.setSlice(20);
		/*		
	
	
			else {
	//			SimpleITK.show( imgMov, "Mov init", true );
				//		SimpleITK.show( imgResITKRef2, "Ref init", true );
				//SimpleITK.show( imgResITKMov3, "Mov final", true );
			}
		}
		*/
	 }

	
	 
	 public int[] subSamplingFactorsAtSuccessiveLevels(int levelMin,int levelMax,boolean doubleIterAtCoarsestLevels,double basis){
		if(levelMax<levelMin)levelMax=levelMin;
		if(levelMin<=0)return null;
		 //Compute factors : given min=1 max=2 , basis=10, i want {1,10}     given min=2, max=5, i want {10,100,1000,1000,10000,10000}; 
//		System.out.println("Parametres reçus : levelMin="+levelMin+" , levelMax="+levelMax+" doubles iteration at coarsest ? "+doubleIterAtCoarsestLevels);
//		System.out.println("Parametres amendés : levelMin="+levelMin+" , levelMax="+levelMax+" doubles iteration at coarsest ? "+doubleIterAtCoarsestLevels);
		int nbLevels=levelMax-levelMin+1;
		int nbDouble=doubleIterAtCoarsestLevels ? nbLevels-2 : 0;
		int nbTot=nbLevels+nbDouble;
		int []ret=new int[nbTot];
		for(int i=0;i<2 && i<nbTot;i++) {
			ret[nbTot-1-i]=(int)Math.round(Math.pow(basis,(levelMin-1+i)));
		}
		for(int i=0;i<(nbTot-2)/2;i++) {
			ret[nbTot-1-(2+i*2)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
			ret[nbTot-1-(2+i*2+1)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
		}
		return ret;
	}
	
	public int [][] imageDimsAtSuccessiveLevels(int []dimsImg,int []subFactors) {		
		if (subFactors==null)return null;
		int n=subFactors.length;
		int [][]ret=new int[n][3];
		for(int i=0;i<n;i++) for(int j=0;j<3;j++)ret[i][j]=(int)Math.ceil(dimsImg[j]/subFactors[i]);
		return ret;
	}
	
	// on the python tutorials, they say to use 2*shrink factor, but that seems heavy for our data. It depends on voxel size, thus we adapt here there
	public double[] sigmaFactorsAtSuccessiveLevels(double voxSizeInit,int[]subFactors,double rapportSigma) {
		if (subFactors==null)return null;
		double[]ret=new double[subFactors.length];
		for(int i=0;i<subFactors.length;i++)ret[i]=voxSizeInit*subFactors[i]*rapportSigma;
		return ret;
	}

	
	public Image imagePlusToItkImage(ImagePlus img) {
		IJ.save(img,"/home/fernandr/Temp/img.tif");	
		ImageFileReader reader = new ImageFileReader();
		reader.setFileName("/home/fernandr/Temp/img.tif");
		return(reader.execute());
	}
	
	public ImagePlus itkImageToImagePlus(Image img) {
		ImageFileWriter imgW=new ImageFileWriter();
		imgW.execute(img,"/home/fernandr/Temp/img.tif",false);
		return(IJ.openImage("/home/fernandr/Temp/img.tif"));
	}
	
	 
	 @SuppressWarnings("deprecation")
	public void registerImages(ImagePlus ijImgRef,ImagePlus ijImgMov,int []levelMin,int []levelMax,
								boolean doubleIterAtCoarsestLevels,double basis,int similarityUsed,
								double rapportSigma,int nbIter,boolean hasCapillary){
		 
		/**
		 * entry parameters
		 */	  
		double maxStep = 0.1;
		double minStep = 0.0000000001;
		int numberOfIterations = nbIter;
		double relaxationFactor = 0.5;
		long imageDimension=3;
		final int JOINT=1;
		final int CORRELATION=2;
		final int MATTES=3;
		final int MEANSQUARE=4;
		int similarity;
		if(similarityUsed <1 || similarityUsed>4)similarity=MATTES;
		else similarity=similarityUsed;

		/**
		 * Successive used resolutions for pyramidal behaviour
		 */ 
    	int []dimsImg=new int[] {ijImgRef.getWidth(),ijImgRef.getHeight(),ijImgRef.getStackSize()};
		double voxSizeInit=Math.min(ijImgRef.getCalibration().pixelWidth,ijImgRef.getCalibration().pixelDepth);	

		//Compute translations
		int []tabShrinkTrans= subSamplingFactorsAtSuccessiveLevels(levelMin[0],levelMax[0],doubleIterAtCoarsestLevels,basis);
		double []tabSigmaTrans= sigmaFactorsAtSuccessiveLevels(voxSizeInit,tabShrinkTrans,rapportSigma);
		int [][]tabSizesTrans=imageDimsAtSuccessiveLevels(dimsImg,tabShrinkTrans);		
		boolean  doTrans=(tabSizesTrans!=null);
		VectorUInt32 vectShrinkTrans=new VectorUInt32(1);
		VectorDouble vectSigmaTrans=new VectorDouble(1);
		if(doTrans) {
			vectShrinkTrans=new VectorUInt32(tabShrinkTrans.length) ;
			vectSigmaTrans=new VectorDouble(tabShrinkTrans.length);
			for(int i = 0 ;i<tabShrinkTrans.length;i++) {
				vectShrinkTrans.set(i,tabShrinkTrans[i]);
				vectSigmaTrans.set(i,tabSigmaTrans[i]);
			}
			int nbLevelsTrans=tabShrinkTrans.length;
		}
			
		//Compute rotations
		int []tabShrinkRot= subSamplingFactorsAtSuccessiveLevels(levelMin[1],levelMax[1],doubleIterAtCoarsestLevels,basis);
		double []tabSigmaRot= sigmaFactorsAtSuccessiveLevels(voxSizeInit,tabShrinkRot,rapportSigma);
		int [][]tabSizesRot=imageDimsAtSuccessiveLevels(dimsImg,tabShrinkRot);		
		boolean  doRot=(tabSizesRot!=null);
		VectorUInt32 vectShrinkRot=new VectorUInt32(1);
		VectorDouble vectSigmaRot=new VectorDouble(1);
		if(doRot) {
			vectShrinkRot=new VectorUInt32(tabShrinkRot.length);
			vectSigmaRot=new VectorDouble(tabShrinkRot.length);
			for(int i = 0 ;i<tabShrinkRot.length;i++) {
				vectShrinkRot.set(i,tabShrinkRot[i]);
				vectSigmaRot.set(i,tabSigmaRot[i]);
			}
			int nbLevelsRot=tabShrinkRot.length;
		}
		
		//Compute affine
		int []tabShrinkAff= subSamplingFactorsAtSuccessiveLevels(levelMin[2],levelMax[2],doubleIterAtCoarsestLevels,basis);
		double []tabSigmaAff= sigmaFactorsAtSuccessiveLevels(voxSizeInit,tabShrinkAff,rapportSigma);
		int [][]tabSizesAff=imageDimsAtSuccessiveLevels(dimsImg,tabShrinkAff);
		boolean  doAffine=(tabSizesAff!=null);
		VectorUInt32 vectShrinkAff=new VectorUInt32(1);
		VectorDouble vectSigmaAff=new VectorDouble(1);
		if(doAffine) {
			vectShrinkAff=new VectorUInt32(tabShrinkAff.length) ;	
			vectSigmaAff=new VectorDouble(tabShrinkAff.length);
			for(int i = 0 ;i<tabShrinkAff.length;i++) {
				vectShrinkAff.set(i,tabShrinkAff[i]);
				vectSigmaAff.set(i,tabSigmaAff[i]);
			}
			int nbLevelsAff=tabShrinkAff.length;
		}
		
		
	
		/**
		 * setup main actors
		 */	  

		VectorDouble vec=new VectorDouble (5);
		vec.set(0, 6);
		vec.set(1, 6);
		vec.set(2, 6);
		vec.set(3, 6);
		vec.set(4, 6);
		ImageRegistrationMethod imgRegMethod = new ImageRegistrationMethod();	
		imgRegMethod.setOptimizerScalesFromIndexShift();
		//imgRegMethod.setMetricSamplingPercentagePerLevel(vec); 		  
		imgRegMethod.setMetricSamplingStrategy(MetricSamplingStrategyType.REGULAR);
		imgRegMethod.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
		switch(similarity) {
			case JOINT:imgRegMethod.setMetricAsJointHistogramMutualInformation();break;
			case MEANSQUARE:imgRegMethod.setMetricAsMeanSquares();break;
			case CORRELATION:imgRegMethod.setMetricAsCorrelation();break;
			case MATTES:imgRegMethod.setMetricAsMattesMutualInformation();break;
		}
		imgRegMethod.setOptimizerAsRegularStepGradientDescent( maxStep,minStep,numberOfIterations,relaxationFactor);

		IterationUpdate cmd;
		Image imgRef = imagePlusToItkImage(ijImgRef);
		Image imgRefNoCap;
		Image imgMovNoCap;
		if(hasCapillary)imgRefNoCap=imagePlusToItkImage(MRUtils.removeCapillary(ijImgRef,false));
		else imgRefNoCap=imagePlusToItkImage(ijImgRef);
		Image imgMov = imagePlusToItkImage(ijImgMov);
		if(hasCapillary)imgMovNoCap=imagePlusToItkImage(MRUtils.removeCapillary(ijImgMov,false));
		else imgMovNoCap=imagePlusToItkImage(ijImgMov);
		ResampleImageFilter resampler = new ResampleImageFilter();
		resampler.setReferenceImage(imgRefNoCap);
		resampler.setDefaultPixelValue(0);
	
		
		/**
		 * Setup and execution of the Translation registration
		 */	  
		
		TranslationTransform initialTransform = new TranslationTransform(3);
		org.itk.simple.Transform globalTransform=new org.itk.simple.Transform(initialTransform);
		imgRegMethod.setInitialTransform(globalTransform);
		imgRegMethod.removeAllCommands();
		cmd = new IterationUpdate(imgRegMethod,tabShrinkTrans,tabSizesTrans,tabSigmaTrans);
		imgRegMethod.addCommand( EventEnum.sitkIterationEvent, cmd);
		if(doTrans) {
			imgRegMethod.setShrinkFactorsPerLevel(vectShrinkTrans);
			imgRegMethod.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
			imgRegMethod.setSmoothingSigmasPerLevel(vectSigmaTrans);
			imgRegMethod.execute( imgRefNoCap, imgMovNoCap );
		}
		System.out.println(itkTransformToString(globalTransform,"Transformation apres etape 1 (translation)"));
		resampler.setTransform(globalTransform);
		imgRegMethod.setOptimizerScalesFromIndexShift();
		Image imgResITKStep1=resampler.execute(imgMov);
		
		
		
		
		/**
		 * Setup and execution of the Rigid registration
		 */	  
		VersorRigid3DTransform rotationComponent = new VersorRigid3DTransform();
		if(!doTrans) {
			globalTransform=new org.itk.simple.Transform(rotationComponent);
		}
		else {
			CenteredTransformInitializerFilter filtre=new CenteredTransformInitializerFilter();
			filtre.momentsOn();
			globalTransform=filtre.execute(imgRef,imgMov,rotationComponent);
			//globalTransform.addTransform(rotationComponent);
		}
		imgRegMethod.setInitialTransform(globalTransform);
		imgRegMethod.removeAllCommands();
		cmd = new IterationUpdate(imgRegMethod,tabShrinkRot,tabSizesRot,tabSigmaRot);
		imgRegMethod.addCommand( EventEnum.sitkIterationEvent, cmd);
		if(doRot) {
			imgRegMethod.setShrinkFactorsPerLevel(vectShrinkRot);
			imgRegMethod.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
			imgRegMethod.setSmoothingSigmasPerLevel(vectSigmaRot);
			imgRegMethod.execute( imgRefNoCap, imgMovNoCap );
		}
		System.out.println(itkTransformToString(globalTransform,"Transformation apres etape 2 (rotation)"));
		resampler.setTransform(globalTransform);
		imgRegMethod.setOptimizerScalesFromIndexShift();
		Image imgResITKStep2=resampler.execute(imgMov);

		/**
		 * Setup and execution of the Rigid registration
		 */	  
		AffineTransform affineComponent = new AffineTransform(3);
		globalTransform.addTransform(affineComponent);
		imgRegMethod.setInitialTransform(globalTransform);
		imgRegMethod.removeAllCommands();
		cmd = new IterationUpdate(imgRegMethod,tabShrinkAff,tabSizesAff,tabSigmaAff);
		imgRegMethod.addCommand( EventEnum.sitkIterationEvent, cmd);
		if(doAffine) {
			imgRegMethod.setShrinkFactorsPerLevel(vectShrinkAff);
			imgRegMethod.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
			imgRegMethod.setSmoothingSigmasPerLevel(vectSigmaAff);
			imgRegMethod.execute( imgRefNoCap, imgMovNoCap );
		}
		System.out.println(itkTransformToString(globalTransform,"Transformation apres etape 3 (affine)"));
		resampler.setTransform(globalTransform);
		imgRegMethod.setOptimizerScalesFromIndexShift();
		Image imgResITKStep3=resampler.execute(imgMov);

		
		/**
		 * Display result informations
		 */	  
		System.out.println("-----------------------------------------");
		System.out.println("-----------------------------------------");
		System.out.println("------     End of registration    -------");
		System.out.println("-----------------------------------------");
		System.out.format("Optimizer stop condition: %s\n", imgRegMethod.getOptimizerStopConditionDescription());
		System.out.format(" Iteration: %d\n", imgRegMethod.getOptimizerIteration());
		System.out.format(" Metric value: %f\n", imgRegMethod.getMetricValue());


		/**
		 * Display images
		 */	  
		new ImageJ();
		ImagePlus imRef=itkImageToImagePlus(imgRef);
		ImagePlus imMov=itkImageToImagePlus(imgMov);
		ImagePlus imRes1=itkImageToImagePlus(imgResITKStep1);
		ImagePlus imRes2=itkImageToImagePlus(imgResITKStep2);
		ImagePlus imRes3=itkImageToImagePlus(imgResITKStep3);
		if(hasCapillary) {
			imMov.getProcessor().setMinAndMax(0,imMov.getProcessor().getMax());
			imRef.getProcessor().setMinAndMax(0,imRef.getProcessor().getMax());
			imRes1.getProcessor().setMinAndMax(0,imRes1.getProcessor().getMax());
			imRes2.getProcessor().setMinAndMax(0,imRes2.getProcessor().getMax());
			imRes3.getProcessor().setMinAndMax(0,imRes3.getProcessor().getMax());
		}
		else {
			imMov.getProcessor().setMinAndMax(0,255);
			imRef.getProcessor().setMinAndMax(0,255);
			imRes1.getProcessor().setMinAndMax(0,255);
			imRes2.getProcessor().setMinAndMax(0,255);
			imRes3.getProcessor().setMinAndMax(0,255);	
		}
		
		ImagePlus testAvant=compositeOf(imRef,imMov,"Position initiale");
		ImagePlus testApresStep1=compositeOf(imRef,imRes1,"Apres etape 1");
		ImagePlus testApresStep2=compositeOf(imRef,imRes2,"Apres etape 2");
		ImagePlus testApresStep3=compositeOf(imRef,imRes3,"Apres etape 3");

		testAvant.show();
		testAvant.setSlice(20);
		testApresStep1.show();
		testApresStep1.setSlice(20);
		testApresStep2.show();
		testApresStep2.setSlice(20);
		testApresStep3.show();
		testApresStep3.setSlice(20);
	 }
	 
	 
}  

class IterationUpdate  extends Command {

  private ImageRegistrationMethod m_Method;
  private ArrayList<Integer> listIterations=new ArrayList<Integer>();
  private ArrayList<Double> listScores=new ArrayList<Double>();
  private int [] tabShrink;
  private int  [][]tabSizes;
  private double[] tabSigma;
  private long memoirePyramide=1000;
  private double timeStamp1=0;
  private double timeStamp0=0;
  private double timeStampInit=0;
  private double durationIter=0;
  private double durationTot=0;
  private String memFinIter="";
  public IterationUpdate(ImageRegistrationMethod m,int []tabShrink,int[][]tabSizes,double []tabSigma) {
    super();
    this.tabSigma=tabSigma;
    this.tabShrink=tabShrink;
    this.tabSizes=tabSizes;
    m_Method=m;
    listIterations=new ArrayList<Integer>();
    listScores=new ArrayList<Double>();
    timeStamp1=0;
    timeStamp0=0;
    timeStampInit=0;
    durationTot=0;
    durationIter=0;
  }
  public void reset(ImageRegistrationMethod m) {
	   m_Method=m;
	   listIterations=new ArrayList<Integer>();
	   listScores=new ArrayList<Double>();
	   timeStamp1=0;
	    timeStamp0=0;
	    timeStampInit=0;
	    durationTot=0;	   
	    durationIter=0;	   
  }
  

  public double[]getLastValues(){
	  return new double[] {listIterations.get(listIterations.size()-1).doubleValue(),listScores.get(listScores.size()-1).doubleValue(),durationIter,durationTot};
  }
  
  public void execute() {
	  if(m_Method.getOptimizerStopConditionDescription() != null)memFinIter=m_Method.getOptimizerStopConditionDescription();
		  
	  if(m_Method.getOptimizerIteration()<memoirePyramide) {
		  int mem=(int) m_Method.getCurrentLevel();
		  String st="";
		  if(memoirePyramide<1000) {
			  for(int i=0;i<mem-1;i++)st+="     ";
			  st+="---------\n";
			  for(int i=0;i<mem-1;i++)st+="     ";
			  st+="Niveau "+(mem-1)+" terminé. Passage au niveau suivant\n\n\n\n";
			  for(int i=0;i<mem;i++)st+="     ";
		  }
		  st+="Niveau "+(mem+1)+" / "+tabShrink.length+" . Sigma lissage = "+(MRUtils.dou(tabSigma[mem]))+" mm . Facteur subsampling = "+tabShrink[(int) mem]+
				  " .  Taille images = "+tabSizes[mem][0]+" x "+tabSizes[mem][1]+" x "+tabSizes[mem][2]+" . Démarrage.\n";
		  for(int i=0;i<mem;i++)st+="     ";
		  st+="----------";		  
		  System.out.println(st);		  
	  }
	  memoirePyramide=m_Method.getOptimizerIteration();
	  String pyr="";
	  for(int i=1;i<=m_Method.getCurrentLevel();i++)pyr+="     ";
	  pyr+="Niveau "+(m_Method.getCurrentLevel()+1)+"  |  ";
	  if(timeStamp0==0)  timeStampInit=System.nanoTime()*1.0/1000000.0;
		timeStamp0=timeStamp1;
		timeStamp1=System.nanoTime()*1.0/1000000.0;
		durationIter=(timeStamp0 !=0 ? timeStamp1-timeStamp0 : 0);
		durationTot=(timeStamp0 !=0 ? timeStamp1-timeStampInit : 0)/1000.0;

		double durIter=(durationIter<1000 ? durationIter : durationIter/1000.0);
		String unitIter=(durationIter<1000 ? "ms" : "s ");
		double durTot=(durationTot<180 ? durationTot : durationTot/60);
		String unitTot=(durationTot<180 ? "s " : "mn");
		durationTot=(int)Math.round(timeStamp0 !=0 ? timeStamp1-timeStampInit : 0);
		System.out.format("%sRecalage sur %2d coeurs | Iteration %3d  | Score = %6.4f %%  |  Titer = %8.4f %s  |  Ttot = %5.2f %s  |\n",
				pyr,m_Method.getNumberOfThreads()
				,m_Method.getOptimizerIteration()
				,-100.0*m_Method.getMetricValue()
				, (float)durIter,unitIter
				, (float)durTot,unitTot
				);
		 
	 /* System.out.format("Optimisation recalage... Iteration %5d  |  Similarité = %10.5f %  |  Duree iteration = %10.5f ms  | Duree totale = %10.5f s |",
			  				m_Method.getOptimizerIteration(), -m_Method.getMetricValue()*100.0,  durationIter  , durationTot /1000.0
			  				);
       */      
//	  ("|% d|")
	  
	  
/*	
	System.out.format("Optimisation recalage... Iteration "+m_Method.getOptimizerIteration()
	+", duree totale="+durationTot+" ms");				  	  */
//	org.itk.simple.VectorDouble pos = m_Method.getOptimizerPosition();
//    pos.get(0), pos.get(1));
 }
  
  
    
	  
}
	
	
	
	/*
	 * Rab de code ITK
	 */
	

////////////FIXER LE CENTRE ARTIFICIELLEMENT /////////////
/*	int x=259;
int y=281;
int z=20;  centerFixed[0] = fixedOrigin[0] + fixedSpacing[0] ⋆ fixedSize[0] / 2.0;
centerFixed[1] = fixedOrigin[1] + fixedSpacing[1] ⋆ fixedSize[1] / 2.0;
centerMoving[0] = movingOrigin[0] + movingSpacing[0] ⋆ movingSize[0] / 2.0;
centerMoving[1] = movingOrigin[1] + movingSpacing[1] ⋆ movingSize[1] / 2.0;
initialTransform->SetCenter( centerFixed );
initialTransform->SetTranslation( centerMoving - centerFixed );
initialTransform->SetAngle( 0.0 );
registration->SetInitialTransform( initialTransform );
	VectorDouble vectDou=new VectorDouble(3);
	vectDou.set(0,259*0.0353);
	vectDou.set(1,281*0.0353);
	vectDou.set(2,10);
	rotationComponent.setCenter(vectDou);



	/////////////FIXER LE CENTRE NATIVEMENT.//////////////
	 * 	CenteredTransformInitializerFilter filtre=new CenteredTransformInitializerFilter();
	filtre.momentsOn();
	//globalTransform=filtre.execute(imgRef,imgMov,rotationComponent);

	 */
	

///ADAPTER LE LEARNING RATE
//Le baisser (taille des steps) pour le affine.




///FAIRE DU DEFORMABLE

