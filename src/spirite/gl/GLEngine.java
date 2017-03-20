package spirite.gl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;

public class GLEngine  {
	private final GLOffscreenAutoDrawable drawable;
	private int width = 1;
	private int height = 1;

	
	/** Namespace for Attribute Bindings */
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
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {

				drawable.display();
				drawable.getContext().makeCurrent();		
				initShaders();
			}
		});
	}
	
	// API
	public void setSurfaceSize( int width, int height) {
		if( this.width != width || this.height != height) {
			this.width = width;
			this.height = height;

			this.drawable.setSurfaceSize(width, height);
		}
	}

	public GL4 getGL4() {
		drawable.display();
		drawable.getContext().makeCurrent();
//		this.drawable.getContext().makeCurrent();
		return this.drawable.getGL().getGL4();
	}
	public GL2 getGL2() {
//		this.drawable.getContext().makeCurrent();
		return this.drawable.getGL().getGL2();
	}
	
	public GLAutoDrawable getDrawable() {
		return drawable;
	}
	
	public enum ProgramType {
		DEFAULT,
		SQARE_GRADIENT,
		BASIC_STROKE,
		CHANGE_COLOR,
		PASS_BORDER,
		;
	}
	
	private int programs[] = new int[ProgramType.values().length];
	
	public int getProgram( ProgramType type){
		return programs[type.ordinal()];
	}
	
	// :::: Texture Preperation
	public class PreparedTexture{
		Texture texture;
		
		@Override
		protected void finalize() throws Throwable {
			free();
			super.finalize();
		}
		
		boolean _free = false;
		
		public void free() {
			if( !_free) {
				GL4 gl = drawable.getGL().getGL4();
				_free = true;
				texture.disable(gl);
				texture.destroy(gl);
			}
		}
	}
	public PreparedTexture prepareTexture( BufferedImage bi) {
		GL4 gl = drawable.getGL().getGL4();
		PreparedTexture pt = new PreparedTexture();
		pt.texture = AWTTextureIO.newTexture(gl.getGLProfile(), bi, false);

		pt.texture.setTexParameteri(gl, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
		pt.texture.setTexParameteri(gl, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
		pt.texture.setTexParameteri(gl, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
		pt.texture.setTexParameteri(gl, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);

		pt.texture.enable(gl);
		pt.texture.bind(gl);

/*		IntBuffer tex = GLBuffers.newDirectIntBuffer(1);
        gl.glGenTextures(1, tex);
        gl.glBindTexture(GL.GL_TEXTURE_2D, tex.get(0));
		gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
		gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
		gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(
				GL4.GL_TEXTURE_2D,
				0,
				GL4.GL_RGBA,
				w, h,
				0,
				GL4.GL_RGBA,
				GL4.GL_UNSIGNED_BYTE,
				MUtil.bufferFromImage(bi)
				);*/
		
		return pt;
	}
	
	// :::: Data Buffer Preperation
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
				GL4 gl = drawable.getGL().getGL4();

				gl.glDeleteVertexArrays(1, vao);
				gl.glDeleteBuffers(1, positionBufferObject);
			}
		}
		
		private boolean _free = false;
		private IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1);
		private IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
	}
	public PreparedData prepareRawData( float raw[]) {
		GL4 gl = drawable.getGL().getGL4();
		
		PreparedData pd = new PreparedData();
		
		FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(raw);

	    gl.glGenBuffers(1, pd.positionBufferObject);
	    gl.glBindBuffer( GL4.GL_ARRAY_BUFFER, pd.positionBufferObject.get(0));
	    gl.glBufferData(
	    		GL4.GL_ARRAY_BUFFER, 
	    		vertexBuffer.capacity()*Float.BYTES, 
	    		vertexBuffer, 
	    		GL4.GL_STREAM_DRAW);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

		gl.glGenVertexArrays(1, pd.vao);
		gl.glBindVertexArray(pd.vao.get(0));
		
		return pd;
	}
	
	
	// Initialization
	
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
        		
	}
	
	private int loadProgramFromResources( String vert, String geom, String frag){
		GL4 gl = drawable.getGL().getGL4();

        ArrayList<Integer> shaderList = new ArrayList<>();
        
        if( vert != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GL4.GL_VERTEX_SHADER, vert));
        }
        if( geom != null) {
            shaderList.add( compileShaderFromResource(
    				gl, GL4.GL_GEOMETRY_SHADER, geom));
        }
        if( frag != null ){
            shaderList.add( compileShaderFromResource(
    				gl, GL4.GL_FRAGMENT_SHADER, frag));
        }
        
		int ret = createProgram( gl, shaderList);
		

        for( Integer shader : shaderList) {
        	gl.glDeleteShader(shader);
        }
        
        return ret;
	}
	
	private int createProgram( GL4 gl, ArrayList<Integer> shaderList) {
		// Create and Link Program
		int program = gl.glCreateProgram();

		for( Integer shader : shaderList){
			gl.glAttachShader(program, shader);	
		}
		gl.glLinkProgram(program);
		
		// Check for Errors
		IntBuffer status = GLBuffers.newDirectIntBuffer(1);
        gl.glGetProgramiv(program, GL4.GL_LINK_STATUS, status);
        if (status.get(0) == GL4.GL_FALSE) {

            IntBuffer infoLogLength = GLBuffers.newDirectIntBuffer(1);
            gl.glGetProgramiv(program, GL4.GL_INFO_LOG_LENGTH, infoLogLength);

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

	private int compileShaderFromResource( GL4 gl, int shaderType, String resource) {
		
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
		gl.glGetShaderiv( shader, GL4.GL_COMPILE_STATUS, status);
		if( status.get(0) == GL4.GL_FALSE) {
			
			// Read Compile Errors
			IntBuffer infoLogLength = GLBuffers.newDirectIntBuffer(1);
			gl.glGetShaderiv( shader,  GL4.GL_INFO_LOG_LENGTH, infoLogLength);
			
			ByteBuffer bufferInfoLog = GLBuffers.newDirectByteBuffer(infoLogLength.get(0));
			gl.glGetShaderInfoLog( shader, infoLogLength.get(0), null, bufferInfoLog);
			byte[] bytes = new byte[infoLogLength.get(0)];
			bufferInfoLog.get(bytes);
			String strInfoLog = new String(bytes);
			
			String type = "";
			switch( shaderType) {
			case GL4.GL_VERTEX_SHADER:
				type = "vertex";
				break;
			case GL4.GL_GEOMETRY_SHADER:
				type = "geometry";
				break;
			case GL4.GL_FRAGMENT_SHADER:
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
