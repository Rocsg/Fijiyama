/*
 * 
 */
package io.github.rocsg.fijiyama.common;

import ij.IJ;
import io.github.rocsg.fijiyama.common.VitimageUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class Timer.
 */
public class Timer {
	
	/** The init time. */
	long initTime;
	
	/** The mark time. */
	long markTime;
	
	/**
	 * Instantiates a new timer.
	 */
	public Timer() {
		initTime=System.currentTimeMillis();
	}

	/**
	 * Mark.
	 */
	public void mark() {
		markTime=System.currentTimeMillis();
	}
	
	/**
	 * Gather.
	 *
	 * @param title the title
	 * @return the string
	 */
	public String gather(String title) {
		double d=VitimageUtils.dou(0.001*(System.currentTimeMillis()-markTime));
		return (title+" : "+d+" s");
	}
	
	/**
	 * Gets the time.
	 *
	 * @return the time
	 */
	public double getTime() {
		return VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime));
	}

	/**
	 * Gets the inits the time.
	 *
	 * @return the inits the time
	 */
	public double getInitTime() {
		return VitimageUtils.dou(0.001*initTime);
	}

	/**
	 * Gets the absolute day.
	 *
	 * @return the absolute day
	 */
	public int getAbsoluteDay() {
		return (int)Math.floor((System.currentTimeMillis()/(1000.0*86400)));
	}
		
	/**
	 * Prints the.
	 *
	 * @param title the title
	 */
	public void print(String title) {
		System.out.println(title+" : "+VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");		
	}
	
	/**
	 * To string.
	 *
	 * @return the string
	 */
	public String toString() {
		return (VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");		
	}

	/**
	 * To cute string.
	 *
	 * @return the string
	 */
	public String toCuteString() {
		int seconds=(int) VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime));
		int hours=seconds/3600;
		seconds=seconds-3600*hours;
		int mins=seconds/60;
		seconds=seconds-60*mins;
		return (hours+" hours "+mins+" minutes "+seconds+" seconds");		
	}

	
	/**
	 * Log.
	 *
	 * @param title the title
	 */
	public void log(String title) {
		IJ.log(title+" : "+VitimageUtils.dou(0.001*(System.currentTimeMillis()-initTime))+" s");				
	}
}
