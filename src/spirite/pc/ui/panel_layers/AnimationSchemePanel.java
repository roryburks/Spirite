package spirite.pc.ui.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.MSelectionObserver;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.hybrid.Globals;
import spirite.base.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.pc.graphics.ImageBI;
import spirite.pc.ui.components.OmniEye;


/***
 * AnimationSchemePanel is a grid 
 */
public class AnimationSchemePanel extends JPanel implements MWorkspaceObserver, MSelectionObserver {
	private final MasterControl master;
	private ImageWorkspace ws = null;
	
	private FixedFrameAnimation animation;
	private final JPanel topLeft = new JPanel();
	private final JPanel bottomRight = new JPanel();

	private final int TITLE_BAR_HEIGHT = 16;
	private final int BOTTOM_BAR_HEIGHT = 16;
	private final int TL_WIDTH = 16;
	private final int ROW_HEIGHT = 32;

	private static final Color ARROW_COLOR = Color.RED;
	private static final Color TITLE_BG = Color.WHITE;
	private final Color tickColor = Globals.getColor("animSchemePanel.tickBG");
	
	public AnimationSchemePanel( MasterControl master, FixedFrameAnimation fixedFrameAnimation) {
		this.master = master;
		this.animation = fixedFrameAnimation;
		BuildFromAnimation();
		
		master.addWorkspaceObserver(this);
		ws = master.getCurrentWorkspace();
		if( ws != null) {
			ws.addSelectionObserver(this);
		}
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
		Group vertMid = layout.createParallelGroup();
		vertGroup.addGroup(vertMid);
		{
			Group vertInner = layout.createSequentialGroup();
			vertMid.addGroup(vertInner);
			for( int i = start; i < end; ++i) {
				
				ticks[i-start] = new TickPanel(i);
				horGroups[0].addComponent(ticks[i-start], TL_WIDTH,TL_WIDTH,TL_WIDTH);
				vertInner.addComponent(ticks[i-start],ROW_HEIGHT,ROW_HEIGHT,ROW_HEIGHT);
				System.out.println(i);
			}
		}
		
		layout.linkSize(SwingConstants.VERTICAL, ticks);
		
		// Fill out the animation
		for( int i=0; i < layers.size(); ++i) {
			Group vertInner = layout.createSequentialGroup();
			vertMid.addGroup(vertInner);
			AnimationLayer layer = layers.get(i);
			
			int currentTick = start;
			
			List<Component> linked = new ArrayList<>();
			
			for(Frame frame : layer.getFrames()) {
				if( frame.getMarker() == Marker.FRAME)
				{
					int fs = Math.max( currentTick,frame.getStart());
					int fe = Math.max(fs, Math.min(end, frame.getEnd()));
					
					if( fs < fe) {
						if( fs > currentTick)
							vertInner.addGap(ROW_HEIGHT * fs - currentTick);
						
						Component component = new FramePanel( frame, fe-fs-1);
						linked.add(component);
						int h = ROW_HEIGHT * (fe-fs);
						vertInner.addComponent(component,h,h,h);
						horGroups[1+i].addComponent(component);
												
						currentTick = fe;
					}
				}
			}

			linked.add(titles[i]);
			layout.linkSize(SwingConstants.HORIZONTAL, linked.toArray( new Component[linked.size()]));
		}
		
		// Lower Bar
		Group bottomVertGroup = layout.createParallelGroup();
		vertGroup.addGroup(bottomVertGroup);
		
		bottomVertGroup.addComponent(bottomRight, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT, BOTTOM_BAR_HEIGHT);
		horGroups[0].addComponent(bottomRight, TL_WIDTH,TL_WIDTH,TL_WIDTH);

		for( int j=0; j < layers.size(); ++j) {
			AnimationLayer layer = layers.get(j);
			
			Component c = new BottomPanel();
			
			horGroups[j+1].addComponent(c);
			bottomVertGroup.addComponent(c);
			layout.linkSize(SwingConstants.HORIZONTAL, c, titles[j]);
		}
		
		layout.setHorizontalGroup(horGroup);
		layout.setVerticalGroup(vertGroup);
		
		this.setLayout(layout);
	}
	
	// :::: WorkspaceObserver
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {	}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {	}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( ws != null) {
			ws.removeSelectionObserver(this);
		}
		ws = selected;
		if( ws != null) {
			ws.addSelectionObserver(this);
		}
		
		Rebuild();
	}
	
	// :::: SelectionOberver
	@Override
	public void selectionChanged(Node newSelection) {
		this.repaint();
	}
	
	private class TitleBar extends JPanel {
		String title;
		private final JLabel label = new JLabel();
		
		private TitleBar( String title) {
			//this.setLayout(new GridLayout());
			
			this.setBackground(TITLE_BG );
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
			
			this.setBackground( tickColor);
			
			label.setFont( new Font("Tahoma",Font.BOLD, 10));

			this.add(label);
		}
	}
	
	private class BottomPanel extends JPanel {
		private final JToggleButton btnLock = new JToggleButton("X");
		private final JToggleButton btnSettings = new JToggleButton("S");
		private final JToggleButton omniEye = new OmniEye();
		
		private BottomPanel() {
			btnLock.setBorder(null);
			btnSettings.setBorder(null);
			

			GroupLayout layout = new GroupLayout(this);
			layout.setHorizontalGroup( layout.createSequentialGroup()
					.addGroup( layout.createParallelGroup()
							.addComponent(btnLock)
							.addComponent(btnSettings))
					.addComponent(omniEye));
			this.setBorder( BorderFactory.createLineBorder(Color.BLACK));
			layout.setVerticalGroup( layout.createParallelGroup()
					.addGroup( layout.createSequentialGroup()
							.addComponent(btnLock)
							.addComponent(btnSettings))
					.addComponent(omniEye));
			
			layout.linkSize( SwingConstants.VERTICAL, btnLock,btnSettings);
			
			this.setLayout(layout);
		}
	}
	
	private class FramePanel extends JPanel {
		private final Frame frame;
		
		private final JToggleButton btnVLock = new JToggleButton("V");
		private final JToggleButton btnLLock = new JToggleButton("L");
		private final JPanel drawPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				ImageBI img = (ImageBI)master.getRenderEngine().accessThumbnail(frame.getLayerNode(), ImageBI.class);
				
				if( img != null)
					g.drawImage( img.img, 0, 0, this.getWidth(), this.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
			}
		};
		
		private FramePanel( Frame frame, int extendHeight) {
			this.frame = frame;
			btnVLock.setBorder(null);
			btnLLock.setBorder(null);

			drawPanel.setOpaque(false);
			
			
			GroupLayout layout = new GroupLayout(this);
			
			Group hor = layout.createParallelGroup();
			hor.addGroup(layout.createSequentialGroup()
					.addGroup( layout.createParallelGroup()
							.addComponent(btnVLock)
							.addComponent(btnLLock))
					.addComponent(drawPanel));
			
			Group vert = layout.createSequentialGroup();
			vert.addGroup(layout.createParallelGroup()
					.addGroup( layout.createSequentialGroup()
							.addComponent(btnVLock)
							.addComponent(btnLLock))
					.addComponent(drawPanel, ROW_HEIGHT,ROW_HEIGHT,ROW_HEIGHT));
			
			layout.setHorizontalGroup( hor);
			layout.setVerticalGroup( vert);
			
			if( extendHeight > 0) {
				Component c = new FrameExtendPanel();
				vert.addComponent(c);
				hor.addComponent(c);
			}

			drawPanel.setBorder( BorderFactory.createLineBorder(Color.BLACK));
			
			layout.linkSize( SwingConstants.VERTICAL, btnVLock,btnLLock);
			
			this.setLayout(layout);
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			setBackground( (ws != null && ws.getSelectedNode() == frame.getLayerNode()) ? Color.YELLOW : Color.WHITE );
			
			//g.fillRect( 0, 0, this.getWidth(), this.getHeight());
			
			super.paintComponent(g);
		}

		private class FrameExtendPanel extends JPanel {
			public FrameExtendPanel() {
				this.setOpaque(false);
			}
			
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(ARROW_COLOR);
				
				int b = 2;
				int tw = 2;
				int hw = 8;
				int hh = 8;
				int c = this.getWidth() / 2;
				int H = this.getHeight();
				int W = this.getWidth();

				g.fillRect( c - tw/2, b, tw, H - 2*b - hh);
				g.fillPolygon( new int[] {(W-hw)/2,(W+hw)/2,c}, new int[] {H-b-hh,H-b-hh,H-b}, 3);
			};
		}
	}

	
}
