package spirite.base.pen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import spirite.base.brains.ToolsetManager.PenDrawMode;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.image_data.SelectionEngine.BuiltSelection;
import spirite.base.image_data.images.ABuiltImageData;
import spirite.base.pen.PenTraits.PenDynamics;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.util.Colors;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;
import spirite.base.util.glmath.Vec2i;
import spirite.base.util.interpolation.CubicSplineInterpolator2D;
import spirite.base.util.interpolation.Interpolator2D;
import spirite.base.util.interpolation.Interpolator2D.InterpolatedPoint;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;

public abstract class StrokeEngine {

	// =============
	// ==== Abstract Methods
	protected abstract boolean drawToLayer( DrawPoints points, boolean permanent);
	protected abstract void prepareDisplayLayer();
	protected abstract void onStart();
	protected abstract void onEnd();
	protected abstract void drawDisplayLayer( GraphicsContext gc);
	
	// Made as a class instead of parameters for extendability
	public static class DrawPoints {
		public final float[] x;
		public final float[] y;
		public final float[] w;
		public final int length;
		public DrawPoints( float[] x, float[] y, float[] w) {
			this.x = x;
			this.y = y;
			this.w = w;
			
			this.length = x.length;
			if( x.length != y.length || x.length != w.length)
				System.out.println("BAD");
		}
	}
	

	// ==== 
	public enum STATE { READY, DRAWING };
	public enum Method {
		BASIC(0), 
		ERASE(1), 
		PIXEL(2)
		;
		
		public final int fileId;
		private Method( int fid) {this.fileId = fid;}
		public static Method fromFileId(int fid) {
			for( Method m : Method.values())
				if( m.fileId == fid)
					return m;
			return null;
		}
	};
	
	public static final double DIFF = 1;
	
	// Pen States
	protected PenState oldState = new PenState();
	protected PenState newState = new PenState();
	protected PenState rawState = new PenState();	// Needed to prevent UndoAction from double-tranforming
	protected StrokeEngine.STATE state = StrokeEngine.STATE.READY;
	protected ArrayList<PenState> prec = new ArrayList<>();	// Recording of raw states

	// Context
	protected StrokeEngine.StrokeParams stroke = null;
	protected ABuiltImageData data;
	protected BuiltSelection sel;
	
	// Interpolation
	private Interpolator2D _interpolator = null;
	private float interpos = 0;
	
	// :::: Get's
	public StrokeEngine.StrokeParams getParams() {
		return stroke;
	}
	public ABuiltImageData getImageData() {
		return data;
	}
	public StrokeEngine.STATE getState() {
		return state;
	}
	/** Methods used to record the Stroke so that it can be repeated
	 *	Could possibly combine them into a single class */
	public PenState[] getHistory() {
		PenState[] array = new PenState[prec.size()];
		return prec.toArray(array);
	}
	public BuiltSelection getLastSelection() {
		return sel;
	}

	/**
	 * Starts a new stroke using the workspace's current selection as the 
	 * selection mask 
	 * 
	 * @return true if the data has been changed, false otherwise.*/
	public final boolean startStroke( 
			StrokeParams params, 
			PenState ps, 
			ABuiltImageData data,
			BuiltSelection selection) 
	{
		if( ! prepareStroke(params, data, selection))
			return false;
		buildInterpolator(params, ps);
		
		if( sel.selection != null) {
/*			selectionMask = new BufferedImage( 
					data.getWidth(), data.getHeight(), Globals.BI_FORMAT);
			MUtil.clearImage(selectionMask);
			
			Graphics2D g2 = (Graphics2D)selectionMask.getGraphics();
			g2.translate(sel.offsetX, sel.offsetY);
			sel.selection.drawSelectionMask(g2);
			g2.dispose();*/
		}
		
		// Starts recording the Pen States
		prec = new ArrayList<PenState>();
		Vec2i layerSpace = (data.convert(new Vec2i((int)Math.round(ps.x),(int)Math.round(ps.y))));
		
		oldState.x = layerSpace.x;
		oldState.y = layerSpace.y;
		oldState.pressure = ps.pressure;
		newState.x = layerSpace.x;
		newState.y = layerSpace.y;
		newState.pressure = ps.pressure;
		rawState.x = ps.x;
		rawState.y = ps.y;
		rawState.pressure = ps.pressure;
		prec.add( ps);
		
		state = StrokeEngine.STATE.DRAWING;
		
		
		
		drawToLayer( new DrawPoints( 
				new float[] {ps.x},
				new float[] {ps.y}, 
				new float[] {params.dynamics.getSize(ps)}),
				false);
//		return startDrawStroke( newState);
		return true;
	}
	
	private boolean prepareStroke( 
			StrokeParams params, 
			ABuiltImageData data,
			BuiltSelection selection)
	{
		if( data == null) 
			return false;
		
		this.data = data;
		stroke = params;

		
		sel = selection;
		interpos = 0;
		onStart( );
		prepareDisplayLayer();
		return true;
	}
	
	private void buildInterpolator( StrokeParams params, PenState ps)
	{
		switch( params.interpolationMethod){
		case CUBIC_SPLINE:
			_interpolator = new CubicSplineInterpolator2D(null, true);
			break;
		default:
			_interpolator = null;
			break;
		}
		if( _interpolator != null) _interpolator.addPoint(ps.x, ps.y);
	}

	public final boolean stepStroke( PenState ps) {
		Vec2i layerSpace = data.convert( new Vec2i( (int)Math.round(ps.x), (int)Math.round(ps.y)));
		PenState buff;
		newState.x = layerSpace.x;
		newState.y = layerSpace.y;
		newState.pressure = ps.pressure;
		rawState.x = ps.x;
		rawState.y = ps.y;
		rawState.pressure = ps.pressure;
		
		if( state != StrokeEngine.STATE.DRAWING || data == null) {
			MDebug.handleWarning( WarningType.STRUCTURAL, this, "Data Dropped mid-stroke (possible loss of Undo functionality)");
			return false;
		}
		
		boolean changed = false;
		if( oldState.x != newState.x || oldState.y != newState.y)
		{
			prec.add( new PenState( rawState));
			if( _interpolator != null) 
				_interpolator.addPoint(rawState.x, rawState.y);

			prepareDisplayLayer();
			changed = this.drawToLayer( buildPoints(_interpolator, prec, stroke), false);
		}

		oldState.x = newState.x;
		oldState.y = newState.y;
		oldState.pressure = newState.pressure;
		
		return changed;
	}
	

	/** Finalizes the stroke, resetting the state, anchoring the strokeLayer
	 * to the data, and flushing the used resources. */
	public final void endStroke() {

		state = StrokeEngine.STATE.READY;
		
		if( data != null) {
			GraphicsContext gc = data.checkoutRaw().getGraphics();
			drawStrokeLayer(gc);
			data.checkin();
		}
		
		onEnd();
		_interpolator = null;
	}
	
	
	/**
	 * In order to speed up undo/redo, certain Stroke Engines will batch all
	 * draw commands into a single command instead of updating the stroke layer
	 * repeatedly.
	 */
	public void batchDraw(StrokeParams params, PenState[] points, ABuiltImageData builtImage, BuiltSelection mask) 
	{
		prepareStroke(params, builtImage, mask);
		buildInterpolator(params, points[0]);

		if( _interpolator != null) {
			// NOTE: startStroke already adds the first Point into the 
			//	Interpolator, so we start at point 2 (index 1).
			for( int i=1; i < points.length; ++i) {
				_interpolator.addPoint(points[i].x, points[i].y);
			}
		}

		this.drawToLayer( buildPoints(_interpolator, Arrays.asList(points), stroke), false);
		
		if( data != null) {
			GraphicsContext gc = data.checkoutRaw().getGraphics();
			drawStrokeLayer(gc);
			data.checkin();
		}
		
		_interpolator = null;
	}
	
	public static DrawPoints buildPoints(Interpolator2D localInterpolator, List<PenState> penStates, StrokeParams params) {
		PenState buff;
		
		if( localInterpolator != null) {
			FloatCompactor fcx = new FloatCompactor();
			FloatCompactor fcy = new FloatCompactor();
			FloatCompactor fcw = new FloatCompactor();
	
			float localInterpos = 0;
			InterpolatedPoint ip = localInterpolator.evalExt(localInterpos);
			buff = new PenState( ip.x, ip.y, (float) MUtil.lerp(penStates.get(ip.left).pressure, penStates.get(ip.right).pressure, ip.lerp));
			fcx.add(ip.x);
			fcy.add(ip.y);
			fcw.add(params.dynamics.getSize(buff));
			
			while( localInterpos + DIFF < localInterpolator.getCurveLength()) {
				localInterpos += DIFF;
				ip = localInterpolator.evalExt(localInterpos);
	
				buff = new PenState( ip.x, ip.y, (float) MUtil.lerp(penStates.get(ip.left).pressure, penStates.get(ip.right).pressure, ip.lerp));
				fcx.add(ip.x);
				fcy.add(ip.y);
				fcw.add(params.dynamics.getSize(buff));
			}
			
			return new DrawPoints(fcx.toArray(), fcy.toArray(), fcw.toArray());
		}
		else{
			float[] xs = new float[penStates.size()];
			float[] ys = new float[penStates.size()];
			float[] ws = new float[penStates.size()];
			for( int i=0; i < penStates.size(); ++i) {
				xs[i] = penStates.get(i).x;
				ys[i] = penStates.get(i).y;
				ws[i] = params.dynamics.getSize(penStates.get(i));
			}
			return new DrawPoints( xs, ys, ws);
		}
	}
	
	public void batchDraw( DrawPoints points, ABuiltImageData builtImage, BuiltSelection mask) 
	{
		prepareStroke(null, builtImage, mask);

		this.drawToLayer( points, false);
		
		if( data != null) {
			GraphicsContext gc = data.checkoutRaw().getGraphics();
			drawStrokeLayer(gc);
			data.checkin();
		}
	}


	// Draws the Stroke Layer onto the graphics
	public void drawStrokeLayer( GraphicsContext gc) {
		float oldAlpha = gc.getAlpha();
		Composite oldComp = gc.getComposite();
		
		switch( stroke.getMethod()) {
		case BASIC:
		case PIXEL:
			gc.setComposite(Composite.SRC_OVER, stroke.getAlpha());
			break;
		case ERASE:
			gc.setComposite(Composite.DST_OUT, stroke.getAlpha());
			break;
		}
/*		Graphics2D g2 = (Graphics2D)g;
		Composite c = g2.getComposite();
		switch( stroke.getMethod()) {
		case BASIC:
		case PIXEL:
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER,stroke.getAlpha()));
			break;
		case ERASE:
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_OUT,stroke.getAlpha()));
			break;
		}*/
		drawDisplayLayer(gc);
		gc.setComposite( oldComp, oldAlpha);
	}
	
	

	/** 
	 * StrokeParams define the style/tool/options of the Stroke.
	 * 
	 * lock is not actually used yet, but changing data mid-stroke is a 
	 * bar idea.
	 */
	public static class StrokeParams {
		public enum InterpolationMethod {
			NONE,
			CUBIC_SPLINE,
		}
		
		
		private int c = Colors.BLACK;
		private StrokeEngine.Method method = StrokeEngine.Method.BASIC;
		private PenDrawMode mode = PenDrawMode.NORMAL;
		private float width = 1.0f;
		private float alpha = 1.0f;
		private boolean hard = false;
		private PenDynamics dynamics = PenDynamicsConstants.getBasicDynamics();
		private int maxWidth = 25;
		private InterpolationMethod interpolationMethod = InterpolationMethod.CUBIC_SPLINE;
		
	
		private boolean locked = false;

		public StrokeParams() {}
		
		/** If Params are locked, they're being used and can't be changed.
		 * Only the base StrokeEngine can lock/unlock Params.  Once they are 
		 * locked they will usually never be unlocked as the UndoEngine needs
		 * to remember the saved settings.
		 */
		public boolean isLocked() {return locked;}

		public int getColor() {return c;}
		public void setColor( int c) {
			if( !locked)
				this.c = c;
		}

		public StrokeEngine.Method getMethod() {return method;}
		public void setMethod( StrokeEngine.Method method) {
			if( !locked)
				this.method = method;
		}
		
		public PenDrawMode getMode() {return mode;}
		public void setMode( PenDrawMode mode) {
			if( !locked) 
				this.mode = mode;				
		}

		public float getWidth() { return width;}
		public void setWidth( float width) {
			if( !locked)
				this.width = width;
		}

		public float getAlpha() {return alpha;}
		public void setAlpha( float alpha) {
			if( !locked)
				this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
		}

		public boolean isHard() {return hard;}
		public void setHard( boolean hard) {
			if( !locked)
				this.hard = hard;
		}

		public PenDynamics getDynamics() {return dynamics;}
		public void setDynamics( PenDynamics dynamics) {
			if( !locked && dynamics != null)
				this.dynamics = dynamics;
		}

		public int getMaxWidth() { return this.maxWidth;}
		public void setMaxWidth( int width) {
			if( !locked) this.maxWidth = width;
		}

		public InterpolationMethod getInterpolationMethod() { return this.interpolationMethod;}
		public void setInterpolationMethod( InterpolationMethod method) {
			if(!locked) this.interpolationMethod = method;
		}
		

		/**
		 * Bakes the PenDynamics of the original StrokeParameters and bakes its dynamics
		 * in-place over the given penStates, returning an equivalent StrokeParams, but 
		 * with Linear Dynamics
		 */
		public static StrokeParams bakeAndNormalize( StrokeParams original, PenState[] penStates) {
			StrokeParams out = new StrokeParams();
			out.alpha = original.alpha;
			out.c = original.c;
			out.dynamics = PenDynamicsConstants.LinearDynamics();
			out.hard = original.hard;
			out.interpolationMethod = original.interpolationMethod;
			out.method = original.method;
			out.mode = original.mode;
			out.width = original.width;
			
			for( int i=0; i<penStates.length; ++i)
				penStates[i].pressure = original.getDynamics().getSize(penStates[i]);
			
			return out;
		}
	}
}
