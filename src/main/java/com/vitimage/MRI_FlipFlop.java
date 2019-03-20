package com.vitimage;

import ij.ImagePlus;

public class MRI_FlipFlop extends Acquisition {

	public MRI_FlipFlop(String sourcePath,Capillary cap) {
		super(AcquisitionType.MRI_FLIPFLOP_SEQ, sourcePath,cap);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void readDataFromFile() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processData() {
		// TODO Auto-generated method stub
		
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
