// Rory Burks

package spirite.panel_work;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.brains.MasterControl;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.MReferenceObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.RenderEngine;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.panel_work.WorkPanel.Zoomer;

public class ReferencePanel extends JPanel 
	implements MImageObserver, MReferenceObserver

{
	private final MasterControl master;
	private final Zoomer zoomer;
	private final RenderEngine renderer;
	
	private ImageWorkspace workspace;
	
	private static final long serialVersionUID = 1L;
	public ReferencePanel(WorkPanel context, MasterControl master) {
		this.zoomer = context.refzoomer;
		this.master = master;
		this.renderer = master.getRenderEngine();
		
		workspace = context.workspace;
		if( workspace == null) {
			MDebug.handleError( ErrorType.FATAL, this, "Reference Panel with no WS");
		}
		workspace.addImageObserver(this);
		workspace.addReferenceObserve(this);
				
		
		this.setOpaque(false);
        
    }
    @Override
    public void paintComponent( Graphics g) {
        super.paintComponent(g);
        
        if( workspace != null) {
            RenderSettings settings = new RenderSettings();
            settings.workspace = workspace;
            settings.node = workspace.getReferenceRoot();
            
            BufferedImage image = renderer.renderImage(settings);
            Graphics2D g2 = (Graphics2D)g;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, workspace.getRefAlpha()));
            g.drawImage( image, 
            		zoomer.itsX(0), zoomer.itsY(0),
            		zoomer.itsX(image.getWidth()), zoomer.itsY(image.getHeight()),
            		0, 0, image.getWidth(), image.getHeight(), null);
        }
        
    }
	
	// :::: MImageObserver
	@Override public void structureChanged(StructureChange evt) {}
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
}
