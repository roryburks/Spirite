package spirite.pc.ui.generic;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import javafx.util.Pair;
import spirite.base.image_data.Animation;
import spirite.hybrid.Globals;

public class BetterTree extends JPanel {
	private final List<BTNode> roots = new ArrayList<>();
	
	private final BTDnDManager dnd = new BTDnDManager();
	
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
		roots.add( new BranchingNode(new JButton("Test5")));
		
		this.setDropTarget(dnd);
		
		RebuildTree();
	}
	
	
	private void RebuildTree() {
		this.removeAll();
		
		GroupLayout layout = new GroupLayout(this);
		
		Group horGroup = layout.createParallelGroup();
		SequentialGroup vertGroup = layout.createSequentialGroup();
		
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
	
	public List<BTNode> GetRoots() {
		return new ArrayList<>(roots);
	}
	
	public void AddRoot( BTNode node) {
		if( node != null) {
			roots.add(node);
			RebuildTree();
		}
	}
	public void RemoveRoot( BTNode node) {
		roots.remove(node);
		RebuildTree();
	}
	public void ClearRoots() {
		roots.clear();
		RebuildTree();
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
			branch.setBackground(Color.red);
			
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
					/*.addComponent(branch, propBranchWidth, propBranchWidth, propBranchWidth)*/)
				.addGroup( innerHorGroup);
			
			outerVertGroup.addGroup( layout.createSequentialGroup()
					.addComponent(button, propExpandButtonHeight,propExpandButtonHeight,propExpandButtonHeight)
					/*.addComponent(branch, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)*/)
				.addGroup( innerVertGroup);
			
			layout.setHorizontalGroup(outerHorGroup);
			layout.setVerticalGroup(outerVertGroup);
			
			rootPanel.setBackground(Color.BLUE);
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
					.addComponent(content, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
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
	
	// :::: Drag and Drop Management
	public interface BTDnDModule {
		public DataFlavor[] getAcceptedDataFlavors();
		public void interpetDrop(Transferable trans);
	}
	private final List<BTDnDModule> dndModules = new ArrayList<>();
	
	private class BTDnDManager extends DropTarget 
		implements DragGestureListener, DragSourceListener 
	{
		private final DragSource dragSource = DragSource.getDefaultDragSource();
		
		BTDnDManager() {
			dragSource.createDefaultDragGestureRecognizer(
					BetterTree.this, DnDConstants.ACTION_COPY_OR_MOVE, this);
		}
		
		// :::: Inhereted from DropTarget, hears all sources of drags and drops
		@Override
		public synchronized void dragOver(DropTargetDragEvent evt) {
			boolean accepted = false;
			
			for( BTDnDModule module : dndModules) {
				for( DataFlavor df : module.getAcceptedDataFlavors()) {
					if( evt.isDataFlavorSupported(df)) 
						accepted = true;
				}
			}
			if( accepted)
				evt.acceptDrag( DnDConstants.ACTION_REFERENCE);
			else
				evt.rejectDrag();
		}
		
		@Override
		public synchronized void drop(DropTargetDropEvent evt) {
			for( BTDnDModule module : dndModules) {
				for( DataFlavor df : module.getAcceptedDataFlavors()) {
					if( evt.isDataFlavorSupported(df)) {
						module.interpetDrop(evt.getTransferable());
					}
				}
			}
		}
		
		// :::: DragGestureListener/DragSourceListener for drags originating from the tree
		@Override public void dragDropEnd(DragSourceDropEvent arg0) {
			System.out.println("DRAGENTER");}
		@Override public void dragEnter(DragSourceDragEvent arg0) {
			System.out.println("DRAGENTER");}
		@Override public void dragExit(DragSourceEvent arg0) {
			System.out.println("DRAGENTER");}
		@Override public void dragOver(DragSourceDragEvent arg0) {
			System.out.println("DRAGOVER");
			
		}
		@Override public void dropActionChanged(DragSourceDragEvent arg0) {
			System.out.println("DRAGENTER");}
		@Override public void dragGestureRecognized(DragGestureEvent arg0) {
			System.out.println("DRAGENTER");}
	}
}
