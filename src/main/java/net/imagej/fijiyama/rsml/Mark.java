package net.imagej.fijiyama.rsml;

import java.io.FileWriter;
import java.io.IOException;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.geom.GeneralPath;
import javax.swing.Icon;

import net.imagej.fijiyama.rsml.Mark;
import net.imagej.fijiyama.rsml.Root;

/**
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge 
 * Class for the Marks objects
 * */

public class Mark {
	// Mark types
   static String[] typeName = {"Free Text", "Number", "Most Distal Lateral", "Measure", "Interval", "Length", "Anchor"}; 
   // Default values for the different marks
   static String[] typeDefaultValue = {"", "0", "MDL", "MEASURE", "INTERVAL", "", "ANCHOR"};
   // Types for the different values
   static boolean[] typeNeedsValue = {true, true, false, false, false, true, true};
   // Painting flags for the different values
   static boolean[] typeValueNeedsPaint = {true, true, false, false, false, true, true};
   static Icon[] typeIcon = new Icon[typeName.length];
   
   // Flag for the different marks
   static final int FREE_TEXT = 0;
   static final int NUMBER = 1;
   static final int MDL = 2;
   static final int MEASURE = 3;
   static final int INTERVAL = 4;
   static final int LENGTH = 5;
   static final int ANCHOR = 6; // Keep ANCHOR the last one (to prevent it to be considered in the linked marks)
   
   public int type;	// The mark type
   public float lPos;  // in pixels, relative to polygon origin (not to ruler origin)
   public float angle;  // relative to horizontal
   public float diameter;
   public float twinLPos;
   public boolean isForeign = false;
   public String foreignImgName = null;  // XD 20110629
   public String value;
   float xLabel, yLabel;
   Root r;
   GeneralPath gp = new GeneralPath();
   Color contourColor = null, fillColor = null, valueColor = null;
   boolean needsRefresh;
   int dirLabel;
   
   /**
    * Contstructor
    * @param type
    * @param r
    * @param lPos
    * @param twinLPos
    * @param value
    * @param isForeign
    * @param imgName
    */
   public Mark(int type, Root r, float lPos, float twinLPos, String value, boolean isForeign, String imgName) {  // XD 20110629
	      this.type = type;
	      this.r = r;
	      this.lPos = lPos;
	      this.value = value;
	      this.isForeign = isForeign;
	      foreignImgName = imgName;  // XD 20110629
	      this.twinLPos = twinLPos;
	      needsRefresh = true;
	      }

   /**
    * Constructor
    * @param type
    * @param r
    * @param lPos
    * @param value
    */
   public Mark(int type, Root r, float lPos, String value) {
	      this(type, r, lPos, 0.0f, value, false, null);  // XD 20110629
	      }

   /**
    * Constructor
    * @param type
    * @param r
    * @param lPos
    * @param value
    * @param isForeign
    * @param imgName
    */
   public Mark(int type, Root r, float lPos, String value, boolean isForeign, String imgName) {  // XD 20110629
	      this(type, r, lPos, 0.0f, value, isForeign, imgName);  // XD 20110629
	      }
   
   /**
    * Get the positon of the twin mark
    * @param lPos
    */
   public void setTwinPosition(float lPos) {
      if (lPos <= this.lPos) {
         twinLPos = this.lPos;
         this.lPos = lPos;
         }
      else twinLPos = lPos;
      }


   /**
    * Return the value of the mark as a string
    * @return value
    */
   public String getValue() {return value;}

   /**
    * Does the mark need to be refreshed
    */
   public void needsRefresh() {
      needsRefresh = true;
      }
 

   /**
    * Read the mark information from the datafile
    * @param parentDOM the xml element containg the mark
    * @param r the current root
    * @return the Mark
    */
   static public Mark read(org.w3c.dom.Node parentDOM, Root r) {
      return read(parentDOM, r, false, null, 1.0f);
      }

   /**
    * Read the mark information from the datafile
    * @param parentDOMthe xml element containg the mark
    * @param r the current root
    * @param isForeign does the mak was imported from a different image?
    * @param imgName name of the image the makr is coming from
    * @param dpiRatio scale
    * @return the Mark
    */
   static public Mark read(org.w3c.dom.Node parentDOM, Root r, boolean isForeign, String imgName, float dpiRatio) {  // XD 20110629
      org.w3c.dom.Node nodeDOM = parentDOM.getFirstChild();
      String tn = null, value = null;
      float lPos = 0.0f, twinLPos = 0.0f;
      int type = -1;
      while (nodeDOM != null) {
         String nName = nodeDOM.getNodeName();
         if (nName.equals("typeName")) {
            tn = nodeDOM.getFirstChild().getNodeValue();
            for (type = typeName.length - 1; type >= 0; type--) {
               if (tn.equals(typeName[type])) break;
               }
            }
         else if (nName.equalsIgnoreCase("lPos")) lPos = dpiRatio * Float.valueOf(nodeDOM.getFirstChild().getNodeValue()).floatValue();
         else if (nName.equalsIgnoreCase("twinLPos")) twinLPos = dpiRatio * Float.valueOf(nodeDOM.getFirstChild().getNodeValue()).floatValue();
         else if (nName.equals("value")) {
            org.w3c.dom.Node childDOM = nodeDOM.getFirstChild();
            if (childDOM == null) value = "";
            else value = childDOM.getNodeValue();
            }
         nodeDOM = nodeDOM.getNextSibling();
         }
      return (type == -1) ? null : new Mark(type, r, lPos, twinLPos, value, isForeign, imgName);  // XD 20110629
      }
   
   
   /**
    * Save the mark to an RSML file
    * @param dataOut
    * @throws IOException
    */
   public void saveRSML(FileWriter dataOut) throws IOException {
	      if (isForeign) return;
	      String nL = System.getProperty("line.separator");
	      dataOut.write("				  <annotation name='"+typeName[type]+"'>" + nL);
	      dataOut.write("				    <lPos>" + Float.toString(lPos) + "</lPos>" + nL);
	      dataOut.write("				    <point x='"+ Float.toString(xLabel) + "' y='"+ Float.toString(yLabel) +"'/>" + nL);
	      dataOut.write("				    <value>" + value + "</value>" + nL);
	      dataOut.write("				    <software>smartroot</software>" + nL);
	      dataOut.write("				  </annotation>" + nL);
	      }
     
   /**
    * Get the number of possible marks
    * @return
    */
   public static int getTypeCount() {return typeName.length;}
   
   /**
    * Get the default value of the mark
    * @param type
    * @return
    */
   public static String getDefaultValue(int type) {return typeDefaultValue[type];}
   
   /**
    * Does the makr need a value
    * @param type of the mark
    * @return
    */
   public static boolean needsValue(int type) {return typeNeedsValue[type];}
      
   /**
    * Is the mark a interval mark
    * @param type
    * @return
    */
   public static boolean needsTwinPosition(int type) {return (type == INTERVAL);}

   /**
    * Is the mark a interval mark
    * @return 
    */
   public boolean needsTwinPosition() {return (type == INTERVAL);}
      
   /**
    * 
    * @return
    */
   public boolean needsValue() {return typeNeedsValue[type];}
    
   /**
     * Get the name of the mark  
     * @param type
     * @return the name
     */
   public static String getName(int type) {return typeName[type];}

   /**
    * Get the icon for the mark
    * @param type
    * @return the icon
    */
   public static Icon getIcon(int type) {return typeIcon[type];}

   /**
    * Get the type of the mark, as an integer
    * @param name
    * @return the type
    */
   public static int getTypeNum(String name) {
      int i = typeName.length - 1;
      while (i >= 0 && !typeName[i].equals(name)) i--;
      return i;  // return -1 if name is not a valid type name
      }
   
   /**
    * Move the makr along the root
    * @param delta the differenc between the current and previous position
    */
   public void move(float delta) {
      lPos += delta; 
      if (type == INTERVAL) twinLPos += delta;
      needsRefresh = true;
      }
   
   /**
    * 
    */
   static {
      typeIcon[FREE_TEXT] = new Icon() {
         int[] xPoints = {0,  0, 5};
         int[] yPoints = {0, 10, 5};
         public int getIconHeight() {return 10;}
         public int getIconWidth() {return 10;}
         public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(Color.green);
            g.fillPolygon(xPoints, yPoints, xPoints.length);
            g.setColor(Color.blue);
            g.drawPolygon(xPoints, yPoints, xPoints.length);
            g.translate(-x, -y);
            }
         };
     typeIcon[NUMBER] = new Icon() {
         int[] xPoints = {0,  0, 5};
         int[] yPoints = {0, 10, 5};
         public int getIconHeight() {return 10;}
         public int getIconWidth() {return 10;}
         public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(Color.yellow);
            g.fillPolygon(xPoints, yPoints, xPoints.length);
            g.setColor(Color.red);
            g.drawPolygon(xPoints, yPoints, xPoints.length);
            g.translate(-x, -y);
            }
         };   
      typeIcon[MDL] = new Icon() {
         int[] xPoints = {0,  0, 5};
         int[] yPoints = {0, 10, 5};
         public int getIconHeight() {return 10;}
         public int getIconWidth() {return 10;}
         public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(Color.orange);
            g.fillPolygon(xPoints, yPoints, xPoints.length);
            g.setColor(Color.green);
            g.drawPolygon(xPoints, yPoints, xPoints.length);
            g.translate(-x, -y);
            }
         };
      typeIcon[INTERVAL] = new Icon() {
         int[] xPoints1 = {0, 0, 4,  4, 0,  0};
         int[] yPoints1 = {0, 5, 0, 10, 5, 10};
         int[] xPoints2 = {10, 10, 6,  6, 10,  10};
         int[] yPoints2 = {0, 5, 0, 10, 5, 10};
         public int getIconHeight() {return 10;}
         public int getIconWidth() {return 10;}
         public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(Color.yellow);
            g.fillPolygon(xPoints1, yPoints1, xPoints1.length);
            g.fillPolygon(xPoints2, yPoints2, xPoints2.length);
            g.setColor(Color.green);
            g.drawPolygon(xPoints1, yPoints1, xPoints1.length);
            g.drawPolygon(xPoints2, yPoints2, xPoints2.length);
            g.translate(-x, -y);
            }
         };
      typeIcon[ANCHOR] = new Icon() {
         int[] xPoints = {0,  0, 10, 10};
         int[] yPoints = {0, 10,  0, 10};
         public int getIconHeight() {return 10;}
         public int getIconWidth() {return 10;}
         public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(Color.yellow);
            g.fillPolygon(xPoints, yPoints, xPoints.length);
            g.setColor(Color.green);
            g.drawPolygon(xPoints, yPoints, xPoints.length);
            g.translate(-x, -y);
            }
         };
      typeIcon[MEASURE] = new Icon() {
         int[] xPoints = {0,  0, 5};
         int[] yPoints = {0, 10, 5};
         public int getIconHeight() {return 10;}
         public int getIconWidth() {return 10;}
         public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(Color.blue);
            g.fillPolygon(xPoints, yPoints, xPoints.length);
            g.setColor(Color.yellow);
            g.drawPolygon(xPoints, yPoints, xPoints.length);
            g.translate(-x, -y);
            }
         };
      typeIcon[LENGTH] = new Icon() {
         int[] xPoints = {0, 10};
         int[] yPoints = {5,  5};
         public int getIconHeight() {return 10;}
         public int getIconWidth() {return 10;}
         public void paintIcon(Component c, Graphics g, int x, int y) {
            g.translate(x, y);
            g.setColor(Color.cyan);
            g.drawPolygon(xPoints, yPoints, xPoints.length);
            g.translate(-x, -y);
            }
         };
      }
   
   /**
    * Convert the mark to a string
    * @return the mark value
    */
   public String toString(){
	   return "Mark_"+this.value;
   }
 

   }