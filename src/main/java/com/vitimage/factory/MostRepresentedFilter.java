package com.vitimage.factory;

import com.vitimage.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class MostRepresentedFilter {
	public static int MAX_NB_CLASSES=256;
	public static double HIGH_DISTANCE=10E8;
	
	public static void main (String[]args) {
		ImageJ ij=new ImageJ();
		/*System.out.println(mostRepresentedValue(
				new int[] {10,11,12,12,14,11,15,16,13,18,19},
				new double[] {0,0,1,1,0,0,0,0,1,0,0}
				));
		
		VitiDialogs.getYesNoUI("Select segmentation image to filter");
		ImagePlus imgIn=IJ.openImage();
		double ray=VitiDialogs.getDoubleUI("Select radius of 2D structuring element", "Radius (in pixels)", 2);
		if(ray<1) {
			for(double ra=1;ra<=7 ; ra+=0.3) {
				printLookup(ra,buildLookup(ra));				
			}
			return;
		}
		boolean [][]lookup=buildLookup(ray);
		double [][]distances=buildDistances(ray);
		printLookup(ray,lookup);				
		ImagePlus imgOut=mostRepresentedFilteringWithRadius(imgIn,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgIn.show();
		imgOut.show();
		imgOut.setTitle(imgIn.getTitle()+"_filtered");
		*/
	}


	
	public static ImagePlus mostRepresentedFilteringWithRadius(ImagePlus imgIn,double radius,double[][]distances) {
		boolean[][]lookup=buildLookup(radius);
		ImagePlus imgOut=imgIn.duplicate();
		int dimX=imgIn.getWidth();
		int dimY=imgIn.getHeight();
		int dimZ=imgIn.getStackSize();
		int ray=(lookup.length-1)/2;
		int []vals=new int[lookup.length*lookup[0].length];
		double[] dist=new double[lookup.length*lookup[0].length];
		
		for(int z=0;z<dimZ;z++) {
			if(z%10==0)System.out.println(z+"/"+dimZ);
			byte[] valsImg=(byte[])imgIn.getStack().getProcessor(z+1).getPixels();
			byte[] valsOut=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
			for(int x=ray;x<dimX-ray;x++) {
				for(int y=ray;y<dimY-ray;y++){
					int index=0;
					for(int dx=-ray;dx<ray+1;dx++) {
						for(int dy=-ray;dy<ray+1;dy++){
							if(! lookup[dx+ray][dy+ray]) {
								dist[index]=10E8;
								vals[index++]=-1;
							}
							else {
								vals[index]=((byte)(valsImg[dimX*(y+dy)+(x+dx)]) & 0xff);
								dist[index++]=distances[dx+ray][dy+ray];
							}
						}
					}
					valsOut[dimX*(y)+(x)]=((byte)(VitimageUtils.mostRepresentedValue(vals, dist,256)  & 0xff));
				}			
			}
		}
		return imgOut;
	}
	
	
	
	public static boolean[][]buildLookup(double ray){
		int halfSize=( (int) Math.floor(ray));
		boolean[][]tab=new boolean[2*halfSize+1][2*halfSize+1];
		for(int i=0;i<tab.length;i++) {
			for(int j=0;j<tab.length;j++) {
				tab[i][j]=(Math.sqrt( (halfSize-i) * (halfSize-i) +(halfSize-j) * (halfSize-j))<=ray);
			}
		}
		return tab;
	}
	
	public static double[][]buildDistances(double ray){
		int halfSize=( (int) Math.floor(ray));
		double[][]tab=new double[2*halfSize+1][2*halfSize+1];
		for(int i=0;i<tab.length;i++) {
			for(int j=0;j<tab.length;j++) {
				tab[i][j]=(Math.sqrt( (halfSize-i) * (halfSize-i) +(halfSize-j) * (halfSize-j)));
			}
		}
		return tab;
	}

	
	
	public static void printLookup(double ra, boolean [][]tab) {
		System.out.println("Displaying structuring element of size "+ra+"\n");
		for(int i=0;i<tab.length;i++) {
			for(int j=0;j<tab.length;j++) {
				System.out.print((tab[i][j] ? "  @  " : "  -  "));
			}
			System.out.println();			
			System.out.println();			
		}
		System.out.println();			
		System.out.println();			
		System.out.println();			
	}
	
	
	
	
	public MostRepresentedFilter() {
		// TODO Auto-generated constructor stub
	}

}
