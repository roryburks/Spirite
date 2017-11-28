package spirite.base.image_data.selection;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;

public abstract class ALiftedData {
	public abstract void drawLiftedData( GraphicsContext gc);
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract IImage readonlyAccess();
}
