package fr.cirad.image.rsmlviewer;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.UUID;

import fr.cirad.image.rsmlviewer.FSR;
import fr.cirad.image.rsmlviewer.Mark;
import fr.cirad.image.rsmlviewer.Node;
import fr.cirad.image.rsmlviewer.Root;
import fr.cirad.image.rsmlviewer.RootModel;

  
/** 
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge 
 * 
 * Root class.
 * */


// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

class Root{
   Node firstNode, lastNode;
   int nNodes;
   public ArrayList<Root> childList = new ArrayList<Root>();
   
   String rootKey = "";
   String parentKey = null;
   int poIndex = 1;
   
   Node parentNode;
   Root parent;
   Root firstChild;
   Root lastChild;
   float distanceFromApex = 0;
   float distanceFromBase = 0;
   float childDensity = 0;
   float insertAngl = 0;
   float interBranch = 0;
   int isChild = 0;
   String parentName;
   int gM = 0;
   
   ArrayList<Root> deletedRoot, keepRoot;
   public Vector<Mark> markList;
   public Mark anchor, MDL;
   float rulerAtOrigin = 0.0f;  // in pixel units
   private String rootID = "";
   private float dpi, pixelSize;
   
   public GeneralPath bordersGP = new GeneralPath();
   public GeneralPath axisGP = new GeneralPath();
   public GeneralPath nodesGP = new GeneralPath();
   public GeneralPath ticksGP = new GeneralPath();
   public GeneralPath tipsGP = new GeneralPath();
   public GeneralPath convexhullGP = new GeneralPath();

   /** new */
   public GeneralPath parallelsGP = new GeneralPath();
   
   
   static String noName = FSR.prefs.get("root_ID", "root_");
   static Color axisColor = Color.green;
   static Color nodesColor = Color.orange;
   static Color bordersColor = Color.red;
   static Color areaColor = Color.yellow;
   static Color ticksColor = Color.yellow;
   static Color tipsColor = Color.yellow;
   static Color childTipsColor = Color.green;
   static Color childNodesColor = Color.green;
   static Color childAxisColor = Color.yellow;
   static Color childBordersColor = Color.orange;
   
   static int nextRootKey = 1;

   /**
    * Constructor
    * Used when opening a xml file
    * @param dpi
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
    * Add a node to the root
    * @param x
    * @param y
    * @param addToBase
    * @return
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
    * Add a mark to the root
    * @param type
    * @param value
    * @param markerPosition
    * @return
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
    * Add a mark to the root
    * @param m
    */
   public void addMark(Mark m) {
      if (m.type == Mark.ANCHOR) {
         anchor = m;
         }
      else markList.add(m);
      }


   /**
    * compute the number of nodes inside the root
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
    * Delete all the root informations
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
    * Ge the root name
    * @return
    */
   public String getRootID() {return rootID; }
 
   /**
    * Ge the root length
    * @return
    */
   public float getRootLength() {return lastNode.cLength + rulerAtOrigin; }

   /**
    * Get the value of the ruelr at origin
    * @return
    */
   public float getRulerAtOrigin() {return rulerAtOrigin; }
   
   /**
    * Convert the longitudinal position from cm to pixels
    * @param cm
    * @return
    */
   public float lPosCmToPixels(float cm) {
      return cm / pixelSize - this.rulerAtOrigin;
      }

   /**
    * Convert the longitudinal position from pixels to cm
    * @param pixels
    * @return
    */
   public float lPosPixelsToCm(float pixels) {
      return pixelSize * (pixels + this.rulerAtOrigin);
      }

   
   /**
    * Read the RSML file
    * @param parentDOM
    */
   public void readRSML(org.w3c.dom.Node parentDOM, RootModel rm, Root parentRoot, String origin) {
	  
	  int counter = 1, clock = 1; // The counter is used to select only one node in x (x = counter)
	  if(origin.equals("Root System Analyzer")) {
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
    * Set the DPI value for internal reference
    * @param dpi
    */
   public void setDPI(float dpi) {
      this.dpi = dpi;
      pixelSize = ((float) 2.54 / dpi);
      }

   
   /**
    * Set the plant ontology accession to a new value
    * @param po
    */
   public void setPoAccession(int po) {
      this.poIndex = (rootID.length() == 0) ? 0 : po;
      }   
   
   /**
    * Set the root name to a new value
    * @param rootID
    */
   public void setRootID(String rootID) {
      this.rootID = (rootID.length() == 0) ? noName : rootID;
      updateChildren();
      }
   
   /**
    * Set the root key to a new value
    * @param rootID
    */
   public void setRootKey(String rootKey) {
      this.rootKey = (rootKey.length() == 0) ? noName : rootKey;
      updateChildren();
      }

   /**
    * Set the orign of the ruler (base of the root)
    * @param rulerAtOrigin
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
    * Is the root valid (has at least one node)
    * @return
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
    * Attach the selected root to the parent and set child informations
    * @param r the root to be attach
    */
   public void attachChild(Root r){
	   childList.add(r);
	   setFirstChild();
	   setLastChild();
	   setChildDensity();
   }
   
   
   /**
    * Update the children information
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
    * Set if the root is a child
    * @param l true if child
    */
   public void isChild(int l){ isChild = l; }
   
   /**
    * 
    * @return true if the root is child
    */
   public int isChild(){ return isChild; }
      
   
   /**
    * Attach the selected root as parent
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
    * Update the root information relative to his parent
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
    * set the distance from the parent apex
    * @param d distance from apex
    */
   public void setDistanceFromApex(float d){ distanceFromApex = d;}
   
   
   /**
    * Automatically set distance from parent apex 
    */
   public void setDistanceFromApex(){
	   if(parent != null) distanceFromApex = parent.getRootLength() - distanceFromBase;
	   if(distanceFromApex < 0) distanceFromApex = 0;
   }
   
   /**
    * set the distance from the parent base
    * @param d distance from base
    */
   public void setDistanceFromBase(float d){ distanceFromBase = d;}
   
   /**
    * Automatically set distance from parent base 
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
    * Set which child is the first one on the root (closest from the base)
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
    * Set which child is the last one on the root (closest from the apex)
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
    * set the insertion angle of the root in its parent
    * @param ins the insertion angle
    */
   public void setInsertAngl(float ins){insertAngl = ins;}  
   
   
   /**
    * Automatically set the insertion angle of the root on its parent
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
    * Set the parentNode, which is the closest node in the parent from the base node of root
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
    * Set the interbranch distance between this root and the previous child on the parent
    * @param iB the interbranch
    */
   public void setInterBranch(float iB) {interBranch = iB;}
   
   
   /**
    * Automatically set the interbranch distance between this root and the previous child on the parent
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
    * get the closest node in the root from a given position on the root
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
    * Set the name of the parent (internal reference, does not change the value in the parent)
    * @param n
    */
   public void setParentName(String n){
	   parentName = n;
   }
   
   /**
    * Set the identifier of the parent (internal reference, does not change the value in the parent)
    * @param n
    */
   public void setParentKey(String n){
	   parentKey = n;
   }
   
   /**
    * Get the object of the root parent
    * @return parent the parent root
    */
   public Root getParent(){ return parent; }
   
   /**
    * Get the parent's closest node from the base of this root
    * @return parentNode the parent's closest node from the base of this root
    */
   public Node getParentNode(){ return parentNode;}
   
   /**
    * the distance of the insertion point from the parent's apex
    * @return distanceFromApex the distance of the the insertion point from the parent's apex
    */
   public float getDistanceFromApex(){ return distanceFromApex;}

   /**
    * Get the distance of the insertion point from the parent's base
    * @return distanceFromBase the distance of the insertion point from the parent's base
    */
   public float getDistanceFromBase(){ 
	  if(isChild() > 0) return distanceFromBase;
	  else return -1;
   }
   
   /**
    * Get the parent root name (id any)
    * @return parentName the name of the parent
    */
   public String getParentName(){ 
	   if(parentName != null) return parentName;
	   else return "-1";
	   }
   
   /**
    * Get the interbranch distance
    * @return interBranch the inter branch distance between this root and the previous one on the parent
    */
   public float getInterBranch() {return interBranch;}
   
   
   
   /**
    * Get the lateral density insde the ramified region
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
    * Get the object of the lateral root the closest to the base
    * @return firstChild the first lateral root (the closest from the base)
    */
   public Root getFirstChild(){return firstChild;}

   /**
    * Get the object of the lateral root the closest to the apex
    * @return lastChild the last lateral root (the closest from the apex)
    */
   public Root getLastChild(){return lastChild;}
   
   /**
    * Get the insertion angle of the root
    * @return insertAngl the insertion angle on the parent root
    */
   public float getInsertAngl(){
	   if(isChild() > 0) return insertAngl;
	   else return -1;
	   }

   
   /**
    * The average diameter of all the nodes
    * @return
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
    * Get the average interbanch distance of the lateral roots
    * @return
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
    * @return the root id
    */
   public String toString(){
	   return rootID;
   }

   
   /**
    * Get the volume of the root. The root is considered as a succesion of truncated cones
    * @return
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
    * Get the surface of the root
    * @return
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
    * Get the surface of the lateral roots (if any)
    * @return
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
    * Get the root key
    * @return
    */
   public String getRootKey(){
	   return rootKey;
   } 
   
   /**
    * Get a new root id value
    * @return unique root identifier
    */
   public String getNewRootKey(){
   	return UUID.randomUUID().toString();
   }
   
   
   /**
    * Return an integer indicating if the root is on the right or the left side of its parent (if it is a lateral)
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
 * Returns the tortuosity of the root
 * @return
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
 * Return the vector lenght of the root (as defined in Armengaud 2009)
 * @return
 */
public float getVectorLength() {
	Node n1 = this.firstNode;
	Node n2 = this.lastNode;   
	return (float) Math.sqrt(Math.pow((n1.x-n2.x),2) + Math.pow((n1.y-n2.y),2));
}

/**
 * Return the total length of all the children
 * @return
 */
public float getChildrenLength() {
	float cl = 0;   
	for(int i = 0; i < childList.size(); i++){
		cl = cl + childList.get(i).getRootLength();
	}
	return cl;
}

/**
 * Return the mean children angle 
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
 * Return the min X coordinate of the root
 * @return
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
 * Return the max X coordinate of the root
 * @return
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
 * Return the min Y coordinate of the root
 * @return
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
 * Return the max Y coordinate of the root
 * @return
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
 * Return the min X coordinate of the root and its children
 * @return
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
 * Return the max X coordinate of the root and its children
 * @return
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
 * Return the min Y coordinate of the root and its children
 * @return
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
 * Return the max Y coordinate of the root and its children
 * @return
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
 * Return the average orientation of the root
 * @return
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
 * Get the Plant Ontology accession of the root
 * @return
 */
public String getPoAccession(){
	return FSR.listPoNames[poIndex];
}


/**
 * Get the convexhull area
 * @return
 */
public float getConvexHullArea(){
	return 0;
}

/**
 * Get the root convexhull
 * @return
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

}
