package spirite.panel_layers;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.GroupLayout.Alignment;
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

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.WarningType;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.dialogs.NewLayerDPanel.NewLayerHelper;
import spirite.image_data.Animation;
import spirite.image_data.AnimationManager;
import spirite.image_data.GroupTree;
import spirite.image_data.ImageHandle;
import spirite.image_data.GroupTree.GroupNode;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.GroupTree.NodeValidator;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.RenderEngine;
import spirite.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.image_data.ImageWorkspace.MImageObserver;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.ImageWorkspace.OpacityChange;
import spirite.image_data.ImageWorkspace.StructureChange;
import spirite.image_data.ImageWorkspace.VisibilityChange;
import spirite.image_data.RenderEngine.RenderSettings;
import spirite.image_data.animation_data.FixedFrameAnimation;
import spirite.ui.UIUtil;
import spirite.ui.ContentTree;
import spirite.ui.ContentTree.CCButton;

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
		
		this.context = context;


		if( workspace == null)
			nodeRoot = null;
		else
			nodeRoot = workspace.getRootNode();

		constructFromRoot();
		
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
    			dataUsed = ((LayerNode)obj).getLayer().getUsedImages();
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
    				dataUsed.addAll( ((LayerNode)lnode).getLayer().getUsedImages());
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
	public void structureChanged( StructureChange evt) {
		if( evt instanceof VisibilityChange) {
			Node changedNode = evt.getChangedNodes().get(0);	// should be no need to sanity check
			
			for( int i = 0; i<buttonPanel.getButtonRowCount(); ++i) {
				CCButton button = buttonPanel.getButtonAt( i, 0);
				Node node = (Node)((DefaultMutableTreeNode)button.getAssosciatedTreePath().getLastPathComponent()).getUserObject();
				if( node == changedNode) {
					button.setSelected(node.isVisible());
					break;
				}
			}
		}else if( evt instanceof OpacityChange) {
//			if( evt.get)
		}
		

		if( evt.isGroupTreeChange()) {
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
		

		// TODO: Fix the problem this is causing and re-implement
		_cfw_setExpandedStateRecursively( (DefaultMutableTreeNode)model.getRoot());
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
	private void _cfw_setExpandedStateRecursively( DefaultMutableTreeNode tree_node) {
		for( Enumeration<TreeNode> e = tree_node.children(); e.hasMoreElements();) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)e.nextElement();
			_cfw_setExpandedStateRecursively( child);
		}
		
		try {
			GroupTree.Node node = (GroupTree.Node)tree_node.getUserObject();
    		if( node.isExpanded() ) {
    			TreePath path =  new TreePath(tree_node.getPath());
    			tree.expandPath(path);
    		}
    		else {}
    		
    		if( node == workspace.getSelectedNode()) {
    			TreePath path =  new TreePath(tree_node.getPath());
    			tree.addSelectionPath(path);
    		}
    			
		} catch( ClassCastException e) {}
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
			GroupTree.Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			node.setVisible(button.isSelected());
		}
		if( button.buttonNum == 1) {

			Node node = getNodeFromPath( button.getAssosciatedTreePath());
			
			if( button.isSelected())
				workspace.addToggle(node);
			else
				workspace.remToggle(node);
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
					menuScheme.addAll(Arrays.asList(new String[][] {
								{"-"},
								{"&Construct Simple Animation From Group", "animfromgroup", null},
								{"&Delete  "+descriptor, "delete", null}, 
								{"&Add Group To Animation As New Layer", "animinsert", null}
					}));
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
			manager.linkAnimation(
					manager.addAnimation(new FixedFrameAnimation(group)),
					group);
			break;}
		case "animinsert":{
			GroupNode group = (GroupNode)contextMenu.node;
			AnimationManager manager = workspace.getAnimationManager();
			Animation anim  = manager.getSelectedAnimation();
			if( anim == null) break;
			
			anim.importGroup(group);
			manager.linkAnimation(anim, group);
			
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
			workspace.addNewSimpleLayer( workspace.getSelectedNode(), 
					helper.width, helper.height, helper.name, helper.color);
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
			
			workspace.addNewRigLayer(workspace.getSelectedNode(), 
					helper.width, helper.height, helper.name, helper.color);
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
			if( editingNode != null) {
				String text = renderPanel.label.getText();

				workspace.renameNode(editingNode, text);
			}
			
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
			
			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				UIUtil.drawTransparencyBG(g, null);
				
				if( node != null) {
					RenderSettings settings = new RenderSettings();
					settings.workspace = workspace;
					settings.width = getWidth();
					settings.height = getHeight();
					
					if( node instanceof LayerNode)
						settings.layer = ((LayerNode)node).getLayer();
					else if( node instanceof GroupNode)
						settings.node = (GroupNode)node;

					RenderingHints newHints = new RenderingHints(
				             RenderingHints.KEY_TEXT_ANTIALIASING,
				             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					newHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, 
							RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
					newHints.put( RenderingHints.KEY_INTERPOLATION, 
							RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					
					settings.hints = newHints;
					

					g.drawImage(renderEngine.renderImage(settings), 0, 0, null);
				}
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
		
		if( workspace != null)
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
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importBelow( Transferable trans, TreePath path) {
		try {
			workspace.moveBelow(nodeFromTransfer(trans), nodeFromPath(path));
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importInto( Transferable trans, TreePath path, boolean top) {
		try {
			workspace.moveInto(nodeFromTransfer(trans), (GroupNode)nodeFromPath(path), top);
			return true;
		} catch (Exception e) {
			MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
			return false;
		}
	}
	@Override
	protected boolean importOut( Transferable trans) {
			try {
				workspace.moveInto(nodeFromTransfer(trans), nodeRoot, false);
				return true;
			} catch (Exception e) {
				MDebug.handleWarning(WarningType.STRUCTURAL, this, "Bad Transfer (NodeTree)");
				return false;
			}
	}
}

