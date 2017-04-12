package spirite.panel_work;

import java.awt.Component;

import spirite.image_data.ImageWorkspace;
import spirite.panel_work.WorkPanel.View;

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
