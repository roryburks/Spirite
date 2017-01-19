package spirite.brains;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	private final SettingsManager settingsManager;
    private final List<Color> active_colors;
    private Map<Integer,Color> palette_colors;

    private final static Color default_palette[] = {
        Color.BLACK, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY, Color.WHITE,
        Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.YELLOW,
        Color.ORANGE, Color.PINK
    };

    PaletteManager( MasterControl master) {
    	settingsManager = master.getSettingsManager();
    	palette_colors = new HashMap<>();
        active_colors = new ArrayList<Color>();

        active_colors.add(0, Color.black);
        active_colors.add(1, Color.white);
        
        loadDefaultPalette();
    }
    
    public void loadDefaultPalette() {
    	palette_colors.clear();

        for( int i = 0; i<default_palette.length; ++i) {
        	palette_colors.put(i, default_palette[i]);
        }
        
        triggerColorChanged();
    }
    
    // :::: Active Color Methods
    public Color getActiveColor( int i) {
    	return active_colors.get(i);
    }
    public void setActiveColor( int i, Color color) {
    	active_colors.set(i, color);
        triggerColorChanged();
    }
    public void toggleActiveColors() {
    	Color t = active_colors.get(0);
    	active_colors.set(0, active_colors.get(1));
    	active_colors.set(1, t);
        triggerColorChanged();
    }


    // :::: Palette Color Methods
    public Color getPaletteColor( int i) {
    	return palette_colors.get(i);
    }
    public void setPaletteColor( int i, Color color) {
        palette_colors.put(i, color);
        triggerColorChanged();
    }
    public void addPaletteColor( Color color) {
    	for( int i=0; i < 1000; ++i) {
    		if( !palette_colors.containsKey(i)) {
    			palette_colors.put(i, color);
    			break;
    		}
    	}
        triggerColorChanged();
    }
    public void removePaletteColor( int i) {
    	palette_colors.remove(i);
        triggerColorChanged();
    }
    
    public int getPaletteColorCount() {
        return palette_colors.size();
    }
    
    public Collection<Color> getColors() {
    	return palette_colors.values();
    }
    public Set<Entry<Integer,Color>> getPalette() {
    	return palette_colors.entrySet();
    }
    
    public List<String> getStoredPaletteNames() {
    	return settingsManager.getStoredPalettes();
    }
    
    
    public boolean savePalette( String name) {
    	// The format of a Saved Palette is slightly more complicated than is needed
    	//	but to preserve space and dimensionality, it's sufficient.
    	
    	// [1] First byte corresponds to number of consecutive color datas
    	// [4*n] n*4 bytes representing the color data
    	//		(if first byte was 0x00), 
    	//		[1] next byte represents consecutive empty datas
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	
    	// Step 1: find the highest Color index
    	int lastIndex = -1;
    	
    	Iterator<Integer> it = palette_colors.keySet().iterator();
    	while( it.hasNext()) {
    		int index = it.next();
    		if( index > lastIndex) lastIndex = index;
    	}
    	
    	// Step 2: itterate through, constructing raw data
    	int caret = 0;
    	int peekCount = 0;
    	boolean data = false;
    	
    	
    	while( caret <= lastIndex) {
    		data = palette_colors.containsKey(caret);
    		peekCount = 1;
    		
    		while( palette_colors.containsKey(caret + peekCount) == data) {
    			peekCount++;	// could do with tricky pre-increment, but too unreadable
    		}
    		
    		
    		while ( peekCount > 0) {
    			// Note since we're using bytes to denote distance, in the offchance
    			// that there are more than 255 conescutives, make sure to add
    			//	intermediate markets
    			int tCount = (peekCount > 0xff) ? 0xff : peekCount;
    			

        		if( !data)  {
        			bos.write( 0x00);
        			bos.write(tCount);
        		}
        		else {
        			bos.write(tCount);
        			for( int i=0; i<tCount; ++i) {
        				Color c = palette_colors.get(caret+i);
        				bos.write(c.getRed());
        				bos.write(c.getGreen());
        				bos.write(c.getBlue());
        				bos.write(c.getAlpha());
        			}
        		}
        		
        		peekCount -= tCount;
        		caret += tCount;
    		}
    	}

    	settingsManager.saveRawPalette(name, bos.toByteArray());
    	
    	return true;	// Not sure why/if it should ever return false
    }
    public boolean loadPalette( String name) {
    	byte raw[] = settingsManager.getRawPalette(name);
    	if( raw == null) return false;
    	
    	palette_colors.clear();
    	ByteArrayInputStream bis = new ByteArrayInputStream(raw);
  
    	
    	int caret = 0;
    	int count = bis.read();
    	
    	while( bis.available() > 0) {
	    	if( count == 0) {
	    		caret += bis.read();
	    	}
	    	else {
	    		for( int i = 0; i < count; ++i) {
	    			Color c = new Color( bis.read(), bis.read(), bis.read(), bis.read());
	    			palette_colors.put(i+caret, c);
	    		}
	    	}
    	}
    	
    	triggerColorChanged();
    	
    	return true;
    }
    
    // :::: Hotkeys
    public void performCommand( String command) {
    	if( command.equals("swap"))
    		toggleActiveColors();
    }
    
    // :::: Palette Change Observer
    public static interface MPaletteObserver {
        public void colorChanged();
    }
    
    List<MPaletteObserver> paletteObservers = new ArrayList<>();
    public void addPaletteObserver( MPaletteObserver obs) { paletteObservers.add(obs); }
    public void removePaletteObserver( MPaletteObserver obs) {paletteObservers.remove(obs);}
    
    private void triggerColorChanged() {
        for( MPaletteObserver obs : paletteObservers)
            obs.colorChanged();
    }
}
