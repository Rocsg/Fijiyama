package com.vitimage;

import java.awt.Rectangle;
import java.util.Random;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;

public class MRUtils implements Fit{
	
	public MRUtils() {
		
	}
	
	public static double []getT1Times(int nb){
		return (  (nb==3) ? (new double[]{600,1200,2400}) : (new double[]{600,1200,2400,10000}));
	}

	public static double []getProportionalTimes(double valMin, double valMax,double step){
		double[]tab=new double[(int)(Math.ceil((double)0.00001+((valMax-valMin)/step)))];
		for (int i=0;i<tab.length;i++){
			tab[i]=valMin+(double)((i*step));
		}
		return tab;
	}
	
	public static int getCorrespondingSliceInHyperImageMR(int curT,int totalT,int curZ,int totalZ, int curEc, int totalEchoes) {
		return totalEchoes*totalZ*curT+totalEchoes*curZ+curEc;		
	}
	
	
	public static double[]getDataForVoxel(ImagePlus imgIn,int xCor,int yCor,int curT,int totalT,int curZ,int totalZ,int nEchoesExpected,int crossWidth,int crossThick,boolean gaussianWeighting){
		if(gaussianWeighting) {IJ.showMessage("Not applicable : gaussian weighting. Abort");gaussianWeighting=false;}
		int xMax=imgIn.getWidth();
		int yMax=imgIn.getHeight();
		int zMax=imgIn.getNSlices();
		int zCor=curZ;
		int xm,ym,xM,yM,zm,zM;
		xm=xCor-crossWidth;
		xM=xCor+crossWidth;
		ym=yCor-crossWidth;
		yM=yCor+crossWidth;
		zm=curZ-crossThick;
		zM=curZ+crossThick;
		xm=Math.max(xm, 0);
		xM=Math.min(xMax-1, xM);
		ym=Math.max(ym, 0);
		yM=Math.min(yMax-1, yM);
		zm=Math.max(zm, 0);
		zM=Math.min(zMax-1, zM);
		double[]data= new double[nEchoesExpected];
		int nHits=(xM-xm+1)*(yM-ym+1)*(zM-zm+1);
		if( (xCor>xMax-1) || (yCor>yMax-1)) {IJ.log("Bad coordinates. Data set to 0"); return data;}
		double[][][]weights=new double[xM-xm+1][yM-ym+1][zM-zm+1];
		double sigmaX=crossWidth;
		double sigmaY=crossWidth;
		double sigmaZ=crossThick;
		
		
		double sum=0;
		for(int x=xm;x<=xM;x++) {
			for(int y=ym;y<=yM;y++) {
				for(int z=zm;z<=zM;z++) {	
					if(!gaussianWeighting)weights[x-xm][y-ym][z-zm]=1.0/nHits;
					else {
						if(sigmaX<=0 && sigmaZ<=0) {//One point
							weights[x-xm][y-ym][z-zm]=1;
						}
						else if(sigmaX<=0 && sigmaZ>0) {//Mono dimensional along z
							weights[x-xm][y-ym][z-zm]=1/(Math.pow(2*Math.PI,0.5)*sigmaZ) * Math.exp(- (z-zCor)*(z-zCor)/(sigmaZ*sigmaZ));
						}
						else if(sigmaX>0 && sigmaZ<=0) {//Two dimensional along x and y
							weights[x-xm][y-ym][z-zm]=1/(Math.pow(2*Math.PI,1)*sigmaX*sigmaY) * Math.exp(-  (x-xCor)*(x-xCor)/(sigmaX*sigmaX)  -  (y-yCor)*(y-yCor)/(sigmaY*sigmaY));
						}
						else {//Three dimensional gaussian
							weights[x-xm][y-ym][z-zm]=1/(Math.pow(2*Math.PI,1.5)*sigmaX*sigmaY*sigmaZ) * Math.exp(-  (x-xCor)*(x-xCor)/(sigmaX*sigmaX)  -  (y-yCor)*(y-yCor)/(sigmaY*sigmaY)  -  (z-zCor)*(z-zCor)/(sigmaZ*sigmaZ));
						}
					}
					sum+=weights[x-xm][y-ym][z-zm];
				}
			}
		}
		if(gaussianWeighting) {
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					for(int z=zm;z<=zM;z++) {	
						weights[x-xm][y-ym][z-zm]/=sum;
					}
				}
			}
		}
		for(int ec=0;ec<nEchoesExpected;ec++) {			
			for(int z=zm;z<=zM;z++) {
				int indexZ=nEchoesExpected*totalZ*curT+nEchoesExpected*z+1+ec;
				for(int x=xm;x<=xM;x++) {
					for(int y=ym;y<=yM;y++) {
						data[ec]+=weights[x-xm][y-ym][z-zm]*(double)((float[])(imgIn.getStack().getProcessor(indexZ).getPixels()))[x + xMax * y];
					}
				}
			}
		}
		return data;
		
	}

	
	
	public static double[][]getFullDataForVoxel(ImagePlus imgIn,int xCor,int yCor,int curT,int totalT,int curZ,int totalZ,int nEchoesExpected,int crossWidth,int crossThick,boolean gaussianWeighting){
		if(gaussianWeighting) {IJ.showMessage("Not applicable : gaussian weighting. Abort");gaussianWeighting=false;}
		int xMax=imgIn.getWidth();
		int yMax=imgIn.getHeight();
		int zMax=imgIn.getNSlices();
		int zCor=curZ;
		int xm,ym,xM,yM,zm,zM;
		xm=xCor-crossWidth;
		xM=xCor+crossWidth;
		ym=yCor-crossWidth;
		yM=yCor+crossWidth;
		zm=curZ-crossThick;
		zM=curZ+crossThick;
		xm=Math.max(xm, 0);
		xM=Math.min(xMax-1, xM);
		ym=Math.max(ym, 0);
		yM=Math.min(yMax-1, yM);
		zm=Math.max(zm, 0);
		zM=Math.min(zMax-1, zM);
		int nHits=(xM-xm+1)*(yM-ym+1)*(zM-zm+1);
		int incr=0;
		double[][]data= new double[nEchoesExpected][nHits];
		if( (xCor>xMax-1) || (yCor>yMax-1)) {IJ.log("Bad coordinates. Data set to 0"); return null;}
		for(int z=zm;z<=zM;z++) {
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					for(int ec=0;ec<nEchoesExpected;ec++) {			
						int indexZ=nEchoesExpected*totalZ*curT+nEchoesExpected*z+1+ec;
						//System.out.println("Do "+x+" , "+y+" , "+z+" , "+ec+" with borders="+xm+"-"+xM+"  "+ym+"-"+yM+"  "+zm+"-"+zM+"  0-"+(nEchoesExpected-1)+" with data.size="+data.length);
						data[ec][incr]=(double)((float[])(imgIn.getStack().getProcessor(indexZ).getPixels()))[x + xMax * y];
					}
					incr++;
				}
			}
		}
		return data;
	}

	
	
	public static int[][]getFullCoordsForVoxel(ImagePlus imgIn,int xCor,int yCor,int curT,int totalT,int curZ,int totalZ,int nEchoesExpected,int crossWidth,int crossThick,boolean gaussianWeighting){
		int xMax=imgIn.getWidth();
		int yMax=imgIn.getHeight();
		int zMax=imgIn.getNSlices();
		int zCor=curZ;
		int xm,ym,xM,yM,zm,zM;
		xm=xCor-crossWidth;
		xM=xCor+crossWidth;
		ym=yCor-crossWidth;
		yM=yCor+crossWidth;
		zm=curZ-crossThick;
		zM=curZ+crossThick;
		xm=Math.max(xm, 0);
		xM=Math.min(xMax-1, xM);
		ym=Math.max(ym, 0);
		yM=Math.min(yMax-1, yM);
		zm=Math.max(zm, 0);
		zM=Math.min(zMax-1, zM);
		int nHits=(xM-xm+1)*(yM-ym+1)*(zM-zm+1);
		int incr=0;
		int[][]data= new int[nHits][8];
		if( (xCor>xMax-1) || (yCor>yMax-1)) {IJ.log("Bad coordinates. Data set to 0"); return null;}
		for(int z=zm;z<=zM;z++) {
			for(int x=xm;x<=xM;x++) {
				for(int y=ym;y<=yM;y++) {
					for(int ec=0;ec<nEchoesExpected;ec++) {			
						int indexZ=nEchoesExpected*totalZ*curT+nEchoesExpected*z+1+ec;
						//System.out.println("Do "+x+" , "+y+" , "+z+" , "+ec+" with borders="+xm+"-"+xM+"  "+ym+"-"+yM+"  "+zm+"-"+zM+"  0-"+(nEchoesExpected-1)+" with data.size="+data.length);
						data[incr]=new int[] {x,y,0,0,0,0,0,0};
					}
					incr++;
				}
			}
		}
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
				case T2_RELAX_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)Math.exp(-(techo / param[1])),sigma));break;

				case T1_RECOVERY: tab[indT]=(double)(param[0]* (double)(1-Math.exp(-(techo / param[1]))));break;
				case T1_RECOVERY_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)(1-Math.exp(-(techo / param[1]))) , sigma));break;
			 	case T1_RECOVERY_RICE_NORMALIZED: tab[indT]=(double)(RiceEstimator.besFunkCost( (double)(1-Math.exp(-(techo / param[0]))) , sigma));break;

				case MULTICOMP: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])));break;
				case MULTICOMP_BIAS: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3]))+param[4]);break;
				case MULTICOMP_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])),sigma));break;
				case TRICOMP_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])+param[4]* (double)Math.exp(-(techo / param[5]))),sigma) );break;
			}
		}
		return tab;
	}


	public static double fittingAccuracy(double[] tabData,double[] tabTimes,double sigma,double[] estimatedParams,int fitType,boolean debug){		
		double []realP=fittenRelaxationCurve(tabTimes,estimatedParams,sigma,fitType);
		double cumulator=0;
		double meanBg=RiceEstimator.computeSigmaAndMeanBgFromRiceSigmaStatic(sigma)[0];
		for(int indT=0;indT<tabTimes.length;indT++)cumulator+=(double)((realP[indT]-tabData[indT]) * (realP[indT]-tabData[indT]));

		//Case T2 relaxation
		double valRef=0;
		for(int t=0;t<tabData.length;t++)valRef+=(1.0/tabData.length)*tabData[t];
		if(debug)System.out.println("is"+tabData.length);
		if(debug)System.out.println("Cumulatorinit="+cumulator);
		if(debug)System.out.println("valref="+valRef);
		if(debug)System.out.println("meanbg="+meanBg);
		cumulator=Math.sqrt(cumulator/tabTimes.length);
		if(debug)System.out.println("RMSE="+cumulator);
		cumulator=cumulator*100.0/((Math.abs(valRef-meanBg)));
		if(debug)System.out.println("Cumulator final="+cumulator);
		if(cumulator>99)cumulator=99;
		return cumulator;
	}	

	public static double[] fittingAccuraciesFullData(double[][] tabData,double[] tabTimes,double sigma,double[] estimatedParams,int fitType,boolean debug,RiceEstimator riceEstimator){		
		double []realP=fittenRelaxationCurve(tabTimes,estimatedParams,sigma,fitType);
		double khi2=0;
		double diff=0;
		double sigmaEst=0;
		for(int indT=0;indT<tabData.length;indT++) {
			sigmaEst=riceEstimator.estimateSigma(sigma, realP[indT]);
			for(int indP=0;indP<tabData[indT].length;indP++) {
				diff=(realP[indT]-tabData[indT][indP]);
				khi2+=diff*diff/(sigmaEst*sigmaEst);
			}
		}
		int N_PARAMS=( (fitType==MRUtils.T1_RECOVERY_RICE || fitType==MRUtils.T2_RELAX_RICE) ? 2 : (fitType==MRUtils.TRICOMP_RICE) ? 6 : 4);
		double pVal=getPvalue(khi2,tabTimes.length*tabData[0].length-N_PARAMS);
		double khi2Ret=khi2/(tabTimes.length*tabData[0].length-N_PARAMS);
 		return new double[] {khi2Ret,pVal*100};
	}	

	

	public static double getPvalue(double khi2,int freedomDegrees){
		ChiSquaredDistributionImpl x2 = new ChiSquaredDistributionImpl( freedomDegrees );
		try {
			return(x2.cumulativeProbability(khi2));
		} catch (MathException e) {
			e.printStackTrace();
		}
		return -1;
	}


	

	public static double[] fittingAccuracies(double[] tabData,double[] tabTimes,double sigma,double[] estimatedParams,int fitType,boolean debug,RiceEstimator riceEstimator,int nbPoints){		
		double []realP=fittenRelaxationCurve(tabTimes,estimatedParams,sigma,fitType);
		double khi2=0;
		double diff=0;
		double sigmaEst=0;
		for(int indT=0;indT<tabData.length;indT++) {
			sigmaEst=riceEstimator.estimateSigma(sigma, realP[indT])/Math.sqrt(nbPoints);
			diff=(realP[indT]-tabData[indT]);
			khi2+=diff*diff/(sigmaEst*sigmaEst);
		}
		int N_PARAMS=( (fitType==MRUtils.T1_RECOVERY_RICE || fitType==MRUtils.T2_RELAX_RICE) ? 2 : (fitType==MRUtils.TRICOMP_RICE) ? 6 : 4);
		double pVal=getPvalue(khi2,tabTimes.length-N_PARAMS);
		double khi2Ret=khi2/(tabTimes.length-N_PARAMS);
 		return new double[] {khi2Ret,pVal*100};
	}	

	public static double[][]makeFit(double[]tabTimes, double[]tabData,int fitType,int algType,int nbIter,double sigma,int nRepetMonteCarlo,int nAveragePts,boolean debugLM,RiceEstimator riceEstimator){
		Random rand=new Random();
		int nParams=(fitType==MRUtils.T2_RELAX_RICE || fitType==MRUtils.T1_RECOVERY_RICE || fitType==MRUtils.TWOPOINTS) ? 2 : (fitType==MRUtils.TRICOMP_RICE ) ? 6 : 4;
		double[]tmpSimulatedData=new double[tabData.length];
		double[][]resultsMonteCarlo=new double[nParams][nRepetMonteCarlo];
		double[]estimatedParams;
		double[]simEstimatedParams;
		double[]estimatedSigmas=new double[nParams];
		double[]estimatedMeans=new double[nParams];
		double sigmaZero;
		if(algType==TWOPOINTS){
			TwoPointsCurveFitterNoBias twopointsfitter=new TwoPointsCurveFitterNoBias(tabTimes, tabData,fitType, sigma);
			twopointsfitter.doFit();
			estimatedParams=twopointsfitter.getParams();
			for(int n=0;n<nRepetMonteCarlo;n++) {
				for(int dat=0;dat<tabData.length;dat++) {
					sigmaZero=riceEstimator.estimateSigma(sigma,tabData[dat])/Math.sqrt(nAveragePts); 
					tmpSimulatedData[dat]=tabData[dat]+rand.nextGaussian()*sigmaZero;
				}
				twopointsfitter=new TwoPointsCurveFitterNoBias(tabTimes, tmpSimulatedData,fitType, sigma);
				twopointsfitter.doFit();
				simEstimatedParams=twopointsfitter.getParams();
				if(false)System.out.println("\nsimulation "+n+" sig="+VitimageUtils.dou(sigmaZero)+" : data= "+TransformUtils.stringVectorN(tmpSimulatedData, ""));
				if(false)System.out.println("    data init        : data= "+TransformUtils.stringVectorN(tabData, ""));
				if(false)System.out.println("   params opt : "+TransformUtils.stringVectorN(estimatedParams, ""));
				if(false)System.out.println("    mcparams  : "+TransformUtils.stringVectorN(simEstimatedParams, ""));
				for(int p=0;p<nParams;p++)resultsMonteCarlo[p][n]=simEstimatedParams[p];
			}
		}
		else if(algType==LM){
			LMCurveFitterNoBias lmfitter=new LMCurveFitterNoBias(tabTimes,tabData,fitType,sigma,debugLM);
		 	lmfitter.configLMA(LMCurveFitterNoBias.lambda,LMCurveFitterNoBias.minDeltaChi2,nbIter);
		 	lmfitter.doFit();
			estimatedParams=lmfitter.getParams();
			for(int n=0;n<nRepetMonteCarlo;n++) {
				for(int dat=0;dat<tabData.length;dat++) {
					sigmaZero=riceEstimator.estimateSigma(sigma,tabData[dat])/Math.sqrt(nAveragePts); 
					tmpSimulatedData[dat]=tabData[dat]+rand.nextGaussian()*sigmaZero;
				}
				lmfitter=new LMCurveFitterNoBias(tabTimes,tmpSimulatedData,fitType,sigma,debugLM);
				lmfitter.doFit();
				simEstimatedParams=lmfitter.getParams();
				if(false)System.out.println("\nsimulation "+n+" sig="+VitimageUtils.dou(sigmaZero)+" : data= "+TransformUtils.stringVectorN(tmpSimulatedData, ""));
				if(false)System.out.println("    data init        : data= "+TransformUtils.stringVectorN(tabData, ""));
				if(false)System.out.println("   params opt : "+TransformUtils.stringVectorN(estimatedParams, ""));
				if(false)System.out.println("    mcparams  : "+TransformUtils.stringVectorN(simEstimatedParams, ""));
				for(int p=0;p<nParams;p++)resultsMonteCarlo[p][n]=simEstimatedParams[p];
			}
		}
		else {
			SimplexCurveFitterNoBias simpfitter=new SimplexCurveFitterNoBias(tabTimes,tabData,fitType,sigma);
		 	simpfitter.config(nbIter);
		 	simpfitter.doFit();
			estimatedParams=simpfitter.getParams();
			for(int n=0;n<nRepetMonteCarlo;n++) {
				for(int dat=0;dat<tabData.length;dat++) {
					sigmaZero=riceEstimator.estimateSigma(sigma,tabData[dat])/Math.sqrt(nAveragePts); 
					tmpSimulatedData[dat]=tabData[dat]+rand.nextGaussian()*sigmaZero;
//					System.out.println("SIGMA="+sigma+"   SIGMAZER="+sigmaZero+"  nAveragePts="+nAveragePts);
				}
				simpfitter=new SimplexCurveFitterNoBias(tabTimes,tmpSimulatedData,fitType,sigma);
				simpfitter.doFit();
				simEstimatedParams=simpfitter.getParams();
				if(false)System.out.println("\nsimulation "+n+" sig="+VitimageUtils.dou(sigmaZero)+" : data= "+TransformUtils.stringVectorN(tmpSimulatedData, ""));
				if(false)System.out.println("    data init        : data= "+TransformUtils.stringVectorN(tabData, ""));
				if(false)System.out.println("   params opt : "+TransformUtils.stringVectorN(estimatedParams, ""));
				if(false)System.out.println("    mcparams  : "+TransformUtils.stringVectorN(simEstimatedParams, ""));
				for(int p=0;p<nParams;p++)resultsMonteCarlo[p][n]=simEstimatedParams[p];
			}
		}
		for(int p=0;p<nParams;p++) {estimatedSigmas[p]=VitimageUtils.statistics1D(resultsMonteCarlo[p])[1];estimatedMeans[p]=VitimageUtils.statistics1D(resultsMonteCarlo[p])[0];}
		if(false)System.out.println("Verite terrain : "+TransformUtils.stringVectorN(estimatedParams, ""));
		if(false)System.out.println("Means  terrain : "+TransformUtils.stringVectorN(estimatedMeans, ""));
		if(false)System.out.println("Deviation std  : "+TransformUtils.stringVectorN(estimatedSigmas, ""));
		double [][]ret=new double[2][nParams];
		ret[0]=estimatedParams;
		ret[1]=estimatedSigmas;
		return ret;
	}


	public static double dou(double d){
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}
	

	
	
}
