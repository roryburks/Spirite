package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.renderer.RenderEngine.RenderMethod;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.ImageWorkspace;
import spirite.gui.hybrid.SButton;
import spirite.gui.hybrid.SComboBox;
import spirite.gui.hybrid.SLabel;
import spirite.gui.hybrid.SPanel;
import spirite.gui.hybrid.STabbedPane;
import spirite.hybrid.Globals;
import spirite.pc.ui.UIUtil;
import spirite.pc.ui.components.SliderPanel;
import spirite.pc.ui.dialogs.Dialogs;
import spirite.pc.ui.dialogs.NewLayerDPanel.NewLayerHelper;
import spirite.pc.ui.omni.OmniFrame.OmniComponent;
import spirite.pc.ui.panel_layers.anim.LayerAnimView;

public class LayersPanel extends SPanel
	implements OmniComponent 
{
	private static final long serialVersionUID = 1L;
	private final Color comboSel = new Color( 164,164,216);
	private final Color comboNill = new Color( 196,196,196);
	

	private final Dialogs dialogs;
	private final LayerTreePanel layerTreePanel;
	private final SButton btnNewLayer = new SButton();
	private final SButton btnNewGroup = new SButton();
	private final OpacitySlider opacitySlider =  new OpacitySlider();
	private final LayerAnimView layerAnimView;
	private final LayerTabPane layerTabPane;
	
	// Render Chooser Components
	private final SLabel rcLabel = new SLabel("Mode:");
	private final SPanel rcOptions = new SPanel();
	private final SComboBox<RenderTuple> renderCombo;
	private final RenderOptionCellRenderer renderer = new RenderOptionCellRenderer();
	
	private boolean uilocked = false;
	
	public class RenderTuple {
		final RenderMethod method;
		int value;
		RenderTuple( RenderMethod method) {
			this.method = method;
			this.value = method.defaultValue;
		}
	}
	
	class LayerTabPane extends STabbedPane {
		LayerTabPane() {
			this.addTab("Normal View", layerTreePanel);
			this.addTab("AnimView", layerAnimView);
		}
	}
		
	/**
	 * Create the panel.
	 */
	public LayersPanel(MasterControl master) {  
		
		this.dialogs = master.getDialogs();
		layerTreePanel = new LayerTreePanel(master, this);
		layerAnimView = new LayerAnimView( master, this);
		layerTabPane = new LayerTabPane();	// must be after its sub-components
		
		RenderMethod values[] = RenderMethod.values();
		RenderTuple options[] = new RenderTuple[ values.length];
		for( int i=0; i<values.length; ++i)
			options[i] = new RenderTuple(values[i]);
		renderCombo = new SComboBox<RenderTuple>(options);
		
		initComponents();
		initLayout();
		initBindings();
		
		updateSelected();
		
		layerTabPane.addChangeListener((evt) -> {
			ImageWorkspace ws = master.getCurrentWorkspace();
			if( ws != null) {
				ws.getAnimationManager().getView().setUsingAnimationView( layerTabPane.getSelectedIndex() == 1);
				ws.triggerFlash();
			}
		});
	}
	
	private void initComponents() {
		btnNewLayer.setToolTipText("New Layer");
		btnNewLayer.setIcon(Globals.getIcon("new_layer"));

		btnNewGroup.setToolTipText("New Group");
		btnNewGroup.setIcon( Globals.getIcon("new_group"));
		rcLabel.setFont(new Font("Tahoma", 0, 10));

		renderCombo.setForeground(Globals.getColor("textDark"));
		renderCombo.setRenderer( renderer);
	}
	
	private void initBindings() {
		btnNewLayer.addActionListener((evt) -> {
			// Create New Layer
			ImageWorkspace workspace = layerTreePanel.workspace;
			NewLayerHelper helper = dialogs.callNewLayerDialog(workspace);
			
			if( helper != null ) {
				workspace.addNewSimpleLayer( workspace.getSelectedNode(), 
					helper.width, helper.height, helper.name, helper.color.getRGB(), helper.imgType);
			}
		});
		btnNewGroup.addActionListener((evt) -> {
			// Create New Group
			GroupTree.Node selected_node = layerTreePanel.getSelectedNode();
			
			layerTreePanel.workspace.addGroupNode(selected_node, "Test");
		});
		renderCombo.addActionListener( (evt) -> {
			if( !uilocked) {
				updateSelMethod();
				resetRCOptionPanel();
			}
		});
	}
	
	private void updateSelMethod() {
		GroupTree.Node selected = layerTreePanel.getSelectedNode();
		if( selected != null) {
			RenderTuple sel = ((RenderTuple)renderCombo.getSelectedItem());
			selected.getRender().setMethod(sel.method, sel.value);
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
						.addComponent(layerTabPane, GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
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
					.addComponent(layerTabPane, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
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
			renderCombo.setSelectedIndex(selected.getRender().getMethod().ordinal());
			
			renderCombo.getItemAt(selected.getRender().getMethod().ordinal()).value 
				= selected.getRender().getRenderValue();
		}
		
		resetRCOptionPanel();
		uilocked = false;
	}
	
	private void resetRCOptionPanel() {
		rcOptions.removeAll();
		RenderTuple sel = ((RenderTuple)renderCombo.getSelectedItem());
		switch( sel.method) {
		case COLOR_CHANGE_HUE:
		case COLOR_CHANGE_FULL:
			renderer.ccPanel.setBackground(new Color(sel.value));
			rcOptions.add(renderer.ccPanel);
			break;
		default:
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
				setValue( selected.getRender().getAlpha());
			}
		}
		
		@Override
		public void onValueChanged(float newValue) {
			GroupTree.Node selected = layerTreePanel.getSelectedNode();
			if( selected != null) {
				selected.getRender().setAlpha(getValue());
			}
			super.onValueChanged(newValue);
		}
	}
	
	/** CellRenderer for the RenderOption Combo Box. */
	public class RenderOptionCellRenderer implements ListCellRenderer<RenderTuple> {
		private final SPanel panel = new SPanel();
		private final SLabel lbl = new SLabel(true);
		
		private SPanel ccPanel = new SPanel();
		
		public RenderOptionCellRenderer() {
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
	@Override public void onCleanup() {
		layerTreePanel.cleanup();
	}

	@Override public JComponent getComponent() {
		return this;
	}
}
