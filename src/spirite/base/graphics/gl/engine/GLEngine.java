package spirite.base.graphics.gl.engine;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.hackoeur.jglm.Mat4;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;

import spirite.base.graphics.GraphicsContext.CapMethod;
import spirite.base.graphics.GraphicsContext.JoinMethod;
import spirite.base.graphics.gl.engine.GLEngine.ProgramType;
import spirite.base.graphics.gl.engine.GLMultiRenderer.GLRenderer;
import spirite.base.util.MatrixBuilder;
import spirite.hybrid.Globals;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

/**
 * GLEngine is the root point for dealing with OpenGL through JOGL.  It handles
 * the initialization of surfaces and components and manages the resources, 
 * making sure that they are properly de-allocated inside OpenGL
 * 
 * Uses a Singleton paradigm that can be accessed through GLEngine.getInstance()
 * 
 * TODO: I do want to make some effort to support machines running earlier GL 
 * versions (GL2).
 * 
 * Geometry Shaders and LINE_ADJACENCY_STRIPS for the stroke engine could easily
 * be done in software.  Replacing Framebuffers with some other method would not
 * be difficult but might be extremely slow.
 * 
 * NOTE:  Care should be taken to make sure
 * GLFunctionality does not leak beyond the two packages so that functions that require
 * OpenGL are kept explicit and modular (so that you don't try to access OpenGL
 * when it's not available)
 * 
 * TODO: Make _doComplexLineProg behave more as expected.
 * 
 * @author Rory Burks
 */
public class GLEngine  {
	private final GLOffscreenAutoDrawable drawable;
	int width = 1;
	int height = 1;

	private static GLEngine singly = null;
	public static GLEngine getInstance() {
		if( singly == null) MDebug.handleError(ErrorType.STRUCTURAL_MAJOR, "Attempted to get GL Engine that hasn't been initialized.");
		return singly;
	}
	
	/** Attempts to initialize the GLEngine, throwing an exception if it fails. */
	public static void initialize() 
		throws MGLException
	{
		if( singly == null)
			singly = new GLEngine();
	}
	public static class MGLException extends Exception {
		public MGLException(String message) {
			super(message);
		}
	}
	
	private GLEngine() throws MGLException {
		System.out.println("GL Creation");
		// Create Offscreen OpenGl Surface
		GLProfile profile = GLProfile.getDefault();
        GLDrawableFactory fact = GLDrawableFactory.getFactory(profile);
		GLCapabilities caps = new GLCapabilities(profile);
        caps.setHardwareAccelerated(true); 
        caps.setDoubleBuffered(false); 
        caps.setAlphaBits(8); 
        caps.setRedBits(8); 
        caps.setBlueBits(8); 
        caps.setGreenBits(3); 
        caps.setOnscreen(false);

		drawable = fact.createOffscreenAutoDrawable(
				fact.getDefaultDevice(), 
				caps,
				new DefaultGLCapabilitiesChooser(), 
				1, 1);
		
		ex = null;
		
		// Debug
		drawable.addGLEventListener( new GLEventListener() {
			@Override public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {}
			@Override public void dispose(GLAutoDrawable arg0) {
				System.out.println("DISPOSE_ENGINE");
			}
			@Override public void display(GLAutoDrawable arg0) {}
			@Override public void init(GLAutoDrawable gad) {
				try {
				initShaders();
				} catch (MGLException e) {
					ex = e;
				}
				//GL gl = gad.getGL();
				//gl = gl.getContext().setGL(  GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err }) );
			}
		});	

		drawable.display();
		
		if( ex != null) throw ex;
	}
	private static MGLException ex = null;
		
	public final GLRenderer clearRenderer = new GLRenderer() {
		@Override public void render(GL gl) {
			clearSurface(gl.getGL2());
		}
	};
	
	// ============
	// ==== Simple API
	public void setSurfaceSize( int width, int height) {
		if( this.width != width || this.height != height) {
			this.width = width;
			this.height = height;

			this.drawable.setSurfaceSize(width, height);
		}
	}

	public GLContext getContext() {
		return drawable.getContext();
	}
	
	public GL2 getGL2() {
		drawable.getContext().makeCurrent();
		return drawable.getGL().getGL2();
	}
	
	public GLAutoDrawable getAutoDrawable() {
		return drawable;
	}

	/** Writes the active GL Surface to a BufferedImage */
	public BufferedImage glSurfaceToImage(int type) {
		GL2 gl = getGL2();
		BufferedImage bi = null;
//		long time;
		
//		time = System.currentTimeMillis();
		
		switch( type) {
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE:
		bi = new BufferedImage(width, height, type);
		
		IntegerInterleavedRaster iir = (IntegerInterleavedRaster)bi.getRaster();
		IntBuffer ib = IntBuffer.wrap(iir.getDataStorage());
		
		gl.glReadPixels( 0, 0, width, height, 
				GL2.GL_BGRA,
				GL2.GL_UNSIGNED_INT_8_8_8_8_REV,
				ib);
		break;
		default: 
			return null;
		}
		
		// Flip Vertically
		if( true) {
			final WritableRaster raster = bi.getRaster();
			Object scanline1 = null;
			Object scanline2 = null;
			for( int i=0; i<bi.getHeight()/2; ++i) {
				scanline1 = raster.getDataElements(0, i, bi.getWidth(), 1, scanline1);
				scanline2 = raster.getDataElements(0, bi.getHeight()-i-1, bi.getWidth(), 1, scanline2);
				raster.setDataElements(0, i, bi.getWidth(), 1, scanline2);
				raster.setDataElements(0, bi.getHeight()-i-1, bi.getWidth(), 1, scanline1);
			}
		}
//		time = System.currentTimeMillis();
//		bi = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
//        		.readPixelsToBufferedImage( getGL2(), 0, 0, width, height, true);
//        System.out.println("AWT: " + (System.currentTimeMillis()-time));
        
		return bi;
		
	}

    public static final FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
	public void clearSurface(GL2 gl2) {
        gl2.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
	}
	
	// =================
	// ==== Program Management
	public enum ProgramType {
		DEFAULT,
		SQARE_GRADIENT,
		CHANGE_COLOR,
		GRID,
		
		PASS_BASIC,
		PASS_BORDER,
		PASS_INVERT,
		PASS_RENDER,
		PASS_ESCALATE, 
		
		STROKE_SPORE,
		STROKE_BASIC, 
		STROKE_PIXEL, 
		
		POLY_RENDER,
		LINE_RENDER,
		;
	}
	private void setDefaultBlendMode(GL2 gl, ProgramType type) {
        switch( type) {
		case STROKE_BASIC:
		case STROKE_PIXEL:
		case POLY_RENDER:
		case LINE_RENDER:
		case DEFAULT:
		case PASS_RENDER:
		case PASS_BASIC:
		case GRID:
		case PASS_ESCALATE:
	        gl.glEnable(GL.GL_BLEND);
	        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
	        gl.glBlendEquation(GL2.GL_FUNC_ADD);
	        break;
		case CHANGE_COLOR:
		case PASS_INVERT:
		case SQARE_GRADIENT:
		case STROKE_SPORE:
	        gl.glEnable(GL.GL_BLEND);
	        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE);
	        gl.glBlendEquation(GL2.GL_MAX);
	        break;
		case PASS_BORDER:
	        gl.glEnable(GL.GL_BLEND);
	        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	        gl.glBlendEquation(GL2.GL_FUNC_ADD);
        
        }
	}
	
	private int programs[] = new int[ProgramType.values().length];
	
	public int getProgram( ProgramType type){
		return programs[type.ordinal()];
	}

	/**
	 * Applies an OpenGL program to the active GL Surface.
	 * 
	 * @param type		Program to Apply
	 * @param params	Class containing the Parameters to apply
	 * @param trans		Transform to apply to the texture (applied in Screen-space).
	 * 		If null (or is an Identity Transform), then the texture is drawn stretched 
	 * 		over the GL Surface
	 * @param internal	Whether or not it is a GL->GL draw 
	 * 		(if so the texture will not be flipped vertically)
	 */
	public void applyPassProgram(
			ProgramType type,
			GLParameters params, 
			AffineTransform trans, 
			boolean internal,
			GL2 gl)
	{
		applyPassProgram( type, params, trans, 0, 0, params.width, params.height, internal, gl);
	}

	/**
	 * Applies an OpenGL program to the active GL Surface.
	 * 
	 * @param type		Program to Apply
	 * @param params	Class containing the Parameters to apply
	 * @param trans		Transform to apply to the texture (applied in Screen-space).
	 * 		If null (or is an Identity Transform), then the texture is drawn stretched 
	 * 		over the GL Surface
	 * @param x1		Coordinates for where to draw the texture inside the GL Screen
	 * 		space.  For example if param.width/height was 1000, 1000 and you were drawing
	 * 		a 128x128 Texture, using x1, y1, x2, y2 = 0, 0, 128, 128 would draw the texture
	 * 		in the top-left of the GL Screen without any form of stretching
	 * @param y1 "
	 * @param x2 "
	 * @param y2 "
	 * @param internal	Whether or not it is a GL->GL draw 
	 * 		(if so the texture will not be flipped vertically)
	 */
	public void applyPassProgram(
			ProgramType type, 
			GLParameters params, 
			AffineTransform trans,
			float x1, float y1, float x2, float y2,
			boolean internal,
			GL2 gl)
	{
		addOrtho(params, trans);

		PreparedData pd = prepareRawData(new float[]{
			// x  y   u   v
			x1, y1, 0.0f, (internal)?1.0f:0.0f,
			x2, y1, 1.0f, (internal)?1.0f:0.0f,
			x1, y2, 0.0f, (internal)?0.0f:1.0f,
			x2, y2, 1.0f, (internal)?0.0f:1.0f,
		}, new int[]{2,2}, gl);
        applyProgram( type, params, pd, gl, GL2.GL_TRIANGLE_STRIP, 4);
        pd.free();
        params.clearInternalParams();
	}

	public void applyLineProgram( ProgramType type, int[] xPoints, int[] yPoints, 
			int numPoints, GLParameters params, AffineTransform trans, GL2 gl) 
	{
		addOrtho(params, trans);

        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = xPoints[i];
        	data[i*2+1] = yPoints[i];
        }
        
		PreparedData pd = prepareRawData(data, new int[]{2}, gl);
		applyProgram( type, params, pd, gl, GL2.GL_LINE_STRIP, numPoints);
		pd.free();
        params.clearInternalParams();
	}
	
	public void applyLineProgram( ProgramType type, float[] xPoints, float[] yPoints, 
			int numPoints, GLParameters params, AffineTransform trans, GL2 gl) 
	{
		addOrtho(params, trans);
        
        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = xPoints[i];
        	data[i*2+1] = yPoints[i];
        }
		PreparedData pd = prepareRawData(data, new int[]{2}, gl);
		applyProgram( type, params, pd, gl, GL2.GL_LINE_STRIP, numPoints);
		pd.free();
        params.clearInternalParams();
	}

	/**
	 * Draws a complex line that transforms the line description into a geometric
	 * shape by combining assorted primitive renders to create the specified 
	 * join/cap methods.
	 * 
	 * @param xPoints	Array containing the x coordinates.
	 * @param yPoints 	Array containing the x coordinates.
	 * @param numPoints	Number of points to use for the render.
	 * @param cap	How to draw the end-points.
	 * @param join	How to draw the line joints.
	 * @param loop	Whether or not to close the loop by joining the two end points 
	 * 	together (cap is ignored if this is true because the curve has no end points)
	 * @param width	Width of the line.
	 * @param params	GLParameters describing the GL Attributes to use
	 * @param trans		Transform to apply to the rendering.
	 * @param gl
	 */
	public void applyComplexLineProgram( int[] xPoints, int[] yPoints, 
			int numPoints, CapMethod cap, JoinMethod join, boolean loop, float width,
			GLParameters params, AffineTransform trans, GL2 gl) 
	{
		addOrtho(params, trans);
		
		int size = numPoints+(loop?3:2);
		float data[] = new float[2*size];
		for(int i=1; i< numPoints+1; ++i) {
			data[i*2] = xPoints[i-1];
			data[i*2+1] = yPoints[i-1];
		}
		if( loop) {
			data[0] = xPoints[numPoints-1];
			data[1] = yPoints[numPoints-1];
			data[2*(numPoints+1)] = xPoints[0];
			data[2*(numPoints+1)+1] = yPoints[0];	
			if( numPoints > 2) {
				data[2*(numPoints+2)] = xPoints[1];
				data[2*(numPoints+2)+1] = yPoints[1];
			}
		}
		else {
			data[0] = xPoints[0];
			data[1] = yPoints[0];
			data[2*(numPoints+1)] = xPoints[numPoints-1];
			data[2*(numPoints+1)+1] = yPoints[numPoints-1];	
		}
		
		PreparedData pd = prepareRawData(data, new int[]{2}, gl);
		_doCompliexLineProg( pd, size, cap, join, width, params, trans, gl);
		pd.free();
        params.clearInternalParams();
	}
	

	/**
	 * Draws a complex line that transforms the line description into a geometric
	 * shape by combining assorted primitive renders to create the specified 
	 * join/cap methods.
	 * 
	 * @param xPoints	Array containing the x coordinates.
	 * @param yPoints 	Array containing the x coordinates.
	 * @param numPoints	Number of points to use for the render.
	 * @param cap	How to draw the end-points.
	 * @param join	How to draw the line joints.
	 * @param loop	Whether or not to close the loop by joining the two end points 
	 * 	together (cap is ignored if this is true because the curve has no end points)
	 * @param width	Width of the line.
	 * @param params	GLParameters describing the GL Attributes to use
	 * @param trans		Transform to apply to the rendering.
	 * @param gl
	 */
	public void applyComplexLineProgram( float[] xPoints, float[] yPoints, 
			int numPoints, CapMethod cap, JoinMethod join, boolean loop, float width,
			GLParameters params, AffineTransform trans, GL2 gl) 
	{
		// NOTE: identical code to above but without implicit casting
		addOrtho(params, trans);

		int size = numPoints+(loop?3:2);
		float data[] = new float[2*size];
		for(int i=1; i< numPoints+1; ++i) {
			data[i*2] = xPoints[i-1];
			data[i*2+1] = yPoints[i-1];
		}
		if( loop) {
			data[0] = xPoints[numPoints-1];
			data[1] = yPoints[numPoints-1];
			data[2*(numPoints+1)] = xPoints[0];
			data[2*(numPoints+1)+1] = yPoints[0];	
			if( numPoints > 2) {
				data[2*(numPoints+2)] = xPoints[1];
				data[2*(numPoints+2)+1] = yPoints[1];
			}
		}
		else {
			data[0] = xPoints[0];
			data[1] = yPoints[0];
			data[2*(numPoints+1)] = xPoints[numPoints-1];
			data[2*(numPoints+1)+1] = yPoints[numPoints-1];	
		}
		
		PreparedData pd = prepareRawData(data, new int[]{2}, gl);
		_doCompliexLineProg( pd, size, cap, join, width, params, trans, gl);
		pd.free();
        params.clearInternalParams();
	}
	
	private void _doCompliexLineProg( PreparedData pd, int count, CapMethod cap, 
			JoinMethod join, float width, GLParameters params, AffineTransform trans, GL2 gl)
	{
		// TODO: implement Rounded joins and all cap methods
		int uJoin = 0;
		switch( join) {
			case BEVEL: uJoin = 2;break;
			case MITER: uJoin = 1; break;
			case ROUNDED:break;
		}

		gl.glEnable(GL2.GL_MULTISAMPLE );
		params.addInternalParam(new GLParameters.GLParam1i("uJoin",uJoin));
		params.addInternalParam(new GLParameters.GLParam1f("uWidth", width/ 2.0f));
		applyProgram( ProgramType.LINE_RENDER, params, pd, gl, GL3.GL_LINE_STRIP_ADJACENCY, count);
		gl.glDisable(GL2.GL_MULTISAMPLE );
        params.clearInternalParams();
	}

	public enum PolyType {
		STRIP(GL.GL_TRIANGLE_STRIP), 
		FAN(GL.GL_TRIANGLE_FAN), 
		LIST(GL.GL_TRIANGLES);
		
		public final int glConst;
		PolyType( int glc) {this.glConst = glc;}
	}
	public void applyPolyProgram( 
			ProgramType type,
			int[] xPoints,
			int[] yPoints, 
			int numPoints,
			PolyType polyType,
			GLParameters params,
			AffineTransform trans,
			GL2 gl) 
	{
		addOrtho(params, trans);
        
        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = (float)xPoints[i];
        	data[i*2+1] = (float)yPoints[i];
        }
		PreparedData pd = prepareRawData(data, new int[]{2}, gl);
		
		applyProgram( type, params, pd, gl, polyType.glConst, numPoints);
		pd.free();
        params.clearInternalParams();
	}
	public void applyPolyProgram(
			ProgramType type, 
			float[] xPoints, 
			float[] yPoints, 
			int numPoints, 
			PolyType polyType,
			GLParameters params, 
			AffineTransform trans, 
			GL2 gl) 
	{
		addOrtho(params, trans);
        
        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = xPoints[i];
        	data[i*2+1] = yPoints[i];
        }
		PreparedData pd = prepareRawData(data, new int[]{2}, gl);
		
		applyProgram( type, params, pd, gl, polyType.glConst, numPoints);
		pd.free();
        params.clearInternalParams();
		
	}
	
	
	/** Combines the world AffineTransform with an Orthogonal Transform as 
	 * defined by the parameters to create a 4D Perspective Matrix*/
	private void addOrtho( GLParameters params, AffineTransform trans) {
        Mat4 matrix = new Mat4(MatrixBuilder.orthagonalProjectionMatrix(
        		0, params.width, (params.flip)?params.height:0, (params.flip)?0:params.height, -1, 1));
        
        if( trans != null) {
	        Mat4 matrix2 = new Mat4( MatrixBuilder.wrapAffineTransformAs4x4(trans));
	        matrix = matrix2.multiply(matrix);
        }
        
        params.addInternalParam( new GLParameters.GLUniformMatrix4fv(
        		"perspectiveMatrix", 1, true, matrix.getBuffer()));
	}
	
	/** Applies the specified Shader Program with the provided parameters, using 
	 * the basic xyuv texture construction.*/
	private void applyProgram( ProgramType type, GLParameters params, PreparedData pd,
			 GL2 gl, int primFormat, int primCount) 
	{
		int w = params.width;
		int h = params.height;
//		setSurfaceSize(w, h);
		int prog = getProgram(type);
		gl.getGL2().glViewport(0, 0, w, h);
		

        
        gl.glUseProgram(prog);
        
        // Bind Attribute Streams
        pd.init();

        // Bind Texture
        if( params.texture != null) {
        	gl.glActiveTexture(GL.GL_TEXTURE0);

            gl.glBindTexture(GL2.GL_TEXTURE_2D, params.texture.load(gl));
    		gl.glEnable(GL2.GL_TEXTURE_2D);
    		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);
        }
        if( params.texture2 != null) {
        	gl.glActiveTexture(GL.GL_TEXTURE1);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, params.texture2.load(gl));
    		gl.glEnable(GL2.GL_TEXTURE_2D);
    		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture2"), 1);
        }

		// Bind Uniform
        if( params != null)
        	params.apply(gl, prog);
        
        // Set Blend Mode
        if( params.useBlendMode) {
	        if( params.useDefaultBlendmode) {
	        	setDefaultBlendMode(gl, type);
	        }
	        else {
		        gl.glEnable(GL.GL_BLEND);
		        gl.glBlendFuncSeparate(
		        		params.bm_sfc, params.bm_dfc, params.bm_sfa, params.bm_dfa);
		        gl.glBlendEquationSeparate(params.bm_fc, params.bm_fa);
	        }
        }

		// Start Draw
		gl.glDrawArrays( primFormat, 0, primCount);
        gl.glDisable( GL.GL_BLEND);
		
		// Finished Drawing
		gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glUseProgram(0);
        pd.deinit();
        if( params.texture != null)
        	params.texture.unload();
        if( params.texture2 != null) 
        	params.texture2.unload();
	}

	
	
	// ==================
	// ==== Texture Preperation
	public class PreparedTexture{
		private IntBuffer tex = GLBuffers.newDirectIntBuffer(1);
		private final GL2 gl;
		final int w, h;
		
		PreparedTexture( GL2 gl, int w, int h) {
			this.gl = gl;
			this.w = w;
			this.h = h;
		}
		
		@Override
		protected void finalize() throws Throwable {
			free();
			super.finalize();
		}
		
		public int getTexID() {return tex.get(0);}
		private boolean _free = false;
		
		public void free() {
			if( !_free) {
				_free = true;
				gl.glDeleteTextures(1, tex);

				c_texes.remove(this);
			}
		}
	}
	public PreparedTexture prepareTexture( BufferedImage bi, GL2 gl) {
		int w = bi.getWidth();
		int h = bi.getHeight();
		PreparedTexture pt = new PreparedTexture(gl, w, h);

		pt.tex = GLBuffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, pt.tex);
        gl.glBindTexture(GL2.GL_TEXTURE_2D, pt.tex.get(0));
		gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri( GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		
		WritableRaster rast = bi.getRaster();
		

		if( rast instanceof ByteInterleavedRaster) {
			gl.glTexImage2D(
					GL2.GL_TEXTURE_2D,
					0,
					GL2.GL_RGBA,
					w, h,
					0,
					GL2.GL_RGBA,
					GL2.GL_UNSIGNED_INT_8_8_8_8,
					ByteBuffer.wrap(((ByteInterleavedRaster)rast).getDataStorage())
					);
		}
		if( rast instanceof IntegerInterleavedRaster) {
			gl.glTexImage2D(
					GL2.GL_TEXTURE_2D,
					0,
					GL2.GL_RGBA,
					w, h,
					0,
					GL2.GL_BGRA,
					GL2.GL_UNSIGNED_INT_8_8_8_8_REV,
					IntBuffer.wrap(((IntegerInterleavedRaster)rast).getDataStorage())
					);
		}
		
/*		__ts[met] = System.currentTimeMillis();
		__dims[met] = w*h;
		
		long tdim = 0;
		for( int i=0; i < TRACK_PREC; ++i) tdim += __dims[i];
		System.out.println( 1000 * tdim / (double)(__ts[met] - __ts[(met+1)%TRACK_PREC]));
		System.out.println( 1000 * 30 / (double)(__ts[met] - __ts[(met+1)%TRACK_PREC]));
		met = (met+1)%TRACK_PREC;*/

		c_texes.add(pt);
		
		return pt;
	}
//	private static final int TRACK_PREC = 30;
//	private long __ts[] = new long[TRACK_PREC];
//	private int __dims[] = new int[TRACK_PREC];
//	private int met = 0;
	
	// =================
	// ==== Data Buffer Preperation
	public class PreparedData{
		private final GL2 gl;
		PreparedData( GL2 gl) {
			this.gl = gl;
		}
		@Override
		protected void finalize() throws Throwable {
			free();
			super.finalize();
		}
		
/*		public int getBuffer() {
			return positionBufferObject.get(0);
		}*/
		
		/** Frees the VBO from GLMemory*/
		public void free() {
			if( !_free) {
				_free = true;
				gl.glDeleteVertexArrays(1, vao);
				gl.glDeleteBuffers(1, positionBufferObject);

				c_data.remove(this);
			}
		}
		
		/** Binds the described buffer to the active rendering. */
		public void init() {
	        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferObject.get(0));
	        
	        int totalLength = 0;
	        for( int i=0; i < lengths.length; ++i) {
		        gl.glEnableVertexAttribArray(i);
		        totalLength += lengths[i];
	        }
	        int offset = 0;
	        for( int i=0; i < lengths.length; ++i) {
		        gl.glVertexAttribPointer(i, lengths[i], GL2.GL_FLOAT, false, 4*totalLength, 4*offset);
		        offset += lengths[i];
	        }

		}
		/** Unbind the Vertex Attribute Arrays*/
		public void deinit() {
	        for( int i=0; i < lengths.length; ++i) 
		        gl.glDisableVertexAttribArray(i);
		}
		
		private int[] lengths;
		private boolean _free = false;
		private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1);
		private IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
	}
	public PreparedData prepareRawData( float raw[], int[] lengths, GL2 gl) {
		PreparedData pd = new PreparedData(gl);
		
		FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(raw);

	    gl.glGenBuffers(1, pd.positionBufferObject);
	    gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, pd.positionBufferObject.get(0));
	    gl.glBufferData(
	    		GL2.GL_ARRAY_BUFFER, 
	    		vertexBuffer.capacity()*Float.BYTES, 
	    		vertexBuffer, 
	    		GL2.GL_STREAM_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

		gl.glGenVertexArrays(1, pd.vao);
		gl.glBindVertexArray(pd.vao.get(0));
		
		pd.lengths = lengths;
		
		this.c_data.add(pd);
		
		return pd;
	}
	
	
	// ==============
	// ==== Initialization
	
	private void initShaders() throws MGLException {
        programs[ProgramType.DEFAULT.ordinal()] =  loadProgramFromResources(
				"shaders/basic.vert", 
				null, 
				"shaders/square_grad.frag");
        programs[ProgramType.SQARE_GRADIENT.ordinal()] =  loadProgramFromResources(
				"shaders/pass.vert", 
				null, 
				"shaders/square_grad.frag");
        programs[ProgramType.STROKE_BASIC.ordinal()] = loadProgramFromResources(
				"shaders/brushes/stroke_basic.vert", 
				"shaders/brushes/stroke_basic.geom", 
				"shaders/brushes/stroke_basic.frag");
        programs[ProgramType.CHANGE_COLOR.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_change_color.frag");
        programs[ProgramType.PASS_BORDER.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_border.frag");
        programs[ProgramType.PASS_INVERT.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_invert.frag");
        programs[ProgramType.PASS_RENDER.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_render.frag");
        programs[ProgramType.PASS_ESCALATE.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_escalate.frag");
        programs[ProgramType.STROKE_SPORE.ordinal()] = loadProgramFromResources( 
				"shaders/brushes/brush_spore.vert", 
				"shaders/brushes/brush_spore.geom", 
				"shaders/brushes/brush_spore.frag");
        programs[ProgramType.PASS_BASIC.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_basic.frag");
        programs[ProgramType.GRID.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/etc/pass_grid.frag");
        programs[ProgramType.STROKE_PIXEL.ordinal()] = loadProgramFromResources( 
				"shaders/brushes/stroke_pixel.vert", 
				null, 
				"shaders/brushes/stroke_pixel.frag");
        programs[ProgramType.POLY_RENDER.ordinal()] = loadProgramFromResources( 
				"shaders/shapes/poly_render.vert", 
				null, 
				"shaders/shapes/shape_render.frag");
        programs[ProgramType.LINE_RENDER.ordinal()] = loadProgramFromResources( 
				"shaders/shapes/line_render.vert", 
				"shaders/shapes/line_render.geom", 
				"shaders/shapes/shape_render.frag");
	}
	
	private int loadProgramFromResources( String vert, String geom, String frag) 
			throws MGLException
	{
		GL2 gl = drawable.getGL().getGL2();

        ArrayList<Integer> shaderList = new ArrayList<>();
        
        if( vert != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GL2.GL_VERTEX_SHADER, vert));
        }
        if( geom != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GL3.GL_GEOMETRY_SHADER, geom));
        }
        if( frag != null ){
            shaderList.add( compileShaderFromResource(
    				gl, GL2.GL_FRAGMENT_SHADER, frag));
        }
        
		int ret = createProgram( gl, shaderList);
		

        for( Integer shader : shaderList) {
        	gl.glDeleteShader(shader);
        }
        
        return ret;
	}
	
	private int createProgram( GL2 gl, ArrayList<Integer> shaderList) 
			throws MGLException 
	{
		// Create and Link Program
		int program = gl.glCreateProgram();

		for( Integer shader : shaderList){
			gl.glAttachShader(program, shader);	
		}
		gl.glLinkProgram(program);
		
		// Check for Errors
		IntBuffer status = GLBuffers.newDirectIntBuffer(1);
        gl.glGetProgramiv(program, GL2.GL_LINK_STATUS, status);
        if (status.get(0) == GL2.GL_FALSE) {

            IntBuffer infoLogLength = GLBuffers.newDirectIntBuffer(1);
            gl.glGetProgramiv(program, GL2.GL_INFO_LOG_LENGTH, infoLogLength);

            ByteBuffer bufferInfoLog = GLBuffers.newDirectByteBuffer(infoLogLength.get(0));
            gl.glGetProgramInfoLog(program, infoLogLength.get(0), null, bufferInfoLog);
            byte[] bytes = new byte[infoLogLength.get(0)];
            bufferInfoLog.get(bytes);
            String strInfoLog = new String(bytes);

			infoLogLength.clear();
			bufferInfoLog.clear();
			throw new MGLException("Link Program." + strInfoLog);

        }


		for( Integer shader : shaderList){
			gl.glDetachShader(program, shader);
		}
        status.clear();

        return program;
	}

	private int compileShaderFromResource( GL2 gl, int shaderType, String resource) 
			throws MGLException 
	{
		// Read Shader text from resource file
		String shaderText = null;
		try {
			InputStream is = Globals.class.getClassLoader().getResource(resource).openStream();
			
			Scanner scanner = new Scanner( is);
			scanner.useDelimiter("\\A");
			shaderText = scanner.next();
			scanner.close();
		} catch (IOException e) {
			MDebug.handleError(ErrorType.RESOURCE, e, "Couldn't Load Shader");
			return -1;
		}
		
		int shader = gl.glCreateShader(shaderType);
		
		// Compile data into buffer form, then feed it to the GPU compiler
		String[] lines = {shaderText};
		IntBuffer lengths = GLBuffers.newDirectIntBuffer( new int[]{lines[0].length()});
		gl.glShaderSource( shader, 1, lines, lengths);
		gl.glCompileShader(shader);
		
		// Read Status
		IntBuffer status = GLBuffers.newDirectIntBuffer(1);
		gl.glGetShaderiv( shader, GL2.GL_COMPILE_STATUS, status);
		if( status.get(0) == GL2.GL_FALSE) {
			
			// Read Compile Errors
			IntBuffer infoLogLength = GLBuffers.newDirectIntBuffer(1);
			gl.glGetShaderiv( shader,  GL2.GL_INFO_LOG_LENGTH, infoLogLength);
			
			ByteBuffer bufferInfoLog = GLBuffers.newDirectByteBuffer(infoLogLength.get(0));
			gl.glGetShaderInfoLog( shader, infoLogLength.get(0), null, bufferInfoLog);
			byte[] bytes = new byte[infoLogLength.get(0)];
			bufferInfoLog.get(bytes);
			String strInfoLog = new String(bytes);
			
			String type = "";
			switch( shaderType) {
			case GL2.GL_VERTEX_SHADER:
				type = "vertex";
				break;
			case GL3.GL_GEOMETRY_SHADER:
				type = "geometry";
				break;
			case GL2.GL_FRAGMENT_SHADER:
				type = "fragment";
				break;
			default:
				type = "unknown type: " + shaderType;
				break;
			}
			infoLogLength.clear();
			bufferInfoLog.clear();
			throw new MGLException("Failed to compile GLSL shader (" + type + "): " + strInfoLog);
			
		}
		lengths.clear();
		status.clear();
		
		return shader;
	}
	
	// =========
	// ==== Resource Tracking
	final List<GLMultiRenderer> c_glmus = new ArrayList<>();
	final List<PreparedTexture> c_texes = new ArrayList<>();
	final List<PreparedData> c_data = new ArrayList<>();
	
	public String dispResourcesUsed() {
		String str = "";
		
//		IntBuffer ib = GLBuffers.newDirectIntBuffer(1);
//		getGL2().glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, ib);
//		str += ib.get(0) + "\n";
		
		str += "FBOs: \n";
		for( int i=0; i < c_glmus.size(); ++i) {
			str += i + "["+c_glmus.get(i).getTexture() +"] : (" + c_glmus.get(i).width + "," + c_glmus.get(i).height + ")\n";
		}
		str += "Textures: \n";
		for( int i=0; i < c_texes.size(); ++i) {
			str += i + "[" + c_texes.get(i).getTexID()+ "] : (" + c_texes.get(i).w + "," + c_texes.get(i).h + ")\n";
		}
		str += "VBOs: \n";
		for( int i=0; i < c_data.size(); ++i) {
			str += i + "[" + c_data.get(i).positionBufferObject.get(0)+","+c_data.get(i).vao.get(i)+"] \n";
		}
		
		return str;
	}

}
