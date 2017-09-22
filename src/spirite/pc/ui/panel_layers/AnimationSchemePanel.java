package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import spirite.base.image_data.Animation;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.base.image_data.animation_data.FixedFrameAnimation.Marker;


/***
 * AnimationSchemePanel is a grid 
 */
public class AnimationSchemePanel extends JPanel {
	private FixedFrameAnimation animation;
	private final JPanel topLeft = new JPanel();
	private final JPanel bottomRight = new JPanel();

	private final int TITLE_BAR_HEIGHT = 16;
	private final int BOTTOM_BAR_HEIGHT = 16;
	private final int TL_WIDTH = 16;
	private final int ROW_HEIGHT = 32;
	
	public AnimationSchemePanel( FixedFrameAnimation fixedFrameAnimation) {
		this.animation = fixedFrameAnimation;
		BuildFromAnimation();
		
	}
	
	public void Rebuild() {
		BuildFromAnimation();	
	}
	
	private void BuildFromAnimation() {
		int start = animation.getStart();
		int end = animation.getEnd();
		
		this.removeAll();
		List<AnimationLayer> layers = animation.getLayers();
		
		GroupLayout layout = new GroupLayout(this);
		
		Group horGroup = layout.createSequentialGroup();
		Group vertGroup = layout.createSequentialGroup();
		
		Group[] horGroups = new Group[layers.size() + 1];
		for( int i=0; i < layers.size() + 1; ++i) {
			horGroups[i] = layout.createParallelGroup();
			horGroup.addGroup(horGroups[i]);
		}
		
		// Title Bar
		Group titleVertGroup = layout.createParallelGroup();
		vertGroup.addGroup(titleVertGroup);
		horGroups[0].addComponent(topLeft, TL_WIDTH,TL_WIDTH,TL_WIDTH);
		titleVertGroup.addComponent(topLeft, TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT);
		
		Component[] titles= new Component[layers.size()];
		
		for( int i=0; i < layers.size(); ++i) {
			AnimationLayer layer = layers.get(i);
			titles[i] = new TitleBar(layer.getName());
			horGroups[i+1].addComponent(titles[i]);
			titleVertGroup.addComponent( titles[i], TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT,TITLE_BAR_HEIGHT);
		}
		layout.linkSize(SwingConstants.HORIZONTAL, titles);
		
		// Animation Bars
		Component[] ticks = new Component[Math.max(0,end-start)];
		Group[] vertGroups = new Group[Math.max(0, end-start)];
		for( int i = start; i < end; ++i) {
			vertGroups[i-start]= layout.createParallelGroup();
			vertGroup.addGroup(vertGroups[i-start]);
			
			ticks[i-start] = new TickPanel(i);
			horGroups[0].addComponent(ticks[i-start], TL_WIDTH,TL_WIDTH,TL_WIDTH);
			vertGroups[i-start].addComponent(ticks[i-start],ROW_HEIGHT,ROW_HEIGHT,ROW_HEIGHT);
		}
		layout.linkSize(SwingConstants.VERTICAL, ticks);
		
		// Fill out the animation
		for( int i=0; i < layers.size(); ++i) {
			AnimationLayer layer = layers.get(i);
			
			for(Frame frame : layer.getFrames()) {
				if( frame.getMarker() == Marker.FRAME)
				{
					int fs = Math.max(start, frame.getStart());
					int fe = Math.min( end, Math.max(fs, frame.getEnd()));
					
					if( fs < fe) {
						Component component = new FramePanel();
						int h = ROW_HEIGHT * (fe-fs);
						vertGroups[fs-start].addComponent(component,h,h,h);
						horGroups[1+i].addComponent(component);
					}
				}
			}
		}
		
		// Lower Bar
		Group bottomVertGroup = layout.createParallelGroup();
		vertGroup.addGroup(bottomVertGroup);
		
		bottomVertGroup.addComponent(bottomRight, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT);
		horGroups[0].addComponent(bottomRight, TL_WIDTH,TL_WIDTH,TL_WIDTH);

		for( int j=0; j < layers.size(); ++j) {
			AnimationLayer layer = layers.get(j);
			// TODO
		}
		
		layout.setHorizontalGroup(horGroup);
		layout.setVerticalGroup(vertGroup);
		
		this.setLayout(layout);
	}
	
	private class TitleBar extends JPanel {
		String title;
		private final JLabel label = new JLabel();
		
		private TitleBar( String title) {
			//this.setLayout(new GridLayout());
			
			this.setBackground( new Color((int)(Math.random()*0xfffff)));
			this.title = title;
			
			label.setFont( new Font("Tahoma",Font.BOLD, 10));

			this.add(label);
			label.setText(title);
		}
	}
	
	private class TickPanel extends JPanel {
		private int tick;
		private final JLabel label = new JLabel();
		
		private TickPanel( int tick) {
			this.tick = tick;
			label.setText(""+tick);
			
			this.setBackground( new Color((int)(Math.random()*0xfffff)));
			
			label.setFont( new Font("Tahoma",Font.BOLD, 10));

			this.add(label);
		}
	}
	
	private class FramePanel extends JPanel {
		private FramePanel() {
			this.setBorder( BorderFactory.createLineBorder(Color.BLACK));
		}
	}
}
