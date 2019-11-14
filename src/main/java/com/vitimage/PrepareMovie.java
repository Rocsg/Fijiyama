package com.vitimage;

//1  
//32  2 33 
//54  34 87
//3   88 90
//48  91 138
//26  139  165
//4   166  179

import java.util.ArrayList;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Transform;
//import org.python.core.packagecache.SysPackageManager;

import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.VitimageUtils.AcquisitionType;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import math3d.Point3d;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.distanceMap3d.EDT;

public class PrepareMovie {
	public static int INTERP_STEP=1;
	public static int NB_INTERP=120;
	public static int DAY_MAX=3;
	public static String TYPE_FIELD="";
	public static void main(String [] args) {
		ImageJ imag=new ImageJ();
//		smoothMushroom();
		//computeContinuousSegmentationBasedOnDistanceMapsRayCasting();
		//smoothMushroom();
		//testWithSplineInterpolation();
		//buildFullGlobalFieldsInvertedForSplines();
		//buildInterpolatedInvertedFieldsWithSplines();
		//subSampleRegistrationsWithSplineInterpolation();
		//checkIntermediaryStacksSpline();
//		checkIntermediaryStacks();
		resampleMaskAndSmoothTissuesUsingFieldsSpline();
		
		//		chiant();
//		phaseX();
		//produceIntermediaryStacks();
//		produceSuccessiveSlicesForSequence_05_RegistrationInVTK(1);
		//		produceSuccessiveSlicesForSequence_05_RegistrationInVTK(2);
		//produceSuccessiveSlicesForSequence_05_RegistrationInVTK(0);
		produceGhostImagesForSilhouette();
		//produceIntermediaryStacks();
		//		phaseY();
//		phaseZ();
		System.out.println("MAIN FINI !");
		//VitimageUtils.waitFor(1000000000);
//		makeSmoother2();
	}
	
	public PrepareMovie() {
		// TODO Auto-generated constructor stub
	}

	
	public static void smoothMushroom() {
		System.out.println("Start subsample registrations");
		int dayMax=PrepareMovie.DAY_MAX;
		int nbInterp=PrepareMovie.NB_INTERP;
		ImagePlus img,imgIn;
		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
				System.out.println("day="+dayI+" et i="+i);
				imgIn=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/segCont_"+dayI+""+dayIPlus+"_"+i+".tif");
				IJ.saveAsTiff(imgIn, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/segCont_"+dayI+""+dayIPlus+"_"+i+"_sigma_0.tif");
				for(int incr=4;incr<4;incr+=2) {
					double sigma=0.011+incr*0.004;
					img=new Duplicator().run(imgIn);
					IJ.run(img,"32-bit","");
					img=VitimageUtils.gaussianFiltering(img, sigma,0,sigma);
					img.setDisplayRange(0, 255);
					IJ.run(img,"8-bit","");
					IJ.saveAsTiff(img, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/segCont_"+dayI+""+dayIPlus+"_"+i+"_sigma_"+incr+".tif");
				}
			}
		}
	}
	
	
	

	//////////////PHASE Y/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////PHASE Y/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////PHASE Y/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//REGISTRATION AND INTERPOLATION PHASE
	public static void phaseY() {
		System.out.println("Start phase Y");
		//computeDenseRegistrations(80);
//		subSampleRegistrationsNoSpline();
//		produceIntermediaryStacks();
//		checkIntermediaryStacks();
//		////////////////resampleAndThresholdMaskUsingFields();

		
		//		gaussian();
		//produceIntermediaryStacks();
		//resampleMaskAndSmoothTissuesUsingFields();
//		produceGhostImagesForSilhouette();
		//produceFullImagesForMPR();
//		checkSuccessiveMasks();
	}

	public static void subSampleRegistrationsNoSpline() {
		System.out.println("Start subsample registrations");
		int dayMax=PrepareMovie.DAY_MAX;
		int nbInterp=PrepareMovie.NB_INTERP;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			System.out.print("Interpolation dayI="+dayI+" "+ (l1/1000.0)+"   open");
			ImagePlus img0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayI+"_registered.tif");
			ImagePlus img1=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayIPlus+"_registered.tif");
			System.out.println("Opening transfos...");
			ItkTransform tr01=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+dayI+"_to_"+dayIPlus+".mhd");
			ItkTransform tr10=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+dayIPlus+"_to_"+dayI+".mhd");
			tr01=VitimageUtils.Sub222Dense(tr01);
			tr10=VitimageUtils.Sub222Dense(tr10);
			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
				l1=-lStart+System.currentTimeMillis();System.out.println("start interp" + i+" : "+l1);
				ItkTransform tr03=(tr10.multiplyDenseField((fact*(i+1))));
				tr03=tr03.getInverseOfDenseField();

				ItkTransform tr13=(tr01.multiplyDenseField((1-fact*(i+1))));
				tr13=tr13.getInverseOfDenseField();
				
				tr03.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSub"+""+dayI+""+dayIPlus+"_"+i+".mhd");
				tr13.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSub"+""+dayIPlus+""+dayI+"_"+i+".mhd");

				ImagePlus test03=tr03.transformImage(img0, img0);
				IJ.run(test03,"Multiply...","value="+(1-fact*(i+1))+" stack");

				ImagePlus test13=tr13.transformImage(img1, img1);
				IJ.run(test13,"Multiply...","value="+(fact*(i+1))+" stack");

				ImagePlus imgSum=new ImageCalculator().run("Add create 32-bit stack",test03, test13) ;	
				imgSum.setDisplayRange(0, 1);
				IJ.run(imgSum,"8-bit","");
				IJ.saveAsTiff(imgSum,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples.tif");
				l1=-lStart+System.currentTimeMillis();System.out.println("sum done : "+(l1/1000.0));
			}
			l1=-lStart+System.currentTimeMillis();System.out.println("saving done "+l1);
		}
	}
	

	
	public static void buildFullGlobalFieldsInvertedForSplines() {
		System.out.println("Start subsample registrations");
		int dayMax=PrepareMovie.DAY_MAX;
		int nbInterp=PrepareMovie.NB_INTERP;
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/d1_to_0.tif");
		imgRef=VitimageUtils.Sub222(imgRef);
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
		ItkTransform[]tabTransInit=new ItkTransform[dayMax];
		ItkTransform[]tabTransInitComposed=new ItkTransform[dayMax];
		System.out.println("Lecture des champs");
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.println("Traitement champ numero "+dayI);
			int dayIPlus=dayI+1;
			tabTransInit[dayI]=VitimageUtils.Sub222Dense(ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+dayI+"_to_"+dayIPlus+".mhd"));
		}			
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.print("Construction transformation globale jour "+dayI+" : ");
			tabTransInitComposed[dayI]=new ItkTransform(tabTransInit[0]);
			System.out.print("Ajout trans "+(0)+""+(1));
			for(int dayJ=1;dayJ<=dayI;dayJ++) {
				tabTransInitComposed[dayI].addTransform(new ItkTransform(tabTransInit[dayJ]));
				System.out.print("Ajout trans "+(dayJ)+""+(dayJ+1)+"");
			}
			System.out.println("Inversion champ");
			tabTransInitComposed[dayI]=tabTransInitComposed[dayI].flattenDenseField(imgRef).getInverseOfDenseField();
			tabTransInitComposed[dayI].writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+(dayI+1)+"_to_0_global.mhd");
		}
		ImagePlus[] tabtr0=new ImagePlus[3];
		tabtr0[0]=new Duplicator().run(imgRef);
		IJ.run(tabtr0[0],"Multiply...","value=0 stack");
		tabtr0[1]=new Duplicator().run(tabtr0[0]);
		tabtr0[2]=new Duplicator().run(tabtr0[0]);
		new ItkTransform(new Transform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(tabtr0))).writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_0_to_0_global.mhd");		
	}

	public static void buildInterpolatedInvertedFieldsWithSplines() {
		ItkTransform trTmp;
		int dayMax=PrepareMovie.DAY_MAX;
		int nbInterp=PrepareMovie.NB_INTERP;
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/d1_to_0.tif");
		imgRef=VitimageUtils.Sub222(imgRef);
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
		ItkTransform[]tabTransGlobal=new ItkTransform[dayMax];
		System.out.println("Start spline interpolation of fields");
	
		//Lire tous les champs, les mettre dans un tableau
		System.out.println("Lecture des champs");
		for(int dayI=0;dayI<dayMax;dayI++) {
			tabTransGlobal[dayI]=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+(dayI+1)+"_to_0_global.mhd");
		}

		//Les spliner et sauvegarder les versions intermediaires
		double intermTime;
		int N=dayMax+1;
		Point3d p0;
		Point3d pT;
		int index;
		double[]dataT=new double[N];
		for(int t=0;t<N;t++)dataT[t]=t;

		int[]dims=VitimageUtils.getDimensions(imgRef);
		double[]voxS=VitimageUtils.getVoxelSizes(imgRef);
		int X=dims[0];
		int Y=dims[1];
		int Z=dims[2];
		double voxSX=voxS[0];
		double voxSY=voxS[1];
		double voxSZ=voxS[2];
		ImagePlus []fieldTmp=new ImagePlus[3];
		fieldTmp[0]=new Duplicator().run(imgRef);
		IJ.run(fieldTmp[0],"32-bit","");
		fieldTmp[1]=new Duplicator().run(fieldTmp[0]);
		fieldTmp[2]=new Duplicator().run(fieldTmp[0]);
		float[][][]tabFieldTmp=new float[3][Z][];
		for(int z=0;z<Z;z++) {
			for(int dim=0;dim<3;dim++) {
				tabFieldTmp[dim][z]=(float[]) fieldTmp[dim].getStack().getProcessor(z+1).getPixels();
			}
		}

		System.out.println("Allocation des tableaux");
		double[][][][][]tabVals=new double[X][Y][Z][3][N];
		PolynomialSplineFunction[][][][]tabFunc=new PolynomialSplineFunction[X][Y][Z][3];
		System.out.println("Lecture et conservation des données initiales. Dimension tableau, en Megadoubles = "+VitimageUtils.dou(X*Y*Z*N*3/1000000.0));
		for(int x=0;x<X;x++) {
			System.out.println("x="+x+"/"+X);
			for(int y=0;y<Y;y++) {
				for(int z=0;z<Z;z++) {
					p0=new Point3d(x*voxSX,y*voxSY,z*voxSZ);
					tabVals[x][y][z][0][0]=0;
					tabVals[x][y][z][1][0]=0;
					tabVals[x][y][z][2][0]=0;
					
					for(int day=0;day<dayMax;day++) {
						pT=tabTransGlobal[day].transformPoint(p0);
						tabVals[x][y][z][0][day+1]=pT.x-p0.x;
						tabVals[x][y][z][1][day+1]=pT.y-p0.y;
						tabVals[x][y][z][2][day+1]=pT.z-p0.z;
					}
					for(int dim=0;dim<3;dim++) {
						tabFunc[x][y][z][dim]=new SplineInterpolator().interpolate(dataT,tabVals[x][y][z][dim]);
					}	
				}
			}			
		}
	
	
		System.out.println("Construction des champs de vecteurs");
		for(int day=0;day<3;day++) {
			for(int interp=0;interp<=nbInterp;interp+=PrepareMovie.INTERP_STEP) {
				intermTime=day+interp*1.0/nbInterp;
				System.out.println("da="+day+" et interp="+interp+" --> intermTime="+intermTime);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						index=x+X*y;
						for(int z=0;z<Z;z++) {
							for(int dim=0;dim<3;dim++) {
								tabFieldTmp[dim][z][index]=(float)tabFunc[x][y][z][dim].value(intermTime);
							}
						}
					}
				}
				trTmp=new ItkTransform(new Transform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(fieldTmp)));
				trTmp.getInverseOfDenseField().writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline"+""+day+""+(day+1)+"_"+interp+".mhd");
			}
		}
	}	
	

	
	
	
	
	public static void subSampleRegistrationsWithSplineInterpolation() {
		int dayMax=PrepareMovie.DAY_MAX;
		int nbInterp=PrepareMovie.NB_INTERP;
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/d1_to_0.tif");
		imgRef=VitimageUtils.Sub222(imgRef);
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
		ItkTransform[]tabTransGlobal=new ItkTransform[dayMax+1];
		System.out.println("Start spline interpolation of fields");
	
		//Lire tous les champs, les mettre dans un tableau
		System.out.println("Lecture des champs");
		for(int dayI=0;dayI<=dayMax;dayI++) {
			tabTransGlobal[dayI]=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+(dayI)+"_to_0_global.mhd");
		}
		//tab0 = 0 to 0  , tab1= 1 to 0 , tab2 = 2 to 0 ...
		
		
		
		System.out.println("Start subsample registrations with spline interpolation");		
		l1=-lStart+System.currentTimeMillis();
		
		ItkTransform tr0x_vi,tr0x_vip,tri0,triP0;
		//Compute interpolation of fields
		for(int dayI=2;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			System.out.print("Interpolation dayI="+dayI+" "+ (l1/1000.0)+"   open");
			ImagePlus imgi=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayI+"_registered.tif");
			ImagePlus imgip=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayIPlus+"_registered.tif");

			
			
			for(int i=55;i<=nbInterp;i+=INTERP_STEP) {
				System.gc();
				l1=-lStart+System.currentTimeMillis();System.out.print("Day "+dayI+" - " + i+" : "+(l1/1000.0)+"   ");
				System.out.print("Lecture 0x...");
				System.out.print("ajout i0...");
				tr0x_vi=new ItkTransform(tabTransGlobal[dayI]);
				tr0x_vip=new ItkTransform(tabTransGlobal[dayI+1]);
				System.out.print("ajout ip0...");
				tr0x_vi.addTransform(ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline"+""+dayI+""+(dayIPlus)+"_"+i+".mhd"));
				tr0x_vip.addTransform(ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline"+""+dayI+""+(dayIPlus)+"_"+i+".mhd"));
	
				
	

				
				/*				tr0x_vi=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline"+""+dayI+""+(dayIPlus)+"_"+i+".mhd");
				tr0x_vip=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline"+""+dayI+""+(dayIPlus)+"_"+i+".mhd");
				tri0=tabTransGlobal[dayI];
				triP0=tabTransGlobal[dayIPlus];

				System.out.print("ajout i0...");
				tr0x_vi.addTransform(tri0);
				
				System.out.print("ajout ip0...");
				tr0x_vip.addTransform(triP0);
*/
				System.out.print("mult1...");
				ImagePlus testix=tr0x_vi.transformImage(imgi, imgi);
//				IJ.saveAsTiff(testix,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples_spline_for.tif");
				IJ.run(testix,"Multiply...","value="+(1-fact*(i+1))+" stack");

				System.out.print("mult2...");
				ImagePlus testipx=tr0x_vip.transformImage(imgip, imgip);
//				IJ.saveAsTiff(testipx,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples_spline_back.tif");
				IJ.run(testipx,"Multiply...","value="+(fact*(i+1))+" stack");

				System.out.print("ajout...");
				ImagePlus imgSum=new ImageCalculator().run("Add create 32-bit stack",testix, testipx) ;	
				imgSum.setDisplayRange(0, 1);
				IJ.run(imgSum,"8-bit","");
				System.out.print("save...");
				IJ.saveAsTiff(imgSum,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples_spline.tif");
				System.out.println();
			}
		}
	}
	

	public static void testWithSplineInterpolation() {
		int dayMax=PrepareMovie.DAY_MAX;
		int nbInterp=PrepareMovie.NB_INTERP;
		ImagePlus imgRef=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/d1_to_0.tif");
		imgRef=VitimageUtils.Sub222(imgRef);
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
	
		System.out.println("Start subsample registrations with spline interpolation");		
		l1=-lStart+System.currentTimeMillis();
		ImagePlus imgi=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		ImagePlus []imgTab=new ImagePlus[dayMax*(nbInterp/INTERP_STEP)];
		int incr=0;
		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			for(int i=0;i<nbInterp;i+=INTERP_STEP) {
				System.gc();
				l1=-lStart+System.currentTimeMillis();System.out.print("Day "+dayI+" - " + i+" : "+(l1/1000.0)+"   ");
				System.out.print("Lecture 0x...");
				System.out.print("ajout i0...");
				ItkTransform tr=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline"+""+dayI+""+(dayIPlus)+"_"+i+".mhd");
				imgTab[incr++]=tr.transformImage(imgi, imgi);
				System.out.println();
			}
		}
		ImagePlus imgFull=Concatenator.run(imgTab);
		imgFull.show();
		IJ.saveAsTiff(imgFull,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/testContinuSpline.tif");
		System.out.println();

	}
	
	
	
	
	
	
	public static void checkIntermediaryStacks() {
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		int theSlice=190;
		int incr=0;
		ImagePlus[]tabImg=new ImagePlus[(dayMax)*nbInterp/INTERP_STEP];
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.println("No spline Traitement jour "+dayI);
			int dayIPlus=dayI+1;
			for(int in=0;in<nbInterp;in+=INTERP_STEP) {
				System.out.println(" "+in);
				ImagePlus extract=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+in+"_samples.tif");
				tabImg[incr++]=new Duplicator().run(extract,theSlice,theSlice);
				extract.close();
			}
		}
		ImagePlus imgRet=Concatenator.run(tabImg);
		imgRet.show();
		IJ.saveAsTiff(imgRet, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/successive_2d_views.tif");
		VitimageUtils.waitFor(1000000);
	}
	
	
	
	public static void checkIntermediaryStacksSpline() {
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		int theSlice=190;
		int incr=0;
		ImagePlus[]tabImg=new ImagePlus[(dayMax)*nbInterp/INTERP_STEP];
		ImagePlus[]tabImgFor=new ImagePlus[(dayMax)*nbInterp/INTERP_STEP];
		ImagePlus[]tabImgBack=new ImagePlus[(dayMax)*nbInterp/INTERP_STEP];
		System.out.println("Et en effet, taille des tableaux="+tabImgBack.length);
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.println("Traitement jour "+dayI);
			int dayIPlus=dayI+1;
			for(int in=0;in<nbInterp;in+=INTERP_STEP) {
				System.out.println(" "+in);
				ImagePlus extract=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+in+"_samples_spline.tif");
				//ImagePlus extractFor=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+in+"_samples_spline_for.tif");
				//ImagePlus extractBack=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+in+"_samples_spline_back.tif");
				//tabImgFor[incr]=new Duplicator().run(extractFor,theSlice,theSlice);
				//tabImgBack[incr]=new Duplicator().run(extractBack,theSlice,theSlice);
				tabImg[incr++]=new Duplicator().run(extract,theSlice,theSlice);
				extract.close();
			}
		}
		ImagePlus imgRet=Concatenator.run(tabImg);
		imgRet.show();
		IJ.saveAsTiff(imgRet, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/successive_2d_views_spline.tif");
		VitimageUtils.waitFor(1000000);
	}
	
	
	
	
	
	
	public static void produceIntermediaryStacks() {
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
//		for(int dayI=0;dayI<dayMax;dayI++) {
		for(int dayI=0;dayI<1;dayI++) {
			System.out.print("Interpolation dayI="+dayI+" "+ (l1/1000.0)+"   open");
			int dayIPlus=dayI+1;
			System.out.println("Opening images...");
			ImagePlus img0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayI+"_registered.tif");
			ImagePlus img1=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayIPlus+"_registered.tif");
			System.out.println("Opening transfos...");
			ImagePlus imgSum;
			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
				l1=-lStart+System.currentTimeMillis();System.out.print("Stack production. Start d"+dayI+" - "+(i)+" at t="+(l1/1000.0)+"  ");
				System.out.print("read transfo...");
				ItkTransform trSample01=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSub"+""+dayI+""+dayIPlus+"_"+i+".mhd");
				System.out.print("transform I0...");
				ImagePlus test01=trSample01.transformImage(img0, img0);
				System.out.print("multiply I0...");
				test01.setDisplayRange(0, 1);
				IJ.run(test01,"8-bit","");
				IJ.saveAsTiff(test01,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/front_d_"+dayI+""+dayIPlus+"_"+i+"_samples.tif");
				IJ.run(test01,"Multiply...","value="+(1-fact*(i+1))+" stack");

				System.out.print("read transfo...");
				ItkTransform trSample10=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSub"+""+dayIPlus+""+dayI+"_"+i+".mhd");
				System.out.print("transform I0...");
				ImagePlus test10=trSample10.transformImage(img1, img1);
				System.out.print("multiply I0...");
				test10.setDisplayRange(0, 1);
				IJ.run(test10,"8-bit","");
				IJ.saveAsTiff(test10,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/back_d_"+dayI+""+dayIPlus+"_"+i+"_samples.tif");
				IJ.run(test10,"Multiply...","value="+(fact*(i+1))+" stack");

				System.out.print("compose I0 and I1...");
				imgSum=new ImageCalculator().run("Add create 32-bit stack",test01, test10) ;	
				System.out.print("save...");
				//IJ.saveAsTiff(imgSum,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples.tif");
				l1=-lStart+System.currentTimeMillis();System.out.println("sum done : "+(l1/1000.0));

			}
			l1=-lStart+System.currentTimeMillis();System.out.println("saving done "+l1);
		}
	}
	
	
	
	public static void produceSuccessiveSlicesForSequence_05_RegistrationInVTK(int todo) {
		if(todo==2) {
			ImagePlus []tab=new ImagePlus[776];
			for(int i=0;i<776;i++) {
				ImagePlus img=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/fusion/frame_"+i+".tif");
				tab[i]=img;
				System.out.println(i+" "+img.getBitDepth()+" "+img.getBytesPerPixel()+" "+img.getHeight()+" "+img.getWidth()+" "+img.getStackSize()); 
				
			}
			ImagePlus img2=Concatenator.run(tab);
			img2.show();
			VitimageUtils.waitFor(1000000);
			System.exit(0);
		}
		System.out.println("step1");
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		int slice=184;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
		int incrFrame=0;
		int incrTime=0;
		int incrDay=0;
		String repExport="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/fusion/";
		String repSource="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/";
		int videEntre=80;
		ImagePlus imgR0,imgG0,imgB0,imgR,imgG,imgB,imgTampon=null;
		ArrayList<ImagePlus>tabSucc=new ArrayList();
		
		
		//Sequence 1 : construire avec incr++ les images de la transition 1
		
		//Pour chaque incr,
		System.out.println("step2");
		
		imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
		imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+nbInterp+"_samples.tif");
		imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
		int dimX=imgG0.getWidth();
		int dimY=imgG0.getHeight();
		int dimZ=imgG0.getStackSize();
		int x_0=(videEntre+dimX)/2*0;
		int x_1=dimX+videEntre-(videEntre+dimX)/2*0;
		int x_full=(dimX+videEntre)/2;
		int fullX=dimX*2+videEntre;
		int fullY=dimY;
		int fullZ=1;
		imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
		imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
		imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
		System.out.println("Frame="+incrFrame);
		
		System.out.println("step3 : start");
		//de 0 a 30, on voit image 1 seul a gauche en vert
		for(int t=0;t<4;t++) {
			if(todo==1) {imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 0,t/4.0, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -green appearing");
			incrFrame++;
		}
		for(int t=0;t<26;t++) {
			if(todo==1) {imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 0,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -green is there");
			incrFrame++;
		}
		

		
		System.out.println("step4");
		//de 30 à 60, on voit image 1 à gauche en vert et image 2 à droite en rouge.
		//de 60 à 100, il ne se passe rien
		for(int t=0;t<4;t++) {
			if(todo==1) {imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, t/4.0,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -red appearing");
			incrFrame++;
		}
		for(int t=0;t<66;t++) {
			if(todo==1) {imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -red is there appearing");
			incrFrame++;
		}
				
		
		System.out.println("step5");
		//de 100 à 200, il y a la sinusoide
		for(int t=0;t<100;t++) {
			double varDist=0.5-0.5*Math.cos(Math.PI*((t+1)/100.0));
			double dist=(int)Math.round(varDist*(videEntre/2));
			x_0=(int)Math.round((videEntre+dimX)/2*(varDist));
			x_1=(int)Math.round(dimX+videEntre-(varDist)*(videEntre+dimX)/2);
			if(todo==1) {imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -sinusoide");
			incrFrame++;
		}
		
		for(int t=0;t<35;t++) {
			if(todo==1) {imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -idle");
			incrFrame++;
		}
				
		System.out.println("step6");

		//de 200 à 320, on recale img 2 sur img1
		for(int t=120;t>=0;t--) {
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+t+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -recalage");
			incrFrame++;
		}

		//pause
		for(int t=0;t<60;t++) {
			if(todo==1) {imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et size="+tabSucc.size()+" -idle");
			incrFrame++;
		}
		
		System.out.println("step7");

/*		//for(30) coupe 320
		//changer temps tous les 10 (3)
		incrTime=0;
		for(int t=0;t<30;t++) {
			if(t%10==0)incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime+" -moving time %10");
			incrFrame++;
		}

		
		System.out.println("step8");

		//for(28)
		//changer temps tous les 7 (7)
		for(int t=0;t<28;t++) {
			if(t%7==0)incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime+" -moving time %7");
			incrFrame++;
		}

		//for(30)
		//changer temps tous les 5 (13)
		for(int t=0;t<30;t++) {
			if(t%5==0)incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime+" -moving time %5");
			incrFrame++;
		}

		System.out.println("step9");

		//for(28)
		//changer temps tous les 4 (19)
		for(int t=0;t<24;t++) {
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime+" -moving time %10");
			incrFrame++;
		}

		
		//for(18)
		//changer temps tous les 3 (25)
		for(int t=0;t<18;t++) {
			if(t%3==0)incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime);
			incrFrame++;
		}
		System.out.println("step10");

		//for(14)
		//changer temps tous les 2 (32)
		for(int t=0;t<20;t++) {
			if(t%2==0)incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime);
			incrFrame++;
		}

	*/	
		//for(11)
		//changer temps tous les 1 jusqu'à 40
		incrTime=0;
		for(int t=0;t<40;t++) {
			incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, 0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime+" -moving time forward composite %1");
			incrFrame++;
		}
		System.out.println("step11");

		//fusion 1
		for(int t=0;t<15;t++) {
			incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1,1, t/15.0,0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime+" -moving time forward fuse 1");
			incrFrame++;
		}
		for(int t=0;t<15;t++) {
			incrTime++;
			if(todo==1) {imgR0=IJ.openImage(repSource+"back_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgR=VitimageUtils.getSliceUncropped(imgR0, slice, x_0,fullX);
			imgG0=IJ.openImage(repSource+"front_d_"+(incrDay)+""+(incrDay+1)+"_"+incrTime+"_samples.tif");
			imgG=VitimageUtils.getSliceUncropped(imgG0, slice, x_1,fullX);
			imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+incrTime+".tif");
			imgB=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			imgTampon=VitimageUtils.compositeRGBLByte(imgR, imgG,imgB,imgB, 1-(t+1)/15.0,1-(t+1)/15.0,1- (t+1)/15.0,(t+1)/15.0);
			IJ.run(imgTampon, "Stack to RGB", "");
			IJ.saveAsTiff(IJ.getImage(), repExport+"frame_"+incrFrame+".tif");
			IJ.getImage().close();}
			System.out.println("Frame="+incrFrame+" et instant="+incrTime+" -moving time forward fuse 2");
			incrFrame++;
		}

		//for(restant) 
		//montrer la fusion
		for(int t=incrTime+1;t<NB_INTERP;t++) {
			if(todo==1) {imgB0=IJ.openImage(repSource+"full"+(incrDay)+""+(incrDay+1)+"_"+t+".tif");
			imgTampon=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
			IJ.run(imgTampon, "RGB Color", "");
			IJ.saveAsTiff(imgTampon, repExport+"frame_"+incrFrame+".tif");
			tabSucc.add(imgTampon);}
			System.out.println("Frame="+incrFrame+" et forward jour=0 et instant="+t);
			incrFrame++;
		}
		for(int da=1;da<dayMax;da++) {
			for(int t=0;t<NB_INTERP+(da==dayMax-1 ? 1 : 0);t++) {
				if(todo==1) {imgB0=IJ.openImage(repSource+"full"+(da)+""+(da+1)+"_"+t+".tif");
				imgTampon=VitimageUtils.getSliceUncropped(imgB0, slice,x_full, fullX);
				IJ.run(imgTampon, "RGB Color", "");
				IJ.saveAsTiff(imgTampon, repExport+"frame_"+incrFrame+".tif");
				tabSucc.add(imgTampon);}
				System.out.println("Frame="+incrFrame+" et forward jour="+da+" et instant="+t);
				incrFrame++;
			}
		}
				
		System.out.println("step12");

	}
	


	
	public static void resampleMaskAndSmoothTissuesUsingFields() {
		System.out.println("Start resample and threshold mask using fields");
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));

		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			System.out.print("Interpolation masques et images sur dayI="+dayI+" "+ (l1/1000.0)+"   open");
			ImagePlus imgMaskBoth=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/both_D0.tif");
			
			ItkTransform trBaseToDayI=new ItkTransform();
			for(int i=0;i<dayI;i++) {
				trBaseToDayI.addTransform(ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+(i)+"_to_"+(i+1)+"_sub.mhd"));
			}
			
			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
				l1=-lStart+System.currentTimeMillis();System.out.print("Resample mask and tissues. Start d"+dayI+" - " +i+" at t="+(l1/1000.0));
				System.out.print(" transfo build... ");
				ItkTransform trCur=(new ItkTransform(trBaseToDayI));
				trCur.addTransform(	ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSub"+TYPE_FIELD+dayI+""+dayIPlus+"_"+i+".mhd"));
				

				System.out.print(" masque... ");
				ImagePlus maskBothTmp=trCur.transformImage(imgMaskBoth,imgMaskBoth);
				IJ.run(maskBothTmp, "Median...", "radius=2 stack");
				ImagePlus tmpCamb=VitimageUtils.thresholdByteImage(maskBothTmp, 0.5, 1.5);
				ImagePlus tmpVes=VitimageUtils.thresholdByteImage(maskBothTmp, 1.5, 2.5);
				ImagePlus tmpMoe=VitimageUtils.thresholdByteImage(maskBothTmp, 2.5, 3.5);
				maskBothTmp.setDisplayRange(0, 255);
				IJ.run(maskBothTmp, "8-bit", "");
				maskBothTmp.setDisplayRange(0, 3);

				System.out.print(" masquage... ");
				ImagePlus imgTemp = IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples.tif");				
				imgTemp.setDisplayRange(0, 1);
				IJ.run(imgTemp, "8-bit", "");
				IJ.saveAsTiff(imgTemp, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/full"+dayI+""+dayIPlus+"_"+i+".tif");
				ImageCalculator ic = new ImageCalculator();

				ImagePlus vesMasked = ic.run("AND create stack", imgTemp, tmpVes);
				IJ.run(vesMasked,"32-bit","");
				vesMasked=VitimageUtils.gaussianFiltering(vesMasked, 0.015,0.015,0.12);
				vesMasked.setDisplayRange(0, 255);
				IJ.run(vesMasked,"8-bit","");
				
				ImagePlus moeMasked = ic.run("AND create stack", imgTemp, tmpMoe);
				IJ.run(moeMasked,"32-bit","");
				moeMasked=VitimageUtils.gaussianFiltering(moeMasked,  0.03,0.03,0.07);
				moeMasked.setDisplayRange(0, 255);
				IJ.run(moeMasked,"8-bit","");

				ImagePlus cambMasked = ic.run("AND create stack", imgTemp, tmpCamb);
				IJ.run(cambMasked,"32-bit","");
				cambMasked=VitimageUtils.gaussianFiltering(cambMasked, 0.03,0.03,0.07);
				cambMasked.setDisplayRange(0, 255);
				IJ.run(cambMasked,"8-bit","");
				
				System.out.print(" sauvegardes... ");
				IJ.saveAsTiff(maskBothTmp, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/masques/both"+dayI+""+dayIPlus+"_"+i+".tif");

				IJ.saveAsTiff(vesMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/ves"+dayI+""+dayIPlus+"_"+i+"_gauss.tif");
				IJ.saveAsTiff(moeMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/moe"+dayI+""+dayIPlus+"_"+i+"_gauss.tif");
				IJ.saveAsTiff(cambMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/camb"+dayI+""+dayIPlus+"_"+i+"_gauss.tif");		
				System.out.println();
			}
			l1=-lStart+System.currentTimeMillis();System.out.println("saving done "+l1);
		}
	
		
		
	}


	
	
	
	
	public static void resampleMaskAndSmoothTissuesUsingFieldsSpline() {
		System.out.println("Start resample and threshold mask using fields");
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));
		ItkTransform trCur;
		ImagePlus imgMaskBoth=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/both_D0.tif");
//		for(int dayI=0;dayI<dayMax;dayI++) {
		for(int dayI=0;dayI<1;dayI++) {
			int dayIPlus=dayI+1;
			System.out.print("Interpolation masques et images sur dayI="+dayI+" "+ (l1/1000.0)+"   open");
//			for(int i=81;i<=nbInterp;i+=INTERP_STEP) {
			for(int i=113;i<=nbInterp;i+=INTERP_STEP) {
				l1=-lStart+System.currentTimeMillis();System.out.print("Resample mask and tissues. Start d"+dayI+" - " +i+" at t="+(l1/1000.0));
				System.out.print(" transfo build... ");
				trCur=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSubSpline"+""+dayI+""+(dayIPlus)+"_"+i+".mhd");
				System.out.print(" masque... ");
				ImagePlus maskBothTmp=trCur.transformImage(imgMaskBoth,imgMaskBoth);
				IJ.run(maskBothTmp, "Median...", "radius=2 stack");
				ImagePlus tmpCamb=VitimageUtils.thresholdByteImage(maskBothTmp, 0.5, 1.5);
				ImagePlus tmpVes=VitimageUtils.thresholdByteImage(maskBothTmp, 1.5, 2.5);
				ImagePlus tmpMoe=VitimageUtils.thresholdByteImage(maskBothTmp, 2.5, 3.5);
				maskBothTmp.setDisplayRange(0, 255);
				IJ.run(maskBothTmp, "8-bit", "");
				maskBothTmp.setDisplayRange(0, 3);

				System.out.print(" masquage... ");
				ImagePlus imgTemp = IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples_spline.tif");				
				imgTemp.setDisplayRange(0, 1);
				IJ.run(imgTemp, "8-bit", "");
				IJ.saveAsTiff(imgTemp, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/full"+dayI+""+dayIPlus+"_"+i+".tif");
				ImageCalculator ic = new ImageCalculator();

				ImagePlus vesMasked = ic.run("AND create stack", imgTemp, tmpVes);
				IJ.run(vesMasked,"32-bit","");
				vesMasked=VitimageUtils.gaussianFiltering(vesMasked, 0.015,0.015,0.12);
				vesMasked.setDisplayRange(0, 255);
				IJ.run(vesMasked,"8-bit","");
				
				ImagePlus moeMasked = ic.run("AND create stack", imgTemp, tmpMoe);
				IJ.run(moeMasked,"32-bit","");
				moeMasked=VitimageUtils.gaussianFiltering(moeMasked,  0.03,0.03,0.07);
				moeMasked.setDisplayRange(0, 255);
				IJ.run(moeMasked,"8-bit","");

				ImagePlus cambMasked = ic.run("AND create stack", imgTemp, tmpCamb);
				IJ.run(cambMasked,"32-bit","");
				cambMasked=VitimageUtils.gaussianFiltering(cambMasked, 0.03,0.03,0.07);
				cambMasked.setDisplayRange(0, 255);
				IJ.run(cambMasked,"8-bit","");
				
				System.out.print(" sauvegardes... ");
				IJ.saveAsTiff(maskBothTmp, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/masques/both"+dayI+""+dayIPlus+"_"+i+".tif");

				IJ.saveAsTiff(vesMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/ves"+dayI+""+dayIPlus+"_"+i+"_gauss.tif");
				IJ.saveAsTiff(moeMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/moe"+dayI+""+dayIPlus+"_"+i+"_gauss.tif");
				IJ.saveAsTiff(cambMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/camb"+dayI+""+dayIPlus+"_"+i+"_gauss.tif");		
				System.out.println();
			}
			l1=-lStart+System.currentTimeMillis();System.out.println("saving done "+l1);
		}
	
		
		
	}

	
	public static void resampleAndThresholdMaskUsingFields() {
		System.out.println("Start resample and threshold mask using fields");
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 3 : Interpolation. Start : "+(l1/1000.0));

		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			System.out.print("Interpolation masques et images sur dayI="+dayI+" "+ (l1/1000.0)+"   open");
			ImagePlus imgMaskBoth=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/both_D0.tif");
//			IJ.run(maskBothTmp, "32-bit", "");
			
			ItkTransform trBaseToDayI=new ItkTransform();
			for(int i=0;i<dayI;i++) {
				trBaseToDayI.addTransform(ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+(i)+"_to_"+(i+1)+"_sub.mhd"));
			}
			
			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
				l1=-lStart+System.currentTimeMillis();System.out.print("start interp" + i+" : "+(l1/1000.0));
				System.out.print(" transfo build... ");
				ItkTransform trCur=(new ItkTransform(trBaseToDayI));
				trCur.addTransform(	ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/transSub"+TYPE_FIELD+dayI+""+dayIPlus+"_"+i+".mhd"));
				

				System.out.print(" interpolation masque... ");
				ImagePlus maskBothTmp=trCur.transformImage(imgMaskBoth,imgMaskBoth);
				IJ.run(maskBothTmp, "Median...", "radius=2 stack");
				ImagePlus tmpCamb=VitimageUtils.thresholdByteImage(maskBothTmp, 0.5, 1.5);
				ImagePlus tmpVes=VitimageUtils.thresholdByteImage(maskBothTmp, 1.5, 2.5);
				ImagePlus tmpMoe=VitimageUtils.thresholdByteImage(maskBothTmp, 2.5, 3.5);
				maskBothTmp.setDisplayRange(0, 255);
				IJ.run(maskBothTmp, "8-bit", "");
				maskBothTmp.setDisplayRange(0, 3);

				System.out.print(" masquage... ");
				ImagePlus imgTemp = IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples.tif");				
				imgTemp.setDisplayRange(0, 1);
				IJ.run(imgTemp, "8-bit", "");
				ImageCalculator ic = new ImageCalculator();
				ImagePlus vesMasked = ic.run("AND create stack", imgTemp, tmpVes);
				ImagePlus moeMasked = ic.run("AND create stack", imgTemp, tmpMoe);
				ImagePlus cambMasked = ic.run("AND create stack", imgTemp, tmpCamb);
				
				System.out.print(" sauvegardes... ");
				IJ.saveAsTiff(maskBothTmp, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/masques/both"+dayI+""+dayIPlus+"_"+i+".tif");

				IJ.saveAsTiff(vesMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/ves"+dayI+""+dayIPlus+"_"+i+".tif");
				IJ.saveAsTiff(moeMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/moe"+dayI+""+dayIPlus+"_"+i+".tif");
				IJ.saveAsTiff(cambMasked, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/camb"+dayI+""+dayIPlus+"_"+i+".tif");		
				System.out.println();
			}
			l1=-lStart+System.currentTimeMillis();System.out.println("saving done "+l1);
		}
	
		
		
	}
		
	public static void produceGhostImagesForSilhouette() {
		System.out.println("Silhouette construction");
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nStart : "+(l1/1000.0));
		int incr=0;
		int nbTotal=dayMax*(nbInterp+1);
		ImagePlus imgTemp;
		for(int dayI=0;dayI<1;dayI++) {
			int dayIPlus=dayI+1;
			for(int i=0;i<1;i+=INTERP_STEP) {
				int totalTime=0;
				l1=-lStart+System.currentTimeMillis();
				if(i>0) {
					totalTime=(int)Math.round((l1/1000.0)/(++incr)*nbTotal);
				}
				System.out.print("Silhouette dayI="+dayI+" i="+i+"/"+nbInterp+" at t="+ (l1/1000.0)+". Estimated total time="+totalTime+"...  open...");
				imgTemp = IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples.tif");				
				System.out.print("set slice... ");
				imgTemp.setSlice(230);
				System.out.print("contrast... ");
				IJ.run(imgTemp, "Enhance Contrast", "saturated=0.35");
				System.out.print("threshold... ");
				IJ.setAutoThreshold(imgTemp, "Default dark");
				Prefs.blackBackground = true;
				System.out.print("mask... ");
				IJ.run(imgTemp, "Convert to Mask", "method=Default background=Dark black");
				System.out.print("holes... ");
				IJ.run(imgTemp, "Fill Holes", "stack");
				System.out.println("save... ");
				IJ.saveAsTiff(imgTemp,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/sil"+dayI+""+dayIPlus+"_"+i+".tif");
				System.out.println("Pouet : "+"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/sil"+dayI+""+dayIPlus+"_"+i+".tif");
//				IJ.run(imgTemp, "Median...", "radius=2 stack");
	//			System.out.println("save... ");
		//		IJ.saveAsTiff(imgTemp,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_interp/images/sil"+dayI+""+dayIPlus+"_"+i+"_median_2.tif");
			}
		}
		
	}

	public static void produceFullImagesForMPR() {
		System.out.println("Full image construction");
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		double fact=1.0/nbInterp;
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("\nStart : "+(l1/1000.0));
		int incr=0;
		int nbTotal=dayMax*(nbInterp+1);
		ImagePlus imgTemp;
		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
				int totalTime=0;
				l1=-lStart+System.currentTimeMillis();
				if(i>0) {
					totalTime=(int)Math.round((l1/1000.0)/(++incr)*nbTotal);
				}
				System.out.print("Silhouette dayI="+dayI+" i="+i+"/"+nbInterp+" at t="+ (l1/1000.0)+". Estimated total time="+totalTime+"...  open...");
				imgTemp = IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/samples/d_"+dayI+""+dayIPlus+"_t"+i+"_samples.tif");
				imgTemp.setDisplayRange(0,1);
				IJ.run(imgTemp,"8-bit","");
				IJ.saveAsTiff(imgTemp,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/full"+dayI+""+dayIPlus+"_"+i+".tif");
				System.out.println( "save:"+"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/full"+dayI+""+dayIPlus+"_"+i+".tif");
			}
		}
		
	}

	
	
	public static void checkSuccessiveMasks() {
		int dayMax=DAY_MAX;
		int nbInterp=NB_INTERP;
		int theSlice=190;
		ImagePlus[]tabImgCam=new ImagePlus[(dayMax)*(nbInterp/INTERP_STEP)];
		ImagePlus[]tabImgMoe=new ImagePlus[(dayMax)*(nbInterp/INTERP_STEP)];
		ImagePlus[]tabImgVes=new ImagePlus[(dayMax)*(nbInterp/INTERP_STEP)];
		int incr=0;
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.println("Traitement jour "+dayI);
			int dayIPlus=dayI+1;
			for(int in=0;in<nbInterp;in+=INTERP_STEP) {
				System.out.println(" "+in);
				ImagePlus extractCam=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/camb"+dayI+""+dayIPlus+"_"+in+"_gauss.tif");
				ImagePlus extractVes=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/moe"+dayI+""+dayIPlus+"_"+in+"_gauss.tif");
				ImagePlus extractMoe=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/ves"+dayI+""+dayIPlus+"_"+in+"_gauss.tif");

				tabImgCam[incr]=new Duplicator().run(extractCam,theSlice,theSlice);
				tabImgMoe[incr]=new Duplicator().run(extractMoe,theSlice,theSlice);
				tabImgVes[incr++]=new Duplicator().run(extractVes,theSlice,theSlice);

				extractCam.close();
				extractMoe.close();
				extractVes.close();
			}
		}
		ImagePlus imgRetCam=Concatenator.run(tabImgCam);
		ImagePlus imgRetMoe=Concatenator.run(tabImgMoe);
		ImagePlus imgRetVes=Concatenator.run(tabImgVes);
		imgRetCam.show();
		imgRetMoe.show();
		imgRetVes.show();
		IJ.saveAsTiff(imgRetCam, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/cambium_successive_2d_views.tif");
		IJ.saveAsTiff(imgRetVes, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/vessels_successive_2d_views.tif");
		IJ.saveAsTiff(imgRetMoe, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/moelle_successive_2d_views.tif");
		VitimageUtils.waitFor(1000000);
	}	
	

	
	
	//////////////PHASE Z/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////PHASE Z/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////PHASE Z/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// BUILD MUSHROOM IMAGES
	public static void phaseZ() {
		//computeMaskOfInocZone();
		//extractMushroomSegmentationFromInitialData();
		computeSuccessiveSegmentationBasedOnDistanceMapsRayCasting();
		smoothMushroom();
	}
	
	
	
	public static void computeMaskOfInocZone() {
		System.out.println("\nMovie preparation : computeMaskOfInocZone");
		IJ.setBackgroundColor(0, 0, 0);
		IJ.setForegroundColor(255, 255, 255);

		//Produce ellipse mask
		ImagePlus imgEllipsis=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D1_registered.tif");
		imgEllipsis.show();
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/Ellipse_area_tracked.roi");
		IJ.run(imgEllipsis, "Fill", "stack");		
		IJ.run(imgEllipsis, "Clear Outside", "stack");
		ImagePlus imgD0 = null;
		
		int dayMax=DAY_MAX;
		for(int dayI=0;dayI<dayMax+1;dayI++) {
			int dayIPlus=dayI+1;
			int dayIMoins=dayI-1;
			
			//Produce moelle + vessels + cambium mask		
			String nameImg="both"+(dayI==0 ? "01_0.tif" : ""+dayIMoins+""+dayI+"_"+(NB_INTERP)+".tif");
			ImagePlus tmp=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/masques/"+nameImg);
			ImagePlus imgObj=VitimageUtils.thresholdByteImage(tmp, 1.5, 255);
			
			//Compose both masks
			ImagePlus maskArea = new ImageCalculator().run("AND create stack", imgObj, imgEllipsis);
			if(dayI==0)imgD0=new Duplicator().run(maskArea);
			else {
				maskArea=new ImageCalculator().run("OR create stack", maskArea, imgD0);				
			}
			IJ.saveAsTiff(maskArea, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/mask_inoc_zone_D"+dayI+".tif");	
		}
		imgEllipsis.changes=false;
		imgEllipsis.close();
		
	}
	
	public static int []getParamsCrop() {
		return new int[] { 64, 200, 80, 171, 65, 300};
	}
	
	public static void extractMushroomSegmentationFromInitialData() {
		double threshHigh=0.15;
		int dayMax=DAY_MAX;
		int[]prmCrop=getParamsCrop();
		int sliceMinInocD0=184;
		int sliceMaxInocD0=247;
		
		ImagePlus segs[]=new ImagePlus[4];
		
		//Build mask of mush at D0
		System.out.println("Computation of mushroom position in initial data");
		segs[0]=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		IJ.run(segs[0],"8-bit","");
		segs[0].show();
		IJ.setBackgroundColor(0, 0, 0);
		IJ.setForegroundColor(255, 255, 255);
		for(int slice=1;slice<segs[0].getStackSize();slice++) {
			segs[0].setSlice(slice);
			IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/mush_at_D0.roi");
			if( slice>=sliceMinInocD0 && slice<sliceMaxInocD0) {
				IJ.run(segs[0], "Fill", "slice");	
				IJ.run(segs[0], "Clear Outside", "slice");
			}
			else {
				IJ.run(segs[0], "Select All", "");
				IJ.run(segs[0], "Clear", "slice");
			}
		}
		segs[0].show();
		VitimageUtils.waitFor(10000);
		ImagePlus mask=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/mask_inoc_zone_D0.tif");	
		segs[0]=new ImageCalculator().run("AND create stack", segs[0],mask);
		segs[0]=VitimageUtils.cropImageByte(segs[0],prmCrop[0],prmCrop[1],prmCrop[2],prmCrop[3],prmCrop[4],prmCrop[5]);
		
		
		//For each other day
		for(int dayI=1;dayI<dayMax+1;dayI++) {
			
			//Open img and mask
			mask=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/mask_inoc_zone_D"+dayI+".tif");	
			ImagePlus img=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayI+"_registered.tif");	

			//Compose them
			IJ.run(mask, "Select All", "");
			IJ.run(mask,"Divide...","value=255 stack");

			img=new ImageCalculator().run("Multiply create stack", img,mask);			
			img.setDisplayRange(0, 1);
			
			//Crop the result
			img=VitimageUtils.cropImageFloat(img,prmCrop[0],prmCrop[1],prmCrop[2],prmCrop[3],prmCrop[4],prmCrop[5]);
			ImagePlus img2=new Duplicator().run(img);
//			img2.show();
			img2.setTitle("D"+dayI+"_img");
			img2.setSlice(143);
			IJ.run(img2,"Fire","");
	
			//Filter it
			IJ.run(img, "Median...", "radius=2 stack");	
			ImagePlus img3=new Duplicator().run(img);
			//			img3.show();
			img3.setTitle("D"+dayI+"_median");
			img3.setSlice(143);
			IJ.run(img3,"Fire","");
			//Select greater low intensity connexe component
			segs[dayI]=VitimageUtils.connexe(img, 0.00001, threshHigh, 0, 10E10, 6, 1, false);
		}
		for(int i=0;i<=dayMax;i++) {
			segs[i].show();
			segs[i].setSlice(143);
			segs[i].setDisplayRange(0, 1);
			IJ.run(segs[i],"8-bit","");
			segs[i].setTitle("D"+i+"_connexe");
			IJ.saveAsTiff(segs[i], "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/seg_D"+i+".tif");
			segs[i].changes=false;
		}
		
	}
	
	public static void computeSuccessiveSegmentationBasedOnDistanceMaps() {
		float tMax=1.0001f;
		float tMin=-0.0001f;
		int nbInterp=NB_INTERP;
		int dayMax=DAY_MAX;
		int[]prmCrop=getParamsCrop();

		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			//Open the two segmentation images
			ImagePlus segs0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/seg_D"+dayI+".tif");
			ImagePlus segs1=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/seg_D"+dayIPlus+".tif");
			int X=segs0.getWidth();
			int Y=segs0.getHeight();
			int Z=segs0.getStackSize();
			int index=0;
			//Create the support images
			ImagePlus tStart=new Duplicator().run(segs1);
			IJ.run(tStart,"32-bit","");
			ImagePlus tStop=new Duplicator().run(tStart);
			IJ.run(tStart,"Multiply...","value=0 stack");
			IJ.run(tStop,"Multiply...","value=0 stack");
	
			//distance from seg1 to outside points
			IJ.run(segs1, "Invert", "stack");
			ImagePlus dist1In=EDT.run(ImageHandler.wrap(segs1), 0, true, 0).getImagePlus();	
			dist1In.changes=false;		
			dist1In.setTitle("dist_d"+dayIPlus+"_in.tif");
			dist1In.setDisplayRange(0, 0.3);
			IJ.run(dist1In,"Fire","");
			IJ.saveAsTiff(dist1In,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayIPlus+"_in.tif");
			dist1In.hide();
			
			//distance from seg1 outside contour to inside points
			IJ.run(segs1, "Invert", "stack");
			ImagePlus dist1Out=EDT.run(ImageHandler.wrap(segs1), 0, true, 0).getImagePlus();	
			dist1Out.changes=false;
			dist1Out.setTitle("dist_d"+dayIPlus+"_out.tif");
			dist1Out.setDisplayRange(0, 0.3);
			IJ.run(dist1Out,"Fire","");
			IJ.saveAsTiff(dist1Out,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayIPlus+"_out.tif");
			dist1Out.hide();
			
			//distance from seg0 to outside points
			IJ.run(segs0, "Invert", "stack");
			ImagePlus dist0In=EDT.run(ImageHandler.wrap(segs0), 0, true, 0).getImagePlus();	
			dist0In.changes=false;
			dist0In.setTitle("dist_d"+dayI+"_in.tif");
			dist0In.setDisplayRange(0, 0.3);
			IJ.run(dist0In,"Fire","");
			IJ.saveAsTiff(dist0In,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayI+"_in.tif");
			dist0In.hide();
			
			//distance from seg0 outside contour to inside points
			IJ.run(segs0, "Invert", "stack");
			ImagePlus dist0Out=EDT.run(ImageHandler.wrap(segs0), 0, true, 0).getImagePlus();	
			dist0Out.changes=false;
			dist0Out.setTitle("dist_d"+dayI+"_out.tif");
			IJ.run(dist0Out,"Fire","");
			dist0Out.setDisplayRange(0, 0.3);
			IJ.saveAsTiff(dist0Out,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayI+"_out.tif");
			dist0Out.hide();


		
			
			//Compute the support times for each voxel
			float[]tabDist0Out;
			float[]tabDist0In;
			float[]tabDist1Out;
			float[]tabDist1In;
			float[]tabStart;
			float[]tabStop;
			byte[]tabMask0;
			byte[]tabMask1;
			
			for(int z=1;z<=Z;z++) {
				tabDist0Out=(float[])dist0Out.getStack().getProcessor(z).getPixels();
				tabDist0In=(float[])dist0In.getStack().getProcessor(z).getPixels();
				tabDist1In=(float[])dist1In.getStack().getProcessor(z).getPixels();
				tabDist1Out=(float[])dist1Out.getStack().getProcessor(z).getPixels();
				tabStart=(float[])tStart.getStack().getProcessor(z).getPixels();
				tabStop=(float[])tStop.getStack().getProcessor(z).getPixels();
				tabMask0=(byte[])segs0.getStack().getProcessor(z).getPixels();
				tabMask1=(byte[])segs1.getStack().getProcessor(z).getPixels();

				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						index=y*X+x;
						if(  ((tabMask0[index] & 0xff) == 0) && ((tabMask1[index] & 0xff) == 0)  ) {//si point hors des deux objets
							tabStart[index]=tMax;tabStop[index]=tMin;
						}
						else if(  ((tabMask0[index] & 0xff) > 0) && ((tabMask1[index] & 0xff) > 0)  ) {//si point dans les deux objets
							tabStart[index]=tMin;tabStop[index]=tMax;							
						}
						else if(  ((tabMask0[index] & 0xff) > 0) && ((tabMask1[index] & 0xff) == 0)  ) {//si point dans S0 mais pas dans S1
							tabStart[index]=tMin;
							tabStop[index]=timing(tabDist0In[index],tabDist1Out[index]);
						}
						else if(  ((tabMask0[index] & 0xff) == 0) && ((tabMask1[index] & 0xff) > 0)  ) {//si point dans S1 mais pas dans S0
							tabStop[index]=tMax;
							tabStart[index]=timing(tabDist0Out[index],tabDist1In[index]);
						}
					}		
				}
			}
			tStart.setTitle("tStart_d"+dayI+".tif");
			tStop.setTitle("tStop_d"+dayI+".tif");
			tStart.setDisplayRange(0, 1);
			tStop.setDisplayRange(0, 1);
			IJ.saveAsTiff(tStart,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/tStart_d"+dayI+".tif");
			IJ.saveAsTiff(tStop,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/tStop_d"+dayI+".tif");
			
			
			
			
			//Sauvegarder le masque du champignon a chaque temps
			ImagePlus tempSeg;
			byte[]tabSeg;
//			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
			for(int i=0;i<0;i+=INTERP_STEP) {
				System.out.println("Interp "+i+" ... start !");
				double tCurrent=i*1.0/nbInterp;
				tempSeg=new Duplicator().run(segs0);
				IJ.run(tempSeg,"Multiply...","value=0 stack");
				
				//Compute the support times for each voxel
				
				for(int z=1;z<=Z;z++) {
					tabStart=(float[])tStart.getStack().getProcessor(z).getPixels();
					tabStop=(float[])tStop.getStack().getProcessor(z).getPixels();
					tabSeg=(byte[])tempSeg.getStack().getProcessor(z).getPixels();

					for(int x=0;x<X;x++) {
						for(int y=0;y<Y;y++) {
							index=y*X+x;
							if(  (tabStart[index] < tCurrent) && (tabStop[index] > tCurrent)  ) tabSeg[index]=(byte)(255 & 0xff);
						}		
					}
				}
				IJ.saveAsTiff(tempSeg,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/seg"+dayI+""+dayIPlus+"_"+i+".tif");
			}
		}
	}
	

	public static void computeSuccessiveSegmentationBasedOnDistanceMapsRayCasting() {
		float tMax=1.0001f;
		float tMin=-0.0001f;
		int nbInterp=NB_INTERP;
		int dayMax=DAY_MAX;
		int[]prmCrop=getParamsCrop();

		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;
			//Open the two segmentation images
			ImagePlus segs0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/seg_D"+dayI+".tif");
			ImagePlus segs1=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/seg_D"+dayIPlus+".tif");
			int X=segs0.getWidth();
			int Y=segs0.getHeight();
			int Z=segs0.getStackSize();
			int index=0;
			//Create the support images
			ImagePlus tStart=new Duplicator().run(segs1);
			IJ.run(tStart,"32-bit","");
			ImagePlus tStop=new Duplicator().run(tStart);
			IJ.run(tStart,"Multiply...","value=0 stack");
			IJ.run(tStop,"Multiply...","value=0 stack");
	
			//distance from seg1 to outside points
			IJ.run(segs1, "Invert", "stack");
			ImagePlus dist1In=EDT.run(ImageHandler.wrap(segs1), 0, true, 0).getImagePlus();	
			dist1In.changes=false;		
			dist1In.setTitle("dist_d"+dayIPlus+"_in.tif");
			dist1In.setDisplayRange(0, 0.3);
			IJ.run(dist1In,"Fire","");
			IJ.saveAsTiff(dist1In,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayIPlus+"_in.tif");
			dist1In.hide();
			
			//distance from seg1 outside contour to inside points
			IJ.run(segs1, "Invert", "stack");
			ImagePlus dist1Out=EDT.run(ImageHandler.wrap(segs1), 0, true, 0).getImagePlus();	
			dist1Out.changes=false;
			dist1Out.setTitle("dist_d"+dayIPlus+"_out.tif");
			dist1Out.setDisplayRange(0, 0.3);
			IJ.run(dist1Out,"Fire","");
			IJ.saveAsTiff(dist1Out,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayIPlus+"_out.tif");
			dist1Out.hide();
			
			//distance from seg0 to outside points
			IJ.run(segs0, "Invert", "stack");
			ImagePlus dist0In=EDT.run(ImageHandler.wrap(segs0), 0, true, 0).getImagePlus();	
			dist0In.changes=false;
			dist0In.setTitle("dist_d"+dayI+"_in.tif");
			dist0In.setDisplayRange(0, 0.3);
			IJ.run(dist0In,"Fire","");
			IJ.saveAsTiff(dist0In,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayI+"_in.tif");
			dist0In.hide();
			
			//distance from seg0 outside contour to inside points
			IJ.run(segs0, "Invert", "stack");
			ImagePlus dist0Out=EDT.run(ImageHandler.wrap(segs0), 0, true, 0).getImagePlus();	
			dist0Out.changes=false;
			dist0Out.setTitle("dist_d"+dayI+"_out.tif");
			IJ.run(dist0Out,"Fire","");
			dist0Out.setDisplayRange(0, 0.3);
			IJ.saveAsTiff(dist0Out,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/dist_d"+dayI+"_out.tif");
			dist0Out.hide();

			
			//structure for raycasting
			float[][][]arDist0In=new float[X][Y][Z];
			float[][][]arDist0Out=new float[X][Y][Z];
			float[][][]arDist1Out=new float[X][Y][Z];
			float[][][]arDist1In=new float[X][Y][Z];
			float[][][]arTStart=new float[X][Y][Z];
			float[][][]arTStop=new float[X][Y][Z];
			boolean[][][]arSeg0=new boolean[X][Y][Z];
			boolean[][][]arSeg1=new boolean[X][Y][Z];

			float[]tabDist0Out;
			float[]tabDist0In;
			float[]tabDist1Out;
			float[]tabDist1In;
			float[]tabStart;
			float[]tabStop;
			byte[]tabMask0;
			byte[]tabMask1;
			for(int z=1;z<=Z;z++) {
				tabDist0Out=(float[])dist0Out.getStack().getProcessor(z).getPixels();
				tabDist0In=(float[])dist0In.getStack().getProcessor(z).getPixels();
				tabDist1In=(float[])dist1In.getStack().getProcessor(z).getPixels();
				tabDist1Out=(float[])dist1Out.getStack().getProcessor(z).getPixels();
				tabMask0=(byte[])segs0.getStack().getProcessor(z).getPixels();
				tabMask1=(byte[])segs1.getStack().getProcessor(z).getPixels();

				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						index=y*X+x;
						arDist0In[x][y][z-1]=tabDist0In[index];
						arDist0Out[x][y][z-1]=tabDist0Out[index];
						arDist1In[x][y][z-1]=tabDist1In[index];
						arDist1Out[x][y][z-1]=tabDist1Out[index];
						arSeg0[x][y][z-1]=((tabMask0[index] & 0xff) > 0);
						arSeg1[x][y][z-1]=((tabMask1[index] & 0xff) > 0);
					}
				}
			}
			Object []obj=rayCastDistance(arDist0In,arDist0Out,arDist1In,arDist1Out,arSeg0,arSeg1);
			float[][][]arStart=(float[][][])obj[0];
			float[][][]arStop=(float[][][])obj[1];
				
			for(int z=1;z<=Z;z++) {
				tabStart=(float[])tStart.getStack().getProcessor(z).getPixels();
				tabStop=(float[])tStop.getStack().getProcessor(z).getPixels();
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						index=y*X+x;
						tabStop[index]=arStop[x][y][z-1];
						tabStart[index]=arStart[x][y][z-1];					
					}		
				}
			}
			tStart.setTitle("tStart_d"+dayI+".tif");
			tStop.setTitle("tStop_d"+dayI+".tif");
			tStart.setDisplayRange(0, 1);
			tStop.setDisplayRange(0, 1);
			IJ.saveAsTiff(tStart,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/tStart_d"+dayI+".tif");
			IJ.saveAsTiff(tStop,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/tStop_d"+dayI+".tif");
			
			tStart.show();
			tStop.show();

			
			
			
			//Sauvegarder le masque du champignon a chaque temps
			ImagePlus tempSeg;
			byte[]tabSeg;
			for(int i=0;i<=nbInterp;i+=INTERP_STEP) {
				System.out.println("Interp "+i+" ... start !");
				double tCurrent=i*1.0/nbInterp;
				tempSeg=new Duplicator().run(segs0);
				IJ.run(tempSeg,"Multiply...","value=0 stack");
				
				//Compute the support times for each voxel
				
				for(int z=1;z<=Z;z++) {
					tabStart=(float[])tStart.getStack().getProcessor(z).getPixels();
					tabStop=(float[])tStop.getStack().getProcessor(z).getPixels();
					tabSeg=(byte[])tempSeg.getStack().getProcessor(z).getPixels();

					for(int x=0;x<X;x++) {
						for(int y=0;y<Y;y++) {
							index=y*X+x;
							if(  (tabStart[index] < tCurrent) && (tabStop[index] > tCurrent)  ) tabSeg[index]=(byte)(255 & 0xff);
						}		
					}
				}
				IJ.saveAsTiff(tempSeg,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/seg"+dayI+""+dayIPlus+"_"+i+".tif");
			}
		}
	}
	


	
	
	
	
	
	
	
	
	
	
	
	

	public static void computeContinuousSegmentationBasedOnDistanceMapsRayCasting() {
		float tMax=1.0001f;
		float tMin=-0.0001f;
		int nbInterp=NB_INTERP;
		int dayMax=DAY_MAX;
		int[]prmCrop=getParamsCrop();
		ImagePlus imgStart=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/tStart_d0.tif");
		ImagePlus imgTime=new Duplicator().run(imgStart);
		ImagePlus imgB0=new Duplicator().run(imgStart);
		IJ.run(imgB0,"8-bit","");
		ImagePlus imgB1=new Duplicator().run(imgB0);
		ImagePlus imgB2=new Duplicator().run(imgB0);
		ImagePlus imgB3=new Duplicator().run(imgB0);
		int X=imgStart.getWidth();
		int Y=imgStart.getHeight();
		int Z=imgStart.getStackSize();
		float[][][]tabStart=new float[dayMax][Z][];
		float[][][]tabStop=new float[dayMax][Z][];
		float[][]tabTime=new float[Z][];
		byte[][]tabB0=new byte[Z][];
		byte[][]tabB1=new byte[Z][];
		byte[][]tabB2=new byte[Z][];
		byte[][]tabB3=new byte[Z][];
		double valMin=10E10;
		int index=0;
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.println("Getting data for day "+dayI);
			imgStart=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/tStart_d"+dayI+".tif");
			ImagePlus imgStop=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/tStop_d"+dayI+".tif");
//			imgStop.show();
	//		VitimageUtils.waitFor(100000);
			tabStart[dayI]=new float[Z][];
			tabStop[dayI]=new float[Z][];
			tabTime=new float[Z][];
			for(int z=1;z<=Z;z++) {
				tabStart[dayI][z-1]=(float[])imgStart.getStack().getProcessor(z).getPixels();
				tabStop[dayI][z-1]=(float[])imgStop.getStack().getProcessor(z).getPixels();
			}
		}
		double[]vals=new double[dayMax];
		for(int z=1;z<=Z;z++) {
			System.out.println("processing z="+z);
			tabTime[z-1]=(float[])imgTime.getStack().getProcessor(z).getPixels();
			tabB0[z-1]=(byte[])imgB0.getStack().getProcessor(z).getPixels();
			tabB1[z-1]=(byte[])imgB1.getStack().getProcessor(z).getPixels();
			tabB2[z-1]=(byte[])imgB2.getStack().getProcessor(z).getPixels();
			tabB3[z-1]=(byte[])imgB3.getStack().getProcessor(z).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					index=y*X+x;
					//Brasser les trois images.
					//prendre min sur les trois et l'affecter a un pixel
					valMin=10E10;
					for(int img=0;img<dayMax;img++) {
						if(tabStart[img][z-1][index]>0.999999999)vals[img]=10;
						else vals[img]=tabStart[img][z-1][index]+img;
						if(vals[img]<valMin)valMin=vals[img];
					}
					if(valMin>9.99)tabTime[z-1][index]=2000;//(float)(1000+dayMax+0.001)
					else tabTime[z-1][index]=(float)(valMin+1000);
					tabTime[z-1][index]=10000-tabTime[z-1][index];
					int valB0=(int)Math.floor(tabTime[z-1][index]/256);
					double stay=tabTime[z-1][index]-valB0*256;
					stay*=256;
					int valB1=(int)Math.floor(stay/256);
					stay=stay-256*valB1;
					stay*=256;
					int valB2=(int)Math.floor(stay/256);
					stay=stay-256*valB2;
					stay*=256;
					int valB3=(int)Math.floor(stay/256);
					tabB0[z-1][index]=(byte)(valB0 & 0xff);
					tabB1[z-1][index]=(byte)(valB1 & 0xff);
					tabB2[z-1][index]=(byte)(valB2 & 0xff);
					tabB3[z-1][index]=(byte)(valB3 & 0xff);

					
//					System.out.println("Chiffre initial etait : "+tabTime[z-1][index]);
					//					System.out.println("et tranduction : "+valB0+" "+valB1+" "+valB2+" "+valB3);
					//					if(valB2>0)System.exit(0);
				}
			}
		}
		IJ.saveAsTiff(imgTime,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/segContinuous.tif");
		IJ.saveAsTiff(imgTime,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/segContinuous.tif");
		IJ.saveAsTiff(imgB0,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/segB0.tif");
		IJ.saveAsTiff(imgB1,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/segB1.tif");
		IJ.saveAsTiff(imgB2,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/segB2.tif");
		IJ.saveAsTiff(imgB3,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/mushroom/segB3.tif");
		imgTime.show();
		
		
		ImagePlus imgMask=new Duplicator().run(imgTime);
		imgMask.setDisplayRange(8996.90, 8996.99);
		IJ.run(imgMask,"8-bit","");
		IJ.run(imgMask, "Invert", "stack");
		IJ.run(imgMask, "Divide...", "value=255.000 stack");
		IJ.run(imgMask, "Multiply...", "value=127 stack");
		
		ImagePlus imgTmp;
		for(int dayI=0;dayI<dayMax;dayI++) {
			System.out.println("Traitement jour "+dayI);
			int dayIPlus=dayI+1;
			for(int in=0;in<=nbInterp;in+=INTERP_STEP) {
				System.out.println(in);
				double valDown=9000-dayI-(in+1)/120.0-0.5;
				double valUp=9000-dayI-(in+1)/120.0+0.5;
				imgTmp=new Duplicator().run(imgTime);
				imgTmp.setDisplayRange(valDown, valUp);
				IJ.run(imgTmp,"8-bit","");
				//imgTmp = new ImageCalculator().run("Add create stack", imgTmp, imgMask);				
				IJ.saveAsTiff(imgTmp,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/mushroom/segCont_"+dayI+""+dayIPlus+"_"+in+".tif");
			}
		}
		
	
		
		
		
	}
	

	
	
	
	public static Object [] rayCastDistance(float[][][] dist0In,float[][][]dist0Out,float[][][]dist1In,float[][][]dist1Out,boolean[][][]seg0,boolean[][][]seg1) {
		float tMax=1.0001f;
		float tMin=-0.0001f;
		System.out.println("Starting ray cast");
		int X=dist0In.length;
		int Y=dist0In[0].length;
		int Z=dist0In[0][0].length;
		float[][][]tStart=new float[X][Y][Z];
		float[][][]tStop=new float[X][Y][Z];
		float[][][]rayDist=new float[X][Y][Z];

		
		int[][][]bestX=new int[X][Y][Z];
		int[][][]bestY=new int[X][Y][Z];
		int[][][]bestZ=new int[X][Y][Z];
		int[][][]targetX=new int[X][Y][Z];
		int[][][]targetY=new int[X][Y][Z];
		int[][][]targetZ=new int[X][Y][Z];
		for(int x=0;x<X;x++) {
			for(int y=0;y<Y;y++) {
				for(int z=0;z<Z;z++) {
					if(seg0[x][y][z]) {
						bestX[x][y][z]=x;
						bestY[x][y][z]=y;
						bestZ[x][y][z]=z;
					}
				}
			}
		}
		
		//Construction du tableau de distances
		double[][][]tabDist=new double[3][3][3];
		for(int indX=-1;indX<2;indX++) {
			for(int indY=-1;indY<2;indY++) {
				for(int indZ=-1;indZ<2;indZ++) {
					tabDist[indX+1][indY+1][indZ+1]=Math.sqrt( indX*indX + indY*indY + indZ*indZ );
				}
			}
		}
		tabDist[1][1][1]=10E10;
		double moyGliss=0;
		double factExp=0.001;
		int incr;
		//Passe 1 : pour chaque point de Seg1\Seg0, calculer le point source de Seg0
		System.out.println("points source");
		ArrayList<int[]> tabCoords=null;
		int currentX,currentY,currentZ;
		for(int x=1;x<X-1;x++) {
			for(int y=1;y<Y-1;y++) {
				for(int z=1;z<Z-1;z++) {
					incr=0;
					if(! seg0[x][y][z] && seg1[x][y][z] && bestX[x][y][z]==0) {
			//			System.out.print("  ("+x+","+y+","+z+") ");
//						System.out.println("enter");
						tabCoords=new ArrayList<int[]>();
						currentX=x;
						currentY=y;
						currentZ=z;
						tabCoords.add(new int[] {currentX,currentY,currentZ});
						boolean found=false;
						while(!found) {
							incr++;
							double currentDist=dist0Out[currentX][currentY][currentZ];
							//							System.out.println("\nboucle "+incr+" -->("+currentX+","+currentY+","+currentZ+") à distance "+currentDist);
							//Actualiser currentXYZ en allant chercher le voisin le plus proche et le plus bas dans la carte de distance dist0Out
							double deltaMin=10E10;
							double distanceMin=10E10;
							double delta;
							int indMinX=0;
							int indMinY=0;
							int indMinZ=0;
							for(int indX=-1;indX<2;indX++) {
								for(int indY=-1;indY<2;indY++) {
									for(int indZ=-1;indZ<2;indZ++) {
										delta=dist0Out[currentX+indX][currentY+indY][currentZ+indZ] - currentDist + tabDist[indX+1][indY+1][indZ+1];
										//		System.out.println("evaluation "+indX+","+indY+","+indZ+" delta="+delta);
										if(delta>45000&& delta <10E9) {
											System.out.println("\n\n\n\n\nC est la");
											System.out.println("En effet : currentDist="+currentDist);
											System.out.println("En effet : tabDist[indX+1][indY+1][indZ+1]="+tabDist[indX+1][indY+1][indZ+1]);
											System.out.println("En effet : dist0Out[currentX+indX][currentY+indY][currentZ+indZ]="+dist0Out[currentX+indX][currentY+indY][currentZ+indZ]);
											System.exit(0);
										}
										if(delta<deltaMin) {
											//System.out.println("est un min absolu");
											deltaMin=delta;
											indMinX=indX;
											indMinY=indY;
											indMinZ=indZ;
											distanceMin=tabDist[indX+1][indY+1][indZ+1];
										}
										else if(delta==deltaMin && tabDist[indX+1][indY+1][indZ+1]<distanceMin) {
											//System.out.println("est un min relatif, absolu sur la distance");
											deltaMin=delta;
											indMinX=indX;
											indMinY=indY;
											indMinZ=indZ;
											distanceMin=tabDist[indX+1][indY+1][indZ+1];											
										}
									}
								}
							}
							currentX+=indMinX;
							currentY+=indMinY;
							currentZ+=indMinZ;
							tabCoords.add(new int[] {currentX,currentY,currentZ});
							if(bestX[currentX][currentY][currentZ]>0) {
								//		System.out.println("trouvé !");
								found=true;
							}
							//else System.out.println("pas encore !");
							//if(incr>15)System.exit(0);
						}
						moyGliss=(moyGliss*(1-factExp))+factExp*incr;
//						System.out.println(" en "+incr+" coups, moyGliss="+moyGliss);
	//					VitimageUtils.waitFor(1);
						//retropropagation de bestX,Y,Z a tous les points de la chaine
						int[]coordsBest=tabCoords.get(tabCoords.size()-1);
						for(int ind=tabCoords.size()-2;ind>=0;ind--) {
							int[]coords=tabCoords.get(ind);
							//System.out.println("Retropropagation a ("+coords[0]+","+coords[1]+","+coords[2]+")");
							bestX[coords[0]][coords[1]][coords[2]]=bestX[coordsBest[0]][coordsBest[1]][coordsBest[2]];
							bestY[coords[0]][coords[1]][coords[2]]=bestY[coordsBest[0]][coordsBest[1]][coordsBest[2]];
							bestZ[coords[0]][coords[1]][coords[2]]=bestZ[coordsBest[0]][coordsBest[1]][coordsBest[2]];
						}
						tabCoords.clear();
					}
				}
			}
		}
		System.out.println();

		//Passe 2 : pour chaque point de Seg1\Seg0, calculer le point target de Seg1
		System.out.println("points cible");
		double facteurApproche=0.2;
		double currentXD,currentYD,currentZD;
		for(int x=1;x<X-1;x++) {
			//System.out.print(" x="+x+" ");
			for(int y=1;y<Y-1;y++) {
				for(int z=1;z<Z-1;z++) {
					if(! seg0[x][y][z] && seg1[x][y][z] && bestX[x][y][z]>0) {
						double[]deltaCoord=TransformUtils.multiplyVector(TransformUtils.normalize(new double[] {x-bestX[x][y][z], y-bestY[x][y][z], z-bestZ[x][y][z]}),facteurApproche);
						currentXD=currentX=x;
						currentYD=currentY=y;
						currentZD=currentZ=z;
						boolean found=false;
						while(!found) {
							//Actualiser currentXYZ en allant chercher le suivant
							currentXD+=deltaCoord[0];
							currentYD+=deltaCoord[1];
							currentZD+=deltaCoord[2];
							currentX=(int)Math.round(currentXD);
							currentY=(int)Math.round(currentYD);
							currentZ=(int)Math.round(currentZD);
							if(! seg1[currentX][currentY][currentZ]) {
								found=true;
							}
						}
						targetX[x][y][z]=currentX;
						targetY[x][y][z]=currentY;
						targetZ[x][y][z]=currentZ;
					}
				}
			}
		}
		System.out.println();

		
		
		
		//Passe 3 : calcul explicite des cartes tStart et tStop
		System.out.println("calcul cartes temporelles");
		int nb=0;
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					if(  !seg0[x][y][z] && !seg1[x][y][z]  ) {//si point hors des deux objets
						tStart[x][y][z]=tMax;tStop[x][y][z]=tMin;
					}
					else if(  seg0[x][y][z] && seg1[x][y][z]  ) {//si point dans les deux objets
						tStart[x][y][z]=tMin;tStop[x][y][z]=tMax;							
					}
					else if( seg0[x][y][z] && !seg1[x][y][z] ) {//si point dans S0 mais pas dans S1
						tStart[x][y][z]=tMin;
						tStop[x][y][z]=timing(dist0In[x][y][z],dist1Out[x][y][z]);
					}
					else if( !seg0[x][y][z] && seg1[x][y][z]  ) {//si point dans S1 mais pas dans S0
						tStop[x][y][z]=tMax;
						if(false && targetX[x][y][z] != x && targetY[x][y][z] != y && targetZ[x][y][z] != z ) {
							System.out.println("\nxyz="+x+","+y+","+z+"");
							System.out.println("Bestxyz="+bestX[x][y][z]+","+bestY[x][y][z]+","+bestZ[x][y][z]+"");
							System.out.println("Targetxyz="+targetX[x][y][z]+","+targetY[x][y][z]+","+targetZ[x][y][z]+"");
							System.out.println("et resultat= "+timing(x,y,z,  bestX[x][y][z] , bestY[x][y][z], bestZ[x][y][z]    , targetX[x][y][z], targetY[x][y][z], targetZ[x][y][z]));
							if(nb++>10)System.exit(0);
						}
						tStart[x][y][z]=timing(x,y,z,  bestX[x][y][z] , bestY[x][y][z], bestZ[x][y][z]    , targetX[x][y][z], targetY[x][y][z], targetZ[x][y][z]);
					}
				}		
			}
		}
		return new Object[] {tStart,tStop};
	}	
	
	
	public static float timing(double x,double y,double z   , double x0,double y0,double z0     ,double xf,double yf,double zf) {
		return (float)(  TransformUtils.norm(new double[] {x-x0,y-y0,z-z0})/TransformUtils.norm(new double[] {xf-x0,yf-y0,zf-z0})  );
	}
	
	
	public static float timing(float d1,float d2) {
		return d1/(d1+d2);
	}
	
	
	
	
	//////////////PHASE X/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////PHASE X/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////PHASE X/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//PHASE FOR IMAGES PREPARATION (NORMALIZATION), AND USER DEFINITION OF ROIS
	//During this phase, we go 
	//1)from Di_6_registered to smaller images Di_registered, cropped and normalized following Z
	//2)from these normalized images we produce field to interpolate the movement between successive positions
	public static void phaseX() {
		System.out.println("Start phase X");
		//makeFirstCropAndNormalizeMuAndVar();
		//cleanD0();
		//defineUserRois();
		//produceMaskUsingRois();
	}

	public static void makeFirstCropAndNormalizeMuAndVar() {		
		System.out.println("\nMovie preparation : make first crop and normalize Mu and Variance");
		int dayMax=DAY_MAX;
		for(int dayI=0;dayI<dayMax+1;dayI++) {
			System.out.println("Traitement jour "+dayI);
			ImagePlus imgRef=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+dayI+"_6_registered.tif");
			imgRef=VitimageUtils.cropImageFloat(imgRef, 98,  118, 0, 320, 300, 512);
			imgRef=VitimageUtils.normalizeMeanAndVarianceAlongZ(imgRef);
			imgRef=VitimageUtils.cropImageFloat(imgRef, 0,  0, 40, 320, 300, 472);
			IJ.run(imgRef, "Grays", "");
			imgRef.setSlice(1);
			IJ.run(imgRef, "Select All", "");
			IJ.run(imgRef, "Clear", "slice");
			imgRef.setSlice(472);
			IJ.run(imgRef, "Select All", "");
			IJ.run(imgRef, "Clear", "slice");			
			IJ.save(imgRef,"/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayI+"_registered.tif");
		}
		ImagePlus imgRe=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/mask512.tif");
		imgRe=VitimageUtils.cropImageByte(imgRe, 98,  118, 0, 320, 300, 512);
		imgRe=VitimageUtils.cropImageByte(imgRe, 0,  0, 40, 320, 300, 472);
		IJ.saveAsTiff(imgRe,"/home/fernandr/Bureau/Test/Visu2/Compute/mask472.tif");

	}
	
	public static void defineUserRois() {
		IJ.log("Define several rois on the image Movie_maker_v2/Intermediary_images/D0_registered\n"+
	"\ncambium_int_D0.roi"+
	"\nEllipse_area_tracked.roi"+
	"\next_1_borderline_D0.roi"+
	"\next_1_out_D0.roi"+
	"\nmoelle_int_D0.roi"+
	"\nmoelle_out_D0.roi"+"\nmush_at_D0.roi");
		System.exit(0);
	}
	
	public static void cleanD0() {
		IJ.log("clean D0_registered and save it in replace");
		System.exit(0);
	}
		
	public static void produceMaskUsingRois() {
		System.out.println("Start producing masks using rois");
		System.out.println("0");
		ImagePlus img=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		img.show();
		IJ.run(img, "Select All", "");
		IJ.setBackgroundColor(0, 0, 0);
		IJ.setForegroundColor(255, 255, 255);
		IJ.run(img, "Clear", "stack");
		img.hide();
		
		//Make a mask of cambium-liber only
		ImagePlus imgCambLib=new Duplicator().run(img);
		imgCambLib.show();
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/ext_1_out_D0.roi");
		IJ.run(imgCambLib, "Fill", "stack");		
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/cambium_int_D0.roi");
		IJ.run(imgCambLib, "Clear", "stack");
		IJ.run(imgCambLib,"8-bit","");
		IJ.saveAsTiff(imgCambLib, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/camblib_D0.tif");

		//Make a mask of moelle only
		ImagePlus imgMoelle=new Duplicator().run(img);
		imgMoelle.show();
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/moelle_out_D0.roi");
		System.out.println("21");
		IJ.run(imgMoelle, "Fill", "stack");		
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/moelle_int_D0.roi");
		System.out.println("22");
		IJ.run(imgMoelle, "Clear", "stack");
		IJ.run(imgMoelle,"8-bit","");
		System.out.println("23");
		IJ.saveAsTiff(imgMoelle, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/moelle_D0.tif");

		//Make a mask of vessels only
		ImagePlus imgVessels=new Duplicator().run(img);
		imgVessels.show();
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/cambium_int_D0.roi");
		System.out.println("31");
		IJ.run(imgVessels, "Fill", "stack");		
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/moelle_out_D0.roi");
		System.out.println("31");
		IJ.run(imgVessels, "Clear", "stack");
		IJ.run(imgVessels,"8-bit","");
		System.out.println("33");
		IJ.saveAsTiff(imgVessels, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/vessels_D0.tif");
		
		//Make a mask of inside of moelle only
		ImagePlus imgCenter=new Duplicator().run(img);
		imgCenter.show();
		IJ.open("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/rois/moelle_int_D0.roi");
		System.out.println("21");
		IJ.run(imgCenter, "Fill", "stack");		
		IJ.run(imgCenter,"8-bit","");
		System.out.println("23");
		IJ.saveAsTiff(imgCenter, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/center_D0.tif");

		IJ.run(imgCambLib, "Select All", "");
		IJ.run(imgCambLib,"Divide...","value=255 stack");
		IJ.run(imgCambLib,"Multiply...","value=1 stack");

		IJ.run(imgVessels, "Select All", "");
		IJ.run(imgVessels,"Divide...","value=255 stack");
		IJ.run(imgVessels,"Multiply...","value=2 stack");

		IJ.run(imgMoelle, "Select All", "");
		IJ.run(imgMoelle,"Divide...","value=255 stack");
		IJ.run(imgMoelle,"Multiply...","value=3 stack");

		IJ.run(imgCenter, "Select All", "");
		IJ.run(imgCenter,"Divide...","value=255 stack");
		IJ.run(imgCenter,"Multiply...","value=4 stack");

		ImagePlus temp=new ImageCalculator().run("Add create stack", imgCambLib, imgVessels);	
		temp=new ImageCalculator().run("Add create stack", temp, imgMoelle );	
		ImagePlus imgBoth=new ImageCalculator().run("Add create stack", temp, imgCenter );	

		IJ.saveAsTiff(imgBoth, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/Masques/both_D0.tif");
		imgVessels.changes=false;
		imgMoelle.changes=false;
		imgCambLib.changes=false;
		temp.changes=false;
		imgBoth.changes=false;
		imgCenter.changes=false;
		imgVessels.close();
		imgMoelle.close();
		imgCambLib.close();
		temp.close();
		imgBoth.close();
		imgCenter.close();
		
	}

	

	
	
	

	
	
	
	//Open Di, le lisse, applique le mask_global de la zone champignon, la sauve en tant que masked_float
	public static void makeMaskedFloat(double sigma) {
		for(int d=0;d<4;d++) {
			System.out.println(d);
			//Lisser la full
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_6_registered.tif");
			img=VitimageUtils.gaussianFiltering(img, sigma,sigma,sigma);
			
			//Appliquer le masque
			ImagePlus mask=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/mask_glob_float.tif");
			ImageCalculator ic=new ImageCalculator();
			img=ic.run("Multiply create 32-bit stack", img,mask);							
			img.setTitle("D"+d+"masked");
			img.setDisplayRange(0,1);
			IJ.saveAsTiff(img, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_masked_float.tif");
			//img.show();
		}
	}
	
	

	
	//OPEN Di_masked_float
	//La croppe, garde la composante connexe sombre la plus grosse, l'appelle mush_di_connexe
	public static void makeMush() {
		for(int d=0;d<4;d++) {
			System.out.println(d);
			double seuil= 0.3;
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_masked_float.tif");
			ImagePlus imgCrop=VitimageUtils.cropImageFloat(img, 193, 309, 133, 137, 63, 247);
			//imgCrop=VitimageUtils.gaussianFiltering(imgCrop,0.03,0.03,0.03);
			ImagePlus imgCon=VitimageUtils.connexe(imgCrop, 0.00001,seuil,0,10E8, 6,1,true);
			imgCon.setTitle("mush_d"+d);
			IJ.saveAsTiff(imgCon, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_connexe_.tif");
			System.out.println(d);
//imgCon.show();
		}
	}
	

	//OPEN Di_connexe
	//La lisse, la remet dans le crop initial
	public static void makeSmoother2() {
		for(int d=0;d<4;d++) {
			System.out.println(d);
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_connexe_.tif");
			IJ.run(img,"32-bit","");
			ImagePlus img2=VitimageUtils.gaussianFiltering(img, 0.06,0.06,0.06);
			img2.setDisplayRange(0, 1);
			IJ.run(img2,"8-bit","");
			img2=VitimageUtils.uncropImageByte(img2,193,309,133,512,512,512);
			IJ.saveAsTiff(img2, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_mush_to_size.tif");			
		}
		int d=0;
	}

	//OPEN Di_connexe
	//La lisse, la reconnexe, la relisse, la remet dans le crop initial
	public static void makeSmoother() {
		for(int d=0;d<4;d++) {
			System.out.println(d);
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_connexe_.tif");
			ImagePlus img2=VitimageUtils.gaussianFiltering(img, 0.02,0.02,0.02);
			ImagePlus img3=VitimageUtils.connexe(img2, 127.5,500,0,10E8, 6,1,true);//VitimageUtils.thresholdFloatImage(img2, 127.5, 10000);
			IJ.saveAsTiff(img3, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_smoother.tif");
			ImagePlus img4=VitimageUtils.uncropImageByte(img3,193,309,133,512,512,512);
			IJ.run(img4,"8-bit","");
			img2=VitimageUtils.gaussianFiltering(img, 0.02,0.02,0.02);
			img4.resetDisplayRange();
			IJ.run(img4,"8-bit","");
			IJ.saveAsTiff(img4, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_mush_to_size.tif");
			
		}
	}
	
	


	
	
		
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	///BEFORE HYPERIMAGE
	public static void visuSexyStep_2_makeInocPoint() {
		for(int d=3;d<4;d++) {
			ImagePlus imgInit=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_hr.tif");
			ItkTransform trAdd=ItkTransform.readTransformFromFile("/home/fernandr/Bureau/Test/Visu2/Compute/transforms/transAlignD"+d+".txt");
			int []dims=VitimageUtils.getDimensions(imgInit);
			double[] voxs=VitimageUtils.getVoxelSizes(imgInit);
			double refCenterX=dims[0]*voxs[0]/2;
			double refCenterY=dims[1]*voxs[1]/2;
			double refCenterZ=dims[2]*voxs[2]/2;
			System.out.println("Transformation");
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_1_aligned.tif");
			System.out.println("ok");			
//			Point3d[] pFin=VitimageUtils.detectInoculationPointManually(img,new Point3d(10.58, 5.77,  10.90));//D0
//			Point3d[] pFin=VitimageUtils.detectInoculationPointManually(img,new Point3d(10.37, 5.73,  16.31));//D1
//			Point3d[] pFin=VitimageUtils.detectInoculationPointManually(img,new Point3d(10.65, 5.70,  13.61));//D2
			Point3d[] pFin=VitimageUtils.detectInoculationPointManually(img,new Point3d(8.66, 5.55,  19.69));//D3
			System.out.println("1");
						
			//Compute the transformation that align the inoculation point to the Y+, with the axis already aligned
			Point3d[]pInit=new Point3d[3];
			pInit[0]=new Point3d( refCenterX  , refCenterY     , refCenterZ      );
			pInit[1]=new Point3d( refCenterX  , refCenterY     , refCenterZ + 1  );
			pInit[2]=new Point3d( refCenterX  , refCenterY + 1 , refCenterZ      );
	
			Point3d[] pFinTrans=new Point3d[] { pFin[0] , pFin[1] , pFin[2] };
			ItkTransform trAdd2=ItkTransform.estimateBestRigid3D(pFinTrans,pInit);
			trAdd.addTransform(trAdd2);
			System.out.println("2");
	
			trAdd.simplify();
			trAdd.writeMatrixTransformToFile("/home/fernandr/Bureau/Test/Visu2/Compute/transforms/transInocD"+d+".txt");
			ImagePlus imgTrans=trAdd.transformImage(imgInit, imgInit);
			System.out.println("3");
			IJ.saveAsTiff(imgTrans, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_2_inoc.tif");
			System.out.println("Fini !");
		}
	}
	
	
	
	
	
	
	///BEFORE HYPERIMAGE
	public static void visuSexyStep_7_compose() {
		ItkTransform []trs=new ItkTransform[4];
		for(int d=1;d<4;d++) {
			trs[d]=new ItkTransform();
		}
		for(int d=1;d<4;d++) {
			ItkTransform tr=ItkTransform.readTransformFromFile("/home/fernandr/Bureau/Test/Visu2/Compute/transforms/trans_register_individual"+d+".txt");
			for(int dd=d;dd<4;dd++) {
				trs[dd].addTransform(tr);
			}
		}

		for(int d=1;d<4;d++) {
			trs[d].simplify();
			trs[d].writeMatrixTransformToFile("/home/fernandr/Bureau/Test/Visu2/Compute/transforms/trans_register_global"+d+".txt");
		}

		for(int d=1;d<4;d++) {
			System.out.println("d"+d);
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_5_nocap.tif");
			img=trs[d].transformImage(img,img);
			IJ.saveAsTiff(img, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_6_registered.tif");
		}		
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D0_5_nocap.tif");
		IJ.saveAsTiff(img, "/home/fernandr/Bureau/Test/Visu2/Compute/D0_6_registered.tif");
		System.out.println("ok");
	}
		
	
	
	
	
	///BEFORE HYPERIMAGE
	public static void visuSexyStep_5_removeCapillary() {
		for(int d=0;d<4;d++) {
			System.out.println("D"+d);
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_4_norm.tif");
			ImagePlus out=VitimageUtils.removeCapillaryFromHyperImageForRegistration(img);
			IJ.saveAsTiff(out, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_5_nocap.tif");
		}
	}
	
	
	///AVANT HYPERIMAGE
	public static void visuSexyStep_6_register() {
		ImagePlus imgs[]=new ImagePlus[4];
		ImagePlus imgMask=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/mask512.tif");
		for(int d=1;d<4;d++) {
			System.out.println("Recalage "+d+" sur "+(d-1));
			imgs[d]=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_5_nocap.tif");
			imgs[d-1]=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+(d-1)+"_5_nocap.tif");
			ItkTransform tr=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(
						imgs[d-1], imgs[d], imgMask, Transformation3DType.VERSOR ,MetricType.CORRELATION, 2, 2, 9, 3, 0.1, 0.2, 1, false, false,80,false);
			tr.simplify();
			tr.writeMatrixTransformToFile("/home/fernandr/Bureau/Test/Visu2/Compute/transforms/trans_register_individual"+d+".txt");
		}
	}

	
	
	///INCLASSABLE
	
	public static void visuSexyStep_3_makeCrop() {
		for(int d=0;d<4;d++) {
			System.out.println("D"+d);
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_2_inoc.tif");
			ImagePlus out=VitimageUtils.cropImageShort(img, 0, 0, 256, 512, 512, 512);
			IJ.saveAsTiff(out, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_3_crop.tif");
		}
	}


	
	/// ARTIFICIAL PRODUCTION
	public static void makeThreshold() {
		
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu/Distance_mask.tif");
		img.show();
		img.setSlice(241);
		double sigma=2;
		ImagePlus imgD0=VitimageUtils.thresholdFloatImage(img,63,10000);
		ImagePlus imgD1=VitimageUtils.thresholdFloatImage(img,58,10000);//To see.. difference between successives..
		ImagePlus imgD2=VitimageUtils.thresholdFloatImage(img,49,10000);//To see.. difference between successives..
		ImagePlus imgD3=VitimageUtils.thresholdFloatImage(img,29,10000);//To see.. difference between successives..

		imgD0=VitimageUtils.gaussianFiltering(imgD0, sigma,sigma,sigma);		
		imgD1=VitimageUtils.gaussianFiltering(imgD1, sigma,sigma,sigma);
		imgD2=VitimageUtils.gaussianFiltering(imgD2, sigma,sigma,sigma);
		imgD3=VitimageUtils.gaussianFiltering(imgD3, sigma,sigma,sigma);
		IJ.run(imgD0,"8-bit","");
		IJ.run(imgD1,"8-bit","");
		IJ.run(imgD2,"8-bit","");
		IJ.run(imgD3,"8-bit","");
		
		imgD0.show();
		imgD0.setSlice(241);
		imgD1.show();
		imgD1.setSlice(241);
		imgD2.show();
		imgD2.setSlice(241);
		imgD3.show();
		imgD3.setSlice(241);
		IJ.saveAsTiff(imgD0,"/home/fernandr/Bureau/Test/Visu/segD0.tif");
		IJ.saveAsTiff(imgD1,"/home/fernandr/Bureau/Test/Visu/segD1.tif");
		IJ.saveAsTiff(imgD2,"/home/fernandr/Bureau/Test/Visu/segD2.tif");
		IJ.saveAsTiff(imgD3,"/home/fernandr/Bureau/Test/Visu/segD3.tif");
		
		
		
		
		VitimageUtils.waitFor(100000);
	}
	
	
	// VISU DEPUIS L IMAGE AVEC 40 SLICES
	public static void visuSexyStep_1_makeAlign() {
		for(int d=0;d<4;d++) {
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_hr.tif");
			int []dims=VitimageUtils.getDimensions(img);
			double[] voxs=VitimageUtils.getVoxelSizes(img);
			double refCenterX=dims[0]*voxs[0]/2;
			double refCenterY=dims[1]*voxs[1]/2;
			double refCenterZ=dims[2]*voxs[2]/2;
			Point3d [] pFin=VitimageUtils.detectAxis(img, AcquisitionType.MRI_T1_SEQ);
			Point3d [] pInit=new Point3d[3];
			pInit[0]=new Point3d(refCenterX, refCenterY     , refCenterZ     );//origine
			pInit[1]=new Point3d(refCenterX, refCenterY     , 1 + refCenterZ );//origine + dZ
			pInit[2]=new Point3d(refCenterX, 1 + refCenterY , refCenterZ     );//origine + dY
			for(int i=0;i<3;i++) {
				System.out.println("\nPoint init = "+pInit[i]);
				System.out.println("\nPoint final = "+pFin[i]);
			}
			ItkTransform trAdd=ItkTransform.estimateBestRigid3D(pFin, pInit);
			System.out.println(trAdd.drawableString());
			trAdd.simplify();
			trAdd.writeMatrixTransformToFile("/home/fernandr/Bureau/Test/Visu2/Compute/transforms/transAlignD"+d+".txt");
			ImagePlus imgTrans=trAdd.transformImage(img, img);
			imgTrans.show();
			img.show();
		}
		for(int d=0;d<4;d++) {
			System.out.println("Gestion cas "+d);
			ImagePlus imgInit=IJ.openImage("/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_hr.tif");
			ItkTransform trAdd=ItkTransform.readTransformFromFile("/home/fernandr/Bureau/Test/Visu2/Compute/transforms/transAlignD"+d+".txt");
			ImagePlus imgTrans=trAdd.transformImage(imgInit, imgInit);
			IJ.saveAsTiff(imgTrans, "/home/fernandr/Bureau/Test/Visu2/Compute/D"+d+"_1_aligned.tif");
		}
	}

	
	public static void computeDenseRegistrations(int percentageScore) {
		long l1=System.currentTimeMillis();
		long  lStart=System.currentTimeMillis();
		l1=-lStart+System.currentTimeMillis();System.out.println("Start : "+l1);
		int dayMax=DAY_MAX;
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
 //1 1 6 3 stride=3 sigma=0.8
		
		l1=-lStart+System.currentTimeMillis();System.out.println("\nPhase 1 : registration forward. Start : "+(l1/1000.0));
		for(int dayI=0;dayI<dayMax;dayI++) {
			int dayIPlus=dayI+1;		
			System.out.print("Forward.  dayI="+dayI+"    open");
			ImagePlus img0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayI+"_registered.tif");
			ImagePlus img1=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D"+dayIPlus+"_registered.tif");
			System.out.print(" bm ");
			ItkTransform tr10=BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(img0, img1, mask,Transformation3DType.DENSE, MetricType.CORRELATION,
					levMax, levMin, blockSize, neighSize, varMin, sigma, duration, displayRegistration, displayR2,percentageScore,dayI==0);
			System.out.print(" write");
			tr10.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+dayIPlus+"_to_"+dayI+".mhd");
			ItkTransform tr01=tr10.getInverseOfDenseField();
			tr01.writeAsDenseFieldWithITKExporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_"+dayI+"_to_"+dayIPlus+".mhd");
			System.out.print(" transform");
			ImagePlus res10=tr10.transformImage(img0, img1);
			ImagePlus res01=tr01.transformImage(img0, img0);
			System.out.print(" save");
			IJ.saveAsTiff(res10, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/d"+dayIPlus+"_to_"+dayI+".tif");
			IJ.saveAsTiff(res01, "/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/d"+dayI+"_to_"+dayIPlus+".tif");
		}
		
		
		l1=-lStart+System.currentTimeMillis();System.out.println("backwards accomplished : "+(l1/1000.0));
	}

	public static void chiant2() {
		ItkTransform tr01=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_0_to_1.mhd");
		ItkTransform tr10=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_1_to_0.mhd");
		ImagePlus img0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		ImagePlus img1=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D1_registered.tif");

		img0=VitimageUtils.Sub222(img0);
		img1=VitimageUtils.Sub222(img1);
		tr01=VitimageUtils.Sub222Dense(tr01);
		tr10=VitimageUtils.Sub222Dense(tr10);
	
		IJ.saveAsTiff(img0, "/home/fernandr/Bureau/img0.tif");
		IJ.saveAsTiff(img1, "/home/fernandr/Bureau/img1.tif");
		tr01.writeAsDenseFieldWithITKExporter("/home/fernandr/Bureau/tr01.mhd");
		tr10.writeAsDenseFieldWithITKExporter("/home/fernandr/Bureau/tr10.mhd");		

		ImagePlus d0_on_1=tr01.transformImage(img0, img0);
		ImagePlus d1_on_0=tr10.transformImage(img1, img1);
		System.out.println("3");
		ImagePlus merge011=VitimageUtils.compositeOf(d0_on_1, img1,"011");
		ImagePlus merge100=VitimageUtils.compositeOf(img0,d1_on_0,"100");
		merge011.show();
		merge100.show();
	}
	
	public static void chiantn() {
		ItkTransform tr01=ItkTransform.readFromDenseFieldWithITKImporter("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/champs/recalage_init/trans_0_to_1.mhd");
		ImagePlus img0=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/Img_intermediary/D0_registered.tif");
		ItkTransform tr01Sub=VitimageUtils.Sub222Dense(tr01);
		ImagePlus img01=tr01.transformImage(img0, img0);
		ImagePlus img01sub=tr01Sub.transformImage(img0, img0);
		IJ.saveAsTiff(img01, "/home/fernandr/Bureau/d01.tif");
		IJ.saveAsTiff(img01sub, "/home/fernandr/Bureau/d01_sub.tif");
		img01sub.show();
	}
	
	public static void chiant() {
		String rep="/mnt/DD_COMMON/Data_VITIMAGE/Movie_maker_v2/img_interp/images/";
		String [] strNbTest=new String[] {"01_40.tif","01_80.tif","12_60.tif","23_0.tif","23_60.tif"};
		String [] strParts=new String[] {"ves"};
		ImagePlus img,img2;
		double[][]valGauss=new double[][] {

		};  
		int incr=0;
		int nbComb=valGauss.length * strParts.length * strNbTest.length;
		for(int part=0;part<strParts.length;part++) {
			for(int te=0;te<strNbTest.length;te++) {
				img=IJ.openImage(rep+strParts[part]+strNbTest[te]);
				IJ.run(img,"32-bit","");
				for(int test=0;test<valGauss.length;test++) {
					System.out.println("Computing "+(++incr)+" over "+nbComb);				
					img2=VitimageUtils.gaussianFiltering(img, valGauss[test][0], valGauss[test][1], valGauss[test][2]);
					img2.setDisplayRange(0, 255);
					IJ.run(img2,"8-bit","");
					IJ.run(img2,"Fire","");
					IJ.saveAsTiff(img2, rep+"gaussian_filtered/"+strParts[part]+strNbTest[te]+"_gaussNb"+test);			
					//Camb et moelle : {0.03,0.03,0.07}   vaisseaux : 0.015 0.015 0.012}
				}
			}
		}
	}
	

	
}
