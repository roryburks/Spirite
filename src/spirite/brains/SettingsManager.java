package spirite.brains;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import spirite.MDebug;
import spirite.MDebug.ErrorType;

/***
 * SettingsManager will handle all the various properties and settings
 * that need to be remembered for ease of use reasons and aren't remembered
 * by Swing's default components.
 * 
 * @author Rory Burks
 *
 */
public class SettingsManager {
    private final Preferences prefs;
	private File opennedFile = null;
	private List<String> paletteList = null;
	
	public SettingsManager() {
        prefs = Preferences.userNodeForPackage(spirite.Main.class);
        
	}
	
	// Palette Saving/Loading: 
	//
	/** used by PaletteManager to get the raw data corresponding to a palette. */
	byte[] getRawPalette( String name) {
		List<String> names = getStoredPalettes();
		
		if( !names.contains(name))
			return null;
		
		return prefs.getByteArray("palette."+name, null);
	}
	void saveRawPalette( String name, byte[] raw) {
		if( paletteList == null)
			loadPaletteList();
		
		if( raw.length > Preferences.MAX_VALUE_LENGTH * 3 / 4) {
			MDebug.handleError(ErrorType.ALLOCATION_FAILED, null, "Preference size too large.");
			return;
		}
		
		if( !paletteList.contains(name)) {
			paletteList.add(name);
			prefs.put("PaletteList", String.join("\0", paletteList));
		}
		
		prefs.putByteArray("palette."+name, raw);
	}
	private void loadPaletteList() {
		String raw = prefs.get("PaletteList","");
		
		String entries[] = raw == "" ? new String[0] : raw.split("\0");
		paletteList = new ArrayList<String>(Arrays.asList(entries));
	}
	public List<String> getStoredPalettes() {
		if( paletteList == null)
			loadPaletteList();
		
		return paletteList;
	}
	
	public File getOpennedFile() {
		return opennedFile;
	}
	public void setOpennedFile( File file) {
		opennedFile = file;
	}
}
