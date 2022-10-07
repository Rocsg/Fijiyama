/*
 * 
 */
package io.github.rocsg.fijiyama.common;
import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * The Class DouglasPeuckerSimplify.
 */
//This code is largely inspired from the version of com.goebl.simplify; 
public class DouglasPeuckerSimplify {
    
    /**
     * The Class Range.
     */
    private static class Range {
        
        /** The first. */
        int first;
        
        /** The last. */
        int last;
        
        /**
         * Instantiates a new range.
         *
         * @param first the first
         * @param last the last
         */
        private Range(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

	/**
	 * Simplify simpler.
	 *
	 * @param listIn the list in
	 * @param fixedTmp the fixed tmp
	 * @param oneEvery the one every
	 * @return the list
	 */
	public static List<Pix> simplifySimpler(List<Pix> listIn,List<Integer>fixedTmp,int oneEvery){
		List<Integer>fixed=new ArrayList<Integer>();
		if(fixedTmp.size()==0) {fixed.add(0);fixed.add(listIn.size()-1);}
		else {
			if(fixedTmp.get(0)!=0) fixed.add(0);
			for(int in:fixedTmp)fixed.add(in);
			if(fixed.get(fixed.size()-1)!=(listIn.size()-1)) fixed.add(listIn.size()-1);
		}
		boolean []kept=new boolean[listIn.size()];
		for(int i=0;i<kept.length;i++) {
			if(i%oneEvery==0)kept[i]=true;			
		}
		for(int in:fixed)kept[in]=true;
		List<Pix>listOut=new ArrayList<Pix>();
	    for(int i=0;i<listIn.size();i++) {
	    	if(kept[i])listOut.add(listIn.get(i));
	    }
	    return listOut;
	}

    
    
	/**
	 * Simplify.
	 *
	 * @param listIn the list in
	 * @param fixedTmp the fixed tmp
	 * @param tolerance the tolerance
	 * @return the list
	 */
	public static List<Pix> simplify(List<Pix> listIn,List<Integer>fixedTmp,double tolerance){
		List<Integer>fixed=new ArrayList<Integer>();
		if(fixedTmp.size()==0) {fixed.add(0);fixed.add(listIn.size()-1);}
		else {
			if(fixedTmp.get(0)!=0) fixed.add(0);
			for(int in:fixedTmp)fixed.add(in);
			if(fixed.get(fixed.size()-1)!=(listIn.size()-1)) fixed.add(listIn.size()-1);
		}
		List<Pix>listOut=new ArrayList<Pix>();
	    List<Range> stack = new ArrayList<Range>();    
	    boolean []kept=new boolean[listIn.size()];
	    for(int ind=0;ind<fixed.size()-1;ind++) {
		    stack.add(new Range(fixed.get(ind),fixed.get(ind+1)));
		    kept[fixed.get(ind)]=true;
		    kept[fixed.get(ind+1)]=true;
		}
	    while (!stack.isEmpty()) {
	        Range range = stack.remove(stack.size() - 1);
	        int index = -1;
	        double maxDist = 0f;

	        // find index of point with maximum square distance from first and last point
	        for (int i = range.first + 1; i < range.last; ++i) {
	        	double dist = getSegmentDistance(listIn.get(i),listIn.get(range.first),listIn.get(range.last));
	            if (dist > maxDist) {
	                index = i;
	                maxDist = dist;
	            }
	        }
	        if (maxDist > tolerance) {
	        	kept[index]=true;
	            stack.add(new Range(range.first, index));
	            stack.add(new Range(index, range.last));
		    }
	    }
	    for(int i=0;i<listIn.size();i++) {
	    	if(kept[i])listOut.add(listIn.get(i));
	    }
		return listOut;
	}

	
    /**
     * Gets the segment distance.
     *
     * @param p0 the p 0
     * @param p1 the p 1
     * @param p2 the p 2
     * @return the segment distance
     */
    public static double getSegmentDistance(Pix p0, Pix p1, Pix p2) {
        double x0, y0, x1, y1, x2, y2, dx, dy, t;
        x1 = p1.x; y1 = p1.y;
        x2 = p2.x;y2 = p2.y;
        x0 = p0.x;y0 = p0.y;
        dx = x2 - x1;
        dy = y2 - y1;

        if (dx != 0.0d || dy != 0.0d) {
            t = ((x0 - x1) * dx + (y0 - y1) * dy)  /  (dx * dx + dy * dy);

            if (t > 1.0d) {
                x1 = x2;
                y1 = y2;
            } else if (t > 0.0d) {
                x1 += dx * t;
                y1 += dy * t;
            }
        }

        dx = x0 - x1;
        dy = y0 - y1;

        return Math.sqrt(dx * dx + dy * dy);
    }
}
