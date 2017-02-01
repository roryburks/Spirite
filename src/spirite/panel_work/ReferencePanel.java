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
import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.ReferenceRender;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.panel_work.WorkPanel.Zoomer;

public class ReferencePanel extends JPanel 
	implements MImageObserver, MReferenceObserver

{
	private final MasterControl master;
	private final Zoomer zoomer;
	private final RenderEngine renderer;
	private final boolean front;
	
	private ImageWorkspace workspace;
	
	private static final long serialVersionUID = 1L;
	public ReferencePanel(WorkPanel context, MasterControl master, boolean front) {
		this.zoomer = context.refzoomer;
		this.master = master;
		this.renderer = master.getRenderEngine();
		this.front = front;
		
		workspace = context.workspace;
		if( workspace == null) {
			MDebug.handleError( ErrorType.FATAL, this, "Reference Panel with no WS");
		}
		workspace.addImageObserver(this);
		workspace.getReferenceManager().addReferenceObserve(this);
				
		
		this.setOpaque(false);
        
    }
    @Override
    public void paintComponent( Graphics g) {
        super.paintComponent(g);
        
        if( workspace != null) {
        	RenderSettings settings = new RenderSettings();
        	settings.refRender = (front)?ReferenceRender.FRONT : ReferenceRender.BACK;
        	settings.workspace = workspace;
        	
        	BufferedImage buffer = master.getRenderEngine().renderImage(settings);
            
            Graphics2D g2 = (Graphics2D)g;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, workspace.getReferenceManager().getRefAlpha()));
            g.drawImage( buffer, 
            		zoomer.itsX(0), zoomer.itsY(0),
            		zoomer.itsX(buffer.getWidth()), zoomer.itsY(buffer.getHeight()),
            		0, 0, buffer.getWidth(), buffer.getHeight(), null);
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
