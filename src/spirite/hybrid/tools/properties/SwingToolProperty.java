package spirite.hybrid.tools.properties;

import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;

import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;

public abstract class SwingToolProperty extends ToolsetManager.Property{
	public abstract List<JComponent> buildComponent( DataBinding binding, Group horizontal, Group vertical, GroupLayout layout, ToolSettings settings);
}