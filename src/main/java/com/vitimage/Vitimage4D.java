package com.vitimage;

public class Vitimage4D {

	private ImagePlus[] acquisitionsImage;//Observations goes there
	private ImagePlus parametersImage ;//Data lies here
	private String []observationsLabels;
	private String[] parametersLabels;
	

	public static void main(String[] args) {
		// Test sequence for the class
		Vitimage4D viti = new Vitimage4D();
		
		
		viti.newAcquisition(Acquisition.MRI_T1_SEQ,choosePathUI(),)

	}

	
	
	public Vitimage4D() {
		
	}
	
	public void setGeneralReference() {
		
		
	}
	
	public void newAcquisition(Acquisition acq, ImagePlus[]acquisition,int geometry, int ) {
		
		
	}
	
	public	void registerMriAcquisitions(boolean sameGeometry) {
		
		
	}
	
	public void registerAll()
	
	
	
	
	
}
