
package spirite.base.brains;

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
 * TODO: This entire class really needs to be re-written such that the following
 * functions are allowed and work as intended:
 * -Allowing multiple hotkeys for a single command (but not vice versa)
 * -Allow for saving and loading of Hotkey maps (both to preference string and
 * to file) without using that ugly ISO encoding.
 * 
 * @author Rory Burks
 *
 */
public class HotkeyManager {
    private final Preferences prefs;    
    private final BidiMap<Hotkey,String> hotkey_map;
    

    HotkeyManager() {
        prefs = Preferences.userNodeForPackage(spirite.pc.Spirite.class);
        hotkey_map = new DualHashBidiMap<>();

        loadHotkeys(
            new Object[][] {
                {"context.zoom_in", (new Hotkey( KeyEvent.VK_ADD, 0))},
                {"context.zoom_out", (new Hotkey( KeyEvent.VK_SUBTRACT, 0))},
                {"context.zoom_in_slow", (new Hotkey( KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK))},
                {"context.zoom_out_slow", (new Hotkey( KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK))},
                {"context.zoom_0", (new Hotkey( KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK))},

                {"toolset.PEN", (new Hotkey( KeyEvent.VK_P, 0))},
                {"toolset.ERASER", (new Hotkey( KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK))},
                {"toolset.FILL", (new Hotkey( KeyEvent.VK_B, InputEvent.SHIFT_DOWN_MASK))},
                {"toolset.BOX_SELECTION", (new Hotkey( KeyEvent.VK_R, 0))},
                {"toolset.MOVE", (new Hotkey( KeyEvent.VK_M, 0))},
                {"toolset.COLOR_PICKER", (new Hotkey( KeyEvent.VK_O, 0))},
                {"toolset.PIXEL", (new Hotkey( KeyEvent.VK_A, 0))},
                {"toolset.COMPOSER", (new Hotkey( KeyEvent.VK_CAPS_LOCK, 0))},

                {"palette.swap", (new Hotkey( KeyEvent.VK_X, 0))},
                {"palette.swapBack", (new Hotkey( KeyEvent.VK_Z, 0))},
                
                {"draw.newLayerQuick", (new Hotkey( KeyEvent.VK_INSERT, 0))},
                {"draw.undo", (new Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK))},
                {"draw.redo", (new Hotkey( KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},
                {"draw.clearLayer", (new Hotkey( KeyEvent.VK_DELETE, 0))},
                {"draw.invert", (new Hotkey( KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK ))},
                {"draw.toggle_reference", (new Hotkey( KeyEvent.VK_BACK_QUOTE, 0))},	
                {"draw.lift_to_reference", (new Hotkey( KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},	

                {"draw.shiftLeft", (new Hotkey( KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK))},
                {"draw.shiftRight", (new Hotkey( KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK))},
                {"draw.shiftDown", (new Hotkey( KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK))},
                {"draw.shiftUp", (new Hotkey( KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK))},

                {"select.all", (new Hotkey( KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK))},
                {"select.none", (new Hotkey( KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},
                {"select.invert", (new Hotkey( KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},
                
                {"global.save_image", (new Hotkey( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK))},
                {"global.copy", (new Hotkey( KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK))},
                {"global.copyVisible", (new Hotkey( KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},
                {"global.paste", (new Hotkey( KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK))},
                {"global.pasteAsLayer", (new Hotkey( KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},
                {"global.cut", (new Hotkey( KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK))},
                
                // TODO: This should really be anim., but that might require restructuring/rethinking of
                //	command execution system.
                {"draw.addGapQuick", new Hotkey( KeyEvent.VK_INSERT, InputEvent.SHIFT_DOWN_MASK)},

                {"global.debug1", (new Hotkey( KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK))},
            }
        );
        
        loadBinds(
    		new Object[][] {
    			{"stylus", "toolset.PEN"},
    			{"eraser", "toolset.ERASER"},
    			{"mouse" , "toolset.PEN"}
    		}
        );
    }
    
    /***
     * Returns whether the given keycode is considered a "Modifier" (in other
     * words that key alone should not serve as a hotkey).
     */
    public static boolean isModifier( int keycode) {
    	switch( keycode) {
    	case KeyEvent.VK_CONTROL:
    	case KeyEvent.VK_SHIFT:
    	case KeyEvent.VK_ALT:
    		return true;
		default:
			return false;
    	}
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
    
    public void setCommand( int key, int modifier, String command) {
    	hotkey_map.put( new Hotkey( key, modifier), command);
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
            	String str = new String(buff.array(), "ISO-8859-1");
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
