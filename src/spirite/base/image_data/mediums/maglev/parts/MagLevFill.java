package spirite.base.image_data.mediums.maglev.parts;

import java.util.ArrayList;
import java.util.List;

import spirite.base.brains.tools.ToolSchemes.MagneticFillMode;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.maglev.AMagLevThing;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;

public class MagLevFill extends AMagLevThing {

	public static class StrokeSegment {
		public final int strokeId;
		public final float _pivot;
		public final float _travel;
		public StrokeSegment(int id, float pivot, float travel) 
		{
			this.strokeId = id;
			this._pivot = pivot;
			this._travel = travel;
		}
		public StrokeSegment( MagLevFill.StrokeSegment other) {
			this.strokeId = other.strokeId;
			this._pivot = other._pivot;
			this._travel = other._travel;
		}
	}
	
	public final List<MagLevFill.StrokeSegment> segments;
	public int color;	// Should really be final
	public MagLevFill( List<MagLevFill.StrokeSegment> segments, int color) {
		this.segments = new ArrayList<>(segments.size());
		this.segments.addAll(segments);
		this.color = color;
	}
	public MagLevFill( MagLevFill other) {
		this.segments = new ArrayList<>(other.segments.size());
		this.color = other.color;
		for( int i=0; i < other.segments.size(); ++i)
			this.segments.add( new StrokeSegment(other.segments.get(i)));
	}
	protected MagLevFill _clone()  {return new MagLevFill(this);}
	@Override
	public void draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context) {
		_draw(built, mask, gc, context, false);
	}
	public void _draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context, boolean behind) {
		List<AMagLevThing> things = context.getThings();
		int totalLen = 0;
		int index = 0;
		int thisIndex = things.indexOf(this);

		FloatCompactor outx = new FloatCompactor();
		FloatCompactor outy = new FloatCompactor();

		for( MagLevFill.StrokeSegment s : segments) {
			MagLevStroke stroke = (MagLevStroke)context.getThingById( s.strokeId);
			
			float curveLen = (float) (stroke.direct.length);
			int start = MUtil.clip(0, (int)( s._pivot * curveLen), stroke.direct.length-1);
			int end = MUtil.clip(0, (int) ((s._pivot + s._travel) * curveLen), stroke.direct.length-1);

			for( int c = start; c <= end; ++c) {
				outx.add(stroke.direct.x[c]);
				outy.add(stroke.direct.y[c]);
			}
			for( int c = start; c >= end; --c) {
				outx.add(stroke.direct.x[c]);
				outy.add(stroke.direct.y[c]);
			}
		}
		
		gc.setColor(color);
		if( behind)
			gc.setComposite(Composite.DST_OVER, 1);
		gc.fillPolygon(outx.toArray(), outy.toArray(), outx.size());
	}
	
	// Since MagLevFill operates on references, it doesn't get changed itself
	@Override public float[] getPoints() { return null;}
	@Override public void setPoints(float[] xy) {}
	
	// Specific
	public int getColor() { return color; }
}