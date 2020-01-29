package com.vitimage;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.jfree.chart.plot.ThermometerPlot;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.Memory;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import math3d.Point3d;
import vib.oldregistration.RegistrationAlgorithm;

/**
 * 
 * @author fernandr
 * TODO : describe Class and code articulation
 */
/*TODO : v2 Already done non-regression tests :
* Undo is ok. Activate up to one step done, deactivate when last step removed. Close the contexts previously open, update both structures and interfaces
* Abort is ok.
* One day : handle RegistrationAction with a specific object : a pile of to-do actions	
* 
* */




public class RegistrationManager extends PlugInFrame implements ActionListener {
	private boolean debugMode=true;
	private boolean first2dmessage=true;
	private boolean first3dmessage=true;
	
	private Color colorStdActivatedButton;
	private Color colorGreenRunButton;
	
	//Flags for the kind of viewer
	private static final int VIEWER_3D = 0;
	private static final int VIEWER_2D = 1;
	private static final String saut="<div style=\"height:1px;display:block;\"> </div>";
	private static final String startPar="<p  width=\"650\" >";
	private static final String nextPar="</p><p  width=\"650\" >";

	private static final int MODE_TWO_IMAGES=2;
	private static final int MODE_SERIE=3;

	//Flags for the "run" button tooltip
	private static final int MAIN=81;
	private static final int MANUAL2D=82;
	private static final int MANUAL3D=83;

	//Identifiers for boxLists
	private static final int BOXACT=91;
	private static final int BOXOPT=92;
	private static final int BOXTRANS=93;
	private static final int BOXTIME=94;
	private static final int BOXDISP=95;
	private static final int BOXDISPMAN=96;

	//Identifiers for buttons of registration two imgs
	private static final int SETTINGS=101;
	private static final int RUN=102;
	private static final int UNDO=103;
	private static final int ABORT=104;
	private static final int SAVE=105;
	private static final int FINISH=106;
	private static final int SOS=107;


	
	//Identifiers for buttons of start window
	private static final int RUNTWOIMG=111;
	private static final int RUNSERIE=112;
	private static final int RUNTRANS=113;
	private static final int RUNTRANSCOMP=114;
	private static final int SOSINIT=115;

	//Identifiers for buttons of registration serie
	private static final int RUNNEXTSTEP=131;
	private static final int GOBACKSTEP=132;

	
	//Flags for the state of the registration manager
	private static final int REGWINDOWTWOIMG=121;
	private static final int REGWINDOWSERIEPROGRAMMING=122;
	private static final int REGWINDOWSERIERUNNING=123;
	int registrationWindowMode=REGWINDOWSERIEPROGRAMMING;
	
	//Interface parameters
	private static final long serialVersionUID = 1L;
	private int SOS_CONTEXT_LAUNCH=0;
	private ImagePlus imgView;
	boolean actionAborted=false;
	boolean developerMode=false;
	private ij3d.Image3DUniverse universe;
	String spaces="                                                                                                                                   ";
	private int viewSlice;

	
	//Registration interface attributes
	private String[]textActions=new String[] {"Manual registration","Automatic registration","Images alignment with XYZ axis"};
	private String[]textOptimizers=new String[] {"Block-Matching","ITK"};
	private String[]textTransformsBM=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)","Vector field "};
	private String[]textTransformsITK=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)"};
	private String[]textTransformsMAN=new String[] {"Rigid (no deformations)",};
	private String[]textTransformsALIGN=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)"};
	private String[]textDisplayITK=new String[] {"0-Only at the end (faster)","1-Dynamic display (slower)"};
	private String[]textDisplayBM=new String[] {"0-Only at the end (faster)","1-Dynamic display (slower)","2-Also display score map (slower+)"};
	private String[]textDisplayMan=new String[] {"3d viewer (volume rendering)","2d viewer (classic slicer)"};
	
	//Interface text, label and lists
	private JList<String>listActions=new JList<String>(new String[]{spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces});
	JScrollPane actionsPane = new JScrollPane(listActions);
	private JLabel labelNextAction = new JLabel("Choose the next action :", JLabel.LEFT);
    public JComboBox<String>boxTypeAction=new JComboBox<String>(textActions);
	private JLabel labelOptimizer = new JLabel("Automatic registration optimizer :", JLabel.LEFT);
	public JComboBox<String>boxOptimizer=new JComboBox<String>(  textOptimizers   );
	private JLabel labelTransformation = new JLabel("Transformation to estimate :", JLabel.LEFT);
	public JComboBox<String>boxTypeTrans=new JComboBox<String>( textTransformsBM  );
	private JLabel labelView = new JLabel("Automatic registration display :", JLabel.LEFT);
	public JComboBox<String>boxDisplay=new JComboBox<String>( textDisplayBM );
	private JLabel labelViewMan = new JLabel("Manual registration viewer :", JLabel.LEFT);
	public JComboBox<String>boxDisplayMan=new JComboBox<String>( textDisplayMan );
	private JLabel labelTime1 = new JLabel("Estimated time for this action :", JLabel.LEFT);
	private JLabel labelTime2 = new JLabel("0 mn and 0s", JLabel.CENTER);

	//Buttons, Frames, Panels
	private JButton settingsButton=new JButton("Advanced settings...");
	private JButton settingsDefaultButton=new JButton("Restore default settings...");

	private JButton runButton=new JButton("Start this action");
	private JButton abortButton = new JButton("Abort");
	private JButton undoButton=new JButton("Undo");
	private JButton saveButton=new JButton("Save current state");
	private JButton finishButton=new JButton("Export results");
	private JButton sos1Button=new JButton("Contextual help");

	private JButton runTwoImagesButton = new JButton("Register two 3d images (training mode)");
	private JButton runSerieButton = new JButton("Register 3d series (N-times and/or N-modalities)");
	private JButton transformButton = new JButton("Apply a computed transform to another image");
	private JButton composeTransformsButton = new JButton("Compose successive transforms into a single one");
	private JButton sos0Button =new JButton("Start this action");
	private JButton runNextStepButton = new JButton("Run next step");
	private JButton goBackStepButton = new JButton("Coming soon...");

	private JButton addActionButton = new JButton("Add an action to pipeline");
	private JButton removeSelectedButton = new JButton("Remove last action");
	private JButton validatePipelineButton = new JButton("Approve inter-time pipeline");
	
	private JFrame registrationFrame;
	private JTextArea logArea=new JTextArea("", 10,10);
	private Color colorIdle;
	private JFrame frameLaunch;

	
	
	
	
	
	//Structures for computation (model part);
	private RegistrationAction currentRegAction;
	private ArrayList<ItkTransform> trActions=new ArrayList<ItkTransform>();
	ItkRegistrationManager itkManager;
	BlockMatchingRegistration bmRegistration;
	public int referenceModality=0;
	public int referenceTime=0;
	public int refTime=0;
	public int refMod=0;
	public int movTime=0;
	public int movMod=1;
	private int step=0;
	private int nTimes=0;
	private int nMods=0;
	private ImagePlus[][]images;
	private String[][]paths;
	private int[][][]initDimensions;
	private int[][][]dimensions;
	private double[][][]imageRanges;
	private double[][]imageSizes;//In Mvoxels
	private double[][][]initVoxs;
	private double[][][]voxs;
	private ArrayList<ItkTransform> [][] transforms;
	int estimatedTime=0;
	
	private final double EPSILON=1E-8;
	private String unit="mm";
	private int[]imageParameters=new int[] {512,512,40};
	public int maxAcceptableLevel=3;
	private int nbCpu=1;
	private int jvmMemory=1;
	private int freeMemory=1;
	private long memoryFullSize;
	
	private int mode=MODE_TWO_IMAGES;
	private volatile boolean pipelineValidated=false;

	private int nSteps;

	private boolean[] stepExecuted;
	protected boolean comboBoxChanged=false;


	
	

	
	/**Starting points-------------------------------------------------------------------------------- 
	 * 
	 */
	public RegistrationManager() {
		super("FijiYama : a versatile registration tool for Fiji");
		this.currentRegAction=new RegistrationAction(this);
		regActions.add(this.currentRegAction);
	}

	public static void main(String[]args) {//TestMethod
		ImageJ ij=new ImageJ();
		ij.getAlignmentX();
		RegistrationManager reg=new RegistrationManager();
		reg.developerMode=true;
		reg.run("");
	}
	
	public void run(String arg) {
		if(debugMode) {
			runTest();
		}
		else startLaunchingInterface();
	}

	public void runTest() {
		int TESTLOAD=1;
		int TESTTRANS=2;
		int TESTTWOIMG=3;
		int TESTHYPERIMG=4;
		int TESTCOMPOSE=5;
		int TESTNEWSERIE=6;
		int TESTINTERFACEPROGRAMMING=7;
		int TESTINTERFACERUNNING=8;
	
		int typeTest=TESTNEWSERIE;

		if(typeTest==TESTTWOIMG) {
			String path1="/home/fernandr/Bureau/Test/TWOIMG/imgRef.tif";
			String path2="/home/fernandr/Bureau/Test/TWOIMG/imgMov.tif";
			this.setupTwoImagesRegistrationFromTwoPaths(path1, path2);
			startTwoImagesRegistration();

		}
		else if(typeTest==TESTTWOIMG) {
			String path1="/home/fernandr/Bureau/Test/TWOIMG/imgRef.tif";
			String path2="/home/fernandr/Bureau/Test/TWOIMG/imgMov.tif";
			this.setupTwoImagesRegistrationFromTwoPaths(path1, path2);
			startTwoImagesRegistration();
		}
		else if(typeTest==TESTLOAD) {
			this.loadFromFjmFile("/home/fernandr/Bureau/Pouet/save_DATA_FJM/save.fjm");
			startTwoImagesRegistration();
		}
		else if(typeTest==TESTHYPERIMG) {
			this.setupTwoImagesRegistrationFromTwoPaths("/home/fernandr/Bureau/testXYZCT.tif", "/home/fernandr/Bureau/testXYZCT.tif");
			startTwoImagesRegistration();
		}
		else if(typeTest==TESTTRANS){
			runTransformImage();
		}
		else if(typeTest==TESTCOMPOSE){
			runComposeTransforms();
		}
		else if(typeTest==TESTNEWSERIE) {
			setupSerie();
			startSerie();
		}
		else if(typeTest==TESTINTERFACEPROGRAMMING) {
			this.registrationWindowMode=REGWINDOWSERIEPROGRAMMING;
			this.startRegistrationInterface();
		}
		else if(typeTest==TESTINTERFACERUNNING) {
			this.registrationWindowMode=REGWINDOWSERIERUNNING;
			this.startRegistrationInterface();
		}
	}
	

	
	
	
	
	/** Setup and start methods----------------------------------------------------------------------------------
	 * 
	 */
	public void setupTwoImagesRegistrationFromTwoPaths(String pathToRef,String pathToMov) {
		this.registrationWindowMode=REGWINDOWTWOIMG;
		this.mode=MODE_TWO_IMAGES;
		this.nTimes=1;
		this.nMods=2;
		this.referenceTime=0;this.referenceModality=0;
		this.refTime=0;this.refMod=0;
		this.movTime=0;this.movMod=1;
		setupStructures();
		this.paths[refTime][refMod]=pathToRef;
		this.paths[movTime][movMod]=pathToMov;
		checkComputerCapacity();
		openImagesAndCheckOversizing();
	}
	
	public void startTwoImagesRegistration() {
		startRegistrationInterface();
		openImagesAndCheckOversizing();
		updateViewTwoImages();
		currentRegAction.defineSettingsFromTwoImages(this.images[refTime][refMod],this.images[movTime][movMod],getRegistrationManager(),true);
		enable(new int[] {RUN,SETTINGS,SOS,BOXACT,BOXTRANS,BOXDISPMAN});		
		addLog("Welcome to Fijiyama ! First trial ? Click on \"Contextual help\" to get started",0);
		addLog("Current mode : two-images registration. Choose the next action using the menus, then click on \"Start this action\"",0);
		addLog("",0);
		informAboutComputerCapabilities();
		if(this.step>0)enable(new int[] {RUN,SAVE,UNDO,FINISH});
	}
	
	public void setupSerie() {
		System.out.println("Setup Serie");
		this.serieOutputPath=(debugMode) ? "/home/fernandr/Bureau/Test/SERIE/OUTPUT_DIR" : VitiDialogs.chooseDirectoryUI("Select an empty output directory to begin a new work, or select a directory containing a previous .fjm file","Select empty output dir...");
		if(this.serieOutputPath==null) {IJ.showMessage("No output path given. Abort");return ;}
		this.serieFjmFile=null;
		System.out.println("serieOutputPath="+this.serieOutputPath);
		if(this.serieOutputPath==null)return;
		String[]files=new File(this.serieOutputPath).list();
		if(files.length==0) {
			if(!createNewSerie()) {
				System.out.println("DEBUG 31");return;
			}
		}
		else {
			System.out.print("Output directory "+this.serieOutputPath+" is non empty. Looking for a .jfm config file...");
			boolean found=false;
			for(String str : files) {
				if(str.substring(str.length()-4,str.length()).equals(".fjm")) {
					System.out.println("Found !");
					this.name=str.substring(0,str.length()-4);
					found=true;
				}
			}
			if(!found) {
				System.out.println("Not found !");
				IJ.showMessage("Error : opened a directory with random other files, but no .fjm configuration files. Please choose"+
						" a fresh empty directory, or an existing Fijiyama directory, containing a .fjm file");return;
			}
			loadFromFjmFile(new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath());
		}
	}
	
	public void startSerie(){
		this.mode =MODE_SERIE;
		this.registrationWindowMode=REGWINDOWSERIERUNNING;
		startRegistrationInterface();
		currentRegAction=this.regActions.get(this.step);
		disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS});
		enable(new int[] {RUNNEXTSTEP,GOBACKSTEP,SOS});	
		addLog("Welcome to Fijiyama ! First trial ? Click on \"Contextual help\" to get started",0);
		addLog("Current mode : serie processing. Click on \"Run next action\", or use the menu to modify it before. Series can be long. Keep clicking !",0);
		addLog("",0);
		informAboutComputerCapabilities();	
		addLog("--> Waiting for you to start next action : "+currentRegAction.readableString(false),0);
	}
	
	public boolean createNewSerie() {
		//Get input dir to collect images of serie
		this.name="Fijiyama_serie_"+new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
		this.expression="img_t{Time}_mod{ModalityName}.tif";
		String strTimes="1-3";
		String strMods="MRI;RX";
	
		if(!debugMode)this.name=VitiDialogs.getStringUI("Choose a name for your serie (accepted characters : alphanumeric, underscore and minus)","Serie name",this.name,true);
		System.out.println("Starting new serie "+this.name);
		this.serieInputPath=(debugMode) ? "/home/fernandr/Bureau/Test/SERIE/INPUT_DIR" : VitiDialogs.chooseDirectoryUI("Select an input directory containing 3d images","Select input dir...");
		if(this.serieInputPath==null){IJ.showMessage("Input path = null. Exit");return false;}
		//Get regular expression for image lookup
		if(!debugMode) {
			GenericDialog gd=new GenericDialog("Describe file names with a generic expression");
			gd.addMessage("Write observation times. If no multiple times, leave blank. Example : 1-5 or 10;33;78 ");
			gd.addStringField("Times=", "1-3", 40);
			gd.addMessage("");
			gd.addMessage("Write modalities. If no multiple modalities, leave blank. Example : RX;MRI;PHOTO ");
			gd.addStringField("Modalities=", "MRI;RX", 40);
			gd.addMessage("");
			gd.addMessage("Write expression describing your files, with {ModalityName} for modalities and {Time} for times");
			gd.addStringField("Generic expression", this.expression, 50);
			gd.showDialog();
			if(gd.wasCanceled()){IJ.showMessage("Dialog exited. Abort");return false;}
			strTimes=gd.getNextString();
			strMods=gd.getNextString();
			this.expression=gd.getNextString();
		}
		try {
			this.times=parseTimes(strTimes);
			this.mods=(strMods.length()==0 ? new String[] {""} : strMods.split(";"));
			this.nTimes=this.times.length;
			this.nMods=this.mods.length;
			setupStructures();			
		}catch(Exception e) {IJ.showMessage("Exception when reading parameters. Abort");return false;}
		int []numberPerTime=new int[this.nTimes];
		int []numberPerMod=new int[this.nMods];
		int nbTot=0;
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				File f=new File(this.serieInputPath,expression.replace("{Time}", times[nt]).replace("{ModalityName}",mods[nm]));
				System.out.print("Testing "+f.getAbsolutePath());
				if(f.exists()) {
					System.out.println(" ... Found !");
					this.paths[nt][nm]=f.getAbsolutePath();
					numberPerTime[nt]++;
					numberPerMod[nm]++;
					nbTot++;
				}
				else System.out.println(" ... not found.");
			}
		}
		if(nbTot==0) {IJ.showMessage("No image found. Exit");return false;}
		for(int nt=0;nt<this.nTimes;nt++)if(numberPerTime[nt]==0) {IJ.showMessage("No image found for time "+times[nt]+". Exit");return false;}
		for(int nm=0;nm<this.nMods;nm++)if(numberPerMod[nm]==0) {IJ.showMessage("No image found for modality "+mods[nm]+". Exit");return false;}
		int nbPotentialRef=0;
		for(int nm=0;nm<this.nMods;nm++)if(numberPerMod[nm]==this.nTimes)nbPotentialRef++;
		if(nbPotentialRef==0) {IJ.showMessage("No modality can be used as inter-time reference, because no modality is present at both observation times. Exit");return false;}
		String[]potentialRef=new String[nbPotentialRef];
		int[]potentialRefIndex=new int[nbPotentialRef];
		int index=0;
		for(int nm=this.nMods-1;nm>=0;nm--)if(numberPerMod[nm]==this.nTimes) {potentialRef[index]=mods[nm];potentialRefIndex[index++]=nm;}

		//Select reference modality
		if(!debugMode) {
			GenericDialog gd2=new GenericDialog("Choose reference modality for registration");
			gd2.addMessage("This modality will be the reference image for each time-point. Choose the easiest to compare with all the other ones.");
			gd2.addMessage("The registration process is done with the dimensions of the reference image.\n--> If you choose a low resolution modality, registration can be unaccurate\n"+
					"--> If you choose a high resolution modality, it will be subsampled to prevent memory overflow");
			gd2.addMessage("");
			gd2.addMessage("After reference modality, you will have to choose the reference time. The dirst one is the recommended choice");
			gd2.addChoice("Reference modality", potentialRef, potentialRef[0]);
			gd2.addChoice("Reference time", this.times, times[0]);
			gd2.showDialog();
			if(gd2.wasCanceled()){IJ.showMessage("Dialog exited. Abort");return false;}
			int refInd=gd2.getNextChoiceIndex();
			this.referenceModality=potentialRefIndex[refInd];
			this.referenceTime=gd2.getNextChoiceIndex();
		}		
		
		System.out.println("\nReference modality             : nb."+this.referenceModality+" = "+this.mods[this.referenceModality]);
		System.out.println("Reference time                 : nb."+this.referenceTime+" = "+this.times[this.referenceTime]);
		System.out.println("Serie input path               : "+this.serieInputPath);
		System.out.println("Serie output path              : "+this.serieOutputPath);
		System.out.println("Times                          : "+strTimes);
		System.out.println("Modalities                     : "+strMods);
		System.out.println("Expression                     : "+this.expression);
		System.out.println("Nb images detected             : "+nbTot);
		System.out.println("Nb registrations to compute    : "+(nbTot-1));
		System.out.println("\nEverything fine, then. Keep clicking !");
		if(nbTot>30) {
			System.out.println("There is a lot of images there. Memory saving / Computation time tradeoff is set to memory saving first, for the sake of your RAM");
			this.memorySavingMode=true;
		}
		
		new File(this.serieOutputPath,"Registration_files").mkdirs();
		new File(this.serieOutputPath,"Exported_data").mkdirs();
		
		checkComputerCapacity();
		openImagesAndCheckOversizing();
		defineSerieRegistrationPipeline();
		saveSerieToFjmFile();
		loadFromFjmFile(new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath());
		return true;
	}

	public void printRegActions(String name,ArrayList<RegistrationAction> listAct) {
		System.out.println();
		System.out.println(" # Reg actions "+name+", size="+listAct.size());
		for(int i=0;i<listAct.size();i++) {
			System.out.println(" # "+listAct.get(i));
		}
	}
	
	
	public void defineSerieRegistrationPipeline() {
		this.registrationWindowMode=REGWINDOWSERIEPROGRAMMING;
		ArrayList<RegistrationAction>interTimes=new ArrayList<RegistrationAction> ();
		ArrayList<ArrayList<RegistrationAction>>interMods=new ArrayList<ArrayList<RegistrationAction> >();
		//Define inter-time pipeline, if needed
		System.out.println("Processing intertime pipeline");
		if(this.nTimes>1) {
			this.regActions=new ArrayList<RegistrationAction>();
			this.regActions.add(RegistrationAction.createRegistrationAction(
					images[referenceTime][referenceModality],images[referenceTime][referenceModality], this,RegistrationAction.TYPEACTION_MAN));
			this.regActions.add(RegistrationAction.createRegistrationAction(
					images[referenceTime][referenceModality],images[referenceTime][referenceModality],this,RegistrationAction.TYPEACTION_AUTO).setStepTo(1));
			startRegistrationInterface();
			this.updateList();
			validatePipelineButton.setText("Approve inter-time pipeline");
			if(!debugMode)IJ.showMessage("Define registration pipeline to align images of the reference modality between two successive times.\n"+
			"Click on an action in the bottom list, and modify it using the menus. Click on remove to delete the selected action, \n"+
			"and click on add action to insert a new registration action just before the cursor\n\nOnce done, click on Validate pipeline.");
			waitForValidation();
			interTimes=this.regActions;
		}
		printRegActions("Intertime pipeline", interTimes);

		//Define inter-mods pipeline, if needed
		System.out.println("Processing intermods pipelines");
		if(this.nTimes>1) {
			for(int curMod=0;curMod<this.nMods;curMod++) {
				if(curMod == referenceModality) interMods.add(null);
				else {
					System.out.println("Doing for mod "+curMod);
					this.regActions=new ArrayList<RegistrationAction>();
					this.regActions.add(RegistrationAction.createRegistrationAction(
							images[referenceTime][referenceModality],images[referenceTime][referenceModality], this,RegistrationAction.TYPEACTION_MAN));
					this.regActions.add(RegistrationAction.createRegistrationAction(
							images[referenceTime][referenceModality],images[referenceTime][referenceModality],this,RegistrationAction.TYPEACTION_AUTO).setStepTo(1));
					this.updateBoxesAndCurrentAction();
					if(!debugMode)IJ.showMessage("Define registration pipeline to align "+this.mods[referenceModality]+" with "+this.mods[curMod]+" from the same timepoint.\n"+
							"Click on an action in the bottom list, and modify it using the menus. Click on remove to delete the selected action, \n"+
							"and click on add action to insert a new registration action just before the cursor\n\nOnce done, click on Validate pipeline.");
					validatePipelineButton.setText("Approve "+this.mods[curMod]+"->"+this.mods[referenceModality]+" pipeline");
					waitForValidation();
					interMods.add(this.regActions);
					printRegActions("Intermod "+curMod+" pipeline", interMods.get(interMods.size()-1));
				}
			}
		}
		registrationFrame.setVisible(false);
		registrationFrame.dispose();
		
		//Build sequence of steps
		boolean sequenceRegistrationSaveHumanTime=VitiDialogs.getYesNoUI("Sequencing order tradeoff", "Do you want the sequence to be fit to save human time ? \n"+
				"The \"No\" option means you prefer the sequence being stuck on the logical order of the serie,\n"+
				" alternating automatic steps with manual steps where human presence is compulsory. ");
		
		//If pipeline is in logical order
		if(!sequenceRegistrationSaveHumanTime) {
			this.step=-1;
			this.regActions=new ArrayList<RegistrationAction>();
			System.out.println("Sequencing intertime registrations");
			for(int tRef=0;tRef<this.nTimes-1;tRef++) {
				refTime=tRef;refMod=referenceModality;
				movTime=tRef+1;movMod=referenceModality;
				for(int regSt=0;regSt<interTimes.size();regSt++) {
					this.step++;
					regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(regSt),refTime,refMod,movTime,movMod,step));
				}
				regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_SAVE));
			}
			System.out.println("Sequencing intermodal registrations");
			for(int tRef=0;tRef<this.nTimes-1;tRef++) {
				refTime=tRef;refMod=referenceModality;
				movTime=tRef;
				for(int mMov=0;mMov<this.nMods;mMov++) {
					movMod=mMov;
					if(refMod != movMod) {
						for(int regSt=0;regSt<interMods.get(movMod).size();regSt++) {
							this.step++;
							regActions.add(RegistrationAction.copyWithModifiedElements(interMods.get(movMod).get(regSt),refTime,refMod,movTime,movMod,step));
							this.step++;
							regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_SAVE));
						}
						this.step++;
						regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_SAVE));
					}
				}
			}
			if(regActions.get(regActions.size()-1).typeAction!=RegistrationAction.TYPEACTION_VIEW) {
				this.step++;
				regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_VIEW));
			}
		}
		//If pipeline is in human-time saving mode
		else {
			this.step=-1;
			this.regActions=new ArrayList<RegistrationAction>();
			int[]typeSuccessives=new int[] {0,1};//MAn then AUTO
			for(int type : typeSuccessives) {
				System.out.println("Sequencing intertime registrations");
				for(int tRef=0;tRef<this.nTimes-1;tRef++) {
					refTime=tRef;refMod=referenceModality;
					movTime=tRef+1;movMod=referenceModality;
					for(int regSt=0;regSt<interTimes.size();regSt++) {
						if(interTimes.get(regSt).typeAction==type) {
							this.step++;
							regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(regSt),refTime,refMod,movTime,movMod,step));
						}
					}
				}
				if(regActions.get(regActions.size()-1).typeAction!=RegistrationAction.TYPEACTION_SAVE) {
					this.step++;
					regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_SAVE));
				}
				
				System.out.println("Sequencing intermodal registrations");
				for(int tRef=0;tRef<this.nTimes;tRef++) {
					refTime=tRef;refMod=referenceModality;
					movTime=tRef;
					for(int mMov=0;mMov<this.nMods;mMov++) {
						movMod=mMov;
						if(refMod != movMod && imageExists(movTime, movMod)) {
							for(int regSt=0;regSt<interMods.get(movMod).size();regSt++) {
								if(interMods.get(movMod).get(regSt).typeAction==type) {
									this.step++;
									regActions.add(RegistrationAction.copyWithModifiedElements(interMods.get(movMod).get(regSt),refTime,refMod,movTime,movMod,step));
								}
							}
						}
					}
				}
				if(regActions.get(regActions.size()-1).typeAction!=RegistrationAction.TYPEACTION_SAVE) {
					this.step++;
					regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_SAVE));
				}
				if(regActions.get(regActions.size()-1).typeAction!=RegistrationAction.TYPEACTION_VIEW) {
					this.step++;
					regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_VIEW));
				}
			}
		}
		this.step++;
		regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),referenceTime,referenceModality,referenceTime,referenceModality,step).setActionTo(RegistrationAction.TYPEACTION_ALIGN));
		if(regActions.get(regActions.size()-1).typeAction!=RegistrationAction.TYPEACTION_SAVE) {
			this.step++;
			regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_SAVE));
		}
		if(regActions.get(regActions.size()-1).typeAction!=RegistrationAction.TYPEACTION_VIEW) {
			this.step++;
			regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),refTime,refMod,movTime,movMod,step).setActionTo(RegistrationAction.TYPEACTION_VIEW));
		}
		this.step++;
		regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(0),referenceTime,referenceModality,referenceTime,referenceModality,step).setActionTo(RegistrationAction.TYPEACTION_EXPORT));
		printRegActions("Global pipeline at export",regActions);
		this.nSteps=this.step;
		
		this.step=0;
	}

	
		
//TODO : during Programming, only MAN and AUTO actions available
	
	
	
	
	/** Helpers for setup ----------------------------------------------------------------------------
	* 
	 */
	@SuppressWarnings("unchecked")
	public void setupStructures(){
		regActions=new ArrayList<RegistrationAction>();
		
	}
	
	@SuppressWarnings("restriction")
	public void checkComputerCapacity() {
		int nbCpu=Runtime.getRuntime().availableProcessors();
		//System.out.println("Nb cores for multithreading = "+nbCpu);
		this.jvmMemory=(int)((new Memory().maxMemory() /(1024*1024)));//Java virtual machine available memory (in Megabytes)
		this.freeMemory=(int)(Runtime.getRuntime().freeMemory() /(1024*1024));//Java virtual machine available memory (in Megabytes)
		this.memoryFullSize=0;
		try {
			this.memoryFullSize = ( ((com.sun.management.OperatingSystemMXBean) ManagementFactory
		        .getOperatingSystemMXBean()).getTotalPhysicalMemorySize() )/(1024*1024);
		}	catch(Exception e) {}		
		//System.out.println("Total available memory in virtual machine = "+this.jvmMemory+" MB");
		//System.out.println("Actual free memory in virtual machine = "+this.jvmMemory+" MB");
		//if(this.memoryFullSize>0)System.out.println("Total physical memory on this computer = "+this.memoryFullSize+" MB");
		//else System.out.println("Total physical memory on this computer : cannot be measured");
		this.nbCpu=nbCpu;
		this.maxImageSizeForRegistration=this.jvmMemory/(4*30);		
		//System.out.println("Suggested maximum image size for registration = "+this.maxImageSizeForRegistration+" MB.\n"+
		////"If given images are bigger, Fijiyama will ask you for the permission to subsample it before running registration.\nTherefore, alignment results will be computed using the full-size initial data during the export phase");
	}

	public void informAboutComputerCapabilities() {
		checkComputerCapacity();
		addLog("System check. Available memory="+this.jvmMemory+" MB. #Available processor cores="+this.nbCpu+".",0);
		if(this.memoryFullSize>this.jvmMemory*1.5) {
			addLog("It seems that your computer have more memory : total memory="+(this.memoryFullSize)+" MB.",0);
			addLog("Registration process is time and memory consuming. To give more memory and computation power to Fiji,"+
					" close the plugin then use the Fiji menu \"Edit / Options / Memory & threads\". "+
					"Let at least "+VitimageUtils.getSystemNeededMemory()+" unused to keep your "+VitimageUtils.getSystemName()+" stable and responsive.",0);
		}
	}

	
	public void openImagesAndCheckOversizing() {
		ImagePlus img;
		boolean thereIsBigImages=false;
		int factor=1;
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
					if(this.transforms[nt][nm]==null)this.transforms[nt][nm]=new ArrayList<ItkTransform>();//If it is not the case, it is a startup from a file
					ImagePlus imgTemp=IJ.openImage(this.paths[nt][nm]);
					initDimensions[nt][nm]=VitimageUtils.getDimensionsXYZCT(imgTemp);
					dimensions[nt][nm]=VitimageUtils.getDimensions(imgTemp);
					initVoxs[nt][nm]=VitimageUtils.getVoxelSizes(imgTemp);
					voxs[nt][nm]=VitimageUtils.getVoxelSizes(imgTemp);
					imageSizes[nt][nm]=((1.0*initDimensions[nt][nm][0]*initDimensions[nt][nm][1]*initDimensions[nt][nm][2])/(1024.0*1024.0));
					if(imageSizes[nt][nm]>this.maxImageSizeForRegistration)thereIsBigImages=true;
				}
			}
		}
		if(!thereIsBigImages) {
			for(int nt=0;nt<this.nTimes;nt++) {
				for(int nm=0;nm<this.nMods;nm++) {
					if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
						//System.out.println("Processing image t"+nt+" mod"+nm+" , size="+imageSizes[nt][nm]+" . No resizing");
						img=IJ.openImage(this.paths[nt][nm]);
						if(this.initDimensions[nt][nm][3]*this.initDimensions[nt][nm][4]>1)img=VitimageUtils.stacksFromHyperstackFastBis(img)[0];
						ImageProcessor ip=img.getStack().getProcessor(img.getStackSize()/2+1);
						ip.resetMinAndMax();
						this.imageRanges[nt][nm][0]=ip.getMin();
						this.imageRanges[nt][nm][1]=ip.getMax();
						this.images[nt][nm]=img;
						this.images[nt][nm].setDisplayRange(this.imageRanges[nt][nm][0],this.imageRanges[nt][nm][1]);
						//System.out.println("Display range de "+nt+" "+nm+" = "+TransformUtils.stringVectorN(this.imageRanges[nt][nm], ""));
					}
				}
			}
		}
		else{
			String recap="There is oversized images, which can lead to very slow computation, or memory overflow.\n"+
					"Your computer capability has been detected to :\n"+
					" -> "+this.jvmMemory+" MB of RAM allowed for this plugin"+"\n -> "+this.nbCpu+" parallel threads for computation."+
					"\nWith this configuration, the suggested maximal image size is set to "+this.maxImageSizeForRegistration+" Mvoxels";
			recap+=" but one of your images (at least) is bigger.\nThis setup can lead to memory issues"+
					" as registration is memory/computation intensive. \n.\nProposed solution : Fijiyama can use a subsampling strategy in order to compute registration on smaller images\n"+
					"Therefore, final resampled images will be computed using the initial data, to provide high-quality results\n\n"+
					"Do you agree that Fijiyama subsample data before registration ?";
			recap+="\n.\n.\nSummary of image sizes :\n";
			for(int nt=0;nt<this.nTimes;nt++) {
				for(int nm=0;nm<this.nMods;nm++) {
					if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
						recap+=" Image t"+nt+" mod"+nm+" , size="+imageSizes[nt][nm]+" Mvoxels.";
						if(imageSizes[nt][nm]>this.maxImageSizeForRegistration) {
							recap+=" ----- This image is oversized ------";
						}
						else	recap+=" Size ok.";

						recap +="\n";
					}
				}
			}
			if(VitiDialogs.getYesNoUI("Warning : image oversized", recap)) {
				for(int nt=0;nt<this.nTimes;nt++) {
					for(int nm=0;nm<this.nMods;nm++) {
						if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
							if(imageSizes[nt][nm]<=this.maxImageSizeForRegistration) {
								//System.out.println("Processing image t"+nt+" mod"+nm+" , size="+imageSizes[nt][nm]+" . No resizing");
								this.images[nt][nm]=IJ.openImage(this.paths[nt][nm]);
							}
							else {
								double anisotropy=this.initVoxs[nt][nm][2]/Math.max(this.initVoxs[nt][nm][1], this.initVoxs[nt][nm][0]);
								if(imageSizes[nt][nm]<=4*this.maxImageSizeForRegistration) {
									factor=2;
									if(1.6*anisotropy>=factor) {this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;}
									else {this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.dimensions[nt][nm][2]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;this.voxs[nt][nm][2]*=factor;}
								}
								else if(imageSizes[nt][nm]<=8*this.maxImageSizeForRegistration) {
									factor=3;
									if(1.6*anisotropy>=factor) {this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;}
									else {factor=2;this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.dimensions[nt][nm][2]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;this.voxs[nt][nm][2]*=factor;}
								}
								else if(imageSizes[nt][nm]<=16*this.maxImageSizeForRegistration) {
									factor=4;
									if(1.6*anisotropy>=factor) {this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;}
									else {factor=3;this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.dimensions[nt][nm][2]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;this.voxs[nt][nm][2]*=factor;}
								}
								else if(imageSizes[nt][nm]<=64*this.maxImageSizeForRegistration) {
									factor=8;
									if(1.6*anisotropy>=factor) {this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;}
									else {factor=4;this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.dimensions[nt][nm][2]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;this.voxs[nt][nm][2]*=factor;}
								}
								else {//This oversizing is an abuse. Let's resample it anyway
									int factorPow3=(int)(Math.ceil(Math.pow(imageSizes[nt][nm]/this.maxImageSizeForRegistration, 1.0/3)));
									int factorPow2=(int)(Math.ceil(Math.pow(imageSizes[nt][nm]/this.maxImageSizeForRegistration, 1.0/2)));
									if((factorPow2/anisotropy)<3){factor=factorPow2;this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;}
									else {factor=factorPow3;this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.dimensions[nt][nm][2]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;this.voxs[nt][nm][2]*=factor;
									}					
								}
								//System.out.println("Processing image t"+nt+" mod"+nm+" , size="+imageSizes[nt][nm]+" . Resizing.");
								//System.out.println(" -> Initial image : "+TransformUtils.stringVector(this.initDimensions[nt][nm], "dims")+TransformUtils.stringVector(this.initVoxs[nt][nm], "voxs"));
								//System.out.println(" -> Sampled image : "+TransformUtils.stringVector(this.dimensions[nt][nm], "dims")+TransformUtils.stringVector(this.voxs[nt][nm], "voxs")+ "final size="+((this.dimensions[nt][nm][0]*this.dimensions[nt][nm][1]*this.dimensions[nt][nm][2]/(1024.0*1024.0)))+" Mvoxs" );
								//System.out.println();
								img=IJ.openImage(this.paths[nt][nm]);if(this.initDimensions[nt][nm][3]*this.initDimensions[nt][nm][4]>1)img=VitimageUtils.stacksFromHyperstackFastBis(img)[0];
								ImageProcessor ip=img.getStack().getProcessor(img.getStackSize()/2+1);
								ip.resetMinAndMax();
								this.imageRanges[nt][nm][0]=ip.getMin();
								this.imageRanges[nt][nm][1]=ip.getMax();
								this.images[nt][nm]=ItkTransform.resampleImage(this.dimensions[nt][nm], this.voxs[nt][nm], img,false);
								this.images[nt][nm].setDisplayRange(this.imageRanges[nt][nm][0],this.imageRanges[nt][nm][1]);
								//System.out.println("Display range de "+nt+" "+nm+" = "+TransformUtils.stringVectorN(this.imageRanges[nt][nm], ""));								
							}	
						}
					}
				}
			}
		}
		img=null;
		for(int nt=0;nt<this.nTimes;nt++)for(int nm=0;nm<this.nMods;nm++) if(imageExists(nt,nm)) {
			if(this.transforms[nt][nm]==null) {transforms[nt][nm]=new ArrayList<ItkTransform>();}
		}
		System.gc();
	}
	
	public boolean imageExists(int nt,int nm) {
		return this.paths[nt][nm]!=null;
	}
	
	public static String[]parseTimes(String str){
		if(str.length()==0)return (new String[] {""});
		if(str.contains("-")){
			int min=Integer.parseInt(str.split("-")[0]);
			int max=Integer.parseInt(str.split("-")[1]);
			String []times=new String[max-min+1];
			for(int m=min;m<=max;m++)times[m-min]=""+m+"";
			return times;
		}
		return str.split(";");
	}
	
	public void waitForValidation() {
		this.pipelineValidated=false;
		while(!this.pipelineValidated)VitimageUtils.waitFor(500);
		pipelineValidated=false;
	}

	public RegistrationManager getRegistrationManager() {
		return this;
	}

	public boolean isHyperImage(int n,int m) {
		System.out.println("Test pour savoir la hyperimagité de "+n+" "+m+" : "+(this.initDimensions[n][m][3]*this.initDimensions[n][m][4]>1));
		return (this.initDimensions[n][m][3]*this.initDimensions[n][m][4]>1);
	}
	



	
	
	
	
	
	/** Actions associated to the listener ----------------------------------------------------------------------------------------
 * 
 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(!e.getActionCommand().equals("comboBoxChanged")){
			System.out.println("Starting event : "+e.getActionCommand()+" , "+e.getID()+" with step="+step+" and current action ="+currentRegAction.readableString(false));
		}
		// Listeners for launching interface
		if((e.getSource()==this.sos0Button || e.getSource()==this.runTwoImagesButton || e.getSource()==this.runSerieButton || e.getSource()==this.transformButton || e.getSource()==this.composeTransformsButton)
				&& this.registrationFrame!=null && this.registrationFrame.isVisible()) {
			IJ.showMessage("A Registration manager is running, with the corresponding interface open. Please close this interface before any other operation.");
			return;
		}
				
		if(e.getSource()==this.sos0Button)displaySosMessage(SOS_CONTEXT_LAUNCH);
		else if(e.getSource()==this.runTwoImagesButton) {
			this.registrationWindowMode=REGWINDOWTWOIMG;
			startRegistrationInterface();
			String path1=VitiDialogs.chooseOneRoiPathUI("Select your reference image", "Select your reference image");
			String path2=VitiDialogs.chooseOneRoiPathUI("Select your moving image", "Select your moving image");
			this.setupTwoImagesRegistrationFromTwoPaths(path1, path2);
		}
		else if(e.getSource()==this.runSerieButton) {
			this.registrationWindowMode=REGWINDOWSERIEPROGRAMMING;
			startRegistrationInterface();
			startSerie();
		}
		else if(e.getSource()==this.transformButton) {
			this.registrationWindowMode=REGWINDOWSERIEPROGRAMMING;
			runTransformImage();
		}
		else if(e.getSource()==this.composeTransformsButton) {
			runComposeTransforms();
		}

		
		
		// Listeners for serie programming interface
		else if(e.getSource()==validatePipelineButton) {
			this.pipelineValidated=true;
		}
		else if(e.getSource()==addActionButton) {
			RegistrationAction regAct =new RegistrationAction(this);
			regAct.step=regActions.size();
			regActions.add(regAct);
			this.step=regAct.step;
			currentRegAction=regAct;
			listActions.setSelectedIndex(regAct.step);
			this.updateBoxesAndCurrentAction();
		}
		else if(e.getSource()==removeSelectedButton) {
			if(regActions.size()>1) {
				regActions.remove(regActions.size()-1);
				listActions.setSelectedIndex(Math.min(regActions.size()-1, listActions.getSelectedIndex()));
				currentRegAction=regActions.get( listActions.getSelectedIndex());
				this.step=listActions.getSelectedIndex();
				this.updateBoxesAndCurrentAction();
			}
		}

		
		
		//Listeners for two image registration interface
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run() 
			{
				
				/* Run button is the bigger part : it is the starter/stopper for the main functions*/
				if(e.getSource()==runButton) {
					if(currentRegAction.isDone) {	
						IJ.showMessage("Current action is already done. Nothing to do left.");
						return;
					}
					if((registrationWindowMode==REGWINDOWTWOIMG) &&(transforms[referenceTime][referenceModality].size() > 0) &&  (boxTypeAction.getSelectedIndex()!=2 ) ) {	
						IJ.showMessage("Registration steps cannot be added after an axis alignement step. Use UNDO to return before axis alignement");
						return;
					}
					disable(RUN);
					
					
					
					if( (registrationWindowMode==REGWINDOWSERIERUNNING)&& 
					( currentRegAction.typeAction==RegistrationAction.TYPEACTION_SAVE || currentRegAction.typeAction==RegistrationAction.TYPEACTION_EXPORT || currentRegAction.typeAction==RegistrationAction.TYPEACTION_VIEW ) ){
						if(currentRegAction.typeAction==RegistrationAction.TYPEACTION_SAVE) {
							System.out.println("Saving !");
							currentRegAction.isDone=true;
							step++;trActions.add(new ItkTransform());
							saveSerieToFjmFile();
							step--;trActions.remove(trActions.size()-1);}
						else if(currentRegAction.typeAction==RegistrationAction.TYPEACTION_VIEW) {VitiDialogs.notYet("View NM Serie");}
						else if(currentRegAction.typeAction==RegistrationAction.TYPEACTION_EXPORT) runExportNM();
						addTransformAndAction(new ItkTransform(), 0, 0);
						updateBoxesFromCurrentActionClicked();
						disable(new int[] {BOXACT,BOXDISP,BOXDISPMAN,BOXOPT,BOXTRANS});
						enable(RUN);
						return;
					}
					
					
					
					//TODO : quand il charge, il met automatiquement à MAN la premiere qui vient juste apres
					//TODO : visualisation resultat
					//TODO : export resultat
					//TODO : verifier le bon fonctionnement de alignement
					//TODO : verifier si two images marche toujours
					//TODO : tester la chaine complete avec lancement depuis launching area
					
					
					
					//Automatic registration
					if(((String)(boxTypeAction.getSelectedItem())).equals(textActions[1])){	
						disable(new int[] {SAVE,FINISH,SETTINGS,UNDO,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
						
						//Automatic blockMatching registration
						if(boxOptimizer.getSelectedIndex()==0) {
							runButton.setText("Running Blockmatching...");
							bmRegistration=new BlockMatchingRegistration(images[refTime][refMod],images[movTime][movMod],currentRegAction.typeTrans,MetricType.SQUARED_CORRELATION,
									currentRegAction.sigmaResampling,currentRegAction.sigmaDense , currentRegAction.higherAcc==1 ? -1 : currentRegAction.levelMin,currentRegAction.levelMax,currentRegAction.iterationsBM,
									images[refTime][refMod].getStackSize()/2,null  ,currentRegAction.neighX,currentRegAction.neighZ,
									currentRegAction.bhsX,currentRegAction.bhsZ,currentRegAction.strideX,currentRegAction.strideZ);
							bmRegistration.refRange=imageRanges[refTime][refMod];
							bmRegistration.movRange=imageRanges[movTime][movMod];
							bmRegistration.flagRange=true;
							bmRegistration.percentageBlocksSelectedByScore=currentRegAction.selectScore;
							bmRegistration.minBlockVariance=0.04;
							bmRegistration.displayRegistration=currentRegAction.typeAutoDisplay;
							bmRegistration.displayR2=false;
							bmRegistration.returnComposedTransformationIncludingTheInitialTransformationGiven=false;
							enable(ABORT);
							ItkTransform trTemp=bmRegistration.runBlockMatching(getComposedTransform(movTime,movMod));
							disable(ABORT);
							if(! actionAborted) {
								addTransformAndAction(trTemp,movTime,movMod);
								bmRegistration.closeLastImages();
								bmRegistration.freeMemory();
							}
						}
						
						//Automatic Itk iconic registration
						else {
							runButton.setText("Running Itk registration...");
							itkManager=new ItkRegistrationManager();
							itkManager.refRange=imageRanges[refTime][refMod];
							itkManager.movRange=imageRanges[movTime][movMod];
							itkManager.flagRange=true;
							itkManager.displayRegistration=currentRegAction.typeAutoDisplay;
							itkManager.returnComposedTransformationIncludingTheInitialTransformationGiven=false;
							enable(ABORT);
							System.out.println("STARTING  ITK");
							ItkTransform trTemp=itkManager.runScenarioFromGui(new ItkTransform(),
									images[refTime][refMod],
									getComposedTransform(movTime,movMod).transformImage(images[refTime][refMod], images[movTime][movMod],false), currentRegAction.typeTrans, currentRegAction.levelMin,currentRegAction.levelMax,currentRegAction.iterationsITK,currentRegAction.learningRate);
							disable(ABORT);
							itkManager.freeMemory();
							itkManager=null;
							if(! actionAborted) {
								addTransformAndAction(trTemp,movTime,movMod);
							}
						}
						runButton.setText("Start this action");
						if(!actionAborted)updateViewTwoImages();
						enable(new int[] {RUN,SAVE,FINISH,SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP,UNDO});
						actionAborted=false;
					}
					
					
					
					//Manual registration
					else if(((String)(boxTypeAction.getSelectedItem())).equals(textActions[0])){						
						disable(new int[] {RUN,UNDO});
						//Parameters verification
						if(boxTypeTrans.getSelectedIndex()>0 && boxDisplayMan.getSelectedIndex()==VIEWER_3D) {
							IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform."+
								"If you just intend to compute a rigid transform (no deformation / dilation), please select RIGID in the transformation list. Otherwise, select the 2d viewer in "+
								"the settings to compute a similarity from landmarks points, or select automatic block matching registration to compute a dense vector field");
							enable(new int[] {RUN,UNDO});
							return;
						}
						if(boxTypeTrans.getSelectedIndex()>1 && boxDisplayMan.getSelectedIndex()==VIEWER_2D) {
							IJ.showMessage("Warning : transform is set to Vector field. But the 3d viewer can only compute Rigid and Similarity transform."+
								"Please select automatic block matching registration to compute a dense vector field");
							enable(new int[] {RUN,UNDO});
							return;
						}

						//Starting manual registration
						if(runButton.getText().equals("Start this action")) {
							disable(new int[] {BOXACT,RUN,UNDO});
							runButton.setText("Position ok");
							
							ImagePlus imgMovCurrentState=getComposedTransform(movTime,movMod).transformImage(images[refTime][refMod],images[movTime][movMod],false);
							imgMovCurrentState.setDisplayRange(imageRanges[movTime][movMod][0],imageRanges[movTime][movMod][1]);
							if(boxDisplayMan.getSelectedIndex()==VIEWER_3D) {
								run3dInterface(images[refTime][refMod],imgMovCurrentState);
								setRunToolTip(MANUAL3D);
							}
							else {
								run2dInterface(images[refTime][refMod],imgMovCurrentState);
								setRunToolTip(MANUAL2D);								
							}
							enable(new int[] {ABORT,RUN});
							runButton.setBackground(colorGreenRunButton);
						}
						
						//Finish manual registration
						else {
							//Window verifications and wild abort if needed
							if( ( (boxDisplayMan.getSelectedIndex()==VIEWER_3D) && (universe==null)) || 
							( (boxDisplayMan.getSelectedIndex()==VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage("Image 1")==null) || (WindowManager.getImage("Image 2")==null) ) ) ) {
								disable(new int[] {RUN,ABORT});
								if((WindowManager.getImage("Image 1")!=null)) WindowManager.getImage("Image 1").close();
								if((WindowManager.getImage("Image 2")!=null)) WindowManager.getImage("Image 2").close();
								if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
								runButton.setText("Start this action");
								setRunToolTip(MAIN);
								enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN,UNDO});
								runButton.setBackground(colorStdActivatedButton);
								return;
							}
							
							//Verify number of landmarks, and return if the number of couples is < 5
							if((boxDisplayMan.getSelectedIndex()==VIEWER_2D) && (RoiManager.getInstance().getCount()<10 ) ) {IJ.showMessage("Please identify at least 10 points (5 correspondance couples)");enable(RUN);return;}
							
							
							//Closing manual registration
							disable(new int[] {ABORT,RUN});
							ItkTransform tr=null;
							if(boxDisplayMan.getSelectedIndex()==VIEWER_3D) 	tr=close3dInterface();
							else	tr=close2dInterface();
							addTransformAndAction(tr,movTime,movMod);
					    	updateViewTwoImages();
							runButton.setText("Start this action");
							setRunToolTip(MAIN);
							runButton.setBackground(colorStdActivatedButton);
							enable(new int[] {UNDO,BOXACT,FINISH,SAVE,RUN});
						}
					}
					
				
					//Axis alignment
					else if((boxTypeAction.getSelectedIndex()==2)){						
						disable(RUN);
						if(runButton.getText().equals("Start this action")) {
							if(!developerMode && !VitiDialogs.getYesNoUI("Warning : data finalization", "Warning : image alignment means to transform reference image.\n\n"+
									" For simplicity reasons, this operation ends the registration procedure. No more manual or automatic registration step\n"+
									" can be added after. If after this operation you want to add new registration steps, you will have to undo it before.\n"+
									"\nIf you feel unsafe about what is going on, you can answer \"No\" to abort this operation,\n"+
									" and use the \"Save current state\" button, before running again image alignement. \nConversely, "+
									"answer \"Yes\" to start alignment anyway.\n\nDo you want to start image alignment right now ?")) {enable(RUN);return;}

							//Parameters verification
							if(boxTypeTrans.getSelectedIndex()>0 && boxDisplayMan.getSelectedIndex()==VIEWER_3D) {
								IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform."+
									"If you just intend to compute a rigid transform (no deformation / dilation), please select RIGID in the transformation list. Otherwise, select the 2d viewer in "+
									"the settings to compute a similarity from landmarks points, or select automatic block matching registration to compute a dense vector field");
								enable(new int[] {RUN});
								return;
							}
							if(boxTypeTrans.getSelectedIndex()>1 && boxDisplayMan.getSelectedIndex()==VIEWER_2D) {
								IJ.showMessage("Warning : transform is set to Vector field. But the 3d viewer can only compute Rigid and Similarity transform."+
									"Please select automatic block matching registration to compute a dense vector field");
								enable(new int[] {RUN});
								return;
							}

							//Starting axis alignment
							disable(new int[] {BOXACT,FINISH,SAVE,SETTINGS,UNDO});
							runButton.setText("Axis ok");
							ImagePlus imgRefCurrentState=images[refTime][refMod];
							if(transforms[refTime][refMod].size()>0)imgRefCurrentState=getComposedTransform(refTime,refMod).transformImage(images[refTime][refMod],images[refTime][refMod],false);
							imgRefCurrentState.setDisplayRange(imageRanges[refTime][refMod][0],imageRanges[refTime][refMod][1]);

							if(boxDisplayMan.getSelectedIndex()==VIEWER_3D) {
								run3dInterface(imgRefCurrentState,null);
								setRunToolTip(MANUAL3D);
							}
							else {
								run2dInterface(images[refTime][refMod],null);
								setRunToolTip(MANUAL2D);
							}
							enable(new int[] {ABORT,RUN});
							runButton.setBackground(colorGreenRunButton);
						}

						//Finish axis alignment
						else {
							//Window verifications and wild abort if needed
							if( ( (boxDisplayMan.getSelectedIndex()==VIEWER_3D) && (universe==null)) || 
									( (boxDisplayMan.getSelectedIndex()==VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage("Image 1")==null) || (WindowManager.getImage("Image 2")==null) ) ) ) {
								System.out.println("Wild aborting");
								//Wild aborting procedure
								disable(new int[] {RUN,ABORT,UNDO});
								if((WindowManager.getImage("Image 1")!=null)) {WindowManager.getImage("Image 1").changes=false;WindowManager.getImage("Image 1").close();}
								if((WindowManager.getImage("Image 2")!=null)) {WindowManager.getImage("Image 2").changes=false;WindowManager.getImage("Image 2").close();}
								if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
								runButton.setText("Start this action");
								setRunToolTip(MAIN);
								enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN,UNDO});
								runButton.setBackground(colorStdActivatedButton);
								return;
							}

							//Verify number of landmarks, and return if the number of couples is < 5
							if((boxDisplayMan.getSelectedIndex()==VIEWER_2D) && (RoiManager.getInstance().getCount()<10 ) ) {IJ.showMessage("Please identify at least 10 points (5 correspondance couples)");enable(RUN);return;}

							//Closing axis alignement
							disable(new int[] {RUN,ABORT});
							ItkTransform tr=null;
							System.out.println("HERE JUSTE AVANT");
							if(boxDisplayMan.getSelectedIndex()==VIEWER_3D) {System.out.println("HERE JUSTE JUSTE AVANT");	tr=close3dInterface();}
							else						 	tr=close2dInterface();
							System.out.println("HERE JUSTE JUSTE JUSTEAVANT");							
							currentRegAction.isDone=true;
							addTransformToReference(tr);
							System.out.println("HERE JUSTE JUSTE JUSTE APRES");							
							updateViewTwoImages();
							runButton.setText("Start this action");
							setRunToolTip(MAIN);
							enable(new int[] {UNDO,FINISH,SAVE,SETTINGS,BOXACT,RUN});
							runButton.setBackground(colorStdActivatedButton);
						}
					}
				}
				
				
				/* Abort button, and associated behaviours depending on the context*/
				if(e.getSource()==abortButton) {
					disable(new int[] {RUN,SAVE,FINISH,UNDO, SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
					actionAborted=true;
					System.out.println("Entering aborting procedure");
					
					//Aborting a manual registration or axis alignment procedure
					if(runButton.getText().equals("Position ok") || runButton.getText().equals("Axis ok")){
						disable(new int[] {RUN,ABORT});
						if((WindowManager.getImage("Image 1")!=null)) {WindowManager.getImage("Image 1").changes=false;WindowManager.getImage("Image 1").close();}
						if((WindowManager.getImage("Image 2")!=null)) {WindowManager.getImage("Image 2").changes=false;WindowManager.getImage("Image 2").close();}
						if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
						if(boxDisplayMan.getSelectedIndex()==VIEWER_3D && universe !=null) {universe.close();universe=null;}
						runButton.setText("Start this action");
						setRunToolTip(MAIN);
						enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,BOXTRANS,RUN,UNDO});
						runButton.setBackground(colorStdActivatedButton);
					}
					//Aborting automatic blockmatching registration, killing threads, and checking if threads are deads
					else if(runButton.getText().equals("Running Blockmatching...")){
						System.out.println("aborting bockmatching");
						int nThreads=bmRegistration.threads.length;
						while(!bmRegistration.bmIsInterruptedSucceeded) {
							VitimageUtils.waitFor(100);
							bmRegistration.bmIsInterrupted=true;
							for(int th=0;th<nThreads;th++)bmRegistration.threads[th].interrupt();
						}
					}
					//Aborting automatic itk iconic registration, killing threads, and checking if threads are deads
					else if(runButton.getText().equals("Running Itk registration...")){
						System.out.println("aborting itk registration");
						int trial=0;
						while(itkManager.registrationThread!=null && itkManager.registrationThread.isAlive() && trial<100) {
							trial++;
							System.out.println("Trying !");
							VitimageUtils.waitFor(100);	
							itkManager.itkRegistrationInterrupted=true;
							itkManager.registrationThread.stop();
						}
					}
					runButton.setText("Start this action");
				}				

				
				
				
				
				/*Others buttons : minor and easy functions, settings and parameters modification*/		
				if(e.getSource()==settingsButton) {
					openSettingsDialog();					
					updateEstimatedTime();
				}
				if(e.getSource()==settingsDefaultButton) {
					currentRegAction.defineSettingsFromTwoImages(images[refTime][refMod],images[movTime][movMod],getRegistrationManager(),true);	
					updateBoxesAndCurrentAction();
					updateEstimatedTime();
				}

				if(!comboBoxChanged && (e.getSource()==boxTypeAction || e.getSource()==boxOptimizer  || e.getSource()==boxTypeTrans || e.getSource()==boxDisplay || e.getSource()==boxDisplayMan)) {		
					comboBoxChanged=true;
					updateBoxesAndCurrentAction();
					comboBoxChanged=false;
					updateEstimatedTime();
				}
				
				if(e.getSource()==finishButton  ||  e.getSource()==saveButton) {
					disable(new int[] {RUN,FINISH,SAVE,UNDO});
					if(e.getSource()==finishButton)runExportTwoImages();
					else runSave();
					enable(new int[] {FINISH,SAVE,RUN});
					enable(UNDO);
				}
				if(e.getSource()==undoButton && step>0) {
					disable(new int[] {RUN,SAVE,FINISH,UNDO});
					System.out.println("Start an undo action");
					runUndo();
					enable(new int[] {RUN,SAVE,FINISH,BOXACT,UNDO});
					if(step==0)disable(new int[] {UNDO,SAVE,FINISH});
				}				
				if(e.getSource()==sos1Button) {
					runSos();
				}			
			}
		});
		System.gc();
	}


	
	
	
	

	
	
	
	/** Structures updating routines--------------------------------------------------------------------------
	 * 
	 */
	public void addTransformAndAction(ItkTransform tr,int nt,int nm) {
		currentRegAction.isDone=true;
		RegistrationAction tmpRegOld=currentRegAction;
		tr.step=this.step;
		currentRegAction.movMod=nm;
		currentRegAction.movTime=nt;
		currentRegAction.isDone=true;
		this.transforms[nt][nm].add(tr);
		this.trActions.add(tr);
		if(registrationWindowMode==REGWINDOWTWOIMG) {
			step++;
			RegistrationAction tmpAction=new RegistrationAction(currentRegAction);
			tmpAction.step=currentRegAction.step+1;
			currentRegAction=tmpAction;
			this.regActions.add(currentRegAction);
			this.listActions.setSelectedIndex(step);
			System.out.println("Ending addTransform and action. Size of regActions after : "+regActions.size()+"going to step "+step);
			this.updateList();
			enable(UNDO);
		}
		else if(registrationWindowMode==REGWINDOWSERIERUNNING) {
			if(regActions.size()==(step+1)){
				disable(RUN);
				IJ.showMessage("Serie is finished !");
			}
			step++;
			currentRegAction=regActions.get(step);
			listActions.setSelectedIndex(step);
			updateList();
			addLog("Action finished : "+tmpRegOld.readableString(false),0);
			addLog("",0);
			addLog("--> Waiting for you to start next action : "+currentRegAction.readableString(false),0);
		}
	}
	
	

	
	
	public void addTransformToReference(ItkTransform tr) {
		addTransformAndAction(tr,this.referenceTime,this.referenceModality);
	}
		
	public ItkTransform getComposedTransform(int nt, int nm) {
		if (nt>=this.nTimes)return null;
		if (nm>=this.nMods)return null;
		if (this.transforms[nt][nm].size()==0)return new ItkTransform();
		ItkTransform trTot=new ItkTransform();
		for(int indT=0;indT<this.transforms[nt][nm].size();indT++)trTot.addTransform(this.transforms[nt][nm].get(indT));
		return trTot;
	}
	
	public void runUndo() {
		System.out.println("DEBUG RUNUNDO");
		System.out.println("STEP="+step);
		//Identify the next transforms to be undone, and undone it
		WindowManager.getImage("Registration results after "+(step)+" step"+((step>1)?"s":"")).changes=false;
		WindowManager.getImage("Registration results after "+(step)+" step"+((step>1)?"s":"")).close();
		this.imgView=(step>1) ? WindowManager.getImage("Registration results after "+(step-1)+" step"+(((step-1)>1)?"s":"")) : WindowManager.getImage("Superimposition before registration");
		int nt=regActions.get(step-1).movTime;
		int nm=regActions.get(step-1).movMod;

		int index=this.transforms[nt][nm].size()-1;
		this.transforms[nt][nm].remove(index);
		this.trActions.remove(step-1);
		this.regActions.remove(this.regActions.size()-1);
		this.currentRegAction=this.regActions.get(this.regActions.size()-1);
		step--;
		this.updateList();
		if(this.imgView==null)updateViewTwoImages();
	}
	
	
	public void updateBoxesFromCurrentActionClicked() {
		disable(new int[] {BOXACT,BOXDISP,BOXDISPMAN,BOXOPT,BOXTRANS,SETTINGS});
		if(listActions.getSelectedIndex()>=regActions.size() || (!regActions.get(listActions.getSelectedIndex()).isTransformationAction()) ) {
			System.out.println("Index "+listActions.getSelectedIndex()+" pas good. Voie 1");
			return;
		}
		else {
			System.out.println("Index "+listActions.getSelectedIndex()+" good. Voie 2");
			this.currentRegAction=regActions.get(listActions.getSelectedIndex());
			this.step=listActions.getSelectedIndex();
		}
		boxTypeAction.setSelectedIndex(currentRegAction.typeAction);
		boxTypeTrans.setSelectedIndex(currentRegAction.typeTrans==Transform3DType.DENSE ? 2 : currentRegAction.typeTrans==Transform3DType.RIGID ? 0 : 1);
		boxDisplay.setSelectedIndex(currentRegAction.typeAutoDisplay);
		boxDisplayMan.setSelectedIndex(currentRegAction.typeManViewer);
		boxOptimizer.setSelectedIndex(currentRegAction.typeOpt==OptimizerType.BLOCKMATCHING ? 0 : 1);
		VitimageUtils.waitFor(20);
		updateBoxFieldsToCoherence();
		VitimageUtils.waitFor(20);
		enable(new int[] {BOXACT,BOXTRANS,SETTINGS});
		setState(new int[] {BOXOPT,BOXDISP },boxTypeAction.getSelectedIndex()==1);
		setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
	}
	
	public void updateBoxesAndCurrentAction() {
			setState(new int[] {BOXOPT,BOXTIME,BOXDISP,BOXTRANS,BOXACT},false);
		updateBoxFieldsToCoherence();

		currentRegAction.adjustSettingsFromManager(this);
		updateList();
		VitimageUtils.waitFor(5);
		enable(new int[] {BOXACT,BOXTRANS});
		setState(new int[] {BOXOPT,BOXDISP },boxTypeAction.getSelectedIndex()==1);
		setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
	}
		
	

	public void updateBoxFieldsToCoherence() {
		int valDisp=boxDisplay.getSelectedIndex();		
		int valTrans=boxTypeTrans.getSelectedIndex();		
		DefaultComboBoxModel<String> listModelDisp = new DefaultComboBoxModel<String>();
		DefaultComboBoxModel<String> listModelTrans = new DefaultComboBoxModel<String>();
		if(boxTypeAction.getSelectedIndex()==1 && boxOptimizer.getSelectedIndex()==0) {
	        for(int i=0;i<textDisplayBM.length;i++)listModelDisp.addElement(textDisplayBM[i]);
	        for(int i=0;i<textTransformsBM.length;i++)listModelTrans.addElement(textTransformsBM[i]);
			this.boxDisplay.setModel(listModelDisp);
			this.boxDisplay.setSelectedIndex(valDisp);
			this.boxTypeTrans.setModel(listModelTrans);
			this.boxTypeTrans.setSelectedIndex(valTrans);
		}
		else if(boxTypeAction.getSelectedIndex()==1 && boxOptimizer.getSelectedIndex()==1) {
			for(int i=0;i<textDisplayITK.length;i++)listModelDisp.addElement(textDisplayITK[i]);
	        for(int i=0;i<textTransformsITK.length;i++)listModelTrans.addElement(textTransformsITK[i]);
			this.boxDisplay.setModel(listModelDisp);
			this.boxDisplay.setSelectedIndex(Math.min(valDisp,textDisplayITK.length-1));
			this.boxTypeTrans.setModel(listModelTrans);
			this.boxTypeTrans.setSelectedIndex(Math.min(valTrans,textTransformsITK.length-1));
		}
		else if(boxTypeAction.getSelectedIndex()==0) {
	        for(int i=0;i<textTransformsMAN.length;i++)listModelTrans.addElement(textTransformsMAN[i]);
			this.boxTypeTrans.setModel(listModelTrans);
			this.boxTypeTrans.setSelectedIndex(Math.min(valTrans,textTransformsMAN.length-1));
		}
		else if(boxTypeAction.getSelectedIndex()==2) {
	        for(int i=0;i<textTransformsALIGN.length;i++)listModelTrans.addElement(textTransformsALIGN[i]);
			this.boxTypeTrans.setModel(listModelTrans);
			this.boxTypeTrans.setSelectedIndex(Math.min(valTrans,textTransformsALIGN.length-1));
		}
	}

                                     
	
	
	
	
	
	/** Manual registration and axis alignment routines ---------------------------------------------------------------
	 */
	@SuppressWarnings("deprecation")
	public void run3dInterface(ImagePlus imgRef,ImagePlus imgMov) {
		ImagePlus refCopy=VitimageUtils.imageCopy(imgRef);
		IJ.run(refCopy,"8-bit","");
		this.universe=new ij3d.Image3DUniverse();
		universe.show();

		if(imgMov!=null) {
			ImagePlus movCopy=VitimageUtils.imageCopy(imgMov);		
			movCopy.resetDisplayRange();
			IJ.run(movCopy,"8-bit","");
			universe.addContent(refCopy, new Color3f(Color.red),"refCopy",50,new boolean[] {true,true,true},1,0 );
			universe.addContent(movCopy, new Color3f(Color.green),"movCopy",50,new boolean[] {true,true,true},1,0 );
		}
		else {
			ImagePlus movCopy=VitimageUtils.getBinaryGrid(refCopy,17);
			universe.addContent(refCopy, new Color3f(Color.red),"movCopy",50,new boolean[] {true,true,true},1,0 );
			universe.addOrthoslice(movCopy, new Color3f(Color.white),"refCopy",50,new boolean[] {true,true,true},1);
		}
		ij3d.ImageJ3DViewer.select("refCopy");
		ij3d.ImageJ3DViewer.lock();
		ij3d.ImageJ3DViewer.select("movCopy");
		if(this.registrationWindowMode==this.REGWINDOWTWOIMG) {
			universe.setSize(this.lastViewSizes[0], this.lastViewSizes[1]);
			VitimageUtils.adjustFrameOnScreenRelative((Frame)((JPanel)(this.universe.getCanvas().getParent())).getParent().getParent().getParent(),this.imgView.getWindow(),3,3,10);
		}
		else VitimageUtils.adjustFrameOnScreenRelative((Frame)((JPanel)(this.universe.getCanvas().getParent())).getParent().getParent().getParent(),this.registrationFrame,0,0,10);
		if(!debugMode && first3dmessage)IJ.showMessage("Move the green volume to match the red one\nWhen done, push the \""+runButton.getText()+"\" button to stop\n\nCommands : \n"+
				"mouse-drag the green object to turn the object\nmouse-drag the background to turn the scene\nCTRL-drag to translate the green object");
		first3dmessage=false;
		addLog(" Waiting for you to confirm position or to abort action...",0);
	}
	
	public ItkTransform close3dInterface(){
		Transform3D tr=new Transform3D();
		double[]tab=new double[16];
		universe.getContent("movCopy").getLocalRotate().getTransform(tr);		tr.get(tab);
		ItkTransform itRot=ItkTransform.array16ElementsToItkTransform(tab);
		universe.getContent("movCopy").getLocalTranslate().getTransform(tr);		tr.get(tab);
		ItkTransform itTrans=ItkTransform.array16ElementsToItkTransform(tab);
		itTrans.addTransform(itRot);
		itTrans=itTrans.simplify();
		ItkTransform ret=new ItkTransform(itTrans.getInverse());
		universe.removeAllContents();
		universe.close();
    	universe=null;    
    	return ret;		
	}
	
	public void run2dInterface(ImagePlus imgRef,ImagePlus imgMov) {
		ImagePlus refCopy=VitimageUtils.imageCopy(imgRef);
		ImagePlus movCopy=null;
		IJ.run(refCopy,"8-bit","");
		refCopy.setTitle("Image 1");
		if(imgMov!=null) {
			movCopy=VitimageUtils.imageCopy(imgMov);		
			movCopy.resetDisplayRange();
			IJ.run(movCopy,"8-bit","");
			movCopy.setTitle("Image 2");
		}
		else {
			movCopy=VitimageUtils.getBinaryGrid(refCopy,17);
			movCopy.show();
			movCopy.setTitle("Image 2");
		}

		
		IJ.setTool("point");
		refCopy.show();refCopy.setSlice(refCopy.getStackSize()/2+1);refCopy.updateAndRepaintWindow();
		VitimageUtils.adjustFrameOnScreen((Frame)WindowManager.getWindow("Image 1"), 0,2);
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		VitimageUtils.adjustFrameOnScreenRelative((Frame)rm,(Frame)WindowManager.getWindow("Image 1"),2,1,2);
		movCopy.show();movCopy.setSlice(movCopy.getStackSize()/2+1);movCopy.updateAndRepaintWindow();
		VitimageUtils.adjustFrameOnScreenRelative((Frame)WindowManager.getWindow("Image 2"),rm,2,2,2);
		if(!debugMode && first2dmessage)IJ.showMessage("Examine images, identify correspondances between images and use the Roi manager to build a list of correspondances points. Points should be given this way : \n- Point A  in image 1\n- Correspondant of point A  in image 2\n- Point B  in image 1\n- Correspondant of point B  in image 2\netc...\n"+
		"Once done (at least 5-15 couples of corresponding points), push the \""+runButton.getText()+"\" button to stop\n\n");
		first2dmessage=false;
		addLog(" Waiting for you to confirm position or to abort action...",0);
	}
	
	public ItkTransform close2dInterface(){
		RoiManager rm=RoiManager.getRoiManager();
		Point3d[][]pointTab=convertLandmarksToRealSpacePoints(WindowManager.getImage("Image 1"),WindowManager.getImage("Image 2"));
		WindowManager.getImage("Image 1").changes=false;
		WindowManager.getImage("Image 2").changes=false;
		WindowManager.getImage("Image 1").close();
		WindowManager.getImage("Image 2").close();
		ItkTransform trans=null;
		if(this.boxTypeAction.getSelectedIndex()==0) {
			if(this.boxTypeTrans.getSelectedIndex()==0)trans=ItkTransform.estimateBestRigid3D(pointTab[1],pointTab[0]);
			if(this.boxTypeTrans.getSelectedIndex()==1)trans=ItkTransform.estimateBestSimilarity3D(pointTab[1],pointTab[0]);
		}
		if(this.boxTypeAction.getSelectedIndex()==2) {
			if(this.boxTypeTrans.getSelectedIndex()==0)trans=ItkTransform.estimateBestRigid3D(pointTab[0],pointTab[1]);
			if(this.boxTypeTrans.getSelectedIndex()==1)trans=ItkTransform.estimateBestSimilarity3D(pointTab[0],pointTab[1]);
		}
		rm.close();
    	return trans;		
	}
	
	public Point3d[][] convertLandmarksToRealSpacePoints(ImagePlus imgRef,ImagePlus imgMov){
		RoiManager rm=RoiManager.getRoiManager();
		int nbCouples=rm.getCount()/2;
		Point3d[]pRef=new Point3d[nbCouples];
		Point3d[]pMov=new Point3d[nbCouples];
		IJ.setTool("point");
		for(int indP=0;indP<rm.getCount()/2;indP++){
			pRef[indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			pMov[indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			pRef[indP]=TransformUtils.convertPointToRealSpace(pRef[indP],imgRef);
			pMov[indP]=TransformUtils.convertPointToRealSpace(pMov[indP],imgMov);
		}
		return new Point3d[][] {pRef,pMov};
	}

	
	
	
	
	
	
	
	/** Export-Import-Save routines for Series, images and transforms----------------------------------------------------------------------------------
	*
	*/	
	public void saveSerieToFjmFile() {		
		System.out.println("ENTERING WRITING and saving step ="+this.step);
		//Read informations from ijm file : main fields
		String str="";
		str+="#Fijiyama save###\n";
		str+="#Name="+this.name+"\n";
		str+="#Mode=Serie\n";
		str+="#Output path="+this.serieOutputPath+"\n";
		str+="#Input path="+this.serieInputPath +"\n";
		str+="#Reference Time="+this.referenceTime+"\n";
		str+="#Reference Modality="+this.referenceModality +"\n";
		str+="#Step="+ this.step+"\n";
		str+="#"+""+"\n";
		str+="#Nmods="+this.nMods+"\n";
		for(int i=0;i<nMods;i++)str+="#Mod"+i+"="+this.mods[i]+"\n";
		str+="#"+""+"\n";
		str+="#Ntimes="+this.nTimes+"\n";
		for(int i=0;i<nTimes;i++)str+="#Time"+i+"="+this.times[i]+"\n";
		str+="#"+"" +"\n";
		str+="#Expression="+this.expression +"\n";
		str+="#Nsteps="+this.nSteps+"\n";
		
		//All RegistrationAction serialized .ser object, and associated transform for those already computed
		System.out.println("Starting writing registration files");
		File dirReg=new File(this.serieOutputPath,"Registration_files");		
		for(int st=0;st<this.nSteps;st++) {
			RegistrationAction regTemp=this.regActions.get(st);
			System.out.print(" proceeding "+regTemp.readableString()+"  writing ser");
			regTemp.writeToFile(new File(dirReg,"RegistrationAction_Step_"+st+".ser").getAbsolutePath());
			str+="#"+regTemp.readableString()+"\n";
			File f=new File(dirReg,"Transform_Step_"+st+".txt");
			if(st<this.step) {
				System.out.print(" writing transform also");
				if(regTemp.typeTrans!=Transform3DType.DENSE) 					trActions.get(st).writeMatrixTransformToFile(f.getAbsolutePath());
				else trActions.get(st).writeAsDenseField(f.getAbsolutePath(),this.images[regTemp.refTime][regTemp.refMod]);
				System.out.println(" transform written");
			}
		}
		str+="#"+""+"\n";		
		VitimageUtils.writeStringInFile(str, new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath());
		System.out.println("Writing Fjm serie done.");
	}

	
	public void loadFromFjmFile(String pathToFile) {
		String []names;
		System.out.println("Loading configuration from file "+pathToFile);
		if(pathToFile==null || (!(new File(pathToFile).exists())) || (!(pathToFile.substring(pathToFile.length()-4,pathToFile.length())).equals(".fjm"))) {
			names=VitiDialogs.openFileUI("Select a previous Fijiyama experiment","","fjm");
			if(names==null || (!(new File(names[0],names[1]).exists())) || (!(names[1].substring(names[1].length()-4,names[1].length())).equals(".fjm") ) ) {
				IJ.showMessage("Wrong file. Save cannot be done");
				return;
			}
		}
		else names=new String[] {new File(pathToFile).getParent(),new File(pathToFile).getName()};	
		String modString=VitimageUtils.readStringFromFile(new File(names[0],names[1]).getAbsolutePath()).split("\n")[2].split("=")[1];
		this.name=names[1].substring(0,names[1].length()-4);
		this.serieOutputPath=names[0];
		if(modString.charAt(0)=='S') {
			loadFromSerieFjmFile();
		}
		else loadFromTwoImagesFjmFile();
	}

	
	public void loadFromSerieFjmFile() {
		//Open file and prepare structures
		this.registrationWindowMode=REGWINDOWSERIEPROGRAMMING;
		this.mode=MODE_SERIE;
		
		//Read informations from ijm file : main fields
		String str=VitimageUtils.readStringFromFile(new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath());
		String[]lines=str.split("\n");
		this.serieOutputPath=(lines[3].split("=")[1]);
		this.serieInputPath=(lines[4].split("=")[1]);
		this.referenceTime=Integer.parseInt(lines[5].split("=")[1]);
		this.referenceModality=Integer.parseInt(lines[6].split("=")[1]);
		this.step=Integer.parseInt(lines[7].split("=")[1]);

		//Read modalities
		this.nMods=Integer.parseInt(lines[9].split("=")[1]);
		this.mods=new String[this.nMods];
		for(int i=0;i<this.nMods;i++)this.mods[i]=lines[10+i].split("=")[1];
		
		//Read times and nSteps
		this.nTimes=Integer.parseInt(lines[11+this.nMods].split("=")[1]);
		this.times=new String[this.nTimes];
		for(int i=0;i<this.nTimes;i++)this.times[i]=lines[12+this.nMods+i].split("=")[1];
		this.expression=lines[13+this.nMods+this.nTimes].split("=")[1];
		this.nSteps=Integer.parseInt(lines[14+this.nMods+this.nTimes].split("=")[1]);
		this.stepExecuted=new boolean[this.nSteps];
		//setupStructures
		this.setupStructures();

		//Affecter les paths
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				File f=new File(this.serieInputPath,expression.replace("{Time}", times[nt]).replace("{ModalityName}",mods[nm]));
				System.out.print("Checking existence of image "+f.getAbsolutePath());
				if(f.exists()) {
					System.out.println(" ... Found !");
					this.paths[nt][nm]=f.getAbsolutePath();
				}
				else System.out.println(" ... not found !");
			}
		}
		
		//Check computer capacity and readImages
		this.checkComputerCapacity();
		this.openImagesAndCheckOversizing();

		
		//Read the steps : all RegistrationAction serialized .ser object, and associated transform for those already computed
		File dirReg=new File(this.serieOutputPath,"Registration_files");
		
		for(int st=0;st<this.nSteps;st++) {
			File f=new File(dirReg,"RegistrationAction_Step_"+st+".ser");
			RegistrationAction regTemp=RegistrationAction.readFromFile(f.getAbsolutePath());
			this.regActions.add(regTemp);
			f=new File(dirReg,"Transform_Step_"+st+".txt");
			if(st<this.step) {
				ItkTransform trTemp;
				if(regTemp.typeTrans!=Transform3DType.DENSE)trTemp=ItkTransform.readTransformFromFile(f.getAbsolutePath());
				else trTemp=ItkTransform.readAsDenseField(f.getAbsolutePath());
				addTransformAndAction(trTemp,regTemp.movTime,regTemp.movMod);
			}
		}
		System.out.println("Reading actions and transforms done.");
	}
	
	
	public void runExportNM(){
		VitiDialogs.notYet("RUN EXPORT NM");//TODO
	}

	public void runExportTwoImages() {
		//Ask for target dimensions
		ImagePlus refResult = null,movResult=null;
		GenericDialog gd=new GenericDialog("Choose target dimensions...");
		gd.addMessage("Initial dimensions : ");
		gd.addMessage("..Reference image : "+this.initDimensions[refTime][refMod][0]+" x "+this.initDimensions[refTime][refMod][1]+" x "+this.initDimensions[refTime][refMod][2]+" x "+this.initDimensions[refTime][refMod][3]+" x "+this.initDimensions[refTime][refMod][4]+
				" avec des voxels "+VitimageUtils.dou(this.initVoxs[refTime][refMod][0],6)+" x "+VitimageUtils.dou(this.initVoxs[refTime][refMod][1],6)+" x "+VitimageUtils.dou(this.initVoxs[refTime][refMod][2],6));
		gd.addMessage("..Moving image : "+this.initDimensions[movTime][movMod][0]+" x "+this.initDimensions[movTime][movMod][1]+" x "+this.initDimensions[movTime][movMod][2]+" x "+this.initDimensions[movTime][movMod][4]+" x "+this.initDimensions[movTime][movMod][4]+
				" avec des voxels "+VitimageUtils.dou(this.initVoxs[movTime][movMod][0],6)+" x "+VitimageUtils.dou(this.initVoxs[movTime][movMod][1],6)+" x "+VitimageUtils.dou(this.initVoxs[movTime][movMod][2],6));
		gd.addMessage("\nChoose the target dimensions to export both images");
		gd.addMessage("If you feel lost, consider choosing the first option : Dimensions of reference image.\n.\n.");
		gd.addMessage("After this operation, you can combine reference and moving images with the Color / merge channels tool of ImageJ)");
		gd.addChoice("Choose target dimensions", new String[] {"Dimensions of reference image","Dimensions of moving image","Provide custom dimensions"}, "Dimensions of reference image");
		gd.showDialog();
        if (gd.wasCanceled()) return;	        
		int choice=gd.getNextChoiceIndex();
		ItkTransform transformMov=getComposedTransform(movTime,movMod);
		ItkTransform transformRef=getComposedTransform(refTime, refMod);
		ItkTransform transformMovNoAxis=getComposedTransform(movTime,movMod);
		if(transforms[referenceTime][referenceModality].size()>0)transformMov.addTransform(new ItkTransform(transformRef));
		if(choice==0) {
			refResult=transformRef.transformImage(this.images[refTime][refMod], isHyperImage(refTime, refMod) ? IJ.openImage(this.paths[refTime][refMod]) : this.images[refTime][refMod], false);
			movResult=transformMov.transformImage(this.images[refTime][refMod], isHyperImage(movTime,movMod) ? IJ.openImage(this.paths[movTime][movMod]) : this.images[movTime][movMod], false);
		}
		if(choice==1) {
			refResult=transformRef.transformImage(this.images[movTime][movMod], isHyperImage(refTime, refMod) ? IJ.openImage(this.paths[refTime][refMod]) : this.images[refTime][refMod], false);
			movResult=transformMov.transformImage(this.images[movTime][movMod], isHyperImage(movTime,movMod) ? IJ.openImage(this.paths[movTime][movMod]) : this.images[movTime][movMod], false);
		}

		
		if(choice==2) {
			GenericDialog gd2=new GenericDialog("Provide custom dimensions (and voxel sizes)");
			gd2.addNumericField("Dimension along X",1, 0);
			gd2.addNumericField("Dimension along Y",1, 0);
			gd2.addNumericField("Dimension along Z",1, 0);
			gd2.addNumericField("Voxel size along X",1, 6);
			gd2.addNumericField("Voxel size along Y",1, 6);
			gd2.addNumericField("Voxel size along Z",1, 6);					
			gd2.showDialog();
	        if (gd2.wasCanceled()) return;	        
	        int[]outputDimensions=new int[] {(int)Math.round(gd2.getNextNumber()),(int)Math.round(gd2.getNextNumber()),(int)Math.round(gd2.getNextNumber())};
	        double[]outputVoxs=new double[] {gd2.getNextNumber(),gd2.getNextNumber(),gd2.getNextNumber()};
//	        transformRef.transformImage(outputDimensions, outputVoxs,IJ.openImage(this.paths[refTime][refMod]), false);
			refResult=transformRef.transformImage(outputDimensions, outputVoxs,isHyperImage(refTime, refMod) ? IJ.openImage(this.paths[refTime][refMod]) : this.images[refTime][refMod], false);
			movResult=transformMov.transformImage(outputDimensions, outputVoxs,isHyperImage(movTime,movMod) ? IJ.openImage(this.paths[movTime][movMod]) : this.images[movTime][movMod], false);
		}
		if(transforms[referenceTime][referenceModality].size()>0){
			VitiDialogs.saveImageUI(refResult, "Save reference image", false, "", "image_ref_axis_aligned.tif");
			VitiDialogs.saveImageUI(movResult, "Save moving image", false, "", "image_mov_registered_with_axis_aligned.tif");
		}
		else {
			VitiDialogs.saveImageUI(refResult, "Save reference image", false, "", "image_ref.tif");
			VitiDialogs.saveImageUI(movResult, "Save moving image", false, "", "image_mov_registered.tif");
		}
		if(transforms[referenceTime][referenceModality].size()>0)VitiDialogs.saveMatrixTransformUI(transformRef.simplify(), "Save transform applied to reference image (for axis alignement)", false, "", "transform_ref_axis_alignment.txt");
		if(transformMov.isDense()) {
			if(transforms[referenceTime][referenceModality].size()>0) {
				VitiDialogs.saveDenseFieldTransformUI(transformMov.flattenDenseField(refResult),  "Save global transform applied to moving image", false, "", "transform_mov_to_reference_with_axis_aligned.txt", refResult);	
			}
			else VitiDialogs.saveDenseFieldTransformUI(transformMov.flattenDenseField(refResult),  "Save global transform applied to moving image", false, "", "transform_mov_to_reference.txt", refResult);	
			if(transforms[referenceTime][referenceModality].size()>0)VitiDialogs.saveDenseFieldTransformUI(transformMovNoAxis.flattenDenseField(refResult),  "Save transform applied to moving image without axis alignment", false, "", "transform_mov_to_reference_without_axis_alignment.txt", refResult);	
		}
		else {
			if(transforms[referenceTime][referenceModality].size()>0) {
				VitiDialogs.saveMatrixTransformUI(transformMov.simplify(), "Save global transform applied to moving image", false, "", "transform_mov_to_reference_with_axis_aligned.txt");
			}
			else VitiDialogs.saveMatrixTransformUI(transformMov.simplify(), "Save global transform applied to moving image", false, "", "transform_mov_to_reference.txt");
			if(transforms[referenceTime][referenceModality].size()>0)VitiDialogs.saveMatrixTransformUI(transformMovNoAxis.simplify(), "Save transform applied to moving image without axis alignment", false, "", "transform_mov_to_reference_without_axis_alignment.txt");
		}
		System.out.println("Export finished.");
	}
	
	public void loadFromTwoImagesFjmFile() {
		this.registrationWindowMode=REGWINDOWTWOIMG;
		this.mode=MODE_TWO_IMAGES;
		this.nTimes=1;
		this.nMods=2;
		setupStructures();
		checkComputerCapacity();
		
		//Read informations in main file
		String str=VitimageUtils.readStringFromFile(new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath());
		String[]lines=str.split("\n");
		this.serieOutputPath=(lines[3].split("=")[1]);
		File dirReg=new File(this.serieOutputPath,"Registration_files");
		this.paths[refTime][refMod]=(lines[4].split("=")[1]);
		this.paths[movTime][movMod]=(lines[5].split("=")[1]);
		int tempStep=Integer.parseInt(lines[6].split("=")[1]);

		//Read the already executed steps : RegistrationAction serialized .ser object, and associated transform 
		for(int st=0;st<tempStep;st++) {
			File f=new File(dirReg,"RegistrationAction_Step_"+st+".ser");
			RegistrationAction regTemp=RegistrationAction.readFromFile(f.getAbsolutePath());
			f=new File(dirReg,"Transform_Step_"+st+".txt");
			ItkTransform trTemp;
			if(regTemp.typeTrans!=Transform3DType.DENSE)trTemp=ItkTransform.readTransformFromFile(f.getAbsolutePath());
			else trTemp=ItkTransform.readAsDenseField(f.getAbsolutePath());
			this.currentRegAction=regTemp;
			this.regActions.set(st, regTemp);
			addTransformAndAction(trTemp,regTemp.movTime,regTemp.movMod);
		}
		System.out.println("Reading done.");
	}
	
	public void runSave() {
		System.out.println("DEBUG RUN SAVE mode ="+mode);
		if(mode==MODE_TWO_IMAGES)runSaveTwoImages();
		else saveSerieToFjmFile();
	}


	public void runSaveTwoImages() {
		String mainDir=VitiDialogs.chooseDirectoryUI("Select a directory to save the plugin state", "This place");
		if(mainDir==null ||(!(new File(mainDir).exists()))) {IJ.showMessage("Wrong file. Save cannot be done");return;}
		this.name=VitiDialogs.getStringUI("Choose a name for your serie (accepted characters : alphanumeric, underscore and minus)","Serie name",this.name,true);
		System.out.println("Name="+this.name);
		this.serieOutputPath=new File(mainDir,this.name).getAbsolutePath();

		//Write informations in filePath
		String str="#Fijiyama save###\n"+
				"#Mode="+(mode==MODE_TWO_IMAGES ? "Two_imgs" : "Serie")+"\n"+
				"#Name="+this.name+"\n"+
				"#Output path="+this.serieOutputPath+"\n"+
				"#Path to reference image="+this.paths[refTime][refMod]+"\n"+
				"#Path to moving image="+this.paths[movTime][movMod]+"\n"+
				"#Step="+step+"\n"+
				"#Previous actions=\n";
		for(int indAct=0;indAct<this.regActions.size()-1;indAct++) {
			str+="#"+regActions.get(indAct).readableString()+"\n";
		}
		str+="#\n";
		
		//Save data in a directory
		File dirDataReg=new File(this.serieOutputPath,"Registration_files");
		dirDataReg.mkdirs();
		VitimageUtils.writeStringInFile(str, new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath() );
		for(int indAct=0;indAct<this.regActions.size()-1;indAct++) {
			RegistrationAction regTemp=regActions.get(indAct);
			int ntRef=regTemp.refTime;
			int nmRef=regTemp.refMod;
			String fileName="RegistrationAction_Step_"+indAct+".ser";
			File out=new File(dirDataReg,fileName);
			regTemp.writeToFile(out.getAbsolutePath());
			ItkTransform tr=trActions.get(indAct);
			fileName="Transform_Step_"+indAct+(".txt");
			out=new File(dirDataReg,fileName);
			if(tr.isDense())tr.writeAsDenseField(out.getAbsolutePath(), images[ntRef][nmRef]);
			else tr.writeMatrixTransformToFile(out.getAbsolutePath());
		}
		System.out.println("Saving ok");
	}
	
	public void runTransformImage() {
		ImagePlus imgMov=VitiDialogs.chooseOneImageUI("Select the image to transform (moving image)","Select the image to transform (moving image)");
		if(imgMov==null) {IJ.showMessage("Moving image does not exist. Abort.");return;}
		ImagePlus imgRef=VitiDialogs.chooseOneImageUI("Select the reference image, giving output dimensions (it can be the same)","Select the reference image, giving output dimensions (it can be the same)");
		ItkTransform tr=VitiDialogs.chooseOneTransformsUI("Select the transform to apply , .txt for Itk linear and .transform.tif for Itk dense", "", false);
		if(tr==null) {IJ.showMessage("No transform given. Abort");return;}
		if(imgRef==null) {IJ.showMessage("No reference image provided. Moving image will be used as reference image.");imgRef=VitimageUtils.imageCopy(imgMov);}
		ImagePlus result=tr.transformImage(imgRef, imgMov, false);
		result.setTitle("Transformed image");
		result.show();
	}
	
	public void runComposeTransforms() {
		ArrayList<ItkTransform> listTr=new ArrayList<ItkTransform>();
		boolean oneMore=true;
		int num=1;
		while(oneMore) {
			ItkTransform tr=VitiDialogs.chooseOneTransformsUI("Select the transform #"+(num++)+" , .txt for Itk linear and .transform.tif for Itk dense", "", false);
			if(tr==null) {tr=new ItkTransform();IJ.showMessage("Warning : you included a bad transform file. It was replaced by an identity transform\nin order the process is to be interrupted");}
			listTr.add(tr);
			oneMore=VitiDialogs.getYesNoUI("One more transform ?","One more transform ?");
		}
		ItkTransform trGlob=new ItkTransform();
		for(int i=0;i<listTr.size();i++)trGlob.addTransform(listTr.get(i));
		if(trGlob.isDense()) {
			ImagePlus imgRef=VitiDialogs.chooseOneImageUI("Select the reference image, giving output space dimensions","Select the reference image, giving output space dimensions");
			VitiDialogs.saveDenseFieldTransformUI(trGlob, "Save the resulting composed transform",false,"", "composed_transform.tif", imgRef);
		}
		else {
			VitiDialogs.saveMatrixTransformUI(trGlob, "Save the resulting composed transform", false, "", "composed_transform.txt");
		}
		IJ.showMessage("Transform successfully saved.");
	}
	
	
	
	public void listActionsHasBeenClicked() {
		System.out.println("Click on list !");
	}
	
	
	
	/** Registration Manager gui setup and update----------------------------------------------------- ----------------------------------
	 */
	public void startRegistrationInterface() {
		actualizeLaunchingInterface(false);         
		//Panel with console-style log informations and requests
		JPanel consolePanel=new JPanel();
		consolePanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		consolePanel.setLayout(new GridLayout(1,1,0,0));
		System.out.println("Before, logArea est null ?"+logArea==null);
		System.out.println(logArea.getSize());
		logArea.setSize(600,80);
		System.out.println(logArea.getSize());
		logArea.setBackground(new Color(10,10,10));
		logArea.setForeground(new Color(245,255,245));
		logArea.setFont(new Font(Font.DIALOG,Font.PLAIN,14));
		JScrollPane jscroll=new JScrollPane(logArea);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setEditable(false);	
        
       //Panel with step settings, used for registration of two images, and when programming registration pipelines for series
		JPanel stepSettingsPanel=new JPanel();
		stepSettingsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));		
		stepSettingsPanel.setLayout(new GridLayout(9,2,15,10));

		stepSettingsPanel.add(labelNextAction);
		stepSettingsPanel.add(boxTypeAction);		
		stepSettingsPanel.add(labelTransformation);
		stepSettingsPanel.add(boxTypeTrans);		
		stepSettingsPanel.add(labelOptimizer);
		stepSettingsPanel.add(boxOptimizer);
		stepSettingsPanel.add(labelView);
		stepSettingsPanel.add(boxDisplay);
		stepSettingsPanel.add(labelViewMan);
		stepSettingsPanel.add(boxDisplayMan);
		stepSettingsPanel.add(new JLabel(""));
		stepSettingsPanel.add(new JLabel(""));
		stepSettingsPanel.add(labelTime1);
		stepSettingsPanel.add(labelTime2);		
		stepSettingsPanel.add(new JLabel(""));
		stepSettingsPanel.add(new JLabel(""));
		stepSettingsPanel.add(settingsButton);
		stepSettingsPanel.add(settingsDefaultButton);

		boxTypeAction.addActionListener(this);
		boxOptimizer.addActionListener(this);
		boxTypeTrans.addActionListener(this);
		boxDisplay.addActionListener(this);		
		boxDisplayMan.addActionListener(this);		
		settingsButton.addActionListener(this);
		settingsDefaultButton.addActionListener(this);

		settingsButton.setToolTipText("<html><p width=\"500\">" +"Advanced settings let you manage more parameters of the automatic registration algorithms"+"</p></html>");
		settingsDefaultButton.setToolTipText("<html><p width=\"500\">" +"Restore settings compute and set default parameters suited to your images"+"</p></html>");

		disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS});
		this.boxDisplay.setSelectedIndex(currentRegAction.typeAutoDisplay);

		
		
		//Panel with buttons for the context of two image registration
		JPanel buttonsPanel=new JPanel();
		if(this.registrationWindowMode==REGWINDOWTWOIMG) {
			buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
			buttonsPanel.setLayout(new GridLayout(2,3,40,40));
			buttonsPanel.add(runButton);
			buttonsPanel.add(abortButton);
			buttonsPanel.add(undoButton);
			buttonsPanel.add(finishButton);
			buttonsPanel.add(saveButton);
			buttonsPanel.add(sos1Button);
			
			runButton.addActionListener(this);
			abortButton.addActionListener(this);
			undoButton.addActionListener(this);
			finishButton.addActionListener(this);
			saveButton.addActionListener(this);
			sos1Button.addActionListener(this);
			colorStdActivatedButton=runButton.getBackground();
			colorGreenRunButton=new Color(100,255,100);
			
			abortButton.setToolTipText("<html><p width=\"500\">" +"Abort means killing a running operation and come back to the state before you clicked on Start this action."+
									   "Automatic registration is harder to kill. Please insist on this button until its colour fades to gray"+"</p></html>");
			finishButton.setToolTipText("<html><p width=\"500\">" +"Export aligned images and computed transforms"+"</p></html>");
			saveButton.setToolTipText("<html><p width=\"500\">" +"Save the current state of the plugin in a .fjm file, including the transforms and image paths. This .ijm file can be used later to restart from this point"+"</p></html>");
			sos1Button.setToolTipText("<html><p width=\"500\">" +"Opens a contextual help"+"</p></html>");
			undoButton.setToolTipText("<html><p width=\"500\">" +"Undo works as you would expect : it delete the previous action, and recover the previous state of transforms and images. Fijiyama handles only one undo : two successive actions can't be undone"+"</p></html>");
			finishButton.setToolTipText("<html><p width=\"500\">" +"Export aligned images and computed transforms"+"</p></html>");
			saveButton.setToolTipText("<html><p width=\"500\">" +"Save the current state of the plugin in a .fjm file, including the transforms and image paths. This .ijm file can be used later to restart from this point"+"</p></html>");
			sos1Button.setToolTipText("<html><p width=\"500\">" +"Opens a contextual help"+"</p></html>");
			undoButton.setToolTipText("<html><p width=\"500\">" +"Undo works as you would expect : it delete the previous action, and recover the previous state of transforms and images. Fijiyama handles only one undo : two successive actions can't be undone"+"</p></html>");
			
			setRunToolTip(MAIN);
			this.colorIdle=abortButton.getBackground();
			enable(new int[] {RUN,ABORT,UNDO,SAVE,FINISH,SETTINGS});
		}

		
		else if(this.registrationWindowMode==REGWINDOWSERIEPROGRAMMING) {
			buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
			buttonsPanel.setLayout(new GridLayout(2,2,40,40));
			buttonsPanel.add(addActionButton);
			buttonsPanel.add(removeSelectedButton);
			buttonsPanel.add(validatePipelineButton);
			buttonsPanel.add(sos1Button);

			addActionButton.addActionListener(this);
			removeSelectedButton.addActionListener(this);
			validatePipelineButton.addActionListener(this);
			sos1Button.addActionListener(this);

			addActionButton.setToolTipText("<html><p width=\"500\">" +"Click to add an action (bottom list), and configure it using upper menus"+"</p></html>");
			removeSelectedButton.setToolTipText("<html><p width=\"500\">" +"Remove the selected action (bottom list) from the global pipeline"+"</p></html>");
			validatePipelineButton.setToolTipText("<html><p width=\"500\">" +"Validate the global processing pipeline, and go to next step"+"</p></html>");

			enable(SETTINGS);
			listActions.setSelectedIndex(0);
			this.step=0;
			currentRegAction=regActions.get(this.step);
		}
		
		else if(this.registrationWindowMode==REGWINDOWSERIERUNNING) {
			buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
			buttonsPanel.setLayout(new GridLayout(1,3,40,40));
			buttonsPanel.add(runButton);
			buttonsPanel.add(abortButton);
//			buttonsPanel.add(goBackStepButton);
			buttonsPanel.add(sos1Button);

			runButton.addActionListener(this);
			abortButton.addActionListener(this);
			sos1Button.addActionListener(this);

			runButton.setToolTipText("<html><p width=\"500\">" +"Click here to run the next step in the global pipeline (see the black console log)"+"</p></html>");
//			goBackStepButton.setToolTipText("<html><p width=\"500\">" +"Use this function to compute again a step that went not as well as you expected"+"</p></html>");
			printRegActions("REG ACTIONS AT START OF SERIE PROGRAMMING", regActions);

			disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS,GOBACKSTEP});
			currentRegAction=regActions.get(this.step);
		}

		//Panel with list of actions, used for registration of two images, and when programming registration pipelines for series
		JPanel titleActionPanel=new JPanel();
		titleActionPanel.setBorder(BorderFactory.createEmptyBorder(5,25,0,25));
		titleActionPanel.setLayout(new GridLayout(1,1,10,10));
		titleActionPanel.add(new JLabel(this.registrationWindowMode==REGWINDOWSERIEPROGRAMMING ?  "Global registration pipeline : add/remove an action, select an action to modify it (using the menus), then approve the pipeline)" : "Global registration pipeline "));
		JPanel listActionPanel=new JPanel();
		listActionPanel.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
		listActionPanel.setLayout(new GridLayout(1,1,10,10));
		listActionPanel.add(actionsPane);
		listActions.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {	System.out.println("Gained !");
            if(registrationWindowMode==REGWINDOWSERIEPROGRAMMING)updateBoxesFromCurrentActionClicked();						}
			public void mousePressed(MouseEvent e) {		}
			public void mouseReleased(MouseEvent e) {			}
			public void mouseEntered(MouseEvent e) {			}
			public void mouseExited(MouseEvent e) {			}
		});

		//Main frame and main panel
		registrationFrame=new JFrame();
		JPanel registrationPanelGlobal=new JPanel();
		registrationPanelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		registrationPanelGlobal.setLayout(new BoxLayout(registrationPanelGlobal, BoxLayout.Y_AXIS));
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(jscroll);
		if(this.registrationWindowMode!=REGWINDOWSERIERUNNING) {
			registrationPanelGlobal.add(new JSeparator());
			registrationPanelGlobal.add(stepSettingsPanel);
		}
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(buttonsPanel);
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(titleActionPanel);
		registrationPanelGlobal.add(listActionPanel);
		registrationPanelGlobal.add(new JSeparator());
		registrationFrame.add(registrationPanelGlobal);
		registrationFrame.setTitle("Registration manager : two images registration");
		registrationFrame.pack();

		registrationFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		registrationFrame.addWindowListener(new WindowAdapter(){
             public void windowClosing(WindowEvent e){
                   System.out.println("CLOSING REGISTRATION WINDOW !");
                   closeAllViews();
                   registrationFrame.setVisible(false);
                   dispose();
                   actualizeLaunchingInterface(true);                   
             }
		});
		updateList();
		updateBoxesAndCurrentAction();

		registrationFrame.setVisible(true);
		registrationFrame.repaint();
		VitimageUtils.adjustFrameOnScreen(registrationFrame,2,0);		
		logArea.setVisible(true);
		logArea.repaint();
	}
		
	public void addLog(String t,int level) {
		logArea.append((level==0 ? "\n > ": (level==1) ? "\n " : " ")+t);
		logArea.setCaretPosition(logArea.getDocument().getLength());
	}

	
	public void displaySosMessage(int context){
		if(context==SOS_CONTEXT_LAUNCH) {
			IJ.showMessage(
			"Message d aide\n"+
			"dans le premier contexte\n"+
			"celui de lancement\n"+
			"bla\n"+
			"bla\n"+
			"bla\n"	
			);
		}
	}

	public void runSos(){
		String textToDisplay="";
		String basicFijiText="<b>Main window help : </b>Fijiyama is a versatile tool to perform 3d alignment of images,"+
				" acquired at successive observation times, or with different imaging modalities. ";
		String mainWindowTextTwoImgs=basicFijiText+
				"The most common registration strategy combine these steps :"+startPar+
				" <b>1)   Manual registration with a rigid transform</b> : "+"rough correction of relative image position and orientation"+nextPar+
				" <b>2-a) Automatic registration with a rigid transform : </b>using ITK if the problem is \"easy\" (accurate manual registration and object is dissymetric) or Blockmatching if more robustness is needed."+nextPar+
				" <b>2-b) <i>(optional)</i> Automatic registration with a similarity transform : </b>, if the object shows an isotropic dilation / shrinkage between reference and moving image."+nextPar+
				" <b>2-c) <i>(optional)</i> Automatic registration with a dense vector field : </b> using the Block matching algorithm, for more complex deformation between images."+nextPar+
				" <b>3)   Axis alignment of the reference image : </b> useful to export the results in a reproducible geometry, suited for analysis (for example, with the plant axis along the image Z axis."+saut+
				" The manual registration steps (1 and 3) can be done in a 2d or 3d viewer, depending of your images. Try the 3d viewer first, and if needed, use the  <b>\"Abort\"</b> button to come back to the main window, and choose the 2d viewer."+
				"Automatic registration can be monitored, too. Set the monitoring level using the <b>\"Automatic registration display\"</b>box-list. No monitoring means hoping everything goes fine until the step finish, but it is really faster. Finally, "+
				"automatic algorithms settings can be modified using the settings dialog ( <b>\"Advanced settings\"</b> button )."+saut+saut; 

		String mainWindowTextProgramming=basicFijiText+
				"You're actually running a serie registration. We assume that you trained on the \"Two images registration\" module,"+
				" to understand the main concepts of the plugin, testing the provided functions on your data, and eventually to fine-tune the settings of algorithms."+
				nextPar+"The serie registration process runs the following steps :"+startPar+
				" <b>1)   Defining data directories :</b> input directory for image lookup, and output directory to save plugin state and exported images and transforms"+nextPar+
				" <b>2-a) Defining the inter-time registration pipeline :</b> this pipeline describe the actions to run in order to align successive observations with the reference modality"+nextPar+
				" <b>2-b) Defining the inter-modals registration pipelines :</b> these pipelines (one for each secondary modality) describe the actions to run in order to align secondary modalities with the reference modality"+nextPar+
				" <b>3)   Running full pipeline :</b> During this last step, the actions defined in the inter-time and inter-modal pipelines are associated in a full registration pipeline, and applied until all data are aligned and can be combined."+
				"The default order of the full pipeline is set to save your time. Manual operations are done first, then the automatic parts run autonomously."+nextPar+
				"Once the full pipeline is running, You can ask to stop a running operation, change its settings, and ask to run it again (and also run again the direct following steps)"+
				saut+saut; 
		
		String axisText=
				"Axis alignment of the reference image onto the image XYZ axis, in order to set the object position in both results images that will be exported."+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				saut+
				"All along the registration procedure, only the moving image is moved."+
				"Thus, one could need the final result to be in a geometry that is optimal for biological analysis."+
				"For example, one could expect that the the axis of the object (a plant, a bone, ...) would be aligned with the Z axis (slices normal axis). "+
				"The axis alignment process opens a 3d interface, to interact with the reference image (shown as a red volume)"+
				" and set its alignment relative to the XYZ axis, drawn as white orthogonal lines "+
				"<b>Use the mouse to select (click) and move (click and drag)</b> the red volume or the 3d scene in order to align the red volume onto the grid :"+startPar+
				" -> <b>Click on a volume</b> to select it, or on the background to select the 3d scene."+nextPar+
				" -> <b>Right-click and drag the red volume</b> to turn it,"+nextPar+
				" -> <b>right-click and drag the background</b> or the grid to turn around the 3d scene,"+nextPar+
				" -> <b>push shift key and right-click and drag</b> to translate the red volume or the 3d scene."+
				nextPar+" "+
				" -> Once done, click on the <b>\"Position ok\"</b> button to validate the alignment."+" "+"The transform computed is then applied to the reference and moving images"+saut;
		String bmText="Automatic registration."+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				nextPar+
				"Automatic registration can be done using two different optimizers : "+
				" Block matching compares subparts of images to identify correspondances, and use these correpondances to align the moving image on the reference image ; "+
				" Itk Iconic optimizes a transform, and quantify the evolution of image matching using a global statistical measure. Both use a pyramidal scheme, computing correspondances and transforms from a rough level (max image subsampling factor=2, 4, 8...) to an accurate level (min image subsampling factor=1, 2, 4, ...)."+
				nextPar+
				"Automatic registration is an optimization process, its duration depends on the parameters. The estimated computation is given to help you "+
				"to define parameters according with the computation time you expect. The process starts when clicking on the <b>\"Start this action\"</b> button and finish when all the iterations are done at all levels, or when you click on the <b>\"Abort\"</b> button ."+saut;
				
		String manualText="Manual registration step."+""+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				saut+
				"This operation lets you roughly align the images manually, in order to handle transforms of great amplitude (angle > 15 degrees) that cannot be estimated automatically."+
				"To start this action, click on the <b>\"Start this action\"</b> button. A 3d interface opens, and let you interact with the images."+saut+
				"In the 3d interface, the reference image is shown as a green volume, locked to the 3d scene."+
				"The red volume is the moving image (free to move relatively to the red one and the 3d scene)."+
				"<b>Use the mouse to select (click) and move (click and drag)</b> the red volume or the 3d scene in order to align the red volume onto the green one :"+nextPar+
				" -> <b>Click on a volume</b> to select it, or on the background to select the 3d scene."+nextPar+
				" -> <b>Right-click and drag the red volume</b> to turn it, and <b>the background or the green volume</b> to turn around the 3d scene,"+nextPar+
				" -> <b>push shift key and right-click and drag</b> to translate the red volume or the 3d scene."+
				nextPar+" "+
				"Once done, click on the <b>\"Position ok\"</b> button to validate the alignment."+
				saut;
				
		
		disable(SOS);
		if(this.runButton.getText().equals("Start this action")) {//Nothing running
			textToDisplay="<html>"+saut+""+""+
					( (this.registrationWindowMode==REGWINDOWTWOIMG) ? mainWindowTextTwoImgs : mainWindowTextProgramming )+
			saut+saut+"<b>Contextual help (current settings / parameters) :</b>";
			if(this.boxTypeAction.getSelectedIndex()==0)textToDisplay+=manualText;
			else if(this.boxTypeAction.getSelectedIndex()==1)textToDisplay+=bmText;
			else if(this.boxTypeAction.getSelectedIndex()==2) textToDisplay+=axisText;
		}
		else {
			if(this.boxTypeAction.getSelectedIndex()==0)textToDisplay="<html>"+startPar+"<b>3d manual registration of two images</b>"+"<br/>"+""+nextPar+manualText;
			if(this.boxTypeAction.getSelectedIndex()==1)textToDisplay="<html>"+startPar+"<b>Automatic registration of two images</b>"+"<br/>"+""+nextPar+bmText;
			if(this.boxTypeAction.getSelectedIndex()==2)textToDisplay="<html>"+startPar+"<b>Axis alignment registration of two images</b>"+"<br/>"+""+nextPar+axisText;
		}
		textToDisplay+="<b>Citing this work :</b> R. Fernandez and C. Moisy, <i>Fijiyama : a versatile registration tool for 3D multimodal time-lapse monitoring of biological tissues in Fiji</i> (under review)"+saut+
				"<b>Credits :</b> this work was supported by the \"Plan deperissement du vignoble\"   </p>";
		IJ.showMessage("Fijiyama contextual help", textToDisplay);
		enable(SOS);
	}
	
	public void openSettingsDialog() {
		//Parameters for manual and axis aligment
		if(this.boxTypeAction.getSelectedIndex()!=1) {
	        GenericDialog gd= new GenericDialog("Settings for manual registration");
	        gd.addMessage("No advanced settings for manual registration. The only setting is in the main menu (manual registration viewer)\n.\n"+
	        "The first option (3d viewer) assumes objects of interest are bright structures with an irregular surface,\n"+
	        		"surrounded with a dark background. Under these assumption, manual registration can be done in the 3d viewer.\n.\n"+
	        		"If your images are not of this kind, manual registration in the 3d viewer will be a hard task, and you should set\nthe other option : 2d viewer, to identify landmarks using the classic 2d slicer");
	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        return;
		}

		
		//Parameters for automatic registration
		String message="Successive subsampling factors used, from max to min."+"These parameters have the most dramatic effect\non computation time and results accuracy :\n"+
				"- The max level is the first level being processed, making comparisons between subsampled versions of images."+"\n"+
				"  After subsampling images, the algorithm only sees the global structures, but allows transforms of greater amplitude\n"+
				"  High subsampling levels run faster, and low subsampling levels run slower. But if the max subsampling level is too high, the subsampled image\n  is not informative anymore, and registration could diverge"+"\n.\n"+
				"- The min level is the last subsampling level and the more accurate to be processed\n  Subsampling=1 means using all image informations (no subsampling) \n";

			//Parameters for BlockMatching
			if(this.boxOptimizer.getSelectedIndex()==0) {
			GenericDialog gd= new GenericDialog("Expert mode for Blockmatching registration");
	        String[]levelsMax=new String[maxAcceptableLevel];for(int i=0;i<maxAcceptableLevel;i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[currentRegAction.levelMax-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[currentRegAction.levelMin-1]);
	        gd.addChoice("Higher accuracy (subpixellic level)", new String[] {"Yes","No"},currentRegAction.higherAcc==1 ? "Yes":"No");
	        
	        gd.addMessage("Blocks dimensions. Blocks are image subparts\nCompared to establish correspondances between ref and mov images");
	        gd.addNumericField("Block half-size along X", currentRegAction.bhsX, 0, 3, "original pixels");
	        gd.addNumericField("Block half-size along Y", currentRegAction.bhsY, 0, 3, "original pixels");
	        gd.addNumericField("Block half-size along Z", currentRegAction.bhsZ, 0, 3, "original pixels");

	        gd.addMessage("Searching neighbourhood. Distance to look for\nbetween a reference image block and a moving image block");
	        gd.addNumericField("Block neighbourhood along X", currentRegAction.neighX, 0, 3, "subsampled pixels");
	        gd.addNumericField("Block neighbourhood along Y", currentRegAction.neighY, 0, 3, "subsampled pixels");
	        gd.addNumericField("Block neighbourhood along Z",  currentRegAction.neighZ, 0, 3, "subsampled pixels");

	        gd.addMessage("Spacing between two successive blocks\nalong each dimension");
	        gd.addNumericField("Striding along X", currentRegAction.strideX, 0, 3, "subsampled pixels");
	        gd.addNumericField("Strinding along Y",  currentRegAction.strideY, 0, 3, "subsampled pixels");
	        gd.addNumericField("Strinding along Z",  currentRegAction.strideZ, 0, 3, "subsampled pixels");

	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  currentRegAction.iterationsBM, 0, 3, "iterations");
	        if(this.boxTypeTrans.getSelectedIndex()==2)gd.addNumericField("Sigma for dense field smoothing", currentRegAction.sigmaDense, 3, 6, unit);
	        gd.addNumericField("Percentage of blocks selected by score", currentRegAction.selectScore, 0, 3, "%");
	        if(this.boxTypeTrans.getSelectedIndex()!=2)gd.addNumericField("Percentage kept in Least-trimmed square", currentRegAction.selectLTS, 0, 3, "%");	        

	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        System.out.println("Go");
	        int a=gd.getNextChoiceIndex()+1; currentRegAction.levelMax=a;
	        System.out.println("a="+a);
	        int b=gd.getNextChoiceIndex()+1; b=b<a ? b : a; currentRegAction.levelMin=b;
	        System.out.println("b="+b);
	        a=1-gd.getNextChoiceIndex();
	        currentRegAction.higherAcc=a;
	       	
	       	int c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; currentRegAction.bhsX=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; currentRegAction.bhsY=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; currentRegAction.bhsZ=c;

	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; currentRegAction.neighX=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; currentRegAction.neighY=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; currentRegAction.neighZ=c;

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; currentRegAction.strideX=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; currentRegAction.strideY=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; currentRegAction.strideZ=c;

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; currentRegAction.iterationsBM=c;
	       	if(this.boxTypeTrans.getSelectedIndex()==2) {double d=gd.getNextNumber(); d=d<1E-6 ? 1E-6 : d; currentRegAction.sigmaDense=d;}
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; currentRegAction.selectScore=c;
	       	if(this.boxTypeTrans.getSelectedIndex()!=2) {c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; currentRegAction.selectLTS=c;}
		}
		//Parameters for Itk Iconic
		else {//Itk parameters
			System.out.println("Opening settings for itk");
	        GenericDialog gd= new GenericDialog("Expert mode for Itk registration");
	        System.out.println("MaxAc="+maxAcceptableLevel);
	        System.out.println("CurLevMin="+currentRegAction.levelMin);
	        System.out.println("CurLevMax="+currentRegAction.levelMax);
	        String[]levelsMax=new String[maxAcceptableLevel];for(int i=0;i<maxAcceptableLevel;i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
			System.out.println("After critical");
        gd.addMessage(message);
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[currentRegAction.levelMax-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[currentRegAction.levelMin-1]);
	        
	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  currentRegAction.iterationsITK, 0, 5, "iterations");
	        gd.addNumericField("Learning rate",  currentRegAction.learningRate, 4, 8, " no unit");

	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        int param1=gd.getNextChoiceIndex()+1; 
	        currentRegAction.levelMax=param1;

	        int param2=gd.getNextChoiceIndex()+1; param2=param2<param1 ? param2 : param1;
	        currentRegAction.levelMin=param2;
	       	
	       	int param3=(int)Math.round(gd.getNextNumber());
	       	param3=param3<0 ? 0 : param3;  currentRegAction.iterationsITK=param3;
	       	double param4=gd.getNextNumber();
	       	param4=param4<0 ? EPSILON : param4;  currentRegAction.learningRate=param4;
		}		
	}
	
	public void enable(int but) {
		setState(new int[] {but},true);
	}
	public void disable(int but) {
		setState(new int[] {but},false);
	}

	public void enable(int[]tabBut) {
		setState(tabBut,true);
	}
	public void disable(int[]tabBut) {
		setState(tabBut,false);
	}
			
	public void setState(int[]tabBut,boolean state) {
		for(int but:tabBut) {
			switch(but) {
			case BOXACT:this.boxTypeAction.setEnabled(state);this.labelNextAction.setEnabled(state);break;
			case BOXOPT:this.boxOptimizer.setEnabled(state);this.labelOptimizer.setEnabled(state);break;
			case BOXTRANS:this.boxTypeTrans.setEnabled(state);this.labelTransformation.setEnabled(state);break;
			case BOXDISP:this.boxDisplay.setEnabled(state);this.labelView.setEnabled(state);break;
			case BOXDISPMAN:this.boxDisplayMan.setEnabled(state);this.labelViewMan.setEnabled(state);break;
			case SETTINGS:this.settingsButton.setEnabled(state);this.settingsDefaultButton.setEnabled(state);break;
			case RUN:this.runButton.setEnabled(state);break;
			case UNDO:this.undoButton.setEnabled(state);break;
			case ABORT:this.abortButton.setEnabled(state);abortButton.setBackground(state ? new Color(255,0,0) : colorIdle);break;
			case SAVE:this.saveButton.setEnabled(state);break;
			case FINISH:this.finishButton.setEnabled(state);break;
			case SOS:this.sos1Button.setEnabled(state);break;
			case RUNTWOIMG:this.runTwoImagesButton.setEnabled(state);break;
			case RUNSERIE:this.runSerieButton.setEnabled(state);break;
			case RUNNEXTSTEP:this.runNextStepButton.setEnabled(state);break;
			case GOBACKSTEP:this.goBackStepButton.setEnabled(state);break;
			case SOSINIT:this.sos0Button.setEnabled(state);break;
			case RUNTRANS:this.transformButton.setEnabled(state);break;
			case RUNTRANSCOMP:this.composeTransformsButton.setEnabled(state);break;
			}	
		}	
	}
	
	public void setRunToolTip(int context){
		if(context==MAIN) {
			runButton.setToolTipText("<html><p width=\"500\">" +"Start means starting the action defined by the current configuration, and the current settings."+
			"During the execution, the action can be interrupted using the Abort button. After the execution, the action can be undone using the Undo button"+"</p></html>");
		}
		if(context==MANUAL2D) {
			runButton.setToolTipText("<html><p width=\"500\">" +"Click here to validate the landmarks, compute the corresponding transform, and get the transform applied to images"+"</p></html>");
		}
		if(context==MANUAL3D) {
			runButton.setToolTipText("<html><p width=\"500\">" +"Click here to validate the actual relative position of objects, and get the transform applied to images"+"</p></html>");
		}
	}	
	
	public void updateList() {
		DefaultListModel<String> listModel = new DefaultListModel<String>();
        for(int i=0;i<this.regActions.size()-(this.registrationWindowMode==REGWINDOWTWOIMG ? 0 : 0);i++) {
        	listModel.addElement(regActions.get(i).readableString());
        }
        listModel.addElement("< Next action to come >");
		this.listActions.setModel(listModel);
		this.listActions.setSelectedIndex(this.step);
		ScrollUtil.scroll(listActions,ScrollUtil.SELECTED,new int[] {listActions.getSelectedIndex(),regActions.size()+1});
		listActions.repaint();
	}
			
	public void updateEstimatedTime() {
		if((this.boxTypeAction.getSelectedIndex()==0) && this.boxDisplayMan.getSelectedIndex()==0)this.estimatedTime=300;//manual registration with landmarks
		else if((this.boxTypeAction.getSelectedIndex()==0) && this.boxDisplayMan.getSelectedIndex()==1)this.estimatedTime=240;//manual registration in 3d
		else if((this.boxTypeAction.getSelectedIndex()==2) && this.boxDisplayMan.getSelectedIndex()==0)this.estimatedTime=200;//axis alignment with landmarks
		else if((this.boxTypeAction.getSelectedIndex()==2) && this.boxDisplayMan.getSelectedIndex()==1)this.estimatedTime=240;//axis alignment in 3d
		else {
			if(this.boxOptimizer.getSelectedIndex()==0)this.estimatedTime=(int)Math.round(BlockMatchingRegistration.estimateRegistrationDuration(
					this.imageParameters,currentRegAction.typeAutoDisplay,currentRegAction.levelMin,currentRegAction.levelMax,currentRegAction.iterationsBM,currentRegAction.typeTrans,
					new int[] {currentRegAction.bhsX,currentRegAction.bhsY,currentRegAction.bhsZ},
					new int[] {currentRegAction.strideX,currentRegAction.strideY,currentRegAction.strideZ},
					new int[] {currentRegAction.neighX,currentRegAction.neighY,currentRegAction.neighZ},
					this.nbCpu,currentRegAction.selectScore,currentRegAction.selectLTS,currentRegAction.selectRandom,currentRegAction.subsampleZ==1,currentRegAction.higherAcc
					));
		
			
			else this.estimatedTime=ItkRegistrationManager.estimateRegistrationDuration(
					this.imageParameters,currentRegAction.typeAutoDisplay,currentRegAction.iterationsITK, currentRegAction.levelMin,currentRegAction.levelMax);
		}
		int nbMin=this.estimatedTime/60;
		int nbSec=this.estimatedTime%60;
		this.labelTime2.setText(""+nbMin+" mn and "+nbSec+" s");
		this.labelTime2.setForeground(new Color(20,60,20));
	}

	public void updateViewTwoImages() {
		if(this.registrationWindowMode!=REGWINDOWTWOIMG)return;
		ImagePlus imgMovCurrentState,imgRefCurrentState;
		ItkTransform trMov,trRef = null;
		//Update views
		if(this.transforms[movTime][movMod].size()==0 && this.transforms[refTime][refMod].size()==0 ) {
			imgRefCurrentState=this.images[refTime][refMod];
			imgMovCurrentState=this.images[movTime][movMod];
		}
		else {
			trMov=getComposedTransform(movTime,movMod);
			if(this.transforms[refTime][refMod].size()>0) {
				trRef=getComposedTransform(refTime, refMod);
				trMov.addTransform(trRef);
			}
			imgMovCurrentState= trMov.transformImage(this.images[refTime][refMod],this.images[movTime][movMod],false);
			imgMovCurrentState.setDisplayRange(this.imageRanges[movTime][movMod][0], this.imageRanges[movTime][movMod][1]);
	
			imgRefCurrentState= (trRef==null)? this.images[refTime][refMod] : trRef.transformImage(this.images[refTime][refMod],this.images[refTime][refMod],false);
			imgRefCurrentState.setDisplayRange(this.imageRanges[refTime][refMod][0], this.imageRanges[refTime][refMod][1]);
		}
		
		//Compose images
		imgView=VitimageUtils.compositeNoAdjustOf(imgRefCurrentState,imgMovCurrentState,step==0 ? "Superimposition before registration" : "Registration results after "+(step)+" step"+((step>1)?"s":""));
		this.viewSlice=this.images[refTime][refMod].getStackSize()/2;
		int screenHeight=Toolkit.getDefaultToolkit().getScreenSize().height;
		int screenWidth=Toolkit.getDefaultToolkit().getScreenSize().width;
		if(screenWidth>1920)screenWidth/=2;
		imgView.show();imgView.setSlice(this.viewSlice);
		double zoomFactor=  Math.min((screenHeight/2)/imgView.getHeight()  ,  (screenWidth/2)/imgView.getWidth()); 
		java.awt.Rectangle w = imgView.getWindow().getBounds();
		
		//If little image, enlarge it until its size is between half screen and full screen
		while(imgView.getWindow().getWidth()<(screenWidth/2) && imgView.getWindow().getHeight()<(screenHeight/2)) {
			int sx=imgView.getCanvas().screenX((int) (w.x+w.width));
			int sy=imgView.getCanvas().screenY((int) (w.y+w.height));
			imgView.getCanvas().zoomIn(sx, sy);
			VitimageUtils.waitFor(50);
			this.lastViewSizes=new int[] {imgView.getWindow().getWidth(),imgView.getWindow().getHeight()};
		}
		//If big image, reduce it until its size is between half screen and full screen
		while(imgView.getWindow().getWidth()>(screenWidth) || imgView.getWindow().getHeight()>(screenHeight)) {
			int sx=imgView.getCanvas().screenX((int) (w.x+w.width));
			int sy=imgView.getCanvas().screenY((int) (w.y+w.height));
			imgView.getCanvas().zoomOut(sx, sy);
			this.lastViewSizes=new int[] {imgView.getWindow().getWidth(),imgView.getWindow().getHeight()};
		}
		VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),registrationFrame,0,0,10);
	}

	public void closeAllViews() {
		for(int st=0;st<=this.step;st++) {
			String text=(st==0) ? "Superimposition before registration" :  ("Registration results after "+(st)+" step"+((st>1)? "s" : "")) ;
			if (WindowManager.getImage(text)!=null)WindowManager.getImage(text).close();
		}
		if (WindowManager.getImage("Image 1")!=null)WindowManager.getImage("Image 1").close();
		if (WindowManager.getImage("Image 2")!=null)WindowManager.getImage("Image 2").close();
		if (RoiManager.getInstance()!=null)RoiManager.getInstance().close();
		if (universe!=null) universe.close();
	}	
	
	public void updateBoxFieldsFromRegistrationAction(RegistrationAction reg) {
		boxTypeAction.setSelectedIndex(reg.typeAction);
		boxTypeTrans.setSelectedIndex(reg.typeTrans==Transform3DType.DENSE ? 2 : reg.typeTrans==Transform3DType.RIGID ? 0 : 1);
		boxDisplay.setSelectedIndex(reg.typeAutoDisplay);
		boxDisplayMan.setSelectedIndex(reg.typeManViewer);
		boxOptimizer.setSelectedIndex(reg.typeOpt==OptimizerType.BLOCKMATCHING ? 0 : 1);
		setState(new int[] {BOXOPT,BOXTIME,BOXDISP },boxTypeAction.getSelectedIndex()==1);
		setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
	}

	
	
	/** Launching interface, at the very start
	 * 
	 */
	public void startLaunchingInterface() {
		sos0Button=new JButton("Sos");
		runTwoImagesButton=new JButton("Register two 3d images (training mode)");
		runSerieButton=new JButton("Register 3d series (N-times and/or N-modalities)");
		transformButton=new JButton("Apply a computed transform to another image");
		composeTransformsButton=new JButton("Compose successive transforms into a single one");
		JPanel globalPanel=new JPanel();
		globalPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		globalPanel.setLayout(new BoxLayout(globalPanel, BoxLayout.Y_AXIS));
		
		JPanel somethingPanel=new JPanel();
		somethingPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));		
		somethingPanel.setLayout(new GridLayout(1,1,20,20));
		JLabel jlab=new JLabel("Fijiyama : a versatile 3d registration tool for Fiji",JLabel.CENTER);
		somethingPanel.add(jlab);
		JPanel buttonPanel=new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));		
		buttonPanel.setLayout(new GridLayout(2,2,20,20));
		buttonPanel.add(runTwoImagesButton);
		buttonPanel.add(runSerieButton);
		buttonPanel.add(transformButton);
		buttonPanel.add(composeTransformsButton);
		buttonPanel.add(sos0Button);
		frameLaunch=new JFrame("Fijiyama : a versatile 3d registration tool for Fiji");
		globalPanel.add(new JSeparator());
		globalPanel.add(somethingPanel);
		globalPanel.add(buttonPanel);
		globalPanel.add(new JSeparator());
		frameLaunch.add(globalPanel);
		frameLaunch.pack();
		actualizeLaunchingInterface(true);
		frameLaunch.setVisible(true);
		frameLaunch.repaint();		
		System.out.println("Launching interface done");
	}
	
	public void actualizeLaunchingInterface(boolean expectedState) {
		if(expectedState) {
			sos0Button.setEnabled(true);
			runTwoImagesButton.setEnabled(true);
			runSerieButton.setEnabled(true);
			composeTransformsButton.setEnabled(true);
			transformButton.setEnabled(true);
			sos0Button.addActionListener(this);
			runTwoImagesButton.addActionListener(this);
			runSerieButton.addActionListener(this);
			transformButton.addActionListener(this);
		}
		else {
			sos0Button.setEnabled(false);
			runTwoImagesButton.setEnabled(false);
			runSerieButton.setEnabled(false);
			transformButton.setEnabled(false);
			sos0Button.removeActionListener(this);
			runTwoImagesButton.removeActionListener(this);
			runSerieButton.removeActionListener(this);
			transformButton.removeActionListener(this);			
			composeTransformsButton.removeActionListener(this);			
		}
	}

	
}

	
				

