package spirite.base.image_data.images;

import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;

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
