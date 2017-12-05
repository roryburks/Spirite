package spirite.base.graphics.gl;

import com.jogamp.opengl.GL2;
import spirite.base.graphics.RawImage;
import spirite.base.util.glu.GLC;
import spirite.hybrid.HybridHelper;

import java.nio.IntBuffer;

public class GLImage implements RawImage {
	private static final GLEngine engine = GLEngine.getInstance();
	private final int width, height;
	int tex;
	boolean glOriented = true;
	
	public GLImage( int width, int height) throws InvalidImageDimensionsExeption {
		if( width <= 0 || height <= 0)
			throw new InvalidImageDimensionsExeption("Invalid Image Dimensions");
		engine.glImageLoaded(this);
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
        gl.glTexImage2D(GLC.GL_TEXTURE_2D,0,GL2.GL_RGBA8,
        		width, height, 0, GLC.GL_RGBA, GLC.GL_UNSIGNED_BYTE, null);
	}
	
	public GLImage( GLImage toCopy) {
		engine.glImageLoaded(this);
		GL2 gl = engine.getGL2();
		this.width = toCopy.width;
		this.height = toCopy.height;
		
		// Set the GL Target as the other image's texture and copy the data
		engine.setTarget(toCopy.tex);
		int result[] = new int[1];
		gl.glGenTextures(1, result, 0);
		this.tex = result[0];
        gl.glBindTexture( GLC.GL_TEXTURE_2D, tex);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MIN_FILTER,GLC.GL_NEAREST);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_MAG_FILTER,GLC.GL_NEAREST);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_S,GLC.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLC.GL_TEXTURE_2D,GLC.GL_TEXTURE_WRAP_T,GLC.GL_CLAMP_TO_EDGE);
        gl.glCopyTexImage2D(GLC.GL_TEXTURE_2D,0,GL2.GL_RGBA8,
        		0, 0, width, height, 0);
	}
	
	public GLImage( int texID, int width, int height, boolean glOriented) {
		engine.glImageLoaded(this);
		this.tex = texID;
		this.width = width;
		this.height = height;
		this.glOriented = glOriented;
	}
	
	@Override public int getWidth() { return width; }
	@Override public int getHeight() { return height; }
	public int getTexID() {return tex;}

	@Override
	public void flush() {
		if( tex != 0) {
			final int toDel = tex;
			tex = 0;
			
			// This is needed to make sure that it happens on the AWT Thread to prevent JOGL-internal deadlocks
			HybridHelper.queueToRun( () -> {
				engine.glImageUnloaded(this);
				GL2 gl = engine.getGL2();
				if( engine.getTarget() == toDel)
					engine.setTarget(0);
				gl.glDeleteTextures(1, new int[]{toDel}, 0);
			});
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		flush();
		super.finalize();
	}

	@Override
	public int getByteSize() {
		return width*height*4;
	}

	@Override
	public RawImage deepCopy() {
		return new GLImage( this);
	}

	@Override
	public GLGraphics getGraphics() {
		return new GLGraphics(this);
	}

	@Override
	public int getRGB(int x, int y) {
		if( x < 0 || y < 0 || x >= width || y >= height) return 0;
		GL2 gl = engine.getGL2();
		
		engine.setTarget(this);
		IntBuffer read = IntBuffer.allocate(1);
		gl.glReadnPixels( x, height-y-1, 1, 1, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, 4, read);
		return read.get(0);
	}

	public void setGLOriented( boolean set) {this.glOriented = set;}
	@Override
	public boolean isGLOriented() {
		return glOriented;
	}

}
