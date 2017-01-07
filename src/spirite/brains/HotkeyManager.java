
package spirite.brains;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.prefs.Preferences;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;


/***
 * Each hotkey-able action has a text identifier of the form
 * {context}.{action}
 * 
 * Not just a visual significance, the context text will be used to determine which
 * component needs to handle the hotkey command.  The hotkey manager stores all
 * links between the hotkey-able actions and their corresponding Hotkey (a hotkey
 * is a key-code and its modifier, including possibly some extended modifiers).
 * 
 * The HotkeyManager does not actually dictate the actions to the corresponding
 * component (that's handled in the Main Frame), it simply stores all the hotkeys
 * and what they are linked to.
 * 
 * @author Rory Burks
 *
 */
public class HotkeyManager {
    Preferences prefs;
    
    //Map<Hotkey,String> hotkey_map;
    BidiMap<Hotkey,String> hotkey_map;
    
//    Map<>

    public HotkeyManager() {
        prefs = Preferences.userNodeForPackage(spirite.Main.class);
        hotkey_map = new DualHashBidiMap<>();

        loadHotkeys(
            new Object[][] {
                {"global.zoom_in", (new Hotkey( KeyEvent.VK_ADD, 0))},
                {"global.zoom_out", (new Hotkey( KeyEvent.VK_SUBTRACT, 0))},
                {"global.zoom_in_slow", (new Hotkey( KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK))},
                {"global.zoom_out_slow", (new Hotkey( KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK))},
                {"global.zoom_0", (new Hotkey( KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK))},

                {"toolset.pen", (new Hotkey( KeyEvent.VK_P, 0))},
                {"toolset.eraser", (new Hotkey( KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK))},
                {"toolset.fill", (new Hotkey( KeyEvent.VK_B, InputEvent.SHIFT_DOWN_MASK))},
                {"toolset.box_selection", (new Hotkey( KeyEvent.VK_R, 0))},
                {"toolset.move", (new Hotkey( KeyEvent.VK_M, 0))},
                {"toolset.color_picker", (new Hotkey( KeyEvent.VK_O, 0))},

                {"palette.swap", (new Hotkey( KeyEvent.VK_X, 0))},
                
                {"global.newLayerQuick", (new Hotkey( KeyEvent.VK_INSERT, 0))},

                {"global.undo", (new Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK))},
                {"global.redo", (new Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},
            }
        );
        
        loadBinds(
    		new Object[][] {
    			{"stylus", "toolset.pen"},
    			{"eraser", "toolset.eraser"},
    			{"mouse" , "toolset.pen"}
    		}
        );
    }

    /**
     * Loads the hotkey data from preference space using a hard-coded set of key
     * commands and default hotkeys with their modifier 
     * @param data
     */
    private void loadHotkeys( Object[][] data) {
        for( Object[] obj : data) {
            Hotkey key = (Hotkey)obj[1];
            String s = prefs.get( (String)obj[0], key.toPrefString());

            key.fromPrefString(s);

            hotkey_map.put(key, (String)obj[0]);
        }
    }
    
    private void loadBinds( Object[][] data) {
    	
    }
    
    public String getCommand( int key, int modifier) {
        String s = hotkey_map.get(new Hotkey(key, modifier));
        return s;
    }

    public Hotkey getHotkey( String command) {
        return hotkey_map.getKey(command);
    }

    /**
     * A Hotkey
     */
    public static class Hotkey
    {
        int key;        // from MouseEvent.getButton
                        // or   KeyEvent.getKey
        int modifier;   // from InputEvent.getModifiersEx

        public Hotkey( int key, int modifier) {
            this.key = key;
            this.modifier = modifier;
        }

        public String toPrefString() {
            ByteBuffer buff = ByteBuffer.allocate(10);
            buff.order(ByteOrder.BIG_ENDIAN);

            buff.putInt(key);
            buff.putInt(modifier);

            // !!! Bad
            try {
                return new String(buff.array(), "ISO-8859-1");//buff.array().toString();
            } catch (UnsupportedEncodingException ex) {
                return "";
            }
        }

        public void fromPrefString( String s) {
            ByteBuffer buff;

            // !!! Bad
            try {
                buff = ByteBuffer.wrap(s.getBytes("ISO-8859-1"));
            } catch (UnsupportedEncodingException ex) {
                buff = ByteBuffer.allocate(0);
            }
            buff.order( ByteOrder.BIG_ENDIAN);

            key = buff.getInt();
            modifier = buff.getInt();
        }

        @Override
        public String toString() {

            return ((modifier != 0) ? KeyEvent.getModifiersExText(modifier) + " + " : "") + KeyEvent.getKeyText(key);
        }

        // Added to make sure the HashMap works
        @Override
        public boolean equals( Object obj) {
            if( obj instanceof Hotkey) {
                Hotkey other = (Hotkey) obj;

                if( key == other.key && modifier == other.modifier)
                    return true;
            }
            return false;
        }

        // !!! Automaticall generated by Netbeans
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + this.key;
            hash = 23 * hash + this.modifier;
            return hash;
        }
    }
}
