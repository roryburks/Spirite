package spirite.panel_layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.brains.MasterControl;
import spirite.dialogs.Dialogs;
import spirite.image_data.GroupTree;

public class LayersPanel extends JPanel {
	// LayersPanel needs Master because various dialogs it creates needs
	//	access to it.  Consider centralizing that in the Dialogs class
	//	for better modularity.
	MasterControl master;
	
	private static final long serialVersionUID = 1L;

	LayerTreePanel layerTreePanel;
	JButton btnNewLayer;
	JButton btnNewGroup;
	
	/**
	 * Create the panel.
	 */
	public LayersPanel(MasterControl master) {
		this.master = master;
		
		
		btnNewLayer = new JButton();
		btnNewLayer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNewLayerPress();
			}
		});
		btnNewLayer.setToolTipText("New Layer");
		btnNewGroup = new JButton();
		btnNewGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNewGroupPress();
			}
		});
		btnNewGroup.setToolTipText("New Group");
		
		layerTreePanel = new LayerTreePanel(master);
		
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnNewLayer, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
							.addGap(1)
							.addComponent(btnNewGroup, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE))
						.addComponent(layerTreePanel, GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(layerTreePanel, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btnNewLayer, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnNewGroup, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
					.addGap(16))
		);
		setLayout(groupLayout);

	}
	
	private void btnNewLayerPress() {
		Dialogs.performNewLayerDialog(layerTreePanel.workspace);
	}
	
	private void btnNewGroupPress() {
		GroupTree.Node selected_node = layerTreePanel.getSelectedNode();
		
		layerTreePanel.workspace.addGroupNode(selected_node, "Test");
	}
}
