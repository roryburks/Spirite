package spirite.base.image_data;

import spirite.base.image_data.GroupTree.GroupNode;

public class AnimationView {
	private final ImageWorkspace context;
	private final GroupTree tree;

	AnimationView(ImageWorkspace workspace) {
		this.context = workspace;
		this.tree = new GroupTree(workspace);
	}

	public GroupNode getRoot() {
		return tree.getRoot();
	}

	public void addNode(Animation animation) {
		tree.getRoot()._add(tree.new AnimationNode(animation, animation.getName()), null);
		context.triggerFlash();
	}
}
