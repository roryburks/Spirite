package spirite.pc.ui.panel_work;

import java.awt.Component;

import spirite.base.image_data.ImageWorkspace;
import spirite.pc.ui.panel_work.WorkPanel.View;

/** A WorkArea is a simple abstraction for encapsulating the interactions of 
 * a UIComponent which handles the Drawn Image area
 * 
 * @author Rory Burks
 *
 */
public interface WorkArea {
	public void changeWorkspace( ImageWorkspace workspace, View view);
	public Component getComponent();
}
