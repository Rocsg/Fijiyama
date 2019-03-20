package com.vitimage;

import ij.ImagePlus;

public class MRI_Diff_Seq extends Acquisition{

	public MRI_Diff_Seq(String sourcePath,Capillary cap) {
		super(AcquisitionType.MRI_DIFF_SEQ, sourcePath,cap);
	}

	@Override
	public void readDataFromFile() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processData() {
		//Compute anisotropy map
		
		//Compute longitudinality (norm(Z)/norm(X+Y)
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
