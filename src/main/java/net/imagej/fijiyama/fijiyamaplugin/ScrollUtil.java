/*
 * 
 */
package net.imagej.fijiyama.fijiyamaplugin;
import java.awt.Rectangle;

import javax.swing.JComponent;

// TODO: Auto-generated Javadoc
/**Original class from adamski on stack overflow :
* https://stackoverflow.com/questions/2544758/how-to-adjust-position-of-scroll-in-the-scrollpane
*/
public final class ScrollUtil {
    
    /** The Constant SELECTED. */
    public static final int NONE = 0, TOP = 1, VCENTER = 2, BOTTOM = 4, LEFT = 8, HCENTER = 16, RIGHT = 32,SELECTED = 64;
    
    /** The Constant OFFSET. */
    private static final int OFFSET = 0;// Required for hack (see below).100; 

    /**
     * Instantiates a new scroll util.
     */
    private ScrollUtil() {
    }

    
    
    /**
     * Scroll to specified location.  e.g. scroll(component, BOTTOM).
     *
     * @param c JComponent to scroll.
     * @param part Location to scroll to.  Should be a bit-wise OR of one or moe of the values:
     * NONE, TOP, VCENTER, BOTTOM, LEFT, HCENTER, RIGHT.
     * @param opts the opts
     */
    public static void scroll(JComponent c, int part,int []opts) {
        scroll(c, part & (LEFT|HCENTER|RIGHT), part & (TOP|VCENTER|BOTTOM|SELECTED),opts);
    }
    
    /**
     * Scroll to specified location.  e.g.scroll(component, LEFT, BOTTOM);
     *
     * @param c JComponent to scroll.
     * @param horizontal Horizontal location.  Should take the value: LEFT, HCENTER or RIGHT.
     * @param vertical Vertical location.  Should take the value: TOP, VCENTER or BOTTOM.
     * @param opts the opts
     */
    public static void scroll(JComponent c, int horizontal, int vertical,int[]opts) {
        Rectangle visible = c.getVisibleRect();
        Rectangle bounds = c.getBounds();
        switch (vertical) {
            case TOP:     visible.y = 0; break;
            case VCENTER: visible.y = (bounds.height - visible.height) / 2; break;
            case BOTTOM:  visible.y = bounds.height - visible.height + OFFSET; break;
            case SELECTED:  visible.y = (int)((bounds.height*1.0*opts[0])/opts[1]) - visible.height/2 + OFFSET; break;
       }
        visible.y=Math.max(visible.y, 0);
        switch (horizontal) {
            case LEFT:    visible.x = 0; break;
            case HCENTER: visible.x = (bounds.width - visible.width) / 2; break;
            case RIGHT:   visible.x = bounds.width - visible.width + OFFSET; break;
        }
        c.scrollRectToVisible(visible);
    }
}