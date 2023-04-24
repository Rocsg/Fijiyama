package io.github.rocsg.fijiyama.rsml;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.rsml.Root;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

  
/** 
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge 
 * 
 * Root class.
 * */



public class Root implements Comparable<Root>{
   
	
	   public void repairUnevenSituations() {
		   //Display all
		   Node n=firstNode;
		   while(n.child!=null) {
			   System.out.println(n);
			   n=n.child;
		   }
		   //Detect if 
	   }
	
	   public void cleanNegativeTh() {
		   Node n=firstNode;
		   while(n.child!=null) {
			   if(n.birthTimeHours<0)n.birthTimeHours=0;
			   if(n.birthTime<1)n.birthTime=0;
			   n=n.child;
		   }
		   if(n!=null) {
			   if(n.birthTimeHours<0)n.birthTimeHours=0;
			   if(n.birthTime<1)n.birthTime=0;
		   }
	   }
	
	   public void interpolateTime() {
		   Node n=firstNode;
		   //double dist0=firstNode.distance;
		   //double time0=firstNode.birthTime;
		   //double timehour0=firstNode.birthTimeHours;
		   ArrayList<Double>times=new ArrayList<Double>();
		   ArrayList<Double>timesHours=new ArrayList<Double>();
		   ArrayList<Double>dists=new ArrayList<Double>();
		   dists.add(n.distance);
		   times.add(Double.valueOf(n.birthTime));
		   timesHours.add(Double.valueOf(n.birthTimeHours));
		   while(n.child!=null) {
			   n=n.child;
			   if(n.birthTime>=0) {
				   dists.add(n.distance);
				   times.add(Double.valueOf( n.birthTime ));
				   timesHours.add(Double.valueOf( n.birthTimeHours ));
			   }
		   }
		   Double[]distsTab=dists.toArray(new Double[dists.size()]);
		   Double[]timesTab=times.toArray(new Double[times.size()]);
		   Double[]timesHoursTab=timesHours.toArray(new Double[timesHours.size()]);
		   
		   n=firstNode;
		   while(n.child!=null) {
			   n=n.child;
			   if(n.birthTime<0) {
				   n.birthTime=(float) linearInterpolation(n.distance, distsTab, timesTab);
				   n.birthTimeHours=(float) linearInterpolation(n.distance, distsTab, timesHoursTab);
			   }
		   }
	   }

   
	   public void updateTiming() {
		   boolean debug=false;
		   Node nStart=this.firstNode;
		   Node nStop=this.lastNode;
		   Node curNode=nStart;
		   ArrayList<Node>listNode=new ArrayList<Node>();
		   while(curNode!=null) {
			   listNode.add(curNode);
			   curNode=curNode.child;
		   }
		   int N=listNode.size();
		   this.nNodes=N;
		   Node[]tabNode=listNode.toArray(new Node[N]);
		   Node[]tabNodePrev=listNode.toArray(new Node[N]);
		   Node[]tabNodeNext=listNode.toArray(new Node[N]);
		   boolean[]tabExact=new boolean[N];
		   double[]distToPrev=new double[N]; 
		   double[]distToNext=new double[N];

		   //1 Identify nodes falling exactly on an observation timepoint
		   for(int i=0;i<N;i++) {
			   tabExact[i]=(Math.abs(tabNode[i].birthTime-Math.round(tabNode[i].birthTime))<VitimageUtils.EPSILON);
			   if(debug)  System.out.println(" i="+i+" isExact ?"+tabExact[i]+" "+tabNode[i]);
		   }

		   
		   Node prev=null;
		   double dist=0;
		   for(int i=0;i<N;i++) {
			   if(!tabExact[i]) {
				   if(i>0)dist+=Node.distanceBetween(tabNode[i],tabNode[i-1]);
				   distToPrev[i]=dist;
				   tabNodePrev[i]=prev;			   
			   }
			   else {
				   dist=0;
				   prev=tabNode[i];
			   }
		   }

		   if(debug)  System.out.println("\n\nTHIRD STEP, ESTABLISH BACKWARD");
		   dist=0;
		   Node next=null;
		   for(int i=N-1;i>=0;i--) {
			   if(debug)  System.out.println("  Processing node "+i+" : "+tabNode[i]);
			   if(!tabExact[i]) {
				   if(debug)  System.out.println("   Non exact");
				   if(i<N-1)dist+=Node.distanceBetween(tabNode[i],tabNode[i+1]);
				   distToNext[i]=dist;
				   tabNodeNext[i]=next;			   
				   if(debug)  System.out.println("     Setting distance "+dist+" to next : "+next);
			   }
			   else {
				   if(debug)  System.out.println("   Exact");
				   dist=0;
				   next=tabNode[i];
			   }
		   }

		   if(debug)  System.out.println("\n\nFOURTH STEP, RE ESTIMATE TIME");
		   for(int i=0;i<N;i++) {
			   if(!tabExact[i]) {
				   double estTime=0;
				   double estTimeHours=0;
				   double dh=tabNodeNext[i].birthTimeHours-tabNodePrev[i].birthTimeHours;
				   double dt=tabNodeNext[i].birthTime-tabNodePrev[i].birthTime;
				   double dl=distToNext[i]+distToPrev[i];
				   if(dl<VitimageUtils.EPSILON) {
					   estTime=tabNodePrev[i].birthTime+dt/2.0;
					   estTimeHours=tabNodePrev[i].birthTimeHours+dh/2.0;
				   }
				   else {
					   estTime=tabNodePrev[i].birthTime+dt*(distToPrev[i]/dl);
					   estTimeHours=tabNodePrev[i].birthTimeHours+dh*(distToPrev[i]/dl);
				   }
				   tabNode[i].birthTime=(float) estTime;
				   tabNode[i].birthTimeHours=(float) estTimeHours;
			   }
		   }   
	   }
	    
		
	   /**
	    * Gets the coords at distance.
	    *
	    * @param dist the dist
	    * @return the coords at distance
	    */
	   public double[]getCoordsAtDistance(double dist){
		   if(dist<=0)return new double[]{firstNode.x,firstNode.y,firstNode.birthTime,firstNode.birthTimeHours};
		   if(dist>=lastNode.distance)return new double[]{lastNode.x,lastNode.y,lastNode.birthTime,lastNode.birthTimeHours};
		   ArrayList<Node>nodes=getNodesList();
		   int N=nodes.size();
		   int indBef=-1;int indAft=-1;
		   for(int n=0;n<N-1;n++) {
			   if(nodes.get(n).distance<dist && nodes.get(n+1).distance>=dist) {
				   indBef=n;indAft=n+1;
				   break;
			   }
		   }
			Node n0=nodes.get(indBef);
			Node n1=nodes.get(indAft);				
			double d0=n0.distance;
			double d1=n1.distance;
			double alpha=(dist-d0)/(d1-d0);
			return new double[] {n1.x*alpha+(1-alpha)*n0.x , n1.y*alpha+(1-alpha)*n0.y , n1.birthTime*alpha+(1-alpha)*n0.birthTime,n1.birthTimeHours*alpha+(1-alpha)*n0.birthTimeHours};
	   }
	      
	   
		/**
		 * Compute speed vectors.
		 *
		 * @param deltaBackward the delta backward
		 * @param deltaForward the delta forward
		 * @param zeroPaddingAtLastNode the zero padding at last node
		 */
		public void computeSpeedVectors(double deltaBackward,double deltaForward,boolean zeroPaddingAtLastNode) {
			//TODO : Caution, speed are computed in pixels units.
			ArrayList<Node>nodes=getNodesList();
			int N=nodes.size();

			//For each node, 
			for(int n=0;n<N;n++) {			
				Node nono=nodes.get(n);
				double[]coordsBef=getCoordsAtDistance(nono.distance-deltaBackward);
				double[]coordsAft=getCoordsAtDistance(nono.distance+deltaForward);
				if(coordsBef[2]<1)coordsBef=new double[] {nono.x,nono.y,nono.birthTime,nono.birthTimeHours};
				double deltaTimeHours=coordsAft[3]-coordsBef[3];
				double deltaX=coordsAft[0]-coordsBef[0];
				double deltaY=coordsAft[1]-coordsBef[1];

				double deltaDist=Math.sqrt(deltaX*deltaX+deltaY*deltaY);
				double speed=deltaDist/deltaTimeHours;
				double vx=deltaX*speed/deltaDist;
				double vy=deltaY*speed/deltaDist;
				nono.vx=(float) vx;nono.vy=(float) vy;
				if(nono.birthTime<1) {nono.vx=0;nono.vy=0;}
			}
			if(zeroPaddingAtLastNode) {	nodes.get(N-1).vx=0;nodes.get(N-1).vy=0;}
		}
		
		
		
		public void changeTimeBasis(double timestep,int N) {
			double[]hoursCorrespondingToTimePoints=new double[N];
		    for(int i=0;i<N;i++) {
			   hoursCorrespondingToTimePoints[i]=timestep*i;
		    }
		    double maxHour=hoursCorrespondingToTimePoints[hoursCorrespondingToTimePoints.length-1];

		    Node nFirst=firstNode;
		    Node nLast=lastNode;
		    Node n=nFirst;
		    while(n!=null) {
			   n.birthTime=(float) (n.birthTimeHours/timestep);
		       n=n.child;
		    }
		}

		public boolean removeInterpolatedNodes() {
		   Node n1=firstNode;
		   Node n2=n1.child;
		   while(n1!=null) {
//			   System.out.println("Node : "+n1);
			   boolean exact=(Math.abs(n1.birthTime-Math.round(n1.birthTime))<VitimageUtils.EPSILON);
			   if(!exact) {
				   //				   System.out.println(" Not exact");
				   //We need to suppress this node
				   if(n2==null) {//last node
					   //  System.out.println("   Last node");
					   if(n1==firstNode) {//also the first node
						   //  System.out.println("     First node");
						   return false;//technically the calling function should raise this flag and should suppress this root
					   }
					   else {//last node but not first node
						   //System.out.println("     Not first node");
					       lastNode=n1.parent;
						   n1.parent.child=null;
						   n1=null;
					   }
				   }
				   else {//is not last node
					   //System.out.println("   Not last node");
					   if(n1==firstNode) {//it is the first node but not the last node
						   //  System.out.println("     First node");
						   firstNode=n2;
						   n1=n2;
						   n2=n1.child;
					   }
					   else {//not first node, nor last node
						   //System.out.println("     Not first node");
						   n1.parent.child=n2;
						   n2.parent=n1.parent;
						   n1=n2;
						   n2=n1.child;
					   }
				   }
			   }
			   else {//no need to suppress this node
				   //System.out.println("  Exact");
				   n1=n2;
				   n2=n1.child;
			   }
		   }			
		   return true;
		}
		
		
	   /**
	    * Resample flying points.
	    * Updating a tree-like data structure by finding any "flying points" (i.e. points that are not present in the tree at certain times) 
	    * These points are then added to the tree at the appropriate time.
	    * @return the number of individual modifications applied during correction
	    */
	   public int resampleFlyingPoints(double[]hoursCorrespondingToTimePoints) {
		   int stamp=1000000;
		   boolean debug=false && (this.firstNode!=null && this.firstNode.x==1133 && this.firstNode.y==190);
		   if(debug)System.out.println("Root before update :");
		   if(debug)System.out.println(this.stringNodes());
		   if(debug)System.out.println("Processing flying points on root "+this);
		   ArrayList<Node>ar=new ArrayList<Node>();
		   Node nFirst=firstNode;
		   Node nLast=lastNode;
		   Node n=nFirst;
		   while(n!=null) {
			   ar.add(n);
			   n=n.child;
		   }
		   int N=ar.size();
		   Node[]tabNode=ar.toArray(new Node[N]);
		   int tStart=(int) Math.ceil(nFirst.birthTime);
		   int tStop=(int) Math.floor(nLast.birthTime);
		   int [] indexT=new int[tStop+1];
		   
		   boolean[]tabExact=new boolean[N];
		   boolean[]isNotFlying=new boolean[tStop+1];
		   for(int i=0;i<N;i++) {
			   tabExact[i]=(Math.abs(tabNode[i].birthTime-Math.round(tabNode[i].birthTime))<VitimageUtils.EPSILON);
			   if(tabExact[i]) {
				   int tt=Math.round(tabNode[i].birthTime);
				   indexT[tt]=i;
				   isNotFlying[tt]=true;
			   }
		   }
		   for(int t=tStart;t<=tStop;t++) {
			   if(isNotFlying[t])continue;
			   stamp+=1;
			   if(debug) System.out.println("\n\nDetected flying time : "+t);
			   Node lastBef=null;
			   for(int i=0;i<N;i++) {
				   if(tabNode[i].birthTime<t)lastBef=tabNode[i];
			   }
			   if(debug) System.out.println("Last bef detected="+lastBef);
			   Node firstAft=null;
			   for(int i=N-1;i>=0;i--) {
				   if(tabNode[i].birthTime>t)firstAft=tabNode[i];
			   }
			   if(debug) System.out.println("first aft detected="+firstAft);
			   double DT=lastBef.birthTime-firstAft.birthTime;
			   double DX=lastBef.x-firstAft.x;
			   double DY=lastBef.y-firstAft.y;
			   double dt=t-lastBef.birthTime;
			   double dx=DX*dt/DT;
			   double dy=DY*dt/DT;
			   Node newNode=new Node(lastBef.x+(float)dx,lastBef.y+(float)dy, lastBef,true);
			   newNode.birthTime=t;
			   newNode.birthTimeHours=(float) hoursCorrespondingToTimePoints[t];
			   lastBef.child=newNode;
			   newNode.parent=lastBef;
			   newNode.child=firstAft;
			   firstAft.parent=newNode;
			   nNodes++;
			   
			   ar=new ArrayList<Node>();
			   nFirst=firstNode;
			   nLast=lastNode;
			   n=nFirst;
			   while(n!=null) {
				   ar.add(n);
				   n=n.child;
			   }
			   N=ar.size();
			   tabNode=ar.toArray(new Node[N]);
			   
			   
			   if(debug)System.out.println("Adding node "+newNode);
			   if(debug)System.out.println("Root after update :");
			   if(debug)System.out.println(this.stringNodes());
			   if(debug)System.out.println("");
		   }
		   
		   if(debug) System.out.println("");
		   return stamp;
	   }
		   
	
	   
	   
	   
	   
	   
	   
	   
	   
	   
	   
	   
	   
	   
	   /**
	 
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
   /** The last node. */
   public Node firstNode, lastNode;
   
   /** The n nodes. */
   int nNodes;
   
   /** The child list. */
   public ArrayList<Root> childList = new ArrayList<Root>();
   
   /** The root key. */
   String rootKey = "";
   
   /** The parent key. */
   String parentKey = null;
   
   /** The po index. */
   int poIndex = 1;
   
   /** The parent node. */
   Node parentNode;
   
   /** The parent. */
   Root parent;
   
   /** The first child. */
   Root firstChild;
   
   /** The last child. */
   Root lastChild;
   
   /** The order. */
   int order=1;
   
   /** The distance from apex. */
   float distanceFromApex = 0;
   
   /** The distance from base. */
   float distanceFromBase = 0;
   
   /** The child density. */
   float childDensity = 0;
   
   /** The insert angl. */
   float insertAngl = 0;
   
   /** The inter branch. */
   float interBranch = 0;
   
   /** The is child. */
   int isChild = 0;
   
   /** The parent name. */
   String parentName;
   
   /** The g M. */
   int gM = 0;
   
   /** The keep root. */
   ArrayList<Root> deletedRoot, keepRoot;
   
   /** The mark list. */
   public Vector<Mark> markList;
   
   /** The mdl. */
   public Mark anchor, MDL;
   
   /** The ruler at origin. */
   float rulerAtOrigin = 0.0f;  // in pixel units
   
   /** The root ID. */
   public String rootID = "";
   
   /** The pixel size. */
   private float dpi, pixelSize;
   
   /** The plant number. */
   public int plantNumber;
   
   /** The borders GP. */
   public GeneralPath bordersGP = new GeneralPath();
   
   /** The axis GP. */
   public GeneralPath axisGP = new GeneralPath();
   
   /** The nodes GP. */
   public GeneralPath nodesGP = new GeneralPath();
   
   /** The ticks GP. */
   public GeneralPath ticksGP = new GeneralPath();
   
   /** The tips GP. */
   public GeneralPath tipsGP = new GeneralPath();
   
   /** The convexhull GP. */
   public GeneralPath convexhullGP = new GeneralPath();

   /**  new. */
   public GeneralPath parallelsGP = new GeneralPath();
   
   
   /** The no name. */
   static String noName = FSR.prefs.get("root_ID", "root_");
   
   /** The axis color. */
   static Color axisColor = Color.green;
   
   /** The nodes color. */
   static Color nodesColor = Color.orange;
   
   /** The borders color. */
   static Color bordersColor = Color.red;
   
   /** The area color. */
   static Color areaColor = Color.yellow;
   
   /** The ticks color. */
   static Color ticksColor = Color.yellow;
   
   /** The tips color. */
   static Color tipsColor = Color.yellow;
   
   /** The child tips color. */
   static Color childTipsColor = Color.green;
   
   /** The child nodes color. */
   static Color childNodesColor = Color.green;
   
   /** The child axis color. */
   static Color childAxisColor = Color.yellow;
   
   /** The child borders color. */
   static Color childBordersColor = Color.orange;
   
   /** The next root key. */
   static int nextRootKey = 1;

   /**
    * Constructor
    * Used when opening a xml file.
    *
    * @param dpi digit per inch
    * @param parentDOM the parent DOM
    * @param common the common
    * @param parentRoot the parent root
    * @param rm the rm
    * @param origin the origin
    */
   public Root(float dpi, org.w3c.dom.Node parentDOM, boolean common, Root parentRoot, RootModel rm, String origin) {
	  this.dpi = dpi;
      pixelSize = ((float) 2.54 / dpi);
      nNodes = 0;
      firstNode = lastNode = null;
      markList = new Vector<Mark>();
      rootID = noName;
      nextRootKey++;     
      readRSML(parentDOM, rm, parentRoot, origin);
    }
 
   /**
    * Instantiates a new root.
    *
    * @param dpi the dpi
    * @param parentDOM the parent DOM
    * @param common the common
    * @param parentRoot the parent root
    * @param rm the rm
    * @param origin the origin
    * @param timeLapseModel the time lapse model
    */
   public Root(float dpi, org.w3c.dom.Node parentDOM, boolean common, Root parentRoot, RootModel rm, String origin,boolean timeLapseModel) {
		  this.dpi = dpi;
	      pixelSize = ((float) 2.54 / dpi);
	      nNodes = 0;
	      firstNode = lastNode = null;
	      markList = new Vector<Mark>();
	      rootID = noName;
	      nextRootKey++;     
	      readRSML(parentDOM, rm, parentRoot, origin,timeLapseModel);
	    }

   
   /**
    * Instantiates a new root.
    *
    * @param parentRoot the parent root
    * @param rm the rm
    * @param rootID the root ID
    * @param order the order
    */
   public Root(Root parentRoot, RootModel rm,String rootID,int order) {
	   this.order=order;
	   nNodes = 0;
      firstNode = lastNode = null;
      markList = new Vector<Mark>();
      this.rootID = rootID;
      if(parentRoot!=null) {attachParent(parentRoot);	   parentRoot.attachChild(this);}
      nextRootKey++;     
  }

   
   /**
    * Compute distances.
    */
   public void computeDistances() {
	   Node n=firstNode;
	   n.distance=0;
	   double dist=0;
	   while(n.child!=null) {
		   dist+=n.getDistanceTo(n.child);
		   n=n.child;
		   n.distance=dist;
	   }
   }
   
   /**
    * Gets the AVG median diameter in range.
    *
    * @param rangeMinL the range min L
    * @param rangeMaxL the range max L
    * @return the AVG median diameter in range
    */
   public double getAVGMedianDiameterInRange(double rangeMinL,double rangeMaxL){
	   double dist = 0;
	   double[][]tab=new double[10000][2];
	   ArrayList<Double>ar=new ArrayList<Double>();
	   Node node = this.firstNode;
	   int incr=0;
	   tab[incr++]=new double[] {0,node.diameter};
	   ar.add(Double.valueOf(node.diameter));
		while (node.child != null){
			dist+=node.getDistanceTo(node.child);
			node = node.child;
			tab[incr++]=new double[] {dist,node.diameter};
		}
		int n=0;
		for(int i=0;i<tab.length;i++)if(tab[i][0]>0 && tab[i][0]>=rangeMinL && tab[i][0]<=rangeMaxL)n++;
		double[]tabFinal=new double[n];
		if(n==0)return 0;
		n=0;
		for(int i=0;i<tab.length;i++)if(tab[i][0]>0 && tab[i][0]>=rangeMinL && tab[i][0]<=rangeMaxL)tabFinal[n++]=tab[i][1];
		
		double median=VitimageUtils.MADeStatsDoubleSided(tabFinal,null)[0];
		return median;

   }


   /**
    * Update nnodes.
    */
   public void updateNnodes() {
	   Node n=firstNode;
	   nNodes=0;
	   while(n!=null) {
		   nNodes++;
		   n=n.child;
	   }
   }
   
   /**
    * Gets the node of parent just after my attachment.
    *
    * @return the node of parent just after my attachment
    */
   public Node getNodeOfParentJustAfterMyAttachment() {
	   //Node n=null;
	   Root par=this.parent;
	   Node nn=par.firstNode;
	   Node nnNext=nn.child;
	   Node bestNext=nn;
	   if(nnNext==null)return nn;
	   while(nnNext!=null) {
		   if(nn.y<this.firstNode.y) {
			   bestNext=nnNext;
		   }
		   nn=nnNext;
		   nnNext=nn.child;
	   }
	   return bestNext;
   }
   
   
	/**
	 * Linear interpolation.
	 *
	 * @param val the val
	 * @param xSource the x source
	 * @param ySource the y source
	 * @return the double
	 */
	public static double linearInterpolation(double val,Double[]xSource,Double[]ySource) {
		int N=xSource.length;
		int indexUpper=0;
		if(val<=xSource[0])return ySource[0];
		if(val>=xSource[N-1])return ySource[N-1];
		while(xSource[indexUpper]<val)indexUpper++;
		int indexLower=indexUpper-1;
		double DX=xSource[indexUpper]-xSource[indexLower];
		double DY=ySource[indexUpper]-ySource[indexLower];
		double dx=(val-xSource[indexLower]);
		double dy=DY*dx/DX;
		return ySource[indexLower]+dy;
	}

	/**
	 * Sets the last node hidden.
	 */
	public void setLastNodeHidden() {
		this.lastNode.hiddenWayToChild=true;
	}
   
   /**
    * Add a node to the root.
    *
    * @param x the x
    * @param y the y
    * @param addToBase the add to base
    * @return the node
    */
   public Node addNode(float x, float y, boolean addToBase) {
      if (!addToBase) {
         lastNode = new Node(x, y, lastNode, true);
         if (nNodes == 0) firstNode = lastNode;
         nNodes++;
         return lastNode;
         }
      else {
         firstNode = new Node(x, y, firstNode, false);
         if (nNodes == 0) lastNode = firstNode;
         nNodes++;
         return firstNode; // user must reactualize cLength
         }
      }
   
   /**
    * Adds the node.
    *
    * @param x the x
    * @param y the y
    * @param birthTime the birth time
    * @param addToBase the add to base
    * @return the node
    */
   public Node addNode(double x, double y, double birthTime, double birthTimeHours,boolean addToBase) {
	      if (!addToBase) {
	         lastNode = new Node((float)x, (float)y, lastNode, true);
	         lastNode.birthTime=(float)birthTime;
	         lastNode.birthTimeHours=(float)birthTimeHours;
	         if (nNodes == 0) firstNode = lastNode;
	         nNodes++;
	         return lastNode;
	         }
	      else {
	         firstNode = new Node((float)x, (float)y, firstNode, false);
	         firstNode.birthTime=(float)birthTime;
	         firstNode.birthTimeHours=(float)birthTimeHours;
	         if (nNodes == 0) lastNode = firstNode;
	         nNodes++;
	         return firstNode; // user must reactualize cLength
	         }
	      }
  
   /**
    * Adds the node.
    *
    * @param x the x
    * @param y the y
    * @param birthTime the birth time
    * @param diameter the diameter
    * @param vx the vx
    * @param vy the vy
    * @param addToBase the add to base
    * @return the node
    */
   public Node addNode(double x, double y, double birthTime, double birthTimeHours,double diameter,double vx,double vy,boolean addToBase) {
	      if (!addToBase) {
	         lastNode = new Node((float)x, (float)y, lastNode, true);
	         lastNode.diameter=(float) diameter;lastNode.vx=(float) vx;lastNode.vy=(float) vy;
	         lastNode.birthTime=(float)birthTime;
	         lastNode.birthTimeHours=(float)birthTimeHours;
	         if (nNodes == 0) firstNode = lastNode;
	         nNodes++;
	         return lastNode;
	         }
	      else {
	         firstNode = new Node((float)x, (float)y, firstNode, false);
	         firstNode.diameter=(float) diameter;firstNode.vx=(float) vx;firstNode.vy=(float) vy;
	         firstNode.birthTime=(float)birthTime;
	         firstNode.birthTimeHours=(float)birthTimeHours;
	         if (nNodes == 0) lastNode = firstNode;
	         nNodes++;
	         return firstNode; // user must reactualize cLength
	         }
	      }
   
   /**
    * Add a mark to the root.
    *
    * @param type the type
    * @param value the value
    * @param markerPosition the marker position
    * @return the mark
    */
   public Mark addMark(int type, String value, float markerPosition) {
      float lPos = lPosCmToPixels(markerPosition);
      if (type == Mark.ANCHOR) {
         float v = Float.parseFloat(value);
         if (v != 0.0f) {   // reset rulerOrigin relative to the mark;
            setRulerAtOrigin(-(lPos - v / pixelSize));
            }
         else value = String.valueOf(Math.round(markerPosition * 100.0) / 100.0);
         anchor = new Mark(type, this, lPos, value);
         return null;
         }
      else if (type == Mark.MDL){
    	  for (int i = 0 ;  i< markList.size(); i++){
    		  Mark ma= (Mark) markList.get(i);
    		  if (ma.type == type && !ma.isForeign) markList.remove(i);
    	  }
          Mark m = new Mark(type, this, lPos, value);
          markList.add(m);
          MDL = m;
          return m;
      }
      else {
         Mark m = new Mark(type, this, lPos, value);
         markList.add(m);
         // m.createGraphics();
         return m;
         }
      }

   /**
    * Add a mark to the root.
    *
    * @param m the m
    */
   public void addMark(Mark m) {
      if (m.type == Mark.ANCHOR) {
         anchor = m;
         }
      else markList.add(m);
      }


   
   /**
    * String nodes.
    *
    * @return the string
    */
   public String stringNodes() {
	   String res="";
	   Node n=this.firstNode;
	   while(n!=null) {
		   res+="\n"+n;
		   n=n.child;
	   }
	   return res;
   }
   
   /**
   
   /**
    * compute the number of nodes inside the root.
    */
   public void calcNNodes() {
      if (firstNode == null) {
         nNodes = 0;
         return;
         }
      nNodes = 1;
      for (Node n = firstNode; (n = n.child) != null; nNodes++);
   }


   /**
    * Delete all the root informations.
    */
   public void clear() {
      Node n = firstNode;
      while (n != null) {
         Node n1 = n.child;
         n.child = null;
         n.parent = null;
         n = n1;      
         }
      firstNode = null;
      lastNode = null;
      nNodes = 0;
      rulerAtOrigin = 0.0f;
      rootID = noName;
      }
  

   /**
    * Get the root name.
    *
    * @return the root ID
    */
   public String getRootID() {return rootID; }
 
   /**
    * Get the root length.
    *
    * @return the root length
    */
   public float getRootLength() {return lastNode.cLength + rulerAtOrigin; }

   /**
    * Compute root length.
    *
    * @param t the t
    * @return the double
    */
   public double computeRootLength(double t) {
	   Node n=this.firstNode;
	   double len=0;
	   while (n.child != null && n.child.birthTime<=t) {
		   len+=Node.distanceBetween(n, n.child);
		   n=n.child;
	   }
	   if(n.birthTime<=t && n.child!=null) {
		   double dt=n.child.birthTime-n.birthTime;
		   double dx=Node.distanceBetween(n, n.child);
		   len+=(t-n.birthTime)/dt*dx;
	   }
   	   return len;
	   
   }

   /**
    * Compute root length.
    *
    * @return the double
    */
   public double computeRootLength() {
	   Node n=this.firstNode;
	   double len=0;
	   while (n.child != null) {
		   len+=Node.distanceBetween(n, n.child);
		   n=n.child;
	   }
   	   return len;
	   
   }
  
   /**
    * Get the value of the ruelr at origin.
    *
    * @return the ruler at origin
    */
   public float getRulerAtOrigin() {return rulerAtOrigin; }
   
   /**
    * Convert the longitudinal position from cm to pixels.
    *
    * @param cm the cm
    * @return the float
    */
   public float lPosCmToPixels(float cm) {
      return cm / pixelSize - this.rulerAtOrigin;
      }

   /**
    * Convert the longitudinal position from pixels to cm.
    *
    * @param pixels the pixels
    * @return the float
    */
   public float lPosPixelsToCm(float pixels) {
      return pixelSize * (pixels + this.rulerAtOrigin);
      }

   /**
    * Gets the nodes list.
    *
    * @return the nodes list
    */
   public ArrayList<Node> getNodesList(){
	   Node n=firstNode;
	   ArrayList<Node>nodes=new ArrayList<Node>();
	   nodes.add(n);
	   while(n.child !=null) {
		   n=n.child;
		   nodes.add(n);
	   }
	   return nodes;
   }
   
   /**
    * Read the RSML file.
    *
    * @param parentDOM the parent DOM
    * @param rm the rm
    * @param parentRoot the parent root
    * @param origin the origin
    */
   public void readRSML(org.w3c.dom.Node parentDOM, RootModel rm, Root parentRoot, String origin) {
	  int counter = 1, clock = 1; // The counter is used to select only one node in x (x = counter)
	  //boolean Fijiyama3d=false;
	  if(origin.equals("Root System Analyzer")) {
		  counter = 5;
		  clock = 5;
	  }
	  if(origin.equals("Fijiyama ")) {
		  Fijiyama3d=true;
	  }
	  
	  org.w3c.dom.Node nn = parentDOM.getAttributes().getNamedItem("label");
	  if (nn != null) rootID = nn.getNodeValue();
	 
	  nn = parentDOM.getAttributes().getNamedItem("ID");
	  if (nn != null){
		  rootKey = nn.getNodeValue();
	  }
	 
	  nn = parentDOM.getAttributes().getNamedItem("po:accession");
	  if (nn != null) poIndex = rm.getIndexFromPo(nn.getNodeValue());
	 
	  
	  // Get the diameter nodes
	  org.w3c.dom.Node nodeDiameters = null; 
	  org.w3c.dom.Node nodeDiam = null;	  
      org.w3c.dom.Node nodeDOM = parentDOM.getFirstChild();
      while (nodeDOM != null) {
          String nName = nodeDOM.getNodeName();
          if(nName.equals("functions")){
			   org.w3c.dom.Node nodeFunctions = nodeDOM.getFirstChild();
			   while(nodeFunctions != null){
			      String fName = nodeFunctions.getNodeName();
		          if(fName.equals("function")){
				      String fAtt1 = nodeFunctions.getAttributes().getNamedItem("name").getNodeValue();
				      String fAtt2 = nodeFunctions.getAttributes().getNamedItem("domain").getNodeValue();
			          if(fAtt1.equals("diameter") & fAtt2.equals("polyline")){
			        	  nodeDiameters = nodeFunctions;
			        	  break;
			          }
		          }
		          nodeFunctions = nodeFunctions.getNextSibling();
			   }
          }
          nodeDOM = nodeDOM.getNextSibling(); 
      }
  
	  nodeDOM = parentDOM.getFirstChild();
      while (nodeDOM != null) {
         String nName = nodeDOM.getNodeName();
         // Nodes that are neither name, rulerAtOrigin nor Node elemnts are not considered
         // Read the geometry
         if (nName.equals("geometry")) {
			   org.w3c.dom.Node nodeGeom = nodeDOM.getFirstChild();
			   while (nodeGeom != null) {
				   	String geomName = nodeGeom.getNodeName();
				   if (geomName.equals("polyline")) {
					   org.w3c.dom.Node nodePoint = nodeGeom.getFirstChild();
					   if(nodeDiameters != null) nodeDiam = nodeDiameters.getFirstChild();
					   while (nodePoint != null) {
						   String pointName = nodePoint.getNodeName();
						   if (pointName.equals("point")) {
							   	if(counter == clock){
							   		Node no = addNode(0.0f, 0.0f, false);
			        			 	no.readRSML(nodePoint, nodeDiam, 1);
			        			 	counter = 0;
							   	}
							   	counter++;
						   }
						   nodePoint = nodePoint.getNextSibling();
						   if(nodeDiam != null) nodeDiam = nodeDiam.getNextSibling();
					   }
					   this.firstNode.calcCLength(0.0f);
					   if(validate()){
						   rm.rootList.add(this);
					   }
					   if(parentRoot != null) {
						   attachParent(parentRoot);
						   parentRoot.attachChild(this);
					   }
				   }
				   nodeGeom = nodeGeom.getNextSibling();
			   }
         }
         // Read child roots
         else if (nName.equals("root")){
        	 new Root(dpi, nodeDOM, true, this, rm, origin);
         }
         nodeDOM = nodeDOM.getNextSibling();
      } 

      if(rootKey.equals("")) rootKey = this.getNewRootKey();
      if(rootID.equals(noName)){
    	  rootID = "root_"+rm.nextAutoRootID;
    	  rm.nextAutoRootID++;
      }
      
      Node n = firstNode;
      while(n != null){
    	  if(n.diameter == 0){
    		  //SR.write("Recenter node");
    		 // rm.fit.reCenter(n, 0.05f, 0.5f, true, 1);
    	  }
    	  n = n.child;
      }
      }
      
   
   
   
   
   
   /**
    * Read RSML.
    *
    * @param parentDOM the parent DOM
    * @param rm the rm
    * @param parentRoot the parent root
    * @param origin the origin
    * @param timeLapseModel the time lapse model
    */
   public void readRSML(org.w3c.dom.Node parentDOM, RootModel rm, Root parentRoot, String origin,boolean timeLapseModel) {
	  int counter = 1, clock = 1; // The counter is used to select only one node in x (x = counter)
	  //boolean Fijiyama3d=false;
	  if(origin.equals("Root System Analyzer")) {
		  counter = 5;
		  clock = 5;
	  }
	  if(origin.equals("Fijiyama ")) {
		  Fijiyama3d=true;
		  counter = 5;
		  clock = 5;
	  }
	  
	  org.w3c.dom.Node nn = parentDOM.getAttributes().getNamedItem("label");
	  if (nn != null) rootID = nn.getNodeValue();
	 
	  nn = parentDOM.getAttributes().getNamedItem("ID");
	  if (nn != null){
		  rootKey = nn.getNodeValue();
	  }
	 
	  nn = parentDOM.getAttributes().getNamedItem("po:accession");
	  if (nn != null) poIndex = rm.getIndexFromPo(nn.getNodeValue());
	 
	  
	  // Get the diameter nodes
	  org.w3c.dom.Node nodeDiameters = null; 
	  org.w3c.dom.Node nodeDiam = null;	  
      org.w3c.dom.Node nodeDOM = parentDOM.getFirstChild();
      while (nodeDOM != null) {
          String nName = nodeDOM.getNodeName();
          if(nName.equals("functions")){
			   org.w3c.dom.Node nodeFunctions = nodeDOM.getFirstChild();
			   while(nodeFunctions != null){
			      String fName = nodeFunctions.getNodeName();
		          if(fName.equals("function")){
				      String fAtt1 = nodeFunctions.getAttributes().getNamedItem("name").getNodeValue();
				      String fAtt2 = nodeFunctions.getAttributes().getNamedItem("domain").getNodeValue();
			          if(fAtt1.equals("diameter") & fAtt2.equals("polyline")){
			        	  nodeDiameters = nodeFunctions;
			        	  break;
			          }
		          }
		          nodeFunctions = nodeFunctions.getNextSibling();
			   }
          }
          nodeDOM = nodeDOM.getNextSibling(); 
      }
  
	  nodeDOM = parentDOM.getFirstChild();
      while (nodeDOM != null) {
         String nName = nodeDOM.getNodeName();
         // Nodes that are neither name, rulerAtOrigin nor Node elemnts are not considered
         // Read the geometry
         if (nName.equals("geometry")) {
			   org.w3c.dom.Node nodeGeom = nodeDOM.getFirstChild();
			   while (nodeGeom != null) {
				   	String geomName = nodeGeom.getNodeName();
				   if (geomName.equals("polyline")) {
 					   boolean hasReadFirstChild=false;
					   org.w3c.dom.Node nodePoint = nodeGeom.getFirstChild();
					   if(nodeDiameters != null) nodeDiam = nodeDiameters.getFirstChild();
					   while (nodePoint != null) {
						   String pointName = nodePoint.getNodeName();
						   if (pointName.equals("point")) {
							   	if(true || counter == clock){
							   		Node no = addNode(0.0f, 0.0f, false);
			        			 	
							   		no.readRSML(nodePoint, timeLapseModel);
			        			 	if(!hasReadFirstChild) {
			        			 		this.parentNode=no;
			        			 		hasReadFirstChild=true;
			        			 	}
							   		counter = 0;
							   	}
							   	counter++;
						   }
						   nodePoint = nodePoint.getNextSibling();
						   if(nodeDiam != null) nodeDiam = nodeDiam.getNextSibling();
					   }
					   this.firstNode.calcCLength(0.0f);
					   if(validate()){
						   rm.rootList.add(this);
					   }
					   if(parentRoot != null) {
						   attachParent(parentRoot);
						   parentRoot.attachChild(this);
					   }
				   }
				   nodeGeom = nodeGeom.getNextSibling();
			   }
         }
         // Read child roots
         else if (nName.equals("root")){
        	 Root r=new Root(dpi, nodeDOM, true, this, rm, origin);
		     r.attachParent(this);
			 this.attachChild(r);
			 rm.rootList.add(r);
        }
         nodeDOM = nodeDOM.getNextSibling();
      } 

      if(rootKey.equals("")) rootKey = this.getNewRootKey();
      if(rootID.equals(noName)){
    	  rootID = "root_"+rm.nextAutoRootID;
    	  rm.nextAutoRootID++;
      }
      
      Node n = firstNode;
      while(n != null){
    	  if(n.diameter == 0){
    		  //SR.write("Recenter node");
    		 // rm.fit.reCenter(n, 0.05f, 0.5f, true, 1);
    	  }
    	  n = n.child;
      }
      }
      
   /**
    * To string.
    *
    * @return the string
    */
   public String toString() {
	   return("Root: order="+order+" nbNodes="+nNodes+"\n    parentNode="+parentNode+"\n    first node="+firstNode+"\n    last node="+lastNode);
   }

   /**
    * Set the DPI value for internal reference.
    *
    * @param dpi the new dpi
    */
   public void setDPI(float dpi) {
      this.dpi = dpi;
      pixelSize = ((float) 2.54 / dpi);
      }

   
   /**
    * Set the plant ontology accession to a new value.
    *
    * @param po the new po accession
    */
   public void setPoAccession(int po) {
      this.poIndex = (rootID.length() == 0) ? 0 : po;
      }   
   
   /**
    * Set the root name to a new value.
    *
    * @param rootID the new root ID
    */
   public void setRootID(String rootID) {
      this.rootID = (rootID.length() == 0) ? noName : rootID;
      updateChildren();
      }
   
   /**
    * Set the root key to a new value.
    *
    * @param rootKey the new root key
    */
   public void setRootKey(String rootKey) {
      this.rootKey = (rootKey.length() == 0) ? noName : rootKey;
      updateChildren();
      }

   /**
    * Set the orign of the ruler (base of the root).
    *
    * @param rulerAtOrigin the new ruler at origin
    */
   public void setRulerAtOrigin(float rulerAtOrigin) {
      this.rulerAtOrigin = rulerAtOrigin;
      if (anchor != null) {
         float v = lPosPixelsToCm(anchor.lPos);
         anchor.value = String.valueOf(Math.round(v * 100.0) / 100.0);
         anchor.needsRefresh();
         }
      }
      
   /**
    * Is the root valid (has at least one node).
    *
    * @return true, if successful
    */
   public boolean validate() {
      if (lastNode == null) {
         //IJ.showMessage("Internal data for root " + rootID + " appears to be corrupted." + 
         //      " They will not be written to the datafile and you will have to retrace the root.");
         return false;
      }
      return (!Float.isNaN(lastNode.cLength));
      }

/*
 *  All the methods hereafter are involved in the parent / child relationship
 */

   /**
 * Attach the selected root to the parent and set child informations.
 *
 * @param r the r
 */
   public void attachChild(Root r){
	   childList.add(r);
	   setFirstChild();
	   setLastChild();
	   setChildDensity();
   }
   
   
   /**
    * Update the children information.
    */
   public void updateChildren(){
	   for (int i = 0 ; i < childList.size() ; i++){
		   Root c = (Root) childList.get(i);
		   c.updateRoot();
	   }
	   if(childList.size() > 0){
		   setFirstChild();
		   setLastChild();
		   setChildDensity();
	   }
   }

      
   /**
    * Set if the root is a child.
    *
    * @param l true if child
    */
   public void isChild(int l){ isChild = l; }
   
   /**
    * Checks if is child.
    *
    * @return true if the root is child
    */
   public int isChild(){ return isChild; }
      
   
   /**
    * Attach the selected root as parent.
    *
    * @param r parent root
    */
   public void attachParent(Root r){	   
	   parent = r;
	   isChild(parent.isChild() + 1);
	   setParentNode();
	   setDistanceFromBase();
	   setDistanceFromApex();
	   setInsertAngl();
	   setInterBranch();
	   setParentName(parent.getRootID());
	   updateChildren();
	   poIndex = 2;
   }
   
   /**
    * Update the root information relative to his parent.
    */
   public void updateRoot(){
	   if(parent != null){
	   setDistanceFromBase();
	   setDistanceFromApex();
	   setInsertAngl();
	   setInterBranch();
	   isChild(parent.isChild() + 1);
	   setParentName(parent.getRootID());
	   setParentKey(parent.getRootKey());
	   }
	   if(childList.size() > 0) updateChildren();
   }
   
   
   /**
    * set the distance from the parent apex.
    *
    * @param d distance from apex
    */
   public void setDistanceFromApex(float d){ distanceFromApex = d;}
   
   
   /**
    * Automatically set distance from parent apex.
    */
   public void setDistanceFromApex(){
	   if(parent != null) distanceFromApex = parent.getRootLength() - distanceFromBase;
	   if(distanceFromApex < 0) distanceFromApex = 0;
   }
   
   /**
    * set the distance from the parent base.
    *
    * @param d distance from base
    */
   public void setDistanceFromBase(float d){ distanceFromBase = d;}
   
   /**
    * Automatically set distance from parent base.
    */
   public void setDistanceFromBase(){

	   float dx;
		float dy;
		boolean up;
		Node n = firstNode;
		Node n1 = parentNode;
		Node n2;
		if (n1 != null && n1.parent != null && n1.child != null){
			
			dx = n.x - n1.child.x;
			dy = n.y - n1.child.y;
			float dChild = (float) Math.sqrt( dx * dx + dy * dy);
			
			dx = n.x - n1.parent.x;
			dy = n.y - n1.parent.y;
			float dParent = (float) Math.sqrt( dx * dx + dy * dy);
			
			if(dParent < dChild){
				up = false;
				n2 = n1.parent;
			}
			else{
				up = true;
				n2 = n1.child;
			}

			int inc = 20;
			float stepX = (n2.x - n1.x) / inc;
			float stepY = (n2.y - n1.y) / inc;
			float minDist = 1000;
			float dist = 0;; float x; float y;
			for(int i = 0; i <= inc; i++){
				x = (n2.x + (stepX*i)) - n.x;
				y = (n2.y + (stepY*i)) - n.y;
				dist = (float) Math.sqrt( x * x + y * y);
				if(dist < minDist){
					minDist = dist;
				}
			}
			
			float dl1 = minDist ;
			
			if(up) distanceFromBase = n2.cLength - dl1;
			else distanceFromBase = n2.cLength + dl1;
		}
//		distanceFromBase = parentNode.cLength;
		if(distanceFromBase < 0) distanceFromBase = 0;
   }
   
   /**
    * Set which child is the first one on the root (closest from the base).
    *
    * @return true if there is at least one child, false if not.
    */
   public boolean setFirstChild(){
	   if (childList.size() == 0){
		   firstChild = null;
		   return false;
	   }
	   Root fc = (Root) childList.get(0);
	   for (int i = 0; i < childList.size(); i++){
		   Root c = (Root) childList.get(i);
		   if (c.getDistanceFromApex() > fc.getDistanceFromApex()) fc = c;
	   }
	   firstChild = fc;
	   return true;   
   }
   
   /**
    * Set which child is the last one on the root (closest from the apex).
    *
    * @return true if there is at least one child, false if not.
    */
   public boolean setLastChild(){
	   if (childList.size() == 0){
		   lastChild = null;
		   return false;
	   }
	   Root fc = (Root) childList.get(0);
	   for (int i = 0; i < childList.size(); i++){
		   Root c = (Root) childList.get(i);
		   if (c.getDistanceFromApex() < fc.getDistanceFromApex()) fc = c;
	   }
	   lastChild = fc;
	   lastChild.setDistanceFromBase();
	   addMark(Mark.MDL, "0", lPosPixelsToCm(lastChild.distanceFromBase));
	   return true; 
   }
   
   /**
    * Set the child density of the root inside the ramified region.
    */
   public void setChildDensity(){
	   float dist = lPosPixelsToCm(lastChild.getDistanceFromBase() - firstChild.getDistanceFromBase());
	   if (dist != 0) childDensity = childList.size() / dist;
   }
   
   /**
    * set the insertion angle of the root in its parent.
    *
    * @param ins the insertion angle
    */
   public void setInsertAngl(float ins){insertAngl = ins;}  
   
   
   /**
    * Automatically set the insertion angle of the root on its parent.
    */
   public void setInsertAngl(){
	   Node n = firstNode;
	   int count = 3;
	   float ang = 0;
	   
	   int i = 0;
	   while(n.child != null && i < count){
		   ang += n.theta;
		   n = n.child;
		   i++;
	   }
	   ang = ang / i;

	   if (ang > parentNode.theta) insertAngl = ang - parentNode.theta;
	   else insertAngl = parentNode.theta - ang;
	   if (insertAngl > (float) Math.PI ) insertAngl = (2 *(float) Math.PI) - insertAngl;
   }
      
   
   /**
    * Set the parentNode, which is the closest node in the parent from the base node of root.
    */
   public void setParentNode(){
	   
		Node n = firstNode;
		Root p = (Root) getParent();
		if( p == null){
			parentNode = null;
			return;
		}
		Node np = p.firstNode;
		Node npFinal = p.firstNode;
		double dMin = Point2D.distance((double) n.x, (double) n.y, (double) np.x, (double) np.y);
		double d;
		while (np.child != null){
			np = np.child;
			d = Point2D.distance((double) n.x, (double) n.y, (double) np.x, (double) np.y);
			if (d < dMin){
				dMin = d;
				npFinal = np;
			}
		}	   
		parentNode = npFinal;
	}
      
   
   /**
    * Set the interbranch distance between this root and the previous child on the parent.
    *
    * @param iB the interbranch
    */
   public void setInterBranch(float iB) {interBranch = iB;}
   
   
   /**
    * Automatically set the interbranch distance between this root and the previous child on the parent.
    */
   public void setInterBranch(){
	   if (isChild() == 0){ return; }
	   if (this == parent.firstChild){
		   interBranch = 0;
		   return; }
	   float dist = 0;
	   Root r;
	   for( int i = 0 ; i < parent.childList.size(); i++){
		   r =(Root) parent.childList.get(i);
		   if (this == r) continue;
		   float d = r.getDistanceFromApex() - this.getDistanceFromApex();
		   if (i == 0) dist = Math.abs(d);
		   if (d > 0 && d < dist ){
			   dist = d;
		   }
	   }
	   interBranch = dist;
   }
   
   /**
    * get the closest node in the root from a given position on the root.
    *
    * @param lPos the position on the root
    * @return the closest node
    */
   public Node getClosestNode (float lPos){
	   Node n0 = firstNode;
	   Node n1 = firstNode;
	   float d0 = Math.abs(n0.cLength - lPos);
	   float d1 = 0;
	   while (n0.child != null){
		   n0 = n0.child;
		   d1 = Math.abs(n0.cLength - lPos);
		   if(d1 < d0){
			   d0 = d1;
			   n1 = n0;
		   }
	   }
	   return n1;
   }
   
   
   /**
    * Set the name of the parent (internal reference, does not change the value in the parent).
    *
    * @param n the new parent name
    */
   public void setParentName(String n){
	   parentName = n;
   }
   
   /**
    * Set the identifier of the parent (internal reference, does not change the value in the parent).
    *
    * @param n the new parent key
    */
   public void setParentKey(String n){
	   parentKey = n;
   }
   
   /**
    * Get the object of the root parent.
    *
    * @return parent the parent root
    */
   public Root getParent(){ return parent; }
   
   /**
    * Get the parent's closest node from the base of this root.
    *
    * @return parentNode the parent's closest node from the base of this root
    */
   public Node getParentNode(){ return parentNode;}
   
   /**
    * the distance of the insertion point from the parent's apex.
    *
    * @return distanceFromApex the distance of the the insertion point from the parent's apex
    */
   public float getDistanceFromApex(){ return distanceFromApex;}

   /**
    * Get the distance of the insertion point from the parent's base.
    *
    * @return distanceFromBase the distance of the insertion point from the parent's base
    */
   public float getDistanceFromBase(){ 
	  if(isChild() > 0) return distanceFromBase;
	  else return -1;
   }
   
   /**
    * Get the parent root name (id any).
    *
    * @return parentName the name of the parent
    */
   public String getParentName(){ 
	   if(parentName != null) return parentName;
	   else return "-1";
	   }
   
   /**
    * Get the interbranch distance.
    *
    * @return interBranch the inter branch distance between this root and the previous one on the parent
    */
   public float getInterBranch() {return interBranch;}
   
   
   
   /**
    * Get the lateral density insde the ramified region.
    *
    * @return childDensity the child density inside the ramified region
    */
   public float getChildDensity() {
	   if(childList.size() > 0){
		   float dist = lPosPixelsToCm(lastChild.getDistanceFromBase() - firstChild.getDistanceFromBase());
		   if (dist != 0) return childList.size() / dist;
		   else return 0;
	   }
	   else return 0;
   }

   /**
    * Get the object of the lateral root the closest to the base.
    *
    * @return firstChild the first lateral root (the closest from the base)
    */
   public Root getFirstChild(){return firstChild;}

   /**
    * Get the object of the lateral root the closest to the apex.
    *
    * @return lastChild the last lateral root (the closest from the apex)
    */
   public Root getLastChild(){return lastChild;}
   
   /**
    * Get the insertion angle of the root.
    *
    * @return insertAngl the insertion angle on the parent root
    */
   public float getInsertAngl(){
	   if(isChild() > 0) return insertAngl;
	   else return -1;
	   }

   
   /**
    * The average diameter of all the nodes.
    *
    * @return the AVG diameter
    */
   public float getAVGDiameter(){
	   float n = 0;
	   int m = 0;
	   Node node = this.firstNode;
		n += node.diameter;
		m++;
		while (node.child != null){
			node = node.child;
			n += node.diameter;
			m++;
		}
		return (n / m) * pixelSize;   
   }
   
   /**
    * Get the average interbanch distance of the lateral roots.
    *
    * @return the AVG inter branch distance
    */
   public float getAVGInterBranchDistance(){
	   float n = 0;
	   int m = 0;
	   for(int i = 0 ; i < childList.size() ; i++){
		   Root r = (Root) childList.get(i);
		   n += r.getInterBranch();
		   m++;
	   }
	   return (n / m ) * pixelSize;   
   }
   

 
   
   /**
    * Get the volume of the root. The root is considered as a succesion of truncated cones
    *
    * @return the root volume
    */
   public float getRootVolume(){
	   float vol = 0;
	   Node n = this.firstNode;
		while (n.child != null){
			n = n.child;
			double B = Math.pow(((n.parent.diameter * pixelSize) / 2), 2) * Math.PI;
			double b = Math.pow(((n.diameter * pixelSize) / 2), 2) * Math.PI;
			vol += ((n.length * pixelSize) /3) * ( B + b + Math.sqrt(B * b));
		}	   
		return vol;
   }

   
   /**
    * Get the surface of the root.
    *
    * @return the root surface
    */
   public float getRootSurface(){
	   float surf = 0;
	   Node n = this.firstNode;
		while (n.child != null){
			n = n.child;
			double B = n.parent.diameter * pixelSize * Math.PI;
			double b = n.diameter * pixelSize * Math.PI;
			surf += (n.length * pixelSize) * ( (B + b) / 2);
		}	   
		return surf;
   }
   
   /**
    * Get the surface of the lateral roots (if any).
    *
    * @return the children surface
    */
   public float getChildrenSurface(){
	   float surf = 0;
	   for(int i = 0; i < childList.size(); i++){
		   Node n = childList.get(i).firstNode;
			while (n.child != null){
				n = n.child;
				double B = n.parent.diameter * pixelSize * Math.PI;
				double b = n.diameter * pixelSize * Math.PI;
				surf += (n.length * pixelSize) * ( (B + b) / 2);
			}	   
	   }
		return surf;
   }   
  
   
   /**
    * Get the root key.
    *
    * @return the root key
    */
   public String getRootKey(){
	   return rootKey;
   } 
   
   /**
    * Get a new root id value.
    *
    * @return unique root identifier
    */
   public String getNewRootKey(){
   	return UUID.randomUUID().toString();
   }
   
   
   /**
    * Return an integer indicating if the root is on the right or the left side of its parent (if it is a lateral).
    *
    * @return 0 if not a lateral; 1 if on the left, 2 if on the right
    */
   public int isLeftRight(){
	   
	   if(isChild() == 0) return 0;
	   else{
		   if(this.firstNode != null && this.parentNode != null){
			   if(this.firstNode.x < this.parentNode.x) return 1;
		   		else return 2;
		   }
		   else return 0;
	   }
	   
   }

/**
 * Returns the tortuosity of the root.
 *
 * @return the tortuosity
 */
public float getTortuosity() {
	float tort = 0;
	Node n = this.firstNode;
	Node nPrev = this.firstNode;
	int inc = 0;
	while (n.child != null){
		n = n.child;
		tort += Math.abs(n.theta - nPrev.theta);
		nPrev = n;
		inc++;
	}	   
	return tort / inc;	
}

/**
 * Return the vector lenght of the root (as defined in Armengaud 2009).
 *
 * @return the vector length
 */
public float getVectorLength() {
	Node n1 = this.firstNode;
	Node n2 = this.lastNode;   
	return (float) Math.sqrt(Math.pow((n1.x-n2.x),2) + Math.pow((n1.y-n2.y),2));
}

/**
 * Return the total length of all the children.
 *
 * @return the children length
 */
public float getChildrenLength() {
	float cl = 0;   
	for(int i = 0; i < childList.size(); i++){
		cl = cl + childList.get(i).getRootLength();
	}
	return cl;
}

/**
 * Return the mean children angle.
 *
 * @return the children angle
 */
public float getChildrenAngle(){
	if(childList.size() > 0){
	   float ang = 0;
	   for(int i = 0; i < childList.size(); i++){
		   ang += childList.get(i).getInsertAngl();	   
	   }
		return ang / childList.size();
	}
	else return 0; 
} 

/**
 * Return the min X coordinate of the root.
 *
 * @return the x min
 */
public float getXMin() {
	float min = 100000;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.x < min) min = n.x;
		n = n.child;
	}	   
	return min;	
}

/**
 * Gets the date max.
 *
 * @return the date max
 */
public float getDateMax() {
	float max = -100000;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.birthTime > max) max = n.birthTime;
		n = n.child;
	}	   
	return max;	
}

/**
 * Return the max X coordinate of the root.
 *
 * @return the x max
 */
public float getXMax() {
	float max = 0;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.x > max) max = n.x;
		n = n.child;
	}	   
	return max;	
}


/**
 * Return the min Y coordinate of the root.
 *
 * @return the y min
 */
public float getYMin() {
	float min = 100000;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.y < min) min = n.y;
		n = n.child;
	}	   
	return min;	
}

/**
 * Return the max Y coordinate of the root.
 *
 * @return the y max
 */
public float getYMax() {
	float max = 0;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.y > max) max = n.y;
		n = n.child;
	}	   
	return max;	
}

/**
 * Return the min X coordinate of the root and its children.
 *
 * @return the x min total
 */
public float getXMinTotal() {
	float min = 100000;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.x < min) min = n.x;
		n = n.child;
	}	
	for(int i = 0; i < childList.size(); i++){
		n = childList.get(i).firstNode;		
		while (n.child != null){
			if(n.x < min) min = n.x;
			n = n.child;
		}	
	}	
	return min;	
}


/**
 * Return the max X coordinate of the root and its children.
 *
 * @return the x max total
 */
public float getXMaxTotal() {
	float max = 0;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.x > max) max = n.x;
		n = n.child;
	}	   
	for(int i = 0; i < childList.size(); i++){
		n = childList.get(i).firstNode;		
		while (n.child != null){
			if(n.x > max) max = n.x;
			n = n.child;
		}	
	}		
	return max;	
}


/**
 * Return the min Y coordinate of the root and its children.
 *
 * @return the y min total
 */
public float getYMinTotal() {
	float min = 100000;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.y < min) min = n.y;
		n = n.child;
	}	   
	for(int i = 0; i < childList.size(); i++){
		n = childList.get(i).firstNode;		
		while (n.child != null){
			if(n.y < min) min = n.y;
			n = n.child;
		}	
	}	
	return min;	
}

/**
 * Return the max Y coordinate of the root and its children.
 *
 * @return the y max total
 */
public float getYMaxTotal() {
	float max = 0;
	Node n = this.firstNode;
	while (n.child != null){
		if(n.y > max) max = n.y;
		n = n.child;
	}	   
	for(int i = 0; i < childList.size(); i++){
		n = childList.get(i).firstNode;		
		while (n.child != null){
			if(n.y > max) max = n.y;
			n = n.child;
		}	
	}
	return max;	
}

/**
 * Return the average orientation of the root.
 *
 * @return the root orientation
 */
public float getRootOrientation() {
	float angle = 0;
	int count = 0;
	Node n = this.firstNode;
	while (n.child != null){
		angle += n.theta;
		count ++;
		n = n.child;
	}	   	
	return (angle / count);	
}

  
/**
 * Get the Plant Ontology accession of the root.
 *
 * @return the po accession
 */
public String getPoAccession(){
	return FSR.listPoNames[poIndex];
}


/**
 * Get the convexhull area.
 *
 * @return the convex hull area
 */
public float getConvexHullArea(){
	return 0;
}

/**
 * Get the root convexhull.
 *
 * @return the convex hull
 */
public PolygonRoi getConvexHull(){
		
	List<Integer> xList = new ArrayList<Integer>(); 
	List<Integer> yList = new ArrayList<Integer>();
	
	// Add all the nodes coordinates
	Node n = this.firstNode;
	while (n.child != null){
		xList.add((int) n.x);
		yList.add((int) n.y);
		n = n.child;
	}
	for(int i = 0; i < childList.size(); i++){
		Root r = childList.get(i);
		n = r.firstNode;
		while (n.child != null){
			xList.add((int) n.x);
			yList.add((int) n.y);
			n = n.child;
		}
	}
	int[] xRoiNew = new int[xList.size()];
	int[] yRoiNew = new int[yList.size()];
	for(int l = 0; l < yList.size(); l++){
		xRoiNew[l] = xList.get(l);
		yRoiNew[l] = yList.get(l);
	}
	
	Roi roi = new PolygonRoi(xRoiNew, yRoiNew, yRoiNew.length, Roi.POLYGON);
	return new PolygonRoi(roi.getConvexHull(),  Roi.POLYGON);
	
}

/**
 * Compare to.
 *
 * @param arg0 the arg 0
 * @return the int
 */
@Override
public int compareTo(Root arg0) {
	if(this.firstNode.x==arg0.firstNode.x)return 0;
	else if(this.firstNode.x<arg0.firstNode.x)return -1;
	else return 1;
}

}
