package com.vitimage;

import ij.ImagePlus;

public class MRI_Ge3D extends Acquisition {

	public MRI_Ge3D(String sourcePath,Capillary cap, SupervisionLevel supervisionLevel) {
		super(AcquisitionType.MRI_GE3D_SEQ, sourcePath,cap, supervisionLevel);
		// TODO Auto-generated constructor stub
	}


	public void readDataFromFile() {
		//Make inventory of metadata and set all available parameters : dims, Voxsizes, nbechoes, day, operator, acq duration, angle ...
		
		//Get the slices, stack them
		
	}

	@Override
	public void processData() {
		// nothing to do
		
	}

	@Override
	public void setImageForRegistration() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ImagePlus computeNormalizedHyperImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void quickStartFromFile() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

}
