/*
 * 
 */
package io.github.rocsg.fijiyama.common;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.StackConverter;
import io.github.rocsg.fijiyama.common.VitimageUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class MostRepresentedFilter.
 */
public class MostRepresentedFilter {
	
	/** The max nb classes. */
	public static int MAX_NB_CLASSES=256;
	
	/** The high distance. */
	public static double HIGH_DISTANCE=10E8;
	
	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main (String[]args) {
		ImageJ ij=new ImageJ();
		ImagePlus img=IJ.openImage("/home/rfernandez/Bureau/test.tif");
		img.setDisplayRange(0, 255);
		new StackConverter(img).convertToGray8();
		ImagePlus img2=mostRepresentedFilteringWithRadiusWithBGExclusion(img, 4, false, 255, false, 0);
		img.setTitle("Before");
		img.setDisplayRange(-1,22);
		img.show();
		img2.setTitle("After");
		img2.setDisplayRange(-1,22);
		img2.show();
		VitimageUtils.waitFor(5000000);
		/*System.out.println(mostRepresentedValue(
				new int[] {10,11,12,12,14,11,15,16,13,18,19},
				new double[] {0,0,1,1,0,0,0,0,1,0,0}
				));
		
		VitiDialogs.getYesNoUI("Select segmentation image to filter");
		ImagePlus imgIn=IJ.openImage();
		double ray=VitiDialogs.getDoubleUI("Select radius of 2D structuring element", "Radius (in pixels)", 2);
		if(ray<1) {
			for(double ra=1;ra<=7 ; ra+=0.3) {
				printLookup(ra,buildLookup(ra));				
			}
			return;
		}
		boolean [][]lookup=buildLookup(ray);
		double [][]distances=buildDistances(ray);
		printLookup(ray,lookup);				
		ImagePlus imgOut=mostRepresentedFilteringWithRadius(imgIn,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgOut=mostRepresentedFilteringWithRadius(imgOut,lookup,distances);
		imgIn.show();
		imgOut.show();
		imgOut.setTitle(imgIn.getTitle()+"_filtered");
		*/
	}

	/**
	 * Most represented value.
	 *
	 * @param vals the vals
	 * @param distances the distances
	 * @param MAX_NB_CLASSES the max nb classes
	 * @return the int
	 */
	public static int mostRepresentedValue(int []vals, double []distances,int MAX_NB_CLASSES) {
		double HIGH_DISTANCE=10E10;
		int[]hits=new int[MAX_NB_CLASSES];
		double[]distMin=new double[MAX_NB_CLASSES];
		for(int i=0;i<distMin.length;i++)distMin[i]=HIGH_DISTANCE;

		for(int i=0;i<vals.length;i++) {
			if(vals[i]<0)continue;
			hits[vals[i]]++;
			if(distances[i]<distMin[vals[i]])distMin[vals[i]]=distances[i];
		}
		
		
		
		//recherche du maximum represente le moins distant
		int valMax=0;
		int indMax=-1;
		double distMinOfMax=HIGH_DISTANCE;
		for(int i= 0;i<MAX_NB_CLASSES;i++) {
			if( (hits[i]>valMax)) {
				valMax=hits[i];
				indMax=i;
				distMinOfMax=distMin[i];
			}
			else if( (hits[i]==valMax) && (distMin[i]<=distMinOfMax)) {
				valMax=hits[i];
				indMax=i;
				distMinOfMax=distMin[i];
			}
		}
		return indMax;
	}
	

	/**
	 * Most represented value with BG exclusion.
	 *
	 * @param vals the vals
	 * @param distances the distances
	 * @param MAX_NB_CLASSES the max nb classes
	 * @param bgValue the bg value
	 * @return the int
	 */
	public static int mostRepresentedValueWithBGExclusion(int []vals, double []distances,int MAX_NB_CLASSES,int bgValue) {
		double HIGH_DISTANCE=10E10;
		int[]hits=new int[MAX_NB_CLASSES];
		double[]distMin=new double[MAX_NB_CLASSES];
		for(int i=0;i<distMin.length;i++)distMin[i]=HIGH_DISTANCE;

		for(int i=0;i<vals.length;i++) {
			if(vals[i]<0)continue;
			if(vals[i]==bgValue)continue;
			hits[vals[i]]++;
			if(distances[i]<distMin[vals[i]])distMin[vals[i]]=distances[i];
		}
		
		
		
		//recherche du maximum represente le moins distant
		int valMax=0;
		int indMax=-1;
		double distMinOfMax=HIGH_DISTANCE;
		for(int i= 0;i<MAX_NB_CLASSES;i++) {
			if( (hits[i]>valMax)) {
				valMax=hits[i];
				indMax=i;
				distMinOfMax=distMin[i];
			}
			else if( (hits[i]!=0) && (hits[i]==valMax) && (distMin[i]<=distMinOfMax)) {
				valMax=hits[i];
				indMax=i;
				distMinOfMax=distMin[i];
			}
		}
		if(indMax==-1)return bgValue;
		return indMax;
	}
	
	/**
	 * Gets the radius in voxels.
	 *
	 * @param voxSizes the vox sizes
	 * @param radius the radius
	 * @param is3D the is 3 D
	 * @return the radius in voxels
	 */
	public static int[]getRadiusInVoxels(double[]voxSizes,double radius,boolean is3D){
		int[]valsRet=new int[3];
		for(int i=0;i<3;i++) valsRet[i]=(int)Math.round(radius/voxSizes[i]);
		if(!is3D)valsRet[2]=0;
		return valsRet;
	}

	
	/**
	 * Builds the image lookup.
	 *
	 * @param tabDist the tab dist
	 * @param voxSizes the vox sizes
	 * @param radius the radius
	 * @return the image plus
	 */
	public static ImagePlus buildImageLookup(double[][][]tabDist,double[]voxSizes,double radius) {
		int dimX=tabDist.length;
		int dimY=tabDist[0].length;
		int dimZ=tabDist[0][0].length;
		ImagePlus imgDist = IJ.createImage("lookup_mask", "32-bit black",dimX,dimY,dimZ);
		VitimageUtils.adjustImageCalibration(imgDist, voxSizes,"mm");
		for(int x=0;x<dimX;x++) {
			for(int y=0;y<dimY;y++) {
				for(int z=0;z<dimZ;z++) {
					(imgDist.getStack().getProcessor(z+1)).putPixelValue(x,y,(tabDist[x][y][z]<=radius ? tabDist[x][y][z] : 0));
				}
			}
		}
		IJ.run(imgDist,"Fire","");
		imgDist.setDisplayRange(0, radius);
		imgDist.setSlice(imgDist.getStackSize()/2+1);
		return imgDist;
	}
	

	/**
	 * Builds the distances.
	 *
	 * @param radiusVox the radius vox
	 * @param voxSizes the vox sizes
	 * @return the double[][][]
	 */
	public static double[][][]buildDistances(int []radiusVox,double[]voxSizes){
		int rayX=radiusVox[0];
		int rayY=radiusVox[1];
		int rayZ=radiusVox[2];
		double[][][]tab=new double[2*rayX+1][2*rayY+1][2*rayZ+1];
		int I=tab.length;
		int J=tab[0].length;
		int K=tab[0][0].length;			
		for(int i=0;i<I;i++) {
			for(int j=0;j<J;j++) {
				for(int k=0;k<K;k++) {
				tab[i][j][k]=(Math.sqrt( (rayX-i) * (rayX-i)*voxSizes[0]*voxSizes[0] +
										 (rayY-j) * (rayY-j)*voxSizes[1]*voxSizes[1] +
										 (rayZ-k) * (rayZ-k)*voxSizes[2]*voxSizes[2]));
				}
			}
		}
		return tab;
	}


	
	/**
	 * Most represented filtering with radius.
	 *
	 * @param imgInTmp the img in tmp
	 * @param radius the radius
	 * @param is3D the is 3 D
	 * @param maxNbClasses the max nb classes
	 * @param doPadding the do padding
	 * @return the image plus
	 */
	public static ImagePlus mostRepresentedFilteringWithRadius(ImagePlus imgInTmp,double radius,boolean is3D,int maxNbClasses,boolean doPadding) {
		double []voxS=VitimageUtils.getVoxelSizes(imgInTmp);
		int[]radiusVox=getRadiusInVoxels(voxS,radius,is3D);
		int rayX=radiusVox[0];
		int rayY=radiusVox[1];
		int rayZ=radiusVox[2];
	
		boolean[][][]lookup=buildLookup3D(radius,radiusVox,voxS);
		double[][][]distances=buildDistances(radiusVox,voxS);
		ImagePlus imgLookup=buildImageLookup(distances,voxS,radius);
		imgLookup.show();
		ImagePlus imgIn=new Duplicator().run(imgInTmp);
		VitimageUtils.adjustImageCalibration(imgIn, imgInTmp);
		int dimX=imgIn.getWidth();
		int dimY=imgIn.getHeight();
		int dimZ=imgIn.getStackSize();
		System.out.println("Effectivement dims avant="+dimX+","+dimY+","+dimZ);
		if(doPadding) {
			imgIn= VitimageUtils.uncropImageByte(imgIn,rayX,rayY,rayZ,dimX+2*rayX,dimY+2*rayY,dimZ+2*rayZ);
			imgIn.show();
			imgIn.setSlice(rayZ+1);
			IJ.run(imgIn, "Select All", "");
			IJ.run(imgIn, "Copy", "");
			for(int z=0;z<rayZ;z++) {
				imgIn.setSlice(z+1);
				IJ.run(imgIn, "Paste", "");
			}
			imgIn.setSlice(dimZ+2*rayX-rayZ);
			IJ.run(imgIn, "Select All", "");
			IJ.run(imgIn, "Copy", "");
			for(int z=0;z<rayZ;z++) {
				imgIn.setSlice(dimZ+2*rayX-rayZ+z+1);
				IJ.run(imgIn, "Paste", "");
			}
			imgIn.hide();		
			dimX=imgIn.getWidth();
			dimY=imgIn.getHeight();
			dimZ=imgIn.getStackSize();
			System.out.println("Effectivement dims apres="+dimX+","+dimY+","+dimZ);
		}
		ImagePlus imgOut=imgIn.duplicate();
		VitimageUtils.adjustImageCalibration(imgOut, imgIn);
		int []vals=new int[lookup.length*lookup[0].length*lookup[0][0].length];
		double[] dist=new double[lookup.length*lookup[0].length*lookup[0][0].length];
		byte[][] valsImg=new byte[dimZ][];
		byte[][] valsOut=new byte[dimZ][];
		for(int z=0;z<dimZ;z++) {
			valsImg[z]=(byte[])imgIn.getStack().getProcessor(z+1).getPixels();
			valsOut[z]=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
		}			
		for(int z=rayZ;z<dimZ-rayZ;z++) {
			if(z%10==0)System.out.print("  "+z+"/"+dimZ);
			if(z%100==0)System.out.println(z+"/"+dimZ);
			for(int x=rayX;x<dimX-rayX;x++) {
				for(int y=rayY;y<dimY-rayY;y++){
					int index=0;
					for(int dz=-rayZ;dz<rayZ+1;dz++) {
						for(int dx=-rayX;dx<rayX+1;dx++) {
							for(int dy=-rayY;dy<rayY+1;dy++){
								if(! lookup[dx+rayX][dy+rayY][dz+rayZ]) {
									dist[index]=10E8;
									vals[index++]=-1;
								}
								else {
									vals[index]=((byte)(valsImg[z+dz][dimX*(y+dy)+(x+dx)]) & 0xff);
									dist[index++]=distances[dx+rayX][dy+rayY][dz+rayZ];
								}
							}
						}
					}
					valsOut[z][dimX*(y)+(x)]=((byte)(mostRepresentedValue(vals, dist,maxNbClasses)  & 0xff));
				}			
			}
		}
		imgLookup.close();
		if(doPadding)imgOut=VitimageUtils.cropImageByte(imgOut, rayX, rayY, rayZ,dimX-2*rayX, dimY-2*rayY, dimZ-2*rayZ);
		IJ.run(imgOut,"Fire","");
		imgOut.resetDisplayRange();
		return imgOut;
	}		

	/**
	 * Most represented filtering with radius with BG exclusion.
	 *
	 * @param imgInTmp the img in tmp
	 * @param radius the radius
	 * @param is3D the is 3 D
	 * @param maxNbClasses the max nb classes
	 * @param doPadding the do padding
	 * @param bgValue the bg value
	 * @return the image plus
	 */
	public static ImagePlus mostRepresentedFilteringWithRadiusWithBGExclusion(ImagePlus imgInTmp,double radius,boolean is3D,int maxNbClasses,boolean doPadding,int bgValue) {
		double []voxS=VitimageUtils.getVoxelSizes(imgInTmp);
		int[]radiusVox=getRadiusInVoxels(voxS,radius,is3D);
		int rayX=radiusVox[0];
		int rayY=radiusVox[1];
		int rayZ=radiusVox[2];
	
		boolean[][][]lookup=buildLookup3D(radius,radiusVox,voxS);
		double[][][]distances=buildDistances(radiusVox,voxS);
		ImagePlus imgLookup=buildImageLookup(distances,voxS,radius);
		imgLookup.show();
		ImagePlus imgIn=new Duplicator().run(imgInTmp);
		VitimageUtils.adjustImageCalibration(imgIn, imgInTmp);
		int dimX=imgIn.getWidth();
		int dimY=imgIn.getHeight();
		int dimZ=imgIn.getStackSize();
		System.out.println("Effectivement dims avant="+dimX+","+dimY+","+dimZ);
		if(doPadding) {
			imgIn= VitimageUtils.uncropImageByte(imgIn,rayX,rayY,rayZ,dimX+2*rayX,dimY+2*rayY,dimZ+2*rayZ);
			imgIn.show();
			imgIn.setSlice(rayZ+1);
			IJ.run(imgIn, "Select All", "");
			IJ.run(imgIn, "Copy", "");
			for(int z=0;z<rayZ;z++) {
				imgIn.setSlice(z+1);
				IJ.run(imgIn, "Paste", "");
			}
			imgIn.setSlice(dimZ+2*rayX-rayZ);
			IJ.run(imgIn, "Select All", "");
			IJ.run(imgIn, "Copy", "");
			for(int z=0;z<rayZ;z++) {
				imgIn.setSlice(dimZ+2*rayX-rayZ+z+1);
				IJ.run(imgIn, "Paste", "");
			}
			imgIn.hide();		
			dimX=imgIn.getWidth();
			dimY=imgIn.getHeight();
			dimZ=imgIn.getStackSize();
			System.out.println("Effectivement dims apres="+dimX+","+dimY+","+dimZ);
		}
		ImagePlus imgOut=imgIn.duplicate();
		VitimageUtils.adjustImageCalibration(imgOut, imgIn);
		int []vals=new int[lookup.length*lookup[0].length*lookup[0][0].length];
		double[] dist=new double[lookup.length*lookup[0].length*lookup[0][0].length];
		byte[][] valsImg=new byte[dimZ][];
		byte[][] valsOut=new byte[dimZ][];
		for(int z=0;z<dimZ;z++) {
			valsImg[z]=(byte[])imgIn.getStack().getProcessor(z+1).getPixels();
			valsOut[z]=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
		}			
		for(int z=rayZ;z<dimZ-rayZ;z++) {
			if(z%10==0)System.out.print("  "+z+"/"+dimZ);
			if(z%100==0)System.out.println(z+"/"+dimZ);
			for(int x=rayX;x<dimX-rayX;x++) {
				for(int y=rayY;y<dimY-rayY;y++){
					int index=0;
					for(int dz=-rayZ;dz<rayZ+1;dz++) {
						for(int dx=-rayX;dx<rayX+1;dx++) {
							for(int dy=-rayY;dy<rayY+1;dy++){
								if(! lookup[dx+rayX][dy+rayY][dz+rayZ]) {
									dist[index]=10E8;
									vals[index++]=-1;
								}
								else {
									vals[index]=((byte)(valsImg[z+dz][dimX*(y+dy)+(x+dx)]) & 0xff);
									dist[index++]=distances[dx+rayX][dy+rayY][dz+rayZ];
								}
							}
						}
					}
					if((valsImg[z][dimX*(y)+(x)]) ==(byte)(bgValue  & 0xff)  ) {
						valsOut[z][dimX*(y)+(x)]=(byte)(bgValue  & 0xff);
					}
					else valsOut[z][dimX*(y)+(x)]=((byte)(mostRepresentedValueWithBGExclusion(vals, dist,maxNbClasses,bgValue)  & 0xff));
				}			
			}
		}
		imgLookup.close();
		if(doPadding)imgOut=VitimageUtils.cropImageByte(imgOut, rayX, rayY, rayZ,dimX-2*rayX, dimY-2*rayY, dimZ-2*rayZ);
		IJ.run(imgOut,"Fire","");
		imgOut.resetDisplayRange();
		return imgOut;
	}		


	
	/**
	 * Most represented filtering with radius.
	 *
	 * @param imgIn the img in
	 * @param radius the radius
	 * @param distances the distances
	 * @return the image plus
	 */
	public static ImagePlus mostRepresentedFilteringWithRadius(ImagePlus imgIn,double radius,double[][]distances) {
		boolean[][]lookup=buildLookup(radius);
		ImagePlus imgOut=imgIn.duplicate();
		int dimX=imgIn.getWidth();
		int dimY=imgIn.getHeight();
		int dimZ=imgIn.getStackSize();
		int ray=(lookup.length-1)/2;
		int []vals=new int[lookup.length*lookup[0].length];
		double[] dist=new double[lookup.length*lookup[0].length];
		
		for(int z=0;z<dimZ;z++) {
			if(z%10==0)System.out.println(z+"/"+dimZ);
			byte[] valsImg=(byte[])imgIn.getStack().getProcessor(z+1).getPixels();
			byte[] valsOut=(byte[])imgOut.getStack().getProcessor(z+1).getPixels();
			for(int x=ray;x<dimX-ray;x++) {
				for(int y=ray;y<dimY-ray;y++){
					int index=0;
					for(int dx=-ray;dx<ray+1;dx++) {
						for(int dy=-ray;dy<ray+1;dy++){
							if(! lookup[dx+ray][dy+ray]) {
								dist[index]=10E8;
								vals[index++]=-1;
							}
							else {
								vals[index]=((byte)(valsImg[dimX*(y+dy)+(x+dx)]) & 0xff);
								dist[index++]=distances[dx+ray][dy+ray];
							}
						}
					}
					valsOut[dimX*(y)+(x)]=((byte)(mostRepresentedValue(vals, dist,256)  & 0xff));
				}			
			}
		}
		return imgOut;
	}
	
	
	
	/**
	 * Builds the lookup.
	 *
	 * @param ray the ray
	 * @return the boolean[][]
	 */
	public static boolean[][]buildLookup(double ray){
		int halfSize=( (int) Math.floor(ray));
		boolean[][]tab=new boolean[2*halfSize+1][2*halfSize+1];
		for(int i=0;i<tab.length;i++) {
			for(int j=0;j<tab.length;j++) {
				tab[i][j]=(Math.sqrt( (halfSize-i) * (halfSize-i) +(halfSize-j) * (halfSize-j))<=ray);
			}
		}
		return tab;
	}
	
	
	/**
	 * Builds the lookup 3 D.
	 *
	 * @param radius the radius
	 * @param radiusVox the radius vox
	 * @param voxSizes the vox sizes
	 * @return the boolean[][][]
	 */
	public static boolean[][][]buildLookup3D(double radius,int []radiusVox,double[]voxSizes){
		int rayX=radiusVox[0];
		int rayY=radiusVox[1];
		int rayZ=radiusVox[2];
		boolean[][][]tab=new boolean[2*rayX+1][2*rayY+1][2*rayZ+1];
		int I=tab.length;
		int J=tab[0].length;
		int K=tab[0][0].length;
		for(int i=0;i<I;i++) {
			for(int j=0;j<J;j++) {
				for(int k=0;k<K;k++) {
					tab[i][j][k]=(Math.sqrt( (rayX-i) * (rayX-i)*voxSizes[0]*voxSizes[0] +
							 (rayY-j) * (rayY-j)*voxSizes[1]*voxSizes[1] +
							 (rayZ-k) * (rayZ-k)*voxSizes[2]*voxSizes[2]) < radius);
				}
			}
		}
		return tab;
	}

	
	/**
	 * Builds the distances.
	 *
	 * @param ray the ray
	 * @return the double[][]
	 */
	public static double[][]buildDistances(double ray){
		int halfSize=( (int) Math.floor(ray));
		double[][]tab=new double[2*halfSize+1][2*halfSize+1];
		for(int i=0;i<tab.length;i++) {
			for(int j=0;j<tab.length;j++) {
				tab[i][j]=(Math.sqrt( (halfSize-i) * (halfSize-i) +(halfSize-j) * (halfSize-j)));
			}
		}
		return tab;
	}

	
	
	/**
	 * Prints the lookup.
	 *
	 * @param ra the ra
	 * @param tab the tab
	 */
	public static void printLookup(double ra, boolean [][]tab) {
		System.out.println("Displaying structuring element of size "+ra+"\n");
		for(int i=0;i<tab.length;i++) {
			for(int j=0;j<tab.length;j++) {
				System.out.print((tab[i][j] ? "  @  " : "  -  "));
			}
			System.out.println();			
			System.out.println();			
		}
		System.out.println();			
		System.out.println();			
		System.out.println();			
	}
	
	
	
	

}
