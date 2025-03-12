package io.github.rocsg.fijiyama.common;



import java.lang.Runtime;
import java.lang.Runnable;
import java.lang.InterruptedException;
import java.lang.IllegalArgumentException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.itk.simple.Image;
import org.itk.simple.OtsuThresholdImageFilter;
import org.itk.simple.RecursiveGaussianImageFilter;
import org.itk.simple.ResampleImageFilter;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.opencsv.CSVReader;

import java.io.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.measure.Calibration;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import ij.plugin.ImageInfo;
import ij.plugin.ImagesToStack;
import ij.plugin.LutLoader;
import ij.plugin.RGBStackMerge;
import ij.plugin.Scaler;
import ij.plugin.StackCombiner;
import ij.plugin.filter.Convolver;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.LUT;
import ij.process.StackConverter;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import io.github.rocsg.fijiyama.registration.TransformUtils.VolumeComparator;
import math3d.Point3d;

// TODO: Auto-generated Javadoc
/**
 * The Class VitimageUtils.
 */
public class VitimageUtils {
	
	/** The Constant EPSILON. */
	public final static double EPSILON=0.00001;
	
	/** The Constant LARGE_VALUE. */
	public final static double LARGE_VALUE=1E18;
	
	/** The Constant OP_ADD. */
	public final static int OP_ADD=1;
	
	/** The Constant OP_MULT. */
	public final static int OP_MULT=2;
	
	/** The Constant OP_DIV. */
	public final static int OP_DIV=3;
	
	/** The Constant OP_SUB. */
	public final static int OP_SUB=4;
	
	/** The Constant OP_MEAN. */
	public final static int OP_MEAN=5;
	
	/** The Constant slash. */
	public final static String slash=File.separator;
	
	/** The Constant ERROR_VALUE. */
	public static final int ERROR_VALUE=-1;
	
	/** The Constant COORD_OF_MAX_IN_TWO_LAST_SLICES. */
	public static final int COORD_OF_MAX_IN_TWO_LAST_SLICES=1;
	
	/** The Constant COORD_OF_MAX_ALONG_Z. */
	public static final int COORD_OF_MAX_ALONG_Z=2;
	
	/** The Constant bionanoCapillaryRadius. */
	public final static double bionanoCapillaryRadius=1.15/2;//size in mm in the bionanoNMRI lab

	
	
	
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[]args) {
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/TEST.tif");
		GraphPath<Pix,Bord>graph=getShortestAndDarkestPathInImage(img,4,new Pix(1,1,0),new Pix (20,50,0));
		for(Pix p:graph.getVertexList())System.out.println(p);
		VitimageUtils.waitFor(5000);
		System.exit(0);
	}

	
	
	
	
	
	public boolean isZuluEndangered() {
		if(VitimageUtils.isWindowsOS() && System.getProperties().toString().contains("zulu")) {
			IJ.showMessage("You run windows with zulu JDK. We are sorry, but this is unconvenient\n"+
					" The plugin will close to let you adjust your setup (two operations to make). "+""
				+ "\nThen please read the windows installation instructions on the plugin page"+
					"\nhttps://imagej.net/plugins/fijirelax ");
			return true;
		}
		IJ.log("\nZulu check ok\n\n");
		return false;
	}

	
	
	/**
	 * Gets the shortest and darkest path in image.
	 *
	 * @param imgTemp the img temp
	 * @param connexity the connexity
	 * @param pixStart the pix start
	 * @param pixStop the pix stop
	 * @return the shortest and darkest path in image
	 */
	public static GraphPath<Pix,Bord>getShortestAndDarkestPathInImage(ImagePlus imgTemp,int connexity,Pix pixStart,Pix pixStop){
		ImagePlus img=imgTemp.duplicate();
		if(img.getType()==ImagePlus.GRAY8)img=convertToFloat(img);
		if(img.getType()==ImagePlus.GRAY16)img=convertToFloat(img);
		SimpleWeightedGraph<Pix,Bord>pixGraph=new SimpleWeightedGraph<>(Bord.class);
		Pix[][]tabPix=new Pix[img.getWidth()][img.getHeight()];
		int xM=img.getWidth();
		int yM=img.getHeight();
		float[]tabData=(float[])img.getStack().getPixels(1);
		for(int x=0;x<xM;x++)for(int y=0;y<yM;y++)  {
			tabPix[x][y]=new Pix(x,y,tabData[y*xM+x]);
			if(x==pixStart.x && y==pixStart.y) {tabPix[x][y]=pixStart;}
			if(x==pixStop.x && y==pixStop.y){tabPix[x][y]=pixStop;}
			pixGraph.addVertex(tabPix[x][y]);
		}
		for(int x=0;x<xM;x++)for(int y=0;y<yM;y++) {
			if(tabPix[x][y]==null)continue;
			if( (x<(xM-1)) && (y<(yM-1)) && (connexity==8) ) if(tabPix[x+1][y+1]!=null)pixGraph.addEdge(tabPix[x][y], tabPix[x+1][y+1],new Bord(tabPix[x][y], tabPix[x+1][y+1]));
			if( (x<(xM-1)) && (y>0) && (connexity==8) ) if(tabPix[x+1][y-1]!=null)pixGraph.addEdge(tabPix[x][y], tabPix[x+1][y-1],new Bord(tabPix[x][y], tabPix[x+1][y-1]));
			if( (x<(xM-1)) ) if(tabPix[x+1][y]!=null)pixGraph.addEdge(tabPix[x][y], tabPix[x+1][y],new Bord(tabPix[x][y], tabPix[x+1][y]));
			if( (y<(yM-1)) ) if(tabPix[x][y+1]!=null)pixGraph.addEdge(tabPix[x][y], tabPix[x][y+1],new Bord(tabPix[x][y], tabPix[x][y+1]));
		}			
		for(Bord bord:pixGraph.edgeSet()) pixGraph.setEdgeWeight(bord, bord.getWeightDistExt());
		DijkstraShortestPath<Pix, Bord>djik=new DijkstraShortestPath<Pix, Bord>(pixGraph);
		GraphPath<Pix, Bord> path = djik.getPath(pixStart, pixStop);
		return path;
	}


	
	
	
	
	
	/**
	 * Crop image byte.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param dimX the dim X
	 * @param dimY the dim Y
	 * @param dimZ the dim Z
	 * @return the image plus
	 */
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
	
	/**
	 * Channel splitter.
	 *
	 * @param imgRGB the img RGB
	 * @return the image plus[]
	 */
	public static ImagePlus[]channelSplitter(ImagePlus imgRGB){
		ImagePlus[]tabRet;
		ImagePlus imgTemp=imgRGB.duplicate();
		String tit=imgTemp.getTitle();
		imgTemp.setTitle("temp");
		imgTemp.show();
//		VitimageUtils.waitFor(100);
		IJ.selectWindow(imgTemp.getTitle());
		IJ.run("Split Channels");
		if(imgTemp!=null)imgTemp.close();
//		VitimageUtils.waitFor(100);
		ImagePlus imgR=ij.WindowManager.getImage("temp (red)");
		ImagePlus imgG=ij.WindowManager.getImage("temp (green)");
		ImagePlus imgB=ij.WindowManager.getImage("temp (blue)");
		imgR.setTitle(tit+"_red");
		imgG.setTitle(tit+"_green");
		imgB.setTitle(tit+"_blue");
		tabRet=new ImagePlus[] {imgR.duplicate(),imgG.duplicate(),imgB.duplicate()};
		imgR.close();
		imgG.close();
		imgB.close();
		return tabRet;
	}
	
	/**
	 * Gets the HSB old.
	 *
	 * @param img the img
	 * @return the HSB old
	 */
	public static ImagePlus[]getHSBOld(ImagePlus img){
		img.show();
		IJ.run(img, "HSB Stack", "");
		ImagePlus temp=IJ.getImage();
		temp.hide();
		int Z=temp.getNSlices();
		int T=temp.getNFrames();
		ImagePlus[] imgTab=new ImagePlus[] {
				new Duplicator().run(temp,1,1,1,Z,1,T),
				new Duplicator().run(temp,2,2,1,Z,1,T),
				new Duplicator().run(temp,3,3,1,Z,1,T)					
		};
		temp.close();
		return imgTab;
	}
	
	/**
	 * Laplacian.
	 *
	 * @param val the val
	 * @return the double
	 */
	public static double laplacian(double val) {
		return 1/(1+(val*val));
	}

	/**
	 * Laplacian.
	 *
	 * @param val the val
	 * @param ray the ray
	 * @return the double
	 */
	public static double laplacian(double val,double ray) {
		return laplacian (val/ray);
	}
	
	
	/**
	 * Gets the hsb.
	 *
	 * @param img the img
	 * @return the hsb
	 */
	public static ImagePlus[]getHSB(ImagePlus img){
		if(img.getNSlices()>1) {
			System.out.println("HSB on stack");
			int Z=img.getNSlices();
			ImagePlus[]imgTab=VitimageUtils.stackToSlices(img);
			ImagePlus[][]imgTab2=new ImagePlus[3][Z];
			for(int i=0;i<Z;i++) {
				ImagePlus []temp=getHSB(imgTab[i]);
				for(int can=0;can<3;can++)imgTab2[can][i]=temp[can];
			}
			return new ImagePlus[] {slicesToStack(imgTab2[0]),slicesToStack(imgTab2[1]),slicesToStack(imgTab2[2])};
		}
		ColorProcessor cp=(ColorProcessor) img.getProcessor();
		ImageStack is=cp.getHSBStack();
		return new ImagePlus[] {
				new ImagePlus("hue",is.getProcessor(1)),
				new ImagePlus("sat",is.getProcessor(2)),
				new ImagePlus("bri",is.getProcessor(3)),
		};
	}
	
	


	
    
	public static double[]stringTabToDoubleTab(String[]str){
	double []ret=new double[str.length];
		for(int i=0;i<ret.length;i++) {
			  if(str[i].length()==0)ret[i]=0;
			  else ret[i]=Double.parseDouble(str[i].replace(" ", ""));
		}
		return ret;
	}

	
    /**
     * Split RGB stack head less.
     *
     * @param imp the imp
     * @return the image plus[]
     */
    public static ImagePlus[] splitRGBStackHeadLess(ImagePlus imp) {
        int w = imp.getWidth();
        int h = imp.getHeight();
        ImageStack rgbStack = imp.getStack();
        ImageStack redStack = new ImageStack(w,h);
        ImageStack greenStack = new ImageStack(w,h);
        ImageStack blueStack = new ImageStack(w,h);
        byte[] r,g,b;
        ColorProcessor cp;
        int n = rgbStack.getSize();
        for (int i=1; i<=n; i++) {
            IJ.showStatus(i+"/"+n);
            r = new byte[w*h];
            g = new byte[w*h];
            b = new byte[w*h];
            cp = (ColorProcessor)rgbStack.getProcessor(1);
            cp.getRGB(r,g,b);
            rgbStack.deleteSlice(1);
            //System.gc();
            redStack.addSlice(null,r);
            greenStack.addSlice(null,g);
            blueStack.addSlice(null,b);
            IJ.showProgress((double)i/n);
       }
       String title = imp.getTitle();
       return new ImagePlus[] {
       		new ImagePlus(title+" (red)",redStack),
       		new ImagePlus(title+" (green)",greenStack),
       		new ImagePlus(title+" (blue)",blueStack)
       };
   }

	
	/**
	 * Garbage collector.
	 */
	public static void garbageCollector() {
		Runtime.getRuntime().gc();
	}
	
	/**
	 * Compute rice sigma from background values static.
	 *
	 * @param meanBg the mean bg
	 * @param sigmaBg the sigma bg
	 * @return the double
	 */
	public static double computeRiceSigmaFromBackgroundValuesStatic(double meanBg,double sigmaBg) {
		boolean debug=true;
		double val1=meanBg * Math.sqrt(2.0/Math.PI);
		double val2=sigmaBg * Math.sqrt(2.0/(4.0-Math.PI));
		if(debug && Math.abs((val1-val2)/val2) >0.3) System.out.println("Warning : Acquisition > computeRiceSigmaStatic. Given :M="+meanBg+" , S="+sigmaBg+" gives sm="+val1+" , ss="+val2+"  .sm/ss="+VitimageUtils.dou(val1/val2)+". Using the first one..");
		
		return val1;
	}
	
	/**
	 * Sub 222.
	 *
	 * @param img the img
	 * @return the image plus
	 */
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

	/**
	 * Gets the max value for contrast more intelligent.
	 *
	 * @param img2 the img 2
	 * @param channel the channel
	 * @param time the time
	 * @param z the z
	 * @param percentageKeep the percentage keep
	 * @param factor the factor
	 * @return the max value for contrast more intelligent
	 */
	public static double getMaxValueForContrastMoreIntelligent(ImagePlus img2,int channel,int time,int z,double percentageKeep,double factor) {
		Timer t=new Timer();
		int X=img2.getWidth();
		int Y=img2.getHeight();
		ImagePlus img=new Duplicator().run(img2,channel+1,channel+1,z+1,z+1,time+1,time+1);
		double []d=VitimageUtils.valuesOfBlock(img, 0, 0, 0, X-1, Y-1, 0);
		Arrays.sort(d);
		int index=(int)Math.round(d.length*percentageKeep/100.0);
		//System.out.println("Setting with index="+index+"/"+d.length+" . Val0="+d[0]+" valindex="+d[index]+" valfinale="+d[d.length-1]);
		t.print("After sorting");
		return (d[index]*factor);
	}

	/**
	 * Distance.
	 *
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param x1 the x 1
	 * @param y1 the y 1
	 * @return the double
	 */
	public static double distance(double x0,double y0,double x1,double y1) {
		return Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0));
	}
	
	/**
	 * Distance.
	 *
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param x1 the x 1
	 * @param y1 the y 1
	 * @param z1 the z 1
	 * @return the double
	 */
	public static double distance(double x0,double y0,double z0,double x1,double y1,double z1) {
		return Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0));
	}

	/**
	 * Gets the double sided range for contrast more intelligent.
	 *
	 * @param img2 the img 2
	 * @param channel the channel
	 * @param time the time
	 * @param z the z
	 * @param percentageKeep the percentage keep
	 * @param factor the factor
	 * @return the double sided range for contrast more intelligent
	 */
	public static double []getDoubleSidedRangeForContrastMoreIntelligent(ImagePlus img2,int channel,int time,int z,double percentageKeep,double factor) {
		String str=new ImageInfo().getImageInfo(img2);
		double a=0,b=1;
		if(str!=null) {
			String[]strTab=str.split("\n");
			for(int i=0;i<strTab.length-3;i++) {
				if(strTab[i].contains("Calibration function: y = a+bx")){
					a=Double.parseDouble(strTab[i+1].split(": ")[1]);
					b=Double.parseDouble(strTab[i+2].split(": ")[1]);
				}
			}
		}
		int X=img2.getWidth();
		int Y=img2.getHeight();
		ImagePlus img=new Duplicator().run(img2,channel+1,channel+1,z+1,z+1,time+1,time+1);
		double []d=VitimageUtils.valuesOfBlock(img, 0, 0, 0, X-1, Y-1, 0);
		Arrays.sort(d);
		int indexMax=(int)Math.round(d.length*percentageKeep/100.0);
		int indexMin=(int)Math.round(d.length*1.0*(100.0-percentageKeep)/100.0);
		
		//t.print("Smart display ranging. Percentiles= from index "+indexMin+" to "+indexMax+"/"+d.length+"indices  . Initial ranging=["+img.getDisplayRangeMin()+" , "+img.getDisplayRangeMax()+"] , final Ranging=["+((a+b*d[indexMin]))+" , "+(a+b*d[indexMax])+"]"+". Sorting intensities of the full slice took");
		double valInitMin=(a+b*d[indexMin]);
		double valInitMax=(a+b*d[indexMax]);
		double valMean=0.5*valInitMin+0.5*valInitMax;
		double std=valInitMax-valMean;
		return new double[] {valMean-std,valMean+factor*std};
	}

	
	
	/**
	 *  Some Csv helpers   --------------------------------------------------------------------------------------.
	 *
	 * @param csvPath the csv path
	 * @return the string[][]
	 */
	public static String[][]readStringTabFromCsv(String csvPath){
	    try {
	    	CSVReader reader = new CSVReader(new FileReader(csvPath)); 
			java.util.List<String[]> r = reader.readAll();
			String[][]ret=new String[r.size()][];
			for(int index=0;index<r.size();index++)ret[index]=r.get(index);
			reader.close();
			return ret;
	    }
	    catch(Exception e) {
	    	System.out.println("Got an exception during opening "+csvPath);
    	}
	    return null;
}

	/**
	 * Read double tab from csv.
	 *
	 * @param csvPath the csv path
	 * @return the double[][]
	 */
	public static double[][]readDoubleTabFromCsv(String csvPath){
		String[][]tab=readStringTabFromCsv(csvPath);
		double[][]tab2=new double[tab.length][];
		for(int i=0;i<tab.length;i++) {
			tab2[i]=new double[tab[i].length];
			for(int j=0;j<tab2[i].length;j++) {
				tab2[i][j]=Double.parseDouble(tab[i][j]);
			}
		}
		return tab2;
	}
	
	
	
	/**
	 * Gets the roi surface.
	 *
	 * @param r the r
	 * @return the roi surface
	 */
	public static double getRoiSurface(Roi r) {
        int x0=(int)Math.floor(r.getBounds().getMinX());
        int x1=(int)Math.floor(r.getBounds().getMaxX());
        int y0=(int)Math.floor(r.getBounds().getMinY());
        int y1=(int)Math.floor(r.getBounds().getMaxY());
        int inter=0;
        for(int x=x0;x<=x1;x++) {
            for(int y=y0;y<=y1;y++) {
                if(r.contains(x, y))inter++;
            }
        }
        return inter;
   	
    }


	
	
	
	/**
	 * Write string tab in csv.
	 *
	 * @param tab the tab
	 * @param fileName the file name
	 */
	public static void writeStringTabInCsv(String[][]tab,String fileName) {
//		System.out.println("Impression de tableau de taille "+tab.length+" x "+tab[0].length);
		try { 
			PrintStream l_out = new PrintStream(new FileOutputStream(fileName)); 
			for(int i=0;i<tab.length;i++) {
				for(int j=0;j<tab[i].length;j++) {
					l_out.print(tab[i][j]+","); 
				}
				l_out.println(""); 
			}
			l_out.flush(); 
			l_out.close(); 
			l_out=null; 
		} 
		catch(Exception e){System.out.println(e.toString());} 
	}

	/**
	 * Write string tab in csv 2.
	 *
	 * @param tab the tab
	 * @param fileName the file name
	 */
	public static void writeStringTabInCsv2(String[][]tab,String fileName) {
	//	System.out.println("Impression de tableau de taille "+tab.length+" x "+tab[0].length);
		try { 
			PrintStream l_out = new PrintStream(new FileOutputStream(fileName)); 
			for(int i=0;i<tab.length;i++) {
				for(int j=0;j<tab[i].length;j++) {
					l_out.print(tab[i][j]+(j<tab[i].length-1 ? "," : "")); 
				}
				l_out.println(""); 
			}
			l_out.flush(); 
			l_out.close(); 
			l_out=null; 
		} 
		catch(Exception e){System.out.println(e.toString());} 
	}

	/**
	 * Write double tab in csv.
	 *
	 * @param tab the tab
	 * @param fileName the file name
	 */
	public static void writeDoubleTabInCsv(double[][]tab,String fileName) {
		System.out.println("Impression de tableau de taille "+tab.length+" x "+tab[0].length);
		try { 
			PrintStream l_out = new PrintStream(new FileOutputStream(fileName)); 
			for(int i=0;i<tab.length;i++) {
				for(int j=0;j<tab[i].length;j++) {
					l_out.print(tab[i][j]+" ,"); 
				}
				l_out.println(""); 
			}
			l_out.flush(); 
			l_out.close(); 
			l_out=null; 
		} 
		catch(Exception e){System.out.println(e.toString());} 
	}
	
	/**
	 * Write double tab in csv simple.
	 *
	 * @param tab the tab
	 * @param fileName the file name
	 */
	public static void writeDoubleTabInCsvSimple(double[][]tab,String fileName) {
		System.out.println("Impression de tableau de taille "+tab.length+" x "+tab[0].length);
		try { 
			PrintStream l_out = new PrintStream(new FileOutputStream(fileName)); 
			for(int i=0;i<tab.length;i++) {
				for(int j=0;j<tab[i].length;j++) {
					l_out.print(tab[i][j]+(j<tab[i].length-1 ? "," : "")); 
				}
				l_out.println(""); 
			}
			l_out.flush(); 
			l_out.close(); 
			l_out=null; 
		} 
		catch(Exception e){System.out.println(e.toString());} 
	}

	
	
	/**
	 * Informations about system and virtual machine. Detection of Windows or Unix environments
	 *
	 * @return the system name
	 */
	public static String getSystemName(){
		String os=System.getProperty("os.name").toLowerCase();
		if(os.indexOf("win") >= 0)return "Windows system";
		if(os.indexOf("mac") >= 0)return "Mac iOs system";
		if(os.indexOf("nux") >= 0)return "Linux system";
		return "System";
	}

	/**
	 * Checks if is mac.
	 *
	 * @return true, if is mac
	 */
	public static boolean isMac() {
		String os=System.getProperty("os.name").toLowerCase();
		if(os.indexOf("mac") >= 0)return true;
		return false;
	}
	
	/**
	 * Checks if is windows OS.
	 *
	 * @return true, if is windows OS
	 */
	public static boolean isWindowsOS(){
		String os=System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}
	
	/**
	 * Gets the system needed memory.
	 *
	 * @return the system needed memory
	 */
	public static String getSystemNeededMemory(){
		String os=System.getProperty("os.name").toLowerCase();
		if(os.indexOf("win") >= 0)return "3000 MB";
		if(os.indexOf("mac") >= 0)return "3000 MB";
		if(os.indexOf("nux") >= 0)return "3000 MB";
		return "System";
	}
		
	/**
	 * Gets the nb cores.
	 *
	 * @return the nb cores
	 */
	public static int getNbCores() {
		return	Runtime.getRuntime().availableProcessors();
	}
	
	
	
	
	
	
	/**
	 * Gets the lower string in string tab.
	 *
	 * @param tab the tab
	 * @return the lower string in string tab
	 */
	public static String getLowerStringInStringTab(String[]tab) {
		if(tab.length<1)return null;
		String min=tab[0];
		for(int i=1;i<tab.length;i++) {
			if(tab[i].compareTo(min)>0)min=tab[i];
		}
		return min;
	}
	
	
	/**
	 * Adjust image on screen.
	 *
	 * @param img the img
	 * @param xPosition the x position
	 * @param yPosition the y position
	 */
	/*Adjust position of frames and images on the screen*/
	public static void adjustImageOnScreen(ImagePlus img,int xPosition,int yPosition) {
		adjustFrameOnScreen(img.getWindow(), xPosition, yPosition);
	}

	/**
	 * Adjust frame on screen.
	 *
	 * @param frame the frame
	 * @param xPosition the x position
	 * @param yPosition the y position
	 */
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
	
	/**
	 * Adjust image on screen relative.
	 *
	 * @param img1 the img 1
	 * @param img2Reference the img 2 reference
	 * @param xPosition the x position
	 * @param yPosition the y position
	 * @param distance the distance
	 */
	public static void adjustImageOnScreenRelative(ImagePlus img1,ImagePlus img2Reference,int xPosition,int yPosition,int distance) {
		adjustFrameOnScreenRelative(img1.getWindow(),img2Reference.getWindow(), xPosition, yPosition,distance);
	}
	
	/**
	 * Dimensions match.
	 *
	 * @param img1 the img 1
	 * @param img2 the img 2
	 * @return true, if successful
	 */
	public static boolean dimensionsMatch(ImagePlus img1,ImagePlus img2) {
		if(img1.getWidth()!=img2.getWidth())return false;
		if(img1.getHeight()!=img2.getHeight())return false;
		if(img1.getNSlices()!=img2.getNSlices())return false;
		return true;
	}
	
	/**
	 * Hyper stacking frames.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus hyperStackingFrames(ImagePlus[]img) {
		Concatenator con=new Concatenator();
		con.setIm5D(true);
		ImagePlus hypTemp=con.concatenate(img,true);
		int nCh=img[0].getNChannels();
		String codeStacking="xyczt";
		ImagePlus hyperImage=HyperStackConverter.toHyperStack(hypTemp, nCh, img[0].getNSlices(),img.length,codeStacking,"Grayscale");
		return hyperImage;
	}


	
	

	/*
	public static PolynomialSplineFunction getInterpolator(double[]x,double[]y,boolean forceMonotonic) {
		double[]xd=x;
		double[]yd=y;
		if(x.length==2) {
			xd=new double[] {x[0],0.5*x[0]+0.5*x[1],x[1]};
			yd=new double[] {y[0],0.5*y[0]+0.5*y[1],y[1]};
		}
		else if(x.length==1) {
			xd=new double[] {x[0],x[0],x[0]};
			yd=new double[] {y[0],y[0],y[0]};
		}
		double[]xd2=new double[xd.length+1];
		double[]yd2=new double[yd.length+1];
		for(int i=0;i<xd.length;i++) {
			xd2[i]=xd[i];
			yd2[i]=yd[i];
		}
		for(int i=1;i<xd.length;i++) {
			if(yd2[i]<yd2[i-1]) {
				System.out.println("WARNING : DECREASING TIME :"+i+" : xd["+xd[i-1]+"->"+xd[i]+"] : yd["+yd[i-1]+"->"+yd[i]+"]" );
			};
		}
		double dx=(xd[xd.length-1]-xd[xd.length-2])*100000;
		double dy=(yd[xd.length-1]-yd[xd.length-2])*100000;
		xd2[xd.length]=xd[xd.length-1]+dx;
		yd2[xd.length]=yd[xd.length-1]+dy;
		for(int i=1;i<xd.length;i++) {
			if(xd2[i]==xd2[i-1]) {
				System.out.println("CRITICAL WARNING : : NON MONOTONIC SEQUENCE :"+i+" : xd["+xd[i-1]+"->"+xd[i]+"] : yd["+yd[i-1]+"->"+yd[i]+"]" );
				for(int j=0;j<xd2.length;j++) {xd2[j]+=j*0.1;System.out.println("|"+xd2[j]+"|");}
				
			};
		}
		PolynomialSplineFunction psf=null;
		if(forceMonotonic)	psf=new MonotonicSplineInterpolator().interpolate(xd2, yd2);
		else psf=new SplineInterpolator().interpolate(xd2, yd2);
		return psf;
	}
*/
	
	
	/**
	 * Hyper stack frame to hyper stack channel.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus hyperStackFrameToHyperStackChannel(ImagePlus img) {
		ImagePlus[]imgs=VitimageUtils.stacksFromHyperstackFastBis(img);
		return VitimageUtils.hyperStackingChannels(imgs);
	}

	/**
	 * Hyper stack channel to hyper stack frame.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus hyperStackChannelToHyperStackFrame(ImagePlus img) {
		ImagePlus[]imgs=VitimageUtils.stacksFromHyperstackFastBis(img);
		return VitimageUtils.hyperStackingFrames(imgs);
	}

	/**
	 * Without extension.
	 *
	 * @param name the name
	 * @return the string
	 */
	public static String withoutExtension(String name) {
		int pos = name.lastIndexOf(".");
        if (pos == -1) return name;
        return name.substring(0, pos);
	}
	
	/**
	 * Hyper stacking channels.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus hyperStackingChannels(ImagePlus[]img) {
		Concatenator con=new Concatenator();
		con.setIm5D(true);
		ImagePlus hypTemp=con.concatenate(img,true);
		String codeStacking="xyzct";
		ImagePlus hyperImage=HyperStackConverter.toHyperStack(hypTemp, img.length, img[0].getNSlices(),1,codeStacking,"Grayscale");
		return hyperImage;
	}
	
	/**
	 * Uncrop image byte.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param dimX the dim X
	 * @param dimY the dim Y
	 * @param dimZ the dim Z
	 * @return the image plus
	 */
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
	
	/**
	 * Uncrop image float.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param dimX the dim X
	 * @param dimY the dim Y
	 * @param dimZ the dim Z
	 * @return the image plus
	 */
	public static ImagePlus uncropImageFloat(ImagePlus img,int x0,int y0,int z0,int dimX,int dimY,int dimZ) {
		if(img.getType()!=ImagePlus.GRAY32)return null;
		int oldDimX=img.getWidth();
		int oldDimY=img.getHeight();
		int oldDimZ=img.getStackSize();
		
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,32,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		for(int z=z0;z<z0+oldDimZ && z<z0+dimZ;z++) {
			float[] valsImg=(float[])img.getStack().getProcessor(z-z0+1).getPixels();
			float[] valsOut=(float[])out.getStack().getProcessor(z+1).getPixels();
			for(int x=x0;x<x0+oldDimX && x<x0+dimX;x++) {
				for(int y=y0;y<y0+oldDimY && y<y0+dimY;y++){
					valsOut[dimX*(y)+(x)]=((float)(valsImg[oldDimX*(y-y0)+(x-x0)]));
				}			
			}
		}
		return out;
	}

	
	
	
	
	

	
	
	
	
	

	/**
	 * Adjust frame on screen relative.
	 *
	 * @param currentFrame the current frame
	 * @param referenceFrame the reference frame
	 * @param xPosition the x position
	 * @param yPosition the y position
	 * @param distance the distance
	 */
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


	
	
	public static void setLutToFire(ImagePlus img) {
		WindowManager.setTempCurrentImage(img);
		new LutLoader().run("fire");
	}
	
	public static void setLutToGrays(ImagePlus img) {
		WindowManager.setTempCurrentImage(img);
		new LutLoader().run("grays");
	}
	
	public static void setLut(ImagePlus img,String str) {
		WindowManager.setTempCurrentImage(img);
		if(str.contains("ray")) setLutToGrays(img);
		else setLutToFire(img);		
	}
	
	
	/**
	 * Image checking.
	 *
	 * @param imgInit the img init
	 * @param sliMin the sli min
	 * @param sliMax the sli max
	 * @param periods the periods
	 * @param message the message
	 * @param totalDuration the total duration
	 * @param fluidVisu the fluid visu
	 */
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
		if(img.getType() != ImagePlus.COLOR_RGB)setLutToFire(img);
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

	/**
	 * Image checking.
	 *
	 * @param img the img
	 * @param message the message
	 * @param totalDuration the total duration
	 */
	public static void imageChecking(ImagePlus img,String message,double totalDuration) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,totalDuration,true);
	}

	/**
	 * Image checking fast.
	 *
	 * @param img the img
	 * @param message the message
	 */
	public static void imageCheckingFast(ImagePlus img,String message) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),2,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,2,true);
	}

	/**
	 * Image checking.
	 *
	 * @param img the img
	 * @param message the message
	 */
	public static void imageChecking(ImagePlus img,String message) {
		if (message.compareTo("")==0)imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),4,true);
		else imageChecking(img,0,img.getStackSize()-1,1,message,4,true);
	}

	/**
	 * Image checking.
	 *
	 * @param img the img
	 * @param totalDuration the total duration
	 */
	public static void imageChecking(ImagePlus img,double totalDuration) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),totalDuration,true);
	}

	/**
	 * Image checking.
	 *
	 * @param img the img
	 */
	public static void imageChecking(ImagePlus img) {
		imageChecking(img,0,img.getStackSize()-1,1,img.getTitle(),3,true);
	}
	

	/**
	 * Main automated detectors : axis detection and inoculation point detection. Usable for both MRI T1, T2, and X ray images
	 *
	 * @param img the img
	 * @param threshold the threshold
	 * @return the binary mask
	 */
/*	public static Point3d[] detectAxis(ImagePlus img1,AcquisitionType acqType){
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

		if(VitimageUtils.isRX(img))img=VitimageUtils.eraseBorder(img);
		if(debug)imageChecking(img,"after Erase ");
		if(!VitimageUtils.isT2(img)) {
			
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
*/
	
	
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


	/**
	 * Gets the binary mask unary float.
	 *
	 * @param img the img
	 * @param threshold the threshold
	 * @return the binary mask unary float
	 */
	public static ImagePlus getBinaryMaskUnaryFloat(ImagePlus img,double threshold) {
		ImagePlus result=getBinaryMaskUnary(img,threshold);
		result=convertToFloat(result);
		return result;
	}
	
	
	/**
	 * Gets the binary mask unary.
	 *
	 * @param img the img
	 * @param threshold the threshold
	 * @return the binary mask unary
	 */
	public static ImagePlus getBinaryMaskUnary(ImagePlus img,double threshold) {
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
						if( (tabImg[dimX*y+x] & 0xff) >= (byte)(((int)Math.round(threshold)) & 0xff)  )tabRet[dimX*y+x]=(byte)(1 & 0xff);
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
						if( (tabImg[dimX*y+x] & 0xffff) >= (short)(((int)Math.round(threshold)) & 0xffff)  )tabRet[dimX*y+x]=(byte)(1 & 0xff);
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
						if( (tabImg[dimX*y+x]) >= threshold )tabRet[dimX*y+x]=(byte)(1 & 0xff);
						else tabRet[dimX*y+x]=(byte)(0 & 0xff);
					}
				}
			}
		}
		else VitiDialogs.notYet("getBinary Mask type "+type);
		return ret;
	}

	
	/**
	 * Connexe 2 d no fuck with volume.
	 *
	 * @param img the img
	 * @param threshLow the thresh low
	 * @param threshHigh the thresh high
	 * @param volumeLowInPixels the volume low in pixels
	 * @param volumeHighInPixels the volume high in pixels
	 * @param connexity the connexity
	 * @param selectByVolume the select by volume
	 * @param noVerbose the no verbose
	 * @return the image plus
	 */
	public static ImagePlus connexe2dNoFuckWithVolume(ImagePlus img,double threshLow,double threshHigh,double volumeLowInPixels,double volumeHighInPixels,int connexity,int selectByVolume,boolean noVerbose) {
		double voxVolume=VitimageUtils.getVoxelVolume(img);
		return connexe2d(img,threshLow,threshHigh,volumeLowInPixels*voxVolume,volumeHighInPixels*voxVolume,connexity,selectByVolume,noVerbose);
	}
	
	
	
	/**
	 * Connexe 2 d.
	 *
	 * @param img the img
	 * @param threshLow the thresh low
	 * @param threshHigh the thresh high
	 * @param volumeLowSI the volume low SI
	 * @param volumeHighSI the volume high SI
	 * @param connexity the connexity
	 * @param selectByVolume the select by volume
	 * @param noVerbose the no verbose
	 * @return the image plus
	 */
	public static ImagePlus connexe2d(ImagePlus img,double threshLow,double threshHigh,double volumeLowSI,double volumeHighSI,int connexity,int selectByVolume,boolean noVerbose) {
		ImagePlus []tmpTab=stackToSlices(img);
		for(int i=0;i<tmpTab.length;i++) {
			if((!noVerbose) && ((i%20)==0))System.out.println("Total="+tmpTab.length+"  ");
			if((!noVerbose))System.out.print(i+" ");
			tmpTab[i]=VitimageUtils.connexe(tmpTab[i], threshLow, threshHigh, volumeLowSI, volumeHighSI, connexity, selectByVolume, noVerbose);
		}
		return slicesToStack(tmpTab);
	}
		
	
	
	/**
	 * Stack to slices.
	 *
	 * @param img the img
	 * @return the image plus[]
	 */
	public static ImagePlus[]stackToSlices(ImagePlus img){
		int Z=img.getNSlices();
		ImagePlus []ret=new ImagePlus[Z];
		for(int z=0;z<Z;z++) {
			ret[z]=new Duplicator().run(img, 1, 1, z+1, z+1, 1, 1);
			VitimageUtils.adjustImageCalibration(ret[z],img);
			ret[z].getStack().setSliceLabel(img.getStack().getSliceLabel(z+1),1);
		}
		return ret;
	}	

	/**
	 * Slices to stack.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus slicesToStack(ImagePlus[]imgs) {
		ImagePlus img= ImagesToStack.run(imgs);
		for(int i=0;i<imgs.length;i++) {
			img.getStack().setSliceLabel(imgs[i].getStack().getSliceLabel(1), i+1);
		}
		return img;
	}

	
	/**
	 * Slices with channels to stack with channels.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus slicesWithChannelsToStackWithChannels(ImagePlus[]imgs) {
		int NC=imgs[0].getNChannels();
		int NZ=imgs.length;
		ImagePlus[][] chanTab=new ImagePlus [NC][NZ];
		ImagePlus[] perChanTab=new ImagePlus [NC];
		for(int i=0;i<NC;i++) {
			for(int j=0;j<NZ;j++)chanTab[i][j]=new Duplicator().run(imgs[j],i+1,i+1,1,1,1,1);
			perChanTab[i]=VitimageUtils.slicesToStack(chanTab[i]);
		}
		ImagePlus res=hyperStackingChannels(perChanTab);
		res.setC(1);
		res.setDisplayRange(0,255);
		res.setC(2);
		res.setDisplayRange(0,22);
		
		return res;
	}

	
	
	/**
	 * Connexe binary easier params connexity selectvol.
	 *
	 * @param img the img
	 * @param connexity the connexity
	 * @param selectByVolume the select by volume
	 * @return the image plus
	 */
	public static ImagePlus connexeBinaryEasierParamsConnexitySelectvol(ImagePlus img,int connexity,int selectByVolume) {
		return connexe(img,0.5,256,0,1E100,connexity,selectByVolume,true);
	}
	
	/**
	 * Connected components utilities.
	 *
	 * @param img the img
	 * @param threshLow the thresh low
	 * @param threshHigh the thresh high
	 * @param volumeLowInPixels the volume low in pixels
	 * @param volumeHighInPixels the volume high in pixels
	 * @param connexity the connexity
	 * @param selectByVolume the select by volume
	 * @param noVerbose the no verbose
	 * @return the image plus
	 */
	public static ImagePlus connexeNoFuckWithVolume(ImagePlus img,double threshLow,double threshHigh,double volumeLowInPixels,double volumeHighInPixels,int connexity,int selectByVolume,boolean noVerbose) {
		double voxVolume=VitimageUtils.getVoxelVolume(img);
		return connexe(img,threshLow,threshHigh,volumeLowInPixels*voxVolume,volumeHighInPixels*voxVolume,connexity,selectByVolume,noVerbose);
	}

	
	
	/**
	 * Connexe.
	 *
	 * @param img the img
	 * @param threshLow the thresh low
	 * @param threshHigh the thresh high
	 * @param volumeLowSI the volume low SI
	 * @param volumeHighSI the volume high SI
	 * @param connexity the connexity
	 * @param selectByVolume the select by volume
	 * @param noVerbose the no verbose
	 * @return the image plus
	 */
	public static ImagePlus connexe(ImagePlus img,double threshLow,double threshHigh,double volumeLowSI,double volumeHighSI,int connexity,int selectByVolume,boolean noVerbose) {
		boolean debug=!noVerbose;
		if(debug)System.out.println("Depart connexe");
		int yMax=img.getHeight();
		int xMax=img.getWidth();
		int zMax=img.getStack().getSize();
		int x2=xMax/2;
		int y2=yMax/2;
		int z2=zMax/2;
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double voxVolume=vX*vY*vZ;
		int[][]connexions;
		int[]volume;
		double[]sumX;
		double[]sumY;
		double[]sumZ;
		if(debug)System.out.println("Allocations 1, en MegaInt : "+(0.000001*xMax*yMax*zMax));
		int[][][]tabIn=new int[xMax][yMax][zMax];
		if(0.000001*xMax*yMax*zMax>50) {
			connexions=new int[xMax*yMax*zMax/20][2];
			volume=new int[xMax*yMax*zMax/20];
			sumX=new double[xMax*yMax*zMax/20];
			sumY=new double[xMax*yMax*zMax/20];
			sumZ=new double[xMax*yMax*zMax/20];
		}
		else {
			connexions=new int[xMax*yMax*zMax/2][2];
			volume=new int[xMax*yMax*zMax/2];
			sumX=new double[xMax*yMax*zMax/2];
			sumY=new double[xMax*yMax*zMax/2];
			sumZ=new double[xMax*yMax*zMax/2];
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
							//agrandir le tableau de volumes de 100 %
							int[]volumeBigger=new int[(20*volume.length)/10];
							double[]sumXBigger=new double[(20*sumX.length)/10];
							double[]sumYBigger=new double[(20*sumY.length)/10];
							double[]sumZBigger=new double[(20*sumZ.length)/10];
							if(debug)System.out.println("Volumes array touch to limit. Raising size to "+(14*volume.length)/10);
							for(int i=0;i<curComp;i++) {
								volumeBigger[i]=volume[i];
								sumXBigger[i]=sumX[i];
								sumYBigger[i]=sumY[i];
								sumZBigger[i]=sumZ[i];
							}
							volume=volumeBigger;
							sumX=sumXBigger;
							sumY=sumYBigger;
							sumZ=sumZBigger;
						}

						volume[curComp]++;
						sumX[curComp]+=x-x2;
						sumY[curComp]+=y-y2;
						sumZ[curComp]+=z-z2;
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
									sumX[tabIn[x][y][z]]+=(x+neighbours[nei][0])-x2;
									sumY[tabIn[x][y][z]]+=(y+neighbours[nei][1])-y2;
									sumZ[tabIn[x][y][z]]+=(z+neighbours[nei][2])-z2;
								}
								else {
									if(indexConnexions==connexions.length) {
										//agrandir le tableau de connexions de 20 %
										int[][]connexionsBigger=new int[(14*connexions.length)/10][2];
										if(debug)System.out.println("Connexions array touch to limit. Raising size to "+(14*connexions.length)/10);
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
		int[]lut = resolveConnexitiesGroupsAndExclude(connexions,indexConnexions,curComp+1,volume,sumX,sumY,sumZ,volumeLowSI/voxVolume,volumeHighSI/voxVolume,selectByVolume,noVerbose);


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
	
	/**
	 * Resolve connexities groups and exclude.
	 *
	 * @param connexions the connexions
	 * @param nbCouples the nb couples
	 * @param n the n
	 * @param volume the volume
	 * @param sumX the sum X
	 * @param sumY the sum Y
	 * @param sumZ the sum Z
	 * @param volumeLowP the volume low P
	 * @param volumeHighP the volume high P
	 * @param selectByVolume the select by volume
	 * @param noVerbose the no verbose
	 * @return the int[]
	 */
	@SuppressWarnings("unchecked")
	public static int[] resolveConnexitiesGroupsAndExclude(int  [][] connexions,int nbCouples,int n,int []volume,double[]sumX,double[]sumY,double[]sumZ,double volumeLowP,double volumeHighP,int selectByVolume,boolean noVerbose) {
		int[]prec=new int[n];
		int[]lut=new int[n+1];
		int[]next=new int[n];
		int[]label=new int[n];
		for(int i=0;i<n;i++) {label[i]=i;prec[i]=0;next[i]=0;}

		int indA,indB,valMin,indMin,indMax;
		for(int couple=0;couple<nbCouples;couple++) {
			indA=connexions[couple][0];
			indB=connexions[couple][1];
			if(label[indA]==label[indB])continue;
			if(label[indA]<label[indB]) {
				valMin=label[indA];
				indMin=indA;
				indMax=indB;
			}
			else {
				valMin=label[indB];
				indMin=indB;
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
				sumX[label[i]]+=sumX[i];
				sumX[i]=0;
				sumY[label[i]]+=sumY[i];
				sumY[i]=0;
				sumZ[label[i]]+=sumZ[i];
				sumZ[i]=0;
			}
		}

		
		
		if(selectByVolume<0) {
			int selectedIndex=0;
			double distMin=1E100;
			for(int i=0;i<n;i++) {
				double dist=VitimageUtils.distance(sumX[i]/volume[i],sumY[i]/volume[i],sumZ[i]/volume[i],0,0,0);
				if(dist<distMin) {
					distMin=dist;
					selectedIndex=i;
				}
			}
			for(int i=0;i<n;i++) {
				if(label[i]==selectedIndex)			lut[i]=255;
			}
			return lut;
		}
		else {
			
			//copy and sort volumes
			Object [][]tabSort=new Object[n][2];
			int selectedIndex=0;
			for (int i=0;i<n ;i++) {
				tabSort[i][0]=new Double(volume[i]);
				tabSort[i][1]=new Integer(i);
			}
	
	
			Arrays.sort(tabSort,new VolumeComparator());
			if(selectByVolume>n)selectByVolume=n;
			if(selectByVolume>0)selectedIndex=((Integer)(tabSort[n-selectByVolume][1])).intValue();
	
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
	}


	
	/**
	 * Sub XYZ.
	 *
	 * @param imgIn the img in
	 * @param factors the factors
	 * @param bilinear the bilinear
	 * @return the image plus
	 */
	public static ImagePlus subXYZ(ImagePlus imgIn,double[]factors,boolean bilinear) {
		System.out.print("Subsampling XYZ with factors "+TransformUtils.stringVector(factors,""));
		ImagePlus out=VitimageUtils.imageCopy(imgIn);
		int []dimsNew=VitimageUtils.getDimensions(imgIn);
		System.out.print(TransformUtils.stringVector(dimsNew,"Dims before"));
		for(int d=0;d<3;d++) {
			dimsNew[d]=(int)(Math.ceil( dimsNew[d]*factors[d] ));
		}
		System.out.print(TransformUtils.stringVector(dimsNew,"Dims target"));
//		out.show();
		out.setTitle("Temp for resizing");
		System.out.println("\nScale..."+", "+"x="+factors[0]+" y="+factors[1]+" z="+factors[2]+" width="+dimsNew[0]+" height="+dimsNew[1]+" depth="+dimsNew[2]+" interpolation="+(bilinear ?"Bilinear average":"None")+" process create");
		IJ.run(out, "Scale...", "x="+factors[0]+" y="+factors[1]+" z="+factors[2]+" width="+dimsNew[0]+" height="+dimsNew[1]+" depth="+dimsNew[2]+" interpolation="+(bilinear ?"Bilinear average":"None")+" process create");
		out=IJ.getImage();
		out.hide();
		System.out.println();
		return out;
	}

	
	
	
	public static ImagePlus actualizeData(ImagePlus source,ImagePlus dest) {
		return actualizeDataOld(source,dest);
	}

	
	/**
	 * Actualize data.
	 *
	 * @param source the source
	 * @param dest the dest
	 * @return the image plus
	 */
	/*Various implementations of image composition, for registration visual assessment */
	public static ImagePlus actualizeDataOld(ImagePlus source,ImagePlus dest) {
		int[]dims=VitimageUtils.getDimensions(source);
		int Z=dims[2];int Y=dims[1];int X=dims[0];
		if(source.getType() == ImagePlus.GRAY8) {
			byte[][] valsSource=new byte[Z][];
			byte[][] valsDest=new byte[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(byte [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(byte [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
				//valsSource[z]=valsDest[z];
			}			
		}
		if(source.getType() == ImagePlus.GRAY16) {
			short[][] valsSource=new short[Z][];
			short[][] valsDest=new short[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(short [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(short [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
				//valsSource[z]=valsDest[z];
			}			
		}
		if(source.getType() == ImagePlus.GRAY32) {
			float[][] valsSource=new float[Z][];
			float[][] valsDest=new float[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(float [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(float [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
				//valsSource[z]=valsDest[z];
			}			
		}

		if(source.getType() == ImagePlus.COLOR_RGB) {
			int[][] valsSource=new int[Z][];
			int[][] valsDest=new int[Z][];
			for(int z=0;z<Z;z++) {
				valsSource[z]=(int [])source.getStack().getProcessor(z+1).getPixels();
				valsDest[z]=(int [])dest.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<X;x++)for(int y=0;y<Y;y++)valsDest[z][y*X+x]=valsSource[z][y*X+x];
				//valsSource[z]=valsDest[z];
			}			
		}
		//dest.changes=true;
		dest.updateAndDraw();
		return dest;
		
	}
	





	public static ImagePlus actualizeDataMultiThread(ImagePlus source, ImagePlus dest) {
		int[] dims = VitimageUtils.getDimensions(source);
		int Z = dims[2];
		int Y = dims[1];
		int X = dims[0];

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for (int z = 0; z < Z; z++) {
			Runnable task = createCopyTask(source, dest, z, X, Y);
			executor.execute(task);
		}

		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		dest.updateAndDraw();
		return dest;
	}

	private static Runnable createCopyTask(ImagePlus source, ImagePlus dest, int z, int X, int Y) {
		return () -> {
			Object sourcePixels = source.getStack().getProcessor(z + 1).getPixels();
			Object destPixels = dest.getStack().getProcessor(z + 1).getPixels();

			switch (source.getType()) {
				case ImagePlus.GRAY8:
					byte[] sourceValsByte = (byte[]) sourcePixels;
					byte[] destValsByte = (byte[]) destPixels;
					System.arraycopy(sourceValsByte, 0, destValsByte, 0, X * Y);
					break;
				case ImagePlus.GRAY16:
					short[] sourceValsShort = (short[]) sourcePixels;
					short[] destValsShort = (short[]) destPixels;
					System.arraycopy(sourceValsShort, 0, destValsShort, 0, X * Y);
					break;
				case ImagePlus.GRAY32:
					float[] sourceValsFloat = (float[]) sourcePixels;
					float[] destValsFloat = (float[]) destPixels;
					System.arraycopy(sourceValsFloat, 0, destValsFloat, 0, X * Y);
					break;
				case ImagePlus.COLOR_RGB:
					int[] sourceValsInt = (int[]) sourcePixels;
					int[] destValsInt = (int[]) destPixels;
					System.arraycopy(sourceValsInt, 0, destValsInt, 0, X * Y);
					break;
				default:
					throw new IllegalArgumentException("Unsupported image type");
			}
		};
	}








	/**
	 * Composite RGB double jet.
	 *
	 * @param img1 the img 1
	 * @param img2 the img 2
	 * @param img3 the img 3
	 * @param title the title
	 * @param mask the mask
	 * @param teinte the teinte
	 * @return the image plus
	 */
	public static ImagePlus compositeRGBDoubleJet(ImagePlus img1,ImagePlus img2,ImagePlus img3,String title,boolean mask,int teinte) {
		ImagePlus img1Source=new Duplicator().run(img1);
		ImagePlus img2Source=new Duplicator().run(img2);
		ImagePlus img3Source=new Duplicator().run(img3);
		img1Source.resetDisplayRange();
		img2Source.resetDisplayRange();
		img3Source.resetDisplayRange();
		convertToGray8(img1Source);
		convertToGray8(img2Source);
		convertToGray8(img3Source);
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
	
	/**
	 * Composite RGB double.
	 *
	 * @param img1Source the img 1 source
	 * @param img2Source the img 2 source
	 * @param img3Source the img 3 source
	 * @param coefR the coef R
	 * @param coefG the coef G
	 * @param coefB the coef B
	 * @param title the title
	 * @return the image plus
	 */
	public static ImagePlus compositeRGBDouble(ImagePlus img1Source,ImagePlus img2Source,ImagePlus img3Source,double coefR,double coefG,double coefB,String title){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		ImagePlus img3=new Duplicator().run(img3Source);
		img1.getProcessor().resetMinAndMax();
		img2.getProcessor().resetMinAndMax();
		img3.getProcessor().resetMinAndMax();
		convertToGray8(img1);
		convertToGray8(img2);
		convertToGray8(img3);
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),img3.getStack(),true);
		ImagePlus img=new ImagePlus(title,is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
		
	/**
	 * Composite RGBL byte.
	 *
	 * @param img1Source the img 1 source
	 * @param img2Source the img 2 source
	 * @param img3Source the img 3 source
	 * @param img4Source the img 4 source
	 * @param coefR the coef R
	 * @param coefG the coef G
	 * @param coefB the coef B
	 * @param coefL the coef L
	 * @return the image plus
	 */
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
		
	/**
	 * Composite no adjust of.
	 *
	 * @param img1Source the img 1 source
	 * @param img2Source the img 2 source
	 * @return the image plus
	 */
	public static ImagePlus compositeNoAdjustOf(ImagePlus img1Source,ImagePlus img2Source){
		ImagePlus img1=VitimageUtils.imageCopy(img1Source);
		ImagePlus img2=VitimageUtils.imageCopy(img2Source);
		convertToGray8(img1);
		convertToGray8(img2);
		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}



	public static void convertToGray8(ImagePlus img) {
		if( img.getImageStackSize() > 1)  (new StackConverter( img )).convertToGray8();
	    else (new ImageConverter( img )).convertToGray8();		
	}
	
	/**
	 * Composite no adjust of.
	 *
	 * @param img1 the img 1
	 * @param img2 the img 2
	 * @param title the title
	 * @return the image plus
	 */
	public static ImagePlus compositeNoAdjustOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeNoAdjustOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}

	/**
	 * Composite of.
	 *
	 * @param img1Source the img 1 source
	 * @param img2Source the img 2 source
	 * @return the image plus
	 */
	public static ImagePlus compositeOf(ImagePlus img1Source,ImagePlus img2Source){
		ImagePlus img1=new Duplicator().run(img1Source);
		ImagePlus img2=new Duplicator().run(img2Source);
		img1.resetDisplayRange();
		img2.resetDisplayRange();
		convertToGray8(img1);
		convertToGray8(img2);

		ImageStack is=RGBStackMerge.mergeStacks(img1.getStack(),img2.getStack(),null,true);
		ImagePlus img=new ImagePlus("Composite",is);
		VitimageUtils.adjustImageCalibration(img, img1Source);
		return img;
	}
	
	/**
	 * Composite of.
	 *
	 * @param img1 the img 1
	 * @param img2 the img 2
	 * @param title the title
	 * @return the image plus
	 */
	public static ImagePlus compositeOf(ImagePlus img1,ImagePlus img2,String title){
		ImagePlus composite=compositeOf(img1,img2);
		composite.setTitle(title);
		return composite;
	}


	
	
	
	
	
	
	
	/**
	 * New thread array.
	 *
	 * @param n the n
	 * @return the thread[]
	 */
	/* Various thread helpers. Three first from Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static Thread[] newThreadArray(int n) {  
		return new Thread[n];  
	}  

	/**
	 * Start and join.
	 *
	 * @param threads the threads
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
	
	/**
	 * Start no join.
	 *
	 * @param threads the threads
	 */
	public static void startNoJoin(Thread[] threads){  
		for (int ithread = 0; ithread < threads.length; ++ithread){  
			threads[ithread].setPriority(Thread.NORM_PRIORITY);  
			threads[ithread].start();  
		}
	}   

	/**
	 * Start and join.
	 *
	 * @param thread the thread
	 */
	public static void startAndJoin(Thread thread){  
		thread.setPriority(Thread.NORM_PRIORITY);  
		thread.start();  
		try{     
			thread.join();  
		} catch (InterruptedException ie) {  	System.out.println(ie.getStackTrace());throw new RuntimeException(ie);  }  
	}   
	
	/**
	 * List for threads.
	 *
	 * @param nbP the nb P
	 * @param nbProc the nb proc
	 * @return the int[][]
	 */
	public static int[][]listForThreads(int nbP,int nbProc){
		int [][]indexes=new int[nbProc][];
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[]arrs=new ArrayList[nbProc];
		for(int pro=0;pro<nbProc;pro++) arrs[pro]=new ArrayList<Integer>();
		for(int ind=0;ind<nbP;ind++)arrs[ind%nbProc].add(new Integer(ind));
		for(int pro=0;pro<nbProc;pro++) {
			indexes[pro]=new int[arrs[pro].size()];
			for (int i=0;i<arrs[pro].size();i++) indexes[pro][i]=(Integer)(arrs[pro].get(i)); 
		}
		return indexes;
	}

	

	
	
	
	
	
	
	///These ones should be refactored : it is only for wekaSave
	
	
	/**
	 * Stacks from hyperstack.
	 *
	 * @param hyper the hyper
	 * @param nb the nb
	 * @return the image plus[]
	 */
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

	/**
	 * Stacks from hyperstack bis.
	 *
	 * @param hyper the hyper
	 * @return the image plus[]
	 */
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
			
	/**
	 * Stacks from hyperstack fast.
	 *
	 * @param hyper the hyper
	 * @param nb the nb
	 * @return the image plus[]
	 */
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

	/**
	 * Composite RGB byte.
	 *
	 * @param img1Source the img 1 source
	 * @param img2Source the img 2 source
	 * @param img3Source the img 3 source
	 * @param coefR the coef R
	 * @param coefG the coef G
	 * @param coefB the coef B
	 * @return the image plus
	 */
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

	/**
	 * Composite RGB byte tab.
	 *
	 * @param imgSource the img source
	 * @return the image plus
	 */
	public static ImagePlus compositeRGBByteTab(ImagePlus[] imgSource){
		return compositeRGBByte(imgSource[0],imgSource[1],imgSource[2],1,1,1);
	}

	
	
	
	
	//End
	
	
	
	
	/**
	 * Stacks from hyperstack fast bis.
	 *
	 * @param hyper the hyper
	 * @return the image plus[]
	 */
	public static ImagePlus[]stacksFromHyperstackFastBis(ImagePlus hyper){
		int nbZ=hyper.getNSlices();
		int nbT=hyper.getNFrames();
		int nbC=hyper.getNChannels();
		int nb=nbT*nbC;
		ImagePlus []ret=new ImagePlus[nb];
		for(int ic=0;ic<nbC;ic++) {
			for(int it=0;it<nbT;it++) {
				int i=ic*nbT+it;
				//IJ.log("Hyperstack to stack tab... "+ic+"/"+nbC+" ,  "+it+"/"+nbT);
				ret[i] = new Duplicator().run(hyper, 1+ic, 1+ic, 1, nbZ, 1+it, 1+it);
				VitimageUtils.adjustImageCalibration(ret[i],hyper);
				String []sTab=new String[nbZ];
				for(int z=0;z<nbZ;z++) sTab[z]=ret[i].getStack().getSliceLabel(z+1);
				WindowManager.setTempCurrentImage(ret[i]);
				new LutLoader().run("grays");
				for(int z=0;z<nbZ;z++) ret[i].getStack().setSliceLabel(sTab[z],z+1);
			}
		}
		return ret;
	}
	
	
	

	
	


	
	
	
	
	/**
	 * Draw rectangle in image.
	 *
	 * @param imgIn the img in
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param xf the xf
	 * @param yf the yf
	 * @param value the value
	 * @return the image plus
	 */
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

	/**
	 * Draw parallepiped in image.
	 *
	 * @param imgIn the img in
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param xf the xf
	 * @param yf the yf
	 * @param zf the zf
	 * @param value the value
	 * @return the image plus
	 */
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
	
	/**
	 * Draw cylinder in image.
	 *
	 * @param imgIn the img in
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param value the value
	 * @return the image plus
	 */
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
	
	/**
	 * Gets the pixel index.
	 *
	 * @param x the x
	 * @param X the x
	 * @param y the y
	 * @return the pixel index
	 */
	public static int getPixelIndex(int x,int X,int y){
		return X*y+x;
	}
	
	
	/**
	 * Draw circle no fill in image.
	 *
	 * @param imgIn the img in
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param value the value
	 * @param thickness the thickness
	 * @return the image plus
	 */
	public static ImagePlus drawCircleNoFillInImage(ImagePlus imgIn,double ray,int x0,int y0,int z0,int value,int thickness) {
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
					if( (distance < (ray+thickness/2.0)) && (distance>(ray-thickness/2.0)) ) {
						valsImg[z][xM*y+x]=  (byte)( value & 0xff);
					}
				}
			}			
		}
		return img;
	}

	
	
	
	/**
	 * Draw circle into image.
	 *
	 * @param img the img
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param value the value
	 */
	public static void  drawCircleIntoImage(ImagePlus img,double ray,int x0,int y0,int z0,int value) {
		if(img.getType() == ImagePlus.GRAY32)drawCircleIntoImageFloat(img, ray,x0, y0, z0, value);
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
		int zz0=(int) Math.round(Math.max(z0-ray/voxSZ, 0));
		int zz1=(int) Math.round(Math.min(z0+ray/voxSZ, zM-1));
		int xx0=(int) Math.round(Math.max(x0-ray/voxSX, 0));
		int xx1=(int) Math.round(Math.min(x0+ray/voxSX, xM-1));
		int yy0=(int) Math.round(Math.max(y0-ray/voxSY, 0));
		int yy1=(int) Math.round(Math.min(y0+ray/voxSY, yM-1));
		for(int z=zz0;z<=zz1;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=xx0;x<=xx1;x++) {
				for(int y=yy0;y<=yy1;y++) {
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
		
	}

	/**
	 * Draw circle into image float.
	 *
	 * @param img the img
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param value the value
	 */
	public static void  drawCircleIntoImageFloat(ImagePlus img,double ray,int x0,int y0,int z0,double value) {
		if(img.getType() != ImagePlus.GRAY32)return;
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		double voxSX=img.getCalibration().pixelWidth;
		double voxSY=img.getCalibration().pixelHeight;
		double voxSZ=img.getCalibration().pixelDepth;
		double realDisX;
		double realDisY;
		double realDisZ;
		float[][] valsImg=new float[zM][];
		double distance;
		int zz0=(int) Math.round(Math.max(z0-ray/voxSZ, 0));
		int zz1=(int) Math.round(Math.min(z0+ray/voxSZ, zM-1));
		int xx0=(int) Math.round(Math.max(x0-ray/voxSX, 0));
		int xx1=(int) Math.round(Math.min(x0+ray/voxSX, xM-1));
		int yy0=(int) Math.round(Math.max(y0-ray/voxSY, 0));
		int yy1=(int) Math.round(Math.min(y0+ray/voxSY, yM-1));
		for(int z=zz0;z<=zz1;z++) {
			valsImg[z]=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=xx0;x<=xx1;x++) {
				for(int y=yy0;y<=yy1;y++) {
					realDisX=(x-x0)*voxSX;
					realDisY=(y-y0)*voxSY;
					realDisZ=(z-z0)*voxSZ;
					distance=Math.sqrt( realDisX * realDisX  +  realDisY * realDisY  + realDisZ * realDisZ  );
					if(distance < ray) {
						valsImg[z][xM*y+x]=  (float) value;
					}
				}
			}			
		}
		
	}

	
	/**
	 * Draw circle in image.
	 *
	 * @param imgIn the img in
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param value the value
	 * @return the image plus
	 */
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

	/**
	 * Draw point at pixel coordinates in image modifying it.
	 *
	 * @param imgIn the img in
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param value the value
	 * @return the image plus
	 */
	public static ImagePlus drawPointAtPixelCoordinatesInImageModifyingIt(ImagePlus imgIn,int x0,int y0,int z0,int value) {
		if(imgIn.getType() != ImagePlus.GRAY8)return imgIn;
		int xM=imgIn.getWidth();
		byte[] valsImg=(byte[])imgIn.getStack().getProcessor(z0+1).getPixels();
		valsImg[xM*y0+x0]=  (byte)( value & 0xff);
		return imgIn;
	}
		
	/**
	 * Draw thick line in float image.
	 *
	 * @param imgIn the img in
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param vectZ the vect Z
	 * @param value the value
	 * @return the image plus
	 */
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
		for(int z=0;z<zM;z++) {
			valsImg[z]=(float [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (float)(value);
					}
				}
			}			
		}
		return img;
	}

	/**
	 * Draw thick line in image.
	 *
	 * @param imgIn the img in
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param vectZ the vect Z
	 * @return the image plus
	 */
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
		for(int z=0;z<zM;z++) {
			valsImg[z]=(byte [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (byte)( 255 & 0xff);
					}
				}
			}			
		}
		return img;
	}



	
	
	/**
	 * Draw thick line in short image.
	 *
	 * @param imgIn the img in
	 * @param ray the ray
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param vectZ the vect Z
	 * @return the image plus
	 */
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
		for(int z=0;z<zM;z++) {
			valsImg[z]=(short [])img.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					vectCur=new double[] {(x-x0)*voxSX,(y-y0)*voxSY,(z-z0)*voxSZ};
					distanceLine=TransformUtils.norm(TransformUtils.vectorialSubstraction(vectCur,TransformUtils.proj_u_of_v(vectZ, vectCur)));
					if(distanceLine < ray) {
						valsImg[z][xM*y+x]=  (short)( 255 & 0xffff);
					}
				}
			}			
		}
		return img;
	}
	
	/**
	 * Erase border.
	 *
	 * @param imgIn the img in
	 * @return the image plus
	 */
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
	
	/**
	 * Write black text on image.
	 *
	 * @param text the text
	 * @param img the img
	 * @param fontSize the font size
	 * @param numLine the num line
	 * @return the image plus
	 */
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

	
	
	
	
	
	
	
	/**
	 *  Helpers for outlier detection and removal---------------------------------------------------------.
	 *
	 * @param val the val
	 * @param tabIn the tab in
	 * @param mask the mask
	 * @param eRatio the e ratio
	 * @return the object[]
	 */	
    public static Object[] tuckeyIsOutlier(double val,double []tabIn,double[]mask,double eRatio) {
	    double[]tabStats=getQuartiles(tabIn,mask);
	    double interQuart=tabStats[2]-tabStats[0];
    	if(val<tabStats[1]-eRatio/2.0*interQuart)return new Object[] {true,tabStats[1],tabStats[1]};
    	if(val>tabStats[1]+eRatio/2.0*interQuart)return new Object[] {true,tabStats[1],tabStats[1]};
    	return new Object[]{false,val,tabStats[1]};
    }
    
    /**
     * MA de is outlier.
     *
     * @param val the val
     * @param tabIn the tab in
     * @param mask the mask
     * @param eRatio the e ratio
     * @return the object[]
     */
    public static Object[] MADeIsOutlier(double val,double []tabIn,double[]mask,double eRatio) {
    	double factorB=1.4826;//see reference Leys at al. JESP 2013 - Detecting outliers:
    	double[]tabStats=MADeStatsDoubleSided(tabIn, mask);
    	if(tabStats.length==1)return new Object[] {true,val};
    	double madDown=tabStats[0]-tabStats[1];
    	double madUp=tabStats[2]-tabStats[0];
    	if(val<tabStats[0]-eRatio*factorB*madDown)return new Object[] {true,tabStats[0],tabStats[0]};
    	if(val>tabStats[0]+eRatio*factorB*madUp)return new Object[] {true,tabStats[0],tabStats[0]};
    	return(new Object[] {false,val,tabStats[0]});
    }
	
 
    /**
     * MA de stats double sided.
     *
     * @param tab1 the tab 1
     * @return the double[]
     */
    public static double[] MADeStatsDoubleSided(ArrayList<Double>tab1) {
    	double[]tab2=new double[tab1.size()];
    	for(int i=0;i<tab1.size();i++)tab2[i]=tab1.get(i);
    	return MADeStatsDoubleSided(tab2,null);
    }
    
    /**
     * MA de stats double sided.
     *
     * @param tab the tab
     * @return the double[]
     */
    public static double[] MADeStatsDoubleSided(double[]tab) {
    	return MADeStatsDoubleSided(tab,null);
    }
   
    /**
     * MA de stats double sided.
     *
     * @param tabIn the tab in
     * @param mask the mask
     * @return the double[]
     */
    public static double[] MADeStatsDoubleSided(double[] tabIn,double []mask) {
		if (tabIn.length==0)return null;
		if(mask==null) {mask=new double[tabIn.length];for(int i=0;i<tabIn.length;i++)mask[i]=1;}
		List<Double> l=new ArrayList<Double>();
		for(int i=0;i<tabIn.length;i++)if(mask[i]>0)l.add(tabIn[i]);
		Double []tab=(Double[])(l.toArray(new Double[l.size()]));
	
		Arrays.sort(tab);
		//System.out.print("[");
	//for(int i=0;i<tab.length;i++)System.out.print(tab[i]+" ,");
	//System.out.println("]");

		if(tab.length<5) {
			if(tab.length==0)return new double[] {0,0,0};
			if(tab.length==1)return new double[] {tab[0],tab[0],tab[0]};
			if(tab.length==2)return new double[] {tab[0],tab[0],tab[1]};
			if(tab.length==3)return new double[] {tab[1],tab[0],tab[2]};
			if(tab.length==4)return new double[] {tab[1],tab[0],tab[3]};
		}
		
		double valMed=tab[tab.length/2];
	    double valMedUp=0;
	    double valMedDown=0;
	    if (tab.length%2==0)valMed=(tab[tab.length/2-1]+tab[tab.length/2])/2.0;
	
	    if (tab.length%4==0) {
	    	valMedDown=(tab[tab.length/4-1]+tab[tab.length/4])/2.0;
	    	valMedUp=(tab[3*tab.length/4-1]+tab[3*tab.length/4])/2.0;
	    }
	    if (tab.length%4==1) {
	    	valMedDown=tab[tab.length/4];
	    	valMedUp=tab[3*tab.length/4];
	    }
	    if (tab.length%4==2) {
	    	valMedDown=tab[tab.length/4];
	    	valMedUp=tab[3*tab.length/4];
	    }
	    if (tab.length%4==3) {
	    	valMedDown=(tab[tab.length/4+1]+tab[tab.length/4])/2.0;
	    	valMedUp=(tab[3*tab.length/4-1]+tab[3*tab.length/4])/2.0;
	    }
	    return new double[] {valMed,valMedDown,valMedUp};
    }
    
    /**
     * Gets the quartiles.
     *
     * @param tabInTmp the tab in tmp
     * @param mask the mask
     * @return the quartiles
     */
    public static double[] getQuartiles(double[] tabInTmp,double []mask) {
	  	if (tabInTmp.length==0)return null;
		if(mask==null) {mask=new double[tabInTmp.length];for(int i=0;i<tabInTmp.length;i++)mask[i]=1;}
		List<Double> lTmp=new ArrayList<Double>();
		for(int i=0;i<tabInTmp.length;i++)if(mask[i]>0)lTmp.add(tabInTmp[i]);
		Double []tabIn=(Double[])(lTmp.toArray(new Double[lTmp.size()]));
		
		int N=tabIn.length;
		if (tabIn.length==0)return new double[] {0,0,0};
		List<Double> l=new ArrayList<Double>();
		for(int i=0;i<tabIn.length;i++)l.add(tabIn[i]);
		Double []tab=(Double[])(l.toArray(new Double[l.size()]));
	
		if(tab.length<=5)return new double[] {0,0,0};
		Arrays.sort(tab);
		return new double[] {tab[N/4],tab[N/2],tab[(3*N)/4]};
    }

 	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Write black text on given image.
	 *
	 * @param text the text
	 * @param img the img
	 * @param fontSize the font size
	 * @param dx the dx
	 * @param dy the dy
	 * @return the image plus
	 */
	public static ImagePlus writeBlackTextOnGivenImage(String text, ImagePlus img,int fontSize,int dx,int dy) {
		ImagePlus ret=(img);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(dx,dy, text, font);
		roi.setStrokeColor(Color.black);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			ret.setColor(Color.black);
			IJ.run(ret, "Draw", "stack");
			ret.setRoi((Roi)null);
		}
		return ret;
	}

	
	/**
	 * Write text on image.
	 *
	 * @param text the text
	 * @param img the img
	 * @param fontSize the font size
	 * @param numLine the num line
	 * @return the image plus
	 */
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

	/**
	 * Write text on given image.
	 *
	 * @param text the text
	 * @param img the img
	 * @param fontSize the font size
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param c the c
	 * @param z the z
	 * @param t the t
	 */
	public static void writeTextOnGivenImage(String text, ImagePlus img,int fontSize,int x0,int y0,int c,int z, int t) {
		ImagePlus ret=img; 
		img.setC( c+1);
		img.setZ( z+1);
		img.setT( t+1);
		Font font = new Font("SansSerif", Font.PLAIN, fontSize);
		TextRoi roi = new TextRoi(x0,y0, text, font);
		roi.setStrokeColor(Color.white);
		Overlay overlay = new Overlay();
		overlay.add(roi);
		ret.setOverlay(overlay); 
		Roi[] ovlArray = ret.getOverlay().toArray();
		for (Roi ro: ovlArray) {
			ret.setRoi(ro);
			IJ.run(ret, "Draw", "Slice");
			ret.setRoi((Roi)null);
		}
		ret.setOverlay(null);
	}

	
	/**
	 * Write text on image.
	 *
	 * @param text the text
	 * @param img the img
	 * @param fontSize the font size
	 * @param numLine the num line
	 * @param value the value
	 * @return the image plus
	 */
	public static ImagePlus writeTextOnImage(String text, ImagePlus img,int fontSize,int numLine,double value) {
		return writeTextOnImage(text,  img, fontSize,numLine,value,10.0/512, 10.0/512);
	}

	/**
	 * Write white on image.
	 *
	 * @param text the text
	 * @param img the img
	 * @param fontSize the font size
	 * @param numLine the num line
	 * @return the image plus
	 */
	public static ImagePlus writeWhiteOnImage(String text, ImagePlus img,int fontSize,int numLine) {
		return writeTextOnImage(text,  img, fontSize,numLine,EPSILON,10.0/512, 10.0/512);
	}

	/**
	 * Write text on image.
	 *
	 * @param text the text
	 * @param img the img
	 * @param fontSize the font size
	 * @param numLine the num line
	 * @param value the value
	 * @param xCoordRatio the x coord ratio
	 * @param yCoordRatio the y coord ratio
	 * @return the image plus
	 */
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

/**
 * Removes the every title text in standard float image.
 *
 * @param img the img
 * @return the image plus
 */
/*
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
*/
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

	/**
	 * Put that image in that other.
	 *
	 * @param source the source
	 * @param dest the dest
	 */
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
	 * ?.
	 * Helper functions to access a part of image values, and to compute some statistics on these data
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param ray the ray
	 * @return the double[]
	 */	
	public static double[] stdAndMeanValueofImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.stdAndMeanValueOfImageAround :C or T >1");return null;}
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

	/**
	 * Statistics 1 D no black.
	 *
	 * @param vals the vals
	 * @return the double[]
	 */
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

	/**
	 * Statistics 1 D.
	 *
	 * @param tab the tab
	 * @return the double[]
	 */
	public static double[] statistics1D(ArrayList<Double>tab){
		double[]tab2=new double[tab.size()];
		for(int i=0;i<tab.size();i++)tab2[i]=tab.get(i);
		return statistics1D(tab2);
	}

	
	/**
	 * Statistics 1 D.
	 *
	 * @param vals the vals
	 * @return the double[]
	 */
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
	
	/**
	 * Min and max of tab.
	 *
	 * @param vals the vals
	 * @return the double[]
	 */
	public static double[] minAndMaxOfTab(double[] vals){
		double min=10E35;
		double max=-10E35;
		for(int i=0;i<vals.length ;i++) {
			if(vals[i]>max)max=vals[i];
			if(vals[i]<min)min=vals[i];
		}
		return (new double[] {min,max});
	}
	
	/**
	 * Statistics 2 D.
	 *
	 * @param vals the vals
	 * @return the double[]
	 */
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

	/**
	 * Values of block double slice.
	 *
	 * @param img the img
	 * @param xxm the xxm
	 * @param yym the yym
	 * @param xxM the xx M
	 * @param yyM the yy M
	 * @return the double[]
	 */
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
		
	/**
	 * Values of block double.
	 *
	 * @param img the img
	 * @param xxm the xxm
	 * @param yym the yym
	 * @param zzm the zzm
	 * @param xxM the xx M
	 * @param yyM the yy M
	 * @param zzM the zz M
	 * @return the double[]
	 */
	public static double []valuesOfBlockDouble(ImagePlus img,double xxm,double yym,double zzm,double xxM,double yyM,double zzM) {
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.valuesOfBlockDouble : C or T >1");return null;}
		int zImg=img.getStackSize();
		int xMax=img.getWidth();

		if(zzm<0)zzm=0;
		if(zzM<0)zzM=0;
		if(zzm>=img.getStackSize()-1)zzm=img.getStackSize()-2;
		if(zzM>=img.getStackSize()-1)zzM=img.getStackSize()-2;
		if(zzm>zzM)zzm=zzM;
		
		if(xxm<0)xxm=0;
		if(xxM<0)xxM=0;
		if(xxm>=img.getWidth()-1)xxm=img.getWidth()-2;
		if(xxM>=img.getWidth()-1)xxM=img.getWidth()-2;
		if(xxm>xxM)xxm=xxM;

		if(yym<0)yym=0;
		if(yyM<0)yyM=0;
		if(yym>=img.getHeight()-1)yym=img.getHeight()-2;
		if(yyM>=img.getHeight()-1)yyM=img.getHeight()-2;
		if(yym>yyM)yym=yyM;

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
	
	/**
	 * Mean and var only valid values byte.
	 *
	 * @param img the img
	 * @param rayX the ray X
	 * @return the image plus[]
	 */
	public static ImagePlus[] meanAndVarOnlyValidValuesByte(ImagePlus img,int rayX) {
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.meanAndVarOnlyValidValuesByte : C or T > 1");return null;}
		ImagePlus imgMean=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgMean, img);
		imgMean=convertToFloat(imgMean);
		ImagePlus imgVar=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(imgVar, img);
		imgVar=convertToFloat(imgVar);
		ImagePlus imgMask=VitimageUtils.thresholdByteImage(img, 0,1);
		int dimZ=img.getStackSize();
		for(int z=1;z<=dimZ;z++) {
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
		return new ImagePlus[] {imgMean,imgVar};
	}
	
	/**
	 * Values of block.
	 *
	 * @param img the img
	 * @param xm the xm
	 * @param ym the ym
	 * @param zm the zm
	 * @param xM the x M
	 * @param yM the y M
	 * @param zM the z M
	 * @return the double[]
	 */
	public static double []valuesOfBlock(ImagePlus img,int xm,int ym,int zm,int xM,int yM,int zM) {
		return valuesOfBlock(img,xm,ym,zm,xM, yM,zM,0,0);
	}
	
	
	/**
	 * Values of block.
	 *
	 * @param img the img
	 * @param xm the xm
	 * @param ym the ym
	 * @param zm the zm
	 * @param xM the x M
	 * @param yM the y M
	 * @param zM the z M
	 * @param c the c
	 * @param t the t
	 * @return the double[]
	 */
	public static double []valuesOfBlock(ImagePlus img,int xm,int ym,int zm,int xM,int yM,int zM,int c,int t) {
	//	if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.java : measuring std and mean values in hyperimage");return null;}
		int xMax=img.getWidth();
		if(zm<0)zm=0;
		if(zM>img.getNSlices()-1)zM=img.getNSlices()-1;
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
			for(int zz=zm;zz<=zM;zz++) {
				int z=VitimageUtils.getCorrespondingSliceInHyperImage(img, c, zz, t);
				byte[] valsImg=(byte [])img.getStack().getProcessor(z).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY16) {
			for(int zz=zm;zz<=zM;zz++) {
				int z=VitimageUtils.getCorrespondingSliceInHyperImage(img, c, zz, t);
				short[] valsImg=(short[])img.getStack().getProcessor(z).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
					}
				}			
			}
		}
		else if(img.getType() == ImagePlus.GRAY32) {
			for(int zz=zm;zz<=zM;zz++) {
				int z=VitimageUtils.getCorrespondingSliceInHyperImage(img, c, zz, t);
//				System.out.println("Getting slice "+c+" , "+zz+" , "+t);
				float[] valsImg=(float[])img.getStack().getProcessor(z).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
							ret[hit++]=(  (  (float)valsImg[xMax*y+x]));
					}
				}			
			}
		}
		return ret;
	}
	
	/**
	 * Checks if is null image.
	 *
	 * @param imgIn the img in
	 * @return true, if is null image
	 */
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
	
	/**
	 * Max of hyper image.
	 *
	 * @param img the img
	 * @return the float
	 */
	public static float maxOfHyperImage(ImagePlus img) {
		return maxOfImage(img);//In fact zM=img.getStackSize() gives access to both channels, timeframes and zslice. 
	}
	
	
	/**
	 * Max of image.
	 *
	 * @param img the img
	 * @return the float
	 */
	public static float maxOfImage(ImagePlus img) {
		float max=0;
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		if(img.getType() == ImagePlus.GRAY8) {
			byte[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if (((int)  (valsImg[xM*y+x] & 0xff ))           /*œi my cat's commentary*/ > max)max=(float)((int)(valsImg[xM*y+x] & 0xff)); 
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
						if ((valsImg[xM*y+x] & 0xffff )> max)max=(float)(valsImg[xM*y+x] & 0xffff); 
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
						if ((valsImg[xM*y+x])> max)max=(float)(valsImg[xM*y+x]); 
					}
				}			
			}
		}
		return max;	
	}

	/**
	 * Min of image.
	 *
	 * @param img the img
	 * @return the float
	 */
	public static float minOfImage(ImagePlus img) {
		float min=(float) 1E40;
		int xM=img.getWidth();
		int yM=img.getHeight();
		int zM=img.getStackSize();
		if(img.getType() == ImagePlus.GRAY8) {
			byte[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(byte [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x] & 0xff )< min)min=(float)(valsImg[xM*y+x] & 0xff); 
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
						if ((valsImg[xM*y+x] & 0xffff )< min)min=(float)(valsImg[xM*y+x] & 0xffff); 
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
						if ((valsImg[xM*y+x])< min)min=(float)(valsImg[xM*y+x]); 
					}
				}			
			}
		}
		return min;	
	}

	

	
	
	
	/**
	 * Gets the relaxing popup.
	 *
	 * @param introductionSentence the introduction sentence
	 * @param external the external
	 */
	public static void getRelaxingPopup(String introductionSentence,boolean external) {
		String zenSentence=SentenceOfTheDay.getRandomSentence();
		String message=""+introductionSentence+"\n";
		message+="\nRead these words carefully :\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"+zenSentence+"\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"+
				"\n\nKeep this sentence in mind while taking three deep breaths.\n When you feel confident to go back to work, click OK"; 
		IJ.showMessage(message);
	}
	
	
	
	
	/**
	 * Gets the mask as coords.
	 *
	 * @param img the img
	 * @return the mask as coords
	 */
	public static int[][] getMaskAsCoords(ImagePlus img) {
		int[]dims=VitimageUtils.getDimensions(img);
		if(dims[2]>1) {IJ.showMessage("Warning in VitimageUtils.getMaskAsCoords : extracting 2D coords from multi slices image. Return null");return null;}
		int xM=dims[0];
		int yM=dims[1];
		ArrayList<int[]>arRet=new ArrayList<int[]>();
		if(img.getType() == ImagePlus.GRAY8) {
			byte[]valsImg=(byte [])img.getStack().getProcessor(1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if ((valsImg[xM*y+x] & 0xff )> 0)arRet.add(new int[] {x,y}); 
				}
			}			
		}
		if(img.getType() == ImagePlus.GRAY16) {
			short[]valsImg=(short [])img.getStack().getProcessor(1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if ((valsImg[xM*y+x] & 0xffff )> 0)arRet.add(new int[] {x,y}); 
				}
			}			
		}
		if(img.getType() == ImagePlus.GRAY32) {
			float[]valsImg=(float [])img.getStack().getProcessor(1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					if ((valsImg[xM*y+x])> 0)arRet.add(new int[] {x,y}); 
				}
			}			
		}
		int[][]tab=arRet.toArray(new int[arRet.size()][2]);
		System.out.println(TransformUtils.stringMatrixMN("", tab));
		return tab;
	}

	/**
	 * Mean.
	 *
	 * @param tab the tab
	 * @return the double
	 */
	public static double mean(double[]tab) {
		double ret=0;
		for(int i=0;i<tab.length;i++)ret+=tab[i];
		return (ret/tab.length);
	}
	


	
	/**
	 * Max of image array.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus maxOfImageArray(ImagePlus []imgs) {
		return maxOfImageArray(new ImagePlus[][] {imgs});
	}

	/**
	 * Max of image array.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus maxOfImageArray(ImagePlus [][]imgs) {
		if(imgs[0][0].getType()==ImagePlus.GRAY16)return maxOfImageArrayShort(imgs);
		return maxOfImageArrayDouble(imgs);
	}
		
	/**
	 * Ind max of image array double.
	 *
	 * @param imgs the imgs
	 * @param minThreshold the min threshold
	 * @return the image plus
	 */
	public static ImagePlus indMaxOfImageArrayDouble(ImagePlus []imgs,int minThreshold) {
		int xM=imgs[0].getWidth();
		int yM=imgs[0].getHeight();
		int zM=imgs[0].getStackSize();
		ImagePlus retVal=VitimageUtils.nullImage(imgs[0].duplicate());
		ImagePlus retInd=VitimageUtils.nullImage(imgs[0].duplicate());
		retInd=VitimageUtils.makeOperationOnOneImage(retInd, 1, -1, true);
		float[]valsInd;
		float[]valsVal;
		float[]valsImg;
		for(int z=0;z<zM;z++) {
			valsInd=(float [])retInd.getStack().getProcessor(z+1).getPixels();
			valsVal=(float [])retVal.getStack().getProcessor(z+1).getPixels();
			for(int i=0;i<imgs.length;i++) {
				valsImg=(float [])imgs[i].getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if( ( (valsImg[xM*y+x])> valsVal[xM*y+x]) && ((valsImg[xM*y+x])> minThreshold) ) {
							valsVal[xM*y+x]=valsImg[xM*y+x]; 
							valsInd[xM*y+x]=i; 
						}
					}
				}			
			}
		}
		return retInd;
	}


	
	/**
	 * Max of image array double.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus maxOfImageArrayDouble(ImagePlus [][]imgs) {
		int xM=imgs[0][0].getWidth();
		int yM=imgs[0][0].getHeight();
		int zM=imgs[0][0].getStackSize();
		ImagePlus ret=imgs[0][0].duplicate();
		float[]valsImg;
		float[]valsRet;
		for(int z=0;z<zM;z++) {
			valsRet=(float [])ret.getStack().getProcessor(z+1).getPixels();
			for(int i=0;i<imgs.length;i++) {
				for(int j=0;j<imgs[i].length;j++) {
					valsImg=(float [])imgs[i][j].getStack().getProcessor(z+1).getPixels();
					for(int x=0;x<xM;x++) {
						for(int y=0;y<yM;y++) {
							if ((valsImg[xM*y+x])> valsRet[xM*y+x])valsRet[xM*y+x]=valsImg[xM*y+x]; 
						}
					}			
				}
			}
		}
		return ret;
	}

	/**
	 * Max of image array short.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus maxOfImageArrayShort(ImagePlus [][]imgs) {
		int xM=imgs[0][0].getWidth();
		int yM=imgs[0][0].getHeight();
		int zM=imgs[0][0].getStackSize();
		ImagePlus ret=imgs[0][0].duplicate();
		short[]valsImg;
		short[]valsRet;
		for(int z=0;z<zM;z++) {
			valsRet=(short [])ret.getStack().getProcessor(z+1).getPixels();
			for(int i=0;i<imgs.length;i++) {
				for(int j=0;j<imgs[i].length;j++) {
					valsImg=(short [])imgs[i][j].getStack().getProcessor(z+1).getPixels();
					for(int x=0;x<xM;x++) {
						for(int y=0;y<yM;y++) {
							if (  ((int)(valsImg[xM*y+x] & 0xffff)) > ((int)valsRet[xM*y+x] & 0xffff)  ) valsRet[xM*y+x]=valsImg[xM*y+x]; 
						}
					}			
				}
			}
		}
		return ret;
	}
	
	/**
	 * Mean of image array.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus meanOfImageArray(ImagePlus []imgs) {
		return meanOfImageArray(new ImagePlus[][] {imgs});
	}

	/**
	 * Mean of image array.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus meanOfImageArray(ImagePlus [][]imgs) {
		if(imgs[0][0].getType()==ImagePlus.GRAY16)return meanOfImageArrayShort(imgs);
		else if(imgs[0][0].getType()==ImagePlus.GRAY8)return meanOfImageArrayByte(imgs);
		else return meanOfImageArrayDouble(imgs);
	}
	
	/**
	 * Mean of image array double.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus meanOfImageArrayDouble(ImagePlus [][]imgs) {
		int N=0;
		for(int i=0;i<imgs.length;i++)N+=imgs[i].length;
		int xM=imgs[0][0].getWidth();
		int yM=imgs[0][0].getHeight();
		int zM=imgs[0][0].getStackSize();
		ImagePlus ret=imgs[0][0].duplicate();
		float[][][]valsImg;
		float[]valsRet;
		for(int z=0;z<zM;z++) {
			valsImg=new float[imgs.length][][];
			for(int i=0;i<imgs.length;i++) {
				valsImg[i]=new float[imgs[i].length][];
				for(int j=0;j<imgs[i].length;j++) {
					valsImg[i][j]=(float [])imgs[i][j].getStack().getProcessor(z+1).getPixels();
				}
			}
			valsRet=(float [])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xM;x++) {
				for(int y=0;y<yM;y++) {
					for(int i=0;i<imgs.length;i++) {
						for(int j=0;j<imgs[i].length;j++) {
							valsRet[y*xM+x]+=valsImg[i][j][y*xM+x];
						}
					}	
					valsRet[y*xM+x]/=N;
				}
			}
		}
		return ret;
	}

	/**
	 * Mean of image array short.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus meanOfImageArrayShort(ImagePlus [][]imgs) {
		ImagePlus [][]imgsTemp=new ImagePlus[imgs.length][];
		for(int i=0;i<imgs.length;i++) {
			imgsTemp[i]=new ImagePlus[imgs[i].length];
			for(int j=0;j<imgs[i].length;j++) {
				imgsTemp[i][j]=convertShortToFloatWithoutDynamicChanges(imgs[i][j]);
			}
		}
		return VitimageUtils.convertFloatToShortWithoutDynamicChanges(meanOfImageArrayDouble(imgsTemp)); 
	}
	
	/**
	 * Mean of image array byte.
	 *
	 * @param imgs the imgs
	 * @return the image plus
	 */
	public static ImagePlus meanOfImageArrayByte(ImagePlus [][]imgs) {
		ImagePlus [][]imgsTemp=new ImagePlus[imgs.length][];
		for(int i=0;i<imgs.length;i++) {
			imgsTemp[i]=new ImagePlus[imgs[i].length];
			for(int j=0;j<imgs[i].length;j++) {
				imgsTemp[i][j]=convertByteToFloatWithoutDynamicChanges(imgs[i][j]);
			}
		}
		return VitimageUtils.convertFloatToByteWithoutDynamicChanges(meanOfImageArrayDouble(imgsTemp)); 
	}
	
	/**
	 * Ind max of image.
	 *
	 * @param img the img
	 * @return the int[]
	 */
	public static int []indMaxOfImage(ImagePlus img) {
		int[]coords=new int[3];
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
						if ((valsImg[xM*y+x] & 0xff )> max) {
							max=(double)(valsImg[xM*y+x] & 0xff); 
							coords=new int[] {x,y,z};
						}
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
						if ((valsImg[xM*y+x] & 0xffff )> max) {
							max=(double)(valsImg[xM*y+x] & 0xffff); 
							coords=new int[] {x,y,z};
						}
					}
				}			
			}
		}
		if(img.getType() == ImagePlus.GRAY32) {
			max=-1E10;
			float[]valsImg;
			for(int z=0;z<zM;z++) {
				valsImg=(float [])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<xM;x++) {
					for(int y=0;y<yM;y++) {
						if ((valsImg[xM*y+x])> max) {
							max=(double)(valsImg[xM*y+x]); 
							coords=new int[] {x,y,z};
						}
					}
				}			
			}
		}
		return coords;
	}
	
	/**
	 * Checks if is null short image.
	 *
	 * @param imgIn the img in
	 * @return true, if is null short image
	 */
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
	
	/**
	 * Checks if is null float image.
	 *
	 * @param imgIn the img in
	 * @return true, if is null float image
	 */
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

/*	public static double[][]getHistogram(ImagePlus img,int nbSlicesConsidered){
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
	*//*
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
	}*/
		
	
	
	/**
 * Hyper unstack.
 *
 * @param imgs the imgs
 * @return the image plus[]
 */
public static ImagePlus[]hyperUnstack(ImagePlus[]imgs){
		int cmax=0;
		for(int i=0;i<imgs.length;i++)if(imgs[i].getNChannels()>cmax)cmax=imgs[i].getNChannels();
		int c=cmax;
		int t=imgs[0].getNFrames();
		int n=imgs.length;
		ImagePlus []tabret=new ImagePlus[c*t*n];
		for(int i=0;i<n;i++) {
			ImagePlus[]tabTemp=VitimageUtils.stacksFromHyperstackFastBis(imgs[i]);
			ImagePlus[]tabTemp2=new ImagePlus[cmax*t];
			for(int j=0;j<tabTemp.length;j++)tabTemp2[j]=tabTemp[j];
			for(int j=tabTemp.length;j<tabTemp2.length;j++)tabTemp2[j]=VitimageUtils.nullImage(tabTemp[0]);
			for(int j=0;j<c*t;j++)tabret[i*c*t+j]=tabTemp2[j];
		}
		return tabret;
	}
	
	
	/**
	 * Mean valueof image around.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param ray the ray
	 * @return the double
	 */
	public static double meanValueofImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.meanValueOfImageAround : measuring std and mean values in hyperimage");return 0;}
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

	/**
	 * Values of image around.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param ray the ray
	 * @return the double[]
	 */
	public static double []valuesOfImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.valuesOfImageAround : measuring std and mean values in hyperimage");return null;}
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
	
	/**
	 * Transfer properties.
	 *
	 * @param source the source
	 * @param target the target
	 */
	public static void transferProperties(ImagePlus source,ImagePlus target) {
		if(source.getProperty("Info")!=null) {
			target.setProperty("Info",(String)source.getProperty("Info"));
		}
		VitimageUtils.adjustImageCalibration(target, source);
	}
	
	/**
	 * Crop image.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param dimXX the dim XX
	 * @param dimYY the dim YY
	 * @param dimZZ the dim ZZ
	 * @return the image plus
	 */
	public static ImagePlus cropImage(ImagePlus img,int x0,int y0,int z0,int dimXX,int dimYY,int dimZZ) {
		if(img.getType()==ImagePlus.GRAY8)return cropImageByte(img, x0, y0, z0, dimXX, dimYY, dimZZ);
		if(img.getType()==ImagePlus.GRAY16)return cropImageShort(img, x0, y0, z0, dimXX, dimYY, dimZZ);
		if(img.getType()==ImagePlus.GRAY32)return cropImageFloat(img, x0, y0, z0, dimXX, dimYY, dimZZ);
		return null;
	}
		
		/**
		 * Crop image short.
		 *
		 * @param img the img
		 * @param x0 the x 0
		 * @param y0 the y 0
		 * @param z0 the z 0
		 * @param dimXX the dim XX
		 * @param dimYY the dim YY
		 * @param dimZZ the dim ZZ
		 * @return the image plus
		 */
		public static ImagePlus cropImageShort(ImagePlus img,int x0,int y0,int z0,int dimXX,int dimYY,int dimZZ) {
		if(img.getType()!=ImagePlus.GRAY16)return null;
		int[]dims=VitimageUtils.getDimensions(img);
		int X=dims[0];int Y=dims[1];int Z=dims[2];
		int xm=(int)Math.max(0, x0);
		int xM=(int)Math.min(xm+dimXX-1,X-1);
		int dimX=xM-xm+1;
		int ym=(int)Math.max(0, y0);
		int yM=(int)Math.min(ym+dimYY-1,Y-1);
		int dimY=yM-ym+1;
		int zm=(int)Math.max(0, z0);
		int zM=(int)Math.min(zm+dimZZ-1,Z-1);
		int dimZ=zM-zm+1;		
		
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,16,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		for(int z=zm;z<zm+dimZ;z++) {
			short[] valsImg=(short[])img.getStack().getProcessor(z+1).getPixels();
			short[] valsOut=(short[])out.getStack().getProcessor(z-zm+1).getPixels();
			for(int x=xm;x<xm+dimX;x++) {
				for(int y=ym;y<ym+dimY;y++){
					valsOut[dimX*(y-ym)+(x-xm)]=((short)(valsImg[X*y+x] & 0xffff));
				}			
			}
			out.getStack().setSliceLabel(img.getStack().getSliceLabel(z+1), z-zm+1);
		}
		transferProperties(img, out);
		return out;
	}

		/**
		 * Better signature.
		 *
		 * @param img the img
		 * @param x0 the x 0
		 * @param xf the xf
		 * @param y0 the y 0
		 * @param yf the yf
		 * @param z0 the z 0
		 * @param zf the zf
		 * @return the image plus
		 */
	public static ImagePlus cropFloatImage(ImagePlus img,int x0,int xf,int y0,int yf,int z0,int zf) {
		return cropImageFloat(img,x0,y0,z0,xf-x0+1,yf-y0+1,zf-z0+1);
	}

		
	/**
	 * Crop image float.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param z0 the z 0
	 * @param dimXX the dim XX
	 * @param dimYY the dim YY
	 * @param dimZZ the dim ZZ
	 * @return the image plus
	 */
	public static ImagePlus cropImageFloat(ImagePlus img,int x0,int y0,int z0,int dimXX,int dimYY,int dimZZ) {
		if(img.getType()!=ImagePlus.GRAY32)return null;
		int[]dims=VitimageUtils.getDimensions(img);
		int X=dims[0];int Y=dims[1];int Z=dims[2];
		int xm=(int)Math.max(0, x0);
		int xM=(int)Math.min(xm+dimXX-1,X-1);
		int dimX=xM-xm+1;
		int ym=(int)Math.max(0, y0);
		int yM=(int)Math.min(ym+dimYY-1,Y-1);
		int dimY=yM-ym+1;
		int zm=(int)Math.max(0, z0);
		int zM=(int)Math.min(zm+dimZZ-1,Z-1);
		int dimZ=zM-zm+1;		
		
		ImagePlus out=ij.gui.NewImage.createImage("Mask",dimX,dimY,dimZ,32,ij.gui.NewImage.FILL_BLACK);		
		VitimageUtils.adjustImageCalibration(out, img);
		for(int z=zm;z<zm+dimZ;z++) {
			float[] valsImg=(float[])img.getStack().getProcessor(z+1).getPixels();
			float[] valsOut=(float[])out.getStack().getProcessor(z-zm+1).getPixels();
			for(int x=xm;x<xm+dimX;x++) {
				for(int y=ym;y<ym+dimY;y++){
					valsOut[dimX*(y-ym)+(x-xm)]=((float)(valsImg[X*y+x]));
				}			
			}
			out.getStack().setSliceLabel(img.getStack().getSliceLabel(z+1), z-zm+1);
		}
		transferProperties(img, out);
		return out;
	}

	
	/**
	 * Capillary coordinates along Z.
	 *
	 * @param img3DT the img 3 DT
	 * @param coords the coords
	 * @param capillaryRadius the capillary radius
	 * @return the int[][]
	 */
	public static int[][]capillaryCoordinatesAlongZ(ImagePlus img3DT,int[]coords,double capillaryRadius){
		boolean debug=false;
		ImagePlus img3D=img3DT.duplicate();
		img3D=convertToFloat(img3D);
		int[][]ret=new int[img3D.getNSlices()][2];
		int[]dims=VitimageUtils.getDimensions(img3D);
		double[]voxs=VitimageUtils.getVoxelSizes(img3D);
		
		int rayPix=(int)Math.round(capillaryRadius/(voxs[0]));
		int lookupRadius=(int)Math.round(1.5*rayPix);

		int xMed=coords[0];int yMed=coords[1];int zMed=dims[2]/2;
		ImagePlus imgTemp;
		int xLast=xMed,yLast=yMed;
		if(debug)System.out.println("Initial coords of capillary on central slice : "+xMed+" , "+yMed+" , "+zMed);
		
		for(int z=zMed;z>=0;z--) {
			//Extract a patch around the finding of upside
			imgTemp=VitimageUtils.cropImageFloat(img3D,xLast-lookupRadius,yLast-lookupRadius,z, lookupRadius*2,lookupRadius*2,1);
			//Find cap center in it
			int[]coordsNew=findCapillaryCenterInSlice(imgTemp,capillaryRadius);
			//Update coordinates of last
			xLast=xLast-lookupRadius+coordsNew[0];
			yLast=yLast-lookupRadius+coordsNew[1];
			if(debug)System.out.println("Found best="+xLast+","+yLast+" at slice "+z);
			ret[z][0]=xLast;
			ret[z][1]=yLast;
		}
		xLast=xMed;
		yLast=yMed;
		for(int z=zMed;z<dims[2];z++) {
			//Extract a patch around the finding of upside
			imgTemp=VitimageUtils.cropImageFloat(img3D,xLast-lookupRadius,yLast-lookupRadius, z, lookupRadius*2,lookupRadius*2,1);
			
			//Find cap center in it
			int[]coordsNew=findCapillaryCenterInSlice(imgTemp,capillaryRadius);
			
			//Update coordinates of last
			xLast=xLast-lookupRadius+coordsNew[0];
			yLast=yLast-lookupRadius+coordsNew[1];
			if(debug)System.out.println("Found best="+xLast+","+yLast+" at slice "+z);
	
			ret[z][0]=xLast;
			ret[z][1]=yLast;
		}
		return ret;
	}
	
	/**
	 * Capillary values along Z static.
	 *
	 * @param img3D the img 3 D
	 * @param coords the coords
	 * @param capillaryRadius the capillary radius
	 * @return the double[]
	 */
	public static double[]capillaryValuesAlongZStatic(ImagePlus img3D,int[]coords,double capillaryRadius){
		double[]voxs=VitimageUtils.getVoxelSizes(img3D);
		int[]dims=VitimageUtils.getDimensions(img3D);
		int rayPix=(int)Math.round(capillaryRadius/(voxs[0]));
		int semiRayPix=rayPix/2;
		int lookupRadius=(int)Math.round(1.5*rayPix);

		int xMed=coords[0];int yMed=coords[1];int zMed=dims[2]/2;
		double[]capVals=new double[dims[2]];
		ImagePlus imgTemp;
		int xLast=xMed,yLast=yMed;

		for(int z=zMed;z>=0;z--) {
			//Extract a patch around the finding of upside
			imgTemp=VitimageUtils.cropImageFloat(img3D,xLast-lookupRadius,yLast-lookupRadius,z, lookupRadius*2,lookupRadius*2,1);
			//Find cap center in it
			int[]coordsNew=findCapillaryCenterInSlice(imgTemp,capillaryRadius);
			
			//Update coordinates of last
			xLast=xLast-lookupRadius+coordsNew[0];
			yLast=yLast-lookupRadius+coordsNew[1];

			//Gather information of M0 value
			double[]stats=VitimageUtils.statistics1D(VitimageUtils.valuesOfBlock(img3D,xLast-semiRayPix, yLast-semiRayPix, z-1,xLast+semiRayPix, yLast+semiRayPix, z+1));
			IJ.log("Capillary detected at z="+z+" at coordinates "+xLast+", "+yLast+" with M0="+stats[0]+" std="+stats[1]);
			capVals[z]=stats[0];
		}
		xLast=xMed;
		yLast=yMed;
		for(int z=zMed;z<dims[2];z++) {
			//Extract a patch around the finding of upside
			imgTemp=VitimageUtils.cropImageFloat(img3D,xLast-lookupRadius,yLast-lookupRadius, z, lookupRadius*2,lookupRadius*2,1);
			
			//Find cap center in it
			int[]coordsNew=findCapillaryCenterInSlice(imgTemp,capillaryRadius);
			
			//Update coordinates of last
			xLast=xLast-lookupRadius+coordsNew[0];
			yLast=yLast-lookupRadius+coordsNew[1];
			
			//Gather information of M0 value
			double[]stats=VitimageUtils.statistics1D(VitimageUtils.valuesOfBlock(img3D,xLast-semiRayPix, yLast-semiRayPix, z-1,xLast+semiRayPix, yLast+semiRayPix, z+1));
			System.out.println("Capillary detected at z="+z+" at coordinates "+xLast+", "+yLast+" with M0="+stats[0]+" std="+stats[1]);
			capVals[z]=stats[0];
		}
		return capVals;
	}
	

	
	
	/**
	 * Find capillary center in slice.
	 *
	 * @param imgTemp the img temp
	 * @param capillaryRadius the capillary radius
	 * @return the int[]
	 */
	public static int[] findCapillaryCenterInSlice(ImagePlus imgTemp,double capillaryRadius) {
		ImagePlus img=imgTemp.duplicate();
		double[]voxs=VitimageUtils.getVoxelSizes(img);		
		int rayPix=(int)Math.round(capillaryRadius/(voxs[0]));
		int raySquare=rayPix*rayPix;
		int kernelSize=3*rayPix;
		if (kernelSize%2==0)kernelSize++;
		int distX,distY;
		float[]kernel=new float[kernelSize*kernelSize];
		for(int i=0;i<kernelSize;i++)for(int j=0;j<kernelSize;j++) {
			distX=(i-(kernelSize/2));
			distY=(j-(kernelSize/2));
			kernel[i*kernelSize+j]=((distX*distX+distY*distY)<raySquare) ? 1 : -1;
		}
		new Convolver().convolve(img.getProcessor(), kernel, kernelSize, kernelSize);
		img=VitimageUtils.makeOperationOnOneImage(img,2,-1, false);
		int[]coordsOfMax=VitimageUtils.indMaxOfImage(img);
		return coordsOfMax;
	}

	


	/**
	 * Gets the anti capillary mask.
	 *
	 * @param img the img
	 * @param capillaryRadius the capillary radius
	 * @return the anti capillary mask
	 */
	public static ImagePlus getAntiCapillaryMask(ImagePlus img,double capillaryRadius) {
		boolean debug=false;
		//Find capillary in the central slice
		ImagePlus imgSlice=new Duplicator().run(img,1,1,img.getNSlices()/2+1,img.getNSlices()/2+1,1,1);
		if(imgSlice.getType()!=ImagePlus.GRAY32)imgSlice=convertToFloat(imgSlice);
		int []coordsCentral=findCapillaryCenterInSlice(imgSlice,capillaryRadius);
		int[][]capillaryCenters=capillaryCoordinatesAlongZ(img,coordsCentral,capillaryRadius);

		ImagePlus ret=new Duplicator().run(img,1,1,1,img.getNSlices(),1,1);
		ret=convertToFloat(ret);
		ret=VitimageUtils.makeOperationOnOneImage(ret, 2, 0, true);
		int radiusCapLarge=(int)Math.round(capillaryRadius*2.0/VitimageUtils.getVoxelSizes(img)[0]);
		int radiusSquare=radiusCapLarge*radiusCapLarge;
		int radiusCapShort=(int)Math.round(capillaryRadius*0.7/VitimageUtils.getVoxelSizes(img)[0]);
		int radiusSquareShort=radiusCapShort*radiusCapShort;
		
		int dimX=ret.getWidth();
		int dimY=ret.getHeight();
		for(int z=0;z<img.getNSlices();z++) {
			if(debug)System.out.println("Pour z="+z+" found "+capillaryCenters[z][0]+" , "+capillaryCenters[z][1]);
			float[]retVal=(float[])ret.getStack().getProcessor(z+1).getPixels();
			int xm=Math.max(0,capillaryCenters[z][0]-radiusCapLarge);
			int ym=Math.max(0,capillaryCenters[z][1]-radiusCapLarge);
			int xM=Math.min(dimX-1,capillaryCenters[z][0]+radiusCapLarge);
			int yM=Math.min(dimY-1,capillaryCenters[z][1]+radiusCapLarge);
			for(int x=xm;x<=xM;x++)			for(int y=ym;y<=yM;y++) {
				if( (x-capillaryCenters[z][0])*(x-capillaryCenters[z][0]) + (y-capillaryCenters[z][1]) * (y-capillaryCenters[z][1]) < radiusSquareShort )retVal[dimX*y+x]=4;
				else if( (x-capillaryCenters[z][0])*(x-capillaryCenters[z][0]) + (y-capillaryCenters[z][1]) * (y-capillaryCenters[z][1]) < radiusSquare )retVal[dimX*y+x]=2;
				else retVal[dimX*y+x]=0;
			}
		}
		return ret;
	}

	
	
	
	
	/**
	 * Values of image processor.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param ray the ray
	 * @return the double[]
	 */
	public static double []valuesOfImageProcessor(ImageProcessor img,int x0,int y0,double ray) {
		int xMax=img.getWidth();
		int xm=(int)Math.round(x0-ray);
		int xM=(int)Math.round(x0+ray);
		int ym=(int)Math.round(y0-ray);
		int yM=(int)Math.round(y0+ray);

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
		if(img.getBitDepth() == 8) {
			byte[] valsImg=(byte [])img.getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
				}
			}			
		}
		else if(img.getBitDepth() == 16) {
			short[] valsImg=(short[])img.getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
				}
			}			
		}
		else if(img.getBitDepth() == 32) {
			float[] valsImg=(float[])img.getPixels();
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
						ret[hit++]=(  (  (float)valsImg[xMax*y+x]));
				}
			}			
		}
		return ret;
	}

	
	
	
	/**
	 * Indmax.
	 *
	 * @param tab the tab
	 * @return the int
	 */
	public static int indmax(int[]tab) {
		double max=-1000000;int indmax=0;
		for(int i=0;i<tab.length;i++)if(tab[i]>max) {max=tab[i];indmax=i;}
		return indmax;
	}

	/**
	 * Indmax.
	 *
	 * @param tab the tab
	 * @return the int
	 */
	public static int indmax(double[]tab) {
		double max=-1000000;int indmax=0;
		for(int i=0;i<tab.length;i++)if(tab[i]>max) {max=tab[i];indmax=i;}
		return indmax;
	}
	
	/**
	 * Max.
	 *
	 * @param tab the tab
	 * @return the int
	 */
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

	/**
	 * Max.
	 *
	 * @param tab the tab
	 * @return the double
	 */
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

	/**
	 * Moy two max.
	 *
	 * @param tab the tab
	 * @return the double
	 */
	public static double moyTwoMax(double[]tab){
		if(tab.length==0) {
			IJ.log("In VitimageUtils.max, tab is null. Return 0");
			return 0;
		}
		double max1=-1E10;
		double max2=-1E10;
		for(int i=0;i<tab.length;i++) {
			if(tab[i]>max1) {
				max2=max1;
				max1=tab[i];
			}
			else {
				if(tab[i]>max2) {
					max2=tab[i];
				}
			}
		}
		return ((max1+max2)/2);
	}

	
	/**
	 * Min.
	 *
	 * @param tab the tab
	 * @return the int
	 */
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
	
	/**
	 * Min.
	 *
	 * @param tab the tab
	 * @return the double
	 */
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
	
	/**
	 * Gets the column of tab.
	 *
	 * @param tab the tab
	 * @param column the column
	 * @return the column of tab
	 */
	public static double[]getColumnOfTab(double[][]tab,int column){
		double[]ret=new double[tab.length];
		for(int i=0;i<tab.length;i++)ret[i]=tab[i][column];
		return ret;
	}
	
	/**
	 * Transpose tab.
	 *
	 * @param tab the tab
	 * @return the double[][]
	 */
	public static double[][]transposeTab(double[][]tab){
		double[][]ret=new double[tab[0].length][tab.length];
		for(int i=0;i<tab.length;i++)for(int j=0;j<tab[0].length;j++)ret[j][i]=tab[i][j];
		return ret;
	}

	/**
	 * Gets the min max byte.
	 *
	 * @param imgIn the img in
	 * @return the min max byte
	 */
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
	
	/**
	 * Gets the min max float.
	 *
	 * @param imgIn the img in
	 * @return the min max float
	 */
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
	 * Functions for accessing basic informations about images.
	 *
	 * @param img the img
	 * @return the voxel sizes
	 */	
	public static double[]getVoxelSizes(ImagePlus img){
		return new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
	}
	
	/**
	 * Gets the dimensions.
	 *
	 * @param img the img
	 * @return the dimensions
	 */
	public static int[]getDimensions(ImagePlus img){
		if(img.getNChannels()>1 || img.getNFrames()>1)return new int[] {img.getWidth(),img.getHeight(),img.getNSlices()};
		else return new int[] {img.getWidth(),img.getHeight(),img.getStackSize()};
	}

	/**
	 * Gets the dimensions real space.
	 *
	 * @param img the img
	 * @return the dimensions real space
	 */
	public static double[]getDimensionsRealSpace(ImagePlus img){
		int[]dims=getDimensions(img);
		double[]voxs=getVoxelSizes(img);
		double[]ret=new double[] {dims[0]*voxs[0],dims[1]*voxs[1],dims[2]*voxs[2]};
		return ret;
	}

	/**
	 * Gets the dimensions XYZCT.
	 *
	 * @param img the img
	 * @return the dimensions XYZCT
	 */
	public static int[]getDimensionsXYZCT(ImagePlus img){
		int[]tab1=img.getDimensions();
		return new int[] {tab1[0],tab1[1],tab1[3],tab1[2],tab1[4]};
	}
	
	/**
	 * Adjust image calibration.
	 *
	 * @param img the img
	 * @param voxSize the vox size
	 * @param unit the unit
	 */
	public static void adjustImageCalibration(ImagePlus img,double []voxSize,String unit) {
		if(img==null)return;
		img.getCalibration().setUnit(unit);
		Calibration cal = img.getCalibration();			
		cal.pixelWidth =voxSize[0];
		cal.pixelHeight =voxSize[1];
		cal.pixelDepth =voxSize[2];
	}

	/**
	 * Adjust voxel size.
	 *
	 * @param img the img
	 * @param d the d
	 * @param unit the unit
	 */
	public static void adjustVoxelSize(ImagePlus img,double[]d,String unit) {
		if(img==null)return;
		img.getCalibration().pixelWidth=d[0];
		img.getCalibration().pixelHeight=d[1];
		img.getCalibration().pixelDepth=d[2];
		img.getCalibration().setUnit(unit);
	}

	/**
	 * Adjust voxel size.
	 *
	 * @param img the img
	 * @param d the d
	 */
	public static void adjustVoxelSize(ImagePlus img,double[]d) {
		if(img==null)return;
		img.getCalibration().pixelWidth=d[0];
		img.getCalibration().pixelHeight=d[1];
		img.getCalibration().pixelDepth=d[2];
	}

	/**
	 * Adjust image calibration.
	 *
	 * @param img the img
	 * @param ref the ref
	 */
	public static void adjustImageCalibration(ImagePlus img,ImagePlus ref) {
		if(img==null)return;
		img.getCalibration().setUnit(ref.getCalibration().getUnit());
		img.getCalibration().pixelWidth=ref.getCalibration().pixelWidth;
		img.getCalibration().pixelHeight=ref.getCalibration().pixelHeight;
		img.getCalibration().pixelDepth=ref.getCalibration().pixelDepth;
	}

	/**
	 * Gets the voxel volume.
	 *
	 * @param img the img
	 * @return the voxel volume
	 */
	public static double getVoxelVolume(ImagePlus img) {
		return img.getCalibration().pixelDepth*img.getCalibration().pixelWidth*img.getCalibration().pixelHeight;
	}

	/**
	 * Gets the image center.
	 *
	 * @param ref the ref
	 * @param giveCoordsInRealSpace the give coords in real space
	 * @return the image center
	 */
	public static double[]getImageCenter(ImagePlus ref,boolean giveCoordsInRealSpace){
		int[]dims=VitimageUtils.getDimensions(ref);
		double[]voxs=VitimageUtils.getVoxelSizes(ref);
		double[]ret=new double[3];
		for(int dim=0;dim<3;dim++)ret[dim]=(dims[dim]-1)/2.0*(giveCoordsInRealSpace ? voxs[dim] : 1);
		return ret;
	}

	
	
	
	
	

	/**
	 *  Helpers for writing or reading data in files.
	 *
	 * @param file the file
	 * @return the int
	 */
	public static int readIntFromFile(String file) {
		return Integer.parseInt(readStringFromFile(file));
	}
	
	/**
	 * Write double in file.
	 *
	 * @param file the file
	 * @param a the a
	 */
	public static void writeDoubleInFile(String file,double a) {
		writeStringInFile(""+a,file);
	}
	
	/**
	 * Read double from file.
	 *
	 * @param file the file
	 * @return the double
	 */
	public static double readDoubleFromFile(String file) {
		return Double.parseDouble(readStringFromFile(file));
	}
		
	/**
	 * Write double array in file.
	 *
	 * @param tab the tab
	 * @param file the file
	 */
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
	
	/**
	 * Write int array in file.
	 *
	 * @param tab the tab
	 * @param file the file
	 */
	public static void writeIntArrayInFile(int [][]tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		int nDims=tab[0].length;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			out.write(""+tab.length+"\n");
			for(int i=0;i<nData;i++) {
				
				for(int j=0;j<nDims-1;j++) {
					System.out.println(i+" , "+j);
					out.write(""+tab[i][j]+" ");
				}
				out.write(""+tab[i][nDims-1]+"\n");
			}
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	/**
	 * Write int array 1 D in file.
	 *
	 * @param tab the tab
	 * @param file the file
	 */
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
	
	/**
	 * Read int array 1 D from file.
	 *
	 * @param file the file
	 * @return the int[]
	 */
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
	
	/**
	 * Write double array 1 D in file.
	 *
	 * @param tab the tab
	 * @param file the file
	 */
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
	
	/**
	 * Write double array 1 D in file simple.
	 *
	 * @param tab the tab
	 * @param file the file
	 */
	public static void writeDoubleArray1DInFileSimple(double []tab,String file) {
		int nData=tab.length;
		if(nData<1)return;
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			for(int i=0;i<nData;i++) out.write(tab[i]+"\n");
			out.close();
		} catch (Exception e) {IJ.error("Unable to write data to file: "+file+"error: "+e);}	
	}
	
	/**
	 * Read double array 1 D from file.
	 *
	 * @param file the file
	 * @return the double[]
	 */
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
	
	/**
	 * Write point 3 d array in file.
	 *
	 * @param tab the tab
	 * @param file the file
	 */
	public static void writePoint3dArrayInFile(Point3d[]tab,String file) {
		writeDoubleArrayInFile(VitimageUtils.convertPoint3dArrayToDoubleArray(tab),file);
	}
	
	/**
	 * Read point 3 d array in file.
	 *
	 * @param file the file
	 * @return the point 3 d[]
	 */
	public static Point3d[] readPoint3dArrayInFile(String file) {
		return VitimageUtils.convertDoubleArrayToPoint3dArray(VitimageUtils.readDoubleArrayFromFile(file,3));
	}

	/**
	 * Read double array from file.
	 *
	 * @param file the file
	 * @param nbDimsPerLine the nb dims per line
	 * @return the double[][]
	 */
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
		
	/**
	 * Read int array from file.
	 *
	 * @param file the file
	 * @param nbDimsPerLine the nb dims per line
	 * @return the int[][]
	 */
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

	
	/**
	 * Gets the path of home linux.
	 *
	 * @return the path of home linux
	 */
	public static String getPathOfHomeLinux() {
		return System.getProperty("user.home");
	}
	
	/**
	 * Gets the name of user linux.
	 *
	 * @return the name of user linux
	 */
	public static String getNameOfUserLinux() {
		return new File(getPathOfHomeLinux()).getName();
	}

	
	
	
	/**
	 * Write string in file.
	 *
	 * @param text the text
	 * @param file the file
	 */
	public static void writeStringInFile(String text,String file) {
		if(file ==null)return;
		writeStringInFileUTF8(text,file);
	}
	
	/**
	 * Read string from file.
	 *
	 * @param file the file
	 * @return the string
	 */
	public static String readStringFromFile(String file) {
		if(file ==null)return null;
		String s=readStringFromFileUTF8(file);
		if (s!=null)return s;
		
		String str=null;
		try {
			Charset charset=guessCharset(file);
			str= Files.lines(Paths.get(new File(file).getAbsolutePath()),charset).collect(Collectors.joining("\n"));
		} catch (IOException e) {	return null;	}
		return str;
	}
	
	
	/**
	 * Write string in file UTF 8.
	 *
	 * @param str the str
	 * @param path the path
	 */
	public static void writeStringInFileUTF8(String str,String path) {
		Writer out;
		try {
			out = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(path), StandardCharsets.UTF_8));
			out.write(str);
			out.close();
		} catch (Exception e) {	 e.printStackTrace();	}
	}

	
	/**
	 * Read string from file UTF 8.
	 *
	 * @param path the path
	 * @return the string
	 */
	public static String readStringFromFileUTF8(String path) {
	    String ret="";
	    InputStreamReader isr ;BufferedReader reader;
		try {
			FileInputStream fis = new FileInputStream(path);
	        isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
 	        reader = new BufferedReader(isr);
		    String str;
	        while ((str = reader.readLine()) != null) ret+=str+"\n";
	        reader.close();
	        isr.close();
	        fis.close();
	    } catch (IOException e) {    e.printStackTrace();   return null;}
		return ret;
	}

	
	/**
	 * Guess charset.
	 *
	 * @param file the file
	 * @return the charset
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Charset guessCharset(String file) throws IOException {
		return Charset.defaultCharset();
	}
	

	
	/**
	 * Read string from file 2.
	 *
	 * @param file the file
	 * @return the string
	 */
	public static String readStringFromFile2(String file) {	
		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		}
		StringBuilder content = new StringBuilder();
		String line;

		try {
			while ((line = reader.readLine()) != null) {
			    content.append(line);
			    content.append(System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return content.toString();
	}
	
	
	/**
	 * Write int in file.
	 *
	 * @param file the file
	 * @param a the a
	 */
	public static void writeIntInFile(String file,int a) {
		writeStringInFile(""+a,file);
	}
	
	/**
	 * Writable array.
	 *
	 * @param array the array
	 * @return the string
	 */
	public static String writableArray(double[]array) {
		String ret="";
		for(int i=0;i<array.length-1;i++)ret+=array[i]+" ";
		ret+=array[array.length-1];
		return ret;
	}

	/**
	 * Writable array.
	 *
	 * @param array the array
	 * @return the string
	 */
	public static String writableArray(int[]array) {
		String ret="";
		for(int i=0;i<array.length-1;i++)ret+=array[i]+" ";
		ret+=array[array.length-1];
		return ret;
	}




	
	
	
	
	
	
	
	/**
	 *  Helpers used in Registration steps (BM and ITK).
	 *
	 * @param img the img
	 * @param pixelSpacing the pixel spacing
	 * @return the binary grid
	 */
	public static ImagePlus getBinaryGrid(ImagePlus img,int pixelSpacing) {
		return getBinaryGrid(img, pixelSpacing,true,false);
	}
	
	/**
	 * Gets the binary grid.
	 *
	 * @param img the img
	 * @param pixelSpacing the pixel spacing
	 * @param doubleSizeEveryFive the double size every five
	 * @param displayTextBorder the display text border
	 * @return the binary grid
	 */
	public static ImagePlus getBinaryGrid(ImagePlus img,int pixelSpacing,boolean doubleSizeEveryFive,boolean displayTextBorder) {
		boolean doDouble=false;
		if(pixelSpacing<2)pixelSpacing=2;
		if(pixelSpacing>5)doDouble=true;
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getNSlices();
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, 8);
		VitimageUtils.adjustImageCalibration(ret,img);
		for(int z=0;z<dimZ;z++) {
			byte []tabRet=(byte[])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				for(int y=0;y<dimY;y++) {
					if( ((x%pixelSpacing==pixelSpacing/2)) || 
					    ((y%pixelSpacing==pixelSpacing/2))){
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
	
	
	public static Point3d[] getCoordinatesFromRoiSet(String path){
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.open(path);
		int N=rm.getCount();
		Point3d[]tab=new Point3d[N];
		for(int indP=0;indP<N;indP++){
			tab[indP]=new Point3d(rm.getRoi(indP ).getXBase() , rm.getRoi(indP).getYBase() ,  rm.getRoi(indP).getZPosition());
		}
		rm.reset();
		return tab;
	}
	
	public static Point3d[] getCoordinatesFromRoiSet(String path,ImagePlus img,boolean realWorldCoordinates){
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.open(path);
		int N=rm.getCount();
		Point3d[]tab=new Point3d[N];
		for(int indP=0;indP<N;indP++){
			tab[indP]=new Point3d(rm.getRoi(indP ).getXBase() , rm.getRoi(indP).getYBase() ,  rm.getRoi(indP).getZPosition());
			if(realWorldCoordinates) {
			tab[indP]=TransformUtils.convertPointToRealSpace(tab[indP],img);
			}
		}
		rm.reset();
		return tab;
	}
	
	
	public static Point3d[][] getCoordinatesFromRoiSetOfCorrespondences(String path,ImagePlus imgRef,ImagePlus imgMov, boolean realWorldCoordinates){
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.open(path);
		int N=rm.getCount()/2;
		Point3d[][]tab=new Point3d[2][N];
		for(int indP=0;indP<N;indP++){
			tab[0][indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			tab[1][indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			if(realWorldCoordinates) {
				tab[0][indP]=TransformUtils.convertPointToRealSpace(tab[0][indP],imgRef);
				tab[1][indP]=TransformUtils.convertPointToRealSpace(tab[1][indP],imgMov);
								
			}	
		}
		rm.reset();
		return tab;
	}

	public static void saveRoiAs(Roi []r,String path){
		RoiManager rm = RoiManager.getInstance();
        rm.reset();
		for(Roi rr:r)rm.addRoi(rr);
		rm.runCommand("Save", path);
	}

	public static void writePointRoiToCsv(String pathToRoi,String pathToCsv){
		Point3d[]tab=getCoordinatesFromRoiSet(pathToRoi);
		int N=tab.length;
		double[][]tabdou=new double[N][3];
		for (int i=0;i<N;i++){
			tabdou[i][0]=tab[i].x;
			tabdou[i][1]=tab[i].y;
			tabdou[i][2]=tab[i].z;
		}
		writeDoubleArrayInFile(tabdou, pathToCsv);
	}

	public static void writePoint3dTabToCsv(Point3d[]tab,String pathToCsv){
		int N=tab.length;
		double[][]tabdou=new double[N][3];
		for (int i=0;i<N;i++){
			tabdou[i][0]=tab[i].x;
			tabdou[i][1]=tab[i].y;
			tabdou[i][2]=tab[i].z;
		}
		writeDoubleTabInCsvSimple(tabdou,pathToCsv);
	}


	public static PointRoi []getPointRoiTabMonoSliceFromPoint3dTab(Point3d[]tab,ImagePlus imgRef,boolean convertFromRealWorldCoordinates){
		int nPoints = tab.length;
		System.out.println("NN="+nPoints);
        float[] xPoints = new float[1];
        float[] yPoints = new float[1];
		PointRoi[]tabRoi=new PointRoi[nPoints];
		double vx=1;
		double vy=1;
        if(convertFromRealWorldCoordinates){
			vx=VitimageUtils.getVoxelSizes(imgRef)[0];
			vy=VitimageUtils.getVoxelSizes(imgRef)[1];
		} 
        for (int i = 0; i < nPoints; i++) {
            xPoints[0] = (float) (tab[i].x/vx);
            yPoints[0] = (float) (tab[i].y/vy);
			tabRoi[i]=new PointRoi(xPoints, yPoints, 1);
		}
		return tabRoi;
	}

	public static Point3d[]point3dDoubleTabToSingleTab(Point3d[][]tab2d){
		Point3d[]tab1d=new Point3d[tab2d[0].length*2];
		for(int i =0;i<tab2d[0].length;i++){
			tab1d[i*2]=tab2d[0][i];
			tab1d[i*2+1]=tab2d[1][i];
		}
		return tab1d;
	}

	public static Point3d[][]point3dSingleTabToDoubleTab(Point3d[]tab1d){
		int N=tab1d.length/2;
		Point3d[][]tab2d=new Point3d[2][N];
		for(int i =0;i<N;i++){
			tab2d[0][i]=tab1d[i*2];
			tab2d[1][i]=tab1d[i*2+1];
		}
		return tab2d;
	}


	/**
	 * Images have same characteristics.
	 *
	 * @param img1 the img 1
	 * @param img2 the img 2
	 * @return true, if successful
	 */
	public static boolean imagesHaveSameCharacteristics(ImagePlus img1,ImagePlus img2) {
		double[][]voxs=new double[][] {VitimageUtils.getVoxelSizes(img1),VitimageUtils.getVoxelSizes(img2)};
		int[][]dims=new int[][] {img1.getDimensions(),img2.getDimensions()};
		for(int i=0;i<voxs[0].length;i++)if(Math.abs(voxs[0][i]-voxs[1][i])/voxs[0][i]>0.00000001)return false;
		for(int i=0;i<dims[0].length;i++)if(dims[0][i]!=dims[1][i])return false;
		return true;
	}
	 
	/**
	 * Gets the correspondance list as image plus.
	 *
	 * @param imgRef the img ref
	 * @param tabCorr the tab corr
	 * @param curVoxSize the cur vox size
	 * @param sliceInt2 the slice int 2
	 * @param blockStrideX the block stride X
	 * @param blockStrideY the block stride Y
	 * @param blockStrideZ the block stride Z
	 * @param blockHalfSizeX the block half size X
	 * @param blockHalfSizeY the block half size Y
	 * @param blockHalfSizeZ the block half size Z
	 * @param vectors the vectors
	 * @return the correspondance list as image plus
	 */
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
			int dx=Math.max(0, Math.min(blockStrideX/2-1, blockHalfSizeX));
			int dy=Math.max(0, Math.min(blockStrideY/2-1, blockHalfSizeY));
			int dz=Math.max(0, Math.min(blockStrideZ/2-1, blockHalfSizeZ));
			for(int cor=tabCorr.size()-1;cor>=0;cor--) {//HERE
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
							dataRet[0][dims[0]*yy+xx]=score;
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

	/**
	 * Null image.
	 *
	 * @param in the in
	 * @return the image plus
	 */
	public static ImagePlus nullImage(ImagePlus in) {
		return makeOperationOnOneImage(in,2,0, true);
	}
	
	/**
	 * Caracterize background of image.
	 *
	 * @param img the img
	 * @return the double[]
	 */
	public static double[] caracterizeBackgroundOfImage(ImagePlus img) {
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.caracterizeBackgroundOfImage : measuring std and mean values in hyperimage");return null;}
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
	
	/**
	 * Gets the max value for contrast intelligent.
	 *
	 * @param img the img
	 * @param channel the channel
	 * @param time the time
	 * @param z the z
	 * @param percentageKeep the percentage keep
	 * @param factor the factor
	 * @return the max value for contrast intelligent
	 */
	public static double getMaxValueForContrastIntelligent(ImagePlus img,int channel,int time,int z,double percentageKeep,double factor) {
		int nBins=1000;
		img.setC(channel);
		img.setT(time);
		img.setZ(z);
		ImageStatistics stats = img.getStatistics(ImagePlus.AREA+ImagePlus.MEAN+ImagePlus.MODE+ImagePlus.MIN_MAX, nBins);
		double[] y = stats.histogram();
		double wid=stats.binSize;
		double sum=0;for(double yy : y)sum+=yy;
		double cum=0;int indexFin=0;
		while(cum<sum*(percentageKeep/100.0)){cum+=y[indexFin++];}
		return (factor*(wid*indexFin));
	}

	
	/**
	 * Similarity.
	 *
	 * @param d1 the d 1
	 * @param d2 the d 2
	 * @return the double
	 */
	public static double similarity(double d1,double d2) {
		double diff=Math.abs(d1-d2);
		double mean=(d1+d2)/2;
		return 1-(0.5*diff/mean);
	}

	/**
	 * Convert to float.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus convertToFloat(ImagePlus img) {
		ImagePlus res=img.duplicate();
		if(img.getType()==ImagePlus.GRAY32)return res;
		if(img.getType()==ImagePlus.GRAY16)return VitimageUtils.convertShortToFloatWithoutDynamicChanges(res);
		if(img.getType()==ImagePlus.GRAY8)return VitimageUtils.convertByteToFloatWithoutDynamicChanges(res);
		if(img.getType()==ImagePlus.COLOR_RGB) {convertToGray8(res);return VitimageUtils.convertByteToFloatWithoutDynamicChanges(res);}
		return null;
	}


	/**
	 * Project roi on sub image.
	 *
	 * @param r the r
	 * @return the image plus
	 */
	public static ImagePlus projectRoiOnSubImage(Roi r) {
		Rectangle R=r.getBounds();
		ImagePlus img=ij.gui.NewImage.createFloatImage("", R.width, R.height, 1, ij.gui.NewImage.FILL_BLACK);
		float[]tabData=(float[])img.getStack().getPixels(1);
		for(int x=0;x<R.width;x++)for(int y=0;y<R.height;y++) {
			if(r.contains(R.x+x, R.y+y)) {
				tabData[R.width*y+x]=255;
			}
		}
		return img;
	}


	/**
	 * Project roi tab on sub image.
	 *
	 * @param rTab the r tab
	 * @return the image plus
	 */
	public static ImagePlus projectRoiTabOnSubImage(Roi []rTab) {
		int x=100000;
		int X=0;
		int y=100000;
		int Y=0;
		for(Roi r:rTab) {
			if(r.getBounds().x<x)x=r.getBounds().x;
			if(r.getBounds().y<y)y=r.getBounds().y;
			if((r.getBounds().x+r.getBounds().width)>X)X=r.getBounds().x+r.getBounds().width;
			if((r.getBounds().y+r.getBounds().height)>Y)Y=r.getBounds().y+r.getBounds().height;
		}
		ImagePlus img=ij.gui.NewImage.createFloatImage("", X-x+1, Y-y+1, 1, ij.gui.NewImage.FILL_BLACK);
		float[]tabData=(float[])img.getStack().getPixels(1);
		for(int xx=x;xx<X;xx++)for(int yy=0;yy<Y;yy++)for(Roi r:rTab) if(r.contains(xx,yy)) {
				tabData[(X-x+1)*(yy-y)+(xx-x)]=255;
		}
		return img;
	}

	
	
	/**
	 * Segmentation to roi.
	 *
	 * @param seg the seg
	 * @return the roi[]
	 */
	public static Roi[]segmentationToRoi(ImagePlus seg){
		   
    	ImagePlus imgSeg=VitimageUtils.getBinaryMask(seg, 0.5);
    	if(VitimageUtils.isNullImage(imgSeg))return null;
    	RoiManager rm=RoiManager.getRoiManager();
    	rm.reset();
    	imgSeg.show();
    	Roi rr=imgSeg.getRoi();
    	if(rr!=null)imgSeg.deleteRoi();
//    	imgSeg.resetRoi();
    	IJ.setRawThreshold(imgSeg, 127, 255, null);
        VitimageUtils.waitFor(10);
    	
        IJ.run("Create Selection");
        //VitimageUtils.printImageResume(IJ.getImage(),"getImage");
        Roi r=IJ.getImage().getRoi();
       // System.out.println(r);
        if(r==null)return null;
        Roi[]rois;
        if(r.getClass()==PolygonRoi.class)rois=new Roi[] {r};
        else if(r.getClass()==ShapeRoi.class)rois = ((ShapeRoi)r).getRois();
        else if(r.getClass()==Roi.class)rois=new Roi[] {r};
        else try{
        	rois = ((ShapeRoi)r).getRois();
        }catch(java.lang.ClassCastException cce) {System.out.println(r.getClass());seg.show();seg.setTitle("Bug");cce.printStackTrace();VitimageUtils.waitFor(10000);rm.reset();rm.close(); return null;}
        IJ.getImage().close();
        rm.reset();
        rm.close();
        return rois;
    }

	
	/**
	 * Draw segment into 2 D byte image.
	 *
	 * @param img the img
	 * @param thickness the thickness
	 * @param valToPrint the val to print
	 * @param x0 the x 0
	 * @param y0 the y 0
	 * @param x1 the x 1
	 * @param y1 the y 1
	 * @param dotPoint the dot point
	 */
	public static void drawSegmentInto2DByteImage(ImagePlus img,double thickness,int valToPrint,double x0,double y0,double x1,double y1,boolean dotPoint) {
		int xM=img.getWidth();
		int yM=img.getHeight();
		double chemin=0;
		double[]vectAB=new double[] {x1-x0,y1-y0,0};
		double cheminTarget=TransformUtils.norm(vectAB);
		vectAB=TransformUtils.normalize(vectAB);
		double[]vectOrth=new double[] {vectAB[1],-vectAB[0]};
		byte[] valsImg=(byte [])img.getStack().getProcessor(1).getPixels();
		double delta=0.2;
		int nDec=((int)Math.round(thickness/(delta*2)));
		double xx=x0;
		double yy=y0;
		double deltax=delta*vectAB[0];
		double deltay=delta*vectAB[1];
		double deltaOx=delta*vectOrth[0];
		double deltaOy=delta*vectOrth[1];
		while(chemin<cheminTarget) {
			xx+=deltax;
			yy+=deltay;			
			chemin+=delta;
			//System.out.println(chemin+" : "+( (((int)(chemin))/2)%2));
			if(dotPoint && ( ( (((int)(chemin))/2)%3)==1))continue;
			for(int dec=-nDec;dec<=nDec;dec++) {
				if((yy+deltaOy*dec>=0) && (yy+deltaOy*dec<=(yM-1)) && (xx+deltaOx*dec>=0) && (xx+deltaOx*dec<=(xM-1)) ) {
					valsImg[ xM *((int)Math.round(yy+deltaOy*dec))+((int)Math.round(xx+deltaOx*dec))]=  (byte)( valToPrint & 0xff);
				}
			}			
		}
	}

	
	
	/**
	 * Resize nearest.
	 *
	 * @param img the img
	 * @param targetX the target X
	 * @param targetY the target Y
	 * @param targetZ the target Z
	 * @return the image plus
	 */
	public static ImagePlus resizeNearest(ImagePlus img, int targetX,int targetY,int targetZ) {
		ImagePlus temp=img.duplicate();
        temp=Scaler.resize(temp, targetX,targetY, targetZ, "none"); 		
        if( (temp.getStackSize()==img.getStackSize()) && (temp.getStackSize()>1) && (img.getStackSize()>1) ) {
        	for(int i=0;i<temp.getStackSize();i++)temp.getStack().setSliceLabel(img.getStack().getSliceLabel(i+1), i+1);        	
        }
        VitimageUtils.adjustImageCalibration(temp,img);
        double[]voxSizes=VitimageUtils.getVoxelSizes(img);
        voxSizes[0]*=(img.getWidth()/targetX);
        voxSizes[1]*=(img.getHeight()/targetY);
        voxSizes[2]*=(img.getStackSize()/targetZ);
        VitimageUtils.adjustVoxelSize(temp, voxSizes);
        return temp;
	}

	
	/**
	 * Adjust contrast 3 d.
	 *
	 * @param img the img
	 * @param percentageKeepNormalisation the percentage keep normalisation
	 * @param factorViewNormalisation the factor view normalisation
	 */
	public static void adjustContrast3d(ImagePlus img,double percentageKeepNormalisation,double factorViewNormalisation) {
		double []ranges=getDoubleSidedRangeForContrastMoreIntelligent(img,1,img.getNSlices()/2,1,percentageKeepNormalisation,factorViewNormalisation);
		img.setDisplayRange(ranges[0], ranges[1]);
		img.updateAndDraw();
	}
	
	
	/**
	 * Adds the slice to image.
	 *
	 * @param slice the slice
	 * @param image the image
	 * @return the image plus
	 */
	public static ImagePlus addSliceToImage(ImagePlus slice,ImagePlus image) {
		ImagePlus[]imgs=stackToSlices(image);
		ImagePlus[]end=new ImagePlus[imgs.length+1];
		for(int i=0;i<imgs.length;i++)end[i+1]=imgs[i];
		end[0]=slice;
		return slicesToStack(end);
	}
	
	
	/**
	 * Make operation on one image.
	 *
	 * @param in the in
	 * @param op_1add_2mult_3div_4sub the op 1 add 2 mult 3 div 4 sub
	 * @param val the val
	 * @param copyBefore the copy before
	 * @return the image plus
	 */
	public static ImagePlus makeOperationOnOneImage(ImagePlus in,int op_1add_2mult_3div_4sub,double val,boolean copyBefore) {
		ImagePlus img=null;
		if(copyBefore) {
			img=new Duplicator().run(in);
		}
		else img=in;
		switch (op_1add_2mult_3div_4sub) {
		case OP_ADD :IJ.run(img, "Add...", "value="+val+" stack");break;
		case OP_MULT :IJ.run(img, "Multiply...", "value="+val+" stack");break;
		case OP_DIV :IJ.run(img, "Divide...", "value="+val+" stack");break;
		case OP_SUB :IJ.run(img, "Subtract...", "value="+val+" stack");break;
		}
		for(int c=0;c<img.getNChannels();c++)for(int z=0;z<img.getNSlices();z++)for(int t=0;t<img.getNFrames();t++) {
			if(op_1add_2mult_3div_4sub==OP_MULT) {
				multiplySigma(img,c,z,t,val);
			}
			if(op_1add_2mult_3div_4sub==OP_DIV) {
				multiplySigma(img,c,z,t,1.0/val);
			}
		}
		return img;
	}
	
	
	/**
	 * Multiply sigma.
	 *
	 * @param hyperImg the hyper img
	 * @param c the c
	 * @param z the z
	 * @param t the t
	 * @param factor the factor
	 */
	public static void multiplySigma(ImagePlus hyperImg,int c,int z,int t,double factor) {
		String label=hyperImg.getStack().getSliceLabel(VitimageUtils.getCorrespondingSliceInHyperImage(hyperImg, c, z, t));
		if(label==null)return;
		String[]strTab=label.split("_");
		String newLab="";
		for(int i=0;i<strTab.length;i++) {
			String str=strTab[i];
			
			if(str.contains("SIGMARICE")) {
				double valSig=Double.parseDouble(str.split("=")[1]);
				newLab+=("SIGMARICE="+VitimageUtils.dou(valSig*factor));
			}
			else newLab+=str;
			if(i<strTab.length-1)newLab+="_";
		}
		hyperImg.getStack().setSliceLabel(newLab, VitimageUtils.getCorrespondingSliceInHyperImage(hyperImg, c, z, t));
	}

	
	/**
	 * Restore type.
	 *
	 * @param img the img
	 * @param targetType the target type
	 * @return the image plus
	 */
	public static ImagePlus restoreType(ImagePlus img, int targetType) {
		int imgType=img.getType();
		if(targetType==imgType)return img;
		if(targetType==ImagePlus.GRAY32 && imgType==ImagePlus.GRAY16)return convertShortToFloatWithoutDynamicChanges(img);
		if(targetType==ImagePlus.GRAY32 && imgType==ImagePlus.GRAY8)return convertByteToFloatWithoutDynamicChanges(img);
		if(targetType==ImagePlus.GRAY16 && imgType==ImagePlus.GRAY32)return convertFloatToShortWithoutDynamicChanges(img);
		if(targetType==ImagePlus.GRAY8 && imgType==ImagePlus.GRAY32)return convertByteToFloatWithoutDynamicChanges(img);
		IJ.showMessage("Unexpected conversion in VitimageUtils restoreType : original type="+imgType+" and targetType="+targetType);
		return null;
	}
	
	
	/**
	 * Make operation between two images.
	 *
	 * @param in1 the in 1
	 * @param in2 the in 2
	 * @param op_1add_2mult_3div_4sub the op 1 add 2 mult 3 div 4 sub
	 * @param make32bitResult the make 32 bit result
	 * @return the image plus
	 */
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
		
	
	/**
	 * Multiply.
	 *
	 * @param in1 the in 1
	 * @param in2 the in 2
	 * @param make32bitResult the make 32 bit result
	 * @return the image plus
	 */
	public static ImagePlus multiply(ImagePlus in1,ImagePlus in2,boolean make32bitResult) {
		return makeOperationBetweenTwoImages(in1, in2, 2, make32bitResult);
	}

	/**
	 * Addition.
	 *
	 * @param in1 the in 1
	 * @param in2 the in 2
	 * @param make32bitResult the make 32 bit result
	 * @return the image plus
	 */
	public static ImagePlus addition(ImagePlus in1,ImagePlus in2,boolean make32bitResult) {
		return makeOperationBetweenTwoImages(in1, in2, 1, make32bitResult);
	}
	
	/**
	 * Substract.
	 *
	 * @param in1 the in 1
	 * @param in2 the in 2
	 * @param make32bitResult the make 32 bit result
	 * @return the image plus
	 */
	public static ImagePlus substract(ImagePlus in1,ImagePlus in2,boolean make32bitResult) {
		return makeOperationBetweenTwoImages(in1, in2, 4, make32bitResult);
	}
	
	/**
	 * Divide.
	 *
	 * @param in1 the in 1
	 * @param in2 the in 2
	 * @param make32bitResult the make 32 bit result
	 * @return the image plus
	 */
	public static ImagePlus divide(ImagePlus in1,ImagePlus in2,boolean make32bitResult) {
		return makeOperationBetweenTwoImages(in1, in2, 3, make32bitResult);
	}
	
	/**
	 * Binary operation between two images.
	 *
	 * @param in1 the in 1
	 * @param in2 the in 2
	 * @param op_1OR_2AND_3Pouet_4PRIVEDE the op 1 O R 2 AN D 3 pouet 4 PRIVEDE
	 * @return the image plus
	 */
	public static ImagePlus binaryOperationBetweenTwoImages(ImagePlus in1,ImagePlus in2,int op_1OR_2AND_3Pouet_4PRIVEDE) {
		ImagePlus img=null;
		img=new Duplicator().run(in1);
		VitimageUtils.adjustImageCalibration(img, in1);
		ImageCalculator ic = new ImageCalculator();
		ImagePlus res=null;
		switch (op_1OR_2AND_3Pouet_4PRIVEDE) {
		case OP_ADD :res=ic.run("OR "+" create stack", img, in2);break;
		case OP_MULT :res=ic.run("AND "+" create stack", img, in2);break;
		case OP_DIV :return null;
		case OP_SUB :ImagePlus res2=ic.run("Difference "+" create stack", img, in2);res=ic.run("AND "+" create stack", res2, in1);res2.close();break;
		}
		ImagePlus result=res.duplicate();
		res.close();
		return result;
	}

	/**
	 * Switch axis.
	 *
	 * @param img the img
	 * @param switch_0XY_1XZ_2YZ the switch 0 X Y 1 X Z 2 YZ
	 * @return the image plus
	 */
	public static ImagePlus switchAxis(ImagePlus img,int switch_0XY_1XZ_2YZ) {
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.switchAxis : more than one channel or time");return null;}
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
	
	
	
	
	

	/**
	 * Fade RGB.
	 *
	 * @param img1 the img 1
	 * @param img2 the img 2
	 * @param centerPercent the center percent
	 * @param widthPercent the width percent
	 * @return the image plus
	 */
	public static ImagePlus fadeRGB(ImagePlus img1,ImagePlus img2,int centerPercent,int widthPercent) {
		ImagePlus[]sourceChannels=VitimageUtils.channelSplitter(img1);
		ImagePlus[]segChannels=VitimageUtils.channelSplitter(img2);
		ImagePlus[]targetChannels=new ImagePlus[3];
		
		for(int c=0;c<3;c++)targetChannels[c]=fade8bit(sourceChannels[c],segChannels[c],centerPercent,widthPercent);
		ImagePlus targetRGB=VitimageUtils.compositeRGBByteTab(targetChannels);
		return targetRGB;
	}
	
	/**
	 * Fade 8 bit.
	 *
	 * @param im1 the im 1
	 * @param im2 the im 2
	 * @param centerPercent the center percent
	 * @param widthPercent the width percent
	 * @return the image plus
	 */
	public static ImagePlus fade8bit(ImagePlus im1,ImagePlus im2,int centerPercent,int widthPercent) {
		ImagePlus img1=VitimageUtils.convertByteToFloatWithoutDynamicChanges(im1);
		ImagePlus img2=VitimageUtils.convertByteToFloatWithoutDynamicChanges(im2);
		ImagePlus img3=img2.duplicate();
		int X=img1.getWidth();
		int Y=img1.getHeight();

		int xStart=(int) ((centerPercent-widthPercent/2.0)*0.01*X);
		int yStart=(int) ((centerPercent-widthPercent/2.0)*0.01*Y);
		int xStop=(int) ((centerPercent+widthPercent/2.0)*0.01*X);
		int yStop=(int) ((centerPercent+widthPercent/2.0)*0.01*Y);
		System.out.println("Xstart="+xStart);
		System.out.println("Xstop="+xStop);
		float[]tab1=(float [])img1.getStack().getProcessor(1).getPixels();
		float[]tab2=(float [])img2.getStack().getProcessor(1).getPixels();
		float[]tab3=(float [])img3.getStack().getProcessor(1).getPixels();
		for(int x=0;x<X;x++)for(int y=0;y<Y;y++) {
			int index=X*y+x;
			if(x<=xStart)tab3[index]=tab1[index];
			else if(x>=xStop)tab3[index]=tab2[index];
			else {
				double ratio2=(x-xStart)/(1.0*(xStop-xStart));
				tab3[index]=(float) (ratio2*tab2[index]+(1-ratio2)*tab1[index]);
			}
		}
		img3=VitimageUtils.convertFloatToByteWithoutDynamicChanges(img3);
		return img3;
	}

	
	
	
	
	
	
	
	
	/**
	 * Restriction mask for fading handling.
	 *
	 * @param img the img
	 * @param marginOut the margin out
	 * @return the image plus
	 */
	public static ImagePlus restrictionMaskForFadingHandling (ImagePlus img,int marginOut){
		if(img.getNChannels()>1 || img.getNFrames()>1) {IJ.showMessage("Warning in VitimageUtils.restrictionMaskForFadingHandling : more than one channel or time");return null;}
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

	/**
	 * Gaussian filtering IJ.
	 *
	 * @param imgIn the img in
	 * @param sigmaX the sigma X
	 * @param sigmaY the sigma Y
	 * @param sigmaZ the sigma Z
	 * @return the image plus
	 */
	public static ImagePlus gaussianFilteringIJ(ImagePlus imgIn,double sigmaX,double sigmaY,double sigmaZ) {
		ImagePlus img=new Duplicator().run(imgIn);
		double []voxSizes=VitimageUtils.getVoxelSizes(imgIn);
		double sigX=sigmaX/voxSizes[0];
		double sigY=sigmaY/voxSizes[1];
		double sigZ=sigmaZ/voxSizes[2];
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigX+" y="+sigY+" z="+sigZ);		
		return img;
	}
	
	/**
	 * Resize.
	 *
	 * @param img the img
	 * @param targetX the target X
	 * @param targetY the target Y
	 * @param targetZ the target Z
	 * @return the image plus
	 */
	public static ImagePlus resize(ImagePlus img, int targetX,int targetY,int targetZ) {
        ImagePlus temp=Scaler.resize(img, targetX,targetY, targetZ, " interpolation=Bilinear average create"); 		
        if( (temp.getStackSize()==img.getStackSize()) && (temp.getStackSize()>1) && (img.getStackSize()>1) ) {
        	for(int i=0;i<temp.getStackSize();i++)temp.getStack().setSliceLabel(img.getStack().getSliceLabel(i+1), i+1);        	
        }
        return temp;
	}

	/**
	 * Gaussian filtering.
	 *
	 * @param imgIn the img in
	 * @param sigmaX the sigma X
	 * @param sigmaY the sigma Y
	 * @param sigmaZ the sigma Z
	 * @return the image plus
	 */
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
		if(imgIn.getNSlices()>=4 && sigmaZ>0) {
			gaussFilter.setDirection(2);
			gaussFilter.setSigma(sigmaZ);
			img=gaussFilter.execute(img);
		}
		ImagePlus ret=ItkImagePlusInterface.itkImageToImagePlus(img);
		for(int z=0;z<imgIn.getStackSize();z++)ret.getStack().setSliceLabel(imgIn.getStack().getSliceLabel(z+1), z+1);
		return ret;
	}
			
	

	
	/**
	 * Gets the roi as coords.
	 *
	 * @param r the r
	 * @return the roi as coords
	 */
	public static int[][] getRoiAsCoords(Roi r) {
		int type=r.getType();
		int incr=0;
		if(type==0) {//Rectangle
			IJ.log("Roi type "+type+" rectangle");
			final Rectangle rect = r.getBounds();
			final int x0 = rect.x;			final int y0 = rect.y;
			final int lastX = x0 + rect.width;  final int lastY = y0 + rect.height;
			int[][]ret=new int[rect.width*rect.height][2];
			for( int x = x0; x < lastX; x++ ) {
				for( int y = y0; y < lastY; y++ ){
					ret[incr++]=new int[] {x,y};
				}
			}	
			return ret;
		}
		else if(type==1 || type==2) {//Ovale
			IJ.log("Roi type "+type+" oval");
			final Rectangle rect = r.getBounds();
			final int x0 = rect.x;			final int y0 = rect.y;
			final int lastX = x0 + rect.width;			final int lastY = y0 + rect.height;
			for( int x = x0; x < lastX; x++ ) {				for( int y = y0; y < lastY; y++ ){					if(r.contains(x, y))					incr++;				}			}			
			int[][]ret=new int[incr][2];
			incr=0;
			for( int x = x0; x < lastX; x++ ) {				for( int y = y0; y < lastY; y++ ){					if(r.contains(x, y))					ret[incr++]=new int[] {x,y};				}			}			
			return ret;
		}
		else if(type>=3) {//Free
			IJ.log("Roi type "+type+" other (freeline, shape, ...)");
			int[] x = r.getPolygon().xpoints;
			int[] y = r.getPolygon().ypoints;
			final int n = r.getPolygon().npoints;
			int[][]ret=new int[n][2];
			for( int ind = 0; ind < n; ind++ ) {
				ret[ind]=new int[] {x[ind],y[ind]};
			}	
			return ret;
		}
		else return null;
	}
	



	
	
	
	/**
	 * Helper functions to switch values in images.
	 *
	 * @param img the img
	 * @param valueBefore the value before
	 * @param valueAfter the value after
	 * @return the image plus
	 */		
	public static ImagePlus switchValueInImageFloat(ImagePlus img,float valueBefore, float valueAfter) {
		if(img.getType()!=ImagePlus.GRAY32)return null;
		ImagePlus out=img.duplicate();
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		for(int z=0;z<zMax;z++) {
			float[]valsImg=(float [])out.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xMax;x++) {
				for(int y=0;y<yMax;y++) {
					if(  valsImg[xMax*y+x]==valueBefore ) valsImg[xMax*y+x]=valueAfter;
				}
			}			
		}
		return out;
	}
	
	
	
	
	
	/**
	 * Combine 2 D array of image.
	 *
	 * @param tabTmp the tab tmp
	 * @return the image plus
	 */
	public static ImagePlus combine2DArrayOfImage(ImagePlus[][]tabTmp) {
		int M=tabTmp.length;
		int N=tabTmp[0].length;
		ImagePlus[][]tab=new ImagePlus[M][N];
		int mRef=-1;int nRef=-1;
		for(int m=0;m<M;m++) {
			for(int n=0;n<N;n++) {
				if(tabTmp[m][n]!=null) {						
					tab[m][n]=tabTmp[m][n].duplicate();
					mRef=m; nRef=n;
				}
			}
		}
		for(int m=0;m<M;m++) {
			for(int n=0;n<N;n++) {
				if(tab[m][n]==null)tab[m][n]=VitimageUtils.nullImage(tab[mRef][nRef]);
			}
		}
		StackCombiner sc=new StackCombiner();
		ImageStack[]isLines=new ImageStack[tab.length];
		for(int m=0;m<M;m++) {
			isLines[m]=tab[m][0].getStack();
			for(int n=1;n<N;n++) {
				isLines[m]=sc.combineHorizontally(isLines[m], tab[m][n].getStack());
			}
			if(m>0) {
				isLines[0]=sc.combineVertically(isLines[0], isLines[m]);
			}
		}
		ImagePlus ret=new ImagePlus("",isLines[0]);
		VitimageUtils.adjustImageCalibration(ret,tab[0][0]);
		return ret;
	}

	
	/**
	 * Show with params.
	 *
	 * @param img the img
	 * @param title the title
	 * @param zSlice the z slice
	 * @param min the min
	 * @param max the max
	 * @param waiting the waiting
	 */
	public static void showWithParams(ImagePlus img,String title,int zSlice,double min, double max,int waiting) {
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {public void run() {			
			showWithParams(img,title,zSlice,min,max);
			ImagePlus img=WindowManager.getImage(title);
			img.changes=false;
			VitimageUtils.waitFor(waiting);
			img.close();
		}});
	}
	
	
	
	/**
	 * Show with params.
	 *
	 * @param img the img
	 * @param title the title
	 * @param zSlice the z slice
	 * @param min the min
	 * @param max the max
	 */
	public static void showWithParams(ImagePlus img,String title,int zSlice,double min, double max) {
		if(img.getType()!=ImagePlus.COLOR_RGB)				setLutToFire(img);
		img.setTitle(title);
		img.setSlice(zSlice);
		img.setDisplayRange(min, max);
		img.show();
		img.updateAndRepaintWindow();
	}
	
	
	/**
	 * Gaussian interpolation.
	 *
	 * @param img the img
	 * @param threshLow the thresh low
	 * @param sigmaSI the sigma SI
	 * @return the image plus
	 */
	public static ImagePlus gaussianInterpolation(ImagePlus img, double threshLow,double sigmaSI) {
		ImagePlus imgTemp=img.duplicate();
		imgTemp=convertToFloat(imgTemp);
		ImagePlus mask=VitimageUtils.thresholdImage(imgTemp, threshLow, 1E20);
//		showWithParams(mask, "mask", 1, 0, 1);
		mask=gaussianFilteringIJ(mask, sigmaSI, sigmaSI, sigmaSI);
		ImagePlus smallVals=thresholdFloatImage(mask, -1E10, 1E-8);
		mask=makeOperationBetweenTwoImages(smallVals, mask, 1, true);
		

		ImagePlus imgGauss=gaussianFilteringIJ(imgTemp, sigmaSI, sigmaSI, sigmaSI);
		ImagePlus ret=makeOperationBetweenTwoImages(imgGauss, mask, 3, true);
		return ret;
	}
	

	
	

	/**
	 * Switch two values in image float.
	 *
	 * @param img the img
	 * @param valueBefore the value before
	 * @param valueAfter the value after
	 * @param valueBefore2 the value before 2
	 * @param valueAfter2 the value after 2
	 * @return the image plus
	 */
	public static ImagePlus switchTwoValuesInImageFloat(ImagePlus img,float valueBefore, float valueAfter,float valueBefore2, float valueAfter2) {
		if(img.getType()!=ImagePlus.GRAY32)return null;
		ImagePlus out=img.duplicate();
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		for(int z=0;z<zMax;z++) {
			float[]valsImg=(float [])out.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<xMax;x++) {
				for(int y=0;y<yMax;y++) {
					//System.out.println("Comparing"+ valsImg[xMax*y+x]+" and "+valueBefore);
					if(  valsImg[xMax*y+x]==valueBefore ) {
						//System.out.println("ok");
						valsImg[xMax*y+x]=valueAfter;
					}
					//System.out.println("Comparing"+ valsImg[xMax*y+x]+" and "+valueBefore2);
					else if(  valsImg[xMax*y+x]==valueBefore2 ) {
						valsImg[xMax*y+x]=valueAfter2;
						//System.out.println("ok");
					}
				}
			}			
		}
		return out;
	}

	/**
	 * Switch value in image.
	 *
	 * @param img the img
	 * @param valueBefore the value before
	 * @param valueAfter the value after
	 * @return the image plus
	 */
	public static ImagePlus switchValueInImage(ImagePlus img,int valueBefore, int valueAfter) {
		ImagePlus out=img.duplicate();
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

	/**
	 * Switch two values in image.
	 *
	 * @param img the img
	 * @param valueBefore the value before
	 * @param valueAfter the value after
	 * @param valueBefore2 the value before 2
	 * @param valueAfter2 the value after 2
	 * @return the image plus
	 */
	public static ImagePlus switchTwoValuesInImage(ImagePlus img,int valueBefore, int valueAfter,int valueBefore2, int valueAfter2) {
		ImagePlus out=img.duplicate();
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
						else if(  ((int) ( ((byte)valsImg[xMax*y+x])  & 0xff) ==valueBefore2 ) ) {
							valsImg[xMax*y+x]=(byte)( valueAfter2 &0xff);
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
						//System.out.println("Comparing "+ ((int) ( ((byte)valsImg[xMax*y+x])  & 0xffff))+ " and "+valueBefore);
						if(  ((int) ( ((short)valsImg[xMax*y+x])  & 0xffff) ==valueBefore ) ) {
							//System.out.println("Ok 1");
							valsImg[xMax*y+x]=(short)( valueAfter &0xffff);
						}
						else if(  ((int) ( ((short)valsImg[xMax*y+x])  & 0xffff) ==valueBefore2 ) ) {
							//System.out.println("Ok 2");
							valsImg[xMax*y+x]=(short)( valueAfter2 &0xffff);
						}
					}
				}			
			}
		}
		return out;
	}

		
	/**
	 * Sets the image to value.
	 *
	 * @param imgIn the img in
	 * @param value the value
	 * @return the image plus
	 */
	public static ImagePlus setImageToValue(ImagePlus imgIn,double value) {
		if(imgIn.getType()==ImagePlus.GRAY32)return set32bitToValue(imgIn,value);
		if(imgIn.getType()==ImagePlus.GRAY16)return set16bitToValue(imgIn,(int)Math.round(value));
		if(imgIn.getType()==ImagePlus.GRAY8)return set8bitToValue(imgIn,(int)Math.round(value));
		return null;
	}
	
	/**
	 * Sets the 8 bit to value.
	 *
	 * @param imgIn the img in
	 * @param value the value
	 * @return the image plus
	 */
	public static ImagePlus set8bitToValue(ImagePlus imgIn,int value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}
	
	/**
	 * Sets the 16 bit to value.
	 *
	 * @param imgIn the img in
	 * @param value the value
	 * @return the image plus
	 */
	public static ImagePlus set16bitToValue(ImagePlus imgIn,int value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}

	/**
	 * Sets the 32 bit to value.
	 *
	 * @param imgIn the img in
	 * @param value the value
	 * @return the image plus
	 */
	public static ImagePlus set32bitToValue(ImagePlus imgIn,double value) {
		ImagePlus imgOut=new Duplicator().run(imgIn);
		for(int i=1;i<=imgIn.getStackSize();i++) {
			imgOut.getStack().getProcessor(i).set(value);
		}
		return imgOut;
	}

	/**
	 * Sets the 32 bit to value.
	 *
	 * @param imgIn the img in
	 * @param value the value
	 * @param z the z
	 */
	public static void set32bitToValue(ImagePlus imgIn,double value,int z) {
		ImageProcessor imgproc=imgIn.getStack().getProcessor(z+1);
		imgproc.set(value);
		imgIn.getStack().setProcessor(imgproc,z+1);
	}
	
	/**
	 * Gaussian filtering multi channel.
	 *
	 * @param img the img
	 * @param sigmaXvoxels the sigma xvoxels
	 * @param sigmaYvoxels the sigma yvoxels
	 * @param sigmaZvoxels the sigma zvoxels
	 * @return the image plus
	 */
	public static ImagePlus gaussianFilteringMultiChannel(ImagePlus img,double sigmaXvoxels,double sigmaYvoxels,double sigmaZvoxels) {
		int nbZ=img.getNSlices();
		int nbT=img.getNFrames();
		int nbC=img.getNChannels();
		double []voxs=VitimageUtils.getVoxelSizes(img);
		ImagePlus []imgTabMov=VitimageUtils.stacksFromHyperstackFastBis(img);
		for(int i=0;i<imgTabMov.length;i++) 
				imgTabMov[i]= gaussianFiltering(imgTabMov[i],sigmaXvoxels*voxs[0],sigmaYvoxels*voxs[1],sigmaZvoxels*voxs[2]);
		Concatenator con=new Concatenator();
		con.setIm5D(true);
		ImagePlus img2=con.concatenate(imgTabMov,false);
		img2=HyperStackConverter.toHyperStack(img2, nbC, nbZ,nbT,"xyztc","Grayscale");
		VitimageUtils.adjustImageCalibration(img2, img);
		return img2;
	}
	
	
	/**
	 * Sets the image window size to.
	 *
	 * @param imgView the img view
	 * @param targetWidth the target width
	 */
	public static void setImageWindowSizeTo(ImagePlus imgView,int targetWidth) {
		int max=0;
		java.awt.Rectangle w = imgView.getWindow().getBounds();
		//If little image, enlarge it until its size is between half screen and full screen
		while(imgView.getWindow().getWidth()<(targetWidth) && (max++)<4) {
			int sx=imgView.getCanvas().screenX((int) (w.x+w.width));
			int sy=imgView.getCanvas().screenY((int) (w.y+w.height));
			imgView.getCanvas().zoomIn(sx, sy);
			VitimageUtils.waitFor(50);
		}
		max=0;
		//If big image, reduce it until its size is between half screen and full screen
		while(imgView.getWindow().getWidth()>(targetWidth) && (max++)<4) {
			int sx=imgView.getCanvas().screenX((int) (w.x+w.width));
			int sy=imgView.getCanvas().screenY((int) (w.y+w.height));
			imgView.getCanvas().zoomOut(sx, sy);
		}
	}

	/**
	 * Copy image calibration and range.
	 *
	 * @param target the target
	 * @param source the source
	 */
	public static void copyImageCalibrationAndRange(ImagePlus target,ImagePlus source) {
		if(target.getNChannels()>source.getNChannels()) {IJ.showMessage("Warning in VitimageUtils.copyImageCalibrationAndRange : channels does not match");return;}
		adjustImageCalibration(target, source);
		LUT[]lu=source.getLuts();
		for(int c=0;c<target.getNChannels();c++) {
			target.setC(c+1);
			source.setC(c+1);
			target.setDisplayRange(source.getDisplayRangeMin(), source.getDisplayRangeMax());
			target.setLut(lu[c]);
			//			IJ.run(target,"Fire","");
		}
	}
	
	/**
	 * Crop multi channel float image.
	 *
	 * @param img the img
	 * @param x0 the x 0
	 * @param xf the xf
	 * @param y0 the y 0
	 * @param yf the yf
	 * @param z0 the z 0
	 * @param zf the zf
	 * @return the image plus
	 */
	public static ImagePlus cropMultiChannelFloatImage(ImagePlus img, int x0,int xf,int y0,int yf, int z0, int zf) {
		int nbT=img.getNFrames();
		int nbC=img.getNChannels();
		ImagePlus []imgTabMov=VitimageUtils.stacksFromHyperstackFastBis(img);
		for(int i=0;i<imgTabMov.length;i++) {
			imgTabMov[i]= VitimageUtils.cropImageFloat(imgTabMov[i], x0, y0, z0, xf-x0+1, yf-y0+1, zf-z0+1);
			//System.out.println("Processing i="+i+imageResume(imgTabMov[i]));
		}
		Concatenator con=new Concatenator();
		con.setIm5D(true);
		ImagePlus img2=con.concatenate(imgTabMov,false);
		img2=HyperStackConverter.toHyperStack(img2, nbC, zf-z0+1,nbT,"xyztc","Grayscale");
		VitimageUtils.adjustImageCalibration(img2, img);
		return img2;
	}
	
	
	
	
	
	
	
	/**
	 * Gets the averaging.
	 *
	 * @param img the img
	 * @return the averaging
	 */
	public static int getAveraging(ImagePlus img) {
		String[]strLines=new ImageInfo().getImageInfo(img).split("\n");
		for(String str : strLines)if(str.contains("Number of Averages"))return Integer.parseInt(str.split(": ")[1].split(" ")[0]);
		return 2;
	}
		
	/**
	 * Gets the repetition time.
	 *
	 * @param img the img
	 * @return the repetition time
	 */
	public static double getRepetitionTime(ImagePlus img) {
		String[]strLines=new ImageInfo().getImageInfo(img).split("\n");
		for(String str : strLines)if(str.contains("Repetition Time"))return Double.parseDouble(str.split(": ")[1].split(" ")[0]);
		return 1;
	}
	
	/**
	 * Checks if is bouture.
	 *
	 * @param img the img
	 * @return true, if is bouture
	 */
	public static boolean isBouture(ImagePlus img) {
		String patientName=VitimageUtils.getPatientName(img);
		if(patientName.contains("BOUTURE"))return true;
		if( patientName.contains("_muscat_") || patientName.contains("_ugni_") || patientName.contains("_vigne_") || patientName.contains("_MMC_") || patientName.contains("_MSC_") || patientName.contains("_SSC_") || patientName.contains("_SO4_") || patientName.contains("_I_") || patientName.contains("Merlot") )return true;
		if( patientName.contains("_J") && (patientName.contains("_B") || patientName.contains("DS") ||patientName.contains("PAL") || patientName.contains("PCH") || patientName.contains("NP") || patientName.contains("EL") || patientName.contains("CT"))) return true;
		patientName=img.getStack().getShortSliceLabel(1);
		if(patientName==null)patientName="";
		System.out.println("BOUTURE OBS : "+patientName);
		if(patientName.contains("BOUTURE"))return true;
		if( patientName.contains("_muscat_") || patientName.contains("_ugni_") || patientName.contains("_vigne_") || patientName.contains("_MMC_") || patientName.contains("_MSC_") || patientName.contains("_SSC_") || patientName.contains("_SO4_") || patientName.contains("_I_") || patientName.contains("Merlot") )return true;
		if( patientName.contains("_J") && (patientName.contains("_B") || patientName.contains("DS") ||patientName.contains("PAL") || patientName.contains("PCH") || patientName.contains("NP") || patientName.contains("EL") || patientName.contains("CT"))) return true;
		return false;
	}
	
	
	/**
	 * Gets the patient name.
	 *
	 * @param img the img
	 * @return the patient name
	 */
	public static String getPatientName(ImagePlus img) {
	//	System.out.println("CRASH "+imageResume(img));
		if(new ImageInfo().getImageInfo(img)==null)return "";
		String[]strLines=new ImageInfo().getImageInfo(img).split("\n");
		for(String str : strLines)if(str.contains("Patient ID"))return str.split(": ")[1].split(" ")[0];
		//No patient Name
		return "";
	}

	
	/**
	 * Gets the echo time.
	 *
	 * @param img the img
	 * @return the echo time
	 */
	public static double getEchoTime(ImagePlus img) {
		String[]strLines=new ImageInfo().getImageInfo(img).split("\n");
		for(String str : strLines)if(str.contains("Echo Time"))return Double.parseDouble(str.split(": ")[1].split(" ")[0]);
		//IJ.showMessage("Warning in VitimageUtils.getEchoTime : no detectedEchoTime in "+VitimageUtils.imageResume(img));
		return 1;
	}
	
	
	
	
	/**
	 * Wrappers or implementations of some useful thresholding and dynamic adjustment.
	 *
	 * @param imgIn the img in
	 * @return the image plus
	 */
	public static ImagePlus convertShortToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		ret=IJ.createImage("", "32-bit", ret.getWidth(), ret.getHeight(), ret.getNChannels(), ret.getNSlices(), ret.getNFrames());
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
			ret.getStack().setSliceLabel(imgIn.getStack().getSliceLabel(z+1), z+1);
		}
		transferProperties(imgIn, ret);
		
		return ret;
	}
	
	/**
	 * Convert float to short without dynamic changes.
	 *
	 * @param imgIn the img in
	 * @return the image plus
	 */
	public static ImagePlus convertFloatToShortWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		if(imgIn.getType()==ImagePlus.GRAY16)return ret;
		ret=IJ.createImage("", "16-bit", ret.getWidth(), ret.getHeight(), ret.getNChannels(), ret.getNSlices(), ret.getNFrames());
		float[][] in=new float[imgIn.getStackSize()][];
		short[][] out=new short[ret.getStackSize()][];
		int X=imgIn.getWidth();
		for(int z=0;z<imgIn.getStackSize();z++) {
			in[z]=(float []) imgIn.getStack().getProcessor(z+1).getPixels();
			out[z]=(short []) ret.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					out[z][y*X+x]=(short)((int)(Math.round(in[z][y*X+x])));
				}			
			}
			ret.getStack().setSliceLabel(imgIn.getStack().getSliceLabel(z+1), z+1);
		}
		transferProperties(imgIn, ret);
		
		return ret;
	}

	/**
	 * Convert float to byte without dynamic changes.
	 *
	 * @param imgIn the img in
	 * @return the image plus
	 */
	public static ImagePlus convertFloatToByteWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=new Duplicator().run(imgIn);
		if(imgIn.getType()==ImagePlus.GRAY8)return ret;
		ret=IJ.createImage("", "8-bit", ret.getWidth(), ret.getHeight(), ret.getNChannels(), ret.getNSlices(), ret.getNFrames());
		float[][] in=new float[imgIn.getStackSize()][];
		byte[][] out=new byte[ret.getStackSize()][];
		int X=imgIn.getWidth();
		int res=0;
		for(int z=0;z<imgIn.getStackSize();z++) {
			in[z]=(float []) imgIn.getStack().getProcessor(z+1).getPixels();
			out[z]=(byte []) ret.getStack().getProcessor(z+1).getPixels();

			for(int x=0;x<imgIn.getWidth();x++) {
				for(int y=0;y<imgIn.getHeight();y++) {
					res=((int)(Math.round(in[z][y*X+x])));
					if(res<0)res=0;
					if(res>255)res=255;
					out[z][y*X+x]=(byte)res;
				}			
			}
			ret.getStack().setSliceLabel(imgIn.getStack().getSliceLabel(z+1), z+1);
		}
		transferProperties(imgIn, ret);
		
		return ret;
	}

	
	/**
	 * Threshold image to float mask.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus thresholdImageToFloatMask(ImagePlus img,double thresholdMin, double thresholdMax) {
		if(img.getType()==ImagePlus.GRAY32)return thresholdFloatImageToFloatMask(img,thresholdMin,thresholdMax);
		if(img.getType()==ImagePlus.GRAY16)return thresholdShortImageToFloatMask(img,thresholdMin,thresholdMax);
		if(img.getType()==ImagePlus.GRAY8)return thresholdByteImageToFloatMask(img,thresholdMin,thresholdMax);
		return null;
	}
	
	/**
	 * Threshold image.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus thresholdImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		if(img.getType()==ImagePlus.GRAY32)return thresholdFloatImage(img,thresholdMin,thresholdMax);
		if(img.getType()==ImagePlus.GRAY16)return thresholdShortImage(img,thresholdMin,thresholdMax);
		if(img.getType()==ImagePlus.GRAY8)return thresholdByteImage(img,thresholdMin,thresholdMax);
		return null;
	}
		
	/**
	 * Threshold byte image.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
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
	
	/**
	 * Threshold short image.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus thresholdShortImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		VitimageUtils.adjustImageCalibration(ret,img);
		short[][] in=new short[img.getStackSize()][];
		short[][] out=new short[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(short []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(short []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[z][y*X+x] & 0xffff);
					out[z][y*X+x]=(  val >= thresholdMin ? (        val < thresholdMax  ?   ((short) (255 & 0xffff)) : ((short) (0 & 0xffff)) ) : ((short) (0 & 0xffff)) );
				}			 
			}
		}
		ret.setDisplayRange(0, 255);
		return ret;

	}

	/**
	 * Threshold float image.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus thresholdFloatImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		return getFloatBinaryMask(img,thresholdMin,thresholdMax);
	}

	/**
	 * Threshold byte image to float mask.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus thresholdByteImageToFloatMask(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		ret=convertToFloat(ret);
		VitimageUtils.adjustImageCalibration(ret,img);
		byte[][] in=new byte[img.getStackSize()][];
		float[][] out=new float[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(byte []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[z][y*X+x] & 0xff);
					out[z][y*X+x]=(  val >= thresholdMin ? (        val < thresholdMax  ?   1 : 0 ) : 0 );
				}			 
			}
		}
		return ret;

	}
	
	/**
	 * Threshold short image to float mask.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus thresholdShortImageToFloatMask(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		ret=convertToFloat(ret);
		VitimageUtils.adjustImageCalibration(ret,img);
		short[][] in=new short[img.getStackSize()][];
		float[][] out=new float[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			in[z]=(short []) img.getStack().getProcessor(z+1).getPixels();
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					int val=(int)(in[z][y*X+x] & 0xffff);
					out[z][y*X+x]=(  val >= thresholdMin ? (        val < thresholdMax  ?  1 : 0 ) : 0 );
				}			 
			}
		}
		return ret;

	}

	/**
	 * Threshold float image to float mask.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus thresholdFloatImageToFloatMask(ImagePlus img,double thresholdMin, double thresholdMax) {
		return getFloatBinaryMask(img,thresholdMin,thresholdMax);
	}

	
	
	
	/**
	 * Gets the float binary mask.
	 *
	 * @param img the img
	 * @param valMin the val min
	 * @param valMax the val max
	 * @return the float binary mask
	 */
	public static ImagePlus getFloatBinaryMask(ImagePlus img,double valMin, double valMax) {
		ImagePlus ret=new Duplicator().run(img);
		ret=convertToFloat(ret);
		float[][] out=new float[ret.getStackSize()][];
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			out[z]=(float []) ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					out[z][y*X+x]= (float)(out[z][y*X+x] < valMin ? 0 : (out[z][y*X+x] > valMax ? 0 : 1.0));
				}			 
			}
		}
		return ret;

	}

	/**
	 * Fill 3 d holes in mask.
	 *
	 * @param binaryMask the binary mask
	 * @param threshold the threshold
	 * @param maxVolumeToFill the max volume to fill
	 * @param connexity the connexity
	 * @return the image plus
	 */
	public static ImagePlus fill3dHolesInMask(ImagePlus binaryMask,double threshold,double maxVolumeToFill,int connexity) {
		ImagePlus imgBackToKeepToBack=connexe(binaryMask,-1E10,threshold,maxVolumeToFill,1E40,connexity,0,true);
		imgBackToKeepToBack=VitimageUtils.thresholdImageToFloatMask(imgBackToKeepToBack, 0.9, 1E10);
		return invertBinaryMask(imgBackToKeepToBack);
	}
	
	/**
	 * Fill 2 d holes in mask.
	 *
	 * @param binaryMask the binary mask
	 * @param threshold the threshold
	 * @param maxVolumeToFill the max volume to fill
	 * @param connexity the connexity
	 * @return the image plus
	 */
	public static ImagePlus fill2dHolesInMask(ImagePlus binaryMask,double threshold,double maxVolumeToFill,int connexity) {
		ImagePlus imgBackToKeepToBack=connexe2d(binaryMask,-1E10,threshold,maxVolumeToFill,1E40,connexity,0,true);
		imgBackToKeepToBack=VitimageUtils.thresholdImageToFloatMask(imgBackToKeepToBack, 0.9, 1E10);
		return invertBinaryMask(imgBackToKeepToBack);
	}

	/**
	 * Removes the 2 d blops in object.
	 *
	 * @param binaryMask the binary mask
	 * @param threshold the threshold
	 * @param maxVolumeToRemove the max volume to remove
	 * @param connexity the connexity
	 * @return the image plus
	 */
	public static ImagePlus remove2dBlopsInObject(ImagePlus binaryMask,double threshold,double maxVolumeToRemove,int connexity) {
		ImagePlus imgBackToKeepToBack=connexe2d(binaryMask,threshold,1E10,maxVolumeToRemove,1E40,connexity,0,true);
		imgBackToKeepToBack=VitimageUtils.thresholdImageToFloatMask(imgBackToKeepToBack, 0.9, 1E10);
		return imgBackToKeepToBack;
	}
	
	/**
	 * Invert binary mask.
	 *
	 * @param binaryMask the binary mask
	 * @return the image plus
	 */
	public static ImagePlus invertBinaryMask(ImagePlus binaryMask) {
		if(binaryMask.getType()==ImagePlus.GRAY16 || binaryMask.getType()==ImagePlus.GRAY8) {
			int valMax=(int)Math.round(maxOfImage(binaryMask));
			int valMin=(int)Math.round(minOfImage(binaryMask));
			if(valMax==valMin)valMax=1;
			//System.out.println("Max="+valMax+"  Min="+valMin);
			return switchTwoValuesInImage(binaryMask, valMax, valMin,valMin, valMax);
		}
		if(binaryMask.getType()==ImagePlus.GRAY32) {
			float valMax=maxOfImage(binaryMask);
			float valMin=minOfImage(binaryMask);
			return switchTwoValuesInImageFloat(binaryMask,valMax, valMin, valMin, valMax);
		}
		return null;
	}

	
	/**
	 * Clip float image.
	 *
	 * @param img the img
	 * @param thresholdMin the threshold min
	 * @param thresholdMax the threshold max
	 * @return the image plus
	 */
	public static ImagePlus clipFloatImage(ImagePlus img,double thresholdMin, double thresholdMax) {
		ImagePlus ret=new Duplicator().run(img);
		float[] data;
		float max=(float)thresholdMax;
		float min=(float)thresholdMin;
		int X=img.getWidth();
		int Y=img.getHeight();
		int Z=img.getStackSize();
		for(int z=0;z<Z;z++) {
			data= (float[])ret.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<X;x++) {
				for(int y=0;y<Y;y++) {
					float val=data[y*X+x];
					if(val>max)data[y*X+x]=max;
					if(val<min)data[y*X+x]=min;
				}			 
			}
		}
		return ret;
	}

	
	
	/**
	 * Convert byte to float without dynamic changes.
	 *
	 * @param imgIn the img in
	 * @return the image plus
	 */
	public static ImagePlus convertByteToFloatWithoutDynamicChanges(ImagePlus imgIn) {
		ImagePlus ret=VitimageUtils.imageCopy(imgIn);
		ret=IJ.createImage("", "32-bit", ret.getWidth(), ret.getHeight(), ret.getNChannels(), ret.getNSlices(), ret.getNFrames());
		

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
		transferProperties(imgIn, ret);
		
		return ret;
	}
	
	/**
	 * Gets the otsu threshold.
	 *
	 * @param img the img
	 * @return the otsu threshold
	 */
	public static double getOtsuThreshold(ImagePlus img) {
		OtsuThresholdImageFilter otsu=new OtsuThresholdImageFilter();
		otsu.execute(ItkImagePlusInterface.imagePlusToItkImage(img));
		return otsu.getThreshold();
	}

	/**
	 * Otsu threshold.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus otsuThreshold(ImagePlus img) {
		OtsuThresholdImageFilter otsu=new OtsuThresholdImageFilter();
		otsu.setInsideValue((short)0);
		otsu.setOutsideValue((short)255);
		return(ItkImagePlusInterface.itkImageToImagePlus(otsu.execute(ItkImagePlusInterface.imagePlusToItkImage(img))));
	}


	
	
	
	

	/**
	 * Sum of images.
	 *
	 * @param tabImg the tab img
	 * @return the image plus
	 */
	public static ImagePlus sumOfImages(ImagePlus []tabImg) {
		ImagePlus ret=tabImg[0].duplicate();
		ret=convertToFloat(ret);
		for(int i=1;i<tabImg.length;i++)ret=VitimageUtils.makeOperationBetweenTwoImages(ret, tabImg[i], 1,true);
		return ret;
	}
	
	
	
	
	/**
	 * Many useful functions not organized yet.
	 *
	 * @param N the n
	 * @param message the message
	 */
	public static void printDebugIntro(int N,String message) {
		for(int i=0;i<N;i++)System.out.println("################## DEBUG ###########");
		System.out.println(message);
	}
		
	/**
	 * Str int 6 chars.
	 *
	 * @param nb the nb
	 * @return the string
	 */
	public static String strInt6chars(int nb) {
		if(nb<100000 && nb>9999)  return new String(" "+nb);
		if(nb<10000 && nb>999)    return new String("  "+nb);
		if(nb<1000 && nb>99)      return new String("   "+nb);
		if(nb<100 && nb>9)        return new String("    "+nb);
		if(nb<10)                 return new String("     "+nb);
		                          return new String(""+nb);
	}
		
	/**
	 * Dou.
	 *
	 * @param d the d
	 * @return the double
	 */
	public static double dou(double d){
		if(d<0)return (-dou(-d));
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}

	/**
	 * Dou.
	 *
	 * @param d the d
	 * @param n the n
	 * @return the double
	 */
	public static double dou(double d,int n){
		if(d<0)return (-dou(-d));
		if (d<Math.pow(10, -n))return 0;
		return (double)(Math.round(d * Math.pow(10, n))/Math.pow(10, n));
	}
	
	/**
	 * Wait for.
	 *
	 * @param n the n
	 */
	public static void waitFor(int n) {
		try {
			java.util.concurrent.TimeUnit.MILLISECONDS.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
	
	
	
	
	
	
	
	
	/**
	 * String array sort by tr value.
	 *
	 * @param tabStr the tab str
	 * @return the string[]
	 */
	public static String[] stringArraySortByTrValue(String[]tabStr) {
		String[]tabRet=new String[tabStr.length];
		for(int i=0;i<tabRet.length;i++)tabRet[i]=tabStr[i];
		for(int i=0;i<tabRet.length;i++)for(int j=0;j<tabRet.length-1;j++) {
			if(Integer.parseInt(tabRet[j].replace("TR", "")) > Integer.parseInt(tabRet[j+1].replace("TR", ""))) {
				String s=tabRet[j];tabRet[j]=tabRet[j+1];tabRet[j+1]=s;

			}
		}
		return tabRet;
	}

	
	
	
	/**
	 * String array sort.
	 *
	 * @param tabStr the tab str
	 * @return the string[]
	 */
	public static String[] stringArraySort(String[]tabStr) {
		String[]tabRet=new String[tabStr.length];
		ArrayList<String> listStr=new ArrayList<String>();
		for(String str : tabStr)listStr.add(str);
		Collections.sort(listStr);
		for(int i=0;i<listStr.size();i++)tabRet[i]=listStr.get(i);
		return tabRet;
	}

	
	/**
	 * Double array sort.
	 *
	 * @param tab the tab
	 * @return the double[]
	 */
	public static double[] doubleArraySort(double[]tab) {
		double[]tabRet=new double[tab.length];
		ArrayList<Double> list=new ArrayList<Double>();
		for(double dou : tab)list.add(dou);
		Collections.sort(list);
		for(int i=0;i<list.size();i++)tabRet[i]=list.get(i);
		return tabRet;
	}


	/**
	 * Hypot.
	 *
	 * @param x the x
	 * @param y the y
	 * @return the float
	 */
	public static float hypot(float x, float y) {
		return (float) Math.hypot(x, y);
	}
 
	/**
	 * Gaussian.
	 *
	 * @param x the x
	 * @param sigma the sigma
	 * @return the float
	 */
	public static float gaussian(float x, float sigma) {
		return (float) Math.exp(-(x * x) / (2f * sigma * sigma));
	}
	
	/**
	 * Image copy.
	 *
	 * @param imgRef the img ref
	 * @return the image plus
	 */
	//TODO : weird surnumerous function that have to be suppressed : Duplicator make all the job
	public static ImagePlus imageCopy(ImagePlus imgRef) {
		ImagePlus ret=new Duplicator().run(imgRef);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		for(int z=0;z<imgRef.getStackSize();z++)ret.getStack().setSliceLabel(imgRef.getStack().getSliceLabel(z+1), z+1);
		return ret;
	}
	
	/**
	 * Image copy with slice label.
	 *
	 * @param imgRef the img ref
	 * @param title the title
	 * @return the image plus
	 */
	public static ImagePlus imageCopyWithSliceLabel(ImagePlus imgRef,String title) {
		ImagePlus ret=new Duplicator().run(imgRef);
		VitimageUtils.adjustImageCalibration(ret, imgRef);
		for(int z=0;z<imgRef.getStackSize();z++) {
			ret.getStack().setSliceLabel(title+"_z="+(z+1), z+1);
		}
		ret.setTitle(title);
		return ret;
	}

	
	
	/**
	 * Image tab copy.
	 *
	 * @param imgTabRef the img tab ref
	 * @return the image plus[]
	 */
	public static ImagePlus[] imageTabCopy(ImagePlus []imgTabRef) {
		ImagePlus []imgRetTab=new ImagePlus[imgTabRef.length];
		for(int i=0;i<imgRetTab.length;i++)imgRetTab[i]=imageCopy(imgTabRef[i]);
		return imgRetTab;
	}
	

	
	/**
	 * Gets the date from string.
	 *
	 * @param datStr the dat str
	 * @return the date from string
	 */
	public static Date getDateFromString(String datStr) {
		Date date=null;
		try {
			date = new SimpleDateFormat("yyyyMMdd").parse(datStr);
		} catch (ParseException e) {
			return new Date(0);
		}  
		return date;
	}

	/**
	 * Adds the label on all slices.
	 *
	 * @param img the img
	 * @param label the label
	 */
	public static void addLabelOnAllSlices(ImagePlus img,String label) {
		for(int z=0;z<img.getNSlices();z++)for(int c=0;c<img.getNChannels();c++)for(int f=0;f<img.getNFrames();f++) {
			String oldLab=img.getStack().getSliceLabel(getCorrespondingSliceInHyperImage(img, c, z, f));
			String newLab=""+label+""+(oldLab==null ? "" : oldLab);
			img.getStack().setSliceLabel(newLab,getCorrespondingSliceInHyperImage(img, c, z, f));
		}
	}
	
	
	/**
	 * Gets the channel of max T 1 min T 2 sequence.
	 *
	 * @param hyperImg the hyper img
	 * @return the channel of max T 1 min T 2 sequence
	 */
	public static int getChannelOfMaxT1MinT2Sequence(ImagePlus hyperImg) {
		int cMax=hyperImg.getNChannels();
		String[]tabStr=new String[cMax];
		for(int c=0;c<cMax;c++)tabStr[c]=hyperImg.getStack().getSliceLabel(getCorrespondingSliceInHyperImage(hyperImg, c, 0, 0));
		int indexC=0;
		for(int c=0;c<cMax;c++) {
			if(tabStr[c]==null || tabStr[c].length()==0)continue;
			String[] tabS=tabStr[c].split("_");
			int valT1=0;
			int valT2=0;
			int maxT1=0;
			int maxT2=1000000;
			for(int s=0;s<tabS.length;s++) {
				if(tabS[s].contains("TE="))valT2=(int)Double.parseDouble(tabS[s].replace("TE=",""));
				if(tabS[s].contains("TR="))valT1=(int)Double.parseDouble(tabS[s].replace("TR=",""));
			}
			if(valT1<=0 || valT2<=0 || valT1 > 9999) {continue;}
			System.out.print("detected T1="+valT1+" T2="+valT2);
			if(valT1<=0 || valT2<=0 || valT1 > 9999) {System.out.println("Continue");continue;}
			if(valT1>maxT1) {
				indexC=c;maxT1=valT1;maxT2=valT2;
			}
			if(valT1==maxT1 && valT2<maxT2) {
				indexC=c;maxT1=valT1;maxT2=valT2;				
			}
		}		
		System.out.println("Val detected="+indexC);
		return indexC;
	}
	
	
	
	/**
	 * Gets the corresponding slice in hyper image.
	 *
	 * @param img the img
	 * @param c the c
	 * @param z the z
	 * @param f the f
	 * @return the corresponding slice in hyper image
	 */
	public static int getCorrespondingSliceInHyperImage(ImagePlus img,int c,int z,int f) {
		int nbC=img.getNChannels();
		int nbZ=img.getNSlices();
		return nbC*nbZ*f+nbC*z+c+1;		
	}
	

	
	
	
	/**
	 * Point 3d /xyz utilities.
	 *
	 * @param p the p
	 * @param voxs the voxs
	 * @return the point 3 d
	 */
	public static Point3d toRealSpace(Point3d p,double[]voxs) {
		return new Point3d(p.x*voxs[0],p.y*voxs[1],p.z*voxs[2]);
	}

	/**
	 * To image space.
	 *
	 * @param p the p
	 * @param voxs the voxs
	 * @return the point 3 d
	 */
	public static Point3d toImageSpace(Point3d p,double[]voxs) {
		return new Point3d(p.x/voxs[0],p.y/voxs[1],p.z/voxs[2]);
	}

	/**
	 * Convert double array to point 3 d array.
	 *
	 * @param tab the tab
	 * @return the point 3 d[]
	 */
	public static Point3d[]convertDoubleArrayToPoint3dArray(double[][]tab){
		Point3d[]tabPt=new Point3d[tab.length];
		for(int i=0;i<tab.length;i++)tabPt[i]=new Point3d(tab[i][0],tab[i][1],tab[i][2]);
		return tabPt;		
	}

	/**
	 * Convert point 3 d array to double array.
	 *
	 * @param tab the tab
	 * @return the double[][]
	 */
	public static double[][]convertPoint3dArrayToDoubleArray(Point3d[]tab){
		double[][]tabPt=new double[tab.length][3];
		for(int i=0;i<tab.length;i++){
			tabPt[i][0]=tab[i].x;
			tabPt[i][1]=tab[i].y;
			tabPt[i][2]=tab[i].z;
		}
		return tabPt;		
	}
		
	/**
	 * Dou.
	 *
	 * @param p the p
	 * @return the point 3 d
	 */
	public static Point3d dou(Point3d p) {
		return new Point3d(VitimageUtils.dou(p.x),VitimageUtils.dou(p.y),VitimageUtils.dou(p.z));
	}
	
	/**
	 * Gets the min max points.
	 *
	 * @param tab the tab
	 * @return the min max points
	 */
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
	

	
	
	
	
	/**
	 * Debugging utilities.
	 *
	 * @param img the img
	 * @return the string
	 */
	public static String imageResume(ImagePlus img) {
		if(img==null)return "image est nulle";
		int[]dims=VitimageUtils.getDimensions(img);
		double[]voxs=VitimageUtils.getVoxelSizes(img);
		String s="Image "+img.getTitle()+" coded on "+img.getBitDepth()+" per pixel. Dims="+dims[0]+" X "+dims[1]+" X "+dims[2]+"  VoxS="+voxs[0]+" x "+voxs[1]+" x "+voxs[2]+"  NChannels="+img.getNChannels()+"  NFrames="+img.getNFrames();
		return s;
	}

	/**
	 * Prints the image resume.
	 *
	 * @param img the img
	 */
	public static void printImageResume(ImagePlus img) {
		System.out.println(imageResume(img));
	}

	/**
	 * Prints the image resume.
	 *
	 * @param img the img
	 * @param str the str
	 */
	public static void printImageResume(ImagePlus img,String str) {
		System.out.println(str+" : "+imageResume(img));
	}


	/**
	 * Checks if is from bionano.
	 *
	 * @param img the img
	 * @return true, if is from bionano
	 */
	public static boolean isFromBionano(ImagePlus img) {
		if(img.getStack().getSliceLabel(1)==null)return false;
		return(img.getStack().getSliceLabel(1).contains("BIONANONMRI"));
	}
	
	/**
	 * Checks if is bionano image with capillary.
	 *
	 * @param img the img
	 * @return true, if is bionano image with capillary
	 */
	public static boolean isBionanoImageWithCapillary(ImagePlus img) {
		if(isSorghoHyperImage(img))return true;
		if(isVitimageHyperImage(img))return true;
		return true;
	}
	
	/**
	 * Checks if is ge 3 d.
	 *
	 * @param img the img
	 * @return true, if is ge 3 d
	 */
	public static boolean isGe3d(ImagePlus img) {
		if(img==null) {
			IJ.showMessage("Warning : trying to identify Ge3D applied on null image. Please verify your data");
			return false;
		}
		if(img.getProperty("Info") == null)return false;
		String info=(String)img.getProperty("Info");
		if(info==null)return false;
		if(info.contains("GE3D_BIONANO"))return true;
		return false;
	}
	
	
	/**
	 * Mask without capillary in ge 3 D image.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus maskWithoutCapillaryInGe3DImage(ImagePlus img) {
		//ImagePlus ret=getAntiCapillaryMask(img);
		return null; //TODO
	}

	/**
	 * Checks if is sorgho hyper image.
	 *
	 * @param img the img
	 * @return true, if is sorgho hyper image
	 */
	public static boolean isSorghoHyperImage(ImagePlus img) {
		System.out.println("Sorgho detection starting step 1");
		if(img.getStack().getSliceLabel(1)==null)return false;
		System.out.println("Sorgho detection starting step 2");
		if(! img.getStack().getSliceLabel(1).contains("M0MAP"))return false;
		System.out.println("Sorgho detection starting step 3");
//		if(  (Math.abs(VitimageUtils.getVoxelVolume(img)-0.001)/VitimageUtils.getVoxelVolume(img))>EPSILON)return false; Temporary set in comment
		System.out.println("Sorgho detection starting step 4");
		return true;
	}

	
	/**
	 * Checks if is vitimage hyper image.
	 *
	 * @param img the img
	 * @return true, if is vitimage hyper image
	 */
	public static boolean isVitimageHyperImage(ImagePlus img) {
		System.out.println(VitimageUtils.imageResume(img));
		if(img.getStack().getSliceLabel(1)==null)return false;
		if(img.getStack().getSliceLabel(1).length()==0)return false;
		if(! img.getStack().getSliceLabel(1).contains("M0MAP"))return false;
		if(img.getNSlices()!=40)return false;
		IJ.log("Detected experience : Vitimage\n");
		return true;
	}

	/**
	 * Detect axis irm T 1.
	 *
	 * @param img the img
	 * @param delayForReacting the delay for reacting
	 * @return the point 3 d[]
	 */
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
		convertToGray8(img);
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



}
