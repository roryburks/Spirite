package spirite.pc.ui.generic;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import javafx.util.Pair;
import spirite.base.image_data.Animation;
import spirite.hybrid.Globals;

public class BetterTree extends JPanel {
	private final List<BTNode> roots = new ArrayList<>();
	
	public BetterTree() {
		BTNode nodeInner =  new BranchingNode(new JButton("Test"),
				Arrays.asList( new BTNode[] {
						new LeafNode( new JButton("Test2")),
						new LeafNode( new JButton("Test3"))
				}));
		

		roots.add( new BranchingNode(new JButton("Test3"),
				Arrays.asList( new BTNode[] {
						new LeafNode( new JButton("Test4")),
						nodeInner
				})));
		
		RebuildTree();
	}
	
	private void RebuildTree() {
		this.removeAll();
		
		GroupLayout layout = new GroupLayout(this);
		
		Group horGroup = layout.createParallelGroup();
		Group vertGroup = layout.createSequentialGroup();
		
		for( BTNode node : roots) {
			Component toAdd = node.BuildContent();
			
			horGroup.addComponent(toAdd);
			vertGroup.addComponent(toAdd);
		}
		
		layout.setHorizontalGroup(horGroup);
		layout.setVerticalGroup(vertGroup);
		
		this.setLayout(layout);
		this.repaint();
		this.revalidate();
		this.doLayout();
	}
	
	
	// Properties
	private int propBranchWidth = 16;
	private int propExpandButtonHeight = 16;
	
	abstract class BTNode {
		protected Component title;
		
		protected final JPanel rootPanel = new JPanel();
		
		abstract Component BuildContent();
		abstract List<BTNode> GetLeafs();
	}
	
	/**
	 * 
	 */
	public class BranchingNode extends BTNode {
		private final List<BTNode> subNodes = new ArrayList<>();
		protected boolean _expanded = true;

		public BranchingNode( Component title, Collection<BTNode> leafs) {
			this.title = title;
			if( leafs != null)
				subNodes.addAll(leafs);
		}
		public BranchingNode( Component title) {
			this( title, null);
		}
		
		public void AddNode( BTNode toAdd) {
			subNodes.add( toAdd);
		}
		public void AddNode( int index, BTNode toAdd) {
			subNodes.add( index, toAdd);
		}

		@Override
		List<BTNode> GetLeafs() {
			return subNodes;
		}
		@Override
		Component BuildContent() {
			rootPanel.removeAll();
			GroupLayout layout = new GroupLayout( rootPanel);
			
			Group outerHorGroup = layout.createSequentialGroup();
			Group outerVertGroup = layout.createParallelGroup();
			
			Group innerHorGroup = layout.createParallelGroup();
			Group innerVertGroup = layout.createSequentialGroup();
			
			ExpandButton button = new ExpandButton(this);
			JPanel branch = new JPanel();
			
			innerHorGroup.addComponent(title, 0, 0, Short.MAX_VALUE);
			innerVertGroup.addComponent(title);
			
			if( _expanded) {
				for( BTNode node : subNodes) {
					Component component = node.BuildContent();
					innerHorGroup.addComponent(component, 0, 0, Short.MAX_VALUE);
					innerVertGroup.addComponent(component);
				}
			}
			
			outerHorGroup.addGroup( layout.createParallelGroup()
					.addComponent(button, propBranchWidth, propBranchWidth, propBranchWidth)
					.addComponent(branch, propBranchWidth, propBranchWidth, propBranchWidth))
				.addGroup( innerHorGroup);
			
			outerVertGroup.addGroup( layout.createSequentialGroup()
					.addComponent(button, propExpandButtonHeight,propExpandButtonHeight,propExpandButtonHeight)
					.addComponent(branch))
				.addGroup( innerVertGroup);
			
			layout.setHorizontalGroup(outerHorGroup);
			layout.setVerticalGroup(outerVertGroup);
			
			rootPanel.setLayout(layout);
			
			return rootPanel;
		}
		void SetExpanded( boolean expanded) {
			_expanded = expanded;
			RebuildTree();
		}
		boolean GetExpanded() {return _expanded;}
	}
	
	public class LeafNode extends BTNode {
		private Component content;
		private final JPanel rootPanel = new JPanel();
		
		public LeafNode( Component content) {
			this.content = content;
			
			GroupLayout layout = new GroupLayout(rootPanel);
			layout.setHorizontalGroup( layout.createSequentialGroup()
					.addGap(propBranchWidth)
					.addComponent(content, 0, 0, Short.MAX_VALUE));
			layout.setVerticalGroup( layout.createParallelGroup().addComponent(content));
			rootPanel.setLayout( layout);
		}

		@Override
		Component BuildContent() {
			return rootPanel;
		}
		
		@Override
		List<BTNode> GetLeafs() {
			return null;
		}
	}

	class ExpandButton extends JToggleButton implements ActionListener {
		private final BranchingNode node;
		ExpandButton( BranchingNode node) {
			this.node = node;
			this.setOpaque(false);
			this.setBackground(new Color(0,0,0,0));
			this.setBorder(null);

			this.setIcon(Globals.getIcon("icon.expanded"));
			this.setRolloverIcon(Globals.getIcon("icon.expandedHL"));
			this.setSelectedIcon(Globals.getIcon("icon.unexpanded"));
			this.setRolloverSelectedIcon(Globals.getIcon("icon.unexpandedHL"));
			
			this.setSelected( node.GetExpanded());
			this.addActionListener(this);
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			node.SetExpanded(isSelected());
		}
	}
}
