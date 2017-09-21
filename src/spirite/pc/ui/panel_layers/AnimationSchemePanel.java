package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JLabel;
import javax.swing.JPanel;

import spirite.base.image_data.Animation;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;


/***
 * AnimationSchemePanel is a grid 
 */
public class AnimationSchemePanel extends JPanel {
	private FixedFrameAnimation animation;
	private final JPanel topLeft = new JPanel();
	
	private final int TITLE_BAR_HEIGHT = 16;
	
	public AnimationSchemePanel( FixedFrameAnimation fixedFrameAnimation) {
		this.animation = fixedFrameAnimation;
		BuildFromAnimation();
	}
	
	private void BuildFromAnimation() {
		int start = animation.getStart();
		int end = animation.getEnd();
		
		this.removeAll();
		List<AnimationLayer> layers = animation.getLayers();
		
		GroupLayout layout = new GroupLayout(this);
		
		Group horGroup = layout.createParallelGroup();
		Group vertGroup = layout.createSequentialGroup();
		
		// Title Bar
		Group titleHorGroup = layout.createSequentialGroup();
		Group titleVertGroup = layout.createParallelGroup();
		
		titleHorGroup.addComponent(topLeft);
		titleVertGroup.addComponent(topLeft, TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT);
		
		for( AnimationLayer layer : layers) {
			Component title = new TitleBar(layer.getName());
			titleHorGroup.addComponent(title);
			titleVertGroup.addComponent(title, TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT);
		}
		
		layout.setHorizontalGroup(horGroup.addGroup(titleHorGroup));
		layout.setVerticalGroup(vertGroup.addGroup(titleVertGroup));
		
		this.setLayout(layout);
	}
	
	private class TitleBar extends JPanel {
		String title;
		private final JLabel label = new JLabel();
		
		private TitleBar( String title) {
			this.title = title;

			this.add(label);
			label.setText(title);
		}
	}
}
