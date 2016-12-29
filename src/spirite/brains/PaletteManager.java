package spirite.brains;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/***
 * The PaletteManager stores both the active colors and the palette
 * of colors stored for easy access. 
 * 
 * TODO: make the palettes able to be saved and loaded.
 * 
 * @author Rory Burks
 *
 */
public class PaletteManager {
    List<Color> active_colors;
    List<Color> palette_colors;

    final static Color default_palette[] = {
        Color.BLACK, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY, Color.WHITE,
        Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.YELLOW,
        Color.ORANGE, Color.PINK
    };

    PaletteManager() {
    	palette_colors = new ArrayList();
        active_colors = new ArrayList();

        active_colors.add(0, Color.black);
        active_colors.add(1, Color.white);
        
        palette_colors.addAll(Arrays.asList(default_palette));
    }
    
    // :::: Active Color Methods
    public Color getActiveColor( int i) {
    	return active_colors.get(i);
    }
    public void setActiveColor( int i, Color color) {
    	active_colors.set(i, color);
        colorChanged();
    }
    public void toggleActiveColors() {
    	Color t = active_colors.get(0);
    	active_colors.set(0, active_colors.get(1));
    	active_colors.set(1, t);
        colorChanged();
    }


    // :::: Palette Color Methods
    public Color getPaletteColor( int i) {
    	return palette_colors.get(i);
    }
    public void setPaletteColor( int i, Color color) {
        palette_colors.set(i, color);
        colorChanged();
    }
    public void addPaletteColor( Color color) {
    	palette_colors.add( color);
        colorChanged();
    }
    public void removePaletteColor( int i) {
    	palette_colors.remove(i);
        colorChanged();
    }
    
    public int getPaletteColorCount() {
        return palette_colors.size();
    }
    
    // :::: Hotkeys
    public void performCommand( String command) {
    	if( command.equals("swap"))
    		toggleActiveColors();
    }
    
    // :::: Palette Change Observer
    List<MPaletteObserver> paletteObservers = new ArrayList<>();

    public void addPaletteObserver( MPaletteObserver obs) { paletteObservers.add(obs); }
    public void removePaletteObserver( MPaletteObserver obs) {paletteObservers.remove(obs);}
    void colorChanged() {
        for( MPaletteObserver obs : paletteObservers)
            obs.colorChanged();
    }

    public static interface MPaletteObserver {
        public void colorChanged();
    }
}
