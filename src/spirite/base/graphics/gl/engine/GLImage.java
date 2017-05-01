package spirite.base.graphics.gl.engine;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.gl.GLGraphics;
import spirite.base.image_data.RawImage;
import spirite.base.util.glmath.GLC;

public class GLImage extends RawImage {
	private static final GLEngine engine = GLEngine.getInstance();
	private final int width, height;
	int tex;
	
	public GLImage( int width, int height) {
		GL2 gl = engine.getGL2();
		
		this.width = width;
		this.height = height;
		
		int result[] = new int[1];
		gl.glGenTextures(1, result, 0);
		this.tex = result[0];
        gl.glBindTexture( GLC.GL_TEXTURE_2D, tex);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MIN_FILTER,GLC.GL_NEAREST);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MAG_FILTER,GLC.GL_NEAREST);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_S,GLC.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_T,GLC.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GLC.GL_TEXTURE_2D,0,GL.GL_RGBA8,
        		width, height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
	}
	
	@Override public int getWidth() { return width; }
	@Override public int getHeight() { return height; }
	public int getTexID() {return tex;}

	@Override
	public void flush() {
		if( tex != 0) {
			GL2 gl = engine.getGL2();
			if( engine.getTarget() == tex)
				engine.setTarget(0);
			gl.glDeleteTextures(1, new int[]{tex}, 0);
		}
	}

	@Override
	public int getByteSize() {
		return width*height*4;
	}

	@Override
	public RawImage deepCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GLGraphics getGraphics() {
		return new GLGraphics(this);
	}

	@Override
	public int getRGB(int x, int y) {
		// TODO Auto-generated method stub
		return 0;
	}

}
