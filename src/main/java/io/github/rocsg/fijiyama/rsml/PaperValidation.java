package io.github.rocsg.fijiyama.rsml;

import io.github.rocsg.fijiyama.common.*;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class PaperValidation {
	
	
	public static double pixSize4096=1.842;//µm per pixel
	public static double pixSize512=14.736;//µm per pixel
	public static int thresholdSize=3000;//squared µm
	
	
	public static void main(String []args) {
		ImageJ ij=new ImageJ();
		if(true)fading();
	}
	
	
	
	
	
	
	
	
	public static void fading() {
		String dir="/media/rfernandez/DATA_RO_A/Sorgho_BFF/Vaisseaux/Figures";
		ImagePlus sourceRGB=IJ.openImage(dir+"/Fading_sourceBright.tif");
		ImagePlus segRGB=IJ.openImage(dir+"/Fading_seg.tif");
		ImagePlus targetRGB=VitimageUtils.fadeRGB(sourceRGB,segRGB,53,15);
		targetRGB.show();
	}
	

	
}
