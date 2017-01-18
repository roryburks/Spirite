// Rory Burks

package spirite.brains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ToolsetManager manages the currently selected toolset
 */
public class ToolsetManager {
	
	public static enum Tool {
		PEN("pen"), 
		ERASER("eraser"), 
		FILL("fill"), 
		BOX_SELECTION("box_selection"), 
		MOVE("move"),
		COLOR_PICKER("color_picker");
		
		public final String name;
		Tool( String name){ this.name = name;}
	}
    
    public enum Cursor {MOUSE, STYLUS, ERASER};
    private Cursor cursor = Cursor.MOUSE;
    
    private final Map<Cursor, Tool> selected = new EnumMap<>(Cursor.class);
    
    public ToolsetManager() {
        selected.put(Cursor.MOUSE, Tool.PEN);
        selected.put(Cursor.STYLUS, Tool.PEN);
        selected.put(Cursor.ERASER, Tool.ERASER);
    }
    
    // :::: Get/Set
    public Cursor getCursor() {
    	return cursor;
    }
    public void setCursor( Cursor cursor) {
    	this.cursor = cursor;
    	
    	triggerToolsetChanged(selected.get(cursor));
    }

    public void setSelectedTool( Tool tool) {
        selected.replace(cursor, tool);
        triggerToolsetChanged(tool);
    }
    public void setSelectedTool( String tool) {
    	for( Tool check : Tool.values()) {
    		if( check.name.equals(tool)) {
    			setSelectedTool(check);
    		}
    	}
    }
    
    public Tool getSelectedTool() {
        return selected.get(cursor);
    }

    public int getToolCount() {
        return Tool.values().length;
    }
    public Tool getNthTool( int n) {
        return Tool.values()[n];
    }

    // Gets the position the toolset is in the icons.png image
    // !!!! TODO: There is probably a beter place to put this and make it less
    //  hard-coded, but for now I'll centralize it here
    public int getToolix( Tool tool) {
        int ind = tool.ordinal();

        if( ind != -1) {
            return ind % 4;
        }
        return 4;
    }
    public int getTooliy( Tool tool) {
        int ind = tool.ordinal();

        if( ind != -1) {
            return ind / 4;
        }
        return 4;
    }

    // ==== Toolset Observer
    public interface MToolsetObserver {
        public void toolsetChanged( Tool newTool);
    }
    
    List<MToolsetObserver> toolsetObserver = new ArrayList<>();
    public void addToolsetObserver( MToolsetObserver obs) { toolsetObserver.add(obs); }
    public void removeToolsetObserver( MToolsetObserver obs) { toolsetObserver.remove(obs);}
    
    private void triggerToolsetChanged( Tool newTool) {
        for( MToolsetObserver obs : toolsetObserver) {
            obs.toolsetChanged(newTool);
        }
    }

}
