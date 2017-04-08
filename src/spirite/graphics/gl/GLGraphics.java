package spirite.graphics.gl;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.graphics.GraphicsContext;
import spirite.image_data.GroupTree.GroupNode;
import spirite.pen.StrokeEngine;

public class GLGraphics extends GraphicsContext{
	private final GLStrokeEngine strokeEngine = new GLStrokeEngine();
	private final JOGLDrawer glDrawer = new JOGLDrawer();
	@Override
	public NodeRenderer createNodeRenderer(GroupNode node, RenderEngine context) {
		return new GLNodeRenderer( node, context);
	}

	@Override
	public StrokeEngine getStrokeEngine() {
		return strokeEngine;
	}

	@Override
	public BufferedImage drawBounds(BufferedImage mask, int c, AffineTransform trans, int width, int height) {
		return GLUIDraw.drawBounds(mask, c, trans, width, height);
	}

	@Override public void changeColor(BufferedImage image, Color from, Color to, int mode) {
		glDrawer.changeColor(image, from, to, mode);
	}
	@Override public void invert(BufferedImage image) {
		glDrawer.invert(image);		
	}

}
