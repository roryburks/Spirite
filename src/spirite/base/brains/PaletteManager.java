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

import spirite.base.brains.commands.CommandExecuter;
import spirite.base.image_data.ImageWorkspace;
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
	private final MasterControl master;
	private final SettingsManager settingsManager;
    private final List<Integer> active_colors;
    
    private final Palette defaultPalette = new Palette("DEFAULT");
    //private final Map<Integer,Integer> palette_colors;


    PaletteManager( MasterControl master) {
    	this.master = master;
    	settingsManager = master.getSettingsManager();
        active_colors = new ArrayList<Integer>();

        active_colors.add(0, Colors.BLACK);
        active_colors.add(1, Colors.WHITE);
        active_colors.add(2, Colors.RED);
        active_colors.add(3, Colors.BLACK);
    }
    
    // ==================
    // ==== Active Color Methods
    public int getActiveColor( int i) {
    	return active_colors.get(i);
    }
    public void setActiveColor( int i, int color) {
    	active_colors.set(i, color);
        triggerPaletteChange();
    }
    public void toggleActiveColors() {
    	int t = active_colors.get(0);
    	active_colors.set(0, active_colors.get(1));
    	active_colors.set(1, active_colors.get(2));
    	active_colors.set(2, active_colors.get(3));
    	active_colors.set(3, t);
        triggerPaletteChange();
    }
    public void toggleActiveColorsBackwards() {
    	int t = active_colors.get(2);
    	active_colors.set(2, active_colors.get(1));
    	active_colors.set(1, active_colors.get(0));
    	active_colors.set(0, active_colors.get(3));
    	active_colors.set(3, t);
        triggerPaletteChange();
    }
    
    // ======
    // ==== Pass-throughs
	public Palette getCurrentPalette() {
		ImageWorkspace ws = master.getCurrentWorkspace();
		if( ws == null)
			return defaultPalette;
		return ws.getCurrentPalette();
	}


    private final static int default_palette[] = {
        Colors.BLACK, Colors.DARK_GRAY, Colors.GRAY, Colors.LIGHT_GRAY, Colors.WHITE,
        Colors.RED, Colors.BLUE, Colors.GREEN, Colors.CYAN, Colors.MAGENTA, Colors.YELLOW,
        Colors.ORANGE, Colors.PINK
    };
    public class Palette {
        private final Map<Integer,Integer> colors = new HashMap<>();
        private String name;
        
        public Palette(String name) {
        	this.name = name;
            for( int i = 0; i<default_palette.length; ++i)
            	colors.put(i, default_palette[i]);
        }
        public Palette(byte[] raw, String name) {
        	this.name = name;
        	//byte raw[] = settingsManager.getRawPalette(name);
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
    	    			colors.put(i+caret, c);
    	    		}
    	    		caret += count;
    	    	}
    	    	count = bis.read();
        	}
        }
        
        public String getName() {return name;}
        public void setName(String name) {
        	this.name = name;
        	triggerPaletteChange();
        }
        
    	public Integer getPaletteColor(int i) {
    		return colors.get(i);
    	}
    	public void setPaletteColor(int i, int color) {
    		colors.put(i, color);
    		triggerPaletteChange();
    	}
        public void removePaletteColor( int i) {
        	colors.remove(i);
            triggerPaletteChange();
        }
        public int getPaletteColorCount() {
            return colors.size();
        }
        public Collection<Integer> getColors() {
        	return colors.values();
        }
        public Set<Entry<Integer,Integer>> getPalette() {
        	return colors.entrySet();
        }
		public void addPaletteColor(int argb) {
			if( !colors.containsValue(argb)) {
				int i=0;
				while( colors.containsKey(i++));
				colors.put(i, argb);
			}
		}
        
        public byte[] compress() {
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
        	
        	Iterator<Integer> it = colors.keySet().iterator();
        	while( it.hasNext()) {
        		int index = it.next();
        		if( index > lastIndex) lastIndex = index;
        	}
        	
        	// Step 2: itterate through, constructing raw data
        	int caret = 0;
        	int peekCount = 0;
        	boolean data = false;
        	
        	
        	while( caret <= lastIndex) {
        		data = colors.containsKey(caret);
        		peekCount = 1;
        		
        		while( colors.containsKey(caret + peekCount) == data) {
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
            				int c = colors.get(caret+i);
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

        	return bos.toByteArray();
        	//settingsManager.saveRawPalette(name, bos.toByteArray());
        }
    }

    
    public List<String> getStoredPaletteNames() {
    	return settingsManager.getStoredPalettes();
    }
    
    
    
    // :::: Palette Change Observer
    public static interface MPaletteObserver {
        public void colorChanged();
    }
    ObserverHandler<MPaletteObserver> paletteObs = new ObserverHandler<>();
    public void addPaletteObserver( MPaletteObserver obs) { paletteObs.addObserver(obs);}
    public void removePaletteObserver( MPaletteObserver obs) { paletteObs.removeObserver(obs);}
    
    public void triggerPaletteChange() {
    	if(paletteObs != null)
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
	public boolean executeCommand(String command, Object extra) {
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
