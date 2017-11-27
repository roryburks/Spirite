package spirite.gui.swing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionListener;
import javax.swing.JComponent;

import spirite.gui.generic.SComponent;
import spirite.gui.generic.events.SActionEvent.SActionListener;


public abstract class SwingGuiComponent 
	implements SComponent
{
	public abstract JComponent getComponent();
	
	@Override
	public void setToolTipText( String text) {
		getComponent().setToolTipText(text);
	}
	

}
