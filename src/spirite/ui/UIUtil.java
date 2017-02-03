package spirite.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.WarningType;

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
    private static int _imCountLevel( String s){
    	int r = 0;
    	while( r < s.length() && s.charAt(r) == '.')
    		r++;
    	return Math.min(r, MAX_LEVEL);
    }
    
    /***
	 * Constructs a menu from an array of objects corresponding to the menu scheme as such:
	 * 
	 * The menuScheme is an n-by-3 array 
	 * -The first string represents the menu title as well as its structure (see below)
	 * -The second string represents the actionCommand
	 * -The third string represents the icon associated with it (using Globals.getIcon)
	 * 
	 * Each dot before the name indicates the level it should be in.  For example one dot
	 *   means it goes inside the last zero-dot item, two dots means it should go in the last
	 *   one-dot item, etc.  Note: if you skip a certain level of dot's (eg: going from
	 *   two dots to four dots), then the extra dots will be ignored, possibly resulting
	 *   in unexpected menu form.
	 * The & character before a letter represents the Mnemonic key that should be associated
	 *   with it.
	 * If the title is simply - (perhaps after some .'s representing its depth), then it is
	 *   will simply construct a separator and will ignore the last two elements in the
	 *   array (in fact they don't need to exist).
	 * @param root the Component (be it JPopupMenu or JMenuBar or other) to construct the menu into
	 * @param menuScheme See Above
	 * @param listener the listener which will be sent the Action when an item is selected
     */
	public static void constructMenu( JComponent root, Object menuScheme[][], ActionListener listener) {
		JMenuItem new_node;
    	JMenuItem[] active_root_tree = new JMenuItem[MAX_LEVEL];
    	
    	// If root is a JMenuBar, make sure that all top-level nodes are JMenu's
    	//	instead of JMenuItems (otherwise they glitch out the bar)
    	boolean isMenuBar = (root instanceof JMenuBar);
    	boolean isPopupMenu = (root instanceof JPopupMenu);
    	
    	// Atempt to construct menu from parsed data in menu_scheme
    	int active_level = 0;
    	for( int i = 0; i < menuScheme.length; ++i) {
    		if( menuScheme[i].length == 0 || !(menuScheme[i][0] instanceof String))
    			continue;
    		
    		String title = (String)menuScheme[i][0];
    		char mnemonic = '\0';
    		
    		// Determine the depth of the node and crop off the extra .'s
    		int level =_imCountLevel(title);
    		title = title.substring(level);
    		
    		if( level > active_level ) {
    			MDebug.handleWarning(WarningType.INITIALIZATION, null, "Bad Menu Scheme.");
    			level = active_level;
    		}
    		active_level = level+1;
    		
    		// If it's - that means it's a separator
    		if( title.equals("-")) {
    			if( level == 0 ) {
    				if( isPopupMenu)
    					((JPopupMenu)root).addSeparator();
    			}
    			else
    				((JMenu)active_root_tree[level-1]).addSeparator();
    			
    			active_level--;
    			continue;
    		}
    		
    		// Detect the Mnemonic
    		int mind = title.indexOf('&');
    		if( mind != -1 && mind != title.length()-1) {
    			mnemonic = title.charAt(mind+1);
    			title = title.substring(0, mind) + title.substring(mind+1);
    		}
    		
    		
    		// Determine if it needs to be a Menu (which contains other options nested in it)
    		//	or a plain MenuItem (which doesn't)
    		if( (level != 0 || !isMenuBar) && (i+1 == menuScheme.length || _imCountLevel((String)menuScheme[i+1][0]) <= level)) {
    			new_node = new JMenuItem( title);
    		}
    		else {
    			new_node = new JMenu( title);
    		}
    		if( mnemonic != '\0') {
    			new_node.setMnemonic(mnemonic);
    		}
    		

    		if( menuScheme[i].length > 1 && menuScheme[i][1] instanceof String) {
    			new_node.setActionCommand((String)menuScheme[i][1]);
    			
    			if( listener != null)
    				new_node.addActionListener(  listener);
    		}
    		
    		if( menuScheme[i].length > 2 && menuScheme[i][2] instanceof String)
    			new_node.setIcon(Globals.getIcon((String)menuScheme[i][2]));
    		
    		// Add the MenuItem into the appropriate context
    		if( level == 0) {
    			root.add( new_node);
    		}
    		else {
    			active_root_tree[level-1].add(new_node);
    		}
    		active_root_tree[ level] = new_node;
    	}
	}
}
