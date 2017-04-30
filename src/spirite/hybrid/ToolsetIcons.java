package spirite.hybrid;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import spirite.base.brains.ToolsetManager.Tool;
import spirite.hybrid.MDebug.ErrorType;

public class ToolsetIcons {

    // ===================
    // ==== Tool Icon Sheet Management
    static BufferedImage icon_sheet = null;
    static int is_width, is_height;
    private static final int TOOL_ICON_WIDTH = 24;
    private static final int TOOL_ICON_HEIGHT = 24;
    
    /** Loads the icon sheet from tool_icons.png */
    private static void prepareIconSheet() {
        icon_sheet = null;
        try {
            BufferedImage buff = ImageIO.read ( ToolsetIcons.class.getClassLoader().getResource("tool_icons.png").openStream());
            icon_sheet = new BufferedImage( buff.getWidth(), buff.getHeight(), HybridHelper.BI_FORMAT);
            
            Graphics g = icon_sheet.getGraphics();
            g.drawImage(buff, 0, 0, null);
            g.dispose();

            is_width = icon_sheet.getWidth() / (TOOL_ICON_WIDTH+1);
            is_height = icon_sheet.getHeight() / (TOOL_ICON_HEIGHT+1);
        } catch (IOException e) {
        	MDebug.handleError( ErrorType.RESOURCE, e, "Failed to prepare Toolset Icon Sheet");
        }
    }
    
    /** Draws the icon for the given tool.*/
    public static void drawIcon( Graphics g, Tool tool) {
    	if( icon_sheet == null) prepareIconSheet();
    	int ix = getToolix(tool);
    	int iy = getTooliy(tool);
        g.drawImage( icon_sheet, 0, 0, TOOL_ICON_WIDTH, TOOL_ICON_HEIGHT,
                ix*(TOOL_ICON_WIDTH+1), iy*(TOOL_ICON_HEIGHT+1), 
                ix*(TOOL_ICON_WIDTH+1)+TOOL_ICON_WIDTH, iy*(TOOL_ICON_HEIGHT+1)+TOOL_ICON_HEIGHT, null);
    }

    // Gets the position the toolset is in the icons.png image
    private static int getToolix( Tool tool) {
    	return tool.iconLocation % is_width;
    }
    private static int getTooliy( Tool tool) {
        return tool.iconLocation / is_width;
    }
}
