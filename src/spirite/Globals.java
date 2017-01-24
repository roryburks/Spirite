// Rory Burks

package spirite;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import spirite.MDebug.ErrorType;

/**
 * The Globals object will centralize all globals that might have reason to
 * change (in particular text that might need localizations and user preferences
 * which they can change, though Hotkeys go through the HotkeyManager).
 *
 * How this is implemented may change in the future, so it's best to abstract it
 * 
 * TODO: It is probably best to have either some kind of sorted list for binary
 * 	searches or a contextual tree so that the search time does not get out
 * 	of hand later.  Or just a HashMap
 */
public class Globals {

	// TODO: In the future these databases will probably be stored as text 
	//	files
	private static final String commands[][] = {
			{"global.new_image"},
			{"global.debug_color"},
			{"global.newLayerQuick"},
			{"global.open_image"},
			{"global.save_image"},
			{"global.save_image_as"},
			{"global.undo"},
			{"global.redo"},
			{"global.export"},
			{"global."},

			{"context.zoom_in"},
			{"context.zoom_out"},
			{"context.zoom_in_slow"},
			{"context.zoom_out_slow"},
			{"context.zoom_0"},
			{"context."},

			{"toolset.PEN"},
			{"toolset.ERASER"},
			{"toolset.FILL"},
			{"toolset.BOX_SELECTION"},
			{"toolset.MOVE"},
			{"toolset.COLOR_PICKER"},
			{"toolset."},

			{"palette.swap"},
			{"palette."},
			
			{"frame."},
			
			{"draw.clearLayer"},
			{"draw."},
			
	};
	
    private static final Object colors[][] = {
        {"drawpanel.image.border", new Color(190,190,190)},
        {"drawpanel.layer.border", new Color(16,16,16)},
        {"toolbutton.selected.background", new Color( 128,128,128)},

        {"contentTree.selectedBGDragging", new Color( 192,192,212)},
        {"contentTree.selectedBackground",new Color( 160,160,196)},

        {"undoPanel.selectedBackground",new Color( 160,160,196)},
        {"undoPanel.background",new Color( 238,238,238)},
    };
    
    private static final Object metrics[][] = {
    		{"layerpanel.treenodes.max", new Dimension( 32, 32)},
    		{"contentTree.dragdropLeniency", new Dimension( 0, 10)},
    		{"contentTree.buttonSize", new Dimension( 24, 24)},
    		{"contentTree.buttonMargin", new Dimension( 2, 2)},
    		{"workspace.max_size", new Dimension( 20000,20000)},
    };
    
    private static final Object icons[][] = {
			{"icons2.png", 25, 25, 4,3},
    		{"visible_on", 0, 0},
    		{"visible_off", 1, 0},
    		{"new_layer", 2, 0},
    		{"new_group", 3, 0},
    		{"icon.frame.layers",0,2},
    		{"icon.frame.undoHistory",1,2},
    		{"icon.frame.animationScheme",2,2},
    		{"icon.frame.toolSettings",3,2},
    		
    		{"icons_12x12.png",13,13,3,1},
    		{"palNewColor", 0,0},
    		{"palSavePalette", 1, 0},
    		{"palLoadPalette", 2, 0},
    		
	};


    public static Color getColor( String id) {
        for( int i = 0; i < colors.length; ++i) {
            if( colors[i][0].equals(id))
                return (Color)colors[i][1];
        }

        return Color.black;
    }

    public static Dimension getMetric( String id) {
    	return getMetric( id, new Dimension(64,64));
    }
    public static Dimension getMetric( String id, Dimension defaultSize) {

        for( int i = 0; i < metrics.length; ++i) {
            if( metrics[i][0].equals(id))
                return (Dimension)metrics[i][1];
        }

        return defaultSize;
    }
    
    private static class IconSet {
    	String resourceFile;
    	BufferedImage img = null;
    	ImageIcon iconsheet[][] = null;
    	int widthPerIcon;
    	int heightPerIcon;
    }
    
    
    private static List<IconSet> iconSets = null;
    private static Object[][] iconTable;
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
    	iconTable = new Object[icons.length - setCount][];

    	IconSet set = null;
    	int index = 0;
    	for( Object[] row : icons) {
    		if( row.length == 5) {
    			for( IconSet iset : iconSets) {
    				if( iset.resourceFile == (String)row[0]) {
    					set = iset;
    					break;
    				}
    			}
    		} else {
    			iconTable[index] = new Object[4];
    			iconTable[index][0] = row[0];
    			iconTable[index][1] = set;
    			iconTable[index][2] = row[1];
    			iconTable[index][3] = row[2];
        		index++;
    		}
    	}
    }
    
    
    public static ImageIcon getIcon( String id) {
    	if( iconSets == null)
    		initIconSets();

    	for( int i = 0; i < iconTable.length; ++i) {
    		if( iconTable[i][0].equals(id)) {

    			IconSet set = (IconSet)iconTable[i][1];
    			int x = (Integer)iconTable[i][2];
    			int y = (Integer)iconTable[i][3];
    			
    			if( set.img == null) {
    				// Load the image if it isn't loaded already
    				try {
						set.img = loadIconSheet(Globals.class.getClassLoader().getResource(set.resourceFile).openStream());
					} catch (Exception e) {
						MDebug.handleError( ErrorType.RESOURCE, null, e.getMessage());
				    	return new ImageIcon( new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB));
					}
    			}
    			
    			if( set.iconsheet[x][y] == null) {
    				BufferedImage img = new BufferedImage(set.widthPerIcon-1,set.heightPerIcon-1, set.img.getType());
    				
    				System.out.println((set.heightPerIcon));
    				Graphics g = img.getGraphics();
    				g.drawImage(set.img, -(set.widthPerIcon)*x, -(set.heightPerIcon)*y, null);
    				g.dispose();
    				
    				set.iconsheet[x][y] = new ImageIcon(img);
    			}
    			
    			return set.iconsheet[x][y];
    		}
    	}

    	return new ImageIcon( new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB));
    }
    
    private static BufferedImage loadIconSheet( InputStream is) throws IOException 
    {
    	BufferedImage buff = ImageIO.read(is);
    	BufferedImage img = new BufferedImage( buff.getWidth(), buff.getHeight(), BufferedImage.TYPE_INT_ARGB);
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
}
