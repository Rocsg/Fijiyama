/*
 * 
 */
package io.github.rocsg.fijiyama.fijiyamaplugin;

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.ItkTransform;

// TODO: Auto-generated Javadoc
/**
 * The Class TestScriptBeanshell.
 */
public class TestScriptBeanshell{
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[]args) {
		//runBeanShellMode();
	}

	
	
	/**
	 * Run compose transform in bean shell mode.
	 */
	public static void runComposeTransformInBeanShellMode() {
		
		IJ.log("Starting transformation composition...");
		
		String pathToFirstTransform="/home/rfernandez/Bureau/Tt/Registration_files/Transform_Step_0.txt";//"Complete Here, a path to a .txt or .transform.tif file"; 
		String pathToSecondTransform="/home/rfernandez/Bureau/Tt/Registration_files/Transform_Step_1.txt";//"Complete here, a path to a .txt or .transform.tif file"; 
		String pathToNthTransform="/home/rfernandez/Bureau/Tt/Registration_files/Transform_Step_2.transform.tif";//"Complete here, a path to a .txt or .transform.tif file"; 
		String pathToWriteTheOutputComposedTransformation="/home/rfernandez/Bureau/Tt/Registration_files/Transform_Step_combined.transform.tif"; 
		String pathToReferenceImage="";//Only needed if there is at least one dense transform, elsewhere, keep blank. A reference image, giving output space dimensions
		///home/rfernandez/Bureau/Fijiyama_DOI/Case_01_Two_images/Input_data/imgRef.tif
		ItkTransform tr1=ItkTransform.readTransformFromFile(pathToFirstTransform);
		ItkTransform tr2=ItkTransform.readTransformFromFile(pathToSecondTransform);
		ItkTransform trN=ItkTransform.readTransformFromFile(pathToNthTransform);
		
		ItkTransform trGlob=new ItkTransform();
		trGlob.addTransform(tr1);
		trGlob.addTransform(tr2);
		trGlob.addTransform(trN);

		if(trGlob.isDense()) {//Hope that you gave a ref image
			if(pathToReferenceImage.equals("")) {
				IJ.log("You should give a reference image because one of your transformation is a dense vector field. Aborting.");
				System.exit(0);
			}
			ImagePlus imgRef=IJ.openImage(pathToReferenceImage);
			trGlob.writeAsDenseField(pathToWriteTheOutputComposedTransformation,imgRef);			
		}
		else {
			trGlob.writeMatrixTransformToFile(pathToWriteTheOutputComposedTransformation);
		}
		IJ.log("Transform successfully saved.");
	}

		
		
		
		
		
	
	
	
	
	
	
	
	
	
	
	/**
	 * Run apply transform in bean shell mode.
	 */
	public static void runApplyTransformInBeanShellMode() {
	
//Here lies the beanshell code
//import fr.cirad.image.fijiyama.*;
System.out.println("Toto");	

	String pathToTheImageToBeTransformed="Complete Here"; 
	String pathToTheReferenceImage="Complete here, that can be the same, it is just to know the target dimensions"; 
	String pathToTheTransformation="Complete here, a path to a .txt or .transform.tif file"; 
	String pathToSaveTheResult="Complete here, a path to a future .tif file or something like that";
	
	ImagePlus imgMov=IJ.openImage(pathToTheImageToBeTransformed);
	ImagePlus imgRef=IJ.openImage(pathToTheReferenceImage);
	ItkTransform tr=ItkTransform.readTransformFromFile(pathToTheTransformation);

	if(tr==null) {IJ.showMessage("No transform given. Abort. You should check the path");return;}
	if(imgMov==null) {IJ.showMessage("Moving image does not exist. Abort. You should check the path");return;}
	if(imgRef==null) {IJ.showMessage("Reference image does not exist. Abort. You should check the path");return;}
	
	ImagePlus result=tr.transformImage(imgRef, imgMov, false);
	result.setTitle("Transformed image");
	result.setDisplayRange(imgRef.getDisplayRangeMin(),imgRef.getDisplayRangeMax());
	IJ.saveAsTiff(result, pathToSaveTheResult);

	//Optional : remove the line below after the first test 
	result.show();

		
		
		
		
		
		
		
		
		
		
		
		
	}

}