package spirite.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class UIUtil {

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
			Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
			while( e.hasMoreElements()) {
				DefaultMutableTreeNode node = e.nextElement();
				
				tree.expandPath( new TreePath(node.getPath() ));
			}
		} catch ( ClassCastException e) {
			
		}
	}
	
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
	 *   means it goes inside the last zero-dot item, two dots means it should go in the last
	 *   one-dot item, etc.  !!Note, there should never be any reason to skip dots and doing 
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
    	boolean isMenuBar = (root instanceof JMenuBar);
    	boolean isPopupMenu = (root instanceof JPopupMenu);
    	
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
    			if( level == 0 ) {
    				if( isPopupMenu)
    					((JPopupMenu)root).addSeparator();
    			}
    			else
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
    		if( (level != 0 || !isMenuBar) && (i+1 == menuScheme.length || _imCountLevel((String)menuScheme[i+1][0]) <= level)) {
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
	
	

    /***
	 * I found the behavior of JFormattedTextField to be inadequate so I'm implementing
	 * my own and centralizing it here.
	 *
	 */
	public static class MTextFieldNumber extends JTextField implements DocumentListener {
		private static final long serialVersionUID = 1L;
		private int max = Integer.MAX_VALUE;
		private int min = Integer.MIN_VALUE;
		
		private Color default_color = null;
		private Color bad_color = Color.RED;
		
		private MTFNDocument mdocument;
		
		private boolean valid = true;
		

		private boolean allows_negative = true;
		private boolean allows_floats = false;
		
		public MTextFieldNumber() {
			init();
		}

		public MTextFieldNumber(boolean allowsNegatives, boolean allowsFloats) {
			this.allows_floats = allowsFloats;
			this.allows_negative = allowsNegatives;
			init();
		}
		private void init() {
			mdocument = new MTFNDocument();
			this.setDocument(mdocument);
			this.getDocument().addDocumentListener(this);
			
		}

		
		/***
		 * Document that behaves identically to PlainDocument, but doesn't allow
		 * you to enter non-number characters.
		 */
		public class MTFNDocument extends PlainDocument {
			private static final long serialVersionUID = 1L;
			@Override
			public void insertString( int offset, String str, AttributeSet a) throws BadLocationException {
				
				// Only allow the user to enter part of a string if it's digits.  Possibly with 
				//	- and .  But only one - at the beginning (and only if negatives are allowed)
				//	and only one . (if floats are allowed)
				if( !str.matches("^-?[0-9]*\\.?[0-9]*$") 
						|| (str.startsWith("-") && (offset !=0 || !allows_negative))
						|| (str.contains(".") && (this.getText(0, this.getLength()).contains(".") || !allows_floats)))
					Toolkit.getDefaultToolkit().beep();
				else
					super.insertString(offset, str, a);
			}
		}
		
		
		// :::: API
		public void setMinMax( int min, int max) {
			this.min = min;
			this.max = max;
		}
		public int getNumber() {
			try {
				int i = Integer.parseInt( this.getText());
				return i;
			}
			catch( NumberFormatException e) {
				return 0;
			}
		}
		public boolean getValid() {
			return valid;
		}
		
		// :::: Out of bounds check
		
		// Turn the text field red if the data is out of bounds
		private void checkIfOOB() {
			String text = this.getText();
			
			try {
				int i = Integer.parseInt(text);
				if( text == "") i = 0;
				
				if( i < min || i > max)
					this.outOfBounds();
				else
					this.inBounds();
			}catch( Exception e) {}
		}
		private void outOfBounds() {
			default_color = this.getBackground();
			this.setBackground(bad_color);
			valid = false;
			
		}
		private void inBounds() {
			if( !valid) {
				this.setBackground(default_color);
				valid = true;
			}
		}
		
	
	
		// :::: DocumentListener
		@Override public void changedUpdate(DocumentEvent e) {}
		
		@Override public void removeUpdate(DocumentEvent de) {
			checkIfOOB();
		}
		
		@Override
		public synchronized void insertUpdate(DocumentEvent de) {
			checkIfOOB();
		}
	
	}
}
