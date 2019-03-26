package com.vitimage;

import org.itk.simple.Image;
import org.itk.simple.ImageFileReader;
import org.itk.simple.ImageFileWriter;
import org.itk.simple.PixelIDValueEnum;
import org.itk.simple.VectorDouble;
import org.itk.simple.VectorUInt32;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import math3d.Point3d;

public interface ItkImagePlusInterface {

	
	public static void runInterfaceTestSequence() {
		new ImageJ();
		ImagePlus ijImg1=IJ.openImage("/home/fernandr/Bureau/Test/ITK/4_INTERTIME/T1_J35_to_ref.tif");
		Image itkImg1=imagePlusToItkImage(ijImg1);
		ImagePlus ijImg1Back=itkImageToImagePlus(itkImg1);
		ijImg1Back.getProcessor().setMinAndMax(0,255);
		ijImg1Back.setTitle("Résultat de la double conversion aller retour entre IJ et ITK");
		ijImg1.show();
		ijImg1Back.show();
		VitimageUtils.waitFor(10000);
		int[]tabI=new int[] {1,2,3,4,5};
		VectorUInt32 vectI=ItkImagePlusInterface.intArrayToVectorUInt32(tabI);
		int[]tabI2=ItkImagePlusInterface.vectorUInt32ToIntArray(vectI);
		for(int i=0;i<tabI.length;i++) {
			System.out.println("tabI1 : "+tabI[i]);
			System.out.println("vect : "+vectI.get(i));
			System.out.println("tabI2 : "+tabI2[i]);
		}
		System.out.println();
		
		double[]tabD=new double[] {1,2,3,4,5};
		VectorDouble vectD=doubleArrayToVectorDouble(tabD);
		double[]tabD2=vectorDoubleToDoubleArray(vectD);
		for(int i=0;i<tabD.length;i++) {
			System.out.println("tabD1 : "+tabD[i]);
			System.out.println("vect : "+vectD.get(i));
			System.out.println("tabD2 : "+tabD2[i]);
		}
		System.out.println();

	}
	
	
	/**
	 * Helper functions to convert between ImagePlus (ImageJ format) and Image (org.itk.simple format)
	 * 
	 */
	public static Image imagePlusToItkImage(ImagePlus img) {
		int dimX=img.getWidth(); int dimY=img.getHeight(); int dimZ=img.getStackSize();
		double[]voxSizes=new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
		int type=(img.getType()==ImagePlus.GRAY8 ? 8 : img.getType()==ImagePlus.GRAY16 ? 16 : img.getType()==ImagePlus.GRAY32 ? 32 : 24);
		Image ret=new Image(dimX,dimY,dimZ,type==8 ? PixelIDValueEnum.sitkUInt8 : type==16 ? PixelIDValueEnum.sitkUInt16 : PixelIDValueEnum.sitkFloat32);
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
		else VitiDialogs.notYet("Conversion ImagePlus vers ItkImage type RGB");
		return ret;
	}
	
	public static ImagePlus itkImageToImagePlus(Image img) {
		int dimX=(int) img.getWidth(); int dimY=(int) img.getHeight(); int dimZ=(int) img.getDepth();
		VectorDouble voxSizes= img.getSpacing();		
		int type=(img.getPixelID() ==PixelIDValueEnum.sitkUInt8 ? 8 : img.getPixelID() ==PixelIDValueEnum.sitkUInt16 ? 16 : img.getPixelID() ==PixelIDValueEnum.sitkFloat32 ? 32 : 24);
		ImagePlus ret=IJ.createImage("", dimX, dimY, dimZ, type);
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
		if(type==24) {
			VitiDialogs.notYet("Conversion ItkImage vers ImagePlus type RGB");
		}
		return ret;
	}
		
	public default ImagePlus itkImageToImagePlusSlice(Image img,int slice) {
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
		
	public default Image naiveImagePlusToItkImage(ImagePlus img) {
		IJ.save(img,"/home/fernandr/Temp/img.tif");	
		ImageFileReader reader = new ImageFileReader();
		reader.setFileName("/home/fernandr/Temp/img.tif");
		return(reader.execute());
	}
	
	public default ImagePlus naiveItkImageToImagePlus(Image img) {
		ImageFileWriter imgW=new ImageFileWriter();
		imgW.execute(img,"/home/fernandr/Temp/img.tif",false);
		return(IJ.openImage("/home/fernandr/Temp/img.tif"));
	}

	
	
	/**
	 * Helper functions to convert between arrays (java std format) and VectorDouble (org.itk.simple format)
	 * 
	 */
	public static VectorUInt32 intArrayToVectorUInt32(int[]array) {
		VectorUInt32 vect=new VectorUInt32(array.length);
		for(int i=0;i<array.length ;i++)vect.set(i,array[i]);
		return vect;
	}

	public static int[] vectorUInt32ToIntArray(VectorUInt32 vect) {
		int[] tab=new int[(int) vect.size()];
		for(int i=0;i<tab.length ;i++)tab[i]=(int) vect.get(i);
		return tab;
	}

	public static VectorDouble doubleArrayToVectorDouble(double[]array) {
		VectorDouble vect=new VectorDouble(array.length);
		for(int i=0;i<array.length ;i++)vect.set(i,array[i]);
		return vect;
	}

	public static double[] vectorDoubleToDoubleArray(VectorDouble vect) {
		double[] tab=new double[(int) vect.size()];
		for(int i=0;i<tab.length ;i++)tab[i]=vect.get(i);
		return tab;
	}

	public static Point3d vectorDoubleToPoint3d(VectorDouble vect) {
		return new Point3d(vect.get(0) ,vect.get(1), vect.get(2) );
	}
	

	public default double cuteDouble(double d,int precision){
		if (d < 1.0/Math.pow(10, precision))return 0;
		return (double)(Math.round(d * Math.pow(10, precision))/Math.pow(10, precision));
	}

	/**
	 * Helper enums to ease ItkRegistrationManager description and usage. All of them are actually options of simple ITK's ImageRegistrationMethod
	 * 
	 */	
	public enum Transformation3DType{
		TRANSLATION,
		EULER,
		EULER2D,
		VERSOR,
		AFFINE,
		SIMILARITY
	}

	public enum MetricType{
		JOINT,
		MEANSQUARE,
		CORRELATION,
		MATTES,
		ANTS,
		DEMONS
	}
	
	public enum OptimizerType{
		GRADIENT,
		GRADIENT_REGULAR_STEP,
		GRADIENT_LINE_SEARCH,
		GRADIENT_CONJUGATE,
		LBFGSB,
		LBFGS2,
		POWELL,
		EVOLUTIONARY,
		EXHAUSTIVE,
		AMOEBA
	}

	public enum ScalerType{
		NONE,
		MANUAL,
		SCALER_INDEX,
		SCALER_PHYSICAL,
		JACOBIAN_VERSOR,
	}

	public enum CenteringStrategy{
		NONE,
		IMAGE_CENTER,
		MASS_CENTER
	}

	public enum SamplingStrategy{
		NONE,
		REGULAR,
		RANDOM
	}

}
	