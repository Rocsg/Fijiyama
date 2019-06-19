package com.vitimage;

import java.util.ArrayList;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;

public class HyperImage {
	private String title="";
	private ImagePlus hyperImage;
	private int nbC;
	private int nbT;
	private int nbZ;
	private String path;
	private static double lowThreshForExtinction=0.1;
	private String[]days= {"Day 0","Day 35","Day 70","Day 135","Day 218"};
	private String[]modalities= {"T1-weighted","M0 map from T1 seq","T1 map","T2-weighted","M0 map from T2 seq","T2 map","RX"};
	
	
	public static void testing() {
		String strBoutureTest="B001_PAL";
		
		String pathTest="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+strBoutureTest+"/Computed_data/2_HyperImage";
		String fullPath=pathTest+"/"+strBoutureTest+"/_HyperImage.tif";
		HyperImage hyp=new HyperImage(fullPath,7,40,5,strBoutureTest);
		ImagePlus test=IJ.openImage("/home/fernandr/Bureau/Test/foo.tif");
//		ImagePlus res=VitimageUtils.removeEveryTitleTextInStandardFloatImage(test);
		System.out.println("Pouet1");
		test.show();
		System.out.println("Pouet2");
		System.out.println("Pouet3");
	}
	
	public  ImagePlus printMetaDataOnImage(ImagePlus imgIn,int c,int dayInit,int dayEnd,String textMore) {
		ImagePlus img=new Duplicator().run(imgIn);
		
		//Print title
		img=VitimageUtils.writeTextOnImage(this.title+" "+modalities[c]+" - "+textMore,img ,15, 0);
		
		
		//Print legend
		for(int t=dayInit;t<=dayEnd;t++) {
			img=VitimageUtils.drawThickLineInFloatImage(img, 0.1, 10, 78+30*(t-dayInit), 0,new double[] {0,0,1},1+t-dayInit);
		}
		for(int t=dayInit;t<=dayEnd;t++) {
			img=VitimageUtils.writeTextOnImage("   "+days[t],img ,15, 2+t-dayInit);
		}
		return img;
	}
	
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		boolean unitTesting=false;
		if(unitTesting) {testing();return;}
		for(double sigma=0.01;sigma<0.03;sigma+=0.01) {
			System.out.println("Test avec sigma="+sigma);
			String []bouturesTest=new String[] {"B041_DS"};
		//	String []bouturesTest=new String[] {"B001_PAL","B031_NP","B032_NP","B041_DS","B042_DS","B051_CT"};
			for(int bout=0;bout<bouturesTest.length;bout++) {
				String strBoutureTest=bouturesTest[bout];
				System.out.println("Traitement bouture "+strBoutureTest);
				String pathTest="/home/fernandr/Bureau/Traitements/Bouture6D/Source_data/"+strBoutureTest+"/Computed_data/2_HyperImage";
				String fullPathHyp=pathTest+"/"+strBoutureTest+"_HyperImage.tif";
				String fullPathMaxMap=pathTest+"/"+strBoutureTest+"_Sigma_"+sigma+"_Max_and_last_maps.tif";
				//	String fullPathT2OverT1=pathTest+"/"+strBoutureTest+"_T2_over_T1.tif";
				System.out.println("--ouverture hyperimage");
				HyperImage hyp=new HyperImage(fullPathHyp,7,40,5,strBoutureTest);
				hyp.hyperImage.show();
				System.out.println("--calcul Max et last");
				ImagePlus imgMax=hyp.computeMaxAndLastMaps(false,0,4,sigma);
				//		System.out.println("--calcul T2 sur T1");
				//			ImagePlus imgT2OverT1=hyp.computeT2OverT1();
				System.out.println("--sauvegarde");
				IJ.saveAsTiff(imgMax,fullPathMaxMap);
	//			IJ.saveAsTiff(imgT2OverT1,fullPathT2OverT1);
				hyp.hyperImage.hide();
				imgMax=null;
				imgMax=null;
				hyp=null;
				System.gc();
			}
		}
	}
		
	public HyperImage(String path,int nbC, int nbZ, int nbT,String title) {
		this.title=title;
		this.path=path;
		this.nbZ=nbZ;
		this.nbC=nbC;
		this.nbT=nbT;
		this.hyperImage=IJ.openImage(this.path);
	}

	public ImagePlus getSlice(int channel, int z, int time) {
		return(new Duplicator().run(this.hyperImage, channel, channel, z, z, time, time));
	}
	
	
	public ImagePlus getStack(int channel, int time) {
//		System.out.println("Requete de stack suivant les parametres :");
		//		System.out.println(""+channel+", "+channel+" , "+1+", "+nbZ+" , "+time+" , "+time);
		return(new Duplicator().run(this.hyperImage, channel, channel, 1, nbZ, time, time));
	}
	
	public ImagePlus sliceArrayToImageStack(ArrayList<ImagePlus> array) {
		ImagePlus []tabImg=new ImagePlus[array.size()];
		for(int i =0;i<array.size() ; i++) 	tabImg[i]=array.get(i);
		return (Concatenator.run(tabImg));
	}
	
	
	public ImagePlus computeT2OverT1() {
		ImagePlus[]tabRet=new ImagePlus[this.nbT];
		ImagePlus imgT1;
		ImagePlus imgT2;
		for(int t=1;t<=this.nbT ;t++) {
			imgT1=this.getStack(3,t);
			imgT2=this.getStack(6,t);		
			ImageCalculator ic = new ImageCalculator();
			tabRet[t-1] = ic.run("Divide create 32-bit stack", imgT2, imgT1);			
			tabRet[t-1]=VitimageUtils.writeTextOnImage(this.title+" "+days[t-1]+" - T2 over T1",tabRet[t-1] ,15, 0);
		}
		ImagePlus ret=Concatenator.run(tabRet);
		IJ.run(ret,"Fire","");
		ret.getProcessor().setMinAndMax(0,3);
		return(ret);		
	}
	
	
	public ImagePlus makeMovieRegistrationResults() {
		//Parameters of sequence
		int fps=35;
		double secondsTransition=2;
		int frameIdle=(int)Math.round(fps*secondsTransition);
		int frameRest=20;
		int sliceRef=17;
		int sliceMax=30;
		int sliceMin=10;

		int curSlice=0;
		ArrayList<ImagePlus> movieArray=new ArrayList<ImagePlus>();
		
		//Sequence 1 : idle on first echo, slice 20
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(1, sliceRef,1));
/*
		//Sequence 2 : varying Z + and -
		for(int zz=0;zz<100;zz++) movieArray.add(this.getSlice(1, sliceRef+zz/10 ,1));
		for(int zz=0;zz<200;zz++) movieArray.add(this.getSlice(1, sliceRef+5-zz/10,1));
		for(int zz=0;zz<100;zz++)movieArray.add(this.getSlice(1, sliceRef-5+zz/10,1));
*/
		//Sequence 3 : varying in time
		for(int idl=0;idl<frameIdle;idl++) movieArray.add(this.getSlice(1, sliceRef,1));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(1, sliceRef,2));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(1, sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(1, sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(1, sliceRef,3));

		
		//Sequence 4 : varying in modality
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(1, sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++) movieArray.add(this.getSlice(4,sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(7, sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(7, sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(7, sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(7, sliceRef,3));
		for(int idl=0;idl<frameIdle;idl++)movieArray.add(this.getSlice(7, sliceRef,3));

		
		return sliceArrayToImageStack(movieArray); 
	}
	
	
	public ImagePlus computeMaxAndLastMaps(boolean tissueNormalisation,int dayInit,int dayEnd,double sigma) {
		if(dayInit<0 )dayInit=0;
		if(dayEnd>=this.nbT )dayEnd=this.nbT;
		if(dayEnd-dayInit<1)IJ.showMessage("Computation of max and last on a 1 time sequence : not feasible");
		ImagePlus[] tabTempMaxAndLast=new ImagePlus[2];
		if(tissueNormalisation) {
			
		}
		else {
			//We assume that capillary normalisation is the best.
			boolean []tabTodo=new boolean[this.nbC];
			int nbEffC=0,indexC=0;
			for(int c=0;c<this.nbC;c++) {
				tabTodo[c]=true;
				for(int t=dayInit;t<=dayEnd;t++) {
					ImagePlus img=VitimageUtils.removeEveryTitleTextInStandardFloatImage(this.getStack(c+1,t+1));
					if(VitimageUtils.isNullImage(img)) {
						tabTodo[c]=false;
					}
				}
				if(tabTodo[c])nbEffC++;
			}

			
			ImagePlus []concatTab=new ImagePlus[nbEffC];
			//for each c
			for(int c=0;c<this.nbC;c++) {
				System.out.println("Calcul des cartes pour le canal "+c+" = "+modalities[c]+"...");
				if(tabTodo[c]) {
					ImagePlus []tabImgT=new ImagePlus[dayEnd-dayInit+1];
					for(int t=dayInit;t<=dayEnd;t++)tabImgT[t-dayInit]=VitimageUtils.gaussianFiltering(VitimageUtils.removeEveryTitleTextInStandardFloatImage(this.getStack(c+1,t+1)),sigma,sigma,sigma);
					tabTempMaxAndLast=getMaxAndLastMaps(tabImgT);
					tabTempMaxAndLast[0].setDisplayRange(0, dayEnd-dayInit+2);
					tabTempMaxAndLast[1].setDisplayRange(0, dayEnd-dayInit+2);
					tabTempMaxAndLast[0]=printMetaDataOnImage(tabTempMaxAndLast[0],c,dayInit,dayEnd,"Day of maximum value");
					tabTempMaxAndLast[1]=printMetaDataOnImage(tabTempMaxAndLast[1],c,dayInit,dayEnd,"Day of last value before vanishing");
					
					ImagePlus imgToAdd=Concatenator.run(tabTempMaxAndLast);
					//VitimageUtils.imageChecking(imgToAdd);
					concatTab[indexC++]=imgToAdd;					
				}
			}
			Concatenator con=new Concatenator();
			con.setIm5D(true);
			ImagePlus concatImage=con.concatenate(concatTab,true);
			
			ImagePlus resultImage=HyperStackConverter.toHyperStack(concatImage,
					nbEffC, this.nbZ,2,"xyztc","Grayscale");
			resultImage.setDisplayRange(0, dayEnd-dayInit+2);
//			resultImage.show();
			IJ.run(resultImage,"Fire","");
			resultImage.setTitle(this.title+" Max and Last maps");
			resultImage.setSlice(this.nbZ/2);
			return resultImage;
		}
		
		return null;
		
	}
	
	
	
	
	public ImagePlus[] getMaxAndLastMaps(ImagePlus []tabImg) {
		ImagePlus imgMax=new Duplicator().run(tabImg[0]);
		ImagePlus imgLast=new Duplicator().run(tabImg[0]);
		imgMax.getProcessor().set(0);
		imgLast.getProcessor().set(0);
		int X=imgMax.getWidth();
		float[][] dataIn=new float[tabImg.length][];
		float[] dataMax;
		float[] dataLast;
		int[]tabMaxAndLastTemp=new int[2];
		float[]tabDataTemp=new float[tabImg.length];
		for(int z=0;z<imgMax.getStackSize();z++) {
			for(int t=0;t<tabImg.length;t++)dataIn[t]=(float []) tabImg[t].getStack().getProcessor(z+1).getPixels();
			dataMax=(float []) imgMax.getStack().getProcessor(z+1).getPixels();
			dataLast=(float []) imgLast.getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<imgMax.getWidth();x++) {
				for(int y=0;y<imgMax.getHeight();y++) {
					for(int t=0;t<tabImg.length;t++)tabDataTemp[t]=dataIn[t][y*X+x];
					tabMaxAndLastTemp=indexesOfMaxAndLastOverFloatTab(tabDataTemp,this.lowThreshForExtinction) ;
					dataMax[y*X+x]=tabMaxAndLastTemp[0]+1;
					dataLast[y*X+x]=tabMaxAndLastTemp[1]+1;
				}			
			}
		}
		return new ImagePlus[] {imgMax,imgLast};
	}
	
	
	
	public int[] indexesOfMaxAndLastOverFloatTab(float[]tab,double lowThreshForExtinction) {
		double epsilon=10E-10;
		boolean isNull=true;
		for(float val:tab)if(Math.abs(val)>epsilon)isNull=false;
		if(isNull)return new int[] {-1,-1};
		int indMax=-1;
		int indLast=-1;
		double max=-10E10;
		for(int ind=0;ind<tab.length ;ind++) {
			if(tab[ind]>max) {
				max=tab[ind];
				indMax=ind;
			}
		}
		indLast=tab.length;
		boolean found=false;
		for(int ind=indMax+1;!found && ind<tab.length ;ind++) {
			if(tab[ind]<lowThreshForExtinction) {
				found=true;
				indLast=ind;
			}
		}
		int[]ret=new int[] {indMax,indLast};
		return (ret);
	}
	




}
