package spirite.base.image_data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import spirite.base.image_data.AnimationManager.AnimationState;
import spirite.base.image_data.AnimationManager.MAnimationStateEvent;
import spirite.base.image_data.AnimationManager.MAnimationStateObserver;
import spirite.base.image_data.GroupTree.GroupNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace.MoveChange;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;

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

	public void addNode(Animation animation) {
		tree.getRoot()._add(tree.new AnimationNode(animation, animation.getName()), null);
		workspace.triggerFlash();
	}
	
	private boolean nodeIsInView( Node node) {
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
			nodeToMove.getParent(),
			nodeToMove.getNextNode(),
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
			nodeToMove.getParent(),
			nodeToMove.getNextNode(),
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
			nodeToMove.getParent(),
			nodeToMove.getNextNode(),
			nodeInto,
			nodeBefore);
	}
	
	private void move(
			Node moveNode,
			Node oldParent,
			Node oldNext,
			Node newParent,
			Node newNext) 
	{
		moveNode._del();
		newParent._add(moveNode, newNext);
		_triggerViewChange();
	}
	
	// Observers
	public static interface MAnimationViewObserver {
		public void viewChanged();
	}
	List<WeakReference<MAnimationViewObserver>> animationViewObservers = new ArrayList<>();
	public void addAnimationViewObserver(MAnimationViewObserver obs) {
		animationViewObservers.add(new WeakReference<>(obs));
	}
	public void removeAnimationViewObserver(MAnimationViewObserver obs) {
		Iterator<WeakReference<MAnimationViewObserver>> it = animationViewObservers.iterator();
		while (it.hasNext()) {
			MAnimationViewObserver other = it.next().get();
			if (other == obs || other == null)
				it.remove();
		}
	}
	private void _triggerViewChange() {
		Iterator<WeakReference<MAnimationViewObserver>> it = animationViewObservers.iterator();
		while (it.hasNext()) {
			MAnimationViewObserver other = it.next().get();
			if (other == null)
				it.remove();
			else
				other.viewChanged();
		}
	}
}
