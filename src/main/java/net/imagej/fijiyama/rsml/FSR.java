/*
 * 
 */
package net.imagej.fijiyama.rsml;
/**
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge
 * 
 */

import ij.*;
import java.util.prefs.Preferences;
/**
  * The Class FSR. RSML Builder have to instantiate a FSR first in order to use RootSystem capabilities
  */
 public class FSR {

   /** The preferences in ImageJ. */
   public static Preferences prefs;
   
   /** The list po. */
   // Root ontology terms
   public static String[] listPo = {"PO:0009005", "PO:0020127", "PO:0020121", "PO:0025002", "PO:0003005", "PO:0000043"};
   
   /** The list po names. */
   public static String[] listPoNames = {"Root", "Primary root", "Lateral root", "Basal root", "Nodal root", "Crown root"};
	
   
   /**
    * Class constructor
    */
   public FSR() {
      IJ.register(this.getClass());
      }      

   /**
    * Initialization. Optional for most needs
    */
   public void initialize() {
      prefs = Preferences.userRoot().node("/ImageJ/SmartRoot");
      }

   /**
    * Write
    *
    * @param s the String to write
    */
   public static void write(String s) {
      IJ.log(s);
      }

}


