package spirite.base.graphics.awt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import spirite.base.brains.RenderEngine;
import spirite.base.brains.RenderEngine.NodeRenderer;
import spirite.base.graphics.GraphicsDrawer;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.pen.StrokeEngine;
import spirite.base.image_data.RawImage;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.graphics.ImageBI;

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
	public void changeColor(RawImage image, int cFrom, int cTo, int mode) {
		if( !(image instanceof ImageBI)) {
			MDebug.handleWarning( WarningType.UNSUPPORTED, null, "Unsupported Image Type in AWTDrawer");
			return;
		}
		BufferedImage bi = ((ImageBI)image).img;
		
		// TODO: Make Better (or at least test if there is a better way to access
		//	and change a batch of BufferedImage data)
		int f = (mode == 0) ? cFrom : (cFrom & 0xFFFFFF);
		int t = (mode == 0) ? cTo : (cTo & 0xFFFFFF);
		int rgb;
		for( int x = 0; x < bi.getWidth(); ++x) {
			for( int y=0; y < bi.getHeight(); ++y) {
				rgb = bi.getRGB(x, y);
				switch( mode) {
				case 0:
					if( rgb == f)
						bi.setRGB(x, y, t);
					break;
				case 1:
					if( (rgb & 0xFFFFFF) == f)
						bi.setRGB(x, y, (rgb & 0xFF000000) | t);
					break;
				case 2:
					bi.setRGB(x, y, (rgb & 0xFF000000) | t);
					break;
				}
			}
		}
		
	}

	@Override
	public void invert(RawImage image) {
		if( !(image instanceof ImageBI)) {
			MDebug.handleWarning( WarningType.UNSUPPORTED, null, "Unsupported Image Type in AWTDrawer");
			return;
		}
		BufferedImage bi = ((ImageBI)image).img;
		
		// TODO: Make Better (or at least test if there is a better way to access
		//	and change a batch of BufferedImage data)
		for( int x = 0; x < bi.getWidth(); ++x) {
			for( int y=0; y < bi.getHeight(); ++y) {
				int rgb = bi.getRGB(x, y);
				bi.setRGB(x, y, (rgb & 0xFF000000) | (0xFFFFFF - (rgb&0xFFFFFF)));
			}
		}
	}

}
