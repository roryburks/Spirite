package spirite.gl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import sun.misc.IOUtils;
import sun.security.util.Length;

public class GLEngine implements GLEventListener {
	private final GLOffscreenAutoDrawable drawable;
	private int width = 1;
	private int height = 1;

	private int defaultProgram;
	private int sgProgram;
	
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
		SQARE_GRADIENT
	}
	public int getProgram( ProgramType type){
		switch( type) {
		case DEFAULT:
			return this.defaultProgram;
		case SQARE_GRADIENT:
			return this.sgProgram;
		}
		return 0;
	}
	
	
	// Initialization
	
	private void initShaders() {
		GL4 gl = drawable.getGL().getGL4();

        ArrayList<Integer> shaderList = new ArrayList<>();
        
        shaderList.add( compileShaderFromResource(
				gl,
				GL4.GL_VERTEX_SHADER,
				"shaders/basic.vert"));
        shaderList.add( compileShaderFromResource(
				gl,
				GL4.GL_FRAGMENT_SHADER,
				"shaders/basic.frag"));
        
        defaultProgram = createProgram( gl, shaderList);
        
        for( Integer shader : shaderList) {
        	gl.glDeleteShader(shader);
        }
        

        shaderList = new ArrayList<>();
        
        shaderList.add( compileShaderFromResource(
				gl,
				GL4.GL_VERTEX_SHADER,
				"shaders/square_grad.vert"));
        shaderList.add( compileShaderFromResource(
				gl,
				GL4.GL_FRAGMENT_SHADER,
				"shaders/square_grad.frag"));
        
        sgProgram = createProgram( gl, shaderList);
        
        for( Integer shader : shaderList) {
        	gl.glDeleteShader(shader);
        }
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

			MDebug.handleError(ErrorType.RESOURCE, (Object)null, "Link Program." + strInfoLog);

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
			MDebug.handleError(ErrorType.RESOURCE, (Object)null, "Failed to compile GLSL shader (" + type + "): " + strInfoLog);
			
			infoLogLength.clear();
			bufferInfoLog.clear();
		}
		lengths.clear();
		status.clear();
		
		return shader;
	}

	@Override
	public void display(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
		// TODO Auto-generated method stub
		
	}
}
