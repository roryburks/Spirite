package spirite.base.image_data.images;

import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.images.drawer.IImageDrawer;

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
public interface IInternalImage {
	public int getWidth();
	public int getHeight();
	public int getDynamicX();
	public int getDynamicY();
	public ABuiltImageData build( BuildingImageData building);
	public IInternalImage dupe();
	public void flush();
	public RawImage readOnlyAccess();
	public InternalImageTypes getType();
	public IImageDrawer getImageDrawer(BuildingImageData building);
	
	public static enum InternalImageTypes {
		NORMAL(0),
		DYNAMIC(1),
		PRISMATIC(2),
		MAGLEV(3),
		;
		
		// This way, these values can be used in saving and loading without failing when
		//	an Enum is removed
		public final int permanentCode;
		InternalImageTypes( int i) {this.permanentCode = i;}
		
		public InternalImageTypes fromCode( int code) {
			InternalImageTypes[] values = InternalImageTypes.values();
			for( int i=0; i < values.length; ++i )
				if( values[i].permanentCode == code)
					return values[i];
				
			return null;
		}
	}

}
