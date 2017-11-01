package spirite.pc.ui.panel_layers.layer_properties;

import java.awt.Graphics;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.graphics.renderer.sources.LayerRenderSource;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.layers.Layer;
import spirite.base.image_data.layers.ReferenceLayer;
import spirite.hybrid.HybridUtil;
import spirite.pc.graphics.ImageBI;

public class ReferenceLayerPanel extends JPanel {
	private final MasterControl master;
	ReferenceLayer ref;
	ImageWorkspace workspace;
	
	private final JLabel lblRefTo = new JLabel();
	private final RefDrawArea drawPanel = new RefDrawArea();
	private final JButton btnDeep = new JButton("Deep Copy");
	
	ReferenceLayerPanel( MasterControl master) {
		this.master = master;
		initLayout();
	}
	
	private void initLayout() {
		GroupLayout layout = new GroupLayout(this);
		
		layout.setHorizontalGroup( layout.createSequentialGroup()
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				.addComponent(lblRefTo)
				.addComponent(drawPanel)
				.addComponent(btnDeep, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
			.addGap(3));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addComponent(lblRefTo)
			.addGap(3)
			.addComponent(drawPanel)
			.addGap(3)
			.addComponent(btnDeep)
			.addGap(3));
		
		
		this.setLayout(layout);
	}
	

	void setReference( ReferenceLayer ref, ImageWorkspace workspace) {
		this.ref = ref;
		this.workspace = workspace;
		lblRefTo.setText("Reference to: " + ref.getUnderlying().getName());
		drawPanel.repaint();
	}
	
	class RefDrawArea extends JPanel 
	{
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Layer layer = ref.getUnderlying().getLayer();
			
			RenderSettings settings = new RenderSettings(new LayerRenderSource( workspace, layer));
			RawImage img = master.getRenderEngine().renderImage(settings);
			ImageBI bi = (ImageBI) HybridUtil.convert(img, ImageBI.class);
			
			g.drawImage( bi.img, 0, 0, null);
		}
	}
}
