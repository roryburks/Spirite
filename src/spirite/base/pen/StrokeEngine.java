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
import spirite.base.util.glmath.Vec2i;
import spirite.base.util.interpolation.CubicSplineInterpolator2D;
import spirite.base.util.interpolation.Interpolator2D;
import spirite.base.util.interpolation.Interpolator2D.InterpolatedPoint;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;

public abstract class StrokeEngine {

	// =============
	// ==== Abstract Methods
	protected abstract boolean drawToLayer( List<PenState> states, boolean permanent);
	protected abstract void prepareDisplayLayer();
	protected abstract void onStart();
	protected abstract void onEnd();
	protected abstract void drawDisplayLayer( GraphicsContext gc);
	
	

	// ==== 
	public enum STATE { READY, DRAWING };
	public enum Method {BASIC, ERASE, PIXEL};
	
	private static final double DIFF = 1;
	
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
	private Interpolator2D interpolator = null;
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

		if( data == null) 
			return false;
		
		this.data = data;
		stroke = params;

		
		sel = selection;
		interpos = 0;
		
		if( sel.selection != null) {
/*			selectionMask = new BufferedImage( 
					data.getWidth(), data.getHeight(), Globals.BI_FORMAT);
			MUtil.clearImage(selectionMask);
			
			Graphics2D g2 = (Graphics2D)selectionMask.getGraphics();
			g2.translate(sel.offsetX, sel.offsetY);
			sel.selection.drawSelectionMask(g2);
			g2.dispose();*/
		}
		switch( params.interpolationMethod){
		case CUBIC_SPLINE:
			interpolator = new CubicSplineInterpolator2D(null, true);
			break;
		default:
			interpolator = null;
			break;
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
		if( interpolator != null) interpolator.addPoint(ps.x, ps.y);
		
		state = StrokeEngine.STATE.DRAWING;
		
		onStart( );
		
		prepareDisplayLayer();
		
		
		drawToLayer( Arrays.asList( new PenState[]{ps,
				(stroke.method == Method.PIXEL)?new PenState(ps.x, ps.y+1, ps.pressure):ps}), false);
//		return startDrawStroke( newState);
		return true;
	}

	public final boolean stepStroke( PenState ps) {
		Vec2i layerSpace = data.convert( new Vec2i( (int)Math.round(ps.x), (int)Math.round(ps.y)));
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
			if( interpolator != null) {
				interpolator.addPoint(rawState.x, rawState.y);

				List<PenState> points = new ArrayList<>();

				interpos = 0;
				InterpolatedPoint ip = interpolator.evalExt(interpos);
				points.add(new PenState((int)Math.round(ip.x), (int)Math.round(ip.y), 
						(float) MUtil.lerp(prec.get(ip.left).pressure, prec.get(ip.right).pressure, ip.lerp)));
				while( interpos + DIFF < interpolator.getCurveLength()) {
					interpos += DIFF;
					ip = interpolator.evalExt(interpos);
					
					points.add(new PenState(ip.x, ip.y, 
							(float) MUtil.lerp(prec.get(ip.left).pressure, prec.get(ip.right).pressure, ip.lerp)));
				}
				prepareDisplayLayer();
				changed = this.drawToLayer(points, false);
			}
			else {
//				int d = 2;
//				if( prec.size() >= (d+1)) {
//					changed = this.drawToLayer(prec.subList(prec.size()-(d+1), prec.size()-(d-1)), true);
//				}
				prepareDisplayLayer();
				changed = this.drawToLayer(prec, false);
			}
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
	}
	
	
	/**
	 * In order to speed up undo/redo, certain Stroke Engines will batch all
	 * draw commands into a single command instead of updating the stroke layer
	 * repeatedly.
	 */
	

	public void batchDraw(StrokeParams params, PenState[] points, ABuiltImageData builtImage, BuiltSelection mask) 
	{
		this.startStroke(params, points[0], builtImage, mask);

		//prepareDisplayLayer();	// Not needed becaus it's done in startStroke, but that might change
		if( interpolator != null) {
			// NOTE: startStroke already adds the first Point into the 
			//	Interpolator, so we start at point 2 (index 1).
			for( int i=1; i < points.length; ++i) {
				interpolator.addPoint(points[i].x, points[i].y);
			}
			
			if( points.length >= 2) {
				// Go through and Interpolate, DIFF pixels at a time
				List<PenState> iPoints = new ArrayList<>();
				interpos = 0;
				InterpolatedPoint ip = interpolator.evalExt(interpos);
				iPoints.add(new PenState(
						ip.x, 
						ip.y, 
						(float) MUtil.lerp(points[ip.left].pressure, points[ip.right].pressure, ip.lerp)));
				while( interpos + DIFF < interpolator.getCurveLength()) {
					interpos += DIFF;
					ip = interpolator.evalExt(interpos);
					
					iPoints.add(new PenState(
							ip.x, 
							ip.y, 
							(float) MUtil.lerp(points[ip.left].pressure, points[ip.right].pressure, ip.lerp)));
				}
				iPoints.add(new PenState(
						ip.x, 
						ip.y, 
						(float) MUtil.lerp(points[ip.left].pressure, points[ip.right].pressure, ip.lerp)));
				this.drawToLayer(iPoints, false);
			}
		}
		else
			drawToLayer(Arrays.asList(points), false);
		
		this.endStroke();
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
	}
}
