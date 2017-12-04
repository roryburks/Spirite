package spirite.base.graphics.gl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import com.hackoeur.jglm.Mat4;
import com.jogamp.opengl.GL2;

import spirite.base.graphics.CapMethod;
import spirite.base.graphics.JoinMethod;
import spirite.base.graphics.gl.GLGeom.Primitive;
import spirite.base.graphics.gl.wrap.GLCore.MGLException;
import spirite.base.util.glu.GLC;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.MatrixBuilder;
import spirite.base.util.linear.Rect;
import spirite.hybrid.Globals;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.jogl.JOGLCore;

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
	private static GLEngine singly = null;
	public static GLEngine getInstance() {
		if( singly == null) singly = new GLEngine();
		return singly;
	}
	
	private GLEngine() {}
	
	// ============
	// ==== Simple API
	public GL2 getGL2() {
		return JOGLCore.getGL2();
	}
	public void setGL( GL2 gl) {
		JOGLCore.setGL(gl);
	}



    static final FloatBuffer clearColor = FloatBuffer.wrap( new float[] {0f, 0f, 0f, 0f});
	void clearSurface(GL2 gl2) {
        gl2.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
	}
	
	// ===========
	// ==== FrameBufferObject Management
	private int dbo = -1;
	private int currentTarget = 0;
	private int fbo = 0;
	private int width = 1;
	private int height = 1;
	public int getTarget() {return currentTarget;}
	public int getWidth() { return width;}
	public int getHeight() {return height;}
	public void setTarget( int tex) {
		GL2 gl = getGL2();
		if( currentTarget != tex) {
			// Delete old Framebuffer
			if( currentTarget != 0) {
				gl.glDeleteFramebuffers(1, new int[]{fbo}, 0); 
			}
			
			if( tex == 0) {
		        gl.glBindFramebuffer( GLC.GL_FRAMEBUFFER, 0);
				currentTarget = tex;
				width = 1;
				height = 1;
			}
			else {
				int[] result = new int[1];
				gl.glGenFramebuffers(1, result, 0);
				fbo = result[0];
				gl.glBindFramebuffer(GLC.GL_FRAMEBUFFER, fbo);
				
				currentTarget = tex;
				bindEmptyDB();
	
		        // Attach Texture to FBO
		        gl.glFramebufferTexture2D( GLC.GL_FRAMEBUFFER, GLC.GL_COLOR_ATTACHMENT0, 
		        		GLC.GL_TEXTURE_2D, tex, 0);
		        
		        checkFramebuffer();
			}
		}
	}
	public void setTarget( GLImage img) {
		if( img == null) {
			setTarget(0);
		}
		else {
			GL2 gl = getGL2();
			setTarget(img.getTexID());
			gl.glViewport(0, 0, img.getWidth(), img.getHeight());
			width = img.getWidth();
			height = img.getHeight();
		}
	}
	private void bindEmptyDB() {
		GL2 gl = getGL2();
        gl.glBindRenderbuffer(GLC.GL_RENDERBUFFER, dbo);
        gl.glRenderbufferStorage( GLC.GL_RENDERBUFFER, GLC.GL_DEPTH_COMPONENT16, 1, 1);
        gl.glFramebufferRenderbuffer( GLC.GL_FRAMEBUFFER, GLC.GL_DEPTH_ATTACHMENT, GLC.GL_RENDERBUFFER, dbo);
	}
	private void checkFramebuffer() {
		// Checks is the FrameBuffer was successfully created, displaying a proper
		// Error message if it wasn't.
		GL2 gl = getGL2();
		
		int result = gl.glCheckFramebufferStatus(GLC.GL_FRAMEBUFFER);
        switch( result) {
        case GLC.GL_FRAMEBUFFER_COMPLETE:
    		return;
        case GLC.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT.");
    		break;	
        case GLC.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
    		break;	
        case GLC.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
    		break;	
//        case GLC.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
//        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_FORMATS");
//    		break;	
//        case GLC.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
//        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
//    		break;	
//        case GLC.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
//        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
//    		break;	
        case GLC.GL_FRAMEBUFFER_UNSUPPORTED:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_UNSUPPORTED");
    		break;
//        case GLC.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE :
//        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE");
//    		break;	
//        case GLC.GL_FRAMEBUFFER_UNDEFINED :
//        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction.GL_FRAMEBUFFER_UNDEFINED");
//    		break;	
        case GLC.GL_INVALID_ENUM:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_INVALID_ENUM");
    		break;
        default:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. " + result);
    		break;
        }
	}
	
	// =================
	// ==== Program Management
	public enum ProgramType {
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
		STROKE_V2_LINE_PASS,
		STROKE_AFTERPASS_INTENSIFY,
		
		POLY_RENDER,
		LINE_RENDER,
		;
	}
	private void setDefaultBlendMode(ProgramType type) {
		GL2 gl = getGL2();
        switch( type) {
		case STROKE_BASIC:
		case STROKE_PIXEL:
		case POLY_RENDER:
		case LINE_RENDER:
		case PASS_RENDER:
		case PASS_BASIC:
		case GRID:
		case STROKE_V2_LINE_PASS:
		case PASS_ESCALATE:
	        gl.glEnable(GLC.GL_BLEND);
	        gl.glBlendFunc(GLC.GL_ONE, GLC.GL_ONE_MINUS_SRC_ALPHA);
	        gl.glBlendEquation(GLC.GL_FUNC_ADD);
	        break;
		case STROKE_AFTERPASS_INTENSIFY:
	        gl.glEnable(GLC.GL_BLEND);
	        gl.glBlendFunc(GLC.GL_ONE, GLC.GL_ZERO);
	        gl.glBlendEquation(GLC.GL_FUNC_ADD);
	        break;
		case CHANGE_COLOR:
		case PASS_INVERT:
		case SQARE_GRADIENT:
		case STROKE_SPORE:
	        gl.glEnable(GLC.GL_BLEND);
	        gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE);
	        gl.glBlendEquation(GL2.GL_MAX);
	        break;
		case PASS_BORDER:
	        gl.glEnable(GLC.GL_BLEND);
	        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	        gl.glBlendEquation(GL2.GL_FUNC_ADD);
        
        }
	}
	
	private int programs[] = new int[ProgramType.values().length];
	
	int getProgram( ProgramType type){
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
	 */
	void applyPassProgram(
			ProgramType type,
			GLParameters params, 
			MatTrans trans)
	{
		applyPassProgram( type, params, trans, 0, 0, params.width, params.height);
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
	 */
	void applyPassProgram(
			ProgramType type, 
			GLParameters params, 
			MatTrans trans,
			float x1, float y1, float x2, float y2)
	{
		addOrtho(params, trans);
		
		boolean internal = (params.texture == null) ? false : !params.texture.isGLOriented();


		PreparedPrimitive prim = new PreparedPrimitive( new Primitive(
			new float[]{
				// x  y   u   v
				x1, y1, 0.0f, (internal)?0.0f:1.0f,
				x2, y1, 1.0f, (internal)?0.0f:1.0f,
				x1, y2, 0.0f, (internal)?1.0f:0.0f,
				x2, y2, 1.0f, (internal)?1.0f:0.0f,
			}, new int[]{2,2}, GL2.GL_TRIANGLE_STRIP, new int[]{4}));
		prim.prepare();
        applyProgram( type, params, prim);
        prim.free();
        params.clearInternalParams();
	}

	void applyLineProgram( ProgramType type, int[] xPoints, int[] yPoints, 
			int numPoints, GLParameters params, MatTrans trans) 
	{
		addOrtho(params, trans);

        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = xPoints[i];
        	data[i*2+1] = yPoints[i];
        }

		PreparedPrimitive prim = new PreparedPrimitive( new Primitive(
				data, new int[]{2}, GL2.GL_TRIANGLE_STRIP, new int[]{numPoints}));
		prim.prepare();
		applyProgram( type, params, prim);
		prim.free();
        params.clearInternalParams();
	}
	
	void applyLineProgram( ProgramType type, float[] xPoints, float[] yPoints, 
			int numPoints, GLParameters params, MatTrans trans, GL2 gl) 
	{
		addOrtho(params, trans);
        
        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = xPoints[i];
        	data[i*2+1] = yPoints[i];
        }
		PreparedPrimitive prim = new PreparedPrimitive( new Primitive(
				data, new int[]{2}, GL2.GL_TRIANGLE_STRIP, new int[]{numPoints}));
		prim.prepare();
		applyProgram( type, params, prim);
		prim.free();
        params.clearInternalParams();
	}
	void applyPrimitiveProgram( ProgramType type, GLParameters params, Primitive prim) {
		applyPrimitiveProgram( type, params, prim, new MatTrans());
	}
	void applyPrimitiveProgram( ProgramType type, GLParameters params, Primitive prim, MatTrans trans) {
		addOrtho(params, trans);
//        params.addInternalParam( new GLParameters.GLUniformMatrix4fv(
//        		"perspectiveMatrix", 1, matrix.getBuffer()));
        
		PreparedPrimitive pprim = new PreparedPrimitive( prim);
		pprim.prepare();
		applyProgram( type, params, pprim);
		pprim.free();
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
	 */
	void applyComplexLineProgram(int[] xPoints, int[] yPoints,
								 int numPoints, CapMethod cap, JoinMethod join, boolean loop, float width,
								 GLParameters params, MatTrans trans)
	{
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
		
		_doCompliexLineProg( data, size, cap, join, width, params, trans);
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
	 */
	void applyComplexLineProgram( float[] xPoints, float[] yPoints, 
			int numPoints, CapMethod cap, JoinMethod join, boolean loop, float width,
			GLParameters params, MatTrans trans) 
	{
		// NOTE: identical code to above but without implicit casting
		if( xPoints.length < 2) return;

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
		
		_doCompliexLineProg( data, size, cap, join, width, params, trans);
	}
	
	private void _doCompliexLineProg( float[] data, int count, CapMethod cap, 
			JoinMethod join, float width, GLParameters params, MatTrans trans)
	{
		GL2 gl = getGL2();
		addOrtho(params, trans);
		
		// TODO: implement Rounded joins and all cap methods
		int uJoin = 0;
		switch( join) {
			case BEVEL: uJoin = 2;break;
			case MITER: uJoin = 1; break;
			case ROUNDED:break;
		}
		uJoin = 1;

		if( HybridHelper.getGLCore().getShaderVersion() >= 330) {
			PreparedPrimitive prim = new PreparedPrimitive( new Primitive(
					data, new int[]{2}, GLC.GL_LINE_STRIP_ADJACENCY, new int[]{count}));
			prim.prepare();
			
			gl.glEnable(GLC.GL_MULTISAMPLE );
			params.addInternalParam(new GLParameters.GLParam1i("uJoin",uJoin));
			params.addInternalParam(new GLParameters.GLParam1f("uWidth", width/ 2.0f));
			applyProgram( ProgramType.LINE_RENDER, params, prim);
			gl.glDisable(GLC.GL_MULTISAMPLE );
			
			prim.free();
		}
		else {
			PreparedPrimitive prim = new PreparedPrimitive(GLGeom.lineRenderGeom(data, uJoin, width/2.0f));
			prim.prepare();
			applyProgram( ProgramType.POLY_RENDER, params, prim);
			prim.free();
		}
        params.clearInternalParams();
	}

	public enum PolyType {
		STRIP(GLC.GL_TRIANGLE_STRIP), 
		FAN(GLC.GL_TRIANGLE_FAN), 
		LIST(GLC.GL_TRIANGLES);
		
		public final int glConst;
		PolyType( int glc) {this.glConst = glc;}
	}
	void applyPolyProgram( 
			ProgramType type,
			int[] xPoints,
			int[] yPoints, 
			int numPoints,
			PolyType polyType,
			GLParameters params,
			MatTrans trans,
			GL2 gl) 
	{
		addOrtho(params, trans);
        
        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = (float)xPoints[i];
        	data[i*2+1] = (float)yPoints[i];
        }

		PreparedPrimitive prim = new PreparedPrimitive( new Primitive(
				data, new int[]{2}, polyType.glConst, new int[]{numPoints}));
		prim.prepare();
		applyProgram( type, params, prim);
		prim.free();
        params.clearInternalParams();
	}
	void applyPolyProgram(
			ProgramType type, 
			float[] xPoints, 
			float[] yPoints, 
			int numPoints, 
			PolyType polyType,
			GLParameters params, 
			MatTrans trans, 
			GL2 gl) 
	{
		addOrtho(params, trans);
        
        float data[] = new float[2*numPoints];
        for( int i=0; i < numPoints; ++i) {
        	data[i*2] = xPoints[i];
        	data[i*2+1] = yPoints[i];
        }
		PreparedPrimitive prim = new PreparedPrimitive( new Primitive(
				data, new int[]{2}, polyType.glConst, new int[]{numPoints}));
		prim.prepare();
		applyProgram( type, params, prim);
		prim.free();
        params.clearInternalParams();
		
	}
	
	
	/** Combines the world MatTrans with an Orthogonal Transform as 
	 * defined by the parameters to create a 4D Perspective Matrix*/
	private void addOrtho( GLParameters params, MatTrans trans) {
		int x1, y1, x2, y2;
		if( params.clipRect == null) {
			x1 = 0; x2 = params.width;
			y1 = 0; y2 = params.height;
		}
		else {
			x1 = params.clipRect.x;
			x2 = params.clipRect.x+params.clipRect.width;
			y1 = params.clipRect.y;
			y2 = params.clipRect.y+params.clipRect.height;
		}
		
        Mat4 matrix = new Mat4(MatrixBuilder.orthagonalProjectionMatrix(
        		x1, x2, (params.flip)?y2:y1, (params.flip)?y1:y2, -1, 1));
        
        if( trans != null) {
	        Mat4 matrix2 = new Mat4( MatrixBuilder.wrapTransformAs4x4(trans));
	        matrix = matrix2.multiply(matrix);
        }
        
        matrix = matrix.transpose();
        params.addInternalParam( new GLParameters.GLUniformMatrix4fv(
        		"perspectiveMatrix", 1, matrix.getBuffer()));
	}
	
	/** Applies the specified Shader Program with the provided parameters, using 
	 * the basic xyuv texture construction.*/
	private void applyProgram( ProgramType type, GLParameters params, PreparedPrimitive pprim) 
	{
		GL2 gl = getGL2();
		
		int w = params.width;
		int h = params.height;
        GLGeom.Primitive primitive = pprim.base;
		
		int prog = getProgram(type);
		
		Rect r = params.clipRect;
		if( r == null)
			gl.getGL2().glViewport(0, 0, w, h);
		else
			gl.getGL2().glViewport(r.x, r.y, r.width, r.height);
		

        
        gl.glUseProgram(prog);
        
        // Bind Attribute Streams
        PreparedData pd = pprim.pd;
        pd.init();

        // Bind Texture
        if( params.texture != null) {
        	gl.glActiveTexture(GLC.GL_TEXTURE0);

            gl.glBindTexture(GLC.GL_TEXTURE_2D, params.texture.tex);
    		gl.glEnable(GLC.GL_TEXTURE_2D);
    		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);
        }
        if( params.texture2 != null) {
        	gl.glActiveTexture(GLC.GL_TEXTURE1);
            gl.glBindTexture(GLC.GL_TEXTURE_2D, params.texture2.tex);
    		gl.glEnable(GLC.GL_TEXTURE_2D);
    		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture2"), 1);
        }
        

		// Bind Uniform
        if( params != null)
        	params.apply(gl, prog);
        
        // Set Blend Mode
        if( params.useBlendMode) {
	        if( params.useDefaultBlendmode) {
	        	setDefaultBlendMode(type);
	        }
	        else {
		        gl.glEnable(GLC.GL_BLEND);
		        gl.glBlendFuncSeparate(
		        		params.bm_sfc, params.bm_dfc, params.bm_sfa, params.bm_dfa);
		        gl.glBlendEquationSeparate(params.bm_fc, params.bm_fa);
	        }
        }
        //gl.glEnable(GLC.GL_MULTISAMPLE);
        if( type == ProgramType.STROKE_V2_LINE_PASS) {
	        gl.glEnable(GLC.GL_LINE_SMOOTH);
	        gl.glEnable(GL2.GL_BLEND);
	        gl.glEnable(GL2.GL_LINE_WIDTH);
	        gl.glDepthMask(false);
	        gl.glLineWidth(1);
        }

		// Start Draw
        int start = 0;
        for( int i=0; i < primitive.primitiveLengths.length; ++i) {
        	gl.glDrawArrays(primitive.primitiveTypes[i],  start, primitive.primitiveLengths[i]);
            start += primitive.primitiveLengths[i];
        }
        gl.glDisable( GLC.GL_BLEND);
        gl.glDisable(GLC.GL_LINE_SMOOTH);
        gl.glDisable(GL2.GL_BLEND);
        gl.glDisable(GL2.GL_LINE_WIDTH);
        gl.glDepthMask(true);
		
		// Finished Drawing
		gl.glDisable(GLC.GL_TEXTURE_2D);
        gl.glUseProgram(0);
        pd.deinit();
//        if( params.texture != null)
//        	params.texture.unload();
//        if( params.texture2 != null) 
//        	params.texture2.unload();
	}

	
	
	// ==================
	// ==== Texture Preperation
/*	public class PreparedTexture{
		private IntBuffer tex = IntBuffer.allocate(1);
		final int w, h;
		
		PreparedTexture( int w, int h) {
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
				getGL2().glDeleteTextures(1, tex);

				c_texes.remove(this);
			}
		}
	}*/
	// =================
	// ==== Data Buffer Preperation
	public class PreparedData{
		PreparedData() {}
		@Override
		protected void finalize() throws Throwable {
			if(!_free)
				MDebug.handleWarning(WarningType.STRUCTURAL, "PreparedData (VBO wrapper) not freed before being finalized.");
			free();
			super.finalize();
		}
		
		/** Frees the VBO from GLMemory*/
		public void free() {
			if( !_free) {
				GL2 gl = getGL2();
				
				_free = true;
				gl.glDeleteVertexArrays(1, vao);
				gl.glDeleteBuffers(1, positionBufferObject);

				//c_data.remove(this);
			}
		}
		
		/** Binds the described buffer to the active rendering. */
		public void init() {
			GL2 gl = getGL2();
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
	        	getGL2().glDisableVertexAttribArray(i);
		}
		
		private int[] lengths;
		private boolean _free = false;
		private IntBuffer positionBufferObject = IntBuffer.allocate(1);
		private IntBuffer vao = IntBuffer.allocate(1);
	}
	PreparedData prepareRawData( float raw[], int[] lengths) {
		GL2 gl = getGL2();
		PreparedData pd = new PreparedData();
		
		FloatBuffer vertexBuffer = FloatBuffer.wrap(raw);

		gl.glGenBuffers(1, pd.positionBufferObject);
		gl.glBindBuffer( GLC.GL_ARRAY_BUFFER, pd.positionBufferObject.get(0));
		gl.glBufferData(
	    		GLC.GL_ARRAY_BUFFER, 
	    		vertexBuffer.capacity()*Float.BYTES, 
	    		vertexBuffer, 
	    		GLC.GL_STREAM_DRAW);
		gl.glBindBuffer(GLC.GL_ARRAY_BUFFER, 0);

		gl.glGenVertexArrays(1, pd.vao);
		gl.glBindVertexArray(pd.vao.get(0));
		
		pd.lengths = lengths;
		
		//this.c_data.add(pd);
		
		return pd;
	}

    class PreparedPrimitive{
        private final GLGeom.Primitive base;
        private GLEngine.PreparedData pd = null;
        private boolean prepared = false;

        public PreparedPrimitive( GLGeom.Primitive primitive) {
            this.base = primitive;
        }

        public void prepare() {
            if( prepared)
                free();

            pd = prepareRawData( base.raw, base.attrLengths);
            prepared = true;
        }

        public void free() {
            pd.free();
            pd = null;
        }
    }
	
	// ==============
	// ==== Initialization
	public void init(GL2 gl) throws MGLException {
		// !!! NOTE: It's important that you don't use GLEngine.getGL2() in 
		//	initialization since it might cause it to try and switch the offscreen
		//	drawable to the current context during the initialization of that drawable
		//	creating a locking conflict
		
        // Create a (nearly) empty depth-buffer as a placeholder
		int[] result = new int[1];
        gl.glGenRenderbuffers( 1, result, 0);
        dbo = result[0];
        
		initShaders(gl);
	}
	
	private void initShaders(GL2 gl) throws MGLException {
		try {
			initShaders330(gl);
			HybridHelper.getGLCore().setShaderVersion(330);
			return;
		}catch (MGLException e) {
			MDebug.log("Failed to load #version 330: " + e.getMessage() + "\nAttempting to load #version 100 Shaders.");
		}
		initShaders100(gl);
		HybridHelper.getGLCore().setShaderVersion(100);
	}

	private void initShaders330( GL2 gl) throws MGLException {
        programs[ProgramType.SQARE_GRADIENT.ordinal()] =  loadProgramFromResources( gl,
				"shaders/pass.vert", 
				null, 
				"shaders/square_grad.frag");
        programs[ProgramType.STROKE_BASIC.ordinal()] = loadProgramFromResources( gl,
				"shaders/brushes/stroke_basic.vert", 
				"shaders/brushes/stroke_basic.geom", 
				"shaders/brushes/stroke_basic.frag");
        programs[ProgramType.CHANGE_COLOR.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/pass_change_color.frag");
        programs[ProgramType.PASS_BORDER.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/pass_border.frag");
        programs[ProgramType.PASS_INVERT.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/pass_invert.frag");
        programs[ProgramType.PASS_RENDER.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/pass_render.frag");
        programs[ProgramType.PASS_ESCALATE.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/pass_escalate.frag");
        programs[ProgramType.STROKE_SPORE.ordinal()] = loadProgramFromResources(  gl,
				"shaders/brushes/brush_spore.vert", 
				"shaders/brushes/brush_spore.geom", 
				"shaders/brushes/brush_spore.frag");
        programs[ProgramType.PASS_BASIC.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/pass_basic.frag");
        programs[ProgramType.GRID.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/etc/pass_grid.frag");
        programs[ProgramType.STROKE_V2_LINE_PASS.ordinal()] =
        programs[ProgramType.STROKE_PIXEL.ordinal()] = loadProgramFromResources(  gl,
				"shaders/brushes/stroke_pixel.vert", 
				null, 
				"shaders/brushes/stroke_pixel.frag");
        programs[ProgramType.POLY_RENDER.ordinal()] = loadProgramFromResources(  gl,
				"shaders/shapes/poly_render.vert", 
				null, 
				"shaders/shapes/shape_render.frag");
        programs[ProgramType.LINE_RENDER.ordinal()] = loadProgramFromResources(  gl,
				"shaders/shapes/line_render.vert", 
				"shaders/shapes/line_render.geom", 
				"shaders/shapes/shape_render.frag");
        programs[ProgramType.STROKE_AFTERPASS_INTENSIFY.ordinal()] = loadProgramFromResources(  gl,
				"shaders/pass.vert", 
				null, 
				"shaders/brushes/brush_intensify.frag");
		
	}
	private void initShaders100( GL2 gl) throws MGLException {
        programs[ProgramType.SQARE_GRADIENT.ordinal()] =  loadProgramFromResources( gl,
				"shaders/100/pass.vert", null, "shaders/100/etc/square_grad.frag");
        programs[ProgramType.CHANGE_COLOR.ordinal()] = loadProgramFromResources(  gl,
				"shaders/100/pass.vert", null, "shaders/100/pass_change_color.frag");
        programs[ProgramType.GRID.ordinal()] = loadProgramFromResources(  gl,
				"shaders/100/pass.vert", null, "shaders/100/etc/pass_grid.frag");

        programs[ProgramType.PASS_INVERT.ordinal()] = loadProgramFromResources(  gl,
				"shaders/100/pass.vert", null, "shaders/100/pass_invert.frag");
        programs[ProgramType.PASS_BORDER.ordinal()] = loadProgramFromResources(  gl,
				"shaders/100/pass.vert", null, "shaders/100/pass_border.frag");
        programs[ProgramType.PASS_RENDER.ordinal()] = loadProgramFromResources(  gl,
				"shaders/100/pass.vert", null, "shaders/100/pass_render.frag");
        programs[ProgramType.PASS_ESCALATE.ordinal()] = 0;
        programs[ProgramType.PASS_BASIC.ordinal()] = loadProgramFromResources(  gl,
				"shaders/100/pass.vert", null, "shaders/100/pass_basic.frag");
        
        programs[ProgramType.STROKE_BASIC.ordinal()] = loadProgramFromResources( gl,
				"shaders/100/brushes/stroke_basic.vert", null, "shaders/100/brushes/stroke_basic.frag");
        programs[ProgramType.STROKE_SPORE.ordinal()] = 0;
        programs[ProgramType.STROKE_PIXEL.ordinal()] = 0;
        
        
        programs[ProgramType.LINE_RENDER.ordinal()] = 0;
        programs[ProgramType.POLY_RENDER.ordinal()] = loadProgramFromResources(  gl,
				"shaders/100/shapes/poly_render.vert", null, "shaders/100/shapes/shape_render.frag");
		
	}
	
	private int loadProgramFromResources( GL2 gl, String vert, String geom, String frag) 
			throws MGLException
	{
        ArrayList<Integer> shaderList = new ArrayList<>();
        
        if( vert != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GLC.GL_VERTEX_SHADER, vert));
        }
        if( geom != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GLC.GL_GEOMETRY_SHADER, geom));
        }
        if( frag != null ){
            shaderList.add( compileShaderFromResource(
    				gl, GLC.GL_FRAGMENT_SHADER, frag));
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
		IntBuffer status = IntBuffer.allocate(1);
        gl.glGetProgramiv(program, GL2.GL_LINK_STATUS, status);
        if (status.get(0) == GL2.GL_FALSE) {

            IntBuffer infoLogLength = IntBuffer.allocate(1);
            gl.glGetProgramiv(program, GL2.GL_INFO_LOG_LENGTH, infoLogLength);

            ByteBuffer bufferInfoLog = ByteBuffer.allocate(infoLogLength.get(0));
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
			MDebug.handleError(ErrorType.RESOURCE, e, "Couldn't Load Shader [" + resource + "]");
			return -1;
		}
		
		int shader = gl.glCreateShader(shaderType);
		
		// Compile data into buffer form, then feed it to the GPU compiler
		String[] lines = {shaderText};
		IntBuffer lengths = IntBuffer.wrap( new int[]{lines[0].length()});
		gl.glShaderSource( shader, 1, lines, lengths);
		gl.glCompileShader(shader);
		
		// Read Status
		IntBuffer status = IntBuffer.allocate(1);
		gl.glGetShaderiv( shader, GL2.GL_COMPILE_STATUS, status);
		if( status.get(0) == GL2.GL_FALSE) {
			
			// Read Compile Errors
			IntBuffer infoLogLength = IntBuffer.allocate(1);
			gl.glGetShaderiv( shader,  GL2.GL_INFO_LOG_LENGTH, infoLogLength);
			
			ByteBuffer bufferInfoLog = ByteBuffer.allocate( infoLogLength.get(0));
			gl.glGetShaderInfoLog( shader, infoLogLength.get(0), null, bufferInfoLog);
			byte[] bytes = new byte[infoLogLength.get(0)];
			bufferInfoLog.get(bytes);
			String strInfoLog = new String(bytes);
			
			String type = "";
			switch( shaderType) {
			case GLC.GL_VERTEX_SHADER:
				type = "vertex";
				break;
			case GLC.GL_GEOMETRY_SHADER:
				type = "geometry";
				break;
			case GLC.GL_FRAGMENT_SHADER:
				type = "fragment";
				break;
			default:
				type = "unknown type: " + shaderType;
				break;
			}
			infoLogLength.clear();
			bufferInfoLog.clear();
			throw new MGLException("Failed to compile GLSL shader [" + resource + "] (" + type + "): " + strInfoLog);
			
		}
		lengths.clear();
		status.clear();
		
		return shader;
	}
	
	// =========
	// ==== Resource Tracking
	private final List<WeakReference<GLImage>> c_img = new ArrayList<>();
	
	void glImageLoaded( GLImage img) {
		c_img.add(new WeakReference<GLImage>(img));
		
		Iterator<WeakReference<GLImage>> it = c_img.iterator();
		while( it.hasNext()) {
			if( it.next().get() == null) it.remove();
		}
	}
	
	void glImageUnloaded( GLImage img) {
		Iterator<WeakReference<GLImage>> it = c_img.iterator();
		while( it.hasNext()) {
			GLImage _img = it.next().get();
			if( _img == null || _img == img) it.remove();
		}
	}
	
	public String dispResourcesUsed() {
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#.##");
		
		long size = 0;
		for( WeakReference<GLImage> img : c_img)  {
			if( img.get() != null)
				size += img.get().getByteSize();
		}
		sb.append("Total Texture Size: " + df.format(size / 1048576.0f) + "MB\n");

		sb.append("Textures (" + c_img.size() + ": \n");
		for( WeakReference<GLImage> img : c_img) {
			if( img.get() != null)
				sb.append( "["+img.get().getTexID() +"] : (" + img.get().getWidth() + "," + img.get().getHeight() + ")\n");
		}

		return sb.toString();
	}
	
	public long getUsedResources() {
		long used = 0;
		for( WeakReference<GLImage> img : c_img)  {
			if( img.get() != null)
				used += img.get().getByteSize();
		}
		return used;
	}

}
