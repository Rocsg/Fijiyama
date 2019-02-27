package com.vitimage;
import java.util.ArrayList;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
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
 * 1-CAPILLARY TOOL
 * 2-AXIS ALIGNMENT TOOL
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
	private final int NO_AUTO_CHOICE=-10;
	private final int TR3D_0_MANUAL_TR=0;
	private final int TR3D_1_AUTO_TR=1;
	private final int TR3D_2_MAT_TR=2;
	private final int TR3D_3_ALIGN_TR=3;
	private final int TR3D_4_MANCAP_TR=4;
	private final int TR3D_5_AUTOCAP_TR=5;
	private final String[]tr3DToolsStr= {"Tool 1-1 : Image transformation by #N registration matrices","Tool 1-2 : Image transformation by one matrix (for scripts) ", 
									 "Tool 1-3 : Matrix compositions tool","Tool 1-4 : Bouture Alignment along z axis","Tool 1-5 : User-assisted capillary removal","Tool 1-6 : Automated capillary removal"};
	
	
	public Vitimage_Toolbox(){
	}

	
	/**These two launching methods are facility that leads to the same behaviour.
	 * --> The "main" is the entry point when debugging (from eclipse for example)
	 * --> The "run" is the entry point when using it from ImageJ GUI
	 */	
	public static void main(String[] args) {
		Vitimage_Toolbox vitiTool=new Vitimage_Toolbox();
		vitiTool.runSpecificTesting();
	}	

	public void run(String arg) {
		int chosenTool=chooseToolUI();
		switch(chosenTool) {
			case TOOL_NOTHING:System.out.println("Nothing to do, then. See you !");break;
			case TOOL_0_TESTING:System.out.println("Testing mode !");runTesting();break;
			case TOOL_1_TRANSFORM_3D:System.out.println("Transform 3D !");runTransform3D(NO_AUTO_CHOICE);break;
			case TOOL_2_MRI_WATER_TRACKER:System.out.println("MRI Water tracker !");runMRIWaterTracker();break;
			case TOOL_3_MULTIMODAL_ASSISTANT:System.out.println("Multimodal timeseries assistant !");runMultimodalAssistant();break;
		}
	}

	private int chooseToolUI(){
	        GenericDialog gd= new GenericDialog("Select mode");
	        gd.addChoice("Tool ", toolsStr,toolsStr[0]);
			gd.showDialog();
	        if (gd.wasCanceled()) return -1;
	        else return (gd.getNextChoiceIndex());

	}



	/** REGRESSION TESTS
	 * TR3D_0_MANUAL_TR Regression test : level0=no difference, level1=salt and pepper of value1 (obviously conversion artifact)
	 * TR3D_1_AUTO_TR Regression test : give the same 
	 * 
	 * */
	private void runSpecificTesting() {
		TransformUtils trUt=new TransformUtils();
		new ImageJ();
		ImagePlus img1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_1-3_level1_ImgToAlign.tif");
		img1.show();
		runTransform3D(TR3D_3_ALIGN_TR);
		//		trUt.testResolve();
//		ImagePlus img1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/imgMoving.tif");
//		ImagePlus img2=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/imgRef.tif");
//		img1.show();
		
//Test de l'algo connexe
//		ImagePlus img1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_1-6_level0.tif");
//		img1.show();
//		trUt.testConnexe(img1,1058,500,6);
		
	}
	

	private void runTesting() {
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
	
	
	/** Tool 1
	 * */
	private void runTransform3D(int initChoice) {
		TransformUtils trUt	=new TransformUtils();
		int choice;
		ImagePlus []imgsOut;
		ImagePlus []imgTab;
		double [][] matTransforms;
		double [] matTransformFinal;
		Transform[] matricesToCompose;
		double [] outVoxSize;
		double [] inVoxSize;
		int [] outImgSize;
		String outUnit="mm";
		String titleMov,titleRef;
		
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
				imgTab=chooseMovingAndReferenceImagesUI();
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
				imgTab=chooseMovingAndReferenceImagesUI();
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
			case TR3D_3_ALIGN_TR:
				//Get the opened image
				imgTab=new ImagePlus[] {IJ.getImage()};
				titleMov=imgTab[0].getShortTitle();
				inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				outImgSize=chooseSizeUI(imgTab[0],"Output image size :",AUTOMATIC);
				if (imgTab[0]==null) {System.out.println("No image to be aligned here");return;}
				System.out.println("Selected image : "+imgTab[0].getShortTitle());

				//Wait for 4 points, given in real coordinates
				double[][]inoculationPointCoordinates=trUt.coordinatesForAlignmentTesting();//waitForPointsUI(4,imgTab[0],true);//
								
				//Compute the transformation, according to reference geometry, and the given 4 points
				matTransforms=trUt.computeTransformationForBoutureAlignment(imgTab[0],inoculationPointCoordinates);
						
				//Resample and show input image
				matTransformFinal=trUt.doubleBasisChange(inVoxSize,matTransforms[0],inVoxSize);
				imgsOut=trUt.reech3D(imgTab[0],matTransformFinal,outImgSize,titleMov+"_axis_aligned",false && getYesNoUI("Compute mask ?"));
				for(int  i=0;i<imgsOut.length;i++) {trUt.adjustImageCalibration(imgsOut[i],outVoxSize,outUnit);imgsOut[i].show();}
										
				//Propose to save the transformation
				if(false && getYesNoUI("Save the matrix and its inverse ?")) {
					saveMatrixUI(matTransforms[0],"Save transformation from moving to reference space ?",SUPERVISED,"","mat_"+titleMov+"_to_alignment",trUt);
					saveMatrixUI(matTransforms[1],"Save inverse transformation from reference to moving space ?",SUPERVISED,"","mat_alignment_to_"+titleMov,trUt);
				}
				
				//Propose to save the image
				if(false && getYesNoUI("Save the image (and the mask if so) ?")) {
					for(int i=0;i<imgsOut.length;i++) {saveImageUI(imgsOut[i],(i==0 ?"Registered image":"Mask image"),SUPERVISED,"",imgsOut[i].getTitle());}
				}
				
				break;
			case TR3D_4_MANCAP_TR:System.out.println("Not implemented yet");break;
			case TR3D_5_AUTOCAP_TR:System.out.println("Not implemented yet");break;
		}
	}

	/** Tool 2*/
	private void runMRIWaterTracker() {
		
	}

	/** Tool 3*/
	private void runMultimodalAssistant() {
		
	}

	
	/** Utility functions for tool 1*/
	public ImagePlus[] chooseMovingAndReferenceImagesUI() {
			String none="*None*";
			int index1,index2;
			int[] wList = WindowManager.getIDList();
	        if (wList==null) {IJ.error("No images are open.");return null;}
	        String[] titles = new String[wList.length];
	        for (int i=0; i<wList.length; i++) {
	        	ImagePlus imp = WindowManager.getImage(wList[i]);
	            titles[i] = imp!=null?imp.getTitle():"";
	        }
	 
	        GenericDialog gd= new GenericDialog("Choose moving image and reference image\n\n");
	        gd.addChoice("Moving image", titles,none);
	        gd.addChoice("Reference image", titles,none);
			gd.showDialog();
	        if (gd.wasCanceled()) return null;
	       	index1 = gd.getNextChoiceIndex();
	       	index2 = gd.getNextChoiceIndex();
	        System.out.println("Chosen moving image : "+WindowManager.getImage(wList[index1]).getShortTitle());
	        System.out.println("Chosen reference image : "+WindowManager.getImage(wList[index2]).getShortTitle());
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
	
	
	
	/** Utility functions for tool 2*/

	
	
	/** Utility functions for tool 3*/

	
}

	
	
	
	
	
	
	
	
	
	
	
