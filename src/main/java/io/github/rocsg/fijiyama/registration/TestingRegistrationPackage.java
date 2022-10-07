/*
 * 
 */
package io.github.rocsg.fijiyama.registration;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.itk.simple.DisplacementFieldJacobianDeterminantFilter;
import org.itk.simple.DisplacementFieldTransform;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.plugin.ImageInfo;
import ij.plugin.filter.Convolver;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.fijiyamaplugin.Fijiyama_GUI;
import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationAction;
import io.github.rocsg.fijiyama.registration.BlockMatchingRegistration;
import io.github.rocsg.fijiyama.registration.ItkRegistration;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import math3d.Point3d;

// TODO: Auto-generated Javadoc
/**
 * The Class blockSizeHalfY.
 */
public class TestingRegistrationPackage {

	

	/**
	 * Instantiates a new testing registration package.
	 */
	public TestingRegistrationPackage() {
		// TODO Auto-generated constructor stub
	}

	
	

	/**
	 * The main method.
	 *
	 * @param args the args
	 */
	public static void main(String[]args) {
		@SuppressWarnings("unused")
		ImageJ ij=new ImageJ();
		String[] paths=getInputOutputPathForRegistrationPackage();
		String inputPath=new File(paths[0],"Input_data").getAbsolutePath();
		String outputPath=paths[1];
//		runTest(inputPath,outputPath);
		runTestFijiyamaSupport();
	}

	/**
	 * Run test fijiyama support.
	 */
	public static void runTestFijiyamaSupport() {
		//Last support : Cedric M.
		String dir="/home/fernandr/Bureau/A_Test/Cedric/Case_1/";
		RegistrationAction regAct=RegistrationAction.readFromTxtFile(dir+"regAct.txt");
		ImagePlus imgRef=IJ.openImage(dir+"imgRef.tif");
		ImagePlus imgMov=IJ.openImage(dir+"imgMov.tif");
		BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef,imgMov,regAct);
		bm.runBlockMatching(null,false);
	}
	
	/**
	 * Gets the input output path for registration package.
	 *
	 * @return the input output path for registration package
	 */
	public static String[]getInputOutputPathForRegistrationPackage(){
		String inputPath,outputPath;
		if(new File("/home/fernandr").exists()) {
			inputPath="/home/fernandr/Bureau/A_Test/TestingRegistrationPackageData";
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


	
	/**
	 * Run test.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 */
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
	
	
	/**
	 * Run test level 00 simple passing.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
	public static int runTestLevel00simplePassing(String inputPath,String outputPath){
		return 0;
	}
	
	/**
	 * Run test level 01 fail with exception.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
	public static int runTestLevel01failWithException(String inputPath,String outputPath){
		int a=1/0;
		return 0;
	}
		
	/**
	 * Run test level 02 fail with code 2.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
	public static int runTestLevel02failWithCode2(String inputPath,String outputPath){
		boolean success=false;
		if(success)return 0;
		return 2;
	}
	
	/**
	 * Run test level 03 simple access to classes and objects.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
	public static int runTestLevel03SimpleAccessToClassesAndObjects(String inputPath,String outputPath){
		ItkRegistration itkMan=new ItkRegistration();
		BlockMatchingRegistration bm=new BlockMatchingRegistration();
		return 0;
	}

	
	/**
	 * Run test level 04 transform 2 D.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
	public static int runTestLevel04Transform2D(String inputPath,String outputPath){
		boolean buildingCase=false;
		int errors=0;
		ImagePlus imgIn;
		ImagePlus imgExp;
		ImagePlus out;
		ImagePlus diff;
		ItkTransform tr;
		
		System.out.println(inputPath);
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
	
	
	/**
	 * Run test level 05 transform 3 D.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
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
	
	/**
	 * Run test level 06 transform 4 D.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
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

	/**
	 * Run test level 07 transform 5 D.
	 *
	 * @param inputPath the input path
	 * @param outputPath the output path
	 * @return the int
	 */
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


	
	
	
	
	/**
	 * Builds the test data for jacobian stuff.
	 */
	public static void buildTestDataForJacobianStuff() {
		//Open a standard image with multiple canals
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Traitements/Sorgho/Tests/Test SSM1/Out_input_Fijiyama/Input/hyperImage_T0.tif");
		ItkTransform tr=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Traitements/Sorgho/Tests/Test SSM1/Out_input_Fijiyama/Output/Registration_files/Transform_Step_6.transform.tif");
		double sigma=VitimageUtils.getDimensionsRealSpace(img)[0]/15.0;
		ImagePlus resMaison=tr.getJacobianHomeMadeBecauseOfUnavoidedCoreDumpIssueWithSimpleITKDisplacementFieldJacobianDeterminantFilter(img,sigma,40);
		ImagePlus resITK=null;
		for(int i=0;i<10;i++) {
			System.out.println(i);
			resITK=ItkTransform.getJacobian(tr, img, 40);
			resMaison=tr.getJacobianHomeMadeBecauseOfUnavoidedCoreDumpIssueWithSimpleITKDisplacementFieldJacobianDeterminantFilter(img,sigma,40);
		}
		resMaison.setTitle("Maison");
		resITK.setTitle("resITK");
		resMaison.show();
		resITK.show();
		VitimageUtils.waitFor(10000000);
		
		ImagePlus imgBig=IJ.openImage("/home/fernandr/Bureau/Temp/TestJac/img_T1_M1.tif");
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Temp/TestJac/testImg.tif");
		System.out.println();
		for(int z=0;z<imgRef.getStackSize();z++)imgRef.getStack().setSliceLabel(imgRef.getStack().getSliceLabel(z+1)+"_EXTENSIVE", z+1);
		
		double[]imageCenter=VitimageUtils.getImageCenter(imgRef, true);
		imgRef.show();
		imgRef.setTitle("Ref");
		//Build a transform that push the image to the right
		ItkTransform rigidTranslation=ItkTransform.getRigidTransform(imageCenter, new double[] {0,0,0}, new double[] {-3,0,0});
		ImagePlus testRig=rigidTranslation.transformImage(imgRef, imgRef);
		testRig.show();
		testRig.setTitle("Rigid");
		
		int diff=2;
		Point3d []ptMov =new Point3d [] {
				new Point3d(imageCenter[0]+diff,imageCenter[1],imageCenter[2]),
				new Point3d(imageCenter[0]-diff,imageCenter[1],imageCenter[2]),
				new Point3d(imageCenter[0],imageCenter[1]+diff,imageCenter[2]),
				new Point3d(imageCenter[0],imageCenter[1],imageCenter[2]-diff)
				};
		Point3d []ptRef =new Point3d [] {
				new Point3d(imageCenter[0]+diff/1.1,imageCenter[1],imageCenter[2]),
				new Point3d(imageCenter[0]-diff/1.1,imageCenter[1],imageCenter[2]),
				new Point3d(imageCenter[0],imageCenter[1]+diff/1.2,imageCenter[2]),
				new Point3d(imageCenter[0],imageCenter[1],imageCenter[2]-diff),
				};
		
		ItkTransform homotheticPart=ItkTransform.estimateBestAffine3D(ptRef, ptMov);
		System.out.println(homotheticPart);
		ImagePlus testHomo=homotheticPart.transformImage(imgRef, imgRef);
		testHomo.show();
		testHomo.setTitle("Homo");
		
		//Build a vector field that make a lot of stuff in the right part, and nothing on the left part
		int nZ=5;
		int nX=30;
		int nY=30;
		double[]dimsReal=VitimageUtils.getDimensionsRealSpace(imgRef);
		double deltaCible=dimsReal[1]/10;
		Point3d[][]correspondancesPoints=new Point3d[2][nZ*nX*nY];
		for(int x=0;x<nX;x++)for(int y=0;y<nY;y++)for(int z=0;z<nZ;z++) {
			double deltaX=0;
			double deltaY=(y<nY/2) ? 0 : (x<nX/2 ? -deltaCible : deltaCible);
			double deltaZ=0;
			int ind=x*nY*nZ+y*nZ+z;
			correspondancesPoints[0][ind]=new Point3d(x*1.0/nX*dimsReal[0] , y*1.0/nY*dimsReal[1]  ,  z*1.0/nZ*dimsReal[2]);
			correspondancesPoints[1][ind]=new Point3d(x*1.0/nX*dimsReal[0] +deltaX, y*1.0/nY*dimsReal[1] +deltaY ,  z*1.0/nZ*dimsReal[2] +deltaZ );
		}
		
		
		ItkTransform trr=new ItkTransform(new DisplacementFieldTransform(ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(correspondancesPoints, imgRef, deltaCible, false)));
		
		//Buld a transform that make a translation to the right
		
		//Open the associated vector field that was used, with things in it (contractions)
		
		//
		//In one canal, add 
		VitimageUtils.waitFor(1000000);
	}
	
	
	
	/**
	 * Make little tests.
	 */
	public static void makeLittleTests() {
		//buildTestDataForJacobianStuff();
		//testKhi2();
		buildTestDataForJacobianStuff();
		VitimageUtils.waitFor(2000000);
		System.exit(0);

		ItkTransform tr=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Traitements/Sorgho/Test SSM1/Out_input_Fijiyama/Output/Registration_files/Transform_Step_6.transform.tif");
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Traitements/Sorgho/Test SSM1/Out_input_Fijiyama/Output/Exported_data/hyperImage_T0_after_registration.tif");
		ImagePlus grid=tr.viewAsGrid3D(imgRef, 10);
		grid.show();
		
		
	}
	
	
	
	
	
	
}
