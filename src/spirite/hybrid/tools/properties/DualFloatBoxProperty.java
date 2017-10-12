package spirite.hybrid.tools.properties;

import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.base.util.glmath.Vec2;
import spirite.hybrid.Globals;
import spirite.pc.ui.components.MTextFieldNumber;

public class DualFloatBoxProperty extends SwingToolProperty {
	private float x, y;
	private String _label1, _label2;

	public DualFloatBoxProperty( String id, String hrName, float defaultX, float defaultY, String label1, String label2) {
		this(id, hrName, defaultX, defaultY, label1, label2, 0);
	}
	public DualFloatBoxProperty( String id, String hrName, float defaultX, float defaultY, String label1, String label2, int mask) {
		this.x = defaultX;
		this.y = defaultY;
		this.id = id;
		this.hrName = hrName;
		this._label1 = label1;
		this._label2 = label2;
		this.mask = mask;
	}
	
	@Override public Vec2 getValue() {return new Vec2(x,y);}
	@Override protected void setValue(Object newValue) { 
		this.x = ((Vec2)newValue).x;
		this.y = ((Vec2)newValue).y;
	}
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		JLabel labelMain = new JLabel(hrName + ":");
		JLabel label1 = new JLabel( _label1);
		JLabel label2 = new JLabel( _label2);
		MTextFieldNumber textField1 = new MTextFieldNumber(true, true);
		MTextFieldNumber textField2 = new MTextFieldNumber(true, true);
		
		labelMain.setFont( Globals.getFont("toolset.dropdown"));
		label1.setFont( Globals.getFont("toolset.dropdown"));
		label2.setFont( Globals.getFont("toolset.dropdown"));
		textField1.setText( String.valueOf(x));
		textField2.setText( String.valueOf(y));
		
		// Listener
		DocumentListener listener = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				binding.triggerUIChanged(new Vec2(textField1.getFloat(), textField2.getFloat()));
			}
			public void insertUpdate(DocumentEvent e) {
				binding.triggerUIChanged(new Vec2(textField1.getFloat(), textField2.getFloat()));
			}
			public void removeUpdate(DocumentEvent e) {
				binding.triggerUIChanged(new Vec2(textField1.getFloat(), textField2.getFloat()));
			}
		};
		textField1.getDocument().addDocumentListener(listener);
		textField2.getDocument().addDocumentListener(listener);
		
		binding.setLink( new ChangeExecuter() {
			public void doUIChange(Object newValue) {
				settings.setValue( id, newValue);
			}
			public void doDataChange(Object newValue) {
				Vec2 val = (Vec2)newValue;
				textField1.setText( String.valueOf(val.x));
				textField2.setText( String.valueOf(val.y));
			}
		});
		
		// Layout
		horizontal.addGroup(layout.createParallelGroup()
			.addComponent(labelMain)
			.addGroup(layout.createSequentialGroup()
				.addComponent(textField1, 0, 0, Short.MAX_VALUE)
				.addGap(3)
				.addComponent(label1)
				.addGap(5)
				.addComponent(textField2, 0, 0, Short.MAX_VALUE)
				.addGap(3)
				.addComponent(label2)));
		vertical.addGroup(layout.createSequentialGroup()
			.addComponent(labelMain)
			.addGroup(layout.createParallelGroup()
				.addComponent(textField1, 16, 16, 16)
				.addComponent(textField2, 16, 16, 16)
				.addComponent(label1)
				.addComponent(label2)));
		
		return Arrays.asList(new JComponent[] { textField1, textField2});
	}
}