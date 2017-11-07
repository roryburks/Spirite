package spirite.base.image_data.selection;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;

public abstract class ALiftedSelection {
	ALiftedSelection() {}
	
	public abstract void drawLiftedData( GraphicsContext gc);
	public abstract int getWidth();
	public abstract int getHeight();
	public abstract ALiftedSelection asType( Class<? extends ALiftedSelection> tclass);
	public abstract IImage readonlyAccess();
}
