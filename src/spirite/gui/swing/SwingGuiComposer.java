package spirite.gui.swing;

import spirite.gui.generic.GuiComposer;
import spirite.gui.generic.SButton;

public class SwingGuiComposer extends GuiComposer {

	@Override
	public SButton Button() {
		return new SwingGuiButton();
	}

}
