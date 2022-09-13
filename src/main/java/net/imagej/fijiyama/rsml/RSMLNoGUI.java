/*
 * 
 */
package net.imagej.fijiyama.rsml;
/**
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge
 * 
 * RSML importer interface
 */

import ij.*;
import ij.measure.ResultsTable;
import net.imagej.fijiyama.registration.ItkTransform;
import net.imagej.fijiyama.rsml.FSR;
import net.imagej.fijiyama.rsml.RSMLNoGUI;
import net.imagej.fijiyama.rsml.RootModel;

import java.io.*;

// TODO: Auto-generated Javadoc
/**
 * The Class RSMLNoGUI.
 */
public class RSMLNoGUI{
   
   /** The img source. */
   private ImagePlus imgSource=null;
   
   /** The sr. */
   private FSR sr; 
   
   /**
    * Instantiates a new RSML no GUI.
    */
   public RSMLNoGUI() {    }

   
   /**
    * The main method.
    *
    * @param args the arguments
    */
   public static void main (String[]args) {
	   ImageJ ij=new ImageJ();
	   ImagePlus imgRef=IJ.openImage("/home/rfernandez/Bureau/test.tif");
	  // imgRef.show();
	   ItkTransform tr=ItkTransform.generateRandomDenseField(imgRef, 5,50, 0);
	   //tr.showAsGrid3D(imgRef, 10, "test", 1);
   	   String imgName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Train/20200826-AC-PIP_azote_Seq 6_Boite 00004_IdentificationFailed-Visu.jpg";
	   String rsmlMorganName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Train/20200826-AC-PIP_azote_Seq 6_Boite 00004_IdentificationFailed-Visu.rsml";
	   new RSMLNoGUI().getPreviewGreyScale(imgName,rsmlMorganName,true).show();
	   
   }
   
   /**
    * Gets the preview.
    *
    * @param imgPath the img path
    * @param rsmlPath the rsml path
    * @param lineWidth the line width
    * @param realWidth the real width
    * @param makeConvexHull the make convex hull
    * @param ratioColor the ratio color
    * @return the preview
    */
   public ImagePlus getPreview(String imgPath,String rsmlPath,int lineWidth,boolean realWidth,boolean makeConvexHull,double ratioColor){
	    (sr= new FSR()).initialize();
	    RootModel model = new RootModel(rsmlPath);

	    
	    ImagePlus imgSource=IJ.openImage(imgPath);
	    int w=imgSource.getWidth();
	    int h=imgSource.getHeight();
	   	ResultsTable rt = new ResultsTable();
		ImageStack is= new ImageStack(w, h);
		ImagePlus imgRSML=new ImagePlus(new File(rsmlPath).getName(),model.createImage(true, lineWidth, realWidth, w, h, makeConvexHull,ratioColor));  
		System.out.println("Output : "+imgRSML.getWidth());
		return imgRSML;
   }
   
   
   /**
    * Gets the preview grey scale.
    *
    * @param imgPath the img path
    * @param rsmlPath the rsml path
    * @param makeConvexHull the make convex hull
    * @return the preview grey scale
    */
   public ImagePlus getPreviewGreyScale(String imgPath,String rsmlPath,boolean makeConvexHull){
	    (sr= new FSR()).initialize();
	    RootModel model = new RootModel(rsmlPath);
	    ImagePlus imgSource=IJ.openImage(imgPath);
	    int w=imgSource.getWidth();
	    int h=imgSource.getHeight();
	    ImagePlus imgRSML= model.createGrayScaleImage(imgSource,1,false,false,0);
		return imgRSML;
  }
   	
   /**
    * Gets the transformed preview.
    *
    * @param imgPath the img path
    * @param rsmlPath the rsml path
    * @param lineWidth the line width
    * @param realWidth the real width
    * @param makeConvexHull the make convex hull
    * @param ratioColor the ratio color
    * @return the transformed preview
    */
   public ImagePlus getTransformedPreview(String imgPath,String rsmlPath,int lineWidth,boolean realWidth,boolean makeConvexHull,double ratioColor){
	    (sr= new FSR()).initialize();
	    RootModel model = new RootModel(rsmlPath);

	    ImagePlus imgRef=IJ.openImage(imgPath);
	    double sigma=10;
	    ItkTransform tr=ItkTransform.generateRandomDenseField(imgRef, 5,50, 10);
	    tr.showAsGrid3D(imgRef, 10, "test", 1);
//	    ItkTransform tr=ItkTransform.generateRandomGaussianTransform2D(sigma);
	    model.applyTransformToGeometry(tr);
	    
	    ImagePlus imgSource=IJ.openImage(imgPath);
	    int w=imgSource.getWidth();
	    int h=imgSource.getHeight();
	   	ResultsTable rt = new ResultsTable();
		ImageStack is= new ImageStack(w, h);
		ImagePlus imgRSML=new ImagePlus(new File(rsmlPath).getName(),model.createImage(true, lineWidth, realWidth, w, h, makeConvexHull,ratioColor));  
		System.out.println("Output : "+imgRSML.getWidth());
		return imgRSML;
   }
	

}

