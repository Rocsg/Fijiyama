package com.vitimage.mrutils;

import com.vitimage.common.VitimageUtils;

public class RiceEstimator {

	public static void main(String[]args) {
		RiceEstimator rice=getDefaultRiceEstimatorForNormalizedHyperEchoesT1AndT2Images();
		double sigma=0.05;
		double[]stats=computeSigmaAndMeanBgFromRiceSigmaStatic(sigma);
		System.out.println("Sigma rice="+sigma+"  equivalent mean in zero signal="+stats[0]+"  equivalent std in zero signal="+stats[1]);
		for(double obs=0;obs<1;obs+=0.01) {
			double mea=besFunkCost(obs, sigma);
			double sig=besFunkSigma(obs, sigma);
			System.out.println("Obs="+VitimageUtils.dou(obs)+" Mean="+VitimageUtils.dou(mea)+"  Sigma="+VitimageUtils.dou(sig));
		}
	}
	
	private final static double epsilon=0.00001;
	private final static boolean debug=false;
	private double sigmaRiceMin;
	private double sigmaRiceMax;
	private double observationMin;
	private double observationMax;
	private double []observationRange;
	private double []sigmaRange;
	private int nSig;
	private int nObs;
	private double[][][]lut;
	
	

	
	
	public static RiceEstimator getDefaultRiceEstimatorForNormalizedHyperEchoesT1AndT2Images() {
		RiceEstimator rice=new RiceEstimator(0.0001,1,0.0001,20,1000,1000);
		rice.start();
		return rice;
	}
	
	public RiceEstimator(double sigmaRiceMin,double sigmaRiceMax,double observationMin,double observationMax,int nSig,int nObs) {
		this.sigmaRiceMax=sigmaRiceMax;
		this.sigmaRiceMin=sigmaRiceMin;
		this.observationMin=observationMin;
		this.observationMax=observationMax;
		this.nSig=nSig;
		this.nObs=nObs;
		this.observationRange=new double[nObs];
		this.sigmaRange=new double[nSig];
	}

	public void start() {
		buildSigmaRange();
		buildObservationRange();
		buildLookupTable();
	}
	
	
	
	//Values of sigma table
	public void buildSigmaRange() {
		if(debug)System.out.print("Construction array sigma : ");
		for(int sig=0;sig<nSig;sig++) {
			sigmaRange[sig]=sigmaRiceMin+(sig*1.0)*(sigmaRiceMax-sigmaRiceMin)/(nSig-1);
			if(debug)System.out.print("sig"+sig+"="+sigmaRange[sig]+" , ");
		}
		if(debug)System.out.println();
	}
	
	//index of a sigma in the table
	public double getSigmaCoord(double sigma) {
		double coord=(sigma-sigmaRiceMin)/(sigmaRiceMax-sigmaRiceMin)*(nSig-1);
		if(coord<=0)coord=epsilon;
		if(coord>=nSig-1)coord=nSig-1-epsilon;
		return coord;
	}
	
	//Values of the observations table
	public void buildObservationRange() {
		if(debug)System.out.print("Construction array observations : ");
		double multFactor=this.observationMax/this.observationMin;
		for(int obs=0;obs<nObs;obs++) {		
			observationRange[obs]=this.observationMin*Math.pow(multFactor,obs*1.0/(nObs-1));
			if(debug)System.out.print("obs"+obs+"="+observationRange[obs]+" , ");
		}		
		if(debug)System.out.println();
	}

	//index of an observation in the table
	public double getObservationCoord(double observation) {
		if(observation>=this.observationMax)return nObs-1-epsilon;
		else if(observation<=this.observationMin)return epsilon;
		
		double valMin=this.observationMin;double valMax=this.observationMax;int indMin=0;int indMax=nObs-1;int indMed;double valMed;
		do {
			indMed=(int)Math.round(0.5*(indMax+indMin));
			valMed=observationRange[indMed];
			if(observation<valMed) {indMax=indMed;valMax=valMed;}
			else {indMin=indMed;valMin=valMed;}			
		}
		while((indMax-indMin)>1);
		return indMin+(observation-valMin)/(valMax-valMin);
	}

	
	public void buildLookupTable() {
		double interMin, interMax,interMed,valMin,valMax,valMed;
		System.out.print("Building lookup table for estimation of initial signal amplitude using max likelyhood reverse estimation from observations X rice noise ...");
		int nTot=this.nObs*this.nSig;
		int incr=0;
		this.lut=new double[nSig][nObs][3];//SigmaRice, valObservation, EquivalentNormalSigma
		for(int sig=0;sig<nSig;sig++) {
			double curSigRice=sigmaRange[sig];
			for(int obs=0;obs<nObs;obs++) {
				if((incr++)%50000==0)System.out.print(""+(incr*100)/nTot+"%  ");
				double curObs=observationRange[obs];

				//Looking for the most likelyhood initial signal which leads to observe curObs, when noised by a Rice noise of parameter curSigRice 
				interMax=curObs;interMin=0;interMed=0;
				if(besFunkCost(interMin,curSigRice)>curObs) {}
				else {
					//Recherche iterative
					valMin=besFunkCost(interMin, curSigRice);
					valMax=besFunkCost(interMax, curSigRice);
					do {
						interMed=(interMax+interMin)/2;
						valMed=besFunkCost(interMed, curSigRice);
						if(valMed>=curObs) {interMax=interMed;valMax=valMed;}
						else {interMin=interMed;valMin=valMed;}
					}
					while(Math.abs(valMed-curObs)>epsilon);
				}
				lut[sig][obs]=new double[] {curSigRice,curObs,interMed};
				if(debug)System.out.println("sig"+sig+"  obs"+obs+"  : sigma="+curSigRice+"  observation="+ curObs+"  initialSignal="+interMed);
			}
		}
		System.out.println();
	}
			
	
	public double estimateSigma(double sigmaRice,double observation) {
		double initialSignal=estimateInitialSignal(sigmaRice,observation);
		return besFunkSigma(initialSignal,sigmaRice);
	}
	
	public double []estimateSigmas(double sigmaRice,double []observations) {
		double[]ret=new double[observations.length];
		for(int r=0;r<ret.length;r++)ret[r]=estimateSigma(sigmaRice,observations[r]);
		return ret;
	}
	
	public double estimateInitialSignal(double sigmaRice,double observation) {
		double coordObs=getObservationCoord(observation);
		double coordSig=getSigmaCoord(sigmaRice);
		int corObs0=(int)Math.floor(coordObs);
		int corSig0=(int)Math.floor(coordSig);
		double dObs=coordObs-corObs0;
		double dSig=coordSig-corSig0;
		double targetValue=  dSig    *    dObs    *this.lut[corSig0+1][corObs0+1][2]  +  
		              		 (1-dSig)*    dObs    *this.lut[corSig0  ][corObs0+1][2]  +  
		              		 dSig    *  (1-dObs)  *this.lut[corSig0+1][corObs0  ][2]  +  
		              		 (1-dSig)*  (1-dObs)  *this.lut[corSig0  ][corObs0  ][2];
	
		double erreur=Math.abs(besFunkCost(targetValue, sigmaRice)-observation)/observation*100.0;
		if(debug)System.out.println("\nESTIMATING INITIAL SIGMA from sigmaRice="+sigmaRice+" and obs="+observation);
		if(debug)System.out.println("Résultat : "+targetValue+ " jitter="+erreur+" % ");
		if(debug) {
			System.out.println("\nESTIMATING INITIAL SIGMA from sigmaRice="+sigmaRice+" and obs="+observation);
			System.out.println("Coordonnees detectees : coordSig="+coordSig+"  coordObs="+coordObs);		
			System.out.println("Valeurs 1 1 pond "+(dSig*dObs)+" : sig="+this.lut[corSig0 +1 ][corObs0  +1  ][0]+"  obs="+this.lut[corSig0  +1  ][corObs0  +1  ][1]+"  signal="+this.lut[corSig0  +1  ][corObs0   +1 ][2]);
			System.out.println("Valeurs 0 1 pond "+((1-dSig)*dObs)+" : sig="+this.lut[corSig0  ][corObs0 +1 ][0]+"  obs="+this.lut[corSig0  ][corObs0 +1 ][1]+"  signal="+this.lut[corSig0  ][corObs0 +1 ][2]);
			System.out.println("Valeurs 1 0 pond "+(dSig*(1-dObs))+" : sig="+this.lut[corSig0 +1 ][corObs0  ][0]+"  obs="+this.lut[corSig0 +1 ][corObs0  ][1]+"  signal="+this.lut[corSig0 +1 ][corObs0  ][2]);
			System.out.println("Valeurs 0 0 pond "+((1-dSig)*(1-dObs))+" : sig="+this.lut[corSig0  ][corObs0  ][0]+"  obs="+this.lut[corSig0  ][corObs0  ][1]+"  signal="+this.lut[corSig0  ][corObs0  ][2]);
			System.out.println("Résultat : "+targetValue);
		}
		return targetValue;
	}
	

	static double sigmaWay(double valFunk,double sigma){
		double ret=valFunk*valFunk-2*sigma*sigma;
		return (ret<0 ? 0 : Math.sqrt(ret));
	}
	    
	static double besFunkCost(double d,double sigma2) {
		double alpha=d*d/(4*sigma2*sigma2);
		return (double)(Math.sqrt(Math.PI*sigma2*sigma2/2.0)*( (1+2*alpha)*bessi0NoExp(alpha) + 2*alpha*bessi1NoExp(alpha) ));
	}

	static double besFunkSigma(double d,double sigma2) {
		double alpha=d*d/(4*sigma2*sigma2);
		return Math.sqrt(2*sigma2*sigma2+d*d  - (Math.PI*sigma2*sigma2/2.0)*Math.pow( (1+2*alpha)*bessi0NoExp(alpha) + 2*alpha*bessi1NoExp(alpha) ,2));
	}

	
	static double besFunkDeriv(double d,double sigma) {
		double alpha=d*d/(4*sigma*sigma);
		return (double)(Math.sqrt(Math.PI*alpha/2.0)*(bessi0NoExp(alpha) + bessi1NoExp(alpha) ));
	}
 
	static double bessi0NoExp( double alpha ){
		double x=(double) alpha;
		double ax,ans;
		double y;
	   ax=Math.abs(x);
	   
	   if (ax < 3.75) {
	      y=x/3.75;
	      y=y*y;
	      ans=1/Math.exp(ax)*(1.0+y*(3.5156229+y*(3.0899424+y*(1.2067492+y*(0.2659732+y*(0.0360768+y*0.0045813))))));
	   } else {
	      y=3.75/ax;
	      ans=(1/Math.sqrt(ax))*(0.39894228+y*(0.01328592+y*(0.00225319+y*(-0.00157565+y*(0.00916281+y*(-0.02057706+y*(0.02635537+y*(-0.01647633+y*0.00392377))))))));
	   }
	   return (double)ans;
	}

	static double bessi1NoExp( double alpha){
			double ax,ans;
			double y;
			double x=(double)alpha;
			ax=Math.abs(x);
			if (ax < 3.75) {
		      y=x/3.75;
		      y=y*y;
		      ans=ax/Math.exp(ax)*(0.5+y*(0.87890594+y*(0.51498869+y*(0.15084934+y*(0.02658733+y*(0.00301532+y*0.00032411))))));
		   } else {
		      y=3.75/ax;
		      ans=0.02282967+y*(-0.02895312+y*(0.01787654-y*0.00420059));
		      ans=0.39894228+y*(-0.03988024+y*(-0.00362018+y*(0.00163801+y*(-0.01031555+y*ans))));
		      ans *= (1/Math.sqrt(ax));
		   }
		   return ((double)(x < 0.0 ? -ans : ans));
		}
		
	public static double computeRiceSigmaFromBackgroundValuesStatic(double meanBg,double sigmaBg) {
		boolean debug=true;
		double val1=meanBg * Math.sqrt(2.0/Math.PI);
		double val2=sigmaBg * Math.sqrt(2.0/(4.0-Math.PI));
		if(debug && Math.abs((val1-val2)/val2) >0.3) System.out.println("Warning : Acquisition > computeRiceSigmaStatic. Given :M="+meanBg+" , S="+sigmaBg+" gives sm="+val1+" , ss="+val2+"  .sm/ss="+VitimageUtils.dou(val1/val2)+". Using the first one..");
		
		return val1;
	}

	

	public static double []computeSigmaAndMeanBgFromRiceSigmaStatic(double sigmaRice) {
		boolean debug=true;
		double meanBg=sigmaRice/(Math.sqrt(2.0/Math.PI));
		double stdBg=sigmaRice/(Math.sqrt(2.0/(4.0-Math.PI)));
		return new double[] {meanBg,stdBg};
	}


	
	
}
