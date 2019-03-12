/**
 * 
 */
package com.vitimage;

/**
 * @author fernandr
 *
 */
public class MRI_T1_Seq extends Acquisition implements Fit{

	/**
	 * @param typeAcquisition
	 * @param sourcePath
	 */
	public MRI_T1_Seq(String sourcePath) {
		super(MRI_T1_SEQ, sourcePath);
	}

	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	
	

	@Override
	public void readDataFromFile() {
		//Make inventory of metadata and set all available parameters : dims, Voxsizes, nbechoes, day, operator, acq duration, ...
		
		//For each repetition time
		
		//Get the slices, stack them
		
		//Register the n-1 first echoes on the last one

		//Store the n-1 transformed results, and the nth untransformed into tab
	}


	public void processData() {
		//Compute T1 map
	}

}
