package com.vitimage;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;

import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Euler3DTransform;
import org.itk.simple.Image;
import org.itk.simple.Similarity3DTransform;
import org.itk.simple.VectorDouble;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import math3d.Point3d;

public class Experience_NN_IP {

	
	public static void main (String[]args) {
		ImageJ ij=new ImageJ();
		step4(1);
		//testTorture();
//		step3(1);
//		checkStep2();	
		
	}

	public static void checkStep2(int res) {		
		String outRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_2_to_nn_geometry/";
		String[]strSpec= new String[]{"B001_PAL","B031_NP","B032_NP","B051_CT","B041_DS","B042_DS"};
		String[]strDays= new String[] { "J0" ,"J35", "J70", "J133", "J218"};
		for(String spec : strSpec) {
			for(String day : strDays) {
				System.out.println("\nVerification "+spec+"_"+day);
				ImagePlus img=IJ.openImage(outRep+spec+"_"+day+(res==0 ? "" : "_res1")+".tif");
				VitimageUtils.imageChecking(img);
			}
		}
	}
	
	public static Point3d readCoordinates(String outName,String subRep){
		String outRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/"+subRep;
		File f = new File(outRep,"coordinates_"+outName+".txt");
		Point3d coords=new Point3d();
		try {
			BufferedReader in=new BufferedReader(new FileReader(f));
			coords.x=Double.parseDouble(in.readLine());
			coords.y=Double.parseDouble(in.readLine());
			coords.z=Double.parseDouble(in.readLine());
		} catch (Exception e) {IJ.error("Unable to write augmented file: "+f.getAbsolutePath()+"error: "+e);}
		return coords;
	}
	
	
	public static void writeCoordinates(Point3d coordinates,String outName,String subRep){
		String outRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/"+subRep;
		File f = new File(outRep,"coordinates_"+outName+".txt");
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			out.write(""+coordinates.x);
			out.write("\n"+coordinates.y);
			out.write("\n"+coordinates.z);
			out.close();
		} catch (Exception e) {IJ.error("Unable to write file: "+f.getAbsolutePath()+"error: "+e);}
	}
	
	
	//Cette etape permet d'expertiser les donnees, afin de fournir les coordonnees initiales du point d inoculation
	public static void step3(int res) {
		String sourceRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_2_to_nn_geometry/";
		String[]strSpec= new String[]{"B001_PAL","B031_NP","B032_NP","B051_CT","B041_DS","B042_DS"};//"B001_PAL",
		String[]strDays= new String[] { "J0" ,"J35", "J70", "J133", "J218"};
		for(String spec : strSpec) {
			for(String day : strDays) {
				System.out.println("\nExpertise "+spec+"_"+day);
				ImagePlus img=IJ.openImage(sourceRep+spec+"_"+day+(res==0 ? "" : "_res1")+".tif");
				int []dims=VitimageUtils.getDimensions(img);
				double []voxs=VitimageUtils.getVoxelSizes(img);
				double []realSize=new double[] {dims[0]*voxs[0]  , dims[1]*voxs[1]  , dims[2]*voxs[2] };

				
				img.show();
				IJ.run("Fire","");
				img.getWindow().setSize(640,640);
				img.getCanvas().fitToWindow();
				double[][] tabCoords=VitiDialogs.waitForPointsUI(2, img, true);
				img.close();
				Point3d coordsReal=new Point3d (0.5*tabCoords[0][0]+0.5*tabCoords[1][0], 0.5*tabCoords[0][1]+0.5*tabCoords[1][1]  , 0.5*tabCoords[0][2]+0.5*tabCoords[1][2]);
				Point3d coordsPython=new Point3d(coordsReal.x / realSize[0] , coordsReal.y / realSize[1]  , coordsReal.z / realSize[2]);
				writeCoordinates(coordsReal,""+spec+"_"+day+(res==0 ? "" : "_res1")+"","data_2_to_nn_geometry");
				writeCoordinates(coordsPython,"normalized_"+spec+"_"+day+(res==0 ? "" : "_res1")+"","data_2_to_nn_geometry");
			}
		}
	}

		
		
	
	
	//Cette etape fait l'augmentation de données
	public static void step4(int res) {
		String sourceRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_2_to_nn_geometry/";
		String outRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_3_augmented/";
		String[]strSpec= new String[]{"B001_PAL","B031_NP","B032_NP","B051_CT","B041_DS","B042_DS"};
		String[]strDays= new String[] { "J0" ,"J35", "J70", "J133", "J218"};
		ImagePlus img=IJ.openImage(sourceRep+strSpec[0]+"_"+strDays[0]+(res==0 ? "" : "_res1")+".tif");
		double []voxs=VitimageUtils.getVoxelSizes(img);
		int []dims=VitimageUtils.getDimensions(img);
		double[]center=new double[] {voxs[0]*dims[0]/2.0,voxs[1]*dims[1]/2.0,voxs[2]*dims[2]/2.0};
		VectorDouble centerVect=ItkImagePlusInterface.doubleArrayToVectorDouble(center);

		//parametres rig
		double []varRigTX={0.02,0.07,0.2};
		double []varRigTY={0.02,0.07,0.2};
		double []varRigTZ={0.02,0.08,0.3};
		double []varRigTetaZ={0.06,0.6,6.28};
		double []varRigTetaX={0.001,0.005,0.01};
		double []varRigTetaY= {0.001,0.005,0.01};

		//parametres illumination
		double []varBias= {0.01,0.02,0.05};
		double []varLum= {0.001,0.02,0.5};

		
		//parametres dense
		double sigmaField=0.4;
		double []varFieldX= {0,0.05,0.1};
		double []varFieldY= {0,0.05,0.1};
		double []varFieldZ= {0,0.05,0.1};
		
		//parametres similarity
		double []varSim= {0.01,0.05,0.12};

		//parametres noise
		double []varNoise= {0.001,0.03,0.5};
		for(String spec : strSpec) {
			for(String day : strDays) {
	
				img=IJ.openImage(sourceRep+spec+"_"+day+"_res1.tif");
				System.out.println("\n\n\nTraitement "+spec+"_"+day);
				Point3d coords=readCoordinates(""+spec+"_"+day+(res==0 ? "" : "_res1")+"","data_2_to_nn_geometry");
				System.out.println("Coordonnees initiales="+coords);
				//img.show();
				for(int iRig=0;iRig<3;iRig++) {
					for(int iLig=0;iLig<3;iLig++) {
						for(int iDen=0;iDen<3;iDen++) {
							for(int iSim=0;iSim<3;iSim++) {
								for(int iNoi=0;iNoi<3;iNoi++) {
									System.out.print("  "+spec+"_"+day+" "+iRig+" "+iLig+" "+iDen+" "+iSim+" "+iNoi+"...   ");
									double rigTX=(Math.random()-0.5)*varRigTX[iRig];
									double rigTY=(Math.random()-0.5)*varRigTY[iRig];
									double rigTZ=(Math.random()-0.5)*varRigTZ[iRig];
									double rigTetaX=(Math.random()-0.5)*varRigTetaX[iRig];
									double rigTetaY=(Math.random()-0.5)*varRigTetaY[iRig];
									double rigTetaZ=(Math.random()-0.5)*varRigTetaZ[iRig];

									double bias=(Math.random()-0.5)*varBias[iLig];
									double lum=(Math.random()-0.5)*varLum[iLig];
									
									double fieldX=(Math.random()-0.5)*varFieldX[iDen];
									double fieldY=(Math.random()-0.5)*varFieldY[iDen];
									double fieldZ=(Math.random()-0.5)*varFieldZ[iDen];
									
									double sim=(Math.random()-0.5)*varSim[iSim];
									
									double noise=(Math.random()-0.5)*varNoise[iNoi];

									Object[]imgCoord=torturerImage(img,rigTX,rigTY,rigTZ,rigTetaX,rigTetaY,rigTetaZ,bias,lum,fieldX,fieldY,fieldZ,sim,noise,centerVect,sigmaField,coords);
									ImagePlus img2=(ImagePlus) imgCoord[0];
									Point3d tabCoords=(Point3d) imgCoord[1];
									//System.out.println("Coordonnees augmentees="+tabCoords);
//									VitimageUtils.imageChecking(img2);
									String outName=spec+"_"+day+(res==0 ? "" : "_res1")+""+"_aug__Rig"+iRig+"_Lig"+iLig+"_Den"+iDen+"_Sim"+iSim+"_Noi"+iNoi;
									IJ.saveAsTiff(img2,outRep+outName+".tif");	
									Point3d coordsReal=tabCoords;
									Point3d coordsPython=new Point3d(coordsReal.x / (2*center[0]) , coordsReal.y / (2*center[1]) , coordsReal.z / (2*center[2]));
									writeCoordinates(coordsReal,outName,"data_3_augmented");
									writeCoordinates(coordsPython,"normalized_"+outName,"data_3_augmented");
									System.out.println();
								}
							}
						}
					}
				}
			}
			img.close();
		}
	}
			
	public static void testTorture() {
		ImagePlus img=IJ.openImage("/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_2_to_nn_geometry/B001_PAL_J70.tif");
		img.show();
		ImagePlus img2=(ImagePlus) torturerImage(img,
				0,0,0   ,0,0,0         ,0,0       ,0,0,0      ,0,      1,       
				ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {4.5,4.5,5}),0.4,new Point3d(0,0,0))[0];
		img2.show();
	}
		
	
	public static Object []torturerImage(ImagePlus img,double rigTX,double rigTY,double rigTZ,double rigTetaX,double rigTetaY,double rigTetaZ,
											double bias,double lum,double fieldX,double fieldY,double fieldZ,double sim,double noise,VectorDouble centerVect,double sigmaField,Point3d coords) {
		//Add similarity
		System.out.print("sim...");
		ItkTransform globTrans=new ItkTransform();
		Similarity3DTransform simTrans=new Similarity3DTransform();
		simTrans.setScale(1+sim);
		simTrans.setCenter(centerVect);
		globTrans.addTransform(simTrans);
	
		//Add rigid part
		System.out.print("rig...");
		Euler3DTransform eulTrans=new Euler3DTransform();
		eulTrans.setCenter(centerVect);
		eulTrans.setRotation(rigTetaX,rigTetaY, rigTetaZ);
		eulTrans.setTranslation(ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {rigTX,rigTY, rigTZ}));
		globTrans.addTransform(eulTrans);
		
		//Add dense part
		//System.out.print("den...");
		//Point3d[][]correspondancePoints=buildCorrespondanceTab(img,sigmaField,fieldX,fieldY,fieldZ,centerVect);
		//globTrans.addTransform(new ItkTransform(new DisplacementFieldTransform( ItkTransform.computeDenseFieldFromSparseCorrespondancePoints(correspondancePoints,img,sigmaField))));
		System.out.print("trans...");
		ImagePlus img2=globTrans.transformImage(img, img,false);

		
		
		//Add illumination part
		System.out.print("lig...");
		IJ.run(img2, "Add...", "value="+bias+" stack");
		IJ.run(img2, "Multiply...", "value="+(1+lum)+" stack");

		
		//Add noise part
		System.out.print("noi...");
		for(int i=0;i<img2.getStackSize();i++)img2.getStack().getProcessor(i+1).noise(noise);
		
		Point3d coordsAug=globTrans.transformPointInverse(coords);
		return new Object[]{img2,coordsAug};
	}
	
	
	public static Point3d[][]buildCorrespondanceTab(ImagePlus img,double sigmaField, double fieldX, double fieldY, double fieldZ,VectorDouble centerVect){
		double[]centers=ItkImagePlusInterface.vectorDoubleToDoubleArray(centerVect);
		int nbX=((int)Math.round(2*centers[0]/sigmaField));
		int nbY=((int)Math.round(2*centers[1]/sigmaField));
		int nbZ=((int)Math.round(2*centers[2]/sigmaField));
		Point3d[][]out=new Point3d[2][nbX*nbY*nbZ];
		int item=-1;
		for(int x=0;x<nbX;x++) {
			for(int y=0;y<nbY;y++) {
				for(int z=0;z<nbZ;z++) {
					item++;
					double fiX=(Math.random()-0.5)*fieldX;
					double fiY=(Math.random()-0.5)*fieldY;
					double fiZ=(Math.random()-0.5)*fieldZ;
					out[0][item]=new Point3d(sigmaField*x , sigmaField*y, sigmaField*z);
					out[1][item]=new Point3d(sigmaField*x + fiX, sigmaField*y + fiY, sigmaField*z + fiZ);
				}
			}
		}
		return out;
	}
	
	
	
	//Cette etape met a la taille des reseaux (64 x 64 x 64
	public static void step2(int res){
		String sourceRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_1_aligned/";
		String outRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_2_to_nn_geometry/";
		String[]strSpec= new String[]{"B001_PAL","B031_NP","B032_NP","B051_CT","B041_DS","B042_DS"};
		String[]strDays= new String[] { "J0" ,"J35", "J70", "J133", "J218"};
		ImagePlus img=IJ.openImage(sourceRep+strSpec[0]+"_"+strDays[0]+".tif");
		double []voxs=VitimageUtils.getVoxelSizes(img);
		int []dims=VitimageUtils.getDimensions(img);
		int []dimsEnd;
		if(res==0)dimsEnd=new int[] {128,128,128 };
		else dimsEnd=new int[] {128,128,128 };
		for(int i=0;i<3;i++)voxs[i]*= (1.0*dims[i]/(1.0*dimsEnd[i]));
		ImagePlus imgRef=ij.gui.NewImage.createImage("Mask",dimsEnd[0],dimsEnd[1],dimsEnd[2],32,ij.gui.NewImage.FILL_WHITE);
		VitimageUtils.adjustImageCalibration(imgRef, voxs, "mm");
		ItkTransform identity=new ItkTransform();
		
		for(String spec : strSpec) {
			for(String day : strDays) {
				
				System.out.println("\nConstruction "+spec+"_"+day);
				//Construire l'equivalent subsample en 64x64x64
				img=IJ.openImage(sourceRep+spec+"_"+day+".tif");
				img=identity.transformImage(imgRef, img,false);
				if(res==0) {
					img=VitimageUtils.cropImageShort(img,dimsEnd[0]/6,dimsEnd[1]/6,dimsEnd[2]/6,2*dimsEnd[0]/3,2*dimsEnd[1]/3,2*dimsEnd[2]/3);
					IJ.saveAsTiff(img,outRep+spec+"_"+day+".tif");
				}
				else {
					img=VitimageUtils.cropImageShort(img,dimsEnd[0]/4,dimsEnd[1]/4,dimsEnd[2]/4,2*dimsEnd[0]/4,2*dimsEnd[1]/4,2*dimsEnd[2]/4);
					IJ.saveAsTiff(img,outRep+spec+"_"+day+"_res1.tif");
				}
			}
		}
	}
	
	
	
	
	
	
	
	public static void checkStep1() {		
		String outRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_1_aligned/";
		String[]strSpec= new String[]{"B001_PAL","B031_NP","B032_NP","B051_CT","B041_DS","B042_DS"};
		String[]strDays= new String[] { "J0" ,"J35", "J70", "J133", "J218"};
		for(String spec : strSpec) {
			for(String day : strDays) {
				System.out.println("\nVerification "+spec+"_"+day);
				ImagePlus img=IJ.openImage(outRep+spec+"_"+day+".tif");
				VitimageUtils.imageChecking(img);
			}
		}
	}
			
	public static void step1() {
			
		String basisRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_0_clone/";
		String outRep="/mnt/DD_COMMON/Data_VITIMAGE/Train_space_NN/Full_Exp/data_1_aligned/";
		String[]strSpec= new String[]{"B001_PAL","B031_NP","B032_NP","B051_CT","B041_DS","B042_DS"};
		String[]strDays= new String[] { "J0" ,"J35", "J70", "J133", "J218"};
	
		for(String spec : strSpec) {
			for(String day : strDays) {
				//if(!day.equals("J218"))continue;
				//if(!spec.equals("B041_DS"))continue;
				System.out.println("Traitement "+spec+"_"+day);
				System.out.println("Ouverture a l adresse : "+basisRep+spec+"_"+day+".tif");
				//Ouverture image
				ImagePlus img=IJ.openImage(basisRep+spec+"_"+day+".tif");
				
	
				//mesure valeur capillaire
				int []coordinates=detectCapillaryPositionImg(20,img);
				double normalisationFactor=getCapillaryValue(img,coordinates,4,4);
				
				
	
				//normalisation
				IJ.run(img, "32-bit", "");
				IJ.run(img, "Divide...", "value="+normalisationFactor+" stack");
				
				//detection axe
				int[]dims=VitimageUtils.getDimensions(img);
				double[]voxs=VitimageUtils.getVoxelSizes(img);
				double refCenterX=dims[0]*voxs[0]/2.0;
				double refCenterY=dims[1]*voxs[1]/2.0;
				double refCenterZ=dims[2]*voxs[2]/2.0;
				Point3d[]pInit=new Point3d[3];
				Point3d[]pFin=VitimageUtils.detectAxisIrmT1(img,15);//in the order : center of object along its axis , center + daxis , center + dvect Orthogonal to axis 				
				pInit[0]=new Point3d(refCenterX, refCenterY     , refCenterZ     );//origine
				pInit[1]=new Point3d(refCenterX, refCenterY     , 1 + refCenterZ );//origine + dZ
				pInit[2]=new Point3d(1 + refCenterX, refCenterY , refCenterZ     );//origine + dY
				System.out.println("Image local basis 0 / 0Z / 0Y : \n"+pFin[0]+"\n"+pFin[1]+"\n"+pFin[2]+"");
				ItkTransform trAdd=ItkTransform.estimateBestRigid3D(pFin, pInit);
				System.out.println("Debug");
				System.out.println("Points init :"+pInit[0]+" , "+pInit[1]+" , "+pInit[2]+" , ");
				System.out.println("Points fin :"+pFin[0]+" , "+pFin[1]+" , "+pFin[2]+" , ");
	
				
			
				//resampling
				img=trAdd.transformImage(img,img,false);
				VitimageUtils.imageChecking(img);
				IJ.saveAsTiff(img, outRep+spec+"_"+day+".tif");
			}
		}
	}
	
	
	
	
	
	public static double getCapillaryValue(ImagePlus img, int[]coordinates,int rayXY,int rayZ) {
		int zMin=coordinates[2]-rayZ;
		int zMax=coordinates[2]+rayZ;
		if(zMin<0)zMin=0;
		if(zMax>=img.getStackSize())zMax=img.getStackSize()-1;
		double val=0;
		int nbSlices=0;
		for(int z=zMin;z<=zMax;z++) {
			val+=VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],z,rayXY);
			nbSlices++;
		}
		return (val/nbSlices);
	}

	
	public static int[] detectCapillaryPositionImg(int Z,ImagePlus img2) {
		ImagePlus img=new Duplicator().run(img2);
		RoiManager rm=RoiManager.getRoiManager();
		double valThresh=VitimageUtils.getOtsuThreshold(img2)*0.8;;//standard measured values on echo spin images of bionanoNMRI
		ImagePlus imgSlice=new ImagePlus("", img.getStack().getProcessor(Z));
		//VitimageUtils.imageChecking(imgSlice,"Detect capillary, Before connect group");
		imgSlice=VitimageUtils.gaussianFiltering(imgSlice, 7, 7, 0.1);
		//VitimageUtils.imageChecking(imgSlice,"After gaussian");
		ImagePlus imgCon=VitimageUtils.connexe(imgSlice, valThresh, 10E10, 0, 10E10,4,2,true);
		//VitimageUtils.imageChecking(imgCon,"After selecting the second area up to "+valThresh);
		imgCon.getProcessor().setMinAndMax(0,255);
		IJ.run(imgCon,"8-bit","");
		IJ.setThreshold(imgCon, 255,255);
		rm.reset();
		Roi capArea=new ThresholdToSelection().convert(imgCon.getProcessor());	
		rm.add(imgSlice, capArea, 0);							
		Rectangle rect=capArea.getFloatPolygon().getBounds();
		System.out.println("Capillary position detected is :"+(rect.getX() + rect.getWidth()/2.0)+" , "+
															 (rect.getY() + rect.getHeight()/2.0)+" , "+
														 	 Z);
		
		//Check
		img.getProcessor().resetMinAndMax();
		ImagePlus imgView=new Duplicator().run(img);
		IJ.run(imgView,"8-bit","");
		ImagePlus capCenterImg=VitimageUtils.drawCircleInImage(imgView, 0.7, (int) (rect.getX() + rect.getWidth()/2.0),(int) (rect.getY() + rect.getHeight()/2.0) ,Z,255);		
		ImagePlus res3=VitimageUtils.compositeOf(img,capCenterImg,"Capillary exact position");
		VitimageUtils.imageChecking(res3,18,23,2,"Capillary exact position",3,false);
		res3.close();
		imgView.close();
		
		return new int[] {(int) (rect.getX() + rect.getWidth()/2.0) , (int) (rect.getY() + rect.getHeight()/2.0) , Z , (int)rect.getWidth(),(int)rect.getHeight()};  
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public Experience_NN_IP() {
		// TODO Auto-generated constructor stub
	}

}
