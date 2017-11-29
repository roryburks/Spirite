package spirite.hybrid.tools.properties;

import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.gui.hybrid.SLabel;
import spirite.hybrid.Globals;
import spirite.pc.ui.components.MTextFieldNumber;

public class FloatBoxProperty extends SwingToolProperty {
	private float value;

	public FloatBoxProperty( String id, String hrName, float defaultValue) {
		this( id, hrName, defaultValue, 0);
	}
	public FloatBoxProperty( String id, String hrName, float defaultValue, int mask) {
		this.value = defaultValue;
		this.id = id;
		this.hrName = hrName;
		this.mask = mask;
	}
	
	@Override public Float getValue() {return value;}
	@Override protected void setValue(Object newValue) { this.value = (Float)newValue;}
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		SLabel label = new SLabel( hrName + ":");
		MTextFieldNumber textField = new MTextFieldNumber(true, true);
		
		label.setFont( Globals.getFont("toolset.dropdown"));
		textField.setText( String.valueOf(value));
		
		// Listener
		textField.getDocument().addDocumentListener( new DocumentListener() {
			public void changedUpdate(DocumentEvent arg0) {
				binding.triggerUIChanged(textField.getFloat());
			}
			public void insertUpdate(DocumentEvent arg0) {
				binding.triggerUIChanged(textField.getFloat());
			}
			public void removeUpdate(DocumentEvent arg0) {
				binding.triggerUIChanged(textField.getFloat());
			}
		});
		
		binding.setLink( new ChangeExecuter() {
			@Override public void doUIChanged(Object newValue) {
				settings.setValue( id, newValue);
			}
			@Override public void doDataChanged(Object newValue) {
				textField.setText( String.valueOf(newValue));
			}
		});
		
		horizontal.addGroup(layout.createSequentialGroup()
				.addComponent(label)
				.addGap(3)
				.addComponent(textField, 0, 0, Short.MAX_VALUE));
		vertical.addGroup( layout.createParallelGroup()
				.addComponent(label)
				.addComponent(textField, 16, 16, 16));
		
		return Arrays.asList( new JComponent[] {textField});
	}
}