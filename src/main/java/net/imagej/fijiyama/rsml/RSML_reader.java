package net.imagej.fijiyama.rsml;
/**
 * @author Xavier Draye - Universit� catholique de Louvain
 * @author Guillaume Lobet - Universit� de Li�ge
 *   
 * Main class for the RSML improter
 */

import java.awt.*;

import ij.*;
import ij.plugin.frame.PlugInFrame;
import net.imagej.fijiyama.rsml.FSR;
import net.imagej.fijiyama.rsml.RSMLGUI;
import net.imagej.fijiyama.rsml.RSML_reader;

public class RSML_reader extends PlugInFrame {

	private static final long serialVersionUID = 1L;
	public static FSR sr;
	public static RSMLGUI rsmlGui;
	private static RSML_reader instance;

	/**
	 * Constructor
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
    * Close the window
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
    * Get instance
    * @return
    */
   public static RSML_reader getInstance() {return instance; }

   
   /**
    * Main class
    * @param args
    */
   @SuppressWarnings("unused")
public static void main(String args[]) {
      ImageJ ij = new ImageJ();
      RSML_reader ie = new RSML_reader();
      }
   }

