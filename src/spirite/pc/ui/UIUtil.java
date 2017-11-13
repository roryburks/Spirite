package spirite.pc.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import spirite.base.util.MUtil;

public class UIUtil {
	private static final Color c1 = new Color( 168,168,168);
	private static final Color c2 = new Color( 192,192,192);
	
	/** Draws the grid background that goes behind an image to show transparency. */
	public static void drawTransparencyBG( Graphics g, Rectangle rect) {
		drawTransparencyBG( g, rect, 4);
	}
	public static void drawTransparencyBG( Graphics g, Rectangle rect, int size) {
		Rectangle bounds;
		if( rect == null)
			bounds = g.getClipBounds();
		else
			bounds = rect;
		
		if( bounds.isEmpty())
			return;
		
		for( int i = 0; i*size < bounds.width; ++i) {
			for( int j=0; j*size < bounds.height; ++j) {
				g.setColor(
						(((i+j)%2) == 1)? c1:c2);
				
				g.fillRect(bounds.x + i*size, bounds.y + j*size, size, size);
				
			}
		}
	}

	/***
	 * Draws the string centered in the given Rectangle (using the font already
	 *	set up in the Graphics)
	 */
	public static void drawStringCenter( Graphics g, String text, Rectangle rect) {
		FontMetrics fm = g.getFontMetrics();
		int dx = (rect.width - fm.stringWidth(text))/2;
		int dy = (rect.height - fm.getHeight())/2 + fm.getAscent();
		g.drawString(text, dx, dy);
	}
	
	/***
	 * Expands all nodes in the tree, assumes they are using 
	 * DefaultMutableTreeNode form.  If it doesn't, it won't work
	 */
	public static void expandAllNodes( JTree tree) {
		try {
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
			Enumeration<?> e =  root.depthFirstEnumeration();
			while( e.hasMoreElements()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
				
				tree.expandPath( new TreePath(node.getPath() ));
			}
		} catch ( ClassCastException e) {
			
		}
	}
	
	public static final int MAX_LEVEL = 10;
    public static int _imCountLevel( String s){
    	int r = 0;
    	while( r < s.length() && s.charAt(r) == '.')
    		r++;
    	return Math.min(r, MAX_LEVEL);
    }
    
    /**
     * ClickAdapter is an extention of MouseAdapter that has less discriminating
     * click detection than normal mouse listening.  (In normal Mouse Listening
     * a click is not detected if the cursor moves even a pixel from its starting
     * position, making it very difficult to click with touch/pen input).
     *
     */
    public static class ClickAdapter extends MouseAdapter {
    	int startx, starty;
    	@Override
    	public void mousePressed(MouseEvent e) {
    		super.mousePressed(e);
    		startx = e.getX();
    		starty = e.getY();
    	}
    	@Override
    	public void mouseReleased(MouseEvent e) {
    		super.mouseReleased(e);
    		
    		if( e.getComponent().getBounds().contains(e.getPoint())
    			&& MUtil.distance(startx, starty, e.getX(), e.getY()) < 4)
    		{
    			this.mouseClicked(e);
    		}
    	}
    }
    
    /***
	 * Called when an overlaying component (such as a GlassPane) eats a mouse event, but
	 * still wants the components bellow to receive it.
	 */
	public static void redispatchMouseEvent( Component reciever, Component container, MouseEvent evt) {
		Point p = SwingUtilities.convertPoint(reciever, evt.getPoint(), container);
		
		if( p.y < 0) { 
			// Not in component
		} else {
			Component toSend = 
					SwingUtilities.getDeepestComponentAt(container, p.x, p.y);
			if( toSend != null && toSend != reciever) {
				Point convertedPoint = SwingUtilities.convertPoint(container, p, toSend);
				toSend.dispatchEvent( new MouseEvent(
						toSend,
						evt.getID(),
						evt.getWhen(),
						evt.getModifiers(),
						convertedPoint.x,
						convertedPoint.y,
						evt.getClickCount(),
						evt.isPopupTrigger()
						));
			}
			else {
			}
		}
	}
	
	/** Exists purely to make use of lambdas to simplify code appearance. */
	public static AbstractAction buildAction(ActionPipe pipe ) {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pipe.actionPerformed(e);
			}
		};
	}
	public interface ActionPipe {
		public void actionPerformed( ActionEvent evt);
	}

	/** Loads up an Action Map */
	public static void buildActionMap(JComponent component, Map<KeyStroke, Action> actionMap) {
		for(Entry<KeyStroke,Action> entry : actionMap.entrySet()) {
			String id = entry.getKey().toString();
			component.getInputMap().put(entry.getKey(), id);
			component.getActionMap().put(id, entry.getValue());
		}
	}
}
