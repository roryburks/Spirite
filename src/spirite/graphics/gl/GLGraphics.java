package spirite.graphics.gl;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.graphics.GraphicsContext;
import spirite.graphics.gl.GLEngine.ProgramType;
import spirite.graphics.gl.GLMultiRenderer.GLRenderer;
import spirite.graphics.gl.GLParameters.GLFBOTexture;
import spirite.graphics.gl.GLParameters.GLImageTexture;
import spirite.graphics.gl.GLParameters.GLParam1i;
import spirite.image_data.GroupTree.GroupNode;
import spirite.pen.StrokeEngine;

public class GLGraphics extends GraphicsContext{
	private final GLStrokeEngine strokeEngine = new GLStrokeEngine();
	private final JOGLDrawer glDrawer = new JOGLDrawer();
	private final GLEngine engine = GLEngine.getInstance();
	
	private GL2 gl;
	private int width, height;
	private boolean flip;
	
	
	

	@Override
	public BufferedImage renderToImage(RenderRoutine renderable, int width, int height) {
		gl = engine.getGL2();
		engine.setSurfaceSize(width, height);

		renderable.render(this);

		return engine.glSurfaceToImage();
	}
	
	public void setContext(GL2 gl, int width, int height, boolean flip) {
		this.gl = gl;
		this.width = width;
		this.height = height;
		this.flip = flip;
	}
	public void resetContext() {
		gl = engine.getGL2(); 
		width = engine.width;
		height = engine.height;
		flip = false;
	}
	
	@Override
	public NodeRenderer createNodeRenderer(GroupNode node, RenderEngine context) {
		return new GLNodeRenderer( node, context);
	}

	@Override
	public StrokeEngine getStrokeEngine() {
		return strokeEngine;
	}

	@Override
	public void drawBounds(BufferedImage mask, int c, AffineTransform trans) {
		GLUIDraw.drawBounds(mask, c, trans, width, height);
	}

	@Override public void changeColor(BufferedImage image, Color from, Color to, int mode) {
		glDrawer.changeColor(image, from, to, mode);
	}
	@Override public void invert(BufferedImage image) {
		glDrawer.invert(image);		
	}


	private static final Color c1 = new Color( 168,168,168);
	private static final Color c2 = new Color( 192,192,192);
	public void drawTransparencyBG( Rectangle rect) {
		drawTransparencyBG( rect, 4);
	}
	public void drawTransparencyBG( Rectangle rect, int size) {	
		if( rect.isEmpty())
			return;
		
		GLParameters params = new GLParameters(width, height);
		params.flip = flip;
		params.addParam(new GLParameters.GLParam3f("uColor1", c1.getRed()/255.0f, c1.getGreen()/255.0f, c1.getBlue()/255.0f));
		params.addParam(new GLParameters.GLParam3f("uColor2", c2.getRed()/255.0f, c2.getGreen()/255.0f, c2.getBlue()/255.0f));
		params.addParam(new GLParameters.GLParam1i("uSize", size));
		params.useBlendMode = false;
		
		engine.applyPassProgram(ProgramType.GRID, params, null,
				rect.x, rect.y, rect.x + rect.width, rect.y+rect.height, false, gl);
	}
	
	public void _drawBounds( BufferedImage mask, int cycle, AffineTransform trans) {
		GLMultiRenderer glmu = new GLMultiRenderer(width, height, gl);

		// Render the mask to the a screen-shaped surface
		glmu.init();
		glmu.render( new GLRenderer() {
			@Override public void render(GL gl) {
				engine.clearSurface();
				GLParameters params2 = new GLParameters(width, height);
				params2.texture = new GLImageTexture(mask);
				engine.applyPassProgram( ProgramType.PASS_BASIC, params2, trans,
						0, 0, mask.getWidth(), mask.getHeight(), false, gl.getGL2());
			}
		});
		
		// Render the screen-shaped version of the mask using the 
		//	Border-detecting Shader
		GLParameters params = new GLParameters(width, height);
		params.addParam( new GLParam1i("uCycle", cycle));
		params.texture = new GLFBOTexture(glmu);

    	engine.clearSurface();
		engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, true, engine.getGL2());

		// Clean up and Apply the surface to an image
		glmu.cleanup();
	}
}
