package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;

import spirite.base.brains.MasterControl;
import spirite.base.brains.RenderEngine.RenderMethod;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.ImageWorkspace;
import spirite.hybrid.Globals;
import spirite.pc.dialogs.Dialogs;
import spirite.pc.dialogs.NewLayerDPanel.NewLayerHelper;
import spirite.pc.ui.OmniFrame.OmniComponent;
import spirite.pc.ui.UIUtil;
import spirite.pc.ui.components.SliderPanel;

public class LayersPanel extends OmniComponent {
	private static final long serialVersionUID = 1L;
	private final Color comboSel = new Color( 164,164,216);
	private final Color comboNill = new Color( 196,196,196);
	

	private final Dialogs dialogs;
	private final LayerTreePanel layerTreePanel;
	private final JButton btnNewLayer = new JButton();
	private final JButton btnNewGroup = new JButton();
	private final OpacitySlider opacitySlider =  new OpacitySlider();
	
	// Render Chooser Components
	private final JLabel rcLabel = new JLabel("Mode:");
	private final JPanel rcOptions = new JPanel();
	private final JComboBox<RenderTuple> renderCombo;
	private final RenderOptionCellRenderer renderer = new RenderOptionCellRenderer();
	
	private boolean uilocked = false;
	
	private class RenderTuple {
		final RenderMethod method;
		int value;
		RenderTuple( RenderMethod method) {
			this.method = method;
			this.value = method.defaultValue;
		}
	}
		
	/**
	 * Create the panel.
	 */
	public LayersPanel(MasterControl master) {
		this.dialogs = master.getDialogs();
		layerTreePanel = new LayerTreePanel(master, this);
		
		RenderMethod values[] = RenderMethod.values();
		RenderTuple options[] = new RenderTuple[ values.length];
		for( int i=0; i<values.length; ++i)
			options[i] = new RenderTuple(values[i]);
		renderCombo = new JComboBox<RenderTuple>(options);
		
		initComponents();
		initLayout();
		initBindings();
		
		updateSelected();
	}
	
	private void initComponents() {
		btnNewLayer.setToolTipText("New Layer");
		btnNewLayer.setIcon(Globals.getIcon("new_layer"));
		
		btnNewGroup.setToolTipText("New Group");
		btnNewGroup.setIcon( Globals.getIcon("new_group"));
		rcLabel.setFont(new Font("Tahoma", 0, 10));

		renderCombo.setRenderer( renderer);
	}
	
	private void initBindings() {
		btnNewLayer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Create New Layer
				ImageWorkspace workspace = layerTreePanel.workspace;
				NewLayerHelper helper = dialogs.callNewLayerDialog(workspace);
				workspace.addNewSimpleLayer( workspace.getSelectedNode(), 
						helper.width, helper.height, helper.name, helper.color.getRGB());
			}
		});
		btnNewGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Create New Group
				GroupTree.Node selected_node = layerTreePanel.getSelectedNode();
				
				layerTreePanel.workspace.addGroupNode(selected_node, "Test");
			}
		});
		renderCombo.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if( !uilocked) {
					updateSelMethod();
					resetRCOptionPanel();
				}
				
			}
		});
	}
	
	private void updateSelMethod() {
		GroupTree.Node selected = layerTreePanel.getSelectedNode();
		if( selected != null) {
			RenderTuple sel = ((RenderTuple)renderCombo.getSelectedItem());
			selected.setRenderMethod(sel.method, sel.value);
		}
		
	}
	
	private void initLayout() {		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(3)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(rcLabel)
							.addComponent(renderCombo)
							.addComponent(rcOptions, 30,30,30)
						)
						.addComponent(opacitySlider)
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
					.addGroup( groupLayout.createParallelGroup()
						.addComponent(renderCombo, 18,18,18)
						.addComponent(rcLabel, 18,18,18)
						.addComponent(rcOptions, 18, 18, 18)
					)
					.addComponent(opacitySlider, 32, 32, 32)
					.addGap(0)
					.addComponent(layerTreePanel, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btnNewLayer, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnNewGroup, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
					.addGap(16))
		);
		setLayout(groupLayout);

		rcOptions.setLayout(new GridLayout());
	}
	

	// ================
	// ==== API
	public void updateSelected() {
		uilocked = true;
		opacitySlider.refresh();
		

		GroupTree.Node selected = layerTreePanel.getSelectedNode();
		if( selected != null) {
			renderCombo.setSelectedIndex(selected.getRenderMethod().ordinal());
			
			renderCombo.getItemAt(selected.getRenderMethod().ordinal()).value 
				= selected.getRenderValue();
		}
		
		resetRCOptionPanel();
		uilocked = false;
	}
	
	private void resetRCOptionPanel() {
		rcOptions.removeAll();
		RenderTuple sel = ((RenderTuple)renderCombo.getSelectedItem());
		switch( sel.method) {
		case COLOR_CHANGE:
			renderer.ccPanel.setBackground(new Color(sel.value));
			rcOptions.add(renderer.ccPanel);
			break;
		case DEFAULT:
			break;
		
		}
		rcOptions.doLayout();
		rcOptions.revalidate();
		rcOptions.repaint();
	}
	
	// ==============
	// ==== Custom Components
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
	
	/** CellRenderer for the RenderOption Combo Box. */
	class RenderOptionCellRenderer implements ListCellRenderer<RenderTuple> {
		private final JPanel panel = new JPanel();
		private final JLabel lbl = new JLabel();
		
		private JPanel ccPanel = new JPanel();
		
		RenderOptionCellRenderer() {
			panel.setLayout(new GridLayout());
			panel.add(lbl);
			
			ccPanel.addMouseListener( new UIUtil.ClickAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					RenderTuple sel = ((RenderTuple)renderCombo.getSelectedItem());
					Color c = dialogs.pickColor(new Color(sel.value));
					if( c != null) {
						sel.value = c.getRGB();
						ccPanel.setBackground(c);
						updateSelMethod();
					}
				}
			});
		}
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends RenderTuple> list, 
				RenderTuple value, 
				int index,
				boolean isSelected, 
				boolean cellHasFocus) 
		{
			lbl.setText(value.method.description);
			
			panel.setBackground( (isSelected) ? comboSel : comboNill);
			
			return panel;
		}
	}

	
	// :::: OmniContainer
	@Override
	public void onCleanup() {
		layerTreePanel.cleanup();
	}
}
