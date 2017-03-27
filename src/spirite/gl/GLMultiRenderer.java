package spirite.gl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;

import spirite.MDebug;
import spirite.MDebug.WarningType;

public class GLMultiRenderer {
	private final GL2 gl;
	private final int width, height;
	

	private int fbo;
	private int tex;
	
	public GLMultiRenderer( int width, int height, GL2 gl) {
		this.width = width;
		this.height = height;
		this.gl = gl;
	}
	
	private void checkFramebuffer() {

        switch( gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER)) {
        case GL.GL_FRAMEBUFFER_COMPLETE:
    		break;
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
        case GL3.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS :
        	MDebug.handleWarning(WarningType.UNSUPPORTED, null, "Bad FrameBuffer construction. GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS");
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
        
        
        // Attach Texture to FBO
        gl.glFramebufferTexture2D( GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, 
        		GL.GL_TEXTURE_2D, tex, 0);
        
        checkFramebuffer();
        gl.glBindFramebuffer( GL.GL_FRAMEBUFFER, 0);
	}
		
	
	public static interface GLRenderer {
		public void render(GL gl);
	}
	
	public void render( GLRenderer renderer ) {
		gl.glPushAttrib(GL2.GL_TRANSFORM_BIT | GL2.GL_ENABLE_BIT | GL2.GL_COLOR_BUFFER_BIT);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glDepthMask(false);
		
		// Bind Framebuffer
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo);
		gl.glPushAttrib( GL2.GL_VIEWPORT_BIT);
		gl.glViewport(0, 0, width, height);

		
		renderer.render(gl);
		gl.glPopAttrib();
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		gl.glPopAttrib();

	}
	
	public int getTexture() {
		return this.tex;
	}
//	public void render
	
	public void cleanup() {
		gl.glDeleteFramebuffers(1, Buffers.newDirectIntBuffer(fbo));
		gl.glDeleteTextures(1, Buffers.newDirectIntBuffer(tex));
	}
}
