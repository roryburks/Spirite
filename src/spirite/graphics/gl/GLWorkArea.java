package spirite.graphics.gl;

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
import spirite.brains.MasterControl;
import spirite.brains.RenderEngine;
import spirite.graphics.gl.engine.GLEngine;
import spirite.graphics.gl.engine.GLParameters;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.MSelectionEngineObserver;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionEvent;
import spirite.panel_work.JPenPenner;
import spirite.panel_work.WorkArea;
import spirite.panel_work.WorkPanel;
import spirite.panel_work.WorkPanel.View;


/**
 * GLWorkArea is a WorkArea that uses GLJPanel for rendering directly from OpenGl
 * 
 * @author Rory Burks
 */
public class GLWorkArea implements WorkArea, GLEventListener, MImageObserver, MSelectionEngineObserver
{
	private final WorkPanel context;
	private final GLEngine engine = GLEngine.getInstance();
	private final GLJPanel canvas;

	private ImageWorkspace workspace = null;
	private SelectionEngine selectionEngine;
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
	@Override public void dispose(GLAutoDrawable arg0) {
		System.out.println("DISPOSE");
	}
	@Override public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
	}
	
	@Override
	public void display(GLAutoDrawable glad) {
		System.out.println("START_DISP");
		GLGraphics glgc = new GLGraphics(glad, true);
		
		glad.getContext().makeCurrent();

		GLParameters params;
		int w = glad.getSurfaceWidth();
		int h = glad.getSurfaceHeight();
		GL2 gl = glad.getGL().getGL2();
		
		gl.glViewport(0, 0, w, h);
		
		// Clear Background Color
		Color c = context.getBackground();
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
        	
    		// :::: Draw Back Reference
        	
        	glad.getContext().makeCurrent();
        	RenderEngine renderEngine = workspace.getRenderEngine();
        	renderEngine.renderWorkspace(workspace, glgc, viewTrans);

//        	glgc.setTransform(viewTrans);
//        	glwr.renderWorkspace(workspace, glgc);
        	
        	
//        	glad.getContext().makeCurrent();
/*        	RenderSettings settings = new RenderSettings(
        			renderEngine.getDefaultRenderTarget(workspace));
    		BufferedImage image = renderEngine.renderImage(settings);

    		glad.getContext().makeCurrent();
    		
			// ::: Draw Image
    		params = new GLParameters(w, h);
    		params.flip = true;
    		params.clearParams();
    		params.setUseBlendMode(true);
    		params.texture = new GLParameters.GLImageTexture(image);
    		engine.applyPassProgram(ProgramType.PASS_BASIC, params, viewTrans, 
    				0, 0, image.getWidth(), image.getHeight(), false, gl);*/
        	
        	// :::: Draw Front Reference
        	
        	// :::: Draw Selection Bounds
            Selection selection = selectionEngine.getSelection();

            if( selection != null || selectionEngine.isBuilding()) {

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
    		System.out.println("END_DISP");
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
			selectionEngine.removeSelectionObserver(this);
		}
		workspace = ws;
		selectionEngine = (ws == null)?null:ws.getSelectionEngine();
		if( workspace != null) {
			workspace.addImageObserver(this);
			selectionEngine.addSelectionObserver(this);
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
}
