package spirite.pc.ui.panel_work.gl;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.GLBuffers;

import jpen.owner.multiAwt.AwtPenToolkit;
import spirite.base.brains.MasterControl;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.gl.GLGraphics;
import spirite.pc.jogl.JOGLCore;
import spirite.pc.ui.panel_work.WorkArea;
import spirite.pc.ui.panel_work.WorkPanel;


/**
 * GLWorkArea is a WorkArea that uses GLSPanel for rendering directly from OpenGl
 * 
 * @author Rory Burks
 */
public class GLWorkArea extends WorkArea
	implements GLEventListener
{
	private final GLEngine engine = GLEngine.getInstance();
	private final GLJPanel canvas;
	
	public GLWorkArea(WorkPanel context, MasterControl master) {
		super( context);
//		this.context = context;
		
		// Create UI Component
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
		canvas = new GLJPanel(glcapabilities);
		
		// Add Input Listeners
		AwtPenToolkit.addPenListener(canvas, context.getJPenner());
		canvas.addGLEventListener(this);
		canvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent evt) {
				if( view != null) {
					if( evt.getWheelRotation() < 0)
						view.zoomIn();
					else if( evt.getWheelRotation() > 0)
						view.zoomOut();
				}
			}
		});
	}
	
	// :::: GLEventListener
	@Override public void dispose(GLAutoDrawable arg0) {}
	@Override public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {}
	@Override
	public void display(GLAutoDrawable glad) {
		
		glad.getContext().makeCurrent();

		int w = glad.getSurfaceWidth();
		int h = glad.getSurfaceHeight();
		GL2 gl = glad.getGL().getGL2();
		GLGraphics glgc = new GLGraphics( w, h, true);
		
		engine.setGL(gl);
		engine.setTarget(0);
		
		
		gl.glViewport(0, 0, w, h);
		Color c = (referenceManager == null || !referenceManager.isEditingReference())?normalBG:referenceBG;
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( 
	    		new float[] {c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, 0f});
        gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
        
		drawWork(glgc);
        
        engine.setTarget(0);

		engine.setGL(null);
	}


	@Override
	public void init(GLAutoDrawable glad) {
		GLContext cont = JOGLCore.getContext();

		// Disassociate default context and assosciate the context from teh GLEngine
		//	(so they can share resources)
		GLContext old = glad.getContext();
		old.makeCurrent();
		glad.setContext(null, true);		
		
		GLContext cont2 = glad.createContext(cont);
		cont2.makeCurrent();
		glad.setContext(cont2, true);
	}


	// :::: WorkArea
	@Override
	public Component getComponent() {
		return canvas;
	}
}
