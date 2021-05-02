package fr.cirad.image.rsmlviewer;
/**
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge
 * 
 */

import ij.*;
import java.util.prefs.Preferences;


// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public class FSR {

   public static Preferences prefs;
   
   // Root ontology terms
   public static String[] listPo = {"PO:0009005", "PO:0020127", "PO:0020121", "PO:0025002", "PO:0003005", "PO:0000043"};
   public static String[] listPoNames = {"Root", "Primary root", "Lateral root", "Basal root", "Nodal root", "Crown root"};
	
   
   /**
    * Contstructor
    */
   public FSR() {
      IJ.register(this.getClass());
      }      

   public void initialize() {
      prefs = Preferences.userRoot().node("/ImageJ/SmartRoot");
      }

   /**
    * 
    * @param s
    */
   public static void write(String s) {
      IJ.log(s);
      }

}


