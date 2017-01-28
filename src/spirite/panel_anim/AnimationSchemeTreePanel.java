package spirite.panel_anim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import spirite.MDebug.ErrorType;
import spirite.MDebug.WarningType;
import spirite.MDebug;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.Animation;
import spirite.image_data.AnimationManager;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.AnimationManager.AnimationStructureEvent;
import spirite.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.animation_data.FixedFrameAnimation;
import spirite.image_data.animation_data.FixedFrameAnimation.AnimationLayer;
import spirite.image_data.animation_data.FixedFrameAnimation.Frame;
import spirite.ui.ContentTree;
import spirite.ui.UIUtil;

public class AnimationSchemeTreePanel extends ContentTree 
	implements TreeCellRenderer, MAnimationStructureObserver, MSelectionObserver, MWorkspaceObserver
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
			manager.addStructureObserver(this);
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
	
	private class SALFrame {
		int start;
		int end;
		LayerNode layer;
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
	
	// Back-up component for nodes that don't have anything more specific
	private final JLabel label = new JLabel();	
//	private final Component renderPanel = new TTree();
	
	class FixedFramePanel extends JPanel 
		implements AnimNodeBuilder
	{
		static final int LABEL_HEIGHT = 20;
		
		FixedFrameAnimation anim;
		FixedFramePanel(FixedFrameAnimation anim) {
			this.anim = anim;
			this.setOpaque(false);
			constructLayout();
		}
		
		class FFPOutline extends JPanel
		{
			int dy[];
			FFPOutline( int count) {
				this.setOpaque(false);
				dy = new int[count];
			}
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				g.setColor( new Color( 160,160,220));
				g.drawLine(3, LABEL_HEIGHT, 3, dy[dy.length-1]);
				
				for( int i=0; i<dy.length; ++i) {
					g.drawLine(3, dy[i], 8, dy[i]);
				}
			}
		}
		
		private void constructLayout() {
			List<AnimationLayer> layers = anim.getLayers();
			
			
			GroupLayout layout = new GroupLayout(this);
			
			Group horizontal = layout.createSequentialGroup();
			Group vertical = layout.createParallelGroup();
			
			for( AnimationLayer layer : layers) {
				List<Frame> frames = layer.getFrames();
				FFPOutline outline = new FFPOutline(frames.size());
				
				horizontal.addComponent(outline, 10,10,10);
				vertical.addComponent(outline);
				
				Group subHor = layout.createParallelGroup();
				Group subVert = layout.createSequentialGroup();
				
				JLabel label = new JLabel("Animation Layer");
				
				subHor.addComponent(label);
				subVert.addComponent(label, LABEL_HEIGHT,LABEL_HEIGHT,LABEL_HEIGHT);
				
				int i=0;
				int dy = 10 + LABEL_HEIGHT;
				for( Frame frame : frames) {
					outline.dy[i++] = dy;
					dy += 24;
					TitlePanel tp = new TitlePanel();
					tp.startLabel.setText(""+frame.start);
					tp.endLabel.setText(""+frame.end);
					subHor.addComponent(tp);
					subVert.addComponent(tp,24,24,24);
				}
				horizontal.addGroup(subHor);
				vertical.addGroup(subVert);
			}
			
			layout.setVerticalGroup(vertical);
			layout.setHorizontalGroup(horizontal);
			this.setLayout(layout);
		}
		
		@Override
		public void updateComponent() {
			constructLayout();
		}
		
		@Override
		public Component getComponent() {
			return this;
		}
	}
	
	
	
	
	// :::: TreeCellRenderer
	public interface AnimNodeBuilder  {
		public void updateComponent();
		public Component getComponent();
	}
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
			manager.removeStructureObserver(this);
		}
	}
	
	// :::: ContentTree
	private final Color pseudoselectColor = new Color( 190,160,140,120);
	
	@Override
	protected Color getColor(int row) {
		
		if( tree.isRowSelected(row))
			return super.getColor(row);
		
		return null;
/*		Object usrObj = 
				((DefaultMutableTreeNode)tree.getPathForRow(row).getLastPathComponent()).getUserObject();

		Node selected = (workspace == null) ? null : workspace.getSelectedNode();
		if( selected == null) return null;
		if( selected instanceof GroupNode) return null;
		
		if( usrObj instanceof SALFrame) {
			SALFrame frame = (SALFrame) usrObj;
			
			if( frame.layer.getLayer() == ((LayerNode)selected).getLayer()) {
				return pseudoselectColor;
			}
		}
		return super.getColor(row);*/
	}

	// :::: AnimationStructureObserver
	@Override
	public void animationStructureChanged(AnimationStructureEvent evt) {
		if( evt == null) {
			for( AnimNodeBuilder builder : builderMap.values())
				builder.updateComponent();
		}
		else {
			for( Object anim : evt.getAnimationsAffected()) {
				AnimNodeBuilder builder = builderMap.get(anim);
				if( builder != null)
					builder.updateComponent();
			}
		}
		
		reconstruct();
	}
	
	// :::: TreeSelectionListener (inherited from ContentTree)
	@Override
	public void valueChanged(TreeSelectionEvent evt) {
		super.valueChanged(evt);
		
		DefaultMutableTreeNode node = 
				(DefaultMutableTreeNode)evt.getPath().getLastPathComponent();
		
		Object obj = node.getUserObject();
		if( obj instanceof SALFrame) {
			SALFrame frame = (SALFrame)obj;
			
			workspace.setSelectedNode(frame.layer);
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
			manager.removeStructureObserver(this);
		}
		workspace = selected;
		if( workspace != null) {
			manager = workspace.getAnimationManager();
			manager.addStructureObserver(this);
			workspace.addSelectionObserver(this);
		} else {
			manager = null;
		}
		reconstruct();
	}
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}

}
