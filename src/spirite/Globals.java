// Rory Burks

package spirite;

import java.awt.Color;
import java.awt.Dimension;

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
        {"toolbutton.selected.background", new Color( 128,128,128)}
    };
    
    private static Object metrics[][] = {
    		{"layerpanel.treenodes.max", new Dimension( 40, 40)}
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
    
}
