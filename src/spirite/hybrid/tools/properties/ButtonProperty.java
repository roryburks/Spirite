package spirite.hybrid.tools.properties;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;

import spirite.base.brains.MasterControl;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.hybrid.tools.properties.SwingToolProperty;

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

	@Override public String getValue() { return command; }
	@Override protected void setValue( Object newValue) { this.command = (String)newValue;}
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		JButton button = new JButton(hrName);
		JPanel panel = new JPanel();
		
		button.setActionCommand( command);
		button.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				master.executeCommandString(command);
			}
		});

		horizontal.addGroup(layout.createParallelGroup(Alignment.CENTER)
			.addComponent(button).addComponent(panel));
		vertical.addComponent(button, 20,20,20).addComponent(panel,0,0,0);
		
		return Arrays.asList(new JComponent[] {button});
	}
}