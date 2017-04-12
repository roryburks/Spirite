package spirite.graphics.gl;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;

import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.NodeRenderer;
import spirite.graphics.GraphicsContext;
import spirite.graphics.gl.GLEngine.ProgramType;
import spirite.graphics.gl.GLMultiRenderer.GLRenderer;
import spirite.graphics.gl.GLParameters.GLFBOTexture;
import spirite.graphics.gl.GLParameters.GLImageTexture;
import spirite.graphics.gl.GLParameters.GLParam1i;
import spirite.graphics.gl.GLParameters.GLParam4f;
import spirite.image_data.GroupTree.GroupNode;
import spirite.pen.StrokeEngine;

public class GLGraphics extends GraphicsContext{
	private final GLEngine engine = GLEngine.getInstance();
	
	private final GLAutoDrawable drawable;
	private GL2 gl;
	private int width, height;
	private boolean flip = false;

	GLGraphics( GLAutoDrawable drawable, boolean flip) {
		this.drawable = drawable;
		this.flip = flip;
	}
	public GLGraphics() {
		this.drawable = engine.getAutoDrawable();
	}
	

	public boolean isFlip() {return flip;}
	public int getWidth() {return width;}
	public int getHeight() { return height;}
	
	public GL2 getGL() { reset();return gl;}
	
	private void reset() {
		this.width = drawable.getSurfaceWidth();
		this.height = drawable.getSurfaceHeight();
		this.gl = drawable.getGL().getGL2();
		drawable.getContext().makeCurrent();
		gl.getGL2().glViewport(0, 0, width, height);
	}

	@Override
	public void drawBounds(BufferedImage mask, int c, AffineTransform trans) {
		reset();

		GLMultiRenderer glmu = new GLMultiRenderer(width, height, gl);
		engine.setSurfaceSize(width, height);

		// Render the mask to the a screen-shaped surface
		glmu.init();
		glmu.render( new GLRenderer() {
			@Override public void render(GL gl) {
				GL2 gl2 = gl.getGL2();
				engine.clearSurface(gl2);
				GLParameters params2 = new GLParameters(width, height);
				params2.texture = new GLImageTexture(mask);
				engine.applyPassProgram( ProgramType.PASS_BASIC, params2, trans,
						0, 0, mask.getWidth(), mask.getHeight(), false, gl.getGL2());
			}
		});

		// Clean up and Apply the surface to an image
		GLParameters params2 = new GLParameters(width, height);
		params2.addParam( new GLParam1i("uCycle", c));
		params2.texture = new GLFBOTexture(glmu);
		engine.applyPassProgram( ProgramType.PASS_BORDER, params2, null, false, gl.getGL2());
		
		glmu.cleanup();
	}


	private static final Color c1 = new Color( 168,168,168);
	private static final Color c2 = new Color( 192,192,192);
	public void drawTransparencyBG( Rectangle rect) {
		reset();
		drawTransparencyBG( rect, 4);
	}
	public void drawTransparencyBG( Rectangle rect, int size) {	
		if( rect.isEmpty())
			return;
		reset();
		
		GLParameters params = new GLParameters(width, height);
		params.flip = isFlip();
		params.addParam(new GLParameters.GLParam3f("uColor1", c1.getRed()/255.0f, c1.getGreen()/255.0f, c1.getBlue()/255.0f));
		params.addParam(new GLParameters.GLParam3f("uColor2", c2.getRed()/255.0f, c2.getGreen()/255.0f, c2.getBlue()/255.0f));
		params.addParam(new GLParameters.GLParam1i("uSize", size));
		params.useBlendMode = false;
		
		engine.applyPassProgram(ProgramType.GRID, params, null,
				rect.x, rect.y, rect.x + rect.width, rect.y+rect.height, false, gl);
	}
	
	public void _drawBounds( BufferedImage mask, int cycle, AffineTransform trans) {
		reset();
		GLMultiRenderer glmu = new GLMultiRenderer(width, height, gl);

		// Render the mask to the a screen-shaped surface
		glmu.init();
		glmu.render( new GLRenderer() {
			@Override public void render(GL gl) {
				engine.clearSurface(gl.getGL2());
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

    	engine.clearSurface(gl);
		engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, true, engine.getGL2());

		// Clean up and Apply the surface to an image
		glmu.cleanup();
	}
	@Override
	public void clear() {
		reset();
		engine.clearSurface(gl);
	}
}
