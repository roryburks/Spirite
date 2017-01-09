package spirite.panel_work;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.ImageWorkspace;

public class WorkTabPane extends JTabbedPane 
	implements MWorkspaceObserver, ChangeListener
{
	MasterControl master;
	List<WorkPanel> panels = new ArrayList<>();
	
	
	public WorkTabPane( MasterControl master) {
		this.master = master;
		
		master.addWorkspaceObserver(this);
		
		this.addChangeListener(this);
	}
	
	
	public WorkPanel getCurrentWorkPane() {
		// TODO BAD
		return (WorkPanel)this.getSelectedComponent();
	}
	
	// :::: MWorkspaceObserver
	@Override
	public void currentWorkspaceChanged( ImageWorkspace selected, ImageWorkspace previous) {

		for( int i = 0; i < panels.size(); ++i) {
			if( panels.get(i).workspace == selected) {
				setSelectedIndex(i);
				return;
			}
		}
	}
	
	@Override
	public void newWorkspace( ImageWorkspace newWorkspace) {
		WorkPanel panel =  new WorkPanel(master, newWorkspace);
		panels.add(panel);
		this.addTab("Untilted Image", panel);
	}
	
	@Override
	public void removeWorkspace( ImageWorkspace arg0) {
		// TODO Auto-generated method stub
		
	}

	// :::: ChangeListener
	@Override
	public void stateChanged(ChangeEvent arg0) {
		int i = getSelectedIndex();
		ImageWorkspace selected = panels.get( getSelectedIndex()).workspace;
		
		if( selected != master.getCurrentWorkspace()) {
			master.setCurrentWorkpace(selected);
		}
	}
	
	
}
