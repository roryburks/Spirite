package spirite.panel_anim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.Animation;
import spirite.image_data.AnimationManager;
import spirite.image_data.AnimationManager.AnimationStructureEvent;
import spirite.image_data.AnimationManager.MAnimationStateEvent;
import spirite.image_data.AnimationManager.MAnimationStateObserver;
import spirite.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.animation_data.FixedFrameAnimation;
import spirite.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.image_data.animation_data.FixedFrameAnimation.Frame;
import spirite.image_data.animation_data.FixedFrameAnimation.Marker;
import spirite.ui.ContentTree;
import spirite.ui.UIUtil;

public class AnimationSchemeTreePanel extends ContentTree 
	implements TreeCellRenderer, MAnimationStructureObserver, MSelectionObserver, 
		MWorkspaceObserver, MAnimationStateObserver
{
	private static final long serialVersionUID = 1L;

	// MasterControl needed to add/remove WorkspaceObserver
	private final MasterControl master;
	private ImageWorkspace workspace = null;
	private AnimationManager manager = null;
	
	
	/**
	 * Create the panel.
	 */
	public AnimationSchemeTreePanel( MasterControl master) {
		this.master = master;
		tree.setCellRenderer(this);
		
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
		root.removeAllChildren();
		if( manager != null) {
			
			for( Animation animation : manager.getAnimations()) {
				DefaultMutableTreeNode anim = new DefaultMutableTreeNode(new AnimationTitle(animation));
				
				if( animation instanceof FixedFrameAnimation) 
					constructSimpleAnimationTree(anim, (FixedFrameAnimation)animation);
				
				root.add(anim);
			}
		}
		

		model.nodeStructureChanged(root);
		
/*		Enumeration e = ((DefaultMutableTreeNode)model.getRoot()).depthFirstEnumeration();
		
		while( e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
			model.nodeChanged(node);
		}*/
		
		UIUtil.expandAllNodes(tree);
	}
	
	private void constructSimpleAnimationTree( 
			DefaultMutableTreeNode into, 
			FixedFrameAnimation animation ) 
	{
		List<AnimationLayer> list = animation.getLayers();
		

		DefaultMutableTreeNode animationNode = 
				new DefaultMutableTreeNode( animation);
		
		into.add(animationNode);
	}
	
	private class AnimationTitle {
		final Animation animation;
		AnimationTitle( Animation anim) {
			this.animation = anim;
		}
	}
	
	
	class TitlePanel extends JPanel {
		JPanel imgPanel;
		JLabel titleLabel;
		JLabel startLabel;
		JLabel endLabel;
		
		TitlePanel() {
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

	private final Color pseudoselectColor = Globals.getColor("animSchemePanel.activeNodeBG");
	private final Color tickColor = Globals.getColor("animSchemePanel.tickBG");
	private final Color selectedAnimColor = Globals.getColor("contentTree.selectedBackground");
	// Back-up component for nodes that don't have anything more specific
	
	class FixedFramePanel extends JPanel 
		implements AnimNodeBuilder, MouseListener
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
			tree.addMouseListener(this);
			container.addMouseListener(this);
		}
		

		private class FramePanel extends TitlePanel {}
		private class MarkerPanel extends JPanel {
			final JLabel label = new JLabel();
			
			MarkerPanel() {
				GroupLayout layout = new GroupLayout(this);
				
				System.out.println(label.getFont().getFontName());
				
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
		
		@Override
		protected void paintComponent(Graphics g) {
			Node selected = workspace.getSelectedNode();
			if( !(selected instanceof LayerNode))
				selected = null;
			
			// Paint Background behind current frame
			if( manager.getSelectedAnimation() == anim) {
				int tick = (int) Math.floor( manager.getFrame());
				
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
				FFPOutline outline = new FFPOutline(index);
				
				horizontal.addComponent(outline, OUTLINE_WIDTH,OUTLINE_WIDTH,OUTLINE_WIDTH);
				vertical.addComponent(outline);
				
				subHor = layout.createParallelGroup();
				subVert = layout.createSequentialGroup();
				
				JLabel label = new JLabel("Animation Layer");
				
				subHor.addComponent(label);
				subVert.addComponent(label, LABEL_HEIGHT,LABEL_HEIGHT,LABEL_HEIGHT);

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
						
						subHor.addComponent(component);
						subVert.addComponent(component,h,h,h);
						links.add( new FrameLink(
								dy + behavior.getTreeOffset(), behavior, component));
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
			public abstract Component buildNode();
			public abstract int getHeight();
			public abstract int getTreeOffset();
			public abstract void onPress( MouseEvent evt);
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
			public Component buildNode() {
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
				workspace.setSelectedNode(frame.getLayerNode());
			}
		}
		
		
		/** Behavior for the end marker. */
		private class EndMarkerBehavior extends FrameTypeBehavior {
			@Override
			public Component buildNode() {
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
			public void onPress(MouseEvent evt) {
				// TODO Auto-generated method stub
				
			}
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
		@Override public void mouseEntered(MouseEvent evt) {}
		@Override public void mouseExited(MouseEvent evt) {}
		@Override public void mouseReleased(MouseEvent evt) {}
		@Override public void mouseClicked(MouseEvent evt) {}
		@Override public void mousePressed(MouseEvent evt) {
			// I despise JTree's Coordinate Space Methods.  Why am I even using
			//	this frustrating object when dealing with GroupLayouts is a 
			//	thousand times easier?
			Point p = SwingUtilities.convertPoint(
					evt.getComponent(), evt.getPoint(), tree);
			
			Rectangle r =
					tree.getPathBounds(tree.getClosestPathForLocation(p.x, p.y));
			
			if( !r.contains(p))
				return;
			p.x -= r.x;
			p.y -= r.y;
			
			for( Component c : getComponents()) {
				if( (c instanceof TickPanel) && c.getBounds().contains(p)) {
					manager.setFrame( ((TickPanel)c).tick);
				}
			}
			
			FrameLink fl = linkAt(p);
			
			if( fl != null) {
				fl.behavior.onPress( SwingUtilities.convertMouseEvent(evt.getComponent(), evt, fl.component));
			}
		}

	}
	
	
	
	
	// :::: TreeCellRenderer
	public interface AnimNodeBuilder  {
		public void updateComponent();
		public Component getComponent();
	}
	private final JLabel label = new JLabel();	
	private final TitlePanel labelPanel = new TitlePanel();
	private final HashMap<Animation,AnimNodeBuilder> builderMap = new HashMap<>();
	
	@Override
	public Component getTreeCellRendererComponent(
			JTree tree, 
			Object obj, 
			boolean selected, 
			boolean expanded, 
			boolean leaf, 
			int row, 
			boolean hasFocus) 
	{
		
		Object usrObj = ((DefaultMutableTreeNode)obj).getUserObject();
		
		if( usrObj instanceof AnimationTitle) {
			Animation sa = ((AnimationTitle)usrObj).animation;
			labelPanel.titleLabel.setText(sa.getName()+ " [Simple Animation]");
			labelPanel.startLabel.setText( Float.toString(sa.getStartFrame()));
			labelPanel.endLabel.setText(Float.toString(sa.getEndFrame()));
			return labelPanel;
		}else if( usrObj instanceof Animation) {
			Animation anim = (Animation)usrObj;
			AnimNodeBuilder builder = builderMap.get(anim);
			
			if( builder == null) {
				builder = createComponent(anim);
				builderMap.put(anim, builder);
			}
			
			
			return builder.getComponent();
		}else {
			label.setText("Animation Layer");
		
			return label;
		}
	}
	
	private AnimNodeBuilder createComponent( Animation anim) {
		if( anim instanceof FixedFrameAnimation) {
			return new FixedFramePanel((FixedFrameAnimation)anim);
		}
		return null;
	}
	
	// Called from AnimSchemePanel's OmniContainer.onCleanup
	void cleanup() {
		master.removeWorkspaceObserver(this);
		if( workspace != null) {
			workspace.removeSelectionObserver(this);
			manager.removeAnimationStructureObserver(this);
		}
	}
	
	// :::: ContentTree
	
	@Override
	protected Color getColor(int row) {

		Object usrObj = 
				((DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent()).getUserObject();

		
		if( usrObj instanceof AnimationTitle) {
			Animation selected = (manager == null) ? null : manager.getSelectedAnimation();
			
			if( selected != null && selected == ((AnimationTitle)usrObj).animation) {
				return selectedAnimColor;
			}
		}

		return null;
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
	
	// :::: TreeSelectionListener (inherited from ContentTree)
	@Override
	public void valueChanged(TreeSelectionEvent evt) {
		super.valueChanged(evt);
		
		DefaultMutableTreeNode node = 
				(DefaultMutableTreeNode)evt.getPath().getLastPathComponent();
		
		Object obj = node.getUserObject();
		
		if( obj instanceof AnimationTitle) {
			manager.setSelectedAnimation( ((AnimationTitle) obj).animation);
		}
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
