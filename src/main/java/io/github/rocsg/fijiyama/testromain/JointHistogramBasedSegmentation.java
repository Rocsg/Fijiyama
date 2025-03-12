package io.github.rocsg.fijiyama.testromain;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import io.github.rocsg.fijiyama.common.VitimageUtils;

public class JointHistogramBasedSegmentation {
    public static void main(String[] args) {
        ImageJ ij=new ImageJ();
        String imgPathR="/home/rfernandez/Bureau/A_Test/Gargee/imgRef.tif";//Red channel
        String imgPathG="/home/rfernandez/Bureau/A_Test/Gargee/imgMov.tif";//Gree channel
        ImagePlus imgR=IJ.openImage(imgPathR);
        ImagePlus imgG=IJ.openImage(imgPathG);

        
        ImagePlus[]jointHistograms=computeAndDisplayJointHistograms(imgR,imgG);

        ImagePlus segmentation=makeSegmentationBasedOnRoiInputFromTheUser(imgR,imgG,jointHistograms[0],jointHistograms[1]);
        segmentation.setTitle("segmentation");
        segmentation.show();
    }



    public static ImagePlus makeSegmentationBasedOnRoiInputFromTheUser(ImagePlus imgR,ImagePlus imgG,ImagePlus histo,ImagePlus histoLog){
         //Ask for three rois
        RoiManager roiManager=RoiManager.getRoiManager();
        int nbAreas=3;
        boolean finished=false;

        do {
			VitimageUtils.waitFor(1000);
			if(roiManager.getCount()==nbAreas)finished=true;
			System.out.println("Waiting "+nbAreas+". Current number="+roiManager.getCount());
		}while (!finished);	

        Roi roi1=roiManager.getRoi(0);
        Roi roi2=roiManager.getRoi(1);
        Roi roi3=roiManager.getRoi(2);


        // Get access to original images (imgR and imgG) as dataR[] and dataG[]

        //Create a destination image (float), of the size of the original image
        ImagePlus segmentation=VitimageUtils.nullImage(imgG);

        //Take access to the pixels of segmentation as dataSegmentation[]

        //For each pixl (x,y,z)
            //Collect the value of imgR and imgG. It yields equivalent coordinates within the histogram (x,y)

            //For each roi, check if the point (x,y) is contained by the Roi
                if(roi1.contains(0,0)){
                    
                }
                //If so, this point in the segmentation should be given the label of this "area"
                //If not we don t care

        


        


        return segmentation;
    }


    public JointHistogramBasedSegmentation(){
        //Do nothing
    }

    public static ImagePlus[] computeAndDisplayJointHistograms(ImagePlus imgR,ImagePlus imgG){
        ImagePlus imgProba=ij.gui.NewImage.createImage("Proba",256,256,1,32,ij.gui.NewImage.FILL_BLACK);
        ImagePlus imgLogProba=ij.gui.NewImage.createImage("Log-Proba",256,256,1,32,ij.gui.NewImage.FILL_BLACK);
        float[] dataProba=(float[])(imgProba.getStack().getProcessor(1).getPixels());
        float[] dataLogProba=(float[])(imgLogProba.getStack().getProcessor(1).getPixels());


        //Normally we should verify that they have the same siz
        int X=imgR.getWidth();
        int Y=imgR.getHeight();
        int Z=imgR.getStackSize();
        int Npix=X*Y*Z;
        double fuzzyValue=Math.log(1/Npix)-5;

        //We know that values are between 0 and 255
        int[][]jointHistogramSumOfPixels=new int[256][256];
        double[][]jointHistogramProbability=new double[256][256];
        double[][]jointHistogramLogProbability=new double[256][256];
       
        //Gain access to pixels of image
        byte[] dataR;
        byte[] dataG;
        for(int z=0;z<Z;z++) {
            dataR=(byte[])(imgR.getStack().getProcessor(z+1).getPixels());
            dataG=(byte[])(imgG.getStack().getProcessor(z+1).getPixels());
            for(int x=0;x<X;x++) {
                for(int y=0;y<Y;y++){
                    int valR=((int)(dataR[x+X*y] & 0xff));
                    int valG=((int)(dataG[x+X*y] & 0xff));
                    jointHistogramSumOfPixels[valR][valG]+=1;
                }
            }
        }

        //Normalize for getting a probability

        for(int r=0;r<256;r++)for(int g=0;g<256;g++){
            jointHistogramProbability[r][g]=jointHistogramSumOfPixels[r][g]*1.0/Npix;
            
            //write things in the proba image
            dataProba[r+256*(255-g)]=(float)jointHistogramProbability[r][g];

            if(jointHistogramSumOfPixels[r][g]!=0){
                jointHistogramLogProbability[r][g]=Math.log(jointHistogramSumOfPixels[r][g]*1.0/Npix);
            }
            else{
                jointHistogramLogProbability[r][g]=fuzzyValue;
            }

            //write things in the logproba image
            dataLogProba[r+256*(255-g)]=(float)jointHistogramLogProbability[r][g];
        }          
                      
        imgProba.show();
        imgLogProba.show();
        return new ImagePlus[]{imgProba,imgLogProba};
    }
}
