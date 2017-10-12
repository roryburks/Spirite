package spirite.hybrid.tools.properties;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;
import javax.swing.JRadioButton;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;

public class RadioButtonProperty extends SwingToolProperty {
	private int value;
	private String[] options;

	public RadioButtonProperty( String id, String hrName, int defaultValue, String[] options) {
		this( id, hrName, defaultValue, options, 0);
	}
	public RadioButtonProperty( String id, String hrName, int defaultValue, String[] options, int mask) {
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
		List<JComponent> links = new ArrayList<>( options.length);
		// TODO : Add Binding
		
		
		// Create Components and their settings
		JRadioButton[] radioButtons = new JRadioButton[ options.length];
		for( int i=0; i<options.length; ++i) {
			radioButtons[i] = new JRadioButton(options[i]);
			links.add( radioButtons[i]);
			radioButtons[i].setFont(new Font("Tahoma",Font.PLAIN, 10));
		}
		
		radioButtons[value].setSelected(true);
		
		// Link actions of the Components, and add them to the layout
		Group horSub = layout.createParallelGroup();
		for( int i=0; i<options.length; ++i) {
			final int index = i;
			radioButtons[i].addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for( int i=0; i<options.length; ++i) {
						radioButtons[i].setSelected(i == index);
					}
					settings.setValue(id, index);
				}
			});
			
			horSub.addComponent(radioButtons[i]);
			vertical.addComponent(radioButtons[i]);
		}
		horizontal.addGroup(horSub);

		return links;
	}
}