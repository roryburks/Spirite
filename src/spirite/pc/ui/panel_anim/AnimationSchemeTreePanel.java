package spirite.pc.ui.panel_anim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStateEvent;
import spirite.base.image_data.AnimationManager.MAnimationStateObserver;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.MSelectionObserver;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.base.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.base.image_data.animation_data.FixedFrameAnimation.AnimationLayer.Frame;
import spirite.pc.Globals;
import spirite.pc.ui.UIUtil;

public class AnimationSchemeTreePanel extends JPanel 
	implements  MAnimationStructureObserver, MSelectionObserver, 
		MWorkspaceObserver, MAnimationStateObserver
{
	private static final long serialVersionUID = 1L;

	// MasterControl needed to add/remove WorkspaceObserver
	private final MasterControl master;
	private ImageWorkspace workspace = null;
	private AnimationManager manager = null;
	
	private final JPanel container = new JPanel();
	private final JScrollPane scrollPane = new JScrollPane(container);
	
	/**
	 * Create the panel.
	 */
	public AnimationSchemeTreePanel( MasterControl master) {
		this.master = master;
		this.setLayout(new GridLayout());
		

		container.setBackground(Color.white);
		this.add(scrollPane);
		
		workspace = master.getCurrentWorkspace();
		if( workspace != null) {
			manager = workspace.getAnimationManager();
			manager.addAnimationStructureObserver(this);
			manager.addAnimationStateObserver(this);
			workspace.addSelectionObserver(this);
		}
		
		master.addWorkspaceObserver(this);
		
		reconstruct();
	}

	/***
	 * Constructs the AnimationTree from all the animations stored in the 
	 * AnimationManager
	 */
	private void reconstruct() {
		container.removeAll();
		
		
		GroupLayout layout = new GroupLayout(container);
		
		Group horLeft = layout.createParallelGroup();
		Group horRight = layout.createParallelGroup();
		SequentialGroup vertical = layout.createSequentialGroup();
		
		if( manager != null) {
			
			for( Animation animation : manager.getAnimations()) {

				// Construct the components
				TitlePanel labelPanel = new TitlePanel(animation);
				Component content = constructComponentFromAnimation(animation);
				ExpandButton expandButton = new ExpandButton(content, animation);
				
				
				expandButton.setSelected(manager.getAnimationState(animation).getExpanded());
				content.setVisible(expandButton.isSelected());
				
				
				labelPanel.titleLabel.setText(animation.getName());
				labelPanel.startLabel.setText(""+animation.getStartFrame());
				labelPanel.endLabel.setText(""+animation.getEndFrame());
				
				vertical.addGroup(layout.createParallelGroup()
					.addComponent(labelPanel,TITLE_HEIGHT,TITLE_HEIGHT,TITLE_HEIGHT)
					.addComponent(expandButton,TITLE_HEIGHT,TITLE_HEIGHT,TITLE_HEIGHT))
				.addGroup(layout.createParallelGroup()
					.addComponent(content,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE));
				horLeft.addComponent(expandButton,LEFT_SECTION_WIDTH,LEFT_SECTION_WIDTH,LEFT_SECTION_WIDTH);
				horRight.addComponent(labelPanel);
				horRight.addComponent(content,0,0,Short.MAX_VALUE);
			}
		}
		

//		model.nodeStructureChanged(root);
		vertical.addGap(0,0,Short.MAX_VALUE);
		layout.setVerticalGroup(vertical);
		layout.setHorizontalGroup( layout.createSequentialGroup()
			.addGroup(horLeft)
			.addGroup(horRight));
		
		container.setLayout(layout);
	}
	private static final int LEFT_SECTION_WIDTH = 16;
	private static final int TITLE_HEIGHT = 24;
	
	
	class ExpandButton extends JToggleButton implements ActionListener {
		private final Component content;
		private final Animation animation;
		ExpandButton(Component content, Animation animation) {
			this.content = content;
			this.animation = animation;
			this.setOpaque(false);
			this.setBackground(new Color(0,0,0,0));
			this.setBorder(null);

			this.setIcon(Globals.getIcon("icon.expanded"));
			this.setRolloverIcon(Globals.getIcon("icon.expandedHL"));
			this.setSelectedIcon(Globals.getIcon("icon.unexpanded"));
			this.setRolloverSelectedIcon(Globals.getIcon("icon.unexpandedHL"));
			
			this.addActionListener(this);
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			content.setVisible(isSelected());
			
			manager.getAnimationState(animation).setExpanded(isSelected());
		}
	}
	

	// 
	private Component constructComponentFromAnimation(Animation animation) {
		AnimNodeBuilder builder =builderMap.get(animation);
		
		if( builder == null) {
			builder = createComponent(animation);
			builderMap.put(animation, builder);
		}
		
		return builder.getComponent();
	}

	public interface AnimNodeBuilder  {
		public void updateComponent();
		public Component getComponent();
	}
	private final HashMap<Animation,AnimNodeBuilder> builderMap = new HashMap<>();
	
	private AnimNodeBuilder createComponent( Animation anim) {
		if( anim instanceof FixedFrameAnimation) {
			return new FixedFramePanel((FixedFrameAnimation)anim);
		}
		return null;
	}
	
	class BaseTPanel extends JPanel {
		JPanel imgPanel;
		JLabel titleLabel;
		JLabel startLabel;
		JLabel endLabel;
		
		BaseTPanel() {
			setOpaque(false);
			
			imgPanel = new JPanel();
			titleLabel = new JLabel();
			startLabel = new JLabel();
			endLabel = new JLabel();

			startLabel.setBorder( BorderFactory.createLineBorder(Color.BLACK) );
			endLabel.setBorder( BorderFactory.createLineBorder(Color.BLACK) );
			startLabel.setHorizontalAlignment(JLabel.CENTER);
			endLabel.setHorizontalAlignment(JLabel.CENTER);

			Dimension preview = new Dimension(24,24);
			Dimension time = new Dimension(20,20);
			
			GroupLayout layout = new GroupLayout(this);
			
			layout.setHorizontalGroup( layout.createSequentialGroup()
				.addGap(2)
				.addComponent(imgPanel, preview.width,preview.width,preview.width)
				.addGap(2)
				.addComponent(titleLabel)
				.addGap(2)
				.addComponent(startLabel, time.width,time.width,time.width)
				.addGap(2)
				.addComponent(endLabel, time.width,time.width,time.width)
			);
			
			layout.setVerticalGroup( layout.createSequentialGroup()
				.addGroup( layout.createParallelGroup( GroupLayout.Alignment.TRAILING)
					.addGap(2)
					.addComponent(imgPanel, preview.height,preview.height,preview.height)
					.addComponent(titleLabel, time.height,time.height,time.height)
					.addComponent(startLabel, time.height,time.height,time.height)
					.addComponent(endLabel, time.height,time.height,time.height)
					.addGap(2)
				)
			);
			
			this.setLayout(layout);
		}
	}
	class TitlePanel extends BaseTPanel {
		final Animation animation;
		
		TitlePanel( Animation animation) {
			this.animation = animation;
			this.addMouseListener(new UIUtil.ClickAdapter() {
				@Override
				public void mouseClicked(MouseEvent evt) {
					super.mouseClicked(evt);

					if( manager != null) {
						switch( evt.getButton()) {
						case MouseEvent.BUTTON1:
							manager.setSelectedAnimation(animation);
							break;
						case MouseEvent.BUTTON3:
							contextMenu.animation = animation;
							Object[][] menuScheme = new Object[][]{
								{"Delete Animation", "remAnim", null}
							};
							contextMenu.removeAll();
							UIUtil.constructMenu(contextMenu, menuScheme, contextMenu);
							contextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
							break;
						}
					}
				}
			});
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			if( manager != null && manager.getSelectedAnimation() == animation) {
				g.setColor(selectedAnimColor);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
			super.paintComponent(g);
		}
	}
	private final ContextMenu contextMenu = new ContextMenu();
	private class ContextMenu extends JPopupMenu
		implements ActionListener
	{
		Animation animation;

		@Override
		public void actionPerformed(ActionEvent e) {
			switch( e.getActionCommand()) {
			case "remAnim":
				if( manager != null) {
					manager.removeAnimation(animation);
				}
				break;
			}
		}
	}

	private final Color pseudoselectColor = Globals.getColor("animSchemePanel.activeNodeBG");
	private final Color tickColor = Globals.getColor("animSchemePanel.tickBG");
	private final Color selectedAnimColor = Globals.getColor("contentTree.selectedBackground");
	// Back-up component for nodes that don't have anything more specific
	
	class FixedFramePanel extends JPanel 
		implements AnimNodeBuilder
	{
		static final int TICK_HEIGHT = 24;
		static final int TICK_WIDTH = 20;
		static final int LABEL_HEIGHT = 20;
		static final int NODE_HEIGHT = 24;
		static final int MARKER_HEIGHT = 10;
		static final int OUTLINE_WIDTH = 10;
		FixedFrameAnimation anim;
		ArrayList<ArrayList<FrameLink>> frameLinks = new ArrayList<>();
		
		
		FixedFramePanel(FixedFrameAnimation anim) {
			this.anim = anim;
			this.setOpaque(false);
			constructLayout();
			this.addMouseListener(mouseAdapter);
			this.addMouseMotionListener(mouseAdapter);
		}
		

		private class FramePanel extends BaseTPanel {}
		private class MarkerPanel extends JPanel {
			final JLabel label = new JLabel();
			
			MarkerPanel() {
				GroupLayout layout = new GroupLayout(this);
				
				label.setFont(new Font("Dialog.bold",Font.BOLD, 10));
				
				layout.setHorizontalGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addComponent(label)
					.addContainerGap());
				layout.setVerticalGroup(layout.createParallelGroup()
					.addComponent(label, MARKER_HEIGHT, MARKER_HEIGHT, MARKER_HEIGHT));
				
				this.setLayout(layout);
			}
		}
		private class TickPanel extends JPanel {
			private final JLabel label = new JLabel();
			private int tick;
			TickPanel() {
				this.setOpaque(false);
				GroupLayout layout = new GroupLayout(this);
				
				label.setFont(new Font("Dialog.bold",Font.BOLD, 10));
				label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				label.setHorizontalAlignment(JLabel.CENTER);
				
				layout.setHorizontalGroup(layout.createSequentialGroup()
					.addGap(2)
					.addComponent(label, 0, 0, Short.MAX_VALUE)
					.addGap(2));
				layout.setVerticalGroup(layout.createSequentialGroup()
					.addGap(2)
					.addComponent(label, 0, 0, Short.MAX_VALUE)
					.addGap(2));
				
				this.setLayout(layout);
			}
			void setTick( int tick) {
				this.tick = tick;
				label.setText(""+tick);
			}
		}
		
		private class LinkButton extends JToggleButton implements ActionListener {
			private final AnimationLayer layer;
			LinkButton(AnimationLayer layer) {
				this.layer = layer;
				this.setOpaque(false);
				this.setBackground(new Color(0,0,0,0));
				this.setBorder(null);

				this.setIcon(Globals.getIcon("icon.link"));
				this.setSelectedIcon(Globals.getIcon("icon.unlink"));
				
				this.addActionListener(this);
			}
			@Override
			public void actionPerformed(ActionEvent arg0) {
				
//				manager.getAnimationState(animation).setExpanded(isSelected());
			}
		}

		
		@Override
		protected void paintComponent(Graphics g) {
			Node selected = workspace.getSelectedNode();
			if( !(selected instanceof LayerNode))
				selected = null;
			
			// Paint Background behind current frame
			if( manager.getSelectedAnimation() == anim) {
				int tick = (int) Math.floor( manager.getAnimationState(anim).getMetronom());
				
				if( tick >= anim.getStart() && tick <= anim.getEnd()) {
					g.setColor(tickColor);
					int dy = LABEL_HEIGHT + TICK_HEIGHT*(tick - anim.getStart());
					g.fillRect(0, dy, getWidth(), TICK_HEIGHT);
				}
			}
			
			// Paint Background behind Nodes
			for( List<FrameLink> list : frameLinks) {
				for( FrameLink fl : list) {
					fl.behavior.drawBackground(g, new Rectangle(
							fl.component.getX()-OUTLINE_WIDTH, fl.component.getY(),
							fl.component.getWidth()+OUTLINE_WIDTH, fl.component.getHeight()));
				}
			}
			
			super.paintComponent(g);
			
			if( state != null)
				state.onDraw(g);
		}
		

		// :::: Coordinate Helping Methods
		public Rectangle boundsFromLink(FrameLink fl) {
			return new Rectangle(fl.component.getX()-OUTLINE_WIDTH, fl.component.getY(),
								fl.component.getWidth()+OUTLINE_WIDTH, fl.component.getHeight());
		}
		
		public FrameLink linkAt( Point p) {

			for( List<FrameLink> list : frameLinks) {
				for( FrameLink fl : list) {
					if( boundsFromLink(fl).contains(p))
						return fl;
				}
			}
			return null;
		}
		
		/**
		 * The layout of the FixedFramePanel mimics a collection of 
		 * parallel trees using a dynamically-constructed GroupLayout, 
		 * with each tree corresponding to an Animation layer whose nodes
		 * correspond to animation elements, aligned such that all nodes 
		 * on the same horizontal line appear on the same frame.
		 * 
		 * The exception being Asynchronous Layers, which have their own
		 * heights.
		 */
		private void constructLayout() {
			softResetCache();
			
			List<AnimationLayer> layers = anim.getLayers();
			
			frameLinks.clear();
			this.removeAll();
			
			// Should possibly be GridBagLayout, but I feel more comfortable with
			//	 GroupLayout.
			GroupLayout layout = new GroupLayout(this);
			
			Group horizontal = layout.createSequentialGroup();
			Group vertical = layout.createParallelGroup();

			// Add the list of Frame Ticks
			Group subHor = layout.createParallelGroup();
			Group subVert = layout.createSequentialGroup();
			
			subVert.addGap(LABEL_HEIGHT);
			for( int met= anim.getStart(); met < anim.getEnd(); ++met) {
				TickPanel panel = getFromTPCache();
				panel.setTick(met);
				subVert.addComponent(panel, TICK_HEIGHT, TICK_HEIGHT, TICK_HEIGHT);
				subHor.addComponent(panel, TICK_WIDTH, TICK_WIDTH, TICK_WIDTH);
			}
			horizontal.addGroup(subHor);
			vertical.addGroup(subVert);
			
			for( int index=0; index<layers.size(); ++index) {
				List<Frame> frames = layers.get(index).getFrames();
				ArrayList<FrameLink> links = new ArrayList<>(frames.size());
				
				// Add the Outline (part to the left of a Layer which shows the "tree" path
				FFPOutline outline = new FFPOutline(index);
				horizontal.addComponent(outline, OUTLINE_WIDTH,OUTLINE_WIDTH,OUTLINE_WIDTH);
				vertical.addComponent(outline);
				
				subHor = layout.createParallelGroup();
				subVert = layout.createSequentialGroup();

				// Add the Label for the Animation Layer
				JLabel label = new JLabel("Animation Layer");
				LinkButton linkButton = new LinkButton(layers.get(index));
				
				subHor.addGroup(layout.createSequentialGroup()
					.addComponent(linkButton, OUTLINE_WIDTH, OUTLINE_WIDTH, OUTLINE_WIDTH)
					.addGap(2)
					.addComponent(label));
				subVert.addGroup(layout.createParallelGroup()
						.addComponent(linkButton,LABEL_HEIGHT,LABEL_HEIGHT,LABEL_HEIGHT) 
						.addComponent(label, LABEL_HEIGHT,LABEL_HEIGHT,LABEL_HEIGHT));

				int dy = LABEL_HEIGHT;
				for( Frame frame : frames) {

					FrameTypeBehavior behavior = null;
					
					switch( frame.getMarker()) {
					case END_AND_LOOP:
						behavior = new EndMarkerBehavior();
						break;
					case END_LOCAL_LOOP:
					case START_LOCAL_LOOP:
					case NIL_OUT:
						break;
					case FRAME:
						behavior = new FrameFrameBehavior(frame);
						break;
					}
					if( behavior != null) {
						int h = behavior.getHeight();
						if( h == 0) continue;
						
						Component component = behavior.buildNode();
						component.setPreferredSize(new Dimension(0,h));
						
						subHor.addComponent(component);
						subVert.addComponent(component,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE);
						
						FrameLink link = new FrameLink(
								dy + behavior.getTreeOffset(), behavior, component);
						behavior.setLink(link);
						links.add( link);
						dy += h;
					}
				}
				
				horizontal.addGroup(subHor);
				vertical.addGroup(subVert);
				
				frameLinks.add(links);
			}
			
			layout.setVerticalGroup(vertical);
			layout.setHorizontalGroup(horizontal);
			this.setLayout(layout);
		}

		// For quicker construction certain Nodes are cached and re-used.
		//	Note: as of now shortenned caches are never freed, but they should
		//	be removed when the component is removed anyway
		ArrayList<FramePanel> fpCache = new ArrayList<>();
		ArrayList<MarkerPanel> mpCache = new ArrayList<>();
		ArrayList<TickPanel> tpCache = new ArrayList<>();
		int fpcMet;
		int mpcMet;
		int tpcMet;
		private void softResetCache() {
			fpcMet = 0;
			mpcMet = 0;
			tpcMet = 0;
		}
		private FramePanel getFromFPCache() {
			FramePanel panel;
			if(fpcMet >= fpCache.size()) {
				panel = new FramePanel();
				fpCache.add(panel);
			}
			else 
				panel = fpCache.get(fpcMet);
			++fpcMet;
			return panel;
		}
		private MarkerPanel getFromMPCache() {
			MarkerPanel panel;
			if(mpcMet >= mpCache.size()) {
				panel = new MarkerPanel();
				mpCache.add(panel);
			}
			else 
				panel = mpCache.get(mpcMet);
			++mpcMet;
			return panel;
		}
		private TickPanel getFromTPCache() {
			TickPanel panel;
			if(tpcMet >= tpCache.size()) {
				panel = new TickPanel();
				tpCache.add(panel);
			}
			else 
				panel = tpCache.get(tpcMet);
			++tpcMet;
			return panel;
		}
		
		/**
		 * Defines both how to construct a layer as well as the behavior
		 * it should perform on certain contextual events.
		 */
		private abstract class FrameTypeBehavior {
			protected Component component;
			protected FrameLink link;
			final public Component buildNode() {
				component = _buildNode();
				return component;
			}
			final public void setLink(FrameLink link) {
				this.link = link;
			}
			protected abstract Component _buildNode();
			public abstract int getHeight();
			public abstract int getTreeOffset();
			public abstract void onPress( MouseEvent evt);
			public void onMouseover(MouseEvent evt) {}
			public void drawBackground( Graphics g, Rectangle bounds) {}
		}
		
		/** Behavior for normal frames. */
		private class FrameFrameBehavior extends FrameTypeBehavior {
			public final Frame frame;
			
			FrameFrameBehavior( Frame frame) {
				assert( frame.getMarker() == Marker.FRAME);
				this.frame = frame;
			}

			@Override
			public void drawBackground(Graphics g, Rectangle bounds) {
				if( workspace.getSelectedNode() == frame.getLayerNode()) {
					g.setColor(pseudoselectColor);
					g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
				}
			}
			@Override
			protected Component _buildNode() {
				FramePanel panel = getFromFPCache();
				
				panel.startLabel.setText(""+frame.getStart());
				panel.endLabel.setText(""+frame.getEnd());
				
				return panel;
			}
			
			@Override
			public int getHeight() {
				return NODE_HEIGHT*frame.getLength();
			}
			@Override
			public int getTreeOffset() {
				return NODE_HEIGHT/2;
			}
			@Override
			public void onPress( MouseEvent evt) {
				if( Math.abs(evt.getY() - component.getHeight()) < 5) {
					startState( new ResizingFrameState(link, frame) );
				}
				else
					workspace.setSelectedNode(frame.getLayerNode());
			}
			@Override
			public void onMouseover(MouseEvent evt) {
				
				if( Math.abs(evt.getY() - component.getHeight()) < 5) {
					setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
				}
				else {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				}
			}
		}
		
		
		/** Behavior for the end marker. */
		private class EndMarkerBehavior extends FrameTypeBehavior {
			@Override
			protected Component _buildNode() {
				MarkerPanel panel = getFromMPCache();
				panel.label.setVerticalAlignment(JLabel.CENTER);
				panel.label.setText("End and Loop");
				return panel;
			}

			@Override
			public int getHeight() {
				return MARKER_HEIGHT;
			}
			@Override
			public int getTreeOffset() {
				return MARKER_HEIGHT/2;
			}

			@Override
			public void onPress(MouseEvent evt) {}
		}

		
		
		/** FrameLink is a helper-class that memorizes the link between
		 * a frame and their visual presence in the UI.
		 */
		private class FrameLink {
			final int ypos;
			FrameTypeBehavior behavior;
//			final Frame frame;
			final Component component;
			FrameLink( int y, FrameTypeBehavior behavior, Component component) 
			{this.ypos = y; this.behavior = behavior; this.component = component;}
		}
		
		private State state = null;
		void startState( State state) {
			if( this.state != null) {
				// Add onEnd here if it ever becomes needed
			}
			this.state = state;
			state.onStart();
		}
		
		private abstract class State {
			abstract void onStart();
			void onDrag(MouseEvent evt) {}
			void onRelease(MouseEvent evt) { end();}
			void onDraw( Graphics g) {}
			final void end() {state = null; repaint();}
		}
		private class ResizingFrameState extends State {
			private final FrameLink link;
			private final Frame frame;
			private final int start;
			private int end ;
			
			ResizingFrameState( FrameLink link, Frame frame) {
				this.link = link;
				this.frame = frame;
				this.start = frame.getStart();
				end = start+1;
			}
			@Override void onStart() {}
			@Override
			void onRelease(MouseEvent evt) {
				frame.setLength(end - start);
				super.onRelease(evt);
			}
			@Override
			void onDrag( MouseEvent evt) {
				int i = frameFromY(evt.getY());
				
				
				if( i >= start) end = i+1;
				else end = start;

				link.component.setPreferredSize(new Dimension(0,TICK_HEIGHT*(end-start)));
				doLayout();
				revalidate();
			}

			@Override
			void onDraw(Graphics g) {
				Graphics2D g2 = (Graphics2D)g;
				Stroke old = g2.getStroke();
				g2.setStroke(new BasicStroke(2));
				g.setColor(Color.BLACK);
				g.drawRect(0, getFrameY(start), getWidth(),TICK_HEIGHT*(end-start));
				g2.setStroke(old);
			}
		}
		
		private int frameFromY( int y) {
			if( y < LABEL_HEIGHT) return -1;
			
			int index = (y - LABEL_HEIGHT) / TICK_HEIGHT;
			
			return index;
		}
		
		private int getFrameY( int index) {
			if( index == -1) return 0;
			return LABEL_HEIGHT + TICK_HEIGHT*index;
		}
		private Rectangle getFrameBounds( int index) {
			
			return new Rectangle(0, LABEL_HEIGHT + TICK_HEIGHT*index, getHeight(), LABEL_HEIGHT + TICK_HEIGHT*(index+1));
		}
		
		
		class FFPOutline extends JPanel
		{
			final int col;
			FFPOutline( int col) {
				this.setOpaque(false);
				this.col = col;
			}
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				List<FrameLink> links = frameLinks.get(col);
				
				if(! links.isEmpty()) {
					g.setColor( new Color( 160,160,220));
					g.drawLine(3, LABEL_HEIGHT, 3, 
							frameLinks.get(col).get(frameLinks.get(col).size()-1).ypos);
					
					for( FrameLink fl : frameLinks.get(col)){
						g.drawLine(3, fl.ypos, 8, fl.ypos);
					}
				}
			}
		}


		// :::: AnimNodeBuilder
		@Override
		public void updateComponent() {
			constructLayout();
		}
		
		@Override
		public Component getComponent() {
			return this;
		}

		// :::: MouseListener
		private final MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override public void mousePressed(MouseEvent evt) {
				// Probably unnecessary
				Point p = SwingUtilities.convertPoint(
						evt.getComponent(), evt.getPoint(), FixedFramePanel.this);
				
				for( Component c : getComponents()) {
					if( (c instanceof TickPanel) && c.getBounds().contains(p)) {
						if( manager.getSelectedAnimation() != anim)
							manager.setSelectedAnimation(anim);
						
						manager.getAnimationState(anim).setMetronome(((TickPanel)c).tick);
					}
				}
				
				FrameLink fl = linkAt(p);
				
				if( fl != null) {
					fl.behavior.onPress( SwingUtilities.convertMouseEvent(evt.getComponent(), evt, fl.component));
				}
			}
			
			public void mouseMoved(MouseEvent evt) {
				// Probably unnecessary
				Point p = SwingUtilities.convertPoint(
						evt.getComponent(), evt.getPoint(), FixedFramePanel.this);

				FrameLink fl = linkAt(p);
				if( fl != null) {
					fl.behavior.onMouseover( SwingUtilities.convertMouseEvent(evt.getComponent(), evt, fl.component));
				}
				
			};
			public void mouseDragged(MouseEvent evt) {
				if( state != null)
					state.onDrag(evt);
			};
			
			public void mouseReleased(MouseEvent evt) {
				if( state != null) state.onRelease(evt);
			};
		};

	}
	
	
	// Called from AnimSchemePanel's OmniContainer.onCleanup
	void cleanup() {
		master.removeWorkspaceObserver(this);
		if( workspace != null) {
			workspace.removeSelectionObserver(this);
			manager.removeAnimationStructureObserver(this);
		}
	}

	// :::: AnimationStructureObserver
	@Override
	public void animationAdded(AnimationStructureEvent evt) {
		triggerReconstruct(evt);
	}

	@Override
	public void animationRemoved(AnimationStructureEvent evt) {
		triggerReconstruct(evt);
	}

	@Override
	public void animationChanged(AnimationStructureEvent evt) {
		triggerReconstruct(evt);
	}
	
	boolean reconstructed;
	public void triggerReconstruct( AnimationStructureEvent evt) {
		for( Entry<Animation,AnimNodeBuilder> entry: builderMap.entrySet()){
			if( entry.getKey() == evt.getAnimation()) {
				entry.getValue().updateComponent();
			}
		}
		
		// Attempt to minimize reconstructs in the case multiple triggers happen
		//	in quick succession (before the Panel needs to be redrawn)
		reconstructed = false;
		SwingUtilities.invokeLater( new Runnable() {@Override public void run() {
			if( !reconstructed) {
				reconstructed = true;
				reconstruct();
			}
		}});	
	}

	// :::: MSelectionObserver
	@Override
	public void selectionChanged(Node newSelection) {
		repaint();
	}

	// :::: MWorkspaceObserver
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( workspace != null) {
			workspace.removeSelectionObserver(this);
			manager.removeAnimationStructureObserver(this);
			manager.removeAnimationStateObserver(this);
		}
		workspace = selected;
		if( workspace != null) {
			manager = workspace.getAnimationManager();
			manager.addAnimationStructureObserver(this);
			manager.addAnimationStateObserver(this);
			workspace.addSelectionObserver(this);
		} else {
			manager = null;
		}
		reconstruct();
	}
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}

	// :::: MAnimationStateObserver
	@Override
	public void selectedAnimationChanged(MAnimationStateEvent evt) {
		repaint();
	}

	@Override
	public void animationFrameChanged(MAnimationStateEvent evt) {
		repaint();
	}


	
}