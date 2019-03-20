package com.vitimage;

import java.util.ArrayList;

import com.vitimage.TransformUtils.Geometry;
import com.vitimage.TransformUtils.Misalignment;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import math3d.Point3d;

public class Vitimage4D implements VitiDialogs,TransformUtils,VitimageUtils{
	public enum VineType{
		GRAFTED_VINE,
		VINE,
		CUTTING
	}

	
	public final VineType vineType;
	public final int dayAfterExperience;
	private ArrayList<Acquisition> acquisition;//Observations goes there
	private ArrayList<Geometry> geometry;
	private ArrayList<Misalignment> misalignment;
	private ArrayList<ItkTransform> transformation;
	private ArrayList<Capillary> capillary;
	private ArrayList<ImagePlus> imageForRegistration;
	private int[] referenceImageSize;
	private double[] referenceVoxelSize;
	private int indexOfReference;
	ImagePlus globalReference;
	/**
	 *  Test sequence for the class
	 */
	public static void main(String[] args) {
		Vitimage4D viti = new Vitimage4D(VineType.CUTTING,0);		
		viti.addAcquisition(AcquisitionType.MRI_T1_SEQ,VitiDialogs.chooseDirectoryUI("Choose a directory for the T1 sequence"),
							Geometry.REFERENCE,
							Misalignment.NONE,Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED);
		
		viti.addAcquisition(AcquisitionType.MRI_T2_SEQ,VitiDialogs.chooseDirectoryUI("Choose a directory for the T2 sequence"),
							Geometry.QUASI_REFERENCE,
							Misalignment.LIGHT_RIGID,Capillary.HAS_CAPILLARY,SupervisionLevel.GET_INFORMED);
		
	/*	viti.addAcquisition(AcquisitionType.MRI_GE3D_SEQ,VitiDialogs.chooseDirectoryUI("Choose a directory for the Ge3D sequence"),
							Geometry.REFERENCE,
							Misalignment.VOXEL_SCALE_FACTOR,Capillary.HAS_CAPILLARY);
		
		viti.addAcquisition(AcquisitionType.RX,VitiDialogs.chooseDirectoryUI("Choose a directory for the RX sequence"),
							Geometry.SWITCH_XY,
							Misalignment.VOXEL_SCALE_FACTOR,Capillary.HAS_CAPILLARY);
		*/
		viti.readAllDataFromSource();
		viti.computeAll();
		ImagePlus result=viti.createNormalizedHyperImage();
		result.show();
	}

	
	
	public Vitimage4D(VineType vineType, int dayAfterExperience) {
		this.dayAfterExperience=dayAfterExperience;
		this.vineType=vineType;
		this.indexOfReference=VitimageUtils.ERROR_VALUE;
		acquisition=new ArrayList<Acquisition>();//Observations goes there
		geometry=new ArrayList<Geometry>();
		misalignment=new ArrayList<Misalignment>();
		capillary=new ArrayList<Capillary> ();
		imageForRegistration=new ArrayList<ImagePlus> ();		
	}
	
	
	public void addAcquisition(Acquisition.AcquisitionType acq, String path,Geometry geom,Misalignment mis,Capillary cap,SupervisionLevel sup){
		switch(acq) {
		case MRI_T1_SEQ: this.acquisition.add(new MRI_T1_Seq(path,cap,sup));break;
		case MRI_T2_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case MRI_DIFF_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case MRI_FLIPFLOP_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case MRI_SE_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case MRI_GE3D_SEQ: VitiDialogs.notYet("FlipFlop");break;
		case RX: VitiDialogs.notYet("FlipFlop");break;
		case HISTOLOGY: VitiDialogs.notYet("FlipFlop");break;
		case PHOTOGRAPH: VitiDialogs.notYet("FlipFlop");break;
		case TERAHERTZ: VitiDialogs.notYet("FlipFlop");break;
		}
		this.geometry.add(geom);
		this.misalignment.add(mis);
		this.capillary.add(cap);
		if(geom==Geometry.REFERENCE) {
			this.indexOfReference=geometry.size()-1;
			this.referenceImageSize=new int[] {this.acquisition.get(this.indexOfReference).dimX(),
					this.acquisition.get(this.indexOfReference).dimY(),
					this.acquisition.get(this.indexOfReference).dimZ()};
			this.referenceVoxelSize=new double[] {this.acquisition.get(this.indexOfReference).voxSX(),
					this.acquisition.get(this.indexOfReference).voxSY(),
					this.acquisition.get(this.indexOfReference).voxSZ()};
		}
	}
	
	
	public void readAllDataFromSource() {
		for (Acquisition acq : acquisition) {
			acq.readDataFromFile();
			acq.setImageForRegistration();
		}
	}
	
	public void computeAll() {
		if(this.indexOfReference==VitimageUtils.ERROR_VALUE) {IJ.showMessage("Reference image not set. Cannot compute registration. Abort.");return;}
		for(Acquisition acq:acquisition) {
			acq.readDataFromFile();
			acq.processData();
		}
		handleMajorMisalignments();
		centerAxes();
		if(vineType==VineType.CUTTING) detectInoculationPoints();
		registerAllImages();

	}
	
	
	public void handleMajorMisalignments() {
		for(int i=0;i<acquisition.size();i++) {
			if(geometry.get(i)==Geometry.REFERENCE) continue;
			if(geometry.get(i)==Geometry.UNKNOWN) {}
			switch(geometry.get(i)){
			case REFERENCE: 	
				break;
			case UNKNOWN: 
				VitiDialogs.notYet("Geometry.UNKNOWN");
				break;
			case MIRROR_X:
				VitiDialogs.notYet("Geometry.MIRROR_X");
				break;
			case MIRROR_Y:
				VitiDialogs.notYet("Geometry.MIRROR_Y");
				break;
			case MIRROR_Z:
				VitiDialogs.notYet("Geometry.MIRROR_Z");
				break;
			case SWITCH_XY:
				VitiDialogs.notYet("Geometry.SWITCH_XY");
				break;
			case SWITCH_XZ:
				VitiDialogs.notYet("Geometry.SWITCH_XZ");
				break;
			case SWITCH_YZ:
				VitiDialogs.notYet("Geometry.SWITCH_YZ");
				break;
			}
			acquisition.get(i).setImageForRegistration();			
		}
	}
	
	public void centerAxes() {
		for(Acquisition acq : acquisition) {
			Point3d[]pInit=new Point3d[3];
			Point3d[]pFin=new Point3d[3];
			//    pFin=detectAxis(acq);//in the order : center of object along its axis , center + daxis , center + dvect Orthogonal to axis 

					
			pInit[0]=new Point3d(acq.voxSX()*acq.dimX()/2.0,acq.voxSY()* acq.dimY()/2.0, acq.voxSZ()*acq.dimZ()/2.0);//origine
			pInit[1]=new Point3d(acq.voxSX()*acq.dimX()/2.0,acq.voxSY()* acq.dimY()/2.0, 1 + acq.voxSZ()*acq.dimZ()/2.0);//origine + dZ
			pInit[2]=new Point3d(acq.voxSX()*acq.dimX()/2.0, 1 + acq.voxSY()* acq.dimY()/2.0, acq.voxSZ()*acq.dimZ()/2.0);//origine + dY

			ItkTransform trAdd=ItkTransform.estimateBestRigid3D(pInit, pFin);
			acq.addTransform(trAdd);
		}
	}
	
	public void detectInoculationPoints(){
		for(Acquisition acq : acquisition) {
			Point3d[]pFin=VitimageUtils.detectInoculationPoint(acq);
			
			//Compute and store the inoculation Point, in the coordinates of the original image
			Point3d inoculationPoint=ItkImagePlusInterface.vectorDoubleToPoint3d(
					acq.getTransform().transformPoint(ItkImagePlusInterface.doubleArrayToVectorDouble(
							new double[] {pFin[3].x,pFin[3].y,pFin[3].z})));
			acq.setInoculationPoint(inoculationPoint);

			
			//Compute the transformation that align the inoculation point to the Y+, with the axis already aligned
			Point3d[]pInit=new Point3d[4];
			pInit[0]=new Point3d(acq.voxSX()*acq.dimX()/2.0,acq.voxSY()* acq.dimY()/2.0, acq.voxSZ()*acq.dimZ()/2.0);
			pInit[1]=new Point3d(pInit[0].x , pInit[0].y , pInit[0].z + 1 );
			pInit[2]=new Point3d(pInit[0].x , pInit[0].y + 1 , pInit[0].z);

			Point3d[] pFinTrans=new Point3d[] { pFin[0] , pFin[1] , pFin[2] };
			ItkTransform trAdd=ItkTransform.estimateBestRigid3D(pInit, pFin);
			acq.addTransform(trAdd);
			acq.setImageForRegistration();				
		}
	}
	

	
	public void registerAllImages() {
		for (int i=0;i<this.acquisition.size();i++) {
			if(i==this.indexOfReference)continue;
			ItkRegistrationManager manager=new ItkRegistrationManager();
			manager.runScenarioInterModal(this.acquisition.get(this.indexOfReference),this.acquisition.get(i));
		}
	}
	
	public ImagePlus createNormalizedHyperImage() {
		ArrayList<ImagePlus> imgList=new ArrayList<ImagePlus>();
		for(Acquisition acq : acquisition) imgList.add(acq.computeNormalizedHyperImage());
		ImagePlus[]tabRet=new ImagePlus[imgList.size()];
		for(int i=0;i<imgList.size() ;i++) tabRet[i]=imgList.get(i);
		ImagePlus ret=Concatenator.run(tabRet);
		return ret;
	}


}
