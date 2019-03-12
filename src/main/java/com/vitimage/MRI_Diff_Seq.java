package com.vitimage;

public class MRI_Diff_Seq extends Acquisition{

	public MRI_Diff_Seq(String sourcePath) {
		super(MRI_DIFF_SEQ, sourcePath);
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

}
