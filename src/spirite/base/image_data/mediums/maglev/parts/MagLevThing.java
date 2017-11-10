package spirite.base.image_data.mediums.maglev.parts;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.selection.SelectionMask;

public abstract class MagLevThing {
	public abstract float[] getPoints();
	public abstract void setPoints(float[] xy);
	public abstract void draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context );
	public abstract MagLevThing clone();
}