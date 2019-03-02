package com.vitimage;


import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Scrollbar;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotCanvas;
import ij.gui.PointRoi;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;
import ij.plugin.frame.RoiManager;

/**
 * TODO
 * Bouton exit
 * Bouton toggle auto mode for plot
 * Bouton toggle fits mode
 * Bouton toggle LUT fire
 * 
 * @author fernandr
 *
 */


public class MRICurveExplorerWindow extends Frame implements KeyListener, MouseListener {// 
	Scrollbar scrollbarZ;
	int xMouse=0;
	int yMouse=0;
	int imgWidth,imgHeight;
	double xCoord=0;
	double yCoord=0;
	private Plot plotT1;
	private Plot plotT2;
	private ImageCanvas imgCan1;
	private ImageCanvas imgCan2;
	private PlotCanvas plotCan1;
	private PlotCanvas plotCan2;
	private boolean fixedPlotScale=false;
	private static final double maxPlotY= 10000;
	private double valMaxCourante=10000;
	private static final double maxT1=6000;
	private static final double maxT2=200;
	private static final int WIN_T1=1;
	private static final int WIN_T2=2;
	private static final int WIN_PLOT1=3;
	private static final int WIN_PLOT2=4;
	private static final int WIN_BORDER=5;
	private int currentCanvas=1;
	private double zoomLevel=0;
	int coordX;
	int coordY;
	boolean computeMultiComp=false;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int SCROLLBAR_VERTICAL_SIZE = 10;
	private static final int PLOT_HEIGHT = 450;
	private MRUtils mrUt;
	public MRICurveExplorerWindow(ImagePlus imgT1,ImagePlus imgT2) {
		super();
		imgWidth=imgT1.getWidth();
		imgHeight=imgT1.getHeight();
		this.imgCan1=new ImageCanvas(imgT1);
		this.imgCan2=new ImageCanvas(imgT2);
		mrUt=new MRUtils();
		startPlotsAndRoi();
		initializeGUI();
		//imgT1.hide();
		//imgT2.hide();
		ImageJ ij = IJ.getInstance();

        // Remove ImageJ as keylistener from this and all subcomponents.
        removeKeyListener(ij);
        imgCan1.removeKeyListener(ij);
        imgCan2.removeKeyListener(ij);
        plotCan1.removeKeyListener(ij);
        plotCan2.removeKeyListener(ij);

        removeMouseListener(ij);
        //imgCan1.removeMouseListener(ij);
        //imgCan2.removeMouseListener(ij);
        plotCan1.removeMouseListener(ij);
        plotCan2.removeMouseListener(ij);
        
        addKeyListener(this);
        imgCan1.addKeyListener(this);
        imgCan2.addKeyListener(this);
        plotCan1.addKeyListener(this);
        plotCan2.addKeyListener(this);

        addMouseListener(this);
        imgCan1.addMouseListener(this);
        imgCan2.addMouseListener(this);
        plotCan1.addMouseListener(this);
        plotCan2.addMouseListener(this);
        
        imgCan1.setMagnification(1);
        imgCan2.setMagnification(1);
        this.setResizable(false);
       //	runCurveExplorer();
	}
		
	public void setCanvasSizes() {
		imgCan1.setSize(imgWidth, imgHeight);
		imgCan2.setSize(imgWidth, imgHeight);
		plotCan1.setSize(imgWidth, PLOT_HEIGHT );
		plotCan2.setSize(imgWidth, PLOT_HEIGHT );
	}
	
	public void actualizeValMaxCourante(double valeurRef) {
		if(fixedPlotScale)valMaxCourante=maxPlotY;
		else valMaxCourante=(1000.0*(((int)Math.floor((valeurRef*1.4+800)/1000.0))+1));
	}
	
	public void repaintAll() {
		repaint();
		setCanvasSizes();
		imgCan1.repaint();
		imgCan2.repaint();
		plotCan1.repaint();
		plotCan2.repaint();
		repaint();
}
	
	public void initializeGUI() {
		setTitle("MRI Curve explorer");
		GridBagConstraints c;
		scrollbarZ=new Scrollbar(Scrollbar.HORIZONTAL, 10, 1, 0, 40);

		//Image T1
		setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		add(imgCan1,c);

		//Image T2
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		add(imgCan2,c);
		
		//ScrollBar
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 1;
		add(scrollbarZ,c);
		
		//PlotT1
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 2;
		add(plotCan1,c);

		//PlotT2
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 2;
		add(plotCan2,c);
		setCanvasSizes();
		setSize(1400,1040);
		this.pack();
		setCanvasSizes();
		setVisible(true);
		repaint();
	//	imgT1.hide();
	//	imgT2.hide();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		System.out.println("\nTouche "+e.getKeyChar());
		System.out.println("Fenetre active="+ currentCanvas);
		System.out.println("Coordonnees actives=("+ xMouse+","+yMouse+")");
		System.out.println("Niveau de zoom="+zoomLevel);
		System.out.println("Mag of T1 : "+imgCan1.getMagnification());
		
		if (e.getKeyChar()=='+') {
			System.out.println("Zoom in");
			if(currentCanvas==WIN_T1 || currentCanvas==WIN_T2 ) {
				imgCan1.zoomIn(xMouse,yMouse);
				imgCan2.zoomIn(xMouse,yMouse);
				zoomLevel=imgCan1.getMagnification();
			}
			if(currentCanvas==WIN_PLOT1 || currentCanvas==WIN_PLOT2) {
				plotCan1.zoomIn(xMouse,yMouse);
				plotCan2.zoomIn(xMouse,yMouse);
			}
		}
		if (e.getKeyChar()=='-') {
			System.out.println("Zoom out");
			if( (currentCanvas==WIN_T1 || currentCanvas==WIN_T2) ) {
				zoomLevel=1;
				imgCan1.setMagnification(imgCan1.getMagnification()/2.0);
				imgCan2.setMagnification(imgCan2.getMagnification()/2.0);
				imgCan1.zoomIn(xMouse,yMouse);
				imgCan2.zoomIn(xMouse,yMouse);
			}
			if(currentCanvas==WIN_PLOT1 || currentCanvas==WIN_PLOT2) {
				zoomLevel=1;
				plotCan1.setMagnification(zoomLevel);
				plotCan2.setMagnification(zoomLevel);
			}
		}
		setCanvasSizes();
		pack();
		repaint();
	}
	
	public void mouseClicked(MouseEvent e) {
		System.out.println("\nClick !");
		xMouse=e.getX();
		yMouse=e.getY();
		if(imgCan1.cursorOverImage()) currentCanvas=WIN_T1;
		if(imgCan2.cursorOverImage()) currentCanvas=WIN_T2;
		if(plotCan1.cursorOverImage()) currentCanvas=WIN_PLOT1;
		if(plotCan2.cursorOverImage()) currentCanvas=WIN_PLOT2;
		if(currentCanvas==WIN_T1 || currentCanvas==WIN_T2) {
			PointRoi prT1=new PointRoi(xMouse,yMouse,"large yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
		}
		
		if(currentCanvas==WIN_PLOT1 || currentCanvas==WIN_PLOT2 && xMouse>78 && xMouse<178 && yMouse>29 && yMouse<87 )computeMultiComp=!computeMultiComp;
		if(currentCanvas==WIN_PLOT1 || currentCanvas==WIN_PLOT2 && xMouse>38 && xMouse<578 && yMouse>29 && yMouse<87 )fixedPlotScale=!fixedPlotScale;
		repaint();
		plotCan1.repaint();
		imgCan1.repaint();
		plotCan2.repaint();
		imgCan2.repaint();
		System.out.println("Fenetre active="+ currentCanvas);
		System.out.println("Coordonnees actives=("+ xMouse+","+yMouse+")");
		System.out.println("Niveau de zoom="+zoomLevel);
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
	
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
		
	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	
	
/*	public void runCurveExplorer(){
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
*/
/*	public void locateTheCursor() {
		System.out.println("Demande de localisation");
		if(yMouse < imgHeight) {
			if(xMouse < imgWidth) {
				currentCanvas=WIN_T1;
				coordX=xMouse;coordY=yMouse;
			}
			if(xMouse >= imgWidth) {
				currentCanvas=WIN_T2;
				coordX=xMouse-imgWidth;coordY=yMouse;
			}
		}
		if(yMouse > imgHeight+SCROLLBAR_VERTICAL_SIZE) {
			if(xMouse < imgWidth) {
				currentCanvas=WIN_PLOT1;
				coordX=xMouse;coordY=yMouse-imgHeight-SCROLLBAR_VERTICAL_SIZE;
			}
			if(xMouse >= imgWidth) {
				currentCanvas=WIN_PLOT2;
				coordX=xMouse-imgWidth;coordY=yMouse-imgHeight-SCROLLBAR_VERTICAL_SIZE;
			}
		}
		System.out.println("  --> effectuee :currentWindow="+currentCanvas+" avec CoordMouse=("+xMouse+","+yMouse+")"+" et coord canvas=("+coordX+","+coordY+")");
		
	}
	*/
	
	public void startPlotsAndRoi(){
		plotT1 = new Plot("T1 curve explorer","Tr","Value");
		plotT2 = new Plot("T2 curve explorer","Te","Value");
		plotT1.setSize(imgWidth,PLOT_HEIGHT);
		plotT2.setSize(imgWidth,PLOT_HEIGHT);
		plotCan1=new PlotCanvas(plotT1.getImagePlus());
		plotCan2=new PlotCanvas(plotT2.getImagePlus());
		plotCan1.setPlot(plotT1);
		plotCan2.setPlot(plotT2);
		int maxCurves=20;
		for(int i =0;i<maxCurves;i++){
			plotT1.addPoints(new double[]{0,1},new double[]{0,0},Plot.LINE);
			plotT2.addPoints(new double[]{0,1},new double[]{0,0},Plot.LINE);
		}

		
		plotT1.setLimits(0, maxT1, 0, maxPlotY);
		plotT2.setLimits(0, maxT2, 0, maxPlotY);
//		ImagePlus img;
//	 	img=IJ.getImage();
//		if(img != null)rm.selectAndMakeVisible(img, 0); 
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
		
		Color []tabColor=new Color[]{new Color(150,0,255),new Color(0 ,150 ,255 ) ,new Color(255,0 ,0 ), new Color(0,50 ,0 ),new Color(255,160 ,160), new Color(0,200,0) , new Color(160,200,160) };
		int  []tabFit=new int[]{mrUt.T2_RELAX,mrUt.T2_RELAX_RICE,mrUt.T2_RELAX,mrUt.T2_RELAX_BIAS,mrUt.MULTICOMP,mrUt.MULTICOMP_RICE};
		int []tabAlg=new int[]{mrUt.SIMPLEX,mrUt.SIMPLEX,mrUt.TWOPOINTS,mrUt.SIMPLEX,mrUt.SIMPLEX,mrUt.SIMPLEX};
		int []yPos=new int[]{10,9,8,7,5,3,2};
		double[]tabData=mrUt.getDataForVoxel(imgIn,(int)xCor,(int)yCor);
		double[]tabFittenCurve=new double[tabData.length];
 		double te=11;
 		int sigma=159;int fitType,algType;boolean multiComp=false;
		double longT2=tabData[0]/50;double max=0;
		int indEstMax=(computeMultiComp ? 5 : 2);
 		int indEstMin=0;
 		int nEstim=indEstMax-indEstMin+1;
 		double []estimatedParams;
		int nbEchos=tabData.length;
		String[] tabNames=new String[]{"Fit exp","Fit Rice", "2 Points", "Fit  offset","Fit Biex", "BiexRice"};
		double xMinPlot=0,xMaxPlot=300,xStep=5; 
 		double []tabTimes=mrUt.getProportionalTimes(te,te*tabData.length,te);
		double []tabPlotX=mrUt.getProportionalTimes(xMinPlot,xMaxPlot,xStep);
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
			estimatedParams=mrUt.makeFit(tabTimes, tabData,fitType,algType,100,sigma);
			tabPlotY[indEst-indEstMin]=mrUt.fittenRelaxationCurve(tabPlotX,estimatedParams,sigma,fitType);
			if(indEst==indEstMin)valMax=estimatedParams[0];
	        plotT2.setColor(tabColor[indEst]);
			plotT2.replace(incr++, "line",tabPlotX,tabPlotY[indEst-indEstMin]);
			plotT2.replace(incr++, "line",new double[]{0,(double)(estimatedParams[1])},new double[]{yPos[indEst]*longT2,yPos[indEst]*longT2});
			if(indEst>=4)plotT2.replace(incr++, "line",new double[]{0,(double)(estimatedParams[3]>100 ? 100 : estimatedParams[3])},
													new double[]{ (double)((yPos[indEst]+0.3)*longT2),(double)( (yPos[indEst]+0.3)*longT2) });

			if(indEst<4)IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+mrUt.dou(estimatedParams[0])+" | T2="+mrUt.dou(estimatedParams[1])+" ms | Erreur="+mrUt.dou(mrUt.fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
			else IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+mrUt.dou(estimatedParams[0])+" | T2="+mrUt.dou(estimatedParams[1])+" ms | M02="+mrUt.dou(estimatedParams[2])+" | T22="+mrUt.dou(estimatedParams[3])+" ms | Erreur="+mrUt.dou(mrUt.fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
 		}
 					
		
		String strLegend="MRI spin-echo observations Vs fits methods";
		for(int i=indEstMin;i<=indEstMax;i++) {
			System.out.println("On est à i="+i+" et la legende qui va etre ajoutée est :"+""+(i<4 ?"\n\n" : "\n\n\n")+""+(i-indEstMin)+"-"+tabNames[i]);
			strLegend+=""+(i<4 ?"\n\n" : "\n\n\n")+""+(i-indEstMin)+"-"+tabNames[i];
		}
		actualizeValMaxCourante(Math.max(tabData[0],tabData[1]));
		plotT2.setLimits(0, 200, 0,valMaxCourante);
		plotT2.setColor(new Color(150,150 ,150) );
		plotT2.addLegend(strLegend);
		plotCan2.setPlot(plotT2);
	}


	public void actualizePlotsT1(ImagePlus imgIn,double xCor, double yCor){
		Color []tabColor=new Color[]{new Color(150 ,0 ,255 ),new Color(0,150,255) ,new Color(255,0 ,0 ),new Color(150 ,0 ,255 ),new Color(0,200,0)  };
		int  []tabFit=new int[]{mrUt.T1_RECOVERY,mrUt.T1_RECOVERY_RICE,mrUt.T1_RECOVERY,mrUt.T1_RECOVERY_RICE_NORMALIZED};
		int []tabAlg=new int[]{mrUt.SIMPLEX,mrUt.SIMPLEX,mrUt.TWOPOINTS,mrUt.SIMPLEX};
		int []yPos=new int[]{8,6,4,2};
 		double te=(double)11.5;int sigma=159;int fitType,algType;
		double[]tabData=mrUt.getDataForVoxel(imgIn,(int)xCor,(int)yCor);
		double longT2=tabData[tabData.length-1]/50;double max=0;
		boolean normalized=(tabData[1]+tabData[0])<2;
		int nEstim=normalized?4:3;
		if(normalized)IJ.log("Donnees normalisees");
		double[]tabFittenCurve=new double[tabData.length];
 		double []estimatedParams;
		int nbEchos=tabData.length;
		String[] tabNames=new String[]{"-Fit only","-Fit Rice", "-2 Points", "-Rice norm"};
		double xMinPlot=0,xMaxPlot=6000,xStep=50;
 		double []tabTimes=mrUt.getT1Times(tabData.length);
 		double []tabPlotX=mrUt.getProportionalTimes(xMinPlot,xMaxPlot,xStep);
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
			estimatedParams=mrUt.makeFit(tabTimes, tabData,fitType,algType,100,sigma);
			tabPlotY[indEst]=mrUt.fittenRelaxationCurve(tabPlotX,estimatedParams,sigma,fitType);
			tabMax[indEst]=estimatedParams[0];
			if(indEst==0)valMax=estimatedParams[0];
	        plotT1.setColor(tabColor[indEst]);
			plotT1.replace(incr++, "line",tabPlotX,tabPlotY[indEst]);
			plotT1.replace(incr++, "line",new double[]{0,(double)(estimatedParams[1])},
									   new double[]{estimatedParams[0]+(double)(20*indEst),estimatedParams[0]+(double)(20*indEst)});
			

			IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+mrUt.dou(estimatedParams[0])+" | T1="+mrUt.dou(estimatedParams[1])+" ms | Erreur="+mrUt.dou(mrUt.fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
 		}

		String strLegend="Observations Vs fits methods";
        plotT1.setLimits(xMinPlot, xMaxPlot, 0,valMaxCourante);
        plotT1.setColor(new Color(150,150 ,150) );
		for(int i=0;i<nEstim;i++)strLegend+="\n\n"+i+tabNames[i];
		plotT1.addLegend(strLegend);
		plotCan1.setPlot(plotT1);
	}


	
	
}
