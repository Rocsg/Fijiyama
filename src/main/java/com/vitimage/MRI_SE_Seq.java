package com.vitimage;

public class MRI_SE_Seq extends Acquisition{

	public MRI_SE_Seq(String sourcePath) {
		super(MRI_SE_SEQ, sourcePath);
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

}
