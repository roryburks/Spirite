// Rory Burks

package spirite.dialogs;

import java.awt.Color;
import java.io.File;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;

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
	public static Color pickColor() { return pickColor(null);}
    public static Color pickColor(Color defaultColor) {
        JColorChooser jcp = new JColorChooser();
        jcp.setColor(defaultColor);
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
    
    public static boolean performNewLayerDialog( ImageWorkspace workspace) {

		NewLayerDPanel panel = new NewLayerDPanel(master);
		
		int response = JOptionPane.showConfirmDialog(null,
			panel,
			"New Image",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			Globals.getIcon("new_layer"));
		
		if( response == JOptionPane.OK_OPTION) {
			int w = panel.getValueWidth();
			int h = panel.getValueHeight();
			String name = panel.getValueName();
			//String type = panel.getValueType();
			Color c = panel.getValueColor();
			

			// Add the new layer contextually according to the selected Node
			GroupTree.Node context = workspace.getSelectedNode();
			
			workspace.addNewLayer(context, w, h, name, c);
			return true;
		}
		return false;
    }
}
