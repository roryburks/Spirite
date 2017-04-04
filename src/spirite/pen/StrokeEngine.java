package spirite.pen;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mutil.Interpolation.CubicSplineInterpolator2D;
import mutil.Interpolation.InterpolatedPoint;
import mutil.Interpolation.Interpolator2D;
import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.image_data.DrawEngine;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.pen.PenTraits.PenDynamics;
import spirite.pen.PenTraits.PenState;

public abstract class StrokeEngine {
	public enum STATE { READY, DRAWING };
	
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
		
		private Color c = Color.BLACK;
		private StrokeEngine.Method method = StrokeEngine.Method.BASIC;
		private float width = 1.0f;
		private float alpha = 1.0f;
		private boolean hard = false;
		private PenDynamics dynamics = DrawEngine.getDefaultDynamics();
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

		public Color getColor() {return new Color( c.getRGB());}
		public void setColor( Color c) {
			if( !locked)
				this.c = c;
		}

		public StrokeEngine.Method getMethod() {return method;}
		public void setMethod( StrokeEngine.Method method) {
			if( !locked)
				this.method = method;
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

		public boolean getHard() {return hard;}
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

	public enum Method {BASIC, ERASE, PIXEL}
	
	private static final double DIFF = 5;
	
	private double interpos = 0;

	protected PenState oldState = new PenState();
	protected PenState newState = new PenState();
	protected PenState rawState = new PenState();	// Needed to prevent UndoAction from double-tranforming
	protected StrokeEngine.STATE state = StrokeEngine.STATE.READY;

	protected StrokeEngine.StrokeParams stroke = null;
	protected BuiltImageData data;
	private BufferedImage displayLayer;
	private BufferedImage fixedLayer;
	protected BufferedImage selectionMask;
	

	private Interpolator2D interpolator = null;
	
	// Recording of raw states
	protected ArrayList<PenState> prec = new ArrayList<>();

	protected BuiltSelection sel;
	
	// :::: Get's
	public StrokeEngine.StrokeParams getParams() {
		return stroke;
	}
	public BuiltImageData getImageData() {
		return data;
	}
	public StrokeEngine.STATE getState() {
		return state;
	}
	public BufferedImage getStrokeLayer() {
		return displayLayer;
	}
	// Methods used to record the Stroke so that it can be repeated
	//	Could possibly combine them into a single class
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
			BuiltImageData data,
			BuiltSelection selection) 
	{

		if( data == null) 
			return false;
		
		this.data = data;
		stroke = params;

		displayLayer = new BufferedImage( 
				data.getWidth(), data.getHeight(), Globals.BI_FORMAT);
		fixedLayer = new BufferedImage( 
				data.getWidth(), data.getHeight(), Globals.BI_FORMAT);
		
		sel = selection;
		interpos = 0;
		
		if( sel.selection != null) {
			selectionMask = new BufferedImage( 
					data.getWidth(), data.getHeight(), Globals.BI_FORMAT);
			MUtil.clearImage(selectionMask);
			
			Graphics2D g2 = (Graphics2D)selectionMask.getGraphics();
			g2.translate(sel.offsetX, sel.offsetY);
			sel.selection.drawSelectionMask(g2);
			g2.dispose();
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
		Point layerSpace = (data.convert(new Point(ps.x,ps.y)));
		
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
		
		
		if( MUtil.coordInImage( layerSpace.x, layerSpace.y, displayLayer)) 
		{
//			return startDrawStroke( newState);
		}
		return false;
	}

	public final boolean stepStroke( PenState ps) {
		Point layerSpace = data.convert( new Point( ps.x, ps.y));
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
					points.add(new PenState((int)Math.round(ip.x), (int)Math.round(ip.y), 
							(float) MUtil.lerp(prec.get(ip.left).pressure, prec.get(ip.right).pressure, ip.lerp)));
				}
				MUtil.clearImage(displayLayer);
				changed = this.drawToLayer(displayLayer, points);
			}
			else {
				int d = 2;
				if( prec.size() >= (d+1)) {
					changed = this.drawToLayer(fixedLayer, prec.subList(prec.size()-(d+1), prec.size()-(d-1)));
				}
				MUtil.clearImage(displayLayer);
				Graphics g = displayLayer.getGraphics();
				g.drawImage(fixedLayer, 0, 0, null);
				changed = this.drawToLayer(displayLayer, prec.subList(Math.max(0, prec.size()-(d)), prec.size()));
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
			Graphics g = data.checkoutRaw().getGraphics();
			drawStrokeLayer(g);
			g.dispose();
			data.checkin();
		}
		
		displayLayer.flush();
		fixedLayer.flush();
		displayLayer = null;
		fixedLayer = null;
		if( selectionMask != null) {
			selectionMask.flush();
			selectionMask = null;
		}
	}
	
	
	// =============
	// ==== Abstract Methods
	public abstract boolean drawToLayer( BufferedImage layer, List<PenState> states); 
	
//	public abstract boolean startDrawStroke( PenState ps);
//	public abstract boolean stepDrawStroke( PenState fromState, PenState toState);
//	public abstract void endStroke();

	
	/**
	 * In order to speed up undo/redo, certain Stroke Engines will batch all
	 * draw commands into a single command instead of updating the stroke layer
	 * repeatedly.
	 */
/*	public boolean batchDraw( 
			StrokeEngine.StrokeParams stroke, 
			PenState[] states, 
			BuiltImageData data,
			BuiltSelection mask) {
		return false;
	}*/
	

	public void batchDraw(StrokeParams params, PenState[] points, BuiltImageData builtImage, BuiltSelection mask) 
	{
		this.startStroke(params, points[0], builtImage, mask);

		MUtil.clearImage(displayLayer);
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
						(int)Math.round(ip.x), 
						(int)Math.round(ip.y), 
						(float) MUtil.lerp(points[ip.left].pressure, points[ip.right].pressure, ip.lerp)));
				while( interpos + DIFF < interpolator.getCurveLength()) {
					interpos += DIFF;
					ip = interpolator.evalExt(interpos);
					
					iPoints.add(new PenState(
							(int)Math.round(ip.x), 
							(int)Math.round(ip.y), 
							(float) MUtil.lerp(points[ip.left].pressure, points[ip.right].pressure, ip.lerp)));
				}
				MUtil.clearImage(displayLayer);
				this.drawToLayer(displayLayer, iPoints);
			}
		}
		else
			drawToLayer(displayLayer, Arrays.asList(points));
		
		this.endStroke();
	}


	// Draws the Stroke Layer onto the graphics
	public void drawStrokeLayer( Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Composite c = g2.getComposite();
		switch( stroke.getMethod()) {
		case BASIC:
		case PIXEL:
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER,stroke.getAlpha()));
			break;
		case ERASE:
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_OUT,stroke.getAlpha()));
			break;
		}
		g.drawImage(getStrokeLayer(), 0, 0, null);
		g2.setComposite( c);
	}
	
}
