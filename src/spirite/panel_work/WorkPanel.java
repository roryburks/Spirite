package spirite.panel_work;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import spirite.MDebug;
import spirite.brains.MasterControl.MImageObserver;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.SpiriteImage;
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
 * @author Rory Burks
 */
public class WorkPanel extends javax.swing.JPanel 
        implements MImageObserver, AdjustmentListener, ComponentListener
{
    private static final int SCROLL_RATIO = 10;
    private static final int SCROLL_BUFFER = 100;

    int zoom_level = 0; // zoom_level 0 = 1x, 1 = 2x, 2 = 3x, ...
                        //           -1 = 1/2x, -2 = 1/3x, -3 = 1/4x ....
    float zoom = 1.0f;

    int offsetx, offsety;

    MasterControl master;


    public WorkPanel() {initComponents();}  // !!! Only exists so that the IDE doesn't freak out
    public WorkPanel( MasterControl master) {
        this.master = master;
        initComponents();

        master.addImageObserver(this);

        jscrollHorizontal.addAdjustmentListener(this);
        jscrollVertical.addAdjustmentListener(this);
        this.addComponentListener(this);

        calibrateScrolls();

        offsetx = 0;
        offsety = 0;
    }

    @Override
    public void paint( Graphics g) {
        super.paint(g); // Makes sure all the child components get painted

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

    // :::: WorkPanel Group related
    int itsX( int x) { return Math.round(x * zoom) + offsetx;}
    int itsY( int y) { return Math.round(y * zoom) + offsety;}
    int stiX( int x) { return Math.round((x - offsetx) / zoom);}
    int stiY( int y) { return Math.round((y - offsety) / zoom);}
    
    // *m functions are tweaked such that mouse input effects the pixel it
    //  looks like it should effect
    int itsXm( int x) { return Math.round(x * zoom + zoom/2 - 1) + offsetx;}
    int itsYm( int y) { return Math.round(y * zoom + zoom/2 - 1) + offsety;}
    int stiXm( int x) { return Math.round((x - offsetx-zoom/2-1) / zoom);}
    int stiYm( int y) { return Math.round((y - offsety-zoom/2-1) / zoom);}

    // ::: Internal

    /***
     * Calibrates the Scrollbars to the correct minimum and maximum value
     * such that you can scroll around the entire image +/- the width of the
     * window (with a little buffer determined by SCREEN_BUFFER)
     */
    private void calibrateScrolls() {
    	ImageWorkspace image = master.getCurrentWorkspace();
    	
        if( image == null) {
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
        float hor_max = image.getWidth() * zoom - SCROLL_BUFFER;
        float vert_max = image.getHeight() * zoom - SCROLL_BUFFER;

        jscrollHorizontal.setMinimum( (int)Math.round(hor_min/ratio));
        jscrollVertical.setMinimum( (int)Math.round(vert_min/ratio));

        jscrollHorizontal.setMaximum( (int)Math.round(hor_max/ratio) + jscrollHorizontal.getVisibleAmount());
        jscrollVertical.setMaximum( (int)Math.round(vert_max/ratio) + jscrollVertical.getVisibleAmount());
    }

    /**
     * Arranges the scroll bars such that the view is centered around the
     * given coordinates on the image.
     * (Note, coordinates are in image-space, do not account for zoom)
     */
    private void centerAtPos( int x, int y) {
    	ImageWorkspace image = master.getCurrentWorkspace();

        if(image == null) return;

        final int ratio = SCROLL_RATIO;

        int px = (int)(x*zoom - workSplicePanel.getWidth()/2);
        int py = (int)(y*zoom - workSplicePanel.getHeight()/2);

        jscrollHorizontal.setValue( Math.round(px / (float)ratio));
        jscrollVertical.setValue(( Math.round(py / (float)ratio)));
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
    public void newImage() {
        calibrateScrolls();

    	ImageWorkspace image = master.getCurrentWorkspace();
        centerAtPos( image.getWidth()/2, image.getHeight()/2);
        this.repaint();
    }

    // :::: Adjustment Listener
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if( e.getSource() == jscrollHorizontal) {
            offsetx = -e.getValue()*SCROLL_RATIO;
            this.repaint();
        }
        if( e.getSource() == jscrollVertical) {
            offsety = -e.getValue()*SCROLL_RATIO;
            this.repaint();
        }
    }

    // :::: Component Listener
    @Override
    public void componentResized(ComponentEvent e) {
        int center_x = stiXm(workSplicePanel.getWidth()/2);
        int center_y = stiYm(workSplicePanel.getHeight()/2);

        this.calibrateScrolls();

        this.centerAtPos(center_x, center_y);
    }
    @Override
    public void componentMoved(ComponentEvent e) {}
    @Override
    public void componentShown(ComponentEvent e) {}
    @Override
    public void componentHidden(ComponentEvent e) {}
}
