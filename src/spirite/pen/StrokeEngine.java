package spirite.pen;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.MUtil;
import spirite.image_data.DrawEngine;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.pen.PenTraits.PenDynamics;
import spirite.pen.PenTraits.PenState;
import spirite.pen.StrokeEngine.Method;
import spirite.pen.StrokeEngine.StrokeParams;

public abstract class StrokeEngine {
	public enum STATE { READY, DRAWING };
	
	/** 
	 * StrokeParams define the style/tool/options of the Stroke.
	 * 
	 * lock is not actually used yet, but changing data mid-stroke is a 
	 * bar idea.
	 */
	public static class StrokeParams {
		
		Color c = Color.BLACK;
		StrokeEngine.Method method = StrokeEngine.Method.BASIC;
		private float width = 1.0f;
		private float alpha = 1.0f;
		private boolean hard = false;
		private PenDynamics dynamics = DrawEngine.getDefaultDynamics();
		private int maxWidth = 25;
	
		private boolean locked = false;
		
		public StrokeParams() {}
		
		public void setColor( Color c) {
			if( !locked)
				this.c = c;
		}
		public Color getColor() {return new Color( c.getRGB());}
		
		public void setMethod( StrokeEngine.Method method) {
			if( !locked)
				this.method = method;
		}
		public StrokeEngine.Method getMethod() {return method;}
		
		public void setWidth( float width) {
			if( !locked)
				this.width = width;
		}
		public float getWidth() { return width;}
		
		public void setAlpha( float alpha) {
			if( !locked)
				this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
		}
		public float getAlpha() {return alpha;}
		
		public void setHard( boolean hard) {
			if( !locked)
				this.hard = hard;
		}
		public boolean getHard() {return hard;}
		
		public void setDynamics( PenDynamics dynamics) {
			if( !locked && dynamics != null)
				this.dynamics = dynamics;
		}
		public PenDynamics getDynamics() {
			return dynamics;
		}
		
		public void setMaxWidth( int width) {
			if( !locked) this.maxWidth = width;
		}
		public int getMaxWidth() { return this.maxWidth;}
	}

	public enum Method {BASIC, ERASE, PIXEL}

	protected PenState oldState = new PenState();
	protected PenState newState = new PenState();
	protected PenState rawState = new PenState();	// Needed to prevent UndoAction from double-tranforming
	protected StrokeEngine.STATE state = StrokeEngine.STATE.READY;

	protected StrokeEngine.StrokeParams stroke = null;
	protected BuiltImageData data;
	protected BufferedImage strokeLayer;
	protected BufferedImage compositionLayer;
	protected BufferedImage selectionMask;
	
	// Recording of raw states
	protected List<PenState> prec = new LinkedList<>();

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
		return strokeLayer;
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
			StrokeEngine.StrokeParams s, 
			PenState ps, 
			BuiltImageData data,
			BuiltSelection selection) 
	{

		if( data == null) 
			return false;
		
		this.data = data;
		stroke = s;
		
		strokeLayer = new BufferedImage( 
				data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		compositionLayer = new BufferedImage( 
				data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		
		sel = selection;
		
		if( sel.selection != null) {
			selectionMask = new BufferedImage( 
					data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
			MUtil.clearImage(selectionMask);
			
			Graphics2D g2 = (Graphics2D)selectionMask.getGraphics();
			g2.translate(sel.offsetX, sel.offsetY);
			sel.selection.drawSelectionMask(g2);
			g2.dispose();
		}
		
		// Starts recording the Pen States
		prec = new LinkedList<PenState>();
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
		
		state = StrokeEngine.STATE.DRAWING;
		
		
		if( MUtil.coordInImage( layerSpace.x, layerSpace.y, strokeLayer)) 
		{
			return startDrawStroke( newState);
		}
		return false;
	}

	public final boolean stepStroke( PenState ps) {
		int start_x = oldState.x;
		int start_y = oldState.y;
		double distance = MUtil.distance( start_x, start_y, ps.x, ps.y);
//		int 
		
		
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
		
		boolean changed = stepDrawStroke( oldState, newState);
		if( changed) prec.add( new PenState( rawState));

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
		
		strokeLayer.flush();
		compositionLayer.flush();
		if( selectionMask != null)
			selectionMask.flush();
	}
	
	
	public abstract boolean startDrawStroke( PenState ps);
	public abstract boolean stepDrawStroke( PenState fromState, PenState toState);
//	public abstract void endStroke();

	
	/**
	 * In order to speed up undo/redo, certain Stroke Engines will batch all
	 * draw commands into a single command instead of updating the stroke layer
	 * repeatedly.
	 */
	public boolean batchDraw( 
			StrokeEngine.StrokeParams stroke, 
			PenState[] states, 
			BuiltImageData data,
			BuiltSelection mask) {
		return false;
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
	
	public BufferedImage getCompositionLayer() {
		MUtil.clearImage(compositionLayer);
		return compositionLayer;
	}
}
