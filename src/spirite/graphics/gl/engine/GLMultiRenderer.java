package spirite.graphics.gl.engine;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.graphics.gl.GLGraphics;


/**
 * GLMultiRenderer encapsulates the behavior of a FrameBuffer Object, used both
 * for multi-pass rendering and to store off-screen surfaces as cached GL Textures.
 * 
 * Being containers for OpenGL resources that need to be de-allocated manually, 
 * GLMultiRenderers are tied closely to the GLEngine, which keeps a list of all
 * active GLMultiRenderers at any given time.
 * 
 * @author Rory Burks
 *
 */
public class GLMultiRenderer {
	private final GL2 gl;
	public final int width, height;
	private static final GLEngine engine = GLEngine.getInstance();

	private int fbo;
	private int dbo;
	private int tex;
	
	public GLMultiRenderer( int width, int height, GL2 gl) {
		this.width = width;
		this.height = height;
		this.gl = gl;
		
	}
	
	/** Checks is the FrameBuffer was successfully created, displaying a proper
	 * Error message if it wasn't.
	 */
	private void checkFramebuffer() {
        switch( gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)) {
        case GL.GL_FRAMEBUFFER_COMPLETE:
    		return;
        case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT.");
    		break;	
        case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
    		break;	
        case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
    		break;	
        case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_FORMATS");
    		break;	
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
    		break;	
        case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
    		break;	
        case GL.GL_FRAMEBUFFER_UNSUPPORTED:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_UNSUPPORTED");
    		break;	
        case GL.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE :
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE");
    		break;	
        case GL2.GL_FRAMEBUFFER_UNDEFINED :
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction.GL_FRAMEBUFFER_UNDEFINED");
    		break;	
        case GL.GL_INVALID_ENUM:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_INVALID_ENUM");
    		break;	
        default:
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction (in an unknown way).");
    		break;
        }
	}
	
	/** Loads the frame buffer object and prepares it for use.  
	 * 
	 * NOTE: cleanup() must be called at some point after calling this
	 * so that the native OpenGL resources get freed.
	 */
	public void init() {
		// Allocate FBO
		int[] result = new int[1];
		gl.glGenFramebuffers(1, result, 0);
		this.fbo = result[0];
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
		
		// Allocate Color Texture
		gl.glGenTextures(1, result, 0);
		this.tex = result[0];
        gl.glBindTexture( GL.GL_TEXTURE_2D, tex);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MIN_FILTER,GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_MAG_FILTER,GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_WRAP_S,GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D,GL.GL_TEXTURE_WRAP_T,GL.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL.GL_TEXTURE_2D,0,GL.GL_RGBA8,
        		width, height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
        
        // Create a (nearly) empty depth-buffer as a placeholder
        gl.glGenRenderbuffers( 1, result, 0);
        this.dbo = result[0];
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, dbo);
        gl.glRenderbufferStorage( GL.GL_RENDERBUFFER, GL.GL_DEPTH_COMPONENT16, 1, 1);
        gl.glFramebufferRenderbuffer( GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, dbo);
        
        // TODO: Check for Out of Memory and handle appropriately
//        System.out.println(gl.glGetError());
        
        // Attach Texture to FBO
        gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, 
        		GL.GL_TEXTURE_2D, tex, 0);
        
        checkFramebuffer();
        gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, 0);
        

		engine.c_glmus.add(this);
	}
		
	/** A GLRenderer is passed to the GLMU's render method.  Any GL code inside
	 * the render method of the GLRenderer will be applied to the FrameBuffer
	 * assosciated with the GLMU*/
	public static interface GLRenderer {
		public void render(GL gl);
	}
	
	/** Renders the given GL code within the context of the encapsulated FrameBuffer */
	public void render( GLRenderer renderer ) {
//		gl.glPushAttrib(GL2.GL_TRANSFORM_BIT | GL2.GL_ENABLE_BIT | GL2.GL_COLOR_BUFFER_BIT);
//		gl.glDisable(GL.GL_DEPTH_TEST);
//		gl.glDepthMask(false);
		
		// Bind Framebuffer
//		GLGraphics glgc = new GLGraphics(this);
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
//		gl.glPushAttrib( GL2.GL_VIEWPORT_BIT);
//		gl.glViewport(0, 0, width, height);

		renderer.render(gl);
//		gl.glPopAttrib();
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
//		gl.glPopAttrib();
	}
	
	/** In order to use GLGraphics methods from within the GLRenderer, you should
	 * use this method otherwise the viewport will not get properly updated to match
	 * the size of the GLMU's FBO
	 */
	public void render( GLRenderer renderer, GLGraphics glgc) {
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);

		glgc.useFBO(this);
		renderer.render(gl);
		glgc.unuseFBO();
		
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
	}
	
	/** Gets the ID of the Color Texture that GLMU uses.*/
	public int getTexture() {
		return this.tex;
	}
//	public void render
	
	public void cleanup() {
		gl.glDeleteTextures(1, new int[]{tex}, 0);
		gl.glDeleteRenderbuffers(1, new int[]{dbo}, 0);
		gl.glDeleteFramebuffers(1, new int[]{fbo}, 0); 

		engine.c_glmus.remove(this);
	}
}
