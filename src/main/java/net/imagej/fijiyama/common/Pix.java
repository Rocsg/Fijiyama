package net.imagej.fijiyama.common;

import java.io.Serializable;
import java.util.List;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.imagej.fijiyama.common.VitimageUtils;

public class Pix implements Serializable{
	private static final long serialVersionUID = 1L;
	public boolean isSkeleton=false;
	public double distOut=0;
	public Pix previous=null;
	public double distanceToSkeleton=0;
	public double time=-1;
	public double timeOut;
	public int x;
	public int offX;
	public int offY;
	public int y;
	public int stamp=0;
	public double wayFromPrim;
//	public double wayFromSkel;
	public double dist;
	public Pix source;
	public Pix(int x, int y,double dist) {
		this.x=x;
		this.y=y;	
		this.dist=dist;
	}
	public String toString(){
		return"Pix="+(x+","+y+") , way="+wayFromPrim+" , dist="+dist+" time="+time);
	}
	public static ImagePlus drawPathway(ImagePlus img,List<Pix> listPixs) {
		ImagePlus res=VitimageUtils.convertToFloat(img);
		ImageProcessor ip=img.getStack().getProcessor(1);
		for(int i=0;i<listPixs.size();i++) {
			ip.set(listPixs.get(i).x, listPixs.get(i).y,255);
		}
		ImagePlus imRes=new ImagePlus("",ip);
		return imRes;
	}

	
}

