package com.vitimage;

import java.util.Date;

import ij.ImagePlus;

public abstract class Acquisition {
	public Acquisition(int typeAcquisition,String sourcePath) {		
		this.typeAcquisition=typeAcquisition;
		this.sourcePath=sourcePath;
	}

	
	public final void setDimensions(int dimX,int dimY, int dimZ) {
		this.dimX=dimX;
		this.dimY=dimY;
		this.dimZ=dimZ;		
	}

	public final void setVoxelSizes(double voxSX,double voxSY,double voxSZ) {
		this.voxSX=voxSX;
		this.voxSY=voxSY;
		this.voxSZ=voxSZ;		
	}

	public final void setSpacing(double spacingX,double spacingY,double spacingZ) {
		this.spacingX=spacingX;
		this.spacingY=spacingY;
		this.spacingZ=spacingZ;		
	}

	
	/**
	 * @return the dimX
	 */
	public int getDimX() {
		return dimX;
	}


	/**
	 * @param dimX the dimX to set
	 */
	public void setDimX(int dimX) {
		this.dimX = dimX;
	}


	/**
	 * @return the dimY
	 */
	public int getDimY() {
		return dimY;
	}


	/**
	 * @param dimY the dimY to set
	 */
	public void setDimY(int dimY) {
		this.dimY = dimY;
	}


	/**
	 * @return the dimZ
	 */
	public int getDimZ() {
		return dimZ;
	}


	/**
	 * @param dimZ the dimZ to set
	 */
	public void setDimZ(int dimZ) {
		this.dimZ = dimZ;
	}


	/**
	 * @return the voxSX
	 */
	public double getVoxSX() {
		return voxSX;
	}


	/**
	 * @param voxSX the voxSX to set
	 */
	public void setVoxSX(double voxSX) {
		this.voxSX = voxSX;
	}


	/**
	 * @return the voxSY
	 */
	public double getVoxSY() {
		return voxSY;
	}


	/**
	 * @param voxSY the voxSY to set
	 */
	public void setVoxSY(double voxSY) {
		this.voxSY = voxSY;
	}


	/**
	 * @return the voxSZ
	 */
	public double getVoxSZ() {
		return voxSZ;
	}


	/**
	 * @param voxSZ the voxSZ to set
	 */
	public void setVoxSZ(double voxSZ) {
		this.voxSZ = voxSZ;
	}


	/**
	 * @return the spacingX
	 */
	public double getSpacingX() {
		return spacingX;
	}


	/**
	 * @param spacingX the spacingX to set
	 */
	public void setSpacingX(double spacingX) {
		this.spacingX = spacingX;
	}


	/**
	 * @return the spacingY
	 */
	public double getSpacingY() {
		return spacingY;
	}


	/**
	 * @param spacingY the spacingY to set
	 */
	public void setSpacingY(double spacingY) {
		this.spacingY = spacingY;
	}


	/**
	 * @return the spacingZ
	 */
	public double getSpacingZ() {
		return spacingZ;
	}


	/**
	 * @param spacingZ the spacingZ to set
	 */
	public void setSpacingZ(double spacingZ) {
		this.spacingZ = spacingZ;
	}


	
	
	/**
	 * @return the sourceData
	 */
	public ImagePlus[] getSourceData() {
		return sourceData;
	}


	/**
	 * @param sourceData the sourceData to set
	 */
	public void setSourceData(ImagePlus[] sourceData) {
		this.sourceData = sourceData;
	}


	/**
	 * @return the operator
	 */
	public String getOperator() {
		return operator;
	}


	/**
	 * @param operator the operator to set
	 */
	public void setOperator(String operator) {
		this.operator = operator;
	}


	/**
	 * @return the machineBrand
	 */
	public String getMachineBrand() {
		return machineBrand;
	}


	/**
	 * @param machineBrand the machineBrand to set
	 */
	public void setMachineBrand(String machineBrand) {
		this.machineBrand = machineBrand;
	}


	/**
	 * @return the acquisitionDate
	 */
	public Date getAcquisitionDate() {
		return acquisitionDate;
	}


	/**
	 * @param acquisitionDate the acquisitionDate to set
	 */
	public void setAcquisitionDate(Date acquisitionDate) {
		this.acquisitionDate = acquisitionDate;
	}


	/**
	 * @return the acquisitionPlace
	 */
	public String getAcquisitionPlace() {
		return acquisitionPlace;
	}


	/**
	 * @param acquisitionPlace the acquisitionPlace to set
	 */
	public void setAcquisitionPlace(String acquisitionPlace) {
		this.acquisitionPlace = acquisitionPlace;
	}


	/**
	 * @return the sourcePath
	 */
	public String getSourcePath() {
		return sourcePath;
	}


	/**
	 * @return the typeAcquisition
	 */
	public int getTypeAcquisition() {
		return typeAcquisition;
	}

	public abstract void readDataFromFile();
	public abstract void processData();
	private ImagePlus[]sourceData;
	private ImagePlus[]computedData;
	/**
	 * @return the computedData
	 */
	public ImagePlus[] getComputedData() {
		return computedData;
	}


	/**
	 * @param computedData the computedData to set
	 */
	public void setComputedData(ImagePlus[] computedData) {
		this.computedData = computedData;
	}
	private final String sourcePath;
	private final int typeAcquisition;
	private String operator;
	private String machineBrand;
	private Date acquisitionDate;
	private String acquisitionPlace;
	private int dimX;
	private int dimY;
	private int dimZ;
	private double voxSX;
	private double voxSY;
	private double voxSZ;
	private double spacingX;
	private double spacingY;
	private double spacingZ;
	public static final int MRI_SE_SEQ=0;
	public static final int MRI_T1_SEQ=1;
	public static final int MRI_T2_SEQ=2;
	public static final int MRI_GE3D_SEQ=3;
	public static final int MRI_DIFF_SEQ=4;
	public static final int MRI_FLIP_FLOP_SEQ=5;
	public static final int RX=6;
	public static final int HISTO=7;
	public static final int TERAHERTZ=8;
}
