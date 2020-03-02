package com.vitimage.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

import org.itk.simple.Image;
import org.itk.simple.OtsuThresholdImageFilter;
import org.itk.simple.RecursiveGaussianImageFilter;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import math3d.Point3d;

public interface VitimageUtils {
	public final static double EPSILON=0.00001;
	public static int OP_ADD=1;
	public static int OP_MULT=2;
	public static int OP_DIV=3;
	public static int OP_SUB=4;
	public static int OP_MEAN=5;
	public final static String slash=File.separator;
	public static final int ERROR_VALUE=-1;
	public static final int COORD_OF_MAX_IN_TWO_LAST_SLICES=1;
	public static final int COORD_OF_MAX_ALONG_Z=2;

	public static void main(String[]args) {
		System.out.println("File compiled");
	}

	
	
	/*Informations about system and virtual machine. Detection of Windows or Unix environments*/
	public static String getSystemName(){
		String os=System.getProperty("os.name").toLowerCase();
		if(os.indexOf("win") >= 0)return "Windows system";
		if(os.indexOf("mac") >= 0)return "Mac iOs system";
		if(os.indexOf("nux") >= 0)return "Linux system";
		return "System";
	}

	public static boolean isWindowsOS(){
		String os=System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	public static String getSystemNeededMemory(){
		String os=System.getProperty("os.name").toLowerCase();
		if(os.indexOf("win") >= 0)return "3000 MB";
		if(os.indexOf("mac") >= 0)return "3000 MB";
		if(os.indexOf("nux") >= 0)return "3000 MB";
		return "System";
	}
		
	public static int getNbCores() {
		return	Runtime.getRuntime().availableProcessors();
	}
	
		
	
	
	
	
	/*Adjust position of frames and images on the screen*/
	public static void adjustImageOnScreen(ImagePlus img,int xPosition,int yPosition) {
		adjustFrameOnScreen(img.getWindow(), xPosition, yPosition);
	}

	public static void adjustFrameOnScreen(Frame frame,int xPosition,int yPosition) {
		int border=50;//taskbar, or things like this
        java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        int screenY=(int)Math.round(currentScreen.height);
        if(screenX>1920)screenX/=2;        
        java.awt.Dimension currentDims=frame.getSize();
        int frameX=(int)Math.round(currentDims.width);
        int frameY=(int)Math.round(currentDims.height);

        int x=0;int y=0;
        if(xPosition==0)x=border;
        if(xPosition==1)x=(screenX-frameX)/2;
        if(xPosition==2)x=screenX-border-frameX;
        if(yPosition==0)y=border;
        if(yPosition==1)y=(screenY-frameY)/2;
        if(yPosition==2)y=screenY-border-frameY;
		int xx=frame.getSize().width;
		int yy=frame.getSize().height;		
		frame.setLocation(x, y);
		frame.setSize(xx, yy);
        //System.out.println("Positioning frame at screen coordinates : ( "+x+" , "+y+" )");
    }
	
	public static void adjustImageOnScreenRelative(ImagePlus img1,ImagePlus img2Reference,int xPosition,int yPosition,int distance) {
		adjustFrameOnScreenRelative(img1.getWindow(),img2Reference.getWindow(), xPosition, yPosition,distance);
	}

	public static void adjustFrameOnScreenRelative(Frame currentFrame,Frame referenceFrame,int xPosition,int yPosition,int distance) {
		if(xPosition==3) {currentFrame.setLocation(referenceFrame.getLocationOnScreen().x,referenceFrame.getLocationOnScreen().y);return;}
		java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        int screenY=(int)Math.round(currentScreen.height);
        if(screenX>1920)screenX/=2;        

        java.awt.Dimension currentDims=currentFrame.getSize();
        int currentX=(int)Math.round(currentDims.width);
        int referenceX=(int)Math.round(referenceFrame.getSize().width);
        int currentY=(int)Math.round(currentDims.height);
		int border=50;//taskbar, or things like this
		int x=0;int y=0;
        if(xPosition==0)x=referenceFrame.getLocationOnScreen().x-currentX-distance;//Set to the left of reference
        if(xPosition==1)x=referenceFrame.getLocationOnScreen().x;
        if(xPosition==2)x=referenceFrame.getLocationOnScreen().x+referenceX+distance;//Set to the left of reference
        if(yPosition==0)y=border;
        if(yPosition==1)y=referenceFrame.getLocationOnScreen().y;
        if(yPosition==2)y=screenY-border-currentY;
        currentFrame.setLocation(x, y);
    }


	
	
	
	
	
	
	
	/* Helpers for a fast visual checking of an image  */	
	public static void imageChecking(ImagePlus imgInit,double sliMin,double sliMax,int periods,String message,double totalDuration,boolean fluidVisu) {
		System.out.println("Verifying image names "+message+" with dimensions= "+TransformUtils.stringVector(VitimageUtils.getDimensions(imgInit), "")+" with voxel sizes="+TransformUtils.stringVector(VitimageUtils.getVoxelSizes(imgInit), ""));
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
	

	
	
	
	
	
	
	
	

	
	/*Various implementations of image composition, for registration visual assessment */
	public static ImagePlus actualizeData(ImagePlus source,ImagePlus dest) {
		int[]dims=VitimageUtils.getDimensions(source);
		int Z=dims[2];int Y=dims[1];int X=dims[0];
		if(source.getType() == ImagePlus.GRAY8) {
			byte[][] valsSource=new byte[Z][];
			byte[][] valsDest=new byte[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(byte [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(byte [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}
		if(source.getType() == ImagePlus.GRAY16) {
			short[][] valsSource=new short[Z][];
			short[][] valsDest=new short[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(short [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(short [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}
		if(source.getType() == ImagePlus.GRAY32) {
			float[][] valsSource=new float[Z][];
			float[][] valsDest=new float[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(float [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(float [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}

		if(source.getType() == ImagePlus.COLOR_RGB) {
			int[][] valsSource=new int[Z][];
			int[][] valsDest=new int[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(int [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(int [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
			}			
		}
		//dest.changes=true;
		dest.updateAndDraw();
		return dest;
		
	}
	
	public static ImagePlus compositeRGBDoubleJet(ImagePlus img1,ImagePlus img2,ImagePlus img3,String title,boolean mask,int teinte) {
		ImagePlus img1Source=new Duplicator().run(img1);
		ImagePlus img2Source=new Duplicator().run(img2);
		ImagePlus img3Source=new Duplicator().run(img3);
		img1Source.resetDisplayRange();
		img2Source.resetDisplayRange();
		img3Source.resetDisplayRange();
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

	public static ImagePlus compositeNoAdjustOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeNoAdjustOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}

	public static ImagePlus compositeOf(ImagePlus img1Source,ImagePlus img2Source){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		img1.resetDisplayRange();
		img2.resetDisplayRange();
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


	
	
	
	
	
	
	
	/* Various thread helpers. Three first from Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static Thread[] newThreadArray(int n) {  
		return new Thread[n];  
	}  

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

	public static void startAndJoin(Thread thread){  
		thread.setPriority(Thread.NORM_PRIORITY);  
		thread.start();  
		try{     
			thread.join();  
		} catch (InterruptedException ie) {  	System.out.println(ie.getStackTrace());throw new RuntimeException(ie);  }  
	}   
	
	public static int[][]listForThreads(int nbP,int nbProc){
		int [][]indexes=new int[nbProc][];
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[]arrs=new ArrayList[nbProc];
		for(int pro=0;pro<nbProc;pro++) arrs[pro]=new ArrayList<Integer>();
		for(int ind=0;ind<nbP;ind++)arrs[ind%nbProc].add(new Integer(ind));
		for(int pro=0;pro<nbProc;pro++) {indexes[pro]=new int[arrs[pro].size()]; for (int i=0;i<arrs[pro].size();i++) indexes[pro][i]=(Integer)(arrs[pro].get(i));  }
		return indexes;
	}

	

	
	
	
	
	
	
	
	
	
	/* Conversion functions successive test. The last one is in use */	
	public static ImagePlus[]stacksFromHyperstack(ImagePlus hyper,int nb){
		ImagePlus []ret=new ImagePlus[nb];
		for(int i=0;i<nb;i++) {
			IJ.run(hyper,"Make Substack...","slices=1-"+(hyper.getStackSize()/nb)+" frames="+(i+1)+"-"+(i+1)+"");
			ret[i]=IJ.getImage();
			ret[i].setTitle("Splitting hyperstack, frame "+i);
			ret[i].hide();
			VitimageUtils.adjustImageCalibration(ret[i],hyper);
		}
		return ret;
	}

	public static ImagePlus[]stacksFromHyperstackBis(ImagePlus hyper){
		int nbZ=hyper.getNSlices();
		int nbT=hyper.getNFrames();
		int nbC=hyper.getNChannels();
		int nb=nbT*nbC;
		ImagePlus []ret=new ImagePlus[nb];
		for(int ic=0;ic<nbC;ic++) {
			for(int it=0;it<nbT;it++) {
				System.out.println(ic+"/"+nbC+" ,  "+it+"/"+nbT);
				int i=ic*nbT+it;
				if(nbC>1 && nbT==1) 						IJ.run(hyper,"Make Substack...","channels="+(ic+1)+"-"+(ic+1)+" slices=1-"+nbZ+"");
				else if(nbC==1 && nbT>1) 					IJ.run(hyper,"Make Substack...","slices=1-"+nbZ+" frames="+(it+1)+"-"+(it+1)+"");
				else if(nbC>1 && nbT>1) 					IJ.run(hyper,"Make Substack...","channels="+(ic+1)+"-"+(ic+1)+" slices=1-"+nbZ+" frames="+(it+1)+"-"+(it+1)+"");
				else 										IJ.run(hyper,"Make Substack...","slices=1-"+nbZ+"");
				ret[i]=IJ.getImage();
				System.out.println("Attrape la bonne : "+TransformUtils.stringVectorN(ret[i].getDimensions(),""));
				System.out.println("Dont le titre est = :"+ret[i].getTitle());
				ret[i].setTitle("Splitting hyperstack, channel "+ic+" frame "+it);
				ret[i].hide();
				VitimageUtils.adjustImageCalibration(ret[i],hyper);
			}
		}
		return ret;
	}
		
	public static ImagePlus[]stacksFromHyperstackFast(ImagePlus hyper,int nb){
		ImagePlus []ret=new ImagePlus[nb];
		int sli=hyper.getStackSize()/nb;
		System.out.println("En effet : nbtotal slices="+(sli*9));
		for(int i=0;i<nb;i++) {
			ret[i] = new Duplicator().run(hyper, 1, 1, 1, sli, (i+1), (i+1));
			VitimageUtils.adjustImageCalibration(ret[i],hyper);
		}
		return ret;
	}
	
	public static ImagePlus[]stacksFromHyperstackFastBis(ImagePlus hyper){
		int nbZ=hyper.getNSlices();
		int nbT=hyper.getNFrames();
		int nbC=hyper.getNChannels();
		int nb=nbT*nbC;
		ImagePlus []ret=new ImagePlus[nb];
		for(int ic=0;ic<nbC;ic++) {
			for(int it=0;it<nbT;it++) {
				int i=ic*nbT+it;
				System.out.println(ic+"/"+nbC+" ,  "+it+"/"+nbT);
				ret[i] = new Duplicator().run(hyper, 1+ic, 1+ic, 1, nbZ, 1+it, 1+it);
				VitimageUtils.adjustImageCalibration(ret[i],hyper);
				IJ.run(ret[i],"Grays","");
			}
		}
		return ret;
	}
	
	
	

	
	
	
	
	
	
	
	/* Helper functions to watermark various things in 3d image : Strings, rectangles, cylinders... */		
	public static ImagePlus drawRectangleInImage(ImagePlus imgIn,int x0,int y0,int xf,int yf,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int zM=img.getStackSize();
		byte[][] valsImg=new byte[zM][];
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<=xf;x++) {
				for(int y=y0;y<=yf;y++) {
					valsImg[z][xM*y+x]=  (byte)( ((byte)value) & 0xff);
				}
			}
		}			
		return img;
	}

	public static ImagePlus drawParallepipedInImage(ImagePlus imgIn,int x0,int y0,int z0,int xf,int yf,int zf,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int zM=img.getStackSize();
		byte[][] valsImg=new byte[zM][];
		for(int z=z0;z<=zf;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<=xf;x++) {
				for(int y=y0;y<=yf;y++) {
					valsImg[z][xM*y+x]=  (byte)( ((byte)value) & 0xff);
				}
			}
		}			
		return img;
	}
	
	public static ImagePlus drawCylinderInImage(ImagePlus imgIn,double ray,int x0,int y0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		VitimageUtils.adjustImageCalibration(img, imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double realDisX;
		double realDisY;
		byte[][] valsImg=new byte[zM][];
		double distance;
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					realDisX=(x-x0)*voxSX;
					realDisY=(y-y0)*voxSY;
					distance=Math.sqrt( realDisX * realDisX  +  realDisY * realDisY );
					if(distance < ray) {
						valsImg[z][xM*y+x]=  (byte)( value & 0xff);
					}
				}
			}			
		}
		return img;
	}
	
	public static ImagePlus drawCircleInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		ImagePlus img=new Duplicator().run(imgIn);
		VitimageUtils.adjustImageCalibration(img, imgIn);
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

	public static ImagePlus drawPointAtPixelCoordinatesInImageModifyingIt(ImagePlus imgIn,int x0,int y0,int z0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		int xM=imgIn.getWidth();
		byte[] valsImg=(byte[])imgIn.getStack().getProcessor(z0+1).getPixels();
		valsImg[xM*y0+x0]=  (byte)( value & 0xff);
		return imgIn;
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
	
	public static ImagePlus writeBlackTextOnImage(String text, ImagePlus img,int fontSize,int numLine) {
		ImagePlus ret=new Duplicator().run(img);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(10*img.getWidth()*1.0/512,10*img.getWidth()*1.0/512+numLine*fontSize*2, text, font);
		roi.setStrokeColor(Color.black);
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
		return writeTextOnImage(text,  img, fontSize,numLine,value,10.0/512, 10.0/512);
	}

	public static ImagePlus writeWhiteOnImage(String text, ImagePlus img,int fontSize,int numLine) {
		return writeTextOnImage(text,  img, fontSize,numLine,EPSILON,10.0/512, 10.0/512);
	}

	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize,int numLine,double value,double xCoordRatio,double yCoordRatio) {
		double valMin=img.getProcessor().getMin();
		double valMax=img.getProcessor().getMax();
		ImagePlus ret=new Duplicator().run(img);
		if(value!=EPSILON)ret.getProcessor().setMinAndMax(valMin, value);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi( xCoordRatio*img.getWidth(),yCoordRatio*img.getWidth()+numLine*fontSize*2, text, font);
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
		if(value!=EPSILON)ret.getProcessor().setMinAndMax(valMin, valMax);
		return ret;
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


	
	
	
	
	
	
	

	/**
	 * Helper functions to access a part of image values, and to compute some statistics on these data
	 *  */	
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

	public static double[] statistics1DNoBlack(double[] vals){
		double accumulator=0;
		int hits=0;
		for(int i=0;i<vals.length ;i++) {if(vals[i]>=1) {accumulator+=vals[i];hits++;};}
		double mean=(accumulator/hits);
		accumulator=0;
		for(int i=0;i<vals.length ;i++) if(vals[i]>=1) accumulator+=Math.pow(vals[i]-mean,2);
		double std=Math.sqrt(accumulator/hits);	
		return (new double[] {mean,std});
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
	
	public static double[] minAndMaxOfTab(double[] vals){
		double min=10E35;
		double max=-10E35;
		for(int i=0;i<vals.length ;i++) {
			if(vals[i]>max)max=vals[i];
			if(vals[i]<min)min=vals[i];
		}
		return (new double[] {min,max});
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
	
	public static ImagePlus[] meanAndVarOnlyValidValuesByte(ImagePlus img,int rayX) {
		ImagePlus imgMean=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgMean, img);
		IJ.run(imgMean,"32-bit","");
		ImagePlus imgVar=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgVar, img);
		IJ.run(imgVar,"32-bit","");
		ImagePlus imgMask=VitimageUtils.thresholdByteImage(img, 0,1);
		int dimZ=img.getStackSize();
		System.out.print("Moyenne et variance dans masque ...");
		for(int z=1;z<=dimZ;z++) {
			if(z%10==0)System.out.print("   "+z+"/"+dimZ);
			imgMean.setSlice(z);
			imgVar.setSlice(z);
			imgMask.setSlice(z);
			IJ.run(imgMask, "Create Selection", "");			
			imgMean.setRoi(imgMask.getRoi());
			IJ.run(imgMean, "Mean...", "radius="+rayX+" slice");
			imgVar.setRoi(imgMask.getRoi());
			IJ.run(imgVar, "Variance...", "radius="+rayX+" slice");
			imgMean.deleteRoi();
			imgVar.deleteRoi();
			imgMask.deleteRoi();
		}
		System.out.println("");
		return new ImagePlus[] {imgMean,imgVar};
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
	
	public static boolean isNullImage(ImagePlus imgIn) {
		if(imgIn.getType() == ImagePlus.GRAY16)return isNullShortImage(imgIn);
		if(imgIn.getType() == ImagePlus.GRAY32)return isNullFloatImage(imgIn);
		ImagePlus img=new Duplicator().run(imgIn);
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
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

	public static double[][]getHistogram(ImagePlus img,int nbSlicesConsidered){
		if(img.getType()==ImagePlus.GRAY32 || img.getType()==ImagePlus.COLOR_256 || img.getType()==ImagePlus.COLOR_RGB){
			VitiDialogs.notYet("GetHistogram of float image in VitimageUtils");
			System.exit(0);
		}
		int nBins=img.getStack().getProcessor(1).getHistogram().length;
		double[]histo=new double[nBins];
		double[]histoCumul=new double[nBins];
		int[][]dataHisto=new int[nbSlicesConsidered][nBins];
		for(int n=1;n<nbSlicesConsidered+1;n++) {
			int index=1+(img.getStackSize()*n)/(nbSlicesConsidered+1);
			dataHisto[n-1]=img.getStack().getProcessor(index).getHistogram();
			System.out.println("Get histo : "+TransformUtils.stringVectorN(dataHisto[n-1], ""));
		}
		double sum=0;
		for(int b=0;b<nBins;b++) {
			for(int n=0;n<nbSlicesConsidered;n++) histo[b]+=dataHisto[n][b];
			histoCumul[b]=histo[b]+(b!=0 ? histo[b-1] : 0);
		}
		sum=histoCumul[nBins-1];
		for(int b=0;b<nBins;b++) {histo[b]/=sum;histoCumul[b]/=sum;}
		return new double[][] {histo,histoCumul};
	}
	
	public static int[]getRange(ImagePlus img,double percentageDynamicCovered,int nbSlicesConsidered){
		double firstWing=(100-percentageDynamicCovered)/2;
		double secondWing=percentageDynamicCovered+firstWing;
		firstWing/=100;
		secondWing/=100;
		double[][]histos=getHistogram(img,nbSlicesConsidered);
		int nBins=histos[0].length;
		int firstIndex=0;int secondIndex=nBins-1;
		while((firstIndex<nBins) && (histos[1][firstIndex]<firstWing))firstIndex++;
		if(firstIndex>0)firstIndex--;
		while((secondIndex>=0) && (histos[1][secondIndex]>secondWing))secondIndex--;
		if(secondIndex<nBins-1)secondIndex++;
		System.out.println("Quantiles detectes : q1 a "+firstWing+" : index = "+firstIndex+" cumul="+histos[1][firstIndex]);
		System.out.println("Quantiles detectes : q2 a "+secondWing+" : index = "+secondIndex+" cumul="+histos[1][secondIndex]);
		System.out.println("");
		return new int[] {firstIndex,secondIndex};
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
	
	public static int indmax(int[]tab) {
		double max=-1000000;int indmax=0;
		for(int i=0;i<tab.length;i++)if(tab[i]>max) {max=tab[i];indmax=i;}
		return indmax;
	}

	public static int indmax(double[]tab) {
		double max=-1000000;int indmax=0;
		for(int i=0;i<tab.length;i++)if(tab[i]>max) {max=tab[i];indmax=i;}
		return indmax;
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

	public static double max(double[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.max, tab is null. Return 0");
			return 0;
		}
		double max=tab[0];
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
	
	public static double min(double[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.min, tab is null. Return 0");
			return 0;
		}
		double min=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i]<min)min=tab[i];
		}
		return min;
	}
	
	public static double[]getColumnOfTab(double[][]tab,int column){
		double[]ret=new double[tab.length];
		for(int i=0;i<tab.length;i++)ret[i]=tab[i][column];
		return ret;
	}
	
	public static double[][]transposeTab(double[][]tab){
		double[][]ret=new double[tab[0].length][tab.length];
		for(int i=0;i<tab.length;i++)for(int j=0;j<tab[0].length;j++)ret[j][i]=tab[i][j];
		return ret;
	}

	public static int[] getMinMaxByte(ImagePlus imgIn) {
		byte[]in=new byte[imgIn.getStackSize()];
		int X=imgIn.getWidth();
		int Y=imgIn.getHeight();
		int Z=imgIn.getStackSize();
		int valMin=255;
		int valMax=0;
		for(int z=0;z<Z;z++) {
			in=(byte []) imgIn.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					if(in[y*X+x]>((byte)(valMax & 0xff))) valMax=(int)((byte)in[y*X+x] & 0xff);
					if(in[y*X+x]<((byte)(valMin & 0xff))) valMin=(int)((byte)in[y*X+x] & 0xff);
				}			
			}
		}
		return new int[] {valMin,valMax};
	}
	
	public static double[] getMinMaxFloat(ImagePlus imgIn) {
		float[]in=new float[imgIn.getStackSize()];
		int X=imgIn.getWidth();
		int Y=imgIn.getHeight();
		int Z=imgIn.getStackSize();
		float valMin=(float)10E10;
		float valMax=(float)(-10E10);
		for(int z=0;z<Z;z++) {
			in=(float []) imgIn.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					if(in[y*X+x]>valMax)valMax=in[y*X+x];
					if(in[y*X+x]<valMin)valMin=in[y*X+x];
				}			
			}
		}
		return new double[] {valMin,valMax};
	}


	


	
	
	
	/**
	 * Functions for accessing basic informations about images
	 *  */	
	public static double[]getVoxelSizes(ImagePlus img){
		return new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
	}
	
	public static int[]getDimensions(ImagePlus img){
		return new int[] {img.getWidth(),img.getHeight(),img.getStackSize()};
	}

	public static double[]getDimensionsRealSpace(ImagePlus img){
		int[]dims=getDimensions(img);
		double[]voxs=getVoxelSizes(img);
		double[]ret=new double[] {dims[0]*voxs[0],dims[1]*voxs[1],dims[2]*voxs[2]};
		return ret;
	}

	public static int[]getDimensionsXYZCT(ImagePlus img){
		int[]tab1=img.getDimensions();
		return new int[] {tab1[0],tab1[1],tab1[3],tab1[2],tab1[4]};
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

	public static double[]getImageCenter(ImagePlus ref,boolean giveCoordsInRealSpace){
		int[]dims=VitimageUtils.getDimensions(ref);
		double[]voxs=VitimageUtils.getVoxelSizes(ref);
		double[]ret=new double[3];
		for(int dim=0;dim<3;dim++)ret[dim]=(dims[dim]-1)/2.0*(giveCoordsInRealSpace ? voxs[dim] : 1);
		return ret;
	}

	
	
	
	
	

	/** Helpers for writing or reading data in files */
	public static int readIntFromFile(String file) {
		return Integer.parseInt(readStringFromFile(file));
	}
	
	public static void writeDoubleInFile(String file,double a) {
		writeStringInFile(""+a,file);
	}
	
	public static double readDoubleFromFile(String file) {
		return Double.parseDouble(readStringFromFile(file));
	}
		
	public static void writeDoubleArrayInFile(double [][]tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		int nDims=tab[0].length;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+tab.length+"\n");
			for(int i=0;i<nData;i++) {
				for(int j=0;j<nDims-1;j++) {
					out.write(""+tab[i][j]+" ");
				}
				out.write(""+tab[i][nDims-1]+"\n");
			}
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	public static void writeIntArrayInFile(int [][]tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		int nDims=tab[0].length;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+tab.length+"\n");
			for(int i=0;i<nData;i++) {
				for(int j=0;j<nDims-1;j++) {
					out.write(""+tab[i][j]+" ");
				}
				out.write(""+tab[i][nDims-1]+"\n");
			}
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	public static void writeIntArray1DInFile(int []tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+nData+"\n");
			for(int i=0;i<nData;i++) out.write(tab[i]+"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	public static int[] readIntArray1DFromFile(String file) {
		File fParam=new File(file);
		int nData;
		int[]vals;
		String[]strFile=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new int[nData];
		for(int i=1;i<=nData ; i++) {
			vals[i-1]=Integer.parseInt(strFile[i]);			
		}
		return vals;
	}
	
	public static void writeDoubleArray1DInFile(double []tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+nData+"\n");
			for(int i=0;i<nData;i++) out.write(tab[i]+"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	public static double[] readDoubleArray1DFromFile(String file) {
		File fParam=new File(file);
		int nData;
		double[]vals;
		String[]strFile=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new double[nData];
		for(int i=1;i<=nData ; i++) {
			vals[i-1]=Double.parseDouble(strFile[i]);			
		}
		return vals;
	}
	
	public static void writePoint3dArrayInFile(Point3d[]tab,String file) {
		writeDoubleArrayInFile(VitimageUtils.convertPoint3dArrayToDoubleArray(tab),file);
	}
	
	public static Point3d[] readPoint3dArrayInFile(String file) {
		return VitimageUtils.convertDoubleArrayToPoint3dArray(VitimageUtils.readDoubleArrayFromFile(file,3));
	}

	public static double[][] readDoubleArrayFromFile(String file,int nbDimsPerLine) {
		File fParam=new File(file);
		int nData;
		double[][]vals;
		String[]strFile=null;
		String[]strLine=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new double[nData][nbDimsPerLine];
		for(int i=1;i<=nData ; i++) {
			strLine=strFile[i].split(" ");
			for(int j=0;j<nbDimsPerLine;j++) {
				vals[i-1][j]=Double.parseDouble(strLine[j]);
				
			}
		}
		return vals;
	}
		
	public static int[][] readIntArrayFromFile(String file,int nbDimsPerLine) {
		File fParam=new File(file);
		int nData;
		int[][]vals;
		String[]strFile=null;
		String[]strLine=null;
		try {
			 String str= Files.lines(Paths.get(fParam.getAbsolutePath()) ).collect(Collectors.joining("\n"));
			 strFile=str.split("\n");
        } catch (IOException ex) {        ex.printStackTrace();   }
		nData=Integer.parseInt(strFile[0]);
		vals=new int[nData][nbDimsPerLine];
		for(int i=1;i<=nData ; i++) {
			strLine=strFile[i].split(" ");
			for(int j=0;j<nbDimsPerLine;j++) {
				vals[i-1][j]=Integer.parseInt(strLine[j]);			
			}
		}
		return vals;
	}

	public static void writeStringInFile(String text,String file) {
		if(file ==null)return;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(text);
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}			
	}
	
	public static String readStringFromFile(String file) {
		String str=null;
		try {
			str= Files.lines(Paths.get(new File(file).getAbsolutePath()) ).collect(Collectors.joining("\n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}
	
	public static void writeIntInFile(String file,int a) {
		writeStringInFile(""+a,file);
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




	
	
	
	
	
	
	
	/** Helpers used in Registration steps (BM and ITK)*/
	public static ImagePlus getBinaryGrid(ImagePlus img,int pixelSpacing) {
		return getBinaryGrid(img, pixelSpacing,true,false);
	}
	
	public static ImagePlus getBinaryGrid(ImagePlus img,int pixelSpacing,boolean doubleSizeEveryFive,boolean displayTextBorder) {
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
					if( (doubleSizeEveryFive || doDouble) && (x%pixelSpacing==pixelSpacing/2+1) || 
						    (y%pixelSpacing==pixelSpacing/2+1)){
							tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}	
					if( doubleSizeEveryFive && ( (x%pixelSpacing==pixelSpacing/2+2) && ((x/pixelSpacing)%5==0)) || 
						    ((y%pixelSpacing==pixelSpacing/2+2) &&  ((y/pixelSpacing)%5==0))) {
							tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}	
					if( pixelSpacing>9 && doubleSizeEveryFive && ( (x%pixelSpacing==pixelSpacing/2+3) && ((x/pixelSpacing)%5==0)) || 
						    ((y%pixelSpacing==pixelSpacing/2+3) &&  ((y/pixelSpacing)%5==0))) {
							tabRet[dimX*y+x]=(byte)(255 & 0xff);
					}	
				}
			}
		}	
		return ret;
	}
	 
	public static Object[] getCorrespondanceListAsImagePlus(ImagePlus imgRef,ArrayList<double[][]>tabCorr,double[]curVoxSize,int sliceInt2,int blockStrideX,int blockStrideY,int blockStrideZ,int blockHalfSizeX,int blockHalfSizeY,int blockHalfSizeZ,boolean vectors) {
		int [] dims=VitimageUtils.getDimensions(imgRef);		
		int sliceInt=(sliceInt2<0 ? dims[2]/2 : sliceInt2);
		int sliceIntCorr=0;
		int distMin=1000000;
		ImagePlus ret=IJ.createImage("corr", dims[0], dims[1], dims[2], 32);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		double [] voxS=VitimageUtils.getVoxelSizes(imgRef);
		float[][]dataRet=new float[dims[2]][];
		for(int z=0;z<dims[2];z++) {
			dataRet[z]=(float[])ret.getStack().getProcessor(z+1).getPixels();
		}

		
		if(!vectors) {
			int dx=Math.min(blockStrideX/2-1, blockHalfSizeX);
			int dy=Math.min(blockStrideY/2-1, blockHalfSizeY);
			int dz=Math.min(blockStrideZ/2-1, blockHalfSizeZ);
			for(int cor=0;cor<tabCorr.size();cor++) {
				int x=(int)Math.round(tabCorr.get(cor)[0][0]/voxS[0]*curVoxSize[0]  );
				int y=(int)Math.round(tabCorr.get(cor)[0][1]/voxS[1]*curVoxSize[1]  );
				int z=(int)Math.round(tabCorr.get(cor)[0][2]/voxS[2]*curVoxSize[2]  );
				if(Math.abs(sliceInt-z) < distMin) {
					distMin=Math.abs(sliceInt-z);
					sliceIntCorr=z;
				}
				float score=(float)tabCorr.get(cor)[2][0];
				int x0=Math.max(0, x-dx);
				int y0=Math.max(0, y-dy);
				int z0=Math.max(0, z-dz);
				int xf=Math.min(dims[0]-1, x+dx);
				int yf=Math.min(dims[1]-1, y+dy);
				int zf=Math.min(dims[2]-1, z+dz);
				for(int xx=x0;xx<=xf;xx++) {
					for(int yy=y0;yy<=yf;yy++) {
						for(int zz=z0;zz<=zf;zz++) {
							dataRet[zz][dims[0]*yy+xx]=score;
						}
					}
				}
			}
		}
		else{
			
			for(int cor=0;cor<tabCorr.size();cor++) {
				int x0=(int)Math.round(tabCorr.get(cor)[0][0]/voxS[0]*curVoxSize[0]  );
				int y0=(int)Math.round(tabCorr.get(cor)[0][1]/voxS[1]*curVoxSize[1]  );
				int z0=(int)Math.round(tabCorr.get(cor)[0][2]/voxS[2]*curVoxSize[2]  );
				int xf=(int)Math.round(tabCorr.get(cor)[1][0]/voxS[0]*curVoxSize[0]  );
				int yf=(int)Math.round(tabCorr.get(cor)[1][1]/voxS[1]*curVoxSize[1]  );
				int zf=(int)Math.round(tabCorr.get(cor)[1][2]/voxS[2]*curVoxSize[2]  );
				double dx=(xf-x0)/5;
				double dy=(yf-y0)/5;
				double dz=(zf-z0)/5;
				float score=(float)tabCorr.get(cor)[2][0];
				//Bras fleche suivant XY
				for(int dt=0;dt<=15;dt++) {
					int zz=z0;
					int xx=x0+(int)Math.round(dx*dt);
					int yy=y0+(int)Math.round(dy*dt);
					dataRet[zz][dims[0]*yy+xx]=score;
				}
				//Bras fleche suivant XZ
				for(int dt=0;dt<=15;dt++) {
					int yy=y0;
					int xx=x0+(int)Math.round(dx*dt);
					int zz=z0+(int)Math.round(dz*dt);
					dataRet[zz][dims[0]*yy+xx]=score;
				}
				//Bras fleche suivant YZ
				for(int dt=0;dt<=15;dt++) {
					int xx=x0;
					int zz=z0+(int)Math.round(dz*dt);
					int yy=y0+(int)Math.round(dy*dt);
					dataRet[zz][dims[0]*yy+xx]=score;
				}
				//Base fleche
				for(int ddz=-1;ddz<=1;ddz++)for(int ddx=-1;ddx<=1;ddx++)for(int ddy=-1;ddy<=1;ddy++)dataRet[z0+ddz][dims[0]*(y0+ddy)+x0+ddx]=score;
			}
		}

		return new Object[] {ret,sliceIntCorr};		
	}

	public static ImagePlus nullImage(ImagePlus in) {
		return makeOperationOnOneImage(in,2,0, true);
	}
	
	public static double[] caracterizeBackgroundOfImage(ImagePlus img) {
		int samplSize=Math.min(15,img.getWidth()/10);
		int x0=samplSize;
		int y0=samplSize;
		int x1=img.getWidth()-samplSize;
		int y1=img.getHeight()-samplSize;
		int z01=img.getStackSize()/2;
		double[][] vals=new double[4][2];
		vals[0]=VitimageUtils.valuesOfImageAround(img,x0,y0,z01,samplSize/2);
		vals[1]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);
		vals[2]=VitimageUtils.valuesOfImageAround(img,x1,y0,z01,samplSize/2);
		vals[3]=VitimageUtils.valuesOfImageAround(img,x1,y1,z01,samplSize/2);		
		double [][]stats=new double[4][2];
		double []globStats=VitimageUtils.statistics2D(vals);
		for(int i=0;i<4;i++) {
			stats[i]=(VitimageUtils.statistics1D(vals[i]));
			if( (Math.abs(stats[i][0]-globStats[0])/globStats[0]>0.3)){
				IJ.log("Warning : noise computation  There should be an object in the supposed background\nthat can lead to misestimate background values. Detected at slice "+samplSize/2+"at "+
							(i==0 ?"Up-left corner" : i==1 ? "Down-left corner" : i==2 ? "Up-right corner" : "Down-right corner")+
							". Mean values of squares="+globStats[0]+". Outlier value="+vals[i][0]+" you should inspect the image and run again.");
			}
		}
		return new double[] {globStats[0],globStats[1]};
	}
	
	public static ImagePlus makeOperationOnOneImage(ImagePlus in,int op_1add_2mult_3div_4sub,double val,boolean copyBefore) {
		ImagePlus img=null;
		if(copyBefore) {
			img=new Duplicator().run(in);
			VitimageUtils.adjustImageCalibration(img, in);
		}
		else img=in;
		switch (op_1add_2mult_3div_4sub) {
		case OP_ADD :IJ.run(img, "Add...", "value="+val+" stack");break;
		case OP_MULT :IJ.run(img, "Multiply...", "value="+val+" stack");break;
		case OP_DIV :IJ.run(img, "Divide...", "value="+val+" stack");break;
		case OP_SUB :IJ.run(img, "Substract...", "value="+val+" stack");break;
		}
		return img;
	}
	
	public static ImagePlus makeOperationBetweenTwoImages(ImagePlus in1,ImagePlus in2,int op_1add_2mult_3div_4sub,boolean make32bitResult) {
		ImagePlus img=null;
		img=new Duplicator().run(in1);
		VitimageUtils.adjustImageCalibration(img, in1);
		ImageCalculator ic = new ImageCalculator();
		ImagePlus res=null;
		switch (op_1add_2mult_3div_4sub) {
		case OP_ADD :res=ic.run("Add "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_MULT :res=ic.run("Multiply "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_DIV :res=ic.run("Divide "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_SUB :res=ic.run("Substract "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		case OP_MEAN :res=ic.run("Average "+(make32bitResult ? "32-bit" : "")+" create stack", img, in2);break;
		}
		res.hide();
		return res;
	}
		
	public static ImagePlus binaryOperationBetweenTwoImages(ImagePlus in1,ImagePlus in2,int op_1OR_2AND_3Pouet_4SUB) {
		ImagePlus img=null;
		img=new Duplicator().run(in1);
		VitimageUtils.adjustImageCalibration(img, in1);
		ImageCalculator ic = new ImageCalculator();
		ImagePlus res=null;
		switch (op_1OR_2AND_3Pouet_4SUB) {
		case OP_ADD :res=ic.run("OR "+" create stack", img, in2);break;
		case OP_MULT :res=ic.run("AND "+" create stack", img, in2);break;
		case OP_DIV :return null;
		case OP_SUB :res=ic.run("DIFF "+" create stack", img, in2);break;
		}
		res.hide();
		return res;
	}

	public static ImagePlus switchAxis(ImagePlus img,int switch_0XY_1XZ_2YZ) {
		ImagePlus ret=null;
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		int X2=img.getWidth();
		int Y2=img.getHeight();
		int Z2=img.getStackSize();
		double[]voxOut=VitimageUtils.getVoxelSizes(img);
		double temp;
		int tem;
		if(switch_0XY_1XZ_2YZ==0) {
			ret=ij.gui.NewImage.createImage("Mask",Y,X,Z,img.getBitDepth(),ij.gui.NewImage.FILL_BLACK);	
			temp=voxOut[0];voxOut[0]=voxOut[1];voxOut[1]=temp; 
			tem=X2; X2=Y2;Y2=tem;
			VitimageUtils.adjustImageCalibration(ret, voxOut,"mm");
		}	
		if(switch_0XY_1XZ_2YZ==1) {
			ret=ij.gui.NewImage.createImage("Mask",Z,Y,X,img.getBitDepth(),ij.gui.NewImage.FILL_BLACK);	
			temp=voxOut[0];voxOut[0]=voxOut[2];voxOut[2]=temp; 
			tem=X2;X2=Z2;Z2=tem;
			VitimageUtils.adjustImageCalibration(ret, voxOut,"mm");
		}	
		if(switch_0XY_1XZ_2YZ==2) {
			ret=ij.gui.NewImage.createImage("Mask",X,Z,Y,img.getBitDepth(),ij.gui.NewImage.FILL_BLACK);
			temp=voxOut[1];voxOut[1]=voxOut[2];voxOut[2]=temp;
			tem=Y2;Y2=Z2;Z2=tem;
			VitimageUtils.adjustImageCalibration(ret, voxOut,"mm");
		}	

		
		if(img.getType()==ImagePlus.GRAY8) {
			byte[][] in=new byte[Z][];
			byte[][] out=new byte[Z2][];
			for(int z=0;z<Z;z++)in[z]=(byte []) img.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z2;z++)out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();
			
			for(int z=0;z<Z;z++) {
				if(z%10==0)System.out.print("  z="+z+"/"+Z);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						if(switch_0XY_1XZ_2YZ==0)out[z][x*Y+y]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==1)out[x][y*Z+z]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==2)out[y][z*X+x]=in[z][y*X+x];
					}			 
				}
			}
			System.out.println();
			return ret;
		}
		if(img.getType()==ImagePlus.GRAY16) {
			short[][] in=new short[Z][];
			short[][] out=new short[Z2][];
			for(int z=0;z<Z;z++)in[z]=(short []) img.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z2;z++)out[z]=(short []) ret.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z;z++) {
				if(z%10==0)System.out.print("  z="+z+"/"+Z);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						if(switch_0XY_1XZ_2YZ==0)out[z][x*Y+y]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==1)out[x][y*Z+z]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==2)out[y][z*X+x]=in[z][y*X+x];
					}			 
				}
			}
			System.out.println();
			return ret;
		}
		if(img.getType()==ImagePlus.GRAY32) {
			float[][] in=new float[Z][];
			float[][] out=new float[Z2][];
			for(int z=0;z<Z;z++)in[z]=(float []) img.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z2;z++)out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			for(int z=0;z<Z;z++) {
				if(z%10==0)System.out.print("  z="+z+"/"+Z);
				for(int x=0;x<X;x++) {
					for(int y=0;y<Y;y++) {
						if(switch_0XY_1XZ_2YZ==0)out[z][x*Y+y]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==1)out[x][y*Z+z]=in[z][y*X+x];
						if(switch_0XY_1XZ_2YZ==2)out[y][z*X+x]=in[z][y*X+x];
					}			 
				}
			}
			System.out.println();
			return ret;
		}
		else {
			System.out.println("Switch axis : unsupported format");return null;
		}
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
			
	

	
	


	
	
	
	/**
	 * Helper functions to switch values in images
	 *  */		
	public static ImagePlus switchValueInImage(ImagePlus img,int valueBefore, int valueAfter) {
		ImagePlus out=VitimageUtils.imageCopy(img);
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		if(img.getType() == ImagePlus.GRAY8) {
			for(int z=0;z<zMax;z++) {
				byte[]valsImg=(byte [])out.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xMax;x++) {
					for(int y=0;y<yMax;y++) {
						if(  ((int) ( ((byte)valsImg[xMax*y+x])  & 0xff) ==valueBefore ) ) {
							valsImg[xMax*y+x]=(byte)( valueAfter &0xff);
						}
					}
				}			
			}
		}
		if(img.getType() == ImagePlus.GRAY16) {
			for(int z=0;z<zMax;z++) {
				short[]valsImg=(short [])out.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xMax;x++) {
					for(int y=0;y<yMax;y++) {
						if(  ((int) ( ((short)valsImg[xMax*y+x])  & 0xffff) ==valueBefore ) ) {
							valsImg[xMax*y+x]=(short)( valueAfter &0xffff);
						}
					}
				}			
			}
		}
		return out;
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

	
	
	
	
	
	
	
	
	
	/**Wrappers or implementations of some useful thresholding and dynamic adjustment*/
	public static ImagePlus convertShortToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		IJ.run(ret,"32-bit","");
		float[][] out=new float[ret.getStackSize()][];
		short[][] in=new short[imgIn.getStackSize()][];
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			in[z]=(short []) imgIn.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=((int)((in[z][y*X+x] & 0xffff )));
				}			
			}
		}
		return ret;
	}
		
	public static ImagePlus thresholdByteImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(ret,img);
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
	
	public static ImagePlus convertByteToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=VitimageUtils.imageCopy(imgIn);
		IJ.run(ret,"32-bit","");

		float[][] out=new float[ret.getStackSize()][];
		byte[][] in=new byte[imgIn.getStackSize()][];
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			in[z]=(byte []) imgIn.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=((int)((in[z][y*X+x] & 0xff )));
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


	
	
	
	

	
	
	
	
	
	/**Many useful functions not organized yet*/
	public static void printDebugIntro(int N,String message) {
		for(int i=0;i<N;i++)System.out.println("################## DEBUG ###########");
		System.out.println(message);
	}
		
	public static String strInt6chars(int nb) {
		if(nb<100000 && nb>9999)  return new String(" "+nb);
		if(nb<10000 && nb>999)    return new String("  "+nb);
		if(nb<1000 && nb>99)      return new String("   "+nb);
		if(nb<100 && nb>9)        return new String("    "+nb);
		if(nb<10)                 return new String("     "+nb);
		                          return new String(""+nb);
	}
		
	public static double dou(double d){
		if(d<0)return (-dou(-d));
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}

	public static double dou(double d,int n){
		if(d<0)return (-dou(-d));
		if (d<Math.pow(10, -n))return 0;
		return (double)(Math.round(d * Math.pow(10, n))/Math.pow(10, n));
	}
	
	public static void waitFor(int n) {
		try {
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	public static String[] stringArraySort(String[]tabStr) {
		String[]tabRet=new String[tabStr.length];
		ArrayList<String> listStr=new ArrayList<String>();
		for(String str : tabStr)listStr.add(str);
		Collections.sort(listStr);
		for(int i=0;i<listStr.size();i++)tabRet[i]=listStr.get(i);
		return tabRet;
	}

	public static float hypot(float x, float y) {
		return (float) Math.hypot(x, y);
	}
 
	public static float gaussian(float x, float sigma) {
		return (float) Math.exp(-(x * x) / (2f * sigma * sigma));
	}
	
	public static ImagePlus imageCopy(ImagePlus imgRef) {
		ImagePlus ret=new Duplicator().run(imgRef);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		return ret;
	}
	
	public static ImagePlus imageCopy(ImagePlus imgRef,String title) {
		ImagePlus ret=new Duplicator().run(imgRef);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		for(int z=0;z<imgRef.getStackSize();z++) {
			ret.getStack().setSliceLabel(title+"_z="+(z+1), z+1);
		}
		ret.setTitle(title);
		return ret;
	}

	public static Date getDateFromString(String datStr) {
		Date date=null;
		try {
			date = new SimpleDateFormat("yyyyMMdd").parse(datStr);
		} catch (ParseException e) {
			return new Date(0);
		}  
		return date;
	}

	public static void setLabelOnAllSlices(ImagePlus img,String label) {
		for(int i=0;i<img.getStack().getSize();i++)img.getStack().setSliceLabel(label,i+1);
	}
	
	
	
	
	
	
	/**Point 3d /xyz utilities*/
	public static Point3d toRealSpace(Point3d p,double[]voxs) {
		return new Point3d(p.x*voxs[0],p.y*voxs[1],p.z*voxs[2]);
	}

	public static Point3d toImageSpace(Point3d p,double[]voxs) {
		return new Point3d(p.x/voxs[0],p.y/voxs[1],p.z/voxs[2]);
	}

	public static Point3d[]convertDoubleArrayToPoint3dArray(double[][]tab){
		Point3d[]tabPt=new Point3d[tab.length];
		for(int i=0;i<tab.length;i++)tabPt[i]=new Point3d(tab[i][0],tab[i][1],tab[i][2]);
		return tabPt;		
	}

	public static double[][]convertPoint3dArrayToDoubleArray(Point3d[]tab){
		double[][]tabPt=new double[tab.length][3];
		for(int i=0;i<tab.length;i++){
			tabPt[i][0]=tab[i].x;
			tabPt[i][1]=tab[i].y;
			tabPt[i][2]=tab[i].z;
		}
		return tabPt;		
	}
		
	public static Point3d dou(Point3d p) {
		return new Point3d(VitimageUtils.dou(p.x),VitimageUtils.dou(p.y),VitimageUtils.dou(p.z));
	}
	
	public static Point3d[] getMinMaxPoints(Point3d[]tab) {
		Point3d[]tabRet=new Point3d[] {new Point3d(100000,10000000,1000000),new Point3d(-10000000,-10000000,-10000000)};
		for(Point3d p : tab) {
			if(p.x < tabRet[0].x)tabRet[0].x=p.x;
			if(p.y < tabRet[0].y)tabRet[0].y=p.y;
			if(p.z < tabRet[0].z)tabRet[0].z=p.z;
			if(p.x > tabRet[1].x)tabRet[1].x=p.x;
			if(p.y > tabRet[1].y)tabRet[1].y=p.y;
			if(p.z > tabRet[1].z)tabRet[1].z=p.z;
		}	
		return tabRet;
	}
	

	
	
	
	
	/**Debugging utilities */
	public static String imageResume(ImagePlus img) {
		if(img==null)return "image est nulle";
		int[]dims=VitimageUtils.getDimensions(img);
		double[]voxs=VitimageUtils.getVoxelSizes(img);
		String s="Image "+img.getTitle()+" coded on "+img.getBitDepth()+" per pixel. Dims="+dims[0]+" X "+dims[1]+" X "+dims[2]+"  VoxS="+voxs[0]+" x "+voxs[1]+" x "+voxs[2]+"  NChannels="+img.getNChannels()+"  NFrames="+img.getNFrames();
		return s;
	}

	public static void printImageResume(ImagePlus img) {
		System.out.println(imageResume(img));
	}

	public static void printImageResume(ImagePlus img,String str) {
		System.out.println(str+" : "+imageResume(img));
	}


	

}
