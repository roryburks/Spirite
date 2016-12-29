package spirite.panel_layers;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.brains.MasterControl;
import spirite.dialogs.NewImagePanel;
import spirite.dialogs.NewLayerDPanel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class LayersPanel extends JPanel {
	MasterControl master;
	
	/**
	 * Create the panel.
	 */
	public LayersPanel(MasterControl master) {
		this.master = master;
		
		JButton btnNewLayer = new JButton();
		btnNewLayer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNewLayerPress();
			}
		});
		btnNewLayer.setToolTipText("New Layer");
		JButton button = new JButton();
		button.setToolTipText("New Group");
		
		LayerTreePanel layerTreePanel = new LayerTreePanel(master);
		
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnNewLayer, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
							.addGap(1)
							.addComponent(button, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE))
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
						.addComponent(button, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
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
			String type = panel.getValueType();
			
//			master.getImageManager().getImage();
		}
	}
}
