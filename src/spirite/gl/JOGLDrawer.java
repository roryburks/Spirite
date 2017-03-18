package spirite.gl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hackoeur.jglm.Vec4;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

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
