/*
 * 
 */
package io.github.rocsg.fijiyama.common;
//TODO common

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.itk.simple.DisplacementFieldTransform;
import org.itk.simple.Image;
import org.itk.simple.ImageFileWriter;
import org.itk.simple.PixelIDValueEnum;
import org.itk.simple.SimpleITK;
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
public class ItkImagePlusInterface {
	
	public static int flag_fijiyama_IO=0;//Unitialized
	public static String pathToTempImg="";
	public static final int FLAG_UNSET=0;
	public static final int FLAG_ON=1;
	public static final int FLAG_OFF=2;
	

	public static void main(String[] args) {
		String str="/home/rfernandez/Bureau/Temp/TestGargeeDicomStuff/Volumetric_Images/Sequence_1.tif";
		Timer t=new Timer();
		t.print("Ouverture image");
		ImagePlus img=IJ.openImage(str);
		t.print("Ok");
		VitimageUtils.printImageResume(img);

		t.print("Conversion vers ITK");
		Image itkImg=imagePlusToItkImageNew(img);
		t.print("Ok");

		t.print("Conversion vers ImageJ");
		ImagePlus ijImg=itkImageToImagePlusNew(itkImg);
		t.print("Ok");
	}
	
/*
	public static int getFlag(){
		if(ItkImagePlusInterface.flag_fijiyama_IO!=ItkImagePlusInterface.FLAG_UNSET)return ItkImagePlusInterface.flag_fijiyama_IO;
		if(!(new File(getPathToFlag()).exists())){
			setFlag();
			return ItkImagePlusInterface.flag_fijiyama_IO;
		}

		String s=VitimageUtils.readStringFromFile(getPathToFlag());
		ItkImagePlusInterface.flag_fijiyama_IO=Integer.parseInt(s);
		return ItkImagePlusInterface.flag_fijiyama_IO;
	}

*/

	public static void setSpeedupOnForFiji() {
		if(ItkImagePlusInterface.flag_fijiyama_IO==FLAG_ON) {			
			ItkImagePlusInterface.flag_fijiyama_IO=FLAG_OFF;
			IJ.showMessage("Now speedup Off");
		}
		else {			
			ItkImagePlusInterface.flag_fijiyama_IO=FLAG_ON;
			ItkImagePlusInterface.pathToTempImg=IJ.getDirectory("imagej")+"macros"+"/temp_img.tif";
			if(!ItkImagePlusInterface.pathToTempImg.contains("Fiji.app"))ItkImagePlusInterface.pathToTempImg=IJ.getDirectory("imagej")+".images/temp_img.tif";
			IJ.showMessage("Now speedup On");
		}
	}
	
	public static void setFlag(){
		//If the flag is already set, go.
		if(ItkImagePlusInterface.flag_fijiyama_IO!=ItkImagePlusInterface.FLAG_UNSET)return;

		//If the flag can be read in Fiji.App, go.
		if(new File(IJ.getDirectory("imagej")+"macros"+"/flag_fijiyama_IO.txt").exists()){
			String s=VitimageUtils.readStringFromFile(IJ.getDirectory("imagej")+"macros"+"/flag_fijiyama_IO.txt");
			ItkImagePlusInterface.flag_fijiyama_IO=Integer.parseInt(s);
			ItkImagePlusInterface.pathToTempImg=IJ.getDirectory("imagej")+"macros"+"/temp_img.tif";
			return;
		}
		
		//If the flag can be read in some developer config, go.
		if(new File(IJ.getDirectory("imagej")+".images/flag_fijiyama_IO.txt").exists()){
			String s=VitimageUtils.readStringFromFile(IJ.getDirectory("imagej")+".images/flag_fijiyama_IO.txt").replace("\n", "");
			System.out.println(s);
			ItkImagePlusInterface.flag_fijiyama_IO=Integer.parseInt(s);
			ItkImagePlusInterface.pathToTempImg=IJ.getDirectory("imagej")+".images/temp_img.tif";
			return;
		}
		
		//Else, look for writing it somewhere
		String pathToFlag= IJ.getDirectory("imagej")+"macros"+"/flag_fijiyama_IO.txt";
		if(!pathToFlag.contains("Fiji.app"))pathToFlag=IJ.getDirectory("imagej")+".images/flag_fijiyama_IO.txt";
		if(! (new File (new File(pathToFlag).getParent()).exists())){
			pathToFlag=VitiDialogs.chooseDirectoryNiceUI("Provide a dir for temp files", "Please provide a directory for temporary files");
			pathToFlag=pathToFlag+"/flag_fijiyama_IO.txt";
			IJ.showMessage("Chosen dir:"+pathToFlag);
			
		}

		//Evaluate the possibility of a speedup
		boolean speedup=VitiDialogs.getYesNoUI("Fijiyama speedup update", "An update to Fijiyama allows to speedup computation, depending on your hardware configuration.\n"+
												"Yes : automatic test of your configuration (10 seconds test)\n"+
												"No  : don t test and keep the old behaviour ");
		if(!speedup){
			ItkImagePlusInterface.flag_fijiyama_IO=ItkImagePlusInterface.FLAG_OFF;
			
			try {Files.write(Paths.get(pathToFlag), new String(""+ItkImagePlusInterface.flag_fijiyama_IO).getBytes(), StandardOpenOption.CREATE); } catch (IOException e) {e.printStackTrace();}			
			IJ.showMessage("The optimization is ignored.\n"+
							"You can run again this test by simply removing the flag file located at "+pathToFlag+" , then starting back Fijiyama.");
			return ;
		}

		double[]results=testConfiguration();
		if(results[1]>=results[0]) {
			IJ.showMessage("Your configuration is not optimal for this speedup (maybe Fiji is installed on a HDD drive ?). Optimization is ignored.\n"+
							"You can run again this test by simply removing the flag file located at "+pathToFlag+" , then starting back Fijiyama.");
			ItkImagePlusInterface.flag_fijiyama_IO=ItkImagePlusInterface.FLAG_OFF;
			try {Files.write(Paths.get(pathToFlag), new String(""+ItkImagePlusInterface.flag_fijiyama_IO).getBytes(), StandardOpenOption.CREATE); } catch (IOException e) {e.printStackTrace();}			
		}
		else{
			IJ.showMessage("Your configuration is optimal for this speedup. Estimated acceleration="+VitimageUtils.dou(results[0]/results[1])+". Optimization activated now.\n");
			ItkImagePlusInterface.flag_fijiyama_IO=ItkImagePlusInterface.FLAG_ON;
			ItkImagePlusInterface.pathToTempImg=new File(pathToFlag).getParent()+"/temp_img.tif";
			try {Files.write(Paths.get(pathToFlag), new String(""+ItkImagePlusInterface.flag_fijiyama_IO).getBytes(), StandardOpenOption.CREATE); } catch (IOException e) {e.printStackTrace();}			
		}
	} 

	
	
	public static double[] testConfiguration(){
		IJ.showMessage("Now starting a speedup test. Please click OK, then wait a few seconds.");

		//Create an empty stack of 200 x 200 x 200
		ImagePlus img=IJ.createImage("Test", "8-bit black", 200, 200, 200);


		//Test old behaviour
		Timer t=new Timer();
		t.print("Starting measuring old behaviour");
		for(int i=0;i<10;i++) {
			Image a=ItkImagePlusInterface.imagePlusToItkImageOld(img);
			ImagePlus b=ItkImagePlusInterface.itkImageToImagePlusOld(a);
		}
		t.print("Time for old behaviour");
		double t0=t.getTime();

		//Test new behaviour
		t=new Timer();
		for(int i=0;i<10;i++) {
			String s="/home/rfernandez/Bureau/temp.tif";
			IJ.saveAsTiff(img, s);
			Image image = SimpleITK.readImage(s, PixelIDValueEnum.sitkUnknown);

			ImageFileWriter writer = new ImageFileWriter();
	        writer.setFileName(s);
	        writer.execute(image);
			ImagePlus b=IJ.openImage(s);
		}
		t.print("Time for new behaviour");
		double t1=t.getTime();

		return new double[]{t0,t1};
	}


	
	
	
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
	

	
	public static ImagePlus itkImageToImagePlus(Image img) {
		if(flag_fijiyama_IO==FLAG_ON)return itkImageToImagePlusNew(img);
		else return itkImageToImagePlusOld(img);
	}
		
	public static Image imagePlusToItkImage(ImagePlus img) {
		if(flag_fijiyama_IO==FLAG_ON)return imagePlusToItkImageNew(img);
		else return imagePlusToItkImageOld(img);	
	}
	
	
	public static ImagePlus itkImageToImagePlusNew(Image img) {
		VectorDouble v=img.getSpacing();
		ImageFileWriter writer = new ImageFileWriter();
        writer.setFileName(pathToTempImg);
        writer.execute(img);
		ImagePlus imp=IJ.openImage(pathToTempImg);
		VitimageUtils.adjustVoxelSize(imp, vectorDoubleToDoubleArray(v) );
		
		return imp;
	}
		
	public static Image imagePlusToItkImageNew(ImagePlus img) {
		double[]voxSizes=new double[] {img.getCalibration().pixelWidth,img.getCalibration().pixelHeight,img.getCalibration().pixelDepth};
		IJ.saveAsTiff(img, pathToTempImg);
		Image image = SimpleITK.readImage(pathToTempImg, PixelIDValueEnum.sitkUnknown);
		image.setSpacing(doubleArrayToVectorDouble(voxSizes));
		return image;
	}

	/**
	 * Image plus to itk image.
	 *
	 * @param img the img
	 * @return the image
	 */
	/*Helper functions to convert between ImagePlus (ImageJ format) and Image (org.itk.simple format) */
	public static Image imagePlusToItkImageOld(ImagePlus img) {
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
	 * Itk image to image plus.
	 *
	 * @param img the img
	 * @return the image plus
	 */
	public static ImagePlus itkImageToImagePlusOld(Image img) {
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
    	ImagePlus imgp=null;
    	if(flag_fijiyama_IO!=FLAG_ON) imgp	=itkImageToImagePlusOld(img);
    	else  imgp=itkImageToImagePlusNew(img);
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

	
	
	
	
	
	


}
	