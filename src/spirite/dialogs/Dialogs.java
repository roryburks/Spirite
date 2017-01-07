// Rory Burks

package spirite.dialogs;

import java.awt.Color;
import java.io.File;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import spirite.brains.MasterControl;

/***
 * A static centalized namespace for calling the various Dialogs.
 *
 */
public class Dialogs {
	// !!!! TODO VERY DEBUG
	static MasterControl master;
	public static void setMaster( MasterControl masterc) {
		master = masterc;
	}

    /**
     * Pops up the color picker dialog and queries it for a color
     * @return The color picked or null if they canceled
     */
    public static Color pickColor() {
        JColorChooser jcp = new JColorChooser();
        int response = JOptionPane.showConfirmDialog(null, jcp, "Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            return jcp.getColor();
        }

        return null;
    }
    
    public static File pickFileOpen() {
    	JFileChooser fc = new JFileChooser();
    	
    	File f = master.getSettingsManager().getOpennedFile();
    	
    	if( f == null)
    		f = new File("E:/");
    	
    	fc.setCurrentDirectory( f);
    	int val = fc.showOpenDialog(null);
    	
    	if( val == JFileChooser.APPROVE_OPTION)
    		return fc.getSelectedFile();
    	return null;
    }
    

    public static File pickFileSave() {
    	JFileChooser fc = new JFileChooser();

    	
    	File f = master.getSettingsManager().getOpennedFile();
    	
    	if( f == null)
        	fc.setCurrentDirectory( new File("E:/"));
    	else {
    		fc.setCurrentDirectory( f);
    		fc.setSelectedFile(f);
    	}
    		
    		
    	int val = fc.showSaveDialog(null);
    	
    	if( val == JFileChooser.APPROVE_OPTION)
    		return fc.getSelectedFile();
    	return null;
    	
    }
}
