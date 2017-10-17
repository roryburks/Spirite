package spirite.base.image_data;

import java.util.List;

import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.util.ObserverHandler;

public class AnimationView {
	private final ImageWorkspace workspace;
	private final AnimationManager context;
	private final GroupTree tree;

	AnimationView(ImageWorkspace workspace) {
		this.workspace = workspace;
		this.context = workspace.getAnimationManager();
		this.tree = new GroupTree(workspace);
	}

	public GroupNode getRoot() {
		return tree.getRoot();
	}

	public Node addNode(Animation animation) {
		Node node = tree.new AnimationNode(animation, animation.getName());
		tree.getRoot()._add(node, null);
		workspace.triggerFlash();
		
		return node;
	}
	
	private boolean nodeIsInView( Node node) {
		while( node != null) {
			if( node == tree.getRoot())
				return true;
			node = node.getParent();
		}
		return false;
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
	}
	
	private void _triggerViewChange() {
		animationViewObs.trigger( (MAnimationViewObserver obs) ->{ obs.viewChanged();});
		workspace.triggerFlash();
	}
}
