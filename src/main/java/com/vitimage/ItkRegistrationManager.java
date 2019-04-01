package com.vitimage;

import java.util.ArrayList;

import javax.naming.ldap.ManageReferralControl;

import org.itk.simple.AffineTransform;
import org.itk.simple.CenteredTransformInitializerFilter;
import org.itk.simple.Command;
import org.itk.simple.Euler2DTransform;
import org.itk.simple.Euler3DTransform;
import org.itk.simple.EventEnum;
import org.itk.simple.Image;
import org.itk.simple.ImageFileReader;
import org.itk.simple.ImageFileWriter;
import org.itk.simple.ImageRegistrationMethod;
import org.itk.simple.ResampleImageFilter;
import org.itk.simple.RescaleIntensityImageFilter;
import org.itk.simple.Similarity3DTransform;
import org.itk.simple.TranslationTransform;
import org.itk.simple.VectorDouble;
import org.itk.simple.VectorUInt32;
import org.itk.simple.VersorRigid3DTransform;
import org.itk.simple.ImageRegistrationMethod.MetricSamplingStrategyType;

import com.vitimage.ItkImagePlusInterface.MetricType;
import com.vitimage.ItkImagePlusInterface.OptimizerType;
import com.vitimage.ItkImagePlusInterface.Transformation3DType;

import org.itk.simple.InterpolatorEnum;
import org.itk.simple.RecursiveGaussianImageFilter;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;

public class ItkRegistrationManager implements ItkImagePlusInterface{
	boolean lookLikeOptimizerLooks=false;
	boolean movie3D=false;
	CenteredTransformInitializerFilter centerTransformFilter;
	private RecursiveGaussianImageFilter gaussFilter;
	private ResampleImageFilter resampler;
	private double[]voxelSizeReference;
	private int[]imageSizeReference;
	private Image itkImgViewRef;
	private Image itkImgViewMov;
	private Image itkSummaryRef;
	private Image itkSummaryMov;
	private ImagePlus sliceViewMov;
	private ImagePlus sliceViewRef;
	private ImagePlus sliceView;
	private ImagePlus sliceSummaryRef;
	private ImagePlus sliceSummaryMov;
	private ImagePlus summary;
	private ArrayList<ImagePlus> imgMovSuccessiveResults;
	private int viewWidth;
	private int viewHeight;
	private int viewSlice;
	private int zoomFactor;
	private IterationUpdate updater;
	private ArrayList<Integer> nbLevels;
	private ArrayList<int[]> shrinkFactors;
	private ArrayList<int[][]> dimensions;
	private ArrayList<double[]> sigmaFactors;
	private int currentLevel;
	private int basis=2;//shrink factor at each level

	private ArrayList<CenteringStrategy> centeringStrategies;
	private ArrayList<Transformation3DType> transformation3DTypes;
	private ArrayList<ScalerType>scalerTypes;
	private int nbStep;
	private int currentStep;
	private MetricType metricType;
	private ArrayList<ImageRegistrationMethod> registrationMethods;
	public ImagePlus ijImgRef;
	private Image itkImgRef;
	private ImagePlus ijImgMov;
	private Image itkImgMov;
	private ArrayList<ImagePlus> registrationSummary;
	private ItkTransform transform;
	private ArrayList<double[]> weights;
	private ArrayList<double[]> scales;
	private ArrayList<double[]> voxelSizes;
	private int fontSize=12;


	
	
	/** 
	 * Top level functions : Test function, and main scenarios that will be used by customers classes
	 */
	public int runTestSequence() {
		
		//MATTES256 SUR VERSOR 2 3 PUIS SIMILITUDE 1 2 , avec AMOEBA et sigma 0.8 , 30  0.5 --> de la tuerie
		int nbFailed=0;
		ItkRegistrationManager manager=new ItkRegistrationManager();
		ImagePlus []imgTab=VitiDialogs.chooseTwoImagesUI("Choose reference image and moving image to register\n\n","Reference image","Moving image");
		manager.movie3D=VitiDialogs.getYesNoUI("Compute 3D summary ?");
		ImagePlus imgRef=imgTab[0];
		ImagePlus imgMov=imgTab[1];
		manager.setMovingImage(imgMov);
		manager.setReferenceImage(imgRef);
		manager.setViewSlice(20);
		manager.setMetric(MetricType.MATTES);
		OptimizerType opt=OptimizerType.AMOEBA ;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
	/*	addStepToQueue( 30 20
				int levelMin,int levelMax,double sigma,Transformation3DType typeTransfo,double[]weights,
				OptimizerType optimizerType,ScalerType scalerType,double[]scales, 
				boolean doubleIterAtCoarsestLevels,CenteringStrategy centeringStrategy,SamplingStrategy samplingStrategy
				);*/
		manager.addStepToQueue( 1 ,     4     ,     1     ,    2*2  , 0.8   ,       Transformation3DType.VERSOR,    null,
						opt  , ScalerType.SCALER_PHYSICAL, null ,
				false,         CenteringStrategy.MASS_CENTER,    samplStrat  );
		
		manager.addStepToQueue( 1 ,    2     ,     0.8     ,   3*2,  0.8,     Transformation3DType.SIMILARITY,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
				false,         CenteringStrategy.NONE,    samplStrat  );

		
		manager.register();
		manager.showRegistrationSummary();
		//ImagePlus resultat=manager.computeRegisteredImage();
		//resultat.show();
		
		return nbFailed;
	}

	public void runScenarioInterSpecimen(Vitimage4D reference,Vitimage4D moving) {
		
		
	}
	
	public  ItkTransform runScenarioInterTime(ImagePlus imgRef, ImagePlus imgMov) {
		this.setMovingImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgMov));
		this.setReferenceImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgRef));
		this.setViewSlice(imgRef.getStackSize()/2);
		this.setMetric(MetricType.MATTES);
		OptimizerType opt=OptimizerType.AMOEBA ;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
		int dimMinPyramide=3;
		int dimMinImage=Math.min(imgRef.getWidth()  , Math.min( imgRef.getHeight()  , imgRef.getStackSize() ) );

		this.addStepToQueue( 1 ,    2    ,     1     ,    100  , 0.3   ,       Transformation3DType.VERSOR,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( 1 ,   2    ,     1     ,    100  , 0.3   ,       Transformation3DType.SIMILARITY,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.register();
		this.showRegistrationSummary();
		return this.transform;		
	}
	

	public ImagePlus runScenarioMaidaFlipFlop(ImagePlus imgRef, ImagePlus imgMov) {
		this.setMovingImage(imgMov);
		this.setReferenceImage(imgRef);
		this.setViewSlice(imgRef.getStackSize()/2);
		this.setMetric(MetricType.CORRELATION);
		OptimizerType opt=OptimizerType.GRADIENT_LINE_SEARCH;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
		int dimMinPyramide=3;
		int dimMinImage=Math.min(imgRef.getWidth()  , Math.min( imgRef.getHeight()  , imgRef.getStackSize() ) );
		int levelMax=1;//1+(int)Math.floor(Math.log(dimMinImage/dimMinPyramide)/Math.log(2) );
		int levelMin=1;

		this.addStepToQueue( 1 ,    1    ,     0    ,   200 , 0.4   ,       Transformation3DType.TRANSLATION,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		true,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		//this.addStepToQueue( levelMin ,     levelMax/2    ,     1     ,    40  , 0.3   ,       Transformation3DType.SIMILARITY,    null,
		//		opt  , ScalerType.SCALER_PHYSICAL, null ,
		//false,         CenteringStrategy.MASS_CENTER,    samplStrat  );


		this.register();
		this.showRegistrationSummary();
		
		ImagePlus result=this.computeRegisteredImage();
		return result;
	}

	

	public ItkTransform runScenarioKhalifa(ItkTransform trans, ImagePlus imgRef, ImagePlus imgMov) {
		this.setMovingImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgMov));
		this.setReferenceImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgRef));
		this.setViewSlice(imgRef.getStackSize()/2);
		this.setMetric(MetricType.MATTES);
		OptimizerType opt=OptimizerType.AMOEBA ;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
		int dimMinPyramide=3;
		int dimMinImage=Math.min(imgRef.getWidth()  , Math.min( imgRef.getHeight()  , imgRef.getStackSize() ) );
		int levelMax=3;//1+(int)Math.floor(Math.log(dimMinImage/dimMinPyramide)/Math.log(2) );
		int levelMin=1;

		this.addStepToQueue( levelMin ,     levelMax    ,     1     ,    40  , 0.3   ,       Transformation3DType.VERSOR,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( levelMin ,     levelMax/2    ,     1     ,    40  , 0.3   ,       Transformation3DType.SIMILARITY,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.transform=new ItkTransform(trans);

		this.register();
		this.showRegistrationSummary();
		
		ImagePlus result=this.computeRegisteredImage();
		//return result;
		
		return this.transform;
	}
	
	public ItkTransform runScenarioInterModal(ItkTransform trans, ImagePlus imgRef, ImagePlus imgMov) {
		this.setMovingImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgMov));
		this.setReferenceImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgRef));
		this.setViewSlice(imgRef.getStackSize()/2);
		this.setMetric(MetricType.MATTES);
		OptimizerType opt=OptimizerType.AMOEBA ;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
		int dimMinPyramide=3;
		int dimMinImage=Math.min(imgRef.getWidth()  , Math.min( imgRef.getHeight()  , imgRef.getStackSize() ) );
		int levelMax=3;//1+(int)Math.floor(Math.log(dimMinImage/dimMinPyramide)/Math.log(2) );
		int levelMin=1;

		this.addStepToQueue( 2 ,     3    ,     1     ,    100  , 0.3   ,       Transformation3DType.TRANSLATION,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( 2 ,   3    ,     1     ,    100  , 0.3   ,       Transformation3DType.VERSOR,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( 1 ,     2    ,     1     ,    100  , 0.3   ,       Transformation3DType.TRANSLATION,    null,
				OptimizerType.AMOEBA  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( 1 ,   2    ,     1     ,    100  , 0.3   ,       Transformation3DType.VERSOR,    null,
				OptimizerType.AMOEBA  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

//		this.addStepToQueue( levelMin ,     levelMax/2    ,     1     ,    40  , 0.3   ,       Transformation3DType.SIMILARITY,    null,
		//				opt  , ScalerType.SCALER_PHYSICAL, null ,
		//false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.transform=new ItkTransform(trans);

		this.register();
		this.showRegistrationSummary();
		
		ImagePlus result=this.computeRegisteredImage();
		//return result;
		
		return this.transform;
	}

	
	
	public ItkTransform runScenarioTestStuff(ItkTransform trans, ImagePlus imgRef, ImagePlus imgMov) {
		this.setMovingImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgMov));
		this.setReferenceImage(VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgRef));
		this.setViewSlice(imgRef.getStackSize()/2);
		this.setMetric(MetricType.MATTES);
		OptimizerType opt=OptimizerType.AMOEBA ;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
		int dimMinPyramide=3;
		int dimMinImage=Math.min(imgRef.getWidth()  , Math.min( imgRef.getHeight()  , imgRef.getStackSize() ) );

		this.addStepToQueue( 2 ,     3    ,     1     ,    100  , 0.3   ,       Transformation3DType.TRANSLATION,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( 2 ,   3    ,     1     ,    100  , 0.3   ,       Transformation3DType.VERSOR,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( 1 ,     2    ,     1     ,    100  , 0.3   ,       Transformation3DType.TRANSLATION,    null,
				OptimizerType.AMOEBA  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( 1 ,   2    ,     1     ,    100  , 0.3   ,       Transformation3DType.VERSOR,    null,
				OptimizerType.AMOEBA  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		//		this.addStepToQueue( levelMin ,     levelMax/2    ,     1     ,    40  , 0.3   ,       Transformation3DType.SIMILARITY,    null,
		//				opt  , ScalerType.SCALER_PHYSICAL, null ,
		//false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.transform=new ItkTransform(trans);

		this.register();
		this.showRegistrationSummary();
		
		ImagePlus result=this.computeRegisteredImage();
		//return result;
		
		return this.transform;
	}

	
	public ImagePlus[] runScenarioInterEchoes(ImagePlus refImgSource, ImagePlus movImgSource,
					double learningRate,int nbIteration, Transformation3DType transform,OptimizerType opt,MetricType metr,double sigma) {
				
		ImagePlus refImg=new Duplicator().run(refImgSource);
		ImagePlus movImg=new Duplicator().run(movImgSource);
		IJ.run(refImg,"32-bit","");
		IJ.run(movImg,"32-bit","");
		this.setMovingImage(movImg);
		this.setReferenceImage(refImg);
		this.setViewSlice(refImg.getStackSize()/2);
		this.setMetric(metr);
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
		int levelMax=1;
		int levelMin=1;
		OptimizerType opt2=OptimizerType.AMOEBA;
		this.addStepToQueue( levelMin ,    levelMax     ,     sigma   ,   80 ,learningRate   ,       Transformation3DType.TRANSLATION,    null,
				opt2  , ScalerType.SCALER_PHYSICAL , null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );
		this.addStepToQueue( levelMin ,    levelMax     ,     sigma   ,   80 ,learningRate   ,       Transformation3DType.VERSOR,    null,
				opt2  , ScalerType.SCALER_PHYSICAL , null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );
		this.addStepToQueue( levelMin ,    levelMax     ,     sigma   ,   80 ,learningRate   ,       Transformation3DType.SIMILARITY,    null,
				opt2  , ScalerType.SCALER_PHYSICAL , null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

/*		this.addStepToQueue( levelMin ,    levelMax     ,     0.5     ,    30  , 0.5   ,       Transformation3DType.TRANSLATION,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.addStepToQueue( levelMin ,    levelMax     ,     0.3    ,    30 , 0.3   ,       Transformation3DType.TRANSLATION,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );
*/
		this.register();
		//this.showRegistrationSummary();
		IJ.log("Score="+this.registrationMethods.get(registrationMethods.size()-1).getMetricValue());
		ImagePlus result=this.computeRegisteredImage();
		//result.show();
			return (new ImagePlus[] {result,summary});
		}

	
	
	/**
	 * Medium level functions : constructor, and managers algorithms. The Manager Handling should follow this order (see below in the commentary)
	 * In the order :
	 * call the constructor
	 * setMovingImage(ijImgMov);
	 * setReferenceImage(ijImgMov);
	 * setMetric(MetricType metricType)
	 * (eventually) setCapillaryHandlingOn
	 * addStepToQueue( ... )
	 * addStepToQueue( ... )
	 * addStepToQueue( ... )
	 * register();
	 * ImagePlus resultat=computeRegisteredImage();
	 */
	public ItkRegistrationManager() {
		this.registrationSummary=new ArrayList<ImagePlus>();
		this.metricType=null;
		this.transform=null;
		this.imgMovSuccessiveResults=new ArrayList<ImagePlus>();
		this.centerTransformFilter=new CenteredTransformInitializerFilter();
		this.gaussFilter=new RecursiveGaussianImageFilter();
		this.transformation3DTypes=new ArrayList<Transformation3DType>();
		this.centeringStrategies=new ArrayList<CenteringStrategy>();
		this.nbLevels= new ArrayList<Integer> ();
		this.shrinkFactors= new ArrayList<int[]> ();
		this.dimensions= new ArrayList<int[][]>() ;
		this.sigmaFactors= new ArrayList<double[]> ();

		this.registrationMethods= new ArrayList<ImageRegistrationMethod> ();
		this.transformation3DTypes= new ArrayList<Transformation3DType>() ;
		this.scalerTypes= new ArrayList<ScalerType>();
		this.weights= new ArrayList<double[]>() ;
		this.scales= new ArrayList<double[]>() ;

		this.currentLevel=0;
		this.zoomFactor=2;
		this.nbStep=0;
		this.currentStep=0;
		return;
	}

	public void addStepToQueue(int levelMin,int levelMax,double sigma, int iterations,    double learning_rate,  Transformation3DType typeTransfo,double[]weights,
			OptimizerType optimizerType,ScalerType scalerType,double[]scales, 
			boolean doubleIterAtCoarsestLevels,CenteringStrategy centeringStrategy,SamplingStrategy samplingStrategy){


		int[]shrinkFactors=subSamplingFactorsAtSuccessiveLevels(levelMin,levelMax,doubleIterAtCoarsestLevels,this.basis);
		double[]sigmaFactors=sigmaFactorsAtSuccessiveLevels(voxelSizeReference,shrinkFactors,sigma);
		int[][]dimensions=imageDimsAtSuccessiveLevels(imageSizeReference,shrinkFactors);

		this.shrinkFactors.add(shrinkFactors);
		this.dimensions.add(dimensions);
		this.sigmaFactors.add(sigmaFactors);
		ImageRegistrationMethod reg=new ImageRegistrationMethod();
		reg.setShrinkFactorsPerLevel(ItkImagePlusInterface.intArrayToVectorUInt32(shrinkFactors));
		reg.setSmoothingSigmasAreSpecifiedInPhysicalUnits(true);
		reg.smoothingSigmasAreSpecifiedInPhysicalUnitsOn();
		reg.setSmoothingSigmasPerLevel(ItkImagePlusInterface.doubleArrayToVectorDouble(sigmaFactors));

		switch(metricType) {
		case JOINT:reg.setMetricAsJointHistogramMutualInformation();break;
		case MEANSQUARE:reg.setMetricAsMeanSquares();break;
		case CORRELATION:reg.setMetricAsCorrelation();break;
		case MATTES:reg.setMetricAsMattesMutualInformation(128);break;
		case DEMONS:reg.setMetricAsDemons();break;
		case ANTS:reg.setMetricAsANTSNeighborhoodCorrelation(3);break;			
		}

		double learningRate=learning_rate;
		double maxStep = 0.5;
		double minStep = 0.001;
		int numberOfIterations = iterations;
		double relaxationFactor = 0.8;
		double convergenceMinimumValue=1E-6;
		int convergenceWindowSize=10;
		double gradientConvergenceTolerance=1E-5;
		double solutionAccuracy=1e-5;
		int maximumLineIterations=100;
		double stepLength=1;
		double stepTolerance=1e-6;
		double valueTolerance=1e-6;
		VectorUInt32 numberOfSteps=ItkImagePlusInterface.intArrayToVectorUInt32(new int[] {4,4,4});//Lets check that, one day.
		double simplexDelta=learning_rate;

		switch(optimizerType){
		case GRADIENT:reg.setOptimizerAsGradientDescent(learningRate/30.0, numberOfIterations, convergenceMinimumValue, convergenceWindowSize);break;
		case GRADIENT_REGULAR_STEP: reg.setOptimizerAsRegularStepGradientDescent(learningRate, minStep, numberOfIterations,relaxationFactor);break;
		case GRADIENT_LINE_SEARCH: reg.setOptimizerAsGradientDescentLineSearch(learningRate, numberOfIterations,convergenceMinimumValue, convergenceWindowSize);break;
		case GRADIENT_CONJUGATE: reg.setOptimizerAsConjugateGradientLineSearch(learningRate, numberOfIterations,convergenceMinimumValue, convergenceWindowSize);break;
		case LBFGSB: reg.setOptimizerAsLBFGSB(gradientConvergenceTolerance);break;
		case LBFGS2: reg.setOptimizerAsLBFGS2(solutionAccuracy);break;
		case POWELL: reg.setOptimizerAsPowell(numberOfIterations, maximumLineIterations, stepLength,stepTolerance, valueTolerance);break;
		case EVOLUTIONARY: reg.setOptimizerAsOnePlusOneEvolutionary(numberOfIterations);break;
		case EXHAUSTIVE: reg.setOptimizerAsExhaustive(numberOfSteps, stepLength);break;
		case AMOEBA: reg.setOptimizerAsAmoeba(simplexDelta, numberOfIterations);break;
		}

		switch(samplingStrategy) {
		case NONE: reg.setMetricSamplingStrategy(ImageRegistrationMethod.MetricSamplingStrategyType.NONE);break;
		case REGULAR: reg.setMetricSamplingStrategy(ImageRegistrationMethod.MetricSamplingStrategyType.REGULAR);break;
		case RANDOM: reg.setMetricSamplingStrategy(ImageRegistrationMethod.MetricSamplingStrategyType.RANDOM);break;
		}
		// For a test, one day : SetMetricSamplingPercentagePerLevel() : itk::simple::ImageRegistrationMethod


		this.transformation3DTypes.add(typeTransfo);
		this.scalerTypes.add(scalerType);
		this.scales.add(scales);
		this.weights.add(weights);
		this.centeringStrategies.add(centeringStrategy);
		this.registrationMethods.add(reg);
		nbStep++;
	}

	public void runNextStep(){
		if(this.transform==null && currentStep>0) { System.err.println("Pas de transformation calculee a l etape precedente. Exit");return;}
		boolean flagCentering=false;
		///////// PROTECTION DES DONNEES ENTRANTES ///////////////////
		Image itkImgRefTrans=new Image(itkImgRef);
		Image itkImgMovTrans=new Image(itkImgMov);


		/////////// STRATEGIE DE CENTRAGE EVENTUEL DE LA TRANSFORMATION //////////////
		switch(centeringStrategies.get(currentStep)) {
		case NONE: ;break;
		case IMAGE_CENTER: 
			centerTransformFilter.geometryOn();
			centerTransformFilter.setOperationMode(CenteredTransformInitializerFilter.OperationModeType.GEOMETRY);
			flagCentering=true;
			break;
		case MASS_CENTER: 
			centerTransformFilter.momentsOn();
			centerTransformFilter.setOperationMode(CenteredTransformInitializerFilter.OperationModeType.MOMENTS);
			flagCentering=true;
			break;
		}


		/////////// CHOIX DE LA TRANSFORMATION A AJOUTER, ET CENTRAGE EN FONCTION DE LA STRATEGIE //////////////	
		ItkTransform trPlus=null;
		switch(transformation3DTypes.get(currentStep)) {
			case EULER: trPlus=new ItkTransform(new Euler3DTransform());break;
			case EULER2D: trPlus=new ItkTransform(new Euler2DTransform());break;
			case VERSOR:trPlus=new ItkTransform(new VersorRigid3DTransform());break;
			case TRANSLATION:trPlus=new ItkTransform(new TranslationTransform(3));break;
			case AFFINE:trPlus=new ItkTransform(new AffineTransform(3));break;
			case SIMILARITY:trPlus=new ItkTransform(new Similarity3DTransform());break;
		}
		if(flagCentering && transformation3DTypes.get(currentStep)!= Transformation3DType.TRANSLATION)trPlus=new ItkTransform(centerTransformFilter.execute(this.itkImgRef,this.itkImgMov,trPlus));
		if(this.transform==null)this.transform=trPlus;
		else this.transform.addTransform(trPlus);

		this.registrationMethods.get(currentStep).setInitialTransform(this.transform);
//		this.registrationMethods.get(currentStep).setInitialTransform((org.itk.simple.Transform)this.transform);
		
		/////////// SCALING PARAMETERS STATEGY //////////////	
		switch(scalerTypes.get(currentStep)) {
		case NONE: ;break;
		case MANUAL: this.registrationMethods.get(currentStep).setOptimizerScales(ItkImagePlusInterface.doubleArrayToVectorDouble(scales.get(currentStep)));break;
		case SCALER_INDEX: this.registrationMethods.get(currentStep).setOptimizerScalesFromIndexShift();break;
		case SCALER_PHYSICAL: this.registrationMethods.get(currentStep).setOptimizerScalesFromPhysicalShift();break;
		case JACOBIAN_VERSOR: this.registrationMethods.get(currentStep).setOptimizerScalesFromJacobian();break;
		}

		/////////// PARAMETERS WEIGHTING FOR FOSTERING SOME OR SOME BEHAVIOUR //////////////	
		if(this.weights.get(this.currentStep)!=null && this.weights.get(this.currentStep).length>0) {
			this.registrationMethods.get(currentStep).setOptimizerWeights(ItkImagePlusInterface.doubleArrayToVectorDouble(this.weights.get(currentStep)));		
		}

		//////////// GO ! ////////////////
		try{this.registrationMethods.get(currentStep).execute(itkImgRefTrans,itkImgMovTrans);}catch(Exception e) {System.out.println(e.toString());}
		this.currentStep++;
	}

	public void register(){
		/*this.resamplerMov=new ResampleImageFilter();
		this.resamplerMov.setDefaultPixelValue(0);
		this.resamplerMov.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(this.voxelSizeReference));
		this.resamplerMov.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(this.imageSizeReference));
*/
		
		this.currentStep=0;
		while(currentStep<nbStep) {
			this.createUpdater();
			//System.out.println("\n\nNOUVELLE ETAPE DE RECALAGE\nTransformation avant optimisation :\n"+transform);
			updateView(this.dimensions.get(0)[0],this.sigmaFactors.get(0)[0],this.shrinkFactors.get(0)[0],
						"Position before registration, Red=Ref, Green=Mov",this.transform==null ? new ItkTransform() : this.transform);
			this.runNextStep();
			//System.out.println("\n\nTransformation après optimisation :\n"+transform);
			this.resampler.setTransform(this.transform);
			ImagePlus temp=ItkImagePlusInterface.itkImageToImagePlus(this.resampler.execute(this.itkImgMov));
			//temp.getProcessor().setMinAndMax(0, 255);
			this.imgMovSuccessiveResults.add(temp);
		}
		displayEndOfRegistrationMessage();
		//displayAlignmentResults();
		this.sliceView.changes=false;
		this.sliceView.close();
	}

	
	
	
	/**
	 * Functions for displaying, tracking and keep memories of registration results along the computation
	 */
	public void displayEndOfRegistrationMessage() {
		System.out.println("-----------------------------------------");
		System.out.println("-----------------------------------------");
		System.out.println("------     End of registration    -------");
		System.out.println("-----------------------------------------");
		System.out.format("Optimizer stop condition: %s\n", registrationMethods.get(nbStep-1).getOptimizerStopConditionDescription());
		System.out.format(" Iteration: %d\n", registrationMethods.get(nbStep-1).getOptimizerIteration());
		System.out.format(" Metric value: %f\n",registrationMethods.get(nbStep-1).getMetricValue());
	}

	public void displayAlignmentResults() {
		this.sliceView.changes=false;
		this.sliceView.close();
		
		
		ImagePlus resInit=VitimageUtils.compositeOf(ijImgRef,ijImgMov,"Position before registration, Red=Ref, Green=Mov");
		VitimageUtils.imageChecking(resInit,"Initial superposition");
		for(int st=nbStep-1;st<nbStep;st++) {
			ImagePlus res=VitimageUtils.compositeOf(ijImgRef,imgMovSuccessiveResults.get(st),"Registration result after step"+st+", Red=Ref, Green=Mov");
			VitimageUtils.imageChecking(res,"Registration result");
			imgMovSuccessiveResults.get(st).getProcessor().resetMinAndMax();
			IJ.run(imgMovSuccessiveResults.get(st),"16-bit","");
		}
	}
	
	public void showRegistrationSummary() {
/*		ImagePlus summary=ij.gui.NewImage.createImage("Registration summary (ref=reference, green=moving)",imageSizeReference[0],imageSizeReference[1],
										registrationSummary.size(),24,ij.gui.NewImage.FILL_BLACK);
		for(int i =0;i<registrationSummary.size() ; i++) {
			summary.getStack().setProcessor(registrationSummary.get(i).getProcessor(),i+1);
		}
*/
		ImagePlus []tabImg=new ImagePlus[registrationSummary.size()];
		for(int i =0;i<registrationSummary.size() ; i++) {
			tabImg[i]=registrationSummary.get(i);
		}
		this.summary=Concatenator.run(tabImg);
		IJ.saveAsTiff(this.summary, "/home/fernandr/tmpImageRec.tif");
		VitimageUtils.imageChecking(this.summary,0,this.summary.getStackSize()-1,2,"Registration summary",20,false);
		this.summary.changes=false;
		this.summary.close();
	}
	
	public ImagePlus computeRegisteredImage() {
		if(transform==null)return null;
		ResampleImageFilter resampler = new ResampleImageFilter();
		resampler.setReferenceImage(itkImgRef);
		resampler.setDefaultPixelValue(0);
		resampler.setTransform(transform);
//		resampler.setInterpolator(InterpolatorEnum.sitkLinear);
		Image imgResultat=resampler.execute(itkImgMov);
		ImagePlus res=ItkImagePlusInterface.itkImageToImagePlus(imgResultat);
		res.getProcessor().resetMinAndMax();
		return res;
	}

	public void updateView(int []dimensions,double sigmaFactor,int shrinkFactor,String viewText,ItkTransform currentTrans) {
		this.gaussFilter.setSigma(sigmaFactor);
		VectorDouble vectVoxSizes=new VectorDouble(3);
		vectVoxSizes.set(0,this.voxelSizeReference[0]*this.imageSizeReference[0]/dimensions[0]);
		vectVoxSizes.set(1,this.voxelSizeReference[1]*this.imageSizeReference[1]/dimensions[1]);
		vectVoxSizes.set(2,this.voxelSizeReference[2]*this.imageSizeReference[2]/dimensions[2]);

		//Update reference viewing image and the viewing slice, if needed
		this.resampler=new ResampleImageFilter();
		this.resampler.setDefaultPixelValue(0);
		this.resampler.setTransform(new ItkTransform());
		if(this.lookLikeOptimizerLooks) {
			this.resampler.setOutputSpacing(vectVoxSizes);
			this.resampler.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimensions));
		}
		else {
			this.resampler.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(this.voxelSizeReference));
			this.resampler.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(this.imageSizeReference));
		}
		this.itkImgViewRef=this.resampler.execute(this.itkImgRef);
		if(this.lookLikeOptimizerLooks) {
			for(int i=0;i<3;i++) {
				if(this.imageSizeReference[i]>=4) {
					this.gaussFilter.setDirection(i);
					itkImgViewRef=this.gaussFilter.execute(this.itkImgViewRef);			
				}
			}
		}
		this.sliceViewRef=itkImageToImagePlusSlice(this.itkImgViewRef,(int)Math.ceil(this.viewSlice*1.0/(this.lookLikeOptimizerLooks ? shrinkFactor : 1)));
		
		//Update moving image
		this.resampler=new ResampleImageFilter();
		this.resampler.setDefaultPixelValue(0);
		if(this.lookLikeOptimizerLooks) {
			this.resampler.setOutputSpacing(vectVoxSizes);
			this.resampler.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(dimensions));
		}
		else {
			this.resampler.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(this.voxelSizeReference));
			this.resampler.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(this.imageSizeReference));			
		}
		if(this.transform!=null)this.resampler.setTransform(this.transform);
		this.itkImgViewMov=this.resampler.execute(this.itkImgMov);
		if(this.lookLikeOptimizerLooks) {
			for(int i=0;i<3;i++) {
				if(this.imageSizeReference[i]>=4) {
					this.gaussFilter.setDirection(i);
					itkImgViewRef=this.gaussFilter.execute(this.itkImgViewRef);	
				}
			}
		//	this.gaussFilter.setDirection(2);
		//	this.itkImgViewMov=this.gaussFilter.execute(this.itkImgViewMov);
		}
		this.sliceViewMov=itkImageToImagePlusSlice(this.itkImgViewMov,(int)Math.ceil(this.viewSlice*1.0/(this.lookLikeOptimizerLooks ? shrinkFactor : 1)));

		//Compose the images
		//Comportement 1 : va surement clignoter.
		//Il existe un moyen de faire autrement, en copiant directement les données dedans, et en appelant update();
		/*this.sliceViewRef.getProcessor().resetMinAndMax();
		this.sliceViewMov.getProcessor().resetMinAndMax();
		IJ.run(this.sliceViewRef,"8-bit","");
		IJ.run(this.sliceViewMov,"8-bit","");*/
		if(this.sliceView==null || this.sliceViewRef.getWidth() != sliceView.getWidth()) {
			if(this.sliceView!=null) {this.sliceView.changes=false;this.sliceView.close();}
			this.sliceView=VitimageUtils.compositeOf(this.sliceViewRef,this.sliceViewMov,"Registration is running. Red=Reference, Green=moving");
			//this.sliceView=Vitimage_Toolbox.writeTextOnImage(viewText,this.sliceView,this.fontSize*this.sliceView.getWidth()/this.imageSizeReference[0]);
			this.sliceView.show();
			this.sliceView.getWindow().setSize(this.viewWidth,this.viewHeight);
			this.sliceView.getCanvas().fitToWindow();	
		}
		else {//Copie en place
			ImagePlus temp=VitimageUtils.compositeOf(this.sliceViewRef,this.sliceViewMov,"Red=Reference, Green=moving");
			temp=VitimageUtils.writeTextOnImage(viewText,temp,this.fontSize*temp.getWidth()/this.imageSizeReference[0],0);
			temp=VitimageUtils.writeTextOnImage(currentTrans.drawableString(),temp,this.fontSize*temp.getWidth()/this.imageSizeReference[0]-2,1);
			IJ.run(this.sliceView, "Select All", "");
			//IJ.setBackgroundColor(0, 0, 0);
			//IJ.run(this.sliceView, "Clear", "slice");
			//IJ.run(this.sliceView, "Select All", "");
			IJ.run(temp, "Select All", "");
			temp.copy();
			this.sliceView.paste();
			
			//Vitimage_Toolbox.putThatImageInThatOther(temp,this.sliceView);
		}
		//Prepare slices for summary 
		this.resampler=new ResampleImageFilter();
		this.resampler.setDefaultPixelValue(0);
		this.resampler.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(this.voxelSizeReference));
		this.resampler.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(this.imageSizeReference));
		this.itkSummaryRef=this.resampler.execute(this.itkImgViewRef);
		this.itkSummaryMov=this.resampler.execute(this.itkImgViewMov);
		if(movie3D) {
			this.sliceSummaryMov=ItkImagePlusInterface.itkImageToImagePlus(this.itkSummaryMov);
			this.sliceSummaryRef=ItkImagePlusInterface.itkImageToImagePlus(this.itkSummaryRef);
		}
		else {
			this.sliceSummaryMov=itkImageToImagePlusSlice(this.itkSummaryMov,this.viewSlice);
			this.sliceSummaryRef=itkImageToImagePlusSlice(this.itkSummaryRef,this.viewSlice);
		}
		/*this.sliceSummaryRef.getProcessor().resetMinAndMax();
		this.sliceSummaryMov.getProcessor().resetMinAndMax();
		IJ.run(this.sliceSummaryRef,"8-bit","");
		IJ.run(this.sliceSummaryMov,"8-bit","");*/
		ImagePlus temp2=VitimageUtils.compositeOf(this.sliceSummaryRef,this.sliceSummaryMov,"Registration is running. Red=Reference, Green=moving");
		temp2=VitimageUtils.writeTextOnImage(viewText,temp2,this.fontSize*temp2.getWidth()/this.imageSizeReference[0],0);
		temp2=VitimageUtils.writeTextOnImage(currentTrans.drawableString(),temp2,this.fontSize*temp2.getWidth()/this.imageSizeReference[0]-2,1);
		this.registrationSummary.add(temp2);
	}


	
	

	/**
	 * Initializers
	 */
	public void setReferenceImage(ImagePlus img) {
		this.ijImgRef=new Duplicator().run(img);
		this.viewHeight=ijImgRef.getHeight()*zoomFactor;
		this.viewWidth=ijImgRef.getWidth()*zoomFactor;
		this.itkImgRef=ItkImagePlusInterface.imagePlusToItkImage(ijImgRef);
		this.imageSizeReference=new int[] {ijImgRef.getWidth(),ijImgRef.getHeight(),ijImgRef.getStackSize()};
		this.voxelSizeReference=new double[] {ijImgRef.getCalibration().pixelWidth,ijImgRef.getCalibration().pixelHeight,ijImgRef.getCalibration().pixelDepth};
		this.viewSlice=(int)Math.round(ijImgRef.getStackSize()/2.0);
	}

	public void setViewSlice(int slic) {
		this.viewSlice=slic;
		
	}
	
	public void setMovingImage(ImagePlus img) {
		this.ijImgMov=new Duplicator().run(img);
		this.itkImgMov=ItkImagePlusInterface.imagePlusToItkImage(ijImgMov);
	}

	public void setMetric(MetricType metricType) {
		this.metricType=metricType;
	}

	public void setInitialTransformation(ItkTransform trans) {
		if(this.transform !=null || this.currentStep>0) System.err.println("Une transformation initiale est deja existante");
		else this.transform=new ItkTransform(trans);
	}

	public void createUpdater() {
		this.updater=new IterationUpdate(this,this.registrationMethods.get(currentStep));
		this.registrationMethods.get(this.currentStep).removeAllCommands();
		this.registrationMethods.get(this.currentStep).addCommand(EventEnum.sitkIterationEvent,this.updater);
	}






	/**
	 * Helper functions to build the pyramidal scheme based on few parameters
	 */
	public int[] subSamplingFactorsAtSuccessiveLevels(int levelMin,int levelMax,boolean doubleIterAtCoarsestLevels,double basis){
		if(levelMax<levelMin)levelMax=levelMin;
		if(levelMin<=0)return null;
		int nbLevels=levelMax-levelMin+1;
		int nbDouble=doubleIterAtCoarsestLevels ? (nbLevels>2 ? nbLevels-2 : 0) : 0;
		int nbTot=nbLevels+nbDouble;
		int []ret=new int[nbTot];
		for(int i=0;i<2 && i<nbTot;i++) {
			ret[nbTot-1-i]=(int)Math.round(Math.pow(basis,(levelMin-1+i)));
		}
		if(doubleIterAtCoarsestLevels) {
			for(int i=0;i<(nbTot-2)/2;i++) {
				ret[nbTot-1-(2+i*2)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
				ret[nbTot-1-(2+i*2+1)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
			}
		}
		else {
			for(int i=2;i<nbTot;i++) {
				ret[nbTot-1-i]=(int)Math.round(Math.pow(basis,(levelMin-1+i)));
			}

		}		
		return ret;
	}

	public int [][] imageDimsAtSuccessiveLevels(int []dimsImg,int []subFactors) {		
		if (subFactors==null)return null;
		int n=subFactors.length;
		int [][]ret=new int[n][3];
		for(int i=0;i<n;i++) for(int j=0;j<3;j++)ret[i][j]=(int)Math.ceil(dimsImg[j]/subFactors[i]);
		return ret;
	}

	public double[] sigmaFactorsAtSuccessiveLevels(double []voxSize,int[]subFactors,double rapportSigma) {
		if (subFactors==null)return null;
		double voxSizeInit=Math.min(Math.min(voxSize[0],voxSize[1]),voxSize[2]);
		double[]ret=new double[subFactors.length];
		for(int i=0;i<subFactors.length;i++)ret[i]=voxSizeInit*subFactors[i]*rapportSigma;
		return ret;
	}




	/**
	 * Minor getters/setters
	 */
	public ArrayList<Integer> getNbLevels() {
		return nbLevels;
	}

	public void setNbLevels(ArrayList<Integer> nbLevels) {
		this.nbLevels = nbLevels;
	}

	public int[] getCurrentShrinkFactors() {
		return shrinkFactors.get(currentStep);
	}

	public void setShrinkFactors(ArrayList<int[]> shrinkFactors) {
		this.shrinkFactors = shrinkFactors;
	}

	public int[][] getCurrentDimensions() {
		return dimensions.get(currentStep);
	}

	public double[] getCurrentSigmaFactors() {
		return sigmaFactors.get(currentStep);
	}

	public int getCurrentLevel() {
		return currentLevel;
	}

	public void setCurrentLevel(int currentLevel) {
		this.currentLevel = currentLevel;
	}

	public ArrayList<ScalerType> getScalerTypes() {
		return scalerTypes;
	}

	public void setScalerTypes(ArrayList<ScalerType> scalerTypes) {
		this.scalerTypes = scalerTypes;
	}

	public int getNbSteps() {
		return nbStep;
	}

	public void setNbSteps(int nbStep) {
		this.nbStep = nbStep;
	}

	public int getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(int currentStep) {
		this.currentStep = currentStep;
	}

	public ArrayList<double[]> getWeights() {
		return weights;
	}

	public void setWeights(ArrayList<double[]> weights) {
		this.weights = weights;
	}

	public ArrayList<double[]> getScales() {
		return scales;
	}

	public void setScales(ArrayList<double[]> scales) {
		this.scales = scales;
	}

}

/**
 * Listener for gathering optimizer events. It allows the manager to update and display the process state along the running
 */
class IterationUpdate  extends Command {
	private double initScore=1;
	private ImageRegistrationMethod method;
	private ItkRegistrationManager manager;
	private int [] tabShrink;
	private double refreshingPeriod=50;
	private int  [][]tabSizes;
	private double[] tabSigma;
	private long memoirePyramide=1000;
	private double timeStamp1=0;
	private double timeStamp0=0;
	private double timeStampInit=0;
	private double durationIter=0;
	private double durationTot=0;
	private double nextViewTime=0+refreshingPeriod;
	private double improvement=0;
	public IterationUpdate(ItkRegistrationManager manager,ImageRegistrationMethod method) {
		super();
		this.manager=manager;
		this.tabSigma=manager.getCurrentSigmaFactors();
		this.tabShrink=manager.getCurrentShrinkFactors();
		this.tabSizes=manager.getCurrentDimensions();
		this.method=method;
		timeStamp1=0;
		timeStamp0=0;
		timeStampInit=0;
		durationTot=0;
		durationIter=0;
		
	}

	public void execute() {
		boolean isNewLevel=(method.getOptimizerIteration()<memoirePyramide);		  
		int mem=(int) method.getCurrentLevel();

		if(isNewLevel) {
			String st="";
			if(memoirePyramide<1000) {
				for(int i=0;i<mem-1;i++)st+="     ";
				st+="---------\n";
				for(int i=0;i<mem-1;i++)st+="     ";
				st+="Niveau "+(mem-1)+" terminé. Passage au niveau suivant\n\n\n\n";
				for(int i=0;i<mem;i++)st+="     ";
			}
			st+="Niveau "+(mem+1)+" / "+tabShrink.length+" .\n Sigma lissage = "+(MRUtils.dou(tabSigma[mem]))+" mm . Facteur subsampling = "+tabShrink[(int) mem]+
					" .  Taille images = "+tabSizes[mem][0]+" x "+tabSizes[mem][1]+" x "+tabSizes[mem][2]+" . Execution sur "+method.getNumberOfThreads()+" coeurs. Démarrage.\n";
			for(int i=0;i<mem;i++)st+="     ";
			st+="----------";		  
			System.out.println(st);		  
			//IJ.log(st);
		}
		memoirePyramide=method.getOptimizerIteration();
		String pyr="";
		for(int i=1;i<=method.getCurrentLevel();i++)pyr+="     ";
		pyr+="Step "+(manager.getCurrentStep()+1)+"/"+manager.getNbSteps()+" | Niveau "+(method.getCurrentLevel()+1)+"/"+tabSigma.length+" | ";
		if(timeStamp0==0)  timeStampInit=System.nanoTime()*1.0/1000000.0;
		timeStamp0=timeStamp1;
		timeStamp1=System.nanoTime()*1.0/1000000.0;
		durationIter=(timeStamp0 !=0 ? timeStamp1-timeStamp0 : 0);
		durationTot=(timeStamp0 !=0 ? timeStamp1-timeStampInit : 0)/1000.0;
		double durIter=(durationIter<1000 ? durationIter : durationIter/1000.0);
		String unitIter=(durationIter<1000 ? "ms" : "s ");
		double durTot=(durationTot<180 ? durationTot : durationTot/60);
		String unitTot=(durationTot<180 ? "s " : "mn");
		durationTot=(int)Math.round(timeStamp0 !=0 ? timeStamp1-timeStampInit : 0);
		if(isNewLevel)initScore=-100.0*method.getMetricValue();
		improvement=Math.abs((-100.0*method.getMetricValue() - initScore)/initScore)*100;
		if(method.getOptimizerIteration()==0)System.out.format("%sIteration %3d  |  Score = %6.4f  |  Titeration = %8.4f %s  |  Ttotal = %5.2f %s  | Improvement = %4.2f |\n",
				pyr,method.getOptimizerIteration()
				,-100.0*method.getMetricValue()
				, (float)durIter,unitIter
				, (float)durTot,unitTot,improvement
				);
		else System.out.format("%sIteration %3d  |  Score = %6.4f  |  Titeration = %8.4f %s  |  Ttotal = %5.2f %s  | Evolution = %4.2f%% |\n",
				pyr,method.getOptimizerIteration()
				,-100.0*method.getMetricValue()
				, (float)durIter,unitIter
				, (float)durTot,unitTot,improvement
				);
		if(durationTot>nextViewTime) {
			nextViewTime=durationTot+refreshingPeriod;
			String st=String.format("Step=%1d/%1d - Niveau=%1d/%1d - Iter=%3d - Score=%6.4f - Evolution=%4.2f%%",
					(manager.getCurrentStep()+1),
					manager.getNbSteps(),
					(method.getCurrentLevel()+1),tabSigma.length,method.getOptimizerIteration(),-100.0*method.getMetricValue(),improvement);
			this.manager.updateView(this.tabSizes[mem],tabSigma[mem],this.tabShrink[mem],st,new ItkTransform(method.getInitialTransform()));
		}
	}

}
