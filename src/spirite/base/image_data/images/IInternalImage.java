package spirite.base.image_data.images;

import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;

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
	public IBuiltImageData build( BuildingImageData building);
	public IInternalImage dupe();
	public void flush();
	public RawImage readOnlyAccess();
	public InternalImageTypes getType();
	
	public static enum InternalImageTypes {
		NORMAL,		// 0
		DYNAMIC,	// 1
		PRISMATIC	// 2
	}
}
