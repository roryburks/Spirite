package spirite.gl;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.Random;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import mutil.MatrixBuilder;
import spirite.gl.GLEngine.PreparedData;
import spirite.gl.GLEngine.PreparedTexture;
import spirite.gl.GLEngine.ProgramType;
import spirite.gl.GLParameters.GLParam1i;
import spirite.gl.GLParameters.GLParam4f;


/**
 * 
 * @author Guy
 *
 */
public class JOGLDrawer {
	private final GLEngine engine = GLEngine.getInstance();
	
	public JOGLDrawer() {
	}

    static int width = 640; 
    static int height = 480; 
    static int numPoints = 1000; 
    static Random r = new Random(); 
    
    private void applyProgram( BufferedImage bi, int prog, GLParameters params){
		GL3 gl = engine.getGL3();
		
		int w = bi.getWidth();
		int h = bi.getHeight();
		engine.setSurfaceSize(w, h);

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
        
        gl.glUseProgram(prog);
        
        // Bind Attribute Streams
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 4*4, 0);
        gl.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 4*4, 4*2);

        // Bind Texture
		PreparedTexture pt = engine.prepareTexture(bi);
		gl.glEnable(GL3.GL_TEXTURE_2D);
		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);

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
        pt.free();
		pd.free();
		
		GLAutoDrawable drawable = engine.getDrawable();
        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, w, h, true); 
        
		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(im, 0, 0, null);
    }

    /**
     * @param options 0th bit: 0 = ignore alpha, 1 = test alpha
     */
    public void changeColor( BufferedImage bi, Color from, Color to, int options) {
    	GLParameters params = new GLParameters();

    	params.addParam( new GLParam1i("optionMask", options));
    	params.addParam( new GLParam4f("cFrom", 
    			from.getRed()/255f, from.getGreen()/255f, from.getBlue()/255f, from.getAlpha()/255f));
    	params.addParam( new GLParam4f("cTo", 
    			to.getRed()/255f, to.getGreen()/255f, to.getBlue()/255f, to.getAlpha()/255f));
    	
    	applyProgram(bi, engine.getProgram(ProgramType.CHANGE_COLOR), params);
    }
    
    public void invert( BufferedImage bi) {
    	applyProgram( bi, engine.getProgram(ProgramType.PASS_INVERT), null);
    }
}
