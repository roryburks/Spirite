package spirite.base.image_data.mediums.maglev.parts;

import java.util.ArrayList;
import java.util.List;

import spirite.base.brains.tools.ToolSchemes.MagneticFillMode;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;

public class MagLevFill extends AMagLevThing {

	public static class StrokeSegment {
		public final int strokeIndex;
		public final float _pivot;
		public final float _travel;
		public StrokeSegment(int index, float pivot, float travel) 
		{
			this.strokeIndex = index;
			this._pivot = pivot;
			this._travel = travel;
		}
		public StrokeSegment( MagLevFill.StrokeSegment other) {
			this.strokeIndex = other.strokeIndex;
			this._pivot = other._pivot;
			this._travel = other._travel;
		}
	}
	
	public final List<MagLevFill.StrokeSegment> segments;
	public int color;	// Should really be final
	public final MagneticFillMode mode;
	public MagLevFill( List<MagLevFill.StrokeSegment> segments, int color, MagneticFillMode mode) {
		this.segments = new ArrayList<>(segments.size());
		this.segments.addAll(segments);
		this.color = color;
		this.mode = mode;
	}
	public MagLevFill( MagLevFill other) {
		this.segments = new ArrayList<>(other.segments.size());
		this.color = other.color;
		this.mode = other.mode;
		for( int i=0; i < other.segments.size(); ++i)
			this.segments.add( new StrokeSegment(other.segments.get(i)));
	}
	public MagLevFill clone()  {
		return new MagLevFill(this);
	}
	@Override
	public void draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context) {
		List<AMagLevThing> things = context.getThings();
		int totalLen = 0;
		int index = 0;

		FloatCompactor outx = new FloatCompactor();
		FloatCompactor outy = new FloatCompactor();

		for( MagLevFill.StrokeSegment s : segments) {
			MagLevStroke stroke = (MagLevStroke)things.get(s.strokeIndex);
			
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
		if( mode == mode.BEHIND)
			gc.setComposite(Composite.DST_OVER, 1);
		gc.fillPolygon(outx.toArray(), outy.toArray(), outx.size());
		
	}
	
	// Since MagLevFill operates on references, it doesn't get changed itself
	@Override public float[] getPoints() { return null;}
	@Override public void setPoints(float[] xy) {}
	public int getColor() { return color; }
}