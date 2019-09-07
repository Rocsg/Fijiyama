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
import java.sql.Date;
import java.util.ArrayList;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.ImageFileWriter;
import org.itk.simple.InverseDisplacementFieldImageFilter;
import org.itk.simple.ResampleImageFilter;
import org.itk.simple.Transform;

import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.Vitimage4D.VineType;
import com.vitimage.VitimageUtils.AcquisitionType;
import com.vitimage.VitimageUtils.Capillary;
import com.vitimage.VitimageUtils.ComputingType;
import com.vitimage.VitimageUtils.SupervisionLevel;

import Hough_Package.Hough_Circle;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.FolderOpener;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import math3d.Point3d;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.distanceMap3d.EDT;
import ij.measure.ResultsTable;
import trainableSegmentation.Trainable_Segmentation;

public class TestRomain {

	
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
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
		makeAllPhoto();
		//testHoughGameTransform();
		//makeTrainingHybride();
		//		testBalanceDesBlancs();
		//testDefineDepth();
		VitimageUtils.waitFor(1000000);
		System.exit(0);

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

	
	public static void makeAllPhoto() {
		int stepEnd=3;
		String []specimen= {"CEP015_RES2","CEP014_RES1","CEP016_RES3","CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int i=0;i<specimen.length;i++) {	
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
	
	public static void testDefineDepth() {
		int nSlices=60;
		int nDepthDef=11;
		int []depthUp=new int[nSlices];
		int []depthDown=new int[nSlices];
		boolean []virtuals=new boolean[nSlices];

		int[]sliDef=new int[nDepthDef];
		int[]depthDef=new int[nDepthDef];
		sliDef= new int[]{1,6,12,16,21,25,33,38,44,52,60};
		depthDef= new int[]{520,488,446,420,382,360,300,262,222,165,106};
		double thickMoy=(depthDef[0]-depthDef[nDepthDef-1])*1.0/(sliDef[nDepthDef-1]-sliDef[0]);

		for(int pt=0;pt<nDepthDef-1;pt++) {
			int curSlice=sliDef[pt]-1;
			int curDepth=depthDef[pt];
			int nextSlice=sliDef[pt+1]-1;
			int nextDepth=depthDef[pt+1];
			double thickBetween=(curDepth-nextDepth)*1.0/(nextSlice-curSlice);
			for(int sl=curSlice;sl<nextSlice;sl++) {
				depthUp[sl]=(int)Math.round(  curDepth- thickBetween*(sl-curSlice));
			}
		}
		depthUp[nSlices-1]=depthDef[nDepthDef-1];
		for(int pt=1;pt<nSlices;pt++)depthDown[pt-1]=depthUp[pt]+1;
		depthDown[nSlices-1]=depthUp[nSlices-1]-(int)Math.round(thickMoy);			

	
		for(int i=0;i<nSlices;i++) {
			System.out.println("Slice "+i+" up="+depthUp[i]+" down="+depthDown[i]+" , thickness="+(depthUp[i]-depthDown[i]));
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
	
	
	public static void chiasseAeffacerJusteApres() {
		ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B099_PCH/Source_data/J0/Computed_data/0_Registration/imgRegistration_acq_2_step_afterIPalignment.tif");
		ImagePlus imgMov=IJ.openImage("/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/B099_PCH/Source_data/J0/Computed_data/0_Registration/imgRegistration_acq_2_step_afterIPalignment.tif");
		ItkTransform tr=ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,0.124,0,1,0,0,0,0,1,0});
		imgMov=tr.transformImage(imgRef, imgMov);
		tr=BlockMatchingRegistration.setupAndRunStandardBlockMatchingWithoutFineParameterization(imgRef,imgMov,null,true,false,true);
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
		
		ImagePlus I1=tr1.transformImage(I0,I0);
		ImagePlus I2=tr2.transformImage(I1,I1);
		
		ItkTransform tr1add2=new ItkTransform(tr1);
		tr1add2.addTransform(new ItkTransform(tr2));

		ItkTransform tr2add1=new ItkTransform(tr2);
		tr2add1.addTransform(new ItkTransform(tr1));

		ImagePlus I2_2add1=tr2add1.transformImage(I0,I0);
		ImagePlus I2_1add2=tr1add2.transformImage(I0,I0);

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
		ImagePlus imgD0to2_v1=tr02v1.transformImage(imgD0, imgD0);
		ImagePlus imgD0to2_v2=tr02v2.transformImage(imgD0, imgD0);
		System.out.println("transform...");
		ImagePlus imgD2to0_v1=tr20v1.transformImage(imgD2, imgD2);
		ImagePlus imgD2to0_v2=tr20v2.transformImage(imgD2, imgD2);
		System.out.println("transform...");
		ImagePlus imgD0to0_v1=tr00v1.transformImage(imgD0, imgD0);
		ImagePlus imgD0to0_v2=tr00v2.transformImage(imgD0, imgD0);
	
		
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
		ImagePlus imgRec1=tr10.transformImage(imgMovD1,imgMovD1);
		ImagePlus imgRec2=tr20.transformImage(imgMovD2,imgMovD2);
		ImagePlus imgRec3=tr30.transformImage(imgMovD3,imgMovD3);

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
		ImagePlus imgTest1=trFull.transformImage(imgMov, imgMov);
		System.out.println("Here4");
		ImagePlus imgTest2=tr01.transformImage(imgMov, imgMov);
		System.out.println("Here5");
		imgTest2=tr12.transformImage(imgTest2,imgTest2);
		imgTest2=tr23.transformImage(imgTest2,imgTest2);
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
		ImagePlus test=field.transformImage(imgRef,imgMov);
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
		ItkTransform tr10=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(imgRef, imgMov, mask,Transformation3DType.DENSE, MetricType.CORRELATION,
				levMax, levMin, blockSize, neighSize, varMin, sigma, duration, displayRegistration, displayR2,80,true);
		tr10.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/TEST_trans_"+dayIPlus+"_to_"+dayI+".mhd");
		System.out.print(" transform");
		ImagePlus res10=tr10.transformImage(imgRef, imgMov);
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
			ItkTransform tr10=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(imgRefSub, imgMovSub, mask,Transformation3DType.DENSE, MetricType.CORRELATION,
					levMax, levMin, blockSize, neighSize, varMin, sigma, duration, displayRegistration, displayR2,80,false);
			System.out.print(" write");
			tr10.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/trans_"+dayIPlus+"_to_"+dayI+".mhd");
			System.out.print(" transform");
			ImagePlus res=tr10.transformImage(imgRef, imgMov);
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
			ItkTransform tr01=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(imgRefSub, imgMovSub, mask,Transformation3DType.DENSE, MetricType.CORRELATION,
					levMax, levMin, blockSize, neighSize, varMin, sigma, duration, displayRegistration, displayR2,80,false);
			System.out.print(" write");
			tr01.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Visu2_keep/champs/recalage_init/trans_"+dayI+"_to_"+dayIPlus+".mhd");
			System.out.print(" transform");
			ImagePlus res=tr01.transformImage(imgRef, imgMov);
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public TestRomain() {
		// TODO Auto-generated constructor stub
	}

}
