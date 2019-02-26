package com.vitimage;
import java.io.File;
import java.util.ArrayList;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import imagescience.transform.Transform;

/**
 * The entry point for the whole toolbox. This toolbox provides three main tools :
 * 1) Transform 3D, the tools for image transformation, matrix composition, axis alignment, capillary substraction
 * 2) MRI Explorer, tool for curve fitting and modeling, T1, T2, M0 map extraction
 * 3) Multimodal time-series assistant, a tool to assist management of high throughput experiment on multi-modal-times series
 *
 * @author Romain Fernandez
 */





public class Vitimage_Toolbox implements PlugIn {
	private final int TOOL_NOTHING=-1;
	private final int TOOL_0_TESTING=0;
	private final int TOOL_1_TRANSFORM_3D=1;
	private final int TOOL_2_MRI_WATER_TRACKER=2;
	private final int TOOL_3_MULTIMODAL_ASSISTANT=3;
	private final boolean SUPERVISED=false;
	private final boolean AUTOMATIC=true;
	
	private final String[]toolsStr= {"TOOool 0 : testing","Tool 1 : Transform 3D ","Tool 2 : MRI Water tracker",
									 "Tool 2 : MRI Water tracker", "Tool 3 : Multimodal Timeseries assistant"};

	private final int TR3D_0_MANUAL_TR=0;
	private final int TR3D_1_AUTO_TR=1;
	private final int TR3D_2_MAT_TR=2;
	private final int TR3D_3_ALIGN_TR=3;
	private final int TR3D_4_MANCAP_TR=4;
	private final int TR3D_5_AUTOCAP_TR=5;
	private final String[]tr3DToolsStr= {"Tool 1-1 : Image transformation by #N registration matrices","Tool 1-2 : Image transformation by one matrix (for scripts) ",
									 "Tool 1-3 : Matrix compositions tool","Tool 1-4 : User-assisted capillary removal","Tool 1-5 : Automated capillary removal"};
	
	
	public Vitimage_Toolbox(){
	}

	
	/**These two launching methods are facility that leads to the same behaviour.
	 * --> The "main" is the entry point when debugging (from eclipse for example)
	 * --> The "run" is the entry point when using it from ImageJ GUI
	 */	
	public static void main(String[] args) {
		Vitimage_Toolbox vitiTool=new Vitimage_Toolbox();
		vitiTool.runTesting();
	}	

	public void run(String arg) {
		int chosenTool=chooseToolUI();
		switch(chosenTool) {
			case TOOL_NOTHING:IJ.log("Nothing to do, then. See you !");break;
			case TOOL_0_TESTING:IJ.log("Testing mode !");runTesting();break;
			case TOOL_1_TRANSFORM_3D:IJ.log("Transform 3D !");runTransform3D();break;
			case TOOL_2_MRI_WATER_TRACKER:IJ.log("MRI Water tracker !");runMRIWaterTracker();break;
			case TOOL_3_MULTIMODAL_ASSISTANT:IJ.log("Multimodal timeseries assistant !");runMultimodalAssistant();break;
		}
	}

	/**GUI that helps one to choose the tool to be used
	 */	
	private int chooseToolUI(){
	        GenericDialog gd= new GenericDialog("Select mode");
	        gd.addChoice("Tool ", toolsStr,toolsStr[0]);
			gd.showDialog();
	        if (gd.wasCanceled()) return -1;
	        else return (gd.getNextChoiceIndex());

	}




	private void runTesting() {
		Class<?> clazz = Vitimage_Toolbox.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		new ImageJ();
		ImagePlus img1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/imgMoving.tif");
		ImagePlus img2=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/imgRef.tif");
		img1.show();
		img2.show();
		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");

	}
	
	

	private void runTransform3D() {
		TransformUtils trUt	=new TransformUtils();
		int choice;
		ImagePlus imgOut;
		ImagePlus []imgTab;
		double [][] matTransforms;
		Transform[] matricesToCompose;
		double [] outVoxSize;
		int [] outImgSize;
		double [] inVoxSize;
		GenericDialog gd= new GenericDialog("Transform 3D tools");
        gd.addChoice("Transform 3D mode ", tr3DToolsStr,tr3DToolsStr[0]);
		gd.showDialog();
		if (gd.wasCanceled()) choice=-1;
        else choice=gd.getNextChoiceIndex();

		// 0 choice (full manual) : Initial behaviour, ask for init and final espace, ask #matrices, ask for matrices, ask for output dimensions
		// 1 choice (full automatic) : Ask for init image, ask for a matrix, ask for output dimensions
		// 2 choice (matrix computation) : Ask for #matrices, matrices to compose, and output matrix file
		// 3 choice (bouture alignment) : Ask for a point, output the transformed image and the corresponding matrix
		// 4 choice (manual capillary removal) : ask for a point, output the transformed image
		// 5 choice (automatic capillary removal) : ask for a point, output the transformed image
		switch(choice) {
			case TR3D_0_MANUAL_TR:
				  imgTab=chooseMovingAndReferenceImagesUI();
				  outImgSize=chooseSizeUI(imgTab[1],"Output image size :",SUPERVISED);
				  outVoxSize=chooseVoxSizeUI(imgTab[1],"Output voxel size :",SUPERVISED);
				  inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				  matricesToCompose=chooseMatricesUI("Matrix path from moving to reference",SUPERVISED,trUt);
				  matTransforms=trUt.composeMatrices(matricesToCompose);
				  matTransforms[0]=trUt.doubleBasisChange(inVoxSize,matTransforms[0],outVoxSize);
				  imgOut=trUt.reech3D(imgTab[0],matTransforms[0],outImgSize,imgTab[0].getShortTitle()+"_registered on_"+imgTab[1].getShortTitle());
			  	  imgOut.show();
				  break;
			
			case TR3D_1_AUTO_TR:
				  imgTab=chooseMovingAndReferenceImagesUI();
				  if(imgTab ==null)return;
				  outImgSize=chooseSizeUI(imgTab[1],"Output image size :",AUTOMATIC);
				  outVoxSize=chooseVoxSizeUI(imgTab[1],"Output voxel size :",AUTOMATIC);
				  inVoxSize=chooseVoxSizeUI(imgTab[0],"",AUTOMATIC);
				  matricesToCompose=chooseMatricesUI("Matrix path from moving to reference",AUTOMATIC,trUt);
				  matTransforms=trUt.composeMatrices(matricesToCompose);
				  matTransforms[0]=trUt.doubleBasisChange(inVoxSize,matTransforms[0],outVoxSize);
			  	  imgOut=trUt.reech3D(imgTab[0],matTransforms[0],outImgSize,imgTab[0].getShortTitle()+"_registered on_"+imgTab[1].getShortTitle());
			  	  imgOut.show();
			  	  break;
			  	  
			case TR3D_2_MAT_TR:IJ.log("Not implemented yet");break;
			case TR3D_3_ALIGN_TR:IJ.log("Not implemented yet");break;
			case TR3D_4_MANCAP_TR:IJ.log("Not implemented yet");break;
			case TR3D_5_AUTOCAP_TR:IJ.log("Not implemented yet");break;
		}
	}

	
	private void runMRIWaterTracker() {
		
	}

	private void runMultimodalAssistant() {
		
	}

	
	
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
	        IJ.log("Chosen moving image : "+WindowManager.getImage(wList[index1]).getShortTitle());
	        IJ.log("Chosen reference image : "+WindowManager.getImage(wList[index2]).getShortTitle());
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
		        if (gd.wasCanceled()) {IJ.log("Warning : vox sizes set by default 1.0 1.0 1.0"); return new double[] {1.0,1.0,1.0};}
		 
		        tabRet[0] = gd.getNextNumber();
		        tabRet[1] = gd.getNextNumber();
		        tabRet[2] = gd.getNextNumber();
			 return tabRet;
		 }
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
		        if (gd.wasCanceled()) {IJ.log("Warning : Dims set by default 100.0 100.0 100.0"); return new int[] {100,100,100};}
		 
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
				pathSplit=(od.getPath()).split("/");
				pathLog="";
				for(int i=0;i<iTr;i++)pathLog="--";
				pathLog=pathLog+"-> Tr "+iTr+" "+pathSplit[pathSplit.length-1];
				IJ.log(pathLog);				
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
		 
}

	
	
	
	
	
	
	
	
	
	
	
