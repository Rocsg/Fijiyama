package com.vitimage;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.plugin.frame.RoiManager;

public class MRUtils implements Fit{
	private boolean fixedPlotScale=true;
	private static final double maxPlotY=10000;
	private static final double maxT1=6000;
	private static final double maxT2=200;
	private Plot plotT1=null;
	private Plot plotT2=null;
	private RoiManager rm;
	
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

	
	
	public void runCurveExplorer(){
		startPlotsAndRoi();
		double xCor=1,yCor=1;
		int count=1;
		while((rm.getCount()>=1)){//Boucle principale
			try {java.util.concurrent.TimeUnit.MILLISECONDS.sleep(100);} catch(java.lang.InterruptedException  ie){}
			count=rm.getCount();
			ImagePlus img;
			img=IJ.getImage();
			if((img != null) && (! img.getTitle().equals("T1 curve explorer") ) &&  (! img.getTitle().equals("T2 curve explorer") ) && (count > 1)){
			 	rm.selectAndMakeVisible(img, 1);
				xCor=(double)rm.getRoi(count-1).getXBase();
				yCor=(double)rm.getRoi(count-1).getYBase();
				rm.reset();
				PointRoi pr=new PointRoi(xCor,yCor,"large yellow hybrid");
				img.setRoi(pr);
				rm.add(img,pr,0);
				IJ.log("---------------------------------");
				if(img.getStack().getSize()<6){
					IJ.log("T1  : Nouveau point d etude : "+xCor+","+yCor);
					actualizePlotsT1(img,xCor,yCor);
					plotT1.updateImage();
				}
				else {
					IJ.log("T2  : Nouveau point d etude : "+xCor+","+yCor);
					actualizePlotsT2(img,xCor,yCor);
					plotT2.updateImage();
					plotT2.show();
				}
			}
		}
		closePlotsAndRoi();
	}


	
	public void startPlotsAndRoi(){
		plotT1 = new Plot("T1 curve explorer","Tr","Value");
		plotT2 = new Plot("T2 curve explorer","Te","Value");
		int maxCurves=10;
		for(int i =0;i<maxCurves;i++){
			plotT1.addPoints(new double[]{0,1},new double[]{0,0},Plot.LINE);
			plotT2.addPoints(new double[]{0,1},new double[]{0,0},Plot.LINE);
		}

		
		plotT1.setLimits(0, maxT1, 0, maxPlotY);plotT1.setSize(650,650);plotT1.show();
		plotT2.setLimits(0, maxT2, 0, maxPlotY);plotT2.setSize(650,650);plotT2.show();			
		ImagePlus img;
		rm = RoiManager.getInstance();
		if(rm == null) rm = new RoiManager();
		PointRoi pr=new PointRoi(1,1,"large yellow hybrid");
		rm.addRoi(pr);
	 	img=IJ.getImage();
		if(img != null)rm.selectAndMakeVisible(img, 0); 
	}


	public void closePlotsAndRoi(){
		java.awt.Window win;
		win=WindowManager.getWindow("T1 curve explorer");
		if(win != null){
			IJ.selectWindow("T1 curve explorer");
			WindowManager.getCurrentWindow().close();
		}
		win=WindowManager.getWindow("T2 curve explorer");
		if(win != null){
			IJ.selectWindow("T2 curve explorer");
			WindowManager.getCurrentWindow().close();
		}
		RoiManager rm = RoiManager.getInstance();
		if(rm != null) rm.close();		
		IJ.log("Fin de l'exploration");
	}


	public void actualizePlotsT2(ImagePlus imgIn,double xCor, double yCor){
		Color []tabColor=new Color[]{new Color(150,0,255),new Color(0 ,150 ,255 ) ,new Color(255,0 ,0 ), new Color(255,160 ,160), new Color(0,200,0) , new Color(160,200,160) };
		int  []tabFit=new int[]{T2_RELAX,T2_RELAX_RICE,T2_RELAX,MULTICOMP,MULTICOMP_RICE};
		int []tabAlg=new int[]{SIMPLEX,SIMPLEX,TWOPOINTS,SIMPLEX,SIMPLEX};
		int []yPos=new int[]{10,9,8,6,5,3,2};
		double[]tabData=getDataForVoxel(imgIn,(int)xCor,(int)yCor);
		double[]tabFittenCurve=new double[tabData.length];
 		double te=11;
 		int sigma=159;int fitType,algType;boolean multiComp=false;
		double longT2=tabData[0]/50;double max=0;
		int indEstMax=2;
 		int indEstMin=0;
 		int nEstim=indEstMax-indEstMin+1;
 		double []estimatedParams;
		int nbEchos=tabData.length;
		String[] tabNames=new String[]{"Fit exp","Fit Rice", "2 Points", "Fit Biex", "BiexRice"};
		double xMinPlot=0,xMaxPlot=300,xStep=5; 
 		double []tabTimes=getProportionalTimes(te,te*tabData.length,te);
		double []tabPlotX=getProportionalTimes(xMinPlot,xMaxPlot,xStep);
		int nxPlot=(int)(Math.ceil((xMaxPlot-xMinPlot)/xStep));
		double[][]tabPlotY=new double[nEstim][nxPlot];
		double valMax=0;
		int incr=0;

        plotT2.setLineWidth(4);
		plotT2.replace(incr++,"x",tabTimes,tabData);
		plotT2.replace(incr++, "line",new double[]{0,0},new double[]{0,0});
        plotT2.setLineWidth(2);

 		for(int indEst=indEstMin;indEst<=indEstMax;indEst++){
			fitType=tabFit[indEst];algType=tabAlg[indEst];
			estimatedParams=makeFit(tabTimes, tabData,fitType,algType,100,sigma);
			tabPlotY[indEst-indEstMin]=fittenRelaxationCurve(tabPlotX,estimatedParams,sigma,fitType);
			if(indEst==indEstMin)valMax=estimatedParams[0];
	        plotT2.setColor(tabColor[indEst]);
			plotT2.replace(incr++, "line",tabPlotX,tabPlotY[indEst-indEstMin]);
			plotT2.replace(incr++, "line",new double[]{0,(double)(estimatedParams[1])},new double[]{yPos[indEst]*longT2,yPos[indEst]*longT2});
			if(indEst>=5)plotT2.replace(incr++, "line",new double[]{0,(double)(estimatedParams[3]>100 ? 100 : estimatedParams[3])},
													new double[]{ (double)((yPos[indEst]+0.3)*longT2),(double)( (yPos[indEst]+0.3)*longT2) });

			if(indEst<5)IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+dou(estimatedParams[0])+" | T2="+dou(estimatedParams[1])+" ms | Erreur="+dou(fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
			else IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+dou(estimatedParams[0])+" | T2="+dou(estimatedParams[1])+" ms | M02="+dou(estimatedParams[2])+" | T22="+dou(estimatedParams[3])+" ms | Erreur="+dou(fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
 		}
 					
		
		String strLegend="MRI spin-echo observations Vs fits methods";
		for(int i=indEstMin;i<=indEstMax;i++)strLegend+=""+(i<6 ?"\n\n" : "\n\n\n")+""+(i-indEstMin)+"-"+tabNames[i];
		if(!fixedPlotScale) plotT2.setLimits(0, 200, 0,valMax*(double)1.3);
		plotT2.setColor(new Color(150,150 ,150) );
		plotT2.addLegend(strLegend);
	}


	public void actualizePlotsT1(ImagePlus imgIn,double xCor, double yCor){
		Color []tabColor=new Color[]{new Color(150 ,0 ,255 ),new Color(0,150,255) ,new Color(255,0 ,0 ),new Color(150 ,0 ,255 ),new Color(0,200,0)  };
		int  []tabFit=new int[]{T1_RECOVERY,T1_RECOVERY_RICE,T1_RECOVERY,T1_RECOVERY_RICE_NORMALIZED};
		int []tabAlg=new int[]{SIMPLEX,SIMPLEX,TWOPOINTS,SIMPLEX};
		int []yPos=new int[]{8,6,4,2};
 		double te=(double)11.5;int sigma=159;int fitType,algType;
		double[]tabData=getDataForVoxel(imgIn,(int)xCor,(int)yCor);
		double longT2=tabData[tabData.length-1]/50;double max=0;
		boolean normalized=(tabData[1]+tabData[0])<2;
		int nEstim=normalized?4:3;
		if(normalized)IJ.log("Donnees normalisees");
		double[]tabFittenCurve=new double[tabData.length];
 		double []estimatedParams;
		int nbEchos=tabData.length;
		String[] tabNames=new String[]{"-Fit only","-Fit Rice", "-2 Points", "-Rice norm"};
		double xMinPlot=0,xMaxPlot=6000,xStep=50;
 		double []tabTimes=getT1Times(tabData.length);
 		double []tabPlotX=getProportionalTimes(xMinPlot,xMaxPlot,xStep);
		int nxPlot=(int)(Math.ceil((xMaxPlot-xMinPlot)/xStep));
		double[][]tabPlotY=new double[nEstim][nxPlot];
		double valTemp,valMax=0;
		double[]tabMax=new double[nEstim];
		int incr=0;
	
		plotT1.setLineWidth(4);
		plotT1.replace(incr++,"x",tabTimes,tabData);
		plotT1.replace(incr++, "line",new double[]{0,0},new double[]{0,0});
        plotT1.setLineWidth(2);

 		for(int indEst=0;indEst<nEstim;indEst++){
			fitType=tabFit[indEst];algType=tabAlg[indEst];
			estimatedParams=makeFit(tabTimes, tabData,fitType,algType,100,sigma);
			tabPlotY[indEst]=fittenRelaxationCurve(tabPlotX,estimatedParams,sigma,fitType);
			tabMax[indEst]=estimatedParams[0];
			if(indEst==0)valMax=estimatedParams[0];
	        plotT1.setColor(tabColor[indEst]);
			plotT1.replace(incr++, "line",tabPlotX,tabPlotY[indEst]);
			plotT1.replace(incr++, "line",new double[]{0,(double)(estimatedParams[1])},
									   new double[]{estimatedParams[0]+(double)(20*indEst),estimatedParams[0]+(double)(20*indEst)});
			

			IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+dou(estimatedParams[0])+" | T1="+dou(estimatedParams[1])+" ms | Erreur="+dou(fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
 		}

		String strLegend="Observations Vs fits methods";
        if(!fixedPlotScale)plotT1.setLimits(xMinPlot, xMaxPlot, 0,valMax*(double)1.3);
        plotT1.setColor(new Color(150,150 ,150) );
		for(int i=0;i<nEstim;i++)strLegend+="\n\n"+i+tabNames[i];
		plotT1.addLegend(strLegend);
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
