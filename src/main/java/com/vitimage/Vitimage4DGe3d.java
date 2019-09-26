package com.vitimage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import com.sun.tools.extcheck.Main;
import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.OptimizerType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;
import com.vitimage.TransformUtils.Geometry;
import com.vitimage.TransformUtils.Misalignment;
import com.vitimage.VitimageUtils.Capillary;
import com.vitimage.VitimageUtils.ComputingType;
import com.vitimage.VitimageUtils.SupervisionLevel;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import math3d.Point3d;

public class Vitimage4DGe3d  extends Vitimage4D implements VitiDialogs,TransformUtils,VitimageUtils{
	private double[] referenceVoxelSizeSmall;
	private double[]referenceVoxelSizeGe3d;
	public int[]referenceImageSizeSmall;
	public int[]referenceImageSizeGe3d;
	public static final int targetHyperSize=7;
	/**
	 *  Test sequence for the class
	 */
	public static void main(String[] args) {
		ImageJ ij=new ImageJ();
		String subject="B099_PCH";
		String day="J0";
		Vitimage4DGe3d viti = new Vitimage4DGe3d(VineType.CUTTING,0,"/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+subject+"/Source_data/"+day,
								subject+"_"+day,ComputingType.COMPUTE_ALL);			
		viti.start(2);
		viti.normalizedHyperImage.show();
	}
	
	
	
	
	public Vitimage4DGe3d(VineType vineType, int dayAfterExperience,String sourcePath,String title,ComputingType computingType) {
		super(vineType, dayAfterExperience, sourcePath,title,computingType);
		this.acquisitionStandardReference=AcquisitionType.MRI_GE3D;
		this.computingType=computingType;
		this.title=title;
		this.sourcePath=sourcePath;
		this.vineType=vineType;
		acquisition=new ArrayList<Acquisition>();//Observations goes there
		geometry=new ArrayList<Geometry>();
		misalignment=new ArrayList<Misalignment>();
		capillary=new ArrayList<Capillary> ();
		imageForRegistration=null;
		transformation=new ArrayList<ItkTransform>();
		transformation.add(new ItkTransform());//No transformation for the first image, that is the reference image
	}
	
}
