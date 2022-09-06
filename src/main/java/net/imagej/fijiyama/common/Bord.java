package net.imagej.fijiyama.common;

import java.io.Serializable;

public class Bord implements Serializable{
	public Pix pix1;
	public Pix pix2;
	public double len;
	public Bord(Pix pix1,Pix pix2) {
		this.pix1=pix1;
		this.pix2=pix2;
		this.len=len(pix1,pix2);
	}
	public double len(Pix pix1,Pix pix2) {
		if(pix1.x==pix2.x)return 1;
		if(pix1.y==pix2.y)return 1;
		return 1.414;
	}
	public double getWeightDistExt() {
		return len*(pix1.dist+pix2.dist)/2.0;
	}
	public double getWeightEuclidian() {
		return len;
	}
}