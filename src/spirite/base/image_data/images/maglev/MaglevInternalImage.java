package spirite.base.image_data.images.maglev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingImageData;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.images.ABuiltImageData;
import spirite.base.image_data.images.IInternalImage;
import spirite.base.image_data.images.drawer.IImageDrawer;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.pen.StrokeEngine.DrawPoints;
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
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
public class MaglevInternalImage implements IInternalImage {
	//final List<MagLevStroke> strokes;
	//final List<MagLevFill> fills;
	final List<MagLevThing> things;
	
	private final ImageWorkspace context;
	RawImage builtImage = null;
	private boolean isBuilt = false;
	
	public MaglevInternalImage( ImageWorkspace context) {
		this.context = context;
		this.things =  new ArrayList<>();
		if( context != null) {
			// TODO: unflag "isBuilt" when Workspace changes size
		}
	}
	public MaglevInternalImage( ImageWorkspace context, List<MagLevThing> things) {
		this.context = context;
		this.things = new ArrayList<>( things.size());
		this.things.addAll(things);
	}
	private MaglevInternalImage( MaglevInternalImage other) {
		this.context = other.context;
		this.things = new ArrayList<>(other.things.size());
		
		for( MagLevThing thing : other.things)
			this.things.add(thing.clone());
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
		abstract void draw(ABuiltImageData built, BuiltSelection mask, GraphicsContext gc, MaglevInternalImage context );
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
		void draw(ABuiltImageData built, BuiltSelection mask, GraphicsContext gc, MaglevInternalImage context) {
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
		int color;
		public MagLevFill( List<StrokeSegment> segments, int color) {
			this.segments = new ArrayList<>(segments.size());
			this.segments.addAll(segments);
			this.color = color;
		}
		public MagLevFill( MagLevFill other) {
			this.segments = new ArrayList<>(other.segments.size());
			this.color = other.color;
			for( int i=0; i < segments.size(); ++i)
				this.segments.add( new StrokeSegment(other.segments.get(i)));
		}
		protected MagLevFill clone()  {
			return new MagLevFill(this);
		}
		@Override
		void draw(ABuiltImageData built, BuiltSelection mask, GraphicsContext gc, MaglevInternalImage context) {
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

	@Override public ABuiltImageData build(BuildingImageData building) {return new MaglevBuiltImageData(building);}
	@Override public IImageDrawer getImageDrawer(BuildingImageData building) 
		{return new MaglevImageDrawer(this, building);}

	@Override public IInternalImage dupe() {return new MaglevInternalImage(this);}
	@Override public InternalImageTypes getType() {return InternalImageTypes.MAGLEV;}

	@Override public RawImage readOnlyAccess() {
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
	
				ABuiltImageData built = this.build(new BuildingImageData(context.getHandleFor(this), 0, 0));
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
	

	public class MaglevBuiltImageData extends ABuiltImageData {

		final int box;
		final int boy;
		
		public MaglevBuiltImageData(BuildingImageData building) 
		{
			super(building.handle);
			this.box = building.ox;
			this.boy = building.oy;
		}

		@Override public int getWidth() {return context.getWidth(); }
		@Override public int getHeight() {return context.getHeight();}

		@Override public float convertX(float x) {return x;}
		@Override public float convertY(float y) {return y;}
		@Override public Vec2i convert(Vec2i p) {return p;}

		@Override public Rect getBounds() 
			{return new Rect( box, boy, context.getWidth(), context.getHeight());}
		@Override public void draw(GraphicsContext gc) 
			{gc.drawImage(builtImage, box, boy);}
		@Override public void drawBorder(GraphicsContext gc) 
			{gc.drawRect(box, boy, context.getWidth(), context.getHeight());}
		@Override public MatTrans getCompositeTransform() 
			{return MatTrans.TranslationMatrix(box, boy);}
		@Override public MatTrans getScreenToImageTransform() 
			{return MatTrans.TranslationMatrix(-box, -boy);}

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
