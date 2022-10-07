/*
 * 
 */
package io.github.rocsg.fijiyama.registration;

import java.util.ArrayList;

import org.itk.simple.AffineTransform;
import org.itk.simple.CenteredTransformInitializerFilter;
import org.itk.simple.Command;
import org.itk.simple.Euler2DTransform;
import org.itk.simple.Euler3DTransform;
import org.itk.simple.EventEnum;
import org.itk.simple.Image;
import org.itk.simple.ImageRegistrationMethod;
import org.itk.simple.ResampleImageFilter;
import org.itk.simple.Similarity3DTransform;
import org.itk.simple.TranslationTransform;
import org.itk.simple.VectorDouble;
import org.itk.simple.VectorUInt32;
import org.itk.simple.VersorRigid3DTransform;
import org.itk.simple.RecursiveGaussianImageFilter;

import ij.IJ;
import ij.ImagePlus;
import io.github.rocsg.fijiyama.common.ItkImagePlusInterface;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.CenteringStrategy;
import io.github.rocsg.fijiyama.registration.IterationUpdate;
import io.github.rocsg.fijiyama.registration.ItkRegistration;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.MetricType;
import io.github.rocsg.fijiyama.registration.OptimizerType;
import io.github.rocsg.fijiyama.registration.SamplingStrategy;
import io.github.rocsg.fijiyama.registration.ScalerType;
import io.github.rocsg.fijiyama.registration.Transform3DType;

// TODO: Auto-generated Javadoc
/**
 * The Class ItkRegistration.
 */
public class ItkRegistration implements ItkImagePlusInterface{
	
	/** The additional transform. */
	ItkTransform additionalTransform=new ItkTransform();
	
	/** The return composed transformation including the initial transformation given. */
	public boolean returnComposedTransformationIncludingTheInitialTransformationGiven=true;
	
	/** The ref range. */
	public double[]refRange;
	
	/** The mov range. */
	public double[]movRange;
	
	/** The flag range. */
	public boolean flagRange=false;
	
	/** The look like optimizer looks. */
	boolean lookLikeOptimizerLooks=false;
	
	/** The text info at each iteration. */
	boolean textInfoAtEachIteration=false;
	
	/** The movie 3 D. */
	boolean movie3D=false;
	
	/** The display registration. */
	public int displayRegistration=1;
	
	/** The center transform filter. */
	CenteredTransformInitializerFilter centerTransformFilter;
	
	/** The gauss filter. */
	private RecursiveGaussianImageFilter gaussFilter;
	
	/** The resampler. */
	private ResampleImageFilter resampler;
	
	/** The voxel size reference. */
	private double[]voxelSizeReference;
	
	/** The image size reference. */
	private int[]imageSizeReference;
	
	/** The itk img view ref. */
	private Image itkImgViewRef;
	
	/** The itk img view mov. */
	private Image itkImgViewMov;
	
	/** The itk summary ref. */
	private Image itkSummaryRef;
	
	/** The itk summary mov. */
	private Image itkSummaryMov;
	
	/** The slice view mov. */
	private ImagePlus sliceViewMov;
	
	/** The slice view ref. */
	private ImagePlus sliceViewRef;
	
	/** The slice view. */
	private ImagePlus sliceView;
	
	/** The slice summary ref. */
	private ImagePlus sliceSummaryRef;
	
	/** The slice summary mov. */
	private ImagePlus sliceSummaryMov;
	
	/** The img mov successive results. */
	//private ImagePlus summary;
	private ArrayList<ImagePlus> imgMovSuccessiveResults;
	
	/** The view width. */
	private int viewWidth;
	
	/** The view height. */
	private int viewHeight;
	
	/** The view slice. */
	private int viewSlice;
	
	/** The zoom factor. */
	private int zoomFactor;
	
	/** The updater. */
	private IterationUpdate updater;
	
	/** The nb levels. */
	private ArrayList<Integer> nbLevels;
	
	/** The shrink factors. */
	private ArrayList<int[]> shrinkFactors;
	
	/** The dimensions. */
	private ArrayList<int[][]> dimensions;
	
	/** The sigma factors. */
	private ArrayList<double[]> sigmaFactors;
	
	/** The current level. */
	private int currentLevel;
	
	/** The basis. */
	private int basis=2;//shrink factor at each level

	/** The centering strategies. */
	private ArrayList<CenteringStrategy> centeringStrategies;
	
	/** The transformation 3 D types. */
	private ArrayList<Transform3DType> transformation3DTypes;
	
	/** The scaler types. */
	private ArrayList<ScalerType>scalerTypes;
	
	/** The nb step. */
	private int nbStep;
	
	/** The current step. */
	private int currentStep;
	
	/** The metric type. */
	private MetricType metricType;
	
	/** The registration methods. */
	private ArrayList<ImageRegistrationMethod> registrationMethods;
	
	/** The ij img ref. */
	public ImagePlus ijImgRef;
	
	/** The itk img ref. */
	private Image itkImgRef;
	
	/** The ij img mov. */
	private ImagePlus ijImgMov;
	
	/** The itk img mov. */
	private Image itkImgMov;
	
	/** The registration summary. */
	private ArrayList<ImagePlus> registrationSummary;
	
	/** The transform. */
	private ItkTransform transform;
	
	/** The weights. */
	private ArrayList<double[]> weights;
	
	/** The scales. */
	private ArrayList<double[]> scales;
	
	/** The font size. */
	private int fontSize=12;
	
	/** The registration thread. */
	public Thread registrationThread;
	
	/** The itk registration interrupted. */
	public volatile boolean itkRegistrationInterrupted=false;
	
	/** The itk is interrupted succeeded. */
	public volatile boolean itkIsInterruptedSucceeded=false;

	/**
	 * Free memory.
	 */
	public void freeMemory(){
		if(registrationSummary.size()>0) {
			registrationSummary=null;
		}
		itkImgMov=null;
		ijImgMov=null;
		itkImgRef=null;
		ijImgRef=null;

		itkImgViewRef=null;
		itkImgViewMov=null;
		itkSummaryRef=null;
		itkSummaryMov=null;
		sliceViewMov=null;
		sliceViewRef=null;
		sliceView=null;
		sliceSummaryRef=null;
		sliceSummaryMov=null;
		if(imgMovSuccessiveResults.size()>0) {
			imgMovSuccessiveResults=null;
		}
		System.gc();
	}
	
	/**
	 *  
	 * Top level functions : Test function, and main scenarios that will be used by customers classes.
	 *
	 * @return the int
	 */
	public int runTestSequence() {
		
		//MATTES256 SUR VERSOR 2 3 PUIS SIMILITUDE 1 2 , avec AMOEBA et sigma 0.8 , 30  0.5 --> de la tuerie
		int nbFailed=0;
		ItkRegistration manager=new ItkRegistration();
		ImagePlus []imgTab=VitiDialogs.chooseTwoImagesUI("Choose reference image and moving image to register\n\n","Reference image","Moving image");
		manager.movie3D=VitiDialogs.getYesNoUI("Compute 3D summary ?","");
		ImagePlus imgRef=imgTab[0];
		ImagePlus imgMov=imgTab[1];
		manager.setMovingImage(imgMov);
		manager.setReferenceImage(imgRef);
		manager.setViewSlice(20);
		manager.setMetric(MetricType.MATTES);
		OptimizerType opt=OptimizerType.ITK_AMOEBA ;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
	/*	addStepToQueue( 30 20
				int levelMin,int levelMax,double sigma,Transform3DType typeTransfo,double[]weights,
				OptimizerType optimizerType,ScalerType scalerType,double[]scales, 
				boolean doubleIterAtCoarsestLevels,CenteringStrategy centeringStrategy,SamplingStrategy samplingStrategy
				);*/
		manager.addStepToQueue( 1 ,     4     ,     1     ,    2*2  , 0.8   ,       Transform3DType.VERSOR,    null,
						opt  , ScalerType.SCALER_PHYSICAL, null ,
				false,         CenteringStrategy.MASS_CENTER,    samplStrat  );
		
		manager.addStepToQueue( 1 ,    2     ,     0.8     ,   3*2,  0.8,     Transform3DType.SIMILARITY,    null,
				opt  , ScalerType.SCALER_PHYSICAL, null ,
				false,         CenteringStrategy.NONE,    samplStrat  );

		
		manager.register();
		freeMemory();
		return nbFailed;
	}
	
	/**
	 * Run scenario from gui.
	 *
	 * @param transformInit the transform init
	 * @param imgRef the img ref
	 * @param imgMov the img mov
	 * @param transformType the transform type
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param nIterations the n iterations
	 * @param learningRate the learning rate
	 * @return the itk transform
	 */
	public ItkTransform runScenarioFromGui(ItkTransform transformInit, ImagePlus imgRef, ImagePlus imgMov, Transform3DType transformType,int levelMin,int levelMax,int nIterations,double learningRate) {
		this.setMovingImage(imgMov);
		this.setReferenceImage(imgRef);
		this.setViewSlice(imgRef.getStackSize()/2);
		this.setMetric(MetricType.CORRELATION);
		OptimizerType opt=OptimizerType.ITK_AMOEBA ;
		SamplingStrategy samplStrat=SamplingStrategy.NONE;
		
		this.addStepToQueue( levelMin,     levelMax    ,     1     ,   nIterations  , learningRate   ,       transformType,    null,
				opt  , ScalerType.NONE/*ScalerType.SCALER_PHYSICAL*/, null ,
		false,         CenteringStrategy.IMAGE_CENTER,    samplStrat  );

		this.transform=ItkTransform.itkTransformFromCoefs(new double[] {1,0,0,0,0,1,0,0,0,0,1,0});//new ItkTransform(transformInit);
		this.register();
		if(this.itkRegistrationInterrupted)return null;
		if(this.returnComposedTransformationIncludingTheInitialTransformationGiven) return this.transform;
		else {
			if(transformInit.isDense())return new ItkTransform((transformInit.getInverseOfDenseField()).addTransform(this.transform));
			else return new ItkTransform(transformInit.getInverse().addTransform(this.transform));
		}
	}

	/**
	 * Estimate registration duration.
	 *
	 * @param dims the dims
	 * @param viewRegistrationLevel the view registration level
	 * @param nbIter the nb iter
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @return the int
	 */
	public static int estimateRegistrationDuration(int[]dims,int viewRegistrationLevel,int nbIter,int levelMin,int levelMax) {
		double imageSize=dims[0]*dims[1]*dims[2];
		double[]imageSizes=new double[levelMax-levelMin+1];
		double sumSize=0;
		for(int lev=levelMax;lev>=levelMin;lev--) {imageSizes[levelMax-lev]=imageSize/Math.pow(2,(lev-1));sumSize+=imageSizes[levelMax-lev];}
		double factorProcess=1E-8;
		double factorView=1E-7;
		double factorInit=2E-7;
		double bonusTime=factorInit*imageSize;
		double displayTime=nbIter*factorView*(levelMax-levelMin+1)*viewRegistrationLevel*imageSize;
		double processingTime=factorProcess*sumSize*nbIter;
		return (int)Math.round(processingTime+displayTime+bonusTime);
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
	public ItkRegistration() {
		this.registrationSummary=new ArrayList<ImagePlus>();
		this.metricType=null;
		this.transform=null;
		this.imgMovSuccessiveResults=new ArrayList<ImagePlus>();
		this.centerTransformFilter=new CenteredTransformInitializerFilter();
		this.gaussFilter=new RecursiveGaussianImageFilter();
		this.transformation3DTypes=new ArrayList<Transform3DType>();
		this.centeringStrategies=new ArrayList<CenteringStrategy>();
		this.nbLevels= new ArrayList<Integer> ();
		this.shrinkFactors= new ArrayList<int[]> ();
		this.dimensions= new ArrayList<int[][]>() ;
		this.sigmaFactors= new ArrayList<double[]> ();

		this.registrationMethods= new ArrayList<ImageRegistrationMethod> ();
		this.transformation3DTypes= new ArrayList<Transform3DType>() ;
		this.scalerTypes= new ArrayList<ScalerType>();
		this.weights= new ArrayList<double[]>() ;
		this.scales= new ArrayList<double[]>() ;

		this.currentLevel=0;
		this.zoomFactor=2;
		this.nbStep=0;
		this.currentStep=0;
		return;
	}

	/**
	 * Adds the step to queue.
	 *
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param sigma the sigma
	 * @param iterations the iterations
	 * @param learning_rate the learning rate
	 * @param typeTransfo the type transfo
	 * @param weights the weights
	 * @param optimizerType the optimizer type
	 * @param scalerType the scaler type
	 * @param scales the scales
	 * @param doubleIterAtCoarsestLevels the double iter at coarsest levels
	 * @param centeringStrategy the centering strategy
	 * @param samplingStrategy the sampling strategy
	 */
	public void addStepToQueue(int levelMin,int levelMax,double sigma, int iterations,    double learning_rate,  Transform3DType typeTransfo,double[]weights,
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
		default:reg.setMetricAsCorrelation();	break;			
		}

		double learningRate=learning_rate;
		double minStep = 0.001;
		int numberOfIterations = iterations;
		double relaxationFactor = 0.8;
		double convergenceMinimumValue=1E-6;
		int convergenceWindowSize=10;
		double stepLength=1;
		VectorUInt32 numberOfSteps=ItkImagePlusInterface.intArrayToVectorUInt32(new int[] {4,4,4});//Lets check that, one day.
		double simplexDelta=learning_rate;

		switch(optimizerType){
		case ITK_GRADIENT:reg.setOptimizerAsGradientDescent(learningRate/30.0, numberOfIterations, convergenceMinimumValue, convergenceWindowSize);break;
		case ITK_GRADIENT_REGULAR_STEP: reg.setOptimizerAsRegularStepGradientDescent(learningRate, minStep, numberOfIterations,relaxationFactor);break;
		case ITK_GRADIENT_LINE_SEARCH: reg.setOptimizerAsGradientDescentLineSearch(learningRate, numberOfIterations,convergenceMinimumValue, convergenceWindowSize);break;
		case ITK_GRADIENT_CONJUGATE: reg.setOptimizerAsConjugateGradientLineSearch(learningRate, numberOfIterations,convergenceMinimumValue, convergenceWindowSize);break;
		//case LBFGSB: reg.setOptimizerAsLBFGSB(gradientConvergenceTolerance);break;
		//case LBFGS2: reg.setOptimizerAsLBFGS2(solutionAccuracy);break;
		//case POWELL: reg.setOptimizerAsPowell(numberOfIterations, maximumLineIterations, stepLength,stepTolerance, valueTolerance);break;
		//case EVOLUTIONARY: reg.setOptimizerAsOnePlusOneEvolutionary(numberOfIterations);break;
		case ITK_EXHAUSTIVE: reg.setOptimizerAsExhaustive(numberOfSteps, stepLength);break;
		case ITK_AMOEBA: reg.setOptimizerAsAmoeba(simplexDelta, numberOfIterations);break;
		default: reg.setOptimizerAsAmoeba(simplexDelta, numberOfIterations);break;
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

	/**
	 * Run next step.
	 */
	public void runNextStep(){
		if(this.transform==null && currentStep>0) { System.err.println("Pas de transformation calculee a l etape precedente. Exit");return;}
		boolean flagCentering=false;
		Image itkImgRefTrans=new Image(itkImgRef);
		Image itkImgMovTrans=new Image(itkImgMov);

		//Centering strategies
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


		//Check transform
		ItkTransform trPlus=null;
		switch(transformation3DTypes.get(currentStep)) {
			case EULER: trPlus=new ItkTransform(new Euler3DTransform());break;
			case EULER2D: trPlus=new ItkTransform(new Euler2DTransform());break;
			case VERSOR:trPlus=new ItkTransform(new VersorRigid3DTransform());break;
			case TRANSLATION:trPlus=new ItkTransform(new TranslationTransform(3));break;
			case AFFINE:trPlus=new ItkTransform(new AffineTransform(3));break;
			case SIMILARITY:trPlus=new ItkTransform(new Similarity3DTransform());break;
			default:trPlus=new ItkTransform(new Euler3DTransform());break;
		}
		if(flagCentering && transformation3DTypes.get(currentStep)!= Transform3DType.TRANSLATION)trPlus=new ItkTransform(centerTransformFilter.execute(this.itkImgRef,this.itkImgMov,trPlus));

		if(this.transform==null)this.transform=trPlus;
		else this.transform.addTransform(trPlus);
		this.registrationMethods.get(currentStep).setInitialTransform(this.transform);	
		switch(scalerTypes.get(currentStep)) {
		case NONE: ;break;
		case MANUAL: this.registrationMethods.get(currentStep).setOptimizerScales(ItkImagePlusInterface.doubleArrayToVectorDouble(scales.get(currentStep)));break;
		case SCALER_INDEX: this.registrationMethods.get(currentStep).setOptimizerScalesFromIndexShift();break;
		case SCALER_PHYSICAL: this.registrationMethods.get(currentStep).setOptimizerScalesFromPhysicalShift();break;
		case JACOBIAN_VERSOR: this.registrationMethods.get(currentStep).setOptimizerScalesFromJacobian();break;
		}

		// PARAMETERS WEIGHTING FOR FOSTERING SOME OR SOME BEHAVIOUR //////////////	
		if(this.weights.get(this.currentStep)!=null && this.weights.get(this.currentStep).length>0) {
			this.registrationMethods.get(currentStep).setOptimizerWeights(ItkImagePlusInterface.doubleArrayToVectorDouble(this.weights.get(currentStep)));		
		}

 		//////////// GO ! ////////////////
		this.registrationThread= new Thread() {  { setPriority(Thread.NORM_PRIORITY); }  
			public void run() {  
				try {
					registrationMethods.get(currentStep).execute(itkImgRefTrans,itkImgMovTrans);
					}catch(Exception e) {System.out.println("Interruption of Itk registration");itkRegistrationInterrupted=true;}
			}
		};
		VitimageUtils.startAndJoin(this.registrationThread);
		if(itkRegistrationInterrupted) {
			itkIsInterruptedSucceeded=true;
			this.currentStep=100000;
		}
		else this.currentStep++;
	}

	/**
	 * Register.
	 */
	public void register(){
		this.currentStep=0;
		while(currentStep<nbStep) {
			int excludeMargin=ijImgRef.getStackSize()/4;
			this.registrationMethods.get(currentStep).setMetricFixedMask(ItkImagePlusInterface.imagePlusToItkImage(VitimageUtils.restrictionMaskForFadingHandling(this.ijImgRef,excludeMargin)));
			this.registrationMethods.get(currentStep).setMetricMovingMask(ItkImagePlusInterface.imagePlusToItkImage(VitimageUtils.restrictionMaskForFadingHandling(this.ijImgMov,excludeMargin)));

			this.createUpdater();
			updateView(this.dimensions.get(0)[0],this.sigmaFactors.get(0)[0],this.shrinkFactors.get(0)[0],
						"Position before registration, Red=Ref, Green=Mov",this.transform==null ? new ItkTransform() : this.transform);
			this.runNextStep();
			if(this.displayRegistration>0) {
				this.resampler.setTransform(this.transform);
				ImagePlus temp=ItkImagePlusInterface.itkImageToImagePlus(this.resampler.execute(this.itkImgMov));
				this.imgMovSuccessiveResults.add(temp);
			}

		}
		displayEndOfRegistrationMessage();
		if(this.displayRegistration>0) {
			this.sliceView.changes=false;
			this.sliceView.close();
		}
	}

	
	
	
	/**
	 * Functions for displaying, tracking and keep memories of registration results along the computation.
	 */
	public void displayEndOfRegistrationMessage() {
		IJ.log("-----------------------------------------");
		IJ.log("-----------------------------------------");
		IJ.log("------     End of registration    -------");
		IJ.log("-----------------------------------------");
		IJ.log("Optimizer stop condition: "+registrationMethods.get(nbStep-1).getOptimizerStopConditionDescription()+"\n");
		IJ.log(" Iteration: "+registrationMethods.get(nbStep-1).getOptimizerIteration()+"\n");
		IJ.log(" Metric value: "+registrationMethods.get(nbStep-1).getMetricValue()+"\n");
	}

	/**
	 * Compute registered image.
	 *
	 * @return the image plus
	 */
	public ImagePlus computeRegisteredImage() {
		if(transform==null)return null;
		ResampleImageFilter resampler = new ResampleImageFilter();
		resampler.setReferenceImage(itkImgRef);
		resampler.setDefaultPixelValue(0);
		resampler.setTransform(transform);
		Image imgResultat=resampler.execute(itkImgMov);
		ImagePlus res=ItkImagePlusInterface.itkImageToImagePlus(imgResultat);
		res.getProcessor().resetMinAndMax();
		return res;
	}

	/**
	 * Update view.
	 *
	 * @param dimensions the dimensions
	 * @param sigmaFactor the sigma factor
	 * @param shrinkFactor the shrink factor
	 * @param viewText the view text
	 * @param currentTrans the current trans
	 */
	public void updateView(int []dimensions,double sigmaFactor,int shrinkFactor,String viewText,ItkTransform currentTrans) {
		if(this.displayRegistration==0)return;
		this.gaussFilter.setSigma(sigmaFactor);
		VectorDouble vectVoxSizes=new VectorDouble(3);
		vectVoxSizes.set(0,this.voxelSizeReference[0]*this.imageSizeReference[0]/dimensions[0]);
		vectVoxSizes.set(1,this.voxelSizeReference[1]*this.imageSizeReference[1]/dimensions[1]);
		vectVoxSizes.set(2,this.voxelSizeReference[2]*this.imageSizeReference[2]/dimensions[2]);

		//Update reference viewing image and the viewing slice, if needed
		this.resampler=new ResampleImageFilter();
		this.resampler.setDefaultPixelValue(0);
		this.resampler.setTransform(ItkTransform.getTransformForResampling(this.voxelSizeReference, VitimageUtils.getVoxelSizes(this.ijImgMov)));
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
		this.sliceViewRef=ItkImagePlusInterface.itkImageToImagePlusSlice(this.itkImgViewRef,(int)Math.ceil(this.viewSlice*1.0/(this.lookLikeOptimizerLooks ? shrinkFactor : 1)));
		if(this.flagRange)this.sliceViewRef.setDisplayRange(this.refRange[0], this.refRange[1]);
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
		else this.resampler.setTransform(ItkTransform.getTransformForResampling(this.voxelSizeReference, VitimageUtils.getVoxelSizes(this.ijImgMov)));
		this.itkImgViewMov=this.resampler.execute(this.itkImgMov);
		if(this.lookLikeOptimizerLooks) {
			for(int i=0;i<3;i++) {
				if(this.imageSizeReference[i]>=4) {
					this.gaussFilter.setDirection(i);
					itkImgViewRef=this.gaussFilter.execute(this.itkImgViewRef);	
				}
			}
		}
		this.sliceViewMov=ItkImagePlusInterface.itkImageToImagePlusSlice(this.itkImgViewMov,(int)Math.ceil(this.viewSlice*1.0/(this.lookLikeOptimizerLooks ? shrinkFactor : 1)));
		if(this.flagRange)this.sliceViewMov.setDisplayRange(this.movRange[0], this.movRange[1]);

		//Compose the images
		if(this.sliceView==null || this.sliceViewRef.getWidth() != sliceView.getWidth()) {
			if(this.sliceView!=null) {this.sliceView.changes=false;this.sliceView.close();}
			if(flagRange)this.sliceView=VitimageUtils.compositeNoAdjustOf(this.sliceViewRef,this.sliceViewMov,"Registration is running. Red=Reference, Green=moving");
			else this.sliceView=VitimageUtils.compositeOf(this.sliceViewRef,this.sliceViewMov,"Registration is running. Red=Reference, Green=moving");
			this.sliceView.show();
			this.sliceView.getWindow().setSize(this.viewWidth,this.viewHeight);
			this.sliceView.getCanvas().fitToWindow();	
		}
		else {//Copie en place
			ImagePlus temp=null;
			if(flagRange)temp=VitimageUtils.compositeNoAdjustOf(this.sliceViewRef,this.sliceViewMov,"Red=Reference, Green=moving");
			else temp=VitimageUtils.compositeOf(this.sliceViewRef,this.sliceViewMov,"Red=Reference, Green=moving");
			temp=VitimageUtils.writeTextOnImage(viewText,temp,this.fontSize*temp.getWidth()/this.imageSizeReference[0],0);
			temp=VitimageUtils.writeTextOnImage(currentTrans.drawableString(),temp,this.fontSize*temp.getWidth()/this.imageSizeReference[0]-2,1);
			IJ.run(this.sliceView, "Select All", "");
			IJ.run(temp, "Select All", "");
			temp.copy();
			this.sliceView.paste();			
		}

		//Prepare slices for summary 
		this.resampler=new ResampleImageFilter();
		this.resampler.setDefaultPixelValue(0);
		this.resampler.setOutputSpacing(ItkImagePlusInterface.doubleArrayToVectorDouble(this.voxelSizeReference));
		this.resampler.setSize(ItkImagePlusInterface.intArrayToVectorUInt32(this.imageSizeReference));
		this.resampler.setTransform(ItkTransform.getTransformForResampling(this.voxelSizeReference, this.voxelSizeReference));
		this.itkSummaryRef=this.resampler.execute(this.itkImgViewRef);
		this.itkSummaryMov=this.resampler.execute(this.itkImgViewMov);
		if(movie3D) {
			this.sliceSummaryMov=ItkImagePlusInterface.itkImageToImagePlus(this.itkSummaryMov);
			this.sliceSummaryRef=ItkImagePlusInterface.itkImageToImagePlus(this.itkSummaryRef);
		}
		else {
			this.sliceSummaryMov=ItkImagePlusInterface.itkImageToImagePlusSlice(this.itkSummaryMov,this.viewSlice);
			this.sliceSummaryRef=ItkImagePlusInterface.itkImageToImagePlusSlice(this.itkSummaryRef,this.viewSlice);
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
	 * Sets the text info at each iteration on.
	 */
	public void setTextInfoAtEachIterationOn() {
		textInfoAtEachIteration=true;
	}
	
	/**
	 * Sets the text info at each iteration off.
	 */
	public void setTextInfoAtEachIterationOff() {
		textInfoAtEachIteration=false;
	}
	

	
	

	/**
	 * Initializers.
	 *
	 * @param imgIn the new reference image
	 */
	public void setReferenceImage(ImagePlus imgIn) {
		ImagePlus img=null;
		if(imgIn.getType()==ImagePlus.GRAY32)img=VitimageUtils.imageCopy(imgIn);
		else if(imgIn.getType()==ImagePlus.GRAY16)img=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgIn);
		else if(imgIn.getType()==ImagePlus.GRAY8)img=VitimageUtils.convertByteToFloatWithoutDynamicChanges(imgIn);
		else IJ.log("Warning : unusual type of image in ITKRegistrationManager.setReferenceImage : "+imgIn.getType()+" . Registration will fail shortly");
		this.ijImgRef=VitimageUtils.imageCopy(img);
		this.viewHeight=ijImgRef.getHeight()*zoomFactor;
		this.viewWidth=ijImgRef.getWidth()*zoomFactor;
		this.itkImgRef=ItkImagePlusInterface.imagePlusToItkImage(ijImgRef);
		this.imageSizeReference=new int[] {ijImgRef.getWidth(),ijImgRef.getHeight(),ijImgRef.getStackSize()};
		this.voxelSizeReference=new double[] {ijImgRef.getCalibration().pixelWidth,ijImgRef.getCalibration().pixelHeight,ijImgRef.getCalibration().pixelDepth};
		this.viewSlice=(int)Math.round(ijImgRef.getStackSize()/2.0);
	}

	/**
	 * Sets the view slice.
	 *
	 * @param slic the new view slice
	 */
	public void setViewSlice(int slic) {
		this.viewSlice=slic;		
	}
	
	/**
	 * Sets the moving image.
	 *
	 * @param imgIn the new moving image
	 */
	public void setMovingImage(ImagePlus imgIn) {
		ImagePlus img=null;
		VitimageUtils.printImageResume(imgIn);
		if(imgIn.getType()==ImagePlus.GRAY32)img=VitimageUtils.imageCopy(imgIn);
		else if(imgIn.getType()==ImagePlus.GRAY16)img=VitimageUtils.convertShortToFloatWithoutDynamicChanges(imgIn);
		else if(imgIn.getType()==ImagePlus.GRAY8) {img=VitimageUtils.convertByteToFloatWithoutDynamicChanges(imgIn);}
		else IJ.log("Warning : unusual type of image in ITKRegistrationManager.setMovingImage : "+imgIn.getType()+" . Registration will fail shortly");
		this.ijImgMov=VitimageUtils.imageCopy(img);
		this.itkImgMov=ItkImagePlusInterface.imagePlusToItkImage(ijImgMov);
		
	}

	/**
	 * Sets the metric.
	 *
	 * @param metricType the new metric
	 */
	public void setMetric(MetricType metricType) {
		this.metricType=metricType;
	}

	/**
	 * Sets the initial transformation.
	 *
	 * @param trans the new initial transformation
	 */
	public void setInitialTransformation(ItkTransform trans) {
		if(this.transform !=null || this.currentStep>0) System.err.println("Une transformation initiale est deja existante");
		else this.transform=new ItkTransform(trans);
	}

	/**
	 * Creates the updater.
	 */
	public void createUpdater() {
		this.updater=new IterationUpdate(this,this.registrationMethods.get(currentStep));
		this.registrationMethods.get(this.currentStep).removeAllCommands();
		this.registrationMethods.get(this.currentStep).addCommand(EventEnum.sitkIterationEvent,this.updater);
	}






	/**
	 * Helper functions to build the pyramidal scheme based on few parameters.
	 *
	 * @param levelMin the level min
	 * @param levelMax the level max
	 * @param doubleIterAtCoarsestLevels the double iter at coarsest levels
	 * @param basis the basis
	 * @return the int[]
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
				ret[nbTot-1-(2+i*2)]=(int)Math.round(Math.pow(basis,(levelMin-1+i+2))) ;
				ret[nbTot-1-(2+i*2+1)]= (int)Math.round(Math.pow(basis,(levelMin-1+i+2)));
				
			}
		}
		else {
			for(int i=2;i<nbTot;i++) {
					ret[nbTot-1-i]=(int)Math.round(Math.pow(basis,(levelMin-1+i))) ;
			}

		}		
		return ret;
	}

	/**
	 * Image dims at successive levels.
	 *
	 * @param dimsImg the dims img
	 * @param subFactors the sub factors
	 * @return the int[][]
	 */
	public int [][] imageDimsAtSuccessiveLevels(int []dimsImg,int []subFactors) {		
		if (subFactors==null)return null;
		int n=subFactors.length;
		int [][]ret=new int[n][3];
		for(int i=0;i<n;i++) for(int j=0;j<3;j++)ret[i][j]=(int)Math.ceil(dimsImg[j]/subFactors[i]);
		return ret;
	}

	/**
	 * Sigma factors at successive levels.
	 *
	 * @param voxSize the vox size
	 * @param subFactors the sub factors
	 * @param rapportSigma the rapport sigma
	 * @return the double[]
	 */
	public double[] sigmaFactorsAtSuccessiveLevels(double []voxSize,int[]subFactors,double rapportSigma) {
		if (subFactors==null)return null;
		double[]ret=new double[subFactors.length];
		for(int i=0;i<subFactors.length;i++) {
			double voxSizeMin=Math.min(Math.min(voxSize[0]*subFactors[i],voxSize[1]*subFactors[i]),voxSize[2]*subFactors[i]);
			ret[i]=voxSizeMin*rapportSigma;
		}
		return ret;
	}




	/**
	 * Minor getters/setters.
	 *
	 * @return the nb levels
	 */
	public ArrayList<Integer> getNbLevels() {
		return nbLevels;
	}

	/**
	 * Sets the nb levels.
	 *
	 * @param nbLevels the new nb levels
	 */
	public void setNbLevels(ArrayList<Integer> nbLevels) {
		this.nbLevels = nbLevels;
	}

	/**
	 * Gets the current shrink factors.
	 *
	 * @return the current shrink factors
	 */
	public int[] getCurrentShrinkFactors() {
		return shrinkFactors.get(currentStep);
	}

	/**
	 * Sets the shrink factors.
	 *
	 * @param shrinkFactors the new shrink factors
	 */
	public void setShrinkFactors(ArrayList<int[]> shrinkFactors) {
		this.shrinkFactors = shrinkFactors;
	}

	/**
	 * Gets the current dimensions.
	 *
	 * @return the current dimensions
	 */
	public int[][] getCurrentDimensions() {
		return dimensions.get(currentStep);
	}

	/**
	 * Gets the current sigma factors.
	 *
	 * @return the current sigma factors
	 */
	public double[] getCurrentSigmaFactors() {
		return sigmaFactors.get(currentStep);
	}

	/**
	 * Gets the current level.
	 *
	 * @return the current level
	 */
	public int getCurrentLevel() {
		return currentLevel;
	}

	/**
	 * Sets the current level.
	 *
	 * @param currentLevel the new current level
	 */
	public void setCurrentLevel(int currentLevel) {
		this.currentLevel = currentLevel;
	}

	/**
	 * Gets the scaler types.
	 *
	 * @return the scaler types
	 */
	public ArrayList<ScalerType> getScalerTypes() {
		return scalerTypes;
	}

	/**
	 * Sets the scaler types.
	 *
	 * @param scalerTypes the new scaler types
	 */
	public void setScalerTypes(ArrayList<ScalerType> scalerTypes) {
		this.scalerTypes = scalerTypes;
	}

	/**
	 * Gets the nb steps.
	 *
	 * @return the nb steps
	 */
	public int getNbSteps() {
		return nbStep;
	}

	/**
	 * Sets the nb steps.
	 *
	 * @param nbStep the new nb steps
	 */
	public void setNbSteps(int nbStep) {
		this.nbStep = nbStep;
	}

	/**
	 * Gets the current step.
	 *
	 * @return the current step
	 */
	public int getCurrentStep() {
		return currentStep;
	}

	/**
	 * Sets the current step.
	 *
	 * @param currentStep the new current step
	 */
	public void setCurrentStep(int currentStep) {
		this.currentStep = currentStep;
	}

	/**
	 * Gets the weights.
	 *
	 * @return the weights
	 */
	public ArrayList<double[]> getWeights() {
		return weights;
	}

	/**
	 * Sets the weights.
	 *
	 * @param weights the new weights
	 */
	public void setWeights(ArrayList<double[]> weights) {
		this.weights = weights;
	}

	/**
	 * Gets the scales.
	 *
	 * @return the scales
	 */
	public ArrayList<double[]> getScales() {
		return scales;
	}

	/**
	 * Sets the scales.
	 *
	 * @param scales the new scales
	 */
	public void setScales(ArrayList<double[]> scales) {
		this.scales = scales;
	}

	/**
	 * Gets the current transform.
	 *
	 * @return the current transform
	 */
	public ItkTransform getCurrentTransform() {
		return new ItkTransform(this.transform);
	}
	

	
}
	

/**
 * Listener for gathering optimizer events. It allows the manager to update and display the process state along the running
 */
class IterationUpdate  extends Command {
	private double initScore=1;
	private ImageRegistrationMethod method;
	private ItkRegistration manager;
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
	public IterationUpdate(ItkRegistration manager,ImageRegistrationMethod method) {
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
				st+="Level "+(mem-1)+" finished. Going to next level\n\n\n\n";
				for(int i=0;i<mem;i++)st+="     ";
			}
			st+="Level "+(mem+1)+" / "+tabShrink.length+" .\n Sigma for smoothing = "+(VitimageUtils.dou(tabSigma[mem]))+" mm . Subsampling factor = "+tabShrink[(int) mem]+
					" .  Image size = "+tabSizes[mem][0]+" x "+tabSizes[mem][1]+" x "+tabSizes[mem][2]+" . Execution with "+method.getNumberOfThreads()+" threads. Starting.\n";
			for(int i=0;i<mem;i++)st+="     ";
			st+="----------";		  
			IJ.log(st);		  
			//IJ.log(st);
		}
		memoirePyramide=method.getOptimizerIteration();
		String pyr="";
		for(int i=1;i<=method.getCurrentLevel();i++)pyr+="     ";
		pyr+="Step "+(manager.getCurrentStep()+1)+"/"+manager.getNbSteps()+" | Level "+(method.getCurrentLevel()+1)+"/"+tabSigma.length+" | ";
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
		else if(method.getOptimizerIteration()%(manager.textInfoAtEachIteration ? 1 :  10    )==0)System.out.format("%sIteration %3d  |  Score = %6.4f  |  Titeration = %8.4f %s  |  Ttotal = %5.2f %s  | Evolution = %4.2f%% |\n",
				pyr,method.getOptimizerIteration()
				,-100.0*method.getMetricValue()
				, (float)durIter,unitIter
				, (float)durTot,unitTot,improvement
				);
		if(durationTot>nextViewTime) {
			nextViewTime=durationTot+refreshingPeriod;
			String st=String.format("Step=%1d/%1d - Level=%1d/%1d - Iter=%3d - Score=%6.4f - Evolution=%4.2f%%",
					(manager.getCurrentStep()+1),
					manager.getNbSteps(),
					(method.getCurrentLevel()+1),tabSigma.length,method.getOptimizerIteration(),-100.0*method.getMetricValue(),improvement);
			this.manager.updateView(this.tabSizes[mem],tabSigma[mem],this.tabShrink[mem],st,new ItkTransform(method.getInitialTransform()));
		}
	}

}
