package spirite.graphics.gl;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import mutil.MatrixBuilder;
import spirite.Globals;
import spirite.graphics.GraphicsContext;
import spirite.graphics.awt.AWTContext;
import spirite.graphics.gl.engine.GLEngine;
import spirite.graphics.gl.engine.GLEngine.PreparedData;
import spirite.graphics.gl.engine.GLEngine.ProgramType;
import spirite.graphics.gl.engine.GLMultiRenderer;
import spirite.graphics.gl.engine.GLMultiRenderer.GLRenderer;
import spirite.graphics.gl.engine.GLParameters;
import spirite.pen.PenTraits.PenState;
import spirite.pen.StrokeEngine;

/**
 * The GLStrokeEngine is a StrokeEngine that uses OpenGL to create a particular
 * kind of Stroke.
 * 
 * @author Rory Burks
 *
 */
class GLStrokeEngine extends StrokeEngine {
	private final GLEngine engine = GLEngine.getInstance();
	private GLMultiRenderer fixedLayer;
	private GLMultiRenderer displayLayer;
	private int w, h;
	
	public GLStrokeEngine() {
	}
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
	@Override
	protected void onStart() {
		w = data.getWidth();
		h = data.getHeight();
		fixedLayer = new GLMultiRenderer( w, h, engine.getGL2());
		displayLayer = new GLMultiRenderer( w, h, engine.getGL2());
		
		fixedLayer.init();
		displayLayer.init();
	}
	@Override
	protected void onEnd() {
		fixedLayer.cleanup();
		displayLayer.cleanup();
		fixedLayer = null;
		displayLayer = null;
	}
	

	@Override
	protected void prepareDisplayLayer() {
		engine.getContext().makeCurrent();
		displayLayer.render( new GLRenderer() {
			@Override
			public void render(GL _gl) {
				GL2 gl = _gl.getGL2();
				engine.setSurfaceSize(w, h);
				engine.clearSurface(gl.getGL2());
				gl.glViewport(0, 0, w, h);
				
				
				GLParameters params = new GLParameters(w, h);
				params.texture = new GLParameters.GLFBOTexture(fixedLayer);
				engine.applyPassProgram(ProgramType.PASS_BASIC, params, null, true, engine.getGL2());
			}
		});
	}
	@Override
	protected void drawDisplayLayer(GraphicsContext gc) {
		if( gc instanceof AWTContext) {
			engine.setSurfaceSize(w, h);
			engine.clearSurface(engine.getGL2());
			GLParameters params = new GLParameters(w, h);
			params.texture = new GLParameters.GLFBOTexture(displayLayer);
			engine.applyPassProgram(ProgramType.PASS_BASIC, params, null, true, engine.getGL2());
			
			BufferedImage bi = engine.glSurfaceToImage(Globals.BI_FORMAT);
			gc.drawImage( bi, 0, 0);
		}
		else if( gc instanceof GLGraphics) {
			GLGraphics glgc = (GLGraphics)gc;
			glgc.reset();
			
			GLParameters params = new GLParameters(glgc.getWidth(), glgc.getHeight());
			params.texture = new GLParameters.GLFBOTexture(displayLayer);
			params.flip = glgc.isFlip();
			params.addParam( new GLParameters.GLParam1i("uComp", 0));
			params.addParam( new GLParameters.GLParam1f("uAlpha", glgc.getAlpha()));
			GLGraphics.setCompositeBlend(params, gc.getComposite());
			
			
			engine.applyPassProgram(ProgramType.PASS_RENDER, params, glgc.getTransform(), 
					0, 0, w, h, true, glgc.getGL());
		}
	}
	

	@Override
	protected boolean drawToLayer( List<PenState> states, boolean permanent) {
		GLMultiRenderer glmu = (permanent)?fixedLayer:displayLayer;

		glmu.render( new GLRenderer() {
			@Override public void render(GL gl) {
//				_strokeSpore( states.get(states.size()-1));
				_stroke( composeVBuffer(states), stroke.getHard()?1:0);
			}
		});
		
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
		PreparedData pd = engine.prepareRawData(raw, new int[]{2,1,1}, gl);

		// Clear Surface
        int prog = engine.getProgram(ProgramType.STROKE_SPORE);
        gl.glUseProgram( prog);

        // Bind Attribute Streams
        pd.init();

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

        gl.glUseProgram(0);

        pd.deinit();
        pd.free();
	}
	
	private class GLVBuffer {
		float[] vBuffer;
		int len;
	}
	
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
			
/*			if( i == states.size()-1 && stroke.getMethod() == Method.PIXEL) {
				// TODO: Exagerate last line segment so pixel drawing works as expected
				raw[off+0] = data.convertX(ps.x)+0.5f;
				raw[off+1] = data.convertY(ps.y)+0.5f;
			}*/
		}

		vb.vBuffer = raw;
		vb.len = num;
		
		return vb;
	}

	/**
	 * Hardware Accelerated Strokes work by feeding a linestrip with position,
	 * size, pressure, and color information.  The vertex shader usually just
	 * passes it to a geometry shader that will expand it into a proper shape
	 * to be filled by the fragment shader.
	 */
	private void _stroke( GLVBuffer glvb, int mode) {
		GL2 gl = engine.getGL2();
		int w = data.getWidth();
		int h = data.getHeight();
		
		engine.setSurfaceSize( w, h);
		
		
		
		PreparedData pd = engine.prepareRawData(glvb.vBuffer, new int[]{4,1,1}, gl);

		// Clear Surface
        int prog = 0;
        
        switch( stroke.getMethod()) {
		case BASIC:
		case ERASE:
        	prog = engine.getProgram(ProgramType.STROKE_BASIC);
			break;
		case PIXEL:
        	prog = engine.getProgram(ProgramType.STROKE_PIXEL);
			break;
        
        }
        gl.glUseProgram( prog);

        // Bind Attribute Streams
        pd.init();
        
        // Bind Uniforms
        int u_perspectiveMatrix = gl.glGetUniformLocation( prog, "perspectiveMatrix");
        int uColor = gl.glGetUniformLocation( prog, "uColor");
        int uMode = gl.glGetUniformLocation( prog, "uMode");
        FloatBuffer orthagonalMatrix = GLBuffers.newDirectFloatBuffer(
        	MatrixBuilder.orthagonalProjectionMatrix(-0.5f, w-0.5f, -0.5f, h-0.5f, -1, 1)
        );
        gl.glUniformMatrix4fv(u_perspectiveMatrix, 1, true, orthagonalMatrix);
        gl.glUniform3f(uColor, 
        		stroke.getColor().getRed()/255.0f,
        		stroke.getColor().getGreen()/255.0f,
        		stroke.getColor().getBlue()/255.0f);
        gl.glUniform1f( gl.glGetUniformLocation(prog, "uH"), (float)h);
        gl.glUniform1i( uMode, mode);
        

        gl.glEnable(GL2.GL_MULTISAMPLE);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_MAX);

    	gl.glDrawArrays(GL3.GL_LINE_STRIP_ADJACENCY, 0, glvb.len);
        


        gl.glDisable( GL.GL_BLEND);
        gl.glDisable(GL2.GL_MULTISAMPLE);
        gl.glUseProgram(0);

        pd.deinit();
        pd.free();
	}
	
}
