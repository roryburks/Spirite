package spirite.pc.ui.panel_layers.anim;

import java.awt.Color;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;
import javax.swing.JLabel;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.animations.rig.RigAnimation;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.gui.hybrid.SPanel;

public class RigAnimationSchemePanel extends SPanel {
	private final MasterControl master;
	private final RigAnimation animation;

	public RigAnimationSchemePanel(MasterControl master, AnimationNode anode) {
		this.master = master;
		this.animation = (RigAnimation) anode.getAnimation();

		rebuildLayout();
	}

	private static final int TITLE_BAR_HEIGHT = 20;
	private static final int BOTTOM_BAR_HEIGHT = 6;
	private static final int TICK_BAR_WIDTH = 50;

	private int tickHeight = 300;

	private void rebuildLayout() {
		GroupLayout layout = new GroupLayout(this);

		TitleBar title = new TitleBar();
		BottomBar bottom = new BottomBar();

		Group subHor = layout.createSequentialGroup();
		Group subVert = layout.createParallelGroup();

		List<Part> parts = animation.getSpriteLayers().get(0).getParts();	// TODO
		JComponent[] tickers = new JComponent[parts.size()];
		for( int i=0; i<parts.size(); ++i) {
			Part part = parts.get(i);
			PartLabel label = new PartLabel(part);
			PartTicker ticker = new PartTicker(part);
			tickers[i] = ticker;

			subHor.addGroup(layout.createParallelGroup()
					.addComponent(label,TICK_BAR_WIDTH,TICK_BAR_WIDTH,TICK_BAR_WIDTH)
					.addComponent(ticker,TICK_BAR_WIDTH,TICK_BAR_WIDTH,TICK_BAR_WIDTH));
			subVert.addGroup(layout.createSequentialGroup()
					.addComponent(label)
					.addComponent(ticker,tickHeight,tickHeight,tickHeight));
		}
		//layout.linkSize(SwingConstants.VERTICAL, tickers);

		layout.setHorizontalGroup( layout.createParallelGroup()
				.addComponent(title)
				.addGroup(subHor)
				.addComponent(bottom));
		layout.setVerticalGroup( layout.createSequentialGroup()
				.addComponent(title,TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT)
				.addGroup(subVert)
				.addComponent(bottom, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT));

		this.setLayout(layout);
	}

	private class TitleBar extends SPanel {
		TitleBar() {
			this.setBackground(new Color(0xFFBBCCBB));
			this.add(new JLabel(animation.getName()));
		}
	}

	private class BottomBar extends SPanel {
		BottomBar() {
		}
	}

	private class PartLabel extends SPanel {
		Part part;
		PartLabel(Part part) {
			this.part = part;

			this.add(new JLabel(part.getTypeName()));
		}
	}
	private class PartTicker extends SPanel {

		public PartTicker(Part part) {
			// TODO Auto-generated constructor stub
		}

	}

}
