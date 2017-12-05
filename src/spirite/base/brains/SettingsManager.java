package spirite.base.brains;

import spirite.base.graphics.GraphicsDrawer;
import spirite.base.graphics.gl.GLDrawer;
import spirite.base.util.MUtil;
import spirite.base.util.interpolation.CubicSplineInterpolator;
import spirite.base.util.linear.Vec2;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.pc.graphics.awt.AWTDrawer;

import java.io.File;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

/***
 * SettingsManager will handle all the various properties and settings
 * that need to be remembered for ease of use reasons and aren't remembered
 * by Swing's default components.
 *
 * It saves and load data persistantly using Java's Preferences package.
 * 
 * TODO: Make a ToolsetManager-esque system for constructing UI for
 * settings so that they can be altered and saved and loaded easily.
 *
 * @author Rory Burks
 *
 */
public class SettingsManager {
    private final Preferences prefs;
    private final MasterControl master;
    private List<String> paletteList = null;

    public SettingsManager(MasterControl master) {
        this.master = master;
        prefs = Preferences.userNodeForPackage(spirite.Spirite.class);
    }


    // ================
    // ==== Graphics Engine Management
    private boolean glMode = false;

    /** If true, then the engine is able to use OpenGL functionality.  If false
     * (either because the user set it to be false or because OpenGL couldn't
     * initialize properly), then the engine must fall back on basic AWT rendering.*/
    public boolean glMode() {
        return glMode;
    }
    public void setGL(boolean b) {
        if(b) {
            glMode = master.initGL();
        }
        else glMode = false;

        if(!glMode) master.initAWT();
    }
    public GraphicsDrawer getDefaultDrawer() {
        return (glMode())?(GLDrawer.getInstance()):(AWTDrawer.getInstance());
    }

    // ==============
    // ==== Palette Saving/Loading:
    /** used by PaletteManager to get the raw data corresponding to a palette. */
    public byte[] getRawPalette( String name) {
        List<String> names = getStoredPalettes();

        if( !names.contains(name))
            return null;

        return prefs.getByteArray("palette."+name, null);
    }
    public void saveRawPalette( String name, byte[] raw) {
        if( paletteList == null)
            loadPaletteList();

        if( raw.length > Preferences.MAX_VALUE_LENGTH * 3 / 4) {
            MDebug.handleError(ErrorType.ALLOCATION_FAILED, null, "Preference size too large.");
            return;
        }

        if( !paletteList.contains(name)) {
            paletteList.add(name);
            prefs.put("PaletteList", MUtil.joinString("\0", paletteList));
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

    public boolean getBoolSetting( String setting, boolean defaultValue) {
        switch( setting) {
            case "promptOnGroupCrop":
                return true;
            case "DEBUG":
            	return false;
            default:
                //MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown Setting String Requested: " + setting);
                return defaultValue;
        }
    }

    public Vec2 getThumbnailSize() {
        return new Vec2( 32, 32);
    }

    /** Returns whether or not the editor will allow editing of image data which
     * is currently not visible. */
    public boolean getAllowsEdittingInvisible() {
        return false;
    }
    


    // ==============
    // ==== Tablet Pressure Curve

    CubicSplineInterpolator interpolator = null;

    // tpcPoints format:
    // [2, short] : Number of points (min 1)
    // Per point:
    //		[4, float] : x
    //		[4, float] : y

    /** Gets the interpolator for interpreting tablet pressure, either recalling
     * it from memory or loading it from preferences.
     */
    public CubicSplineInterpolator getTabletInterpolator() {
        if( interpolator != null)
            return interpolator;
        byte[] raw = prefs.getByteArray("tpcPoints", null);
        Vec2[] points = null;
        if( raw != null) {
            try {
                ByteBuffer buff = ByteBuffer.wrap(raw);

                int num = buff.getShort();
                points = new Vec2[num];
                for( int i=0; i<num; ++i) {
                    float x = buff.getFloat();
                    float y = buff.getFloat();
                    points[i] = new Vec2(x, y);
                }
            } catch( BufferUnderflowException e) {
                raw = null;
            }
        }
        if( raw == null){
            points = new Vec2[2];
            points[0] = new Vec2(0, 0);
            points[1] = new Vec2(1, 1);
        }

        interpolator = new CubicSplineInterpolator(
                Arrays.asList(points), true, true);

        return interpolator;
    }

    /** Changes the intepolator for interpetting tablet pressure to one constructed
     * from the given points, saving it to preferences as it makes it.
     *
     * @param points list of points, must be non-null, at least 1 big, each point should
     * be in between (0,0) and (1,1), inclusive
     * @return the constructed Interpolator
     * */
    public CubicSplineInterpolator setTabletInterpolationPoints( List<Vec2> points) {
        if( points == null || points.size() <= 1 || points.size() > Short.MAX_VALUE)
            return getTabletInterpolator();

        ByteBuffer bb = ByteBuffer.allocate( 2 + 8*2*points.size());

        bb.putShort((short)points.size());
        for( int i=0; i<points.size(); ++i) {
            bb.putDouble(points.get(i).x);
            bb.putDouble(points.get(i).y);
        }
        prefs.putByteArray("tpcPoints", bb.array());


        interpolator = new CubicSplineInterpolator(
                points, true, true);

        return interpolator;
    }

}
