package com.vitimage;

import org.itk.simple.Transform;
import org.itk.simple.VectorDouble;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;

public class ItkTransform extends Transform implements ItkImagePlusInterface{
	public static int runTestSequence() {
		int nbFailed=0;
		
		return nbFailed;
	}
	
	
	public ItkTransform(){
		super();
	}
	
	public ItkTransform(ItkTransform model) {		
		super(model);//Aie ?
		return;
	}

	public ItkTransform(imagescience.transform.Transform tr) {
		//Constructeur a ecrire
		//Ce sera la deuxieme passe
		return;
	}

	public ItkTransform(org.itk.simple.Transform model) {
		super(model);
		return;
	}

	public static ItkTransform estimateBestRigid3D(ImagePlus imgRef,ImagePlus imgMov,double[][]pointsRef,double[][]pointsMov) {
		//A copier
		//Ce sera la deuxieme passe
		return null;
	}

	public static ItkTransform readTransformFromFile(String path) {
		//Constructeur statique à ecrire, qui s'appuie sur les fonctions de lecture et ecriture
		//Ce sera la deuxieme passe
		return null;
	}

	public static ItkTransform computeTransformationForBoutureAlignment(ImagePlus img,boolean automaticDetection,double[][]coordinates,boolean b2) {
		//detectInoculationPoint(imgTempZ[0]
		//Ce sera la deuxieme passe
		return null;
	}


	public ItkTransform simplify() {
		//Methode à écrire, qui retire les identités.
		//Ce sera la deuxieme passe
		return this;
	}

	public int nbTransformComposed() {
		//A reecrire au vu des decouvertes effectuees sur les chevrons
		//Ce sera la deuxieme passe
		int nb=(this.toString().split("<<<<<<<<<<")[0].split(">>>>>>>>>").length-1);
		if(nb==0)nb=1;
		return nb;
	}



	public ImagePlus transformImage(ImagePlus imgMov, ImagePlus imgRef) {
		// Il s'agit de la méthode permettant de faire des transformations effectives des images, d'un espace mov vers un espace ref, en passant par une transformationa
		//Ce sera la deuxieme passe
		return null;
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
