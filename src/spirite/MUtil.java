package spirite;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

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
	
	public static boolean coordInImage( int x, int y, BufferedImage image) {
		if( image == null) return false;
		
		if( x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) 
			return false;
		
		return true;
	}
	
	// :::: UI Functions
	public static class MTextFieldNumber extends JTextField implements DocumentListener {
		private int max = Integer.MAX_VALUE;
		private int min = Integer.MIN_VALUE;
		
		private Color default_color = null;
		private Color bad_color = Color.RED;
		
		private boolean valid = true;
		
		public MTextFieldNumber() {
			this.getDocument().addDocumentListener(this);
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
		
		// ::::
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
		


		@Override public void changedUpdate(DocumentEvent e) {}
		
		@Override public void removeUpdate(DocumentEvent de) {
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
		
		@Override
		public synchronized void insertUpdate(DocumentEvent de) {
			
			String text = "";
			
			// Beep and revert insert if the user tries to enter a non-int
			int i;
			try {
				text = this.getText();
				i = Integer.parseInt( text);
				if( text == "") i = 0;
				
				if( i < min || i > max)
					this.outOfBounds();
				else
					this.inBounds();
			} catch( Exception e) {
				
				Toolkit.getDefaultToolkit().beep();
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						clip( de.getOffset(), de.getLength());
					}
				});
				
				return;
			} 
		}
		
		private synchronized void clip( int o, int l) {
			try {
				this.getDocument().remove(o, l);
				
				// Because even with the synchronized keyword the asynchronous nature of UI can
				//	cause things to slip through if you're mashing buttons, so this fail-safe
				//	exists (though it can inexplicably cause the data to be lost).
				String text = this.getText();
				
				if( text.matches(".*\\D.*")) {
					text.replaceAll("\\D", "");
					this.setText(text);
				}
				
			} catch (BadLocationException e1) {}
		}

	}
}
