package spirite.pc.ui.omni;


import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import spirite.base.util.MUtil;
import spirite.gui.hybrid.SPanel;
import spirite.hybrid.Globals;

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
public class OmniResizeContainerPanel extends SPanel{
	protected int min_stretch = 0;
	protected final List<ResizeBar> leadingBars;
	protected final List<ResizeBar> trailingBars;
	protected ContainerOrientation cOrientation;
	protected JComponent stretchComponent;
	protected int barSize = 8;

	public static final int LEADING = Integer.MAX_VALUE;
	public static final int TRAILING = Integer.MIN_VALUE;
	
	
	public enum ContainerOrientation {
		HORIZONTAL,
		VERTICAL
	}
	
	public OmniResizeContainerPanel(JComponent component, ContainerOrientation orientation) {
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

	public int getBarSize() {return barSize;}
	public void setBarSize( int size) {
		size = Math.max(0,  size);
		if( barSize != size) {
			barSize = size;
			resetLayout();
		}
	}
	
	
	public void setPanelComponent( ResizeBar panel, JComponent component) {
		
	}
	
	protected void resetLayout() {
		this.removeAll();
        GroupLayout layout = new GroupLayout(this);

        GroupLayout.Group stretch = layout.createSequentialGroup();
        GroupLayout.Group noStretch = layout.createParallelGroup(Alignment.LEADING);
        
        for( ResizeBar bar : leadingBars) {
        	if( bar.componentVisible)
        		stretch.addComponent(bar.component, bar.getResizeSize(),bar.getResizeSize(),bar.getResizeSize());
        	stretch.addComponent(bar, barSize, barSize, barSize);
        }
        if( stretchComponent != null) {
        	stretch.addComponent(stretchComponent);
        }
        for( ResizeBar bar : trailingBars) {
        	stretch.addComponent(bar, barSize, barSize, barSize);
        	if( bar.componentVisible)
        		stretch.addComponent(bar.component, bar.getResizeSize(),bar.getResizeSize(),bar.getResizeSize());
        }

        for( ResizeBar bar : leadingBars) {
        	if( bar.componentVisible)
        		noStretch.addComponent(bar.component);
        	noStretch.addComponent(bar);
        }
        for( ResizeBar bar : trailingBars) {
        	if( bar.componentVisible)
        		noStretch.addComponent(bar.component);
        	noStretch.addComponent(bar);
        }
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

		validate();
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
		
		if( position < 0) {
			if( -position >= trailingBars.size()) {
				ret = -trailingBars.size()-1;
				trailingBars.add(new ResizeBar(default_size,min_size, component, true));
			}
			else {
				trailingBars.add(-position-1,new ResizeBar(default_size,min_size,component,true));
			}
		}
		else {
			if( position >= leadingBars.size()) {
				ret = leadingBars.size()+1;
				leadingBars.add(new ResizeBar(default_size,min_size, component,false));
			}
			else {
				leadingBars.add(position-1,new ResizeBar(default_size,min_size,component,false));
			}
		}


		resetLayout();
		return ret;
	}
	
	public void setPanelVisible( int index, boolean visible) {
		if( index == 0) return;
		
		if( index < 0) {
			if( -index >= leadingBars.size()) return;
			leadingBars.get(-index).componentVisible = visible;
		}
		else {
			if( index >= trailingBars.size()) return;
			trailingBars.get(index).componentVisible = visible;
		}
		resetLayout();
	}
	
	public void removePanel( int index) {
	}
	public void removeAllPanels() {
	}
	
	/** List places the West/North components then the East/South ones. */
	public List<ResizeBar> getPanels() {
		return null;
	}
	
	
	public class ResizeBar extends SPanel {
		protected final ResizeBarAdapter adapter;
		protected int size;
		protected int min_size = 50;
		protected JComponent component;
		protected boolean trailing;	
		boolean componentVisible = true;

		protected final Icon iconExpanded;
		protected final Icon iconUnexpanded;
		protected final Color BAR_LINE_COLOR = new Color(190,190,190);
		protected final JButton btnExpand = new JButton();
		protected final SPanel pullBar = new SPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				int depth = 0;
				int breadth = 0;
				g.setColor(BAR_LINE_COLOR);
				switch( cOrientation) {
				case HORIZONTAL:
					depth = getWidth();
					breadth = getHeight();
					g.drawLine( depth/2-2, 10, depth/2-2, breadth-10);
					g.drawLine( depth/2, 5, depth/2, breadth-5);
					g.drawLine( depth/2+2, 10, depth/2+2, breadth-10);
					break;
				case VERTICAL:
					depth = getHeight();
					breadth = getWidth();
					g.drawLine( 10, depth/2-2, breadth-10, depth/2-2);
					g.drawLine( 5, depth/2, breadth-5, depth/2);
					g.drawLine( 10, depth/2+2,  breadth-10, depth/2+2);
					break;
				}
			}
		};
	
		
		
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
		protected ResizeBar(
				int default_size,
				int min_size, 
				JComponent component,
				boolean trailing) 
		{
			this.min_size = min_size;
			this.size = default_size;
			this.component = component;
			this.trailing = trailing;

			// :::: Determine which direction the arrow should be pointing
			//	based on both the local and global orientations as well
			//	as the expanded state
			if(cOrientation == ContainerOrientation.VERTICAL) {
				this.iconExpanded = Globals.getIcon("icon.arrowE");
				this.iconUnexpanded = (trailing)
						?Globals.getIcon("icon.arrowS")
						:Globals.getIcon("icon.arrowN");
			}
			else {
				this.iconExpanded = Globals.getIcon("icon.arrowS");
				this.iconUnexpanded = (trailing)
						?Globals.getIcon("icon.arrowE")
						:Globals.getIcon("icon.arrowW");
			}

			this.adapter = new ResizeBarAdapter();
			this.addMouseListener(adapter);
			this.addMouseMotionListener(adapter);

			btnExpand.setIcon(iconExpanded);
			btnExpand.setBorderPainted(false); 
			btnExpand.setContentAreaFilled(false); 
	        btnExpand.setFocusPainted(false); 
	        btnExpand.setOpaque(false);
			
			GroupLayout layout = new GroupLayout( this);
			
			switch( cOrientation) {
			case HORIZONTAL: 
				layout.setHorizontalGroup(layout.createParallelGroup()
					.addComponent(btnExpand, barSize, barSize, barSize)
					.addComponent(pullBar, barSize, barSize, barSize)
				);
				layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(btnExpand, 12, 12, 12)
					.addComponent(pullBar)
				);
				pullBar.setCursor(new Cursor( Cursor.E_RESIZE_CURSOR));
				break;
			case VERTICAL:
				layout.setVerticalGroup(layout.createParallelGroup()
					.addComponent(btnExpand, barSize, barSize, barSize)
					.addComponent(pullBar, barSize, barSize, barSize)
				);
				layout.setHorizontalGroup(layout.createSequentialGroup()
					.addComponent(btnExpand, 12, 12, 12)
					.addComponent(pullBar)
				);
				pullBar.setCursor(new Cursor( Cursor.N_RESIZE_CURSOR));
				break;
			}
			this.setLayout(layout);
			
			initBindings();
		}
		
		public void initBindings() {
			btnExpand.addActionListener( new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					componentVisible = !componentVisible;
					btnExpand.setIcon((componentVisible)?iconExpanded:iconUnexpanded);
					resetLayout();
				}
			});
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
		
		
	    protected class ResizeBarAdapter extends MouseAdapter {
	    	int start_pos;
	    	int start_size;
	    	
	    	int reserved;
	    	
	    	public ResizeBarAdapter( ) {}
	    	
	    	@Override
	    	public void mousePressed(MouseEvent e) {
	    		Point p = SwingUtilities.convertPoint( 
	    				e.getComponent(),
	    				e.getPoint(), 
	    				OmniResizeContainerPanel.this);

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
	    				OmniResizeContainerPanel.this);

	    		
	    		switch( cOrientation) {
	    		case HORIZONTAL:
	    			if( trailing)
	    				size = start_size + (start_pos - p.x);
	    			else
	    				size = start_size - (start_pos - p.x);
	    				
	        		size = MUtil.clip( 
	        				min_size , 
	        				size,
	        				OmniResizeContainerPanel.this.getWidth()-min_stretch - reserved);
	        		break;
	    		case VERTICAL:
	    			if(trailing)
	    				size = start_size + (start_pos - p.y);
	    			else
	    				size = start_size - (start_pos - p.y);
	        		size = MUtil.clip( 
	        				min_size , 
	        				size,
	        				OmniResizeContainerPanel.this.getHeight()-min_stretch - reserved);
	        		break;
	    		}
	    		
	    		resetLayout();
	    		
	    		super.mouseDragged(e);
	    	}
	    }
	}

	private final ORCPDnDManager dnd = new ORCPDnDManager();
	protected class ORCPDnDManager extends DropTarget 
		implements DragGestureListener, DragSourceListener 
	{
		private final DragSource dragSource = DragSource.getDefaultDragSource();
		List<DragGestureRecognizer> recognizers = new ArrayList<>();
		
		ORCPDnDManager() {
		}
		
		void addDropSource(Component component) {
			recognizers.add(dragSource.createDefaultDragGestureRecognizer(
					component, DnDConstants.ACTION_COPY_OR_MOVE, this));
		}
		void clearAllSources() {
			for( DragGestureRecognizer re : recognizers)
				re.setComponent(null);
			recognizers.clear();
		}

		@Override
		public void dragDropEnd(DragSourceDropEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dragEnter(DragSourceDragEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dragExit(DragSourceEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dragOver(DragSourceDragEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dropActionChanged(DragSourceDragEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dragGestureRecognized(DragGestureEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	}
}