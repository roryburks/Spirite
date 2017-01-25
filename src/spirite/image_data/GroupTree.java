package spirite.image_data;

import java.util.ArrayList;
import java.util.List;

import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.image_data.layers.Layer;

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
	private final GroupNode root;
	private final ImageWorkspace context;	// Might not be needed or belong
	
	GroupTree( ImageWorkspace context) {
		this.context = context;
		root = new GroupNode(null);
	}
	
	// :::: Get
	public GroupNode getRoot() {
		return root;
	}
	
	
	/** Tests to see if nodeP is a parent of nodeC. */
	boolean _isChild( Node nodeP, Node nodeC) {
		Node n = nodeC;
		
		while( n != root && n != null) {
			if( n == nodeP) return true;
			n = n.parent;
		}
		
		return false;
	}
	
	

	public interface NodeValidator {
		boolean isValid(Node node);
		boolean checkChildren(Node node);	// Note, root is always checkable
	}
	public static class NodeValidatorLayer implements NodeValidator {
		@Override public boolean isValid(Node node) {
			return node instanceof LayerNode;
		}
		@Override public boolean checkChildren(Node node) {return true;}
	}

	
	
	// ::: Nodes
	public abstract class Node  {
		protected float alpha = 1.0f;
		protected boolean visible = true;
		protected int x = 0;
		protected int y = 0;
		protected boolean expanded = true;
		protected String name = "";

		// :::: Get/Set
		public boolean isVisible() {
			return (visible && alpha > 0);
		}
		public void setVisible( boolean visible) {
			if( context.nodeInWorkspace(this) && this.visible != visible) {
				context.executeChange( context.new VisibilityChange(this, visible));
			}
			else if( context.verifyReference(this)) {
				this.visible = visible;
				context.triggerReferenceStructureChanged(false);
			}
			else { this.visible = visible;}
		}
		public float getAlpha() {
			return alpha;
		}
		public void setAlpha( float alpha) {
			if( context.nodeInWorkspace(this) && this.alpha != alpha) {
				context.executeChange( context.new OpacityChange( this, alpha));
			}
			else { this.alpha = alpha;}
		}
		public int getOffsetX() {
			return x;
		}
		public int getOffsetY() {
			return y;
		}
		public void setOffsetX( int x, int y) {
			if( context.nodeInWorkspace(this) && (this.x != x || this.y != y)) {
				context.executeChange( context.new OffsetChange( this, x, y));
			}
			else {this.x = x; this.y = y;}
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
			if( !this.name.equals(name)) 
				this.name = name;
		}

		
		// !!!! Note: even though Non-Group Nodes will never use it, it's still useful 
		//	to have for generic purposes
		private final ArrayList<Node> children = new ArrayList<>();
		private Node parent = null;
		
		public Node getParent() {
			return parent;
		}
		
		@SuppressWarnings("unchecked")
		public List<Node> getChildren() {
			return (ArrayList<Node>)children.clone();
		}

		/** Gets the depth of the node.  Child of root = 1.
		 * if the node is disjointed (isn't an ancestor of the root), returns -1.
		 */
		public int getDepth() {
			Node n = this;
			int i = 0;
			while( n != root) {
				++i;
				if( n == null) return -1;
				n = n.parent;
			}
			return i;
		}
		
		public int getDepthFrom( Node ancestor) {
			Node n = this;
			int i = 0;
			while( n != ancestor) {
				++i;
				if( n == null) return -1;
				n = n.parent;
			}
			return i;
		}
		
		/** Gets all children such that they pass the validator test, including
		 * subchildren if their parent pass the checkChildren test.
		 * 
		 * List is composed depth-first.
		 */
		public List<Node> getAllNodesST(NodeValidator validator) {
			List<Node> list = new ArrayList<>();
			_ganst( this, validator, list);
			return list;
		}
		private void _ganst( Node parent, NodeValidator validator, List<Node> list) {
			for( Node child :parent.getChildren()) {
				if( validator.isValid(child))
					list.add(child);
				
				if( validator.checkChildren(child)) 
					_ganst( child, validator, list);
			}
		}
		
		/** Creates a depth-first sequential list of all the Nodes in the tree. */
		public List<Node> getAllNodes() {
			// Could be implemented using getAllNodesST, but this is probably faster
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
		
		
		// For simplicity's sake (particularly regarding Observers), only the GroupTree
		//	has direct access to add/remove commands.
		protected void _add( Node toAdd, Node before) {
			int i = children.indexOf(before);
			
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
		Layer layer;
//		ImageData data;
		
		LayerNode( Layer layer, String name) {
			this.layer = layer;
			this.name = name;
		}
		
		public Layer getLayer() {
			return layer;
		}
	}
}
