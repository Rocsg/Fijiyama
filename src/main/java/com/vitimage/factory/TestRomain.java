package com.vitimage.factory;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Random;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Transform;
import org.scijava.vecmath.Color3f;

import com.vitimage.common.ItkImagePlusInterface;
import com.vitimage.common.TransformUtils;
import com.vitimage.common.VineType;
import com.vitimage.common.VitiDialogs;
import com.vitimage.common.VitimageUtils;
import com.vitimage.common.VitimageUtils.Capillary;
import com.vitimage.common.VitimageUtils.ComputingType;
import com.vitimage.common.VitimageUtils.SupervisionLevel;
import com.vitimage.ndimaging.MRI_Clinical;
import com.vitimage.ndimaging.MRI_Ge3D;
import com.vitimage.ndimaging.MRI_T1_Seq;
import com.vitimage.ndimaging.MRI_T2_Seq;
import com.vitimage.ndimaging.Photo_Slicing_Seq;
import com.vitimage.ndimaging.Vitimage4D;
import com.vitimage.ndimaging.Vitimage5D;
import com.vitimage.registration.BlockMatchingRegistration;
import com.vitimage.registration.ItkTransform;
import com.vitimage.registration.MetricType;
import com.vitimage.registration.Transform3DType;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
import ij.WindowManager;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.plugin.StackCombiner;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import inra.ijpb.binary.geodesic.GeodesicDistanceTransform3DFloat;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import math3d.Point3d;

public class TestRomain {

	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		
		
		ImagePlus irmf=IJ.openImage("/home/fernandr/Bureau/Test/MEDICAL/irm-f.tif");
		ImagePlus irmb=IJ.openImage("/home/fernandr/Bureau/Test/MEDICAL/irm-b.tif");

		//Mettre b apres f
		
		
		//		produceTestForSplineField();
		//testFields();
//		produceIntermediary();
		//		testSpline0();
		//test3Fields();
		//tresGrosseChiasse();
//		petitCacaToutMou();
		//chiachia();
//		scheisse();
		//makeAllGeImport();
		//makeAllAssistedDetections();
		//finishAll4D();
//		makeAll5D();
		
//		makeAllTiImport();
		//testSuccessiveRegistration();		//enormeChiasse();
		//produceStackTestForPhoto();
		//importOnlyGe3dForVerif();
		//testRandom();
		//testCon();
//		testCorr();
		//chiasse5();
//		chiasse7();
		//testBM();
		//makeAllPhoto();
//		inspectAllPhoto();
		//publishResults();
		//testHoughGameTransform();
		//makeTrainingHybride();
		//		testBalanceDesBlancs();
		//inspectRXTof();
		//viewMRI();

	//	takeInputCorrespondanceForBiasEstimation();
		//computeFieldBiasBasedOnMultipleCorrespondances(0,false,false);
		//testStrategyField();
		/*
		boolean fixCenter=true;
		boolean gauss=true;
		double sigma=10;
		computeFieldBiasBasedOnMultipleCorrespondances(0,fixCenter,gauss,sigma);
		chia(0,fixCenter,gauss,sigma);
		sigma=15;
		computeFieldBiasBasedOnMultipleCorrespondances(0,fixCenter,gauss,sigma);
		chia(0,fixCenter,gauss,sigma);
*/
		
		//computeFieldBiasBasedOnMultipleCorrespondances();//takeInputCorrespondanceForBiasEstimation();
//		computeImageForClinicalMRIBiasEstimation();
		//pequenaJala();
//		testTransT1T2();
//		testTransT1M0();
		//ImagePlus test=IJ.openImage("/home/fernandr/tmpImageRec.tif");test.setSlice(10);ImagePlus testSlice=test.crop();
		//ImagePlus []testTab=VitimageUtils.decomposeRGBImage(testSlice);
		/*FeatureStackArray fsa = buildFeatureStackArrayRGBSeparatedMultiThreaded(testTab,
				new boolean[] {true,true,true,true,true,true,true,true,true,true,true,true,true,true,true,true},1,4);*/
				//makeHybrideCepVertical();
//		testTrans();
//		//		charlotte();
		
		
		//		makeExperimentOnSegmentationsAndGetValuesOfPercentageTissueAccordingWithZ();
//		makeExperimentOnSegmentationsAndGetValuesOfPercentageTissueAccordingWithGeodesicFromCenter();
//		makeExperimentOnSegmentationsAndGetValuesOfPercentageTissueAccordingWithGeodesic();
	
		//testGrad();
		//VitimageUtils.waitFor(100000);
		ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Test/TWOIMG/OUTPUT_DIR/Exported_data/img_moving_after_registration.tif");
		imgMov=VitimageUtils.getBinaryGrid(imgMov, 17, true, true);
		IJ.run(imgMov,"8-bit","");
		imgMov.show();
		VitimageUtils.showImageIn3D(imgMov,false);
		VitimageUtils.waitFor(1000000);
		
		testNewSchemeNoAlgo();

		//makeHybrideCepVertical();
		String str="pouetpouet.fjm";
		System.out.println(str);
		System.out.println(str.substring(str.length()-4,str.length()));
		System.out.println(str.substring(str.length()-4,str.length()).equals(".fjm"));
		VitimageUtils.waitFor(1000000000);
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/testXYZCT.tif");
		ImagePlus imgTrans=new ItkTransform().transformHyperImage(imgRef, imgMov);
		imgMov.show();
		imgTrans.show();
		VitimageUtils.waitFor(1000000000);
		ImagePlus img=null;
		ImagePlus[]testImg=VitimageUtils.stacksFromHyperstackFastBis(img);
		for(int i=0;i<testImg.length;i++)testImg[i].show();
		img.show();
		imgRef=IJ.openImage("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B080_NP/Computed_data/2_HyperImage/B080_NP_HyperImage.tif");
		VitimageUtils.printImageResume(imgRef);
		imgRef.show();
     	double[][]tab=VitimageUtils.getHistogram(imgRef,3);
     	System.out.println(TransformUtils.stringMatrixMN("",tab));
     	int[]range=VitimageUtils.getRange(imgRef,95,3);

		imgMov=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/imgRef_Trans1.tif");
		ImagePlus refCopy=VitimageUtils.imageCopy(imgRef);
		ImagePlus movCopy=VitimageUtils.imageCopy(imgMov);
		refCopy.resetDisplayRange();
		IJ.run(refCopy,"8-bit","");
		movCopy.resetDisplayRange();
		IJ.run(movCopy,"8-bit","");
		movCopy.show();
		VitimageUtils.waitFor(10000);
		ij3d.Image3DUniverse universe=new ij3d.Image3DUniverse();
		universe.show();
		universe.addContent(refCopy, new Color3f(Color.red),"imgRef",50,new boolean[] {true,true,true},1,0 );
		imgMov.show();
		VitimageUtils.waitFor(5000);
		universe.addContent(movCopy, new Color3f(Color.green),"imgMov",50,new boolean[] {true,true,true},1,0 );
		ij3d.ImageJ3DViewer.select("imgRef");
		ij3d.ImageJ3DViewer.lock();
		ij3d.ImageJ3DViewer.select("imgMov");
		IJ.showMessage("Move the green volume to match the red one\nWhen done, push the \"Validate\" button to stop\n\nCommands : \n"+
		"mouse-drag the green object to turn the object\nmouse-drag the background to turn the scene\nCTRL-drag to translate the green object");
		VitimageUtils.waitFor(1000000);
		
		
		/*
		int[][]pouet=VitimageUtils.listForThreads(66,13);
		System.out.println(TransformUtils.stringMatrixN(pouet, ""));
		*/
		
		img=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/imgRef_Ttest.tif");
		int[]dims=VitimageUtils.getDimensions(img);
		double[]dimsD=new double[] {dims[0],dims[1],dims[2]};
		double[]voxs=VitimageUtils.getVoxelSizes(img);
		double[]dimsCenter=TransformUtils.multiplyVector(TransformUtils.multiplyVector(dimsD, voxs),0.5);
		double[]dimsPreCenter=TransformUtils.multiplyVector(TransformUtils.multiplyVector(dimsD, voxs),0.49);
		ItkTransform[]trans=new ItkTransform[] {ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,0  , 0,1,0,0,  0,0,1,0,  0,0,0,1}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0.1}, new double[] {0,0,0}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0.2}, new double[] {0,0,0}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0.3}, new double[] {0,0,0}),//Bof
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0.4}, new double[] {0,0,0}),//Bof
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0.1,0,0}, new double[] {0,0,0}),//5
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0.2,0,0}, new double[] {0,0,0}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0.3,0,0}, new double[] {0,0,0}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0}, new double[] {0.2,0,0}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0}, new double[] {0.5,0,0}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0}, new double[] {1,0,0}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0}, new double[] {0,0,0.2}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0}, new double[] {0,0,0.5}),
												ItkTransform.getRigidTransform(dimsCenter, new double[] {0,0,0}, new double[] {0,0,1.0})
				};
		
		for(int i=0;i<trans.length;i++) {
			ItkTransform tr=trans[i];
			System.out.println("\n\nTrans "+i+" front"+tr.drawableString());
			System.out.println("Trans "+i+" rear"+new ItkTransform(tr.getInverse()).drawableString());
			//ImagePlus imgT=tr.transformImage(img,img);
			//IJ.saveAsTiff(imgT, "/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/imgRef_Trans"+i+".tif");
		}

		
		//ItkTransform trRotZ=ItkTransform.getRigidTransform(center, angles, translation)

		
		VitimageUtils.waitFor(100000000);
		//stressTest();
		
		//ImagePlus img2=transformParts12(img,values,25,25);
//		ImagePlus imgT1Bef=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/HYBRID/T1BEFORE.tif");
//		ImagePlus imgT1Aft=transformParts12(imgT1Bef,values,512,512);
//		ImagePlus imgN=evaluateIntensityBias();
		//		IJ.saveAsTiff(imgN, "/home/fernandr/Bureau/test.tif");
		//imgN=IJ.openImage("/home/fernandr/Bureau/test.tif");
		//ImagePlus imgN2=correctionBizarre(imgN);
		//IJ.saveAsTiff(imgN2, "/home/fernandr/Bureau/test2.tif");
		//imgN.show();
		//imgN2.show();

		//Image
		//makeHybrideCepVertical();
//		evaluateIntensityBias();
		//		extractIRMnormalisationInfoForCep5D();
		//makeHybrideCepVerticalTotal3D() ;
		
		//testRoi();
		VitimageUtils.waitFor(1000000);

		VitimageUtils.waitFor(1000000);
		ImagePlus imgSegAniso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/segAniso.tif");
		ImagePlus imgSegIso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/segmentation_iso.tif");
//		computeGeodesicDistanceMap(ImagePlus imgSeg,int labelSeed,int firstLabelIncludedInExtensionArea,int firstLabelExcludedFromExtensionArea,int referenceDimForAnisotropyReduction)
		ImagePlus result=VitimageUtils.computeGeodesicDistanceMap(imgSegIso,  3 ,            1,                                             3                                        ,2);
		imgSegIso.show();
		result.show();
		
		VitimageUtils.waitFor(100000000);
		ImagePlus imgMri=IJ.openImage("/home/fernandr/Bureau/test_MRI.tif");
		ImagePlus imgFib=IJ.openImage("/home/fernandr/Bureau/test_FIB.tif");
		imgMri.show();
		imgFib.show();
//		VitimageUtils.compositeRGBDoubleJet(VitimageUtils.nullImage(imgMri), imgFib,imgMri, "test1_blue", false, 0).show();;
		//		VitimageUtils.compositeRGBDoubleJet(VitimageUtils.nullImage(imgMri), imgFib,imgMri, "test1_grays", false, 1).show();;
		VitimageUtils.compositeRGBDoubleJet(VitimageUtils.nullImage(imgMri), imgFib,imgMri, "test1_jet", false, 2).show();;

		VitimageUtils.waitFor(1000000);
//		computeCambiumAreas();
		/*
		makeExperimentOnSegmentationsAndGetValuesOfPercentageTissueAccordingWithGeodesic();
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/test.tif.tif");
		img.show();
		makeTaggingSurface(true,false);
		VitimageUtils.waitFor(100000000);
		System.exit(0);
		
		makeTaggingSurface(true,false);
	*/
		
	}

	
	public static void makingData() {
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/TWOIMG/imgRef.tif");
		String []mods=new String[] {"MRI","RX"};
		String []times=new String[] {"1","2","3"};
		int incr=0;
		ImagePlus []imgs=new ImagePlus[5];
		for(int im=0;im<2;im++) {
			for(int it=0;it<3;it++) {
				if(im==1 && it==1)continue;
				double angleZ=(Math.random()-0.5);
				double angleY=(Math.random()-0.5)*0.1;
				double angleX=(Math.random()-0.5)*0.1;
				double factMult=1+(  (Math.random()-0.5))*0.5;
				double factAdd=Math.random()*0.1;
				double dX=(Math.random()-0.5);
				double dY=(Math.random()-0.5);
				double dZ=(Math.random()-0.5);
				ItkTransform tr=ItkTransform.getRigidTransform(VitimageUtils.getImageCenter(img,true), new double[] {angleX,angleY,angleZ}, new double[] {dX,dY,dZ});
				ImagePlus temp=tr.transformImage(img, img, false);
				temp=VitimageUtils.makeOperationOnOneImage(temp, 1, factAdd,false);
				temp=VitimageUtils.makeOperationOnOneImage(temp, 2, factMult,false);
				VitimageUtils.setLabelOnAllSlices(temp,"img_t"+times[it]+"_mod"+mods[im]+".tif");
				IJ.saveAsTiff(temp,"/home/fernandr/Bureau/Test/SERIE/INPUT_DIR/img_t"+times[it]+"_mod"+mods[im]+".tif");
				imgs[incr++]=temp;
			}			
		}
		ImagePlus result=Concatenator.run(imgs);
		result.show();
	}
	
	public static void testIsDense() {
		ItkTransform tr=new ItkTransform();
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/testImg.tif");
		System.out.println("Is dense ? "+tr.isDense());
		ItkTransform tr2=ItkTransform.getIdentityDenseFieldTransform(imgRef);
		tr.addTransform(tr);
		System.out.println("Is dense ? "+tr.isDense());
	}
	
	public static void testNewSchemeNoAlgo() {
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/testImg.tif");
		ImagePlus imgTemp=IJ.openImage("/home/fernandr/Bureau/testImg.tif");
		imgRef.show();
//		imgTemp.show();
		ItkTransform rot1=ItkTransform.getRigidTransform(VitimageUtils.getImageCenter(imgRef,true), new double[] {0,0,0.4}, new double[] {0,0,0});
		ItkTransform trans1=ItkTransform.getRigidTransform(VitimageUtils.getImageCenter(imgRef,true), new double[] {0,0,0}, new double[] {2,0,0});
		ItkTransform tr=new ItkTransform(rot1);
		tr.addTransform(trans1);
		ImagePlus testMov=tr.transformImage(imgRef,imgRef,false);
		testMov.show();
		imgTemp=rot1.transformImage(imgTemp, imgTemp, false);
		imgTemp.show();
		ImagePlus imgMov=trans1.transformImage(imgTemp, imgTemp, false);
		imgMov.show();
		
		ItkTransform transEstimatedAtFirstStep=ItkTransform.getRigidTransform(VitimageUtils.getImageCenter(imgRef,true), new double[] {0,0,0}, new double[] {-2,0,0});
		ImagePlus imgAfterFirstStep=transEstimatedAtFirstStep.transformImage(imgMov, imgMov, false);
		imgAfterFirstStep.show();
		ItkTransform transEstimatedAtSecondStep=ItkTransform.getRigidTransform(VitimageUtils.getImageCenter(imgRef,true), new double[] {0,0,-1}, new double[] {0,0,0});
		ImagePlus imgAfterSecondStep=transEstimatedAtSecondStep.transformImage(imgAfterFirstStep, imgAfterFirstStep,false);
		imgAfterSecondStep.show();
		ItkTransform trAll=new ItkTransform(transEstimatedAtFirstStep);
		trAll.addTransform(new ItkTransform(transEstimatedAtSecondStep));
		ImagePlus imgAfterAllStep=trAll.transformImage(imgMov, imgMov, false);
		imgAfterAllStep.show();
		ItkTransform trOnlySecondStepComputed=new ItkTransform((transEstimatedAtFirstStep.getInverse()));
		trOnlySecondStepComputed.addTransform(trAll);
		System.out.println("REAL TRANSFORM="+transEstimatedAtSecondStep.drawableString());
		System.out.println("ESTIMATED TRANSFORM="+trOnlySecondStepComputed.drawableString());
		
	}

	
/*	
	polygon = roi.getPolygon()
			  n_points = polygon.npoints
			  x = polygon.xpoints
			  y = polygon.ypoints
*/
	
	public static void testRoi() {
		String subject="B079_NP";
		String day="J35";
		String slash="/";
		//Load transfo T1 from Vit4D, getT1 M0				
		ItkTransform transT14D=	ItkTransform.readTransformFromFile("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+subject+"/Source_data/"+day
				+slash+ "Computed_data"+slash+"0_Registration"+slash+"transformation_"+0+"_step_"+"afterItkRegistration"+".txt");
		
		//Load transfo Both from Vit5D
		ItkTransform transTall5D=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+subject+"/Computed_data/0_Registration/transfo_last_"+day+".tif");
		System.out.println("   Processing composition");				
		transT14D.addTransform(transTall5D);
		ImagePlus imgEcho=IJ.openImage("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+subject+"/Source_data/"+day+"/Source_data/MRI_T1_SEQ/Computed_data/1_RegisteredStacks/Recovery_1.tif");
		IJ.saveAsTiff(imgEcho, "/home/fernandr/Bureau/imgInit.tif");
		imgEcho=transT14D.transformImage(imgEcho, imgEcho,false);
		IJ.saveAsTiff(imgEcho, "/home/fernandr/Bureau/imgAfter.tif");
		double[]voxS=VitimageUtils.getVoxelSizes(imgEcho);
		System.out.println(TransformUtils.stringVectorN(voxS, "VoxelSizes"));
		Point3d p1=new Point3d(256*voxS[0],256*voxS[1],20*voxS[2]);
		Point3d p2=new Point3d(306*voxS[0],256*voxS[1],20*voxS[2]);
		Point3d p3=new Point3d(256*voxS[0],256*voxS[1],21*voxS[2]);
		System.out.println("Init="+p1);
		System.out.println("Transfo="+transT14D.transformPoint(p1));
		System.out.println("Init="+p2);
		System.out.println("Transfo="+transT14D.transformPoint(p2));
		System.out.println("Init="+p3);
		System.out.println("Transfo="+transT14D.transformPoint(p3));
	}
	
	
	
	
	
	
	
	
	
	public static void makeTaggingSurface(boolean doHealthy,boolean borderOnly) {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		int []sliceRootStocks= new int[] {200+86 , 200+76 , 200+174 ,          200+65 , 200+97 , 200+108 ,        200+71  , 200+74 ,  200+68 ,       200+59 , 200+42  , 200+118}; 
		Strel3D str=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(2,2, 2);

		for(int i=0;i<12;i++) {
			String spec=specimen[i];
			System.out.println("Processing "+spec);
			ImagePlus dist=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+(doHealthy ? "/distanceSain.tif" : "/distanceFull.tif"));
			
			ImagePlus binary=VitimageUtils.thresholdFloatImage(dist,0.000001, 1000);
			ImagePlus binaryEcorce=null;
			if(borderOnly) {
				ImagePlus binErode=new ImagePlus("",Morphology.erosion(binary.getImageStack(),str));
				binaryEcorce=VitimageUtils.makeOperationBetweenTwoImages(binary,binErode, 4,false);
			}
			else binaryEcorce=VitimageUtils.imageCopy(binary);
			//		VitimageUtils.imageChecking(binaryEcorce,"binaryEcorce");ng 
//			binaryEcorce.show();
//			VitimageUtils.waitFor(100000);

//			dist.setDisplayRange(0, 300);
//			dist.show();
//			VitimageUtils.waitFor(10000000);
			ImagePlus distEcorce=VitimageUtils.makeOperationBetweenTwoImages(binaryEcorce, dist,2,true);
			distEcorce=VitimageUtils.makeOperationOnOneImage(distEcorce, 3,255, true);
			double ray10=6;
			double ray5=3;
			double ray1=2;
			double ray05=1;
				
			int X=dist.getWidth();
			int Y=dist.getHeight();
			int Z=dist.getStackSize();
			int index;
			
			for(int z=0;z<Z;z++) {
				float[]vals=(float[])distEcorce.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						index=y*X+x;
						if(Math.abs((vals[index])%100)<ray10)vals[index]=0;
						if(Math.abs((vals[index])%50)<ray5)vals[index]=0;
						if(Math.abs((vals[index])%10)<ray1)vals[index]=0;
						if(Math.abs((vals[index])%5)<ray05)vals[index]=0;
					}
				}
			}
			distEcorce.setDisplayRange(0, 300);
			IJ.run(distEcorce,"8-bit","");
			IJ.saveAsTiff(distEcorce,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distInventaire"+(doHealthy ? "_Healthy":"_Full")+(borderOnly ? "_border" :"_surface")+".tif");
//			VitimageUtils.waitFor(1000000000);
		}
	}
	
	public static void getPercentageTissueAccordingWithGeodesic() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		int []sliceRootStocks= new int[] {200+86 , 200+76 , 200+174 ,          200+65 , 200+97 , 200+108 ,        200+71  , 200+74 ,  200+68 ,       200+59 , 200+42  , 200+118}; 
		for(int i=0;i<12;i++) {
			
			/**Parameters handling, opening anisotropic segmentation from the weka trained classifier*/
			String spec=specimen[i];
			System.out.println("Traitement "+specimen[i]);
			int sliceRootStock=sliceRootStocks[i];
			ImagePlus segAniso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation.tif");
			

			
			
			/**Build mask image of zones full and healthy at an isotropic resolution (1mm x 1 mm x 1 mm)*/
			System.out.println("Seg full iso.");
			ImagePlus segFullAniso=VitimageUtils.thresholdByteImage(segAniso, 1, 10);
			ImagePlus segFullIso=VitimageUtils.subXYZ(segFullAniso, new double[] {0.722,0.722,1}, true);
			segFullIso=VitimageUtils.thresholdByteImage(segFullIso,10,256);
			System.out.println("Ok. Seg sain iso.");
			ImagePlus segSainAniso=VitimageUtils.thresholdByteImage(segAniso, 1, 2);
			ImagePlus segSainIso=VitimageUtils.subXYZ(segSainAniso, new double[] {0.722,0.722,1}, true);
			segSainIso=VitimageUtils.thresholdByteImage(segSainIso,10,256);
			
			//Get images dimension at isotropic resolution (1mm x 1 mm x 1 mm)
			System.out.println("Ok. Dims.");
			int X=segSainIso.getWidth();
			int Y=segSainIso.getHeight();
			int Z=segSainIso.getStackSize();
			

			
			
			/**Build seed images for geodesic maps*/
			//The seeds will be the voxels of the cep intersecting a XY plane located at 20 cm (200 slices) below the estimated trunk top
			System.out.println("Ok. build seed sain.");
			ImagePlus seedSain=VitimageUtils.makeOperationOnOneImage(segSainIso,2,0, true);
			seedSain=VitimageUtils.drawParallepipedInImage(seedSain, 0, 0, sliceRootStock, X-1, Y-1, sliceRootStock, 1);
			seedSain=VitimageUtils.makeOperationBetweenTwoImages(seedSain, segSainIso, 2,false);

			System.out.println("Ok. Seg full iso.");
			ImagePlus seedFull=VitimageUtils.makeOperationOnOneImage(segFullIso,2,0, true);
			seedFull=VitimageUtils.drawParallepipedInImage(seedFull, 0, 0, sliceRootStock, X-1, Y-1, sliceRootStock, 1);
			seedFull=VitimageUtils.makeOperationBetweenTwoImages(seedFull, segSainIso, 2,false);


			
			
			
			/**Compute geodesic map for healthy tissue, and for all tissue, in isotropic grid, then resample it in the anisotropic grid*/
			System.out.println("Ok. Distance sain");
			float[] floatWeights = new float[] {1000,1414,1732};
			ImagePlus distanceSain=new ImagePlus(
					"geodistance",new GeodesicDistanceTransform3DFloat(
							floatWeights,true).geodesicDistanceMap(seedSain.getStack(), segSainIso.getStack())
					);
			segSainIso=VitimageUtils.makeOperationOnOneImage(segSainIso,3,255, true);
			segSainIso=VitimageUtils.drawParallepipedInImage(segSainIso, 0, 0, sliceRootStock+1, X-1, Y-1, Z-1, 0);
			distanceSain=VitimageUtils.makeOperationBetweenTwoImages(distanceSain, segSainIso, 2, true);
			
			System.out.println("Ok. Distance full");
			ImagePlus distanceFull=new ImagePlus(
					"geodistance",new GeodesicDistanceTransform3DFloat(
							floatWeights,true).geodesicDistanceMap(seedFull.getStack(), segFullIso.getStack())
					);
			segFullIso=VitimageUtils.makeOperationOnOneImage(segFullIso,3,255, true);
			segFullIso=VitimageUtils.drawParallepipedInImage(segFullIso, 0, 0, sliceRootStock+1, X-1, Y-1, Z-1, 0);
			distanceFull=VitimageUtils.makeOperationBetweenTwoImages(distanceFull, segFullIso, 2,true);

			System.out.println("Ok. sain aniso");
			VitimageUtils.adjustImageCalibration(distanceSain, segSainIso);
			ImagePlus imgDistSain=VitimageUtils.subXYZ(distanceSain,new double[] {1/0.722,1/0.722,1}, false);
			int maxSain=(int)Math.ceil(VitimageUtils.maxOfImage(distanceSain)+0.001);

			System.out.println("Ok. full aniso");
			VitimageUtils.adjustImageCalibration(distanceFull, segFullIso);
			ImagePlus imgDistFull=VitimageUtils.subXYZ(distanceFull,new double[] {1/0.722,1/0.722,1}, false);
			int maxFull=(int)Math.ceil(VitimageUtils.maxOfImage(distanceFull)+0.001);
			
			
			
			
			/**Inventory quantities of each tissues along the geodesic distances*/
			segSainAniso=VitimageUtils.makeOperationOnOneImage(segSainAniso, 3,255, false);
			int[][]countTabFull=VitimageUtils.countVolumeByDistance(segAniso,imgDistFull,4,0.0001,300,3);
			VitimageUtils.writeIntArrayInFile(countTabFull,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/countTabGeodesicFull.txt");
//			IJ.saveAsTiff(segAniso, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segAniso.tif"); ?? Utility ??
			IJ.saveAsTiff(imgDistFull, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/imgDistFull.tif");
		}
	}

		
	public static void computeCambiumAreas() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		int []sliceRootStocks= new int[] {200+86 , 200+76 , 200+174 ,          200+65 , 200+97 , 200+108 ,        200+71  , 200+74 ,  200+68 ,       200+59 , 200+42  , 200+118}; 
		for(int i=0;i<12;i++) {
			/**Parameters handling, opening anisotropic segmentation from the weka trained classifier*/
			String spec=specimen[i];
			System.out.println("Traitement "+specimen[i]);
			int sliceRootStock=sliceRootStocks[i];
			ImagePlus segAniso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation.tif");

			/**Compute area of cambium,*/
			ImagePlus boundary1=VitimageUtils.boundaryZone(segAniso, 1,4,6,255);		
			ImagePlus boundary2=VitimageUtils.boundaryZone(segAniso, 1,0,6,255);		
			ImagePlus boundary3=VitimageUtils.boundaryZone(segAniso, 2,4,6,255);		
			ImagePlus boundary4=VitimageUtils.boundaryZone(segAniso, 2,0,6,255);		
			ImagePlus allBounds=VitimageUtils.binaryOperationBetweenTwoImages(boundary1, boundary2, 1);
			allBounds=VitimageUtils.binaryOperationBetweenTwoImages(allBounds, boundary3, 1);
			allBounds=VitimageUtils.binaryOperationBetweenTwoImages(allBounds, boundary4, 1);
			ImagePlus irmT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_2_THIN_low_mri_t1.tif");
			ImagePlus irmT2=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_3_THIN_low_mri_t2.tif");
			ImagePlus irmDP=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_4_THIN_low_mri_m0.tif");
			ImagePlus irmPhoto=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_9_THIN_low_photo_rgb.tif");
			ImagePlus segFull=VitimageUtils.thresholdByteImage(segAniso, 1, 5);
			ImagePlus segFullNoEc=VitimageUtils.thresholdByteImage(segAniso, 1, 4);

/*			allBounds.show();
			segAniso.show();
			irmT1.show();
			irmT2.show();
			irmPhoto.show();
			VitimageUtils.compositeOf(irmT2, allBounds).show();
			VitimageUtils.compositeOf(segFull, allBounds).show();
			VitimageUtils.compositeOf(segFullNoEc, allBounds).show();
*/
			
			ImagePlus allBoundsDil=VitimageUtils.morpho(allBounds, 1, 4);
			allBoundsDil=VitimageUtils.binaryOperationBetweenTwoImages(allBoundsDil, segFullNoEc, 2);
			allBoundsDil=VitimageUtils.switchValueInImage(allBoundsDil, 255, 1);
			irmT2=VitimageUtils.makeOperationBetweenTwoImages(allBoundsDil, irmT2,2,false);
			irmT1=VitimageUtils.makeOperationBetweenTwoImages(allBoundsDil, irmT1,2,false);
			irmDP=VitimageUtils.makeOperationBetweenTwoImages(allBoundsDil, irmDP,2,false);
			System.out.println("Before sub : ");
			VitimageUtils.printImageResume(irmT2);
			VitimageUtils.printImageResume(allBoundsDil);
			
			
			irmT1=VitimageUtils.subXYZ(irmT1, new double[] {0.722,0.722,1}, true);
			irmT2=VitimageUtils.subXYZ(irmT2, new double[] {0.722,0.722,1}, true);
			irmDP=VitimageUtils.subXYZ(irmDP, new double[] {0.722,0.722,1}, true);
			allBoundsDil=VitimageUtils.switchValueInImage(allBoundsDil, 1, 255);
			System.out.println("sub de Dil");
			allBoundsDil=VitimageUtils.subXYZ(allBoundsDil, new double[] {0.722,0.722,1}, true);
			allBoundsDil=VitimageUtils.thresholdByteImage(allBoundsDil, 127,256);
			IJ.saveAsTiff(irmT1,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_T1.tif");
			IJ.saveAsTiff(irmT2,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_T2.tif");
			IJ.saveAsTiff(irmDP,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_PD.tif");
			IJ.saveAsTiff(allBoundsDil,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_area.tif");
			System.out.println("after sub : ");
			VitimageUtils.printImageResume(irmT2);
			VitimageUtils.printImageResume(allBoundsDil);
			
			//		VitimageUtils.compositeOf(irmT2, allBoundsDil).show();
			//		VitimageUtils.waitFor(10000000);
				
				
				
			/*
			ImagePlus boundary6=VitimageUtils.boundaryZone(segAniso, 1,4,6,1);		
			boundary6.setTitle("Boundary 6");
			boundary6.setDisplayRange(0, 1);
			countTabFull=VitimageUtils.countVolumeByDistance(boundary6,imgDistFull,1,0.0001,300,3);
			VitimageUtils.writeIntArrayInFile(countTabFull,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/countTabGeodesicCambiumHealthy.txt");
			ImagePlus mixEcorceHealthy=VitimageUtils.makeOperationBetweenTwoImages(boundary6,imgDistFull,2,true);
			mixEcorceHealthy.setDisplayRange(0, 300);
			IJ.run(mixEcorceHealthy,"Fire","");
			IJ.saveAsTiff(mixEcorceHealthy,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distanceEcorceHealthy.tif");
			Strel3D str=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(2,2, 2);
			ImagePlus img=VitimageUtils.thresholdByteImage(boundary6, 1,255);
			img =new ImagePlus("",Morphology.dilation(img.getImageStack(),str));
			IJ.saveAsTiff(img,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumAreaHealthy.tif");
			img=VitimageUtils.makeOperationOnOneImage(img, 3,255, false);

			ImagePlus irmT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_2_THIN_low_mri_t1.tif");
			irmT1=VitimageUtils.makeOperationBetweenTwoImages(img, irmT1, 2, true);
			irmT1.setDisplayRange(0, 255);
			IJ.run(irmT1,"Fire","");
			IJ.saveAsTiff(irmT1,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumValuesHealthyInT1.tif");

			irmT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_3_THIN_low_mri_t2.tif");
			irmT1=VitimageUtils.makeOperationBetweenTwoImages(img, irmT1, 2, true);
			irmT1.setDisplayRange(0, 255);
			IJ.run(irmT1,"Fire","");
			IJ.saveAsTiff(irmT1,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumValuesHealthyInT2.tif");

			irmT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_4_THIN_low_mri_m0.tif");
			irmT1=VitimageUtils.makeOperationBetweenTwoImages(img, irmT1, 2, true);
			irmT1.setDisplayRange(0, 255);
			IJ.run(irmT1,"Fire","");
			IJ.saveAsTiff(irmT1,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumValuesHealthyInM0.tif");
			
			
			
			
			boundary6=VitimageUtils.boundaryZone(segAniso, 2,4,6,1);		
			boundary6.setTitle("Boundary 6");
			boundary6.setDisplayRange(0, 1);
			countTabFull=VitimageUtils.countVolumeByDistance(boundary6,imgDistFull,1,0.0001,300,3);
			VitimageUtils.writeIntArrayInFile(countTabFull,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/countTabGeodesicCambiumNecrose.txt");
			ImagePlus mixEcorceNecrose=VitimageUtils.makeOperationBetweenTwoImages(boundary6,imgDistFull,2,true);
			mixEcorceNecrose.setDisplayRange(0, 300);
			IJ.run(mixEcorceNecrose,"Fire","");
			IJ.saveAsTiff(mixEcorceNecrose,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distanceEcorceNecrose.tif");
			str=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(2,2, 2);
			img=VitimageUtils.thresholdByteImage(boundary6, 1,255);
			img =new ImagePlus("",Morphology.dilation(img.getImageStack(),str));
			IJ.saveAsTiff(img,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumAreaNecrose.tif");
			img=VitimageUtils.makeOperationOnOneImage(img, 3,255, false);

			irmT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_2_THIN_low_mri_t1.tif");
			irmT1=VitimageUtils.makeOperationBetweenTwoImages(img, irmT1, 2, true);
			irmT1.setDisplayRange(0, 255);
			IJ.run(irmT1,"Fire","");
			IJ.saveAsTiff(irmT1,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumValuesNecroseInT1.tif");
			
			irmT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_3_THIN_low_mri_t2.tif");
			irmT1=VitimageUtils.makeOperationBetweenTwoImages(img, irmT1, 2, true);
			irmT1.setDisplayRange(0, 255);
			IJ.run(irmT1,"Fire","");
			IJ.saveAsTiff(irmT1,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumValuesNecroseInT2.tif");

			irmT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_4_THIN_low_mri_m0.tif");
			irmT1=VitimageUtils.makeOperationBetweenTwoImages(img, irmT1, 2, true);
			irmT1.setDisplayRange(0, 255);
			IJ.run(irmT1,"Fire","");
			IJ.saveAsTiff(irmT1,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambiumValuesNecroseInM0.tif");

			ImagePlus binFull=VitimageUtils.thresholdByteImage(segAniso, 1, 10);
			binFull=VitimageUtils.makeOperationOnOneImage(binFull, 3, 255, true);
			
			ImagePlus mixFull=VitimageUtils.makeOperationBetweenTwoImages(binFull,imgDistFull,2,true);
			mixFull.setDisplayRange(0, 300);
			IJ.run(mixFull,"Fire","");
			IJ.saveAsTiff(imgDistSain,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distanceSain.tif");
			IJ.saveAsTiff(mixFull,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distanceFull.tif");
			
						*/

		}
	}

	
	
	
	/*
	
	public static ImagePlus skeletonize3D(ImagePlus imRef) {
		Skeletonize3D_ skel=new Skeletonize3D_();
		skel.imRef=imRef;
		skel.width = imRef.getWidth();
		skel.height = imRef.getHeight();
		skel.depth = imRef.getStackSize();
		skel.inputImage = imRef.getStack();
						
		// Prepare data
		skel.prepareData(skel.inputImage);
		// Compute Thinning	
		skel.computeThinImage(skel.inputImage);
	
		// Convert image to binary 0-255
		for(int i = 1; i <= skel.inputImage.getSize(); i++)skel.inputImage.getProcessor(i).multiply(255);
		return new ImagePlus("",skel.inputImage);
	}

	*/
	
	public static ImagePlus makeSexyRayX(int mod) {
		String specimen="CEP020_APO1";
		ImagePlus imgSeg=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+specimen+"/segmentation.tif");
		ImagePlus imgRX=new Duplicator().run(IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specimen+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/hyperimage_THIN.tif"),1, 1, 1,385, mod, mod );
		imgSeg.setDisplayRange(0, 5);
		IJ.run(imgSeg, "Fire", "");
		IJ.run(imgSeg, "RGB Color", "");
		IJ.run(imgSeg, "HSB Stack", "");
		ImagePlus[] channels = ChannelSplitter.split(imgSeg);
		IJ.run(imgRX,"32-bit","");
		imgRX=VitimageUtils.makeOperationOnOneImage(imgRX, 2, 1.0/255.0, true);
		channels[2]=VitimageUtils.makeOperationBetweenTwoImages(channels[2], imgRX,2,true);
		channels[2].setDisplayRange(0,255);
		IJ.run(channels[2],"8-bit","");
		
		ImagePlus ret=convertHSBToRGBStack(channels);
		VitimageUtils.adjustImageCalibration(ret, imgRX);
		return ret;
	}
	
	
	public static ImagePlus convertHSBToRGBStack(ImagePlus []hsbTab) {
		int Z=hsbTab[0].getStackSize();
		ImagePlus []retTab=new ImagePlus[Z];
		ImagePlus []tempTab=new ImagePlus[3];
		for(int z=0;z<Z;z++) {
			System.out.println(z);
			for(int can=0;can<3;can++)tempTab[can]=new Duplicator().run(hsbTab[can],1, 1, z+1,z+1, 1, 1 );
			retTab[z]=Concatenator.run(tempTab);
			new ImageConverter(retTab[z]).convertHSBToRGB();
		}
		return(Concatenator.run(retTab));
	}
	
	public static ImagePlus chichi() {
		int nBatches=10;
		ImagePlus[]tab=new ImagePlus[nBatches];
		String rep="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/CEP011_AS1/Batch_";
		for(int b=0;b<10;b++) {
			System.out.println("\nOuverture de "+rep+b+"/probabilities_"+b+".tif");
			tab[b]=IJ.openImage(rep+b+"/probabilities_"+b+".tif");			
			System.out.println("Effectivement : "+TransformUtils.stringVector(VitimageUtils.getDimensions(tab[b]),""));
		}
		return Concatenator.run(tab);
	}
	
	
	public static ImagePlus probaStackToHyperProba(ImagePlus img,int nClasses){
		int Z=img.getStackSize();
		int Zlow=Z/nClasses;
		ImagePlus ret=VitimageUtils.imageCopy(img);
		System.out.println("Commande hyperstackage : "+"Stack to Hyperstack..."+ "order=xytzc channels=1 slices="+Zlow+" frames="+nClasses+" display=Grayscale");
		IJ.run(ret,"Stack to Hyperstack...", "order=xytzc channels=1 slices="+Zlow+" frames="+nClasses+" display=Grayscale");
		return ret;
	}
	
	
	public static ImagePlus[] consensusFactors(ImagePlus img,int nClasses) {
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize()/nClasses;
		float[][]tabVal=new float[nClasses][];
		float[][]tabOutProb=new float[Z][];
		float[][]tabOutCons=new float[Z][];
		ImagePlus []imgVals=new ImagePlus[nClasses];
		ImagePlus retProb= new Duplicator().run(img, 1, 1, 1,Z, 1, 1);
		ImagePlus retCons= new Duplicator().run(img, 1, 1, 1,Z, 1, 1);
		double max1;
		int indmax1;
		double max2;
		int index;
		String s="";
		double[]valsProb=new double[nClasses];
		for(int z=0;z<Z;z++) {
			tabOutProb[z]=(float[])retProb.getStack().getProcessor(z+1).getPixels();
			tabOutCons[z]=(float[])retCons.getStack().getProcessor(z+1).getPixels();
			for(int c=0;c<nClasses;c++) {
				imgVals[c]=new Duplicator().run(img,1,1,z+1,z+1,c+1,c+1);
				tabVal[c]=(float[])imgVals[c].getStack().getProcessor(1).getPixels();
			}
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					index=y*X+x;
					for(int c=0;c<nClasses;c++) {
						valsProb[c]=tabVal[c][index];
					}
					//Recueillir le maximum et le deuxieme maximum
					if(index%7237==0) {
						s+="En effet, tab="+TransformUtils.stringVectorN(valsProb, "valsProb");
					}
					max1=VitimageUtils.max(valsProb);
					indmax1=VitimageUtils.indmax(valsProb);
					valsProb[indmax1]=0;
					max2=VitimageUtils.max(valsProb);
					if(index%7237==0) {
						s+="  donc, tab="+TransformUtils.stringVectorN(valsProb, "valsProb")+" max1="+max1+" , max2="+max2 +"\n";
					}
					tabOutProb[z][index]=(float)(max1);
					tabOutCons[z][index]=(float)(max1/(Math.max(max2,10E-6)));
				}
			}
		}
		retProb.setDisplayRange(0, 1);
		retCons.setDisplayRange(0.9, 10);
		IJ.run(retProb,"Fire","");
		IJ.run(retCons,"Fire","");
		retCons.setDisplayRange(0.9, 10);
		VitimageUtils.writeStringInFile(s, "/home/fernandr/Bureau/test.txt");
		return new ImagePlus[] {retProb,retCons};
	}
	
	
	
	public static ImagePlus correctionBizarre(ImagePlus img) {
		double []factors=new double[2000];
		double []valsSlice=new double[2000];
		double valRef=107;
		int[][]vals=new int[][] {{15 ,32,68,95,124,160,200,230,413,501,528,539,545},{40,44,60,70,80,90,100,107,107,88,83,80,76}};
		for(int i=0;i<vals[0][0];i++)valsSlice[i]=vals[1][0];
		for(int j=0;j<vals[0].length-1;j++) {
			//System.out.println("VALEUR VRAIE : "+vals[0][j]+"--> "+vals[1][j]);
			for(int J=vals[0][j];J<vals[0][j+1];J++) {
				valsSlice[J]=vals[1][j+1] * (J-vals[0][j])*1.0/(vals[0][j+1]-vals[0][j]) +  vals[1][j] * (vals[0][j+1]-J)*1.0/(vals[0][j+1]-vals[0][j])    ;
				//System.out.println("v           : "+J+"--> "+valsSlice[J]);
			}
		}
		for(int i=vals[0][vals[0].length-1];i<2000;i++)valsSlice[i]=vals[1][vals[0].length-1];

		for(int i=0;i<2000;i++) {
			factors[i]=valRef*1.0/valsSlice[i];
			//System.out.println("Final : "+i+" -> "+factors[i]);
		}
		if(img.getType()!=ImagePlus.GRAY8) {			System.out.println("bizarre : unsupported format : "+img.getType()+" format expected="+ImagePlus.GRAY8);return null;}
		int valAvant;
		int valApres;
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		ImagePlus ret=VitimageUtils.imageCopy(img);		
		for(int z=0;z<Z;z++) {
			System.out.println(z);
			byte[] valsImg=(byte[])img.getStack().getProcessor(z+1).getPixels();
			byte[] valsOut=(byte[])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++){
					//valAvant=valsImg[X*(y)+(x)]  & 0xff;
					//valApres=(int)(Math.round(factors[z]*valAvant ));
					//System.out.println("Valeurs : avant="+valAvant+" et apres="+valApres);
					valsOut[X*(y)+(x)]=(byte)( (int) (Math.round(   (factors[z])*( (int) ( valsImg[X*(y)+(x)]  & 0xff  ) ) ) ) );
				}			
			}
			
		}
		System.out.println("FINI");
		return ret;
	}
	
	

	public static ImagePlus evaluateIntensityBias() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		ImagePlus[]transMod=new ImagePlus[12];
		for(int i=0;i<12;i++) {
			System.out.println("Traitement "+specimen[i]);
			String spec=specimen[i];
			String repSpec="/home/fernandr/Bureau/Traitements/Cep5D/"+spec;
			ImagePlus imgMRI=IJ.openImage(repSpec+"/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_2.tif");
			ItkTransform transMRIfromMachine=new ItkTransform((ItkTransform.readTransformFromFile(
				repSpec+"/Source_data/MRI_CLINICAL/Computed_data/1_Transformations/transformation_2.txt")).getInverse());
			transMRIfromMachine.addTransform(ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,-175,0,1,0,-175,0,0,1,-153.6}));
			int []dims=VitimageUtils.getDimensions(imgMRI);
			transMod[i]=transMRIfromMachine.transformImage(VitimageUtils.uncropImageByte(VitimageUtils.switchAxis(imgMRI,2),100, 100, 100, dims[0]+200, dims[2]+200, dims[1]+200), imgMRI,false);
		}
		ImagePlus glob=makeCombination3by4(transMod);
		glob.show();
		return glob;
	}
	
	
	
	
	public static ImagePlus transformParts12(ImagePlus img,double []values,int xP,int yP) {
		ImagePlus ret=VitimageUtils.imageCopy(img);
		int[]dims=VitimageUtils.getDimensions(img);
		int X=dims[0];
		int Y=dims[1];
		int Z=dims[2];
		for(int z=0;z<Z;z++) {
			short[] valsImg=(short [])img.getStack().getProcessor(z+1).getPixels();
			short[] valsRet=(short [])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int index=X*y+x;
					double val=values[4*(y/yP)+(x/xP)]/values[0];
					valsRet[index] = (short)Math.round(   (1/val)*(  (  (short)valsImg[index])  & 0xffff) );
					}
				}			
			}
		return ret;
	}

	
	
	public static void verifMRIEqualization() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		String []modalities= {"Sequence_0.tif", "Sequence_1.tif","Sequence_2.tif"};
		String []modalitiesAp= {"stack_0.tif", "stack_1.tif","stack_2.tif"};
		ImagePlus [][]test=new ImagePlus[3][12];
		ImagePlus [][]testAp=new ImagePlus[3][12];
		ImagePlus []testFin=new ImagePlus[3];
		ImagePlus []testFinAp=new ImagePlus[3];
		for(int m=0;m<3;m++) {//modalities.length
			for(int s=0;s<specimen.length;s++) {
				String rep="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[s]+"/Source_data/MRI_CLINICAL/Computed_data/0_Stacks/";
				String repAp="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[s]+"/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/";
				System.out.println("Lecture de "+rep+modalities[m]);
				test[m][s]=IJ.openImage(rep+modalities[m]);
				System.out.println("Lecture de "+repAp+modalitiesAp[m]);
				testAp[m][s]=IJ.openImage(repAp+modalitiesAp[m]);
			}
			testFin[m]=makeCombination3by4(test[m]);
			testFin[m].setTitle(modalities[m]+" AVANT");
			testFin[m].show();
			testFinAp[m]=makeCombination3by4(testAp[m]);
			testFinAp[m].setTitle(modalitiesAp[m]+" APRES");
			testFinAp[m].show();
		}
	}
				

	
	
	public static ImagePlus makeCombination3by4(ImagePlus[]tab) {
		StackCombiner sc=new StackCombiner();
		ImageStack l1=sc.combineHorizontally(sc.combineHorizontally(sc.combineHorizontally(tab[0].getStack(),tab[1].getStack()),tab[2].getStack()),tab[3].getStack());
		ImageStack l2=sc.combineHorizontally(sc.combineHorizontally(sc.combineHorizontally(tab[4].getStack(),tab[5].getStack()),tab[6].getStack()),tab[7].getStack());
		ImageStack l3=sc.combineHorizontally(sc.combineHorizontally(sc.combineHorizontally(tab[8].getStack(),tab[9].getStack()),tab[10].getStack()),tab[11].getStack());
		ImagePlus ret=new ImagePlus("",sc.combineVertically(l1, sc.combineVertically(l2, l3)));
		VitimageUtils.adjustImageCalibration(ret,tab[0]);
		return ret;
	}
	
	
	
	public static ImagePlus makeCombination2by3(ImagePlus[]tab) {
		StackCombiner sc=new StackCombiner();
		ImageStack l1=sc.combineHorizontally(sc.combineHorizontally(tab[0].getStack(),tab[1].getStack()),tab[2].getStack());
		ImageStack l2=sc.combineHorizontally(sc.combineHorizontally(tab[3].getStack(),tab[4].getStack()),tab[5].getStack());
		ImagePlus ret=new ImagePlus("",sc.combineVertically(l1, l2));
		VitimageUtils.adjustImageCalibration(ret,tab[0]);
		return ret;
	}

	public static ImagePlus makeCombination2by3(ImagePlus i1,ImagePlus i2,ImagePlus i3,ImagePlus i4,ImagePlus i5,ImagePlus i6) {
		return makeCombination2by3(new ImagePlus[] {i1,i2,i3,i4,i5,i6});
	}

		

	
	
	
	
	public static void extractIRMnormalisationInfoForCep5D() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		String []modalities= {"Sequence_0.tif", "Sequence_1.tif","Sequence_2.tif"};
		ImagePlus[][]tabImg=new ImagePlus[modalities.length][specimen.length];
		ImagePlus[]tabMod=new ImagePlus[modalities.length];
		for(int m=0;m<modalities.length;m++) {
			for(int s=0;s<specimen.length;s++) {
				String rep="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[s]+"/Source_data/MRI_CLINICAL/Computed_data/0_Stacks/";
				ImagePlus img=IJ.openImage(rep+modalities[m]);
				img.show();
				double [][]pt= VitiDialogs.waitForPointsUI(1,img,false);
				img.hide();
				tabImg[m][s]=VitimageUtils.cropImageCapillary(img,(int)Math.round(pt[0][0]),(int)Math.round(pt[0][1]),(int)Math.round(pt[0][2]),12,12,12);
			}
			tabMod[m]=makeCombination3by4(tabImg[m]);
			tabMod[m].show();
		}
		ImagePlus ret=Concatenator.run(tabMod);
		IJ.run(ret,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices=25 frames="+modalities.length+" display=Grayscale");
		ret.show();
	}
	
	
	
	
	public static void makeHybrideCepVerticalTotal3D() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		String []modalities= {"Mod_0_THIN_low_photo_bnw.tif", "Mod_1_THIN_low_rx.tif","Mod_2_THIN_low_mri_t1.tif","Mod_3_THIN_low_mri_t2.tif","Mod_4_THIN_low_mri_m0.tif","Mod_5_THIN_low_mri_diff.tif","Mod_6_THIN_low_photo_Red.tif","Mod_7_THIN_low_photo_Green.tif","Mod_8_THIN_low_photo_Blue.tif"};
		int []slices=new int[] {5,10,15,20,25,30,35,40,45,50,60,70,80,90,100,110};
		ImagePlus[][]tabMods=new ImagePlus[9][12];
		ImagePlus[]tabFinal=new ImagePlus[9];
		ImagePlus[]tabHigh=new ImagePlus[12];
		ImagePlus highFinal;
		for(int i=0;i<specimen.length;i++) {
			System.out.println("Lecture "+specimen[i]);
			String rep="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/";
			tabHigh[i]=IJ.openImage(rep+"PHOTOGRAPH/Computed_data/3_Hyperimage/photo_high.tif");
			System.out.println("Ouverture image "+rep+"PHOTOGRAPH/Computed_data/3_Hyperimage/photo_high.tif");
			tabHigh[i]=VitimageUtils.writeBlackTextOnImage(specimen[i]+" Photo high res ",tabHigh[i],30,0);

			for(int j=0;j<modalities.length;j++) {
				tabMods[j][i]=IJ.openImage(rep+"PHOTOGRAPH/Computed_data/3_Hyperimage/"+modalities[j]);
				System.out.println("Ouverture image "+rep+"PHOTOGRAPH/Computed_data/3_Hyperimage/"+modalities[j]);
				tabMods[j][i]=VitimageUtils.writeTextOnImage(specimen[i]+" "+modalities[j], tabMods[j][i],11,0);
			}
		}

		System.out.println("Concatenation de chaque modalité");
		for(int j=0;j<modalities.length;j++) {
			tabFinal[j]=Concatenator.run(tabMods[j]);
		}

		System.out.println("Concatenation finale");
		int Z=tabFinal[0].getStackSize();
		highFinal=Concatenator.run(tabHigh);
		ImagePlus retHoriz=Concatenator.run(tabFinal);
		IJ.run(retHoriz,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+Z+" frames=9 display=Grayscale");
		VitimageUtils.adjustImageCalibration(retHoriz,tabMods[0][0]);
		VitimageUtils.adjustImageCalibration(highFinal,tabMods[0][0]);
		retHoriz.show();
		highFinal.show();
	}
	
	
	

	
	
	
	
	
	public static void makeHybrideCepVertical() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		String []modalities= {"Mod_0_low_photo_bnw.tif", "Mod_1_low_rx.tif","Mod_2_low_mri_t1.tif","Mod_3_low_mri_t2.tif","Mod_4_low_mri_m0.tif","Mod_5_low_mri_diff.tif","Mod_6_low_photo_Red.tif","Mod_7_low_photo_Green.tif","Mod_8_low_photo_Blue.tif"};
		int []slices=new int[] {5,10,15,20,25,30,35,40,45,50,60,70,80,90,100,110};
		ImagePlus[][]tabMods=new ImagePlus[12][9];
		ImagePlus[]tabFinal=new ImagePlus[9];
		ImagePlus[][][]tabSlices=new ImagePlus[12][9][slices.length];
		ImagePlus[][]tabSlicesVert=new ImagePlus[9][slices.length*12];
		ImagePlus[][]tabSlicesHoriz=new ImagePlus[9][slices.length*2 ];
		ImagePlus[]tabHigh=new ImagePlus[12];
		ImagePlus highFinal;
		ImagePlus[][]tabHighSlices=new ImagePlus[12][slices.length];
		ImagePlus[]tabHighSlicesVert=new ImagePlus[slices.length*12];
		for(int i=0;i<specimen.length;i++) {
			System.out.println("Lecture "+specimen[i]);
			String rep="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/";
			tabHigh[i]=IJ.openImage(rep+"/PHOTOGRAPH/Computed_data/3_Hyperimage/photo_high.tif");
			for(int k=0;k<slices.length;k++) {
				tabHigh[i].setSlice(slices[k]+1);
				tabHighSlices[i][k]=tabHigh[i].crop();
				tabHighSlices[i][k]=VitimageUtils.writeBlackTextOnImage(specimen[i]+" Photo high res z="+(slices[k]+1), tabHighSlices[i][k],20,0);
				//tabHighSlices[i][k].show();
				//VitimageUtils.waitFor(500);
				
			}
			for(int j=0;j<modalities.length;j++) {
				tabMods[i][j]=IJ.openImage(rep+"/PHOTOGRAPH/Computed_data/3_Hyperimage/"+modalities[j]);
				for(int k=0;k<slices.length;k++) {
					tabMods[i][j].setSlice(slices[k]+1);
					tabSlices[i][j][k]=tabMods[i][j].crop();
					if(j<1 || j>4)tabSlices[i][j][k]=VitimageUtils.writeTextOnImage(specimen[i]+" Photo high res z="+(slices[k]+1), tabSlices[i][j][k],11,0);
				}
			}
		}
	
		System.out.println("Construction verticale ");
		//Construction vert
		for(int slHyb=0;slHyb<12*slices.length;slHyb++) {
			int slSpec=slHyb%12;
			int slProf=slHyb/12;
			tabHighSlicesVert[slHyb]=VitimageUtils.imageCopy(tabHighSlices[slSpec][slProf]);
		}		
		//highFinal=Concatenator.run(tabHighSlicesVert);			
		//highFinal.show();
		for(int mod=0;mod<9;mod++) {
			System.out.print(" "+mod);
			for(int slHyb=0;slHyb<12*slices.length;slHyb++) {
				int slSpec=slHyb%12;
				int slProf=slHyb/12;
				tabSlicesVert[mod][slHyb]=VitimageUtils.imageCopy(tabSlices[slSpec][mod][slProf]);
			}		
			tabFinal[mod]=Concatenator.run(tabSlicesVert[mod]);			
		}	
		System.out.println("\nHyperImage");
		ImagePlus retVert=Concatenator.run(tabFinal);
		IJ.run(retVert,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+(12*slices.length)+" frames=9 display=Grayscale");
		retVert.show();
	}

	
	public static void testT2M0() {
		String rep="/home/fernandr/Bureau/Traitements/Cep5D/CEP013_AS3/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/";
		int chiffre=40;
		VitimageUtils.compositeGridByte(IJ.openImage(rep+"Mod_1_low_rx.tif"),IJ.openImage(rep+"Mod_2_low_mri_t1.tif"),chiffre,chiffre,chiffre,"T1RX avant").show();		
	}
	

	public static void tt() {
		String rep="/home/fernandr/Bureau/Traitements/Cep5D/CEP013_AS3/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/";
		ImagePlus t1=IJ.openImage(rep+"Mod_1_low_rx.tif");
		ImagePlus t2=IJ.openImage(rep+"Mod_2_low_mri_t1.tif");

		t1.show();
		t1.setTitle("t1");
		t1.setSlice(100);
		t2.show();
		t2.setSlice(100);
		t2.setTitle("t2");
		System.out.println("3");
		VitimageUtils.waitFor(1000);
		System.out.println("2");
		VitimageUtils.waitFor(1000);
		System.out.println("1");
		VitimageUtils.waitFor(1000);
		VitimageUtils.actualizeData(t2, t1);
		System.out.println("Lapin !");
		VitimageUtils.waitFor(1000);
		System.out.println("En effet :");
/*		ImagePlus t3=VitimageUtils.imageCopy(t1);
		ImagePlus t4=VitimageUtils.imageCopy(t2);

		t3.show();
		t3.setSlice(100);
		t3.setTitle("t3");
		t4.show();
		t4.setSlice(100);
		t4.setTitle("t4");
		*/
	}
	
	
	
	
	public static void testTransT1T2() {
		String rep="/home/fernandr/Bureau/Test/T12/";
		ImagePlus imgT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/CEP011_AS1/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_0.tif");
		ImagePlus imgT2=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/CEP011_AS1/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_1.tif");
		ItkTransform trT2=MRI_Clinical.registerT1T2(imgT1,imgT2,null,2); 
		ImagePlus imgT2Reg=trT2.transformImage(imgT1, imgT2,false);
		IJ.saveAsTiff(imgT1, rep+"T1.tif");
		IJ.saveAsTiff(imgT2, rep+"T2.tif");
		IJ.saveAsTiff(imgT2Reg, rep+"T2REC.tif");		
	}
	public static void testTransT1M0() {
		String rep="/home/fernandr/Bureau/Test/T12/";
		ImagePlus imgT1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/CEP011_AS1/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_0.tif");
		ImagePlus imgM0=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/CEP011_AS1/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_2.tif");
		ItkTransform trM0=MRI_Clinical.registerT1T2(imgT1,imgM0,null,2); 
		ImagePlus imgM0Reg=trM0.transformImage(imgT1, imgM0,false);
		IJ.saveAsTiff(imgT1, rep+"T1.tif");
		IJ.saveAsTiff(imgM0, rep+"M0.tif");
		IJ.saveAsTiff(imgM0Reg, rep+"M0REC.tif");
	}
	
	public static void testTrans() {
		String []specs=new String[] {"CEP011_AS1","CEP012_AS2"};
		for(String spec : specs) {
			ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_2_low_mri_t1.tif");
			ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/Mod_1_low_rx.tif");
			ImagePlus imgMovSub=VitimageUtils.Sub222(imgMov);
			ImagePlus imgRefSub=VitimageUtils.Sub222(imgRef);
			double sigma=15;
			int levelMax=2;
			int levelMin=1;
			
			int viewSlice=18;
			int nbIterations=10;
			int neighXY=2;	int neighZ=0;
			int bSXY=5;		int bSZ=2;
			int strideXY=1;		int strideZ=1;
			ItkTransform transRet=null;
	
	
			sigma=10;
			levelMax=1;
			levelMin=1;
			BlockMatchingRegistration bmRegistration3=new BlockMatchingRegistration(imgRefSub,imgMovSub,Transform3DType.DENSE,MetricType.CORRELATION,
					0,sigma,levelMin,levelMax,nbIterations,viewSlice,null,neighXY,neighXY,neighZ,bSXY,bSXY,bSZ,strideXY,strideXY,strideZ);
			bmRegistration3.displayRegistration=0;
			bmRegistration3.displayR2=false;
			transRet=bmRegistration3.runBlockMatching(null);
			//bmRegistration.closeLastImages();
			bmRegistration3.freeMemory();
			ImagePlus res3=transRet.transformImage(imgRef, imgMov,false);
			res3.setDisplayRange(0, 255);
			IJ.run(res3,"8-bit","");
			res3.show();
			res3.setTitle("res3");
			ImagePlus comp3=VitimageUtils.compositeOf(imgRef, res3);
			IJ.saveAsTiff(comp3, "/home/fernandr/Bureau/Test/TestRegClin/"+spec+"comp3_SIG_10_CORR.tif");
			transRet=transRet.flattenDenseField(imgRef);
			transRet.writeAsDenseField("/home/fernandr/Bureau/Test/TestRegClin/"+spec+"field3_10_CORR.tif", imgRef);
	
			sigma=15;
			levelMax=1;
			levelMin=1;
			bmRegistration3=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
					0,sigma,levelMin,levelMax,nbIterations,viewSlice,null,neighXY,neighXY,neighZ,bSXY,bSXY,bSZ,strideXY,strideXY,strideZ);
			bmRegistration3.displayRegistration=0;
			bmRegistration3.displayR2=false;
			transRet=bmRegistration3.runBlockMatching(null);
			//bmRegistration.closeLastImages();
			bmRegistration3.freeMemory();
			res3=transRet.transformImage(imgRef, imgMov,false);
			res3.setDisplayRange(0, 255);
			IJ.run(res3,"8-bit","");
			res3.show();
			res3.setTitle("res3");
			comp3=VitimageUtils.compositeOf(imgRef, res3);
			IJ.saveAsTiff(comp3, "/home/fernandr/Bureau/Test/TestRegClin/"+spec+"comp3_SIG_15_CORR.tif");
			transRet=transRet.flattenDenseField(imgRef);
			transRet.writeAsDenseField("/home/fernandr/Bureau/Test/TestRegClin/"+spec+"field3_15_CORR.tif", imgRef);
		}
	}
			
	
	
	
	public static void chi() {
		ItkTransform trans=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/champ_0_FIX.tif");
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/compAvant_CEP011_AS1.tif");
		trans.showAsGrid3D(imgRef, 20, "Field", 200);		
		trans=ItkTransform.smoothDeformationTransform(trans,5,5,5);
		trans.showAsGrid3D(imgRef, 20, "Field Gauss", 200);		
		
	}
	
	public static void chia(int it,boolean fixCenter,boolean gauss,double sigma) {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		ItkTransform trans=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/champ_"+it+("_SIG"+sigma)+(fixCenter ? "_FIX" : "_NO")+(gauss ? "_GAUSS" : "")+".tif");
		for(String spec : specimen) {
			System.out.println("Traitement "+spec);
			System.out.println("chargement images");
			ImagePlus mri=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/mri_"+spec+".tif");
			ImagePlus rx=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/rx_"+spec+".tif");
			System.out.println("transformation image");
			ImagePlus mrTrans=trans.transformImage(mri, mri,false);
			System.out.println("composition images");
			ImagePlus compAvant=VitimageUtils.compositeOf(rx,mri,"Avant");
			ImagePlus compApres=VitimageUtils.compositeOf(rx,mrTrans,"Apres");
			System.out.println("sauvegarde images");
			IJ.saveAsTiff(compAvant, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/compAvant_"+spec+".tif");
			IJ.saveAsTiff(compApres, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/compApresIter_"+it+("_SIG"+sigma)+"_"+spec+(fixCenter ? "_FIX" : "_NO")+(gauss ? "_GAUSS" : "")+".tif");
		}
	}
	
	//TODO : modify because getCorrespondanceListAsImagePlus(Point) does not exist anymore
/*	public static void computeFieldBiasBasedOnMultipleCorrespondances(int iteration,boolean fixCenter,boolean gauss,double sigma) {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		int nSpec=6;
		
		ImagePlus imgRef1=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/rx_"+specimen[0]+".tif");
		ImagePlus imgRef=VitimageUtils.Sub222(imgRef1);
		double[]voxRough=VitimageUtils.getVoxelSizes(imgRef);
		double voxZ=voxRough[2];
		double voxX=voxRough[0];
		double voxY=voxRough[1];
		Point3d[][][]pts=new Point3d[(iteration+1)][nSpec][];
		int total=0;
		for(int i=0;i<nSpec;i++) {
			String spec=specimen[i];
			for(int iter=0;iter<(iteration+1);iter++) {
				if(new File("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/points_"+spec+"_iteration_"+iteration+".txt").exists()) {
					pts[iter][i]=VitimageUtils.readPoint3dArrayInFile("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/points_"+spec+"_iteration_"+iteration+".txt");
					total+=pts[iter][i].length/2;
				}
			}
		}//		
		Point3d [][]correspondancePoints=new Point3d[(iteration+1)][total];
		int incr=0;
		
		for(int i=0;i<nSpec;i++) {
			for(int iter=0;iter<(iteration+1);iter++) {
				for(int j=0;j<pts[iter][i].length/2;j++) {
					correspondancePoints[1][incr]=pts[iter][i][2*j];//IRM
					correspondancePoints[0][incr++]=pts[iter][i][2*j+1];//RX
				}
			}
		}
		
		System.out.println("Total avant fix="+total);
		if(fixCenter) {
			Point3d [][]correspondancePointsSave=new Point3d[2][total];
			for(int i=0;i<correspondancePoints.length;i++)for(int j=0;j<correspondancePoints[0].length;j++)
				correspondancePointsSave[i][j]=new Point3d(correspondancePoints[i][j].x,correspondancePoints[i][j].y,correspondancePoints[i][j].z);
			double[]slices= {185*voxZ,200*voxZ,230*voxZ};
			double[]xiz= {60*voxX,100*voxX,120*voxX,140*voxX,180*voxX};
			double[]yiz= {60*voxX,100*voxX,120*voxX,140*voxX,180*voxX};
			int nPer=(int)Math.round(total*1.0/(4.0*45));
			int incr2=0;
			correspondancePoints=new Point3d[2][total+3*5*5*nPer];
			System.out.println("A ajouter = "+(3*5*5*nPer));
			System.out.println("Final = "+(total+3*5*5*nPer));
			
			for(int np=0;np<nPer;np++)for(double x : xiz)for(double y : yiz)for(double z:slices) {
				correspondancePoints[1][incr2]=correspondancePoints[0][incr2]=new Point3d(x+np*voxX,y+np*voxY,z+2*np*voxZ);
				incr2++;
			}
			System.out.println("Apres copie fix, incr2="+incr2);
			for(int i=0;i<total;i++) {
				for(int j=0;j<2;j++) {
					correspondancePoints[j][i+incr2]=correspondancePointsSave[j][i];
				}
				
			}
		}
		
		ImagePlus img=(ImagePlus)(VitimageUtils.getCorrespondanceListAsImagePlus(imgRef,correspondancePoints,new double[] {1,1,1},-1)[0]) ;
		IJ.saveAsTiff(img, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/corrPoints_"+iteration+"_"+(fixCenter ? "_FIX" : "_NO")+".tif");
		System.out.println("En effet, incr="+incr+" et total="+total);
		ItkTransform trans=new ItkTransform(new DisplacementFieldTransform(ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(correspondancePoints, imgRef, sigma, false)));
		System.out.println("Here8");
		if(gauss)trans=ItkTransform.smoothDeformationTransform(trans,sigma/2,sigma/2,sigma/2);
		trans.writeAsDenseField("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/champ_"+iteration+("_SIG"+sigma)+(fixCenter ? "_FIX" : "_NO")+(gauss ? "_GAUSS" : "")+".tif", imgRef);
		
		System.out.println("Here9");
		trans.showAsGrid3D(imgRef, 10,"Field"+(fixCenter ? "_FIX" : "_NO")+(gauss ? "_GAUSS" : ""),100);
	}
	
	*/
	
	
	public static void takeInputCorrespondanceForBiasEstimation() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		int nSpec=12;
		int iteration=1;
		ImagePlus imgRef;
		Point3d[]pts;
		for(int i=7;i<nSpec;i++) {
			
			System.out.println("Traitement "+specimen[i]);
			String spec=specimen[i];
			String repSpec="/home/fernandr/Bureau/Traitements/Cep5D/"+spec;
			System.out.println("Load");
			ImagePlus mri=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/mri_"+spec+".tif");
			ImagePlus rx=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/rx_"+spec+".tif");
			ImagePlus composite=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/composite_"+spec+".tif");
			ImagePlus imgplus=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/compApresIter_0_SIG10.0_"+spec+"_FIX_GAUSS.tif");
			imgplus.show();
			pts=VitiDialogs.waitForPointsUIUntilClickOnSlice1(mri,rx,composite,true);//PT 1 = MRI  PT2=RX
			VitimageUtils.writePoint3dArrayInFile(pts,"/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/points_"+spec+"_iter_"+iteration+".txt");	
			imgplus.close();
		}//		ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(correspondancePoints, imgRef, sigma, zeroPaddingOutside)
	}
	
	
	public static void testStrategyField() {
		ItkTransform field=ItkTransform.readAsDenseField("/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/champ_0_SIG10.tif");
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int i=0;i<12;i++) {
			System.out.println("Traitement "+specimen[i]);
			String spec=specimen[i];
			String repSpec="/home/fernandr/Bureau/Traitements/Cep5D/"+spec;
			ImagePlus imgRX=IJ.openImage(repSpec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Stacks2/imgRef_RXForMRItif.tif");
			ImagePlus imgMRI=IJ.openImage(repSpec+"/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_0.tif");

			ItkTransform transMRItoRX= new ItkTransform(
				ItkTransform.readTransformFromFile(repSpec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Transforms2/trans_MRI_AUTO.txt"));
			ItkTransform transMRIfromMachine=new ItkTransform((ItkTransform.readTransformFromFile(
				repSpec+"/Source_data/MRI_CLINICAL/Computed_data/1_Transformations/transformation_0.txt")).getInverse());

			transMRIfromMachine.addTransform(ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,-175,0,1,0,-175,0,0,1,-153.6}));
			transMRIfromMachine.addTransform(field);
			transMRIfromMachine.addTransform(ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,-175,0,1,0,-175,0,0,1,-153.6} ).getInverse( ));
			transMRIfromMachine.addTransform(new ItkTransform(ItkTransform.readTransformFromFile(
				repSpec+"/Source_data/MRI_CLINICAL/Computed_data/1_Transformations/transformation_0.txt")));
			transMRIfromMachine.addTransform(transMRItoRX);
			ImagePlus resAvecDef=transMRIfromMachine.transformImage(imgRX, imgMRI,false);
			ImagePlus resSansDef=transMRItoRX.transformImage(imgRX, imgMRI,false);
		
			ImagePlus compAvant=VitimageUtils.compositeOf(imgRX,resSansDef,"Avant");
			ImagePlus compApres=VitimageUtils.compositeOf(imgRX,resAvecDef,"Apres");
			System.out.println("sauvegarde images");
			IJ.saveAsTiff(compAvant, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/Better/Avant_"+spec+".tif");
			IJ.saveAsTiff(compApres, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/Better/Apres_"+spec+".tif");
		}
		return;	
	}
	
	
	
	/*Pour construire les images dans le cadre bias, j ai :
	* Pris la transformation machine inverse
	Je lui ai ajoute un decalage 

	* Cette transformation me permet de reechantillonner IRM dans la geometrie init
	Donc techniquement, je pourrais :
	ImagePlus imgMRI=IJ.openImage(repSpec+"/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_0.tif");
	T=transMRIMachine.add(decalage).add(field).add(decalageinverse).add(transMRImachineinverse).add(transToRX)
	T.transformImage(mri)
	composite(rx,mri)
	*/
	
	
	
	
	public static void computeImageForClinicalMRIBiasEstimation() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int i=0;i<12;i++) {
			System.out.println("Traitement "+specimen[i]);
			String spec=specimen[i];
			String repSpec="/home/fernandr/Bureau/Traitements/Cep5D/"+spec;
			System.out.println("Load");
			ItkTransform transMRIfromMachine=new ItkTransform((ItkTransform.readTransformFromFile(repSpec+"/Source_data/MRI_CLINICAL/Computed_data/1_Transformations/transformation_0.txt")).getInverse());
			transMRIfromMachine.addTransform(ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,-175,0,1,0,-175,0,0,1,-153.6}));
			ItkTransform transRXtoMRI= new ItkTransform((ItkTransform.readTransformFromFile(repSpec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Transforms2/trans_MRI_AUTO.txt")).getInverse());
			transRXtoMRI.addTransform(transMRIfromMachine);
			ImagePlus imgRX=IJ.openImage(repSpec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Stacks2/imgRef_RXForMRItif.tif");
			ImagePlus imgMRI=IJ.openImage(repSpec+"/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/stack_0.tif");

			ImagePlus imgRef=IJ.createImage("", 512, 512, 768, 8);
			VitimageUtils.adjustImageCalibration(imgRef, imgMRI);
			System.out.println(TransformUtils.stringVector(VitimageUtils.getVoxelSizes(imgMRI),""));
			System.out.println("Trans 1");
			imgRX=transRXtoMRI.transformImage(imgRef, imgRX,false);
			imgMRI=transMRIfromMachine.transformImage(imgRef,imgMRI,false);
			ImagePlus comp=VitimageUtils.compositeOf(imgRX, imgMRI);
			comp.setTitle(spec);
			IJ.saveAsTiff(imgMRI, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/mri_"+spec+".tif");
			IJ.saveAsTiff(imgRX, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/rx_"+spec+".tif");
			IJ.saveAsTiff(comp, "/home/fernandr/Bureau/Traitements/Cep5D/FieldsBias/composite_"+spec+".tif");
		}
	}
	
	public static void viewMRI() {
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		String dir="/home/fernandr/Bureau/Test/Bias/";
		String tmp;
		for(int i=0;i<12;i++) {
			System.out.println("Traitement "+specimen[i]);
			String spec=specimen[i];
			ImagePlus imgRef=IJ.openImage(
					"/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Stacks2/imgRef_RXForMRItif.tif");
			ImagePlus imgMov=IJ.openImage(
					"/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Stacks2/imgMov_MRI_registeredAuto.tif");
	
			imgRef.setDisplayRange(0,255);
			imgMov.setDisplayRange(0,i==3 ? 70 : 180);
			IJ.run(imgMov,"8-bit","");
			ImagePlus res=VitimageUtils.compositeNoAdjustOf(imgRef,imgMov);
		    VitimageUtils.showImageUntilItIsClosed(res,spec);
		}			
	}
	
	
	public static void pequenaJala() {
		String []specimen= {"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		String dir="/home/fernandr/Bureau/Test/Bias/";
		String tmp;
		for(int i=0;i<12;i++) {
			System.out.println("Traitement "+specimen[i]);
			String spec=specimen[i];
			ImagePlus imgRefHigh=IJ.openImage(
					"/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Stacks2/imgRef_RXForMRItif.tif");
			ImagePlus imgMovHigh=IJ.openImage(
					"/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/1_Registration/Stacks2/imgMov_MRI_registeredAuto.tif");
			imgMovHigh.setTitle("RES_0");
			imgRefHigh.setTitle("REF");
			ImagePlus imgRef=VitimageUtils.Sub222(imgRefHigh);
			ImagePlus imgMov=VitimageUtils.Sub222(imgMovHigh);
			tmp=spec+"REF.tif";imgRefHigh.setTitle(tmp);IJ.saveAsTiff(imgRefHigh,dir+tmp);
			tmp=spec+"RES_0.tif";imgMovHigh.setTitle(tmp);IJ.saveAsTiff(imgMovHigh,dir+tmp);
			int sigma=45;
			int nit=7;
			for(int config=0;config<3;config++) {
				
				int levelMax=3-config/2;
				int levelMin=3-(config+1)/2;
				System.out.println("Sigma="+sigma+" nit="+nit+" levelMin="+levelMin+" levelMax="+levelMax);
				int viewSlice=120;
				int nbIterations=nit;//14
				int neighXY=2;	int neighZ=0;
				int bSXY=7;		int bSZ=7;
				int strideXY=2;		int strideZ=3;
				ItkTransform transRet=null;
				BlockMatchingRegistration bmRegistration=new BlockMatchingRegistration(imgRef,imgMov,Transform3DType.DENSE,MetricType.SQUARED_CORRELATION,
							0,sigma,levelMin,levelMax,nbIterations,viewSlice,null,neighXY,neighXY,neighZ,bSXY,bSXY,bSZ,strideXY,strideXY,strideZ);
		//		bmRegistration.flagSingleView=true;
				bmRegistration.displayRegistration=0;
				bmRegistration.displayR2=false;
				transRet=bmRegistration.runBlockMatching(null);
				//bmRegistration.closeLastImages();
				bmRegistration.freeMemory();
				ImagePlus res=transRet.transformImage(imgRefHigh, imgMovHigh,false);
				res.setDisplayRange(0, 255);
				IJ.run(res,"8-bit","");
				tmp=spec+"RES_MAX_"+levelMax+"_MIN_"+levelMin+".tif";res.setTitle(tmp);IJ.saveAsTiff(res,dir+tmp);
			}
		}
	}
	
	


	public static void chiasse7() {
		String []specimen= {"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int i=0;i<12;i++) {
			System.out.println("Traitement "+specimen[i]);
			ImagePlus imgMask=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/RX/Computed_data/0_Stacks/mask.tif");
			VitimageUtils.adjustImageCalibration(imgMask, new double[] {0.6,0.6,0.6},"mm");
			IJ.saveAsTiff(imgMask,"/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/RX/Computed_data/0_Stacks/mask.tif");
			imgMask.close();
			System.out.print("1");

			ImagePlus imgRX=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/RX/Computed_data/0_Stacks/RX.tif");
			VitimageUtils.adjustImageCalibration(imgRX, new double[] {0.6,0.6,0.6},"mm");
			IJ.saveAsTiff(imgRX,"/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/RX/Computed_data/0_Stacks/RX.tif");
			imgRX.close();
			System.out.print("2");

			ImagePlus imgHyp=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/RX/Computed_data/3_HyperImage/hyperImage.tif");
			VitimageUtils.adjustImageCalibration(imgHyp, new double[] {0.6,0.6,0.6},"mm");
			IJ.saveAsTiff(imgHyp,"/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Source_data/RX/Computed_data/3_HyperImage/hyperImage.tif");
			imgHyp.close();
			System.out.print("3");
		
		
		}
	}
	
	
	public static void chiasse4() {
		ImagePlus test=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/For_Cedric/2_After_Segmentation/CEP022_APO3_after_step_2_segmentation.tif");		
		IJ.run(test,"8-bit","");
		ImagePlus[]result=VitimageUtils.meanAndVarOnlyValidValuesByte(test,5);
		result[0].show();
		result[1].show();
	}
	
	
	public static void chiasse3() {
		ImagePlus test=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/For_Cedric/2_After_Segmentation/CEP022_APO3_after_step_2_segmentation.tif");
		test.show();
		int nExp=4;
		int[]tabDiamXY=new int[] {5,6,7,8}; 
		int[]tabZup=new int[] {1,1,1,1,2,2,2,2,2,2,2,2};
		int[]tabZdown=new int[] {2,2,2,2,1,3,1,3,1,3,1,3};
		boolean[]tabBool=new boolean[] {true,true,true,true,false,false,false,false,false,false,false,false};
		ImagePlus res1;
		for(int i=0;i<nExp;i++) {
			System.out.println("");
			System.out.println("Traitements jeu de parametres "+i+" : "+"-"+tabDiamXY[i]+"--"+tabZup[i]+"-"+tabZdown[i]+"--"+tabBool[i]);
			System.out.println();
			res1=VitimageUtils.normalizationSliceRGB(test,tabDiamXY[i],tabDiamXY[i],tabZup[i],tabZdown[i],tabBool[i]);
			res1.setTitle(""+tabDiamXY[i]+"-"+tabDiamXY[i]+"--"+tabZup[i]+"-"+tabZdown[i]+"--"+tabBool[i]+"_RECTVERS.tif");
			IJ.saveAsTiff(res1,"/home/fernandr/Bureau/Test/TestEqualization/"+res1.getTitle());
		}

	}


	
	public static void makeTrainingHybride() {
		String []specimen= {"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		//(84-2) par 10  +1 = 8 : 2 12 22 32 42 52 62 72 82 
		//81-2 par 10 +1 = 8 = 2 12 22 32 42 52 62 72
		ImagePlus imgTab[]=new ImagePlus[12];
		int[]tabDim=new int[12];
		int nbTotOut=0;
		int max=0;
		int incr=0;
		int curSli=0;
		
		//Preparer les stacks
		for(int i=0;i<12;i++) {
			imgTab[i]=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Computed_data/0_Stacks/Stack_low_res.tif");
			tabDim[i]=imgTab[i].getStackSize();
			nbTotOut+=(tabDim[i]-2)/10+1;
			if(nbTotOut>max)max=nbTotOut;
		}
		
		//Recuperer les slices concernees
		ImagePlus []tabOut=new ImagePlus[nbTotOut];
		for(int it=0;it<max;it++) {
			curSli=2+10*it;
			for(int sp=0;sp<12;sp++) {
				if(curSli<=tabDim[sp]) {
					System.out.println("Out slice "+(incr+1)+" = img"+sp+"["+curSli+"]");
					imgTab[sp].setSlice(curSli);
					tabOut[incr]=imgTab[sp].crop();
					incr++;
				}
			}
		}
		ImagePlus ret=Concatenator.run(tabOut);
		ret.show();
	}

	
	public static void inspectAllPhoto() {
		int stepEnd=3;//
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3" ,"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int i=0;i<specimen.length;i++) {//	
			System.out.println("\n\n\nTraitement specimen "+specimen[i]+" jusqu a step "+stepEnd);
			String pathToData="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i];
			ImagePlus p2=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Computed_data/1_Registration/Stacks/img_low_after_step_1.tif");
			ImagePlus p=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Computed_data/2_Segmentation/wood_only_rgb_radius_4_pruned.tif");
			//IJ.run(p,"8-bit","");
			//IJ.run(p2,"8-bit","");
			//p=VitimageUtils.compositeOf(p, p2);
			p.show();
			p.setTitle("ppp.tif"+specimen[i]);
			boolean flag=false;
			int incr=0;
			while(!flag) {
				if(WindowManager.getImage("ppp.tif"+specimen[i])==null)flag=true;
				VitimageUtils.waitFor(1000);
				System.out.println(incr++);
			}
			p.close();
		}		
	}
	
	public static void publishResults() {
		int publishedStep=3;
		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3" ,"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int i=0;i<specimen.length;i++) {
			System.out.println(i);
			if(publishedStep==1) {			
				String outputDir="/home/fernandr/Bureau/Traitements/Cep5D/For_Cedric/1_After_PVC_Alignement/";
				String inputData="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Computed_data/1_Registration/Stacks/img_low_after_step_1.tif";
				ImagePlus img=IJ.openImage(inputData);
				img.setTitle(specimen[i]+"_after_step_1_pvc_alignement.tif");
				IJ.saveAsTiff(img, outputDir+specimen[i]+"_after_step_1_pvc_alignement.tif");
			}
			if(publishedStep==2) {			
				String outputDir="/home/fernandr/Bureau/Traitements/Cep5D/For_Cedric/2_After_Segmentation/";
				File f=new File(outputDir);
				f.mkdir();
				String inputData="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Computed_data/2_Segmentation/wood_only_rgb_radius_4_pruned.tif";
				ImagePlus img=IJ.openImage(inputData);
				img.setTitle(specimen[i]+"_after_step_2_segmentation.tif");
				IJ.saveAsTiff(img, outputDir+specimen[i]+"_after_step_2_segmentation.tif");
			}
			if(publishedStep==3) {			
				String outputDir="/home/fernandr/Bureau/Traitements/Cep5D/For_Cedric/3_After_Equalization/";
				File f=new File(outputDir);
				f.mkdir();
				String inputData="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i]+"/Computed_data/2_Segmentation/wood_only_rgb_equalized.tif";
				ImagePlus img=IJ.openImage(inputData);
				img.setTitle(specimen[i]+"_after_step_3_equalization.tif");
				IJ.saveAsTiff(img, outputDir+specimen[i]+"_after_step_3_equalization.tif");
			}
		}
	}
	
	public static void makeAllPhoto() {
		int stepEnd=7;//

		String []specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3" ,"CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int i=0;i<12;i++) {//specimen.length	
			System.out.println("\n\n\nTraitement specimen "+specimen[i]+" jusqu a step "+stepEnd);
			String pathToData="/home/fernandr/Bureau/Traitements/Cep5D/"+specimen[i];
			Photo_Slicing_Seq photo=new Photo_Slicing_Seq(pathToData,Capillary.HAS_NO_CAPILLARY,SupervisionLevel.AUTONOMOUS,specimen[i],ComputingType.COMPUTE_ALL);
			photo.start(stepEnd);
		}
	}
	
	
	public static void testCon() {
		ImagePlus imgTest=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/CEP019_S3_Martyr/Computed_data/1_Registration/maskOutPVC.tif");		
		ImagePlus res=VitimageUtils.connexe(imgTest, 200, 256, 0, 10E10, 6, 1, false);
		res.resetDisplayRange();
		res.show();
	}
	

	
	
	public static void testAssemblage() {
		ImagePlus canalR=IJ.openImage("/home/fernandr/Bureau/canalR.tif");
		ImagePlus canalG=IJ.openImage("/home/fernandr/Bureau/canalR.tif");
		ImagePlus canalJet=IJ.openImage("/home/fernandr/Bureau/canalJet.tif");
		IJ.run(canalJet,"8-bit","");
		ImagePlus maskJet=VitimageUtils.thresholdByteImage(canalJet, 1, 256);
		IJ.run(maskJet,"Invert","");
		IJ.run(maskJet, "Divide...", "value=255");
		canalR = new ImageCalculator().run("Multiply create", canalR, maskJet);
		canalG = new ImageCalculator().run("Multiply create", canalG, maskJet);
		IJ.run(canalR,"Red","");
		IJ.run(canalG,"Green","");
		IJ.run(canalJet,"Fire","");
		ImagePlus result=RGBStackMerge.mergeChannels(new ImagePlus[] {canalR,canalG,canalJet},false);
		result.show();
	}
	
	public static void testAssemblage2() {
		ImagePlus canalR=IJ.openImage("/home/fernandr/Bureau/canalR.tif");
		ImagePlus canalG=IJ.openImage("/home/fernandr/Bureau/canalR.tif");
		ImagePlus canalCorrR=IJ.openImage("/home/fernandr/Bureau/corrRed.tif");
		ImagePlus canalCorrG=IJ.openImage("/home/fernandr/Bureau/corrGreen.tif");
		ImagePlus canalCorrB=IJ.openImage("/home/fernandr/Bureau/corrBlue.tif");
	
		
		ImagePlus finalR = new ImageCalculator().run("Add create", canalR, canalCorrR);
		ImagePlus finalG = new ImageCalculator().run("Add create", canalG, canalCorrG);
		ImagePlus finalB = canalCorrB;
		IJ.run(finalR,"Red","");
		IJ.run(finalG,"Green","");
		IJ.run(finalB,"Blue","");
		ImagePlus result=RGBStackMerge.mergeChannels(new ImagePlus[] {finalR,finalG,finalB},false);
		result.show();
	}
	

	public static void testWeka() {
		
//		ImagePlus imgTest=IJ.openImage("/home/fernandr/Bureau/testST.tif");		
		ImagePlus imgTest=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/CEP019_S3_Martyr/Computed_data/0_Stacks/Stack_low_res.tif");		
		ImagePlus result=VitimageUtils.makeWekaSegmentation(imgTest,"/home/fernandr/Bureau/Traitements/Cep5D/CEP019_S3_Martyr/classifier_bois.model");
		imgTest.show();
		IJ.run(result, "8-bit", "");
		result.show();
	}

	
	public static void makeMartyr() {
		String repSource="/mnt/DD_COMMON/Data_VITIMAGE/Ceps_par_specimen/S3/";
		String repDest="/mnt/DD_COMMON/Data_VITIMAGE/Ceps_par_specimen/S3_Martyr/";
		for(int i=0;i<200;i++) {
			System.out.println(i);
			String numb=(i<100 ? "00"+i : "0"+i);
			String totSource=repSource+"DSC_"+numb+".jpg";
			String totDest=repDest+"DSC_"+numb+".tif";
			File f=new File(totSource);
			if(f.exists()) {
				ImagePlus imp = IJ.openImage(totSource);
				System.out.println("Ouverture image "+totSource);
				IJ.run(imp, "Scale...", "x=0.25 y=0.25 width=1500 height=1000 interpolation=Bilinear average create");
				ImagePlus img=IJ.getImage();
				IJ.saveAs(img, "Tiff", totDest);
				img.changes=false;
				img.close();
				
			}
		}		
	}
	

	public static void produceStackTestForPhoto() {
		String rep="/home/fernandr/Bureau/Test/TestPhotoStack/";
		ImagePlus imgRef=IJ.openImage(rep+"modele_pour_generer_stack_sub.tif");
		int nbImgs=50;
		double varX=100;
		double varY=100;
		double varTeta=20;
		IJ.run("Colors...", "foreground=white background=white selection=yellow");
		
		for(int i =0;i<nbImgs;i++) {			
			ImagePlus imgTemp=imgRef.duplicate();
			double dx=(-0.5*Math.random()) * varX;
			double dy=(-0.5*Math.random()) * varY;
			double dteta=(-0.5*Math.random()) * varTeta;
			System.out.println("i="+i+" .... teta="+dteta+" x="+dx+" y="+dy);
			IJ.run(imgTemp, "Rotate... ", "angle="+dteta+" grid=1 interpolation=Bilinear fill");
//			IJ.run(imgTemp, "Translate...", "x="+dx+" y="+dy+" interpolation=Bilinear fill");
			IJ.saveAsTiff(imgTemp,rep+"Stack/IMG_"+i+".tif");
		}
	}
	
	

	
	
	public static void makeAllTiImport() {
		String []specimen= {"B079_NP","B080_NP", "B081_NP", "B089_EL" ,"B090_EL" ,"B091_EL" ,"B098_PCH", "B099_PCH" ,"B100_PCH"};
//		int[]days= {0,35,70,105,133};
		int[]days= {105};

		for(int i=0;i<specimen.length;i++) {	
			for(int j=0;j<days.length;j++) {	
				for(int k=0;k<50;k++)System.out.println();
				System.out.println("Traitement specimen "+specimen[i]+" at day "+days[j]);
				String pathToData="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i]+"/Source_data/J"+days[j];
				File f=new File(pathToData);
				if(f.exists()) {
					System.out.println("\n\n\nProcessing "+pathToData);			
					MRI_T1_Seq mri1=new MRI_T1_Seq(pathToData+"/Source_data/MRI_T1_SEQ",
							Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,specimen[i]+"_J"+days[j]+"_MRI_T1",ComputingType.COMPUTE_ALL);
					mri1.start();
					MRI_T2_Seq mri2=new MRI_T2_Seq(pathToData+"/Source_data/MRI_T2_SEQ",
							Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,specimen[i]+"_J"+days[j]+"_MRI_T2",ComputingType.COMPUTE_ALL);
					mri2.start();
				}
			}
		}
	}
	
	public static void makeAllGeImport() {
		String []specimen= {"B079_NP","B080_NP", "B081_NP", "B089_EL" ,"B090_EL" ,"B091_EL" ,"B098_PCH", "B099_PCH" ,"B100_PCH"};
		int[]days= {0,35,70,105,133};
//		int[]days= {105};

		for(int i=0;i<specimen.length;i++) {	
			for(int j=0;j<days.length;j++) {	
				for(int k=0;k<50;k++)System.out.println();
				System.out.println("Traitement specimen "+specimen[i]+" at day "+days[j]);
				String pathToData="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i]+"/Source_data/J"+days[j];
				File f=new File(pathToData);
				if(f.exists()) {
					System.out.println("\n\n\nProcessing "+pathToData);			
					MRI_Ge3D mri1=new MRI_Ge3D(pathToData+"/Source_data/MRI_GE3D",
							Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,specimen[i]+"_J"+days[j]+"_MRI_GE2D",ComputingType.COMPUTE_ALL);
					mri1.start();
				}
			}
		}
	}
	
	
	
	public static void makeAllAssistedDetections(){
//		String []specimen= {"B079_NP","B080_NP", "B081_NP", "B089_EL" ,"B090_EL" ,"B091_EL" ,"B098_PCH", "B099_PCH" ,"B100_PCH"};
//		int[]days= {0,35,70,105,133};
		String []specimen= { "B099_PCH" };
		int[]days= {35};
		for(int i=0;i<specimen.length;i++) {	
			for(int j=0;j<days.length;j++) {	
				for(int k=0;k<50;k++)System.out.println();
				System.out.println("Traitement specimen "+specimen[i]+" at day "+days[j]);
				String pathToData="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i]+"/Source_data/J"+days[j];
				File f=new File(pathToData);
				if(f.exists()) {
					System.out.println("\n\n\nProcessing "+pathToData);			
					Vitimage4D viti = new Vitimage4D(VineType.CUTTING,0,"/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i]+"/Source_data/J"+days[j],
							specimen[i]+"_"+days[j],ComputingType.COMPUTE_ALL);
					viti.start(5);
				}
			}
		}
	}
	
	
	
	public static void finishAll4D(){
		String []specimen= {"B079_NP","B080_NP", "B081_NP", "B089_EL" ,"B090_EL" ,"B091_EL" ,"B098_PCH", "B099_PCH" ,"B100_PCH"};
		int[]days= {0,35,70,105,133};
		for(int i=0;i<specimen.length;i++) {	
			for(int j=0;j<days.length;j++) {	
				if(! ((i<0) || ((i==0) && (j<0)))) {
					for(int k=0;k<50;k++)System.out.println();
					System.out.println("Traitement specimen "+specimen[i]+" at day "+days[j]);
					String pathToData="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i]+"/Source_data/J"+days[j];
					File f=new File(pathToData);
					if(f.exists()) {
						System.out.println("\n\n\nProcessing "+pathToData);			
						Vitimage4D viti = new Vitimage4D(VineType.CUTTING,0,"/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i]+"/Source_data/J"+days[j],
								specimen[i]+"_"+days[j],ComputingType.COMPUTE_ALL);
						viti.start(10);
					}
				}
			}
		}
	}
	
	
	
	public static void makeAll5D(){
		String []specimen= {"B099_PCH" ,"B091_EL" ,"B079_NP","B080_NP", "B081_NP", "B089_EL" ,"B090_EL" ,"B098_PCH", "B100_PCH"};
//		String []specimen= {"B099_PCH"};
		for(int i=0;i<specimen.length;i++) {	
			if(! (i<0) ) {
				for(int k=0;k<50;k++)System.out.println();
				System.out.println("Traitement specimen "+specimen[i]);
				String pathToData="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i];
				System.out.println("\n\n\nProcessing "+pathToData);			
				Vitimage5D viti = new Vitimage5D(VineType.CUTTING,"/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i],
						specimen[i],ComputingType.COMPUTE_ALL);
				viti.start(5);
			}
		}
	}
	
	




	public static void makeAllAxisAndInocDetection() {
		String []specimen= {"B079_NP","B080_NP", "B081_NP", "B089_EL" ,"B090_EL" ,"B091_EL" ,"B098_PCH", "B099_PCH" ,"B100_PCH"};
		for(int i=0;i<specimen.length;i++) {	
			String pathToData="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i];
			System.out.println("\n\n\nProcessing "+pathToData);			

			Vitimage4D viti = new Vitimage4D(VineType.CUTTING,0,"/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+specimen[i],
					specimen[i],ComputingType.COMPUTE_ALL);			
			viti.start(5);//Stands for breaking before the beginning of fine automatic registration part
		}
	}
	
	
	
	public static void scheisse() {
		MRI_T1_Seq mri=new MRI_T1_Seq("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B099_PCH/Source_data/J0/Source_data/MRI_T1_SEQ",
				Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED,"B041_DS_J218_MRI_T1",ComputingType.COMPUTE_ALL);
mri.start();
		VitimageUtils.detectAxis(mri,50);
		
		
	}
	
	public static void importOnlyGe3dForVerif() {
		String repSource="/mnt/DD_COMMON/Data_VITIMAGE/Export_IRM_TEMP";
		String repCible="/mnt/DD_COMMON/Data_VITIMAGE/Export_IRM_TEMP/Pour_Cedric";
		String[]specimen=new String[] {"B079_NP" ,"B080_NP", "B081_NP", "B089_EL", "B090_EL" ,"B091_EL", "B098_PCH" ,"B099_PCH" ,"B100_PCH"};
		String[]days=new String[] {"J0", "J35", "J70" ,"J105" ,"J133"};
		ImagePlus temp;
		for(String spec : specimen) {
			for(String day : days) {
				File f=new File(repSource,spec+"/"+day+"/Ge3d/TR000100/TE000005");
				System.out.println("Traitement de "+spec+" - "+day);
				if(f.exists()) {
					temp = FolderOpener.open(f.getAbsolutePath(), "");
					temp.resetDisplayRange();
					IJ.run(temp,"Fire","");
					IJ.saveAsTiff(temp, repCible+"/"+spec+"_"+day+"_GE3D.tif");
				}				
			}			
		}
	}
	
	
	public static void testBM() {
//		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B099_PCH/Source_data/J0/Computed_data/0_Registration/imgRegistration_acq_2_step_afterIPalignment.tif");		
//		ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B099_PCH/Source_data/J0/Computed_data/0_Registration/imgRegistration_acq_2_step_afterIPalignment.tif");
		String dir="/home/fernandr/Bureau/Traitements/Cep5D/CEP020_APO1/Source_data/MRI_CLINICAL/Computed_data/3_HyperImage/";
		ImagePlus imgRef=IJ.openImage(dir+"stack_0.tif");		
		ImagePlus imgMov=IJ.openImage(dir+"stack_1.tif");	
		IJ.run(imgRef, "Scale...", "x=0.5 y=0.5 z=0.5 width="+imgRef.getWidth()+" height="+imgRef.getHeight()+" depth="+imgRef.getStackSize()+" interpolation=Bilinear average process create");
		imgRef=IJ.getImage();
		IJ.run(imgMov, "Scale...", "x=0.5 y=0.5 z=0.5 width="+imgRef.getWidth()+" height="+imgRef.getHeight()+" depth="+imgRef.getStackSize()+" interpolation=Bilinear average process create");
		imgMov=IJ.getImage();
		ItkTransform tr=BlockMatchingRegistration.blockMatchingRegistrationCepMRIData(imgRef,imgMov,null,true,true,true,true,10);
		tr=tr.flattenDenseField(imgRef);
		tr.writeAsDenseField("/home/fernandr/Bureau/Test/Field.tif", imgRef);
	}
	
	/*
	

	//Test pour vérifier le bien fondé de la matrice de passage à gauche et à droite, juste pour le reechantillonnage
	public static void testRandom4() {
		///Prendre l image blanc2
//		ImagePlus blablanc3=IJ.openImage("/home/fernandr/Bureau/Test/Random/blablanc3.tif");
		ImagePlus blamiblanc3=IJ.openImage("/home/fernandr/Bureau/Test/Random/blamiblablanc3.tif");
		ItkTransform trTest=get30Rotate();
		ImagePlus result=trTest.transformImageReech(blamiblanc3, blamiblanc3);
		blamiblanc3.show();
		result.show();
	}

		
		//Test pour vérifier le bien fondé de la matrice de passage à gauche et à droite, juste pour le reechantillonnage
	public static void testRandom3() {
		///Prendre l image blanc2
		ImagePlus blablanc3=IJ.openImage("/home/fernandr/Bureau/Test/Random/blablanc3.tif");
		ImagePlus blamiblanc3=IJ.openImage("/home/fernandr/Bureau/Test/Random/blamiblablanc3.tif");

		//La rotater puis la subsampler suivant X
		ItkTransform tr=get30Rotate();
		ImagePlus imgTrans=tr.transformImageReech(blamiblanc3, blablanc3);
		double []voxSRef=VitimageUtils.getVoxelSizes(blablanc3);
		double alpha=0;
		double []voxSMov=VitimageUtils.getVoxelSizes(blamiblanc3);
		Point3d[]tabPtRef=new Point3d [] {
			new Point3d(  (132+alpha)  * voxSRef[0],   (291+alpha)    * voxSRef[1],   0     * voxSRef[2]),
			new Point3d(  (132+alpha)  * voxSRef[0],   (116 +alpha)   * voxSRef[1],   0     * voxSRef[2]),
			new Point3d(  (303+alpha)  * voxSRef[0],   (115 +alpha)   * voxSRef[1],   0     * voxSRef[2]),
			new Point3d(  (146+alpha)  * voxSRef[0],   (369 +alpha)   * voxSRef[1],   0     * voxSRef[2]),
			new Point3d(  (317+alpha)  * voxSRef[0],   (368 +alpha)   * voxSRef[1],   0     * voxSRef[2]),
		};
		Point3d[]tabPtMov=new Point3d [] {
			new Point3d( (6 +alpha)  * voxSMov[0], (280 +alpha)    * voxSMov[1],   0     * voxSMov[2]),
			new Point3d( (23.5 +alpha)  * voxSMov[0], ( 129 +alpha)    * voxSMov[1],   0     * voxSMov[2]),
			new Point3d( (53 +alpha)  * voxSMov[0],  (214 +alpha)    * voxSMov[1],   0     * voxSMov[2]),
			new Point3d( (0.8 +alpha)  * voxSMov[0],  (355 +alpha)    * voxSMov[1],   0     * voxSMov[2]),
			new Point3d( (30.4 +alpha)  * voxSMov[0],  (440 +alpha)    * voxSMov[1],   0     * voxSMov[2]),
		};

		//Ouvrir les images, et detecter les correspondances
//		imgTrans.show();
		blablanc3.show();		
		ItkTransform trTest=ItkTransform.estimateBestRigid3D(tabPtMov,tabPtRef);
		ImagePlus result=trTest.transformImageReech(blablanc3, imgTrans);
		result.show();
//		blablanc2.show();
		
		//Construire la transformation a partir des correspondances
		//Resampler, et voir si ça marche
	}

*/

	//Test pour vérifier le bien fondé de la matrice de passage à gauche et à droite, juste pour le reechantillonnage
	public static void testRandom() {
		ImagePlus blablanc=IJ.openImage("/home/fernandr/Bureau/Test/Random/blablanc.tif");
		ImagePlus blablablanc=IJ.openImage("/home/fernandr/Bureau/Test/blablablanc.tif");
		ImagePlus imgOut1=ItkTransform.resampleImageReech(blablablanc,blablanc);
		ItkTransform tr=get30Rotate();
		ImagePlus imgOut2=ItkTransform.resampleImageReech(blablanc,imgOut1);
		blablanc.show();
		blablanc.setTitle("blablanc");
		blablablanc.setTitle("blablablanc");
		blablablanc.show();		
		imgOut1.show();
		imgOut1.setTitle("out1");
		imgOut2.show();
		imgOut2.setTitle("out2");
	}

	/*
	//Test pour vérifier le bien fondé de la matrice de passage à gauche et à droite, lorsque composé avec une transformation
	public static void testRandom2() {
		ImagePlus blablanc=IJ.openImage("/home/fernandr/Bureau/Test/Random/blablanc2.tif");
		ImagePlus blablablanc=IJ.openImage("/home/fernandr/Bureau/Test/Random/blablablanc2.tif");
		ItkTransform tr=get30Rotate();
		System.out.println(tr);
		ImagePlus imgOut1=new Duplicator().run(blablanc);
		for(int i=0;i<6;i++) {
			imgOut1=tr.transformImageReech(blablablanc,imgOut1);
			imgOut1=tr.transformImageReech(blablanc,imgOut1);
		}
		imgOut1.show();
		imgOut1.setTitle("out1");
		blablanc.show();
	}

	*/

	//Test pour vérifier le bien fondé de la matrice de passage à gauche et à droite, lorsque composé avec une transformation

	
	
	public static ItkTransform get30Rotate() {
		// (x'-50)=cosTeta(x-50)+sinTeta(x-50) => x'= xcosTeta+ysinTeta+50(1-cosTeta-sinTeta)
		// (y'-50)=-sinTeta(y-50)+cosTeta(y-50) => y'= x* -sinteta + y * cosTeta + 50 (1-cosTeta+sinTeta)
		double cosTeta=Math.sqrt(3)/2;
		double sinTeta=1.0/2;
		double[]mat=new double[] {
				cosTeta,sinTeta,0,100*(1-cosTeta-sinTeta),
				-sinTeta,cosTeta,0,100*(1-cosTeta+sinTeta),				
				0,0,1,0	
		};
		return ItkTransform.itkTransformFromCoefs(mat);
		
		
	}
	
	
	public static void chiachia() {
		ImagePlus I0=IJ.openImage("/home/fernandr/Bureau/Test/Spline/I0.tif");
		ImagePlus img0=IJ.openImage("/home/fernandr/Bureau/Test/Spline/dou0.tif");
		ImagePlus img25=IJ.openImage("/home/fernandr/Bureau/Test/Spline/dou25.tif");
		ImagePlus imgM25=IJ.openImage("/home/fernandr/Bureau/Test/Spline/douM25.tif");
		ImagePlus imgSplit=IJ.openImage("/home/fernandr/Bureau/Test/Spline/douM25UP25DOWN.tif");

		
		//Champ 1 : tout vers le bas.
		ImagePlus []imgTab1=new ImagePlus[3];
		imgTab1[0]=new Duplicator().run(img0);
		imgTab1[1]=new Duplicator().run(imgM25);
		imgTab1[2]=new Duplicator().run(img0);
		ItkTransform tr1=new ItkTransform(new Transform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(imgTab1)));
		
		//Champ 2 : split : en bas, à droite, en haut à gauche
		ImagePlus []imgTab2=new ImagePlus[3];
		imgTab2[0]=new Duplicator().run(imgSplit);
		imgTab2[1]=new Duplicator().run(img0);
		imgTab2[2]=new Duplicator().run(img0);
		ItkTransform tr2=new ItkTransform(new Transform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(imgTab2)));
		
		ImagePlus I1=tr1.transformImage(I0,I0,false);
		ImagePlus I2=tr2.transformImage(I1,I1,false);
		
		ItkTransform tr1add2=new ItkTransform(tr1);
		tr1add2.addTransform(new ItkTransform(tr2));

		ItkTransform tr2add1=new ItkTransform(tr2);
		tr2add1.addTransform(new ItkTransform(tr1));

		ImagePlus I2_2add1=tr2add1.transformImage(I0,I0,false);
		ImagePlus I2_1add2=tr1add2.transformImage(I0,I0,false);

		I0.setTitle("I0");
		I1.setTitle("I1");
		I2.setTitle("I2");
		I2_2add1.setTitle("I2_2add1");
		I2_1add2.setTitle("I2_1add2");
		I0.show();
		I1.show();
		I2.show();
		I2_2add1.show();
		I2_1add2.show();
	}
	
	public static void showAboutThisField(ItkTransform tr,ImagePlus imgRef, String title) {
		System.out.println("show...");
		tr.showAsGrid3D(imgRef,6,title,150);
		System.out.println("components...");
		ImagePlus[]imgTab=ItkImagePlusInterface.convertItkTransformToImagePlusArray(tr);
		for(int dim=0;dim<3;dim++) {
			imgTab[dim].show();
			IJ.run(imgTab[dim],"Fire","");
			imgTab[dim].setDisplayRange(-0.7, 0.7);
			imgTab[dim].setTitle(title+"_dim"+dim);
		}
	}
	
	public static void enormeChiasse() {
		System.out.println("Je ne suis plus sur de l'ordre dans lequel composer les transfos. C'est le sens de ce test");
		System.out.println("create...");
		ItkTransform tr02v1=new ItkTransform();
		ItkTransform tr02v2=new ItkTransform();
		ItkTransform tr20v1=new ItkTransform();
		ItkTransform tr20v2=new ItkTransform();
		ItkTransform tr00v1=new ItkTransform();
		ItkTransform tr00v2=new ItkTransform();

		System.out.println("open...");
		String ch="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_0_to_1.mhd";		
		ItkTransform tr01=ItkTransform.readFromDenseFieldWithITKImporter(ch);
		ch="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_1_to_0.mhd";		
		ItkTransform tr10=ItkTransform.readFromDenseFieldWithITKImporter(ch);

		System.out.println("open...");
		ch="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_1_to_2.mhd";		
		ItkTransform tr12=ItkTransform.readFromDenseFieldWithITKImporter(ch);
		ch="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_2_to_1.mhd";		
		ItkTransform tr21=ItkTransform.readFromDenseFieldWithITKImporter(ch);
		
		System.out.println("add...");
		tr02v1.addTransform(tr01);
		tr02v1.addTransform(tr12);

		tr02v2.addTransform(tr12);
		tr02v2.addTransform(tr01);


		System.out.println("add...");
		tr20v1.addTransform(tr21);
		tr20v1.addTransform(tr10);

		tr20v2.addTransform(tr10);
		tr20v2.addTransform(tr21);

		System.out.println("add...");
		tr00v1.addTransform(tr01);
		tr00v1.addTransform(tr10);
		tr00v2.addTransform(tr10);
		tr00v2.addTransform(tr01);

		
		System.out.println("open...");
		ImagePlus imgD0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		ImagePlus imgD2=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D2_registered.tif");
		
		System.out.println("transform...");
		ImagePlus imgD0to2_v1=tr02v1.transformImage(imgD0, imgD0,false);
		ImagePlus imgD0to2_v2=tr02v2.transformImage(imgD0, imgD0,false);
		System.out.println("transform...");
		ImagePlus imgD2to0_v1=tr20v1.transformImage(imgD2, imgD2,false);
		ImagePlus imgD2to0_v2=tr20v2.transformImage(imgD2, imgD2,false);
		System.out.println("transform...");
		ImagePlus imgD0to0_v1=tr00v1.transformImage(imgD0, imgD0,false);
		ImagePlus imgD0to0_v2=tr00v2.transformImage(imgD0, imgD0,false);
	
		
		System.out.println("composite...");
		VitimageUtils.compositeNoAdjustOf(imgD0, imgD2to0_v1,"2to0v1").show();
		VitimageUtils.compositeNoAdjustOf(imgD0, imgD2to0_v2,"2to0v2").show();
	
		VitimageUtils.compositeNoAdjustOf(imgD2, imgD0to2_v1,"0to2v1").show();
		VitimageUtils.compositeNoAdjustOf(imgD2, imgD0to2_v2,"0to2v2").show();
		
		VitimageUtils.compositeNoAdjustOf(imgD0, imgD0to0_v1,"0to0v1").show();
		VitimageUtils.compositeNoAdjustOf(imgD0, imgD0to0_v2,"0to0v2").show();
	}

	
	public static void petitCacaToutMou() {
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");

		System.out.println("read...");
		String chComp="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline01_80.mhd";
		ItkTransform tr=ItkTransform.readFromDenseFieldWithITKImporter(chComp);
		showAboutThisField(tr, imgRef, "01_80");

		System.out.println("read...");
		chComp="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline01_116.mhd";
		tr=ItkTransform.readFromDenseFieldWithITKImporter(chComp);
		showAboutThisField(tr, imgRef, "01_116");

		System.out.println("read...");
		chComp="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline01_116.mhd";
		tr=ItkTransform.readFromDenseFieldWithITKImporter(chComp);
		showAboutThisField(tr, imgRef, "12_0");
	}
	
	public static void tresGrosseChiasse() {
		System.out.println("read...");
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		String chComp="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline01_0.mhd";
		System.out.println("read...");
		ItkTransform tr=ItkTransform.readFromDenseFieldWithITKImporter(chComp);
		showAboutThisField(tr, imgRef, "2_0_glob");

		String ch01="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_0_to_1.mhd";		
		String ch12="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_1_to_2.mhd";
		
		System.out.println("read...");
		ItkTransform tr01=ItkTransform.readFromDenseFieldWithITKImporter(ch01);
		ItkTransform tr12=ItkTransform.readFromDenseFieldWithITKImporter(ch12);		
		ItkTransform tr02=ItkTransform.readFromDenseFieldWithITKImporter(ch01);
		System.out.println("add...");
		tr01.addTransform(tr12);
		tr01=tr01.flattenDenseField(imgRef);
		tr12=tr12.flattenDenseField(imgRef);
		System.out.println("show...");
		showAboutThisField(tr01, imgRef, "0_2_glob v01");
		showAboutThisField(tr, imgRef, "0_2_glob v02");

		System.out.println("invert...");
		ItkTransform tr20v1=tr01.getInverseOfDenseField();
		ItkTransform tr20v2=tr12.getInverseOfDenseField();
		System.out.println("show...");
		showAboutThisField(tr01, imgRef, "2_0_glob v01");
		showAboutThisField(tr, imgRef, "2_0_glob v02");
		
		
		//		ItkTransform tr0x=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline12_60.mhd");
//		ItkTransform tr=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline12_60.mhd");		
		
/*		ItkTransform tr00=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_0_to_0_global.mhd");
		tr00.showAsGrid3D(imgIn,6,"00",150);
		ItkTransform tr02=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_2_to_0_global.mhd");
		tr02.showAsGrid3D(imgIn,6,"02",150);

		ItkTransform tr03=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_3_to_0_global.mhd");
		tr03.showAsGrid3D(imgIn,6,"03",150);
	*/	
		
	}
	
	
	public static void grosseChiasse() {
		ImagePlus imgIn=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/seg12_33.tif");
		//imgIn=VitimageUtils.gaussianFiltering(imgIn, 0.1, 0.1, 0.1);
		imgIn.show();
		for(double sigma=0.013;sigma<0.02;sigma*=1.1) {
			System.out.println(sigma);
			ImagePlus img=new Duplicator().run(imgIn);
			IJ.run(img,"32-bit","");
			img=VitimageUtils.gaussianFiltering(img, sigma,sigma,sigma);
			img.setDisplayRange(0, 255);
			IJ.run(img,"8-bit","");
			img.show();
			img.setTitle("sigma="+sigma);
			IJ.run(img,"Fire","");
			img.setSlice(99);
		}
		VitimageUtils.waitFor(1000000);
	}
	
	public static void produceTestForSplineField() {
		ImagePlus img=ij.gui.NewImage.createImage("x",100,100,100,32,ij.gui.NewImage.FILL_BLACK);
		String rep="/home/fernandr/Bureau/Test/Spline/";
		IJ.run(img,"Multiply...","value=0 stack");
		IJ.saveAsTiff(img,rep+"val0.tif");
		ImagePlus img10=new Duplicator().run(img);
		IJ.run(img10,"Add...","value=10 stack");
		IJ.saveAsTiff(img10,rep+"val10.tif");
		ItkTransform tr1=new ItkTransform(new Transform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(new ImagePlus[] {img10,img,img})));
		ItkTransform tr2=new ItkTransform(new Transform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(new ImagePlus[] {img10,img10,img})));
		ItkTransform tr3=new ItkTransform(new Transform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(new ImagePlus[] {img10,img10,img10})));
		tr1.writeAsDenseFieldWithITKExporter(rep+"tr1.transform");
		tr2.writeAsDenseFieldWithITKExporter(rep+"tr2.transform");
		tr3.writeAsDenseFieldWithITKExporter(rep+"tr3.transform");
//		tr1.writeAsDenseField(rep+"tr1.transform", img);
		//		tr2.writeAsDenseField(rep+"tr2.transform", img);
		//tr3.writeAsDenseField(rep+"tr3.transform", img);		
	}
	
	
	public static void produceIntermediary() {
		String rep="/home/fernandr/Bureau/Test/Spline/";
		ItkTransform tr1=ItkTransform.readFromDenseFieldWithITKImporter(rep+"tr1.mhd");
		ItkTransform tr2=ItkTransform.readFromDenseFieldWithITKImporter(rep+"tr2.mhd");
		ItkTransform tr3=ItkTransform.readFromDenseFieldWithITKImporter(rep+"tr3.mhd");
		boolean makeSplines=true;
		double intermTime;
		int N=3+1;
		int X=100;
		int Y=100;
		int Z=100;
		double voxSX=1;
		double voxSY=1;
		double voxSZ=1;
		ImagePlus imgTmpX=ij.gui.NewImage.createImage("x",100,100,100,32,ij.gui.NewImage.FILL_BLACK);
		ImagePlus imgTmpY=ij.gui.NewImage.createImage("y",100,100,100,32,ij.gui.NewImage.FILL_BLACK);
		ImagePlus imgTmpZ=ij.gui.NewImage.createImage("z",100,100,100,32,ij.gui.NewImage.FILL_BLACK);
		float[][]tabTmpX=new float[Z][];
		float[][]tabTmpY=new float[Z][];
		float[][]tabTmpZ=new float[Z][];
		for(int z=0;z<Z;z++) {
			tabTmpX[z]=(float[]) imgTmpX.getStack().getProcessor(z+1).getPixels();
			tabTmpY[z]=(float[]) imgTmpY.getStack().getProcessor(z+1).getPixels();
			tabTmpZ[z]=(float[]) imgTmpZ.getStack().getProcessor(z+1).getPixels();
		}
		
		
		Point3d p0;
		Point3d pT;
		int index;
		double[]dataT=new double[N];
		for(int t=0;t<N;t++)dataT[t]=t;

		System.out.println("Allocation des tableaux");
		double[][][][][]tabVals=new double[X][Y][Z][3][N];
		PolynomialSplineFunction[][][][]tabFunc=new PolynomialSplineFunction[X][Y][Z][3];
		System.out.println("Lecture et conservation des données initiales. Dimension tableau, en Megadoubles = "+VitimageUtils.dou(X*Y*Z*N*3/1000000.0));
		for(int x=0;x<X;x++) {
			System.out.println("x="+x+"/"+X);
			for(int y=0;y<Y;y++) {
				for(int z=0;z<Z;z++) {
					p0=new Point3d(x*voxSX,y*voxSY,z*voxSZ);
					pT=p0;
					tabVals[x][y][z][0][0]=pT.x-p0.x;
					tabVals[x][y][z][1][0]=pT.y-p0.y;
					tabVals[x][y][z][2][0]=pT.z-p0.z;

					pT=tr1.transformPoint(p0);
					tabVals[x][y][z][0][1]=pT.x-p0.x;
					tabVals[x][y][z][1][1]=pT.y-p0.y;
					tabVals[x][y][z][2][1]=pT.z-p0.z;

					pT=tr2.transformPoint(p0);
					tabVals[x][y][z][0][2]=pT.x-p0.x;
					tabVals[x][y][z][1][2]=pT.y-p0.y;
					tabVals[x][y][z][2][2]=pT.z-p0.z;

					pT=tr3.transformPoint(p0);
					tabVals[x][y][z][0][3]=pT.x-p0.x;
					tabVals[x][y][z][1][3]=pT.y-p0.y;
					tabVals[x][y][z][2][3]=pT.z-p0.z;
					
					
					if(makeSplines) {
						tabFunc[x][y][z][0]=new SplineInterpolator().interpolate(dataT,tabVals[x][y][z][0]);
						tabFunc[x][y][z][1]=new SplineInterpolator().interpolate(dataT,tabVals[x][y][z][1]);
						tabFunc[x][y][z][2]=new SplineInterpolator().interpolate(dataT,tabVals[x][y][z][2]);						
					}
					
				}	
			}			
		}
	
	
	
		ImagePlus [] tabConcX=new ImagePlus[3*30];
		ImagePlus []tabConcY=new ImagePlus[3*30];
		ImagePlus []tabConcZ=new ImagePlus[3*30];
		int incr=0;
		System.out.println("Construction des champs de vecteurs");
		for(int da=0;da<3;da++) {
			for(int interp=0;interp<30;interp++) {
				intermTime=da+interp/30.0;
				System.out.println("da="+da+" et interp="+interp+" --> intermTime="+intermTime);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						index=x+X*y;
						for(int z=0;z<Z;z++) {
							tabTmpX[z][index]=(float)tabFunc[x][y][z][0].value(intermTime);
							tabTmpY[z][index]=(float)tabFunc[x][y][z][1].value(intermTime);
							tabTmpZ[z][index]=(float)tabFunc[x][y][z][2].value(intermTime);
						}
					}
				}
				IJ.saveAsTiff(imgTmpX,rep+"interp/tr"+da+"_"+interp+".x.tif");
				tabConcX[incr]=new Duplicator().run(imgTmpX);
				
				IJ.saveAsTiff(imgTmpY,rep+"interp/tr"+da+"_"+interp+".y.tif");
				tabConcY[incr]=new Duplicator().run(imgTmpY);

				IJ.saveAsTiff(imgTmpZ,rep+"interp/tr"+da+"_"+interp+".z.tif");
				tabConcZ[incr]=new Duplicator().run(imgTmpZ);
				
				incr++;
			}
		}
		ImagePlus imgFullX=Concatenator.run(tabConcX);
		imgFullX.show();
		imgFullX.setTitle("X");
		imgFullX.setDisplayRange(-2, 12);
		IJ.run(imgFullX,"Fire","");
		
		ImagePlus imgFullY=Concatenator.run(tabConcY);
		imgFullY.show();
		imgFullY.setTitle("Y");
		imgFullY.setDisplayRange(-2, 12);
		IJ.run(imgFullY,"Fire","");

		ImagePlus imgFullZ=Concatenator.run(tabConcZ);
		imgFullZ.show();
		imgFullZ.setTitle("Z");
		imgFullZ.setDisplayRange(-2, 12);
		IJ.run(imgFullZ,"Fire","");
		
		VitimageUtils.waitFor(1000000000);
	}	
	

	
	public static void testSpline0() {
		double[]dataT=new double[] {0,1,2,3};
		double[]dataValX=new double[] {0,3,3.5,4};
		PolynomialSplineFunction psf=new SplineInterpolator().interpolate(dataT,dataValX);
		System.out.print("X=[");
		for(double t=0;t<dataT.length-1;t+=0.1)System.out.print(" "+(t));
		System.out.println("]");
		System.out.print("Y=[");
		for(double t=0;t<dataT.length-1;t+=0.1)System.out.print(" "+psf.value(t));
		System.out.println("]");
		
	}
	
	
	public static void testGauss() {
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu/s010.tif");
		img.show();
		img.setSlice(241);
		double sigma=0.5;
		ImagePlus img2=VitimageUtils.gaussianFiltering(img, sigma, sigma, sigma);
		img2.show();
		img2.resetDisplayRange();
		img2.setSlice(241);
		VitimageUtils.waitFor(100000);
	}
	
	public static void testFields() {
		ImagePlus imgMovD0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		ImagePlus imgMovD1=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D1_registered.tif");
		ImagePlus imgMovD2=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D2_registered.tif");
		ImagePlus imgMovD3=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D3_registered.tif");
		System.out.println("Here1");
		ItkTransform tr10=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_1_to_0_global.mhd");
		ItkTransform tr20=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_2_to_0_global.mhd");
		ItkTransform tr30=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_3_to_0_global.mhd");
		System.out.println("Here2");
		ImagePlus imgRec1=tr10.transformImage(imgMovD1,imgMovD1,false);
		ImagePlus imgRec2=tr20.transformImage(imgMovD2,imgMovD2,false);
		ImagePlus imgRec3=tr30.transformImage(imgMovD3,imgMovD3,false);

		imgMovD0.show();
		imgRec1.show();
		imgRec2.show();
		imgRec3.show();
		System.out.println("Here6");
		ImagePlus tout=Concatenator.run(new ImagePlus[] {imgMovD0,imgRec1,imgRec2,imgRec3 });
		tout.show();
		VitimageUtils.waitFor(1000000);
	}
	
	
	
	
	public static void testFields2() {
		ImagePlus imgMov=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		System.out.println("Here1");
		ItkTransform tr01=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_0_to_1_sub.mhd");
		ItkTransform tr12=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_1_to_2_sub.mhd");
		ItkTransform tr23=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_2_to_3_sub.mhd");
		System.out.println("Here2");
		ItkTransform trFull=new ItkTransform();
		trFull.addTransform(tr01);
		trFull.addTransform(tr12);
		trFull.addTransform(tr23);
		System.out.println("Here3");
		ImagePlus imgTest1=trFull.transformImage(imgMov, imgMov,false);
		System.out.println("Here4");
		ImagePlus imgTest2=tr01.transformImage(imgMov, imgMov,false);
		System.out.println("Here5");
		imgTest2=tr12.transformImage(imgTest2,imgTest2,false);
		imgTest2=tr23.transformImage(imgTest2,imgTest2,false);
		System.out.println("Here6");
		imgTest1.show();
		imgTest2.show();
		VitimageUtils.waitFor(1000000);
	}
	
	
	public static void chiasse() {
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/Img_intermediary/D0_registered.tif");
		ImagePlus imgMov=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/Img_intermediary/D1_registered.tif");
		ItkTransform field=new ItkTransform(new DisplacementFieldTransform(
				ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(VitiDialogs.registrationPointsUI(10, imgRef,imgRef ,true), imgRef, 0.35, true)));
		ImagePlus test=field.transformImage(imgRef,imgMov,false);
		System.out.println("ComputeDense ok.");
		
		ImagePlus img=field.viewAsGrid3D(imgRef, 7);
		img.show();
		test.show();
		System.out.println("Grid ok.");
		VitimageUtils.waitFor(100000);
		System.exit(0);
		System.out.println("Lecture...");
//		ImagePlus imgMov=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/d1_to_0.tif");
		double varMin=0.05;
		int duration=0;
		boolean displayRegistration=true;
		ImagePlus mask=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/mask472.tif");
		boolean displayR2=false;
		int levMax =1;
		int levMin =0;
		int blockSize = 7;
		int neighSize = 3;
		double sigma  = 0.6 ;
		
		int dayI=0;
		int dayIPlus=dayI+1;		
		ItkTransform tr10=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(imgRef, imgMov, mask,Transform3DType.DENSE, MetricType.CORRELATION,
				levMax, levMin, blockSize, neighSize, varMin, sigma, duration, displayRegistration, displayR2,80,true);
		tr10.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/TEST_trans_"+dayIPlus+"_to_"+dayI+".mhd");
		System.out.print(" transform");
		ImagePlus res10=tr10.transformImage(imgRef, imgMov,false);
		IJ.saveAsTiff(res10, "/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/TESTd"+dayIPlus+"_to_"+dayI+".tif");
		res10.show();
			
		VitimageUtils.waitFor(1000000);
		
	}
	
	public static void test3Fields() {
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("Start : "+l1);
		int dayMax=1;
		double varMin=0.025;
		int duration=0;
		boolean displayRegistration=false;
		ImagePlus mask=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/mask512.tif");
		boolean displayR2=false;
		int levMax =2;
		int levMin =1;
		int blockSize = 3;
		int neighSize = 3;
		double sigma  = 0.7 ;
 
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 1 : registration forward. Start : "+(l1/1000.0));
		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;		
			System.out.print("Forward.  dayI="+dayI+"    open");
			ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+dayI+"_6_registered.tif");
			ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+dayIPlus+"_6_registered.tif");
			//imgRef=VitimageUtils.cropImageFloat(imgRef, 46*2, 55*2, 67*2, 160*2, 158*2, 128*2);
			//imgMov=VitimageUtils.cropImageFloat(imgMov, 46*2, 55*2, 67*2, 160*2, 158*2, 128*2);
			System.out.print(" sub");
			ImagePlus imgRefSub=VitimageUtils.Sub222(imgRef);
			ImagePlus imgMovSub=VitimageUtils.Sub222(imgMov);
			System.out.print(" bm ");
			ItkTransform tr10=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(imgRefSub, imgMovSub, mask,Transform3DType.DENSE, MetricType.CORRELATION,
					levMax, levMin, blockSize, neighSize, varMin, sigma, duration, displayRegistration, displayR2,80,false);
			System.out.print(" write");
			tr10.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/trans_"+dayIPlus+"_to_"+dayI+".mhd");
			System.out.print(" transform");
			ImagePlus res=tr10.transformImage(imgRef, imgMov,false);
			System.out.print(" save");
			IJ.saveAsTiff(res, "/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/d"+dayIPlus+"_to_"+dayI+".tif");
		}
		l1=-lStart+System.currentTimeMillis();System.out.println("forward accomplished : "+(l1/1000.0));
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 2 : registration backwards. Start : "+(l1/1000.0));
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.print("start");
			int dayIPlus=dayI+1;		
			System.out.print("Backwards. dayI="+dayI+"    open");
			ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+dayI+"_6_registered.tif");
			ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+dayIPlus+"_6_registered.tif");
			//imgRef=VitimageUtils.cropImageFloat(imgRef, 46*2, 55*2, 67*2, 160*2, 158*2, 128*2);
			//imgMov=VitimageUtils.cropImageFloat(imgMov, 46*2, 55*2, 67*2, 160*2, 158*2, 128*2);
			System.out.print(" sub");
			ImagePlus imgRefSub=VitimageUtils.Sub222(imgRef);
			ImagePlus imgMovSub=VitimageUtils.Sub222(imgMov); 
			System.out.print(" bm");
			ItkTransform tr01=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(imgRefSub, imgMovSub, mask,Transform3DType.DENSE, MetricType.CORRELATION,
					levMax, levMin, blockSize, neighSize, varMin, sigma, duration, displayRegistration, displayR2,80,false);
			System.out.print(" write");
			tr01.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/trans_"+dayI+"_to_"+dayIPlus+".mhd");
			System.out.print(" transform");
			ImagePlus res=tr01.transformImage(imgRef, imgMov,false);
			System.out.print(" save");
			IJ.saveAsTiff(res, "/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/d"+dayI+"_to_"+dayIPlus+".tif");
		}
		l1=-lStart+System.currentTimeMillis();System.out.println("backwards accomplished : "+(l1/1000.0));
	}
	
	
	public static void test4Fields() {

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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public static void testCorr() {
		int nTest=50000;
		int nCoefs=20;
		int coef1=3;
		int cofTmp;
		double[]vecta=vectRand(10,1,100,100,20);
		double[]vectb=vectRand(5,3,100,100,20);
		double [][]tabVals=new double[nCoefs][nTest];
		double []means=new double[nCoefs];
		double []stds=new double[nCoefs];
		double[]result;
		for(int nc=0;nc<nCoefs;nc++) {
			cofTmp=coef1+5*nc;
			for(int nt=0;nt<nTest;nt++) {
				vecta=vectRand(10,1,cofTmp,cofTmp,2);
				vectb=vectRand(5,3,cofTmp,cofTmp,2);
				tabVals[nc][nt]=correlationCoefficient(vecta, vectb);
				if((nc+nt)==0) {
					System.out.println("Une verif");
					System.out.println(TransformUtils.stringVectorN(vecta,"vecta"));
					System.out.println(TransformUtils.stringVectorN(vectb,"vectb"));
					System.out.println("corr="+tabVals[nc][nt]);
				}
			}
			result=VitimageUtils.statistics1D(tabVals[nc]);
			means[nc]=result[0];
			stds[nc]=result[1];
			System.out.println("Avec nb="+cofTmp+" : mean="+means[nc]+"  ,  std="+stds[nc]);
		}		
	}

	public static double correlationCoefficient(double X[], double Y[]) { 
		//System.out.println("En effet, X.length="+X.length);
		//System.out.println("En effet, Y.length="+Y.length);
		double epsilon=10E-20;
		if(X.length !=Y.length )IJ.log("In correlationCoefficient in BlockMatching, blocks length does not match");
		int n=X.length;
		double sum_X = 0, sum_Y = 0, sum_XY = 0; 
		double squareSum_X = 0, squareSum_Y = 0; 	
		for (int i = 0; i < n; i++) { 
			sum_X = sum_X + X[i]; 		
			sum_Y = sum_Y + Y[i]; 
			sum_XY = sum_XY + X[i] * Y[i]; 
			squareSum_X = squareSum_X + X[i] * X[i]; 
			squareSum_Y = squareSum_Y + Y[i] * Y[i]; 
		} 
		if(squareSum_X<epsilon || squareSum_Y<epsilon )return 0;
		// use formula for calculating correlation  
		// coefficient. 
		return (  (n * sum_XY - sum_X * sum_Y)/ (Math.sqrt((n * squareSum_X - sum_X * sum_X) * (n * squareSum_Y - sum_Y * sum_Y)))); 
	} 
	
	public static double[]vectRand(int a, int b, int N,int n,double sigma){
		Random rand=new Random();
		double[]vect=new double[n];
		for(int i=0;i<n;i++) {
			vect[i]=VitimageUtils.dou(a*i*1.0/N+b+rand.nextDouble()*sigma);
		}
		return vect;
	}
	
	
	
	
	public static int[][]getTestTabRXPhoto(){
		return new int[][]{
			{0,0,0,0},
			{4,4,4,4},
			{5,5,5,5},
			{10,10,10,10},
			{11,11,11,11},
			{16,16,16,15},
			{15,16,17,18},
			{21,22,23,23},
			{22,23,24,25},
			{27,28,29,30},
			{27,28,27,27},
			{30,30,30,30},
			{31,30,30,30},
			{38,38,39,39},
			{39,39,39,39},
			{41,40,39,39},
			{42,42,41,41},
			{47,47,47,47},
			{48,47,47,47},
			{54,54,53,52},
			{54,53,52,52},
			{59,59,58,57},
			{59,59,58,58},
			{64,63,62,61},
			{65,65,64,64},
			{70,70,70,70},
			{71,71,70,70},
			{77,77,76,76},
			{78,78,77,77},
			{83,82,81,80},
			{84,83,83,82},
			{91,90,89,88},
			{91,90,89,88},
			{96,95,94,93},
			{96,95,94,93},
			{101,101,101,101},
			{102,101,101,101},
			{107,108,107,106},
			{107,107,107,108},
			{112,112,111,110},
			{113,112,112,112},
			{118,117,117,118},
			{118,118,118,117},
			{121,121,121,122},
			{124,124,124,123},
			{127,128,128,127},
			{127,128,127,127},
			{132,133,133,133},
			{133,134,134,134},
			{140,141,140,140},
			{140,140,140,140},
			{148,149,148,148},
			{148,148,148,148},
			{159,158,157,156},
			{161,160,160,161},
			{163,163,162,162},
			{165,165,165,165},
			{168,169,170,171},
			{171,172,172,172},
			{174,175,174,174},
			{177,178,178,179},
			{180,180,180,180},
			{181,180,181,181},
			{187,188,188,189},
			{188,188,189,189},
			{190,190,190,190},
			{191,190,189,189},
			{199,200,200,200},
			{199,200,200,199},
			{204,204,203,203},
			{204,204,204,204},
			{211,211,212,213},
			{211,211,211,210},
			{217,218,217,218},
			{217,217,217,216},
			{221,222,221,221},
			{221,222,221,221},
			{226,226,227,227},
			{225,225,225,224},
			{231,232,233,234},
			{232,233,234,234},
			{237,237,236,235},
			{239,240,240,239},
			{245,246,247,247},
			{246,247,247,247},
			{250,249,250,250},
			{252,253,254,255},
			{256,256,255,256},
			{257,257,256,255},
			{261,260,259,258},
			{262,261,260,260},
			{267,266,265,265},
			{268,267,266,265},
			{272,271,270,269},
			{273,272,271,270},
			{278,277,276,277},
			{281,282,282,283},
			{284,285,286,287},
			{286,285,284,283},
			{286,286,286,287},
			{287,286,285,284},
			{292,293,293,294},
			{293,294,294,293},
			{299,300,300,299},
			{298,297,296,295},
			{303,302,301,300},
			{304,303,303,302},
			{310,309,308,307},
			{313,314,313,314},
			{316,315,314,314},
			{317,318,318,317},
			{324,324,323,324},
			{325,326,327,327},
			{330,330,331,331},
			{330,330,331,331},
			{334,334,334,333},
			{337,338,337,337},
			{342,343,344,344},
			{342,343,343,344},
			{349,350,350,350},
			{348,348,348,348},
			{353,353,354,355},
			{355,356,355,355},
			{361,362,363,362},
			{362,361,361,362},
			{367,367,367,367},
			{367,367,366,366},
			{372,373,372,372},
			{374,373,372,372},
			{380,381,380,380},
			{380,380,380,380},
			{386,387,387,387},
			{387,388,389,389},
			{390,391,390,390},
			{393,394,394,394},
			{397,398,397,397},
			{400,401,400,400},
			{403,402,401,401},
			{406,405,404,403},
			{411,411,410,410},
			{411,411,411,411},
			{419,420,419,419},
			{419,419,419,419},
			{425,426,426,426},
			{425,426,427,427},
			{430,430,430,429},
			{433,434,434,434},
			{437,437,436,435}
		};
	}
	
	
	
	
	
	public TestRomain() {
		// TODO Auto-generated constructor stub
	}

}
