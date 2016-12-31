package spirite.image_data;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupTree {
	private GroupNode root;
	private ImageWorkspace context;
	
	public GroupTree( ImageWorkspace context) {
		this.context = context;
		root = new GroupNode(null);
	}
	
	// :::: Get
	public Node getRoot() {
		return root;
	}
	
	// :::: Add
	public void addContextual( Node selected, String group_name) {
		GroupNode to_add = new GroupNode(group_name);
		_addContextual( selected, to_add);
	}
	public void addContextual( Node selected, Rig rig) {
		RigNode to_add = new RigNode(rig);
		_addContextual( selected, to_add);
	}
	
	private void _addContextual( Node selected, Node to_add) {
		if( selected == null || selected == root) {
			to_add.parent = root;
			root.children.add( to_add);
		}
		if( selected instanceof GroupNode) {
			to_add.parent = selected;
			selected.children.add(to_add);
		}
		if( selected instanceof RigNode) {
			GroupNode parent = (GroupNode)selected.parent;
			to_add.parent = parent;
			selected.parent.children.add( parent.children.indexOf(selected), to_add);
		}
	}
	
	// :::: Moving Nodes
	public void moveAbove( Node nodeToMove, Node nodeAbove) {
		if( nodeToMove == null || nodeAbove == null || nodeAbove.parent == null 
				|| nodeToMove.parent == null || _isChild( nodeToMove, nodeAbove.parent))
			return;

		nodeToMove._del();
		nodeAbove.parent._add(nodeToMove, nodeAbove, true);
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		if( nodeToMove == null || nodeUnder == null || nodeUnder.parent == null 
				|| nodeToMove.parent == null || _isChild( nodeToMove, nodeUnder.parent))
			return;

		nodeToMove._del();
		nodeUnder.parent._add(nodeToMove, nodeUnder, false);
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto, boolean top) {
		if( nodeToMove == null || nodeInto == null || nodeToMove.parent == null 
				|| nodeToMove.parent == null || _isChild( nodeToMove, nodeInto))
			return;

		
		nodeToMove._del();
		if( top)
			nodeInto.children.add(0, nodeToMove);
		else
			nodeInto.children.add(nodeToMove);
			
		nodeToMove.parent = nodeInto;
	}
	
	// To make sure you don't try to move a node into one of it's children we perform this test.
	private boolean _isChild( Node node, Node nodeInto) {
		Node n = nodeInto;
		
		while( n != root && n != null) {
			if( n == node) return true;
			n = n.parent;
		}
		
		return false;
	}
	
	// ::: Nodes
	public class Node  {
		// !!!! TODO : Various attributes (such as oppacity) that apply to the group
		//	or the Rig (without altering them) should go here
		private float alpha;
		private boolean visible = true;
		private boolean expanded = true;

		
		// !!!! Note: even though RigNodes will never use it, it's still useful to have for 
		//	generic purposes
		ArrayList<Node> children = new ArrayList<>();
		private Node parent = null;
		
		public Node getParent() {
			return parent;
		}
		public List<Node> getChildren() {
			return (ArrayList<Node>)children.clone();
		}
		
		// :::: Get/Set
		public boolean isVisible() {
			return visible;
		}
		public void setVisible( boolean visisble) {
			this.visible = visible;
		}
		public boolean isExpanded() {
			return expanded;
		}
		public void setExpanded( boolean expanded) {
			this.expanded = expanded;
		}
		
		// For simplicity's sake (particularly regarding Observers), only the GroupTree
		//	has direct access to add/remove commands.
		protected void _add( Node toAdd, Node child, boolean above) {
			if( children == null) return;
			
			int i = children.indexOf(child);
			
			if( i == -1) return;
			
			children.add( i + (above?0:1), toAdd);
			toAdd.parent = this;
		}
		protected void _rem( Node toRem) {
			if( children == null) return;
			
			children.remove(toRem);
			if( toRem.parent == this)
				toRem.parent = null;
		}
		protected void _del() {
			if( parent == null) return;
			parent.children.remove(this);
		}
	}
	
	public class GroupNode extends Node {
		private String name;
		
		GroupNode( String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	public class RigNode extends Node {
		private Rig data;
		
		RigNode( Rig data) {
			this.data = data;
		}
		
		public Rig getRig() {
			return data;
		}
	}
}
