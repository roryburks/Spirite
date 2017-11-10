package spirite.base.image_data.mediums.maglev.parts;

import java.util.ArrayList;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.maglev.MaglevMedium;
import spirite.base.image_data.selection.SelectionMask;

public class MagLevFill extends MagLevThing {

	public static class StrokeSegment {
		public int strokeIndex;
		public int pivot;
		public int travel;
		public StrokeSegment() {}
		public StrokeSegment( MagLevFill.StrokeSegment other) {
			this.strokeIndex = other.strokeIndex;
			this.pivot = other.pivot;
			this.travel = other.travel;
		}
	}
	
	public final List<MagLevFill.StrokeSegment> segments;
	public final int color;
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
	public MagLevFill clone()  {
		return new MagLevFill(this);
	}
	@Override
	public void draw(ABuiltMediumData built, SelectionMask mask, GraphicsContext gc, MaglevMedium context) {
		List<MagLevThing> things = context.getThings();
		int totalLen = 0;
		int index = 0;
		
		for( MagLevFill.StrokeSegment s : segments)
			totalLen += Math.abs(s.travel) + 1;

		float[] outx = new float[totalLen];
		float[] outy = new float[totalLen];

		for( MagLevFill.StrokeSegment s : segments) {
			MagLevStroke stroke = (MagLevStroke)things.get(s.strokeIndex);
			for( int c=0; c <= Math.abs(s.travel); ++c) {
				
				outx[index] = stroke.direct.x[s.pivot + c * ((s.travel > 0)? 1 : -1)];
				outy[index] = stroke.direct.y[s.pivot + c * ((s.travel > 0)? 1 : -1)];
				++index;
			}
		}
		
		gc.setColor(color);
		gc.fillPolygon(outx, outy, outx.length);
		
	}
	
	// Since MagLevFill operates on references, it doesn't get changed itself
	@Override public float[] getPoints() { return null;}
	@Override public void setPoints(float[] xy) {}
	public int getColor() {
		return color;
	}
}