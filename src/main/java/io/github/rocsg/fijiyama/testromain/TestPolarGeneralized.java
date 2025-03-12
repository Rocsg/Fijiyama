package io.github.rocsg.fijiyama.testromain;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.TransformUtils;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
public class TestPolarGeneralized {


    public static void main(String[] args) {
        ImageJ ij=new ImageJ();//Needed for testing


        //Main variables
        boolean isMercatorActivated=false;
        String imgPath="/home/rfernandez/Bureau/A_Test/Gargee/imgMov.tif";//The path of an image of a cutting, with the axis aligned with the z axis
        String roiPath="/home/rfernandez/Bureau/A_Test/Gargee/test.roi";//The path of a PolygonRoi, drawn in the trigonometric direction (counter clockwise)
        int halfWidth=120;//The half size (in X axis) of the resulting image
        int insideRadiusCoverage=80;//The distance within the object (along radius) to be taken into account
        int outsideRadiusCoverage=60;//The distance outside the object (along radius) to be taken into account
        Point.Double expectedCentralPoint=new Point.Double(45,179);//A point indicating roughly the original coordinates 
                                                                  //of the tissue that will be displayed in the center in the resulting image
        

        //Actions to transform an image into a generalized polar transformed image
        ImagePlus img=IJ.openImage(imgPath);
        Polygon poly=getRoiAsPolygon(img,roiPath);
        List<Point.Double> points = resamplePolygonUniform(poly, 1, true); //Evenly space the points along the curve       
        Point.Double center=massCenterOfPointList(points);//Compute the center from the evenly spaced points

//        points = resamplePolygonUniform(poly, 1, true,center);//Evenly space the points along the curve, but with a step of 1 orthogonally to radius       
        ImagePlus []res=applyGeneralizedPolarTransform(img, points, center, expectedCentralPoint,halfWidth, insideRadiusCoverage,outsideRadiusCoverage,isMercatorActivated);
        for(ImagePlus resi : res)resi.show();
        img.setTitle("Original image");
        img.show();
    }


    public static Polygon getRoiAsPolygon(ImagePlus img, String roiPath) {
        img.show();
        RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
		IJ.open(roiPath);
        rm.addRoi(null);
        Roi roi=rm.getRoi(0);
        Polygon poly = roi.getPolygon();
        img.killRoi();
        rm.reset();
        rm.close();
        img.hide();
        return poly;
    }

    public static Point.Double massCenterOfPointList(List<Point.Double> points) {
        double x=0;
        double y=0;
        for(Point.Double p:points) {
            x+=p.x;
            y+=p.y;
        }
        return new Point.Double(x/points.size(),y/points.size());
    }

    public static List<Point.Double> resamplePolygonUniform(Polygon poly, double step, boolean closed) {
        int n = poly.npoints;
        if (n < 2 || step <= 0) {
            // Degenerate cases: return copy of input
            return null;
        }

        // 1) Collect vertices into a list for easier handling
        List<Point> vertices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            vertices.add(new Point(poly.xpoints[i], poly.ypoints[i]));
        }

        // If we are treating it as a closed loop,
        // and the last vertex is not the same as the first,
        // append the first vertex to the end for easier iteration.
        boolean alreadyClosed = (vertices.get(0).distance(vertices.get(n - 1)) < 1e-12);
        if (closed && !alreadyClosed) {
            vertices.add(new Point(vertices.get(0).x, vertices.get(0).y));
        }

        // 2) Compute the total perimeter
        double totalPerimeter = 0.0;
        for (int i = 0; i < vertices.size() - 1; i++) {
            totalPerimeter += vertices.get(i).distance(vertices.get(i + 1));
        }

        // If totalPerimeter is extremely small, just return a copy of input
        if (totalPerimeter < 1e-12) {
            return null;
        }

        // 3) Walk along the boundary, placing points at each 'step' increment
        List<Point.Double> sampledPoints = new ArrayList<>();
        
        // Start at the first vertex
        double currentDistance = 0.0; // distance traveled along perimeter so far
        double nextSampleDist = 0.0;  // the next arc-length at which we place a sample

        // Add the very first vertex
        sampledPoints.add(new Point.Double(vertices.get(0).x, vertices.get(0).y));
        nextSampleDist += step;

        // Iterate over edges
        for (int i = 0; i < vertices.size() - 1; i++) {
            Point p1 = vertices.get(i);
            Point p2 = vertices.get(i + 1);
            double segmentLength = p1.distance(p2);

            // If segmentLength is effectively zero, continue
            if (segmentLength < 1e-12) {
                currentDistance += segmentLength;
                continue;
            }

            // We'll see if there's room on this segment to place sample points
            double startDistanceOnSegment = currentDistance;
            double endDistanceOnSegment = currentDistance + segmentLength;

            // While the next sample distance is within this segment, place points
            while (nextSampleDist <= endDistanceOnSegment + 1e-12) {
                double fraction = (nextSampleDist - startDistanceOnSegment) / segmentLength;
                if (fraction < 0.0) fraction = 0.0;
                if (fraction > 1.0) fraction = 1.0;

                // Interpolate linearly between p1 and p2
                double newX = p1.x + fraction * (p2.x - p1.x);
                double newY = p1.y + fraction * (p2.y - p1.y);
                sampledPoints.add(new Point.Double(newX, newY));

                nextSampleDist += step;
            }

            // We've finished placing points on this segment
            currentDistance += segmentLength;
        }

        // If closed, we might want to ensure that the very last sample is exactly the first point
        // (depending on the use case). If so, you can check distance to first sample and
        // optionally remove or adjust the last sample.

        return sampledPoints;
    }

    public static double tangentialDistance(Point.Double p1,Point.Double p2,Point.Double center){
        double[]vectp1cent=new double[]{center.x-p1.x,center.y-p1.y};
        double[]vectp1centNorm=TransformUtils.normalize(vectp1cent);
        double[]vectp1p2=new double[]{p2.x-p1.x,p2.y-p1.y};
        double[]vectp1p2Norm=TransformUtils.normalize(vectp1p2);
        double cosTeta=TransformUtils.scalarProduct(vectp1centNorm, vectp1p2Norm);
        double sinComponent=Math.sqrt(1-cosTeta*cosTeta);
        return sinComponent*TransformUtils.norm(vectp1p2);
    }

    public static Point.Double toPointDouble(Point p){
        return new Point.Double(p.x,p.y);
    }

    public static List<Point.Double> resamplePolygonUniformGeneralized(Polygon poly, double step, boolean closed,Point.Double center) {
        int n = poly.npoints;
        if (n < 2 || step <= 0) {
            // Degenerate cases: return copy of input
            return null;
        }

        // 1) Collect vertices into a list for easier handling
        List<Point> vertices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            vertices.add(new Point(poly.xpoints[i], poly.ypoints[i]));
        }

        // If we are treating it as a closed loop,
        // and the last vertex is not the same as the first,
        // append the first vertex to the end for easier iteration.
        boolean alreadyClosed = (vertices.get(0).distance(vertices.get(n - 1)) < 1e-12);
        if (closed && !alreadyClosed) {
            vertices.add(new Point(vertices.get(0).x, vertices.get(0).y));
        }

        // 2) Compute the total perimeter
        double totalPerimeter = 0.0;
        for (int i = 0; i < vertices.size() - 1; i++) {
            totalPerimeter += vertices.get(i).distance(vertices.get(i + 1));
        }

        // If totalPerimeter is extremely small, just return a copy of input
        if (totalPerimeter < 1e-12) {
            return null;
        }

        // 3) Walk along the boundary, placing points at each 'step' increment
        List<Point.Double> sampledPoints = new ArrayList<>();
        
        // Start at the first vertex
        double currentDistance = 0.0; // distance traveled along perimeter so far
        double nextSampleDist = 0.0;  // the next arc-length at which we place a sample

        // Add the very first vertex
        sampledPoints.add(new Point.Double(vertices.get(0).x, vertices.get(0).y));
        nextSampleDist += step;

        // Iterate over edges
        for (int i = 0; i < vertices.size() - 1; i++) {
            Point.Double p1 = toPointDouble(vertices.get(i));
            Point.Double p2 = toPointDouble(vertices.get(i + 1));
            double segmentLength = tangentialDistance(p1,p2,center);
            // If segmentLength is effectively zero, continue
            if (segmentLength < 1e-12) {
                currentDistance += segmentLength;
                continue;
            }

            // We'll see if there's room on this segment to place sample points
            double startDistanceOnSegment = currentDistance;
            double endDistanceOnSegment = currentDistance + segmentLength;

            // While the next sample distance is within this segment, place points
            while (nextSampleDist <= endDistanceOnSegment + 1e-12) {
                double fraction = (nextSampleDist - startDistanceOnSegment) / segmentLength;
                if (fraction < 0.0) fraction = 0.0;
                if (fraction > 1.0) fraction = 1.0;

                // Interpolate linearly between p1 and p2
                double newX = p1.x + fraction * (p2.x - p1.x);
                double newY = p1.y + fraction * (p2.y - p1.y);
                sampledPoints.add(new Point.Double(newX, newY));

                nextSampleDist += step;
            }

            // We've finished placing points on this segment
            currentDistance += segmentLength;
        }

        // If closed, we might want to ensure that the very last sample is exactly the first point
        // (depending on the use case). If so, you can check distance to first sample and
        // optionally remove or adjust the last sample.

        return sampledPoints;
    }

    public static ImagePlus []applyGeneralizedPolarTransform(ImagePlus imp,
                                                           List<Point.Double> contour,
                                                           Point.Double center,Point.Double expectedCentralPoint,int halfWidth,int insideRadiusCoverage,int outsideRadiusCoverage,boolean isMercatorActivated) {
        //Split image into 2D slices
        ImagePlus [] slices=VitimageUtils.stackToSlices(imp);
        ImagePlus [] slicesTrans=VitimageUtils.stackToSlices(imp);
        ImagePlus[]result=null;
        //Apply 2D function to the slices
        for (int i=0;i<slices.length;i++){
            result=applyGeneralizedPolarTransform2D(slices[i],contour,center,expectedCentralPoint,halfWidth,insideRadiusCoverage,outsideRadiusCoverage,isMercatorActivated);
            slicesTrans[i]=result[0];
        }
        ImagePlus combinedSlices= VitimageUtils.slicesToStack(slicesTrans);                            
        return new ImagePlus[]{combinedSlices,result[1],result[2],result[3],result[4],result[5],result[6],result[7],result[8]}; 
    }

    public static ImagePlus[] applyGeneralizedPolarTransform2D(ImagePlus imp,
                                                           List<Point.Double> contour,
                                                           Point.Double center,Point.Double expectedCentralPoint,int halfWidth,int insideRadiusCoverage,int outsideRadiusCoverage,boolean isMercatorActivated) {
        // Récupération du processeur d'images d'origine
        ImageProcessor ip = imp.getProcessor();
        int X = ip.getWidth();
        int Y = ip.getHeight();
        int Yprime=insideRadiusCoverage+outsideRadiusCoverage+1;
        double deltaR=1;
        // Création de l'image de sortie
         
        //Find the index of the point of contour that is the nearest to expectedCentralPoint
        double minDist=Double.MAX_VALUE;
        int index=0;
        for(int i=0;i<contour.size();i++){
            double dist=contour.get(i).distance(expectedCentralPoint);
            if(dist<minDist){
                minDist=dist;
                index=i;
            }
        }
       
        //set startIndex and stopIndex to be the index of this nearest point, minus or plus halfWidth
        int startIndex=index-halfWidth;
        int stopIndex=index+halfWidth;
        if(startIndex<0){
            startIndex=0;
        }
        if(stopIndex>=contour.size()-1){
            stopIndex=contour.size()-1;
        }

        //replace the list contour with a crop of it between stopIndex and startIndex
        List<Point.Double> newContour=new ArrayList<Point.Double>();
        for(int i=startIndex;i<=stopIndex;i++){
            newContour.add(contour.get(i));
        }
        contour=newContour;

        int nAngles=contour.size();
        int Xprime=nAngles;
        ImageProcessor resultProcessor = ip.createProcessor(Xprime, Yprime);//TODO : depend on the size factor
        //Images to store the correspondences in x,y, and the widthfactor
        ImageProcessor widthPolarMapProcessor = ip.createProcessor(Xprime, Yprime).convertToFloatProcessor();//TODO : depend on the size factor
        ImageProcessor sinePolarMapProcessor = ip.createProcessor(Xprime, Yprime).convertToFloatProcessor();//TODO : depend on the size factor
        ImageProcessor xPolarMapProcessor = ip.createProcessor(Xprime, Yprime).convertToFloatProcessor();//TODO : depend on the size factor
        ImageProcessor yPolarMapProcessor = ip.createProcessor(Xprime, Yprime).convertToFloatProcessor();//TODO : depend on the size factor
        //In case of
        for(int x=0;x<Xprime;x++)for(int y=0;y<Yprime;y++){
            widthPolarMapProcessor.putPixelValue(x,y,0);
            sinePolarMapProcessor.putPixelValue(x,y,0);
            widthPolarMapProcessor.putPixelValue(x,y,0);
            yPolarMapProcessor.putPixelValue(x,y,0);
        }

        ImageProcessor width2DMapProcessor = ip.createProcessor(X, Y).convertToFloatProcessor();//TODO : depend on the size factor
        ImageProcessor sine2DMapProcessor = ip.createProcessor(X, Y).convertToFloatProcessor();//TODO : depend on the size factor
        ImageProcessor x2DMapProcessor = ip.createProcessor(X, Y).convertToFloatProcessor();//TODO : depend on the size factor
        ImageProcessor y2DMapProcessor = ip.createProcessor(X, Y).convertToFloatProcessor();//TODO : depend on the size factor
        for(int x=0;x<X;x++)for(int y=0;y<Y;y++){
            width2DMapProcessor.putPixelValue(x,y,0);
            sine2DMapProcessor.putPixelValue(x,y,0);
            x2DMapProcessor.putPixelValue(x,y,0);
            y2DMapProcessor.putPixelValue(x,y,0);
        }

       



        // Remplissage de l'image transformée

        if(!isMercatorActivated){//Simple projection, with constant path length along the radius
            
            for (int XpIndex = 0; XpIndex < nAngles; XpIndex++) {
                double xCont=contour.get(XpIndex).x;
                double yCont=contour.get(XpIndex).y;
                double xCont2=contour.get(XpIndex+(XpIndex==(nAngles-1) ? -1 : 1)).x;
                double yCont2=contour.get(XpIndex+(XpIndex==(nAngles-1) ? -1 : 1)).y;
                double[]vectCurveNorm=new double[]{xCont2-xCont,yCont2-yCont,0};
                vectCurveNorm=TransformUtils.normalize(vectCurveNorm);

                double[]vectToCenter=new double[]{center.x-contour.get(XpIndex).x,center.y-contour.get(XpIndex).y,0};
                double[]vectToCenterNorm=TransformUtils.multiplyVector(vectToCenter, 1.0/TransformUtils.norm(vectToCenter));

                double cosine=TransformUtils.scalarProduct(vectCurveNorm, vectToCenterNorm);
                double sine=Math.sqrt(1-cosine*cosine);

                double distanceToCenter=TransformUtils.norm(vectToCenter);
                for (double r = -outsideRadiusCoverage/deltaR; r < insideRadiusCoverage/deltaR; r++  ) {
                    double radius=r*deltaR;
                    double[]vectContToPoint=TransformUtils.multiplyVector(vectToCenterNorm, radius);
                    double xSample = xCont + vectContToPoint[0];
                    double ySample = yCont + vectContToPoint[1];

                    // Récupération du pixel original (interpolation bilinéaire)
                    if (xSample >= 0 && xSample < X && ySample >= 0 && ySample < Y) {
                        float pixelVal = (float) ip.getInterpolatedValue(xSample, ySample);
                        resultProcessor.putPixelValue(XpIndex, (int)(Yprime-1-r-(outsideRadiusCoverage/deltaR)), pixelVal);
                        double widthSizeFactor=0;
                        if(radius>=distanceToCenter)widthSizeFactor=Double.MAX_VALUE;
                        else widthSizeFactor =distanceToCenter/(distanceToCenter-radius);
                        widthPolarMapProcessor.putPixelValue(XpIndex, (int)(Yprime-1-r-(outsideRadiusCoverage/deltaR)), widthSizeFactor);
                        sinePolarMapProcessor.putPixelValue(XpIndex, (int)(Yprime-1-r-(outsideRadiusCoverage/deltaR)), sine);
                        xPolarMapProcessor.putPixelValue(XpIndex, (int)(Yprime-1-r-(outsideRadiusCoverage/deltaR)), xSample);
                        yPolarMapProcessor.putPixelValue(XpIndex, (int)(Yprime-1-r-(outsideRadiusCoverage/deltaR)), ySample);

                        width2DMapProcessor.putPixelValue((int)xSample, (int)ySample, widthSizeFactor);
                        sine2DMapProcessor.putPixelValue((int)xSample, (int)ySample, sine);
                        x2DMapProcessor.putPixelValue((int)xSample, (int)ySample, XpIndex);
                        y2DMapProcessor.putPixelValue((int)xSample, (int)ySample, (int)(Yprime-1-r-(outsideRadiusCoverage/deltaR)));

                    } else {
                        resultProcessor.putPixelValue(XpIndex, (int)(Yprime-1-r-(outsideRadiusCoverage/deltaR)), 0);
                    }
                }
            }
        }
 
        // Création de l'ImagePlus finale
        ImagePlus result = new ImagePlus("GeneralizedPolarTransform", resultProcessor);
        VitimageUtils.copyImageCalibrationAndRange(result, imp);

        ImagePlus imgwpol=new ImagePlus("Width factor polar",widthPolarMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgwpol, imp);
        ImagePlus imgspol=new ImagePlus("Sine factor polar",sinePolarMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgspol, imp);
        ImagePlus imgxpol=new ImagePlus("X correspondence polar",xPolarMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgxpol, imp);
        ImagePlus imgypol=new ImagePlus("Y correspondence polar",yPolarMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgypol, imp);

        ImagePlus imgw2d=new ImagePlus("Width factor 2d",width2DMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgw2d, imp);
        ImagePlus imgs2d=new ImagePlus("Sine factor 2d",sine2DMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgs2d, imp);
        ImagePlus imgx2d=new ImagePlus("X correspondence 2d",x2DMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgx2d, imp);
        ImagePlus imgy2d=new ImagePlus("Y correspondence 2d",y2DMapProcessor);
        VitimageUtils.copyImageCalibrationAndRange(imgy2d, imp);

        return new ImagePlus[]{result,imgwpol,imgspol,imgxpol,imgypol,imgw2d,imgs2d,imgx2d,imgy2d};
    }

    
 
}
