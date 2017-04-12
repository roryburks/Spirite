package spirite.graphics.gl;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import com.hackoeur.jglm.Mat4;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import mutil.MatrixBuilder;
import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.graphics.gl.GLMultiRenderer.GLRenderer;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

/**
 * GLEngine is the root point for dealing with OpenGL through JOGL.  It handles
 * the initialization of surfaces and components and manages the resources, 
 * making sure that they are properly de-allocated inside OpenGL
 * 
 * Uses a Singleton paradigm that can be accessed through GLEngine.getInstance()
 * 
 * TODO: Though there is no plan to add software rendering options for every 
 * feature (JOGL support is reasonably comprehensive), I do want to make some
 * effort to support machines running earlier GL versions (GL2).
 * 
 * Geometry Shaders and LINE_ADJACENCY_STRIPS for the stroke engine could easily
 * be done in software.  Replacing Framebuffers with some other method would not
 * be difficult but might be extremely slow.
 * 
 * NOTE: Constructor is private and access to the singly instance is package-scoped
 * so only the package has access to GLEngine.  Care should be taken to make sure
 * GLFunctionality does not leak beyond this package so that functions that require
 * OpenGL are kept explicit and modular (so that you don't try to access OpenGL
 * when it's not available)
 * 
 * @author Rory Burks
 */
public class GLEngine  {
	private final GLOffscreenAutoDrawable drawable;
	int width = 1;
	int height = 1;

	
	/** Namespace for Attribute Bindings 
	 * CURRENTLY USED VERY RARELY * */
    public static class Attr {
        public static final int POSITION = 0;
        public static final int NORMAL = 2;
        public static final int COLOR = 1;
        public static final int TEXCOORD = 4;
        public static final int DRAW_ID = 5;
        public static final int CAMERA_SPHERE_POS = 6;
        public static final int SPHERE_RADIUS = 7;
    }

	private static final GLEngine singly = new GLEngine();
	static GLEngine getInstance() {
		return singly;
	}
	
	private GLEngine() {
		// Create Offscreen OpenGl Surface
		GLProfile profile = GLProfile.getDefault();
        GLDrawableFactory fact = GLDrawableFactory.getFactory(profile);
		GLCapabilities caps = new GLCapabilities(profile);
        caps.setHardwareAccelerated(true); 
        caps.setDoubleBuffered(false); 
        caps.setAlphaBits(8); 
        caps.setRedBits(8); 
        caps.setBlueBits(8); 
        caps.setGreenBits(8); 
        caps.setOnscreen(false); 


		drawable = fact.createOffscreenAutoDrawable(
				fact.getDefaultDevice(), 
				caps,
				new DefaultGLCapabilitiesChooser(), 
				1, 1);
		
		// Debug
/*		drawable.addGLEventListener( new GLEventListener() {
			@Override public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {}
			@Override public void dispose(GLAutoDrawable arg0) {}
			@Override public void display(GLAutoDrawable arg0) {}
			@Override public void init(GLAutoDrawable gad) {
				GL gl = gad.getGL();
				gl = gl.getContext().setGL(  GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err }) );
			}
		});*/
		

		// The GL object has to be running on the AWTEvent thread for UI objects
		//	to access it without causing thread-locking.
		//
		// TODO: Figure out whether this needs to be SwingUtilities.invokeAndWait
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {

				drawable.display();
				drawable.getContext().makeCurrent();		
				initShaders();
			}
		});
	}
	
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
//		drawable.display();
		drawable.getContext().makeCurrent();
		return drawable.getGL().getGL2();
	}
	
	GLAutoDrawable getAutoDrawable() {
		return drawable;
	}
	
/*	// Probably shoudn't exist.
 	public GLAutoDrawable getDrawable() {
		return drawable;
	}*/

	/** Writes the active GL Surface to a BufferedImage */
	public BufferedImage glSurfaceToImage() {
        return new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage( getGL2(), 0, 0, width, height, true); 
		
	}
	
	public void clearSurface(GL2 gl2) {
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
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
		;
	}
	private void setDefaultBlendMode(GL2 gl, ProgramType type) {
        switch( type) {
		case STROKE_BASIC:
		case STROKE_PIXEL:
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
        Mat4 matrix = new Mat4(MatrixBuilder.orthagonalProjectionMatrix(
        		0, params.width, (params.flip)?params.height:0, (params.flip)?0:params.height, -1, 1));
        
        if( trans != null) {
	        Mat4 matrix2 = new Mat4( MatrixBuilder.wrapAffineTransformAs4x4(trans));
	        matrix = matrix2.multiply(matrix);
        }
        
        params.addParam( new GLParameters.GLUniformMatrix4fv(
        		"perspectiveMatrix", 1, true, matrix.getBuffer()));

		PreparedData pd = prepareRawData(new float[]{
			// x  y   u   v
			x1, y1, 0.0f, (internal)?1.0f:0.0f,
			x2, y1, 1.0f, (internal)?1.0f:0.0f,
			x1, y2, 0.0f, (internal)?0.0f:1.0f,
			x2, y2, 1.0f, (internal)?0.0f:1.0f,
		}, gl);
        applyProgram( type, params, pd, x1, y1, x2, y2, gl);
        pd.free();
	}
	
	/** Applies the specified Shader Program with the provided parameters, using 
	 * the basic xyuv texture construction.*/
	private void applyProgram( ProgramType type, GLParameters params, PreparedData pd,
			float x1, float y1, float x2, float y2, GL2 gl) 
	{
		int w = params.width;
		int h = params.height;
//		setSurfaceSize(w, h);
		int prog = getProgram(type);
		gl.getGL2().glViewport(0, 0, w, h);
		

        
        gl.glUseProgram(prog);
        
        // Bind Attribute Streams
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(0, 2, GL2.GL_FLOAT, false, 4*4, 0);
        gl.glVertexAttribPointer(1, 2, GL2.GL_FLOAT, false, 4*4, 4*2);

        // Bind Texture
        if( params.texture != null) {
        	gl.glActiveTexture(GL.GL_TEXTURE0);
        	params.texture.load(gl);
    		gl.glEnable(GL2.GL_TEXTURE_2D);
    		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);
        }
        if( params.texture2 != null) {
        	gl.glActiveTexture(GL.GL_TEXTURE1);
        	params.texture2.load(gl);
    		gl.glEnable(GL2.GL_TEXTURE_2D);
    		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture2"), 1);
        }

		// Bind Uniform
        if( params != null)
        	params.apply(gl, prog);
        
        // Set Blend Mode
        if( params.useBlendMode) {
	        if( params.useDefaultBlendmode)
	        	setDefaultBlendMode(gl, type);
	        else {
		        gl.glEnable(GL.GL_BLEND);
		        gl.glBlendFuncSeparate(
		        		params.bm_sfc, params.bm_dfc, params.bm_sfa, params.bm_dfa);
		        gl.glBlendEquationSeparate(params.bm_fc, params.bm_fa);
	        }
        }

		// Start Draw
		gl.glDrawArrays(GL2.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisable( GL.GL_BLEND);
		
		// Finished Drawing
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisableVertexAttribArray(0);
		gl.glDisableVertexAttribArray(1);
        gl.glUseProgram(0);
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
		
		PreparedTexture( GL2 gl) {
			this.gl = gl;
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
			}
		}
	}
	public PreparedTexture prepareTexture( BufferedImage bi, GL2 gl) {
		PreparedTexture pt = new PreparedTexture(gl);
		int w = bi.getWidth();
		int h = bi.getHeight();

		pt. tex = GLBuffers.newDirectIntBuffer(1);
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

		
		return pt;
	}
	
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
		
		public int getBuffer() {
			return positionBufferObject.get(0);
		}
		
		public void free() {
			if( !_free) {
				_free = true;
				gl.glDeleteVertexArrays(1, vao);
				gl.glDeleteBuffers(1, positionBufferObject);
			}
		}
		
		private boolean _free = false;
		private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1);
		private IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
	}
	public PreparedData prepareRawData( float raw[], GL2 gl) {
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
		
		return pd;
	}
	
	
	// ==============
	// ==== Initialization
	
	private void initShaders() {
        programs[ProgramType.DEFAULT.ordinal()] = loadProgramFromResources(
				"shaders/basic.vert", 
				null, 
				"shaders/square_grad.frag");
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
	}
	
	private int loadProgramFromResources( String vert, String geom, String frag){
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
	
	private int createProgram( GL2 gl, ArrayList<Integer> shaderList) {
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

			MDebug.handleError(ErrorType.RESOURCE, "Link Program." + strInfoLog);

			infoLogLength.clear();
			bufferInfoLog.clear();
        }


		for( Integer shader : shaderList){
			gl.glDetachShader(program, shader);
		}
        status.clear();

        return program;
	}

	private int compileShaderFromResource( GL2 gl, int shaderType, String resource) {
		
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
			MDebug.handleError(ErrorType.RESOURCE, "Failed to compile GLSL shader (" + type + "): " + strInfoLog);
			
			infoLogLength.clear();
			bufferInfoLog.clear();
		}
		lengths.clear();
		status.clear();
		
		return shader;
	}
}
