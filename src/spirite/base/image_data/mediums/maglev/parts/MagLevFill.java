package spirite.base.image_data.mediums.maglev.parts;

import java.util.ArrayList;
import java.util.List;

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
		public final float start;
		public final float end;
		public StrokeSegment(int id, float start, float end) 
		{
			this.strokeId = id;
			this.start = start;
			this.end = end;
		}
		public StrokeSegment( MagLevFill.StrokeSegment other) {
			this.strokeId = other.strokeId;
			this.start = other.start;
			this.end = other.end;
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
	
	
	
	@Override
	protected MagLevFill _clone()  {return new MagLevFill(this);}
	@Override
	public void draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context) {
		_draw(built, mask, gc, context, false);
	}
	
	/**
	 * 
	 */
	public void _draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context, boolean behind) {
		List<AMagLevThing> things = context.getThings();
		int totalLen = 0;
		int index = 0;
		int thisIndex = things.indexOf(this);

		FloatCompactor outx = new FloatCompactor();
		FloatCompactor outy = new FloatCompactor();

		for( MagLevFill.StrokeSegment s : segments) {
			MagLevStroke stroke = (MagLevStroke)context.getThingById( s.strokeId);
			
			float start = stroke.direct.getNearIndex(s.start);
			float end = stroke.direct.getNearIndex(s.end);
			
			if( end > start) {
				int e = (int)Math.ceil(end);
				for( int c=(int)Math.floor(start); c < e; ++c) {
					outx.add(stroke.direct.x[c]);
					outy.add(stroke.direct.y[c]);
				}
			}
			else {
				int e = (int)Math.floor(end);
				for( int c=(int)Math.ceil(start); c > e; --c) {
					outx.add(stroke.direct.x[c]);
					outy.add(stroke.direct.y[c]);
				}
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