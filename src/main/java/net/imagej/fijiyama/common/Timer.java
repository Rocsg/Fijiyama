package net.imagej.fijiyama.common;

import ij.IJ;
import net.imagej.fijiyama.common.VitimageUtils;

public class Timer {
	long initTime;
	long markTime;
	public Timer() {
		initTime=System.currentTimeMillis();
	}

	public void mark() {
		markTime=System.currentTimeMillis();
	}
	
	public String gather(String title) {
		double d=VitimageUtils.dou(0.001*(System.currentTimeMillis()-markTime));
		return (title+" : "+d+" s");
	}
	
	public double getTime() {
		return VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime));
	}

	public double getInitTime() {
		return VitimageUtils.dou(0.001*initTime);
	}

	public int getAbsoluteDay() {
		return (int)Math.floor((System.currentTimeMillis()/(1000.0*86400)));
	}
		
	public void print(String title) {
		System.out.println(title+" : "+VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");		
	}
	
	public String toString() {
		return (VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");		
	}

	public String toCuteString() {
		int seconds=(int) VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime));
		int hours=seconds/3600;
		seconds=seconds-3600*hours;
		int mins=seconds/60;
		seconds=seconds-60*mins;
		return (hours+" hours "+mins+" minutes "+seconds+" seconds");		
	}

	
	public void log(String title) {
		IJ.log(title+" : "+VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");				
	}
}
