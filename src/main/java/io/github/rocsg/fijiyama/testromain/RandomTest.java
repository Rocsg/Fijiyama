package io.github.rocsg.fijiyama.testromain;

import org.apache.commons.math3.analysis.interpolation.BicubicInterpolatingFunction;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationAction;
import io.github.rocsg.fijiyama.registration.BlockMatchingRegistration;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.MetricType;
import io.github.rocsg.fijiyama.registration.Transform3DType;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import math3d.Point3d;

public class RandomTest {
    public static void main(String[] args) {
        ItkTransform trInit=ItkTransform.array16ElementsToItkTransform(new double[] {1,0,0,36.45090,  0,1,0,-181.5,  0,0,1,0,  0,0,0,1});
        ImageJ ij=new ImageJ();
        String dir="/home/rfernandez/Bureau/A_Test/TroublesLoai/HardRegistration/Raw";
/*
        ImagePlus imgRef=IJ.openImage("/home/rfernandez/Bureau/Test_Sergio/Data_3/Test/ref.tif");
        ImagePlus imgMov=IJ.openImage("/home/rfernandez/Bureau/Test_Sergio/Data_3/Test/mov.tif");
*/ 
        int iRef=1;
        int iMov=3;
        ImagePlus imgRef=IJ.openImage(dir+"/slice"+iRef+"-sub8_ng.tif");
        ImagePlus imgMov=IJ.openImage(dir+"/slice"+iMov+"-sub8_ng.tif");

        System.out.println(imgRef);
        System.out.println(imgMov);
        System.out.println("Toto1");
        RegistrationAction regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov);
        regAct.typeAction=RegistrationAction.TYPEACTION_AUTO;
        regAct.typeTrans=Transform3DType.RIGID;
//        regAct.typeTrans=Transform3DType.DENSE;
        regAct.bhsX=3;
        regAct.bhsY=3;
        regAct.selectLTS=50;
        regAct.selectScore=50;
        regAct.iterationsBMLin=10;        
        regAct.iterationsBMDen=10;        
        regAct.neighX=2;
        regAct.neighY=2;
        regAct.strideX=1;
        regAct.strideY=1;
        regAct.levelMaxLinear=6;
        regAct.levelMinLinear=1;
        regAct.levelMaxDense=3;
        regAct.levelMinDense=1;

        System.out.println(regAct);
        BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov, regAct);
        //bm.minBlockVariance=0.07;
        //bm.minBlockScore=0.1;
        System.out.println("Toto3");

        bm.displayRegistration=2;
        //bm.metricType=MetricType.CORRELATION;
        System.out.println(bm.levelMax);
        System.out.println(bm.levelMin);
        //        bm.setSingleView();
 //       bm.levelMax--;
 //       bm.metricType=MetricType.SQUARED_CORRELATION;
 //       bm.neighbourhoodSizeX=3;
 //       bm.neighbourhoodSizeY=3;
 //       bm.mask=imgMask;
 //       bm.minBlockVariance/=2;
 //       bm.minBlockScore=0.03;
 //       bm.denseFieldSigma*=2;
          ItkTransform trStep1=bm.runBlockMatching(null, false);
//        imgRef.show();
 //       imgMov.show();
        bm.closeLastImages();

         imgRef=IJ.openImage(dir+"/slice"+iRef+".tif");
        imgMov=IJ.openImage(dir+"/slice"+iMov+".tif");

        regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov);
        regAct.typeAction=RegistrationAction.TYPEACTION_AUTO;
        regAct.typeTrans=Transform3DType.DENSE;
        regAct.bhsX=3;
        regAct.bhsY=3;
//        regAct.selectLTS=50;
//        regAct.selectScore=50;
        regAct.iterationsBMLin=10;        
        regAct.neighX=2;
        regAct.neighY=2;
        regAct.strideX=1;
        regAct.strideY=1;
        regAct.levelMaxLinear=3;
        regAct.levelMinLinear=1;

        bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov, regAct);
        bm.displayRegistration=2;
        //bm.minBlockVariance=0.07;
        //bm.minBlockScore=0.1;
        System.out.println("Toto3");
        ItkTransform trStep2=bm.runBlockMatching(trStep1, false);
 
}


    

}
