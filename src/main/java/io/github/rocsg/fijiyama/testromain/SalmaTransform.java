package io.github.rocsg.fijiyama.testromain;
import ij.ImageJ;
import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import io.github.rocsg.fijiyama.common.VitimageUtils;

public class SalmaTransform {

    
    public static void main(String[] args) {
        ImageJ imageJ=new ImageJ();
        System.out.println("SalmaTransform");

        String inputDir="/home/rfernandez/Bureau/Temp/TestSalma/Full_data";
        String outputDir="/home/rfernandez/Bureau/Temp/TestSalma/Full_stack";
      //makeStack(inputDir,outputDir);
        analyzeStack(  inputDir, "/home/rfernandez/Bureau/Temp/TestSalma/Full_stack/stack_line.tif","/home/rfernandez/Bureau/Temp/TestSalma/Full_stack/out_data.csv","/home/rfernandez/Bureau/Temp/TestSalma/Full_stack/out_names.csv");
    }


    public static void makeStack(String inputDir,String outputDir){
        //List images names in inputDir
        File f=new File(inputDir);
        String[]imgNames=f.list();

        //count the number of image, open each image with IJ.openImage and store them into an array
        int n=imgNames.length;
        ImagePlus[]imgArray=new ImagePlus[n];
        for(int i=0;i<n;i++){
            System.out.println(""+imgNames[i]);
            imgArray[i]=IJ.openImage(inputDir+"/"+imgNames[i]);
            IJ.run(imgArray[i],"8-bit","");
            int X=imgArray[i].getWidth();
            int Y=imgArray[i].getHeight();
            imgArray[i]=VitimageUtils.drawRectangleInImage(imgArray[i], 0, 0, 70, Y-1, 0);
            imgArray[i]=VitimageUtils.drawRectangleInImage(imgArray[i], X-70, 0, X-1, Y-1, 0);
            imgArray[i]=VitimageUtils.drawRectangleInImage(imgArray[i], 0, Y-30, X-1, Y-1, 0);
        }

        //Find the maximum width and the maximum height
        int maxWidth=0;
        int maxHeight=0;
        for(int i=0;i<n;i++){
            if(imgArray[i].getWidth()>maxWidth)maxWidth=imgArray[i].getWidth();
            if(imgArray[i].getHeight()>maxHeight)maxHeight=imgArray[i].getHeight();
        }

        //Uncrop each image to fit to this maximum
        for(int i=0;i<n;i++){
            int X=imgArray[i].getWidth();
            int Y=imgArray[i].getHeight();
            int delta=(-X+maxWidth)/2;
            imgArray[i]=VitimageUtils.uncropImageByte(imgArray[i], delta, 0, 0, maxWidth, maxHeight, 1);
        }
        ImagePlus stack=VitimageUtils.slicesToStack(imgArray);
        IJ.saveAsTiff(stack, outputDir+"/stack.tif");        
    }


    public static void analyzeStack(       String inputDir,   String inputImagePath,String outputCsvPath,String outputCsvPathNames){
        //Open the stack
        ImagePlus stack=IJ.openImage(inputImagePath);

        //This is a 32 bit image. For each slice, there is a single column along Y. Read the column and store it into an array
        int n=stack.getStackSize();
        int X=stack.getWidth();
        int Y=stack.getHeight();
        double[][]data=new double[n][Y];
        for(int i=0;i<n;i++){
            for(int y=0;y<Y;y++){
                    data[i][y]=stack.getStack().getProcessor(i+1).getf(0, y);
            }
        }
        

       //List images names in inputDir
       File f=new File(inputDir);
       String[]imgNames=f.list();
       String[][]names=new String[n][1];
       for(int i=0;i<n;i++)names[i][0]=imgNames[i];
        VitimageUtils.writeDoubleTabInCsv(data, outputCsvPath);
        VitimageUtils.writeStringTabInCsv(names, outputCsvPathNames);
    }

    public static void run(String inputDir,String outputDir){
        int test=4;
        File f=new File(inputDir);
        String[]imgNames=f.list();
        for(String imgName:imgNames){
            System.out.println("Processing "+imgName);
            ImagePlus img=IJ.openImage(inputDir+"/"+imgName);
            if(test==1){img.show();VitimageUtils.waitFor(100000000);if(true)return;}

            //Convert img HSB stack
            ImagePlus imgHSB=img.duplicate();
            System.out.println("Toto");
            IJ.run(imgHSB, "HSB Stack", "");
            System.out.println("Toto2");
            if(test==2){imgHSB.show();VitimageUtils.waitFor(100000000);if(true)return;}
                
            //Split HSB stack
            ImagePlus imgHue=new Duplicator().run(imgHSB,1,1,1,1,1,1);
            ImagePlus imgSat=new Duplicator().run(imgHSB,2,2,1,1,1,1);
            ImagePlus imgBrig=new Duplicator().run(imgHSB,3,3,1,1,1,1);
            if(test==3){imgHue.show();imgSat.show();imgBrig.show();VitimageUtils.waitFor(100000000);if(true)return;}

            //Get a mask of the inner area
            ImagePlus imgMaskArea=VitimageUtils.thresholdByteImage(imgSat, 1, 256);            
            for(int i=0;i<20;i++)IJ.run(imgMaskArea, "Dilate", "");
            if(test==4){imgSat.show();imgMaskArea.show();VitimageUtils.waitFor(100000000);if(true)return;}
            
            //TEST 2            imgHSB.show();if(true)return;
            
        }
    }



}
