package io.github.rocsg.fijiyama.testromain;

import ij.IJ;
import ij.WindowManager;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import math3d.Point3d;

public class RandomTest {
    public static void main(String[] args) {
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
