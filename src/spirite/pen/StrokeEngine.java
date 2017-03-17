package spirite.pen;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import spirite.MDebug;
import spirite.MUtil;
import spirite.MDebug.WarningType;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.pen.PenTraits.PenState;
import spirite.pen.StrokeEngine.STATE;

public abstract class StrokeEngine {
	public enum STATE { READY, DRAWING };
	
	protected PenState oldState = new PenState();
	protected PenState newState = new PenState();
	protected PenState rawState = new PenState();	// Needed to prevent UndoAction from double-tranforming
	protected STATE state = STATE.READY;

	protected StrokeParams stroke = null;
	protected BuiltImageData data;
	protected BufferedImage strokeLayer;
	protected BufferedImage compositionLayer;
	protected BufferedImage selectionMask;
	
	// Recording of raw states
	protected List<PenState> prec = new LinkedList<>();

	protected BuiltSelection sel;
	
	// :::: Get's
	public StrokeParams getParams() {
		return stroke;
	}
	public BuiltImageData getImageData() {
		return data;
	}
	public STATE getState() {
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
	public boolean startStroke( 
			StrokeParams s, 
			PenState ps, 
			BuiltImageData data,
			BuiltSelection selection) 
	{

		if( data == null) 
			return false;
		
		this.data = data;
		stroke = s;
		
		strokeLayer = new BufferedImage( 
				data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);
		compositionLayer = new BufferedImage( 
				data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		sel = selection;
		
		if( sel.selection != null) {
			selectionMask = new BufferedImage( 
					data.getWidth(), data.getHeight(), BufferedImage.TYPE_INT_ARGB);
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
		
		state = STATE.DRAWING;
		
		
		if( MUtil.coordInImage( layerSpace.x, layerSpace.y, strokeLayer)) 
		{
			return startDrawStroke( newState);
		}
		return false;
	}

	public boolean stepStroke( PenState ps) {
		Point layerSpace = data.convert( new Point( ps.x, ps.y));
		newState.x = layerSpace.x;
		newState.y = layerSpace.y;
		newState.pressure = ps.pressure;
		rawState.x = ps.x;
		rawState.y = ps.y;
		rawState.pressure = ps.pressure;
		
		if( state != STATE.DRAWING || data == null) {
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
	public void endStroke() {

		state = STATE.READY;
		
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
			StrokeParams stroke, 
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
