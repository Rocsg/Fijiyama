package com.vitimage;


import java.util.Random;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import ij.IJ;
import ij.ImagePlus;
public class MRUtils  {
	public static final int RICE=100;
	public static final int BIAS=1000;
	public static final int SIGMA=10000;
	public static final int MULTI=100000;
    public static final int ALL_AVAILABLE_FIT=1000000;
	public static final int TRI=100000000;

	public static final int STRAIGHT_LINE=0,EXPONENTIAL=STRAIGHT_LINE+4,EXP_RECOVERY=STRAIGHT_LINE+13;
	public static final int T2_RELAX = EXP_RECOVERY +3; //offset 3
    public static final int T2_RELAX_BIAS = T2_RELAX+BIAS; //offset 3
    public static final int T2_RELAX_SIGMA = T2_RELAX+SIGMA; //offset 3
    public static final int T2_RELAX_RICE = T2_RELAX+RICE; //offset 3
	public static final int MULTICOMP=T2_RELAX+MULTI;
    public static final int MULTICOMP_BIAS=T2_RELAX+MULTI+BIAS;
    public static final int MULTICOMP_SIGMA=T2_RELAX+MULTI+SIGMA;
    public static final int MULTICOMP_RICE=T2_RELAX+MULTI+RICE;
    public static final int TRICOMP_RICE=T2_RELAX+TRI+RICE;
 	public static final int T1_RECOVERY = 500; //offset 3
	public static final int T1_RECOVERY_RICE = 501; //offset 3
	public static final int T1_RECOVERY_RICE_NORMALIZED = 504; //offset 3
	public static final int GAUSSIAN=17;
	public static final int ERROR_VALUE= 0;
	public static final int SIMPLEX = 1;
    public static final int LM=2;
    public static final int TWOPOINTS=3; 	   
    public static final int MSEC=3;
    public static final int SEC=0;

    public final static String[] timeunits={"ms", "s"};
    public final static int[] timeitems={MSEC, SEC};
    public final static String[] fititems2={"Simplex","Levenberg-Marquardt"};
    public final static int[] constitems2={SIMPLEX,LM};

	
	static float stdValMaxIRM=50000;
	static float stdSigmaIRM=159;
	
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
				case MRUtils.T2_RELAX: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1])));break;
				case MRUtils.T2_RELAX_SIGMA: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1])));break;
			 	case MRUtils.T2_RELAX_BIAS: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]);break;
				case MRUtils.T2_RELAX_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)Math.exp(-(techo / param[1])),sigma));break;

				case MRUtils.T1_RECOVERY: tab[indT]=(double)(param[0]* (double)(1-Math.exp(-(techo / param[1]))));break;
				case MRUtils.T1_RECOVERY_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)(1-Math.exp(-(techo / param[1]))) , sigma));break;
			 	case MRUtils.T1_RECOVERY_RICE_NORMALIZED: tab[indT]=(double)(RiceEstimator.besFunkCost( (double)(1-Math.exp(-(techo / param[0]))) , sigma));break;

				case MRUtils.MULTICOMP: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])));break;
				case MRUtils.MULTICOMP_BIAS: tab[indT]=(double)(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3]))+param[4]);break;
				case MRUtils.MULTICOMP_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])),sigma));break;
				case MRUtils.TRICOMP_RICE: tab[indT]=(double)(RiceEstimator.besFunkCost(param[0]* (double)Math.exp(-(techo / param[1]))+param[2]* (double)Math.exp(-(techo / param[3])+param[4]* (double)Math.exp(-(techo / param[5]))),sigma) );break;
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
		ChiSquaredDistribution x2 = new ChiSquaredDistribution( freedomDegrees );
		try {
			return(x2.cumulativeProbability(khi2));
		} catch (Exception e) {
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
		if(algType==MRUtils.TWOPOINTS){
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

	public static double[]makeFitSimple(double[]tabTimes, double[]tabData,int fitType,int algType,int nbIter,double sigma){
		Random rand=new Random();
		int nParams=(fitType==MRUtils.T2_RELAX_RICE || fitType==MRUtils.T1_RECOVERY_RICE || fitType==MRUtils.TWOPOINTS) ? 2 : (fitType==MRUtils.TRICOMP_RICE ) ? 6 : 4;
		double[]tmpSimulatedData=new double[tabData.length];
		double[]estimatedParams;
		double[]simEstimatedParams;
		double[]estimatedSigmas=new double[nParams];
		double[]estimatedMeans=new double[nParams];
		double sigmaZero;
		if(algType==MRUtils.TWOPOINTS){
			TwoPointsCurveFitterNoBias twopointsfitter=new TwoPointsCurveFitterNoBias(tabTimes, tabData,fitType, sigma);
			twopointsfitter.doFit();
			estimatedParams=twopointsfitter.getParams();
		}
		else if(algType==LM){
			LMCurveFitterNoBias lmfitter=new LMCurveFitterNoBias(tabTimes,tabData,fitType,sigma,false);
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
		if(false)System.out.println("Verite terrain : "+TransformUtils.stringVectorN(estimatedParams, ""));
		if(false)System.out.println("Means  terrain : "+TransformUtils.stringVectorN(estimatedMeans, ""));
		if(false)System.out.println("Deviation std  : "+TransformUtils.stringVectorN(estimatedSigmas, ""));
		double [][]ret=new double[2][nParams];
		return estimatedParams;

	}


	public static double dou(double d){
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}
	

	
	
}
