/*
 * 
 */
package io.github.rocsg.fijiyama.rsml;

import ij.*;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.RGBStackMerge;
import ij.process.*;
import io.github.rocsg.fijiyama.common.Timer;
import io.github.rocsg.fijiyama.common.VitimageUtils;
import io.github.rocsg.fijiyama.registration.ItkTransform;
import io.github.rocsg.fijiyama.registration.TransformUtils;
import io.github.rocsg.fijiyama.rsml.DataFileFilterRSML;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import java.util.*;
import java.util.List;
import java.io.*;
import java.text.SimpleDateFormat;

// XML file support
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.scijava.vecmath.Point3d;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;  

/** @author Xavier Draye and Guillaume Lobet - Universit� catholique de Louvain */

















//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


class IdenticalDataFileFilter extends javax.swing.filechooser.FileFilter {

	   String rootFName;
	   
	   public IdenticalDataFileFilter (String rootFName) {
		  int l = rootFName.length();
		  int crop = l-(l/10);
	      this.rootFName = rootFName.substring(0, crop);
	   }

	   public boolean accept(File f) {
		   return (f.getName().toLowerCase().endsWith("xml") && f.getName().startsWith(rootFName));
	   }

	   public String getDescription() {
	      return "SmartRoot Datafiles associated with " + rootFName;
	   }
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

class DataFileFilterRSML extends javax.swing.filechooser.FileFilter 
implements java.io.FileFilter {

	public DataFileFilterRSML () { }

	public boolean accept(File f) {
		return f.getName().toLowerCase().endsWith("rsml");
	}

	public String getDescription() {
		return "Root System Markup Language";
	}
}
   

// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

/**
 * The Class RootModel.
 */
public class RootModel extends WindowAdapter{
	  
	   
		//START UNSAFE ZONE
	   
		
		   /**
		    * Root model wild read annotation from rsml.
		    *
		    * @param rsmlFile the rsml file
		    * @return the root model
		    
		   public static RootModel RootModelWildReadAnnotationFromRsml (String rsmlFile) {//Wild read model for Fijiyama did Root model with time, diameter, vx and vy information
				FSR sr= (new FSR());
				sr.initialize();
			   boolean debug=false;
			   String[]str=VitimageUtils.readStringFromFile(rsmlFile).split(System.getProperty("line.separator"));
			   RootModel rm=new RootModel();
			   rm.imgName=str[10].replace("<label>", "").replace("</label>", "");
			   int Nobs=1000000;
			   double[]hours=new double[Nobs];
			   boolean hasHours=true;
			   hasHours=(str[11].contains("observation"));
			   if(hasHours) {
				   String []tab=(str[11].split(">")[1].split("<")[0]).split(",");
				   double[]tabD=new double[tab.length];
			       for(int i=0;i<tab.length;i++)tabD[i]=Double.parseDouble(tab[i]);
			   }
			   rm.hoursCorrespondingToTimePoints=hours;
			   
			   
			   int ind=hasHours?16:15;
			   boolean first;
			   if(debug)System.out.println("Pl"+str[ind]);
			   while(str[ind].contains("<plant")) {
				   ind=ind+1+3;//<root then <point
				   Root rPrim=new Root(null, rm, "",1);
				   first=true;
				   if(debug)System.out.println("Poiprim"+str[ind]);
				   while(str[ind].contains("<point")) {
					   String[]vals=str[ind].replace("<point ","").replace("/>", "").replace("\"", "").split(" ");
					   rPrim.addNode(Double.parseDouble(vals[12].split("=")[1]),Double.parseDouble(vals[13].split("=")[1]),0,0,0,0,first);
					   if(first)first=false;
					   ind++;
					   if(debug)System.out.println("-Poiprim"+str[ind]);
				   }
				   ind+=2;
				   while(str[ind].contains("<point")) {
					   ind++;
				   }
				   rPrim.computeDistances();
				   rm.rootList.add(rPrim);
				   ind=ind+2;//<root or </root
				   if(debug)System.out.println("root lat"+str[ind]);
				   while(str[ind].contains("<root")) {//lateral
					   ind=ind+3;//point
					   Root rLat=new Root(null, rm, "",2);
					   first=true;
					   if(debug)System.out.println("PoiLat"+str[ind]);
					   while(str[ind].contains("<point")) {
						   String[]vals=str[ind].replace("<point ","").replace("/>", "").replace("\"", "").split(" ");
//						   for(int i=0;i<vals.length;i++)System.out.println("Tab["+i+"]="+vals[i]);
						   rLat.addNode(Double.parseDouble(vals[14].split("=")[1]),Double.parseDouble(vals[15].split("=")[1]),0,0,0,0,first);
						   if(first)first=false;
						   ind++;
						   if(debug)System.out.println("-PoiLat?"+str[ind]);
					   }
					   ind+=2;
					   while(str[ind].contains("<point")) {
						   ind++;
					   }
						rLat.computeDistances();
						rPrim.attachChild(rLat);
						rLat.attachParent(rPrim);
						rm.rootList.add(rLat);
					   ind=ind+3;//<root or </root			   
					   if(debug)System.out.println("-root lat?"+str[ind]);
				   }
				   ind=ind+2;//<plant or nothing	
				   if(debug)System.out.println("-plant?"+str[ind]);
			   }
			   System.out.println("End of plant because"+str[ind]);
			   return rm;
		   }

		   
		*/
		
		
	   
			/**
			 * Gets the lateral speeds and depth over time.
			 *
			 * @param tMax the t max
			 * @param plant the plant
			 * @return the lateral speeds and depth over time
			 */
			public double[][][]getLateralSpeedsAndDepthOverTime(int tMax,int plant){
				System.out.println("Working for plant "+plant+" at tmax="+tMax);
				int nbLat=nbLatsPerPlant()[plant];
				double primInitDepth=0;
				for(Root r: rootList){
					if(r.order==1) {
						primInitDepth=r.firstNode.y;
					}
				}
				double[][][]tab=new double[2][nbLat][tMax+1];
				int incr=-1;
				for(Root r: rootList){
					if(r.plantNumber!=plant || r.order==1)continue;
					incr++;
					Node n=r.firstNode;
					double dh=n.child.birthTimeHours - n.birthTimeHours;
					if(dh<=0)dh=VitimageUtils.EPSILON;
					double spe=0;
					while(n!=null) {
						double ti=n.birthTime;
						if(n.child!=null) spe=VitimageUtils.distance(n.x,n.y,n.child.x,n.child.y)/dh;
						tab[0][incr][(int) Math.round(ti)]=-n.y+primInitDepth;
						tab[1][incr][(int) Math.round(ti)]=spe;
						n=n.child;
					}
				}
				return tab;
			}
		
			

		   /**
		    * Write RSML 3 D.
		    *
		    * @param f the f
		    * @param imgExt the img ext
		    * @param shortValues the short values
		    * @param respectStandardRSML the respect standard RSML
		    */
		   public void writeRSML3D(String f,String imgExt,boolean shortValues,boolean respectStandardRSML) {
			   String fileName=VitimageUtils.withoutExtension( new File(f).getName() );
			   nextAutoRootID = 0;
			   org.w3c.dom.Document dom = null;
			   Element re,me,met,mett,sce,plant,rootPrim,rootLat,geomPrim,polyPrim,geomLat,polyLat,pt;

			   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			   try {
				   DocumentBuilder builder = factory.newDocumentBuilder();
				   dom = builder.newDocument();
			   }catch (ParserConfigurationException pce) {logReadError();return; }

			   re=dom.createElement("rsml");

			   //create time list
			   String hours="";
			   for(int i=1;i<hoursCorrespondingToTimePoints.length-1;i++)hours+=VitimageUtils.dou(hoursCorrespondingToTimePoints[i])+",";
			   hours+=VitimageUtils.dou(hoursCorrespondingToTimePoints[hoursCorrespondingToTimePoints.length-1]);
			   
			   //Build and add metadata
			   me=dom.createElement("metadata");
				   met=dom.createElement("version");met.setTextContent("1.4"); me.appendChild(met);	   
				   met=dom.createElement("unit"); met.setTextContent("pixel(um)"); me.appendChild(met);
				   met=dom.createElement("size");  met.setTextContent(""+pixelSize);  me.appendChild(met);
				   met=dom.createElement("last-modified");  met.setTextContent( new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));  me.appendChild(met);
				   met=dom.createElement("software");  met.setTextContent( "RootSystemTracker");  me.appendChild(met);
				   met=dom.createElement("user");  met.setTextContent( "Unknown");  me.appendChild(met);
				   met=dom.createElement("file-key");  met.setTextContent( fileName);  me.appendChild(met);
				   met=dom.createElement("observation-hours");  met.setTextContent( hours);  me.appendChild(met);
				   met=dom.createElement("image"); 
					   mett=dom.createElement("label");  mett.setTextContent( fileName+imgExt);  met.appendChild(mett);
					   mett=dom.createElement("sha256");  mett.setTextContent( "Nothing there");  met.appendChild(mett);	   
				   me.appendChild(met);	  
			   re.appendChild(me);

			   //Build and add scene
			   sce=dom.createElement("scene");

			   		//Build and add plant 1
			   		int incrPlant=0;
			   		for(int indexPrim = 0; indexPrim < rootList.size(); indexPrim++){
			   			Root r =  (Root) rootList.get(indexPrim);
						if(r.isChild==1)continue;
						++incrPlant;
						plant=dom.createElement("plant");plant.setAttribute("ID", ""+(incrPlant));plant.setAttribute("label", "");
							rootPrim=dom.createElement("root");rootPrim.setAttribute("ID", ""+(incrPlant)+".1");rootPrim.setAttribute("label", "");
								geomPrim=dom.createElement("geometry");
									polyPrim=dom.createElement("polyline");
									double[][]coord=getRootCoordinates(r);
									for(int i=0;i<coord.length;i++) {
										pt=dom.createElement("point"); 
										pt.setAttribute(respectStandardRSML ? "x" : "coord_x", ""+(shortValues ? VitimageUtils.dou(coord[i][0]): coord[i][0]));  
										pt.setAttribute(respectStandardRSML ? "y" : "coord_y", ""+(shortValues ? VitimageUtils.dou(coord[i][1]) : coord[i][1]));  
										if(!respectStandardRSML) {
											pt.setAttribute("vx", ""+(shortValues ? VitimageUtils.dou(coord[i][2]) : coord[i][2]));
											pt.setAttribute("vy", ""+(shortValues ? VitimageUtils.dou(coord[i][3]) : coord[i][3]));  
											pt.setAttribute("diameter", ""+(shortValues ? VitimageUtils.dou(coord[i][4]) : coord[i][4]));
											pt.setAttribute("coord_t", ""+(shortValues ? VitimageUtils.dou(coord[i][5]) : coord[i][5]));  
											pt.setAttribute("coord_th", ""+(shortValues ? VitimageUtils.dou(coord[i][6]) : coord[i][6]));  
										}
										polyPrim.appendChild(pt);
									}														
									geomPrim.appendChild(polyPrim);
								rootPrim.appendChild(geomPrim);
								if(respectStandardRSML) {
									Element functionPrim=dom.createElement("functions");
										Element funcPrim=dom.createElement("function");funcPrim.setAttribute("name", "timepoint");funcPrim.setAttribute("domain", "polyline");
										for(int i=0;i<coord.length;i++) {
											pt=dom.createElement("sample");
											pt.setTextContent(""+(shortValues ? VitimageUtils.dou(coord[i][5]) : coord[i][5]));
											funcPrim.appendChild(pt);
										}
										functionPrim.appendChild(funcPrim);
										Element funcPrim2=dom.createElement("function");funcPrim2.setAttribute("name", "hours");funcPrim2.setAttribute("domain", "polyline");
										for(int i=0;i<coord.length;i++) {
											pt=dom.createElement("sample");
											pt.setTextContent(""+(shortValues ? VitimageUtils.dou(coord[i][6]) : coord[i][6]));
											funcPrim2.appendChild(pt);
										}
										functionPrim.appendChild(funcPrim2);
									rootPrim.appendChild(functionPrim);
								}
								
								
								
								
						
								//Ajouter les enfants
								int incrLat=0;
								for(Root rLat : r.childList) {	
									incrLat++;
									rootLat=dom.createElement("root");rootLat.setAttribute("ID", ""+(incrPlant)+".1."+incrLat);rootLat.setAttribute("label", "");
										geomLat=dom.createElement("geometry");
											polyLat=dom.createElement("polyline");
											coord=getRootCoordinates(rLat);
											for(int i=0;i<coord.length;i++) {
												pt=dom.createElement("point");  
												pt.setAttribute(respectStandardRSML ? "x" : "coord_x", ""+(shortValues ? VitimageUtils.dou(coord[i][0]): coord[i][0]));  
												pt.setAttribute(respectStandardRSML ? "y" : "coord_y", ""+(shortValues ? VitimageUtils.dou(coord[i][1]) : coord[i][1]));  
												if(!respectStandardRSML) {
													pt.setAttribute("vx", ""+(shortValues ? VitimageUtils.dou(coord[i][2]) : coord[i][2]));
													pt.setAttribute("vy", ""+(shortValues ? VitimageUtils.dou(coord[i][3]) : coord[i][3]));  
													pt.setAttribute("diameter", ""+(shortValues ? VitimageUtils.dou(coord[i][4]) : coord[i][4]));
													pt.setAttribute("coord_t", ""+(shortValues ? VitimageUtils.dou(coord[i][5]) : coord[i][5]));  
													pt.setAttribute("coord_th", ""+(shortValues ? VitimageUtils.dou(coord[i][6]) : coord[i][6]));  
												}
												polyLat.appendChild(pt);
											}														
											geomLat.appendChild(polyLat);
										rootLat.appendChild(geomLat);
										if(respectStandardRSML) {
											Element functionLat=dom.createElement("functions");
												Element funcLat=dom.createElement("function");funcLat.setAttribute("name", "timepoint");funcLat.setAttribute("domain", "polyline");
												for(int i=0;i<coord.length;i++) {
													pt=dom.createElement("sample");
													pt.setTextContent(""+(shortValues ? VitimageUtils.dou(coord[i][5]) : coord[i][5]));
													funcLat.appendChild(pt);
												}
												functionLat.appendChild(funcLat);
												Element funcLat2=dom.createElement("function");funcLat2.setAttribute("name", "hours");funcLat2.setAttribute("domain", "polyline");
												for(int i=0;i<coord.length;i++) {
													pt=dom.createElement("sample");
													pt.setTextContent(""+(shortValues ? VitimageUtils.dou(coord[i][6]) : coord[i][6]));
													funcLat2.appendChild(pt);
												}
												functionLat.appendChild(funcLat2);
											rootLat.appendChild(functionLat);
										}
									rootPrim.appendChild(rootLat);
								}
							plant.appendChild(rootPrim);
						sce.appendChild(plant);
			   		}							
			   		re.appendChild(sce);
			   dom.appendChild(re);
		   
			   
			   
			   
			   
			   TransformerFactory tf = TransformerFactory.newInstance();
		       Transformer transformer;
		       try {
		           transformer = tf.newTransformer();
		           transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");            
		           transformer.setOutputProperty(OutputKeys.INDENT, "yes");            
		           FileOutputStream outStream = new FileOutputStream(new File(f)); 
		           transformer.transform(new DOMSource(dom), new StreamResult(outStream));
		           outStream.close();
		       } catch (TransformerException eee)  { eee.printStackTrace(); }  catch (Exception ee) { ee.printStackTrace(); }
		   	}

		   
		   /**
		    * Root model wild read from rsml.
		    *
		    * @param rsmlFile the rsml file
		    * @return the root model
		    */
		   public static RootModel RootModelWildReadFromRsml (String rsmlFile) {//Wild read model for Fijiyama did Root model with time, diameter, vx and vy information
			   FSR sr= (new FSR());
			   sr.initialize();
			   boolean debug=false;
			   //String lineSep= System.getProperty("line.separator");

			   int Nobs=100000; //N max of observations
			   
			   String[]str=null;
			   if(VitimageUtils.isWindowsOS())str=VitimageUtils.readStringFromFile(rsmlFile).split("\\n");
			   else str=VitimageUtils.readStringFromFile(rsmlFile).split("\n");
			   RootModel rm=new RootModel();
			   rm.imgName="";
			   rm.pixelSize=(float) Double.parseDouble( str[4].split(">")[1].split("<")[0] );
			   
			   double[]hours=new double[Nobs];
			   boolean hasHours=true;
			   hasHours=(str[9].contains("observation"));
			   double[]tabD=new double[0];
			   if(hasHours) {
				   String []tab=(str[9].split(">")[1].split("<")[0]).split(",");
				   tabD=new double[tab.length+1];
			       for(int i=1;i<tab.length+1;i++)tabD[i]=Double.parseDouble(tab[i-1]);
			   }
			   tabD[0]=tabD[1];
			   rm.hoursCorrespondingToTimePoints=tabD;
			   
			   int ind=hasHours?16:15;
			   boolean first;
			   if(debug)IJ.log("Pl"+str[ind]);
			   while(str[ind].contains("<plant")) {
				   ind=ind+1+3;//<root then <point
				   Root rPrim=new Root(null, rm, "",1);
				   first=true;
				   if(debug)IJ.log("Poiprim"+str[ind]);
				   while(str[ind].contains("<point")) {
					   String[]vals=str[ind].replace("<point ","").replace("/>", "").replace("\"", "").split(" ");
					   if(hasHours)  rPrim.addNode(Double.parseDouble(vals[2].split("=")[1]),Double.parseDouble(vals[3].split("=")[1]),Double.parseDouble(vals[0].split("=")[1]),Double.parseDouble(vals[1].split("=")[1]),Double.parseDouble(vals[4].split("=")[1]),Double.parseDouble(vals[5].split("=")[1]),Double.parseDouble(vals[6].split("=")[1]),first);
					   else           rPrim.addNode(Double.parseDouble(vals[1].split("=")[1]),Double.parseDouble(vals[2].split("=")[1]),Double.parseDouble(vals[0].split("=")[1]),Double.parseDouble(vals[0].split("=")[1]),Double.parseDouble(vals[3].split("=")[1]),Double.parseDouble(vals[4].split("=")[1]),Double.parseDouble(vals[5].split("=")[1]),first);
					   if(first)first=false;
					   ind++;
					   if(debug)IJ.log("-Poiprim"+str[ind]);
				   }
				   rPrim.computeDistances();
				   rm.rootList.add(rPrim);
				   ind=ind+2;//<root or </root
				   if(debug)IJ.log("root lat"+str[ind]);
				   while(str[ind].contains("<root")) {//lateral
					   ind=ind+3;//point
					   Root rLat=new Root(null, rm, "",2);
					   first=true;
					   if(debug)IJ.log("PoiLat"+str[ind]);
					   while(str[ind].contains("<point")) {
						   String[]vals=str[ind].replace("<point ","").replace("/>", "").replace("\"", "").split(" ");
						   if(hasHours)  rLat.addNode(Double.parseDouble(vals[2].split("=")[1]),Double.parseDouble(vals[3].split("=")[1]),Double.parseDouble(vals[0].split("=")[1]),Double.parseDouble(vals[1].split("=")[1]),Double.parseDouble(vals[4].split("=")[1]),Double.parseDouble(vals[5].split("=")[1]),Double.parseDouble(vals[6].split("=")[1]),first);
						   else  rLat.addNode(Double.parseDouble(vals[1].split("=")[1]),Double.parseDouble(vals[2].split("=")[1]),Double.parseDouble(vals[0].split("=")[1]),Double.parseDouble(vals[0].split("=")[1]),Double.parseDouble(vals[3].split("=")[1]),Double.parseDouble(vals[4].split("=")[1]),Double.parseDouble(vals[5].split("=")[1]),first);
						   if(first)first=false;
						   ind++;
						   if(debug)IJ.log("-PoiLat?"+str[ind]);
					   }
						rLat.computeDistances();
						rPrim.attachChild(rLat);
						rLat.attachParent(rPrim);
						rm.rootList.add(rLat);
					   ind=ind+3;//<root or </root			   
					   if(debug)IJ.log("-root lat?"+str[ind]);
				   }
				   ind=ind+2;//<plant or nothing	
				   if(debug)IJ.log("-plant?"+str[ind]);
			   }
			   return rm;
		   }
		   

	
			//END UNSAFE ZONE
		   

	
	
	
	
	
	
/** The version. */
//	static int nextRootKey;
	static String version = "4.1";
	
	/** The datafile key. */
	static String datafileKey = "default";	
	
	
   /** The img. */
   ImagePlus img;
   
   /** The ip. */
   ImageProcessor ip;
   
   /** The angle step. */
   double angleStep;
   
   /** The threshold. */
   float threshold;
   
   /** The d M. */
   float dM=0;
   
   /** The root list. */
   public ArrayList<Root> rootList = new ArrayList<Root>();
   
   /** The next auto root ID. */
   public int nextAutoRootID;

   /**  Return code from selectNode(). */
   static final int NODE = 1;
   
   /** The Constant ROOT. */
   static final int ROOT = 2;
   
   /** The Constant CHILD. */
   static final int CHILD = 3;
   
   /** The Constant PARENT. */
   static final int PARENT = 4;
   
   /** The Constant CHILDPARENT. */
   static final int CHILDPARENT = 5;

   /**  autoBuildFromNode() estimates the putative location for a new node in the direction of the         line joining the previous and current nodes, using a distance which is the minimum of         putative distances 1 & 2 (see AUTOBUILD_STEP_FACTOR_BORDER AUTOBUILD_STEP_FACTOR_DIAMETER)        but which is at least equal to AUTOBUILD_MIN_STEP. */
   /** Putative distance 1 (from the current node) is equal to the
       distance to the root border (along the predicted direction) multiplied by the AUTOBUILD_STEP_FACTOR_BORDER */
   /** Putative distance 2 (from the current node) is equal to the
       root diameter at the current node multiplied by the AUTOBUILD_STEP_FACTOR_DIAMETER */
   /** Minimum angle step for the automatic recentering of nodes in autoBuildFromNode() */
   /** Angle step for the automatic recentering of nodes in autoBuildFromNode(): the angle step
       is equal to AUTOBUILD_THETA_STEP_FACTOR divided by the root diameter */


   float AUTOBUILD_MIN_STEP = 3.0f;
   
   /** The autobuild step factor border. */
   float AUTOBUILD_STEP_FACTOR_BORDER = 0.5f;
   
   /** The autobuild step factor diameter. */
   float AUTOBUILD_STEP_FACTOR_DIAMETER = 2.0f;
   
   /** The autobuild min theta step. */
   float AUTOBUILD_MIN_THETA_STEP = 0.02f;
   
   /** The autobuild theta step factor. */
   float AUTOBUILD_THETA_STEP_FACTOR = (float) Math.PI / 2.0f;



   /**  Modifier flags for tracing operations. */
   static final int AUTO_TRACE = 1;
   
   /** The Constant FREEZE_DIAMETER. */
   static final int FREEZE_DIAMETER = 2;
   
   /** The Constant SNAP_TO_BORDER. */
   static final int SNAP_TO_BORDER = 4;
   
   /** The Constant THRESHOLD_ADAPTIVE1. */
   static final int THRESHOLD_ADAPTIVE1 = 1;
   
   /** The Constant THRESHOLD_ADAPTIVE2. */
   static final int THRESHOLD_ADAPTIVE2 = 2; 
   
   /** The Constant REGULAR_TRACING. */
   static final int REGULAR_TRACING = 1;
   
   /** The Constant LATERAL_TRACING. */
   static final int LATERAL_TRACING = 2;
   
   /** The Constant LINE_TRACING. */
   static final int LINE_TRACING = 4;
   
   /** The Constant LATERAL_TRACING_ONE. */
   static final int LATERAL_TRACING_ONE = 8;

   /** The datafile filter RSML. */
   static private DataFileFilterRSML datafileFilterRSML = new DataFileFilterRSML();

   /** The directory. */
   private String directory;
   
   /** The img name. */
   public String imgName;
   
   /** The previous magnification. */
   public float previousMagnification = 0.0f;
   
   /** The Constant fileSuffix. */
   public static final String[] fileSuffix = {"xml", "xml01", "xml02", "xml03", "xml04"};
   
   /** The Constant fileSuffixRSML. */
   public static final String[] fileSuffixRSML = {"rsml", "rsml01", "rsml02", "rsml03", "rsml04"};

   /** The dpi. */
   private float dpi;
   
   /** The pixel size. */
   public float pixelSize;

/** The n plants. */
private int nPlants;

public double[] hoursCorrespondingToTimePoints;
   
   /**
    *  Constructors.
    */  
   public RootModel() {	 
	   dpi=1;
	   pixelSize = 2.54f / dpi;
   }

   
   /**
    *  Constructors.
    *
    * @param dataFName the data F name
    */  
   public RootModel(String dataFName) {	 
	   dpi = (FSR.prefs!=null ? FSR.prefs.getFloat("DPI_default", dpi) : 1);
	   pixelSize = 2.54f / dpi;
	   readRSML(dataFName);	      	      
   }
   
   /**
    *  Constructors.
    *
    * @param dataFName the data F name
    * @param timeLapseModel the time lapse model
    */  
   public RootModel(String dataFName,boolean timeLapseModel) {	 
	   dpi = (FSR.prefs!=null ? FSR.prefs.getFloat("DPI_default", dpi) : 1);
	   pixelSize = 2.54f / dpi;
	   readRSML(dataFName,timeLapseModel);	      	      
   }
   
  
   /**
    * Computed the scale base on an unit and a resolutiono.
    *
    * @param unit the unit
    * @param resolution the resolution
    * @return the dpi
    */
   public float getDPI(String unit, float resolution){
	   if (unit.startsWith("cm") || unit.startsWith("cen")) {
	          return resolution * 2.54f;
	          }
	      else if (unit.startsWith("mm") || unit.startsWith("mill")) {
	          return (resolution / 10) * 2.54f;
	          }	 
	      else if (unit.startsWith("m") || unit.startsWith("me")) {
	          return (resolution * 100) * 2.54f;
	          }	 	      
	      else if (unit.startsWith("IN") || unit.startsWith("in")) {
	          return resolution;
	          }
	       else {
	    	   return 0.0f;
	          }	      
   }
   
   
   /**
    * Distance between point and segment or ten times vitimage utils large value if the projection does not fall into the segment.
    *
    * @param ptC the pt C
    * @param ptA the pt A
    * @param ptB the pt B
    * @return the ${e.g(1).rsfl()}
    */
   public double distanceBetweenPointAndSegmentOrTenTimesVitimageUtilsLargeValueIfTheProjectionDoesNotFallIntoTheSegment
   		(double[]ptC,double[]ptA,double[]ptB) {
	   double []AB=TransformUtils.vectorialSubstraction(ptB, ptA);
	   double []BA=TransformUtils.vectorialSubstraction(ptA, ptB);
	   double []AC=TransformUtils.vectorialSubstraction(ptC, ptA);
	   double []BC=TransformUtils.vectorialSubstraction(ptC, ptB);
	   double ABscalAC=TransformUtils.scalarProduct(AB,AC);
	   double BAscalBC=TransformUtils.scalarProduct(BA,BC);
	   if(ABscalAC<0 || BAscalBC<0)	  return 10*VitimageUtils.LARGE_VALUE;
	   double[]AD=TransformUtils.proj_u_of_v(AB, AC);
	   double []ptD=TransformUtils.vectorialAddition(ptA, AD);
	   double[]DC=TransformUtils.vectorialSubstraction(ptC, ptD);
	   return TransformUtils.norm(DC);
   }
   
   
   /**
    * Gets the nearest root segment.
    *
    * @param pt the pt
    * @param maxGuessedDistance the max guessed distance
    * @return the nearest root segment
    */
   public Object[]getNearestRootSegment(Point3d pt,double maxGuessedDistance){
	   Node nearestParent=null;
	   Root nearestRoot=null;
	   double distMin=maxGuessedDistance;
	   for(Root r:rootList) {
		   Node n=r.firstNode;
		   while(n!=null && n.child!=null && n.child.birthTime<=pt.z) {
			   double dist=distanceBetweenPointAndSegmentOrTenTimesVitimageUtilsLargeValueIfTheProjectionDoesNotFallIntoTheSegment(
					   new double[] {pt.x,pt.y,0}, new double[] {n.x,n.y,0},new double[] {n.child.x,n.child.y,0});
			   if(dist<distMin) {
				   distMin=dist;
				   nearestParent=n;
				   nearestRoot=r;
			   }
			   n=n.child;
		   }
	   }	   
	   return new Object[] {nearestParent,nearestRoot};
   }
   

   /**
    * Csv send marks.
    *
    * @param pw the pw
    * @param header the header
    * @param name the name
    * @param last the last
    */
   public void csvSendMarks(PrintWriter pw, boolean header, String name, boolean last){
	   
	   if(header)
		   pw.println("image, source, root, root_name, mark_type, position_from_base, diameter, angle, x, y, root_order, root_ontology, value");
	   String stmt;
       for (int i = 0; i < rootList.size(); i++) {
           Root r = (Root) rootList.get(i);
           // Root origin information
           stmt = name + ", ";
           stmt = stmt.concat(imgName + ", ");  // XD 20110629
           stmt = stmt.concat(r.getRootKey() + ", ");
           stmt = stmt.concat(r.getRootID() + ", ");
           stmt = stmt.concat("Origin, ");
           stmt = stmt.concat("0.0, 0.0, 0.0, ");
           stmt = stmt.concat(r.firstNode.x * pixelSize + ", ");
           stmt = stmt.concat(r.firstNode.y * pixelSize + ", ");
           stmt = stmt.concat(r.isChild()+ ", ");
           stmt = stmt.concat(r.getPoAccession()+ ", ");
           stmt = stmt.concat(imgName);
           pw.println(stmt);
           pw.flush();
           // Marks information
           for (int j = 0; j < r.markList.size(); j++) {
              Mark m = (Mark)r.markList.get(j);
              //Point p = r.getLocation(m.lPos * pixelSize);
              stmt = imgName + ", ";
              stmt = stmt.concat((m.isForeign ? m.foreignImgName : imgName) + ", ");  // XD 20110629
              stmt = stmt.concat(r.getRootKey() + ", ");
              stmt = stmt.concat(r.getRootID() + ", ");
              stmt = stmt.concat(Mark.getName(m.type) + ", ");
              stmt = stmt.concat(r.lPosPixelsToCm(m.lPos) + ", ");
              stmt = stmt.concat(m.diameter * pixelSize + ", ");
              stmt = stmt.concat(m.angle + ", ");
//              if (p != null) {
//                 stmt = stmt.concat(p.x * pixelSize + ", ");
//                 stmt = stmt.concat(p.y * pixelSize + ", ");
//              }
//              else {
//                 SR.write("[WARNING] " + Mark.getName(m.type) + " mark '" + m.value + "' on root '"+ r.getRootID() + "' is past the end of root.");
//                 stmt = stmt.concat(" 0.0, 0.0, ");
//              }
              stmt = stmt.concat(r.isChild()+ ", ");
              stmt = stmt.concat(r.getPoAccession()+ ", ");
              if (m.needsTwinPosition()) 
                 stmt = stmt.concat(((m.twinLPos - m.lPos) * pixelSize) + "");
              else stmt = stmt.concat(m.value + "");
              pw.println(stmt);
              pw.flush();
              }
           // Root end information
           stmt = imgName + ", ";
           stmt = stmt.concat(imgName + ", ");  // XD 20110629
           stmt = stmt.concat(r.getRootKey() + ", ");
           stmt = stmt.concat(r.getRootID() + ", ");
           stmt = stmt.concat("Length, ");
           stmt = stmt.concat(r.lPosPixelsToCm(r.getRootLength()) + ", ");
           stmt = stmt.concat("0.0, 0.0, ");
           stmt = stmt.concat(r.lastNode.x * pixelSize + ", ");
           stmt = stmt.concat(r.lastNode.y * pixelSize + ", ");
           stmt = stmt.concat(r.isChild() + ", ");
           stmt = stmt.concat(r.getPoAccession() + ", ");
           stmt = stmt.concat(imgName + "");
           pw.println(stmt);
           if(last) pw.flush();           
           }
       FSR.write("CSV data transfer completed for 'Marks'.");     
   }

   /**
    * Clean wild rsml.
    *
    * @return the int
    */
   public int cleanWildRsml() {//TODO : currently, does not even verify  if primary was there before the lateral !
	   int stamp=0;
	   ArrayList<Root>prim=new ArrayList<Root>();
	   ArrayList<Root>lat=new ArrayList<Root>();
	   for(Root r : rootList) {
		   if(r.childList!=null && r.childList.size()>0) { prim.add(r);stamp+=1000000;}
		   else { lat.add(r);stamp+=1000;}
	   }
	   for(Root rLat : lat) {
		   Node nL=rLat.firstNode;

		   Root bestPrim=null;
		   double distBest=1E10;
		   for(Root rPrim:prim) {
			   Node nP=rPrim.firstNode;
			   while(nP!=null) {
				   double dist=Node.distanceBetween(nP,nL);
				   if(dist<distBest) {
					   distBest=dist;
					   bestPrim=rPrim;
				   }
				   nP=nP.child;
			   }
		   }
		   if(! (bestPrim==rLat.parent) ) {//Proceed to good attachment
			   stamp++;
			   ArrayList<Root>newChi=new ArrayList<Root>();
			   for(Root rOld:rLat.parent.childList)if(rOld!=rLat)newChi.add(rOld);
			   rLat.parent.childList=newChi;
			   bestPrim.attachChild(rLat);
			   rLat.attachParent(bestPrim);
		   }
	   }
	   return stamp;
   }
   
   
   /*
   public static void main(String[]args) {
		FSR sr= (new FSR());
		sr.initialize();
	   boolean testing=true;
	   String ml="1";
	   String boite="00001";
		final String mainDataDir=testing ? "/home/rfernandez/Bureau/A_Test/RSML"
				: "/media/rfernandez/DATA_RO_A/Roots_systems/Data_BPMP/Second_dataset_2021_07/Processing";
		System.out.println(mainDataDir+"/4_RSML/ML"+ml+"_Boite_"+boite+".rsml");
	    RootModel rm=RootModelWildReadFromRsml(mainDataDir+"/4_RSML/ML"+ml+"_Boite_"+boite+".rsml");
	    for(Root r:rm.rootList)System.out.println(r);
	    VitimageUtils.waitFor(5000000);
		
   }
*/
   

   public void setHoursFromPph(double[]hours){
	   hoursCorrespondingToTimePoints=hours;
   }
   
   
   /**
    * The main method.
    *
    * @param args the arguments
    */
   public static void main(String[]args) {
	   FSR sr= (new FSR());
	   sr.initialize();
	  String rsmlInput="/home/rfernandez/Bureau/A_Test/RSML/Wrochy/ML1_Boite_00001.rsml";
	   String rsmlOutput="/home/rfernandez/Bureau/A_Test/RSML/Wrochy/ML1_Boite_00001_V2.rsml";
	   RootModel model = RootModelWildReadFromRsml(rsmlInput);
//			   new RootModel(rsmlInput);
	   model.writeRSML3D(rsmlOutput, "", true,true);
   }   

 	/**
	 * Send the root data to the ResulsTable rt.
	 *
	 * @param rt the rt
	 * @param name the name
	 */
   public void sendRootData(ResultsTable rt, String name){

      for (int i = 0; i < rootList.size(); i++) {
         Root r = (Root) rootList.get(i);
         if (!r.validate()) continue; // corrupted Root instance
         rt.incrementCounter();	
         rt.addValue("image",name);
         rt.addValue("root_name",r.getRootID());
         rt.addValue("root",r.getRootKey());
         rt.addValue("length",r.lPosPixelsToCm(r.getRootLength()));
         rt.addValue("surface",r.getRootSurface());
         rt.addValue("volume",r.getRootVolume());
         rt.addValue("convexhull_area",r.getConvexHullArea());
         rt.addValue("diameter",r.getAVGDiameter());
         rt.addValue("root_order",r.isChild());
         rt.addValue("root_ontology",r.getPoAccession());
         rt.addValue("parent_name",r.getParentName());
         if(r.getParent() != null) rt.addValue("parent",r.getParent().getRootKey());
         else rt.addValue("parent","-1");
         rt.addValue("insertion_position",r.lPosPixelsToCm(r.getDistanceFromBase()));
         rt.addValue("insertion_angle",r.getInsertAngl() * (180 / Math.PI));
         rt.addValue("n_child",r.childList.size());
         rt.addValue("child_density",r.getChildDensity());
         if(r.firstChild != null){
        	 rt.addValue("first_child",r.getFirstChild().getRootKey());
        	 rt.addValue("insertion_first_child",r.lPosPixelsToCm(r.getFirstChild().getDistanceFromBase()));
         }
         else{ 
        	 rt.addValue("first_child","null");
        	 rt.addValue("insertion_first_child","null");	     

         }
         if(r.lastChild != null){
        	 rt.addValue("last_child",r.getLastChild().getRootKey());
        	 rt.addValue("insertion_last_child",r.lPosPixelsToCm(r.getLastChild().getDistanceFromBase()));
         }
         else{ 
        	 rt.addValue("last_child","null");
        	 rt.addValue("insertion_last_child","null");
         }
      }
   }
 
   
	/**
	 * Compute speed vectors.
	 *
	 * @param deltaBefAfter the delta bef after
	 */
	public void computeSpeedVectors(double deltaBefAfter) {
		for(Root r: rootList)r.computeSpeedVectors(deltaBefAfter,deltaBefAfter,false);
	}

   
   /**
    * Send the node data to the Result Table.
    *
    * @param rt the rt
    * @param name the name
    */
   public void sendNodeData(ResultsTable rt, String name){
	   
       for (int i = 0; i < rootList.size(); i++) {
           Root r = (Root) rootList.get(i);
           if (!r.validate()) continue; // corrupted Root instance
           Node n = r.firstNode;
           do {
	  	         rt.incrementCounter();	
	  	         rt.addValue("image",name);
	  	         rt.addValue("root",r.getRootKey());
	  	         rt.addValue("root_name",r.getRootID());
	  	         rt.addValue("x",n.x * pixelSize);
	  	         rt.addValue("y",n.y * pixelSize);
	  	         rt.addValue("theta",n.theta);
	  	         rt.addValue("diameter",n.diameter * pixelSize);
	  	         rt.addValue("distance_from_base",n.cLength * pixelSize);
	  	         rt.addValue("distance_from_apex",(r.getRootLength() - n.cLength ) *pixelSize);
	  	         rt.addValue("root_order",r.isChild());
	  	         rt.addValue("root_ontology",r.getPoAccession());
              } while ((n = n.child) != null);
        }
	 }

   /**
    * Send the image data to the Result Table.
    *
    * @param rt the rt
    * @param name the name
    */
   public void sendImageData(ResultsTable rt, String name){
	   
	   rt.incrementCounter();	
	   rt.addValue("image",name);
	   rt.addValue("tot_root_length",getTotalRLength());
	   //rt.addValue("convexhull_area",getConvexHullArea());
	   // Primary roots
	   rt.addValue("n_primary",getNPRoot());
	   rt.addValue("tot_prim_length",getTotalPRLength());
	   rt.addValue("mean_prim_length",getAvgPRLength());
	   rt.addValue("mean_prim_diameter",getAvgPRDiam());
	   rt.addValue("mean_lat_density",getAvgChildDens());
	   // Secondary roots
	   rt.addValue("n_laterals",getNSRoot());
	   rt.addValue("tot_lat_length",getTotalSRLength());
	   rt.addValue("mean_lat_length",getAvgSRLength());
	   rt.addValue("mean_lat_diameter",getAvgSRDiam());	   
	   rt.addValue("mean_lat_angle",this.getAvgSRInsAng());	   
	 }
   
   /**
    * Get a given root in the root list.
    *
    * @param i the i
    * @return the root
    */
   public Root getRoot (int i) {
      if (i < getNRoot()) return (Root) rootList.get(i);
      else return null;
      }


   /**
    * Get the total number of roots.
    *
    * @return the n root
    */
   public int getNRoot() {
      return rootList.size();
     }
      
 
   /**
    * Ge tthe DPI value for the image.
    *
    * @return the dpi
    */
   public float getDPI() {return dpi; }
   
   /**
    * Set the DPI avlue for the image.
    *
    * @param dpi the new dpi
    */
   public void setDPI(float dpi) {
      this.dpi = dpi;
      pixelSize = (float) (2.54 / dpi);
      for (int i = 0; i < rootList.size(); i++) ((Root) rootList.get(i)).setDPI(dpi);
      }

   /**
    * Remove all the roots from the root list.
    */
   public void clearDatafile() {
      rootList.clear(); 
      }


    /**
     * Log read error.
     */
    private void logReadError() {
      FSR.write("An I/O error occured while attemping to read the datafile.");
      FSR.write("A new empty datafile will be created.");
      FSR.write("Backup versions of the datafile, if any, can be loaded");
      FSR.write("using the File -> Use backup datafile menu item.");
      }

   /**
    * Read RSML.
    *
    * @param f the f
    */
    //TODO : still in use ?
   public void readRSML(String f) {

	   // Choose the datafile
	   String fPath = f;

	   if(f == null){
		   	clearDatafile();
	   		JFileChooser fc = new JFileChooser(new File(directory));
	   		fc.setFileFilter(datafileFilterRSML);
	   		if (fc.showDialog(null, "Select Root System Markup Datafile") == JFileChooser.CANCEL_OPTION) return;	 	
	   		fPath = fc.getSelectedFile().getAbsolutePath();
	   }	   
	
	   nextAutoRootID = 0;
	         
	   
	   org.w3c.dom.Document documentDOM = null;
	   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	   try {
		   DocumentBuilder builder = factory.newDocumentBuilder();
		   documentDOM = builder.parse(new File(fPath) );
	   }
	   catch (SAXException sxe) {
		   logReadError();
		   return;
	   }
	   catch (ParserConfigurationException pce) {
		   logReadError();
		   return;
	   }
	   catch (IOException ioe) {
		   logReadError();
		   return;
	   }

	   documentDOM.normalize();
	      
	   org.w3c.dom.Node nodeDOM = documentDOM.getFirstChild();
	      	   
	   if (!nodeDOM.getNodeName().equals("rsml")) {
		   logReadError();
		   return;
	   }	
	   
	   String origin = "smartroot";
	   // Navigate the whole document
	   nodeDOM = nodeDOM.getFirstChild();
	   while (nodeDOM != null) {
		   
		   String nName = nodeDOM.getNodeName();
		   
		   // Get and process the metadata
		   if(nName.equals("metadata")){
			   org.w3c.dom.Node nodeMeta = nodeDOM.getFirstChild();
			   String unit = "cm";
			   float res = 1.0f;			   
			   while (nodeMeta != null) {		   
				   	String metaName = nodeMeta.getNodeName();				   	
				   	// Get the image resolution
				   	if(metaName.equals("unit")) unit = nodeMeta.getFirstChild().getNodeValue();	         		         
				   	if(metaName.equals("resolution")) res = Float.valueOf(nodeMeta.getFirstChild().getNodeValue());
				   	if(metaName.equals("file-key")) datafileKey = nodeMeta.getFirstChild().getNodeValue();
				   	if(metaName.equals("software")) origin = nodeMeta.getFirstChild().getNodeValue(); 	
					nodeMeta = nodeMeta.getNextSibling();
			   }
			   dpi = getDPI(unit, res);
			   FSR.write("resolution = "+dpi);
			   pixelSize = 2.54f / dpi;
		   }
		         
		   // Get the plant
		   if(nName.equals("scene")){
			   org.w3c.dom.Node nodeScene = nodeDOM.getFirstChild();
			   while (nodeScene != null) {		   
				   	String sceneName = nodeScene.getNodeName();
				   	
				   if(sceneName.equals("plant")){
					   org.w3c.dom.Node nodeRoot = nodeScene.getFirstChild();
					   while (nodeRoot != null) {
						   String rootName = nodeRoot.getNodeName();
				   
						   // Get the Roots
						   if(rootName.equals("root")){
							  new Root(dpi, nodeRoot, true, null, this, origin);
						   }
						   nodeRoot = nodeRoot.getNextSibling();
					   }
				  }
				   nodeScene = nodeScene.getNextSibling();  
			   }	 
		   }
		   nodeDOM = nodeDOM.getNextSibling();  
	   }
	  FSR.write(rootList.size()+" root(s) were created");
	   setDPI(dpi);
   	}
   
   /**
    * Read common datafile structure.
    *
    * @param f the f
    */
   public void readRSMLNew(String f) {

	   // Choose the datafile
	   String fPath = f;

	   if(f == null){
		   	clearDatafile();
	   		JFileChooser fc = new JFileChooser(new File(directory));
	   		fc.setFileFilter(datafileFilterRSML);
	   		if (fc.showDialog(null, "Select Root System Markup Datafile") == JFileChooser.CANCEL_OPTION) return;	 	
	   		fPath = fc.getSelectedFile().getAbsolutePath();
	   }	   
	
	   nextAutoRootID = 0;
	         
	   
	   org.w3c.dom.Document documentDOM = null;
	   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	   try {
		   DocumentBuilder builder = factory.newDocumentBuilder();
		   documentDOM = builder.parse(new File(fPath) );
	   }
	   catch (SAXException sxe) {
		   logReadError();
		   return;
	   }
	   catch (ParserConfigurationException pce) {
		   logReadError();
		   return;
	   }
	   catch (IOException ioe) {
		   logReadError();
		   return;
	   }

	   documentDOM.normalize();
	      
	   org.w3c.dom.Node nodeDOM = documentDOM.getFirstChild();
	      	   
	   if (!nodeDOM.getNodeName().equals("rsml")) {
		   logReadError();
		   return;
	   }	
	   
	   String origin = "smartroot";
	   // Navigate the whole document
	   nodeDOM = nodeDOM.getFirstChild();
	   while (nodeDOM != null) {
		   
		   String nName = nodeDOM.getNodeName();
		   
		   // Get and process the metadata
		   if(nName.equals("metadata")){
			   org.w3c.dom.Node nodeMeta = nodeDOM.getFirstChild();
			   String unit = "cm";
			   float res = 1.0f;			   
			   while (nodeMeta != null) {		   
				   	String metaName = nodeMeta.getNodeName();				   	
				   	// Get the image resolution
				   	if(metaName.equals("unit")) unit = nodeMeta.getFirstChild().getNodeValue();	         		         
				   	if(metaName.equals("resolution")) res = Float.valueOf(nodeMeta.getFirstChild().getNodeValue());
				   	if(metaName.equals("file-key")) datafileKey = nodeMeta.getFirstChild().getNodeValue();
				   	if(metaName.equals("software")) origin = nodeMeta.getFirstChild().getNodeValue(); 	
					nodeMeta = nodeMeta.getNextSibling();
			   }
			   dpi = getDPI(unit, res);
			   //SR.write("resolution = "+dpi);
			   pixelSize = 2.54f / dpi;
		   }
		         
		   // Get the plant
		   if(nName.equals("scene")){
			   org.w3c.dom.Node nodeScene = nodeDOM.getFirstChild();
			   while (nodeScene != null) {		   
				   	String sceneName = nodeScene.getNodeName();
				   	
				   if(sceneName.equals("plant")){
					   org.w3c.dom.Node nodeRoot = nodeScene.getFirstChild();
					   while (nodeRoot != null) {
						   String rootName = nodeRoot.getNodeName();
				   
						   // Get the Roots
						   if(rootName.equals("root")){
							   System.out.println("nodeRoot="+nodeRoot);
							   System.out.println("origin="+origin);
							   System.out.println("dpi="+dpi);
							  new Root(dpi, nodeRoot, true, null, this, origin);
						   }
						   nodeRoot = nodeRoot.getNextSibling();
					   }
				  }
				   nodeScene = nodeScene.getNextSibling();  
			   }	 
		   }
		   nodeDOM = nodeDOM.getNextSibling();  
	   }
	   FSR.write(rootList.size()+" root(s) were created");
	   setDPI(dpi);
   	}

   /**
    * Read RSML.
    *
    * @param f the f
    * @param timeLapseModel the time lapse model
    */
   public void readRSML(String f,boolean timeLapseModel) {

	   // Choose the datafile
	   String fPath = f;

	   if(f == null){
		   	clearDatafile();
	   		JFileChooser fc = new JFileChooser(new File(directory));
	   		fc.setFileFilter(datafileFilterRSML);
	   		if (fc.showDialog(null, "Select Root System Markup Datafile") == JFileChooser.CANCEL_OPTION) return;	 	
	   		fPath = fc.getSelectedFile().getAbsolutePath();
	   }	   
	
	   nextAutoRootID = 0;
	         
	   
	   org.w3c.dom.Document documentDOM = null;
	   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	   try {
		   DocumentBuilder builder = factory.newDocumentBuilder();
		   documentDOM = builder.parse(new File(fPath) );
	   }
	   catch (SAXException sxe) {
		   logReadError();
		   return;
	   }
	   catch (ParserConfigurationException pce) {
		   logReadError();
		   return;
	   }
	   catch (IOException ioe) {
		   logReadError();
		   return;
	   }

	   documentDOM.normalize();
	      
	   org.w3c.dom.Node nodeDOM = documentDOM.getFirstChild();
	      	   
	   if (!nodeDOM.getNodeName().equals("rsml")) {
		   logReadError();
		   return;
	   }	
	   
	   String origin = "smartroot";
	   // Navigate the whole document
	   nodeDOM = nodeDOM.getFirstChild();
	   while (nodeDOM != null) {
		   
		   String nName = nodeDOM.getNodeName();
		   
		   // Get and process the metadata
		   if(nName.equals("metadata")){
			   org.w3c.dom.Node nodeMeta = nodeDOM.getFirstChild();
			   String unit = "cm";
			   float res = 1.0f;			   
			   while (nodeMeta != null) {		   
				   	String metaName = nodeMeta.getNodeName();				   	
				   	// Get the image resolution
				   	if(metaName.equals("unit")) unit = nodeMeta.getFirstChild().getNodeValue();	         		         
				   	if(metaName.equals("resolution")) res = Float.valueOf(nodeMeta.getFirstChild().getNodeValue());
				   	if(metaName.equals("file-key")) datafileKey = nodeMeta.getFirstChild().getNodeValue();
				   	if(metaName.equals("software")) origin = nodeMeta.getFirstChild().getNodeValue(); 	
					nodeMeta = nodeMeta.getNextSibling();
			   }
			   dpi = getDPI(unit, res);
			   //SR.write("resolution = "+dpi);
			   pixelSize = 2.54f / dpi;
		   }
		         
		   // Get the plant
		   if(nName.equals("scene")){
			   org.w3c.dom.Node nodeScene = nodeDOM.getFirstChild();
			   while (nodeScene != null) {		   
				   	String sceneName = nodeScene.getNodeName();
				   	
				   if(sceneName.equals("plant")){
					   org.w3c.dom.Node nodeRoot = nodeScene.getFirstChild();
					   while (nodeRoot != null) {
						   String rootName = nodeRoot.getNodeName();
				   
						   // Get the Roots
						   if(rootName.equals("root")){
							  new Root(dpi, nodeRoot, true, null, this, origin,timeLapseModel);
						   }
						   nodeRoot = nodeRoot.getNextSibling();
					   }
				  }
				   nodeScene = nodeScene.getNextSibling();  
			   }	 
		   }
		   nodeDOM = nodeDOM.getNextSibling();  
	   }
	   FSR.write(rootList.size()+" root(s) were created");
	   setDPI(dpi);
   	}

  
   
   /**
    * Write RSML.
    *
    * @param f the f
    * @param imgExt the img ext
    */
   public void writeRSML(String f,String imgExt) {
	   String fileName=VitimageUtils.withoutExtension( new File(f).getName() );
	  // String fPath = f;	
	   nextAutoRootID = 0;
	   org.w3c.dom.Document dom = null;
	   Element re,me,met,mett,sce,plant,rootPrim,rootLat,geomPrim,polyPrim,geomLat,polyLat,pt;

	   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	   try {
		   DocumentBuilder builder = factory.newDocumentBuilder();
		   dom = builder.newDocument();
	   }catch (ParserConfigurationException pce) {logReadError();return; }

	   re=dom.createElement("rsml");

	   //Build and add metadata
	   me=dom.createElement("metadata");
		   met=dom.createElement("version");met.setTextContent("1"); me.appendChild(met);	   
		   met=dom.createElement("unit"); met.setTextContent("pixel"); me.appendChild(met);
		   met=dom.createElement("resolution");  met.setTextContent("1");  me.appendChild(met);
		   met=dom.createElement("last-modified");  met.setTextContent( new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));  me.appendChild(met);
		   met=dom.createElement("software");  met.setTextContent( "Fijiyama ");  me.appendChild(met);
		   met=dom.createElement("user");  met.setTextContent( "John Doe ");  me.appendChild(met);
		   met=dom.createElement("file-key");  met.setTextContent( fileName);  me.appendChild(met);
		   met=dom.createElement("image"); 
			   mett=dom.createElement("label");  mett.setTextContent( fileName+imgExt);  met.appendChild(mett);
			   mett=dom.createElement("sha256");  mett.setTextContent( "Nothing there");  met.appendChild(mett);	   
		   me.appendChild(met);	  
	   re.appendChild(me);

	   //Build and add scene
	   sce=dom.createElement("scene");

	   		//Build and add plant 1
	   		int incrPlant=0;
	   		for(int indexPrim = 0; indexPrim < rootList.size(); indexPrim++){
	   			Root r =  (Root) rootList.get(indexPrim);
				if(r.isChild==1)continue;
				++incrPlant;
				plant=dom.createElement("plant");plant.setAttribute("ID", ""+(incrPlant));plant.setAttribute("label", "");
					rootPrim=dom.createElement("root");rootPrim.setAttribute("ID", ""+(incrPlant)+".1");rootPrim.setAttribute("label", "");
						geomPrim=dom.createElement("geometry");
							polyPrim=dom.createElement("polyline");
							double[][]coord=getRootCoordinates(r);
							for(int i=0;i<coord.length;i++) {
								pt=dom.createElement("point");  pt.setAttribute("x", ""+(coord[i][0]));  pt.setAttribute("y", ""+(coord[i][1]));  polyPrim.appendChild(pt);
							}														
							geomPrim.appendChild(polyPrim);
						rootPrim.appendChild(geomPrim);
				
						//Ajouter les enfants
						int incrLat=0;
						for(Root rLat : r.childList) {	
							incrLat++;
							rootLat=dom.createElement("root");rootLat.setAttribute("ID", ""+(incrPlant)+".1."+incrLat);rootLat.setAttribute("label", "");
								geomLat=dom.createElement("geometry");
									polyLat=dom.createElement("polyline");
									coord=getRootCoordinates(rLat);
									for(int i=0;i<coord.length;i++) {
										pt=dom.createElement("point");  pt.setAttribute("x", ""+(coord[i][0]));  pt.setAttribute("y", ""+(coord[i][1]));  polyLat.appendChild(pt);
									}														
									geomLat.appendChild(polyLat);
								rootLat.appendChild(geomLat);
							rootPrim.appendChild(rootLat);
						}
					plant.appendChild(rootPrim);
				sce.appendChild(plant);
	   		}							
	   		re.appendChild(sce);
	   dom.appendChild(re);
   
	   
	   
	   
	   
	   TransformerFactory tf = TransformerFactory.newInstance();
       Transformer transformer;
       try {
           transformer = tf.newTransformer();
           transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");            
           transformer.setOutputProperty(OutputKeys.INDENT, "yes");            
           FileOutputStream outStream = new FileOutputStream(new File(f)); 
           transformer.transform(new DOMSource(dom), new StreamResult(outStream));
           outStream.close();
       } catch (TransformerException eee)  { eee.printStackTrace(); }  catch (Exception ee) { ee.printStackTrace(); }
   	}
     

   
   
	/**
	 * Creates the superposition time lapse from path.
	 *
	 * @param imgPath the img path
	 * @param rsmlPath the rsml path
	 * @return the image plus
	 */
	public static ImagePlus createSuperpositionTimeLapseFromPath(String imgPath,String rsmlPath) {
		ImagePlus imgReg=IJ.openImage(imgPath);
		int Nt=imgReg.getStackSize();
		ImagePlus []tabReg=VitimageUtils.stackToSlices(imgReg);
		ImagePlus[]tabRes=new ImagePlus[Nt];
		RootModel rm=RootModel.RootModelWildReadFromRsml(rsmlPath);
		for(int i=0;i<Nt;i++) {
			ImagePlus imgRSML=rm.createGrayScaleImageWithTime(imgReg,1,false,(i+1),true,new boolean[] {true,true,true,false,true},new double[] {2,2});
			imgRSML.setDisplayRange(0, Nt+3);
			tabRes[i]=RGBStackMerge.mergeChannels(new ImagePlus[] {tabReg[i],imgRSML}, true);
			IJ.run(tabRes[i],"RGB Color","");
		}
		ImagePlus res=VitimageUtils.slicesToStack(tabRes);
		return res;
	}

   
   

   
   /**
    * Gets the root coordinates.
    *
    * @param r the r
    * @return the root coordinates
    */
   public double[][]getRootCoordinates(Root r){
	   Node n=r.firstNode;
	   int incr=1;
	   while (n.child != null) {n = n.child;incr++;}
	   double[][]ret=new double[incr][2];
	   incr=0;
	   n=r.firstNode;
	   ret[0]=new double[] {n.x,n.y,n.vx,n.vy,n.diameter,n.birthTime,n.birthTimeHours};
	   while (n.child != null) {
		   n = n.child;
		   incr++;
		   ret[incr]=new double[] {n.x,n.y,n.vx,n.vy,n.diameter,n.birthTime,n.birthTimeHours};
	   }
	   return ret;
   }
   
   /**
    * Ge the directory containing the image.
    *
    * @return the directory
    */
   public String getDirectory () { return directory; }
 

	/**
	 *  Get the closest root from the base of a given root .
	 *
	 * @return the closest root from the apex of the root r
	 */
	
   public void cleanNegativeTh() {
	   for(Root r: rootList) {
		   r.cleanNegativeTh();
	   }
   }
   
	   public int resampleFlyingRoots() {
		   int stamp=0;
		   for(Root r: rootList) {
			   stamp+=r.resampleFlyingPoints(hoursCorrespondingToTimePoints);
		   }
		   return stamp;
	   }
   
    	/**
	     * Gets the closest node.
	     *
	     * @param pt the pt
	     * @return the closest node
	     */
	    public Object[] getClosestNode(Point3d pt) {
   		double x=pt.x;
   		double y=pt.y;
   		double distMin=1E18;
   		Node nodeMin=null;
   		Root rootMin=null;
   		for(Root r: rootList) {
   			Node n=r.firstNode;
   			while(n!=null) {
   				double dist=Math.sqrt( (x-n.x)*(x-n.x)+(y-n.y)*(y-n.y) );
   				if(dist<distMin && n.birthTime<=pt.z) {
   					distMin=dist;
   					rootMin=r;
   					nodeMin=n;
   				}
   				n=n.child;
   			}
   		}
   		return new Object[] {nodeMin,rootMin};
   	}
       	
	       /**
	        * Gets the closest node in primary.
	        *
	        * @param pt the pt
	        * @return the closest node in primary
	        */
	       public Object[] getClosestNodeInPrimary(Point3d pt) {
       		
       		double x=pt.x;
       		double y=pt.y;
       		double distMin=1E18;
       		Node nodeMin=null;
       		Root rootMin=null;
       		for(Root r: rootList) {
       			if(r.childList==null || r.childList.size()==0)continue;
       			Node n=r.firstNode;
       			while(n!=null) {
       				double dist=Math.sqrt( (x-n.x)*(x-n.x)+(y-n.y)*(y-n.y) );
       				if(dist<distMin && n.birthTime<=pt.z) {
       					distMin=dist;
       					rootMin=r;
       					nodeMin=n;
       				}
       				n=n.child;
       			}
       		}
       		return new Object[] {nodeMin,rootMin};
       	}
       
	/**
	 * Gets the closest root.
	 *
	 * @param r the r
	 * @return the closest root
	 */
	public Root getClosestRoot(Root r){
		Node n = r.firstNode;
		int ls = rootList.size();
		if (ls == 1) return null;
		Root rp;
		Root rpFinal = null;
		float dist;
		float distMin = 1000000.0f;
		
		for (int i = 0; i < ls; i++){
			rp =(Root) rootList.get(i);
			if(rp.getRootKey() == r.getRootKey()) continue;
			Node np = rp.firstNode;
			dist = (float) Point2D.distance((double) n.x, (double) n.y, (double) np.x, (double) np.y);
			if (dist < distMin){
				distMin = dist;
				rpFinal = rp;
			}
			while (np.child != null){
				np = np.child;
				dist = (float) Point2D.distance((double) n.x, (double) n.y, (double) np.x, (double) np.y);
				if (dist < distMin){
					distMin = dist;
					rpFinal = rp;
				}
			}
		}
		return rpFinal;
	}
	
	/**
	 * Attach c to p.
	 *
	 * @param p the parent root
	 * @param c the child root
	 */
	public void setParent(Root p, Root c){
		c.attachParent(p);
		p.attachChild(c);
	}
	
	/**
	 * Get the number of primary roots.
	 *
	 * @return the number of primary roots
	 */
	public int getNPRoot(){
		int n = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() == 0) n++;
		}
		return n;
	}
	
	/**
	 * Gets the NP root.
	 *
	 * @param t the t
	 * @return the NP root
	 */
	public int getNPRoot(double t){
		int n = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() == 0 && r.firstNode.birthTime<=t) n++;
		}
		return n;
	}
	
	/**
	 * Get the number of secondary roots.
	 *
	 * @return the number of secondary roots
	 */
	public int getNSRoot(){
		int n = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() !=0) n++;
		}
		return n;
	}
	
	/**
	 * Gets the NS root.
	 *
	 * @param t the t
	 * @return the NS root
	 */
	public int getNSRoot(double t){
		int n = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() !=0 && r.firstNode.birthTime<=t) n++;
		}
		return n;
	}
	
	/**
	 * Get the average length of secondary roots.
	 *
	 * @return the average length of secondary roots
	 */
	public float getAvgSRLength(){
		float n = 0;
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() !=0) {
				n += r.getRootLength();
				m++;
			}
		}
		return n/m * pixelSize;
	}
	
	/**
	 * Get average length of primary roots.
	 *
	 * @return the average length of primary roots
	 */
	public float getAvgPRLength(){
		float n = 0;
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() == 0 ) {
				n += r.getRootLength();
				m++;
			}
		}
		return n/m * pixelSize;
	}
	
	/**
	 * Get the total root length.
	 *
	 * @return the totla root length
	 */
	public float getTotalRLength(){
		float n = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			n += r.getRootLength();
		}
		return n * pixelSize;		
		
	}
	
	/**
	 * Get the average length of all roots.
	 *
	 * @return the average length of all roots
	 */
	public float getAvgRLength(){
		float n = 0;
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			n += r.getRootLength();
			m++;
		}
		return n/m * pixelSize;
	}
	
	/**
	 * Get the average diameter of secondary roots.
	 *
	 * @return the average diameter of secondary roots
	 */
	public float getAvgSRDiam(){
		float n = 0;
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() != 0) {
				Node node = r.firstNode;
				n += node.diameter;
				m++;
				while (node.child != null){
					node = node.child;
					n += node.diameter;
					m++;
				}
			}
		}
		return n/m * pixelSize;
	}
	
	
	/**
	 * Get the average insertion angle of secondary roots.
	 *
	 * @return the average insertion angle of secondary roots
	 */
	public float getAvgSRInsAng(){
		float n = 0;
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() !=0 ) {
				n += r.getInsertAngl() * (180 / Math.PI);
				m++;
			}
		}
		return n/m ;
	}
	
	
	/**
	 * Get the total length of the primary roots.
	 *
	 * @return the total length of the primary roots
	 */
	public float getTotalPRLength(){
		int l = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if(r.isChild() == 0){
				l += r.lPosPixelsToCm(r.getRootLength());
			}
		}
		return l;
	}
	
	/**
	 * Gets the total PR length.
	 *
	 * @param t the t
	 * @return the total PR length
	 */
	public float getTotalPRLength(double t){
		int l = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if(r.isChild() == 0){
				l += r.computeRootLength(t);
			}
		}
		return l;
	}
	

	
	
	/**
	 * Sets the plant numbers.
	 */
	public void setPlantNumbers() {
		ArrayList<Root>listTemp=new ArrayList<Root>();
		//Set stamp to primaries
		for(Root r : rootList) if(r.isChild() <= 0 )listTemp.add(r);
		Collections.sort(listTemp);
		for(int i=0;i<listTemp.size();i++)listTemp.get(i).plantNumber=i;
		this.nPlants=listTemp.size();

		//Set stamp to laterals
		for(Root r : rootList) if(r.isChild() > 0 ) r.plantNumber=r.parent.plantNumber;
	}
	
	/**
	 * Gets the PR length.
	 *
	 * @param t the t
	 * @return the PR length
	 */
	public float getPRLength(double t){
		int l = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			System.out.println("New : "+r);
			System.out.println("Got :");
			System.out.println(r.parentKey);
			System.out.println(r.rootID);
		}
		return l;
	}

	
	/**
	 * Gets the lengths.
	 *
	 * @param t the t
	 * @return the lengths
	 */
	public double[][] getLengths(double t){
		if(this.nPlants==0)return null;
		double[][]lengs=new double[this.nPlants][2];
		for(Root r: rootList){
			int index=r.plantNumber;
			lengs[index][r.order-1]+=r.computeRootLength(t);			
		}
		return lengs;
	}


	/**
	 * Gets the number and lenght over time serie.
	 *
	 * @param tMax the t max
	 * @return the number and lenght over time serie
	 */
	//Compute a series of statistics over root system present in image [plant number][time][stat : 0=LenPrim, 1=Nlat, 2=Llat]
	public double[][][] getNumberAndLenghtOverTimeSerie(int tMax){
		if(this.nPlants==0)return null;
		double[][][]lengs=new double[this.nPlants][tMax][3];
		for(int t=1;t<=tMax;t++) {
			for(Root r: rootList){
				int index=r.plantNumber;
				if(r.order==1)lengs[index][t-1][0]+=r.computeRootLength(t);
				else {
					lengs[index][t-1][2]+=r.computeRootLength(t);
					lengs[index][t-1][1]++;					
				}
			}
		}
		return lengs;
	}

	
	/**
	 * Nb lats per plant.
	 *
	 * @return the int[]
	 */
	public int[]nbLatsPerPlant(){
		int[]count=new int[5];
		for(Root r: rootList){
			if(r.order==2)count[r.plantNumber]++;
		}
		return count;
	}
	
	
	
	/**
	 * Project rsml on image.
	 *
	 * @param rm the rm
	 * @param registeredStack the registered stack
	 * @param zoomFactor the zoom factor
	 * @param primaryRadius the primary radius
	 * @param secondaryRadius the secondary radius
	 * @param isOldRsmlVersion the is old rsml version
	 * @param binaryColor the binary color
	 * @param projectOnStack the project on stack
	 * @return the image plus
	 */
	public static ImagePlus projectRsmlOnImage(RootModel rm,ImagePlus registeredStack,int zoomFactor,
			int primaryRadius,int secondaryRadius,boolean isOldRsmlVersion,boolean binaryColor,boolean projectOnStack) {
		if(!isOldRsmlVersion) {
			IJ.showMessage("Not yet coded");
			System.exit(0);
		}
		Timer t=new Timer();
		int Nt=registeredStack.getStackSize();
		ImagePlus []tabRes=new ImagePlus[Nt];
		ImagePlus []tabReg=VitimageUtils.stackToSlices(registeredStack);
		for(int i=0;i<Nt;i++) {
			ImagePlus imgRSML=rm.createGrayScaleImageWithTime(
					registeredStack,zoomFactor,binaryColor,(i+1),true,
					new boolean[] {true,true,true,false,true},new double[] {primaryRadius,secondaryRadius});
			imgRSML.setDisplayRange(0, binaryColor ? 255 : (Nt+3));
			tabRes[i]=RGBStackMerge.mergeChannels(new ImagePlus[] {projectOnStack ? tabReg[i] : VitimageUtils.nullImage(tabReg[i]),imgRSML}, true);
			IJ.run(tabRes[i],"RGB Color","");
		}
		t.print("Updating root model took : ");
		ImagePlus res=VitimageUtils.slicesToStack(tabRes);
		return res;
	}
 	

	
	
	/**
	 * Gets the prim depth over time.
	 *
	 * @param tMax the t max
	 * @param plant the plant
	 * @return the prim depth over time
	 */
	public double[]getPrimDepthOverTime(int tMax,int plant){	
		double []ret=new double[tMax];
		for(int i=0;i<ret.length;i++)ret[i]=-100000000;
		for(Root r: rootList){
			if(r.plantNumber!=plant || r.order!=1)continue;
			Node n=r.firstNode;
			float y0=n.y;
			int curTime=0;
			while(n.child!=null) {
				n=n.child;
				curTime=(int) Math.floor(n.birthTime)-1;
				if(curTime<0)curTime=0;
				ret[curTime]=n.y-y0;
			}
			for(int i=0;i<ret.length;i++) {
				if(ret[i]<=-1000000) {
					double lastVit=-10;
					int j=i;
					while(lastVit<0 && (--j)>=0) {
						if(ret[j]>-1000000){
							lastVit=ret[j];
						}
					}
					j=i;
					while(lastVit<0 && (++j)<ret.length) {
						if(ret[j]>-1000000){
							lastVit=ret[j];
						}
					}
					ret[i]=lastVit;
				}
			}	
		}
		return ret;		
	}

	
	/**
	 * Gets the prim length over time.
	 *
	 * @param tMax the t max
	 * @param plant the plant
	 * @return the ${e.g(1).rsfl()}
	 */
	public double[][]getPrimLengthOverTime(int tMax,int plant){	
		double [][]ret=new double[tMax][2];
		for(Root r: rootList){
			if(r.plantNumber!=plant || r.order!=1)continue;
			Node n=r.firstNode;
			float dist=0;
			int curTime=0;
			Node ntmp;
			while(n.child!=null) {
				ntmp=n;
				n=n.child;
				curTime=(int) Math.floor(n.birthTime)-1;
				if(curTime<0)curTime=0;
				dist+=Node.distanceBetween(ntmp, n);
				ret[curTime][0]=dist;
			}
			if(curTime<tMax-1)for(int  t=Math.max(curTime,1);t<tMax;t++)ret[t][0]=ret[t-1][0];
			
		}
		for(int j=1;j<ret.length;j++)ret[j][1]=ret[j][0]-ret[j-1][0];
		return ret;		
	}

	/**
	 * Get the total lengthd of the lateral roots.
	 *
	 * @return the total lenght of the lateral roots
	 */
	public float getTotalSRLength(){
		int l = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if(r.isChild() > 0){
				l += r.lPosPixelsToCm(r.getRootLength());
			}
		}
		return l;
	}

	/**
	 * Gets the total SR length.
	 *
	 * @param t the t
	 * @return the total SR length
	 */
	public float getTotalSRLength(double t){
		int l = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if(r.isChild() > 0){
				l += r.computeRootLength(t);
			}
		}
		return l;
	}

	
	/**
	 * Get the average diameter of primary roots.
	 *
	 * @return the average diameter of primary roots
	 */
	public float getAvgPRDiam(){
		float n = 0;
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() == 0) {
				Node node = r.firstNode;
				n += node.diameter;
				m++;
				while (node.child != null){
					node = node.child;
					n += node.diameter;
					m++;
				}
			}
		}
		return n / m * pixelSize;
	}
	
	/**
	 * Get average interbranch distance.
	 *
	 * @return the average interbranch distance
	 */
	public float getAvgInterBranch(){
		float iB = 0;
		Root r;
		int n = 0;
		for(int i = 0 ; i < rootList.size() ; i++){
			r = (Root) rootList.get(i);
			if(r.getInterBranch() != 0){
				iB += r.getInterBranch();
				n++;
			}
		}
		return (iB / n) * pixelSize;
	}

	/**
	 * Get the number of nodes of the primary roots.
	 *
	 * @return the number of nodes of the primary roots
	 */
	public int getNPNode(){
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() == 0) {
				Node node = r.firstNode;
				m++;
				while (node.child != null){
					node = node.child;
					m++;
				}
			}
		}
		return m;
	}
	
	/**
	 * Get the number of node of the secondary roots.
	 *
	 * @return the number of node of the secondary roots
	 */
	
	public int getNSNode(){
		int m = 0;
		Root r;
		for(int i =0 ; i < rootList.size() ; i++){
			r =  (Root) rootList.get(i);
			if (r.isChild() != 0) {
				Node node = r.firstNode;
				m++;
				while (node.child != null){
					node = node.child;
					m++;
				}
			}
		}
		return m;
	}
	
	
	/**
	 * Get a list of strings containing all the name of roots having children.
	 *
	 * @return a list of strings containing all the name of roots having children
	 */
	public String[] getParentRootNameList(){
		int ind = 0;
		int c = 0;
		Root r;
		for (int i = 0 ; i < rootList.size(); i++){
			r = (Root) rootList.get(i);
			if(r.childList.size() != 0){
				ind ++;
			}
		}	
		String[] n = new String[ind];
		for (int i = 0 ; i < rootList.size(); i++){
			r = (Root) rootList.get(i);
			if(r.childList.size() != 0){
				n[i-c] = r.getRootID();
			}
			else c++;
		}	
		return n;
	}
	
	
	/**
	 * Get a list of strings containing all the name of primary.
	 *
	 * @return a list of strings containing all the name of primary
	 */
	public String[] getPrimaryRootNameList(){
		int ind = 0;
		int c = 0;
		Root r;
		for (int i = 0 ; i < rootList.size(); i++){
			r = (Root) rootList.get(i);
			if(r.isChild() == 0){
				ind ++;
			}
		}	
		String[] n = new String[ind];
		for (int i = 0 ; i < rootList.size(); i++){
			r = (Root) rootList.get(i);
			if(r.isChild() == 0){
				n[i-c] = r.getRootID();
			}
			else c++;
		}	
		return n;
	}
	
	
	/**
	 * Gets the primary roots.
	 *
	 * @return the primary roots
	 */
	public Root[]getPrimaryRoots(){
		int ind = 0;
		int incr=0;
		Root r;
		for (int i = 0 ; i < rootList.size(); i++){
			r = (Root) rootList.get(i);
			if(r.isChild() == 0){
				ind ++;
			}
		}	
		Root[] n = new Root[ind];
		for (int i = 0 ; i < rootList.size(); i++){
			r = (Root) rootList.get(i);
			if(r.isChild() == 0){
				n[incr++] = r;
			}
		}	
		return n;
		
	}
	
	/**
	 * Get the average child density of all the parent roots of the image.
	 *
	 * @return the average child density of all the parent roots of the image
	 */
	public float getAvgChildDens(){
		float cd = 0;
		int n = 0;
		Root r;
		for (int i = 0 ; i < rootList.size(); i++){
			r = (Root) rootList.get(i);
			if(r.getChildDensity() != 0){
				cd += r.getChildDensity();
				n++;
			}
		}
		return cd / n;
	}
	
	/**
	 * Return the image name.
	 *
	 * @return the string
	 */
    public String toString(){
    	return this.imgName;
    }


    /**
     * Get the center of the tracing.
     *
     * @return the center
     */
    public float[] getCenter(){
    	float[] coord = new float[2];
    	
    	// Get x
    	float min = 1e5f, max = 0; 
    	Root r;
    	Node n;
    	for(int i = 0; i < rootList.size(); i++){
    		r = rootList.get(i);
			n = r.firstNode;
			while(n.child != null){
				if(n.x < min) min = n.x;
				if(n.x > max) max = n.x;
				n = n.child;
			}
    	}
    	coord[0] = min + ((max-min)/2);
    	
    	
    	// Get y
    	min = 1e5f;
    	max = 0; 
    	for(int i = 0; i < rootList.size(); i++){
    		r = rootList.get(i);
			n = r.firstNode;
			while(n.child != null){
				if(n.y < min) min = n.y;
				if(n.y > max) max = n.y;
				n = n.child;
			}
    	}
    	coord[1] = min + ((max-min)/2);

    	
    	
    	return coord;
    }


    /**
     * Get the widht of the tracing.
     *
     * @return the min Y
     */
    public int getMinY(){
    	float min = 1e5f; 
    	Root r;
    	Node n;
    	for(int i = 0; i < rootList.size(); i++){
    		r = rootList.get(i);
			n = r.firstNode;
			while(n.child != null){
				if(n.y < min) min = n.y;
				n = n.child;
			}
    	}
    	return (int) min;
    }

    /**
     * Get the widht of the tracing.
     *
     * @return the min X
     */
    public int getMinX(){
    	float min = 1e5f; 
    	Root r;
    	Node n;
    	for(int i = 0; i < rootList.size(); i++){
    		r = rootList.get(i);
			n = r.firstNode;
			while(n.child != null){
				if(n.x < min) min = n.x;
				n = n.child;
			}
    	}
    	return (int) min;
    }    
    
    /**
     * Get the widht of the tracing.
     *
     * @param add the add
     * @return the width
     */
    public int getWidth(boolean add){
    	float min = 1e5f, max = 0; 
    	Root r;
    	Node n;
    	for(int i = 0; i < rootList.size(); i++){
    		r = rootList.get(i);
			n = r.firstNode;
			while(n.child != null){
				if(n.x < min) min = n.x;
				if(n.x > max) max = n.x;
				n = n.child;
			}
    	}
    	if(add) return (int)(max+min);
    	else return (int)(max-min);
    }
    
    /**
     * Get the height of the tracing.
     *
     * @param add the add
     * @return the height
     */
    public int getHeight(boolean add){
    	float min = 1e5f, max = 0; 
    	Root r;
    	Node n;
    	for(int i = 0; i < rootList.size(); i++){
    		r = rootList.get(i);
			n = r.firstNode;
			while(n.child != null){
				if(n.y < min) min = n.y;
				if(n.y > max) max = n.y;
				n = n.child;
			}
    	}
    	if(add) return (int)(max+min);
    	else return (int)(max-min);    
    }


   
    /**
     * Refine description.
     *
     * @param maxRangeBetweenNodes the max range between nodes
     */
    public void refineDescription(int maxRangeBetweenNodes) {
       	Root r;
    	Node n,n1,n0;
    	for(int i = 0; i < rootList.size(); i++){
			r =  (Root) rootList.get(i);
			n = r.firstNode;
			n0= r.firstNode;
			while(n.child != null){
				n1 = n;
				n = n.child;
				if(VitimageUtils.distance(n.x, n.y, n1.x,n1.y)>maxRangeBetweenNodes) {
					Node nPlus=new Node((float)(n.x*0.5+n1.x*0.5),(float)(n.y*0.5+n1.y*0.5), n1, true);
					nPlus.child=n;
					n=n0;
				}				
			}
    	}   
    	
    }
    
    /**
     * Distance.
     *
     * @param n1 the n 1
     * @param n2 the n 2
     * @return the double
     */
    public double distance(Node n1,Node n2) {
    	return VitimageUtils.distance(n1.x, n1.y, n2.x, n2.y);
    }
    
    /**
     * Attach lat to prime.
     */
    public void attachLatToPrime() {
    	Root r,rPar;
    	Node nPrim,nLat,newNode=null;
    	for(int i = 0; i < rootList.size(); i++){
			r =  (Root) rootList.get(i);
			if(r.isChild()==0)continue;
			rPar=r.parent;
			nLat = r.firstNode;
			nPrim=rPar.firstNode;

			double distMin=100000;
			while(nPrim.child!=null) {
				nPrim=nPrim.child;
				if(distance(nPrim,nLat)<distMin) {
					distMin=distance(nPrim,nLat);
					newNode=new Node(nPrim.x,nPrim.y,nLat.diameter,nLat,false);
				}
			}
			r.firstNode=newNode;
		}
    }

    
    
    
    /**
     * Apply transform to geometry.
     *
     * @param tr the tr
     */
    public void applyTransformToGeometry(ItkTransform tr) {
    	Root r;
    	Node n,n1;
    	double[]coords;
    	for(int i = 0; i < rootList.size(); i++){
			r =  (Root) rootList.get(i);
			n = r.firstNode;
			if(r.isChild==1 ) {
				coords=tr.transformPoint(new double[] {n.x,n.y,0});
				n.x+=(n.x-(float) coords[0]);
				n.y+=(n.y-(float) coords[1]);
			}
			while(n.child != null){
				n1 = n;
				n = n.child;
				coords=tr.transformPoint(new double[] {n.x,n.y,0});
				n.x+=(n.x-(float) coords[0]);
				n.y+=(n.y-(float) coords[1]);
			}
    	}
    }
    
    
	/**
	 *  Adapted from ImageProcessor in IJ v1.53i
	 *
	 * @param img the img
	 * @param x1 the x 1
	 * @param y1 the y 1
	 * @param x2 the x 2
	 * @param y2 the y 2
	 * @param drawOneEveryX the draw one every X
	 * @param lineWidth the line width
	 * @param valueToSet the value to set
	 */
	public static void drawDotline(ImagePlus img,int x1, int y1,int x2, int y2,int drawOneEveryX,double lineWidth,double valueToSet) {
		ImageProcessor ip=img.getProcessor();
		int type=img.getType();
		drawDotline(ip, type, x1, y1, x2, y2, drawOneEveryX, lineWidth, valueToSet);
	}
		
	/**
	 *  Adapted from ImageProcessor in IJ v1.53i
	 *
	 * @param ip the ip
	 * @param type the type
	 * @param x1 the x 1
	 * @param y1 the y 1
	 * @param x2 the x 2
	 * @param y2 the y 2
	 * @param drawOneEveryX the draw one every X
	 * @param lineWidth the line width
	 * @param valueToSet the value to set
	 */
	public static void drawDotline(ImageProcessor ip,int type,int x1, int y1,int x2, int y2,int drawOneEveryX,double lineWidth,double valueToSet) {
		double val=Math.max(valueToSet,1);
		int valueI=(int)Math.round(val);
		int valueF=Float.floatToIntBits((float)val);
		int dx = x2-x1;
		int dy = y2-y1;
		int absdx = dx>=0?dx:-dx;
		int absdy = dy>=0?dy:-dy;
		int n = absdy>absdx?absdy:absdx;
		double xinc = dx!=0 ? (double)dx/n : 0; //single point (dx=dy=n=0): avoid division by zero
		double yinc = dy!=0 ? (double)dy/n : 0;
		double x = x1;
		double y = y1;
		double rCar=(lineWidth/2)*(lineWidth/2);
		for (int i=0; i<=n; i++) {
			if (lineWidth<=1) {
				if((drawOneEveryX>0) && ((i)%drawOneEveryX)!=0) {}
				else  ip.putPixel((int)Math.round(x), (int)Math.round(y),(type==ImagePlus.GRAY8 ? valueI : valueF));
			}
			else {
				if((drawOneEveryX>0) && ((i)%(drawOneEveryX*lineWidth)!=0)) {			}
				else {
					for(int ddx=(int) -lineWidth;ddx<=lineWidth;ddx++)for(int ddy=(int) -lineWidth;ddy<=lineWidth;ddy++)if((ddx*ddx+ddy*ddy)<=(rCar)) {
						ip.putPixel((int)Math.round(x+ddx), (int)Math.round(y+ddy),(type==ImagePlus.GRAY8 ? valueI : valueF));					
					}					
				}
			}
			x += xinc;
			y += yinc;
		}
	}

	

	/**
	 * View rsml and image sequence superposition.
	 *
	 * @param rm the rm
	 * @param imageSequence the image sequence
	 * @param zoomFactor the zoom factor
	 * @return the insert angl
	 */
	static ImagePlus viewRsmlAndImageSequenceSuperposition(RootModel rm,ImagePlus imageSequence,int zoomFactor) {
		int Nt=imageSequence.getStackSize();
		ImagePlus []tabRes=new ImagePlus[Nt];
		ImagePlus []tabReg=VitimageUtils.stackToSlices(imageSequence);
		for(int i=0;i<Nt;i++) {
			ImagePlus imgRSML=rm.createGrayScaleImageWithTime(imageSequence,zoomFactor,false,(i+1),true,new boolean[] {true,true,true,false,true},new double[] {2,2});
			imgRSML.setDisplayRange(0, Nt+3);
			tabRes[i]=RGBStackMerge.mergeChannels(new ImagePlus[] {tabReg[i],imgRSML}, true);
			IJ.run(tabRes[i],"RGB Color","");
		}
		ImagePlus res=VitimageUtils.slicesToStack(tabRes);
		return res;
	}

    
    /**
     * Creates the gray scale image with time.
     *
     * @param imgInitSize the img init size
     * @param SIZE_FACTOR the size factor
     * @param binaryColor the binary color
     * @param observationTime the observation time
     * @param dotLineForHidden the dot line for hidden
     * @param symbolOptions the symbol options
     * @param lineWidths the line widths
     * @return the image plus
     */
    public ImagePlus createGrayScaleImageWithTime(ImagePlus imgInitSize, int SIZE_FACTOR,boolean binaryColor,double observationTime,boolean dotLineForHidden,boolean []symbolOptions,double[]lineWidths) {
    	int[]initDims=new int[] {imgInitSize.getWidth(),imgInitSize.getHeight()};
    	return createGrayScaleImageWithTime(initDims, SIZE_FACTOR, binaryColor, observationTime, dotLineForHidden, symbolOptions,lineWidths);
    }
 
    
    
    /**
     * Creates the gray scale image with time.
     *
     * @param initDims the init dims
     * @param SIZE_FACTOR the size factor
     * @param binaryColor the binary color
     * @param observationTime the observation time
     * @param dotLineForHidden the dot line for hidden
     * @param symbolOptions the symbol options
     * @param lineWidths the line widths
     * @return the image plus
     */
    public ImagePlus createGrayScaleImageWithTime(int []initDims, int SIZE_FACTOR,boolean binaryColor,double observationTime,boolean dotLineForHidden,boolean []symbolOptions,double[]lineWidths) {
    	boolean showSymbols=symbolOptions[0];
    	boolean distinguishStartSymbol=symbolOptions[1];
    	boolean distinguishDateStartSymbol=symbolOptions[3];
    	boolean showIntermediateSymbol=symbolOptions[4];
    	if(observationTime<0)observationTime=1E8;//Full root system
    	int w=initDims[0]*SIZE_FACTOR;
 	    int h=initDims[1]*SIZE_FACTOR;
 	    double maxDate=0;
 	    ImagePlus imgRSML=new ImagePlus("",new ByteProcessor(w, h));  
		ImageProcessor ip=imgRSML.getProcessor();

		//draw lines and dot lines
		int nTot=0;
		int nPrim=0; 
		int nSecond=0;
		for(int i = 0; i < rootList.size(); i++){
			Root r =  (Root) rootList.get(i);
			Node n = r.firstNode;
			Node n1;
			int color=(r.order==1 ? 127 : 255);
			if(r.order==1)nPrim++;
			if(r.order==2)nSecond++;
			nTot++;
			double rMaxDate=r.getDateMax();
			if(maxDate<rMaxDate)maxDate=rMaxDate;
			boolean timeOver=false;
			while(n.child != null && (!timeOver) && (n.birthTime<observationTime)){
				n1 = n;
				n = n.child;
				int dotEvery=0;
				double width=0;
				if(lineWidths[r.order-1]<=1) {
					dotEvery=n1.hiddenWayToChild ? 2 : 0;
					width=1;
				}
				else {
					dotEvery=n1.hiddenWayToChild ? 2 : 0;
					width=n1.hiddenWayToChild ? 1 : lineWidths[r.order-1];
				}
				if(n.birthTime>observationTime) {
					double ratio=(observationTime-n1.birthTime)/(n.birthTime-n1.birthTime);
					double partialX=(n.x-n1.x)*ratio+n1.x;
					double partialY=(n.y-n1.y)*ratio+n1.y;
					drawDotline(ip,ImagePlus.GRAY8,(int) ((n1.x+0.5)*SIZE_FACTOR), (int) ((n1.y+0.5)*SIZE_FACTOR), (int) ((partialX+0.5)*SIZE_FACTOR), (int) ((partialY+0.5)*SIZE_FACTOR),dotEvery,width,binaryColor ? color : n.birthTime); 
					timeOver=true;
				}
				else {
					drawDotline(ip,ImagePlus.GRAY8,(int) ((n1.x+0.5)*SIZE_FACTOR), (int) ((n1.y+0.5)*SIZE_FACTOR), (int) ((n.x+0.5)*SIZE_FACTOR), (int) ((n.y+0.5)*SIZE_FACTOR),dotEvery,width,binaryColor ? color: n.birthTime); 
				}
			}
		}
		for(int i = 0; i < rootList.size(); i++){
			if(!showSymbols)continue;
			Root r =  (Root) rootList.get(i);
			Node n = r.firstNode;
			if(n.birthTime>observationTime) continue;
			Node n1;
			double wid=lineWidths[r.order-1];
			if(distinguishStartSymbol) {
				//draw starting point as start symbol
				ip.setColor(Color.white);
				ip.drawRect((int) ((n.x+0.5)*SIZE_FACTOR-2), (int) ((n.y+0.5)*SIZE_FACTOR)-2, 5, 5);
				ip.setColor(Color.black);
				ip.drawRect((int) ((n.x+0.5)*SIZE_FACTOR-1), (int) ((n.y+0.5)*SIZE_FACTOR)-1, 3, 3);
			}
			else if(distinguishDateStartSymbol){
				//draw starting point as date start
				double[]vectOrient=TransformUtils.normalize(new double[] {n.vx,n.vy});
				if(Math.abs(vectOrient[0])>Math.abs(vectOrient[1]))vectOrient=TransformUtils.multiplyVector(vectOrient, 1/Math.abs(vectOrient[0]));
				else vectOrient=TransformUtils.multiplyVector(vectOrient, 1/Math.abs(vectOrient[1]));
				ip.setColor(Color.white);
				ip.setLineWidth((int) wid);
				ip.drawLine((int) (n.x-vectOrient[0]*(wid)), (int) (n.y-vectOrient[1]*(wid)),(int) (n.x+vectOrient[0]*(wid)), (int) (n.y+vectOrient[1]*(wid)));
			}
			else {
				//draw starting point as any symbol
				ip.setColor(Color.white);
				ip.drawRect((int) ((n.x+0.5)*SIZE_FACTOR-1), (int) ((n.y+0.5)*SIZE_FACTOR)-1, 3, 3);
				if(n.birthTime>=maxDate)maxDate=n.birthTime;
			}
			boolean timeOver=false;
			while(n.child != null && (!timeOver) && (n.birthTime<observationTime)){
				n1 = n;
				n = n.child;
				if(n.child==null && distinguishDateStartSymbol)continue;
				if(n.birthTime>observationTime) continue;
				double delta=Math.abs(n.birthTime-Math.round(n.birthTime));
				if(!showIntermediateSymbol && delta >0.001)continue;
				if(delta<=0.001 && distinguishDateStartSymbol) {
					//draw date start symbol
					double[]vectOrient=TransformUtils.normalize(new double[] {-n.vy,n.vx});
					if(Math.abs(vectOrient[0])>Math.abs(vectOrient[1]))vectOrient=TransformUtils.multiplyVector(vectOrient, 1/Math.abs(vectOrient[0]));
					else vectOrient=TransformUtils.multiplyVector(vectOrient, 1/Math.abs(vectOrient[1]));
					ip.setColor(Color.white);
					ip.setLineWidth((int) wid);
					ip.drawLine((int) (n.x-vectOrient[0]*(wid)), (int) (n.y-vectOrient[1]*(wid)),(int) (n.x+vectOrient[0]*(wid)), (int) (n.y+vectOrient[1]*(wid)));
				}
				else {
					//draw intermediary point					
					int xCenter=(int) ((n.x+0.5)*SIZE_FACTOR-1);
					int yCenter=(int) ((n.y+0.5)*SIZE_FACTOR)-1;
					ip.setColor(Color.white);
					ip.drawRect((int) ((n.x+0.5)*SIZE_FACTOR-1), (int) ((n.y+0.5)*SIZE_FACTOR)-1, 3, 3);
					ip.setColor(Color.black);
					ip.drawPixel(xCenter+1,yCenter );
					ip.drawPixel(xCenter,yCenter+1 );
					ip.drawPixel(xCenter+1,yCenter+2 );
					ip.drawPixel(xCenter+2,yCenter+1 );
				}
			}			
			//draw end point
			if(n.birthTime<=observationTime) {
				int xCenter=(int) ((n.x+0.5)*SIZE_FACTOR-1);
				int yCenter=(int) ((n.y+0.5)*SIZE_FACTOR)-1;
				ip.setColor(Color.white);
				ip.drawRect((int) ((n.x+0.5)*SIZE_FACTOR-1), (int) ((n.y+0.5)*SIZE_FACTOR)-1, 3, 3);
				ip.setColor(Color.black);
				ip.drawPixel(xCenter,yCenter );
				ip.drawPixel(xCenter+2,yCenter );
				ip.drawPixel(xCenter+2,yCenter+2 );
				ip.drawPixel(xCenter,yCenter+2 );
			}
		}
		if(binaryColor)		IJ.run(imgRSML,"Red/Green","");
		else IJ.run(imgRSML,"Fire","");
		imgRSML.setDisplayRange(0, maxDate+2);
		return imgRSML;
    }

    
    /**
     * Creates the gray scale image time lapse.
     *
     * @param imgInitSize the img init size
     * @param observationTimes the observation times
     * @param lineWidths the line widths
     * @param deltaModel the delta model
     * @return the image plus
     */
    public ImagePlus createGrayScaleImageTimeLapse(ImagePlus imgInitSize, double []observationTimes,double[]lineWidths,double deltaModel) {//TODO : pass into hours
    	int[]initDims=new int[] {imgInitSize.getWidth(),imgInitSize.getHeight()};
    	int Z=observationTimes.length;
    	ImagePlus imgRSML=IJ.createImage("",initDims[0],initDims[1], Z,8);
    	int w=initDims[0];
 	    int h=initDims[1];
 	    double maxDate=0;
		ImageProcessor[] ips=new ImageProcessor[Z];
		for(int z=0;z<Z;z++)ips[z]=imgRSML.getStack().getProcessor(z+1);

		//Draw lines and dots
		int nTot=0;
		int nPrim=0; 
		int nSecond=0;
		for(int i = 0; i < rootList.size(); i++){
			Root r =  (Root) rootList.get(i);
			Node n = r.firstNode;
			Node n1;
			int lwid=(int) lineWidths[r.order-1];
			int color=(r.order==1 ? 1 : 2);
			int dotEvery=lwid*2;
			if(r.order==1)nPrim++;
			if(r.order==2)nSecond++;
			nTot++;
			int incr=0;
			while(n.child != null){
				n1 = n;
				n = n.child;
				//There is a central line element between n1 and n, that need to be drawn with color and lwid				

				//Identify the first index t where line appear in dot
				int indFirstDot=0;
				for(int indt=0;indt<Z;indt++)if(n1.birthTimeHours>=(observationTimes[indt]-deltaModel))indFirstDot=indt;
				indFirstDot++;

				//Identify the first index t where line appear in plain
				int indFirstPlain=indFirstDot;
				for(int indt=indFirstDot;indt<Z;indt++)if(n.birthTimeHours>(observationTimes[indt]-deltaModel))indFirstPlain=indt;
				indFirstPlain++;
				if(indFirstPlain>Z)indFirstPlain=Z;
				
				//Draw differently these cases
				for(int indt=indFirstDot;indt<indFirstPlain;indt++) {
					//In that case, Identify the part to draw and draw it in dot
					double deltaT=n.birthTimeHours-n1.birthTimeHours;
					if(deltaT<VitimageUtils.EPSILON)deltaT=1;
					double deltaTrelative=(observationTimes[indt]-n1.birthTimeHours)/deltaT;
					double deltaX=n.x-n1.x;
					double deltaY=n.y-n1.y;
					double interX=n1.x+deltaTrelative*deltaX;
					double interY=n1.y+deltaTrelative*deltaY;
					ips[indt].setColor(color);
					ips[indt].setLineWidth(lwid);
					drawDotline(ips[indt],ImagePlus.GRAY8,(int)n1.x,(int)n1.y,(int)interX,(int)interY, dotEvery,lwid,color);
					//then draw the parent node
					double delta=Math.abs(n1.birthTime-Math.round(n1.birthTimeHours));
					if(delta<=0.001 ||true) {
						//draw date start symbol
					}
					else {
						//draw intermediary point					
						int xCenter=(int) ((n.x+0.5)-1);
						int yCenter=(int) ((n.y+0.5)-1);
						ips[indt].setColor(3);
						ips[indt].drawRect((int) ((n.x+0.5)-1), (int) ((n.y+0.5))-1, 3, 3);
						ips[indt].setColor(0);
						ips[indt].drawPixel(xCenter+1,yCenter );
						ips[indt].drawPixel(xCenter,yCenter+1 );
						ips[indt].drawPixel(xCenter+1,yCenter+2 );
						ips[indt].drawPixel(xCenter+2,yCenter+1 );
					}				
				}

				for(int indt=indFirstPlain;indt<Z;indt++) {
					//In the other case, draw the line in plain
					ips[indt].setColor(color);
					ips[indt].setLineWidth(lwid);
					//ips[indt].drawLine((int) n1.x,(int)n1.y,(int)n.x,(int)n.y);					
					drawDotline(ips[indt],ImagePlus.GRAY8,(int)n1.x,(int)n1.y,(int)n.x,(int)n.y, dotEvery,lwid,color);
					//then draw the parent node
					double delta=Math.abs(n1.birthTimeHours-Math.round(n1.birthTimeHours));
					if(delta<=0.001 ||true) {
						//draw date start symbol
					}
					else {
						//draw intermediary point					
						int xCenter=(int) ((n.x+0.5)-1);
						int yCenter=(int) ((n.y+0.5)-1);
						ips[indt].setColor(3);
						ips[indt].drawRect((int) ((n.x+0.5)-1), (int) ((n.y+0.5))-1, 3, 3);
						ips[indt].setColor(0);
						ips[indt].drawPixel(xCenter+1,yCenter );
						ips[indt].drawPixel(xCenter,yCenter+1 );
						ips[indt].drawPixel(xCenter+1,yCenter+2 );
						ips[indt].drawPixel(xCenter+2,yCenter+1 );
					}				
				}				
			}
		}

		imgRSML.setDisplayRange(0, 3);
		return imgRSML;
    }

    
    
    
    /**
     * Creates the gray scale image.
     *
     * @param imgRef the img ref
     * @param sigmaSmooth the sigma smooth
     * @param convexHull the convex hull
     * @param skeleton the skeleton
     * @param width the width
     * @return the image plus
     */
    public ImagePlus createGrayScaleImage(ImagePlus imgRef,double sigmaSmooth,boolean convexHull,boolean skeleton,int width) {
    	int w=imgRef.getWidth();
 	    int h=imgRef.getHeight();
 	    ImagePlus imgRSML=new ImagePlus("",this.createImage(false, width, false, w, h, convexHull));  
 	   imgRSML=VitimageUtils.gaussianFiltering(imgRSML, sigmaSmooth, sigmaSmooth, sigmaSmooth);
		if(skeleton) {
			ImageProcessor ip=imgRSML.getProcessor();
			for(int i = 0; i < rootList.size(); i++){
				Root r =  (Root) rootList.get(i);
				System.out.println("Got root "+r);
				Node n = r.firstNode;
				Node n1;
				while(n.child != null){
					n1 = n;
					n = n.child;
					ip.setColor(Color.white);
					ip.setLineWidth(width);
			    	//ip.drawLine((int) (n1.x), (int) (n1.y), (int) (n.x), (int) (n.y));
				}
			}
			for(int i = 0; i < rootList.size(); i++){
				Root r =  (Root) rootList.get(i);
				Node n = r.firstNode;
				Node n1;
				ip.setColor(Color.white);
				ip.drawRect((int) (n.x-1), (int) (n.y-1), 3, 3);
				double distLast=0;
				while(n.child != null){
					n1 = n;
					n = n.child;
					distLast+=distance(n,n1);
					if(distLast>5) {
						ip.setColor(Color.black);
						ip.drawRect((int) (n.x-1 ), (int) (n.y-1), 3, 3);
						distLast=0;
					}
				}
			}
		}
		return imgRSML;
    }
    
    
    /**
     * Create an image processor based on the roots contained into the root system.
     *
     * @param color the color
     * @param line the line
     * @param real the real
     * @param w the w
     * @param h the h
     * @param convexhull the convexhull
     * @return the image processor
     */
    public ImageProcessor createImage(boolean color, int line, boolean real, int w, int h, boolean convexhull){
    	
    	Root r;
    	Node n, n1;
    	ImagePlus tracing;

    	
    	if(color) tracing = IJ.createImage("tracing", "RGBblack", w, h, 1);
    	else tracing = IJ.createImage("tracing", "8-bitblack", w, h, 1);
  
        	
    	ImageProcessor tracingP = tracing.getProcessor();    	
    	
	    //if(name == null) fit.checkImageProcessor();
    	for(int i = 0; i < rootList.size(); i++){
			r =  (Root) rootList.get(i);
			n = r.firstNode;
			
			if(color){
				switch(r.isChild()){
					case 0: tracing.setColor(      new Color((int)Math.round(255),0,0)); break;
					case 1: tracing.setColor( new Color(0,   (int)Math.round(255),0  )); break;
					case 2: tracing.setColor(new Color(0,0,(int)Math.round(255))); break;
				}
			}
			else tracing.setColor(Color.white);
			
			while(n.child != null){
				n1 = n;
				n = n.child;
				if(real) tracingP.setLineWidth((int)((r.isChild()==0) ? n.diameter : n.diameter-1));
				else tracingP.setLineWidth((int)((r.isChild()==0) ? line : line-1));
		    	tracingP.drawLine((int) (n1.x), (int) (n1.y), (int) (n.x), (int) (n.y));
			}
			tracing.setProcessor(tracingP);
			if(convexhull){
				if(r.isChild() == 0){
					tracing.setColor(Color.yellow);
					PolygonRoi ch = r.getConvexHull();
		    		int[] xRoi = ch.getXCoordinates();
		    		int[] yRoi = ch.getYCoordinates();
		    		Rectangle rect = ch.getBounds();		
		    		for(int j = 1 ; j < xRoi.length; j++){
		    			tracingP.drawLine(xRoi[j-1]+rect.x, yRoi[j-1]+rect.y, xRoi[j]+rect.x, yRoi[j]+rect.y);
		    		}
	    			tracingP.drawLine(xRoi[xRoi.length-1]+rect.x, yRoi[xRoi.length-1]+rect.y, xRoi[0]+rect.x, yRoi[0]+rect.y);
				}
			}
			tracingP = tracing.getProcessor();
    	}
    	
    	return tracingP;
    }
    

    
    
    
    
    
    
    
    
    
    
    
    /**
     * Create an image processor based on the roots contained into the root system.
     *
     * @param color the color
     * @param line the line
     * @param real the real
     * @param w the w
     * @param h the h
     * @param convexhull the convexhull
     * @param ratioColor the ratio color
     * @return the image processor
     */
    public ImageProcessor createImage(boolean color, int line, boolean real, int w, int h, boolean convexhull,double ratioColor){
    	
    	Root r;
    	Node n, n1;
    	ImagePlus tracing;

    	
    	if(color) tracing = IJ.createImage("tracing", "RGBblack", w, h, 1);
    	else tracing = IJ.createImage("tracing", "8-bit", w, h, 1);
  
        	
    	ImageProcessor tracingP = tracing.getProcessor();    	
    	
	    //if(name == null) fit.checkImageProcessor();
    	for(int i = 0; i < rootList.size(); i++){
			r =  (Root) rootList.get(i);
			n = r.firstNode;
			
			if(color){
				switch(r.isChild()){
					case 0: tracing.setColor(      new Color((int)Math.round(255*ratioColor),0,0)); break;
					case 1: tracing.setColor( new Color(0,   (int)Math.round(255*ratioColor),0  )); break;
					case 2: tracing.setColor(new Color(0,0,(int)Math.round(255*ratioColor))); break;
				}
			}
			else tracing.setColor(Color.black);
			
			while(n.child != null){
				n1 = n;
				n = n.child;
				if(real) tracingP.setLineWidth((int) n.diameter);
				else tracingP.setLineWidth(line);
		    	tracingP.drawLine((int) (n1.x), (int) (n1.y), (int) (n.x), (int) (n.y));
			}
			tracing.setProcessor(tracingP);
			if(convexhull){
				if(r.isChild() == 0){
					tracing.setColor(Color.yellow);
					PolygonRoi ch = r.getConvexHull();
		    		int[] xRoi = ch.getXCoordinates();
		    		int[] yRoi = ch.getYCoordinates();
		    		Rectangle rect = ch.getBounds();		
		    		for(int j = 1 ; j < xRoi.length; j++){
		    			tracingP.drawLine(xRoi[j-1]+rect.x, yRoi[j-1]+rect.y, xRoi[j]+rect.x, yRoi[j]+rect.y);
		    		}
	    			tracingP.drawLine(xRoi[xRoi.length-1]+rect.x, yRoi[xRoi.length-1]+rect.y, xRoi[0]+rect.x, yRoi[0]+rect.y);
				}
			}
			tracingP = tracing.getProcessor();
    	}
    	
    	return tracingP;
    }

    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Create an image processor based on the roots contained into the root system.
     *
     * @param color the color
     * @param line the line
     * @param real the real
     * @param convexhull the convexhull
     * @return the image processor
     */
    public ImageProcessor createImage(boolean color, int line, boolean real, boolean convexhull){
    	
    	Root r;
    	Node n, n1;
    	ImagePlus tracing;

    	int w = getWidth(false);
    	int h = getHeight(true);
    	    	
    	if(color) tracing = IJ.createImage("tracing", "RGB", w+100, h+100, 1);
    	else tracing = IJ.createImage("tracing", "8-bit", w+100, h+100, 1);
  
        	
    	ImageProcessor tracingP = tracing.getProcessor();    	
    	
	    //if(name == null) fit.checkImageProcessor();
    	for(int i = 0; i < rootList.size(); i++){
			r =  (Root) rootList.get(i);
			n = r.firstNode;
			
			if(color){
				switch(r.isChild()){
					case 0: tracing.setColor(Color.red); break;
					case 1: tracing.setColor(Color.green); break;
					case 2: tracing.setColor(Color.blue); break;
				}
			}
			else tracing.setColor(Color.black);
			
			while(n.child != null){
				n1 = n;
				n = n.child;
				if(real) tracingP.setLineWidth((int) n.diameter);
				else tracingP.setLineWidth(line);
		    	tracingP.drawLine((int) (n1.x-getMinX()+50), (int) (n1.y-getMinY()+50), (int) (n.x-getMinX()+50), (int) (n.y-getMinY()+50));
			}
			tracing.setProcessor(tracingP);
			if(convexhull){
				if(r.isChild() == 0){
					tracing.setColor(Color.red);
					PolygonRoi ch = r.getConvexHull();
		    		int[] xRoi = ch.getXCoordinates();
		    		int[] yRoi = ch.getYCoordinates();
		    		Rectangle rect = ch.getBounds();		
		    		for(int j = 1 ; j < xRoi.length; j++){
		    			tracingP.drawLine(xRoi[j-1]+rect.x, yRoi[j-1]+rect.y, xRoi[j]+rect.x, yRoi[j]+rect.y);
		    		}
	    			tracingP.drawLine(xRoi[xRoi.length-1]+rect.x, yRoi[xRoi.length-1]+rect.y, xRoi[0]+rect.x, yRoi[0]+rect.y);
				}
			}
			tracingP = tracing.getProcessor();
    	}
    	
    	return tracingP;
    }
    
    
    
    /**
     * Get the index of the po accession.
     *
     * @param po the po
     * @return the index from po
     */
    public int getIndexFromPo(String po){
    	for(int i = 0; i < FSR.listPo.length; i++){
    		if(po.equals(FSR.listPo[i])) return i;
    	}
    	return 0;
    }
    
    /**
     * Get the convexhull area of all the roots in the image.
     *
     * @return the convex hull area
     */
    public float getConvexHullArea(){
    	return 0;
    }
    
    /**
     * Get the convexhull of all the roots in the image. Uses the native image functions
     *
     * @return the convex hull
     */
    public PolygonRoi getConvexHull(){
    		
    	List<Integer> xList = new ArrayList<Integer>(); 
    	List<Integer> yList = new ArrayList<Integer>();
    	
    	// Add all the nodes coordinates
    	for(int i = 0; i < rootList.size(); i++){
    		Root r = rootList.get(i);
    		Node n = r.firstNode;
    		while (n.child != null){
    			xList.add((int) n.x);
    			yList.add((int) n.y);
    			n = n.child;
    		}
    		xList.add((int) n.x);
    		yList.add((int) n.y);
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
     * Distance.
     *
     * @param x0 the x 0
     * @param y0 the y 0
     * @param x1 the x 1
     * @param y1 the y 1
     * @return the double
     */
    public double distance(double x0,double y0,double x1,double y1) {
		return Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0));
	}

 
    

    
}

