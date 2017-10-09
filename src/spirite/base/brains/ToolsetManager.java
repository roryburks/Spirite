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

/**
 * The ToolsetManager manages the set of Tools available, which tool is
 * selected, and what the settings for each tool is.
 *
 * @author Rory Burks
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

        /** Extra Data is data stored with certain PropertyType's, usually
         * StringArrays of Human-Readable formats of the options the property has,
         * stored for constructing the UI Component.*/
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
        // RADIO_BUTTON is a special Property Type in that contains an finite
        //	amount of on-off options, only one of which can be active at a given
        //	time.
        RADIO_BUTTON,
        // DropDown
        DROP_DOWN, 
        // Input Boxes
        DUAL_FLOAT_BOX, FLOAT_BOX
    }

    // =======================
    // ==== Setting Schemes
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
                {"mode", PropertyType.RADIO_BUTTON, "Apply Mode", 0, 0,
                        new String[] {"Check Alpha", "Ignore Alpha", "Change All"}
                },
        };

        return constructFromScheme(scheme);
    }
    private ToolSettings constructReshapeSettings() {
        final Object[][] scheme = {
                {"cropSelection", PropertyType.BUTTON, "Apply Transform", "draw.applyTransform",  DISABLE_ON_NO_SELECTION},
                {"scale", PropertyType.DUAL_FLOAT_BOX, "Scale", new Vec2(1,1), DISABLE_ON_NO_SELECTION, new String[] {"x","y"}},
                {"translation", PropertyType.DUAL_FLOAT_BOX, "Translation", new Vec2(0,0), DISABLE_ON_NO_SELECTION, new String[] {"x","y"}},
                {"rotation", PropertyType.FLOAT_BOX, "Rotation", (float)0, DISABLE_ON_NO_SELECTION},
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

    /** This method exists mostly for development purposes, it verifies that
     * the value you are trying to set a property to is the type that it
     * should be, so that the proper error can be Managed inside this class. */
    private Class<?> getValueClassFromType( PropertyType type) {
        switch( type) {
        case SIZE:
        case OPACITY:
            return Float.class;
        case BUTTON:
            return String.class;
        case CHECK_BOX:
            return Boolean.class;
        case DROP_DOWN:
        case RADIO_BUTTON:
            return Integer.class;
		case DUAL_FLOAT_BOX:
			return Vec2.class;
		case FLOAT_BOX:
			return Float.class;
        }

        return null;	// Should be no way to reach this
    }

    // ===============
    // ==== Toolset Observer
    public interface MToolsetObserver {
        public void toolsetChanged( Tool newTool);
//        public void toolsetPropertyChanged( Tool tool);
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
