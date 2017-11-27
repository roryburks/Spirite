package spirite.gui.generic;

import spirite.base.graphics.RawImage;
import spirite.gui.generic.events.SActionEvent.SActionListener;

public interface SButton extends SComponent {
	public void setIcon( RawImage img);
	

	public void addActionListner( SActionListener listener);
	public void removeActionListener( SActionListener listener);
}
