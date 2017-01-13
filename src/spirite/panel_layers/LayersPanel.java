package spirite.panel_layers;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.brains.MasterControl;
import spirite.dialogs.Dialogs;
import spirite.image_data.GroupTree;
import spirite.ui.UIUtil;

public class LayersPanel extends JPanel {
	// LayersPanel needs Master because various dialogs it creates needs
	//	access to it.  Consider centralizing that in the Dialogs class
	//	for better modularity.
	MasterControl master;
	
	private static final long serialVersionUID = 1L;

	LayerTreePanel layerTreePanel;
	JButton btnNewLayer;
	JButton btnNewGroup;
	OpacitySlider opacitySlider;
		
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
		btnNewGroup = new JButton();
		btnNewGroup.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnNewGroupPress();
			}
		});
		btnNewGroup.setToolTipText("New Group");
		
		
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(opacitySlider)
						)
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

	}
	
	/***
	 * The OpacitySlider Swing Component
	 */
	class OpacitySlider extends JPanel  {
		OSMA adapter = new OSMA();
		OpacitySlider() {
			addMouseMotionListener(adapter);
			addMouseListener(adapter);
		}
		
		@Override
		public void paintComponent(Graphics g) {
			
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
					layerTreePanel.workspace.setNodeAlpha(selected, alpha);
					
					repaint();
				}
			}
		}
	}

	
	private void btnNewLayerPress() {
		Dialogs.performNewLayerDialog(layerTreePanel.workspace);
	}
	
	private void btnNewGroupPress() {
		GroupTree.Node selected_node = layerTreePanel.getSelectedNode();
		
		layerTreePanel.workspace.addGroupNode(selected_node, "Test");
	}
}
