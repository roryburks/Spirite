package spirite.gl;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import com.hackoeur.jglm.Vec4;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import spirite.MUtil;
import spirite.gl.GLEngine.PreparedData;
import spirite.gl.GLEngine.ProgramType;
import spirite.gl.GLUIDraw.GradientType;

public class JOGLDrawer {
	private final GLEngine engine = GLEngine.getInstance();
	
	public JOGLDrawer() {
	}

    static int width = 640; 
    static int height = 480; 
    static int numPoints = 1000; 
    static Random r = new Random(); 

    
    public void changeColor( BufferedImage bi, Color from, Color to) {
		GL4 gl = engine.getGL4();
		
		int w = bi.getWidth();
		int h = bi.getHeight();

        try {
			ImageIO.write(bi, "png", new File("E:/Test1.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PreparedData pd = engine.prepareRawData(new float[]{
			// x    y      u     v
			-1.0f, -1.0f, 0.0f, 1.0f,
			+1.0f, -1.0f, 1.0f, 1.0f,
			-1.0f, +1.0f, 0.0f, 0.0f,
			+1.0f, +1.0f, 1.0f, 0.0f,
		});

		// Clear Surface
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL4.GL_COLOR, 0, clearColor);
        
        int prog = engine.getProgram(ProgramType.CHANGE_COLOR);
        gl.glUseProgram(prog);
        
        // Bind Attribute Streams
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, pd.getBuffer());
        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 4*4, 0);
        gl.glVertexAttribPointer(1, 2, GL4.GL_FLOAT, false, 4*4, 4*2);

        // Bind Texture
//		Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), bi, false);
//		Texture texture = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), bi, false));

		IntBuffer tex = GLBuffers.newDirectIntBuffer(1);
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
				);
		
		gl.glUniform1i(gl.glGetUniformLocation(prog, "myTexture"), 0);
		
		// Bind Uniforms
		int cFrom = gl.glGetUniformLocation(prog, "cFrom");
		int cTo = gl.glGetUniformLocation(prog, "cTo");
		gl.glUniform4f( cFrom, from.getRed()/255f, from.getGreen()/255f, from.getBlue()/255f, from.getAlpha()/255f);
		gl.glUniform4f( cTo, to.getRed()/255f, to.getGreen()/255f, to.getBlue()/255f, to.getAlpha()/255f);

		System.out.println(from.getRed()/255f);
		System.out.println(from.getGreen()/255f);
		System.out.println(from.getBlue()/255f);
		System.out.println(from.getAlpha()/255f);
		
		// Start Draw
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL4.GL_ONE, GL4.GL_ONE);
        gl.glBlendEquation(GL4.GL_MAX);
		gl.glDrawArrays(GL4.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisable( GL.GL_BLEND);
		
		// Finished Drawing
		gl.glDisableVertexAttribArray(0);
		gl.glDisableVertexAttribArray(1);
		gl.glDeleteTextures(1, tex);
//		texture.disable(gl);
//		texture.destroy(gl);
		pd.free();
		
		GLAutoDrawable drawable = engine.getDrawable();
        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, w, h, true); 
        try {
			ImageIO.write(im, "png", new File("E:/Test.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		MUtil.clearImage(bi);
		Graphics2D g = (Graphics2D)bi.getGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(im, 0, 0, null);
    }

	public BufferedImage renderTriangle() {
		return GLUIDraw.drawColorGradient( 0.5f, GradientType.SATURATION, width, height);
	}
	public BufferedImage _old2() {
		engine.setSurfaceSize(width, height);
		
		GL4 gl = engine.getGL4();

		FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(
        	new float[] {
                    -1.0f, -1.0f, 0.0f, 1.0f,
                    +1.0f, -1.0f, 0.0f, 1.0f,
                    -1.0f, +1.0f, 0.0f, 1.0f,
                    +1.0f, +1.0f, 0.0f, 1.0f,
                    
                    +1.0f, +0.000f, 0.0f, 1.0f,
                    +0.0f, +1.000f, 0.0f, 1.0f,
                    +0.0f, +0.000f, 1.0f, 1.0f,
                    +1.0f, +1.000f, 1.0f, 1.0f
        	}
        );

	    IntBuffer positionBufferObject = GLBuffers.newDirectIntBuffer(1);
	    IntBuffer vao = GLBuffers.newDirectIntBuffer(1);
	    
	    gl.glGenBuffers(1, positionBufferObject);
	    gl.glBindBuffer( GL4.GL_ARRAY_BUFFER, positionBufferObject.get(0));
	    gl.glBufferData(
	    		GL4.GL_ARRAY_BUFFER, 
	    		vertexBuffer.capacity()*Float.BYTES, 
	    		vertexBuffer, 
	    		GL4.GL_STATIC_DRAW);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
	    
		gl.glGenVertexArrays(1, vao);
		gl.glBindVertexArray(vao.get(0));


	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0f, 0f, 0f, 0f});
        gl.glClearBufferfv(GL4.GL_COLOR, 0, clearColor);

        gl.glUseProgram( engine.getProgram(ProgramType.DEFAULT));

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, positionBufferObject.get(0));
        gl.glEnableVertexAttribArray(GLEngine.Attr.POSITION);
        gl.glEnableVertexAttribArray(GLEngine.Attr.COLOR);
        gl.glVertexAttribPointer(GLEngine.Attr.POSITION, Vec4.NUM, GL4.GL_FLOAT, false, Vec4.STRIDE, 0);
        gl.glVertexAttribPointer(GLEngine.Attr.COLOR, Vec4.NUM, GL4.GL_FLOAT, false, Vec4.STRIDE, Vec4.STRIDE*4);

        gl.glDrawArrays(GL4.GL_TRIANGLE_STRIP, 0, 4);

        gl.glDisableVertexAttribArray(GLEngine.Attr.POSITION);
        gl.glDisableVertexAttribArray(GLEngine.Attr.COLOR);
        gl.glUseProgram(0);
        
		
		gl.glDeleteVertexArrays(1, vao);
		gl.glDeleteBuffers(1, positionBufferObject);

		GLAutoDrawable drawable = engine.getDrawable();
        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, width, height, true); 
        
        return im;
	}
	
	public BufferedImage _old() {
		engine.setSurfaceSize(width, height);
		
		GLAutoDrawable drawable = engine.getDrawable();
		drawable.display();
		drawable.getContext().makeCurrent();

        List<Float> data = new ArrayList<>(numPoints * 2); 

        // simulate some data here 
        for (int i = 0; i < numPoints; i++) { 
            float x = r.nextInt(width); 
            float y = r.nextInt(height); 
            data.add(x); 
            data.add(y); 
        } 

        // x and y for each point, 4 bytes for each 
        FloatBuffer buffer = ByteBuffer.allocateDirect(numPoints * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(); 
        for (Float d : data) { 
            buffer.put(d); 
        } 
        buffer.rewind(); 


        GL2 gl = drawable.getGL().getGL2(); 
        
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); 
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glViewport(0, 0, width, height); 

        // use pixel coordinates 
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION); 
        gl.glLoadIdentity(); 

        gl.glOrtho(0d, width, height, 0d, -1d, 1d); 
        gl.glPointSize(4f); 
        gl.glColor3f(0.76f, 0.2f, 0.4f); 

        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY); 
        gl.glVertexPointer(2, GL2.GL_FLOAT, 0, buffer); 
        gl.glDrawArrays(GL2.GL_POINTS, 0, numPoints); 
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY); 
        
        

        BufferedImage im = new AWTGLReadBufferUtil(drawable.getGLProfile(), true)
        		.readPixelsToBufferedImage(
        				gl, 0, 0, width, height, true); 

		return im;
	}
}
