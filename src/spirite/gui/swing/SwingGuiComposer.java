package spirite.gui.swing;

import spirite.gui.generic.GuiComposer;
import spirite.gui.generic.ISButton;

public class SwingGuiComposer extends GuiComposer {

	@Override
	public ISButton Button() {
		return new SwingGuiButton();
	}

}
