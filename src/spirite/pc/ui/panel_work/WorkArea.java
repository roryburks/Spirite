package spirite.pc.ui.panel_work;

import java.awt.Color;
import java.awt.Component;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.GLBuffers;

import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.gl.GLEngine;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ReferenceManager;
import spirite.base.image_data.ReferenceManager.MReferenceObserver;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.image_data.selection.SelectionEngine.MSelectionEngineObserver;
import spirite.base.image_data.selection.SelectionEngine.SelectionEvent;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.util.glmath.MatTrans;
import spirite.base.util.glmath.Rect;
import spirite.hybrid.Globals;
import spirite.pc.pen.JPenPenner;
import spirite.pc.ui.panel_work.WorkPanel.View;

/** A WorkArea is a simple abstraction for encapsulating the interactions of 
 * a UIComponent which handles the Drawn Image area
 * 
 * @author Rory Burks
 *
 */
public abstract class WorkArea implements MImageObserver, MFlashObserver, MSelectionEngineObserver, MReferenceObserver {

	protected static final Color normalBG = Globals.getColor("workArea.normalBG");
	protected static final Color referenceBG = Globals.getColor("workArea.referenceBG");
	
//	private final WorkPanel context;

	protected ImageWorkspace workspace = null;
	protected SelectionEngine selectionEngine;
	protected ReferenceManager referenceManager;
	protected View view;
	
	private final JPenPenner penner;
	
	
	public WorkArea(WorkPanel context) {
		this.penner = context.getJPenner();
	}
	public void changeWorkspace( ImageWorkspace ws, View view) {
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
	public abstract Component getComponent();
	
	public void drawWork( GraphicsContext gc) {
		// Clear Background Color
        
        if( workspace != null) {
        	// :::: Draw Background Grid
    		Rect rect = new Rect( view.itsX(0), view.itsY(0), 
    				(int)Math.round(workspace.getWidth()*view.getZoom()),
	        		(int)Math.round(workspace.getHeight()*view.getZoom()));
    		gc.drawTransparencyBG(rect, 8);

        	MatTrans viewTrans = view.getViewTransform();

        	// :::: Draw Image with References
        	RenderEngine renderEngine = workspace.getRenderEngine();
        	
        	gc.setTransform(viewTrans);
        	gc.setComposite(Composite.SRC_OVER, workspace.getReferenceManager().getRefAlpha());
        	renderEngine.renderReference(workspace, gc, false);
        	
        	gc.setComposite(Composite.SRC_OVER, 1);
        	renderEngine.renderWorkspace(workspace, gc, viewTrans);

        	gc.setTransform(viewTrans);
        	gc.setComposite(Composite.SRC_OVER, workspace.getReferenceManager().getRefAlpha());
        	renderEngine.renderReference(workspace, gc, true);
        	gc.setComposite(Composite.SRC_OVER, 1);
        	
            // :::: Draw Border around the active Data
        	
        	BuildingMediumData active = workspace.buildActiveData();
        	
        	if( active != null) {
        		active.doOnBuiltData((built) -> {

                	gc.setComposite(gc.getComposite(), 0.3f);
                    gc.setColor(Globals.getColor("drawpanel.layer.border").getRGB());
                    
                    built.drawBorder(gc);
        		});
        	}
            
        	// :::: Draw Selection Bounds
        	SelectionMask selection = selectionEngine.getSelection();

            if( selection != null) {
            	MatTrans selTrans = viewTrans;
            	selTrans.concatenate(selectionEngine.getLiftedDrawTrans());
            	gc.setTransform(selTrans);
            	selection.drawBounds(gc);
            }
            
        	gc.setTransform(null);
            if( penner.drawsOverlay())
            	penner.paintOverlay(gc);
        }
	}
	


	// :::: MImageObserver
	@Override public void imageChanged(ImageChangeEvent evt) { getComponent().repaint(); }
	@Override public void structureChanged(StructureChangeEvent evt) { getComponent().repaint(); }
	
	// :::: MSelectionObserver
	@Override public void selectionBuilt(SelectionEvent evt) { getComponent().repaint(); }
	@Override public void buildingSelection(SelectionEvent evt) { getComponent().repaint(); }

	// :::: MReferenceObserver
	@Override public void referenceStructureChanged(boolean hard) { getComponent().repaint(); }
	@Override public void toggleReference(boolean referenceMode) { getComponent().repaint(); }

	// :::: MFlashObserver
	@Override public void flash() {getComponent().repaint(); }
}
