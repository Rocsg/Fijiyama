package com.vitimage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.itk.simple.Image;
import org.itk.simple.OtsuThresholdImageFilter;
import org.itk.simple.PixelIDValueEnum;
import org.itk.simple.RecursiveGaussianImageFilter;
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
import ij.plugin.Duplicator;
import ij.plugin.GaussianBlur3D;
import ij.plugin.HyperStackReducer;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Binary;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import imagescience.transform.Transform;
import math3d.Point3d;

public interface VitimageUtils {
	public static void main(String[]args) {
		/*Point3d[] pInit=new Point3d[3];
		Point3d[] pFin=new Point3d[3];
		Acquisition mriT1=MRI_T1_Seq.getTestAcquisitionMriT1();
		mriT1.getTransformedRegistrationImage().show();

		System.out.println("\n\n\nTEST SEQUENCE 1 : DETECT AND CHANGE AXIS");
		pFin=VitimageUtils.detectAxis(mriT1);
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
		case "MRI_GE3D_SEQ": return AcquisitionType.MRI_GE3D_SEQ;
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

	
	public enum AcquisitionType{
		MRI_SE_SEQ,
		MRI_T1_SEQ,
		MRI_T2_SEQ,
		MRI_GE3D_SEQ,
		MRI_DIFF_SEQ,
		MRI_FLIPFLOP_SEQ,
		RX,
		HISTOLOGY,
		PHOTOGRAPH,
		TERAHERTZ
	}

	public enum SupervisionLevel{
		AUTONOMOUS,
		GET_INFORMED,
		ASK_FOR_ALL
	}	

	public enum ConsoleOutput{
		MUTE,
		VERBOSE
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
		double xMoyDown=0,yMoyDown=0,zMoyDown=0;
		int hitsUp=0,hitsDown=0;

		//Step 1 : apply gaussian filtering and convert to 8 bits
		GaussianBlur3D.blur(img,10*0.035/vX,10*0.035/vY,2*0.5/vZ);
		img.getProcessor().resetMinAndMax();
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];
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
		imageChecking(imgUp);
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,true);
		imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,true);
		imageChecking(imgDownCon,"imgDownCon");

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
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,255, 0,10E10, 6, 1,false);//L objet
		ImagePlus imgMask2=VitimageUtils.connexe(imgMask0,1,255, 0,10E10, 6, 2,false);//le cap
		IJ.run(imgMask1,"8-bit","");
		IJ.run(imgMask2,"8-bit","");
		ImageCalculator ic=new ImageCalculator();
		ImagePlus imgMask3=ic.run("OR create stack", imgMask1,imgMask2);							
		return imgMask3;
	}


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
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,255, nbThreshObjects*voxVolume,10E10, 26, 0,false);//L objet
		ImagePlus imgMask2=VitimageUtils.getBinaryMask(imgMask1,1);
		return imgMask2;
	}
	
	
	
	public static Point3d[] detectAxis(Acquisition acq){
		ImagePlus img=acq.getTransformedRegistrationImage();
		imageChecking(img,"Start detect axis type "+acq.getAcquisitionType());
		boolean debug=false;
		int xMax=acq.dimX();
		int yMax=acq.dimY();
		int zMax=acq.dimZ();
		double vX=acq.voxSX();
		double vY=acq.voxSY();
		double vZ=acq.voxSZ();
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=0,yMoyDown=0,zMoyDown=0;
		int hitsUp=0,hitsDown=0;

		//Step 1 : apply gaussian filtering and convert to 8 bits
		GaussianBlur3D.blur(img,10*0.035/vX,10*0.035/vY,2*0.5/vZ);
		img.getProcessor().resetMinAndMax();
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
		imageChecking(img,"fin step1 ");

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];
		if(acq.acquisitionType != AcquisitionType.MRI_T2_SEQ) {
			
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
		imageChecking(imgUp);
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2,true);
		imageChecking(imgUpCon,"imgUpCon");
		
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		imageChecking(imgDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2,true);
		imageChecking(imgDownCon,"imgDownCon");

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
	
	
	
	
	

	public static Point3d[] detectInoculationPointGuidedByAutomaticComputedOutline(ImagePlus img,ImagePlus maskForOutline) {
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
		imgSlice=connexe(imgSlice,255,255,0,10E10,8,1,true);
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
		imgSlice=connexe(imgSlice,255,255,0,10E10,8,1,true);
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


	public static ImagePlus compositeOf(ImagePlus img1Source,ImagePlus img2Source){
		//	return(RGBStackMerge.mergeChannels(new ImagePlus[] {img1,img2},false));
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		img1.getProcessor().resetMinAndMax();
		img2.getProcessor().resetMinAndMax();
		IJ.run(img1,"8-bit","");
		IJ.run(img2,"8-bit","");
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		return new ImagePlus("Composite",is);
	}

	public static ImagePlus compositeOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeOf(img1,img2);
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


	public static double getVoxelVolume(ImagePlus img) {
		return img.getCalibration().pixelDepth*img.getCalibration().pixelWidth*img.getCalibration().pixelHeight;
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

	
	
	public static ImagePlus getBinaryMask(ImagePlus img,double threshold) {
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		double[]voxSizes=new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
		
		int type=(img.getType()==ImagePlus.GRAY8 ? 8 : img.getType()==ImagePlus.GRAY16 ? 16 : img.getType()==ImagePlus.GRAY32 ? 32 : 24);
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		Calibration cal = ret.getCalibration();
		cal.setUnit("mm");cal.pixelWidth =voxSizes[0]; cal.pixelHeight =voxSizes[1]; cal.pixelDepth =voxSizes[2];
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

	
	
	
	public static double voxelVolume(ImagePlus img) {
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		return vX*vY*vZ;
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
		int[][][]tabIn=new int[xMax][yMax][zMax];
		int[][]connexions=new int[xMax*yMax*zMax*3][2];
		int[]volume=new int[xMax*yMax*zMax];
		int[][]neighbours=new int[][]{{1,0,0,0},{1,1,0,0},{0,1,0,0},{0,0,1,0},{1,0,1,0},{1,1,1,0},{0,1,1,0} };
		int curComp=0;
		int indexConnexions=0;
		if(debug)System.out.println("Choix d'un type");
		switch(img.getType()) {
		case ImagePlus.GRAY8:
			byte[] imgInB;
			for(int z=0;z<zMax;z++) {
				imgInB=(byte[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInB[x+xMax*y] & 0xff) < threshHigh+1 )  && ((float)((imgInB[x+xMax*y]) & 0xff) > threshLow-1) )tabIn[x][y][z]=-1;
			}
			break;
		case ImagePlus.GRAY16:
			short[] imgInS;
			for(int z=0;z<zMax;z++) {
				imgInS=(short[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInS[x+xMax*y] & 0xffff) < threshHigh+1 )  && ((float)((imgInS[x+xMax*y]) & 0xffff) > threshLow-1) )tabIn[x][y][z]=-1;					
			}
			break;
		case ImagePlus.GRAY32:
			float[] imgInF;
			for(int z=0;z<zMax;z++) {
				imgInF=(float[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((imgInF[x+xMax*y]) < threshHigh+1 )  && (((imgInF[x+xMax*y])) > threshLow-1) )tabIn[x][y][z]=-1;					
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

	public static void testConnexe(ImagePlus img,int thresh,int volMin,int con) {
		ImagePlus out=connexe(img,thresh,10E32,volMin,1000000000,con,0,true);
		out.show();
		//		IJ.setMinAndMax(min, max);
	}

	public static void testResolve() {
		int[][]connexions=new int[][]{{1,2},{3,4},{4,5},{2,6},{1,1}};
		int nbCouples=5;
		int n=7;
		int []volumes= {0,20,40,80,160,320,640};
		int volMin = 100;
		int volMax = 10000;
		int[]result=resolveConnexitiesGroupsAndExclude(connexions,nbCouples,n,volumes,volMin,volMax,0,true);
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
		else imageChecking(img,0,img.getStackSize()-1,1,message,4,true);
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
	
	public static ImagePlus gaussianFiltering(ImagePlus imgIn,double sigmaX,double sigmaY,double sigmaZ) {
		Image img=ItkImagePlusInterface.imagePlusToItkImage(imgIn);
		RecursiveGaussianImageFilter gaussFilter=new RecursiveGaussianImageFilter();
		if(imgIn.getWidth()>=4 && sigmaX>0) {
			gaussFilter.setDirection(0);
			gaussFilter.setSigma(sigmaX);
			img=gaussFilter.execute(img);
		}
		if(imgIn.getHeight()>=4 && sigmaY>0) {
			gaussFilter.setDirection(1);
			gaussFilter.setSigma(sigmaY);
			img=gaussFilter.execute(img);
		}
		if(imgIn.getStackSize()>=4 && sigmaZ>0) {
			gaussFilter.setDirection(2);
			gaussFilter.setSigma(sigmaZ);
			img=gaussFilter.execute(img);
		}
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
		} catch (InterruptedException ie) {  	throw new RuntimeException(ie);  }  
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



}
