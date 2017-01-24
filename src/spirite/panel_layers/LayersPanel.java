package spirite.panel_layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.dialogs.Dialogs;
import spirite.image_data.GroupTree;
import spirite.ui.components.SliderPanel;
import spirite.ui.OmniFrame.OmniComponent;

public class LayersPanel extends OmniComponent {
	private static final long serialVersionUID = 1L;

	private final LayerTreePanel layerTreePanel;
	private final JButton btnNewLayer;
	private final JButton btnNewGroup;
	private final OpacitySlider opacitySlider;
		
	/**
	 * Create the panel.
	 */
	public LayersPanel(MasterControl master) {
		
		opacitySlider = new OpacitySlider();
		layerTreePanel = new LayerTreePanel(master, this);
		btnNewLayer = new JButton();
		btnNewGroup = new JButton();
		
		initComponents();
	}
	
	private void initComponents() {
		btnNewLayer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNewLayerPress();
			}
		});
		btnNewLayer.setToolTipText("New Layer");
		btnNewLayer.setIcon(Globals.getIcon("new_layer"));
		
		btnNewGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNewGroupPress();
			}
		});
		btnNewGroup.setToolTipText("New Group");
		btnNewGroup.setIcon( Globals.getIcon("new_group"));
		
		
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(3)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(opacitySlider)
						)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnNewLayer, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
							.addGap(1)
							.addComponent(btnNewGroup, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE))
						.addComponent(layerTreePanel, GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
					.addGap(3))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
						.addGap(3)
					.addComponent(opacitySlider, 20, 20, 20)
					.addGap(0)
					.addComponent(layerTreePanel, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btnNewLayer, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnNewGroup, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
					.addGap(16))
		);
		setLayout(groupLayout);

		opacitySlider.refresh();
		
	}
	
	/** The OpacitySlider Swing Component */
	class OpacitySlider extends SliderPanel {
		OpacitySlider() {
			setMin(0.0f);
			setMax(1.0f);
			setLabel("Opacity: ");
		}
		
		public void refresh() {
			if( layerTreePanel == null) return;
			GroupTree.Node selected = layerTreePanel.getSelectedNode();
			if( selected != null) {
				setValue( selected.getAlpha());
			}
		}
		
		@Override
		public void onValueChanged(float newValue) {
			GroupTree.Node selected = layerTreePanel.getSelectedNode();
			if( selected != null) {
				selected.setAlpha(getValue());
			}
			super.onValueChanged(newValue);
		}
	}
	
	public void updateSelected() {
		opacitySlider.refresh();
	}

	
	private void btnNewLayerPress() {
		Dialogs.performNewLayerDialog(layerTreePanel.workspace);
	}
	
	private void btnNewGroupPress() {
		GroupTree.Node selected_node = layerTreePanel.getSelectedNode();
		
		layerTreePanel.workspace.addGroupNode(selected_node, "Test");
	}
	
	// :::: OmniContainer
	@Override
	public void onCleanup() {
		layerTreePanel.cleanup();
	}
}
