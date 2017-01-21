package spirite.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
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

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.WarningType;

public class UIUtil {
	private static final Color c1 = new Color( 168,168,168);
	private static final Color c2 = new Color( 192,192,192);
	/** Draws the grid background that goes behind an image to show transparency. */
	public static void drawTransparencyBG( Graphics g, Rectangle rect) {
		Rectangle bounds;
		if( rect == null)
			bounds = g.getClipBounds();
		else
			bounds = rect;
		
		if( bounds.isEmpty())
			return;
		
		int size = 4;
		
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
	public static void constructMenu( JComponent root, String menuScheme[][], ActionListener listener) {
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
    		if( mnemonic != '\0')
    			new_node.setMnemonic(0);
    		

    		if( menuScheme[i].length > 1 && menuScheme[i][1] instanceof String) {
    			new_node.setActionCommand((String)menuScheme[i][1]);
    			
    			if( listener != null)
    				new_node.addActionListener(  listener);
    		}
    		
    		if( menuScheme[i].length > 2 && menuScheme[i][2] instanceof String)
    			new_node.setIcon(Globals.getIcon(menuScheme[i][2]));
    		
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
	

	public static class SliderPanel extends JPanel {
		private float value = 0.0f;
		private float min = 0.0f;
		private float max = 1.0f;
		private String label = "";
		protected boolean hardCapped = true;
		
		public SliderPanel() {
			MouseAdapter adapter = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					setValue( widthToValue( e.getX() / (float)getWidth()));
					super.mousePressed(e);
				}
				@Override
				public void mouseDragged(MouseEvent e) {
					setValue( widthToValue( e.getX() / (float)getWidth()));
					super.mouseDragged(e);
				}
			};

			addMouseListener( adapter);
			addMouseMotionListener( adapter);
		}
		
		public void onValueChanged( float newValue) {
			repaint();
		}
		
		// :::: Getters/Setters
		public float getValue() {
			return value;
		}

		public void setValue(float value) {
			if( hardCapped)
				value = Math.min( max, Math.max(min, value));
			if( this.value != value) {
				this.value = value;
				onValueChanged( value);
			}
		}

		public float getMin() {
			return min;
		}

		public void setMin(float min) {
			this.min = min;
			if( hardCapped)
				value = Math.max(min, value);
		}

		public float getMax() {
			return max;
		}

		public void setMax(float max) {
			this.max = max;
			if( hardCapped)
				value = Math.max(min, value);
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			if( label == null) label = "";
			this.label = label;
		}
		
		// :::: Determine how it's drawn
		protected String valueAsString(float value) {
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(2);
			df.setMinimumFractionDigits(2);
			return df.format(value);
		}
		
		protected float valueToWidth(float value) {
			return Math.max(0.0f, Math.min(1.0f, (value - min) / (max - min)));
		}
		protected float widthToValue( float portion) {
			return portion * (max-min)  + min;
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;

			Paint oldP = g2.getPaint();
			Paint newP = new GradientPaint( 0,0, new Color(64,64,64), getWidth(), 0, new Color( 128,128,128));
			g2.setPaint(newP);
			g2.fillRect(0, 0, getWidth(), getHeight());

			newP = new GradientPaint( 0,0, new Color(120,120,190), 0, getHeight(), new Color( 90,90,160));
			g2.setPaint(newP);
			g2.fillRect( 0, 0, Math.round(getWidth()*valueToWidth(value)), getHeight());
			
			g2.setColor( new Color( 222,222,222));
			
			UIUtil.drawStringCenter(g2, label + valueAsString(value), getBounds());

			g2.setPaint(oldP);
			g2.setColor( Color.BLACK);
			g2.drawRect(0, 0, getWidth()-1, getHeight()-1);
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
