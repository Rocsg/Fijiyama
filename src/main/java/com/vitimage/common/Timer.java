package com.vitimage.common;

import ij.IJ;

public class Timer {
	long initTime;
	public Timer() {
		initTime=System.currentTimeMillis();
	}

	public void print(String title) {
		System.out.println(title+" : "+VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");		
	}
	
	public void log(String title) {
		IJ.log(title+" : "+VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");				
	}
}
