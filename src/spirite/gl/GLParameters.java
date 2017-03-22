package spirite.gl;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL3;

/**
 * GLParameters encapsulates the GL Uniform Parameters so that they
 * can be passed to more abstract GL Rendering methods.
 * 
 * @author Rory Burks
 *
 */
public class GLParameters {
	public abstract static class GLParam {
		protected final String name;
		GLParam( String name) { this.name = name;}
		public abstract void apply( GL3 gl, int prog);
		int getUniformLocation( GL3 gl, int prog) {
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
		public void apply(GL3 gl, int prog) {
			gl.glUniform1i( getUniformLocation(gl, prog), i1);
		}
	}
	public static class GLParam4f extends GLParam {
		private final float f1, f2, f3, f4;
		public GLParam4f( String name, float f1, float f2, float f3, float f4) {
			super(name);
			this.f1 = f1; this.f2 = f2; this.f3 = f3; this.f4 = f4;
		}
		@Override
		public void apply(GL3 gl, int prog) {
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
		public void apply(GL3 gl, int prog) {
			gl.glUniformMatrix4fv(getUniformLocation(gl, prog), 
					count, transpose, buffer);
		}
	}
	
	private final List<GLParam> params = new ArrayList<>();
	
	public void addParam( GLParam param) {
		params.add(param);
	}

	public void apply( GL3 gl, int prog) {
		for( GLParam param : params) {
			param.apply(gl, prog);
		}
	}
}
