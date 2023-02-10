/*
 * 
 */
package io.github.rocsg.fijiyama.rsml;
import java.awt.geom.*;

import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.rsml.Node;

// TODO: Auto-generated Javadoc
/**
 * The Class Node.
 *
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge
 * 
 * Node class.
 */


// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

/**
 * Constructor
 * @author guillaumelobet
 *
 */
public class Node {
   
   /** The birth time. */
   public float x, y, theta, length, cLength, diameter, birthTime,birthTimeHours;
   
   /** The vy. */
   public float vx,vy;
   
   /** The child. */
   // length and cLength are in pixels
   public Node child;
   
   /** The hidden way to child. */
   boolean hiddenWayToChild=false;
   
   /** The parent. */
   public Node parent;
   
   /** The distance. */
   public double distance;
   
   /** The needs refresh. */
   boolean needsRefresh;
   
   /** The b cross 23. */
   boolean bCross01 = false, bCross23 = false;
   
   /** The p cross 23. */
   boolean pCross01 = false, pCross23 = false;



  /**
   * Constructor.
   *
   * @param x the x
   * @param y the y
   * @param d the d
   * @param n the n
   * @param after the after
   */
   public Node(float x, float y, float d, Node n, boolean after) {
      this.x = x;
      this.y = y;
      this.diameter = d;
      if (after) {
         parent = n;
         if (parent != null) parent.child = this;
         child = null;
         }
      else {
         child = n;
         if (child != null) child.parent = this;
         parent = null;
         }
      needsRefresh = true;
      }

   /**
    * Instantiates a new node.
    *
    * @param x the x
    * @param y the y
    * @param n the n
    * @param after the after
    */
   public Node(float x, float y, Node n, boolean after) {
      this(x, y, 0f, n, after);
      }

   /**
    * Build the node.
    */
   public void buildNode() {
      if (diameter < 1.0f) diameter = 1.0f;

      // calculate length and theta where required
      if (parent != null) {
         float dx = x - parent.x;
         float dy = y - parent.y;
         parent.theta = vectToTheta(dx, dy);
         parent.length = norm(dx, dy);
         }
      if (child != null) {
         float dx = child.x - x;
         float dy = child.y - y;
         theta = vectToTheta(dx, dy);
         length = norm(dx, dy);
         }
      }

   /**
    * Norm.
    *
    * @param dx the dx
    * @param dy the dy
    * @return the float
    */
   public static float norm(float dx, float dy) {
	      return (float) Math.sqrt(dx * dx + dy * dy); 
	      }
	   
   /**
    * Checks for exact birth time.
    *
    * @return true, if successful
    */
   public boolean hasExactBirthTime() {
	   int bthInt=(int)Math.round(birthTime);
	   double delta=Math.abs(bthInt-birthTime);
	   return (delta<VitimageUtils.EPSILON);
   }
   
   /**
    * Norm.
    *
    * @param x0 the x 0
    * @param y0 the y 0
    * @param x1 the x 1
    * @param y1 the y 1
    * @return the float
    */
   public static float norm(float x0, float y0, float x1, float y1) {
      return norm(x1 - x0, y1 - y0); 
      }
	   
   
   /**
    * Checks if is parent or equal.
    *
    * @param n the n
    * @return true, if is parent or equal
    */
   public boolean isParentOrEqual(Node n) {
	   Node nThis=this;
	   while(nThis!=null) {
		   if(n==nThis)return true;
		   nThis=nThis.child;
	   }
	   return false;
   }
  
   /**
    * Copy.
    *
    * @param n the n
    */
   public void copy(Node n) {
      x = n.x;
      y = n.y;
      theta = n.theta;
      length = n.length;
      cLength = n.cLength;
      diameter = n.diameter;
      birthTime=n.birthTime;
      birthTimeHours=n.birthTimeHours;
          if (parent != null) parent.needsRefresh = true;
      if (child != null) child.needsRefresh = true;
      needsRefresh = true;
      }

   /**
    * Translate.
    *
    * @param dx the dx
    * @param dy the dy
    */
   public void translate(float dx, float dy) {
      x += dx;
      y += dy;    
      needsRefresh = true;
      }

 

   /**
    * Get the distance between this node and an other one.
    *
    * @param n the n
    * @return the distance to
    */
   public float getDistanceTo (Node n) {
      float d = 0.0f;
      for (Node n1 = this; n1 != n; n1 = n1.child) { 
         if (n1 == null) return 0.0f;
         d += (float) Point2D.distance((double) n1.x, (double) n1.y,
                                       (double) n1.child.x, (double) n1.child.y);
         }
      return d;
      }

   
   /**
    * Distance between.
    *
    * @param n1 the n 1
    * @param n2 the n 2
    * @return the float
    */
   public static float distanceBetween(Node n1,Node n2) {
	   return (float) Point2D.distance((double) n1.x, (double) n1.y,(double) n2.x, (double) n2.y);
   }
   
   /**
    * Compute the length between the base of the root and the node.
    */
   public void calcCLength() {
      calcCLength(this.cLength);
      }

	/**
	 * Compute the length between the base of the root and the node.
	 *
	 * @param startValue the start value
	 */
   public void calcCLength(float startValue) {
      this.cLength = startValue;
      Node n = this;
      while (n.child != null) {
         n.child.cLength = n.cLength + n.length;
         n = n.child;
         }
      }
   
   /**
    * Read the node information from and RSML file.
    *
    * @param parentDOM the xml elemnt containg the x/y coordinates
    * @param diamDOM the xml element contining the diameter elements
    * @param dpi digit per inch
    */
   public void readRSML(org.w3c.dom.Node parentDOM, org.w3c.dom.Node diamDOM, float dpi) {
	   
		  org.w3c.dom.Node nn = parentDOM.getAttributes().getNamedItem("x");
		  if (nn != null) x = Float.valueOf(nn.getNodeValue()).floatValue() * dpi;
		  nn = parentDOM.getAttributes().getNamedItem("y");
		  if (nn != null) y = Float.valueOf(nn.getNodeValue()).floatValue() * dpi;
		  if(diamDOM != null){
			  diameter = Float.valueOf(diamDOM.getFirstChild().getNodeValue()).floatValue() * dpi;
		  }
		  else{
			  diameter = 2;
		  }
		  
	      if (parent != null) {
	          float dx = x - parent.x;
	          float dy = y - parent.y;
	          parent.theta = vectToTheta(dx, dy);
	          parent.length = (float) Math.sqrt(dx * dx + dy * dy);
//	          parent.calcBorders();
	          //parent.calcPoles(0);
	         }
	      needsRefresh = true;
	      }
   
   /**
    * Read RSML.
    *
    * @param parentDOM the parent DOM
    * @param timeLapse the time lapse
    */
   public void readRSML(org.w3c.dom.Node parentDOM,boolean timeLapse) {
		  org.w3c.dom.Node nn = parentDOM.getAttributes().getNamedItem("coord_t");
		  if (nn != null) birthTime = Float.valueOf(nn.getNodeValue());
		  nn = parentDOM.getAttributes().getNamedItem("coord_x");
		  if (nn != null) x = Float.valueOf(nn.getNodeValue()).floatValue();
		  nn = parentDOM.getAttributes().getNamedItem("coord_y");
		  if (nn != null) y = Float.valueOf(nn.getNodeValue()).floatValue();
		  nn = parentDOM.getAttributes().getNamedItem("diameter");
		  if (nn != null) diameter = Float.valueOf(nn.getNodeValue()).floatValue();
		  nn = parentDOM.getAttributes().getNamedItem("vx");
		  if (nn != null) vx = Float.valueOf(nn.getNodeValue()).floatValue();
		  nn = parentDOM.getAttributes().getNamedItem("vy");
		  if (nn != null) vy = Float.valueOf(nn.getNodeValue()).floatValue();
      }

   /**
    * To string.
    *
    * @return the string
    */
   public String toString() {
	   return("Node : x="+x+" y="+y+" t="+birthTime+" hours="+birthTimeHours+" diam="+diameter+" vx="+vx+" vy="+vy+" haschild ?"+(this.child==null)+" hasparent ?"+(this.parent==null));
   }
  
   /**
    * Convert a vector to an angle.
    *
    * @param dirX the dir X
    * @param dirY the dir Y
    * @return the float
    */
   public static float vectToTheta (float dirX, float dirY) {
	      float norm = (float) Math.sqrt(dirX * dirX + dirY * dirY);
	      return (float) (dirY <= 0 ? Math.acos(dirX / norm) 
	              : 2.0 * Math.PI - Math.acos(dirX / norm));      

	 }
}


