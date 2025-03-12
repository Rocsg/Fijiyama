package io.github.rocsg.fijiyama.testromain;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import jogamp.opengl.glu.mipmap.Image;

public class HistCorrection {
    public static void main(String[]args){

        test();

    }
    public static void test(){
        ImageJ ij=new ImageJ();
        ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test_2.tif");
        System.out.println(img);
        //Separate the channels
        ImagePlus[]channels=VitimageUtils.channelSplitter(img);
        int xM=img.getWidth();
        int yM=img.getHeight();

        //Compute the mean and standard deviation of each channel at each vertical line
        for(int i=0;i<channels.length;i++){
            ImagePlus channel=channels[i];


            double[]valsMu=new double[xM];
            double[]valsSigma=new double[xM];
            for(int j=0;j<channel.getWidth();j++){
                double[]vals=VitimageUtils.valuesOfBlock(channel, j, 0, 0, j, yM-1, 0);
                //Compute the mean and standard deviation of the channel at the vertical line j
                double[]tab=VitimageUtils.statistics1D(vals);
                valsMu[j]=tab[0];
                valsSigma[j]=tab[1];
            }
            //compute a moving average of it
            double[]valsMuNew=movingAverageWindow(valsMu, 1000);
            valsMu=movingAverageWindow(valsMu, 10);
            double[]valsSigmaNew=movingAverageWindow(valsSigma, 1000);

            //Access all pixel wise
            byte[] valsImg=(byte[])(channel.getStack().getProcessor(1).getPixels());
            for(int x=0;x<xM;x++) {
                System.out.println("Mu="+valsMu[x]+" Sigma="+valsSigma[x]);
                System.out.println("MuNew="+valsMuNew[x]+" SigmaNew="+valsSigmaNew[x]);
                for(int y=0;y<yM;y++) {
    //                System.out.println();
                    double val=(double)((int)(  (byte)(valsImg[xM*y+x]&0xff) &0xff));
  //                  System.out.println(val);
                    double valRel=(val-valsMu[x])/valsSigma[x];
//                    System.out.println(valRel);
                    double valFut=(valRel*valsSigma/*New*/[x]+valsMuNew[x]);
//                    System.out.println (valFut);
                    if(valFut>255)valFut=255;
                    if(valFut<0)valFut=0;
                    valsImg[xM*y+x]=(byte) (((int)valFut) & 0xff);
//                    VitimageUtils.waitFor(1000000);
                }
            }			

        }

        ImagePlus result=VitimageUtils.compositeRGBByte(channels[0], channels[1], channels[2], 1,1,1);
        img.show();
        result.show();
    }

    public static double[]movingAverage(double[]tab,double alpha){
        double[]ret=new double[tab.length];
        ret[0] = tab[0]; // Initialize the first value to the first data point
        
        for (int i = 1; i < tab.length; i++) {
            ret[i] = alpha * tab[i] + (1 - alpha) * ret[i - 1];
        }

        return ret;
    }
    public static double[]movingAverageWindow(double[]tab,int window){
        double[]ret=new double[tab.length];

        int nb=0;
        double sum=0;    
        for (int i = 0; i < tab.length; i++) {
            int winMin=Math.max(0,i-window/2);
            int winMax=Math.min(tab.length,i+window/2);
            for(int j=winMin;j<winMax;j++){
                sum+=tab[j];
                nb++;
            }
            ret[i]=sum/nb;
            sum=0;
            nb=0;
        }
        return ret;
    }
}

    //For each vertical line (Y line) of the image, compute the mu and sigma of R, G, and B channels

