/*
 * 
 */
package net.imagej.fijiyama.registration;

import ij.ImagePlus;
//import imagescience.transform.Transform;
import math3d.Point3d;
import net.imagej.fijiyama.common.VitimageUtils;


// TODO: Auto-generated Javadoc
/**
 * The Interface TransformUtils.
 */
public interface TransformUtils {
	
	/**
	 * The Enum Geometry.
	 */
	public enum Geometry{
		
		/** The reference. */
		REFERENCE,
		
		/** The quasi reference. */
		QUASI_REFERENCE,
		
		/** The switch xy. */
		SWITCH_XY,
		
		/** The switch xz. */
		SWITCH_XZ,
		
		/** The switch yz. */
		SWITCH_YZ,
		
		/** The mirror x. */
		MIRROR_X,
		
		/** The mirror y. */
		MIRROR_Y,
		
		/** The mirror z. */
		MIRROR_Z,
		
		/** The unknown. */
		UNKNOWN
	}
		
	
	
	/**
	 * The Enum Misalignment.
	 */
	public enum Misalignment{
		
		/** The none. */
		NONE,
		
		/** The light rigid. */
		LIGHT_RIGID,
		
		/** The great rigid. */
		GREAT_RIGID,
		
		/** The light similarity. */
		LIGHT_SIMILARITY,
		
		/** The strong similarity. */
		STRONG_SIMILARITY,
		
		/** The light deformation. */
		LIGHT_DEFORMATION,
		
		/** The strong deformation. */
		STRONG_DEFORMATION,		
		
		/** The voxel scale factor. */
		VOXEL_SCALE_FACTOR,
		
		/** The unknown. */
		UNKNOWN
	}
	

	
		
	
	
	/**
	 * Mathematic utilities.
	 *
	 * @param x the x
	 * @param y the y
	 * @return the double
	 */
	public static double calculateAngle(double x, double y)
	{
	    double angle = Math.toDegrees(Math.atan2(y, x));
	    // Keep angle between 0 and 360
	    angle = angle + Math.ceil( -angle / 360 ) * 360;

	    return angle;
	}
	
	/**
	 * Diff teta.
	 *
	 * @param teta1 the teta 1
	 * @param teta2 the teta 2
	 * @return the double
	 */
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


	/**
	 * The Class VolumeComparator.
	 */
	@SuppressWarnings("rawtypes")
	class VolumeComparator implements java.util.Comparator {
		   
   		/**
   		 * Compare.
   		 *
   		 * @param o1 the o 1
   		 * @param o2 the o 2
   		 * @return the int
   		 */
   		public int compare(Object o1, Object o2) {
		      return ((Double) ((Object[]) o1)[0]).compareTo((Double)((Object[]) o2)[0]);
		   }
		}
	
	/**
	 * The Class AngleComparator.
	 */
	@SuppressWarnings("rawtypes")
	class AngleComparator implements java.util.Comparator {
	   
   	/**
   	 * Compare.
   	 *
   	 * @param o1 the o 1
   	 * @param o2 the o 2
   	 * @return the int
   	 */
   	public int compare(Object o1, Object o2) {
	      return ((Double) ((Double[]) o1)[2]).compareTo((Double)((Double[]) o2)[2]);
	   }
	}
	
	/**
	 * Norm.
	 *
	 * @param v the v
	 * @return the double
	 */
	public static double norm(double[]v){
		double tot=0;
		for(int i=0;i<v.length;i++)tot+=v[i]*v[i];
		return Math.sqrt(tot);
	}

	/**
	 * Normalize.
	 *
	 * @param v the v
	 * @return the double[]
	 */
	public static double[] normalize(double[]v){
		double[] ret=new double[v.length];
		double nrm=norm(v);
		for(int i=0;i<v.length;i++)ret[i]=v[i]/nrm;
		return ret;
	}

	/**
	 * Scalar product.
	 *
	 * @param v1 the v 1
	 * @param v2 the v 2
	 * @return the double
	 */
	public static double scalarProduct(double[]v1,double []v2){
		double tot=0;
		for(int i=0;i<v1.length;i++)tot+=v1[i]*v2[i];
		return tot;
	}

	/**
	 * Scalar product.
	 *
	 * @param v1 the v 1
	 * @param v2 the v 2
	 * @return the double
	 */
	public static double scalarProduct(Point3d v1,Point3d v2){
		return(v1.x*v2.x+v1.y*v2.y+v1.z*v2.z);
	}

	/**
	 * Sum vector.
	 *
	 * @param v the v
	 * @param v2 the v 2
	 * @return the double[]
	 */
	public static double [] sumVector(double[]v,double []v2){
		return vectorialAddition(v, v2);
	}

	/**
	 * Sum vector.
	 *
	 * @param v the v
	 * @param v2 the v 2
	 * @param v3 the v 3
	 * @return the double[]
	 */
	public static double [] sumVector(double[]v,double []v2,double[]v3){
		return sumVector(v,sumVector(v2,v3));
	}

	
	/**
	 * Multiply vector.
	 *
	 * @param v the v
	 * @param v2 the v 2
	 * @return the double[]
	 */
	public static double [] multiplyVector(double[]v,double []v2){
		double[] ret=new double[3];
		ret[0]=v[0]*v2[0];
		ret[1]=v[1]*v2[1];
		ret[2]=v[2]*v2[2];
		return ret;
	}

	/**
	 * Multiply vector.
	 *
	 * @param v the v
	 * @param factor the factor
	 * @return the double[]
	 */
	public static double [] multiplyVector(double[]v,double factor){
		double[] ret=new double[3];
		ret[0]=v[0]*factor;
		ret[1]=v[1]*factor;
		ret[2]=v[2]*factor;
		return ret;
	}

	/**
	 * Vectorial product.
	 *
	 * @param v1 the v 1
	 * @param v2 the v 2
	 * @return the double[]
	 */
	public static double[] vectorialProduct(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[1]*v2[2]-v1[2]*v2[1];		
		ret[1]=v1[2]*v2[0]-v1[0]*v2[2];		
		ret[2]=v1[0]*v2[1]-v1[1]*v2[0];		
		return ret;
	}

	/**
	 * Vectorial substraction.
	 *
	 * @param v1 the v 1
	 * @param v2 the v 2
	 * @return the double[]
	 */
	public static double[] vectorialSubstraction(double[]v1,double[]v2){
		double[] ret=new double[v1.length];
		for(int i=0;i<v1.length;i++)ret[i]=v1[i]-v2[i];		
		return ret;
	}

	/**
	 * Vectorial addition.
	 *
	 * @param v1 the v 1
	 * @param v2 the v 2
	 * @return the double[]
	 */
	public static double[] vectorialAddition(double[]v1,double[]v2){
		double[] ret=new double[v1.length];
		for(int i=0;i<v1.length;i++)ret[i]=v1[i]+v2[i];		
		return ret;
	}


	/**
	 * Vectorial mean.
	 *
	 * @param v1 the v 1
	 * @param v2 the v 2
	 * @return the double[]
	 */
	public static double[] vectorialMean(double[]v1,double[]v2){
		double[] ret=new double[v1.length];
		for(int i=0;i<v1.length;i++)ret[i]=(v1[i]+v2[i])/2.0;		
		return ret;
	}

	
	/**
	 * Invert vector.
	 *
	 * @param v1 the v 1
	 * @return the double[]
	 */
	public static double[] invertVector(double[]v1){
		double[] ret=new double[v1.length];
		for(int i=0;i<v1.length;i++)ret[i]=1/v1[i];		
		return ret;
	}

	/**
	 * Proj u of v.
	 *
	 * @param u the u
	 * @param v the v
	 * @return the double[]
	 */
	public static double[] proj_u_of_v(double[]u,double[]v){
		double scal1=scalarProduct(u,v);
		double scal2=scalarProduct(u,u);
		return multiplyVector(u,scal1/scal2);
	}
	
	/**
	 * Convert point to real space.
	 *
	 * @param p the p
	 * @param img the img
	 * @return the point 3 d
	 */
	public static Point3d convertPointToRealSpace(Point3d p,ImagePlus img) {
		double alpha=0;//for itk
		return new Point3d((p.x+alpha)*img.getCalibration().pixelWidth , (p.y+alpha)*img.getCalibration().pixelHeight , (p.z+alpha)*img.getCalibration().pixelDepth);
	}

	/**
	 * Convert point to image space.
	 *
	 * @param p the p
	 * @param img the img
	 * @return the point 3 d
	 */
	public static Point3d convertPointToImageSpace(Point3d p,ImagePlus img) {
		double alpha=0;//for itk
		return new Point3d(p.x/img.getCalibration().pixelWidth-alpha, p.y/img.getCalibration().pixelHeight-alpha, p.z/img.getCalibration().pixelDepth-alpha);
	}

	/**
	 * Point 3 d to double array.
	 *
	 * @param p the p
	 * @return the double[]
	 */
	public static double[] point3dToDoubleArray(Point3d p) {return new double[] {p.x,p.y,p.z};}
	
	/**
	 * Double array to point 3 d.
	 *
	 * @param d the d
	 * @return the point 3 d
	 */
	public static Point3d doubleArrayToPoint3d(double[]d) {return new Point3d(d[0],d[1],d[2]);}
	

	/**
	 * IO Utilities for Vectors and Matrices.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 */
	static void printVector(double []vect,String vectNom){
		System.out.println(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	/**
	 * String vector N.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @return the string
	 */
	static String stringVectorN(double []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=vect[i]+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	/**
	 * String vector N dou.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @return the string
	 */
	static String stringVectorNDou(double []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=VitimageUtils.dou(vect[i])+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	/**
	 * String vector N.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @return the string
	 */
	static String stringVectorN(int []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=vect[i]+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	/**
	 * String matrix N.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @return the string
	 */
	static String stringMatrixN(int [][]vect,String vectNom){
		String str=vectNom+" = [ \n";
		for(int i=0;i<vect.length;i++)str+=stringVectorN(vect[i],"Ligne "+i+"")+"\n";
		str+=" ]";
		return str;
	}

	
	/**
	 * String matrix MN.
	 *
	 * @param sTitre the s titre
	 * @param confusionMatrix the confusion matrix
	 * @return the string
	 */
	public static String stringMatrixMN(String sTitre,double[][] confusionMatrix){
		String s=new String();
		s+=""+sTitre+" , matrice de taille "+confusionMatrix.length+" X "+confusionMatrix[0].length+"\n";
		for(int i=0;i<confusionMatrix.length;i++){
			s+="[ ";
			for(int j=0;j<confusionMatrix[i].length-1;j++){
				s+=VitimageUtils.dou(confusionMatrix[i][j]);
				s+=" , ";
			}
			s+=VitimageUtils.dou(confusionMatrix[i][confusionMatrix[i].length-1])+" ] \n";
		}
		return s;
	}	
	
	/**
	 * String matrix MN.
	 *
	 * @param sTitre the s titre
	 * @param confusionMatrix the confusion matrix
	 * @return the string
	 */
	public static String stringMatrixMN(String sTitre,int[][] confusionMatrix){
		String s=new String();
		s+=""+sTitre+" , matrice de taille "+confusionMatrix.length+" X "+confusionMatrix[0].length+"\n";
		for(int i=0;i<confusionMatrix.length;i++){
			s+="[ ";
			for(int j=0;j<confusionMatrix[i].length-1;j++){
				s+=VitimageUtils.dou(confusionMatrix[i][j]);
				s+=" , ";
			}
			s+=VitimageUtils.dou(confusionMatrix[i][confusionMatrix[i].length-1])+" ] \n";
		}
		return s;
	}	

	
	/**
	 * String vector.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @return the string
	 */
	public static String stringVector(double []vect,String vectNom){
		return(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	/**
	 * String vector dou.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @return the string
	 */
	public static String stringVectorDou(double []vect,String vectNom){
		return(vectNom+" = [ "+VitimageUtils.dou(vect[0])+" , "+VitimageUtils.dou(vect[1])+" , "+VitimageUtils.dou(vect[2])+" ]");
	}

	/**
	 * String vector dou.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @param n the n
	 * @return the string
	 */
	public static String stringVectorDou(double []vect,String vectNom,int n){
		return(vectNom+" = [ "+VitimageUtils.dou(vect[0],n)+" , "+VitimageUtils.dou(vect[1],n)+" , "+VitimageUtils.dou(vect[2],n)+" ]");
	}

	/**
	 * String vector.
	 *
	 * @param vect the vect
	 * @param vectNom the vect nom
	 * @return the string
	 */
	public static String stringVector(int []vect,String vectNom){
		return(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}
	


	
}
