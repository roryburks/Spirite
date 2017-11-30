package spirite.base.image_data.mediums.maglev;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.Vec2;

public abstract class AMagLevThing {
	int id = -1;

	public AMagLevThing() {}
	public AMagLevThing(int id) {this.id = id;}
	
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
	
	public void transform(MatTrans trans) {
		float[] points = getPoints();
		
		if( points == null)return;
		
		for( int i=0; i < points.length; i += 2) {
			Vec2 to = trans.transform(new Vec2(points[i],points[i+1]));
			points[i] = to.x;
			points[i+1] = to.y;
		}
		setPoints(points);
	}
}