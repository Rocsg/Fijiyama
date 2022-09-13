/*
 * 
 */
package net.imagej.fijiyama.rsml;

import java.io.File;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imagej.fijiyama.common.*;
import net.imagej.fijiyama.fijiyamaplugin.Fijiyama_GUI;
import net.imagej.fijiyama.rsml.RSMLNoGUI;

// TODO: Auto-generated Javadoc
/**
 * The Class Test_RSMLViewer.
 */
public class Test_RSMLViewer {

	/**
	 * Instantiates a new test RSML viewer.
	 */
	public Test_RSMLViewer() {}
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[]args) {		
		ImageJ ij=new ImageJ();
		//stuff();
		//		viewThingsSecondReport();
		//viewThingsThirdReport();
		viewThingsFifthReport();
	}
		
	
	/**
	 * Stuff.
	 */
	public static void stuff() {
		String in="/home/rfernandez/Bureau/A_Test/Input";
		String out="/home/rfernandez/Bureau/A_Test/Output";
		Fijiyama_GUI.startBatchRsml(in,out,false);
	}

	/**
	 * View things fifth report.
	 */
	public static void viewThingsFifthReport() {
		String subjectName="20200826-AC-PIP_azote_Seq 7_Boite 00021_IdentificationFailed-Visu";
		String dataRsml1="/home/rfernandez/Bureau/A_Test/Racines/Tests/test_02_morgan_labels/input/20200826-AC-PIP_azote_Seq_1_Boite_00001_IdentificationFailed-Visu.rsml";
		String dataRsml2="/home/rfernandez/Bureau/A_Test/Racines/Tests/test_00_rootnav/output/20200826-AC-PIP_azote_Seq_1_Boite_00001_IdentificationFailed-Visu.rsml";
		String dataRsml3="/home/rfernandez/Bureau/A_Test/Racines/Tests/test_01_morgan/output/20200826-AC-PIP_azote_Seq_1_Boite_00001_IdentificationFailed-Visu.rsml";
		String dataRsml4="/home/rfernandez/Bureau/A_Test/Racines/Tests/test_02_morgan_labels/output/20200826-AC-PIP_azote_Seq_1_Boite_00001_IdentificationFailed-Visu.rsml";
		String dataRsml5="/home/rfernandez/Bureau/A_Test/Racines/Tests/test_11_morgan_60/output/20200826-AC-PIP_azote_Seq_1_Boite_00001_IdentificationFailed-Visu.rsml";
		String dataRsml6="/home/rfernandez/Bureau/A_Test/Racines/Tests/test_11_morgan_1000/output/20200826-AC-PIP_azote_Seq_1_Boite_00001_IdentificationFailed-Visu.rsml";
		String dataImg="/home/rfernandez/Bureau/A_Test/Racines/Tests/test_02_morgan_labels/input/20200826-AC-PIP_azote_Seq_1_Boite_00001_IdentificationFailed-Visu.jpg";

		viewSuperposition(dataImg,dataRsml1,"Given");
		viewSuperposition(dataImg,dataRsml2,"Result rootnav");
		/*viewSuperposition(dataImg,dataRsml3,"Result morgan 1");
		viewSuperposition(dataImg,dataRsml4,"Result morgan 2");
		viewSuperposition(dataImg,dataRsml5,"Result morgan 60");
		*/viewSuperposition(dataImg,dataRsml6,"Result morgan 1000");
		
	}
	
	/**
	 * View things third report.
	 */
	public static void viewThingsThirdReport() {
		String imgName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Train/20200826-AC-PIP_azote_Seq 6_Boite 00004_IdentificationFailed-Visu.jpg";
		String rsmlMorganName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Results_Train_Custom/20200826-AC-PIP_azote_Seq 6_Boite 00004_IdentificationFailed-Visu.rsml";
		String rsmlMichaelName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Results_Train_Official/20200826-AC-PIP_azote_Seq 6_Boite 00004_IdentificationFailed-Visu.rsml";		

		viewSuperposition(imgName,rsmlMorganName,"Reconstruction réseau Morgan");
		viewSuperposition(imgName,rsmlMorganName,"Reconstruction réseau Morgan");
		
	}

	
	
	/**
	 * View things fourth report.
	 */
	public static void viewThingsFourthReport() {
		ImagePlus[]mik=new ImagePlus[6];
		ImagePlus[]morg=new ImagePlus[6];
		ImagePlus[]mikNNOut=new ImagePlus[6];
		ImagePlus[]morgNNOut=new ImagePlus[6];
		for(int i=1;i<7;i++) {
			String imgName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Train/20200826-AC-PIP_azote_Seq "+i+"_Boite 00004_IdentificationFailed-Visu.jpg";
			String rsmlMorganName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Results_Train_Custom/20200826-AC-PIP_azote_Seq "+i+"_Boite 00004_IdentificationFailed-Visu.rsml";
			String rsmlMichaelName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Results_Train_Official/20200826-AC-PIP_azote_Seq "+i+"_Boite 00004_IdentificationFailed-Visu.rsml";		
			String morganNNOutputName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Results_Train_Custom/20200826-AC-PIP_azote_Seq "+i+"_Boite 00004_IdentificationFailed-Visu_Color_output.png";
			String mikNNOutputName="/home/rfernandez/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Results_Train_Official/20200826-AC-PIP_azote_Seq "+i+"_Boite 00004_IdentificationFailed-Visu_Color_output.png";
			
			viewSuperposition(imgName,rsmlMorganName,"Reconstruction réseau Morgan");
			morg[i-1]=IJ.getImage();
			morg[i-1].hide();
			morgNNOut[i-1]=IJ.openImage(morganNNOutputName);
			viewSuperposition(imgName,rsmlMichaelName,"Reconstruction réseau Michael");
			mik[i-1]=IJ.getImage();
			mik[i-1].hide();
			mikNNOut[i-1]=IJ.openImage(mikNNOutputName);
			
		}
		ImagePlus morgSerie=VitimageUtils.slicesToStack(morg);
		ImagePlus mikSerie=VitimageUtils.slicesToStack(mik);
		ImagePlus morgNNSerie=VitimageUtils.slicesToStack(morgNNOut);
		ImagePlus mikNNSerie=VitimageUtils.slicesToStack(mikNNOut);
				
		morgSerie.setTitle("Serie Boite 00004 etudiée avec réseau Morgan");
		morgSerie.show();
		morgNNSerie.setTitle("Serie Boite 00004 sortie réseau Morgan");
		morgNNSerie.show();

		mikSerie.setTitle("Serie Boite 00004 etudiée avec réseau Nottingham");		
		mikSerie.show();
		mikNNSerie.setTitle("Serie Boite 00004 sortie réseau Nottingham");
		mikNNSerie.show();
	}
	
	
	
	/**
	 * View things second report.
	 */
	public static void viewThingsSecondReport() {

		//Data training Morgan
		int sequence=6;
		String boite="00004";
		String dirMorgan="/home/fernandr/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan/Train/";
		String rsmlMorganName=dirMorgan+"20200826-AC-PIP_azote_Seq "+sequence+"_Boite "+boite+"_IdentificationFailed-Visu.rsml";
		viewSuperposition(rsmlMorganName, "Morgan_"+sequence+"_"+boite);
		//Data training Michael
		String box="3547"; // From 3526 to 3575 test    3576 3599 v   3600 3796 tr: arabido" Une grosse 3546
		String dirMichael="/home/fernandr/Bureau/Traitements/Racines/Data_rootnav/";
		String rsmlMichaelName=dirMichael+box+"/image_"+box+".rsml";
		viewSuperposition(rsmlMichaelName, "Michael_"+box);				
	}
	
		
	/**
	 * View things first report.
	 */
	public static void viewThingsFirstReport() {	
		
		int testCase=1;
		int seq=6;
		String mainPath="/home/fernandr/Bureau/A_Test/BPMP/Reproduction_01_avec_reseau_morgan";
		String imgPath="";
		String rsmlPath="";
		if(testCase==0) {//Training :
			//view superposition of initial root and initial rsml result
			imgPath=mainPath+"/Train/20200826-AC-PIP_azote_Seq "+seq+"_Boite 00004_IdentificationFailed-Visu.jpg";
			rsmlPath=mainPath+"/Train/20200826-AC-PIP_azote_Seq "+seq+"_Boite 00004_IdentificationFailed-Visu.rsml";
			viewSuperposition(imgPath, rsmlPath,"Source");

			//view superposition of initial root and final rsml result Rootnav
			imgPath=mainPath+"/Train/20200826-AC-PIP_azote_Seq "+seq+"_Boite 00004_IdentificationFailed-Visu.jpg";
			rsmlPath=mainPath+"/Results_Train_Official/20200826-AC-PIP_azote_Seq "+seq+"_Boite 00004_IdentificationFailed-Visu.rsml";
			viewSuperposition(imgPath, rsmlPath,"Result official");

			//view superposition of initial root and final rsml result Morgan
			imgPath=mainPath+"/Train/20200826-AC-PIP_azote_Seq "+seq+"_Boite 00004_IdentificationFailed-Visu.jpg";
			rsmlPath=mainPath+"/Results_Train_Custom/20200826-AC-PIP_azote_Seq "+seq+"_Boite 00004_IdentificationFailed-Visu.rsml";
			viewSuperposition(imgPath, rsmlPath,"Result_Custom");

		
		}
		
		if(testCase==1) {//Test :
			int boite=33;
			//view superposition of initial root and roo rsml result
			imgPath=mainPath+"/Test/20200826-AC-PIP_azote_Seq 6_Boite 000"+boite+"_IdentificationFailed-Visu.jpg";
			rsmlPath=mainPath+"/Results_Test_Official/20200826-AC-PIP_azote_Seq 6_Boite 000"+boite+"_IdentificationFailed-Visu.rsml";
			viewSuperposition(imgPath, rsmlPath,"Official");
			
			//view superposition of initial root and roo rsml result
			imgPath=mainPath+"/Test/20200826-AC-PIP_azote_Seq 6_Boite 000"+boite+"_IdentificationFailed-Visu.jpg";
			rsmlPath=mainPath+"/Results_Test_Custom/20200826-AC-PIP_azote_Seq 6_Boite 000"+boite+"_IdentificationFailed-Visu.rsml";
			viewSuperposition(imgPath, rsmlPath,"Custom");

		}
		


	
	}

	
	/**
	 * View superposition.
	 *
	 * @param rsmlPath the rsml path
	 * @param title the title
	 */
	public static void viewSuperposition(String rsmlPath,String title) {
		String nameImg=new String(rsmlPath);
		nameImg=nameImg.replace(".rsml",".tif");
		if(!new File(nameImg).exists()) {
			nameImg=new String(rsmlPath);
			nameImg=nameImg.replace(".rsml",".jpg");
		}
		viewSuperposition(nameImg, rsmlPath,"Source");
	}
	
	
	
	/**
	 * View superposition.
	 *
	 * @param imgPath the img path
	 * @param rsmlPath the rsml path
	 * @param title the title
	 */
	public static void viewSuperposition(String imgPath,String rsmlPath,String title) {
		boolean realWidth=true;
		boolean makeConvexHull=false;
		double ratioColor=0.8;
		double ratioNB=0.7;
		int lineWidth=1;
		int testCase=0;
		
		
		if(!new File(imgPath).exists()) {
			System.out.println("Wrong path to img : "+imgPath);
			return;
		}
		if(!new File(rsmlPath).exists()) {
			System.out.println("Wrong path to rsml : "+rsmlPath);
			return;
		}
		
		IJ.log("Here");
		IJ.log(imgPath);
		IJ.log(rsmlPath);
		ImagePlus img;
		img=new RSMLNoGUI().getPreview(imgPath,rsmlPath,lineWidth,realWidth,makeConvexHull,ratioColor);
		IJ.log("THere");
		img.show();
		ImagePlus img2=IJ.openImage(imgPath);
		IJ.run(img2, "Multiply...", "value="+ratioNB);
		img2.show();
		System.out.println("Here 1");
		IJ.run(img2, "RGB Color", "");
		System.out.println("Here 2");
		System.out.println("Here 3");
		IJ.run("Merge Channels...",
				"c1=["+img.getTitle()+
				"] c4=["+img2.getTitle()+"] create");
		System.out.println("Here 4");
		ImagePlus img3=IJ.getImage();
		IJ.run(img3, "Stack to RGB", "");
		img3.close();
		img2=IJ.getImage();
		img2.setTitle(title);
	}

		/**
		 * Pouet.
		 */
		public static void pouet() {
			String mainPath="/home/fernandr/Bureau/A_Test/BPMP/Reproduction_03_training_jean_zay";
			String t=mainPath+"/valid";
			File f=new File(t);
			String []ss=f.list();
			for (String s : ss) {
				if(s.contains(".jpg")) {
					System.out.println("Processing "+s);
					ImagePlus img=IJ.openImage(new File(t,s).getAbsolutePath());
					IJ.run(img, "RGB Color", "");
					IJ.save(img,new File(t,s).getAbsolutePath());					
				}
/*				else if(s.contains(".jpg")) {
					ImagePlus img=IJ.openImage(new File(t,s).getAbsolutePath());
					IJ.run(img, "RGB Color", "");
					IJ.saveAsTiff(img,new File(t,s).getAbsolutePath());
				}*/
			}
			System.exit(0);
		}
}



/*
IJ.run(imp, "32-bit", "");
IJ.run("Merge Channels...", "c1=[20200826-AC-PIP_azote_Seq 3_Boite 00001_IdentificationFailed-Visu.rsml] c4=[20200826-AC-PIP_azote_Seq 3_Boite 00001_IdentificationFailed-Visu.jpg] create");
IJ.run(imp, "RGB Color", "");
ImagePlus imp2 = CompositeConverter.makeComposite(imp);
IJ.run("In [+]", "");
IJ.run("In [+]", "");
IJ.run("In [+]", "");
IJ.run("In [+]", "");
IJ.run("Out [-]", "");
IJ.run("In [+]", "");
*/