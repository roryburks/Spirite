package spirite.panel_work.gl;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
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
import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.brains.RenderEngine;
import spirite.graphics.GraphicsContext.Composite;
import spirite.graphics.gl.GLGraphics;
import spirite.graphics.gl.GLWorkspaceRenderer;
import spirite.graphics.gl.engine.GLEngine;
import spirite.graphics.gl.engine.GLParameters;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ReferenceManager;
import spirite.image_data.ImageWorkspace.BuiltImageData;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.MSelectionEngineObserver;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionEvent;
import spirite.image_data.layers.Layer;
import spirite.panel_work.WorkArea;
import spirite.panel_work.WorkPanel;
import spirite.panel_work.WorkPanel.View;
import spirite.pen.JPenPenner;


/**
 * GLWorkArea is a WorkArea that uses GLJPanel for rendering directly from OpenGl
 * 
 * @author Rory Burks
 */
public class GLWorkArea 
	implements WorkArea, GLEventListener, MImageObserver, MSelectionEngineObserver, MReferenceObserver
{
	private static final Color normalBG = Globals.getColor("workArea.normalBG");
	private static final Color referenceBG = Globals.getColor("workArea.referenceBG");
	
	private final WorkPanel context;
	private final GLEngine engine = GLEngine.getInstance();
	private final GLJPanel canvas;

	private ImageWorkspace workspace = null;
	private SelectionEngine selectionEngine;
	private ReferenceManager referenceManager;
	private View view;
	
	private final JPenPenner penner;

	GLWorkspaceRenderer glwr;
	
	public GLWorkArea(WorkPanel context, MasterControl master) {
		this.context = context;

    	glwr = new GLWorkspaceRenderer(master);
		
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
		GLGraphics glgc = new GLGraphics(glad, true);
		
		glad.getContext().makeCurrent();

		GLParameters params;
		int w = glad.getSurfaceWidth();
		int h = glad.getSurfaceHeight();
		GL2 gl = glad.getGL().getGL2();
		
		gl.glViewport(0, 0, w, h);
		
		// Clear Background Color
		Color c = (referenceManager == null || !referenceManager.isEditingReference())?normalBG:referenceBG;
	    FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer( 
	    		new float[] {c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f, 0f});
        gl.glClearBufferfv(GL2.GL_COLOR, 0, clearColor);
        
        if( workspace != null) {
        	// :::: Draw Background Grid
    		Rectangle rect = new Rectangle( view.itsX(0), view.itsY(0), 
    				(int)Math.round(workspace.getWidth()*view.getZoom()),
	        		(int)Math.round(workspace.getHeight()*view.getZoom()));
    		glgc.drawTransparencyBG(rect, 8);
    		

        	AffineTransform viewTrans = view.getViewTransform();

        	// :::: Draw Image with References
        	RenderEngine renderEngine = workspace.getRenderEngine();
        	
        	glgc.setTransform(viewTrans);
        	glgc.setComposite(Composite.SRC_OVER, workspace.getReferenceManager().getRefAlpha());
//        	renderEngine.renderReference(workspace, glgc, false);
        	
        	glgc.setComposite(Composite.SRC_OVER, 1);
        	renderEngine.renderWorkspace(workspace, glgc, viewTrans);

        	glgc.setTransform(viewTrans);
        	glgc.setComposite(Composite.SRC_OVER, workspace.getReferenceManager().getRefAlpha());
        	renderEngine.renderReference(workspace, glgc, true);
        	glgc.setComposite(Composite.SRC_OVER, 1);

            // :::: Draw Border around the active Data
            BuiltImageData active = workspace.buildActiveData();
            
            if( active!= null) {
                glgc.setColor(Globals.getColor("drawpanel.layer.border"));
                
                Rectangle r = active.getBounds();
                glgc.drawRect( r.x, r.y, r.width, r.height);
            }
            
        	// :::: Draw Selection Bounds
            Selection selection = selectionEngine.getSelection();

            if( selection != null || selectionEngine.isBuilding()) {

                glgc.setColor( Color.BLACK);
            	glgc.setTransform(viewTrans);
                if(selectionEngine.isBuilding()) 
                	selectionEngine.drawBuildingSelection(glgc);
                if( selection != null) {
                	AffineTransform trans = new AffineTransform(viewTrans);
                	trans.translate( selectionEngine.getOffsetX(), selectionEngine.getOffsetY());
                	glgc.setTransform(trans);
                	selection.drawSelectionBounds(glgc);
                }
            }
            
        	glgc.setTransform(null);
            if( penner.drawsOverlay())
            	penner.paintOverlay(glgc);
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
		if( workspace != null) {
			workspace.removeImageObserver(this);
			selectionEngine.removeSelectionObserver(this);
			referenceManager.removeReferenceObserve(this);
		}
		workspace = ws;
		selectionEngine = (ws == null)?null:ws.getSelectionEngine();
		referenceManager = (ws == null)?null:ws.getReferenceManager();
		if( workspace != null) {
			workspace.addImageObserver(this);
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
}
