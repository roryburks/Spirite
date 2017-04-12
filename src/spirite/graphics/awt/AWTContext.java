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

public class AWTContext extends GraphicsContext{
	AWTStrokeEngine strokeEngine = new AWTStrokeEngine();
	
	private final Graphics g;
	
	public AWTContext( Graphics g) {
		this.g = g;
	}

	@Override
	public BufferedImage renderToImage(RenderRoutine renderable, int width, int height) {
		BufferedImage bi = new BufferedImage( width, height, Globals.BI_FORMAT);
		
		Graphics sub = bi.getGraphics();
		renderable.render(new AWTContext( sub));
		sub.dispose();
		return bi;
	}

	@Override
	public NodeRenderer createNodeRenderer(GroupNode node, RenderEngine context) {
		return new AWTNodeRenderer(node, context);
	}

	@Override
	public StrokeEngine getStrokeEngine() {
		return strokeEngine;
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

	@Override
	public void changeColor(BufferedImage image, Color from, Color to, int mode) {
		// TODO: Make Better (or at least test if there is a better way to access
		//	and change a batch of BufferedImage data)
		int f = (mode == 0) ? from.getRGB() : (from.getRGB() & 0xFFFFFF);
		int t = (mode == 0) ? to.getRGB() : (to.getRGB() & 0xFFFFFF);
		int rgb;
		for( int x = 0; x < image.getWidth(); ++x) {
			for( int y=0; y < image.getHeight(); ++y) {
				rgb = image.getRGB(x, y);
				switch( mode) {
				case 0:
					if( rgb == f)
						image.setRGB(x, y, t);
					break;
				case 1:
					if( (rgb & 0xFFFFFF) == f)
						image.setRGB(x, y, (rgb & 0xFF000000) | t);
					break;
				case 2:
					image.setRGB(x, y, (rgb & 0xFF000000) | t);
					break;
				}
			}
		}
	}

	@Override
	public void invert(BufferedImage image) {
		// TODO: Make Better (or at least test if there is a better way to access
		//	and change a batch of BufferedImage data)
		for( int x = 0; x < image.getWidth(); ++x) {
			for( int y=0; y < image.getHeight(); ++y) {
				int rgb = image.getRGB(x, y);
				image.setRGB(x, y, (rgb & 0xFF000000) | (0xFFFFFF - (rgb&0xFFFFFF)));
			}
		}
	}


}
