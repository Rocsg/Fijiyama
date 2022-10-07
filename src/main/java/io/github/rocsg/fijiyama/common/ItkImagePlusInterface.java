/*
 * 
 */
package io.github.rocsg.fijiyama.common;
//TODO common

import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.PixelIDValueEnum;
import org.itk.simple.VectorDouble;
import org.itk.simple.VectorFloat;
import org.itk.simple.VectorUInt32;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import io.github.rocsg.fijiyama.common.VitiDialogs;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import math3d.Point3d;

// TODO: Auto-generated Javadoc
/**
 * The ImagePlus op_1add_2mult_3div_4sub.
 */
public interface ItkImagePlusInterface {
	

	
	
	/**
	 * Int array to vector U int 32.
	 *
	 * @param array the array
	 * @return the vector U int 32
	 */
	/* Helper functions to convert between arrays (java std format) and VectorDouble (org.itk.simple format) */
	public static VectorUInt32 intArrayToVectorUInt32(int[]array) {
		VectorUInt32 vect=new VectorUInt32(array.length);
		for(int i=0;i<array.length ;i++)vect.set(i,array[i]);
		return vect;
	}

	/**
	 * ${e.g(2).rsfu()}.
	 *
	 * @param vect the vect
	 * @return the ${e.g(1).rsfl()}
	 */
	public static int[] vectorUInt32ToIntArray(VectorUInt32 vect) {
		int[] tab=new int[(int) vect.size()];
		for(int i=0;i<tab.length ;i++)tab[i]=(int) vect.get(i);
		return tab;
	}

	/**
	 * Double array to vector double.
	 *
	 * @param array the array
	 * @return the vector double
	 */
	public static VectorDouble doubleArrayToVectorDouble(double[]array) {
		VectorDouble vect=new VectorDouble(array.length);
		for(int i=0;i<array.length ;i++)vect.set(i,array[i]);
		return vect;
	}

	/**
	 * Vector double to double array.
	 *
	 * @param vect the vect
	 * @return the double[]
	 */
	public static double[] vectorDoubleToDoubleArray(VectorDouble vect) {
		double[] tab=new double[(int) vect.size()];
		for(int i=0;i<tab.length ;i++)tab[i]=vect.get(i);
		return tab;
	}

	/**
	 * Vector double to point 3 d.
	 *
	 * @param vect the vect
	 * @return the point 3 d
	 */
	public static Point3d vectorDoubleToPoint3d(VectorDouble vect) {
		return new Point3d(vect.get(0) ,vect.get(1), vect.get(2) );
	}
	


	
	/**
	 * Image plus to itk image.
	 *
	 * @param img the img
	 * @return the image
	 */
	/*Helper functions to convert between ImagePlus (ImageJ format) and Image (org.itk.simple format) */
	public static Image imagePlusToItkImage(ImagePlus img) {
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		double[]voxSizes=new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
		int type=(img.getType()==ImagePlus.GRAY8 ? 8 : img.getType()==ImagePlus.GRAY16 ? 16 : img.getType()==ImagePlus.GRAY32 ? 32 : 4);
		Image ret=new Image(dimX,dimY,dimZ,type==8 ? PixelIDValueEnum.sitkUInt8 : type==16 ? PixelIDValueEnum.sitkUInt16 : type==32 ? PixelIDValueEnum.sitkFloat32 : PixelIDValueEnum.sitkUInt32);
		ret.setSpacing(doubleArrayToVectorDouble(voxSizes));
		VectorUInt32 coordinates=new VectorUInt32(3);
		if(type==8) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				byte []tabData=(byte[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						ret.setPixelAsUInt8(coordinates,tabData[dimX*y+x]);
					}
				}
			}
		}
		else if(type==16) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				short []tabData=(short[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						ret.setPixelAsUInt16(coordinates,tabData[dimX*y+x]);
					}
				}
			}
		}	
		else if(type==32) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				float []tabData=(float[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						ret.setPixelAsFloat(coordinates,tabData[dimX*y+x]);
					}
				}
			}
		}	
		else if(type==4) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				int []tabData=(int[])img.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						ret.setPixelAsUInt32(coordinates,tabData[dimX*y+x]);
					}
				}
			}
		}	
		else VitiDialogs.notYet("Conversion ImagePlus vers ItkImage type RGB");
		return ret;
	}
		
	/**
	 * Convert itk transform to image plus array.
	 *
	 * @param tr the tr
	 * @return the image plus[]
	 */
	public static ImagePlus[] convertItkTransformToImagePlusArray(ItkTransform tr){
		return convertDisplacementFieldToImagePlusArrayAndNorm((new DisplacementFieldTransform((org.itk.simple.Transform)tr).getDisplacementField()));
	}
	
	/**
	 * Convert displacement field to image plus array and norm.
	 *
	 * @param img the img
	 * @return the image plus[]
	 */
	public static ImagePlus[] convertDisplacementFieldToImagePlusArrayAndNorm(Image img){
		int dimX=(int) img.getWidth(); int dimY=(int) img.getHeight(); int dimZ=(int) img.getDepth();
		VectorDouble voxSizes= img.getSpacing();		
		ImagePlus []ret=new ImagePlus[4];
		for(int dim=0;dim<4;dim++) {
			ret[dim]=IJ.createImage("", dimX, dimY, dimZ, 32);
			Calibration cal = ret[dim].getCalibration();
			cal.setUnit("mm");
			cal.pixelWidth =voxSizes.get(0); cal.pixelHeight =voxSizes.get(1); cal.pixelDepth =voxSizes.get(2);
		}
		VectorUInt32 coordinates=new VectorUInt32(3);
		VectorDouble values=new VectorDouble(3);
		int coordIJ;
		for(int z=0;z<dimZ;z++) {
			coordinates.set(2,z);
			float [][]tabData=new float[4][];
			tabData[0]=(float[])ret[0].getStack().getProcessor(z+1).getPixels();
			tabData[1]=(float[])ret[1].getStack().getProcessor(z+1).getPixels();
			tabData[2]=(float[])ret[2].getStack().getProcessor(z+1).getPixels();
			tabData[3]=(float[])ret[3].getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				coordinates.set(0,x);
				for(int y=0;y<dimY;y++) {
					coordinates.set(1,y);
					coordIJ=dimX*y+x;
					values= img.getPixelAsVectorFloat64(coordinates);
					tabData[0][coordIJ]=(float)( values.get(0));
					tabData[1][coordIJ]=(float)( values.get(1));
					tabData[2][coordIJ]=(float)( values.get(2));
					tabData[3][coordIJ]=(float)( Math.sqrt ( values.get(0) * values.get(0) + values.get(1) * values.get(1) + values.get(2) * values.get(2) ) );
				}
			}
		}
		return ret;
	}
	
	/**
	 * Convert displacement field float to image plus array.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus[] convertDisplacementFieldFloatToImagePlusArray(Image img){
		IJ.log("Converting displacement field to ImagePlus array");
		int dimX=(int) img.getWidth(); int dimY=(int) img.getHeight(); int dimZ=(int) img.getDepth();
		VectorDouble voxSizes= img.getSpacing();		
		ImagePlus[]ret=new ImagePlus[3];
		for(int dim=0;dim<3;dim++) {
			ret[dim]=IJ.createImage("", dimX, dimY, dimZ, 32);
			Calibration cal = ret[dim].getCalibration();
			cal.setUnit("mm");
			cal.pixelWidth =voxSizes.get(0); cal.pixelHeight =voxSizes.get(1); cal.pixelDepth =voxSizes.get(2);
		}
		VectorUInt32 coordinates=new VectorUInt32(3);
		VectorFloat values=new VectorFloat(3);
		int coordIJ;
		for(int z=0;z<dimZ;z++) {
			coordinates.set(2,z);
			float [][]tabData=new float[3][];
			tabData[0]=(float[])ret[0].getStack().getProcessor(z+1).getPixels();
			tabData[1]=(float[])ret[1].getStack().getProcessor(z+1).getPixels();
			tabData[2]=(float[])ret[2].getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				coordinates.set(0,x);
				for(int y=0;y<dimY;y++) {
					coordinates.set(1,y);
					coordIJ=dimX*y+x;
					values= img.getPixelAsVectorFloat32(coordinates);
					tabData[0][coordIJ]=(float)( values.get(0));
					tabData[1][coordIJ]=(float)( values.get(1));
					tabData[2][coordIJ]=(float)( values.get(2));
				}
			}
		}
		return ret;
	}
	
	/**
	 * Convert image plus array to displacement field.
	 *
	 * @param imgs the imgs
	 * @return the ${e.g(1).rsfl()}
	 */
	public static Image convertImagePlusArrayToDisplacementField(ImagePlus []imgs){
		int dimX=(int) imgs[0].getWidth(); int dimY=(int) imgs[0].getHeight(); int dimZ=(int) imgs[0].getStackSize();
		double[]voxSizes=new double[] {imgs[0].getCalibration().pixelWidth,imgs[0].getCalibration().pixelHeight,imgs[0].getCalibration().pixelDepth};
		Image ret=new Image(dimX,dimY,dimZ,PixelIDValueEnum.sitkVectorFloat64);
		ret.setSpacing(doubleArrayToVectorDouble(voxSizes));
		VectorUInt32 coordinates=new VectorUInt32(3);
		VectorDouble values=new VectorDouble(3);
		int coordIJ;
		for(int z=0;z<dimZ;z++) {
			coordinates.set(2,z);
			float [][]tabData=new float[3][];
			tabData[0]=(float[])imgs[0].getStack().getProcessor(z+1).getPixels();
			tabData[1]=(float[])imgs[1].getStack().getProcessor(z+1).getPixels();
			tabData[2]=(float[])imgs[2].getStack().getProcessor(z+1).getPixels();
			for(int x=0;x<dimX;x++) {
				coordinates.set(0,x);
				for(int y=0;y<dimY;y++) {
					coordinates.set(1,y);
					coordIJ=dimX*y+x;
					values.set(0,tabData[0][coordIJ]);
					values.set(1,tabData[1][coordIJ]);
					values.set(2,tabData[2][coordIJ]);
					ret.setPixelAsVectorFloat64(coordinates,values);
				}
			}
		}
		System.gc();
		return ret;
	}

	/**
	 * Itk image to image plus.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus itkImageToImagePlus(Image img) {
		int dimX=(int) img.getWidth(); int dimY=(int) img.getHeight(); int dimZ=(int) img.getDepth();
		VectorDouble voxSizes= img.getSpacing();		
		int type=(img.getPixelID() ==PixelIDValueEnum.sitkUInt8 ? 8 : img.getPixelID() ==PixelIDValueEnum.sitkUInt16 ? 16 : img.getPixelID() ==PixelIDValueEnum.sitkFloat32 ? 32 : img.getPixelID() ==PixelIDValueEnum.sitkFloat64 ? 64 : 4);
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, ( type < 33 ? type : 32));
		Calibration cal = ret.getCalibration();
		cal.setUnit("mm");
		cal.pixelWidth =voxSizes.get(0); cal.pixelHeight =voxSizes.get(1); cal.pixelDepth =voxSizes.get(2);

		VectorUInt32 coordinates=new VectorUInt32(3);
		if(type==8) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				byte []tabData=(byte[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						tabData[dimX*y+x]=(byte)( img.getPixelAsUInt8(coordinates) & 0xff);
					}
				}
			}
		}

		if(type==16) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				short []tabData=(short[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						tabData[dimX*y+x]=(short)( img.getPixelAsUInt16(coordinates) & 0xffff);
					}
				}
			}
		}
	
		if(type==32) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				float []tabData=(float[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						tabData[dimX*y+x]=(float)( img.getPixelAsFloat(coordinates) );
					}
				}
			}
		}

		if(type==4) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				int []tabData=(int[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						tabData[dimX*y+x]=(int)( img.getPixelAsUInt32(coordinates) );
					}
				}
			}
		}

		if(type==64) {
			for(int z=0;z<dimZ;z++) {
				coordinates.set(2,z);
				float []tabData=(float[])ret.getStack().getProcessor(z+1).getPixels();
				for(int x=0;x<dimX;x++) {
					coordinates.set(0,x);
					for(int y=0;y<dimY;y++) {
						coordinates.set(1,y);
						tabData[dimX*y+x]=(float)( img.getPixelAsDouble(coordinates) );
					}
				}
			}
		}

		return ret;
	}
	
    /**
     * Itk image to image plus stack.
     *
     * @param img the img
     * @param slice the slice
     * @return the ${e.g(1).rsfl()}
     */
    public static ImagePlus itkImageToImagePlusStack(Image img,int slice) {
    	ImagePlus imgp=itkImageToImagePlus(img);
		imgp.setSlice(slice);
		return imgp;
    }
	
	/**
	 * Itk image to image plus slice.
	 *
	 * @param img the img
	 * @param slice the slice
	 * @return the image plus
	 */
	public static ImagePlus itkImageToImagePlusSlice(Image img,int slice) {
		int dimX=(int) img.getWidth(); int dimY=(int) img.getHeight(); int dimZ=(int) img.getDepth();
		if(slice>dimZ)slice=dimZ;
		if(slice<1)slice=1;
		VectorDouble voxSizes= img.getSpacing();		
		int type=(img.getPixelID() ==PixelIDValueEnum.sitkUInt8 ? 8 : img.getPixelID() ==PixelIDValueEnum.sitkUInt16 ? 16 : img.getPixelID() ==PixelIDValueEnum.sitkFloat32 ? 32 : 24);
		ImagePlus ret=IJ.createImage("", dimX, dimY, 1, type);
		Calibration cal = ret.getCalibration();
		cal.setUnit("mm");
		cal.pixelWidth =voxSizes.get(0); cal.pixelHeight =voxSizes.get(1); cal.pixelDepth =voxSizes.get(2);

		VectorUInt32 coordinates=new VectorUInt32(3);
		if(type==8) {
			coordinates.set(2,slice-1);
			byte []tabData=(byte[])ret.getStack().getProcessor(1).getPixels();
			for(int x=0;x<dimX;x++) {
				coordinates.set(0,x);
				for(int y=0;y<dimY;y++) {
					coordinates.set(1,y);
					tabData[dimX*y+x]=(byte)( img.getPixelAsUInt8(coordinates) & 0xff);
				}
			}
		}

		if(type==16) {
			coordinates.set(2,slice-1);
			short []tabData=(short[])ret.getStack().getProcessor(1).getPixels();
			for(int x=0;x<dimX;x++) {
				coordinates.set(0,x);
				for(int y=0;y<dimY;y++) {
					coordinates.set(1,y);
					tabData[dimX*y+x]=(short)( img.getPixelAsUInt16(coordinates) & 0xffff);
				}
			}
		}
	
		if(type==32) {
			coordinates.set(2,slice-1);
			float []tabData=(float[])ret.getStack().getProcessor(1).getPixels();
			for(int x=0;x<dimX;x++) {
				coordinates.set(0,x);
				for(int y=0;y<dimY;y++) {
					coordinates.set(1,y);
					tabData[dimX*y+x]=(float)( img.getPixelAsFloat(coordinates) );
				}
			}
		}
		if(type==24) {
			VitiDialogs.notYet("Conversion Slice ItkImage vers ImagePlus type RGB");
		}
		return ret;
	}
		
	
	
	
	
	
	


}
	