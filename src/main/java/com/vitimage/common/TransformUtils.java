package com.vitimage.common;

import ij.ImagePlus;
import imagescience.transform.Transform;
import math3d.Point3d;


public interface TransformUtils {
	public enum Geometry{
		REFERENCE,
		QUASI_REFERENCE,
		SWITCH_XY,
		SWITCH_XZ,
		SWITCH_YZ,
		MIRROR_X,
		MIRROR_Y,
		MIRROR_Z,
		UNKNOWN
	}
		
	
	
	public enum Misalignment{
		NONE,
		LIGHT_RIGID,
		GREAT_RIGID,
		LIGHT_SIMILARITY,
		STRONG_SIMILARITY,
		LIGHT_DEFORMATION,
		STRONG_DEFORMATION,		
		VOXEL_SCALE_FACTOR,
		UNKNOWN
	}
	

	
		
	
	
	/**
	 * Mathematic utilities
	 * @param img
	 * @param computeZaxisOnly
	 * @param cornersCoordinates
	 * @param ignoreUnattemptedDimensions
	 * @return
	 */
	public static double calculateAngle(double x, double y)
	{
	    double angle = Math.toDegrees(Math.atan2(y, x));
	    // Keep angle between 0 and 360
	    angle = angle + Math.ceil( -angle / 360 ) * 360;

	    return angle;
	}
	
	public static double diffTeta(double teta1,double teta2) {
		//120   130  -> -10
		//130   120  -> 10
		//30    330  -> 30  -30  -> 60 
		//330    30  -> -30  30  -> -60 
		if(Math.abs(teta1-teta2)>180) {
			if(teta1>teta2)return(teta1-360-teta2);
			if(teta2>teta1)return(teta1-(teta2-360));
		}
		else {
			return teta1-teta2;
		}
		return 0;
	}


	@SuppressWarnings("rawtypes")
	class VolumeComparator implements java.util.Comparator {
		   public int compare(Object o1, Object o2) {
		      return ((Double) ((Object[]) o1)[0]).compareTo((Double)((Object[]) o2)[0]);
		   }
		}
	
	@SuppressWarnings("rawtypes")
	class AngleComparator implements java.util.Comparator {
	   public int compare(Object o1, Object o2) {
	      return ((Double) ((Double[]) o1)[2]).compareTo((Double)((Double[]) o2)[2]);
	   }
	}
	
	public static double norm(double[]v){
		return Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
	}

	public static double[] normalize(double[]v){
		double[] ret=new double[3];
		double nrm=norm(v);
		ret[0]=v[0]/nrm;
		ret[1]=v[1]/nrm;
		ret[2]=v[2]/nrm;
		return ret;
	}

	public static double scalarProduct(double[]v1,double []v2){
		return(v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2]);
	}

	public static double scalarProduct(Point3d v1,Point3d v2){
		return(v1.x*v2.x+v1.y*v2.y+v1.z*v2.z);
	}

	public static double [] sumVector(double[]v,double []v2){
		double[] ret=new double[3];
		ret[0]=v[0]+v2[0];
		ret[1]=v[1]+v2[1];
		ret[2]=v[2]+v2[2];
		return ret;
	}

	public static double [] sumVector(double[]v,double []v2,double[]v3){
		return sumVector(v,sumVector(v2,v3));
	}

	
	public static double [] multiplyVector(double[]v,double []v2){
		double[] ret=new double[3];
		ret[0]=v[0]*v2[0];
		ret[1]=v[1]*v2[1];
		ret[2]=v[2]*v2[2];
		return ret;
	}

	public static double [] multiplyVector(double[]v,double factor){
		double[] ret=new double[3];
		ret[0]=v[0]*factor;
		ret[1]=v[1]*factor;
		ret[2]=v[2]*factor;
		return ret;
	}

	public static double[] vectorialProduct(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[1]*v2[2]-v1[2]*v2[1];		
		ret[1]=v1[2]*v2[0]-v1[0]*v2[2];		
		ret[2]=v1[0]*v2[1]-v1[1]*v2[0];		
		return ret;
	}

	public static double[] vectorialSubstraction(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[0]-v2[0];		
		ret[1]=v1[1]-v2[1];		
		ret[2]=v1[2]-v2[2];		
		return ret;
	}

	public static double[] vectorialAddition(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[0]+v2[0];		
		ret[1]=v1[1]+v2[1];		
		ret[2]=v1[2]+v2[2];		
		return ret;
	}

	public static double[] invertVector(double[]v1){
		double[] ret=new double[3];
		ret[0]=1/v1[0];		
		ret[1]=1/v1[1];		
		ret[2]=1/v1[2];		
		return ret;
	}

	public static double[] proj_u_of_v(double[]u,double[]v){
		double scal1=scalarProduct(u,v);
		double scal2=scalarProduct(u,u);
		return multiplyVector(u,scal1/scal2);
	}
	
	public static Point3d convertPointToRealSpace(Point3d p,ImagePlus img) {
		double alpha=0;//for itk
		return new Point3d((p.x+alpha)*img.getCalibration().pixelWidth , (p.y+alpha)*img.getCalibration().pixelHeight , (p.z+alpha)*img.getCalibration().pixelDepth);
	}

	public static Point3d convertPointToImageSpace(Point3d p,ImagePlus img) {
		double alpha=0;//for itk
		return new Point3d(p.x/img.getCalibration().pixelWidth-alpha, p.y/img.getCalibration().pixelHeight-alpha, p.z/img.getCalibration().pixelDepth-alpha);
	}

	public static double[] point3dToDoubleArray(Point3d p) {return new double[] {p.x,p.y,p.z};}
	
	public static Point3d doubleArrayToPoint3d(double[]d) {return new Point3d(d[0],d[1],d[2]);}
	

	/**
	 * IO Utilities for Vectors and Matrices
	 * @param vect
	 * @param vectNom
	 */
	static void printVector(double []vect,String vectNom){
		System.out.println(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	static String stringVectorN(double []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=vect[i]+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	static String stringVectorNDou(double []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=VitimageUtils.dou(vect[i])+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	static String stringVectorN(int []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=vect[i]+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	static String stringMatrixN(int [][]vect,String vectNom){
		String str=vectNom+" = [ \n";
		for(int i=0;i<vect.length;i++)str+=stringVectorN(vect[i],"Ligne "+i+"")+"\n";
		str+=" ]";
		return str;
	}

	public static String stringVector(double []vect,String vectNom){
		return(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	public static String stringVectorDou(double []vect,String vectNom){
		return(vectNom+" = [ "+VitimageUtils.dou(vect[0])+" , "+VitimageUtils.dou(vect[1])+" , "+VitimageUtils.dou(vect[2])+" ]");
	}

	public static String stringVectorDou(double []vect,String vectNom,int n){
		return(vectNom+" = [ "+VitimageUtils.dou(vect[0],n)+" , "+VitimageUtils.dou(vect[1],n)+" , "+VitimageUtils.dou(vect[2],n)+" ]");
	}

	public static String stringVector(int []vect,String vectNom){
		return(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}
	

	public static double[][]composeMatrices(Transform[]matricesToCompose){
		 double [][]matRet=new double[2][12];
		 Transform transGlobal=new Transform();
		 Transform transInd;
		 System.out.println("Et matrices to Compose fait tant de long : "+matricesToCompose.length);
		 for(int tr=0;tr<matricesToCompose.length;tr++) {
			 transInd=matricesToCompose[tr];
			 System.out.println("TransIND 1/2= :");
   		for(int ii=0;ii<3;ii++){
   			System.out.println("[ "+transInd.get(ii,0)+"  "+transInd.get(ii,1)+"  "+transInd.get(ii,2)+"  "+transInd.get(ii,3)+" ]");
       	}
			System.out.println("");
	    		
		 	transInd.transform(transGlobal);
		 	System.out.println("TransIND 2/2= :");
   		for(int ii=0;ii<3;ii++){
				System.out.println("[ "+transInd.get(ii,0)+"  "+transInd.get(ii,1)+"  "+transInd.get(ii,2)+"  "+transInd.get(ii,3)+" ]");
       	}
			System.out.println("");
			 transGlobal=new Transform(transInd);			 
		 }

		 transInd=new Transform(transGlobal);
		 transInd.invert();
		 matRet[0]=transformToArray(transGlobal);
		 matRet[1]=transformToArray(transInd);
		 return matRet;
	 }
	 
	public static double[][]composeMatrices(double[][]matricesToCompose){
		Transform[] trans=new Transform[matricesToCompose.length];
		for(int i=0;i<matricesToCompose.length;i++)trans[i]=arrayToTransform(matricesToCompose[i]);
		return (composeMatrices(trans));
	 }
	 
	
	public static double[]transformToArray(Transform transGlobal){
	 	double[]ret=new double[]{transGlobal.get(0,0) ,transGlobal.get(0,1) ,transGlobal.get(0,2) ,transGlobal.get(0,3) ,
	 							 transGlobal.get(1,0) ,transGlobal.get(1,1) ,transGlobal.get(1,2) ,transGlobal.get(1,3) ,
 							 	 transGlobal.get(2,0) ,transGlobal.get(2,1) ,transGlobal.get(2,2) ,transGlobal.get(2,3)
 							 	 };
		 return (ret);
	 }
	
	public static Transform arrayToTransform(double[]mat){
	 	Transform tr=new Transform();
	 	for(int j=0;j<12;j++) {
	 		tr.set(j/4,j%4,mat[j]);	 	
	 	}
	 	return tr;
 	}

	
}
