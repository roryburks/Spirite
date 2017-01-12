package spirite.panel_layers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.Globals;
import spirite.image_data.ImageData;

public class LayerTreeNodePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	JTextField label;
	LTNPPanel ppanel;

	static final Color c1 = new Color( 168,168,168);
	static final Color c2 = new Color( 192,192,192);
	static final int N = 8;
	public class LTNPPanel extends JPanel {
		public ImageData image = null;
		
		@Override
		public void paint(Graphics g) {
			int width = this.getWidth();
			int height = this.getHeight();

			
			for(int i =0; i < N; ++i) {
				for( int j=0; j<N;++j) {
					g.setColor(
							(((i+j)%2) == 1)? c1:c2);
					g.fillRect(i*width/N, j*height/N, width/N, height/N);
				}
			}
			
			if( image != null) {
				Graphics2D g2 = (Graphics2D)g;
				RenderingHints oldHints = g2.getRenderingHints();
				RenderingHints newHints = new RenderingHints(
			             RenderingHints.KEY_TEXT_ANTIALIASING,
			             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				newHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, 
						RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
				newHints.put( RenderingHints.KEY_INTERPOLATION, 
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				g2.setRenderingHints(newHints);
				g2.drawImage(
						image.getData(), 
						0, 0, 
						this.getWidth(),
						this.getHeight(),
						null);
				g2.setRenderingHints(oldHints);
			}
		}
		
	}
	
	/**
	 * Create the panel.
	 */
	public LayerTreeNodePanel() {
		label = new JTextField("Name");
		ppanel = new LTNPPanel();
		
		label.setFont( new Font("Tahoma", Font.BOLD, 12));
		label.setEditable( true);
		label.setOpaque(false);
		label.setBorder(null);
		
		
		this.setOpaque( false);
		
		Dimension size = Globals.getMetric("layerpanel.treenodes.max");
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createSequentialGroup()
				.addGap(2)
				.addComponent(ppanel, size.width, size.width, size.width)
				.addPreferredGap(ComponentPlacement.RELATED)
				.addComponent(label, 10 ,  128, Integer.MAX_VALUE)
				.addGap(2)
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(2)
							.addComponent(ppanel, size.height,  size.height, size.height))
						.addGroup(groupLayout.createSequentialGroup()
							.addContainerGap()
							.addComponent(label)))
					.addGap(2)
				)
		);
		setLayout(groupLayout);

	}
}
