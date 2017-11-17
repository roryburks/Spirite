package spirite.base.image_data;

import java.util.List;

import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.GroupTree.AnimationNode;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.util.ObserverHandler;

/**
 * The AnimationView maintains the view-state of the extended AnimationView (the
 * second tab on the layers panel).  It is primarily just a wrapper for a Group
 * tree defining its structure, but it also can contain extra-Group-structure 
 * visual properties such as view properties relative to frame (e.g. one above,
 * one below), and visual properties based on Animation Layer.
 */
public class AnimationView implements MAnimationStructureObserver {
	private final ImageWorkspace workspace;
	private final AnimationManager context;
	private final GroupTree tree;

	AnimationView(ImageWorkspace workspace, AnimationManager context) {
		this.workspace = workspace;
		this.context = context;
		this.tree = new GroupTree(workspace);

		context.addAnimationStructureObserver(this);
	}

	public GroupNode getRoot() {
		return tree.getRoot();
	}

	
	private boolean nodeIsInView( Node node) {
		while( node != null) {
			if( node == tree.getRoot())
				return true;
			node = node.getParent();
		}
		return false;
	}
	
	// ===================
	// ==== Selection ====
	private Node selected;
	public Node getSelectedNode() {
		return selected;
	}
	public void setSelectedNode(Node sel) {
		selected = sel;
		_triggerViewSelectionChange(sel);
	}
	


	private boolean animationView = false;
	public boolean isUsingAnimationView() {
		return animationView;
	}
	public void setUsingAnimationView( boolean using) { 
		if( this.animationView != using) {
			this.animationView = using;
			//this.context.triggerSelectedChanged();
		}
	}
	
	// ===========================
	// ==== Node Add/Subtract ====
	public Node addNode(Animation animation) {
		Node node = tree.new AnimationNode(animation, animation.getName());
		tree.getRoot()._add(node, null);
		workspace.triggerFlash();
		
		return node;
	}
	public void RemoveNode( Node node) {
		if(!nodeIsInView(node) || node == tree.getRoot())
			return;
		
		
		node._del();
		_triggerViewChange();
	}
	public void moveAbove( Node nodeToMove, Node nodeAbove) {
		if( nodeToMove == null || nodeAbove == null || nodeAbove.getParent() == null 
				|| nodeToMove.getParent() == null || tree._isChild( nodeToMove, nodeAbove.getParent()))
			return;
		move(nodeToMove,
			nodeAbove.getParent(),
			nodeAbove);
	}
	public void moveBelow( Node nodeToMove, Node nodeUnder) {
		if( nodeToMove == null || nodeUnder == null || nodeUnder.getParent() == null 
				|| nodeToMove.getParent() == null || tree._isChild( nodeToMove, nodeUnder.getParent()))
			return;
		
		List<Node> children = nodeUnder.getParent().getChildren();
		int i = children.indexOf(nodeUnder);
		Node nodeBefore;
		
		if( i+1 == children.size())
			nodeBefore = null;
		else
			nodeBefore = children.get(i+1);
		
		move(nodeToMove,
			nodeUnder.getParent(),
			nodeBefore);
	}
	public void moveInto( Node nodeToMove, GroupNode nodeInto, boolean top) {
		if( nodeToMove == null || nodeInto == null 
				|| nodeToMove.getParent() == null || tree._isChild( nodeToMove, nodeInto))
			return;
		
		Node nodeBefore = null;
		if( top && !nodeInto.getChildren().isEmpty()) {
			nodeBefore = nodeInto.getChildren().get(0);
		}

		move(nodeToMove,
			nodeInto,
			nodeBefore);
	}
	
	private void move(
			Node moveNode,
			Node newParent,
			Node newNext) 
	{
		if( moveNode.getRoot() == getRoot()) {
			moveNode._del();	
		}
		newParent._add(moveNode, newNext);
		_triggerViewChange();
	}
	
	// Observers
	private final ObserverHandler<MAnimationViewObserver> animationViewObs = new ObserverHandler<>();
	public void addAnimationViewObserver(MAnimationViewObserver obs) { animationViewObs.addObserver(obs);}
	public void removeAnimationViewObserver(MAnimationViewObserver obs) { animationViewObs.removeObserver(obs);}
	
	public static interface MAnimationViewObserver {
		public void viewChanged();
		public void viewSelectionChange( Node selected);
	}

	private void _triggerViewChange() {
		animationViewObs.trigger( (MAnimationViewObserver obs) ->{ obs.viewChanged();});
		workspace.triggerFlash();
	}
	private void _triggerViewSelectionChange( Node selected) {
		animationViewObs.trigger( (MAnimationViewObserver obs) ->{ obs.viewSelectionChange(selected);});
		workspace.triggerFlash();
	}

	// :: MAnimationStructureObserver
	@Override public void animationAdded(AnimationStructureEvent evt) {}
	@Override public void animationChanged(AnimationStructureEvent evt) {}
	@Override public void animationRemoved(AnimationStructureEvent evt) {
		for( Node node : tree.getRoot().getAllAncestors()) {
			if( node instanceof AnimationNode && ((AnimationNode) node).getAnimation() == evt.getAnimation())
				node._del();
		}
	}

}
