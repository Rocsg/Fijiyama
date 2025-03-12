package io.github.rocsg.fijiyama.testromain;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.frame.RoiManager;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationAction;
import io.github.rocsg.fijiyama.registration.BlockMatchingRegistration;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.MetricType;
import io.github.rocsg.fijiyama.registration.Transform3DType;
import math3d.Point3d;

public class SerieSergio_data3 {
    static String mainDir="/home/rfernandez/Bureau/Test_Sergio/Data_3/";
    static String twoImgDir=mainDir+"Tests_2img/";
    static String serieDir=mainDir+"Whole_serie/";
    static String[]names=new String[]{
        "2024_2_27_Andrano.tif",
        "2024_3_12_Andrano.tif",
        "2024_3_21_Andrano.tif",
        "2024_3_28_Andrano.tif",
        "2024_4_24_Andrano.tif"        
    };
    public static void main(String[] args) {
        ImageJ ij=new ImageJ();
        testBm();
    }


    public static void testBm(){
        ImagePlus imgRef=IJ.openImage("/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_sur_ndvi/2024_2_27_Andrano.tif");
        ImagePlus imgMov=IJ.openImage("/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_sur_ndvi/2024_3_12_Andrano.tif");
        ImagePlus imgMask=IJ.openImage("/home/rfernandez/Bureau/Test_Sergio/Data_3/Whole_serie/Masks/2024_2_27_Andrano_mask.tif");
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]*0.05;

        RegistrationAction regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov);
        regAct.typeAction=RegistrationAction.TYPEACTION_AUTO;
        regAct.typeTrans=Transform3DType.DENSE;
        regAct.typeAutoDisplay=2;
        regAct.levelMaxDense=4;
        regAct.levelMinDense=1;
        regAct.strideX=2;
        regAct.strideY=2;
        regAct.bhsX=3;
        regAct.bhsY=3;
        regAct.sigmaDense=sigma;
        regAct.selectScore=70;
        System.out.println(regAct);
        VitimageUtils.printImageResume(imgMov);
        BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov, regAct);
        System.out.println("Toto3");

        
        bm.setSingleView();
        bm.metricType=MetricType.SQUARED_CORRELATION;
        bm.mask=imgMask;
        bm.minBlockVariance/=2;
        bm.minBlockScore=0.1;

        ItkTransform tr=bm.runBlockMatching(null, false);
   
        
        System.out.println("Toto33");
        ImagePlus img=tr.viewAsGrid3D(imgRef, 100);
        System.out.println("Toto4");
        img.show();
        imgMov.setTitle("imgMov");
        imgRef.setTitle("imgRef");
        imgRef.show();
        imgRef.duplicate().show();
        imgMov.show();
        ImagePlus imgMov2=tr.transformImage(imgRef, imgMov);
        imgMov2.setTitle("imgMov2");
        imgMov2.show();
//        tr.writeAsDenseField("/home/rfernandez/Bureau/Test_Sergio/Data_3/Test_2_img_rec/Data/Field.tif", imgRef);

        ImagePlus before=VitimageUtils.compositeNoAdjustOf(imgRef, imgMov);
        ImagePlus after=VitimageUtils.compositeNoAdjustOf(imgRef, imgMov2);
        before.setTitle("before");
        after.setTitle("after");
        before.show();
        after.show();
 
    }






    public static void generateNdvi(){
        String dirToRawSmall="/home/rfernandez/Bureau/Test_Sergio/Data_3/Whole_serie/Small/";
        String dirToRawGrayscale="/home/rfernandez/Bureau/Test_Sergio/Data_3/Whole_serie/SmallGrayscale/";
        String dirToRawSmallNdvi="/home/rfernandez/Bureau/Test_Sergio/Data_3/Whole_serie/SmallNdvi/";

        for(String s:names){
            System.out.println(s);
            ImagePlus imgRaw=IJ.openImage(dirToRawSmall+s);
            ImagePlus imgGrayscale=IJ.openImage(dirToRawGrayscale+s);
            VitimageUtils.printImageResume(imgRaw);
            ImagePlus imgIR=new Duplicator().run(imgRaw,1,1,5,5,1,1);
            ImagePlus imgR=new Duplicator().run(imgRaw,1,1,3,3,1,1);  

            /* 
            imgR.show();
            imgIR.show();
            imgRaw.show();
            VitimageUtils.waitFor(50000);
            */
            imgIR=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgIR);
            imgR=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgR);
            ImagePlus imgSub=VitimageUtils.makeOperationBetweenTwoImages(imgIR, imgR, 4, true);
            /*imgSub.show();
            VitimageUtils.waitFor(50000);*/
            ImagePlus imgAdd=VitimageUtils.makeOperationBetweenTwoImages(imgIR, imgR, 1, true);
            ImagePlus imgNdvi=VitimageUtils.makeOperationBetweenTwoImages(imgSub, imgAdd, 3, true);
            VitimageUtils.adjustImageCalibration(imgNdvi, imgGrayscale);
            IJ.saveAsTiff(imgNdvi, dirToRawSmallNdvi+s);
        }

    }


    public static void generatePairs1to2(){
        ItkTransform tr1to2Rig=ItkTransform.readTransformFromFile( serieDir+"Processing_Fijiyama/Registration_files/Transform_Step_0.txt");
        ItkTransform tr1to2Aff=ItkTransform.readTransformFromFile( serieDir+"Processing_Fijiyama/Registration_files/Transform_Step_1.txt");
        tr1to2Rig.addTransform(tr1to2Aff);
        ItkTransform trInv=new ItkTransform(tr1to2Rig.getInverse());
        //Image used as reference
        ImagePlus imgRef=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_2_img_rec/Data/Ref.tif");
        ImagePlus imgMov=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_2_img_rec/Data/Ref.tif");
        //Correspondences after tr1to2
        String path="/home/rfernandez/Bureau/Test_Sergio/Data_3/RoiSet.zip";
        Point3d[][]tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(path,imgRef,imgMov,true);
        int Ncorr=tab[0].length;
        
        //Go back correspondence from Mov
        for(int n=0;n<Ncorr;n++){
            tab[1][n]=trInv.transformPoint(tab[1][n]);
        }
        ItkTransform trAffGlob=ItkTransform.estimateBestAffine3D(tab[0],tab[1]);
        
        
        //Compute the remaining dense field
        for(int n=0;n<Ncorr;n++){
            tab[1][n]=trAffGlob.transformPoint(tab[1][n]);
        }
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]*0.1;
        ItkTransform trDenseGlob=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(tab, imgRef, sigma);
        ItkTransform trGlob=new ItkTransform();
        trGlob.addTransform(trAffGlob);
        trGlob.addTransform(trDenseGlob);

        //Apply all the transformation to original image
        ImagePlus imgRefInit=IJ.openImage(serieDir+"SmallGrayscale/2024_2_27_Andrano.tif");
        ImagePlus imgMovInit=IJ.openImage(serieDir+"SmallGrayscale/2024_3_12_Andrano.tif");
        ImagePlus imgTransformed=trGlob.transformImage(imgRefInit, imgMovInit);


        imgRefInit.setTitle("imgRefInit");
        imgMovInit.setTitle("imgMovInit");
        imgTransformed.setTitle("imgTransformed");
        imgRefInit.show();
        imgMovInit.show();
        imgTransformed.show();
        VitimageUtils.compositeNoAdjustOf(imgRefInit, imgTransformed).show();
        
        ImagePlus imgNdviInit=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Whole_serie/SmallNdvi/"+names[1]);

        ImagePlus ndviTransformed=trGlob.transformImage(imgRefInit, imgNdviInit);
        IJ.saveAsTiff(ndviTransformed, "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_sur_ndvi/2024_3_12_Andrano.tif");


    }







    /**************Tests *********************************************************************************************/
     public static void testToCsv(){
        String pathToRoi="/home/rfernandez/Bureau/Test_Sergio/Data_3/RoiSet.zip";
        String pathToCsv="/home/rfernandez/Bureau/Test_Sergio/Data_3/RoiSet.csv";
        VitimageUtils.writePointRoiToCsv(pathToRoi,pathToCsv);

        pathToRoi="/home/rfernandez/Bureau/Test_Sergio/Data_3/OtherRoiSet.zip";
        pathToCsv="/home/rfernandez/Bureau/Test_Sergio/Data_3/OtherRoiSet.csv";
        VitimageUtils.writePointRoiToCsv(pathToRoi,pathToCsv);

    }


    public static void testReadThenWriteRoi(){
        String path="/home/rfernandez/Bureau/Test_Sergio/Data_3/RoiSet.zip";
        System.out.println("Toto1");
        ImagePlus imgRef=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_2_img_rec/Data/Ref.tif");
        System.out.println("Toto2");
        Point3d[]tab=VitimageUtils.getCoordinatesFromRoiSet(path,imgRef,true);
        System.out.println("Toto3 N="+tab.length);
        PointRoi []pr=VitimageUtils.getPointRoiTabMonoSliceFromPoint3dTab(tab,imgRef,true);
        System.out.println("Toto4");
        VitimageUtils.saveRoiAs(pr,"/home/rfernandez/Bureau/Test_Sergio/Data_3/OtherRoiSet.zip");
        System.out.println("Toto5");
    }   






    public static void testReadRois(){
        String path="/home/rfernandez/Bureau/Test_Sergio/Data_3/RoiSet.zip";
        ImagePlus imgRef=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test/ref.tif");
        ImagePlus imgMov=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test/mov.tif");
        Point3d[][]tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(path,imgRef,imgMov,true);
        for(int i=0;i<tab[0].length;i++)System.out.println(i+" ("+tab[0][i]+") -> ("+tab[1][i]+")" );
    }

    public static void testRedoDenseRegistrationFromPoints(){
        String path="/home/rfernandez/Bureau/Test_Sergio/Data_3/RoiSet.zip";
        ImagePlus imgRef=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_2_img_rec/Data/Ref.tif");
        ImagePlus imgMov=IJ.openImage( "/home/rfernandez/Bureau/Test_Sergio/Data_3/Tests_2img/Test_2_img_rec/Data/Mov.tif");
        Point3d[][]tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(path,imgRef,imgMov,true);
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]*0.1;
        ItkTransform tr=ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(tab, imgRef, sigma);
        ImagePlus imgMovRecOnRef=tr.transformImage(imgRef, imgMov);
        ImagePlus imgBefore=VitimageUtils.compositeNoAdjustOf(imgRef, imgMov);
        imgBefore.setTitle("before");
        imgBefore.show();
        ImagePlus imgAfter=VitimageUtils.compositeNoAdjustOf(imgRef, imgMovRecOnRef);
        imgAfter.setTitle("after");
        imgAfter.show();        
    }

 

}
