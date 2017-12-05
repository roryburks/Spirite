package spirite.gui.swing;

import spirite.gui.generic.SComponent;

import javax.swing.*;


public abstract class SwingGuiComponent 
	implements SComponent
{
	public abstract JComponent getComponent();
	
	@Override
	public void setToolTipText( String text) {
		getComponent().setToolTipText(text);
	}
	

}
