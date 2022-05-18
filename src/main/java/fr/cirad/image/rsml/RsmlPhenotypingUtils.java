package fr.cirad.image.rsml;
import org.scijava.vecmath.Point3d;

import fr.cirad.image.common.VitimageUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import java.io.File;
import java.util.ArrayList;

public class RsmlPhenotypingUtils {

	
	public static void main(String []args) {
		ImageJ ij=new ImageJ();
		//test6();
		testProduceSimplifyLatV1() ;
		//testCompareViewsOfExpert();
	}
	

	//Only test the view
	public static void testCompareViewsOfExpert() {
		int boi=6;
		System.out.println("\nBoite "+boi);
		String strBoi=(boi<10 ?"0":"")+boi;
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_000"+strBoi; 
		String rsmlPath=dataPath+"/Expertized_models/";
		int N=new File(rsmlPath).list().length-1;
		String nom1="4_2_Model.rsml";
		String nom2="4_2_Model_"+(N<1000 ? "0":"")+(N<100 ? "0":"")+(N<10 ? "0":"") +N+".rsml";
		String rsmlPredPath=dataPath+nom1;
		String rsmlExpertPath=rsmlPath+nom2;
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
		RootModel rmPred=RootModel.RootModelWildReadFromRsml(rsmlPredPath);
		rmPred.cleanWildRsml();
		rmPred.resampleFlyingRoots();
		RootModel rmExpert=RootModel.RootModelWildReadFromRsml(rsmlExpertPath);		
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		ImagePlus imgPred=RootModel.viewRsmlAndImageSequenceSuperposition(rmPred,imgSeq,1);
		imgPred.setTitle("PRED");
		imgPred.show();
		ImagePlus imgExpert=RootModel.viewRsmlAndImageSequenceSuperposition(rmExpert,imgSeq,1);
		imgExpert.setTitle("EXPERT");
		imgExpert.show();
	}
	
	//Only test the view
	public static void test0() {
		System.out.println("Running test sequence 1");
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Processing_by_box/ML1_Boite_00006"; 
		String rsmlPath=dataPath+"/4_2_Model.rsml";
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		RootModel.viewRsmlAndImageSequenceSuperposition(rm,imgSeq,1).show();
		System.out.println("Test sequence 1 finished");
	}
		
	//Test measurement of a lateral root
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
		
	//Test measurement of a primary root
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

	//Test counting primaries and laterals
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

	
	//Count for each plant : nlats, llats,lprim and compare with expertized plant
	public static void testProduceStatsPrimLatLengthNumber() {
		String[]items=new String[]{"deltaNprim","deltaLprim","deltaNlat","deltaLlat"};
		int boiMinIndex=1;
		int boiMaxIndex=20;
		int nBoi=boiMaxIndex-boiMinIndex+1;
		int nSpec=nBoi*5;
		double[][]datas=new double[nSpec][8];
		ArrayList<Double>stats=new ArrayList<Double>();
		for(int boi=boiMinIndex;boi<=boiMaxIndex;boi++) {
			System.out.println("\nBoite "+boi);
			String strBoi=(boi<10 ?"0":"")+boi;
			
			//Manage data path
			String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_000"+strBoi; 
			String rsmlPath=dataPath+"/Expertized_models/";
			int N=new File(rsmlPath).list().length-1;
			String nom1="4_2_Model.rsml";
			String nom2="4_2_Model_"+(N<1000 ? "0":"")+(N<100 ? "0":"")+(N<10 ? "0":"") +N+".rsml";
			String rsmlPredPath=dataPath+"/"+nom1;
			String rsmlExpertPath=rsmlPath+nom2;
			String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";			
			ImagePlus imgSeq=IJ.openImage(imgSeqPath);
			int t=imgSeq.getStackSize();
			
			//Open and clean the models
			RootModel rmPred=RootModel.RootModelWildReadFromRsml(rsmlPredPath);
			rmPred.cleanWildRsml();
			rmPred.resampleFlyingRoots();
			rmPred.setPlantNumbers();
			double[][][]valPred=rmPred.getNumberAndLenghtOverTimeSerie(t);
			RootModel rmExpert=RootModel.RootModelWildReadFromRsml(rsmlExpertPath);
			rmExpert.cleanWildRsml();
			rmExpert.resampleFlyingRoots();
			rmExpert.setPlantNumbers();
			double[][][]valExpert=rmExpert.getNumberAndLenghtOverTimeSerie(t);
			
			//Test 1
			for(int i=0;i<5;i++) {
				int indexSpec=(boi-boiMinIndex)*5+i;
				datas[indexSpec][0]=boi;
				datas[indexSpec][1]=i;
				datas[indexSpec][0+2]=valPred[i][t-1][0];
				datas[indexSpec][1+2]=valExpert[i][t-1][0];
				datas[indexSpec][0+2+2]=valPred[i][t-1][1];
				datas[indexSpec][1+2+2]=valExpert[i][t-1][1];
				datas[indexSpec][0+4+2]=valPred[i][t-1][2];
				datas[indexSpec][1+4+2]=valExpert[i][t-1][2];
			}
		}
		VitimageUtils.writeDoubleTabInCsv(datas,"/home/rfernandez/Bureau/A_Test/RSML/OverallAnalyses/StatsPrimLat_1/statsPrimLatTmax.csv");
		System.out.println("Data production ok");
		for(int s=0;s<4;s++) {
			double[]st=VitimageUtils.statistics1D(datas[s]);
			System.out.println("Stats "+items[s]+ "= "+st[0]+" +- "+st[1]);
		}
	}



	public static Object[]readExpertizedData(int boi){
		String strBoi=(boi<10 ?"0":"")+boi;
		
		//Manage data path
		String dataPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_000"+strBoi; 
		String rsmlPath=dataPath+"/Expertized_models/";
		int N=new File(rsmlPath).list().length-1;
		String nom1="4_2_Model.rsml";
		String nom2="4_2_Model_"+(N<1000 ? "0":"")+(N<100 ? "0":"")+(N<10 ? "0":"") +N+".rsml";
		String rsmlPredPath=dataPath+"/"+nom1;
		String rsmlExpertPath=rsmlPath+nom2;
		String imgSeqPath=dataPath+"/1_5_RegisteredSequence.tif";			
		ImagePlus imgSeq=IJ.openImage(imgSeqPath);
		
		//Open and clean the models
		RootModel rmPred=RootModel.RootModelWildReadFromRsml(rsmlPredPath);
		rmPred.cleanWildRsml();
		rmPred.resampleFlyingRoots();
		rmPred.setPlantNumbers();
		rmPred.computeSpeedVectors(5);
		RootModel rmExpert=RootModel.RootModelWildReadFromRsml(rsmlExpertPath);
		rmExpert.cleanWildRsml();
		rmExpert.resampleFlyingRoots();
		rmExpert.setPlantNumbers();
		rmExpert.computeSpeedVectors(5);
		return new Object[] {imgSeq,rmPred,rmExpert};
	}
	
	
	//Export for each plant : nlats, llats,lprim and compare with expertized plant
	public static void testProduceSimplifyLatV1() {
		int boiMinIndex=1;
		int boiMaxIndex=20;
		int nBoi=boiMaxIndex-boiMinIndex+1;
		int nSpec=nBoi*5;
		double[][]datas=new double[nSpec][8];
		ArrayList<Double>stats=new ArrayList<Double>();
		for(int boi=boiMinIndex;boi<=boiMaxIndex;boi++) {
			System.out.println("\nBoite "+boi);
			Object[]obj=readExpertizedData(boi);
			ImagePlus imgSeq=(ImagePlus)obj[0];
			RootModel rmPred=(RootModel)obj[1];
			RootModel rmExpert=(RootModel)obj[2];

			//Export for each plant, for each lat : at each time, growing speed.
			int t=imgSeq.getStackSize();

			for(int i=0;i<5;i++) {
				double[][][]valsLatPred=rmPred.getLateralSpeedsAndDepthOverTime(t,i);
				double[][][]valsLatExpert=rmExpert.getLateralSpeedsAndDepthOverTime(t,i);
				double[]depthExp=getStartingDepth(valsLatExpert);
				double[]depthPred=getStartingDepth(valsLatPred);
				
				VitimageUtils.writeDoubleTabInCsvSimple(valsLatPred[0],"/home/rfernandez/Bureau/A_Test/RSML/OverallAnalyses/StatsSpeedOver_2/statsboite_"+boi+"_plant_"+i+"_ZPred.csv");
				VitimageUtils.writeDoubleTabInCsvSimple(valsLatExpert[0],"/home/rfernandez/Bureau/A_Test/RSML/OverallAnalyses/StatsSpeedOver_2/statsboite_"+boi+"_plant_"+i+"_ZExp.csv");
				VitimageUtils.writeDoubleTabInCsvSimple(valsLatPred[1],"/home/rfernandez/Bureau/A_Test/RSML/OverallAnalyses/StatsSpeedOver_2/statsboite_"+boi+"_plant_"+i+"_SPred.csv");
				VitimageUtils.writeDoubleTabInCsvSimple(valsLatExpert[1],"/home/rfernandez/Bureau/A_Test/RSML/OverallAnalyses/StatsSpeedOver_2/statsboite_"+boi+"_plant_"+i+"_SExp.csv");

				VitimageUtils.writeDoubleArray1DInFileSimple(depthPred,"/home/rfernandez/Bureau/A_Test/RSML/OverallAnalyses/StatsSpeedOver_2/statsboite_"+boi+"_plant_"+i+"_StartPred.csv");
				VitimageUtils.writeDoubleArray1DInFileSimple(depthExp,"/home/rfernandez/Bureau/A_Test/RSML/OverallAnalyses/StatsSpeedOver_2/statsboite_"+boi+"_plant_"+i+"_StartExp.csv");
			}
			System.out.println("Data production ok");
			System.out.println("Data production ok");
		}
	}

	public static double[]getStartingDepth(double[][][]vals){
		double[]ret=new double[vals[0].length];
		for(int j=0;j<vals[0].length;j++) {
			for(int i=0;i<vals[0][j].length;i++) {
				if(vals[0][j][i]!=0) {
					ret[j]=vals[0][j][i];
					break;
				}
			}
		}
		return ret;
	}
	
	
}
