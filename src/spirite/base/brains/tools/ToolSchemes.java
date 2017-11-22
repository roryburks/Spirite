package spirite.base.brains.tools;

import java.util.HashMap;
import java.util.Map;

import spirite.base.brains.MasterControl;
import spirite.base.brains.ToolsetManager.Property;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.hybrid.tools.properties.ButtonProperty;
import spirite.hybrid.tools.properties.CheckBoxProperty;
import spirite.hybrid.tools.properties.DropDownProperty;
import spirite.hybrid.tools.properties.DualFloatBoxProperty;
import spirite.hybrid.tools.properties.FloatBoxProperty;
import spirite.hybrid.tools.properties.SliderProperty;
import spirite.hybrid.tools.properties.RadioButtonProperty;
import spirite.hybrid.tools.properties.SizeProperty;

public class ToolSchemes {
	// Property BitMask bits
    public static final int DISABLE_ON_NO_SELECTION = 0x01;
	
	
	// Note: This scheme could really also have a "get Tool.X" method, but that would
	//	make it non-functional
	public static interface ToolScheme {
		public Property[] getScheme(MasterControl master);
	}

	public enum PenDrawMode {
		NORMAL("Normal"),
		KEEP_ALPHA("Preserve Alpha"),
		BEHIND("Behind");
		
		public final String hrName;
		PenDrawMode( String hrName) {this.hrName = hrName;}
		@Override public String toString() {return hrName;}
	}
	
	public static ToolScheme Pen = (m) -> new Property[] {
        	new DropDownProperty<PenDrawMode>("mode", "Draw Mode", PenDrawMode.NORMAL, PenDrawMode.class),
        	new SliderProperty("alpha", "Opacity", 1.0f),
        	new SizeProperty("width","Width", 5.0f),
        	new CheckBoxProperty("hard","Hard Edged",false),
    };
	public static ToolScheme Pixel = (m) -> new Property[] {
    		new SliderProperty("alpha", "Opacity", 1.0f)
    };
	public static ToolScheme Eraser = (m) ->  new Property[] {
        	new SliderProperty("alpha", "Opacity", 1.0f),
        	new SizeProperty("width","Width", 5.0f),
        	new CheckBoxProperty("hard","Hard Edged",false)
    };

	public enum BoxSelectionShape {
		RECTANGLE("Rectangle"),
		OVAL("Oval"),
		;
		
		public final String hrName;
		BoxSelectionShape( String hrName) {this.hrName = hrName;}
		@Override public String toString() {return hrName;}
	}
	public static ToolScheme BoxSelection  = (m) ->  new Property[] {
    		new DropDownProperty<BoxSelectionShape>("shape","Shape", BoxSelectionShape.RECTANGLE, BoxSelectionShape.class)
    };
	public static ToolScheme Crop = (master) ->  new Property[] {
    		new ButtonProperty("cropSelection","Crop Selection", "draw.cropSelection", master),
    		new CheckBoxProperty("quickCrop", "Crop on Finish", false),
    		new CheckBoxProperty("shrinkOnly", "Shrink-only Crop", false)
    };
	

	public enum FlipMode {
		HORIZONTAL("Horizontal Flipping"),
		VERTICAL( "Vertical Flipping"),
		BY_MOVEMENT("Determine from Movement"),
		;
	
		public final String hrName;
		FlipMode( String hrName) {this.hrName = hrName;}
		@Override public String toString() {return hrName;}
	}
	public static ToolScheme Flipper = (m) -> new Property[] {
    		new RadioButtonProperty<FlipMode>( "flipMode", "Flip Mode", FlipMode.BY_MOVEMENT, FlipMode.class),
    };

	public enum ColorChangeScopes {
		LOCAL("Local"),
		GROUP("Entire Layer/Group"),
		PROJECT("Entire Project")
		;
		
		public final String hrName;
		ColorChangeScopes( String hrName) {this.hrName = hrName;}
		@Override public String toString() {return hrName;}
	}
	public enum ColorChangeMode {
		CHECK_ALL("Check Alpha", 0),
		IGNORE_ALPHA("Ignore Alpha", 1),
		AUTO("Change All", 2)
		;
	
		public final String hrName;
		public final int shaderCode;	// A code passed to the shader/algorithm that implements it
		ColorChangeMode( String hrName, int code) {
			this.hrName = hrName;
			this.shaderCode = code;
		}
		@Override public String toString() {return hrName;}
	}
	public static ToolScheme ColorChange  = (m) ->  new Property[] {
    		new DropDownProperty<ColorChangeScopes>("scope", "Scope", ColorChangeScopes.LOCAL, ColorChangeScopes.class),
    		new RadioButtonProperty<ColorChangeMode>("mode", "Apply Mode", ColorChangeMode.CHECK_ALL, ColorChangeMode.class)
    };

	public static ToolScheme Reshaper = (master) -> new Property[] {
    		new ButtonProperty("cropSelection", "Apply Transform", "draw.applyTransform", master, DISABLE_ON_NO_SELECTION),
    		new DualFloatBoxProperty("scale", "Scale", 1, 1, "x", "y", DISABLE_ON_NO_SELECTION),
    		new DualFloatBoxProperty("translation", "Translation", 0, 0, "x", "y", DISABLE_ON_NO_SELECTION),
    		new FloatBoxProperty( "rotation", "Rotation", 0, DISABLE_ON_NO_SELECTION)
    };
	
	public static ToolScheme ExciseEraser = (m) -> new Property[]{
    		new SizeProperty("width", "Width", 5.0f),
    		new CheckBoxProperty("precise", "Precise Erase", true)
    };
	public static ToolScheme Bone = (master) -> new Property[]{
			new CheckBoxProperty("resize", "Resize", false),
    		//new ButtonProperty("resize", "Resize", null, master),
    		new SliderProperty("leniency", "Resize Leniency", 0.1f, 0, 1),
    		new DropDownProperty<ToolSchemes.BoneStretchMode>("mode", "Resize Mode", ToolSchemes.BoneStretchMode.SCALE, ToolSchemes.BoneStretchMode.class),
    		new ButtonProperty("do", "Do Bone Transform", null, master),
    		new CheckBoxProperty("preview", "Preview", false)
    };

	public enum MagneticFillMode {
		NORMAL("Normal", 0),
		BEHIND("Behind", 1),
		;
	
		public final String hrName;
		public final int fileId;
		MagneticFillMode( String hrName, int fileId) {
			this.hrName = hrName;
			this.fileId = fileId;
		}
		@Override public String toString() {return hrName;}
		public static MagneticFillMode fromFileId(int fileId) {
			for( MagneticFillMode mode : MagneticFillMode.values())
				if( mode.fileId == fileId)
					return mode;
				
			return null;
		}
	}
	public static ToolScheme MagneticFill = (m) -> new Property[] {
			new DropDownProperty<MagneticFillMode>("mode", "Fill Mode", MagneticFillMode.NORMAL, MagneticFillMode.class)
	};
	
	

	public static Map<Tool,ToolScheme> getToolSchemes() {
		Map<Tool,ToolScheme> map = new HashMap<>();
		
		map.put(Tool.PEN, Pen);
		map.put(Tool.PIXEL, Pixel);
		map.put(Tool.ERASER, Eraser);
		map.put(Tool.BOX_SELECTION, BoxSelection);
		map.put(Tool.CROP, Crop);
		map.put(Tool.FLIPPER, Flipper);
		map.put(Tool.COLOR_CHANGE, ColorChange);
		map.put(Tool.RESHAPER, Reshaper);
		map.put(Tool.EXCISE_ERASER, ExciseEraser);
		map.put(Tool.BONE, Bone);
		map.put(Tool.MAGLEV_FILL, MagneticFill);
		
		return map;
	}

	public enum BoneStretchMode {
		SCALE("Scale"),
		CLIP_HEAD("Clip Head"),
		CLIP_EVEN("Clamp"),
		SCALE_TO_BONE("Scale perpendicular to bone."),
		INTELI("Scale LoA")
		;
	
		public final String hrName;
		BoneStretchMode( String hrName) {this.hrName = hrName;}
		@Override public String toString() {return hrName;}
	}
}
