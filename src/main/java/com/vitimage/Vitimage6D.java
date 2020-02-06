package com.vitimage;

import java.io.File;

import com.vitimage.VitimageUtils.ComputingType;

import ij.ImageJ;

public class Vitimage6D {
	public final static String slash=File.separator;

	public static void main (String[]args) {
		String mainPath="/mnt/DD_COMMON/Traitements/Bouture6D/Source_data";
		File f=new File(mainPath);
		String []vitiNames=f.list();
		boolean computeOnlyAcquisitionsAndMaps=true;
		ComputingType computationType=ComputingType.COMPUTE_ALL;
		for(int i=vitiNames.length-2;i<vitiNames.length;i++) {
			
			for(int j=0;j<10;j++)System.out.println("");
			System.out.println("######################################################################################################################");
			System.out.println("######################################################################################################################");
			System.out.println("######################################################################################################################");
			System.out.println("######################################################################################################################");
			System.out.println("######################################################################################################################");
			System.out.println("########## Nouvelle bouture : "+vitiNames[i]);
			System.out.println("######################################################################################################################");
			String vitiPath=f.getAbsolutePath()+slash+vitiNames[i];

			ImageJ imageJ=new ImageJ();
			Vitimage5D viti = new Vitimage5D(VineType.CUTTING,vitiPath,vitiNames[i],computationType);
			viti.start(2);
			
			//			viti.getNormalizedHyperImage().show();
			VitimageUtils.waitFor(10000);
			imageJ.quit();
			viti.freeMemory();
			viti=null;
			VitimageUtils.waitFor(10000);
		}
		
	}
	
	public Vitimage6D() {
		// TODO Auto-generated constructor stub
		
	}

	
	
}
