package com.vitimage.hyperweka;

import fiji.util.gui.GenericDialogPlus;
import fiji.util.gui.OverlayedImageCanvas;
import hr.irb.fastRandomForest.FastRandomForest;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;

import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.StackConverter;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.FeatureStack3D;
import trainableSegmentation.FeatureStackArray;
import trainableSegmentation.ImageOverlay;
import trainableSegmentation.ImageScience;
import trainableSegmentation.RoiListOverlay;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.vitimage.common.TransformUtils;
import com.vitimage.common.VitiDialogs;
import com.vitimage.common.VitimageUtils;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.Prediction;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.core.WekaPackageManager;
import weka.gui.GUIChooserApp;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;
import weka.core.PluginManager;
import weka.gui.visualize.PlotData2D;
import weka.gui.visualize.ThresholdVisualizePanel;

/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iargandacarreras@gmail.com), Verena Kaynig,
 *          Albert Cardona
 */



/**
 * Segmentation plugin based on the machine learning library Weka
 */
public class Weka_Save implements PlugIn
{
	public int nbTrees=200;
	public int nbFeatures=14;
	public static String versionName="V3_Grand_Horloger";
	public boolean classBalance=true;
	public int fixedMinSigma=1;
	public int fixedMaxSigma=4;
	Evaluate3D evaluate3d;
	public boolean isProcessing3D = false;
	public int[]zStop;
	public int[]zStart;
	int N1=0;
	int N2=0;
	public boolean signal=false;
	public static final int N_SPECIMENS=12;
	public boolean debug=false;
	public boolean computingImportance=true;
	final static boolean testing=true;
	//final static String versionTesting="/home/fernandr/Bureau/EX_CEDRIC/V3/";
	final static String versionTesting="/home/fernandr/Bureau/EX_CEDRIC/SUBS/SUB_16/";
	final Weka_Save wekasave;
	static final String[]stringSelections=new String[] {"WEKA_ALL","WEKA_RANDOM","WEKA_EXCLUDE_SPECIMEN","WEKA_EXCLUDE_SYMPTOM"};
	static final String[]stringSpecimens=new String[] {"CEP011_AS1","CEP012_AS2","CEP013_AS3","CEP014_RES1","CEP015_RES2","CEP016_RES3","CEP017_S1","CEP018_S2","CEP019_S3","CEP020_APO1","CEP021_APO2","CEP022_APO3"};
	static final String[]stringSymptoms=new String[] {"AS","RES","S","APO"};
	static final String[]stringSymptomsNumeric=new String[] {"0-0","1-1","2-2","3-3"};
	final boolean isHorizontalStuffForTesting=false;
	final static int WEKA_ALL=0;
	final static int WEKA_RANDOM=1;
	final static int WEKA_EXCLUDE_SPECIMEN=2;
	final static int WEKA_EXCLUDE_SYMPTOM=3;
	final static int WEKA_SPECIMEN_CROSS_VALIDATION=4;
	final static int WEKA_SYMPTOM_CROSS_VALIDATION=5;
	final static int WEKA_SPECIMEN_TWO_FOLD_CROSS_VALIDATION=6;
	final static int WEKA_SPECIMEN_THREE_FOLD_CROSS_VALIDATION=7;
	final static int WEKA_SPECIMEN_TWO_FOLD_EXCLUDE_RES=8;
	final static int WEKA_IS_TRAINING_POINT=0;
	final static int WEKA_IS_TEST_POINT=1;
	boolean[]tabHyperModalities= {true,true,true,true};
	final String[]stringHyperModalities= {"Use RX","Use T1","Use T2","Use M0"};
	public final String []stringHyperFeatures= {"Division between channels","Gaussian smoothing","Edge enhancement","Gaussians difference","Min value","Max value","Median value","Variance value","Deriche edges","Func1","Func2","Func3"};
	boolean []tabHyperFeatures=new boolean[] { true /*the division*/,true/*gauss*/,true/*edges*/,true/*difference*/,true/*min*/,true/*max*/,true/*mediane*/,true/*variance*/,true/*Deriche*/,false/*func1*/,false/*func2*/,false/*func3*/};
//	boolean []tabHyperFeatures=new boolean[] { true /*the division*/,true/*gauss*/,false         ,false            , false     , false     , false         , false          , false        ,false/*func1*/,false/*func2*/,false/*func3*/};
	boolean goodTrainerWasLastUsedAndParamsHadNotMove=false;

	boolean badTrainerWasLastUsed=false;
	public double magnif=0;
	public boolean isHyper=false;
	public ImagePlus hyperImage=null;
	ImagePlus[]hyperTab;
	ImagePlus rgbImage;
	/** plugin's name */
	public static final String PLUGIN_NAME = "Trainable Weka Segmentation";
	/** plugin's current version */
	public static final String PLUGIN_VERSION =  Weka_Save.versionName;

	/** reference to the segmentation backend */
	private WekaSegmentation_Save wekaSegmentation = null;

	/** image to display on the GUI */
	private ImagePlus displayImage = null;
	/** image to be used in the training */
	private ImagePlus trainingImage = null;
	/** result image after classification */
	private ImagePlus classifiedImage = null;
	/** GUI window
	 *  */
	private CustomWindow win = null;
	/** number of classes in the GUI */
	private int numOfClasses = 2;
	/** array of number of traces per class */
	private int[] traceCounter = new int[WekaSegmentation_Save.MAX_NUM_CLASSES];
	/** flag to display the overlay image */
	private boolean showColorOverlay = false;
	/** executor service to launch threads for the plugin methods and events */
	private final ExecutorService exec = Executors.newFixedThreadPool(1);

	/** train classifier button */
	private JButton debugButton = null;
	/** train classifier button */
	private JButton manageColorsButton = null;
	/** train classifier button */
	private JButton loadClassSetupButton = null;
	/** train classifier button */
	private JButton saveClassSetupButton = null;
	/** train classifier button */
	private JButton toggleHyperButton = null;
	private JButton toggleTrainingButton = null;
	private JButton togglePhotoButton = null;



	private JButton loadExamplesButton = null;
	/** train classifier button */
	private JButton saveExamplesButton = null;
	/** train classifier button */
	private JButton trainButton = null;

	private JButton trainSeparatedButton = null;
	/** toggle overlay button */
	private JButton overlayButton = null;
	/** create result button */
	private JButton resultButton = null;
	/** get probability maps button */
	private JButton probabilityButton = null;
	/** plot result button */
	private JButton plotButton = null;
	/** apply classifier button */
	private JButton applyButton = null;
	/** load classifier button */
	private JButton loadClassifierButton = null;
	/** save classifier button */
	private JButton saveClassifierButton = null;
	/** load data button */
	private JButton loadDataButton = null;
	/** save data button */
	private JButton saveDataButton = null;
	/** settings button */
	private JButton settingsButton = null;
	private JButton runTestButton = null;
	private JButton runOnOtherButton = null;
	/** Weka button */
	private JButton wekaButton = null;
	/** create new class button */
	private JButton addClassButton = null;

	/** array of roi list overlays to paint the transparent rois of each class */
	private RoiListOverlay [] roiOverlay = null;

	/** available colors for available classes */
	private Color[] colors = new Color[]{Color.red, Color.green, Color.blue,
			Color.cyan, Color.magenta};

	/** Lookup table for the result overlay image */
	private LUT overlayLUT = null;

	/** array of trace lists for every class */
	private java.awt.List[] exampleList = null;
	/** array of buttons for adding each trace class */
	private JButton [] addExampleButton = null;

	// Macro recording constants (corresponding to  
	// static method names to be called)
	/** name of the macro method to add the current trace to a class */
	public static final String ADD_TRACE = "addTrace";
	/** name of the macro method to delete the current trace */
	public static final String DELETE_TRACE = "deleteTrace";
	/** name of the macro method to train the current classifier */
	public static final String TRAIN_CLASSIFIER = "trainClassifier";
	/** name of the macro method to toggle the overlay image */
	public static final String TOGGLE_OVERLAY = "toggleOverlay";
	/** name of the macro method to get the binary result */
	public static final String GET_RESULT = "getResult";
	/** name of the macro method to get the probability maps */
	public static final String GET_PROBABILITY = "getProbability";
	/** name of the macro method to plot the threshold curves */
	public static final String PLOT_RESULT = "plotResultGraphs";
	/** name of the macro method to apply the current classifier to an image or stack */
	public static final String APPLY_CLASSIFIER = "applyClassifier";
	/** name of the macro method to load a classifier from file */
	public static final String LOAD_CLASSIFIER = "loadClassifier";
	/** name of the macro method to save the current classifier into a file */
	public static final String SAVE_CLASSIFIER = "saveClassifier";
	/** name of the macro method to load data from an ARFF file */
	public static final String LOAD_DATA = "loadData";
	/** name of the macro method to save the current data into an ARFF file */
	public static final String SAVE_DATA = "saveData";
	/** name of the macro method to create a new class */
	public static final String CREATE_CLASS = "createNewClass";
	/** name of the macro method to launch the Weka Chooser */
	public static final String LAUNCH_WEKA = "launchWeka";
	/** name of the macro method to enable/disable a feature */
	public static final String SET_FEATURE = "setFeature";
	/** name of the macro method to set the membrane thickness */
	public static final String SET_MEMBRANE_THICKNESS = "setMembraneThickness";
	/** name of the macro method to set the membrane patch size */
	public static final String SET_MEMBRANE_PATCH = "setMembranePatchSize";
	/** name of the macro method to set the minimum kernel radius */
	public static final String SET_MINIMUM_SIGMA = "setMinimumSigma";
	/** name of the macro method to set the maximum kernel radius */
	public static final String SET_MAXIMUM_SIGMA = "setMaximumSigma";
	/**
	 * name of the macro method to enable/disable the class homogenization
	 * @deprecated use SET_BALANCE instead
	 **/
	public static final String SET_HOMOGENIZATION = "setClassHomogenization";
	/** name of the macro method to enable/disable the class balance */
	public static final String SET_BALANCE = "setClassBalance";
	/** name of the macro method to set a new classifier */
	public static final String SET_CLASSIFIER = "setClassifier";
	/** name of the macro method to save the feature stack into a file or files */
	public static final String SAVE_FEATURE_STACK = "saveFeatureStack";
	/** name of the macro method to change a class name */
	public static final String CHANGE_CLASS_NAME = "changeClassName";
	/** name of the macro method to set the overlay opacity */
	public static final String SET_OPACITY = "setOpacity";
	/** boolean flag set to true while training */
	private boolean trainingFlag = false;
	private boolean existingUserColormap=false;
	private int[] colorChoices=new int[WekaSegmentation_Save.MAX_NUM_CLASSES];
	//                                                0     1       2      3      4      5     6          7       8       9           10           11       12       13
	private final String[] colorsStr=new String[] {"Red","Green","Blue","Cyan","Pink","White","Yellow","Black","Gray","Dark Gray","Light Gray","Magenta","Orange","Custom color..."};
	private final Color[] colorsTab=new Color[] {Color.red,Color.green,Color.blue,Color.cyan,Color.pink,Color.white,Color.yellow,Color.black,Color.gray,Color.DARK_GRAY,Color.LIGHT_GRAY,Color.magenta,Color.orange};
	private boolean []isCustomColor=new boolean[WekaSegmentation_Save.MAX_NUM_CLASSES];
	/**
	 * Basic constructor for graphical user interface use
	 */


	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		if(Weka_Save.testing) {
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/ML_CEP/IMAGES/imgHybrid.tif");
//			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/SE5.tif");
			img.show();
		}
		Weka_Save ws=new Weka_Save();
		ws.run("");//
	}

	
	
	public void runOnOtherImage() {
		boolean debug=true;	
		boolean compute=true;
		if(debug)loadClassSetup("/home/fernandr/Bureau/ML_CEP/SETUPS/setup.MCLASS");

		for (int s=10;s<12;s++) {
			String spec= stringSpecimens[s];
			String nomImg="/home/fernandr/Bureau/Traitements/Cep5D/"+spec+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/hyperimage_THIN.tif";
			System.out.println("STARTING SEGMENTATION ON "+nomImg);
			String outputPath=debug ? "/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+spec+"/" : VitiDialogs.chooseDirectoryUI("Choose a directory for image exportation", "Select");
			new File(outputPath).mkdirs();
			System.out.println("Load classifier...");
			loadClassifierWithoutOtherActions(debug ? "/home/fernandr/Bureau/ML_CEP/MODELS/classifier.model":null);
			System.out.println("Ok. Load image...");
			ImagePlus hyperNew=(debug ? IJ.openImage(nomImg) : VitiDialogs.chooseOneImageUI("Choose hyperimage to apply classifier", ""));
			System.out.println("Ok. Starting from new hyperimage...");
			int batchSize=44;
			startFromNewHyperImage(hyperNew);
			System.out.println("Ok. Get updated image...");
			ImagePlus[]trainNew=getUpdatedTrainingHyperImage();
			int Z=trainNew[0].getStackSize();
			int Y=trainNew[0].getHeight();
			int X=trainNew[0].getWidth();
			int nBatches=(int)Math.ceil(Z*1.0/batchSize);
			System.out.println("Ok. Nb batches : "+nBatches+" of size 44");
			ImagePlus[][]retTab=new ImagePlus[2][nBatches];
			if(compute) {

				for(int nB=0;nB<nBatches;nB++) {
					System.out.println("Computing mini-batches number "+nB+" for hyperimage with dimZ="+Z+" slices...");
					int firstZ=nB*batchSize;
					int lastZ=Math.min((nB+1)*batchSize-1,Z-1);
					System.out.println("Processing batch from "+firstZ+" to "+lastZ);
					String outputBatch=outputPath+"Batch_"+nB+"/";
					new File(outputBatch).mkdirs();
					ImagePlus hyperPti= new Duplicator().run(hyperNew, 1, 1, firstZ+1, lastZ+1, 1, 9);
					IJ.saveAsTiff(hyperPti, outputBatch+"hyper_"+nB+".tif");			
					startFromNewHyperImage(hyperPti);
					FeatureStackArray fsa=buildFeatureStackArrayRGBSeparatedMultiThreadedV2(getUpdatedTrainingHyperImage(),tabHyperFeatures,(int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()));
					wekaSegmentation.setFeatureStackArray(fsa);
					wekaSegmentation.setUpdateFeatures(false);
					wekaSegmentation.applyClassifier(false);//False means no probability maps
					retTab[0][nB] = wekaSegmentation.getClassifiedImage();
					retTab[0][nB].setDisplayRange(0, 255);
					IJ.run(retTab[0][nB],"8-bit","");
					IJ.saveAsTiff(retTab[0][nB], outputBatch+"segmentation_"+nB+".tif");						
					if(s==0 || s==9) {
						wekaSegmentation.applyClassifier(true);//False means no probability maps
						retTab[1][nB] = wekaSegmentation.getClassifiedImage();
						IJ.saveAsTiff(retTab[1][nB], outputBatch+"probabilities_"+nB+".tif");					
					}
				}
			}
			int sum=0;
			for(int nB=0;nB<nBatches;nB++) {
				retTab[0][nB] = IJ.openImage(outputPath+"Batch_"+nB+"/segmentation_"+nB+".tif");
				System.out.println("Debug : "+TransformUtils.stringVector(VitimageUtils.getDimensions(retTab[0][nB]),"Batch"+nB));
				sum+=retTab[0][nB].getStackSize();
			}
			ImagePlus ret=Concatenator.run(retTab[0]);
			IJ.run(ret,"Stack to Hyperstack...", "order=xyzct channels=1 slices="+sum+" frames=1 display=Grayscale");
			IJ.saveAsTiff(ret, outputPath+"segmentation.tif");
			for(int cl=0;cl<5;cl++) {ImagePlus re=VitimageUtils.thresholdByteImage(ret, cl, cl+1);IJ.saveAsTiff(re, outputPath+"segmentation_"+wekaSegmentation.getClassLabels()[cl]+".tif");}
		}
	}


	public void runTest() {
		GenericDialog gd=new GenericDialog("Select configuration");
		gd.addChoice("Select test to run", new String[] {"Exp_1_WEKA_SPECIMEN_CROSS_VALIDATION one cep versus all, 12 experiences, few minutes",
														 "Exp_2_WEKA_SPECIMEN_TWO_FOLD_CROSS_VALIDATION two ceps versus all, 66 experiences, half an hour",
														 "Exp_3_WEKA_SYMPTOM_CROSS_VALIDATION one symptom versus all, 4 experiences, few minutes",
														 "Exp_4_TWO_FOLD among 16 configurations of imaging devices, 1056 experiences, overnighter",
														 "Exp_5_TWO_FOLD among 8 successive resolutions, 528 experiences, 2 hours",
														 "Just have a look on a subsample version of examples"},
														"Exp_2_WEKA_SPECIMEN_TWO_FOLD_CROSS_VALIDATION two ceps versus all, 66 experiences, half an hour");														
		gd.showDialog();						
		int choice=gd.getNextChoiceIndex();

		int optiNfeat=14;
		int optiNtrees=200;
		int optiSigMin=1;
		int optiSigMax=128;
		int[]lookupClasses=null;
		String[]strNew=null;
		wekasave.nbTrees=optiNtrees;
		wekasave.nbFeatures=optiNfeat;
		wekaSegmentation.setMaximumSigma(optiSigMax);
		wekaSegmentation.setMinimumSigma(optiSigMin);
		loadClassSetup("/home/fernandr/Bureau/ML_CEP/SETUPS/setup.MCLASS");
		String optiExampleSet="/home/fernandr/Bureau/ML_CEP/EXAMPLES/EXAMPLES_MOYEN/examples.MROI";
//		String optiFeatureStack="/home/fernandr/Bureau/ML_CEP/EXAMPLES/EXAMPLES_MOYEN/examples.MROI";
		String examplesPath=optiExampleSet;

		
		if(choice==0) {//CROSS VALID ONE-FOLD
			String resultPath="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_1_ONE_FOLD/";
			loadFeatureStack(wekasave, "/home/fernandr/Bureau/ML_CEP/FEATURESTACK/FEATURESTACK_1_128/");
			evaluateCross(null,examplesPath,resultPath,WEKA_SPECIMEN_CROSS_VALIDATION,true,null,null);							
		}
		
		
		if(choice==1) {//CROSS VALID TWO-FOLD
			String resultPath="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_2_TWO_FOLD/";
			loadFeatureStack(wekasave, "/home/fernandr/Bureau/ML_CEP/FEATURESTACK/FEATURESTACK_1_128/");
			evaluateCross(null,examplesPath,resultPath,WEKA_SPECIMEN_TWO_FOLD_CROSS_VALIDATION,true,null,null);							
		}
		
	
		if(choice==2) {//CROSS VALID SYMPTOM-FOLD
			String resultPath="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_3_SYMPTOM/";
			loadFeatureStack(wekasave, "/home/fernandr/Bureau/ML_CEP/FEATURESTACK/FEATURESTACK_1_128/");
			evaluateCross(null,examplesPath,resultPath,WEKA_SYMPTOM_CROSS_VALIDATION,true,null,null);							
		}
		
		
		if(choice==3) {//TWO FOLD AMONG MODALITIES COMBINATION
			String resultPath="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_4_MODALITIES/";
			boolean []tab=new boolean[4];
			for(int i=1;i<16;i++) {
				tab[0]=(i>=8);
				tab[1]=i-(8*(tab[0] ? 1 : 0))>=4;
				tab[2]=i-(8*(tab[0] ? 1 : 0))-(4*(tab[1] ? 1 : 0))>=2;
				tab[3]=(i%2)==1;
				System.out.println("\n\n\n\n\nCode "+i+" = "+tab[0]+" , "+tab[1]+" , "+tab[2]+" , "+tab[3]+"");
				String name="CROSSMOD_"+(tab[0] ? "1" : "0")+(tab[1] ? "1" : "0")+(tab[2] ? "1" : "0")+(tab[3] ? "1" : "0");
				wekasave.tabHyperModalities=tab;
				wekasave.goodTrainerWasLastUsedAndParamsHadNotMove=false;
				String resultPathMod=resultPath+"MOD_"+name+"/";
				File dir = new File(resultPathMod);dir.mkdirs();
				evaluateCross(null,examplesPath,resultPathMod,WEKA_SPECIMEN_TWO_FOLD_CROSS_VALIDATION,true,lookupClasses,strNew);
			}
		}
	
		if(choice==4) {//TWO FOLD AMONG RESOLUTIONS
			ImagePlus hyperTmp=VitimageUtils.imageCopy(hyperImage);
			boolean buildExamples=true;
			boolean buildFeatures=false;
			boolean onlyConsensusExamples=true;
			boolean stepResults=true;
			String resultPath="/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_5_RESOLUTIONS"+(onlyConsensusExamples?"_CONSENSUS" :"")+"/";
			double voxX0=VitimageUtils.getVoxelSizes(hyperTmp)[0];
			for (int subSampl=1;subSampl<2 ; subSampl++) {
				System.out.println("\n\n\n\nTest avec facteur de subsample="+subSampl);
				String outExamplesPath="/home/fernandr/Bureau/ML_CEP/EXAMPLES/EXAMPLES_MOYEN_SUB"+(onlyConsensusExamples?"_CONSENSUS" :"")+"/SUB_"+subSampl+"/";
				File dir=new File(outExamplesPath);dir.mkdirs();
				outExamplesPath=outExamplesPath+"examples.MROI";
				String outImgPath="/home/fernandr/Bureau/ML_CEP/IMAGES/imgHybrid_Sub"+subSampl+".tif";
				String resultPathMod=resultPath+"SUB_"+subSampl+"/";
				dir = new File(resultPathMod);dir.mkdirs();
				String outFeatPath="/home/fernandr/Bureau/ML_CEP/FEATURESTACK/FEATURESTACK_SUB_"+subSampl+"/";
				dir = new File(outFeatPath);dir.mkdirs();
				System.out.println("String charges");
				if(subSampl>=8*voxX0)wekasave.fixedMaxSigma=optiSigMax/8;
				else if(subSampl>=4*voxX0)wekasave.fixedMaxSigma=optiSigMax/4;
				else if(subSampl>=2*voxX0)wekasave.fixedMaxSigma=optiSigMax/2;
//				this.wekaSegmentation.setMaximumSigma(wekasave.fixedMaxSigma);
//			wekaSegmentation.setFeaturesDirty();
				
				System.out.println("Sigma charges");
				if(buildExamples) {
					System.out.println("Fixer le sigma a "+wekaSegmentation.getMaximumSigma());
					System.out.println("Ok. Convertir image et examples avec facteur = "+(1.0/subSampl));
					startFromNewHyperImage(hyperTmp);
					ImagePlus imgNew=convertImageAndExamplesToLowerXYResolution(voxX0/subSampl,"/home/fernandr/Bureau/ML_CEP/IMAGES/imgHybrid.tif",optiExampleSet,outExamplesPath,outImgPath,onlyConsensusExamples);
					System.out.println("Ok. demarrer de cette nouvelle image");
					if(buildFeatures) {
						loadExamplesFromFile(outExamplesPath,0,null,null,null);
						startFromNewHyperImage(imgNew);
						System.out.println("Ok. Calculer la feature stack en utilisant "+" RX ?"+wekasave.tabHyperModalities[0]+" T1 ?"+wekasave.tabHyperModalities[1]+" T2 ?"+wekasave.tabHyperModalities[2]+" M0 ?"+wekasave.tabHyperModalities[3]);
						FeatureStackArray fsa=buildFeatureStackArrayRGBSeparatedMultiThreadedV2(getUpdatedTrainingHyperImage(),tabHyperFeatures,(int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekasave.fixedMaxSigma));
						System.out.println("Ok. Sauver la feature stack");
						wekaSegmentation.setFeatureStackArray(fsa);
						wekaSegmentation.setUpdateFeatures(false);
						saveFeatureStack(outFeatPath);
						saveFeatureSetup(outFeatPath+"setup.MFEATURE");
						
						System.out.println("Ok. ajuster les parametres et charger la feature stack");
						goodTrainerWasLastUsedAndParamsHadNotMove=true;
						badTrainerWasLastUsed=false;
						System.out.println("Ok. Fini boucle.");
					}
				}
				if(stepResults) {
					System.out.println("Yolo!");
					startFromNewHyperImage(hyperTmp);
					ImagePlus img=IJ.openImage(outImgPath);
					System.out.println("Starting from new hyperimage");VitimageUtils.waitFor(1000);
					startFromNewHyperImage(img);
					System.out.println("ok. Loading feature stack");VitimageUtils.waitFor(1000);
					loadFeatureStack(this,  outFeatPath);
					System.out.println("ok. Loading class setup");VitimageUtils.waitFor(1000);
					loadClassSetup("/home/fernandr/Bureau/ML_CEP/SETUPS/setup.MCLASS");
					System.out.println("ok. Loading examples");VitimageUtils.waitFor(1000);
					loadExamplesFromFile(outExamplesPath,0,null,null,null);
					System.out.println("ok. Evaluate cross");VitimageUtils.waitFor(1000);
					evaluateCross(null,outExamplesPath,resultPathMod,WEKA_SPECIMEN_TWO_FOLD_CROSS_VALIDATION,true,lookupClasses,strNew);
					System.out.println("Ok");
				}			
			}		
		}
		if(choice==5) {//Just have a look on a subsampled version
			GenericDialog gd2=new GenericDialog("Select subsampling factor");
			gd2.addChoice("Select factor", new String[] {"1","2","3","4","5","6","7","8","9","10"},
															"1");														
			gd2.showDialog();						
			int val=gd2.getNextChoiceIndex()+1;
			
			String outExamplesPath="/home/fernandr/Bureau/ML_CEP/EXAMPLES/EXAMPLES_MOYEN_SUB/SUB_"+val+"/examples.MROI";			
			String outFeatPath="/home/fernandr/Bureau/ML_CEP/FEATURESTACK/FEATURESTACK_SUB_"+val+"/";
			boolean stepBuild=false;
			System.out.println("Loading image");
			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/ML_CEP/IMAGES/imgHybrid_Sub"+val+".tif");
			System.out.println("Ok. Starting again");
			startFromNewHyperImage(img);
			System.out.println("Ok.Loading features");
			loadFeatureStack(this,  outFeatPath);
			System.out.println("Ok.Loading setup");
			loadClassSetup("/home/fernandr/Bureau/ML_CEP/SETUPS/setup.MCLASS");
			System.out.println("Ok.Loading examples");
			loadExamplesFromFile(outExamplesPath,0,null,null,null);
			System.out.println("Load finished");
		}		
	
	}


	
	public void startFromNewHyperImage(ImagePlus imgNew) {
		isHyper=true;
		tabI=new ImagePlus[9];
		VitimageUtils.printImageResume(imgNew);
		hyperImage=VitimageUtils.imageCopy(imgNew);
		hyperTab=VitimageUtils.stacksFromHyperstackFast(hyperImage,9);
		System.out.println("Starting from new image. ");
		System.out.println("Building training images...");
		trainingImageRX=VitimageUtils.compositeRGBByte(hyperTab[2],hyperTab[3],hyperTab[1],1,1,1);
		trainingImageRX=VitimageUtils.writeTextOnImage("Training image T1-T2-RX", trainingImageRX, 9, 0);

		System.out.println("Building display image 1...");
		rgbImage=VitimageUtils.compositeRGBByte(hyperTab[6],hyperTab[7],hyperTab[8],1,1,1);
		for(int i=0;i<9;i++)tabI[i]=VitimageUtils.imageCopy(rgbImage);
		rgbHyper = Concatenator.run(tabI);
		IJ.run(rgbHyper,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+(rgbImage.getStackSize())+" frames=9");

		System.out.println("Building display image 2...");
		trainingImage=trainingImageRX;
		wekaSegmentation.setTrainingImage(trainingImage);
		for(int i=0;i<9;i++)tabI[i]=VitimageUtils.imageCopy(trainingImage);
		trainingHyper = Concatenator.run(tabI);
		IJ.run(trainingHyper,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+(rgbImage.getStackSize())+" frames=9");
		wekasave.switchViewToHyperimage();
//		wekasave.displayImage=
		//				displayImage=VitimageUtils.imageCopy(hyperImage);
		//	win.setImagePlus(displayImage);		
	}

	public static double[][][]assembleConfusionsTab(double[][][][]tabIn){
		int nClasses=tabIn[0][0].length;int nTrial=tabIn.length;
		System.out.println("nClasses="+nClasses);
		System.out.println("nTrial="+nTrial);
		double[][][]tabOut=new double[2][nClasses][nClasses];
		for(int n1=0;n1<nClasses;n1++)for(int n2=0;n2<nClasses;n2++)for(int tr=0;tr<nTrial;tr++) {tabOut[0][n1][n2]+=tabIn[tr][0][n1][n2];tabOut[1][n1][n2]+=tabIn[tr][1][n1][n2];}
		return tabOut;		
	}





	public ImagePlus buildConfusionImage(int[][][] tabIn,int nClasses,ImagePlus canalPhotoGris) {
		System.out.println("Construction de l image des confusions");
		int nOk;
		int nT=tabIn.length;
		int nC=nClasses;
		int[]dims=VitimageUtils.getDimensions(canalPhotoGris);
		int X=dims[0];
		int Y=dims[1];
		int Z=dims[2];
		int nExTot=0;
		int x,y,z,index,act,pred,nErreurs,max,indmax;
		//Construire l image support, canal vert et canal rouge, et les images erreurs individuelles
		System.out.println("preparation des donnees");
		ImagePlus []trials=new ImagePlus[nT];
		ImagePlus martyr=VitimageUtils.imageCopy(canalPhotoGris);
		martyr=VitimageUtils.makeOperationOnOneImage(martyr,VitimageUtils.OP_MULT,0,true);
		ImagePlus  outR=VitimageUtils.imageCopy(martyr);
		ImagePlus  outG=VitimageUtils.imageCopy(martyr);
		byte[][][]dataTrial=new byte[nT][Z][];
		for(int t=0;t<nT;t++) {
			trials[t]=VitimageUtils.imageCopy(martyr);
			for(int zz=0;zz<Z;zz++)dataTrial[t][zz]=(byte[])(trials[t].getStack().getProcessor(zz+1).getPixels()) ;
		}
		byte[][]dataR=new byte[Z][];
		byte[][]dataG=new byte[Z][];
		for(int zz=0;zz<Z;zz++) {
			dataR[zz]=(byte[])(outR.getStack().getProcessor(zz+1).getPixels()) ;
			dataG[zz]=(byte[])(outG.getStack().getProcessor(zz+1).getPixels()) ;
		}


		//Pour chaque cas rencontre, inventorier le resultat dans dataTrial : 255 si c est ok, le numero de classe predit sinon
		String s="";
		for(int t=0;t<nT;t++) {
			int nEx=tabIn[t][5].length;
			nExTot+=nEx;
			for(int ex=0;ex<nEx;ex++) {
				act= tabIn[t][5][ex];
				pred= tabIn[t][6][ex];
				x= tabIn[t][7][ex];
				y= tabIn[t][8][ex];
				z= tabIn[t][9][ex]-1;
				index=y*X+x;
				if(act==pred)dataTrial[t][z][index]=(byte)(255 & 0xff);
				else dataTrial[t][z][index]=(byte)((pred+1) & 0xff);
			}
		}

		//Pour chaque case, construire les valeurs R et G. 
		//Si erreurs=0 (0,255) .
		//Si erreur=1 (240+classe,240) .
		//Si erreur=2 et deux fois la meme, (240+classe1,170), sinon 240,160 
		//Si erreur=3 et deux fois la même, (240+classe1,100), sinon 240,80
		//Si erreur>=4 et une erreur>=moitie, (240+classe1,max(0,50-nErreurs)), sinon 240,0,max(0,50-nErreurs))
		int []preds;
		for(int t=0;t<nT;t++) {
			int nEx=tabIn[t][5].length;
			nExTot+=nEx;
			for(int ex=0;ex<nEx;ex++) {
				x= tabIn[t][7][ex];
				y= tabIn[t][8][ex];
				z= tabIn[t][9][ex]-1;
				index=y*X+x;
				if(dataR[z][index]==(byte)(0 & 0xff) && dataG[z][index]==(byte)(0 & 0xff)) {//Point pas encore traite lors d un autre trial
					//					System.out.print("a xyz="+x+","+y+","+z+"  ");
					preds=new int[256];
					nErreurs=0;
					for(int t2=0;t2<nT;t2++) {
						preds[(int)((dataTrial[t2][z][index] & 0xff))]++;
						//						System.out.print("| t2="+t2+" augmente "+(int)((dataTrial[t2][z][index] & 0xff))+" ");
					}
					max=0;indmax=1;nOk=preds[255];
					for(int i=1;i<=nC;i++) {
						nErreurs+=preds[i];
						if(preds[i]>max) {max=preds[i];indmax=i;}
					}
					//System.out.println("\n Nerreurs="+nErreurs+"+Nok="+nOk+"\n");
					if(nErreurs==0) {dataR[z][index]=(byte)(0 & 0xff);dataG[z][index]=(byte)(255 & 0xff);}
					if(nErreurs==1) {dataR[z][index]=(byte)((240+indmax) & 0xff);dataG[z][index]=(byte)(200 & 0xff);}
					if(nErreurs==2) {dataR[z][index]=(byte)((240+(preds[indmax]==2 ? indmax : 0) & 0xff));dataG[z][index]=(byte)(150 & 0xff);}
					if(nErreurs==3) {dataR[z][index]=(byte)((240+(preds[indmax]==2 ? indmax : 0) & 0xff));dataG[z][index]=(byte)(100 & 0xff);}
					if(nErreurs>3) {dataR[z][index]=(byte)((240+(preds[indmax]>=(nErreurs/2) ? indmax : 0) & 0xff));dataG[z][index]=(byte)((Math.min(40,nErreurs)) & 0xff);}
				}
			}
		}
		ImagePlus tmp=VitimageUtils.thresholdByteImage(outG, 1, 256);
		IJ.run(tmp, "Invert", "stack");
		tmp=VitimageUtils.makeOperationOnOneImage(tmp, VitimageUtils.OP_DIV,255,false);
		tmp=VitimageUtils.makeOperationBetweenTwoImages(tmp, canalPhotoGris,VitimageUtils.OP_MULT,false);
		ImagePlus out=VitimageUtils.compositeRGBLByte(outR, outG, martyr,tmp, 1,1, 1, 1);
		System.out.println("Image construite. out.");
		return out;
	}





	public Weka_Save(){
		evaluate3d=new Evaluate3D();
		wekasave=this;
		// Create overlay LUT
		final byte[] red = new byte[ 256 ];
		final byte[] green = new byte[ 256 ];
		final byte[] blue = new byte[ 256 ];
		// assign colors to classes				
		colors = new Color[ WekaSegmentation_Save.MAX_NUM_CLASSES ];

		for(int i = 0 ; i < WekaSegmentation_Save.MAX_NUM_CLASSES; i++)
		{
			colorChoices[i]=i%13;
			colors[i]=colorsTab[colorChoices[i]];
			red[i] = (byte) colors[ i ].getRed();
			green[i] = (byte) colors[ i ].getGreen();
			blue[i] = (byte) colors[ i ].getBlue();
		}
		overlayLUT = new LUT(red, green, blue);

		exampleList = new java.awt.List[WekaSegmentation_Save.MAX_NUM_CLASSES];
		addExampleButton = new JButton[WekaSegmentation_Save.MAX_NUM_CLASSES];

		roiOverlay = new RoiListOverlay[WekaSegmentation_Save.MAX_NUM_CLASSES];
		toggleHyperButton = new JButton("View hyperimage");
		toggleHyperButton.setToolTipText("Switch between views");
		toggleHyperButton.setEnabled(false);
		toggleTrainingButton = new JButton("View training img");
		toggleTrainingButton.setToolTipText("Switch between views");
		toggleTrainingButton.setEnabled(true);
		togglePhotoButton = new JButton("View photo");
		togglePhotoButton.setToolTipText("Switch between views");
		togglePhotoButton.setEnabled(true);
		loadExamplesButton = new JButton("Load examples");
		loadExamplesButton.setToolTipText("Load examples from a .MROI file");
		saveExamplesButton = new JButton("Save examples");
		saveExamplesButton.setToolTipText("Save examples to a .MROI file");

		runTestButton = new JButton("Run performance tests");
		runTestButton.setToolTipText("Current test=recognition rate VS modalities");
		runOnOtherButton = new JButton("Classify all ceps");
		runOnOtherButton.setToolTipText("");
		debugButton = new JButton("Debug info");
		debugButton.setToolTipText("");
		manageColorsButton = new JButton("Manage colors");
		manageColorsButton.setToolTipText("Set color in segmentation visualization for each class");
		loadClassSetupButton = new JButton("Load setup");
		loadClassSetupButton.setToolTipText("Load a setup of class with associated names and colours");
		saveClassSetupButton = new JButton("Save setup");
		saveClassSetupButton.setToolTipText("Save a setup of class with associated names and colours to a .MCLASS file");

		//trainButton = new JButton("Standard training");
		//trainButton.setToolTipText("Start training the classifier using standard weka behaviour");

		trainSeparatedButton = new JButton("Hyperimage training");
		trainSeparatedButton.setToolTipText("Start training the classifier using hyperimage behaviour.\nThe features will be computed with the 4 channels");
		overlayButton = new JButton("Toggle overlay");
		overlayButton.setToolTipText("Toggle between current segmentation and original image");
		overlayButton.setEnabled(false);

		resultButton = new JButton("Create result");
		resultButton.setToolTipText("Generate result image");
		resultButton.setEnabled(false);

		probabilityButton = new JButton("Get probability");
		probabilityButton.setToolTipText("Generate current probability maps");
		probabilityButton.setEnabled(false);

		plotButton = new JButton("Plot result");
		plotButton.setToolTipText("Plot result based on different metrics");
		plotButton.setEnabled(false);

		applyButton = new JButton ("Apply classifier");
		applyButton.setToolTipText("Apply current classifier to a single image or stack");
		applyButton.setEnabled(false);

		loadClassifierButton = new JButton ("Load classifier");
		loadClassifierButton.setToolTipText("Load Weka classifier from a file");

		saveClassifierButton = new JButton ("Save classifier");
		saveClassifierButton.setToolTipText("Save current classifier into a file");
		saveClassifierButton.setEnabled(false);

		loadDataButton = new JButton ("Load data");
		loadDataButton.setToolTipText("Load previous segmentation from an ARFF file");

		saveDataButton = new JButton ("Save data");
		saveDataButton.setToolTipText("Save current segmentation into an ARFF file");
		saveDataButton.setEnabled(false);

		addClassButton = new JButton ("Create new class");
		addClassButton.setToolTipText("Add one more label to mark different areas");

		settingsButton = new JButton ("Settings");
		settingsButton.setToolTipText("Display settings dialog");

		/** The Weka icon image */
		ImageIcon icon = new ImageIcon(Weka_Save.class.getResource("/trainableSegmentation/images/weka.png"));
		wekaButton = new JButton( icon );
		wekaButton.setToolTipText("Launch Weka GUI chooser");

		showColorOverlay = false;
		//trainButton.setEnabled(false);
	}


	/** Thread that runs the training. We store it to be able to
	 * to interrupt it from the GUI */
	private Thread trainingTask = null;

	/**
	 * Button listener
	 */
	private ActionListener listener = new ActionListener() {





		public void actionPerformed(final ActionEvent e) {


			final String command = e.getActionCommand();

			// listen to the buttons on separate threads not to block
			// the event dispatch thread
			exec.submit(new Runnable() {


				public void run()

				{
					if(e.getSource() == runTestButton)runTest();
					if(e.getSource() == runOnOtherButton)runOnOtherImage();
					else if(e.getSource() == trainSeparatedButton)runStopTrainingRGBSeparated(command);
					//else if(e.getSource() == trainButton)runStopTraining(command);	
					else if(e.getSource() == loadClassSetupButton)loadClassSetup(null);			
					else if(e.getSource() == manageColorsButton)manageColors();	
					else if(e.getSource() == debugButton)debugInfo();	
					else if(e.getSource() == saveClassSetupButton)saveClassSetup(null);	
					else if(e.getSource() == loadExamplesButton) loadExamples();
					else if(e.getSource() == toggleHyperButton)toggleHyperView(0);	
					else if(e.getSource() == toggleTrainingButton)toggleHyperView(1);	
					else if(e.getSource() == togglePhotoButton)toggleHyperView(2);	
					else if(e.getSource() == saveExamplesButton)saveExamples();			

					else if(e.getSource() == overlayButton)toggleOverlay();
					else if(e.getSource() == resultButton)showClassificationImage();
					else if(e.getSource() == probabilityButton)showProbabilityImage();
					else if(e.getSource() == plotButton)plotResult();
					else if(e.getSource() == applyButton)applyClassifierToTestData();
					else if(e.getSource() == loadClassifierButton){
						loadClassifierWithoutOtherActions(null);
						
						win.updateButtonsEnabling();
					}
					else if(e.getSource() == saveClassifierButton){
						win.setButtonsEnabled( false );
						saveClassifier();
						win.setButtonsEnabled( true);
					}
					else if(e.getSource() == loadDataButton)loadTrainingData();
					else if(e.getSource() == saveDataButton)saveTrainingData();
					else if(e.getSource() == addClassButton)addNewClass();
					else if(e.getSource() == settingsButton){
						showSettingsDialog();
						win.updateButtonsEnabling();
					}
					else if(e.getSource() == wekaButton)launchWeka();
					else{
						for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
						{
							if(e.getSource() == exampleList[i])
							{
								deleteSelected(e);
								break;
							}
							if(e.getSource() == addExampleButton[i])
							{
								addExamples(i);
								break;
							}
						}
						win.updateButtonsEnabling();
					}

				}



			});
		}
	};

	/**
	 * Item listener for the trace lists
	 */
	private ItemListener itemListener = new ItemListener() {

		public void itemStateChanged(final ItemEvent e) {

			exec.submit(new Runnable() {

				public void run() {

					for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
					{
						if(e.getSource() == exampleList[i])
							listSelected(e, i);
					}
				}
			});
		}
	};
	private int lastCurrentSlice;
	private int lastCurrentFrame;
	private boolean isRGB=false;
	public ImagePlus rgbHyper;
	public ImagePlus[] tabI;
	public ImagePlus trainingHyper;
	public int currentView=0;
	public ImagePlus trainingImageRX;
	public ImagePlus trainingImageM0;

	/**
	 * Custom canvas to deal with zooming an panning
	 */
	private class CustomCanvas extends OverlayedImageCanvas
	{
		/**
		 * default serial version UID
		 */
		private static final long serialVersionUID = 1L;

		CustomCanvas(ImagePlus imp)
		{
			super(imp);
			Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
			setMinimumSize(dim);
			setSize(dim.width, dim.height);
			setDstDimensions(dim.width, dim.height);
			addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent ke) {
					repaint();
				}
			});
		}
		//@Override
		public void setDrawingSize(int w, int h) {}

		public void setDstDimensions(int width, int height) {
			super.dstWidth = width;
			super.dstHeight = height;
			// adjust srcRect: can it grow/shrink?
			int w = Math.min((int)(width  / magnification), imp.getWidth());
			int h = Math.min((int)(height / magnification), imp.getHeight());
			int x = srcRect.x;
			if (x + w > imp.getWidth()) x = w - imp.getWidth();
			int y = srcRect.y;
			if (y + h > imp.getHeight()) y = h - imp.getHeight();
			srcRect.setRect(x, y, w, h);
			repaint();
		}

		//@Override
		public void paint(Graphics g) {
			Rectangle srcRect = getSrcRect();
			double mag = getMagnification();
			int dw = (int)(srcRect.width * mag);
			int dh = (int)(srcRect.height * mag);
			g.setClip(0, 0, dw, dh);

			super.paint(g);

			int w = getWidth();
			int h = getHeight();
			g.setClip(0, 0, w, h);

			// Paint away the outside
			g.setColor(getBackground());
			g.fillRect(dw, 0, w - dw, h);
			g.fillRect(0, dh, w, h - dh);
		}

		public void setImagePlus(ImagePlus imp)
		{
			super.imp = imp;
		}
	}

	/**
	 * Custom window to define the Trainable Weka Segmentation GUI
	 */
	private class CustomWindow extends StackWindow
	{
		/** default serial version UID */
		private static final long serialVersionUID = 1L;
		/** layout for annotation panel */
		private GridBagLayout boxAnnotation = new GridBagLayout();
		/** constraints for annotation panel */
		private GridBagConstraints annotationsConstraints = new GridBagConstraints();

		/** scroll panel for the label/annotation panel */
		private JScrollPane scrollPanel = null;

		/** panel containing the annotations panel (right side of the GUI) */
		private JPanel labelsJPanel = new JPanel();
		/** Panel with class radio buttons and lists */
		private JPanel annotationsPanel = new JPanel();
		/** buttons panel (left side of the GUI) */
		private JPanel buttonsPanel = new JPanel();
		/** training panel (included in the left side of the GUI) */
		private JPanel trainingJPanel = new JPanel();
		private JPanel viewJPanel = new JPanel();
		/** training panel (included in the left side of the GUI) */
		private JPanel manageJPanel = new JPanel();
		/** options panel (included in the left side of the GUI) */
		private JPanel optionsJPanel = new JPanel();
		/** main GUI panel (containing the buttons panel on the left,
		 *  the image in the center and the annotations panel on the right */
		private Panel all = new Panel();

		/** 50% alpha composite */
		private final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );
		/** 25% alpha composite */
		//final Composite transparency025 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f );
		/** opacity (in %) of the result overlay image */
		private int overlayOpacity = 33;
		/** alpha composite for the result overlay image */
		private Composite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);
		/** current segmentation result overlay */
		private ImageOverlay resultOverlay;

		/** boolean flag set to true when training is complete */
		private boolean trainingComplete = false;
		ScrollbarWithLabel ttSelector;
		Scrollbar slicesliceSelector;
		ScrollbarWithLabel ZZselector;

		/**
		 * Construct the plugin window
		 * 
		 * @param imp input image
		 */
		CustomWindow(ImagePlus imp)
		{
			super(imp, new CustomCanvas(imp));
			ttSelector=this.tSelector;
			slicesliceSelector=this.sliceSelector;
			ZZselector=this.zSelector;
			final CustomCanvas canvas = (CustomCanvas) getCanvas();

			// Check image dimensions to avoid a GUI larger than the
			// screen dimensions
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			double screenWidth = screenSize.getWidth();
			double screenHeight = screenSize.getHeight();

			// Zoom in if image is too small
			while( ( ic.getWidth() < screenWidth/2 ||
					ic.getHeight() < screenHeight/2 ) &&
					ic.getMagnification() < 32.0 )
			{
				final int canvasWidth = ic.getWidth();
				ic.zoomIn( 0, 0 );
				// check if canvas size changed (otherwise stop zooming)
				if( canvasWidth == ic.getWidth() )
				{
					ic.zoomOut(0, 0);
					break;
				}
			}
			// Zoom out if canvas is too large
			while( ( ic.getWidth() > 0.75 * screenWidth ||
					ic.getHeight() > 0.75 * screenHeight ) &&
					ic.getMagnification() > 1/72.0 )
			{
				final int canvasWidth = ic.getWidth();
				ic.zoomOut( 0, 0 );
				// check if canvas size changed (otherwise stop zooming)
				if( canvasWidth == ic.getWidth() )
				{
					ic.zoomIn(0, 0);
					break;
				}
			}
			// add roi list overlays (one per class)
			for(int i = 0; i < WekaSegmentation_Save.MAX_NUM_CLASSES; i++)
			{
				roiOverlay[i] = new RoiListOverlay();
				roiOverlay[i].setComposite( transparency050 );
				((OverlayedImageCanvas)ic).addOverlay(roiOverlay[i]);
			}

			// add result overlay
			resultOverlay = new ImageOverlay();
			resultOverlay.setComposite( overlayAlpha );
			((OverlayedImageCanvas)ic).addOverlay(resultOverlay);

			// Remove the canvas from the window, to add it later
			removeAll();

			setTitle( Weka_Save.PLUGIN_NAME + " " + Weka_Save.PLUGIN_VERSION );

			// Annotations panel
			annotationsConstraints.anchor = GridBagConstraints.NORTHWEST;
			annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
			annotationsConstraints.gridwidth = 1;
			annotationsConstraints.gridheight = 1;
			annotationsConstraints.gridx = 0;
			annotationsConstraints.gridy = 0;

			annotationsPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
			annotationsPanel.setLayout(boxAnnotation);

			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)//Creer panel // Creer Button. Ajouter le bouton au panel 
			{
				exampleList[i].addActionListener(listener);
				exampleList[i].addItemListener(itemListener);
				addExampleButton[i] = new JButton("Add to " + wekaSegmentation.getClassLabel(i));
				addExampleButton[i].setToolTipText("Add markings of label '" + wekaSegmentation.getClassLabel(i) + "'");

				annotationsConstraints.insets = new Insets(5, 5, 6, 6);

				annotationsPanel.add( addExampleButton[i], annotationsConstraints );
				annotationsConstraints.gridy++;

				annotationsConstraints.insets = new Insets(0,0,0,0);

				annotationsPanel.add( exampleList[i], annotationsConstraints );
				annotationsConstraints.gridy++;
			}

			// Select first class
			addExampleButton[0].setSelected(true);

			// Add listeners
			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)addExampleButton[i].addActionListener(listener);


			toggleHyperButton.addActionListener(listener);
			toggleTrainingButton.addActionListener(listener);
			togglePhotoButton.addActionListener(listener);
			loadExamplesButton.addActionListener(listener);
			saveExamplesButton.addActionListener(listener);
			debugButton.addActionListener(listener);
			runTestButton.addActionListener(listener);
			runOnOtherButton.addActionListener(listener);

			manageColorsButton.addActionListener(listener);
			loadClassSetupButton.addActionListener(listener);
			saveClassSetupButton.addActionListener(listener);
			//trainButton.addActionListener(listener);
			trainSeparatedButton.addActionListener(listener);
			overlayButton.addActionListener(listener);
			resultButton.addActionListener(listener);
			probabilityButton.addActionListener(listener);
			plotButton.addActionListener(listener);
			applyButton.addActionListener(listener);
			loadClassifierButton.addActionListener(listener);
			saveClassifierButton.addActionListener(listener);
			loadDataButton.addActionListener(listener);
			saveDataButton.addActionListener(listener);
			addClassButton.addActionListener(listener);
			settingsButton.addActionListener(listener);
			wekaButton.addActionListener(listener);

			// add especial listener if the training image is a stack
			if(null != sliceSelector)
			{
				// set slice selector to the correct number
				sliceSelector.setValue( imp.getCurrentSlice() );
				// add adjustment listener to the scroll bar
				sliceSelector.addAdjustmentListener(new AdjustmentListener() 
				{

					public void adjustmentValueChanged(final AdjustmentEvent e) {
						exec.submit(new Runnable() {
							public void run() {							
								if(e.getSource() == sliceSelector )
								{
									displayImage.killRoi();
									drawExamples();
									updateExampleLists();
									if(showColorOverlay)
									{
										updateResultOverlay();
										displayImage.updateAndDraw();
									}
								}

							}
						});

					}
				} );  

				// add especial listener if the training image is a hyperimage
				if(null != tSelector)
				{
					System.out.println("Running hyperimage extension...");
					System.out.println("Parameters handling...");
					isHyper=true;
					tabI=new ImagePlus[9];
					hyperImage=VitimageUtils.imageCopy(displayImage);
					hyperTab=VitimageUtils.stacksFromHyperstackFast(hyperImage,9);
					System.out.println("Building training images...");
					trainingImageRX=VitimageUtils.compositeRGBByte(hyperTab[2],hyperTab[3],hyperTab[1],1,1,1);
					trainingImageRX=VitimageUtils.writeTextOnImage("Training image T1-T2-RX", trainingImageRX, 9, 0);

					System.out.println("Building display image 1...");
					rgbImage=VitimageUtils.compositeRGBByte(hyperTab[6],hyperTab[7],hyperTab[8],1,1,1);
					for(int i=0;i<9;i++)tabI[i]=VitimageUtils.imageCopy(rgbImage);
					rgbHyper = Concatenator.run(tabI);
					IJ.run(rgbHyper,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+(rgbImage.getStackSize())+" frames=9");

					System.out.println("Building display image 2...");
					trainingImage=trainingImageRX;
					this.getWekaSegmentation().setTrainingImage(trainingImage);
					for(int i=0;i<9;i++)tabI[i]=VitimageUtils.imageCopy(trainingImage);
					trainingHyper = Concatenator.run(tabI);
					IJ.run(trainingHyper,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+(rgbImage.getStackSize())+" frames=9");

					System.out.println("Ok. Lancement interface.\n\n\n");
					// set slice selector to the correct number
					tSelector.setValue( 1 );
					// add adjustment listener to the scroll bar
					tSelector.addAdjustmentListener(new AdjustmentListener() 
					{

						public void adjustmentValueChanged(final AdjustmentEvent e) {
							exec.submit(new Runnable() {
								public void run() {							
									if(e.getSource() == tSelector)
									{
										displayImage.setSlice((displayImage.getFrame()-1)*trainingImage.getStackSize()+sliceSelector.getValue());
										displayImage.killRoi();
										drawExamples();
										updateExampleLists();
										if(showColorOverlay)
										{
											updateResultOverlay();
											displayImage.updateAndDraw();
										}
										//										AdjustmentEvent ae=new AdjustmentEvent(sliceSelector, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,AdjustmentEvent.UNIT_INCREMENT,sliceSelector.getValue()+1);
										//									AdjustmentEvent ae2=new AdjustmentEvent(sliceSelector, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,AdjustmentEvent.UNIT_DECREMENT,sliceSelector.getValue()-1);
									}

								}
							});

						}
					}
							);
				}


				// mouse wheel listener to update the rois while scrolling
				addMouseWheelListener(new MouseWheelListener() {

					@Override
					public void mouseWheelMoved(final MouseWheelEvent e) {

						exec.submit(new Runnable() {
							public void run() 
							{
								//IJ.log("moving scroll");
								displayImage.setSlice((displayImage.getFrame()-1)*trainingImage.getStackSize()+sliceSelector.getValue());
								displayImage.killRoi();
								drawExamples();
								updateExampleLists();
								if(showColorOverlay)
								{
									updateResultOverlay();
									displayImage.updateAndDraw();
								}
							}
						});

					}
				});

				// key listener to repaint the display image and the traces
				// when using the keys to scroll the stack
				KeyListener keyListener = new KeyListener() {

					@Override
					public void keyTyped(KeyEvent e) {}

					@Override
					public void keyReleased(final KeyEvent e) {
						exec.submit(new Runnable() {
							public void run() 
							{
								System.out.println("Evenement key released="+e.getKeyChar());
								if(e.getKeyChar()=='3' && currentView!=2)toggleHyperView(2);
								if(e.getKeyChar()=='2' && currentView!=1)toggleHyperView(1);
								if(e.getKeyChar()=='1' && currentView!=1)toggleHyperView(0);
								if(e.getKeyChar()=='5' || e.getKeyChar()=='4' || e.getKeyChar()=='6')toggleOverlay();
								if(e.getKeyCode() == KeyEvent.VK_LEFT ||
										e.getKeyCode() == KeyEvent.VK_RIGHT ||
										e.getKeyCode() == KeyEvent.VK_LESS ||
										e.getKeyCode() == KeyEvent.VK_GREATER ||
										e.getKeyCode() == KeyEvent.VK_COMMA ||
										e.getKeyCode() == KeyEvent.VK_PERIOD)
								{
									//IJ.log("moving scroll");
									displayImage.killRoi();
									updateExampleLists();
									drawExamples();
									if(showColorOverlay)
									{
										updateResultOverlay();
										displayImage.updateAndDraw();
									}
								}
							}
						});

					}

					@Override
					public void keyPressed(KeyEvent e) {}
				};
				// add key listener to the window and the canvas
				addKeyListener(keyListener);
				canvas.addKeyListener(keyListener);

			}

			// Labels panel (includes annotations panel)			
			GridBagLayout labelsLayout = new GridBagLayout();
			GridBagConstraints labelsConstraints = new GridBagConstraints();
			labelsJPanel.setLayout( labelsLayout );
			labelsConstraints.anchor = GridBagConstraints.NORTHWEST;
			labelsConstraints.fill = GridBagConstraints.HORIZONTAL;
			labelsConstraints.gridwidth = 1;
			labelsConstraints.gridheight = 1;
			labelsConstraints.gridx = 0;
			labelsConstraints.gridy = 0;
			labelsJPanel.add( annotationsPanel, labelsConstraints );

			// Scroll panel for the label panel
			scrollPanel = new JScrollPane( labelsJPanel );
			scrollPanel.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
			scrollPanel.setMinimumSize( labelsJPanel.getPreferredSize() );


			// Setup and settings panel
			manageJPanel.setBorder(BorderFactory.createTitledBorder("Manage setup and options"));
			GridBagLayout manageLayout = new GridBagLayout();
			GridBagConstraints manageConstraints = new GridBagConstraints();
			manageConstraints.anchor = GridBagConstraints.NORTHWEST;
			manageConstraints.fill = GridBagConstraints.HORIZONTAL;
			manageConstraints.gridwidth = 1;
			manageConstraints.gridheight = 1;
			manageConstraints.gridx = 0;
			manageConstraints.gridy = 0;
			manageConstraints.insets = new Insets(5, 5, 6, 6);
			manageJPanel.setLayout(manageLayout);
			manageJPanel.add(manageColorsButton, manageConstraints);
			manageConstraints.gridy++;
			manageJPanel.add(loadClassSetupButton, manageConstraints);
			manageConstraints.gridy++;
			manageJPanel.add(saveClassSetupButton, manageConstraints);
			manageConstraints.gridy++;
			manageJPanel.add(loadExamplesButton, manageConstraints);
			manageConstraints.gridy++;
			manageJPanel.add(saveExamplesButton, manageConstraints);
			manageConstraints.gridy++;
			manageJPanel.add(addClassButton, manageConstraints);
			manageConstraints.gridy++;
			manageJPanel.add(settingsButton, manageConstraints);
			manageConstraints.gridy++;


			
			
			
			
			
			// Training panel (left side of the GUI)
			trainingJPanel.setBorder(BorderFactory.createTitledBorder("Training"));
			GridBagLayout trainingLayout = new GridBagLayout();
			GridBagConstraints trainingConstraints = new GridBagConstraints();
			trainingConstraints.anchor = GridBagConstraints.NORTHWEST;
			trainingConstraints.fill = GridBagConstraints.HORIZONTAL;
			trainingConstraints.gridwidth = 1;
			trainingConstraints.gridheight = 1;
			trainingConstraints.gridx = 0;
			trainingConstraints.gridy = 0;
			trainingConstraints.insets = new Insets(5, 5, 6, 6);
			trainingJPanel.setLayout(trainingLayout);
			trainingJPanel.add(trainSeparatedButton, trainingConstraints);
			trainingConstraints.gridy++;
			trainingJPanel.add(resultButton, trainingConstraints);
			trainingConstraints.gridy++;
			trainingJPanel.add(probabilityButton, trainingConstraints);
			trainingConstraints.gridy++;
			trainingJPanel.add(runOnOtherButton, trainingConstraints);
			trainingConstraints.gridy++;
			trainingJPanel.add(runTestButton, trainingConstraints);
			trainingConstraints.gridy++;
			//trainingJPanel.add(plotButton, trainingConstraints);
			//trainingConstraints.gridy++;
			//trainingJPanel.add(trainButton, trainingConstraints);
			//trainingConstraints.gridy++;


			// View panel (left side of the GUI)
			viewJPanel.setBorder(BorderFactory.createTitledBorder("View"));
			GridBagLayout viewLayout = new GridBagLayout();
			GridBagConstraints viewConstraints = new GridBagConstraints();
			viewConstraints.anchor = GridBagConstraints.NORTHWEST;
			viewConstraints.fill = GridBagConstraints.HORIZONTAL;
			viewConstraints.gridwidth = 1;
			viewConstraints.gridheight = 1;
			viewConstraints.gridx = 0;
			viewConstraints.gridy = 0;
			viewConstraints.insets = new Insets(5, 5, 6, 6);
			viewJPanel.setLayout(viewLayout);
			viewJPanel.add(overlayButton, viewConstraints);
			viewConstraints.gridy++;
			viewJPanel.add(toggleHyperButton, viewConstraints);
			viewConstraints.gridy++;
			viewJPanel.add(toggleTrainingButton, viewConstraints);
			viewConstraints.gridy++;
			viewJPanel.add(togglePhotoButton, viewConstraints);
			viewConstraints.gridy++;








			// Classifier and data
			optionsJPanel.setBorder(BorderFactory.createTitledBorder("Classifier and data"));
			GridBagLayout optionsLayout = new GridBagLayout();
			GridBagConstraints optionsConstraints = new GridBagConstraints();
			optionsConstraints.anchor = GridBagConstraints.NORTHWEST;
			optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
			optionsConstraints.gridwidth = 1;
			optionsConstraints.gridheight = 1;
			optionsConstraints.gridx = 0;
			optionsConstraints.gridy = 0;
			optionsConstraints.insets = new Insets(5, 5, 6, 6);
			optionsJPanel.setLayout(optionsLayout);

			optionsJPanel.add(applyButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(loadClassifierButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(saveClassifierButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(loadDataButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(saveDataButton, optionsConstraints);
			optionsConstraints.gridy++;
			optionsJPanel.add(wekaButton, optionsConstraints);
			optionsConstraints.gridy++;

			
			
			
			
			// Buttons panel (including training and options)
			GridBagLayout buttonsLayout = new GridBagLayout();
			GridBagConstraints buttonsConstraints = new GridBagConstraints();
			buttonsPanel.setLayout(buttonsLayout);
			buttonsConstraints.anchor = GridBagConstraints.NORTHWEST;
			buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
			buttonsConstraints.gridwidth = 1;
			buttonsConstraints.gridheight = 1;
			buttonsConstraints.gridx = 0;
			buttonsConstraints.gridy = 0;
			buttonsPanel.add(manageJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(trainingJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(viewJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(optionsJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsConstraints.insets = new Insets(5, 5, 6, 6);

			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints allConstraints = new GridBagConstraints();
			all.setLayout(layout);

			allConstraints.anchor = GridBagConstraints.NORTHWEST;
			allConstraints.fill = GridBagConstraints.BOTH;
			allConstraints.gridwidth = 1;
			allConstraints.gridheight = 2;
			allConstraints.gridx = 0;
			allConstraints.gridy = 0;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;

			all.add(buttonsPanel, allConstraints);

			allConstraints.gridx++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			allConstraints.gridheight = 1;
			all.add(canvas, allConstraints);

			allConstraints.gridy++;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			if(null != sliceSelector) {
				all.add(sliceSelector, allConstraints);
				allConstraints.gridy++;
			}			
			if(null != tSelector) {
				all.add(tSelector, allConstraints);
			}
			allConstraints.gridx++;
			allConstraints.gridy--;
			allConstraints.gridy--;
			allConstraints.anchor = GridBagConstraints.NORTHEAST;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			allConstraints.gridheight = 1;
			all.add( scrollPanel, allConstraints );
			
			GridBagLayout wingb = new GridBagLayout();
			GridBagConstraints winc = new GridBagConstraints();
			winc.anchor = GridBagConstraints.NORTHWEST;
			winc.fill = GridBagConstraints.BOTH;
			winc.weightx = 1;
			winc.weighty = 1;
			setLayout(wingb);
			add(all, winc);





			// Fix minimum size to the preferred size at this point
			pack();
			setMinimumSize( getPreferredSize() );
		}


		/**
		 * Get the Weka segmentation object. This tricks allows to 
		 * extract the information from the plugin and use it from
		 * static methods. 
		 * 
		 * @return Weka segmentation data associated to the window.
		 */
		protected WekaSegmentation_Save getWekaSegmentation()
		{
			return wekaSegmentation;
		}


		/**
		 * Get current label lookup table (used to color the results)
		 * @return current overlay LUT
		 */
		public LUT getOverlayLUT()
		{
			return overlayLUT;
		}


		/*

		Bugs :
			Quand je bascule de T1T2RX à T1T2M0, tous les training examples s effacent
			La touche 0 ne marche pas, ça ne fait rien
			Quand je sauve les settings, ça ne sauve pas les filtres utilisés, pire, l'arbre s est retrouve avec des mauvais reglages.
		 */





		/**
		 * Draw the painted traces on the display image
		 */
		protected void drawExamples()
		{
			int currentSlice = (isHyper) ? (displayImage.getCurrentSlice()- (displayImage.getFrame()-1)*trainingImage.getStackSize() ) : displayImage.getCurrentSlice();
			//System.out.println("Cursli="+currentSlice+" , sli="+displayImage.getCurrentSlice()+" t="+displayImage.getFrame());
			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			{
				roiOverlay[i].setColor(colors[i]);
				final ArrayList< Roi > rois = new ArrayList<Roi>();
				for (Roi r : wekaSegmentation.getExamples(i, currentSlice))
				{
					rois.add(r);
					//IJ.log("painted ROI: " + r + " in color "+ colors[i] + ", slice = " + currentSlice);
				}
				roiOverlay[i].setRoi(rois);
			}

			displayImage.updateAndDraw();
		}

		/**
		 * Update the example lists in the GUI
		 */
		protected void updateExampleLists()
		{
			int currentSlice =(isHyper)  ? (displayImage.getCurrentSlice()- (displayImage.getFrame()-1)*trainingImage.getStackSize() ) : displayImage.getCurrentSlice();

			for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			{
				exampleList[i].removeAll();
				for(int j=0; j<wekaSegmentation.getExamples(i, currentSlice).size(); j++)
					exampleList[i].add("trace " + j + " (Z=" + currentSlice+")");
			}

		}

		protected boolean isToogleEnabled()
		{
			return showColorOverlay;
		}

		/**
		 * Get the displayed image. This method can be used to
		 * extract the ROIs from the current image.
		 * 
		 * @return image being displayed in the custom window
		 */
		protected ImagePlus getDisplayImage()
		{
			return this.getImagePlus();
		}

		/**
		 * Set the slice selector enable option
		 * @param b true/false to enable/disable the slice selector
		 */
		public void setSliceSelectorEnabled(boolean b)
		{
			if(null != tSelector)
				tSelector.setEnabled(b);
			if(null != sliceSelector)
				sliceSelector.setEnabled(b);
		}

		/**
		 * Repaint all panels
		 */
		public void repaintAll()
		{
			this.annotationsPanel.repaint();
			getCanvas().repaint();
			this.buttonsPanel.repaint();
			this.all.repaint();
		}

		/**
		 * Add new segmentation class (new label and new list on the right side)
		 */
		public void addClass()
		{
			int classNum = numOfClasses;

			exampleList[classNum] = new java.awt.List(5);
			exampleList[classNum].setForeground(colors[classNum]);

			exampleList[classNum].addActionListener(listener);
			exampleList[classNum].addItemListener(itemListener);
			addExampleButton[classNum] = new JButton("Add to " + wekaSegmentation.getClassLabel(classNum));

			annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
			annotationsConstraints.insets = new Insets(5, 5, 6, 6);

			boxAnnotation.setConstraints(addExampleButton[classNum], annotationsConstraints);
			annotationsPanel.add(addExampleButton[classNum]);
			annotationsConstraints.gridy++;

			annotationsConstraints.insets = new Insets(0,0,0,0);

			boxAnnotation.setConstraints(exampleList[classNum], annotationsConstraints);
			annotationsPanel.add(exampleList[classNum]);
			annotationsConstraints.gridy++;

			// Add listener to the new button
			addExampleButton[classNum].addActionListener(listener);

			numOfClasses++;

			// recalculate minimum size of scroll panel
			scrollPanel.setMinimumSize( labelsJPanel.getPreferredSize() );

			repaintAll();
		}

		/**
		 * Set the image being displayed on the custom canvas
		 * @param imp new image
		 */
		public void setImagePlus(final ImagePlus imp)
		{
			super.imp = imp;
			((CustomCanvas) super.getCanvas()).setImagePlus(imp);
			Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
			((CustomCanvas) super.getCanvas()).setDstDimensions(dim.width, dim.height);
			imp.setWindow(this);
			repaint();
		}

		/**
		 * Enable / disable buttons
		 * @param s enabling flag
		 */
		protected void setButtonsEnabled(boolean s)
		{


			runTestButton.setEnabled(s);
			runOnOtherButton.setEnabled(s);
			saveExamplesButton.setEnabled(s);
			loadExamplesButton.setEnabled(s);
			toggleHyperButton.setEnabled(s);
			toggleTrainingButton.setEnabled(s);
			togglePhotoButton.setEnabled(s);			




			debugButton.setEnabled(true);
			manageColorsButton.setEnabled(s);
			loadClassSetupButton.setEnabled(s);
			saveClassSetupButton.setEnabled(s);

			//trainButton.setEnabled(s);
			trainSeparatedButton.setEnabled(s);
			overlayButton.setEnabled(s);
			resultButton.setEnabled(s);
			probabilityButton.setEnabled(s);
			plotButton.setEnabled(s);
			applyButton.setEnabled(s);
			loadClassifierButton.setEnabled(s);
			saveClassifierButton.setEnabled(s);
			loadDataButton.setEnabled(s);
			saveDataButton.setEnabled(s);
			addClassButton.setEnabled(s);
			settingsButton.setEnabled(s);
			wekaButton.setEnabled(s);
			for(int i = 0 ; i < wekaSegmentation.getNumOfClasses(); i++)
			{
				exampleList[i].setEnabled(s);
				addExampleButton[i].setEnabled(s);
			}
			setSliceSelectorEnabled(s);
			//trainButton.setEnabled(false);
		}

		/**
		 * Update buttons enabling depending on the current status of the plugin
		 */
		protected void updateButtonsEnabling()
		{
			// While training, set disable all buttons except the train buttons, 
			// which will be used to stop the training by the user. 
			if( trainingFlag )
			{
				setButtonsEnabled( false );
				//trainButton.setEnabled( badTrainerWasLastUsed );	
				trainSeparatedButton.setEnabled( goodTrainerWasLastUsedAndParamsHadNotMove );	
			}
			else // If the training is not going on
			{
				runTestButton.setEnabled(true);
				runOnOtherButton.setEnabled(true);
				manageColorsButton.setEnabled( true );	
				loadClassSetupButton.setEnabled( true );	
				saveClassSetupButton.setEnabled( true );	
				final boolean classifierExists =  null != wekaSegmentation.getClassifier();

				//trainButton.setEnabled( classifierExists );
				trainSeparatedButton.setEnabled( classifierExists );
				applyButton.setEnabled( win.trainingComplete );

				final boolean resultExists = null != classifiedImage &&
						null != classifiedImage.getProcessor();

				saveClassifierButton.setEnabled( win.trainingComplete );
				overlayButton.setEnabled(resultExists);
				resultButton.setEnabled( win.trainingComplete );
				plotButton.setEnabled( win.trainingComplete );				
				probabilityButton.setEnabled( win.trainingComplete );

				loadClassifierButton.setEnabled(true);
				loadDataButton.setEnabled(true);

				addClassButton.setEnabled(wekaSegmentation.getNumOfClasses() < WekaSegmentation_Save.MAX_NUM_CLASSES);
				settingsButton.setEnabled(true);
				wekaButton.setEnabled(true);

				// Check if there are samples in any slice
				boolean examplesEmpty = true;
				for( int n = 1; n <= trainingImage.getImageStackSize(); n++ )
					for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i ++)
						if( wekaSegmentation.getExamples( i, n ).size() > 0)
						{
							examplesEmpty = false;
							break;
						}
				boolean loadedTrainingData = null != wekaSegmentation.getLoadedTrainingData();
				saveExamplesButton.setEnabled(!examplesEmpty);
				loadExamplesButton.setEnabled(true);
				toggleHyperButton.setEnabled(isHyper && currentView!= 0);
				toggleTrainingButton.setEnabled(isHyper && currentView!=1);
				togglePhotoButton.setEnabled(isHyper && currentView !=2);			

				saveDataButton.setEnabled(!examplesEmpty || loadedTrainingData);

				for(int i = 0 ; i < wekaSegmentation.getNumOfClasses(); i++)
				{
					exampleList[i].setEnabled(true);
					addExampleButton[i].setEnabled(true);
				}
				setSliceSelectorEnabled(true);
			}
			debugButton.setEnabled(true);
			//trainButton.setEnabled(false);
		}

		/**
		 * Toggle between overlay and original image with markings
		 */
		public void toggleOverlay()		{
			showColorOverlay = !showColorOverlay;
			if (showColorOverlay && null != classifiedImage)updateResultOverlay();
			else resultOverlay.setImage(null);
			displayImage.updateAndDraw();
		}

		/**
		 * Set a new result (classified) image
		 * @param classifiedImage new result image
		 */
		protected void setClassfiedImage(ImagePlus classifiedImage) 
		{
			updateClassifiedImage(classifiedImage);	
		}

		/**
		 * Update the buttons to add classes with current information
		 */
		public void updateAddClassButtons() 
		{
			int wekaNumOfClasses = wekaSegmentation.getNumOfClasses();
			while (numOfClasses < wekaNumOfClasses)
				win.addClass();
			for (int i = 0; i < numOfClasses; i++)
				addExampleButton[i].setText("Add to " + wekaSegmentation.getClassLabel(i));

			win.updateButtonsEnabling();
			repaintWindow();
		}

		/**
		 * Set the flag to inform the the training has finished or not
		 * 
		 * @param b tranining complete flag
		 */
		void setTrainingComplete(boolean b)
		{
			this.trainingComplete = b;
		}

		/**
		 * Get training image
		 * @return training image
		 */
		public ImagePlus getTrainingImage()
		{
			return trainingImage;
		}
	}// end class CustomWindow

	/**
	 * Plugin run method
	 */
	public void run(String arg)
	{
		// check if the image should be process in 3D
		if( arg.equals( "3D" ) )
			isProcessing3D = true;

		// instantiate segmentation backend
		wekaSegmentation = new WekaSegmentation_Save( isProcessing3D ,this);
		FastRandomForest rf = new FastRandomForest();
		rf.setNumTrees(200);
		rf.setNumFeatures(14);
		rf.setSeed( (new Random()).nextInt() );
		rf.setNumThreads( Prefs.getThreads() );
		wekaSegmentation.setClassifier(rf);




		System.out.println("Starting weka. 3D mode="+isProcessing3D);
		for(int i = 0; i < wekaSegmentation.getNumOfClasses() ; i++)
		{
			exampleList[i] = new java.awt.List(5);
			exampleList[i].setForeground(colors[i]);
		}
		numOfClasses = wekaSegmentation.getNumOfClasses();

		//get current image
		if (null == WindowManager.getCurrentImage())
		{
			trainingImage = IJ.openImage();
			if (null == trainingImage) return; // user canceled open dialog
		}
		else
		{
			trainingImage = WindowManager.getCurrentImage();
			// hide input image (to avoid accidental closing)
			trainingImage.getWindow().setVisible( false );
		}


		if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
			IJ.log("Warning: at least one dimension of the image "  +
					"is larger than 1024 pixels.\n" +
					"Feature stack creation and classifier training " +
					"might take some time depending on your computer.\n");

		wekaSegmentation.setTrainingImage(trainingImage);

		// The display image is a copy of the training image (single image or stack)
		displayImage = trainingImage.duplicate();
		displayImage.setSlice( trainingImage.getCurrentSlice() );
		displayImage.setTitle( Weka_Save.PLUGIN_NAME + " " + Weka_Save.PLUGIN_VERSION );

		ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.FREELINE);

		//Build GUI
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						win = new CustomWindow(displayImage);
						win.pack();

					}
				});
		if(false) {
			wekaSegmentation.setEnabledFeatures(new boolean[]{
					true, 	/* Gaussian_blur */
					false, 	/* Hessian */
					false, 	/* Derivatives */
					false, 	/* Laplacian */
					false,	/* Structure */
					false,	/* Edges */
					false,	/* Difference of Gaussian */
					false,	/* Minimum */
					false,	/* Maximum */
					true,	/* Mean */
					false,	/* Median */
					true	/* Variance */
			});
		}
	}

	/**
	 * Add examples defined by the user to the corresponding list
	 * in the GUI and the example list in the segmentation object.
	 * 
	 * @param i GUI list index
	 */
	private void addExamples(int i)
	{
		//get selected pixels
		final Roi r = displayImage.getRoi();
		if (null == r)
			return;

		// IJ.log("Adding trace to list " + i);

		final int n = (isHyper)  ? (displayImage.getCurrentSlice()- (displayImage.getFrame()-1)*trainingImage.getStackSize() ) : displayImage.getCurrentSlice();
		displayImage.killRoi();
		wekaSegmentation.addExample(i, r, n);
		traceCounter[i]++;
		win.drawExamples();
		win.updateExampleLists();
		// Record
		String[] arg = new String[] {
				Integer.toString(i), 
				Integer.toString(n)	};
		record(ADD_TRACE, arg);
	}


	/**
	 * Update the result image
	 * 
	 * @param classifiedImage new result image
	 */
	public void updateClassifiedImage(ImagePlus classifiedImage)
	{
		this.classifiedImage = classifiedImage;
	}

	/**
	 * Update the result image overlay with the corresponding slice
	 */
	public void updateResultOverlay()
	{

		int currentSlice = (isHyper)  ? (displayImage.getCurrentSlice()- (displayImage.getFrame()-1)*trainingImage.getStackSize() ) : displayImage.getCurrentSlice();

		ImageProcessor overlay = classifiedImage.getImageStack().getProcessor(currentSlice).duplicate();
		overlay = overlay.convertToByte(false);
		overlay.setColorModel(overlayLUT);
		win.resultOverlay.setImage(overlay);
	}

	/**
	 * Select a list and deselect the others
	 * 
	 * @param e item event (originated by a list)
	 * @param i list index
	 */
	void listSelected(final ItemEvent e, final int i)
	{
		// find the right slice of the corresponding ROI

		win.drawExamples();
		displayImage.setColor(Color.YELLOW);

		for(int j = 0; j < wekaSegmentation.getNumOfClasses(); j++)
		{
			if (j == i)
			{
				int currentSlice = (isHyper)  ? (displayImage.getCurrentSlice()- (displayImage.getFrame()-1)*trainingImage.getStackSize() ) : displayImage.getCurrentSlice();

				final Roi newRoi = 
						wekaSegmentation.getExamples(i, currentSlice)
						.get(exampleList[i].getSelectedIndex());
				// Set selected trace as current ROI
				newRoi.setImage(displayImage);
				displayImage.setRoi(newRoi);
			}
			else
				exampleList[j].deselect(exampleList[j].getSelectedIndex());
		}

		displayImage.updateAndDraw();
	}

	/**
	 * Delete one of the ROIs
	 *
	 * @param e action event
	 */
	void deleteSelected(final ActionEvent e)
	{
		System.out.println("Entree dans delete selected");
		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
			if (e.getSource() == exampleList[i])
			{
				//delete item from ROI
				int index = exampleList[i].getSelectedIndex();
				int currentSlice = (isHyper)  ? (displayImage.getCurrentSlice()- (displayImage.getFrame()-1)*trainingImage.getStackSize() ) : displayImage.getCurrentSlice();
				// kill Roi from displayed image

				if(displayImage.getRoi().equals( 
						wekaSegmentation.getExamples(i, currentSlice).get(index) ))
					displayImage.killRoi();

				// delete item from the list of ROIs of that class and slice
				wekaSegmentation.deleteExample(i, currentSlice, index);
				//delete item from GUI list
				exampleList[i].remove(index);
			}

		win.drawExamples();
		win.updateExampleLists();
	}

	/**
	 * Run/stop the classifier training
	 * 
	 * @param command current text of the training button ("Train classifier" or "STOP")
	 */
	@SuppressWarnings("deprecation")
	void runStopTraining(final String command) 
	{
		wekaSegmentation.setUpdateFeatures(true);
		badTrainerWasLastUsed=true;
		goodTrainerWasLastUsedAndParamsHadNotMove=false;
		System.out.println("Call arrive bad");
		// If the training is not going on, we start it
		if (command.equals("Standard training")) 
		{				
			trainingFlag = true;
			//trainButton.setText("STOP");
			final Thread oldTask = trainingTask;
			// Disable rest of buttons until the training has finished
			win.updateButtonsEnabling();
			System.out.println("DEBUG : training size="+trainingImage.getStackSize());
			// Set train button text to STOP
			//trainButton.setText("STOP");							

			// Thread to run the training
			Thread newTask = new Thread() {								 

				public void run()
				{
					// Wait for the old task to finish
					if (null != oldTask) 
					{
						try { 
							IJ.log("Waiting for old task to finish...");
							oldTask.join(); 
						} 
						catch (InterruptedException ie)	{ /*IJ.log("interrupted");*/ }
					}

					try{
						// Macro recording
						String[] arg = new String[] {};
						record(TRAIN_CLASSIFIER, arg);

						if( wekaSegmentation.trainClassifier() )
						{
							if( this.isInterrupted() )
							{
								//IJ.log("Training was interrupted by the user.");
								wekaSegmentation.shutDownNow();
								win.trainingComplete = false;
								return;
							}
							wekaSegmentation.applyClassifier(false);
							classifiedImage = wekaSegmentation.getClassifiedImage();
							if(showColorOverlay)
								win.toggleOverlay();
							win.toggleOverlay();
							win.trainingComplete = true;
						}
						else
						{
							IJ.log("The traning did not finish.");
							win.trainingComplete = false;
						}

					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					catch( OutOfMemoryError err )
					{
						err.printStackTrace();
						IJ.log( "ERROR: plugin run out of memory. Please, "
								+ "use a smaller input image or fewer features." );
					}
					finally
					{
						trainingFlag = false;						
						//trainButton.setText("Standard training");
						win.updateButtonsEnabling();										
						trainingTask = null;
					}
				}

			};

			//IJ.log("*** Set task to new TASK (" + newTask + ") ***");
			trainingTask = newTask;
			newTask.start();							
		}
		else if (command.equals("STOP")) 							  
		{
			try{
				trainingFlag = false;
				win.trainingComplete = false;
				IJ.log("Training was stopped by the user!");
				win.setButtonsEnabled( false );
				//trainButton.setText("Standard training");

				if(null != trainingTask)
				{
					trainingTask.interrupt();
					// Although not recommended and already deprecated,
					// use stop command so WEKA classifiers are actually
					// stopped.
					trainingTask.stop();
				}
				else
					IJ.log("Error: interrupting training failed becaused the thread is null!");

				wekaSegmentation.shutDownNow();
				win.updateButtonsEnabling();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}



	/**
	 * Run/stop the classifier training
	 * 
	 * @param command current text of the training button ("Train classifier" or "STOP")
	 */
	@SuppressWarnings("deprecation")
	void runStopTrainingRGBSeparated(final String command) 
	{
		if (command.equals("Hyperimage training")) 
		{				

			System.out.println("DEBUG : training separated. Size (RGB)="+trainingImage.getStackSize());
			final Thread oldTask = trainingTask;
			// Disable rest of buttons until the training has finished

			// Thread to run the training
			Thread newTask = new Thread() {								 

				public void run()
				{
					// Wait for the old task to finish
					if (null != oldTask) {
						try { IJ.log("Waiting for old task to finish...");oldTask.join(); 	} 
						catch (InterruptedException ie)	{ /*IJ.log("interrupted");*/ }
					}
					trainingFlag = true;
					trainSeparatedButton.setText("STOP");
					win.updateButtonsEnabling();

					try{
						//Actualize the featureStack
						if(!goodTrainerWasLastUsedAndParamsHadNotMove) {
							int nbCan=0;
							for(int c=0;c<tabHyperModalities.length;c++)if(tabHyperModalities[c])nbCan++;
							ImagePlus []trainTab=new ImagePlus[nbCan];nbCan=0;
							for(int c=0;c<tabHyperModalities.length;c++)if(tabHyperModalities[c]) {trainTab[nbCan]=VitimageUtils.imageCopy(hyperTab[c+1]);nbCan++;}
							FeatureStackArray fsa=null;
							if(isProcessing3D){
								fsa=buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(trainTab,tabHyperFeatures, (int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()));
							}
							else {
								fsa=buildFeatureStackArrayRGBSeparatedMultiThreadedV2(trainTab,tabHyperFeatures,(int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()));
							}
							goodTrainerWasLastUsedAndParamsHadNotMove=true;
							badTrainerWasLastUsed=false;
							wekaSegmentation.setFeatureStackArray(fsa);
							wekaSegmentation.setUpdateFeatures(false);

						}

						// Set train button text to STOP
						trainSeparatedButton.setText("STOP");							
						computingImportance=true;
						((FastRandomForest)(wekaSegmentation.getClassifier())).setComputeImportances(computingImportance);
						if( wekaSegmentation.trainClassifier() ){
							System.out.println("HereHereHere 9");
							if( this.isInterrupted() ){
								wekaSegmentation.shutDownNow();
								win.trainingComplete = false;
								return;
							}
							wekaSegmentation.applyClassifier(false);//False means no probability maps
							classifiedImage = wekaSegmentation.getClassifiedImage();
							if(showColorOverlay)
								win.toggleOverlay();
							win.toggleOverlay();
							win.trainingComplete = true;
						}
						else
						{
							IJ.log("The traning did not finish.");
							win.trainingComplete = false;
						}
						printImportances();
						computingImportance=false;
						((FastRandomForest)(wekaSegmentation.getClassifier())).setComputeImportances(computingImportance);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					catch( OutOfMemoryError err )
					{
						err.printStackTrace();
						IJ.log( "ERROR: plugin run out of memory. Please, "
								+ "use a smaller input image or fewer features." );
					}
					finally
					{
						trainingFlag = false;						
						trainSeparatedButton.setText("Hyperimage training");
						win.updateButtonsEnabling();										
						trainingTask = null;

					}
				}

			};

			//IJ.log("*** Set task to new TASK (" + newTask + ") ***");
			trainingTask = newTask;
			newTask.start();							
		}
		else if (command.equals("STOP")) 							  
		{
			try{
				trainingFlag = false;
				win.trainingComplete = false;
				IJ.log("Training was stopped by the user!");
				win.setButtonsEnabled( false );
				trainSeparatedButton.setText("Hyperimage training");

				if(null != trainingTask)
				{
					trainingTask.interrupt();
					// Although not recommended and already deprecated,
					// use stop command so WEKA classifiers are actually
					// stopped.
					trainingTask.stop();
				}
				else
					IJ.log("Error: interrupting training failed becaused the thread is null!");

				wekaSegmentation.shutDownNow();
				win.updateButtonsEnabling();
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}








	/**
	 * Display the whole image after classification
	 */
	void showClassificationImage()
	{
		if( null == classifiedImage )
		{
			// if not result image is there yet, calculate it
			win.setButtonsEnabled( false );
			wekaSegmentation.applyClassifier( false );
			classifiedImage = wekaSegmentation.getClassifiedImage();
			win.updateButtonsEnabling();
		}
		final ImagePlus resultImage = classifiedImage.duplicate();

		resultImage.setTitle( "Classified image" );

		convertTo8bitNoScaling( resultImage );

		resultImage.getProcessor().setColorModel( overlayLUT );
		resultImage.getImageStack().setColorModel( overlayLUT );
		resultImage.updateAndDraw();

		resultImage.show();
	}

	/**
	 * Convert image to 8 bit in place without scaling it
	 * 
	 * @param image input image
	 */
	static void convertTo8bitNoScaling( ImagePlus image )
	{
		boolean aux = ImageConverter.getDoScaling();

		ImageConverter.setDoScaling( false );

		if( image.getImageStackSize() > 1)			
			(new StackConverter( image )).convertToGray8();
		else
			(new ImageConverter( image )).convertToGray8();

		ImageConverter.setDoScaling( aux );
	}

	/**
	 * Display the current probability maps
	 */
	void showProbabilityImage()
	{
		IJ.showStatus("Calculating probability maps...");
		IJ.log("Calculating probability maps...");
		win.setButtonsEnabled(false);
		try{		
			wekaSegmentation.applyClassifier(true);
		}catch(Exception ex){
			IJ.log("Error while applying classifier! (please send bug report)");
			ex.printStackTrace(); 
			win.updateButtonsEnabling();
			return;
		}
		final ImagePlus probImage = wekaSegmentation.getClassifiedImage();
		if(null != probImage)
		{
			probImage.setDimensions( numOfClasses, displayImage.getNSlices(),
					displayImage.getNFrames());
			if ( displayImage.getNSlices() * displayImage.getNFrames() > 1 )
				probImage.setOpenAsHyperStack( true );
			probImage.show();
		}
		win.updateButtonsEnabling();
		IJ.showStatus("Done.");
		IJ.log("Done");
	}

	/**
	 * Plot the current result
	 */
	void plotResult()
	{
		IJ.showStatus("Evaluating current data...");
		IJ.log("Evaluating current data...");
		win.setButtonsEnabled(false);
		final Instances data;
		if (wekaSegmentation.getTraceTrainingData() != null)
			data = wekaSegmentation.getTraceTrainingData();
		else
			data = wekaSegmentation.getLoadedTrainingData();

		if(null == data)
		{
			IJ.error( "Error in plot result", 
					"No data available yet to plot results: you need to trace\n"
							+ "some training samples or load data from file." );
			win.updateButtonsEnabling();
			return;
		}

		displayGraphs(data, wekaSegmentation.getClassifier());
		win.updateButtonsEnabling();
		IJ.showStatus("Done.");
		IJ.log("Done");
	}

	/**
	 * Display the threshold curve window (for precision/recall, ROC, etc.).
	 *
	 * @param data input instances
	 * @param classifier classifier to evaluate
	 */
	public static void displayGraphs(Instances data, AbstractClassifier classifier)
	{
		ThresholdCurve tc = new ThresholdCurve();

		ArrayList<Prediction> predictions = null;
		try {
			final EvaluationUtils eu = new EvaluationUtils();
			predictions = eu.getTestPredictions(classifier, data);
		} catch (Exception e) {
			IJ.log("Error while evaluating data!");
			e.printStackTrace();
			return;
		}

		Instances result = tc.getCurve(predictions);
		ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
		vmc.setName(result.relationName() + " (display only)");
		PlotData2D tempd = new PlotData2D(result);
		tempd.setPlotName(result.relationName());
		tempd.addInstanceNumberAttribute();
		try {
			vmc.addPlot(tempd);
		} catch (Exception e) {
			IJ.log("Error while adding plot to visualization panel!");
			e.printStackTrace();
			return;
		}
		String plotName = vmc.getName();
		JFrame jf = new JFrame("Weka Classifier Visualize: "+plotName);
		jf.setSize(500,400);
		jf.getContentPane().setLayout(new BorderLayout());
		jf.getContentPane().add(vmc, BorderLayout.CENTER);
		jf.setVisible(true);
	}

	/**
	 * Apply classifier to test data. As it is implemented right now, 
	 * it will use one thread per input image and slice. 
	 */
	public void applyClassifierToTestData()
	{
		// array of files to process
		File[] imageFiles;
		String storeDir = "";

		// create a file chooser for the image files
		String dir = OpenDialog.getLastDirectory();
		if (null == dir)
			dir = OpenDialog.getDefaultDirectory();
		if( Prefs.useFileChooser )
		{
			JFileChooser fileChooser = new JFileChooser( dir );
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setMultiSelectionEnabled(true);
			fileChooser.setDialogTitle( "Select file(s) to classify" );

			// get selected files or abort if no file has been selected
			int returnVal = fileChooser.showOpenDialog(null);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				imageFiles = fileChooser.getSelectedFiles();
				OpenDialog.setLastDirectory( imageFiles[ 0 ].getParent() );
			} else {
				return;
			}
		}
		else // use FileDialog
		{
			final Frame parent = IJ.getInstance();
			final FileDialog fd = new FileDialog( parent,
					"Select file(s) to classify", FileDialog.LOAD );
			fd.setDirectory( dir );
			fd.setMultipleMode( true );
			// files only
			fd.setFilenameFilter( new FilenameFilter(){
				public boolean accept( File dir, String name )
				{
					final File f = new File( dir + File.separator + name );
					if( f.exists() && !f.isDirectory() )
						return true;
					else
						return false;
				}
			});
			// get selected files or abort if no file has been selected
			fd.setVisible( true );
			imageFiles = fd.getFiles();
			if( null == imageFiles || imageFiles.length == 0 )
				return;
			else
				OpenDialog.setLastDirectory( imageFiles[ 0 ].getParent() );
		}

		boolean showResults = true;
		boolean storeResults = false;

		if (imageFiles.length >= 3) {

			int decision = JOptionPane.showConfirmDialog(null, "You decided to process three or more image " +
					"files. Do you want the results to be stored on the disk instead of opening them in Fiji?",
					"Save results?", JOptionPane.YES_NO_OPTION);

			if (decision == JOptionPane.YES_OPTION) {
				final DirectoryChooser dc = new DirectoryChooser(
						"Select folder to store results");
				DirectoryChooser.setDefaultDirectory( dir );
				storeDir = dc.getDirectory();
				if( null == storeDir )
					return;

				showResults  = false;
				storeResults = true;
			}
		}

		final boolean probabilityMaps;

		int decision = JOptionPane.showConfirmDialog(null, "Create probability maps instead of segmentation?", "Probability maps?", JOptionPane.YES_NO_OPTION);
		if (decision == JOptionPane.YES_OPTION)
			probabilityMaps = true;
		else
			probabilityMaps = false;

		final int numProcessors     = Prefs.getThreads();
		final int numThreads        = Math.min(imageFiles.length, numProcessors);
		final int numFurtherThreads = (int)Math.ceil((double)(numProcessors - numThreads)/imageFiles.length) + 1;

		IJ.log("Processing " + imageFiles.length + " image file(s) in " + numThreads + " thread(s)....");

		win.setButtonsEnabled(false);

		Thread[] threads = new Thread[numThreads];

		class ImageProcessingThread extends Thread {

			private final int     numThread;
			private final int     numThreads;
			private final File[]  imageFiles;
			private final boolean storeResults;
			private final boolean showResults;
			private final String  storeDir;

			public ImageProcessingThread(int numThread, int numThreads,
					File[] imageFiles,
					boolean storeResults, boolean showResults,
					String storeDir) {
				this.numThread     = numThread;
				this.numThreads    = numThreads;
				this.imageFiles    = imageFiles;
				this.storeResults  = storeResults;
				this.showResults   = showResults;
				this.storeDir      = storeDir;
			}

			public void run() {

				for (int i = numThread; i < imageFiles.length; i += numThreads) {
					File file = imageFiles[i];

					ImagePlus testImage = IJ.openImage(file.getPath());

					if( null == testImage )
					{
						IJ.log( "Error: " + file.getPath() + " is not a valid image file.");
						IJ.error("Trainable Weka Segmentation I/O error", "Error: " + file.getPath() + " is not a valid image file.");
						return;						
					}
					if( testImage.getNSlices() == 1 && isProcessing3D )
					{
						IJ.log( "Error: " + file.getPath() + " is a 2D image but Trainable Weka Segmentation "
								+ "is working in 3D." );
						IJ.error( "Wrong image dimensions: " + file.getPath() + " is a 2D image but "
								+ "Trainable Weka Segmentation is working in 3D." );
						return;
					}

					IJ.log("Processing image " + file.getName() + " in thread " + numThread);

					ImagePlus segmentation =
							wekaSegmentation.applyClassifier( testImage,
									numFurtherThreads, probabilityMaps );
					if( null == segmentation )
					{
						IJ.log( "Error: " + file.getName() + "could not be "
								+ "classified!" );
						return;
					}
					if ( !probabilityMaps )
					{
						// convert slices to 8-bit and apply overlay LUT
						convertTo8bitNoScaling( segmentation );
						segmentation.getProcessor().setColorModel( overlayLUT );
						segmentation.getImageStack().setColorModel( overlayLUT );
						segmentation.updateAndDraw();
					}

					if ( showResults )
					{
						segmentation.show();
						testImage.show();
					}
					else if ( storeResults ) {
						String filename = storeDir + File.separator + file.getName();
						IJ.log("Saving results to " + filename);
						IJ.save(segmentation, filename);
						segmentation.close();
						testImage.close();
						// force garbage collection
						segmentation = null;
						testImage = null;
						System.gc();
					}
				}
			}
		}

		// start threads
		for (int i = 0; i < numThreads; i++) {

			threads[i] = new ImageProcessingThread(i, numThreads, imageFiles, storeResults, showResults, storeDir);
			// Record
			String[] arg = new String[] {
					imageFiles[i].getParent(),
					imageFiles[i].getName(),
					"showResults=" + showResults,
					"storeResults=" + storeResults,
					"probabilityMaps="+ probabilityMaps,
					storeDir	};
			record(APPLY_CLASSIFIER, arg);
			threads[i].start();
		}

		// join all threads
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {}
		}

		win.updateButtonsEnabling();
	}
	/**
	 * Load a Weka classifier from a file
	 */

	public void loadClassifierWithoutOtherActions(String filePath) {
		String resultPath="";
		if(filePath==null) {
			OpenDialog od = new OpenDialog( "Choose Weka classifier file", "" );
			if (od.getFileName()==null)
				return;
			IJ.log("Loading Weka classifier from " + od.getDirectory() + od.getFileName() + "...");
			// Record
			String[] arg = new String[] { od.getDirectory() + od.getFileName() };
			resultPath=od.getDirectory() + od.getFileName();
			record(LOAD_CLASSIFIER, arg);
		}
		else resultPath=filePath;
			
		wekaSegmentation.setUpdateFeatures(true);

		win.setButtonsEnabled(false);

		final AbstractClassifier oldClassifier = wekaSegmentation.getClassifier();


		// Try to load Weka model (classifier and train header)
		if(  !wekaSegmentation.loadClassifier(resultPath) )
		{
			IJ.error("Error when loading Weka classifier from file");
			IJ.log("Error: classifier could not be loaded.");
			win.updateButtonsEnabling();
			return;
		}
	}
	
	
	public void loadClassifier()
	{
		OpenDialog od = new OpenDialog( "Choose Weka classifier file", "" );
		if (od.getFileName()==null)
			return;
		IJ.log("Loading Weka classifier from " + od.getDirectory() + od.getFileName() + "...");
		// Record
		String[] arg = new String[] { od.getDirectory() + od.getFileName() };
		
		wekaSegmentation.setUpdateFeatures(true);
		record(LOAD_CLASSIFIER, arg);

		win.setButtonsEnabled(false);

		final AbstractClassifier oldClassifier = wekaSegmentation.getClassifier();


		// Try to load Weka model (classifier and train header)
		if(  !wekaSegmentation.loadClassifier(od.getDirectory() + od.getFileName()) )
		{
			IJ.error("Error when loading Weka classifier from file");
			IJ.log("Error: classifier could not be loaded.");
			win.updateButtonsEnabling();
			return;
		}
		if(  !wekaSegmentation.loadClassifier(od.getDirectory() + od.getFileName()) )
		{
			IJ.error("Error when loading Weka classifier from file");
			IJ.log("Error: classifier could not be loaded.");
			win.updateButtonsEnabling();
			return;
		}

		IJ.log("Read header from " + od.getDirectory() + od.getFileName() + " (number of attributes = " + wekaSegmentation.getTrainHeader().numAttributes() + ")");

		if(wekaSegmentation.getTrainHeader().numAttributes() < 1)
		{
			IJ.error("Error", "No attributes were found on the model header");
			wekaSegmentation.setClassifier(oldClassifier);
			win.updateButtonsEnabling();
			return;
		}
		// Set the flag of training complete to true
		wekaSegmentation.applyClassifier(false);//False means no probability maps
		classifiedImage = wekaSegmentation.getClassifiedImage();
		if(showColorOverlay)
			win.toggleOverlay();
		win.toggleOverlay();
		win.trainingComplete = true;


		win.trainingComplete = true;

		// update GUI
		win.updateAddClassButtons();

		IJ.log("Loaded " + od.getDirectory() + od.getFileName());
	}

	/**
	 * Load a Weka model (classifier) from a file
	 * @param filename complete path and file name
	 * @return classifier
	 */
	public static AbstractClassifier readClassifier(String filename)
	{
		AbstractClassifier cls = null;
		// deserialize model
		try {
			cls = (AbstractClassifier) SerializationHelper.read(filename);
		} catch (Exception e) {
			IJ.log("Error when loading classifier from " + filename);
			e.printStackTrace();
		}
		return cls;
	}

	/**
	 * Save current classifier into a file
	 */
	public void saveClassifier()
	{
		SaveDialog sd = new SaveDialog("Save model as...", "classifier",".model");
		if (sd.getFileName()==null)
			return;

		// Record
		String[] arg = new String[] { sd.getDirectory() + sd.getFileName() };
		record(SAVE_CLASSIFIER, arg);

		if( !wekaSegmentation.saveClassifier(sd.getDirectory() + sd.getFileName()) )
		{
			IJ.error("Error while writing classifier into a file");
			return;
		}
	}

	/**
	 * Write classifier into a file
	 *
	 * @param classifier classifier
	 * @param trainHeader train header containing attribute and class information
	 * @param filename name (with complete path) of the destination file
	 * @return false if error
	 */
	public static boolean saveClassifier(
			AbstractClassifier classifier,
			Instances trainHeader,
			String filename)
	{
		File sFile = null;
		boolean saveOK = true;


		IJ.log("Saving model to file...");

		try {
			sFile = new File(filename);
			OutputStream os = new FileOutputStream(sFile);
			if (sFile.getName().endsWith(".gz"))
			{
				os = new GZIPOutputStream(os);
			}
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
			objectOutputStream.writeObject(classifier);
			if (trainHeader != null)
				objectOutputStream.writeObject(trainHeader);
			objectOutputStream.flush();
			objectOutputStream.close();
		}
		catch (Exception e)
		{
			IJ.error("Save Failed", "Error when saving classifier into a file");
			saveOK = false;
			e.printStackTrace();
		}
		if (saveOK)
			IJ.log("Saved model into the file " + filename);

		return saveOK;
	}

	/**
	 * Write classifier into a file
	 *
	 * @param cls classifier
	 * @param filename name (with complete path) of the destination file
	 * @return false if error
	 */
	public static boolean writeClassifier(AbstractClassifier cls, String filename)
	{
		try {
			SerializationHelper.write(filename, cls);
		} catch (Exception e) {
			IJ.log("Error while writing classifier into a file");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Load previously saved data
	 */
	public void loadTrainingData()
	{
		OpenDialog od = new OpenDialog("Choose data file", OpenDialog.getLastDirectory(), "data.arff");
		if (od.getFileName()==null)
			return;

		// Macro recording
		String[] arg = new String[] { od.getDirectory() + od.getFileName() };
		record(LOAD_DATA, arg);

		win.setButtonsEnabled(false);
		IJ.log("Loading data from " + od.getDirectory() + od.getFileName() + "...");
		wekaSegmentation.loadTrainingData(od.getDirectory() + od.getFileName());
		win.updateButtonsEnabling();
	}

	/**
	 * Save training model into a file
	 */
	public void saveTrainingData()
	{
		SaveDialog sd = new SaveDialog("Choose save file", "data",".arff");
		if (sd.getFileName()==null)
			return;

		// Macro recording
		String[] arg = new String[] { sd.getDirectory() + sd.getFileName() };
		record(SAVE_DATA, arg);

		win.setButtonsEnabled(false);

		if( !wekaSegmentation.saveData(sd.getDirectory() + sd.getFileName()) )
			IJ.showMessage("There is no data to save");

		win.updateButtonsEnabling();
	}


	/**
	 * Add new class in the panel (up to MAX_NUM_CLASSES)
	 */
	private void addNewClass()
	{
		if(wekaSegmentation.getNumOfClasses() == WekaSegmentation_Save.MAX_NUM_CLASSES)
		{
			IJ.showMessage("Trainable Weka Segmentation", "Sorry, maximum number of classes has been reached");
			return;
		}

		String inputName = JOptionPane.showInputDialog("Please input a new label name");

		if(null == inputName)
			return;


		if (null == inputName || 0 == inputName.length()) 
		{
			IJ.error("Invalid name for class");
			return;
		}
		inputName = inputName.trim();

		if (0 == inputName.toLowerCase().indexOf("add to "))
			inputName = inputName.substring(7);

		// Add new name to the list of labels
		wekaSegmentation.setClassLabel(wekaSegmentation.getNumOfClasses(), inputName);
		wekaSegmentation.addClass();

		// Add new class label and list
		win.addClass();

		repaintWindow();

		// Macro recording
		String[] arg = new String[] { inputName };
		record(CREATE_CLASS, arg);
	}

	/**
	 * Add new class in the panel (up to MAX_NUM_CLASSES)
	 */
	private void addNewClass(String s)
	{
		if(wekaSegmentation.getNumOfClasses() == WekaSegmentation_Save.MAX_NUM_CLASSES)
		{
			IJ.showMessage("Trainable Weka Segmentation", "Sorry, maximum number of classes has been reached");
			return;
		}

		String inputName = s;

		if(null == inputName)
			return;


		if (null == inputName || 0 == inputName.length()) 
		{
			IJ.error("Invalid name for class");
			return;
		}
		inputName = inputName.trim();

		if (0 == inputName.toLowerCase().indexOf("add to "))
			inputName = inputName.substring(7);

		// Add new name to the list of labels
		wekaSegmentation.setClassLabel(wekaSegmentation.getNumOfClasses(), inputName);
		wekaSegmentation.addClass();

		// Add new class label and list
		win.addClass();

		repaintWindow();

		// Macro recording
		String[] arg = new String[] { inputName };
		record(CREATE_CLASS, arg);
	}

	/**
	 * Repaint whole window
	 */
	private void repaintWindow()
	{
		// Repaint window
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						win.invalidate();
						win.validate();
						win.repaint();
					}
				});
	}

	/**
	 * Call the Weka chooser
	 */
	public static void launchWeka()
	{
		GUIChooserApp chooser = new GUIChooserApp();
		for (WindowListener wl : chooser.getWindowListeners())
		{
			chooser.removeWindowListener(wl);
		}
		chooser.setVisible(true);
	}

	/**
	 * Show advanced settings dialog
	 *
	 * @return false when canceled
	 */
	public boolean showSettingsDialog()
	{
		GenericDialogPlus gd = new GenericDialogPlus("Segmentation settings");

		final boolean[] oldEnableFeatures = wekaSegmentation.getEnabledFeatures();
		final String[] availableFeatures = isProcessing3D ?	FeatureStack3D.availableFeatures : FeatureStack.availableFeatures;

		gd.addMessage("Standard training features");
		final int rows = (int) Math.round( availableFeatures.length/4.0 );
		gd.addCheckboxGroup( rows, 4, availableFeatures, oldEnableFeatures );
		disableMissingFeatures(gd);
		if(wekaSegmentation.getLoadedTrainingData() != null){
			final Vector<Checkbox> v = gd.getCheckboxes();
			for(Checkbox c : v)c.setEnabled(false);
			gd.addMessage("WARNING: no features are selectable while using loaded data");
		}


		gd.addMessage("\nHyperimage training features");
		final int rows2 = (int) Math.round( stringHyperFeatures.length/4.0 );
		gd.addCheckboxGroup( rows2, 4, stringHyperFeatures, tabHyperFeatures );


		gd.addMessage("Modalities used for hyperimage segmentation");
		gd.addCheckboxGroup( 1, 4,stringHyperModalities,tabHyperModalities);

		if( !isProcessing3D ){
			gd.addNumericField( "Membrane thickness:",wekaSegmentation.getMembraneThickness(), 0 );
			gd.addNumericField( "Membrane patch size:",wekaSegmentation.getMembranePatchSize(), 0 );
		}
		gd.addNumericField("Minimum sigma:", wekaSegmentation.getMinimumSigma(), 1);
		gd.addNumericField("Maximum sigma:", wekaSegmentation.getMaximumSigma(), 1);


		if(wekaSegmentation.getLoadedTrainingData() != null){
			final int nNumericFields = isProcessing3D ? 2 : 4;
			for(int i = 0; i < nNumericFields; i++)((TextField) gd.getNumericFields().get( i )).setEnabled(false);
		}

		gd.addMessage("Classifier options:");

		// Add Weka panel for selecting the classifier and its options
		GenericObjectEditor m_ClassifierEditor = new GenericObjectEditor();
		PropertyPanel m_CEPanel = new PropertyPanel(m_ClassifierEditor);
		m_ClassifierEditor.setClassType(Classifier.class);
		m_ClassifierEditor.setValue(wekaSegmentation.getClassifier());

		// add classifier editor panel
		gd.addComponent( m_CEPanel,  GridBagConstraints.HORIZONTAL , 1 );

		Object c = (Object)m_ClassifierEditor.getValue();
		String originalOptions = "";
		String originalClassifierName = c.getClass().getName();
		if (c instanceof OptionHandler) originalOptions = Utils.joinOptions(((OptionHandler)c).getOptions());

		gd.addMessage("Class names:");
		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)gd.addStringField("Class "+(i+1), wekaSegmentation.getClassLabel(i), 15);


		gd.addMessage("Advanced options:");
		gd.addCheckbox( "3D features", wekasave.isProcessing3D );
		gd.addCheckbox( "Balance classes", wekaSegmentation.doClassBalance() );
		gd.addButton("Save feature stack", new SaveFeatureStackButtonListener(	"Select location to save feature stack", wekaSegmentation,this ) );
		gd.addButton("Load feature stack", new LoadFeatureStackButtonListener(	"Select location to load feature stack", wekaSegmentation ,this) );
		gd.addSlider("Result overlay opacity", 0, 100, win.overlayOpacity);
		gd.addHelp("http://fiji.sc/Trainable_Weka_Segmentation");


		gd.showDialog();
		if (gd.wasCanceled())return false;

		final int numOfFeatures = availableFeatures.length;
		final boolean[] newEnableFeatures = new boolean[numOfFeatures];
		boolean featuresChanged = false;

		// Read checked features and check if any of them changed
		for(int i = 0; i < numOfFeatures; i++)	{
			newEnableFeatures[i] = gd.getNextBoolean();
			if (newEnableFeatures[i] != oldEnableFeatures[i])	featuresChanged = true;
		}

		// Read checked hyperfeatures and check if any of them changed
		for(int i = 0; i < tabHyperFeatures.length; i++){
			boolean feat=gd.getNextBoolean();
			if (feat != tabHyperFeatures[i]) {
				this.goodTrainerWasLastUsedAndParamsHadNotMove=false;
				System.out.println("Feature modified : "+stringHyperFeatures[i]+" passe de "+tabHyperFeatures[i]+" a "+feat);
				tabHyperFeatures[i]=feat;
			}
		}
		for(int i = 0; i < 4; i++){
			boolean feat=gd.getNextBoolean();
			if (feat != tabHyperModalities[i]) {
				this.goodTrainerWasLastUsedAndParamsHadNotMove=false;
				System.out.println("Modality modified : "+stringHyperModalities[i]+" passe de "+tabHyperModalities[i]+" a "+feat);
				tabHyperModalities[i]=feat;
			}
		}
		if ( !isProcessing3D ){
			final int newThickness = (int) gd.getNextNumber();
			if( newThickness != wekaSegmentation.getMembraneThickness() )			{
				featuresChanged = true;
				wekaSegmentation.setMembraneThickness(newThickness);
			}
			final int newPatch = (int) gd.getNextNumber();
			if( newPatch != wekaSegmentation.getMembranePatchSize() )			{
				featuresChanged = true;
				wekaSegmentation.setMembranePatchSize(newPatch);
			}
		}

		// Field of view (minimum and maximum sigma/radius for the filters)
		final float newMinSigma = (float) gd.getNextNumber();
		if(newMinSigma != wekaSegmentation.getMinimumSigma() && newMinSigma > 0){
			featuresChanged = true;
			wekaSegmentation.setMinimumSigma(newMinSigma);
		}

		final float newMaxSigma = (float) gd.getNextNumber();
		if(newMaxSigma != wekaSegmentation.getMaximumSigma() && newMaxSigma >= wekaSegmentation.getMinimumSigma())	{
			featuresChanged = true;
			wekaSegmentation.setMaximumSigma(newMaxSigma);
		}

		if(wekaSegmentation.getMinimumSigma() > wekaSegmentation.getMaximumSigma()){
			IJ.error("Error in the field of view parameters: they will be reset to default values");
			wekaSegmentation.setMinimumSigma(0f);
			wekaSegmentation.setMaximumSigma(128f);
		}

		// Set classifier and options
		c = (Object)m_ClassifierEditor.getValue();
		String options = "";
		final String[] optionsArray = ((OptionHandler)c).getOptions();
		if (c instanceof OptionHandler) options = Utils.joinOptions( optionsArray );

		if( !originalClassifierName.equals( c.getClass().getName() ) || !originalOptions.equals( options ) ){
			AbstractClassifier cls;
			try{
				cls = (AbstractClassifier) (c.getClass().newInstance());
				cls.setOptions( optionsArray );
			}
			catch(Exception ex){ex.printStackTrace();return false;}

			// Assign new classifier
			wekaSegmentation.setClassifier( cls );

			// Set the training flag to false  
			win.trainingComplete = false;

			IJ.log("Current classifier: " + c.getClass().getName() + " " + options);
		}

		boolean classNameChanged = false;
		for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)	{
			String s = gd.getNextString();
			if (null == s || 0 == s.length()) {
				IJ.log("Invalid name for class " + (i+1));
				continue;
			}
			s = s.trim();
			if(!s.equals(wekaSegmentation.getClassLabel(i)))	{
				if (0 == s.toLowerCase().indexOf("add to "))s = s.substring(7);

				wekaSegmentation.setClassLabel(i, s);
				classNameChanged = true;
				addExampleButton[i].setText("Add to " + s);
			}
		}

		// Update flag to balance number of class instances
		boolean val = gd.getNextBoolean();
		if( wekasave.isProcessing3D != val ) {
			if(wekasave.isProcessing3D || switchTo3DMode(false)) {wekasave.isProcessing3D=val; wekaSegmentation.isProcessing3D=val;this.goodTrainerWasLastUsedAndParamsHadNotMove=false;}
		}
		final boolean balanceClasses = gd.getNextBoolean();
		if( wekaSegmentation.doClassBalance() != balanceClasses )wekaSegmentation.setClassBalance( balanceClasses );


		// Update result overlay alpha
		final int newOpacity = (int) gd.getNextNumber();
		if( newOpacity != win.overlayOpacity ){
			win.overlayOpacity = newOpacity;
			win.overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, win.overlayOpacity / 100f);
			win.resultOverlay.setComposite(win.overlayAlpha);
			if( showColorOverlay )displayImage.updateAndDraw();
		}


		// If there is a change in the class names,
		// the data set (instances) must be updated.
		if(classNameChanged)win.pack();

		// Update feature stack if necessary
		if(featuresChanged)	wekaSegmentation.setFeaturesDirty();//Force recomputation of features
		else if( !wekaSegmentation.getFeatureStackArray().isEmpty()&& wekaSegmentation.getFeatureStackArray().getReferenceSliceIndex() != -1)wekaSegmentation.setUpdateFeatures(false);

		return true;
	}  // end showSettingsDialog

	// Quite of a hack from Johannes Schindelin:
	// use reflection to insert classifiers, since there is no other method to do that...
	static {
		try {
			IJ.showStatus("Loading Weka properties...");
			IJ.log("Loading Weka properties...");
			Field field = GenericObjectEditor.class.getDeclaredField("EDITOR_PROPERTIES");
			field.setAccessible(true);
			Properties editorProperties = (Properties)field.get(null);
			String key = "weka.classifiers.Classifier";
			String value = editorProperties.getProperty(key);
			value += ",hr.irb.fastRandomForest.FastRandomForest";
			editorProperties.setProperty(key, value);
			//new Exception("insert").printStackTrace();
			//System.err.println("value: " + value);
			WekaPackageManager.loadPackages( true );
			// add classifiers from properties (needed after upgrade to WEKA version 3.7.11)
			PluginManager.addFromProperties(editorProperties);						
		} catch (Exception e) {
			IJ.error("Could not insert my own cool classifiers!");
		}
	}

	/**
	 * Button listener class to handle the button action from the
	 * settings dialog to save the feature stack
	 */
	static class SaveFeatureStackButtonListener implements ActionListener
	{
		private String title;
		private TextField text;
		private WekaSegmentation_Save wekaSegmentation;
		private Weka_Save wekasave;

		/**
		 * Construct a listener for the save feature stack button
		 * 
		 * @param title save dialog title
		 * @param wekaSegmentation2 reference to the segmentation backend
		 */
		public SaveFeatureStackButtonListener(
				String title,
				WekaSegmentation_Save wekaSegmentation2 ,Weka_Save wekasave)
		{
			this.title = title;
			this.wekaSegmentation = wekaSegmentation2;
			this.wekasave=wekasave;
		}

		/**
		 * Method to run when pressing the save feature stack button
		 */
		public void actionPerformed(ActionEvent e)
		{		
			SaveDialog sd = new SaveDialog("Save the feature stack under the name feature-stack", "feature-stack", ".tif");
			final String dir = sd.getDirectory();
			final String fileWithExt = "feature-stack.tif";
			System.out.println("Enregistrement dans "+fileWithExt);
			if(null == dir || null == fileWithExt)
				return;
			final FeatureStackArray featureStackArray	= wekaSegmentation.getFeatureStackArray();
			for(int i=0; i<featureStackArray.getSize(); i++)
				wekaSegmentation.saveFeatureStack( i+1, dir);
			wekasave.saveFeatureSetup(dir+"setup.MFEATURE");
		}
	}	



	/**
	 * Button listener class to handle the button action from the
	 * settings dialog to save the feature stack
	 */
	static class LoadFeatureStackButtonListener implements ActionListener
	{
		private String title;
		private TextField text;
		private WekaSegmentation_Save wekaSegmentation;
		private Weka_Save wekasave;

		/**
		 * Construct a listener for the save feature stack button
		 * 
		 * @param title save dialog title
		 * @param wekaSegmentation2 reference to the segmentation backend
		 */
		public LoadFeatureStackButtonListener(
				String title,
				WekaSegmentation_Save wekaSegmentation2 ,Weka_Save wekasave)
		{
			this.title = title;
			this.wekaSegmentation = wekaSegmentation2;
			this.wekasave=wekasave;

		}

		/**
		 * Method to run when pressing the save feature stack button
		 */
		public void actionPerformed(ActionEvent e)
		{		
			
			//load header
			OpenDialog od=new OpenDialog("Choose file.MFEATURE associated feature-stack files","","setup.MFEATURE");
			if (od.getFileName()==null)return ;
			String dirName=od.getDirectory();
			wekasave.loadFeatureStack(wekasave,dirName);
			/*
			wekasave.loadFeatureSetup(fullPath);
			int Z=wekasave.rgbImage.getStackSize();
			int X=wekasave.rgbImage.getWidth();
			int Y=wekasave.rgbImage.getHeight();
			FeatureStackArray featuresArray = new FeatureStackArray(wekasave.rgbImage.getStackSize(), wekaSegmentation.getMinimumSigma(), wekaSegmentation.getMaximumSigma(), false,1, 1, null);
			System.out.println("\nLoading feature stack...");
			for(int z=1;z<=wekasave.rgbImage.getStackSize();z++) {
				String sli=(z>999 ? "" : ( z>99 ? "0" : ( z>9 ? "00" : "000") ) )+z+".tif";
				if(z==1)System.out.println("z="+z+" reading "+new File(dirName,"feature-stack"+sli).getAbsolutePath());
				ImageStack stack = IJ.openImage(new File(dirName,"feature-stack"+sli).getAbsolutePath()).getStack();
				FeatureStack fs=new FeatureStack(X,Y,false);
				fs.setStack(stack);
				featuresArray.set(fs, z-1);
				featuresArray.setEnabledFeatures(fs.getEnabledFeatures());
			}
			System.out.println("Volume de données considerees : #Feat="+featuresArray.get(0).getSize() + "  #X="+wekasave.rgbImage.getWidth()+"  #Y="+wekasave.rgbImage.getHeight()+ "  #Z="+wekasave.rgbImage.getStackSize()+" = "+
					VitimageUtils.dou(((featuresArray.get(0).getSize())*wekasave.rgbImage.getWidth()*wekasave.rgbImage.getHeight()*wekasave.rgbImage.getStackSize())/1E9)+" Giga-valeurs");

			wekasave.goodTrainerWasLastUsedAndParamsHadNotMove=true;
			wekasave.badTrainerWasLastUsed=false;
			wekaSegmentation.setFeatureStackArray(featuresArray);
			wekaSegmentation.setUpdateFeatures(false);
			*/
		}
	}	

	
	public void loadFeatureStack(Weka_Save wekasave,String dirName) {
		wekasave.loadFeatureSetup(dirName+"setup.MFEATURE");
		int Z=wekasave.rgbImage.getStackSize();
		int X=wekasave.rgbImage.getWidth();
		int Y=wekasave.rgbImage.getHeight();
		FeatureStackArray featuresArray = new FeatureStackArray(wekasave.rgbImage.getStackSize(), wekaSegmentation.getMinimumSigma(), wekaSegmentation.getMaximumSigma(), false,1, 1, null);
		System.out.println("\nChargement feature stack...");
		for(int z=1;z<=wekasave.rgbImage.getStackSize();z++) {
			String sli=(z>999 ? "" : ( z>99 ? "0" : ( z>9 ? "00" : "000") ) )+z+".tif";
			if(z==1)System.out.println("z="+z+" , reading file "+new File(dirName,"feature-stack"+sli).getAbsolutePath());
			ImageStack stack = IJ.openImage(new File(dirName,"feature-stack"+sli).getAbsolutePath()).getStack();
			FeatureStack fs=new FeatureStack(X,Y,false);
			fs.setStack(stack);
			featuresArray.set(fs, z-1);
			featuresArray.setEnabledFeatures(fs.getEnabledFeatures());
		}
		System.out.println("Volume de données considerees : #Feat="+featuresArray.get(0).getSize() + "  #X="+wekasave.rgbImage.getWidth()+"  #Y="+wekasave.rgbImage.getHeight()+ "  #Z="+wekasave.rgbImage.getStackSize()+" = "+
				VitimageUtils.dou(((featuresArray.get(0).getSize())*wekasave.rgbImage.getWidth()*wekasave.rgbImage.getHeight()*wekasave.rgbImage.getStackSize())/1E9)+" Giga-valeurs");
		wekasave.goodTrainerWasLastUsedAndParamsHadNotMove=true;
		wekasave.badTrainerWasLastUsed=false;
		wekaSegmentation.setFeatureStackArray(featuresArray);
		wekaSegmentation.setUpdateFeatures(false);
		System.out.println("Feature stack lue depuis "+dirName+" et chargee en memoire.");
	}



	/* **********************************************************
	 * Macro recording related methods
	 * *********************************************************/

	/**
	 * Macro-record a specific command. The command names match the static 
	 * methods that reproduce that part of the code.
	 * 
	 * @param command name of the command including package info
	 * @param args set of arguments for the command
	 */
	public static void record(String command, String... args) 
	{
		command = "call(\"trainableSegmentation.Weka_Segmentation." + command;
		for(int i = 0; i < args.length; i++)
			command += "\", \"" + args[i];
		command += "\");\n";
		// in Windows systems, replace backslashes by double ones
		if( IJ.isWindows() )
			command = command.replaceAll( "\\\\", "\\\\\\\\" );
		if(Recorder.record)
			Recorder.recordString(command);
	}

	/**
	 * Add the current ROI to a specific class and slice. 
	 * 
	 * @param classNum string representing the class index
	 * @param nSlice string representing the slice number
	 */
	public static void addTrace(
			String classNum,
			String nSlice)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			final Roi roi = win.getDisplayImage().getRoi();
			wekaSegmentation.addExample(Integer.parseInt(classNum), 
					roi, Integer.parseInt(nSlice));
			win.getDisplayImage().killRoi();
			win.drawExamples();
			win.updateExampleLists();
		}
	}

	/**
	 * Delete a specific ROI from the list of a specific class and slice
	 * 
	 * @param classNum string representing the class index
	 * @param nSlice string representing the slice number
	 * @param index string representing the index of the trace to remove
	 */
	public static void deleteTrace(
			String classNum,
			String nSlice,
			String index)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			wekaSegmentation.deleteExample(Integer.parseInt(classNum),
					Integer.parseInt(nSlice),
					Integer.parseInt(index) );
			win.getDisplayImage().killRoi();
			win.drawExamples();
			win.updateExampleLists();
		}
	}

	/**
	 * Train the current classifier
	 */
	public static void trainClassifier()
	{
		System.out.println("Go to static version of classification");
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			// Disable buttons until the training has finished
			win.setButtonsEnabled(false);

			win.setTrainingComplete(false);

			if( wekaSegmentation.trainClassifier() )
			{
				win.setTrainingComplete(true);
				wekaSegmentation.applyClassifier(false);
				win.setClassfiedImage( wekaSegmentation.getClassifiedImage() );
				if(win.isToogleEnabled())
					win.toggleOverlay();
				win.toggleOverlay();
			}
			win.updateButtonsEnabling();		
		}
	}

	/**
	 * Get the current result (labeled image)
	 */
	public static void getResult()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			ImagePlus classifiedImage =  wekaSegmentation.getClassifiedImage();
			if( null == classifiedImage )
			{
				// check if the training is complete
				if( win.trainingComplete )
				{
					// if not result image is there yet, calculate it
					win.setButtonsEnabled( false );
					wekaSegmentation.applyClassifier( false );
					classifiedImage = wekaSegmentation.getClassifiedImage();
					win.updateButtonsEnabling();
				}
				else
				{
					IJ.log( "Result image could not be created: "
							+ " you need to train or load a classifier first." );
					return;
				}
			}
			final ImagePlus resultImage = classifiedImage.duplicate();

			resultImage.setTitle("Classified image");

			convertTo8bitNoScaling( resultImage );

			resultImage.getProcessor().setColorModel( win.getOverlayLUT() );
			resultImage.getImageStack().setColorModel( win.getOverlayLUT() );
			resultImage.updateAndDraw();

			resultImage.show();
		}
	}

	/**
	 * Display the current probability maps
	 */
	public static void getProbability()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			IJ.showStatus("Calculating probability maps...");
			IJ.log("Calculating probability maps...");
			win.setButtonsEnabled(false);
			wekaSegmentation.applyClassifier(true);
			final ImagePlus probImage = wekaSegmentation.getClassifiedImage();
			if(null != probImage)
			{
				probImage.setDimensions( wekaSegmentation.getNumOfClasses(),
						win.getTrainingImage().getNSlices(), win.getTrainingImage().getNFrames() );
				if( win.getTrainingImage().getNSlices() * win.getTrainingImage().getNFrames() > 1 )
					probImage.setOpenAsHyperStack( true );
				probImage.show();
			}
			win.updateButtonsEnabling();
			IJ.showStatus("Done.");
			IJ.log("Done");
		}
	}

	/**
	 * Plot the current result (threshold curves)
	 */
	public static void plotResultGraphs()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			IJ.showStatus("Evaluating current data...");
			IJ.log("Evaluating current data...");
			win.setButtonsEnabled(false);
			final Instances data;
			if (wekaSegmentation.getTraceTrainingData() != null)
				data = wekaSegmentation.getTraceTrainingData();
			else
				data = wekaSegmentation.getLoadedTrainingData();

			if(null == data)
			{
				IJ.error("Error in plot result", "No data available yet to display results");
				return;
			}

			displayGraphs(data, wekaSegmentation.getClassifier());
			win.updateButtonsEnabling();
			IJ.showStatus("Done.");
			IJ.log("Done");
		}
	}

	/**
	 * Apply current classifier to specific image (2D or stack)
	 * 
	 * @param dir input image directory path
	 * @param fileName input image name
	 * @param showResultsFlag string containing the boolean flag to display results
	 * @param storeResultsFlag string containing the boolean flag to store result in a directory
	 * @param probabilityMapsFlag string containing the boolean flag to calculate probabilities instead of a binary result
	 * @param storeDir directory to store the results
	 */
	public static void applyClassifier(
			String dir,
			String fileName,
			String showResultsFlag,
			String storeResultsFlag,
			String probabilityMapsFlag,
			String storeDir)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			ImagePlus testImage = 
					IJ.openImage( dir + File.separator + fileName );

			if(null == testImage)
			{
				IJ.log("Error: " + dir + File.separator	+ fileName 
						+ " could not be opened");
				return;
			}

			boolean probabilityMaps = probabilityMapsFlag.contains("true");
			boolean storeResults = storeResultsFlag.contains("true");
			boolean showResults = showResultsFlag.contains("true");

			IJ.log( "Processing image " + dir + File.separator + fileName );

			ImagePlus segmentation = wekaSegmentation.applyClassifier(testImage, 0, probabilityMaps);

			if( !probabilityMaps )
			{
				// apply LUT to result image
				convertTo8bitNoScaling( segmentation );
				segmentation.getProcessor().setColorModel( win.getOverlayLUT() );
				segmentation.getImageStack().setColorModel( win.getOverlayLUT() );
				segmentation.updateAndDraw();
			}

			if (showResults) 
			{
				segmentation.show();
				testImage.show();
			}

			if (storeResults) 
			{
				String filename = storeDir + File.separator + fileName;
				IJ.log("Saving results to " + filename);
				IJ.save(segmentation, filename);
				segmentation.close();
				testImage.close();
			}
		}
	}

	/**
	 * Toggle current result overlay image
	 */
	public static void toggleOverlay()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			win.toggleOverlay();
		}
	}

	/**
	 * Load a new classifier
	 * 
	 * @param newClassifierPathName classifier file name with complete path
	 */
	public static void loadClassifier(String newClassifierPathName)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			IJ.log("Loading Weka classifier from " + newClassifierPathName + "...");

			win.setButtonsEnabled(false);

			final AbstractClassifier oldClassifier = wekaSegmentation.getClassifier();

			// Try to load Weka model (classifier and train header)
			if(  !wekaSegmentation.loadClassifier(newClassifierPathName) )
			{
				IJ.error("Error when loading Weka classifier from file");
				win.updateButtonsEnabling();
				return;
			}

			IJ.log("Read header from " + newClassifierPathName + " (number of attributes = " + wekaSegmentation.getTrainHeader().numAttributes() + ")");

			if(wekaSegmentation.getTrainHeader().numAttributes() < 1)
			{
				IJ.error("Error", "No attributes were found on the model header");
				wekaSegmentation.setClassifier(oldClassifier);
				win.updateButtonsEnabling();
				return;
			}
			wekaSegmentation.applyClassifier(false);//False means no probability maps
			win.toggleOverlay();
			win.toggleOverlay();
			// Set the flag of training complete to true
			win.trainingComplete = true;

			// update GUI
			win.updateAddClassButtons();

			IJ.log("Loaded " + newClassifierPathName);
		}
	}

	/**
	 * Save current classifier into a file
	 * 
	 * @param classifierPathName complete path name for the classifier file
	 */
	public static void saveClassifier( String classifierPathName )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			if( !wekaSegmentation.saveClassifier( classifierPathName ) )
			{
				IJ.error("Error while writing classifier into a file");
				return;
			}
		}
	}

	/**
	 * Load training data from file
	 * 
	 * @param arffFilePathName complete path name of the ARFF file
	 */
	public static void loadData(String arffFilePathName )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			win.setButtonsEnabled(false);
			IJ.log("Loading data from " + arffFilePathName + "...");
			wekaSegmentation.loadTrainingData( arffFilePathName );
			win.updateButtonsEnabling();
		}
	}

	/**
	 * Save training data into an ARFF file
	 * 
	 * @param arffFilePathName complete path name of the ARFF file
	 */
	public static void saveData(String arffFilePathName)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			if( !wekaSegmentation.saveData( arffFilePathName ))
				IJ.showMessage("There is no data to save");
		}
	}

	/**
	 * Create a new class 
	 * 
	 * @param inputName new class name
	 */
	public static void createNewClass( String inputName )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			if (null == inputName || 0 == inputName.length()) 
			{
				IJ.error("Invalid name for class");
				return;
			}
			inputName = inputName.trim();

			if (0 == inputName.toLowerCase().indexOf("add to "))
				inputName = inputName.substring(7);

			// Add new name to the list of labels
			wekaSegmentation.setClassLabel(wekaSegmentation.getNumOfClasses(), inputName);
			wekaSegmentation.addClass();

			// Add new class label and list
			win.addClass();

			win.updateAddClassButtons();
		}
	}


	/**
	 * Set membrane thickness for current feature stack
	 * 
	 * @param newThicknessStr new membrane thickness (in pixel units)
	 */
	public static void setMembraneThickness(String newThicknessStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final int newThickness = Integer.parseInt(newThicknessStr);		
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			if( newThickness != wekaSegmentation.getMembraneThickness() )
				wekaSegmentation.setFeaturesDirty();
			wekaSegmentation.setMembraneThickness(newThickness);
		}
	}

	/**
	 * Set a new membrane patch size for current feature stack
	 * 
	 * @param newPatchSizeStr new patch size (in pixel units)
	 */
	public static void setMembranePatchSize(String newPatchSizeStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			int newPatchSize = Integer.parseInt(newPatchSizeStr);
			if( newPatchSize  != wekaSegmentation.getMembranePatchSize() )
				wekaSegmentation.setFeaturesDirty();
			wekaSegmentation.setMembranePatchSize( newPatchSize );
		}
	}

	/**
	 * Set a new minimum radius for the feature filters
	 * 
	 * @param newMinSigmaStr new minimum radius (in float pixel units)
	 */
	public static void setMinimumSigma(String newMinSigmaStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			float newMinSigma = Float.parseFloat(newMinSigmaStr);
			if(newMinSigma  != wekaSegmentation.getMinimumSigma() && newMinSigma > 0)
			{
				wekaSegmentation.setFeaturesDirty();
				wekaSegmentation.setMinimumSigma(newMinSigma);
			}
			if(wekaSegmentation.getMinimumSigma() >= wekaSegmentation.getMaximumSigma())
			{
				IJ.error("Error in the field of view parameters: they will be reset to default values");
				wekaSegmentation.setMinimumSigma(0f);
				wekaSegmentation.setMaximumSigma(128f);
			}
		}
	}

	/**
	 * Set a new maximum radius for the feature filters
	 * 
	 * @param newMaxSigmaStr new maximum radius (in float pixel units)
	 */
	public static void setMaximumSigma(String newMaxSigmaStr)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			float newMaxSigma = Float.parseFloat(newMaxSigmaStr);
			if(newMaxSigma  != wekaSegmentation.getMaximumSigma() && newMaxSigma > wekaSegmentation.getMinimumSigma())
			{
				wekaSegmentation.setFeaturesDirty();
				wekaSegmentation.setMaximumSigma(newMaxSigma);
			}
			if(wekaSegmentation.getMinimumSigma() >= wekaSegmentation.getMaximumSigma())
			{
				IJ.error("Error in the field of view parameters: they will be reset to default values");
				wekaSegmentation.setMinimumSigma(0f);
				wekaSegmentation.setMaximumSigma(128f);
			}
		}
	}

	/**
	 * Set the class homogenization flag for training
	 *
	 * @param flagStr true/false if you want to balance the number of samples per class before training
	 */
	public static void setClassHomogenization(String flagStr)
	{
		setClassBalance( flagStr );
	}

	/**
	 * Set the class balance flag for training
	 *
	 * @param flagStr true/false if you want to balance the number of samples per class before training
	 */
	public static void setClassBalance( String flagStr )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			boolean flag = Boolean.parseBoolean(flagStr);
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			wekaSegmentation.setClassBalance( flag );
		}
	}

	/**
	 * Set classifier for current segmentation
	 * 
	 * @param classifierName classifier name with complete package information
	 * @param options classifier options
	 */
	public static void setClassifier(String classifierName, String options)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			try {
				AbstractClassifier cls = (AbstractClassifier)( Class.forName(classifierName).newInstance() );
				cls.setOptions( options.split(" "));
				wekaSegmentation.setClassifier(cls);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Save current feature stack(s)
	 * 
	 * @param dir directory to save the stack(s)
	 * @param fileWithExt file name with extension for the file(s)
	 */
	public static void saveFeatureStack(String dir)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			final FeatureStackArray featureStackArray = wekaSegmentation.getFeatureStackArray();
			if(featureStackArray.isEmpty())
			{
				featureStackArray.updateFeaturesMT();
			}
			for(int i=0; i<featureStackArray.getSize(); i++)
			{
				final String fileName = dir + "feature-stack" 
				+ String.format("%04d", (i+1)) + ".tif";
				if( !featureStackArray.get(i).saveStackAsTiff(fileName))
				{
					IJ.error("Error", "Feature stack could not be saved");
					return;
				}

				IJ.log("Saved feature stack for slice " + (i+1) + " as " + fileName);
			}
		}
	}

	/**
	 * Change a class name
	 * 
	 * @param classIndex index of the class to change
	 * @param className new class name
	 */
	public static void changeClassName(String classIndex, String className)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			int classNum = Integer.parseInt(classIndex);
			wekaSegmentation.setClassLabel(classNum, className);
			win.updateAddClassButtons();
			win.pack();
		}
	}

	/**
	 * Enable/disable a single feature of the feature stack(s)
	 * 
	 * @param feature name of the feature + "=" true/false to enable/disable
	 */
	public static void setFeature(String feature)
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();
			final boolean isProcessing3D = wekaSegmentation.isProcessing3D();

			int index = feature.indexOf("=");
			String featureName = feature.substring(0, index);
			boolean featureValue = feature.contains("true");

			boolean[] enabledFeatures = wekaSegmentation.getEnabledFeatures();
			boolean forceUpdate = false;
			for(int i=0; i<enabledFeatures.length; i++)
			{
				final String availableFeature = isProcessing3D ?
						FeatureStack3D.availableFeatures[i] :
							FeatureStack.availableFeatures[i];
						if ( availableFeature.equals(featureName) && featureValue != enabledFeatures[i])
						{
							enabledFeatures[i] = featureValue;
							forceUpdate = true;
						}
			}
			wekaSegmentation.setEnabledFeatures(enabledFeatures);

			if(forceUpdate)
			{
				// Force features to be updated
				wekaSegmentation.setFeaturesDirty();
			}
		}
	}	

	/**
	 * Set overlay opacity
	 * @param newOpacity string containing the new opacity value (integer 0-100)
	 */
	public static void setOpacity( String newOpacity )
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			win.overlayOpacity = Integer.parseInt(newOpacity);
			AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,  win.overlayOpacity  / 100f);			
			win.resultOverlay.setComposite(alpha);
		}
	}	

	/**
	 * Disables features which rely on missing third party libraries.
	 * @param gd settings dialog
	 * */
	public static void disableMissingFeatures(final GenericDialog gd)
	{
		if (!isImageScienceAvailable()) {
			IJ.log("Warning: ImageScience library unavailable. " +
					"Some training features will be disabled.");
			@SuppressWarnings("unchecked")
			final Vector<Checkbox> v = gd.getCheckboxes();
			for (int i = 0; i < v.size(); i++) {
				if (FeatureStack.IMAGESCIENCE_FEATURES[i]) {
					v.get(i).setState(false);
					v.get(i).setEnabled(false);
				}
			}
		}
	}

	/**
	 * Check if ImageScience features are available
	 * @return true if ImageScience features are available
	 */
	private static boolean isImageScienceAvailable() {
		try {
			return ImageScience.isAvailable();
		}
		catch (final NoClassDefFoundError err) {
			return false;
		}
	}
	/**
	 * Create label image out of the current user traces. For convention, the
	 * label zero is used to define pixels with no class assigned. The rest of
	 * integer values correspond to the order of the classes (1 for the first
	 * class, 2 for the second class, etc.).
	 *
	 * @return label image containing user-defined traces (zero for undefined pixels)
	 */
	public static ImagePlus getLabelImage()
	{
		final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
		if( iw instanceof CustomWindow )
		{
			final CustomWindow win = (CustomWindow) iw;
			final WekaSegmentation_Save wekaSegmentation = win.getWekaSegmentation();

			final int numClasses = wekaSegmentation.getNumOfClasses();
			final int width = win.getTrainingImage().getWidth();
			final int height = win.getTrainingImage().getHeight();
			final int depth = win.getTrainingImage().getNSlices();
			System.out.println("Depth="+depth);
			final ImageStack labelStack;
			if( numClasses < 256)
				labelStack = ImageStack.create( width, height, depth, 8 );
			else if ( numClasses < 256 * 256 )
				labelStack = ImageStack.create( width, height, depth, 16 );
			else
				labelStack = ImageStack.create( width, height, depth, 32 );

			final ImagePlus labelImage = new ImagePlus( "Labels", labelStack );
			for( int i=0; i<depth; i++ )
			{
				labelImage.setSlice( i+1 );
				for( int j=0; j<numClasses; j++ )
				{
					List<Roi> rois = wekaSegmentation.getExamples( j, i+1 );
					for( final Roi r : rois )
					{
						final ImageProcessor ip = labelImage.getProcessor();
						ip.setValue( j+1 );
						if( r.isLine() )
						{
							ip.setLineWidth( Math.round( r.getStrokeWidth() ) );
							ip.draw( r );
						}
						else
							ip.fill( r );
					}
				}
			}
			labelImage.setSlice( 1 );
			labelImage.setDisplayRange( 0, numClasses );
			return labelImage;
		}
		return null;
	}




	private void toggleHyperView(int newView) {
		this.lastCurrentSlice = isHyper ? (displayImage.getCurrentSlice()- (displayImage.getFrame()-1)*trainingImage.getStackSize() ) : displayImage.getCurrentSlice();
		this.lastCurrentFrame = isHyper ? displayImage.getFrame() : -1;
		if (newView==2) {
			System.out.println("To photo");
			switchViewToPhoto();
			displayImage.updateAndDraw();
			toggleHyperButton.setEnabled(true);
			togglePhotoButton.setEnabled(false);
			toggleTrainingButton.setEnabled(true);
		}
		else if (newView==0) {
			System.out.println("To Hyperimage");
			switchViewToHyperimage();
			displayImage.updateAndDraw();
			toggleHyperButton.setEnabled(false);
			togglePhotoButton.setEnabled(true);
			toggleTrainingButton.setEnabled(true);
		}
		else if (newView==1) {
			System.out.println("To Training");
			switchViewToTraining();
			displayImage.updateAndDraw();
			toggleHyperButton.setEnabled(true);
			togglePhotoButton.setEnabled(true);
			toggleTrainingButton.setEnabled(false);
		}
		win.repaint();
		IJ.run("Out [-]");
		IJ.run("Out [-]");
		IJ.run("Out [-]");
		IJ.run("In [+]");
		IJ.run("In [+]");
		IJ.run("In [+]");
		displayImage.setT(this.lastCurrentFrame);
		win.getDisplayImage().setSlice(this.lastCurrentSlice+(this.lastCurrentFrame-1)*this.trainingImage.getStackSize());
		displayImage.setSlice(this.lastCurrentSlice+(this.lastCurrentFrame-1)*this.trainingImage.getStackSize());
		win.ZZselector.setValue(this.lastCurrentSlice);
		win.updateSliceSelector();
		displayImage.killRoi();
		win.drawExamples();
		win.updateExampleLists();
		if(showColorOverlay){
			updateResultOverlay();
			displayImage.updateAndDraw();
		}
	}

	private void switchViewToPhoto() {
		displayImage=VitimageUtils.imageCopy(rgbHyper);
		win.setImagePlus(displayImage);
	}

	private void switchViewToTraining() {
		displayImage=VitimageUtils.imageCopy(trainingHyper);
		win.setImagePlus(displayImage);
	}

	private void switchViewToHyperimage() {
		displayImage=VitimageUtils.imageCopy(hyperImage);
		win.setImagePlus(displayImage);
	}

	private void saveExamples() {
		saveExamplesInFile(convertExamplesToSlicePointRoi(),rgbImage,this.wekaSegmentation.getClassLabels(),null);
		System.out.println("Examples saved.");
	}

	private void loadExamples() {
		loadExamplesFromFile(null,0,new int[][] {{0},{0}},null,null);
		System.out.println("Examples loaded.");
	}


	public static void saveExamplesInFile(PointRoi[][]prTab,ImagePlus imageOfTargetSize,String[]classLabels,String fileOutput) {		
		String fullPath="";
		if(fileOutput==null) {
			VitiDialogs.getYesNoUI("","Warning : this routine will build a lot of Roi files, on for each slice represented."+ 
					"You should consider store them in a separate directory. Also, the 'no' and the 'cancel' answer are just here for the sake of politeness.");	
			SaveDialog sd = new SaveDialog("Choose save file", "examples",".MROI");
			if (sd.getFileName()==null)return;
			String dirName=sd.getDirectory();
			String fullName=sd.getFileName();
			fullPath=new File(dirName,fullName).getAbsolutePath();
		}
		else fullPath=fileOutput;
		String fullPathNoExt=fullPath.substring(0, fullPath.lastIndexOf('.'));
		System.out.println("Saving ROI collection in "+fullPath);


		System.out.println(fullPathNoExt);
		System.out.println("Nombre de classes annoncees : "+prTab.length);

		int X=imageOfTargetSize.getWidth();
		int Y=imageOfTargetSize.getHeight();

		for(int cl=0;cl<prTab.length;cl++) {
			System.out.println("Classe "+cl+" , nombre de z : "+prTab[cl].length);
			for(int z=1;z<=prTab[cl].length;z++) {
				PointRoi pr=prTab[cl][z-1];
				if(pr.getContainedPoints().length==0)continue;
				int removed=0;
				for(Point  p : pr)if( (p.x<0) || (p.y<0) || (p.x>=X) || (p.y>=Y))removed++;
				String sliceFileName=fullPathNoExt+"_Class_"+cl+"_Slice_"+z+".txt";
				PrintWriter writer=null;
				try {
					writer = new PrintWriter(sliceFileName, "UTF-8");
				} catch (FileNotFoundException | UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				writer.println((pr.getContainedPoints().length-removed));
				for(Point p:pr) {
					if( (p.x>=0) || (p.y>=0) || (p.x<X) || (p.y<Y))writer.println((int)Math.round(p.x)+" "+(int)Math.round(p.y));
				}
				writer.close();	
			}	
		}
		PrintWriter writer=null;
		try {
			writer = new PrintWriter(fullPathNoExt+".MROI", "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		writer.println(prTab.length);
		for(int cl=0;cl<prTab.length;cl++) {
			writer.println(classLabels[cl]);
		}
		writer.close();	
		return;
	}


	public static PointRoi convertRoiListToUniquePointRoi(List<Roi>list) {
		PointRoi prOut=new PointRoi();
		for(Roi r : list) {
			Point[] ptTab=r.getContainedPoints();
			for(int pt=0;pt<ptTab.length;pt++) {
				prOut.addPoint(ptTab[pt].x,ptTab[pt].y);
			}
		}
		return prOut;
	}


	public PointRoi [][] convertExamplesToSlicePointRoi() {
		System.out.println("Conversion des examples");
		int nClasses=this.numOfClasses;
		PointRoi[][]tabRoi=new PointRoi[nClasses][this.trainingImage.getStackSize()];

		//Pour chaque classe
		for(int cl=0;cl<nClasses;cl++) {

			//Pour chaque slice, récuperer la liste des roi, l'assembler en une seule pointroi
			for(int sli=1;sli<=this.trainingImage.getStackSize();sli++) {
				System.out.println("Taille train="+this.trainingImage.getStackSize());
				List<Roi>list=this.wekaSegmentation.getExamples(cl, sli);
				tabRoi[cl][sli-1]=this.convertRoiListToUniquePointRoi(list);
			}
		}
		return tabRoi;
	}


	public void removeAllExamples() {
		System.out.println("Taille2 train="+this.trainingImage.getStackSize());
		for(int sl=1;sl<=this.trainingImage.getStackSize();sl++) {
			for(int cl=0;cl<this.numOfClasses;cl++) {
				int nbEx=this.wekaSegmentation.getExamples(cl,sl).size();
				for(int ex=nbEx-1;ex>=0;ex--)this.wekaSegmentation.deleteExample(cl, sl,ex);
			}
		}
	}
	public ImagePlus[]getUpdatedTrainingHyperImage(){
		int nbCan=0;
		for(int c=0;c<tabHyperModalities.length;c++)if(tabHyperModalities[c])nbCan++;
		ImagePlus []trainTab=new ImagePlus[nbCan];nbCan=0;
		for(int c=0;c<tabHyperModalities.length;c++)if(tabHyperModalities[c]) {trainTab[nbCan]=VitimageUtils.imageCopy(hyperTab[c+1]);nbCan++;}
		return trainTab;
	}

	public ImagePlus runTrainingNoThread(boolean classify) {
		System.out.println("\nEntrainement du classifieur no thread");
		trainingFlag = true;
		trainSeparatedButton.setText("STOP");
		win.updateButtonsEnabling();

		try{
			//Actualize the featureStack
			if(!goodTrainerWasLastUsedAndParamsHadNotMove) {
				FeatureStackArray fsa=null;
				if(isProcessing3D){
					fsa=buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(getUpdatedTrainingHyperImage(),tabHyperFeatures, (int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()));
				}
					
				else {
					fsa=buildFeatureStackArrayRGBSeparatedMultiThreadedV2(getUpdatedTrainingHyperImage(),tabHyperFeatures,(int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()));
				}
				goodTrainerWasLastUsedAndParamsHadNotMove=true;
				badTrainerWasLastUsed=false;
				wekaSegmentation.setFeatureStackArray(fsa);
				wekaSegmentation.setUpdateFeatures(false);
			}

			// Set train button text to STOP
			trainSeparatedButton.setText("STOP");
			wekaSegmentation.setClassBalance(wekasave.classBalance);
			FastRandomForest rf=(FastRandomForest)wekaSegmentation.getClassifier();
			rf.setNumFeatures(wekasave.nbFeatures);
			rf.setNumTrees(wekasave.nbTrees);
			rf.setComputeImportances(computingImportance);
			if( wekaSegmentation.trainClassifier() ){
				if(computingImportance)printImportances();
				if(classify) {
					wekaSegmentation.applyClassifier(false);//False means no probability maps				
					classifiedImage = wekaSegmentation.getClassifiedImage();
					if(showColorOverlay)
						win.toggleOverlay();
					win.toggleOverlay();
					win.trainingComplete = true;
				}
			}
			else
			{
				IJ.log("The traning did not finish.");
				win.trainingComplete = false;
			}

		}
		catch(Exception e) {e.printStackTrace();}
		catch( OutOfMemoryError err ){
			err.printStackTrace();
			IJ.log( "ERROR: plugin run out of memory. Please, "
					+ "use a smaller input image or fewer features." );
		}
		finally
		{
			trainingFlag = false;						
			trainSeparatedButton.setText("Hyperimage training");
			win.updateButtonsEnabling();										
			trainingTask = null;

		}
		if(classify) {
			classifiedImage = wekaSegmentation.getClassifiedImage();
			if(showColorOverlay)
				win.toggleOverlay();
			win.toggleOverlay();
			win.trainingComplete = true;
			return classifiedImage;
		}
		win.trainingComplete = true;
		return null;
	}


	public void listFeatures() {
		FeatureStack fs=wekaSegmentation.getFeatureStack(1);
		for(int i=0;i<fs.getSize();i++) {
			System.out.println("Feature "+i+fs.getSliceLabel(i+1));
		}
	}
	
	public void printImportances() {
		System.out.println(" Computation of Features importances....");
		double[]tabImp=((FastRandomForest)wekaSegmentation.getClassifier()).getFeatureImportances();
		System.out.println("Affichage des importances :");
		System.out.println(TransformUtils.stringVectorN(tabImp, ""));
		FeatureStack fs=wekaSegmentation.getFeatureStack(1);
		String[]strFeat= {"Init","-Gauss-","DiffGauss","DivGauss","Edges","Max","Min","Med","Var","Deriche"};
		int[]sig=getSigmaTab((int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()));
		int nbFeat=10;
		double []impFeat=new double[nbFeat];
		double []impFeatRel=new double[nbFeat];
		double []impSig=new double[sig.length];
		double[]impCan=new double[4];
		double total=0;
		for(int i=0;i<fs.getSize();i++) {
			total+=tabImp[i];
		}
		for(int i=0;i<fs.getSize();i++) {
			System.out.println("Feature "+i+fs.getSliceLabel(i+1)+" : "+VitimageUtils.dou(100*tabImp[i]/total)+" %");
			for(int f=0;f<nbFeat;f++) {
				if(fs.getSliceLabel(i+1).indexOf(strFeat[f])!=-1)impFeat[f]+=VitimageUtils.dou(100*tabImp[i]/total);
			}
			for(int c=0;c<4;c++) {
				if(fs.getSliceLabel(i+1).indexOf("_Can"+c)!=-1)impCan[c]+=VitimageUtils.dou(100*tabImp[i]/total);
			}
			for(int s=0;s<impSig.length;s++) {
				if(fs.getSliceLabel(i+1).indexOf("_SIG"+sig[s])!=-1)impSig[s]+=VitimageUtils.dou(100*tabImp[i]/total);
			}
		}
		double totalC=0;
		for(int c=0;c<4;c++) totalC+=impCan[c];
		System.out.println("\nRecapitulatif par canal:");
		for(int c=0;c<4;c++) System.out.println("Canal "+c+" : "+wekasave.stringHyperModalities[c]+" : "+(100*impCan[c]/totalC)+" % ");
		System.out.println("\nRecapitulatif par Famille de features:");
		double totalF=0;
		for(int f=0;f<nbFeat;f++) { 
			if(f==0 || f==4)impFeatRel[f]=impFeat[f];
			else if(f==3)impFeatRel[f]=impFeat[f]/impCan.length;
			else impFeatRel[f]=impFeat[f];
			totalF++;
			System.out.println("Feature "+f+" : "+strFeat[f]+" : importance de la famille = "+impFeat[f]+" % , importance relative (divisée par taille famille) = "+(100*impFeatRel[f]/totalF));
		}
		System.out.println("\nRecapitulatif par sigma:");
		double totalS=0;
		for(int s=0;s<impSig.length;s++) totalS+=impSig[s];
		for(int s=0;s<impSig.length;s++) {
			System.out.println("Sigma "+s+" - "+sig[s]+" pixels : importance de la famille = "+(100*impSig[s]/totalS)+" % ");
		}


		System.out.println("Ok.\n");
	}

	
	
	public void evaluateCross(String dirOfFeatureStack,String exampleFile,String fileToExport1,int selection,boolean buildImage,int[]lookup,String[]strNew) {
		double[][][][]confusions=null;
		int[][][]testRes=null;
		String dir="/home/fernandr/Bureau/Examples_3/RUN_A/";
		System.out.println("Demarrage d'une action d evaluation croisée de type "+selection);
		if(dirOfFeatureStack !=null)loadFeatureStack(this,dirOfFeatureStack);
		System.out.println("Feature stack chargee depuis "+dirOfFeatureStack);
		String fileToExport;
		
		
		if(selection==WEKA_SPECIMEN_CROSS_VALIDATION) {
			System.out.println("Starting weka cross validation 1-fold");
			confusions=new double[12][2][][];
			testRes=new int[12][][];
			System.out.println("\n\n\n\n\n\n############################################################################################\n##############################################################################################\n################################ STARTING SPECIMEN CROSS VALIDATION ##########################\n##############################################################################################\n##############################################################################################");
			IJ.log("\n\n\n\n\n\n############################################################################################\n##############################################################################################\n################################ STARTING SPECIMEN CROSS VALIDATION ##########################\n##############################################################################################\n##############################################################################################");
			for(int spec=0;spec<12;spec++) {	
				int[]tabTrain=new int[11];
				int[]tabTest=new int[1];
				int incr=0;
				for(int s=0;s<N_SPECIMENS;s++)if(s!=spec)tabTrain[incr++]=s;
				tabTest[0]=spec;
				System.out.println("\n\n\n#################  Attempt "+spec+" , excluding specimen "+stringSpecimens[spec]+" #################");
				IJ.log("\n\n\n#################  Attempt "+spec+" , excluding specimen "+stringSpecimens[spec]+" #################");
				fileToExport=fileToExport1+"SPEC_"+spec+"_";
				Object[]objs=evaluateV2(exampleFile, fileToExport, WEKA_EXCLUDE_SPECIMEN, new int[][] {tabTrain,tabTest},lookup,strNew);	
				confusions[spec]=(double[][][])objs[0];
				testRes[spec]=(int[][])objs[1];
			}
		}
		else if(selection==WEKA_SYMPTOM_CROSS_VALIDATION) {
			System.out.println("Starting weka cross validation symptom-fold");
			System.out.println("\n\n\n\n\n\n##############################################################\n####### STARTING SYMPTOM CROSS VALIDATION ############\n##################################################################\n");
			IJ.log("\n\n\n\n\n\n##############################################################\n####### STARTING SYMPTOM CROSS VALIDATION ############\n##################################################################\n");
			confusions=new double[4][2][][];
			testRes=new int[4][][];
			if(dirOfFeatureStack !=null)loadFeatureStack(this,dirOfFeatureStack);
			for(int sympt=0;sympt<4;sympt++) {	
				System.out.println("\n\n\n#################  Attempt "+sympt+" , excluding specimen "+stringSymptoms[sympt]+" #################");
				IJ.log("\n\n\n ################# Attempt "+sympt+" , excluding specimen "+stringSymptoms[sympt]+" #################");
				fileToExport=fileToExport1+"SYMPT_"+sympt+"_";
				Object[]objs=evaluateV2(exampleFile, fileToExport, WEKA_SYMPTOM_CROSS_VALIDATION, new int[][] {{sympt}},lookup,strNew);	
				confusions[sympt]=(double[][][])objs[0];
				testRes[sympt]=(int[][])objs[1];
			}
		}
		if(selection==WEKA_SPECIMEN_TWO_FOLD_CROSS_VALIDATION) {
			System.out.println("Starting weka cross validation 2-fold");
			System.out.println("\n\n\n\n\n\n############################################################################################\n##############################################################################################\n################################ STARTING SPECIMEN CROSS VALIDATION ##########################\n##############################################################################################\n##############################################################################################");
			IJ.log("\n\n\n\n\n\n############################################################################################\n##############################################################################################\n################################ STARTING SPECIMEN CROSS VALIDATION ##########################\n##############################################################################################\n##############################################################################################");
			int nExp=6*11;
			int incr=-1;
			confusions=new double[nExp][2][][];
			testRes=new int[nExp][][];
			if(dirOfFeatureStack !=null)loadFeatureStack(this,dirOfFeatureStack);
			for(int spec1=0;spec1<12;spec1++) {	
				for(int spec2=spec1+1;spec2<12;spec2++) {	
					int[]tabTrain=new int[10];
					int[]tabTest=new int[2];
					int incr2=0;
					for(int s=0;s<N_SPECIMENS;s++)if(s!=spec1 && s!=spec2)tabTrain[incr2++]=s;
					tabTest[0]=spec1;
					tabTest[1]=spec2;
					int[][]specs=new int[][] {tabTrain,tabTest};
					System.out.println("\n\n\n#################  Attempt "+(incr+2)+"/"+nExp+" : training on "+TransformUtils.stringVectorN(tabTrain,"")+"  , testing on "+TransformUtils.stringVectorN(tabTest,"")+"");
					IJ.log("\n\n\n#################  Attempt "+(++incr+2)+"/"+nExp+" : training on "+TransformUtils.stringVectorN(tabTrain,"")+"  , testing on "+TransformUtils.stringVectorN(tabTest,"")+"");
					fileToExport=fileToExport1+"TWOFOLD_"+spec1+"-"+spec2+"_";
					Object[]objs=evaluateV2(exampleFile, fileToExport, WEKA_EXCLUDE_SPECIMEN, specs,lookup,strNew);	
					confusions[incr]=(double[][][])objs[0];
					testRes[incr]=(int[][])objs[1];
				}
			}
		}
		double[][][]confusionsSpec=assembleConfusionsTab(confusions);
		double[][]computationsTrain=statsFromConfusion(confusionsSpec[0]);
		double[][]computationsTest=statsFromConfusion(confusionsSpec[1]);
		System.out.println("\n\n\n\n\n\n  GLOBAL CROSS-VALIDATION RESULTS\n\n");
		IJ.log("\n\n\n\n\n\n  GLOBAL CROSS-VALIDATION RESULTS\n\n");
		writeConfusionStats(confusionsSpec[0],computationsTrain," train set",fileToExport1+"GLOB_TRAIN");
		writeConfusionStats(confusionsSpec[1],computationsTest," test set",fileToExport1+"GLOB_TEST");
		wekaSegmentation.wekasave.goodTrainerWasLastUsedAndParamsHadNotMove=true;
		if(buildImage) {
			ImagePlus confusionImg=buildConfusionImage(testRes,wekasave.wekaSegmentation.getNumOfClasses(),hyperTab[0]);
			IJ.saveAsTiff(confusionImg, fileToExport1+"GLOB_IMG.tif");
		}
	}


	public int[][]getActualAndPredictedValuesOnTrainAndTestSetWithoutImage(int[][]examplesValues){
		int n1=0;
		int n2=0;
		String s1="";
		String s2="";
		int N=examplesValues.length;int Ntrain=0;int Ntest=0;int incrTrain=0;int incrTest=0;int valAct=0;int valPred=0;
		int[]predictedByClassifier=new int[N];
		FastRandomForest rf=(FastRandomForest)wekaSegmentation.getClassifier();
		Instances dat=wekaSegmentation.createTrainingInstancesFromCoordinates(examplesValues);
		System.out.println("Features calculees pour les examples. Calcul des predictions");
		int exValSur10=examplesValues.length/10;
		int versionCompute=1;
		try {
			System.out.println("dat.size ?"+dat.size());
			double[][]tabProb=rf.distributionsForInstances(dat);
			for(int i=0;i<tabProb.length;i++) {
				predictedByClassifier[i]=VitimageUtils.indmax(tabProb[i]);

				if(examplesValues[i][4]==WEKA_IS_TRAINING_POINT)Ntrain++;
				else Ntest++;
			}
		} catch (Exception e) {e.printStackTrace();}

		System.out.println("Ok.");
		int[][]ret=new int[10][];
		ret[0]=new int[Ntrain];//actual value in training set
		ret[1]=new int[Ntrain];//predicted value in training set
		ret[2]=new int[Ntrain];//actual value in training set
		ret[3]=new int[Ntrain];//predicted value in training set
		ret[4]=new int[Ntrain];//predicted value in training set

		ret[5]=new int[Ntest];//actual value in test set
		ret[6]=new int[Ntest];//predicted value in test set
		ret[7]=new int[Ntest];//predicted value in test set
		ret[8]=new int[Ntest];//predicted value in test set
		ret[9]=new int[Ntest];//predicted value in test set

		for(int i=0;i<N;i++) {
			valPred=predictedByClassifier[i];
			valAct=examplesValues[i][3];
			if(examplesValues[i][4]==WEKA_IS_TRAINING_POINT) {ret[2][incrTrain]=examplesValues[i][0];ret[3][incrTrain]=examplesValues[i][1];ret[4][incrTrain]=examplesValues[i][2];ret[0][incrTrain]=valAct;ret[1][incrTrain++]=valPred;}
			else {
				ret[5][incrTest]=valAct;ret[6][incrTest]=valPred;ret[7][incrTest]=examplesValues[i][0];ret[8][incrTest]=examplesValues[i][1];ret[9][incrTest++]=examplesValues[i][2];
				if(ret[5][incrTest-1]==3 && ret[6][incrTest-1]==0) {
					s1+="\nPoint amadou predit comme un BG : "+ret[7][incrTest-1]+","+ret[8][incrTest-1]+","+ret[9][incrTest-1];
					n1++;
				}
				if(ret[5][incrTest-1]==0 && ret[6][incrTest-1]==3) {
					s2+="\nPoint BG predit comme un amadou : "+ret[7][incrTest-1]+","+ret[8][incrTest-1]+","+ret[9][incrTest-1];
					n2++;
				}
			}

		}
		N1+=n1;
		N2+=n2;
		System.out.println("N1="+N1);
		System.out.println("N2="+N2);
		//N1=27142
		//N2=1881
		//		VitimageUtils.writeStringInFile(s1, "/home/fernandr/Bureau/doc1.txt");
		//		VitimageUtils.writeStringInFile(s2, "/home/fernandr/Bureau/doc2.txt");
		return ret;
	}



	public Object[] evaluateV2(String exampleFile,String fileToExport,int selection,int[][]paramsN,int[]lookup,String[]strNew) {
		boolean exportFullData=debug;
		double[][][]ret=new double[3][][];
		System.out.println("Entering evaluate V2");
		int[][]examplesValues=loadExamplesFromFile(exampleFile,selection,paramsN,lookup,strNew);
		runTrainingNoThread(false);
		if(examplesValues==null) {System.out.println("No evaluation possible because no examples exists");return null;}
		int [][]trainAndTestSets= getActualAndPredictedValuesOnTrainAndTestSetWithoutImage(examplesValues);
		System.out.println("Calcul train and test set");
		String sTrain="Coordinates of training points. "+trainAndTestSets[0].length+" points\n";
		if(exportFullData) {
			System.out.println("Sauvegarde donnees completes du calcul : train set");
			for(int i=0;i<trainAndTestSets[0].length;i++) {
				sTrain+=(trainAndTestSets[0][i]==trainAndTestSets[1][i])+" . Val act="+trainAndTestSets[0][i]+" et val pred="+trainAndTestSets[1][i]+" aux coordonnees ("+trainAndTestSets[2][i]+", "+trainAndTestSets[3][i]+", "+trainAndTestSets[4][i]+")\n";
			}		

			if(fileToExport!=null)VitimageUtils.writeStringInFile(sTrain, fileToExport+"_train.txt");
			sTrain="Coordinates of testing points. "+trainAndTestSets[0].length+" points\n";
			System.out.println("Sauvegarde donnees completes du calcul : test set");
			for(int i=0;i<trainAndTestSets[5].length;i++) {
				sTrain+=(trainAndTestSets[5][i]==trainAndTestSets[6][i])+" . Val act="+trainAndTestSets[5][i]+" et val pred="+trainAndTestSets[6][i]+" aux coordonnees ("+trainAndTestSets[7][i]+", "+trainAndTestSets[8][i]+", "+trainAndTestSets[9][i]+")\n";
			}
			if(fileToExport!=null)VitimageUtils.writeStringInFile(sTrain, fileToExport+"_test.txt");
		}
		System.out.println("\nResults on the training set ");
		ret[0]=computeStatisticsOnPrediction(trainAndTestSets[0],trainAndTestSets[1],this.numOfClasses,"Training set",fileToExport+"_stats_train");
		System.out.println("Results on the test set ");
		ret[1]=computeStatisticsOnPrediction(trainAndTestSets[5],trainAndTestSets[6],this.numOfClasses,"Test set",fileToExport+"_stats_test");

		return new Object[] {ret,trainAndTestSets};
	}


	public double getFeatureValue(int x0,int y0,int z0,int numFeature) {
		return this.wekaSegmentation.featureStackArray.get(z0).getProcessor(numFeature+1).getPixelValue(x0, y0);
	}

	public static int getSpecimenHorizontal(int x, int y, int z) {
		return ((z-1)%2)*6 + ((int)Math.round(y )/250)*3+  (((int)Math.round(x)/375));
	}


	public int[][] loadExamplesFromFile(String fichier,int select,int [][]param,int []lookupClasses,String[]strNew){
		String exportDataInCustomFile=null;//"/home/fernandr/Bureau/ML_CEP/DATA_NN/data_all.txt";	
		String s="";
		int nbFeatures=this.wekaSegmentation.featureStackArray.getNumOfFeatures();
		boolean hasLookup=(lookupClasses!=null);
		if(hasLookup) {
			System.out.println("Application d'un fichier de lookup. Correspondances : ");
			for(int i=0;i<lookupClasses.length;i++)System.out.println("Classe "+i+" -> "+lookupClasses[i]);
		}
		System.out.println("There3 avec sekect="+select);
		if((select>0) && (select!=WEKA_SYMPTOM_CROSS_VALIDATION)) {//Weka_symptom
			System.out.println("Mise a jour pour division du set d exemples en trainset/testset, directive "+ (stringSelections[select])+": "+  
				( select<2 ? param[0] : "\nSpecimens de train "+TransformUtils.stringVectorN(param[0],"") +"\nSpecimens de test "+TransformUtils.stringVectorN(param[1],""  ) +((select==1 )? "%" : "")  ) );
		}
		boolean[]tabSelTrain=new boolean[N_SPECIMENS];
		boolean[]tabSelTest=new boolean[N_SPECIMENS];
		System.out.println("There3");
		if((select==2 || select>3) && select!=WEKA_SYMPTOM_CROSS_VALIDATION) {
			for(int n=0;n<N_SPECIMENS;n++)tabSelTrain[n]=false;
			for(int n=0;n<param[0].length;n++)tabSelTrain[param[0][n]]=true;
			for(int n=0;n<N_SPECIMENS;n++)tabSelTest[n]=false;
			for(int n=0;n<param[1].length;n++)tabSelTest[param[1][n]]=true;
		}
		System.out.println("There4");
		if(select==WEKA_SYMPTOM_CROSS_VALIDATION) {//Weka_symptom
			System.out.println("Mise a jour pour weka symptom");
			for(int n=0;n<N_SPECIMENS;n++)tabSelTrain[n]=true;
			for(int n=0;n<N_SPECIMENS;n++)tabSelTest[n]=false;
			tabSelTrain[0+3*param[0][0]]=tabSelTrain[1+3*param[0][0]]=tabSelTrain[2+3*param[0][0]]=false;tabSelTest[0+3*param[0][0]]=tabSelTest[1+3*param[0][0]]=tabSelTest[2+3*param[0][0]]=true;
		}
		System.out.println("\nRecap utilisations ceps : " );
		for(int i=0;i<N_SPECIMENS;i++) System.out.print(i+" "+(tabSelTrain[i] ? "Train " : "" )+(tabSelTest[i] ? "Test "  : "")+" | " );
		System.out.println("\n");
		int X=rgbImage.getWidth();
		int Y=rgbImage.getHeight();
		ArrayList<int[]> listRet=new ArrayList();
		removeAllExamples();
		String fullPath=null;
		if(fichier==null) {
			OpenDialog od=new OpenDialog("Choose file.MROI to load","","examples.MROI");
			if (od.getFileName()==null)return null;
			String dirName=od.getDirectory();
			String fullName=od.getFileName();
			fullPath=new File(dirName,fullName).getAbsolutePath();
		}
		else fullPath=fichier;
		String fullPathNoExt=fullPath.substring(0, fullPath.lastIndexOf('.'));
		System.out.println("Loading ROI collection in "+fullPath);
		String mroiFile=fullPathNoExt+".MROI";
		File f=new File(mroiFile);
		if(!f.exists())return null;
		int nClasses=0;
		int nSlices=0;
		int nClassesFile=0;
		try{
			InputStream flux=new FileInputStream(mroiFile); 
			InputStreamReader read=new InputStreamReader(flux);
			BufferedReader buff=new BufferedReader(read);
			String test=buff.readLine();
			nClassesFile=Integer.parseInt(test);
			
			if(!hasLookup) {
				nClasses=nClassesFile;
				System.out.println("Nombre de classes="+nClasses);
				if(nClasses<numOfClasses) {
					IJ.log("This won't go very well like this, because you want to load a model with a number of class lower than the actual one, what will crash the inteface. What you should do is to close the interface, and open it again. This command abort now to prevent this bad behaviour");
					buff.close();
					return null;
				}
				
				for(int i=0;i<nClasses;i++) {
					if(numOfClasses<i+1)wekasave.addNewClass(buff.readLine());
					else Weka_Save.changeClassName(""+i+"",buff.readLine() );
				}
			}
			else {
				nClasses=VitimageUtils.max(lookupClasses)+1;
				for(int i=0;i<nClasses;i++) {
					if(numOfClasses<i+1) {
						addNewClass(strNew[i]);
					}
					else {
						Weka_Save.changeClassName(""+i+"",strNew[i] );
					}
				}
				numOfClasses=nClasses;
				wekaSegmentation.setNumOfClasses(nClasses);
			}
			buff.close(); 
		}		
		catch (Exception e){e.printStackTrace();}
		int dimZ=this.displayImage.getStackSize()/(this.displayImage.getNFrames());
		nSlices=dimZ;
		PointRoi[][] prTab=new PointRoi[nClassesFile][nSlices];


		for(int cl=0;cl<nClassesFile;cl++) {
			System.out.print("-> Examples classe "+cl+" / "+nClassesFile);
			int incrTrTot=0;int incrTeTot=0;
			for(int z=1;z<=nSlices;z++) {    
				if(exportDataInCustomFile!=null)System.out.println("Demarrage " +z+" / "+nSlices+" : ");
				FeatureStack fsZ=null;
				s="";
				if(exportDataInCustomFile!=null)fsZ = this.wekaSegmentation.featureStackArray.get(z-1);
				prTab[cl][z-1]=new PointRoi();
				String stringFileSlice=fullPathNoExt+"_Class_"+cl+"_Slice_"+z+".txt";
				f=new File(stringFileSlice);
				if(!f.exists())continue;	
				try{
					InputStream flux=new FileInputStream(stringFileSlice); 
					InputStreamReader read=new InputStreamReader(flux);
					BufferedReader buff=new BufferedReader(read);
					String ligne=buff.readLine();
					int nPt=Integer.parseInt(ligne);
					//				System.out.print("Slice "+z+" points disponibles : "+nPt);
					int incrTrain=0;int incrTest=0;
					for(int i=0;i<nPt;i++) {
						if(exportDataInCustomFile!=null)if(((1000*i)/nPt)%100==0)System.out.print("-");
						ligne=buff.readLine();
						int x=Integer.parseInt(ligne.split(" ")[0]);
						int y=Integer.parseInt(ligne.split(" ")[1]);
						if( (x<0) || (y<0) || (x>=X) || (y>=Y))continue;
						if(select==WEKA_RANDOM ) {
							if(Math.random()<(param[0][0]/100.0)) {
								prTab[cl][z-1].addPoint(x, y);
								listRet.add(new int[] {(int)Math.round(x),(int)Math.round(y),z,hasLookup ? lookupClasses[cl]:cl,WEKA_IS_TRAINING_POINT});
								incrTrain++;
							}
							else {
								listRet.add(new int[] {(int)Math.round(x),(int)Math.round(y),z,hasLookup ? lookupClasses[cl]:cl,WEKA_IS_TEST_POINT});
								incrTest++;
							}
						}
						else if((select==WEKA_EXCLUDE_SPECIMEN) || (select==WEKA_SYMPTOM_CROSS_VALIDATION)) {
							if(tabSelTest[(z-1)%12]) {
								listRet.add(new int[] {(int)Math.round(x),(int)Math.round(y),z,hasLookup ? lookupClasses[cl]:cl,WEKA_IS_TEST_POINT} );
								incrTest++;
							}
							if (tabSelTrain[(z-1)%12]){
								listRet.add(new int[] {(int)Math.round(x),(int)Math.round(y),z,hasLookup ? lookupClasses[cl]:cl,WEKA_IS_TRAINING_POINT});
								prTab[cl][z-1].addPoint(x, y);		
								incrTrain++;
							}
						}
						else {
							prTab[cl][z-1].addPoint(x, y);
							listRet.add(new int[] {(int)Math.round(x),(int)Math.round(y),z,hasLookup ? lookupClasses[cl]:cl,WEKA_IS_TRAINING_POINT});
							incrTrain++;
							if(exportDataInCustomFile!=null) {
								String s2=""+x+";"+y+";"+(z-1)+";"+cl+";"+incrTrain;
								for (int zz=0; zz<nbFeatures; zz++) {
									s2+=";"+getFeatureValue(x,y,z-1,zz);
								} 
								s2+="\n";
								s+=s2;
							}
						}
					}
					buff.close(); 
					incrTeTot+=incrTest;
					incrTrTot+=incrTrain;
					if(hasLookup)this.addExampleFromLoadedFile(lookupClasses[cl],prTab[cl][z-1],z);
					else this.addExampleFromLoadedFile(cl,prTab[cl][z-1],z);
					if(exportDataInCustomFile!=null) VitimageUtils.writeStringInFile(s, exportDataInCustomFile+"_cl_"+cl+"z_"+(z-1)+".txt");
				}	
				catch (Exception e){}
			}
			System.out.println(" #Total="+(incrTeTot+incrTrTot)+" , #train set="+incrTrTot+" , #test set="+incrTeTot);
		}
		int nbTrain=0,nbTest=0;
		int[][] retTab=new int[listRet.size()][5];
		for(int i=0;i<listRet.size();i++) {retTab[i]=listRet.get(i);if(retTab[i][4]==WEKA_IS_TRAINING_POINT)nbTrain++; else nbTest++;}
		System.out.println("Set initial="+retTab.length+" exemples. Train set="+nbTrain+" exemples , Test set="+nbTest+" exemples");
		if(nbTrain+nbTest>0)saveExamplesButton.setEnabled(true);
		System.out.println("End loading Examples");
		return retTab;
	}


	public void loadExamplesToEvaluate(String fichier){
		int X=rgbImage.getWidth();
		int Y=rgbImage.getHeight();
		String fullPath=null;
		if(fichier==null) {
			OpenDialog od=new OpenDialog("Choose file.MROI to load","","examples.MROI");
			if (od.getFileName()==null)return ;
			String dirName=od.getDirectory();
			String fullName=od.getFileName();
			fullPath=new File(dirName,fullName).getAbsolutePath();
		}
		else fullPath=fichier;
		String fullPathNoExt=fullPath.substring(0, fullPath.lastIndexOf('.'));
		System.out.println("Loading ROI collection in "+fullPath);
		String mroiFile=fullPathNoExt+".MROI";
		File f=new File(mroiFile);
		if(!f.exists())return ;
		int nClasses=0;
		int nSlices=0;

		try{
			InputStream flux=new FileInputStream(mroiFile); 
			InputStreamReader read=new InputStreamReader(flux);
			BufferedReader buff=new BufferedReader(read);
			String test=buff.readLine();
			nClasses=Integer.parseInt(test);
			System.out.println("Nombre de classes="+nClasses);
			if(nClasses<numOfClasses) {
				IJ.log("This won't go very well like this, because you want to load a model with a number of class lower than the actual one, what will crash the inteface. What you should do is to close the interface, and open it again. This command abort now to prevent this bad behaviour");
				buff.close();
				return;
			}
			buff.close(); 
		}		
		catch (Exception e){e.printStackTrace();}
		int dimZ=this.displayImage.getStackSize();
		nSlices=dimZ;
		PointRoi[][] prTab=new PointRoi[nClasses][nSlices];


		for(int cl=0;cl<nClasses;cl++) {
			System.out.print("-> Examples classe "+cl);
			int incrTrTot=0;int incrTeTot=0;
			for(int z=1;z<=nSlices;z++) {    
				prTab[cl][z-1]=new PointRoi();
				String stringFileSlice=fullPathNoExt+"_Class_"+cl+"_Slice_"+z+".txt";
				f=new File(stringFileSlice);
				if(!f.exists())continue;	
				try{
					InputStream flux=new FileInputStream(stringFileSlice); 
					InputStreamReader read=new InputStreamReader(flux);
					BufferedReader buff=new BufferedReader(read);
					String ligne=buff.readLine();
					int nPt=Integer.parseInt(ligne);
					int incrTrain=0;int incrTest=0;
					for(int i=0;i<nPt;i++) {
						ligne=buff.readLine();
						int x=Integer.parseInt(ligne.split(" ")[0]);
						int y=Integer.parseInt(ligne.split(" ")[1]);
						if( (x<0) || (y<0) || (x>=X) || (y>=Y))continue;
						evaluate3d.addExample(new int[] {(int)Math.round(x),(int)Math.round(y),z-1,cl});
					}
					buff.close(); 
				}		
				catch (Exception e){}
			}
		}
	}



	public void actionEvaluation() {
		int step=4;
		evaluate3d.computeInstances(wekasave,wekaSegmentation,(int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()),wekasave.tabHyperFeatures) ;
		if(step==1) {
			loadExamplesToEvaluate(null);
			evaluate3d.finalizeExamplesList();
			evaluate3d.saveEvaluate3D();
			evaluate3d.prepareImages();
			step++;
		}
		if(step==2) {
			evaluate3d.computeInstances(wekasave,wekaSegmentation,(int)Math.round(wekaSegmentation.getMinimumSigma()),(int)Math.round(wekaSegmentation.getMaximumSigma()),wekasave.tabHyperFeatures) ;
			evaluate3d.fuseInstancesAndCoords();
			step++;
		}
		if(step==3) {
			evaluate3d.
			exportForNN();
			//evaluate3d.trainAndTest();
		}
	}

	
	

	public int[][]computeExamplesValues(int selection,int param) {
		PointRoi[][]prTab=convertExamplesToSlicePointRoi();
		ArrayList<int[]> listRet=new ArrayList();
		for(int cl=0;cl<prTab.length;cl++) {
			System.out.println("Classe "+cl+" , nombre de z : "+prTab[cl].length);
			for(int z=1;z<=prTab[cl].length;z++) {
				PointRoi pr=prTab[cl][z-1];
				if(pr.getContainedPoints().length==0)continue;
				for(Point p:pr) {

					if(selection==WEKA_RANDOM) {
						if(Math.random()<(param/100.0)) listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TRAINING_POINT});
						else listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TEST_POINT});
					}

					else if(selection==WEKA_EXCLUDE_SPECIMEN) {
						if(!this.isHorizontalStuffForTesting) {
							if((z-1)%12==param) listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TEST_POINT} );
							else listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TRAINING_POINT});
						}
						else {
							if(getSpecimenHorizontal((int)Math.round(p.x),(int)Math.round(p.y),z)==param) listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TEST_POINT} );
							else listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TRAINING_POINT});							
						}
					}
					else if(selection==WEKA_EXCLUDE_SYMPTOM) {
						if(!this.isHorizontalStuffForTesting) {
							if(((z-1)%12)/3==param)listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TEST_POINT});
							else listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TRAINING_POINT});
						}
						else {
							if(getSpecimenHorizontal((int)Math.round(p.x),(int)Math.round(p.y),z)/3==param) listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TEST_POINT} );
							else listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TRAINING_POINT});						
						}
					}
					else 	listRet.add(new int[] {(int)Math.round(p.x),(int)Math.round(p.y),z,cl,WEKA_IS_TRAINING_POINT});
				}
			}
		}
		int nbTrain=0,nbTest=0;
		int[][] retTab=new int[listRet.size()][5];
		for(int i=0;i<listRet.size();i++) {retTab[i]=listRet.get(i);if(retTab[i][4]==WEKA_IS_TRAINING_POINT)nbTrain++; else nbTest++;}
		System.out.println("Division du set constitue de "+retTab.length+" exemples. Train set="+nbTrain+" et test set="+nbTest);

		return retTab;
	}











	public boolean switchTo3DMode(boolean isCalledByBotSubRoutineAndNotByGUI) {
		//Si pas hyper, dire non et retourner
		if(!this.isHyper) {VitiDialogs.notYet("Switch to 3D mode in wekaSave with image that is not hyper image : not yet implemented");return false;}
		//Proposer de sauvegarder les settings
		if(VitiDialogs.getYesNoUI("","Do you want to save the settings (especially the 3D mode parameter) ?"))saveClassSetup(null);
		return true;
	}


	public void loadZstartAndZstop(String fullPath) {
		System.out.println("Loading zStart and zStop from "+fullPath);
		try{
			BufferedReader buff=new BufferedReader(new InputStreamReader(new FileInputStream(new File(fullPath))));

			//Lecture des specimen
			int nbSpec=Integer.parseInt(buff.readLine());
			if(nbSpec !=N_SPECIMENS)return;
			zStart=new int[N_SPECIMENS];
			zStop=new int[N_SPECIMENS];

			//Pour chaque specimen, lire son nom, son zStart (le numero de la premiere slice ou il est present dans l'hyperimage hybride et le numero de la derniere)
			for(int s=0;s<N_SPECIMENS;s++) {
				String str=buff.readLine();
				String stSpec=str.split(" ")[0];
				zStart[s]=Integer.parseInt(str.split(" ")[1]);
				zStop[s]=Integer.parseInt(str.split(" ")[2]);
				System.out.println("Lu : specimen "+stSpec + " intervalle=[ "+zStart[s]+" , "+zStop[s]+" ]");
			}
			buff.close(); 
		}		
		catch (Exception e){e.printStackTrace();}
	}

	
	

	public static double [][]computeStatisticsOnPrediction(int[]actual,int[]predicted,int nClasses,String title,String fileToExportResults) {
		String s="";
		if(actual.length!=predicted.length) {s+="WARNING : SIZE DOES NOT MATCH IN WEKA_SAVE. EXIT !\n";VitimageUtils.writeStringInFile(s, fileToExportResults);System.out.println(s);System.exit(0);}
		if(actual.length==0) {s+="Set "+title+" ne contient pas de donnees a analyser.\n";};
		double total=predicted.length;
		System.out.println("Calcul sur un jeu de "+total+" examples");
		s+="Etude d'un jeu de "+total+" examples\n";
		//Calcul de la matrice de confusion
		double[][]confusionMatrix=new double[nClasses][nClasses];//i stands for predicted (rows), and j stands for actual (columns)
		for(int i=0;i<predicted.length;i++)confusionMatrix[predicted[i]][actual[i]]++;

		s+=TransformUtils.stringMatrixMN("Matrice de confusion cas d etude "+title,confusionMatrix)+"\n";

		double[][]computations=statsFromConfusion(confusionMatrix);
		writeConfusionStats(confusionMatrix,computations," statistiques de confusion "+title,fileToExportResults);
		return confusionMatrix;
	}

	public static void writeConfusionStats(double[][]confusionMatrix,double[][]computations,String title, String fileToExportResults) {
		String s="";
		int nClasses=computations[0].length;
		double accuracy=computations[3][0];
		double total=computations[3][1];
		double[]precision=computations[0];
		double[]recall=computations[1];
		double []accuracies=computations[2];
		double[]subtotAct=computations[4];
		double []subtotPred=computations[5];
		s+=TransformUtils.stringMatrixMN( "Confusion matrix "+title, confusionMatrix);
		s+=TransformUtils.stringVectorN(precision, "Precision classes")+"\n";
		s+=TransformUtils.stringVectorN(recall, "Recall classes")+"\n";
		s+=TransformUtils.stringVectorN(accuracies, "Accuracies classes")+"\n";
		s+=TransformUtils.stringVectorN(subtotAct, "Subtotal actual")+"\n";
		s+=TransformUtils.stringVectorN(subtotPred, "Subtotal predicted")+"\n";
		s+="Accuracy="+accuracy+"\n";
		s+="Statistiques calculees sur "+(int)Math.round(total)+" valeurs\n";
		VitimageUtils.writeStringInFile(s, fileToExportResults+".txt");
		System.out.println(s);
		IJ.log(s);
	}

	public static double[][]statsFromConfusion(double[][]confusionMatrix){
		int nClasses=confusionMatrix.length;
		int total=0;for(int i=0;i<nClasses;i++)for(int j=0;j<nClasses;j++)total+=confusionMatrix[i][j];

		//Calcul de l accuracy
		double trace=0;for(int i=0;i<nClasses;i++)trace+=confusionMatrix[i][i];
		double accuracy=(total>0 ? trace/total : 0);

		//Calcul precision, recall et accuracy individuelles
		double []subtotalActual=new double[nClasses];
		double []subtotalPredicted=new double[nClasses];
		double []precision=new double[nClasses];
		double []recall=new double[nClasses];
		double []accuracies=new double[nClasses];
		for(int actcl=0;actcl<nClasses;actcl++) for(int predcl=0;predcl<nClasses;predcl++) {subtotalActual[actcl]+=confusionMatrix[predcl][actcl];  subtotalPredicted[predcl]+=confusionMatrix[predcl][actcl];}
		for(int cl=0;cl<nClasses;cl++) {precision[cl]=subtotalPredicted[cl]>0 ? confusionMatrix[cl][cl]/subtotalPredicted[cl] : 0;  recall[cl]=subtotalActual[cl] > 0 ? confusionMatrix[cl][cl]/subtotalActual[cl] : 0;}
		for(int cl=0;cl<nClasses;cl++) {accuracies[cl]=total > 0 ? (total+2*confusionMatrix[cl][cl]-subtotalPredicted[cl]-subtotalActual[cl])/total : 0;}
		return new double[][] {precision,recall,accuracies,{accuracy,total},subtotalActual,subtotalPredicted};
	}






	public int[][] getActualAndPredictedValuesOnTrainAndTestSet(int[][]tab,ImagePlus img ){
		IJ.log("3-31");
		int N=tab.length;int Ntrain=0;int Ntest=0;int incrTrain=0;int incrTest=0;int valAct=0;int valPred=0;
		IJ.log("3-32");
		for(int i=0;i<N;i++) {
			if(tab[i][4]==WEKA_IS_TRAINING_POINT)Ntrain++;
			else Ntest++;
		}
		IJ.log("3-33");
		int[][]ret=new int[10][];
		ret[0]=new int[Ntrain];//actual value in training set
		ret[1]=new int[Ntrain];//predicted value in training set
		ret[2]=new int[Ntrain];//actual value in training set
		ret[3]=new int[Ntrain];//predicted value in training set
		ret[4]=new int[Ntrain];//predicted value in training set

		ret[5]=new int[Ntest];//actual value in test set
		ret[6]=new int[Ntest];//predicted value in test set
		ret[7]=new int[Ntest];//predicted value in test set
		ret[8]=new int[Ntest];//predicted value in test set
		ret[9]=new int[Ntest];//predicted value in test set
		for(int i=0;i<N;i++) {
			valAct=(int)Math.round(img.getStack().getProcessor(tab[i][2]).getValue(tab[i][0], tab[i][1]));
			valPred=tab[i][3];
			if(tab[i][4]==WEKA_IS_TRAINING_POINT) {ret[2][incrTrain]=tab[i][0];ret[3][incrTrain]=tab[i][1];ret[4][incrTrain]=tab[i][2];ret[0][incrTrain]=valAct;ret[1][incrTrain++]=valPred;}
			else {ret[5][incrTest]=valAct;ret[6][incrTest]=valPred;ret[7][incrTest]=tab[i][0];ret[8][incrTest]=tab[i][1];ret[9][incrTest++]=tab[i][2];}
		}
		return ret;
	}



	private void addExampleFromLoadedFile(int cl,PointRoi r,int slice){
		displayImage.killRoi();
		wekaSegmentation.addExample(cl, r, slice);
		traceCounter[cl]++;
		win.drawExamples();
		win.updateExampleLists();
	}


	private void loadFeatureSetup(String file) {
		String fullPath=null;
		System.out.println("Action loadFeatureSetup");
		if(file==null) {
			OpenDialog od=new OpenDialog("Choose a setup file for your classes","","setup.MFEATURE");
			this.existingUserColormap=true;
			if (od.getFileName()==null)
				return;
			String dirName=od.getDirectory();
			String fullName=od.getFileName();
			fullPath=new File(dirName,fullName).getAbsolutePath();
		}
		else fullPath=file;
		System.out.println("Loading setup from "+fullPath);
		try{
			InputStream flux=new FileInputStream(new File(fullPath)); 
			InputStreamReader read=new InputStreamReader(flux);
			BufferedReader buff=new BufferedReader(read);

			//Lecture des settings
			boolean[]tab=new boolean[12];
			buff.readLine();//"################# SETTINGS #############");
			for(int i=0;i<12;i++)tab[i]=(buff.readLine().split(" ")[1]).equals("true");


			float newMin=((float)(Double.parseDouble( (buff.readLine().split(" ")[1]  )  )  ));
			float newMax=((float)(Double.parseDouble( (buff.readLine().split(" ")[1]  )  )  ));
			if(wekaSegmentation.getMaximumSigma() !=  newMax) {this.goodTrainerWasLastUsedAndParamsHadNotMove=false;wekaSegmentation.setMaximumSigma(newMax);}
			if(wekaSegmentation.getMinimumSigma() !=  newMin) {this.goodTrainerWasLastUsedAndParamsHadNotMove=false;wekaSegmentation.setMinimumSigma(newMin);}
			System.out.println("Lecture intervalle de sigma : ["+newMin+" , "+newMax+" ]");

			FastRandomForest rf=(FastRandomForest)wekaSegmentation.getClassifier();
			int numFeat=Integer.parseInt( (buff.readLine().split(" ")[1] ) ) ;
			int numTrees=Integer.parseInt( (buff.readLine().split(" ")[1] ) ) ;
			if(rf.getNumFeatures()!=numFeat || rf.getNumFeatures()!=numTrees ) {rf=new FastRandomForest();rf.setNumFeatures(numFeat);rf.setNumTrees(numTrees);wekaSegmentation.setClassifier(rf);}
			System.out.println("parametres de l'arbre lus dans settings : feat , trees="+numFeat+" , "+numTrees);


			for(int mod=0;mod<4;mod++) {
				boolean val=buff.readLine().split(" ")[1].equals("true");
				if(tabHyperModalities[mod]!=val) {this.goodTrainerWasLastUsedAndParamsHadNotMove=false;tabHyperModalities[mod]=val;}
			}
			for(int feat=0;feat<8;feat++) {
				boolean val=buff.readLine().split(" ")[1].equals("true");
				if(tabHyperFeatures[feat]!=val) {this.goodTrainerWasLastUsedAndParamsHadNotMove=false;tabHyperFeatures[feat]=val;}
			}
			boolean val=buff.readLine().split(" ")[1].equals("true");
			if(isProcessing3D!=val) {this.goodTrainerWasLastUsedAndParamsHadNotMove=false;wekasave.isProcessing3D=val; wekaSegmentation.isProcessing3D=val;}
			
			buff.close(); 
			System.out.println("Is new feature stack needed ? "+(this.goodTrainerWasLastUsedAndParamsHadNotMove ? " Not necessary, the current feature stack is ok" : "Yes, a new feature stack will be computed during the next training"));
		}		
		catch (Exception e){e.printStackTrace();}
		this.overlayLUT = new LUT(this.getColorsChannelValue(0),this.getColorsChannelValue(1),this.getColorsChannelValue(2));
		win.drawExamples();
		win.updateExampleLists();
		repaintWindow();
		existingUserColormap=true;
		Weka_Save.toggleOverlay();
		Weka_Save.toggleOverlay();
		System.out.println("Load setup : action finished");
		return;
	}

	private void saveFeatureSetup(String file) {
		String fullPath=null;
		System.out.println("Action saveFeatureSetup");
		if(file==null) {
			SaveDialog sd = new SaveDialog("Choose a place to save the setup file", "setup",".MFEATURE");
			if (sd.getFileName()==null)
				return;
			String dirName=sd.getDirectory();
			String fullName=sd.getFileName();
			fullPath=new File(dirName,fullName).getAbsolutePath();
		}
		else fullPath=file;
		PrintWriter writer=null;
		try {
			writer = new PrintWriter(fullPath, "UTF-8");
			//Ecriture des settings
			writer.println("################# SETTINGS #############");
			boolean[]tabFeat=wekaSegmentation.getEnabledFeatures();
			writer.println("Gaussian_blur "+tabFeat[0]);
			writer.println("Hessian "+tabFeat[1]);
			writer.println("Derivatives "+tabFeat[2]);
			writer.println("Laplacian "+tabFeat[3]);
			writer.println("Structure "+tabFeat[4]);
			writer.println("Edges "+tabFeat[5]);
			writer.println("Difference_of_gaussians "+tabFeat[6]);
			writer.println("Minimum "+tabFeat[7]);
			writer.println("Maximum "+tabFeat[8]);
			writer.println("Mean "+tabFeat[9]);
			writer.println("Median "+tabFeat[10]);
			writer.println("Variance "+tabFeat[11]);
			writer.println("Min_sigma "+ wekaSegmentation.getMinimumSigma()  );
			writer.println("Max_sigma "+ wekaSegmentation.getMaximumSigma()  );
			writer.println("Num_features_tree "+ ((FastRandomForest)wekaSegmentation.getClassifier()).getNumFeatures());
			writer.println("Num_trees "+  ((FastRandomForest)wekaSegmentation.getClassifier()).getNumTrees() );			
			writer.println("HYPER_use_RX: "+tabHyperModalities[0]);
			writer.println("HYPER_use_T1: "+tabHyperModalities[1]);
			writer.println("HYPER_use_T2: "+tabHyperModalities[2]);
			writer.println("HYPER_use_M0: "+tabHyperModalities[3]);
			writer.println("HYPER_FEAT_DIVISION: "+tabHyperFeatures[0]);
			writer.println("HYPER_FEAT_GAUSS: "+tabHyperFeatures[1]);
			writer.println("HYPER_FEAT_EDGES: "+tabHyperFeatures[2]);
			writer.println("HYPER_FEAT_GAUSS_DIFF: "+tabHyperFeatures[3]);
			writer.println("HYPER_FEAT_MIN: "+tabHyperFeatures[4]);
			writer.println("HYPER_FEAT_MAX: "+tabHyperFeatures[5]);
			writer.println("HYPER_FEAT_MEDIAN: "+tabHyperFeatures[6]);
			writer.println("HYPER_FEAT_VARIANCE: "+tabHyperFeatures[7]);
			writer.println("HYPER_FEAT_DERICHE: "+tabHyperFeatures[4]);
			writer.println("HYPER_FEAT_NONE1: "+tabHyperFeatures[5]);
			writer.println("HYPER_FEAT_NONE2: "+tabHyperFeatures[6]);
			writer.println("HYPER_FEAT_NONE3: "+tabHyperFeatures[7]);
			writer.println("Processing_3D: "+isProcessing3D);


			writer.close();	
		} catch (FileNotFoundException | UnsupportedEncodingException e) {e.printStackTrace();}
	}



	private void loadClassSetup(String file) {
		String fullPath=null;
		System.out.println("Action loadClassSetup");
		if(file==null) {
			OpenDialog od=new OpenDialog("Choose a setup file for your classes","","setup.MCLASS");
			this.existingUserColormap=true;
			if (od.getFileName()==null)
				return;
			String dirName=od.getDirectory();
			String fullName=od.getFileName();
			fullPath=new File(dirName,fullName).getAbsolutePath();
		}
		else fullPath=file;
		System.out.println("Loading setup from "+fullPath);
		try{
			InputStream flux=new FileInputStream(new File(fullPath)); 
			InputStreamReader read=new InputStreamReader(flux);
			BufferedReader buff=new BufferedReader(read);

			//Lecture des classes et couleurs
			buff.readLine();//"##### ################## CLASSES #############"
			int nClas=Integer.parseInt(buff.readLine());
			System.out.println("Nombre de classes lues="+nClas);
			if(nClas<numOfClasses) {
				IJ.log("This won't go very well like this, because you want to load a model with a number of class lower than the actual one, what will crash the inteface. What you should do is to close the interface, and open it again. This command abort now to prevent this bad behaviour");
				buff.close();
				return;
			}

			for(int i=0;i<nClas;i++) {
				if(this.numOfClasses<i+1)this.addNewClass(buff.readLine());
				else Weka_Save.changeClassName(""+i+"",buff.readLine() );
				int r=Integer.parseInt(buff.readLine());
				int g=Integer.parseInt(buff.readLine());
				int b=Integer.parseInt(buff.readLine());
				if(r>=0) {
					this.colors[i]=new Color(r,g,b);
					this.isCustomColor[i]=true;
				}
				else {
					this.colorChoices[i]=(-r-1);
					this.colors[i]=this.colorsTab[this.colorChoices[i]];
					this.isCustomColor[i]=false;
				}
				exampleList[i].setForeground(colors[i]);
			}

			buff.close(); 
		}		
		catch (Exception e){e.printStackTrace();}
		this.overlayLUT = new LUT(this.getColorsChannelValue(0),this.getColorsChannelValue(1),this.getColorsChannelValue(2));
		win.drawExamples();
		win.updateExampleLists();
		repaintWindow();
		existingUserColormap=true;
		Weka_Save.toggleOverlay();
		Weka_Save.toggleOverlay();
		System.out.println("Load setup : action finished");
		return;
	}

	private void saveClassSetup(String file) {
		String fullPath=null;
		System.out.println("Action saveClassSetup");
		if(file==null) {
			SaveDialog sd = new SaveDialog("Choose a place to save the setup file", "setup",".MCLASS");
			if (sd.getFileName()==null)
				return;
			String dirName=sd.getDirectory();
			String fullName=sd.getFileName();
			fullPath=new File(dirName,fullName).getAbsolutePath();
		}
		else fullPath=file;
		PrintWriter writer=null;
		try {
			writer = new PrintWriter(fullPath, "UTF-8");

			//Ecriture des classes et de leurs couleurs respectives
			writer.println("################## CLASSES #############");
			writer.println(this.numOfClasses);
			for(int cl=0;cl<this.numOfClasses;cl++) {
				System.out.println("Ecriture de la classe"+cl);
				writer.println(this.wekaSegmentation.getClassLabel(cl));
				if(this.isCustomColor[cl]) {
					writer.println(this.colors[cl].getRed());
					writer.println(this.colors[cl].getGreen());
					writer.println(this.colors[cl].getBlue());
				}
				else {
					writer.println(-1-this.colorChoices[cl]);
					writer.println(-1-this.colorChoices[cl]);
					writer.println(-1-this.colorChoices[cl]);
				}
			}

			writer.close();	
		} catch (FileNotFoundException | UnsupportedEncodingException e) {e.printStackTrace();}
	}


	private void manageColors() {
		System.out.println("Action manageColors, avec userColormap"+this.existingUserColormap);
		GenericDialog gd= new GenericDialog("Select colors");
		for(int i=0;i<this.numOfClasses;i++) {
			//			System.out.println("phase 1 : traitement classe num"+i+" et booleen="+this.isCustomColor[i]);
			String[]choices=new String[colorsStr.length+(this.isCustomColor[i]?1:0)];
			for(int j=0;j<colorsStr.length;j++) {choices[!this.isCustomColor[i] ? j : j+1]=colorsStr[j];}
			if(this.isCustomColor[i]) choices[0]="custom("+this.colors[i].getRed()+" , "+this.colors[i].getGreen()+" , "+this.colors[i].getBlue()+")";
			if(! this.existingUserColormap) {
				gd.addChoice("Classe "+(i+1)+" ("+this.wekaSegmentation.getClassLabel(i)+") : ",choices,choices[i]);
			}
			else{
				if(this.isCustomColor[i]) {
					gd.addChoice("Classe "+(i+1)+" ("+this.wekaSegmentation.getClassLabel(i)+") : ",choices,choices[0]);
				}
				else{
					gd.addChoice("Classe "+(i+1)+" ("+this.wekaSegmentation.getClassLabel(i)+") : ",choices,choices[colorChoices[i]]);
				}					
			}
		}
		gd.showDialog();
		if (gd.wasCanceled()) return;	  

		for(int i=0;i<this.numOfClasses;i++) {
			this.colorChoices[i]=gd.getNextChoiceIndex();
			System.out.println("phase 2 : traitement classe num"+i+" choix effectue="+this.colorChoices[i]+" et booleen="+this.isCustomColor[i]);
			if(!this.isCustomColor[i] && this.colorChoices[i]<13)this.colors[i]=colorsTab[this.colorChoices[i]];
			else if(!this.isCustomColor[i] && this.colorChoices[i]==13) {
				this.isCustomColor[i]=true;
				this.colors[i] = JColorChooser.showDialog(null, "Color choice for classe "+(i+1)+" ("+this.wekaSegmentation.getClassLabel(i)+") : ", Color.WHITE);
			}
			else if( this.isCustomColor[i] && this.colorChoices[i]==0) {
				this.colors[i]=this.colors[i];//Actually, do nothing
			}
			else if( this.isCustomColor[i] && this.colorChoices[i]<14) {
				System.out.println("Et pourtant on passe la , à i="+i+" et this.colorChoices="+this.colorChoices[i]);
				this.colorChoices[i]=this.colorChoices[i]-1;
				this.colors[i]=this.colorsTab[this.colorChoices[i]];
				this.isCustomColor[i]=false;
			}
			else if( this.isCustomColor[i] && this.colorChoices[i]==14) {
				this.colors[i] = JColorChooser.showDialog(null, "Color choice for classe "+(i+1)+" ("+this.wekaSegmentation.getClassLabel(i)+") : ", Color.WHITE);
			}
			exampleList[i].setForeground(colors[i]);
		}

		this.overlayLUT = new LUT(this.getColorsChannelValue(0),this.getColorsChannelValue(1),this.getColorsChannelValue(2));
		win.drawExamples();
		win.updateExampleLists();
		repaintWindow();
		existingUserColormap=true;
		System.out.println("Colormap : action finished");
		Weka_Save.toggleOverlay();
		Weka_Save.toggleOverlay();
		return;
	}



	public byte[] getColorsChannelValue(int ch) {
		if(ch<0 || ch>2)return null;
		else {
			byte[]tab=new byte[256];
			for(int i=0;i<this.numOfClasses;i++) {
				tab[i]= (byte) (  (   (ch==0)  ? this.colors[i].getRed() : (ch==1)  ? this.colors[i].getGreen() : this.colors[i].getBlue() ) & 0xff); 
			}
			return tab;
		}
	}

	public String lutToString() {
		String ret="";
		byte[] vals=this.overlayLUT.getBytes();
		for(int i=0;i<this.numOfClasses;i++) {
			ret+=(0xff & vals[i])+" , "+(0xff & vals[256+i]) +" , "+(0xff &vals[512+i])+"\n"; 
		}	
		return ret;
	}


	public void debugInfo() {
		System.out.println("\nAffichage d'informations pour le debug");
		System.out.println(lutToString());
	}


	public static int[]getSigmaTab(int sigMin,int sigMax) {
		int sigg=sigMin;int nbSigma=0;
		while(sigg<=sigMax) {nbSigma++;sigg*=2;}
		final int[]sigmas=new int[nbSigma+1];
		final int nSig=nbSigma;
		sigmas[0]=0;
		sigmas[1]=sigMin;
		for(int i=2;i<nbSigma+1;i++) {
			sigmas[i]=sigmas[i-1]*2;
		}
		return sigmas;
	}


	public static int computeNbFeat(int C,int nbSigma,boolean[]features) {
		int numFeat=C;//each channel
		if(features[0]) {
			System.out.println("Division entre canaux. Ajoute "+(C*(C-1))*nbSigma+" calculs par slice");
			numFeat+=(C*(C-1))*nbSigma;//the division between channels at sigma=1, sigma=4, sigma=10
		}
		if(features[1]) {
			System.out.println("Lissage gaussien. Ajoute "+(C*nbSigma)+" calculs par slice");
			numFeat+=C*nbSigma;//Gaussian blur
		}
		if(features[2]) {
			System.out.println("Detection de bordures. Ajoute "+(C)+" calculs par slice");
			numFeat+=C;//Edges
		}
		if(features[3]) {
			System.out.println("Difference de gaussiennes de tailles successives. Ajoute "+(C*nbSigma)+" calculs par slice");
			numFeat+=C*nbSigma;//Difference of gaussian
		}
		if(features[4]) {
			System.out.println("Minimum dans un voisinage local de rayon sigma. Ajoute "+(C*nbSigma)+" calculs par slice");
			numFeat+=C*nbSigma;//Minimum
		}
		if(features[5]) {
			System.out.println("Maximum dans un voisinage local de rayon sigma. Ajoute "+(C*nbSigma)+" calculs par slice");
			numFeat+=C*nbSigma;//Maximum
		}
		if(features[6]) {
			System.out.println("Mediane dans un voisinage local de rayon sigma. Ajoute "+(C*nbSigma)+" calculs par slice");
			numFeat+=C*nbSigma;//Median
		}
		if(features[7]) {
			System.out.println("Variance dans un voisinage local de rayon sigma. Ajoute "+(C*nbSigma)+" calculs par slice");
			numFeat+=C*nbSigma;//Variance
		}
		if(features[8]) {
			System.out.println("Filtre gradient de canny deriche. Ajoute "+(C*nbSigma)+" calculs par slice");
			numFeat+=C*nbSigma;//Variance
		}
		return numFeat;
	}		

	public static FeatureStack[] FeatureStackArrayToFeatureStackTab(FeatureStackArray fsa) {
		FeatureStack[]tab=new FeatureStack[fsa.getSize()];
		for(int n=0;n<fsa.getSize();n++)tab[n]=fsa.get(n);
		return tab;
	}


	public static FeatureStackArray buildFeatureStackArrayRGBSeparatedMultiThreadedV2(ImagePlus []img,boolean []features,final int sigMin,final int sigMax) {
		int nbProc= VitimageUtils.getNbCores();
//		if(img[0].getWidth()<100)nbProc/=2;
		int sigg=sigMin;int nbSigma=0;
		while(sigg<=sigMax) {nbSigma++;sigg*=2;}
		final int[]sigmas=new int[nbSigma];
		final int nSig=nbSigma;
		sigmas[0]=sigMin;
		for(int i=1;i<nbSigma;i++) {
			sigmas[i]=sigmas[i-1]*2;
		}

		final int X=img[0].getWidth();
		final int Y=img[0].getHeight();
		final int Z=img[0].getStackSize();
		final int C=img.length;
		System.out.println("Construction feature stack avec sigma successifs = "+TransformUtils.stringVectorN(sigmas, ""));
		int numFeat=computeNbFeat(C,nbSigma,features);
		final int nbFeat=numFeat;
		final long t0=System.currentTimeMillis();

		final int nbTotalCalculus=numFeat*Z;
		FeatureStackArray featuresArray = new FeatureStackArray(img[0].getStackSize(), 1, 128, false,1, 1, null);
		System.out.println("Nb features total = "+numFeat);
		System.out.println("Nb operations total programmees = "+nbTotalCalculus);
		final int eachInt=(nbTotalCalculus/300)*3;
		AtomicInteger counter=new AtomicInteger(0);
		AtomicInteger countPercent=new AtomicInteger(0);
		AtomicInteger atomNumThread=new AtomicInteger(0);
		System.out.println("Building structures for each of the "+(nbProc-1)+" threads");
		int nThread=nbProc-1;
		int[]tabSendToThread=new int[Z];
		int[]placeIntoThread=new int[Z];
		int[]incrPlacesThread=new int[nThread];
		int[][]tabSlicesOfEachThread=new int[nThread][];
		for(int z=0;z<Z;z++) {
			tabSendToThread[z]=z%nThread;
			placeIntoThread[z]=incrPlacesThread[z%nThread]++;
		}
		for(int nt=0;nt<nThread;nt++) {
			tabSlicesOfEachThread[nt]=new int[incrPlacesThread[nt]];
			incrPlacesThread[nt]=0;
		}
		for(int z=0;z<Z;z++) {
			placeIntoThread[z]=incrPlacesThread[z%nThread];
			tabSlicesOfEachThread[z%nThread][incrPlacesThread[z%nThread]++]=z;
		}

		System.out.println("Recap attributions slices pour chaque coeur : ");
		for(int nt=0;nt<nThread;nt++) {
			System.out.print("Coeur numero "+nt+" doit traiter "+tabSlicesOfEachThread[nt].length+" slices : ");
			for(int i=0;i<tabSlicesOfEachThread[nt].length;i++)System.out.print("  "+tabSlicesOfEachThread[nt][i]+"  ");
			System.out.println();
		}

		final ImagePlus [][][]imgSlices=new ImagePlus[nThread][][];
		for(int nt=0;nt<nThread;nt++) {
			imgSlices[nt]=new ImagePlus[tabSlicesOfEachThread[nt].length][img.length];
			for(int isl=0;isl<tabSlicesOfEachThread[nt].length;isl++) {
				for(int c=0;c<C;c++) {img[c].setSlice(tabSlicesOfEachThread[nt][isl]+1);imgSlices[nt][isl][c]=img[c].crop();}
			}
		}
		final ImageStack []imgStack=new ImageStack[Z];
		final Thread[] threads = VitimageUtils.newThreadArray(nThread);  
		for (int ithread = 0; ithread < nThread; ithread++) {  
			threads[ithread] = new Thread() {  { setPriority(Thread.NORM_PRIORITY); }  
			public void run() {
				int nt=atomNumThread.getAndIncrement();
				int localCount=0;
				String []can=new String[C];
				int nZ=tabSlicesOfEachThread[nt].length;
				for(int c=0;c<C;c++)can[c]="_Can"+c;
				for(int z=0;z<nZ;z++) {				
					int zz=tabSlicesOfEachThread[nt][z];
					ImagePlus []init=new ImagePlus[C];
					for(int c=0;c<C;c++)init[c]=VitimageUtils.imageCopy(imgSlices[nt][z][c]);
					ImagePlus temp;
					ImagePlus temp2;
					ImageStack stack = new ImageStack(X,Y);


					///////// LES CANAUX INITIAUX
					System.out.print(" canaux"+zz);
					for(int  c=0;c<C;c++) {
						temp=VitimageUtils.imageCopy(init[c]);stack.addSlice("Init"+can[c]+"_SIG0",temp.getStack().getProcessor(1));
					}
					localCount=counter.addAndGet(C);
					if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
						long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
						System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
								dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
						IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
								dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
						IJ.showProgress(countPercent.get()/100.0);
					}



					///////// GAUSSIENNES
					if(features[1]|| features[3] || features[0]) {
						ImagePlus [][]tabG=new ImagePlus[sigmas.length+1][C];
						System.out.print(" gauss"+zz);
						for(int c=0;c<C;c++)tabG[0][c]=VitimageUtils.imageCopy(init[c]);//Copie pour la div de gaussiennes
						for(int isig =0;isig<sigmas.length;isig++) {				
							for(int c=0;c<C;c++) {
								tabG[isig+1][c]=VitimageUtils.imageCopy(init[c]);
								IJ.run(tabG[isig+1][c], "Gaussian Blur...", "radius="+sigmas[isig]);
								if(features[1])stack.addSlice("-Gauss-"+can[c]+"_SIG"+sigmas[isig],tabG[isig+1][c].getStack().getProcessor(1));
							}
						}
						if(features[1])localCount=counter.addAndGet(C*nSig);
						if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
							long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent.get()/100.0);
						}

						///////// DIFFERENCE DE GAUSSIENNES
						if(features[3]) {
							System.out.print(" diffG"+zz);
							for(int isig =0;isig<sigmas.length;isig++) {
								for(int c=0;c<C;c++) {
									temp=VitimageUtils.makeOperationBetweenTwoImages(tabG[isig][c], tabG[isig+1][c], VitimageUtils.OP_SUB,true);temp.setDisplayRange(-128, 128);IJ.run(temp,"8-bit","");
									stack.addSlice("DiffGauss"+can[c]+"_SIG"+sigmas[isig],temp.getStack().getProcessor(1));
								}
							}
							localCount=counter.addAndGet(C*nSig);
							if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
								long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
								System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
										dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
								IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
										dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
								IJ.showProgress(countPercent.get()/100.0);
							}
						}
						///////// DIVISIONS ENTRE CANAUX
						if(features[0]) { 
							System.out.print(" GdivG"+zz);
							for(int i=0;i<sigmas.length;i++) {
								for(int c1=0;c1<C;c1++) {
									for(int c2=0;c2<C;c2++) {
										if(c1 != c2) {
											temp=VitimageUtils.makeOperationBetweenTwoImages(tabG[i+1][c1],tabG[i+1][c2], VitimageUtils.OP_DIV,true);IJ.run(temp, "Log", "");temp.setDisplayRange(-3, 3);IJ.run(temp,"8-bit","");
											stack.addSlice(can[c1]+"DivGauss"+can[c2]+"_SIG"+sigmas[i],temp.getStack().getProcessor(1));
										}
									}
								}
							}


							localCount=counter.addAndGet(C*(C-1)*nSig);
							if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
								long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
								System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
										dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
								IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
										dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
								IJ.showProgress(countPercent.get()/100.0);
							}
						}				
					}




					///////// EDGES
					if(features[2]) {
						System.out.print(" edges"+zz);
						for(int c=0;c<C;c++) {
							temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Find Edges", "");stack.addSlice("Edges"+can[c]+"_SIG0",temp.getStack().getProcessor(1));
						}
						localCount=counter.addAndGet(C);
						if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
							long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent.get()/100.0);
						}
					}






					///////// MEDIAN
					if(features[6]) {  //med
						System.out.print(" median"+zz);
						for(int sig : sigmas) {
							for(int c=0;c<C;c++) {
								temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Median...", "radius="+sig);stack.addSlice("Med"+can[c]+"_SIG"+sig,temp.getStack().getProcessor(1));
							}
						}
						localCount=counter.addAndGet(C*nSig);
						if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
							long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent.get()/100.0);
						}
					}


					///////// VARIANCE
					if(features[7]) {  //var
						System.out.print(" variance"+zz);
						for(int sig : sigmas) {
							for(int c=0;c<C;c++) {
								temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp,"32-bit","");IJ.run(temp, "Variance...", "radius="+sig);
								temp.setDisplayRange(0, 3000);IJ.run(temp,"8-bit","");stack.addSlice("Var"+can[c]+"_SIG"+sig,temp.getStack().getProcessor(1));
							}
						}
						localCount=counter.addAndGet(C*nSig);
						if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
							long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent.get()/100.0);
						}
					}


					///////// MINIMUM
					if(features[4]) { //min
						System.out.print(" min"+zz);
						for(int sig : sigmas) {
							for(int c=0;c<C;c++) {
								temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Minimum...", "radius="+sig);stack.addSlice("Min"+can[c]+"_SIG"+sig,temp.getStack().getProcessor(1));
							}
						}
						localCount=counter.addAndGet(C*nSig);
						if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
							long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent.get()/100.0);
						}
					}



					///////// MAXIMUM
					if(features[5]) {  //max
						System.out.print(" max"+zz);
						for(int sig : sigmas) {
							for(int c=0;c<C;c++) {
								temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Maximum...", "radius="+sig);stack.addSlice("Max"+can[c]+"_SIG"+sig,temp.getStack().getProcessor(1));
							}
						}
						localCount=counter.addAndGet(C*nSig);
						if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
							long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent.get()/100.0);
						}
					}

					///////// DERICHE
					if(features[8]) {  //max
						System.out.print(" deriche"+zz);
						for(int sig : sigmas) {
							for(int c=0;c<C;c++) {
								temp=VitimageUtils.cannyDericheGradient(init[c],sig);stack.addSlice("Deriche"+can[c]+"_SIG"+sig,temp.getStack().getProcessor(1));
							}
						}
						localCount=counter.addAndGet(C*nSig);
						if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
							long t1=System.currentTimeMillis();double dt=VitimageUtils.dou((t1-t0)/1000.0);double dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent.get()/100.0);
						}
					}




					imgStack[zz]=stack;
				}
			} //fin run
			};
		}

		VitimageUtils.startAndJoin(threads);  
		System.out.println("Feature stack ok.\n\n\n");
		for(int z=0;z<Z;z++) {
			FeatureStack fs=new FeatureStack(X,Y,false);
			fs.setStack(imgStack[z]);
			featuresArray.set(fs, z);
			featuresArray.setEnabledFeatures(fs.getEnabledFeatures());
		}
		return featuresArray;
	}


	
	


	
	public static FeatureStackArray convertStacksToFeatureStackArray(ImageStack[]stacks,int sigMin,int sigMax) {
		final int X=stacks[0].getWidth();
		final int Y=stacks[0].getHeight();
		final int Z=stacks[0].getSize();
		FeatureStackArray fts=new FeatureStackArray(Z,sigMin,sigMax,false,1,1,null);
		for(int z=0;z<stacks[0].getSize();z++) {
			ImageStack stack = new ImageStack(X,Y);
			for(int f=0;f<stacks.length;f++) {
				stack.addSlice(stacks[f].getSliceLabel(1), stacks[f].getProcessor(z+1));
			}  
			FeatureStack fs=new FeatureStack(X,Y,false);
			fs.setStack(stack);
			fts.set(fs, z);
		}
		return fts;	
	}

	

	
	
	
	
	public static FeatureStackArray buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(
			final ImagePlus []init,final boolean []features,int sigMin,int sigMax) {
		long t0=System.currentTimeMillis();
		long t1=0;
		double dt=0;
		double dT=0;
		int sigg=sigMin;int nbSigma=0;
		while(sigg<=sigMax) {nbSigma++;sigg*=2;}
		final int[]sigmas=new int[nbSigma];
		final int nSig=nbSigma;
		sigmas[0]=sigMin;
		for(int i=1;i<nbSigma;i++) {
			sigmas[i]=sigmas[i-1]*2;
		}
		int nSigma=sigmas.length;
		final int X=init[0].getWidth();
		final int Y=init[0].getHeight();
		final int Z=init[0].getStackSize();
		final int C=init.length;
		System.out.println("Construction feature stack avec sigma successifs = "+TransformUtils.stringVectorN(sigmas, ""));
		int numFeat=computeNbFeat(C,nSigma,features);
		final int nbFeat=numFeat;

		final int eachInt=((numFeat)/300)*3;
		int localCount=0;
		int countPercent=0;
		String []can=new String[C];
		for(int c=0;c<C;c++)can[c]="_Can"+c;

		int incr=0;
		final ImageStack []imgStack=new ImageStack[numFeat];
		ImagePlus temp;
		ImagePlus temp2;

		///////// LES CANAUX INITIAUX
		for(int  c=0;c<C;c++) {
			System.out.print(" canaux "+c);
			imgStack[incr++]=VitimageUtils.imageCopy(init[c],"Init"+can[c]+"_SIG0").getStack();
			localCount++;
			if(((localCount*1000)/(numFeat)>countPercent)) {
				t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
				System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
				dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
				IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
				dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
				IJ.showProgress(countPercent/1000.0);
			}
		}

		

		///////// MEDIAN
		if(features[6]) {  //med
			
			for(int sig : sigmas) {
				for(int c=0;c<C;c++) {    
					System.out.print(" median "+sig+" "+c);// IJ.run(temp,"3D Fast Filters","filter=Median radius_x_pix="+sig+" radius_y_pix="+sig+" radius_z_pix="+sig+" Nb_cpus=12");
					temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Median 3D...","x="+sig+" y="+sig+" z="+sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Med"+can[c]+"_SIG"+sig).getStack();
					localCount++;
					if(((localCount*1000)/(numFeat)>countPercent)) {
						t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
						System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
						IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
						IJ.showProgress(countPercent/1000.0);
					}
				}
			}
		}



		///////// GAUSSIENNES
		if(features[1]|| features[3] || features[0]) {
			ImagePlus [][]tabG=new ImagePlus[sigmas.length+1][C];
			for(int c=0;c<C;c++)tabG[0][c]=VitimageUtils.imageCopy(init[c]);//Copie pour la div de gaussiennes
			for(int isig =0;isig<sigmas.length;isig++) {				
				for(int c=0;c<C;c++) {
					System.out.print(" gauss "+sigmas[isig]+" "+c);
					tabG[isig+1][c]=VitimageUtils.imageCopy(init[c],"-Gauss-"+can[c]+"_SIG"+sigmas[isig]);
					IJ.run(tabG[isig+1][c], "Gaussian Blur 3D...", "x="+sigmas[isig]+" y="+sigmas[isig]+" z="+sigmas[isig]);
					if(features[1])imgStack[incr++]=tabG[isig+1][c].getStack();
					localCount++;
					if(((localCount*1000)/(numFeat)>countPercent)) {
						t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
						System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
						IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
						IJ.showProgress(countPercent/1000.0);
					}
				}
			}
			if(features[1])		

			///////// DIFFERENCE DE GAUSSIENNES
			if(features[3]) {
				for(int isig =0;isig<sigmas.length;isig++) {
					for(int c=0;c<C;c++) {
						System.out.print(" diffG "+sigmas[isig]+" "+c);
						temp=VitimageUtils.makeOperationBetweenTwoImages(tabG[isig][c], tabG[isig+1][c], VitimageUtils.OP_SUB,true);temp.setDisplayRange(-128, 128);IJ.run(temp,"8-bit","");
						imgStack[incr++]=VitimageUtils.imageCopy(temp,"DiffGauss"+can[c]+"_SIG"+sigmas[isig]).getStack();
						localCount++;
						if(((localCount*1000)/(numFeat)>countPercent)) {
							t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
							System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
							dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
							IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
							dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
							IJ.showProgress(countPercent/1000.0);
						}
					}
				}
			}
			///////// DIVISIONS ENTRE CANAUX
			if(features[0]) { 
				for(int i=0;i<sigmas.length;i++) {
					for(int c1=0;c1<C;c1++) {
						for(int c2=0;c2<C;c2++) {
							if(c1 != c2) {
								System.out.print(" GdivG "+sigmas[i]+" "+c1+"/"+c2);								
								temp=VitimageUtils.makeOperationBetweenTwoImages(tabG[i+1][c1],tabG[i+1][c2], VitimageUtils.OP_DIV,true);IJ.run(temp, "Log", "stack");temp.setDisplayRange(-3, 3);IJ.run(temp,"8-bit","");
								imgStack[incr++]=VitimageUtils.imageCopy(temp,can[c1]+"DivGauss"+can[c2]+"_SIG"+sigmas[i]).getStack();
								localCount++;
								if(((localCount*1000)/(numFeat)>countPercent)) {
									t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
									System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
									IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
									dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
									IJ.showProgress(countPercent/1000.0);
								}
							}
						}
					}
				}


			}				
		}




		///////// EDGES
		if(features[2]) {
			System.out.print(" edges2D");
			for(int c=0;c<C;c++) {
				System.out.print(" edges2D "+c);
				temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Find Edges", "stack");imgStack[incr++]=VitimageUtils.imageCopy(temp,"Edges"+can[c]+"_SIG0").getStack();
				localCount++;
				if(((localCount*1000)/(numFeat)>countPercent)) {
					t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
					System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
					IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
					IJ.showProgress(countPercent/1000.0);
				}
			}
		}




		
		///////// VARIANCE
		if(features[7]) {  //var
			System.out.print(" variance");
			for(int sig : sigmas) {
				for(int c=0;c<C;c++) {
					System.out.print(" variance "+sig+" "+c);
					temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp,"32-bit","");IJ.run(temp, "Variance 3D...", "x="+sig+" y="+sig+" z="+sig);
					temp.setDisplayRange(0, 3000);IJ.run(temp,"8-bit","");imgStack[incr++]=VitimageUtils.imageCopy(temp,"Var"+can[c]+"_SIG"+sig).getStack();
					localCount++;
					if(((localCount*1000)/(numFeat)>countPercent)) {
						t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
						System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
						IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
						IJ.showProgress(countPercent/1000.0);
					}
				}
			}
		}


		///////// MINIMUM
		if(features[4]) { //min
			System.out.print(" min");
			for(int sig : sigmas) {
				for(int c=0;c<C;c++) {
					System.out.print(" min "+sig+" "+c);
					temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Minimum 3D...", "x="+sig+" y="+sig+" z="+sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Min"+can[c]+"_SIG"+sig).getStack();
					localCount++;
					if(((localCount*1000)/(numFeat)>countPercent)) {
						t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
						System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
						IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
						IJ.showProgress(countPercent/1000.0);
					}
				}
			}
		}



		///////// MAXIMUM
		if(features[5]) {  //max
			System.out.print(" max");
			for(int sig : sigmas) {
				for(int c=0;c<C;c++) {
					System.out.print(" max "+sig+" "+c);
					temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Maximum 3D...", "x="+sig+" y="+sig+" z="+sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Max"+can[c]+"_SIG"+sig).getStack();
					localCount++;
					if(((localCount*1000)/(numFeat)>countPercent)) {
						t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
						System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
						IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
						IJ.showProgress(countPercent/1000.0);
					}
				}
			}
		}
		
		///////// DERICHE
		if(features[8]) {  //max
			System.out.print(" deriche");
			for(int sig : sigmas) {
				for(int c=0;c<C;c++) {
					System.out.print(" deriche "+sig+" "+c);
					temp=VitimageUtils.edges3DByte(init[c],sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Deriche"+can[c]+"_SIG"+sig).getStack();
					localCount++;
					if(((localCount*1000)/(numFeat)>countPercent)) {
						t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(numFeat-localCount)/localCount);
						System.out.print("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent++)+" %0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
						IJ.log("\n# Calculs effectues : "+localCount+" / "+numFeat+" ("+(countPercent)+"%0) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
						IJ.showProgress(countPercent/100.0);
					}
				}
			}
		}
		System.out.println("Feature stack of specimen "+" ok.");
		return convertStacksToFeatureStackArray(imgStack,sigMin,sigMax);
	}
	
	
	
	
	
	

	
	public ImagePlus convertImageAndExamplesToLowerXYResolution(double factor,String fileImgInPath,String fileExamplesSource,String outExamplesPath,String outImgPath,boolean onlyConsensusExamples) {
		//Images handling
		System.out.println("Starting conversion of image and examples, with size factor="+factor);
		System.out.println("Images and files handling");
		String fileEx=fileExamplesSource;
		String repMainOut="/home/fernandr/Bureau/EX_CEDRIC/SUBS/";
		int fact=(int)Math.round(1.0/factor);
		String repExOut=repMainOut+"SUB_"+fact+"/EXAMPLES/";
		new File(repExOut).mkdirs();
		String fileImgOut=repMainOut+"SUB_"+fact+"/imgHybrid.tif";

		ImagePlus imgHyper=IJ.openImage(fileImgInPath);
		ImagePlus[]hyperTabNew=VitimageUtils.stacksFromHyperstackFast(imgHyper,9);
		System.out.println("Ouverture de "+fileImgInPath+" de dims "+TransformUtils.stringVector(VitimageUtils.getDimensions(hyperTabNew[0]), ""));

		ImagePlus img=VitimageUtils.imageCopy(hyperTabNew[0]);
		int[]dims=VitimageUtils.getDimensions(img);
		double[]voxs=VitimageUtils.getVoxelSizes(img);
		int X=dims[0];		int Y=dims[1];		int Z=dims[2];

		//Make subsampling with nearest
		System.out.println("Subsampling hyperTab");
		for(int i=0;i<hyperTabNew.length;i++) {
			System.out.print("Processing modality "+i+" "+TransformUtils.stringVector(VitimageUtils.getDimensions(hyperTabNew[i])," dims avant="));
			hyperTabNew[i]=VitimageUtils.subXYZ(hyperTabNew[i],new double[] {factor,factor,1},true);
			System.out.println(" "+TransformUtils.stringVector(VitimageUtils.getDimensions(hyperTabNew[i])," dims apres="));
		}
		int Ns=hyperTabNew[0].getStackSize();
		ImagePlus imgNew=VitimageUtils.imageCopy(hyperTabNew[0]);
		System.out.println("Concatenating hyperTab with Ns="+Ns);
		ImagePlus retHybrid=Concatenator.run(hyperTabNew);
		IJ.run(retHybrid,"Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+Ns+" frames=9 display=Grayscale");
		System.out.println("Ok.");
		
		int[]dimsNew=VitimageUtils.getDimensions(imgNew);
		double[]voxsNew=VitimageUtils.getVoxelSizes(imgNew);
		int Xnew=dimsNew[0];		int Ynew=dimsNew[1];		int Znew=dimsNew[2];
		
		
		//Examples handling
		System.out.println("Loading old examples");
		String[]classLabels=this.wekaSegmentation.getClassLabels();
		int[][]examplesPoints=loadExamplesFromFile(fileEx,0,null,null,null);
		
		//Could be done with linked list
		//Gather examples in old geometry, and compute their new coordinates in subsampled image
		System.out.println("Attributing new coordinates to "+examplesPoints.length+" old examples on a grid of "+Xnew+" x "+Ynew+" x "+Znew);
		int[][][][]listVals=new int[Xnew][Ynew][Znew][];
		int[][][]examplesNew=new int[Xnew][Ynew][Znew];
		for(int pt=0;pt<examplesPoints.length;pt++) {
			int x=examplesPoints[pt][0];
			int y=examplesPoints[pt][1];
			int z=examplesPoints[pt][2]-1;
			int cl=examplesPoints[pt][3];

	         /*        
            Ancienne coord    coord attendue apres sub(2)             Anc.coord+(vold-vNew)/2          resultat*vOld/vNew
  			0 devient 0      		    -0.25  						  -0.5   						  -0.25
			1 devient 0					0.25  						   0.5    						  0.25
			2 devient 1					0.75						     1.5                           0.75 
			3 devient 1					1.25     						2.5  						    1.25
			
			                 
            Ancienne coord    coord attendue apres sub(4)             Anc.coord+(vold-vNew)/2          resultat*vOld/vNew
			0 devient 			-0.375    								-1.5   							ok
			1 devient			 -.125  							   -0.5   							ok
			2 devient 			0.125    								  0.5   						ok
			3 devient 			0.375    								  1.5   						ok
        */

			int xNew=(int)Math.round( (x+((voxs[0]-voxsNew[0])/2))*voxs[0]/voxsNew[0]); //a dessiner
			int yNew=(int)Math.round( (y+((voxs[1]-voxsNew[1])/2))*voxs[1]/voxsNew[1]); //a dessiner
			int zNew=(int)Math.round( (z+((voxs[2]-voxsNew[2])/2))*voxs[2]/voxsNew[2]); //a dessiner
			//System.out.println("from "+x+","+y+","+z+" to "+xNew+","+yNew+","+zNew);
			int Nt=(listVals[xNew][yNew][zNew]==null) ? 0 : listVals[xNew][yNew][zNew].length;
			int[]tab=new int[Nt+1];
			for(int t=0;t<Nt;t++)tab[t]=listVals[xNew][yNew][zNew][t];
			tab[Nt]=cl;
			listVals[xNew][yNew][zNew]=tab;
			//System.out.println("Did pt="+pt+"/"+examplesPoints.length);
		}
		
		System.out.println("Attributing most represented class for each voxel");
		//For each future voxel, select the most represented class over its surface
		for(int xNew=0;xNew<Xnew;xNew++) {
			for(int yNew=0;yNew<Ynew;yNew++) {
				for(int zNew=0;zNew<Znew;zNew++) {
					//Determiner la classe la plus courante
					if(listVals[xNew][yNew][zNew]==null) {
						examplesNew[xNew][yNew][zNew]=-1;
						continue;
					}
					int[]tabNb=new int[5];
					for(int t=0;t<listVals[xNew][yNew][zNew].length;t++) tabNb[listVals[xNew][yNew][zNew][t]]++;
					examplesNew[xNew][yNew][zNew]=VitimageUtils.indmax(tabNb);
					if(onlyConsensusExamples && nombreNonNuls(tabNb)>1) {
						examplesNew[xNew][yNew][zNew]=-1;//			System.out.println("Processed  "+xNew+","+yNew+","+zNew+" dote de la liste "+TransformUtils.stringVectorN(listVals[xNew][yNew][zNew], "classe choisie = "+examplesNew[xNew][yNew][zNew]));
					}
				}
			}
		}

		//Build array of PointRoi to be exported, with respect to slices and class
		System.out.println("Building point rois for each class and slice");
		PointRoi [][]tabRoi=new PointRoi[5][Znew];
		int[]nbEnd=new int[5];
		for(int zNew=0;zNew<Znew;zNew++) {
			for(int cl=0;cl<5;cl++) {
				tabRoi[cl][zNew]=new PointRoi();
				for(int yNew=0;yNew<Ynew;yNew++) {
					for(int xNew=0;xNew<Xnew;xNew++) {
						if(examplesNew[xNew][yNew][zNew]==cl) {
							nbEnd[cl]++;
							tabRoi[cl][zNew].addPoint(xNew, yNew);
						}
					}
				}
			}
		}
		System.out.println("Set final apres subsampling :");
		for(int cl=0;cl<5;cl++)System.out.println("   -> Examples sub classe "+cl+" #Total="+nbEnd[cl]);
		System.out.println("Writing examples in "+outExamplesPath);
		Weka_Save.saveExamplesInFile(tabRoi,imgNew,classLabels,outExamplesPath);
		System.out.println("Writing imgHybridnew in "+outImgPath);
		IJ.saveAsTiff(retHybrid, outImgPath);
		return retHybrid;
	}
	
	
	
	
	
	public static int nombreNonNuls(int[]tab) {
		int incr=0;
		for(int i=0;i<tab.length;i++)if(tab[i]!=0)incr++;
		return incr;
	}
	
	
	
	
	
}










/*
public static FeatureStackArray buildFeatureStackArrayRGBSeparatedMultiThreadedV2Processing3D(ImagePlus []img,final boolean []features,final int sigMin,final int sigMax,int[]zStart,int[]zStop) {
	final long t0=System.currentTimeMillis();	
	//Separer l image initiale en plus petits paquets
	int []dims=VitimageUtils.getDimensions(img[0]);
	int Zfull=dims[2];
	int X=dims[0];
	int Y=dims[1];
	double sumOct=0;
	final ImagePlus[]imgSeparated=new ImagePlus[img.length];
	final FeatureStack[]featSeparated=new FeatureStack[];
	for(int m=0;m<img.length;m++) {
		for(int s=0;s<N_SPECIMENS;s++) {
			imgSeparated[s][m]=VitimageUtils.cropImageByte(img[m],0, 0, zStart[s]-1, X-1,Y-1, zStop[s]-1);
			sumOct+=(zStop[s]-zStart[s]+1)*X*Y;
			System.out.println("CUT : sumOct="+VitimageUtils.dou((sumOct)/1000000.0) );
		}
	}
	int sigg=sigMin;int nbSigma=0;
	while(sigg<=sigMax) {nbSigma++;sigg*=2;}
	final int[]sigmas=new int[nbSigma];
	final int nSig=nbSigma;
	sigmas[0]=sigMin;
	for(int i=1;i<nbSigma;i++) {
		sigmas[i]=sigmas[i-1]*2;
	}



	//Here le multithreading
	final int nbFeat=computeNbFeat(img.length, nbSigma, features);
	AtomicInteger counter=new AtomicInteger(0);
	AtomicInteger countPercent=new AtomicInteger(0);
	AtomicInteger atomNumThread=new AtomicInteger(0);
	final Thread[] threads = VitimageUtils.newThreadArray(N_SPECIMENS);  
	final int N_SPEC=N_SPECIMENS;
	for (int ithread = 0; ithread < N_SPECIMENS; ithread++) {  
		threads[ithread] = new Thread() {  { setPriority(Thread.NORM_PRIORITY); }  
		public void run() {
			int s=atomNumThread.getAndIncrement();
			featSeparated[s]=buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(imgSeparated[s],features,sigmas,s,counter,countPercent,nbFeat,t0);
		}
		};
	}
	VitimageUtils.startAndJoin(threads);  
	System.out.println("Feature stack ok.\n\n\n");


	//Assembler en une unique featureStackArray
	FeatureStackArray fts=new FeatureStackArray(Zfull,sigMin,sigMax,false,1,1,null);
	int incr=0;
	for(int s=0;s<N_SPECIMENS;s++) {
		for(int z=0;z<featSeparated[s].length;z++) {
			fts.set(featSeparated[s][z], incr++);
		}
	}

	return fts;

}
*/









/*

public static FeatureStackArray buildFeatureStackRGBSeparatedMultiThreadedV2Processing3D(
		final ImagePlus []init,final boolean []features,final int []sigmas,final int numSpecimen,
		AtomicInteger counter,AtomicInteger countPercent,final int nbTotalCalculus) {
	FeatureStackArray fts=new FeatureStackArray(Zfull,sigMin,sigMax,false,1,1,null);
	FeatureStack[]fs=new FeatureStack[nbTotalCalculus];
	long t0=System.currentTimeMillis();
	long t1=0;
	double dt=0;
	double dT=0;
	int nSigma=sigmas.length;
	final int X=init[0].getWidth();
	final int Y=init[0].getHeight();
	final int Z=init[0].getStackSize();
	final int C=init.length;
	System.out.println("Construction feature stack avec sigma successifs = "+TransformUtils.stringVectorN(sigmas, ""));
	int numFeat=computeNbFeat(C,nSigma,features);
	final int nbFeat=numFeat;

	final int eachInt=((nbTotalCalculus*N_SPECIMENS)/300)*3;
	int localCount=0;
	String []can=new String[C];
	for(int c=0;c<C;c++)can[c]="_Can"+c;

	int incr=0;
	final ImageStack []imgStack=new ImageStack[numFeat];
	ImagePlus temp;
	ImagePlus temp2;

	///////// LES CANAUX INITIAUX
	System.out.print(" canaux "+stringSpecimens[numSpecimen]);
	for(int  c=0;c<C;c++) {
		imgStack[incr++]=VitimageUtils.imageCopy(init[c],"Init"+can[c]+"_SIG0").getStack();
	}
	localCount=counter.addAndGet(C);
	if(((localCount*1000)/(nbTotalCalculus*N_SPECIMENS)>countPercent.get())) {
		t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus*N_SPECIMENS-localCount)/localCount);
		System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+" /1000) . Elapsed time = "+
		dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
		IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
		dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
		IJ.showProgress(countPercent.get()/1000.0);
	}


	///////// GAUSSIENNES
	if(features[1]|| features[3] || features[0]) {
		ImagePlus [][]tabG=new ImagePlus[sigmas.length+1][C];
		System.out.print(" gauss "+stringSpecimens[numSpecimen]);
		for(int c=0;c<C;c++)tabG[0][c]=VitimageUtils.imageCopy(init[c]);//Copie pour la div de gaussiennes
		for(int isig =0;isig<sigmas.length;isig++) {				
			for(int c=0;c<C;c++) {
				tabG[isig+1][c]=VitimageUtils.imageCopy(init[c],"-Gauss-"+can[c]+"_SIG"+sigmas[isig]);
				IJ.run(tabG[isig+1][c], "Gaussian Blur 3D...", "x="+sigmas[isig]+" y="+sigmas[isig]+" z="+sigmas[isig]);
				if(features[1])imgStack[incr++]=tabG[isig+1][c].getStack();
			}
		}
		if(features[1])		localCount=counter.addAndGet(C*nSigma);
		if(((localCount*1000)/(nbTotalCalculus*N_SPECIMENS)>countPercent.get())) {
			t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus*N_SPECIMENS-localCount)/localCount);
			System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+" /1000) . Elapsed time = "+
			dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
			IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
			dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
			IJ.showProgress(countPercent.get()/1000.0);
		}

		///////// DIFFERENCE DE GAUSSIENNES
		if(features[3]) {
			System.out.print(" diffG"+stringSpecimens[numSpecimen]);
			for(int isig =0;isig<sigmas.length;isig++) {
				for(int c=0;c<C;c++) {
					temp=VitimageUtils.makeOperationBetweenTwoImages(tabG[isig][c], tabG[isig+1][c], VitimageUtils.OP_SUB,true);temp.setDisplayRange(-128, 128);IJ.run(temp,"8-bit","");
					imgStack[incr++]=VitimageUtils.imageCopy(temp,"DiffGauss"+can[c]+"_SIG"+sigmas[isig]).getStack();
				}
			}
			localCount=counter.addAndGet(C*nSigma);
			if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
				t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
				System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
				IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
				IJ.showProgress(countPercent.get()/100.0);
			}
		}
		///////// DIVISIONS ENTRE CANAUX
		if(features[0]) { 
			System.out.print(" GdivG"+stringSpecimens[numSpecimen]);
			for(int i=0;i<sigmas.length;i++) {
				for(int c1=0;c1<C;c1++) {
					for(int c2=0;c2<C;c2++) {
						if(c1 != c2) {
							temp=VitimageUtils.makeOperationBetweenTwoImages(tabG[i+1][c1],tabG[i+1][c2], VitimageUtils.OP_DIV,true);IJ.run(temp, "Log", "");temp.setDisplayRange(-3, 3);IJ.run(temp,"8-bit","");
							imgStack[incr++]=VitimageUtils.imageCopy(temp,can[c1]+"DivGauss"+can[c2]+"_SIG"+sigmas[i]).getStack();
						}
					}
				}
			}


			localCount=counter.addAndGet(C*(C-1)*nSigma);
			if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
				t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
				System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
				IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
						dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
				IJ.showProgress(countPercent.get()/100.0);
			}
		}				
	}




	///////// EDGES
	if(features[2]) {
		System.out.print(" edges2D"+stringSpecimens[numSpecimen]);
		for(int c=0;c<C;c++) {
			temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Find Edges", "");imgStack[incr++]=VitimageUtils.imageCopy(temp,"Edges"+can[c]+"_SIG0").getStack();
		}
		localCount=counter.addAndGet(C);
		if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
			t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
			System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
			IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
			IJ.showProgress(countPercent.get()/100.0);
		}
	}






	///////// MEDIAN
	if(features[6]) {  //med
		System.out.print(" median"+stringSpecimens[numSpecimen]);
		for(int sig : sigmas) {
			for(int c=0;c<C;c++) {
				temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Median...", "radius="+sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Med"+can[c]+"_SIG"+sig).getStack();
			}
		}
		localCount=counter.addAndGet(C*nSigma);
		if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
			t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
			System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
			IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
			IJ.showProgress(countPercent.get()/100.0);
		}
	}


	///////// VARIANCE
	if(features[7]) {  //var
		System.out.print(" variance"+stringSpecimens[numSpecimen]);
		for(int sig : sigmas) {
			for(int c=0;c<C;c++) {
				temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp,"32-bit","");IJ.run(temp, "Variance...", "radius="+sig);
				temp.setDisplayRange(0, 3000);IJ.run(temp,"8-bit","");imgStack[incr++]=VitimageUtils.imageCopy(temp,"Var"+can[c]+"_SIG"+sig).getStack();
			}
		}
		localCount=counter.addAndGet(C*nSigma);
		if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
			t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
			System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
			IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
			IJ.showProgress(countPercent.get()/100.0);
		}
	}


	///////// MINIMUM
	if(features[4]) { //min
		System.out.print(" min"+stringSpecimens[numSpecimen]);
		for(int sig : sigmas) {
			for(int c=0;c<C;c++) {
				temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Minimum...", "radius="+sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Min"+can[c]+"_SIG"+sig).getStack();
			}
		}
		localCount=counter.addAndGet(C*nSigma);
		if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
			t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
			System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
			IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
			IJ.showProgress(countPercent.get()/100.0);
		}
	}



	///////// MAXIMUM
	if(features[5]) {  //max
		System.out.print(" max"+stringSpecimens[numSpecimen]);
		for(int sig : sigmas) {
			for(int c=0;c<C;c++) {
				temp=VitimageUtils.imageCopy(init[c]);IJ.run(temp, "Maximum...", "radius="+sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Max"+can[c]+"_SIG"+sig).getStack();
			}
		}
		localCount=counter.addAndGet(C*nSigma);
		if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
			t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
			System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
			IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
			IJ.showProgress(countPercent.get()/100.0);
		}
	}
	
	///////// DERICHE
	if(features[8]) {  //max
		System.out.print(" deriche"+stringSpecimens[numSpecimen]);
		for(int sig : sigmas) {
			for(int c=0;c<C;c++) {
				temp=VitimageUtils.edges3DByte(init[c],sig);imgStack[incr++]=VitimageUtils.imageCopy(temp,"Deriche"+can[c]+"_SIG"+sig).getStack();
			}
		}
		localCount=counter.addAndGet(C*nSigma);
		if(((localCount*100)/nbTotalCalculus)>countPercent.get()) {
			t1=System.currentTimeMillis();dt=VitimageUtils.dou((t1-t0)/1000.0);dT=VitimageUtils.dou(dt*(nbTotalCalculus-localCount)/localCount);
			System.out.print("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");
			IJ.log("\n# Calculs effectues : "+localCount+" / "+nbTotalCalculus+" ("+countPercent.incrementAndGet()+"%) . Elapsed time = "+
					dt+" s . Remaining time = "+dT+" s . Total time = "+VitimageUtils.dou(dt+dT)+" s ############  ");	
			IJ.showProgress(countPercent.get()/100.0);
		}
	}
	System.out.println("Feature stack of specimen "+stringSpecimens[numSpecimen]+" ok.");
	return convertStacksToFeatureStacks(imgStack);
}
*/





