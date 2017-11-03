package spirite.base.image_data.mediums.maglev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.IMedium;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.DrawPoints;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.MUtil;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.MatTrans.NoninvertableException;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2;
import spirite.base.util.glmath.Vec2i;
import spirite.base.util.interpolation.CubicSplineInterpolator2D;
import spirite.base.util.interpolation.Interpolator2D;
import spirite.hybrid.HybridHelper;

/**
 * A Maglev Internal Image is an image that floats just above the surface, 
 * not quite planting its feat in the ground.  Essentially it is a kind of 
 * Scalable Vector Image, storing all the different stroke and fill actions
 * as logical vertex data rather than rendered pixel data, allowing them to
 * be scaled/rotated without generation loss	.
 */
public class MaglevMedium implements IMedium {
	//final List<MagLevStroke> strokes;
	//final List<MagLevFill> fills;
	final List<MagLevThing> things;
	
	private final ImageWorkspace context;
	RawImage builtImage = null;
	private boolean isBuilt = false;
	
	public MaglevMedium( ImageWorkspace context) {
		this.context = context;
		this.things =  new ArrayList<>();
		if( context != null) {
			// TODO: unflag "isBuilt" when Workspace changes size
		}
	}
	public MaglevMedium( ImageWorkspace context, List<MagLevThing> things) {
		this.context = context;
		this.things = new ArrayList<>( things.size());
		this.things.addAll(things);
	}
	private MaglevMedium( MaglevMedium other) {
		this.context = other.context;
		this.things = new ArrayList<>(other.things.size());
		
		for( MagLevThing thing : other.things)
			this.things.add(thing.clone());
		
		this.isBuilt = other.isBuilt;
		this.builtImage = (other.builtImage == null) ? null : other.builtImage.deepCopy();
	}
	
	public void boneConform( float x1, float y1, float x2, float y2, Interpolator2D to) {
		Vec2 b = new Vec2(x2-x1, y2-y1);
		float scale_b = b.getMag();
		float scale_b_s = scale_b*scale_b;
		
		for( MagLevThing thing : things) {
			float[] toTransform = thing.getPoints();
			if( toTransform == null) continue;
			
			for( int i=0; i < toTransform.length; i += 2) {
				Vec2 a = new Vec2(toTransform[i]-x1, toTransform[i+1]-y1);
				
				float a1 = a.getMag();
				Vec2 a2 = a.sub(b.scalar(a.dot(b)/b.dot(b)));
			}
			
		}
	}
	
	void addThing( MagLevThing thing) {
		things.add(thing);
	}
	void popThing() {
		things.remove(things.size()-1);
	}
		
	public static abstract class MagLevThing {
		abstract float[] getPoints();
		abstract void setPoints(float[] xy);
		abstract void draw(ABuiltMediumData built, BuiltSelection mask, GraphicsContext gc, MaglevMedium context );
		protected abstract MagLevThing clone();
	}
	public static class MagLevStroke extends MagLevThing {
		public final PenState[] states;
		public final StrokeParams params;
		DrawPoints direct;
		
		public MagLevStroke( PenState[] states, StrokeParams params) {
			this.states = states;
			this.params = params;
			
			Interpolator2D interpolator;
			switch( params.getInterpolationMethod()){
			case CUBIC_SPLINE:
				interpolator = new CubicSplineInterpolator2D(null, true);
				break;
			default:
				interpolator = null;
				break;
			}
			if( interpolator != null) {
				for( PenState ps : states)
					interpolator.addPoint(ps.x, ps.y);
			}
			direct = StrokeEngine.buildPoints(interpolator, Arrays.asList(states), params);
		}
		
		protected MagLevStroke clone() {
			PenState[] newStates = new PenState[states.length];
			for( int i=0; i < states.length; ++i) {
				newStates[i] = new PenState( states[i]);
			}
			
			return new MagLevStroke(newStates, params);
		}

		@Override
		void draw(ABuiltMediumData built, BuiltSelection mask, GraphicsContext gc, MaglevMedium context) {
			StrokeEngine _engine = context.context.getSettingsManager().getDefaultDrawer().getStrokeEngine();
			_engine.batchDraw(params, states, built, mask);
		}

		@Override
		float[] getPoints() {
			float[] data = new float[states.length*2];
			for( int i=0; i<states.length;++i) {
				data[i*2] = states[i].x;
				data[i*2+1] = states[i].y;
			}
			return data;
		}

		@Override
		void setPoints(float[] xy) {
			for( int i=0; i<states.length; ++i) {
				states[i].x = xy[i*2];
				states[i].y = xy[i*2+1];
			}
			Interpolator2D interpolator;
			switch( params.getInterpolationMethod()){
			case CUBIC_SPLINE:
				interpolator = new CubicSplineInterpolator2D(null, true);
				break;
			default:
				interpolator = null;
				break;
			}
			if( interpolator != null) {
				for( PenState ps : states)
					interpolator.addPoint(ps.x, ps.y);
			}
			direct = StrokeEngine.buildPoints(interpolator, Arrays.asList(states), params);
		}
	}
	public static class MagLevFill extends MagLevThing {

		public static class StrokeSegment {
			public int strokeIndex;
			public int pivot;
			public int travel;
			public StrokeSegment() {}
			public StrokeSegment( StrokeSegment other) {
				this.strokeIndex = other.strokeIndex;
				this.pivot = other.pivot;
				this.travel = other.travel;
			}
		}
		
		public final List<StrokeSegment> segments;
		public final int color;
		public MagLevFill( List<StrokeSegment> segments, int color) {
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
		protected MagLevFill clone()  {
			return new MagLevFill(this);
		}
		@Override
		void draw(ABuiltMediumData built, BuiltSelection mask, GraphicsContext gc, MaglevMedium context) {
			int totalLen = 0;
			for( StrokeSegment s : segments)
				totalLen += Math.abs(s.travel) + 1;

			float[] outx = new float[totalLen];
			float[] outy = new float[totalLen];
			int index = 0;
			for( StrokeSegment s : segments) {
				MagLevStroke stroke = (MagLevStroke)context.things.get(s.strokeIndex);
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
		@Override float[] getPoints() { return null;}
		@Override void setPoints(float[] xy) {}
		public int getColor() {
			return color;
		}
	}

	// ==== Easy Junk
	@Override public int getWidth() {return context.getWidth(); }
	@Override public int getHeight() {return context.getHeight();}

	@Override public int getDynamicX() { return 0; }
	@Override public int getDynamicY() { return 0; }

	@Override public ABuiltMediumData build(BuildingMediumData building) {return new MaglevBuiltImageData(building);}
	@Override public IImageDrawer getImageDrawer(BuildingMediumData building) 
		{return new MaglevImageDrawer(this, building);}

	@Override public IMedium dupe() {return new MaglevMedium(this);}
	@Override public IMedium copyForSaving() {return new MaglevMedium(this);}
	@Override public InternalImageTypes getType() {return InternalImageTypes.MAGLEV;}

	@Override public IImage readOnlyAccess() {
		Build();
		return builtImage;
	}
	
	@Override
	public void flush() {
		if( builtImage != null)
			builtImage.flush();
		builtImage = null;
		isBuilt = false;
		//this.strokes.clear();
	}


	// ==== Hard Junk
	boolean building = false;
	void Build() {
		if( !building) {
			building = true;
			if( !isBuilt) {
				builtImage = HybridHelper.createImage(getWidth(), getHeight());
				GraphicsContext gc = builtImage.getGraphics();
	
				ABuiltMediumData built = this.build(new BuildingMediumData(context.getHandleFor(this), 0, 0));
				BuiltSelection mask = new BuiltSelection(null, 0, 0);
				for( MagLevThing thing : things) {
					thing.draw( built, mask, gc, this);
				}
			}
			isBuilt = true;
			building = false;
		}
	}
	void unbuild() {
		if( this.isBuilt) {
			this.isBuilt = false;
			builtImage.flush();
			builtImage = null;
		}
	}
	/** Be careful with these things, they can break. */
	public List<MagLevThing> getThings() {
		return new ArrayList<MagLevThing>(things);
	}
	

	public class MaglevBuiltImageData extends ABuiltMediumData {
		MatTrans trans;
		//final int box;
		//final int boy;
		
		public MaglevBuiltImageData(BuildingMediumData building) 
		{
			super(building.handle);
			this.trans = building.trans;
		}

		@Override public int getWidth() {return context.getWidth(); }
		@Override public int getHeight() {return context.getHeight();}

		@Override public Vec2i convert(Vec2i p) {return p;}
		@Override public Vec2 convert(Vec2 p) {return p;}

		@Override public Rect getBounds() 
		{
			// TODO
			return new Rect( (int)trans.getM02(), (int)trans.getM12(), context.getWidth(), context.getHeight());
		}
		@Override public void draw(GraphicsContext gc) 
		{
			MatTrans oldTrans = gc.getTransform();
			gc.preTransform(trans);
			gc.drawImage(builtImage, 0, 0);
			gc.setTransform(oldTrans);
		}
		@Override public void drawBorder(GraphicsContext gc) 
		{
			MatTrans oldTrans = gc.getTransform();
			gc.preTransform(trans);
			gc.drawRect(0, 0, context.getWidth(), context.getHeight());
			gc.setTransform(oldTrans);
		}
		@Override public MatTrans getCompositeTransform() 
			{return new MatTrans(trans);}
		@Override public MatTrans getScreenToImageTransform() 
		{
			try {
				return trans.createInverse();
			} catch (NoninvertableException e) {
				return new MatTrans();
			}
		}

		// Counter-intuitively, checking in and checking out of a MaglevInternalImage
		//	can be a thing that makes sense to do as the StrokeEngine uses it for the
		//	behavior we want.
		@Override public GraphicsContext checkout() { return checkoutRaw().getGraphics();}
		@Override public void checkin() { handle.refresh();}
		@Override public RawImage checkoutRaw() {
			Build();
			return builtImage;
		}
	}
}
