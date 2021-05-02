package fr.cirad.image.fijiyama;

import java.awt.Color;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.ListModel;

import org.apache.commons.io.FileUtils;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;

import fr.cirad.image.fijiyama.Fijiyama_GUI;
import fr.cirad.image.fijiyama.RegistrationAction;

import fr.cirad.image.common.TransformUtils;
import fr.cirad.image.common.VitiDialogs;
import fr.cirad.image.common.VitimageUtils;
import fr.cirad.image.registration.BlockMatchingRegistration;
import fr.cirad.image.registration.ItkRegistration;
import fr.cirad.image.registration.ItkTransform;
import fr.cirad.image.registration.OptimizerType;
import fr.cirad.image.registration.Transform3DType;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageInfo;
import ij.plugin.Memory;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij3d.ImageJ3DViewer;
import ij3d.shortcuts.ShortCuts;
import math3d.Point3d;


public class RegistrationManager{
	ImagePlus maskImage=null;
	ImagePlus[][]maskImageArray=null;
	String pathToMask="None";
	int recursiveCalls=0;
	public int stepBuild=0;
	public int ENDING_STEP=10000;
	ArrayList<ArrayList<RegistrationAction> >interMods=new ArrayList<ArrayList<RegistrationAction> >();
	ArrayList<RegistrationAction>interTimes;
	private Fijiyama_GUI fijiyamaGui;
	public ij3d.Image3DUniverse universe;
	private int nbCpu=1;
	private int jvmMemory=1;
	private long memoryFullSize;
	private boolean oversizingDialogSeen=false;
	private boolean downSizingWhenNeeded=true;
	int estimatedTime=0;
	int estimatedGlobalTime=0;
	private int nbChannelsOfInputData=1;
	private int nbTimesOfInputData=1;

	private boolean first2dmessageHasBeenViewed=true;
	private boolean first3dmessageHasBeenViewed=false;
	
	public boolean isBionanoImagesWithCapillary;
	public boolean isSerie=false;
	public String serieOutputPath;
	private String serieInputPath;
	private String[] times=new String[] {"t0"};
	private String[] mods=new String[] {"ref","mov"};
	private String expression="img.tif";
	private String name;
	public int nTimes;
	public int nMods;
	public int referenceModality=0;
	public int referenceTime=0;
	public int refTime=0;
	public int refMod=0;
	public int movTime=0;
	public int movMod=1;
	private int step=0;
	private int nSteps=0;
	
	public String[][]paths;
	private int[][][]initDimensions;
	private int[][][]dimensions;
	private boolean isSubSampled[][];
	private double[][][][][]imageRanges;
	private double[][][]initVoxs;
	private double[][][]voxs;
	public ImagePlus[][]images;
	public double[][][]globalRanges;
	private double[][]imageSizes;//In Mvoxels
	private int maxImageSizeForRegistration=12;//In Mvoxels
	public String unit="mm";
	private int[]imageParameters=new int[] {512,512,40};
	public int maxAcceptableLevel=3;

	public ArrayList<ItkTransform>[][]transforms;
	ArrayList<ItkTransform> trActions;
	ArrayList<RegistrationAction>regActions;
	private RegistrationAction currentRegAction;
	private String stringSerie="Series";
	private String stringTwoImgs="TwoImgs";
	private int memoryFactorForGoodUX=100;//This is the maximum number of images that can be located in the RAM at the same time.
	//It Will be used to set the max image size, and decide wheter subsampling or not for computation
	private int[][] imageTypes;
	private LUT[][][] imageLuts;
	public boolean isGe3d=false;
	
	
	
	
	
	
	
	/*Constructor, creator, loader and setup to build a serie****************************************************************************/
	public RegistrationManager(Fijiyama_GUI fijiyamaGui) {
		this.fijiyamaGui=fijiyamaGui;
	}

	public boolean setupFromFjmFile(String pathToFjmFile) {
		System.out.println("Starting setup");
		unsetAllStructures();
		//Verify the configuration file, and detect registration style: serie or two images
		if(pathToFjmFile==null || (!(new File(pathToFjmFile).exists())) ||
				(!(pathToFjmFile.substring(pathToFjmFile.length()-4,pathToFjmFile.length())).equals(".fjm"))) {
			return false;
		}
		String []names = new String[] {new File(pathToFjmFile).getParent(),new File(pathToFjmFile).getName()};	
		this.name=names[1].substring(0,names[1].length()-4);
		this.serieOutputPath=names[0];

		
		String fjmFileContent=VitimageUtils.readStringFromFile(new File(names[0],names[1]).getAbsolutePath());
		if(fjmFileContent.split("\n")[2].split("=")[1].equals(stringSerie)) {
			isSerie=true;
			fijiyamaGui.modeWindow=Fijiyama_GUI.WINDOWSERIERUNNING;
		}
		else {
			isSerie=false;
			fijiyamaGui.modeWindow=Fijiyama_GUI.WINDOWTWOIMG;
		}

		//Get parameters from file
		String[]lines=fjmFileContent.split("\n");
		this.name=lines[1].split("=")[1];
		//this.serieOutputPath=(lines[3].split("=")[1]);
		this.serieInputPath=(lines[4].split("=")[1]);
		this.step=safeParsePositiveInt(lines[7].split("=")[1]);
		this.nSteps=safeParsePositiveInt(lines[8].split("=")[1]);
		
		if(! isSerie) {
			fijiyamaGui.mode=Fijiyama_GUI.MODE_TWO_IMAGES;
			this.nTimes=1;
			this.nMods=2;
			this.referenceTime=0;this.referenceModality=0;
			this.refTime=0;this.refMod=0;
			this.movTime=0;this.movMod=1;
			this.setupStructures();
			this.paths[0][0]=lines[5].split("=")[1];
			this.paths[0][1]=lines[6].split("=")[1];
			fijiyamaGui.modeWindow=Fijiyama_GUI.WINDOWTWOIMG;
		}
		else {
			fijiyamaGui.mode=Fijiyama_GUI.MODE_SERIE;
			fijiyamaGui.modeWindow=Fijiyama_GUI.WINDOWSERIERUNNING;
			this.referenceTime=safeParsePositiveInt(lines[5].split("=")[1]);
			this.referenceModality=safeParsePositiveInt(lines[6].split("=")[1]);
			//Read modalities
			this.nMods=safeParsePositiveInt(lines[10].split("=")[1]);
			if(this.nMods>1) {
				this.mods=new String[this.nMods];
				for(int i=0;i<this.nMods;i++)this.mods[i]=lines[11+i].split("=")[1];
			}
			
			//Read times and nSteps
			this.nTimes=safeParsePositiveInt(lines[12+this.nMods].split("=")[1]);
			if(this.nTimes>1) {
				this.times=new String[this.nTimes];
				for(int i=0;i<this.nTimes;i++) {
					this.times[i]=lines[13+this.nMods+i].split("=")[1];
				}
			}
			this.expression=lines[14+this.nMods+this.nTimes].split("=")[1];
			this.pathToMask=lines[15+this.nMods+this.nTimes].split("=")[1];
			if(this.pathToMask.equals("None"))this.maskImage=null;
			else this.maskImage=IJ.openImage(this.pathToMask);

			this.setupStructures();

			//Affecter les paths
			for(int nt=0;nt<this.nTimes;nt++) {
				for(int nm=0;nm<this.nMods;nm++) {
					File f=new File(this.serieInputPath,expression.replace("{Time}", times[nt]).replace("{ModalityName}",mods[nm]));
					IJ.log("Series images lookup : checking existence of image "+f.getAbsolutePath());
					if(f.exists()) {
						IJ.log("       Found.");
						this.paths[nt][nm]=f.getAbsolutePath();
					}
					else IJ.log("      Not found.");
				}
			}
			IJ.log("");
		}
	
		//Check computer capacity and get a working copy of images
		this.checkComputerCapacity(true);
		if(!this.openImagesAndCheckOversizing())return false;
		ItkTransform trTemp = null;
		
		//Read the steps : all RegistrationAction serialized .ser object, and associated transform for those already computed
		File dirReg=new File(this.serieOutputPath,"Registration_files");		
		for(int st=0;st<this.nSteps;st++) {
			File f=new File(dirReg,"RegistrationAction_Step_"+st+".ser");
			RegistrationAction regTemp=RegistrationAction.readFromTxtFile(f.getAbsolutePath());
			if(regTemp.isDone()) {
				f=new File(dirReg,"Transform_Step_"+st+(((regTemp.typeAction!=1) || (regTemp.typeTrans!=Transform3DType.DENSE))?".txt":".transform.tif"));
				IJ.log("Transformation lookup : "+f+" type "+regTemp.typeTrans);
				
				if(regTemp.typeTrans!=Transform3DType.DENSE || regTemp.typeAction!=1)trTemp=ItkTransform.readTransformFromFile(f.getAbsolutePath());
				else trTemp=ItkTransform.readAsDenseField(f.getAbsolutePath());				
			}
			addTransformAndActionBlindlyForBuilding(trTemp,regTemp);
		}
		currentRegAction=regActions.get(step);
		this.unit=this.images[this.referenceTime][this.referenceModality].getCalibration().getUnit();
		System.out.println("Finishing setup");
		return true;
	}
	
	public static boolean isInteger(String s) {
		try {
			int a=Integer.parseInt(s);	
		}catch (Exception e) {return false;}
		return true;
	}

	
	public static int safeParsePositiveInt(String s) {
		int ERR_CODE=-999999999;
		int ret=ERR_CODE;
		try {
			ret=Integer.parseInt(s);	
		}catch (Exception e) {int a=1;}
		if(ret!=ERR_CODE)return ret;
		
		ret=0;
		int nbChars=s.length();
		boolean isInteger=false;
		int index=0;
		while((index<(s.length()-1) && !isInteger(""+s.charAt(index)))){index++;}
		while((index<(s.length()-1) && isInteger(""+s.charAt(index)))){
			String s2=""+s.charAt(index);
			ret*=10;
			ret+=Integer.parseInt(s2);
			index++;
		}
		return ret;
	}
	
	
	public boolean setupFromTwoImages(String[]providedPaths) {	
		System.out.println("Starting from two imags");
		String[]paths=(providedPaths!=null ? providedPaths :  getRefAndMovPaths());
		if(paths==null)return false;
		if(providedPaths==null)if(!createOutputPathAndFjmFile())return false;//Else it is testing
		this.nTimes=1;
		this.nMods=2;
		this.referenceTime=0;this.referenceModality=0;
		this.refTime=0;this.refMod=0;
		this.movTime=0;this.movMod=1;
		System.out.println("Starting 2");
		
		setupStructures();
		this.paths[refTime][refMod]=paths[0];
		this.paths[movTime][movMod]=paths[1];
		checkComputerCapacity(true);
		if(!openImagesAndCheckOversizing())return false;
		addFirstActionOfPipeline();
		String fjmPath=saveSerieToFjmFile();
		unsetAllStructures();
		setupFromFjmFile(fjmPath);
		return true;
	}
	
	public void defineInputDataTesting(int testingCase,String testInputDir) {
		this.serieInputPath=testInputDir;
		if(testingCase==3) {
			this.expression="{ModalityName}.tif";
			this.times=new String[] {""};
			this.mods=new String[] {"PHOTO","MRI","RX"};
			this.nTimes=1;
			this.nMods=3;
			this.referenceTime=0;
			this.referenceModality=0;
			setupStructures();
		}
		if(testingCase==4) {
			this.expression="MRI_D{Time}.tif";
			this.times=new String[] {"0","1","2","3"};
			this.mods=new String[] {""};
			this.nTimes=4;
			this.nMods=1;
			this.referenceTime=0;
			this.referenceModality=0;
			setupStructures();
		}
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				File f=new File(this.serieInputPath,expression.replace("{Time}", times[nt]).replace("{ModalityName}",mods[nm]));
				if(f.exists()) {
					this.paths[nt][nm]=f.getAbsolutePath();
				}
			}
		}
	}
	
	public boolean startSetupSerieFromScratch(int testingCase,String testInputDir){
		if(testingCase==0) {
			if(!createOutputPathAndFjmFile())return false;
			if(!defineInputData())return false;
		}
		else {
			defineInputDataTesting(testingCase,testInputDir);
		}
		this.isSerie=true;
		checkComputerCapacity(false);
		if(!openImagesAndCheckOversizing())return false;
		this.stepBuild=0;
		defineSerieRegistrationPipeline("BEGIN");
		return true;
	}

	public void endSetupSerieFromScratch(){
		boolean serieGotDenseTransforms=false;
		for(int i=0;i<regActions.size();i++) {
			if(regActions.get(i).typeAction==RegistrationAction.TYPEACTION_AUTO && regActions.get(i).typeTrans==Transform3DType.DENSE)serieGotDenseTransforms=true;
		}
		if(serieGotDenseTransforms) setPathToMask();

		step=0;
		this.updateNbSteps();
	
		currentRegAction=regActions.get(0);
		String fjmFile=saveSerieToFjmFile();
		setupFromFjmFile(fjmFile);
		fijiyamaGui.modeWindow=Fijiyama_GUI.WINDOWSERIERUNNING;
		
		startTheSerie();
	}
	
	public void startTheSerie() {
		fijiyamaGui.startSerie();
		if(fijiyamaGui.frameLaunch!=null)fijiyamaGui.frameLaunch.setVisible(false);
		step=0;
		currentRegAction=regActions.get(step);
		this.updateNbSteps();
		fijiyamaGui.updateList();
	}
	
	public void setPathToMask() {
		if(VitiDialogs.getYesNoUI("Compute without mask ?","Fijiyama can prevent estimating deformations in interest area, to keep their shape untouched\nSelect \"Yes\" if you don't care (most common case), or \"No\" to provide a mask. "))pathToMask="None";
		else pathToMask=VitiDialogs.chooseOneRoiPathUI("Choose mask image","Choose mask image");
		if(pathToMask==null)pathToMask="None";
		setMaskImage();
	}
	
	public boolean setMaskImage() {
		if(pathToMask.equals("None")) {maskImage=null;return false;}
		else {maskImage=IJ.openImage(pathToMask);return true;}		
	}
	
	
	
	
	
	
	/*Setup helpers ********************************************************************************************************************/
	public String[] getRefAndMovPaths() {
		try {
			String pathToRef;
			try{pathToRef=VitiDialogs.openJFileUI("Choose a reference (fixed) image", "", "");} catch (Exception e) {return null;}
			if(pathToRef==null)return null;
			String dirRef=new File(pathToRef).getParent();
			VitimageUtils.waitFor(200);
			String pathToMov;
			try{pathToMov=VitiDialogs.openJFileUI("Choose a moving image to align with the reference image", dirRef, "");} catch (Exception e) {return null;}
			if( (pathToMov==null))return null;
			else return (new String[] {pathToRef,pathToMov}  );
		}
		catch (NoClassDefFoundError er) {IJ.showMessage("Warning : you haven't installed ImageJ-ITK as expected (see tutorial for more information)");System.exit(0);};
		return null;
	}
		
	public boolean createOutputPathAndFjmFile() {
		this.serieOutputPath=VitiDialogs.chooseDirectoryUI("Select an output directory for your work","Select an empty output directory to begin a new work");
		if(this.serieOutputPath==null) {IJ.showMessage("No output path given. Abort");return false ;}
		String[]files=new File(this.serieOutputPath).list();
		if(files.length!=0)  {
			IJ.showMessage("Directory already contains files. \nAn empty directory is needed to begin a new experiment\n(or select \"Open a previous study\" to go on an experiment)");
			if(VitiDialogs.getYesNoUI("Remove older contents ?", "Do you want to erase all the contents of this directory : \n\n"+this.serieOutputPath+"\n.\n!! Warning : this action cannot be undone !!")) {
				try {FileUtils.deleteDirectory(new File(this.serieOutputPath));} catch (IOException e) {e.printStackTrace();}
				new File(this.serieOutputPath).mkdir();
			}
			else {
				IJ.showMessage("Please select another directory");
				return createOutputPathAndFjmFile() ;
			}
		}
		new File(this.serieOutputPath,"Registration_files").mkdirs();
		new File(this.serieOutputPath,"Exported_data").mkdirs();	
		this.name="Fijiyama_series_"+new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date());
		this.name=VitiDialogs.getStringUI("Choose a name for your series (accepted characters : alphanumeric, underscore and minus)","Series name",this.name,true);
		IJ.showMessage("All files will be written in "+this.serieOutputPath+"\nConfiguration file="+this.name+".fjm \n");
		return true;
	}
	
	public boolean defineInputData() {
		this.expression="img_t{Time}_mod{ModalityName}.tif";
		String strTimes="1-3";
		String strMods="MRI;RX";
		this.serieInputPath=VitiDialogs.chooseDirectoryUI("Select an input directory containing 3d images","Select input dir...");
		if(this.serieInputPath==null){IJ.showMessage("Input path = null. Exit");return false;}
		//Get regular expression for image lookup
		if(!fijiyamaGui.getAutoRepMode()) {
			GenericDialog gd=new GenericDialog("Describe file names with a generic expression");
			gd.addMessage("Write observation times. If no multiple times, leave blank. Example : 1-5 or 10;33;78 ");
			gd.addStringField("Times=", strTimes, 40);
			gd.addMessage("");
			gd.addMessage("Write modalities. If no multiple modalities, leave blank. Example : RX;MRI;PHOTO ");
			gd.addStringField("Modalities=", strMods, 40);
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
		} catch(Exception e) {IJ.showMessage("Exception when reading parameters. Abort");e.printStackTrace();return false;}
		setupStructures();			
		int []numberPerTime=new int[this.nTimes];
		int []numberPerMod=new int[this.nMods];
		int nbTot=0;
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				File f=new File(this.serieInputPath,expression.replace("{Time}", times[nt]).replace("{ModalityName}",mods[nm]));
				if(f.exists()) {
					this.paths[nt][nm]=f.getAbsolutePath();
					numberPerTime[nt]++;
					numberPerMod[nm]++;
					nbTot++;
				}
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
		if(!fijiyamaGui.getAutoRepMode()) {
			GenericDialog gd2=new GenericDialog("Choose reference modality and reference time for registration");
			gd2.addMessage("This modality will be the reference image for each time-point. Choose the easiest to compare with all the other ones.");
			gd2.addMessage("The registration process is done with the dimensions of the reference image.\n--> If you choose a low resolution modality, registration can be unaccurate\n"+
					"--> If you choose a high resolution modality, it will be subsampled to prevent memory overflow");
			gd2.addMessage("");
			gd2.addMessage("After reference modality, you will have to choose the reference time. The first one is the default choice");
			gd2.addChoice("Reference modality", potentialRef, potentialRef[0]);
			gd2.addChoice("Reference time", this.times, this.times[0]);
			gd2.showDialog();
			if(gd2.wasCanceled()){IJ.showMessage("Dialog exited. Abort");return false;}
			int refInd=gd2.getNextChoiceIndex();
			int tt=gd2.getNextChoiceIndex();
			this.referenceTime=this.nTimes>1 ? tt : 0;
			this.referenceModality=potentialRefIndex[refInd];
		}		
		
		IJ.log("\nReference modality             : nb."+this.referenceModality+" = "+this.mods[this.referenceModality]);
		IJ.log("Reference time             : "+this.referenceTime);
		IJ.log("Series input path               : "+this.serieInputPath);
		IJ.log("Series output path              : "+this.serieOutputPath);
		IJ.log("Times                          : "+strTimes);
		IJ.log("Modalities                     : "+strMods);
		IJ.log("Expression                     : "+this.expression);
		IJ.log("Nb images detected             : "+nbTot);
		IJ.log("Nb registrations to compute    : "+(nbTot-1));
		IJ.log("\nEverything fine, then. Keep clicking !");
		return true;
	}

	public void defineSerieRegistrationPipeline(String s) {
		recursiveCalls++;
		if(recursiveCalls>100) 			{IJ.log("Recursive calls >100. There should be 50 modalities and 50 successive times, what seems really strange. Please send a mail to developers. Aborting procedure"+this.stepBuild+" from s");return;}

		if(this.stepBuild==0) {
			fijiyamaGui.modeWindow=Fijiyama_GUI.WINDOWSERIEPROGRAMMING;
			//ArrayList<ArrayList<RegistrationAction>>interMods=new ArrayList<ArrayList<RegistrationAction> >();
			if(this.nTimes<=1)this.stepBuild=3;
			else this.stepBuild=1;
		}
		
		if(this.stepBuild==1) {
			this.regActions=new ArrayList<RegistrationAction>();

			this.regActions.add(RegistrationAction.createRegistrationAction(
					images[referenceTime][referenceModality],images[referenceTime][referenceModality],
					this.fijiyamaGui,this,RegistrationAction.TYPEACTION_MAN));

			this.regActions.add(RegistrationAction.createRegistrationAction(
					images[referenceTime][referenceModality],images[referenceTime][referenceModality],
					this.fijiyamaGui,this,RegistrationAction.TYPEACTION_AUTO).setStepTo(1));

			this.currentRegAction=regActions.get(0);
			this.nSteps=2;
			fijiyamaGui.startRegistrationInterface();
			fijiyamaGui.interfaceIsRunning=true;
			this.currentRegAction=this.regActions.get(0);
			fijiyamaGui.updateBoxFieldsFromRegistrationAction(currentRegAction);
			fijiyamaGui.validatePipelineButton.setText("Approve inter-time pipeline");

			if(!fijiyamaGui.getAutoRepMode())IJ.showMessage("Define registration pipeline to align images of the reference modality between two successive times.\n"+
			"Click on an action in the bottom list, and modify it using the menus. Click on remove to delete the selected action, \n"+
			"and click on add action to insert a new registration action just before the cursor\n\nOnce done, click on Validate pipeline.");
			
			this.stepBuild++;
			return;
		}

		if(this.stepBuild==2) {		
			interTimes=this.regActions;
			if(fijiyamaGui.developerMode)printRegActions(" comIntertime pipeline", interTimes);
			fijiyamaGui.registrationFrame.setVisible(false);
			VitimageUtils.waitFor(150);
			fijiyamaGui.registrationFrame.setVisible(true);
			this.stepBuild=3;
			defineSerieRegistrationPipeline(s+"parent stepBuild="+this.stepBuild+" interTimeValidated ");
			return;
		}
		
		
		//Define inter-mods pipeline, if needed
		if(this.stepBuild<ENDING_STEP && this.nMods<2) {
			this.stepBuild=ENDING_STEP;defineSerieRegistrationPipeline(s+"parent stepBuild="+this.stepBuild+" No mods here");return;
		}
		if(this.stepBuild<ENDING_STEP && this.stepBuild>=3 && ((this.stepBuild%2)==1)){
			int curMod=(this.stepBuild-3)/2;
			if(curMod>=this.nMods) {this.stepBuild=ENDING_STEP;defineSerieRegistrationPipeline(s+"parent stepBuild="+this.stepBuild+" curMod>=this.nMods");return;}
			if(curMod == referenceModality) { interMods.add(null);this.stepBuild+=2;defineSerieRegistrationPipeline(s+"parent stepBuild="+this.stepBuild+" curMod==referenceModality");return;}
			else {
				this.regActions=new ArrayList<RegistrationAction>();
				this.regActions.add(RegistrationAction.createRegistrationAction(
						images[referenceTime][referenceModality],images[referenceTime][referenceModality],
						this.fijiyamaGui,this,RegistrationAction.TYPEACTION_MAN));
				this.regActions.add(RegistrationAction.createRegistrationAction(
						images[referenceTime][referenceModality],images[referenceTime][referenceModality],
						this.fijiyamaGui,this,RegistrationAction.TYPEACTION_AUTO).setStepTo(1));
				this.currentRegAction=this.regActions.get(0);
				System.out.println("THERE 1: "+this.currentRegAction.fullLengthDescription());
				this.nSteps=2;
				if(!fijiyamaGui.interfaceIsRunning) {fijiyamaGui.startRegistrationInterface();fijiyamaGui.interfaceIsRunning=true;}
				else {
					fijiyamaGui.registrationFrame.setVisible(false);
					VitimageUtils.waitFor(150);
					fijiyamaGui.registrationFrame.setVisible(true);
				}
				System.out.println("THERE 2: "+this.currentRegAction.fullLengthDescription());
				fijiyamaGui.updateBoxFieldsFromRegistrationAction(currentRegAction);
				if(!fijiyamaGui.getAutoRepMode()) {
					VitiDialogs.getYesNoUI("","Define registration pipeline to align "+this.mods[referenceModality]+" with "+this.mods[curMod]+" from the same timepoint.\n"+
						"Click on an action in the bottom list, and modify it using the menus. Click on remove to delete the selected action, \n"+
						"and click on add action to insert a new registration action just before the cursor\n\nOnce done, click on Validate pipeline.");
					
				}
				System.out.println("THERE 3: "+this.currentRegAction.fullLengthDescription());
				fijiyamaGui.validatePipelineButton.setText("Approve "+this.mods[curMod]+"->"+this.mods[referenceModality]+" pipeline");
				if(!fijiyamaGui.validatePipelineButton.isEnabled())fijiyamaGui.validatePipelineButton.setEnabled(true);
			}
			this.stepBuild++;
			VitimageUtils.waitFor(150);
			System.out.println("THERE 4: "+this.currentRegAction.fullLengthDescription());
			return;
		}
				
		//Define inter-mods pipeline, if needed
		if(this.stepBuild<ENDING_STEP && this.stepBuild>=3 && this.stepBuild%2==0){
			int curMod=(this.stepBuild-3)/2;
			interMods.add(this.regActions);
			if(fijiyamaGui.developerMode)printRegActions("Intermod "+curMod+" pipeline", interMods.get(interMods.size()-1));
			this.stepBuild++;
			defineSerieRegistrationPipeline(s+"parent stepBuild="+this.stepBuild+" validated interMod ");
		}

		
		
		if(this.stepBuild==ENDING_STEP) {		
			this.stepBuild++;
			fijiyamaGui.registrationFrame.setVisible(false);
			fijiyamaGui.registrationFrame.dispose();
			sequenceStepsOfTheCreatedPipeline(interTimes, interMods);
			endSetupSerieFromScratch();
		}
	}
	
	public void sequenceStepsOfTheCreatedPipeline(ArrayList<RegistrationAction>interTimes,ArrayList<ArrayList<RegistrationAction> >interMods) {
		System.out.println("Sequencing start");

	
		this.step=-1;
		this.regActions=new ArrayList<RegistrationAction>();
		int[]typeSuccessives=new int[] {0,1,1,1};//Manual actions, followed by automatic rigid action, followed by automatic sim and dense actions
		Transform3DType[][]transTypeSuccessives=new Transform3DType[][] {{Transform3DType.RIGID,Transform3DType.SIMILARITY},{Transform3DType.RIGID,Transform3DType.RIGID},{Transform3DType.SIMILARITY,Transform3DType.SIMILARITY},{Transform3DType.DENSE,Transform3DType.DENSE}};//Manual actions, followed by automatic rigid action, followed by automatic sim and dense actions
		int nType=typeSuccessives.length;
		boolean[]thereIsSome=new boolean[nType];
		int []numberPhase=new int[nType];
		
		//Inventory of number for each category
		System.out.println("Here 2");
		for(int n=0;n<nType;n++) {
			for(int i=0;interTimes!=null && i<interTimes.size();i++) {
				RegistrationAction reg=interTimes.get(i);
				Transform3DType typ1=transTypeSuccessives[n][0];
				Transform3DType typ2=transTypeSuccessives[n][1];
				if(reg.typeAction==typeSuccessives[n] && (reg.typeTrans==typ1 || reg.typeTrans==typ2))  thereIsSome[n]=true;
			
			}
			for(int i=0;interMods!=null && i<interMods.size();i++) {
				if(interMods.get(i)==null || interMods.get(i).size()==0)continue;
				for(int j=0;j<interMods.get(i).size();j++) {
					RegistrationAction reg=interMods.get(i).get(j);
					Transform3DType typ1=transTypeSuccessives[n][0];
					Transform3DType typ2=transTypeSuccessives[n][1];
					if(reg.typeAction==typeSuccessives[n] && (reg.typeTrans==typ1 || reg.typeTrans==typ2))thereIsSome[n]=true;
				}
			}
		}

		for(int n=0;n<nType;n++) System.out.println(thereIsSome[n]);
		//Detect phase number for each, and which one is final step
		int lastFoundIndex=0;
		for(int n=nType-1;(n>=0) && (lastFoundIndex==0) ;n--) {
			if(thereIsSome[n])lastFoundIndex=n;
		}
		int incr=1;
		for(int n=0;n<nType;n++) {
			if(thereIsSome[n])numberPhase[n]=incr++;
		}
		//numberPhase[lastFoundIndex]+=8;
		int delta;
		for(int ind =0;ind<typeSuccessives.length;ind++) {
			int type=typeSuccessives[ind];
			Transform3DType typ1=transTypeSuccessives[ind][0];
			Transform3DType typ2=transTypeSuccessives[ind][1];
			delta=0;
			for(int tRef=0;tRef<this.nTimes-1;tRef++) {
				if(tRef<this.referenceTime) {
					refTime=tRef+1;refMod=referenceModality;
					movTime=tRef;movMod=referenceModality;
				}
				else {
						refTime=tRef;refMod=referenceModality;
					movTime=tRef+1;movMod=referenceModality;
				}
				for(int regSt=0;regSt<interTimes.size();regSt++) {
					if(interTimes.get(regSt).typeAction==type && (interTimes.get(regSt).typeTrans==typ1 || interTimes.get(regSt).typeTrans==typ2)) {
						this.step++;
						regActions.add(RegistrationAction.copyWithModifiedElements(interTimes.get(regSt),refTime,refMod,movTime,movMod,step));
						delta++;
					}
				}
			}
			for(int tRef=0;tRef<this.nTimes;tRef++) {
				refTime=tRef;refMod=referenceModality;
				movTime=tRef;
				for(int mMov=0;mMov<this.nMods;mMov++) {
					movMod=mMov;
					if(refMod != movMod && imageExists(movTime, movMod)) {
						for(int regSt=0;regSt<interMods.get(movMod).size();regSt++) {
							if(interMods.get(movMod).get(regSt).typeAction==type && (interMods.get(movMod).get(regSt).typeTrans==typ1 || interMods.get(movMod).get(regSt).typeTrans==typ2)) {
								this.step++;
								regActions.add(RegistrationAction.copyWithModifiedElements(interMods.get(movMod).get(regSt),refTime,refMod,movTime,movMod,step));
								delta++;
							}
						}
					}
				}
			}
			if(thereIsSome[ind] && delta>0) {
				this.step++;
				regActions.add(new RegistrationAction().setActionTo(RegistrationAction.TYPEACTION_SAVE).setStepTo(step));
				this.step++;
				RegistrationAction reg=new RegistrationAction().setActionTo(RegistrationAction.TYPEACTION_EXPORT).setStepTo(step);				
				reg.paramOpt=numberPhase[ind];
				regActions.add(reg);
			}
		}

		this.step++;
		regActions.add(RegistrationAction.copyWithModifiedElements(RegistrationAction.copyWithModifiedElements(regActions.get(0),refTime,refMod,movTime,movMod,step),referenceTime,referenceModality,referenceTime,referenceModality,step).setActionTo(RegistrationAction.TYPEACTION_ALIGN));
		this.step++;
		RegistrationAction reg=RegistrationAction.copyWithModifiedElements(RegistrationAction.copyWithModifiedElements(regActions.get(0),refTime,refMod,movTime,movMod,step),referenceTime,referenceModality,referenceTime,referenceModality,step).setActionTo(RegistrationAction.TYPEACTION_EXPORT);
		reg.paramOpt=8;
		regActions.add(reg);
		if(fijiyamaGui.developerMode)printRegActions("Global pipeline at export",regActions);
		this.nSteps=this.step;
		this.step=0;
		System.out.println("Sequencing end");
	}
	
	@SuppressWarnings("unchecked")
	public void setupStructures() {
		regActions=new ArrayList<RegistrationAction>();
		trActions=new ArrayList<ItkTransform>();
		this.globalRanges=new double[this.nTimes][this.nMods][2];
		this.images=new ImagePlus[this.nTimes][this.nMods];
		this.isSubSampled=new boolean[this.nTimes][this.nMods];
		this.paths=new String[this.nTimes][this.nMods];
		this.initDimensions=new int[this.nTimes][nMods][5];
		this.dimensions=new int[this.nTimes][this.nMods][3];
		this.initVoxs=new double[this.nTimes][this.nMods][3];
		this.imageSizes=new double[this.nTimes][this.nMods];
		this.imageRanges=new double[this.nTimes][this.nMods][this.nbTimesOfInputData][this.nbChannelsOfInputData][2];
		this.imageTypes=new int[this.nTimes][this.nMods];
		this.imageLuts=new LUT[this.nTimes][this.nMods][];
		this.voxs=new double[this.nTimes][this.nMods][3];	
		this.transforms=(ArrayList<ItkTransform>[][])new ArrayList[this.nTimes][this.nMods];
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				this.transforms[nt][nm]=new ArrayList<ItkTransform>();
			}
		}
	}

	public int valueAllTypes() {
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				if(imageTypes[nt][nm]!=imageTypes[this.referenceTime][this.referenceModality])return -1;
			}
		}
		return imageTypes[this.referenceTime][this.referenceModality];
	}
	
	public void unsetAllStructures() {
		if(regActions!=null)for(int i=0;i<regActions.size();i++)regActions.set(i,null);
		regActions=null;
		trActions=null;
		this.images=null;
		this.globalRanges=null;
		this.isSubSampled=null;
		this.paths=null;
		this.initDimensions=null;
		this.dimensions=null;
		this.initVoxs=null;
		this.imageSizes=null;
		this.imageRanges=null;
		this.voxs=null;
		if(transforms!=null) {
			for(int nt=0;nt<this.transforms.length;nt++) {		
				for(int nm=0;nm<this.transforms[nt].length;nm++) {
					this.transforms[nt][nm]=null;
				}
			}
		}
		this.transforms=null;
		this.nTimes=0;
		this.nMods=0;
		this.referenceModality=0;
		this.referenceTime=0;
		this.refTime=0;
		this.refMod=0;
		this.movTime=0;
		this.movMod=1;
		this.step=0;
		this.nSteps=0;
	}
	

	public String osIndependantPath(String s) {
		return s.replace("\\\\", "\\").replace("\\", "\\\\");
	}
	
	public boolean openImagesAndCheckOversizing() {
		String recapMain="There is oversized images, which can lead to very slow computation, or memory overflow.\n"+
				"Your computer capability has been detected to :\n"+
				" -> "+this.jvmMemory+" MB of RAM allowed for this plugin"+"\n -> "+this.nbCpu+" parallel threads for computation."+
				"\nWith this configuration, the suggested maximal image size is set to "+this.maxImageSizeForRegistration+" Mvoxels";
		recapMain+=" but one of your images (at least) is bigger.\nThis setup can lead to memory issues"+
				" as registration is memory/computation intensive. \n.\nProposed solution : Fijiyama can use a subsampling strategy in order to compute registration on smaller images\n"+
				"Therefore, final resampled images will be computed using the initial data, to provide high-quality results\n\n"+
				"Do you agree that Fijiyama subsample data before registration ?";
		IJ.log("Checking odversized images. Limit="+this.maxImageSizeForRegistration);
		recapMain+="\n.\n.\nSummary of image sizes :\n";
		String recap="";
		ImagePlus img;
		boolean thereIsBigImages=false;
		int factor=1;
		boolean initialImageHyperDimsDefined=false;
		int mInit=0;
		int tInit=0;
		boolean rxDetected=false;
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
					if(this.transforms[nt][nm]==null)this.transforms[nt][nm]=new ArrayList<ItkTransform>();//If it is not the case, it is a startup from a file
					File f=new File(this.paths[nt][nm]);
					if(f.isFile()) {System.out.println("Le fichier existe");}
					else {System.out.println("Le fichier n' existe pas");}
					ImagePlus imgTemp=IJ.openImage(osIndependantPath(this.paths[nt][nm]));

					if(detectRX(imgTemp))rxDetected=true;
					if( ! isBionanoImagesWithCapillary && (VitimageUtils.isBionanoImageWithCapillary(imgTemp)) ) {
						isBionanoImagesWithCapillary=true;
						this.maskImageArray=new ImagePlus[this.nTimes][this.nMods];
					}
					if( ! isGe3d && (VitimageUtils.isGe3d(imgTemp)) ) {
						isGe3d=true;
					}
					this.imageTypes[nt][nm]=imgTemp.getType();
					if(initialImageHyperDimsDefined) {
						if((this.nbChannelsOfInputData != imgTemp.getNChannels()) || (this.nbTimesOfInputData != imgTemp.getNFrames())) {
							this.nbChannelsOfInputData=Math.max(this.nbChannelsOfInputData,imgTemp.getNChannels());
							this.imageRanges=new double[this.nTimes][this.nMods][this.nbTimesOfInputData][this.nbChannelsOfInputData][2];
							IJ.showMessage("Warning","Warning : hyperdimensions does not match between\n"+
									"First images and "+
									"current image : nb channels="+imgTemp.getNChannels()+" , nt times="+imgTemp.getNFrames()+"\n"+
									"-> From path : "+this.paths[nt][nm]+"\n.\n. Chosen number channels is set to max between both : "+this.nbChannelsOfInputData+".\n.\n"
									+"Another way to register images that does not have the same hyperdimensions,\n"+
									"is to use the tool duplicate (CTRL+MAJ+D in ImageJ) to prepare data with same hyperdimensions\n"+
									"Then register it, and after, use the Fijiyama tool \"Apply transformation\"\n"+
									" to use the resulting transformations you computed");							
						}
					}
					else {
						this.nbChannelsOfInputData=imgTemp.getNChannels();
						this.nbTimesOfInputData=imgTemp.getNFrames();
						this.imageRanges=new double[this.nTimes][this.nMods][this.nbTimesOfInputData][this.nbChannelsOfInputData][2];
						initialImageHyperDimsDefined=true;
						mInit=nm;
						tInit=nt;
						isBionanoImagesWithCapillary=VitimageUtils.isBionanoImageWithCapillary(imgTemp);
					}
					if(nt==this.referenceTime && nm==this.referenceModality)this.unit=imgTemp.getCalibration().getUnit();
					initDimensions[nt][nm]=VitimageUtils.getDimensionsXYZCT(imgTemp);
					dimensions[nt][nm]=VitimageUtils.getDimensions(imgTemp);
					initVoxs[nt][nm]=VitimageUtils.getVoxelSizes(imgTemp);
					voxs[nt][nm]=VitimageUtils.getVoxelSizes(imgTemp);
					imageSizes[nt][nm]=VitimageUtils.dou(((1.0*initDimensions[nt][nm][0]*initDimensions[nt][nm][1]*initDimensions[nt][nm][2])/(1024.0*1024.0)));
					if(imageSizes[nt][nm]>this.maxImageSizeForRegistration) {
						thereIsBigImages=true;
						this.isSubSampled[nt][nm]=true;
					}
					else this.isSubSampled[nt][nm]=false;

				}
			}
		}
		if(!thereIsBigImages) {
			for(int nt=0;nt<this.nTimes;nt++) {
				for(int nm=0;nm<this.nMods;nm++) {
					if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
						img=IJ.openImage(this.paths[nt][nm]);
						this.imageLuts[nt][nm]=img.getLuts();
						if(img.getType()==ImagePlus.COLOR_RGB)IJ.run(img,"8-bit","");
						for(int nt2=0;nt2<this.nbTimesOfInputData;nt2++) {
							for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {									
								img.setC(Math.min(img.getNChannels(), nm2+1));
								img.setT(nt2+1);
								
								this.imageRanges[nt][nm][nt2][nm2]=VitimageUtils.getDoubleSidedRangeForContrastMoreIntelligent(img, Math.min(img.getNChannels(), nm2+1)-1, nt2, img.getNSlices()/2, Fijiyama_GUI.percentileDisplay,Fijiyama_GUI.widthRangeDisplay);
//								this.imageRanges[nt][nm][nt2][nm2][0]=img.getDisplayRangeMin();
//								this.imageRanges[nt][nm][nt2][nm2][1]=img.getDisplayRangeMax();			
								//IJ.log("No big hyperimgs On a eu :"+TransformUtils.stringVectorN(this.imageRanges[nt][nm][nt2][nm2], ""));
							}
						}
						if(isBionanoImagesWithCapillary) {
							if(img.getNChannels()>=4 && (img.getStack().getSliceLabel(VitimageUtils.getCorrespondingSliceInHyperImage(img, 3, 0, 0)).contains("MASKMAP")) ){ 
								this.maskImageArray[nt][nm]=new Duplicator().run(img,4,4,1,this.initDimensions[nt][nm][2],1,1);
								this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], 2,10E8);
								this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],1, -1, true);
								this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],2, -1, true);
							}
							else {
								this.maskImageArray[nt][nm]=new Duplicator().run(img,1,1,1,this.initDimensions[nt][nm][2],1,1);
								this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], -10E8,10E8);
							}
						}
						int can=VitimageUtils.getChannelOfMaxT1MinT2Sequence(img);
						img=new Duplicator().run(img,can+1,can+1,1,this.initDimensions[nt][nm][2],1,1);
						this.images[nt][nm]=img;
						this.images[nt][nm].setDisplayRange(this.imageRanges[nt][nm][0][can][0],this.imageRanges[nt][nm][0][can][1]);
						this.globalRanges[nt][nm][0]=this.imageRanges[nt][nm][0][can][0];
						this.globalRanges[nt][nm][1]=this.imageRanges[nt][nm][0][can][1];
						this.isSubSampled[nt][nm]=false;
					}
				}
			}
		}
		else{
			
			recap=recapMain;
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

			if(!oversizingDialogSeen) {
				downSizingWhenNeeded=VitiDialogs.getYesNoUI("Warning : image(s) oversized", recap);
				oversizingDialogSeen=true;
			}
			if(downSizingWhenNeeded) {
				IJ.log("Running resize before start");
				for(int nt=0;nt<this.nTimes;nt++) {
					for(int nm=0;nm<this.nMods;nm++) {
						if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
							IJ.log("Processing image "+nt+"-"+nm+" . Initial voxel size="+VitimageUtils.dou(this.initVoxs[nt][nm][0])+"x"+VitimageUtils.dou(this.initVoxs[nt][nm][1])+"x"+VitimageUtils.dou(this.initVoxs[nt][nm][0])+" and dims="+this.dimensions[nt][nm][0]+"x"+this.dimensions[nt][nm][1]+"x"+this.dimensions[nt][nm][2]);
							if(imageSizes[nt][nm]<=this.maxImageSizeForRegistration) {
								img=IJ.openImage(this.paths[nt][nm]);
								this.imageLuts[nt][nm]=img.getLuts();
								if(img.getType()==ImagePlus.COLOR_RGB)IJ.run(img,"8-bit","");
								for(int nt2=0;nt2<this.nbTimesOfInputData;nt2++) {
									for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {									
										img.setC(Math.min(img.getNChannels(),nm2+1));
										img.setT(nt2+1);
										this.imageRanges[nt][nm][nt2][nm2]=VitimageUtils.getDoubleSidedRangeForContrastMoreIntelligent(img, Math.min(img.getNChannels(), nm2+1)-1, nt2, img.getNSlices()/2, Fijiyama_GUI.percentileDisplay,Fijiyama_GUI.widthRangeDisplay);
										//this.imageRanges[nt][nm][nt2][nm2][0]=img.getDisplayRangeMin();
										//this.imageRanges[nt][nm][nt2][nm2][1]=img.getDisplayRangeMax();									
									}
								}
								int can=VitimageUtils.getChannelOfMaxT1MinT2Sequence(img);
								this.images[nt][nm]=new Duplicator().run(img,can+1,can+1,1,img.getNSlices(),1,1);
								this.globalRanges[nt][nm][0]=this.imageRanges[nt][nm][0][can][0];
								this.globalRanges[nt][nm][1]=this.imageRanges[nt][nm][0][can][1];
								
								if(isBionanoImagesWithCapillary) {
									if(img.getNChannels()>=4 && (img.getStack().getSliceLabel(VitimageUtils.getCorrespondingSliceInHyperImage(img, 3, 0, 0)).contains("MASKMAP")) ){ 
										this.maskImageArray[nt][nm]=new Duplicator().run(img,4,4,1,img.getNSlices(),1,1);
										this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], 2,10E8);
										this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],1, -1, true);
										this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],2, -1, true);
									}
									else {
										this.maskImageArray[nt][nm]=new Duplicator().run(img,1,1,1,img.getNSlices(),1,1);
										this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], -10E8,10E8);
									}
								}

								
								
								this.isSubSampled[nt][nm]=false;
								IJ.log("   -> target voxel size="+VitimageUtils.dou(this.initVoxs[nt][nm][0])+"x"+VitimageUtils.dou(this.initVoxs[nt][nm][1])+"x"+VitimageUtils.dou(this.initVoxs[nt][nm][0])+" and dims="+this.dimensions[nt][nm][0]+"x"+this.dimensions[nt][nm][1]+"x"+this.dimensions[nt][nm][2]);
							}
							else {
								this.isSubSampled[nt][nm]=true;
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
								img=IJ.openImage(this.paths[nt][nm]);
								this.imageLuts[nt][nm]=img.getLuts();
								if(img.getType()==ImagePlus.COLOR_RGB)IJ.run(img,"8-bit","");
								for(int nt2=0;nt2<this.nbTimesOfInputData;nt2++) {
									for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {									
										img.setC(Math.min(img.getNChannels(),nm2+1));
										img.setT(nt2+1);
										this.imageRanges[nt][nm][nt2][nm2]=VitimageUtils.getDoubleSidedRangeForContrastMoreIntelligent(img, Math.min(img.getNChannels(), nm2+1)-1, nt2, img.getNSlices()/2, Fijiyama_GUI.percentileDisplay,Fijiyama_GUI.widthRangeDisplay);
										//this.imageRanges[nt][nm][nt2][nm2][0]=img.getDisplayRangeMin();
										//this.imageRanges[nt][nm][nt2][nm2][1]=img.getDisplayRangeMax();									
										IJ.log("Big imgs On a eu :"+TransformUtils.stringVectorN(this.imageRanges[nt][nm][nt2][nm2], ""));
									}
								}

								if(isBionanoImagesWithCapillary) {
									if(img.getNChannels()>=4 && (img.getStack().getSliceLabel(VitimageUtils.getCorrespondingSliceInHyperImage(img, 3, 0, 0)).contains("MASKMAP")) ){ 
										this.maskImageArray[nt][nm]=new Duplicator().run(img,4,4,1,this.initDimensions[nt][nm][2],1,1);
										this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], 2,10E8);
										this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],1, -1, true);
										this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],2, -1, true);
									}
									else {
										this.maskImageArray[nt][nm]=new Duplicator().run(img,1,1,1,this.initDimensions[nt][nm][2],1,1);
										this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], -10E8,10E8);
										
									}
								}
								
								
								
								int can=VitimageUtils.getChannelOfMaxT1MinT2Sequence(img);
								img=new Duplicator().run(img,can+1,can+1,1,this.initDimensions[nt][nm][2],1,1);
								this.images[nt][nm]=ItkTransform.resampleImage(this.dimensions[nt][nm], this.voxs[nt][nm], img,false);
								this.images[nt][nm].setDisplayRange(this.imageRanges[nt][nm][0][can][0],this.imageRanges[nt][nm][0][can][1]);
								this.globalRanges[nt][nm][0]=this.imageRanges[nt][nm][0][can][0];
								this.globalRanges[nt][nm][1]=this.imageRanges[nt][nm][0][can][1];

								IJ.log("   -> target voxel size="+VitimageUtils.dou(this.voxs[nt][nm][0])+"x"+VitimageUtils.dou(this.voxs[nt][nm][1])+"x"+VitimageUtils.dou(this.voxs[nt][nm][0])+" and dims="+this.dimensions[nt][nm][0]+"x"+this.dimensions[nt][nm][1]+"x"+this.dimensions[nt][nm][2]);
							}	
						}
					}
				}
			}
			else {
				IJ.showMessage("Forcing ignoring oversize of images. Yolo mode. Trying to read images.");
				for(int nt=0;nt<this.nTimes;nt++) {
					for(int nm=0;nm<this.nMods;nm++) {
						if(this.paths[nt][nm]!=null) {//There is an image to process for this modality/time
							this.images[nt][nm]=IJ.openImage(this.paths[nt][nm]);
							this.isSubSampled[nt][nm]=false;
							this.imageLuts[nt][nm]=this.images[nt][nm].getLuts();
							if(this.images[nt][nm].getType()==ImagePlus.COLOR_RGB)IJ.run(this.images[nt][nm],"8-bit","");
							for(int nt2=0;nt2<this.nbTimesOfInputData;nt2++) {
								for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {									
									this.images[nt][nm].setC(Math.min(this.images[nt][nm].getNChannels(), nm2+1));
									this.images[nt][nm].setT(nt2+1);
									this.imageRanges[nt][nm][nt2][nm2]=VitimageUtils.getDoubleSidedRangeForContrastMoreIntelligent(this.images[nt][nm], Math.min(this.images[nt][nm].getNChannels(), nm2+1)-1, nt2, this.images[nt][nm].getNSlices()/2,Fijiyama_GUI.percentileDisplay,Fijiyama_GUI.widthRangeDisplay);
									//this.imageRanges[nt][nm][nt2][nm2][0]=this.images[nt][nm].getDisplayRangeMin();
									//this.imageRanges[nt][nm][nt2][nm2][1]=this.images[nt][nm].getDisplayRangeMax();									
								}
							}
		
							
							if(isBionanoImagesWithCapillary) {
								if(this.images[nt][nm].getNChannels()>=4 && (this.images[nt][nm].getStack().getSliceLabel(VitimageUtils.getCorrespondingSliceInHyperImage(this.images[nt][nm], 3, 0, 0)).contains("MASKMAP")) ){ 
									this.maskImageArray[nt][nm]=new Duplicator().run(this.images[nt][nm],4,4,1,this.initDimensions[nt][nm][2],1,1);
									this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], 2,10E8);
									this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],1, -1, true);
									this.maskImageArray[nt][nm]=VitimageUtils.makeOperationOnOneImage(this.maskImageArray[nt][nm],2, -1, true);
								}
								else {
									this.maskImageArray[nt][nm]=new Duplicator().run(this.images[nt][nm],1,1,1,this.initDimensions[nt][nm][2],1,1);
									this.maskImageArray[nt][nm]=VitimageUtils.getFloatBinaryMask(this.maskImageArray[nt][nm], -10E8,10E8);
									
								}
							}
							
							
							
							
							int can=VitimageUtils.getChannelOfMaxT1MinT2Sequence(this.images[nt][nm]);

							this.images[nt][nm]=new Duplicator().run(this.images[nt][nm],can+1,can+1,1,this.initDimensions[nt][nm][2],1,1);
							this.images[nt][nm].setDisplayRange(this.imageRanges[nt][nm][can][0][0],this.imageRanges[nt][nm][can][0][1]);
							this.globalRanges[nt][nm][0]=this.imageRanges[nt][nm][0][can][0];
							this.globalRanges[nt][nm][1]=this.imageRanges[nt][nm][0][can][1];
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
		fijiyamaGui.viewSlice=images[referenceTime][referenceModality].getNSlices()/2+1;

		
		IJ.log("-----------------------------\nSummary of image importation\n-----------------------------\n");
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				if(!imageExists(nt,nm))IJ.log("\nTime "+nt+" Mod "+nm+" : no image");
				else {
					IJ.log("\nTime "+nt+" Mod "+nm+" : image exists. ");
					IJ.log(" - Initial image dimensions : "+TransformUtils.stringVectorN(this.initDimensions[nt][nm],""));
					IJ.log(" - Image size="+imageSizes[nt][nm]+" Mvoxels");
					IJ.log(" - Used image in registration : "+VitimageUtils.imageResume(this.images[nt][nm]));
					if(this.nbChannelsOfInputData*this.nbTimesOfInputData==1)	IJ.log(" - Detected ranges for canals and times : c0t0 :"+TransformUtils.stringVectorN(this.imageRanges[nt][nm][0][0],""));
					else {
						IJ.log(" - Detected ranges for canals and times ");
						for(int nt2=0;nt2<this.nbTimesOfInputData;nt2++) {
							for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {
								IJ.log("   .....c"+nm2+"t"+nt2+" : "+TransformUtils.stringVectorN(this.imageRanges[nt][nm][nt2][nm2],""));
							}
						}
					}
				}				
			}
		}
		IJ.log("-----------------------------\n");
		
		if(rxDetected)isBionanoImagesWithCapillary=false;
		return true;
	}

	public void addFirstActionOfPipeline() {
		step=0;
		nSteps=1;
		currentRegAction=new RegistrationAction(fijiyamaGui,this);
		currentRegAction.defineSettingsFromTwoImages(this.images[refTime][refMod],this.images[movTime][movMod],this,true);
		regActions.add(currentRegAction);
	}
	
	
	
	public boolean detectRX(ImagePlus img) {
		if(img==null)return false;
		if(img.getStack()==null) return false;
		if(img.getStack().getSliceLabel(1)==null)return false;
		if(img.getStack().getSliceLabel(1).length()<6)return false;		
		return(img.getStack().getSliceLabel(1).substring(0, 6).equals("slice0"));		
	}
	
	
	/* Instance Export-Save-Load routines**********************************************************************************************/
	public String saveSerieToFjmFile() {		
		//Read informations from ijm file : main fields
		String str="";
		str+="#Fijiyama save###\n";
		str+="#Name="+this.name+"\n";
		str+="#Mode="+this.getModeString()+"\n";
		str+="#Output path="+this.serieOutputPath+"\n";
		str+="#Input path="+this.serieInputPath +"\n";
		if(! isSerie) {
			str+="#PathToRef="+this.paths[0][0]+"\n";
			str+="#PathToMov="+this.paths[0][1]+"\n";
		}
		else{
			str+="#Reference Time="+this.referenceTime+"\n";
			str+="#Reference Modality="+this.referenceModality +"\n";
		}
		if(fijiyamaGui.modeWindow==Fijiyama_GUI.WINDOWSERIEPROGRAMMING) {
			str+="#Step="+ (Math.min(this.getNbSteps()-1, this.step))+"\n";
		}
		else if(fijiyamaGui.modeWindow==Fijiyama_GUI.WINDOWSERIERUNNING) {
			if(this.step>0)str+="#Step="+ (Math.min(this.getNbSteps()-1, this.step))+"\n";
			else str+="#Step="+ (Math.min(this.getNbSteps()-1, this.step+1))+"\n";		
		}
		else {
			str+="#Step="+ (Math.min(this.getNbSteps()-1, this.step+ 1))+"\n";
		}
		str+="#NbSteps="+ this.nSteps+"\n";
		str+="#"+""+"\n";
		if(isSerie) {			
			str+="#Nmods="+this.nMods+"\n";
			for(int i=0;i<nMods;i++)str+="#Mod"+i+"="+this.mods[i]+"\n";
			str+="#"+""+"\n";
			str+="#Ntimes="+this.nTimes+"\n";
			for(int i=0;i<nTimes;i++)str+="#Time"+i+"="+this.times[i]+"\n";
			str+="#"+"" +"\n";
			str+="#Expression="+this.expression +"\n";
			str+="#PathToMask="+pathToMask+"\n";
			str+="#"+"" +"\n";
		}

		//All RegistrationAction serialized .ser object, and associated transform for those already computed
		File dirReg=new File(this.serieOutputPath,"Registration_files");
		for(int st=0;st<this.nSteps;st++) {
			RegistrationAction regTemp=this.regActions.get(st);
			regTemp.writeToTxtFile(new File(dirReg,"RegistrationAction_Step_"+st+".ser").getAbsolutePath());
			str+="#"+regTemp.readableString()+"\n";
			if(st<this.step) {
				File f=new File(dirReg,"Transform_Step_"+st+".txt");
				trActions.get(st).writeToFileWithTypeDetection(f.getAbsolutePath(), this.images[regTemp.refTime][regTemp.refMod]);
			}
		}
		str+="#"+""+"\n";
		VitimageUtils.writeStringInFile(str, new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath());
		return (new File(this.serieOutputPath,this.name+".fjm").getAbsolutePath());
	}

	public void exportImagesAndComposedTransforms(double typeExport) {
		int val=(int)Math.round(typeExport);
		//Ask for target dimensions
		ImagePlus referenceGeometryForTransforms;
		//ItkTransform transformAlignementRef=(transforms[referenceTime][referenceModality].size()>0) ? getComposedTransform(referenceTime, referenceModality) : new ItkTransform();
		//ItkTransform trTemp;
		
		GenericDialog gd=new GenericDialog("Choose target dimensions...");
		gd.addMessage("This operation will export all images in the same geometry, and combine it in a 4D/5D hyperimage");
		gd.addMessage("The export geometry has to be chosen. A common choice is to export both in the geometry of the reference image.");
		gd.addMessage("Another choice (up to you) is to define custom dimensions and voxel sizes for the target geometry");
		gd.addMessage("Reference image dimensions are : "+this.initDimensions[refTime][refMod][0]+" x "+this.initDimensions[refTime][refMod][1]+" x "+this.initDimensions[refTime][refMod][2]+" x "+this.initDimensions[refTime][refMod][3]+" x "+this.initDimensions[refTime][refMod][4]+
				" with voxel size "+VitimageUtils.dou(this.initVoxs[refTime][refMod][0],6)+" x "+VitimageUtils.dou(this.initVoxs[refTime][refMod][1],6)+" x "+VitimageUtils.dou(this.initVoxs[refTime][refMod][2],6));
		gd.addMessage("\nChoose the target dimensions to export both images");
		gd.addMessage("If you feel lost, please choose the first option : Dimensions of reference image.\n.\n.");
		gd.addChoice("Choose target dimensions", new String[] {"Dimensions of reference image","Provide custom dimensions"}, "Dimensions of reference image");

		int choice=0;
		if(val>=8) {
			gd.showDialog();
	        if (gd.wasCanceled()) return;	        
	        choice=gd.getNextChoiceIndex();
		}
		
		if(choice==0) {
			referenceGeometryForTransforms=IJ.openImage(this.paths[referenceTime][referenceModality]);
			int nZ=referenceGeometryForTransforms.getNSlices();
			referenceGeometryForTransforms=new Duplicator().run(referenceGeometryForTransforms,1,1,1,nZ,1,1);
		}
		else {
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
	        referenceGeometryForTransforms=new ItkTransform().transformImage(outputDimensions, outputVoxs,this.images[refTime][refMod], false);			
		}

		String s=(val>=8) ? "_FINAL" : ("_After_Phase_"+(val%8));
		boolean saveIndividualImages=(val>=8);
		getViewOfImagesTransformedAndSuperposedSerieWithThisReference(referenceGeometryForTransforms,saveIndividualImages,true,s);
		if(val>=8)IJ.showMessage("The combined hyperimage, and the individual registered images are exported in the target directory : \n"+(new File(this.serieOutputPath,"Exported_data").getAbsolutePath())+"\n");
//		else IJ.showMessage("The intermediary combined hyperimage (code="+s+" is exported in the target directory : \n"+(new File(this.serieOutputPath,"Exported_data").getAbsolutePath())+"\n");
	}
		

	
	
	
	
	
	/*Current action lifecycling ********************************************************************************************************************/	
	public void startCurrentAction() {
	}
	
	public void abortCurrentAction() {
		if(! currentRegAction.isAbortable())return;
		
	}
		
	public void finishCurrentAction(ItkTransform tr) {
		if(! addTransformToAction(tr,currentRegAction,currentRegAction.movTime,currentRegAction.movMod))return;//End of serie
		try{switchToFollowingAction();}catch(Exception e) {e.printStackTrace();}
		fijiyamaGui.addLog("Action finished",0);
		fijiyamaGui.addLog("",0);
		if(fijiyamaGui.modeWindow==Fijiyama_GUI.MODE_TWO_IMAGES )fijiyamaGui.addLog("Waiting for you to "+"set and start next action : Step "+currentRegAction.step,0);
		else fijiyamaGui.addLog("Waiting for you to "+ "start"+" next action : "+currentRegAction.readableString(false),0);
		fijiyamaGui.updateList();
		fijiyamaGui.updateEstimatedTime();
	}
		
	public RegistrationAction switchToFollowingAction(){
		if(this.step==regActions.size()-1) {//Si c est la derniere action
			if(fijiyamaGui.modeWindow==Fijiyama_GUI.WINDOWSERIERUNNING) {
				fijiyamaGui.serieIsFinished();
				fijiyamaGui.unpassThrough("End of pipeline ! Congratulations.");
				return null;
			}
			else{
				currentRegAction=new RegistrationAction(currentRegAction).setStepTo(++step);
				this.regActions.add(currentRegAction);
				if(!currentRegAction.isTransformationAction())currentRegAction.typeAction=0;
				fijiyamaGui.updateBoxFieldsFromRegistrationAction(currentRegAction);
				this.nSteps++;
				return currentRegAction;
			}
		}
		else {
			this.step++;
			currentRegAction=regActions.get(step);
			if(currentRegAction.isTransformationAction() && (!isSerie))fijiyamaGui.updateBoxFieldsFromRegistrationAction(currentRegAction);
			return currentRegAction;
		}
	}
	
	public RegistrationAction	removeLastAction() {
		if(regActions.size()>1) {
			regActions.remove(regActions.size()-1);
			this.nSteps=regActions.size();
			this.step--;
			this.currentRegAction=regActions.get(regActions.size()-1);
		}
		return this.currentRegAction;

	}
	
	
	
	
	
	
	/*Lifecycling helpers*****************************************************************************************************************************/
	public boolean addTransformToAction(ItkTransform tr,RegistrationAction reg,int nt,int nm) {
		currentRegAction.setDone();
		tr.step=this.step;
		this.transforms[nt][nm].add(tr);
		this.trActions.add(tr);
		if(fijiyamaGui.isRunningSerie()){
			if(regActions.size()==(step+1)) {
				fijiyamaGui.serieIsFinished();
			return false;

			}
			else currentRegAction=regActions.get(step+1);
		}
		return true;
	}
	
	public void addTransformAndActionBlindlyForBuilding(ItkTransform tr,RegistrationAction reg) {
		if(!reg.isDone()) {
			regActions.add(reg);
		}
		else {			
			if(reg.getStep()>=this.step)IJ.showMessage("Bad adding transform to current pile !");
			regActions.add(reg);
			tr.step=reg.getStep();
			trActions.add(tr);
			transforms[reg.movTime][reg.movMod].add(tr);
		}
	}

	public void undoLastActionInSerieContext() {
		if(step==0)return;
		int nt=regActions.get(step-1).movTime;
		int nm=regActions.get(step-1).movMod;
		int index=this.transforms[nt][nm].size()-1;
		this.transforms[nt][nm].remove(index);
		this.trActions.remove(step-1);
		step--;
		this.currentRegAction=this.regActions.get(step);
		this.currentRegAction.setUndone();
		fijiyamaGui.updateList();
	}
					
	public void undoLastActionInTwoImagesContext() {
		if(step==0)return;
		int nt=regActions.get(step-1).movTime;
		int nm=regActions.get(step-1).movMod;
		int index=this.transforms[nt][nm].size()-1;
		this.transforms[nt][nm].remove(index);
		this.trActions.remove(step-1);
		this.regActions.remove(this.regActions.size()-1);
		this.currentRegAction=this.regActions.get(this.regActions.size()-1);
		step--;
		nSteps--;
		ImagePlus imgToClose= WindowManager.getImage(Fijiyama_GUI.getImgViewText(step+1)) ;
		if(imgToClose!=null){
			imgToClose.changes=false;
			imgToClose.close();
		}
		fijiyamaGui.imgView=WindowManager.getImage(Fijiyama_GUI.getImgViewText(step));
		if(fijiyamaGui.imgView==null)fijiyamaGui.updateView();
		currentRegAction.setUndone();
		fijiyamaGui.updateBoxFieldsFromRegistrationAction(this.currentRegAction);
	}
				
	public void defineDefaultSettingsForCurrentAction(){
		currentRegAction.defineSettingsFromTwoImages(images[currentRegAction.refTime][currentRegAction.refMod],images[currentRegAction.movTime][currentRegAction.movMod],this,true);	
	}

	public void changeCurrentAction(int stepClicked) {
		this.currentRegAction=regActions.get(stepClicked);
		this.step=stepClicked;
	}
		


/*	
	shortcut.File\ >\ Quit=ctrl pressed W
			shortcut.View\ >\ Fullscreen=ctrl pressed F
			#shortcut.View\ >\ Set\ view\ >\ +\ YZ=pressed NUMPAD4
			Show_Local_Coordinate_System_When_Adding_Content=false
			Startup_Width=512
			shortcut.Edit\ >\ Delete=pressed DELETE
			Show_Global_Coordinate_System=true
			Background=[0.0,0.0,0.0]
	*/
	
	
	
	/*Manual registration routines ********************************************************************************************************************/	
	public void start3dManualRegistration(ImagePlus imgRef,ImagePlus imgMov) {
		ImagePlus refCopy=VitimageUtils.imageCopy(imgRef);
		VitimageUtils.adjustContrast3d(refCopy, Fijiyama_GUI.percentileDisplay,Fijiyama_GUI.widthRangeDisplay);
		IJ.run(refCopy,"8-bit","");
		this.universe=new ij3d.Image3DUniverse();
		universe.show();
		ShortCuts sho=universe.getShortcuts();
		sho.clearShortCut("View > Set view > + YZ");
		String sentence="";
		if(imgMov!=null) {
			ImagePlus movCopy=VitimageUtils.imageCopy(imgMov);		
			VitimageUtils.adjustContrast3d(movCopy, Fijiyama_GUI.percentileDisplay,Fijiyama_GUI.widthRangeDisplay);
			IJ.run(movCopy,"8-bit","");
			universe.removeAllContents();
			universe.addContent(refCopy, new Color3f(Color.red),"refCopy",50,new boolean[] {true,true,true},1,0 );
			universe.addContent(movCopy, new Color3f(Color.green),"movCopy",50,new boolean[] {true,true,true},1,0 );
			sentence="Move the green volume (moving image) to match the red one (reference image).";
		}
		else {
			ImagePlus movCopy=VitimageUtils.getBinaryGrid(refCopy,17,true,false);
			universe.removeAllContents();
			universe.addContent(refCopy, new Color3f(Color.green),"movCopy",50,new boolean[] {true,true,true},1,0 );
			universe.addOrthoslice(movCopy, new Color3f(Color.white),"refCopy",50,new boolean[] {true,true,true},1);
			sentence="Move the red volume (reference image) to fit with the white lines (image axis).";
		}
		ij3d.ImageJ3DViewer.select("movCopy");
		universe.getSelected().showCoordinateSystem(true);
		universe.getSelected().setThreshold(80);
		ij3d.ImageJ3DViewer.select("refCopy");
		universe.getSelected().showCoordinateSystem(true);
		universe.getSelected().setThreshold(80);
		if(fijiyamaGui.isRunningTwoImagesTraining()) {
			universe.setSize(fijiyamaGui.lastViewSizes[0], fijiyamaGui.lastViewSizes[1]);
			VitimageUtils.adjustFrameOnScreenRelative((Frame)((JPanel)(this.universe.getCanvas().getParent())).getParent().getParent().getParent(),fijiyamaGui.imgView.getWindow(),1,1,10);
		}
		else VitimageUtils.adjustFrameOnScreenRelative((Frame)((JPanel)(this.universe.getCanvas().getParent())).getParent().getParent().getParent(),
				fijiyamaGui.registrationFrame,fijiyamaGui.getRelativeOptimalPositionFor2DView(),0,10);
		if(!fijiyamaGui.getAutoRepMode() && (!first3dmessageHasBeenViewed)) {
			String mess=sentence+"\nWhen done, push the \""+fijiyamaGui.getRunButtonText()+"\" button to stop.\n.\nControls : \n";
			mess+="- Rotations : Mouse-drag the background to turn the scene, and mouse-drag an object to turn it.\n"+
					  "For accurate rotations, use the arrows (numpad 7 & numpad 9  for X axis, right & left for Y axis, numpad 1 & numpad 3 for Z axis)\n";
			if(VitimageUtils.isWindowsOS()) {
				mess+="- Translations :  use the numerical keypad (4 & 6 for X axis, 2 & 8 for Y axis, 0 & 5 for Z axis)";						
			}
			else {
				mess+="- Translations : hold the SHIFT key and drag an object to translate it\n"+
				"For accurate translations, use the numerical keypad (4 & 6 for X axis, 2 & 8 for Y axis, 0 & 5 for Z axis)\n";				
			}
			mess+="- To zoom / unzoom, scroll with the mouse or use pageup and pagedown";
			IJ.showMessage(mess);			
			}

		first3dmessageHasBeenViewed=true;
		fijiyamaGui.addLog(" Waiting for you to confirm position or to abort action...",0);
	}
	
	public ItkTransform finish3dManualRegistration(){
		Transform3D tr=new Transform3D();
		double[]tab=new double[16];
		//Collect transform applied to mov
		universe.getContent("movCopy").getLocalRotate().getTransform(tr);		tr.get(tab);
		ItkTransform itRot=ItkTransform.array16ElementsToItkTransform(tab);
		universe.getContent("movCopy").getLocalTranslate().getTransform(tr);		tr.get(tab);
		ItkTransform itTrans=ItkTransform.array16ElementsToItkTransform(tab);
		itTrans.addTransform(itRot);
		itTrans=itTrans.simplify();
	ItkTransform trMov=new ItkTransform(itTrans.getInverse());

		//Collect transform applied to ref
		tr=new Transform3D();
		tab=new double[16];
		universe.getContent("refCopy").getLocalRotate().getTransform(tr);		tr.get(tab);
		itRot=ItkTransform.array16ElementsToItkTransform(tab);
		universe.getContent("refCopy").getLocalTranslate().getTransform(tr);		tr.get(tab);
		itTrans=ItkTransform.array16ElementsToItkTransform(tab);
		itTrans.addTransform(itRot);
		
		itTrans=itTrans.simplify();
		ItkTransform trRef=new ItkTransform(itTrans);
		
		trMov.addTransform(trRef);
		trMov=trMov.simplify();
		
		
		universe.removeAllContents();
		universe.close();
    	universe=null;    
        fijiyamaGui.addKeyListener(IJ.getInstance());
    	return trMov;		
	}
	
	public void start2dManualRegistration(ImagePlus imgRef,ImagePlus imgMov) {
		ImagePlus refCopy=VitimageUtils.imageCopy(imgRef);
		ImagePlus movCopy=null;
		IJ.run(refCopy,"8-bit","");
		refCopy.setTitle(fijiyamaGui.displayedNameImage1);
		if(imgMov!=null) {
			movCopy=VitimageUtils.imageCopy(imgMov);		
			IJ.run(movCopy,"8-bit","");
			movCopy.setTitle(fijiyamaGui.displayedNameImage2);
		}
		else {
			movCopy=VitimageUtils.getBinaryGrid(refCopy,17);
			movCopy.setTitle(fijiyamaGui.displayedNameImage2);
		}
		IJ.setTool("point");
		refCopy.show();refCopy.setSlice(refCopy.getNSlices()/2+1);refCopy.updateAndRepaintWindow();
		VitimageUtils.adjustFrameOnScreen((Frame)WindowManager.getWindow(fijiyamaGui.displayedNameImage1), 0,2);
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		VitimageUtils.adjustFrameOnScreenRelative((Frame)rm,(Frame)WindowManager.getWindow(fijiyamaGui.displayedNameImage1),2,1,2);
		movCopy.show();movCopy.setSlice(movCopy.getNSlices()/2+1);movCopy.updateAndRepaintWindow();
		VitimageUtils.adjustFrameOnScreenRelative((Frame)WindowManager.getWindow(fijiyamaGui.displayedNameImage2),rm,2,2,2);
		if(!fijiyamaGui.getAutoRepMode() && first2dmessageHasBeenViewed)IJ.showMessage("Examine images, identify correspondances between images and use the Roi manager to build a list of corresponding points. Points should be given this way : \n- Point A  in image 1\n- Correspondant of point A  in image 2\n- Point B  in image 1\n- Correspondant of point B  in image 2\netc...\n"+
		"Once done (at least 5-15 couples of corresponding points), push the \""+fijiyamaGui.getRunButtonText()+"\" button to stop\n\n");
		first2dmessageHasBeenViewed=false;
		fijiyamaGui.addLog(" Waiting for you to confirm position or to abort action...",0);
	}
	
	public ItkTransform finish2dManualRegistration(){
		RoiManager rm=RoiManager.getRoiManager();
		ImagePlus imgTemp=WindowManager.getImage(fijiyamaGui.displayedNameImage1).duplicate();
		Point3d[][]pointTab=convertLandmarksToPoints(WindowManager.getImage(fijiyamaGui.displayedNameImage1),WindowManager.getImage(fijiyamaGui.displayedNameImage2),true);
		for(int i=0;i<pointTab.length;i++) {
			for(int j=0;j<pointTab[i].length;j++) {
				System.out.println("Point "+i+" "+j+" : "+pointTab[i][j].x+","+pointTab[i][j].y+","+pointTab[i][j].z);
			}
		}
		WindowManager.getImage(fijiyamaGui.displayedNameImage1).changes=false;
		WindowManager.getImage(fijiyamaGui.displayedNameImage2).changes=false;
		WindowManager.getImage(fijiyamaGui.displayedNameImage1).close();
		WindowManager.getImage(fijiyamaGui.displayedNameImage2).close();
		ItkTransform trans=null;
		if(currentRegAction.typeAction==RegistrationAction.TYPEACTION_MAN) {
			if(currentRegAction.typeTrans==Transform3DType.RIGID)trans=ItkTransform.estimateBestRigid3D(pointTab[1],pointTab[0]);
			else if(currentRegAction.typeTrans==Transform3DType.SIMILARITY)trans=ItkTransform.estimateBestSimilarity3D(pointTab[1],pointTab[0]);
			else if(currentRegAction.typeTrans==Transform3DType.DENSE)trans=pointTab[0].length==0 ? new ItkTransform() : ItkTransform.estimateBestDense3D(pointTab[1],pointTab[0],imgTemp,currentRegAction.sigmaDense);
		}
		else if(currentRegAction.typeAction==RegistrationAction.TYPEACTION_ALIGN) {
			if(currentRegAction.typeTrans==Transform3DType.RIGID)trans=ItkTransform.estimateBestRigid3D(pointTab[0],pointTab[1]);
			else if(currentRegAction.typeTrans==Transform3DType.SIMILARITY)trans=ItkTransform.estimateBestSimilarity3D(pointTab[0],pointTab[1]);
		}
		rm.close();
		System.out.println("Obtained matrix : "+trans.drawableString());
		return trans;		
	}
	
	public ItkTransform finish2dEvaluation(){
		RoiManager rm=RoiManager.getRoiManager();
		Point3d[][]pointTabImg=convertLandmarksToPoints(WindowManager.getImage(fijiyamaGui.displayedNameImage1),WindowManager.getImage(fijiyamaGui.displayedNameImage2),false);
		double []voxSizesReg=VitimageUtils.getVoxelSizes(getCurrentRefImage());
		double []voxSizesEvaluate=VitimageUtils.getVoxelSizes(WindowManager.getImage(fijiyamaGui.displayedNameImage1));
		WindowManager.getImage(fijiyamaGui.displayedNameImage1).changes=false;
		WindowManager.getImage(fijiyamaGui.displayedNameImage2).changes=false;
		WindowManager.getImage(fijiyamaGui.displayedNameImage1).close();
		WindowManager.getImage(fijiyamaGui.displayedNameImage2).close();
		rm.close();

		boolean threeFoldEvaluation=false;//(VitiDialogs.getYesNoUI("Compare with last manual step ?", "Compare with last manual step  ? (\"No\" means evaluating only last step)"));//false
		String nameMeasureTxt=new File(this.serieOutputPath,"measurements"+new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date())+".csv").getAbsolutePath();
		computeMismatches(new Point3d[][] {pointTabImg[0],pointTabImg[1]},voxSizesReg,voxSizesEvaluate,nameMeasureTxt);

		if(threeFoldEvaluation) {
			int lastMan=0;
			for(int s=0;s<this.step;s++)if(regActions.get(s).isDone() && regActions.get(s).typeAction==RegistrationAction.TYPEACTION_MAN)lastMan=s;
			ItkTransform trTemp=new ItkTransform();	
			for(int indT=lastMan+1;indT<this.step;indT++) {
				trTemp.addTransform(this.transforms[currentRegAction.movTime][currentRegAction.movMod].get(indT));
			}
			for(int i=0;i<pointTabImg[1].length;i++) {
				pointTabImg[1][i]=VitimageUtils.toRealSpace(pointTabImg[1][i],voxSizesEvaluate);
				pointTabImg[1][i]=trTemp.transformPoint(pointTabImg[1][i]);
				pointTabImg[1][i]=VitimageUtils.toImageSpace(pointTabImg[1][i],voxSizesEvaluate);
			}
			
			nameMeasureTxt=new File(this.serieOutputPath,"measurements"+new SimpleDateFormat("yyyy-MM-dd_hh-mm").format(new Date())+"_Second.csv").getAbsolutePath();
			computeMismatches(new Point3d[][] {pointTabImg[0],pointTabImg[1]},voxSizesReg,voxSizesEvaluate,nameMeasureTxt);
		}
		return new ItkTransform();
	}
	
	public void computeMismatches(Point3d[][]pointTabImg,double[]voxSizesReg,double[]voxSizesEvaluate,String nameFile) {		
		IJ.log("Registration evaluation : computing distance between corresponding points.");
		int nCouples=pointTabImg[0].length;
		double[][]dataExport=new double[nCouples][3+3+3+3+1];//coordIntRef, coordIntMov,DistanceImg,distanceReal,GlobDistReal
		double[][]dataStats=new double[4][3+3+3+3+1];//coordIntRef, coordIntMov,DistanceImg,distanceReal,GlobDistReal
//		double[]data;
		for(int i=0;i<nCouples;i++) {
			//Coordinates of reference point
			dataExport[i][0]=pointTabImg[0][i].x;
			dataExport[i][1]=pointTabImg[0][i].y;
			dataExport[i][2]=pointTabImg[0][i].z;

			//Coordinates of corresponding point in moving image
			dataExport[i][3+0]=pointTabImg[1][i].x;
			dataExport[i][3+1]=pointTabImg[1][i].y;
			dataExport[i][3+2]=pointTabImg[1][i].z;
	
			//Distance in voxels in the registration image space and in real space (unit), along each dimension
			for(int dim=0;dim<3;dim++) {
				dataExport[i][6+dim]= Math.abs(  dataExport[i][0+dim] - dataExport[i][3+dim])*voxSizesReg[dim]/voxSizesEvaluate[dim];
				dataExport[i][9+dim]= dataExport[i][6+dim]*voxSizesEvaluate[dim];
			}			
			dataExport[i][12]=Math.sqrt(dataExport[i][9]*dataExport[i][9] + dataExport[i][9+1]*dataExport[i][9+1] + dataExport[i][9+2]*dataExport[i][9+2]);
		}

		double[][]transposedData=VitimageUtils.transposeTab(dataExport);
		for(int i=0;i<3+3+3+3+1;i++) {
			dataStats[0][i]=VitimageUtils.min(transposedData[i]);
			dataStats[1][i]=VitimageUtils.max(transposedData[i]);
			dataStats[2][i]=VitimageUtils.statistics1D(transposedData[i])[0];
			dataStats[3][i]=VitimageUtils.statistics1D(transposedData[i])[1];
		}

		
		//Set in shape of a CSV
		String unit=getCurrentRefImage().getCalibration().getUnit();
		String s="#Point,Ref_pt._X,Ref_pt._Y,Ref_pt._Z,"+     "Mov_pt._X,Mov_pt._Y,Mov_pt._Z,"+
				 "deltaX(pixels),deltaY(pixels),deltaZ(pixels),"+     "deltaX("+unit+"),deltaY("+unit+"),deltaZ("+unit+"),"+
				 "Distance("+unit+")\n";
		for(int pt=0;pt<nCouples;pt++) {
			s+="Point_"+pt;
			for(int dat=0;dat<3+3+3+3+1;dat++)s+=","+dataExport[pt][dat];
			s+="\n";
		}		
		String[]measurements= {"Min","Max","Mean","Std"};
		for(int pt=0;pt<4;pt++) {
			s+=measurements[pt];
			for(int dat=0;dat<3+3+3+3+1;dat++)s+=","+dataStats[pt][dat];
			s+="\n";
		}		
		IJ.log("Saving file to output path "+this.serieOutputPath);
		VitimageUtils.writeStringInFile(s, nameFile);
		
		for(int i=0;i<dataStats.length;i++)for(int j=0;j<dataStats.length;j++)dataStats[i][j]=VitimageUtils.dou(dataStats[i][j]);
		IJ.showMessage("Your data have been written as a CSV file (excel-friendly) in the output directory : \n"+nameFile+"\nCheck the ImageJ log for an overview of mismatch measurements (mean, std, min, max)");
		IJ.log("Distance in pixels along X axis. Mean="+dataStats[2][0+6]+ ", Std="+ dataStats[3][0+6]+", Min="+dataStats[0][ 6]+", Max="+dataStats[1][ 6 ]);                                              
		IJ.log("                             along Y axis. Mean="+dataStats[2][0+7]+ ", Std="+  dataStats[3][ 7 ]+", Min="+dataStats[0][ 7 ]+", Max="+dataStats[1][ 7 ]);                                              
		IJ.log("                              along Z axis. Mean="+dataStats[2][0+8]+ ", Std="+ dataStats[3][ 8 ]+", Min="+dataStats[0][ 8 ]+", Max="+dataStats[1][ 8 ]);                                              
		IJ.log("Distance in real space ("+unit+") along X axis. Mean="+dataStats[2][0+9]+ ", Std="+dataStats[3][ 9 ]+", Min="+dataStats[0][ 9 ]+", Max="+dataStats[1][ 9 ]);                                              
		IJ.log("                                           along Y axis. Mean="+dataStats[2][0+10]+ ", Std="+ dataStats[3][ 10 ]+", Min="+dataStats[0][ 10 ]+", Max="+dataStats[1][ 10 ]);                                              
		IJ.log("                                           along Z axis. Mean="+dataStats[2][0+11]+ ", Std="+ dataStats[3][ 11 ]+", Min="+dataStats[0][ 11 ]+", Max="+dataStats[1][ 11 ]);                                              
		IJ.log("                                              (norm)      . Mean="+dataStats[2][0+12]+ ", Std="+ dataStats[3][ 12 ]+", Min="+dataStats[0][ 12 ]+", Max="+dataStats[1][ 12 ]);                                              
	}

	public Point3d[][] convertLandmarksToPoints(ImagePlus imgRef,ImagePlus imgMov,boolean computeCoordinatesInRealSpaceUsingCalibration){
		RoiManager rm=RoiManager.getRoiManager();
		int nbCouples=rm.getCount()/2;
		Point3d[]pRef=new Point3d[nbCouples];
		Point3d[]pMov=new Point3d[nbCouples];
//		IJ.setTool("point");
		for(int indP=0;indP<rm.getCount()/2;indP++){
			pRef[indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			pMov[indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			if(computeCoordinatesInRealSpaceUsingCalibration) {
				pRef[indP]=TransformUtils.convertPointToRealSpace(pRef[indP],imgRef);
				pMov[indP]=TransformUtils.convertPointToRealSpace(pMov[indP],imgMov);
			}
		}
		return new Point3d[][] {pRef,pMov};
	}
	
	public ImagePlus getViewOfImagesTransformedAndSuperposedSerieWithThisReference(ImagePlus referenceGeometryForTransforms,boolean saveIndividualImages,boolean saveCombined,String suffixCombined) {
		long t0= System.currentTimeMillis();
		IJ.log("...Timing (at start) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
		final ItkTransform transformAlignementRef=(transforms[referenceTime][referenceModality].size()>0) ? getComposedTransform(referenceTime, referenceModality) : new ItkTransform();
		ImagePlus[] hyperImg=new ImagePlus[nTimes*nMods];
		ItkTransform[][]trsTemp=new ItkTransform[nTimes][nMods];
		for(int nt=0;nt<this.nTimes;nt++) for(int nm=0;nm<this.nMods;nm++)trsTemp[nt][nm]=new ItkTransform();
		IJ.log("...Timing (after transfo prep) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");

		//For the times before the reference
		for(int nt=0;nt<this.referenceTime;nt++) {
			//Step 1 : For each time, at the reference modality, compose the transforms from time to time to go to reference time
			for(int tt=nt;tt<this.referenceTime;tt++) {
				trsTemp[nt][this.referenceModality].addTransform(getComposedTransform(tt,this.referenceModality));
			}
			
			for(int nm=0;nm<this.nMods;nm++) {
				if(imageExists(nt, nm)) {
					if(nm!=this.referenceModality) {
						//Step 2 : at each time, add the transform that goes from each modality to the reference modality
						trsTemp[nt][nm]= getComposedTransform(nt, nm);
						//Step 3 : Add the steps 1 transform to the step 2 transforms
						trsTemp[nt][nm].addTransform(new ItkTransform(trsTemp[nt][this.referenceModality]));
					}
				}
			}
		}
		IJ.log("...Timing (after steps 1, 2, 3) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
		//For the times after the reference and the reference
		for(int nt=this.nTimes-1;nt>=this.referenceTime;nt--) {
			//Step 1 : For each time, at the reference modality, compose the transforms from time to time to go to reference time
			for(int tt=nt;tt>this.referenceTime;tt--) {
				trsTemp[nt][this.referenceModality].addTransform(getComposedTransform(tt,this.referenceModality));
			}
			
			for(int nm=0;nm<this.nMods;nm++) {
				if(imageExists(nt, nm)) {
					if(nm!=this.referenceModality) {
						//Step 2 : at each time, add the transform that goes from each modality to the reference modality
						trsTemp[nt][nm]= getComposedTransform(nt, nm);
						//Step 3 : Add the steps 1 transform to the step 2 transforms
						trsTemp[nt][nm].addTransform(new ItkTransform(trsTemp[nt][this.referenceModality]));
					}
				}
			}
		}
		IJ.log("...Timing (after steps 4, 5, 6) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
			

		
		

		
		//Step 4 : Add alignment transform to both, then export images
		String infoGlobal="";
		String infoLocal="";
		ImagePlus imgTemp=IJ.openImage(this.paths[this.referenceTime][this.referenceModality]);
		for(int nt=0;nt<this.nTimes;nt++) {
			for(int nm=0;nm<this.nMods;nm++) {
				if((nt==0)&&(nm==0))infoGlobal=new ImageInfo().getImageInfo(imgTemp);
				infoLocal=new ImageInfo().getImageInfo(imgTemp);
				int index=nm*this.nTimes+nt;
				if(imageExists(nt, nm)) {
					IJ.log("Computing result data for time "+times[nt]+" and modality "+mods[nm]);
					IJ.log("...Timing (transforming image number "+nt+","+nm+", start) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
					trsTemp[nt][nm].addTransform(new ItkTransform(transformAlignementRef));
					IJ.log("...Timing (transforming image number "+nt+","+nm+", transfo complete) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
					imgTemp=IJ.openImage(this.paths[nt][nm]);
					IJ.log("...Timing (transforming image number "+nt+","+nm+", image opened) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
					System.out.println("Transforming with reference="+VitimageUtils.imageResume(referenceGeometryForTransforms));
					System.out.println("Transforming imgTemp="+VitimageUtils.imageResume(imgTemp));
					hyperImg[index]=trsTemp[nt][nm].transformImage(referenceGeometryForTransforms,imgTemp, false,true,t0,false);//!VitimageUtils.isBouture(imgTemp)
					IJ.log("...Timing (transforming image number "+nt+","+nm+", image transformed) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
					if(saveIndividualImages) {
						VitimageUtils.addLabelOnAllSlices(hyperImg[index],"t="+this.times[nt]+" mod="+this.mods[nm]);
						if(isBionanoImagesWithCapillary) {
							for(int c=1;c<hyperImg[index].getNChannels();c++) {hyperImg[index].setC(c);IJ.run(hyperImg[index],"Fire","");}
							hyperImg[index].setC(1);
						}
						IJ.saveAsTiff(hyperImg[index],nameForExport(nt, nm,true));
						IJ.log("...Timing (transforming image number "+nt+","+nm+", result image saved) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
						trsTemp[nt][nm].writeToFileWithTypeDetection(nameForExport(nt, nm,false), referenceGeometryForTransforms);	
						IJ.log("...Timing (transforming image number "+nt+","+nm+", dense transform saved) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
					}
				}
				else {
					 hyperImg[index]=VitimageUtils.nullImage(referenceGeometryForTransforms);
				}
				if(this.valueAllTypes()==ImagePlus.GRAY32 || this.valueAllTypes()==-1)IJ.run(hyperImg[index],"32-bit","");
				IJ.log("Img t"+nt+" mod"+nm+" index "+index+"  ready. ");
			}
		}
		IJ.log("...Timing (after individual export) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");


		
		//Compute and display the resulting hyperimage
		Concatenator con=new Concatenator();
		ImagePlus hypTemp=con.concatenate(VitimageUtils.hyperUnstack(hyperImg),false);
		IJ.log("Image pile size before hyperstacking="+VitimageUtils.imageResume(hypTemp));
		con.setIm5D(true);
		IJ.log("...Timing (after brute force concatenation) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
		
		
		
		int futureM=nMods;
		int futureT=nTimes;
		double[][]rangesFinal=new double[1][1];
		//LUT[]lutsFinal=new LUT[1];
		String codeStacking="xyztc";
		futureT=this.nbTimesOfInputData*this.nTimes;
		futureM=this.nbChannelsOfInputData*this.nMods;
		IJ.log("Case 0");
		if(this.nTimes>1 && this.nMods>1 && this.nbChannelsOfInputData>1 && this.nbTimesOfInputData>1) {
			//The hell here. Does nothing. None correction possible. Result 5D hyperimage will be messy interleaved. Hell inside, hell outside
			if(saveIndividualImages)IJ.showMessage("Input data is 5D images with \"m\" multiple modalities and \"t\" times (see the \"channel\" and \"Frame\" slicer).\n"+
						   "You registered it along multiple mods and times\n"+
						   "The resulting combined 5D hyperimage that will appear soon will be severely messy\n"+
						   "Hopefully, the exported indivudal images will be good.");
			rangesFinal=new double[this.nMods*this.nbChannelsOfInputData][2];
			//lutsFinal=new LUT[this.nMods*this.nbChannelsOfInputData];
			for(int nm1=0;nm1<this.nMods;nm1++)for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {
				rangesFinal[nm1*this.nbChannelsOfInputData+nm2]=new double[] {this.imageRanges[this.referenceTime][this.referenceModality][0][nm2][0],this.imageRanges[this.referenceTime][this.referenceModality][nm1][0][1]};
				//lutsFinal[nm1*this.nbChannelsOfInputData+nm2]=this.imageLuts[this.referenceTime][this.referenceModality][nm2];
			}
		}
		else if(this.nTimes>1 && this.nMods>1 && this.nbChannelsOfInputData>1 && this.nbTimesOfInputData==1) {
			IJ.log("Case 1");
			if(saveIndividualImages)IJ.showMessage("Input data is 4D images with \"m\" multiple modalities (see the \"channel\" slicer)."+
						   "As You registered it along \"M\" multiple mods and \"T\" successive times,\n"+
						   "The combined hyperimage will gather T and m on the same slicer\n.\n"+
							"Hopefully, the exported indivudal images will be good.");
			//As data come is Mods / Times / Nb Channels, and have to be stacked MOD / TIM, we gather Time and Nb Channels to ensure slicer coherence
			futureM=nMods;
			futureT=nTimes*this.nbChannelsOfInputData;
			rangesFinal=new double[this.nMods][2];
			//lutsFinal=new LUT[this.nMods];
			for(int nm1=0;nm1<this.nMods;nm1++) {
				rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][nm1][0][0][0],this.imageRanges[this.referenceTime][nm1][0][0][1]};
				//lutsFinal[nm1]=this.imageLuts[this.referenceTime][nm1][0];
			}
		}
		else if(this.nTimes==1 && this.nMods>1 && this.nbChannelsOfInputData>1 && this.nbTimesOfInputData>1) {
			IJ.log("Case 1.5");
			if(saveIndividualImages)IJ.showMessage("Input data is 4D images with \"m\" multiple modalities and t multiple times (see the \"channel\" and \"frames\" slicers)."+
						   "As You registered it along \"M\" multiple mods and \"T\" successive times,\n"+
						   "The combined hyperimage could be a little bit messy to understand\n.\n"+
							"Hopefully, the exported indivudal images will be good.");
			//As data come is Mods / Times / Nb Channels, and have to be stacked MOD / TIM, we gather Time and Nb Channels to ensure slicer coherence
			futureM=nMods*this.nbChannelsOfInputData;
			futureT=this.nbTimesOfInputData;
			rangesFinal=new double[this.nMods*this.nbChannelsOfInputData][2];
			//lutsFinal=new LUT[this.nMods*this.nbChannelsOfInputData];
			for(int nm1=0;nm1<this.nMods;nm1++) {
				for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {
					rangesFinal[nm1*this.nbChannelsOfInputData+nm2]=new double[] {this.imageRanges[this.referenceTime][nm1][0][nm2][0],this.imageRanges[this.referenceTime][nm1][0][nm2][1]};
					//lutsFinal[nm1*this.nbChannelsOfInputData+nm2]=this.imageLuts[this.referenceTime][nm1][nm2];
				}
			}
		}
		else if(this.nTimes>1 && this.nMods>1 && this.nbChannelsOfInputData==1 && this.nbTimesOfInputData>1) {
			if(saveIndividualImages)IJ.showMessage("Input data is 4D images with \"t\" multiple time points (see the \"frame\" slicer)."+
					   "As you registered it along \"M\" multiple mods and \"T\" successive times,\n"+
					   "The combined hyperimage will gather T and t on the same slicer (\"Frames\")\n.\n"+
						"Hopefully, the exported indivudal images will be good.");
			IJ.log("Case 2");
			rangesFinal=new double[this.nMods][2];
			//lutsFinal=new LUT[this.nMods];
			for(int nm1=0;nm1<this.nMods;nm1++){
				rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][nm1][0][0][0],this.imageRanges[this.referenceTime][nm1][0][0][1]};
				//lutsFinal[nm1]=this.imageLuts[this.referenceTime][nm1][0];
			}
		}
		else if(this.nTimes>1 && this.nMods==1 && this.nbChannelsOfInputData>1 && this.nbTimesOfInputData>1) {
			if(saveIndividualImages)IJ.showMessage("Input data is 4D images with \"t\" multiple time points and m multiple modalities (see the \"frame\" and  \"channel\" slicers)."+
					   "As you registered it along  \"T\" successive times,\n"+
					   "The combined hyperimage will gather T and t on the same slicer (\"Frames\")\n. Combined image could be messy.\n"+
						"Hopefully, the exported indivudal images will be good.");
			IJ.log("Case 2.5");
			rangesFinal=new double[this.nbChannelsOfInputData][2];
			//lutsFinal=new LUT[this.nbChannelsOfInputData];
			for(int nm1=0;nm1<this.nbChannelsOfInputData;nm1++){
				rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][this.referenceModality][0][nm1][0],this.imageRanges[this.referenceTime][this.referenceModality][0][nm1][1]};
				//lutsFinal[nm1]=this.imageLuts[this.referenceTime][this.referenceModality][nm1];
			}
		}
		else if(this.nTimes==1 && this.nMods>1 && this.nbChannelsOfInputData==1 && this.nbTimesOfInputData>1) {
			IJ.log("Case 3");
			rangesFinal=new double[this.nMods][2];
			//lutsFinal=new LUT[this.nMods];
			for(int nm1=0;nm1<this.nMods;nm1++){
				rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][nm1][0][0][0],this.imageRanges[this.referenceTime][nm1][0][0][1]};
				//lutsFinal[nm1]=this.imageLuts[this.referenceTime][nm1][0];
			}
		}
		else if(this.nTimes>1 && this.nMods==1 && this.nbChannelsOfInputData>1 && this.nbTimesOfInputData==1) {
			IJ.log("Case 4");
			//Here data come in Times / Mods, and have to be stacked that way
			codeStacking="xyzct";
			rangesFinal=new double[this.nbChannelsOfInputData][2];
			//lutsFinal=new LUT[this.nbChannelsOfInputData];
			for(int nm1=0;nm1<this.nbChannelsOfInputData;nm1++){
				rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][this.referenceModality][0][nm1][0],this.imageRanges[this.referenceTime][this.referenceModality][0][nm1][1]};
				//lutsFinal[nm1]=this.imageLuts[this.referenceTime][this.referenceModality][nm1];
			}
		}
		else if(this.nMods>1 && this.nbChannelsOfInputData>1) {
			IJ.log("Case 5");
			//Here data come in Mods / mods, what should be an error. Let s guess that the the so-called Mods were Times, for a time-lapse
			if(!saveIndividualImages || !VitiDialogs.getYesNoUI("Warning","Your input data was 4D image with multiple modalities (m="+this.nbChannelsOfInputData+" on the \"Channels\" slicer),\n"+
					   "Therefore, you registered these input images as multiple modalities (M="+this.nMods+"\n"+
					   "You see that this makes a lot of modalities interleaving at the end,\n"+
					   "If this is what you mean, answer Yes.\nElsewhere, If you think you made a mistake,"+
					   "and intend to register m multiple modalities of images having M multiple times instead, "+
					   "answer  \"No\"")) {
				futureM=this.nbChannelsOfInputData;
				futureT=this.nMods;
				rangesFinal=new double[this.nbChannelsOfInputData][2];
				//lutsFinal=new LUT[this.nbChannelsOfInputData];
				for(int nm1=0;nm1<this.nbChannelsOfInputData;nm1++){
					rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][this.referenceModality][0][nm1][0],this.imageRanges[this.referenceTime][this.referenceModality][0][nm1][1]};
					//lutsFinal[nm1]=this.imageLuts[this.referenceTime][this.referenceModality][nm1];
				}
				codeStacking="xyzct";
			}
			else {
				rangesFinal=new double[this.nMods*this.nbChannelsOfInputData][2];
				//lutsFinal=new LUT[this.nMods*this.nbChannelsOfInputData];
				for(int nm1=0;nm1<this.nMods;nm1++)for(int nm2=0;nm2<this.nbChannelsOfInputData;nm2++) {
					rangesFinal[nm1*this.nbChannelsOfInputData+nm2]=new double[] {this.imageRanges[this.referenceTime][nm1][0][nm2][0],this.imageRanges[this.referenceTime][nm1][0][nm2][1]};
					//lutsFinal[nm1*this.nbChannelsOfInputData+nm2]=this.imageLuts[this.referenceTime][nm1][nm2];
				}
				codeStacking="xyzct";
			}
		}
		else if(this.nTimes>1 && this.nbTimesOfInputData>1) {
			IJ.log("Case 6");
			//Here data come in Times / times, what should be an error. Let s guess that the the so-called Times were Mods, for a multimod
			if(!saveIndividualImages || !VitiDialogs.getYesNoUI("Warning","Your input data was 4D image with multiple times (t="+this.nbTimesOfInputData+" on the \"Frames\" slicer),\n"+
						   "Therefore, you registered these input images as successive imaging times (T="+this.nTimes+"\n"+
						   "You see that this makes a lot of times interleaving at the end,\n"+
						   "If this is what you mean, answer Yes.\nElsewhere, If you think you made a mistake,"+
						   "and intend to register T multiple modalities of images having t multiple times instead, "+
						   "answer  \"No\"")) {
				futureM=this.nTimes;
				futureT=this.nbTimesOfInputData;
				rangesFinal=new double[this.nTimes][2];
				//lutsFinal=new LUT[this.nTimes];
				for(int nm1=0;nm1<this.nTimes;nm1++){
					rangesFinal[nm1]=new double[] {this.imageRanges[nm1][this.referenceModality][0][0][0],this.imageRanges[nm1][this.referenceModality][0][0][1]};
					//lutsFinal[nm1]=this.imageLuts[nm1][this.referenceModality][0];
				}
			}
			else {
				futureM=1;
				futureT=this.nTimes*this.nbTimesOfInputData;
				rangesFinal=new double[1][2];
				//lutsFinal=new LUT[1];
				for(int nm1=0;nm1<1;nm1++){
					rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][this.referenceModality][0][0][0],this.imageRanges[this.referenceTime][this.referenceModality][0][0][1]};
					//lutsFinal[nm1]=this.imageLuts[this.referenceTime][this.referenceModality][0];
				}				
			}
		}
		else  {
			IJ.log("Case 7");
			//Here data come in only Times, only Mods, or Times / Mods, and have to be stacked that way
			rangesFinal=new double[this.nMods][2];
			//lutsFinal=new LUT[this.nMods];
			for(int nm1=0;nm1<this.nMods;nm1++){
				rangesFinal[nm1]=new double[] {this.imageRanges[this.referenceTime][nm1][0][0][0],this.imageRanges[this.referenceTime][nm1][0][0][1]};
				//lutsFinal[nm1]=this.imageLuts[this.referenceTime][nm1][0];
			}
		}
		IJ.log("Case 9");
		ImagePlus hyperImage=null;
		IJ.log("...Timing (after managing chans/times range) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
		IJ.log("Stacking into an hyperimage with dims : Canals="+futureM+" , Zslices="+referenceGeometryForTransforms.getNSlices()+" , Times="+futureT);
		
		
		
		
		
		
		if(valueAllTypes()==ImagePlus.COLOR_RGB) {
			hypTemp.setDimensions(futureM, referenceGeometryForTransforms.getNSlices(), futureT);
			if(codeStacking=="xyzct")(new HyperStackConverter()).shuffle(hypTemp,HyperStackConverter.ZCT );
			if(codeStacking=="xyztc")(new HyperStackConverter()).shuffle(hypTemp,HyperStackConverter.ZTC );
			if(codeStacking=="xyczt")(new HyperStackConverter()).shuffle(hypTemp,HyperStackConverter.CZT );
			hyperImage=hypTemp.duplicate();
			IJ.log("...Timing (after hyperstack reordering) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
		}
		else {
			IJ.log("Starting hyperstacking "+futureM+" , "+referenceGeometryForTransforms.getNSlices()+" , "+futureT+" , "+codeStacking );
			hyperImage=HyperStackConverter.toHyperStack(hypTemp, futureM, referenceGeometryForTransforms.getNSlices(),futureT,codeStacking,"Grayscale");
			for(int c=1;c<=futureM;c++) {
				IJ.log("On canal "+c+"setting range to "+rangesFinal[c-1][0]+" to "+rangesFinal[c-1][1]);
				hyperImage.setC(c);
				hyperImage.setDisplayRange(rangesFinal[c-1][0], rangesFinal[c-1][1]);
				if(isBionanoImagesWithCapillary) {
					IJ.run(hyperImage,"Fire","");
				}
			}
			hyperImage.setC(1);
			IJ.log("...Timing (after hyperstack reordering) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
		}
		//if(WindowManager.getImage(fijiyamaGui.displayedNameCombinedImage) != null) {WindowManager.getImage(fijiyamaGui.displayedNameCombinedImage).close();}
		if(saveCombined) IJ.saveAsTiff(hyperImage, nameForResultingCombinedHyperImage(suffixCombined));

		
		
		hyperImage.show();
		VitimageUtils.waitFor(200);
		hyperImage.setTitle(suffixCombined);
		//hyperImage.setTitle(fijiyamaGui.displayedNameCombinedImage);
		IJ.log("...Timing (after opening corresponding ImageWindow for visualization) : "+VitimageUtils.dou((System.currentTimeMillis()-t0)/1000.0)+" s");
		IJ.log("End of sequence, timer closing.");
		
		return hyperImage;

	}
		
	public ImagePlus getViewOfImagesTransformedAndSuperposedTwoImg() {
		ImagePlus imgMovCurrentState,imgRefCurrentState,imgView;
		ItkTransform trMov,trRef = null;

		//Update views
		if(this.transforms[movTime][movMod].size()==0 && this.transforms[refTime][refMod].size()==0 ) {
			imgRefCurrentState=this.images[refTime][refMod];
			imgMovCurrentState=new ItkTransform().transformImage(imgRefCurrentState, this.images[movTime][movMod],false);
		}
		else {
			trMov=getComposedTransform(movTime,movMod);
			if(this.transforms[refTime][refMod].size()>0) {
				trRef=getComposedTransform(refTime, refMod);
				trMov.addTransform(trRef);
			}
			imgMovCurrentState= trMov.transformImage(this.images[refTime][refMod],this.images[movTime][movMod],false);	
			imgRefCurrentState= (trRef==null)? new ItkTransform().transformImage(this.images[refTime][refMod],this.images[refTime][refMod],false) : trRef.transformImage(this.images[refTime][refMod],this.images[refTime][refMod],false);
		}
		imgRefCurrentState.setDisplayRange(this.globalRanges[refTime][refMod][0], this.globalRanges[refTime][refMod][1]);
		imgMovCurrentState.setDisplayRange(this.globalRanges[movTime][movMod][0], this.globalRanges[movTime][movMod][1]);
		
		//Compose images
		imgView=VitimageUtils.compositeNoAdjustOf(imgRefCurrentState,imgMovCurrentState,step==0 ? "Superimposition before registration" : "Registration results after "+(step)+" step"+((step>1)?"s":""));
		int viewSlice=this.images[refTime][refMod].getNSlices()/2;
		imgView.setSlice(viewSlice);
		return imgView;
	}
	

	
	
	
	
	
	
	/*Simple helpers ********************************************************************************************************************/	
	public int getMaxAcceptableLevelForTheCurrentAction() {
		return maxAcceptableLevel;
	}
	
	public ItkTransform getComposedTransform(int nt, int nm) {
		if (nt>=this.nTimes)return null;
		if (nm>=this.nMods)return null;
		if (this.transforms[nt][nm].size()==0)return new ItkTransform();
		ItkTransform trTot=new ItkTransform();
		for(int indT=0;indT<this.transforms[nt][nm].size();indT++)trTot.addTransform(this.transforms[nt][nm].get(indT));
		return trTot;
	}
		
	public ItkTransform getComposedTransform(int nt, int nm,int lastStep) {
		if (nt>=this.nTimes)return null;
		if (nm>=this.nMods)return null;
		if(lastStep>=this.step)return null;
		if (this.transforms[nt][nm].size()==0)return new ItkTransform();
		ItkTransform trTot=new ItkTransform();
		for(int indT=0;indT<this.transforms[nt][nm].size();indT++) {
			if(this.transforms[nt][nm].get(indT).step<=lastStep) {
				trTot.addTransform(this.transforms[nt][nm].get(indT));
			}
		}
		return trTot;
	}

	public int estimateTime(RegistrationAction regAct) {;
		if((regAct.typeAction==0) && (regAct.typeManViewer==0))return 300;//manual registration with landmarks
		if((regAct.typeAction==0) && (regAct.typeManViewer==1))return 240;//manual registration with landmarks
		if((regAct.typeAction==2) && (regAct.typeManViewer==0))return 200;//manual registration with landmarks
		if((regAct.typeAction==2) && (regAct.typeManViewer==1))return 240;//manual registration with landmarks
		else {
			if(regAct.typeOpt==OptimizerType.BLOCKMATCHING) {
				return (int)Math.round(BlockMatchingRegistration.estimateRegistrationDuration(
					dimensions[regAct.refTime][regAct.refMod],regAct.typeAutoDisplay,regAct.typeTrans==Transform3DType.DENSE ? regAct.levelMinDense : regAct.levelMaxLinear,regAct.typeTrans==Transform3DType.DENSE ? regAct.levelMaxDense : regAct.levelMaxLinear,
							regAct.typeTrans==Transform3DType.DENSE ? regAct.iterationsBMDen : regAct.iterationsBMLin,regAct.typeTrans,
					new int[] {regAct.bhsX,regAct.bhsY,regAct.bhsZ},   new int[] {regAct.strideX,regAct.strideY,regAct.strideZ},
					new int[] {regAct.neighX,regAct.neighY,regAct.neighZ},
					this.nbCpu,regAct.selectScore,regAct.selectLTS,regAct.selectRandom,regAct.subsampleZ==1,regAct.higherAcc
					));			
			}
			else {
				return ItkRegistration.estimateRegistrationDuration(
					this.imageParameters,regAct.typeAutoDisplay,regAct.iterationsITK, regAct.typeTrans==Transform3DType.DENSE ? regAct.levelMinDense : regAct.levelMinLinear,regAct.levelMaxLinear);
			}
		}
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

	public boolean isHyperImage(int n,int m) {
		return (this.initDimensions[n][m][3]*this.initDimensions[n][m][4]>1);
	}
	
	@SuppressWarnings("restriction")
	public String []checkComputerCapacity(boolean verbose) {
		this.nbCpu=Runtime.getRuntime().availableProcessors();
		this.jvmMemory=(int)((new Memory().maxMemory() /(1024*1024)));//Java virtual machine available memory (in Megabytes)
		this.memoryFullSize=0;
		String []str=new String[] {"",""};

		str[0]="Welcome to Fijiyama "+fijiyamaGui.versionFlag+" ! \nFirst trial ? Click on \"Contextual help\" to get started. \n User requests, issues ? Contact : romainfernandez06ATgmail.com\n";
		if(fijiyamaGui.currentContextIsSerie())str[0]+="Current mode : series processing. Click on \"Run next action\", or use the menu to modify it before. Series can be long. Keep clicking !";
		else str[0]+="Current mode : two-images registration. Choose the next action using the menus, then click on \"Start this action\"";
		str[1]="System check. Available memory in JVM="+this.jvmMemory+" MB. #Available processor cores="+this.nbCpu+".";
		try {
			this.memoryFullSize = ( ((com.sun.management.OperatingSystemMXBean) ManagementFactory
		        .getOperatingSystemMXBean()).getTotalPhysicalMemorySize() )/(1024*1024);
		}	catch(Exception e) {return str;}		
		int maxTemp=this.jvmMemory/(memoryFactorForGoodUX);	
		this.maxImageSizeForRegistration=Math.min(maxTemp, this.maxImageSizeForRegistration);	
		if((this.memoryFullSize>this.jvmMemory*2) && (this.memoryFullSize-this.jvmMemory>4000))  {
			str[1]+="\nIt seems that your computer have more memory : total memory="+(this.memoryFullSize)+" MB."+
					"Registration process is time and memory consuming. To give more memory and computation power to Fiji,"+
					" close the plugin then use the Fiji menu \"Edit / Options / Memory & threads\". "+
					"Let at least "+VitimageUtils.getSystemNeededMemory()+" unused to keep your "+VitimageUtils.getSystemName()+" stable and responsive.";
		}
		if(verbose)		return str;
		else return new String[] {"",""};
	}

	public ListModel<String> getPipelineAslistModelForGUI() {
		DefaultListModel<String> listModel = new DefaultListModel<String>();
        for(int i=0;i<this.regActions.size();i++) listModel.addElement(regActions.get(i).readableString());
        listModel.addElement("< Next action to come >");
        return listModel;
	}
	
	public void printRegActions(String name,ArrayList<RegistrationAction> listAct) {
		IJ.log("");
		IJ.log(" # Reg actions "+name+", size="+(listAct==null ? "NULL" : listAct.size()));
		if(listAct!=null) {
			for(int i=0;i<listAct.size();i++) {
				IJ.log(" # "+listAct.get(i));
			}
		}
	}
	
	public String nameForResultingCombinedHyperImage(String suffix) {
		String dirpath=new File(this.serieOutputPath,"Exported_data").getAbsolutePath();
		String prefix="combined_hyperimage_"+suffix+".tif";
		return new File(dirpath,prefix).getAbsolutePath();
	}
	
	public String nameForExport(int nt,int nm,boolean yesForImageNoForTransform) {
		String extension=expression.substring(expression.length()-4,expression.length());
		String expressionNoExtension=expression.substring(0,expression.length()-4);
		String dirpath=new File(this.serieOutputPath,"Exported_data").getAbsolutePath();
		String prefix=( yesForImageNoForTransform ? "" : "transform_global_" );
		String suffix=yesForImageNoForTransform ? "_after_registration"+extension : ".txt";
		if(this.isSerie) {
			if(this.nTimes>1) {
				if(this.nMods>1) {
					return new File(dirpath,prefix+expressionNoExtension.replace("{Time}",times[nt]).replace("{ModalityName}",mods[nm])+suffix).getAbsolutePath();
				}
				else {
					return new File(dirpath,prefix+expressionNoExtension.replace("{Time}",times[nt])+suffix).getAbsolutePath();					
				}
			}
			else {
				if(this.nMods>1) {
					return new File(dirpath,prefix+expressionNoExtension.replace("{ModalityName}",mods[nm])+suffix).getAbsolutePath();
				}
				else {
					return "";
				}
			}
		}
		else {//Two imgs mode
			if(nm==0)return new File(dirpath,prefix+"img_reference"+suffix).getAbsolutePath();
			else return new File(dirpath,prefix+"img_moving"+suffix).getAbsolutePath();
		}
	}
	
	public String openFjmFileAndGetItsPath() {
		String name=VitiDialogs.openJFileUI("Select previous study configuration file ( .fjm)", "", ".fjm");
		return(name);
	}

	
	
	
	
	
	
	/* Getters/setters and simple access informations**************************************************************************************/
	public boolean axisAlignmentDone() {
/*		System.out.println("IS alignement done "+referenceTime+" , "+referenceModality+" ? "+transforms[referenceTime][referenceModality].size());
		for(int i=0;i<transforms.length;i++) {
			for(int j=0;j<transforms[i].length;j++) {
				System.out.println("TRANSFORMS "+i+","+j+" : "+transforms[i][j].size());
				if((i==0) && (j==0)) {
					for(int k=0;k<transforms[i][j].size();k++) {
						System.out.println(transforms[referenceTime][referenceModality].get(k).drawableString());
					}
				}
			}
		}
	*/	
		return false;//(transforms[referenceTime][referenceModality].size() > 0);
	}
	
	public ImagePlus getCurrentRefImage() {
		//System.out.println("CURRENT REF="+currentRegAction.refTime+" "+currentRegAction.refMod);
		return images[currentRegAction.refTime][currentRegAction.refMod];		                              
	}

	public ImagePlus getCurrentMovImage() {
		return images[currentRegAction.movTime][currentRegAction.movMod];		                              
	}
	
	public double[] getCurrentRefRange() {
		return globalRanges[currentRegAction.refTime][currentRegAction.refMod];
	}
	
	public double[] getCurrentMovRange() {
		return globalRanges[currentRegAction.movTime][currentRegAction.movMod];
	}
	
	public ItkTransform getCurrentRefComposedTransform() {
		return getComposedTransform(currentRegAction.refTime, currentRegAction.refMod);
	}
	
	public ItkTransform getCurrentMovComposedTransform() {
		return getComposedTransform(currentRegAction.movTime, currentRegAction.movMod);
	}
	
	public ItkTransform getCurrentMovComposedTransform(int lastStep) {
		return getComposedTransform(currentRegAction.movTime, currentRegAction.movMod,lastStep);
	}
	
	public String getModeString() {
		return isSerie ? stringSerie : stringTwoImgs;
	}
	
	public int getNbSteps() {
		return nSteps;
	}

	public String getUnit() {
		return unit;
	}

	public int getStep() {
		return step;
	}
	
	public RegistrationAction getCurrentAction() {
		return this.currentRegAction;
	}
	
	public void setStep(int st) {
		this.step=st;
	}
	
	public void updateNbSteps() {
		this.nSteps=regActions.size();
	}

	
	public void freeMemory() {
		if(images!=null)for(int i=0;i<images.length;i++)for(int j=0;j<images[i].length;j++)images[i][j]=null;
		images=null;
		if(transforms!=null)for(int i=0;i<transforms.length;i++)for(int j=0;j<transforms[i].length;j++)transforms[i][j].clear();
		transforms=null;
		if(trActions!=null)trActions.clear();
		trActions=null;		
		System.gc();

	}

}
