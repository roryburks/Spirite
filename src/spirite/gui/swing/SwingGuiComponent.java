package spirite.gui.swing;

import javax.swing.JComponent;

import spirite.gui.generic.SComponent;


public abstract class SwingGuiComponent 
	implements SComponent
{
	public abstract JComponent getComponent();
	
	@Override
	public void setToolTipText( String text) {
		getComponent().setToolTipText(text);
	}
	

}
