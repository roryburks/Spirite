package spirite.pc.ui.components;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import spirite.base.util.MUtil;
import spirite.pc.ui.UIUtil;

public class BoxList<T> extends JPanel {
	private int box_w, box_h;
	private final List<T> entries = new ArrayList<T>();

	private final JPanel content = new JPanel();
	private final JScrollPane scroll = new JScrollPane(content);
	
	// ========
	// ==== Semi-Abstract
	protected boolean attemptMove(int from, int to) {
		entries.add(to, entries.remove(from));
		return true;
	}
	
	
	public BoxList(Collection<T> entries, int width, int height) {
		this.box_w = width;
		this.box_h = height;
		
		if( entries != null)
			this.entries.addAll(entries);
		
		this.setLayout(new GridLayout());
		this.add(scroll);
		
		initMap();
		rebuild();
		
		this.addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e) {
				rebuild();
			}
		});
		
		content.addMouseListener( new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocus();
				
				Component clicked = content.getComponentAt(e.getPoint());
				if( components != null && clicked != null) {
					int i = components.indexOf(clicked);
					if( i != -1) 
						setSelectedIndex(i);
				}
			}
		});
	}
	
	private int num_per_row;
	private List<Component> components = null;
	private void rebuild() {
		content.removeAll();
		
		int w = scroll.getViewport().getWidth();
		//int h = content.getHeight();

		num_per_row = Math.max(1, w / box_w);
		int dw = w / num_per_row;
		
		//int dh = (entries.size() - 1)/num_per_row + 1;
		
		GroupLayout layout = new GroupLayout(content);

		Group vertMain = layout.createSequentialGroup();
		Group horMain = layout.createParallelGroup();
		
		Group vertSub = null;
		Group horSub = null;
		
		int x = 0;
		components = new ArrayList<>(entries.size());
		for( int i=0; i < entries.size(); ++i) {
			if( x == 0) {
				vertSub = layout.createParallelGroup();
				horSub = layout.createSequentialGroup();
				vertMain.addGroup(vertSub);
				horMain.addGroup(horSub);
			}
			
			Component c = renderer.getNodeFor(entries.get(i), i, i == selectedIndex);
			components.add(c);
			vertSub.addComponent(c, box_h, box_h, box_h);
			horSub.addComponent(c, dw, dw, dw);
			
			if( ++x == num_per_row)
				x = 0;
		}
		
		layout.setVerticalGroup(vertMain);
		layout.setHorizontalGroup(horMain);
		
		content.setLayout(layout);
	}
	
	// ========
	// ==== SimpleAPI
	public int getNumPerRow() {return num_per_row;}
	
	// ========
	// ==== Input
	private void initMap() {
		Map<KeyStroke, Action> actionMap = new HashMap<>(4);

		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),  UIUtil.buildAction( (e) -> {
			if( selectedIndex != -1)
				this.setSelectedIndex(Math.max(0, selectedIndex - num_per_row));
		}));
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),  UIUtil.buildAction( (e) -> {
			if( selectedIndex != -1) 
				this.setSelectedIndex(Math.min(components.size()-1, selectedIndex+num_per_row));
		}));
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),  UIUtil.buildAction( (e) -> {
			if( selectedIndex != -1)
				this.setSelectedIndex(Math.max(0, selectedIndex-1));
		}));
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),  UIUtil.buildAction( (e) -> {
			if( selectedIndex != -1) 
				this.setSelectedIndex(Math.min(components.size()-1, selectedIndex+1));
		}));
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK),  UIUtil.buildAction((e) -> {
			if( selectedIndex != -1 && selectedIndex != 0) {
				if( attemptMove( selectedIndex, selectedIndex-1))
					selectedIndex = selectedIndex-1;
			}
		}));
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK),  UIUtil.buildAction((e) -> {
			if( selectedIndex != -1 && selectedIndex != entries.size()-1) {
				if( attemptMove( selectedIndex, selectedIndex+1))
					selectedIndex = selectedIndex+1;
			}
		}));
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK),  UIUtil.buildAction((e) -> {
			int to = Math.max(0, selectedIndex - num_per_row);
			
			if( selectedIndex != -1 && to != selectedIndex) {
				if( attemptMove( selectedIndex, to))
					selectedIndex = to;
				
			}
		}));
		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK),  UIUtil.buildAction((e) -> {
			int to = Math.min(entries.size()-1, selectedIndex + num_per_row);
			
			if( selectedIndex != -1 && to != selectedIndex) {
				if( attemptMove( selectedIndex, to))
					selectedIndex = to;
				
			}
		}));
		
		UIUtil.buildActionMap( this, actionMap);
	}
	
	// =======
	// ==== Node Renderer
	private BoxListNodeRenderer<T> renderer = (t, index, selected) -> {
		return new JLabel(t.toString());
	};
	
	public void setRenderer( BoxListNodeRenderer<T> renderer) {
		this.renderer = renderer;
		rebuild();
	}
	
	public interface BoxListNodeRenderer<T> {
		public Component getNodeFor( T t, int index, boolean selected);
	}
	
	// ========
	// ==== Add/Remove/Move
	public void addEntry( T newEntry) {addEntry(newEntry, entries.size());}
	public void addEntry( T newEntry, int index) {
		index = Math.max(0, Math.min(entries.size(), index));
		entries.add(newEntry);
		rebuild();
	}
	
	public void removeEntry( T toRemove) {
		entries.remove(toRemove);
		rebuild();
	}
	
	public void resetEntries( Collection<? extends T> newEntries, int selected) {
		selectedIndex = selected;
		entries.clear();
		if( newEntries != null && newEntries.size() > 0)
			entries.addAll(newEntries);
		rebuild();
    	repaint();
	}
	
	// ==========
	// ==== Selection
	private int selectedIndex = -1;
	
	@FunctionalInterface
	public interface SelectionAction {
		public void onSelectionChanged(int newSelection);
	}
	SelectionAction selectionAction = null;
	public void setSelectionAction( SelectionAction action) {
		this.selectionAction = action;
	}
	
	public void setSelectedIndex(int i) {
		i = MUtil.clip(-1, i, entries.size()-1);
		selectedIndex = i;
		
		scroll.scrollRectToVisible((i == -1) ? new Rectangle(0,0,1,1) : components.get(i).getBounds());
		

		rebuild();
    	repaint();
    	
		if( selectionAction != null)
			selectionAction.onSelectionChanged(i);
	}
	public int getSelectedIndex() {return selectedIndex;}
}