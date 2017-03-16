package spirite.image_data.draw;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.SwingUtilities;

import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import spirite.gl.GLEngine;

public class JOGLDrawer {
	private final GLEngine engine = GLEngine.getInstance();
	
//	GLAutoDrawable drawable;
	public JOGLDrawer() {
/*		GLProfile profile = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(profile);
        caps.setHardwareAccelerated(true); 
        caps.setDoubleBuffered(false); 
        caps.setAlphaBits(8); 
        caps.setRedBits(8); 
        caps.setBlueBits(8); 
        caps.setGreenBits(8); 
        caps.setOnscreen(false); 
		GLDrawableFactory df = GLDrawableFactory.getFactory(profile);
		drawable = df.createOffscreenAutoDrawable(
				df.getDefaultDevice(), 
				caps,
				new DefaultGLCapabilitiesChooser(), 
				width, height);*/
		
	}

    static int width = 300; 
    static int height = 480; 
    static int numPoints = 1000; 
    static Random r = new Random(); 

	public BufferedImage renderTriangle() {
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
