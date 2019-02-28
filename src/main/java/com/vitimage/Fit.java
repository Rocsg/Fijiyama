package com.vitimage;


public interface Fit {   
	public static final int RICE=100;
	public static final int BIAS=1000;
	public static final int SIGMA=10000;
	public static final int MULTI=100000;
    public static final int ALL_AVAILABLE_FIT=1000000;

	public static final int STRAIGHT_LINE=0,EXPONENTIAL=STRAIGHT_LINE+4,EXP_RECOVERY=STRAIGHT_LINE+13;
	public static final int T2_RELAX = EXP_RECOVERY +3; //offset 3
    public static final int T2_RELAX_BIAS = T2_RELAX+BIAS; //offset 3
    public static final int T2_RELAX_SIGMA = T2_RELAX+SIGMA; //offset 3
    public static final int T2_RELAX_RICE = T2_RELAX+RICE; //offset 3
	public static final int MULTICOMP=T2_RELAX+MULTI;
    public static final int MULTICOMP_BIAS=T2_RELAX+MULTI+BIAS;
    public static final int MULTICOMP_SIGMA=T2_RELAX+MULTI+SIGMA;
    public static final int MULTICOMP_RICE=T2_RELAX+MULTI+RICE;
 	public static final int T1_RECOVERY = 500; //offset 3
	public static final int T1_RECOVERY_RICE = 501; //offset 3
	public static final int T1_RECOVERY_RICE_NORMALIZED = 504; //offset 3
	public static final int GAUSSIAN=17;
	public static final int ERROR_VALUE= 0;
	public static final int SIMPLEX = 1;
    public static final int LM=2;
    public static final int TWOPOINTS=3; 	   
    public static final int MSEC=3;
    public static final int SEC=0;

    public final static String[] timeunits={"ms", "s"};
    public final static int[] timeitems={MSEC, SEC};
    public final static String[] fititems2={"Simplex","Levenberg-Marquardt"};
    public final static int[] constitems2={SIMPLEX,LM};

	
	static float stdValMaxIRM=50000;
	static float stdSigmaIRM=159;
  
    
    
    
    
   
	
}


 