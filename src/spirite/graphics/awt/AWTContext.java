package spirite.graphics.awt;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import spirite.Globals;
import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.graphics.GraphicsContext;
import spirite.image_data.GroupTree.GroupNode;
import spirite.pen.StrokeEngine;

/**
 * AWTContext is a GraphicsContext using only native AWT calls (as well as some
 * software rendering).  For the most part it is a wrapper for a Graphics object.
 * 
 * @author Rory Burks
 *
 */
public class AWTContext extends GraphicsContext{
	
	private final Graphics g;
	
	public AWTContext( Graphics g) {
		this.g = g;
	}

	@Override
	public void drawBounds(BufferedImage mask, int c, AffineTransform trans) {
		Rectangle r = g.getClipBounds();
		
		BufferedImage bi = new BufferedImage( r.width, r.height, Globals.BI_FORMAT);
		Graphics2D g2 = (Graphics2D)(bi.getGraphics());
		g2.setTransform(trans);
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g2.drawImage(mask, 1, 0, null);
		g2.drawImage(mask, -1, 0, null);
		g2.drawImage(mask, 0, 1, null);
		g2.drawImage(mask, 0, -1, null);
		g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_OUT, 1));
		g2.drawImage(mask, 0, 0, null);
		g2.dispose();
		
		g.drawImage(bi, r.x, r.y, null);
	}
}
