package spirite.panel_work;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelEvent;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import spirite.brains.MasterControl;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChange;

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
 * TODO: The various conversions to and from screen coordinates and image coordinate
 *   cause a little drift in things like auto-centering the image.  The things that
 *   absolutely must be pixel perfect (in particular drawing) have been ironed down, 
 *   but a lot of the little UI pieces have off-by-two-or-three errors here and there.
 *
 * @author Rory Burks
 */
public class WorkPanel extends javax.swing.JPanel 
        implements MImageObserver,  
        	AdjustmentListener, ComponentListener, AWTEventListener
{
	private static final long serialVersionUID = 1L;

	int w = 0;	// Image Width
	int h = 0;	// Image Height
	int h_w = 0;	// Half Image Width
	int h_h = 0;	// Half Image Height
	
	// Stores the point where the image is centered at so that the UI components
	//	don't have to do a lot of guess-work when resizing components etc.
	private Point center = new Point(0,0);
	
	// 1 "tick" of the scrollbar to corresponds to SCROLL_RATIO pixels at zoom 1
	private static final int SCROLL_RATIO = 10;
	
	// At least SCROLL_BUFFER pixels must be visible in the scroll region for any axis
	// 	negative SCROLL_BUFFER would allow you to scroll the image off-screen entirely.
    private static final int SCROLL_BUFFER = 100;

    // WorkPanel needs Master because some of its components need it
    private final MasterControl master;
    public final ImageWorkspace workspace;

    private int offsetx, offsety;

    public WorkPanel( MasterControl master, ImageWorkspace workspace) {
        this.master = master;
        this.workspace = workspace;
        initComponents();

        // Add Listeners/Observers
        workspace.addImageObserver(this);

        jscrollHorizontal.addAdjustmentListener(this);
        jscrollVertical.addAdjustmentListener(this);
        this.addComponentListener(this);

        // Add Swing-Level listener for MouseWheelEvents so that the lower
        //	panels do not eat the event.
        //
        // !!!! NOTE: This adds a semi-global reference of WorkPanel to the
        //	Default Toolkit meaning it will not be garbage collected.  For
        //	now this is fine because WorkPanel's life is the same as the program's
        //	but that might change in the future, so you'd have to clean up.
        long eventMask = AWTEvent.MOUSE_WHEEL_EVENT_MASK;
        Toolkit.getDefaultToolkit().addAWTEventListener( this, eventMask);

		calibrateScrolls();

		h = workspace.getHeight();
		w = workspace.getWidth();
		h_h = h/2;
		h_w = w/2;
		
		center.x = workspace.getWidth() / 2;
		center.y = workspace.getHeight() / 2;

    }
    
    public final Zoomer zoomer = new Zoomer();
    public final RefZoomer refzoomer= new RefZoomer();
    
    public class RefZoomer extends Zoomer {
    	private int cx = 0;
    	private int cy = 0;

    	public int getCX() {return cx;}
    	public int getCY() {return cy;}
    	public void setCX(int x) {cx = x; workspace.getReferenceManager().triggerReferenceStructureChanged(false);}
    	public void setCY(int y) {cy = y; workspace.getReferenceManager().triggerReferenceStructureChanged(false);}
    	
    	
    	@Override
    	public void setZoomLevel(int amount) {
            
    		super.setZoomLevel(amount);
    		workspace.getReferenceManager().triggerReferenceStructureChanged(false);
    	}
    	
    	public float getRawZoom() {return zoom;}
    	public void setFineZoom( float zoom) {
    		this.zoom = zoom;
    		workspace.getReferenceManager().triggerReferenceStructureChanged(false);
    	}
    	
    	public float getZoom() {
    		return super.getZoom() * zoomer.getZoom();
    	}
    	
        // :::: Coordinate Conversion methods

        int itsX( int x) { return Math.round( (x + cx) * zoom  * zoomer.zoom) + offsetx  ;}
        int itsY( int y) { return Math.round( (y +cy) * zoom * zoomer.zoom) + offsety  ;}
        int stiX( int x) { return Math.round((x - offsetx) / zoom * zoomer.zoom  - cx);}
        int stiY( int y) { return Math.round((y - offsety ) / zoom * zoomer.zoom  - cy);}
        
        int itsXm( int x) { return (int) (Math.floor((x + cx) * zoom * zoomer.zoom) + offsetx  );}
        int itsYm( int y) { return (int) (Math.floor((y  + cy)* zoom * zoomer.zoom) + offsety );}
        int stiXm( int x) { return (int) Math.floor((x - offsetx  ) / zoom * zoomer.zoom - cx);}
        int stiYm( int y) { return (int) Math.floor((y - offsety ) / zoom * zoomer.zoom  - cy);}
    }
    
    public class Zoomer {


        // zoom_level 0 = 1x, 1 = 2x, 2 = 3x, ...
        //  -1 = 1/2x, -2 = 1/3x, -3 = 1/4x ....
        int zoom_level = 0; 
        float zoom = 1.0f;
        
        
        // :::: API
        public void setZoomLevel( int amount) {
            // Remember the center so you can re-center the scroll at that position

            // Change zoom_level
            zoom_level = amount;

            // Recalculate zoom factor
            zoom = (zoom_level >= 0) ? zoom_level + 1 : 1/(float)(-zoom_level+1);

            // Readjust the Scrollbar
            calibrateScrolls();
            centerAtPos(center.x, center.y);
            
            workSplicePanel.drawPanel.refreshPennerCoords();
            repaint();
        }
        
        public void zoomIn() {
            if( zoom_level >= 11)
            	setZoomLevel(((zoom_level+1)/4)*4 + 3);   // Arithmetic's a little unintuitive because of zoom_level's off by 1
            else if( zoom_level >= 3)
            	setZoomLevel(((zoom_level+1)/2)*2 + 1);
            else
            	setZoomLevel( zoom_level+1);
        }
        public void zoomOut() {
            if( zoom_level > 11)
            	setZoomLevel((zoom_level/4)*4-1);
            else if( zoom_level > 3)
            	setZoomLevel((zoom_level/2)*2-1);
            else
            	setZoomLevel(zoom_level - 1);
        }

        public int getZoomLevel( ) {
            return zoom_level;
        }
        
        public float getZoom() {
        	return zoom;
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
        
        Rectangle itsRm( Rectangle rect) {
        	return new Rectangle(
        		itsXm( rect.x),
        		itsYm( rect.y),
        		Math.round(rect.width * zoom),
        		Math.round(rect.height*zoom)
        	);
        }
    }
    
    private void setCenter( int x, int y) {
    	center.x = Math.max(0, Math.min(workspace.getWidth(), x));
    	center.y = Math.max(0, Math.min(workspace.getHeight(), y));
    }
    
    // Image Coordinates that you're mouse-over'd displayed in the bottom bar
    private int mx = 0;
    private int my = 0;
    void refreshCoordinates( int x, int y) {
    	if( mx != x || my != y) {
    		mx = x;
    		my = y;
    		
    		Rectangle rect = new Rectangle(0,0,workspace.getWidth(), workspace.getHeight());
    		if( rect.contains(x, y)) {
    			coordinateLabel.setText( mx + "," + my);
    		}
    		else
    			coordinateLabel.setText("");
    	}
    }
    
    


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
        float hor_max = workspace.getWidth() * zoomer.zoom - SCROLL_BUFFER;
        float vert_max = workspace.getHeight() * zoomer.zoom - SCROLL_BUFFER;

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

        int px = Math.round(x*zoomer.zoom - workSplicePanel.getWidth()/2.0f);
        int py = Math.round(y*zoomer.zoom - workSplicePanel.getHeight()/2.0f);

        jscrollHorizontal.setValue( Math.round(px / (float)ratio));
        jscrollVertical.setValue(( Math.round(py / (float)ratio)));
        
        setCenter( x, y);
    }
    
    public Point getCenter() {
    	Point c = new Point();

    	c.x = zoomer.stiXm(workSplicePanel.getWidth()/2);
    	c.y = zoomer.stiYm(workSplicePanel.getHeight()/2);
    	
    	return c;
    }
    

    private void initComponents() {

        jscrollVertical = new JScrollBar();
        jscrollHorizontal = new JScrollBar();
        workSplicePanel = new WorkSplicePanel( this, master);
        coordinateLabel = new JLabel();
        zoomPanel = new JPanel() {
        	@Override
        	protected void paintComponent(Graphics g) {
        		super.paintComponent(g);
        		// :: Draws the zoom level in the bottom right corner
                if(zoomer.zoom_level >= 0) {
                    g.setFont( new Font("Tahoma", Font.PLAIN, 12));
                    g.drawString(Integer.toString(zoomer.zoom_level+1), this.getWidth() - ((zoomer.zoom_level > 8) ? 16 : 12), this.getHeight()-5);
                }
                else {
                    g.setFont( new Font("Tahoma", Font.PLAIN, 8));
                    g.drawString("1/", this.getWidth() - 15, this.getHeight() - 10);
                    g.setFont( new Font("Tahoma", Font.PLAIN, 10));
                    g.drawString(Integer.toString(-zoomer.zoom_level+1), this.getWidth() - ((zoomer.zoom_level < -8) ? 10 : 8), this.getHeight()-4);
                }
        	}
        };

        jscrollHorizontal.setOrientation(javax.swing.JScrollBar.HORIZONTAL);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(workSplicePanel)
                    .addComponent(jscrollHorizontal, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup( layout.createParallelGroup()
                    .addComponent(jscrollVertical)	
                 // !!!! TODO: figure out why this is necessary to keep the layout working as expected and find a better way
                    .addComponent(zoomPanel, 0, 0, jscrollVertical.getPreferredSize().width)
                 // !!!!
                )
            )
            .addComponent(coordinateLabel)
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jscrollVertical, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(workSplicePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup( layout.createParallelGroup()
                .addComponent(jscrollHorizontal)
                .addComponent(zoomPanel, 0, 0, jscrollHorizontal.getPreferredSize().height)
            )
            .addComponent(coordinateLabel, 24,24,24)
        );
    }
    
    private JLabel coordinateLabel;
    private javax.swing.JScrollBar jscrollHorizontal;
    private javax.swing.JScrollBar jscrollVertical;
    WorkSplicePanel workSplicePanel;	// !!!! Maybe should be private with getter
    private JPanel zoomPanel;

    // ==== Event Listeners and Observers ====

    // :::: MImageObserver
	@Override
	public void imageChanged(ImageChangeEvent evt) {
		this.repaint();
	}

	@Override
	public void structureChanged(StructureChange evt) {
		if( workspace.getWidth() != w || workspace.getHeight() != h) {
			w = workspace.getWidth();
			h = workspace.getHeight();
			h_w = w/2;
			h_h = h/2;
			
	        calibrateScrolls();
	
	        centerAtPos( this.center.x, this.center.y);
	        this.repaint();
		}
	}


    // :::: AdjustmentListener
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        if( e.getSource() == jscrollHorizontal) {
            offsetx = -e.getValue()*SCROLL_RATIO;
            if( !calibrating)  {
            	setCenter( zoomer.stiXm(workSplicePanel.getHeight()/2), center.y);
            }
            this.repaint();
        }
        if( e.getSource() == jscrollVertical) {
        	offsety = -e.getValue()*SCROLL_RATIO;
            if( !calibrating) {
            	setCenter( center.x, zoomer.stiYm(workSplicePanel.getHeight()/2));
            }
            this.repaint();
        }
        
    }

    // :::: ComponentListener
    @Override public void componentMoved(ComponentEvent e) {}
    @Override public void componentShown(ComponentEvent e) {}
    @Override public void componentHidden(ComponentEvent e) {}
    @Override
    public void componentResized(ComponentEvent e) {
        this.calibrateScrolls();

        this.centerAtPos(center.x, center.y);
    }

    // :::: AWTEvent Listener, MOUSE_WHEEL_EVENT_MASK
    // I'm not sure if this is necessary, but some weird behavior compells 
    //	me to do it.
    boolean eventlock = false;	
	@Override
	public void eventDispatched(AWTEvent raw) {
		if( eventlock) return;
		eventlock = true;
		if( raw instanceof MouseWheelEvent) {
			mouseWheelEvent((MouseWheelEvent)raw);
		}
		eventlock = false;
	}

	private void mouseWheelEvent( MouseWheelEvent evt) {
		if(evt.isControlDown()) {			
			// Verify that the Mouse Event happens within the space of
			//	the WorkPanel and convert its point to local coordinates.
			Component source = (Component)evt.getSource();
			if( SwingUtilities.isDescendingFrom(this, source)) {
				Point p = SwingUtilities.convertPoint(source, evt.getPoint(), this);

				if( this.contains(p)) {
				if( evt.getWheelRotation() < 0) {
					center.x = (center.x + zoomer.stiX(p.x))/2;
					center.y = (center.y + zoomer.stiY(p.y))/2;
					zoomer.zoomIn();
				}
				else if( evt.getWheelRotation() > 0)
					zoomer.zoomOut();
				}
			}
		}
	}

}
