// Rory Burks

package spirite.hybrid;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import spirite.hybrid.MDebug.ErrorType;

/**
 * The Globals object will centralize all globals that might have reason to
 * change (in particular text that might need localizations and user preferences
 * which they can change, though Hotkeys go through the HotkeyManager).
 *
 * How this is implemented may change in the future, so it's best to abstract it
 * 
 * TODO: Move iconTable to a Dictionary
 */
public class Globals {
	
	public static Dictionary<String,Color> colors = null;
	private static void createColorDictionary() {
	    final Object data[][] = {
	        {"drawpanel.image.border", new Color(190,190,190)},
	        {"drawpanel.bid.border", new Color(16,16,16)},
	        {"drawpanel.layer.border", new Color(190,190,120)},
	        {"toolbutton.selected.background", new Color( 128,128,128)},

	        {"contentTree.selectedBGDragging", new Color( 192,192,212)},
	        {"contentTree.selectedBackground",new Color( 160,160,196)},

	        {"undoPanel.selectedBackground",new Color( 160,160,196)},
	        {"undoPanel.background",new Color( 238,238,238)},
	        
	        {"animSchemePanel.activeNodeBG", new Color( 190,160,140,120)},
	        {"animSchemePanel.tickBG", new Color( 250,250,190)},

	        {"workArea.normalBG", new Color(238,238,238)},
	        {"workArea.referenceBG", new Color( 210,210,242)},
	    };
	    colors = new Hashtable<>(data.length);
	    for( Object[] datum : data) {
	    	colors.put((String)datum[0], (Color)datum[1]);
	    }
	}
	
    private static Dictionary<String,Dimension> metrics = null;
    private static void createMetricsDictionary() {
	    final Object data[][] = {
    		{"layerpanel.treenodes.max", new Dimension( 32, 32)},
    		{"contentTree.dragdropLeniency", new Dimension( 0, 10)},
    		{"contentTree.buttonSize", new Dimension( 24, 24)},
    		{"contentTree.buttonMargin", new Dimension( 2, 2)},
    		{"workspace.max_size", new Dimension( 20000,20000)},
	    };
	    metrics = new Hashtable<>(data.length);
	    for( Object[] datum : data) {
	    	metrics.put((String)datum[0], (Dimension)datum[1]);
	    }
    }
    
    private static final Object icons[][] = {
			{"icons2.png", 25, 25, 5,3},
    		{"visible_on", 0, 0},
    		{"visible_off", 1, 0},
    		{"new_layer", 2, 0},
    		{"new_group", 3, 0},

    		{"icon.anim.stepF", 0,1},
    		{"icon.anim.play", 1,1},
    		{"icon.anim.stepB", 2,1},
    		{"icon.anim.export", 3,1},
    		
    		{"icon.frame.layers",0,2},
    		{"icon.frame.undoHistory",1,2},
    		{"icon.frame.animationScheme",2,2},
    		{"icon.frame.toolSettings",3,2},
    		{"icon.frame.referenceScheme",4,2},
    		
    		{"icons_12x12.png",13,13,5,4},
    		{"palNewColor", 0,0},
    		{"palSavePalette", 1, 0},
    		{"palLoadPalette", 2, 0},
    		{"icon.link", 3, 0},
    		{"icon.unlink", 4, 0},
    		
    		{"icon.rig.new", 0, 1},
    		{"icon.rig.rem", 1, 1},
    		{"icon.rig.visOn", 2, 1},
    		{"icon.rig.visOff", 3, 1},

    		{"icon.expanded", 0, 2},
    		{"icon.expandedHL", 1, 2},
    		{"icon.unexpanded", 2, 2},
    		{"icon.unexpandedHL", 3, 2},

    		{"icon.arrowS", 0, 3},
    		{"icon.arrowW", 1, 3},
    		{"icon.arrowN", 2, 3},
    		{"icon.arrowE", 3, 3},
    		
	};


    public static Color getColor( String id) {
    	if( colors == null ) createColorDictionary();
    	Color c = colors.get(id);
        return (c == null)?Color.black:c;
    }

    public static Dimension getMetric( String id) {
    	return getMetric( id, new Dimension(64,64));
    }
    public static Dimension getMetric( String id, Dimension defaultSize) {
    	if( metrics == null) createMetricsDictionary();
    	Dimension d = metrics.get(id);
        return (d == null)?defaultSize:d;
    }
    
    
    // ================
    // ==== Icon Management
    private static class IconSet {
    	String resourceFile;
    	BufferedImage img = null;
    	ImageIcon iconsheet[][] = null;
    	int widthPerIcon;
    	int heightPerIcon;
    }
    private static class IconID {
    	IconSet set;
    	int x, y;
    }
    
    
    private static List<IconSet> iconSets = null;
    private static Dictionary<String, IconID> iconIDs;
    private static void initIconSets() {
    	iconSets = new ArrayList<>();
    	// Construct the iconTable from 
    	
    	// Step 1: 
    	int setCount = 0;
    	for( Object[] row : icons) {
    		if( row.length == 5) {
    			
    			IconSet set = new IconSet();
    			
    			set.resourceFile = (String) row[0];
    			set.widthPerIcon = (Integer) row[1];
    			set.heightPerIcon = (Integer) row[2];
    			int w = (Integer)row[3];
    			int h = (Integer)row[4];
    			set.iconsheet = new ImageIcon[w][];
    			
    			for( int i = 0; i<w; ++i) {
    				set.iconsheet[i] = new ImageIcon[h];
    				for( int j=0; j<h; ++j)
    					set.iconsheet[i][j] = null;
    			}
    			
    			iconSets.add(set);
    			setCount++;
    		}
    	}
    	
    	// Step 2: add the individual table entires
    	iconIDs = new Hashtable<>(icons.length-setCount);

    	IconSet set = null;
    	for( Object[] row : icons) {
    		if( row.length == 5) {
    			for( IconSet iset : iconSets) {
    				if( iset.resourceFile == (String)row[0]) {
    					set = iset;
    					break;
    				}
    			}
    		} else {
    			IconID icon = new IconID();
    			icon.set = set;
    			icon.x = (int)row[1];
    			icon.y = (int)row[2];
    			iconIDs.put((String)row[0], icon);
    		}
    	}
    }
    
    public static final ImageIcon defaultIcon = new ImageIcon( new BufferedImage(1,1,HybridHelper.BI_FORMAT));
    public static ImageIcon getIcon( String id) {
    	if( iconSets == null)
    		initIconSets();
    	
    	IconID icon = iconIDs.get(id);
    	if( icon != null) {
    		IconSet set = icon.set;
    		if( set.img == null) {
    			try {
    				set.img = loadIconSheet(Globals.class.getClassLoader().getResourceAsStream(set.resourceFile));
    			} catch (Exception e) {
					MDebug.handleError( ErrorType.RESOURCE, null, e.getMessage());
			    	return defaultIcon;
				}
    		}
    		
    		if( set.iconsheet[icon.x][icon.y] == null) {

				BufferedImage img = new BufferedImage(set.widthPerIcon-1,set.heightPerIcon-1, set.img.getType());
				
				Graphics g = img.getGraphics();
				g.drawImage(set.img, -(set.widthPerIcon)*icon.x, -(set.heightPerIcon)*icon.y, null);
				g.dispose();
				
				set.iconsheet[icon.x][icon.y] = new ImageIcon(img);
    		}
    		return set.iconsheet[icon.x][icon.y];
    	}

    	return defaultIcon;
    }
    
    private static BufferedImage loadIconSheet( InputStream is) throws IOException 
    {
    	BufferedImage buff = ImageIO.read(is);
    	BufferedImage img = new BufferedImage( buff.getWidth(), buff.getHeight(), HybridHelper.BI_FORMAT);
    	img.getGraphics().drawImage(buff, 0, 0, null);
        
        // Turns all pixels the same color as the top-left pixel into transparent
        //  pixels
        if( img != null) {
            int base = img.getRGB(0, 0);

            for(int x = 0; x < img.getWidth(); ++x) {
                for( int y = 0; y < img.getHeight(); ++y)
                    if( base == img.getRGB(x, y))
                    	img.setRGB(x, y, 0);
            }
        }
        
    	return img;
    }
    
    private static final Font defaultFont = new Font("Tahoma", Font.PLAIN, 10);
    public static Font getFont( String name) {
    	return defaultFont;
    }
}
