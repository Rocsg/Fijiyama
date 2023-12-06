/*
 * 
 */
package io.github.rocsg.fijiyama.rsml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import org.itk.simple.AffineTransform;
import org.itk.simple.Image;
import org.itk.simple.ImageFileWriter;
import org.itk.simple.PixelIDValueEnum;
import org.itk.simple.SimpleITK;
import org.itk.simple.VectorUInt32;

import ij.plugin.frame.RoiManager;
import com.opencsv.CSVReader;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.frame.SyncWindows;
import io.github.rocsg.fijiyama.common.ItkImagePlusInterface;
import io.github.rocsg.fijiyama.common.Timer;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.ItkTransform;

// TODO: Auto-generated Javadoc
/**
 * The Class Tests.
 */
public class Tests {
	
	/**
	 * The main method.
	 *
	 * @param args the args
	 */
/*	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		sync();
		//		viewTimeLapse();
		//		viewModel();
	}
	*/
	
	
	public static void doDataInTestPablo() {
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/A_Test/Fijiyama_track_issues/20230508_Pablo/Reproduction/Reprod_Vanilla/Input_data/imgRef.tif");
		
	}
	
	
	public static void testAllItkModif(){
		//Open an image located at /home/rfernandez/Bureau/test.tif
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test.tif");
		//Convert by using the old conversion method, and time the duration
		Timer t=new Timer();
		t.print("Starting measuring old behaviour");
		Image a=ItkImagePlusInterface.imagePlusToItkImageOld(img);
		ImagePlus b1=ItkImagePlusInterface.itkImageToImagePlusOld(a);
	
		t.print("Time for old behaviour");

		//Convert by using the new conversion method, and time the duration
		//setFlag();
		t=new Timer();
		t.print("Starting measuring new behaviour");
		a=ItkImagePlusInterface.imagePlusToItkImageNew(img);
		ImagePlus b2=ItkImagePlusInterface.itkImageToImagePlusNew(a);
		t.print("Time for new behaviour");		


		b1.show();
		b2.show();
	}


	public static void testConfig2() {
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test.tif");
		img.show();
		Image image = SimpleITK.readImage("/home/rfernandez/Bureau/test.tif", PixelIDValueEnum.sitkUnknown);
		ImageFileWriter writer = new ImageFileWriter();
        writer.setFileName("/home/rfernandez/Bureau/test2.tif");
        writer.execute(image);
		ImagePlus b=IJ.openImage("/home/rfernandez/Bureau/test2.tif");
		b.show();
	}
	
	
	public static double[] testConfiguration(){
		IJ.showMessage("Now starting a speedup test. Please click OK, then wait a few seconds.");

		//Create an empty stack of 200 x 200 x 200
		ImagePlus img=IJ.createImage("Test", "8-bit black", 200, 200, 200);


		//Test old behaviour
		Timer t=new Timer();
		t.print("Starting measuring old behaviour");
		for(int i=0;i<10;i++) {
			Image a=ItkImagePlusInterface.imagePlusToItkImageOld(img);
			ImagePlus b=ItkImagePlusInterface.itkImageToImagePlusOld(a);
		}
		t.print("Time for old behaviour");
		double t0=t.getTime();

		//Test new behaviour
		t=new Timer();
		for(int i=0;i<10;i++) {
			String s="/home/rfernandez/Bureau/temp.tif";
			IJ.saveAsTiff(img, s);
			Image image = SimpleITK.readImage(s, PixelIDValueEnum.sitkUnknown);

			ImageFileWriter writer = new ImageFileWriter();
	        writer.setFileName(s);
	        writer.execute(image);
			ImagePlus b=IJ.openImage(s);
		}
		t.print("Time for new behaviour");
		double t1=t.getTime();

		return new double[]{t0,t1};
	}

	public static void main(String[]args) throws Exception {
		ImageJ ij=new ImageJ();
		Timer t=new Timer();
		for(double i=0;i<(1000*1000*100);i++) {
			//int rand=random.nextInt();
		}
		t.print("");
	}
//		testAllItkModif();
		
		

	public static void dot(){

		Timer t=new Timer();
		t.print("start");
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/tt.tif");
		t.print("opened");
		for(int i=0;i<10;i++) {
			Image a=ItkImagePlusInterface.imagePlusToItkImage(img);
			ImagePlus b=ItkImagePlusInterface.itkImageToImagePlus(a);
		}
		t.print("converted1");
		for(int i=0;i<10;i++) {
			IJ.saveAsTiff(img, "/home/rfernandez/Bureau/tt2.tif");
			Image image = SimpleITK.readImage("/home/rfernandez/Bureau/tt2.tif", PixelIDValueEnum.sitkUnknown);
			ImageFileWriter writer = new ImageFileWriter();
	        writer.setFileName("/home/rfernandez/Bureau/tt2.tif");
	        writer.execute(image);
			ImagePlus b=IJ.openImage("/home/rfernandez/Bureau/tt2.tif");
;		}
		t.print("converted2");

		//ItkTransform trans = ;
		
		/*
		doDataInTestPablo();
		String dirExp="/media/rfernandez/DATA_RO_A/Aerenchyme/Dataset_Kaue_Novembre";
		computeSummaryOfLacuneData(dirExp,dirExp+"/Results_aerenchyme_measurements.csv",false);
		/home/rfernandez/Bureau/A_Test/Aerenchyme
		String dirExp="/home/rfernandez/Bureau/A_Test/Aerenchyme/Test_Kevin";	
		computeSummaryOfLacuneData(dirExp,dirExp+"/Results_aerenchyme_measurements.csv",true);
*/
		return;
	}
	
	
	
	
	
	
	
	
	
	
	public static void computeSummaryOfLacuneData(String dirExp,String pathToOutputCsv,boolean newWay) {
		String dirSource=new File(dirExp,"1_Source").getAbsolutePath();
		String dirRoi=new File(dirExp,"3_CellRoi").getAbsolutePath();
		String dirCortex=new File(dirExp,"2_CortexRoi").getAbsolutePath();
		String dirLac=new File(dirExp,"5_LacunesIndices").getAbsolutePath();
		String[]imgNames=new File(dirSource).list();
		ArrayList<String>ar=new ArrayList<String>();
		for(String s:imgNames)ar.add(s);
		ar.sort(null);
		int N=ar.size();
		String[][]finalTab=new String[N+1][4];
		finalTab[0]=new String[] {"0_PARAM_ImgName","1_PARAM_INFO","2_PARAM_Geno","3_PARAM_Management","4_PARAM_Depth","5_PARAM_Repetition","6_PARAM_Magnification","7_PHENE_cortex_surf(pix)","8_PHENE_stele_surf(pix)","9_PHENE_lacune_surf(pix)","10_PHENE_lacune_ratio(percent)"};
		Timer t=new Timer();
		for(int i=0;i<N;i++) {
			IJ.log("\n"+i+" / "+N);
			String[]tab=new String[11];
			tab[0]=ar.get(i);
			String baseName=ar.get(i).split(".tif")[0];
			IJ.log(baseName);
			if(!new File(dirLac,baseName+".tif.csv").exists()) {
				if(baseName.split("_")[0].contains("SAHEL")||baseName.split("_")[0].contains("IRAT")||baseName.split("_")[0].contains("KINA")) {
					tab[1]="Genotype was not selected";
				}
				else  tab[1]="Lacune annotations were not provided";
				tab[2]=baseName.split("_")[0];
				tab[3]=baseName.split("_")[1+(newWay ? 1 : 0)];
				tab[4]=baseName.split("_")[2+(newWay ? 1 : 0)];
				tab[5]=baseName.split("_")[3+(newWay ? 1 : 0)];
				tab[6]=""+getMagnification(dirSource+"/"+baseName+".tif");
				for(int iii=7;iii<11;iii++)tab[iii]="NA";
				finalTab[i+1]=tab;
				continue;
			}
			tab[1]="No info";
			System.out.println("Basename="+baseName);
			tab[2]=baseName.split("_")[0];
			tab[3]=baseName.split("_")[1+(newWay ? 1 : 0)];
			tab[4]=baseName.split("_")[2+(newWay ? 1 : 0)];
			tab[5]=baseName.split("_")[3+(newWay ? 1 : 0)];
			tab[6]=""+getMagnification(dirSource+"/"+baseName+".tif");
			

			double totCortex=0;
			double totLacune=0;
			
			ImagePlus img=IJ.openImage(dirSource+"/"+baseName+".tif");			
			//RoiManager rm=new RoiManager();
			IJ.open(dirRoi+"/"+baseName+".tif.zip");
			RoiManager rm=RoiManager.getInstance();
			int nR=rm.getCount();

			boolean []isLacune=new boolean[nR];
			String[][]tabLac=VitimageUtils.readStringTabFromCsv(dirLac+"/"+baseName+".tif.csv");
			for(int l=1;l<tabLac.length;l++) {
				int index=Integer.parseInt(tabLac[l][1])-1;
				if(index>=isLacune.length) {
					System.out.println("Alerte : "+index);
				}
				else isLacune[index]=true;
			}
			int totNbCell=0;
			int totNbLac=0;
			for(int r=0;r<nR;r++) {
				totNbCell++;
				double surf=getRoiSurface(rm.getRoi(r));
				totCortex+=surf;
				if(isLacune[r]) {
					totLacune+=surf;
					totNbLac++;
				}
				//System.out.println("After roi "+r+" totCortex="+totCortex+" lacune="+totLacune);
			}
			
			
			rm.reset();
			IJ.open(dirCortex+"/"+baseName+".tifstele_out.zip");
			double surfIn=getRoiSurface(rm.getRoi(0));
			//IJ.open(dirCortex+"/"+baseName+".tifcortex_out.zip");
			//double surfOut=VitimageUtils.getRoiSurface(rm.getRoi(1));
			tab[7]=""+(totCortex);
			tab[8]=""+(surfIn);
			tab[9]=""+totLacune;
			System.out.println("\n"+baseName+"Coucou, j'ai calcule ca : ");
			System.out.println("Tot nb Cell="+totNbCell);
			System.out.println("Tot nb Lacune="+totNbLac);
			System.out.println("Surf tot cell="+totCortex);
			System.out.println("Surf tot lacune="+totLacune);
			if(totLacune>totCortex) {
				System.out.println("WARNING  ! BIG PROBLEM \n\n\nWARNING BIG PRROBLEM\n\n");
				VitimageUtils.waitFor(10000);
			}
			tab[10]=""+dou(100*totLacune/(totCortex));
			finalTab[i+1]=tab;
			rm.reset();
		
		}
		writeStringTabInCsv2(finalTab, pathToOutputCsv);
		IJ.showMessage("J ai fini");
	}

	public static void writeStringTabInCsv2(String[][]tab,String fileName) {
	//	System.out.println("Impression de tableau de taille "+tab.length+" x "+tab[0].length);
		try { 
			PrintStream l_out = new PrintStream(new FileOutputStream(fileName)); 
			for(int i=0;i<tab.length;i++) {
				for(int j=0;j<tab[i].length;j++) {
					l_out.print(tab[i][j]+(j<tab[i].length-1 ? "," : "")); 
				}
				l_out.println(""); 
			}
			l_out.flush(); 
			l_out.close(); 
			l_out=null; 
		} 
		catch(Exception e){System.out.println(e.toString());} 
	}
	
	public static double dou(double d){
		if(d<0)return (-dou(-d));
		if (d<0.0001)return 0;
		return (double)(Math.round(d * 10000)/10000.0);
	}


	public static double getRoiSurface(Roi r) {
        int x0=(int)Math.floor(r.getBounds().getMinX());
        int x1=(int)Math.floor(r.getBounds().getMaxX());
        int y0=(int)Math.floor(r.getBounds().getMinY());
        int y1=(int)Math.floor(r.getBounds().getMaxY());
        int inter=0;
        for(int x=x0;x<=x1;x++) {
            for(int y=y0;y<=y1;y++) {
                if(r.contains(x, y))inter++;
            }
        }
        return inter;
   	
    }

	
	/** Some Csv helpers   --------------------------------------------------------------------------------------   */
	public static String[][]readStringTabFromCsv(String csvPath){
	    try {
	    	CSVReader reader = new CSVReader(new FileReader(csvPath)); 
			java.util.List<String[]> r = reader.readAll();
			String[][]ret=new String[r.size()][];
			for(int index=0;index<r.size();index++)ret[index]=r.get(index);
			reader.close();
			return ret;
	    }
	    catch(Exception e) {
	    	System.out.println("Got an exception during opening "+csvPath);
    	}
	    return null;
}

	
	public static double getMagnification(String path) {
		
		ImagePlus img=IJ.openImage(path);
		double d=0;
		if(img.getInfoProperty().split("Objective Correction").length>1) {
			d=Double.parseDouble( img.getInfoProperty().split("Objective Correction")[1].split("NominalMagnification")[1].split("\"")[1]);
			System.out.println("Detected : "+d);
			return d;
		}
		else d=20;
		System.out.println("Guessed : "+d);
		return d;
	}
		

	
	
	
	

	public static void sync() {
		ImageJ ij=new ImageJ();
		String imgPath="/home/rfernandez/Bureau/A_Test/RootSystemTracker/TestSplit/Processing_of_TEST1230403-SR-split/230403SR055/11_stack.tif";
		ImagePlus img1=IJ.openImage(imgPath);
		img1.setTitle("Img 1");
		ImagePlus img2=IJ.openImage(imgPath);
		img2.setTitle("Img 2");
		img1.show();
		img2.show();
		
		SyncWindows syn=new SyncWindows();
		syn.run("");
		VitimageUtils.waitFor(1000);
		Vector v=new Vector();
		v.addElement(WindowManager.getIDList()[0]);
		v.addElement(WindowManager.getIDList()[1]);
		//syn.addWindows(v);
		img1.getWindow().getCanvas().addMouseMotionListener(syn);
		img2.getWindow().getCanvas().addMouseMotionListener(syn);
	}
	
	
	/**
	 * View time lapse.
	 */
	public static void viewTimeLapse() {
		String rsmlPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_00002/Expertized_models/4_2_Model_0055.rsml";
		String imagePath="/home/rfernandez/Bureau/A_Test/RSML/1_Registered/ML1_Boite_00002.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus stackReg=IJ.openImage(imagePath);
		double[]observationTimes=new double[200];
		for(int i=0;i<observationTimes.length;i++)observationTimes[i]=(i+1)/10;
		ImagePlus res=rm.createGrayScaleImageTimeLapse(stackReg, observationTimes,new double[] {2,1},0.5); 
		res.show();
	}
	
	/**
	 * View expert.
	 */
	public static void viewExpert() {
		String rsmlPath="/home/rfernandez/Bureau/A_Test/RSML/Retour Amandine/ML1_Boite_00002/Expertized_models/4_2_Model_0055.rsml";
		String imagePath="/home/rfernandez/Bureau/A_Test/RSML/1_Registered/ML1_Boite_00002.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus stackReg=IJ.openImage(imagePath);
		ImagePlus res=RootModel.projectRsmlOnImage(rm,stackReg,1,2,1,true,true,false);
		res.show();		
	}

	/**
	 * View model.
	 */
	public static void viewModel() {
		String rsmlPath="/home/rfernandez/Bureau/A_Test/RSML/4_RSML_BACKTRACK/ML1_Boite_00008.rsml";
		String imagePath="/home/rfernandez/Bureau/A_Test/RSML/1_Registered/ML1_Boite_00008.tif";
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		ImagePlus stackReg=IJ.openImage(imagePath);
		ImagePlus res=RootModel.projectRsmlOnImage(rm,stackReg,1,2,1,true,false,true);
		res.show();				
		stackReg.show();
	}
	
}
