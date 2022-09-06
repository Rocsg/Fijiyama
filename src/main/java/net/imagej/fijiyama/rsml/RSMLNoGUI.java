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

public class RSMLNoGUI{
   private ImagePlus imgSource=null;
   private FSR sr; 
   public RSMLNoGUI() {    }

   
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
   
   
   public ImagePlus getPreviewGreyScale(String imgPath,String rsmlPath,boolean makeConvexHull){
	    (sr= new FSR()).initialize();
	    RootModel model = new RootModel(rsmlPath);
	    ImagePlus imgSource=IJ.openImage(imgPath);
	    int w=imgSource.getWidth();
	    int h=imgSource.getHeight();
	    ImagePlus imgRSML= model.createGrayScaleImage(imgSource,1,false,false,0);
		return imgRSML;
  }
   	
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

