package spirite.hybrid.tools.properties;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.gui.hybrid.SRadioButton;

public class RadioButtonProperty<T extends Enum<T>> extends SwingToolProperty {
	private T value;
	private final Class<T> type;

	public RadioButtonProperty( String id, String hrName, T defaultValue, Class<T> type) {
		this( id, hrName, defaultValue, 0, type);
	}
	public RadioButtonProperty( String id, String hrName, T defaultValue, int mask, Class<T> type) {
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
		int len = type.getEnumConstants().length;
		List<JComponent> links = new ArrayList<>( len);
		// TODO : Add Binding
		
		
		// Create Components and their settings
		SRadioButton[] radioButtons = new SRadioButton[  len];
		for( int i=0; i< len; ++i) {
			radioButtons[i] = new SRadioButton(type.getEnumConstants()[i].toString());
			links.add( radioButtons[i]);
			radioButtons[i].setFont(new Font("Tahoma",Font.PLAIN, 10));
		}
		
		radioButtons[value.ordinal()].setSelected(true);
		
		// Link actions of the Components, and add them to the layout
		Group horSub = layout.createParallelGroup();
		for( int i=0; i< len; ++i) {
			final T option = type.getEnumConstants()[i];
			radioButtons[i].addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for( int i=0; i<len; ++i) {
						radioButtons[i].setSelected(i == option.ordinal());
					}
					settings.setValue(id, option);
				}
			});
			
			horSub.addComponent(radioButtons[i]);
			vertical.addComponent(radioButtons[i]);
		}
		horizontal.addGroup(horSub);

		return links;
	}
}