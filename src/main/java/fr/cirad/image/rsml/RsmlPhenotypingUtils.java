package fr.cirad.image.rsml;
import org.scijava.vecmath.Point3d;

import fr.cirad.image.common.VitimageUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import java.io.File;

public class RsmlPhenotypingUtils {

	
	public static void main(String []args) {
		ImageJ ij=new ImageJ();
		test5();
	}
	

	
	//Only test the view
	public static void test0() {
		System.out.println("Running test sequence 1");
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Processing_by_box/ML1_Boite_00002"; 
		String rsmlPath=dataPath+"/4_2_Model.rsml";
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		RootModel.viewRsmlAndImageSequenceSuperposition(rm,imgSeq,1).show();
		System.out.println("Test sequence 1 finished");
	}
		
	//Test a first measurement of a lateral root
	public static void test1() {
		System.out.println("Running test sequence 1");
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Processing_by_box/ML1_Boite_00002"; 
		String rsmlPath=dataPath+"/4_2_Model.rsml";
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		RootModel.viewRsmlAndImageSequenceSuperposition(rm,imgSeq,1).show();
		System.out.println("Test sequence 1 finished");
		Root r=(Root)(rm.getNearestRootSegment(new Point3d(535,825,100), 100.0)[1]);
		System.out.println(r);

			for(int i=0;i<23 ;i++)System.out.println("Len "+i+"="+r.computeRootLength(i));
			System.out.println("Len 21.1="+r.computeRootLength(21.1));
			System.out.println("Len 21.5="+r.computeRootLength(21.5));
			System.out.println("Len 21.8="+r.computeRootLength(21.8));
	}
		
	//Test a first measurement of a primary root
	public static void test2() {
		System.out.println("Running test sequence 1");
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Processing_by_box/ML1_Boite_00002"; 
		String rsmlPath=dataPath+"/4_2_Model.rsml";
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		RootModel.viewRsmlAndImageSequenceSuperposition(rm,imgSeq,1).show();
		System.out.println("Test sequence 1 finished");
		Root r=(Root)(rm.getNearestRootSegment(new Point3d(619,447,100), 100.0)[1]);
		System.out.println(r);

			for(int i=0;i<23 ;i++)System.out.println("Len "+i+"="+r.computeRootLength(i));
			System.out.println("Len 21.1="+r.computeRootLength(21.1));
			System.out.println("Len 21.5="+r.computeRootLength(21.5));
			System.out.println("Len 21.8="+r.computeRootLength(21.8));
	}

	public static void test3() {
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Processing_by_box/ML1_Boite_00002"; 
		String rsmlPath=dataPath+"/4_2_Model.rsml";
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		RootModel.viewRsmlAndImageSequenceSuperposition(rm,imgSeq,1).show();
		//Check target parameters for various values of t, especially for real values
		for(int t=0;t<=imgSeq.getStackSize();t++) {
			System.out.println();
			double N=rm.getNPRoot(t);
			double L=rm.getTotalPRLength(t);
			System.out.println("Number and length of primaries at t="+t+" : "+N+" , "+L);
			N=rm.getNSRoot(t);
			L=rm.getTotalSRLength(t);
			System.out.println("Number and length of laterals at t="+t+" : "+N+" , "+L);
		}
	
		System.out.println("Test sequence ok");
	}
	
	public static void test4() {
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_00001"; 
		String rsmlPath=dataPath+"/Expertized_models/";
		int N=new File(rsmlPath).list().length-1;
		String nom1="4_2_Model_0000.rsml";
		String nom2="4_2_Model_"+(N<1000 ? "0":"")+(N<100 ? "0":"")+(N<10 ? "0":"") +N+".rsml";
		String rsmlInitPath=rsmlPath+nom1;
		String rsmlFinalPath=rsmlPath+nom2;
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
		RootModel rmInit=RootModel.RootModelWildReadFromRsml(rsmlInitPath);
		rmInit.cleanWildRsml();
		rmInit.resampleFlyingRoots();
		RootModel rmFinal=RootModel.RootModelWildReadFromRsml(rsmlFinalPath);
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		RootModel.viewRsmlAndImageSequenceSuperposition(rmInit,imgSeq,1).show();
		RootModel.viewRsmlAndImageSequenceSuperposition(rmFinal,imgSeq,1).show();

		
		for(int t=0;t<=imgSeq.getStackSize();t++) {
			System.out.println();
			double NInit=rmInit.getNPRoot(t);
			double LInit=rmInit.getTotalPRLength(t);
			double NFinal=rmFinal.getNPRoot(t);
			double LFinal=rmFinal.getTotalPRLength(t);
			double deltaN=(NInit-NFinal)*100.0/NFinal;
			double deltaL=(LInit-LFinal)*100.0/LFinal;
			System.out.println("Primaries at t="+t+" : N="+NInit+" ("+NFinal+"), delta="+deltaN+" %  , L="+LInit +" ("+LFinal+"), delta="+deltaL+" % ");
			NInit=rmInit.getNSRoot(t);
			LInit=rmInit.getTotalSRLength(t);
			NFinal=rmFinal.getNSRoot(t);
			LFinal=rmFinal.getTotalSRLength(t);
			deltaN=(NInit-NFinal)*100.0/NFinal;
			deltaL=(LInit-LFinal)*100.0/LFinal;
			System.out.println("Laterals at t="+t+" : N="+NInit+" ("+NFinal+"), delta="+deltaN+" %  , L="+LInit +" ("+LFinal+"), delta="+deltaL+" % ");
		}
	
		System.out.println("Test sequence ok");
		
	}
	
	public static void test5() {
		String[]items=new String[]{"deltaNprim","deltaLprim","deltaNlat","deltaLlat"};
		double[][]datas=new double[4][13];
		for(int boi=1;boi<13;boi++) {
			System.out.println("\nBoite "+boi);
			String strBoi=(boi<10 ?"0":"")+boi;
			String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_000"+strBoi; 
			String rsmlPath=dataPath+"/Expertized_models/";
			int N=new File(rsmlPath).list().length-1;
			String nom1="4_2_Model_0000.rsml";
			String nom2="4_2_Model_"+(N<1000 ? "0":"")+(N<100 ? "0":"")+(N<10 ? "0":"") +N+".rsml";
			String rsmlInitPath=rsmlPath+nom1;
			String rsmlFinalPath=rsmlPath+nom2;
			String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
			RootModel rmInit=RootModel.RootModelWildReadFromRsml(rsmlInitPath);
			rmInit.cleanWildRsml();
			rmInit.resampleFlyingRoots();
			RootModel rmFinal=RootModel.RootModelWildReadFromRsml(rsmlFinalPath);
			
			ImagePlus imgSeq=IJ.openImage(imgSeqPath);
			/*
			RootModel.viewRsmlAndImageSequenceSuperposition(rmInit,imgSeq,1).show();
			RootModel.viewRsmlAndImageSequenceSuperposition(rmFinal,imgSeq,1).show();
			 */
			
			int t=imgSeq.getStackSize();
			double NInit=rmInit.getNPRoot(t);
			double LInit=rmInit.getTotalPRLength(t);
			double NFinal=rmFinal.getNPRoot(t);
			double LFinal=rmFinal.getTotalPRLength(t);
			double deltaN=VitimageUtils.dou( (NInit-NFinal)*100.0/NFinal);
			double deltaL=VitimageUtils.dou( (LInit-LFinal)*100.0/LFinal);
			datas[0][boi]=deltaN;
			datas[1][boi]=deltaL;
			System.out.println("Primary root at t="+t+" : N roots="+NInit+" (Expected value="+NFinal+"), delta="+deltaN+" %  , Total length="+LInit +" (Expected value="+LFinal+"), Error="+deltaL+" % ");
			NInit=rmInit.getNSRoot(t);
			LInit=rmInit.getTotalSRLength(t);
			NFinal=rmFinal.getNSRoot(t);
			LFinal=rmFinal.getTotalSRLength(t);
			deltaN=VitimageUtils.dou( (NInit-NFinal)*100.0/NFinal);
			deltaL=VitimageUtils.dou( (LInit-LFinal)*100.0/LFinal);
			datas[2][boi]=deltaN;
			datas[3][boi]=deltaL;
			System.out.println("Lateral roots at t="+t+" : N roots="+NInit+" (Expected value="+NFinal+"), delta="+deltaN+" %  , Total length="+LInit +" (Expected value="+LFinal+"), Error="+deltaL+" % ");
		}	
		System.out.println("Test sequence ok");
		for(int s=0;s<4;s++) {
			double[]st=VitimageUtils.statistics1D(datas[s]);
			System.out.println("Stats "+items[s]+ "= "+st[0]+" +- "+st[1]);
		}
	}

}
