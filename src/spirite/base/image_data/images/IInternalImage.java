package spirite.base.image_data.images;

import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageHandle;

public interface IInternalImage {
	public int getWidth();
	public int getHeight();
	public IBuiltImageData build( ImageHandle handle, int ox, int oy);
	public IInternalImage dupe();
	public void flush();
	public RawImage readOnlyAccess();
}
