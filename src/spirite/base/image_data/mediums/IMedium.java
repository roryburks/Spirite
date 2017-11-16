package spirite.base.image_data.mediums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.graphics.IImage;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.mediums.drawer.IImageDrawer;

/**
 * IInternalImages are a form of base data type that serves as an intermediate between
 * Layers and RawImages/CachedImages or other primary data points.
 * 
 * For now all IInternalImages have the same basic functionality of wrapping one or
 * more RawImage and piping them up to the image-modifying functions based on structure,
 * but in the future it's possible that IInternalImages might be made of completely
 * different kind of things such as Vector-based image data, though this would require
 * substantial modifications to the GraphicsContext Wrapper (or perhaps another such
 * object for wrapping various drawing tool functionality).
 * 
 * @author Rory Burks
 *
 */
public interface IMedium {
	public int getWidth();
	public int getHeight();
	public int getDynamicX();
	public int getDynamicY();
	public ABuiltMediumData build( BuildingMediumData building);
	public IMedium dupe();
	public IMedium copyForSaving();	// Probably not best to offload this work to individual
											// internal image types, but it's the least immediate work
	public void flush();
	public IImage readOnlyAccess();
	public InternalImageTypes getType();
	public IImageDrawer getImageDrawer(BuildingMediumData building);
	
	public static enum InternalImageTypes {
		NORMAL(0),
		DYNAMIC(1),
		PRISMATIC(2),
		MAGLEV(3),
		DERIVED_MAGLEV(4, false),
		;
		
		// This way, these values can be used in saving and loading without failing when
		//	an Enum is removed
		public final int permanentCode;
		public final boolean userCreateable;
		InternalImageTypes( int i) {
			this(i, true);
		}
		InternalImageTypes( int i, boolean createable) {
			this.permanentCode = i;
			this.userCreateable = createable;
		}
		
		public InternalImageTypes fromCode( int code) {
			InternalImageTypes[] values = InternalImageTypes.values();
			for( int i=0; i < values.length; ++i )
				if( values[i].permanentCode == code)
					return values[i];
				
			return null;
		}
		private static InternalImageTypes[] _createableTypes = null;

		public static InternalImageTypes[] createableTypes() {
			if( _createableTypes == null) {
				List<InternalImageTypes> createables = new ArrayList<>(InternalImageTypes.values().length);
				createables.addAll(Arrays.asList(InternalImageTypes.values()));
				createables.removeIf((type) -> !type.userCreateable);
				_createableTypes = createables.toArray(new InternalImageTypes[0]);
			}
			
			return _createableTypes;
		}
	}

}
