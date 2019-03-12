package com.vitimage;

public class MRI_Ge3D extends Acquisition {

	public MRI_Ge3D(int typeAcquisition, String sourcePath) {
		super(typeAcquisition, sourcePath);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void readDataFromFile() {
		//Make inventory of metadata and set all available parameters : dims, Voxsizes, nbechoes, day, operator, acq duration, angle ...
		
		//Get the slices, stack them
		
	}

	@Override
	public void processData() {
		// nothing to do
		
	}

}
