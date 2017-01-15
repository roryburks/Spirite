//Rory Burks

package spirite.panel_work;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jpen.owner.multiAwt.AwtPenToolkit;
import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.image_data.ImageData;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.image_data.SelectionEngine.MSelectionEngineObserver;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionEvent;

/**
 * DrawPanel is the 
 *
 */
public class DrawPanel extends JPanel
     implements MImageObserver, ActionListener, MSelectionEngineObserver
{
	private static final long serialVersionUID = 1L;

	// Why it needs Master: Penner needs Master, and it also needs access
	//	to the RenderEngine which caches drawn images
	MasterControl master;
	
	WorkPanel context;
	ImageWorkspace workspace;
	Penner penner;

	Timer paint_timer;
	
	int metronome = 0;

	public DrawPanel() {}
	public DrawPanel(WorkPanel context) {
		this.context = context;
		this.master = context.master;
		this.workspace = context.workspace;
		this.setBackground(new Color(0, 0, 0, 0));

		penner = new Penner( this);
		
		workspace.addImageObserver(this);
		workspace.getSelectionEngine().addSelectionObserver(this);
		
		paint_timer = new Timer( 40, this);
		paint_timer.start();

	//	this.addMouseListener(mouser);
//		this.addMouseMotionListener(mouser);
		
		AwtPenToolkit.addPenListener(this, penner);
	}
 

    // :::: Paint
    @Override
    public void paintComponent( Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if( master == null) return; // Needed so the IDE doesn't freak out

        // Draw Image
        ImageWorkspace workspace = context.workspace;
        if( workspace != null) {
        	
        	RenderSettings settings = new RenderSettings();
        	settings.workspace = workspace;
        	
        	BufferedImage image = master.getRenderEngine().renderImage(settings);

        	if( image != null) {
            g.drawImage( image, context.itsX(0), context.itsY(0),
            		context.itsX(image.getWidth()), context.itsY(image.getHeight()),
            		0, 0, image.getWidth(), image.getHeight(), null);
        	}


            // Draw Border around the complete image
            Stroke old_stroke = g2.getStroke();
            Stroke new_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4,2}, 0);
            g2.setStroke(new_stroke);
            g2.setColor(Globals.getColor("drawpanel.image.border"));
            g2.drawRect( context.itsX(0)-1,
		            context.itsY(0)-1,
		            (int)Math.round(workspace.getWidth()*context.zoom)+1,
		            (int)Math.round(workspace.getHeight()*context.zoom)+1);
            
            // Draw Border around the active Layer
            ImageData active = workspace.getActiveData();
            
            if( active != null) {
	            BufferedImage data = active.getData();
	            if( data.getWidth() != workspace.getWidth() || data.getHeight() != workspace.getHeight()) {
	                new_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4,2}, metronome/4.0f);
	                g2.setStroke(new_stroke);
	                g2.setColor(Globals.getColor("drawpanel.layer.border"));
	                g2.drawRect( context.itsX(0)-1,
	    		            context.itsY(0)-1,
	    		            (int)Math.round(data.getWidth()*context.zoom)+1,
	    		            (int)Math.round(data.getHeight()*context.zoom)+1);
	            }
            }


            g2.setStroke(old_stroke);
            
            
            // Draw Grid
            if( context.zoom >= 4) {
                for( int i = 0; i < workspace.getWidth(); ++i) {
                    g2.drawLine(context.itsX(i), 0, context.itsX(i), this.getHeight());
                }
                for( int i = 0; i < workspace.getHeight(); ++i) {
                    g2.drawLine(0, context.itsY(i), this.getWidth(), context.itsY(i));
                }
            }
        }

        // Draw Border around selection
        Selection selection = workspace.getSelectionEngine().getSelection();

        if( selection != null) {
        	AffineTransform trans = g2.getTransform();
            Stroke old_stroke = g2.getStroke();
            Stroke new_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4,2}, 0);
            g2.scale(context.zoom, context.zoom);
            g2.translate(context.itsX(0), context.itsY(0));
            g2.setStroke(new_stroke);
            selection.drawSelectionBounds(g);
            g2.setStroke(old_stroke);
            g2.setTransform(trans);
        }
    }


	@Override
	public void actionPerformed(ActionEvent arg0) {
    	metronome = (metronome + 1) % 16;

    //	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
	}
	
	@Override	public void imageChanged() {
    	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
    }
	@Override	public void structureChanged(StructureChange evt) {	}
 
	
	void cleanUp() {
		AwtPenToolkit.removePenListener(this, penner);
		paint_timer.stop();
		penner.cleanUp();
	}
	@Override
	public void selectionBuilt(SelectionEvent evt) {
		
	}
	@Override
	public void buildingSelection(SelectionEvent evt) {
    	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
		
	}
}
