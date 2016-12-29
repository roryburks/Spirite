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
    String[] toolset = {
        "pen","eraser","fill","box_selection", "move", "color_picker"
    };
    
    public enum Cursor {MOUSE, STYLUS, ERASER};
    private Cursor cursor = Cursor.MOUSE;
    
    Map<Cursor, Integer> selected = new EnumMap<>(Cursor.class);
    
    public ToolsetManager() {
        selected.put(Cursor.MOUSE, 0);
        selected.put(Cursor.STYLUS, 0);
        selected.put(Cursor.ERASER, 1);
    }
    
    // Get/Set currently used Cursor
    public Cursor getCursor() {
    	return cursor;
    }
    public void setCursor( Cursor cursor) {
    	this.cursor = cursor;
    	
    	toolsetChanged(selected.get(cursor));
    }

    public void setTool( String tool) {
        int ind = Arrays.asList(toolset).indexOf(tool);

        if( ind != -1) {
            selected.replace(cursor, ind);
            toolsetChanged(ind);
        }
    }
    
    public String getSelectedTool() {
        return toolset[selected.get(cursor)];
    }

    public int getToolCount() {
        return toolset.length;
    }
    public String getTool( int index) {
        return toolset[index];
    }

    // Gets the position the toolset is in the icons.png image
    // !!!! There is probably a beter place to put this and make it less
    //  hard-coded, but for now I'll centralize it here
    public int getToolix( String tool) {
        int ind = Arrays.asList(toolset).indexOf(tool);

        if( ind != -1) {
            return ind % 4;
        }
        return 4;
    }
    public int getTooliy( String tool) {
        int ind = Arrays.asList(toolset).indexOf(tool);

        if( ind != -1) {
            return ind / 4;
        }
        return 4;
    }

    // ==== Toolset Observer
    List<MToolsetObserver> toolsetObserver = new ArrayList<>();

    public void addToolsetObserver( MToolsetObserver obs) { toolsetObserver.add(obs); }
    public void removeToolsetObserver( MToolsetObserver obs) { toolsetObserver.remove(obs);}
    private void toolsetChanged( int index) {
        for( MToolsetObserver obs : toolsetObserver) {
            obs.toolsetChanged(toolset[index]);
        }
    }

    public interface MToolsetObserver {
        public void toolsetChanged( String new_tool);
    }
}
