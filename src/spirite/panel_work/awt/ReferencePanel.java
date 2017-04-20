// Rory Burks

package spirite.panel_work.awt;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import spirite.brains.MasterControl;
import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.ReferenceRenderSource;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.graphics.GraphicsContext;
import spirite.graphics.awt.AWTContext;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.image_data.ReferenceManager.Reference;
import spirite.panel_work.WorkPanel;
import spirite.panel_work.WorkPanel.View;

public class ReferencePanel extends JPanel 
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
        	
        	BufferedImage buffer = renderer.renderImage(settings);
            
            Graphics2D g2 = (Graphics2D)g;
            GraphicsContext gc = new AWTContext(g2);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, workspace.getReferenceManager().getRefAlpha()));
            g2.translate(zoomer.itsX(0), zoomer.itsY(0));
            g2.scale(zoomer.getZoom(), zoomer.getZoom());
            
            g.drawImage( buffer, 0, 0, buffer.getWidth(), buffer.getHeight(), null);
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
