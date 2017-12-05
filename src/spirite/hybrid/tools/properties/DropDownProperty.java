package spirite.hybrid.tools.properties;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.gui.hybrid.SComboBox;
import spirite.gui.hybrid.SLabel;
import spirite.hybrid.Globals;

import javax.swing.*;
import javax.swing.GroupLayout.Group;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

public class DropDownProperty<T extends Enum<T>> extends SwingToolProperty {
	private T value;
	private final Class<T> type;

	public DropDownProperty( String id, String hrName, T defaultValue, Class<T> type) {
		this(id, hrName, defaultValue,  0, type);
	}
	public DropDownProperty( String id, String hrName, T defaultValue, int mask, Class<T> type) {
		this.value = defaultValue;
		this.id = id;
		this.hrName = hrName;
		this.mask = mask;
		this.type = type;
	}
	
	@Override public T getValue() {return value;}
	@Override protected void setValue(Object newValue) { this.value = (T)newValue;}
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		SComboBox<T> comboBox = new SComboBox<>();
		SLabel label = new SLabel( hrName + ":", false);
		label.setFont( Globals.getFont("toolset.dropdown"));
		
		// Init Components
		for( T t : type.getEnumConstants()) {
			comboBox.addItem(t);
		}
		comboBox.setFont(new Font("Tahoma",Font.PLAIN, 10));
		comboBox.setSelectedIndex(value.ordinal());
		comboBox.addItemListener( new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if( e.getStateChange() == ItemEvent.SELECTED)
					binding.triggerUIChanged(comboBox.getSelectedItem());			
			}
		});
		
		// Binding
		binding.setLink( new ChangeExecuter() {
			@Override public void doUIChanged(Object newValue) {
				settings.setValue( id, newValue);
			}
			@Override
			public void doDataChanged(Object newValue) {
				comboBox.setSelectedItem((T)newValue);
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