package com.vitimage;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.plugin.frame.RoiManager;

public class MRUtils implements Fit{
	
	public MRUtils() {
		
	}
	
	public double []getT1Times(int nb){
		return (  (nb==3) ? (new double[]{600,1200,2400}) : (new double[]{600,1200,2400,10000}));
	}

	public double []getProportionalTimes(double valMin, double valMax,double step){
		double[]tab=new double[(int)(Math.ceil((double)0.00001+((valMax-valMin)/step)))];
		for (int i=0;i<tab.length;i++){
			tab[i]=valMin+(double)((i*step));
		}
		return tab;
	}
	
	public double[]getDataForVoxel(ImagePlus imgIn,int xCor,int yCor){
		int xMax=imgIn.getWidth();
		int yMax=imgIn.getHeight();
		int zMax=imgIn.getStack().getSize();
		double[]data= new double[zMax];
		if( (xCor>xMax-1) || (yCor>yMax-1))IJ.log("Bad coordinates. Data set to 0"); 		
		for(int z=0;z<zMax;z++)data[z]=(double)((float[])(imgIn.getStack().getProcessor(z+1).getPixels()))[xCor + xMax * yCor];
		return data;
	}

	
	

	//Compute the fitten curve, given the parameters previously estimated
	public static double[] fittenRelaxationCurve(double[]tEchos,double []param,double sigma,int fitType){
		double[]tab=new double[tEchos.length];
		double techo;
		for(int indT=0;indT<tEchos.length;indT++){
			techo=tEchos[indT];
			switch(fitType){
				case T2_RELAX: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1])));break;
				case T2_RELAX_SIGMA: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1])));break;
			 	case T2_RELAX_BIAS: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]);break;
				case T2_RELAX_RICE: tab[indT]=(double)(besFunkCost(param[0]* (double)Math.exp(-(techo / param[1])),sigma));break;

				case T1_RECOVERY: tab[indT]=(double)(param[0]* (double)(1-Math.exp(-(techo / param[1]))));break;
				case T1_RECOVERY_RICE: tab[indT]=(double)(besFunkCost(param[0]* (double)(1-Math.exp(-(techo / param[1]))) , sigma));break;
			 	case T1_RECOVERY_RICE_NORMALIZED: tab[indT]=(double)(besFunkCost( (double)(1-Math.exp(-(techo / param[0]))) , sigma));break;

				case MULTICOMP: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])));break;
				case MULTICOMP_BIAS: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3]))+param[4]);break;
				case MULTICOMP_RICE: tab[indT]=(double)(besFunkCost(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])),sigma));break;
			}
		}
		return tab;
	}


	public static double fittingAccuracy(double[] tabData,double[] tabTimes,double sigma,double[] estimatedParams,int fitType){		
		double []realP=fittenRelaxationCurve(tabTimes,estimatedParams,sigma,fitType);
		double cumulator=0;
		for(int indT=0;indT<tabTimes.length;indT++)cumulator+=(double)((realP[indT]-tabData[indT]) * (realP[indT]-tabData[indT]));

		//Case T2 relaxation
		if(tabData[0]>tabData[tabData.length-1])cumulator=(double)(Math.sqrt(cumulator/tabTimes.length)*100.0/(tabData[0]));
		else cumulator=(double)(Math.sqrt(cumulator/tabTimes.length)*100.0/(tabData[tabData.length-1]));//Case T1 recovery
		return cumulator;
	}	



	public double[]makeFit(double[]tabTimes, double[]tabData,int fitType,int algType,int nbIter,double sigma){
		double[]estimatedParams;
		if(algType==TWOPOINTS){
			TwoPointsCurveFitterNoBias twopointsfitter=new TwoPointsCurveFitterNoBias(tabTimes, tabData,fitType, sigma);
			twopointsfitter.doFit();
			estimatedParams=twopointsfitter.getParams();
		}
		else if(algType==LM){
			LMCurveFitterNoBias lmfitter=new LMCurveFitterNoBias(tabTimes,tabData,fitType,sigma);
		 	lmfitter.configLMA(LMCurveFitterNoBias.lambda,LMCurveFitterNoBias.minDeltaChi2,nbIter);
		 	lmfitter.doFit();
			estimatedParams=lmfitter.getParams();
		}
		else {
			SimplexCurveFitterNoBias simpfitter=new SimplexCurveFitterNoBias(tabTimes,tabData,fitType,sigma);
		 	simpfitter.config(nbIter);
		 	simpfitter.doFit();
			estimatedParams=simpfitter.getParams();
		}
		return estimatedParams;
	}


	public static double dou(double d){
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}
	

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
		
		
	
	
	
}
