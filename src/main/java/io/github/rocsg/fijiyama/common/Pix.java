/*
 * 
 */
package io.github.rocsg.fijiyama.common;

import java.io.Serializable;
import java.util.List;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import io.github.rocsg.fijiyama.common.VitimageUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class Pix.
 */
public class Pix implements Serializable{
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	
	/** The is skeleton. */
	public boolean isSkeleton=false;
	
	/** The dist out. */
	public double distOut=0;
	
	/** The previous. */
	public Pix previous=null;
	
	/** The distance to skeleton. */
	public double distanceToSkeleton=0;
	
	/** The time. */
	public double time=-1;
	
	/** The time out. */
	public double timeOut;
	
	/** The time. */
	public double timeHours=-1;
	
	/** The time out. */
	public double timeOutHours;
	
	/** The x. */
	public int x;
	
	/** The off X. */
	public int offX;
	
	/** The off Y. */
	public int offY;
	
	/** The y. */
	public int y;
	
	/** The stamp. */
	public int stamp=0;
	
	/** The way from prim. */
	public double wayFromPrim;

/** The dist. */
//	public double wayFromSkel;
	public double dist;
	
	/** The source. */
	public Pix source;
	
	/**
	 * Instantiates a new pix.
	 *
	 * @param x the x
	 * @param y the y
	 * @param dist the dist
	 */
	public Pix(int x, int y,double dist) {
		this.x=x;
		this.y=y;	
		this.dist=dist;
	}
	
	/**
	 * To string.
	 *
	 * @return the voxel sizes
	 */
	public String toString(){
		return"Pix="+(x+","+y+") , way="+wayFromPrim+" , dist="+dist+" time="+time);
	}
	
	/**
	 * Draw pathway.
	 *
	 * @param img the img
	 * @param listPixs the list pixs
	 * @return the image plus
	 */
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

