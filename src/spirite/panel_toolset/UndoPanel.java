package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.DefaultListModel;
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
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.DrawEngine;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.UndoEngine;
import spirite.image_data.UndoEngine.FillAction;
import spirite.image_data.UndoEngine.MUndoEngineObserver;
import spirite.image_data.UndoEngine.StrokeAction;
import spirite.image_data.UndoEngine.StructureAction;
import spirite.image_data.UndoEngine.UndoIndex;

/***
 * The UndoPanel shows the History of all Undoable actions and lets you navigate them
 * through click.
 * 
 * @author Rory Burks
 */
public class UndoPanel extends JPanel
	implements MUndoEngineObserver, ListCellRenderer<UndoIndex>, ListSelectionListener,
	MWorkspaceObserver
{
	// UndoPanel needs Master because it needs to add a WorkspaceObserver.
	//	It's possible that it shouldn't be storing Master
	MasterControl master;
	
	private static final long serialVersionUID = 1L;
	JScrollPane container;
	JList<UndoIndex> list;
	DefaultListModel<UndoIndex> model;
	UndoEngine engine = null;
	
	public UndoPanel( MasterControl master) {
		this.master = master;
		this.setLayout( new GridLayout());
		
		// Set the list up and link it with everything
		model = new DefaultListModel<>();
		list = new JList<UndoIndex>();
		list.setModel(model);
		list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount( -1);
		list.setCellRenderer(this);
		list.getSelectionModel().addListSelectionListener(this);
		
		// Put the List in a ScrollPane
		container = new JScrollPane(list);
		container.setPreferredSize( new Dimension(240,300));
		this.add(container);
		
		// Link the various components
		ImageWorkspace ws = master.getCurrentWorkspace();
		if( ws != null) {
			engine = ws.getUndoEngine();
			engine.addUndoEngineObserver(this);
			constructFromHistory( engine.constructUndoHistory());
		}
		master.addWorkspaceObserver(this);
		
	}

	
	/***
	 * Using the UndoHistory from UndoEngine.constructUndoHistory, removes
	 * the old model data and constructs it anew.
	 */
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
		else if( value.action instanceof StructureAction) {
			actionString = ((StructureAction)value.action).change.description;
		}
		
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
		if( engine != null)
			list.setSelectedIndex(engine.getQueuePosition());
	}

	@Override
	public void redo() {
		if( engine != null)
			list.setSelectedIndex(engine.getQueuePosition());
	}
	
	// :::: ListSelectionListener
	@Override
	public void valueChanged(ListSelectionEvent evt) {
		
		// Compared to other Listeners, ListSelectionListeners seem really loosey goosey
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				if( engine == null)
					return;
				
				
				ListSelectionModel model = (ListSelectionModel) evt.getSource();
				int i = model.getMinSelectionIndex();
				Rectangle rect = list.getCellBounds(i,i);
				if( rect != null)
					list.scrollRectToVisible( rect);
				
				if( i != engine.getQueuePosition()) {
					engine.setQueuePosition(i);
				}
			}
		});
	}


	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		// Unlink the old Workspace's UndoEngine and link the new
		if( engine != null)
			engine.removeUndoEngineObserver(this);
		
		if( selected != null) {
			engine = selected.getUndoEngine();
			engine.addUndoEngineObserver(this);
			constructFromHistory(engine.constructUndoHistory());
		} else {
			model.clear();
		}
	}


	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {	}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {	}
}
