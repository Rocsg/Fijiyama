package io.github.rocsg.fijiyama.registration;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.List;
import ij.gui.*;
import ij.process.ImageProcessor;
import io.github.rocsg.fijiyama.common.VitimageUtils;

public class Reslicer3D {
    
    public Reslicer3D(){

    }


    public static void main(String[]args){
        ImageJ ij=new ImageJ();
        String mainDir="/home/rfernandez/Bureau/A_Test/Aerenchyme/Xrays/Test_IJReslice/";
        String csvPath=mainDir+"test_csv1.csv";
        String imgPath=mainDir+"test_img1.tif";
        ImagePlus imgSource=IJ.openImage(imgPath);
        double[][]initPolyline=VitimageUtils.readDoubleTabFromCsv(csvPath);

        /* Debug 1
        for(int i=0;i<initPolyline.length;i++){
            System.out.println();
            for(int j=0;j<initPolyline[i].length;j++)System.out.print(initPolyline[i][j]+"  ");
        }*/

        ArrayList<double[]>resampledPolyline=resampleCurve(initPolyline, 50);
        
        /* Debug 2
        for(int i=0;i<resampledPolyline.size();i++){
            System.out.println();
            for(int j=0;j<resampledPolyline.get(i).length;j++)System.out.print(VitimageUtils.dou(resampledPolyline.get(i)[j])+"  ");
        }*/
        
        
        
        double[][][]initCosines=createDirectionCosinesFromPointList(resampledPolyline);

        /* Debug 3
        for(int i=0;i<initCosines.length;i++){
            System.out.println("cosines "+i+" size="+initCosines[0].length);
            System.out.println(TransformUtils.stringMatrixMN("", initCosines[i]));
        }*/
        ImagePlus result=resliceTiffImageAlongCurve(imgSource, resampledPolyline, initCosines, 100);
        result.show();
        return;
    }
    

    public static double[][][] createDirectionCosinesFromPointList(ArrayList<double[]>polyline) {
            int N = polyline.size();
            double[][][]directionCosines = new double[N-1][3][3];//Pt, nb axis, coord
            for (int i = 0; i < N - 1; i++) directionCosines[i][2]=TransformUtils.normalize(TransformUtils.vectorialSubstraction(polyline.get(i+1), polyline.get(i)));
    
            // Compute the first orthogonal axis
            double[]axisTemp= (Math.abs(directionCosines[0][2][1]) < 0.9999) ? new double[]{0.0, -1.0, 0.0} : new double[]{-1.0, 0.0, 0.0};
            directionCosines[0][0] = TransformUtils.normalize(TransformUtils.vectorialProduct(axisTemp, directionCosines[0][2]));
            directionCosines[0][1] = TransformUtils.normalize(TransformUtils.vectorialProduct(directionCosines[0][2], directionCosines[0][0]));
    
            // Compute iteratively the following orthogonal axis
            for (int i = 1; i < N - 1; i++) {
                directionCosines[i][1]= TransformUtils.normalize(TransformUtils.vectorialProduct(directionCosines[i][2], directionCosines[i-1][0]));
                directionCosines[i][0] = TransformUtils.normalize(TransformUtils.vectorialProduct(directionCosines[i][1], directionCosines[i][2]));
            }

            return directionCosines;
    }
    
    public static ArrayList<double[]> resampleCurve(double[][] initCurve, double step) {
        int n = initCurve.length;
        double[] x = new double[n];
        double[] y = new double[n];
        double[] z = new double[n];
        double[]distances= new double[n];
        distances[0]=0;
        for (int i = 1; i < n; i++) distances[i]=distances[i-1]+TransformUtils.norm(TransformUtils.vectorialSubstraction(initCurve[i], initCurve[i-1]));
        double totalDist=distances[n-1];

        // Convert curve to separate arrays for x, y, and z coordinates
        for (int i = 0; i < n; i++) {
            x[i] = initCurve[i][0];
            y[i] = initCurve[i][1];
            z[i] = initCurve[i][2];
        }

        // Create interpolators for each dimension
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction fx = interpolator.interpolate(distances, x);
        PolynomialSplineFunction fy = interpolator.interpolate(distances, y);
        PolynomialSplineFunction fz = interpolator.interpolate(distances, z);

        // Resample the curve
        ArrayList<double[]>resampledCurve = new ArrayList<double[]>();
        double summedDist=0;
        while(summedDist<totalDist){
            resampledCurve.add(new double[]{fx.value(summedDist),fy.value(summedDist),fz.value(summedDist)});
            summedDist+=step;
        }
        return resampledCurve;
    }
    

    public static ImagePlus resliceTiffImageAlongCurve(ImagePlus source,ArrayList<double[]>curve,double[][][]directionCosines,int radius){
        if(source.getType()!=ImagePlus.GRAY8)return null;
        int N = directionCosines.length;
        int[] dims = source.getDimensions();
        int[] dimsOut = new int[]{radius*2,radius*2, N};
        int xMaxIn=dimsOut[0];
        int xMaxOut=2*radius;
        ImagePlus result = ij.gui.NewImage.createImage("resliced", dimsOut[0], dimsOut[1], dimsOut[2], 8,ij.gui.NewImage.FILL_BLACK);
        byte [][] valSource=new byte[dims[2]][];
        byte [][] valResult=new byte[N][];
        for (int i = 0; i < N; i++) valResult[i]=(byte[])result.getStack().getProcessor(i+1).getPixels();
        for (int i = 0; i < dims[2]; i++) valSource[i]=(byte[])source.getStack().getProcessor(i+1).getPixels();

        for (int k = 0; k < N; k++) {
            if(k%100==0)System.out.println("Processing slice "+k+" / "+N);
            for(int i=0;i<2*radius;i++){
                for(int j=0;j<2*radius;j++){
                    double[]pt=TransformUtils.vectorialAddition(TransformUtils.vectorialAddition(curve.get(k),TransformUtils.multiplyVector(directionCosines[k][0],(i-radius))),TransformUtils.multiplyVector(directionCosines[k][1],j-radius));
                    if(pt[0]>=1 && pt[0]<dims[0]-1 && pt[1]>=1 && pt[1]<dims[1]-1 && pt[2]>=1 && pt[2]<dims[2]-1){
                        valResult[k][xMaxOut*j+i]=valSource[(int)Math.round(pt[2])][xMaxIn*(int)Math.round(pt[1])+(int)Math.round(pt[0])];
                        System.out.println(i+","+j+" is read "+(int)Math.round(pt[2]));
                    }
                }                
            }
        }
        return result;
    }

}
    











