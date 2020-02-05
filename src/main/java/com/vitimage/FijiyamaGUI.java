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
import javax.swing.JTextField;

import org.jfree.chart.plot.ThermometerPlot;
import org.python.antlr.base.mod;
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




public class FijiyamaGUI extends PlugInFrame implements ActionListener {
	public ImagePlus imgView;
	private boolean debugMode=true;
	public volatile boolean passThroughActivated=false;
	ItkRegistrationManager itkManager;
	BlockMatchingRegistration bmRegistration;
	private Color colorStdActivatedButton;
	private Color colorGreenRunButton;
	
	//Flags for the kind of viewer
	private static final String saut="<div style=\"height:1px;display:block;\"> </div>";
	private static final String startPar="<p  width=\"650\" >";
	private static final String nextPar="</p><p  width=\"650\" >";

	public static final int MODE_TWO_IMAGES=2;
	public static final int MODE_SERIE=3;

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
	private static final int RUNALL=108;
	private static final int UNDO=103;
	private static final int ABORT=104;
	private static final int SAVE=105;
	private static final int FINISH=106;
	private static final int SOS=107;
	private final double EPSILON=1E-8;


	
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
	public static final int WINDOWTWOIMG=121;
	public static final int WINDOWSERIEPROGRAMMING=122;
	public static final int WINDOWSERIERUNNING=123;
	public static final int WINDOWIDLE=124;
	int modeWindow=WINDOWSERIEPROGRAMMING;
	
	//Interface parameters
	private static final long serialVersionUID = 1L;
	private int SOS_CONTEXT_LAUNCH=0;
	private volatile boolean actionAborted=false;
	boolean developerMode=false;
	String spaces="                                                                                                                                   ";
	public int viewSlice;

	
	//Registration interface attributes
	private String[]textActions=new String[] {"Manual registration","Automatic registration","Images alignment with XYZ axis","---","---","---","---"};
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
	private JButton runThroughButton=new JButton("Chain-run automatic steps");
	private JButton abortButton = new JButton("Abort");
	private JButton undoButton=new JButton("Undo");
	private JButton saveButton=new JButton("Save current state");
	private JButton finishButton=new JButton("Export results");
	private JButton sosButton=new JButton("Contextual help");

	private JButton runTwoImagesButton = new JButton("Register two 3d images (training mode)");
	private JButton runSerieButton = new JButton("Create new 3d serie (N-times and/or N-modalities)");
	private JButton loadFjmButton = new JButton("Open a previous study from a fjm file");
	private JButton transformButton = new JButton("Apply a computed transform to another image");
	private JButton composeTransformsButton = new JButton("Compose successive transforms into a single one");
	private JButton runNextStepButton = new JButton("Run next step");
	private JButton goBackStepButton = new JButton("Coming soon...");

	private JButton addActionButton = new JButton("Add an action to pipeline");
	private JButton removeSelectedButton = new JButton("Remove last action");
	public JButton validatePipelineButton = new JButton("Approve inter-time pipeline");
	
	public JFrame registrationFrame;
	private JTextArea logArea=new JTextArea("", 10,10);
	private Color colorIdle;
	private JFrame frameLaunch;

	//Structures for computation (model part);
	private int mode=MODE_TWO_IMAGES;
	private volatile boolean pipelineValidated=false;
	protected boolean comboBoxChanged=false;
	public int[] lastViewSizes;
	private RegistrationManager regManager;
	
	private int screenHeight=0;
	private int screenWidth=0;

	public String displayedNameImage1="Image 1";
	public String displayedNameImage2="Image 2";
	public String displayedNameCombinedImage="Data combined";
	public String displayedNameHyperImage="Data combined";

	private int waitingTimeHyperImage=30;

	
	
	
	
	
	//TODO : Testing to do
	// Fonction undo : undo apres recman apres rectrans
	// Fonction save : saving, saving and load, saving and saving and load
	
	//TODO : during Programming, only MAN and AUTO actions available
	//TODO : quand il charge, il met automatiquement à MAN la premiere qui vient juste apres
	//TODO : visualisation resultat
	//TODO : export resultat
	//TODO : verifier le bon fonctionnement de alignement
	//TODO : verifier si two images marche toujours
	//TODO : tester la chaine complete avec lancement depuis launching area
	
	//TODO : pour la V3, afin d'achever la construction du patron MVC, mettre les runnables et les interfaces de recalage manuel dans RegistrationAction


	
	
	

	/*Starting points*************************************************************************************************************/
	public FijiyamaGUI() {
		super("FijiYama : a versatile registration tool for Fiji");
		regManager=new RegistrationManager(this);
		this.screenHeight=Toolkit.getDefaultToolkit().getScreenSize().height;
		this.screenWidth=Toolkit.getDefaultToolkit().getScreenSize().width;
		if(this.screenWidth>1920)this.screenWidth/=2;
	}

	public static void main(String[]args) {//TestMethod
		ImageJ ij=new ImageJ();
		FijiyamaGUI reg=new FijiyamaGUI();
		reg.developerMode=true;
		reg.run("");
	}
	
	public void run(String arg) {
		if(debugMode) runTest();
		else {
			startLaunchingInterface();
			modeWindow=WINDOWIDLE;
		}
	}

	public void runTest() {
		int TESTCOMPOSE=1;
		int TESTTRANS=2;

		int TESTNEWTWOIMG=3;
		int TESTLOADTWOIMG=4;
		
		int TESTHYPERIMG=5;

		int TESTSERIE=10;
		int TESTOLDSERIE=6;
		int TESTNEWSERIE=7;
		int TESTINTERFACEPROGRAMMING=8;
		int TESTINTERFACERUNNING=9;
	
		int typeTest=TESTLOADTWOIMG;

		if(typeTest==TESTTRANS){
			ItkTransform.transformImageWithGui();
		}
		else if(typeTest==TESTCOMPOSE){
			ItkTransform.composeTransformsWithGui();
		}
		else if(typeTest==TESTNEWSERIE) {
			this.mode=MODE_SERIE;
			regManager.setupSerieFromScratch();
			startSerie();
		}
		else if(typeTest==TESTOLDSERIE) {
			this.mode=MODE_SERIE;
			regManager.setupFromFjmFile(findFjmFileInDir("/home/fernandr/Bureau/Test/SERIE/OUTPUT_DIR/"));

			startSerie();
		}
		else if(typeTest==TESTSERIE) {
			this.mode=MODE_SERIE;
			if(!new File("/home/fernandr/Bureau/Test/SERIE/OUTPUT_DIR/").exists())regManager.setupSerieFromScratch();
			else {
				String fjmFile=findFjmFileInDir("/home/fernandr/Bureau/Test/SERIE/OUTPUT_DIR/");
				if(fjmFile==null)regManager.setupSerieFromScratch();
				else regManager.setupFromFjmFile(fjmFile);
			}
			startSerie();
		}		
		else if(typeTest==TESTNEWTWOIMG) {
			this.mode=MODE_TWO_IMAGES;
			regManager.setupFromTwoImages();
			startTwoImagesRegistration();
		}
		else if(typeTest==TESTLOADTWOIMG) {
			this.mode=MODE_TWO_IMAGES;
			String fjmFile=findFjmFileInDir("/home/fernandr/Bureau/Test/TWOIMG/OUTPUT_DIR/");
			if(fjmFile==null)regManager.setupFromTwoImages();
			else regManager.setupFromFjmFile(fjmFile);
			startTwoImagesRegistration();
			this.modeWindow=WINDOWTWOIMG;
		}		
	}
	
	public void startTwoImagesRegistration() {
		this.mode=MODE_TWO_IMAGES;
		this.modeWindow=WINDOWTWOIMG;
		startRegistrationInterface();
		updateView();
		enable(new int[] {RUN,SETTINGS,SOS,BOXACT,BOXTRANS,BOXDISPMAN});		
		welcomeAndInformAboutComputerCapabilities();
		if(regManager.getStep()>0)enable(new int[] {RUN,SAVE,UNDO,FINISH});
	}

	public void startSerie(){
		this.mode =MODE_SERIE;
		this.modeWindow=WINDOWSERIERUNNING;

		System.out.println("HERE CUR="+regManager.getCurrentAction());
		startRegistrationInterface();
		System.out.println("THERE CUR="+regManager.getCurrentAction());
		updateBoxFieldsFromRegistrationAction(regManager.getCurrentAction());
		disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS});
		enable(new int[] {RUNNEXTSTEP,GOBACKSTEP,SOS});	
		welcomeAndInformAboutComputerCapabilities();	
		addLog("Waiting for you to start next action : "+regManager.getCurrentAction().readableString(false),0);
	}
	
	
	public String findFjmFileInDir(String dir) {
		File f=new File(dir);
		String[]strs=f.list();
		String result="";
		for(String str : strs)if(str.substring(str.length()-4,str.length()).equals(".fjm"))result=str;
		if(result.equals(""))return null;
		return new File(dir,result).getAbsolutePath();		
	}
	
	
	
	

	
	
	/* Registration Manager gui  and launching interface gui ************************************************************************************************/
	public void startRegistrationInterface() {
		actualizeLaunchingInterface(false);         
		//Panel with console-style log informations and requests
		JPanel consolePanel=new JPanel();
		consolePanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		consolePanel.setLayout(new GridLayout(1,1,0,0));
		logArea.setSize(600,80);
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

		if(modeWindow!=WINDOWSERIERUNNING) {
			boxTypeAction.addActionListener(this);		
			boxOptimizer.addActionListener(this);
			boxTypeTrans.addActionListener(this);
			boxDisplay.addActionListener(this);		
			boxDisplayMan.addActionListener(this);		
		}
		settingsButton.addActionListener(this);
		settingsDefaultButton.addActionListener(this);

		settingsButton.setToolTipText("<html><p width=\"500\">" +"Advanced settings let you manage more parameters of the automatic registration algorithms"+"</p></html>");
		settingsDefaultButton.setToolTipText("<html><p width=\"500\">" +"Restore settings compute and set default parameters suited to your images"+"</p></html>");

		disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS});
		this.boxDisplay.setSelectedIndex(regManager.getCurrentAction().typeAutoDisplay);
		this.boxTypeAction.setSelectedIndex(regManager.getCurrentAction().typeAction);
		this.boxDisplayMan.setSelectedIndex(regManager.getCurrentAction().typeManViewer);
		this.boxOptimizer.setSelectedIndex(regManager.getCurrentAction().typeOpt==OptimizerType.BLOCKMATCHING ? 0 : 1);
		this.boxTypeTrans.setSelectedIndex(regManager.getCurrentAction().typeTrans == Transform3DType.RIGID ? 0 : regManager.getCurrentAction().typeTrans == Transform3DType.DENSE ? 2 : 1 );
		
		//Panel with buttons for the context of two image registration
		JPanel buttonsPanel=new JPanel();
		if(this.modeWindow==WINDOWTWOIMG) {
			buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
			buttonsPanel.setLayout(new GridLayout(2,3,40,40));
			buttonsPanel.add(runButton);
			buttonsPanel.add(abortButton);
			buttonsPanel.add(undoButton);
			buttonsPanel.add(finishButton);
			buttonsPanel.add(saveButton);
			buttonsPanel.add(sosButton);
			
			runButton.addActionListener(this);
			abortButton.addActionListener(this);
			undoButton.addActionListener(this);
			finishButton.addActionListener(this);
			saveButton.addActionListener(this);
			sosButton.addActionListener(this);
			colorStdActivatedButton=runButton.getBackground();
			colorGreenRunButton=new Color(100,255,100);
			
			abortButton.setToolTipText("<html><p width=\"500\">" +"Abort means killing a running operation and come back to the state before you clicked on Start this action."+
									   "Automatic registration is harder to kill. Please insist on this button until its colour fades to gray"+"</p></html>");
			finishButton.setToolTipText("<html><p width=\"500\">" +"Export aligned images and computed transforms"+"</p></html>");
			saveButton.setToolTipText("<html><p width=\"500\">" +"Save the current state of the plugin in a .fjm file, including the transforms and image paths. This .ijm file can be used later to restart from this point"+"</p></html>");
			sosButton.setToolTipText("<html><p width=\"500\">" +"Opens a contextual help"+"</p></html>");
			undoButton.setToolTipText("<html><p width=\"500\">" +"Undo works as you would expect : it delete the previous action, and recover the previous state of transforms and images. Fijiyama handles only one undo : two successive actions can't be undone"+"</p></html>");
			finishButton.setToolTipText("<html><p width=\"500\">" +"Export aligned images and computed transforms"+"</p></html>");
			saveButton.setToolTipText("<html><p width=\"500\">" +"Save the current state of the plugin in a .fjm file, including the transforms and image paths. This .ijm file can be used later to restart from this point"+"</p></html>");
			sosButton.setToolTipText("<html><p width=\"500\">" +"Opens a contextual help"+"</p></html>");
			undoButton.setToolTipText("<html><p width=\"500\">" +"Undo works as you would expect : it delete the previous action, and recover the previous state of transforms and images. Fijiyama handles only one undo : two successive actions can't be undone"+"</p></html>");
			
			setRunToolTip(MAIN);
			this.colorIdle=abortButton.getBackground();
			disable(ABORT);
			enable(new int[] {RUN,UNDO,SAVE,FINISH,SETTINGS});
		}

		
		else if(this.modeWindow==WINDOWSERIEPROGRAMMING) {
			buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
			buttonsPanel.setLayout(new GridLayout(2,2,40,40));
			buttonsPanel.add(addActionButton);
			buttonsPanel.add(removeSelectedButton);
			buttonsPanel.add(validatePipelineButton);
			buttonsPanel.add(sosButton);

			addActionButton.addActionListener(this);
			removeSelectedButton.addActionListener(this);
			validatePipelineButton.addActionListener(this);
			sosButton.addActionListener(this);

			addActionButton.setToolTipText("<html><p width=\"500\">" +"Click to add an action (bottom list), and configure it using upper menus"+"</p></html>");
			removeSelectedButton.setToolTipText("<html><p width=\"500\">" +"Remove the selected action (bottom list) from the global pipeline"+"</p></html>");
			validatePipelineButton.setToolTipText("<html><p width=\"500\">" +"Validate the global processing pipeline, and go to next step"+"</p></html>");

			enable(SETTINGS);
			listActions.setSelectedIndex(0);
		}
		
		else if(this.modeWindow==WINDOWSERIERUNNING) {
			buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
			buttonsPanel.setLayout(new GridLayout(2,3,40,40));
			buttonsPanel.add(runButton);
			buttonsPanel.add(abortButton);
			buttonsPanel.add(undoButton);
			buttonsPanel.add(runThroughButton);
			buttonsPanel.add(new JLabel(""));
			//			buttonsPanel.add(goBackStepButton);
			buttonsPanel.add(sosButton);

			runThroughButton.addActionListener(this);
			undoButton.addActionListener(this);
			runButton.addActionListener(this);
			abortButton.addActionListener(this);
			sosButton.addActionListener(this);

			runButton.setToolTipText("<html><p width=\"500\">" +"Click here to run the next step in the global pipeline (see the black console log)"+"</p></html>");
//			goBackStepButton.setToolTipText("<html><p width=\"500\">" +"Use this function to compute again a step that went not as well as you expected"+"</p></html>");
			if(regManager.getStep()>0)enable(UNDO);
			else disable(UNDO);
			disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS,GOBACKSTEP,ABORT});
			enableChainIfPossible();
		}

		//Panel with list of actions, used for registration of two images, and when programming registration pipelines for series
		JPanel titleActionPanel=new JPanel();
		titleActionPanel.setBorder(BorderFactory.createEmptyBorder(5,25,0,25));
		titleActionPanel.setLayout(new GridLayout(1,1,10,10));
		titleActionPanel.add(new JLabel(this.modeWindow==WINDOWSERIEPROGRAMMING ?  "Global registration pipeline : add/remove an action, select an action to modify it (using the menus), then approve the pipeline)" : "Global registration pipeline "));
		JPanel listActionPanel=new JPanel();
		listActionPanel.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
		listActionPanel.setLayout(new GridLayout(1,1,10,10));
		listActionPanel.add(actionsPane);
		listActions.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {actionClickedInList();}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
		});

		//Main frame and main panel
		registrationFrame=new JFrame();
		JPanel registrationPanelGlobal=new JPanel();
		registrationPanelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		registrationPanelGlobal.setLayout(new BoxLayout(registrationPanelGlobal, BoxLayout.Y_AXIS));
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(jscroll);
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(stepSettingsPanel);
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(buttonsPanel);
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(titleActionPanel);
		registrationPanelGlobal.add(listActionPanel);
		registrationPanelGlobal.add(new JSeparator());
		registrationFrame.add(registrationPanelGlobal);
		registrationFrame.setTitle("Fijiyama registration manager ");
		registrationFrame.pack();

		registrationFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		registrationFrame.addWindowListener(new WindowAdapter(){
             public void windowClosing(WindowEvent e){
                   System.out.println("CLOSING REGISTRATION WINDOW !");
                   closeAllViews();
                   registrationFrame.setVisible(false);
                   dispose();
                   actualizeLaunchingInterface(true);           
                   modeWindow=WINDOWIDLE;
             }
		});
		updateList();
		//updateBoxFieldsFromRegistrationAction(regManager.getCurrentAction());
		if(modeWindow!=WINDOWSERIERUNNING) updateBoxFieldsToCoherenceAndApplyToRegistrationAction();
		registrationFrame.setVisible(true);
		registrationFrame.repaint();
		VitimageUtils.adjustFrameOnScreen(registrationFrame,2,0);		
		logArea.setVisible(true);
		logArea.repaint();
		updateList();
	}
	
	public void startLaunchingInterface() {
		sosButton=new JButton("Sos");
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
		buttonPanel.setLayout(new GridLayout(3,2,20,20));
		buttonPanel.add(runTwoImagesButton);
		buttonPanel.add(runSerieButton);
		buttonPanel.add(loadFjmButton);
		buttonPanel.add(sosButton);
		buttonPanel.add(transformButton);
		buttonPanel.add(composeTransformsButton);
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
			sosButton.setEnabled(true);
			runTwoImagesButton.setEnabled(true);
			runSerieButton.setEnabled(true);
			composeTransformsButton.setEnabled(true);
			transformButton.setEnabled(true);
			loadFjmButton.setEnabled(true);
			
			sosButton.addActionListener(this);
			runTwoImagesButton.addActionListener(this);
			runSerieButton.addActionListener(this);
			transformButton.addActionListener(this);
			composeTransformsButton.addActionListener(this);
			loadFjmButton.addActionListener(this);
			
		}
		else {
			sosButton.setEnabled(false);
			runTwoImagesButton.setEnabled(false);
			runSerieButton.setEnabled(false);
			transformButton.setEnabled(false);
			composeTransformsButton.setEnabled(false);
			loadFjmButton.setEnabled(false);

			sosButton.removeActionListener(this);
			runTwoImagesButton.removeActionListener(this);
			runSerieButton.removeActionListener(this);
			transformButton.removeActionListener(this);			
			composeTransformsButton.removeActionListener(this);			
			loadFjmButton.removeActionListener(this);;
		}
	}


	
	
	
	
	
	
	
	
	
	/* Listeners of the launching interface */
	public void performActionInLaunchingInterface(ActionEvent e) {
				
		if(e.getSource()==this.sosButton)displaySosMessage(SOS_CONTEXT_LAUNCH);
		else if(e.getSource()==this.runTwoImagesButton) {
			if(! regManager.setupFromTwoImages())return;
			startTwoImagesRegistration();
			modeWindow=WINDOWTWOIMG;
		}
	
		else if(e.getSource()==this.runSerieButton) {
			regManager.setupSerieFromScratch();
			startSerie();
			modeWindow=WINDOWSERIERUNNING;
		}
		
		else if(e.getSource()==this.loadFjmButton) {
			regManager.setupFromFjmFile(regManager.openFjmFileAndGetItsPath());
			startSerie();
			modeWindow=WINDOWSERIERUNNING;
		}
		
		else if(e.getSource()==this.transformButton) {
			ItkTransform.transformImageWithGui();
		}
		
		else if(e.getSource()==this.composeTransformsButton) {
			ItkTransform.composeTransformsWithGui();
		}
	}
	
	/* Listeners for serie programming interface*/
	public void performActionInProgrammingSerieInterface(ActionEvent e) {
		if(e.getSource()==validatePipelineButton) {
			this.pipelineValidated=true;
		}
		
		else if(e.getSource()==addActionButton) {
			regManager.setStep(regManager.regActions.size()-1);
			RegistrationAction regAct=regManager.switchToFollowingAction();
			listActions.setSelectedIndex(regAct.step);
			this.updateBoxFieldsFromRegistrationAction(regAct);
		}
		
		else if(e.getSource()==removeSelectedButton) {
			RegistrationAction regAct=regManager.removeLastAction();
			listActions.setSelectedIndex(regAct.step);
			updateBoxFieldsFromRegistrationAction(regAct);
		}
	}	
	
	/* Listener of the running serie and running two images parts **********************************************************************************************/
	@Override
	public void actionPerformed(ActionEvent e) {
		logActionEvent(e);
		if(this.modeWindow==WINDOWIDLE) {
			System.out.println("IDLE !");
			performActionInLaunchingInterface(e);return;
		}
		if((e.getSource()==this.runTwoImagesButton ||  e.getSource()==this.loadFjmButton ||e.getSource()==this.runSerieButton || e.getSource()==this.transformButton || e.getSource()==this.composeTransformsButton)
				&& this.registrationFrame!=null && this.registrationFrame.isVisible()) {
			IJ.showMessage("A Registration manager is running, with the corresponding interface open. Please close this interface before any other operation.");
			return;
		}

		
		
		if(this.modeWindow==WINDOWSERIEPROGRAMMING) {
			performActionInProgrammingSerieInterface(e);
		}
		
		
	
		

		
		//Listeners for two image registration interface and serie registration interface
		/*Simple actions      */
		if( ( (e.getSource()==runButton && modeWindow==WINDOWSERIERUNNING)&& regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_SAVE) || e.getSource()==saveButton) {
			System.out.println("Start a saving action");
			disable(new int[] {RUN,RUNALL,FINISH,SAVE,UNDO});
			if(modeWindow==WINDOWTWOIMG)regManager.getCurrentAction().typeAction=RegistrationAction.TYPEACTION_SAVE;
			regManager.getCurrentAction().setDone();
			regManager.finishCurrentAction(new ItkTransform());//TODO : sort of a hack, to be solved. 
			regManager.saveSerieToFjmFile();
			System.out.println("Finishing a saving action");
			VitimageUtils.waitFor(500);
			enable(new int[] {FINISH,SAVE,RUN});
			enableChainIfPossible();
			enable(UNDO);
			if(passThroughActivated)passThrough("Save finished");
			return;
		}

		if( (e.getSource()==runButton && (modeWindow==WINDOWSERIERUNNING)&& regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_EXPORT) || e.getSource()==finishButton) {
			disable(new int[] {RUN,RUNALL,FINISH,SAVE,UNDO});
			if(modeWindow==WINDOWTWOIMG)regManager.getCurrentAction().typeAction=RegistrationAction.TYPEACTION_EXPORT;
			regManager.getCurrentAction().setDone();
			regManager.exportImagesAndComposedTransforms();
			regManager.finishCurrentAction(new ItkTransform());//TODO : sort of a hack, to be solved.
			enable(new int[] {FINISH,SAVE,RUN});
			enableChainIfPossible();
			enable(UNDO);
			if(passThroughActivated)passThrough("Export finished");
			return;
		}
		
		if(  (e.getSource()==runButton && modeWindow==WINDOWSERIERUNNING) && regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_VIEW ) {
			disable(new int[] {RUN,RUNALL,FINISH,SAVE,UNDO});
			final ExecutorService exec = Executors.newFixedThreadPool(1);
			exec.submit(new Runnable() {
				public void run() 
				{	showHyperImage(regManager.getViewOfImagesTransformedAndSuperposedSerieWithThisReference(regManager.images[regManager.referenceTime][regManager.referenceModality]),waitingTimeHyperImage);
				}
			});
			regManager.finishCurrentAction(new ItkTransform());//TODO : sort of a hack, to be solved.
			if(passThroughActivated)passThrough("View finished");
			return;
		}
		
		/*Settings and parameters modification*/		
		if(e.getSource()==settingsButton) {
			openSettingsDialog();					
			updateEstimatedTime();
		}
		if(e.getSource()==settingsDefaultButton) {
			regManager.defineDefaultSettingsForCurrentAction();
			updateBoxFieldsToCoherenceAndApplyToRegistrationAction();
		}

		if(!comboBoxChanged && (e.getSource()==boxTypeAction || e.getSource()==boxOptimizer  || e.getSource()==boxTypeTrans || e.getSource()==boxDisplay || e.getSource()==boxDisplayMan)) {		
			comboBoxChanged=true;
			boxClikedInGui();
			updateEstimatedTime();
			comboBoxChanged=false;
		}
		
		if(e.getSource()==undoButton && regManager.getStep()>0) {
			disable(new int[] {RUN,RUNALL,SAVE,FINISH,UNDO});
			System.out.println("Start an undo action");
			if(modeWindow==WINDOWSERIERUNNING)regManager.undoLastActionInSerieContext();
			else regManager.undoLastActionInTwoImagesContext();
			System.out.println("Finishing an undo action");
			
			enable(new int[] {RUN,SAVE,FINISH,BOXACT,UNDO});
			enableChainIfPossible();
			if(regManager.getStep()==0)disable(new int[] {UNDO,SAVE,FINISH});
		}				
		if(e.getSource()==sosButton) {
			runSos();
		}			
	
		
				
	
				
		/* Abort button, and associated behaviours depending on the context*/
		if(e.getSource()==abortButton) {
			disable(new int[] {RUN,RUNALL,SAVE,FINISH,UNDO, SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
			actionAborted=true;
			unpassThrough();
			System.out.println("Entering aborting procedure");
			
			//Aborting a manual registration or axis alignment procedure
			if(runButton.getText().equals("Position ok") || runButton.getText().equals("Axis ok")){
				disable(new int[] {RUN,RUNALL,ABORT});
				if((WindowManager.getImage(displayedNameImage1)!=null)) {WindowManager.getImage(displayedNameImage1).changes=false;WindowManager.getImage(displayedNameImage1).close();}
				if((WindowManager.getImage(displayedNameImage2)!=null)) {WindowManager.getImage(displayedNameImage2).changes=false;WindowManager.getImage(displayedNameImage2).close();}
				if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
				if(boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_3D && regManager.universe !=null) {
					regManager.universe.close();
					regManager.universe=null;
				}
				runButton.setText("Start this action");
				setRunToolTip(MAIN);
				enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,BOXTRANS,RUN,UNDO});
				enableChainIfPossible();
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


	
		if(e.getSource()==runThroughButton) {
			passThroughActivated=true;
			passThrough("Starting passThrough");
			return;
		}
		
		
				
				
				
				
				
		/* Run button is the bigger part : it is the starter/stopper for the main functions*/
		if(e.getSource()==runButton) {
			final ExecutorService exec = Executors.newFixedThreadPool(1);
			exec.submit(new Runnable() {
				public void run() 
				{			

					//In these two cases, do nothing
					if(regManager.getCurrentAction().isDone()) {	
						IJ.showMessage("Current action is already done. Nothing to do left.");
						return;
					}
					if( (modeWindow==WINDOWTWOIMG) && (regManager.axisAlignmentDone()) && (! (regManager.getCurrentAction().typeAction==2) ) ){	
						IJ.showMessage("Registration steps cannot be added after an axis alignement step. Use UNDO to return before axis alignement");
						return;
					}
					
					//Else
					disable(new int[] {RUN,RUNALL});
					
					
					
			
					//Automatic registration
					if(regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_AUTO){	
						disable(new int[] {SAVE,FINISH,SETTINGS,UNDO,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
						
						System.out.println("Starting automatic registration");
						//Automatic blockMatching registration
						if(regManager.getCurrentAction().typeOpt==OptimizerType.BLOCKMATCHING) {
							System.out.println("Starting BLK :"+actionAborted);
							actionAborted=false;
							runButton.setText("Running Blockmatching...");
							bmRegistration=BlockMatchingRegistration.setupBlockMatchingRegistration(regManager.getCurrentRefImage(),regManager.getCurrentMovImage(),regManager.getCurrentAction());
							bmRegistration.refRange=regManager.getCurrentRefRange();
							bmRegistration.movRange=regManager.getCurrentMovRange();
							bmRegistration.flagRange=true;
							bmRegistration.percentageBlocksSelectedByScore=regManager.getCurrentAction().selectScore;
							bmRegistration.minBlockVariance=0.04;
							bmRegistration.displayRegistration=regManager.getCurrentAction().typeAutoDisplay;
							bmRegistration.displayR2=false;
							bmRegistration.returnComposedTransformationIncludingTheInitialTransformationGiven=false;
							enable(ABORT);
							ItkTransform trTemp=bmRegistration.runBlockMatching(regManager.getCurrentMovComposedTransform());
							disable(ABORT);
							System.out.println("On y arrive. Abort vaut : "+actionAborted);
							if(! actionAborted) {
								regManager.finishCurrentAction(trTemp);
								bmRegistration.closeLastImages();
								bmRegistration.freeMemory();
							}
							actionAborted=false;
						}
						
								//Automatic Itk iconic registration
						else {
							System.out.println("Starting ITK :"+actionAborted);
							actionAborted=false;
							runButton.setText("Running Itk registration...");
							itkManager=new ItkRegistrationManager();
							itkManager.refRange=regManager.getCurrentRefRange();
							itkManager.movRange=regManager.getCurrentMovRange();
							itkManager.flagRange=true;
							itkManager.displayRegistration=regManager.getCurrentAction().typeAutoDisplay;
							itkManager.returnComposedTransformationIncludingTheInitialTransformationGiven=false;
							enable(ABORT);
							System.out.println("STARTING  ITK");
							ItkTransform trTemp=itkManager.runScenarioFromGui(new ItkTransform(),
									regManager.getCurrentRefImage(),
									regManager.getCurrentMovComposedTransform().transformImage(regManager.getCurrentRefImage(), regManager.getCurrentMovImage(),false),
									regManager.getCurrentAction().typeTrans, regManager.getCurrentAction().levelMin,regManager.getCurrentAction().levelMax,regManager.getCurrentAction().iterationsITK,regManager.getCurrentAction().learningRate);
							disable(ABORT);
							itkManager.freeMemory();
							itkManager=null;
							if(! actionAborted) {//TODO : this is not the same for blockmatching. What is the logic behind ?
								regManager.finishCurrentAction(trTemp);
							}
							actionAborted=false;
						}
						runButton.setText("Start this action");
						if(!actionAborted)updateView();
						enable(new int[] {RUN,SAVE,FINISH,SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP,UNDO});
						enableChainIfPossible();
						actionAborted=false;
						System.out.println("Finishing automatic registration"+passThroughActivated);
						if(passThroughActivated)passThrough("Registration finished"+passThroughActivated);
					}
					
					
			
					//Manual registration
					else if(regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_MAN){						
						disable(new int[] {RUN,RUNALL,UNDO});
						//Parameters verification
						if(regManager.getCurrentAction().typeTrans!=Transform3DType.RIGID && regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) {
							IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform."+
								"If you just intend to compute a rigid transform (no deformation / dilation), please select RIGID in the transformation list. Otherwise, select the 2d viewer in "+
								"the settings to compute a similarity from landmarks points, or select automatic block matching registration to compute a dense vector field");
							enable(new int[] {RUN,UNDO});
							enableChainIfPossible();							
							return;
						}
						if(regManager.getCurrentAction().typeTrans==Transform3DType.DENSE && regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_2D) {
							IJ.showMessage("Warning : transform is set to Vector field. But the 3d viewer can only compute Rigid and Similarity transform."+
								"Please select automatic block matching registration to compute a dense vector field");
							enable(new int[] {RUN,UNDO});
							enableChainIfPossible();							
							return;
						}

						//Starting manual registration
						if(runButton.getText().equals("Start this action")) {
							disable(new int[] {BOXACT,RUN,RUNALL,UNDO});
							runButton.setText("Position ok");
							System.out.println("Starting run. Currentaction="+regManager.getCurrentAction());
							ImagePlus imgMovCurrentState=regManager.getCurrentMovComposedTransform().transformImage(regManager.getCurrentRefImage(),regManager.getCurrentMovImage(),false);
							imgMovCurrentState.setDisplayRange(regManager.getCurrentMovRange()[0],regManager.getCurrentMovRange()[1]);
							if(regManager.getCurrentAction().typeAction==RegistrationAction.VIEWER_3D) {
								regManager.start3dManualRegistration(regManager.getCurrentRefImage(),imgMovCurrentState);
								setRunToolTip(MANUAL3D);
							}
							else {
								regManager.start2dManualRegistration(regManager.getCurrentRefImage(),imgMovCurrentState);
								setRunToolTip(MANUAL2D);								
							}
							enable(new int[] {ABORT,RUN});
							enableChainIfPossible();
							runButton.setBackground(colorGreenRunButton);
						}
						
						//Finish manual registration
						else {
							//Window verifications and wild abort if needed
							if( ( (boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_3D) && (regManager.universe==null)) || 
							( (boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage(displayedNameImage1)==null) || (WindowManager.getImage(displayedNameImage2)==null) ) ) ) {
								disable(new int[] {RUN,RUNALL,ABORT});
								if((WindowManager.getImage(displayedNameImage1)!=null)) WindowManager.getImage(displayedNameImage1).close();
								if((WindowManager.getImage(displayedNameImage2)!=null)) WindowManager.getImage(displayedNameImage2).close();
								if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
								runButton.setText("Start this action");
								setRunToolTip(MAIN);
								enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN,UNDO});
								enableChainIfPossible();								
								runButton.setBackground(colorStdActivatedButton);
								actionAborted=false;
								return;
							}
							
							//Verify number of landmarks, and return if the number of couples is < 5
							if((regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_2D) && (RoiManager.getInstance().getCount()<10 ) ) {IJ.showMessage("Please identify at least 10 points (5 correspondance couples)");enable(new int[] {RUN,RUNALL});return;}
							
							//Closing manual registration
							disable(new int[] {ABORT,RUN,RUNALL});
							actionAborted=false;
							ItkTransform tr=null;
								
							if(regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) 	tr=regManager.finish3dManualRegistration();
							else	tr=regManager.finish2dManualRegistration();
							regManager.finishCurrentAction(tr);
							updateView();
							runButton.setText("Start this action");
							setRunToolTip(MAIN);
							runButton.setBackground(colorStdActivatedButton);
							actionAborted=false;
							enable(new int[] {UNDO,BOXACT,FINISH,SAVE,RUN});
							enableChainIfPossible();							
						}
					}
					
				
					//Axis alignment
					else if(regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_ALIGN){						
						disable(new int[] {RUN,RUNALL});
						if(runButton.getText().equals("Start this action")) {
							if(!developerMode && !VitiDialogs.getYesNoUI("Warning : data finalization", "Warning : image alignment means to transform reference image.\n\n"+
									" For simplicity reasons, this operation ends the registration procedure. No more manual or automatic registration step\n"+
									" can be added after. If after this operation you want to add new registration steps, you will have to undo it before.\n"+
									"\nIf you feel unsafe about what is going on, you can answer \"No\" to abort this operation,\n"+
									" and use the \"Save current state\" button, before running again image alignement. \nConversely, "+
									"answer \"Yes\" to start alignment anyway.\n\nDo you want to start image alignment right now ?")) {enable(new int[] {RUN,RUNALL});return;}

							//Parameters verification
							if(boxTypeTrans.getSelectedIndex()>0 && boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_3D) {
								IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform."+
									"If you just intend to compute a rigid transform (no deformation / dilation), please select RIGID in the transformation list. Otherwise, select the 2d viewer in "+
									"the settings to compute a similarity from landmarks points, or select automatic block matching registration to compute a dense vector field");
								enable(new int[] {RUN});
								enableChainIfPossible();								
								actionAborted=false;
								return;
							}
							if(boxTypeTrans.getSelectedIndex()>1 && boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_2D) {
								IJ.showMessage("Warning : transform is set to Vector field. But the 3d viewer can only compute Rigid and Similarity transform."+
									"Please select automatic block matching registration to compute a dense vector field");
								enable(new int[] {RUN});
								enableChainIfPossible();
								actionAborted=false;
								return;
							}

							//Starting axis alignment
							disable(new int[] {BOXACT,FINISH,SAVE,SETTINGS,UNDO});
							runButton.setText("Axis ok");
							ImagePlus imgRefCurrentState=regManager.getCurrentRefImage();
							if(regManager.axisAlignmentDone())imgRefCurrentState=regManager.getCurrentRefComposedTransform().transformImage(regManager.getCurrentRefImage(),regManager.getCurrentRefImage(),false);
							imgRefCurrentState.setDisplayRange(regManager.getCurrentRefRange()[0],regManager.getCurrentRefRange()[1]);

							if(regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) {
								regManager.start3dManualRegistration(imgRefCurrentState,null);
								setRunToolTip(MANUAL3D);
							}
							else {
								regManager.start2dManualRegistration(regManager.getCurrentRefImage(),null);
								setRunToolTip(MANUAL2D);
							}
							enable(new int[] {ABORT,RUN});
							actionAborted=false;
							enableChainIfPossible();							
							runButton.setBackground(colorGreenRunButton);
						}

						//Finish axis alignment
						else {
							//Window verifications and wild abort if needed
							if( ( (regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) && (regManager.universe==null)) || 
									( (regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage(displayedNameImage1)==null) || (WindowManager.getImage(displayedNameImage2)==null) ) ) ) {
								System.out.println("Wild aborting");
								//Wild aborting procedure
								disable(new int[] {RUN,RUNALL,ABORT,UNDO});
								if((WindowManager.getImage(displayedNameImage1)!=null)) {WindowManager.getImage(displayedNameImage1).changes=false;WindowManager.getImage(displayedNameImage1).close();}
								if((WindowManager.getImage(displayedNameImage2)!=null)) {WindowManager.getImage(displayedNameImage2).changes=false;WindowManager.getImage(displayedNameImage2).close();}
								if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
								runButton.setText("Start this action");
								setRunToolTip(MAIN);
								enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN,UNDO});
								enableChainIfPossible();								
								runButton.setBackground(colorStdActivatedButton);
								actionAborted=false;
								return;
							}

							//Verify number of landmarks, and return if the number of couples is < 5
							if((regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_2D) && (RoiManager.getInstance().getCount()<10 ) ) {IJ.showMessage("Please identify at least 10 points (5 correspondance couples)");enable(new int[] {RUN,RUNALL});return;}

							//Closing axis alignement
							disable(new int[] {RUN,RUNALL,ABORT});
							ItkTransform tr=null;
							if(regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) {
								tr=regManager.finish3dManualRegistration();
							}
							else{
								tr=regManager.finish2dManualRegistration();
							}
							regManager.finishCurrentAction(tr);
							updateView();
							runButton.setText("Start this action");
							setRunToolTip(MAIN);
							actionAborted=false;
							enable(new int[] {UNDO,FINISH,SAVE,SETTINGS,BOXACT,RUN});
							enableChainIfPossible();							
							runButton.setBackground(colorStdActivatedButton);
						}
						actionAborted=false;
					}
					System.out.println("End of actionPerform. CurrReg="+regManager.getCurrentAction());
					System.out.println("...");
					System.out.println("...");
				}
			});
		}
		System.gc();
	}



	
	public void passThrough(String s) {
		System.out.println("\n\n"+s+"PASS THROUGH ! passhThroug="+passThroughActivated+"\n\n");
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run() 
			{			

				System.out.println("STARTING RUNNABLE"+passThroughActivated);
				if(regManager.getCurrentAction().typeAction==0 || regManager.getCurrentAction().typeAction==2) {
					System.out.println("GOING TO UNPASS"+passThroughActivated);
					unpassThrough();
					System.out.println("HAD UNPASS"+passThroughActivated);
					return;
				}
				System.out.println("DISABLING BUTTONS"+passThroughActivated);
				disable(new int[] {RUN,RUNALL,ABORT,UNDO});
				System.out.println("PERFORMING"+passThroughActivated);
				actionPerformed(new ActionEvent(runButton, 0, "Auto go on"));
				System.out.println("OK"+passThroughActivated);
			}
		});
		System.out.println("SENT"+passThroughActivated);
	}

	public void enableChainIfPossible() {
		if (regManager.getCurrentAction().typeAction==1)enable(RUNALL);
		else disable(RUNALL);
	}
	
	public void unpassThrough() {
		if(!passThroughActivated)return;
		IJ.showMessage("Chain run is finished : next action need human intervention");
		enable(new int[] {RUN,RUNALL});
		passThroughActivated=false;
		if(regManager.getStep()>0)enable(UNDO);
	}
	
	public void showHyperImage(ImagePlus hyp,int seconds) {
		hyp.show();
		hyp.setSlice(viewSlice);
		int lastingSeconds=seconds;
		while(lastingSeconds>0 && hyp.isVisible()) {
			hyp.setTitle(displayedNameHyperImage+" - "+lastingSeconds+" s before automatic closing");
			VitimageUtils.waitFor(1000);
			lastingSeconds--;
		}
		if(hyp.isVisible())hyp.close();
		enable(new int[] {UNDO,RUN,RUNALL});
	}
	
	
	/* Updating the Gui elements,menu, list, buttons and views **********************************************************************************************/	
	public void updateList() {
		this.listActions.setModel(regManager.getPipelineAslistModelForGUI());
		this.listActions.setSelectedIndex(regManager.getStep());
		ScrollUtil.scroll(listActions,ScrollUtil.SELECTED,new int[] {listActions.getSelectedIndex(),regManager.getNbSteps()+1});
	}
			
	public void updateEstimatedTime() {
		int estimatedTime=regManager.estimateTime(regManager.getCurrentAction());
		int nbMin=estimatedTime/60;
		int nbSec=estimatedTime%60;
		this.labelTime2.setText(""+nbMin+" mn and "+nbSec+" s");
	}
		
	public void actionClickedInList() {
		if(modeWindow!=WINDOWSERIEPROGRAMMING)return;
		System.out.println("Here1");
		if(listActions.getSelectedIndex()>=regManager.getNbSteps() ) return;
		System.out.println("Here2");

		disable(new int[] {BOXACT,BOXDISP,BOXDISPMAN,BOXOPT,BOXTRANS,SETTINGS});
		System.out.println("clicked number "+listActions.getSelectedIndex());
		regManager.changeCurrentAction(listActions.getSelectedIndex());
		updateList();
		VitimageUtils.waitFor(20);
		updateBoxFieldsFromRegistrationAction(regManager.getCurrentAction());
		System.out.println("Everything has been done");
	}
	
	public void boxClikedInGui() {
		setState(new int[] {BOXOPT,BOXTIME,BOXDISP,BOXTRANS,BOXACT},false);
		updateBoxFieldsToCoherenceAndApplyToRegistrationAction();
		updateList();
		VitimageUtils.waitFor(5);
		enable(new int[] {BOXACT,BOXTRANS});
		setState(new int[] {BOXOPT,BOXDISP },boxTypeAction.getSelectedIndex()==1);
		setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
		if(modeWindow==WINDOWSERIERUNNING)disable(new int[] {BOXACT,BOXTRANS,BOXOPT,BOXDISP,BOXDISPMAN});
	}

	public void updateBoxFieldsToCoherenceAndApplyToRegistrationAction() {
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
		regManager.getCurrentAction().updateFieldsFromBoxes(boxTypeAction.getSelectedIndex(),boxTypeTrans.getSelectedIndex(),boxOptimizer.getSelectedIndex(),boxDisplay.getSelectedIndex(),boxDisplayMan.getSelectedIndex(),modeWindow);
	}
 
	public void updateBoxFieldsFromRegistrationAction(RegistrationAction reg) {
		if(modeWindow==WINDOWTWOIMG && (!reg.isTransformationAction())) {reg.typeAction=RegistrationAction.TYPEACTION_MAN;reg.typeTrans=Transform3DType.RIGID ;}
		System.out.println("HERE EN ARRIVANT : "+reg.typeAction);
		boxTypeAction.setSelectedIndex(reg.typeAction < 3 ? reg.typeAction : 0);
		boxTypeTrans.setSelectedIndex(reg.typeTrans==Transform3DType.DENSE ? 2 : reg.typeTrans==Transform3DType.RIGID ? 0 : 1);
		boxDisplay.setSelectedIndex(reg.typeAutoDisplay);
		boxDisplayMan.setSelectedIndex(reg.typeManViewer);
		boxOptimizer.setSelectedIndex(reg.typeOpt==OptimizerType.BLOCKMATCHING ? 0 : 1);
		if(modeWindow!=WINDOWSERIERUNNING)updateBoxFieldsToCoherenceAndApplyToRegistrationAction();
		updateList();
		setState(new int[] {BOXOPT,BOXTIME,BOXDISP },boxTypeAction.getSelectedIndex()==1);
		setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
		if(modeWindow==WINDOWSERIERUNNING)disable(new int[] {BOXACT,BOXTRANS,BOXOPT,BOXDISP,BOXDISPMAN});
	}

	public void updateView() {
		if(this.modeWindow!=WINDOWTWOIMG)return;
		this.imgView=regManager.getViewOfImagesTransformedAndSuperposedTwoImg();
		imgView.show();
		VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),registrationFrame,0,0,10);
		double zoomFactor=Math.min((screenHeight/2)/imgView.getHeight()  ,  (screenWidth/2)/imgView.getWidth()); 
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
//		imgView.updatePosition(1, this.viewSlice,1);
		imgView.setSlice(viewSlice);
		
		System.out.println(viewSlice);
		imgView.updateAndRepaintWindow();
	}
	
	public static String getImgViewText(int st){
		return ( (st==0) ? "Superimposition before registration" :  ("Registration results after "+(st)+" step"+((st>1)? "s" : "")) );
	}
	
	public void closeAllViews() {
		for(int st=0;st<=regManager.getStep();st++) {
			if (WindowManager.getImage(getImgViewText(st))!=null)WindowManager.getImage(getImgViewText(st)).close();
		}
		if (WindowManager.getImage(displayedNameImage1)!=null)WindowManager.getImage(displayedNameImage2).close();
		if (WindowManager.getImage(displayedNameImage2)!=null)WindowManager.getImage(displayedNameImage2).close();
		if (RoiManager.getInstance()!=null)RoiManager.getInstance().close();
		if (regManager.universe!=null) regManager.universe.close();
	}	
	
	public void undoButtonPressed() {
		//Change the current view, and call update for RegistrationManager
		System.out.println("Entering undo callback");
		regManager.undoLastActionInTwoImagesContext();
		updateList();
		
	}
		

	
	
	
	
	
	
		
	
	/* Minor helpers and getters/setters************************************************************************************************/
	
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
					( (this.modeWindow==WINDOWTWOIMG) ? mainWindowTextTwoImgs : mainWindowTextProgramming )+
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
	        String[]levelsMax=new String[regManager.getMaxAcceptableLevelForTheCurrentAction()];
	        for(int i=0;i<regManager.getMaxAcceptableLevelForTheCurrentAction();i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[regManager.getCurrentAction().levelMax-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[regManager.getCurrentAction().levelMin-1]);
	        gd.addChoice("Higher accuracy (subpixellic level)", new String[] {"Yes","No"},regManager.getCurrentAction().higherAcc==1 ? "Yes":"No");
	        
	        gd.addMessage("Blocks dimensions. Blocks are image subparts\nCompared to establish correspondances between ref and mov images");
	        gd.addNumericField("Block half-size along X", regManager.getCurrentAction().bhsX, 0, 3, "original pixels");
	        gd.addNumericField("Block half-size along Y", regManager.getCurrentAction().bhsY, 0, 3, "original pixels");
	        gd.addNumericField("Block half-size along Z", regManager.getCurrentAction().bhsZ, 0, 3, "original pixels");

	        gd.addMessage("Searching neighbourhood. Distance to look for\nbetween a reference image block and a moving image block");
	        gd.addNumericField("Block neighbourhood along X", regManager.getCurrentAction().neighX, 0, 3, "subsampled pixels");
	        gd.addNumericField("Block neighbourhood along Y", regManager.getCurrentAction().neighY, 0, 3, "subsampled pixels");
	        gd.addNumericField("Block neighbourhood along Z",  regManager.getCurrentAction().neighZ, 0, 3, "subsampled pixels");

	        gd.addMessage("Spacing between two successive blocks\nalong each dimension");
	        gd.addNumericField("Striding along X", regManager.getCurrentAction().strideX, 0, 3, "subsampled pixels");
	        gd.addNumericField("Strinding along Y",  regManager.getCurrentAction().strideY, 0, 3, "subsampled pixels");
	        gd.addNumericField("Strinding along Z",  regManager.getCurrentAction().strideZ, 0, 3, "subsampled pixels");

	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  regManager.getCurrentAction().iterationsBM, 0, 3, "iterations");
	        if(this.boxTypeTrans.getSelectedIndex()==2)gd.addNumericField("Sigma for dense field smoothing", regManager.getCurrentAction().sigmaDense, 3, 6, regManager.getUnit());
	        gd.addNumericField("Percentage of blocks selected by score", regManager.getCurrentAction().selectScore, 0, 3, "%");
	        if(this.boxTypeTrans.getSelectedIndex()!=2)gd.addNumericField("Percentage kept in Least-trimmed square", regManager.getCurrentAction().selectLTS, 0, 3, "%");	        

	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        System.out.println("Go");
	        int a=gd.getNextChoiceIndex()+1; regManager.getCurrentAction().levelMax=a;
	        System.out.println("a="+a);
	        int b=gd.getNextChoiceIndex()+1; b=b<a ? b : a; regManager.getCurrentAction().levelMin=b;
	        System.out.println("b="+b);
	        a=1-gd.getNextChoiceIndex();
	        regManager.getCurrentAction().higherAcc=a;
	       	
	       	int c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().bhsX=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().bhsY=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().bhsZ=c;

	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().neighX=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().neighY=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().neighZ=c;

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().strideX=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().strideY=c;
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().strideZ=c;

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().iterationsBM=c;
	       	if(this.boxTypeTrans.getSelectedIndex()==2) {double d=gd.getNextNumber(); d=d<1E-6 ? 1E-6 : d; regManager.getCurrentAction().sigmaDense=d;}
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().selectScore=c;
	       	if(this.boxTypeTrans.getSelectedIndex()!=2) {c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().selectLTS=c;}
		}
		//Parameters for Itk Iconic
		else {//Itk parameters
			System.out.println("Opening settings for itk");
	        GenericDialog gd= new GenericDialog("Expert mode for Itk registration");
	        System.out.println("MaxAc="+regManager.getMaxAcceptableLevelForTheCurrentAction());
	        System.out.println("CurLevMin="+regManager.getCurrentAction().levelMin);
	        System.out.println("CurLevMax="+regManager.getCurrentAction().levelMax);
	        String[]levelsMax=new String[regManager.getMaxAcceptableLevelForTheCurrentAction()];for(int i=0;i<regManager.getMaxAcceptableLevelForTheCurrentAction();i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
			System.out.println("After critical");
			gd.addMessage(message);
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[regManager.getCurrentAction().levelMax-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[regManager.getCurrentAction().levelMin-1]);
	        
	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  regManager.getCurrentAction().iterationsITK, 0, 5, "iterations");
	        gd.addNumericField("Learning rate",  regManager.getCurrentAction().learningRate, 4, 8, " no unit");

	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        int param1=gd.getNextChoiceIndex()+1; 
	        regManager.getCurrentAction().levelMax=param1;

	        int param2=gd.getNextChoiceIndex()+1; param2=param2<param1 ? param2 : param1;
	        regManager.getCurrentAction().levelMin=param2;
	       	
	       	int param3=(int)Math.round(gd.getNextNumber());
	       	param3=param3<0 ? 0 : param3;  regManager.getCurrentAction().iterationsITK=param3;
	       	double param4=gd.getNextNumber();
	       	param4=param4<0 ? EPSILON : param4;  regManager.getCurrentAction().learningRate=param4;
		}		
	}

	public void serieIsFinished() {
		IJ.showMessage("Serie is finished !");
		disable(new int[] {RUN,RUNALL});
	}
	
	public void welcomeAndInformAboutComputerCapabilities() {		
		String[]str=regManager.checkComputerCapacity();
		addLog(str[0],0);
		addLog(str[1],0);		
	}

	public void waitForValidation() {
		this.pipelineValidated=false;
		while(!this.pipelineValidated)VitimageUtils.waitFor(500);
		pipelineValidated=false;
	}

	public void logActionEvent(ActionEvent e) {
		if(regManager.regActions==null || regManager.getCurrentAction()==null)return;
		if(!e.getActionCommand().equals("comboBoxChanged")){
			System.out.println("Starting event : "+e.getActionCommand()+" , "+e.getID()+" with step="+regManager.getStep()+" and current action ="+regManager.getCurrentAction().readableString(false));
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
			case SOS:this.sosButton.setEnabled(state);break;
			case RUNTWOIMG:this.runTwoImagesButton.setEnabled(state);break;
			case RUNSERIE:this.runSerieButton.setEnabled(state);break;
			case RUNNEXTSTEP:this.runNextStepButton.setEnabled(state);break;
			case GOBACKSTEP:this.goBackStepButton.setEnabled(state);break;
			case RUNTRANS:this.transformButton.setEnabled(state);break;
			case RUNTRANSCOMP:this.composeTransformsButton.setEnabled(state);break;
			case RUNALL:this.runThroughButton.setEnabled(state);break;
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
	
	public FijiyamaGUI getFijiyamaGUI() {
		return this;
	}

	public boolean getDebugMode() {
		return debugMode;
	}

	public String getRunButtonText() {
		return runButton.getText();				
	}
	
	public boolean isProgrammingSerie() {
		return modeWindow==WINDOWSERIEPROGRAMMING;
	}

	public boolean isRunningSerie() {
		return modeWindow==WINDOWSERIERUNNING;
	}
	
	public boolean currentContextIsSerie() {
		return (this.mode==MODE_SERIE);
	}

	public boolean isRunningTwoImagesTraining() {
		return modeWindow==WINDOWTWOIMG;
	}
	
}

	
				

