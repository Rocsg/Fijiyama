package net.imagej.fijiyama.rsml;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class Tests {
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		viewTimeLapse();
		//		viewModel();
	}

	public static void viewTimeLapse() {
		String rsmlPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_00002/Expertized_models/4_2_Model_0055.rsml";
		String imagePath="/home/rfernandez/Bureau/A_Test/RSML/1_Registered/ML1_Boite_00002.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus stackReg=IJ.openImage(imagePath);
		double[]observationTimes=new double[200];
		for(int i=0;i<observationTimes.length;i++)observationTimes[i]=(i+1)/10;
		ImagePlus res=rm.createGrayScaleImageTimeLapse(stackReg, observationTimes,new double[] {2,1},0.5); 
		res.show();
	}
	
	public static void viewExpert() {
		String rsmlPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_00002/Expertized_models/4_2_Model_0055.rsml";
		String imagePath="/home/rfernandez/Bureau/A_Test/RSML/1_Registered/ML1_Boite_00002.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus stackReg=IJ.openImage(imagePath);
		ImagePlus res=RootModel.projectRsmlOnImage(rm,stackReg,1,2,1,true,true,false);
		res.show();		
	}

	public static void viewModel() {
		String rsmlPath="/home/rfernandez/Bureau/A_Test/RSML/4_RSML_BACKTRACK/ML1_Boite_00008.rsml";
		String imagePath="/home/rfernandez/Bureau/A_Test/RSML/1_Registered/ML1_Boite_00008.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus stackReg=IJ.openImage(imagePath);
		ImagePlus res=RootModel.projectRsmlOnImage(rm,stackReg,1,2,1,true,false,true);
		res.show();				
		stackReg.show();
	}
	
}
