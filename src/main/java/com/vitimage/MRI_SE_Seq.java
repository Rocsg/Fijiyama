package com.vitimage;


import ij.ImagePlus;

public class MRI_SE_Seq extends Acquisition{

	public MRI_SE_Seq(String sourcePath,Capillary cap, SupervisionLevel supervisionLevel) {
		super(AcquisitionType.MRI_SE_SEQ, sourcePath,cap, supervisionLevel);
	}

	public void readDataFromFile() {
		//Make inventory of metadata and set all available parameters : dims, Voxsizes, nbechoes, day, operator, acq duration, ...
		
		//Get the slices, stack them
		
				
	}

	@Override
	public void processData() {
		//Do nothing
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
