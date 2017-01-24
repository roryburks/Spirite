package spirite.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import spirite.MUtil;

/**
 * 
 * Unlike most index systems that start at 0 and use -1 to indicate
 * non-existent, for ResizePanel Components:
 *  <li>0 represents non-existent
 *  <li>positive numbers represent panels on the west/north (1 being the
 *         closest to the edge)
 *  <li>negative numbers represent panels on the east/south (-1 being the
 *         closest to the edge)
 * @author Rory Burks
 *
 */
public class ResizeContainerPanel extends JPanel{
	private int min_stretch = 0;
	private final List<ResizeBar> leadingBars;
	private final List<ResizeBar> trailingBars;
	private ContainerOrientation cOrientation;
	private JComponent stretchComponent;

	public static final int LEADING = Integer.MAX_VALUE;
	public static final int TRAILING = Integer.MIN_VALUE;
	
	
	public enum ContainerOrientation {
		HORIZONTAL,
		VERTICAL
	}
	
	public ResizeContainerPanel(JComponent component, ContainerOrientation orientation) {
		leadingBars = new ArrayList<>();
		trailingBars = new ArrayList<>();
		this.cOrientation = orientation;
		this.stretchComponent = component;
	}
	

	public ContainerOrientation getOrientation() { return cOrientation;}
	public void setOrientation( ContainerOrientation orientation) {
		this.cOrientation = orientation;
	}
	
	public int getStretchArea() { return min_stretch;}
	public void setStretchArea(int min) {
		min_stretch = min;
	}
	
	
	public void setPanelComponent( ResizeBar panel, JComponent component) {
		
	}
	
	private void resetLayout() {
        GroupLayout layout = new GroupLayout(this);

        GroupLayout.Group stretch = layout.createSequentialGroup();
        GroupLayout.Group noStretch = layout.createParallelGroup(Alignment.LEADING);
        
        for( ResizeBar bar : leadingBars) {
        	stretch.addComponent(bar.component, bar.getResizeSize(),bar.getResizeSize(),bar.getResizeSize())
        		.addComponent(bar, 6, 6, 6);
        }
        if( stretchComponent != null) {
        	stretch.addComponent(stretchComponent);
        }
        for( ResizeBar bar : trailingBars) {
        	stretch.addComponent(bar, 6, 6, 6)
        		.addComponent(bar.component, bar.getResizeSize(),bar.getResizeSize(),bar.getResizeSize());
        }

        for( ResizeBar bar : leadingBars)
        	noStretch.addComponent(bar.component).addComponent(bar);
        for( ResizeBar bar : trailingBars)
        	noStretch.addComponent(bar.component).addComponent(bar);
        if( stretchComponent != null)
        	noStretch.addComponent(stretchComponent);
        
        switch( cOrientation) {
        case HORIZONTAL:
            layout.setHorizontalGroup(stretch);
            layout.setVerticalGroup(noStretch);
            break;
        case VERTICAL:
            layout.setHorizontalGroup(noStretch);
            layout.setVerticalGroup(stretch);
            break;
        }
        this.setLayout(layout);
        
	}
	
	/** If the position is less than the smallest or more than the largest,
	 * it'll add to the closest, so use Integer.MIN_VALUE to add it to the
	 * left-most right (or up-most down) and Integer.Max_VALUE to add it
	 * to the right-most left (or down-most up)
	 * 
	 * Alternately use LEADING and TRAILING.
	 * 
	 * If position is 0, it'll add it to the left as the right-most.
	 * */
	public int addPanel( int min_size, int default_size, int position, JComponent component) {
		if( position == 0) position = Integer.MAX_VALUE;
		int ret = position;
		
		System.out.println(ret);
		if( position < 0) {
			if( -position >= trailingBars.size()) {
				trailingBars.add(new ResizeBar(default_size,min_size, component, true));
				ret = -trailingBars.size();
			}
			else {
				trailingBars.add(-position-1,new ResizeBar(default_size,min_size,component,true));
			}
		}
		else {
			if( position >= leadingBars.size()) {
				leadingBars.add(new ResizeBar(default_size,min_size, component,false));
				ret = leadingBars.size();
			}
			else {
				leadingBars.add(position-1,new ResizeBar(default_size,min_size,component,false));
			}
		}

		System.out.println(ret);

		resetLayout();
		return ret;
	}
	public void removePanel( int index) {
		
	}
	public void removeAllPanels() {
		
	}
	
	/** List places the West/North components then the East/South ones. */
	public List<ResizeBar> getPanels() {
		return null;
	}
	
	
	public class ResizeBar extends JPanel {
		private final ResizeBarAdapter adapter;
		private int size;
		private int min_size = 50;
		private JComponent component;
		private final boolean trailing;
		
		/**
		 * 
		 * @param orientation
		 * @param default_size
		 * @param component 
		 * @param alerter		
		 * 		An interface that will be called when a logical resize is happening.
		 * @param container		
		 * 		An external component is needed to smooth out mouse events during
		 * 		a time when the ResizePanel's coordinate space may be changing, so it
		 * 		needs
		 */
		private ResizeBar(
				int default_size,
				int min_size, 
				JComponent component,
				boolean trailing) 
		{
			this.min_size = min_size;
			this.size = default_size;
			this.component = component;
			this.trailing = trailing;

			this.adapter = new ResizeBarAdapter();
			this.addMouseListener(adapter);
			this.addMouseMotionListener(adapter);
			
			switch( cOrientation) {
			case HORIZONTAL:
				this.setCursor(new Cursor( Cursor.E_RESIZE_CURSOR));
				break;
			case VERTICAL:
				this.setCursor(new Cursor( Cursor.N_RESIZE_CURSOR));
				break;
			}
			
		}
		
		public int getResizeSize() {
			return size;
		}
		
		/** Sets the minimum amount of stretch room (room for the stretching component
		 * that isn't one of the Resize Panels).	 */
		public void setMinStretch(int min) {
			min_stretch = min;
		}
		
		public void setMinResizeSize( int min) {
			min_size = min;
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponents(g);

			int depth;
			int breadth;
			g.setColor(new Color(190,190,190));
			switch( cOrientation) {
			case HORIZONTAL:
				depth = getWidth();
				breadth = getHeight();
				g.drawLine( 0, 10, 0, breadth-10);
				g.drawLine( depth/2, 5, depth/2, breadth-5);
				g.drawLine( depth-1, 10, depth-1, breadth-10);
				break;
			case VERTICAL:
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
	    	
	    	int reserved;
	    	
	    	public ResizeBarAdapter( ) {}
	    	
	    	@Override
	    	public void mousePressed(MouseEvent e) {
	    		Point p = SwingUtilities.convertPoint( 
	    				e.getComponent(),
	    				e.getPoint(), 
	    				ResizeContainerPanel.this);

				reserved = 0;

				// Maybe there's a sexy way to combine two List Iterators,
				//	but this isn't Python
				for( ResizeBar other : leadingBars) {
					if( other == ResizeBar.this) continue;

					switch( cOrientation) {
					case HORIZONTAL:
						reserved += other.getResizeSize() + other.getWidth();
						break;
					case VERTICAL:
						reserved += other.getResizeSize() + other.getHeight();
						break;
					}
				}
				for( ResizeBar other : trailingBars) {
					if( other == ResizeBar.this) continue;

					switch( cOrientation) {
					case HORIZONTAL:
						reserved += other.getResizeSize() + other.getWidth();
						break;
					case VERTICAL:
						reserved += other.getResizeSize() + other.getHeight();
						break;
					}
				}

	    		switch( cOrientation) {
				case HORIZONTAL:
	    			start_pos = p.x;
	    			break;
				case VERTICAL:
	    			start_pos = p.y;
	    			break;
	    		}
	    		
	    		start_size = size;
	    	}
	    	
	    	@Override
	    	public void mouseDragged(MouseEvent e) {
	    		Point p = SwingUtilities.convertPoint( 
	    				e.getComponent(),
	    				e.getPoint(), 
	    				ResizeContainerPanel.this);

	    		
	    		switch( cOrientation) {
	    		case HORIZONTAL:
	    			if( trailing)
	    				size = start_size + (start_pos - p.x);
	    			else
	    				size = start_size - (start_pos - p.x);
	    				
	        		size = MUtil.clip( 
	        				min_size , 
	        				size,
	        				ResizeContainerPanel.this.getWidth()-min_stretch - reserved);
	        		break;
	    		case VERTICAL:
	    			if( trailing)
	    				size = start_size + (start_pos - p.y);
	    			else
	    				size = start_size - (start_pos - p.y);
	        		size = start_size - (start_pos - p.y);
	        		size = MUtil.clip( 
	        				min_size , 
	        				size,
	        				ResizeContainerPanel.this.getHeight()-min_stretch - reserved);
	        		break;
	    		}
	    		
	    		resetLayout();
	    		
	    		super.mouseDragged(e);
	    	}
	    }
	}
}
