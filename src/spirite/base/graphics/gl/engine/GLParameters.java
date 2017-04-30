package spirite.base.graphics.gl.engine;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL2;

import spirite.base.graphics.gl.engine.GLEngine.PreparedTexture;

/**
 * GLParameters encapsulates many of the attributes representing "how" OpenGL
 * should render data.  These include:
 * -Shader Uniforms
 * -Logical Representations of textures to be loaded
 * -Dimensions of the Orthogonal Projection to be used
 * -Whether or not that projection should be flipped vertically
 * -Which BlendModes to use (or if to use them at all) 
 * 
 * @author Rory Burks
 *
 */
public class GLParameters {
	final List<GLParam> params = new ArrayList<>();
	public GLTexture texture;
	public GLTexture texture2;
	public int width, height;
	public boolean flip = false;

	public boolean useBlendMode = true;
	boolean useDefaultBlendmode = true;
	int bm_sfc, bm_sfa, bm_dfc, bm_dfa, bm_fc, bm_fa;
	
	public GLParameters( int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void addParam( GLParam param) {
		params.add(param);
	}
	
	public void clearParams() {
		params.clear();
	}

	// Not sure if this needs to be package-scoped
	void apply( GL2 gl, int prog) {
		for( GLParam param : params) {
			param.apply(gl, prog);
		}
		for( GLParam param : internalParams) {
			param.apply(gl, prog);
		}
	}
	
	// =============
	// ==== Internal Parameters
	//	In order to make GLParameters re-useable but not add time/energy/modularity
	//	bloat by copying everything internally, a separate Internal Parameter list
	//	is maintained where the GLEngine can add GLParams that are needed internally
	//	(such as the perspectiveMatrix) to be removed once it's finished with them.
	//
	//	NOTE: this makes GLParameters somewhat less Multithread friendly, but excessive
	//	care is already needed working with JOGL in multiple threads.

	final List<GLParam> internalParams = new ArrayList<>();
	
	void addInternalParam( GLParam param) {
		internalParams.add(param);
	}
	void clearInternalParams() {
		internalParams.clear();
	}
	
	// =============
	// ==== BlendModes
	public void useDefaultBlendMode() {
		useDefaultBlendmode = true;
	}
	public void setUseBlendMode( boolean use) {
		useBlendMode = use;
	}
	public void setBlendMode( int src_factor, int dst_factor, int formula) {
		useDefaultBlendmode = false;
		bm_sfc=bm_sfa = src_factor;
		bm_dfc=bm_dfa = dst_factor;
		bm_fc = bm_fa = formula;
	}
	public void setBlendModeExt( 
			int src_factor_color, int dst_factor_color, int formula_color,
			int src_factor_alpha, int dst_factor_alpha, int formula_alpha)
	{
		useDefaultBlendmode = false;
		bm_sfc=  src_factor_color;
		bm_dfc= dst_factor_color;
		bm_fc = formula_color;
		
		bm_sfa=  src_factor_alpha;
		bm_dfa= dst_factor_alpha;
		bm_fa = formula_alpha;
		
	}
	
	// ==============
	// ==== Textures
	
	/** A class storing things that can be transferred into  */
	public abstract static class GLTexture {
		public abstract int load(GL2 gl);
		public abstract void unload();
		public abstract int getWidth();
		public abstract int getHeight();
	}
	
	
	public static class GLImageTexture extends GLTexture {
		private PreparedTexture texture;
		private final BufferedImage image;
		
		public GLImageTexture( BufferedImage image) {
			this.image = image;
		}

		@Override
		public int load(GL2 gl){
			texture = GLEngine.getInstance().prepareTexture(image, gl);

			return texture.getTexID();
		}
		@Override
		public void unload(){
			texture.free();
		}

		@Override public int getWidth() {
			return (image == null)?0:image.getWidth();
		}
		@Override public int getHeight() {
			return (image == null)?0:image.getHeight();
		}
	}
	public static class GLFBOTexture extends GLTexture {
		private final GLMultiRenderer glmu;
		public GLFBOTexture( GLMultiRenderer glmu) {
			this.glmu = glmu;
		}
		@Override
		public int load(GL2 gl) {
	        //set the texture up to be used for painting a surface ...
	        gl.glBindTexture(GL2.GL_TEXTURE_2D, glmu.getTexture());
	        int textureTarget = GL2.GL_TEXTURE_2D;
	        gl.glEnable(textureTarget);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_MIN_FILTER,GL2.GL_NEAREST);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_MAG_FILTER,GL2.GL_NEAREST);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_WRAP_S,GL2.GL_REPEAT);
	        gl.glTexParameteri(textureTarget,GL2.GL_TEXTURE_WRAP_T,GL2.GL_REPEAT);
	        
	        return glmu.getTexture();
		}

		@Override public void unload() {}
		@Override public int getWidth() { return glmu.width; }
		@Override public int getHeight() { return glmu.height; }	
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
	public static class GLParam3f extends GLParam {
		private final float f1, f2, f3;
		public GLParam3f( String name, float f1, float f2, float f3) {
			super(name);
			this.f1 = f1; this.f2 = f2; this.f3 = f3;
		}
		@Override
		public void apply(GL2 gl, int prog) {
			gl.glUniform3f( getUniformLocation(gl, prog), 
					f1, f2, f3);
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
