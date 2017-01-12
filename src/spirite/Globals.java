// Rory Burks

package spirite;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Icon;

/**
 * The Globals object will centralize all globals that might have reason to
 * change (in particular text that might need localizations and user preferences
 * which they can change, though Hotkeys go through the HotkeyManager).
 *
 * How this is implemented may change in the future, so it's best to abstract it
 * 
 * TODO: It is probably best to have either some kind of sorted list for binary
 * 	searches or a contextual tree so that the search time does not get out
 * 	of hand later
 */
public class Globals {
    private static Object colors[][] = {
        {"drawpanel.image.border", new Color(190,190,190)},
        {"drawpanel.layer.border", new Color(16,16,16)},
        {"toolbutton.selected.background", new Color( 128,128,128)},

        {"contentTree.selectedBGDragging", new Color( 192,192,212)},
        {"contentTree.selectedBackground",new Color( 160,160,196)},

        {"undoPanel.selectedBackground",new Color( 160,160,196)},
        {"undoPanel.background",new Color( 238,238,238)},
    };
    
    private static Object metrics[][] = {
    		{"layerpanel.treenodes.max", new Dimension( 32, 32)},
    		{"contentTree.dragdropLeniency", new Dimension( 0, 10)},
    		{"contentTree.buttonSize", new Dimension( 24, 24)},
    		{"contentTree.buttonMargin", new Dimension( 2, 2)},
    		{"workspace.max_size", new Dimension( 20000,20000)},
    };


    public static Color getColor( String id) {
        for( int i = 0; i < colors.length; ++i) {
            if( colors[i][0].equals(id))
                return (Color)colors[i][1];
        }

        return Color.black;
    }

    public static Dimension getMetric( String id) {
    	return getMetric( id, new Dimension(64,64));
    }
    public static Dimension getMetric( String id, Dimension defaultSize) {

        for( int i = 0; i < metrics.length; ++i) {
            if( metrics[i][0].equals(id))
                return (Dimension)metrics[i][1];
        }

        return defaultSize;
    }
    
    
    public static Icon getIcon( String id) {
    	return null;
    }
}
