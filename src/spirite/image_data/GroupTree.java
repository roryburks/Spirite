package spirite.image_data;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.draw_engine.UndoEngine;
import spirite.image_data.ImageWorkspace.StructureChange;

/***
 * @author Rory Burks
 *
 */
public class GroupTree {
	private GroupNode root;
	ImageWorkspace context;
	
	public GroupTree( ImageWorkspace context) {
		this.context = context;
		root = new GroupNode(null);
	}
	
	// :::: Get
	public GroupNode getRoot() {
		return root;
	}
	
	// :::: Moving Nodes
	public void moveAbove( Node nodeToMove, Node nodeAbove) {
		if( nodeToMove == null || nodeAbove == null || nodeAbove.parent == null 
				|| nodeToMove.parent == null || _isChild( nodeToMove, nodeAbove.parent))
			return;

		nodeToMove._del();
//		nodeAbove.parent._add(nodeToMove, nodeAbove, true);
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		if( nodeToMove == null || nodeUnder == null || nodeUnder.parent == null 
				|| nodeToMove.parent == null || _isChild( nodeToMove, nodeUnder.parent))
			return;

		nodeToMove._del();
//		nodeUnder.parent._add(nodeToMove, nodeUnder, false);
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
		protected float alpha;
		protected boolean visible = true;
		protected boolean expanded = true;
		protected String name = "";

		
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
		
		public Node getNodeBefore() {
			if( parent == null) 
				return null;
			
			Node before;
			List<Node> children = getParent().getChildren();
			int i = children.indexOf( this);
			if( i == -1) {
				MDebug.handleError( ErrorType.STRUCTURAL, this, "Group Tree malformation (Not child of own parent).");
				return null;
			}
			if( i == 0)
				return null;
			
			return children.get(i-1);
		}
		
		// :::: Get/Set
		public boolean isVisible() {
			return visible;
		}
		public void setVisible( boolean visible, boolean undoable) {
			if( this.visible != visible) {
				this.visible = visible;
				
				if( undoable) {
					UndoEngine engine = context.getUndoEngine();
					engine.storeAction( engine.new VisibilityAction(this, visible), null);
				}
				context.refreshImage();
			}
			
		}
		public boolean isExpanded() {
			return expanded;
		}
		public void setExpanded( boolean expanded) {
			this.expanded = expanded;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			if( !this.name.equals(name)) {
				this.name = name;

				// Contruct and trigger the StructureChangeEvent
				StructureChange evt = null;
//				StructureChange evt = new StructureChange(context, StructureChangeType.RENAME);
//				context.triggerStructureChanged( evt);
			}
		}
		
		// For simplicity's sake (particularly regarding Observers), only the GroupTree
		//	has direct access to add/remove commands.
		protected void _add( Node toAdd, Node child) {
			int i = children.indexOf(child);
			
			if( i == -1) 
				children.add(toAdd);
			else
				children.add( i, toAdd);
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
		GroupNode( String name) {
			this.name = name;
		}
		
	}
	
	public class LayerNode extends Node {
		ImageData data;
		
		LayerNode( ImageData data, String name) {
			this.data = data;
			this.name = name;
		}
		
		public ImageData getImageData() {
			return data;
		}
	}
}
