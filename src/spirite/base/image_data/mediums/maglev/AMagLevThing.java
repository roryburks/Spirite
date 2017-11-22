package spirite.base.image_data.mediums.maglev;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.selection.SelectionMask;

public abstract class AMagLevThing {
	int id;
	
	public int getId() {return id;}
	
	public abstract float[] getPoints();
	public abstract void setPoints(float[] xy);
	public abstract void draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context );
	public final AMagLevThing clone() {
		AMagLevThing other = _clone();
		other.id = id;
		return other;
	}
	protected abstract AMagLevThing _clone();
}