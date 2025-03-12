package io.github.rocsg.fijiyama.testromain;

import java.awt.Color;
import java.io.File;

import inra.ijpb.geometry.Ellipse;
import inra.ijpb.measure.region2d.InertiaEllipse;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel3D;
import inra.ijpb.watershed.MarkerControlledWatershedTransform2D;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import io.github.rocsg.fijiyama.common.Timer;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;

@SuppressWarnings("deprecation")
public class PosterSorgho {	
	static int []splitTab2={
			9,9,9,
			8,8,8,8,8,
			7,7,7,7,7,7,7,7,
			6,6,6,6,
			5,5,5,5,5,5,5,5,
			4,4,4,4,
			3,3,3,3,
			2,2,2,2,
			1,1,1,1,1,1
		};
	static String []specTab2={
			/*Split 11  0*/			
			
			
			/*Split 9  3*/
			"2017_G80_P2_E21",/*8*/
			"2019_G80_P3_E10",/*9*/
			"2018_G80_P5_E17",/*10*/

			/*Split 8  5*/			
			"2017_G28_P3_E15",/*9*/
			"2019_G01_P85_E14",/*8*/
			"2017___",/*8*/
			"2017_G26_P5_E19",/*9*/
			"2017_G1_P5_E14",/*10*/
			
			/*Split 7  8*/
			"2017_G1_P5_E20"/*8 */,
			"2019_G80_P96_E10"/*8 */,
			"2019_G26_P38_E12",/*8*/
			"2019_G80_P51_E9",/*8*/
			"2018_G28_P1_E10",/*8*/
			"2019_G26_P38_E13",/*8*/
			"2017_G28_P5_E15",/*9*/
			"2017_G28_P4_E13",/*8*/

			/*Split 6  4 */
			"2017_G1_P2_E15",/*9*/
			"2017_G1_P2_E17",/*8*/
			"2019_G26_P83_E13",/*9*/
			"2017_G28_P1_E15",/*8*/

			/*Split 5    8*/
			"2017_G1_P1_E19",/*8*/
			"2017_G28_P2_E21",/*8*/
			"2019_G80_P33_E9",/*9*/
			"2017_G26_P4_E19",/*8*/
			"2019_G26_P110_E13",/*8*/
			"2017_G80_P5_E12",/*9*/
			"2017_G80_P4_E11",/*9*/
			"2019_G26_P110_E13",/*9*/

			/*Split 4       4*/
			"2019_G01_P61_E12",/*8*/
			"2019_G80_P30_E9",/*8*/
			"2019_G80_P66_E14",/*8*/
			"2017_G28_P1_E17",/*8*/

			/*Split 3      4*/
			"2019_G80_P15_E10",/*8*/
			"2017_G1_P2_E19",/*10*/
			"2019_G80_P57_E9",/*9*/
			"2017_G1_P4_E15",/*9*/

			/*Split 2      4*/
			"2019_G80_P42_E8",/*9*/
			"2019_G01_P85_E13",/*9*/
			"2019_G26_P11_E13",/*9*/
			"2019_G01_P16_E14",/*9*/

			/*Split 1     6*/
			"2017_G28_P2_E15",/*9*/			
			"2017_G26_P5_E23",/*9*/
			"2019_G26_P53_E14",/*9*/			
			"2017_G28_P4_E15",/*9*/			
			"2017_G80_P5_E16",/*9*/			
			"2017_G80_P3_E15",/*9*/	
	};
	static String []specTab={
			/*Split 11  0*/			
			/*Split 9  2*/
			"2019_G80_P3_E10",/*9*/
			"2018_G80_P5_E17",/*10*/ //////////////////

			/*Split 8  3*/			
			"2017_G28_P3_E15",/*9*/
			"2017_G26_P5_E19",/*9*/
			"2017_G1_P5_E14",/*10*/ //////////////////
			
			/*Split 7  1*/
			"2017_G28_P5_E15",/*9*/

			/*Split 6  2 */
			"2017_G1_P2_E15",/*9*/
			"2019_G26_P83_E13",/*9*/

			/*Split 5    4*/
			"2019_G80_P33_E9",/*9*/
			"2017_G80_P5_E12",/*9*/
			"2017_G80_P4_E11",/*9*/
			"2019_G26_P110_E13",/*9*/

			/*Split 4       0*/

			/*Split 3      3*/
			"2017_G1_P2_E19",/*10*/ //////////
			"2019_G80_P57_E9",/*9*/
			"2017_G1_P4_E15",/*9*/

			/*Split 2      4*/
			"2019_G80_P42_E8",/*9*/
			"2019_G01_P85_E13",/*9*/
			"2019_G26_P11_E13",/*9*/
			"2019_G01_P16_E14",/*9*/

			/*Split 1     6*/
			"2017_G28_P2_E15",/*9*/			
			"2017_G26_P5_E23",/*9*/
			"2019_G26_P53_E14",/*9*/			
			"2017_G28_P4_E15",/*9*/			
			"2017_G80_P5_E16",/*9*/			
			"2017_G80_P3_E15",/*9*/	
	};
	static int []splitTab={
			9,9,
			8,8,8,
			7,
			6,6,
			5,5,5,5,
			3,3,3,
			2,2,2,2,
			1,1,1,1,1,1
		};
	static int []centerTab= {
			42,54,
			42,42,42,
			42,
			42,42,
			42,42,42,42,
			42,42,42,
			42,42,42,42,
			42,42,42,42,42,42
	};
	static int []widthTab= {
			19,20,
			19,19,19,
			19,
			19,19,
			19,19,19,19,
			19,19,19,
			19,19,19,19,
			19,19,19,19,19,19
	};

	
	public static void main(String[] args) {
		Timer t=new Timer();
		for(int numSpec=0;numSpec<specTab.length;numSpec++) {
			if(numSpec!=1)continue;
			System.out.println("\nProcessing "+numSpec+"/"+specTab.length);
			t.print("run1");
			//run1(numSpec);
			t.print("run2");
			run2(numSpec);
			System.out.println("Ok");
		}
	}
	
	public static void merde2() {
		for(int numSpec=0;numSpec<specTab.length;numSpec++) {
			File f=new File("/media/rfernandez/BACKUP_DATA_RO_A1/DATA_RO_A/Sorgho_BFF/Vaisseaux/Data/Computation_and_results/Step_02/Splits/split_"+splitTab[numSpec]+
					"_over_8/"+specTab[numSpec]);
			if(!f.exists())System.out.println("/split_"+splitTab[numSpec]+"_over_8/"+specTab[numSpec]+"   --> exists ? "+f.exists());
		}
	}
	
	public static void merde() {
		String dir="/media/rfernandez/BACKUP_DATA_RO_A1/DATA_RO_A/Sorgho_BFF/Vaisseaux/Data/Computation_and_results/Step_02/Splits/split_1_over_8/";
		File f=new File(dir);
		String[]tabSpec=f.list();
		System.out.println(f+" exists ? "+f.exists()+" "+ tabSpec.length);
		for(String sp : tabSpec) {
			ImagePlus img=IJ.openImage(dir+sp+"/slice_sub_8.tif");
			img.show();
			VitimageUtils.waitFor(700);
			ImagePlus img2=IJ.openImage(dir+sp+"/Cute_distance_voronoi.tif");
			img2.show();
			System.out.println();
			VitiDialogs.getYesNoUI("","split_1 "+sp);
			
		}
	}
	
	
	public static void run1(int numSpec) {
		///media/rfernandez/BACKUP_DATA_RO_A1/DATA_RO_A/Sorgho_BFF/Vaisseaux/Data/Computation_and_results/Step_02/Splits/split_7_over_8/2017_G1_P4_E19
		//Generate coloured voronoi
		ImageJ ij=new ImageJ();
		ImagePlus img= generateColouredVoronoiMode1HighRes(splitTab[numSpec],specTab[numSpec]);
		IJ.saveAsTiff(img,"/home/rfernandez/Bureau/A_Test/Poster_sorgho/vor/"+specTab[numSpec]+"_vor.tif");
			
	}
	
	
	public static void run2(int numSpec) {
		ImageJ ij=new ImageJ();
		ImagePlus vorRGB=IJ.openImage("/home/rfernandez/Bureau/A_Test/Poster_sorgho/vor/"+specTab[numSpec]+"_vor.tif");
		ImagePlus sourceRGB=IJ.openImage("/media/rfernandez/BACKUP_DATA_RO_A1/DATA_RO_A/Sorgho_BFF/Img_lvl_1/"+specTab[numSpec]+".tif");
		sourceRGB.getProcessor().setMinAndMax(25, 178);//35 193
		ImagePlus targetRGB=VitimageUtils.fadeRGB(sourceRGB,vorRGB,centerTab[numSpec],widthTab[numSpec]);
		IJ.saveAsTiff(targetRGB, "/home/rfernandez/Bureau/A_Test/Poster_sorgho/Results/"+specTab[numSpec]+".tif");
		/*		
		
		String dir="/media/rfernandez/BACKUP_DATA_RO_A1/DATA_RO_A/Sorgho_BFF/Vaisseaux/Figures";
		ImagePlus sourceRGB=IJ.openImage(dir+"/Fading_sourceBright.tif");
		ImagePlus segRGB=IJ.openImage(dir+"/Fading_seg.tif");
		VitimageUtils.printImageResume(sourceRGB);
		VitimageUtils.printImageResume(segRGB);
		
		ImagePlus targetRGB=VitimageUtils.fadeRGB(sourceRGB,segRGB,53,15);
		targetRGB.show();		*/
	}
	
	
	
	
	
	
	
	public static double[]getDistancesFromCenterToContour(double[][]coords,ImagePlus img,double[]center){
		int n=coords.length;
		double[]distances=new double[coords.length];

		int xM=img.getWidth();
		int yM=img.getHeight();
		byte[]vals=(byte [])img.getStack().getProcessor(1).getPixels();
		for(int i=0;i<n;i++) {
			double dx=coords[i][0]-center[0];
			double dy=coords[i][1]-center[1];
			double ab=Math.sqrt(dx*dx+dy*dy);
			dx=dx/ab;
			dy=dy/ab;
			boolean found=false;
			boolean out=false;
			double X=coords[i][0];
			double Y=coords[i][1];
			while((!out) && (!found)) {
				//System.out.println("Iter : "+X+","+Y);
				X+=0.5*dx;
				Y+=0.5*dy;
				int XX=(int)Math.round(X);
				int YY=(int)Math.round(Y);
				if( (XX<0) || (YY<0) ||  (XX>=xM) ||  (YY>=yM) ) {
					out=true;
					continue;
				}
				if ((vals[xM*YY+XX] & 0xff )> 0)found=true;					
			}

			if(out) {
				distances[i]=1;
			}
			else {
				double dx1=coords[i][0]-center[0];
				double dy1=coords[i][1]-center[1];
				double dx2=X-center[0];
				double dy2=Y-center[1];
				double dist1=Math.sqrt(dx1*dx1+dy1*dy1);
				double dist2=Math.sqrt(dx2*dx2+dy2*dy2);
				distances[i]=dist1/dist2;
				//System.out.println(" distances["+i+"]="+distances[i]);
			}				
			//VitimageUtils.waitFor(100);
		}
		
		
		return distances;
	}
	
	
	
	
	
	  public static String getVesselsDir() {
	    	return "/media/rfernandez/BACKUP_DATA_RO_A1/DATA_RO_A/Sorgho_BFF/Vaisseaux";
	    }
	  
	
	    public static String getSplitDir(int split) {
	    	return getVesselsDir()+"/Data/Computation_and_results/Step_02/Splits/"+"split_"+split+"_over_8";
	    }

	    
	  	public static ImagePlus getVoronoi(ImagePlus imgBin,boolean binary) {
	  		ImagePlus t1=imgBin.duplicate();
	  		t1=VitimageUtils.invertBinaryMask(t1);
	  		IJ.run(t1, "Voronoi", "");
	  		if(binary)t1=VitimageUtils.thresholdByteImage(t1, 1, 256);
	  		return t1;
	  	}
	  
	    
	    
	    
	    
	    public static ImagePlus generateColouredVoronoiMode1HighRes(int split,String spec) {
	    	String splitDir=getSplitDir(split);
	    	String pathToCsv=splitDir+"/"+spec+"/Vessels_descriptor_anomaly_check.csv";
	    	int extMode=1;//0=blanc, 1=grey, 2=keep
	    	
	    	
	    	//Get slice center
	    	String[][]data=VitimageUtils.readStringTabFromCsv(splitDir+"/"+spec+"/circle.csv");
	    	System.out.println(splitDir+"/"+spec+"/circle.csv");
	    	double[]sliceCenter=new double[] {Double.parseDouble(data[0][0]),Double.parseDouble(data[0][1])};
	    	
	    	//////////MULTIPLY BY 8
	    	for (int i=0;i<sliceCenter.length;i++)sliceCenter[i]*=8;
	    	
	    	
	    	//Get contour mask
			ImagePlus imgContour=IJ.openImage(splitDir+"/"+spec+"/concave_hull.tif");//Is 8 times lower
			ImagePlus imgSeg=IJ.openImage(splitDir+"/"+spec+"/segmentation_no_out.tif");
			int Xpti=imgContour.getWidth();
			int Ypti=imgContour.getHeight();
			
			imgContour=VitimageUtils.resizeNearest(imgContour, Xpti*8, Ypti*8, 1);
			IJ.run(imgContour, "Gaussian Blur...", "sigma=4");
			imgContour=VitimageUtils.thresholdByteImage(imgContour, 128, 256);
			ImagePlus circle=VitimageUtils.imageCopy(imgContour);
			
			imgSeg=VitimageUtils.resizeNearest(imgSeg, Xpti*8, Ypti*8, 1);
			IJ.run(imgSeg, "Gaussian Blur...", "sigma=8");
			imgSeg=VitimageUtils.thresholdByteImage(imgSeg, 128, 256);
			ImagePlus voronoi=getVoronoi(imgSeg, true);
			voronoi=dilation(voronoi, 6, false);
			           

			//////////RESAMPLE ALSO ?
			//Take voronoi mask and Compute colormap of voronoi against distance
			ImagePlus imgVorCells=VitimageUtils.imageCopy(voronoi);
			ImagePlus imgVorInv=VitimageUtils.imageCopy(voronoi);
			IJ.run(imgVorInv,"Invert","");
			IJ.run(imgVorCells,"Invert","");
			Roi[]rois=VitimageUtils.segmentationToRoi(imgVorInv);
			double[][]centroidsVor=new double[rois.length][];
			///imgVorInv.show();

			
			///////////SHOULD BE ROBUST TO RESAMPLING ?
			for(int n=0;n<rois.length;n++) {
				centroidsVor[n]=rois[n].getContourCentroid();
				double[]distances=getDistancesFromCenterToContour(new double[][] {centroidsVor[n]}, imgContour, sliceCenter);
				imgVorInv.setRoi(rois[n]);
				int value=(int)Math.round(distances[0]*255);
				imgVorInv.setColor(new Color(value,value,value));
				imgVorInv.getRoi().setFillColor(new Color(value,value,value));
			    IJ.run(imgVorInv,"Fill","");
				imgVorInv.resetRoi();
			}
			ImagePlus vorInsideDistanceColor=imgVorInv.duplicate();
			ImagePlus vorInsideDistanceColorOne=imgVorInv.duplicate();
			IJ.run(vorInsideDistanceColorOne,"8-bit","");
			IJ.run(vorInsideDistanceColorOne,"32-bit","");
			vorInsideDistanceColorOne=VitimageUtils.makeOperationOnOneImage(vorInsideDistanceColorOne, 3, 255, true);
			
			IJ.run(vorInsideDistanceColor,"Fire","");
			IJ.run(vorInsideDistanceColor,"RGB Color", "");
			
			
			
			
			//Get initial source data
			ImagePlus imgSourceRGB=IJ.openImage("/media/rfernandez/BACKUP_DATA_RO_A1/DATA_RO_A/Sorgho_BFF/Img_lvl_1/"+spec+".tif");///////SET SLICE NON SUB
			ImagePlus imgSourceGray=VitimageUtils.imageCopy(imgSourceRGB);
			IJ.run(imgSourceGray,"8-bit","");
			double[]circleCoords=VitimageUtils.stringTabToDoubleTab(				VitimageUtils.readStringTabFromCsv(						new File(splitDir+"/"+spec+"/circle.csv").getAbsolutePath())[0]);	
	    	//////////MULTIPLY BY 8
	    	for (int i=0;i<circleCoords.length;i++)circleCoords[i]*=8;
 
			
			 
			//Generate mask of contour inside
			ImagePlus disk=circle.duplicate();
			IJ.run(disk,"Invert","");		IJ.run(disk,"Fill Holes","");		IJ.run(disk,"Invert","");
			ImagePlus voronoiIn=VitimageUtils.binaryOperationBetweenTwoImages(voronoi, disk, 2);
			ImagePlus contours=VitimageUtils.binaryOperationBetweenTwoImages(voronoiIn, circle, 1);
			ImagePlus maskVoronoi=VitimageUtils.getBinaryMaskUnary(contours, 0.5);
			
			
			int[]colorContourVess=new int[] {200,200,200};
			int contourVessSize=2;
			double alphaGrey=1.6;//2.0
			int offsetGrey=90;//120
			double dzetaGrey=1.4;
			double betaGrey=0.15;
			int[]colorContourVoronoi=new int[] {20,20,255};
			ImagePlus contourVess=imgSeg.duplicate();
			dilation(contourVess, contourVessSize*4, false);///////MULTIPLIED BY 4
			contourVess=VitimageUtils.binaryOperationBetweenTwoImages(contourVess, imgSeg, 4);
			contourVess=VitimageUtils.getBinaryMaskUnary(contourVess, 0.5);

			//We have segmentation_no_out, mask of cells
			//contours, mask of voronoi and circle
			//imgVorCells, mask of voronoi areas inside the circle
			//contourVess
			//vorInsideDistanceColor, imagecoloured with respect to distance inside influence areas
			
			//Build mask of only distance color
			imgSeg=VitimageUtils.getBinaryMaskUnary(imgSeg, 0.5);
			ImagePlus maskOnlyDistance=VitimageUtils.binaryOperationBetweenTwoImages(contours, contourVess	, 1);
			maskOnlyDistance=VitimageUtils.binaryOperationBetweenTwoImages(maskOnlyDistance, imgSeg	, 1);
			maskOnlyDistance=VitimageUtils.invertBinaryMask(maskOnlyDistance);		
			
			
			//Prepare data
			ImagePlus[]imgSourceVess=VitimageUtils.splitRGBStackHeadLess(imgSourceRGB);
			ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
			ImagePlus imgSourceRGBGrey2=imgSourceRGB.duplicate();
			IJ.run(imgSourceRGBGrey,"8-bit","");
			IJ.run(imgSourceRGBGrey,"32-bit","");
			ImagePlus imgSourceRGBGreyExt=imgSourceRGBGrey.duplicate();
			IJ.run(imgSourceRGBGreyExt,"RGB Color","");
			imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 3	, alphaGrey, true);
			imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 1	, offsetGrey, true);
			imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 2	, 1.0/255.0, true);
			vorInsideDistanceColorOne=VitimageUtils.makeOperationOnOneImage(vorInsideDistanceColorOne, 2, betaGrey, true);
			imgSourceRGBGrey=VitimageUtils.makeOperationBetweenTwoImages(imgSourceRGBGrey, vorInsideDistanceColorOne, 1, true);
			//imgSourceRGBGrey.show();
			//VitimageUtils.waitFor(500000);
			ImagePlus[]imgSourceDist=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
			ImagePlus[]imgContourVess=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
			ImagePlus[]imgContourVor=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
			ImagePlus[]imgExtRGB=VitimageUtils.splitRGBStackHeadLess(extMode==1 ? imgSourceRGBGreyExt : imgSourceRGBGrey2);
			contourVess=VitimageUtils.getBinaryMaskUnaryFloat(contourVess, 0.5);
			contours=VitimageUtils.getBinaryMaskUnaryFloat(contours, 0.5);
			ImagePlus[]ret=new ImagePlus[3];
			disk=VitimageUtils.getBinaryMaskUnaryFloat(disk, 0.5);
			ImagePlus diskOut=VitimageUtils.invertBinaryMask(disk);
			//contours.show();
			
			for(int can=0;can<3;can++) {
				//Reset values of distance color where needed
				imgSourceDist[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], maskOnlyDistance, 2, true);
				//Multiply by grey values
				imgSourceDist[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], imgSourceRGBGrey, 2, true);
				imgSourceDist[can]=VitimageUtils.makeOperationOnOneImage(imgSourceDist[can], 2,dzetaGrey, true);
				IJ.run(imgSourceDist[can],"8-bit","");

				//Reset values of vessels color where needed
				imgSourceVess[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceVess[can],imgSeg, 2, true);
				imgSourceVess[can]=VitimageUtils.makeOperationOnOneImage(imgSourceVess[can],1,1.4, true);
				IJ.run(imgSourceVess[can],"8-bit","");

				//Set contourVess
				imgContourVess[can]=VitimageUtils.makeOperationOnOneImage(contourVess, 2, colorContourVess[can],true);
				IJ.run(imgContourVess[can],"8-bit","");

				//Set contour
				imgContourVor[can]=VitimageUtils.makeOperationOnOneImage(contours, 2, colorContourVoronoi[can],true);
				imgContourVor[can].setDisplayRange(0, 255);
				IJ.run(imgContourVor[can],"8-bit","");

				
				//Add everything
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], imgSourceVess[can], 1, false);
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgContourVess[can], 1, false);
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgContourVor[can], 1, false);
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],disk,2, false);
				
				//Process outside
				imgExtRGB[can]=VitimageUtils.makeOperationBetweenTwoImages(imgExtRGB[can],diskOut,2, false);
				imgExtRGB[can]=VitimageUtils.makeOperationOnOneImage(imgExtRGB[can],2, 1.5,false);
				if(extMode==0) {
					imgExtRGB[can]=VitimageUtils.makeOperationOnOneImage(imgExtRGB[can],2,1000000000,false);
				}
				ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgExtRGB[can], 1, false);
			}

			
			
			ImagePlus result=VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
			result.setTitle(spec);
			//result.show();
			
			VitimageUtils.waitFor(20);
			imgVorInv.changes=false;
			imgVorInv.close();//2016_G1_P3_E19 jolie
			return result;
	    }


	    
	    
	
    public static ImagePlus generateColouredVoronoiMode1(int split,String spec) {
    	String splitDir=getSplitDir(split);
    	String pathToCsv=splitDir+"/"+spec+"/Vessels_descriptor_anomaly_check.csv";
    	int extMode=1;//0=blanc, 1=grey, 2=keep
    	
    	//Get slice center
    	String[][]data=VitimageUtils.readStringTabFromCsv(splitDir+"/"+spec+"/circle.csv");
    	double[]sliceCenter=new double[] {Double.parseDouble(data[0][0]),Double.parseDouble(data[0][1])};
    	
    	//Get contour mask
		ImagePlus imgContour=IJ.openImage(splitDir+"/"+spec+"/concave_hull.tif");
		ImagePlus imgSeg=IJ.openImage(splitDir+"/"+spec+"/segmentation_no_out.tif");
    	
		//Take voronoi mask and Compute colormap of voronoi against distance
		ImagePlus voronoi=IJ.openImage(splitDir+"/"+spec+"/voronoi.tif");
		ImagePlus imgVorCells=IJ.openImage(splitDir+"/"+spec+"/voronoi.tif");
		ImagePlus imgVorInv=IJ.openImage(splitDir+"/"+spec+"/voronoi.tif");
		IJ.run(imgVorInv,"Invert","");
		IJ.run(imgVorCells,"Invert","");
		Roi[]rois=VitimageUtils.segmentationToRoi(imgVorInv);
		double[][]centroidsVor=new double[rois.length][];
		//imgVorInv.show();
		for(int n=0;n<rois.length;n++) {
			centroidsVor[n]=rois[n].getContourCentroid();
			double[]distances=getDistancesFromCenterToContour(new double[][] {centroidsVor[n]}, imgContour, sliceCenter);
			imgVorInv.setRoi(rois[n]);
			int value=(int)Math.round(distances[0]*255);
			imgVorInv.setColor(new Color(value,value,value));
			imgVorInv.getRoi().setFillColor(new Color(value,value,value));
		    IJ.run(imgVorInv,"Fill","");
			imgVorInv.resetRoi();
		}
		ImagePlus vorInsideDistanceColor=imgVorInv.duplicate();
		ImagePlus vorInsideDistanceColorOne=imgVorInv.duplicate();
		IJ.run(vorInsideDistanceColorOne,"8-bit","");
		IJ.run(vorInsideDistanceColorOne,"32-bit","");
		vorInsideDistanceColorOne=VitimageUtils.makeOperationOnOneImage(vorInsideDistanceColorOne, 3, 255, true);
		
		IJ.run(vorInsideDistanceColor,"Fire","");
		IJ.run(vorInsideDistanceColor,"RGB Color", "");
		
		//Get initial source data
		ImagePlus imgSourceRGB=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
		ImagePlus imgSourceGray=IJ.openImage(splitDir+"/"+spec+"/slice_sub_8.tif");
		IJ.run(imgSourceGray,"8-bit","");
		double[]circleCoords=VitimageUtils.stringTabToDoubleTab(				VitimageUtils.readStringTabFromCsv(						new File(splitDir+"/"+spec+"/circle.csv").getAbsolutePath())[0]);	
		ImagePlus circle=IJ.openImage(splitDir+"/"+spec+"/concave_hull.tif");

		//Generate mask of contour inside
		ImagePlus disk=circle.duplicate();
		IJ.run(disk,"Invert","");		IJ.run(disk,"Fill Holes","");		IJ.run(disk,"Invert","");
		ImagePlus voronoiIn=VitimageUtils.binaryOperationBetweenTwoImages(voronoi, disk, 2);
		ImagePlus contours=VitimageUtils.binaryOperationBetweenTwoImages(voronoiIn, circle, 1);
		ImagePlus maskVoronoi=VitimageUtils.getBinaryMaskUnary(contours, 0.5);
		
		
		int[]colorContourVess=new int[] {200,200,200};
		int contourVessSize=2;
		double alphaGrey=2.0;
		int offsetGrey=120;
		double dzetaGrey=1.4;
		double betaGrey=0.15;
		int[]colorContourVoronoi=new int[] {20,20,255};
		ImagePlus contourVess=imgSeg.duplicate();
		dilation(contourVess, contourVessSize, false);
		contourVess=VitimageUtils.binaryOperationBetweenTwoImages(contourVess, imgSeg, 4);
		contourVess=VitimageUtils.getBinaryMaskUnary(contourVess, 0.5);

		//We have segmentation_no_out, mask of cells
		//contours, mask of voronoi and circle
		//imgVorCells, mask of voronoi areas inside the circle
		//contourVess
		//vorInsideDistanceColor, imagecoloured with respect to distance inside influence areas
		
		//Build mask of only distance color
		imgSeg=VitimageUtils.getBinaryMaskUnary(imgSeg, 0.5);
		ImagePlus maskOnlyDistance=VitimageUtils.binaryOperationBetweenTwoImages(contours, contourVess	, 1);
		maskOnlyDistance=VitimageUtils.binaryOperationBetweenTwoImages(maskOnlyDistance, imgSeg	, 1);
		maskOnlyDistance=VitimageUtils.invertBinaryMask(maskOnlyDistance);		
		
		
		//Prepare data
		ImagePlus[]imgSourceVess=VitimageUtils.splitRGBStackHeadLess(imgSourceRGB);
		ImagePlus imgSourceRGBGrey=imgSourceRGB.duplicate();
		ImagePlus imgSourceRGBGrey2=imgSourceRGB.duplicate();
		IJ.run(imgSourceRGBGrey,"8-bit","");
		IJ.run(imgSourceRGBGrey,"32-bit","");
		ImagePlus imgSourceRGBGreyExt=imgSourceRGBGrey.duplicate();
		IJ.run(imgSourceRGBGreyExt,"RGB Color","");
		imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 3	, alphaGrey, true);
		imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 1	, offsetGrey, true);
		imgSourceRGBGrey=VitimageUtils.makeOperationOnOneImage(imgSourceRGBGrey, 2	, 1.0/255.0, true);
		vorInsideDistanceColorOne=VitimageUtils.makeOperationOnOneImage(vorInsideDistanceColorOne, 2, betaGrey, true);
		imgSourceRGBGrey=VitimageUtils.makeOperationBetweenTwoImages(imgSourceRGBGrey, vorInsideDistanceColorOne, 1, true);
		//imgSourceRGBGrey.show();
		//VitimageUtils.waitFor(500000);
		ImagePlus[]imgSourceDist=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
		ImagePlus[]imgContourVess=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
		ImagePlus[]imgContourVor=VitimageUtils.splitRGBStackHeadLess(vorInsideDistanceColor.duplicate());
		ImagePlus[]imgExtRGB=VitimageUtils.splitRGBStackHeadLess(extMode==1 ? imgSourceRGBGreyExt : imgSourceRGBGrey2);
		contourVess=VitimageUtils.getBinaryMaskUnaryFloat(contourVess, 0.5);
		contours=VitimageUtils.getBinaryMaskUnaryFloat(contours, 0.5);
		ImagePlus[]ret=new ImagePlus[3];
		disk=VitimageUtils.getBinaryMaskUnaryFloat(disk, 0.5);
		ImagePlus diskOut=VitimageUtils.invertBinaryMask(disk);
		
		
		for(int can=0;can<3;can++) {
			//Reset values of distance color where needed
			imgSourceDist[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], maskOnlyDistance, 2, true);
			//Multiply by grey values
			imgSourceDist[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], imgSourceRGBGrey, 2, true);
			imgSourceDist[can]=VitimageUtils.makeOperationOnOneImage(imgSourceDist[can], 2,dzetaGrey, true);
			IJ.run(imgSourceDist[can],"8-bit","");

			//Reset values of vessels color where needed
			imgSourceVess[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceVess[can],imgSeg, 2, true);
			imgSourceVess[can]=VitimageUtils.makeOperationOnOneImage(imgSourceVess[can],1,1.4, true);
			IJ.run(imgSourceVess[can],"8-bit","");

			//Set contourVess
			imgContourVess[can]=VitimageUtils.makeOperationOnOneImage(contourVess, 2, colorContourVess[can],true);
			IJ.run(imgContourVess[can],"8-bit","");

			//Set contour
			imgContourVor[can]=VitimageUtils.makeOperationOnOneImage(contours, 2, colorContourVoronoi[can],true);
			IJ.run(imgContourVor[can],"8-bit","");

			//Add everything
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(imgSourceDist[can], imgSourceVess[can], 1, false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgContourVess[can], 1, false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgContourVor[can], 1, false);
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can],disk,2, false);
			
			//Process outside
			imgExtRGB[can]=VitimageUtils.makeOperationBetweenTwoImages(imgExtRGB[can],diskOut,2, false);
			imgExtRGB[can]=VitimageUtils.makeOperationOnOneImage(imgExtRGB[can],2, 1.5,false);
			if(extMode==0) {
				imgExtRGB[can]=VitimageUtils.makeOperationOnOneImage(imgExtRGB[can],2,1000000000,false);
			}
			ret[can]=VitimageUtils.makeOperationBetweenTwoImages(ret[can], imgExtRGB[can], 1, false);
		}
	
		ImagePlus result=VitimageUtils.compositeRGBByte(ret[0], ret[1], ret[2],1,1,1);				
		result.setTitle(spec);
		//result.show();
		VitimageUtils.waitFor(20);
		imgVorInv.changes=false;
		imgVorInv.close();//2016_G1_P3_E19 jolie
		return result;
    }

    
	
	
	public static  ImagePlus erosion(ImagePlus img, int radius,boolean is3d) {
		Strel3D str2=inra.ijpb.morphology.strel.DiskStrel.fromDiameter(radius);
		Strel3D str3=inra.ijpb.morphology.strel.BallStrel.fromDiameter(radius);
		return new ImagePlus("",Morphology.erosion(img.getImageStack(),is3d ? str3 : str2));
	}
	
	public static ImagePlus dilation(ImagePlus img, int radius,boolean is3d) {
		Strel3D str2=inra.ijpb.morphology.strel.DiskStrel.fromDiameter(radius);
		Strel3D str3=inra.ijpb.morphology.strel.BallStrel.fromDiameter(radius);
		return new ImagePlus("",Morphology.dilation(img.getImageStack(),is3d ? str3 : str2));
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	
}
