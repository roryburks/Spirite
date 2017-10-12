package spirite.base.graphics.gl;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.gl.GLEngine.PreparedData;
import spirite.base.graphics.gl.GLEngine.ProgramType;
import spirite.base.graphics.gl.GLGeom.Primitive;
import spirite.base.pen.PenTraits.PenState;
import spirite.base.pen.StrokeEngine;
import spirite.base.util.Colors;
import spirite.base.util.MatrixBuilder;
import spirite.base.util.glmath.GLC;
import spirite.base.util.glmath.MatTrans;
import spirite.hybrid.HybridHelper;
import spirite.pc.PCUtil;
import spirite.pc.graphics.ImageBI;
import spirite.pc.graphics.awt.AWTContext;

/**
 * The GLStrokeEngine is a StrokeEngine that uses OpenGL to create a particular
 * kind of Stroke.
 * 
 * @author Rory Burks
 *
 */
class GLStrokeEngine extends StrokeEngine {
	private final GLEngine engine = GLEngine.getInstance();
	private GLImage fixedLayer;
	private GLImage displayLayer;
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
		fixedLayer = new GLImage( w, h);
		displayLayer = new GLImage( w, h);
	}
	@Override
	protected void onEnd() {
		fixedLayer.flush();
		displayLayer.flush();
		fixedLayer = null;
		displayLayer = null;
	}
	

	@Override
	protected void prepareDisplayLayer() {
		GL2 gl = engine.getGL2();
		
		GLGraphics glgc = displayLayer.getGraphics();
//		engine.setTarget(displayLayer);
		glgc.clear();
		
		GLParameters params = new GLParameters(w, h);
		params.texture = new GLParameters.GLImageTexture(fixedLayer);
		glgc.applyPassProgram(ProgramType.PASS_BASIC, params, null);
//		engine.setTarget(0);
	}
	@Override
	protected void drawDisplayLayer(GraphicsContext gc) {
		if( gc instanceof AWTContext) {
			GLImage img = new GLImage(w, h);
			GLGraphics glgc = img.getGraphics();
			
			glgc.clear();
			GLParameters params = new GLParameters(w, h);
			params.texture = new GLParameters.GLImageTexture(displayLayer);
			glgc.applyPassProgram(ProgramType.PASS_BASIC, params, null);
			
			BufferedImage bi = PCUtil.glSurfaceToImage(
					HybridHelper.BI_FORMAT, engine.getWidth(), engine.getHeight());
			gc.drawImage( new ImageBI(bi), 0, 0);
			engine.setTarget(0);
		}
		else if( gc instanceof GLGraphics) {
			GLGraphics glgc = (GLGraphics)gc;
//			glgc.reset();
			
			GLParameters params = new GLParameters(glgc.getWidth(), glgc.getHeight());
			params.texture = new GLParameters.GLImageTexture(displayLayer);
			params.flip = glgc.isFlip();
			params.addParam( new GLParameters.GLParam1i("uComp", 0));
			params.addParam( new GLParameters.GLParam1f("uAlpha", glgc.getAlpha()));
			GLGraphics.setCompositeBlend(params, gc.getComposite());
			
			
			glgc.applyPassProgram(ProgramType.PASS_RENDER, params, glgc.getTransform(), 
					0, 0, w, h);
		}
	}
	
	

	@Override
	protected boolean drawToLayer( List<PenState> states, boolean permanent) {
		GLImage drawTo = (permanent)?fixedLayer:displayLayer;
		
		engine.setTarget(drawTo);
		_stroke( composeVBuffer(states), stroke.isHard()?1:0);
		
		if( stroke.getMethod() == Method.BASIC) {
			GLParameters params = new GLParameters(w, h);
			params.texture = new GLParameters.GLImageTexture(drawTo);
			
			engine.applyPassProgram(ProgramType.STROKE_AFTERPASS_INTENSIFY, params, new MatTrans(), 
					0, 0, w, h);
		}
		
		engine.setTarget(0);
		
		return true;
	}
	
	private void _strokeSpore(PenState ps) {
		float[] raw = new float[4*13];
		float x = ps.x;
		float y = ps.y;
		
		float size = stroke.getDynamics().getSize(ps) * stroke.getWidth();
		
		raw[0] = data.convertX(x);
		raw[1] = data.convertY(y);
		raw[2] = size;
		raw[3] = ps.pressure;

		for( int i=0; i<4; ++i) {
			int off = (i+1)*4;
			raw[off+0] = data.convertX(x) + size/2.0f * (float)Math.cos(Math.PI/2.0*i);
			raw[off+1] = data.convertY(y) + size/2.0f * (float)Math.sin(Math.PI/2.0*i);
			raw[off+2] = stroke.getDynamics().getSize(ps);
			raw[off+3] = ps.pressure;
		}
		for( int i=0; i<8; ++i) {
			int off = (i+5)*4;
			raw[off+0] = data.convertX(x) + size * (float)Math.cos(Math.PI/8.0+Math.PI/4.0*i);
			raw[off+1] = data.convertY(y) + size * (float)Math.sin(Math.PI/8.0+Math.PI/4.0*i);
			raw[off+2] = stroke.getDynamics().getSize(ps);
			raw[off+3] = ps.pressure;
		}
		

		int w = data.getWidth();
		int h = data.getHeight();
		
		GL2 gl = engine.getGL2();
		PreparedData pd = engine.prepareRawData(raw, new int[]{2,1,1});

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
        int c = stroke.getColor();
        gl.glUniform3f(uColor, 
        		Colors.getRed(c)/255.0f,
        		Colors.getGreen(c)/255.0f,
        		Colors.getBlue(c)/255.0f);


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

	private final static int BASIC_STRIDE = 3;
	private GLVBuffer composeVBuffer( List<PenState> states) {
		GLVBuffer vb = new GLVBuffer();
		
		// Step 1: Determine how much space is needed
		int num = states.size() + 2;

		float raw[] = new float[BASIC_STRIDE*(num)];
		int o = 1;	// first point is 0,0,0,0
		for( int i=0; i < states.size(); ++i) {
			int off = (o++)*BASIC_STRIDE;
			
			PenState ps = states.get(i);
			// x y z w
			raw[off+0] = data.convertX(ps.x);
			raw[off+1] = data.convertY(ps.y);
			
			// size pressure
			raw[off+2] = stroke.getDynamics().getSize(ps) * stroke.getWidth();
//			raw[off+3] = ps.pressure;
			
/*			if( i == states.size()-1 && stroke.getMethod() == Method.PIXEL) {
				// TODO: Exagerate last line segment so pixel drawing works as expected
				raw[off+0] = data.convertX(ps.x)+0.5f;
				raw[off+1] = data.convertY(ps.y)+0.5f;
			}*/
		}

		raw[0] = raw[BASIC_STRIDE];
		raw[1] = raw[BASIC_STRIDE+1];
		raw[(1 + states.size())*BASIC_STRIDE] = raw[(states.size())*BASIC_STRIDE];
		raw[(1 + states.size())*BASIC_STRIDE+1] = raw[(states.size())*BASIC_STRIDE+1];

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

		GLParameters params = new GLParameters(w, h);

		int c = stroke.getColor();
		params.addParam( new GLParameters.GLParam3f("uColor", 
				Colors.getRed(c)/255.0f,
				Colors.getGreen(c)/255.0f,
				Colors.getBlue(c)/255.0f));
		params.addParam( new GLParameters.GLParam1i("uMode", mode));
		params.addParam( new GLParameters.GLParam1f("uH", h));
		params.setBlendMode(GLC.GL_SRC_ALPHA, GLC.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_MAX);

		if( HybridHelper.getGLCore().getShaderVersion() >= 330) {
			params.addParam( new GLParameters.GLParam1f("uH", h));

			if( stroke.getMethod() == Method.PIXEL) {
				engine.applyPrimitiveProgram( ProgramType.STROKE_PIXEL, params, new Primitive(
						glvb.vBuffer, new int[]{2,1}, GL3.GL_LINE_STRIP_ADJACENCY, new int[]{glvb.len}));
			}
			else {
				Primitive[] prims = GLGeom.strokeV2LinePassGeom( glvb.vBuffer);

//				params.texture = new ;
				params.addParam( new GLParameters.GLParam1f("uAlpha", 1));
				engine.applyPrimitiveProgram( ProgramType.POLY_RENDER, params, prims[1]);
				engine.applyPrimitiveProgram( (stroke.isHard())? ProgramType.STROKE_PIXEL : ProgramType.STROKE_V2_LINE_PASS, params, prims[0]);
			}
		}
		else {
			ProgramType type = (stroke.getMethod() ==Method.PIXEL) 
					? ProgramType.STROKE_PIXEL
					: ProgramType.STROKE_BASIC;
			engine.applyPrimitiveProgram(type, params, GLGeom.strokeBasicGeom(glvb.vBuffer, h));
		}
	}
	
}
