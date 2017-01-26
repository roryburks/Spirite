// Rory Burks

package spirite.dialogs;

import java.awt.Color;
import java.io.File;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;

/***
 * A static centalized namespace for calling the various Dialogs.
 *
 */
public class Dialogs {
	private final MasterControl master;
	
	public Dialogs( MasterControl masterc) {
		this.master = masterc;
	}

    /**
     * Pops up the color picker dialog and queries it for a color
     * @return The color picked or null if they canceled
     */
	public Color pickColor() { return pickColor(null);}
    public Color pickColor(Color defaultColor) {
        JColorChooser jcp = new JColorChooser();
        jcp.setColor(defaultColor);
        int response = JOptionPane.showConfirmDialog(null, jcp, "Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            return jcp.getColor();
        }

        return null;
    }
    
    public File pickFileOpen() {
    	JFileChooser fc = new JFileChooser();
    	
    	File f = master.getSettingsManager().getOpenFilePath();
    	
    	for( FileFilter filter : fc.getChoosableFileFilters()) {
    		fc.removeChoosableFileFilter(filter);
    	}
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("Supported Image Files", "jpg", "jpeg", "png", "bmp", "sif", "sif~"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("Spirite Workspace File", "sif"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG File", "jpg", "jpeg"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG File", "png"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("Bitmap File", "bmp"));
    	fc.addChoosableFileFilter( fc.getAcceptAllFileFilter());
    	
    	fc.setCurrentDirectory( f);
    	int val = fc.showOpenDialog(null);
    	
    	if( val == JFileChooser.APPROVE_OPTION)
    		return fc.getSelectedFile();
    	return null;
    }
    

    public File pickFileSave() {
    	JFileChooser fc = new JFileChooser();
    	
    	File f = master.getSettingsManager().getWorkspaceFilePath();

    	for( FileFilter filter : fc.getChoosableFileFilters()) {
    		fc.removeChoosableFileFilter(filter);
    	}
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("Spirite Workspace File", "sif"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("WARNING: To save as Image File use Export Option", "\0"));
    	fc.addChoosableFileFilter( fc.getAcceptAllFileFilter());
    	

		fc.setCurrentDirectory( f);
		fc.setSelectedFile(f);
    		
    	int val = fc.showSaveDialog(null);
    	
    	if( val == JFileChooser.APPROVE_OPTION)
    		return fc.getSelectedFile();
    	return null;
    	
    }
    

    public File pickFileExport() {
    	JFileChooser fc = new JFileChooser();
    	
    	File f = master.getSettingsManager().getImageFilePath();

    	for( FileFilter filter : fc.getChoosableFileFilters()) {
    		fc.removeChoosableFileFilter(filter);
    	}
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("Supported Image Files", "jpg", "jpeg", "png", "bmp", "sif", "sif~"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("Spirite Workspace File", "sif"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("JPEG File", "jpg", "jpeg"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG File", "png"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("Bitmap File", "bmp"));
    	fc.addChoosableFileFilter( fc.getAcceptAllFileFilter());
    	
		fc.setCurrentDirectory( f);
		fc.setSelectedFile(f);
    		
    	int val = fc.showSaveDialog(null);
    	
    	if( val == JFileChooser.APPROVE_OPTION)
    		return fc.getSelectedFile();
    	return null;
    }
    
    public boolean performNewLayerDialog( ImageWorkspace workspace) {

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
			
			workspace.addNewSimpleLayer(context, w, h, name, c);
			return true;
		}
		return false;
    }
    

    /** Prompts for a new image dialog and then performs it. */
    public void promptNewImage() {     
        NewImagePanel panel = new NewImagePanel(master);

        int response = JOptionPane.showConfirmDialog(null,
                panel,
                "New Image",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            master.newWorkspace(panel.getValueWidth(), panel.getValueHeight(), panel.getValueColor(), true);
            master.getCurrentWorkspace().finishBuilding();
        }
    }                                            

    /**     */
    public void promptDebugColor() {
        // TODO DEBUG
        JColorChooser jcp = new JColorChooser();
        int response = JOptionPane.showConfirmDialog(null, jcp, "Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            master.getPaletteManager().setActiveColor(0, jcp.getColor());
        }
    }
}
