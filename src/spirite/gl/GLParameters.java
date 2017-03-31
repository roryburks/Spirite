package spirite.gl;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL2;

import spirite.gl.GLEngine.PreparedTexture;

/**
 * GLParameters encapsulates the GL Uniform Parameters so that they
 * can be passed to more abstract GL Rendering methods.
 * 
 * @author Rory Burks
 *
 */
public class GLParameters {
	public final List<GLParam> params = new ArrayList<>();
	public GLTexture texture;
	public int width, height;
	
	public GLParameters( int width, int height) {
		this.width = width;
		this.height = height;
	}
	public GLParameters( GLParameters other) {
		this.width = other.width;
		this.height = other.height;
		this.params.addAll(other.params);
		this.texture = other.texture;
	}
	
	public void addParam( GLParam param) {
		params.add(param);
	}

	public void apply( GL2 gl, int prog) {
		for( GLParam param : params) {
			param.apply(gl, prog);
		}
	}
	
	// ==============
	// ==== Textures
	
	/** A class storing things that can be transferred into  */
	public abstract static class GLTexture {
		public abstract int load();
		public abstract void unload();
	}
	
	public static class GLImageTexture extends GLTexture {
		private PreparedTexture texture;
		private final BufferedImage image;
		
		public GLImageTexture( BufferedImage image) {
			this.image = image;
		}

		@Override
		public int load(){
			texture = GLEngine.getInstance().prepareTexture(image);
			
			return texture.getTexID();
		}
		@Override
		public void unload(){
			texture.free();
		}
	}
	public static class GLFBOTexture extends GLTexture {
		private final GLMultiRenderer glmu;
		public GLFBOTexture( GLMultiRenderer glmu) {
			this.glmu = glmu;
		}
		@Override
		public int load() {
	        //set the texture up to be used for painting a surface ...
			GL2 gl = GLEngine.getInstance().getGL2();
	        gl.glBindTexture(GL2.GL_TEXTURE_2D, glmu.getTexture());
	        int textureTarget = GL2.GL_TEXTURE_2D;
	        gl.glEnable(textureTarget);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_MIN_FILTER,GL2.GL_LINEAR);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_MAG_FILTER,GL2.GL_LINEAR);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_WRAP_S,GL2.GL_REPEAT);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_WRAP_T,GL2.GL_REPEAT);
	        
	        return glmu.getTexture();
		}

		@Override public void unload() {}	
	}
	
	// ==============
	// ==== Specific Parameters

	public abstract static class GLParam {
		protected final String name;
		GLParam( String name) { this.name = name;}
		public abstract void apply( GL2 gl, int prog);
		int getUniformLocation( GL2 gl, int prog) {
			return gl.glGetUniformLocation( prog, name);
		}
	}

	public static class GLParam1i extends GLParam {
		private int i1;
		public GLParam1i( String name, int i1) {
			super(name);
			this.i1 = i1;
		}
		@Override
		public void apply(GL2 gl, int prog) {
			gl.glUniform1i( getUniformLocation(gl, prog), i1);
		}
	}
	public static class GLParam1ui extends GLParam {
		private int i1;
		public GLParam1ui( String name, int i1) {
			super(name);
			this.i1 = i1;
		}
		@Override
		public void apply(GL2 gl, int prog) {
			gl.glUniform1ui( getUniformLocation(gl, prog), i1);
		}
	}
	public static class GLParam1f extends GLParam {
		private float f1;
		public GLParam1f( String name, float f1) {
			super(name);
			this.f1 = f1;
		}
		@Override
		public void apply(GL2 gl, int prog) {
			gl.glUniform1f( getUniformLocation(gl, prog), f1);
		}
	}
	public static class GLParam4f extends GLParam {
		private final float f1, f2, f3, f4;
		public GLParam4f( String name, float f1, float f2, float f3, float f4) {
			super(name);
			this.f1 = f1; this.f2 = f2; this.f3 = f3; this.f4 = f4;
		}
		@Override
		public void apply(GL2 gl, int prog) {
			gl.glUniform4f( getUniformLocation(gl, prog), 
					f1, f2, f3, f4);
		}
	}
	public static class GLUniformMatrix4fv extends GLParam {
		private final FloatBuffer buffer;
		private final int count;
		private final boolean transpose;
		public GLUniformMatrix4fv( String name, int count, boolean transpose, FloatBuffer buffer) {
			super(name);
			this.count = count;
			this.transpose = transpose;
			this.buffer = buffer;
		}
		@Override
		public void apply(GL2 gl, int prog) {
			gl.glUniformMatrix4fv(getUniformLocation(gl, prog), 
					count, transpose, buffer);
		}
	}
	
}
