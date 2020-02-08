package com.vitimage.factory;
import java.awt.Rectangle;

import com.vitimage.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.TextReader;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.StackConverter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;
import trainableSegmentation.utils.Utils;
import weka.core.*;

public class ScriptAutoParamWekaMultiChannelMultiSlices {
	// MON CODE A MOI
	public WekaSegmentation wekaSegmentation;

	public ScriptAutoParamWekaMultiChannelMultiSlices(ImagePlus imgInit) {
		System.out.println("Construction de l'objet courant");
		this.wekaSegmentation=new WekaSegmentation(imgInit);
	}

	
	public static void justDoIt() {
		ImageJ ij=new ImageJ();
		ImagePlus imgTrain=IJ.openImage("/home/fernandr/Bureau/Test/Test_WEKA/imgTrain.tif");
		ImagePlus imgTest=IJ.openImage("/home/fernandr/Bureau/Test/Test_WEKA/imgTest.tif");
		WekaSegmentation ws=new WekaSegmentation(false);
		ws.setTrainingImage(imgTrain);
		String repertoire="";
		repertoire="/home/fernandr/Bureau/Test/Test_WEKA/Roi_for_training_1/";      
		ws.addClass();
		ws.addClass();
		ws.addClass();
		String[]mesClasses=new String[] { "1_Vaisseaux_vides","2_Vaisseaux_pleins","3_Fond","4_Cire","5_Bois" };
		ws.setClassLabels(mesClasses);
	
		int nbExamplesPerClass=1;
		for(int classe=0;classe<5;classe++) {
			for (int i=1;i<=nbExamplesPerClass;i++) {
				Roi roiTmp=getRoiStaticFromNameOfFile(repertoire+"CL"+(classe+1)+"_EX"+i+".txt");
				ws.addExample( classe ,roiTmp,1);
			}
		}
		ws.trainClassifier();
		ImagePlus output = ws.applyClassifier(imgTrain,4,false);
		ImagePlus output2 = ws.applyClassifier(imgTest,4,false);

		output.show();		
		output2.show();		
	}
	
	
	public static void main (String[]args) {
		ImageJ ij=new ImageJ();
		justDoIt();
		VitimageUtils.waitFor(1000000);
		
		ScriptAutoParamWekaMultiChannelMultiSlices scr= null;
		int partie=1;
		int nSlicesPerSample=5;
		boolean testSurOrdiKhalifa=true;
		/* Partie 1 : entrainement du modele*/
		if(partie==1) {
			//Une image hybride a été préparée, à partir de données normalisées, ainsi que les roi qui definissent les zones
			//Charger l image hybride
			System.out.println("Lecture de l'image RX...");
			ImagePlus imgTrain=null;
			if(testSurOrdiKhalifa) {
				imgTrain=IJ.openImage("/home/fernandr/Bureau/Test/Test_WEKA/imgTrain.tif");      
			}
			else {
				imgTrain=IJ.openImage("F:\\test_weka\\image_RX_hybride_pour_entrainement.tif");      
			}
			scr=new ScriptAutoParamWekaMultiChannelMultiSlices(imgTrain);
			scr.wekaSegmentation.addClass();
			scr.wekaSegmentation.addClass();
			scr.wekaSegmentation.addClass();
			String[]mesClasses=new String[] { "1_Vaisseaux_vides","2_Vaisseaux_pleins","3_Fond","4_Cire","5_Bois" };
			scr.wekaSegmentation.setClassLabels(mesClasses);
		//REaliser l entrainement
			System.out.print("Realiser l'entrainement...");
			scr.makeTraining(imgTrain);
			System.out.println("ok");

			//Realiser la segmentation de l image entrainement
			System.out.print("Realiser la segmentation...");
			ImagePlus segmentationOfTrainingImage=scr.applyFilter(imgTrain);  // ou bien segmentationWeka
			System.out.println("ok");
			segmentationOfTrainingImage.show();
		}


		partie=0;
		/* Partie 2 : Application du modele */
		if(partie==2) {
			//Pour toutes les images test
			String repertoire="";
			if(testSurOrdiKhalifa)repertoire="/home/kdiouf/Bureau/Experiences/...a completer/";
			else repertoire="F:\\test_weka\\";
			String [] nomImages=new String[] { "2I6_RX_norm.tif"};// a enrichir une fois que ça marche pour toutes les images
			for(String strImgCourante : nomImages) {
				ImagePlus img=IJ.openImage(strImgCourante);
				String fullStr=repertoire+strImgCourante;
				System.out.println("Traitement de l image : "+fullStr);
				ImagePlus output=scr.applyFilter(img); // ou bien applyfilter ???
				output.setTitle("Segmentation de "+strImgCourante);
				output.show();
			}
		}
	}

	/*
	 * Helper functions
	 */
	public ImagePlus[] divideImageInSubStacks(ImagePlus imgBig,int nbSlicesPerSample){
		int dimZ =imgBig.getStackSize();
		System.out.println("Division de l'image en substacks... taille initiale de l'image : "+dimZ+" slices");
		int nbSamples=0;
		if (dimZ%nbSlicesPerSample==0) {
			nbSamples=dimZ/nbSlicesPerSample;
		}
		else {
			nbSamples=(dimZ/nbSlicesPerSample)+1;
		}

		ImagePlus[]tabSubResults=new ImagePlus[nbSamples];

		//pour chaque morceau
		for (int part=0 ;part<nbSamples; part++){
			int sliceMin=part*nbSlicesPerSample;
			int sliceMax=part*nbSlicesPerSample+(nbSlicesPerSample-1);
			if(sliceMax>=dimZ)sliceMax=dimZ-1;

			System.out.println("  - Part "+part+" selection des slices ["+sliceMin+" - "+sliceMax+"]");
			tabSubResults[part] = new Duplicator().run(imgBig,1+sliceMin,1+sliceMax);
		}
		return tabSubResults;
	}


	public ImagePlus concatImageTabIntoOneImage(ImagePlus[] tabImg) {
		//int n=tabImg.length;
		//int dimZ=0;
		ImagePlus concatImage=Concatenator.run(tabImg);
		System.out.println("L image concatenee resultant");
		return concatImage;
	}


	public FeatureStackArray computeFeatures (ImagePlus img) {
		//Detail des parametres utilses
		int sigmaMin=1;
		int sigmaStep=1;
		int nbSigmaTest=3;
		int nbFeatures=1+nbSigmaTest;
		int iterFeat=0;
		System.out.println("Le calcul va etre effectué, avec sigmaMin="+sigmaMin+" , et sigmaStep="+sigmaStep+" et nb de sigma="+nbSigmaTest );
		System.out.println("--Total #Features = "+nbFeatures);

		ImagePlus[] images=new ImagePlus[nbFeatures];
		ImageStack[]stacks=new ImageStack[nbFeatures];
		Duplicator duplicator = new Duplicator();
		if(img.getType() != 32)new StackConverter(img).convertToGray32();  


		images[iterFeat] = duplicator.run(img);
		iterFeat++;



		//calculer les features
		for(int sig=sigmaMin;sig<=sigmaMin+sigmaStep*(nbSigmaTest-1) ; sig+=sigmaStep){
			images[iterFeat] = duplicator.run(img);
			IJ.run(images[iterFeat], "Gaussian Blur 3D...", "x="+sig+" y="+sig+" z="+sig);
			iterFeat++;
		}

		//Convertir en stacks dans le tableau de stacks
		System.out.println("--Empilement des stacks correspondant aux images calculées");
		for(int it=0;it<iterFeat ;it++){
			stacks[it]=images[it].getStack();
			images[it].close();
			images[it]=null;
		}
		images=null;


		//en faire un FeatureStackArray et le renvoyer
		// ASSEMBLAGE DU TABLEAU COMPLET (FeatureStackArray, avec une stack par slice de l'image initiale,
		//chaque stack representant la slice vue sous toutes les features
		// Calcul des FeatureStacks pour mettre dans le FeaturesArray, une stack par slice de l'image originale
		System.out.println("STEP 3 : Reunion des features par slice");
		// the FeatureStackArray contains a FeatureStack for every slice in our original image
		double sigMin=sigmaMin;
		double sigMax=sigmaMin+sigmaStep*(nbSigmaTest-1);
		boolean useNeighbors=false;
		int membraneSize=0;
		int membranePatchSize=0;
		boolean[] enabledFeatures=new boolean[] {true,false,false,false,false,false,false,false,false,   false,false,false,false,false,false,false,false,false};
		FeatureStackArray featuresArray = new FeatureStackArray(nbFeatures,(float)sigMin,(float)sigMax, false,membraneSize,membranePatchSize,enabledFeatures);
//		for (int  slice = 1; slice <= img.getStackSize(); slice++) {
		for (int  ft = 1; ft <= nbFeatures; ft++) {
			// create empty feature stack
			FeatureStack featuresOfCurrentSlice = new FeatureStack( stacks[0].getWidth(), stacks[0].getHeight(), false );

			// set my features to the feature stack
			featuresOfCurrentSlice.setStack( stacks[ft-1]);

			// put my feature stack into the array
			featuresArray.set(featuresOfCurrentSlice, ft -1);
			// ???????
			 featuresArray.setEnabledFeatures(featuresOfCurrentSlice.getEnabledFeatures());
			// ???????
		}
		System.out.println("Et pourtant, est null= ?"+(featuresArray==null));
		System.out.println("Et quelle est sa taille ? "+featuresArray.getSize());
		return featuresArray;
	}




	//Calculer les features, charger les examples, generer le modele
	public void makeTraining (ImagePlus imgTrain) {
		System.out.println("ENTRAINEMENT ");
		System.out.println(" 1) Calcul des features");
		FeatureStackArray fsa=this.computeFeatures(imgTrain);
		System.out.println("Et pourtant weka Segmentation est null ="+(this.wekaSegmentation==null));
		this.wekaSegmentation.setFeatureStackArray(fsa);
		this.wekaSegmentation.setTrainingImage(imgTrain);
		System.out.println(" 2) Lecture des exemples");
		this.readTrainingExamplesProvided();
		System.out.println(" 3) Entrainement du modele");
		if ( ! this.wekaSegmentation.trainClassifier())
			throw new RuntimeException("Uh oh! No training today.");
		System.out.println();
		System.out.println("Test de la segmentation sur l image training");
		ImagePlus outputTest=this.applyFilterSubImage(imgTrain);
		System.out.println();
		System.out.println("Test effectue");
		outputTest.show();
		VitimageUtils.waitFor(10000);
		outputTest.close();
		return;
	}


	public ImagePlus applyFilter(ImagePlus img) {
		System.out.println("Calcul de la segmentation d'une image a partir du filtre entraine");
		boolean doProba=false;

		//Calculer le tableau de substacks
		System.out.println(" 1) calcul des sous images");
		ImagePlus []tabImg=this.divideImageInSubStacks(img, 5);
		System.out.println("Nombre de substacks="+tabImg.length);

		//Calculer la segmentation de chaque element du tableau
		for(int part=0; part<tabImg.length; part++ ) {
			System.out.println(" image "+part+"  : calcul segmentation ");
			applyFilterSubImage(tabImg[part]);
		}
		ImagePlus imageFiltered= concatImageTabIntoOneImage(tabImg);
		imageFiltered.setTitle("Classified");
		//VISUALISATION DU RESULTAT
		return imageFiltered;


	}


	public ImagePlus applyFilterSubImage(ImagePlus img) {
		boolean doProba=false;
		//Calculer les features
		FeatureStackArray fsa=computeFeatures(img);

		//L appliquer
		this.wekaSegmentation.setFeatureStackArray(fsa);
		System.out.println("STEP 7 : Classification de l'image partielle");
		System.out.println("Et pourtant fsa sizz="+fsa.getSize());
		System.out.println("Et pourtant, nb features="+fsa.getNumOfFeatures());
		System.out.println("Et pourtant, weka.numOfClasses "+this.wekaSegmentation.getNumOfClasses());
		System.out.println("Et pourtant, weka.numRandomFeatures "+this.wekaSegmentation.getNumRandomFeatures());
		ImagePlus output = this.wekaSegmentation.applyClassifier(fsa,2,doProba);
		return output;
	}



	//Cette methode ajoute à la segmentation courante des exemples
	public void readTrainingExamplesProvided() {
		boolean testSurOrdiKhalifa=true;
		String repertoire="";
		if(testSurOrdiKhalifa) {
			repertoire="/home/fernandr/Bureau/Test/Test_WEKA/Roi_for_training_1/";      
		}
		else {
			repertoire="F:\\test_weka\\";
		}
		System.out.println("Ajout des exemples depuis le dossier : "+repertoire);

		int numSlice=1;
		int nbExamplesPerClass=1;
		int numClasseCourante=0;
		RoiManager rm=RoiManager.getRoiManager();

		//Pour toutes les classes, faire
		for(int classe=0;classe<5;classe++) {
			//Pour toutes les Roi de la classe, faire
			for (int i=1;i<=nbExamplesPerClass;i++) {
				Roi roiTmp=getRoiFromNameOfFile(repertoire+"CL"+(classe+1)+"_EX"+i+".txt");
				this.wekaSegmentation.addExample( classe ,roiTmp,numSlice);
			}
		}
	}



	public Roi getRoiFromNameOfFile(String arg) {
		if (IJ.versionLessThan("1.26f"))
			return null;
		TextReader tr = new TextReader();
		ImageProcessor ip = tr.open(arg);
		if (ip==null)
			return null;
		int width = ip.getWidth();
		int height = ip.getHeight();
		if (width!=2 || height<3) {
			IJ.showMessage("ROI Importer", "Two column text file required");
			return null;
		}
		double d = ip.getPixelValue(0, 0);
		if (d!=(int)d) {
			IJ.showMessage("ROI Importer", "Integer coordinates required");
			return null;
		}
		int[] x = new int[height];
		int[] y = new int[height];
		for (int i=0; i<height; i++) {
			x[i] = (int)Math.round(ip.getPixelValue(0, i));
			y[i] = (int)Math.round(ip.getPixelValue(1, i));
		}

		Roi roi = new PolygonRoi(x, y, height, null, Roi.FREEROI);
		if (roi.getLength()/x.length>10)
			roi = new PolygonRoi(x, y, height, null, Roi.POLYGON); // use "handles"
		Rectangle r = roi.getBoundingRect();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null || imp.getWidth()<r.x+r.width || imp.getHeight()<r.y+r.height) {
			new ImagePlus(tr.getName(), new ByteProcessor(Math.abs(r.x)+r.width+10, Math.abs(r.y)+r.height+10)).show();
			imp = WindowManager.getCurrentImage();
		}
		if (imp!=null)
			return roi;
		return null;
	}



	public static Roi getRoiStaticFromNameOfFile(String arg) {
		if (IJ.versionLessThan("1.26f"))
			return null;
		TextReader tr = new TextReader();
		ImageProcessor ip = tr.open(arg);
		if (ip==null)
			return null;
		int width = ip.getWidth();
		int height = ip.getHeight();
		if (width!=2 || height<3) {
			IJ.showMessage("ROI Importer", "Two column text file required");
			return null;
		}
		double d = ip.getPixelValue(0, 0);
		if (d!=(int)d) {
			IJ.showMessage("ROI Importer", "Integer coordinates required");
			return null;
		}
		int[] x = new int[height];
		int[] y = new int[height];
		for (int i=0; i<height; i++) {
			x[i] = (int)Math.round(ip.getPixelValue(0, i));
			y[i] = (int)Math.round(ip.getPixelValue(1, i));
		}

		Roi roi = new PolygonRoi(x, y, height, null, Roi.FREEROI);
		if (roi.getLength()/x.length>10)
			roi = new PolygonRoi(x, y, height, null, Roi.POLYGON); // use "handles"
		Rectangle r = roi.getBoundingRect();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null || imp.getWidth()<r.x+r.width || imp.getHeight()<r.y+r.height) {
			new ImagePlus(tr.getName(), new ByteProcessor(Math.abs(r.x)+r.width+10, Math.abs(r.y)+r.height+10)).show();
			imp = WindowManager.getCurrentImage();
		}
		if (imp!=null)
			return roi;
		return null;
	}


}





/*
package com.vitimage;
import java.awt.Rectangle;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import ij.plugin.Duplicator;
import ij.plugin.TextReader;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.StackConverter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.WekaSegmentation;
import trainableSegmentation.utils.Utils;
import weka.core.*;

public class ScriptAutoParamWekaMultiChannelMultiSlices {
// MON CODE A MOI

public WekaSegmentation wekaSegmentation;

public ScriptAutoParamWekaMultiChannelMultiSlices() {
}

public static void main(String[]args) {
ScriptAutoParamWekaMultiChannelMultiSlices scr= new ScriptAutoParamWekaMultiChannelMultiSlices();
scr.calculerSegmentationWeka();

}




public void calculerSegmentationWeka() {
//boolean testing= true;
// PREMIERE ETAPE OUVERTURE DES DONNEES SOURCES
//if (testing==true) {
//ImagePlus imgRX=IJ.openImage("/home/kdiouf/Bureau/Experiences/Test_WEKA/RX_crop_crop_crop.tif");
//}
//OUVRIR L IMAGE TOTALE
//else {
ImagePlus imgRX=IJ.openImage("/media/kdiouf/Seagate Expansion Drive/Origine/Stade S1 - 2-I-6/2-I-6-240418 RX");
//}


// DEFINITION DES FEATURES
//Detail des parametres utilses
int sigmaMin=1;
int sigmaStep=1;
int nbSigmaTest=3;
int nbFeatures=1+nbSigmaTest;
int iterFeat=0;
boolean doProba=false;
System.out.println("Le calcul va etre effectué, avec sigmaMin="+sigmaMin+" , et sigmaStep="+sigmaStep+" et nb de sigma="+nbSigmaTest );
System.out.println("--Total #Features = "+nbFeatures);



// quelles features je veux ? On va commencer cool avec quelques lissages gaussiens, de 1 à 4
//La logique est d'abord d'appliquer le filtre de chaque feature a l'image complete. Dans les etapes suivantes
//On "depilera" chacune de ces stack de feature, pour reconstruire les données autrement


// CONSTRUCTION DU PREMIER TABLEAU DE FEATURES, INCLUANT LES IMAGES "FEATUREES"
//Attention, il faut savoir a l'avance combien de features on a, et les ranger au fur et a mesure dans un tableau qu'on aura construit
System.out.println("STEP 1 : Features");

ImagePlus[] images=new ImagePlus[nbFeatures];
ImageStack[]stacks=new ImageStack[nbFeatures];
Duplicator duplicator = new Duplicator();
new StackConverter(imgRX).convertToGray32();  



System.out.println("Bilan des futures occupations memoire");
double val=((double)(nbFeatures*imgRX.getStackSize()*imgRX.getHeight()*imgRX.getWidth()))*0.000001;
System.out.println("Attention : nombre de megavoxels a stocker  : "+val);


//
//Construction des images contenant les features, rangés suivant l'axe Z,et convertis au format float-32
//


//Ajout des images initiales
System.out.println("--Insertion des images initiales");
images[iterFeat] = duplicator.run(imgRX); //Dupliquer ou non
//images[iterFeat] = duplicator.run(imgRX);
iterFeat++;



//Ajout des images lissées de sigmaMin à sigmaMax, par creneau de 1
System.out.println("--Calcul des lissages");
for(int sig=sigmaMin;sig<sigmaMin+sigmaStep*(nbSigmaTest-1) ; sig+=sigmaStep){
System.out.println("Calcul du lissage avec sigma="+sig);
images[iterFeat] = duplicator.run(imgRX);
//images[iterFeat] = duplicator.run(imgRX);
IJ.run(images[iterFeat], "Gaussian Blur 3D...", "x="+sig+" y="+sig+" z="+sig);
iterFeat++;
}




//Ajout des stacks corresponand à tous les features dans le tableau stacks
System.out.println("--Empilement des stacks correspondant aux images calculées");
for(int it=0;it<iterFeat ;it++){
stacks[it]=images[it].getStack();
images[it].close();
images[it]=null;
}
images=null;


// ASSEMBLAGE DU TABLEAU COMPLET (FeatureStackArray, avec une stack par slice de l'image initiale,
//chaque stack representant la slice vue sous toutes les features
//
// Calcul des FeatureStacks pour mettre dans le FeaturesArray, une stack par slice de l'image originale
//
System.out.println("STEP 3 : Reunion des features par slice");

// the FeatureStackArray contains a FeatureStack for every slice in our original image
FeatureStackArray featuresArray = new FeatureStackArray(imgRX.getStackSize());
for (int  slice = 1; slice <= imgRX.getStackSize(); slice++) {
if(slice%20==0)System.out.println("--Traitement slice "+slice+" / "+imgRX.getStackSize());
ImageStack stack = new ImageStack(imgRX.getWidth(), imgRX.getHeight());
for (  int feat=0 ; feat < iterFeat ; feat++){
stack.addSlice("feat"+feat, stacks[feat].getProcessor(slice));
}
// create empty feature stack
FeatureStack features = new FeatureStack( stack.getWidth(), stack.getHeight(), false );

// set my features to the feature stack
features.setStack( stack);

// put my feature stack into the array
featuresArray.set(features, slice -1);

// featuresArray.set(new FeatureStack(stack.getWidth(), stack.getHeight(), false ).setStack(stack),slice-1);

// ???????
featuresArray.setEnabledFeatures(features.getEnabledFeatures());
// ???????
}

// AJOUT DES TRAINING EXAMPLES (les points, les makeOval)
System.out.println("STEP 4 : Construction du WEKA Segmentation");
this.wekaSegmentation = new WekaSegmentation(imgRX);
this.wekaSegmentation.setFeatureStackArray(featuresArray);

// CONSTRUCTION DE L 'OBJET WEKASEGMENTATION
//Au debut, weka segmentation est initialisé avec deux classes
//Ici on en ajoute 3 pour travailler avec 5 classes : Vaisseaux vides , vaisseaux pleins, fond, cire, bois (contenant les parties blanches)
this.wekaSegmentation.addClass();
this.wekaSegmentation.addClass();
this.wekaSegmentation.addClass();


System.out.println("STEP 5 : Entrainement du classifieur");

//or provide initializer
String[]mesClasses=new String[] { "0_Vaisseaux_vides","1_Vaisseaux_pleins","2_Fond","3_Cire","4_Bois" };
this.wekaSegmentation.setClassLabels(mesClasses);

// AJOUT DES TRAINING EXAMPLES (les points, les makeOval)
this.readTrainingExamplesProvided();



// ENTRAINTEMENT DE L'OBJET WEKA SEGMENTATION
System.out.println("STEP 6 : Entrainement du classifieur");
if ( ! wekaSegmentation.trainClassifier())
   throw new RuntimeException("Uh oh! No training today.");




//APPLICATION DE l'OBJET SUR LA PARTIE ETUDIEE
System.out.println("STEP 7 : Classification de l'image partielle");
ImagePlus output = wekaSegmentation.applyClassifier(featuresArray,2,doProba);
output.setLut( Utils.getGoldenAngleLUT() );
output.setTitle("Classified");
//VISUALISATION DU RESULTAT
output.show();
// output.setSlice(25);



//APPLICATION DE l'OBJET SUR TOUTE L IMAGE
System.out.println("STEP 7 : Classification de l'image partielle");


//CONSTRUIRE LE FEATURE ARRAY

//APPLIQUER SUR L IMAGE TOTALE

//ImagePlus output = wekaSegmentation.applyClassifier(featuresArray,2,doProba);
//output.setLut( Utils.getGoldenAngleLUT() );
//output.setTitle("Classified");
//VISUALISATION DU RESULTAT
//output.show();






}





//Cette methode ajoute à la segmentation courante des exemples
public void readTrainingExamplesProvided() {
int numSlice=1;
int nbExamplesPerClass=1;
int numClasseCourante=0;
RoiManager rm=RoiManager.getRoiManager();


int nbTotalDeRoi=rm.getCount();
System.out.println("Le nombre de Roi qui ont été detectees est :"+nbTotalDeRoi);
System.out.println("Et en effet, on avait 5 classes, et on voulait utiliser 5 exemples par classe, donc on attendant 25");
//Pour toutes les classes, faire
for(int classe=0;classe<5;classe++) {
//Pour toutes les Roi de la classe, faire
for (int i=1;i<=nbExamplesPerClass;i++) {
System.out.println("Pour la classe "+(classe+1)+" lecture de l exemple numero "+i);
Roi roiTmp=getRoiFromNameOfFile("/home/kdiouf/Bureau/CL"+(classe+1)+"_EX"+i+".txt");
this.wekaSegmentation.addExample( classe ,roiTmp,numSlice);
// rm.addRoi(tmp.getRoi());


// Roi r=rm.getRoi(i);
}
}
}



public Roi getRoiFromNameOfFile(String arg) {
if (IJ.versionLessThan("1.26f"))
return null;
TextReader tr = new TextReader();
ImageProcessor ip = tr.open(arg);
if (ip==null)
return null;
int width = ip.getWidth();
int height = ip.getHeight();
if (width!=2 || height<3) {
IJ.showMessage("ROI Importer", "Two column text file required");
return null;
}
double d = ip.getPixelValue(0, 0);
if (d!=(int)d) {
IJ.showMessage("ROI Importer", "Integer coordinates required");
return null;
}
int[] x = new int[height];
int[] y = new int[height];
for (int i=0; i<height; i++) {
x[i] = (int)Math.round(ip.getPixelValue(0, i));
y[i] = (int)Math.round(ip.getPixelValue(1, i));
}

Roi roi = new PolygonRoi(x, y, height, null, Roi.FREEROI);
if (roi.getLength()/x.length>10)
roi = new PolygonRoi(x, y, height, null, Roi.POLYGON); // use "handles"
Rectangle r = roi.getBoundingRect();
ImagePlus imp = WindowManager.getCurrentImage();
if (imp==null || imp.getWidth()<r.x+r.width || imp.getHeight()<r.y+r.height) {
new ImagePlus(tr.getName(), new ByteProcessor(Math.abs(r.x)+r.width+10, Math.abs(r.y)+r.height+10)).show();
imp = WindowManager.getCurrentImage();
}
if (imp!=null)
return roi;
return null;
}


}
 */