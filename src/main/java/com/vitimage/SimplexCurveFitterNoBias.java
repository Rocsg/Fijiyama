package com.vitimage;

import ij.macro.Interpreter;
import ij.macro.Program;
import ij.macro.Tokenizer;

public class SimplexCurveFitterNoBias implements Fit{

	
	
	
	private static final int CUSTOM = 21;
	
	public static  int IterFactor = 500;
	protected double sigma;   
	private static final double alpha = -1.0;	  // reflection coefficient
	private static final double beta = 0.5;	  // contraction coefficient
	private static final double gamma = 2.0;	  // expansion coefficient
	private static final double root2 = 1.414214; // square root of 2
	
	protected int fit;                // Number of curve type to fit
	protected double[] xData, yData;  // x,y data to fit
	protected int numPoints;          // number of data points
	protected int numParams;          // number of parametres
	protected int numVertices;        // numParams+1 (includes sumLocalResiduaalsSqrd)
	private int worst;			// worst current parametre estimates
	private int nextWorst;		// 2nd worst current parametre estimates
	private int best;			// best current parametre estimates
	protected double[][] simp; 		// the simplex (the last element of the array at each vertice is the sum of the square of the residuals)
	protected double[] next;		// new vertex to be tested
	private int numIter;		// number of iterations so far
	private int maxIter; 	// maximum number of iterations per restart
	private int restarts; 	// number of times to restart simplex after first soln.
	private static int defaultRestarts = 2;  // default number of restarts
	private int nRestarts;  // the number of restarts that occurred
	private static double maxError = 1e-10;    // maximum error tolerance
	private double[] initialParams;  // user specified initial parameters
	private long time;  //elapsed time in ms
	private String customFormula;
	private static int customParamCount;
	private double[] initialValues;
	private Interpreter macro;
private int providedM0;

	public static double sigmaWay(double valFunk,double sigma){
		return valFunk*valFunk-2*sigma*sigma;
	}
	public static double besFunkCost(double value,double sigma) {
		double alpha=value*value/(4*sigma*sigma);
		return Math.sqrt(Math.PI*sigma*sigma/2.0)*( (1+2*alpha)*bessi0NoExp(alpha) + 2*alpha*bessi1NoExp(alpha) );
	}	
	public static double besFunkDeriv(double value,double sigma) {
		double alpha=value*value/(4*sigma*sigma);
		return Math.sqrt(Math.PI*alpha/2.0)*(bessi0NoExp(alpha) + bessi1NoExp(alpha) );
	}
	public static double bessi0NoExp( double x ){
	   double ax,ans;
	   double y;
	   ax=Math.abs(x);
	   if (ax < 3.75) {
	      y=x/3.75;
	      y=y*y;
	      ans=1/Math.exp(ax)*(1.0+y*(3.5156229+y*(3.0899424+y*(1.2067492+y*(0.2659732+y*(0.0360768+y*0.0045813))))));
	   } else {
	      y=3.75/ax;
	      ans=(1/Math.sqrt(ax))*(0.39894228+y*(0.01328592+y*(0.00225319+y*(-0.00157565+y*(0.00916281+y*(-0.02057706+y*(0.02635537+y*(-0.01647633+y*0.00392377))))))));
	   }
	   return ans;
	}
	//Fonction de bessel modifiee de premiere espece d'ordre 1
	public static double bessi1NoExp( double x){
		double ax,ans;
		double y;
		ax=Math.abs(x);
		if (ax < 3.75) {
	      y=x/3.75;
	      y=y*y;
	      ans=ax/Math.exp(ax)*(0.5+y*(0.87890594+y*(0.51498869+y*(0.15084934+y*(0.02658733+y*(0.00301532+y*0.00032411))))));
	   } else {
	      y=3.75/ax;
	      ans=0.02282967+y*(-0.02895312+y*(0.01787654-y*0.00420059));
	      ans=0.39894228+y*(-0.03988024+y*(-0.00362018+y*(0.00163801+y*(-0.01031555+y*ans))));
	      ans *= (1/Math.sqrt(ax));
	   }
	   return x < 0.0 ? -ans : ans;
	}

    /** Construct a new SimplexCurveFitter. */
    public SimplexCurveFitterNoBias (double[] xData, double[] yData, int fitType,double sigma) {
		this.sigma=sigma;
        this.xData = xData;
        this.yData = yData;
        numPoints = xData.length;
        this.fit=fitType;
        
        if (fit<STRAIGHT_LINE )
            throw new IllegalArgumentException("Invalid fit type");
      
        initialize(fit);
    }
    
    public void config(int maxIterations) {
    	IterFactor=maxIterations;
    }
    
    public void doFit() {
    	doFit(fit, false);
    }
    
    public void doFit(int fitType) {
      doFit(fitType, false);
    }
    
    public void doFit(int fitType, boolean showSettings) {

        int saveFitType = fitType;
        fit = fitType;
       
 		if (initialParams!=null) {
			for (int i=0; i<numParams; i++)
				simp[0][i] = initialParams[i];
			initialParams = null;
		}
 		long startTime = System.currentTimeMillis();
        restart(0);
        
        numIter = 0;
        boolean done = false;
        double[] center = new double[numParams];  // mean of simplex vertices
        while (!done) {
            numIter++;
            for (int i = 0; i < numParams; i++) center[i] = 0.0;
            // get mean "center" of vertices, excluding worst
            for (int i = 0; i < numVertices; i++)
                if (i != worst)
                    for (int j = 0; j < numParams; j++)
                        center[j] += simp[i][j];
            // Reflect worst vertex through centre
            for (int i = 0; i < numParams; i++) {
                center[i] /= numParams;
                next[i] = center[i] + alpha*(simp[worst][i] - center[i]);
            }
            sumResiduals(next);
            // if it's better than the best...
            if (next[numParams] <= simp[best][numParams]) {
                newVertex();
                // try expanding it
                for (int i = 0; i < numParams; i++)
                    next[i] = center[i] + gamma * (simp[worst][i] - center[i]);
                sumResiduals(next);
                // if this is even better, keep it
                if (next[numParams] <= simp[worst][numParams])
                    newVertex();
            }
            // else if better than the 2nd worst keep it...
            else if (next[numParams] <= simp[nextWorst][numParams]) {
                newVertex();
            }
            // else try to make positive contraction of the worst
            else {
                for (int i = 0; i < numParams; i++)
                    next[i] = center[i] + beta*(simp[worst][i] - center[i]);
                sumResiduals(next);
                // if this is better than the second worst, keep it.
                if (next[numParams] <= simp[nextWorst][numParams]) {
                    newVertex();
                }
                // if all else fails, contract simplex in on best
                else {
                    for (int i = 0; i < numVertices; i++) {
                        if (i != best) {
                            for (int j = 0; j < numVertices; j++)
                                simp[i][j] = beta*(simp[i][j]+simp[best][j]);
                            sumResiduals(simp[i]);
                        }
                    }
                }
            }
            order();
            
            double rtol = 2 * Math.abs(simp[best][numParams] - simp[worst][numParams]) /
            (Math.abs(simp[best][numParams]) + Math.abs(simp[worst][numParams]) + 0.0000000001);
            
            if (numIter >= maxIter)
            	done = true;
            else if (rtol < maxError) {
                restarts--;
                if (restarts < 0)
                    done = true;
                else
                    restart(best);
             }
        }
        fitType = saveFitType;
    }
        
	public int doCustomFit(String equation, double[] initialValues, boolean showSettings) {
		customFormula = null;
		customParamCount = 0;
		Program pgm = (new Tokenizer()).tokenize(equation);
		if (!pgm.hasWord("y")) return 0;
		if (!pgm.hasWord("x")) return 0;
		String[] params = {"a","b","c","d","e"};
		for (int i=0; i<params.length; i++) {
		if (pgm.hasWord(params[i]))
			customParamCount++;
		}
		if (customParamCount==0)
			return 0;
		customFormula = equation;
		String code =
			"var x, a, b, c, d, e;\n"+
			"function dummy() {}\n"+
			equation+";\n"; // starts at program counter location 19
	   macro = new Interpreter();
		macro.run(code, null);
		if (macro.wasError())
			return 0;
		this.initialValues = initialValues;
		doFit(CUSTOM, showSettings);
		return customParamCount;
	}


    /** Initialise the simplex */
    protected void initialize(int fitType) {
        // Calculate some things that might be useful for predicting parametres
        numParams = getNumParams(fitType);
        numVertices = numParams + 1;      // need 1 more vertice than parametres,
        simp = new double[numVertices][numVertices];
        next = new double[numVertices];
        
        double firstx = xData[0];
        double firsty = yData[0];
       
        double lastx = xData[numPoints-1];
        double lasty = yData[numPoints-1];
        double xmean = (firstx+lastx)/2.0;
        double ymean = (firsty+lasty)/2.0;
        double miny=firsty, maxy=firsty;
        if (fit==GAUSSIAN) {
            for (int i=1; i<numPoints; i++) {
              if (yData[i]>maxy) maxy = yData[i];
              if (yData[i]<miny) miny = yData[i];
            }
        }
        double slope;
        if ((lastx - firstx) != 0.0)
            slope = (lasty - firsty)/(lastx - firstx);
        else
            slope = 1.0;
        double yintercept = firsty - slope * firstx;
        maxIter = IterFactor * numParams * numParams;  // Where does this estimate come from?
        restarts = defaultRestarts;
        nRestarts = 0;
        switch (fit) {
            case STRAIGHT_LINE:
                simp[0][0] = yintercept;
                simp[0][1] = slope;
                break;
            case EXPONENTIAL:
                simp[0][0] = 0.1;
                simp[0][1] = 0.01;
                break;
           case EXP_RECOVERY:
                simp[0][0] = 0.1;
                simp[0][1] = 0.01;
                simp[0][2] = 0.1;
                break;            
            case GAUSSIAN:
                simp[0][0] = miny;   // a0
                simp[0][1] = maxy;   // a1
                simp[0][2] = xmean;  // x0
                simp[0][3] = 3.0;    // sigma
                break;            
           case CUSTOM:
                if (macro==null)
                	throw new IllegalArgumentException("No custom formula!");
                if (initialValues!=null && initialValues.length>=numParams) {
                	for (int i=0; i<numParams; i++)
                		simp[0][i] = initialValues[i];
                } else {
                	for (int i=0; i<numParams; i++)
                		simp[0][i] = 1.0;
                }
                break;
           case T1_RECOVERY:
               simp[0][0] = lasty;
               simp[0][1] = 1000.0;
           case T1_RECOVERY_RICE:
               simp[0][0] = lasty;
               simp[0][1] = 1000.0;
           case T1_RECOVERY_RICE_NORMALIZED:
               simp[0][1] = 1000.0;
           case T2_RELAX_BIAS:
               simp[0][0] = firsty;
               simp[0][1] = 50.0;
               simp[0][2] = 0.0;
               break;
           case T2_RELAX_RICE:
               simp[0][0] = firsty;
               simp[0][1] = 50.0;
               break;
           case T2_RELAX_SIGMA:
               simp[0][0] = firsty;
               simp[0][1] = 50.0;
               break;
           case T2_RELAX:
               simp[0][0] = firsty;
               simp[0][1] = 50.0;
               break;
          case MULTICOMP_BIAS:
        	   simp[0][0] = firsty/2;
        	   simp[0][1] = 20;
        	   simp[0][2] = firsty/2;
        	   simp[0][3] = 60;
        	   simp[0][4] = 0;
               break;
           case MULTICOMP:
        	   simp[0][0] = firsty/2;
        	   simp[0][1] = 20;
        	   simp[0][2] = firsty/2;
        	   simp[0][3] = 60;
               break;
           case MULTICOMP_RICE:
        	   simp[0][0] = firsty/2;
        	   simp[0][1] = 20;
        	   simp[0][2] = firsty/2;
        	   simp[0][3] = 60;
               break;
        }
    }
        
    /** Restart the simplex at the nth vertex */
    void restart(int n) {
        // Copy nth vertice of simplex to first vertice
        for (int i = 0; i < numParams; i++) {
            simp[0][i] = simp[n][i];
        }
        sumResiduals(simp[0]);          // Get sum of residuals^2 for first vertex
        double[] step = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            step[i] = simp[0][i] / 2.0;     // Step half the parametre value
            if (step[i] == 0.0)             // We can't have them all the same or we're going nowhere
                step[i] = 0.01;
        }
        // Some kind of factor for generating new vertices
        double[] p = new double[numParams];
        double[] q = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            p[i] = step[i] * (Math.sqrt(numVertices) + numParams - 1.0)/(numParams * root2);
            q[i] = step[i] * (Math.sqrt(numVertices) - 1.0)/(numParams * root2);
        }
        // Create the other simplex vertices by modifing previous one.
        for (int i = 1; i < numVertices; i++) {
            for (int j = 0; j < numParams; j++) {
                simp[i][j] = simp[i-1][j] + q[j];
            }
            simp[i][i-1] = simp[i][i-1] + p[i-1];
            sumResiduals(simp[i]);
        }
        // Initialise current lowest/highest parametre estimates to simplex 1
        best = 0;
        worst = 0;
        nextWorst = 0;
        order();
        nRestarts++;
    }
        
    // Display simplex [Iteration: s0(p1, p2....), s1(),....] in Log window
    void showSimplex(int iter) {
        ij.IJ.log("" + iter);
        for (int i = 0; i < numVertices; i++) {
            String s = "";
            for (int j=0; j < numVertices; j++)
                s += "  "+ ij.IJ.d2s(simp[i][j], 6);
            ij.IJ.log(s);
        }
    }
        
    /** Get number of parameters for current fit formula */
    public int getNumParams(int fitType) {
        switch (fitType) {
            case STRAIGHT_LINE: return 2;
            case EXPONENTIAL: return 2;
            case GAUSSIAN: return 4;
            case EXP_RECOVERY: return 3;
            case CUSTOM: return customParamCount;

			case T1_RECOVERY: return 2;
			case T1_RECOVERY_RICE: return 2;
			case T1_RECOVERY_RICE_NORMALIZED: return 1;

			case T2_RELAX:  return 2;
			case T2_RELAX_BIAS:  return 3;
			case T2_RELAX_RICE:  return 2;
			case T2_RELAX_SIGMA:  return 2;

			case MULTICOMP_BIAS:  return 5;
			case MULTICOMP:  return 4;
			case MULTICOMP_RICE:  return 4;
			case MULTICOMP_SIGMA:  return 4;
        }
        return 0;
    }
        
	/** Returns formula value for parameters 'p' at 'x' */
	public double f(double[] p, double x) {
		if (fit==CUSTOM) {
			macro.setVariable("x", x);
			macro.setVariable("a", p[0]);
			if (customParamCount>1) macro.setVariable("b", p[1]);
			if (customParamCount>2) macro.setVariable("c", p[2]);
			if (customParamCount>3) macro.setVariable("d", p[3]);
			if (customParamCount>4) macro.setVariable("e", p[4]);
			macro.run(19);
			return macro.getVariable("y");
		} else
			return f(fit, p, x);
	}

   /** Returns 'fit' formula value for parameters "p" at "x" */
    public double f(int fit, double[] p, double x) {
    	double y;
        switch (fit) {
            case STRAIGHT_LINE:
                return p[0] + p[1]*x;
            case EXPONENTIAL:
                return p[0]*Math.exp(p[1]*x);
            case EXP_RECOVERY:
                return p[0]*(1-Math.exp(-p[1]*x))+p[2];
            case GAUSSIAN:
                return p[0]+(p[1]-p[0])*Math.exp(-(x-p[2])*(x-p[2])/(2.0*p[3]*p[3]));
			case T2_RELAX_BIAS:
                return p[0]*Math.exp(-(x / p[1]) )+ p[2]; // p[1] - echo times
            case T2_RELAX:
                return p[0]*Math.exp(-(x / p[1]) ); // p[1] - echo times
            case T2_RELAX_SIGMA:
                return sigmaWay(p[0]*Math.exp(-(x / p[1]) ),sigma); // p[1] - echo times
			case T2_RELAX_RICE:
                return besFunkCost(p[0]*Math.exp(-(x / p[1]) ),sigma); // p[1] - echo times
            case T1_RECOVERY:
            	return p[0]*(1 - Math.exp(-(x / p[1])));
            case T1_RECOVERY_RICE:
            	return besFunkCost(p[0]*(1 - Math.exp(-(x / p[1]))),sigma);
            case T1_RECOVERY_RICE_NORMALIZED:
            	return besFunkCost((1 - Math.exp(-(x / p[0]))),sigma);
            case MULTICOMP_BIAS:
            	return  p[0]* Math.exp(-(x / p[1])) + p[2]* Math.exp(-(x / p[3]))+p[4]; // a[1] - echo times
            case MULTICOMP:
            	return  p[0]* Math.exp(-(x / p[1])) + p[2]* Math.exp(-(x / p[3])); // a[1] - echo times
            case MULTICOMP_RICE:
            	return  besFunkCost(p[0]* Math.exp(-(x / p[1])) + p[2]* Math.exp(-(x / p[3])),sigma); // a[1] - echo times
            default:
                return 0.0;
        }
    }
    
    /** Get the set of parameter values from the best corner of the simplex */
    public double[] getParams() {
        order();
        return simp[best];
    }
    
	/** Returns residuals array ie. differences between data and curve. */
	public double[] getResiduals() {
		int saveFit = fit;
		double[] params = getParams();
		double[] residuals = new double[numPoints];
		if (fit==CUSTOM) {
			for (int i=0; i<numPoints; i++)
				residuals[i] = yData[i] - f(params, xData[i]);
		} else {
			for (int i=0; i<numPoints; i++)
				residuals[i] = yData[i] - f(fit, params, xData[i]);
		}
		fit = saveFit;
		return residuals;
	}
    
    /* Last "parametre" at each vertex of simplex is sum of residuals
     * for the curve described by that vertex
     */
    public double getSumResidualsSqr() {
        double sumResidualsSqr = (getParams())[getNumParams(fit)];
        return sumResidualsSqr;
    }
    
    /**  Returns the standard deviation of the residuals. */
    public double getSD() {
    	double[] residuals = getResiduals();
		int n = residuals.length;
		double sum=0.0, sum2=0.0;
		for (int i=0; i<n; i++) {
			sum += residuals[i];
			sum2 += residuals[i]*residuals[i];
		}
		double stdDev = (n*sum2-sum*sum)/n;
		return Math.sqrt(stdDev/(n-1.0));
    }
    
    /** Returns R^2, where 1.0 is best.
    <pre>
     r^2 = 1 - SSE/SSD
     
     where:	 SSE = sum of the squares of the errors
                 SSD = sum of the squares of the deviations about the mean.
    </pre>
    */
    public double getRSquared() {
        double sumY = 0.0;
        for (int i=0; i<numPoints; i++) sumY += yData[i];
        double mean = sumY/numPoints;
        double sumMeanDiffSqr = 0.0;
        for (int i=0; i<numPoints; i++)
            sumMeanDiffSqr += sqr(yData[i]-mean);
        double rSquared = 0.0;
        if (sumMeanDiffSqr>0.0)
            rSquared = 1.0 - getSumResidualsSqr()/sumMeanDiffSqr;
        return rSquared;
    }

    /**  Get a measure of "goodness of fit" where 1.0 is best. */
    public double getFitGoodness() {
        double sumY = 0.0;
        for (int i = 0; i < numPoints; i++) sumY += yData[i];
        double mean = sumY / numPoints;
        double sumMeanDiffSqr = 0.0;
        int degreesOfFreedom = numPoints - getNumParams(fit);
        double fitGoodness = 0.0;
        for (int i = 0; i < numPoints; i++) {
            sumMeanDiffSqr += sqr(yData[i] - mean);
        }
        if (sumMeanDiffSqr > 0.0 && degreesOfFreedom != 0)
            fitGoodness = 1.0 - (getSumResidualsSqr() / degreesOfFreedom) * ((numPoints) / sumMeanDiffSqr);
        
        return fitGoodness;
    }
    
    /** Get a string description of the curve fitting results
     * for easy output.
     */
        
    double sqr(double d) { return d * d; }
    
	/** Adds sum of square of residuals to end of array of parameters */
	void sumResiduals (double[] x) {
		x[numParams] = 0.0;
		if (fit==CUSTOM) {
			for (int i=0; i<numPoints; i++)
			x[numParams] = x[numParams] + sqr(f(x,xData[i])-yData[i]);
		} else {
			for (int i=0; i<numPoints; i++)
			x[numParams] = x[numParams] + sqr(f(fit,x,xData[i])-yData[i]);
		}
	}

    /** Keep the "next" vertex */
    void newVertex() {
        for (int i = 0; i < numVertices; i++)
            simp[worst][i] = next[i];
    }
    
    /** Find the worst, nextWorst and best current set of parameter estimates */
    void order() {
        for (int i = 0; i < numVertices; i++) {
            if (simp[i][numParams] < simp[best][numParams])	best = i;
            if (simp[i][numParams] > simp[worst][numParams]) worst = i;
        }
        nextWorst = best;
        for (int i = 0; i < numVertices; i++) {
            if (i != worst) {
                if (simp[i][numParams] > simp[nextWorst][numParams]) nextWorst = i;
            }
        }
        //        IJ.log("B: " + simp[best][numParams] + " 2ndW: " + simp[nextWorst][numParams] + " W: " + simp[worst][numParams]);
    }

    /** Get number of iterations performed */
    public int getIterations() {
        return numIter;
    }
    
    /** Get maximum number of iterations allowed */
    public int getMaxIterations() {
        return maxIter;
    }
    
    /** Set maximum number of iterations allowed */
    public void setMaxIterations(int x) {
        maxIter = x;
    }
    
    /** Get number of simplex restarts to do */
    public int getRestarts() {
        return defaultRestarts;
    }
    
    /** Set number of simplex restarts to do */
    public void setRestarts(int n) {
        defaultRestarts = n;
    }

	/** Sets the initial parameters, which override the default initial parameters. */
	public void setInitialParameters(double[] params) {
		initialParams = params;
	}

    /**
     * Gets index of highest value in an array.
     * 
     * @param              Double array.
     * @return             Index of highest value.
     */
    public static int getMax(double[] array) {
        double max = array[0];
        int index = 0;
        for(int i = 1; i < array.length; i++) {
            if(max < array[i]) {
            	max = array[i];
            	index = i;
            }
        }
        return index;
    }
    
	public double[] getXPoints() {
		return xData;
	}
	
	public double[] getYPoints() {
		return yData;
	}
	
	public int getFit() {
		return fit;
	}

	
}
