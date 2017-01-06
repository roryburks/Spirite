package spirite.panel_layers;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.Globals;

public class LayerTreeNodePanel extends JPanel {
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
		
//		Dimension maxWidth = Globals.getMetric("layerpanel.treenodes.max");
		
		
		this.setBackground( new Color(0,0,0,0));
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(2)
					.addComponent(ppanel, GroupLayout.PREFERRED_SIZE, Globals.getMetric("layerpanel.treenodes.max").width, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(label, 10 ,  128, Integer.MAX_VALUE)
					.addGap(2)
				)
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
					.addGap(2)
				)
		);
		setLayout(groupLayout);

	}
}
