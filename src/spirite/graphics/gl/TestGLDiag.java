package spirite.graphics.gl;

import java.nio.FloatBuffer;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.GLBuffers;

import spirite.graphics.gl.GLUIDraw.GradientType;

public class TestGLDiag extends JDialog  {

	GLContext cont = null;
	boolean init = false;
	int met = 0;
    
    
	private final JTabbedPane tabbed = new JTabbedPane();
	
	public TestGLDiag() {
		setBounds(100, 100, 450, 500);
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
		final GLCanvas glcanvas = new GLCanvas(glcapabilities);
		
		

		glcanvas.addGLEventListener( new GLEventListener() {
			@Override
			public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
			}
			
			@Override
			public void init(GLAutoDrawable glad) {
				SwingUtilities.invokeLater( new Runnable() { @Override public void run() {
					if( !init) {
						glad.setContext(glad.createContext(GLEngine.getInstance().getContext()), true);
						init = true;
						System.out.println("INIT");
						glcanvas.repaint();
					}
				}});
			}
			
			@Override
			public void dispose(GLAutoDrawable arg0) {
			}
			
			@Override
			public void display(GLAutoDrawable glad) {
				if( !init) return;
				
				int w = glad.getSurfaceWidth();
				int h = glad.getSurfaceHeight();
				
				
				GL2 gl = glad.getGL().getGL2();

			    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( new float[] {0, 0, 0, 0f});
		        gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
		        
		        gl.glViewport( 0, 0, w, h);
		        
		        switch( met) {
		        case 0:
			        GLUIDraw.drawColorGradient(0.4f, GradientType.RED, w, h, gl);
			        break;
		        case 1:
			        GLUIDraw.drawColorGradient(0.4f, GradientType.GREEN, w, h, gl);
			        break;
		        case 2:
			        GLUIDraw.drawColorGradient(0.4f, GradientType.BLUE, w, h, gl);
			        break;
		        case 3:
			        GLUIDraw.drawColorGradient(0.4f, GradientType.HUE, w, h, gl);
			        break;
		        case 4:
			        GLUIDraw.drawColorGradient(0.4f, GradientType.SATURATION, w, h, gl);
			        break;
		        case 5:
			        GLUIDraw.drawColorGradient(0.4f, GradientType.VALUE, w, h, gl);
			        break;
		        }
		        
			}
		});

		tabbed.addTab("Red", glcanvas);
		tabbed.addTab("Green", null);
		tabbed.addTab("Blue", null);
		tabbed.addTab("Hue", null);
		tabbed.addTab("Saturation", null);
		tabbed.addTab("Value", null);
		
		
		tabbed.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				met = tabbed.getSelectedIndex();
				glcanvas.repaint();
			}
		});
		
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(tabbed, 0, 100, Short.MAX_VALUE)
					.addGap(12))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup()
							.addComponent(tabbed, 0, 241, Short.MAX_VALUE)
					)
					.addGap(10))
		);
		getContentPane().setLayout(groupLayout);
	}
}
