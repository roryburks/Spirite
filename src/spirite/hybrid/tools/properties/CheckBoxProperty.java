package spirite.hybrid.tools.properties;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.gui.hybrid.SCheckBox;

public class CheckBoxProperty extends SwingToolProperty {
	private boolean value;

	public CheckBoxProperty( String id, String hrName, boolean defaultValue) {
		this(id, hrName, defaultValue, 0);
	}
	public CheckBoxProperty( String id, String hrName, boolean defaultValue, int mask) {
		this.value = defaultValue;
		this.id = id;
		this.hrName = hrName;
		this.mask = mask;
	}
	
	@Override public Boolean getValue() {return value;}
	@Override protected void setValue(Object newValue) { this.value = (Boolean)newValue;}

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		SCheckBox checkbox = new SCheckBox(hrName);
		
		checkbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				binding.triggerUIChanged(checkbox.isSelected());
			}
		});
		checkbox.setFont(new Font("Tahoma",Font.PLAIN, 10));
		checkbox.setSelected(value);
		
		binding.setLink( new ChangeExecuter() {
			@Override public void doUIChanged(Object newValue) {
				settings.setValue( id, newValue);
			}
			@Override public void doDataChanged(Object newValue) {
				checkbox.setSelected((Boolean)newValue);
			}
		});
		
		horizontal.addGap(5).addComponent(checkbox);
		vertical.addComponent(checkbox);

		return Arrays.asList(new JComponent[] {checkbox});
	}		
}