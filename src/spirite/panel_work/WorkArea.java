package spirite.panel_work;

import java.awt.Component;

import spirite.image_data.ImageWorkspace;
import spirite.panel_work.WorkPanel.View;

public interface WorkArea {
	public void changeWorkspace( ImageWorkspace workspace, View view);
	public Component getComponent();
}
