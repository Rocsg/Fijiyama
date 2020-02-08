package com.vitimage.mrutils;
import com.vitimage.common.VitimageUtils;

import lma.LMAFunction;
import lma.LMAMatrix.InvertException;
import lma.implementations.LMA;

public class LMCurveFitterNoBias {

	public boolean debugLM=false;
	public final static String[] timeunits={"ms", "s"};
	public final static int[] timeitems={MRUtils.MSEC, MRUtils.SEC};
	public final static String[] fititems2={"Simplex","Levenberg-Marquardt"};
	public final static int[] constitems2={MRUtils.SIMPLEX,MRUtils.LM};


	protected int fit;                // Number of curve type to fit

	protected double[][] data; // x,y data to fit
	private LMA lma;
	private double[] parameters;

	private float gfit;
	public static double lambda = 0.001;
	public static double minDeltaChi2 = 1e-30;
	public static int maxIterations=200;

	
	
	 

	static double sigmaWay(double valFunk,double sigma){
		double ret=valFunk*valFunk-2*sigma*sigma;
		return (ret<0 ? 0 : Math.sqrt(ret));
	}
	    
	static double besFunkCost(double d,double sigma2) {
		double alpha=d*d/(4*sigma2*sigma2);
		return (double)(Math.sqrt(Math.PI*sigma2*sigma2/2.0)*( (1+2*alpha)*bessi0NoExp(alpha) + 2*alpha*bessi1NoExp(alpha) ));
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
		
		
	
	
	
	

	/** Construct a new CurveFitter. */
	public LMCurveFitterNoBias (double[] xData, double[] yData, int fitType,double sigma,boolean debugLM) {
		this.fit=fitType;
		this.debugLM=debugLM;
		if (fit<MRUtils.STRAIGHT_LINE)
			throw new IllegalArgumentException("Invalid fit type");
		int xlength=xData.length;
		int ylength=yData.length;
		if (xlength!= ylength)
			throw new IllegalArgumentException("Arrays' lengths do not match");

		data=new double[2][xlength];
		data[0]=xData;
		data[1]=yData;

		double firstx = xData[0];
		double firsty = yData[0];
		double thirdx = xData[2];
		double thirdy = yData[2];

		double mult=Math.sqrt(firsty/thirdy);
		double val=firsty*mult;
		double tHope=(thirdx-firstx)/(2*Math.log(mult));
		if(tHope<5 || tHope > 80)tHope=20;
		double tHope2=tHope*3;
		double lastx = xData[xlength-1];
		double lasty = yData[xlength-1];
		int np=getNumParams(fitType);

		parameters=new double[np];
		double[] inparameters=new double[np];
		double slope=10.0;
		double islope=100;
		if ((lastx - firstx) != 0.0)
			slope = (lasty - firsty)/(lastx - firstx);

		double yintercept = firsty - slope * firstx;

		switch (fit) {
		case MRUtils.STRAIGHT_LINE:
			inparameters[0] = slope;
			inparameters[1] = yintercept;
			lma = new LMA(new Line(),inparameters,data);
			break;
		case MRUtils.T1_RECOVERY:
			inparameters[0] = lasty;
			inparameters[1] = 1000.0;
			T1Recovery t1r=new T1Recovery();
			t1r.setSigma(sigma);
			lma = new LMA(t1r,inparameters,data);
			break;
		case MRUtils.T1_RECOVERY_RICE:
			inparameters[0] = lasty;
			inparameters[1] = 1000.0;
			T1RecoveryRice t1rr=new T1RecoveryRice();
			t1rr.setSigma(sigma);
			t1rr.setLM(this);
			lma = new LMA(t1rr,inparameters,data);
			break;

		case MRUtils.T2_RELAX_RICE:
			inparameters[0] = val;
			inparameters[1] = 40;//tHope;
			T2RelaxRice t2rr=new T2RelaxRice();
			t2rr.setSigma(sigma);
			t2rr.setLM(this);
			lma = new LMA(t2rr,inparameters,data);
			break;

		case MRUtils.MULTICOMP:
			inparameters[0] = val/2;
			inparameters[1] = 30;//tHope;
			inparameters[2] = val/2;
			inparameters[3] = 60;//tHope2;
			lma = new LMA(
					new TMulticomp(),inparameters,
					data);
			break;
		case MRUtils.MULTICOMP_RICE:
			inparameters[0] = val/2;
			inparameters[1] = 20;//tHope;
			inparameters[2] = val/3;
			inparameters[3] = 100;//tHope2;
			TMulticompRice t2mcr=new TMulticompRice();
			t2mcr.setSigma(sigma);
			t2mcr.setLM(this);
			lma = new LMA(t2mcr,inparameters,data);
			break;
		case MRUtils.TRICOMP_RICE:
			inparameters[0] = val/3;
			inparameters[1] = 15;//tHope;
			inparameters[2] = val/4;
			inparameters[3] = 40;//tHope2;
			inparameters[2] = val/5;
			inparameters[3] = 80;//tHope2;
			TTricompRice t3mcr=new TTricompRice();
			t3mcr.setLM(this);
			t3mcr.setSigma(sigma);
			lma = new LMA(t3mcr,inparameters,data);
			break;
		
		}
	}


	public void configLMA(double lambda, double minDeltaChi2, int maxIterations) {
		if (lma==null) return;
		lma.lambda=lambda;
		lma.minDeltaChi2=minDeltaChi2;
		lma.maxIterations=maxIterations;
	}

	public void doFit() {
		try {
			lma.fit();
			parameters=lma.parameters;
			gfit=lma.chi2Goodness();

		} catch (InvertException e) {
			for(int i=0;i<parameters.length;i++)parameters[i]=MRUtils.ERROR_VALUE;
		}
	}

	public void doFit(double lambda, double minDeltaChi2, int maxIterations) {
		try {
			lma.fit( lambda,  minDeltaChi2,  maxIterations);
			parameters=lma.parameters;
			gfit=lma.chi2Goodness();
		} catch (InvertException e) {
			for(int i=0;i<parameters.length;i++)parameters[i]=MRUtils.ERROR_VALUE;}
	}

	public double[] getParams() {
		return parameters;
	}

	public LMA getLMA() {
		return lma;
	}

	/** Get number of parameters for current fit formula */
	public static int getNumParams(int fitType) {
		switch (fitType) {
		case MRUtils.STRAIGHT_LINE: return Line.Nparams  ;
		case MRUtils.T1_RECOVERY: return T1Recovery.Nparams ;  
		case MRUtils.T1_RECOVERY_RICE: return T1RecoveryRice.Nparams ;  
		case MRUtils.T2_RELAX_RICE:  return  T2RelaxRice.Nparams  ;
		case MRUtils.MULTICOMP:  return  TMulticomp.Nparams  ;
		case MRUtils.MULTICOMP_RICE:  return  TMulticompRice.Nparams  ;
		case MRUtils.TRICOMP_RICE:  return  TTricompRice.Nparams  ;
		}
		return 0;
	}



	/*    	p[0]*(1 - Math.exp(-(x / p[1]))); // p[1] - repetition times
	 */
	public static class T1Recovery extends LMAFunction {
		public double sigma=0;
		public LMCurveFitterNoBias lm;
		public void setLM(LMCurveFitterNoBias lm) {this.lm=lm;}
		public void setSigma(double sig){
			sigma=sig;
		}

		public static final int Nparams=2;
		 
		public double getY(double x, double[] a) {
			if(lm.debugLM)System.out.println("T1Recov x="+x+"  a[0]="+a[0]+"  a[1]="+a[1]+"   res="+a[0]*(1 - Math.exp(-(x / a[1]))));
			if(lm.debugLM)VitimageUtils.waitFor(10);
			return  a[0]*(1 - Math.exp(-(x / a[1]))); // a[1] - repetition times
		}

		
		public double getPartialDerivate(double x, double[] a, int parameterIndex) {
			switch (parameterIndex) {
			case 0: return  1.0-Math.exp(-x/a[1]);
			case 1: return  -(a[0]*x*Math.exp(-x/a[1]))/(a[1]*a[1]);

			} throw new RuntimeException("No such parameter index: " + parameterIndex);
		}

		public String toString() {
			return "y=a[0]*(1 - Math.exp(-(x / a[1])))";
		}
	} // end class

	/*    	p[0]*(1 - Math.exp(-(x / p[1]))); // p[1] - repetition times
	 */
	public static class T1RecoveryRice extends LMAFunction {
		public double sigma=0;
		public LMCurveFitterNoBias lm;
		public void setLM(LMCurveFitterNoBias lm) {this.lm=lm;}
		public void setSigma(double sig){
			sigma=sig;
		}
		public static final int Nparams=2;
		 
		public double getY(double x, double[] a) {
			if(lm.debugLM)System.out.println("T1RecovRice sigma="+sigma+"  x="+x+"  a[0]="+a[0]+"  a[1]="+a[1]+"   res="+besFunkCost(a[0]*(1 - Math.exp(-(x / a[1]))),sigma));
			if(lm.debugLM)VitimageUtils.waitFor(10);
			return  besFunkCost(a[0]*(1 - Math.exp(-(x / a[1]))),sigma); // a[1] - repetition times
		}

		@Override
		public double getPartialDerivate(double x, double[] a, int parameterIndex) {
			switch (parameterIndex) {
			case 0: return  besFunkDeriv( a[0]*(1 - Math.exp(-(x / a[1]))), sigma )*(1.0-Math.exp(-x/a[1]));
			case 1: return  besFunkDeriv( a[0]*(1 - Math.exp(-(x / a[1]))), sigma )*(-(a[0]*x*Math.exp(-x/a[1]))/(a[1]*a[1]));

			} throw new RuntimeException("No such parameter index: " + parameterIndex);
		}

		public String toString() {
			return "y=a[0]*(1 - Math.exp(-(x / a[1])))";
		}
	} // end class


	/*
	 * T2_RELAX_RICE:
	                return p[0]*Math.exp(-(x / p[1]) )+ p[2]; // p[1] - echo times
	 */

	public static class T2RelaxRice extends LMAFunction {
		public static final int Nparams=2;
		public double sigma=0;
		public LMCurveFitterNoBias lm;
		public void setLM(LMCurveFitterNoBias lm) {this.lm=lm;}
		public void setSigma(double sig){
			sigma=sig;
		}

		@Override
		public double getY(double x, double[] a) {
			if(lm.debugLM)System.out.println("T2Rice sigma="+sigma+"  x="+x+"  a[0]="+a[0]+"  a[1]="+a[1]+"   res="+besFunkCost( a[0]* Math.exp(-(x / a[1])),sigma));
			if(lm.debugLM)VitimageUtils.waitFor(10);
			return  besFunkCost( a[0]* Math.exp(-(x / a[1])),sigma);	
		}


		@Override
		public double getPartialDerivate(double x, double[] a, int parameterIndex) {
			switch (parameterIndex) {
			case 0: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])), sigma ) * Math.exp(-x/a[1]);  
			case 1: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])), sigma ) * a[0]*x*Math.exp(-x/a[1])/(a[1]*a[1]);
			} 

			throw new RuntimeException("No such parameter index: " + parameterIndex);
		}

		public String toString() {
			return "y=a[0]* Math.exp(-(x / a[1])) +a[2] + rice noise (sigma)";
		}
	} // end class




	public static class TMulticomp extends LMAFunction {
		public static final int Nparams=4;

		@Override
		public double getY(double x, double[] a) {
			//return  a[0]* Math.exp(-(x / a[1])) +a[2]; // a[1] - echo times
			return  a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])); // a[1] - echo times
		}

		@Override
		public double getPartialDerivate(double x, double[] a, int parameterIndex) {
			switch (parameterIndex) {
			case 0: return  Math.exp(-x/a[1]);  
			case 1: return  a[0]*x*Math.exp(-x/a[1])/(a[1]*a[1]);
			case 2: return  Math.exp(-x/a[3]);  
			case 3: return  a[2]*x*Math.exp(-x/a[3])/(a[3]*a[3]);
			} 

			throw new RuntimeException("No such parameter index: " + parameterIndex);
		}

		public String toString() {
			return "y=a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3]))";
		}
	} // end class


	/*
	 * T2TRICOMP_RICE
	                return p[0]*Math.exp(-(x / p[1]) )+ p[2]; // p[1] - echo times
	 */

	public static class TTricompRice extends LMAFunction {
		public static final int Nparams=6;
		public double sigma=0;
		public LMCurveFitterNoBias lm;
		public void setLM(LMCurveFitterNoBias lm) {this.lm=lm;}
		public void setSigma(double sig){
			sigma=sig;
		}

		@Override
		public double getY(double x, double[] a) {
			if(lm.debugLM)System.out.println("T2TRIRice sigma="+sigma+"  x="+x+"  a[0]="+a[0]+"  a[1]="+a[1]+"  a[2]="+a[2]+"  a[3]="+a[3]+"  a[4]="+a[4]+"  a[5]="+a[5]+"   res="+ besFunkCost( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma));
			if(lm.debugLM)VitimageUtils.waitFor(10);
			//return  a[0]* Math.exp(-(x / a[1])) +a[2]; // a[1] - echo times
			return  besFunkCost( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma); // a[1] - echo times
		}

		@Override
		public double getPartialDerivate(double x, double[] a, int parameterIndex) {
			switch (parameterIndex) {
			case 0: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma)*Math.exp(-x/a[1]);  
			case 1: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma)*a[0]*x*Math.exp(-x/a[1])/(a[1]*a[1]);
			case 2: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma)*Math.exp(-x/a[3]);  
			case 3: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma)*a[2]*x*Math.exp(-x/a[3])/(a[3]*a[3]);
			case 4: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma)*Math.exp(-x/a[5]);  
			case 5: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + a[4]* Math.exp(-(x / a[5])), sigma)*a[4]*x*Math.exp(-x/a[5])/(a[5]*a[5]);
			} 

			throw new RuntimeException("No such parameter index: " + parameterIndex);
		}

		public String toString() {
			return "y=a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + Rice noise estimation";
		}
	} // end class

	/*s
	 * T2MULTICOMP_RICE
	                return p[0]*Math.exp(-(x / p[1]) )+ p[2]; // p[1] - echo times
	 */

	public static class TMulticompRice extends LMAFunction {
		public static final int Nparams=4;
		public double sigma=0;
		public LMCurveFitterNoBias lm;
		public void setLM(LMCurveFitterNoBias lm) {this.lm=lm;}
		public void setSigma(double sig){
			sigma=sig;
		}

		
		@Override
		public double getY(double x, double[] a) {
			if(lm.debugLM)System.out.println("T2MULTIRice sigma="+sigma+"  x="+x+"  a[0]="+a[0]+"  a[1]="+a[1]+"  a[2]="+a[2]+"  a[3]="+a[3]+"   res="+besFunkCost( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])), sigma));
			if(lm.debugLM)VitimageUtils.waitFor(10);
			//return  a[0]* Math.exp(-(x / a[1])) +a[2]; // a[1] - echo times
			return  besFunkCost( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])), sigma); // a[1] - echo times
		}

		@Override
		public double getPartialDerivate(double x, double[] a, int parameterIndex) {
			switch (parameterIndex) {
			case 0: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])), sigma)*Math.exp(-x/a[1]);  
			case 1: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])), sigma)*a[0]*x*Math.exp(-x/a[1])/(a[1]*a[1]);
			case 2: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])), sigma)*Math.exp(-x/a[3]);  
			case 3: return  besFunkDeriv( a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])), sigma)*a[2]*x*Math.exp(-x/a[3])/(a[3]*a[3]);
			} 

			throw new RuntimeException("No such parameter index: " + parameterIndex);
		}

		public String toString() {
			return "y=a[0]* Math.exp(-(x / a[1])) + a[2]* Math.exp(-(x / a[3])) + Rice noise estimation";
		}
	} // end class



	/** Linear function with a form of y = a0 * x + a1 */
	public static class Line extends LMAFunction {
		public static final int Nparams=2;

		@Override
		public double getY(double x, double[] a) {
			return a[0] * x + a[1];
		}
		@Override
		public double getPartialDerivate(double x, double[] a, int parameterIndex) {
			switch (parameterIndex) {
			case 0: return x;
			case 1: return 1;
			}
			throw new RuntimeException("No such parameter index: " + parameterIndex);
		}

		public String toString() {
			return "y=a[0] * x + a[1]";
		}
	} // end class
}