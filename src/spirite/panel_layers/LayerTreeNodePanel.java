package spirite.panel_layers;

import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.Globals;

public class LayerTreeNodePanel extends JPanel {
	JLabel label;
	JPanel ppanel;

	/**
	 * Create the panel.
	 */
	public LayerTreeNodePanel() {
		label = new JLabel( "Name");
		ppanel = new JPanel();
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(2)
					.addComponent(ppanel, GroupLayout.PREFERRED_SIZE, Globals.getMetric("layerpanel.treenodes.max").width, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(label)
					.addContainerGap(65, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(2)
							.addComponent(ppanel, GroupLayout.PREFERRED_SIZE,  Globals.getMetric("layerpanel.treenodes.max").height, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(label)))
					.addContainerGap(34, Short.MAX_VALUE))
		);
		setLayout(groupLayout);

	}
}
