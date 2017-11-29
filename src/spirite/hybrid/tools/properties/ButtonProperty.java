package spirite.hybrid.tools.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.JButton;
import javax.swing.JComponent;

import spirite.base.brains.MasterControl;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.gui.hybrid.SPanel;

public class ButtonProperty extends SwingToolProperty {
	private final MasterControl master;	// I don't love this
	private String command;

	public ButtonProperty( String id, String hrName, String actionCommand, MasterControl master) {
		this( id, hrName, actionCommand, master, 0);
	}
	public ButtonProperty( String id, String hrName, String actionCommand, MasterControl master, int mask) {
		this.command = actionCommand;
		this.master = master;
		this.hrName = hrName;
		this.id = id;
		this.mask = mask;
	}

	// Note: For Buttons, 
	@Override public Boolean getValue() { return false; }
	@Override protected void setValue( Object newValue) { }
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		JButton button = new JButton(hrName);
		SPanel panel = new SPanel();
		
		button.setActionCommand( command);
		button.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				if( command != null)
					master.executeCommandString(command);
				settings.setValue(id, null);
			}
		});

		horizontal.addGroup(layout.createParallelGroup(Alignment.CENTER)
			.addComponent(button).addComponent(panel));
		vertical.addComponent(button, 20,20,20).addComponent(panel,0,0,0);
		
		return Arrays.asList(new JComponent[] {button});
	}
}