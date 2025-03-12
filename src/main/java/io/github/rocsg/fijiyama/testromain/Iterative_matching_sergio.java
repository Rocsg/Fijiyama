package io.github.rocsg.fijiyama.testromain;

import com.jogamp.nativewindow.util.Point;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import math3d.Point3d;

public class Iterative_matching_sergio { 
    static double ratioFactorForSigmaComputation=25;
       
    public static void main(String[] args) {
        ImageJ ij=new ImageJ();
        String dirImgRef="/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/Grayscale/2024_2_27_Andrano.tif";
        String dirImgMov="/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/Grayscale/2024_3_28_Andrano.tif";
        ImagePlus imgRef=IJ.openImage(dirImgRef);
        ImagePlus imgMov=IJ.openImage(dirImgMov);

        ItkTransform tr=new ItkTransform();
        ImagePlus imgMovReg=tr.transformImage(imgRef, imgMov);
        ImagePlus imgFused=VitimageUtils.compositeNoAdjustOf(imgRef, imgMovReg);
        imgFused.show();
        Point3d[][]tab=null;
        while(true){      
            Point3d[][]pts=registrationPointsSingleImageUI(4, imgFused,true);
            tab=addPointTabToPointTab(tab,pts);
            if(true){
                tr=computeTrFromPoints(imgRef, imgMov,tab);
                imgMovReg=tr.transformImage(imgRef, imgMov);
                imgFused.close();
                imgFused=VitimageUtils.compositeNoAdjustOf(imgRef, imgMovReg);
                imgFused.show();
            }

            //if(pts[0][0].x<200)updateNeeded=true;

        }
        //updateView();


    }


    public static Point3d[][]addPointTabToPointTab(Point3d[][]tab,Point3d[][]pts){
        if(tab==null){
            tab=pts;
        }else{
            int N=tab[0].length;
            Point3d[][]tabNew=new Point3d[2][N+pts[0].length];
            for(int i=0;i<N;i++){
                tabNew[0][i]=new Point3d(tab[0][i].x,tab[0][i].y,tab[0][i].z);
                tabNew[1][i]=new Point3d(tab[1][i].x,tab[1][i].y,tab[1][i].z);
            }
            for(int i=0;i<pts[0].length;i++){
                tabNew[0][N+i]=new Point3d(pts[0][i].x,pts[0][i].y,pts[0][i].z);
                tabNew[1][N+i]=new Point3d(pts[1][i].x,pts[1][i].y,pts[1][i].z);
            }
            tab=tabNew;
        }
        return tab;
    }

    public static Point3d[][] copyPointTab(Point3d[][]tab){
        int N=tab[0].length;
        Point3d[][]tabNew=new Point3d[2][N];
        for(int i=0;i<N;i++){
            tabNew[0][i]=new Point3d(tab[0][i].x,tab[0][i].y,tab[0][i].z);
            tabNew[1][i]=new Point3d(tab[1][i].x,tab[1][i].y,tab[1][i].z);
        }
        return tabNew;
    }

    public static ItkTransform computeTrFromPoints(ImagePlus imgRef, ImagePlus imgMov,Point3d[][]tabTemp){
        Point3d[][]tab=copyPointTab(tabTemp);
        ItkTransform trAffGlob=ItkTransform.estimateBestAffine3D(tab[1],tab[0]);
        ImagePlus imgTransAffine=trAffGlob.transformImage(imgRef, imgMov);
        
        //Compute the remaining dense field
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]/ratioFactorForSigmaComputation;
        int Ncorr=tab[0].length;
        for(int i=0;i<Ncorr;i++){
            tab[0][i]=trAffGlob.transformPoint(tab[0][i]);
        }
        System.out.println("After reg affine, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));

        //Combine into a single transformation
        ItkTransform trDenseGlob=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(tab, imgRef, sigma);
        ItkTransform trGlob=new ItkTransform();
        trGlob.addTransform(trAffGlob);
        trGlob.addTransform(trDenseGlob);
        ImagePlus imgTransDense=trGlob.transformImage(imgRef, imgMov);
        tab=copyPointTab(tabTemp);
        for(int i=0;i<Ncorr;i++){
            tab[0][i]=trGlob.transformPoint(tab[0][i]);
        }
        System.out.println("After reg dense, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));

        return trGlob;}











	public static Point3d[][] registrationPointsSingleImageUI(int nbWantedPointsPerImage,ImagePlus imgRef,boolean realCoordinates){
		System.out.println("Toto321");
		boolean isUserDefinedEnd=(nbWantedPointsPerImage==-1);
		if(isUserDefinedEnd)nbWantedPointsPerImage=1000000000;
		int squareSize=0;
		ImagePlus imgRefBis=imgRef.duplicate();
		System.out.println("Toto322");

		imgRefBis.getProcessor().resetMinAndMax();
		if(isUserDefinedEnd){
			squareSize=imgRef.getWidth()/50;
			imgRefBis=VitimageUtils.drawRectangleInImage(imgRefBis, 0,0, squareSize, squareSize, 0);
		}
		System.out.println("Toto323");

		imgRefBis.show();
		imgRefBis.setTitle("Reference image");
		System.out.println("Toto324");
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		IJ.showMessage("Examine images and click on "+(nbWantedPointsPerImage*2)+" points to compute the correspondances,\n on both reference image and moving image (see image titles)"+
		"For each selected point, use the Roi Manager to save it, with \"add to manager\" option.\n Please follow the following order : "+
		"\n   Point 1 : item A on reference image\n    Point 2 : item A on moving image\n    Point 3 : item B on reference image\n Point 4 : item B on moving image \n...");
		boolean finished =false;
		do {
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean finishCausedByUser=isUserDefinedEnd && rm.getCount()>0 && (rm.getRoi(rm.getCount()-1 ).getXBase()<squareSize)&& (rm.getRoi(rm.getCount()-1 ).getYBase()<squareSize);
			if(( rm.getCount()>=nbWantedPointsPerImage*2 || finishCausedByUser) && VitiDialogs.getYesNoUI("","Confirm points ?"))finished=true;
			System.out.println("Waiting "+(nbWantedPointsPerImage*2)+". Current number="+rm.getCount());
		}while (!finished);	
		int nCouples=Math.max((isUserDefinedEnd ? 0 : nbWantedPointsPerImage), rm.getCount()/2);
		Point3d []pRef=new Point3d[nCouples];
		Point3d []pMov=new Point3d[nCouples];
		for(int indP=0;indP<nCouples;indP++){
			pRef[indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			pMov[indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			if(realCoordinates) {
				pRef[indP]=TransformUtils.convertPointToRealSpace(pRef[indP],imgRef);
				pMov[indP]=TransformUtils.convertPointToRealSpace(pMov[indP],imgRef);				
			}	
		}
		imgRefBis.close();
		return new Point3d[][] {pRef,pMov};
	}
	
	







    }



