package com.vitimage;

import java.util.ArrayList;

/**
 * A 2D cubic spline class with support for arbitrary additional spline data
 * arrays. 
 * @author Shinmera
 * @license GPLv3
 * @version 1.1.0
 */
public class Spline {
    private ArrayList<double[]> pointData;
    private ArrayList<double[]> splineData;
    private ArrayList<double[]> intData;
    private double[] s;
    private int pointDistance = 0;
    private int pointAmount = 0;
    
    /**
     * Create a new Spline instance with the X and Y path data. Additional
     * spline data arrays can be added later on. Note that the interpolated
     * spline points are calculated immediately, so this can be time consuming!
     * @param xs Array of spline X points.
     * @param ys Array of spline Y points.
     */
    public Spline(double[] xs,double[] ys){
        if(xs.length == 0 || xs.length != ys.length)
            throw new IllegalArgumentException("Array sizes equal to zero or do not match!");
        pointData = new ArrayList<double[]>();
        splineData = new ArrayList<double[]>();
        intData = new ArrayList<double[]>();
        
        calculateS(xs, ys);
        setPointDistance(2);
        
        addPointDataArray(xs);
        addPointDataArray(ys);
    }
    
    /**
     * Adds a new array of data to the spline. The interpolated data is
     * calculated immediately, so this function can be time consuming!
     * @param x The point data to interpolate along the spline.
     * @return The index of the data in the spline.
     */
    public int addPointDataArray(double[] x){
        pointData.add(x);
        double[] cx = calculateC(x);
        splineData.add(cx);
        intData.add(calculateRealPoints(x, cx));
        return pointData.size()-1;
    }
    
    /**
     * Set the distance between each interpolated point.
     * Note that the amount of points is changed through this as well.
     * @param n The distance in units.
     */
    public void setPointDistance(int n){
        if(pointDistance == n)return;
        pointDistance=n;
        pointAmount=(int)Math.ceil(s[s.length-1]/pointDistance)+1;
        calculateAllRealPoints();
    }
    public int getPointDistance(){return pointDistance;}
    
    /**
     * Set the amount of interpolated points.
     * Note that the point distance is changed through this as well.
     * @param n The amount of points.
     */
    public void setPointAmount(int n){
        if(pointAmount == n)return;
        pointAmount=n;
        pointDistance=(int)(s[s.length-1]/(pointAmount-1));
        calculateAllRealPoints();
    }
    public int getPointAmount(){return pointAmount;}
    
    /**
     * Returns the spline interpolated X data.
     * @return An array of interpolated values.
     */
    public double[] getIntX(){return getInt(0);}
    /**
     * Returns the spline interpolated Y data.
     * @return An array of interpolated values.
     */
    public double[] getIntY(){return getInt(1);}
    /**
     * Returns the spline data of index n. Note that n=0 returns the interpolated
     * X data and n=1 returns the interpolated Y data.
     * @param n The data index.
     * @return An array of interpolated values.
     */
    public double[] getInt(int n){return intData.get(n);}
    
    /**
     * Calculates the distances between the start and the given points.
     */
    private void calculateS(double[] x, double[] y){
        s = new double[x.length];
        if(x.length <= 1)return;
        boolean err = false;
        
        double a=0,b=0,c=0,d=0,e=0,f=0,g=0,dn=0;
        for(int i=1; i<x.length-1; i++){
            a=x[i]-x[i-1];
            b=y[i]-y[i-1];
            c=x[i+1]-x[i];
            d=y[i+1]-y[i];
            e=x[i+1]-x[i-1];
            f=y[i+1]-y[i-1];
            dn=a*d-b*c;
            if(dn == 0){
                g=1;
            }else{
                double dz = c*e+d*f;
                if(dz == 0){
                    g = Math.PI/2.0;
                }else{
                    dz = dz/dn;
                    g = Math.sqrt(1+dz*dz)*Math.atan(1.0/Math.abs(dz));
                }
            }
            double ds=g*Math.sqrt(a*a+b*b);
            if(ds < 0){
                err = true;
                break;
            }else{
                s[i] = s[i-1]+ds;
            }
        }
        //Calculate the data for the last point.
        if(!err){
            g=a;
            a=-c;
            c=-g;
            g=b;
            b=-d;
            d=-g;
            e=-e;
            f=-f;
            dn=a*d-b*c;
            if(dn == 0){
                g = 1;
            }else{
                double dz = c*e+d*f;
                if(dz == 0){
                    g = Math.PI/2.0;
                }else{
                    dz = dz/dn;
                    g = Math.sqrt(1+dz*dz)*Math.atan(1.0/Math.abs(dz));
                }
            }
            double ds = g*Math.sqrt(a*a+b*b);
            if(ds < 0){
                err = true;
            }else{
                s[x.length-1] = s[x.length-2]+ds;
            }
        }
        
        //An error occurred. Approximate the distance through a simple pythagoras.
        if(err){
            for(int i=1; i<x.length; i++){
                double ds = Math.sqrt(Math.pow(x[i-1]-x[i],2)+Math.pow(y[i-1]-y[i],2));
                s[i] = s[i-1]+ds;
            }
        }
    }
    
    /**
     * Calculates the spline data for the given positional parameter.
     * Note that the distance array has to have been calculated prior for this
     * function to work, or an exception will be thrown
     * @param x The positional data to interpolate.
     * @return The spline data for x.
     */
    private double[] calculateC(double[] x){
        double[] u = new double[x.length];
        double[] c = new double[x.length];
        c[0] = 0;
        u[0] = 0;
        for(int i=1;i < c.length-1; i++){
            double q = (s[i] - s[i-1]) / (s[i+1] - s[i-1]);
            double p = q * c[i-1] + 2;
            c[i] = (q-1) / p;
            u[i] = (6 * ((x[i+1] - x[i]) / (s[i+1] - s[i]) - (x[i] - x[i-1]) / (s[i] - s[i-1])) / (s[i+1] - s[i-1]) - q*u[i-1]) / p;
        }
        c[c.length-1]=0;
        for(int i = c.length-2;i >= 0; i--){
            c[i] = c[i] * c[i+1] + u[i];
        }
        return c;
    }
    
    /**
     * Calculate the position of a point along the spline.
     * @param x Array of fixed points to interpolate between.
     * @param c Array of spline data.
     * @param p The position along the path.
     * @return The interpolated position.
     */
    private double interpolate(double[] x, double[] c, double p){
        if(s.length<2) return 0.0;
        double yp;
        int klow = 1;
        int khigh = s.length;
        
        while(khigh-klow > 1){
            int k = (khigh+klow)/2;
            if(s[k-1] > p){
                khigh = k;
            }else{
                klow = k;
            }
        }
        khigh--;
        klow--;
        double dx = s[khigh]-s[klow];
        if(dx == 0){
            yp= 0.5 * (x[klow] + x[khigh]);
        }else{
            double ai = (s[khigh] - p) / dx;
            double bi = (p - s[klow]) / dx;
            yp = ai * x[klow] + bi * x[khigh] + ((Math.pow(ai,3) - ai) * c[klow] + (Math.pow(bi,3) - bi) * c[khigh]) * dx*dx / 6;
        }
        
        return yp;
    }
    
    /**
     * Calculates the actual point positions on the spline. n identifies the
     * number of points to make. The higher n is, the more precise the
     * approximation will be.
     * @param x The positional data.
     * @param cx The spline data.
     * @param n Number of points to approximate
     * @return Array of real spline points.
     */
    private double[] calculateRealPoints(double[] x, double[] cx){
        double[] xi = new double[pointAmount];
        
        int pos = 0;
        for(double i=s[0]; i<=s[s.length-1]+pointDistance/2; i+=pointDistance){
            xi[pos] = interpolate(x, cx, i);
            pos++;
        }
        return xi;
    }
    
    /**
     * Calculates the interpolated point data for all data sets in the spline.
     * This is more efficient than calling calculateRealPoints() for each of
     * them individually.
     */
    private void calculateAllRealPoints(){
        for(int i=0; i<intData.size(); i++){intData.set(i, new double[pointData.get(0).length]);}
        
        int pos = 0;
        for(double i=s[0]; i<=s[s.length-1]+pointDistance/2; i+=pointDistance){
            for(int j=0; j<intData.size(); j++){
                intData.get(j)[pos] = interpolate(pointData.get(j), splineData.get(j), i);
            }
            pos++;
        }
    }
}
