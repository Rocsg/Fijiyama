package com.vitimage;

import java.io.File;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;

public class IntelligentFile extends File {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ArrayList<String>indexes;
	boolean isDirectoryOfIRMSlices=false; //true if it's a directory with no directory in it
	boolean isBionanoV2T2SeqDirectory=false; //true if it's a directory with no directory in it
	boolean isBionanoV2T1TrDirectory=false; //true if it's a directory with no directory in it
	boolean isBionanoV2T1SeqSlices=false; //true if it's a directory with no directory in it
	Date date=null;
	public IntelligentFile(String pathname) {
		super(pathname);
		indexes=new ArrayList<String>();
		// TODO Auto-generated constructor stub
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String ret= "IntelligentFile [Path="+this.getAbsolutePath()+", indexes= |";
		if(indexes!=null)for(int i=0;i<indexes.size();i++)ret+=" "+this.indexes.get(i)+" |";
		ret+=" , date=" + date + "]";
		return ret;
	}

	public ArrayList<String> getIndexes() {
		return indexes;
	}

	public void setIndexes(ArrayList<String> indexes) {
		this.indexes = indexes;
	}

	public void addIndex(String strIndex) {
		if(! isPresent(strIndex) ) {
			if(indexes==null)indexes=new ArrayList<String>();
			indexes.add(strIndex);
		}
	}
	
	public boolean isPresent(String strIndex) {
		if(indexes==null)return false;
		for(int i=0;i<indexes.size();i++)if (indexes.get(i).equals(strIndex))return true;
		return false;
	}
	
	public void addIndexes(ArrayList<String>comingIndexes) {
		if(comingIndexes==null)return;
		for(int i=0;i<comingIndexes.size();i++)addIndex(comingIndexes.get(i));
	}

	public void detectIndexesInName(String []strOfInterest) {
		if(strOfInterest==null)return;
		for(int i=0;i<strOfInterest.length;i++)if(this.getName().indexOf(strOfInterest[i])>=0 )this.addIndex(strOfInterest[i]);
	}
	
	
	public IntelligentFile(URI uri) {
		super(uri);
		// TODO Auto-generated constructor stub
	}

	public IntelligentFile(String parent, String child) {
		super(parent, child);
		// TODO Auto-generated constructor stub
	}

	public IntelligentFile(File parent, String child) {
		super(parent, child);
		// TODO Auto-generated constructor stub
	}

}
