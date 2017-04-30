package spirite.base.graphics.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import spirite.base.brains.RenderEngine;
import spirite.base.brains.RenderEngine.NodeRenderer;
import spirite.base.graphics.GraphicsDrawer;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.pc.Globals;
import spirite.pen.StrokeEngine;

public class AWTDrawer extends GraphicsDrawer {

	private static AWTDrawer singly;
	public static AWTDrawer getInstance() {if( singly == null) singly = new AWTDrawer(); return singly;}
	private AWTDrawer() {}
	AWTStrokeEngine strokeEngine = new AWTStrokeEngine();
	@Override
	public NodeRenderer createNodeRenderer(GroupNode node, RenderEngine context) {
		return new AWTNodeRenderer(node, context);
	}

	@Override
	public StrokeEngine getStrokeEngine() {
		return strokeEngine;
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
