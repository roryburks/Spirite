package spirite.panel_work;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.SwingUtilities;

import spirite.brains.MasterControl.MCurrentImageObserver;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.brains.MasterControl;

/**
 *WorkPanel is a container for all the elements of the Draw area.  All external
 *	classes should interact with this Form Element (for things like controlling
 *	zoom, etc) instead of going to the inner Panel as this Form controls things
 *	such as scroll bars which feeds information in a one-way scheme.
 *
 *Internal Panels should use WorkPanel to convert screen coordinates to image
 *	coordinates.
 *
 * A note about understanding the various coordinate systems:
 * -The Scrollbar Values correspond directly to the offset of the image in windospace
 *   So if you wanted to draw image point 0,0 in the top-left corner, the scrollbar
 * 	 should be set to the values 0,0.  With the scroll at 1,1, then the point in the top
 * 	 left corner would be <10,10> (with SCROLL_RATIO of 10), meaning the image is drawn
 * 	 with offset <-10,-10>
 *
 * @author Rory Burks
 */
public class WorkPanel extends javax.swing.JPanel 
        implements MImageObserver,  
        	AdjustmentListener, ComponentListener
{
	Point center = new Point(0,0);
	private static final long serialVersionUID = 1L;
	
	// 1 "tick" of the scrollbar to corresponds to SCROLL_RATIO pixels at zoom 1
	private static final int SCROLL_RATIO = 10;
	
	// at least SCROLL_BUFFER pixels must be visible in the scroll region for any axis
	// 	negative SCROLL_BUFFER would allow you to scroll the image off-screen entirely
    private static final int SCROLL_BUFFER = 100;

    // zoom_level 0 = 1x, 1 = 2x, 2 = 3x, ...
    //  -1 = 1/2x, -2 = 1/3x, -3 = 1/4x ....
    int zoom_level = 0; 
    float zoom = 1.0f;

    int offsetx, offsety;

    MasterControl master;
    ImageWorkspace workspace;


    public WorkPanel( MasterControl master, ImageWorkspace workspace) {
        this.master = master;
        this.workspace = workspace;
        initComponents();

        workspace.addImageObserver(this);

        jscrollHorizontal.addAdjustmentListener(this);
        jscrollVertical.addAdjustmentListener(this);
        this.addComponentListener(this);



        System.out.println("1");
		calibrateScrolls();
        System.out.println("2");

		center.x = workspace.getWidth() / 2;
		center.y = workspace.getHeight() / 2;

    }

    @Override
    public void paint( Graphics g) {
    	// Let swing do the heavy lifting.
        super.paint(g); 
        

        // :: Draws the zoom level in the bottom right corner
        if(zoom_level >= 0) {
            g.setFont( new Font("Tahoma", Font.PLAIN, 12));
            g.drawString(Integer.toString(zoom_level+1), this.getWidth() - ((zoom_level > 8) ? 16 : 12), this.getHeight()-5);
        }
        else {
            g.setFont( new Font("Tahoma", Font.PLAIN, 8));
            g.drawString("1/", this.getWidth() - 15, this.getHeight() - 10);
            g.setFont( new Font("Tahoma", Font.PLAIN, 10));
            g.drawString(Integer.toString(-zoom_level+1), this.getWidth() - ((zoom_level < -8) ? 10 : 8), this.getHeight()-4);
        }
    }
    
    // :::: API
    public void zoom( int amount) {
        // Remember the center so you can re-center the scroll at that position
        int center_x = stiXm(workSplicePanel.getWidth()/2);
        int center_y = stiYm(workSplicePanel.getHeight()/2);

        // Change zoom_level
        zoom_level = amount;

        // Recalculate zoom factor
        zoom = (zoom_level >= 0) ? zoom_level + 1 : 1/(float)(-zoom_level+1);

        calibrateScrolls();

        centerAtPos(center_x, center_y);
        repaint();
    }

    public int getZoomLevel( ) {
        return zoom_level;
    }

    // :::: Coordinate Conversion methods
    // its : converts image coordinates to screen coordinates (accounting for zoom)
    // sti : converts screen coordinates to image coordinates
    int itsX( int x) { return Math.round(x * zoom) + offsetx;}
    int itsY( int y) { return Math.round(y * zoom) + offsety;}
    int stiX( int x) { return Math.round((x - offsetx) / zoom);}
    int stiY( int y) { return Math.round((y - offsety) / zoom);}
    
    // *m functions are as above, but tweaked for mouse coordinates, such that 
    // 	mouse input is rounded as visually expected.
    int itsXm( int x) { return (int) (Math.floor(x * zoom) + offsetx);}
    int itsYm( int y) { return (int) (Math.floor(y * zoom) + offsety);}
    int stiXm( int x) { return (int) Math.floor((x - offsetx) / zoom);}
    int stiYm( int y) { return (int) Math.floor((y - offsety) / zoom);}
    // ::: Internal

    /***
     * Calibrates the Scrollbars to the correct minimum and maximum value
     * such that you can scroll around the entire image +/- the width of the
     * window (with a little buffer determined by SCREEN_BUFFER)
     */
    private boolean calibrating = false;
    private void calibrateScrolls() {
    	calibrating = true;
        if( workspace == null) {
            jscrollHorizontal.setEnabled(false);
            jscrollVertical.setEnabled(false);
            return;
        }

        jscrollHorizontal.setEnabled(true);
        jscrollVertical.setEnabled(true);

        int width = workSplicePanel.getWidth();
        int height = workSplicePanel.getHeight();

        float ratio = SCROLL_RATIO;

        float hor_min = -width + SCROLL_BUFFER;
        float vert_min = -height + SCROLL_BUFFER;
        float hor_max = workspace.getWidth() * zoom - SCROLL_BUFFER;
        float vert_max = workspace.getHeight() * zoom - SCROLL_BUFFER;

        jscrollHorizontal.setMinimum( (int)Math.round(hor_min/ratio));
        jscrollVertical.setMinimum( (int)Math.round(vert_min/ratio));

        jscrollHorizontal.setVisibleAmount(50);
        jscrollVertical.setVisibleAmount(50);

        jscrollHorizontal.setMaximum( (int)Math.round(hor_max/ratio) + jscrollHorizontal.getVisibleAmount());
        jscrollVertical.setMaximum( (int)Math.round(vert_max/ratio) + jscrollVertical.getVisibleAmount());        
        calibrating = false;
    }

    /**
     * Arranges the scroll bars such that the view is centered around the
     * given coordinates on the image.
     * (Note, coordinates are in image-space; do not account for zoom as 
     * the method will do that for you)
     */
    private void centerAtPos( int x, int y) {
        final int ratio = SCROLL_RATIO;

        int px = Math.round(x*zoom - workSplicePanel.getWidth()/2.0f);
        int py = Math.round(y*zoom - workSplicePanel.getHeight()/2.0f);

        System.out.println("Aleph");
        jscrollHorizontal.setValue( Math.round(px / (float)ratio));
        jscrollVertical.setValue(( Math.round(py / (float)ratio)));
        System.out.println("Bet");
        
        center.x = x;
        center.y = y;
    }
    
    public Point getCenter() {
    	Point c = new Point();

    	c.x = stiXm(workSplicePanel.getWidth()/2);
    	c.y = stiYm(workSplicePanel.getHeight()/2);
    	
    	return c;
    }


    // !! Start Generated Code        
    private void initComponents() {

        jscrollVertical = new javax.swing.JScrollBar();
        jscrollHorizontal = new javax.swing.JScrollBar();
        workSplicePanel = new WorkSplicePanel( this);

        jscrollHorizontal.setOrientation(javax.swing.JScrollBar.HORIZONTAL);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(workSplicePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jscrollHorizontal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0)
                .addComponent(jscrollVertical, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jscrollVertical, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(workSplicePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addComponent(jscrollHorizontal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }                      
    

               
    private javax.swing.JScrollBar jscrollHorizontal;
    private javax.swing.JScrollBar jscrollVertical;
    private WorkSplicePanel workSplicePanel;
    // !! End Generated Code

    // ==== Event Listeners and Observers ====

    // :::: MImageObserver
	@Override
	public void imageChanged() {
		this.repaint();
	}

	@Override
	public void structureChanged(StructureChange evt) {
		
        calibrateScrolls();

        centerAtPos( workspace.getWidth()/2, workspace.getHeight()/2);
        this.repaint();
	}


    // :::: Adjustment Listener
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if( e.getSource() == jscrollHorizontal) {
            offsetx = -e.getValue()*SCROLL_RATIO;
            if( !calibrating)
            	center.x = stiXm(workSplicePanel.getWidth()/2);
            this.repaint();
        }
        if( e.getSource() == jscrollVertical) {
            offsety = -e.getValue()*SCROLL_RATIO;
            if( !calibrating)
            	center.x = stiXm(workSplicePanel.getWidth()/2);
            this.repaint();
        }
        
    }

    // :::: Component Listener
    @Override
    public void componentResized(ComponentEvent e) {
        this.calibrateScrolls();

        this.centerAtPos(center.x, center.y);
    }
    @Override
    public void componentMoved(ComponentEvent e) {}
    @Override
    public void componentShown(ComponentEvent e) {}
    @Override
    public void componentHidden(ComponentEvent e) {}


}
