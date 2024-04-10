package io.github.rocsg.fijiyama.testromain;

import ij.IJ;
import ij.WindowManager;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import math3d.Point3d;

public class RandomTest {
    public static void main(String[] args) {
        double[]vectorX=new double[]{0.9787218841449,0.1256730196066,-0.16220285336216};
        double[]vectorY=new double[]{0.17485594309579,-0.09716430040583,0.9797879861943};
        double[]vectorZ=TransformUtils.vectorialProduct(vectorX, vectorY);
        TransformUtils.printVector(vectorZ, "stuff");
        System.exit(0);

        System.out.println("Hello World!");
        Point3d[][]pointTab=new Point3d[][]{
            {new Point3d(0,0,0), new Point3d(1,0,0), new Point3d(0,1,0),   new Point3d(0,0,1), new Point3d(1,0,1), new Point3d(0,1,1)},
            {new Point3d(0,0,0), new Point3d(1.5,0,0), new Point3d(0,1.5,0),   new Point3d(0,0,1.5), new Point3d(1.5,0,1.5), new Point3d(0,1.5,1.5)}
        };
		for(int i=0;i<pointTab.length;i++) {
			for(int j=0;j<pointTab[i].length;j++) {
				System.out.println("Point "+i+" "+j+" : "+pointTab[i][j].x+","+pointTab[i][j].y+","+pointTab[i][j].z);
			}
		}

        ItkTransform trans=ItkTransform.estimateBestAffine3D(pointTab[1],pointTab[0]);
        System.out.println(trans);
    }
}
