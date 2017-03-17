package spirite.gl;

import spirite.pen.PenTraits.PenState;

import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.hackoeur.jglm.Vec4;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.glsl.ShaderCode;

import mutil.MatrixBuilder;
import spirite.MDebug;
import spirite.gl.GLEngine.PreparedData;
import spirite.gl.GLEngine.ProgramType;
import spirite.image_data.DrawEngine.StrokeParams;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.SelectionEngine.BuiltSelection;
import spirite.pen.StrokeEngine;

public class GLStrokeEngine extends StrokeEngine {
	private final GLEngine engine = GLEngine.getInstance();
	
	public GLStrokeEngine() {
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	
	@Override
	public boolean startStroke(StrokeParams s, PenState ps, BuiltImageData data, BuiltSelection selection) {
		return super.startStroke(s, ps, data, selection);
	}
	
	@Override
	public boolean startDrawStroke(PenState ps) {
		return false;
	}

	@Override
	public boolean stepDrawStroke(PenState fromState, PenState toState) {
		if( fromState.x == toState.x && fromState.y == toState.y)
			return false;
		
		_stroke( new PenState[] {fromState,toState});
		return true;
	}
	
	@Override
	public boolean batchDraw(StrokeParams stroke, PenState[] states, BuiltImageData data, BuiltSelection mask) {
		super.startStroke(stroke, states[0], data, mask);
		_stroke( states);
		super.endStroke();
		return true;
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
	private void _stroke( PenState[] states) {

		int w = data.getWidth();
		int h = data.getHeight();
		
		engine.setSurfaceSize( w, h);
		GL4 gl = engine.getGL4();
		
		// Prepare Data as a buffer
		float raw[] = new float[6*states.length];
		
		for( int i=0; i< states.length; ++i) {
			int off = i*6;
			// x y z w
			raw[off+0] = states[i].x;
			raw[off+1] = states[i].y;
//			raw[off+0] = 2.0f * states[i].x / (float)w - 1.0f;
//			raw[off+1] = -(2.0f * states[i].y / (float)h - 1.0f);
			raw[off+2] = 0.0f;
			raw[off+3] = 1.0f;
			
			// size pressure
			raw[off+4] = stroke.getDynamics().getSize(states[i]) * stroke.getWidth();
			MDebug.log(""+raw[off+4]);
			raw[off+5] = states[i].pressure;
		}
		
		PreparedData pd = engine.prepareRawData(raw);

		// Clear Surface
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL4.GL_COLOR, 0, clearColor);

        int prog = engine.getProgram(ProgramType.BASIC_STROKE);
        gl.glUseProgram( prog);

        // Bind Attribute Streams
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray( ATTR_POS);
        gl.glEnableVertexAttribArray( ATTR_SIZE);
        gl.glEnableVertexAttribArray( ATTR_PRESSURE);
        gl.glVertexAttribPointer( ATTR_POS, 4, GL4.GL_FLOAT, false, STRIDE, 0);
        gl.glVertexAttribPointer( ATTR_SIZE, 1, GL4.GL_FLOAT, false, STRIDE, 4*4);
        gl.glVertexAttribPointer( ATTR_PRESSURE, 1, GL4.GL_FLOAT, false, STRIDE, 4*5);
        
        // Bind Uniforms
        int u_perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
        	MatrixBuilder.orthagonalProjectionMatrix(0, w, 0, h, -100, 10)
        );
        gl.glUniformMatrix4fv(u_perspectiveMatrix, 1, true, orthagonalMatrix);
        

        gl.glDrawArrays(GL4.GL_LINE_STRIP, 0, states.length);
        

        gl.glDisableVertexAttribArray( ATTR_POS);
        gl.glDisableVertexAttribArray( ATTR_SIZE);
        gl.glDisableVertexAttribArray( ATTR_PRESSURE);
        gl.glUseProgram(0);
        
        pd.free();

		GLAutoDrawable drawable = engine.getDrawable();
        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, data.getWidth(), data.getHeight(), true); 
		Graphics g = strokeLayer.getGraphics();
		g.drawImage(im, 0, 0, null);
		
	}
	
}
