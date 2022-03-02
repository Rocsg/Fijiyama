package fr.cirad.image.rsml;

import fr.cirad.image.rsml.Root;
import fr.cirad.image.rsml.RootModel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class Tests {
	
	public static void main(String[]args) {
		ImageJ ij=new ImageJ();
		System.out.println("Echo world");
		FSR sr= (new FSR());
		sr.initialize();
		RootModel rm=new RootModel();
		Root r1=new Root(null, rm, "",1);
		r1.addNode(10,10,1,true);
		r1.addNode(20,20,1.1,false);
		r1.addNode(30,30,1.2,false);
		r1.addNode(40,40,1.3,false);
		r1.addNode(40,50,1.4,false);
		r1.addNode(40,60,1.5,false);
		r1.addNode(40,70,1.6,false);
		r1.addNode(40,80,1.4,false);
		r1.addNode(40,90,1.5,false);
		r1.addNode(40,100,1.6,false);
		r1.addNode(40,110,1.4,false);
		r1.addNode(40,120,1.5,false);
		r1.addNode(40,130,1.6,false);
		Root r2=new Root(null,rm,"",2);
		r2.addNode(40,50,1.7,true);
		r1.attachChild(r2);
		r2.attachParent(r1);
		r2.addNode(50,60,1.8,false);
		r2.addNode(60,70,1.9,false);
		r2.addNode(70,80,2.0,false);
		r2.addNode(80,90,2.1,false);
		r2.addNode(90,100,2.2,false);
		r2.addNode(100,120,2.3,false);
		
		rm.rootList.add(r1);//float dpi, org.w3c.dom.Node parentDOM, boolean common, Root parentRoot, RootModel rm, String origin)
		rm.rootList.add(r2);//float dpi, org.w3c.dom.Node parentDOM, boolean common, Root parentRoot, RootModel rm, String origin)
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/Sogho_joli.tif");
//		,double sigmaSmooth,boolean convexHull,boolean skeleton,int width
		rm.createGrayScaleImage(img,0,false,true,2).show(); 
		rm.writeRSML3D("/home/rfernandez/Bureau/test.rsml","TEST",true);
	}
}
