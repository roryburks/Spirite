package spirite.graphics.gl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
import spirite.brains.MasterControl;
import spirite.graphics.gl.GLEngine.ProgramType;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.panel_work.WorkArea;
import spirite.panel_work.WorkPanel;
import spirite.panel_work.WorkPanel.View;


/**
 * GLWorkArea is a WorkArea that uses GLJPanel for rendering directly from OpenGl
 * 
 * @author Rory Burks
 */
public class GLWorkArea implements WorkArea, GLEventListener, MImageObserver
{
	private final WorkPanel context;
	private final GLEngine engine = GLEngine.getInstance();
	private final GLJPanel canvas;
	
	private final GLGraphics graphics;
	private ImageWorkspace workspace = null;
	private View view;
	
	public GLWorkArea(WorkPanel context, MasterControl master) {
		this.context = context;
		this.graphics = (GLGraphics)master.getGraphicsContext();
		
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
	@Override public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
	}
	@Override
	public void display(GLAutoDrawable glad) {
		glad.getContext().makeCurrent();

		GLParameters params;
		int w = glad.getSurfaceWidth();
		int h = glad.getSurfaceHeight();
		GL2 gl = glad.getGL().getGL2();
		
		graphics.setContext(gl, w, h, true);
		gl.glViewport(0, 0, w, h);
		
		// Clear Background Color
		Color c = context.getBackground();
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( 
	    		new float[] {c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, 0f});
        gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
        
        if( workspace != null) {
        	// Draw Background Grid

    		Rectangle rect = new Rectangle( view.itsX(0), view.itsY(0), 
    				(int)Math.round(workspace.getWidth()*view.getZoom()),
	        		(int)Math.round(workspace.getHeight()*view.getZoom()));
    		graphics.drawTransparencyBG(rect, 8);
        	
    		// :::: Draw Back Reference
    		
        	BuiltImageData bd = workspace.buildActiveData();
        	if( bd != null) {
        		// ::: Draw Image
            	AffineTransform viewTrans = view.getViewTransform();
        		params = new GLParameters(w, h);
        		params.flip = true;
        		params.clearParams();
        		params.setUseBlendMode(true);
        		BufferedImage bi = bd.handle.deepAccess();
        		params.texture = new GLParameters.GLImageTexture(bi);
        		engine.applyPassProgram(ProgramType.PASS_BASIC, params, viewTrans, 
        				0, 0, bi.getWidth(), bi.getHeight(), false, gl);
        	}
        	
        	// :::: Draw Front Reference
        	
        	// :::: Draw Selection Bounds
        }
	}


	@Override
	public void init(GLAutoDrawable glad) {
		GLContext cont = engine.getContext();

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
	public void changeWorkspace(ImageWorkspace ws, View view) {
		// TODO
		if( workspace != null) {
			workspace.removeImageObserver(this);
		}
		workspace = ws;
		if( workspace != null) {
			workspace.addImageObserver(this);
		}
		this.view = view;
	}

	@Override
	public Component getComponent() {
		return canvas;
	}

	// :::: MImageObserver
	@Override public void imageChanged(ImageChangeEvent evt) { canvas.repaint(); }
	@Override public void structureChanged(StructureChangeEvent evt) { canvas.repaint(); }

	

}
