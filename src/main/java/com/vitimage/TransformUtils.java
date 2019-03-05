package com.vitimage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import com.vitimage.Vitimage_Toolbox.VolumeComparator;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.GaussianBlur3D;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import imagescience.transform.Transform;

public class TransformUtils {
	
	int COORD_OF_MAX_IN_TWO_LAST_SLICES=1;
	public TransformUtils() {
		
	}
	
	
	/** T2 is the transformation, written in the second basis
	*   T1 is the transformation that we are looking for, written in the initial basis
	*   P12 is the passage matrix, that allows one to resample the image (I1)
	*   in order to get (I2), thus : vect1 = P12 x vect2
	*   P21 is the inverse matrix
	*   vxRatio is the ratio between the voxel sizes : vx2/vx1     
	* */
    public void basisChange(double [] T2,double vxRatio,double vyRatio,double vzRatio,double [] T1){
		double vxRatInv=1.0/vxRatio;
		double vyRatInv=1.0/vyRatio;
		double vzRatInv=1.0/vzRatio;
		double [] P12={vxRatio,0,0,-0.5+vxRatio/2, 0,vyRatio,0,-0.5+vyRatio/2,  0,0,vzRatio,-0.5+vzRatio/2};
		double [] P21={vxRatInv,0,0,-0.5+vxRatInv/2, 0,vyRatInv,0,-0.5+vyRatInv/2,  0,0,vzRatInv,-0.5+vzRatInv/2};
		double [] intermediaryRes=new double[12];	
		matrixProduct(T2,P21,intermediaryRes);
		matrixProduct(P12,intermediaryRes,T1);
		System.out.println("Basis change");
		printMatrix("T2",T2);
		printMatrix("P12",P12);
		printMatrix("P21",P21);
		printMatrix("T1=P12 x (T2 x P21)",T1);
	}

	/** Tinter is the transformation matrix, written in the registration space
	*   P1 is the passage matrix from the initial space to the registration space, built from the voxelSizes of the initial space scales1
	*   P2 is the passage matrix from the registration space to the visualization space, built from the voxelSizes of the initial space scales2
	* @return The global transformation matrix T' = P1 x T x P2
	* */
	public double[] doubleBasisChange(double [] scales1,double [] Tinter,double [] scales2){
		double [] result=new double[12];
		double vxRatio=scales1[0];
		double vyRatio=scales1[1];
		double vzRatio=scales1[2];
		double vxRatInv=scales2[0];
		double vyRatInv=scales2[1];
		double vzRatInv=scales2[2];
		double [] P1={1/vxRatio,0,0,-0.5, 0,1/vyRatio,0,-0.5,  0,0,1/vzRatio,-0.5};
		double [] P2={vxRatInv,0,0,vxRatInv/2, 0,vyRatInv,0,vyRatInv/2,  0,0,vzRatInv,vzRatInv/2};
		double [] intermediaryRes=new double[12];	
		matrixProduct(Tinter,P2,intermediaryRes);
		matrixProduct(P1,intermediaryRes,result);
		System.out.println("Double basis change");
		printMatrix("P1, Passage from initial space to registration space : ",P1);
		printMatrix("T, Initial transformation matrix for registration : ",Tinter);
		printMatrix("P2, Passage from registration space to visualization space : ",P2);
		printMatrix("Global transformation = P1 x T x P2",result);
		return result;
	}

	public void printMatrix(String sTitre,double[]tab){
		String s=new String();
		s+="\n Affichage de la matrice ";
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
		s+= "[ 0 , 0 , 0 , 1 ]\n\n";
		System.out.println(s);	
	}	
	
	public String stringMatrix(String sTitre,double[]tab){
		String s=new String();
		s+="\n Affichage de la matrice ";
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
		s+= "[ 0 , 0 , 0 , 1 ]\n\n";
		return(s);	
	}	

	public void matrixProduct(double[]tab1,double[]tab2,double[]tab3){
		//Matrix product in homogeneous coordinates 
		// [      Rotation       |Translation_x]
		// [      Rotation       |Translation_y]
		// [      Rotation       |Translation_z]
		// [   0     0      0    |      1      ]
		for(int i=0;i<3;i++){
			//Rotation component
			for(int j=0;j<4;j++){
				tab3[i*4+j]=tab1[i*4]*tab2[j] + tab1[i*4+1]*tab2[4+j] + tab1[i*4+2]*tab2[8+j];
			}
			//Additional computation for the translation component
			tab3[i*4+3]+=tab1[i*4+3];
		}
	}

	public double[][]composeMatrices(Transform[]matricesToCompose){
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
	 
	public double[]transformToArray(Transform transGlobal){
	 	double[]ret=new double[]{transGlobal.get(0,0) ,transGlobal.get(0,1) ,transGlobal.get(0,2) ,transGlobal.get(0,3) ,
	 							 transGlobal.get(1,0) ,transGlobal.get(1,1) ,transGlobal.get(1,2) ,transGlobal.get(1,3) ,
 							 	 transGlobal.get(2,0) ,transGlobal.get(2,1) ,transGlobal.get(2,2) ,transGlobal.get(2,3)
 							 	 };
		 return (ret);
	 }
	
	public Transform arrayToTransform(double[]mat){
	 	Transform tr=new Transform();
	 	for(int j=0;j<12;j++) {
	 		tr.set(j/4,j%4,mat[j]);	 	
	 	}
	 	return tr;
 	}

	public Transform readMatrixFromFile(String filePath,boolean debug) {
		Transform tr=new Transform(1,0,0,0,0,1,0,0,0,0,1,0);
		if (filePath==null)return tr;
		String strFile;
		try {
			 strFile= Files.lines(Paths.get(filePath) ).collect(Collectors.joining("\n"));
        } catch (IOException ex) {
	        ex.printStackTrace();
	        strFile="None\nNone";
        }

		if(debug){
			System.out.println("STRING LUE DANS FICHIER MATRICE=");
			System.out.println(strFile);
		}
        String[]strLines=strFile.split("\n");
        if(strLines[0].charAt(0)=='#'){
        	if(strLines[0].charAt(1)=='I'){ //Transform file in ITK format
        	   if(debug)System.out.println("ITK file");
			   String[]strValues=strLines[3].split(" ");
    		   if(strValues.length==13){
    		   		tr.set(Double.valueOf(strValues[1]), Double.valueOf(strValues[2]), Double.valueOf(strValues[3]), Double.valueOf(strValues[10]), 
    		   			   Double.valueOf(strValues[4]), Double.valueOf(strValues[5]), Double.valueOf(strValues[6]), Double.valueOf(strValues[11]),
    		   			   Double.valueOf(strValues[7]), Double.valueOf(strValues[8]), Double.valueOf(strValues[9]), Double.valueOf(strValues[12]) );
    		   }
    		   else System.out.println("ITK Transform file is corrupted ");
        	}
        	else if(strLines[0].charAt(2)=='S' || strLines[0].charAt(2)=='A'){//Transform file in ImageJ's Transform_IO format
			   if(debug)System.out.println("Transform_IO file");
			   String[]strValuesl1=strLines[2].split(" ");
			   String[]strValuesl2=strLines[3].split(" ");
				   String[]strValuesl3=strLines[4].split(" ");
    		   if(strValuesl1.length==4  && strValuesl2.length==4  && strValuesl3.length==4 ){
    		   		tr.set(Double.valueOf(strValuesl1[0]), Double.valueOf(strValuesl1[1]), Double.valueOf(strValuesl1[2]), Double.valueOf(strValuesl1[3]), 
    		   			   Double.valueOf(strValuesl2[0]), Double.valueOf(strValuesl2[1]), Double.valueOf(strValuesl2[2]), Double.valueOf(strValuesl2[3]),
    		   			   Double.valueOf(strValuesl3[0]), Double.valueOf(strValuesl3[1]), Double.valueOf(strValuesl3[2]), Double.valueOf(strValuesl3[3]) );
    		   			   if(strLines[0].charAt(2)=='S') tr.invert();// patch for the transformation computed with an manual alignment with 3dviewer
    		   }
    		   else System.out.println("ImageJ Transform file is corrupted ");
        	}
        }
        else {
    		if(debug){System.out.println("Unknown format");}
        }
    	if(debug){
    		System.out.println("Matrix read is :");
    		for(int i=0;i<3;i++){
				System.out.println("[ "+tr.get(i,0)+"  "+tr.get(i,1)+"  "+tr.get(i,2)+"  "+tr.get(i,3)+" ]");
        	}
			System.out.println("");
    	}			        
  		return tr;
	}

	public void writeMatrixToFile(String filePath,Transform tr) {
		File f = new File(filePath);
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			out.write("# Affine Transformation written in Transform_IO style, but inverted for direct processing of resampling\n");
			out.write("# at "+(new Date())+"\n");
			out.write(tr.get(0,0)+" "+tr.get(0,1)+" "+tr.get(0,2)+" "+tr.get(0,3)+"\n"+tr.get(1,0)+" "+tr.get(1,1)+" "+tr.get(1,2)+" "+tr.get(1,3)+"\n"
					 +tr.get(2,0)+" "+tr.get(2,1)+" "+tr.get(2,2)+" "+tr.get(2,3)+"\n"+tr.get(3,0)+" "+tr.get(3,1)+" "+tr.get(3,2)+" "+tr.get(3,3)+"\n");
			out.close();
		} catch (Exception e) {
			IJ.error("Unable to write transformation to file: "+f.getAbsolutePath()+"error: "+e);
		}
	}
	
	public void adjustImageCalibration(ImagePlus img,double []voxSize,String unit) {
		if(img==null)return;
		img.getCalibration().setUnit(unit);
		Calibration cal = img.getCalibration();			
		cal.pixelWidth =voxSize[0];
		cal.pixelHeight =voxSize[1];
		cal.pixelDepth =voxSize[2];
	}
	
	public double getVoxelVolume(ImagePlus img) {
		return img.getCalibration().pixelDepth*img.getCalibration().pixelWidth*img.getCalibration().pixelHeight;
	}
	
	
	public double[][] computeTransformationForBoutureAlignment(ImagePlus img,boolean computeZaxisOnly,double[][]cornersCoordinates){
		Analyze anna=Vitimage_Toolbox.anna;
		//new Duplicator().run(img,1,img.getStackSize()).show();
		anna.remember("Entree dans calcul de transformation", "computeZaxisOnly ? "+computeZaxisOnly+"");
		anna.storeEntryImage(img,"Image sur laquelle l'axe va etre calculé");
		boolean debug=true;
		double xFinal=255;
		double yFinal=380;
		double zFinal=20;
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
				anna.notifyStep("Lissage 3D puis reduction à 8 bits","facteurs de lisage (en pixels)=("+8*0.035/vX+" , "+8*0.035/vY+" , "+2*0.5/vZ+")");
		GaussianBlur3D.blur(img,8*0.035/vX,8*0.035/vY,2*0.5/vZ);
				anna.storeImage(img, "resultat lissage gaussien");
				anna.rememberImageDynamic(img,"resulat lissage gaussien");
		StackConverter sc=new StackConverter(img);
		sc.convertToGray8();
				anna.storeImage(img, "resultat conversion 8 bits");
				anna.rememberImageDynamic(img,"resulat conversion 8 bits");

		//Step 2 : apply automatic threshold
				anna.notifyStep("Seuillage automatique","seuil impossible à determiner car methode de Otsu automatique appliquee");
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
				anna.remember("Valeurs calculees","zQuarter="+zQuarter+" , zVentile="+zVentile);
		for(int i=0;i<zVentile;i++) {
			stackUp.addSlice("",img.getStack().getProcessor(zMax/2+zQuarter-zVentile+1+i));//de zmax/2 à zMax/2 + 5 --> ajouter zMax/2 à la fin			
			stackDown.addSlice("",img.getStack().getProcessor(zMax/2-zQuarter+1+i+1));//de zmax/2-5 à zMax/2   --> ajouter zMax/2-5 à la fin
		}
		ImagePlus imgUp=new ImagePlus("upMASK",stackUp);
		ImagePlus imgUpCon=connexe(imgUp,0,29,0,10E10,6,2);
				anna.storeImage(imgUp, "Masque de la partie superieure");
				anna.rememberImageDynamic(imgUp," partie superieure");
				anna.storeImage(imgUp, "Connexe de la partie superieure");
				anna.rememberImageDynamic(imgUpCon," connexe partie superieure");
				
		ImagePlus imgDown=new ImagePlus("downMASK",stackDown);
		ImagePlus imgDownCon=connexe(imgDown,0,29,0,10E10,6,2);
				anna.storeImage(imgDown, "Masque de la partie inferieure");
				anna.rememberImageDynamic(imgUp," partie inferieure");
				anna.storeImage(imgUp, "Connexe de la partie inferieure");
				anna.rememberImageDynamic(imgUpCon," connexe partie inferieure");

		
		
		//Step 3 : compute the two centers of mass
					anna.notifyStep("Calcul des centres de masse","");
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

		anna.remember("Points et centre de masse up (en pixels)","Points="+hitsUp+" Center=("+xMoyUp+" , "+yMoyUp+" , "+zMoyUp+")");
		anna.remember("Points et centre de masse down (en pixels)","Points="+hitsDown+" Center=("+xMoyDown+" , "+yMoyDown+" , "+zMoyDown+")");
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
		anna.remember("Points et centre de masse up (en real coordinates)","Points="+hitsUp+" Center=("+xMoyUp+" , "+yMoyUp+" , "+zMoyUp+")");
		anna.remember("Points et centre de masse down (en real coordinates)","Points="+hitsDown+" Center=("+xMoyDown+" , "+yMoyDown+" , "+zMoyDown+")");
		if(debug) {
			System.out.println("Center of mass up (real coordinates)= "+xMoyUp+"  ,  "+yMoyUp+"  ,  "+zMoyUp);
			System.out.println("Center of mass down (real coordinates)= "+xMoyDown+"  ,  "+yMoyDown+"  ,  "+zMoyDown);
		}
		
		//Step 4 : compute the new basis
		anna.notifyStep("Calcul de la nouvelle base", "pour la transformation");
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
			zP=(xMoyUp+xMoyDown)/2.0;
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
		anna.remember("Points d'inoculation choisi (en real coordinates)","=("+xP+" , "+yP+" , "+zP+")");
		
		
		//Center of tissue to PI, make the second vector
		vectYinit[0]=xP-xMoyDown;
		vectYinit[1]=yP-yMoyDown;
		vectYinit[2]=zP-zMoyDown;

		//"Graham-schmidt orthogonalisation" (Not exactly, as we use Y and Z to produce X)

		//E3=v3/norm(v3)
		anna.remember(stringVector(vectZinit,"V3"),"");
		vectZfin=normalize(vectZinit);
		anna.remember(stringVector(vectZinit,"E3"),"");

		if(debug) printVector(vectZinit,"v3=");
		if(debug) printVector(vectZfin,"E3=");
		//u2=v2-proj_u3_of_v2  ; E2=v2/norm(v2)
		if(debug) printVector(vectYinit,"v2=");
		anna.remember(stringVector(vectYinit,"V2"),"");
		vectYfin=vectorialSubstraction(vectYinit,proj_u_of_v(vectZinit,vectYinit));
		vectYfin=normalize(vectYfin);
		if(debug) printVector(vectYfin,"E2=");
		anna.remember(stringVector(vectYfin,"E2"),"");

		//For E1
		vectXfin=vectorialProduct(vectYfin,vectZfin);
		if(debug) printVector(vectXfin,"E1=");
		anna.remember(stringVector(vectXfin,"E1"),"");




		//Retrieve the translation parameters 
		//We want that through the transformation, the inoculation point get the coordinates : xFinal,yFinal,zFinal.
		//That stands for that experiment in which all the data lies in 512 x 512 x 40 stacks.
		//If one of the dimensions differs a lot, it should propose another strategy. 
		anna.notifyStep("Calcul de la translation restante", "");
		if((xMax != 512) || (yMax != 512) || (zMax < 38) || (zMax > 42)){//Alert
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
		xFinal=xFinal*vX+vX/2;	
		yFinal=yFinal*vY+vY/2;	
		zFinal=zFinal*vZ+vZ/2;	
		
		double trans[]=new double[3];		
		
		trans[0]=xP-vectXfin[0]*xFinal-vectYfin[0]*yFinal-vectZfin[0]*zFinal;
		trans[1]=yP-vectXfin[1]*xFinal-vectYfin[1]*yFinal-vectZfin[1]*zFinal;
		trans[2]=zP-vectXfin[2]*xFinal-vectYfin[2]*yFinal-vectZfin[2]*zFinal;
		if(debug) System.out.println("Translation : ["+trans[0]+" , "+trans[1]+" , "+trans[2]+"]");
		anna.remember(stringVector(new double[] {trans[0],trans[1],trans[2]},"Translation"),"");
		
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
		anna.remember(stringMatrix(" Matrice complete obtenue en fin de fonction",transformToArray(tr)),"");
		return(new double[][] {transformToArray(tr),transformToArray(trInv) });	
	}
	
	
	
	
	
	public ImagePlus[] reech3D(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title,boolean computeMask) {
		 ImagePlus[]ret;
		 if(computeMask) {
			 ret=new ImagePlus[2];
			 ImagePlus maskIn=ij.gui.NewImage.createImage("Mask",sizes[0],sizes[1],sizes[2],8,ij.gui.NewImage.FILL_WHITE);
			 ret[1]=reech3DByte(maskIn,matInput,sizes,"Mask_"+title);
		 }
		 else ret=new ImagePlus[1];
		 if (imPlusFlo.getType()==ImagePlus.GRAY8)ret[0]=reech3DByte(imPlusFlo,matInput,sizes,title);
		 else if (imPlusFlo.getType()==ImagePlus.GRAY16)ret[0]=reech3DShort(imPlusFlo,matInput,sizes,title);
		 else if (imPlusFlo.getType()==ImagePlus.GRAY32)ret[0]=reech3DFloat(imPlusFlo,matInput,sizes,title);	
		 else {
			 System.out.println("Type not handled yet");return null;
		 }
		 return ret;
	 }
	/**
	* Execute the plugin functionality: resampling of an image, according to eventual transformations and scalings changes
	*
	* This plugin handle the most general case (whereas it handles simpler cases) in which we have an image in a geometry G1
	* that is resampled in a geometry G2 with a lower resolution in order to be registered to another one,
	* and then resampled in a geometry G3 for visualization purpose
	* 
	* This plugin is based on the C function reech3D, implemented by Grégoire Malandain (greg@sophia.inria.fr)
	* @return the resampled ImagePlus.
	*/
    public ImagePlus reech3DByte(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title) {
		//Input and Output images opening
		//~ ImagePlus imPlusRec=new ImagePlus(sizes[0],sizes[1],sizes[2]);
		ImagePlus imPlusRec=ij.gui.NewImage.createImage(title,sizes[0],sizes[1],sizes[2],8,ij.gui.NewImage.FILL_BLACK);
		ImageStack stackIn = imPlusFlo.getStack();
		ImageStack stackOut = imPlusRec.getStack();

		//Dimensions
		int zMaxIn = stackIn.getSize();
		int yMaxIn=imPlusFlo.getHeight();
		int xMaxIn=imPlusFlo.getWidth();
		
		int zMaxOut=stackOut.getSize();
		int yMaxOut=imPlusRec.getHeight();
		int xMaxOut=imPlusRec.getWidth();

		// Allows images access
		byte[][] imgIn= new byte[zMaxIn][];
		byte[][] imgOut= new byte[zMaxOut][];
        for(int z=1;z<=zMaxIn;z++){imgIn[z-1]=(byte[])(stackIn.getProcessor(z).getPixels());}
        for(int z=1;z<=zMaxOut;z++){imgOut[z-1]=(byte[])(stackOut.getProcessor(z).getPixels());}

		// Gregoire Malandain's C algorithm variables
		int i, j, k, ix, iy, iz;
		double x, y, z, dx, dy, dz, dxdy,dxdz,dydz,dxdydz;
		double res;
		double v6, v5, v4;
		int rdimx=xMaxOut, rdimy=yMaxOut, rdimz=zMaxOut;
		int tdimx=xMaxIn, tdimy=yMaxIn, tdimz=zMaxIn;
		//int tdimxy=tdimx*tdimy;
		int t1dimx=tdimx-1, t1dimy=tdimy-1, t1dimz=tdimz-1;
		double ddimx = (double)tdimx-0.5, ddimy = (double)tdimy-0.5;
		double ddimz = (double)tdimz-0.5;

		double [] mat=matInput;

		//Let's go!
		//For all voxels in the resulting image
		for ( k = 0; k < rdimz; k ++ ) {
			if((k>10) && (k%(rdimz/10)==0))System.out.println("Computing resulting image : slice, " + k +" / " + rdimz);
			for ( j = 0; j < rdimy; j ++ ) {
				for ( i = 0; i < rdimx; i ++) {
					
					/* Computed the location of the corresponding point in the original floating image */
					x = mat[0] * i +  mat[1] * j + mat[2] * k + mat[3];
					if ((x < -0.5) || ( x > ddimx)) { imgOut[k][i + xMaxOut * j]= 0xff & 0 ; continue; }
					y = mat[4] * i +  mat[5] * j + mat[6] * k + mat[7];
					if ((y < -0.5) || ( y > ddimy)) { imgOut[k][i + xMaxOut * j]= 0xff & 0; continue; }
					z = mat[8] * i +  mat[9] * j + mat[10] * k + mat[11];
					if ((z < -0.5) || ( z > ddimz)) { imgOut[k][i + xMaxOut * j]= 0xff & 0; continue; }
					
					/* here, the point lies on the borders or completely inside
					the image */
					ix = (int)x;
					iy = (int)y;
					iz = (int)z;


					/* If we are not in the border */
					if ( (x > 0.0) && (ix < t1dimx) &&
					(y > 0.0) && (iy < t1dimy) &&
					(z > 0.0) && (iz < t1dimz) ) {
						/* the corresponding point is in the box defined
						by (ix[+1],iy[+1],iz[+1]) */
						dx = x - ix;
						dy = y - iy;
						dz = z - iz;
						dxdy = dx*dy;
						dxdz = dx*dz;
						dydz = dy*dz;
						dxdydz = dxdy*dz;

						/* we have
						v[7]=dxdydz;            coefficient of tbuf(ix+1,iy+1,iz+1)
						v[6]=dxdz-dxdydz;       coefficient of tbuf(ix+1,iy,  iz+1)
						v[5]=dxdy-dxdydz;       coefficient of tbuf(ix+1,iy+1,iz  )
						v[4]=dx-dxdy-v[6];      coefficient of tbuf(ix+1,iy  ,iz  )
						v[3]=dydz-dxdydz;       coefficient of tbuf(ix  ,iy+1,iz+1)
						v[2]=dz-dydz-v[6];      coefficient of tbuf(ix  ,iy  ,iz+1)
						v[1]=dy-dydz-v[5];      coefficient of tbuf(ix  ,iy+1,iz  )
						v[0]=1-dy-dz+dydz-v[4]; coefficient of tbuf(ix  ,iy  ,iz  )
						*/
						v6 = dxdz-dxdydz;
						v5 = dxdy-dxdydz;
						v4 = dx-dxdy-v6;

						res = dxdydz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix+1,iy+1,iz+1) */

						res += (dydz-dxdydz) * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix  ,iy+1,iz+1) */
						
						res += v6 * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xff));
						/* tbuf(ix+1  ,iy,  iz+1) */

						res += (dz-dydz-v6) * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));
						/* tbuf(ix  ,iy  ,iz+1) */

						res += v5 * ((int)(imgIn[iz][ix+1 + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix+1,iy+1,iz  ) */
						
						res += (dy-dydz-v5) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix  ,iy+1,iz  ) */

						res += v4 * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));
						/* tbuf(ix+1,iy  ,iz  ) */

						res += (1-dy-dz+dydz-v4) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff));
						/* tbuf(ix  ,iy  ,iz  ) */

						imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					
					
					/* here, we are sure we are on some border */
					//~ tpt += ix + iy * tdimx + iz * tdimxy;
 					if ( (x < 0.0) || (ix == t1dimx) ) {
						if ( (y < 0.0) || (iy == t1dimy) ) {
							if ( (z < 0.0) || (iz == t1dimz) ) {							
								//In a XYZ corner
								imgOut[k][i + xMaxOut * j] = (byte)(0xff & imgIn[iz][ix + xMaxIn * iy]);
								continue;
							}
							
							// On a XY-edge
							dz = z - iz;
							res  = (1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff));/* (1-dz)* tbuf(ix,iy,iz) */
							res += dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));/* dz * tbuf(ix,iy,iz+1) */
							
							imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						dy = y - iy;
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a XZ-edge
							res  = (1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* (1-dy)* tbuf(ix,iy,iz) */
							res += dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));     /* dy * tbuf(ix,iy+1,iz) */
							imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a X-face
						dz = z - iz;
						res = (1-dy)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
						res += dy*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));    /* tbuf(ix,iy+1,iz) */
						res += (1-dy)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));    /* tbuf(ix,iy,iz+1) */
						res += dy*dz * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xff));        /* tbuf(ix,iy+1,iz+1) */
						imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					/* here we are sure that the border is either
					along the Y or the Z axis */
					dx = x - ix;
					if ( (y < 0.0) || (iy == t1dimy) ) {
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a YZ-edge
							res = (1-dx) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
							res += dx * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));    /* tbuf(ix+1,iy,iz) */
							imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a Y-face
						dz = z - iz;
						res = (1-dx)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
						res += dx*(1-dz) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));    /* tbuf(ix+1,iy,iz) */
						res += (1-dx)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));    /* tbuf(ix,iy,iz+1) */
						res += dx*dz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xff));        /* tbuf(ix+1,iy,iz+1) */
						imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					//On a Z-face
					dy = y - iy;
					res = (1-dx)*(1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
					res += dx*(1-dy) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));    /* tbuf(ix+1,iy,iz) */
					res += (1-dx)*dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));    /* tbuf(ix,iy+1,iz) */
					res += dx*dy * ((int)(imgIn[iz][ix+1 + xMaxIn * iy+1] & 0xff));        /* tbuf(ix+1,iy+1,iz) */
					imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
				}
			}								
		}			
		return imPlusRec;
    }
    
	public ImagePlus reech3DShort(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title) {
		//Input and Output images opening
		//~ ImagePlus imPlusRec=new ImagePlus(sizes[0],sizes[1],sizes[2]);
		ImagePlus imPlusRec=ij.gui.NewImage.createImage(title,sizes[0],sizes[1],sizes[2],16,ij.gui.NewImage.FILL_BLACK);
		ImageStack stackIn = imPlusFlo.getStack();
		ImageStack stackOut = imPlusRec.getStack();

		//Dimensions
		int zMaxIn = stackIn.getSize();
		int yMaxIn=imPlusFlo.getHeight();
		int xMaxIn=imPlusFlo.getWidth();
		
		int zMaxOut=stackOut.getSize();
		int yMaxOut=imPlusRec.getHeight();
		int xMaxOut=imPlusRec.getWidth();

		// Allows images access
		short[][] imgIn= new short[zMaxIn][];
		short[][] imgOut= new short[zMaxOut][];
        for(int z=1;z<=zMaxIn;z++){imgIn[z-1]=(short[])(stackIn.getProcessor(z).getPixels());}
        for(int z=1;z<=zMaxOut;z++){imgOut[z-1]=(short[])(stackOut.getProcessor(z).getPixels());}

		// Gregoire Malandain's C algorithm variables
		int i, j, k, ix, iy, iz;
		double x, y, z, dx, dy, dz, dxdy,dxdz,dydz,dxdydz;
		double res;
		double v6, v5, v4;
		int rdimx=xMaxOut, rdimy=yMaxOut, rdimz=zMaxOut;
		int tdimx=xMaxIn, tdimy=yMaxIn, tdimz=zMaxIn;
		//int tdimxy=tdimx*tdimy;
		int t1dimx=tdimx-1, t1dimy=tdimy-1, t1dimz=tdimz-1;
		double ddimx = (double)tdimx-0.5, ddimy = (double)tdimy-0.5;
		double ddimz = (double)tdimz-0.5;

		double [] mat=matInput;

		//Let's go!
		//For all voxels in the resulting image
		for ( k = 0; k < rdimz; k ++ ) {
			if((k>10) && (k%(rdimz/10)==0))System.out.println("Computing resulting image : slice, " + k +" / " + rdimz);
			for ( j = 0; j < rdimy; j ++ ) {
				for ( i = 0; i < rdimx; i ++) {
					
					/* Computed the location of the corresponding point in the original floating image */
					x = mat[0] * i +  mat[1] * j + mat[2] * k + mat[3];
					if ((x < -0.5) || ( x > ddimx)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0 ; continue; }
					y = mat[4] * i +  mat[5] * j + mat[6] * k + mat[7];
					if ((y < -0.5) || ( y > ddimy)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					z = mat[8] * i +  mat[9] * j + mat[10] * k + mat[11];
					if ((z < -0.5) || ( z > ddimz)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					
					/* here, the point lies on the borders or completely inside
					the image */
					ix = (int)x;
					iy = (int)y;
					iz = (int)z;


					/* If we are not in the border */
					if ( (x > 0.0) && (ix < t1dimx) &&
					(y > 0.0) && (iy < t1dimy) &&
					(z > 0.0) && (iz < t1dimz) ) {
						/* the corresponding point is in the box defined
						by (ix[+1],iy[+1],iz[+1]) */
						dx = x - ix;
						dy = y - iy;
						dz = z - iz;
						dxdy = dx*dy;
						dxdz = dx*dz;
						dydz = dy*dz;
						dxdydz = dxdy*dz;

						/* we have
						v[7]=dxdydz;            coefficient of tbuf(ix+1,iy+1,iz+1)
						v[6]=dxdz-dxdydz;       coefficient of tbuf(ix+1,iy,  iz+1)
						v[5]=dxdy-dxdydz;       coefficient of tbuf(ix+1,iy+1,iz  )
						v[4]=dx-dxdy-v[6];      coefficient of tbuf(ix+1,iy  ,iz  )
						v[3]=dydz-dxdydz;       coefficient of tbuf(ix  ,iy+1,iz+1)
						v[2]=dz-dydz-v[6];      coefficient of tbuf(ix  ,iy  ,iz+1)
						v[1]=dy-dydz-v[5];      coefficient of tbuf(ix  ,iy+1,iz  )
						v[0]=1-dy-dz+dydz-v[4]; coefficient of tbuf(ix  ,iy  ,iz  )
						*/
						v6 = dxdz-dxdydz;
						v5 = dxdy-dxdydz;
						v4 = dx-dxdy-v6;

						res = dxdydz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix+1,iy+1,iz+1) */

						res += (dydz-dxdydz) * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix  ,iy+1,iz+1) */
						
						res += v6 * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xffff));
						/* tbuf(ix+1  ,iy,  iz+1) */

						res += (dz-dydz-v6) * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));
						/* tbuf(ix  ,iy  ,iz+1) */

						res += v5 * ((int)(imgIn[iz][ix+1 + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix+1,iy+1,iz  ) */
						
						res += (dy-dydz-v5) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix  ,iy+1,iz  ) */

						res += v4 * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));
						/* tbuf(ix+1,iy  ,iz  ) */

						res += (1-dy-dz+dydz-v4) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff));
						/* tbuf(ix  ,iy  ,iz  ) */

						imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					
					
					/* here, we are sure we are on some border */
					//~ tpt += ix + iy * tdimx + iz * tdimxy;
 					if ( (x < 0.0) || (ix == t1dimx) ) {
						if ( (y < 0.0) || (iy == t1dimy) ) {
							if ( (z < 0.0) || (iz == t1dimz) ) {							
								//In a XYZ corner
								imgOut[k][i + xMaxOut * j] = (short)(0xffff & imgIn[iz][ix + xMaxIn * iy]);
								continue;
							}
							
							// On a XY-edge
							dz = z - iz;
							res  = (1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff));/* (1-dz)* tbuf(ix,iy,iz) */
							res += dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));/* dz * tbuf(ix,iy,iz+1) */
							
							imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						dy = y - iy;
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a XZ-edge
							res  = (1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* (1-dy)* tbuf(ix,iy,iz) */
							res += dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));     /* dy * tbuf(ix,iy+1,iz) */
							imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a X-face
						dz = z - iz;
						res = (1-dy)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
						res += dy*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));    /* tbuf(ix,iy+1,iz) */
						res += (1-dy)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));    /* tbuf(ix,iy,iz+1) */
						res += dy*dz * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xffff));        /* tbuf(ix,iy+1,iz+1) */
						imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					/* here we are sure that the border is either
					along the Y or the Z axis */
					dx = x - ix;
					if ( (y < 0.0) || (iy == t1dimy) ) {
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a YZ-edge
							res = (1-dx) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
							res += dx * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));    /* tbuf(ix+1,iy,iz) */
							imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a Y-face
						dz = z - iz;
						res = (1-dx)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
						res += dx*(1-dz) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));    /* tbuf(ix+1,iy,iz) */
						res += (1-dx)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));    /* tbuf(ix,iy,iz+1) */
						res += dx*dz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xffff));        /* tbuf(ix+1,iy,iz+1) */
						imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					//On a Z-face
					dy = y - iy;
					res = (1-dx)*(1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
					res += dx*(1-dy) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));    /* tbuf(ix+1,iy,iz) */
					res += (1-dx)*dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));    /* tbuf(ix,iy+1,iz) */
					res += dx*dy * ((int)(imgIn[iz][ix+1 + xMaxIn * iy+1] & 0xffff));        /* tbuf(ix+1,iy+1,iz) */
					imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
				}
			}								
		}			
		return imPlusRec;
    }
	
	public ImagePlus reech3DFloat(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title) {
		//Input and Output images opening
		//~ ImagePlus imPlusRec=new ImagePlus(sizes[0],sizes[1],sizes[2]);
		ImagePlus imPlusRec=ij.gui.NewImage.createImage(title,sizes[0],sizes[1],sizes[2],32,ij.gui.NewImage.FILL_BLACK);
		ImageStack stackIn = imPlusFlo.getStack();
		ImageStack stackOut = imPlusRec.getStack();

		//Dimensions
		int zMaxIn = stackIn.getSize();
		int yMaxIn=imPlusFlo.getHeight();
		int xMaxIn=imPlusFlo.getWidth();
		
		int zMaxOut=stackOut.getSize();
		int yMaxOut=imPlusRec.getHeight();
		int xMaxOut=imPlusRec.getWidth();

		// Allows images access
		float[][] imgIn= new float[zMaxIn][];
		float[][] imgOut= new float[zMaxOut][];
        for(int z=1;z<=zMaxIn;z++){imgIn[z-1]=(float[])(stackIn.getProcessor(z).getPixels());}
        for(int z=1;z<=zMaxOut;z++){imgOut[z-1]=(float[])(stackOut.getProcessor(z).getPixels());}

		// Gregoire Malandain's C algorithm variables
		int i, j, k, ix, iy, iz;
		double x, y, z, dx, dy, dz, dxdy,dxdz,dydz,dxdydz;
		double res;
		double v6, v5, v4;
		int rdimx=xMaxOut, rdimy=yMaxOut, rdimz=zMaxOut;
		int tdimx=xMaxIn, tdimy=yMaxIn, tdimz=zMaxIn;
		//int tdimxy=tdimx*tdimy;
		int t1dimx=tdimx-1, t1dimy=tdimy-1, t1dimz=tdimz-1;
		double ddimx = (double)tdimx-0.5, ddimy = (double)tdimy-0.5;
		double ddimz = (double)tdimz-0.5;

		double [] mat=matInput;

		//Let's go!
		//For all voxels in the resulting image
		for ( k = 0; k < rdimz; k ++ ) {
			if((k>10) && (k%(rdimz/10)==0))System.out.println("Computing resulting image : slice, " + k +" / " + rdimz);
			for ( j = 0; j < rdimy; j ++ ) {
				for ( i = 0; i < rdimx; i ++) {
					
					/* Computed the location of the corresponding point in the original floating image */
					x = mat[0] * i +  mat[1] * j + mat[2] * k + mat[3];
					if ((x < -0.5) || ( x > ddimx)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0 ; continue; }
					y = mat[4] * i +  mat[5] * j + mat[6] * k + mat[7];
					if ((y < -0.5) || ( y > ddimy)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					z = mat[8] * i +  mat[9] * j + mat[10] * k + mat[11];
					if ((z < -0.5) || ( z > ddimz)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					
					/* here, the point lies on the borders or completely inside
					the image */
					ix = (int)x;
					iy = (int)y;
					iz = (int)z;


					/* If we are not in the border */
					if ( (x > 0.0) && (ix < t1dimx) &&
					(y > 0.0) && (iy < t1dimy) &&
					(z > 0.0) && (iz < t1dimz) ) {
						/* the corresponding point is in the box defined
						by (ix[+1],iy[+1],iz[+1]) */
						dx = x - ix;
						dy = y - iy;
						dz = z - iz;
						dxdy = dx*dy;
						dxdz = dx*dz;
						dydz = dy*dz;
						dxdydz = dxdy*dz;

						/* we have
						v[7]=dxdydz;            coefficient of tbuf(ix+1,iy+1,iz+1)
						v[6]=dxdz-dxdydz;       coefficient of tbuf(ix+1,iy,  iz+1)
						v[5]=dxdy-dxdydz;       coefficient of tbuf(ix+1,iy+1,iz  )
						v[4]=dx-dxdy-v[6];      coefficient of tbuf(ix+1,iy  ,iz  )
						v[3]=dydz-dxdydz;       coefficient of tbuf(ix  ,iy+1,iz+1)
						v[2]=dz-dydz-v[6];      coefficient of tbuf(ix  ,iy  ,iz+1)
						v[1]=dy-dydz-v[5];      coefficient of tbuf(ix  ,iy+1,iz  )
						v[0]=1-dy-dz+dydz-v[4]; coefficient of tbuf(ix  ,iy  ,iz  )
						*/
						v6 = dxdz-dxdydz;
						v5 = dxdy-dxdydz;
						v4 = dx-dxdy-v6;

						res = dxdydz * ((float)(imgIn[iz+1][ix+1 + xMaxIn * (iy+1)]));
						/* tbuf(ix+1,iy+1,iz+1) */

						res += (dydz-dxdydz) * ((float)(imgIn[iz+1][ix + xMaxIn * (iy+1)]));
						/* tbuf(ix  ,iy+1,iz+1) */
						
						res += v6 * ((float)(imgIn[iz+1][ix+1 + xMaxIn * iy]));
						/* tbuf(ix+1  ,iy,  iz+1) */

						res += (dz-dydz-v6) * ((float)(imgIn[iz+1][ix + xMaxIn * iy] ));
						/* tbuf(ix  ,iy  ,iz+1) */

						res += v5 * ((float)(imgIn[iz][ix+1 + xMaxIn * (iy+1)]));
						/* tbuf(ix+1,iy+1,iz  ) */
						
						res += (dy-dydz-v5) * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));
						/* tbuf(ix  ,iy+1,iz  ) */

						res += v4 * ((float)(imgIn[iz][ix+1 + xMaxIn * iy]));
						/* tbuf(ix+1,iy  ,iz  ) */

						res += (1-dy-dz+dydz-v4) * ((float)(imgIn[iz][ix + xMaxIn * iy]));
						/* tbuf(ix  ,iy  ,iz  ) */

						imgOut[k][i + xMaxOut * j] = (float)res;
						continue;
					}
					
					
					
					/* here, we are sure we are on some border */
					//~ tpt += ix + iy * tdimx + iz * tdimxy;
 					if ( (x < 0.0) || (ix == t1dimx) ) {
						if ( (y < 0.0) || (iy == t1dimy) ) {
							if ( (z < 0.0) || (iz == t1dimz) ) {							
								//In a XYZ corner
								imgOut[k][i + xMaxOut * j] = imgIn[iz][ix + xMaxIn * iy];
								continue;
							}
							
							// On a XY-edge
							dz = z - iz;
							res  = (1-dz) * ((float)(imgIn[iz][ix + xMaxIn * iy]));/* (1-dz)* tbuf(ix,iy,iz) */
							res += dz * ((float)(imgIn[iz+1][ix + xMaxIn * iy]));/* dz * tbuf(ix,iy,iz+1) */
							
							imgOut[k][i + xMaxOut * j] = (float)res;
							continue;
						}
						
						dy = y - iy;
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a XZ-edge
							res  = (1-dy) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* (1-dy)* tbuf(ix,iy,iz) */
							res += dy * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));     /* dy * tbuf(ix,iy+1,iz) */
							imgOut[k][i + xMaxOut * j] = (float)res;
							continue;
						}
						
						//On a X-face
						dz = z - iz;
						res = (1-dy)*(1-dz) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
						res += dy*(1-dz) * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));    /* tbuf(ix,iy+1,iz) */
						res += (1-dy)*dz * ((float)(imgIn[iz+1][ix + xMaxIn * iy]));    /* tbuf(ix,iy,iz+1) */
						res += dy*dz * ((float)(imgIn[iz+1][ix + xMaxIn * (iy+1)]));        /* tbuf(ix,iy+1,iz+1) */
						imgOut[k][i + xMaxOut * j] = (float)res;
						continue;
					}
					
					/* here we are sure that the border is either
					along the Y or the Z axis */
					dx = x - ix;
					if ( (y < 0.0) || (iy == t1dimy) ) {
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a YZ-edge
							res = (1-dx) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
							res += dx * ((float)(imgIn[iz][ix+1 + xMaxIn * iy]));    /* tbuf(ix+1,iy,iz) */
							imgOut[k][i + xMaxOut * j] =  (float)res;
							continue;
						}
						
						//On a Y-face
						dz = z - iz;
						res = (1-dx)*(1-dz) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
						res += dx*(1-dz) * ((float)(imgIn[iz][ix+1 + xMaxIn * iy] ));    /* tbuf(ix+1,iy,iz) */
						res += (1-dx)*dz * ((float)(imgIn[iz+1][ix + xMaxIn * iy] ));    /* tbuf(ix,iy,iz+1) */
						res += dx*dz * ((float)(imgIn[iz+1][ix+1 + xMaxIn * iy] ));        /* tbuf(ix+1,iy,iz+1) */
						imgOut[k][i + xMaxOut * j] =(float)res;
						continue;
					}
					
					//On a Z-face
					dy = y - iy;
					res = (1-dx)*(1-dy) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
					res += dx*(1-dy) * ((float)(imgIn[iz][ix+1 + xMaxIn * iy]));    /* tbuf(ix+1,iy,iz) */
					res += (1-dx)*dy * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));    /* tbuf(ix,iy+1,iz) */
					res += dx*dy * ((float)(imgIn[iz][ix+1 + xMaxIn * iy+1] ));        /* tbuf(ix+1,iy+1,iz) */
					imgOut[k][i + xMaxOut * j] = (float)0;
				}
			}								
		}			
		return imPlusRec;
    }

	
	
	

	ImagePlus connexe(ImagePlus img,double threshLow,double threshHigh,double volumeLow,double volumeHigh,int connexity,int selectByVolume) {
		boolean debug=true;
		if(debug)System.out.println("Depart connexe");
		int yMax=img.getHeight();
		int xMax=img.getWidth();
		int zMax=img.getStack().getSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		double voxVolume=vX*vY*vZ;
		int[][][]tabIn=new int[xMax][yMax][zMax];
		int[][]connexions=new int[xMax*yMax*zMax*3][2];
		int[]volume=new int[xMax*yMax*zMax];
		int[][]neighbours=new int[][]{{1,0,0,0},{1,1,0,0},{0,1,0,0},{0,0,1,0},{1,0,1,0},{1,1,1,0},{1,1,0,0} };
		int curComp=0;
		int indexConnexions=0;
		if(debug)System.out.println("Choix d'un type");
		switch(img.getType()) {
		case ImagePlus.GRAY8:
			byte[] imgInB;
			for(int z=0;z<zMax;z++) {
				imgInB=(byte[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInB[x+xMax*y] & 0xff) < threshHigh+1 )  && ((float)((imgInB[x+xMax*y]) & 0xff) > threshLow-1) )tabIn[x][y][z]=-1;
			}
			break;
		case ImagePlus.GRAY16:
			short[] imgInS;
			for(int z=0;z<zMax;z++) {
				imgInS=(short[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((float)(imgInS[x+xMax*y] & 0xffff) < threshHigh+1 )  && ((float)((imgInS[x+xMax*y]) & 0xffff) > threshLow-1) )tabIn[x][y][z]=-1;					
			}
			break;
		case ImagePlus.GRAY32:
			float[] imgInF;
			for(int z=0;z<zMax;z++) {
				imgInF=(float[])(img.getStack().getProcessor(z+1).getPixels());
				for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++)if(  ((imgInF[x+xMax*y]) < threshHigh+1 )  && (((imgInF[x+xMax*y])) > threshLow-1) )tabIn[x][y][z]=-1;					
			}
			break;
		}
		
		if(debug)System.out.println("Boucle principale");
		//Boucle principale
		for(int x=0;x<xMax;x++) {
			for(int y=0;y<yMax;y++) {
				for(int z=0;z<zMax;z++) {
					if(tabIn[x][y][z]==0)continue;//Point du fond
					if(tabIn[x][y][z]==-1) {
						tabIn[x][y][z]=(++curComp);//New object
						volume[curComp]++;
					}
					if(tabIn[x][y][z]>0) {//Here we need to explore the neighbours
						for(int nei=0;nei<7;nei++)neighbours[nei][3]=1;//At the beginning, every neighbour is possible. 
						//Then we need to reduce access according to images dims and chosen connexity
						if(x==xMax-1)neighbours[0][3]=neighbours[1][3]=neighbours[4][3]=neighbours[5][3]=0;
						if(y==yMax-1)neighbours[1][3]=neighbours[2][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(z==zMax-1)neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==4)neighbours[1][3]=neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==6)neighbours[1][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==8)neighbours[3][3]=neighbours[4][3]=neighbours[5][3]=neighbours[6][3]=0;
						if(connexity==18)neighbours[5][3]=0;

						//Given these neighbours, we can visit them
						for(int nei=0;nei<7;nei++) {
							if(neighbours[nei][3]==1) {
								if(tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]==0)continue;
								if(tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]==-1) {
									tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]]=tabIn[x][y][z];
									volume[tabIn[x][y][z]]++;
								}
								else {
									connexions[indexConnexions][0]=tabIn[x+neighbours[nei][0]][y+neighbours[nei][1]][z+neighbours[nei][2]];
									connexions[indexConnexions++][1]=tabIn[x][y][z];
								}
							}
						}
					}
				}	
			}			
		}
		
		if(debug)System.out.println("Resolution des conflits entre groupes connexes");
		//Resolution des groupes d'objets connectes entre eux (formes en U, et cas plus compliqués)
		int[]lut = resolveConnexitiesGroupsAndExclude(connexions,indexConnexions,curComp+1,volume,volumeLow/voxVolume,volumeHigh/voxVolume,selectByVolume);
		
		
		//Build computed image of objects
		ImagePlus imgOut=ij.gui.NewImage.createImage(img.getShortTitle()+"_"+connexity+"CON",xMax,yMax,zMax,16,ij.gui.NewImage.FILL_BLACK);
		short[] imgOutTab;
		for(int z=0;z<zMax;z++) {
			imgOutTab=(short[])(imgOut.getStack().getProcessor(z+1).getPixels());
			for(int x=0;x<xMax;x++)for(int y=0;y<yMax;y++) imgOutTab[x+xMax*y] = (short) ( lut[tabIn[x][y][z]]  );
		}
		imgOut.getProcessor().setMinAndMax(0,lut[curComp+1]);
		return imgOut;	
	}
	
	public void testConnexe(ImagePlus img,int thresh,int volMin,int con) {
		ImagePlus out=connexe(img,thresh,10E32,volMin,1000000000,con,0);
		out.show();
//		IJ.setMinAndMax(min, max);
	}
	
	public void testResolve() {
		int[][]connexions=new int[][]{{1,2},{3,4},{4,5},{2,6},{1,1}};
		int nbCouples=5;
		int n=7;
		int []volumes= {0,20,40,80,160,320,640};
		int volMin = 100;
		int volMax = 10000;
		int[]result=resolveConnexitiesGroupsAndExclude(connexions,nbCouples,n,volumes,volMin,volMax,0);
	}
	
	public int[] resolveConnexitiesGroupsAndExclude(int  [][] connexions,int nbCouples,int n,int []volume,double volumeLowP,double volumeHighP,int selectByVolume) {
		boolean debug=false;
		int[]prec=new int[n];
		int[]lut=new int[n+1];
		int[]next=new int[n];
		int[]label=new int[n];
		if(debug)System.out.println("");
		if(debug)System.out.println("C'est parti, avec n="+n);
		for(int i=0;i<n;i++) {label[i]=i;prec[i]=0;next[i]=0;}
		
		int indA,indB,valMin,valMax,indMin,indMax;
		for(int couple=0;couple<nbCouples;couple++) {
			if(debug)System.out.println("Lecture couple "+couple);
			indA=connexions[couple][0];
			indB=connexions[couple][1];
			if(debug)System.out.println("--indA="+indA+" et indB="+indB);
			if(debug)System.out.println("--valA="+label[indA]+" et valB="+label[indB]);
			if(label[indA]==label[indB])continue;
			if(debug)System.out.println("--toujours dans la boucle. Recherche du maximum parmi les deux");
			if(label[indA]<label[indB]) {
				if(debug)System.out.println("-----cas 1");
				valMin=label[indA];
				indMin=indA;
				valMax=label[indB];
				indMax=indB;
			}
			else {
				if(debug)System.out.println("-----cas 2");
				valMin=label[indB];
				indMin=indB;
				valMax=label[indA];
				indMax=indA;
			}
			if(debug)System.out.println("--au sortir de la recherche de min max");
			if(debug)System.out.println("----indMin="+indMin+" et indMax="+indMax);
			if(debug)System.out.println("----valMin="+label[indMin]+" et valMax="+label[indMax]);
			if(debug)System.out.println("-Entree dans while 1");			
			while(next[indMin]>0)indMin=next[indMin];
			if(debug)System.out.println("---sortie du while 1, next[next[next[indMin]]]="+indMin);
			if(debug)System.out.println("-Entree dans while 2");			
			while(prec[indMax]>0)indMax=prec[indMax];
			if(debug)System.out.println("---sortie du while 2, prec[prec[prec[indMax]]]="+indMax);
			prec[indMax]=indMin;
			next[indMin]=indMax;
			if(debug)System.out.println("--Association effectuee. Application en boucle");
			while(next[indMin]>0) {
				indMin=next[indMin];
				label[indMin]=valMin;
			}
			if(debug)System.out.println("--Sortie while 3, dernier indice ayant recu : "+indMin+", qui a recu comme les autres "+valMin);
		}

		
		System.out.println("");
		if(debug)System.out.println("Partie 2 : collecte les volumes");
		//Compute number of objects and volume
		for (int i=1;i<n ;i++){
			if(debug)System.out.println("--Element "+i+" de volume "+volume[i]);
			if(label[i]!=i) {
				if(debug)System.out.println("----Element fusionne a l element "+label[i]+" qui avait un volume de "+volume[label[i]]);
				volume[label[i]]+=volume[i];
				volume[i]=0;
			}
		}
		//copy and sort volumes
		Object [][]tabSort=new Object[n][2];
		int selectedIndex=0;
		for (int i=0;i<n ;i++) {
			tabSort[i][0]=new Double(volume[i]);
			tabSort[i][1]=new Integer(i);
		}

		
		Arrays.sort(tabSort,new VolumeComparator());
		if(selectByVolume>n)selectByVolume=n;
		if(selectByVolume<1)selectByVolume=0;
		if(selectByVolume!=0)selectedIndex=((Integer)(tabSort[n-selectByVolume][1])).intValue();
		
		//Exclude too big or too small objects,
		System.out.println("");
		if(debug)System.out.println("Partie 3 : Exclure par volume et attribuer les luts");
		int displayedValue=1;
		for (int i=1;i<n ;i++){
			if(debug)System.out.println("--Element "+i+" de label "+label[i]+" et de volume "+volume[i]);
			if(selectByVolume!=0) {
				if(i==selectedIndex)lut[i]=255;
				else lut[i]=0;
			}
			else if( (volume[i]>0) && (volume[i]>=volumeLowP) && (volume[i]<=volumeHighP) ) {
				if(debug)System.out.println("On lui dedie une teinte de la LUT. Ce sera la teinte "+displayedValue);
				lut[i]=displayedValue++;
			}
		}
		if(displayedValue>65000) {System.out.println("Warning : "+(displayedValue-1)+" connected components");}
		else System.out.println("Number of connected components detected : "+(selectByVolume>0 ? 1 : (displayedValue-1)));

		//Group labels
		if(debug)System.out.println("Partie 4 : Chacun prend sa lut");
		for (int i=0;i<n ;i++){
			lut[i]=lut[label[i]];
			if(debug)System.out.println("----Element traite :"+i+" recoit la teinte "+lut[i]);
		}
		//Tricky little parameters to provide a good display after operation;
		if(selectByVolume !=0)lut[n]=255;
		else lut[n]=displayedValue;
		return lut;
	}
	
	
	
	
	
	public double[] detectInoculationPoint(ImagePlus img) {
		int xMax=img.getWidth();
		int yMax=img.getHeight();
		int zMax=img.getStackSize();
		double vX=img.getCalibration().pixelWidth;
		double vY=img.getCalibration().pixelHeight;
		double vZ=img.getCalibration().pixelDepth;
		int facteurAniso=(int)Math.round(vZ/vX);
		double IPStdZSize=4; //mm
		double IPStdXSize=2; //mm
		double sigmaXY=IPStdXSize/2.0;
		double sigmaZ=IPStdXSize/4.0;
		int sigmaPlotZ=5;
		double sigmaXYInPixels=10;//sigmaXY/vX;
		double sigmaZInPixels=0.2;//sigmaZ/vZ;
		int minPossibleZ=zMax/4;
		int maxPossibleZ=(zMax*3)/4;
		System.out.println("\nBlur");
		IJ.run(img, "Gaussian Blur 3D...", "x="+sigmaXYInPixels+" y="+sigmaXYInPixels+" z="+sigmaZInPixels+"");
		System.out.println(" Ok.");

		
		ImagePlus imgSlice= new Duplicator().run(img, minPossibleZ,minPossibleZ);
		System.out.println("\nOutline detection ...");
		IJ.setAutoThreshold(imgSlice, "Default dark");
		Prefs.blackBackground = true;
		IJ.run(imgSlice, "Convert to Mask", "method=Default background=Dark calculate black");
		for(int er=0;er<6;er++)IJ.run(imgSlice, "Erode", "stack");
		IJ.run(imgSlice, "Outline", "stack");
		ImagePlus imgOutline= new Duplicator().run(imgSlice, 1,1);
		Vitimage_Toolbox.imageChecking(imgOutline);
		imgSlice=connexe(imgSlice,255,255,0,10E10,8,1);
		Vitimage_Toolbox.imageChecking(imgSlice);
		System.out.println(" Ok.");

		
		System.out.println("\nSelection equipartited points for analysis and sort by angle around the center...");
		IJ.run(imgSlice, "8-bit", "");
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.run(imgSlice, "Create Selection", "");
		rm.addRoi(imgSlice.getRoi());
		Roi roi=rm.getRoi(0);
		FloatPolygon fp=roi.getContainedFloatPoints();
		int nAngles=fp.npoints;
		double [][]tabCoord=new double[nAngles][3];
		Double [][]tabSort=new Double[nAngles][3];
		double xCenter = 256;
		double yCenter = 256;
		for (int i=0; i<nAngles; i++) {
			tabSort[i][0]=new Double(fp.xpoints[i]);
			tabSort[i][1]=new Double(fp.ypoints[i]);
			tabSort[i][2]=new Double(calculateAngle(tabSort[i][0]-xCenter,yCenter-tabSort[i][1]));
		}
		imgSlice.changes=false;
		imgSlice.close();
		rm.close();
		//sort by angles
		Arrays.sort(tabSort,new AngleComparator());
		for (int i=0; i<nAngles; i++) {
			tabCoord[i][0]=tabSort[i][0].doubleValue();
			tabCoord[i][1]=tabSort[i][1].doubleValue();
			tabCoord[i][2]=tabSort[i][2].doubleValue();
		}
		System.out.println(" Ok.");
		double [][]meanValues=new double[nAngles][zMax];
		
		System.out.println("\nMeasurements");
		ImagePlus measures=ij.gui.NewImage.createImage("measures",nAngles,zMax*facteurAniso,3,32,ij.gui.NewImage.FILL_BLACK);
		float[]measuresImg0=(float[]) measures.getStack().getProcessor(1).getPixels();
		float[]measuresImg1=(float[]) measures.getStack().getProcessor(2).getPixels();
		float[]measuresImg2=(float[]) measures.getStack().getProcessor(3).getPixels();

		for (int ang=0; ang<nAngles; ang++){
			for (int z=0; z<zMax; z++){			
				meanValues[ang][z]=meanValueofImageAround(img,(int)Math.round(tabCoord[ang][0]),(int)Math.round(tabCoord[ang][1]),z,sigmaXYInPixels);
				for(int i=0;i<facteurAniso;i++)measuresImg0[nAngles*(z*facteurAniso+i)+ang]=(float) meanValues[ang][z];
			}
		}
		
		System.out.println(" Ok.");
		
		
		
		
		System.out.println("\nScore computation");
//
		double[][][]scores=new double[nAngles][zMax][3];
		for (int ang=0; ang<nAngles; ang++){
			for (int z=minPossibleZ; z<=maxPossibleZ; z++){			
				double acc=0;
				for(int i=-sigmaPlotZ;i<=sigmaPlotZ;i++)acc+=meanValues[ang][z+i];
				scores[ang][z][0]=acc/(2*sigmaPlotZ+1);
				scores[ang][z][1]=(scores[ang][z][0]-meanValues[ang][z])/scores[ang][z][0];
				scores[ang][z][2]=(scores[ang][z][0]-meanValues[ang][z]);
				for(int i=0;i<facteurAniso;i++)measuresImg1[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][1];
				for(int i=0;i<facteurAniso;i++)measuresImg2[nAngles*(z*facteurAniso+i)+ang]=(float) scores[ang][z][2];
			}
		}
		
		measures.getProcessor().setMinAndMax(0,3000);
		IJ.run(measures,"Fire","");
		measures.show();
		ImagePlus imgDetect=new Duplicator().run(measures,1,measures.getStackSize());
		IJ.run(imgDetect, "Gaussian Blur...", "sigma="+(1/(2*vX))+" stack");
		imgDetect.show();
		double[][]coordMax=getCoordinatesOf(imgDetect,COORD_OF_MAX_IN_TWO_LAST_SLICES,minPossibleZ*facteurAniso,maxPossibleZ*facteurAniso);
		System.out.println("Maximum relatif obtenu à ("+coordMax[0][0]+" , "+coordMax[0][1]+" ) soit, en coordonnees images : ( "+
							tabCoord[(int)Math.round(coordMax[0][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[0][0])][1]+" , "+
							((coordMax[0][1]-facteurAniso/2.0)/facteurAniso)+" )");
		System.out.println("Maximum absolu obtenu à ("+coordMax[1][0]+" , "+coordMax[1][1]+" ) soit, en coordonnees images : ( "+
							tabCoord[(int)Math.round(coordMax[1][0])][0]+" , "+tabCoord[(int)Math.round(coordMax[1][0])][1]+" , "+
							((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)+" )");

		return new double[]{tabCoord[(int)Math.round(coordMax[1][0])][0]*vX+vX/2.0,tabCoord[(int)Math.round(coordMax[1][0])][1]*vY+vY/2.0,((coordMax[1][1]-facteurAniso/2.0)/facteurAniso)*vZ+vZ/2.0};
	}
		
	public static double calculateAngle(double x, double y)
	{
	    double angle = Math.toDegrees(Math.atan2(y, x));
	    // Keep angle between 0 and 360
	    angle = angle + Math.ceil( -angle / 360 ) * 360;

	    return angle;
	}

	class VolumeComparator implements java.util.Comparator {
		   public int compare(Object o1, Object o2) {
		      return ((Double) ((Object[]) o1)[0]).compareTo((Double)((Object[]) o2)[0]);
		   }
		}
	
	class AngleComparator implements java.util.Comparator {
		   public int compare(Object o1, Object o2) {
		      return ((Double) ((Double[]) o1)[2]).compareTo((Double)((Double[]) o2)[2]);
		   }
		}
		
	public double meanValueofImageAround(ImagePlus img,int x0,int y0,int z0,double ray) {
			int xMax=img.getWidth();
			int xm=(int)Math.round(x0-ray);
			int xM=(int)Math.round(x0+ray);
			int ym=(int)Math.round(y0-ray);
			int yM=(int)Math.round(y0+ray);
			if(z0<0)z0=0;
			if(z0>img.getStackSize()-1)z0=img.getStackSize()-1;

			if(xm<0)xm=0;
			if(ym<0)ym=0;
			if(xm>img.getWidth()-1)xm=img.getWidth()-1;
			if(ym>img.getHeight()-1)ym=img.getHeight()-1;

			if(xM<0)xM=0;
			if(yM<0)yM=0;
			if(xM>img.getWidth()-1)xM=img.getWidth()-1;
			if(yM>img.getHeight()-1)yM=img.getHeight()-1;
			double accumulator=0;
			double nbHits=0;
			if(img.getType() == ImagePlus.GRAY8) {
				byte[] valsImg=(byte [])img.getStack().getProcessor(z0+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
							accumulator+= (int)(  (  (byte)valsImg[xMax*y+x])  & 0xff);
							nbHits++;
						}			
					}
				}			
			}
			else if(img.getType() == ImagePlus.GRAY16) {
				short[] valsImg=(short[])img.getStack().getProcessor(z0+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
							accumulator+= (int)(  (  (short)valsImg[xMax*y+x])  & 0xffff);
							nbHits++;
						}			
					}
				}			
			}
			else if(img.getType() == ImagePlus.GRAY32) {
				float[] valsImg=(float[])img.getStack().getProcessor(z0+1).getPixels();
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						if( ((x-xM)*(x-xM)+(y-yM)*(y-yM)) < (ray*ray) ) {
							accumulator+=(float)valsImg[xMax*y+x];
							nbHits++;
						}			
					}
				}			
			}
			if(nbHits==0)return 0;
			else return (accumulator/nbHits);
	}
	
	public double[][] getCoordinatesOf(ImagePlus img,int computationStyle,int minZ,int maxZ){
		if(computationStyle==COORD_OF_MAX_IN_TWO_LAST_SLICES) {
			double[][]tabRet=new double[2][3];
			tabRet[1][2]=tabRet[0][2]=-10E10;
			if(img.getType() == ImagePlus.GRAY32) {
				float[]valsImg2=(float[])img.getStack().getProcessor(2).getPixels();
				float[]valsImg3=(float[])img.getStack().getProcessor(3).getPixels();
				int xMax=img.getWidth();
				
				for(int x=0;x<xMax;x++) {
					for(int y=minZ;y<=maxZ;y++) {
						if(tabRet[0][2]<(float)valsImg2[xMax*y+x]) {
							tabRet[0][2]=(float)valsImg2[xMax*y+x];
							tabRet[0][0]=x;
							tabRet[0][1]=y;							
						}
						if(tabRet[1][2]<(float)valsImg3[xMax*y+x]) {
							tabRet[1][2]=(float)valsImg3[xMax*y+x];
							tabRet[1][0]=x;
							tabRet[1][1]=y;							
						}
					}
				}
			}
			return tabRet;
		}
		return null;
	}
	
	
	
	public double norm(double[]v){
		return Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
	}

	public double[] normalize(double[]v){
		double[] ret=new double[3];
		double nrm=norm(v);
		ret[0]=v[0]/nrm;
		ret[1]=v[1]/nrm;
		ret[2]=v[2]/nrm;
		return ret;
	}

	public double scalarProduct(double[]v1,double []v2){
		return(v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2]);
	}

	public double [] multiplyVector(double[]v,double factor){
		double[] ret=new double[3];
		ret[0]=v[0]*factor;
		ret[1]=v[1]*factor;
		ret[2]=v[2]*factor;
		return ret;
	}

	public double[] vectorialProduct(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[1]*v2[2]-v1[2]*v2[1];		
		ret[1]=v1[2]*v2[0]-v1[0]*v2[2];		
		ret[2]=v1[0]*v2[1]-v1[1]*v2[0];		
		return ret;
	}

	public double[] vectorialSubstraction(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[0]-v2[0];		
		ret[1]=v1[1]-v2[1];		
		ret[2]=v1[2]-v2[2];		
		return ret;
	}
	
	public double[] proj_u_of_v(double[]u,double[]v){
		double scal1=scalarProduct(u,v);
		double scal2=scalarProduct(u,u);
		return multiplyVector(u,scal1/scal2);
	}

	void printVector(double []vect,String vectNom){
		System.out.println(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	public String stringVector(double []vect,String vectNom){
		return(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	
	
}
