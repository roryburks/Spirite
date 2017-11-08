package spirite.base.image_data.selection;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.util.glmath.MatTrans;

public abstract class ALiftedData {
	public abstract void drawLiftedData( GraphicsContext gc);
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract ALiftedData asType( Class<? extends ALiftedData> tclass);
	public abstract IImage readonlyAccess();
}
