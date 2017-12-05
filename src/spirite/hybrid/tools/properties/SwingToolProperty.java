package spirite.hybrid.tools.properties;

import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;

import javax.swing.*;
import javax.swing.GroupLayout.Group;
import java.util.List;

public abstract class SwingToolProperty extends ToolsetManager.Property{
	public abstract List<JComponent> buildComponent( DataBinding<?> binding, Group horizontal, Group vertical, GroupLayout layout, ToolSettings settings);
}