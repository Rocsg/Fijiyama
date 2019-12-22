package com.vitimage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import com.vitimage.Vitimage_Toolbox.VolumeComparator;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.plugin.GaussianBlur3D;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import imagescience.transform.Transform;
import math3d.Point3d;

public interface TransformUtils {
	public enum Geometry{
		REFERENCE,
		QUASI_REFERENCE,
		SWITCH_XY,
		SWITCH_XZ,
		SWITCH_YZ,
		MIRROR_X,
		MIRROR_Y,
		MIRROR_Z,
		UNKNOWN
	}
		
	
	
	public enum Misalignment{
		NONE,
		LIGHT_RIGID,
		GREAT_RIGID,
		LIGHT_SIMILARITY,
		STRONG_SIMILARITY,
		LIGHT_DEFORMATION,
		STRONG_DEFORMATION,		
		VOXEL_SCALE_FACTOR,
		UNKNOWN
	}
	
	
	
	
	/**
	 * Top level functions
	 * @param img
	 * @param computeZaxisOnly
	 * @param cornersCoordinates
	 * @param ignoreUnattemptedDimensions
	 * @return
	 */
	
	/**
	 * Resampling 3D utilities
	 * @param img
	 * @param computeZaxisOnly
	 * @param cornersCoordinates
	 * @param ignoreUnattemptedDimensions
	 * @return
	 */
	public static ImagePlus[] reech3D(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title,boolean computeMask) {
		 ImagePlus[]ret;
		 if(computeMask) {
			 ret=new ImagePlus[2];
			 ImagePlus maskIn=ij.gui.NewImage.createImage("Mask",sizes[0],sizes[1],sizes[2],8,ij.gui.NewImage.FILL_WHITE);
			 ret[1]=reech3DByte(maskIn,matInput,sizes,"Mask_"+title);
		 }
		 else ret=new ImagePlus[1];
		 if (imPlusFlo.getType()==ImagePlus.GRAY8)ret[0]=reech3DByte(imPlusFlo,matInput,sizes,title);
		 else if (imPlusFlo.getType()==ImagePlus.GRAY16)ret[0]=reech3DShort(imPlusFlo,matInput,sizes,title);
		 else if (imPlusFlo.getType()==ImagePlus.GRAY32)ret[0]=reech3DFloat(imPlusFlo,matInput,sizes,title);	
		 else {
			 System.out.println("Type not handled yet");return null;
		 }
		 return ret;
	 }
  
    public static ImagePlus reech3DByte(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title) {
		//Input and Output images opening
		//~ ImagePlus imPlusRec=new ImagePlus(sizes[0],sizes[1],sizes[2]);
		ImagePlus imPlusRec=ij.gui.NewImage.createImage(title,sizes[0],sizes[1],sizes[2],8,ij.gui.NewImage.FILL_BLACK);
		ImageStack stackIn = imPlusFlo.getStack();
		ImageStack stackOut = imPlusRec.getStack();

		//Dimensions
		int zMaxIn = stackIn.getSize();
		int yMaxIn=imPlusFlo.getHeight();
		int xMaxIn=imPlusFlo.getWidth();
		
		int zMaxOut=stackOut.getSize();
		int yMaxOut=imPlusRec.getHeight();
		int xMaxOut=imPlusRec.getWidth();

		// Allows images access
		byte[][] imgIn= new byte[zMaxIn][];
		byte[][] imgOut= new byte[zMaxOut][];
        for(int z=1;z<=zMaxIn;z++){imgIn[z-1]=(byte[])(stackIn.getProcessor(z).getPixels());}
        for(int z=1;z<=zMaxOut;z++){imgOut[z-1]=(byte[])(stackOut.getProcessor(z).getPixels());}

		// Gregoire Malandain's C algorithm variables
		int i, j, k, ix, iy, iz;
		double x, y, z, dx, dy, dz, dxdy,dxdz,dydz,dxdydz;
		double res;
		double v6, v5, v4;
		int rdimx=xMaxOut, rdimy=yMaxOut, rdimz=zMaxOut;
		int tdimx=xMaxIn, tdimy=yMaxIn, tdimz=zMaxIn;
		//int tdimxy=tdimx*tdimy;
		int t1dimx=tdimx-1, t1dimy=tdimy-1, t1dimz=tdimz-1;
		double ddimx = (double)tdimx-0.5, ddimy = (double)tdimy-0.5;
		double ddimz = (double)tdimz-0.5;

		double [] mat=matInput;

		//Let's go!
		//For all voxels in the resulting image
		for ( k = 0; k < rdimz; k ++ ) {
			if((k>10) && (k%(rdimz/10)==0))System.out.println("Computing resulting image : slice, " + k +" / " + rdimz);
			for ( j = 0; j < rdimy; j ++ ) {
				for ( i = 0; i < rdimx; i ++) {
					
					/* Computed the location of the corresponding point in the original floating image */
					x = mat[0] * i +  mat[1] * j + mat[2] * k + mat[3];
					if ((x < -0.5) || ( x > ddimx)) { imgOut[k][i + xMaxOut * j]= 0xff & 0 ; continue; }
					y = mat[4] * i +  mat[5] * j + mat[6] * k + mat[7];
					if ((y < -0.5) || ( y > ddimy)) { imgOut[k][i + xMaxOut * j]= 0xff & 0; continue; }
					z = mat[8] * i +  mat[9] * j + mat[10] * k + mat[11];
					if ((z < -0.5) || ( z > ddimz)) { imgOut[k][i + xMaxOut * j]= 0xff & 0; continue; }
					
					/* here, the point lies on the borders or completely inside
					the image */
					ix = (int)x;
					iy = (int)y;
					iz = (int)z;


					/* If we are not in the border */
					if ( (x > 0.0) && (ix < t1dimx) &&
					(y > 0.0) && (iy < t1dimy) &&
					(z > 0.0) && (iz < t1dimz) ) {
						/* the corresponding point is in the box defined
						by (ix[+1],iy[+1],iz[+1]) */
						dx = x - ix;
						dy = y - iy;
						dz = z - iz;
						dxdy = dx*dy;
						dxdz = dx*dz;
						dydz = dy*dz;
						dxdydz = dxdy*dz;

						/* we have
						v[7]=dxdydz;            coefficient of tbuf(ix+1,iy+1,iz+1)
						v[6]=dxdz-dxdydz;       coefficient of tbuf(ix+1,iy,  iz+1)
						v[5]=dxdy-dxdydz;       coefficient of tbuf(ix+1,iy+1,iz  )
						v[4]=dx-dxdy-v[6];      coefficient of tbuf(ix+1,iy  ,iz  )
						v[3]=dydz-dxdydz;       coefficient of tbuf(ix  ,iy+1,iz+1)
						v[2]=dz-dydz-v[6];      coefficient of tbuf(ix  ,iy  ,iz+1)
						v[1]=dy-dydz-v[5];      coefficient of tbuf(ix  ,iy+1,iz  )
						v[0]=1-dy-dz+dydz-v[4]; coefficient of tbuf(ix  ,iy  ,iz  )
						*/
						v6 = dxdz-dxdydz;
						v5 = dxdy-dxdydz;
						v4 = dx-dxdy-v6;

						res = dxdydz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix+1,iy+1,iz+1) */

						res += (dydz-dxdydz) * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix  ,iy+1,iz+1) */
						
						res += v6 * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xff));
						/* tbuf(ix+1  ,iy,  iz+1) */

						res += (dz-dydz-v6) * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));
						/* tbuf(ix  ,iy  ,iz+1) */

						res += v5 * ((int)(imgIn[iz][ix+1 + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix+1,iy+1,iz  ) */
						
						res += (dy-dydz-v5) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));
						/* tbuf(ix  ,iy+1,iz  ) */

						res += v4 * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));
						/* tbuf(ix+1,iy  ,iz  ) */

						res += (1-dy-dz+dydz-v4) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff));
						/* tbuf(ix  ,iy  ,iz  ) */

						imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					
					
					/* here, we are sure we are on some border */
					//~ tpt += ix + iy * tdimx + iz * tdimxy;
 					if ( (x < 0.0) || (ix == t1dimx) ) {
						if ( (y < 0.0) || (iy == t1dimy) ) {
							if ( (z < 0.0) || (iz == t1dimz) ) {							
								//In a XYZ corner
								imgOut[k][i + xMaxOut * j] = (byte)(0xff & imgIn[iz][ix + xMaxIn * iy]);
								continue;
							}
							
							// On a XY-edge
							dz = z - iz;
							res  = (1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff));/* (1-dz)* tbuf(ix,iy,iz) */
							res += dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));/* dz * tbuf(ix,iy,iz+1) */
							
							imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						dy = y - iy;
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a XZ-edge
							res  = (1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* (1-dy)* tbuf(ix,iy,iz) */
							res += dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));     /* dy * tbuf(ix,iy+1,iz) */
							imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a X-face
						dz = z - iz;
						res = (1-dy)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
						res += dy*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));    /* tbuf(ix,iy+1,iz) */
						res += (1-dy)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));    /* tbuf(ix,iy,iz+1) */
						res += dy*dz * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xff));        /* tbuf(ix,iy+1,iz+1) */
						imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					/* here we are sure that the border is either
					along the Y or the Z axis */
					dx = x - ix;
					if ( (y < 0.0) || (iy == t1dimy) ) {
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a YZ-edge
							res = (1-dx) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
							res += dx * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));    /* tbuf(ix+1,iy,iz) */
							imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a Y-face
						dz = z - iz;
						res = (1-dx)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
						res += dx*(1-dz) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));    /* tbuf(ix+1,iy,iz) */
						res += (1-dx)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xff));    /* tbuf(ix,iy,iz+1) */
						res += dx*dz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xff));        /* tbuf(ix+1,iy,iz+1) */
						imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					//On a Z-face
					dy = y - iy;
					res = (1-dx)*(1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xff)); /* tbuf(ix,iy,iz) */
					res += dx*(1-dy) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xff));    /* tbuf(ix+1,iy,iz) */
					res += (1-dx)*dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xff));    /* tbuf(ix,iy+1,iz) */
					res += dx*dy * ((int)(imgIn[iz][ix+1 + xMaxIn * iy+1] & 0xff));        /* tbuf(ix+1,iy+1,iz) */
					imgOut[k][i + xMaxOut * j] = (byte)(0xff & ((byte) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
				}
			}								
		}			
		return imPlusRec;
    }
    
	public static ImagePlus reech3DShort(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title) {
		//Input and Output images opening
		//~ ImagePlus imPlusRec=new ImagePlus(sizes[0],sizes[1],sizes[2]);
		ImagePlus imPlusRec=ij.gui.NewImage.createImage(title,sizes[0],sizes[1],sizes[2],16,ij.gui.NewImage.FILL_BLACK);
		ImageStack stackIn = imPlusFlo.getStack();
		ImageStack stackOut = imPlusRec.getStack();

		//Dimensions
		int zMaxIn = stackIn.getSize();
		int yMaxIn=imPlusFlo.getHeight();
		int xMaxIn=imPlusFlo.getWidth();
		
		int zMaxOut=stackOut.getSize();
		int yMaxOut=imPlusRec.getHeight();
		int xMaxOut=imPlusRec.getWidth();

		// Allows images access
		short[][] imgIn= new short[zMaxIn][];
		short[][] imgOut= new short[zMaxOut][];
        for(int z=1;z<=zMaxIn;z++){imgIn[z-1]=(short[])(stackIn.getProcessor(z).getPixels());}
        for(int z=1;z<=zMaxOut;z++){imgOut[z-1]=(short[])(stackOut.getProcessor(z).getPixels());}

		// Gregoire Malandain's C algorithm variables
		int i, j, k, ix, iy, iz;
		double x, y, z, dx, dy, dz, dxdy,dxdz,dydz,dxdydz;
		double res;
		double v6, v5, v4;
		int rdimx=xMaxOut, rdimy=yMaxOut, rdimz=zMaxOut;
		int tdimx=xMaxIn, tdimy=yMaxIn, tdimz=zMaxIn;
		//int tdimxy=tdimx*tdimy;
		int t1dimx=tdimx-1, t1dimy=tdimy-1, t1dimz=tdimz-1;
		double ddimx = (double)tdimx-0.5, ddimy = (double)tdimy-0.5;
		double ddimz = (double)tdimz-0.5;

		double [] mat=matInput;

		//Let's go!
		//For all voxels in the resulting image
		for ( k = 0; k < rdimz; k ++ ) {
			if((k>10) && (k%(rdimz/10)==0))System.out.println("Computing resulting image : slice, " + k +" / " + rdimz);
			for ( j = 0; j < rdimy; j ++ ) {
				for ( i = 0; i < rdimx; i ++) {
					
					/* Computed the location of the corresponding point in the original floating image */
					x = mat[0] * i +  mat[1] * j + mat[2] * k + mat[3];
					if ((x < -0.5) || ( x > ddimx)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0 ; continue; }
					y = mat[4] * i +  mat[5] * j + mat[6] * k + mat[7];
					if ((y < -0.5) || ( y > ddimy)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					z = mat[8] * i +  mat[9] * j + mat[10] * k + mat[11];
					if ((z < -0.5) || ( z > ddimz)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					
					/* here, the point lies on the borders or completely inside
					the image */
					ix = (int)x;
					iy = (int)y;
					iz = (int)z;


					/* If we are not in the border */
					if ( (x > 0.0) && (ix < t1dimx) &&
					(y > 0.0) && (iy < t1dimy) &&
					(z > 0.0) && (iz < t1dimz) ) {
						/* the corresponding point is in the box defined
						by (ix[+1],iy[+1],iz[+1]) */
						dx = x - ix;
						dy = y - iy;
						dz = z - iz;
						dxdy = dx*dy;
						dxdz = dx*dz;
						dydz = dy*dz;
						dxdydz = dxdy*dz;

						/* we have
						v[7]=dxdydz;            coefficient of tbuf(ix+1,iy+1,iz+1)
						v[6]=dxdz-dxdydz;       coefficient of tbuf(ix+1,iy,  iz+1)
						v[5]=dxdy-dxdydz;       coefficient of tbuf(ix+1,iy+1,iz  )
						v[4]=dx-dxdy-v[6];      coefficient of tbuf(ix+1,iy  ,iz  )
						v[3]=dydz-dxdydz;       coefficient of tbuf(ix  ,iy+1,iz+1)
						v[2]=dz-dydz-v[6];      coefficient of tbuf(ix  ,iy  ,iz+1)
						v[1]=dy-dydz-v[5];      coefficient of tbuf(ix  ,iy+1,iz  )
						v[0]=1-dy-dz+dydz-v[4]; coefficient of tbuf(ix  ,iy  ,iz  )
						*/
						v6 = dxdz-dxdydz;
						v5 = dxdy-dxdydz;
						v4 = dx-dxdy-v6;

						res = dxdydz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix+1,iy+1,iz+1) */

						res += (dydz-dxdydz) * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix  ,iy+1,iz+1) */
						
						res += v6 * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xffff));
						/* tbuf(ix+1  ,iy,  iz+1) */

						res += (dz-dydz-v6) * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));
						/* tbuf(ix  ,iy  ,iz+1) */

						res += v5 * ((int)(imgIn[iz][ix+1 + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix+1,iy+1,iz  ) */
						
						res += (dy-dydz-v5) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));
						/* tbuf(ix  ,iy+1,iz  ) */

						res += v4 * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));
						/* tbuf(ix+1,iy  ,iz  ) */

						res += (1-dy-dz+dydz-v4) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff));
						/* tbuf(ix  ,iy  ,iz  ) */

						imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					
					
					/* here, we are sure we are on some border */
					//~ tpt += ix + iy * tdimx + iz * tdimxy;
 					if ( (x < 0.0) || (ix == t1dimx) ) {
						if ( (y < 0.0) || (iy == t1dimy) ) {
							if ( (z < 0.0) || (iz == t1dimz) ) {							
								//In a XYZ corner
								imgOut[k][i + xMaxOut * j] = (short)(0xffff & imgIn[iz][ix + xMaxIn * iy]);
								continue;
							}
							
							// On a XY-edge
							dz = z - iz;
							res  = (1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff));/* (1-dz)* tbuf(ix,iy,iz) */
							res += dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));/* dz * tbuf(ix,iy,iz+1) */
							
							imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						dy = y - iy;
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a XZ-edge
							res  = (1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* (1-dy)* tbuf(ix,iy,iz) */
							res += dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));     /* dy * tbuf(ix,iy+1,iz) */
							imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a X-face
						dz = z - iz;
						res = (1-dy)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
						res += dy*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));    /* tbuf(ix,iy+1,iz) */
						res += (1-dy)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));    /* tbuf(ix,iy,iz+1) */
						res += dy*dz * ((int)(imgIn[iz+1][ix + xMaxIn * (iy+1)] & 0xffff));        /* tbuf(ix,iy+1,iz+1) */
						imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					/* here we are sure that the border is either
					along the Y or the Z axis */
					dx = x - ix;
					if ( (y < 0.0) || (iy == t1dimy) ) {
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a YZ-edge
							res = (1-dx) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
							res += dx * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));    /* tbuf(ix+1,iy,iz) */
							imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
							continue;
						}
						
						//On a Y-face
						dz = z - iz;
						res = (1-dx)*(1-dz) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
						res += dx*(1-dz) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));    /* tbuf(ix+1,iy,iz) */
						res += (1-dx)*dz * ((int)(imgIn[iz+1][ix + xMaxIn * iy] & 0xffff));    /* tbuf(ix,iy,iz+1) */
						res += dx*dz * ((int)(imgIn[iz+1][ix+1 + xMaxIn * iy] & 0xffff));        /* tbuf(ix+1,iy,iz+1) */
						imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
						continue;
					}
					
					//On a Z-face
					dy = y - iy;
					res = (1-dx)*(1-dy) * ((int)(imgIn[iz][ix + xMaxIn * iy] & 0xffff)); /* tbuf(ix,iy,iz) */
					res += dx*(1-dy) * ((int)(imgIn[iz][ix+1 + xMaxIn * iy] & 0xffff));    /* tbuf(ix+1,iy,iz) */
					res += (1-dx)*dy * ((int)(imgIn[iz][ix + xMaxIn * (iy+1)] & 0xffff));    /* tbuf(ix,iy+1,iz) */
					res += dx*dy * ((int)(imgIn[iz][ix+1 + xMaxIn * iy+1] & 0xffff));        /* tbuf(ix+1,iy+1,iz) */
					imgOut[k][i + xMaxOut * j] = (short)(0xffff & ((short) (  (res>=0.0)  ?  ( (int)(res+0.5) )  :  ( 0 )   ) ));
				}
			}								
		}			
		return imPlusRec;
    }
	
	public static ImagePlus reech3DFloat(ImagePlus imPlusFlo,  double [] matInput,int [] sizes,String title) {
		//Input and Output images opening
		//~ ImagePlus imPlusRec=new ImagePlus(sizes[0],sizes[1],sizes[2]);
		ImagePlus imPlusRec=ij.gui.NewImage.createImage(title,sizes[0],sizes[1],sizes[2],32,ij.gui.NewImage.FILL_BLACK);
		ImageStack stackIn = imPlusFlo.getStack();
		ImageStack stackOut = imPlusRec.getStack();

		//Dimensions
		int zMaxIn = stackIn.getSize();
		int yMaxIn=imPlusFlo.getHeight();
		int xMaxIn=imPlusFlo.getWidth();
		
		int zMaxOut=stackOut.getSize();
		int yMaxOut=imPlusRec.getHeight();
		int xMaxOut=imPlusRec.getWidth();

		// Allows images access
		float[][] imgIn= new float[zMaxIn][];
		float[][] imgOut= new float[zMaxOut][];
        for(int z=1;z<=zMaxIn;z++){imgIn[z-1]=(float[])(stackIn.getProcessor(z).getPixels());}
        for(int z=1;z<=zMaxOut;z++){imgOut[z-1]=(float[])(stackOut.getProcessor(z).getPixels());}

		// Gregoire Malandain's C algorithm variables
		int i, j, k, ix, iy, iz;
		double x, y, z, dx, dy, dz, dxdy,dxdz,dydz,dxdydz;
		double res;
		double v6, v5, v4;
		int rdimx=xMaxOut, rdimy=yMaxOut, rdimz=zMaxOut;
		int tdimx=xMaxIn, tdimy=yMaxIn, tdimz=zMaxIn;
		//int tdimxy=tdimx*tdimy;
		int t1dimx=tdimx-1, t1dimy=tdimy-1, t1dimz=tdimz-1;
		double ddimx = (double)tdimx-0.5, ddimy = (double)tdimy-0.5;
		double ddimz = (double)tdimz-0.5;

		double [] mat=matInput;

		//Let's go!
		//For all voxels in the resulting image
		for ( k = 0; k < rdimz; k ++ ) {
			if((k>10) && (k%(rdimz/10)==0))System.out.println("Computing resulting image : slice, " + k +" / " + rdimz);
			for ( j = 0; j < rdimy; j ++ ) {
				for ( i = 0; i < rdimx; i ++) {
					
					/* Computed the location of the corresponding point in the original floating image */
					x = mat[0] * i +  mat[1] * j + mat[2] * k + mat[3];
					if ((x < -0.5) || ( x > ddimx)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0 ; continue; }
					y = mat[4] * i +  mat[5] * j + mat[6] * k + mat[7];
					if ((y < -0.5) || ( y > ddimy)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					z = mat[8] * i +  mat[9] * j + mat[10] * k + mat[11];
					if ((z < -0.5) || ( z > ddimz)) { imgOut[k][i + xMaxOut * j]= 0xffff & 0; continue; }
					
					/* here, the point lies on the borders or completely inside
					the image */
					ix = (int)x;
					iy = (int)y;
					iz = (int)z;


					/* If we are not in the border */
					if ( (x > 0.0) && (ix < t1dimx) &&
					(y > 0.0) && (iy < t1dimy) &&
					(z > 0.0) && (iz < t1dimz) ) {
						/* the corresponding point is in the box defined
						by (ix[+1],iy[+1],iz[+1]) */
						dx = x - ix;
						dy = y - iy;
						dz = z - iz;
						dxdy = dx*dy;
						dxdz = dx*dz;
						dydz = dy*dz;
						dxdydz = dxdy*dz;

						/* we have
						v[7]=dxdydz;            coefficient of tbuf(ix+1,iy+1,iz+1)
						v[6]=dxdz-dxdydz;       coefficient of tbuf(ix+1,iy,  iz+1)
						v[5]=dxdy-dxdydz;       coefficient of tbuf(ix+1,iy+1,iz  )
						v[4]=dx-dxdy-v[6];      coefficient of tbuf(ix+1,iy  ,iz  )
						v[3]=dydz-dxdydz;       coefficient of tbuf(ix  ,iy+1,iz+1)
						v[2]=dz-dydz-v[6];      coefficient of tbuf(ix  ,iy  ,iz+1)
						v[1]=dy-dydz-v[5];      coefficient of tbuf(ix  ,iy+1,iz  )
						v[0]=1-dy-dz+dydz-v[4]; coefficient of tbuf(ix  ,iy  ,iz  )
						*/
						v6 = dxdz-dxdydz;
						v5 = dxdy-dxdydz;
						v4 = dx-dxdy-v6;

						res = dxdydz * ((float)(imgIn[iz+1][ix+1 + xMaxIn * (iy+1)]));
						/* tbuf(ix+1,iy+1,iz+1) */

						res += (dydz-dxdydz) * ((float)(imgIn[iz+1][ix + xMaxIn * (iy+1)]));
						/* tbuf(ix  ,iy+1,iz+1) */
						
						res += v6 * ((float)(imgIn[iz+1][ix+1 + xMaxIn * iy]));
						/* tbuf(ix+1  ,iy,  iz+1) */

						res += (dz-dydz-v6) * ((float)(imgIn[iz+1][ix + xMaxIn * iy] ));
						/* tbuf(ix  ,iy  ,iz+1) */

						res += v5 * ((float)(imgIn[iz][ix+1 + xMaxIn * (iy+1)]));
						/* tbuf(ix+1,iy+1,iz  ) */
						
						res += (dy-dydz-v5) * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));
						/* tbuf(ix  ,iy+1,iz  ) */

						res += v4 * ((float)(imgIn[iz][ix+1 + xMaxIn * iy]));
						/* tbuf(ix+1,iy  ,iz  ) */

						res += (1-dy-dz+dydz-v4) * ((float)(imgIn[iz][ix + xMaxIn * iy]));
						/* tbuf(ix  ,iy  ,iz  ) */

						imgOut[k][i + xMaxOut * j] = (float)res;
						continue;
					}
					
					
					
					/* here, we are sure we are on some border */
					//~ tpt += ix + iy * tdimx + iz * tdimxy;
 					if ( (x < 0.0) || (ix == t1dimx) ) {
						if ( (y < 0.0) || (iy == t1dimy) ) {
							if ( (z < 0.0) || (iz == t1dimz) ) {							
								//In a XYZ corner
								imgOut[k][i + xMaxOut * j] = imgIn[iz][ix + xMaxIn * iy];
								continue;
							}
							
							// On a XY-edge
							dz = z - iz;
							res  = (1-dz) * ((float)(imgIn[iz][ix + xMaxIn * iy]));/* (1-dz)* tbuf(ix,iy,iz) */
							res += dz * ((float)(imgIn[iz+1][ix + xMaxIn * iy]));/* dz * tbuf(ix,iy,iz+1) */
							
							imgOut[k][i + xMaxOut * j] = (float)res;
							continue;
						}
						
						dy = y - iy;
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a XZ-edge
							res  = (1-dy) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* (1-dy)* tbuf(ix,iy,iz) */
							res += dy * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));     /* dy * tbuf(ix,iy+1,iz) */
							imgOut[k][i + xMaxOut * j] = (float)res;
							continue;
						}
						
						//On a X-face
						dz = z - iz;
						res = (1-dy)*(1-dz) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
						res += dy*(1-dz) * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));    /* tbuf(ix,iy+1,iz) */
						res += (1-dy)*dz * ((float)(imgIn[iz+1][ix + xMaxIn * iy]));    /* tbuf(ix,iy,iz+1) */
						res += dy*dz * ((float)(imgIn[iz+1][ix + xMaxIn * (iy+1)]));        /* tbuf(ix,iy+1,iz+1) */
						imgOut[k][i + xMaxOut * j] = (float)res;
						continue;
					}
					
					/* here we are sure that the border is either
					along the Y or the Z axis */
					dx = x - ix;
					if ( (y < 0.0) || (iy == t1dimy) ) {
						if ( (z < 0.0) || (iz == t1dimz) ) {
							
							//On a YZ-edge
							res = (1-dx) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
							res += dx * ((float)(imgIn[iz][ix+1 + xMaxIn * iy]));    /* tbuf(ix+1,iy,iz) */
							imgOut[k][i + xMaxOut * j] =  (float)res;
							continue;
						}
						
						//On a Y-face
						dz = z - iz;
						res = (1-dx)*(1-dz) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
						res += dx*(1-dz) * ((float)(imgIn[iz][ix+1 + xMaxIn * iy] ));    /* tbuf(ix+1,iy,iz) */
						res += (1-dx)*dz * ((float)(imgIn[iz+1][ix + xMaxIn * iy] ));    /* tbuf(ix,iy,iz+1) */
						res += dx*dz * ((float)(imgIn[iz+1][ix+1 + xMaxIn * iy] ));        /* tbuf(ix+1,iy,iz+1) */
						imgOut[k][i + xMaxOut * j] =(float)res;
						continue;
					}
					
					//On a Z-face
					dy = y - iy;
					res = (1-dx)*(1-dy) * ((float)(imgIn[iz][ix + xMaxIn * iy])); /* tbuf(ix,iy,iz) */
					res += dx*(1-dy) * ((float)(imgIn[iz][ix+1 + xMaxIn * iy]));    /* tbuf(ix+1,iy,iz) */
					res += (1-dx)*dy * ((float)(imgIn[iz][ix + xMaxIn * (iy+1)]));    /* tbuf(ix,iy+1,iz) */
					res += dx*dy * ((float)(imgIn[iz][ix+1 + xMaxIn * iy+1] ));        /* tbuf(ix+1,iy+1,iz) */
					imgOut[k][i + xMaxOut * j] = (float)0;
				}
			}								
		}			
		return imPlusRec;
    }


	


	public static double[][] getCoordinatesOf(ImagePlus img,int computationStyle,int minZ,int maxZ){
		if(computationStyle==VitimageUtils.COORD_OF_MAX_IN_TWO_LAST_SLICES) {
			double[][]tabRet=new double[2][3];
			tabRet[1][2]=tabRet[0][2]=-10E10;
			if(img.getType() == ImagePlus.GRAY32) {
				float[]valsImg2=(float[])img.getStack().getProcessor(2).getPixels();
				float[]valsImg3=(float[])img.getStack().getProcessor(3).getPixels();
				int xMax=img.getWidth();
				
				for(int x=0;x<xMax;x++) {
					for(int y=minZ;y<=maxZ;y++) {
						if(tabRet[0][2]<(float)valsImg2[xMax*y+x]) {
							tabRet[0][2]=(float)valsImg2[xMax*y+x];
							tabRet[0][0]=x;
							tabRet[0][1]=y;							
						}
						if(tabRet[1][2]<(float)valsImg3[xMax*y+x]) {
							tabRet[1][2]=(float)valsImg3[xMax*y+x];
							tabRet[1][0]=x;
							tabRet[1][1]=y;							
						}
					}
				}
			}
			return tabRet;
		}
		return null;
	}
	
	
		
	
	
	/**
	 * Mathematic utilities
	 * @param img
	 * @param computeZaxisOnly
	 * @param cornersCoordinates
	 * @param ignoreUnattemptedDimensions
	 * @return
	 */
	public static double calculateAngle(double x, double y)
	{
	    double angle = Math.toDegrees(Math.atan2(y, x));
	    // Keep angle between 0 and 360
	    angle = angle + Math.ceil( -angle / 360 ) * 360;

	    return angle;
	}
	
	public static double diffTeta(double teta1,double teta2) {
		//120   130  -> -10
		//130   120  -> 10
		//30    330  -> 30  -30  -> 60 
		//330    30  -> -30  30  -> -60 
		if(Math.abs(teta1-teta2)>180) {
			if(teta1>teta2)return(teta1-360-teta2);
			if(teta2>teta1)return(teta1-(teta2-360));
		}
		else {
			return teta1-teta2;
		}
		return 0;
	}


	class VolumeComparator implements java.util.Comparator {
		   public int compare(Object o1, Object o2) {
		      return ((Double) ((Object[]) o1)[0]).compareTo((Double)((Object[]) o2)[0]);
		   }
		}
	
	class AngleComparator implements java.util.Comparator {
	   public int compare(Object o1, Object o2) {
	      return ((Double) ((Double[]) o1)[2]).compareTo((Double)((Double[]) o2)[2]);
	   }
	}
	
	public static double norm(double[]v){
		return Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
	}

	public static double[] normalize(double[]v){
		double[] ret=new double[3];
		double nrm=norm(v);
		ret[0]=v[0]/nrm;
		ret[1]=v[1]/nrm;
		ret[2]=v[2]/nrm;
		return ret;
	}

	public static double scalarProduct(double[]v1,double []v2){
		return(v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2]);
	}

	public static double scalarProduct(Point3d v1,Point3d v2){
		return(v1.x*v2.x+v1.y*v2.y+v1.z*v2.z);
	}

	public static double [] sumVector(double[]v,double []v2){
		double[] ret=new double[3];
		ret[0]=v[0]+v2[0];
		ret[1]=v[1]+v2[1];
		ret[2]=v[2]+v2[2];
		return ret;
	}

	public static double [] sumVector(double[]v,double []v2,double[]v3){
		return sumVector(v,sumVector(v2,v3));
	}

	
	public static double [] multiplyVector(double[]v,double []v2){
		double[] ret=new double[3];
		ret[0]=v[0]*v2[0];
		ret[1]=v[1]*v2[1];
		ret[2]=v[2]*v2[2];
		return ret;
	}

	public static double [] multiplyVector(double[]v,double factor){
		double[] ret=new double[3];
		ret[0]=v[0]*factor;
		ret[1]=v[1]*factor;
		ret[2]=v[2]*factor;
		return ret;
	}

	public static double[] vectorialProduct(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[1]*v2[2]-v1[2]*v2[1];		
		ret[1]=v1[2]*v2[0]-v1[0]*v2[2];		
		ret[2]=v1[0]*v2[1]-v1[1]*v2[0];		
		return ret;
	}

	public static double[] vectorialSubstraction(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[0]-v2[0];		
		ret[1]=v1[1]-v2[1];		
		ret[2]=v1[2]-v2[2];		
		return ret;
	}

	public static double[] vectorialAddition(double[]v1,double[]v2){
		double[] ret=new double[3];
		ret[0]=v1[0]+v2[0];		
		ret[1]=v1[1]+v2[1];		
		ret[2]=v1[2]+v2[2];		
		return ret;
	}

	public static double[] invertVector(double[]v1){
		double[] ret=new double[3];
		ret[0]=1/v1[0];		
		ret[1]=1/v1[1];		
		ret[2]=1/v1[2];		
		return ret;
	}

	
	public static double[] proj_u_of_v(double[]u,double[]v){
		double scal1=scalarProduct(u,v);
		double scal2=scalarProduct(u,u);
		return multiplyVector(u,scal1/scal2);
	}
	
	
	public static Point3d convertPointToRealSpace(Point3d p,ImagePlus img) {
		double alpha=0;//for itk
		return new Point3d((p.x+alpha)*img.getCalibration().pixelWidth , (p.y+alpha)*img.getCalibration().pixelHeight , (p.z+alpha)*img.getCalibration().pixelDepth);
	}

	public static Point3d convertPointToImageSpace(Point3d p,ImagePlus img) {
		double alpha=0;//for itk
		return new Point3d(p.x/img.getCalibration().pixelWidth-alpha, p.y/img.getCalibration().pixelHeight-alpha, p.z/img.getCalibration().pixelDepth-alpha);
	}
	

	public static double[] point3dToDoubleArray(Point3d p) {return new double[] {p.x,p.y,p.z};}
	
	public static Point3d doubleArrayToPoint3d(double[]d) {return new Point3d(d[0],d[1],d[2]);}
	

	/**
	 * IO Utilities for Vectors and Matrices
	 * @param vect
	 * @param vectNom
	 */
	static void printVector(double []vect,String vectNom){
		System.out.println(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	static String stringVectorN(double []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=vect[i]+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	static String stringVectorNDou(double []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=VitimageUtils.dou(vect[i])+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	static String stringVectorN(int []vect,String vectNom){
		String str=vectNom+" = [ ";
		for(int i=0;i<vect.length;i++)str+=vect[i]+(i==vect.length-1 ? " ]" : " , ");
		return str;
	}

	static String stringMatrixN(int [][]vect,String vectNom){
		String str=vectNom+" = [ \n";
		for(int i=0;i<vect.length;i++)str+=stringVectorN(vect[i],"Ligne "+i+"")+"\n";
		str+=" ]";
		return str;
	}

	public static String stringVector(double []vect,String vectNom){
		return(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	public static String stringVectorDou(double []vect,String vectNom){
		return(vectNom+" = [ "+VitimageUtils.dou(vect[0])+" , "+VitimageUtils.dou(vect[1])+" , "+VitimageUtils.dou(vect[2])+" ]");
	}

	public static String stringVectorDou(double []vect,String vectNom,int n){
		return(vectNom+" = [ "+VitimageUtils.dou(vect[0],n)+" , "+VitimageUtils.dou(vect[1],n)+" , "+VitimageUtils.dou(vect[2],n)+" ]");
	}

	
	public static String stringVector(int []vect,String vectNom){
		return(vectNom+" = [ "+vect[0]+" , "+vect[1]+" , "+vect[2]+" ]");
	}

	
	public static double[]transformToArray(Transform transGlobal){
	 	double[]ret=new double[]{transGlobal.get(0,0) ,transGlobal.get(0,1) ,transGlobal.get(0,2) ,transGlobal.get(0,3) ,
	 							 transGlobal.get(1,0) ,transGlobal.get(1,1) ,transGlobal.get(1,2) ,transGlobal.get(1,3) ,
 							 	 transGlobal.get(2,0) ,transGlobal.get(2,1) ,transGlobal.get(2,2) ,transGlobal.get(2,3)
 							 	 };
		 return (ret);
	 }
	
	public static Transform arrayToTransform(double[]mat){
	 	Transform tr=new Transform();
	 	for(int j=0;j<12;j++) {
	 		tr.set(j/4,j%4,mat[j]);	 	
	 	}
	 	return tr;
 	}

	public static Transform readMatrixFromFile(String filePath,boolean debug) {
		Transform tr=new Transform(1,0,0,0,0,1,0,0,0,0,1,0);
		if (filePath==null)return tr;
		String strFile;
		try {
			 strFile= Files.lines(Paths.get(filePath) ).collect(Collectors.joining("\n"));
        } catch (IOException ex) {
	        ex.printStackTrace();
	        strFile="None\nNone";
        }

		if(debug){
			System.out.println("STRING LUE DANS FICHIER MATRICE=");
			System.out.println(strFile);
		}
        String[]strLines=strFile.split("\n");
        if(strLines[0].charAt(0)=='#'){
        	if(strLines[0].charAt(1)=='I'){ //Transform file in ITK format
        	   if(debug)System.out.println("ITK file");
			   String[]strValues=strLines[3].split(" ");
    		   if(strValues.length==13){
    		   		tr.set(Double.valueOf(strValues[1]), Double.valueOf(strValues[2]), Double.valueOf(strValues[3]), Double.valueOf(strValues[10]), 
    		   			   Double.valueOf(strValues[4]), Double.valueOf(strValues[5]), Double.valueOf(strValues[6]), Double.valueOf(strValues[11]),
    		   			   Double.valueOf(strValues[7]), Double.valueOf(strValues[8]), Double.valueOf(strValues[9]), Double.valueOf(strValues[12]) );
    		   }
    		   else System.out.println("ITK Transform file is corrupted ");
        	}
        	else if(strLines[0].charAt(2)=='S' || strLines[0].charAt(2)=='A'){//Transform file in ImageJ's Transform_IO format
			   if(debug)System.out.println("Transform_IO file");
			   String[]strValuesl1=strLines[2].split(" ");
			   String[]strValuesl2=strLines[3].split(" ");
				   String[]strValuesl3=strLines[4].split(" ");
    		   if(strValuesl1.length==4  && strValuesl2.length==4  && strValuesl3.length==4 ){
    		   		tr.set(Double.valueOf(strValuesl1[0]), Double.valueOf(strValuesl1[1]), Double.valueOf(strValuesl1[2]), Double.valueOf(strValuesl1[3]), 
    		   			   Double.valueOf(strValuesl2[0]), Double.valueOf(strValuesl2[1]), Double.valueOf(strValuesl2[2]), Double.valueOf(strValuesl2[3]),
    		   			   Double.valueOf(strValuesl3[0]), Double.valueOf(strValuesl3[1]), Double.valueOf(strValuesl3[2]), Double.valueOf(strValuesl3[3]) );
    		   			   if(strLines[0].charAt(2)=='S') tr.invert();// patch for the transformation computed with an manual alignment with 3dviewer
    		   }
    		   else System.out.println("ImageJ Transform file is corrupted ");
        	}
        }
        else {
    		if(debug){System.out.println("Unknown format");}
        }
    	if(debug){
    		System.out.println("Matrix read is :");
    		for(int i=0;i<3;i++){
				System.out.println("[ "+tr.get(i,0)+"  "+tr.get(i,1)+"  "+tr.get(i,2)+"  "+tr.get(i,3)+" ]");
        	}
			System.out.println("");
    	}			        
  		return tr;
	}

	public static void writeMatrixToFile(String filePath,Transform tr) {
		File f = new File(filePath);
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
			out.write("# Affine Transformation written in Transform_IO style, but inverted for direct processing of resampling\n");
			out.write("# at "+(new Date())+"\n");
			out.write(tr.get(0,0)+" "+tr.get(0,1)+" "+tr.get(0,2)+" "+tr.get(0,3)+"\n"+tr.get(1,0)+" "+tr.get(1,1)+" "+tr.get(1,2)+" "+tr.get(1,3)+"\n"
					 +tr.get(2,0)+" "+tr.get(2,1)+" "+tr.get(2,2)+" "+tr.get(2,3)+"\n"+tr.get(3,0)+" "+tr.get(3,1)+" "+tr.get(3,2)+" "+tr.get(3,3)+"\n");
			out.close();
		} catch (Exception e) {
			IJ.error("Unable to write transformation to file: "+f.getAbsolutePath()+"error: "+e);
		}
	}
	
	public static String stringMatrixMN(String sTitre,double[][] confusionMatrix){
		String s=new String();
		s+=""+sTitre+" , matrice de taille "+confusionMatrix.length+" X "+confusionMatrix[0].length+"\n";
		for(int i=0;i<confusionMatrix.length;i++){
			s+="[ ";
			for(int j=0;j<confusionMatrix[i].length-1;j++){
				s+=VitimageUtils.dou(confusionMatrix[i][j]);
				s+=" , ";
			}
			s+=VitimageUtils.dou(confusionMatrix[i][confusionMatrix[i].length-1])+" ] \n";
		}
		return s;
	}	

	public static void printMatrix(String sTitre,double[]tab){
		String s=new String();
		s+="\n Affichage de la matrice ";
		s+=sTitre;
		s+="\n";		
		for(int i=0;i<3;i++){
			s+="[ ";
			for(int j=0;j<3;j++){
				s+=tab[i*4+j];
				s+=" , ";
			}
			s+=tab[i*4+3];
			s+=" ] \n";
		}
		s+= "[ 0 , 0 , 0 , 1 ]\n\n";
		System.out.println(s);	
	}	
	
	/* T2 is the transformation, written in the second basis
	*   T1 is the transformation that we are looking for, written in the initial basis
	*   P12 is the passage matrix, that allows one to resample the image (I1)
	*   in order to get (I2), thus : vect1 = P12 x vect2
	*   P21 is the inverse matrix
	*   vxRatio is the ratio between the voxel sizes : vx2/vx1     
	*/
    public static void basisChange(double [] T2,double vxRatio,double vyRatio,double vzRatio,double [] T1){
		double vxRatInv=1.0/vxRatio;
		double vyRatInv=1.0/vyRatio;
		double vzRatInv=1.0/vzRatio;
		double [] P12={vxRatio,0,0,-0.5+vxRatio/2, 0,vyRatio,0,-0.5+vyRatio/2,  0,0,vzRatio,-0.5+vzRatio/2};
		double [] P21={vxRatInv,0,0,-0.5+vxRatInv/2, 0,vyRatInv,0,-0.5+vyRatInv/2,  0,0,vzRatInv,-0.5+vzRatInv/2};
		double [] intermediaryRes=new double[12];	
		matrixProduct(T2,P21,intermediaryRes);
		matrixProduct(P12,intermediaryRes,T1);
		System.out.println("Basis change");
		printMatrix("T2",T2);
		printMatrix("P12",P12);
		printMatrix("P21",P21);
		printMatrix("T1=P12 x (T2 x P21)",T1);
	}

	/* Tinter is the transformation matrix, written in the registration space
	*   P1 is the passage matrix from the initial space to the registration space, built from the voxelSizes of the initial space scales1
	*   P2 is the passage matrix from the registration space to the visualization space, built from the voxelSizes of the initial space scales2
	* @return The global transformation matrix T' = P1 x T x P2
	*/
	public static double[] doubleBasisChange(double [] scales1,double [] Tinter,double [] scales2){
		double [] result=new double[12];
		double vxRatio=scales1[0];
		double vyRatio=scales1[1];
		double vzRatio=scales1[2];
		double vxRatInv=scales2[0];
		double vyRatInv=scales2[1];
		double vzRatInv=scales2[2];
		double [] P1={1/vxRatio,0,0,-0.5, 0,1/vyRatio,0,-0.5,  0,0,1/vzRatio,-0.5};
		double [] P2={vxRatInv,0,0,vxRatInv/2, 0,vyRatInv,0,vyRatInv/2,  0,0,vzRatInv,vzRatInv/2};
		double [] intermediaryRes=new double[12];	
		matrixProduct(Tinter,P2,intermediaryRes);
		matrixProduct(P1,intermediaryRes,result);
		System.out.println("Double basis change");
		Vitimage_Toolbox.anna.rememberMatrix("P1, Passage from initial space to registration space : ",ItkTransform.stringMatrix("",P1));
		Vitimage_Toolbox.anna.rememberMatrix("T, Initial transformation matrix for registration : ",ItkTransform.stringMatrix("",Tinter));
		Vitimage_Toolbox.anna.rememberMatrix("P2, Passage from registration space to visualization space : ",ItkTransform.stringMatrix("",P2));
		Vitimage_Toolbox.anna.rememberMatrix("Global transformation = P1 x T x P2",ItkTransform.stringMatrix("",result));

		printMatrix("P1, Passage from initial space to registration space : ",P1);
		printMatrix("T, Initial transformation matrix for registration : ",Tinter);
		printMatrix("P2, Passage from registration space to visualization space : ",P2);
		printMatrix("Global transformation = P1 x T x P2",result);
		return result;
	}

	public static void matrixProduct(double[]tab1,double[]tab2,double[]tab3){
		//Matrix product in homogeneous coordinates 
		// [      Rotation       |Translation_x]
		// [      Rotation       |Translation_y]
		// [      Rotation       |Translation_z]
		// [   0     0      0    |      1      ]
		for(int i=0;i<3;i++){
			//Rotation component
			for(int j=0;j<4;j++){
				tab3[i*4+j]=tab1[i*4]*tab2[j] + tab1[i*4+1]*tab2[4+j] + tab1[i*4+2]*tab2[8+j];
			}
			//Additional computation for the translation component
			tab3[i*4+3]+=tab1[i*4+3];
		}
	}

	public static double[][]composeMatrices(Transform[]matricesToCompose){
		 double [][]matRet=new double[2][12];
		 Transform transGlobal=new Transform();
		 Transform transInd;
		 System.out.println("Et matrices to Compose fait tant de long : "+matricesToCompose.length);
		 for(int tr=0;tr<matricesToCompose.length;tr++) {
			 transInd=matricesToCompose[tr];
			 System.out.println("TransIND 1/2= :");
    		for(int ii=0;ii<3;ii++){
    			System.out.println("[ "+transInd.get(ii,0)+"  "+transInd.get(ii,1)+"  "+transInd.get(ii,2)+"  "+transInd.get(ii,3)+" ]");
        	}
			System.out.println("");
	    		
		 	transInd.transform(transGlobal);
		 	System.out.println("TransIND 2/2= :");
    		for(int ii=0;ii<3;ii++){
				System.out.println("[ "+transInd.get(ii,0)+"  "+transInd.get(ii,1)+"  "+transInd.get(ii,2)+"  "+transInd.get(ii,3)+" ]");
        	}
			System.out.println("");
			 transGlobal=new Transform(transInd);			 
		 }

		 transInd=new Transform(transGlobal);
		 transInd.invert();
		 matRet[0]=transformToArray(transGlobal);
		 matRet[1]=transformToArray(transInd);
		 return matRet;
	 }
	 
	public static double[][]composeMatrices(double[][]matricesToCompose){
		Transform[] trans=new Transform[matricesToCompose.length];
		for(int i=0;i<matricesToCompose.length;i++)trans[i]=arrayToTransform(matricesToCompose[i]);
		return (composeMatrices(trans));
	 }
	 
	
	
}
