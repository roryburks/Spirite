package spirite.brains;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.File;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import mutil.Interpolation.CubicSplineInterpolator;
import spirite.MDebug;
import spirite.MDebug.ErrorType;

/***
 * SettingsManager will handle all the various properties and settings
 * that need to be remembered for ease of use reasons and aren't remembered
 * by Swing's default components.
 * 
 * It saves and load data persistantly using Java's Preferences package.
 * 
 * @author Rory Burks
 *
 */
public class SettingsManager {
    private final Preferences prefs;
	private List<String> paletteList = null;
	
	public SettingsManager() {
        prefs = Preferences.userNodeForPackage(spirite.Spirite.class);
	}
	
	/** If true, then the engine is able to use OpenGL functionality.  If false
	 * (either because the user set it to be false or because OpenGL couldn't 
	 * initialize properly), then the engine must fall back on basic AWT rendering.*/
	public boolean glMode() {
		return false;
	}
	
	// ==============
	// ==== Palette Saving/Loading: 
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

	// ====================
	// ==== Recent-Used FilePath Settings
	private boolean lastUsedWorkspace = true;
	private File workspaceFilePath;
	private File imageFilePath;
	private File aafFilePath;
	public File getWorkspaceFilePath() {
		if( workspaceFilePath == null)
	        workspaceFilePath = new File( prefs.get("wsPath", System.getProperty("user.dir")));
		return workspaceFilePath;
	}
	public void setWorkspaceFilePath( File file) {
		workspaceFilePath = file;
		prefs.put("wsPath", file.getPath());
		lastUsedWorkspace = true;
	}
	public File getImageFilePath() {
		if( imageFilePath == null)
	        imageFilePath = new File(prefs.get("imgPath", System.getProperty("user.dir")));
		return imageFilePath;
	}
	public void setImageFilePath( File file) {
		imageFilePath = file;
		prefs.put("imgPath", file.getPath());
		lastUsedWorkspace = false;
	}
	public File getAAFFilePath() {
		if( aafFilePath == null)
			aafFilePath = new File( prefs.get("aafPath", System.getProperty("user.dir")));
		return aafFilePath;
	}
	public void setAAFFilePath( File file) {
		aafFilePath = file;
		prefs.put("aafPath", file.getPath());
	}
	public File getOpenFilePath() {
		return lastUsedWorkspace ? getWorkspaceFilePath():getImageFilePath();
	}
	
	// ===============
	// ==== Simple Settings
	public int getDefaultWidth() {return 640;}
    public int getDefaultHeight() { return 480;}
    
    public boolean getBoolSetting( String setting) {
    	switch( setting) {
    	case "promptOnGroupCrop":
    		return true;
		default:
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown Setting String Requested: " + setting);
        	return false;
    	}
    }
    
    public Dimension getThumbnailSize() {
    	return new Dimension( 32, 32);
    }
    
    /** Returns whether or not the editor will allow editing of image data which
     * is currently not visible.
     * 
     *  !!! TODO: UNIMPLEMENTED !!*/
    public boolean getAllowsEdittingInvisible() {
    	return false;
    }
    
    
    // ==============
    // ==== Tablet Pressure Curve

    CubicSplineInterpolator interpolator = null;

	// tpcPoints format:
	// [2, short] : Number of points (min 1)
	// Per point:
	//		[8, double] : x
	//		[8, double] : y
    
    /** Gets the interpolator for interpreting tablet pressure, either recalling
     * it from memory or loading it from preferences.
     */
    public CubicSplineInterpolator getTabletInterpolator() {
    	if( interpolator != null)
    		return interpolator;
    	byte[] raw = prefs.getByteArray("tpcPoints", null);
    	Point2D[] points = null;
    	if( raw != null) {
    		try {
		    	ByteBuffer buff = ByteBuffer.wrap(raw);
		    	
		    	int num = buff.getShort();
		    	points = new Point2D[num];
		    	for( int i=0; i<num; ++i) {
		    		double x = buff.getDouble();
		    		double y = buff.getDouble();
		    		points[i] = new Point2D.Double(x, y);
		    	}
    		} catch( BufferUnderflowException e) {
    			raw = null;
    		}
    	}
    	if( raw == null){
    		points = new Point2D[2];
    		points[0] = new Point2D.Double(0, 0);
    		points[1] = new Point2D.Double(1, 1);
    	}
    	
    	interpolator = new CubicSplineInterpolator(
    			Arrays.asList(points), true, true);
    	
    	return interpolator;
    }
    
    /** Changes the intepolator for interpetting tablet pressure to one constructed
     * from the given points, saving it to preferences as it makes it.
     * 
     * @param a list of points, must be non-null, at least 1 big, each point should
     * be in between (0,0) and (1,1), inclusive
     * @return the constructed Interpolator
     * */
    public CubicSplineInterpolator setTabletInterpolationPoints( List<Point2D> points) {
    	if( points == null || points.size() <= 1 || points.size() > Short.MAX_VALUE)
    		return getTabletInterpolator();
    	
    	ByteBuffer bb = ByteBuffer.allocate( 2 + 8*2*points.size());
    	
    	bb.putShort((short)points.size());
    	for( int i=0; i<points.size(); ++i) {
    		bb.putDouble(points.get(i).getX());
    		bb.putDouble(points.get(i).getY());
    	}
    	prefs.putByteArray("tpcPoints", bb.array());
    	

    	interpolator = new CubicSplineInterpolator(
    			points, true, true);
    	
    	return interpolator;
    }
}
