/*
 * 
 */
package io.github.rocsg.fijiyama.fijiyamaplugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import org.scijava.java3d.Transform3D;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import io.github.rocsg.fijiyama.common.Timer;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.BlockMatchingRegistration;
import io.github.rocsg.fijiyama.registration.ItkRegistration;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.OptimizerType;
import io.github.rocsg.fijiyama.registration.Transform3DType;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import io.github.rocsg.fijiyama.rsml.FSR;
import io.github.rocsg.fijiyama.rsml.RootModel;

// TODO: Auto-generated Javadoc
/**
 * The Class Fijiyama_GUI.
 *
 * @author Rocsg
 * Fijiyama is a user-friendly tool for 3d registration of images, multimodal series, and/or time-lapse series
 * To date, this work is under review. Article title : Fijiyama: a registration tool for 3D multimodal time-lapse imaging
 * 
 * Acknowledgements :
 * Johannes Schindelin and Albert Cardona (and many other people) for Fiji (ref Fiji)
 * Benjamin Schmid, Johannes Schindelin, Albert Cardona (et al) for the ImageJ 3dviewer.
 * The ITK and SimpleITK team that sets registration algorithms freely available
 * Gregoire Malandain, Sebastien Ourselin and Olivier Commowick for explanations about Block-Matching
 * 
 * TODO : 
 * ******* Critical bugs ********* 
 * (None)
 * 
 * ******* Elements to investigate and possible refactoring ********* 
 * (priority=0/3, difficulty=1/3) Uniform description of saveSerie function in RegistrationManager.java for both modes (series and two images registration)
 * (priority=1/3, difficulty=2/3) homogeneity of the threads and runnable lifecycles for automatic registration : harmonization of the kill switch behaviour between ITK and BM 
 * (priority=1/3, difficulty=2/3) Raising RegistrationAction class to host basic behaviours for these actions 
 * (priority=1/3, difficulty=1/3) the export method getViewOfImagesTransformedAndSuperposedSerieWithThisReferenc is very long to execute when dealing with big images and dense fields. It should be optimized...
 * 
 * ******* Fixes testing ********* 
 * (None)
 * 
 * ******* Testing needs ********* 
 * Opening an ended series. Why is it crashing for the five time series ?
 * DOI unitary tests 
 * (None)
 * 
 * ******* Next evolutions ********* 
 * Opening an ended series. Why is it crashing for the five time series ?
 * DOI unitary tests running quasi-autonomously
 * (None)
 * 
 * ******* User requests ********* 
 * (priority=2/3, difficulty=1/3) Make similarity possible for manual registration to handle miscalibration (Anne-Sophie Spilmont, Khalifa Diouf)
 * (priority=2/3, difficulty=1/3) Integration of interest point auto-detection on geometrical bases (Cedric Moisy)
 * (priority=3/3, difficulty=2/3) Hyperimage support (Jean-Luc and Cedric)  Has to be tested
 * (priority=1/3, difficulty=2/3) Possibility of changing variance selection. Imply to version the fjm file to add new parameters to these archives
 */


//TODO : when using Fijiyama for BM1, encountered a non-blocking bug : on a manual registration (the first, I think), the ref image was given with all the channels.
// The same situation reproduces then with the last step of dense field registration
//Seems to be ok, just the image 1125 that is weird

public class Fijiyama_GUI extends PlugInFrame implements ActionListener {
	
	/** The Constant percentileDisplay. */
	public static final double percentileDisplay=99;
	
	/** The Constant widthRangeDisplay. */
	public static final double widthRangeDisplay=1.2;
	
	/** The do stress test. */
	public boolean doStressTest=false;
	
	/** The is survivor vnc tunnel little display. */
	public boolean isSurvivorVncTunnelLittleDisplay=false;
	
	/** The version name. */
	public String versionName="Handsome honeysuckle";
	
	/** The time version flag. */
	public String timeVersionFlag="  v4.2.0 2023-07-07 -16:08 PM (Mtp), Andrew feature";
	
	/** The version flag. */
	public String versionFlag=versionName+timeVersionFlag;
	
	
	/** The img view. */
	public ImagePlus imgView;
	
	/** The enable high acc. */
	private boolean enableHighAcc=true;
	
	/** The debug mode. */
	private boolean debugMode=true;
	
	/** The auto rep. */
	private boolean autoRep=false;
	
	/** The pass through activated. */
	public volatile boolean passThroughActivated=false;
	
	/** The itk manager. */
	ItkRegistration itkManager;
	
	/** The bm registration. */
	BlockMatchingRegistration bmRegistration;
	
	/** The color std activated button. */
	private Color colorStdActivatedButton;
	
	/** The color green run button. */
	private Color colorGreenRunButton;
	
	/** The interface is running. */
	public boolean interfaceIsRunning=false;
	
	/** The sos level. */
	public int sosLevel=0;
	
	/** The reg interrogation level. */
	public int regInterrogationLevel=0;
	
	/** The Constant saut. */
	//Frequent html codes for the help window
	private static final String saut="<div style=\"height:1px;display:block;\"> </div>";
	
	/** The Constant startPar. */
	private static final String startPar="<p  width=\"650\" >";
	
	/** The Constant nextPar. */
	private static final String nextPar="</p><p  width=\"650\" >";

	/** The Constant MODE_TWO_IMAGES. */
	//Flags for the window modes
	public static final int MODE_TWO_IMAGES=2;
	
	/** The Constant MODE_SERIE. */
	public static final int MODE_SERIE=3;
	
	/** The mode. */
	public int mode=MODE_TWO_IMAGES;

	/** The Constant MAIN. */
	//Flags for the "run" button tooltip
	private static final int MAIN=81;
	
	/** The Constant MANUAL2D. */
	private static final int MANUAL2D=82;
	
	/** The Constant MANUAL3D. */
	private static final int MANUAL3D=83;

	/** The Constant BOXACT. */
	//Identifiers for boxLists
	private static final int BOXACT=91;
	
	/** The Constant BOXOPT. */
	private static final int BOXOPT=92;
	
	/** The Constant BOXTRANS. */
	private static final int BOXTRANS=93;
	
	/** The Constant BOXTIME. */
	private static final int BOXTIME=94;
	
	/** The Constant BOXDISP. */
	private static final int BOXDISP=95;
	
	/** The Constant BOXDISPMAN. */
	private static final int BOXDISPMAN=96;

	/** The Constant SETTINGS. */
	//Identifiers for buttons of registration two imgs
	private static final int SETTINGS=101;
	
	/** The Constant RUN. */
	private static final int RUN=102;
	
	/** The Constant RUNALL. */
	private static final int RUNALL=108;
	
	/** The Constant UNDO. */
	private static final int UNDO=103;
	
	/** The Constant ABORT. */
	private static final int ABORT=104;
	
	/** The Constant SAVE. */
	private static final int SAVE=105;
	
	/** The Constant FINISH. */
	private static final int FINISH=106;
	
	/** The Constant SOS. */
	private static final int SOS=107;
	
	/** The epsilon. */
	private final double EPSILON=1E-8;
	
	/** The Constant RUNTWOIMG. */
	//Identifiers for buttons of start window
	private static final int RUNTWOIMG=111;
	
	/** The Constant RUNSERIE. */
	private static final int RUNSERIE=112;
	
	/** The Constant RUNTRANS. */
	private static final int RUNTRANS=113;
	
	/** The Constant RUNTRANSCOMP. */
	private static final int RUNTRANSCOMP=114;
	
	/** The Constant BATCHRSML. */
	private static final int BATCHRSML=115;

	/** The Constant RUNNEXTSTEP. */
	//Identifiers for buttons of registration serie
	private static final int RUNNEXTSTEP=131;
	
	/** The Constant GOBACKSTEP. */
	private static final int GOBACKSTEP=132;

	
	/** The Constant WINDOWTWOIMG. */
	//Flags for the state of the registration manager
	public static final int WINDOWTWOIMG=121;
	
	/** The Constant WINDOWSERIEPROGRAMMING. */
	public static final int WINDOWSERIEPROGRAMMING=122;
	
	/** The Constant WINDOWSERIERUNNING. */
	public static final int WINDOWSERIERUNNING=123;
	
	/** The Constant WINDOWIDLE. */
	public static final int WINDOWIDLE=124;
	
	/** The mode window. */
	int modeWindow=WINDOWSERIEPROGRAMMING;
	
	/** The Constant serialVersionUID. */
	//Interface parameters
	private static final long serialVersionUID = 1L;
	
	/** The sos context launch. */
	private int SOS_CONTEXT_LAUNCH=0;
	
	/** The action aborted. */
	private volatile boolean actionAborted=false;
	
	/** The developer mode. */
	boolean developerMode=false;
	
	/** The spaces. */
	String spaces="                                                                                                                                   ";
	
	/** The view slice. */
	public int viewSlice;

	
	/** The text actions. */
	//Registration interface attributes
	private String[]textActions=new String[] {"1- Manual registration","2- Automatic registration","3- Align both images with XYZ axis"," "," "," ","-- Evaluate mismatch"};
	
	/** The text optimizers. */
	private String[]textOptimizers=new String[] {"Block-Matching","ITK","Mass center","Inertia axis"};
	
	/** The text transforms BM. */
	private String[]textTransformsBM=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)","Vector field "};
	
	/** The text transforms ITK. */
	private String[]textTransformsITK=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)"};
	
	/** The text transforms mass. */
	private String[]textTransformsMass=new String[] {"Rigid (no deformations)"};
	
	/** The text transforms MAN. */
	private String[]textTransformsMAN=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)","Vector field "};
	
	/** The text transforms ALIGN. */
	private String[]textTransformsALIGN=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)"};
	
	/** The text display ITK. */
	private String[]textDisplayITK=new String[] {"0-Only at the end (faster)","1-Dynamic display (slower)"};
	
	/** The text display BM. */
	private String[]textDisplayBM=new String[] {"0-Only at the end (faster)","1-Dynamic display (slower)","2-Also display score map (slower+)"};
	
	/** The text display man. */
	private String[]textDisplayMan=new String[] {"3d viewer (volume rendering)","2d viewer (classic slicer)"};
	
	/** The text display mass. */
	private String[]textDisplayMass=new String[] {"0-Only at the end (faster)"};
	
	/** The list actions. */
	//Interface text, label and lists
	private JList<String>listActions=new JList<String>(new String[]{spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces});
	
	/** The actions pane. */
	JScrollPane actionsPane = new JScrollPane(listActions);
	
	/** The label next action. */
	private JLabel labelNextAction = new JLabel("Choose the next action :", JLabel.LEFT);
    
    /** The box type action. */
    public JComboBox<String>boxTypeAction=new JComboBox<String>(textActions);
	
	/** The label optimizer. */
	private JLabel labelOptimizer = new JLabel("Automatic registration optimizer :", JLabel.LEFT);
	
	/** The box optimizer. */
	public JComboBox<String>boxOptimizer=new JComboBox<String>(  textOptimizers   );
	
	/** The label transformation. */
	private JLabel labelTransformation = new JLabel("Transformation to estimate :", JLabel.LEFT);
	
	/** The box type trans. */
	public JComboBox<String>boxTypeTrans=new JComboBox<String>( textTransformsBM  );
	
	/** The label view. */
	private JLabel labelView = new JLabel("Automatic registration display :", JLabel.LEFT);
	
	/** The box display. */
	public JComboBox<String>boxDisplay=new JComboBox<String>( textDisplayBM );
	
	/** The label view man. */
	private JLabel labelViewMan = new JLabel("Manual registration viewer :", JLabel.LEFT);
	
	/** The box display man. */
	public JComboBox<String>boxDisplayMan=new JComboBox<String>( textDisplayMan );
	
	/** The label time 1. */
	private JLabel labelTime1 = new JLabel("Estimated time for this action :", JLabel.LEFT);
	
	/** The label time 2. */
	private JLabel labelTime2 = new JLabel("0 mn and 0s", JLabel.CENTER);

	//Buttons, Frames, Panels
	/** The settings button. */
	//Registration Frame buttons
	public JButton settingsButton=new JButton("Advanced settings...");
	
	/** The settings default button. */
	private JButton settingsDefaultButton=new JButton("Restore default settings...");
	
	/** The run button. */
	private JButton runButton=new JButton("Start this action");
	
	/** The run through button. */
	private JButton runThroughButton=new JButton("Chain-run automatic steps");
	
	/** The abort button. */
	private JButton abortButton = new JButton("Abort");
	
	/** The undo button. */
	private JButton undoButton=new JButton("Undo");
	
	/** The save button. */
	private JButton saveButton=new JButton("Save current state");
	
	/** The finish button. */
	private JButton finishButton=new JButton("Export results");
	
	/** The sos button. */
	private JButton sosButton=new JButton("Help");

	/** The run two images button. */
	//Launching frame buttons
	private JButton runTwoImagesButton = new JButton("Two images registration (training mode)");
	
	/** The batch rsml button. */
	private JButton batchRsmlButton = new JButton("Batch correction of Rsml files");
	
	/** The run serie button. */
	private JButton runSerieButton = new JButton("Series registration (N-times and/or N-modalities)");
	
	/** The load fjm button. */
	private JButton loadFjmButton = new JButton("Open a previous study (two imgs or series) from a fjm file");
	
	/** The transform button. */
	private JButton transformButton = new JButton("Apply a computed transform to another image");
	
	/** The compose transforms button. */
	private JButton composeTransformsButton = new JButton("Compose successive transformations into a single one");
	
	/** The run next step button. */
	private JButton runNextStepButton = new JButton("Run next step");
	
	/** The go back step button. */
	private JButton goBackStepButton = new JButton("Coming soon...");

	/** The add action button. */
	//Programming serie buttons
	private JButton addActionButton = new JButton("Add an action to pipeline");
	
	/** The remove selected button. */
	private JButton removeSelectedButton = new JButton("Remove last action");
	
	/** The validate pipeline button. */
	public JButton validatePipelineButton = new JButton("Approve inter-time pipeline");
	
	/** The registration frame. */
	//Some more Gui objects and constants
	public JFrame registrationFrame;
	
	/** The log area. */
	private JTextArea logArea=new JTextArea("", 11,10);
	
	/** The color idle. */
	private Color colorIdle;
	
	/** The frame launch. */
	public JFrame frameLaunch;
	
	/** The screen height. */
	private int screenHeight=0;
	
	/** The screen width. */
	private int screenWidth=0;
	
	/** The last view sizes. */
	public int[] lastViewSizes=new int[] {700,700};//Only useful for serie running
	
	/** The displayed name image 1. */
	public final String displayedNameImage1="Image 1";
	
	/** The displayed name image 2. */
	public final String displayedNameImage2="Image 2";
	
	/** The displayed name combined image. */
	public final String displayedNameCombinedImage="Data_combined";
	
	/** The displayed name hyper image. */
	public final String displayedNameHyperImage="Data_combined";
	
	/** The waiting time hyper image. */
	private final int waitingTimeHyperImage=30;


	/** The combo box changed. */
	protected boolean comboBoxChanged=false;
	
	/** The reg manager. */
	private RegistrationManager regManager;
	
	/** The my survival font for little displays. */
	private Font mySurvivalFontForLittleDisplays=null;
	
	/** The undo button has been pressed. */
	private boolean undoButtonHasBeenPressed=false;
	
	/** The yogitea level. */
	private int yogiteaLevel=0;
	
	/** The pair level. */
	private int pairLevel;
	



	

	
	
	

	/**
	 * Instantiates a new fijiyama GUI.
	 */
	/*Starting points*************************************************************************************************************/
	public Fijiyama_GUI() {
		super("FijiYama : a versatile registration tool for Fiji");
		if(new File("/users/bionanonmri/").exists()) {
			this.isSurvivorVncTunnelLittleDisplay=true;
			IJ.showMessage("Detected Bionano server. \nSurvival display, but numerous cores");
		}
		regManager=new RegistrationManager(this);
		this.screenHeight=Toolkit.getDefaultToolkit().getScreenSize().height;
		this.screenWidth=Toolkit.getDefaultToolkit().getScreenSize().width;
		if(this.screenWidth>1920)this.screenWidth/=2;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher( 
            new KeyEventDispatcher()  {  
                public boolean dispatchKeyEvent(KeyEvent e){
                    if(e.getID() == KeyEvent.KEY_PRESSED){
                        handleKeyPress(e);
                    }
                    return false;
                }  
        });	 
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[]args) {//TestMethod
		@SuppressWarnings("unused")
		ImageJ ij=new ImageJ();
		Fijiyama_GUI reg=new Fijiyama_GUI();
		//reg.timeVersionFlag="Release time : "+new SimpleDateFormat("yyyy-MM-dd - hh:mm").format(new Date());
		reg.versionFlag=reg.versionName+reg.timeVersionFlag;
		
		reg.developerMode=true;
		reg.debugMode=true;
		reg.run("");
	}
	
	/**
	 * Run.
	 *
	 * @param arg the arg
	 */
	public void run(String arg) {
			IJ.log(System.getProperties().toString());
			if(VitimageUtils.isWindowsOS() && System.getProperties().toString().contains("zulu")) {
				IJ.showMessage("You run windows with zulu JDK. We are sorry, but this is unconvenient\n"+
						" The plugin will close to let you adjust your setup (two operations to make). "+""
					+ "\nThen please read the windows installation instructions on the plugin page"+
						"\nhttps://imagej.net/plugins/fijirelax ");
				return;
			}
			IJ.log("\nZulu check ok\n\n");
			startLaunchingInterface();
			modeWindow=WINDOWIDLE;
	}

	//TODO : match centroids
	
	
	/**
	 * Start two images registration.
	 */
	public void startTwoImagesRegistration() {
		this.mode=MODE_TWO_IMAGES;
		this.modeWindow=WINDOWTWOIMG;
		startRegistrationInterface();
		updateView();
		enable(new int[] {RUN,SETTINGS,SOS,BOXACT,BOXTRANS,BOXDISPMAN});		
		welcomeAndInformAboutComputerCapabilities();
		if(regManager.getStep()>0)enable(new int[] {RUN,SAVE,UNDO,FINISH});
	}

	/**
	 * Start serie.
	 */
	public void startSerie(){
		this.mode =MODE_SERIE;
		this.modeWindow=WINDOWSERIERUNNING;
		startRegistrationInterface();
		updateBoxFieldsFromRegistrationAction(regManager.getCurrentAction());
		disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS});
		enable(new int[] {RUNNEXTSTEP,GOBACKSTEP,SOS});	
		welcomeAndInformAboutComputerCapabilities();	
		addLog("Waiting for you to start next action : "+regManager.getCurrentAction().readableString(false),0);
	}
	
	
	/**
	 * Find fjm file in dir.
	 *
	 * @param dir the dir
	 * @return the string
	 */
	public String findFjmFileInDir(String dir) {
		File f=new File(dir);
		String[]strs=f.list();
		String result="";
		if(strs!=null && strs.length>0)for(String str : strs)if(str.length()>=4 && str.substring(str.length()-4,str.length()).equals(".fjm"))result=str;
		if(result.equals(""))return null;
		return new File(dir,result).getAbsolutePath();		
	}
	
	
	
	

	
	
	/**
	 * Start registration interface.
	 */
	/* Registration Manager gui  and launching interface gui ************************************************************************************************/
	public void startRegistrationInterface() {

		IJ.log("Starting Fijiyama registration interface");
		actualizeLaunchingInterface(false);         
		//Panel with console-style log informations and requests
		JPanel consolePanel=new JPanel();
		consolePanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		consolePanel.setLayout(new GridLayout(1,1,0,0));
		logArea.setSize(isSurvivorVncTunnelLittleDisplay ? 400 : 600,isSurvivorVncTunnelLittleDisplay ?  57 : 80);
		logArea.setBackground(new Color(10,10,10));
		logArea.setForeground(new Color(245,255,245));
		logArea.setFont(new Font(Font.DIALOG,Font.PLAIN,isSurvivorVncTunnelLittleDisplay ? 10 : 14));
		JScrollPane jscroll=new JScrollPane(logArea);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setEditable(false);	

       //Panel with step settings, used for registration of two images, and when programming registration pipelines for series
		JPanel stepSettingsPanel=new JPanel();
		mySurvivalFontForLittleDisplays=null;
		if(isSurvivorVncTunnelLittleDisplay ) {
			stepSettingsPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));		
			stepSettingsPanel.setLayout(new GridLayout(8,2,6,4));
			String name=boxTypeTrans.getFont().getFamily();
			mySurvivalFontForLittleDisplays=new Font(name,Font.BOLD,11);
			labelNextAction.setFont(mySurvivalFontForLittleDisplays);
			logArea.setFont(mySurvivalFontForLittleDisplays);
			boxTypeAction.setFont(mySurvivalFontForLittleDisplays);
			labelTransformation.setFont(mySurvivalFontForLittleDisplays);
			boxTypeTrans.setFont(mySurvivalFontForLittleDisplays);
			labelOptimizer.setFont(mySurvivalFontForLittleDisplays);
			boxOptimizer.setFont(mySurvivalFontForLittleDisplays);
			labelView.setFont(mySurvivalFontForLittleDisplays);
			boxDisplay.setFont(mySurvivalFontForLittleDisplays);
			labelViewMan.setFont(mySurvivalFontForLittleDisplays);
			boxDisplayMan.setFont(mySurvivalFontForLittleDisplays);
			labelTime1.setFont(mySurvivalFontForLittleDisplays);
			labelTime2.setFont(mySurvivalFontForLittleDisplays);
			settingsButton.setFont(mySurvivalFontForLittleDisplays);
			settingsDefaultButton.setFont(mySurvivalFontForLittleDisplays);
			sosButton.setFont(mySurvivalFontForLittleDisplays);
			finishButton.setFont(mySurvivalFontForLittleDisplays);
			runThroughButton.setFont(mySurvivalFontForLittleDisplays);
			runButton.setFont(mySurvivalFontForLittleDisplays);
			saveButton.setFont(mySurvivalFontForLittleDisplays);
			abortButton.setFont(mySurvivalFontForLittleDisplays);
			undoButton.setFont(mySurvivalFontForLittleDisplays);
			validatePipelineButton.setFont(mySurvivalFontForLittleDisplays);
			removeSelectedButton.setFont(mySurvivalFontForLittleDisplays);
			addActionButton.setFont(mySurvivalFontForLittleDisplays);
			goBackStepButton.setFont(mySurvivalFontForLittleDisplays);
			runNextStepButton.setFont(mySurvivalFontForLittleDisplays);
		}
		else{
			stepSettingsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));		
			stepSettingsPanel.setLayout(new GridLayout(9,2,15,10));
		}

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
		if(! isSurvivorVncTunnelLittleDisplay )stepSettingsPanel.add(new JLabel(""));
		stepSettingsPanel.add(labelTime1);
		stepSettingsPanel.add(labelTime2);		
		stepSettingsPanel.add(new JLabel(""));
		if(! isSurvivorVncTunnelLittleDisplay )stepSettingsPanel.add(new JLabel(""));
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

		if(modeWindow!=WINDOWSERIERUNNING) {
			disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS});
			this.boxDisplay.setSelectedIndex(regManager.getCurrentAction().typeAutoDisplay);
			this.boxTypeAction.setSelectedIndex(regManager.getCurrentAction().typeAction);
			this.boxDisplayMan.setSelectedIndex(regManager.getCurrentAction().typeManViewer);
			if(regManager.getCurrentAction().typeOpt==OptimizerType.BLOCKMATCHING)this.boxOptimizer.setSelectedIndex(0);
			else if(regManager.getCurrentAction().typeOpt==OptimizerType.MASSCENTER)this.boxOptimizer.setSelectedIndex(2);
			else if(regManager.getCurrentAction().typeOpt==OptimizerType.INERTIA_AXIS)this.boxOptimizer.setSelectedIndex(3);
			else this.boxOptimizer.setSelectedIndex(1);
			this.boxTypeTrans.setSelectedIndex(regManager.getCurrentAction().typeTrans == Transform3DType.RIGID ? 0 : regManager.getCurrentAction().typeTrans == Transform3DType.DENSE ? 2 : 1 );
		}
		
		//Panel with buttons for the context of two image registration
		JPanel buttonsPanel=new JPanel();
		if(this.modeWindow==WINDOWTWOIMG) {
			if(isSurvivorVncTunnelLittleDisplay ) {
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
				buttonsPanel.setLayout(new GridLayout(2,3,10,10));
			}
			else {
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
				buttonsPanel.setLayout(new GridLayout(2,3,40,40));
			}
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
			
			int width=isSurvivorVncTunnelLittleDisplay ? 350 : 500;
			abortButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Abort means killing a running operation and come back to the state before you clicked on Start this action."+
									   "Automatic registration is harder to kill. Please insist on this button until its colour fades to gray"+"</p></html>");
			finishButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Export aligned images and computed transformations"+"</p></html>");
			saveButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Save the current state of the plugin in a .fjm file, including the transformations and image paths."+
										" This .ijm file can be loaded later to restart from this point"+"</p></html>");
			sosButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Opens a contextual help"+"</p></html>");
			undoButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Undo delete the previous action, and recover the previous state of transformations and images"+"</p></html>");
			
			setRunToolTip(MAIN);
			this.colorIdle=abortButton.getBackground();
			disable(ABORT);
			enable(new int[] {RUN,UNDO,SAVE,FINISH,SETTINGS});
		}

		
		else if(this.modeWindow==WINDOWSERIEPROGRAMMING) {
			if(isSurvivorVncTunnelLittleDisplay ) {
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
				buttonsPanel.setLayout(new GridLayout(2,2,10,10));
			}
			else{
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
				buttonsPanel.setLayout(new GridLayout(2,2,40,40));				
			}
			buttonsPanel.add(addActionButton);
			buttonsPanel.add(removeSelectedButton);
			buttonsPanel.add(validatePipelineButton);
			buttonsPanel.add(sosButton);

			addActionButton.addActionListener(this);
			removeSelectedButton.addActionListener(this);
			validatePipelineButton.addActionListener(this);
			sosButton.addActionListener(this);

			int width=isSurvivorVncTunnelLittleDisplay ? 350 : 500;
			addActionButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Click to add an action (bottom list), and configure it using upper menus"+"</p></html>");
			removeSelectedButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Remove the selected action (bottom list) from the global pipeline"+"</p></html>");
			validatePipelineButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Validate the global processing pipeline, and go to next step"+"</p></html>");

			enable(SETTINGS);
			listActions.setSelectedIndex(0);
		}
		
		else if(this.modeWindow==WINDOWSERIERUNNING) {
			if(isSurvivorVncTunnelLittleDisplay ) {
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
				buttonsPanel.setLayout(new GridLayout(2,3,10,10));
			}
			else{
				buttonsPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
				buttonsPanel.setLayout(new GridLayout(2,3,40,40));			
			}
			buttonsPanel.add(runButton);
			buttonsPanel.add(abortButton);
			buttonsPanel.add(undoButton);
			buttonsPanel.add(runThroughButton);
			buttonsPanel.add(new JLabel(""));
			buttonsPanel.add(sosButton);

			runThroughButton.addActionListener(this);
			undoButton.addActionListener(this);
			runButton.addActionListener(this);
			abortButton.addActionListener(this);
			sosButton.addActionListener(this);

			int width=isSurvivorVncTunnelLittleDisplay ? 350 : 500;
			runButton.setToolTipText("<html><p width=\""+(width)+"\">" +"Click here to run the next step in the global pipeline (see the black console log)"+"</p></html>");
			if(regManager.getStep()>0)enable(UNDO);
			else disable(UNDO);
			disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS,GOBACKSTEP,ABORT});
			enableChainIfPossible();
		}

		//Panel with list of actions, used for registration of two images, and when programming registration pipelines for series
		JPanel titleActionPanel=new JPanel();
		JPanel listActionPanel=new JPanel();
		if(isSurvivorVncTunnelLittleDisplay ) {
			titleActionPanel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
			titleActionPanel.setLayout(new GridLayout(1,1,0,0));
		}
		else {
			titleActionPanel.setBorder(BorderFactory.createEmptyBorder(5,25,0,25));
			titleActionPanel.setLayout(new GridLayout(1,1,10,10));			
		}
		JLabel jlab=new JLabel(this.modeWindow==WINDOWSERIEPROGRAMMING ?  "Global registration pipeline : add/remove an action, select an action to modify it (using the menus), then approve the pipeline)" : "Global registration pipeline ");
		if(this.isSurvivorVncTunnelLittleDisplay)jlab.setFont(mySurvivalFontForLittleDisplays);
		titleActionPanel.add(jlab);
		if(isSurvivorVncTunnelLittleDisplay ) {
			listActionPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
			listActionPanel.setLayout(new GridLayout(1,1,0,0));
		}
		else {
			listActionPanel.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
			listActionPanel.setLayout(new GridLayout(1,1,10,10));		
		}
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
		if(isSurvivorVncTunnelLittleDisplay ) {
			registrationFrame.setSize(600,680);
			registrationPanelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		}
		else {
			registrationFrame.setSize(750,850);
			registrationPanelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));			
		}
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
		if(isSurvivorVncTunnelLittleDisplay ) {
			registrationFrame.setSize(600,680);
		}
		else {
			registrationFrame.setSize(750,850);
		}
		
		registrationFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		registrationFrame.addWindowListener(new WindowAdapter(){
             public void windowClosing(WindowEvent e){
                   IJ.showMessage("See you next time !");
                   registrationFrame.setVisible(false);
                   regManager.freeMemory();
                   closeAllViews();
               }
		});
		updateList();
		if(modeWindow!=WINDOWSERIERUNNING) updateBoxFieldsToCoherenceAndApplyToRegistrationAction(true);
		registrationFrame.setVisible(true);
		registrationFrame.repaint();
		VitimageUtils.adjustFrameOnScreen(registrationFrame,2,0);		
		logArea.setVisible(true);
		logArea.repaint();
		updateList();

	}

	

	/**
	 * Start launching interface.
	 */
	public void startLaunchingInterface() {
		IJ.log("Starting Fijiyama launching interface");
		IJ.log("Welcome to Fijiyama "+versionFlag);
		IJ.log("\nUser requests, issues ? \nContact : romainfernandez06ATgmail.com");
		IJ.log(".\n\n Message update : 11 may 2023.");
		IJ.log("\nRecent fix: temporary issue with 8-bit support due to recent update. Thanks Pablo for finding out the situation producing the bug");
		IJ.log("\nFijiyama is free and always will. However it is a pleasure to know that it helps in any way.");
		IJ.log("We received much positive feedbacks, and we discover from time to time in journals what you're up to with this tool.\n We wish you to have nice registrations with Fijiyama !\n");
		IJ.log(versionFlag);
		this.modeWindow=WINDOWIDLE;
		sosButton=new JButton("Help");
		runTwoImagesButton=new JButton("Two images registration (training mode)");
		batchRsmlButton=new JButton("Batch correction of Rsml files");
		runSerieButton=new JButton("Series registration (N-times and/or N-modalities)");
		transformButton=new JButton("Apply a computed transform to another image");
		composeTransformsButton=new JButton("Compose successive transformations into a single one");
		JPanel globalPanel=new JPanel();
		globalPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		globalPanel.setLayout(new BoxLayout(globalPanel, BoxLayout.Y_AXIS));
		
		JPanel somethingPanel=new JPanel();
		somethingPanel.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));		
		somethingPanel.setLayout(new GridLayout(2,1,20,20));
		JLabel jlab=new JLabel("Fijiyama : a versatile 3d registration tool for Fiji",JLabel.CENTER);
		JLabel jlab2=new JLabel("Version : "+versionFlag,JLabel.CENTER);
		somethingPanel.add(jlab);
		somethingPanel.add(jlab2);
		JPanel buttonPanel=new JPanel();
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));		
		buttonPanel.setLayout(new GridLayout(3,2,20,20));
		buttonPanel.add(runTwoImagesButton);
		buttonPanel.add(runSerieButton);
		buttonPanel.add(loadFjmButton);
		buttonPanel.add(batchRsmlButton);
		buttonPanel.add(transformButton);
		buttonPanel.add(composeTransformsButton);
		buttonPanel.add(sosButton);
		frameLaunch=new JFrame("Fijiyama : a versatile 3d registration tool for Fiji");
		globalPanel.add(new JSeparator());
		globalPanel.add(somethingPanel);
		globalPanel.add(buttonPanel);
		globalPanel.add(new JSeparator());
		frameLaunch.add(globalPanel);
		frameLaunch.pack();
		actualizeLaunchingInterface(true);
		frameLaunch.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){                  
                  IJ.showMessage("See you next time !");
                  regManager.freeMemory();
                  closeAllViews();
            }
		});
		frameLaunch.setVisible(true);
		frameLaunch.repaint();		
	}
	
	/**
	 * Actualize launching interface.
	 *
	 * @param expectedState the expected state
	 */
	public void actualizeLaunchingInterface(boolean expectedState) {
		if(expectedState) {
			sosButton.setEnabled(true);
			runTwoImagesButton.setEnabled(true);
			batchRsmlButton.setEnabled(true);
			runSerieButton.setEnabled(true);
			composeTransformsButton.setEnabled(true);
			transformButton.setEnabled(true);
			loadFjmButton.setEnabled(true);
			
			sosButton.addActionListener(this);
			runTwoImagesButton.addActionListener(this);
			batchRsmlButton.addActionListener(this);
			runSerieButton.addActionListener(this);
			transformButton.addActionListener(this);
			composeTransformsButton.addActionListener(this);
			loadFjmButton.addActionListener(this);
		}
		else {
			sosButton.setEnabled(false);
			runTwoImagesButton.setEnabled(false);
			batchRsmlButton.setEnabled(false);
			runSerieButton.setEnabled(false);
			transformButton.setEnabled(false);
			composeTransformsButton.setEnabled(false);
			loadFjmButton.setEnabled(false);

			sosButton.removeActionListener(this);
			runTwoImagesButton.removeActionListener(this);
			batchRsmlButton.removeActionListener(this);
			runSerieButton.removeActionListener(this);
			transformButton.removeActionListener(this);			
			composeTransformsButton.removeActionListener(this);			
			loadFjmButton.removeActionListener(this);;
		}
	}


	
	
	
	
	
	
	/**
	 * Perform action in launching interface.
	 *
	 * @param e the e
	 */
	/* Listeners of the launching interface */
	public void performActionInLaunchingInterface(ActionEvent e) {
		if(e.getSource()==this.sosButton)displaySosMessage(SOS_CONTEXT_LAUNCH);
		else if(e.getSource()==this.runTwoImagesButton) {
			if(! regManager.setupFromTwoImages(null))return;
			frameLaunch.setVisible(false);
			startTwoImagesRegistration();
			modeWindow=WINDOWTWOIMG;
		}
		else if(e.getSource()==this.batchRsmlButton) {
			startBatchRsml(null,null,true);
			frameLaunch.setVisible(false);
			modeWindow=WINDOWTWOIMG;
		}
	
		else if(e.getSource()==this.runSerieButton) {
			modeWindow=WINDOWSERIEPROGRAMMING;
			if(regManager.startSetupSerieFromScratch(0,null)) {}
			else {actualizeLaunchingInterface(true);modeWindow=WINDOWIDLE;}
			frameLaunch.setVisible(false);
		}
		
		else if(e.getSource()==this.loadFjmButton) {
			String path=regManager.openFjmFileAndGetItsPath();
			if(path!=null)regManager.setupFromFjmFile(path);
			else {
				frameLaunch.setVisible(false);
				return;
			}
			if(this.mode==MODE_SERIE) {
				startSerie();
				modeWindow=WINDOWSERIERUNNING;
			}
			else {
				startTwoImagesRegistration();
				modeWindow=WINDOWTWOIMG;
			}
			frameLaunch.setVisible(false);
		}
		
		else if(e.getSource()==this.transformButton) {
			ItkTransform.transformImageWithGui();
			frameLaunch.setVisible(false);
		}
		
		else if(e.getSource()==this.composeTransformsButton) {
			ItkTransform.composeTransformsWithGui();
			frameLaunch.setVisible(false);
		}
	}
	
	/**
	 * Perform action in programming serie interface.
	 *
	 * @param e the e
	 */
	/* Listeners for serie programming interface*/
	public void performActionInProgrammingSerieInterface(ActionEvent e) {
		if(e.getSource()==validatePipelineButton) {
			boolean isValid=true;
			int defect=0;
			for(int i=0;i<regManager.regActions.size()-1;i++) {
				if(!RegistrationAction.isValidOrder(regManager.regActions.get(i),regManager.regActions.get(i+1))) {
					defect=i;
					isValid=false;
				}
			}
			if(!isValid) {
				IJ.showMessage("Wrong pipeline : transforms should go in a logical order. \nA rougher transformation goes before finer, and manual goes before automatic\n.The problem was encountered between "+
			"steps "+(""+defect)+" and "+(defect+1));
				return;
			}
			regManager.defineSerieRegistrationPipeline("SEND FROM INTERFACE");
		}
		
		else if(e.getSource()==addActionButton) {
			boolean isValid=true;
			int defect=regManager.regActions.size()-2;
			if(regManager.regActions.size()>=2 && (!RegistrationAction.isValidOrder(regManager.regActions.get(regManager.regActions.size()-2),regManager.regActions.get(regManager.regActions.size()-1)))) isValid=false;
			if(!isValid) {
				IJ.showMessage("Wrong pipeline : transforms should go in a logical order. \nA rougher transformation goes before finer, and manual goes before automatic\n.The problem was encountered between "+
			"steps "+(""+defect)+" and "+(defect+1));
				return;
			}
			regManager.setStep(regManager.regActions.size()-1);
			RegistrationAction regAct=regManager.switchToFollowingAction();
			listActions.setSelectedIndex(regAct.step);
			this.updateBoxFieldsFromRegistrationAction(regAct);
		}
		
		else if(e.getSource()==removeSelectedButton) {
			RegistrationAction regAct=regManager.removeLastAction();
			listActions.setSelectedIndex(regAct.step);
			updateList();
			updateBoxFieldsFromRegistrationAction(regAct);
		}
		else if(((e.getSource()==boxTypeAction && boxTypeAction.hasFocus()) ||
				 (e.getSource()==boxOptimizer  && boxOptimizer.hasFocus()) ||
				 (e.getSource()==boxTypeTrans  && boxTypeTrans.hasFocus())  ||
				 (e.getSource()==boxDisplay    && boxDisplay.hasFocus())    ||
				 (e.getSource()==boxDisplayMan)&& boxDisplayMan.hasFocus()) ) {		
			if(modeWindow==WINDOWTWOIMG && (boxTypeAction.getSelectedIndex()>2) && (boxTypeAction.getSelectedIndex()!=RegistrationAction.TYPEACTION_EVALUATE)) {
				boxTypeAction.setSelectedIndex(0);
			}
			boxClikedInGui();
			updateEstimatedTime();
		}
		/*Settings and parameters modification*/		
		if(e.getSource()==settingsButton) {
			addLog("Modifying settings...", 1);
			openSettingsDialog();					
			updateEstimatedTime();
			addLog("Settings closed.", 1);
		}
		if(e.getSource()==settingsDefaultButton) {
			addLog("Settings set to default.", 1);
			regManager.defineDefaultSettingsForCurrentAction();
			updateBoxFieldsToCoherenceAndApplyToRegistrationAction(true);
		}

	}	
	
	/**
	 * Action performed.
	 *
	 * @param e the e
	 */
	/* Listener of the running serie and running two images parts **********************************************************************************************/
	@SuppressWarnings("deprecation")
	@Override
	public void actionPerformed(ActionEvent e) {
		logActionEvent(e);
		if(this.modeWindow==WINDOWIDLE) {
			performActionInLaunchingInterface(e);return;
		}
		if((e.getSource()==this.runTwoImagesButton || e.getSource()==this.batchRsmlButton ||  e.getSource()==this.loadFjmButton ||e.getSource()==this.runSerieButton || e.getSource()==this.transformButton || e.getSource()==this.composeTransformsButton)
				&& this.registrationFrame!=null && this.registrationFrame.isVisible()) {
			IJ.showMessage("A Registration manager is running, with the corresponding interface open. Please close this interface before any other operation.");
			return;
		}

		
		
		if(this.modeWindow==WINDOWSERIEPROGRAMMING) {
			performActionInProgrammingSerieInterface(e);
			return;
		}
		

		//Listeners for two image registration interface and serie registration interface
		/*Simple actions      */
		if(e.getSource()==saveButton || ( (e.getSource()==runButton && modeWindow==WINDOWSERIERUNNING)&& regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_SAVE) ) {
			disable(new int[] {RUN,FINISH,SAVE,UNDO});
			disable(RUNALL);
			addLog("Saving current state...", 1);
			if(modeWindow==WINDOWTWOIMG)regManager.getCurrentAction().typeAction=RegistrationAction.TYPEACTION_SAVE;
			regManager.getCurrentAction().setDone();
			regManager.finishCurrentAction(new ItkTransform());
			regManager.saveSerieToFjmFile();
			enable(new int[] {FINISH,SAVE,RUN});
			enableChainIfPossible();
			enable(UNDO);
			if(passThroughActivated)passThrough("Save finished");
			addLog("Saving done.", 1);
			return;
		}

		if(e.getSource()==finishButton || (e.getSource()==runButton && (modeWindow==WINDOWSERIERUNNING)&& regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_EXPORT)) {
			addLog("Exporting results...", 1);
			disable(new int[] {RUN,RUNALL,FINISH,SAVE,UNDO});
			if(modeWindow==WINDOWTWOIMG)regManager.getCurrentAction().typeAction=RegistrationAction.TYPEACTION_EXPORT;
			VitimageUtils.waitFor(20);
			regManager.getCurrentAction().setDone();
			double valType= (modeWindow==WINDOWSERIERUNNING) ? regManager.getCurrentAction().paramOpt : 8;
			regManager.exportImagesAndComposedTransforms(valType);
			regManager.finishCurrentAction(new ItkTransform());
			enable(new int[] {FINISH,SAVE,RUN});
			enableChainIfPossible();
			enable(UNDO);
			if(passThroughActivated)passThrough("Export finished");
			addLog("Export ok.", 1);
			return;
		}
	
		
		
		if(  (e.getSource()==runButton && modeWindow==WINDOWSERIERUNNING) && regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_VIEW ) {
			addLog("Viewing results...", 1);
			disable(new int[] {RUN,RUNALL,FINISH,SAVE,UNDO});
			final ExecutorService exec = Executors.newFixedThreadPool(1);
			exec.submit(new Runnable() {
				public void run() 
				{	
					showHyperImage(regManager.getViewOfImagesTransformedAndSuperposedSerieWithThisReference(regManager.images[regManager.referenceTime][regManager.referenceModality],false,false,""),waitingTimeHyperImage);
					enable(new int[] {FINISH,SAVE,RUN});
					enableChainIfPossible();
				}
			});
			regManager.finishCurrentAction(new ItkTransform());
			if(passThroughActivated)unpassThrough("Please wait for the resulting combined hyperimage to build and appear on your screen\nbefore going on the pipeline.");
			addLog("Viewing ok.", 1);
			return;
		}
		
		/*Settings and parameters modification*/		
		if(e.getSource()==settingsButton) {
			addLog("Modifying settings...", 1);
			openSettingsDialog();					
			updateEstimatedTime();
			addLog("Settings closed.", 1);
		}
		if(e.getSource()==settingsDefaultButton) {
			addLog("Settings set to default.", 1);
			regManager.defineDefaultSettingsForCurrentAction();
			updateBoxFieldsToCoherenceAndApplyToRegistrationAction(true);
		}

		if(!comboBoxChanged && ((e.getSource()==boxTypeAction && boxTypeAction.hasFocus()) 
							  ||(e.getSource()==boxOptimizer  && boxOptimizer.hasFocus())
							  ||(e.getSource()==boxTypeTrans  && boxTypeTrans.hasFocus())
							  ||(e.getSource()==boxDisplay    && boxDisplay.hasFocus())
							  ||(e.getSource()==boxDisplayMan && boxDisplayMan.hasFocus()))) {		
	
			if(modeWindow==WINDOWTWOIMG && (boxTypeAction.getSelectedIndex()>2) && (boxTypeAction.getSelectedIndex()!=RegistrationAction.TYPEACTION_EVALUATE)) {
				boxTypeAction.setSelectedIndex(0);
			}
			comboBoxChanged=true;
			boxClikedInGui();
			updateEstimatedTime();
			comboBoxChanged=false;
		}
		
		if(e.getSource()==undoButton && regManager.getStep()>0) {
			if(!undoButtonHasBeenPressed)if(!VitiDialogs.getYesNoUI("Undo", "Undo will remove last action and restore previous state. Confirm ?"))return;
			undoButtonHasBeenPressed=true;
			addLog("Undoing last action...", 1);
			disable(new int[] {RUN,RUNALL,SAVE,FINISH,UNDO});
			if(modeWindow==WINDOWSERIERUNNING)regManager.undoLastActionInSerieContext();
			else regManager.undoLastActionInTwoImagesContext();
			enable(new int[] {RUN,SAVE,FINISH,BOXACT,UNDO});
			enableChainIfPossible();
			if(regManager.getStep()==0)disable(new int[] {UNDO,SAVE,FINISH});
			addLog("Undo ok.", 1);
		}				
		if(e.getSource()==sosButton) {
			runSos();
		}			
	
		
				
	
				
		/* Abort button, and associated behaviours depending on the context*/
		if(e.getSource()==abortButton) {
			addLog("Trying to abort a running action...", 1);
			//Aborting a manual registration or axis alignment procedure
			if(runButton.getText().equals("Position ok") || runButton.getText().equals("Axis ok")){
				disable(new int[] {RUN,RUNALL,SAVE,FINISH,UNDO, SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
				actionAborted=true;
				unpassThrough("Manual registration interruption caught. Manual mode activated.");			
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
		        addKeyListener(IJ.getInstance());
			}
			//Aborting automatic blockmatching registration, killing threads, and checking if threads are deads
			else if(runButton.getText().equals("Running Blockmatching...")){
				if(bmRegistration==null || bmRegistration.threads==null) {
					addLog("To early for aborting automatic registration. Try in a few seconds...", 1);return;
				}
				int nThreads=bmRegistration.threads.length;
				if(nThreads<0) {
					addLog("To early for aborting automatic registration. Try in a few seconds...", 1);
					return;
				}
				disable(new int[] {RUN,RUNALL,SAVE,FINISH,UNDO, SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
				actionAborted=true;
				unpassThrough("Registration interruption caught. Manual mode activated.");			
				while(!bmRegistration.bmIsInterruptedSucceeded) {
					VitimageUtils.waitFor(200);
					bmRegistration.bmIsInterrupted=true;
					if(bmRegistration.threads==null)bmRegistration.bmIsInterruptedSucceeded=true;
					else for(int th=0;th<nThreads;th++)bmRegistration.threads[th].interrupt();
				}
			}
			//Aborting automatic itk iconic registration, killing threads, and checking if threads are deads
			else if(runButton.getText().equals("Running Itk registration...")){
				itkManager.itkRegistrationInterrupted=true;
				if(itkManager== null || itkManager.registrationThread==null || (!itkManager.registrationThread.isAlive())) {
					addLog("To early for aborting automatic registration. Try in a few seconds...", 1);
					return;
				}
				int trial=0;
				disable(new int[] {RUN,RUNALL,SAVE,FINISH,UNDO, SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
				actionAborted=true;
				unpassThrough("Registration interruption caught. Manual mode activated.");			
				while(itkManager != null && itkManager.registrationThread!=null && itkManager.registrationThread.isAlive() && trial<100) {
					trial++;
					VitimageUtils.waitFor(200);	

					if(itkManager!=null) {
						itkManager.itkRegistrationInterrupted=true;
						if(itkManager.registrationThread.isAlive()) itkManager.registrationThread.stop();
					}
				}
				if(itkManager!=null) {
					itkManager.freeMemory();
					itkManager=null;
				}
			}
			runButton.setText("Start this action");
			addLog("Aborting ok.", 1);
		}				


	
		if(e.getSource()==runThroughButton) {
			addLog("Starting chain-run...", 1);
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
					if( (modeWindow==WINDOWTWOIMG) && (regManager.axisAlignmentDone()) && ( (regManager.getCurrentAction().typeAction!=2) && (regManager.getCurrentAction().typeAction!=6) ) ){	
						IJ.showMessage("Registration steps cannot be added after an axis alignement step. Use UNDO to return before axis alignement");
						return;
					}
					
					//Else
					disable(new int[] {RUN,RUNALL,FINISH,SAVE});
					
					
					
			
					//Automatic registration
					if(regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_AUTO){	
						if(regManager.estimatedTime>250 && modeWindow==WINDOWTWOIMG) {
							if(!VitiDialogs.getYesNoUI("Warning: computation time","Computation time estimated to "+regManager.estimatedTime+" seconds\n"+
								"Click Yes to run this action, no to go back to settings\n.\n.\nHint : to reduce computation time, you can:\n"+
									"\n-set levelMin and levelMax to a higher value"+
									"\n-set strides to a higher value"+
									"\n-reduce the number of iterations"+
									"\n-set block size to a lesser value"
									)) {enable(new int[] {RUN,RUNALL,FINISH,SAVE});return;}
						}
						disable(new int[] {RUN,SAVE,FINISH,SETTINGS,UNDO,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
						//Automatic blockMatching registration
						if(regManager.getCurrentAction().typeOpt==OptimizerType.BLOCKMATCHING) {
							addLog("Starting Block-matching registration...", 1);
							actionAborted=false;
							runButton.setText("Running Blockmatching...");
							ItkTransform trTemp=null;
							for(int phase=(doStressTest ? 0 : 1);phase<2;phase++) {
								bmRegistration=BlockMatchingRegistration.setupBlockMatchingRegistration(regManager.getCurrentRefImage(),regManager.getCurrentMovImage(),regManager.getCurrentAction());
								bmRegistration.consoleOutputActivated=isSurvivorVncTunnelLittleDisplay;
								bmRegistration.timingMeasurement=developerMode;
								//TODO
								bmRegistration.setRefRange(regManager.getCurrentRefRange());
								bmRegistration.setMovRange(regManager.getCurrentMovRange());
								bmRegistration.flagRange=true;
								bmRegistration.percentageBlocksSelectedByScore=regManager.getCurrentAction().selectScore;
								bmRegistration.minBlockVariance=0.04;
								bmRegistration.displayRegistration=regManager.getCurrentAction().typeAutoDisplay;
								bmRegistration.displayR2=false;
	
								if(regManager.isBionanoImagesWithCapillary)bmRegistration.mask=regManager.maskImageArray[regManager.getCurrentAction().refTime][regManager.getCurrentAction().refMod].duplicate();
								if(regManager.isGe3d)bmRegistration.mask=VitimageUtils.maskWithoutCapillaryInGe3DImage(regManager.getCurrentRefImage());
								
								else if(modeWindow==WINDOWTWOIMG /*&& regManager.getCurrentAction().typeTrans==Transform3DType.DENSE*/) {
									regManager.setPathToMask();
									if(regManager.maskImage!=null)bmRegistration.mask=VitimageUtils.imageCopy(regManager.maskImage);
								}
								else if(modeWindow==WINDOWSERIERUNNING /*&& regManager.getCurrentAction().typeTrans==Transform3DType.DENSE*/) {
									if(regManager.setMaskImage())bmRegistration.mask=VitimageUtils.imageCopy(regManager.maskImage);
								}
								bmRegistration.returnComposedTransformationIncludingTheInitialTransformationGiven=false;
								enable(ABORT);
								if(isSurvivorVncTunnelLittleDisplay)bmRegistration.adjustZoomFactor(0.5);

								trTemp=bmRegistration.runBlockMatching(regManager.getCurrentMovComposedTransform(),phase==0);
							}
							disable(ABORT);
							if(! actionAborted) {
								regManager.finishCurrentAction(trTemp);
								bmRegistration.closeLastImages();
								bmRegistration.freeMemory();
							}
							actionAborted=false;
							addLog("Block-matching registration finished.", 1);
						}
						
								//Automatic Itk iconic registration
						else {
							addLog("Starting Itk registration...", 1);
							actionAborted=false;
							runButton.setText("Running Itk registration...");
							itkManager=new ItkRegistration();
							itkManager.refRange=regManager.getCurrentRefRange();
							itkManager.movRange=regManager.getCurrentMovRange();
							itkManager.flagRange=true;
							itkManager.displayRegistration=regManager.getCurrentAction().typeAutoDisplay;
							itkManager.returnComposedTransformationIncludingTheInitialTransformationGiven=false;
							enable(ABORT);
							ItkTransform trTemp=itkManager.runScenarioFromGui(new ItkTransform(),
									regManager.getCurrentRefImage(),
									regManager.getCurrentMovComposedTransform().transformImage(regManager.getCurrentRefImage(), regManager.getCurrentMovImage(),false),
									regManager.getCurrentAction().typeTrans, regManager.getCurrentAction().levelMinLinear,regManager.getCurrentAction().levelMaxLinear,regManager.getCurrentAction().iterationsITK,regManager.getCurrentAction().learningRate);
							disable(ABORT);
							if(itkManager==null || itkManager.itkRegistrationInterrupted) {
								enable(new int[] {RUN,SAVE,FINISH,SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP,UNDO});
								return;
							}
							if(itkManager!=null)itkManager.freeMemory();
							itkManager=null;
							if(! actionAborted) {
								regManager.finishCurrentAction(trTemp);
							}
							actionAborted=false;
							addLog("Itk registration finished", 1);
						}
						runButton.setText("Start this action");
						if(!actionAborted)updateView();
						enable(new int[] {RUN,SAVE,FINISH,SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP,UNDO});
						enableChainIfPossible();
						actionAborted=false;
						if(passThroughActivated)passThrough("Registration finished"+passThroughActivated);
					}
					
					
			
					//Manual registration
					else if(regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_MAN){						
						disable(new int[] {RUN,RUNALL,UNDO});
						//Parameters verification
						if(regManager.getCurrentAction().typeTrans!=Transform3DType.RIGID && regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) {
							IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform. Please check your settings");
							enable(new int[] {RUN,UNDO,SAVE,FINISH});
							enableChainIfPossible();							
							addLog("Wrong arguments. Manual registration is over...", 1);
							return;
						}
						
	
						//Starting manual registration
						if(runButton.getText().equals("Start this action")) {
							addLog("Starting manual registration...", 1);
							disable(new int[] {BOXACT,RUN,RUNALL,UNDO});
							disable(new int[] {BOXACT,FINISH,SAVE,SETTINGS,UNDO});
							runButton.setText("Position ok");
							ImagePlus imgMovCurrentState=regManager.getCurrentMovComposedTransform().transformImage(regManager.getCurrentRefImage(),regManager.getCurrentMovImage(),false);
							imgMovCurrentState.setDisplayRange(regManager.getCurrentMovRange()[0],regManager.getCurrentMovRange()[1]);
							if(regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) {
								if(true)System.out.println("Starting viewer 3D in Fijiyama GUI");
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
							addLog("Waiting for position confirmation from user (green button)...", 1);

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
								addLog("Manual registration took a wild abort exception (images may have been closed during the run)...", 1);
								return;
							}
							
							//Verify number of landmarks, and return if the number of couples is < 5
							if((regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_2D) && (RoiManager.getInstance().getCount()<8 ) && regManager.getCurrentAction().typeTrans != Transform3DType.DENSE ) {IJ.showMessage("Please identify at least 8 points (4 correspondance couples)");enable(new int[] {RUN,RUNALL});return;}
							
							//Closing manual registration
							disable(new int[] {ABORT,RUN,RUNALL});
							actionAborted=false;
							ItkTransform tr=null;
								
							if(regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) 	tr=regManager.finish3dManualRegistration();
							else	tr=regManager.finish2dManualRegistration();
							regManager.finishCurrentAction(tr);
							if(!regManager.isSerie)updateView();
							runButton.setText("Start this action");
							setRunToolTip(MAIN);
							runButton.setBackground(colorStdActivatedButton);
							actionAborted=false;
							enable(new int[] {UNDO,BOXACT,FINISH,SAVE,RUN,SETTINGS});
							enableChainIfPossible();							
							addLog("Manual registration finished.", 1);
						}
					}
					
				
					//Evaluate alignment
					else if(regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_EVALUATE){
						if(boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_3D) {
							IJ.showMessage("Please select the 2d viewer for this action");enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN,UNDO});
							return;
						}
						disable(new int[] {RUN,RUNALL,UNDO});
						//Starting manual registration
						if(runButton.getText().equals("Start this action")) {
							addLog("Starting evaluation...", 1);
							disable(new int[] {BOXACT,RUN,RUNALL,UNDO});
							disable(new int[] {BOXACT,FINISH,SAVE,SETTINGS,UNDO});
							runButton.setText("Position ok");
							ImagePlus imgMovCurrentState=regManager.getCurrentMovComposedTransform().transformImage(regManager.getCurrentRefImage(),regManager.getCurrentMovImage(),false);
							imgMovCurrentState.setDisplayRange(regManager.getCurrentMovRange()[0],regManager.getCurrentMovRange()[1]);
							regManager.start2dManualRegistration(regManager.getCurrentRefImage(),imgMovCurrentState);
							setRunToolTip(MANUAL2D);								

							enable(new int[] {ABORT,RUN});
							runButton.setBackground(colorGreenRunButton);
							addLog("Waiting for user to confirm points for evaluation...", 1);
						}
						
						//Finish avaluation
						else {
							//Window verifications and wild abort if needed
							if( ( (boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_3D) || 
							( (boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage(displayedNameImage1)==null) || (WindowManager.getImage(displayedNameImage2)==null) ) ) ) ) {
								disable(new int[] {RUN,RUNALL,ABORT});
								if((WindowManager.getImage(displayedNameImage1)!=null)) WindowManager.getImage(displayedNameImage1).close();
								if((WindowManager.getImage(displayedNameImage2)!=null)) WindowManager.getImage(displayedNameImage2).close();
								if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
								runButton.setText("Start this action");
								setRunToolTip(MAIN);
								enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN,UNDO});
								runButton.setBackground(colorStdActivatedButton);
								actionAborted=false;
								addLog("Evaluation took a wild abort exception (images may have been closed during the run)...", 1);
								return;
							}
							
							//Verify number of landmarks, and return if the number of couples is < 5
							if((regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_2D) && (RoiManager.getInstance().getCount()<10 ) ) {IJ.showMessage("Please identify at least 10 points (5 correspondance couples)");enable(new int[] {RUN,RUNALL});return;}
							
							//Closing manual registration
							disable(new int[] {ABORT,RUN,RUNALL});
							actionAborted=false;
							ItkTransform tr=new ItkTransform();
							regManager.finish2dEvaluation();
							regManager.finishCurrentAction(tr);
							updateView();
							runButton.setText("Start this action");
							setRunToolTip(MAIN);
							runButton.setBackground(colorStdActivatedButton);
							actionAborted=false;
							enable(new int[] {UNDO,BOXACT,FINISH,SAVE,RUN,SETTINGS});
							enableChainIfPossible();							
							addLog("Manual registration finished.", 1);
						}
					}
					
				

					
					//Axis alignment
					else if(regManager.getCurrentAction().typeAction==RegistrationAction.TYPEACTION_ALIGN){						
						disable(new int[] {RUN,RUNALL});
						if(runButton.getText().equals("Start this action")) {
							addLog("Starting alignment of both images...", 1);
							//Parameters verification
							if(boxTypeTrans.getSelectedIndex()>0 && boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_3D) {
								IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform.");
								enable(new int[] {RUN,FINISH,SAVE,UNDO});
								enableChainIfPossible();								
								actionAborted=false;
								addLog("Wrong arguments. Alignment is over...", 1);
								return;
							}
							if(boxTypeTrans.getSelectedIndex()>1 && boxDisplayMan.getSelectedIndex()==RegistrationAction.VIEWER_2D) {
								IJ.showMessage("Warning : transform is set to Vector field. But the 2d viewer can only compute Rigid and Similarity transform.");
								enable(new int[] {RUN,FINISH,SAVE,UNDO});
								enableChainIfPossible();
								actionAborted=false;
								addLog("Wrong arguments. Alignment is over...", 1);
								
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
							addLog("Waiting for position confirmation from user (green button)...", 1);
						}

						//Finish axis alignment
						else {
							//Window verifications and wild abort if needed
							if( ( (regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_3D) && (regManager.universe==null)) || 
									( (regManager.getCurrentAction().typeManViewer==RegistrationAction.VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage(displayedNameImage1)==null) || (WindowManager.getImage(displayedNameImage2)==null) ) ) ) {
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
								addLog("Alignment procedure took a wild abort exception (images may have been closed during the run)...", 1);
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
							addLog("Alignement finished.", 1);
						}
						actionAborted=false;
					}
				}
			});
		}
		System.gc();
	}


	/**
	 * Start batch rsml.
	 *
	 * @param dirIn the dir in
	 * @param dirOutTemp the dir out temp
	 * @param multiThread the multi thread
	 */
	public static void startBatchRsml(String dirIn,String dirOutTemp,boolean multiThread) {
		if(dirIn==null) {
			dirIn=VitiDialogs.chooseDirectoryUI("Select input dir.","Select an input directory with couples files (rsml , image)");
			dirOutTemp=VitiDialogs.chooseDirectoryUI("Select output dir","Select an output directory to begin a new work");
		}
		final String dirOut=new String(dirOutTemp);
		if(new File(dirOut).list().length>0) {
			if(!VitiDialogs.getYesNoUI("Warning", "Warning: there seems to be other files in the output dir. Risk of erasing older files. Process anyway ?"))return;
		}
		final String[][]filesList=rsmlFilesList(dirIn,dirOut);
		for(int i=0;i<filesList[0].length;i++) {
			System.out.println("At start, file["+i+"]="+filesList[0][i]);
		}
		if(filesList==null) {IJ.showMessage("Critical fail : bogus rsml file list, rsml and image files does not match. Next time, please provide a well-formed directory with couples (rsml, image) files, with the same basename");return;}
		int nImgs=filesList[0].length;
		int nMins=1*nImgs;
		boolean display = VitiDialogs.getYesNoUI("Dynamic display", "This procedure will register automatically "+nImgs+" rsml files. A very fancy live display of correction is available. Computation time : 3 mn / rsml with display, 1 mn without. Use the fancy display ?");
		IJ.log("Starting correction of a bunch of "+nImgs+" Rsml files. Multithreading on "+VitimageUtils.getNbCores());
		IJ.log("Expected total computation time = "+nMins+" minutes");
		Timer t=new Timer();
		int N=filesList[0].length;
		FSR sr= (new FSR());
		sr.initialize();

		if(display)multiThread=false;

		if(multiThread) {
			int Ncores=VitimageUtils.getNbCores()/4;
			Thread[]tabThreads=VitimageUtils.newThreadArray(Ncores);
			final int[][]tab=VitimageUtils.listForThreads(N, Ncores);
			AtomicInteger atomNumThread=new AtomicInteger(0);
			
			for (int ithread = 0; ithread < Ncores; ithread++) {  
				tabThreads[ithread] = new Thread() {
					{ setPriority(Thread.NORM_PRIORITY); }  
					public void run() {  		
						int tInd=atomNumThread.getAndIncrement();
						for(int i=0;i<tab[tInd].length;i++) {
							int imgInd=tab[tInd][i];
//							System.out.println("In thread "+tInd+" processing index "+tInd+" = "+);
							IJ.log("\nStarting processing Rsml # "+(imgInd+1)+" / "+N+" at "+t.toCuteString());
							if(new File(filesList[1][imgInd]).exists()) {
								IJ.log("Skipping rsml cause output file already exists : "+filesList[1][imgInd]);
								continue;
							}
							RootModel r=BlockMatchingRegistration.setupAndRunRsmlBlockMatchingRegistration(filesList[0][imgInd], display,true);
							r.writeRSML(filesList[3][imgInd], dirOut);
							ImagePlus img=IJ.openImage(filesList[0][imgInd]);
							IJ.save(img, filesList[1][imgInd]);
							IJ.log("\nFinished processing Rsml # "+(imgInd+1)+" / "+N+" at "+t.toCuteString());
							r.clearDatafile();
							r=null;
							img=null;
							
						}
					}
				};  		
			}				
			VitimageUtils.startNoJoin(tabThreads);
		}
		else {
			for(int i=0;i<N;i++) {
				IJ.log("\nStarting processing Rsml # "+(i+1)+" / "+N+" at "+t.toCuteString());
				if(new File(filesList[1][i]).exists()) {
					IJ.log("Skipping rsml cause output file already exists : "+filesList[1][i]);
					continue;
				}
				RootModel r=BlockMatchingRegistration.setupAndRunRsmlBlockMatchingRegistration(filesList[0][i], display,false);
				r.writeRSML(filesList[3][i], dirOut);
				ImagePlus img=IJ.openImage(filesList[0][i]);
				IJ.save(img, filesList[1][i]);
				IJ.log("\nFinished processing Rsml # "+(i+1)+" / "+N+" at "+t.toCuteString());
				r.clearDatafile();
				r=null;
				img=null;
			}
		}
	}
	
	/**
	 * Rsml files list.
	 *
	 * @param dirIn the dir in
	 * @param dirOut the dir out
	 * @return the string[][]
	 */
	public static String[][] rsmlFilesList(String dirIn,String dirOut){
		boolean debug=true;
		String []initNames=new File(dirIn).list();
		int Ninit=initNames.length;
		int Nrsml=Ninit/2;
		int count=0;
		if((Ninit%2)==1) {IJ.showMessage("Fail in RSML file list : number of input files is not multiple of 2"); return null;}
		for(String file : initNames) {if ( file.substring(file.lastIndexOf('.')).equals(".rsml") )count++;}
		if(count !=Nrsml){IJ.showMessage("Fail in RSML file list : number of rsml files is not half the number of total input files"); return null;}
		String[][]ret=new String[4][Nrsml];
		int incr=0;
		for(String imgName : initNames) {
			if( ! imgName.substring(imgName.lastIndexOf('.')).equals(".rsml") ) {
				String rsmlName=VitimageUtils.withoutExtension(imgName)+".rsml";
				ret[0][incr]=new File(dirIn,imgName).getAbsolutePath();
				ret[1][incr]=new File(dirOut,imgName).getAbsolutePath();
				ret[2][incr]=new File(dirIn,rsmlName).getAbsolutePath();
				ret[3][incr]=new File(dirOut,rsmlName).getAbsolutePath();
				if(! new File(dirIn,rsmlName).exists()){IJ.showMessage("Fail in RSML file list : there is no rsml file corresponding to image "+(new File(dirIn,imgName).getAbsolutePath())); return null;}
				incr++;						
			}
		}
		IJ.log("Ready to process "+Nrsml+" couples of (img, rsml) files");
		if(debug) {
			for(int i=0;i<ret[0].length;i++) {
				System.out.println();
				System.out.println(ret[0][i]);
				System.out.println(ret[1][i]);
				System.out.println(ret[2][i]);
				System.out.println(ret[3][i]);
			}
		}
		return ret;
	}


	
	
	/**
	 * Pass through.
	 *
	 * @param s the s
	 */
	public void passThrough(String s) {
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run() 
			{			
				if(regManager.getCurrentAction().typeAction==0 || regManager.getCurrentAction().typeAction==2) {
					unpassThrough("Next step cannot be handled automatically : axis alignment or manual registration.");
					return;
				}
				disable(new int[] {RUN,RUNALL,ABORT,UNDO});
				actionPerformed(new ActionEvent(runButton, 0, "Auto go on"));
			}
		});
	}

	/**
	 * Enable chain if possible.
	 */
	public void enableChainIfPossible() {
		if (regManager.getCurrentAction().typeAction==1)enable(RUNALL);
		else disable(RUNALL);
	}
	
	/**
	 * Unpass through.
	 *
	 * @param s the s
	 */
	public void unpassThrough(String s) {
		if(!passThroughActivated)return;
		IJ.showMessage("Chain run is finished\n"+s);
		enable(new int[] {RUN,RUNALL});
		passThroughActivated=false;
		if(regManager.getStep()>0)enable(UNDO);
	}
	
	/**
	 * Show hyper image.
	 *
	 * @param hyp the hyp
	 * @param seconds the seconds
	 */
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
	
	
	/**
	 * Update list.
	 */
	/* Updating the Gui elements,menu, list, buttons and views **********************************************************************************************/	
	public void updateList() {
		this.listActions.setModel(regManager.getPipelineAslistModelForGUI());
		this.listActions.setSelectedIndex(regManager.getStep());
		ScrollUtil.scroll(listActions,ScrollUtil.SELECTED,new int[] {listActions.getSelectedIndex(),regManager.getNbSteps()+1});
	}
			
	/**
	 * Update estimated time.
	 */
	public void updateEstimatedTime() {
		int estimatedTime=regManager.estimateTime(regManager.getCurrentAction());
		regManager.estimatedTime=estimatedTime;
		int nbMin=estimatedTime/60;
		int nbSec=estimatedTime%60;
		this.labelTime2.setText(""+nbMin+" mn and "+nbSec+" s");
	}
	
	/**
	 * Action clicked in list.
	 */
	public void actionClickedInList() {
		if(modeWindow!=WINDOWSERIEPROGRAMMING)return;
		if(listActions.getSelectedIndex()>=regManager.getNbSteps() ) return;
		disable(new int[] {BOXACT,BOXDISP,BOXDISPMAN,BOXOPT,BOXTRANS});
		VitimageUtils.waitFor(10);
		regManager.changeCurrentAction(listActions.getSelectedIndex());
		updateList();
		boxTypeAction.setSelectedIndex(regManager.getCurrentAction().typeAction);
		updateBoxFieldsToCoherenceAndApplyToRegistrationAction(false);
		boxTypeTrans.setSelectedIndex(regManager.getCurrentAction().typeTrans==Transform3DType.RIGID ? 0 : regManager.getCurrentAction().typeTrans==Transform3DType.SIMILARITY ? 1 : 2);
		updateBoxFieldsToCoherenceAndApplyToRegistrationAction(false);
		boxDisplay.setSelectedIndex(regManager.getCurrentAction().typeAutoDisplay);
		boxDisplayMan.setSelectedIndex(regManager.getCurrentAction().typeManViewer);
		if(regManager.getCurrentAction().typeOpt==OptimizerType.BLOCKMATCHING)this.boxOptimizer.setSelectedIndex(0);
		else if(regManager.getCurrentAction().typeOpt==OptimizerType.MASSCENTER)this.boxOptimizer.setSelectedIndex(2);
		else if(regManager.getCurrentAction().typeOpt==OptimizerType.INERTIA_AXIS)this.boxOptimizer.setSelectedIndex(3);
		else this.boxOptimizer.setSelectedIndex(1);
		updateBoxFieldsToCoherenceAndApplyToRegistrationAction(true);
	}

	/**
	 * Box cliked in gui.
	 */
	public void boxClikedInGui() {
		disable(new int[] {BOXOPT,BOXTIME,BOXDISP,BOXTRANS,BOXACT});
		updateBoxFieldsToCoherenceAndApplyToRegistrationAction(true);
		updateList();
		VitimageUtils.waitFor(10);
		enable(new int[] {BOXACT,BOXTRANS});
		setState(new int[] {BOXOPT,BOXDISP },boxTypeAction.getSelectedIndex()==1);
		setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
		if(modeWindow==WINDOWSERIERUNNING)disable(new int[] {BOXACT,BOXTRANS,BOXOPT,BOXDISP,BOXDISPMAN});
		
	}

	/**
	 * Update box fields to coherence and apply to registration action.
	 *
	 * @param applyToAction the apply to action
	 */
	public void updateBoxFieldsToCoherenceAndApplyToRegistrationAction(boolean applyToAction) {
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
		else if(boxTypeAction.getSelectedIndex()==1 && boxOptimizer.getSelectedIndex()>1) {
			for(int i=0;i<textDisplayMass.length;i++)listModelDisp.addElement(textDisplayMass[i]);
	        for(int i=0;i<textTransformsMass.length;i++)listModelTrans.addElement(textTransformsMass[i]);
			
	        this.boxDisplay.setModel(listModelDisp);
			this.boxDisplay.setSelectedIndex(Math.min(valDisp,textDisplayMass.length-1));
			this.boxTypeTrans.setModel(listModelTrans);
			this.boxTypeTrans.setSelectedIndex(Math.min(valTrans,textTransformsMass.length-1));
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
		if(applyToAction)regManager.getCurrentAction().updateFieldsFromBoxes(boxTypeAction.getSelectedIndex(),boxTypeTrans.getSelectedIndex(),boxOptimizer.getSelectedIndex(),boxDisplay.getSelectedIndex(),boxDisplayMan.getSelectedIndex(),modeWindow);
		enable(BOXACT);
		enable(BOXTRANS);
		if(boxTypeAction.getSelectedIndex()==0)enable(new int[] {BOXDISPMAN});
		if(boxTypeAction.getSelectedIndex()==1)enable(new int[] {BOXOPT,BOXDISP});
	}
 
	/**
	 * Update box fields from registration action.
	 *
	 * @param reg the reg
	 */
	public void updateBoxFieldsFromRegistrationAction(RegistrationAction reg) {
		if(modeWindow==WINDOWTWOIMG && (!reg.isTransformationAction())) {reg.typeAction=RegistrationAction.TYPEACTION_MAN;reg.typeTrans=Transform3DType.RIGID ;}
		boxTypeAction.setSelectedIndex(((reg.typeAction < 3)||(reg.typeAction==RegistrationAction.TYPEACTION_EVALUATE)) ? reg.typeAction : 0);
		if(modeWindow!=WINDOWSERIERUNNING)updateBoxFieldsToCoherenceAndApplyToRegistrationAction(true);
		boxTypeTrans.setSelectedIndex(reg.typeTrans==Transform3DType.DENSE ? 2 : reg.typeTrans==Transform3DType.RIGID ? 0 : 1);
		if(modeWindow!=WINDOWSERIERUNNING)updateBoxFieldsToCoherenceAndApplyToRegistrationAction(true);
		boxDisplay.setSelectedIndex(reg.typeAutoDisplay);
		boxDisplayMan.setSelectedIndex(reg.typeManViewer);
		if(regManager.getCurrentAction().typeOpt==OptimizerType.BLOCKMATCHING)this.boxOptimizer.setSelectedIndex(0);
		else if(regManager.getCurrentAction().typeOpt==OptimizerType.MASSCENTER)this.boxOptimizer.setSelectedIndex(2);
		else if(regManager.getCurrentAction().typeOpt==OptimizerType.INERTIA_AXIS)this.boxOptimizer.setSelectedIndex(3);
		else this.boxOptimizer.setSelectedIndex(1);
		updateList();
		setState(new int[] {BOXOPT,BOXTIME,BOXDISP },boxTypeAction.getSelectedIndex()==1);
		setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
		if(modeWindow==WINDOWSERIERUNNING)disable(new int[] {BOXACT,BOXTRANS,BOXOPT,BOXDISP,BOXDISPMAN});
	}

	/**
	 * Gets the relative optimal position for 2 D view.
	 *
	 * @return the relative optimal position for 2 D view
	 */
	public int getRelativeOptimalPositionFor2DView() {
		java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        if(screenX>1920)screenX/=2;        
		if(registrationFrame.getLocationOnScreen().x+registrationFrame.getSize().getWidth()/2 > screenX/2) return 0;
		else return 2;
	}

	/**
	 * Update view.
	 */
	public void updateView() {
		if(this.modeWindow!=WINDOWTWOIMG)return;
		ImagePlus temp=null;
		boolean firstView=false;
		if(this.imgView==null || (!this.imgView.isVisible()))firstView=true;
		if(firstView){
			this.imgView=regManager.getViewOfImagesTransformedAndSuperposedTwoImg();
			imgView.show();
			VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),registrationFrame,getRelativeOptimalPositionFor2DView(),0,10);
		}
		else {
			temp=this.imgView;
			this.imgView=regManager.getViewOfImagesTransformedAndSuperposedTwoImg();
			imgView.show();
			VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),temp.getWindow(),1,1,10);
		}
		java.awt.Rectangle w = imgView.getWindow().getBounds();
		int max=0;
		
		//If little image, enlarge it until its size is between half screen and full screen
		while(imgView.getWindow().getWidth()<(screenWidth/2) && imgView.getWindow().getHeight()<(screenHeight/2) && (max++)<4) {
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
		if(firstView) {
			VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),registrationFrame,0,0,10);
		}
		else {
			VitimageUtils.adjustFrameOnScreenRelative(imgView.getWindow(),temp.getWindow(),1,1,10);
		}
		imgView.setSlice(viewSlice);
		imgView.updateAndRepaintWindow();
	}
	
	/**
	 * Gets the img view text.
	 *
	 * @param st the st
	 * @return the img view text
	 */
	public static String getImgViewText(int st){
		return ( (st==0) ? "Superimposition before registration" :  ("Registration results after "+(st)+" step"+((st>1)? "s" : "")) );
	}
	
	/**
	 * Close all views.
	 */
	public void closeAllViews() {
		for(int st=0;st<=regManager.getStep();st++) {
			if (WindowManager.getImage(getImgViewText(st))!=null)WindowManager.getImage(getImgViewText(st)).close();
		}
		if (WindowManager.getImage(displayedNameImage1)!=null)WindowManager.getImage(displayedNameImage2).close();
		if (WindowManager.getImage(displayedNameImage2)!=null)WindowManager.getImage(displayedNameImage2).close();
		if (RoiManager.getInstance()!=null)RoiManager.getInstance().close();
		if (regManager.universe!=null) regManager.universe.close();
		if(WindowManager.getImage(displayedNameCombinedImage)!=null)WindowManager.getImage(displayedNameCombinedImage).close();
	}	
	
	/**
	 * Undo button pressed.
	 */
	public void undoButtonPressed() {
		//Change the current view, and call update for RegistrationManager
		regManager.undoLastActionInTwoImagesContext();
		updateList();
		
	}
		

	
	
	
	
	
	
		
	
	/* Minor helpers and getters/setters************************************************************************************************/
	
	/**
	 * Adds the log.
	 *
	 * @param t the t
	 * @param level the level
	 */
	public void addLog(String t,int level) {
		logArea.append((level==0 ? "\n > ": (level==1) ? "\n " : " ")+t);
		logArea.setCaretPosition(logArea.getDocument().getLength());
	}

	/**
	 * Display sos message.
	 *
	 * @param context the context
	 */
	public void displaySosMessage(int context){
		if(context==SOS_CONTEXT_LAUNCH) {
			IJ.showMessage(
			"Start menu\n"+
			"* First trial ? Choose register two images and test Fijiyama with demo images or with your own data.\n"+
			"* Ready to process a N-time M-modalities series ? Choose Register 3d series\n"+
			"* Go on a previous experiment (two images or series) ? Choose Open a previous study\n"+
			"More information ? Visit the webpage : www.imagej.net/Fijiyama\n"
			);
		}
	}

	/**
	 * Run sos.
	 */
	public void runSos(){
		if(( regManager.getCurrentAction().typeAction==0) || (regManager.getCurrentAction().typeAction==2) ||
		(regManager.getCurrentAction().typeAction==1 && actionAborted==true)) enable(RUN);
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
				"You're actually running a series registration. We assume that you trained on the \"Two images registration\" module,"+
				" to understand the main concepts of the plugin, testing the provided functions on your data, and eventually to fine-tune the settings of algorithms."+
				nextPar+"The series registration process runs the following steps :"+startPar+
				" <b>1)   Defining data directories :</b> input directory for image lookup, and output directory to save plugin state and exported images and transformations"+nextPar+
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
				" and set its alignment relative to the XYZ axis, drawn as white orthogonal lines "+nextPar+
				"- Rotations : Mouse-drag the background to turn the scene, and mouse-drag an object to turn it."+
				" For accurate rotations, use the arrows (numpad 7 & numpad 9  for X axis, numpad 1 & numpad 3 for Y axis, character 'p' & character 'o' for Z axis)"+nextPar+
				(VitimageUtils.isWindowsOS() ? "- Translations :  use the numerical keypad (4 & 6 for X axis, 2 & 8 for Y axis, 0 & 5 for Z axis)" :	
					"- Translations : hold the SHIFT key and drag an object to translate it."+
					"For accurate translations, use the numerical keypad (4 & 6 for X axis, 2 & 8 for Y axis, 0 & 5 for Z axis)")	+	
				"- To zoom / unzoom, scroll with the mouse or use pageup and pagedown"+

				nextPar+" "+
				" -> Once done, click on the <b>\"Position ok\"</b> button to validate the alignment."+" "+"The transform computed is then applied to the reference and moving images"+saut;
		String bmText="Automatic registration."+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				nextPar+
				"Automatic registration can be done using two different optimizers : "+
				" Block matching compares subparts of images to identify correspondances, and use these correpondances to align the moving image on the reference image ; "+
				" Itk Iconic optimizes a transform, and quantify the evolution of image matching using a global statistical measure. Both use a pyramidal scheme, computing correspondances and transformations from a rough level (max image subsampling factor=2, 4, 8...) to an accurate level (min image subsampling factor=1, 2, 4, ...)."+
				nextPar+
				"Automatic registration is an optimization process, its duration depends on the parameters. The estimated computation is given to help you "+
				"to define parameters according with the computation time you expect. The process starts when clicking on the <b>\"Start this action\"</b> button and finish when all the iterations are done at all levels, or when you click on the <b>\"Abort\"</b> button ."+saut;
				
		String manualText="Manual registration step."+""+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				saut+
				"This operation lets you roughly align the images manually, in order to handle transformations of great amplitude (angle > 15 degrees) that cannot be estimated automatically."+
				"To start this action, click on the <b>\"Start this action\"</b> button. A 3d interface opens, and let you interact with the images."+saut+
				"In the 3d interface, the reference image is shown as a red volume and the moving image as a green volume. Interact with them to make them match."+nextPar+
				"- Rotations : Mouse-drag the background to turn the scene, and mouse-drag an object to turn it."+
				" For accurate rotations, use the arrows (numpad 7 & numpad 9  for X axis, numpad 1 & numpad 3 for Y axis, character 'p' & character 'o' for Z axis)"+nextPar+
				(VitimageUtils.isWindowsOS() ? "- Translations :  use the numerical keypad (4 & 6 for X axis, 2 & 8 for Y axis, 0 & 5 for Z axis)" :	
					"- Translations : hold the SHIFT key and drag an object to translate it."+
					"For accurate translations, use the numerical keypad (4 & 6 for X axis, 2 & 8 for Y axis, 0 & 5 for Z axis)")	+nextPar+
				"- To zoom / unzoom, scroll with the mouse or use pageup and pagedown"+
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
		textToDisplay+="<b>Citing this work :</b> R. Fernandez and C. Moisy (2020) <i>Fijiyama : a registration tool for 3D multimodal time-lapse imaging</i> Bioinformatics</i> (under review)"+saut+
				"<b>Credits :</b> this work was supported by the \"Plan deperissement du vignoble\"   </p>";
		IJ.showMessage("Fijiyama contextual help", textToDisplay);
		enable(SOS);
	}
	
	/**
	 * Open settings dialog.
	 */
	public void openSettingsDialog() {
		//Parameters for manual and axis aligment
		if(this.boxTypeAction.getSelectedIndex()!=1) {
	        GenericDialog gd= new GenericDialog("Settings for manual registration");
	        gd.addMessage("No advanced settings for manual registration.\n.\n"+
	        "The first option (3d viewer) assumes objects of interest are bright structures with an irregular surface,\n"+
	        		"surrounded with a dark background. Under these assumptions, manual registration can be done in the 3d viewer.\n.\n"+
	        		"If your images are not of this kind, manual registration in the 3d viewer will be difficult, and you should select\nthe 2d viewer, to identify landmarks using the classic 2d slicer");
	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        return;
		}

		
		//Parameters for automatic registration
		String message="Successive subsampling factors used, from max to min."+"These parameters have the most dramatic effect\non computation time and results accuracy :\n"+
				"- The max level is the first level being processed, making comparisons between subsampled versions of images."+"\n"+
				"  After subsampling images, the algorithm only sees the global structures, but allows transformations of greater amplitude\n"+
				"  High subsampling levels run faster, and low subsampling levels run slower. But if the max subsampling level is too high, the subsampled image\n  is not informative anymore, and registration could diverge"+"\n.\n"+
				"- The min level is the last subsampling level and the more accurate to be processed\n  Subsampling=1 means using all image informations (no subsampling) \n";

			//Parameters for BlockMatching
		if(this.boxOptimizer.getSelectedIndex()==0) {
			GenericDialog gd= new GenericDialog("Expert mode for Blockmatching registration");
			if(regManager.getCurrentAction().getLevelMax()>regManager.maxAcceptableLevel)regManager.maxAcceptableLevel=regManager.getCurrentAction().getLevelMax();
			String[]levelsMax=new String[regManager.getMaxAcceptableLevelForTheCurrentAction()];
	        for(int i=0;i<regManager.getMaxAcceptableLevelForTheCurrentAction();i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[regManager.getCurrentAction().getLevelMax()-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[regManager.getCurrentAction().getLevelMin()-1]);
	        if(enableHighAcc)gd.addChoice("Higher accuracy (subpixellic level)", new String[] {"Yes","No"},regManager.getCurrentAction().higherAcc==1 ? "Yes":"No");

	        gd.addMessage("Blocks dimensions (image subparts to compare in ref and mov)");
	        gd.addNumericField("Block half-size along X", regManager.getCurrentAction().bhsX, 0, 3, "subsampled pixels");
	        gd.addNumericField("... along Y", regManager.getCurrentAction().bhsY, 0, 3, "subsampled pixels");
	        gd.addNumericField("... along Z", regManager.getCurrentAction().bhsZ, 0, 3, "subsampled pixels");

	        gd.addMessage("Maximal distance between matching points (at each iteration)");
	        gd.addNumericField("Block neighbourhood along X", regManager.getCurrentAction().neighX, 0, 3, "subsampled pixels");
	        gd.addNumericField("... along Y", regManager.getCurrentAction().neighY, 0, 3, "subsampled pixels");
	        gd.addNumericField("... along Z",  regManager.getCurrentAction().neighZ, 0, 3, "subsampled pixels");

	        gd.addMessage("Spacing between successive blocks along each dimension");
	        gd.addNumericField("Striding along X", regManager.getCurrentAction().strideX, 0, 3, "subsampled pixels");
	        gd.addNumericField("... along Y",  regManager.getCurrentAction().strideY, 0, 3, "subsampled pixels");
	        gd.addNumericField("... along Z",  regManager.getCurrentAction().strideZ, 0, 3, "subsampled pixels");

	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  regManager.getCurrentAction().getIterationsBM(), 0, 3, "iterations");
	        if(this.boxTypeTrans.getSelectedIndex()==2)gd.addNumericField("Sigma for dense field smoothing", regManager.getCurrentAction().sigmaDense, 3, 12, regManager.getUnit());
	        gd.addNumericField("Percentage of blocks selected by score", regManager.getCurrentAction().selectScore, 0, 3, "%");
	        if(this.boxTypeTrans.getSelectedIndex()!=2)gd.addNumericField("Percentage kept in Least-trimmed square", regManager.getCurrentAction().selectLTS, 0, 3, "%");	        
	        if(this.isSurvivorVncTunnelLittleDisplay)gd.setFont(mySurvivalFontForLittleDisplays);
	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        int a=gd.getNextChoiceIndex()+1; regManager.getCurrentAction().setLevelMax(a);
	        int b=gd.getNextChoiceIndex()+1; b=b<a ? b : a; regManager.getCurrentAction().setLevelMin(b);
	        if(enableHighAcc) {
	        	a=1-gd.getNextChoiceIndex();
	        	regManager.getCurrentAction().higherAcc=a;
	        }
	       	int c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().bhsX=Math.min(7,Math.max(c,3));
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().bhsY=Math.min(7,Math.max(c,3));
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().bhsZ=Math.min(7,Math.max(c,0));

	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().neighX=Math.min(7,Math.max(c,1));
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().neighY=Math.min(7,Math.max(c,1));
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; regManager.getCurrentAction().neighZ=Math.min(7,Math.max(c,0));

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().strideX=Math.min(100,Math.max(c,1));
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().strideY=Math.min(100,Math.max(c,1));
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().strideZ=Math.min(100,Math.max(c,1));

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().setIterationsBM(Math.min(100,Math.max(c,1)));
	       	if(this.boxTypeTrans.getSelectedIndex()==2) {double d=gd.getNextNumber(); d=d<1E-6 ? 1E-6 : d; regManager.getCurrentAction().sigmaDense=d;}
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().selectScore=Math.min(100,Math.max(c,5));
	       	if(this.boxTypeTrans.getSelectedIndex()!=2) {c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; regManager.getCurrentAction().selectLTS=Math.min(100,Math.max(c,5));}
		}
		//Parameters for Itk Iconic
		else if(this.boxOptimizer.getSelectedIndex()==0){//Itk parameters
	        GenericDialog gd= new GenericDialog("Expert mode for Itk registration");
	        String[]levelsMax=new String[regManager.getMaxAcceptableLevelForTheCurrentAction()];for(int i=0;i<regManager.getMaxAcceptableLevelForTheCurrentAction();i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
			gd.addMessage(message);
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[regManager.getCurrentAction().levelMaxLinear-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[regManager.getCurrentAction().levelMinLinear-1]);
	        
	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  regManager.getCurrentAction().iterationsITK, 0, 5, "iterations");
	        gd.addNumericField("Learning rate",  regManager.getCurrentAction().learningRate, 4, 8, " no unit");
	        
	        if(this.isSurvivorVncTunnelLittleDisplay)gd.setFont(mySurvivalFontForLittleDisplays);
	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        int param1=gd.getNextChoiceIndex()+1; 
	        regManager.getCurrentAction().levelMaxLinear=param1;

	        int param2=gd.getNextChoiceIndex()+1; param2=param2<param1 ? param2 : param1;
	        regManager.getCurrentAction().levelMinLinear=param2;
	       	
	       	int param3=(int)Math.round(gd.getNextNumber());
	       	param3=param3<0 ? 0 : param3;  regManager.getCurrentAction().iterationsITK=param3;
	       	double param4=gd.getNextNumber();
	       	param4=param4<0 ? EPSILON : param4;  regManager.getCurrentAction().learningRate=param4;
		}	
		else {
			int a=1; //Nothing
		}
	}

	/**
	 * Serie is finished.
	 */
	public void serieIsFinished() {
		IJ.showMessage("Series is finished !");
		disable(new int[] {RUN,RUNALL});
	}
	
	/**
	 * Welcome and inform about computer capabilities.
	 */
	public void welcomeAndInformAboutComputerCapabilities() {		
		String[]str=regManager.checkComputerCapacity(true);
		addLog(str[0],0);
		addLog(str[1],0);		
	}

	/**
	 * Log action event.
	 *
	 * @param e the e
	 */
	public void logActionEvent(ActionEvent e) {
		if(regManager.regActions==null || regManager.getCurrentAction()==null)return;
	}

	
	
	/**
	 * Enable.
	 *
	 * @param but the but
	 */
	public void enable(int but) {
		setState(new int[] {but},true);
	}
	
	/**
	 * Disable.
	 *
	 * @param but the but
	 */
	public void disable(int but) {
		setState(new int[] {but},false);
	}

	/**
	 * Enable.
	 *
	 * @param tabBut the tab but
	 */
	public void enable(int[]tabBut) {
		setState(tabBut,true);
	}
	
	/**
	 * Disable.
	 *
	 * @param tabBut the tab but
	 */
	public void disable(int[]tabBut) {
		setState(tabBut,false);
	}
			
	/**
	 * Sets the state.
	 *
	 * @param tabBut the tab but
	 * @param state the state
	 */
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
			case BATCHRSML:this.batchRsmlButton.setEnabled(state);break;
			case RUNSERIE:this.runSerieButton.setEnabled(state);break;
			case RUNNEXTSTEP:this.runNextStepButton.setEnabled(state);break;
			case GOBACKSTEP:this.goBackStepButton.setEnabled(state);break;
			case RUNTRANS:this.transformButton.setEnabled(state);break;
			case RUNTRANSCOMP:this.composeTransformsButton.setEnabled(state);break;
			case RUNALL:this.runThroughButton.setEnabled(state);break;
			}	
		}	
	}
	
	/**
	 * Sets the run tool tip.
	 *
	 * @param context the new run tool tip
	 */
	public void setRunToolTip(int context){
		if(context==MAIN) {
			runButton.setToolTipText("<html><p width=\"500\">" +
			"The action can be interrupted using the Abort button."+"</p></html>");
		}
		if(context==MANUAL2D) {
			runButton.setToolTipText("<html><p width=\"500\">" +"Click here to validate the landmarks, compute the corresponding transform, and get the transform applied to images"+"</p></html>");
		}
		if(context==MANUAL3D) {
			runButton.setToolTipText("<html><p width=\"500\">" +"Click here to validate the actual relative position of objects, and get the transform applied to images"+"</p></html>");
		}
	}	
	
	/**
	 * Gets the fijiyama GUI.
	 *
	 * @return the fijiyama GUI
	 */
	public Fijiyama_GUI getFijiyamaGUI() {
		return this;
	}

	/**
	 * Gets the debug mode.
	 *
	 * @return the debug mode
	 */
	public boolean getDebugMode() {
		return debugMode;
	}

	/**
	 * Gets the auto rep mode.
	 *
	 * @return the auto rep mode
	 */
	public boolean getAutoRepMode() {
		return autoRep;
	}

	/**
	 * Gets the run button text.
	 *
	 * @return the run button text
	 */
	public String getRunButtonText() {
		return runButton.getText();				
	}
	
	/**
	 * Checks if is programming serie.
	 *
	 * @return true, if is programming serie
	 */
	public boolean isProgrammingSerie() {
		return modeWindow==WINDOWSERIEPROGRAMMING;
	}

	/**
	 * Checks if is running serie.
	 *
	 * @return true, if is running serie
	 */
	public boolean isRunningSerie() {
		return modeWindow==WINDOWSERIERUNNING;
	}
	
	/**
	 * Current context is serie.
	 *
	 * @return true, if successful
	 */
	public boolean currentContextIsSerie() {
		return (this.mode==MODE_SERIE);
	}

	/**
	 * Checks if is running two images training.
	 *
	 * @return true, if is running two images training
	 */
	public boolean isRunningTwoImagesTraining() {
		return modeWindow==WINDOWTWOIMG;
	}


	/**
	 * Sending out an sos.
	 */
	public void sendingOutAnSos() {
		System.out.println("Sending out an sos");
		enable(new int[] {RUN,SETTINGS,BOXACT,BOXTRANS,SAVE,FINISH,UNDO});
	}

	/**
	 * More about reg action.
	 */
	public void moreAboutRegAction() {
		System.out.println("More about reg actions");
		regManager.printRegActions("", regManager.regActions);
	}
	
	/**
	 * Handle key released.
	 *
	 * @param e the e
	 */
	public void handleKeyReleased(KeyEvent e) {
		System.out.println("Released ! : "+e);
		if(e.getKeyChar()=='t' )pairLevel++;
		if((pairLevel>=2 && ((pairLevel%2)==0))) {Toolkit.getDefaultToolkit().beep();IJ.log("Released Nb points : "+pairLevel);}
	}
	
	/**
	 * Handle key press.
	 *
	 * @param e the e
	 */
	public void handleKeyPress(KeyEvent e) {
		//		System.out.println("Got a key pressed : "+e.getKeyCode()+" = "+e.getKeyChar());
		if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_T) {
			pairLevel++;
			if((pairLevel>=2 && ((pairLevel%2)==0))) {Toolkit.getDefaultToolkit().beep();IJ.log("Pressed Nb points : "+pairLevel);}
		}
		if(sosLevel==2 && e.getKeyChar()=='s') {sosLevel=0;sendingOutAnSos();return;}
		else if(sosLevel==1 && e.getKeyChar()=='o')sosLevel=2;
		else if(sosLevel==0 && e.getKeyChar()=='s')sosLevel=1;
		else sosLevel=0;

		if(regInterrogationLevel==2 && e.getKeyChar()=='g') {regInterrogationLevel=0;moreAboutRegAction();return;}
		else if(regInterrogationLevel==1 && e.getKeyChar()=='e')regInterrogationLevel=2;
		else if(regInterrogationLevel==0 && e.getKeyChar()=='r')regInterrogationLevel=1;
		else regInterrogationLevel=0;

		if(yogiteaLevel==6 && e.getKeyChar()=='a') {yogiteaLevel=0;VitimageUtils.getRelaxingPopup("",true);return;}
		else if(yogiteaLevel==5 && e.getKeyChar()=='e')yogiteaLevel=6;
		else if(yogiteaLevel==4 && e.getKeyChar()=='t')yogiteaLevel=5;
		else if(yogiteaLevel==3 && e.getKeyChar()=='i')yogiteaLevel=4;
		else if(yogiteaLevel==2 && e.getKeyChar()=='g')yogiteaLevel=3;
		else if(yogiteaLevel==1 && e.getKeyChar()=='o')yogiteaLevel=2;
		else if(yogiteaLevel==0 && e.getKeyChar()=='y')yogiteaLevel=1;
		else yogiteaLevel=0;
		if(regManager==null) return;
		if(regManager.universe==null)return;
		if(regManager.universe.getSelected()==null)return;
		double[]vectTrans=TransformUtils.multiplyVector(VitimageUtils.getDimensionsRealSpace(regManager.getCurrentRefImage()),0.01);
		double angle=0.5*Math.PI/180.0;//~1 degree
		//Translations
		if(e.getKeyChar()=='4') {regManager.universe.getSelected().applyTranslation((float)(-vectTrans[0]), 0, 0);return;}
		if(e.getKeyChar()=='6') {regManager.universe.getSelected().applyTranslation((float)vectTrans[0], 0, 0);return;}
		if(e.getKeyChar()=='8') {regManager.universe.getSelected().applyTranslation(0, (float)(-vectTrans[1]), 0);return;}
		if(e.getKeyChar()=='2') {regManager.universe.getSelected().applyTranslation(0, (float)vectTrans[1], 0);return;}
		if(e.getKeyChar()=='0') {regManager.universe.getSelected().applyTranslation(0, 0, (float)vectTrans[0]);return;}
		if(e.getKeyChar()=='5') {regManager.universe.getSelected().applyTranslation(0, 0, (float)(-vectTrans[0]));return;}

		//Rotations
		Transform3D tr=null;
		double[]angles=new double[3];		
		if(e.getKeyCode()==79)angles[1]=angle;
		if(e.getKeyCode()==80)angles[1]=-angle;
		if(e.getKeyChar()=='7')angles[0]=angle;
		if(e.getKeyChar()=='9')angles[0]=-angle;
		if(e.getKeyChar()=='3')angles[2]=angle;
		if(e.getKeyChar()=='1')angles[2]=-angle;		
		double[]imageCenter=VitimageUtils.getImageCenter(regManager.getCurrentRefImage(),true);		
		ItkTransform itkTr=ItkTransform.getRigidTransform(imageCenter, angles, new double[] {0,0,0});
		tr=ItkTransform.itkTransformToIj3dTransform(itkTr); 
		regManager.universe.getSelected().applyTransform(tr);
	}
}


