package spirite.base.image_data;

import spirite.base.graphics.RenderProperties;
import spirite.base.graphics.renderer.RenderEngine.RenderMethod;
import spirite.base.image_data.layers.Layer;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

import java.util.ArrayList;
import java.util.List;

/***
 * A GroupTree is a generalized, abstract class for storing assorted image
 * data (mostly Layers, but can be expanded further) in a tree format (where
 * non-leaf nodes are specific "Group" nodes.  The primary storage
 * structure within ImageWorkspace is a GroupTree which
 * is used both to find the Data and determine if it "exists", but Group
 * trees outside of that can exist.  One must be careful with these 
 * group trees, though, as any MediumHandles referred to by them may be
 * de-contextualized as they are removed from the ImageWorkspace.
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
	
	// ===============
	// ==== Get
	public GroupNode getRoot() { return root; }
	
	/** Tests to see if nodeC is an ancestor of nodeP. */
	boolean _isChild( Node nodeP, Node nodeC) {
		Node n = nodeC;
		
		while( n != root && n != null) {
			if( n == nodeP) return true;
			n = n.parent;
		}
		
		return false;
	}
	
	/** A NodeValidator is an interface used for constructing a list of all 
	 * Nodes within a certain Node (and all subchildren) that pass th
	 * provided tests. */
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
	public abstract class Node  implements RenderProperties.Trigger{
		protected RenderProperties render;
		
		protected int x = 0;
		protected int y = 0;
		protected boolean expanded = true;
		protected String name = "";

		// !!!! Note: even though Non-Group Nodes will never use it, it's still useful 
		//	to have for generic purposes
		private final ArrayList<Node> children = new ArrayList<>();
		private Node parent = null;
		
		private Node() {
			render = new RenderProperties( this);
		}
		private Node( Node other, String name) {
			this.name = name;
			this.expanded = other.expanded;
			this.x = other.x;
			this.y = other.y;
			
			render = new RenderProperties(other.render, this);
		}

		// ==============
		// ==== Get/Set
		
		// :::: From RenderProperties.Trigger
		@Override
		public boolean visibilityChanged( boolean newVisible) {
			RenderProperties copy = new RenderProperties(this.render);
			copy.visible = newVisible;
			return _changeProperties(copy);
		}
		@Override
		public boolean alphaChanged ( float newAlpha) {
			RenderProperties copy = new RenderProperties(this.render);
			copy.alpha = newAlpha;
			return _changeProperties(copy);
		}
		@Override
		public boolean methodChanged(RenderMethod newMethod, int newValue) {
			RenderProperties copy = new RenderProperties(this.render);
			copy.method = newMethod;
			copy.renderValue = newValue;
			return _changeProperties(copy);
		}
		
		private boolean _changeProperties( RenderProperties newProps) {
			if( context.nodeInWorkspace(this)) {
				context.executeChange( context.new RenderPropertiesChange(this, newProps));
				return false;
			}
			else if( context.getReferenceManager().isReferenceNode(this))
				context.getReferenceManager().triggerReferenceStructureChanged(false);
			context.triggerFlash();
			return true;
		}
		
		
		public RenderProperties getRender() {return render;}
		
		public int getOffsetX() {return x;}
		public int getOffsetY() {return y;}
		public void setOffset( int x, int y) {
			if( context.nodeInWorkspace(this) && (this.x != x || this.y != y)) {
				context.executeChange( context.new OffsetChange( this, x, y));
			}
			else {this.x = x; this.y = y;}
		}
		
		public boolean isExpanded() {return expanded;}
		public void setExpanded( boolean expanded) {
			this.expanded = expanded;
		}
		
		public String getName() {return name;}
		public void setName(String name) {
			if( !this.name.equals(name)) {
				context.executeChange( context.new RenameChange(name, this));
			}
		}
		
		public ImageWorkspace getContext() {return context;}

		
		// =============
		// ==== Node Structure Querying
		public Node getParent() { return parent; }
		
		public List<Node> getChildren() {
			return new ArrayList<>(children);
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
		
		/** Returns how deep the node is from a given ancestor.
		 * Returns -1 if this node is not an ancestor of the other node. */
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
		
		/** Gets all LayerNodes within the given node, including itself. */
		public List<LayerNode> getAllLayerNodes() {
			// Sure we have Validators to do this, but this is common enough to
			// implement.  Plus it grabs itself it's a layer node.
			List<LayerNode> list = new ArrayList<>();
			
			if( this instanceof LayerNode) 
				list.add((LayerNode)this);
			else
				_galn( this, list);
			
			return list;
		}
		private void _galn( Node parent, List<LayerNode> list) {
			for( Node child :parent.getChildren()) {
				if( child instanceof LayerNode)
					list.add( (LayerNode) child);
				else
					_galn(child, list);

			}
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
		public List<Node> getAllAncestors() {
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
		
		/*** Gets the node that comes after this one in the GroupTree.  Or null
		 *  if this is the last node. */
		public Node getNextNode() {
			if( parent == null) 
				return null;
			
			List<Node> children = getParent().getChildren();
			int i = children.indexOf( this);
			if( i == -1) {
				MDebug.handleError( ErrorType.STRUCTURAL, "Group Tree malformation (Not child of own parent).");
				return null;
			}
			if( i == children.size()-1)
				return null;
			
			return children.get(i+1);
		}
		
		/** Gets the node that comes before this one (or null if it's the first) */
		public Node getPreviousNode() {
			if( parent == null) 
				return null;
			
			List<Node> children = getParent().getChildren();
			int index = children.indexOf(this);

			if( index == -1) {
				MDebug.handleError( ErrorType.STRUCTURAL, "Group Tree malformation (Not child of own parent).");
				return null;
			}
			if( index == 0)
				return null;
			return children.get(index-1);
			
		}
		
		
		// ====================
		// ==== Direct Adding/Removing Methods
		
		/** Adds a Subchild.
		 * NOTE! Should use ImageWorkspace.add* methods if you want the 
		 * UndoEngine to track the actions. */
		protected void _add( Node toAdd, Node before) {
			int i = children.indexOf(before);
			
			if( i == -1) 
				children.add(toAdd);
			else
				children.add( i, toAdd);
			toAdd.parent = this;
		}
		/** Removes a Subchild 
		 * NOTE! Should use ImageWorkspace.removeNode if you want the 
		 * UndoEngine to track the actions. */
		protected void _rem( Node toRem) {
			children.remove(toRem);
			if( toRem.parent == this)
				toRem.parent = null;
		}
		/** Removes this node from its parent.
		 * NOTE! Should use ImageWorkspace.removeNode if you want the 
		 * UndoEngine to track the actions. */
		protected void _del() {
			if( parent == null) return;
			parent.children.remove(this);
		}
		public Node getRoot() {
			Node node = this;
			while(node.parent != null)
				node = node.parent;
			return node;
		}
	}
	
	/** A GroupNode is a container node which accepts sub-children. */
	public class GroupNode extends Node {
		GroupNode( String name) {
			this.name = name;
		}
		GroupNode( GroupNode other, String name) {
			super( other, name);
		}
		
	}
	
	/** A LayerNode is a node container for a Layer.
	 * 
	 * Though LayerNodes can technically accept sub-children, doing so may 
	 * yield unexpected behavior.*/
	public class LayerNode extends Node {
		private final Layer layer;
		
		LayerNode( LayerNode other, Layer layer, String name) {
			super( other, name);
			this.layer = layer;
		}
		
		LayerNode( Layer layer, String name) {
			this.layer = layer;
			this.name = name;
		}
		
		public Layer getLayer() {
			return layer;
		}
	}
	
	public class AnimationNode extends Node {
		private final Animation animation;
		
		AnimationNode( Animation animation, String name) {
			this.animation = animation;
			this.name = name;
		}
		
		public Animation getAnimation() {
			return animation;
		}
		
	}
}
