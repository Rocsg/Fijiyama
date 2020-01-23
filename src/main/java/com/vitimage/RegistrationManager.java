package com.vitimage;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
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
import javax.swing.JSeparator;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.plugin.Memory;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import math3d.Point3d;

/**
 * 
 * @author fernandr
 *This class give access to iconic and blockmatching registration
 */

public class RegistrationManager extends PlugInFrame implements ActionListener,KeyListener, MouseListener {
	//Flags for the kind of viewer
	private static final int VIEWER_3D = 71;
	private static final int VIEWER_2D = 72;

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

	//Identifiers for buttons
	private static final int SETTINGS=101;
	private static final int RUN=102;
	private static final int UNDO=103;
	private static final int ABORT=104;
	private static final int SAVE=105;
	private static final int FINISH=106;
	private static final int SOS=107;

	//Attributes linked to the registrations actions;
	ItkRegistrationManager itkManager;
	BlockMatchingRegistration bmRegistration;
	private final double EPSILON=1E-8;
	boolean actionAborted=false;
	boolean flagAxis=false;
	boolean flagAxisWasLast=false;
	boolean flagDense=false;
	boolean flagDenseWasLast=false;
	boolean developerMode=false;
	//ImagePlus imgMovCurrentState;
	//ImagePlus imgRefCurrentState;
	private int expectedExecutionTime=60;
	private int []listExpectedExecutionTimes=new int[] {60,300,900,1800,(int) 1E8};
	private ij3d.Image3DUniverse universe;
	int estimatedTime=0;
	String spaces="-                                                                                                                                   ";
	private String []textPreviousActions=new String[] {spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces,spaces};
	private Object[][] registrationParametersValues=new Object[][] {
		{"Type transform",Transform3DType.RIGID},		{"Sigma resampling",0},		{"Sigma dense",1.0},  {"Level min",2},		{"Level max",2},   {"Higher accuracy",0},
		{"Iterations BM",6},		{"Iterations ITK",100},
		{"BM Neigh X",3},		{"BM Neigh Y",3},		{"BM Neigh Z",0},
		{"BM BHS X",7},		{"BM BHS Y",7},		{"BM BHS Z",1},
		{"BM Stride X",3},		{"BM Stride Y",3},		{"BM Stride Z",3}, {"BM Select score",50},{"BM Select LTS",50},{"BM Select Rand",10},{"BM Subsample Z",0},
		{"ITK optimizer",OptimizerType.ITK_AMOEBA},		{"Type optimizer",OptimizerType.BLOCKMATCHING}, {"ITK Learning rate",0.3}
	};
	private int step=0;
	private int viewRegistrationLevel=1;
	private int viewSlice;
	private int nTimes=0;
	private int nMods=0;
	private ImagePlus[][]images;
	private String[][]paths;
	private int[][][]initDimensions;
	private int[][][]dimensions;
	private double[][][]imageRanges;
	private int[][]imageSizes;//In Mvoxels
	private double[][][]initVoxs;
	private double[][][]voxs;
	private ItkTransform[][] previousTransforms;
	private ItkTransform[][] transforms;
	private String unit="mm";
	private int[]imageParameters=new int[] {512,512,40};
	private int maxAcceptableLevel=3;
	private int nbCpu=1;
	private int freeMemory=1;
	
	private int maxImageSizeForRegistration=32;//In Mvoxels
	private int mode;
	private int MODE_TWO_IMAGES=1;

	
		//Start interface attributes
	private static final long serialVersionUID = 1L;
	private JButton sos0Button = null;
	private JButton runTwoImagesButton = null;
	private JButton runMultiModalButton = null;
	private JButton runMultiTimeButton = null;
	private JButton runMultiModalMultiTimeButton = null;
	private JPanel buttonPanel=null;
	private int SOS_CONTEXT_LAUNCH=0;
	private ImagePlus imgView;
	
	
	//Registration interface attributes
	private String[]textActions=new String[] {"Manual registration","Automatic registration","Align results with image axis"};
	private String[]textOptimizers=new String[] {"Block-Matching","ITK"};
	private String[]textTransformsBM=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)","Vector field "};
	private String[]textTransformsITK=new String[] {"Rigid (no deformations)","Similarity (isotropic deform.)"};
	private String[]textTiming=new String[] {"Read a mail (1 mn)","Take a coffee (5 mn)","Go for a walk (15mn)","Read a paper (30mn)","No limitation (inf mn)"};
	private String[]textDisplayITK=new String[] {"0-Only at the end (faster)","1-Dynamic display (slower)"};
	private String[]textDisplayBM=new String[] {"0-Only at the end (faster)","1-Dynamic display (slower)","2-Also display score map (slower+)"};
	private String[]textDisplayMan=new String[] {"2d viewer (classic slicer)","3d viewer (volume rendering)"};
	private Transform3DType[] transform3DList=new Transform3DType[] {Transform3DType.RIGID,Transform3DType.SIMILARITY,Transform3DType.DENSE};
	
	private JLabel labelList = new JLabel("Previous actions", JLabel.CENTER);
	private JList<String>listActions=new JList<String>(textPreviousActions);
	private JLabel labelNextAction = new JLabel("Choose the next action :", JLabel.LEFT);
    private JComboBox<String>boxTypeAction=new JComboBox<String>(textActions);
	private JLabel labelOptimizer = new JLabel("Optimizer used :", JLabel.LEFT);
	private JComboBox<String>boxOptimizer=new JComboBox<String>(  textOptimizers   );
	private JLabel labelTransformation = new JLabel("Transformation to estimate :", JLabel.LEFT);
	private JComboBox<String>boxTypeTrans=new JComboBox<String>( textTransformsBM  );
	private JLabel labelTiming = new JLabel("Max computation time :", JLabel.LEFT);
	private JComboBox<String>boxTiming=new JComboBox<String>( textTiming );
	private JLabel labelView = new JLabel("Display automatic registration :", JLabel.LEFT);
	private JComboBox<String>boxDisplay=new JComboBox<String>( textDisplayBM );
	private JLabel labelViewMan = new JLabel("Viewer for manual registration :", JLabel.LEFT);
	private JComboBox<String>boxDisplayMan=new JComboBox<String>( textDisplayMan );
	private JLabel labelTime1 = new JLabel("Estimated time for this action :", JLabel.LEFT);
	private JLabel labelTime2 = new JLabel("0 mn and 0s", JLabel.CENTER);
	private JButton settingsButton=new JButton("Advanced settings...");
	private JButton settingsDefaultButton=new JButton("Restore default settings...");
	private JButton runButton=new JButton("Start this action");
	private JButton abortButton = new JButton("Abort");
	private JButton undoButton=new JButton("Undo");
	private JButton saveButton=new JButton("Save current state");
	private JButton finishButton=new JButton("Export results");
	private JButton sos1Button=new JButton("Contextual help");
	private JFrame registrationFrame;
	private Color colorIdle;
	private int kindOfViewer=VIEWER_3D;

	
	/**Starting entry points 
	 * 
	 */
	public RegistrationManager() {
		super("FijiYama : a versatile registration tool for Fiji");
	}

	public static void main(String[]args) {//TestMethod
		ImageJ ij=new ImageJ();
		ij.getAlignmentX();
		RegistrationManager reg=new RegistrationManager();
		reg.developerMode=true;
		reg.run("");
	}
	
	
	//TODO : registration image in big for bm, with aside other ones 

	
	public void run(String arg) {
		if(developerMode) {
			this.startFromSavedStateTwoImages("/home/fernandr/Bureau/TESTSAVE/TESTMAN/save_DATA_FJM/save.fjm");
			/*this.setupTwoImagesRegistration(
					"/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/imgRef_Ttest_16b.tif",
					"/mnt/DD_COMMON/Data_VITIMAGE/Old_test/BM_subvoxel/imgRef_Trans8_16b.tif");
			this.mode=MODE_TWO_IMAGES;
			*/
			
		}
		else startLaunchingInterface();
	}


	/** Setup methods
	 * 
	 */
	public void setupNimagesRegistration(int nTimes,int nMods) {
		setupStructures();
		checkComputerCapacity();
		openImagesAndCheckOversizing();
		startRegistrationInterface();
		updateViewTwoImages();
		enable(new int[] {RUN,SETTINGS,SOS,BOXACT,BOXTRANS,BOXDISPMAN});		
	}

	public static void testBuild() {		
		double sqrt3=Math.pow(2, 1/3.0);
		for(int nt=0;nt<5;nt++)		for(int nm=0;nm<5;nm++) {
			double factor=1;
			int val=5*nt+nm;
			if(val<12)factor=Math.pow(sqrt3, val);
			System.out.println("Creating with factor="+factor +"  giving volume factor="+Math.pow(factor,3));
			ImagePlus imgOut=IJ.createImage("", (int)Math.round(100*factor), (int)Math.round(100*factor),(int)Math.round(100*factor),8);
			VitimageUtils.adjustImageCalibration(imgOut,new double[] {1,1,3},"mm");
			IJ.saveAsTiff(imgOut,"/home/fernandr/Bureau/Test/TestBigData/img"+nt+""+nm+".tif");
		}
		
		//create image
		//
	}
	
	public void setupTwoImagesRegistration(String pathToRef,String pathToMov) {
		this.nTimes=1;
		this.nMods=2;
		setupStructures();
		this.paths[0][0]=pathToRef;
		this.paths[0][1]=pathToMov;
		checkComputerCapacity();
		openImagesAndCheckOversizing();
		startRegistrationInterface();
		defineRegistrationSettingsFromTwoImages(this.images[0][0],this.images[0][1]);
		updateViewTwoImages();
		enable(new int[] {RUN,SETTINGS,SOS,BOXACT,BOXTRANS,BOXDISPMAN});		
	}


	public void setupStructures(){
		this.images=new ImagePlus[this.nTimes][this.nMods];
		this.transforms=new ItkTransform[this.nTimes][this.nMods];
		this.previousTransforms=new ItkTransform[this.nTimes][this.nMods];
		this.paths=new String[this.nTimes][this.nMods];
		this.initDimensions=new int[this.nTimes][nMods][3];
		this.dimensions=new int[this.nTimes][this.nMods][3];
		this.initVoxs=new double[this.nTimes][this.nMods][3];
		this.imageSizes=new int[this.nTimes][this.nMods];
		this.imageRanges=new double[this.nTimes][this.nMods][2];
		this.voxs=new double[this.nTimes][this.nMods][3];
	}

	
	
	/** Main listener
 * 
 */
	@Override
	public void actionPerformed(ActionEvent e) {
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		exec.submit(new Runnable() {
			public void run() 
			{
				if(e.getSource()==settingsButton) {
					openSettingsDialog();					
					updateEstimatedTime();
				}
				if(e.getSource()==settingsDefaultButton) {
					defineRegistrationSettingsFromTwoImages(images[0][0],images[0][1]);	
					updateFieldsDisplay();
					updateEstimatedTime();
				}

				if(e.getSource()==boxTypeAction) {		
					setState(new int[] {BOXOPT,BOXTIME,BOXDISP },boxTypeAction.getSelectedIndex()==1);
					setState(new int[] {BOXDISPMAN },boxTypeAction.getSelectedIndex()!=1);
					updateFieldsDisplay();
					updateEstimatedTime();
				}
				
				if(e.getSource()==boxOptimizer || e.getSource()==boxTiming || e.getSource()==boxTypeTrans || e.getSource()==boxDisplay) {
					if(e.getSource()==boxTiming) {
						expectedExecutionTime=listExpectedExecutionTimes[boxTiming.getSelectedIndex()];
						System.out.println("Update expected time = "+expectedExecutionTime);
					}
					if(e.getSource()==boxOptimizer) {
						setParameterOptimizerType(boxOptimizer.getSelectedIndex()==0 ? OptimizerType.BLOCKMATCHING : OptimizerType.ITK_AMOEBA);
						System.out.println("Update optimizer = "+getParameterOptimizerType());
						updateFieldsDisplay();
					}
					if(e.getSource()==boxTypeTrans) {
						setParameterTransformType(transform3DList[boxTypeTrans.getSelectedIndex()]);
						System.out.println("Update transform type = "+getParameterTransformType());
					}
					if(e.getSource()==boxDisplay) {
						viewRegistrationLevel=boxDisplay.getSelectedIndex();
						System.out.println("Update dynamic display level= "+viewRegistrationLevel);
					}
					updateEstimatedTime();
				}

				if(e.getSource()==runButton) {
					disable(RUN);
					if(flagDenseWasLast)flagDenseWasLast=false;//Adding a transform over a Dense one -> dense is not anymore removable by undo, thus the transform will be exported as a dense field
					if(flagAxisWasLast)flagAxisWasLast=false;//Adding a transform over a Dense one -> dense is not anymore removable by undo, thus the transform will be exported as a dense field
					//si automatique
					else if(((String)(boxTypeAction.getSelectedItem())).equals(textActions[1])){	
						boolean undoEn=undoButton.isEnabled();
						disable(new int[] {RUN,SAVE,FINISH,SETTINGS,UNDO,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
						if(boxOptimizer.getSelectedIndex()==0) {//BlockMatching
							disable(RUN);
							runButton.setText("Running Blockmatching...");
							bmRegistration=new BlockMatchingRegistration(flagAxis ? transforms[0][0].transformImage(images[0][0],images[0][0],false) : images[0][0],images[0][1],getParameterTransformType(),MetricType.SQUARED_CORRELATION,
									getParameterValue("Sigma resampling"),getParameterSigma(),getParameterValue("Higher accuracy")==1 ? -1 : getParameterValue("Level min"),getParameterValue("Level max"),getParameterValue("Iterations BM"),
									images[0][0].getStackSize()/2,null  ,getParameterValue("BM Neigh X"),getParameterValue("BM Neigh Z"),
									getParameterValue("BM BHS X"),getParameterValue("BM BHS Z"),	getParameterValue("BM Stride X"),getParameterValue("BM Stride Z"));
							bmRegistration.refRange=imageRanges[0][0];
							bmRegistration.movRange=imageRanges[0][1];
							bmRegistration.flagRange=true;
							bmRegistration.percentageBlocksSelectedByScore=getParameterValue("BM Select score");
							bmRegistration.minBlockVariance=0.04;
							bmRegistration.displayRegistration=viewRegistrationLevel;
							bmRegistration.displayR2=false;
							enable(ABORT);
							ItkTransform trTemp=bmRegistration.runBlockMatching(new ItkTransform(transforms[0][1]));
							disable(ABORT);
							if(! actionAborted) {
								setTransform(trTemp,0,1);
								bmRegistration.closeLastImages();
								bmRegistration.freeMemory();
								addAction("Blockmatching "+getParameterTransformType()+" "+(getParameterTransformType()==Transform3DType.DENSE ? (getParameterSigma()+" ") : "") +
										" -lev"+(getParameterValue("Higher accuracy")==1 ? -1 : getParameterValue("Level min"))+"|"+getParameterValue("Level max")+
										" -Nei"+getParameterValue("BM Neigh X")+"|"+getParameterValue("BM Neigh Y")+"|"+getParameterValue("BM Neigh Z")+
										" -BHS"+getParameterValue("BM BHS X")+"|"+getParameterValue("BM BHS Y")+"|"+getParameterValue("BM BHS Z")+
										" -Str"+getParameterValue("BM Stride X")+"|"+getParameterValue("BM Stride Y")+"|"+getParameterValue("BM Stride Z")+
										" -Scr "+getParameterValue("BM Select score")+" -Lts "+getParameterValue("BM Select LTS"));
								if(getParameterTransformType()==Transform3DType.DENSE) {
									if(! flagDense) {flagDense=true;flagDenseWasLast=true;}
								}
							}
							else actionAborted=false;
							if(undoEn)enable(UNDO);//TODO : abort marche pas avec 2D
						}
						else {//Itk iconic
							runButton.setText("Running Itk registration...");
							itkManager=new ItkRegistrationManager();
							itkManager.refRange=imageRanges[0][0];
							itkManager.movRange=imageRanges[0][1];
							itkManager.flagRange=true;
							itkManager.displayRegistration=viewRegistrationLevel;
							enable(ABORT);
							System.out.println("STARTING  ITK");
							ItkTransform trTemp=itkManager.runScenarioFromGui(new ItkTransform(transforms[0][1]),
									flagAxis ? transforms[0][0].transformImage(images[0][0]  ,    images[0][0],false) : images[0][0],
											images[0][1], getParameterTransformType(), getParameterValue("Level min"),getParameterValue("Level max"),getParameterValue("Iterations ITK"),getParameterItkLearningRate());
							disable(ABORT);
						
							if(! actionAborted) {
								setTransform(trTemp,0,1);
								addAction("Itk iconic "+getParameterTransformType()+" "+(getParameterTransformType()==Transform3DType.DENSE ? (getParameterSigma()+" ") : "") +
										" -lev"+getParameterValue("Level min")+"|"+getParameterValue("Level max")+
										" -parameters to define");
								if(getParameterTransformType()==Transform3DType.DENSE) {
									if(! flagDense) {flagDense=true;flagDenseWasLast=true;}
								}
							}
							else actionAborted=false;
						}
						runButton.setText("Start this action");
						updateViewTwoImages();
						enable(new int[] {RUN,SAVE,FINISH,SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
						if(undoEn)enable(UNDO);
					}
					
					//si manuel
					if(((String)(boxTypeAction.getSelectedItem())).equals(textActions[0])){						
						disable(RUN);

						//Parameters verification
						if(boxTypeTrans.getSelectedIndex()>0 && kindOfViewer==VIEWER_3D) {
							IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform."+
								"If you just intend to compute a rigid transform (no deformation / dilation), please select RIGID in the transformation list. Otherwise, select the 2d viewer in "+
								"the settings to compute a similarity from landmarks points, or select automatic block matching registration to compute a dense vector field");
							enable(RUN);
							return;
						}
						if(boxTypeTrans.getSelectedIndex()>1 && kindOfViewer==VIEWER_2D) {
							IJ.showMessage("Warning : transform is set to Vector field. But the 3d viewer can only compute Rigid and Similarity transform."+
								"Please select automatic block matching registration to compute a dense vector field");
							enable(RUN);
							return;
						}

						//Starting manual registration
						if(runButton.getText().equals("Start this action")) {
							disable(new int[] {BOXACT,RUN});
							runButton.setText("Position ok");
							
							ImagePlus imgMovCurrentState=transforms[0][1].transformImage(images[0][0],images[0][1],false);
							imgMovCurrentState.setDisplayRange(imageRanges[0][0][0],imageRanges[0][0][1]);
							if(kindOfViewer==VIEWER_3D) {
								run3dInterface(flagAxis ? transforms[0][0].transformImage(images[0][0],images[0][0],false) : images[0][0],imgMovCurrentState);
								setRunToolTip(MANUAL3D);
							}
							else {
								run2dInterface(flagAxis ? transforms[0][0].transformImage(images[0][0],images[0][0],false) : images[0][0],imgMovCurrentState);
								setRunToolTip(MANUAL2D);								
							}
							enable(new int[] {ABORT,RUN});
						}
						else {
							//Window verifications
							if( ( (kindOfViewer==VIEWER_3D) && (universe==null)) || 
							( (kindOfViewer==VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage("Image 1")==null) || (WindowManager.getImage("Image 2")==null) ) ) ) {
								//Wild aborting procedure
								boolean undoEn=(undoButton.isEnabled());
								disable(new int[] {RUN,ABORT});
								if((WindowManager.getImage("Image 1")!=null)) WindowManager.getImage("Image 1").close();
								if((WindowManager.getImage("Image 2")!=null)) WindowManager.getImage("Image 2").close();
								if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
								runButton.setText("Start this action");
								setRunToolTip(MAIN);
								enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN});
								if(undoEn)enable(UNDO);
								return;
							}
							if((kindOfViewer==VIEWER_2D) && (RoiManager.getInstance().getCount()<10 ) ) {IJ.showMessage("Please identify at least 10 points (5 correspondance couples)");enable(RUN);return;}
							disable(new int[] {ABORT,RUN});
							
							//Closing manual registration
							ItkTransform tr=null;
							if(kindOfViewer==VIEWER_3D) 	tr=close3dInterface();
							else						 	tr=close2dInterface();
							addTransform(tr,0,1);
					    	addAction("Manual registration" +(kindOfViewer==VIEWER_2D ? " in 2d interface" : "in 3d interface"));
							updateViewTwoImages();
							runButton.setText("Start this action");
							setRunToolTip(MAIN);
							enable(new int[] {UNDO,BOXACT,FINISH,SAVE,RUN});
						}
					}
					
				
					
					else {//si axis alignment
						if((boxTypeAction.getSelectedIndex()==2)){						
							disable(RUN);
							
							if(runButton.getText().equals("Start this action")) {

								//Parameters verification
								if(boxTypeTrans.getSelectedIndex()>0 && kindOfViewer==VIEWER_3D) {
									IJ.showMessage("Warning : transform is not set to Rigid. But the 3d viewer can only compute Rigid transform."+
										"If you just intend to compute a rigid transform (no deformation / dilation), please select RIGID in the transformation list. Otherwise, select the 2d viewer in "+
										"the settings to compute a similarity from landmarks points, or select automatic block matching registration to compute a dense vector field");
									enable(RUN);
									return;
								}
								if(boxTypeTrans.getSelectedIndex()>1 && kindOfViewer==VIEWER_2D) {
									IJ.showMessage("Warning : transform is set to Vector field. But the 3d viewer can only compute Rigid and Similarity transform."+
										"Please select automatic block matching registration to compute a dense vector field");
									enable(RUN);
									return;
								}

								//Running axis alignment
								disable(new int[] {BOXACT,FINISH,SAVE,SETTINGS,UNDO});
								runButton.setText("Axis ok");
								if(kindOfViewer==VIEWER_3D) {
									run3dInterface((!flagAxis) ? images[0][0] : transforms[0][0].transformImage(images[0][0],images[0][0],false),null);
									setRunToolTip(MANUAL3D);
								}
								else {
									run2dInterface((!flagAxis) ? images[0][0] : transforms[0][0].transformImage(images[0][0],images[0][0],false),null);
									setRunToolTip(MANUAL2D);
								}
								enable(new int[] {ABORT,RUN});
							}
							else {
								//Plugin state verification
								if( ( (kindOfViewer==VIEWER_3D) && (universe==null)) || 
										( (kindOfViewer==VIEWER_2D) && ( ( RoiManager.getInstance()==null) || (WindowManager.getImage("Image 1")==null) || (WindowManager.getImage("Image 2")==null) ) ) ) {
									System.out.println("Wild aborting");
									//Wild aborting procedure
									boolean undoEn=(undoButton.isEnabled());
									disable(new int[] {RUN,ABORT});
									if((WindowManager.getImage("Image 1")!=null)) {WindowManager.getImage("Image 1").changes=false;WindowManager.getImage("Image 1").close();}
									if((WindowManager.getImage("Image 2")!=null)) {WindowManager.getImage("Image 2").changes=false;WindowManager.getImage("Image 2").close();}
									if(RoiManager.getInstance()!=null)RoiManager.getInstance().close();
									runButton.setText("Start this action");
									setRunToolTip(MAIN);
									enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN});
									if(undoEn)enable(UNDO);
									return;
								}
								if((kindOfViewer==VIEWER_2D) && (RoiManager.getInstance().getCount()<10 ) ) {IJ.showMessage("Please identify at least 10 points (5 correspondance couples)");enable(RUN);return;}

								//Closing axis alignement
								disable(new int[] {RUN,ABORT});
								ItkTransform tr=null;
								if(kindOfViewer==VIEWER_3D) 	tr=close3dInterface();
								else						 	tr=close2dInterface();
								addTransformToAllTimesAndMods(tr);
								if(! flagAxis) {flagAxis=true;flagAxisWasLast=true;}
								addAction("Axis alignment of reference");
								updateViewTwoImages();
								runButton.setText("Start this action");
								setRunToolTip(MAIN);
								enable(new int[] {UNDO,FINISH,SAVE,SETTINGS,BOXACT,RUN});
							}
						}
					}
				}
				
				if(e.getSource()==undoButton && step>0) {
					disable(new int[] {RUN,SAVE,FINISH});
					System.out.println("Start an undo action");
					if(flagDenseWasLast) {flagDenseWasLast=false;flagDense=false;}
					if(flagAxisWasLast) {flagAxisWasLast=false;flagAxis=false;}
					applyUndoToTransforms();
					removeAction();
					updateViewTwoImages();
					enable(new int[] {RUN,SAVE,FINISH,BOXACT});
					disable(UNDO);
				}
				
				if(e.getSource()==sos1Button) {
					runSos();
				}
				
				if(e.getSource()==finishButton  ||  e.getSource()==saveButton) {
					disable(new int[] {RUN,FINISH,SAVE,UNDO});
					if(e.getSource()==finishButton)runExport();
					else runSave();
					enable(new int[] {FINISH,SAVE,RUN});
					if(step>0)enable(UNDO);
				}
				
				if(e.getSource()==abortButton) {
					boolean undoEn=(undoButton.isEnabled());
					disable(new int[] {RUN,SAVE,FINISH,UNDO, SETTINGS,BOXACT,BOXOPT,BOXTIME,BOXTRANS,BOXDISP});
					actionAborted=true;
					System.out.println("Entering aborting procedure");
					if(runButton.getText().equals("Position ok")){
						disable(new int[] {RUN,ABORT});
						close3dInterface();
						runButton.setText("Start this action");
						setRunToolTip(MAIN);
						enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN});
						if(undoEn)enable(UNDO);
					}
					else if(runButton.getText().equals("Axis ok")){
						disable(new int[] {RUN,ABORT});
						close3dInterface();
						runButton.setText("Start this action");
						setRunToolTip(MAIN);
						enable(new int[] {FINISH,SAVE,SETTINGS,BOXACT,RUN});
						if(undoEn)enable(UNDO);
					}
					else if(runButton.getText().equals("Running Blockmatching...")){
						System.out.println("aborting bockmatching");
						int nThreads=bmRegistration.threads.length;
						while(!bmRegistration.bmIsInterruptedSucceeded) {
							VitimageUtils.waitFor(100);
							bmRegistration.bmIsInterrupted=true;
							for(int th=0;th<nThreads;th++)bmRegistration.threads[th].interrupt();
						}
					}
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
			}
		});
		System.gc();
	}

	
	
	/** Export-Save routines and sos
	*
	*/
	public void runExport() {
		//Ask for target dimensions
		ImagePlus refResult = null,movResult=null;
		GenericDialog gd=new GenericDialog("Choose target dimensions...");
		gd.addMessage("Initial dimensions : ");
		gd.addMessage("..Reference image : "+this.initDimensions[0][0][0]+" x "+this.initDimensions[0][0][1]+" x "+this.initDimensions[0][0][2]+
				" avec des voxels "+this.initVoxs[0][0][0]+" x "+this.initVoxs[0][0][1]+" x "+this.initVoxs[0][0][2]);
		gd.addMessage("..Moving image : "+this.initDimensions[0][1][0]+" x "+this.initDimensions[0][1][1]+" x "+this.initDimensions[0][1][2]+
				" avec des voxels "+this.initVoxs[0][1][0]+" x "+this.initVoxs[0][1][1]+" x "+this.initVoxs[0][1][2]);
		gd.addMessage("\nYou can choose the target dimensions to export both images,\nand the transform that resample the moving image onto the reference image");
		gd.addMessage("If you feel lost after this explanation, consider using dimensions of the reference image.\n.\n.");
		gd.addMessage("After this operation, you can combine reference and moving images with the Color / merge channels tool of ImageJ)");
		gd.addChoice("Choose target dimensions", new String[] {"Dimensions of reference image","Dimensions of moving image","Provide custom dimensions"}, "Dimensions of reference image");
		gd.showDialog();
		int choice=gd.getNextChoiceIndex();
		if(choice==0) {
			refResult=this.transforms[0][0].transformImage(this.images[0][0], this.images[0][0], false);
			movResult=this.transforms[0][1].transformImage(this.images[0][0], this.images[0][1], false);
		}
		if(choice==1) {
			refResult=this.transforms[0][0].transformImage(this.images[0][1], this.images[0][0], false);
			movResult=this.transforms[0][1].transformImage(this.images[0][1], this.images[0][1], false);
		}

		if(choice==2) {
			GenericDialog gd2=new GenericDialog("Provide custom dimensions (and voxel sizes)");
			gd2.addNumericField("Dimension along X",1, 0);
			gd2.addNumericField("Dimension along Y",1, 0);
			gd2.addNumericField("Dimension along Z",1, 0);
			gd2.addNumericField("Voxel size along X",1, 6);
			gd2.addNumericField("Voxel size along Y",1, 6);
			gd2.addNumericField("Voxel size along Z",1, 6);					
			gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        int[]outputDimensions=new int[] {(int)Math.round(gd.getNextNumber()),(int)Math.round(gd.getNextNumber()),(int)Math.round(gd.getNextNumber())};
	        double[]outputVoxs=new double[] {gd.getNextNumber(),gd.getNextNumber(),gd.getNextNumber()};
			refResult=this.transforms[0][0].transformImage(outputDimensions, outputVoxs,this.images[0][0], false);
			movResult=this.transforms[0][1].transformImage(outputDimensions, outputVoxs,this.images[0][1], false);
		}
		VitiDialogs.saveImageUI(refResult, "Save reference image", false, "", "image_ref_transformed.tif");
		VitiDialogs.saveImageUI(movResult, "Save moving image", false, "", "image_mov_transformed.tif");
		if(this.flagAxis)VitiDialogs.saveMatrixTransformUI(this.transforms[0][0].simplify(), "Save transform applied to reference image (for axis alignement)", false, "", "transform_ref.txt");
		if(this.flagDense)VitiDialogs.saveDenseFieldTransformUI(this.transforms[0][1].flattenDenseField(refResult),  "Save transform applied to moving image", false, "", "transform_mov.txt", refResult);	
		System.out.println("Exportation finished.");
	}
	
	public void startFromSavedStateTwoImages(String pathToFile) {
		this.mode=MODE_TWO_IMAGES;
		String []names;
		if(pathToFile==null) {
			names=VitiDialogs.openFileUI("Select a file to load a plugin state","save","fjm");
			if(names==null) {
				IJ.showMessage("No file selected. Save cannot be done");
				return;
			}
		}
		else {
			names=new String[] {new File(pathToFile).getParent(),new File(pathToFile).getName()};
		}
		this.nTimes=1;
		this.nMods=2;
		setupStructures();
		checkComputerCapacity();
		
		String nameDir=names[0];
		String nameWithNoExt=names[1].substring(0,names[1].lastIndexOf('.'));
		File dirData=new File(nameDir,nameWithNoExt+"_DATA_FJM");
		String nameDataDir=dirData.getAbsolutePath();
		String filePath=new File(nameDir,names[1]).getAbsolutePath();

		//Read informations in filePath
		String str=VitimageUtils.readStringFromFile(filePath);
		String[]lines=str.split("\n");
		this.mode=Integer.parseInt(lines[1].split("=")[1]);
		nameDataDir=(lines[2].split("=")[1]);
		this.paths[0][0]=(lines[3].split("=")[1]);
		this.paths[0][1]=(lines[4].split("=")[1]);
		this.flagAxis=Integer.parseInt(lines[5].split("=")[1])==1;
		this.flagDense=Integer.parseInt(lines[6].split("=")[1])==1;
		int tempStep=Integer.parseInt(lines[7].split("=")[1]);
		for(int st=0;st<tempStep;st++) {
			addAction(lines[8].split("=")[1].split("~")[st].split(": ")[1]);
		}
		
		//Read data from directory
		if(this.flagAxis) 			this.transforms[0][0]=ItkTransform.readTransformFromFile(new File(nameDataDir,"transfo_ref.txt").getAbsolutePath());
		if(this.flagDense)			this.transforms[0][1]=ItkTransform.readAsDenseField(new File(nameDataDir,"transfo_mov.tif").getAbsolutePath());
		else this.transforms[0][1]=ItkTransform.readTransformFromFile(new File(nameDataDir,"transfo_mov.txt").getAbsolutePath());
		System.out.println("Reading done from file = "+filePath);
		openImagesAndCheckOversizing();
		startRegistrationInterface();
		updateViewTwoImages();
		defineRegistrationSettingsFromTwoImages(this.images[0][0],this.images[0][1]);
		enable(new int[] {RUN,SETTINGS,SOS,BOXACT,BOXTRANS,BOXDISPMAN});		
		
	}
	
	public void runSave() {
		if(mode==MODE_TWO_IMAGES)runSaveTwoImages();
		else runSaveNmImages();
	}

	public void runSaveNmImages() {
		
	}
	
	public void runSaveTwoImages() {
		String []names=VitiDialogs.openFileUI("Select a file to save the plugin state","save",".fjm");
		if(names==null) {
			IJ.showMessage("No file selected. Save cannot be done");
			return;
		}
		System.out.println("Opened : path = "+names[0]+"  file="+names[1]);
		String nameDir=names[0];
		String nameFile=names[1];
		System.out.println("Test split :");
		System.out.println("Est null ? "+(names[1]==null));
		System.out.println("Last index="+names[1].lastIndexOf('.'));
		String nameWithNoExt=names[1].substring(0,names[1].lastIndexOf('.'));
		File dirData=new File(nameDir,nameWithNoExt+"_DATA_FJM");
		String nameDataDir=dirData.getAbsolutePath();
		String filePath=new File(nameDir,names[1]).getAbsolutePath();

		//Write informations in filePath
		System.out.println("DEBUG 16");
		String str="#Fijiyama save###\n"+
				"#Mode="+MODE_TWO_IMAGES+"\n"+
				"#Saving path="+nameDataDir+"\n"+
				"#Path to reference image="+this.paths[0][0]+"\n"+
				"#Path to moving image="+this.paths[0][1]+"\n"+
				"#Flag axis="+(this.flagAxis ? 1 : 0)+"\n"+
				"#Flag dense="+(this.flagDense ? 1 : 0)+"\n"+
				"#Step="+step+"\n"+
				"#Previous actions=";
		for(int st=0;st<step;st++)str+=this.listActions.getModel().getElementAt(st)+"~";
		str+="\n#\n";
		VitimageUtils.writeStringInFile(str, filePath);
		System.out.println("DEBUG 17");
		
		//Save data in a directory
		dirData.mkdirs();
		VitimageUtils.writeStringInFile(str, new File(nameDataDir,nameFile).getAbsolutePath());
		String tempPath;
		System.out.println("DEBUG 18");
		if(flagAxis) {
			tempPath=new File(nameDataDir,"transfo_ref.txt").getAbsolutePath();
			this.transforms[0][0]=this.transforms[0][0].simplify();
			this.transforms[0][0].writeMatrixTransformToFile(tempPath);
		}
		if(flagDense){
			tempPath=new File(nameDataDir,"transfo_mov.tif").getAbsolutePath();
			this.transforms[0][1]=this.transforms[0][1].flattenDenseField(this.images[0][0]);
			this.transforms[0][1].writeAsDenseField(tempPath,this.images[0][0]);
		}
		else {
			tempPath=new File(nameDataDir,"transfo_mov.txt").getAbsolutePath();
			this.transforms[0][1]=this.transforms[0][1].simplify();
			this.transforms[0][1].writeMatrixTransformToFile(tempPath);
		}
		System.out.println("DEBUG 19");

		System.out.println("Saving done. File = "+filePath);
	}

	
	
	/** Updating routines for structures, view and buttons
	 * 
	 */
	public void addAction(String text) {
		this.textPreviousActions[step]="Step "+step+" : "+text;
		this.updateList();
		step++;
		this.listActions.setSelectedIndex(step);
	}
	
	public void removeAction() {
		this.textPreviousActions[step-1]="-";
		this.updateList();
		step--;
		this.listActions.setSelectedIndex(step);
	}
	
	public void updateList() {
		DefaultListModel<String> listModel = new DefaultListModel<String>();
        for(int i=0;i<textPreviousActions.length;i++)listModel.addElement(textPreviousActions[i]);
		this.listActions.setModel(listModel);
	}
			
	public void updateEstimatedTime() {
		if((this.boxTypeAction.getSelectedIndex()==0) && this.boxDisplayMan.getSelectedIndex()==0)this.estimatedTime=300;//manual registration with landmarks
		if((this.boxTypeAction.getSelectedIndex()==0) && this.boxDisplayMan.getSelectedIndex()==1)this.estimatedTime=120;//manual registration in 3d
		if((this.boxTypeAction.getSelectedIndex()==2) && this.boxDisplayMan.getSelectedIndex()==0)this.estimatedTime=200;//axis alignment with landmarks
		if((this.boxTypeAction.getSelectedIndex()==2) && this.boxDisplayMan.getSelectedIndex()==1)this.estimatedTime=120;//axis alignment in 3d
		else {
			if(this.boxOptimizer.getSelectedIndex()==0)this.estimatedTime=(int)Math.round(BlockMatchingRegistration.estimateRegistrationDuration(
					this.imageParameters,viewRegistrationLevel,this.expectedExecutionTime,getParameterValue("Level min"),getParameterValue("Level max"),getParameterValue("Iterations BM"),getParameterTransformType(),
					new int[] {getParameterValue("BM BHS X"),getParameterValue("BM BHS Y"),getParameterValue("BM BHS Z")},
					new int[] {getParameterValue("BM Stride X"),getParameterValue("BM Stride Y"),getParameterValue("BM Stride Z")},
					new int[] {getParameterValue("BM Neigh X"),getParameterValue("BM Neigh Y"),getParameterValue("BM Neigh Z")},
					this.nbCpu,getParameterValue("BM Select score"),getParameterValue("BM Select LTS"),getParameterValue("BM Select Rand"),getParameterValue("BM Subsample Z")==1,getParameterValue("Higher accuracy")
					));
		
			
			else this.estimatedTime=ItkRegistrationManager.estimateRegistrationDuration(
					this.imageParameters,viewRegistrationLevel,this.expectedExecutionTime,getParameterValue("Iterations ITK"), getParameterValue("Level min"),getParameterValue("Level max"));
		}
		int nbMin=this.estimatedTime/60;
		int nbSec=this.estimatedTime%60;
		this.labelTime2.setText(""+nbMin+" mn and "+nbSec+" s");
		System.out.println("Actualisation : estimatedTime="+this.estimatedTime+" et expectedTime="+this.expectedExecutionTime);
		if(this.estimatedTime>this.expectedExecutionTime)this.labelTime2.setForeground(new Color(200,20,20));
		else this.labelTime2.setForeground(new Color(10,200,10));
		if(this.boxTypeAction.getSelectedIndex()!=1)this.labelTime2.setForeground(new Color(150,150,150));
	}

	public void updateViewTwoImages() {
		ImagePlus imgMovCurrentState,imgRefCurrentState;
		if(this.transforms[0][1]==null)imgMovCurrentState=this.images[0][1];
		else imgMovCurrentState= this.transforms[0][1].transformImage(this.images[0][0],this.images[0][1],false);
		imgMovCurrentState.setDisplayRange(this.imageRanges[0][0][0], this.imageRanges[0][0][1]);
		
		if(this.transforms[0][0]==null)imgRefCurrentState=this.images[0][0];
		else imgRefCurrentState= this.transforms[0][0].transformImage(this.images[0][0],this.images[0][0],false);		
		imgRefCurrentState.setDisplayRange(this.imageRanges[0][0][0], this.imageRanges[0][0][1]);

		imgView=VitimageUtils.compositeNoAdjustOf(imgRefCurrentState,imgMovCurrentState,step==0 ? "Superimposition before registration" : "Registration results after "+step+" steps");
		this.viewSlice=this.images[0][0].getStackSize()/2;
		imgView.show();imgView.setSlice(this.viewSlice);imgView.updateAndRepaintWindow();
		adjustFrameOnScreenRelative(imgView.getWindow(),registrationFrame,0,0);
	}

	public void updateFieldsDisplay() {
		System.out.println("Updating fields");
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
		else {
			for(int i=0;i<textDisplayITK.length;i++)listModelDisp.addElement(textDisplayITK[i]);
	        for(int i=0;i<textTransformsITK.length;i++)listModelTrans.addElement(textTransformsITK[i]);
			this.boxDisplay.setModel(listModelDisp);
			this.boxDisplay.setSelectedIndex(valDisp>=textDisplayITK.length ? textDisplayITK.length-1 : valDisp);
			this.boxTypeTrans.setModel(listModelTrans);
			this.boxTypeTrans.setSelectedIndex(valTrans>=textTransformsITK.length ? textTransformsITK.length : valTrans);
		}
		this.boxDisplay.repaint();
		this.viewRegistrationLevel=this.boxDisplay.getSelectedIndex();
	}

	public void addTransform(ItkTransform tr,int nt,int nm) {
		for(int nnt=0;nnt<this.nTimes;nnt++)for(int nnm=0;nnm<this.nMods;nnm++) if(imageExists(nnt,nnm)) {
			if((nnt==nt) && (nnm==nm)) {
				this.previousTransforms[nt][nm]=new ItkTransform(this.transforms[nt][nm]);
				this.transforms[nt][nm].addTransform(tr);
			}
			else {
				this.previousTransforms[nt][nm]=this.transforms[nt][nm];
			}
		}
	}

	public void addTransformToAllTimesAndMods(ItkTransform tr) {
		for(int nnt=0;nnt<this.nTimes;nnt++)for(int nnm=0;nnm<this.nMods;nnm++)if(imageExists(nnt,nnm)) {
			this.previousTransforms[nnt][nnm]=new ItkTransform(this.transforms[nnt][nnm]);
			this.transforms[nnt][nnm].addTransform(tr);
		}
	}
	
	public void setTransform(ItkTransform tr,int nt,int nm) {
		for(int nnt=0;nnt<this.nTimes;nnt++)for(int nnm=0;nnm<this.nMods;nnm++)if(imageExists(nnt,nnm)) { 
			if((nnt==nt) && (nnm==nm)) {
				this.previousTransforms[nt][nm]=new ItkTransform(this.transforms[nt][nm]);
				this.transforms[nt][nm]=new ItkTransform(tr);
			}
			else this.previousTransforms[nt][nm]=this.transforms[nt][nm];
		}		
	}
	
	public void applyUndoToTransforms() {
		for(int nt=0;nt<this.nTimes;nt++)for(int nm=0;nm<this.nMods;nm++)  if(imageExists(nt,nm)){
			System.out.println("removing transform to this one "+nt+" "+nm);
			System.out.println("Transform previous before= "+this.previousTransforms[nt][nm].drawableString());
			System.out.println("Transform current before= "+this.transforms[nt][nm].drawableString());
			this.transforms[nt][nm]=new ItkTransform(this.previousTransforms[nt][nm]);
			this.previousTransforms[nt][nm]=new ItkTransform() ;
			System.out.println("Transform previous after= "+this.previousTransforms[nt][nm].drawableString());
			System.out.println("Transform current after= "+this.transforms[nt][nm].drawableString());
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
			case BOXACT:this.boxTypeAction.setEnabled(state);break;
			case BOXOPT:this.boxOptimizer.setEnabled(state);break;
			case BOXTRANS:this.boxTypeTrans.setEnabled(state);break;
			case BOXTIME:this.boxTiming.setEnabled(state);break;
			case BOXDISP:this.boxDisplay.setEnabled(state);break;
			case BOXDISPMAN:this.boxDisplayMan.setEnabled(state);break;
			case SETTINGS:this.settingsButton.setEnabled(state);this.settingsDefaultButton.setEnabled(state);break;
			case RUN:this.runButton.setEnabled(state);break;
			case UNDO:this.undoButton.setEnabled(state);break;
			case ABORT:this.abortButton.setEnabled(state);abortButton.setBackground(state ? new Color(255,0,0) : colorIdle);break;
			case SAVE:this.saveButton.setEnabled(state);break;
			case FINISH:this.finishButton.setEnabled(state);break;
			case SOS:this.sos1Button.setEnabled(state);break;
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
	
	                                     
	
	
	/** Routines describing manual registration. If called with a null imgMov, set orthoslices axis instead in order to run registration of ref with XYZ axis
	 */
	@SuppressWarnings("deprecation")
	public void run3dInterface(ImagePlus imgRef,ImagePlus imgMov) {
		ImagePlus refCopy=VitimageUtils.imageCopy(imgRef);
		IJ.run(refCopy,"8-bit","");
		this.universe=new ij3d.Image3DUniverse();
		universe.show();
		System.out.println("En effet, step="+step);

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
		IJ.showMessage("Move the green volume to match the red one\nWhen done, push the \""+runButton.getText()+"\" button to stop\n\nCommands : \n"+
				"mouse-drag the green object to turn the object\nmouse-drag the background to turn the scene\nCTRL-drag to translate the green object");
		adjustFrameOnScreenRelative((Frame)((JPanel)(this.universe.getCanvas().getParent())).getParent().getParent().getParent(),this.imgView.getWindow(),0,1);
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
		refCopy.show();
		refCopy.setTitle("Image 1");
		if(imgMov!=null) {
			movCopy=VitimageUtils.imageCopy(imgMov);		
			movCopy.resetDisplayRange();
			IJ.run(movCopy,"8-bit","");
			movCopy.show();
			movCopy.setTitle("Image 2");
		}
		else {
			movCopy=VitimageUtils.getBinaryGrid(refCopy,17);
			movCopy.show();
			movCopy.setTitle("Image 2");
		}

		IJ.showMessage("Examine images, identify correspondances between images and use the Roi manager to build a list of correspondances points. Points should be given this way : \n- Point A  in image 1\n- Correspondant of point A  in image 2\n- Point B  in image 1\n- Correspondant of point B  in image 2\netc...\n"+
		"Once done (at least 5-15 couples of corresponding points), push the \""+runButton.getText()+"\" button to stop\n\n");
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		adjustImageOnScreen(movCopy, 0,0);
		adjustFrameOnScreenRelative((Frame)rm,refCopy.getWindow(),0,0);
		adjustFrameOnScreenRelative(refCopy.getWindow(),(Frame)rm,0,0);
		
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
	
//	TODO : debug window
	//TODO : already read messages

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

	//TODO : uniformiser interface, evolutif en fonction des options
	//TODO : faire evoluer l aide contextuelle pour expliquer un peu tout
	//TODO : abort marche pas avec landmarks
	//TODO : landmarks 2d estime transfo en fonction du choix actuel
	//TODO : grid plus claire, avec un centre
	
	/** Gui setup
	 */
	public void startRegistrationInterface() {
		//Panel 1 , with main settings
		JPanel registrationPanel1=new JPanel();
		registrationPanel1.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));		
		registrationPanel1.setLayout(new GridLayout(10,2,15,10));
		registrationPanel1.add(labelNextAction);
		registrationPanel1.add(boxTypeAction);		
		registrationPanel1.add(labelTransformation);
		registrationPanel1.add(boxTypeTrans);		
		registrationPanel1.add(labelView);
		registrationPanel1.add(boxDisplay);
		registrationPanel1.add(labelOptimizer);
		registrationPanel1.add(boxOptimizer);
		registrationPanel1.add(labelViewMan);
		registrationPanel1.add(boxDisplayMan);
		registrationPanel1.add(new JLabel(""));
		registrationPanel1.add(new JLabel(""));
		registrationPanel1.add(labelTime1);
		registrationPanel1.add(labelTime2);		
		registrationPanel1.add(new JLabel(""));
		registrationPanel1.add(new JLabel(""));
		registrationPanel1.add(settingsButton);
		registrationPanel1.add(settingsDefaultButton);
		boxTypeAction.addActionListener(this);
		boxOptimizer.addActionListener(this);
		boxTypeTrans.addActionListener(this);
		boxTiming.addActionListener(this);
		boxDisplay.addActionListener(this);		
		settingsButton.addActionListener(this);
		settingsDefaultButton.addActionListener(this);
		disable(new int[] {BOXOPT,BOXACT,BOXTIME,BOXTRANS,BOXDISP,BOXDISPMAN,SETTINGS});
		this.boxDisplay.setSelectedIndex(this.viewRegistrationLevel);
		settingsButton.setToolTipText("<html><p width=\"500\">" +"Advanced settings let you manage more parameters of the automatic registration algorithms"+"</p></html>");
		settingsDefaultButton.setToolTipText("<html><p width=\"500\">" +"Restore settings compute and set default parameters suited to your images"+"</p></html>");

		
		//Panel 2 , with run/undo buttons
		JPanel registrationPanel2=new JPanel();
		registrationPanel2.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
		registrationPanel2.setLayout(new GridLayout(1,3,40,40));
		registrationPanel2.add(runButton);
		registrationPanel2.add(abortButton);
		registrationPanel2.add(undoButton);
		runButton.addActionListener(this);
		setRunToolTip(MAIN);
		
		abortButton.addActionListener(this);
		abortButton.setEnabled(false);
		this.colorIdle=abortButton.getBackground();
		abortButton.setToolTipText("<html><p width=\"500\">" +"Abort means killing a running operation and come back to the state before you clicked on Start this action."+
				"Automatic registration is harder to kill. Please insist on this button until its colour fades to gray"+"</p></html>");

		undoButton.addActionListener(this);
		undoButton.setToolTipText("<html><p width=\"500\">" +"Undo works as you would expect : it delete the previous action, and recover the previous state of transforms and images. Fijiyama handles only one undo : two successive actions can't be undone"+"</p></html>");
		disable(new int[] {RUN,ABORT,UNDO});
		
		//Panel 3 , with export and sos
		JPanel registrationPanel3=new JPanel();
		registrationPanel3.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
		registrationPanel3.setLayout(new GridLayout(1,3,40,40));
		registrationPanel3.add(finishButton);
		registrationPanel3.add(saveButton);
		registrationPanel3.add(sos1Button);
		finishButton.addActionListener(this);
		saveButton.addActionListener(this);
		sos1Button.addActionListener(this);
		finishButton.setToolTipText("<html><p width=\"500\">" +"Export aligned images and computed transforms"+"</p></html>");
		saveButton.setToolTipText("<html><p width=\"500\">" +"Save the current state of the plugin in a .fjm file, including the transforms and image paths. This .ijm file can be used later to restart from this point"+"</p></html>");
		sos1Button.setToolTipText("<html><p width=\"500\">" +"Opens a contextual help"+"</p></html>");
		disable(new int[] {SAVE,FINISH});
		
		//Panel 4 , with list of previous moves
		JPanel registrationPanel4=new JPanel();
		registrationPanel4.setBorder(BorderFactory.createEmptyBorder(25,25,0,25));
		registrationPanel4.setLayout(new GridLayout(1,1,10,10));
		registrationPanel4.add(labelList);
		JPanel registrationPanel5=new JPanel();
		registrationPanel5.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
		registrationPanel5.setLayout(new GridLayout(1,1,10,10));
		registrationPanel5.add(listActions);
		listActions.setSelectedIndex(0);
		

		//Main frame and main panel
		registrationFrame=new JFrame();
		JPanel registrationPanelGlobal=new JPanel();
		registrationPanelGlobal.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		registrationPanelGlobal.setLayout(new BoxLayout(registrationPanelGlobal, BoxLayout.Y_AXIS));
		registrationPanelGlobal.add(registrationPanel1);
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(registrationPanel2);
//		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(registrationPanel3);
		registrationPanelGlobal.add(new JSeparator());
		registrationPanelGlobal.add(registrationPanel4);
		registrationPanelGlobal.add(registrationPanel5);
		registrationFrame.add(registrationPanelGlobal);
		registrationFrame.setTitle("Registration manager : two images registration");
		registrationFrame.pack();
		registrationFrame.setVisible(true);
		registrationFrame.repaint();
		adjustFrameOnScreen(registrationFrame,2,0);
	}
	
	
	
	
	
	/** Helpers to ease the setup : computer measurements, handling of oversized images, automatic definition of parameters values depending on the ref image
	* 
	 */
	public void checkComputerCapacity() {
		int nbCpu=Runtime.getRuntime().availableProcessors();
		System.out.println("Nb cores for multithreading = "+nbCpu);
		this.freeMemory=(int)((new Memory().maxMemory() /(1024*1024)));//Java virtual machine available memory (in Megabytes)
		System.out.println("Available memory in virtual machine = "+this.freeMemory+" MB");
		this.nbCpu=nbCpu;
		this.maxImageSizeForRegistration=this.freeMemory/(4*30);		
		System.out.println("Suggested maximum image size for registration = "+this.maxImageSizeForRegistration+" MB.\n"+
		"If given images are bigger, Fijiyama will ask you for the permission to subsample it before running registration.\nTherefore, alignment results will be computed using the full-size initial data during the export phase");
	}

	public void openImagesAndCheckOversizing() {
		System.out.println("Verif 1 reading : "+this.transforms[0][1].drawableString());
		ImagePlus img;
		boolean thereIsBigImages=false;
		int factor=1;
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
					if(this.transforms[nt][nm]==null)this.transforms[nt][nm]=new ItkTransform();//If it is not the case, it is a startup from a file
					ImagePlus imgTemp=IJ.openImage(this.paths[nt][nm]);
					initDimensions[nt][nm]=VitimageUtils.getDimensions(imgTemp);
					dimensions[nt][nm]=VitimageUtils.getDimensions(imgTemp);
					initVoxs[nt][nm]=VitimageUtils.getVoxelSizes(imgTemp);
					voxs[nt][nm]=VitimageUtils.getVoxelSizes(imgTemp);
					imageSizes[nt][nm]=(int)Math.round((1.0*initDimensions[nt][nm][0]*initDimensions[nt][nm][1]*initDimensions[nt][nm][2])/(1024.0*1024.0));
					if(imageSizes[nt][nm]>this.maxImageSizeForRegistration)thereIsBigImages=true;
				}
			}
		}
		System.out.println("Verif 2 reading : "+this.transforms[0][1].drawableString());
		if(!thereIsBigImages) {
			for(int nt=0;nt<this.nTimes;nt++) {
				for(int nm=0;nm<this.nMods;nm++) {
					if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
						System.out.println("Processing image t"+nt+" mod"+nm+" , size="+imageSizes[nt][nm]+" . No resizing");
						img=IJ.openImage(this.paths[nt][nm]);
						ImageProcessor ip=img.getStack().getProcessor(img.getStackSize()/2+1);
						ip.resetMinAndMax();
						this.imageRanges[nt][nm][0]=ip.getMin();
						this.imageRanges[nt][nm][1]=ip.getMax();
						this.images[nt][nm]=img;
						this.images[nt][nm].setDisplayRange(this.imageRanges[nt][nm][0],this.imageRanges[nt][nm][1]);
						System.out.println("Display range de "+nt+" "+nm+" = "+TransformUtils.stringVectorN(this.imageRanges[nt][nm], ""));
					}
				}
			}
			System.out.println("Verif 99-1 reading : "+this.transforms[0][1].drawableString());
		}
		else{
			String recap="There is oversized images, which can lead to very slow computation, or memory overflow.\n"+
					"Your computer capability has been detected to :\n"+
					" -> "+this.freeMemory+" MB of RAM allowed for this plugin"+"\n -> "+this.nbCpu+" parallel threads for computation."+
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
								System.out.println("Processing image t"+nt+" mod"+nm+" , size="+imageSizes[nt][nm]+" . No resizing");
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
									else {factor=factorPow3;this.dimensions[nt][nm][0]/=factor;this.dimensions[nt][nm][1]/=factor;this.dimensions[nt][nm][2]/=factor;this.voxs[nt][nm][0]*=factor;this.voxs[nt][nm][1]*=factor;this.voxs[nt][nm][2]*=factor;}
								
								}
								System.out.println("Processing image t"+nt+" mod"+nm+" , size="+imageSizes[nt][nm]+" . Resizing.");
								System.out.println(" -> Initial image : "+TransformUtils.stringVector(this.initDimensions[nt][nm], "dims")+TransformUtils.stringVector(this.initVoxs[nt][nm], "voxs"));
								System.out.println(" -> Sampled image : "+TransformUtils.stringVector(this.dimensions[nt][nm], "dims")+TransformUtils.stringVector(this.voxs[nt][nm], "voxs")+ "final size="+((this.dimensions[nt][nm][0]*this.dimensions[nt][nm][1]*this.dimensions[nt][nm][2]/(1024.0*1024.0)))+" Mvoxs" );
								System.out.println();
								img=IJ.openImage(this.paths[nt][nm]);
								ImageProcessor ip=img.getStack().getProcessor(img.getStackSize()/2+1);
								ip.resetMinAndMax();
								this.imageRanges[nt][nm][0]=ip.getMin();
								this.imageRanges[nt][nm][1]=ip.getMax();
								this.images[nt][nm]=ItkTransform.resampleImage(this.dimensions[nt][nm], this.voxs[nt][nm], img,false);
								this.images[nt][nm].setDisplayRange(this.imageRanges[nt][nm][0],this.imageRanges[nt][nm][1]);
								System.out.println("Display range de "+nt+" "+nm+" = "+TransformUtils.stringVectorN(this.imageRanges[nt][nm], ""));								
							}	
						}
					}
				}
			}
			System.out.println("Verif 99-2 reading : "+this.transforms[0][1].drawableString());

		}
		img=null;
		for(int nt=0;nt<this.nTimes;nt++)for(int nm=0;nm<this.nMods;nm++) if(imageExists(nt,nm)) {
			if(this.transforms[nt][nm]==null) {transforms[nt][nm]=new ItkTransform();}
			previousTransforms[nt][nm]=new ItkTransform();
		}
		System.gc();
	}
	
	public void defineRegistrationSettingsFromTwoImages(ImagePlus imgRef,ImagePlus imgMov) {
		int nbStrideAtMinLevel=100;
		double minSubResolutionImageSizeLog2=6.0;//In power of two : min resolution=64;
		double maxSubResolutionImageSizeLog2=8.0;//In power of two : max resolution=512;
		int strideMinZ=3;

		int[]dimsTemp=VitimageUtils.getDimensions(imgRef);
		double[]voxsTemp=VitimageUtils.getVoxelSizes(imgRef);
		double[]sizesTemp=new double[] {dimsTemp[0]*voxsTemp[0],dimsTemp[1]*voxsTemp[1],dimsTemp[2]*voxsTemp[2]};				
		this.setParameterSigma(sizesTemp[0]/20);//Default : gaussian kernel for dense field estimation is 20 times smaller than image
		double anisotropyVox=voxsTemp[2]/Math.max(voxsTemp[1],voxsTemp[0]);
		int levelMin=0;
		int levelMax=0;
		boolean subZ=false;
		setParameterValue("Iterations BM",6);
		setParameterValue("Iterations ITK",100);
		setParameterValue("BM Neigh X",3);
		setParameterValue("BM Neigh Y",3);
		setParameterValue("BM Neigh Z",3);
		if((dimsTemp[2]>=5) && (anisotropyVox<1.5)) {//Cas 3D pur
			subZ=true;
			int []dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-minSubResolutionImageSizeLog2),
						              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-minSubResolutionImageSizeLog2),
					              	  (int)Math.floor(Math.log(dimsTemp[2])/Math.log(2)-minSubResolutionImageSizeLog2)};
			levelMax=Math.min(Math.min(dimsLog2[0], dimsLog2[1]), dimsLog2[2]);
			dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-maxSubResolutionImageSizeLog2),
		              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-maxSubResolutionImageSizeLog2),
	              	  (int)Math.floor(Math.log(dimsTemp[2])/Math.log(2)-maxSubResolutionImageSizeLog2)};
			levelMin=Math.max(Math.max(dimsLog2[0], dimsLog2[1]), dimsLog2[2]);	
			if(levelMin>levelMax)levelMin=levelMax;
			setParameterValue("BM Subsample Z",1);
			setParameterValue("Level min",levelMin);
			setParameterValue("Level max",levelMax);
			setParameterValue("Higher accuracy",levelMin<1 ? 1 : 0);
		}
		else {	
			//Si dims[2]<5, cas 2D --> pas de subsampleZ, levelMin et max defini sur dims 0 et 1, neighZ=0 BHSZ=0 strideZ=1;
			//Sinon si anisotropyVox>1.5 -> pas de subsampleZ levelMin et max defini sur dims 0 et 1, neighZ=3 BHSZ=prop strideZ=prop;
			int []dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-minSubResolutionImageSizeLog2),
		              (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-minSubResolutionImageSizeLog2) };
			System.out.println(TransformUtils.stringVectorN(dimsLog2, "dimsLog2"));
			levelMax=Math.min(dimsLog2[0], dimsLog2[1]);
			dimsLog2=new int[] {(int)Math.floor(Math.log(dimsTemp[0])/Math.log(2)-maxSubResolutionImageSizeLog2),
			    (int)Math.floor(Math.log(dimsTemp[1])/Math.log(2)-maxSubResolutionImageSizeLog2)};
			levelMin=Math.max(dimsLog2[0], dimsLog2[1]);	
			System.out.println(TransformUtils.stringVectorN(dimsLog2, "dimsLog2"));
			if(levelMin>levelMax)levelMin=levelMax;
			setParameterValue("BM Subsample Z",0);
			setParameterValue("Level min",levelMin);
			setParameterValue("Level max",levelMax);
			setParameterValue("Higher accuracy",levelMin<1 ? 1 : 0);
		}
		int subFactorMin=(int)Math.round(Math.pow(2, -1+Math.max(1,levelMin)));
		int subFactorMax=(int)Math.round(Math.pow(2, -1+Math.max(1,levelMax)));
		int []targetDimsLevelMin=new int[] {dimsTemp[0]/subFactorMin,dimsTemp[1]/subFactorMin,dimsTemp[2]/(subZ ? subFactorMin : 1)};
		int []targetDimsLevelMax=new int[] {dimsTemp[0]/subFactorMax,dimsTemp[1]/subFactorMax,dimsTemp[2]/(subZ ? subFactorMin : 1)};
		System.out.println("Targets dims at level Min : "+TransformUtils.stringVectorN(targetDimsLevelMin,""));
		System.out.println("Targets dims at level Max : "+TransformUtils.stringVectorN(targetDimsLevelMax,""));

		int[]strides=new int[] { (int) Math.round(Math.max(1,Math.ceil(targetDimsLevelMin[0]/nbStrideAtMinLevel))),
								 (int) Math.round(Math.max(1,Math.ceil(targetDimsLevelMin[1]/nbStrideAtMinLevel))),
								 (int) Math.round(Math.max(strideMinZ,Math.ceil(targetDimsLevelMin[2]/nbStrideAtMinLevel))) };
		setParameterValue("BM Stride X",strides[0]);
		setParameterValue("BM Stride Y",strides[1]);
		setParameterValue("BM Stride Z",strides[2]);
		setParameterValue("BM BHS X",(int) Math.round(Math.max(strides[0],3)));
		setParameterValue("BM BHS Y",(int) Math.round(Math.max(strides[1],3)));
		setParameterValue("BM BHS Z",(int) Math.round(Math.max(strides[2],3)));
		if(dimsTemp[2]<5) {//cas 2D
			setParameterValue("BM Neigh Z",0);
			setParameterValue("BM BHS Z",0);
			setParameterValue("BM Stride Z",1);
		}
		this.maxAcceptableLevel=levelMax;
	}
	
	public void displayRegistrationparametersValues(String context) {
		System.out.println("Displaying parameters values "+context);
		for(int i=0;i<registrationParametersValues.length;i++)System.out.println(" - "+registrationParametersValues[i][0]+"="+registrationParametersValues[i][1]);
		System.out.println();
	}
	

	
	
	
	/** Helper functions
	 * 
	 */
	public boolean imageExists(int nt,int nm) {
		return this.paths[nt][nm]!=null;
	}
	
	public static void adjustImageOnScreen(ImagePlus img,int xPosition,int yPosition) {
		adjustFrameOnScreen(img.getWindow(), xPosition, yPosition);
	}

	public static void adjustFrameOnScreen(Frame frame,int xPosition,int yPosition) {
		int border=50;//taskbar, or things like this
        java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        int screenY=(int)Math.round(currentScreen.height);
        if(screenX>1920)screenX/=2;        
        java.awt.Dimension currentFrame=frame.getSize();
        int frameX=(int)Math.round(currentFrame.width);
        int frameY=(int)Math.round(currentFrame.height);
 
        int x=0;int y=0;
        if(xPosition==0)x=border;
        if(xPosition==1)x=(screenX-frameX)/2;
        if(xPosition==2)x=screenX-border-frameX;
        if(yPosition==0)y=border;
        if(yPosition==1)y=(screenY-frameY)/2;
        if(yPosition==2)y=screenY-border-frameY;
        frame.setLocation(x, y);
    }

	
	
	public static void adjustImageOnScreenRelative(ImagePlus img1,ImagePlus img2Reference,int xPosition,int yPosition) {
		adjustFrameOnScreenRelative(img1.getWindow(),img2Reference.getWindow(), xPosition, yPosition);
	}

	public static void adjustFrameOnScreenRelative(Frame frameToAdjust,Frame frameReference,int xPosition,int yPosition) {
        System.out.println("Adjusting image with frame");
        java.awt.Dimension currentScreen = Toolkit.getDefaultToolkit().getScreenSize();
        int screenX=(int)Math.round(currentScreen.width);
        int screenY=(int)Math.round(currentScreen.height);
        if(screenX>1920)screenX/=2;        

		int border=50;//taskbar, or things like this
		int x=0;int y=0;
        if(xPosition==0)x=frameReference.getLocationOnScreen().x-frameToAdjust.getSize().width-border;
        if(xPosition==2)x=frameReference.getLocationOnScreen().x+frameReference.getSize().width+border;
        if(yPosition==0)y=border;
        if(yPosition==1)y=(screenY-frameToAdjust.getSize().height)/2;
        if(yPosition==2)y=screenY-border-frameToAdjust.getSize().height;
        frameToAdjust.setLocation(x, y);
    }

	
	
	/** Setters/ getters for registration parameters
	 *  
	 */
	public int getParameterValue(String name) {
		for(int i=0;i<registrationParametersValues.length;i++)if(name.equals(registrationParametersValues[i][0]))return (int)registrationParametersValues[i][1];
		return 0;
	}
	public void setParameterValue(String name,int a) {
		for(int i=0;i<registrationParametersValues.length;i++)if(name.equals(registrationParametersValues[i][0]))registrationParametersValues[i][1]=a;
	}

	public double getParameterSigma() {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("Sigma dense"))return (double)registrationParametersValues[i][1];
		return 0;		
	}
	public void setParameterSigma(double sigma) {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("Sigma dense"))registrationParametersValues[i][1]=sigma;
	}

	public Transform3DType getParameterTransformType() {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("Type transform"))return (Transform3DType)registrationParametersValues[i][1];
		return null;		
	}
	public void setParameterTransformType(Transform3DType transform) {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("Type transform"))registrationParametersValues[i][1]=transform;
	}

	public OptimizerType getParameterOptimizerType() {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("Type optimizer"))return (OptimizerType)registrationParametersValues[i][1];
		return null;		
	}
	public void setParameterOptimizerType(OptimizerType opt) {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("Type optimizer"))registrationParametersValues[i][1]=opt;
	}
	
	public OptimizerType getParameterItkOptimizerType(){
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("ITK optimizer"))return (OptimizerType)registrationParametersValues[i][1];
		return null;
	}
	public void setParameterItkOptimizerType(OptimizerType opt) {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("ITK optimizer"))registrationParametersValues[i][1]=opt;
	}

	public double getParameterItkLearningRate() {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("ITK Learning rate"))return (double)registrationParametersValues[i][1];
		return 0;		
	}
	public void setParameterItkLearningRate(double learningRate) {
		for(int i=0;i<registrationParametersValues.length;i++)if(registrationParametersValues[i][0].equals("ITK Learning rate"))registrationParametersValues[i][1]=learningRate;
	}
	
	/** Launching interface, at the very start
	 * 
	 */
	public void startLaunchingInterface() {
		sos0Button=new JButton("Sos");
		runTwoImagesButton=new JButton("Register two images");
		runMultiModalButton=new JButton("Register multiple modalities");
		runMultiTimeButton=new JButton("Register multiple time");
		runMultiModalMultiTimeButton=new JButton("Register multiple modal multiple times");

		buttonPanel=new JPanel();
		buttonPanel.setLayout(new GridLayout(5,1,20,20));
		buttonPanel.add(sos0Button);
		buttonPanel.add(runTwoImagesButton);
		buttonPanel.add(runMultiModalButton);
		buttonPanel.add(runMultiTimeButton);
		buttonPanel.add(runMultiModalMultiTimeButton);
		add(buttonPanel);
		pack();
		actualizeLaunchingInterface(true);
		System.out.println("Launching interface done");

	}
	
	public void actualizeLaunchingInterface(boolean expectedState) {
		if(expectedState) {
			sos0Button.addMouseListener(this);
			runTwoImagesButton.addMouseListener(this);
			runMultiModalButton.addMouseListener(this);
			runMultiTimeButton.addMouseListener(this);
			runMultiModalMultiTimeButton.addMouseListener(this);
		}
		else {
			sos0Button.removeMouseListener(this);
			runTwoImagesButton.removeMouseListener(this);
			runMultiModalButton.removeMouseListener(this);
			runMultiTimeButton.removeMouseListener(this);
			runMultiModalMultiTimeButton.removeMouseListener(this);			
		}
		setVisible(true);
		repaint();		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getSource()==this.sos0Button)displaySosMessage(SOS_CONTEXT_LAUNCH);
		else if(e.getSource()==this.runTwoImagesButton) {
			String path1=VitiDialogs.chooseOneRoiPathUI("Select your reference image", "Select your reference image");
			String path2=VitiDialogs.chooseOneRoiPathUI("Select your reference image", "Select your reference image");
			this.setupTwoImagesRegistration(path1, path2);
			actualizeLaunchingInterface(false);
		}
	}

	
	/** advanced settings menu
	 */
	public void openSettingsDialog() {
		if(this.boxTypeAction.getSelectedIndex()!=1) {//BlockMatching parameters
	        GenericDialog gd= new GenericDialog("Settings for manual registration");
	        gd.addMessage("In most images, objects of interest are bright structures with an irregular surface,\n"+
	        		"surrounded with a dark background.\nUnder these assumption, manual registration can be done in the 3d viewer.\n"+
	        		"If your images are not of this kind, you should set the other option : Use 2d viewer");
	        gd.addChoice("Choose the most adapted viewer : ",new String[] {"3d viewer","2d viewer"}, this.kindOfViewer==VIEWER_3D ? "3d viewer" : "2d viewer");
	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        this.kindOfViewer=(gd.getNextChoiceIndex()==0) ? VIEWER_3D : VIEWER_2D;		
	        return;
		}
		String message="Successive subsampling factors used, from max to min"+"\n \n"+
				"The max level is the first level being processed, using the under-sampled image"+"\n"+
				"At this resolution, the step runs faster, and only uses the global shapes of structures\n"+
				"If the max level is too high, registration could diverge"+"\n"+
				"Conversely, the min level is the last one, slower, but more accurate\n"+"These two parameters have a dramatic effect on computation time and results accuracy";
			if(this.boxOptimizer.getSelectedIndex()==0) {//BlockMatching parameters
	        GenericDialog gd= new GenericDialog("Expert mode for Blockmatching registration");
	        String[]levelsMax=new String[maxAcceptableLevel];for(int i=0;i<maxAcceptableLevel;i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
	        gd.addMessage(message);
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[this.getParameterValue("Level max")-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[this.getParameterValue("Level min")-1]);
	        gd.addChoice("Higher accuracy (subpixellic level)", new String[] {"Yes","No"},this.getParameterValue("Higher accuracy")==1 ? "Yes":"No");
	        
	        gd.addMessage("Blocks dimensions. Blocks are image subparts\nCompared to establish correspondances between ref and mov images");
	        gd.addNumericField("Block half-size along X", this.getParameterValue("BM BHS X"), 0, 3, "original pixels");
	        gd.addNumericField("Block half-size along Y", this.getParameterValue("BM BHS Y"), 0, 3, "original pixels");
	        gd.addNumericField("Block half-size along Z", this.getParameterValue("BM BHS Z"), 0, 3, "original pixels");

	        gd.addMessage("Searching neighbourhood. Distance to look for\nbetween a reference image block and a moving image block");
	        gd.addNumericField("Block neighbourhood along X", this.getParameterValue("BM Neigh X"), 0, 3, "subsampled pixels");
	        gd.addNumericField("Block neighbourhood along Y", this.getParameterValue("BM Neigh Y"), 0, 3, "subsampled pixels");
	        gd.addNumericField("Block neighbourhood along Z",  this.getParameterValue("BM Neigh Z"), 0, 3, "subsampled pixels");

	        gd.addMessage("Spacing between two successive blocks\nalong each dimension");
	        gd.addNumericField("Striding along X", this.getParameterValue("BM Stride X"), 0, 3, "subsampled pixels");
	        gd.addNumericField("Strinding along Y",  this.getParameterValue("BM Stride Y"), 0, 3, "subsampled pixels");
	        gd.addNumericField("Strinding along Z",  this.getParameterValue("BM Stride Z"), 0, 3, "subsampled pixels");

	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  this.getParameterValue("Iterations BM"), 0, 3, "iterations");
	        if(this.boxTypeTrans.getSelectedIndex()==2)gd.addNumericField("Sigma for dense field smoothing", 1, 3, 6, unit);
	        gd.addNumericField("Percentage of blocks selected by score", this.getParameterValue("BM Select score"), 0, 3, "%");
	        if(this.boxTypeTrans.getSelectedIndex()!=2)gd.addNumericField("Percentage kept in Least-trimmed square", this.getParameterValue("BM Select LTS"), 0, 3, "%");	        

	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        System.out.println("Go");
	        int a=gd.getNextChoiceIndex()+1; this.setParameterValue("Level max", a);
	        System.out.println("a="+a);
	        int b=gd.getNextChoiceIndex()+1; b=b<a ? b : a; this.setParameterValue("Level min", b);
	        System.out.println("b="+b);
	        a=1-gd.getNextChoiceIndex();
	        System.out.println("Valeur actualisee pour higher accuracy="+a);
	        this.setParameterValue("Higher accuracy",a);
	       	
	       	int c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; this.setParameterValue("BM BHS X", c);
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; this.setParameterValue("BM BHS Y", c);
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; this.setParameterValue("BM BHS Z", c);

	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; this.setParameterValue("BM Neigh X", c);
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; this.setParameterValue("BM Neigh Y", c);
	       	c=(int)Math.round(gd.getNextNumber()); c=c<0 ? 0 : c; this.setParameterValue("BM Neigh Z", c);

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; this.setParameterValue("BM Stride X", c);
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; this.setParameterValue("BM Stride Y", c);
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; this.setParameterValue("BM Stride Z", c);

	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; this.setParameterValue("Iterations BM", c);
	       	if(this.boxTypeTrans.getSelectedIndex()==2) {double d=gd.getNextNumber(); d=d<1E-6 ? 1E-6 : d; this.setParameterSigma(d);}
	       	c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; this.setParameterValue("BM Select score", c);
	       	if(this.boxTypeTrans.getSelectedIndex()!=2) {c=(int)Math.round(gd.getNextNumber()); c=c<1 ? 1 : c; this.setParameterValue("BM Select LTS", c);}
		}
		else {//Itk parameters
	        GenericDialog gd= new GenericDialog("Expert mode for Itk registration");
	        String[]levelsMax=new String[maxAcceptableLevel];for(int i=0;i<maxAcceptableLevel;i++)levelsMax[i]=""+((int)Math.round(Math.pow(2, (i))))+"";
	        gd.addMessage(message);
	        gd.addChoice("Max subsampling factor (high=fast)",levelsMax, levelsMax[this.getParameterValue("Level max")-1]);
	        gd.addChoice("Min subsampling factor (low=slow)",levelsMax, levelsMax[this.getParameterValue("Level min")-1]);
	        
	        gd.addMessage("Others");
	        gd.addNumericField("Number of iterations per level",  this.getParameterValue("Iterations ITK"), 0, 5, "iterations");
	        gd.addNumericField("Learning rate",  this.getParameterItkLearningRate(), 4, 8, " no unit");

	        gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        int param1=gd.getNextChoiceIndex()+1; 
	        this.setParameterValue("Level max", param1);

	        int param2=gd.getNextChoiceIndex()+1; param2=param2<param1 ? param2 : param1;
	        this.setParameterValue("Level min", param2);
	       	
	       	int param3=(int)Math.round(gd.getNextNumber());
	       	param3=param3<0 ? 0 : param3; this.setParameterValue("Iterations ITK", param3);
	       	double param4=gd.getNextNumber();
	       	param4=param4<0 ? EPSILON : param4; this.setParameterItkLearningRate(param4);
		}		
	}
	


	
	/** Everything about help dialogs
	 * 
	 */
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

	public void runSos() {
		String saut="<div style=\"height:1px;display:block;\"> </div>";
		String startPar="<p  width=\"600\" >";
		String nextPar="</p><p  width=\"600\" >";
		String textToDisplay="";
		String axisText=
				"Axis alignment of the reference image onto the image XYZ axis, in order to set the object position in both results images that will be exported."+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				nextPar+
				"All along the registration procedure, only the moving image is moved."+
				"Thus, one could need the final result to be in a geometry that is optimal for biological analysis."+
				"For example, one could expect that the the axis of the object (a plant, a bone, ...) would be aligned with the Z axis (slices normal axis). "+
				"The axis alignment process opens a 3d interface, to interact with the reference image (shown as a red volume)"+
				" and set its alignment relative to the XYZ axis, drawn as white orthogonal lines "+
				"<b>Use the mouse to select (click) and move (click and drag)</b> the red volume or the 3d scene in order to align the red volume onto the grid :"+saut+
				" -> <b>Click on a volume</b> to select it, or on the background to select the 3d scene."+saut+
				" -> <b>Right-click and drag the red volume</b> to turn it,"+saut+
				" -> <b>right-click and drag the background</b> or the grid to turn around the 3d scene,"+saut+
				" -> <b>push shift key and right-click and drag</b> to translate the red volume or the 3d scene."+saut+saut+
				nextPar+" "+
				"Once done, click on the <b>\"Position ok\"</b> button to validate the alignment."+
				nextPar+" "+"The transform computed is then applied to the reference and moving images";
		String bmText="Automatic registration."+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				nextPar+
				"Automatic registration can be done using two different optimizers : "+
				" Block matching compares subparts of images to identify correspondances, and use these correpondances to align the moving image on the reference image ; "+
				" Itk Iconic optimizes a transform, and measure the evolution of image matching using a global statistical measure. Both use a pyramidal scheme, computing correspondances and transforms from a rough level (image subsampling=2^levelmax) to a precise level (image subsampling=2^levelmin)."+
				nextPar+
				"Automatic registration is an optimization process, its duration depends on the parameters. The estimated computation is given to help you"+
				"to define parameters according with the computation time you expect. The process is started when clicking on the <b>\"Start this action\"</b> button and finish when all the iterations are done at all levels. .";
				
		String manualText="Manual registration step."+""+
				"This operation can be done at any time, following or being followed by any registration operation. "+
				"The process can be interrupted, using the <b>\"Abort\"</b> button during the execution, or undone once finished, using the <b>\"Undo\"</b> button."+							
				nextPar+
				"This operation lets you roughly align the images manually, in order to handle transforms of great amplitude (angle > 15 degrees) that cannot be estimated automatically."+
				"To start this action, click on the <b>\"Start this action\"</b> button. A 3d interface opens, and let you interact with the images."+nextPar+
				"In the 3d interface, the reference image is shown as a green volume, locked to the 3d scene."+
				"The red volume is the moving image (free to move relatively to the red one and the 3d scene)."+
				"<b>Use the mouse to select (click) and move (click and drag)</b> the red volume or the 3d scene in order to align the red volume onto the green one :"+saut+
				" -> <b>Click on a volume</b> to select it, or on the background to select the 3d scene."+saut+
				" -> <b>Right-click and drag the red volume</b> to turn it,"+saut+
				" -> <b>right-click and drag the background</b> or the green volume to turn around the 3d scene,"+saut+
				" -> <b>push shift key and right-click and drag</b> to translate the red volume or the 3d scene."+saut+saut+
				nextPar+" "+
				"Once done, click on the <b>\"Position ok\"</b> button to validate the alignment."+
				nextPar+" ";
				
		
		disable(SOS);
		if(this.runButton.getText().equals("Start this action")) {//Nothing running
			textToDisplay="<html>"+startPar+"<b>Two images registration main window</b>"+""+nextPar+"The actual parameters are set to run the following operation :  "+nextPar;
			if(this.boxTypeAction.getSelectedIndex()==0)textToDisplay+=manualText;
			else if(this.boxTypeAction.getSelectedIndex()==1)textToDisplay+=bmText;
			else if(this.boxTypeAction.getSelectedIndex()==2) textToDisplay+=axisText;
		}
		else {
			if(this.boxTypeAction.getSelectedIndex()==0)textToDisplay="<html>"+startPar+"<b>Help during 3d manual registration</b>"+"<br/>"+""+nextPar+manualText;
			if(this.boxTypeAction.getSelectedIndex()==1)textToDisplay="<html>"+startPar+"<b>Help during automatic registration</b>"+"<br/>"+""+nextPar+bmText;
			if(this.boxTypeAction.getSelectedIndex()==2)textToDisplay="<html>"+startPar+"<b>Help during axis alignment registration</b>"+"<br/>"+""+nextPar+axisText;
		}
		textToDisplay+=nextPar+"  </p>";
		IJ.showMessage("Fijiyama contextual help", textToDisplay);
		enable(SOS);
	}
	
	
	
	
	/** unuseful listeners
	 * 
	 */
	@Override
	public void mouseReleased(MouseEvent e) {	}

	@Override
	public void mouseEntered(MouseEvent e) {	}

	@Override
	public void mouseExited(MouseEvent e) {	}

	@Override
	public void keyTyped(KeyEvent e) {	}

	@Override
	public void keyPressed(KeyEvent e) {	}

	@Override
	public void keyReleased(KeyEvent e) {	}

	@Override
	public void mousePressed(MouseEvent e) {	}


	

	

	
}

	
				

