package spirite.pc.ui.generic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import spirite.hybrid.Globals;

public class BetterTree extends JPanel {
	private final List<BTNode> roots = new ArrayList<>();
	private final BidiMap<Component,BTNode> nodeLink = new DualHashBidiMap<>();
	
	private final BTDnDManager dnd = new BTDnDManager();
	
	public BetterTree() {
		this.setDropTarget(dnd);
		
		RebuildTree();
	}
	
	
	private void RebuildTree() {
		this.removeAll();
		nodeLink.clear();
		
		GroupLayout layout = new GroupLayout(this);
		
		Group horGroup = layout.createParallelGroup();
		SequentialGroup vertGroup = layout.createSequentialGroup();
		
		for( BTNode node : roots) {
			Component toAdd = node.BuildContent();
			
			horGroup.addComponent(toAdd);
			vertGroup.addComponent(toAdd);
			dnd.addDropSource(toAdd);
			nodeLink.put( toAdd, node);
		}
		
		layout.setHorizontalGroup(horGroup);
		layout.setVerticalGroup(vertGroup);
		
		this.setLayout(layout);
		this.repaint();
		this.revalidate();
		this.doLayout();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke( new BasicStroke(2));
		g2.setColor(Color.BLACK);
		
		if( dragging != null) {
			if( draggingRelativeTo == null) {
				if( draggingDirection == DropDirection.ABOVE) {
					int dy = (roots.isEmpty()) ? 0 : nodeLink.getKey(roots.get(0)).getY();
					g2.drawLine(0, dy, this.getWidth(), dy);
				}
				else if( draggingDirection == DropDirection.BELOW) {
					BTNode lowest = GetLowestChild();
					if( lowest == null)
						g2.drawLine(0, 0, this.getWidth(), 0);
					else {
						Component linked = nodeLink.getKey(lowest);
						int dy = linked.getY() + linked.getHeight();
						g2.drawLine(0, dy, this.getWidth(), dy);
					}
				}
			}
			else if( draggingRelativeTo == dragging) {
				// Self-drag
			}
			else {
				Component linked = nodeLink.getKey(draggingRelativeTo);
				if( draggingDirection == DropDirection.ABOVE) {
					int dy = linked.getY();
					g2.drawLine(0, dy, this.getWidth(), dy);
				}
				else if( draggingDirection == DropDirection.INTO) {
					g2.drawRect(0, linked.getY(), this.getWidth(), linked.getHeight());
				}
				else if( draggingDirection == DropDirection.BELOW) {
					int dy = linked.getY() + linked.getHeight();
					g2.drawLine(0, dy, this.getWidth(), dy);
				}
			}
		}
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
	
	BTNode GetLowestChild() {
		if( roots.isEmpty())
			return null;
		
		BTNode node = roots.get(roots.size()-1);
		
		while( node instanceof BranchingNode) {
			List<BTNode> branches = node.GetLeafs();
			if( branches.isEmpty())
				return node;
			node = branches.get(branches.size()-1);
		}
		
		return node;
	}
	
	
	// Properties
	private int propBranchWidth = 16;
	private int propExpandButtonHeight = 16;
	
	public abstract class BTNode {
		protected Component title;
		private DnDBinding binding = null;
		
		protected final JPanel rootPanel = new JPanel();
		
		protected abstract Component BuildContent();
		abstract List<BTNode> GetLeafs();
		public Component getTitle() {return title;}
		
		public void setDnDBindings(DnDBinding binding) { this.binding = binding;}
		public DnDBinding getBinding() {return binding;}
	}
	
	
	public BTNode getNodeAtPoint( Point p) {
		for( BTNode node : roots) {
			Component comp = nodeLink.getKey(node);
			
			if( comp == null)
				continue;
			if( comp.getY() <= p.y && comp.getY() + comp.getHeight() > p.y ) {
				if( node instanceof BranchingNode) {
					Point p2 = new Point(p);
					p2 = SwingUtilities.convertPoint(this, p, comp);
					BTNode ret = _getNodeAtPointSub(p2, (BranchingNode)node);
					if( ret != null)
						return ret;
				}
				return node;
			}
		}
		
		for( Entry<Component,BTNode> entry : nodeLink.entrySet()){
			if( entry.getKey().getY() <= p.y && entry.getKey().getY() + entry.getKey().getHeight() > p.y) {

				return entry.getValue();	
			}
		}
		return null;
	}
	public BTNode _getNodeAtPointSub( Point p, BranchingNode branch) {
		for( BTNode node : branch.subNodes) {
			Component comp = nodeLink.getKey(node);
			
			if( comp == null)
				continue;
			if( comp.getY() <= p.y && comp.getY() + comp.getHeight() > p.y ) {
				if( node instanceof BranchingNode) {
					Point p2 = new Point(p);
					p2 = SwingUtilities.convertPoint( nodeLink.getKey(branch), p, comp);
					BTNode ret = _getNodeAtPointSub(p2, (BranchingNode)node);
					if( ret != null)
						return ret;
				}
				return node;
			}
		}
		return null;
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
		protected Component BuildContent() {
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
					dnd.addDropSource(component);
					nodeLink.put(component, node);
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
			
			//rootPanel.setBackground(Color.BLUE);
			rootPanel.setLayout(layout);
			
			return rootPanel;
		}
		public void SetExpanded( boolean expanded) {
			_expanded = expanded;
			RebuildTree();
		}
		public boolean GetExpanded() {return _expanded;}
	}
	
	public class LeafNode extends BTNode {
		private final JPanel rootPanel = new JPanel();
		
		public LeafNode( Component content) {
			this.title = content;
			
			GroupLayout layout = new GroupLayout(rootPanel);
			layout.setHorizontalGroup( layout.createSequentialGroup()
					.addGap(propBranchWidth)
					.addComponent(content, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
			layout.setVerticalGroup( layout.createParallelGroup().addComponent(content));
			rootPanel.setLayout( layout);
		}

		@Override
		protected Component BuildContent() {
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
	public interface DnDBinding {
		public Transferable buildTransferable();
		public Image drawCursor();
		public void dragOut();

		public DataFlavor[] getAcceptedDataFlavors();
		public void interpretDrop( Transferable trans, DropDirection direction);
	}
	public enum DropDirection {
		ABOVE, BELOW, INTO
	}

	private BTNode dragging = null;
	private BTNode draggingRelativeTo = null;
	private DropDirection draggingDirection = DropDirection.ABOVE; 	// 0: into, 1: Above, -1: Bellow
	private DnDBinding rootBinding = null;
	
	public void setRootBinding( DnDBinding binding) { this.rootBinding = binding;}
	
	
	private class BTDnDManager extends DropTarget 
		implements DragGestureListener, DragSourceListener 
	{
		private final DragSource dragSource = DragSource.getDefaultDragSource();
		
		BTDnDManager() {
		}
		
		void addDropSource(Component component) {
			dragSource.createDefaultDragGestureRecognizer(
					component, DnDConstants.ACTION_COPY_OR_MOVE, this);
		}
		
		// :::: Inhereted from DropTarget, hears all sources of drags and drops
		@Override
		public synchronized void dragOver(DropTargetDragEvent evt) {
			BTNode oldNode = draggingRelativeTo;
			DropDirection oldDir = draggingDirection;
			
			draggingRelativeTo = getNodeAtPoint(evt.getLocation());
			int my = evt.getLocation().y;
			if( draggingRelativeTo == null) {
				BTNode lowest = GetLowestChild();
				draggingDirection = DropDirection.ABOVE;
				
				if( lowest != null) {
					Rectangle bounds = nodeLink.getKey(lowest).getBounds();
					if( my > bounds.y + bounds.height)
						draggingDirection = DropDirection.BELOW;
				}
			}
			else {
				Rectangle bounds = nodeLink.getKey(draggingRelativeTo).getBounds();
				if( draggingRelativeTo instanceof BranchingNode && 
					my > bounds.y + bounds.height/4 &&
					my < bounds.y + (bounds.height*3)/4) 
				{
					draggingDirection = DropDirection.INTO;
				}
				else if( my < bounds.y + bounds.height/2)
					draggingDirection = DropDirection.ABOVE;
				else
					draggingDirection = DropDirection.BELOW;
			}
			
			DnDBinding binding = (draggingRelativeTo == null) ? rootBinding : draggingRelativeTo.binding;

			boolean accepted = false;
			if( binding != null) {
				for( DataFlavor df : binding.getAcceptedDataFlavors()) {
					if( evt.isDataFlavorSupported(df)) 
						accepted = true;
				}
			}
			if( accepted)
				evt.acceptDrag( DnDConstants.ACTION_COPY);
			else
				evt.rejectDrag();
			
			if( oldDir != draggingDirection || oldNode != draggingRelativeTo)
				BetterTree.this.repaint();
		}
		
		@Override
		public synchronized void drop(DropTargetDropEvent evt) {
			
			if( draggingRelativeTo == dragging && dragging != null)
				return;
			
			DnDBinding binding = (draggingRelativeTo == null) ? rootBinding : draggingRelativeTo.binding;

			if( binding != null) {
				for( DataFlavor df : binding.getAcceptedDataFlavors()) {
					if( evt.isDataFlavorSupported(df)) {
						binding.interpretDrop(evt.getTransferable(), draggingDirection);
					}
				}
			}
		}
		
		// :::: DragGestureListener/DragSourceListener for drags originating from the tree
		@Override public void dragDropEnd(DragSourceDropEvent evt) {
			Point p = evt.getLocation();
			SwingUtilities.convertPointFromScreen(p, BetterTree.this);
			
			if( !BetterTree.this.contains(p) && dragging != null) {
				System.out.println(p + "::" + BetterTree.this.getBounds());
				DnDBinding binding = dragging.getBinding();
				if( binding != null)
					binding.dragOut();
			}
			
			dragging = null;
			BetterTree.this.repaint();
		}
		@Override public void dragEnter(DragSourceDragEvent arg0) {}
		@Override public void dragExit(DragSourceEvent arg0) {}
		@Override public void dragOver(DragSourceDragEvent arg0) {}
		@Override public void dropActionChanged(DragSourceDragEvent arg0) {}
		@Override public void dragGestureRecognized(DragGestureEvent evt) {
			if( dragging != null)
				return;
			

			BTNode node = nodeLink.get( evt.getComponent());
			
			if( node != null) {
				DnDBinding binding = node.getBinding();
				if( binding != null) {
					dragging = node;

					Cursor cursor = DragSource.DefaultMoveDrop;
					dragSource.startDrag( 
							evt, 
							cursor, 
							binding.drawCursor(),
							new Point(10,10),
							binding.buildTransferable(), 
							this);
				}
			}
		}
		
	}
}
