package spirite.gl;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import mutil.MatrixBuilder;
import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import sun.awt.image.ByteInterleavedRaster;

/**
 * GLEngine is the root point for dealing with OpenGL through JOGL.  It handles
 * the initialization of surfaces and components and manages the resources, 
 * making sure that they are properly de-allocated inside OpenGL
 * 
 * Uses a Singleton paradigm that can be accessed through GLEngine.getInstance()
 * 
 * @author Rory Burks
 */
public class GLEngine  {
	private final GLOffscreenAutoDrawable drawable;
	private int width = 1;
	private int height = 1;

	
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
	public static GLEngine getInstance() {
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
	
	// ============
	// ==== Simple API
	public void setSurfaceSize( int width, int height) {
		if( this.width != width || this.height != height) {
			this.width = width;
			this.height = height;

			this.drawable.setSurfaceSize(width, height);
		}
	}

	public GL3 getGL3() {
		drawable.display();
		drawable.getContext().makeCurrent();
		return drawable.getGL().getGL3();
	}
	
	public GLAutoDrawable getDrawable() {
		return drawable;
	}
	
	public void clearSurface() {
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        getGL3().glClearBufferfv(GL3.GL_COLOR, 0, clearColor);
	}
	
	// =================
	// ==== Program Management
	public enum ProgramType {
		DEFAULT,
		SQARE_GRADIENT,
		BASIC_STROKE,
		CHANGE_COLOR,
		PASS_BORDER,
		PASS_INVERT,
		PASS_BASIC,
		PASS_ESCALATE,
		;
	}
	
	private int programs[] = new int[ProgramType.values().length];
	
	public int getProgram( ProgramType type){
		return programs[type.ordinal()];
	}

	/** Applies the given program using the given parameters, in simple Pass 
	 * format (i.e. drawing the image onto the screen in a 1:1 way, while 
	 * applying a certain fragment shader.	 */
	public void applyPassProgram(ProgramType type, GLParameters params, AffineTransform trans){
		applyPassProgram( type, params, trans, 0, 0, params.width, params.height);
	}

	/** Applies the given program using the given parameters, in simple Pass 
	 * format (i.e. drawing the image onto the screen in a 1:1 way, while 
	 * applying a certain fragment shader.	 
	 * 
	 * x1, y1, x2, y2 describes the area of the screen which the Image should 
	 * be drawn.  For example, when drawing a 128x128 image in a 1000x1000 screen,
	 * you would use 0, 0, 128, 128.
	 */
	public void applyPassProgram(
			ProgramType type, GLParameters params, AffineTransform trans,
			float x1, float y1, float x2, float y2)
	{
		GLParameters modifiedParams = new GLParameters(params);

        Mat4 matrix = new Mat4(MatrixBuilder.orthagonalProjectionMatrix(
        		0, params.width, 0, params.height, -1, 1));
        if( trans != null) {
	        Mat4 matrix2 = new Mat4( MatrixBuilder.wrapAffineTransformAs4x4(trans));
	        matrix = matrix2.multiply(matrix);
        }
        
        modifiedParams.addParam( new GLParameters.GLUniformMatrix4fv(
        		"perspectiveMatrix", 1, true, matrix.getBuffer()));
        
        applyProgram( type, modifiedParams, x1, y1, x2, y2);
	}
	
	/** Applies the specified Shader Program with the provided parameters, using 
	 * the basic xyuv texture construction.*/
	public void applyProgram( ProgramType type, GLParameters params,
			float x1, float y1, float x2, float y2) 
	{
		int w = params.width;
		int h = params.height;
		setSurfaceSize(w, h);
		GL3 gl = getGL3();
		int prog = getProgram(type);

		PreparedData pd = prepareRawData(new float[]{
			// x  y   u   v
			x1, h-y1, 0.0f, 0.0f,
			x2, h-y1, 1.0f, 0.0f,
			x1, h-y2, 0.0f, 1.0f,
			x2, h-y2, 1.0f, 1.0f,
		});

        
        gl.glUseProgram(prog);
        
        // Bind Attribute Streams
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(0, 2, GL3.GL_FLOAT, false, 4*4, 0);
        gl.glVertexAttribPointer(1, 2, GL3.GL_FLOAT, false, 4*4, 4*2);

        // Bind Texture
        if( params.texture != null) {
        	params.texture.load();
    		gl.glEnable(GL3.GL_TEXTURE_2D);
    		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);
        }

		// Bind Uniform
        
        if( params != null)
        	params.apply(gl, prog);

		// Start Draw
        switch( type) {
		case BASIC_STROKE:
		case DEFAULT:
		case PASS_BASIC:
	        gl.glEnable(GL.GL_BLEND);
	        gl.glBlendFuncSeparate(
	        		GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA,
	        		GL3.GL_ONE, GL3.GL_ONE);
	        gl.glBlendEquation(GL3.GL_FUNC_ADD);
	        break;
		case PASS_BORDER:
		case CHANGE_COLOR:
		case PASS_INVERT:
		case SQARE_GRADIENT:
		case PASS_ESCALATE:
	        gl.glEnable(GL.GL_BLEND);
	        gl.glBlendFunc(GL3.GL_ONE, GL3.GL_ONE);
	        gl.glBlendEquation(GL3.GL_MAX);
	        break;
        
        }
		gl.glDrawArrays(GL3.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisable( GL.GL_BLEND);
		
		// Finished Drawing
		gl.glDisable(GL3.GL_TEXTURE_2D);
		gl.glDisableVertexAttribArray(0);
		gl.glDisableVertexAttribArray(1);
        gl.glUseProgram(0);
		pd.free();
        if( params.texture != null) {
        	params.texture.unload();
        }
	}
	
	/** Writes the active GL Surface to a BufferedImage */
	public BufferedImage glSurfaceToImage() {
        return new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage( getGL3(), 0, 0, width, height, false); 
		
	}
	
	// ==================
	// ==== Texture Preperation
	public class PreparedTexture{
		private IntBuffer tex = GLBuffers.newDirectIntBuffer(1);
		
		@Override
		protected void finalize() throws Throwable {
			free();
			super.finalize();
		}
		
		public int getTexID() {return tex.get(0);}
		private boolean _free = false;
		
		public void free() {
			if( !_free) {
				GL3 gl = drawable.getGL().getGL3();
				_free = true;
				gl.glDeleteTextures(1, tex);
			}
		}
	}
	public PreparedTexture prepareTexture( BufferedImage bi) {
		GL3 gl = drawable.getGL().getGL3();
		PreparedTexture pt = new PreparedTexture();
		int w = bi.getWidth();
		int h = bi.getHeight();

		pt. tex = GLBuffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, pt.tex);
        gl.glBindTexture(GL3.GL_TEXTURE_2D, pt.tex.get(0));
		gl.glTexParameteri( GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
		gl.glTexParameteri( GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
		gl.glTexParameteri( GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri( GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
		
		gl.glTexImage2D(
				GL3.GL_TEXTURE_2D,
				0,
				GL3.GL_RGBA,
				w, h,
				0,
				GL3.GL_RGBA,
				GL3.GL_UNSIGNED_INT_8_8_8_8,
				ByteBuffer.wrap(((ByteInterleavedRaster)bi.getRaster()).getDataStorage())
				);
		
		return pt;
	}
	
	// =================
	// ==== Data Buffer Preperation
	public class PreparedData{		
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
				GL3 gl = drawable.getGL().getGL3();

				gl.glDeleteVertexArrays(1, vao);
				gl.glDeleteBuffers(1, positionBufferObject);
			}
		}
		
		private boolean _free = false;
		private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1);
		private IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
	}
	public PreparedData prepareRawData( float raw[]) {
		GL3 gl = drawable.getGL().getGL3();
		
		PreparedData pd = new PreparedData();
		
		FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(raw);

	    gl.glGenBuffers(1, pd.positionBufferObject);
	    gl.glBindBuffer( GL3.GL_ARRAY_BUFFER, pd.positionBufferObject.get(0));
	    gl.glBufferData(
	    		GL3.GL_ARRAY_BUFFER, 
	    		vertexBuffer.capacity()*Float.BYTES, 
	    		vertexBuffer, 
	    		GL3.GL_STREAM_DRAW);
        gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

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
				"shaders/square_grad.vert", 
				null, 
				"shaders/square_grad.frag");
        programs[ProgramType.BASIC_STROKE.ordinal()] = loadProgramFromResources(
				"shaders/stroke_basic.vert", 
				"shaders/stroke_basic.geom", 
				"shaders/stroke_basic.frag");
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
        programs[ProgramType.PASS_BASIC.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_basic.frag");
        programs[ProgramType.PASS_ESCALATE.ordinal()] = loadProgramFromResources( 
				"shaders/pass.vert", 
				null, 
				"shaders/pass_escalate.frag");
        		
	}
	
	private int loadProgramFromResources( String vert, String geom, String frag){
		GL3 gl = drawable.getGL().getGL3();

        ArrayList<Integer> shaderList = new ArrayList<>();
        
        if( vert != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GL3.GL_VERTEX_SHADER, vert));
        }
        if( geom != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GL3.GL_GEOMETRY_SHADER, geom));
        }
        if( frag != null ){
            shaderList.add( compileShaderFromResource(
    				gl, GL3.GL_FRAGMENT_SHADER, frag));
        }
        
		int ret = createProgram( gl, shaderList);
		

        for( Integer shader : shaderList) {
        	gl.glDeleteShader(shader);
        }
        
        return ret;
	}
	
	private int createProgram( GL3 gl, ArrayList<Integer> shaderList) {
		// Create and Link Program
		int program = gl.glCreateProgram();

		for( Integer shader : shaderList){
			gl.glAttachShader(program, shader);	
		}
		gl.glLinkProgram(program);
		
		// Check for Errors
		IntBuffer status = GLBuffers.newDirectIntBuffer(1);
        gl.glGetProgramiv(program, GL3.GL_LINK_STATUS, status);
        if (status.get(0) == GL3.GL_FALSE) {

            IntBuffer infoLogLength = GLBuffers.newDirectIntBuffer(1);
            gl.glGetProgramiv(program, GL3.GL_INFO_LOG_LENGTH, infoLogLength);

            ByteBuffer bufferInfoLog = GLBuffers.newDirectByteBuffer(infoLogLength.get(0));
            gl.glGetProgramInfoLog(program, infoLogLength.get(0), null, bufferInfoLog);
            byte[] bytes = new byte[infoLogLength.get(0)];
            bufferInfoLog.get(bytes);
            String strInfoLog = new String(bytes);

			MDebug.handleError(ErrorType.RESOURCE, "Link Program." + strInfoLog);

			infoLogLength.clear();
			bufferInfoLog.clear();
        }

        shaderList.forEach(shader -> gl.glDetachShader(program, shader));

        status.clear();

        return program;
	}

	private int compileShaderFromResource( GL3 gl, int shaderType, String resource) {
		
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
		gl.glGetShaderiv( shader, GL3.GL_COMPILE_STATUS, status);
		if( status.get(0) == GL3.GL_FALSE) {
			
			// Read Compile Errors
			IntBuffer infoLogLength = GLBuffers.newDirectIntBuffer(1);
			gl.glGetShaderiv( shader,  GL3.GL_INFO_LOG_LENGTH, infoLogLength);
			
			ByteBuffer bufferInfoLog = GLBuffers.newDirectByteBuffer(infoLogLength.get(0));
			gl.glGetShaderInfoLog( shader, infoLogLength.get(0), null, bufferInfoLog);
			byte[] bytes = new byte[infoLogLength.get(0)];
			bufferInfoLog.get(bytes);
			String strInfoLog = new String(bytes);
			
			String type = "";
			switch( shaderType) {
			case GL3.GL_VERTEX_SHADER:
				type = "vertex";
				break;
			case GL3.GL_GEOMETRY_SHADER:
				type = "geometry";
				break;
			case GL3.GL_FRAGMENT_SHADER:
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
