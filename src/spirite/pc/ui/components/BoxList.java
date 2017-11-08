package spirite.pc.ui.components;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

public class BoxList<T> extends JPanel {
	private int box_w, box_h;
	private final List<T> entries = new ArrayList<T>();

	private final JPanel content = new JPanel();
	private final JScrollPane scroll = new JScrollPane(content);
	
	public BoxList(Collection<T> entries, int width, int height) {
		this.box_w = width;
		this.box_h = height;
		
		if( entries != null)
			this.entries.addAll(entries);
		
		this.setLayout(new GridLayout());
		this.add(scroll);
		
		rebuild();
		
		this.addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e) {
				rebuild();
			}
		});
	}
	
	private void rebuild() {
		content.removeAll();
		
		int w = content.getWidth();
		int h = content.getHeight();

		int num_per_row = Math.max(1, w / box_w);
		int dw = w / num_per_row;
		
		//int dh = (entries.size() - 1)/num_per_row + 1;
		
		GroupLayout layout = new GroupLayout(content);

		Group vertMain = layout.createSequentialGroup();
		Group horMain = layout.createParallelGroup();
		
		Group vertSub = null;
		Group horSub = null;
		
		int x = 0;
		for( int i=0; i < entries.size(); ++i) {
			if( x == 0) {
				vertSub = layout.createParallelGroup();
				horSub = layout.createSequentialGroup();
				vertMain.addGroup(vertSub);
				horMain.addGroup(horSub);
			}
			
			Component c = renderer.getNodeFor(entries.get(i), i);
			vertSub.addComponent(c, box_h, box_h, box_h);
			horSub.addComponent(c, dw, dw, dw);
			
			if( ++x == num_per_row)
				x = 0;
		}
		
		layout.setVerticalGroup(vertMain);
		layout.setHorizontalGroup(horMain);
		
		content.setLayout(layout);
	}
	
	// =======
	// ==== Node Renderer
	private BoxListNodeRenderer<T> renderer = (t, index) -> {
		return new JLabel(t.toString());
	};
	
	public void setRenderer( BoxListNodeRenderer<T> renderer) {
		this.renderer = renderer;
		rebuild();
	}
	
	public interface BoxListNodeRenderer<T> {
		public Component getNodeFor( T t, int index);
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
	
	public void resetEntries( Collection<T> newEntries) {
		entries.clear();
		entries.addAll(newEntries);
		rebuild();
	}
}
