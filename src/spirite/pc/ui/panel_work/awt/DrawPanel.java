//Rory Burks

package spirite.pc.ui.panel_work.awt;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jpen.owner.multiAwt.AwtPenToolkit;
import spirite.base.brains.MasterControl;
import spirite.base.graphics.renderer.RenderEngine;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.selection.SelectionEngine;
import spirite.base.image_data.selection.SelectionEngine.MSelectionEngineObserver;
import spirite.base.image_data.selection.SelectionEngine.SelectionEvent;
import spirite.base.image_data.selection.SelectionMask;
import spirite.gui.hybrid.SPanel;
import spirite.hybrid.Globals;
import spirite.hybrid.HybridUtil;
import spirite.pc.graphics.ImageBI;
import spirite.pc.graphics.awt.AWTContext;
import spirite.pc.pen.JPenPenner;
import spirite.pc.ui.panel_work.WorkPanel;
import spirite.pc.ui.panel_work.WorkPanel.View;

/**
 * DrawPanel is the main UI component for drawing.  It captures the User's input 
 * using the JPen library (for both Mouse and Drawing Tablet) and draws the image
 * in the WorkArea
 * 
 * @author Rory Burks
 */
public class DrawPanel extends SPanel
     implements MImageObserver, ActionListener, MSelectionEngineObserver, 
     	MNodeSelectionObserver
{
	private static final long serialVersionUID = 1L;

//	private final MasterControl master;
	private final RenderEngine renderEngine;
	private SelectionEngine selectionEngine;

	// Components other things need access to
	public ImageWorkspace workspace;
	public final WorkPanel context;
	public View zoomer;
	
	private final JPenPenner penner;
	private final Timer paint_timer;
	
	private int metronome = 0;

	public DrawPanel(WorkPanel context, MasterControl master) {
		this.renderEngine = master.getRenderEngine();
//		this.master = master;
		this.context = context;
		this.setBackground(new Color(0, 0, 0, 0));
		this.setOpaque( false);

		penner = context.getJPenner();
		
		paint_timer = new Timer( 40, this);
		paint_timer.start();
		
		AwtPenToolkit.addPenListener(this, penner);
	}

	
    // :::: Paint
    @Override
    public void paintComponent( Graphics g) {
        super.paintComponent(g);
        
        GraphicsContext gc = new AWTContext(g, getWidth(), getHeight());

        if( workspace == null) {
        	return;
        }
        Graphics2D g2 = (Graphics2D) g;
        final float zoom = zoomer.getZoom();
        
        final BasicStroke dashedStroke = new BasicStroke(
        		1/(( zoom >= 4)?(zoom/2):zoom), 
        		BasicStroke.CAP_BUTT, 
        		BasicStroke.JOIN_BEVEL, 
        		0, 
        		new float[]{4,2}, 
        		0);
        final Stroke baseStroke = g2.getStroke();

        // Draw Border around the Workspace
/*        g2.setStroke(dashedStroke);
        g2.setColor(Globals.getColor("drawpanel.image.border"));
        g2.drawRect( zoomer.itsX(0)-1,
        		zoomer.itsY(0)-1,
	            (int)Math.round(workspace.getWidth()*zoom)+1,
	            (int)Math.round(workspace.getHeight()*zoom)+1);*/
        
        // Draw Image
    	RenderSettings settings = new RenderSettings(
    			renderEngine.getDefaultRenderTarget(workspace));
        long start = System.currentTimeMillis();
        // Render the image
    	BufferedImage image = ((ImageBI)HybridUtil.convert(renderEngine.renderImage(settings), ImageBI.class)).img;

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
        SelectionMask selection = selectionEngine.getSelection();

        if( selection != null ) {
        	AffineTransform trans = g2.getTransform();
        	AffineTransform trans2 = g2.getTransform();
        	g2.translate(zoomer.itsX(0), zoomer.itsY(0));
        	g2.scale(zoom, zoom);
            g2.setColor(Color.black);
            g2.setStroke(dashedStroke);
            if( selection != null) {
            	// TODO
            	selection.drawBounds(gc);
            }
            g2.setStroke(baseStroke);
            g2.setTransform(trans);
        }
        
        if( penner.drawsOverlay())
        	penner.paintOverlay(gc);
        
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
	}

	@Override
	public void selectionChanged(Node newSelection) {
    	context.repaint( SwingUtilities.convertRectangle(this, this.getBounds(), context));
	}


	public void changeWorkspace(ImageWorkspace ws, View view) {
		if( workspace != null) {
			workspace.removeImageObserver(this);
			selectionEngine.removeSelectionObserver(this);
			workspace.removeSelectionObserver(this);
		}
		workspace = ws;
		if( workspace == null)
			selectionEngine = null;
		else {
			selectionEngine = workspace.getSelectionEngine();
			workspace.addImageObserver(this);
			workspace.getSelectionEngine().addSelectionObserver(this);
			workspace.addSelectionObserver(this);
		}
		this.zoomer = view;
	}
}
