// Rory Burks

package spirite.pc.ui.panel_work.awt;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.GraphicsContext.Composite;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.graphics.renderer.sources.ReferenceRenderSource;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.ReferenceManager.MReferenceObserver;
import spirite.gui.hybrid.SPanel;
import spirite.pc.graphics.awt.AWTContext;
import spirite.pc.ui.panel_work.WorkPanel;
import spirite.pc.ui.panel_work.WorkPanel.View;

import java.awt.*;

public class ReferencePanel extends SPanel 
	implements MImageObserver, MReferenceObserver

{
//	private final MasterControl master;
	private final RenderEngine renderer;
	private final boolean front;
	
	private View zoomer;
	
	private ImageWorkspace workspace;
	
	
	private static final long serialVersionUID = 1L;
	public ReferencePanel(WorkPanel context, MasterControl master, boolean front) {
		this.renderer = master.getRenderEngine();
		this.front = front;
		
		this.setOpaque(false);
        
    }
    @Override
    public void paintComponent( Graphics g) {
        super.paintComponent(g);
        
        if( workspace != null) {
        	RenderSettings settings = new RenderSettings(
        			new ReferenceRenderSource(workspace,front));
        	
        	RawImage buffer = renderer.renderImage(settings);
            
            GraphicsContext gc = new AWTContext(g, getWidth(), getHeight());
            gc.setComposite( Composite.SRC_OVER, workspace.getReferenceManager().getRefAlpha());
            gc.translate(zoomer.itsX(0), zoomer.itsY(0));
            gc.scale(zoomer.getZoom(), zoomer.getZoom());
            
            gc.drawImage( buffer, 0, 0);
        }
        
    }
    
    
	
	// :::: MImageObserver
	@Override public void structureChanged(StructureChangeEvent evt) {}
	@Override
	public void imageChanged(ImageChangeEvent evt) {
		this.repaint();
	}
	
	// :::: ReferenceObserver
	@Override
	public void referenceStructureChanged(boolean hard) {
		repaint();
		
	}
	@Override
	public void toggleReference(boolean referenceMode) {	}
	public void changeWorkspace(ImageWorkspace ws, View view) {
		if( workspace != null) {
			workspace.removeImageObserver(this);
			workspace.getReferenceManager().removeReferenceObserve(this);
		}
		workspace = ws;
		if( workspace != null) {
			workspace.addImageObserver(this);
			workspace.getReferenceManager().addReferenceObserve(this);
		}
		
		this.zoomer = view;
	}
}
