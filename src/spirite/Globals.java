// Rory Burks

package spirite;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;

/**
 * The Globals object will centralize all globals that might have reason to
 * change (in particular text that might need localizations and user preferences
 * which they can change, though Hotkeys go through the HotkeyManager).
 *
 * How this is implemented may change in the future, so it's best to abstract it
 */
public class Globals {
    private static Object colors[][] = {
        {"drawpanel.image.border", new Color(190,190,190)},
        {"drawpanel.layer.border", new Color(16,16,16)},
        {"toolbutton.selected.background", new Color( 128,128,128)},

        {"layerpanel.tree.selectedBGDragging", new Color( 192,192,212)},
        {"layerpanel.tree.selectedBackground",new Color( 160,160,196)},
    };
    
    private static Object metrics[][] = {
    		{"layerpanel.treenodes.max", new Dimension( 40, 40)},
    		{"layerpanel.treenodes.dragdropleniency", new Dimension( 0, 10)},
    };
    
	// Each dot before the name indicates the level it should be in.  For example one dot
	//	means it goes inside the last zero-dot item, two dots means it should go in the last
	//	one-dot item, etc.
	// Note, there should never be any reason to skip dots and doing so will probably 
	//	break it.
	private static Object[][] menu_scheme = {
			{"File", KeyEvent.VK_F, null},
			{".New Image", KeyEvent.VK_N, "global.new_image"},
			{".Debug Color", KeyEvent.VK_C, "global.debug_color"},
			
			{"Edit", KeyEvent.VK_E, null},
			
			{"Window", KeyEvent.VK_W, null},
			{".Dialogs", KeyEvent.VK_D, null},
			{"..Layers", KeyEvent.VK_L, "frame.showLayerFrame"},
			{"..Tools", KeyEvent.VK_T, "frame.showToolsFrame"},
			{"..-",null,null},
			{"..Animation Scheme", KeyEvent.VK_S, "frame.showAnimSchemeFrame"},
	};

    public static Color getColor( String id) {
        for( int i = 0; i < colors.length; ++i) {
            if( colors[i][0].equals(id))
                return (Color)colors[i][1];
        }

        return Color.black;
    }
    
    public static Dimension getMetric( String id) {

        for( int i = 0; i < metrics.length; ++i) {
            if( metrics[i][0].equals(id))
                return (Dimension)metrics[i][1];
        }

        return new Dimension( 64,64);
    }
    
    public static Object[][] getMenuSchem() {
    	return menu_scheme;
    }
    
}
