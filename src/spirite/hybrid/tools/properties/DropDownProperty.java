package spirite.hybrid.tools.properties;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.GroupLayout.Group;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.hybrid.Globals;
import spirite.hybrid.tools.properties.SwingToolProperty;

public class DropDownProperty extends SwingToolProperty {
	private int value;
	private String[] options;

	public DropDownProperty( String id, String hrName, int defaultValue, String[] options) {
		this(id, hrName, defaultValue, options, 0);
	}
	public DropDownProperty( String id, String hrName, int defaultValue, String[] options, int mask) {
		this.value = defaultValue;
		this.id = id;
		this.hrName = hrName;
		this.options = options;
		this.mask = mask;
	}
	
	@Override public Integer getValue() {return value;}
	@Override protected void setValue(Object newValue) { this.value = (Integer)newValue;}
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		JComboBox<String> comboBox = new JComboBox<>();
		JLabel label = new JLabel( hrName + ":");
		label.setFont( Globals.getFont("toolset.dropdown"));
		
		// Init Components
		for( int i=0; i<options.length; ++i) {
			comboBox.addItem(options[i]);
		}
		comboBox.setFont(new Font("Tahoma",Font.PLAIN, 10));
		comboBox.setSelectedIndex(value);
		comboBox.addItemListener( new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if( e.getStateChange() == ItemEvent.SELECTED)
					binding.triggerUIChanged(comboBox.getSelectedIndex());			
			}
		});
		
		// Binding
		binding.setLink( new ChangeExecuter() {
			@Override public void doUIChange(Object newValue) {
				settings.setValue( id, newValue);
			}
			@Override
			public void doDataChange(Object newValue) {
				comboBox.setSelectedIndex((Integer)newValue);
			}
		});
		
		// Add Components to Layout
		horizontal.addGroup(layout.createParallelGroup()
				.addComponent(label)
				.addComponent(comboBox, 0, 0, Short.MAX_VALUE));
		vertical.addComponent(label)
				.addComponent(comboBox, 20, 20, 20);
		
		
		return Arrays.asList(new JComponent[] {comboBox});
	}
}