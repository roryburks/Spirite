package spirite.base.brains;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import spirite.base.brains.MasterControl.CommandExecuter;
import spirite.base.util.Colors;
import spirite.base.util.ObserverHandler;


/***
 * The PaletteManager stores both the active colors and the palette
 * of colors stored for easy access. 
 * 
 * Palettes are saved using the SettingsManager, which stores them using the 
 * Preference system.  A list of all saved palettes arestored in "PaletteList" 
 * and each palette is stored in "palette.[name]"
 * 
 * @author Rory Burks
 *
 */
public class PaletteManager 
	implements CommandExecuter
{
	private final SettingsManager settingsManager;
    private final List<Integer> active_colors;
    private final Map<Integer,Integer> palette_colors;

    private final static int default_palette[] = {
        Colors.BLACK, Colors.DARK_GRAY, Colors.GRAY, Colors.LIGHT_GRAY, Colors.WHITE,
        Colors.RED, Colors.BLUE, Colors.GREEN, Colors.CYAN, Colors.MAGENTA, Colors.YELLOW,
        Colors.ORANGE, Colors.PINK
    };

    PaletteManager( MasterControl master) {
    	settingsManager = master.getSettingsManager();
    	palette_colors = new HashMap<>();
        active_colors = new ArrayList<Integer>();

        active_colors.add(0, Colors.BLACK);
        active_colors.add(1, Colors.WHITE);
        active_colors.add(2, Colors.RED);
        active_colors.add(3, Colors.BLACK);
        
        loadDefaultPalette();
    }
    
    public void loadDefaultPalette() {
    	palette_colors.clear();

        for( int i = 0; i<default_palette.length; ++i) {
        	palette_colors.put(i, default_palette[i]);
        }
        
        triggerColorChanged();
    }
    
    // ==================
    // ==== Active Color Methods
    public int getActiveColor( int i) {
    	return active_colors.get(i);
    }
    public void setActiveColor( int i, int color) {
    	active_colors.set(i, color);
        triggerColorChanged();
    }
    public void toggleActiveColors() {
    	int t = active_colors.get(0);
    	active_colors.set(0, active_colors.get(1));
    	active_colors.set(1, active_colors.get(2));
    	active_colors.set(2, active_colors.get(3));
    	active_colors.set(3, t);
        triggerColorChanged();
    }
    public void toggleActiveColorsBackwards() {
    	int t = active_colors.get(2);
    	active_colors.set(2, active_colors.get(1));
    	active_colors.set(1, active_colors.get(0));
    	active_colors.set(0, active_colors.get(3));
    	active_colors.set(3, t);
        triggerColorChanged();
    }


    // ===================
    // ==== Palette Color Methods
    public Integer getPaletteColor( int i) {
    	return palette_colors.get(i);
    }
    public void setPaletteColor( int i, int color) {
        palette_colors.put(i, color);
        triggerColorChanged();
    }
    public void addPaletteColor( int color) {
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
    
    public Collection<Integer> getColors() {
    	return palette_colors.values();
    }
    public Set<Entry<Integer,Integer>> getPalette() {
    	return palette_colors.entrySet();
    }
    
    public List<String> getStoredPaletteNames() {
    	return settingsManager.getStoredPalettes();
    }
    
    // ==================
    // ==== Palette Saving/Loading
    
    /** Saves the palette as the given name. */
    public boolean savePalette( String name) {
    	// For the most part Palettes are stored as an array of 4 bytes per
    	//	color in RGBA format/order.  But to preserve dimensionality of 
    	//	the palette while avoiding excessive "whitespace" bytes, the following
    	// 	format is used:
    	
    	// [1] First byte corresponds to number of consecutive color datas
    	// [4*n] n*4 bytes representing the color data, in RGBA form
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
        				int c = palette_colors.get(caret+i);
        				bos.write(Colors.getRed(c));
        				bos.write(Colors.getGreen(c));
        				bos.write(Colors.getBlue(c));
        				bos.write(Colors.getAlpha(c));
        			}
        		}
        		
        		peekCount -= tCount;
        		caret += tCount;
    		}
    	}

    	settingsManager.saveRawPalette(name, bos.toByteArray());
    	
    	return true;	// Not sure why/if it should ever return false
    }
    
    /** Loads the palette of the given name. */
    public boolean loadPalette( String name) {
    	byte raw[] = settingsManager.getRawPalette(name);
    	if( raw == null) return false;
    	
    	palette_colors.clear();
    	ByteArrayInputStream bis = new ByteArrayInputStream(raw);
    	
    	int caret = 0;
    	int count = bis.read();
    	
    	while( bis.available() > 0) {
	    	if( count == 0) {
	    		int i = bis.read();
	    		caret += i;
	    	}
	    	else {
	    		for( int i = 0; i < count; ++i) {
	    			int r = bis.read();
	    			int g = bis.read();
	    			int b = bis.read();
	    			int a = bis.read();
	    			int c = Colors.toColor(a, r, g, b);
	    			palette_colors.put(i+caret, c);
	    		}
	    		caret += count;
	    	}
	    	count = bis.read();
    	}
    	
    	triggerColorChanged();
    	
    	return true;
    }
    
    
    
    // :::: Palette Change Observer
    public static interface MPaletteObserver {
        public void colorChanged();
    }
    ObserverHandler<MPaletteObserver> paletteObs = new ObserverHandler<>();
    public void addPaletteObserver( MPaletteObserver obs) { paletteObs.addObserver(obs);}
    public void removePaletteObserver( MPaletteObserver obs) { paletteObs.removeObserver(obs);}
    
    private void triggerColorChanged() {
    	paletteObs.trigger((MPaletteObserver obs) -> {obs.colorChanged();});
    }
    

    // ================
    // ==== Implemented Interfaces

    // :::: CommandExecuter
	@Override
	public List<String> getValidCommands() {
		return Arrays.asList( new String[]{"swap","swapBack"});
	}

	@Override
	public String getCommandDomain() {
		return "palette";
	}

	@Override
	public boolean executeCommand(String command) {
    	switch (command) {
    	case "swap":
    		toggleActiveColors();
    		return true;
    	case "swapBack":
    		toggleActiveColorsBackwards();
    		return true;
    	}
		return false;
	}
}
