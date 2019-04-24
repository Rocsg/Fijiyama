package com.vitimage;

import org.itk.simple.ResampleImageFilter;
import org.itk.simple.SimpleITK;
import org.itk.simple.Transform;
import org.itk.simple.VectorDouble;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.Concatenator;
import ij.plugin.GaussianBlur3D;
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

	public static ItkTransform estimateBestAffine3D(Point3d[]setRef,Point3d[]setMov) {
		return fastMatrixToItkTransform(FastMatrix.bestLinear( setMov, setRef));
	}
	
	
	public static ItkTransform estimateBestRigid3D(Point3d[]setRef,Point3d[]setMov) {
		return fastMatrixToItkTransform(FastMatrix.bestRigid( setMov, setRef, false ));
	}

	public static ItkTransform estimateBestSimilarity3D(Point3d[]setRef,Point3d[]setMov) {
		return fastMatrixToItkTransform(FastMatrix.bestRigid( setMov, setRef, true ));
	}
	
	public ImagePlus transformImage(ImagePlus imgRef, ImagePlus imgMov) {
		int val=Math.min(  10    ,    Math.min(   imgMov.getWidth()/20    ,   imgMov.getHeight()/20  ));
		int valMean=(int)Math.round(      VitimageUtils.meanValueofImageAround(imgMov,val,val,0,val)*0.5 + VitimageUtils.meanValueofImageAround(imgMov,imgMov.getWidth()-val-1,imgMov.getHeight()-val-1,0,val)*0.5    );
		ResampleImageFilter resampler=new ResampleImageFilter();
		resampler.setDefaultPixelValue(valMean);
		resampler.setReferenceImage(ItkImagePlusInterface.imagePlusToItkImage(imgRef));
		resampler.setTransform(this);
		return (ItkImagePlusInterface.itkImageToImagePlus(resampler.execute(ItkImagePlusInterface.imagePlusToItkImage(imgMov))));
	}	

	public ImagePlus viewAsGrid3D(ImagePlus imgRef,int pixelSpacing) {
		ImagePlus grid=VitimageUtils.getBinaryGrid(imgRef, pixelSpacing);
		return this.transformImage(imgRef,grid);
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
	
	public static ItkTransform readTransformFromFile(String path) {
		VitiDialogs.notYet("ItkTransform > readTransformFromFile");
		return null;
	}

	public void writeToFile(String path) {
		SimpleITK.writeTransform(this,path);
	}

	public static ItkTransform readFromFile(String path) {
		return(new ItkTransform(SimpleITK.readTransform(path)));
	}

	public Point3d transformPoint(Point3d pt) {
		VectorDouble vect=new VectorDouble(3);
		vect.set(0,pt.x);
		vect.set(1,pt.y);
		vect.set(2,pt.z);
		double []coords=ItkImagePlusInterface.vectorDoubleToDoubleArray(this.transformPoint(vect));
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

/*

	public static ItkTransform computeTransformationForBoutureAlignment(ImagePlus img,boolean computeZaxisOnly,boolean ignoreUnattemptedDimensions){
		boolean debug=true;
		int yMax=img.getHeight();
		int xMax=img.getWidth();
		int zMax=img.getStack().getSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double xMoyUp=0,yMoyUp=0,zMoyUp=0;
		double xMoyDown=0,yMoyDown=0,zMoyDown=0;
		int hitsUp=0,hitsDown=0;
		
		//Step 1 : apply gaussian filtering and convert to 8 bits
		System.out.println("Lissage 3D puis reduction à 8 bits : facteurs de lisage (en pixels)=("+8*0.035/vX+" , "+8*0.035/vY+" , "+2*0.5/vZ+")");
		GaussianBlur3D.blur(img,8*0.035/vX,8*0.035/vY,2*0.5/vZ);
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();

		//Step 2 : apply automatic threshold
		ByteProcessor[] maskTab=new ByteProcessor[zMax];
		for(int z=0;z<zMax;z++){
			maskTab[z]=(ByteProcessor) img.getStack().getProcessor(z+1);
			maskTab[z].setAutoThreshold("Otsu dark");
			maskTab[z].createMask();
		}
		//Extract two substacks for the upper part and the lower part of the object
		ImageStack stackUp = new ImageStack(xMax, yMax);	
		ImageStack stackDown = new ImageStack(xMax, yMax);
		int zQuarter=zMax/4;
		int zVentile=zMax/40;
		zVentile=(zVentile < 10 ? 10 : zVentile);
		if(zMax<zVentile*2+2)zVentile=zMax/2-1;
		System.out.println("Valeurs calculees , zQuarter="+zQuarter+" , zVentile="+zVentile);
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",img.getStack().getProcessor(zMax/2+zQuarter-zVentile+1+i));//de zmax/2 à zMax/2 + 5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",img.getStack().getProcessor(zMax/2-zQuarter+1+i+1));//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		ImagePlus imgUpCon=VitimageUtils.connexe(imgUp,0,29,0,10E10,6,2,true);
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		ImagePlus imgDownCon=VitimageUtils.connexe(imgDown,0,29,0,10E10,6,2,true);
		
		
		//Step 3 : compute the two centers of mass
		System.out.println("Calcul des centres de masse");
		short[][]valsDownCon=new short[zQuarter][];
		short[][]valsUpCon=new short[zQuarter][];
		for(int z=0;z<zVentile;z++){
			valsDownCon[z]=(short[])(imgDownCon).getStack().getProcessor(z+1).getPixels();
			valsUpCon[z]=(short[])(imgUpCon).getStack().getProcessor(z+1).getPixels();
		}

		for(int x=0;x<xMax;x++){
			for(int y=0;y<yMax;y++){
				for(int z=0;z<zVentile;z++){								
					if(valsDownCon[z][xMax*y+x]==((short)255)){//We are in the first part of the object
						hitsDown++;
						xMoyDown+=x;yMoyDown+=y;zMoyDown+=z;
					}
					if(valsUpCon[z][xMax*y+x]==((short	)255)){//We are in the first part of the object
						hitsUp++;
						xMoyUp+=x;yMoyUp+=y;zMoyUp+=z;
					}
				}
			}
		}
		xMoyUp=xMoyUp/hitsUp;//Center of mass computation. 
		yMoyUp=yMoyUp/hitsUp;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyUp=zMoyUp/hitsUp+zMax/2+zQuarter-zVentile;//due to the extraction of a substack zmax/2-zQuarter+1 - zmax/2     zMax/2+zQuarter-zVentile

		xMoyDown=xMoyDown/hitsDown;//Center of mass computation. 
		yMoyDown=yMoyDown/hitsDown;//Double type stands a 15 digits precisions; which is enough here, until #voxels < 5.10^12 
		zMoyDown=zMoyDown/hitsDown+zMax/2-zQuarter+1;//due to the extraction of a substack zmax/2 - zmax/2+zQuarter       zMax/2-zQuarter+1

		if(debug) {
			System.out.println("HitsUp="+hitsUp+" ..Center of mass up = "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("HitsDown="+hitsDown+" ..Center of mass down = "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}
		
		xMoyUp=xMoyUp*vX+vX/2;		
		yMoyUp=yMoyUp*vY+vY/2;		
		zMoyUp=zMoyUp*vZ+vZ/2;		
		xMoyDown=xMoyDown*vX+vX/2;		
		yMoyDown=yMoyDown*vY+vY/2;		
		zMoyDown=zMoyDown*vZ+vZ/2;		
		if(debug) {
			System.out.println("Center of mass up (coord reel)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (coord reel)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}
		
		//Step 4 : compute the new basis
		double []vectXfin=new double[3];
		double []vectYinit=new double[3];
		double []vectYfin=new double[3];
		double []vectZinit=new double[3];
		double []vectZfin=new double[3];

		//Last vector of the base : tissue axis from low Z values to High Z values
		vectZinit[1]=yMoyUp-yMoyDown;
		vectZinit[2]=zMoyUp-zMoyDown;

		//Compute the center of inoculation point
		double xP=0,yP=0,zP=0;
		if(computeZaxisOnly) {//Guess a point in the lower part of the image
			xP=(xMoyUp+xMoyDown)/2.0;
			yP=(yMoyUp+yMoyDown)/2.0+100*vY;
			zP=(zMoyUp+zMoyDown)/2.0;
		}
		else {//We know the actual point
			for(int i=0;i<cornersCoordinates.length;i++) {
				xP+=cornersCoordinates[i][0];
				yP+=cornersCoordinates[i][1];
				zP+=cornersCoordinates[i][2];
			}
			xP/=cornersCoordinates.length;
			yP/=cornersCoordinates.length;
			zP/=cornersCoordinates.length;
		}
		if(debug) System.out.println("Inoculation point (in real coordinates) : "+xP+"  ,  "+yP+"  ,  "+zP);
		
		
		//Center of tissue to PI, make the second vector
		vectYinit[0]=xP-xMoyDown;
		vectYinit[1]=yP-yMoyDown;
		vectYinit[2]=zP-zMoyDown;

		//"Graham-schmidt orthogonalisation" (Not exactly, as we use Y and Z to produce X)

		//E3=v3/norm(v3)
		vectZfin=normalize(vectZinit);

		if(debug) printVector(vectZinit,"v3=");
		if(debug) printVector(vectZfin,"E3=");
		//u2=v2-proj_u3_of_v2  ; E2=v2/norm(v2)
		if(debug) printVector(vectYinit,"v2=");
		vectYfin=vectorialSubstraction(vectYinit,proj_u_of_v(vectZinit,vectYinit));
		vectYfin=normalize(vectYfin);
		if(debug) printVector(vectYfin,"E2=");

		//For E1
		vectXfin=vectorialProduct(vectYfin,vectZfin);
		if(debug) printVector(vectXfin,"E1=");




		//Retrieve the translation parameters 
		//We want that through the transformation, the inoculation point get the coordinates : xFinal,yFinal,zFinal.
		//That stands for that experiment in which all the data lies in 512 x 512 x 40 stacks.
		//If one of the dimensions differs a lot, it should propose another strategy. 
		System.out.println("Calcul de la translation restante");
		if((xMax != 512) || (yMax != 512) || (zMax < 38) || (zMax > 42)){//Alert
			if(!ignoreUnattemptedDimensions) {
				GenericDialog gd = new GenericDialog("Alert !\nUnattempted image dimensions : "+xMax+" x "+yMax+" x "+zMax+"\n512 x 512 x 40 expected\nPlease choose the location of the inoculation point after transformation...");
	        gd.addNumericField("Confirm X  : ", xMax/2, 5);
	        gd.addNumericField("Confirm Y  : ", (yMax*380)/512, 5);
	        gd.addNumericField("Confirm Z  : ", zMax/2, 5);
	        gd.showDialog();
	        if (gd.wasCanceled()) return null;
			xFinal=gd.getNextNumber();	 
			yFinal=gd.getNextNumber();	 
			zFinal=gd.getNextNumber();	 
			}
			else {
				xFinal=xMax/2;
				yFinal=(yMax*380)/512;
				zFinal=zMax/2;
			}
		
		}
		xFinal=xFinal*vX+vX/2;	
		yFinal=yFinal*vY+vY/2;	
		zFinal=zFinal*vZ+vZ/2;	
		
		double trans[]=new double[3];		
		
		trans[0]=xP-vectXfin[0]*xFinal-vectYfin[0]*yFinal-vectZfin[0]*zFinal;
		trans[1]=yP-vectXfin[1]*xFinal-vectYfin[1]*yFinal-vectZfin[1]*zFinal;
		trans[2]=zP-vectXfin[2]*xFinal-vectYfin[2]*yFinal-vectZfin[2]*zFinal;
		if(debug) System.out.println("Translation : ["+trans[0]+" , "+trans[1]+" , "+trans[2]+"]");
		
		//Assemble and write the transformation matrix
		Transform tr=new Transform(vectXfin[0],vectYfin[0],vectZfin[0],trans[0],
								   vectXfin[1],vectYfin[1],vectZfin[1],trans[1],
								   vectXfin[2],vectYfin[2],vectZfin[2],trans[2]);

		if(debug) System.out.println("Et en effet, lorsqu'on transforme le point final :"+xFinal+" , "+yFinal+" , "+zFinal+"...");
		if(debug) System.out.println("On obtient bien le point initial attendu, qui était : "+xP+" , "+yP+" , "+zP+"...");
		double valXtest=vectXfin[0]*xFinal+vectYfin[0]*yFinal+vectZfin[0]*zFinal+trans[0];
		double valYtest=vectXfin[1]*xFinal+vectYfin[1]*yFinal+vectZfin[1]*zFinal+trans[1];
		double valZtest=vectXfin[2]*xFinal+vectYfin[2]*yFinal+vectZfin[2]*zFinal+trans[2];
		
		if(debug) System.out.println("La preuve, résultat : "+valXtest+" , "+valYtest+" , "+valZtest+"...");
	
		Transform trInv=new Transform(tr);
		trInv.invert();
		System.out.println(" Matrice complete obtenue en fin de fonction "+ItkTransform.stringMatrix("",transformToArray(tr)));
		return ItkTransform.ijTransformToItkTransform(tr);	
	}

	*/



}
