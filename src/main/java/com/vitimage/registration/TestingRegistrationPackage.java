package com.vitimage.registration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.vitimage.common.VitiDialogs;
import com.vitimage.common.VitimageUtils;
import com.vitimage.fijiyama.Fijiyama_GUI;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

public class TestingRegistrationPackage {

	public TestingRegistrationPackage() {
		// TODO Auto-generated constructor stub
	}

	
	public static void main(String[]args) {
		@SuppressWarnings("unused")
		ImageJ ij=new ImageJ();
		
		String[] paths=getInputOutputPathForRegistrationPackage();
		String inputPath=new File(paths[0],"Input_data").getAbsolutePath();
		String outputPath=paths[1];
		runTest(inputPath,outputPath);
	}

	public static String[]getInputOutputPathForRegistrationPackage(){
		String inputPath,outputPath;
		if(new File("/home/fernandr").exists()) {
			inputPath="/home/fernandr/Bureau/Test/TestingRegistrationPackageData";
		}
		else {
			inputPath=VitiDialogs.chooseDirectoryUI("Localize TestingRegistrationPackageData directory", "Select this directory");
			String nameFile=new File(inputPath).getName();
			System.out.println("Directory selected : "+nameFile);
			if(!nameFile.equals("TestingRegistrationPackageData")){
				IJ.showMessage("Bad directory for testing this package, or name changed. Abort");
				return null;
			}			
		}
		outputPath=new File(inputPath,"Output_data_"+new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date())).getAbsolutePath();
		//new File(outputPath).mkdirs();
		return new String[] {inputPath,outputPath};
	}


	
	public static void runTest(String inputPath,String outputPath) {
		int firstTest=0;
		int lastTest=12;
		int numberTest=13;
		numberTest=Math.max(numberTest,lastTest+1);
		String[]testNames=new String[numberTest];
		for(int i=0;i<numberTest;i++)testNames[i]="Test "+(i<10 ? "0" : "")+i+" : no test set at this step       ";
        int index=0;
		testNames[index++]="Test 00 : simple passing with code 0     ";
        testNames[index++]="Test 01 : fail with exception            ";
        testNames[index++]="Test 02 : fail with code 2               ";
        testNames[index++]="Test 03 : access to classes and objects  ";
        testNames[index++]="Test 04 : transform 2D image(8/16/24/32b)";
        testNames[index++]="Test 05 : transform 3D image rig/dense   ";
        testNames[index++]="Test 06 : transform 4D image chan/frame  ";
        testNames[index++]="Test 07 : transform 5D image chan & frame";
        testNames[index++]="Test 08 : rigid registration 2D 8-32b    ";
        testNames[index++]="Test 09 : manual registration slicer     ";
        testNames[index++]="Test 10 : manual registration 3D         ";
        testNames[index++]="Test 11 : rigid registration 3D          ";
        testNames[index++]="Test 12 : dense registration 3D          ";
	
		int []passed=new int[numberTest+1];//0= everything ok, 1=exception caught, 2+ = other
		for(int test=firstTest;test<=lastTest;test++) {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}		
			System.out.println("\n\nTestingRegistrationPackage. Running test "+test);
			passed[test]=0;
			int temp=0;
			try {
				switch(test) {
					case 0:temp=runTestLevel00simplePassing(inputPath,outputPath);break;
					case 1:temp=runTestLevel01failWithException(inputPath,outputPath);break;
					case 2:temp=runTestLevel02failWithCode2(inputPath,outputPath);break;
					case 3:temp=runTestLevel03SimpleAccessToClassesAndObjects(inputPath,outputPath);break;
					case 4:temp=runTestLevel04Transform2D(inputPath,outputPath);break;
					case 5:temp=runTestLevel05Transform3D(inputPath,outputPath);break;
					case 6:temp=runTestLevel06Transform4D(inputPath,outputPath);break;
					case 7:temp=runTestLevel07Transform5D(inputPath,outputPath);break;
					default:break;
				}		
			}catch (Exception e) {
				System.out.println("In TestingRegistrationPackage.java, test number "+test+" failed. Reason : ");
				e.printStackTrace();
				temp=-1;
			};
			passed[test]=temp;
			System.out.println("Result : "+passed[test]);
		}		
		int nbExceptions=0;
		int nbErrorCodes=0;
		System.out.println("\nTest results summary :\n----------------------------------------------------------------\nNUMBER  : TITLE                          ... RESULT\n----------------------------------------------------------------");
		for(int test=0;test<firstTest;test++) {
			System.out.println(testNames[test]+"... "+"not tested.");
			if(test%5==4)System.out.println("----------------------------------------------------------------");
		}
		for(int test=firstTest;test<=lastTest;test++) {
			System.out.println(testNames[test]+"... "+(passed[test]==0 ?"passed" : passed[test]==-1 ? "failed with exception" : "failed with code "+passed[test]));
			if(test!=1 && passed[test]==-1)nbExceptions++;
			if(test!=2 && passed[test]>0)nbErrorCodes++;
			if(test%5==4)System.out.println("----------------------------------------------------------------");
		}
		for(int test=lastTest+1;test<numberTest;test++) {
			System.out.println(testNames[test]+"... "+"not tested.");
			if(test%5==4)System.out.println("----------------------------------------------------------------");
		}
		int nbTests=lastTest-firstTest+1;
		System.out.println("\nNumber of successful tests   : "+((lastTest-firstTest+1-nbExceptions-nbErrorCodes)+" / "+(lastTest-firstTest+1))); 
		System.out.println("Number of failed tests       : "+(nbExceptions+nbErrorCodes)+" / "+(lastTest-firstTest+1)); 
		System.out.println("Number of Exceptions caughts : "+nbExceptions);
		System.out.println("Number of Error codes        : "+nbErrorCodes);
		
	}
	
	
	public static int runTestLevel00simplePassing(String inputPath,String outputPath){
		return 0;
	}
	
	public static int runTestLevel01failWithException(String inputPath,String outputPath){
		int a=1/0;
		return 0;
	}
		
	public static int runTestLevel02failWithCode2(String inputPath,String outputPath){
		boolean success=false;
		if(success)return 0;
		return 2;
	}
	
	public static int runTestLevel03SimpleAccessToClassesAndObjects(String inputPath,String outputPath){
		ItkRegistration itkMan=new ItkRegistration();
		BlockMatchingRegistration bm=new BlockMatchingRegistration();
		return 0;
	}

	
	public static int runTestLevel04Transform2D(String inputPath,String outputPath){
		boolean buildingCase=false;
		int errors=0;
		ImagePlus imgIn;
		ImagePlus imgExp;
		ImagePlus out;
		ImagePlus diff;
		ItkTransform tr;
		
		imgIn=IJ.openImage(new File(inputPath,"img_2D_32bits.tif").getAbsolutePath());		
		tr=ItkTransform.getRigidTransform(ItkTransform.getImageCenter(imgIn), new double[] {0,0,20},new double[] {0,0,0});
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY32)errors+=1;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_00_result1.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_00_result1.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullFloatImage(diff))errors+=2;
		}
		
		imgIn=IJ.openImage(new File(inputPath,"img_2D_32bits.tif").getAbsolutePath());
		IJ.run(imgIn,"16-bit","");
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY16)errors+=4;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_00_result2.tif").getAbsolutePath());
		else{
			imgExp=IJ.openImage(new File(inputPath,"level_00_result2.tif").getAbsolutePath());
			IJ.run(imgExp,"32-bit","");
			IJ.run(out,"32-bit","");
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullFloatImage(diff))errors+=8;
		}
		
		imgIn=IJ.openImage(new File(inputPath,"img_2D_32bits.tif").getAbsolutePath());
		IJ.run(imgIn,"8-bit","");
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY8)errors+=16;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_00_result3.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_00_result3.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullImage(diff))errors+=32;
		}
		
		imgIn=IJ.openImage(new File(inputPath,"img_2D_32bits.tif").getAbsolutePath());
		IJ.run(imgIn,"RGB Color","");
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.COLOR_RGB)errors+=64;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_00_result4.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_00_result4.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullImage(diff))errors+=128;
		}
		
		return errors;
	}
	
	
	public static int runTestLevel05Transform3D(String inputPath,String outputPath){
		boolean buildingCase=false;
		int errors=0;
		ImagePlus imgIn;
		ImagePlus imgExp;
		ImagePlus out;
		ImagePlus diff;
		ItkTransform tr;
		
		imgIn=IJ.openImage(new File(inputPath,"img_3D_32bits.tif").getAbsolutePath());		
		tr=ItkTransform.getRigidTransform(ItkTransform.getImageCenter(imgIn), new double[] {0,0,20},new double[] {0,0,5});
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY32)errors+=1;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_05_result1.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_05_result1.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullFloatImage(diff))errors+=2;
		}
		
		imgIn=IJ.openImage(new File(inputPath,"img_3D_32bits.tif").getAbsolutePath());
		IJ.run(imgIn,"16-bit","");
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY16)errors+=4;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_05_result2.tif").getAbsolutePath());
		else{
			imgExp=IJ.openImage(new File(inputPath,"level_05_result2.tif").getAbsolutePath());
			IJ.run(imgExp,"32-bit","");
			IJ.run(out,"32-bit","");
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullFloatImage(diff))errors+=8;
		}
		
		imgIn=IJ.openImage(new File(inputPath,"img_3D_32bits.tif").getAbsolutePath());
		IJ.run(imgIn,"8-bit","");
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY8)errors+=16;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_05_result3.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_05_result3.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullImage(diff))errors+=32;
		}
		
		imgIn=IJ.openImage(new File(inputPath,"img_3D_32bits.tif").getAbsolutePath());
		IJ.run(imgIn,"RGB Color","");
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.COLOR_RGB)errors+=64;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_05_result4.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_05_result4.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullImage(diff))errors+=128;
		}
		
		
		
		imgIn=IJ.openImage(new File(inputPath,"img_3D_32bits.tif").getAbsolutePath());
		IJ.run(imgIn,"8-bit","");
		tr.addTransform(ItkTransform.readAsDenseField(new File(inputPath,"Transform_Step_0.transform.tif").getAbsolutePath()));
		out=tr.transformImage(imgIn,imgIn,false);
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_05_result5.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_05_result5.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullImage(diff))errors+=32;
		}
		
		tr=tr.getFlattenDenseField(imgIn);
		out=tr.transformImage(imgIn,imgIn,false);
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_05_result6.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_05_result6.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullImage(diff))errors+=32;
		}
		
		
		
		return errors;
	}
	
	public static int runTestLevel06Transform4D(String inputPath,String outputPath){
		boolean buildingCase=false;
		int errors=0;
		
		ImagePlus imgExp;
		ImagePlus imgIn;
		ImagePlus out;
		ImagePlus diff;
		ItkTransform tr;
		
		imgIn=IJ.openImage(new File(inputPath,"img_4D_Chan.tif").getAbsolutePath());		
		tr=ItkTransform.getRigidTransform(ItkTransform.getImageCenter(imgIn), new double[] {0,0,20},new double[] {0,0,5});
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY8)errors+=1;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_06_result1.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_06_result1.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullFloatImage(diff))errors+=2;
		}

		imgIn=IJ.openImage(new File(inputPath,"img_4D_Frames.tif").getAbsolutePath());		
		tr=ItkTransform.getRigidTransform(ItkTransform.getImageCenter(imgIn), new double[] {0,0,20},new double[] {0,0,5});
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY8)errors+=1;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_06_result2.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_06_result2.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullFloatImage(diff))errors+=2;
		}
		
		return errors;
	}

	public static int runTestLevel07Transform5D(String inputPath,String outputPath){
		boolean buildingCase=false;
		int errors=0;
		ImagePlus imgIn;
		ImagePlus imgExp;
		ImagePlus out;
		ImagePlus diff;
		ItkTransform tr;
		
		imgIn=IJ.openImage(new File(inputPath,"img_5D.tif").getAbsolutePath());		
		tr=ItkTransform.getRigidTransform(ItkTransform.getImageCenter(imgIn), new double[] {0,0,20},new double[] {0,0,5});
		out=tr.transformImage(imgIn,imgIn,false);
		if(out.getType()!=ImagePlus.GRAY8)errors+=1;
		if(buildingCase)IJ.saveAsTiff(out,new File(inputPath,"level_07_result1.tif").getAbsolutePath());
		else {
			imgExp=IJ.openImage(new File(inputPath,"level_07_result1.tif").getAbsolutePath());
			diff=VitimageUtils.makeOperationBetweenTwoImages(imgExp,out,VitimageUtils.OP_SUB,true);
			if(!VitimageUtils.isNullFloatImage(diff))errors+=2;
		}
		
		return errors;
	}


}
