package com.vitimage;

import java.util.ArrayList;

import org.itk.simple.ComposeImageFilter;
import org.itk.simple.DisplacementFieldJacobianDeterminantFilter;
import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.ImageFileReader;
import org.itk.simple.ImageFileWriter;
import org.itk.simple.InterpolatorEnum;
import org.itk.simple.MultiplyImageFilter;
import org.itk.simple.PixelIDValueEnum;
import org.itk.simple.ResampleImageFilter;
import org.itk.simple.SimpleITK;
import org.itk.simple.SimpleITKJNI;
import org.itk.simple.Transform;
import org.itk.simple.VectorDouble;
import org.itk.simple.VectorIndexSelectionCastImageFilter;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.ChannelSplitter;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.GaussianBlur3D;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.StackConverter;
import math3d.Point3d;
import vib.FastMatrix;

public class ItkTransform extends Transform implements ItkImagePlusInterface{

	public static int runTestSequence() {
		int nbFailed=0;
		//test bestRigid, bestAffine, bestSimilityde
		//test transform ij to ITK, then apply to image
		//test fast mat to ITK, then apply to image
		//compare with itk applyed to image
		//test transform image
		return nbFailed;
	}
	
	
	public ItkTransform(){
		super();
	}
	
	public ItkTransform(ItkTransform model) {		
		super(model);
		return;
	}

	public static ItkTransform itkTransformFromCoefs(double[]coefs) {
		org.itk.simple.AffineTransform aff=new org.itk.simple.AffineTransform( ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] { coefs[0] , coefs[1] , coefs[2] ,      coefs[4] , coefs[5] , coefs[6] ,   coefs[8] , coefs[9] , coefs[10] } ),    
				ItkImagePlusInterface.doubleArrayToVectorDouble( new double[] { coefs[3] , coefs[7] , coefs[11] } ), ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {0,0,0} ) );
		return new ItkTransform(aff);
	}
	
	
	public static ItkTransform ijTransformToItkTransform(imagescience.transform.Transform tr) {
		org.itk.simple.AffineTransform aff=new org.itk.simple.AffineTransform(
				ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {tr.get(0,0),tr.get(0,1),tr.get(0,2),  tr.get(1,0),tr.get(1,1),tr.get(1,2),    tr.get(2,0),tr.get(2,1),tr.get(2,2) } ),
				ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {tr.get(0,3),tr.get(1,3),tr.get(2,3)} ),   ItkImagePlusInterface.doubleArrayToVectorDouble(   new double[] {0,0,0}  )  );
		return new ItkTransform(aff);
	}

	public static ItkTransform fastMatrixToItkTransform(FastMatrix fm) {
		double[]tab=fm.rowwise16();
		org.itk.simple.AffineTransform aff=new org.itk.simple.AffineTransform(
				ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {tab[0],tab[1],tab[2],  tab[4],tab[5],tab[6],    tab[8],tab[9],tab[10] } ),
				ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {tab[3],tab[7],tab[11] } ),   ItkImagePlusInterface.doubleArrayToVectorDouble(   new double[] {0,0,0}  )  );
		return new ItkTransform(aff);
	}

	public ItkTransform(org.itk.simple.Transform tr) {
		super(tr);
		return ;
	}

	public static ItkTransform array16ElementsToItkTransform(double[] tab) {
		org.itk.simple.AffineTransform aff=new org.itk.simple.AffineTransform(
				ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {tab[0],tab[1],tab[2], tab[4],tab[5],tab[6],    tab[8],tab[9],tab[10] } ),
				ItkImagePlusInterface.doubleArrayToVectorDouble(new double[] {tab[3],tab[7],tab[11]} ),   ItkImagePlusInterface.doubleArrayToVectorDouble(   new double[] {0,0,0}  )  );
		return new ItkTransform(aff);
	}


	public static ItkTransform estimateBestAffine3D(Point3d[]setRef,Point3d[]setMov) {
		return fastMatrixToItkTransform(FastMatrix.bestLinear( setMov, setRef));
	}
	
	
	
	public static ItkTransform estimateBestRigid3D(Point3d[]setRef,Point3d[]setMov) {
		return fastMatrixToItkTransform(FastMatrix.bestRigid( setMov, setRef, false ));
	}

	public static Point3d centerOfPointSet(Point3d []pIn) {
		double xMoy=0;
		double yMoy=0;
		double zMoy=0;
		Point3d []pOut=new Point3d[pIn.length];
		for(Point3d p : pIn) {
			xMoy+=p.x;
			yMoy+=p.y;
			zMoy+=p.z;
		}
		xMoy/=pIn.length;
		yMoy/=pIn.length;
		zMoy/=pIn.length;
		return new Point3d(xMoy,yMoy,zMoy);
	}

	
	public static Point3d []centerPointSet(Point3d []pIn) {
		double xMoy=0;
		double yMoy=0;
		double zMoy=0;
		Point3d []pOut=new Point3d[pIn.length];
		for(Point3d p : pIn) {
			xMoy+=p.x;
			yMoy+=p.y;
			zMoy+=p.z;
		}
		xMoy/=pIn.length;
		yMoy/=pIn.length;
		zMoy/=pIn.length;
		for(int i=0;i<pIn.length;i++) {
			pOut[i]=new Point3d( pIn[i].x-xMoy, pIn[i].y-yMoy , pIn[i].z-zMoy );
		}		
		return pOut;
	}
	
	public static ItkTransform estimateBestSimilarity3D(Point3d[]setRef,Point3d[]setMov) {
		FastMatrix fm=FastMatrix.bestRigid( setMov, setRef, true );
		System.out.println("Similarity transform computed. Coefficient of dilation : "+Math.pow(fm.det(),-0.333333));
		IJ.log("Similarity transform computed. Coefficient of dilation : "+Math.pow(fm.det(),-0.333333));
		return fastMatrixToItkTransform(fm);
	}

	public static double estimateGlobalDilationFactor(Point3d[]setRef,Point3d[]setMov) {
		FastMatrix fm=FastMatrix.bestRigid( setMov, setRef, true );
		System.out.println("Similarity transform computed. Coefficient of dilation : "+Math.pow(fm.det(),-0.333333));
		return Math.pow(fm.det(),-0.333333);
	}

	
	
	
	public static ItkTransform getRigidTransform(double []center,double[]angles,double[]translation) {
		org.itk.simple.VectorDouble cent=ItkImagePlusInterface.doubleArrayToVectorDouble(center);
		org.itk.simple.VectorDouble trans=ItkImagePlusInterface.doubleArrayToVectorDouble(translation);		
		return new ItkTransform(new org.itk.simple.Transform(new org.itk.simple.Euler3DTransform (cent,angles[0],angles[1],angles[2], trans)));
	}

	
	
	public ImagePlus transformImage(ImagePlus imgRef, ImagePlus imgMov) {
		if(imgMov.getType()==4) {
			ImagePlus[] channels = ChannelSplitter.split(imgMov);
			for(int i=0;i<3;i++) {
				VitimageUtils.adjustImageCalibration(channels[i], imgRef);
				channels[i]=transformImage(imgRef,channels[i]);
			}
			ImagePlus ret=new ImagePlus("",RGBStackMerge.mergeStacks(channels[0].getStack(),channels[1].getStack(),channels[2].getStack(),true));
			VitimageUtils.adjustImageCalibration(ret, imgRef);
			return ret;
		}
		int val=Math.min(  10    ,    Math.min(   imgMov.getWidth()/20    ,   imgMov.getHeight()/20  ));
		int valMean=(int)Math.round(      VitimageUtils.meanValueofImageAround(imgMov,val,val,0,val)*0.5 + VitimageUtils.meanValueofImageAround(imgMov,imgMov.getWidth()-val-1,imgMov.getHeight()-val-1,0,val)*0.5    );
		ResampleImageFilter resampler=new ResampleImageFilter();
		resampler.setDefaultPixelValue(valMean);
		resampler.setReferenceImage(ItkImagePlusInterface.imagePlusToItkImage(imgRef));
		resampler.setTransform(this);
		return (ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgMov))));
	}	

/*	public ItkTransform getTransformImageReech(ImagePlus imgRef, ImagePlus imgMov) {
		double[]voxSRef=VitimageUtils.getVoxelSizes(imgRef);
		double[]voxSMov=VitimageUtils.getVoxelSizes(imgMov);
		double[]voxSReal=new double[] {1,1,1};
		ItkTransform tr=new ItkTransform();
		tr.addTransform(getTransformForResampling(voxSRef,voxSReal));
		tr.addTransform(this);
		tr.addTransform(getTransformForResampling(voxSReal,voxSMov));
		return tr;
	}	

	public ItkTransform getTransformImageReech(double[]voxSRef,double[]voxSMov) {
		double[]voxSReal=new double[] {1,1,1};
		ItkTransform tr=new ItkTransform();
		tr.addTransform(getTransformForResampling(voxSRef,voxSReal));
		tr.addTransform(this);
		tr.addTransform(getTransformForResampling(voxSReal,voxSMov));
		return tr;
	}	


	public ImagePlus transformImageReech(ImagePlus imgRef, ImagePlus imgMov) {
		int val=Math.min(  10    ,    Math.min(   imgMov.getWidth()/20    ,   imgMov.getHeight()/20  ));
		int valMean=(int)Math.round(      VitimageUtils.meanValueofImageAround(imgMov,val,val,0,val)*0.5 + VitimageUtils.meanValueofImageAround(imgMov,imgMov.getWidth()-val-1,imgMov.getHeight()-val-1,0,val)*0.5    );
		ResampleImageFilter resampler=new ResampleImageFilter();
		resampler.setDefaultPixelValue(valMean);
		resampler.setReferenceImage(ItkImagePlusInterface.imagePlusToItkImage(imgRef));
		double[]voxSRef=VitimageUtils.getVoxelSizes(imgRef);
		double[]voxSMov=VitimageUtils.getVoxelSizes(imgMov);
		double[]voxSReal=new double[] {1,1,1};
		ItkTransform tr=new ItkTransform();
		tr.addTransform(getTransformForResampling(voxSReal,voxSRef));
		tr.addTransform(this);
		System.out.println("\nLa transfo initiale etait :"+this.drawableString());
		tr.addTransform(getTransformForResampling(voxSMov,voxSReal));
		System.out.println("\nLa transfo finale est :"+tr.drawableString());
		resampler.setTransform(this);
		return (ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgMov))));
	}	
*/
	public static ImagePlus resampleImageReech(ImagePlus imgRef, ImagePlus imgMov) {
		int val=Math.min(  10    ,    Math.min(   imgMov.getWidth()/20    ,   imgMov.getHeight()/20  ));
		int valMean=(int)Math.round(      VitimageUtils.meanValueofImageAround(imgMov,val,val,0,val)*0.5 + VitimageUtils.meanValueofImageAround(imgMov,imgMov.getWidth()-val-1,imgMov.getHeight()-val-1,0,val)*0.5    );
		ResampleImageFilter resampler=new ResampleImageFilter();
		resampler.setDefaultPixelValue(valMean);
		resampler.setReferenceImage(ItkImagePlusInterface.imagePlusToItkImage(imgRef));
		double[]voxSRef=VitimageUtils.getVoxelSizes(imgRef);
		double[]voxSMov=VitimageUtils.getVoxelSizes(imgMov);
		ItkTransform tr=getTransformForResampling(voxSRef,voxSMov);
		resampler.setTransform(tr);
		return (ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgMov))));
	}	


	
	
	public static ItkTransform getTransformForResampling(double[]voxSRef,double[]voxSMov) {
		double[]mat=new double[] {  1, 0, 0, 0.5*(voxSRef[0]-voxSMov[0]) ,
				0, 1, 0, 0.5*(voxSRef[1]-voxSMov[1]) ,
				0, 0, 1, 0.5*(voxSRef[2]-voxSMov[2]) };
		return ItkTransform.itkTransformFromCoefs(mat);
	}
		
	
	
	public static Point3d[][]trimCorrespondances(Point3d[][] correspondancePoints,ImagePlus imgRef,double sigma,double rejectThreshold){
		System.out.println("Sigma utilisé ="+sigma+" ="+sigma/imgRef.getCalibration().pixelWidth+" voxels");
		double epsilon = 10E-10;
		double epsilonRejection=10E-6;
		double []voxSizes=VitimageUtils.getVoxelSizes(imgRef);
		int []dimensions=VitimageUtils.getDimensions(imgRef);
		int nPt=correspondancePoints[0].length;
		//Construire la liste des pixels concernes, à partir des corespondance points (attention à inverser vox size)
		int [][]vectPoints=new int[nPt][3];
		double [][]vectVals=new double[nPt][3];
		for(int i=0;i<vectPoints.length;i++) {
			vectPoints[i][0]=(int)Math.round(correspondancePoints[0][i].x/voxSizes[0]);
			vectPoints[i][1]=(int)Math.round(correspondancePoints[0][i].y/voxSizes[1]);
			vectPoints[i][2]=(int)Math.round(correspondancePoints[0][i].z/voxSizes[2]);
			vectVals[i][0]=correspondancePoints[1][i].x-correspondancePoints[0][i].x;
			vectVals[i][1]=correspondancePoints[1][i].y-correspondancePoints[0][i].y;
			vectVals[i][2]=correspondancePoints[1][i].z-correspondancePoints[0][i].z;
		}
		
		
		//Construire l'image des poids, et l'image des valeurs suivant X, Y et Z
		ImagePlus imgWeights=IJ.createImage("tempW", dimensions[0], dimensions[1], dimensions[2], 32);
		ImagePlus imgFieldX=IJ.createImage("tempX", dimensions[0], dimensions[1], dimensions[2], 32);
		ImagePlus imgFieldY=IJ.createImage("tempY", dimensions[0], dimensions[1], dimensions[2], 32);
		ImagePlus imgFieldZ=IJ.createImage("tempZ", dimensions[0], dimensions[1], dimensions[2], 32);
		VitimageUtils.adjustImageCalibration(imgWeights, imgRef);
		VitimageUtils.adjustImageCalibration(imgFieldX, imgRef);
		VitimageUtils.adjustImageCalibration(imgFieldY, imgRef);
		VitimageUtils.adjustImageCalibration(imgFieldZ, imgRef);
		
		//Y inserer les informations necessaires
		imgWeights.getProcessor().set(0);
		imgFieldX.getProcessor().set(0);
		imgFieldY.getProcessor().set(0);
		imgFieldZ.getProcessor().set(0);
		
		for(int i=0;i<nPt;i++) {
			imgWeights.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1], 1);
			imgFieldX.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1], (float) vectVals[i][0]);
			imgFieldY.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1], (float) vectVals[i][1]);
			imgFieldZ.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1],(float) vectVals[i][2]);
		}
		
		//Lisser tout le monde
		imgWeights=VitimageUtils.gaussianFiltering(imgWeights, sigma,sigma , sigma);
		imgFieldX=VitimageUtils.gaussianFiltering(imgFieldX, sigma,sigma , sigma);
		imgFieldY=VitimageUtils.gaussianFiltering(imgFieldY, sigma,sigma , sigma);
		imgFieldZ=VitimageUtils.gaussianFiltering(imgFieldZ, sigma,sigma , sigma);
		
		//puis diviser les valeurs suivants X,Y et Z par les poids lissés. Si les poids sont < epsilon, mettre 0
		for(int z=0;z<dimensions[2];z++) {
			float[] valsW=(float [])imgWeights.getStack().getProcessor(z+1).getPixels();
			float[] valsX=(float [])imgFieldX.getStack().getProcessor(z+1).getPixels();
			float[] valsY=(float [])imgFieldY.getStack().getProcessor(z+1).getPixels();
			float[] valsZ=(float [])imgFieldZ.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimensions[0];x++) {
				for(int y=0;y<dimensions[1];y++){
					int index=dimensions[0]*y+x;
					if(valsW[index]<epsilon)valsX[index]=valsY[index]=valsZ[index]=0;
					else{
						valsX[index]/=valsW[index];
						valsY[index]/=valsW[index];
						valsZ[index]/=valsW[index];
					}
				}
			}			
		}
		//Pour chaque point de correspondance, calculer les statistiques locales autour du point, suivant chaque coordonnee X,Y,Z
		int [] flagPoint=new int [nPt];
		int keepedPoints=0;
		for(int i=0;i<nPt;i++) {
			//System.out.println("Trim base sur dense field : evaluation du point "+i+" aux coordonnees ("+vectPoints[i][0]+","+vectPoints[i][1]+","+vectPoints[i][2]+") ");
			double []statsX=VitimageUtils.statistics1D(VitimageUtils.valuesOfImageAround(imgFieldX, vectPoints[i][0],  vectPoints[i][1],  vectPoints[i][2], sigma*40));
			double []statsY=VitimageUtils.statistics1D(VitimageUtils.valuesOfImageAround(imgFieldY, vectPoints[i][0],  vectPoints[i][1],  vectPoints[i][2], sigma*40));
			double []statsZ=VitimageUtils.statistics1D(VitimageUtils.valuesOfImageAround(imgFieldZ, vectPoints[i][0],  vectPoints[i][1],  vectPoints[i][2], sigma*40));
			//System.out.println("Valeurs mesurees : VectPoint=("+vectVals[i][0]+","+vectVals[i][1]+","+vectVals[i][2]+") et StatImgX="+statsX[0]+" +- "+statsX[1]+" , StatImgY="+statsY[0]+" +- "+statsZ[1]+" , StatImgY="+statsZ[0]+" +- "+statsZ[1]);
			if( (vectVals[i][0]>statsX[0]+rejectThreshold*statsX[1]+epsilonRejection || vectVals[i][0]<statsX[0]-rejectThreshold*statsX[1]-epsilonRejection ) ||
				(vectVals[i][1]>statsY[0]+rejectThreshold*statsY[1]+epsilonRejection || vectVals[i][1]<statsY[0]-rejectThreshold*statsY[1]-epsilonRejection ) ||
				(vectVals[i][2]>statsZ[0]+rejectThreshold*statsZ[1]+epsilonRejection || vectVals[i][2]<statsZ[0]-rejectThreshold*statsZ[1]-epsilonRejection ) ) {
				//System.out.println("Point rejected");
				flagPoint [i]=-1;
			}
			else {
				//System.out.println("Point accepted");
				flagPoint[i]=1;
				keepedPoints++;
			}
		}
		Point3d [][]newCorrespondances=new Point3d[2][keepedPoints];
		keepedPoints=0;
		for(int i=0;i<nPt;i++) {
			if(flagPoint[i]>0) {
				newCorrespondances[0][keepedPoints]=new Point3d(correspondancePoints[0][i].x,correspondancePoints[0][i].y,correspondancePoints[0][i].z);
				newCorrespondances[1][keepedPoints]=new Point3d(correspondancePoints[1][i].x,correspondancePoints[1][i].y,correspondancePoints[1][i].z);
				keepedPoints++;
			}
		}
		return newCorrespondances;
	}

	
	
	
	
	public static Image computeDenseFieldFromSparseCorrespondancePoints(Point3d[][]correspondancePoints,ImagePlus imgRef,double sigma,boolean zeroPaddingOutside) {
		double epsilon = 10E-20;
		double []voxSizes=VitimageUtils.getVoxelSizes(imgRef);
		int []dimensions=VitimageUtils.getDimensions(imgRef);
		int nPt=correspondancePoints[0].length;
		System.out.println("Points pour correspondance : "+nPt+" couples.");
		//Construire la liste des pixels concernes, à partir des corespondance points (attention à inverser vox size)
		int [][]vectPoints=new int[nPt][3];
		double [][]vectVals=new double[nPt][3];
		for(int i=0;i<vectPoints.length;i++) {
			vectPoints[i][0]=(int)Math.round(correspondancePoints[0][i].x/voxSizes[0]);
			vectPoints[i][1]=(int)Math.round(correspondancePoints[0][i].y/voxSizes[1]);
			vectPoints[i][2]=(int)Math.round(correspondancePoints[0][i].z/voxSizes[2]);
			vectVals[i][0]=correspondancePoints[1][i].x-correspondancePoints[0][i].x;
			vectVals[i][1]=correspondancePoints[1][i].y-correspondancePoints[0][i].y;
			vectVals[i][2]=correspondancePoints[1][i].z-correspondancePoints[0][i].z;
		}
		

		//Construire l'image des poids, et l'image des valeurs suivant X, Y et Z
		ImagePlus imgWeights=IJ.createImage("tempW", dimensions[0], dimensions[1], dimensions[2], 32);
		ImagePlus imgFieldX=IJ.createImage("tempX", dimensions[0], dimensions[1], dimensions[2], 32);
		ImagePlus imgFieldY=IJ.createImage("tempY", dimensions[0], dimensions[1], dimensions[2], 32);
		ImagePlus imgFieldZ=IJ.createImage("tempZ", dimensions[0], dimensions[1], dimensions[2], 32);
		VitimageUtils.adjustImageCalibration(imgWeights, imgRef);
		VitimageUtils.adjustImageCalibration(imgFieldX, imgRef);
		VitimageUtils.adjustImageCalibration(imgFieldY, imgRef);
		VitimageUtils.adjustImageCalibration(imgFieldZ, imgRef);
		
		//Y inserer les informations necessaires
		imgWeights.getProcessor().set(0);
		imgFieldX.getProcessor().set(0);
		imgFieldY.getProcessor().set(0);
		imgFieldZ.getProcessor().set(0);
		
		for(int i=0;i<nPt;i++) {
			if(   (vectPoints[i][2]+1>0 && vectPoints[i][2]+1<=dimensions[2]) &&
					(vectPoints[i][0]>=0 && vectPoints[i][0]<dimensions[0]) &&
					( vectPoints[i][1]>=0 &&  vectPoints[i][1]<dimensions[1]) ) {
				imgWeights.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1], 1);
				imgFieldX.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1], (float) vectVals[i][0]);
				imgFieldY.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1], (float) vectVals[i][1]);
				imgFieldZ.getStack().getProcessor(vectPoints[i][2]+1).setf(vectPoints[i][0], vectPoints[i][1],(float) vectVals[i][2]);
			}
		}
		
		imgWeights=VitimageUtils.gaussianFilteringIJ(imgWeights, sigma,sigma , (zeroPaddingOutside ? 1 : 5)*sigma);
		imgFieldX=VitimageUtils.gaussianFilteringIJ(imgFieldX, sigma,sigma , (zeroPaddingOutside ? 1 : 5)*sigma);
		imgFieldY=VitimageUtils.gaussianFilteringIJ(imgFieldY, sigma,sigma , (zeroPaddingOutside ? 1 : 5)*sigma);
		imgFieldZ=VitimageUtils.gaussianFilteringIJ(imgFieldZ, sigma,sigma , (zeroPaddingOutside ? 1 : 5)*sigma);
		
		
		
		
		//puis diviser les valeurs suivants X,Y et Z par les poids lissés. Si les poids sont < epsilon, mettre 0
		for(int z=0;z<dimensions[2];z++) {
			float[] valsW=(float [])imgWeights.getStack().getProcessor(z+1).getPixels();
			float[] valsX=(float [])imgFieldX.getStack().getProcessor(z+1).getPixels();
			float[] valsY=(float [])imgFieldY.getStack().getProcessor(z+1).getPixels();
			float[] valsZ=(float [])imgFieldZ.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimensions[0];x++) {
				for(int y=0;y<dimensions[1];y++){
					int index=dimensions[0]*y+x;
					if(valsW[index]<epsilon)valsW[index]=valsX[index]=valsY[index]=valsZ[index]=0;
					else{
						valsX[index]/=valsW[index];
						valsY[index]/=valsW[index];
						valsZ[index]/=valsW[index];
						valsW[index]/=valsW[index];
					}
				}
			}			
		}
		
		if(zeroPaddingOutside) {
			//Creer un nouveau vecteur de modifs
			ArrayList<Point3d> pts0=new ArrayList<Point3d>();
			ArrayList<Point3d> pts1=new ArrayList<Point3d>();
			int nX=(int) Math.ceil(voxSizes[0]/(2*sigma)*dimensions[0]);
			int nY=(int) Math.ceil(voxSizes[1]/(2*sigma)*dimensions[1]);
			int nZ=(int) Math.ceil(voxSizes[2]/(5*sigma)*dimensions[2]);
			System.out.println("Zero padding : ajout de blocks de correspondance : "+nX+" X "+nY+" X "+nZ+" , total="+(nX*nY*nZ));
			int hits=0;
			for(int i=0;i<nPt;i++) {
				pts0.add(correspondancePoints[0][i]);
				pts1.add(correspondancePoints[1][i]);				
				for(int ii=-2;ii<3;ii++) {
					for(int jj=-2;jj<3;jj++) {
						for(int kk=-2;kk<3;kk++) {
							Point3d pt0=new Point3d(correspondancePoints[0][i].x+ii*voxSizes[0], correspondancePoints[0][i].y+jj*voxSizes[1],correspondancePoints[0][i].z+kk*voxSizes[2]);
							Point3d pt1=new Point3d(correspondancePoints[1][i].x+ii*voxSizes[0], correspondancePoints[1][i].y+jj*voxSizes[1],correspondancePoints[1][i].z+kk*voxSizes[2]);
							pts0.add(pt0);
							pts1.add(pt1);	
						}
					}
				}
			}
			for(int z=0;z<nZ-1;z++) {
				int indZ=(int) (Math.round(z*5*sigma)/voxSizes[2]);
				float[] valsW=(float [])imgWeights.getStack().getProcessor(indZ+1).getPixels();
				for(int x=0;x<nX-1;x++) {
					int indX=(int) (Math.round(x*2*sigma)/voxSizes[0]);
					for(int y=0;y<nY-1;y++) {
						int indY=(int) (Math.round(y*2*sigma)/voxSizes[1]);
						if(valsW[dimensions[0]*indY+indX]<epsilon) {
							hits++;
							pts0.add(new Point3d(x*2*sigma,y*2*sigma,z*5*sigma));
							pts1.add(new Point3d(x*2*sigma,y*2*sigma,z*5*sigma));
						}
					}
				}
			}
			System.out.println("Bilan : taille totale = "+(nX*nY*nZ)+" , padding realise = "+hits+" points");
			
			
			Point3d [][]newCorr=new Point3d[2][pts0.size()];
			for(int i=0;i<pts0.size();i++) {
				newCorr[0][i]=pts0.get(i);
				newCorr[1][i]=pts1.get(i);
			}
			return computeDenseFieldFromSparseCorrespondancePoints(newCorr,imgRef,sigma,false);
		}
		
		
		//Construire le champ vectoriel associe et le rendre
	  	return ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(new ImagePlus [] {imgFieldX,imgFieldY,imgFieldZ});

	}
	
	public ImagePlus viewAsGrid3D(ImagePlus imgRef,int pixelSpacing) {
		ImagePlus grid=VitimageUtils.getBinaryGrid(imgRef, pixelSpacing);
		return this.transformImage(imgRef,grid);
	}
	
	public ImagePlus[] showAsCoordinates(String title,int slice) {
		ImagePlus[]coordinates=ItkImagePlusInterface.convertItkTransformToImagePlusArray(this);
		coordinates[0].setTitle(title+".X");
		coordinates[1].setTitle(title+".Y");
		coordinates[2].setTitle(title+".Z");
		for(int i =0;i<3;i++) {
			coordinates[i].show();
			coordinates[i].resetDisplayRange();
			coordinates[i].getWindow().setSize(512, 512);
			coordinates[i].getCanvas().fitToWindow();
			coordinates[i].setSlice(slice);
			IJ.run(coordinates[i],"Fire","");
		}
		return coordinates;
	}
	
	
	public void showAsGrid3D(ImagePlus imgRef,int pixelSpacing,String title,int slice) {
		ImagePlus grid=VitimageUtils.getBinaryGrid(imgRef, pixelSpacing);
		ImagePlus temp=this.transformImage(imgRef,grid);
		temp.setTitle(title);
		temp.show();
		temp.getWindow().setSize(512, 512);
		temp.getCanvas().fitToWindow();
		temp.setSlice(slice);
	}

	
	public static ImagePlus getJacobian(ItkTransform tr,ImagePlus imgRef) {
		System.out.println("A0");
		DisplacementFieldTransform df;
		System.out.println("A1");
		df= new DisplacementFieldTransform((Transform)(tr));
		System.out.println("A2");
		Image im=df.getDisplacementField();
		System.out.println("A3");
		ImagePlus ret=getJacobian(im);
		System.out.println("A4");
		
		
		df.delete();
		System.out.println("A5");
		im.delete();
		System.out.println("A6");
		return ret;
//		return getJacobian(new DisplacementFieldTransform((Transform)(tr.flattenDenseField(imgRef))).getDisplacementField());
	}
	
	public static ImagePlus getJacobian(Image denseField) {
		System.out.println("B1");
		DisplacementFieldJacobianDeterminantFilter df=new  DisplacementFieldJacobianDeterminantFilter();
		Image im=df.execute(new Image(denseField));//Cette ligne produit plus tard le crash
		System.out.println("B2");
		ImagePlus imIJ=null;
		//				ImagePlus imIJ=ItkImagePlusInterface.itkImageToImagePlus(im);
		System.out.println("B3");
		return imIJ;
		//		return (ItkImagePlusInterface.itkImageToImagePlus(new DisplacementFieldJacobianDeterminantFilter().execute(denseField)));		
	}

	
	
	public ImagePlus transformHyperImage4D(ImagePlus hyperRef,ImagePlus hyperMov,int dimension) {
		ImagePlus []imgTabRef=VitimageUtils.stacksFromHyperstack(hyperRef, dimension);
		ImagePlus []imgTabMov=VitimageUtils.stacksFromHyperstack(hyperMov, dimension);
		if(imgTabRef.length != imgTabMov.length)IJ.showMessage("Warning in transformHyperImage4D: tab sizes does not match");
		for(int i=0;i<dimension;i++) {			
			imgTabMov[i]= transformImage(imgTabRef[i], imgTabMov[i]);
		}
		return Concatenator.run(imgTabMov);
	}
	

	public void writeMatrixTransformToFile(String path) {
		SimpleITK.writeTransform(this,path);
	}

	
	public ImagePlus normOfDenseField(ImagePlus imgRef) {
		System.out.println("Compute norm of dense field");
		//Recuperer les dimensions
		int dimX=imgRef.getWidth();
		int dimY=imgRef.getHeight();
		int dimZ=imgRef.getStackSize();
		
		//Construire les futures images hotes pour les dimensions x y et z
		ImagePlus ret;
		double []voxSizes=new double [] {imgRef.getCalibration().pixelWidth , imgRef.getCalibration().pixelHeight, imgRef.getCalibration().pixelDepth};
		ret=IJ.createImage("", dimX, dimY, dimZ, 32);
		ret.getCalibration().setUnit("mm");
		ret.getCalibration().pixelWidth=voxSizes[0];
		ret.getCalibration().pixelHeight=voxSizes[1];
		ret.getCalibration().pixelDepth=voxSizes[2];
	
		
		//Pour chaque voxel, calculer la transformee
		VectorDouble coords=new VectorDouble(3);
		double distance=0;
		VectorDouble coordsTrans=new VectorDouble(3);
		for(int k=0;k<dimZ;k++) {
			System.out.print(" "+((k*100)/dimZ)+" %");
			float[]tab=(float[])ret.getStack().getProcessor(k+1).getPixels();
			
			for(int i=0;i<dimX;i++)for(int j=0;j<dimY;j++) {
				int ind=dimX*j+i;
				coords.set(0,i*voxSizes[0]);
				coords.set(1,j*voxSizes[1]);
				coords.set(2,k*voxSizes[2]);
				coordsTrans=(this.transformPoint(coords));
				distance=Math.sqrt(  (coordsTrans.get(0)-coords.get(0)) * (coordsTrans.get(0)-coords.get(0)) + (coordsTrans.get(1)-coords.get(1)) * (coordsTrans.get(1)-coords.get(1)) + (coordsTrans.get(2)-coords.get(2)) * (coordsTrans.get(2)-coords.get(2)) );
				tab[ind]=(float)(distance);
			}
		}
		ret.resetDisplayRange();
		System.out.println();
		return ret;
	}
	
	
	
	public ItkTransform flattenDenseField(ImagePlus imgRef) {
		System.out.println("Flattening dense field transform");
		//Recuperer les dimensions
		int dimX=imgRef.getWidth();
		int dimY=imgRef.getHeight();
		int dimZ=imgRef.getStackSize();
		
		//Construire les futures images hotes pour les dimensions x y et z
		ImagePlus []ret=new ImagePlus[3];
		double []voxSizes=new double [] {imgRef.getCalibration().pixelWidth , imgRef.getCalibration().pixelHeight, imgRef.getCalibration().pixelDepth};
		for(int i=0;i<3;i++) {
			ret[i]=IJ.createImage("", dimX, dimY, dimZ, 32);
			ret[i].getCalibration().setUnit("mm");
			ret[i].getCalibration().pixelWidth=voxSizes[0];
			ret[i].getCalibration().pixelHeight=voxSizes[1];
			ret[i].getCalibration().pixelDepth=voxSizes[2];
		}
		
		//Pour chaque voxel, calculer la transformee
		VectorDouble coords=new VectorDouble(3);
		VectorDouble coordsTrans=new VectorDouble(3);
		for(int k=0;k<dimZ;k++) {
			if (dimZ<100 || (k%5)==0) System.out.print(" "+((k*100)/dimZ)+" %");
			float[]tabX=(float[])ret[0].getStack().getProcessor(k+1).getPixels();
			float[]tabY=(float[])ret[1].getStack().getProcessor(k+1).getPixels();
			float[]tabZ=(float[])ret[2].getStack().getProcessor(k+1).getPixels();
			
			for(int i=0;i<dimX;i++)for(int j=0;j<dimY;j++) {
				int ind=dimX*j+i;
				coords.set(0,i*voxSizes[0]);
				coords.set(1,j*voxSizes[1]);
				coords.set(2,k*voxSizes[2]);
				coordsTrans=(this.transformPoint(coords));
				tabX[ind]=(float)(coordsTrans.get(0)-coords.get(0));
				tabY[ind]=(float)(coordsTrans.get(1)-coords.get(1));
				tabZ[ind]=(float)(coordsTrans.get(2)-coords.get(2));
			}
		}
		System.out.println();
		return new ItkTransform(new DisplacementFieldTransform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(ret)));
	}
	
	public static ItkTransform getIdentityDenseFieldTransform(ImagePlus imgRef) {
		ImagePlus img=imgRef.duplicate();
		IJ.run(img,"32-bit","");
		for(int i=0;i<img.getStackSize();i++)img.getStack().getProcessor(i+1).set(0);

		return new ItkTransform(new DisplacementFieldTransform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(new ImagePlus[] {img,img,img})));
	}
	
	public void writeAsDenseFieldWithITKExporter(String path){
		String shortPath = (path != null) ? path.substring(0,path.indexOf('.')) : "";
		ImageFileWriter imWri=new ImageFileWriter();
		imWri.execute((new DisplacementFieldTransform((Transform)(this))).getDisplacementField(),shortPath+".mhd",false);
	}
	
	public static ItkTransform readFromDenseFieldWithITKImporter(String path){
		ImageFileReader imRead=new ImageFileReader();
		imRead.setFileName(path);
		return new ItkTransform(new DisplacementFieldTransform( imRead.execute() ));
	}
	
	
	
	
	public ItkTransform getInverseOfDenseField() {
		ImagePlus[]imgs=ItkImagePlusInterface.convertDisplacementFieldToImagePlusArray(new DisplacementFieldTransform((org.itk.simple.Transform)this).getDisplacementField() );
		ImagePlus[]imgsInv=new ImagePlus[3];
		imgsInv[0]=new Duplicator().run(imgs[0]);
		IJ.run(imgsInv[0],"Multiply...","value=0 stack");
		imgsInv[1]=new Duplicator().run(imgs[0]);
		imgsInv[2]=new Duplicator().run(imgs[1]);
		
		double []voxs=VitimageUtils.getVoxelSizes(imgs[0]);
		int [] dims=VitimageUtils.getDimensions(imgs[0]);
		double sigma=Math.max(voxs[1], voxs[2]);
		int nbVox=(dims[0])*(dims[1])*(dims[2]);
		int iter=0;
		//Generer les paires de points
		Point3d[][]correspondancePoints=new Point3d[2][nbVox];

		for(int x=0;x<dims[0];x++) {
			if(x%20==0)System.out.print(" "+x+"/"+dims[0]);
			for(int y=0;y<dims[1];y++) {
				for(int z=0;z<dims[2];z++) {
					double xx=x*voxs[0];
					double yy=y*voxs[1];
					double zz=z*voxs[2];
					double dx=(imgs[0].getStack().getProcessor(z+1).getf(x, y));
					double dy=(imgs[1].getStack().getProcessor(z+1).getf(x, y));
					double dz=(imgs[2].getStack().getProcessor(z+1).getf(x, y));
					correspondancePoints[0][iter]=new Point3d(xx+dx,yy+dy,zz+dz);
					correspondancePoints[1][iter]=new Point3d(xx,yy,zz);
					iter++;
				}
			}
		}
		
		//Calculer le champ correspondant		
		return new ItkTransform(new DisplacementFieldTransform( computeDenseFieldFromSparseCorrespondancePoints(correspondancePoints,imgs[0],sigma,false) ));
	}
	

	
	
	public void writeAsDenseField(String path,ImagePlus imgRef) {
		String shortPath = (path != null) ? path.substring(0,path.indexOf('.')) : "";
		ImagePlus[]trans=new ImagePlus[3];
		DisplacementFieldTransform df=new DisplacementFieldTransform((Transform)(this));
			
		trans=ItkImagePlusInterface.convertDisplacementFieldToImagePlusArray(df.getDisplacementField());
		IJ.saveAsTiff(trans[0],shortPath+".x.tif");
		IJ.saveAsTiff(trans[1],shortPath+".y.tif");
		IJ.saveAsTiff(trans[2],shortPath+".z.tif");
		IJ.run(trans[0],"8-bit","");
		IJ.saveAsTiff(trans[0],shortPath+".transform.tif");
	}
	
	
	public ItkTransform multiplyDenseField(double factor) {
			MultiplyImageFilter mul=new MultiplyImageFilter();
			VectorIndexSelectionCastImageFilter vectFilter=new VectorIndexSelectionCastImageFilter();
			ComposeImageFilter compFilter=new ComposeImageFilter();
			DisplacementFieldTransform df=new DisplacementFieldTransform((Transform)(this));
			vectFilter.setIndex(0);Image ix=vectFilter.execute(df.getDisplacementField());
			vectFilter.setIndex(1);Image iy=vectFilter.execute(df.getDisplacementField());
			vectFilter.setIndex(2);Image iz=vectFilter.execute(df.getDisplacementField());
			ix=mul.execute(factor, ix);
			iy=mul.execute(factor, iy);
			iz=mul.execute(factor, iz);
			return (new ItkTransform(new DisplacementFieldTransform( compFilter.execute(ix,iy,iz)  )));
	
	}
	
	
	
	
	public static ItkTransform readAsDenseField(String path) {
		String shortPath = (path != null) ? path.substring(0,path.indexOf('.')) : "";
		ImagePlus[]trans=new ImagePlus[3];
		trans[0]=IJ.openImage(shortPath+".x.tif");
		trans[1]=IJ.openImage(shortPath+".y.tif");
		trans[2]=IJ.openImage(shortPath+".z.tif");
		return new ItkTransform(new DisplacementFieldTransform(ItkImagePlusInterface.convertImagePlusArrayToDisplacementField(trans)));
	}
	
	
	public static ItkTransform readTransformFromFile(String path) {
		ItkTransform tr=null;
		try{
			if(path.charAt(path.length()-1) == 'f')tr=readAsDenseField(path);
			else tr=new ItkTransform(SimpleITK.readTransform(path));
			return tr;
		} catch (Exception e) {		IJ.log("Wrong transform file. Please provide a   * .transform.tif file or a *.txt file with an ITK matrix in it");return null; }
	}

	public Point3d transformPoint(Point3d pt) {
		VectorDouble vect=new VectorDouble(3);
		vect.set(0,pt.x);
		vect.set(1,pt.y);
		vect.set(2,pt.z);
		double []coords=ItkImagePlusInterface.vectorDoubleToDoubleArray(this.transformPoint(vect));
		return new Point3d(coords[0],coords[1],coords[2]);
	}
	

	public Point3d transformPointInverse(Point3d pt) {
		VectorDouble vect=new VectorDouble(3);
		vect.set(0,pt.x);
		vect.set(1,pt.y);
		vect.set(2,pt.z);
		double []coords=ItkImagePlusInterface.vectorDoubleToDoubleArray(this.getInverse().transformPoint(vect));
		return new Point3d(coords[0],coords[1],coords[2]);
	}

	
	public double[][] toAffineArrayRepresentation() {
		Point3d pt0=new Point3d(0,0,0);
		Point3d pt0Tr=this.transformPoint(pt0);
		Point3d pt1=new Point3d(1,0,0);
		Point3d pt1Tr=this.transformPoint(pt1);
		Point3d pt2=new Point3d(0,1,0);
		Point3d pt2Tr=this.transformPoint(pt2);
		Point3d pt3=new Point3d(0,0,1);
		Point3d pt3Tr=this.transformPoint(pt3);
		return new double[][] {    { pt1Tr.x-pt0Tr.x , pt2Tr.x-pt0Tr.x , pt3Tr.x-pt0Tr.x   ,pt0Tr.x    },    
											   { pt1Tr.y-pt0Tr.y , pt2Tr.y-pt0Tr.y , pt3Tr.y-pt0Tr.y   ,pt0Tr.y    },  
											   { pt1Tr.z-pt0Tr.z , pt2Tr.z-pt0Tr.z , pt3Tr.z-pt0Tr.z   ,pt0Tr.z    },  
											   { 0 , 0 , 0 , 1 }   };
	}
	
	public ItkTransform simplify() {
		double[][]transArray=this.toAffineArrayRepresentation();
		imagescience.transform.Transform transIj=new imagescience.transform.Transform(transArray);
		return ItkTransform.ijTransformToItkTransform(transIj);		
	}

	public int nbTransformComposed() {
		VitiDialogs.notYet("ItkTransform > nbTransformComposed");
		return 0;
		//A reecrire au vu des decouvertes effectuees sur les chevrons
		//Ce sera la deuxieme passe
		
		/*
		int nb=(this.toString().split("<<<<<<<<<<")[0].split(">>>>>>>>>").length-1);
		if(nb==0)nb=1;
		return nb;*/
	}


	
	public String drawableString() {
		double[][]array=this.toAffineArrayRepresentation();
		String str=String.format("Current transform\n[%8.5f  %8.5f  %8.5f  %8.5f]\n[%8.5f  %8.5f  %8.5f  %8.5f]\n[%8.5f  %8.5f  %8.5f  %8.5f]\n[%8.5f  %8.5f  %8.5f  %8.5f]",
									array[0][0] , array[0][1] , array[0][2] , array[0][3] ,
									array[1][0] , array[1][1] , array[1][2] , array[1][3] ,
									array[2][0] , array[2][1] , array[2][2] , array[2][3] ,
									array[3][0] , array[3][1] , array[3][2] , array[3][3] );
		return str;
	}

	
	public String toString(String title) {
		//A reecrire au vu des decouvertes effectuees sur les chevrons
		//Ce sera la deuxieme passe
		String ret="";
		int nb=this.nbTransformComposed();
		ret=this.toString();
		if(nb==1) {
			return("Transformation "+title+"\nDe type transformation ITK à "+nb+" composante\n>>>>>>>> Composante 1 >>>>>>>>>>>>>>>>>>>>>>>>\n"+(itkTransfoStepToString(ret.substring(ret.indexOf("\n")))));
		}
		else {
			String []tab1=ret.split("<<<<<<<<<<");
			String[] tabActives=(tab1[1].split("\n")[2]).split("[ ]{1,}");
			String[] transforms=tab1[0].split(">>>>>>>>>");
			ret="Transformation "+title+"\nDe type transformation ITK à "+nb+" composantes\n";
			for(int i=1;i<transforms.length;i++) {
				ret+=">>>>>>>> Composante "+i+" ("+(tabActives[i].equals("1") ? "libre" : "fixée")+
						") >>>>>>>>>>>>>>>>\n"+itkTransfoStepToString(transforms[i]) ;
			}
			return ret;
		}
	}

	public String itkTransfoStepToString(String step){		 
		//A reecrire au vu des decouvertes effectuees sur les chevrons
		//Ce sera la deuxieme passe
		double[]mat= itkTransformStepToArray(step);
		String strType=(((step).split("\n"))[1]).split("[ ]{1,}")[1];
		if(strType.equals("Euler3DTransform")) {
			String angles=(((step).split("\n"))[21]).split(":")[1];
			strType+=angles;
		}
		return (stringMatrix("Categorie geometrique : "+strType,mat));
	}

	public double[] itkTransformStepToArray(String step) {
		//A reecrire au vu des decouvertes effectuees sur les chevrons
		//Ce sera la deuxieme passe
		String strInit=step;
		String []tabLign=strInit.split("\n");
		String detectIdentity=tabLign[1].split("[ ]{1,}")[1];
		if(detectIdentity.equals("IdentityTransform")) {
			return (new double[] {1,0,0,0,0,1,0,0,0,0,1,0});
		}
		else if(detectIdentity.equals("TranslationTransform")) {
			String valsTrans[]=tabLign[9].split("\\[")[1].split("\\]")[0].split(", ");
			return (new double[] {1,0,0,Double.valueOf(valsTrans[0]),0,1,0,Double.valueOf(valsTrans[1]),0,0,1,Double.valueOf(valsTrans[2])});
		}
		String []vals123=tabLign[10].split("[ ]{1,}");
		String []vals456=tabLign[11].split("[ ]{1,}");
		String []vals789=tabLign[12].split("[ ]{1,}");
		String []valsT=((((tabLign[13].split("\\[")[1]).split("\\]"))[0]).split(", "));
		return(new double[] {Double.parseDouble(vals123[1]),Double.parseDouble(vals123[2]),Double.parseDouble(vals123[3]),Double.parseDouble(valsT[0]),
				Double.parseDouble(vals456[1]),Double.parseDouble(vals456[2]),Double.parseDouble(vals456[3]),Double.parseDouble(valsT[1]),
				Double.parseDouble(vals789[1]),Double.parseDouble(vals789[2]),Double.parseDouble(vals789[3]),Double.parseDouble(valsT[2]) } );		
	}


	public double[] itkTransformToArray(org.itk.simple.Transform transfo) {
		//A reecrire au vu des decouvertes effectuees sur les chevrons
		//Ce sera la deuxieme passe
		double [][]ret;
		double[] temp;
		int nb=this.nbTransformComposed();
		String str=transfo.toString();
		if(nb==1) {
			return(itkTransformStepToArray(str.substring(str.indexOf("\n"))));
		}
		else {
			String []tab1=str.split("<<<<<<<<<<");
			String[] transforms=tab1[0].split(">>>>>>>>>");
			double[][]tabMat=new double[transforms.length-1][12];
			for(int i=1;i<transforms.length;i++) {
				tabMat[i-1]=itkTransformStepToArray(transforms[i]) ;
			}
			ret=TransformUtils.composeMatrices(tabMat);
			return ret[0];
		}
	}


	public static String stringMatrix(String sTitre,double[]tab){
		//A reecrire au vu des decouvertes effectuees sur les chevrons
		//Ce sera la deuxieme passe
		String s=new String();
		s+=sTitre;
		s+="\n";		
		for(int i=0;i<3;i++){
			s+="[ ";
			for(int j=0;j<3;j++){
				s+=tab[i*4+j];
				s+=" , ";
			}
			s+=tab[i*4+3];
			s+=" ] \n";
		}
		s+= "[ 0 , 0 , 0 , 1 ]\n";
		return(s);	
	}



}