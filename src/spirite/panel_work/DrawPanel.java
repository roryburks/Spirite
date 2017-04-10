//Rory Burks

package spirite.panel_work;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.brains.MasterControl;
import spirite.brains.RenderEngine;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.image_data.GroupTree;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.image_data.SelectionEngine;
import spirite.image_data.SelectionEngine.MSelectionEngineObserver;
import spirite.image_data.SelectionEngine.Selection;
import spirite.image_data.SelectionEngine.SelectionEvent;
import spirite.image_data.layers.Layer;
import spirite.panel_work.WorkPanel.Zoomer;

/**
 * DrawPanel is the main UI component for drawing.  It captures the User's input 
 * using the JPen library (for both Mouse and Drawing Tablet) and draws the image
 * in the WorkArea
 * 
 * @author Rory Burks
 */
public class DrawPanel extends JPanel
     implements MImageObserver, ActionListener, MSelectionEngineObserver, 
     	MSelectionObserver
{
	private static final long serialVersionUID = 1L;

	private final MasterControl master;
	private final RenderEngine renderEngine;
	private final SelectionEngine selectionEngine;

	// Components other things need access to
	public final ImageWorkspace workspace;
	public final WorkPanel context;
	public final Zoomer zoomer;
	
	private final JPenPenner penner;
	private final Timer paint_timer;
	
	private int metronome = 0;

	public DrawPanel(WorkPanel context, MasterControl master) {
		this.renderEngine = master.getRenderEngine();
		this.workspace = context.workspace;
		this.selectionEngine = workspace.getSelectionEngine();
		this.master = master;
		this.context = context;
		this.zoomer = context.zoomer;
		this.setBackground(new Color(0, 0, 0, 0));
		this.setOpaque( false);

		penner = new JPenPenner( this, master);
		
		workspace.addImageObserver(this);
		workspace.getSelectionEngine().addSelectionObserver(this);
		workspace.addSelectionObserver(this);
		
		paint_timer = new Timer( 40, this);
		paint_timer.start();
		
		AwtPenToolkit.addPenListener(this, penner);
	}
 
	public void refreshPennerCoords() {
		penner.refreshCoordinates();
	}
	
	public Penner getPenner() {
		return penner.penner;
	}

	
    // :::: Paint
    @Override
    public void paintComponent( Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        final float zoom = zoomer.getZoom();
        
        if( workspace == null) {
        	MDebug.handleError(ErrorType.STRUCTURAL, null, "Null Workspaced Draw Panel");
        	return;
        }
        
        final BasicStroke dashedStroke = new BasicStroke(
        		1/(( zoom >= 4)?(zoom/2):zoom), 
        		BasicStroke.CAP_BUTT, 
        		BasicStroke.JOIN_BEVEL, 
        		0, 
        		new float[]{4,2}, 
        		0);
        final Stroke baseStroke = g2.getStroke();

        // Draw Image
    	RenderSettings settings = new RenderSettings(
    			renderEngine.getDefaultRenderTarget(workspace));


        // Draw Border around the Workspace
/*        g2.setStroke(dashedStroke);
        g2.setColor(Globals.getColor("drawpanel.image.border"));
        g2.drawRect( zoomer.itsX(0)-1,
        		zoomer.itsY(0)-1,
	            (int)Math.round(workspace.getWidth()*zoom)+1,
	            (int)Math.round(workspace.getHeight()*zoom)+1);*/

        long start = System.currentTimeMillis();
        // Render the image
    	BufferedImage image = renderEngine.renderImage(settings);

    	if( image != null) {
        g.drawImage( image, zoomer.itsX(0), zoomer.itsY(0),
        		zoomer.itsX(image.getWidth()), zoomer.itsY(image.getHeight()),
        		0, 0, image.getWidth(), image.getHeight(), null);
    	}
        
        // Draw Border around the active Layer
        GroupTree.Node selected = workspace.getSelectedNode();
        
        if( selected!= null) {
            g2.setStroke(dashedStroke);
            g2.setColor(Globals.getColor("drawpanel.layer.border"));
            
        	if( selected instanceof GroupTree.LayerNode) {
        		Layer layer = ((GroupTree.LayerNode) selected).getLayer();
        		
        		g2.drawRect( 
        				zoomer.itsX(selected.getOffsetX()), 
        				zoomer.itsY(selected.getOffsetY()), 
        				(int)(layer.getWidth()* zoomer.getZoom()), 
        				(int)(layer.getHeight() * zoomer.getZoom()));
        	}
        }
        
        // Draw Border on active Data
/*        BuiltImageData bid = workspace.buildActiveData();
        
        if( bid != null) {
			AffineTransform transform = g2.getTransform();
            g2.setStroke(dashedStroke);
            g2.setColor(Globals.getColor("drawpanel.bid.border"));
            g2.translate(zoomer.itsX(0), zoomer.itsY(0));
            g2.scale(zoomer.getZoom(), zoomer.getZoom());
            bid.drawBorder(g2);
			g2.setTransform( transform);
        }*/


        g2.setStroke(baseStroke);
        
        
        // Draw Grid
/*        int w = getWidth();
        int h = getHeight();
        if( zoom >= 4) {
        	g2.setColor( new Color(90,90,90));
            for( int i = 0; i < workspace.getWidth(); i+=2) {
            	if( zoomer.itsX(i) < 0) continue;
            	if( zoomer.itsX(i) > w) break;
                g2.drawLine(zoomer.itsX(i), 0, zoomer.itsX(i), this.getHeight());
            }
            for( int i = 0; i < workspace.getHeight(); i+=2) {
            	if( zoomer.itsY(i) < 0) continue;
            	if( zoomer.itsY(i) > h) break;
                g2.drawLine(0, zoomer.itsY(i), this.getWidth(), zoomer.itsY(i));
            }
        }*/
        

        // Draw Border around Selection
        Selection selection = selectionEngine.getSelection();

        if( selection != null || selectionEngine.isBuilding()) {
        	AffineTransform trans = g2.getTransform();
            g2.translate(zoomer.itsX(0), zoomer.itsY(0));
            g2.scale(zoom, zoom);
            g2.setColor(Color.black);
            g2.setStroke(dashedStroke);
            if(selectionEngine.isBuilding()) 
            	selectionEngine.drawBuildingSelection(g);
            if( selection != null) {
                g2.translate( selectionEngine.getOffsetX(), selectionEngine.getOffsetY());
            	selection.drawSelectionBounds(master.getGraphicsContext(),  g);
            }
            g2.setStroke(baseStroke);
            g2.setTransform(trans);
        }
        
        if( penner.drawsOverlay())
        	penner.paintOverlay(g);
        
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
	@Override	public void structureChanged(StructureChangeEvent evt) {	}
 
	
	
	// :::: MSelectionEngineObserver
	@Override	public void selectionBuilt(SelectionEvent evt) {}
	@Override
	public void buildingSelection(SelectionEvent evt) {
    	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
	}
	

	/** Removes Global references (Swing-Global and JNI-Global) to avoid leaks */
	public void cleanUp() {
		AwtPenToolkit.removePenListener(this, penner);
		paint_timer.stop();
		penner.cleanUp();
	}

	@Override
	public void selectionChanged(Node newSelection) {
    	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
	}
}
