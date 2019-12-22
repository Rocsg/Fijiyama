package com.vitimage;


import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;

import com.itextpdf.text.log.SysoCounter;

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
import ij.plugin.frame.PlugInFrame;
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


public class MRIHyperCurves extends PlugInFrame implements ActionListener,KeyListener, MouseListener {// 
	Scrollbar scrollbarZ;
	Scrollbar scrollbarT;
	int xMouse=0;
	int yMouse=0;
	int crossWidth=0;
	int tCor=1;
	int numberTimes=0;
	int zCor=1;
	int numberZ=0;
	int xCor=1;
	int yCor=1;
	int imgWidth,imgHeight;
	double M0T1Last=0;
	double T1Last=0;
	double M0T2MonoLast=0;
	double M0T21Last=0;
	double M0T22Last=0;
	double T2Last=0;
	double T21Last=0;
	double T22Last=0;
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
	Button btnToggle;
	Button btnNothing;
	Button btnCompute;
	TextField info1;
	TextField info2;
	TextField info12;
	TextField info22;
	TextField info13;
	TextField info23;
	private int buttonWidth = 50;
	private int buttonHeight = 34;
	private int currentCanvas=1;
	private double zoomLevel=0;
	int coordX;
	static PlugInFrame instance;
	int coordY;
	boolean computeMultiComp=true;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int SCROLLBAR_VERTICAL_SIZE = 10;
	private static final int PLOT_HEIGHT = 450;
	private MRUtils mrUt;
	double te=11.157;
	int nTe=16;
	int nTr=3;
	double [] voxSizeT1=MRUtils.getT1Times(nTr);
	double [] voxSizeT2=MRUtils.getProportionalTimes(te, nTe*te,te);
	double [][][]tabSigmas;
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		String rep="/home/fernandr/Bureau/Test/Explorer/";
		ImagePlus echoesT1=IJ.openImage(rep+"B099_PCH_HyperEchoesT1.tif");
		ImagePlus dupT1=VitimageUtils.imageCopy(echoesT1);
		dupT1.show();
		ImagePlus echoesT2=IJ.openImage(rep+"B099_PCH_HyperEchoesT2.tif");
		ImagePlus hyper=IJ.openImage(rep+"B099_PCH_HyperImage.tif");
		MRICurveExplorerWindow explorer=new MRICurveExplorerWindow(echoesT1, echoesT2);

		
	}
	
	
	public static double[]statsBgHyperEchoe(ImagePlus img,int curT,int totalT,int curZ,int totalZ, int curEc, int totalEchoes,boolean makeFourCorners){
		int dimX=img.getWidth();
		int dimY=img.getHeight();
		int samplSize=Math.min(10+20,dimX/15);
		if(dimX<100)samplSize=12;
		int x0=samplSize;
		int deltaZ=2;
		int y0=samplSize;
		int x1=dimX-samplSize;
		int y1=dimY-samplSize;
		double[][] vals=new double[(2*deltaZ+1)*4*totalEchoes][];
		if(makeFourCorners) {
			for(int dz=-deltaZ;dz<deltaZ+1;dz++) {
				for(int echo=0;echo<totalEchoes;echo++) {
					int z01=MRUtils.getCorrespondingSliceInHyperImageMR(curT,totalT,(curZ+dz),totalZ,echo,totalEchoes);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+0]=VitimageUtils.valuesOfImageAround(img,x1,y1,z01,samplSize/2);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+1]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+2]=VitimageUtils.valuesOfImageAround(img,x1,y0,z01,samplSize/2);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+3]=VitimageUtils.valuesOfImageAround(img,x1,y1,z01,samplSize/2);		
				}
			}
		}
		else {
			for(int dz=-deltaZ;dz<deltaZ+1;dz++) {
				for(int echo=0;echo<totalEchoes;echo++) {
					int z01=MRUtils.getCorrespondingSliceInHyperImageMR(curT,totalT,(curZ+dz),totalZ,echo,totalEchoes);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+0]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+1]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+2]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);
					vals[totalEchoes*(dz+deltaZ)*4+echo*4+3]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);		
				}
			}
		}
		System.out.println("");
		return VitimageUtils.statistics2D(vals);
	}	
	
	
	public static double[][][]buildSigmaTab(int numberZ,int numberTimes,int nTr,int nTe,ImagePlus imgT1,ImagePlus imgT2){
		int zMinComp=numberZ/4;
		int zMaxComp=(numberZ*3)/4;
		double[][][]tab=new double[2][numberTimes][numberZ];
		for(int t=0;t<numberTimes;t++) {
			for(int z=zMinComp;z<=zMaxComp;z++) {
				IJ.log("");
				double sigmaT1=0;
				double sigmaT2=0;
				//Mesurer le niveau de bruit pour la coupe z//Le convertir en sigma rice	
				double[]valsT1=statsBgHyperEchoe(imgT1,t,numberTimes,z,numberZ,0,3,imgT1.getWidth()>500);
				System.out.println("T1 noise estimation. Time "+t+" slice "+z);
				double sigmaRiceT1=Acquisition.computeRiceSigmaFromBackgroundValuesStatic(valsT1[0],valsT1[1]);
				IJ.log("T1 noise estimation. Time "+t+" slice "+z+" .  Vals="+valsT1[0]+" , "+valsT1[1]+" , sigma riceT1="+sigmaRiceT1);
				sigmaT1=sigmaRiceT1;
				//Mesurer le niveau de bruit pour la coupe z//Le convertir en sigma rice	
				double[]valsT2=statsBgHyperEchoe(imgT2,t,numberTimes,z,numberZ,0,16,false);
				System.out.println("T2 noise estimation. Time "+t+" slice "+z);
				double sigmaRiceT2=Acquisition.computeRiceSigmaFromBackgroundValuesStatic(valsT2[0],valsT2[1]);
				IJ.log("T2 noise estimation. Time "+t+" slice "+z+" Vals="+valsT2[0]+" , "+valsT2[1]+" , sigma riceT2="+sigmaRiceT2);
				sigmaT2=sigmaRiceT2;
				tab[0][t][z]=sigmaT1;
				tab[1][t][z]=sigmaT2;
			}		
			for(int z=0;z<zMinComp;z++) {tab[0][t][z]=tab[0][t][zMinComp];tab[1][t][z]=tab[1][t][zMinComp];}
			for(int z=zMaxComp+1;z<numberZ;z++) {tab[0][t][z]=tab[0][t][zMaxComp];tab[1][t][z]=tab[1][t][zMaxComp];}
		}
		return tab;
	}
	
	public MRICurveExplorerWindow(ImagePlus imgT1,ImagePlus imgT2) {
		super("Vitimage MRI Water tracker ");
		tCor=1;
		numberTimes=imgT2.getStackSize()/(40*16);
		zCor=20;
		numberZ=40;
		tabSigmas=new double[2][numberTimes][numberZ];
		tabSigmas=buildSigmaTab(numberZ,numberTimes,3,16,imgT1,imgT2);
		WindowManager.addWindow(this); 
		if (instance != null)
	    {
		      instance.toFront();
		      return;
		    }
	    WindowManager.addWindow(this);
	    instance = this;
	    imgWidth=imgT1.getWidth();
		imgHeight=imgT1.getHeight();
		this.imgCan1=new ImageCanvas(imgT1);
		this.imgCan2=new ImageCanvas(imgT2);
		mrUt=new MRUtils();
		ImageJ ij = IJ.getInstance();
        removeKeyListener(ij);
        removeMouseListener(ij);
        startPlotsAndRoi();
		initializeGUI();
		repaintAll();
//		repaint();
   	}
		
	public void setCanvasSizes() {
		imgCan1.setSize(imgWidth*((int)Math.ceil(500/imgWidth)), imgHeight*((int)Math.ceil(500/imgWidth)));
		imgCan2.setSize(imgWidth*((int)Math.ceil(500/imgWidth)), imgHeight*((int)Math.ceil(500/imgWidth)));
		plotCan1.setSize(imgWidth*((int)Math.ceil(500/imgWidth)), PLOT_HEIGHT );
		plotCan2.setSize(imgWidth*((int)Math.ceil(500/imgWidth)), PLOT_HEIGHT );
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
		ImageJ ij = IJ.getInstance();
		setTitle("MRI Curve explorer");
		GridBagConstraints c;

		Panel p1=new Panel();
		setLayout(new GridBagLayout());
		//Texts
		info1=new TextField("Click on the image to get the associated parameters",70);info1.setEditable(false);info1.setFont(new Font("Helvetica", 0, 12));
		info2=new TextField("'s' : switch on/off biexp, '+'/'-' : zoom, 'f' fix plot scale",70);info2.setEditable(false);info2.setFont(new Font("Helvetica", 0, 12));
		add(info1);
		add(info2);

		
		
		//Image T1
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		add(imgCan1,c);
        imgCan1.removeKeyListener(ij);
        imgCan1.addKeyListener(this);
        imgCan1.removeMouseListener(ij);
        imgCan1.addMouseListener(this);

		//Image T2
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		add(imgCan2,c);
        imgCan2.removeKeyListener(ij);
        imgCan2.addKeyListener(this);
        imgCan2.removeMouseListener(ij);
        imgCan2.addMouseListener(this);
		
		//ScrollBar 1 (Z)
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 2;
		scrollbarZ=new Scrollbar(Scrollbar.HORIZONTAL, zCor, 1, 1,this.numberZ+1);
		add(scrollbarZ,c);
		
		//ScrollBar 2 (T)
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 3;
		scrollbarT=new Scrollbar(Scrollbar.HORIZONTAL, tCor, 1, 1,this.numberTimes+1);
		add(scrollbarT,c);
		
		//PlotT1
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 4;
		add(plotCan1,c);
        plotCan1.removeKeyListener(ij);
        plotCan1.addKeyListener(this);
        plotCan1.removeMouseListener(ij);
        plotCan1.addMouseListener(this);

		//PlotT2
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 4;
		add(plotCan2,c);
        plotCan2.removeKeyListener(ij);
        plotCan2.addKeyListener(this);
        plotCan2.removeMouseListener(ij);
        plotCan2.addMouseListener(this);
        
		setCanvasSizes();
		setSize(1600,1040);
		this.pack();
		if(this.imgHeight<100) {
			imgCan1.setMagnification(5);
	        imgCan2.setMagnification(5);
		}
		else if(this.imgHeight<400) {
			imgCan1.setMagnification(2);
	        imgCan2.setMagnification(2);
		}
		else{
			imgCan1.setMagnification(1);
	        imgCan2.setMagnification(1);
		}
		setCanvasSizes();
        this.setResizable(false);
		setVisible(true);
		repaint();
	//	imgT1.hide();
	//	imgT2.hide();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		System.out.println("Touche "+e.getKeyChar());
//		System.out.println("Fenetre active="+ currentCanvas);
		//		System.out.println("Coordonnees actives=("+ xMouse+","+yMouse+")");
		//System.out.println("Niveau de zoom="+zoomLevel);
		//System.out.println("Mag of T1 : "+imgCan1.getMagnification());
		
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
		if (e.getKeyChar()=='f') fixedPlotScale=!fixedPlotScale;
		if (e.getKeyChar()=='s') {
			if(computeMultiComp) {
				int maxCurves=20;
				for(int i =0;i<maxCurves;i++){
					plotT1.replace(i,"line",new double[]{0,1},new double[]{0,0});
					plotT2.replace(i,"line",new double[]{0,1},new double[]{0,0});
				}
				plotT1.setLimits(0, maxT1, 0, maxPlotY);
				plotT2.setLimits(0, maxT2, 0, maxPlotY);
			}
			computeMultiComp=!computeMultiComp;
			System.out.println("Et on sort de la, multicomp="+computeMultiComp);
			PointRoi prT1=new PointRoi(xMouse,yMouse,(crossWidth==0 ? "small" : crossWidth==3 ? "extra large" : crossWidth==2 ? "large" : "medium" )+" yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			xCor=imgCan1.offScreenX(xMouse);
			yCor=imgCan1.offScreenY(yMouse);

			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizeDisplayedNumbers();
			plotCan2.repaint();
		}
		if (e.getKeyChar()=='m') {VitiDialogs.notYet("Map computation launch in MRI Curve explorer");}
		if (e.getKeyChar()=='8' || e.getKeyChar()=='2' || e.getKeyChar()=='4' || e.getKeyChar()=='6' ) {
			if((e.getKeyChar()=='8') && (tCor<numberTimes) )tCor+=1;
			if((e.getKeyChar()=='2') && (tCor>1) )tCor-=1;
			if((e.getKeyChar()=='6') && (zCor<numberZ) )zCor+=1;
			if((e.getKeyChar()=='4') && (zCor>1) )zCor-=1;
			scrollbarT.setValues(tCor, 1,1,numberTimes+1);
			scrollbarZ.setValues(zCor,1,1,numberZ+1);
			System.out.println("Changing coordinates to t="+tCor+"/"+this.numberTimes+"  , z="+zCor+"/"+this.numberZ);
			imgCan2.getImage().setPosition(3,zCor,tCor);
			imgCan1.getImage().setPosition(1,zCor,tCor);
			imgCan2.getImage().setPosition(1,zCor,tCor);
			imgCan1.getImage().setPosition(3,zCor,tCor);
			PointRoi prT1=new PointRoi(xMouse,yMouse,(crossWidth==0 ? "small" : crossWidth==3 ? "extra large" : crossWidth==2 ? "large" : "medium" )+" yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			xCor=imgCan1.offScreenX(xMouse);
			yCor=imgCan1.offScreenY(yMouse);

			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizeDisplayedNumbers();
		}
		if (e.getKeyChar()=='n') {
			this.crossWidth=3;
			PointRoi prT1=new PointRoi(xMouse,yMouse,(crossWidth==0 ? "small" : crossWidth==3 ? "extra large" : crossWidth==2 ? "large" : "medium" )+" yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			xCor=imgCan1.offScreenX(xMouse);
			yCor=imgCan1.offScreenY(yMouse);
			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizeDisplayedNumbers();
		}
		if (e.getKeyChar()=='b') {
			this.crossWidth=2;
			PointRoi prT1=new PointRoi(xMouse,yMouse,(crossWidth==0 ? "small" : crossWidth==3 ? "extra large" : crossWidth==2 ? "large" : "medium" )+" yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			xCor=imgCan1.offScreenX(xMouse);
			yCor=imgCan1.offScreenY(yMouse);
			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizeDisplayedNumbers();
		}
		if (e.getKeyChar()=='v') {
			this.crossWidth=1;
			PointRoi prT1=new PointRoi(xMouse,yMouse,(crossWidth==0 ? "small" : crossWidth==3 ? "extra large" : crossWidth==2 ? "large" : "medium" )+" yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			xCor=imgCan1.offScreenX(xMouse);
			yCor=imgCan1.offScreenY(yMouse);
			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizeDisplayedNumbers();
		}
		if (e.getKeyChar()=='c') {
			this.crossWidth=0;
			PointRoi prT1=new PointRoi(xMouse,yMouse,(crossWidth==0 ? "small" : crossWidth==3 ? "extra large" : crossWidth==2 ? "large" : "medium" )+" yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			xCor=imgCan1.offScreenX(xMouse);
			yCor=imgCan1.offScreenY(yMouse);
			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizeDisplayedNumbers();
			plotCan2.repaint();
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
			PointRoi prT1=new PointRoi(xMouse,yMouse,(crossWidth==0 ? "small" : crossWidth==3 ? "extra large" : crossWidth==2 ? "large" : "medium" )+" yellow hybrid");
			Overlay over=new Overlay(prT1);
			imgCan1.setOverlay(over);
			imgCan2.setOverlay(over);
			xCor=imgCan1.offScreenX(xMouse);
			yCor=imgCan1.offScreenY(yMouse);
			actualizePlotsT1(imgCan1.getImage(),imgCan1.offScreenX(xMouse),imgCan1.offScreenY(yMouse));
			actualizePlotsT2(imgCan2.getImage(),imgCan2.offScreenX(xMouse),imgCan2.offScreenY(yMouse));
			actualizeDisplayedNumbers();
//			String infoT2 = String.format("M0 = %7.1f   |   T1 = %6.1f ms   |   T2 = %5.1f ms", M0Last,T1Last,T2Last);
//			info2.setText(infoT2);
		}
		
		
		repaint();
		plotCan1.repaint();
		imgCan1.repaint();
		plotCan2.repaint();
		imgCan2.repaint();
		System.out.println("Fenetre active="+ currentCanvas+" | Coordonnees actives=("+ xMouse+","+yMouse+")"+"  |  Niveau de zoom="+zoomLevel+" | x="+this.xCor+" y="+this.yCor+" z="+zCor+"t="+tCor);
	}

	public void actualizeDisplayedNumbers() {
		String infoT1 = String.format("M0 = %5.3f - T1 = %6.1f ms", M0T1Last,T1Last);
		info1.setText(infoT1);
		String infoT2 = String.format("M0=%5.3f - M01=%5.3f - M02=%5.3f || T2=%5.1f ms - T21=%5.1f ms - T22=%5.1f ms", M0T2MonoLast,M0T21Last,M0T22Last,T2Last,T21Last,(T22Last>1000 ? 999 : T22Last));
		info2.setText(infoT2);
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

	public void windowClosing(WindowEvent paramWindowEvent)
	  {
	    super.windowClosing(paramWindowEvent);
	    instance = null;
	  }
	
	public void startPlotsAndRoi(){
		plotT1 = new Plot("T1 curve explorer","Tr","Value");
		plotT2 = new Plot("T2 curve explorer","Te","Value");
		plotT1.setSize((imgWidth>200 ? imgWidth : 500),PLOT_HEIGHT);
		plotT2.setSize((imgWidth>200 ? imgWidth : 500),PLOT_HEIGHT);
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
		Color []tabColor=new Color[]{new Color(150,0,255),new Color(0 ,150 ,255 ) ,new Color(255,0 ,0 ),new Color(0,200,0) , new Color(255,160 ,160), new Color(160,200,160),  };
		int  []tabFit=new int[]{mrUt.T2_RELAX_RICE,mrUt.T2_RELAX,mrUt.T2_RELAX,mrUt.MULTICOMP_RICE,mrUt.MULTICOMP};
		int []tabAlg=new int[]{mrUt.SIMPLEX,mrUt.SIMPLEX,mrUt.TWOPOINTS,mrUt.SIMPLEX,mrUt.SIMPLEX};
		int []yPos=new int[]{10,8,6,4,2,0};
		double[]tabData=mrUt.getDataForVoxel(imgIn,(int)xCor,(int)yCor,tCor-1,this.numberTimes,zCor-1,numberZ,this.nTe,this.crossWidth);
		double[]tabFittenCurve=new double[tabData.length];
 		double te=11;
 		double sigma=this.tabSigmas[1][tCor-1][zCor-1];int fitType,algType;boolean multiComp=false;
		double longT2=0.01;double max=0;
		int indEstMax=(computeMultiComp ? 4 : 2);
 		int indEstMin=0;
 		int nEstim=indEstMax-indEstMin+1;
 		double []estimatedParams;
		int nbEchos=tabData.length;
		String[] tabNames=new String[]{"One T2 with noise","One T2 naive", "One T2 two-echoes", "Two T2 with noise", "Two T2 naive"};
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
			plotT2.replace(incr++, "line",tabPlotX,tabPlotY[indEst-indEstMin]);//Afficher la courbe
			plotT2.replace(incr++, "line",new double[]{0,(double)(estimatedParams[1])},new double[]{yPos[indEst]*longT2+0.8,yPos[indEst]*longT2+0.8});//Afficher le T2
			if(indEst>=3)plotT2.replace(incr++, "line",new double[]{0,(double)(estimatedParams[3]>xMaxPlot ? xMaxPlot : estimatedParams[3])},
														new double[]{yPos[indEst]*longT2+0.8-0.008,yPos[indEst]*longT2+0.8-0.008});

			if(indEst<3)IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+mrUt.dou(estimatedParams[0])+" | T2="+mrUt.dou(estimatedParams[1])+" ms | Erreur="+mrUt.dou(mrUt.fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
			else IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+mrUt.dou(estimatedParams[0])+" | T2="+mrUt.dou(estimatedParams[1])+" ms | M02="+mrUt.dou(estimatedParams[2])+" | T22="+mrUt.dou(estimatedParams[3])+" ms | Erreur="+mrUt.dou(mrUt.fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
			if(indEst==0) {//Actualize values displayed in info text
				M0T2MonoLast=mrUt.dou(estimatedParams[0]);T2Last=mrUt.dou(estimatedParams[1]);
			}
			if(indEst==3) {//Actualize values displayed in info text
				M0T21Last=mrUt.dou(estimatedParams[0]);T21Last=mrUt.dou(estimatedParams[1]);
				M0T22Last=mrUt.dou(estimatedParams[2]);T22Last=mrUt.dou(estimatedParams[3]);
			}
		}
			
		
		String strLegend="MRI T2 relaxation";
		for(int i=indEstMin;i<=indEstMax;i++) {
			strLegend+=""+(i<4 ?"\n\n" : "\n\n\n")+""+(i-indEstMin)+"-"+tabNames[i];
		}
		actualizeValMaxCourante(Math.max(tabData[0],tabData[1]));
		plotT2.setLimits(0, xMaxPlot, 0,1.0);
		plotT2.setColor(new Color(150,150 ,150) );
		plotT2.addLegend(strLegend,"top-right");
		plotCan2.setPlot(plotT2);
	}

	public void actualizePlotsT1(ImagePlus imgIn,double xCor, double yCor){
		Color []tabColor=new Color[]{new Color(150 ,0 ,255 ),new Color(0,150,255) ,new Color(255,0 ,0 ),new Color(150 ,0 ,255 ),new Color(0,200,0)  };
		int  []tabFit=new int[]{mrUt.T1_RECOVERY_RICE,mrUt.T1_RECOVERY,mrUt.T1_RECOVERY};
		int []tabAlg=new int[]{mrUt.SIMPLEX,mrUt.SIMPLEX,mrUt.TWOPOINTS,mrUt.SIMPLEX};
		int []yPos=new int[]{8,6,4,2};
 		double te=(double)11.5;int fitType,algType;double sigma=this.tabSigmas[0][tCor-1][zCor-1];
 		double[]tabData;
 		tabData=mrUt.getDataForVoxel(imgIn,(int)xCor,(int)yCor,tCor-1,this.numberTimes,zCor-1,numberZ,this.nTr,this.crossWidth);
 		
 		double longT2=tabData[tabData.length-1]/50;double max=0;
		boolean normalized=false;
		int nEstim=normalized?4:3;
		if(normalized)IJ.log("Donnees normalisees");
		double[]tabFittenCurve=new double[tabData.length];
 		double []estimatedParams;
		int nbEchos=tabData.length;
		String[] tabNames=new String[]{"-Fit with noise estimation","-Naive fit","-Two echoes computation"};
		double xMinPlot=0,xMaxPlot=6000,xStep=50;
 		double []tabTimes=mrUt.getT1Times(tabData.length);
 		double []tabPlotX=mrUt.getProportionalTimes(xMinPlot,xMaxPlot,xStep);
		int nxPlot=(int)(Math.ceil((xMaxPlot-xMinPlot)/xStep));
		double[][]tabPlotY=new double[nEstim][nxPlot];
		double valTemp,valMax=0;
		double[]tabMax=new double[nEstim];
		int incr=0;
        IJ.log("\n New point : ("+xCor+" , "+yCor+")");
	
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
									   new double[]{1.1-indEst*0.02,1.1-indEst*0.02});
			

			IJ.log(""+tabNames[indEst]+". Valeurs estimées : M0="+mrUt.dou(estimatedParams[0])+" | T1="+mrUt.dou(estimatedParams[1])+" ms | Erreur="+mrUt.dou(mrUt.fittingAccuracy(tabData,tabTimes,sigma,estimatedParams,fitType)) );
			if(indEst==0) {
				M0T1Last=mrUt.dou(estimatedParams[0]);
				T1Last=mrUt.dou(estimatedParams[1]);
			}
 		}

		String strLegend="MRI T1 recovery";
        plotT1.setLimits(xMinPlot, xMaxPlot, 0,1.3);
        plotT1.setColor(new Color(150,150 ,150) );
		for(int i=0;i<nEstim;i++)strLegend+="\n\n"+i+tabNames[i];
		plotT1.addLegend(strLegend,"bottom-right");
		plotCan1.setPlot(plotT1);
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}


	
	
}
