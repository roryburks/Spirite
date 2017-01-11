package spirite.ui;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class UIUtil {
    public static final int MAX_LEVEL = 10;
    private static int _imCountLevel( String s){
    	int r = 0;
    	while( s.charAt(r) == '.')
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
	 *	 means it goes inside the last zero-dot item, two dots means it should go in the last
	 *	 one-dot item, etc.  !!Note, there should never be any reason to skip dots and doing 
	 *   so will probably break it.
	 * The & character before a letter represents the Mnemonic key that should be associated
	 *   with it.
	 * If the title is simply - (perhaps after some .'s representing its depth), then it is
	 *   will simply construct a separator and will ignore the last two elements in the
	 *   array (in fact they don't need to exist).
     * @param root the Component (be it JPopupMenu or JMenuBar or other) to construct the menu into
     * @param menuScheme See Above
     * @param listener the listener which will be sent the Action when an item is selected
     */
	public static void constructMenu( JComponent root, String menuScheme[][], ActionListener listener) {
		JMenuItem new_node;
    	JMenuItem[] active_root_tree = new JMenuItem[MAX_LEVEL];
    	
    	// If root is a JMenuBar, make sure that all top-level nodes are JMenu's
    	//	instead of JMenuItems (otherwise they glitch out the bar)
    	boolean baseMenu = (root instanceof JMenuBar);
    	
    	// Atempt to construct menu from parsed data in menu_scheme
		// !!!! TODO: note, there are very few sanity checks in here for now
//    	int active_level = 0;
    	for( int i = 0; i < menuScheme.length; ++i) {
    		String title = (String)menuScheme[i][0];
    		char mnemonic = '\0';
    		
    		// Determine the depth of the node and crop off the extra .'s
    		int level =_imCountLevel(title);
    		title = title.substring(level);
    		
    		// If it's - that means it's a separator
    		if( title.equals("-")) {
    			((JMenu)active_root_tree[level-1]).addSeparator();
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
    		if( (level != 0 || !baseMenu) && (i+1 == menuScheme.length || _imCountLevel((String)menuScheme[i+1][0]) <= level)) {
    			new_node = new JMenuItem( title);
    		}
    		else {
    			new_node = new JMenu( title);
    		}
    		if( mnemonic != '\0')
    			new_node.setMnemonic(0);
    		

    		if( menuScheme[i][1] != null) {
    			new_node.setActionCommand((String)menuScheme[i][1]);
    			
    			if( listener != null)
    				new_node.addActionListener(  listener);
    		}
    		
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
