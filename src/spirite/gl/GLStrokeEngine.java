package spirite.gl;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import mutil.MatrixBuilder;
import spirite.MUtil;
import spirite.gl.GLEngine.PreparedData;
import spirite.gl.GLEngine.ProgramType;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
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
	public boolean startDrawStroke(PenState ps) {
		return false;
	}

	@Override
	public boolean stepDrawStroke(PenState fromState, PenState toState) {
		if( fromState.x == toState.x && fromState.y == toState.y)
			return false;

		_stroke(composeVBufferFromNew(toState));
		return true;
	}
	
	@Override
	public boolean batchDraw(StrokeEngine.StrokeParams stroke, PenState[] states, BuiltImageData data, BuiltSelection mask) {
		super.startStroke(stroke, states[0], data, mask);
		_stroke( composeVBufferFromArray(states));
		return true;
	}
	
	
	private class GLVBuffer {
		float[] vBuffer;
		int len;
	}

	private GLVBuffer composeVBufferFromNew( PenState toStates) {
		GLVBuffer vb = new GLVBuffer();
		
		// Prepare Data as a buffer
		float raw[] = new float[6*(prec.size()+3)];
		
		raw[4] = -1;
		for( int i=0; i<prec.size(); ++i) {
			PenState recState = prec.get(i);
			int off = (i+1)*6;
			// x y z w
			raw[off+0] = data.convertX(recState.x);
			raw[off+1] = data.convertY(recState.y);
			raw[off+2] = 0.0f;
			raw[off+3] = 1.0f;
			
			// size pressure
			raw[off+4] = stroke.getDynamics().getSize(recState) * stroke.getWidth();
			raw[off+5] = recState.pressure;
		}

		int off = (prec.size()+1)*6;
		// x y z w
		raw[off+0] = toStates.x;
		raw[off+1] = toStates.y;
		raw[off+2] = 0.0f;
		raw[off+3] = 1.0f;
		
		// size pressure
		raw[off+4] = stroke.getDynamics().getSize(toStates) * stroke.getWidth();
		raw[off+5] = toStates.pressure;
		
		raw[ (prec.size()+2)*6+4] = -1;
		
		vb.vBuffer = raw;
		vb.len = prec.size()+3;
		
		return vb;
	}
	private GLVBuffer composeVBufferFromArray( PenState[] states) {
		GLVBuffer vb = new GLVBuffer();
		
		// Prepare Data as a buffer
		float raw[] = new float[6*(states.length+2)];
		
		raw[4] = -1;
		for( int i=0; i< states.length; ++i) {
			int off = (i+1)*6;
			// x y z w
			raw[off+0] = data.convertX(states[i].x);
			raw[off+1] = data.convertY(states[i].y);
			raw[off+2] = 0.0f;
			raw[off+3] = 1.0f;
			
			// size pressure
			raw[off+4] = stroke.getDynamics().getSize(states[i]) * stroke.getWidth();
			raw[off+5] = states[i].pressure;
		}
		raw[ (states.length+1)*6+4] = -1;
		
		vb.vBuffer = raw;
		vb.len = states.length+2;
		
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

        BufferedImage im = engine.glSurfaceToImage(); 
		MUtil.clearImage(strokeLayer);
		Graphics2D g2 = (Graphics2D)strokeLayer.getGraphics();
		g2.drawImage(im, 0, 0, null);

		if( sel.selection != null) {
			g2.setComposite( AlphaComposite.getInstance(AlphaComposite.DST_IN));
			g2.drawImage(selectionMask, 0, 0, null);
		}
		g2.dispose();
	}
	
}
