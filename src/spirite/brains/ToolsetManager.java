// Rory Burks

package spirite.brains;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.brains.MasterControl.CommandExecuter;

/**
 * ToolsetManager manages the currently selected toolset
 */
public class ToolsetManager 
	implements CommandExecuter
{
	
	public static enum Tool {
		PEN("Pen"), 
		ERASER("Eraser"), 
		FILL("Fill"), 
		BOX_SELECTION("Box Selection"), 
		MOVE("Move"),
		COLOR_PICKER("Color Picker"),
		PIXEL("Pixel"),
		CROP("Cropper"),
		COMPOSER("Rig Composer");
		
		public final String description;
		Tool( String name){ this.description = name;}
	}
    
    public enum Cursor {MOUSE, STYLUS, ERASER};
    private Cursor cursor = Cursor.MOUSE;
    
    private final Map<Cursor, Tool> selected = new EnumMap<>(Cursor.class);
    private final Map<Tool,ToolSettings> toolSettings = new HashMap<>();
    
    public ToolsetManager() {
        selected.put(Cursor.MOUSE, Tool.PEN);
        selected.put(Cursor.STYLUS, Tool.PEN);
        selected.put(Cursor.ERASER, Tool.ERASER);

        toolSettings.put( Tool.PEN, constructPenSettings());
        toolSettings.put( Tool.PIXEL, constructPixelSettings());
        toolSettings.put( Tool.ERASER, constructEraseSettings());
        toolSettings.put( Tool.CROP, constructCropperSettings());
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
    public boolean setSelectedTool( String tool) {
    	for( Tool check : Tool.values()) {
    		if( check.name().equals(tool)) {
    			setSelectedTool(check);
    			return true;
    		}
    	}
    	return false;
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
    
    public ToolSettings getToolSettings(Tool tool) {
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
    public class ToolSettings {
    	Property properties[];
    	
    	public PropertySchemeNode[] getPropertyScheme() {
    		PropertySchemeNode ret[] = new PropertySchemeNode[properties.length];
    		
    		for( int i=0; i<properties.length; ++i) {
    			ret[i] = new PropertySchemeNode( properties[i]);
    		}
    		return ret;
    	}
    	
    	public Object getValue( String id) {
    		for( int i=0; i<properties.length; ++i) {
    			if( properties[i].id.equals(id)) {
    				return properties[i].value;
    			}
    		}
    		
    		return null;
    	}

    	public void setValue( String id, Object value) {
    		for( int i=0; i<properties.length; ++i) {
    			if( properties[i].id.equals(id)) {
    				if(!getValueClassFromType(properties[i].type).isInstance(value))
    					throw new ClassCastException("Value type does not match Property scheme.");
    				properties[i].value = value;
    			}
    		}
    	}
    }
    static class Property 
    {
    	String id;
    	PropertyType type;
    	String hiName;
    	Object value;
    	int mask;
    }
    public static class PropertySchemeNode 
    {
    	public final String id;
    	public final PropertyType type;
    	public final String hiName;
    	public final Object value;
    	public final int attributeMask;
    	PropertySchemeNode( Property other) {
    		id = other.id;
    		hiName = other.hiName;
    		type = other.type;
    		value = other.value;
    		attributeMask = other.mask;
    	}
    }
    
    public enum PropertyType {
    	SIZE, OPACITY, CHECK_BOX,BUTTON
    }
    
    // :::: Setting Schemes
    private ToolSettings constructPenSettings() {
    	final Object[][] scheme = {
    			{"alpha", PropertyType.OPACITY, "Opacity", 1.0f},
    			{"width", PropertyType.SIZE, "Width", 5.0f},
    	};
    	
    	return constructFromScheme(scheme);
    }
    private ToolSettings constructEraseSettings() {
    	final Object[][] scheme = {
    			{"alpha", PropertyType.OPACITY, "Opacity", 5.0f},
    			{"width",  PropertyType.SIZE, "Width", 5.0f},
    	};
    	
    	return constructFromScheme(scheme);
    }
    private ToolSettings constructPixelSettings() {
    	final Object[][] scheme = {
    			{"alpha", PropertyType.OPACITY, "Opacity", 1.0f},
    	};
    	
    	return constructFromScheme(scheme);
    }
    private ToolSettings constructCropperSettings() {
    	final Object[][] scheme = {
    			{"cropSelection", PropertyType.BUTTON, "Crop Selection", "draw.cropSelection",  DISABLE_ON_NO_SELECTION},
    			{"quickCrop", PropertyType.CHECK_BOX, "Crop on Finish", false},
    			{"shrinkOnly", PropertyType.CHECK_BOX, "Shrink-only Crop", false},
    	};
    	
    	return constructFromScheme(scheme);
    }
    	
    public static final int DISABLE_ON_NO_SELECTION = 0x01;
    
    /**<pre>
     * Constructs ToolSettings from a Nx4 Scheme, returning null if the 
     * scheme is malformed.
     *
     * Setting Scheme Format: a Nx4 array with each 4-length entry 
     *	corresponding to:
     * 0: the id which is used to access/refer to it
     * 1: an identifier of the type of preference, which determines both
     *	the GUI element that appears for it and the data-type of the value
     * 2: A human-readable label that appears on GUI and tooltips
     * 3: the default value
     * 4: (optional) an integer mask that corresponds to various behavior
     * 	such as when the GUI element should be disabled
     </pre>*/
    ToolSettings constructFromScheme( Object[][] scheme) {
    	ToolSettings settings = new ToolSettings();
    	settings.properties = new Property[scheme.length];

    	for(int i=0; i<scheme.length; ++i) {
    		try {
    			if( scheme[i].length > 5) 
    				throw new Exception("Bad Row Type");

    			settings.properties[i] = new Property();
    			settings.properties[i].id = (String)scheme[i][0];
    			settings.properties[i].type = (PropertyType)scheme[i][1];
    			settings.properties[i].hiName = (String)scheme[i][2];
    			if( scheme[i].length > 4) 
        			settings.properties[i].mask = (int)scheme[i][4];
    			
    			if(  !getValueClassFromType(settings.properties[i].type).isInstance(scheme[i][3])) {
        			throw new Exception("Value Class does not match type. Expected: " + getValueClassFromType(settings.properties[i].type).getClass() + " got: " + scheme[i][3].getClass() );
    			}
    			settings.properties[i].value = scheme[i][3];
    		
    		} catch( Exception e) {
    			MDebug.handleError(ErrorType.STRUCTURAL, this, "Improper Toolset Settings Scheme: " + e.getMessage());
    			return null;
    		}
    	}
    	
    	return settings;
    }
    
    //
    Class<?> getValueClassFromType( PropertyType type) {
    	switch( type) {
    	case SIZE:
    	case OPACITY:
    		return Float.class;
		case BUTTON:
			return String.class;
		case CHECK_BOX:
			return Boolean.class;
    	}
    	
    	return null;	// Should be no way to reach this
    }
    
    public static abstract class ToolsetSettingsPanel extends JPanel {
    	public abstract void updateSettings( ToolSettings settings);
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

    // :::: CommandExecuter
	@Override
	public List<String> getValidCommands() {
		Tool[] tools= Tool.values();
		List<String> list = new ArrayList<>(tools.length);
		
		for( Tool tool : tools) {
			list.add( tool.name());
		}
		return list;
	}
	@Override public String getCommandDomain() {
		return "toolset";
	}

	@Override
	public boolean executeCommand(String commmand) {
		return setSelectedTool(commmand);
	}

}
