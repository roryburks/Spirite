package spirite.pc.ui.panel_work;

import spirite.base.graphics.CapMethod;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.JoinMethod;
import spirite.base.graphics.LineAttributes;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.*;
import spirite.base.image_data.ReferenceManager;
import spirite.base.image_data.ReferenceManager.MReferenceObserver;
import spirite.base.image_data.layers.puppet.BasePuppet.BaseBone;
import spirite.base.image_data.layers.puppet.BasePuppet.BasePart;
import spirite.base.image_data.layers.puppet.PuppetLayer;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.image_data.selection.SelectionEngine.MSelectionEngineObserver;
import spirite.base.image_data.selection.SelectionEngine.SelectionEvent;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.util.Colors;
import spirite.base.util.linear.MatTrans;
import spirite.base.util.linear.Rect;
import spirite.base.util.linear.Vec2;
import spirite.hybrid.Globals;
import spirite.pc.pen.JPenPenner;
import spirite.pc.ui.panel_work.WorkPanel.View;

import java.awt.*;

/** A WorkArea is a simple abstraction for encapsulating the interactions of 
 * a UIComponent which handles the Drawn Image area
 * 
 * @author Rory Burks
 *
 */
public abstract class WorkArea implements MImageObserver, MFlashObserver, MSelectionEngineObserver, MReferenceObserver {

	protected static final Color normalBG = Globals.getColor("bg");
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
            
            // :::: Draw skeleton
            Node node = workspace.getSelectedNode();
            if( node instanceof LayerNode && ((LayerNode) node).getLayer() instanceof PuppetLayer) {
            	PuppetLayer puppet = (PuppetLayer) ((LayerNode) node).getLayer();
            	if( puppet.isSkeletonVisible()) {
    				gc.setTransform(viewTrans);
    				gc.setLineAttributes(new LineAttributes(4, CapMethod.ROUND, JoinMethod.MITER, null));
    				gc.setColor(Colors.WHITE);
    				gc.setComposite(Composite.SRC_OVER, 1);
    				
            		for( BasePart part : puppet.getBase().getParts()) {
            			BaseBone bone = part.getBone();
            			if( bone != null) {
            				Vec2 _n = new Vec2(bone.x2-bone.x1, bone.y2-bone.y1).normalize();
            				Vec2 n = new Vec2(bone.y1-bone.y2, bone.x2-bone.x1).normalize();
            				Vec2 mid = new Vec2((bone.x1 + bone.x2)/2f, (bone.y1 + bone.y2)/2f);
            				float[] x = new float[] 
            					{bone.x1-n.x, bone.x1, bone.x1+n.x, mid.x+n.x*3, bone.x2+n.x, bone.x2, bone.x2-n.x, mid.x-n.x*3};
            				float[] y = new float[] 
            					{bone.y1, bone.y1 - n.x, bone.y1,   mid.y+n.y*3, bone.y2, bone.y2+n.x, bone.y2,     mid.y-n.y*3};
            				
            				gc.fillPolygon(x, y, 8);
            				//gc.drawLine(bone.x1, bone.y1, bone.x2, bone.y2);
            			}
            		}
            	}
            }
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
