package com.vitimage.ndimaging;

import ij.ImagePlus;

public class MRI_Diff_Seq extends Acquisition{

	public MRI_Diff_Seq(String sourcePath,Capillary cap, SupervisionLevel supervisionLevel) {
		super(AcquisitionType.MRI_DIFF_SEQ, sourcePath,cap, supervisionLevel);
	}

	public void readDataFromFile() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processData() {
		//Compute anisotropy map
		
		//Compute longitudinality (norm(Z)/norm(X+Y)
	}

	public ImagePlus computeNormalizedHyperImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setImageForRegistration() {
		// TODO Auto-generated method stub
		
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
