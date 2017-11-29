package spirite.pc.ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

import spirite.gui.hybrid.SPanel;
import spirite.hybrid.Globals;

public class SimpleTree extends SPanel {

	protected final List<TableTab> tabs  = new ArrayList<TableTab>();
	
	public SimpleTree() {
		reconstruct();
	}
	
	public JComponent getTitleComponent( int i) {
		if( i < 0 || i >= tabs.size()) return null;
		
		return tabs.get(i).title;
	}
	public JComponent getContentComponent( int i) {
		if( i < 0 || i >= tabs.size()) return null;
		
		return tabs.get(i).content;
	}
	public void setTitleComponent( int i, JComponent component) {
		if( i >= 0 && i < tabs.size()) {
			tabs.get(i).title = component;
			reconstruct();
		}
	}
	public void setContentComponent( int i, JComponent component) {
		if( i >= 0 && i < tabs.size()) {
			tabs.get(i).content = component;
			reconstruct();
		}
	}
	public void addTab( 
			JComponent title, 
			JComponent content, 
			ExpandWatcher watcher,
			boolean expanded) 
	{
		TableTab tt = new TableTab();
		tt.title = title;
		tt.content = content;
		tt.watcher = watcher;
		tt.expanded = expanded;
		tabs.add(tt);
		reconstruct();
	}

	private static final int LEFT_SECTION_WIDTH = 16;
	private static final int TITLE_HEIGHT = 24;
	/**
	 * Reconstructs the Tree's Layout
	 */
	protected void reconstruct() {
		this.removeAll();
		
		GroupLayout layout = new GroupLayout(this);
		
		Group horLeft = layout.createParallelGroup();
		Group horRight = layout.createParallelGroup();
		SequentialGroup vertical = layout.createSequentialGroup();
		
		for( TableTab tab : tabs) {

			ExpandButton expandButton = new ExpandButton(tab.content, tab.watcher);
			expandButton.setSelected(tab.expanded);
			tab.content.setVisible(tab.expanded);
			

			vertical.addGroup(layout.createParallelGroup()
				.addComponent(tab.title,TITLE_HEIGHT,TITLE_HEIGHT,TITLE_HEIGHT)
				.addComponent(expandButton,TITLE_HEIGHT,TITLE_HEIGHT,TITLE_HEIGHT))
			.addGroup(layout.createParallelGroup()
				.addComponent(tab.content,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE));
			horLeft.addComponent(expandButton,LEFT_SECTION_WIDTH,LEFT_SECTION_WIDTH,LEFT_SECTION_WIDTH);
			horRight.addComponent(tab.title);
			horRight.addComponent(tab.content,0,0,Short.MAX_VALUE);
		}
		vertical.addGap(0,0,Short.MAX_VALUE);
		layout.setVerticalGroup(vertical);
		layout.setHorizontalGroup( layout.createSequentialGroup()
			.addGroup(horLeft)
			.addGroup(horRight));
		this.setLayout(layout);
		
	}
	

	public static interface ExpandWatcher {
		public void expandChanged(boolean expanded);
	}
	class ExpandButton extends JToggleButton implements ActionListener {
		private final Component content;
		private final ExpandWatcher watcher;
		ExpandButton(Component content, ExpandWatcher watcher) {
			this.content = content;
			this.watcher = watcher;
			this.setOpaque(false);
			this.setBackground(new Color(0,0,0,0));
			this.setBorder(null);

			this.setIcon(Globals.getIcon("icon.expanded"));
			this.setRolloverIcon(Globals.getIcon("icon.expandedHL"));
			this.setSelectedIcon(Globals.getIcon("icon.unexpanded"));
			this.setRolloverSelectedIcon(Globals.getIcon("icon.unexpandedHL"));
			
			this.addActionListener(this);
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			content.setVisible(isSelected());
			if( watcher != null)
				watcher.expandChanged(isSelected());
		}
	}
	
	
	private class TableTab {
		JComponent title;
		JComponent content;
		ExpandWatcher watcher;
		boolean expanded = true;
	}
}
