package spirite.pc.ui.panel_layers;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.brains.RenderEngine;
import spirite.base.file.AnimIO;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager;
import spirite.base.image_data.GroupTree;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.GroupTree.NodeValidator;
import spirite.base.image_data.ImageHandle;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.MSelectionObserver;
import spirite.base.image_data.ImageWorkspace.OpacityChange;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.ImageWorkspace.VisibilityChange;
import spirite.base.image_data.RawImage;
import spirite.base.image_data.animation_data.FixedFrameAnimation;
import spirite.hybrid.Globals;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.WarningType;
import spirite.pc.dialogs.NewLayerDPanel.NewLayerHelper;
import spirite.pc.graphics.ImageBI;
import spirite.pc.graphics.awt.AWTContext;
import spirite.pc.ui.ContentTree;
import spirite.pc.ui.UIUtil;


public class LayerTreePanel extends ContentTree 
	implements MImageObserver, MWorkspaceObserver, MSelectionObserver,
	 TreeSelectionListener, TreeExpansionListener, ActionListener
{
	private final LayersPanel context;
	public LayerTreePanel(MasterControl master, LayersPanel context) {
		super();
		this.master = master;
		this.renderEngine = master.getRenderEngine();
		
		// Add Observers
		workspace = master.getCurrentWorkspace();
		if( workspace != null) {
			workspace.addImageObserver(this);
			workspace.addSelectionObserver(this);
		}
		master.addWorkspaceObserver(this);
		
		// Set Tree Properties
		renderer = new LTPCellRenderer();
		editor = new LTPCellEditor(tree, renderer);
		tree.setCellRenderer(renderer);
		tree.setCellEditor(editor);
		tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.addTreeExpansionListener(this);
		tree.setEditable(true);
		setButtonsPerRow(2);

		scrollPane.setAutoscrolls(false);
		


		if( workspace == null)
			nodeRoot = null;
		else
			nodeRoot = workspace.getRootNode();

		constructFromRoot();

		this.context = context;
	}
	
	// LayerTreePanel only needs master to add and remove a WorkspaceObserver
	//	and to hook into the RenderEngine (for drawing the thumbnails)
	private final MasterControl master;
	private final RenderEngine renderEngine;
	ImageWorkspace workspace;	// Non-private because LayersPanel needs access to this
	
	private static final long serialVersionUID = 1L;
	private final LTPCellEditor editor;
	private final LTPCellRenderer renderer;
	protected GroupNode nodeRoot = null;
	

	

    
    // :::: Get/Set
    public GroupTree.Node getSelectedNode() {
    	return getNodeFromPath( tree.getSelectionPath());
    }
    
    
    protected GroupTree.Node getNodeFromPath( TreePath path) {
    	if( path != null) {
    		try {
	    		return (GroupTree.Node)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
	    	}catch (ClassCastException c) {
	    		MDebug.handleWarning( WarningType.STRUCTURAL, this, "Tree Node isn't a GroupTree Node");
	    	}
    	}

		return null;
    }
    
    // :::: MImageObserver interface
    @SuppressWarnings("unchecked")
	@Override
	public synchronized void imageChanged(ImageChangeEvent evt) {
    	Enumeration<DefaultMutableTreeNode> e =
    			((DefaultMutableTreeNode)tree.getModel().getRoot()).depthFirstEnumeration();

		List<ImageHandle> changedImages = evt.getChangedImages();
    	
    	while( e.hasMoreElements()) {
    		DefaultMutableTreeNode treeNode = e.nextElement();
    		Object obj = treeNode.getUserObject();
    		
    		Collection<ImageHandle> dataUsed;
    		
    		if( obj instanceof LayerNode) {
    			dataUsed = ((LayerNode)obj).getLayer().getImageDependencies();
    		}
    		else if( obj instanceof GroupNode) {
    			dataUsed = new LinkedHashSet<>();	// LinkedHashSet efficiently avoids duplicates
				
    			List<Node> layerNodes = ((GroupNode)obj).getAllNodesST( new NodeValidator() {
					@Override public boolean isValid(Node node) {
						return (node.isVisible() && node instanceof LayerNode);
					}
					@Override public boolean checkChildren(Node node) {
						return node.isVisible();
					}
				});
    			
    			for( Node lnode : layerNodes) {
    				dataUsed.addAll( ((LayerNode)lnode).getLayer().getImageDependencies());
    			}
			}
    		else
    			continue;
    		
    		// Test the intersection of changedImages and dataUsed to see if the node needs to be re-drawn
    		List<ImageHandle> intersection = new ArrayList<>(dataUsed);
    		intersection.retainAll(changedImages);
    		
    		if( !dataUsed.isEmpty()) {
				// !!!! TODO: This draws only the PART that is necessary, but
				// 	probably causes a lot of unnecessary code to execute internally
				//	there might be a far better Swing-intended way to repaint a
				//	particular node within the tree
				tree.repaint();
    		}
    	}
    }
	@Override
	public void structureChanged( StructureChangeEvent evt) {
		if( evt.change instanceof VisibilityChange) {
			Node changedNode = evt.change.getChangedNodes().get(0);	// should be no need to sanity check
			
			for( int i = 0; i<buttonPanel.getButtonRowCount(); ++i) {
				CCButton button = buttonPanel.getButtonAt( i, 0);
				Node node = (Node)((DefaultMutableTreeNode)button.getAssosciatedTreePath().getLastPathComponent()).getUserObject();
				if( node == changedNode) {
					button.setSelected(node.isVisible());
					break;
				}
			}
		}else if( evt.change instanceof OpacityChange) {
//			if( evt.get)
		}
		

		if( evt.change.isGroupTreeChange()) {
			constructFromRoot();
		}
	}
	
	// :::: MSelectionObserver
	@Override
	public void selectionChanged(Node newSelection) {
		TreePath path = getPathOfNode( newSelection);
		
		if(path != null)
			tree.setSelectionPath(path);
	}
	
	@SuppressWarnings("unchecked")
	private TreePath getPathOfNode( Node nodeToFind){
		TreeModel m = tree.getModel();
		
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)m.getRoot();
		Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
		
		while( e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			
			if( node.getUserObject() == nodeToFind) {
				return new TreePath(node.getPath());
			}
		}
		
		return null;
	}
	

	public void constructFromRoot() {
		constructFromNode( nodeRoot);
	}
	public void constructFromNode( GroupNode node) {
		startBuilding();
		root.removeAllChildren();
		if( node != null)
			_cfw_construcRecursively( node, root);
		model.nodeStructureChanged(root);
		
		_cfw_setExpandedState();
		transferHandler.stopDragging();
		finishBuilding();
	}
	
	private void _cfw_construcRecursively( GroupTree.Node group_node, DefaultMutableTreeNode tree_node) {
		for( GroupTree.Node child : group_node.getChildren()) {
			DefaultMutableTreeNode node_to_add = new DefaultMutableTreeNode(child);

			tree_node.add( node_to_add);
			
			_cfw_construcRecursively( child, node_to_add);
		}
		
		tree_node.setAllowsChildren( group_node instanceof GroupTree.GroupNode);
	}
	@SuppressWarnings("unchecked")
	private void _cfw_setExpandedState() {
		// A bit ugly, but the Tree Root is not an instance of GroupNode
		//	(as resetting it every time the workspace is changed would far more cumbersome)
		//	so the first step is done semi-manually
		for( Enumeration<TreeNode> e = ((DefaultMutableTreeNode)model.getRoot()).children(); e.hasMoreElements();) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
			_cfw_setExpandedStateRecursively( child);
		}
	}
	@SuppressWarnings("unchecked")
	private void _cfw_setExpandedStateRecursively( DefaultMutableTreeNode tree_node) {
		GroupTree.Node node = (GroupTree.Node)tree_node.getUserObject();
		if( node.isExpanded() ) {
			for( Enumeration<TreeNode> e = tree_node.children(); e.hasMoreElements();) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
				_cfw_setExpandedStateRecursively( child);
			}
			TreePath path =  new TreePath(tree_node.getPath());
			tree.expandPath(path);
		}
		else {}
		
		if( node == workspace.getSelectedNode()) {
			TreePath path =  new TreePath(tree_node.getPath());
			tree.addSelectionPath(path);
		}
	}
	
	// :::: Calld by Parent's OmniContainer.onCleanup
	void cleanup() {
		master.removeWorkspaceObserver(this);
		if( workspace != null) {
			workspace.removeImageObserver( this);
			workspace.removeSelectionObserver(this);
		}
	}
    
	// :::: ContentTree
	public Node nodeFromPath( TreePath path) {
		return (Node)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
	}
	
	protected Node nodeFromTransfer( Transferable trans ) throws UnsupportedFlavorException, IOException 
	{
		Node node = ((NodeTransferable)trans.getTransferData(FLAVOR)).node;
		return node;
	}
	

	@Override
	protected void buttonPressed(CCButton button) {
		if( button.buttonNum == 0) {
			// Visibility Button
			GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			node.setVisible(button.isSelected());
		}
		else if( button.buttonNum == 1) {
			// Link Button
			Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			if( button.isSelected()) {
				workspace.getStageManager().stageNode(node);
//				node.setRenderMethod(RenderMethod.COLOR_CHANGE, 0);
			}
			else {
				workspace.getStageManager().unstageNode(node);
//				node.setRenderMethod(RenderMethod.DEFAULT, 0);
			}
/*			if( button.isSelected())
				workspace.addToggle(node);
			else
				workspace.remToggle(node);*/
		}
	}
	@Override
	protected void buttonCreated(CCButton button) {
		if( button.buttonNum == 0) {
			GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			button.setSelected( node.isVisible());
			button.setIcon( Globals.getIcon("visible_off"));
			button.setSelectedIcon(Globals.getIcon("visible_on"));
		}
		else if( button.buttonNum == 1) {
			GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			button.setSelected( workspace.getStageManager().getNodeStage(node) != -1);
			
		}
	}
	@Override
	protected void clickPath(TreePath path, MouseEvent evt) {
		super.clickPath(path, evt);
		if( evt.getButton() == MouseEvent.BUTTON3) {
			
			// All-context menu items
			List<String[]> menuScheme = new ArrayList<>(
				Arrays.asList(new String[][] {
					{"&New..."},
					{".New Simple &Layer", "newLayer", "new_layer"},
					{".New Layer &Group", "newGroup", "new_group"},
					{".New &Rig Layer", "newRig", null}
				})
			);
			contextMenu.node = null;
			
			// Construct the base Context Menu
			
			if( path != null) {
				DefaultMutableTreeNode node = 
						(DefaultMutableTreeNode)path.getLastPathComponent();
				Object usrObj = node.getUserObject();
				contextMenu.node = (Node)usrObj;
				
				String descriptor = "...";
				if( usrObj instanceof GroupNode) {
					descriptor = "Layer Group";
				}
				if( usrObj instanceof LayerNode) {
					descriptor = "Layer";
				}
	
				// All-node related menu items
				menuScheme.addAll(Arrays.asList(new String[][] {
						{"-"},
						{"D&uplicate "+descriptor, "duplicate", null}, 
						{"&Delete  "+descriptor, "delete", null}, 
				}));
				
				// Add parts to the menu scheme depending on node type
				if( usrObj instanceof GroupNode) {
					menuScheme.add( new String[] {"-"});
					menuScheme.add( new String[] {"&Construct Simple Animation From Group", "animfromgroup", null});
					if( workspace.getAnimationManager().getSelectedAnimation() != null)
						menuScheme.add( new String[]{"&Add Group To Animation As New Layer", "animinsert", null});
					menuScheme.add( new String[] {"&Write Group To GIF Animation", "giffromgroup", null});
				}
				else if( usrObj instanceof LayerNode) {
					if( ((LayerNode) usrObj).getNextNode() instanceof LayerNode) {
						menuScheme.add( new String[]{"&Merge Layer Down", "mergeDown", null});
					}
				}
			}

			// Show the ContextMenu
			contextMenu.removeAll();
			UIUtil.constructMenu(contextMenu, menuScheme.toArray( new String[0][]), this);
			
			contextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}
	

	
	public static class NodeTransferable implements Transferable {
		public final Node node;
		NodeTransferable( Node node) {
			this.node = node;
		}
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if( flavor.equals(FLAVOR))	return this;
			else throw new UnsupportedFlavorException(flavor);
		}	
		@Override public DataFlavor[] getTransferDataFlavors() {return flavors;}
		@Override public boolean isDataFlavorSupported(DataFlavor flavor) {return flavor.equals(FLAVOR);}
	}
	final static public DataFlavor FLAVOR = new DataFlavor( NodeTransferable.class, "Group Tree Node");
	private static final DataFlavor flavors[] = {FLAVOR};
	
	@Override
	protected boolean allowsHoverOut() {
		return true;
	}
	@Override
	protected Transferable buildTransferable(DefaultMutableTreeNode node) {
		if( node.getUserObject() instanceof Node) {
			return new NodeTransferable((Node) node.getUserObject());
		}
		return super.buildTransferable(node);
	}
	@Override
	protected boolean validTransferable(DataFlavor[] dfs) {
		for( int i=0; i<dfs.length; ++i) {
			if(dfs[i] == FLAVOR) {
				return true;
			}
		}
		return false;
	}
	
	
	
	
	private final LTPContextMenu contextMenu = new LTPContextMenu();
	class LTPContextMenu extends JPopupMenu {
		Node node = null;
		
		public LTPContextMenu() {}
	}

	// :::: TreeExpansionListener
	@Override
	public void treeCollapsed(TreeExpansionEvent evt) {
		super.treeCollapsed(evt);
		
		// Store the Expanded state in the GroupTree so that the data remembers the UI state
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		if( node != null)
			node.setExpanded(false);
	}
	@Override
	public void treeExpanded(TreeExpansionEvent evt) {
		super.treeExpanded(evt);
		
		// Store the Expanded state in the GroupTree so that the data remembers the UI state
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		if( node != null)
			node.setExpanded(true);
		
	}
	

	// :::: WorkspaceObserver
	@Override
	public void currentWorkspaceChanged( ImageWorkspace current, ImageWorkspace previous) {
		contextMenu.node = null;
		
		// Remove assosciations with the old Workspace and add ones to the new
		if( workspace != null) {
			workspace.removeImageObserver(this);
			workspace.removeSelectionObserver(this);
		}
		workspace = current;
		
		if( current != null) {
			workspace.addImageObserver( this);
			workspace.addSelectionObserver(this);
		}

		if( workspace == null)
			nodeRoot = null;
		else
			nodeRoot = workspace.getRootNode();
		constructFromRoot();
	}
	@Override	public void newWorkspace( ImageWorkspace arg0) {}
	@Override	public void removeWorkspace( ImageWorkspace arg0) {}
	

	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		// ActionCommands from JPopupMenu
		switch( evt.getActionCommand()) {
		case "animfromgroup":{
			GroupNode group = (GroupNode)contextMenu.node;
			AnimationManager manager = workspace.getAnimationManager();
			manager.addAnimation(new FixedFrameAnimation(group));
			break;}
		case "giffromgroup":{
			GroupNode group = (GroupNode)contextMenu.node;
			try {
				AnimIO.exportGroupGif(group, new File("E:/test.gif"), 8);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;}
		case "animinsert":{
			GroupNode group = (GroupNode)contextMenu.node;
			AnimationManager manager = workspace.getAnimationManager();
			Animation anim  = manager.getSelectedAnimation();
			if( anim == null) break;
			
			anim.importGroup(group);
			
			break;}
		case "animBreakBind":
			AnimationManager manager = workspace.getAnimationManager();
//			manager.

			// TODO
			break;
		case "newGroup":
			workspace.addGroupNode(contextMenu.node, "New Group");
			break;
		case "newLayer": {
			NewLayerHelper helper = master.getDialogs().callNewLayerDialog(workspace);
			if( helper != null) {
				workspace.addNewSimpleLayer( workspace.getSelectedNode(), 
						helper.width, helper.height, helper.name, helper.color.getRGB());
			}
			break;}
		case "duplicate":
			workspace.duplicateNode(contextMenu.node);
			break;
		case "delete":
			workspace.removeNode(contextMenu.node);
			break;
		case "mergeDown":
			if( contextMenu.node instanceof LayerNode)	// should be unnecessary
				workspace.mergeNodes( contextMenu.node.getNextNode(), (LayerNode) contextMenu.node);
			break;
		case "newRig":{

			NewLayerHelper helper = master.getDialogs().callNewLayerDialog(workspace);
			
			if( helper != null) 
				workspace.addNewRigLayer(workspace.getSelectedNode(), 
						helper.width, helper.height, helper.name, helper.color.getRGB());
			break;}
		default:
			MDebug.log(evt.getActionCommand());
		}
	}
	
	/** TreeCellRender */
	private class LTPCellRenderer extends DefaultTreeCellRenderer {
		private static final long serialVersionUID = 1L;
		private final LayerTreeNodePanel renderPanel;
		
		LTPCellRenderer() {
			renderPanel = new LayerTreeNodePanel();
		}
		
		@Override
		public Component getTreeCellRendererComponent(
				JTree tree, 
				Object value, 
				boolean selected, 
				boolean expanded, 
				boolean leaf, 
				int row, 
				boolean hasFocus) 
		{
			
			
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object obj = node.getUserObject();

			// Determine what kind of data the node on the tree contains and then 
			//	alter the node visuals accordingly
			if( obj instanceof GroupTree.Node) {
				String str = ((GroupTree.Node)obj).getName();
				if( MDebug.DEBUG && obj instanceof GroupTree.LayerNode) {
					try {
						str += " " + ((GroupTree.LayerNode)obj).getLayer().getActiveData().handle.getID();
					}catch(Exception e) {}
				}
				renderPanel.label.setText( str);
			}

			if( obj instanceof GroupTree.Node) 
				renderPanel.ppanel.node = (GroupTree.Node)obj;
		
			return renderPanel;
		}
	}

	/** Tree Cell Editor */
	private class LTPCellEditor extends DefaultTreeCellEditor
		implements KeyListener 
	{
		private final LayerTreeNodePanel renderPanel;
		private GroupTree.Node editingNode = null;
		
		public LTPCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
			super(tree, renderer);
			renderPanel = new LayerTreeNodePanel();
			renderPanel.label.addKeyListener(this);
		}
		
		
		@Override
		public Component getTreeCellEditorComponent(
				JTree tree, 
				Object value, 
				boolean isSelected, 
				boolean expanded,
				boolean leaf, 
				int row) 
		{
			Object obj = ((DefaultMutableTreeNode)value).getUserObject();

			// Determine what kind of data the node on the tree contains and then 
			//	alter the node visuals accordingly
			if( obj instanceof GroupTree.Node) {
				editingNode = (GroupTree.Node)obj;
				renderPanel.label.setText( editingNode.getName());
			}

			if( obj instanceof GroupTree.Node) 
				renderPanel.ppanel.node = (GroupTree.Node)obj;
		
			return renderPanel;
		}
		
		@Override
		public void cancelCellEditing() {
			super.cancelCellEditing();
			saveText();
		}
		
		@Override
		public boolean stopCellEditing() {
			return super.stopCellEditing();
		}
		
		private void saveText() {
			if( editingNode != null)
				editingNode.setName(renderPanel.label.getText());
			
			editingNode = null;
		}
		
		@Override
		public boolean isCellEditable(EventObject evt) {
			if( evt == null)
				return true;
			return false;
		}


		// :::: KeyListener
		@Override
		public void keyPressed(KeyEvent e) {
			if( e.getKeyCode() == KeyEvent.VK_ENTER) {
				this.cancelCellEditing();
			}
		}
		@Override		public void keyReleased(KeyEvent e) {}
		@Override		public void keyTyped(KeyEvent e) {}
	}

	
	
	/** Tree Node Panels */
	class LayerTreeNodePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final JTextField label;
		private final LTNPPanel ppanel;

		static final int N = 8;
		/** The Panel that has the Image Thumbnail in it. */
		class LTNPPanel extends JPanel {
//			public ImageData image = null;
			GroupTree.Node node = null;

			LTNPPanel() {
				this.setOpaque(false);
			}
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				
				
				if( node != null) {
					RawImage img = renderEngine.accessThumbnail(node, ImageBI.class);
					if( img != null) {
						UIUtil.drawTransparencyBG(g, new Rectangle( img.getWidth(), img.getHeight()));
						(new AWTContext(g, getWidth(), getHeight())).drawImage( img, 0, 0);
					}
					else 
						UIUtil.drawTransparencyBG(g, null);
				}
				else 
					UIUtil.drawTransparencyBG(g, null);
			}
			
		}
		
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

	// :::: TreeSelectionListener inherited from ContentTree
	@Override
	public void valueChanged(TreeSelectionEvent evt) {
		super.valueChanged(evt);
		
		// Called whenever the user has selected a new tree node, updates the 
		//	 Workspace so that the  active part (the part that gets drawn on) 
		//	 is changed.
		GroupTree.Node node = getNodeFromPath( evt.getPath());
		
		if( workspace != null && workspace.nodeInWorkspace(node))
			workspace.setSelectedNode(node);
		if( context != null)
			context.updateSelected();
	}

	// :::: ContentTree
	@Override
	protected boolean importAbove( Transferable trans, TreePath path) {
		try {
			workspace.moveAbove( nodeFromTransfer(trans), nodeFromPath(path));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree) ia");
			return false;
		}
	}
	@Override
	protected boolean importBelow( Transferable trans, TreePath path) {
		try {
			workspace.moveBelow(nodeFromTransfer(trans), nodeFromPath(path));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree) ib");
			return false;
		}
	}
	@Override
	protected boolean importInto( Transferable trans, TreePath path, boolean top) {
		try {
			workspace.moveInto(nodeFromTransfer(trans), (GroupNode)nodeFromPath(path), top);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree) ii");
			return false;
		}
	}
	@Override
	protected boolean importOut( Transferable trans) {
			try {
				workspace.moveInto(nodeFromTransfer(trans), nodeRoot, false);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree) io");
				return false;
			}
	}
}

