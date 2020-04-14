package com.vitimage.fijiyama;

import java.io.File;

import com.vitimage.common.VitiDialogs;
import com.vitimage.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.LUT;

public class TestingFijiyamaPackage {

	public TestingFijiyamaPackage() {
		// TODO Auto-generated constructor stub
	}

	

	public static void main(String[]args) {
		
		ImageJ ij=new ImageJ();
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/test.tif");
		LUT[] luts = img.getLuts();
		for(int i=0;i<luts.length;i++) {
			System.out.println("Lut number " +i+luts[i]);
		}
		VitimageUtils.waitFor(1000000);
		ImagePlus imgRef=img.duplicate();
		img.setC(1);
		img.setDisplayRange(0, 100);
		img.setC(2);
		img.setDisplayRange(0, 1000);
		img.setC(3);
		img.setDisplayRange(0, 10000);
		imgRef.show();
		img.show();
		
		//Ecrire les labels
		for(int c=1;c<=7;c++) {
				img.getStack().setSliceLabel("c="+c+", z="+1+", t="+1,VitimageUtils.getCorrespondingSliceInHyperImage(img, c-1, 1-1, 1-1));
				img.setC(c);img.setDisplayRange(0, 1+(c-1)*0.33);
		
		}

		
		//0_M
		for(int c=1;c<=7;c++) {
			IJ.saveAsTiff(new Duplicator().run(img,c, c, 1, 40, 1, 1),"/home/fernandr/Bureau/Test/HyperSerie/0_M/Input/img_mod"+c+".tif");
		}
		
		//1_T
		for(int t=1;t<=5;t++) {
			IJ.saveAsTiff(new Duplicator().run(img,1, 1, 1, 40, t, t),"/home/fernandr/Bureau/Test/HyperSerie/1_T/Input/img_t"+t+".tif");
		}

		//2_MT
		for(int c=1;c<=7;c++) {
			for(int t=1;t<=5;t++) {
				IJ.saveAsTiff(new Duplicator().run(img,c, c, 1, 40, t, t),"/home/fernandr/Bureau/Test/HyperSerie/2_MT/Input/img_t"+t+"_mod"+c+".tif");
			}
		}
		
		//3_mT
		for(int t=1;t<=5;t++) {
			IJ.saveAsTiff(new Duplicator().run(img,1, 7, 1, 40, t, t),"/home/fernandr/Bureau/Test/HyperSerie/3_mT/Input/img_t"+t+".tif");
		}

		//4_Mt
		for(int m=1;m<=7;m++) {
			IJ.saveAsTiff(new Duplicator().run(img,m, m, 1, 40, 1, 5),"/home/fernandr/Bureau/Test/HyperSerie/4_Mt/Input/img_mod"+m+".tif");
		}
	}		
		/*
	
	//Integration test (after running unit test in TestingRegistrationPackage.java)
	public void runTest() {
		System.out.println("Testing mode");
		int TEST_TWO_IMGS=1;
		int TEST_ABDOMEN=2;
		int TEST_MULTIMODAL_SERIES=3;
		int TEST_TIME_LAPSE_SERIES=4;
		int TEST_GUI=5;

		String nameDOI=VitiDialogs.chooseDirectoryUI("Localize Fijiyama_DOI dir, see https://imagej.net/Fijiyama)", "Select this DOI file");
		String nameFile=new File(nameDOI).getName();
		System.out.println("Fijiyama_DOI file selected : "+nameDOI);
		if(!nameFile.equals("Fijiyama_DOI")){
			IJ.showMessage("Bad Fijiyama_DOI file, or name changed. Abort");
			return;
		}
		IJ.showMessage("Testing procedure\nTest 1 = two images\nTest 2 = Abdomen\nTest 3 = Multimodal series\nTest 4 = Time-lapse series\n5 = Test Gui");
		int currentTest=VitiDialogs.chooseNumberUI("Choose the test (between 1 and 4)",1,5,1);
		if(currentTest==TEST_GUI) {
			System.out.println("Gui test");
			Fijiyama_GUI gui=new Fijiyama_GUI();
			gui.modeWindow=WINDOWIDLE;
			gui.run("");
			gui.modeWindow=WINDOWIDLE;
			gui.startLaunchingInterface();
			gui.modeWindow=WINDOWIDLE;
			return;
		}

		regManager.createOutputPathAndFjmFile();
		if(currentTest==TEST_TWO_IMGS) {
			System.out.println("Two imgs test");
			File dirCase=new File(nameDOI,"Case_01_Two_images");
			File inputDir=new File(dirCase,"Input_data");
			regManager.setupFromTwoImages(new String[] {new File(inputDir,"imgRef.tif").getAbsolutePath(),new File(inputDir,"imgMov.tif").getAbsolutePath()});
			if(frameLaunch!=null)frameLaunch.setVisible(false);
			modeWindow=WINDOWTWOIMG;
			startTwoImagesRegistration();
		}
		if(currentTest==TEST_ABDOMEN) {
			System.out.println("Abdomen test");
			File dirCase=new File(nameDOI,"Case_02_Abdomen");
			File inputDir=new File(dirCase,"Input_data");
			regManager.setupFromTwoImages(new String[] {new File(inputDir,"RX.tif").getAbsolutePath(),new File(inputDir,"MRI.tif").getAbsolutePath()});
			if(frameLaunch!=null)frameLaunch.setVisible(false);
			modeWindow=WINDOWTWOIMG;
			startTwoImagesRegistration();
		}
		if(currentTest==TEST_MULTIMODAL_SERIES) {
			System.out.println("Multimodal series test");
			File dirCase=new File(nameDOI,"Case_03_Cep_3_mods");
			File inputDir=new File(dirCase,"Input_data");
			modeWindow=WINDOWSERIEPROGRAMMING;
			regManager.startSetupSerieFromScratch(3,inputDir.getAbsolutePath());
		}
		if(currentTest==TEST_TIME_LAPSE_SERIES) {
			System.out.println("Time-lapse series test");
			modeWindow=WINDOWSERIEPROGRAMMING;
			File dirCase=new File(nameDOI,"Case_04_Time_series");
			File inputDir=new File(dirCase,"Input_data");
			modeWindow=WINDOWSERIEPROGRAMMING;
			regManager.startSetupSerieFromScratch(4,inputDir.getAbsolutePath());
		}
		
		


	}
	*/
}
