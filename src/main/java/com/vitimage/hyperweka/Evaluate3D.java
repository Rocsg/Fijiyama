package com.vitimage.hyperweka;

import java.io.File;
import java.util.ArrayList;

import com.vitimage.common.VitimageUtils;

import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImagePlus;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.FeatureStackArray;
import weka.classifiers.AbstractClassifier;
import weka.core.Attribute;
import weka.core.Instances;

public class Evaluate3D {
//	private Weka_Save wekasave;
	public static final int N_SPECIMENS=12;
	public static final int N_MODS=4;
	static final int []slicesThick=new int[] {25,30,35,40,45,50,60,70,80,90,100,110};//From TestRomain.java{5,10,15,20,25,30,35,40,45,50,60,70,80,90,100,110};
	private int[][]slicesThin;
	private ArrayList <int[]>dataCoordsTmp=new ArrayList<int[]>();//x,y,z,specimen, oldz,classe expected
	private int [][][]dataCoords;//x,y,z,zold,classe expected
	private int[][]coordsGlob;//x,y,z,specimen,classe expected
	private Instances []inst;//features
	private Instances instGlob;
	public int nFeatures=0;
	private String repSourceCentral="/home/fernandr/Bureau/Traitements/Cep5D/";
	private String repStockCentral="/home/fernandr/Bureau/Traitements/Cep5D/Processing3D/";
	private String title="Test3D_1";
	static final String[]stringSpecimens=new String[] {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
	static final String[]stringModalities=new String[] {"Mod_1_THIN_low_rx.tif","Mod_2_THIN_low_mri_t1.tif","Mod_3_THIN_low_mri_t2.tif","Mod_4_THIN_low_mri_m0.tif"};
	

	/**
	 * 
	 *  
	 *  Step 1 usage : create the object during reading example List from Weka_Save, and add progressively examples*/
	public Evaluate3D() {
		slicesThin=new int[N_SPECIMENS][slicesThick.length];
		for(int s=0;s<N_SPECIMENS;s++) {
			slicesThin[s]=VitimageUtils.readIntArray1DFromFile("/home/fernandr/Bureau/Traitements/Cep5D/CEP011_AS1/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/depthSlicesTab.txt");
		}
		
		dataCoords=new int[N_SPECIMENS][][];
		System.out.println("Recherche du fichier "+repStockCentral+title+"/"+title+"_COORDS_spec_0.txt");
		if(new File(repStockCentral+title+"/Coords/"+title+"__COORDS_spec_0.txt").exists()) {
			for(int spec=0;spec<N_SPECIMENS;spec++)this.dataCoords[spec]=VitimageUtils.readIntArrayFromFile(repStockCentral+title+"/Coords/"+title+"__COORDS_spec_"+spec+".txt",5);
		}
		this.inst=new Instances[N_SPECIMENS];
		for(int spec=0;spec<N_SPECIMENS;spec++) {
			if(new File(repStockCentral+title+"/Instances/"+title+"__INSTANCES__spec_"+spec+".arff").exists()) {
				this.inst[spec]=WekaSegmentation_Save.readDataFromARFF(repStockCentral+title+"/Instances/"+title+"__INSTANCES__spec_"+spec+".arff");
			}
		}
		
		if(new File(repStockCentral+title+"/Coords/"+title+"__COORDS_GLOB.txt").exists()) {
			this.instGlob=WekaSegmentation_Save.readDataFromARFF(repStockCentral+title+"/Instances/"+title+"__INSTANCES__GLOB.arff");
			this.coordsGlob=VitimageUtils.readIntArrayFromFile(repStockCentral+title+"/Coords/"+title+"__COORDS_GLOB.txt",5);
		}
	}
	
	//Receive : x,y,z,class expected
	public void addExample(int[]data) {
		int[]da=computeCoordinatesCorrespondances(data[2]);//it is : z, specimen
		dataCoordsTmp.add(new int[] {data[0],data[1],da[0],da[1],data[2],data[3]});// it is x, y , z, specimen, zold, class
	}

	//Out : Z,specimen, (ITK norm, begins at 0)
	public int[] computeCoordinatesCorrespondances(int sliceInHybridVert) {
		int spec=sliceInHybridVert%12;
		return new int[] {slicesThin[spec][slicesThick[sliceInHybridVert/12]-1],spec};
	}

	
	public void finalizeExamplesList() {
		int[]tabNb=new int[N_SPECIMENS];
		for(int i=0;i<dataCoordsTmp.size();i++)tabNb[dataCoordsTmp.get(i)[3]]++;
		for(int spec=0;spec<N_SPECIMENS;spec++) {dataCoords[spec]=new int[tabNb[spec]][];tabNb[spec]=0;}
		for(int i=0;i<dataCoordsTmp.size();i++) {
			int numSpec=dataCoordsTmp.get(i)[3];
			int numIncr=tabNb[numSpec];
			dataCoords[numSpec][numIncr]=new int[] {dataCoordsTmp.get(i)[0],dataCoordsTmp.get(i)[1],dataCoordsTmp.get(i)[2],dataCoordsTmp.get(i)[4],dataCoordsTmp.get(i)[5]}; //it is : x,y,z,zold,class
			tabNb[numSpec]++;
		}
	}
		
	public void saveEvaluate3D() {
		for(int spec=0;spec<N_SPECIMENS;spec++)VitimageUtils.writeIntArrayInFile(dataCoords[spec],repStockCentral+title+"/Coords/"+title+"__COORDS_spec_"+spec+".txt");
	}
	
	public void prepareImages() {
		System.out.println("Preparation des images pour eval3D...");
		for(int spec=0;spec<N_SPECIMENS;spec++) {
			System.out.println("Copie spec "+spec);
			for(int m=0;m<stringModalities.length;m++) {
				System.out.println("     Copie mod "+m);
				if(!new File(repSourceCentral+stringSpecimens[spec]+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/"+stringModalities[m]).exists()) {
					ImagePlus tmp=IJ.openImage(repSourceCentral+stringSpecimens[spec]+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/"+stringModalities[m]);
					IJ.saveAsTiff(tmp, repStockCentral+"/Images/Img_spec_"+spec+"__"+"mod_"+m+".tif");	
				}
			}	
		} 
	}
	
	
	
	
	
	
	
	
	/**
	 * 
	 *  
	 *  Step 2 usage : load the object and compute the feature values*/
	public void initializeInstances(FeatureStackArray fsa) {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=1; i<=fsa.getNumOfFeatures(); i++)		{
			String attString = fsa.getLabel(i);
			attributes.add(new Attribute(attString));
		}
		ArrayList<String> classes = new ArrayList<String>();
		classes.add("BG");
		classes.add("SAIN");
		classes.add("NECROSE");
		classes.add("AMADOU");
		classes.add("ECORCE");

		attributes.add(new Attribute("class", classes));
		for(int spec=0;spec<N_SPECIMENS;spec++) {
			this.inst[spec] =  new Instances( "segment", attributes, 1 );
		}
	}

	
	public void computeInstances(Weka_Save wekasave,WekaSegmentation_Save wekaSegmentation,int sigMin,int sigMax,boolean []features) {
		System.out.println("Debug...");
		System.out.println("Et en effet, datacoords existe "+(dataCoords==null));

		System.out.println("Et en effet, datacoords[0] existe "+(dataCoords[0]==null));
		System.out.println("Et en effet, il a de la longueur "+dataCoords[0].length);
		System.out.println("Et en effet, le premier contenu existe "+dataCoords[0][0]==null);
		System.out.println("Et en effet, il a de la longueur "+dataCoords[0][0].length);
		System.out.println("Et en effet, le premier contenu existe "+dataCoords[0][0][0]);
		//Compute and set the nb of Features
		int[]sigmas=wekasave.getSigmaTab((int)Math.round(sigMin),(int)Math.round(sigMax));
		this.nFeatures=wekasave.computeNbFeat(N_MODS, sigmas.length, features);
		//For each specimen		
		for(int spec=0;spec<N_SPECIMENS;spec++) {
			System.out.println("Construction feature stack pour specimen "+spec);
			FeatureStack[]fs;
			//Load the hyperTab
			System.out.println("Loading images...");
			ImagePlus[]img=new ImagePlus[N_MODS];
			for(int m=0;m<stringModalities.length;m++) {
				img[m]=IJ.openImage(repStockCentral+"/Images/Img_spec_"+spec+"__"+"mod_"+m+".tif");
			}
			
			//Compute featureStackTab
			if(spec==0) {
				FeatureStackArray fsa=Weka_Save.buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(img, features, sigMin, sigMax);			
				initializeInstances(fsa);
				for(int i=0;i<fsa.getSize();i++) {
					ImagePlus temp=new ImagePlus("slice "+i,fsa.get(i).getStack());
					IJ.saveAsTiff(temp, repStockCentral+"FeaturesImages/"+title+"__INSTANCES_slice_"+i+"__spec_"+spec+".tif");
				}
				System.exit(0);
				fs=Weka_Save.FeatureStackArrayToFeatureStackTab((Weka_Save.buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(img, features, sigMin, sigMax)) );
			}
			else fs=Weka_Save.FeatureStackArrayToFeatureStackTab((Weka_Save.buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(img, features, sigMin, sigMax)) );
			
			
			//For each example, store the values as an Instance object	
			System.out.println("Compute instances");
			for(int ex=0;ex<dataCoords[spec].length;ex++) {
				System.out.println("Traitement exemple "+ex);
				this.inst[spec].add( fs[dataCoords[spec][ex][2] ].createInstance( dataCoords[spec][ex][0], dataCoords[spec][ex][1],dataCoords[spec][ex][4]) );
			}
			System.out.println("Write instances");
			WekaSegmentation_Save.writeDataToARFF(this.inst[spec], repStockCentral+title+"/Instances/"+title+"__INSTANCES__spec_"+spec+".arff");
		}
	}
	
	
	public void fuseInstancesAndCoords() {
		System.out.println("Start fuse");
		instGlob=new Instances(inst[0],0);
		
		for(int spec=0;spec<N_SPECIMENS;spec++) {
			int n=inst[spec].numInstances();
			for(int i=0;i<n;i++)instGlob.add(inst[spec].get(i));
		}
		WekaSegmentation_Save.writeDataToARFF(this.instGlob, repStockCentral+title+"/Instances/"+title+"__INSTANCES__GLOB.arff");
		
		System.out.println("Middle fuse");
		coordsGlob=new int[instGlob.numInstances()][5];
		int incr=0;
		for(int spec=0;spec<N_SPECIMENS;spec++) {
			for(int i=0;i<dataCoords[spec].length;i++) {
				coordsGlob[incr++]=dataCoords[spec][i];
				coordsGlob[incr-1][3]=spec;
			}		
		}
		VitimageUtils.writeIntArrayInFile(coordsGlob,repStockCentral+title+"/Coords/"+title+"__COORDS_GLOB.txt");
		System.out.println("End fuse");
	}

	
	public String stringSpecimensInUse(boolean[]specTrained,boolean[]specTested,int numExp,int nbExp) {
		String s="Experience "+(nbExp>0 ? (" "+(numExp+1)+" / "+nbExp) : "" )+"\n   Training specimens = ";
		for(int i=0;i<N_SPECIMENS;i++)if(specTrained[i])s+=stringSpecimens[i]+" ,";
		s+="\n   Testing specimens = ";
		for(int i=0;i<N_SPECIMENS;i++)if(specTested[i])s+=stringSpecimens[i]+" ,";
		s+="\n";
		return s;
	}
	
	public String stringBoolsInUse(boolean[]specTrained) {
		String s="--";
		for(int i=0;i<N_SPECIMENS;i++)if(specTrained[i])s+=stringSpecimens[i]+" ,";
		s+="\n";
		return s;
	}

	
	
	
	
	
	
	
	/**
	 * 
	 *  
	 *  Step 3 usage : use the features to compute scores*/
	public void trainAndTest() {
		//Instances and datacoords are loaded
		boolean[]specTrained=new boolean[N_SPECIMENS];
		boolean[]specTested=new boolean[N_SPECIMENS];
		
		//For all possible cases
		int nExp=6*11;
		int incr=-1;
		double[][][][]confusions=new double[nExp][2][][];
		int[][][]testRes=new int[nExp][][];

		for(int spec1=0;spec1<N_SPECIMENS;spec1++) {	
			for(int spec2=spec1+1;spec2<N_SPECIMENS;spec2++) {	

				//Select populations
				incr++;
				for(int i=0;i<N_SPECIMENS;i++) {specTrained[i]=true;specTested[i]=false;}
				specTrained[spec1]=specTrained[spec2]=false;
				specTested[spec1]=specTested[spec2]=true;
				System.out.println(stringSpecimensInUse(specTrained,specTested,incr,nExp));
		
				
				//Build Instancetab
				Instances dataTrain=createInstanceTab(specTrained,"Datatrain_incr"+incr);
				Instances dataTest=createInstanceTab(specTested,"Datatest_incr"+incr);
				int[]actualTrain=createClassTab(specTrained,"Classtrain_incr"+incr);
				int[]actualTest=createClassTab(specTested,"Classtest_incr"+incr);

				
				//Build and train classifier
				FastRandomForest rf=new FastRandomForest();
				rf.setNumFeatures(14);
				rf.setNumTrees(300);
				AbstractClassifier classifier=rf;
				System.out.print("Entrainement ...");
				try {classifier.buildClassifier(dataTrain);	} catch (Exception e) {e.printStackTrace();	}	
				System.out.println(" Ok.");
				int[]predictedTrain=getPredictedValues(dataTrain,classifier,"Predictedtrain_incr"+incr);
				int[]predictedTest=getPredictedValues(dataTest,classifier,"Predictedtest_incr"+incr);

				//Calculer les scores et les afficher
				confusions[incr][0]=Weka_Save.computeStatisticsOnPrediction(actualTrain,predictedTrain,5,"Training",repStockCentral+title+"/Confusion/stats_training_"+incr);
				confusions[incr][1]=Weka_Save.computeStatisticsOnPrediction(actualTest,predictedTest,5,"Testing",repStockCentral+title+"/Confusion/stats_testing_"+incr);
			}
		}
		
		//A la fin, englober toutes les experiences, et calculer une accuracy globale
		double[][][]confusionsSpec=Weka_Save.assembleConfusionsTab(confusions);
		double[][]computationsTrain=Weka_Save.statsFromConfusion(confusionsSpec[0]);
		double[][]computationsTest=Weka_Save.statsFromConfusion(confusionsSpec[1]);
		System.out.println("\n\n\n\n\n\n  GLOBAL CROSS-VALIDATION RESULTS\n\n");
		IJ.log("\n\n\n\n\n\n  GLOBAL CROSS-VALIDATION RESULTS\n\n");
		Weka_Save.writeConfusionStats(confusionsSpec[0],computationsTrain," train set over two fold cross ",repStockCentral+title+"/Confusion/stats_training_GLOB_TRAIN");
		Weka_Save.writeConfusionStats(confusionsSpec[1],computationsTest," test set over two fold cross ",repStockCentral+title+"/Confusion/stats_training_GLOB_TEST");
	}
	
	
	
	
	public void exportForNN() {
		System.out.println("Here1");
		double[][]vals=new double[instGlob.size()][];
		System.out.println("Here2");
		for(int i=0;i<vals.length;i++)vals[i]=instGlob.get(i).toDoubleArray();
		System.out.println("Here3");
		VitimageUtils.writeDoubleArrayInFile(vals, repStockCentral+title+".txt");
		System.out.println("Here4");
		System.out.println("Résultat ecrit dans "+repStockCentral+title+".txt");
	}
	
	
	
	
	
	
	public int[] createClassTab(boolean[]specSelected,String title) {
		int tot=0;
		for(int i=0;i<coordsGlob.length;i++)if(specSelected[coordsGlob[i][3]])tot++;
		int [] ret=new int [tot];tot=0;
		for(int i=0;i<coordsGlob.length;i++)if(specSelected[coordsGlob[i][3]])ret[tot++]=coordsGlob[i][4];
		System.out.println("Create "+tot+" classes for class tab "+title);
		return ret;
	}

	
	public Instances createInstanceTab(boolean[]specSelected,String title) {
		int tot=0;
		Instances ret=new Instances(this.instGlob,0);
		for(int i=0;i<coordsGlob.length;i++)if(specSelected[coordsGlob[i][3]]) {tot++;ret.add(this.instGlob.get(i));}
		System.out.println("Create "+tot+" instances for instance tab "+title);
		return ret;
	}
	
	public int[]getPredictedValues(Instances data,AbstractClassifier classifier,String title){
		System.out.println("Computing "+data.size()+" predicted values for "+title);
		int[]ret=new int[data.size()];
		try {
			double[][]tabProb=classifier.distributionsForInstances(data);
			for(int i=0;i<tabProb.length;i++) ret[i]=VitimageUtils.indmax(tabProb[i]);
		} catch (Exception e) {e.printStackTrace();}
		return ret;
	}
		
		
	

	
	

	
	
}
