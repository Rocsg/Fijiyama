package com.vitimage;

import java.util.ArrayList;

import ij.ImagePlus;

public class Vitimage4D {
	public int dayAfterInoculation;
	private ArrayList<Acquisition> acquisition;//Observations goes there
	
	
	
	/**
	 *  Test sequence for the class
	 */
	public static void main(String[] args) {
		Vitimage4D viti = new Vitimage4D(0,5);		
		viti.addAcquisition(Acquisition.MRI_T1_SEQ,choosePathUI(),Geometry.REFERENCE,Misalignment.NONE);
		viti.addAcquisition(Acquisition.MRI_T2_SEQ,choosePathUI(),Geometry.REFERENCE,Misalignment.LIGHT_RIGID);
		viti.addAcquisition(Acquisition.MRI_GE3D_SEQ,choosePathUI(),Geometry.REFERENCE,Misalignment.SCALE_FACTOR);
		viti.addAcquisition(Acquisition.RX,choosePathUI(),Geometry.SWITCH_XY,Misalignment.SCALE_FACTOR);
		viti.computeAll();
		ImagePlus result=viti.createNormalizedHyperImage();
		result.show();
	}

	
	
	public Vitimage4D(int dayAfterInoculation,int nbAcquisitions) {
		this.dayAfterInoculation=dayAfterInoculation;
		
	}
	
	public ImagePlus createNormalizedHyperImage() {
		
		
	}
	
	public void addAcquisition(int type, String path,int geometry) {
		switch(type) {
		case MRI_T1_SEQ: acquisition.add(new MRI_T1_Seq());
		
	}
	
	public	void registerMriAcquisitions(Misalignment.LIGHT_RIGID mis) {
		
		
	}
	
	
	public void computeAll() {
		handleMajorMisalignments();
		alignAndCenterAxes();
		registerM
		
	
	}
	
	
}
