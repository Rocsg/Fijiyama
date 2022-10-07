/*
 * 
 */
package io.github.rocsg.fijiyama.rsml;
/**
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge
 *   
 * Main class for the RSML improter
 */

import java.awt.*;

import ij.*;
import ij.plugin.frame.PlugInFrame;
import io.github.rocsg.fijiyama.rsml.FSR;
import io.github.rocsg.fijiyama.rsml.RSMLGUI;
import io.github.rocsg.fijiyama.rsml.RSML_reader;

// TODO: Auto-generated Javadoc
/**
 * The Class RSML_reader.
 */
public class RSML_reader extends PlugInFrame {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	
	/** The sr. */
	public static FSR sr;
	
	/** The rsml gui. */
	public static RSMLGUI rsmlGui;
	
	/** The instance. */
	private static RSML_reader instance;

	/**
	 * Constructor.
	 */
	public RSML_reader() {
      super("RSML Reader");

      if (instance != null) {
         IJ.error("RSML Reader is already running");
         return;
         }
         
      (sr = new FSR()).initialize();
      rsmlGui = new RSMLGUI();      
      }

   /**
    * Close the window.
    */
   public void dispose() {
      Rectangle r = getBounds();
      FSR.prefs.putInt("Explorer.Location.X", r.x);
      FSR.prefs.putInt("Explorer.Location.Y", r.y);
      FSR.prefs.putInt("Explorer.Location.Width", r.width);
      FSR.prefs.putInt("Explorer.Location.Height", r.height);
      rsmlGui.dispose();
      instance = null;
      super.dispose();
      }

   /**
    * Get instance.
    *
    * @return single instance of RSML_reader
    */
   public static RSML_reader getInstance() {return instance; }

   
   /**
    * Main class.
    *
    * @param args the arguments
    */
   @SuppressWarnings("unused")
public static void main(String args[]) {
      ImageJ ij = new ImageJ();
      RSML_reader ie = new RSML_reader();
      }
   }

