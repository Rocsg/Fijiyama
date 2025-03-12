/*
 * 
 */
package io.github.rocsg.fijiyama.common;
import java.awt.Color;

import javax.swing.JFileChooser;

import org.scijava.vecmath.Color3f;
import org.scijava.java3d.Transform3D;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import ij.process.StackConverter;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import math3d.Point3d;

// TODO: Auto-generated Javadoc
/**
 * The Boolean int[] vectorUInt32ToIntArray(VectorUInt32 vect).
 */
public interface VitiDialogs {
	
	/** The vect. */
	public final static int NO_AUTO_CHOICE=-10;
	
	/** The Constant TOOL_NOTHING. */
	public final static int TOOL_NOTHING=-1;
	
	/** The Constant TOOL_1_TRANSFORM_3D. */
	public final static int TOOL_1_TRANSFORM_3D=0;
	
	/** The Constant TOOL_2_MRI_WATER_TRACKER. */
	public final static int TOOL_2_MRI_WATER_TRACKER=1;
	
	/** The Constant TOOL_3_MULTIMODAL_ASSISTANT. */
	public final static int TOOL_3_MULTIMODAL_ASSISTANT=2;
	
	/** The Constant TR3D_0_MANUAL_TR. */
	public final static int TR3D_0_MANUAL_TR=0;
	
	/** The Constant TR3D_1_AUTO_TR. */
	public final static int TR3D_1_AUTO_TR=1;
	
	/** The Constant TR3D_2_MAT_TR. */
	public final static int TR3D_2_MAT_TR=2;
	
	/** The Constant TR3D_3_MANALIGN_TR. */
	public final static int TR3D_3_MANALIGN_TR=3;
	
	/** The Constant TR3D_4_AUTOALIGN_TR. */
	public final static int TR3D_4_AUTOALIGN_TR=4;
	
	/** The Constant TR3D_5_MANCAP_TR. */
	public final static int TR3D_5_MANCAP_TR=5;
	
	/** The Constant TR3D_6_AUTOCAP_TR. */
	public final static int TR3D_6_AUTOCAP_TR=6;
	
	/** The Constant vect. */
	public final static int MRI_0_EXPLORER=0;
	
	/** The Constant MRI_1_T1_CALCULATION. */
	public final static int MRI_1_T1_CALCULATION=1;
	
	/** The Constant MRI_2_T2_CALCULATION. */
	public final static int MRI_2_T2_CALCULATION=2;	
	
	/** The Constant SUPERVISED. */
	public final static boolean SUPERVISED=false;
	
	/** The Constant AUTOMATIC. */
	public final static boolean AUTOMATIC=true;
	
	/** The Constant OS_SEPARATOR. */
	public final static String OS_SEPARATOR=System.getProperties().getProperty("file.separator");

	/**
	 * Gets the int UI.
	 *
	 * @param strGuess the str guess
	 * @param parameter the parameter
	 * @param value the value
	 * @return the int UI
	 */
	public static int getIntUI(String strGuess,String parameter,int value) {
		int ret=0;
		GenericDialog gd = new GenericDialog(strGuess);
		gd.addNumericField(parameter+" :",value,0);
        gd.showDialog();
        if (gd.wasCanceled()) {return value;}
 
	        ret = (int)Math.round(gd.getNextNumber());
		 return ret;
	 }
 
	/**
	 *  
	 * UI generic functions to access images.
	 *
	 * @param strGuess the str guess
	 * @param strImg1 the str img 1
	 * @return the image plus
	 */
	public static ImagePlus chooseOneImageUI(String strGuess,String strImg1) {
			ImagePlus imgRet;
			String open="Select this option to select image into a dialog";
			int index1;
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
			gd.showDialog();
	        if (gd.wasCanceled()) return null;
	       	index1 = gd.getNextChoiceIndex()-1;

	       	if(index1 < 0) {
	       		OpenDialog od1=new OpenDialog("Select "+strImg1);
	       		imgRet=IJ.openImage(od1.getPath());
	       	}
	       	else imgRet=WindowManager.getImage(wList[index1]);	      	   	    
        	return imgRet;
	}

	/**
	 * Choose one image UI path.
	 *
	 * @param strGuess the str guess
	 * @param strImg1 the str img 1
	 * @return the string
	 */
	public static String chooseOneImageUIPath(String strGuess,String strImg1) {
       		OpenDialog od1=new OpenDialog("Select "+strImg1);
       		return od1.getPath();
	}

	
	
	/**
	 * Choose one roi path UI.
	 *
	 * @param strGuess the str guess
	 * @param strImg1 the str img 1
	 * @return the string
	 */
	public static String chooseOneRoiPathUI(String strGuess,String strImg1) {
   		OpenDialog od1=new OpenDialog("Select "+strGuess);
   		return od1.getPath();
	}

	/**
	 * Choose two images UI.
	 *
	 * @param strGuess the str guess
	 * @param strImg1 the str img 1
	 * @param strImg2 the str img 2
	 * @return the image plus[]
	 */
	public static ImagePlus[] chooseTwoImagesUI(String strGuess,String strImg1, String strImg2) {
			ImagePlus[]imgRet=new ImagePlus[2];
			String open="* I will choose image in the explorer *";
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
	   	    
        	return imgRet;
	}
		

	/**
	 *  
	 * UI generic functions to ask simple data : image dimensions, voxel size, boolean yes/no , double or integer number, or String.
	 *
	 * @param img the img
	 * @param strGuess the str guess
	 * @param autonomyLevel the autonomy level
	 * @return the int[]
	 */
	public static int[] chooseSizeUI(ImagePlus img,String strGuess,boolean autonomyLevel) {
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

	/**
	 * Choose vox size UI.
	 *
	 * @param img the img
	 * @param strGuess the str guess
	 * @param autonomyLevel the autonomy level
	 * @return the double[]
	 */
	public static double[] chooseVoxSizeUI(ImagePlus img,String strGuess,boolean autonomyLevel) {
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

	/**
	 * Choose number UI.
	 *
	 * @param strGuess the str guess
	 * @param min the min
	 * @param max the max
	 * @param def the def
	 * @return the int
	 */
	public static int chooseNumberUI(String strGuess,int min,int max,int def) {
		GenericDialog gd = new GenericDialog(strGuess);
        gd.addNumericField("Choose a number :",def,6);
        gd.showDialog();
        int ans=0;
        if (gd.wasCanceled()) ans=0;
        else ans= (int)gd.getNextNumber();
        if(ans<min)ans=min;
        if(ans>max)ans=max;
        return ans;
	}
	
	/**
	 * Gets the double UI.
	 *
	 * @param strGuess the str guess
	 * @param parameter the parameter
	 * @param value the value
	 * @return the double UI
	 */
	public static double getDoubleUI(String strGuess,String parameter,double value) {
		double ret=0;
		GenericDialog gd = new GenericDialog(strGuess);
		gd.addNumericField(parameter+" :",value,1);
		
        gd.showDialog();
        if (gd.wasCanceled()) {return value;}
 
	        ret = gd.getNextNumber();
		 return ret;
	 }
	
	/**
	 * Gets the yes no UI.
	 *
	 * @param title the title
	 * @param strGuess the str guess
	 * @return the yes no UI
	 */
	public static boolean getYesNoUI(String title,String strGuess) {
        GenericDialog gd=new GenericDialog(title);
        gd.addMessage(strGuess);
        gd.enableYesNoCancel("Yes", "No");
        gd.showDialog();
    	return (gd.wasOKed());
	}

	/**
	 * Gets the string UI.
	 *
	 * @param strGuess the str guess
	 * @param parameter the parameter
	 * @param value the value
	 * @param forceStringToBeValidForFilenames the force string to be valid for filenames
	 * @return the string UI
	 */
	public static String getStringUI(String strGuess,String parameter,String value,boolean forceStringToBeValidForFilenames) {
		String ret="";
		do{
			GenericDialog gd = new GenericDialog(strGuess);
			gd.addStringField(parameter+" :",value,20);
	        gd.showDialog();
	        if (gd.wasCanceled()) {IJ.showMessage("Dialog cancel. Value is set to default : "+value);return value;}
	        ret=gd.getNextString();
		}
		while(!isStandardName(ret) && forceStringToBeValidForFilenames);
		return ret;
	}
	
	/**
	 * Temporary.
	 *
	 * @param strGuess the str guess
	 * @param value the value
	 * @return the int UI
	 */
	public static int getIntUI(String strGuess,int value) {
		return (int) IJ.getNumber(strGuess, value);
	}
	
	
	/**
	 *  
	 * UI generic functions to open other files : directories, roiset, transforms.
	 *
	 * @param strGuess the str guess
	 * @param strApproveButton the str approve button
	 * @return the string
	 */
	public static String chooseDirectoryNiceUI(String strGuess,String strApproveButton){
		if(true)return (IJ.getDirectory(strGuess));
		JFileChooser jf=new JFileChooser();
		jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jf.setDialogTitle(strGuess);
		jf.setApproveButtonText(strApproveButton);
		if (jf.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)return jf.getSelectedFile().getAbsolutePath(); 
		return null;
	}
	
	/**
	 * Choose directory UI.
	 *
	 * @param strGuess the str guess
	 * @param strApproveButton the str approve button
	 * @return the string
	 */
	public static String chooseDirectoryUI(String strGuess,String strApproveButton){
		if(true)return (IJ.getDirectory(strGuess));
		JFileChooser jf=new JFileChooser();
		jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		jf.setDialogTitle(strGuess);
		jf.setApproveButtonText(strApproveButton);
		if (jf.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)return jf.getSelectedFile().getAbsolutePath(); 
		return null;
	}

	
	/**
	 * Open mac file UI.
	 *
	 * @param strGuess the str guess
	 * @param dirName the dir name
	 * @param extension the extension
	 * @return the string
	 */
	public static String openMacFileUI(String strGuess, String dirName, String extension) {
		OpenDialog op=new OpenDialog(strGuess,dirName, extension);
		return op.getPath();
	}
	
	/**
	 * Open J file UI.
	 *
	 * @param strGuess the str guess
	 * @param dirName the dir name
	 * @param extension the extension
	 * @return the string
	 */
	public static String openJFileUI(String strGuess,String dirName,String extension){
		if(VitimageUtils.isMac()){
			System.out.println("Mac system detected. Mac routine opening file.");
			return openMacFileUI(strGuess,dirName,extension);
		}
		System.out.println("Not mac system detected. Not mac routine opening file.");

		
		JFileChooser jf;
		if(dirName!=null)jf=new JFileChooser(dirName);
		else jf=new JFileChooser();
		
		jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jf.setDialogTitle(strGuess);
		jf.setApproveButtonText("Choose this file");
		if (jf.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)return jf.getSelectedFile().getAbsolutePath(); 
		return null;
	 }

	/**
	 * Choose transforms UI.
	 *
	 * @param strGuess the str guess
	 * @param autonomyLevel the autonomy level
	 * @return the itk transform
	 */
	public static ItkTransform chooseTransformsUI(String strGuess,boolean autonomyLevel){
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
		return (globalTransform);
	 }
		
	/**
	 * Choose one transforms UI.
	 *
	 * @param strGuess the str guess
	 * @param path the path
	 * @param autonomyLevel the autonomy level
	 * @return the itk transform
	 */
	public static ItkTransform chooseOneTransformsUI(String strGuess,String path,boolean autonomyLevel){
		if(autonomyLevel==AUTOMATIC) return ItkTransform.readTransformFromFile(path);
		OpenDialog od=new OpenDialog(strGuess);
		IJ.log("Reading transform from file : "+od.getPath());
		return(ItkTransform.readTransformFromFile(od.getPath()));
	 }
		

	/**
	 *  
	 * UI generic functions to save transforms, images, or text in files.
	 *
	 * @param tr the tr
	 * @param strGuess the str guess
	 * @param autonomyLevel the autonomy level
	 * @param path the path
	 * @param title the title
	 * @param imgRef the img ref
	 */
	public static void saveDenseFieldTransformUI(ItkTransform tr,String strGuess,boolean autonomyLevel,String path,String title,ImagePlus imgRef){
		if(autonomyLevel==AUTOMATIC) {
			String pathSave=path+""+title;
			tr.writeAsDenseField(pathSave,imgRef);
		}
		else {
			SaveDialog sd=new SaveDialog(strGuess,title,".transform.tif");
			if(sd.getDirectory()==null ||  sd.getFileName()==null)return;
			String pathSave=sd.getDirectory()+""+sd.getFileName();
			tr.writeAsDenseField(pathSave,imgRef);
		}
	}
	
	/**
	 * Save matrix transform UI.
	 *
	 * @param tr the tr
	 * @param strGuess the str guess
	 * @param autonomyLevel the autonomy level
	 * @param path the path
	 * @param title the title
	 */
	public static void saveMatrixTransformUI(ItkTransform tr,String strGuess,boolean autonomyLevel,String path,String title){
		if(autonomyLevel==AUTOMATIC) {
			String pathSave=path+""+title;
			tr.writeMatrixTransformToFile(pathSave);
		}
		else {
			SaveDialog sd=new SaveDialog(strGuess,title,".txt");
			if(sd.getDirectory()==null ||  sd.getFileName()==null)return;
			String pathSave=sd.getDirectory()+""+sd.getFileName();
			tr.writeMatrixTransformToFile(pathSave);
		}
	}

	/**
	 * Save image UI.
	 *
	 * @param img the img
	 * @param strGuess the str guess
	 * @param autonomyLevel the autonomy level
	 * @param path the path
	 * @param title the title
	 */
	public static void saveImageUI(ImagePlus img,String strGuess,boolean autonomyLevel,String path,String title) {
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

	/**
	 * Save image UI path.
	 *
	 * @param strGuess the str guess
	 * @param title the title
	 * @return the string
	 */
	public static String saveImageUIPath(String strGuess,String title) {
		SaveDialog sd=new SaveDialog(strGuess,title,"");
		if(sd.getDirectory()==null ||  sd.getFileName()==null)return null;
		String pathSave=sd.getDirectory()+""+sd.getFileName();
		return pathSave;
	}

	
	
	/**
	 * Save text file UI.
	 *
	 * @param text the text
	 * @param strGuess the str guess
	 * @param suggestedDir the suggested dir
	 * @param title the title
	 * @param extension the extension
	 */
	public static void saveTextFileUI(String text,String strGuess,String suggestedDir,String title,String extension){
		SaveDialog sd=new SaveDialog(strGuess,title,extension);
		if(sd.getDirectory()==null ||  sd.getFileName()==null)return;
		String pathSave=sd.getDirectory()+""+sd.getFileName();
		VitimageUtils.writeStringInFile(text, pathSave);
	}
	

	
	
	
	/**
	 *  
	 * UI generic functions to open landmark perspective, with an image (or two) open, and waiting for the points to be selected.
	 *
	 * @param nbWantedPoints the nb wanted points
	 * @param img the img
	 * @param realCoordinates the real coordinates
	 * @return the double[][]
	 */
	public static double[][] waitForReferencePointsUI(int nbWantedPoints,ImagePlus img,boolean realCoordinates){
		double[][]tabRet=new double[nbWantedPoints][3];
		RoiManager rm=RoiManager.getRoiManager();
		ImagePlus temp=img.duplicate();
		temp.show();
		rm.reset();
		IJ.setTool("point");
		boolean finished =false;
		do {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(rm.getCount()==nbWantedPoints && getYesNoUI("","Confirm points ?"))finished=true;
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
			System.out.println("Got point number "+indP+" : {"+tabRet[indP][0]+","+tabRet[indP][1]+","+tabRet[indP][2]+"}");
		}
		temp.close();
		return tabRet;
	}

	/**
	 * Wait for points UI until click on slice 1.
	 *
	 * @param rx the rx
	 * @param mri the mri
	 * @param img2 the img 2
	 * @param realCoordinates the real coordinates
	 * @return the point 3 d[]
	 */
	public static Point3d[] waitForPointsUIUntilClickOnSlice1(ImagePlus rx,ImagePlus mri,ImagePlus img2,boolean realCoordinates){
		ImagePlus img=new Duplicator().run(img2);
		img.setTitle("waitForPointsUI");
		img.show();
		VitimageUtils.adjustImageCalibration(img, img2);
		double[][]tabRet=new double[50000][3];
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		boolean finished =false;
		int incr=0;
		int lastBeep=0;
		do {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			incr++;
			if(rm.getCount()>0 && rm.getRoi(rm.getCount()-1).getZPosition()==1)finished=true;
			if(rm.getCount()%2==0 && (rm.getCount() != lastBeep)) {
				lastBeep=rm.getCount();
			}
			if((incr%20) == 0)System.out.println("Waiting. Number of points : "+rm.getCount());
		}while (!finished);	
		for(int indP=0;indP<rm.getCount()-1;indP++){
			tabRet[indP][0]=rm.getRoi(indP).getXBase();
			tabRet[indP][1]=rm.getRoi(indP).getYBase();
			tabRet[indP][2]=rm.getRoi(indP).getZPosition();
			if(realCoordinates) {
				tabRet[indP][0]=(tabRet[indP][0])*(img.getCalibration().pixelWidth);
				tabRet[indP][1]=(tabRet[indP][1])*(img.getCalibration().pixelHeight);
				tabRet[indP][2]=(tabRet[indP][2])*(img.getCalibration().pixelDepth);
			}	
			System.out.println("Point retenu numéro "+indP+" : {"+tabRet[indP][0]+","+tabRet[indP][1]+","+tabRet[indP][2]+"}");
		}
		Point3d []ptRet=new Point3d[rm.getCount()-1];
		for(int i=0;i<rm.getCount()-1;i++)ptRet[i]=new Point3d(tabRet[i][0],tabRet[i][1],tabRet[i][2]);
		img.hide();
		return ptRet;
	}
		
	/**
	 * Wait for points UI.
	 *
	 * @param nbWantedPoints the nb wanted points
	 * @param img the img
	 * @param realCoordinates the real coordinates
	 * @return the double[][]
	 */
	public static double[][] waitForPointsUI(int nbWantedPoints,ImagePlus img,boolean realCoordinates){
		double[][]tabRet=new double[nbWantedPoints][3];
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		boolean finished =false;
		do {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(rm.getCount()==nbWantedPoints)finished=true;
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
			System.out.println("Got point number "+indP+" : {"+tabRet[indP][0]+","+tabRet[indP][1]+","+tabRet[indP][2]+"}");
		}
		return tabRet;
	}
	
	/**
	 * Registration points UI.
	 *
	 * @param nbWantedPointsPerImage the nb wanted points per image
	 * @param imgRef the img ref
	 * @param imgMov the img mov
	 * @param realCoordinates the real coordinates
	 * @return the point 3 d[][]
	 */
	public static Point3d[][] registrationPointsUI(int nbWantedPointsPerImage,ImagePlus imgRef,ImagePlus imgMov,boolean realCoordinates){
		System.out.println("Toto321");
		boolean isUserDefinedEnd=(nbWantedPointsPerImage==-1);
		if(isUserDefinedEnd)nbWantedPointsPerImage=1000000000;
		int squareSize=0;
		ImagePlus imgRefBis=imgRef.duplicate();
		ImagePlus imgMovBis=imgMov.duplicate();
		System.out.println("Toto322");

		imgRefBis.getProcessor().resetMinAndMax();
		imgMovBis.getProcessor().resetMinAndMax();
		if(isUserDefinedEnd){
			squareSize=imgRef.getWidth()/50;
			imgRefBis=VitimageUtils.drawRectangleInImage(imgRefBis, 0,0, squareSize, squareSize, 0);
		}
		System.out.println("Toto323");

		imgRefBis.show();
		imgMovBis.show();
		imgMovBis.setTitle("Moving image");
		imgRefBis.setTitle("Reference image");
		System.out.println("Toto324");
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		IJ.showMessage("Examine images and click on "+(nbWantedPointsPerImage*2)+" points to compute the correspondances,\n on both reference image and moving image (see image titles)"+
		"For each selected point, use the Roi Manager to save it, with \"add to manager\" option.\n Please follow the following order : "+
		"\n   Point 1 : item A on reference image\n    Point 2 : item A on moving image\n    Point 3 : item B on reference image\n Point 4 : item B on moving image \n...");
		boolean finished =false;
		do {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean finishCausedByUser=isUserDefinedEnd && rm.getCount()>0 && (rm.getRoi(rm.getCount()-1 ).getXBase()<squareSize)&& (rm.getRoi(rm.getCount()-1 ).getYBase()<squareSize);
			if(( rm.getCount()>=nbWantedPointsPerImage*2 || finishCausedByUser) && getYesNoUI("","Confirm points ?"))finished=true;
			System.out.println("Waiting "+(nbWantedPointsPerImage*2)+". Current number="+rm.getCount());
		}while (!finished);	
		int nCouples=Math.max((isUserDefinedEnd ? 0 : nbWantedPointsPerImage), rm.getCount()/2);
		Point3d []pRef=new Point3d[nCouples];
		Point3d []pMov=new Point3d[nCouples];
		for(int indP=0;indP<nCouples;indP++){
			pRef[indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			pMov[indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			if(realCoordinates) {
				pRef[indP]=TransformUtils.convertPointToRealSpace(pRef[indP],imgRef);
				pMov[indP]=TransformUtils.convertPointToRealSpace(pMov[indP],imgRef);				
			}	
		}
		imgRefBis.close();
		imgMovBis.close();
		return new Point3d[][] {pRef,pMov};
	}
	
	
	
	
	/**
	 *  
	 * Display a point detected on an image, and ask for confirmation.
	 *
	 * @param img the img
	 * @param suggestedInocPoint the suggested inoc point
	 * @return the point 3 d
	 */
	public static Point3d inspectInoculationPoint(ImagePlus img,Point3d suggestedInocPoint) {
		double ray=1;//in mm
		ImagePlus imgInspect=new Duplicator().run(img);
		Point3d pointCoordImage=TransformUtils.convertPointToImageSpace(suggestedInocPoint,imgInspect);
		imgInspect.getProcessor().resetMinAndMax();
		new StackConverter(imgInspect).convertToGray8();

		ImagePlus imgPoint=ij.gui.NewImage.createImage("point",img.getWidth(),img.getHeight(),img.getStackSize(),8,ij.gui.NewImage.FILL_BLACK);
		VitimageUtils.adjustImageCalibration(imgPoint,imgInspect);
		imgPoint=VitimageUtils.drawCircleInImage(imgPoint,ray,(int)Math.round(pointCoordImage.x),(int)Math.round(pointCoordImage.y),(int)Math.round(pointCoordImage.z),255);
		ImagePlus comp=VitimageUtils.compositeOf(imgInspect, imgPoint);
		VitimageUtils.imageChecking(comp, (int)Math.round(pointCoordImage.z)-comp.getStackSize()/10,
										  (int)Math.round(pointCoordImage.z)+comp.getStackSize()/10, 
									  	  3, "Suggested inoculation point", 5,false);
		comp.show();
		comp.setSlice((int)Math.round(pointCoordImage.z)+1);
		int secondsLast=15;
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		while(secondsLast-- > 0) {
			comp.setTitle("Change inoc point if necessary. "+secondsLast+" s left...");
			VitimageUtils.waitFor(1000);
		}
		if(rm.getCount()==0) {
			rm.close();
			comp.changes=false;
			comp.close();
			imgInspect.changes=false;
			imgInspect.close();		
			return suggestedInocPoint;
		}
		
		pointCoordImage=new Point3d(rm.getRoi(0 ).getXBase() , rm.getRoi(0).getYBase() ,  rm.getRoi(0).getZPosition());
		pointCoordImage=TransformUtils.convertPointToRealSpace(pointCoordImage,imgInspect);
		rm.close();
		comp.changes=false;
		comp.close();
		imgInspect.changes=false;
		imgInspect.close();
		return pointCoordImage;		
	}
	
	/**
	 * Inspect axis.
	 *
	 * @param img the img
	 * @param vectZ the vect Z
	 * @param ptOrigine the pt origine
	 * @param delayForReacting the delay for reacting
	 * @return the double[][]
	 */
	public static double[][] inspectAxis(ImagePlus img ,double[] vectZ,Point3d ptOrigine,int delayForReacting){
		double ray=1;//in mm
		ImagePlus imgInspect=new Duplicator().run(img);
		Point3d pointCoordImage=TransformUtils.convertPointToImageSpace(ptOrigine,imgInspect);
		imgInspect.getProcessor().resetMinAndMax();
		new StackConverter(imgInspect).convertToGray8();

		ImagePlus imgPoint=ij.gui.NewImage.createImage("point",img.getWidth(),img.getHeight(),img.getStackSize(),8,ij.gui.NewImage.FILL_BLACK);
		VitimageUtils.adjustImageCalibration(imgPoint,imgInspect);
		imgPoint=VitimageUtils.drawThickLineInImage(imgPoint,ray,(int)Math.round(pointCoordImage.x),(int)Math.round(pointCoordImage.y),(int)Math.round(pointCoordImage.z),vectZ);
		ImagePlus comp=VitimageUtils.compositeOf(imgInspect, imgPoint);
		VitimageUtils.imageCheckingFast(comp,  "Suggested axis");
		comp.show();
		comp.setSlice((int)Math.round(pointCoordImage.z)+1);
		int secondsLast=delayForReacting;
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		while(secondsLast-- > 0) {
			comp.setTitle("Change axis if necessary. Z-up then Z-down. "+secondsLast+" s left...");
			VitimageUtils.waitFor(1000);
		}
		if(rm.getCount()<2) {
			rm.close();
			comp.changes=false;
			comp.close();
			imgInspect.changes=false;
			imgInspect.close();		
			return new double[][]{vectZ,new double[] {0,0,0},new double[] {0,0,0}};
		}
		
		Point3d pointCoordImageUp=new Point3d(rm.getRoi(0 ).getXBase() , rm.getRoi(0).getYBase() ,  rm.getRoi(0).getZPosition());
		pointCoordImageUp=TransformUtils.convertPointToRealSpace(pointCoordImageUp,imgInspect);
		Point3d pointCoordImageDown=new Point3d(rm.getRoi(1 ).getXBase() , rm.getRoi(1).getYBase() ,  rm.getRoi(1).getZPosition());
		pointCoordImageDown=TransformUtils.convertPointToRealSpace(pointCoordImageDown,imgInspect);
		rm.close();
		comp.changes=false;
		comp.close();
		imgInspect.changes=false;
		imgInspect.close();
		return new double[][]{TransformUtils.normalize(new double[] { pointCoordImageUp.x - pointCoordImageDown.x , 
																pointCoordImageUp.y - pointCoordImageDown.y , 
																pointCoordImageUp.z - pointCoordImageDown.z } ), 
													new double[] { pointCoordImageUp.x , pointCoordImageUp.y , pointCoordImageUp.z } ,
													new double[] { pointCoordImageDown.x , pointCoordImageDown.y , pointCoordImageDown.z } };
		}

		
	/**
	 *  
	 * Helpers to check conformity of the String given by the user.
	 *
	 * @param c the c
	 * @return true, if is standard char
	 */
	//work like isAlphaNumeric, but including also underscore and minus
	public static boolean isStandardChar(char c) {
		int val=(int)c;
		if(val>=97 && val<=122)return true;//Character from a to z
		if(val>=65 && val<=90)return true;//Character from A to Z
		if(val>=48 && val<=57)return true;//Character from A to Z
		if(val==95 || val==45)return true;//Respectively, underscore and minus symbols
		return false;
	}
	
	/**
	 * Checks if is standard name.
	 *
	 * @param str the str
	 * @return true, if is standard name
	 */
	public static boolean isStandardName(String str) {
		if(str.length()<=0)return false;
		for (int i=0;i<str.length();i++)if (!isStandardChar(str.charAt(i)))return false;
		return true;
	}
	

	/**
	 * Not yet.
	 *
	 * @param strGuess the str guess
	 */
	public static void notYet(String strGuess) {
		IJ.showMessage("Not yet implemented : "+strGuess+"\nOur team constantly struggle to provide you new features, accordingly to you needs.\n"+
						"Please consider sending a feature request to : romainfernandez06@gmail.com");
	}
	
    /**
     * Manual registration in 3 D.
     *
     * @param imRef the im ref
     * @param imMov the im mov
     * @return the itk transform
     */
    public static ItkTransform manualRegistrationIn3D(ImagePlus imRef,ImagePlus imMov) {
    	ImagePlus imgRef=new Duplicator().run(imRef);
    	VitimageUtils.adjustImageCalibration(imgRef,imRef);
    	imgRef.setTitle("imgRef3D");
    	ImagePlus imgMov=new Duplicator().run(imMov);
    	VitimageUtils.adjustImageCalibration(imgMov,imMov);
    	ij3d.Image3DUniverse univ=new ij3d.Image3DUniverse();
		univ.show();
		univ.addContent(imgRef, new Color3f(Color.red),"imgRef",50,new boolean[] {true,true,true},1,0 );
		univ.addContent(imgMov, new Color3f(Color.green),"imgMov",50,new boolean[] {true,true,true},1,0 );
		ij3d.ImageJ3DViewer.select("imgRef");
		ij3d.ImageJ3DViewer.lock();
		ij3d.ImageJ3DViewer.select("imgMov");
		int iter=0;    	
		imgRef.show();
		VitiDialogs.getYesNoUI("","Red volume is fixed, green volume can move.\n Use the 3d viewer to adjust the green volume with the red volume using the mouse.\n Click-drag=rotate , Shift+Click-drag=translate.\nClose imgRef3D to confirm transformation is done");		
		while(WindowManager.getImage("imgRef3D")!=null) {
			VitimageUtils.waitFor(1000);
			System.out.print(iter+++" ");
		}
    	
    	Transform3D tr=new Transform3D();
		double[]tab=new double[16];
		univ.getContent("imgMov").getLocalRotate().getTransform(tr);
		tr.get(tab);
		ItkTransform itRot=ItkTransform.array16ElementsToItkTransform(tab);
		univ.getContent("imgMov").getLocalTranslate().getTransform(tr);
		tr.get(tab);
		ItkTransform itTrans=ItkTransform.array16ElementsToItkTransform(tab);
		itTrans.addTransform(itRot);
		itTrans=itTrans.simplify();
		System.out.println("Global transform computed : "+itTrans);
		ItkTransform ret=new ItkTransform(itTrans.getInverse());
		univ.removeAllContents();
		univ.close();
    	univ=null;    
    	return ret;
    }
        


}
	
	
	
	
	
	
	
	