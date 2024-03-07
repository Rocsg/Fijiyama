package io.github.rocsg.fijiyama.registration;

import ij.ImagePlus;
import io.github.rocsg.fijiyama.fijiyamaplugin.RegistrationAction;
import javassist.bytecode.analysis.ControlFlow.Block;

public class TestGargee {
    public static void main(String[] args) {
        System.out.println("Hello Gargee");
        System.out.println("Now running the test...");
        test();
    }

    public static void testRegistration(){
        ImagePlus imgRef=null;
        ImagePlus imgMov=null;
        RegistrationAction regAct=new RegistrationAction();
        regAct.defineSettingsFromTwoImages(imgRef, imgMov, null, false);
        regAct.bhsX=3;
        regAct.bhsY=3;
        
        BlockMatchingRegistration bm = BlockMatchingRegistration.setupBlockMatchingRegistration(imgRef, imgMov,regAct);
        ItkTransform tr=bm.runBlockMatching(null, false);

        //Apply the transform 
        ImagePlus imgRegistered=tr.transformImage(imgRef, imgMov);
        
    }


    public static void test() {
        //ItkTransform myRigidBodyTransform=ItkTransform.readTransformFromFile("Please write here your path");
        //or
        ItkTransform myRigidBodyTransform=ItkTransform.array16ElementsToItkTransform(new double[]{1,0,0,1,  3,1,4,2,  6,8,1,3,  0,0,0,1});
        doSomething(myRigidBodyTransform);
        System.out.println("Test passed!");

        double[] aMatrixtoVector = myRigidBodyTransform.from2dMatrixto1dVector();

        //System.out.println(aMatrixtoVector);   
       // for(int i=0;i<6 ; i++)System.out.println(aMatrixtoVector[i]);
        
        for(int i=0;i<6 ; i++)System.out.print(aMatrixtoVector[i]+", ");
        System.out.println();
    }


    public static void doSomething(ItkTransform myRigidBodyTransform) {
        System.out.println("I am doing something with the transform");
    }


}
