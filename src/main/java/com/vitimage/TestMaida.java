package com.vitimage;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import trainableSegmentation.WekaSegmentation;

public class TestMaida {

	
	public static void test() {
		ImagePlus imgToSegment=new ImagePlus("/home/fernandr/Bureau/imgTest.tif");
		WekaSegmentation weka= new WekaSegmentation(imgToSegment);
		weka.loadClassifier("/home/fernandr/Bureau/classifier.model");
		System.out.println("Classifieur chargé");
		weka.loadNewImage(imgToSegment);
		weka.applyClassifier(false);
		ImagePlus binary=weka.getClassifiedImage();
		System.out.println("binary chargée");
		System.out.println(binary.getStackSize());
		binary.show();
		VitimageUtils.waitFor(10000);
	}
	
	public static void main(String[]args) {
		System.out.println("Lancement tests Maida");
		ImageJ ij=new ImageJ();
		test();
		System.exit(0);
		
		ImagePlus imgReference=IJ.openImage("/home/fernandr/Bureau/Test/flip_flop/Echo_001_1.dcm");
		ImagePlus imgMoving=IJ.openImage("/home/fernandr/Bureau/Test/flip_flop/Echo_001_2.dcm");
		imgReference=Concatenator.run(new ImagePlus[] {imgReference,imgReference,imgReference,imgReference});
		imgReference.setTitle("Refrence");
		imgReference.getProcessor().resetMinAndMax();
		imgReference.show();

		imgMoving=Concatenator.run(new ImagePlus[] {imgMoving,imgMoving,imgMoving,imgMoving});
		imgMoving.setTitle("Moving");
		imgMoving.getProcessor().resetMinAndMax();
		imgMoving.show();

		ImagePlus imgResult=runAutomaticRegistration(imgReference,imgMoving);
		imgResult.setTitle("Result");
		imgResult.getProcessor().resetMinAndMax();
		imgResult.show();
		
	}
	
	
	public TestMaida() {
		// TODO Auto-generated constructor stub
	}

	
	
	
	public static ImagePlus runAutomaticRegistration(ImagePlus imgReference,ImagePlus imgMoving) {
		//System.out.println("Recalage automatique de "+imgMoving.getTitle()+" sur "+imgReference.getTitle());
		System.out.println("La");
		System.out.println("Conversion...");
		ImagePlus imgMov=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgMoving);
		ImagePlus imgRef=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgReference);
		ItkRegistrationManager manager=new ItkRegistrationManager();
		System.out.println("Ok.");
		System.out.println("Recalage...");
		ImagePlus result=manager.runScenarioMaidaFlipFlop(imgRef, imgMov);
		System.out.println("Ok.");
		result.setTitle("Resultat recalage auto avant conversion");
		result.getProcessor().resetMinAndMax();
		result.show();
		ImagePlus result2=VitimageUtils.convertFloatToShortWithoutDynamicChanges(result);
		System.out.println("Conversion  ok");
		result2.setTitle("Resultat recalage auto apres conversion");
		result2.getProcessor().resetMinAndMax();
		//result2.show();
		return result2;
	}
	
	
	
	
}
