package spirite.panel_anim;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.AnimationManager;
import spirite.image_data.AnimationManager.AnimationStructureEvent;
import spirite.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.animation_data.AbstractAnimation;
import spirite.image_data.animation_data.SimpleAnimation;
import spirite.image_data.animation_data.SimpleAnimation.AnimationLayer;
import spirite.ui.ContentTree;
import spirite.ui.UIUtil;

public class AnimationSchemeTreePanel extends ContentTree 
	implements TreeCellRenderer, MAnimationStructureObserver, MSelectionObserver, MWorkspaceObserver
{
	private static final long serialVersionUID = 1L;

	ImageWorkspace workspace = null;
	AnimationManager manager = null;
	
	
	/**
	 * Create the panel.
	 */
	public AnimationSchemeTreePanel( MasterControl master) {
		
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
			
			for( AbstractAnimation animation : manager.getAnimations()) {
				DefaultMutableTreeNode anim = new DefaultMutableTreeNode(animation);
				
				if( animation instanceof SimpleAnimation) 
					constructSimpleAnimationTree(anim, (SimpleAnimation)animation);
				
				root.add(anim);
			}
		}
		

		model.nodeStructureChanged(root);
		UIUtil.expandAllNodes(tree);
	}
	
	private void constructSimpleAnimationTree( 
			DefaultMutableTreeNode into, 
			SimpleAnimation animation ) 
	{
		List<AnimationLayer> list = animation.getLayers();
		
		
		for( AnimationLayer layer : list) {
			DefaultMutableTreeNode layerNode = new DefaultMutableTreeNode(layer);

			List<LayerNode> frames = layer.getFrames();
			List<Integer> keys = layer.getKeyTimes();
			
			for( int i = 0; i < frames.size(); ++i) {
				SALFrame frame = new SALFrame();
				frame.start = keys.get(i);
				frame.end = keys.get(i+1);
				frame.layer = frames.get(i);
				DefaultMutableTreeNode child =  new DefaultMutableTreeNode(frame);
				layerNode.add(child);
			}
			
			into.add(layerNode);
		}
	}
	
	private class SALFrame {
		int start;
		int end;
		LayerNode layer;
	}

	JLabel label = new JLabel();
	
	class ASTNPanel extends JPanel {
		JPanel imgPanel;
		JLabel titleLabel;
		JLabel startLabel;
		JLabel endLabel;
		
		ASTNPanel() {
			setOpaque(false);
			
			imgPanel = new JPanel();
			titleLabel = new JLabel();
			startLabel = new JLabel();
			endLabel = new JLabel();

			startLabel.setBorder( BorderFactory.createLineBorder(Color.BLACK) );
			endLabel.setBorder( BorderFactory.createLineBorder(Color.BLACK) );
			startLabel.setHorizontalAlignment(JLabel.CENTER);
			endLabel.setHorizontalAlignment(JLabel.CENTER);
			
			GroupLayout layout = new GroupLayout(this);
			
			layout.setHorizontalGroup( layout.createSequentialGroup()
				.addGap(5)
				.addComponent(imgPanel, 32, 32,32)
				.addGap(5)
				.addComponent(titleLabel)
				.addGap(5)
				.addComponent(startLabel, 24, 24, 24)
				.addGap(5)
				.addComponent(endLabel, 24, 24, 24)
			);
			
			layout.setVerticalGroup( layout.createSequentialGroup()
				.addGap(5)
				.addGroup( layout.createParallelGroup( GroupLayout.Alignment.TRAILING)
					.addComponent(imgPanel, 32, 32, 32)
					.addComponent(titleLabel, 24, 24, 24)
					.addComponent(startLabel, 24, 24, 24)
					.addComponent(endLabel, 24, 24, 24)
				)
				.addGap(5)
			);
			
			this.setLayout(layout);
		}
	}
	
	ASTNPanel renderPanel = new ASTNPanel();
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
		
		if( usrObj instanceof SimpleAnimation) {
			SimpleAnimation sa = (SimpleAnimation)usrObj;
			renderPanel.titleLabel.setText(sa.getName()+ " [Simple Animation]");
			renderPanel.startLabel.setText( Float.toString(sa.getStartFrame()));
			renderPanel.endLabel.setText(Float.toString(sa.getEndFrame()));
			return renderPanel;
		}else if( usrObj instanceof SALFrame) {
			SALFrame frame = (SALFrame)usrObj;

			renderPanel.titleLabel.setText("");
			renderPanel.startLabel.setText(Integer.toString(frame.start));
			renderPanel.endLabel.setText(Integer.toString(frame.end));
			
			
			return renderPanel;
		}else {
			label.setText("Animation Layer");
		
			return label;
		}
	}
	
	// :::: ContentTree
	Color pseudoselectColor = new Color( 190,160,140,120);
	@SuppressWarnings("unchecked")
	@Override
	protected void paintCCPanel(Graphics g, int width, int height, boolean drawBG) {
		if( drawBG) {
			g.setColor( bgColor);
			g.fillRect( 0, 0, width-1, height-1);
		}
		
		
		Enumeration <DefaultMutableTreeNode> e =
				((DefaultMutableTreeNode)tree.getModel().getRoot()).depthFirstEnumeration();
		
		Node selected = (workspace == null) ? null : workspace.getSelectedNode();
		if( selected != null)  {		
			while( e.hasMoreElements()) {
				DefaultMutableTreeNode node = e.nextElement();
				
				if( node.getUserObject() instanceof SALFrame) {
					SALFrame frame = (SALFrame)node.getUserObject();
					
					if( frame.layer == selected) {
						Rectangle rect = tree.getPathBounds( new TreePath(node.getPath()));
						g.setColor(pseudoselectColor);
						g.fillRect(0, rect.y, width-1, rect.height-1);
					}
				}
			}
		}
		
		super.paintCCPanel(g, width, height, false);
	}

	// :::: AnimationStructureObserver
	@Override
	public void animationStructureChanged(AnimationStructureEvent evt) {
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
