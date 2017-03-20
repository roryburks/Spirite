// Rory Burks

package spirite.brains;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
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
		PEN("Pen", 0), 
		ERASER("Eraser",1), 
		FILL("Fill",2), 
		BOX_SELECTION("Box Selection",3), 
		FREEFORM_SELECTION("Free Selection",4), 
		MOVE("Move",5),
		PIXEL("Pixel",6),
		CROP("Cropper",7),
		COMPOSER("Rig Composer",8),
		FLIPPER("Horizontal/Vertical Flipping",9),
		RESHAPER("Reshaping Tool",10),
		COLOR_CHANGE("Color Change Tool",11),
		COLOR_PICKER("Color Picker",12),;
		
		public final String description;
		public final int iconLocation;
		Tool( String name, int offset){ 
			this.description = name; 
			
			// Could possibly just be replaced with .ordinal, but that's probably
			//	bad practice.
			this.iconLocation = offset;	
		}
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
        toolSettings.put( Tool.BOX_SELECTION, constructBoxSelectionSettings());
        toolSettings.put( Tool.CROP, constructCropperSettings());
        toolSettings.put( Tool.FLIPPER, constructFlipperSettings());
        toolSettings.put( Tool.COLOR_CHANGE, constructColorChangeSettings());
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
    

    BufferedImage icon_sheet = null;
    int is_width, is_height;
    private static final int TOOL_ICON_WIDTH = 24;
    private static final int TOOL_ICON_HEIGHT = 24;
    // Loads the icon sheet from icons.resources
    private void prepareIconSheet() {
        icon_sheet = null;
        try {
            BufferedImage buff = ImageIO.read ( getClass().getClassLoader().getResource("tool_icons.png").openStream());
            icon_sheet = new BufferedImage( buff.getWidth(), buff.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            
            Graphics g = icon_sheet.getGraphics();
            g.drawImage(buff, 0, 0, null);
            g.dispose();

            is_width = icon_sheet.getWidth() / (TOOL_ICON_WIDTH+1);
            is_height = icon_sheet.getHeight() / (TOOL_ICON_HEIGHT+1);
        } catch (IOException e) {
        	MDebug.handleError( ErrorType.RESOURCE, e, "Failed to prepare Toolset Icon Sheet");
        }
    }
    
    public void drawIcon( Graphics g, Tool tool) {
    	if( icon_sheet == null) prepareIconSheet();
    	int ix = getToolix(tool);
    	int iy = getTooliy(tool);
        g.drawImage( icon_sheet, 0, 0, TOOL_ICON_WIDTH, TOOL_ICON_HEIGHT,
                ix*(TOOL_ICON_WIDTH+1), iy*(TOOL_ICON_HEIGHT+1), 
                ix*(TOOL_ICON_WIDTH+1)+TOOL_ICON_WIDTH, iy*(TOOL_ICON_HEIGHT+1)+TOOL_ICON_HEIGHT, null);
    }

    // Gets the position the toolset is in the icons.png image
    private int getToolix( Tool tool) {
    	return tool.iconLocation % is_width;
    }
    private int getTooliy( Tool tool) {
        return tool.iconLocation / is_width;
    }


    // :::: Toolset Settings
    public class ToolSettings {
    	Property properties[];
    	
    	public Property[] getPropertyScheme() {
    		Property ret[] = new Property[properties.length];
    		
    		for( int i=0; i<properties.length; ++i) {
    			ret[i] = properties[i];
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
    	
    	public Object getExtra(String id) {
    		for( int i=0; i<properties.length; ++i) {
    			if( properties[i].id.equals(id)) {
    				return properties[i].extra;
    			}
    		}
    		return null;
    	}
    }
    public static class Property 
    {
    	String id;
    	PropertyType type;
    	String hiName;
    	Object value;
    	Object extra;
    	int mask;

    	public String getId() {return id;}
    	public PropertyType getType() {return type;}
    	public String getName() {return hiName;}
    	public Object getValue() {return value;}
    	public Object getExtra() {return extra;}
    	public int getMask() {return mask;}
    }
    
    public enum PropertyType {
    	SIZE, OPACITY, CHECK_BOX,BUTTON, 
    	// RADIO_BUTTON is a special Property Type in that 
    	RADIO_BUTTON,
    	// DropDown
    	DROP_DOWN
    }
    
    // :::: Setting Schemes
    private ToolSettings constructPenSettings() {
    	final Object[][] scheme = {
    			{"alpha", PropertyType.OPACITY, "Opacity", 1.0f},
    			{"width", PropertyType.SIZE, "Width", 5.0f},
    			{"hard", PropertyType.CHECK_BOX, "Hard Edged", false},
    	};
    	
    	return constructFromScheme(scheme);
    }
    private ToolSettings constructEraseSettings() {
    	final Object[][] scheme = {
    			{"alpha", PropertyType.OPACITY, "Opacity", 5.0f},
    			{"width",  PropertyType.SIZE, "Width", 5.0f},
    			{"hard", PropertyType.CHECK_BOX, "Hard Edged", false},
    	};
    	
    	return constructFromScheme(scheme);
    }
    private ToolSettings constructPixelSettings() {
    	final Object[][] scheme = {
    			{"alpha", PropertyType.OPACITY, "Opacity", 1.0f},
    	};
    	
    	return constructFromScheme(scheme);
    }
    private ToolSettings constructBoxSelectionSettings() {
    	final Object[][] scheme = {
    			{"shape", PropertyType.DROP_DOWN, "Shape", 0, 0, 
    				new String[]{"Rectangle","Oval"}},
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
    private ToolSettings constructFlipperSettings() {
    	final Object[][] scheme = {
    			{"flipMode", PropertyType.RADIO_BUTTON, "Flip Mode", 2, 0,
    				new String[] {"Horizontal Flipping", "Vertical Flipping", "Determine from Movement"}
    			},
    	};
    	
    	return constructFromScheme(scheme);
    }
   
    private ToolSettings constructColorChangeSettings() {
    	final Object[][] scheme = {
        		{"scope", PropertyType.DROP_DOWN,"Scope",  0, 0, new String[]{"Local","Entire Layer/Group","Entire Project"}},
    			{"ignoreAlpha", PropertyType.CHECK_BOX, "Ignore Alpha", true},
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
     * 5: (optional) any additional type-specific data needed
     </pre>*/
    ToolSettings constructFromScheme( Object[][] scheme) {
    	ToolSettings settings = new ToolSettings();
    	settings.properties = new Property[scheme.length];

    	for(int i=0; i<scheme.length; ++i) {
    		try {
    			if( scheme[i].length < 4) 
    				throw new Exception("Bad Row Type");

    			settings.properties[i] = new Property();
    			settings.properties[i].id = (String)scheme[i][0];
    			settings.properties[i].type = (PropertyType)scheme[i][1];
    			settings.properties[i].hiName = (String)scheme[i][2];
    			if( scheme[i].length > 4) 
        			settings.properties[i].mask = (int)scheme[i][4];
    			if( scheme[i].length > 5)
    				settings.properties[i].extra = scheme[i][5];
    			
    			if(  !getValueClassFromType(settings.properties[i].type).isInstance(scheme[i][3])) {
        			throw new Exception("Value Class does not match type. Expected: " + getValueClassFromType(settings.properties[i].type).getClass() + " got: " + scheme[i][3].getClass() );
    			}
    			settings.properties[i].value = scheme[i][3];
    		
    		} catch( Exception e) {
    			MDebug.handleError(ErrorType.STRUCTURAL, e, "Improper Toolset Settings Scheme: " + e.getMessage());
    			return null;
    		}
    	}
    	
    	return settings;
    }
    
    private 
    
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
		case DROP_DOWN:
			return Integer.class;
		case RADIO_BUTTON:
			return Integer.class;
    	}
    	
    	return null;	// Should be no way to reach this
    }
    
    public static abstract class ToolsetSettingsPanel extends JPanel {
    	public abstract void updateSettings( ToolSettings settings);
    }
    
    // ==== Toolset Observer
    public interface MToolsetObserver {
        public void toolsetChanged( Tool newTool);
//        public void toolsetPropertyChanged( Tool tool);
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
