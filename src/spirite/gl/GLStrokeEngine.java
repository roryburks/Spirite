package spirite.gl;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import mutil.MatrixBuilder;
import spirite.gl.GLEngine.PreparedData;
import spirite.gl.GLEngine.ProgramType;
import spirite.pen.PenTraits.PenState;
import spirite.pen.StrokeEngine;

/**
 * The GLStrokeEngine is a StrokeEngine that uses OpenGL to create a particular
 * kind of Stroke.
 * 
 * @author Rory Burks
 *
 */
public class GLStrokeEngine extends StrokeEngine {
	private final GLEngine engine = GLEngine.getInstance();
	
	public GLStrokeEngine() {
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	

	@Override
	public boolean drawToLayer( BufferedImage layer, List<PenState> states) {
		_stroke( composeVBuffer(states));
		

        BufferedImage im = engine.glSurfaceToImage(); 
		Graphics2D g2 = (Graphics2D)layer.getGraphics();
		g2.drawImage(im, 0, 0, null);

		if( sel.selection != null) {
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
			g2.drawImage(selectionMask, 0, 0, null);
		}
		g2.dispose();
		
		return true;
	}
	
	private void _strokeSpore(PenState ps) {

		float[] raw = new float[4*13];
		
		float size = stroke.getDynamics().getSize(ps) * stroke.getWidth();
		
		raw[0] = data.convertX(ps.x);
		raw[1] = data.convertY(ps.y);
		raw[2] = size;
		raw[3] = ps.pressure;

		for( int i=0; i<4; ++i) {
			int off = (i+1)*4;
			raw[off+0] = data.convertX(ps.x) + size/2.0f * (float)Math.cos(Math.PI/2.0*i);
			raw[off+1] = data.convertY(ps.y) + size/2.0f * (float)Math.sin(Math.PI/2.0*i);
			raw[off+2] = stroke.getDynamics().getSize(ps);
			raw[off+3] = ps.pressure;
		}
		for( int i=0; i<8; ++i) {
			int off = (i+5)*4;
			raw[off+0] = data.convertX(ps.x) + size * (float)Math.cos(Math.PI/8.0+Math.PI/4.0*i);
			raw[off+1] = data.convertY(ps.y) + size * (float)Math.sin(Math.PI/8.0+Math.PI/4.0*i);
			raw[off+2] = stroke.getDynamics().getSize(ps);
			raw[off+3] = ps.pressure;
		}
		

		int w = data.getWidth();
		int h = data.getHeight();
		
		engine.setSurfaceSize( w, h);
		GL2 gl = engine.getGL2();
		PreparedData pd = engine.prepareRawData(raw);

		// Clear Surface
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
	    gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);

        int prog = engine.getProgram(ProgramType.STROKE_SPORE);
        gl.glUseProgram( prog);

        // Bind Attribute Streams
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray( 0);
        gl.glEnableVertexAttribArray( 1);
        gl.glEnableVertexAttribArray( 2);
        gl.glVertexAttribPointer( 0, 2, GL2.GL_FLOAT, false, 4*4, 0);
        gl.glVertexAttribPointer( 1, 1, GL2.GL_FLOAT, false, 4*4, 4*2);
        gl.glVertexAttribPointer( 2, 1, GL2.GL_FLOAT, false, 4*4, 4*3);

        // Bind Uniforms
        int u_perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
        	MatrixBuilder.orthagonalProjectionMatrix(-0.5f, w-0.5f, -0.5f, h-0.5f, -1, 1)
        );
        gl.glUniformMatrix4fv(u_perspectiveMatrix, 1, true, orthagonalMatrix);
        int uColor = gl.glGetUniformLocation( prog, "uColor");
        gl.glUniform3f(uColor, 
        		stroke.getColor().getRed()/255.0f,
        		stroke.getColor().getGreen()/255.0f,
        		stroke.getColor().getBlue()/255.0f);


        gl.glEnable(GL2.GL_MULTISAMPLE);
        gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE );
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_MAX);

    	gl.glDrawArrays(GL3.GL_POINTS, 0, 13);
        


        gl.glDisable( GL.GL_BLEND);
        gl.glDisable(GL2.GL_MULTISAMPLE);

        gl.glDisableVertexAttribArray( 0);
        gl.glDisableVertexAttribArray( 1);
        gl.glDisableVertexAttribArray( 2);
        gl.glUseProgram(0);
        
        pd.free();

        BufferedImage im = engine.glSurfaceToImage(); 
//		MUtil.clearImage(strokeLayer);
//		Graphics2D g2 = (Graphics2D)displayLayer.getGraphics();
//		g2.drawImage(im, 0, 0, null);

//		if( sel.selection != null) {
//			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
//			g2.drawImage(selectionMask, 0, 0, null);
//		}
//		g2.dispose();
	}
	
	private class GLVBuffer {
		float[] vBuffer;
		int len;
	}
	
	private static final int DISTANCE_THRESHOLD = 5;
	private GLVBuffer composeVBuffer( List<PenState> states) {
		GLVBuffer vb = new GLVBuffer();
		
		// Step 1: Determine how much space is needed
		int num = states.size() + 2;

		float raw[] = new float[6*(num)];
		int o = 1;	// first point is 0,0,0,0
		for( int i=0; i < states.size(); ++i) {
			int off = (o++)*6;
			
			PenState ps = states.get(i);
			// x y z w
			raw[off+0] = data.convertX(ps.x);
			raw[off+1] = data.convertY(ps.y);
			raw[off+2] = 0.0f;
			raw[off+3] = 1.0f;
			
			// size pressure
			raw[off+4] = stroke.getDynamics().getSize(ps) * stroke.getWidth();
			raw[off+5] = ps.pressure;
			
			if( i == states.size()-1) {
				off = (o++)*6;
				// x y z w
				raw[off+0] = data.convertX(ps.x);
				raw[off+1] = data.convertY(ps.y);
				raw[off+2] = 0.0f;
				raw[off+3] = 1.0f;
				
				// size pressure
				raw[off+4] = stroke.getDynamics().getSize(ps) * stroke.getWidth();
				raw[off+5] = ps.pressure;
				continue;
			}
		}

		vb.vBuffer = raw;
		vb.len = num;
		
		return vb;
	}

	private final static int ATTR_POS = 0;
	private final static int ATTR_SIZE = 1;
	private final static int ATTR_PRESSURE = 2;
	private final static int STRIDE = 6*Float.BYTES;
	/**
	 * Hardware Accelerated Strokes work by feeding a linestrip with position,
	 * size, pressure, and color information.  The vertex shader usually just
	 * passes it to a geometry shader that will expand it into a proper shape
	 * to be filled by the fragment shader.
	 */
	private void _stroke( GLVBuffer glvb) {
		int w = data.getWidth();
		int h = data.getHeight();
		
		engine.setSurfaceSize( w, h);
		GL2 gl = engine.getGL2();
		
		
		PreparedData pd = engine.prepareRawData(glvb.vBuffer);

		// Clear Surface
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);

        int prog = engine.getProgram(ProgramType.BASIC_STROKE);
        gl.glUseProgram( prog);

        // Bind Attribute Streams
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray( ATTR_POS);
        gl.glEnableVertexAttribArray( ATTR_SIZE);
        gl.glEnableVertexAttribArray( ATTR_PRESSURE);
        gl.glVertexAttribPointer( ATTR_POS, 4, GL2.GL_FLOAT, false, STRIDE, 0);
        gl.glVertexAttribPointer( ATTR_SIZE, 1, GL2.GL_FLOAT, false, STRIDE, 4*4);
        gl.glVertexAttribPointer( ATTR_PRESSURE, 1, GL2.GL_FLOAT, false, STRIDE, 4*5);
        
        // Bind Uniforms
        int u_perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
        	MatrixBuilder.orthagonalProjectionMatrix(-0.5f, w-0.5f, -0.5f, h-0.5f, -1, 1)
        );
        gl.glUniformMatrix4fv(u_perspectiveMatrix, 1, true, orthagonalMatrix);
        int uColor = gl.glGetUniformLocation( prog, "uColor");
        gl.glUniform3f(uColor, 
        		stroke.getColor().getRed()/255.0f,
        		stroke.getColor().getGreen()/255.0f,
        		stroke.getColor().getBlue()/255.0f);
        gl.glUniform1f( gl.glGetUniformLocation(prog, "uH"), (float)h);
        

        gl.glEnable(GL2.GL_MULTISAMPLE);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_MAX);

    	gl.glDrawArrays(GL3.GL_LINE_STRIP_ADJACENCY, 0, glvb.len);
        


        gl.glDisable( GL.GL_BLEND);
        gl.glDisable(GL2.GL_MULTISAMPLE);

        gl.glDisableVertexAttribArray( ATTR_POS);
        gl.glDisableVertexAttribArray( ATTR_SIZE);
        gl.glDisableVertexAttribArray( ATTR_PRESSURE);
        gl.glUseProgram(0);
        
        pd.free();
	}
	
}
