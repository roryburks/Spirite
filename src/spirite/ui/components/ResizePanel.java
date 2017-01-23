package spirite.ui.components;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * A ResizePanel keeps track of the size of a component within a container.
 * It does not actually do any resziing, but rather it tracks mouse movement
 * and converts it to the intended size which a Layout can deal with.  
 * 
 * For now it is binded to RootFrame, but it could easily
 * be generalized using a passed interface instead resetLayout()
 */
public class ResizePanel extends JPanel {
	
	public enum Orientation {
		EAST, WEST, NORTH, SOUTH
	};
	
	private final Orientation orientation;
	private final ResizeBarAdapter adapter;
	private final ResizeAlerter alerter;
	private final Container container;
	private int size;
	private int buffer = 50;
	
	/**
	 * 
	 * @param orientation
	 * @param default_size
	 * @param alerter		
	 * 		An interface that will be called when a logical resize is happening.
	 * @param container		
	 * 		An external component is needed to smooth out mouse events during
	 * 		a time when the ResizePanel's coordinate space may be changing, so it
	 * 		needs
	 */
	public ResizePanel(Orientation orientation, int default_size, Container container, ResizeAlerter alerter) {
		this.orientation = orientation;
		this.adapter = new ResizeBarAdapter();
		this.alerter = alerter;
		this.container = container;
		this.size = default_size;

		this.addMouseListener(adapter);
		this.addMouseMotionListener(adapter);
		
		switch( orientation) {
		case EAST:
		case WEST:
			this.setCursor(new Cursor( Cursor.E_RESIZE_CURSOR));
			break;
		case NORTH:
		case SOUTH:
			this.setCursor(new Cursor( Cursor.N_RESIZE_CURSOR));
			break;
		}
		
	}
	
	public int getResizeSize() {
		return size;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponents(g);

		int depth;
		int breadth;
		g.setColor(new Color(190,190,190));
		switch( orientation) {
		case EAST:
		case WEST:
			depth = getWidth();
			breadth = getHeight();
			g.drawLine( 0, 10, 0, breadth-10);
			g.drawLine( depth/2, 5, depth/2, breadth-5);
			g.drawLine( depth-1, 10, depth-1, breadth-10);
			break;
		case NORTH:
		case SOUTH:
			depth = getHeight();
			breadth = getWidth();
			g.drawLine( 10, 0, breadth-10, 0);
			g.drawLine( 5, depth/2, breadth-5, depth/2);
			g.drawLine( 10, depth-1,  breadth-10, depth-1);
			break;
		}
		
	}
	
    private class ResizeBarAdapter extends MouseAdapter {
    	int start_pos;
    	int start_size;
    	
    	public ResizeBarAdapter( ) {}
    	
    	@Override
    	public void mousePressed(MouseEvent e) {
    		Point p = SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), container);
    		
    		switch( orientation) {
    		case EAST:
    		case WEST:
    			start_pos = p.x;
    			break;
    		case NORTH:
    		case SOUTH:
    			start_pos = p.y;
    			break;
    		}
    		start_size = size;
    	}
    	
    	@Override
    	public void mouseDragged(MouseEvent e) {
    		Point p = SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), container);

    		switch( orientation) {
    		case EAST:
        		size = start_size + (start_pos - p.x);
        		size = Math.max( buffer, Math.min( getParent().getWidth()-buffer, size));
        		break;
    		case WEST:
        		size = start_size - (start_pos - p.x);
        		size = Math.max( buffer, Math.min( getParent().getWidth()-buffer, size));
        		break;
    		case NORTH:
        		size = start_size - (start_pos - p.y);
        		size = Math.max( buffer, Math.min( getParent().getHeight()-buffer, size));
        		break;
    		case SOUTH:
        		size = start_size + (start_pos - p.y);
        		size = Math.max( buffer, Math.min( getParent().getHeight()-buffer, size));
        		break;
    		}
    		
    		alerter.alert();
    		
    		super.mouseDragged(e);
    	}
    }
    
    public static interface ResizeAlerter {
    	public void alert();
    }
}