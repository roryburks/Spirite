package spirite;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;

public class MUtil {

	// :::: Math Functions
	public static int packInt( int low, int high) {
		return (low&0xffff) | ((high&0xffff) << 16);
	}
	
	public static int low16( int i) {
		return i & 0xffff;
	}
	
	public static int high16( int i) {
		return i >>> 16;
	}
	
	public static float cycle( float start, float end, float t) {
		float diff = end - start;
		
		return ((t - start) % diff + diff) % diff + start;
	}
	
	// :::: String
	
	// :::: Other
	public static boolean coordInImage( int x, int y, BufferedImage image) {
		if( image == null) return false;
		
		if( x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) 
			return false;
		
		return true;
	}
	
	/***
	 * I found the behavior of JFormattedTextField to be inadequate so I'm implementing
	 * my own and centralizing it here.
	 *
	 */
	public static class MTextFieldNumber extends JTextField implements DocumentListener {
		private int max = Integer.MAX_VALUE;
		private int min = Integer.MIN_VALUE;
		
		private Color default_color = null;
		private Color bad_color = Color.RED;
		
		private MTFNDocument mdocument;
		
		private boolean valid = true;
		
		public MTextFieldNumber() {
			mdocument = new MTFNDocument();
			this.setDocument(mdocument);
			this.getDocument().addDocumentListener(this);
		}
		
		
		/***
		 * Document that behaves identically to PlainDocument, but doesn't allow
		 * you to enter non-number characters.
		 */
		public static class MTFNDocument extends PlainDocument {
			private boolean allows_negative = true;
			private boolean allows_floats = false;
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
