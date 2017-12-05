package spirite.hybrid;

import spirite.base.graphics.RawImage;
import spirite.gui.generic.GuiComposer;
import spirite.gui.swing.SwingGuiComposer;
import spirite.pc.graphics.ImageBI;

import java.awt.image.BufferedImage;

/**
 * HybridUI
 * 
 * @author RBurks
 *
 */
public class HybridUI {
	public static GuiComposer guiComposer = new SwingGuiComposer();
	public static GuiComposer getGuiComposer() {
		return guiComposer;
	}
	
	public static RawImage createImage( int w, int h) {
		return new ImageBI(new BufferedImage(1,1,HybridHelper.BI_FORMAT));
	}
}
