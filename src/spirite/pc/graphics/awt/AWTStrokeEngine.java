package spirite.pc.graphics.awt;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import spirite.base.graphics.GraphicsContext;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.hybrid.HybridHelper;
import spirite.pc.graphics.ImageBI;

/***
 * The StrokeEngine operates asynchronously to the input data.  In general
 * the stroke is only drawn at a rate of 60FPS regardless of how fast the 
 * pen input is performed.
 * 
 * The StrokeEngine creates three BufferedImages the size of the ImageData
 * in question:
 * -The strokeLayer stores the actual stroke visually.  Strokes are drawn on 
 *   this layer before being anchored to the ImageData layer at the end of the
 *   stroke so that transparency and other blend methods can be performed without
 *   worrying about the stroke drawing over itself.
 * -The compositionLayer is stored for the benefit of ImageData which needs
 *   another layer in order for certain blend modes/Stroke styles to properly
 *   render
 * -The selectionMask is cached because the memory waste is minimal compared
 *   to the amount of extra cycles it'd be to constantly draw an inverse mask
 *   of the selection.
 */
class AWTStrokeEngine extends StrokeEngine{
	BufferedImage displayLayer;
	BufferedImage fixedLayer;
	BufferedImage selectionMask;
	
	@Override
	protected void onStart() {
		int w = data.getWidth();
		int h = data.getHeight();
		
		displayLayer = new BufferedImage( w, h, HybridHelper.BI_FORMAT);
		fixedLayer = new BufferedImage( w, h, HybridHelper.BI_FORMAT);
		
		if( sel.selection != null) {
			selectionMask = new BufferedImage( w, h, HybridHelper.BI_FORMAT);
			
			GraphicsContext gc = new AWTContext(selectionMask);
			gc.clear();
			gc.translate(sel.offsetX, sel.offsetY);
			sel.selection.drawSelectionMask(gc);
//			g2.dispose();
		}
	}
	@Override
	protected void onEnd() {
		displayLayer.flush();
		fixedLayer.flush();
		displayLayer = null;
		fixedLayer = null;
		if( selectionMask != null) {
			selectionMask.flush();
			selectionMask = null;
		}
	}
	

	@Override
	protected void prepareDisplayLayer() {
		Graphics2D g = (Graphics2D)displayLayer.getGraphics();
        g.setBackground(new Color(255, 255, 255, 0));
		g.clearRect(0, 0, displayLayer.getWidth(), displayLayer.getHeight());
		g.drawImage(fixedLayer, 0, 0, null);
		
	}
	
	@Override
	protected boolean drawToLayer(List<PenState> states, boolean permanent) {
		BufferedImage layer = (permanent)?fixedLayer:displayLayer;
		
		Graphics g = layer.getGraphics();
		Graphics2D g2 = (Graphics2D)g;
		g.setColor( new Color(stroke.getColor()));

		for( int i=1; i < states.size(); ++i) {
			PenState fromState = states.get(i-1);
			PenState toState = states.get(i);
			if( stroke.getMethod() != StrokeEngine.Method.PIXEL){
				g2.setStroke( new BasicStroke( 
						stroke.getDynamics().getSize(toState)*stroke.getWidth(), 
						BasicStroke.CAP_ROUND, 
						BasicStroke.CAP_SQUARE));
			}
			g2.drawLine( fromState.x, fromState.y, toState.x, toState.y);

			

			if( sel.selection != null) {
				g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
				g2.drawImage(selectionMask, 0, 0, null);
			}
		}
		
		g.dispose();
		return true;
	}

	@Override
	protected void drawDisplayLayer(GraphicsContext gc) {
		gc.drawImage(new ImageBI(displayLayer), 0, 0);
	}
}