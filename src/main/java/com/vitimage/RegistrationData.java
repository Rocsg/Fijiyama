package com.vitimage;

import java.util.ArrayList;
import ij.ImagePlus;

public class RegistrationData {
	private String[][]paths;
	private int[][][]initDimensions;
	private int[][][]dimensions;
	private double[][][]imageRanges;
	private double[][]imageSizes;//In Mvoxels
	private double[][][]initVoxs;
	private double[][][]voxs;

	private final double EPSILON=1E-8;
	private String unit="mm";
	private int[]imageParameters=new int[] {512,512,40};
	public int maxAcceptableLevel=3;

	public int nTimes;
	public int nMods;
	ImagePlus a;
	public ArrayList<ItkTransform>[][]transforms;
	public ImagePlus[][]images;
	
	
	
	private int maxImageSizeForRegistration=32;//In Mvoxels
	private String serieOutputPath;
	private String serieFjmFile;
	private String serieInputPath;
	private String[] times;
	private String[] mods;
	private String expression;
	private String name;

	private boolean memorySavingMode=false;
	
	
	
	public RegistrationData() {
	}
	
	public RegistrationData(int nTimes,int nData) {
		this.images=new ImagePlus[this.nTimes][this.nMods];
		this.transforms=(ArrayList<ItkTransform>[][])new ArrayList[this.nTimes][this.nMods];
		for(int nt=0;nt<this.nTimes;nt++)		for(int nm=0;nm<this.nMods;nm++)	this.transforms[nt][nm]=new ArrayList<ItkTransform>();
		this.paths=new String[this.nTimes][this.nMods];
		this.initDimensions=new int[this.nTimes][nMods][5];
		this.dimensions=new int[this.nTimes][this.nMods][3];
		this.initVoxs=new double[this.nTimes][this.nMods][3];
		this.imageSizes=new double[this.nTimes][this.nMods];
		this.imageRanges=new double[this.nTimes][this.nMods][2];
		this.voxs=new double[this.nTimes][this.nMods][3];	
	}
	
	
	

}
