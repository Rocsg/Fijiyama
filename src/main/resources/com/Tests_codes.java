import org.itk.simple.Euler3DTransform;
import org.itk.simple.TranslationTransform;
import org.itk.simple.VersorRigid3DTransform;

import com.vitimage.MRUtils;
import com.vitimage.TransformUtils;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;



if(false) {//ITK basic testing
	ImagePlus imgRX=IJ.openImage("/home/fernandr/Bureau/Test/ITK/3_INTERMOD/RX.tif");
	ImagePlus imgGE=IJ.openImage("/home/fernandr/Bureau/Test/ITK/3_INTERMOD/ge3D.tif");
	ImagePlus imgT1_J70=IJ.openImage("/home/fernandr/Bureau/Test/ITK/4_INTERTIME/T1_J70_to_ref.tif");
	ImagePlus imgT1_J35=IJ.openImage("/home/fernandr/Bureau/Test/ITK/4_INTERTIME/T1_J35_to_ref.tif");
	ImagePlus imgT1_J0_600=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_600_32bit.tif");
	ImagePlus imgT1_J0_1200=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
	ImagePlus imgT1_J0_2400=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
	ImagePlus imgT2_J0_10000=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T2_J0_10000_32bit.tif");
	ImagePlus imgT2_J0_10000_rot=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T2_J0_10000_32bit_rotate_3degZ.tif");
	int =2;
	int levelMax=5;
	boolean coarsestDouble=true;
	double basis=2;
	int []levelsMin=new int[] {0,3,0};
	int []levelsMax=new int[] {0,5,0};
	registerImages(imgGE,imgRX,levelsMin,levelsMax,true,2,4,2,100,false);
}		



if(false) {//Scripting ITK Transform
	org.itk.simple.Transform tr1=new org.itk.simple.Transform();
	tr1.addTransform(new Euler3DTransform());
	tr1.addTransform(new TranslationTransform(3));
	System.out.println(tr1);
	System.out.println("\n\n\n\n\n\n\n\n\n\n\n");
	org.itk.simple.Transform tr2=new org.itk.simple.Transform();
	tr2.addTransform(new VersorRigid3DTransform());
	tr2.addTransform(new TranslationTransform(3));
	tr1.addTransform(tr2);
	System.out.println(tr1);
}




if(false) {//Test remove capillary
			ImageJ ij=new ImageJ();
			ImagePlus img1_J0_600=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_600_32bit.tif");
			ImagePlus img1_J0_1200=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
			ImagePlus img1_J0_2400=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T1_J0_1200_32bit.tif");
			ImagePlus img2_J0_10000=IJ.openImage("/home/fernandr/Bureau/Test/ITK/SEQS/T2_J0_10000_32bit.tif");
			ImagePlus imgInTest=img2_J0_10000;
			ImagePlus imgTest;
			imgTest=MRUtils.removeCapillary(imgInTest,false);
			imgInTest.show();
			imgTest.show();
		}


if(false) {//Test MRI CUrve explorer
	ImagePlus img1=IJ.openImage("/home/fernandr/Bureau/Test/IRM/T1seq.tif");//T1_seq_aligned.tif
	ImagePlus img2=IJ.openImage("/home/fernandr/Bureau/Test/IRM/T2seq.tif");//T2_seq_aligned.tif
	MRICurveExplorerWindow explorer=new MRICurveExplorerWindow(img1,img2);
}


if(false) {//Test detection inoculation point
	//ImageJ thisImageJ=new ImageJ();
	//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1_echo1_axis_aligned.tif");
	ImagePlus imgT1=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
	//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/RX_J218_to_ref.tif");
	imgT1.show();
	ImagePlus imgTT2 = new Duplicator().run(imgT1, 1,imgT1.getStackSize());
	new TransformUtils().detectInoculationPoint(imgTT2);
}


if(false) {//Test AutoAlign
	//System.out.println(TransformUtils.calculateAngle(0,10));//
//EASYImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
//MEDIUMImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/RX_J218_to_ref.tif");
//HARD		ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1_echo1_axis_aligned.tif");
	ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/RX_J210.tif");//Official test
	img.show();
	
	//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/T1_J35.tif");//Official test
	//ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/ge3D_J35.tif");//Official test OK
	//			ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/AutoAlign/T2_J35.tif");//Official test
	anna.notifyAlgo("Lancement alignement image","Pour la horde !","");
	anna.notifyStep("Ouverture image entree","RX_J210.tif");
	anna.storeImage(img,"Image test");
	runTransform3D(TR3D_4_AUTOALIGN_TR);
}



if(false) {//Test Remove capillary
	//ImageJ thisImageJ=new ImageJ();
	MRUtils mrUt=new MRUtils();
	ImagePlus im1=IJ.openImage("/home/fernandr/eclipse-workspace/MyIJPlugs/vitimage/src/test/imgs/Test_1-4_capToRemove.tif");
	im1.show();
	ImagePlus im2=mrUt.removeCapillary(im1,true);
	im2.show();
}




if(false) {//Test manual Alignment  TODO : probleme, on dirait : mal aligné avec Z
	//ImageJ thisImageJ=new ImageJ();
	MRUtils mrUt=new MRUtils();
	ImagePlus img=IJ.openImage("/home/fernandr/Bureau/Test/T1J70_to_ref.tif");
	imageChecking(img);
	img.show();
	runTransform3D(TR3D_3_MANALIGN_TR);
}

