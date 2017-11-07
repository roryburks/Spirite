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
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.gl.GLGraphics;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.ReferenceManager;
import spirite.base.image_data.ReferenceManager.MReferenceObserver;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.image_data.selection.SelectionEngine.MSelectionEngineObserver;
import spirite.base.image_data.selection.SelectionEngine.SelectionEvent;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.Globals;
import spirite.pc.jogl.JOGLCore;
import spirite.pc.pen.JPenPenner;
import spirite.pc.ui.panel_work.WorkArea;
import spirite.pc.ui.panel_work.WorkPanel;
import spirite.pc.ui.panel_work.WorkPanel.View;


/**
 * GLWorkArea is a WorkArea that uses GLJPanel for rendering directly from OpenGl
 * 
 * @author Rory Burks
 */
public class GLWorkArea 
	implements WorkArea, GLEventListener, MImageObserver, MSelectionEngineObserver, MReferenceObserver, MFlashObserver
{
	private static final Color normalBG = Globals.getColor("workArea.normalBG");
	private static final Color referenceBG = Globals.getColor("workArea.referenceBG");
	
//	private final WorkPanel context;
	private final GLEngine engine = GLEngine.getInstance();
	private final GLJPanel canvas;

	private ImageWorkspace workspace = null;
	private SelectionEngine selectionEngine;
	private ReferenceManager referenceManager;
	private View view;
	
	private final JPenPenner penner;
	
	public GLWorkArea(WorkPanel context, MasterControl master) {
//		this.context = context;
		
		// Create UI Component
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities glcapabilities = new GLCapabilities( glprofile );
		canvas = new GLJPanel(glcapabilities);
		
		this.penner = context.getJPenner();
		
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
		
		// Clear Background Color
		Color c = (referenceManager == null || !referenceManager.isEditingReference())?normalBG:referenceBG;
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( 
	    		new float[] {c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, 0f});
        gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
        
        if( workspace != null) {
        	// :::: Draw Background Grid
    		Rect rect = new Rect( view.itsX(0), view.itsY(0), 
    				(int)Math.round(workspace.getWidth()*view.getZoom()),
	        		(int)Math.round(workspace.getHeight()*view.getZoom()));
    		glgc.drawTransparencyBG(rect, 8);

        	MatTrans viewTrans = view.getViewTransform();

        	// :::: Draw Image with References
        	RenderEngine renderEngine = workspace.getRenderEngine();
        	
        	glgc.setTransform(viewTrans);
        	glgc.setComposite(Composite.SRC_OVER, workspace.getReferenceManager().getRefAlpha());
        	renderEngine.renderReference(workspace, glgc, false);
        	
        	glgc.setComposite(Composite.SRC_OVER, 1);
        	renderEngine.renderWorkspace(workspace, glgc, viewTrans);

        	glgc.setTransform(viewTrans);
        	glgc.setComposite(Composite.SRC_OVER, workspace.getReferenceManager().getRefAlpha());
        	renderEngine.renderReference(workspace, glgc, true);
        	glgc.setComposite(Composite.SRC_OVER, 1);

            // :::: Draw Border around the active Data
        	
        	BuildingMediumData active = workspace.buildActiveData();
        	
        	if( active != null) {
        		active.doOnBuiltData((built) -> {

                	glgc.setComposite(glgc.getComposite(), 0.3f);
                    glgc.setColor(Globals.getColor("drawpanel.layer.border").getRGB());
                    
                    built.drawBorder(glgc);
        		});
        	}
            
        	// :::: Draw Selection Bounds
        	SelectionMask selection = selectionEngine.getSelection();

            if( selection != null) {
            	glgc.setTransform(viewTrans);
            	selection.drawBounds(glgc, true);
            }
            
        	glgc.setTransform(null);
            if( penner.drawsOverlay())
            	penner.paintOverlay(glgc);
        }
        
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
	public void changeWorkspace(ImageWorkspace ws, View view) {
		if( workspace != null) {
			workspace.removeImageObserver(this);
			workspace.removeFlashObserve(this);
			selectionEngine.removeSelectionObserver(this);
			referenceManager.removeReferenceObserve(this);
		}
		workspace = ws;
		selectionEngine = (ws == null)?null:ws.getSelectionEngine();
		referenceManager = (ws == null)?null:ws.getReferenceManager();
		if( workspace != null) {
			workspace.addImageObserver(this);
			workspace.addFlashObserve(this);
			selectionEngine.addSelectionObserver(this);
			referenceManager.addReferenceObserve(this);
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
	
	// :::: MSelectionObserver
	@Override public void selectionBuilt(SelectionEvent evt) { canvas.repaint(); }
	@Override public void buildingSelection(SelectionEvent evt) { canvas.repaint(); }

	// :::: MReferenceObserver
	@Override public void referenceStructureChanged(boolean hard) { canvas.repaint(); }
	@Override public void toggleReference(boolean referenceMode) { canvas.repaint(); }

	// :::: MFlashObserver
	@Override public void flash() {canvas.repaint(); }
}
