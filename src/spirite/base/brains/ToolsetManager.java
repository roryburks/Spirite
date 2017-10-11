package spirite.base.brains;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import spirite.base.brains.MasterControl.CommandExecuter;
import spirite.base.util.glmath.Vec2;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.ToolProperties.*;

/**
 * The ToolsetManager manages the set of Tools available, which tool is
 * selected, and what the settings for each tool is.
 *
 * @author Rory Burks
 */
public class ToolsetManager
        implements CommandExecuter
{
	private final MasterControl master;

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

            // Could be replaced with .ordinal for now, but since these
            // numbers are also tied to the position they appear on
            //	tool_icons.png, it's a good idea to allow for more flexibility
            this.iconLocation = offset;
        }
    }

    public enum Cursor {MOUSE, STYLUS, ERASER};
    private Cursor cursor = Cursor.MOUSE;

    // Each cursor uses a different tool.
    private final Map<Cursor, Tool> selected = new EnumMap<>(Cursor.class);
    private final Map<Tool,ToolSettings> toolSettings = new HashMap<>();

    public ToolsetManager(MasterControl master) {
    	this.master = master;
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
        toolSettings.put( Tool.RESHAPER, constructReshapeSettings());
    }

    // ===============
    // ==== Get/Set
    public Cursor getCursor() {
        return cursor;
    }
    public void setCursor( Cursor cursor) {
        this.cursor = cursor;

        triggerToolsetChanged(selected.get(cursor));
    }

    public Tool getSelectedTool() {
        return selected.get(cursor);
    }
    public void setSelectedTool( Tool tool) {
        selected.remove(cursor);
        selected.put(cursor, tool);
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

    public int getToolCount() {
        return Tool.values().length;
    }
    public Tool getNthTool( int n) {
        return Tool.values()[n];
    }

    public ToolSettings getToolSettings(Tool tool) {
        return toolSettings.get(tool);
    }


    /**
     * ToolSettins is an abstract to describe all the settings a particular tool
     * has.  For quick development and modularity purposes, these settings are
     * not inherently hard-coded, but are constructed from an object array scheme
     * (see constructFromScheme for the format) in which strings are used to
     * get a particular property.
     */
    public class ToolSettings {
        Property properties[];
        Tool tool;
        
        ToolSettings( Tool tool) {
        	
        }

        public Property[] getPropertyScheme() {
            Property ret[] = new Property[properties.length];

            for( int i=0; i<properties.length; ++i) {
                ret[i] = properties[i];
            }
            return ret;
        }

        public Property getProperty(String id) {
            for( int i=0; i<properties.length; ++i) {
                if( properties[i].id.equals(id)) {
                    return properties[i];
                }
            }

            return null;
        }
        
        public Object getValue( String id) {
            for( int i=0; i<properties.length; ++i) {
                if( properties[i].id.equals(id)) {
                    return properties[i].getValue();
                }
            }

            return null;
        }

        public void setValue( String id, Object value) {
            for( int i=0; i<properties.length; ++i) {
                if( properties[i].id.equals(id)) {
                    properties[i].setValue(value);
                    triggerToolsetPropertyChanged( this.tool, properties[i]);
                }
            }
        }

        /** Extra Data is data stored with certain PropertyType's, usually
         * StringArrays of Human-Readable formats of the options the property has,
         * stored for constructing the UI Component.*/
//        public Object getExtra(String id) {
//            for( int i=0; i<properties.length; ++i) {
//                if( properties[i].id.equals(id)) {
//                    return properties[i].extra;
//                }
//            }
//            return null;
//        }
    }
    public static abstract class Property
    {
        protected String id;
        protected String hiName;
        protected int mask;

        public String getId() {return id;}
        public String getName() {return hiName;}
        public int getMask() {return mask;}
        
        public abstract Object getValue();
        protected abstract void setValue(Object newValue);
    }

    // =======================
    // ==== Setting Schemes
    private ToolSettings constructPenSettings() {
        Property[] props = {
	        	new OpacityProperty("alpha", "Opacity", 1.0f),
	        	new SizeProperty("width","Width", 5.0f),
	        	new CheckBoxProperty("hard","Hard Edged",false)
        };

        return constructFromScheme(props, Tool.PEN);
    }
    private ToolSettings constructEraseSettings() {
        Property[] props = {
            	new OpacityProperty("alpha", "Opacity", 1.0f),
            	new SizeProperty("width","Width", 5.0f),
            	new CheckBoxProperty("hard","Hard Edged",false)
        };

        return constructFromScheme(props, Tool.ERASER);
    }
    private ToolSettings constructPixelSettings() {
        Property[] props = {
        		new OpacityProperty("alpha", "Opacity", 1.0f)
        };

        return constructFromScheme(props, Tool.PIXEL);
    }
    private ToolSettings constructBoxSelectionSettings() {
        Property[] props = {
        		new DropDownProperty("shape","Shape", 0, new String[]{"Rectangle","Oval"})
        };

        return constructFromScheme(props, Tool.BOX_SELECTION);
    }
    private ToolSettings constructCropperSettings() {
        Property[] props = {
        		new ButtonProperty("cropSelection","Crop Selection", "draw.cropSelection", this.master),
        		new CheckBoxProperty("quickCrop", "Crop on Finish", false),
        		new CheckBoxProperty("shrinkOnly", "Shrink-only Crop", false)
        };
        
        return constructFromScheme(props, Tool.CROP);
    }
    private ToolSettings constructFlipperSettings() {
        Property[] props = {
        		new RadioButtonProperty( "flipMode", "Flip Mode", 2, 
        				new String[] {"Horizontal Flipping", "Vertical Flipping", "Determine from Movement"}),
        };

        return constructFromScheme(props, Tool.FLIPPER);
    }

    private ToolSettings constructColorChangeSettings() {
    	Property[] props = {
    		new DropDownProperty("scope", "Scope", 0, new String[]{"Local","Entire Layer/Group","Entire Project"}),
    		new RadioButtonProperty("mode", "Apply Mode", 0,
                    new String[] {"Check Alpha", "Ignore Alpha", "Change All"})
    	};

        return constructFromScheme( props, Tool.COLOR_CHANGE);
    }
    private ToolSettings constructReshapeSettings() {
    	Property[] props = {
    		new ButtonProperty("cropSelection", "Apply Transform", "draw.applyTransform", master, DISABLE_ON_NO_SELECTION),
    		new DualFloatBoxProperty("scale", "Scale", 1, 1, "x", "y", DISABLE_ON_NO_SELECTION),
    		new DualFloatBoxProperty("translation", "Translation", 0, 0, "x", "y", DISABLE_ON_NO_SELECTION),
    		new FloatBoxProperty( "rotation", "Rotation", 0, DISABLE_ON_NO_SELECTION)
    	};
    	
        return constructFromScheme( props, Tool.RESHAPER);
    }

    public static final int DISABLE_ON_NO_SELECTION = 0x01;

    ToolSettings constructFromScheme( Property[] properties, Tool tool) {
        ToolSettings settings = new ToolSettings(tool);
        settings.properties = properties;
        return settings;
    }

    // ===============
    // ==== Toolset Observer
    public interface MToolsetObserver {
        public void toolsetChanged( Tool newTool);
        public void toolsetPropertyChanged( Tool tool, Property property);
    }

    List<WeakReference<MToolsetObserver>> toolsetObserver = new ArrayList<>();
    public void addToolsetObserver( MToolsetObserver obs) {
        toolsetObserver.add( new WeakReference<ToolsetManager.MToolsetObserver>(obs));
    }
    public void removeToolsetObserver( MToolsetObserver obs) {
        Iterator<WeakReference<MToolsetObserver>> it = toolsetObserver.iterator();
        while( it.hasNext()) {
            MToolsetObserver other = it.next().get();
            if( other == null || other == obs)
                it.remove();
        }
    }

    private void triggerToolsetChanged( Tool newTool) {
        Iterator<WeakReference<MToolsetObserver>> it = toolsetObserver.iterator();
        while( it.hasNext()) {
            MToolsetObserver obs = it.next().get();
            if( obs == null )
                it.remove();
            else
                obs.toolsetChanged(newTool);
        }
    }
    private void triggerToolsetPropertyChanged( Tool tool, Property property) {
        Iterator<WeakReference<MToolsetObserver>> it = toolsetObserver.iterator();
        while( it.hasNext()) {
            MToolsetObserver obs = it.next().get();
            if( obs == null )
                it.remove();
            else
                obs.toolsetPropertyChanged( tool, property);
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
