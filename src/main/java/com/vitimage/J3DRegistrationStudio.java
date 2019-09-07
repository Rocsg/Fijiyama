package com.vitimage;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Color3f;

import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;

import fiji.util.gui.OverlayedImageCanvas;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.Concatenator;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij3d.Image3DUniverse;
import imagescience.transform.Transform;
import math3d.Point3d;
import trainableSegmentation.FeatureStack;
import trainableSegmentation.RoiListOverlay;

public class J3DRegistrationStudio implements PlugIn {
	ij3d.Image3DUniverse univ;
	boolean hasReferenceGeometry=false;
	boolean volumeRenderingManualRegistrationIsRunning=false;
	boolean landmarksBasedManualRegistrationIsRunning=false;
	boolean colorMode=true;
	boolean colorOrder=true;
	boolean thereIsDenseField=false;
	boolean invertedRealized=false;
	ImagePlus imgRef;
	ImagePlus imgMov;
	ImagePlus imgMask;
	ImagePlus imgResult;
	ItkTransform transGeom=new ItkTransform();
	ArrayList<ItkTransform>transResults=new ArrayList<ItkTransform>();
	ItkTransform currentGlobalTransform=new ItkTransform();
	ImagePlus imgFus;
	JFrame toolbar;
	JButton stopButton;
	JButton switchColorButton;
	JButton changeLUTButton;
	JButton saveButton;
	JButton loadButton;
	JButton useMaskButton;
	JButton switchRefButton;
	JButton manRegistrationButton;
	JButton autoRegistrationButton;
	JButton undoButton;
	boolean isBusy=false;
	final int nbProcessors;
	/** executor service to launch threads for the plugin methods and events */
	private final ExecutorService exec = Executors.newFixedThreadPool(1);

	/**
	 * Listeners
	 */
	private ActionListener listener = new ActionListener() {


		public void actionPerformed(final ActionEvent e) {

			 if( e.getSource()==stopButton) {
				 if(volumeRenderingManualRegistrationIsRunning){try{	System.out.println("Stop volume rendering action starts...");	close3dInterfaceAndWriteCorrespondingTransform() ; stopButton.setEnabled(false);updateGlobalTransformAndMovingImage(); updateFusionAndShow();
					}catch(Exception ee){ee.printStackTrace();	}	}
				 else if(landmarksBasedManualRegistrationIsRunning){try{	System.out.println("Stop landmark taking action starts...");	landmarksBasedManualRegistrationIsRunning=false;	stopButton.setEnabled(false);				}catch(Exception ee){ee.printStackTrace();	}	}
				 setButtonLocked(false);
				 return;
			 }
			 if( !isBusy ) {
				 exec.submit(new Runnable() {


					public void run() 
					{
						setButtonLocked(true);
						if(e.getSource() == saveButton){try{	System.out.println("Save action starts...");	saveRegisteredImages();	
								}catch(Exception ee){ee.printStackTrace();	}	}
						
						else if(e.getSource() == loadButton){try{		System.out.println("Load action starts...");	 loadTransform();					updateGlobalTransformAndMovingImage(); updateFusionAndShow();
								}catch(Exception ee){ee.printStackTrace();	}	}
						
						else if(e.getSource() == switchRefButton){try{		System.out.println("Switch reference action starts...");	 switchReference();		updateGlobalTransformAndMovingImage(); updateFusionAndShow();			
						}catch(Exception ee){ee.printStackTrace();	}	}

						
						else if(e.getSource() == manRegistrationButton){try{		System.out.println("Man registration action starts...");            runManualRegistration();  if(!volumeRenderingManualRegistrationIsRunning) { updateGlobalTransformAndMovingImage(); updateFusionAndShow();}
								}catch(Exception ee){ee.printStackTrace();	}	}
						
						else if(e.getSource() == autoRegistrationButton){try{		System.out.println("Automatic registration action starts...");		runAutomaticRegistration();	updateGlobalTransformAndMovingImage(); updateFusionAndShow();		
								}catch(Exception ee){ee.printStackTrace();	}	}
												
						else if(e.getSource() == switchColorButton){try{		System.out.println("Switch color action starts...");			colorOrder=!colorOrder;		updateFusionAndShow();	
								}catch(Exception ee){ee.printStackTrace();	}	}

						else if(e.getSource() == changeLUTButton){try{		System.out.println("Change LUT action starts...");				colorMode=!colorMode;	        updateFusionAndShow();	
								}catch(Exception ee){ee.printStackTrace();	}	}

						else if(e.getSource() == useMaskButton){try{		System.out.println("Use maskl action starts...");			loadCustomMask();		
						}catch(Exception ee){ee.printStackTrace();	}	}


						else if(e.getSource() == undoButton && transResults.size()>0){try{		System.out.println("undo action starts...");	undo();		 updateGlobalTransformAndMovingImage();  updateFusionAndShow();
								}catch(Exception ee){ee.printStackTrace();	}	}
						if(!volumeRenderingManualRegistrationIsRunning)setButtonLocked(false);
					}							
				 });
			 }
		}
	};


	public void setButtonLocked(boolean lock) {
		isBusy=lock;
		switchColorButton.setEnabled(!lock);
		undoButton.setEnabled(!lock);
		loadButton.setEnabled(!lock);
		switchRefButton.setEnabled(!lock);
		saveButton.setEnabled(!lock);
		changeLUTButton.setEnabled(!lock);
		autoRegistrationButton.setEnabled(!lock);
		manRegistrationButton.setEnabled(!lock);
		useMaskButton.setEnabled(!lock);
	}
	
	
	//Entry point at testing time
	public static void main(String[] args) {
		boolean testing=false;
		ImageJ ij=new ImageJ();
		J3DRegistrationStudio j3D=new J3DRegistrationStudio();

		if(testing) {
			j3D.setImgRef(IJ.openImage("/home/fernandr/Bureau/Test/TestKhalifa/imgRef_IRM.tif"));
			j3D.setImgMov(IJ.openImage("/home/fernandr/Bureau/Test/TestKhalifa/imgMov_RX.tif"));
			j3D.updateFusionAndShow();
			j3D.startGUI();
		}
		else {
			ImagePlus []imgTab=VitiDialogs.chooseTwoImagesUI("Please choose a reference (fixed) image, and a moving image.","Reference image","Moving image");
			j3D.setImgRef(imgTab[0]);
			j3D.setImgMov(imgTab[1]);
			j3D.updateFusionAndShow();
			j3D.startGUI();						
		}
	}


	//Entry point at using time
	@Override
	public void run(String arg0) {
		ImageJ ij=new ImageJ();
		J3DRegistrationStudio j3D=new J3DRegistrationStudio();
		ImagePlus[]imgIn=VitiDialogs.chooseTwoImagesUI("Choose reference image (fixed image)\n and moving image (to be aligned with reference)", "Reference image", "Moving image");
		j3D.setImgMov(imgIn[1]);
		j3D.setImgRef(imgIn[0]);
		j3D.updateFusionAndShow();
		j3D.startGUI();
	}
	
	public J3DRegistrationStudio() {
		this.nbProcessors = Runtime.getRuntime().availableProcessors();
	}

	
	public void setImgMov(ImagePlus imgMov){
		this.imgMov=imgMov.duplicate();
		updateGlobalTransformAndMovingImage();
	}
	
	public void setImgRef(ImagePlus imgRef){
		this.imgRef=imgRef.duplicate();
	}
	

	public void updateFusionAndShow() {		
		String ti=colorOrder ? "Red=reference, green=moving (sliders switches views and Z)" : "Red=moving, green=reference (sliders switches views and Z)";
		ImagePlus []imgs=new ImagePlus[3];
		ImagePlus nullImage=VitimageUtils.setImageToValue(this.imgRef.duplicate(),0);
		imgs[0]=VitimageUtils.compositeOf(colorOrder ? imgRef : nullImage,colorOrder ? nullImage : imgRef);
		imgs[1]=VitimageUtils.compositeOf(colorOrder ? imgRef : imgResult, colorOrder ? imgResult : imgRef);
		imgs[2]=VitimageUtils.compositeOf(colorOrder ? nullImage : imgResult, colorOrder ? imgResult : nullImage);
		if(this.imgFus!=null)this.imgFus.close();
		this.imgFus=Concatenator.run(imgs);
		int slice=this.imgFus.getStackSize()/2;
		this.imgFus.show();
		this.imgFus.setTitle(ti);
		this.imgFus.setZ(this.imgRef.getStackSize()/2);
		this.imgFus.setT(2);
		this.imgFus.setSlice(this.imgFus.getStackSize()/2);
	}
	
	
	
	public void runManualRegistration() {
		GenericDialog gd= new GenericDialog("Select mode");
        gd.addChoice("Interaction mode", new String[] {"Grab objects in 3d","Point out correspondance points in 2d"},"Point out correspondance points in 2d");
        gd.showDialog();
        int method=gd.getNextChoiceIndex();
        int nbPoints;
        if(method==0)create3dInterface();
        if(method==1) {
    		gd= new GenericDialog("Select transformation estimated");
            gd.addChoice("Transformation type. \n\n-> Rigid-body is the most common case : translation + rotation, with no object deformations.\n-> Similarity allows to combine rigid-body with a global scaling operation.", 
            		new String[] {"Rigid-body","Similarity","Ultra-fine similarity"},"Rigid-body");
            gd.addCheckbox("In case of Similarity chosen, check this box if you estimate this similarity in order to fix the voxel sizes in the reference image)", false);
            gd.showDialog();
            int type=gd.getNextChoiceIndex();
            boolean voxelFixing=gd.getNextBoolean();
                       nbPoints=( 5 + 2 * type*type);//The more freedom, the more points to estimate
           
            Point3d [][]pointTab=getRegistrationLandmarks(nbPoints);
			Point3d []pRef=pointTab[0];
			Point3d []pMov=pointTab[1];
			ItkTransform trans=null;
			if(type==0) {
				trans=ItkTransform.estimateBestRigid3D(pMov,pRef);
				System.out.println("\nTransformation rigide calculée : "+trans.drawableString());
			}
			else{
				if(!voxelFixing)trans=ItkTransform.estimateBestSimilarity3D(pMov,pRef);
				else {
					//Estimer la variation en voxel sizes
					double dilationFactor=ItkTransform.estimateGlobalDilationFactor(pMov,pRef);
					trans=ItkTransform.estimateBestSimilarity3D(pMov,pRef);
					//Avertir l'utilisateur pour qu il mette a jour ses donnnees
					System.out.println("Please fix your moving image voxel size as follows : ");
					double[]oldVoxSize=VitimageUtils.getVoxelSizes(imgMov);
					double[]newVoxSize=TransformUtils.multiplyVector(oldVoxSize, dilationFactor);
					System.out.println(TransformUtils.stringVector(oldVoxSize, "Old voxel sizes"));
					System.out.println(TransformUtils.stringVector(newVoxSize, "New voxel sizes"));
					double maxXold=imgMov.getWidth()*oldVoxSize[0];
					double maxYold=imgMov.getHeight()*oldVoxSize[1];
					double maxZold=imgMov.getStackSize()*oldVoxSize[2];
					double maxXnew=imgMov.getWidth()*newVoxSize[0];
					double maxYnew=imgMov.getHeight()*newVoxSize[1];
					double maxZnew=imgMov.getStackSize()*newVoxSize[2];

					if(false) {
					//Appliquer l'ancienne chaine de transfos (avec similitude) sur les 8 coins du cube de l image avant changement voxel size. Relever les coordonnees. Ce sont les coordonnees cible actuelle (sert de ref)
					Point3d[]tabPtsCible=new Point3d[] { new Point3d(0,0,0) ,  new Point3d(0,0,maxZold) , new Point3d(0,maxYold,0) ,  new Point3d(0,maxYold,maxZold) ,
													new Point3d(maxXold,0,0) ,  new Point3d(maxXold,0,maxZold) , new Point3d(maxXold,maxYold,0) ,  new Point3d(maxXold,maxYold,maxZold)	};
					ItkTransform actualTransformOldVox=new ItkTransform(currentGlobalTransform);
					actualTransformOldVox.addTransform(trans);
					for(Point3d p : tabPtsCible)p=actualTransformOldVox.transformPoint(p);
					
					
					//Changer les voxel size
					VitimageUtils.adjustImageCalibration(this.imgMov, newVoxSize,this.imgMov.getCalibration().getUnit());

					//Appliquer l'ancienne chaine de transfos (sans la similitude) sur les 8 coins du cube de l image apres changement voxel size. Relever les coordonnees. Ce sont les coordonnees source actuelle (sert de mov)
					Point3d[]tabPtsSource=new Point3d[] { new Point3d(0,0,0) ,  new Point3d(0,0,maxZnew) , new Point3d(0,maxYnew,0) ,  new Point3d(0,maxYnew,maxZnew) ,
							new Point3d(maxXnew,0,0) ,  new Point3d(maxXnew,0,maxZnew) , new Point3d(maxXnew,maxYnew,0) ,  new Point3d(maxXnew,maxYnew,maxZnew)	};
					for(Point3d p : tabPtsSource)p=currentGlobalTransform.transformPoint(p);
									
					
					//Calculer la transformation rigide qui permet d'aller des coordonnees source aux coordonnees cible
					trans=ItkTransform.estimateBestRigid3D(tabPtsSource,tabPtsCible);					
					}
					else {
						ItkTransform avoxInv=new ItkTransform((ItkTransform.itkTransformFromCoefs(new double[] {1/dilationFactor,0,0,0,   0,1/dilationFactor,0,0,   0,0,1/dilationFactor,0})).getInverse());
						VitimageUtils.adjustImageCalibration(this.imgMov, newVoxSize,this.imgMov.getCalibration().getUnit());
						ItkTransform globTransfoInv=new ItkTransform(this.currentGlobalTransform.getInverse());
						ItkTransform Ah2=new ItkTransform();
						Ah2.addTransform(globTransfoInv);
						Ah2.addTransform(avoxInv);
						Ah2.addTransform(this.currentGlobalTransform);
						Ah2.addTransform(trans);
						trans=Ah2.simplify();
					}
					
				}
			}
			this.transResults.add(trans);
        }
        
    }

	
	
	
	public Point3d[][] getRegistrationLandmarks(int minimumNbWantedPointsPerImage){
		this.landmarksBasedManualRegistrationIsRunning=true;
		ImagePlus imgRefBis=imgRef.duplicate();		imgRefBis.getProcessor().resetMinAndMax();		imgRefBis.show();		imgRefBis.setTitle("Reference image");
		ImagePlus imgMovBis=imgResult.duplicate();	imgMovBis.getProcessor().resetMinAndMax();		imgMovBis.show();		imgMovBis.setTitle("Moving image");
		Point3d []pRef=new Point3d[1000];
		Point3d []pMov=new Point3d[1000];
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.setTool("point");
		IJ.showMessage("Examine images and click on at least "+(minimumNbWantedPointsPerImage*2)+" points to compute the correspondances,\n on both reference image and moving image (see image titles), then click on the button \n It's enough\""+
		"For each selected point, use the Roi Manager to save it, with \"add to manager\" option.\n Please follow the following order : "+
		"\n   Point 1 : item A on reference image\n    Point 2 : item A on moving image\n    Point 3 : item B on reference image\n Point 4 : item B on moving image \n...");
		boolean finished =false;
		do {
			if(rm.getCount()>=minimumNbWantedPointsPerImage*2 ) stopButton.setEnabled(true);
			try {
				java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if( !this.landmarksBasedManualRegistrationIsRunning)finished=true;
			System.out.println("Select at least "+(minimumNbWantedPointsPerImage*2)+" points, then click on the button \"It's enough\". Current number="+rm.getCount());
		}while (!finished);
		int nbCouples=0;
		for(int indP=0;indP<rm.getCount()/2;indP++){
			pRef[indP]=new Point3d(rm.getRoi(indP*2 ).getXBase() , rm.getRoi(indP * 2).getYBase() ,  rm.getRoi(indP * 2).getZPosition());
			pMov[indP]=new Point3d(rm.getRoi(indP*2 +1 ).getXBase() , rm.getRoi(indP * 2 +1 ).getYBase() ,  rm.getRoi(indP * 2 +1 ).getZPosition());
			pRef[indP]=TransformUtils.convertPointToRealSpace(pRef[indP],imgRef);
			pMov[indP]=TransformUtils.convertPointToRealSpace(pMov[indP],imgRef);
			nbCouples++;
		}
		System.out.println("Number of correspondance pairs = "+nbCouples);
		imgRefBis.close();
		imgMovBis.close();
		Point3d []pRefRet=new Point3d[nbCouples];		Point3d []pMovRet=new Point3d[nbCouples];
		for(int i=0;i<pRefRet.length;i++) {
			pRefRet[i]=pRef[i];
			pMovRet[i]=pMov[i];
		}
		return new Point3d[][] {pRefRet,pMovRet};
	}
	
	
	
	
    public void create3dInterface() {
		volumeRenderingManualRegistrationIsRunning=true;
    	this.univ=new ij3d.Image3DUniverse();
		univ.show();
		univ.addContent(imgRef, new Color3f(Color.red),"imgRef",50,new boolean[] {true,true,true},1,0 );
		univ.addContent(imgResult, new Color3f(Color.green),"imgMov",50,new boolean[] {true,true,true},1,0 );
		ij3d.ImageJ3DViewer.select("imgRef");
		ij3d.ImageJ3DViewer.setThreshold("50");
		ij3d.ImageJ3DViewer.lock();
		ij3d.ImageJ3DViewer.select("imgMov");
		ij3d.ImageJ3DViewer.setThreshold("50");
		VitiDialogs.getYesNoUI("Red volume is fixed, green volume can move.\n Use the 3d viewer to adjust the green volume with the red volume using the mouse.\n Click-drag=rotate , Shift+Click-drag=translate.");
		stopButton.setEnabled(true);
		this.setButtonLocked(true);
    }

    public void close3dInterfaceAndWriteCorrespondingTransform() {
    	if(this.univ ==null)return;
		Transform3D tr=new Transform3D();
		double[]tab=new double[16];

		univ.getContent("imgMov").getLocalRotate().getTransform(tr);
		tr.get(tab);
		ItkTransform itRot=ItkTransform.array16ElementsToItkTransform(tab);

		univ.getContent("imgMov").getLocalTranslate().getTransform(tr);
		tr.get(tab);
		ItkTransform itTrans=ItkTransform.array16ElementsToItkTransform(tab);
		itTrans.addTransform(itRot);
		itTrans=itTrans.simplify();

		System.out.println("Global transform computed : "+itTrans);
		this.transResults.add(new ItkTransform(itTrans.getInverse()));
		univ.removeAllContents();
		univ.close();
    	this.univ=null;    
    	volumeRenderingManualRegistrationIsRunning=false;
    }
        
	public void loadCustomMask() {
		if(VitiDialogs.getYesNoUI("Loading mask image. Is your image ready ?"))this.imgMask=IJ.openImage();
	}
	
	public void runAutomaticRegistration() {
		Transformation3DType transType;
		int method;
		int duration;
		MetricType metr;
		int levelMax;
		int levelMin;
		int blockSize;
		int neighSize;
		boolean displayRegistration=true;
		boolean displayR2=true;
		GenericDialog gd= new GenericDialog("Select mode");
        gd.addChoice("Registration method ", new String[] {"Block-Matching","Iconic"},"Block-Matching");
        gd.addChoice("Transformation estimated ", new String[] {"Translation","Rigid-body","Similarity","Affine","Dense field"},"Rigid-body");
        gd.addChoice("Similarity measure ", new String[] {"Squared difference (don t go there)","Correlation coefficient (ok for monomodal)","Squared correlation coefficient (ok for multimodal too)","Mutual information (not with block matching)"},"Correlation coefficient (ok for monomodal)");
        gd.addChoice("Pyramid starting level", new String[] {"4 (very high subsampling, more dangerous)","3 (high subsampling)","2 (subsampling)","1 (image as it)"},"1");
        gd.addChoice("Pyramid last level", new String[] {"2 (subsampling)","1 (image as it)","0 (oversampling)","-1 (high oversampling)"},"1");
        gd.addChoice("Optimization duration", new String[] {"Not too long","A while","Numerous iterations"},"A while");
        gd.addChoice("BM : Block size (for blocks comparison)", new String[] {"5x5xn (little blocks, lower significativity, but faster)","7x7xn ","9x9xn ","11x11xn ","13x13xn (big blocks, higher significativity)","15x15xn (big blocks, higher significativity, but slower)"},"9x9xn ");
        gd.addChoice("BM : Neighbourhood size for finding correspondance (in pixels)", new String[] {"1 (little, but faster)","2 (just enough)","3 (bigger neighbourhood)","4","5(big neighbourhood, but slower)"},"1");
        gd.addCheckbox("Display current state during Registration ? (if no, 2 times faster, but you cannot monitor what is going on)", true);
        gd.addCheckbox("Display successive R^2 during Registration ? (if no, again 2 times faster, but you really cannot monitor what is going on)", true);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        method=gd.getNextChoiceIndex();

        switch(gd.getNextChoiceIndex()) {
        case 0 : transType=Transformation3DType.TRANSLATION;break;
        case 1 : transType=Transformation3DType.VERSOR;break;
        case 2 : transType=Transformation3DType.SIMILARITY;break;
        case 3 : transType=Transformation3DType.AFFINE;break;
        case 4 : transType=Transformation3DType.DENSE;break;
        default : transType=Transformation3DType.VERSOR;break;
        }
        
        switch(gd.getNextChoiceIndex()) {
        case 0 : metr=MetricType.MEANSQUARE;break;
        case 1 : metr=MetricType.CORRELATION;break;
        case 2 : metr=MetricType.SQUARED_CORRELATION;break;
        case 3 : metr=MetricType.MATTES;break;
        default : metr=MetricType.SQUARED_CORRELATION;break;
        }
  
        levelMax=4-gd.getNextChoiceIndex();        
        levelMin=2-gd.getNextChoiceIndex();
        duration=gd.getNextChoiceIndex();
        blockSize=5+2*gd.getNextChoiceIndex();
        neighSize=1+gd.getNextChoiceIndex();
        displayRegistration=gd.getNextBoolean();
        displayR2=gd.getNextBoolean();
        
        if(method==0) {
        	double varMin=0.05;
//        	varMin=VitiDialogs.getYesNoUI("Use the assistant for minimal variance determination ?") ? runAssistantMinimumVariance(): VitiDialogs.getDoubleUI("Set minimum block variance","Variance",0.05);
        	double sigma=0.8;
 //       	if(transType==Transformation3DType.DENSE)sigma=VitiDialogs.getYesNoUI("Use the assistant for spatial field smoothing factor determination ?") ? runAssistantSigma() : VitiDialogs.getDoubleUI("Set smoothing factor","smoothing",0.05);
        	this.transResults.add(BlockMatchingRegistration.setupAndRunRoughBlockMatchingWithoutFineParameterization(imgRef, imgResult, imgMask, transType,metr,levelMax,levelMin,blockSize,neighSize,varMin,sigma,duration,displayRegistration,displayR2,100, false));
        	if(transType==Transformation3DType.DENSE)thereIsDenseField=true;
        }
        if(method==1) {
        	this.transResults.add(new ItkTransform());VitiDialogs.notYet("Not yet implemented : ITK iconic registration scheme access from J3DRegistration");
        }
        System.out.println("Finished !");
        
	}
	
	
	
	 public void saveRegisteredImages() {
		 if( VitiDialogs.getYesNoUI("Do you want to export images and transform in a reference geometry ?\n A reference geometry is a standard setup to uniformize your data \nto make future experiments and publications easier.\n\n"+
				 					"In plant science, common setups are to align plant axis with x, y or z :\n -> Geometry 1 make plants appearing with axis aligned with the z axis...\n -> Geometry 2 make plants appearing with axis aligned with x axis"+
				 "\n -> Geometry 3 make plants appearing with axis aligned with the y axis...")) {
			 computePostReferenceGeometryAndExport();
			 return; 
		 }
		 
		 VitiDialogs.saveImageUI(imgResult, "Save registered moving image as tif...",false , "", "imgMov_after_registration.tif");
		 if(VitiDialogs.getYesNoUI("Also save reference image ? (useful if you switched ref and mov, or if you used the function \"set reference geometry\")"))	{
				 VitiDialogs.saveImageUI(imgRef, "Save reference image as tif...",false , "", "imgRef_after_registration.tif");
		 }
		 if(VitiDialogs.getYesNoUI("Save transform, by the way ? (useful to reproduce results easily)"))	{
			if(this.thereIsDenseField) VitiDialogs.saveDenseFieldTransformUI(this.currentGlobalTransform.flattenDenseField(imgRef), "Save transform as a dense field in a file *.transform.tif", false, "","result_transform", this.imgRef);
			else VitiDialogs.saveMatrixTransformUI(this.currentGlobalTransform.simplify(), "Save transform as a ITK matrix in a file *.txt", false, "","result_transform");
		 }
	 }
		
	 
	 public void computePostReferenceGeometryAndExport() {
			GenericDialog gd= new GenericDialog("Select geometry");
	        gd.addChoice("Geometry", new String[] {"Plant axis along Z axis"},"Plant axis along Z axis");
	        gd.showDialog();
	        int method=gd.getNextChoiceIndex();
	        ItkTransform tr=new ItkTransform();
	        boolean geometryIsOk=false;
	        if(method==0) {
	        	ImagePlus imgTemp;
	        	do {
	        		Point3d plusX=new Point3d(1,0,0);
	        		Point3d plusY=new Point3d(0,1,0);
	        		Point3d plusZ=new Point3d(0,0,1);
	        		Point3d vectZ;
	        		Point3d vectY;
	        		tr=new ItkTransform();
		        	//Step 1 : center of interest
		        	IJ.showMessage("Select the center of your interest zone (one point) then use the roi manager to validate it");
		        	Point3d centerPointBefore=VitimageUtils.convertDoubleArrayToPoint3dArray(VitiDialogs.waitForReferencePointsUI(1,imgRef,true))[0];
		        	Point3d centerPointAfter=TransformUtils.convertPointToRealSpace(new Point3d(imgRef.getWidth()/2,imgRef.getHeight()/2,imgRef.getStackSize()/2), imgRef);
		        	Point3d[] ptsBefore=new Point3d[] { centerPointBefore,centerPointBefore.plus(plusX),centerPointBefore.plus(plusY),centerPointBefore.plus(plusZ) };
		        	Point3d[] ptsAfter=new Point3d[] { centerPointAfter,centerPointAfter.plus(plusX),centerPointAfter.plus(plusY),centerPointAfter.plus(plusZ) };
		        	tr.addTransform(ItkTransform.estimateBestRigid3D(ptsBefore,ptsAfter));
		        	imgTemp=tr.transformImage(imgRef, imgRef);
	        		imgTemp.setTitle("Reference point is set. Computing Z axis");

		        	
		        	//Step 1 : align axis
    				IJ.showMessage("Select two points defining the Z axis :\n -> First point up (first slices)\n -> Second point down (last slices)\n click on \"add to roi manager\" after selecting each point");
		        	Point3d[] ptsTempBef=VitimageUtils.convertDoubleArrayToPoint3dArray(VitiDialogs.waitForReferencePointsUI(2,imgTemp,true));
	        		vectZ=TransformUtils.doubleArrayToPoint3d(TransformUtils.normalize(TransformUtils.vectorialSubstraction( 
	        				TransformUtils.point3dToDoubleArray(ptsTempBef[1]),
	        				TransformUtils.point3dToDoubleArray(ptsTempBef[0]))));
	        		ptsBefore=new Point3d[] { centerPointBefore,centerPointBefore.plus(plusX),centerPointBefore.plus(vectZ) };
	        		ptsAfter=new Point3d[] { centerPointBefore,centerPointBefore.plus(plusX),centerPointBefore.plus(plusZ) };		        	
		        	tr.addTransform(ItkTransform.estimateBestRigid3D(ptsBefore,ptsAfter));
		        	imgTemp=tr.transformImage(imgRef, imgRef);
	
		        	
		        	//Step 2 : align radial
		        	IJ.showMessage("On your favourite slice, select two points to define the future Y axis\n. A transformation will be computed in order to \n set these points aligned horizontally in your screen,\n with the first point up in your screen, the second point down in your screen");
		        	ptsTempBef=VitimageUtils.convertDoubleArrayToPoint3dArray(VitiDialogs.waitForReferencePointsUI(2,imgTemp,true));
	        		vectY=TransformUtils.doubleArrayToPoint3d(TransformUtils.normalize(TransformUtils.vectorialSubstraction( 
	        				TransformUtils.point3dToDoubleArray(ptsTempBef[1]),
	        				TransformUtils.point3dToDoubleArray(ptsTempBef[0]))));
	        		ptsBefore=new Point3d[] { centerPointBefore,centerPointBefore.plus(plusZ),centerPointBefore.plus(vectY) };
	        		ptsAfter=new Point3d[] { centerPointBefore,centerPointBefore.plus(plusZ),centerPointBefore.plus(plusY) };		        	
		        	tr.addTransform(ItkTransform.estimateBestRigid3D(ptsBefore,ptsAfter));
		        	imgTemp=tr.transformImage(imgRef, imgRef);
		        	
		        	VitimageUtils.imageChecking(imgTemp);
		        	imgTemp.show();
		        	geometryIsOk=VitiDialogs.getYesNoUI("Confirm geometry ?");
	        	}while(!geometryIsOk);
	        	imgTemp.close();
	        	ItkTransform glob=new ItkTransform(this.currentGlobalTransform);
	        	glob.addTransform(tr);	        	
	        	VitiDialogs.saveImageUI(glob.transformImage(imgRef, imgMov), "Save registered moving image in reference geometry as tif...",false , "", "imgMov_after_registration_in_reference_geometry.tif");
	        	VitiDialogs.saveImageUI(tr.transformImage(imgRef, imgRef), "Save reference image in reference geometry as tif...",false , "", "imgRef_after_registration_in_reference_geometry.tif");
	        	if(this.thereIsDenseField) {
	        		VitiDialogs.saveDenseFieldTransformUI(this.currentGlobalTransform.flattenDenseField(imgRef), "Save transform from mov to ref in a file *.transform.tif", false, "","mov_to_ref", this.imgRef);
	        		VitiDialogs.saveDenseFieldTransformUI(glob.flattenDenseField(imgRef), "Save transform from mov to ref then reference as a dense field in a file *.transform.tif", false, "","mov_to_ref_then_reference_geometry", this.imgRef);
	        		VitiDialogs.saveMatrixTransformUI(tr, "Save transform to reference geometry in a file *.txt", false, "","to_reference_geometry");
	        	}
	        	else {
	        		VitiDialogs.saveMatrixTransformUI(this.currentGlobalTransform.simplify(), "Save transform from mov to ref as a ITK matrix in a file *.txt", false, "","mov_to_ref");
	        		VitiDialogs.saveMatrixTransformUI(glob.simplify(), "Save transform from mov to ref then reference in a file *.txt", false, "","mov_to_ref_then_reference_geometry");
	        		VitiDialogs.saveMatrixTransformUI(tr, "Save transform to reference geometry in a file *.txt", false, "","to_reference_geometry");
	        	}
	        }
	 }
	 
	 
	public void loadTransform() {
		 ItkTransform tr=VitiDialogs.chooseOneTransformsUI("Select_transformation ( *.transform.tif  or *.txt )","",false);
		 if(tr==null)return;
		 this.transResults.add(tr);		 
	 }
	
	
	public void switchReference() {
		ImagePlus temp=imgMov.duplicate();
		imgMov=imgRef.duplicate();
		imgRef=temp;
		if(thereIsDenseField)this.currentGlobalTransform=this.currentGlobalTransform.flattenDenseField(imgRef);
		else this.currentGlobalTransform=this.currentGlobalTransform.simplify();
		this.currentGlobalTransform=new ItkTransform(this.currentGlobalTransform.getInverse());
		this.transResults.clear();
		this.transResults.add(this.currentGlobalTransform);
		this.invertedRealized=true;//No more undo possible
	}
	
	
	
	public void undo() {
		if(this.invertedRealized)IJ.log("Sorry, but after switching reference and moving, it is no more possible to undo actions");
		this.transResults.remove(transResults.size()-1);
	}
	
	public void updateGlobalTransformAndMovingImage() {
		if(this.imgFus!=null)this.imgFus.hide();
		ItkTransform tr=new ItkTransform();
		for(ItkTransform t : this.transResults)tr.addTransform(t);
		
		this.currentGlobalTransform=tr;
		System.out.println("Current transform="+tr.simplify().drawableString());
		this.imgResult=this.currentGlobalTransform.transformImage(this.imgRef, this.imgMov);
	}
	
	
	
	public void startGUI() {
		System.out.println("Starting GUI...");
	if(imgRef == null || imgMov==null) {IJ.log("Null image imgRef or imgMov. Exit");System.exit(0);}
	switchColorButton = new JButton("Switch colors between reference and moving");
	switchColorButton.setToolTipText("Switch colors between reference and moving");
	switchColorButton.setEnabled(true);
	
	changeLUTButton = new JButton("Change colors mode ");
	changeLUTButton.setToolTipText("Mode 1 : Red/Green, mode 2 : White/Fire");
	changeLUTButton.setEnabled(true);
	
	saveButton = new JButton("Save current transform");
	saveButton.setToolTipText("save the transform into a readable file");
	saveButton.setEnabled(true);
	
	switchRefButton = new JButton("Switch reference and moving images");
	switchRefButton.setToolTipText("Specially useful after having process a lot of complex transforms between a huge moving image and a tiny reference image.\nUsing this tool, you switch reference and moving, and you get at the same time access to the inverse transform");
	switchRefButton.setEnabled(true);
	
	loadButton = new JButton("Load transform");
	loadButton.setToolTipText("Load and apply to moving image a transform previously computed");
	loadButton.setEnabled(true);
	
	stopButton = new JButton("It's enough");
	stopButton.setToolTipText("Click here when running manual registration, to inform the system that you finished your job and want to proceed");
	stopButton.setEnabled(false);

	manRegistrationButton = new JButton("Compute a first manual registration or adjust voxel size");
	manRegistrationButton.setToolTipText("Run an interface to make a first rough alignment of the two images. This can also be use to fix some voxel problems on the moving image.");
	manRegistrationButton.setEnabled(true);
	
	autoRegistrationButton = new JButton("Compute automatic registration ");
	autoRegistrationButton.setToolTipText("Run an automatic algorithm for computing a fine registration of moving image on reference image");
	autoRegistrationButton.setEnabled(true);
	
	undoButton = new JButton("Undo last registration step ");
	undoButton.setToolTipText("Undo the last registration computed, and restore the previous state");
	undoButton.setEnabled(true);
	
	useMaskButton = new JButton("Load a custom mask ");
	useMaskButton.setToolTipText("You can use it to load a custom 8 bit image with pixels set to 0 shows regions not used for registration");
	useMaskButton.setEnabled(true);
	
	ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.POINT);
	
	//Build GUI
	this.toolbar= new JFrame();
	this.toolbar.setTitle("J3DRegistrationStudio");
	JPanel buttonsPanel = new JPanel();
	JPanel colorPanel=new JPanel();
	JPanel registrationPanel=new JPanel();
	JPanel transformPanel=new JPanel();
	
	// Color panel
	colorPanel.setBorder(BorderFactory.createTitledBorder("Color options"));
	GridBagLayout colorLayout = new GridBagLayout();
	GridBagConstraints colorConstraints = new GridBagConstraints();
	colorConstraints.anchor = GridBagConstraints.NORTHWEST;
	colorConstraints.fill = GridBagConstraints.HORIZONTAL;
	colorConstraints.gridwidth = 1;
	colorConstraints.gridheight = 1;
	colorConstraints.gridx = 0;
	colorConstraints.gridy = 0;
	colorConstraints.insets = new Insets(5, 5, 6, 6);
	colorPanel.setLayout(colorLayout);
	colorPanel.add(switchColorButton, colorConstraints);
	colorConstraints.gridy++;
	colorPanel.add(changeLUTButton, colorConstraints);
	colorConstraints.gridy++;
				
	
	// Registration panel
	registrationPanel.setBorder(BorderFactory.createTitledBorder("Registration steps"));
	GridBagLayout registrationLayout = new GridBagLayout();
	GridBagConstraints registrationConstraints = new GridBagConstraints();
	registrationConstraints.anchor = GridBagConstraints.NORTHWEST;
	registrationConstraints.fill = GridBagConstraints.HORIZONTAL;
	registrationConstraints.gridwidth = 1;
	registrationConstraints.gridheight = 1;
	registrationConstraints.gridx = 0;
	registrationConstraints.gridy = 0;
	registrationConstraints.insets = new Insets(5, 5, 6, 6);
	registrationPanel.setLayout(registrationLayout);
	registrationPanel.add(manRegistrationButton, registrationConstraints);
	registrationConstraints.gridy++;
	registrationPanel.add(autoRegistrationButton, registrationConstraints);
	registrationConstraints.gridy++;
	registrationPanel.add(undoButton, registrationConstraints);
	registrationConstraints.gridy++;
	registrationConstraints.gridy++;
	registrationPanel.add(stopButton, registrationConstraints);
				
	
	// Transform panel
	transformPanel.setBorder(BorderFactory.createTitledBorder("Tranformations and image manager"));
	GridBagLayout transformLayout = new GridBagLayout();
	GridBagConstraints transformConstraints = new GridBagConstraints();
	transformConstraints.anchor = GridBagConstraints.NORTHWEST;
	transformConstraints.fill = GridBagConstraints.HORIZONTAL;
	transformConstraints.gridwidth = 1;
	transformConstraints.gridheight = 1;
	transformConstraints.gridx = 0;
	transformConstraints.gridy = 0;
	transformConstraints.insets = new Insets(5, 5, 6, 6);
	transformPanel.setLayout(registrationLayout);
	transformPanel.add(useMaskButton, transformConstraints);
	transformConstraints.gridy++;
	transformPanel.add(saveButton, transformConstraints);
	transformConstraints.gridy++;
	transformPanel.add(loadButton, transformConstraints);
	transformConstraints.gridy++;
	transformPanel.add(switchRefButton, transformConstraints);
	transformConstraints.gridy++;
	
	// Add listeners
	switchColorButton.addActionListener(listener);
	changeLUTButton.addActionListener(listener);
	saveButton.addActionListener(listener);
	loadButton.addActionListener(listener);
	useMaskButton.addActionListener(listener);
	switchRefButton.addActionListener(listener);
	manRegistrationButton.addActionListener(listener);
	autoRegistrationButton.addActionListener(listener);
	undoButton.addActionListener(listener);
	stopButton.addActionListener(listener);
				
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
	buttonsPanel.add(colorPanel, buttonsConstraints);
	buttonsConstraints.gridy++;
	buttonsPanel.add(transformPanel, buttonsConstraints);
	buttonsConstraints.gridy++;
	buttonsPanel.add(registrationPanel, buttonsConstraints);
	buttonsConstraints.gridy++;
	buttonsConstraints.insets = new Insets(5, 5, 6, 6);
	
	this.toolbar.add(buttonsPanel);
				
	// Propagate all listeners
		for (KeyListener kl : this.toolbar.getKeyListeners()) {
			buttonsPanel.addKeyListener(kl);
		}
		
		this.toolbar.pack();
		this.toolbar.setVisible(true);
	}	
	
	



	
}
