package spirite.brains;

import java.io.File;

/***
 * SettingsManager will handle all the various properties and settings
 * that need to be remembered for ease of use reasons and aren't remembered
 * by Swing's default components.
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
