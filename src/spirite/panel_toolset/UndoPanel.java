package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.draw_engine.DrawEngine;
import spirite.draw_engine.UndoEngine;
import spirite.draw_engine.UndoEngine.FillAction;
import spirite.draw_engine.UndoEngine.MUndoEngineObserver;
import spirite.draw_engine.UndoEngine.StrokeAction;
import spirite.draw_engine.UndoEngine.UndoIndex;
import spirite.draw_engine.UndoEngine.VisibilityAction;

public class UndoPanel extends JPanel
	implements MUndoEngineObserver, ListCellRenderer<UndoIndex>, ListSelectionListener
{
	private static final long serialVersionUID = 1L;
	JScrollPane container;
	MasterControl master;
	JList<UndoIndex> list;
	DefaultListModel<UndoIndex> model;
	UndoEngine engine;
	
	public UndoPanel( MasterControl master) {
		this.master = master;
		this.setLayout( new GridLayout());
		
		model = new DefaultListModel<>();
		list = new JList<UndoIndex>();
		list.setModel(model);
		list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount( -1);
		list.setCellRenderer(this);
		list.getSelectionModel().addListSelectionListener(this);
		
		container = new JScrollPane(list);
		container.setPreferredSize( new Dimension(240,300));
		this.add(container);
		
		engine = master.getCurrentWorkspace().getUndoEngine();
		engine.addUndoEngineObserver(this);
		
		model.addElement(null);
		container.setAutoscrolls(true);
	}

	
	private void constructFromHistory(List<UndoIndex> undoHistory ){
		model.clear();
		
		model.addElement(null);
		
		for( UndoIndex index : undoHistory) {
			model.addElement( index);
		}
		

		list.setSelectedIndex( model.size() - 1);
	}

	
	
	// :::: Cell Rendering
	UPRPanel renderPanel = new UPRPanel();
	
	class UPRPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		Color selectedColor;
		Color normalColor;
		JPanel preview = new JPanel();
		JLabel label = new JLabel();
		
		
		UPRPanel() {
			selectedColor = Globals.getColor("undoPanel.selectedBackground");
			normalColor = Globals.getColor("undoPanel.background");
			
			preview.setBackground(Color.black);
			GroupLayout layout = new GroupLayout(this);
			Dimension d = new Dimension( 40,40);
			
			layout.setHorizontalGroup( layout.createSequentialGroup()
					.addGap(10)
					.addComponent(preview, d.width,  d.width,  d.width)
					.addGap(2)
					.addComponent(label)
			);
			layout.setVerticalGroup( layout.createParallelGroup()
					.addGroup( layout.createSequentialGroup()
						.addGap(2)
						.addComponent(preview,  d.height, d.height, d.height)
						.addGap(2)
					)
					.addGroup( layout.createSequentialGroup()
						.addGap(10)
						.addComponent(label)
					)
					
			);
			
			this.setLayout(layout);
		}
	}

	@Override
	public Component getListCellRendererComponent(
			JList<? extends UndoIndex> list, 
			UndoIndex value, 
			int index,
			boolean isSelected, 
			boolean cellHasFocus) 
	{
		String actionString = "";
		
		if( value == null)
			actionString = "[Base Image]";
		else if( value.action instanceof StrokeAction) {
			DrawEngine.Method m = ((StrokeAction)value.action).getParams().getMethod();
			actionString = "Stroke: ";
			switch( m) {
			case BASIC:
				actionString += "Pixel";
				break;
			case ERASE:
				actionString += "Erase";
				break;
			}
		}
		else if( value.action instanceof FillAction)
			actionString = "Fill";
		else if( value.action instanceof VisibilityAction)
			actionString = "Visibility change";
		
		renderPanel.label.setText(actionString);
		
		
		if( isSelected)
			renderPanel.setBackground(renderPanel.selectedColor);
		else
			renderPanel.setBackground(renderPanel.normalColor);
		return renderPanel;
	}


	// :::: MUndoEngineObserver
	@Override
	public void historyChanged(List<UndoIndex> undoHistory) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				constructFromHistory( undoHistory);
			}
		});
		
	}

	@Override
	public void undo() {
		list.setSelectedIndex(engine.getQueuePosition());
	}

	@Override
	public void redo() {
		list.setSelectedIndex(engine.getQueuePosition());
	}
	
	// :::: ListSelectionListener
	@Override
	public void valueChanged(ListSelectionEvent evt) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				ListSelectionModel model = (ListSelectionModel) evt.getSource();
				int i = model.getMinSelectionIndex();
				list.scrollRectToVisible( list.getCellBounds(i,i));
				
				if( i != engine.getQueuePosition()) {
					engine.setQueuePosition(i);
				}
			}
		});
	}
}
