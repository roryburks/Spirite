package spirite.panel_layers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.dialogs.Dialogs;
import spirite.image_data.GroupTree;
import spirite.ui.components.SliderPanel;

public class LayersPanel extends JPanel {
	// LayersPanel needs Master because various dialogs it creates needs
	//	access to it.  Consider centralizing that in the Dialogs class
	//	for better modularity.
	MasterControl master;
	
	private static final long serialVersionUID = 1L;

	private final LayerTreePanel layerTreePanel;
	private final JButton btnNewLayer;
	private final JButton btnNewGroup;
	private final OpacitySlider opacitySlider;
		
	/**
	 * Create the panel.
	 */
	public LayersPanel(MasterControl master) {
		this.master = master;
		
		
		opacitySlider = new OpacitySlider();
		layerTreePanel = new LayerTreePanel(master, this);
		
		
		btnNewLayer = new JButton();
		btnNewLayer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNewLayerPress();
			}
		});
		btnNewLayer.setToolTipText("New Layer");
		btnNewLayer.setIcon(Globals.getIcon("new_layer"));
		
		btnNewGroup = new JButton();
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
/*	class OpacitySlider extends JPanel  {
		OSMA adapter = new OSMA();
		OpacitySlider() {
			addMouseMotionListener(adapter);
			addMouseListener(adapter);
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D)g;
			
			Paint oldP = g2.getPaint();
			Paint newP = new GradientPaint( 0,0, new Color(64,64,64), getWidth(), 0, new Color( 128,128,128));
			g2.setPaint(newP);
			g2.fillRect(0, 0, getWidth(), getHeight());
			

			GroupTree.Node selected = layerTreePanel.getSelectedNode();
			
			if( selected!= null) {
				newP = new GradientPaint( 0,0, new Color(120,120,190), 0, getHeight(), new Color( 90,90,160));
				g2.setPaint(newP);
				g2.fillRect( 0, 0, Math.round(getWidth()*selected.getAlpha()), getHeight());
				
				g2.setColor( new Color( 222,222,222));
				
				UIUtil.drawStringCenter(g2, "Opacity: " + Math.round(selected.getAlpha() * 100), getBounds());
			}
			

			g2.setPaint(oldP);
		}

		private class OSMA extends MouseAdapter {
			@Override
			public void mousePressed(MouseEvent e) {
				updateAlpha(e);
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				updateAlpha(e);
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			void updateAlpha( MouseEvent e) {
				GroupTree.Node selected = layerTreePanel.getSelectedNode();
				
				if( selected != null) {
					float alpha = (float)e.getX() / (float)getWidth();
					alpha = Math.min( 1.0f, Math.max(0.0f, alpha));
					selected.setAlpha(alpha);
					
					repaint();
				}
			}
		}
	}*/
	
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
}
