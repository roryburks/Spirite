package spirite.image_data;

import java.util.ArrayList;
import java.util.List;

public class GroupTree {
	private GroupNode root;
	
	public GroupTree() {
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
	
	// ::: Nodes
	public class Node {
		// !!!! TODO : Various attributes (such as oppacity) that apply to the group
		//	or the Rig (without altering them) should go here
		private float alpha;
		private boolean visible = true;

		
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
	}
	
	public class GroupNode extends Node {
		private String name;
		private boolean expanded;
		
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
