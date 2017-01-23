//Rory Burks

package spirite.panel_work;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.RenderEngine;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.MSelectionEngineObserver;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionEvent;
import spirite.image_data.layers.Layer;
import spirite.ui.UIUtil;

/**
 * DrawPanel is the main UI component for drawing.  It captures the User's input 
 * using the JPen library (for both Mouse and Drawing Tablet) and draws the image
 * in the WorkArea
 * 
 * @author Rory Burks
 */
public class DrawPanel extends JPanel
     implements MImageObserver, ActionListener, MSelectionEngineObserver
{
	private static final long serialVersionUID = 1L;

	private final RenderEngine renderEngine;	
	private final Penner penner;
	final WorkPanel context;
	final ImageWorkspace workspace;
	private final SelectionEngine selectionEngine;
	private final Timer paint_timer;
	
	private int metronome = 0;

	public DrawPanel(WorkPanel context, MasterControl master) {
		this.renderEngine = master.getRenderEngine();
		this.context = context;
		this.workspace = context.workspace;
		this.selectionEngine = workspace.getSelectionEngine();
		this.setBackground(new Color(0, 0, 0, 0));

		penner = new Penner( this, master);
		
		workspace.addImageObserver(this);
		workspace.getSelectionEngine().addSelectionObserver(this);
		
		paint_timer = new Timer( 40, this);
		paint_timer.start();
		
		AwtPenToolkit.addPenListener(this, penner);
	}
 
	public void refreshPennerCoords() {
		penner.refreshCoordinates();
	}

    // :::: Paint
    @Override
    public void paintComponent( Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        float zoom = context.getZoom();

        // Draw Image
        if( workspace != null) {
        	
        	
        	RenderSettings settings = new RenderSettings();
        	settings.workspace = workspace;
        	


            // Draw Border around the Workspace
            Stroke old_stroke = g2.getStroke();
            Stroke new_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4,2}, 0);
            g2.setStroke(new_stroke);
            g2.setColor(Globals.getColor("drawpanel.image.border"));
            g2.drawRect( context.itsX(0)-1,
		            context.itsY(0)-1,
		            (int)Math.round(workspace.getWidth()*zoom)+1,
		            (int)Math.round(workspace.getHeight()*zoom)+1);

            // Draw Transparency BG
            int dx = (context.itsX(0) >= 0)?0:-context.itsX(0);
            int dy = (context.itsY(0) >= 0)?0:-context.itsY(0);
            UIUtil.drawTransparencyBG(g, new Rectangle( 
            		Math.max(0, context.itsX(0)),
            		Math.max(0, context.itsY(0)),
            		Math.min(context.getWidth(), (int)Math.round(workspace.getWidth()*zoom)-2-dx),
            		Math.min(context.getHeight(), (int)Math.round(workspace.getHeight()*zoom)-2-dy)),
            		8);
            
            // Render the image
        	BufferedImage image = renderEngine.renderImage(settings);

        	if( image != null) {
            g.drawImage( image, context.itsX(0), context.itsY(0),
            		context.itsX(image.getWidth()), context.itsY(image.getHeight()),
            		0, 0, image.getWidth(), image.getHeight(), null);
        	}
            
            // Draw Border around the active Layer
            GroupTree.Node selected = workspace.getSelectedNode();
            
            if( selected!= null) {
                new_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4,2}, metronome/4.0f);
                g2.setStroke(new_stroke);
                g2.setColor(Globals.getColor("drawpanel.layer.border"));
                
            	if( selected instanceof GroupTree.LayerNode) {
            		Layer layer = ((GroupTree.LayerNode) selected).getLayer();
            		
            		g2.drawRect( 
            				context.itsX(selected.getOffsetX()), 
            				context.itsY(selected.getOffsetY()), 
            				(int)(layer.getWidth()* context.getZoom()), 
            				(int)(layer.getHeight() * context.getZoom()));
            	}
            }


            g2.setStroke(old_stroke);
            
            
            // Draw Grid
            int w = getWidth();
            int h = getHeight();
            if( zoom >= 4) {
            	g2.setColor( new Color(90,90,90));
                for( int i = 0; i < workspace.getWidth(); i+=2) {
                	if( context.itsX(i) < 0) continue;
                	if( context.itsX(i) > w) break;
                    g2.drawLine(context.itsX(i), 0, context.itsX(i), this.getHeight());
                }
                for( int i = 0; i < workspace.getHeight(); i+=2) {
                	if( context.itsY(i) < 0) continue;
                	if( context.itsY(i) > h) break;
                    g2.drawLine(0, context.itsY(i), this.getWidth(), context.itsY(i));
                }
            }
        }

        // Draw Border around Selection
        Selection selection = null;
        if( selectionEngine.isBuilding())
        	selection = selectionEngine.getBuildingSelection();
        else 
        	selection = workspace.getSelectionEngine().getSelection();

        if( selection != null) {
        	AffineTransform trans = g2.getTransform();
            Stroke old_stroke = g2.getStroke();
            Stroke new_stroke = new BasicStroke(1/(( zoom >= 4)?(zoom/2):zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4/zoom,2/zoom}, 0);
            g2.translate(context.itsX(0), context.itsY(0));
            g2.scale(zoom, zoom);
            g2.translate( selectionEngine.getOffsetX(), selectionEngine.getOffsetY());
            g2.setColor(Color.black);
            g2.setStroke(new_stroke);
            selection.drawSelectionBounds(g);
            g2.setStroke(old_stroke);
            g2.setTransform(trans);
        }
        
    }


    // :::: ActionListener (for timer)
	@Override
	public void actionPerformed(ActionEvent arg0) {
    	metronome = (metronome + 1) % 16;

    //	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
	}
	
	// :::: MImageObserver
	@Override	public void imageChanged(ImageChangeEvent evt) {
    	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
    }
	@Override	public void structureChanged(StructureChange evt) {	}
 
	
	
	// :::: MSelectionEngineObserver
	@Override	public void selectionBuilt(SelectionEvent evt) {}
	@Override
	public void buildingSelection(SelectionEvent evt) {
    	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
	}
	

	/** Removes Global references (Swing-Global and JNI-Global) to avoid leaks */
	void cleanUp() {
		AwtPenToolkit.removePenListener(this, penner);
		paint_timer.stop();
		penner.cleanUp();
	}
}
