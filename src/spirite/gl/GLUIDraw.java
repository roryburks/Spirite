package spirite.gl;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.GLBuffers;

import spirite.gl.GLEngine.ProgramType;
import spirite.gl.GLMultiRenderer.GLRenderer;
import spirite.gl.GLParameters.GLFBOTexture;
import spirite.gl.GLParameters.GLImageTexture;
import spirite.gl.GLParameters.GLParam1i;
import spirite.gl.GLParameters.GLParam4f;

/** 
 * GLUIDraw is a mostly-static class encapsulates some general-purpose 
 * UI Drawing methods which need graphical acceleration to be drawn quickly.
 * 
 * @author Rory Burks
 *
 */
public class GLUIDraw {
	private static final GLEngine engine = GLEngine.getInstance();
	
	enum GradientType {
		RED,
		GREEN,
		BLUE,
		HUE,
		SATURATION,
		VALUE
	}
	
	/** A debug method which demonstrates the GLMultiRender capabilities:
	 * draws a border around the image, then changes that border color to red.
	 */
	public static BufferedImage _ddbounds( 
			BufferedImage bi, int cycle )
	{
		int w = bi.getWidth();
		int h = bi.getHeight();
		GL2 gl = engine.getGL2();
		engine.setSurfaceSize(w, h);

		GLMultiRenderer glmu = new GLMultiRenderer(
				w, h, gl.getGL2());
		glmu.init();
		
		
		glmu.render( new GLRenderer() {
			@Override
			public void render(GL _gl) {
				GLParameters params = new GLParameters(w, h);
				params.addParam( new GLParam1i("uCycle", cycle));
				params.texture = new GLImageTexture(bi);
				engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, false);
			}
		});
		

		GLMultiRenderer glmub = new GLMultiRenderer(
				w, h, gl.getGL2());
		glmub.init();
		glmub.render( new GLRenderer() {
			@Override
			public void render(GL _gl) {
				GLParameters params = new GLParameters(w, h);
				params.addParam( new GLParam1i("uCycle", cycle));
				params.texture = new GLFBOTexture(glmu);
				engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, true);
			}
		});

		GLParameters params = new GLParameters(w, h);

    	params.addParam( new GLParam1i("optionMask", 2));
    	params.addParam( new GLParam4f("cFrom", 0, 0, 0, 1));
    	params.addParam( new GLParam4f("cTo", 1, 0, 0, 1));
    	params.texture = new GLFBOTexture(glmu);
    	
    	engine.clearSurface();
    	engine.applyPassProgram(ProgramType.CHANGE_COLOR, params, null, true);
        
		glmu.cleanup();
		glmub.cleanup();

        BufferedImage im = engine.glSurfaceToImage();
       
		return im;
	}
	

	/**
	 * Draws a border around the given image with the given transform applied to 
	 * it, returns a BufferedImage of dimensions (swidth x sheight).
	 * @param image	The image to draw a border around.
	 * @param cycle	The offset of the cycle in which the dashes are to be drawn
	 * @param trans	The transform to apply to the image to draw it on the screen
	 * @param swidth	The width of the screen to draw it on.
	 * @param sheight	The height of the screen to draw it on.
	 * @return
	 */
	public static BufferedImage drawBounds( 
			BufferedImage image, int cycle, AffineTransform trans, int swidth, int sheight) 
	{
		GLMultiRenderer glmu = new GLMultiRenderer(swidth, sheight, 
				engine.getGL2());
		engine.setSurfaceSize(swidth, sheight);

		// Render the mask to the a screen-shaped surface
		glmu.init();
		glmu.render( new GLRenderer() {
			@Override public void render(GL gl) {
				GLParameters params2 = new GLParameters(swidth, sheight);
				params2.texture = new GLImageTexture(image);
				engine.applyPassProgram( ProgramType.CHANGE_COLOR, params2, trans,
						0, 0, image.getWidth(), image.getHeight(), false);
			}
		});
		
		// Render the screen-shaped version of the mask using the 
		//	Border-detecting Shader
		GLParameters params = new GLParameters(swidth, sheight);
		params.addParam( new GLParam1i("uCycle", cycle));
		params.texture = new GLFBOTexture(glmu);

    	engine.clearSurface();
		engine.applyPassProgram(ProgramType.PASS_BORDER, params, null, true);
		
		// Clean up and Apply the surface to an image
		glmu.cleanup();

        BufferedImage im = engine.glSurfaceToImage();
       
		return im;
	}
	
	public static BufferedImage drawColorGradient( float fixed, GradientType type, int w, int h) {
		engine.setSurfaceSize(w,h);
		
		GL2 gl = engine.getGL2();
		

		FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(
        	new float[] {
    			// x      y     z     w    rsx     rsy
                -1.0f, -1.0f, 0.0f, 1.0f,  0.0f, 0.0f,
                +1.0f, -1.0f, 0.0f, 1.0f,  1.0f, 0.0f,
                -1.0f, +1.0f, 0.0f, 1.0f,  0.0f, 1.0f,
                +1.0f, +1.0f, 0.0f, 1.0f,  1.0f, 1.0f
        	}
        );
		
	    IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1);
	    IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
	    
	    gl.glGenBuffers(1, positionBufferObject);
	    gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, positionBufferObject.get(0));
	    gl.glBufferData(
	    		GL2.GL_ARRAY_BUFFER, 
	    		vertexBuffer.capacity()*Float.BYTES, 
	    		vertexBuffer, 
	    		GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

		gl.glGenVertexArrays(1, vao);
		gl.glBindVertexArray(vao.get(0));
		

	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
		
		
		// Start Draw
		int prog = engine.getProgram(ProgramType.SQARE_GRADIENT);
        gl.glUseProgram( prog);
        int varCol = gl.glGetUniformLocation( prog, "varCol");
        int fixedCol = gl.glGetUniformLocation( prog, "fixedCol");
        
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl.glEnableVertexAttribArray(GLEngine.Attr.POSITION);
        gl.glEnableVertexAttribArray(GLEngine.Attr.COLOR);
        gl.glVertexAttribPointer(GLEngine.Attr.POSITION, 4, GL2.GL_FLOAT, false, 4*6, 0);
        gl.glVertexAttribPointer(GLEngine.Attr.COLOR, 2, GL2.GL_FLOAT, false, 4*6, 4*4);

        gl.glUniform1i( varCol, type.ordinal());
        gl.glUniform1f( fixedCol, fixed);
        
        gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);

		// End Draw
        
        gl.glDisableVertexAttribArray(GLEngine.Attr.POSITION);
        gl.glDisableVertexAttribArray(GLEngine.Attr.COLOR);
        gl.glUseProgram(0);

		gl.glDeleteVertexArrays(1, vao);
		gl.glDeleteBuffers(1, positionBufferObject);
		
        BufferedImage im = engine.glSurfaceToImage();
        return im;
	}
}
