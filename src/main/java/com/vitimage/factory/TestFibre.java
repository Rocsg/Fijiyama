package com.vitimage.factory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import com.vitimage.common.TransformUtils;
import com.vitimage.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import inra.ijpb.binary.geodesic.GeodesicDistanceTransform3DFloat;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;

public class TestFibre {

	public static void main (String []args) {
		// code for test :  0                 1          2              3            4             5             6            7          8            9             10          11
		String[]specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		ImageJ ij=new ImageJ();
		//buildIsoSegmentationParts();
//		buildCambiumParts();
		String spec=getSpecimens()[0];
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation.tif");
		ImagePlus finalDistance=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distance_roots.tif");
		ImagePlus testRay=watemarkFloatData(VitimageUtils.noNanInFloat(finalDistance,0),0, 12000, 10, 20, 2, 0);
		testRay.show();
		VitimageUtils.waitFor(100000000);
		System.exit(0);
		testFibers();
		buildBubbles(10);
		watermarkCambiumsAndFibers();		
	}


	public static void testFibers() {
		for(int num=0;num<12;num++) {
			if(num==0 || num==6 || num==8)continue;
			String spec=getSpecimens()[num];
			testFibers(num,0,0,null,null);
		}
	}

	public static void testFibersLooking() {
		for(int num=8;num<9;num++) {
			String spec=getSpecimens()[num];
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation.tif");
			ImagePlus finalDistance=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/dist_GLOB_"+"HEALTHY"+".tif");
			ImagePlus[]grads=new ImagePlus[3];
			grads[0]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_X.tif");
			grads[1]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Y.tif");
			grads[2]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Z.tif");
	

			for(int i=-8;i<30;i+=1)for(int j=-30;j<30;j+=1) {
				System.out.println("PROCESSING "+i+" "+j);
				int[][]coords=VitimageUtils.getContactPoints(i, j)[num];
				boolean feasible=true;
				for(int p=0;p<coords.length;p++) {
					if(img.getStack().getVoxel(coords[p][0], coords[p][1],coords[p][2]) !=1) {
						feasible=false;
					}
					if(feasible) {testFibers(num,i,j,grads,finalDistance);
					try {
						Files.copy(new File("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/victorious_over_2.txt").toPath(),new File("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/victorious_"+i+"_"+j+"_over_2.txt").toPath(),StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					}
				}
			}
		}
	}


	public static void watermarkCambiumsAndFibers() {
		String[]specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		for(int num=0;num<12;num++) {
			String spec=specimen[num];
			System.out.println("processing specimen "+specimen[num]);
			//Open cambiumT1
			ImagePlus imgCamb=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_T1.tif");
			ImagePlus imgFib3=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/fibers_level_3.tif");
			int X=imgCamb.getWidth();
			int Y=imgCamb.getHeight();
			int Z=imgCamb.getStackSize();
			VitimageUtils.printImageResume(imgCamb, "imgCamb");
			//Make a threshold mask of it
			System.out.println("ok. Thresh");
			ImagePlus mask=VitimageUtils.thresholdByteImage(imgCamb, 1, 256);
			ImagePlus maskF3=VitimageUtils.thresholdByteImage(imgFib3, 1, 256);
		
			//Check the first non null slice (lowering Z), and lower it of 20 (to get a part where object is well expanded in space, not a tip
			int firstSlice=Z-1;
			int firstSliceF3=Z-1;
			while(VitimageUtils.isNullImage(VitimageUtils.cropImageByte(mask, 0, 0, firstSlice, X,Y,1)))firstSlice--;
			firstSlice-=30;
			while(VitimageUtils.isNullImage(VitimageUtils.cropImageByte(maskF3, 0, 0, firstSliceF3, X,Y,1)))firstSliceF3--;
			firstSliceF3-=30;
					
			//Compute distance in it
			ImagePlus seed=VitimageUtils.drawParallepipedInImage(mask, 0, 0,0, X-1, Y-1,  firstSlice-1, 0);
			ImagePlus seedF3=VitimageUtils.drawParallepipedInImage(maskF3, 0, 0,0, X-1, Y-1,  firstSliceF3-1, 0);
			
			//Use it to make a watermark mask
			System.out.println("ok. Dist");
			float[] floatWeights = new float[] {1000,1414,1732};
			System.out.println("Computing distance");
			ImagePlus distance=new ImagePlus(
					"geodistance_ecorce",new GeodesicDistanceTransform3DFloat(
							floatWeights,true).geodesicDistanceMap(seed.getStack(), mask.getStack())
					);		
			ImagePlus distanceF3=new ImagePlus(
					"geodistance_ecorce",new GeodesicDistanceTransform3DFloat(
							floatWeights,true).geodesicDistanceMap(seedF3.getStack(), maskF3.getStack())
					);		

			System.out.println("ok. Water1");
			ImagePlus maskWater=VitimageUtils.noNanInFloat(distance,0);
			maskWater=watemarkFloatData(maskWater,0, 1000,10, 20,2,0);
			maskWater=VitimageUtils.thresholdFloatImage(maskWater,0.0001,10E10);
			maskWater=VitimageUtils.switchValueInImage(maskWater,255,1);
			IJ.saveAsTiff(VitimageUtils.makeOperationBetweenTwoImages(imgCamb, maskWater, 2,false),"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_T1_watermarked.tif");

			System.out.println("ok. Water3");
			maskWater=VitimageUtils.noNanInFloat(distanceF3,0);
			maskWater=watemarkFloatData(maskWater,0, 1000,10, 20,2,0);
			maskWater=VitimageUtils.thresholdFloatImage(maskWater,0.0001,10E10);
			maskWater=VitimageUtils.switchValueInImage(maskWater,255,1);
			IJ.saveAsTiff(VitimageUtils.makeOperationBetweenTwoImages(imgFib3, maskWater, 2,false),"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/fibers_level_3_watermarked.tif");
		}
	}


	//QUE FAIRE ? 
	// RELANCER LES FIBRES. Leur donner plus de taille au début, et ne pas supprimer le start
	//FAIRE LE MARQUAGE TOUS LES CENTIMETRES
	
	
	/**
	 * Function for watermarking images such as distance maps, in order to make appear rays of data
	 * @param firstRayCenter 
	 * @param lastConsideredValue
	 * @param spaceBetweenRays
	 * @param percentageLose
	 * @param fiftyPercentbiggerRayEveryTens
	 * @param factorMultiplicativeForTenthRaySize
	 */
	public static ImagePlus watemarkFloatData(ImagePlus img,double firstRayCenter, double lastConsideredValue,double spaceBetweenRays, double percentageLose,double factorMultiplicativeForTenthRaySize,double valueToApplyToRays) {
		ImagePlus imgOut=VitimageUtils.imageCopy(img);
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();		
		double alpha=spaceBetweenRays*(2*percentageLose/100.0);
		double alphaEchelle=percentageLose/2;
		float[]valsOut;
		float val;
		double valEchelle;
		int index;
		double distance;
		for(int z=0;z<Z;z++) {
			valsOut=(float[])imgOut.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					val=valsOut[X*y+x];
					if(val<lastConsideredValue && val>firstRayCenter-alpha) {//Faire quelque chose
						//Trouver le numero de raie le plus proche
						valEchelle=(val-firstRayCenter)/spaceBetweenRays;
						index=(int)Math.round(valEchelle);
						distance=Math.abs(valEchelle-index);
						if(index%10==0 && distance < (percentageLose/200.0)*factorMultiplicativeForTenthRaySize)valsOut[X*y+x]=(float)valueToApplyToRays;
						if(index%10!=0 && distance < (percentageLose/200.0))valsOut[X*y+x]=(float)valueToApplyToRays;	
					}
				}
			}
		}
		return imgOut;
	}
	
	/**
	 * For each cep, build an image with a bubble centered on the tip of each productive branche 
	 */
	public static void buildBubbles(int radius) {
		double targetVoxelSize=0.5;
		double []initialVoxelSize= {0.7224,0.7224,1.0};
		double []factors=TransformUtils.multiplyVector(initialVoxelSize,1/targetVoxelSize);
		int[][][]contactPointsSain=VitimageUtils.getContactPoints(0,0);
		String []specimen= getSpecimens();
		for(int num=0;num<12;num++) {
			
			String spec=specimen[num];
			int[][]contactPoints=contactPointsSain[num];
			int zStart=VitimageUtils.getSliceRootStocks()[num];
			System.out.println("Processing "+(contactPoints.length)+" bubbles for "+spec);
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso.tif");
			ImagePlus img2=VitimageUtils.imageCopy(img);
			int X=img.getWidth();
			int Y=img.getHeight();
			int Z=img.getStackSize();		

			//Draw contact points in the branches
			img=VitimageUtils.nullImage(img);			
			for(int p=0;p<contactPoints.length;p++) {
				for(int dim=0;dim<3;dim++)contactPoints[p][dim]=(int)Math.round(contactPoints[p][dim]*factors[dim]);
				img=VitimageUtils.drawCircleInImage(img, radius, contactPoints[p][0],contactPoints[p][1],contactPoints[p][2],255);
			}			

			/*Draw basis of distance
			img2=VitimageUtils.thresholdByteImage(img, 1,6);
			img2=VitimageUtils.drawParallepipedInImage(img2, 0, 0, 0, X-1, Y-1, zStart, 0);
			img2=VitimageUtils.drawParallepipedInImage(img2, 0, 0, 0, X-1, Y-1, zStart, 0);

			
			img=VitimageUtils.binaryOperationBetweenTwoImages(img, img2, 1);
			*/
			img.setDisplayRange(0, 255);
			IJ.run(img,"Grays","");		
			IJ.saveAsTiff(img, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/bubbles.tif");
		}
		
	}
	
	public static void buildCambiumParts() {
		String[]specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		double targetVoxelSize=0.5;
		double []initialVoxelSize= {0.7224,0.7224,1.0};
		double []factors=TransformUtils.multiplyVector(initialVoxelSize, 1/targetVoxelSize);
		for(int num=0;num<12;num++) {
			String spec=specimen[num];
			System.out.println("Processing "+spec);
			ImagePlus segAniso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation.tif");
			System.out.println("ok. Boundaries");
			ImagePlus boundaryEc=VitimageUtils.boundaryZone(segAniso, 1,4,6,255);		
			ImagePlus boundaryBack=VitimageUtils.boundaryZone(segAniso, 1,0,6,255);
			ImagePlus boundaries=VitimageUtils.binaryOperationBetweenTwoImages(boundaryEc, boundaryBack, 1);
			System.out.println("ok. Dilation");
			Strel3D str=inra.ijpb.morphology.strel.BallStrel.fromDiameter(5);
			boundaries =new ImagePlus("",Morphology.dilation(boundaries.getImageStack(),str));
			ImagePlus boundariesMono=VitimageUtils.switchValueInImage(boundaries,255	,1);
			
			System.out.println("ok. Masking");
			String []outName= {"T1","T2","M0"};
			String []inName= {"Mod_2_THIN_low_mri_t1.tif","Mod_3_THIN_low_mri_t2.tif","Mod_4_THIN_low_mri_m0.tif"};
			for(int mod=0;mod<3;mod++) {
				System.out.println("   Processing mod "+outName[mod]);
				ImagePlus mri=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/"+inName[mod]);
				System.out.println("   ok. masking");
				mri=VitimageUtils.makeOperationBetweenTwoImages(mri, boundariesMono, 2, true);
				System.out.println("   ok. gauss");
				mri=VitimageUtils.gaussianFiltering(mri, 0.6, 0.6, 0.6);
				System.out.println("   ok. upsampling");
				mri=VitimageUtils.subXYZPerso(mri, factors, true,0);
				System.out.println("   ok. save");
				mri.setDisplayRange(0, 255);
				IJ.run(mri,"8-bit","");
				IJ.saveAsTiff(mri,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/cambium_"+outName[mod]+".tif");
			}
		}
	}

	
	
	public static void buildIsoSegmentationParts() {
		String[]specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		ImageJ ij=new ImageJ();
		for(int num=0;num<12;num++) {
			String spec=specimen[num];
			System.out.println("\nPROCESSING "+spec);
			System.out.print("Processing "+spec);
			ImagePlus segmentationIso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso.tif");
			IJ.run(segmentationIso,"Grays","");
			segmentationIso.setDisplayRange(0,255);
			IJ.saveAsTiff(segmentationIso,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso.tif");
			System.out.print("ok. ");
			
			ImagePlus imgSain=VitimageUtils.thresholdByteImage(segmentationIso, 1,2);
			IJ.run(imgSain,"Grays","");
			imgSain.setDisplayRange(0,255);
			IJ.saveAsTiff(imgSain,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso_SAIN.tif");
			System.out.print("ok. ");

			ImagePlus imgAmad=VitimageUtils.thresholdByteImage(segmentationIso, 3,4);
			IJ.run(imgAmad,"Grays","");
			imgAmad.setDisplayRange(0,255);
			IJ.saveAsTiff(imgAmad,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso_AMAD.tif");
			System.out.print("ok. ");

			ImagePlus imgNecr=VitimageUtils.thresholdByteImage(segmentationIso, 2,3);
			IJ.run(imgNecr,"Grays","");
			imgNecr.setDisplayRange(0,255);
			IJ.saveAsTiff(imgNecr,"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso_NECROSE.tif");
			System.out.println("ok... ");
		}
	}

	public static void postProcessFibers() {
		String[]specimen= {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
		ImageJ ij=new ImageJ();
		for(int num=1;num<12;num++) {
			String spec=specimen[num];
			System.out.println("\nPROCESSING "+spec);
			for(int n=-4;n<4;n++) {
				System.out.println("Processing "+n);
				ImagePlus fiberTest=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/fibers_level_"+n+".tif");
				Strel3D strFat=inra.ijpb.morphology.strel.BallStrel.fromDiameter(3);//CuboidStrel.fromRadiusList(1,1,1);
				ImagePlus fiberTest2=new ImagePlus("",Morphology.dilation(fiberTest.getImageStack(),strFat));
				System.out.println("morpho ok");
				fiberTest2=VitimageUtils.gaussianFiltering(fiberTest2, 0.6,0.6,0.6);
				System.out.println("gauss ok");
				fiberTest2.setDisplayRange(0, 255);
				IJ.run(fiberTest2,"8-bit","");
				IJ.saveAsTiff(fiberTest2, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/fibers_level_"+n+".tif");
				System.out.println("save ok");
			}
		}
	}

	public static String[]getSpecimens(){
		return new String[]{"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
	}

	
	/**
	 * Build the set of fibers for all ceps, except if testing >= 0 . In this case processing CEP0(11+testing) with test configuration 
	 * 
	 */	
	public static void testFibers(int numSpec,int i,int j,ImagePlus[]gradsOptions,ImagePlus distOptions) {		
		int[][][]contactPointsSain=VitimageUtils.getContactPoints(i,j);
		String []specimen= getSpecimens();
		int[]sliceRootStocks=new int[]{200+86 , 200+76 , 200+174 ,          200+65 , 200+97 , 200+108 ,        200+71  , 200+74 ,  200+68 ,       200+59 , 200+42  , 200+118}; 
		
		//MAIN RUPTORS
		boolean computeDistance=false;
		boolean verbose=false;
		boolean drawSexyFibers=true && (distOptions==null);
		boolean computeFibers=true;
		boolean writeDebugImagesForDistance=false;
		boolean excludeOutOfObjectForGradComputation=false;
		boolean showResult=true && (distOptions==null);
		boolean useMostRepresentativeFilter =true;
		boolean forwardAndBackwardComputation=false;
		boolean removeInsideBark=true;
		boolean useAllSpecimenForPathways=false;
		boolean useAllSpecimenForChamfer=false;
		boolean recentWoodForBoth=false;
		if(recentWoodForBoth)useAllSpecimenForChamfer=useAllSpecimenForPathways=false;
		
		//Parameters for distance computation
		int cambiumFunction=1;
		double alphaQuad=6;//7  6
		double betaQuad=1.5;//2  1.5
		double alphaLaplace=600;
		double betaLaplace=60;
		int maxPixelsLengthPathwayInCep=1500;
		double ponderationOfChamferOverCambiumBand=8;
		
		//Parameters for seeds and fibers
		boolean smoothGrad=false;
		boolean fromTheBack=false;
		boolean doRepulsionForce=true;
		double alphaRepulsion=50; //400
		double betaRepulsion=2;  //Try with 2 2.7
		double gammaRepulsion=numSpec==8 ? 13 : 15;
		int earlyBreak=100000;
		double step=0.02;
		int fiberRay=2;
		int nbSeedsPerArea=4;
		int seedSpacing=1;
		
		//Specimen and output parameters
		double targetVoxelSize=0.5;
		double []initialVoxelSize= {0.7224,0.7224,1.0};
		String spec=specimen[numSpec];
		//int[][]pointsNecr=contactPointsNecrose[numSpec];
		int[][]pointsSain=contactPointsSain[numSpec];
		int nPoints=pointsSain.length;
		int sliceRootStock=sliceRootStocks[numSpec];
		int victoryZ=(int)Math.round(sliceRootStock*initialVoxelSize[2]/targetVoxelSize-26);		

		
		
	
		//Compute distance and gradients
		ImagePlus segmentationIso=null;
		ImagePlus finalDistance=null;
		ImagePlus []grads=new ImagePlus[3];		
		if(computeDistance) {
			ImagePlus[]tab=buildDistanceMaps(numSpec,spec,sliceRootStock,useAllSpecimenForPathways,useAllSpecimenForChamfer,cambiumFunction,initialVoxelSize,targetVoxelSize,fromTheBack,alphaQuad,
													betaQuad,alphaLaplace,betaLaplace,maxPixelsLengthPathwayInCep,ponderationOfChamferOverCambiumBand,
													writeDebugImagesForDistance,useMostRepresentativeFilter,forwardAndBackwardComputation,contactPointsSain[numSpec],
													removeInsideBark,recentWoodForBoth);

			finalDistance=tab[0];
			segmentationIso=tab[1];
			finalDistance=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/dist_GLOB_"+
					(useAllSpecimenForPathways ? "FULL" : "HEALTHY")+".tif");
			grads=VitimageUtils.yoloGradients(finalDistance,1,excludeOutOfObjectForGradComputation ? -20 : -10000000,10000000,true);
			IJ.saveAsTiff(grads[0], "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_X.tif");
			IJ.saveAsTiff(grads[1], "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Y.tif");
			IJ.saveAsTiff(grads[2], "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Z.tif");
		}
		else {
			if(distOptions!=null)finalDistance=distOptions;
			else finalDistance=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/dist_GLOB_"+
							(useAllSpecimenForPathways ? "FULL" : "HEALTHY")+".tif");
	//		grads=VitimageUtils.yoloGradients(finalDistance,1,excludeOutOfObjectForGradComputation ? -20 : -10000000,10000000,true);
			segmentationIso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso.tif");
			if(gradsOptions!=null) {
				grads[0]=gradsOptions[0];
				grads[1]=gradsOptions[1];
				grads[2]=gradsOptions[2];
			}
			else {
				/*grads=VitimageUtils.yoloGradients(finalDistance,1,excludeOutOfObjectForGradComputation ? -20 : -10000000,10000000,true);
				IJ.saveAsTiff(grads[0], "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_X.tif");
				IJ.saveAsTiff(grads[1], "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Y.tif");
				IJ.saveAsTiff(grads[2], "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Z.tif");*/
				grads[0]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_X.tif");
				grads[1]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Y.tif");
				grads[2]=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Z.tif");
			}
			if(smoothGrad)for(int dim=0;dim<3;dim++)grads[dim]=VitimageUtils.gaussianFiltering(grads[dim], 1, 1, 1);
		}
		
	
		/////////////////////////////////////////////////// 
		//Config de tres fines à très grosses fibres
		/////////////////////////////////////////////////
		for(int config=3;config<4;config++) {
			fiberRay=5;//(int)Math.round(Math.pow(2, Math.max(config,1)));
			nbSeedsPerArea=(4-config);
		
			//Compute points for starting

			double[][]userDefinedPoints=new double[nPoints*nbSeedsPerArea*nbSeedsPerArea][3];
			for(int p=0;p<nPoints;p++){
				for(int ddi=0;ddi<nbSeedsPerArea*seedSpacing-seedSpacing/2;ddi+=seedSpacing){
					for(int ddj=0;ddj<nbSeedsPerArea*seedSpacing-seedSpacing/2;ddj+=seedSpacing){
						userDefinedPoints[p*nbSeedsPerArea*nbSeedsPerArea+(ddi/seedSpacing)*nbSeedsPerArea+(ddj/seedSpacing)]=new double[]{contactPointsSain[numSpec][p][0]*initialVoxelSize[0]/targetVoxelSize +(ddi-seedSpacing*nbSeedsPerArea/2),contactPointsSain[numSpec][p][1]*initialVoxelSize[1]/targetVoxelSize+ddj-seedSpacing*nbSeedsPerArea/2 ,contactPointsSain[numSpec][p][2]*initialVoxelSize[2]/targetVoxelSize};
					}
				}
			}		

			//Compute fibers and show result
			if(computeFibers) {
				double[][]fibersStart= userDefinedPoints;// : buildFibersStart(seeds,10,0.5);//(seeds,nbTotalSeedsBranches,0.5);
				ImagePlus []out=fiberTracking(finalDistance,grads,fibersStart,fiberRay,doRepulsionForce,alphaRepulsion,betaRepulsion,gammaRepulsion,fromTheBack,earlyBreak,verbose,step,segmentationIso,victoryZ,spec,drawSexyFibers);
				if(drawSexyFibers) {
					out[0].setDisplayRange(0, 255);
				
					out[1].setDisplayRange(0, 255);
					IJ.run(out[0],"8-bit","");
					IJ.run(out[1],"8-bit","");
					ImagePlus computingArea=null;
					if(useAllSpecimenForChamfer) {
						computingArea=VitimageUtils.thresholdByteImage(segmentationIso,1,5);
					}
					else if(recentWoodForBoth){
						computingArea=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seedEc_COMBINED.tif");
					}
					else computingArea=VitimageUtils.thresholdByteImage(segmentationIso,1,2); 
					
					ImagePlus composite=VitimageUtils.compositeOf(out[0], computingArea,"comp");
					if(showResult) {
						out[1].show();
						composite.show();
					}
					IJ.saveAsTiff(out[1],"/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/fibers_level_"+config+".tif" );
				}
			}
		}
		if(computeDistance) {//if not, let's suppose it is a test and we would like to do it again after, then don t remove the file produced a step before
	//		new File("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Z.tif").delete();
			//		new File("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_Y.tif").delete();
			//new File("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/grad_X.tif").delete();
			//new File("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/dist_GLOB_HEALTHY.tif").delete();
		}
	}
	
	
	
	
	
	

	/**
	 * Build the potential map for driving particles from one part of the specimen to the other part.
	 * int numSpec : from 0 to 11 (included) , 
	 * String spec : from "CEP011_AS1" to "CEP022_APO3"
	 * int sliceRootStock : starting (or ending point), defined as the slice number in the initial image, pointing a stable point in the lower trunk
	 * int cambiumFunction : 0=laplace , 1 = quadratic (good choice, according to the experiments), 2 = none
	 * boolean useAllSpecimenForPathways : if true, use all the circumference to guide fibers through the quad function that leads fibers to stay close to cambium. 
	 * 							   if false, use only the circumference part that shows an interface between healthy wood and bark or background
	 * boolean useAllSpecimenForChamfer : if true, use all the specimen to compute distance map from start to stop
	 * 							   		   if false, use only the health wood area
	 * double targetVoxelSize : necessary to compute isotropic distances computations, and allows to "ease" the optimisation with low-varying potential function. Also allows high definition results
	 * boolean fromTheBack : if true, drive particles from the trunk to the branches. If false, it is the inverse
	 * double alphaQuad and betQuad : parameters of the quadratic function f(dist)=( (dist-alpha)/beta )^2 that give cost to go far away from cambium (assuming it is alpha voxels from the bark)
	 * double alphaLaplace, betLaplace : parameters of laplace function alpha / (beta+x^2) if used for cambium cost, in place of
	 * 
	 */	
	public static ImagePlus []buildDistanceMaps(int numSpec,String spec,int sliceRootStock,boolean useAllSpecimenForPathways,boolean useAllSpecimenForChamfer,int cambiumFunction,
											double[]initVoxelSize,double targetVoxelSize,boolean fromTheBack,
											double alphaQuad,double betaQuad,double alphaLaplace,double betaLaplace,
											int maxPixelsLenghtPathwayInCep,double ponderationOfChamferOverCambiumBand, 
											boolean writeDebugImagesForDistance, boolean useMostRepresentativeFilter,
											boolean forwardAndBackwardComputation,int[][]branchesOfInterest,boolean removeInsideBark, boolean recentWoodForBoth) {
		
		//Parameters handling
		int nBranches=branchesOfInterest.length;
		System.out.println("Building distance maps on "+spec+" using all for pathways ? "+useAllSpecimenForPathways+" . Using all for chamfer ? "+useAllSpecimenForChamfer+" . From the back ? "+fromTheBack);
		double[]factors={initVoxelSize[0]/targetVoxelSize,initVoxelSize[1]/targetVoxelSize,initVoxelSize[2]/targetVoxelSize};
		int sliceStart=(int)Math.round(sliceRootStock*initVoxelSize[2]/targetVoxelSize);
		int[][]coordinatesBackwardStart=new int[nBranches][3];
		for(int i=0;i<nBranches;i++)for(int j=0;j<3;j++)coordinatesBackwardStart[i][j]=(int)Math.round(branchesOfInterest[i][j]*initVoxelSize[j]/targetVoxelSize);
		ImagePlus binarySegForDistance=null;
		ImagePlus segAniso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation"+(removeInsideBark ? "_no_inside_bark" : "")+".tif");
		if(useMostRepresentativeFilter)segAniso=VitimageUtils.mostRepresentedFilteringWithRadius(segAniso, 2, true, 5, false);
				

		//resample the global segmentation it in the targetGeometry
		System.out.println("Ok. Resampling");
		VitimageUtils.printImageResume(segAniso,"segAniso");
		ImagePlus seg=VitimageUtils.subXYZPerso(segAniso, factors, false,0);
		int X=seg.getWidth();		int Y=seg.getHeight();	int Z=seg.getStackSize();
		VitimageUtils.printImageResume(seg,"seg");
		if(useMostRepresentativeFilter)seg=VitimageUtils.mostRepresentedFilteringWithRadius(seg, useAllSpecimenForChamfer ? 1 : 1, true, 6, false);
		IJ.saveAsTiff(seg, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_iso.tif");
		
		
		//Get the volume used for pathways and for distance from roots...
		//Remove the area under the starting point which is in consideration for this experiment, and prepare seed for roots
		System.out.println("Ok. Build pathway");
		seg=VitimageUtils.drawParallepipedInImage(seg, 0, 0, sliceStart+1, X-1, Y-1, Z-1, 0);
		binarySegForDistance=VitimageUtils.thresholdByteImage(seg, 1,useAllSpecimenForChamfer ? 256 : 2);//if not all specimen, we use only the area of class #1 (healthy wood)
		ImagePlus seedRootStock=VitimageUtils.drawParallepipedInImage(seg, 0, 0, 0, X-1, Y-1, sliceStart-1, 0);
		seedRootStock=VitimageUtils.thresholdByteImage(seedRootStock, 1,10);
		
		//Building seed image for computing distance map from ecorce to the center
		System.out.println("Ok. Building seed image for ecorce");
		Strel3D strPti=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(1,1,1);
		ImagePlus seedEcorce=null;
		if(useAllSpecimenForPathways) {
			ImagePlus tmp=VitimageUtils.thresholdByteImage(seg, 1, 256);
			seedEcorce=new ImagePlus("",Morphology.dilation(tmp.getImageStack(),strPti));
			seedEcorce=VitimageUtils.makeOperationBetweenTwoImages(seedEcorce,tmp,4,false);
			if(writeDebugImagesForDistance)IJ.saveAsTiff(seedEcorce, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seedEc_NECR.tif");
		}
		if (!useAllSpecimenForPathways || recentWoodForBoth) {
			//Extract voxels of background or ecorce in the neighbouring of healthy wood. They are potential candidates for healthy cambium
			ImagePlus tmp1=VitimageUtils.boundaryZone(seg, 0,1,26,1);
			ImagePlus tmp2=VitimageUtils.boundaryZone(seg, 4,1,26,1);
			seedEcorce=VitimageUtils.makeOperationBetweenTwoImages(tmp1, tmp2, 1, false);
			seedEcorce=VitimageUtils.thresholdByteImage(seedEcorce,1,256);
			seedEcorce.setTitle("seedEcorce");

			if(writeDebugImagesForDistance)IJ.saveAsTiff(seedEcorce, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seedEc_HEALTH.tif");
			//Plus tard, a ajouter fermer les trous de l image de background, et sans doute un peu de most represented filter en prealable
		}

		
		//Restrict the area of computation around the new wood (under active cambiums)
		if(recentWoodForBoth) {
			Strel3D strFat=inra.ijpb.morphology.strel.BallStrel.fromDiameter(alphaQuad);//CuboidStrel.fromRadiusList(1,1,1);
			ImagePlus ecorceThick=new ImagePlus("",Morphology.dilation(seedEcorce.getImageStack(),strFat));
			ecorceThick=new ImagePlus("",Morphology.dilation(ecorceThick.getImageStack(),strFat));
			ecorceThick=new ImagePlus("",Morphology.dilation(ecorceThick.getImageStack(),strFat));
			ecorceThick=new ImagePlus("",Morphology.dilation(ecorceThick.getImageStack(),strFat));
			ecorceThick=new ImagePlus("",Morphology.dilation(ecorceThick.getImageStack(),strFat));
			ecorceThick=new ImagePlus("",Morphology.dilation(ecorceThick.getImageStack(),strFat));
			ecorceThick.setTitle("thick");
			seedEcorce.setTitle("thin");
			binarySegForDistance.setTitle("binarySeg");
	//		ecorceThick.show();
			//		seedEcorce.show();
			//binarySegForDistance.show();
	//		VitimageUtils.waitFor(10000000);
			binarySegForDistance=VitimageUtils.binaryOperationBetweenTwoImages(binarySegForDistance, ecorceThick, 2);//Compute intersection of thick cambium with healthy wood
			for(int b=0;b<nBranches;b++)binarySegForDistance=VitimageUtils.drawCircleInImage(binarySegForDistance, 15, coordinatesBackwardStart[b][0], coordinatesBackwardStart[b][1], coordinatesBackwardStart[b][2],255);
			if(useMostRepresentativeFilter) {
				binarySegForDistance=VitimageUtils.switchValueInImage(binarySegForDistance,255, 1);
				binarySegForDistance=VitimageUtils.mostRepresentedFilteringWithRadius(binarySegForDistance, 1, true, 2, false);
				binarySegForDistance=VitimageUtils.switchValueInImage(binarySegForDistance,1, 255);
			}
			if(writeDebugImagesForDistance)IJ.saveAsTiff(binarySegForDistance, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seedEc_COMBINED.tif");

		}

	
		//Computing distance map for ecorce
		System.out.println("Ok. Building ecorce distance");
		float[] floatWeights = new float[] {1000,1414,1732};
		System.out.println("Computing distance from ecorce");
		ImagePlus distanceEcorce=new ImagePlus(
				"geodistance_ecorce",new GeodesicDistanceTransform3DFloat(
						floatWeights,true).geodesicDistanceMap(seedEcorce.getStack(), binarySegForDistance.getStack())
				);		
		distanceEcorce=VitimageUtils.noNanInFloat(distanceEcorce,(float) -1);
		ImagePlus distanceEcLaplace=(cambiumFunction==0 ? laplace(distanceEcorce,alphaLaplace,betaLaplace) : cambiumFunction==1 ?quadraticBounds(distanceEcorce,alphaQuad,betaQuad) : VitimageUtils.makeOperationOnOneImage(distanceEcorce,2,0, true));
		if(writeDebugImagesForDistance)IJ.saveAsTiff(distanceEcorce, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distance_bark.tif");
		if(writeDebugImagesForDistance)IJ.saveAsTiff(distanceEcLaplace, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/dist_bark_after_laplacian.tif");

		

		//Computing distance map from rootstock to the branches (or inverse, depending on parameter fromTheBack)
		System.out.println("Computing distance from rootstock");
		ImagePlus distanceRoot=new ImagePlus(
				"geodistance_ecorce",new GeodesicDistanceTransform3DFloat(
						floatWeights,true).geodesicDistanceMap(seedRootStock.getStack(), binarySegForDistance.getStack())
				);
		if(writeDebugImagesForDistance)IJ.saveAsTiff(distanceRoot, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distance_roots.tif");

		// Compute the summary distance of branches to the trunk)
		double maxDist=0;
		double minDist=100000;
		double[] distanceValuesAtImpactPoints=new double[nBranches];
		for(int p=0;p<nBranches;p++) {
			distanceValuesAtImpactPoints[p]=distanceRoot.getStack().getVoxel(coordinatesBackwardStart[p][0], coordinatesBackwardStart[p][1], coordinatesBackwardStart[p][2]);
			if(maxDist<distanceValuesAtImpactPoints[p])maxDist=distanceValuesAtImpactPoints[p];
			if(minDist>distanceValuesAtImpactPoints[p])minDist=distanceValuesAtImpactPoints[p];
			System.out.println("Branche "+p+" is at distance "+distanceValuesAtImpactPoints[p]+" from the root stock");
		}
		if(!fromTheBack) {
			distanceRoot=VitimageUtils.noNanInFloat(distanceRoot, maxPixelsLenghtPathwayInCep);
			distanceRoot=VitimageUtils.makeOperationOnOneImage(distanceRoot,2, -ponderationOfChamferOverCambiumBand/(forwardAndBackwardComputation ? 2 : 1), true);
			distanceRoot=VitimageUtils.makeOperationOnOneImage(distanceRoot,1, ponderationOfChamferOverCambiumBand*maxPixelsLenghtPathwayInCep, true);
		}
		else distanceRoot=VitimageUtils.noNanInFloat(distanceRoot, 0);

		//If needed, build distance map from the branches
		ImagePlus distanceBranches=null;
		if(forwardAndBackwardComputation) {
			//Prepare seed image and area for seeding.
			ImagePlus binSegBackward=VitimageUtils.imageCopy(binarySegForDistance);
			ImagePlus  seedsBackward=VitimageUtils.nullImage(binarySegForDistance);
			//Seed the extremity of "pipes" which length are computed in ordre to balance the various distances of each branchefrom root stock
			for(int br=0;br<nBranches;br++) {
				int sizeCylinder=(int)Math.round(2+(maxDist-distanceValuesAtImpactPoints[br]));
				binSegBackward=VitimageUtils.drawParallepipedInImage(binSegBackward, coordinatesBackwardStart[br][0]-1, coordinatesBackwardStart[br][1]-1, coordinatesBackwardStart[br][2]-sizeCylinder,
																						coordinatesBackwardStart[br][0]+1, coordinatesBackwardStart[br][1]+1, coordinatesBackwardStart[br][2],0);
				binSegBackward=VitimageUtils.drawParallepipedInImage(binSegBackward, coordinatesBackwardStart[br][0], coordinatesBackwardStart[br][1], coordinatesBackwardStart[br][2]-sizeCylinder,
						coordinatesBackwardStart[br][0], coordinatesBackwardStart[br][1], coordinatesBackwardStart[br][2],0);
				seedsBackward.getStack().setVoxel(coordinatesBackwardStart[br][0], coordinatesBackwardStart[br][1], coordinatesBackwardStart[br][2]-sizeCylinder, 255);
			}
			if(writeDebugImagesForDistance)IJ.saveAsTiff(binSegBackward, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/binSegBackward.tif");
			if(writeDebugImagesForDistance)IJ.saveAsTiff(seedsBackward, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seedsBackward.tif");

			
			//Compute effective distance
			System.out.println("Computing distance from branches");
			distanceBranches=new ImagePlus(
					"geodistance_branches",new GeodesicDistanceTransform3DFloat(
							floatWeights,true).geodesicDistanceMap(seedsBackward.getStack(), binSegBackward.getStack())
					);
			if(writeDebugImagesForDistance)IJ.saveAsTiff(distanceBranches, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distance_branches.tif");
			distanceBranches=VitimageUtils.noNanInFloat(distanceBranches, 0);
			distanceBranches=VitimageUtils.makeOperationOnOneImage(distanceBranches,2,ponderationOfChamferOverCambiumBand/(forwardAndBackwardComputation ? 2 : 1), true);
		}		
		
		
		//Combine both distance maps to produce final map
		System.out.println("Ok. Combining distances...");
		ImagePlus finalDistance=VitimageUtils.makeOperationBetweenTwoImages(distanceRoot, distanceEcLaplace, 4, true);
		if(forwardAndBackwardComputation)finalDistance=VitimageUtils.makeOperationBetweenTwoImages(finalDistance, distanceBranches, 1, true);
		IJ.saveAsTiff(finalDistance, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/dist_GLOB_"+
		(useAllSpecimenForPathways ? "FULL" : "HEALTHY")+".tif");
		if(writeDebugImagesForDistance)IJ.saveAsTiff(binarySegForDistance, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/distance_area"+(useAllSpecimenForPathways ? "FULL" : "HEALTHY")+".tif");
		return new ImagePlus[] {finalDistance,seg};
	}
	
	
	
	
	


	
	/**
	 * Algorithm of fiber tracking. Parameters list to write
	 */	
	public static ImagePlus[]fiberTracking(ImagePlus distance,ImagePlus[]gradients, double[][] fibersStart,double fiberRay,
											boolean doRepulsion, double alpha, double beta,double gamma,boolean fromBottomToTop,
											int earlyBreakIteration,boolean debug,double step,ImagePlus segmentation,int victoryZ,String spec,boolean drawSexyFibers) {
		System.out.println("Ok. Params");
		String s="";
		System.out.println("Starting fiber tracking with nSeeds="+fibersStart.length);
		if(debug)VitimageUtils.printImageResume(distance,"Distance");
		if(debug)VitimageUtils.printImageResume(gradients[0],"Grad X");
		//distance.show();
		distance.setDisplayRange(0, 200);
		distance.setTitle("Distance used");
		for(int dim=0;dim<3;dim++) {
			//gradients[dim].show();
			gradients[dim].setDisplayRange(-10, 10);
			gradients[dim].setTitle("Grad["+dim+"]");
		}
		ImagePlus fibers=VitimageUtils.imageCopy(distance);
		fibers.setTitle("fibers");
		fibers=VitimageUtils.makeOperationOnOneImage(fibers,2,0, true);
		ImagePlus fibersSexy=VitimageUtils.imageCopy(fibers);
		fibersSexy.setTitle("Fibers sexy");

		System.out.println("Ok. Values");
		int fiberRayInt=(int)Math.ceil(fiberRay);
		int nSeeds=fibersStart.length;
		int nbMaxMoves=earlyBreakIteration*nSeeds;
		int nbRunning=nSeeds;
		int X=gradients[0].getWidth();
		int Y=gradients[1].getHeight();
		int Z=gradients[2].getStackSize();
		double xx,dx;
		double yy,dy;
		double zz,dz;
		int x0,y0,z0;
		int nbBrDist=0;
		int nbBrAngl=0;
		int nbBrAnglGrad=0;
		int nbBrBord=0;
		int nbGrad=0;
		int nbBrFor=0;
		int incrMov=-1;
		int nbVictorious=0;
		
		//Algorithm parameters
		double stepMin=step/100;
		double angleMax=3.10;
		double minForward=-4;

		System.out.println("Ok. Tabss");
		double[][][]coords=new double[nSeeds][earlyBreakIteration+2][3];
		double[][]distances=new double[nSeeds][earlyBreakIteration+2];
		int incr[]=new int[nSeeds];
		int[]status=new int[nSeeds];
		double[]grad=new double[3];
		float[][][] grads=new float[3][Z][];
		float[][] valFibers=new float[Z][];
		float[][] valFibersSexy=new float[Z][];
		byte[][] valSegmentation=new byte[Z][];
		float[][] valDistance=new float[Z][];
		double[][][]factors=new double[2][2][2];
		double[]tempGradWithoutRepulsion=new double[3];
		double[]tempGradWithRepulsion=new double[3];
		double[]gradDirWithRepulsion=new double[3];
		double[]gradDirWithoutRepulsion=new double[3];
		double[]saveGradNorm=new double[3];
		double []saveGrad=new double[3];
		int realInd1,realInd2;
		int indCur;
		int incrCur;
		double valMin;
		double []tmpForce=new double[3];
		double[]resultante=new double[3];

		System.out.println("Ok. Image data management");
		for(int z=0;z<Z;z++) {
			for(int dim=0;dim<3;dim++)grads[dim][z]=(float []) gradients[dim].getStack().getProcessor(z+1).getPixels();
			valFibers[z]=(float[])fibers.getStack().getProcessor(z+1).getPixels();
			valFibersSexy[z]=(float[])fibersSexy.getStack().getProcessor(z+1).getPixels();
			valDistance[z]=(float[])distance.getStack().getProcessor(z+1).getPixels();
			valSegmentation[z]=(byte[])segmentation.getStack().getProcessor(z+1).getPixels();
		}
		
		
		//Initialiser toutes les distances
		for(int p=0;p<nSeeds;p++) {
			distances[p][0]=0;
			coords[p][0]=fibersStart[p];
		}
		
		//Boucle principale
		while(nbRunning>0 && incrMov<nbMaxMoves) {			
			incrMov++;
			//Chercher celui avec la distance la plus petite.
			valMin=100000000;
			indCur=-1;
			incrCur=-1;
			for(int path=0;path<nSeeds;path++) {
				if(status[path]>0 || incr[path]>earlyBreakIteration-2)continue;
				if(distances[path][incr[path]]<valMin) {
					indCur=path;
					valMin=distances[path][incr[path]];
					incrCur=incr[path];
				}
			}
			if(indCur==-1) {nbRunning=0;continue;}

			
			
			//Calculer le gradient local de la fonction de distance
			xx=coords[indCur][incrCur][0];
			yy=coords[indCur][incrCur][1];
			zz=coords[indCur][incrCur][2];
			x0=(int)Math.floor(xx);
			y0=(int)Math.floor(yy);
			z0=(int)Math.floor(zz);
			dx=xx-x0;
			dy=yy-y0;
			dz=zz-z0;
			if(incrMov%10000==0)System.out.print(" "+incrMov);
			if(incrMov%100000==0)System.out.println();
			if(debug)System.out.println("\nMove # "+incrMov+" , applied on particle "+indCur+" with int coordinates  ( "+x0+" , "+y0+" , "+ z0+" )");
			factors[0][0][0]=(1-dx)*(1-dy)*(1-dz);
			factors[0][0][1]=(1-dx)*(1-dy)*(dz);
			factors[0][1][0]=(1-dx)*(dy)*(1-dz);
			factors[0][1][1]=(1-dx)*(dy)*(dz);
			factors[1][0][0]=(dx)*(1-dy)*(1-dz);
			factors[1][0][1]=(dx)*(1-dy)*(dz);
			factors[1][1][0]=(dx)*(dy)*(1-dz);
			factors[1][1][1]=(dx)*(dy)*(dz);
				
			tempGradWithoutRepulsion=new double[] {0,0,0};
			for(int di=0;di<2;di++)for(int dj=0;dj<2;dj++)for(int dk=0;dk<2;dk++)for(int dim=0;dim<3;dim++)tempGradWithoutRepulsion[dim]+=grads[dim][z0+dk][(y0+dj)*X+(x0+di)]*factors[di][dj][dk];
		
			
			//Chercher tous ceux encore en course, et pour chacun, calculer la force d interaction et sommer le tout
			resultante=new double[3];
			for(int path=0;path<nSeeds;path++) {
				if(status[path]>0 || path ==indCur)continue;
				tmpForce=repulsionForce(coords[path][incr[path]],coords[indCur][incrCur],alpha,beta,gamma);
				resultante=TransformUtils.sumVector(resultante,tmpForce);
			}

			//Calculer la composante orthogonale au gradient
			resultante=TransformUtils.vectorialSubstraction(resultante,TransformUtils.proj_u_of_v(tempGradWithoutRepulsion, resultante));

			//L ajouter au gradient deja connu
			tempGradWithRepulsion=TransformUtils.vectorialAddition(tempGradWithoutRepulsion, resultante);

			
			//Actualiser incr, avancer d un step. Ecrire le nouveau point. 
			gradDirWithRepulsion=TransformUtils.normalize(tempGradWithRepulsion);
			gradDirWithoutRepulsion=TransformUtils.normalize(tempGradWithoutRepulsion);
			grad=TransformUtils.multiplyVector(gradDirWithRepulsion, step);
			incrCur++;
			incr[indCur]=incrCur;

			for(int dim=0;dim<3;dim++)coords[indCur][incrCur][dim]=coords[indCur][incrCur-1][dim]+grad[dim];
			if(debug)System.out.println("Valeur de "+TransformUtils.stringVectorDou(coords[indCur][incrCur-1],"")+" actualisée a : "+TransformUtils.stringVectorDou(coords[indCur][incrCur],""));
			
			//Calculer sa nouvelle distance
			xx=coords[indCur][incrCur][0];
			yy=coords[indCur][incrCur][1];
			zz=coords[indCur][incrCur][2];
			x0=(int)Math.floor(xx);
			y0=(int)Math.floor(yy);
			z0=(int)Math.floor(zz);
			dx=xx-x0;
			dy=yy-y0;
			dz=zz-z0;
			distances[indCur][incrCur]=0;
			factors[0][0][0]=(1-dx)*(1-dy)*(1-dz);
			factors[0][0][1]=(1-dx)*(1-dy)*(dz);
			factors[0][1][0]=(1-dx)*(dy)*(1-dz);
			factors[0][1][1]=(1-dx)*(dy)*(dz);
			factors[1][0][0]=(dx)*(1-dy)*(1-dz);
			factors[1][0][1]=(dx)*(1-dy)*(dz);
			factors[1][1][0]=(dx)*(dy)*(1-dz);
			factors[1][1][1]=(dx)*(dy)*(dz);					
			for(int di=0;di<2;di++)for(int dj=0;dj<2;dj++)for(int dk=0;dk<2;dk++)distances[indCur][incrCur]+=(double)((float[])valDistance[z0+dk])[(y0+dj)*X+(x0+di)]*factors[di][dj][dk];
		

			for(int di=-fiberRayInt;di<fiberRayInt+1;di++)for(int dj=-fiberRayInt;dj<fiberRayInt+1;dj++) for(int dk=-fiberRayInt;dk<fiberRayInt+1;dk++){
				if(di*di+dj*dj+dk*dk < fiberRay*fiberRay) {
					valFibers[z0+dk][(y0+dj)*X+(x0+di)]=255;
				}
			}

			//Verifier si la progression est respectee
			double valForward=distances[indCur][incrCur]-distances[indCur][incrCur-1];
			if(incrCur>1 && valForward<minForward) {status[indCur]=1;nbRunning--;System.out.println(" particule "+indCur+" stopping because forward="+valForward);nbBrFor++;if(z0>546)nbVictorious++;}

			//Verifier si le gradient a une norme acceptable
			double valGrad=TransformUtils.norm(gradDirWithRepulsion);
			if(valGrad<1E-6) {status[indCur]=1;nbRunning--;System.out.println(" particule "+indCur+" stopping because grad norm="+valGrad);nbGrad++;if(z0>546)nbVictorious++;}
			
			//Calculer l'angle formé, verifier si c est inferieur à seuil
			double valAngle=(incrCur<2) ? 0 : angle(TransformUtils.vectorialSubstraction(coords[indCur][incrCur], coords[indCur][incrCur-1]) ,
		               TransformUtils.vectorialSubstraction(coords[indCur][incrCur-1], coords[indCur][incrCur-2]));
			if(incrCur>0 && valAngle>angleMax){status[indCur]=1;nbRunning--;System.out.println(" path "+indCur+" stopping because angle="+valAngle+" . Incr="+incrCur);nbBrAngl++;if(z0>victoryZ)nbVictorious++;}
				
			//Verifier si le nouveau point est proche du bord de l'image
			double valProche= (coords[indCur][incrCur][0]<fiberRay+1) || (coords[indCur][incrCur][0]>X-(fiberRay+2)) ||
					 (coords[indCur][incrCur][1]<fiberRay+1) || (coords[indCur][incrCur][1]>Y-(fiberRay+2)) ||
					 (coords[indCur][incrCur][2]<fiberRay+1) || (coords[indCur][incrCur][2]>Z-(fiberRay+2)) ? 1 : 0;
			if(valProche>0  ) {status[indCur]=1;nbRunning--;System.out.println(" path "+indCur+" stopping because point approaching border : "+TransformUtils.stringVector(coords[indCur][incrCur],""));nbBrBord++;if(z0>victoryZ)nbVictorious++;}			

			//Debug informations, if necessary
			if(debug)System.out.println("   "+TransformUtils.stringVectorDou(gradDirWithoutRepulsion, "Direction avant rep")+"   "+TransformUtils.stringVectorDou(gradDirWithRepulsion, "Direction avec"));
			if(debug)System.out.println("   "+TransformUtils.stringVectorDou(grad, "grad utilisé"));
			if(debug)System.out.println("   ValAngle="+VitimageUtils.dou(valAngle,6)+"  valForward="+VitimageUtils.dou(valForward,6)+"  Potentiel i="+VitimageUtils.dou(distances[indCur][incrCur],6)+"  Potentiel i+1="+VitimageUtils.dou(distances[indCur][incrCur],6));
		}			
		System.out.println("\n\n\n   Nb Path start="+nSeeds+" nb at the end="+nbRunning+" nbBrBorder="+nbBrBord+" nbBrAng="+nbBrAngl+" nbBrAngGrad="+nbBrAnglGrad+" nbGrad="+nbGrad+" nbBrDis="+nbBrDist+" nbBrFor="+nbBrFor);
		System.out.println("   Nb victorious = "+nbVictorious+" / "+nSeeds);
		String sVictory="##Nb_victorious= "+nbVictorious+" / "+nSeeds;
		
		
		
		if(!drawSexyFibers) {
			VitimageUtils.writeStringInFile(sVictory, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/victorious_over_2.txt");
			return new ImagePlus[] {fibers,fibersSexy};
		}
		VitimageUtils.writeStringInFile(sVictory, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/victorious_over_"+fibersStart.length+".txt");
		//Dessiner l'image sexy : dessiner en clair toutes les fibres qui ont reussi a atteindre le bout, en sombre celles qui ne l'atteignent pas
		int pixelStartOnPath=26;
		int incrStart=(int)Math.round(pixelStartOnPath/step);
		double decX=0.3;
		double decY=-0.4;
		double decZ=-0.1;
		double rayAtStart=decX*decX+decY*decY+decZ*decZ+1.5+fiberRay/10;
		double rayAtStop=(9*fiberRay)/10+rayAtStart;
		double diffRay=rayAtStop-rayAtStart;
		int color=0;
		double laplaceAlpha=0.03*255;
		double laplaceBeta=0.03 ;
		double laplaceGamma=8;
		int val;
		double distX;
		double rayUsed;
		for(int indCurSexy=0;indCurSexy<nSeeds;indCurSexy++) {
			System.out.print(indCurSexy+"/"+nSeeds+" ");
			if(indCurSexy%10 ==0)System.out.println();
			if(debug)System.out.print("Traitement fibre "+indCurSexy+"  a atteint l increment "+incr[indCurSexy]+" a l altitude= "+coords[indCurSexy][incr[indCurSexy]-1][2]);
			int incrMax=incr[indCurSexy];

			//Verifier si elle atteint le bas. Sinon ne pas la dessiner
			if(coords[indCurSexy][incr[indCurSexy]-1][2]<victoryZ) {System.out.println();continue;}
			
			if(debug)System.out.println(" is victorious...");
			//Dessiner la fibre
			for(int incrCurSexy=incrStart;incrCurSexy<incr[indCurSexy];incrCurSexy++) {
				xx=coords[indCurSexy][incrCurSexy][0];
				yy=coords[indCurSexy][incrCurSexy][1];
				zz=coords[indCurSexy][incrCurSexy][2];
				x0=(int)Math.round(xx);
				y0=(int)Math.round(yy);
				z0=(int)Math.round(zz);
					//val=(int)(( (valSegmentation[z0][(y0)*X+(x0)]))  &0xff );
					//if(val==1 || val==4)rayUsed=rayAtStart+(incrCurSexy*1.0/incrMax)*diffRay;
					//else rayUsed=0.00001;
//				System.out.println("Incr="+incrCurSexy+" rayUsed="+rayUsed);
				rayUsed=rayAtStart+(incrCurSexy*1.0/incrMax)*diffRay;
				for(int di=-fiberRayInt-3;di<fiberRayInt+4;di++)for(int dj=-fiberRayInt-3;dj<fiberRayInt+4;dj++) for(int dk=-fiberRayInt-3;dk<fiberRayInt+4;dk++){
					distX=Math.sqrt( ((di+decX)*(di+decX))+(dj+decY)*(dj+decY)+(dk+decZ)*(dk+decZ) )/rayUsed;
					//System.out.println("rayUsed="+rayUsed+" didjdk="+(di+decX)+" , "+(dj+decY)+" , "+(dk+decZ)+" et distX="+distX+" et func="+laplaceFunc(distX, laplaceAlpha, laplaceBeta, laplaceGamma));
					valFibersSexy[z0+dk][(y0+dj)*X+(x0+di)]+=(float)laplaceFunc(distX, laplaceAlpha, laplaceBeta, laplaceGamma);
				}
				//System.exit(0);
			}
		}
		return new ImagePlus[] {fibers,fibersSexy};
	}
	
	
	
	
	
	
	

	/**
	 * Value of a quadratic centered on alpha, with a beta scaling factor : f(x)= ( (x-alpha)/beta )^2
	 */
	public static double quadraticBoundsFunc(double x,double alpha,double beta) {
		return ((x-alpha)/beta)*((x-alpha)/beta);
	}

	/**
	 * Value of a laplace function centered on 0, with a alpha proportional factor, with a sqrt(beta) scaling factor : f(x)= ( (alpha)/(1+beta*beta) )
	 */
	public static double laplaceFunc(double x,double alpha,double beta) {
		return alpha/(beta+x*x);
	}
	
	
	/**
	 * Value of a laplace function centered on 0, with a alpha proportional factor, with a sqrt(beta) scaling, at power gamma : f(x)= ( (alpha)/(1+beta^gamma) )
	 */
	public static double laplaceFunc(double x,double alpha,double beta,double gamma) {
		return alpha/(beta+Math.pow(x, gamma));
	}

	/**
	 * Transformation of an image (a distance map for example), using quadraticBoundFunc
	 */
	public static ImagePlus quadraticBounds(ImagePlus imgRef,double alpha,double beta) {
		ImagePlus img=VitimageUtils.imageCopy(imgRef);
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		float[][] vals=new float[Z][];
		for(int z=0;z<Z;z++) {
			vals[z]=(float[])img.getStack().getProcessor(z+1).getPixels();
		}
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					vals[z][y*X+x]=(float)quadraticBoundsFunc(vals[z][y*X+x],alpha,beta);
				}			 
			}
		}
		img.setTitle("laplacefunc");
		return img;
	}

	
	/**
	 * Force vector between two particles P1 and P2, separated by a vector V12, F=normalized(V12) * alpha / norm(V12)^beta )  if norm(V12)<maxDistance , 0 else
	 */
	public static double[]repulsionForce(double[]coords1,double[]coords2,double alpha,double beta,double maxDistanceForComputation) {
		double[]vect12=TransformUtils.vectorialSubstraction(coords2, coords1);
		double vect12norm=TransformUtils.norm(vect12);
		if(vect12norm>maxDistanceForComputation)return new double[] {0,0,0};
		return TransformUtils.multiplyVector(vect12,alpha*Math.pow(vect12norm,-beta));
	}
	
	
	/**
	 * Transformation of an image (a distance map for example), using laplaceFunc
	 */
	public static ImagePlus laplace(ImagePlus imgRef,double alpha,double beta) {
		ImagePlus img=VitimageUtils.imageCopy(imgRef);
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		float[][] vals=new float[Z][];
		for(int z=0;z<Z;z++) {
			vals[z]=(float[])img.getStack().getProcessor(z+1).getPixels();
		}
		for(int z=0;z<Z;z++) {
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					vals[z][y*X+x]=(float)laplaceFunc(vals[z][y*X+x],alpha,beta);
				}			 
			}
		}
		img.setTitle("laplacefunc");
		return img;
	}

	
	/**
	 * Angle between two vectors
	 */
	public static double angle(double[]dir1,double []dir2) {
		return Math.acos(TransformUtils.scalarProduct(TransformUtils.normalize(dir1) , TransformUtils.normalize(dir2)));
	}
	
	
	
	
	
	
	
	
	









	
	
	
	public static ImagePlus[]fiberTrackingV1(ImagePlus distance,ImagePlus seeds,ImagePlus[]gradients, double[][] fibersStart,double fiberRay,double alphaExp,boolean doRepulsion, double alpha, double beta,double gamma,boolean fromBottomToTop,int earlyBreakIteration,boolean compareDebug,boolean debug,double step) {
		String s="";
		System.out.println("Starting fiber tracking");
		System.out.println("Ok. Params 1");
		if(debug)VitimageUtils.printImageResume(distance,"Distance");
		if(debug)VitimageUtils.printImageResume(gradients[0],"Grad X");
		distance.show();
		distance.setDisplayRange(0, 200);
		distance.setTitle("distance on");
		IJ.run(seeds,"32-bit","");
		System.out.println("Ok. Params 2");
		ImagePlus fibers=VitimageUtils.imageCopy(distance);
		fibers.setTitle("fibers");
		fibers=VitimageUtils.makeOperationOnOneImage(fibers,2,0, true);
		ImagePlus fibersSexy=VitimageUtils.imageCopy(fibers);
		fibersSexy.setTitle("Fibers sexy");
		System.out.println("Ok. Values");
		int pathMax=earlyBreakIteration;//100000;
		int fiberRayInt=(int)Math.ceil(fiberRay);
		int nSeeds=fibersStart.length;
		int nbRunning=nSeeds;
		int X=gradients[0].getWidth();
		int Y=gradients[1].getHeight();
		int Z=gradients[2].getStackSize();
		double xx,dx;
		double yy,dy;
		double zz,dz;
		int x0,y0,z0;
		int nbBrDist=0;
		int nbBrAngl=0;
		int nbBrAnglGrad=0;
		int nbBrBord=0;
		int nbBrFor=0;
		int incrMov=-1;
		int nbVictorious=0;
		
		//Algorithm parameters
		double stepMin=step/100;
		double angleMax=3.0;
		double minForward=-0.4;

		System.out.println("Ok. Tabs");
		double[][][]coords=new double[pathMax+2][nSeeds][3];
		double[][]distances=new double[pathMax+2][nSeeds];
		int incr=-1;
		int[]status=new int[nSeeds];
		coords[0]=fibersStart;
		double[]grad=new double[3];
		float[][][] grads=new float[3][Z][];
		float[][] valFibers=new float[Z][];
		float[][] valFibersSexy=new float[Z][];
		float[][] valDistance=new float[Z][];
		double[][][]factors=new double[2][2][2];
		double[][]tempGrads=new double[nSeeds][3];
		double[][]tempGradsWithRepulsion=new double[nSeeds][3];
		double[]gradDirWithRepulsion=new double[3];
		double[]gradDirWithoutRepulsion=new double[3];
		double[]saveGradNorm=new double[3];
		double []saveGrad=new double[3];
		int realInd1,realInd2;
		double[][][]forces;
		double[]repForce=new double[3];

		System.out.println("Ok. Image data management");
		for(int z=0;z<Z;z++) {
			for(int dim=0;dim<3;dim++)grads[dim][z]=(float []) gradients[dim].getStack().getProcessor(z+1).getPixels();
			valFibers[z]=(float[])fibers.getStack().getProcessor(z+1).getPixels();
			valFibersSexy[z]=(float[])fibersSexy.getStack().getProcessor(z+1).getPixels();
			valDistance[z]=(float[])distance.getStack().getProcessor(z+1).getPixels();
		}
		while(nbRunning>0 && incr<pathMax) {
			incr++;	
			int []indexes=new int[nbRunning];
			int indIncr=0;
			//Pour chaque chemin, si status=0
			for(int path=0;path<nSeeds;path++) {
				if(status[path]>0)continue;
				indexes[indIncr++]=path;
				tempGrads[path]=new double[] {0,0,0};
				//Calculer le gradient local par interpolation
				for(int dim=0;dim<3;dim++)grad[dim]=0;
				xx=coords[incr][path][0];
				yy=coords[incr][path][1];
				zz=coords[incr][path][2];
				x0=(int)Math.floor(coords[incr][path][0]);
				y0=(int)Math.floor(coords[incr][path][1]);
				z0=(int)Math.floor(coords[incr][path][2]);
				if(debug)System.out.println("\nIteration "+incr+" applied on particle with int coordinates  ( "+x0+" , "+y0+" , "+ z0+" )");
				if(compareDebug)s+="\n\nIteration "+incr+" applied on particle with int coordinates  ( "+x0+" , "+y0+" , "+ z0+" )";
				dx=xx-x0;
				dy=yy-y0;
				dz=zz-z0;
				factors[0][0][0]=(1-dx)*(1-dy)*(1-dz);
				factors[0][0][1]=(1-dx)*(1-dy)*(dz);
				factors[0][1][0]=(1-dx)*(dy)*(1-dz);
				factors[0][1][1]=(1-dx)*(dy)*(dz);
				factors[1][0][0]=(dx)*(1-dy)*(1-dz);
				factors[1][0][1]=(dx)*(1-dy)*(dz);
				factors[1][1][0]=(dx)*(dy)*(1-dz);
				factors[1][1][1]=(dx)*(dy)*(dz);
					
				if(compareDebug)s+="\nAVANT GRAD : "+TransformUtils.stringVectorDou(tempGrads[path],"",6);
				for(int di=0;di<2;di++)for(int dj=0;dj<2;dj++)for(int dk=0;dk<2;dk++)for(int dim=0;dim<3;dim++)tempGrads[path][dim]+=grads[dim][z0+dk][(y0+dj)*X+(x0+di)]*factors[di][dj][dk];
				if(compareDebug)s+="\nAPRES GRAD : "+TransformUtils.stringVectorDou(tempGrads[path],"",6);
			}
			if(!doRepulsion) {}
			else {				
				//Here is the work : check the position of everybody, and add a repulsive force, orthogonal to the gradient one, depending on the others
				//Calculer chacune des forces de repulsion entre deux points deux à deux
				forces=new double[indIncr][indIncr][3];
				for(int ind1=0;ind1<indIncr; ind1++){
					for(int ind2=ind1+1;ind2<indIncr; ind2++){
						realInd1=indexes[ind1];
						realInd2=indexes[ind2];
						repForce=repulsionForce(coords[incr][realInd1],coords[incr][realInd2],alpha,beta,gamma);
						forces[ind1][ind2]=TransformUtils.multiplyVector(repForce,-1);//La force qui s applique a realInd2
						forces[ind2][ind1]=TransformUtils.multiplyVector(repForce,1);//La force qui s applique a realInd1
					}
				}
				
				//Calculer la resultante appliquee à chaque point
				for(int ind1=0;ind1<indIncr; ind1++){
					double []res=new double[3];
					for(int ind2=0;ind2<indIncr; ind2++){
						if(ind2==ind1)continue;
						res=TransformUtils.sumVector(res,forces[ind1][ind2]);
					}

					//Calculer la composante orthogonale au gradient
					res=TransformUtils.vectorialSubstraction(res,TransformUtils.proj_u_of_v(tempGrads[indexes[ind1]], res));

					//L ajouter au gradient deja connu
					tempGradsWithRepulsion[indexes[ind1]]=TransformUtils.vectorialAddition(tempGrads[indexes[ind1]], res);
				}

			}
			for(int path=0;path<nSeeds;path++) {
				if(status[path]>0)continue;
				xx=coords[incr][path][0];
				yy=coords[incr][path][1];
				zz=coords[incr][path][2];
				x0=(int)Math.floor(coords[incr][path][0]);
				y0=(int)Math.floor(coords[incr][path][1]);
				z0=(int)Math.floor(coords[incr][path][2]);
				dx=xx-x0;
				dy=yy-y0;
				dz=zz-z0;
				gradDirWithRepulsion=TransformUtils.normalize(tempGradsWithRepulsion[path]);
				gradDirWithoutRepulsion=TransformUtils.normalize(tempGrads[path]);
				grad=TransformUtils.multiplyVector(gradDirWithRepulsion, step);
				
				//Avancer d un step. Ecrire le nouveau point. 
				for(int dim=0;dim<3;dim++)coords[incr+1][path][dim]=coords[incr][path][dim]+grad[dim];
				System.out.println("Valeur de "+TransformUtils.stringVectorDou(coords[incr][path],"")+" actualisée a : "+TransformUtils.stringVectorDou(coords[incr+1][path],""));
				if(compareDebug)s+="\nValeur de "+TransformUtils.stringVectorDou(coords[incr][path],"")+" actualisée a : "+TransformUtils.stringVectorDou(coords[incr+1][path],"");
				xx=coords[incr+1][path][0];
				yy=coords[incr+1][path][1];
				zz=coords[incr+1][path][2];
				x0=(int)Math.floor(coords[incr+1][path][0]);
				y0=(int)Math.floor(coords[incr+1][path][1]);
				z0=(int)Math.floor(coords[incr+1][path][2]);
				dx=xx-x0;
				dy=yy-y0;
				dz=zz-z0;
				//System.out.println("A ecrit les coordonnéées suivantes qui seront : "+TransformUtils.stringVectorDou(coords[incr+1][path], "")+" cad + "+TransformUtils.stringVectorDou(grad,"") );
				for(int di=-fiberRayInt;di<fiberRayInt+1;di++)for(int dj=-fiberRayInt;dj<fiberRayInt+1;dj++) for(int dk=-fiberRayInt;dk<fiberRayInt+1;dk++){
					if(di*di+dj*dj+dk*dk < fiberRay*fiberRay) {
						valFibers[z0+dk][(y0+dj)*X+(x0+di)]=255;
						valFibersSexy[z0+dk][(y0+dj)*X+(x0+di)]=255;
					}
				}
				valFibers[z0][y0*X+x0]=255;
				distances[incr+1][path]=0;
				factors[0][0][0]=(1-dx)*(1-dy)*(1-dz);
				factors[0][0][1]=(1-dx)*(1-dy)*(dz);
				factors[0][1][0]=(1-dx)*(dy)*(1-dz);
				factors[0][1][1]=(1-dx)*(dy)*(dz);
				factors[1][0][0]=(dx)*(1-dy)*(1-dz);
				factors[1][0][1]=(dx)*(1-dy)*(dz);
				factors[1][1][0]=(dx)*(dy)*(1-dz);
				factors[1][1][1]=(dx)*(dy)*(dz);
					
				for(int di=0;di<2;di++)for(int dj=0;dj<2;dj++)for(int dk=0;dk<2;dk++)distances[incr+1][path]+=(double)((float[])valDistance[z0+dk])[(y0+dj)*X+(x0+di)]*factors[di][dj][dk];

				//Verifier si la progression est respectee
				double valForward=distances[incr+1][path]-distances[incr][path];
				if(incr>1 && valForward<minForward) {status[path]=1;nbRunning--;System.out.println(" path "+path+" stopping because forward="+valForward);nbBrFor++;if(z0>546)nbVictorious++;}
				
						
/*
				System.out.println("info angle 1 :"+TransformUtils.stringVector( coords[incr+1][path]    , ""));
				System.out.println("info angle 2 :"+TransformUtils.stringVector( coords[incr][path]    , ""));
				System.out.println("info angle 3 :"+TransformUtils.stringVector( coords[incr][path]    , ""));
				if(incr>0)System.out.println("info angle 4 :"+TransformUtils.stringVector( coords[incr-1][path]    , ""));
				if(compareDebug)s+="\ninfo angle 1 :"+TransformUtils.stringVector( coords[incr+1][path]    , "");
				if(compareDebug)s+="\ninfo angle 2 :"+TransformUtils.stringVector( coords[incr][path]    , "");
				if(compareDebug)s+="\ninfo angle 3 :"+TransformUtils.stringVector( coords[incr][path]    , "");
				if(compareDebug && incr>0)s+="\ninfo angle 4 :"+TransformUtils.stringVector( coords[incr-1][path]    , "");
				*///Calculer l'angle formé, verifier si c est inferieur à seuil
				double valAngle=(incr==0) ? 0 : angle(TransformUtils.vectorialSubstraction(coords[incr+1][path], coords[incr][path]) ,
			               TransformUtils.vectorialSubstraction(coords[incr][path], coords[incr-1][path]));
				if(incr>0 && valAngle>angleMax){status[path]=1;nbRunning--;System.out.println(" path "+path+" stopping because angle="+valAngle);nbBrAngl++;if(z0>546)nbVictorious++;}
				
				//Verifier si le nouveau point est proche du bord de l'image
				double valProche= (coords[incr+1][path][0]<fiberRay+1) || (coords[incr+1][path][0]>X-(fiberRay+2)) ||
						 (coords[incr+1][path][1]<fiberRay+1) || (coords[incr+1][path][1]>Y-(fiberRay+2)) ||
						 (coords[incr+1][path][2]<fiberRay+1) || (coords[incr+1][path][2]>Z-(fiberRay+2)) ? 1 : 0;
				if(valProche>0  ) {status[path]=1;nbRunning--;System.out.println(" path "+path+" stopping because point approaching border : "+TransformUtils.stringVector(coords[incr+1][path],""));nbBrBord++;if(z0>546)nbVictorious++;}			
				//if(debug)System.out.println("   "+TransformUtils.stringVectorDou(new double[] {xx,yy,zz}, "coords double="));
				if(debug)System.out.println("   "+TransformUtils.stringVectorDou(gradDirWithoutRepulsion, "Direction avant rep")+"   "+TransformUtils.stringVectorDou(gradDirWithRepulsion, "Direction avec"));
				if(debug)System.out.println("   "+TransformUtils.stringVectorDou(grad, "grad utilisé"));
				if(debug)System.out.println("   ValAngle="+VitimageUtils.dou(valAngle,6)+"  valForward="+VitimageUtils.dou(valForward,6)+"  Potentiel i="+VitimageUtils.dou(distances[incr][path],6)+"  Potentiel i+1="+VitimageUtils.dou(distances[incr+1][path],6));
				if(compareDebug)s+="\n   "+TransformUtils.stringVectorDou(gradDirWithoutRepulsion, "Direction avant rep",6)+"   "+TransformUtils.stringVectorDou(gradDirWithRepulsion, "Direction avec",6);
				if(compareDebug)s+=	"\n   "+TransformUtils.stringVectorDou(grad, "grad utilisé",6);
				if(compareDebug)s+="\n   ValAngle="+VitimageUtils.dou(valAngle,6)+"  valForward="+VitimageUtils.dou(valForward,6)+"  Potentiel i="+VitimageUtils.dou(distances[incr][path],6)+"  Potentiel i+1="+VitimageUtils.dou(distances[incr+1][path],6);
			}	
			
			if(earlyBreakIteration>1000)System.out.println(" fin iteration, nb Path start="+nSeeds+" nb at the end="+nbRunning+" nbBrBorder="+nbBrBord+" nbBrAng="+nbBrAngl+" nbBrAngGrad="+nbBrAnglGrad+" nbBrDis="+nbBrDist+" nbBrFor="+nbBrFor);
		}			
		System.out.println("\nNb Path start="+nSeeds+" nb at the end="+nbRunning+" nbBrBorder="+nbBrBord+" nbBrAng="+nbBrAngl+" nbBrAngGrad="+nbBrAnglGrad+" nbBrDis="+nbBrDist+" nbBrFor="+nbBrFor);
		if(compareDebug)s+="\n\nNb Path start="+nSeeds+" nb at the end="+nbRunning+" nbBrBorder="+nbBrBord+" nbBrAng="+nbBrAngl+" nbBrAngGrad="+nbBrAnglGrad+" nbBrDis="+nbBrDist+" nbBrFor="+nbBrFor;
		System.out.println("Nb victorious = "+nbVictorious+" / "+nSeeds);
		if(compareDebug)s+="\nNb victorious = "+nbVictorious+" / "+nSeeds;
		if(compareDebug)VitimageUtils.writeStringInFile(s, "/home/fernandr/Bureau/fiberV1.txt");
		return new ImagePlus[] {fibers,fibersSexy};
	}
	
	
	
	
	public static double[][] buildFibersStart(ImagePlus seeds,int nSeeds,double radius) {
		System.out.println("Construction du start des fibres, avec nb fibres = "+nSeeds+" . Show image :");
		ArrayList<double[]>points=new ArrayList();
		double[][]ret=new double[nSeeds][3];
		int X=seeds.getWidth();
		int Y=seeds.getHeight();
		int Z=seeds.getStackSize();
		byte[][] vals=new byte[Z][];
		for(int z=0;z<Z;z++) {
			vals[z]=(byte[])seeds.getStack().getProcessor(z+1).getPixels();
		}

		seeds.show();
		//Build list of potential points for starting
		for(int z=0;z<Z;z++)for(int x=0;x<X;x++)for(int y=0;y<Y;y++) if(((byte)( vals[z][y*X+x] & 0xff)) != ((byte) 0 & 0xff))points.add(new double[] {x,y,z}); 
		System.out.println("Inventaire des points seeds potentiels : "+points.size());
		
		
		//Randomly take some and remove neighbouring ones according to radius parameter. If not enough point left, abort
		for(int n=0;n<nSeeds;n++) {
			int selected=(int)Math.round(Math.random()*(points.size()-1));
			ret[n]=points.get(selected);
			System.out.println("Iteration # "+n+" point selectionne "+TransformUtils.stringVector(ret[n], ""));
			for(int nR=points.size()-1;nR>=0;nR--) {
				if(TransformUtils.norm(TransformUtils.vectorialSubstraction(points.get(nR), ret[n]))<radius)points.remove(nR);
			}
			System.out.println(" apres retrait des points proches, reste "+points.size());
			if(points.size()<1 && n<nSeeds-1) {System.out.println("Abort : pas assez de points potentiels, ou fibres trop grosses.");System.exit(0);}
		}
		System.out.println("");
		return ret;
	}
	
	
	

	

/*		if(computeSeeds) seeds=buildSeedImage(numSpec,spec,targetVoxelSize,sliceRootStock,fromTheBack,useAllSpecimenForPathways,pointsNecr,pointsSain,nbSeedsPerArea);				
		else {
			if(doFastTest)seeds=IJ.openImage("/home/fernandr/Bureau/seeds"+(fromTheBack ? "_BOTTOM" : "_TOP")+".tif");
			else seeds=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seeds"+(fromTheBack ? "_BOTTOM" : "_TOP")+".tif");
		}
*/

	
	public static ImagePlus buildSeedImage(int numSpec,String spec,double targetVoxelSize,int sliceRootStock, boolean fromTheBack,boolean useAllSpecimenForPathways,int[][]pointsNecr,int[][]pointsSain,int nbSeedsPerArea) {
		System.out.println("Building seed image...");
		int segmentationKind=(useAllSpecimenForPathways ? 1 : 0); //0 = sain , 1=all		
		double[]factors={0.722/targetVoxelSize,0.722/targetVoxelSize,1/targetVoxelSize};
		int sliceStart=(int)Math.round(sliceRootStock/targetVoxelSize);
		ImagePlus seedsForFibers=null;
		
		if(! fromTheBack) {
			System.out.println("Ok. From the top...");
			//Take an image at the right size to match the distance map
			seedsForFibers=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_SAIN.tif");
			seedsForFibers=VitimageUtils.makeOperationOnOneImage(seedsForFibers, 2, 0, true);
			seedsForFibers=VitimageUtils.subXYZ(seedsForFibers, factors, true);

			System.out.println("Ok. setting healthy points...");
			VitimageUtils.printImageResume(seedsForFibers,"Seeds for fibers");
			//Seed the attempted points
			for(int i=0;i<pointsSain.length;i++) {
				System.out.println("  i="+i+" : "+((int)Math.round(pointsSain[i][0]*factors[0]))+" , "+((int)Math.round(pointsSain[i][1]*factors[1]))+" , "+((int)Math.round(pointsSain[i][2]*factors[2])));
				for(int di=0;di<nbSeedsPerArea;di++)for(int dj=0;dj<nbSeedsPerArea;dj++) {
					seedsForFibers=VitimageUtils.drawPointAtPixelCoordinatesInImageModifyingIt(seedsForFibers, 
							(int)Math.round(pointsSain[i][0]*factors[0])+di, (int)Math.round(pointsSain[i][1]*factors[1])+dj, (int)Math.round(pointsSain[i][2]*factors[2]),255);
				}
			}
			System.out.println("Ok. setting necrotic points...");
			for(int i=0;i<pointsNecr.length;i++) {
				System.out.println("  i="+i+" : "+((int)Math.round(pointsNecr[i][0]*factors[0]))+" , "+((int)Math.round(pointsNecr[i][1]*factors[1]))+" , "+((int)Math.round(pointsNecr[i][2]*factors[2])));
				for(int di=0;di<nbSeedsPerArea;di++)for(int dj=0;dj<nbSeedsPerArea;dj++) {
					seedsForFibers=VitimageUtils.drawPointAtPixelCoordinatesInImageModifyingIt(seedsForFibers, 
						(int)Math.round(pointsNecr[i][0]*factors[0])+di, (int)Math.round(pointsNecr[i][1]*factors[1])+dj, (int)Math.round(pointsNecr[i][2]*factors[2]),255);
				}
			}
			System.out.println("Ok. saving...");
			seedsForFibers=VitimageUtils.thresholdByteImage(seedsForFibers, 127,256);
			IJ.saveAsTiff(seedsForFibers, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seeds_TOP.tif");
		}
		else {
			System.out.println("Ok. From the back...");
			//Get the volume used for pathways ...
			ImagePlus segSainAniso=null;
			if(segmentationKind==0)segSainAniso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation_SAIN.tif");
			else if(segmentationKind==1) {
				segSainAniso=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/segmentation.tif");
				segSainAniso=VitimageUtils.thresholdByteImage(segSainAniso,1,256);
			}

			System.out.println("Ok. preparing image...");
			seedsForFibers=VitimageUtils.subXYZ(segSainAniso, factors, true);
			seedsForFibers=VitimageUtils.thresholdByteImage(seedsForFibers,127, 256);
			int X=seedsForFibers.getWidth();		int Y=seedsForFibers.getHeight();	int Z=seedsForFibers.getStackSize();
			seedsForFibers=VitimageUtils.drawParallepipedInImage(seedsForFibers, 0, 0, 0, X-1, Y-1, sliceStart-3,0);
			seedsForFibers=VitimageUtils.drawParallepipedInImage(seedsForFibers, 0, 0, sliceStart-1, X-1, Y-1, Z-1,0);

			System.out.println("Ok. contouring cambium...");
			Strel3D strPti6XY=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(4,4,0);
			Strel3D strPti1XY=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(1,1,0);
			ImagePlus seedsEro =new ImagePlus("",Morphology.erosion(seedsForFibers.getImageStack(),strPti6XY));
			ImagePlus seedsEro2 =new ImagePlus("",Morphology.erosion(seedsEro.getImageStack(),strPti1XY));
			seedsForFibers=VitimageUtils.makeOperationBetweenTwoImages(seedsEro, seedsEro2, 4,true);
			System.out.println("Ok. saving...");
			IJ.saveAsTiff(seedsForFibers, "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/seeds_BOTTOM.tif");
		}
		return seedsForFibers;
	}
	
	
	public static ImagePlus removeBarkNonConnectedToBackgroundIn2D(ImagePlus seg,int volumeMin) {
		int Z=seg.getStackSize();
		int X=seg.getWidth();
		int Y=seg.getHeight();
		ImagePlus []slicesCorrected=new ImagePlus[Z];
		Strel3D str8=inra.ijpb.morphology.strel.CuboidStrel.fromRadiusList(1,1,1);
		for(int z=0;z<Z;z++) {
			System.out.print(z+" / "+Z);
			//Extraire la slice
			ImagePlus slice=VitimageUtils.cropImageByte(seg, 0, 0, z, X, Y, 1);
			//Extraire le background
			ImagePlus backMask=VitimageUtils.thresholdByteImage(slice,0,1);
			
			//Extraire les n composantes connexes de la slice
			ImagePlus barkMask=VitimageUtils.thresholdByteImage(slice, 4,5);
			ImagePlus composantes=VitimageUtils.connexe(barkMask,1,256, 0, 10E10, 8,0, true);
			ImagePlus compNonUsed=VitimageUtils.thresholdShortImage(composantes,1,65000);
			compNonUsed=VitimageUtils.binaryOperationBetweenTwoImages(barkMask, compNonUsed, 4);//Image of other components
			compNonUsed=VitimageUtils.switchValueInImage(compNonUsed,255,4);
			int nComposantes=(int)Math.round(VitimageUtils.maxOfImage(composantes));
			boolean []keep=new boolean[nComposantes];
	
			//Pour chacune
			ImagePlus sliWithoutBark=VitimageUtils.thresholdByteImage(slice, 1,4);
			ImagePlus additionOfBarks=VitimageUtils.nullImage(slice);
			System.out.println("  Ncomposantes="+nComposantes);
			for(int comp=0;comp<nComposantes;comp++) {
				//La dilater en 2D
				ImagePlus sliComp=VitimageUtils.thresholdShortImage(composantes,comp+1,comp+2);
				ImagePlus sliCompDil=new ImagePlus("",Morphology.dilation(sliComp.getImageStack(),str8));
				
				//Regarder si l intersecton avec le background est nulle.
				//Si l intersection est non nulle, on la garde en l etat. Sinon on met du bois noir a la place
				keep[comp]=!VitimageUtils.isNullImage(VitimageUtils.binaryOperationBetweenTwoImages(sliCompDil,backMask, 2));
				sliComp=VitimageUtils.switchValueInImage(sliComp,255, (keep[comp] ? 4 : 2)); 
				additionOfBarks=VitimageUtils.makeOperationBetweenTwoImages(additionOfBarks,sliComp,1,false);
			}
			slice=VitimageUtils.switchValueInImage(slice,4,0);
			slicesCorrected[z]=VitimageUtils.makeOperationBetweenTwoImages(slice, additionOfBarks, 1, false);
			slicesCorrected[z]=VitimageUtils.makeOperationBetweenTwoImages(slicesCorrected[z],compNonUsed, 1, false);
		}

		//Reassembler l image de segmentation
		ImagePlus ret= Concatenator.run(slicesCorrected);
		VitimageUtils.adjustImageCalibration(ret, seg);
		return ret;
	}
	





}
