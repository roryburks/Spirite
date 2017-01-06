package spirite.brains;

import java.io.File;

/***
 * Handles all the various settings;
 * 
 * @author Rory Burks
 *
 */
public class SettingsManager {
	private File opennedFile = null;
	
	public File getOpennedFile() {
		return opennedFile;
	}
	public void setOpennedFile( File file) {
		opennedFile = file;
	}
}
