// Rory Burks

package spirite.dialogs;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.CommandExecuter;
import spirite.dialogs.NewLayerDPanel.NewLayerHelper;
import spirite.graphics.gl.TestGLDiag;
import spirite.image_data.ImageWorkspace;

/***
 * A static centalized namespace for calling the various Dialogs.
 *
 */
public class Dialogs 
	implements CommandExecuter
{
	private final MasterControl master;
	
	public enum DialogType {
		HOTKEY,
		TABLET,
		DEBUG,
		DBGL,
//		PICK_COLOR,
	}
	
	public Dialogs( MasterControl masterc) {
		this.master = masterc;
	}
	
	public boolean openDialog( String string) {
		for( DialogType type : DialogType.values()) {
			if( type.name().equals(string)) {
				openDialog( type);
				return true;
			}
		}
		return false;
	}
	
	public void openDialog( DialogType type) {
		switch( type) {
		case HOTKEY:{
			HotkeyDialog dialog = new HotkeyDialog(master);
			dialog.setVisible(true);
			break;}
		case TABLET:{
			TabletDialog dialog = new TabletDialog(master);
			dialog.setVisible(true);
			break;}
		case DEBUG: {
			DebugDialog dialog = new DebugDialog(master);
			dialog.setVisible(true);
			break;}
		case DBGL: {
			TestGLDiag dialog = new TestGLDiag();
			dialog.setVisible(true);
			break;}
		}
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
    	
    	if( val == JFileChooser.APPROVE_OPTION) {
    		File ret = fc.getSelectedFile();
    		
    		if( ret.getName().endsWith(".sif") || ret.getName().endsWith(".sif~"))
    			master.getSettingsManager().setWorkspaceFilePath(ret);
    		else
    			master.getSettingsManager().setImageFilePath(ret);
    		return ret;
    	}
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

    	if( val == JFileChooser.APPROVE_OPTION) {
    		File ret = fc.getSelectedFile();
    		
    		if( ret.getName().endsWith(".sif") || ret.getName().endsWith(".sif~"))
    			master.getSettingsManager().setWorkspaceFilePath(ret);
    		else
    			master.getSettingsManager().setImageFilePath(ret);
    		return ret;
    	}
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

    	if( val == JFileChooser.APPROVE_OPTION) {
    		File ret = fc.getSelectedFile();
    		
    		if( ret.getName().endsWith(".sif") || ret.getName().endsWith(".sif~"))
    			master.getSettingsManager().setWorkspaceFilePath(ret);
    		else
    			master.getSettingsManager().setImageFilePath(ret);
    		return ret;
    	}
    	return null;
    }
    
    public File pickAAFExport() {
    	JFileChooser fc = new JFileChooser();
    	
    	File f = master.getSettingsManager().getAAFFilePath();

    	for( FileFilter filter : fc.getChoosableFileFilters()) {
    		fc.removeChoosableFileFilter(filter);
    	}
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("AAF File", "aaf"));
    	fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG File", "png"));
    	fc.addChoosableFileFilter( fc.getAcceptAllFileFilter());
    	
		fc.setCurrentDirectory( f);
		fc.setSelectedFile(f);
    		
    	int val = fc.showSaveDialog(null);
    	
    	if( val == JFileChooser.APPROVE_OPTION) {
    		master.getSettingsManager().setAAFFilePath(fc.getSelectedFile());
    		return fc.getSelectedFile();
    	}
    	return null;
    }
    
    public NewLayerHelper callNewLayerDialog( ImageWorkspace workspace) {

		NewLayerDPanel panel = new NewLayerDPanel(master);
		
		int response = JOptionPane.showConfirmDialog(null,
			panel,
			"New Image",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE,
			Globals.getIcon("new_layer"));
		
		if( response == JOptionPane.OK_OPTION) {

			// Add the new layer contextually according to the selected Node
//			GroupTree.Node context = workspace.getSelectedNode();
//			
//			workspace.addNewSimpleLayer(context, w, h, name, c);
			return panel.getHelper();
		}
		return null;
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

    // :::: CommandExecuter
	@Override
	public List<String> getValidCommands() {
		DialogType[] dialogTypes= DialogType.values();
		List<String> list = new ArrayList<>(dialogTypes.length);
		
		for( DialogType type : dialogTypes) {
			list.add( type.name());
		}
		return list;
	}
	@Override public String getCommandDomain() {
		return "dialog";
	}

	@Override
	public boolean executeCommand(String commmand) {
		return openDialog(commmand);
	}
}
