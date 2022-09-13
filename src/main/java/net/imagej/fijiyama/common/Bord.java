/*
 * 
 */
package net.imagej.fijiyama.common;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The Class Bord.
 */
public class Bord implements Serializable{
	public Pix pix1;
	public Pix pix2;
	public double len;
	
	/**
	 * Instantiates a new bord.
	 *
	 * @param pix1 the pix 1
	 * @param pix2 the pix 2
	 */
	public Bord(Pix pix1,Pix pix2) {
		this.pix1=pix1;
		this.pix2=pix2;
		this.len=len(pix1,pix2);
	}
	
	/**
	 * Len.
	 *
	 * @param pix1 the pix 1
	 * @param pix2 the pix 2
	 * @return the double
	 */
	public double len(Pix pix1,Pix pix2) {
		if(pix1.x==pix2.x)return 1;
		if(pix1.y==pix2.y)return 1;
		return 1.414;
	}
	
	/**
	 * Gets the weight dist ext.
	 *
	 * @return the image plus
	 */
	public double getWeightDistExt() {
		return len*(pix1.dist+pix2.dist)/2.0;
	}
	
	/**
	 * Gets the weight euclidian.
	 *
	 * @return the weight euclidian
	 */
	public double getWeightEuclidian() {
		return len;
	}
}