package com.vitimage;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Path;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.ImageCalculator;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import trainableSegmentation.Trainable_Segmentation;

public class TestRomain {

	
	public static void main(String [] args) {
		ImageJ imag=new ImageJ();
		
//		ImagePlus img3=  multiplyFloatImages(img1,img2);
//		img3.show();
//		test();
//		System.exit(0);
		ImagePlus img1=IJ.openImage("/home/fernandr/Bureau/Test/Test_NN/test_IRM-1.tif");
		IJ.run(img1,"32-bit","");
		img1.show();
		Trainable_Segmentation_EX ts;
		ts= new Trainable_Segmentation_EX();
		ts.run("");
		VitimageUtils.waitFor(10000);
		
		
	}

	
	
	public static void test() {

		ImagePlus img1=IJ.openImage("/home/fernandr/Bureau/Test/Test_NN/test_IRM-1.tif");
		ImagePlus img2=IJ.openImage("/home/fernandr/Bureau/Test/Test_NN/test_IRM.tif");
		IJ.run(img1,"32-bit","");
		IJ.run(img2,"32-bit","");
		img1.show();
//		img2.show();
		VitimageUtils.waitFor(10000);
		RoiManager rm=RoiManager.getRoiManager();
		Roi roi=rm.getRoi(0);
		
		rm.remove(0);
		rm.close();
		img1.changes=false;
		img1.close();
		 img1=IJ.openImage("/home/fernandr/Bureau/Test/Test_NN/test_IRM-1.tif");
		 img1.show();
		 rm=RoiManager.getRoiManager();
		System.out.println("Retrait effectué");
		VitimageUtils.waitFor(5000);
		System.out.println("Chargement equivalent");
		rm.setVisible(true);
		rm.show();
		VitimageUtils.waitFor(50000);
	}
	
	
	/** Input of the plugin :    - ImageJ is opened, with NImg 2-D images opened : M0, T1, T2, RX, ....
	 *  						 - The ROI manager is opened, with Nroi ROIs : bois_noir,bois_pasnoir, bois_pasdutoutnoir, ... 
	 *  						 
	*/	
	public void run(String arg) {
		
		//Script parameters (feel free to change them)
		boolean chooseCustomDirectory=false;
		boolean testRomain=true;	
		String pathOnCedricComputer="D:\\TRAVAIL\\Manip\\VITIMAGE_Imagerie\\ANALYSE DONNEES\\2019-01_Recalage_CEP002_to_photograph_31_mu\\mesures sur ROI\\data\\blabla";
		String pathOnRomainComputer="/home/fernandr/Bureau/Test/TestRoiCedric";
		int nbMinOfImages=1;
		int nbMinOfRois=1;		
		//Edit from last version : Have to import this : import java.awt.Point;

		
		//Setup for output path
		String stDir =System.getProperty("user.dir");
		String path="";
		if(chooseCustomDirectory) {
			OpenDialog od=new OpenDialog("Choose an output path");
			path=od.getDirectory();
		}
		else{
			if(testRomain)path=pathOnRomainComputer;
			else path=pathOnCedricComputer;
		}		

		
		//Setup information to user
		IJ.log("Setup summary :");
		IJ.log("-- Current dir : "+stDir);
		IJ.log("-- Output dir  : "+path);
		

		
		//  Step 1 : Identify images and get their titles
		IJ.log("\nImage detection");
		String imgName;
		int[] wList = WindowManager.getIDList();
        if (wList==null) {IJ.error("No images are open.");return;}
        String[] imgTitles = new String[wList.length];
        ImagePlus[]imgs=new ImagePlus[wList.length];
		int yMax=WindowManager.getImage(wList[0]).getHeight();
		int xMax=WindowManager.getImage(wList[0]).getWidth();
        for(int indImg=0;indImg<wList.length;indImg++){
			imgTitles[indImg]=WindowManager.getImage(wList[indImg]).getShortTitle();
			IJ.log(" - Image numero "+indImg+" : "+imgTitles[indImg]);
			imgs[indImg]=WindowManager.getImage(wList[indImg]);
			WindowManager.getImage(wList[indImg]).hide();
        }
		if(wList.length < nbMinOfImages){IJ.error("Not enough opened images : "+wList.length);return;}
		int nbImg=wList.length;
		

		//Step 2 : Identify Rois and get their titles
		IJ.log("\nROIs detection");
        String roiName;
		RoiManager roiMg=RoiManager.getRoiManager();
        if (roiMg==null) {IJ.error("No ROI opened : ");return;}
		int nbRoi=roiMg.getCount(); 								// compte le nb de ROI dans manager
        String[] roiTitles = new String[nbRoi];
        for(int indRoi=0;indRoi<nbRoi;indRoi++){
			roiTitles[indRoi]=roiMg.getName(indRoi);
			IJ.log(" - Roi numero "+indRoi+" : "+roiTitles[indRoi]);
        }
		if(nbRoi < nbMinOfRois){IJ.error("Not enough ROIs : "+nbRoi);return;}  // défini le nb de ROI minimal
		
		

		//--------------------------------------
		// For each couple (Image , ROI ), export the target pixels in two structured formats :
		// --> rows vectors (Xpixel,Ypixel,VALUEpixel) in a excel-style file ROI_DATA_nameoftheroi_nameofthemodality.csv  
		// --> Masked image, in a image file ROI_DATA_nameoftheroi_nameofthemodality.tif
		//

		
		IJ.log("\n\nProcessing Data");
		int nMatch;
		File f = new File(path+((testRomain) ? "/" : "\\")+"pixel_data.csv");
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));		
	
			
			
			//For each roi
			for(int indRoi=0;indRoi<nbRoi;indRoi++){
				Roi currentRoi=roiMg.getRoi(indRoi);
				
				
				//for each image
				for(int indImg=0;indImg<nbImg;indImg++){
					imgs[indImg].show();
					System.out.println(indImg+","+indRoi);
   		            IJ.log("Processing Image-"+indImg+" ("+imgTitles[indImg]+")   X   Roi-"+indRoi+" ("+roiTitles[indRoi]+")");
					nMatch=0;
	
					//Open the image
					roiMg.runCommand(WindowManager.getImage(wList[indImg]),"Show All");
					roiMg.select(indRoi);
						
					byte[] pixelsIn=(byte[])(imgs[indImg].getStack().getProcessor(1).getPixels());
					ByteProcessor maskProc = imgs[indImg].createRoiMask();
					byte[] mask=(byte[])maskProc.getPixels();
	
					ImagePlus imPlusOut=ij.gui.NewImage.createImage("img_ROI_"+roiTitles[indRoi]+"_"+imgTitles[indImg]+".tif",xMax,yMax,1,8,ij.gui.NewImage.FILL_BLACK);  // cree image vide pour y ajouter les pixels de la ROI
					byte[] pixelsOut=(byte[])(imPlusOut).getProcessor().getPixels();
					for(int xx=0;xx<xMax;xx++){
						for(int yy=0;yy<yMax;yy++){
							if(mask[xMax*yy+xx]==(byte)( 255)) {
								nMatch++;
								pixelsOut[xMax*yy+xx]=pixelsIn[xMax*yy+xx];//copy it in a new image
								out.write(xx+" "+yy+" "+(int)(pixelsIn[xMax*yy+xx] & 0xff)+" "+roiTitles[indRoi]+" "+imgTitles[indImg]+"\n");//write it to csv file
							}
						}
					}
					IJ.log(" -> Number of pixels in this area = "+nMatch);
	
	//				imPlusOut.show(); //Uncomment this line to open the successive sub-images 
					FileSaver fs=new FileSaver(imPlusOut);
					String s="img_ROI_"+roiTitles[indRoi]+"_"+imgTitles[indImg]+".tif";
					fs.saveAsTiff(path+((testRomain) ? "/" : "\\")+s);
	
					imgs[indImg].hide();
				}
			}
			out.close();
		} catch (Exception e) {	IJ.log("Here is a problem");		}

		// Step 3 : suggest that user can play with joint histogram and get fun
		IJ.log("Use the joint histogram routine in order to explore the link between modalities over the rois");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public TestRomain() {
		// TODO Auto-generated constructor stub
	}

}
