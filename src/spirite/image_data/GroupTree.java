package spirite.image_data;

import java.util.ArrayList;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.ErrorType;

/***
 * Though almost all ImageData goes through the group tree before being
 * displayed and/or manipulated, GroupTree should function primarily as
 * a container, not as an interface.
 * 
 * Note: "floating" Nodes which are nested inside a GroupTree, but are not
 *	linked to it through its root-branch system can exist for numerous 
 * 	reasons, such as for storing undo actions or because a UI component
 * 	hasn't re-constructed its data based on the GroupTree changes.
 * 
 * @author Rory Burks
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
	
	// To make sure you don't try to move a node into one of it's children we perform this test.
	boolean _isChild( Node node, Node nodeInto) {
		Node n = nodeInto;
		
		while( n != root && n != null) {
			if( n == node) return true;
			n = n.parent;
		}
		
		return false;
	}
	

	// Creates a depth-first sequential list of all the LayerNodes in the tree
	public List<LayerNode> getAllLayerNodes() {
		List<LayerNode> list = new ArrayList<>();
		_galn_rec( root, list);
		return list;
	}
	private void _galn_rec( GroupNode parent, List<LayerNode>list) {
		for( Node child : parent.getChildren()) {
			if( child instanceof LayerNode) {
				list.add((LayerNode) child);
			}
			else if( child instanceof GroupNode){
				_galn_rec( (GroupNode) child, list);
			}
		}
	}

	// Creates a depth-first sequential list of all the Nodes in the tree
	public List<Node> getAllNodes() {
		List<Node> list = new ArrayList<>();
		_gan_rec( root, list);
		return list;
	}
	private void _gan_rec( GroupNode parent, List<Node>list) {
		for( Node child : parent.getChildren()) {
			list.add( child);
			if( child instanceof GroupNode){
				_gan_rec( (GroupNode) child, list);
			}
		}
	}
	
	// ::: Nodes
	public abstract class Node  {
		// !!!! TODO : Various attributes (such as opacity) that apply to the group
		//	or the Node (without altering them) should go here
		protected float alpha = 1.0f;
		protected boolean visible = true;
		protected boolean expanded = true;
		protected String name = "";

		
		// !!!! Note: even though Non-Group Nodes will never use it, it's still useful 
		//	to have for generic purposes
		ArrayList<Node> children = new ArrayList<>();
		private Node parent = null;
		
		public Node getParent() {
			return parent;
		}
		
		@SuppressWarnings("unchecked")
		public List<Node> getChildren() {
			return (ArrayList<Node>)children.clone();
		}
		
		/***
		 * Gets the node that comes after you in the tree.  Or null if you're the last
		 * node.
		 */
		public Node getNextNode() {
			if( parent == null) 
				return null;
			
			List<Node> children = getParent().getChildren();
			int i = children.indexOf( this);
			if( i == -1) {
				MDebug.handleError( ErrorType.STRUCTURAL, this, "Group Tree malformation (Not child of own parent).");
				return null;
			}
			if( i == children.size()-1)
				return null;
			
			return children.get(i+1);
		}
		
		// :::: Get/Set
		public boolean isVisible() {
			return visible;
		}
		void setVisible( boolean visible) {
			if( this.visible != visible) {
				this.visible = visible;
			}
			
		}
		public float getAlpha() {
			return alpha;
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
		void setName(String name) {
			if( !this.name.equals(name)) {
				this.name = name;
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
