package com.vitimage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.itk.simple.DiscreteGaussianImageFilter;
import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.OtsuThresholdImageFilter;
import org.itk.simple.PixelIDValueEnum;
import org.itk.simple.RecursiveGaussianImageFilter;
import org.itk.simple.ResampleImageFilter;
import org.itk.simple.SmoothingRecursiveGaussianImageFilter;
import org.itk.simple.VectorDouble;
import org.itk.simple.VectorUInt32;

import com.vitimage.TransformUtils.AngleComparator;
import com.vitimage.TransformUtils.VolumeComparator;
import com.vitimage.Vitimage4D.VineType;
import com.vitimage.VitimageUtils.Capillary;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.Calibration;
import ij.plugin.ChannelSplitter;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.GaussianBlur3D;
import ij.plugin.HyperStackReducer;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.Binary;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import imagescience.transform.Transform;
import math3d.Point3d;
import trainableSegmentation.WekaSegmentation;

public interface VitimageUtils {
	public static void main(String[]args) {
		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/Random/img.tif");
		double[]vect;
		vect=valuesOfBlockDouble(img,10,10.9999,10,12,12.9999,12);
		System.out.println("Values in case 1 : ");
		System.out.println(TransformUtils.stringVectorN(vect, "case 1 "));
		/*Point3d[] pInit=new Point3d[3];
		Point3d[] pFin=new Point3d[3];
		Acquisition mriT1=MRI_T1_Seq.getTestAcquisitionMriT1();
		mriT1.getTransformedRegistrationImage().show();

		System.out.println("\n\n\nTEST SEQUENCE 1 : DETECT AND CHANGE AXIS");
		pFin=VitimageUtils.(mriT1);
		System.out.println(pFin[0]);
		System.out.println(pFin[1]);
		System.out.println(pFin[2]);
		pInit[0]=mriT1.convertPointToRealSpace(  new Point3d(mriT1.dimX()/2.0  ,   mriT1.dimY()/2.0,    mriT1.dimZ()/2.0) ) ;//origine
		pInit[1]=mriT1.convertPointToRealSpace(  new Point3d(mriT1.dimX()/2.0  ,   mriT1.dimY()/2.0,    mriT1.dimZ()/2.0  ) );//origine (we'll add  dZ)
		pInit[2]=mriT1.convertPointToRealSpace(  new Point3d(mriT1.dimX()/2.0  ,   mriT1.dimY()/2.0,    mriT1.dimZ()/2.0) );//origine   (we'll add dX)
		pInit[1].z+=1;
		pInit[2].x+=1;
		System.out.println(pInit[0]);
		System.out.println(pInit[1]);
		System.out.println(pInit[2]);
		mriT1.addTransform(ItkTransform.estimateBestRigid3D(pFin, pInit));
		System.out.println("Transformation actualisée :"+mriT1.getTransform());
		mriT1.getTransformedRegistrationImage().show();

case 1  = [ 86.96999999999997 , 183.57999999999998 , 241.13 , 120.76999999999998 , 197.46999999999997 , 244.1 , 148.60999999999999 , 209.37 , 246.07999999999998 , 86.96999999999997 , 183.57999999999998 , 241.13 , 120.76999999999998 , 197.46999999999997 , 244.1 , 148.60999999999999 , 209.37 , 246.07999999999998 , 86.96999999999997 , 183.57999999999998 , 241.13 , 120.76999999999998 , 197.46999999999997 , 244.1 , 148.60999999999999 , 209.37 , 246.07999999999998
		System.out.println("\n\n\nTEST SEQUENCE 3 : DETECT INOCULATION POINT");
		pFin =VitimageUtils.detectInoculationPoint(mriT1);
		System.out.println(pFin[0]);
		System.out.println(pFin[1]);
		System.out.println(pFin[2]);
		pInit[0]=mriT1.convertPointToRealSpace(  new Point3d(mriT1.dimX()/2.0  ,   mriT1.dimY()/2.0,    mriT1.dimZ()/2.0) ) ;//origine
		pInit[1]=mriT1.convertPointToRealSpace(  new Point3d(mriT1.dimX()/2.0  ,   mriT1.dimY()/2.0,    mriT1.dimZ()/2.0  ) );//origine (we'll add  dZ)
		pInit[2]=mriT1.convertPointToRealSpace(  new Point3d(mriT1.dimX()/2.0  ,   mriT1.dimY()/2.0,    mriT1.dimZ()/2.0) );//origine   (we'll add dY)
		pInit[1].z+=1;
		pInit[2].y+=1;
		System.out.println(pInit[0]);
		System.out.println(pInit[1]);
		System.out.println(pInit[2]);
		System.out.println("Inoculation retrieved on initial image : "+mriT1.getInoculationPoint());
		pFin=new Point3d[] {pFin[0] , pFin[1] ,  pFin[2]};
		mriT1.addTransform(ItkTransform.estimateBestRigid3D(pFin, pInit));
		System.out.println("Transformation actualisée :"+mriT1.getTransform());
		mriT1.getTransformedRegistrationImage().show();
*/



	}

	public final static String slash=File.separator;
	public static final int ERROR_VALUE=-1;
	public static final int COORD_OF_MAX_IN_TWO_LAST_SLICES=1;
	public static final int COORD_OF_MAX_ALONG_Z=2;

	public static  AcquisitionType stringToAcquisitionType(String str){
		switch(str) {
		case "MRI_SE_SEQ": return AcquisitionType.MRI_SE_SEQ;
		case "MRI_T1_SEQ": return AcquisitionType.MRI_T1_SEQ;
		case "MRI_T2_SEQ": return AcquisitionType.MRI_T2_SEQ;
		case "MRI_GE3D": return AcquisitionType.MRI_GE3D;
		case "MRI_DIFF_SEQ": return AcquisitionType.MRI_DIFF_SEQ;
		case "MRI_FLIPFLOP_SEQ": return AcquisitionType.MRI_FLIPFLOP_SEQ;
		case "RX": return AcquisitionType.RX;
		case "HISTOLOGY": return AcquisitionType.HISTOLOGY;
		case "PHOTOGRAPH": return AcquisitionType.PHOTOGRAPH;
		case "TERAHERTZ": return AcquisitionType.TERAHERTZ;
		}
		return null;
	}
	
	public static VineType stringToVineType(String str){
		switch(str) {
		case "GRAFTED_VINE": return VineType.GRAFTED_VINE;
		
		case "VINE": return VineType.VINE;
		case "CUTTING": return VineType.CUTTING;
		}
		return null;
	}
	
	public enum ComputingType{
		COMPUTE_ALL,
		EVERYTHING_UNTIL_MAPS,
		EVERYTHING_AFTER_MAPS
	}
	
	public enum AcquisitionType{
		MRI_SE_SEQ,
		MRI_T1_SEQ,
		MRI_T2_SEQ,
		MRI_DIFF_SEQ,
		MRI_FLIPFLOP_SEQ,
		RX,
		HISTOLOGY,
		PHOTOGRAPH,
		TERAHERTZ,
		MRI_GE3D
	}

	public enum SupervisionLevel{
		AUTONOMOUS,
		GET_INFORMED,
		ASK_FOR_ALL
	}	

	public enum Capillary{
		HAS_CAPILLARY,
		HAS_NO_CAPILLARY
	}

	public static Date getDateFromString(String datStr) {
		Date date=null;
		try {
			date = new SimpleDateFormat("yyyyMMdd").parse(datStr);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			return new Date(0);
		}  
		return date;
	}

	public static String writableArray(double[]array) {
		String ret="";
		for(int i=0;i<array.length-1;i++)ret+=array[i]+" ";
		ret+=array[array.length-1];
		return ret;
	}

	public static String writableArray(int[]array) {
		String ret="";
		for(int i=0;i<array.length-1;i++)ret+=array[i]+" ";
		ret+=array[array.length-1];
		return ret;
	}

	
	
	public static ImagePlus thresholdFloatImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		IJ.run(ret,"8-bit","");
		float[][] in=new float[img.getStackSize()][];
		byte[][] out=new byte[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(float []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					out[z][y*X+x]=(  in[z][y*X+x] >= thresholdMin ? (        in[z][y*X+x] < thresholdMax  ?   ((byte) (255 & 0xff)) : ((byte) (0 & 0xff)) ) : ((byte) (0 & 0xff)) );
				}			 
			}
		}
		return ret;

	}
	
	
	
	public static ImagePlus thresholdShortImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		IJ.run(ret,"8-bit","");
		short[][] in=new short[img.getStackSize()][];
		byte[][] out=new byte[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(short []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[z][y*X+x] & 0xffff);
					out[z][y*X+x]=(  val >= thresholdMin ? (        val < thresholdMax  ?   ((byte) (255 & 0xff)) : ((byte) (0 & 0xff)) ) : ((byte) (0 & 0xff)) );
				}			 
			}
		}
		return ret;

	}
	
	

	public static ImagePlus thresholdByteImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		byte[][] in=new byte[img.getStackSize()][];
		byte[][] out=new byte[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(byte []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[z][y*X+x] & 0xff);
					out[z][y*X+x]=(  val >= thresholdMin ? (        val < thresholdMax  ?   ((byte) (255 & 0xff)) : ((byte) (0 & 0xff)) ) : ((byte) (0 & 0xff)) );
				}			 
			}
		}
		return ret;

	}
	
	
	
	/**
	 * Connected components utilities
	 * @param img
	 * @param computeZaxisOnly
	 * @param cornersCoordinates
	 * @param ignoreUnattemptedDimensions
	 * @return
	 */
	public static ImagePlus connexe(ImagePlus img,double threshLow,double threshHigh,double volumeLow,double volumeHigh,int connexity,int selectByVolume,boolean noVerbose) {
		boolean debug=!noVerbose;
		if(debug)System.out.println("Depart connexe");
		int yMax=img.getHeight();
		int xMax=img.getWidth();
		int zMax=img.getStack().getSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double voxVolume=vX*vY*vZ;
		int[][]connexions;
		int[]volume;
		if(debug)System.out.println("Allocations 1, en MegaInt : "+(0.000001*xMax*yMax*zMax));
		int[][][]tabIn=new int[xMax][yMax][zMax];
		if(0.000001*xMax*yMax*zMax>20) {
			if(debug)System.out.println("Allocations 2, en MegaInt : "+(0.000001*xMax*yMax*zMax/20));
			connexions=new int[xMax*yMax*zMax/20][2];
			if(debug)System.out.println("Allocations 3, en MegaInt : "+(0.000001*xMax*yMax*zMax/20));
			volume=new int[xMax*yMax*zMax/20];
		}
		else {
			if(debug)System.out.println("Allocations 2, en MegaInt : "+(0.000001*xMax*yMax*zMax/5));
			connexions=new int[xMax*yMax*zMax/5][2];
			if(debug)System.out.println("Allocations 3, en MegaInt : "+(0.000001*xMax*yMax*zMax/5));
			volume=new int[xMax*yMax*zMax/5];
		}
		int[][]neighbours=new int[][]{{1,0,0,0},{1,1,0,0},{0,1,0,0},{0,0,1,0},{1,0,1,0},{1,1,1,0},{0,1,1,0} };
		int curComp=0;
		int indexConnexions=0;
		if(debug)System.out.println("Choix d'un type");
		switch(img.getType()) {
		case ImagePlus.GRAY8:
			byte[] imgInB;
			for(int z=0;z<zMax;z++) {
				imgInB=(byte[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInB[x+xMax*y] & 0xff) < threshHigh )  && ((float)((imgInB[x+xMax*y]) & 0xff) >= threshLow) )tabIn[x][y][z]=-1;
			}
			break;
		case ImagePlus.GRAY16:
			short[] imgInS;
			for(int z=0;z<zMax;z++) {
				imgInS=(short[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInS[x+xMax*y] & 0xffff) < threshHigh )  && ((float)((imgInS[x+xMax*y]) & 0xffff) >=threshLow) )tabIn[x][y][z]=-1;					
			}
			break;
		case ImagePlus.GRAY32:
			float[] imgInF;
			for(int z=0;z<zMax;z++) {
				imgInF=(float[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((imgInF[x+xMax*y]) < threshHigh )  && (((imgInF[x+xMax*y])) >= threshLow) )tabIn[x][y][z]=-1;					
			}
			break;
		}

		if(debug)System.out.println("Boucle principale");
		//Boucle principale
		for(int x=0;x<xMax;x++) {
			for(int y=0;y<yMax;y++) {
				for(int z=0;z<zMax;z++) {
					if(tabIn[x][y][z]==0)continue;//Point du fond
					if(tabIn[x][y][z]==-1) {
						tabIn[x][y][z]=(++curComp);//New object
						if(curComp==volume.length) {
							//agrandir le tableau de volumes de 20 %
							int[]volumeBigger=new int[(12*volume.length)/10];
							if(debug)System.out.println("Volumes array touch to limit. Raising size to "+(12*volume.length)/10);
							for(int i=0;i<curComp;i++) {
								volumeBigger[i]=volume[i];
								volumeBigger[i]=volume[i];
							}
							volume=volumeBigger;
						}

						volume[curComp]++;
					}
					if(tabIn[x][y][z]>0) {//Here we need to explore the neighbours
						for(int nei=0;nei<7;nei++)neighbours[nei][3]=1;//At the beginning, every neighbour is possible. 
						//    Z axis
						//     /|\ 6---------5         
						//      | /|        /|
						//      |/ |       / |  
						//      3---------4  |
						//      |  |      |  |
						//      |  2------|--1
						//      | /       | /
						//      |/        |/
						//      X---------0-----> X axis
 						//
						//Then we need to reduce access according to images dims and chosen connexity
						if(x==xMax-1)neighbours[0][3]=neighbours[1][3]=neighbours[4][3]=neighbours[5][3]=0;
						if(y==yMax-1)neighbours[1][3]=neighbours[2][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(z==zMax-1)neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==4)neighbours[1][3]=neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==6)neighbours[1][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==8)neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==18)neighbours[5][3]=0;

						//Given these neighbours, we can visit them
						for(int nei=0;nei<7;nei++) {
							if(neighbours[nei][3]==1) {
								//System.out.println("Go, avec nei="+nei+" x="+x+" y="+y+" z"+z+" NEIS={"+neighbours[nei][0]+","+neighbours[nei][1]+","+neighbours[nei][2]"}");
								if(tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]==0)continue;
								if(tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]==-1) {
									tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]=tabIn[x][y][z];
									volume[tabIn[x][y][z]]++;
								}
								else {
									if(indexConnexions==connexions.length) {
										//agrandir le tableau de connexions de 20 %
										int[][]connexionsBigger=new int[(12*connexions.length)/10][2];
										if(debug)System.out.println("Connexions array touch to limit. Raising size to "+(12*connexions.length)/10);
										for(int i=0;i<indexConnexions;i++) {
											connexionsBigger[i][0]=connexions[i][0];
											connexionsBigger[i][1]=connexions[i][1];
										}
										connexions=connexionsBigger;
									}
									connexions[indexConnexions][0]=tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]];
									connexions[indexConnexions++][1]=tabIn[x][y][z];
								}
							}
						}
					}
				}	
			}			
		}

		//System.out.println("Resolution des conflits entre groupes connexes");
		//Resolution des groupes d'objets connectes entre eux (formes en U, et cas plus compliqués)
		int[]lut = resolveConnexitiesGroupsAndExclude(connexions,indexConnexions,curComp+1,volume,volumeLow/voxVolume,volumeHigh/voxVolume,selectByVolume,noVerbose);


		//Build computed image of objects
		ImagePlus imgOut=ij.gui.NewImage.createImage(img.getShortTitle()+"_"+connexity+"CON",xMax,yMax,zMax,16,ij.gui.NewImage.FILL_BLACK);
		short[] imgOutTab;
		for(int z=0;z<zMax;z++) {
			imgOutTab=(short[])(imgOut.getStack().getProcessor(z+1).getPixels());
			for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++) imgOutTab[x+xMax*y] = (short) ( lut[tabIn[x][y][z]]  );
		}
		imgOut.getProcessor().setMinAndMax(0,lut[curComp+1]);
		VitimageUtils.adjustImageCalibration(imgOut, img);
		return imgOut;	
	}

	public static int[] resolveConnexitiesGroupsAndExclude(int  [][] connexions,int nbCouples,int n,int []volume,double volumeLowP,double volumeHighP,int selectByVolume,boolean noVerbose) {
		int[]prec=new int[n];
		int[]lut=new int[n+1];
		int[]next=new int[n];
		int[]label=new int[n];
		for(int i=0;i<n;i++) {label[i]=i;prec[i]=0;next[i]=0;}

		int indA,indB,valMin,valMax,indMin,indMax;
		for(int couple=0;couple<nbCouples;couple++) {
			indA=connexions[couple][0];
			indB=connexions[couple][1];
			if(label[indA]==label[indB])continue;
			if(label[indA]<label[indB]) {
				valMin=label[indA];
				indMin=indA;
				valMax=label[indB];
				indMax=indB;
			}
			else {
				valMin=label[indB];
				indMin=indB;
				valMax=label[indA];
				indMax=indA;
			}
			while(next[indMin]>0)indMin=next[indMin];
			while(prec[indMax]>0)indMax=prec[indMax];
			prec[indMax]=indMin;
			next[indMin]=indMax;
			while(next[indMin]>0) {
				indMin=next[indMin];
				label[indMin]=valMin;
			}
		}
		//Compute number of objects and volume
		for (int i=1;i<n ;i++){
			if(label[i]!=i) {
				volume[label[i]]+=volume[i];
				volume[i]=0;
			}
		}
		//copy and sort volumes
		Object [][]tabSort=new Object[n][2];
		int selectedIndex=0;
		for (int i=0;i<n ;i++) {
			tabSort[i][0]=new Double(volume[i]);
			tabSort[i][1]=new Integer(i);
		}


		Arrays.sort(tabSort,new VolumeComparator());
		if(selectByVolume>n)selectByVolume=n;
		if(selectByVolume<1)selectByVolume=0;
		if(selectByVolume!=0)selectedIndex=((Integer)(tabSort[n-selectByVolume][1])).intValue();

		//Exclude too big or too small objects,
		int displayedValue=1;
		for (int i=1;i<n ;i++){
			if(selectByVolume!=0) {
				if(i==selectedIndex)lut[i]=255;
				else lut[i]=0;
			}
			else if( (volume[i]>0) && (volume[i]>=volumeLowP) && (volume[i]<=volumeHighP) ) {
				lut[i]=displayedValue++;
			}
		}
		if(displayedValue>65000) {System.out.println("Warning : connexe , "+(displayedValue-1)+" connected components");}
		else if(! noVerbose)System.out.println("Number of connected components detected : "+(selectByVolume>0 ? 1 : (displayedValue-1)));

		//Group labels
		for (int i=0;i<n ;i++){
			lut[i]=lut[label[i]];
		}
		//Tricky little parameters to provide a good display after operation;
		if(selectByVolume !=0)lut[n]=255;
		else lut[n]=displayedValue;
		return lut;
	}


	/**
	 * Main automated detectors : axis detection and inoculation point detection. Usable for both MRI T1, T2, and X ray images
	 * @param img1
	 * @param acqType
	 * @return
	 */
	public static Point3d[] detectAxis(ImagePlus img1,AcquisitionType acqType){
		ImagePlus img=new Duplicator().run(img1);
		boolean debug=false;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=1,yMoyDown=1,zMoyDown=1;
		int hitsUp=0,hitsDown=0;

		//Step 1 : apply gaussian filtering and convert to 8 bits
		if(acqType != AcquisitionType.RX)img=VitimageUtils.gaussianFiltering(img, 18*0.035 , 18*0.035 , 3*0.5);
		img.getProcessor().setMinAndMax(
				acqType==AcquisitionType.MRI_T1_SEQ ? 200 : 10000, 
				acqType==AcquisitionType.MRI_T1_SEQ ? 3000 : 50000);
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		if(debug)imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];

		if(acqType == AcquisitionType.RX)img=VitimageUtils.eraseBorder(img);
		if(debug)imageChecking(img,"after Erase ");
		if(acqType != AcquisitionType.MRI_T2_SEQ) {
			
			System.out.println("Mask lookup for center of object, case of hign SNR (T1 or RX)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setAutoThreshold("Otsu dark");
				maskTab[z]=maskTab[z].createMask();
			}
		}
		else {
			System.out.println("Mask lookup for center of object, case of low SNR (T2)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setThreshold(20,255,1);
				maskTab[z]=maskTab[z].createMask();
			}
			
		}
			
			
		//Step 2.1 : Extract two substacks for the upper part and the lower part of the object
		ImageStack stackUp = new ImageStack(xMax, yMax);	
		ImageStack stackDown = new ImageStack(xMax, yMax);
		int zQuarter=zMax/4;
		int zVentile=zMax/40;
		zVentile=(zVentile < 10 ? 10 : zVentile);
		if(zMax<zVentile*2+2)zVentile=zMax/2-1;
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",maskTab[zMax/2+zQuarter-zVentile+i] );//de zmax/2 +zQuarter-zVentile à zMax/2 + zQuarter-zVentile5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",maskTab[zMax/2-zQuarter+i+1] );//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		VitimageUtils.adjustImageCalibration(imgUp, img);
		if(debug)imageChecking(imgUp);				
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,false);
		if(debug)imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		VitimageUtils.adjustImageCalibration(imgDown, img);
		if(debug)imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,false);
		if(debug)imageChecking(imgDownCon,"imgDownCon");
		IJ.saveAsTiff(imgUpCon,"/home/fernandr/Bureau/pouet.tif");
		
		if(VitimageUtils.isNullImage(imgUpCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgUp,"Up init");
			imgUpCon=VitimageUtils.gaussianFiltering(imgUp, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgUpCon,"Apres filtrage");
			//imgUpCon.show();
			//VitimageUtils.waitFor(10000);
			imgUpCon=VitimageUtils.getBinaryMask(imgUpCon, 253);
			if(debug)imageChecking(imgUpCon,"Apres seuillage");
			imgUpCon=connexe(imgUpCon,30,256,0,10E10,6,1,false);
			if(debug)imageChecking(imgUpCon,"Apres connexe");
		}

		if(VitimageUtils.isNullImage(imgDownCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgDown,"Down init");
			imgDownCon=VitimageUtils.gaussianFiltering(imgDown, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgDownCon,"Apres filtrage");
			imgDownCon=VitimageUtils.getBinaryMask(imgDownCon, 254);
			if(debug)imageChecking(imgDownCon,"Apres seuillage");
			imgDownCon=connexe(imgDownCon,30,256,0,10E10,6,1,false);
			if(debug)imageChecking(imgDownCon,"Apres connexe");
		}
		
		System.out.println("There");
		
		//Step 3 : compute the two centers of mass
		short[][]valsDownCon=new short[zQuarter][];
		short[][]valsUpCon=new short[zQuarter][];
		for(int z=0;z<zVentile;z++){
			valsDownCon[z]=(short[])(imgDownCon).getStack().getProcessor(z+1).getPixels();
			valsUpCon[z]=(short[])(imgUpCon).getStack().getProcessor(z+1).getPixels();
		}

		for(int x=0;x<xMax;x++){
			for(int y=0;y<yMax;y++){
				for(int z=0;z<zVentile;z++){								
					if(valsDownCon[z][xMax*y+x]==((short)255)){//We are in the first part of the object
						hitsDown++;
						xMoyDown+=x;yMoyDown+=y;zMoyDown+=z;
					}
					if(valsUpCon[z][xMax*y+x]==((short	)255)){//We are in the first part of the object
						hitsUp++;
						xMoyUp+=x;yMoyUp+=y;zMoyUp+=z;
					}
				}
			}
		}
		System.out.println("Here");

		if(hitsUp==0)hitsUp=1;
		if(hitsDown==0)hitsDown=1;
		xMoyUp=xMoyUp/hitsUp;//Center of mass computation. 
		yMoyUp=yMoyUp/hitsUp;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyUp=zMoyUp/hitsUp+zMax/2+zQuarter-zVentile;//due to the extraction of a substack zmax/2-zQuarter+1 - zmax/2     zMax/2+zQuarter-zVentile

		xMoyDown=xMoyDown/hitsDown;//Center of mass computation. 
		yMoyDown=yMoyDown/hitsDown;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyDown=zMoyDown/hitsDown+zMax/2-zQuarter+1;//due to the extraction of a substack zmax/2 - zmax/2+zQuarter       zMax/2-zQuarter+1
		debug =true;
		if(debug) {
			System.out.println("HitsUp="+hitsUp+" ..Center of mass up = "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("HitsDown="+hitsDown+" ..Center of mass down = "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}
		System.out.println("Here");

		xMoyUp=xMoyUp*vX;		
		yMoyUp=yMoyUp*vY;		
		zMoyUp=zMoyUp*vZ;	
		xMoyDown=xMoyDown*vX;		
		yMoyDown=yMoyDown*vY;		
		zMoyDown=zMoyDown*vZ;		
		if(debug) {
			System.out.println("Center of mass up (coord reel)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (coord reel)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}
		System.out.println("THere");

		//Step 4 : compute the axis vector, that will stands for Z vector after alignement
		double[]vectZ=TransformUtils.normalize(new double[] {xMoyUp - xMoyDown , yMoyUp - yMoyDown , zMoyUp - zMoyDown});
		double[][]axisVerificationMatrix=VitiDialogs.inspectAxis( img1 ,vectZ,new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 ),0);
		double []vectZbis=axisVerificationMatrix[0];
		double epsilon=0.0000001;
		if(TransformUtils.norm(TransformUtils.vectorialSubstraction(vectZbis,vectZ))>epsilon) {//Une erreur a été corrigée par l'utilisateur
			System.out.println("En effet il y a eu modification");
			xMoyUp=axisVerificationMatrix[1][0];xMoyDown=axisVerificationMatrix[2][0];
			yMoyUp=axisVerificationMatrix[1][1];yMoyDown=axisVerificationMatrix[2][1];
			zMoyUp=axisVerificationMatrix[1][2];zMoyDown=axisVerificationMatrix[2][2];
			vectZ=vectZbis;
		}

		
		System.out.println("Vecteur axial ="+TransformUtils.stringVector(vectZ,""));
		double []vectXtmp=new double[] {1,0,0};
		double []vectX=TransformUtils.normalize(TransformUtils.vectorialSubstraction(vectXtmp, TransformUtils.proj_u_of_v(vectZ, vectXtmp)));
		System.out.println("Vecteur orthogonal ="+TransformUtils.stringVector(vectX,""));

		
		
		

		//Step 5 : compute the three points
		Point3d origine=new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 );
		Point3d ptUp= new Point3d(origine.x + vectZ[0]   ,  origine.y + vectZ[1] , origine.z + vectZ[2]);
		Point3d ptRight= new Point3d(origine.x + vectX[0]   ,  origine.y + vectX[1] , origine.z + vectX[2]);
		return new Point3d[] {origine,ptUp,ptRight};
	}

	public static int max(int[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.max, tab is null. Return 0");
			return 0;
		}
		int max=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i]>max)max=tab[i];
		}
		return max;
	}

	public static int min(int[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.min, tab is null. Return 0");
			return 0;
		}
		int min=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i]<min)min=tab[i];
		}
		return min;
	}
	
	
	
	
	public static Point3d[] detectAxisIrmT1(ImagePlus img,int delayForReacting){
		boolean debug=false;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=1,yMoyDown=1,zMoyDown=1;
		int hitsUp=0,hitsDown=0;

		//Step 1 : apply gaussian filtering and convert to 8 bits
		img=VitimageUtils.gaussianFiltering(img, 18*0.035 , 18*0.035 , 3*0.5);
		img.getProcessor().setMinAndMax(0,1);
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		if(debug)imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];

		System.out.println("Mask lookup for center of object, case of hign SNR (T1 or RX)");
		for(int z=0;z<zMax;z++){
			maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
			maskTab[z].setAutoThreshold("Otsu dark");
			maskTab[z]=maskTab[z].createMask();
		}
			
			
		//Step 2.1 : Extract two substacks for the upper part and the lower part of the object
		ImageStack stackUp = new ImageStack(xMax, yMax);	
		ImageStack stackDown = new ImageStack(xMax, yMax);
		int zQuarter=zMax/4;
		int zVentile=zMax/40;
		zVentile=(zVentile < 10 ? 10 : zVentile);
		if(zMax<zVentile*2+2)zVentile=zMax/2-1;
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",maskTab[zMax/2+zQuarter-zVentile+i] );//de zmax/2 +zQuarter-zVentile à zMax/2 + zQuarter-zVentile5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",maskTab[zMax/2-zQuarter+i+1] );//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		VitimageUtils.adjustImageCalibration(imgUp, img);
		if(debug)imageChecking(imgUp);				
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		VitimageUtils.adjustImageCalibration(imgDown, img);
		if(debug)imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgDownCon,"imgDownCon");
		IJ.saveAsTiff(imgUpCon,"/home/fernandr/Bureau/pouet.tif");
		
		if(VitimageUtils.isNullImage(imgUpCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgUp,"Up init");
			imgUpCon=VitimageUtils.gaussianFiltering(imgUp, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgUpCon,"Apres filtrage");
			//imgUpCon.show();
			//VitimageUtils.waitFor(10000);
			imgUpCon=VitimageUtils.getBinaryMask(imgUpCon, 253);
			if(debug)imageChecking(imgUpCon,"Apres seuillage");
			imgUpCon=connexe(imgUpCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgUpCon,"Apres connexe");
		}

		if(VitimageUtils.isNullImage(imgDownCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgDown,"Down init");
			imgDownCon=VitimageUtils.gaussianFiltering(imgDown, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgDownCon,"Apres filtrage");
			imgDownCon=VitimageUtils.getBinaryMask(imgDownCon, 254);
			if(debug)imageChecking(imgDownCon,"Apres seuillage");
			imgDownCon=connexe(imgDownCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgDownCon,"Apres connexe");
		}
		
		
		
		//Step 3 : compute the two centers of mass
		short[][]valsDownCon=new short[zQuarter][];
		short[][]valsUpCon=new short[zQuarter][];
		for(int z=0;z<zVentile;z++){
			valsDownCon[z]=(short[])(imgDownCon).getStack().getProcessor(z+1).getPixels();
			valsUpCon[z]=(short[])(imgUpCon).getStack().getProcessor(z+1).getPixels();
		}

		for(int x=0;x<xMax;x++){
			for(int y=0;y<yMax;y++){
				for(int z=0;z<zVentile;z++){								
					if(valsDownCon[z][xMax*y+x]==((short)255)){//We are in the first part of the object
						hitsDown++;
						xMoyDown+=x;yMoyDown+=y;zMoyDown+=z;
					}
					if(valsUpCon[z][xMax*y+x]==((short	)255)){//We are in the first part of the object
						hitsUp++;
						xMoyUp+=x;yMoyUp+=y;zMoyUp+=z;
					}
				}
			}
		}
		System.out.println("hERE");
		if(hitsUp==0)hitsUp=1;
		if(hitsDown==0)hitsDown=1;
		xMoyUp=xMoyUp/hitsUp;//Center of mass computation. 
		yMoyUp=yMoyUp/hitsUp;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyUp=zMoyUp/hitsUp+zMax/2+zQuarter-zVentile;//due to the extraction of a substack zmax/2-zQuarter+1 - zmax/2     zMax/2+zQuarter-zVentile

		xMoyDown=xMoyDown/hitsDown;//Center of mass computation. 
		yMoyDown=yMoyDown/hitsDown;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyDown=zMoyDown/hitsDown+zMax/2-zQuarter+1;//due to the extraction of a substack zmax/2 - zmax/2+zQuarter       zMax/2-zQuarter+1

		if(debug) {
			System.out.println("HitsUp="+hitsUp+" ..Center of mass up = "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("HitsDown="+hitsDown+" ..Center of mass down = "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		xMoyUp=xMoyUp*vX;		
		yMoyUp=yMoyUp*vY;		
		zMoyUp=zMoyUp*vZ;	
		xMoyDown=xMoyDown*vX;		
		yMoyDown=yMoyDown*vY;		
		zMoyDown=zMoyDown*vZ;		
		if(debug) {
			System.out.println("Center of mass up (coord reel)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (coord reel)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		//Step 4 : compute the axis vector, that will stands for Z vector after alignement
		double[]vectZ=TransformUtils.normalize(new double[] {xMoyUp - xMoyDown , yMoyUp - yMoyDown , zMoyUp - zMoyDown});
		double[][]axisVerificationMatrix=VitiDialogs.inspectAxis( img ,vectZ,new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 ),0);
		double []vectZbis=axisVerificationMatrix[0];
		double epsilon=0.0000001;
		if(TransformUtils.norm(TransformUtils.vectorialSubstraction(vectZbis,vectZ))>epsilon) {//Une erreur a été corrigée par l'utilisateur
			System.out.println("En effet il y a eu modification");
			xMoyUp=axisVerificationMatrix[1][0];xMoyDown=axisVerificationMatrix[2][0];
			yMoyUp=axisVerificationMatrix[1][1];yMoyDown=axisVerificationMatrix[2][1];
			zMoyUp=axisVerificationMatrix[1][2];zMoyDown=axisVerificationMatrix[2][2];
			vectZ=vectZbis;
		}

		
		System.out.println("Vecteur axial ="+TransformUtils.stringVector(vectZ,""));
		double []vectXtmp=new double[] {1,0,0};
		double []vectX=TransformUtils.normalize(TransformUtils.vectorialSubstraction(vectXtmp, TransformUtils.proj_u_of_v(vectZ, vectXtmp)));
		System.out.println("Vecteur orthogonal ="+TransformUtils.stringVector(vectX,""));

		
		
		

		//Step 5 : compute the three points
		Point3d origine=new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 );
		Point3d ptUp= new Point3d(origine.x + vectZ[0]   ,  origine.y + vectZ[1] , origine.z + vectZ[2]);
		Point3d ptRight= new Point3d(origine.x + vectX[0]   ,  origine.y + vectX[1] , origine.z + vectX[2]);
		return new Point3d[] {origine,ptUp,ptRight};
	}

	
	public static ImagePlus cropImageShort(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY16)return null;
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,16,ij.gui.NewImage.FILL_WHITE);		
		VitimageUtils.adjustImageCalibration(out, img);
		int xMax=img.getWidth();
		for(int z=z0;z<z0+dimZ;z++) {
			short[] valsImg=(short[])img.getStack().getProcessor(z+1).getPixels();
			short[] valsOut=(short[])out.getStack().getProcessor(z-z0+1).getPixels();
			for(int x=x0;x<x0+dimX;x++) {
				for(int y=y0;y<y0+dimY;y++){
					valsOut[dimX*(y-y0)+(x-x0)]=((short)(valsImg[xMax*y+x] & 0xffff));
				}			
			}
		}
		return out;
	}
	
	

	public static ImagePlus uncropImageShort(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY16)return null;
		int oldDimX=img.getWidth();
		int oldDimY=img.getHeight();
		int oldDimZ=img.getStackSize();
		
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,16,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		for(int z=z0;z<z0+oldDimZ;z++) {
			short[] valsImg=(short[])img.getStack().getProcessor(z-z0+1).getPixels();
			short[] valsOut=(short[])out.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<x0+oldDimX;x++) {
				for(int y=y0;y<y0+oldDimY;y++){
					valsOut[dimX*(y)+(x)]=((short)(valsImg[oldDimX*(y-y0)+(x-x0)] & 0xffff));
				}			
			}
		}
		return out;
	}

	
	public static ImagePlus getSliceUncropped(ImagePlus img,int slice,int offsetX,int newDimX) {
		int oldDimX=img.getWidth();
		int dimY=img.getHeight();
		ImagePlus out=ij.gui.NewImage.createImage("out",newDimX,dimY,1,8,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		byte[] valsImg=(byte[])img.getStack().getProcessor(slice).getPixels();
		byte[] valsOut=(byte[])out.getStack().getProcessor(1).getPixels();
		for(int x=offsetX;x<offsetX+oldDimX && x<newDimX;x++) {
			for(int y=0;y<dimY ;y++){
				valsOut[newDimX*(y)+(x)]=((byte)(valsImg[oldDimX*(y)+(x-offsetX)] & 0xff));
			}			
		}
		
		return out;
	}

	public static ImagePlus uncropImageByte(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY8)return null;
		int oldDimX=img.getWidth();
		int oldDimY=img.getHeight();
		int oldDimZ=img.getStackSize();
		
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,8,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		for(int z=z0;z<z0+oldDimZ && z<z0+dimZ;z++) {
			byte[] valsImg=(byte[])img.getStack().getProcessor(z-z0+1).getPixels();
			byte[] valsOut=(byte[])out.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<x0+oldDimX && x<x0+dimX;x++) {
				for(int y=y0;y<y0+oldDimY && y<y0+dimY;y++){
					valsOut[dimX*(y)+(x)]=((byte)(valsImg[oldDimX*(y-y0)+(x-x0)] & 0xff));
				}			
			}
		}
		return out;
	}
	

	
	public static ImagePlus cropImageByte(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY8)return null;
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,8,ij.gui.NewImage.FILL_WHITE);		
		VitimageUtils.adjustImageCalibration(out, img);
		int xMax=img.getWidth();
		for(int z=z0;z<z0+dimZ;z++) {
			byte[] valsImg=(byte[])img.getStack().getProcessor(z+1).getPixels();
			byte[] valsOut=(byte[])out.getStack().getProcessor(z-z0+1).getPixels();
			for(int x=x0;x<x0+dimX;x++) {
				for(int y=y0;y<y0+dimY;y++){
					valsOut[dimX*(y-y0)+(x-x0)]=((byte)(valsImg[xMax*y+x] & 0xff));
				}			
			}
		}
		return out;
	}
	

	
	public static ImagePlus cropImageFloat(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY32)return null;
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,32,ij.gui.NewImage.FILL_WHITE);		
		VitimageUtils.adjustImageCalibration(out, img);
		int xMax=img.getWidth();
		for(int z=z0;z<z0+dimZ;z++) {
			float[] valsImg=(float[])img.getStack().getProcessor(z+1).getPixels();
			float[] valsOut=(float[])out.getStack().getProcessor(z-z0+1).getPixels();
			for(int x=x0;x<x0+dimX;x++) {
				for(int y=y0;y<y0+dimY;y++){
					valsOut[dimX*(y-y0)+(x-x0)]=(float)valsImg[xMax*y+x];
				}			
			}
		}
		return out;
	}
	
	
	public static Point3d[] detectAxis(Acquisition acq,int delayForReacting){
		ImagePlus img=null;
		img=acq.getImageForRegistration();
		AcquisitionType acqType=acq.acquisitionType;
		boolean debug=false;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=1,yMoyDown=1,zMoyDown=1;
		int hitsUp=0,hitsDown=0;
		if(debug)imageChecking(img,"Start detect axis type "+acq.getAcquisitionType());

		//Step 1 : apply gaussian filtering and convert to 8 bits
		if(acqType != AcquisitionType.RX)img=VitimageUtils.gaussianFiltering(img, 18*0.035 , 18*0.035 , 3*0.5);
		img.getProcessor().setMinAndMax(
				( acqType==AcquisitionType.MRI_T1_SEQ ? 200 : (acqType==AcquisitionType.MRI_GE3D ? 700 : 10000)), 
				( acqType==AcquisitionType.MRI_T1_SEQ ? 3000 : (acqType==AcquisitionType.MRI_GE3D ? 10000 : 50000)) );
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		if(debug)imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];

		if(acqType == AcquisitionType.RX)img=VitimageUtils.eraseBorder(img);
		if(debug)imageChecking(img,"after Erase ");
		if(acqType != AcquisitionType.MRI_T2_SEQ) {
			
			System.out.println("Mask lookup for center of object, case of hign SNR (T1 or RX)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setAutoThreshold("Otsu dark");
				maskTab[z]=maskTab[z].createMask();
			}
		}
		else {
			System.out.println("Mask lookup for center of object, case of low SNR (T2)");
			for(int z=0;z<zMax;z++){
				maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
				maskTab[z].setThreshold(20,255,1);
				maskTab[z]=maskTab[z].createMask();
			}
			
		}
			
			
		//Step 2.1 : Extract two substacks for the upper part and the lower part of the object
		ImageStack stackUp = new ImageStack(xMax, yMax);	
		ImageStack stackDown = new ImageStack(xMax, yMax);
		int zQuarter=zMax/4;
		int zVentile=zMax/40;
		zVentile=(zVentile < 10 ? 10 : zVentile);
		if(zMax<zVentile*2+2)zVentile=zMax/2-1;
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",maskTab[zMax/2+zQuarter-zVentile+i] );//de zmax/2 +zQuarter-zVentile à zMax/2 + zQuarter-zVentile5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",maskTab[zMax/2-zQuarter+i+1] );//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		VitimageUtils.adjustImageCalibration(imgUp, img);
		if(debug)imageChecking(imgUp);				
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		VitimageUtils.adjustImageCalibration(imgDown, img);
		if(debug)imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,true);
		if(debug)imageChecking(imgDownCon,"imgDownCon");
		IJ.saveAsTiff(imgUpCon,"/home/fernandr/Bureau/pouet.tif");
		
		if(VitimageUtils.isNullImage(imgUpCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgUp,"Up init");
			imgUpCon=VitimageUtils.gaussianFiltering(imgUp, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgUpCon,"Apres filtrage");
			//imgUpCon.show();
			//VitimageUtils.waitFor(10000);
			imgUpCon=VitimageUtils.getBinaryMask(imgUpCon, 253);
			if(debug)imageChecking(imgUpCon,"Apres seuillage");
			imgUpCon=connexe(imgUpCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgUpCon,"Apres connexe");
		}

		if(VitimageUtils.isNullImage(imgDownCon)) {
			System.out.println("Handling case of void moelle");
			if(debug)imageChecking(imgDown,"Down init");
			imgDownCon=VitimageUtils.gaussianFiltering(imgDown, 30*vX, 30*vY, 3*vZ);
			if(debug)imageChecking(imgDownCon,"Apres filtrage");
			imgDownCon=VitimageUtils.getBinaryMask(imgDownCon, 254);
			if(debug)imageChecking(imgDownCon,"Apres seuillage");
			imgDownCon=connexe(imgDownCon,30,256,0,10E10,6,1,true);
			if(debug)imageChecking(imgDownCon,"Apres connexe");
		}
		
		
		
		//Step 3 : compute the two centers of mass
		short[][]valsDownCon=new short[zQuarter][];
		short[][]valsUpCon=new short[zQuarter][];
		for(int z=0;z<zVentile;z++){
			valsDownCon[z]=(short[])(imgDownCon).getStack().getProcessor(z+1).getPixels();
			valsUpCon[z]=(short[])(imgUpCon).getStack().getProcessor(z+1).getPixels();
		}

		for(int x=0;x<xMax;x++){
			for(int y=0;y<yMax;y++){
				for(int z=0;z<zVentile;z++){								
					if(valsDownCon[z][xMax*y+x]==((short)255)){//We are in the first part of the object
						hitsDown++;
						xMoyDown+=x;yMoyDown+=y;zMoyDown+=z;
					}
					if(valsUpCon[z][xMax*y+x]==((short	)255)){//We are in the first part of the object
						hitsUp++;
						xMoyUp+=x;yMoyUp+=y;zMoyUp+=z;
					}
				}
			}
		}
		if(hitsUp==0)hitsUp=1;
		if(hitsDown==0)hitsDown=1;
		xMoyUp=xMoyUp/hitsUp;//Center of mass computation. 
		yMoyUp=yMoyUp/hitsUp;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyUp=zMoyUp/hitsUp+zMax/2+zQuarter-zVentile;//due to the extraction of a substack zmax/2-zQuarter+1 - zmax/2     zMax/2+zQuarter-zVentile

		xMoyDown=xMoyDown/hitsDown;//Center of mass computation. 
		yMoyDown=yMoyDown/hitsDown;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyDown=zMoyDown/hitsDown+zMax/2-zQuarter+1;//due to the extraction of a substack zmax/2 - zmax/2+zQuarter       zMax/2-zQuarter+1

		if(debug) {
			System.out.println("HitsUp="+hitsUp+" ..Center of mass up = "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("HitsDown="+hitsDown+" ..Center of mass down = "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		xMoyUp=xMoyUp*vX;		
		yMoyUp=yMoyUp*vY;		
		zMoyUp=zMoyUp*vZ;	
		xMoyDown=xMoyDown*vX;		
		yMoyDown=yMoyDown*vY;		
		zMoyDown=zMoyDown*vZ;		
		if(debug) {
			System.out.println("Center of mass up (coord reel)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (coord reel)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}

		//Step 4 : compute the axis vector, that will stands for Z vector after alignement
		double[]vectZ=TransformUtils.normalize(new double[] {xMoyUp - xMoyDown , yMoyUp - yMoyDown , zMoyUp - zMoyDown});
		double[][]axisVerificationMatrix=VitiDialogs.inspectAxis( acq.getTransformedRegistrationImage() ,vectZ,new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 ),delayForReacting);
		double []vectZbis=axisVerificationMatrix[0];
		double epsilon=0.0000001;
		if(TransformUtils.norm(TransformUtils.vectorialSubstraction(vectZbis,vectZ))>epsilon) {//Une erreur a été corrigée par l'utilisateur
			System.out.println("En effet il y a eu modification");
			xMoyUp=axisVerificationMatrix[1][0];xMoyDown=axisVerificationMatrix[2][0];
			yMoyUp=axisVerificationMatrix[1][1];yMoyDown=axisVerificationMatrix[2][1];
			zMoyUp=axisVerificationMatrix[1][2];zMoyDown=axisVerificationMatrix[2][2];
			vectZ=vectZbis;
		}

		
		System.out.println("Vecteur axial ="+TransformUtils.stringVector(vectZ,""));
		double []vectXtmp=new double[] {1,0,0};
		double []vectX=TransformUtils.normalize(TransformUtils.vectorialSubstraction(vectXtmp, TransformUtils.proj_u_of_v(vectZ, vectXtmp)));
		System.out.println("Vecteur orthogonal ="+TransformUtils.stringVector(vectX,""));

		
		
		

		//Step 5 : compute the three points
		Point3d origine=new Point3d(xMoyUp*0.5+xMoyDown*0.5 , yMoyUp*0.5+yMoyDown*0.5 , zMoyUp*0.5+zMoyDown*0.5 );
		Point3d ptUp= new Point3d(origine.x + vectZ[0]   ,  origine.y + vectZ[1] , origine.z + vectZ[2]);
		Point3d ptRight= new Point3d(origine.x + vectX[0]   ,  origine.y + vectX[1] , origine.z + vectX[2]);
		return new Point3d[] {origine,ptUp,ptRight};
	}
	
	
	
	
	
	public static double[][] getCentersOfSlicesFromMask(ImagePlus img){
		img.show();
		Analyzer an=new Analyzer(img);
		int zMax=img.getStackSize();
		double voxZ=VitimageUtils.getVoxelSizes(img)[2];
		double[][]tabRet=new double[zMax][3];
		for(int i=0;i<zMax;i++) {
			img.setSlice(i+1);
			IJ.run(img, "Select All", "");
			an.measure();
			an.displayResults();
			tabRet[i][0]=an.getResultsTable().getValue(8,i);
			tabRet[i][1]=an.getResultsTable().getValue(9,i);
			tabRet[i][2]=i*voxZ;
		}
		img.hide();
		return tabRet;
	}

	
	
	
	
	
	public static ImagePlus smoothContourOfPlant(ImagePlus img2,int slice) {
		int zMax=img2.getStackSize();
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
//		VitimageUtils.imageChecking(img2,"A l arrivee de smooth contour");
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*0.5,vX*0.5,vX*0.5);
		img.getStack().getProcessor(1).set(0);
		img.getStack().getProcessor(2).set(0);
		img.getStack().getProcessor(3).set(0);
		img.getStack().getProcessor(4).set(0);
		img.getStack().getProcessor(zMax).set(0);
		img.getStack().getProcessor(zMax-1).set(0);
		img.getStack().getProcessor(zMax-2).set(0);
		img.getStack().getProcessor(zMax-3).set(0);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		ImagePlus imgMask=VitimageUtils.getBinaryMask(img,thresh);
		//						VitimageUtils.imageChecking(imgMask,"Premier masque");
		imgMask=VitimageUtils.connexe(imgMask,1,256, 0,10E10, 6, 1,false);//L objet
		imgMask=new Duplicator().run(imgMask,slice,slice);
		//				VitimageUtils.imageChecking(imgMask,"Connexe");
		IJ.run(imgMask,"8-bit","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Dilate","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
		IJ.run(imgMask,"Erode","");
//		VitimageUtils.imageChecking(imgMask,"8bit");
//		VitimageUtils.imageChecking(imgMask,"Apres morpho");
		
		IJ.run(imgMask, "Fill Holes", "stack");
		//		VitimageUtils.imageChecking(imgMask,"Apres fill holes");
		
		imgMask=VitimageUtils.gaussianFiltering(imgMask, 10*vX, 10*vX,0);
		//		VitimageUtils.imageChecking(imgMask,"Apres lissage");
		imgMask.getProcessor().resetMinAndMax();
		return VitimageUtils.getBinaryMask(imgMask, 210);		
	}
	
	
	public static Point3d[] detectInoculationPointManually(ImagePlus img, Point3d inocPoint) {
		boolean ghetto=true;
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( img.getWidth()/2.0 , img.getHeight()/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		return ret;		
	}

	public static Point3d[] detectInoculationPointGuidedByAutomaticComputedOutline(ImagePlus img,ImagePlus maskForOutline) {
		boolean ghetto=true;
		ImagePlus imgCheck=new Duplicator().run(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		int facteurAniso=(int)Math.round(vZ/vX);
		double IPStdZSize=4; //mm
		double IPStdXSize=2; //mm
		double sigmaXY=IPStdXSize/2.0;
		double sigmaZ=IPStdXSize/4.0;
		int sigmaPlotZ=5;
		double sigmaXYInPixels=10*0.035/vX;//sigmaXY/vX;
		double sigmaZInPixels=0.2*0.5/vZ;//sigmaZ/vZ;
		int minPossibleZ=zMax/4+2;
		int maxPossibleZ=(zMax*3)/4-2;
		ImagePlus imgSlice=new Duplicator().run(maskForOutline,1,1);
		IJ.run(imgSlice, "Outline", "stack");
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		ImagePlus imgOutline= new Duplicator().run(imgSlice, 1,1);
		imgSlice=connexe(imgSlice,255,256,0,10E10,8,1,true);
		System.out.println(" Ok.");


		System.out.print("Selection equipartited points for analysis and sort by angle around the center...");
		IJ.run(imgSlice, "8-bit", "");
		imgSlice.getProcessor().invert();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(imgSlice, "Create Selection", "");
		rm.addRoi(imgSlice.getRoi());
		Roi roi=rm.getRoi(0);
		FloatPolygon fp=roi.getContainedFloatPoints();
		int nAngles=fp.npoints;
		System.out.println("Nombre de points selectionnes : "+nAngles);
		double [][]tabCoord=new double[nAngles][3];
		Double [][]tabSort=new Double[nAngles][3];
		double xCenter = img.getWidth()/2;
		double yCenter = img.getHeight()/2;
		for (int i=0; i<nAngles; i++) {
			tabSort[i][0]=new Double(fp.xpoints[i]);
			tabSort[i][1]=new Double(fp.ypoints[i]);
			tabSort[i][2]=new Double(TransformUtils.calculateAngle(tabSort[i][0]-xCenter,yCenter-tabSort[i][1]));
			
		}
		imgSlice.changes=false;
		imgSlice.close();
		rm.close();
		//sort by angles
		Arrays.sort(tabSort,new AngleComparator());
		for (int i=0; i<nAngles; i++) {
			tabCoord[i][0]=tabSort[i][0].doubleValue();
			tabCoord[i][1]=tabSort[i][1].doubleValue();
			tabCoord[i][2]=tabSort[i][2].doubleValue();
		}
		System.out.println(" Ok.");
		double [][]meanValues=new double[nAngles][zMax];

		System.out.print("Measurements");
		ImagePlus measures=ij.gui.NewImage.createImage("measures",nAngles,zMax*facteurAniso,3,32,ij.gui.NewImage.FILL_BLACK);
		float[]measuresImg0=(float[]) measures.getStack().getProcessor(1).getPixels();
		float[]measuresImg1=(float[]) measures.getStack().getProcessor(2).getPixels();
		float[]measuresImg2=(float[]) measures.getStack().getProcessor(3).getPixels();

		for (int ang=0; ang<nAngles; ang++){
			for (int z=0; z<zMax; z++){			
				meanValues[ang][z]=VitimageUtils.meanValueofImageAround(img,(int)Math.round(tabCoord[ang][0]),(int)Math.round(tabCoord[ang][1]),z,sigmaXYInPixels);
				for(int i=0;i<facteurAniso;i++)measuresImg0[nAngles*(z*facteurAniso+i)+ang]=(float) meanValues[ang][z];
			}
		}

		System.out.println(" Ok.");




		System.out.print("Score computation");
		//
		double[][][]scores=new double[nAngles][zMax][3];
		for (int ang=0; ang<nAngles; ang++){
			for (int z=minPossibleZ; z<=maxPossibleZ; z++){			
				double acc=0;
				for(int i=-sigmaPlotZ;i<=sigmaPlotZ;i++)acc+=meanValues[ang][z+i];
				scores[ang][z][0]=acc/(2*sigmaPlotZ+1);
				scores[ang][z][1]=(scores[ang][z][0]-meanValues[ang][z])/scores[ang][z][0];
				scores[ang][z][2]=(scores[ang][z][0]-meanValues[ang][z]);
				for(int i=0;i<facteurAniso;i++)measuresImg1[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][1];
				for(int i=0;i<facteurAniso;i++)measuresImg2[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][2];
			}
		}

		System.out.println(" Ok.");
		measures.getProcessor().setMinAndMax(measures.getProcessor().getMin(),measures.getProcessor().getMax());
		IJ.run(measures,"Fire","");
		//	anna.storeImage(measures, "Image de score");
		ImagePlus imgDetect=new Duplicator().run(measures,1,measures.getStackSize());
		IJ.run(imgDetect, "Gaussian Blur...", "sigma="+(1/(2*vX))+" stack");
		//	anna.remember("Parametre de lissage utilise, en pixel","sigma="+(1/(2*vX)));
		//	anna.storeImage(imgDetect, "Image de score lissee");
		imgDetect.getProcessor().resetMinAndMax();
		imgDetect.show();
		imgDetect.setTitle("Score map for inoculation detection");
		VitimageUtils.waitFor(4000);
		imgDetect.hide();
		double[][]coordMax=TransformUtils.getCoordinatesOf(imgDetect,VitimageUtils.COORD_OF_MAX_IN_TWO_LAST_SLICES,minPossibleZ*facteurAniso,maxPossibleZ*facteurAniso);
		System.out.println("Maximum relatif obtenu à ("+coordMax[0][0]+" , "+coordMax[0][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[0][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[0][0])][1]+" , "+
				((coordMax[0][1]-facteurAniso/2.0)/facteurAniso)+" )");
		System.out.println("Maximum absolu obtenu à ("+coordMax[1][0]+" , "+coordMax[1][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[1][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[1][0])][1]+" , "+
				((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)+" )");
		Point3d inocPoint=TransformUtils.convertPointToRealSpace(new Point3d( tabCoord[(int)Math.round(coordMax[1][0])][0],  tabCoord[(int)Math.round(coordMax[1][0])][1],  ((coordMax[1][1]-facteurAniso/2.0)/facteurAniso) ),img) ;

		inocPoint=VitiDialogs.inspectInoculationPoint( imgCheck ,inocPoint);

		
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( xMax/2.0 , yMax/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		imgCheck.close();
		return ret;		
	}

/*
	public static Point3d[] detectInoculationPoint(ImagePlus img,double thresholdMin) {
		ImagePlus imgCheck=new Duplicator().run(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		int facteurAniso=(int)Math.round(vZ/vX);
		double IPStdZSize=4; //mm
		double IPStdXSize=2; //mm
		double sigmaXY=IPStdXSize/2.0;
		double sigmaZ=IPStdXSize/4.0;
		int sigmaPlotZ=5;
		double sigmaXYInPixels=10*0.035/vX;//sigmaXY/vX;
		double sigmaZInPixels=0.2*0.5/vZ;//sigmaZ/vZ;
		int minPossibleZ=zMax/4+2;
		int maxPossibleZ=(zMax*3)/4-2;
		System.out.print("Blur");
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigmaXYInPixels+" y="+sigmaXYInPixels+" z="+sigmaZInPixels+"");
		System.out.println(" Ok.");
		imageChecking(img);
		ImagePlus imgSlice= new Duplicator().run(img, minPossibleZ,minPossibleZ);
		System.out.print("Outline detection ...");
		imgSlice.getProcessor().resetMinAndMax();
		imgSlice.show();
		VitimageUtils.waitFor(2000);
		imgSlice.hide();
			System.out.println("Application seuillage de valeur "+thresholdMin);
		imgSlice.getProcessor().setThreshold(thresholdMin,Math.pow(2,16)-1,ImageProcessor.BLACK_AND_WHITE_LUT);
		Prefs.blackBackground = true;
		IJ.run(imgSlice, "Convert to Mask", "method=Default background=Dark calculate black");
		VitimageUtils.waitFor(2000);
		for(int er=0;er<6;er++) {
			IJ.run(imgSlice, "Erode", "stack");
			imgSlice.setTitle("Erosion numero "+er);
			imgSlice.show();
			VitimageUtils.waitFor(2000);
			imgSlice.hide();
		}
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		IJ.run(imgSlice, "Outline", "stack");
		imgSlice.show();
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		ImagePlus imgOutline= new Duplicator().run(imgSlice, 1,1);
		imgSlice=connexe(imgSlice,255,256,0,10E10,8,1,true);
		System.out.println(" Ok.");


		System.out.print("Selection equipartited points for analysis and sort by angle around the center...");
		IJ.run(imgSlice, "8-bit", "");
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		imgSlice.getProcessor().invert();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(imgSlice, "Create Selection", "");
		rm.addRoi(imgSlice.getRoi());
		Roi roi=rm.getRoi(0);
		FloatPolygon fp=roi.getContainedFloatPoints();
		int nAngles=fp.npoints;
		System.out.println("Nombre de points selectionnes : "+nAngles);
		double [][]tabCoord=new double[nAngles][3];
		Double [][]tabSort=new Double[nAngles][3];
		double xCenter = img.getWidth()/2;
		double yCenter = img.getHeight()/2;
		for (int i=0; i<nAngles; i++) {
			tabSort[i][0]=new Double(fp.xpoints[i]);
			tabSort[i][1]=new Double(fp.ypoints[i]);
			tabSort[i][2]=new Double(TransformUtils.calculateAngle(tabSort[i][0]-xCenter,yCenter-tabSort[i][1]));
			
		}
		imgSlice.changes=false;
		imgSlice.close();
		rm.close();
		//sort by angles
		Arrays.sort(tabSort,new AngleComparator());
		for (int i=0; i<nAngles; i++) {
			tabCoord[i][0]=tabSort[i][0].doubleValue();
			tabCoord[i][1]=tabSort[i][1].doubleValue();
			tabCoord[i][2]=tabSort[i][2].doubleValue();
		}
		System.out.println(" Ok.");
		double [][]meanValues=new double[nAngles][zMax];

		System.out.print("Measurements");
		ImagePlus measures=ij.gui.NewImage.createImage("measures",nAngles,zMax*facteurAniso,3,32,ij.gui.NewImage.FILL_BLACK);
		float[]measuresImg0=(float[]) measures.getStack().getProcessor(1).getPixels();
		float[]measuresImg1=(float[]) measures.getStack().getProcessor(2).getPixels();
		float[]measuresImg2=(float[]) measures.getStack().getProcessor(3).getPixels();

		for (int ang=0; ang<nAngles; ang++){
			for (int z=0; z<zMax; z++){			
				meanValues[ang][z]=VitimageUtils.meanValueofImageAround(img,(int)Math.round(tabCoord[ang][0]),(int)Math.round(tabCoord[ang][1]),z,sigmaXYInPixels);
				for(int i=0;i<facteurAniso;i++)measuresImg0[nAngles*(z*facteurAniso+i)+ang]=(float) meanValues[ang][z];
			}
		}

		System.out.println(" Ok.");




		System.out.print("Score computation");
		//
		double[][][]scores=new double[nAngles][zMax][3];
		for (int ang=0; ang<nAngles; ang++){
			for (int z=minPossibleZ; z<=maxPossibleZ; z++){			
				double acc=0;
				for(int i=-sigmaPlotZ;i<=sigmaPlotZ;i++)acc+=meanValues[ang][z+i];
				scores[ang][z][0]=acc/(2*sigmaPlotZ+1);
				scores[ang][z][1]=(scores[ang][z][0]-meanValues[ang][z])/scores[ang][z][0];
				scores[ang][z][2]=(scores[ang][z][0]-meanValues[ang][z]);
				for(int i=0;i<facteurAniso;i++)measuresImg1[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][1];
				for(int i=0;i<facteurAniso;i++)measuresImg2[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][2];
			}
		}

		System.out.println(" Ok.");
		measures.getProcessor().setMinAndMax(measures.getProcessor().getMin(),measures.getProcessor().getMax());
		IJ.run(measures,"Fire","");
		//	anna.storeImage(measures, "Image de score");
		ImagePlus imgDetect=new Duplicator().run(measures,1,measures.getStackSize());
		IJ.run(imgDetect, "Gaussian Blur...", "sigma="+(1/(2*vX))+" stack");
		//	anna.remember("Parametre de lissage utilise, en pixel","sigma="+(1/(2*vX)));
		//	anna.storeImage(imgDetect, "Image de score lissee");
		imgDetect.getProcessor().resetMinAndMax();
		imgDetect.show();
		imgDetect.setTitle("Score map for inoculation detection");
		VitimageUtils.waitFor(5000);
		imgDetect.hide();
		double[][]coordMax=TransformUtils.getCoordinatesOf(imgDetect,VitimageUtils.COORD_OF_MAX_IN_TWO_LAST_SLICES,minPossibleZ*facteurAniso,maxPossibleZ*facteurAniso);
		System.out.println("Maximum relatif obtenu à ("+coordMax[0][0]+" , "+coordMax[0][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[0][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[0][0])][1]+" , "+
				((coordMax[0][1]-facteurAniso/2.0)/facteurAniso)+" )");
		System.out.println("Maximum absolu obtenu à ("+coordMax[1][0]+" , "+coordMax[1][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[1][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[1][0])][1]+" , "+
				((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)+" )");
		Point3d inocPoint=TransformUtils.convertPointToRealSpace(new Point3d( tabCoord[(int)Math.round(coordMax[1][0])][0],  tabCoord[(int)Math.round(coordMax[1][0])][1],  ((coordMax[1][1]-facteurAniso/2.0)/facteurAniso) ),img) ;

		inocPoint=VitiDialogs.inspectInoculationPoint( imgCheck ,inocPoint);

		
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( xMax/2.0 , yMax/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		imgCheck.close();
		return ret;		
	}

	public static Point3d[] detectInoculationPointIRMT2(ImagePlus img,ImagePlus mask) {
		ImagePlus imgCheck=new Duplicator().run(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;

		//Preparation des donnees pour lecture valeurs
		int facteurAniso=(int)Math.round(vZ/vX);
		double IPStdZSize=4; //mm
		double IPStdXSize=2; //mm
		double sigmaXY=IPStdXSize/2.0;
		double sigmaZ=IPStdXSize/4.0;
		int sigmaPlotZ=5;
		double sigmaXYInPixels=10*0.035/vX;//sigmaXY/vX;
		double sigmaZInPixels=0.2*0.5/vZ;//sigmaZ/vZ;
		int minPossibleZ=zMax/4+2;
		int maxPossibleZ=(zMax*3)/4-2;
		System.out.print("Blur");
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigmaXYInPixels+" y="+sigmaXYInPixels+" z="+sigmaZInPixels+"");
		System.out.println(" Ok.");

		
		//Preparation masque outline
		
		
		IJ.run(mask, "Erode", "stack");
		IJ.run(mask, "Gaussian Blur 3D...", "x="+sigmaXYInPixels+" y="+sigmaXYInPixels+" z="+sigmaZInPixels+"");
		System.out.println("Application seuillage de valeur "+200);
		mask.getProcessor().setThreshold(200,255,ImageProcessor.BLACK_AND_WHITE_LUT);
		Prefs.blackBackground = true;
		IJ.run(mask, "Convert to Mask", "method=Default background=Dark calculate black");		
		ImagePlus imgSlice= new Duplicator().run(mask, (minPossibleZ+maxPossibleZ)/2,(minPossibleZ+maxPossibleZ)/2);
		System.out.print("Outline detection ...");
		imgSlice.show();
		VitimageUtils.waitFor(2000);
		imgSlice.hide();
		IJ.run(imgSlice, "Outline", "stack");
		imgSlice.show();
		VitimageUtils.waitFor(2000);
		imgSlice.hide();

		System.out.print("Selection equipartited points for analysis and sort by angle around the center...");
		IJ.run(imgSlice, "8-bit", "");
		imgSlice.show();
		VitimageUtils.waitFor(1000);
		imgSlice.hide();
		imgSlice.getProcessor().invert();
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(imgSlice, "Create Selection", "");
		rm.addRoi(imgSlice.getRoi());
		Roi roi=rm.getRoi(0);
		FloatPolygon fp=roi.getContainedFloatPoints();
		int nAngles=fp.npoints;
		System.out.println("Nombre de points selectionnes : "+nAngles);
		double [][]tabCoord=new double[nAngles][3];
		Double [][]tabSort=new Double[nAngles][3];
		double xCenter = img.getWidth()/2;
		double yCenter = img.getHeight()/2;
		for (int i=0; i<nAngles; i++) {
			tabSort[i][0]=new Double(fp.xpoints[i]);
			tabSort[i][1]=new Double(fp.ypoints[i]);
			tabSort[i][2]=new Double(TransformUtils.calculateAngle(tabSort[i][0]-xCenter,yCenter-tabSort[i][1]));
			
		}
		imgSlice.changes=false;
		imgSlice.close();
		rm.close();
		//sort by angles
		Arrays.sort(tabSort,new AngleComparator());
		for (int i=0; i<nAngles; i++) {
			tabCoord[i][0]=tabSort[i][0].doubleValue();
			tabCoord[i][1]=tabSort[i][1].doubleValue();
			tabCoord[i][2]=tabSort[i][2].doubleValue();
		}
		System.out.println(" Ok.");
		double [][]meanValues=new double[nAngles][zMax];

		System.out.print("Measurements");
		ImagePlus measures=ij.gui.NewImage.createImage("measures",nAngles,zMax*facteurAniso,3,32,ij.gui.NewImage.FILL_BLACK);
		float[]measuresImg0=(float[]) measures.getStack().getProcessor(1).getPixels();
		float[]measuresImg1=(float[]) measures.getStack().getProcessor(2).getPixels();
		float[]measuresImg2=(float[]) measures.getStack().getProcessor(3).getPixels();

		for (int ang=0; ang<nAngles; ang++){
			for (int z=0; z<zMax; z++){			
				meanValues[ang][z]=VitimageUtils.meanValueofImageAround(img,(int)Math.round(tabCoord[ang][0]),(int)Math.round(tabCoord[ang][1]),z,sigmaXYInPixels);
				for(int i=0;i<facteurAniso;i++)measuresImg0[nAngles*(z*facteurAniso+i)+ang]=(float) meanValues[ang][z];
			}
		}

		System.out.println(" Ok.");




		System.out.print("Score computation");
		//
		double[][][]scores=new double[nAngles][zMax][3];
		for (int ang=0; ang<nAngles; ang++){
			for (int z=minPossibleZ; z<=maxPossibleZ; z++){			
				double acc=0;
				for(int i=-sigmaPlotZ;i<=sigmaPlotZ;i++)acc+=meanValues[ang][z+i];
				scores[ang][z][0]=acc/(2*sigmaPlotZ+1);
				scores[ang][z][1]=(scores[ang][z][0]-meanValues[ang][z])/scores[ang][z][0];
				scores[ang][z][2]=(scores[ang][z][0]-meanValues[ang][z]);
				for(int i=0;i<facteurAniso;i++)measuresImg1[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][1];
				for(int i=0;i<facteurAniso;i++)measuresImg2[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][2];
			}
		}

		System.out.println(" Ok.");
		measures.getProcessor().setMinAndMax(measures.getProcessor().getMin(),measures.getProcessor().getMax());
		IJ.run(measures,"Fire","");
		//	anna.storeImage(measures, "Image de score");
		ImagePlus imgDetect=new Duplicator().run(measures,1,measures.getStackSize());
		IJ.run(imgDetect, "Gaussian Blur...", "sigma="+(1/(2*vX))+" stack");
		//	anna.remember("Parametre de lissage utilise, en pixel","sigma="+(1/(2*vX)));
		//	anna.storeImage(imgDetect, "Image de score lissee");
		imgDetect.getProcessor().resetMinAndMax();
		imgDetect.show();
		imgDetect.setTitle("Score map for inoculation detection");
		VitimageUtils.waitFor(5000);
		imgDetect.hide();
		double[][]coordMax=TransformUtils.getCoordinatesOf(imgDetect,VitimageUtils.COORD_OF_MAX_IN_TWO_LAST_SLICES,minPossibleZ*facteurAniso,maxPossibleZ*facteurAniso);
		System.out.println("Maximum relatif obtenu à ("+coordMax[0][0]+" , "+coordMax[0][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[0][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[0][0])][1]+" , "+
				((coordMax[0][1]-facteurAniso/2.0)/facteurAniso)+" )");
		System.out.println("Maximum absolu obtenu à ("+coordMax[1][0]+" , "+coordMax[1][1]+" ) soit, en coordonnees images : ( "+
				tabCoord[(int)Math.round(coordMax[1][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[1][0])][1]+" , "+
				((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)+" )");
		Point3d inocPoint=TransformUtils.convertPointToRealSpace(new Point3d( tabCoord[(int)Math.round(coordMax[1][0])][0],  tabCoord[(int)Math.round(coordMax[1][0])][1],  ((coordMax[1][1]-facteurAniso/2.0)/facteurAniso) ),img) ;

		inocPoint=VitiDialogs.inspectInoculationPoint( imgCheck ,inocPoint);

		
		
		Point3d origine=TransformUtils.convertPointToRealSpace(new Point3d( xMax/2.0 , yMax/2.0 ,0),img);
		origine.z=inocPoint.z;

		double[]vect=new double[] {inocPoint.x - origine.x , inocPoint.y - origine.y , inocPoint.z - origine.z };
		double[]vectNorm=TransformUtils.normalize(vect);
		System.out.println(TransformUtils.stringVector(vectNorm,"Vecteur normalisé de la moelle vers le point d inoculation"));
		Point3d originePlusDinoc=new Point3d( origine.x + vectNorm[0] , origine.y + vectNorm[1] , inocPoint.z  + vectNorm[2]);
		Point3d originePlusDz=new Point3d( origine.x  ,origine.y , origine.z + 1 );
		Point3d[]ret=new Point3d[] { origine ,  originePlusDz , originePlusDinoc , inocPoint};
		imgCheck.close();
		return ret;		
	}
*/

	
	
	
	
	
	
	/*
	public static ImagePlus areaOfPertinentComputation2 (ImagePlus img2,double sigmaGaussMapInPixels){
		int zMax=img2.getStackSize();
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		img.getStack().getProcessor(1).set(0);
		if(zMax>3) {
			img.getStack().getProcessor(2).set(0);
			img.getStack().getProcessor(zMax).set(0);
			img.getStack().getProcessor(zMax-1).set(0);
		}
		ImagePlus imgMask0=VitimageUtils.getBinaryMask(img,thresh);
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,256, 0,10E10, 6, 1,false);//L objet
		ImagePlus imgMask2=VitimageUtils.connexe(imgMask0,1,256, 0,10E10, 6, 2,false);//le cap
		IJ.run(imgMask1,"8-bit","");
		IJ.run(imgMask2,"8-bit","");
		ImageCalculator ic=new ImageCalculator();
		ImagePlus imgMask3=ic.run("OR create stack", imgMask1,imgMask2);							
		return imgMask3;
	}
*/

	public static ImagePlus areaOfPertinentMRIMapComputation (ImagePlus img2,double sigmaGaussMapInPixels){
		double voxVolume=VitimageUtils.getVoxelVolume(img2);
		int nbThreshObjects=100;
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		ImagePlus imgMask0=VitimageUtils.getBinaryMask(img,thresh);
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,256, nbThreshObjects*voxVolume,10E10, 26, 0,false);//L objet
		System.out.println(nbThreshObjects);
		ImagePlus imgMask2=VitimageUtils.getBinaryMask(imgMask1,1);
		return imgMask2;
	}
	
	

	public static ImagePlus restrictionMaskForFadingHandling (ImagePlus img,int marginOut){
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		if(marginOut>dimZ/3)marginOut=dimZ/3;
		ImagePlus ret=IJ.createImage("MaskRegistration_"+img.getTitle(), dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		for(int z=0;z<dimZ;z++) {
			if((z<marginOut) || ( (dimZ-z)<marginOut))continue;
			byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					tabRet[dimX*y+x]=(byte)(255 & 0xff);
				}
			}
		}
		return ret;
	}
	
	
	
	public static ImagePlus convertFloatToShortWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		IJ.run(ret,"16-bit","");
		float[][] in=new float[imgIn.getStackSize()][];
		short[][] out=new short[ret.getStackSize()][];
		int index;
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			in[z]=(float []) imgIn.getStack().getProcessor(z+1).getPixels();
			out[z]=(short []) ret.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=(short)((int)(Math.round(in[z][y*X+x])));
				}			
			}
		}
		return ret;
	}
	
	public static ImagePlus convertShortToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		IJ.run(ret,"32-bit","");
		float[][] out=new float[ret.getStackSize()][];
		short[][] in=new short[imgIn.getStackSize()][];
		int index;
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			in[z]=(short []) imgIn.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=(float)((int)((in[z][y*X+x] & 0xffff )));
				}			
			}
		}
		return ret;
	}
	
	public static ImagePlus convertByteToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		IJ.run(ret,"8-bit","");
		float[][] out=new float[ret.getStackSize()][];
		byte[][] in=new byte[imgIn.getStackSize()][];
		int index;
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			in[z]=(byte []) imgIn.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=(byte)((int)((in[z][y*X+x] & 0xff )));
				}			
			}
		}
		return ret;
	}
	

	public static double getOtsuThreshold(ImagePlus img) {
		OtsuThresholdImageFilter otsu=new OtsuThresholdImageFilter();
		otsu.execute(ItkImagePlusInterface.imagePlusToItkImage(img));
		return otsu.getThreshold();
	}

	public static ImagePlus otsuThreshold(ImagePlus img) {
		OtsuThresholdImageFilter otsu=new OtsuThresholdImageFilter();
		otsu.setInsideValue((short)0);
		otsu.setOutsideValue((short)255);
		return(ItkImagePlusInterface.itkImageToImagePlus(otsu.execute(ItkImagePlusInterface.imagePlusToItkImage(img))));
	}

	public static ImagePlus multiplyImage(ImagePlus imgIn,double val) {
		ImagePlus img=new Duplicator().run(imgIn);
		IJ.run(img,"Multiply...","value="+val+" stack");
		return img;
	}
	
	
	public static ImagePlus compositeRGBByte(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,double coefR,double coefG,double coefB){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		IJ.run(img1,"Multiply...","value="+coefR+" stack");
		IJ.run(img2,"Multiply...","value="+coefG+" stack");
		IJ.run(img3,"Multiply...","value="+coefB+" stack");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),img3.getStack(),true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}

	public static ImagePlus balanceColors(ImagePlus imgIn,int radius) {
		System.out.println("\n Normalisation image, avec rayon="+radius);
		ImagePlus img=new Duplicator().run(imgIn);
		ImagePlus[] channels = ChannelSplitter.split(img);
		int xM=channels[0].getWidth();		
		int yM=channels[1].getHeight();
		int zM=channels[2].getStackSize();
		ImagePlus[][] tabImg=new ImagePlus[3][zM];
		double[]tempVals;
		double [][][] vals=new double[3][zM][5];
		for(int can=0;can<3;can++) {
			for(int z=0;z<zM;z++) {
				channels[can].setSlice(z+1);
				tabImg[can][z]=channels[can].crop();
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],0, 0, 0, radius, radius, 0);vals[can][z][0]=VitimageUtils.statistics1D(tempVals)[0];
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],xM-radius-1, 0, 0, xM-1, radius, 0);vals[can][z][1]=VitimageUtils.statistics1D(tempVals)[0];
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],0, yM-radius-1, 0, radius, yM-1, 0);vals[can][z][2]=VitimageUtils.statistics1D(tempVals)[0];
				tempVals=VitimageUtils.valuesOfBlock(tabImg[can][z],xM-radius-1,  yM-radius-1, 0, xM-1, yM-1, 0);vals[can][z][3]=VitimageUtils.statistics1D(tempVals)[0];
				vals[can][z][4]=0.25*(vals[can][z][0]+vals[can][z][1]+vals[can][z][2]+vals[can][z][3]);
				//System.out.println("Canal="+can+" Z="+z+" Valeurs="+TransformUtils.stringVectorN(vals[can][z], "")+"");
				IJ.run(tabImg[can][z],"32-bit","");
				tabImg[can][z].getProcessor().setMinAndMax(0,vals[can][z][4]);
				IJ.run(tabImg[can][z],"8-bit","");
			}
			channels[can]=Concatenator.run(tabImg[can]);
		}
		ImageStack is=RGBStackMerge.mergeStacks(channels[0].getStack(),channels[1].getStack(),channels[2].getStack(),true);
		img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, imgIn);
		return img;
	}

	
	public static ImagePlus compositeRGJetDouble(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,double coefR,double coefJet,double coefB,String title){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		img1.getProcessor().resetMinAndMax();
		img2.getProcessor().resetMinAndMax();
		img3.getProcessor().resetMinAndMax();
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		IJ.run(img3,"8-bit","");
		IJ.run(img3,"Fire...","");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),img3.getStack(),true);
		ImagePlus img=new ImagePlus(title,is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}

	
	
	public static ImagePlus compositeRGBDoubleJet(ImagePlus img1,ImagePlus img2,ImagePlus img3,String title,boolean mask,int teinte) {
		ImagePlus img1Source=new Duplicator().run(img1);
		ImagePlus img2Source=new Duplicator().run(img2);
		ImagePlus img3Source=new Duplicator().run(img3);
		img1Source.getProcessor().resetMinAndMax();
		img2Source.getProcessor().resetMinAndMax();
		img3Source.getProcessor().resetMinAndMax();
		IJ.run(img1Source,"8-bit","");
		IJ.run(img2Source,"8-bit","");
		IJ.run(img3Source,"8-bit","");
		if(mask) {
			ImagePlus maskJet=VitimageUtils.thresholdByteImage(img3Source, 1, 256);
			IJ.run(maskJet,"Invert","");
			IJ.run(maskJet, "Divide...", "value=255");
			img1Source = new ImageCalculator().run("Multiply create", img1Source, maskJet);
			img2Source = new ImageCalculator().run("Multiply create", img2Source, maskJet);
		}
		IJ.run(img1Source,"Red","");
		IJ.run(img2Source,"Green","");
		if(teinte==0)IJ.run(img3Source,"Blue","");
		if(teinte==1)IJ.run(img3Source,"Grays","");
		if(teinte==2)IJ.run(img3Source,"Fire","");
		ImagePlus ret=RGBStackMerge.mergeChannels(new ImagePlus[] {img1Source,img2Source,img3Source},false);
		IJ.run(ret,"RGB Color","");
		return ret;
	}
	
	
	
	public static ImagePlus compositeRGBDouble(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,double coefR,double coefG,double coefB,String title){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		img1.getProcessor().resetMinAndMax();
		img2.getProcessor().resetMinAndMax();
		img3.getProcessor().resetMinAndMax();
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		IJ.run(img3,"8-bit","");
//		img3.getProcessor().setMinAndMax(60, 200);
//		IJ.run(img3,"8-bit","");
//		IJ.run(img1,"Multiply...","value="+coefR+" stack");
		//		IJ.run(img2,"Multiply...","value="+coefG+" stack");
		//IJ.run(img3,"Multiply...","value="+coefB+" stack");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),img3.getStack(),true);
		ImagePlus img=new ImagePlus(title,is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
	
	
	public static ImagePlus compositeRGBLByte(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,ImagePlus img4Source,double coefR,double coefG,double coefB,double coefL){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		ImagePlus img4=new Duplicator().run(img4Source);
		IJ.run(img1,"Multiply...","value="+coefR+" stack");
		IJ.run(img2,"Multiply...","value="+coefG+" stack");
		IJ.run(img3,"Multiply...","value="+coefB+" stack");
		IJ.run(img4,"Multiply...","value="+coefL+" stack");
		ImagePlus img=RGBStackMerge.mergeChannels(new ImagePlus[] {img1,img2,img3,img4},true);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
	
	

	
	public static ImagePlus compositeOf(ImagePlus img1Source,ImagePlus img2Source){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		img1.getProcessor().resetMinAndMax();
		img2.getProcessor().resetMinAndMax();
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}


	public static Point3d[]convertDoubleArrayToPoint3dArray(double[][]tab){
		Point3d[]tabPt=new Point3d[tab.length];
		for(int i=0;i<tab.length;i++)tabPt[i]=new Point3d(tab[i][0],tab[i][1],tab[i][2]);
		return tabPt;		
	}
	
	public static ImagePlus compositeNoAdjustOf(ImagePlus img1Source,ImagePlus img2Source){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
	
	public static ImagePlus compositeOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}

	public static ImagePlus compositeNoAdjustOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeNoAdjustOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}

	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize,int numLine) {
		ImagePlus ret=new Duplicator().run(img);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(10*img.getWidth()*1.0/512,10*img.getWidth()*1.0/512+numLine*fontSize*2, text, font);
		roi.setStrokeColor(Color.white);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			IJ.run(ret, "Draw", "stack");
			ret.setRoi((Roi)null);
		}
		return ret;
	}

	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize,int numLine,double value) {
		double valMin=img.getProcessor().getMin();
		double valMax=img.getProcessor().getMax();
		ImagePlus ret=new Duplicator().run(img);
		ret.getProcessor().setMinAndMax(valMin, value);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(10*img.getWidth()*1.0/512,10*img.getWidth()*1.0/512+numLine*fontSize*2, text, font);
		roi.setStrokeColor(Color.white);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			IJ.run(ret, "Draw", "stack");
			ret.setRoi((Roi)null);
		}
		ret.getProcessor().setMinAndMax(valMin, valMax);
		return ret;
	}


	public static ImagePlus removeEveryTitleTextInStandardFloatImage(ImagePlus img) {
		int heightMax=50;
		ImagePlus ret=new Duplicator().run(img);
		
		if(img.getType() != ImagePlus.GRAY32)return null;
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		float[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(float [])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if (y<heightMax) {
						valsImg[xM*y+x]=0; 
					}
				}
			}			
		}
		return ret;
	}

	public static void putThatImageInThatOther(ImagePlus source,ImagePlus dest) {
		int dimX= source.getWidth(); int dimY= source.getHeight(); int dimZ= source.getStackSize();
		for(int z=0;z<dimZ;z++) {
			int []tabDest=(int[])dest.getStack().getProcessor(z+1).getPixels();
			int []tabSource=(int[])source.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					tabDest[dimX*y+x]=tabSource[dimX*y+x];
				}
			}
		}
	}


	public static void adjustImageCalibration(ImagePlus img,double []voxSize,String unit) {
		if(img==null)return;
		img.getCalibration().setUnit(unit);
		Calibration cal = img.getCalibration();			
		cal.pixelWidth =voxSize[0];
		cal.pixelHeight =voxSize[1];
		cal.pixelDepth =voxSize[2];
	}

	public static void adjustImageCalibration(ImagePlus img,ImagePlus ref) {
		if(img==null)return;
		img.getCalibration().setUnit(ref.getCalibration().getUnit());
		img.getCalibration().pixelWidth=ref.getCalibration().pixelWidth;
		img.getCalibration().pixelHeight=ref.getCalibration().pixelHeight;
		img.getCalibration().pixelDepth=ref.getCalibration().pixelDepth;
	}

	public static void soundAlert(String message) {
		if(message.equals("Inoculation")) {
			try {
				Runtime.getRuntime().exec("aplay /home/fernandr/Audio/Inoc.wav &");
				Runtime.getRuntime().exec("aplay /home/fernandr/Audio/Inoc2.wav &");
				VitimageUtils.waitFor(5000);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			IJ.showMessage("Message not understood. Cannot say it");
		}
	}
		
	public static double getVoxelVolume(ImagePlus img) {
		return img.getCalibration().pixelDepth*img.getCalibration().pixelWidth*img.getCalibration().pixelHeight;
	}

	public static ImagePlus drawCircleInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double realDisX;
		double realDisY;
		double realDisZ;
		byte[][] valsImg=new byte[zM][];
		double distance;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					realDisX=(x-x0)*voxSX;
					realDisY=(y-y0)*voxSY;
					realDisZ=(z-z0)*voxSZ;
					distance=Math.sqrt( realDisX * realDisX  +  realDisY * realDisY  + realDisZ * realDisZ  );
					if(distance < ray) {
						valsImg[z][xM*y+x]=  (byte)( 122 & 0xff);
					}
				}
			}			
		}
		return img;
	}
	
	public static ImagePlus drawCircleInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double realDisX;
		double realDisY;
		double realDisZ;
		byte[][] valsImg=new byte[zM][];
		double distance;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					realDisX=(x-x0)*voxSX;
					realDisY=(y-y0)*voxSY;
					realDisZ=(z-z0)*voxSZ;
					distance=Math.sqrt( realDisX * realDisX  +  realDisY * realDisY  + realDisZ * realDisZ  );
					if(distance < ray) {
						valsImg[z][xM*y+x]=  (byte)( value & 0xff);
					}
				}
			}			
		}
		return img;
	}

	
	public static ImagePlus drawThickLineInFloatImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,double[]vectZ,double value) {
		if(imgIn.getType() != ImagePlus.GRAY32)return null;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double[]vectCur;
		double distanceLine;
		float[][] valsImg=new float[zM][];
		double distance;
		int hit=0;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					if(++hit %197000000==0) {
						System.out.println("Debug");
						double[]vectorProj=TransformUtils.proj_u_of_v( vectZ,vectCur);
						double[]difference=TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur));
						double norme=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
						System.out.println(TransformUtils.stringVector(vectZ, "vectZ"));
						System.out.println("coords point="+x+","+y+","+z);
						System.out.println("origine="+x0+","+y0+","+z0);
						System.out.println(TransformUtils.stringVector(vectCur, "vectCur"));
						System.out.println(TransformUtils.stringVector(vectorProj, "vectorProj"));
						System.out.println(TransformUtils.stringVector(difference, "difference"));						
						System.out.println("Norme = "+norme);						
					}

						
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (float)(value);
					}
				}
			}			
		}
		return img;
	}

	
	
	
	public static ImagePlus drawThickLineInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,double[]vectZ) {
		if(imgIn.getType() == ImagePlus.GRAY32)return drawThickLineInFloatImage(imgIn,ray,x0,y0,z0,vectZ,255);
		if(imgIn.getType() == ImagePlus.GRAY16)return drawThickLineInShortImage(imgIn,ray,x0,y0,z0,vectZ);
		if(imgIn.getType() != ImagePlus.GRAY8)return null;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double[]vectCur;
		double distanceLine;
		byte[][] valsImg=new byte[zM][];
		double distance;
		int hit=0;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					if(++hit %197000000==0) {
						System.out.println("Debug");
						double[]vectorProj=TransformUtils.proj_u_of_v( vectZ,vectCur);
						double[]difference=TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur));
						double norme=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
						System.out.println(TransformUtils.stringVector(vectZ, "vectZ"));
						System.out.println("coords point="+x+","+y+","+z);
						System.out.println("origine="+x0+","+y0+","+z0);
						System.out.println(TransformUtils.stringVector(vectCur, "vectCur"));
						System.out.println(TransformUtils.stringVector(vectorProj, "vectorProj"));
						System.out.println(TransformUtils.stringVector(difference, "difference"));						
						System.out.println("Norme = "+norme);						
					}

						
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (byte)( 255 & 0xff);
					}
				}
			}			
		}
		return img;
	}

	public static ImagePlus drawThickLineInShortImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,double[]vectZ) {
		if(imgIn.getType() != ImagePlus.GRAY16)return null;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double[]vectCur;
		double distanceLine;
		short[][] valsImg=new short[zM][];
		double distance;
		int hit=0;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(short [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					if(++hit %197000000==0) {
						System.out.println("Debug");
						double[]vectorProj=TransformUtils.proj_u_of_v( vectZ,vectCur);
						double[]difference=TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur));
						double norme=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
						System.out.println(TransformUtils.stringVector(vectZ, "vectZ"));
						System.out.println("coords point="+x+","+y+","+z);
						System.out.println("origine="+x0+","+y0+","+z0);
						System.out.println(TransformUtils.stringVector(vectCur, "vectCur"));
						System.out.println(TransformUtils.stringVector(vectorProj, "vectorProj"));
						System.out.println(TransformUtils.stringVector(difference, "difference"));						
						System.out.println("Norme = "+norme);						
					}

						
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (short)( 255 & 0xffff);
					}
				}
			}			
		}
		return img;
	}

	
	
	
	public static ImagePlus eraseBorder(ImagePlus imgIn) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		byte[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if( (x==0) || (x==xM-1) || (y==0) || (y==yM-1) || (z==0) || (z==zM-1) ) { 	
						valsImg[xM*y+x]=  (byte)(0 & 0xffff);
					}
				}
			}			
		}
		return img;
	}
	
	public static boolean isNullImage(ImagePlus imgIn) {
		if(imgIn.getType() == ImagePlus.GRAY16)return isNullShortImage(imgIn);
		if(imgIn.getType() == ImagePlus.GRAY32)return isNullFloatImage(imgIn);
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		int hit=0;
		byte[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if ((valsImg[xM*y+x] & 0xff )> 0)hit++; 
				}
			}			
		}
		return (hit<1);
	}

	
	public static double maxOfImage(ImagePlus img) {
		double max=0;
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		if(img.getType() == ImagePlus.GRAY8) {
			byte[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x] & 0xff )> max)max=(double)(valsImg[xM*y+x] & 0xff); 
					}
				}			
			}
		}
		if(img.getType() == ImagePlus.GRAY16) {
			short[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(short [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x] & 0xffff )> max)max=(double)(valsImg[xM*y+x] & 0xffff); 
					}
				}			
			}
		}
		if(img.getType() == ImagePlus.GRAY32) {
			float[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(float [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x])> max)max=(double)(valsImg[xM*y+x]); 
					}
				}			
			}
		}
		return max;	
	}

	
	public static boolean isNullShortImage(ImagePlus imgIn) {
		if(imgIn.getType() != ImagePlus.GRAY16)return false;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		int hit=0;
		short[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(short [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if (valsImg[xM*y+x] > 0) {
						hit++;
					//	System.out.println("Valeur supérieure :"+valsImg[xM*y+x]);hit++; 
					}
				}
			}			
		}
		//System.out.println("Hits="+hit);
		return (hit<1);
	}
	

	public static boolean isNullFloatImage(ImagePlus imgIn) {
		double epsilon=10E-10;
		if(imgIn.getType() != ImagePlus.GRAY32)return false;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		int hit=0;
		float[]valsImg;
		for(int z=0;z<zM;z++) {
			valsImg=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if (Math.abs(valsImg[xM*y+x]) > epsilon) {
						hit++;
					//	System.out.println("Valeur supérieure :"+valsImg[xM*y+x]);hit++; 
					}
				}
			}			
		}
		//System.out.println("Hits="+hit);
		return (hit<1);
	}

	
	
	
	public static ImagePlus getBinaryMask(ImagePlus img,double threshold) {
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		int type=(img.getType()==ImagePlus.GRAY8 ? 8 : img.getType()==ImagePlus.GRAY16 ? 16 : img.getType()==ImagePlus.GRAY32 ? 32 : 24);
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		if(type==8) {
			for(int z=0;z<dimZ;z++) {
				byte []tabImg=(byte[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xff) >= (byte)(((int)Math.round(threshold)) & 0xff)  )tabRet[dimX*y+x]=(byte)(255 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==16) {
			for(int z=0;z<dimZ;z++) {
				short []tabImg=(short[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x] & 0xffff) >= (short)(((int)Math.round(threshold)) & 0xffff)  )tabRet[dimX*y+x]=(byte)(255 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else if(type==32) {
			for(int z=0;z<dimZ;z++) {
				float []tabImg=(float[])img.getStack().getProcessor(z+1).getPixels();
				byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					for(int y=0;y<dimY;y++) {
						if( (tabImg[dimX*y+x]) >= threshold )tabRet[dimX*y+x]=(byte)(255 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else VitiDialogs.notYet("getBinary Mask type "+type);
		return ret;
	}

	public static ImagePlus getBinaryGrid(ImagePlus img,int pixelSpacing) {
		boolean doDouble=false;
		if(pixelSpacing<2)pixelSpacing=2;
		if(pixelSpacing>5)doDouble=true;
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		for(int z=0;z<dimZ;z++) {
			byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					if( (x%pixelSpacing==pixelSpacing/2) || 
					    (y%pixelSpacing==pixelSpacing/2)  ){
						tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}
					if( doDouble && (x%pixelSpacing==pixelSpacing/2+1) || 
						    (y%pixelSpacing==pixelSpacing/2+1)){
							tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}	
				}
			}
		}	
		return ret;
	}

	public static double meanValueofImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		int xMax=img.getWidth();
		int xm=(int)Math.round(x0-ray);
		int xM=(int)Math.round(x0+ray);
		int ym=(int)Math.round(y0-ray);
		int yM=(int)Math.round(y0+ray);
		if(z0<0)z0=0;
		if(z0>img.getStackSize()-1)z0=img.getStackSize()-1;

		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;

		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;
		double accumulator=0;
		double nbHits=0;
		if(img.getType() == ImagePlus.GRAY8) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					accumulator+= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
					nbHits++;
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					accumulator+= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
					nbHits++;
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					accumulator+=(float)valsImg[xMax*y+x];
					nbHits++;
				}
			}			
		}
		if(nbHits==0)return 0;
		else return (accumulator/nbHits);
	}

	public static double []valuesOfImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		int xMax=img.getWidth();
		int xm=(int)Math.round(x0-ray);
		int xM=(int)Math.round(x0+ray);
		int ym=(int)Math.round(y0-ray);
		int yM=(int)Math.round(y0+ray);
		if(z0<0)z0=0;
		if(z0>img.getStackSize()-1)z0=img.getStackSize()-1;

		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;

		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;

		int len=(xM-xm+1)*(yM-ym+1);
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]=(  (  (float)valsImg[xMax*y+x]));
				}
			}			
		}
		return ret;
	}
	
	public static ImagePlus multiplyFloatImages(ImagePlus img1,ImagePlus img2) {
		if(img1.getStackSize()!=img2.getStackSize()) {
			IJ.log("Image dimensions does not match");return null;
		}
		else {
			return (new ImageCalculator().run("Multiply create 32-bit stack", img1, img2) );	
		}
	}

	
	public static ImagePlus Sub222(ImagePlus img) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		res.setTransform(new ItkTransform());
		double []voxInit=VitimageUtils.getVoxelSizes(img);
		int []dimInit=VitimageUtils.getDimensions(img);
		for(int i=0;i<3;i++) {
			voxInit[i]*=2;
			dimInit[i]/=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return ItkImagePlusInterface.itkImageToImagePlus(res.execute(ItkImagePlusInterface.imagePlusToItkImage(img)));
	}

	
	public static ImagePlus Up222(ImagePlus img) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		res.setTransform(new ItkTransform());
		double []voxInit=VitimageUtils.getVoxelSizes(img);
		int []dimInit=VitimageUtils.getDimensions(img);
		for(int i=0;i<3;i++) {
			voxInit[i]/=2;
			dimInit[i]*=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return ItkImagePlusInterface.itkImageToImagePlus(res.execute(ItkImagePlusInterface.imagePlusToItkImage(img)));
	}

	public static ItkTransform Up222Dense(ItkTransform tr) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		Image img=new DisplacementFieldTransform((org.itk.simple.Transform)(tr)).getDisplacementField();
		int []dimInit=ItkImagePlusInterface.vectorUInt32ToIntArray(img.getSize());
		double []voxInit=ItkImagePlusInterface.vectorDoubleToDoubleArray(img.getSpacing());
		for(int i=0;i<3;i++) {
			voxInit[i]/=2;
			dimInit[i]*=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return new ItkTransform(new DisplacementFieldTransform( res.execute ( img )));
	}

	public static ItkTransform Sub222Dense(ItkTransform tr) {
		ResampleImageFilter res=new ResampleImageFilter();
		res.setDefaultPixelValue(0);
		Image img=new DisplacementFieldTransform((org.itk.simple.Transform)(tr)).getDisplacementField();
		int []dimInit=ItkImagePlusInterface.vectorUInt32ToIntArray(img.getSize());
		double []voxInit=ItkImagePlusInterface.vectorDoubleToDoubleArray(img.getSpacing());
		for(int i=0;i<3;i++) {
			voxInit[i]*=2;
			dimInit[i]/=2;
		}
		res.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(voxInit));
		res.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimInit));
		return new ItkTransform(new DisplacementFieldTransform( res.execute ( img )));
	}
	

	
	
	public static double []valuesOfBlock(ImagePlus img,int xm,int ym,int zm,int xM,int yM,int zM) {
		int xMax=img.getWidth();
		if(zm<0)zm=0;
		if(zM>img.getStackSize()-1)zM=img.getStackSize()-1;
		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;
		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;

		int len=(xM-xm+1)*(yM-ym+1)*(zM-zm+1);
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			for(int z=zm;z<=zM;z++) {
				byte[] valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			for(int z=zm;z<=zM;z++) {
				short[] valsImg=(short[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			for(int z=zm;z<=zM;z++) {
				float[] valsImg=(float[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]=(  (  (float)valsImg[xMax*y+x]));
					}
				}			
			}
		}
		return ret;
	}
	
	public static ImagePlus normalizeMeanAndVarianceAlongZ(ImagePlus imgTmp) {
		ImagePlus img=new Duplicator().run(imgTmp);
		int[]dims=VitimageUtils.getDimensions(img);
		double[][]stats=new double[dims[2]][2];
		int zMedUp=(dims[2]*6)/10;
		int zMedDown=(dims[2]*4)/10;
		for(int z=0;z<dims[2];z++) {
			if(z%30==0)System.out.print("  "+z+" / "+dims[2]);
			stats[z]=statistics1D(valuesOfBlockDouble(img,0,0,z,dims[0]-1,dims[1]-1,z));
		}
		System.out.println();
		int hits=0;
		double []statsZ=new double[2];
		for(int z=zMedDown;z<=zMedUp;z++) {
			statsZ[0]+=stats[z][0];
			statsZ[1]+=stats[z][1];
			hits++;
		}
		statsZ[0]/=hits;
		statsZ[1]/=hits;
		System.out.println("Moyenne et variance determinés : "+TransformUtils.stringVectorN(statsZ, ""));
		float[] valsImg;
		for(int z=0;z<dims[2];z++) {
			if(z%20==0) {
				System.out.print("  "+z+" / "+dims[2]);
			}
			valsImg=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dims[0];x++) {
				for(int y=0;y<dims[1];y++) {
					valsImg[dims[0]*y+x]=(float) ((float)(  ( (valsImg[dims[0]*y+x]) - stats[z][0]  )/stats[z][1] ) * statsZ[1] + statsZ[0]);
				}
			}			
		}
		System.out.println();
		return img;
	}

	

	
	public static ImagePlus normalizeCapillaryMeanAlongZ(ImagePlus imgTmp) {
		VitiDialogs.notYet("Normalize capillary along Z");
		return null;
	}

	

	
	
	
	
	public static double []valuesOfBlockDoubleSlice(ImagePlus img,double xxm,double yym,double xxM,double yyM) {
		int xMax=img.getWidth();
		if(xxm<0)xxm=0;
		if(yym<0)yym=0;
		if(xxm>=img.getWidth()-1)xxm=img.getWidth()-2;
		if(yym>=img.getHeight()-1)yym=img.getHeight()-2;
		if(xxM<0)xxM=0;
		if(yyM<0)yyM=0;
		if(xxM>=img.getWidth()-1)xxM=img.getWidth()-2;
		if(yyM>=img.getHeight()-1)yyM=img.getHeight()-2;
		int len=(int)Math.round(xxM-xxm+1)*(int)Math.round(yyM-yym+1);

		int xm=(int)Math.floor(xxm);
		int ym=(int)Math.floor(yym);
		int xM=(int)Math.floor(xxM);
		int yM=(int)Math.floor(yyM);
		double xp=xxm-xm;
		double yp=yym-ym;
		double []factors= new double[]{ (1-xp)*(1-yp) ,  (xp)*(1-yp) , (1-xp)*(yp) ,     (xp)*(yp)  	};
		//System.out.println(TransformUtils.stringVectorN(factors,"factors"));
		//si xp=0 , 1 3 5 7 n influent pas
		//si xp=1 , 0 2 4 6 n influent pas
		//si yp=0 , 2 3 6 7 n influent pas
		//si yp=1 , 0 1 4 5 n influent pas
		//si zp=0 , 4 5 6 7 n influent pas
		//si zp=1 , 0 1 2 3 n influent pas	
		//    Z axis
		//     /|\ 6---------7         
		//      | /|        /|
		//      |/ |       / |  
		//      4---------5  |
		//      |  |      |  |
		//      |  2------|--3
		//      | /       | /
		//      |/        |/
		//      0---------1-----> X axis
		//
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			byte[]valsImg=(byte [])img.getStack().getProcessor(1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					//System.out.println("("+x+","+y+","+z+")");
					ret[hit++]= factors[0]*(int)(  (  (byte)valsImg[xMax*y+x])  & 0xff) + factors[1]*(int)(  (  (byte)valsImg[xMax*y+(x+1)])  & 0xff) +
								factors[2]*(int)(  (  (byte)valsImg[xMax*(y+1)+x])  & 0xff) + factors[3]*(int)(  (  (byte)valsImg[xMax*(y+1)+(x+1)])  & 0xff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short [])img.getStack().getProcessor(1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					ret[hit++]= factors[0]*(int)(  (  (short)valsImg[xMax*y+x])  & 0xffff) + factors[1]*(int)(  (  (short)valsImg[xMax*y+(x+1)])  & 0xffff) +
							factors[2]*(int)(  (  (short)valsImg[xMax*(y+1)+x])  & 0xffff) + factors[3]*(int)(  (  (short)valsImg[xMax*(y+1)+(x+1)])  & 0xffff);
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float [])img.getStack().getProcessor(1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					ret[hit++]= factors[0]*  (float)valsImg[xMax*y+x] + factors[1]* (float)valsImg[xMax*y+(x+1)] +
							factors[2]*(float)valsImg[xMax*(y+1)+x] + factors[3]*(float)valsImg[xMax*(y+1)+(x+1)] ;
				}
			}			
		}
		return ret;
	}
	
	
	
	
	public static double []valuesOfBlockDouble(ImagePlus img,double xxm,double yym,double zzm,double xxM,double yyM,double zzM) {
		int zImg=img.getStackSize();
		int xMax=img.getWidth();

		if(zzm<0)zzm=0;
		if(zzm>=img.getStackSize()-1)zzM=img.getStackSize()-2;
		if(zzM<0)zzm=0;
		if(zzM>=img.getStackSize()-1)zzM=img.getStackSize()-2;

		if(xxm<0)xxm=0;
		if(yym<0)yym=0;
		if(xxm>=img.getWidth()-1)xxm=img.getWidth()-2;
		if(yym>=img.getHeight()-1)yym=img.getHeight()-2;
		if(xxM<0)xxM=0;
		if(yyM<0)yyM=0;
		if(xxM>=img.getWidth()-1)xxM=img.getWidth()-2;
		if(yyM>=img.getHeight()-1)yyM=img.getHeight()-2;


		int len=(int)Math.round(xxM-xxm+1)*(int)Math.round(yyM-yym+1)*(int)Math.round(zzM-zzm+1);

		int xm=(int)Math.floor(xxm);
		int ym=(int)Math.floor(yym);
		int zm=(int)Math.floor(zzm);
		int xM=(int)Math.floor(xxM);
		int yM=(int)Math.floor(yyM);
		int zM=(int)Math.floor(zzM);
		double xp=xxm-xm;
		double yp=yym-ym;
		double zp=zzm-zm;
		double []factors= new double[]{ (1-xp)*(1-yp)*(1-zp) ,  (xp)*(1-yp)*(1-zp) , (1-xp)*(yp)*(1-zp) ,     (xp)*(yp)*(1-zp)  ,
									    (1-xp)*(1-yp)*(zp) ,  (xp)*(1-yp)*(zp) , (1-xp)*(yp)*(zp) ,     (xp)*(yp)*(zp) 	};
		//System.out.println(TransformUtils.stringVectorN(factors,"factors"));
		//si xp=0 , 1 3 5 7 n influent pas
		//si xp=1 , 0 2 4 6 n influent pas
		//si yp=0 , 2 3 6 7 n influent pas
		//si yp=1 , 0 1 4 5 n influent pas
		//si zp=0 , 4 5 6 7 n influent pas
		//si zp=1 , 0 1 2 3 n influent pas	
		//    Z axis
		//     /|\ 6---------7         
		//      | /|        /|
		//      |/ |       / |  
		//      4---------5  |
		//      |  |      |  |
		//      |  2------|--3
		//      | /       | /
		//      |/        |/
		//      0---------1-----> X axis
		//
		int hit=0;
		double[] ret=new double[len];
		if(img.getType() == ImagePlus.GRAY8) {
			byte[][] valsImg=new byte[zImg][];
			for(int z=0;z<zImg;z++)valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int z=zm;z<=zM;z++) {
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						//System.out.println("("+x+","+y+","+z+")");
						ret[hit++]= factors[0]*(int)(  (  (byte)valsImg[z][xMax*y+x])  & 0xff) + factors[1]*(int)(  (  (byte)valsImg[z][xMax*y+(x+1)])  & 0xff) +
									factors[2]*(int)(  (  (byte)valsImg[z][xMax*(y+1)+x])  & 0xff) + factors[3]*(int)(  (  (byte)valsImg[z][xMax*(y+1)+(x+1)])  & 0xff) +
									factors[4]*(int)(  (  (byte)valsImg[z+1][xMax*y+x])  & 0xff) + factors[5]*(int)(  (  (byte)valsImg[z+1][xMax*y+(x+1)])  & 0xff) +
									factors[6]*(int)(  (  (byte)valsImg[z+1][xMax*(y+1)+x])  & 0xff) + factors[7]*(int)(  (  (byte)valsImg[z+1][xMax*(y+1)+(x+1)])  & 0xff) ;
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[][] valsImg=new short[zImg][];
			for(int z=0;z<zImg;z++)valsImg[z]=(short [])img.getStack().getProcessor(z+1).getPixels();
			for(int z=zm;z<=zM;z++) {
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						ret[hit++]= factors[0]*(int)(  (  (short)valsImg[z][xMax*y+x])  & 0xffff) + factors[1]*(int)(  (  (short)valsImg[z][xMax*y+(x+1)])  & 0xffff) +
								factors[2]*(int)(  (  (short)valsImg[z][xMax*(y+1)+x])  & 0xffff) + factors[3]*(int)(  (  (short)valsImg[z][xMax*(y+1)+(x+1)])  & 0xffff) +
								factors[4]*(int)(  (  (short)valsImg[z+1][xMax*y+x])  & 0xffff) + factors[5]*(int)(  (  (short)valsImg[z+1][xMax*y+(x+1)])  & 0xffff) +
								factors[6]*(int)(  (  (short)valsImg[z+1][xMax*(y+1)+x])  & 0xffff) + factors[7]*(int)(  (  (short)valsImg[z+1][xMax*(y+1)+(x+1)])  & 0xffff) ;
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[][] valsImg=new float[zImg][];
			for(int z=0;z<zImg;z++)valsImg[z]=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int z=zm;z<=zM;z++) {
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						ret[hit++]= factors[0]*  (float)valsImg[z][xMax*y+x] + factors[1]* (float)valsImg[z][xMax*y+(x+1)] +
								factors[2]*(float)valsImg[z][xMax*(y+1)+x] + factors[3]*(float)valsImg[z][xMax*(y+1)+(x+1)] +
								factors[4]*(float)valsImg[z+1][xMax*y+x] + factors[5]*(float)valsImg[z+1][xMax*y+(x+1)] +
								factors[6]*(float)valsImg[z+1][xMax*(y+1)+x] + factors[7]*(float)valsImg[z+1][xMax*(y+1)+(x+1)] ;
					}
				}			
			}
		}
		return ret;
	}
	
	
	public static ImagePlus setImageToValue(ImagePlus imgIn,double value) {
		if(imgIn.getType()==ImagePlus.GRAY32)return set32bitToValue(imgIn,value);
		if(imgIn.getType()==ImagePlus.GRAY16)return set16bitToValue(imgIn,(int)Math.round(value));
		if(imgIn.getType()==ImagePlus.GRAY8)return set8bitToValue(imgIn,(int)Math.round(value));
		return null;
	}
	
	
	
	public static ImagePlus set8bitToValue(ImagePlus imgIn,int value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}
	
	public static ImagePlus set16bitToValue(ImagePlus imgIn,int value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}

	public static ImagePlus set32bitToValue(ImagePlus imgIn,double value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}

	public static double[] stdAndMeanValueofImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		int xMax=img.getWidth();
		int xm=(int)Math.round(x0-ray);
		int xM=(int)Math.round(x0+ray);
		int ym=(int)Math.round(y0-ray);
		int yM=(int)Math.round(y0+ray);
		if(z0<0)z0=0;
		if(z0>img.getStackSize()-1)z0=img.getStackSize()-1;
		if(xm<0)xm=0;
		if(ym<0)ym=0;
		if(xm>img.getWidth()-1)xm=img.getWidth()-1;
		if(ym>img.getHeight()-1)ym=img.getHeight()-1;
		if(xM<0)xM=0;
		if(yM<0)yM=0;
		if(xM>img.getWidth()-1)xM=img.getWidth()-1;
		if(yM>img.getHeight()-1)yM=img.getHeight()-1;
		double mean=meanValueofImageAround(img,x0,y0,z0,ray);
		double accumulator=0;
		double nbHits=0;
		if(img.getType() == ImagePlus.GRAY8) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
						accumulator+= Math.pow( ((double)(  (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff) )) - mean , 2);
						nbHits++;
					}			
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			short[] valsImg=(short[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
						accumulator+= Math.pow( ((double)(  (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff) )) - mean , 2);
						nbHits++;
					}			
				}
			}			
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			float[] valsImg=(float[])img.getStack().getProcessor(z0+1).getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
						accumulator+=Math.pow(   (   (float)valsImg[xMax*y+x] ) - mean , 2 );
						nbHits++;
					}			
				}
			}			
		}
		if(nbHits==0)return new double[] {0,0};
		return new double[] { mean, Math.sqrt(accumulator/nbHits)};
	}

	public static double[] statistics1D(double[] vals){
		double accumulator=0;
		int hits=0;
		for(int i=0;i<vals.length ;i++) {accumulator+=vals[i];hits++;}
		double mean=(accumulator/hits);
		accumulator=0;
		for(int i=0;i<vals.length ;i++) accumulator+=Math.pow(vals[i]-mean,2);
		double std=Math.sqrt(accumulator/hits);	
		return (new double[] {mean,std});
	}

	public static double[] statistics2D(double[][] vals){
		double accumulator=0;
		int hits=0;
		for(int i=0;i<vals.length ;i++)for(int j=0;j<vals[i].length;j++) {accumulator+=vals[i][j];hits++;}
		double mean=(accumulator/hits);
		accumulator=0;
		for(int i=0;i<vals.length ;i++)for(int j=0;j<vals[i].length;j++) accumulator+=Math.pow(vals[i][j]-mean,2);
		double std=Math.sqrt(accumulator/hits);	
		return (new double[] {mean,std});
	}

	public static ImagePlus removeCapillaryFromRandomMriImage(ImagePlus imgIn) {
		ImagePlus rec=new Duplicator().run(imgIn);	
		ImagePlus img=new Duplicator().run(imgIn);	
		rec=VitimageUtils.gaussianFiltering(rec, 0.3,0.3, 1.0);	
		double val=VitimageUtils.maxOfImage(rec);
		System.out.println("Max detecté ="+val);
		IJ.run(img,"32-bit","");
		IJ.run(img,"Divide...","value="+val+" stack");
		img.getProcessor().resetMinAndMax();
		ImagePlus ret=VitimageUtils.removeCapillaryFromHyperImageForRegistration(img);
		IJ.run(ret,"Multiply...","value="+val+" stack");
		return ret;
	}

	public static ImagePlus removeCapillaryFromHyperImageForRegistration(ImagePlus imgInit) {
		double sigmaFilter=0.3;
		ImagePlus img=new Duplicator().run(imgInit);
		ImagePlus img2=new Duplicator().run(imgInit);
		IJ.run(img2,"Multiply...","value=1000 stack");
		ImagePlus imgSliceInput;
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double diamCap=0.7;
		double valThresh=200;
		double x0RoiCap;
		double y0RoiCap;
		RoiManager rm=RoiManager.getRoiManager();
		img2=VitimageUtils.gaussianFiltering(img2, sigmaFilter,sigmaFilter, sigmaFilter*3);
		img2=VitimageUtils.connexe(img2, valThresh, 10E10, 0, 10E10,6,2,true);
		IJ.run(img2,"8-bit","");
		ImageStack isRet=new ImageStack(img2.getWidth(),img.getHeight(),img.getStackSize());
		for(int z=1;z<=zMax;z++) {
			ImagePlus imgSlice=new ImagePlus("", img2.getStack().getProcessor(z));
			imgSlice.getProcessor().setMinAndMax(0,255);
			IJ.setThreshold(imgSlice, 255,255);
			for(int dil=0;dil<(diamCap/img.getCalibration().pixelWidth);dil++) IJ.run(imgSlice, "Dilate", "stack");
			//VitimageUtils.imageChecking(imgSlice,"After Dil");
			if(VitimageUtils.isNullImage(imgSlice)) {
				imgSliceInput=new ImagePlus("", img.getStack().getProcessor(z));
				if(imgSliceInput.getType()==ImagePlus.GRAY32) {
					isRet.setProcessor(imgSliceInput.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY16) {
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY8) {
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else IJ.log("Remove capillary : image type not handled ("+imgSliceInput.getType()+")");	
			}
			else {
				rm.reset();
				Roi capArea=new ThresholdToSelection().convert(imgSlice.getProcessor());	
				rm.add(imgSlice, capArea, 0);							
				FloatPolygon tabPoly=capArea.getFloatPolygon();
				Rectangle rect=tabPoly.getBounds();
				int xMinRoi=(int) (rect.getX());
				int yMinRoi=(int) (rect.getY());
				int xSizeRoi=(int) (rect.getWidth());
				int ySizeRoi=(int) (rect.getHeight());
				int xMaxRoi=xMinRoi+xSizeRoi;
				int yMaxRoi=yMinRoi+ySizeRoi;				
				imgSliceInput=new ImagePlus("", img.getStack().getProcessor(z));
				if(imgSliceInput.getType()==ImagePlus.GRAY32) {
					float[] valsImg=(float[])(imgSliceInput).getProcessor().getPixels();
					//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
					for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
					isRet.setProcessor(imgSliceInput.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY16) {
					short[] valsImg=(short[])(imgSliceInput).getProcessor().getPixels();
					//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
					for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else if(imgSliceInput.getType()==ImagePlus.GRAY8) {
					byte[] valsImg=(byte[])(imgSliceInput).getProcessor().getPixels();
					//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
					for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
					isRet.setProcessor(imgSlice.getProcessor(),z);
				}
				else IJ.log("Remove capillary : image type not handled ("+imgSliceInput.getType()+")");
			}
		}
		ImagePlus res=new ImagePlus("Result_"+img.getShortTitle()+"_no_cap.tif",isRet);
		VitimageUtils.adjustImageCalibration(res,img);
		return res;	
	}
	
	
	
	public static double[]getVoxelSizes(ImagePlus img){
		return new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
	}
	
	public static int[]getDimensions(ImagePlus img){
		return new int[] {img.getWidth(),img.getHeight(),img.getStackSize()};
	}
	
	public static double dou(double d){
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}
	
	public static ImagePlus[]stacksFromHyperstack(ImagePlus hyper,int nb){
		ImagePlus []ret=new ImagePlus[nb];
		for(int i=0;i<nb;i++) {
			IJ.run(hyper,"Make Substack...","slices=1-"+(hyper.getStackSize()/nb)+" frames="+(i+1)+"-"+(i+1)+"");
			ret[i]=IJ.getImage();
			ret[i].hide();
			VitimageUtils.adjustImageCalibration(ret[i],hyper);
		}
		return ret;
	}
	
	
	public static void imageChecking(ImagePlus imgInit,double sliMin,double sliMax,int periods,String message,double totalDuration,boolean fluidVisu) {
		int minFrameRateForVisualConfort=33;
		int maxDurationForVisualConfort=1000/minFrameRateForVisualConfort;
		if (imgInit==null)return;
		ImagePlus img=new Duplicator().run(imgInit,1,imgInit.getStackSize());
		img.getProcessor().resetMinAndMax();
		String titleOld=img.getTitle();
		String str;
		if (message.compareTo("")==0)str=titleOld;
		else str=message;
		img.setTitle(str);
		int sliceMin=(int)Math.round(sliMin);
		int sliceMax=(int)Math.round(sliMax);
		int miniDuration=0;
		if(periods<1)periods=1;
		if(sliceMin<1)sliceMin=1;
		if(sliceMin>img.getStackSize())sliceMin=img.getStackSize();
		if(sliceMax<1)sliceMax=1;
		if(sliceMax>img.getStackSize())sliceMax=img.getStackSize();
		if(sliceMin>sliceMax)sliceMin=sliceMax;
		img.show();
		if(img.getType() != ImagePlus.COLOR_RGB)IJ.run(img,"Fire","");
		if(sliceMin==sliceMax) {
			miniDuration=(int)Math.round(1000.0*totalDuration/periods);
			img.setSlice(sliceMin);
			for(int i=0;i<periods;i++)
				waitFor(miniDuration);
			return;
		}
		else {
			miniDuration=(int)Math.round(totalDuration*1000.0/periods/(sliceMax-sliceMin+10));
			while(fluidVisu && miniDuration>maxDurationForVisualConfort) {
				periods++;
				miniDuration=(int)Math.round(totalDuration*1000.0/periods/(sliceMax-sliceMin+10));				
			}
			int curSlice=(sliceMin+sliceMax)/2;
			for(int j=0;j<5 ;j++)waitFor(miniDuration);
			img.setSlice((sliceMin+sliceMax/2));
			for(int i=0;i<periods;i++) {
				while (curSlice>sliceMin) {
					img.setSlice(--curSlice);
					waitFor(miniDuration);
				}
				for(int j=0;j<(sliMax-sliMin)/8 ;j++)waitFor(miniDuration);
				while (curSlice<sliceMax) {
					img.setSlice(++curSlice);
					waitFor(miniDuration);
				}
				for(int j=0;j<5 ;j++)waitFor(miniDuration);
			}
		}
		img.close();
	}
	public static void imageChecking(ImagePlus img,String message,double totalDuration) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,totalDuration,true);
	}
	public static void imageCheckingFast(ImagePlus img,String message) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),2,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,2,true);
	}

	public static void imageChecking(ImagePlus img,String message) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),4,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,4,true);
	}
	public static void imageChecking(ImagePlus img,double totalDuration) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration,true);
	}
	public static void imageChecking(ImagePlus img) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),3,true);
	}
	
	public static void waitFor(int n) {
		try {
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public static ImagePlus gaussianFilteringIJ(ImagePlus imgIn,double sigmaX,double sigmaY,double sigmaZ) {
		ImagePlus img=new Duplicator().run(imgIn);
		double []voxSizes=VitimageUtils.getVoxelSizes(imgIn);
		double sigX=sigmaX/voxSizes[0];
		double sigY=sigmaY/voxSizes[1];
		double sigZ=sigmaZ/voxSizes[2];
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigX+" y="+sigY+" z="+sigZ);		
		return img;
	}
	
	
	public static ImagePlus gaussianFiltering(ImagePlus imgIn,double sigmaX,double sigmaY,double sigmaZ) {
		Image img=ItkImagePlusInterface.imagePlusToItkImage(imgIn);
		
		/*
		DiscreteGaussianImageFilter gaussFilter=new DiscreteGaussianImageFilter();
		VectorDouble var=new VectorDouble(3);
		var.set(0,sigmaX*sigmaX);
		var.set(1,sigmaY*sigmaY);
		var.set(2,sigmaZ*sigmaZ);
		VectorDouble err=new VectorDouble(3);
		err.set(0,0.01);
		err.set(1,0.01);
		err.set(2,0.01);
		img=gaussFilter.execute(img,var,50, err,true);
		*/
	
		RecursiveGaussianImageFilter gaussFilter=new RecursiveGaussianImageFilter();
		if(imgIn.getWidth()>=4 && sigmaX>0) {
			gaussFilter.setDirection(0);
			gaussFilter.setSigma(sigmaX);
			img=gaussFilter.execute(img);
		}
		else System.out.println("Gaussian filterin : no work with X");
		if(imgIn.getHeight()>=4 && sigmaY>0) {
			gaussFilter.setDirection(1);
			gaussFilter.setSigma(sigmaY);
			img=gaussFilter.execute(img);
		}
		else System.out.println("Gaussian filterin : no work with Y");
		if(imgIn.getStackSize()>=4 && sigmaZ>0) {
			gaussFilter.setDirection(2);
			gaussFilter.setSigma(sigmaZ);
			img=gaussFilter.execute(img);
		}
		else System.out.println("Gaussian filterin : no work with Z");
		
		return ItkImagePlusInterface.itkImageToImagePlus(img);
	}
	
	public static ImagePlus getTestImage(String title) {
		System.out.println("Ouverture d image test : "+System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
		return IJ.openImage(System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
	}
	
	public static void saveTestResult(ImagePlus img, String title) {
		System.out.println("Sauvegarde image resultat : "+System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
		IJ.saveAsTiff(img, System.getProperty("user.dir")+slash+"src/test/imgs"+slash+title);
	}
	
	
	/** Create a Thread[] array as large as the number of processors available. 
	 * From Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static Thread[] newThreadArray(int n) {  
		int n_cpus = Runtime.getRuntime().availableProcessors();  
		return new Thread[n];  
	}  

	/** Start all given threads and wait on each of them until all are done. 
	 * From Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static void startAndJoin(Thread[] threads){  
		for (int ithread = 0; ithread < threads.length; ++ithread){  
			threads[ithread].setPriority(Thread.NORM_PRIORITY);  
			threads[ithread].start();  
		}
		try{     
			for (int ithread = 0; ithread < threads.length; ++ithread)  
				threads[ithread].join();  
		} catch (InterruptedException ie) {  	System.out.println(ie.getStackTrace());throw new RuntimeException(ie);  }  
	}   

	public static String[] stringArraySort(String[]tabStr) {
		String[]tabRet=new String[tabStr.length];
		ArrayList<String> listStr=new ArrayList<String>();
		for(String str : tabStr)listStr.add(str);
		Collections.sort(listStr);
		for(int i=0;i<listStr.size();i++)tabRet[i]=listStr.get(i);
		return tabRet;
	}

	class Vitimage4DComparator implements java.util.Comparator {
	   public int compare(Object o1, Object o2) {
	      return ( new Integer( ((Vitimage4D) o1).dayAfterExperience) .compareTo(new Integer( ((Vitimage4D) o2).dayAfterExperience)));
	   }
	}

		
	public static ImagePlus makeWekaSegmentation(ImagePlus imgToSegment,String pathToClassifier) {
		WekaSegmentation weka= new WekaSegmentation(imgToSegment);
		weka.loadClassifier(pathToClassifier);
		weka.loadNewImage(imgToSegment);
//		weka.fea
		weka.applyClassifier(false);
		ImagePlus res=weka.getClassifiedImage();
		VitimageUtils.adjustImageCalibration(res,imgToSegment);
		return res;
	}

	
	public static Point3d coordinatesOfObjectInSlice(ImagePlus img,int slice,boolean realCoords){
		double []voxSize=VitimageUtils.getVoxelSizes(img);
		if(img.getType() != ImagePlus.GRAY8) System.out.println("Erreur in coordinatesOfObjectInSlice: image "+img.getTitle()+" should be 8 bit");
		if(slice>=img.getStackSize() || slice<0)System.out.println("Erreur in coordinatesOfObjectInSlice: image "+img.getTitle()+" was asked for slice number "+slice);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int nbHits=0;
		double coords[]=new double[3];
		byte[] valsImg=(byte [])img.getStack().getProcessor(slice+1).getPixels();
		for(int x=0;x<xM;x++)for(int y=0;y<yM;y++)if (  (int)(  (  (byte)valsImg[xM*y+x])  & 0xff)  == 255) {
			nbHits++;
			coords[0]+=x;
			coords[1]+=y;
			coords[2]+=slice;
		}			
		for(int dim=0;dim<3;dim++) {
			coords[dim]/=nbHits;
			if(realCoords)coords[dim]*=voxSize[dim];
		}
		return new Point3d(coords[0],coords[1],coords[2]);
	}
	public static Point3d coordinatesOfObject(ImagePlus img,boolean realCoords){
		double []voxSize=VitimageUtils.getVoxelSizes(img);
		if(img.getType() != ImagePlus.GRAY8) System.out.println("Erreur in coordinatesOfObjectInSlice: image "+img.getTitle()+" should be 8 bit");
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		int nbHits=0;
		double coords[]=new double[3];
		for(int z=0;z<zM;z++) {
			byte[] valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++)for(int y=0;y<yM;y++)if (  (int)(  (  (byte)valsImg[xM*y+x])  & 0xff)  == 255) {
				nbHits++;
				coords[0]+=x;
				coords[1]+=y;
				coords[2]+=z;
			}		
		}
		for(int dim=0;dim<3;dim++) {
			coords[dim]/=nbHits;
			if(realCoords)coords[dim]*=voxSize[dim];
		}
		return new Point3d(coords[0],coords[1],coords[2]);
	}


}
