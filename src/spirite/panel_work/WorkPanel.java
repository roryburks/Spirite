package spirite.panel_work;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

import com.sun.glass.ui.View;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.panel_work.awt.WorkSplicePanel;

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
        	AdjustmentListener, ComponentListener, AWTEventListener, MWorkspaceObserver
{
	private static final long serialVersionUID = 1L;
	
	// ==========
	// ==== Settings
	// 1 "tick" of the scrollbar to corresponds to SCROLL_RATIO pixels at zoom 1
	private static final int SCROLL_RATIO = 10;
	
	// At least SCROLL_BUFFER pixels must be visible in the scroll region for any axis
	// 	negative SCROLL_BUFFER would allow you to scroll the image off-screen entirely.
    private static final int SCROLL_BUFFER = 100;

    
    private final MasterControl master;
    public ImageWorkspace currentWorkspace;

    private final Map<ImageWorkspace,View> views = new HashMap<>();
//    public final View zoomer = new View();
    private final JPenPenner jpenner;

    public WorkPanel( MasterControl master) {
        this.master = master;
        
        jpenner = new JPenPenner(this, master);
        initComponents();


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
		
		master.addWorkspaceObserver(this);



    }
    
    public Penner getPenner() { return jpenner.penner; }
    public JPenPenner getJPenner() {return jpenner;}
    public View getCurrentView() {return views.get(currentWorkspace);}
    public View getView(ImageWorkspace ws) {return views.get(ws);}

    /** The view describes which part of the image is currently being seen and
     * manages conversions between the screen space and the image space. */
    public class View {
        /** zoom_level 0 = 1x, 1 = 2x, 2 = 3x, ...
         *  -1 = 1/2x, -2 = 1/3x, -3 = 1/4x .... */
        int zoom_level = 0; 
        float zoom = 1.0f;
        
        int offsetx, offsety;
    	int w, h;	// Image Width/Height
    	int h_w, h_h;	// Half Image Width/Height
    	

    	// Stores the point where the image is centered at so that the UI components
    	//	don't have to do a lot of guess-work when resizing components etc.
    	int cx, cy;
        
        
        // :::: API
        public void setZoomLevel( int amount) {
            // Remember the center so you can re-center the scroll at that position

            // Change zoom_level
            zoom_level = amount;

            // Recalculate zoom factor
            zoom = (zoom_level >= 0) ? zoom_level + 1 : 1/(float)(-zoom_level+1);

            // Readjust the Scrollbar
            // TODO: If for whatever reason a non-active zoomer gets zoomed, it 
            //	presently affects the active view too
            calibrateScrolls();
            centerAtPos(cx, cy);
            
            jpenner.refreshCoordinates();
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

        public int getZoomLevel( ) { return zoom_level; }
        public float getZoom() { return zoom; }

        // :::: Coordinate Conversion methods
        // its : converts image coordinates to screen coordinates (accounting for zoom)
        // sti : converts screen coordinates to image coordinates
        public int itsX( int x) { return Math.round(x * zoom) + offsetx;}
        public int itsY( int y) { return Math.round(y * zoom) + offsety;}
        public int stiX( int x) { return Math.round((x - offsetx) / zoom);}
        public int stiY( int y) { return Math.round((y - offsety) / zoom);}
        
        // *m functions are as above, but tweaked for mouse coordinates, such that 
        // 	mouse input is rounded as visually expected.
        public int itsXm( int x) { return (int) (Math.floor(x * zoom) + offsetx);}
        public int itsYm( int y) { return (int) (Math.floor(y * zoom) + offsety);}
        public int stiXm( int x) { return (int) Math.floor((x - offsetx) / zoom);}
        public int stiYm( int y) { return (int) Math.floor((y - offsety) / zoom);}
        
        public Rectangle itsRm( Rectangle rect) {
        	return new Rectangle(
        		itsXm( rect.x),
        		itsYm( rect.y),
        		Math.round(rect.width * zoom),
        		Math.round(rect.height*zoom)
        	);
        }
    }
    
    private void setCenter( int x, int y) {
    	View view = views.get(currentWorkspace);
    	if( view != null) {
	    	view.cx = Math.max(0, Math.min(currentWorkspace.getWidth(), x));
	    	view.cy = Math.max(0, Math.min(currentWorkspace.getHeight(), y));
    	}
    }
    
    // Image Coordinates that you're mouse-over'd displayed in the bottom bar
    private int mx = 0;
    private int my = 0;
    public void refreshCoordinates( int x, int y) {
    	if( mx != x || my != y) {
    		mx = x;
    		my = y;
    		
    		Rectangle rect = new Rectangle(0,0,currentWorkspace.getWidth(), currentWorkspace.getHeight());
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
        if( currentWorkspace == null) {
            jscrollHorizontal.setEnabled(false);
            jscrollVertical.setEnabled(false);
            return;
        }

        jscrollHorizontal.setEnabled(true);
        jscrollVertical.setEnabled(true);

        int width = workAreaAsComponent.getWidth();
        int height = workAreaAsComponent.getHeight();

        float ratio = SCROLL_RATIO;

        float hor_min = -width + SCROLL_BUFFER;
        float vert_min = -height + SCROLL_BUFFER;
        float hor_max = currentWorkspace.getWidth() * views.get(currentWorkspace).zoom - SCROLL_BUFFER;
        float vert_max = currentWorkspace.getHeight() *  views.get(currentWorkspace).zoom - SCROLL_BUFFER;

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

        View view = views.get(currentWorkspace);
        if( view != null) {
	        int px = Math.round(x*view.zoom - workAreaAsComponent.getWidth()/2.0f);
	        int py = Math.round(y*view.zoom - workAreaAsComponent.getHeight()/2.0f);
	
	        jscrollHorizontal.setValue( Math.round(px / (float)ratio));
	        jscrollVertical.setValue(( Math.round(py / (float)ratio)));
        }
        
        setCenter( x, y);
    }
    
    public Point getCenter() {
    	Point c = new Point();

        View view = views.get(currentWorkspace);
        if( view != null) {
	    	c.x = view.stiXm(workAreaAsComponent.getWidth()/2);
	    	c.y = view.stiYm(workAreaAsComponent.getHeight()/2);
        }
    	
    	return c;
    }
    

    private void initComponents() {
        jscrollVertical = new JScrollBar();
        jscrollHorizontal = new JScrollBar();
        
        
        WorkSplicePanel wsp = new WorkSplicePanel( this, master);
        workArea = wsp;
        workAreaAsComponent = wsp;
        coordinateLabel = new JLabel();
        zoomPanel = new JPanel() {
        	@Override
        	protected void paintComponent(Graphics g) {
        		super.paintComponent(g);
        		
        		View view = views.get(currentWorkspace);
        		if( view == null) return;
        		
        		// :: Draws the zoom level in the bottom right corner
                if(view.zoom_level >= 0) {
                    g.setFont( new Font("Tahoma", Font.PLAIN, 12));
                    g.drawString(Integer.toString(view.zoom_level+1), this.getWidth() - ((view.zoom_level > 8) ? 16 : 12), this.getHeight()-5);
                }
                else {
                    g.setFont( new Font("Tahoma", Font.PLAIN, 8));
                    g.drawString("1/", this.getWidth() - 15, this.getHeight() - 10);
                    g.setFont( new Font("Tahoma", Font.PLAIN, 10));
                    g.drawString(Integer.toString(-view.zoom_level+1), this.getWidth() - ((view.zoom_level < -8) ? 10 : 8), this.getHeight()-4);
                }
        	}
        };

        workAreaContainer.setLayout( new GridLayout());
        workAreaContainer.add(workAreaAsComponent);
        jscrollHorizontal.setOrientation(javax.swing.JScrollBar.HORIZONTAL);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(workAreaContainer)
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
                .addComponent(workAreaContainer, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
    
    private JPanel workAreaContainer = new JPanel();
    // Very ugly, but neither a strict hierarchy nor an interface alone would work
    private WorkArea workArea;	
    private Component workAreaAsComponent;
    private JPanel zoomPanel;

    // ===============
    // ==== Event Listeners and Observers ====

    // :::: MImageObserver
	@Override
	public void imageChanged(ImageChangeEvent evt) {
		this.repaint();
	}

	@Override
	public void structureChanged(StructureChangeEvent evt) {
    	View zoomer = views.get(currentWorkspace);
		if( zoomer != null && (currentWorkspace.getWidth() != zoomer.w 
				|| currentWorkspace.getHeight() != zoomer.h)) {
			zoomer.w = currentWorkspace.getWidth();
			zoomer.h = currentWorkspace.getHeight();
			zoomer.h_w = zoomer.w/2;
			zoomer.h_h = zoomer.h/2;
			
	        calibrateScrolls();
	
	        centerAtPos( zoomer.cx, zoomer.cy);
	        this.repaint();
		}
	}


    // :::: AdjustmentListener
    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
    	View view = views.get(currentWorkspace);
    	if( view != null) {
	        if( e.getSource() == jscrollHorizontal) {
	            view.offsetx = -e.getValue()*SCROLL_RATIO;
	            if( !calibrating)  {
	            	setCenter( view.stiXm(workAreaAsComponent.getHeight()/2), view.cy);
	            }
	            this.repaint();
	        }
	        if( e.getSource() == jscrollVertical) {
	        	view.offsety = -e.getValue()*SCROLL_RATIO;
	            if( !calibrating) {
	            	setCenter( view.cx, view.stiYm(workAreaAsComponent.getHeight()/2));
	            }
	            this.repaint();
	        }
    	}
        
    }

    // :::: ComponentListener
    @Override public void componentMoved(ComponentEvent e) {}
    @Override public void componentShown(ComponentEvent e) {}
    @Override public void componentHidden(ComponentEvent e) {}
    @Override
    public void componentResized(ComponentEvent e) {
        this.calibrateScrolls();

        View view = views.get(currentWorkspace);
        if( view != null)
        	this.centerAtPos(view.cx, view.cy);
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
		View view = views.get(currentWorkspace);
		if( view == null) return;
		
		if(evt.isControlDown()) {			
			// Verify that the Mouse Event happens within the space of
			//	the WorkPanel and convert its point to local coordinates.
			Component source = (Component)evt.getSource();
			if( SwingUtilities.isDescendingFrom(this, source)) {
				
				Point p = SwingUtilities.convertPoint(source, evt.getPoint(), this);

				if( this.contains(p)) {
				if( evt.getWheelRotation() < 0) {
					view.cx = (view.cx + view.stiX(p.x))/2;
					view.cy = (view.cy + view.stiY(p.y))/2;
					view.zoomIn();
				}
				else if( evt.getWheelRotation() > 0)
					view.zoomOut();
				}
			}
		}
	}

	
	// :::: MWorkspaceObserver
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		currentWorkspace = selected;
		jpenner.penner.changeWorkspace(currentWorkspace, views.get(selected));
		workArea.changeWorkspace(selected, views.get(selected));
	}

	@Override
	public void newWorkspace(ImageWorkspace newWorkspace) {
		View view = new View();
		
		view.h = newWorkspace.getHeight();
		view.w = newWorkspace.getWidth();
		view.h_h = view.h/2;
		view.h_w = view.w/2;
		
		view.cx = newWorkspace.getWidth() / 2;
		view.cy = newWorkspace.getHeight() / 2;
		
		views.put(newWorkspace, view);
		

        // Add Listeners/Observers
		newWorkspace.addImageObserver(this);
	}

	@Override
	public void removeWorkspace(ImageWorkspace newWorkspace) {
		views.remove(newWorkspace);
	}

}
