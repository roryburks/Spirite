package spirite.panel_layers;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.brains.MasterControl;
import spirite.dialogs.NewLayerDPanel;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageWorkspace;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.event.ActionEvent;

public class LayersPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	MasterControl master;
	
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
						.addComponent(layerTreePanel, GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE))
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
		NewLayerDPanel panel = new NewLayerDPanel(master);
		
		int response = JOptionPane.showConfirmDialog(this,
			panel,
			"New Image",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE);
		
		if( response == JOptionPane.OK_OPTION) {
			int w = panel.getValueWidth();
			int h = panel.getValueHeight();
			String name = panel.getValueName();
			//String type = panel.getValueType();
			Color c = panel.getValueColor();
			

			// Add the new layer contextually according to the selected Node
			GroupTree.Node selected_node = layerTreePanel.getSelectedNode();			
			
			ImageWorkspace workspace = master.getCurrentWorkspace();
			
			workspace.addNewRig(selected_node, w, h, name, c);
		}
	}
	
	private void btnNewGroupPress() {
		GroupTree.Node selected_node = layerTreePanel.getSelectedNode();
		
		master.getCurrentWorkspace().addTreeNode(selected_node, "Test");
	}
}
