package io.github.rocsg.fijiyama.testromain;

import com.jogamp.nativewindow.util.Point;

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

public class SerieSergio_data4 {
    static double ratioFactorForSigmaComputation=25;
    static String mainDir="/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/";
    static String[]names=new String[]{
        /* "2024_2_12_Andrano.tif",*/
        "2024_2_27_Andrano.tif",
        "2024_3_12_Andrano.tif",
        "2024_3_21_Andrano.tif",
        "2024_3_28_Andrano.tif",
        "2024_4_24_Andrano.tif"        
    };
    static double subsamplingFactor=4;//Appears as well in python 

    static double[]voxSizes=new double[]{
        /*24_2_12 -> 1.08 cm/px*/
        1.0,
        1.04,
        1.04,
        0.97,
        1.0
    };

    public static void main(String[] args) {
        ImageJ ij=new ImageJ();
        //createCombinedTransforms();
        //resampleChannelsLowRes(1);
            
        resampleChannels(4);
        //openImages();
        //setVoxelSizes();
//        annotate(4);
//      repair();
        //registerWithLandmarks(4);
 /*        registerWithBlockMatching(1,0);
        registerWithBlockMatching(2,0);
        registerWithBlockMatching(3,0);
        registerWithBlockMatching(4,0);*/
//        evaluateMatchingBasedOnLandmarks(1);
        //generateImgFusBm();
        System.out.println("OK");
    }








    public static Point3d[][] repair3(Point3d[][]tab,int[]indexToForget){
        int N=tab[0].length;
        Point3d[][]tabNew=new Point3d[2][N-indexToForget.length+1];
        int ii=0;int j=0;int k=0;
        for(int i=0;i<N;i++){
            if(indexToForget[k]==j){
                System.out.println("Skipping j="+j);
                k++;
                j++;
                continue;
            }
            else{
                System.out.println("Keeping j="+j);
                tabNew[0][ii]=tab[0][j];
                tabNew[1][ii]=tab[1][j];
                j++;
                ii++;
            }
        }
        return tabNew;

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


    
    public static void repair(){
        String pathToRoi="/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/Roi/2024_3_21_Andrano.tif_new.zip";
        // 143 144 145 146  1= 0*2+1  2=0*2+2          143=71*2+1  144=71*2+2
        ImagePlus imgRef=IJ.openImage(mainDir+"Grayscale/"+names[0]);
        ImagePlus imgMov=IJ.openImage(mainDir+"Grayscale/"+names[0]);
        Point3d[][]tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(pathToRoi,imgRef,imgMov,false);
        Point3d[][]newTab=new Point3d[2][tab[0].length-2];
        int N0=tab[0].length;
        for(int i=0;i<71;i++){
            newTab[0][i]=tab[0][i];
            newTab[1][i]=tab[1][i];
        }
        for(int i=71;i<N0-2;i++){
            newTab[0][i]=tab[0][i+2];
            newTab[1][i]=tab[1][i+2];
        }
        String pathToRoiNew="/home/rfernandez/Bureau/Test_Sergio/Data_4/Roi/2024_3_21_Andrano.tif_new.zip";
        Roi[]tabRoi=VitimageUtils.getPointRoiTabMonoSliceFromPoint3dTab(VitimageUtils.point3dDoubleTabToSingleTab(newTab), imgRef, false);
        VitimageUtils.saveRoiAs(tabRoi, pathToRoiNew);
        System.out.println("Repair ok.");
    }


    //STEP 6
    public static void evaluateMatchingBasedOnLandmarks(int n){
        String name=names[n];
        String pathToRoi=mainDir+"Roi/"+name+".zip";
        ImagePlus imgRef=IJ.openImage(mainDir+"Grayscale/"+names[0]);
        ImagePlus imgMov=IJ.openImage(mainDir+"Grayscale/"+names[n]);
        Point3d[][]tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(pathToRoi,imgRef,imgMov,true);

        if(n==3){
            Point3d[][]tab2=new Point3d[2][tab[0].length-2];
            for(int i=0;i<tab[0].length-2;i++){
                tab2[0][i]=tab[0][i];
                tab2[1][i]=tab[1][i];
            }
            tab=tab2;
        }

        ItkTransform trAff=ItkTransform.readTransformFromFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[n])+"_affine.txt");
        ItkTransform trDense=ItkTransform.readTransformFromFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[n])+"_dense.transform.tif");
        ItkTransform trBm=ItkTransform.readTransformFromFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[n])+"_afterbm.transform.tif");

        //Evaluating original position
        System.out.println("Before registration, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));

        //Evaluating after affine
        int Ncorr=tab[0].length;
        for(int i=0;i<Ncorr;i++){
            tab[0][i]=trAff.transformPoint(tab[0][i]);
        }
        System.out.println("After reg affine, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));

        //Evaluating after dense
        tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(pathToRoi,imgRef,imgMov,true);
        for(int i=0;i<Ncorr;i++){
            tab[0][i]=trDense.transformPoint(tab[0][i]);
        }
        System.out.println("After reg dense, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));

        //Evaluating after Blockmatching
        tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(pathToRoi,imgRef,imgMov,true);
        for(int i=0;i<Ncorr;i++){
            tab[0][i]=trBm.transformPoint(tab[0][i]);
        }
        System.out.println("After BlockMatching, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));    
        System.out.println("Eval ok.");
    }


    public static ImagePlus computeMask(ImagePlus img1,ImagePlus img2){
        img1.show();
        img2.show();
        //Create a binary mask of area that have value 171 in 8-bit
        ImagePlus img1_32=VitimageUtils.imageCopy(img1);
        ImagePlus img2_32=VitimageUtils.imageCopy(img2);
        img1_32=VitimageUtils.convertByteToFloatWithoutDynamicChanges(img1_32);
        img2_32=VitimageUtils.convertByteToFloatWithoutDynamicChanges(img2_32);
        img1_32=VitimageUtils.thresholdImageToFloatMask(img1_32, 171, 171);
        img2_32=VitimageUtils.thresholdImageToFloatMask(img2_32, 171, 171);
        ImagePlus imgMask=VitimageUtils.makeOperationBetweenTwoImages(img1_32, img2_32, 1, true);
        imgMask=VitimageUtils.thresholdImageToFloatMask(imgMask, -1, 0.9);
        imgMask=VitimageUtils.gaussianFiltering(imgMask, 100,100,0);
        imgMask=VitimageUtils.thresholdImage(imgMask, 0.9, 10);
        return imgMask;

    }

    public static ImagePlus computeMaskFiveImages(ImagePlus[]imgTab){
        //Create a binary mask of area that have value 171 in 8-bit
        ImagePlus []imgTabMask=new ImagePlus[5];
        for(int i=0;i<5;i++){
            imgTabMask[i]=VitimageUtils.imageCopy(imgTab[i]);
            imgTabMask[i]=VitimageUtils.convertByteToFloatWithoutDynamicChanges(imgTabMask[i]);
            imgTabMask[i]=VitimageUtils.thresholdImageToFloatMask(imgTabMask[i], 171, 171);            
        }
        ImagePlus imgTemp=imgTabMask[0].duplicate();
        for(int i=1;i<5;i++){
            imgTemp=VitimageUtils.makeOperationBetweenTwoImages(imgTemp, imgTabMask[i], 1, true);            
        }
        imgTemp=VitimageUtils.thresholdImageToFloatMask(imgTemp, -1, 0.9);
        imgTemp=VitimageUtils.gaussianFiltering(imgTemp, 100,100,0);
        imgTemp=VitimageUtils.thresholdImage(imgTemp, 0.9, 10);


        return imgTemp;

    }


    public static void resampleChannelsLowRes(int n){
        System.out.println("Starting resampl "+n);
        String name0=VitimageUtils.withoutExtension(names[0]);
        String name=VitimageUtils.withoutExtension(names[n]);
        ImagePlus imgRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/Grayscale/"+name0+".tif");
        double[]voxSizeRefLow=VitimageUtils.getVoxelSizes(imgRef);

        ImagePlus imgRefHR=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/RawHighRes/channel_split/"+name0+"_channel_0.tif");
        System.out.println("Here");
        ItkTransform tr=ItkTransform.readTransformFromFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[n])+"_combined.transform.tif");
//        ItkTransform tr=new ItkTransform();
        System.out.println("THere");
        ImagePlus imgMov=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/Grayscale/"+name+".tif");
        double[]voxSizeMovLow=VitimageUtils.getVoxelSizes(imgMov);

        ImagePlus imgMovHR=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/RawHighRes/channel_split/"+name+"_channel_0.tif");
        VitimageUtils.printImageResume(imgRef);
        VitimageUtils.printImageResume(imgMov);
        VitimageUtils.printImageResume(imgRefHR);
        VitimageUtils.printImageResume(imgMovHR);
        VitimageUtils.adjustVoxelSize(imgRefHR, new double[]{voxSizeRefLow[0]/4,voxSizeRefLow[1]/4,voxSizeRefLow[2]/4},"cm");
        VitimageUtils.adjustVoxelSize(imgMovHR, new double[]{voxSizeMovLow[0]/4,voxSizeMovLow[1]/4,voxSizeMovLow[2]/4},"cm");
        VitimageUtils.printImageResume(imgRefHR);
        VitimageUtils.printImageResume(imgMovHR);

        imgMov=tr.transformImage(imgRef, imgMov);
        imgMovHR=tr.transformImage(imgRefHR, imgMovHR);

        imgRef.show();
        imgRef.setTitle("RefLow");
        imgMov.show();
        imgMov.setTitle("MovLow");

        imgRefHR.show();
        imgRefHR.setTitle("RefHigh");
        imgMovHR.show();
        imgMovHR.setTitle("MovHigh");

        VitimageUtils.waitFor(50000000);    
    }   

    public static void createCombinedTransforms(){
        for(int i=1;i<5;i++){
            System.out.println("i="+i);
            ItkTransform trGlob=ItkTransform.readTransformFromFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[i])+"_dense.transform.tif");
            ItkTransform trBmNoBias=ItkTransform.readTransformFromFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[i])+"_afternobias.transform.tif");
            System.out.println("read ok");
            ItkTransform tr=new ItkTransform();
            tr.addTransform(trGlob);
            tr.addTransform(trBmNoBias);
            System.out.println("add ok");
            ImagePlus imgRef=IJ.openImage(  mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[i])+"_dense.transform.tif");
            tr=tr.getFlattenDenseField(imgRef);
            System.out.println("flatten ok");
            tr.writeAsDenseField(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[i])+"_combined.transform.tif", imgRef);
            System.out.println("write ok");
        }
    }


    public static void resampleChannels(int n){
        System.out.println("Starting resampl "+n);
        String name0=VitimageUtils.withoutExtension(names[0]);
        String name=VitimageUtils.withoutExtension(names[n]);
        ImagePlus imgRef=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/RawHighRes/channel_split/"+name0+"_channel_"+0+".tif");
        ImagePlus imgRefLow=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/Grayscale/"+name0+".tif");
        double[]voxSizeRefLow=VitimageUtils.getVoxelSizes(imgRefLow);
        VitimageUtils.adjustVoxelSize(imgRef, new double[]{voxSizeRefLow[0]/4,voxSizeRefLow[1]/4,voxSizeRefLow[2]/4},"cm");

        ImagePlus imgMovLow=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/Grayscale/"+name+".tif");
        double[]voxSizeMovLow=VitimageUtils.getVoxelSizes(imgMovLow);
        System.out.println("Here");
        ItkTransform tr=ItkTransform.readTransformFromFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[n])+"_combined.transform.tif");

        System.out.println("THere too");
        for(int i=0;i<5;i++){
            System.out.println("i="+i);
            ImagePlus imgMov=IJ.openImage("/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/RawHighRes/channel_split/"+name+"_channel_"+i+".tif");
            VitimageUtils.adjustVoxelSize(imgMov, new double[]{voxSizeMovLow[0]/4,voxSizeMovLow[1]/4,voxSizeMovLow[2]/4},"cm");
            
            System.out.println("Transform");
            VitimageUtils.printImageResume(imgRef);
            VitimageUtils.printImageResume(imgMov);
            imgMov=tr.transformImage(imgRef, imgMov);
            System.out.println("save");
            IJ.saveAsTiff(imgMov, "/home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/RawHighRes/channel_registered/"+name+"_channel_"+i+".tif");            
       }
    }   
//    /home/rfernandez/Bureau/A_Test/Test_Sergio/Data_4/RawHighRes/channel_split


    public static void merde(int n){
        System.out.println("Starting merde "+n);
        String name=names[n];
        ImagePlus imgRef=IJ.openImage(mainDir+"Registered/dense/"+names[0]);
        ImagePlus imgMov=IJ.openImage(mainDir+"Registered/dense/"+names[n]+".dense.tif");
        System.out.println("Opeining image ok");
        ItkTransform trInit=ItkTransform.readAsDenseField(mainDir+"Transformations/"+VitimageUtils.withoutExtension(name)+"_afterbm.transform.tif");
        System.out.println("Reading transform ok");
        ImagePlus imgRes=trInit.transformImage(imgRef, imgMov);
        System.out.println("Transformed image ok");
        IJ.saveAsTiff(imgRes, mainDir+"Registered/bm_0/"+name);

    }


    public static void applyTransformToUpperResolution(int n){
        String name=names[n];
        ItkTransform trInit=ItkTransform.readAsDenseField(mainDir+"Transformations/"+VitimageUtils.withoutExtension(name)+"_afterbm.transform.tif");
        ImagePlus imgRef=IJ.openImage(mainDir+"Registered/dense/"+names[0]);
        ImagePlus imgMov=IJ.openImage(mainDir+"Registered/dense/"+names[n]+".dense.tif");
        ImagePlus imgRes=trInit.transformImage(imgRef, imgMov);
        IJ.saveAsTiff(imgRes, mainDir+"Registered/bm_0/"+name);
    }


    //STEP 5
    public static void registerWithBlockMatching(int n,int step){
        String name=names[n];
        ImagePlus imgRef=null;
        if(step==0)imgRef=IJ.openImage(mainDir+"Registered/dense/"+names[0]);
        else imgRef=IJ.openImage(mainDir+"Registered/imgFus_bm_0.tif");
        
        ImagePlus imgMov=IJ.openImage(mainDir+"Registered/dense/"+names[n]+".dense.tif");
        System.out.println(mainDir+"Registered/dense/"+names[0]);
        System.out.println(mainDir+"Registered/dense/"+names[n]+".dense.tif");
        double sigma=imgRef.getWidth()*VitimageUtils.getVoxelSizes(imgRef)[0]/ratioFactorForSigmaComputation;

        ImagePlus imgMask=null;
        if(step==0)imgMask=computeMask(imgRef,imgMov);
        else imgMask=IJ.openImage(mainDir+"Registered/imgFus_mask.tif");
        imgMask.show();
        imgRef.show();
        ItkTransform trInit=null;//ItkTransform.readAsDenseField(mainDir+"Transformations/"+VitimageUtils.withoutExtension(name)+"_dense.transform.tif");
        if(step==1)trInit=ItkTransform.readAsDenseField(mainDir+"Transformations/"+VitimageUtils.withoutExtension(name)+"_afterbm.transform.tif");


        RegistrationAction regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov);
        regAct.typeAction=RegistrationAction.TYPEACTION_AUTO;
        regAct.typeTrans=Transform3DType.DENSE;
        regAct.typeAutoDisplay=0;
        regAct.levelMaxDense=(n==3 ? 4 : 3);
        regAct.levelMinDense=1;
        regAct.iterationsBMDen=(n==3 ? 6 : 6);
        regAct.strideX=0;
        regAct.strideY=0;
        regAct.bhsX=(n==3 ? 7 : 3);
        regAct.bhsY=7;
        regAct.sigmaDense=sigma;
        regAct.selectScore=(n==3 ? 5 : 20);
        regAct.selectRandom=90;
        regAct.selectLTS=90;
        
        BlockMatchingRegistration bm=BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov, regAct);
        bm.setQuiversOn();
        //bm.setSingleView();
   
        //bm.activateFlagDisplayStatsAboutLandmarksGiven(VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(mainDir+"Roi/"+name+".zip",imgRef,imgMov,true));
        bm.metricType=MetricType.SQUARED_CORRELATION;
        bm.mask=imgMask;
//        bm.minBlockVariance/=2;
        bm.minBlockScore=(n==3 ? 0.03 : 0.15);
        if(step==1) bm.minBlockScore=0.03;
        if(step==1) bm.minBlockVariance/=4;
        if(step==1)bm.levelMax--;

        ItkTransform tr=bm.runBlockMatching(trInit, false);
        tr.writeAsDenseField(mainDir+"Transformations/"+VitimageUtils.withoutExtension(name)+(step==0 ? "_afterbm.tif" : "_afternobias.tif" ), imgRef);
        ImagePlus img=tr.transformImage(imgRef, imgMov);
        IJ.saveAsTiff(img, mainDir+"Registered/"+(step==0 ? "bm_0" :"bm_no_bias"+"/"+name));
/*         
        ImagePlus img=tr.viewAsGrid3D(imgRef, 100);
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
        */
        System.out.println("BM ok.");
    }
 

    public static void generateImgFusBm(){
        
        ImagePlus []imgs=new ImagePlus[5];
        String name=names[1];
        imgs[1]=IJ.openImage(mainDir+"Registered/bm_0/"+name);
        name=names[2];
        imgs[2]=IJ.openImage(mainDir+"Registered/bm_0/"+name);
        name=names[3];
        imgs[3]=IJ.openImage(mainDir+"Registered/bm_0/"+name);
        name=names[4];
        imgs[4]=IJ.openImage(mainDir+"Registered/bm_0/"+name);
        name=names[0];
        imgs[0]=IJ.openImage(mainDir+"Registered/dense/"+name);

        ImagePlus img=VitimageUtils.meanOfImageArray(imgs);
        IJ.saveAsTiff(img, mainDir+"Registered/imgFus_bm_0.tif");
        ImagePlus imgMask=computeMaskFiveImages(imgs);
        IJ.saveAsTiff(imgMask, mainDir+"Registered/imgFus_mask.tif");
    }




    //STEP 4
    public static void registerWithLandmarks(int n){
        int[]indices=new int[]{
            80,81,82,83,84,85,86,87,88,2000
        };
        String name=names[n];
        String pathToRoi=mainDir+"Roi/"+name+".zip";
        ImagePlus imgRef=IJ.openImage(mainDir+"Grayscale/"+names[0]);
        ImagePlus imgMov=IJ.openImage(mainDir+"Grayscale/"+names[n]);
        Point3d[][]tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(pathToRoi,imgRef,imgMov,true);
        if(n==3)tab=repair3(tab, indices);

        System.out.println("Before registration, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));
        //Compute the best affine registration
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
        tab=VitimageUtils.getCoordinatesFromRoiSetOfCorrespondences(pathToRoi,imgRef,imgMov,true);
        if(n==3)tab=repair3(tab, indices);
        for(int i=0;i<Ncorr;i++){
            tab[0][i]=trGlob.transformPoint(tab[0][i]);
        }
        System.out.println("After reg dense, stats = "+ItkTransform.getStatsAboutCorrespondancePoints(tab));

        IJ.saveAsTiff(imgTransAffine, mainDir+"Registered/"+name+".affine.tif");
        IJ.saveAsTiff(imgTransDense, mainDir+"Registered/"+name+".dense.tif");
        trAffGlob.writeMatrixTransformToFile(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[n])+"_affine.txt");
        trGlob=trGlob.getFlattenDenseField(imgRef);
        trGlob.writeAsDenseField(mainDir+"Transformations/"+VitimageUtils.withoutExtension(names[n])+"_dense.tif", imgRef);
        System.out.println("Register ok.");
        ImagePlus imgFuse=VitimageUtils.compositeNoAdjustOf(imgRef, imgTransDense);
        imgFuse.show();
    }
       
       


    //STEP 3
    public static void annotate(int n){
        ImagePlus imgRef=IJ.openImage(mainDir+"Rgb/"+names[0]);
        ImagePlus imgMov=IJ.openImage(mainDir+"Rgb/"+names[n]);
        Point3d[][]pts=VitiDialogs.registrationPointsUI(-1,imgRef,imgMov,true);
        Point3d[]ptslinearized=VitimageUtils.point3dDoubleTabToSingleTab(pts);
        VitimageUtils.writePoint3dTabToCsv(ptslinearized, mainDir+"Csv/"+names[n]+".csv");
        Roi[]roiTab=RoiManager.getInstance().getRoisAsArray();
        VitimageUtils.saveRoiAs(roiTab, mainDir+"Roi/"+names[n]+".zip");
        System.out.println("Annote ok.");
    }


    /**************Tests *********************************************************************************************/
    //STEP 2
    public static void setVoxelSizes(){
        ImagePlus img=null;
        for(int n=0;n<names.length;n++){
            System.out.println(names[n]);
            double valHR=voxSizes[n];
            double valLR=voxSizes[n]*subsamplingFactor;
            
            img=IJ.openImage(mainDir+"Grayscale/"+names[n]);
            VitimageUtils.adjustImageCalibration(img, new double[]{valLR,valLR,1}, "cm");
            IJ.saveAsTiff(img, mainDir+"Grayscale/"+names[n]);
            img.close();

            img=IJ.openImage(mainDir+"Ndvi/"+names[n]);
            VitimageUtils.adjustImageCalibration(img, new double[]{valLR,valLR,1}, "cm");
            IJ.saveAsTiff(img,mainDir+"Ndvi/"+names[n]);
            img.close();

            img=IJ.openImage(mainDir+"Raw/"+names[n]);
            VitimageUtils.adjustImageCalibration(img, new double[]{valLR,valLR,1}, "cm");
            IJ.saveAsTiff(img,mainDir+"Raw/"+names[n]);
            img.close();

/*
            img=IJ.openImage(mainDir+"RawHighRes/"+names[n]);
            VitimageUtils.adjustImageCalibration(img, new double[]{valHR,valHR,1}, "cm");
            img.close();
 */
            img=IJ.openImage(mainDir+"Rgb/"+names[n]);
            VitimageUtils.adjustImageCalibration(img, new double[]{valLR,valLR,1}, "cm");
            IJ.saveAsTiff(img,mainDir+"Rgb/"+names[n]);
            img.close();

        }
    }

    //STEP 1
    public static void generateNdvi(){
        String dirToRaw=mainDir+"Raw/";
        String dirToNdvi=mainDir+"Ndvi/";
        String dirToGrayscale=mainDir+"Grayscale/";

        for(String s:names){
            System.out.println(s);
            ImagePlus imgRaw=IJ.openImage(dirToRaw+s);
            ImagePlus imgIR=new Duplicator().run(imgRaw,5,5);
            ImagePlus imgR=new Duplicator().run(imgRaw,3,3); 
            ImagePlus imgG=new Duplicator().run(imgRaw,2,2);  
            ImagePlus imgB=new Duplicator().run(imgRaw,1,1);  
            imgIR=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgIR);
            imgR=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgR);
            imgG=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgG);
            imgB=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgB);

            //Compute the ndvi
            ImagePlus imgSub=VitimageUtils.makeOperationBetweenTwoImages(imgIR, imgR, 4, true);
            ImagePlus imgAdd=VitimageUtils.makeOperationBetweenTwoImages(imgIR, imgR, 1, true);
            ImagePlus imgNdvi=VitimageUtils.makeOperationBetweenTwoImages(imgSub, imgAdd, 3, true);
            imgNdvi.setDisplayRange(-1, 1);
            VitimageUtils.adjustImageCalibration(imgNdvi, imgRaw);
            IJ.saveAsTiff(imgNdvi, dirToNdvi+s);

            //Compute the grayscale
            ImagePlus imgGrayscale=VitimageUtils.meanOfImageArray(new ImagePlus[]{imgR,imgG,imgB});
            imgGrayscale.setDisplayRange(0, 65000*2);
            VitimageUtils.convertToGray8(imgGrayscale);
            VitimageUtils.adjustImageCalibration(imgGrayscale, imgRaw);            IJ.saveAsTiff(imgNdvi, dirToNdvi+s);

            IJ.saveAsTiff(imgGrayscale, dirToGrayscale+s);
        }
    }


}
