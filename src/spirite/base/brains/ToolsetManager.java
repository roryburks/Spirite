package spirite.base.brains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import spirite.base.brains.MasterControl.CommandExecuter;
import spirite.base.image_data.mediums.drawer.BaseSkeletonDrawer;
import spirite.base.image_data.mediums.drawer.DefaultImageDrawer;
import spirite.base.image_data.mediums.drawer.GroupNodeDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IColorChangeModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IFillModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IFlipModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IMagneticFillModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IStrokeModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IWeightEraserModule;
import spirite.base.image_data.mediums.maglev.MaglevImageDrawer;
import spirite.base.pen.StrokeEngine.Method;
import spirite.base.util.ObserverHandler;
import spirite.hybrid.tools.properties.ButtonProperty;
import spirite.hybrid.tools.properties.CheckBoxProperty;
import spirite.hybrid.tools.properties.DropDownProperty;
import spirite.hybrid.tools.properties.DualFloatBoxProperty;
import spirite.hybrid.tools.properties.FloatBoxProperty;
import spirite.hybrid.tools.properties.OpacityProperty;
import spirite.hybrid.tools.properties.RadioButtonProperty;
import spirite.hybrid.tools.properties.SizeProperty;

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
        PEN("Pen", 0, 0),
        ERASER("Eraser",1, 0),
        FILL("Fill",2, 0),
        BOX_SELECTION("Box Selection",3, 0),
        FREEFORM_SELECTION("Free Selection", 0, 1),
        
        MOVE("Move", 1, 1),
        PIXEL("Pixel", 2, 1),
        CROP("Cropper", 3, 1),
        COMPOSER("Rig Composer",0, 2),
        FLIPPER("Horizontal/Vertical Flipping", 1, 2),
       
        RESHAPER("Reshaping Tool", 2, 2),
        COLOR_CHANGE("Color Change Tool", 3, 2),
        COLOR_PICKER("Color Picker", 0, 3),
        MAGLEV_FILL("Magnetic Fill", 1, 3),
        EXCISE_ERASER("Stroke Erasor", 2, 3),
        
        BONE("Bone Constructor", 4, 0),
        FLOPPYBONE("Bone Deformer", 4, 1),
        PUPPET_BONE("Puppet Bone Composer", 4, 0)
        ;

        public final String description;
        public final int iconX, iconY;
        Tool( String name, int xoffset, int yoffset){
            this.description = name;

            this.iconX = xoffset;
            this.iconY = yoffset;
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

        constructSettings();
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

    public List<Tool> getToolsForDrawer( IImageDrawer drawer) {
    	// Hard-coded ones
    	if( drawer instanceof DefaultImageDrawer)
    		return Arrays.asList(ToolsForDefaultDrawer);
    	if( drawer instanceof GroupNodeDrawer)
    		return Arrays.asList(ToolsForDefaultDrawer);
    	if( drawer instanceof MaglevImageDrawer)
    		return Arrays.asList(ToolsForMaglevDrawer);
    	if( drawer instanceof BaseSkeletonDrawer)
			return Arrays.asList(new Tool[] {Tool.PUPPET_BONE});
//    	if( drawer instanceof GroupNodeDrawer)
//    		return Arrays.asList(ToolsForGroupDrawer);
    	
    	// Dynamically-constructed ones
    	List<Tool> list = new ArrayList<>();
    	
    	if( drawer instanceof IStrokeModule) {
    		if( ((IStrokeModule) drawer).canDoStroke(Method.BASIC))
    			list.add( Tool.PEN);
    		if( ((IStrokeModule) drawer).canDoStroke(Method.ERASE))
        		list.add( Tool.ERASER);
    		if( ((IStrokeModule) drawer).canDoStroke(Method.PIXEL))
        		list.add( Tool.PIXEL);
    	}
    	if( drawer instanceof IFillModule)
    		list.add(Tool.FILL);
    	if( drawer instanceof IFlipModule)
    		list.add(Tool.FLIPPER);
    	if( drawer instanceof IColorChangeModule)
    		list.add(Tool.COLOR_CHANGE);
    	if( drawer instanceof IMagneticFillModule)
    		list.add( Tool.MAGLEV_FILL);
    	if( drawer instanceof IWeightEraserModule)
    		list.add( Tool.EXCISE_ERASER);

    	list.add(Tool.BOX_SELECTION);
    	list.add(Tool.FREEFORM_SELECTION);
    	list.add(Tool.MOVE);
    	list.add(Tool.CROP);
    	list.add(Tool.RESHAPER);
    	
    	list.sort((lhs,rhs) -> {
    		return (lhs.iconX + lhs.iconY*100) - (rhs.iconX + rhs.iconY*100);
    	});
    	
    	return null;
    }
    private static Tool[] ToolsForDefaultDrawer = {
    		Tool.PEN, 
    		Tool.ERASER, 
    		Tool.FILL, 
    		Tool.BOX_SELECTION, 
    		Tool.FREEFORM_SELECTION,
    		Tool.MOVE, 
    		Tool.PIXEL, 
    		Tool.CROP, 
    		Tool.COMPOSER, 
    		Tool.FLIPPER,
    		Tool.RESHAPER, 
    		Tool.COLOR_CHANGE, 
    		Tool.COLOR_PICKER,
    		Tool.MAGLEV_FILL
    };
    private static Tool[] ToolsForGroupDrawer = {
    		Tool.BOX_SELECTION, 
    		Tool.FREEFORM_SELECTION,
    		Tool.MOVE, 
    		Tool.PIXEL, 
    		Tool.CROP, 
    		Tool.COMPOSER, 
    		Tool.FLIPPER,
    		Tool.RESHAPER, 
    		Tool.COLOR_CHANGE, 
    		Tool.COLOR_PICKER,
    		Tool.MAGLEV_FILL
    };
    private static Tool[] ToolsForMaglevDrawer = {
    		Tool.PEN, 
    		Tool.ERASER, 
    		Tool.PIXEL,
    		Tool.MAGLEV_FILL,
    		Tool.EXCISE_ERASER,
    		Tool.BONE,
    		Tool.FLOPPYBONE
    };

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
        	this.tool = tool;
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
    }
    public static abstract class Property
    {
        protected String id;
        protected String hrName;
        protected int mask;

        public String getId() {return id;}
        public String getName() {return hrName;}
        public int getMask() {return mask;}
        
        public abstract Object getValue();
        protected abstract void setValue(Object newValue);
    }

    // =======================
    // ==== Setting Schemes
    public enum PenDrawMode {
    	NORMAL("Normal"),
    	KEEP_ALPHA("Preserve Alpha"),
    	BEHIND("Behind");
    	
    	public final String hrName;
    	PenDrawMode( String hrName) {this.hrName = hrName;}
    	@Override public String toString() {return hrName;}
    }
    public enum BoxSelectionShape {
    	RECTANGLE("Rectangle"),
    	OVAL("Oval"),
    	;
    	
    	public final String hrName;
    	BoxSelectionShape( String hrName) {this.hrName = hrName;}
    	@Override public String toString() {return hrName;}
    }
    public enum ColorChangeScopes {
    	LOCAL("Local"),
    	GROUP("Entire Layer/Group"),
    	PROJECT("Entire Project")
    	;
    	
    	public final String hrName;
    	ColorChangeScopes( String hrName) {this.hrName = hrName;}
    	@Override public String toString() {return hrName;}
    }
    private void constructSettings() {
        toolSettings.put( Tool.PEN, constructFromScheme( new Property[] {
	        	new DropDownProperty<PenDrawMode>("mode", "Draw Mode", PenDrawMode.NORMAL, PenDrawMode.class),
	        	new OpacityProperty("alpha", "Opacity", 1.0f),
	        	new SizeProperty("width","Width", 5.0f),
	        	new CheckBoxProperty("hard","Hard Edged",false),
        }, Tool.PEN));
        toolSettings.put( Tool.PIXEL, constructFromScheme( new Property[] {
        		new OpacityProperty("alpha", "Opacity", 1.0f)
        }, Tool.PIXEL));
        toolSettings.put( Tool.ERASER, constructFromScheme( new Property[] {
            	new OpacityProperty("alpha", "Opacity", 1.0f),
            	new SizeProperty("width","Width", 5.0f),
            	new CheckBoxProperty("hard","Hard Edged",false)
        }, Tool.ERASER));
        toolSettings.put( Tool.BOX_SELECTION, constructFromScheme( new Property[] {
        		new DropDownProperty<BoxSelectionShape>("shape","Shape", BoxSelectionShape.RECTANGLE, BoxSelectionShape.class)
        }, Tool.BOX_SELECTION));
        toolSettings.put( Tool.CROP, constructFromScheme( new Property[] {
        		new ButtonProperty("cropSelection","Crop Selection", "draw.cropSelection", this.master),
        		new CheckBoxProperty("quickCrop", "Crop on Finish", false),
        		new CheckBoxProperty("shrinkOnly", "Shrink-only Crop", false)
        }, Tool.CROP));
        toolSettings.put( Tool.FLIPPER, constructFromScheme( new Property[] {
        		new RadioButtonProperty( "flipMode", "Flip Mode", 2, 
        				new String[] {"Horizontal Flipping", "Vertical Flipping", "Determine from Movement"}),
        }, Tool.FLIPPER));
        toolSettings.put( Tool.COLOR_CHANGE, constructFromScheme( new Property[] {
        		new DropDownProperty<ColorChangeScopes>("scope", "Scope", ColorChangeScopes.LOCAL, ColorChangeScopes.class),
        		new RadioButtonProperty("mode", "Apply Mode", 0,
                        new String[] {"Check Alpha", "Ignore Alpha", "Change All"})
        }, Tool.COLOR_CHANGE));
        toolSettings.put( Tool.RESHAPER, constructFromScheme( new Property[] {
        		new ButtonProperty("cropSelection", "Apply Transform", "draw.applyTransform", master, DISABLE_ON_NO_SELECTION),
        		new DualFloatBoxProperty("scale", "Scale", 1, 1, "x", "y", DISABLE_ON_NO_SELECTION),
        		new DualFloatBoxProperty("translation", "Translation", 0, 0, "x", "y", DISABLE_ON_NO_SELECTION),
        		new FloatBoxProperty( "rotation", "Rotation", 0, DISABLE_ON_NO_SELECTION)
        }, Tool.RESHAPER));
        toolSettings.put( Tool.EXCISE_ERASER, constructFromScheme( new Property[]{
        		new SizeProperty("width", "Width", 5.0f),
        		new CheckBoxProperty("full", "Full Erase", true)
        }, Tool.EXCISE_ERASER));
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
    private final ObserverHandler<MToolsetObserver> toolsetObs = new ObserverHandler<>();
    public void addToolsetObserver( MToolsetObserver obs) { toolsetObs.addObserver(obs);}
    public void removeToolsetObserver( MToolsetObserver obs) {toolsetObs.removeObserver(obs);}

    private void triggerToolsetChanged( Tool newTool) { 
    	toolsetObs.trigger((MToolsetObserver obs)-> {obs.toolsetChanged(newTool);});

    }
    private void triggerToolsetPropertyChanged( Tool tool, Property property) {
    	toolsetObs.trigger((MToolsetObserver obs)-> {obs.toolsetPropertyChanged(tool, property);});
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
    public boolean executeCommand(String commmand, Object extra) {
        return setSelectedTool(commmand);
    }

}
