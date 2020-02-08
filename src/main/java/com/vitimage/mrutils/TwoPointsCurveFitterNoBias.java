package com.vitimage.mrutils;

public class TwoPointsCurveFitterNoBias{

	protected double sigma;  
	protected int fit;                // Number of curve type to fit
	protected double[] xData, yData,yDataSave;  // x,y data to fit
	protected int numPoints;          // number of data points
	protected int numParams;          // number of parametres

	private double[] params;

	/** Construct a new SimplexCurveFitter. */
	public TwoPointsCurveFitterNoBias (double[] xData, double[] yData, int fitType,double sigma) {
		this.sigma=sigma;
		this.xData = xData;
		this.yData = yData;
		this.yDataSave = new double[xData.length];
		this.numPoints = xData.length;
		this.numParams= (fitType==MRUtils.T2_RELAX_BIAS ? 3 : 2);
		this.params= new double[this.numParams];
		this.fit=fitType;

	}



	public static double sigmaWay(double valFunk,double sigma){
		return valFunk*valFunk-2*sigma*sigma;
	}



	public double gozeM0(double T2){
		if(T2==0)return MRUtils.ERROR_VALUE;
		return yData[0]*Math.exp(xData[0]/T2);
	}

	public double gozeT2(){
		if(yData[0]==yData[2])return MRUtils.ERROR_VALUE;
		return((-xData[0]+xData[2])/Math.log(yData[0]/yData[2]));
	}

	public double gozeT1(){
		if(yData[0]>=yData[1])return MRUtils.ERROR_VALUE;
		double a=1;
		double b=-yData[1]/yData[0];
		double c=yData[1]/yData[0]-1;
		double delta=b*b-4*a*c;
		double deltaSq=Math.sqrt(delta);
		double ret=-xData[0]/Math.log( (-b-deltaSq)/(2*a) );
		ret=ret<=0 ? MRUtils.ERROR_VALUE : ret;
		return ret;
	}

	public double gozeM0OfT1(double T1){
		if(T1==MRUtils.ERROR_VALUE)return MRUtils.ERROR_VALUE;
		return yData[1]/(1-Math.exp(-xData[1]/T1));
	}

	public void doFit() {
		doFit(fit, false);
	}

	public void doFit(int fitType) {
		doFit(fitType, false);
	}

	public void doFit(int fitType, boolean showSettings) {

		fit = fitType;
		switch (fit) {
		case MRUtils.T1_RECOVERY:
			params[1]=gozeT1();
			params[0]=gozeM0OfT1(params[1]);
			break;
		case MRUtils.T2_RELAX_SIGMA:
			for(int i =0;i<numPoints;i++){yDataSave[i]=yData[i];yData[i]=sigmaWay(yData[i],sigma);}
			params[1]=gozeT2();
			params[0]=Math.sqrt(gozeM0(params[1]));
			params[1]=params[1]*2;
			for(int i =0;i<numPoints;i++){yData[i]=yDataSave[i];}
			break;
		case MRUtils.T2_RELAX:
			params[1]=gozeT2();
			params[0]=gozeM0(params[1]);
			break;
		}
	}




	/** Get number of parameters for current fit formula */
	public int getNumParams(int fitType) {
		switch (fitType) {
		case MRUtils.T2_RELAX:  return 2;
		case MRUtils.T2_RELAX_RICE:  return 2;
		case MRUtils.T2_RELAX_SIGMA:  return 2;
		}
		return 0;
	}


	/** Get the set of parameter values from the best corner of the simplex */
	public double[] getParams() {
		return params;
	}


}
