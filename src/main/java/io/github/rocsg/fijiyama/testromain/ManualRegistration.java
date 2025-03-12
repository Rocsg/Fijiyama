
package io.github.rocsg.fijiyama.testromain;

import java.util.ArrayList;


import com.jogamp.opengl.GLProfile;


import ij.IJ;

import ij.ImageJ;

//specific libraries

import ij.IJ;

import ij.ImageJ;

import ij.ImagePlus;

import io.github.rocsg.fijiyama.common.VitimageUtils;

import io.github.rocsg.fijiyama.fijiyamaplugin.Fijiyama_GUI;

import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationAction;

import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationManager;

import io.github.rocsg.fijiyama.registration.BlockMatchingRegistration;

import io.github.rocsg.fijiyama.registration.ItkTransform;

import io.github.rocsg.fijiyama.registration.Transform3DType;



// This class is intended to run batch manual registration for the cutting experiences.

//The number of manual registration to do is approximately 120, meaning there is a need to automate the gathering of files, opening of 3d universe, saving and

//organizing of the transformations, etc.

public class ManualRegistration {

   static String testSpecimen = "B_201";   // An-Information-with-elements-separated-with-minus_while-different-fields-will-be-separated-by-underscores

   static String[] timestamps = new String[]{"J001", "J029", "J077", "J141"};


   public static String getPathToReferenceImage(String specimen,int step){

       System.out.println("/home/phukon/Desktop/Cuttings_MRI_registration/"+specimen+"/raw_subsampled/"+specimen+"_"+timestamps[step]+"_sub222.tif");

       return"/home/phukon/Desktop/Cuttings_MRI_registration/"+specimen+"/raw_subsampled/"+specimen+"_"+timestamps[step]+"_sub222.tif";

       

   }


   public static String getPathToMovingImage(String specimen,int step){

       System.out.println("/home/phukon/Desktop/Cuttings_MRI_registration/"+specimen+"/raw_subsampled/"+specimen+"_"+timestamps[step+1]+"_sub222.tif");

       return"/home/phukon/Desktop/Cuttings_MRI_registration/"+specimen+"/raw_subsampled/"+specimen+"_"+timestamps[step+1]+"_sub222.tif";

       

   }




   public static ItkTransform  manualReg (ImagePlus refImage, ImagePlus movImage){
       //Fijiyama_GUI fg=new Fijiyama_GUI();
       RegistrationManager regManager = new RegistrationManager(new Fijiyama_GUI());

       regManager.start3dManualRegistration(refImage,movImage);

       ItkTransform tr=regManager.finish3dManualRegistration(); 

       return tr;      

   }


   public static void main(String[] args) {

       ImageJ ij=new ImageJ();

       System.setProperty("jogl.forceGL3", "true");

       System.setProperty("jogl.x11.display", ":0");

       GLProfile.initSingleton();

       GLProfile glp = GLProfile.get(GLProfile.GL3);

       test1_testStep1With201();

       

   }


   public static void test1_testStep1With201(){

       String specimen=testSpecimen;

       int step=0;

       ImagePlus imgRef = IJ.openImage(getPathToReferenceImage(specimen,step));

       ImagePlus imgMov = IJ.openImage(getPathToMovingImage(specimen,step));

       

       //Make the 3d manual registration

       //Gather the corresponding transformation

       ItkTransform tr=manualReg(imgRef, imgMov);


       //Apply it to the moving image

       ImagePlus imgManuallyRegistered=tr.transformImage(imgRef, imgMov);


   

       //Make superimposition (composite)

       VitimageUtils.compositeNoAdjustOf(imgRef, imgManuallyRegistered).show();

       VitimageUtils.waitFor(50000);

       System.exit(0);

   }

       


   

}