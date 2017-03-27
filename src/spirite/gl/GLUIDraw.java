package spirite.gl;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import mutil.MatrixBuilder;
import spirite.gl.GLEngine.PreparedData;
import spirite.gl.GLEngine.PreparedTexture;
import spirite.gl.GLEngine.ProgramType;
import spirite.gl.GLMultiRenderer.GLRenderer;
import spirite.gl.GLParameters.GLParam1i;
import spirite.gl.GLParameters.GLParam4f;

/** 
 * GLUIDraw is a mostly-static (needs to be linked to a non-static GLEngine
 * object) class encapsulates some general-purpose UI Drawing methods which
 * need graphical acceleration to be drawn quickly.
 * 
 * 
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
		GL3 gl = engine.getGL3();
		engine.setSurfaceSize(w, h);

		GLMultiRenderer glmu = new GLMultiRenderer(
				w, h, gl.getGL2());
		glmu.init();
		
		glmu.render( new GLRenderer() {
			@Override
			public void render(GL _gl) {
				GL3 gl = _gl.getGL3();
				
				PreparedData pd = engine.prepareRawData(new float[]{
					// x  y   u   v
					0, 0, 0.0f, 0.0f,
					w, 0, 1.0f, 0.0f,
					0, h, 0.0f, 1.0f,
					w, h, 1.0f, 1.0f,
				});
				
				// Clear Surface
			    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
		        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor);

		        int prog = engine.getProgram(ProgramType.PASS_BORDER);
		        gl.glUseProgram( prog);

		        // Bind Attribute Streams
		        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, pd.getBuffer());
		        gl.glEnableVertexAttribArray( 0);
		        gl.glEnableVertexAttribArray( 1);
		        gl.glVertexAttribPointer( 0, 2, GL3.GL_FLOAT, false, 4*4, 0);
		        gl.glVertexAttribPointer( 1, 2, GL3.GL_FLOAT, false, 4*4, 4*2);
		        
		        //Bind Texture
				PreparedTexture pt = engine.prepareTexture(bi);
				gl.glEnable(GL3.GL_TEXTURE_2D);
				gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);
				
		        // Bind Uniforms
		        int perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
		        int uCycle = gl.glGetUniformLocation( prog, "uCycle");
		        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
		        	MatrixBuilder.orthagonalProjectionMatrix( 0, w, 0, h, -1, 1)
		        );
		        gl.glUniformMatrix4fv(perspectiveMatrix, 1, true, orthagonalMatrix);
		        gl.glUniform1i(uCycle, cycle);

		        // Start Draw
				gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
		        

				// Free
				gl.glDisable(GL3.GL_TEXTURE_2D);
		        gl.glDisableVertexAttribArray( 0);
		        gl.glDisableVertexAttribArray( 1);
		        gl.glUseProgram(0);
		        pt.free();
		        pd.free();
			}
		});


		PreparedData pd = engine.prepareRawData(new float[]{
			// x  y   u   v
			0, 0, 0.0f, 0.0f,
			w, 0, 1.0f, 0.0f,
			0, h, 0.0f, 1.0f,
			w, h, 1.0f, 1.0f,
		});

		// Clear Surface
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor);

    	GLParameters params = new GLParameters();

    	params.addParam( new GLParam1i("optionMask", 2));
    	params.addParam( new GLParam4f("cFrom", 0, 0, 0, 1));
    	params.addParam( new GLParam4f("cTo", 1, 0, 0, 1));
    	
    	int prog = engine.getProgram(ProgramType.CHANGE_COLOR);
        gl.glUseProgram(prog);
        
        // Bind Attribute Streams
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 4*4, 0);
        gl.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 4*4, 4*2);

        // Bind Texture
//        gl.glPushAttrib(GL.GL_TEXTURE_BI);
//        gl.glActiveTexture(inTextureUnitID);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, glmu.getTexture());
        //set the texture up to be used for painting a surface ...
        int textureTarget = GL3.GL_TEXTURE_2D;
        gl.glEnable(textureTarget);
//        gl.glTexEnvi(GL.GL_TEXTURE_ENV,GL3.GL_TEXTURE_ENV_MODE,GL3.GL_MODULATE);
        gl.glTexParameteri(textureTarget,GL3.GL_TEXTURE_MIN_FILTER,GL3.GL_LINEAR);
        gl.glTexParameteri(textureTarget,GL3.GL_TEXTURE_MAG_FILTER,GL3.GL_LINEAR);
        gl.glTexParameteri(textureTarget,GL3.GL_TEXTURE_WRAP_S,GL3.GL_REPEAT);
        gl.glTexParameteri(textureTarget,GL3.GL_TEXTURE_WRAP_T,GL3.GL_REPEAT);
        

		// Bind Uniforms
        int perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
        	MatrixBuilder.orthagonalProjectionMatrix(0, w, 0, h, -1, 1)
        );
        gl.glUniformMatrix4fv(perspectiveMatrix, 1, true, orthagonalMatrix);
        
        if( params != null)
        	params.apply(gl, prog);

		// Start Draw
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL3.GL_ONE, GL3.GL_ONE);
        gl.glBlendEquation(GL3.GL_MAX);
		gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisable( GL.GL_BLEND);
		
		// Finished Drawing
		gl.glDisable(GL3.GL_TEXTURE_2D);
		gl.glDisableVertexAttribArray(0);
		gl.glDisableVertexAttribArray(1);
        gl.glUseProgram(0);
		pd.free();
        
		glmu.cleanup();

		GLAutoDrawable drawable = engine.getDrawable();
        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, w, h, true); 
       
		return im;
	}
	
	/**
	 * Draws a dashed border around the image
	 * @param image
	 * @return
	 */
	public static BufferedImage drawBounds( 
			BufferedImage image, Rectangle UNUSED, int cycle) 
	{
		int w = image.getWidth();
		int h = image.getHeight();
		
		engine.setSurfaceSize( w, h);
		GL3 gl = engine.getGL3();

		PreparedData pd = engine.prepareRawData(new float[]{
			// x  y   u   v
			0, 0, 0.0f, 0.0f,
			w, 0, 1.0f, 0.0f,
			0, h, 0.0f, 1.0f,
			w, h, 1.0f, 1.0f,
		});
		
		// Clear Surface
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor);

        int prog = engine.getProgram(ProgramType.PASS_BORDER);
        gl.glUseProgram( prog);

        // Bind Attribute Streams
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray( 0);
        gl.glEnableVertexAttribArray( 1);
        gl.glVertexAttribPointer( 0, 2, GL3.GL_FLOAT, false, 4*4, 0);
        gl.glVertexAttribPointer( 1, 2, GL3.GL_FLOAT, false, 4*4, 4*2);
        
        //Bind Texture
		PreparedTexture pt = engine.prepareTexture(image);
		gl.glEnable(GL3.GL_TEXTURE_2D);
		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);
		
        // Bind Uniforms
        int perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
        int uCycle = gl.glGetUniformLocation( prog, "uCycle");
        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
        	MatrixBuilder.orthagonalProjectionMatrix( 0, w, 0, h, -1, 1)
        );
        gl.glUniformMatrix4fv(perspectiveMatrix, 1, true, orthagonalMatrix);
        gl.glUniform1i(uCycle, cycle);

        // Start Draw
		gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
        

		// Free
		gl.glDisable(GL3.GL_TEXTURE_2D);
        gl.glDisableVertexAttribArray( 0);
        gl.glDisableVertexAttribArray( 1);
        gl.glUseProgram(0);
        pt.free();
        pd.free();

		GLAutoDrawable drawable = engine.getDrawable();
        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, w, h, true); 
       
		return im;
	}
	
	public static BufferedImage drawColorGradient( float fixed, GradientType type, int w, int h) {
		engine.setSurfaceSize(w,h);
		
		GL3 gl = engine.getGL3();
		

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
	    gl.glBindBuffer( GL3.GL_ARRAY_BUFFER, positionBufferObject.get(0));
	    gl.glBufferData(
	    		GL3.GL_ARRAY_BUFFER, 
	    		vertexBuffer.capacity()*Float.BYTES, 
	    		vertexBuffer, 
	    		GL3.GL_STATIC_DRAW);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

		gl.glGenVertexArrays(1, vao);
		gl.glBindVertexArray(vao.get(0));
		

	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor);
		
		
		// Start Draw
		int prog = engine.getProgram(ProgramType.SQARE_GRADIENT);
        gl.glUseProgram( prog);
        int varCol = gl.glGetUniformLocation( prog, "varCol");
        int fixedCol = gl.glGetUniformLocation( prog, "fixedCol");
        
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl.glEnableVertexAttribArray(GLEngine.Attr.POSITION);
        gl.glEnableVertexAttribArray(GLEngine.Attr.COLOR);
        gl.glVertexAttribPointer(GLEngine.Attr.POSITION, 4, GL3.GL_FLOAT, false, 4*6, 0);
        gl.glVertexAttribPointer(GLEngine.Attr.COLOR, 2, GL3.GL_FLOAT, false, 4*6, 4*4);

        gl.glUniform1i( varCol, type.ordinal());
        gl.glUniform1f( fixedCol, fixed);
        
        gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);

		// End Draw
        
        gl.glDisableVertexAttribArray(GLEngine.Attr.POSITION);
        gl.glDisableVertexAttribArray(GLEngine.Attr.COLOR);
        gl.glUseProgram(0);

		gl.glDeleteVertexArrays(1, vao);
		gl.glDeleteBuffers(1, positionBufferObject);

		GLAutoDrawable drawable = engine.getDrawable();
        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, w, h, true); 
        
        return im;
	}
}
