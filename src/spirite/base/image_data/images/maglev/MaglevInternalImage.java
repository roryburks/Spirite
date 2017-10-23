package spirite.base.image_data.images.maglev;

import java.util.ArrayList;
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
import spirite.base.pen.StrokeEngine.StrokeParams;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.base.util.glmath.Vec2i;
import spirite.hybrid.HybridHelper;

/**
 * A Maglev Internal Image is an image that floats just above the surface, 
 * not quite planting its feat in the ground.  Essentially it is a kind of 
 * Scalable Vector Image, storing all the different stroke and fill actions
 * as logical vertex data rather than rendered pixel data, allowing them to
 * be scaled/rotated without generation loss	.
 */
public class MaglevInternalImage implements IInternalImage {
	final List<MagLevStroke> strokes;
	private ImageWorkspace context;
	private RawImage builtImage = null;
	private boolean isBuilt = false;
	
	public MaglevInternalImage( ImageWorkspace context) {
		this.context = context;
		this.strokes =  new ArrayList<>();
		if( context != null) {
			// TODO: unflag "isBuilt" when Workspace changes size
		}
	}
	private MaglevInternalImage( MaglevInternalImage other) {
		this.context = other.context;
		this.strokes = new ArrayList<>(other.strokes.size());
		
		for( MagLevStroke stroke : other.strokes) {
			this.strokes.add(stroke.clone());
		}
	}
	
	void addStroke( MagLevStroke stroke) {strokes.add(stroke);}
	void popStroke() {strokes.remove(strokes.size()-1);}
	void unbuild() {
		if( this.isBuilt) {
			this.isBuilt = false;
			builtImage.flush();
			builtImage = null;
		}
	}
	
	public class MagLevStroke {
		PenState[] states;
		StrokeParams params;
		
		MagLevStroke( PenState[] states, StrokeParams params) {
			this.states = states;
			this.params = params;
		}
		
		protected MagLevStroke clone() {
			PenState[] newStates = new PenState[states.length];
			for( int i=0; i < states.length; ++i) {
				newStates[i] = new PenState( states[i]);
			}
			
			return new MagLevStroke(newStates, params);
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
	private void Build() {
		if( !building) {
			building = true;
			if( !isBuilt) {
				builtImage = HybridHelper.createImage(getWidth(), getHeight());
	
				ABuiltImageData built = this.build(new BuildingImageData(context.getHandleFor(this), 0, 0));
				BuiltSelection mask = new BuiltSelection(null, 0, 0);
				for( MagLevStroke stroke : strokes) {
	
					StrokeEngine _engine = context.getSettingsManager().getDefaultDrawer().getStrokeEngine();
					_engine.batchDraw(stroke.params, stroke.states, built, mask);
				}
			}
			isBuilt = true;
			building = false;
		}
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
