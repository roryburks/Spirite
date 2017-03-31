package spirite.gl;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import mutil.Interpolation.CubicSplineInterpolator2D;
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

		_stroke(composeVBuffer(prec));
		return true;
	}
	
	@Override
	public boolean batchDraw(StrokeEngine.StrokeParams stroke, PenState[] states, BuiltImageData data, BuiltSelection mask) {
		super.startStroke(stroke, states[0], data, mask);
		_stroke( composeVBuffer(Arrays.asList(states)));
		return true;
	}
	
	
	private class GLVBuffer {
		float[] vBuffer;
		int len;
	}
	
	private static final int DISTANCE_THRESHOLD = 5;
	private GLVBuffer composeVBuffer( List<PenState> states) {
		GLVBuffer vb = new GLVBuffer();
		CubicSplineInterpolator2D csi = null;
		
		// Step 1: Determine how much space is needed
		int num = states.size() + 2;
		for( int i=0; i < states.size()-1; ++i) {
			PenState ps1 = states.get(i);
			PenState ps2 = states.get(i+1);
			
			double dist = MUtil.distance(ps1.x, ps1.y, ps2.x, ps2.y);
			
			if( dist >= DISTANCE_THRESHOLD) {
				num += ((int)dist)/DISTANCE_THRESHOLD;
			}
		}
		
		if( num != states.size()+1) {
			List<Point2D> points = new ArrayList<>(states.size());
			for( int i=0; i < states.size(); ++i) {
				PenState ps = states.get(i);
				points.add(new Point(ps.x, ps.y));
			}
			csi = new CubicSplineInterpolator2D(points, true);
		}
		
		float raw[] = new float[6*(num)];
		int o = 1;
		for( int i=0; i < states.size()-1; ++i) {
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
			
			PenState psNext = states.get(i+1);
			double dist = MUtil.distance(ps.x, ps.y, psNext.x, psNext.y);
			int n = ((int)dist)/DISTANCE_THRESHOLD;
			
			for( int j=0; j < n; ++j) {
				off = (o++)*6;
				
				Point2D p2 = csi.f(i + j/(double)n);
				// x y z w
				raw[off+0] = data.convertX((int)Math.round(p2.getX()));
				raw[off+1] = data.convertY((int)Math.round(p2.getY()));
				raw[off+2] = 0.0f;
				raw[off+3] = 1.0f;
				
				// size pressure
				raw[off+4] = stroke.getDynamics().getSize(ps) * stroke.getWidth();
				raw[off+5] = ps.pressure * (j/(float)n) + psNext.pressure * (1-j/(float)n);
				
			}
		}

		vb.vBuffer = raw;
		vb.len = num-1;
		
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
