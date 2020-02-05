package com.vitimage;

import com.vitimage.CenteringStrategy;
import com.vitimage.MetricType;
import com.vitimage.OptimizerType;
import com.vitimage.SamplingStrategy;
import com.vitimage.ScalerType;
import com.vitimage.Transform3DType;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;

public class APLIM_Studio  implements PlugIn,ItkImagePlusInterface,VitiDialogs,VitimageUtils {

	
	public static final String []tools= {"Maida flip-flop registration tool","Transform friend image with pre-computed transformation"};
	public static final int TOOL_MAIDA=0;
	public static final int TOOL_TRANSFORM=1;
	public APLIM_Studio() {
	}

	public static double specialRound(double a) {
		return (0.01*Math.round(100*(a+10E-10)));		
	}
	
	
	public static double computeValue(int N,int []c,double test) {
		double []prices=new double[N];
		prices[0]=specialRound(test);
		double valMoy=prices[0];
		for(int i=1;i<N;i++) {
			prices[i]=specialRound(prices[i-1]*(100+c[i-1])/100.0);
			valMoy+=prices[i];
		}
		valMoy/=N;
		return valMoy;
	}
	

	public static double getPrice(int N,int[]c,int target){
		double test=100;
		double modif=50;
		boolean found=false;
		double resultMoy=0;
		int iteration=0;
		while(!found) {
			System.out.println("Iteration "+(iteration++));
			System.out.println("Test="+test);
			System.out.println("modif="+modif);
			resultMoy=computeValue(N,c,test);
			System.out.println("resultMoy="+resultMoy);
			if(Math.abs(resultMoy-target)<0.005)found=true;
			else if(resultMoy<target)test=test+modif;
			else if(resultMoy>target)test=test-modif;
			modif=modif*0.8;
		}
		return test;
	}


	public static void main(String[] args) {
		int N=5;
		int[]c=new int[] {10,20,-20,-10};
		int target=182;
		double d=getPrice(N,c,target);
		System.out.println("Target value="+d);
		
		
		/*		ImageJ ij=new ImageJ();
		APLIM_Studio apls=new APLIM_Studio();
		apls.startPlugin();*/
	}
	public void run(String arg) {
		this.startPlugin();
	}

	
	public void startPlugin() {
		int choiceInt=this.chooseToolUI();
		System.out.println("Tool chosen : "+tools[choiceInt]);
		switch(choiceInt) {
		case TOOL_MAIDA:runMaidaRegistration();break;
		case TOOL_TRANSFORM:runTransformFriendImage();break;
		}
	}

	
	/**
	/* Tool chooser
	 * */
	private int chooseToolUI(){
	        GenericDialog gd= new GenericDialog("Select mode");
	        gd.addChoice("Tool ", tools,tools[0]);
			gd.showDialog();
	        if (gd.wasCanceled()) return -1;
	        else return (gd.getNextChoiceIndex());
	}

	
	public void runMaidaRegistration() {
		//Images opening. Handling the case of monoslice images by duplicating the slices
		ImagePlus[]imgs=VitiDialogs.chooseTwoImagesUI("Choose reference image and moving image","Reference","Moving");
		ImagePlus imgRef=imgs[0];
		ImagePlus imgMov=imgs[1];
		
		int nbSlicesRef=imgRef.getStackSize();
		if(nbSlicesRef==1) {
			String titl=imgRef.getTitle();
			imgRef=Concatenator.run(new ImagePlus[] {imgRef,imgRef,imgRef,imgRef});
			imgRef.setTitle(titl);
		}
		int nbSlicesMov=imgMov.getStackSize();
		if(nbSlicesMov==1) {
			String titl=imgMov.getTitle();
			imgMov=Concatenator.run(new ImagePlus[] {imgMov,imgMov,imgMov,imgMov});
			imgMov.setTitle(titl);
		}
		String refTitle=imgRef.getShortTitle();
		String movTitle=imgMov.getShortTitle();
		

		//ItkRegistration setup
		ItkRegistrationManager manager=new ItkRegistrationManager();
		manager.setTextInfoAtEachIterationOn();
		manager.setReferenceImage(imgRef);
		manager.setMovingImage(imgMov);
		manager.setViewSlice(imgRef.getStackSize()/2);
		manager.setMetric(MetricType.CORRELATION);
//		OptimizerType opt=OptimizerType.AMOEBA;
		OptimizerType opt=OptimizerType.ITK_AMOEBA;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;

		
		//Registration steps setup
		manager.addStepToQueue( 2 ,   2    ,     0    ,   20 , 0.4   ,       Transform3DType.TRANSLATION,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		true,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );


		//Registration execution
		manager.register();
		//manager.showRegistrationSummary();
			
		
		//Results handling
		ItkTransform resTrans=manager.getCurrentTransform();
		
		ImagePlus result=resTrans.transformImage(imgRef,imgMov,false);
		if(nbSlicesMov==1) {
			result=new Duplicator().run(result,1,1);
			imgMov=new Duplicator().run(imgMov,1,1);
		}
		if(nbSlicesRef==1) {
			imgRef=new Duplicator().run(imgRef,1,1);
		}
		
		result.getProcessor().setMinAndMax(imgMov.getProcessor().getMin(),imgMov.getProcessor().getMax());
		result.show();
		System.out.println("Result transformation : \n"+resTrans.drawableString());
		if(VitiDialogs.getYesNoUI("","Do you want to save the computed transformation ?"))VitiDialogs.saveMatrixTransformUI(
											resTrans, "Save the computed transformation", SUPERVISED, "", "Mat_transform_"+movTitle+"_to_"+refTitle);
		if(VitiDialogs.getYesNoUI("","Do you want to save the resulting transformed image ?"))VitiDialogs.saveImageUI(
											result,"Save the result image",SUPERVISED,"", "Result image_"+movTitle+"_to_"+refTitle+".tif"); 
	
		if(VitiDialogs.getYesNoUI("","Do you want to visualize the Before/After composite image ?")){

			ImagePlus refDup=new Duplicator().run(imgRef);
			ImagePlus movDup=new Duplicator().run(imgMov);
			ImagePlus imgBefore=VitimageUtils.compositeOf(refDup,movDup);
						
			refDup=new Duplicator().run(imgRef);
			ImagePlus resDup=new Duplicator().run(result);
			ImagePlus imgAfter=VitimageUtils.compositeOf(refDup,resDup);

			ImagePlus beforeAfterImg=Concatenator.run(new ImagePlus[] {imgBefore,imgAfter});
			VitimageUtils.adjustImageCalibration(beforeAfterImg, imgRef);
			beforeAfterImg.setTitle("Before / After");
			beforeAfterImg.show();
		}
	}

		
		
		
	
	

	public void runTransformFriendImage() {		
		ImagePlus[]imgs=VitiDialogs.chooseTwoImagesUI("Choose reference image and moving image","Reference (used as reference image space)","Moving image (to be transformed)");
		ItkTransform trans=VitiDialogs.chooseOneTransformsUI("Choose transform to apply","", SUPERVISED);
		ImagePlus result=trans.transformImage(imgs[0],imgs[1],false);
		result.getProcessor().setMinAndMax(imgs[1].getProcessor().getMin(),imgs[0].getProcessor().getMax());
		result.show();
		if(VitiDialogs.getYesNoUI("","Do you want to save the resulting transformed image ?"))VitiDialogs.saveImageUI(
				result,"Save the result image",SUPERVISED,"", "Result image.tif"); 

	}
		
	
	
}
