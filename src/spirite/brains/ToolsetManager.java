// Rory Burks

package spirite.brains;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

/**
 * ToolsetManager manages the currently selected toolset
 */
public class ToolsetManager {
	
	public static enum Tool {
		PEN("Pen"), 
		ERASER("Eraser"), 
		FILL("Fill"), 
		BOX_SELECTION("Box Selection"), 
		MOVE("Move"),
		COLOR_PICKER("Color Picker");
		
		public final String description;
		Tool( String name){ this.description = name;}
	}
    
    public enum Cursor {MOUSE, STYLUS, ERASER};
    private Cursor cursor = Cursor.MOUSE;
    
    private final Map<Cursor, Tool> selected = new EnumMap<>(Cursor.class);
    private final Map<Tool,ToolsetSettings> toolSettings = new HashMap<>();
    
    public ToolsetManager() {
        selected.put(Cursor.MOUSE, Tool.PEN);
        selected.put(Cursor.STYLUS, Tool.PEN);
        selected.put(Cursor.ERASER, Tool.ERASER);

        toolSettings.put( Tool.PEN, new PixelSettings());
        toolSettings.put( Tool.ERASER, new PixelSettings());
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
    		if( check.name().equals(tool)) {
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
    
    public ToolsetSettings getToolsetSettings(Tool tool) {
    	return toolSettings.get(tool);
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


    // :::: Toolset Settings
    public static abstract class ToolsetSettings {
    	
    }
    public static class PixelSettings extends ToolsetSettings {
    	private float width = 5;
    	
    	public float getWidth() {return width;}
    	public void setWidth( float width) {this.width = width;}
    }
    public static class EraserSettings extends ToolsetSettings {
    	private float width = 5;
    	
    	public float getWidth() {return width;}
    	public void setWidth( float width) {this.width = width;}
    }
    
    
    public static abstract class ToolsetSettingsPanel extends JPanel {
    	public abstract void updateSettings( ToolsetSettings settings);
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
