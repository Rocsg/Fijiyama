package com.vitimage;

import com.vitimage.VitimageUtils.Capillary;

import ij.ImagePlus;

public class MRI_SE_Seq extends Acquisition{

	public MRI_SE_Seq(String sourcePath,Capillary cap) {
		super(AcquisitionType.MRI_SE_SEQ, sourcePath,cap);
	}

	@Override
	public void readDataFromFile() {
		//Make inventory of metadata and set all available parameters : dims, Voxsizes, nbechoes, day, operator, acq duration, ...
		
		//Get the slices, stack them
		
				
	}

	@Override
	public void processData() {
		//Do nothing
	}

	@Override
	public ImagePlus createNormalizedHyperImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setImageForRegistration() {
		// TODO Auto-generated method stub
		
	}

}
