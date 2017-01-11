package spirite.panel_layers;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.Globals;

public class LayerTreeNodePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	JTextField label;
	JPanel ppanel;

	/**
	 * Create the panel.
	 */
	public LayerTreeNodePanel() {
		label = new JTextField("Name");
		ppanel = new JPanel();
		
		label.setFont( new Font("Tahoma", Font.BOLD, 12));
		label.setEditable( true);
		label.setOpaque(false);
		label.setBorder(null);
		
		this.setOpaque( false);
		
		Dimension size = Globals.getMetric("layerpanel.treenodes.max");
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createSequentialGroup()
				.addGap(2)
				.addComponent(ppanel, size.width, size.width, size.width)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(label, 10 ,  128, Integer.MAX_VALUE)
				.addGap(2)
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(2)
							.addComponent(ppanel, size.height,  size.height, size.height))
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(label)))
					.addGap(2)
				)
		);
		setLayout(groupLayout);

	}
}
