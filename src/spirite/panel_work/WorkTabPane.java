package spirite.panel_work;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.FileChangeEvent;
import spirite.image_data.ImageWorkspace.MWorkspaceFileObserver;
import spirite.panel_work.WorkPanel.Zoomer;
import spirite.ui.UIUtil;

public class WorkTabPane extends JTabbedPane 
	implements MWorkspaceObserver, ChangeListener,
		MouseListener, ActionListener, MWorkspaceFileObserver
{
	// Needs to rememver master so it can pass it on to WorkPanel's
	private final MasterControl master;
	
	// Panels should maintain a 1:1 assosciation with tabs
	private final List<WorkPanel> panels = new ArrayList<>();
	
	
	public WorkTabPane( MasterControl master) {
		this.master = master;
		
		master.addWorkspaceObserver(this);
		
		this.addChangeListener(this);
		this.addMouseListener(this);
	}
	
	
	public WorkPanel getCurrentWorkPane() {
		int index = this.getSelectedIndex();
		if( index != -1)
			return panels.get(index);
		
		return null;
	}
	
	public Zoomer getZoomerForWorkspace( ImageWorkspace ws) {
		for( WorkPanel panel : panels) {
			if( panel.workspace == ws) {
				return panel.zoomer;
			}
		}
		return null;
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
		newWorkspace.addWorkspaceFileObserve(this);
		panels.add(panel);
		
		String title;
		
		if( newWorkspace.getFile() == null) {
			title = "Untitled Image";
		}
		else {
			title = newWorkspace.getFile().getName();
		}
		
		this.addTab(title, panel);
	}
	
	@Override
	public void removeWorkspace( ImageWorkspace workspace) {
		for( int i = 0; i < panels.size(); ++i) {
			WorkPanel panel = panels.get(i);
			if( panel.workspace == workspace) {
				this.removeTabAt(i);
				panels.remove(i);
				panel.workSplicePanel.drawPanel.cleanUp();
				break;
			}
		}
	}

	// :::: ChangeListener
	@Override
	public void stateChanged(ChangeEvent evt) {
		if( getSelectedIndex() == -1)
			return;
		
		ImageWorkspace selected = panels.get( getSelectedIndex()).workspace;
		
		if( selected != master.getCurrentWorkspace()) {
			master.setCurrentWorkpace(selected);
		}
	}


	// :::: MouseListener, ActionListener for ContextMenu behavior
	private final WTPContextMenu contextMenu = new WTPContextMenu();
	private class WTPContextMenu extends JPopupMenu {
		int tabID;
	}
	@Override
	public void mousePressed(MouseEvent evt) {
		if( evt.getButton() == MouseEvent.BUTTON3) {
			for( int i = 0; i < getTabCount(); ++i) {
				Rectangle rect = getBoundsAt(i);
				if( rect.contains(evt.getPoint())) {
					
					contextMenu.tabID = i;
					
					String [][] menuScheme =  {
						{"&Close File", "close", null},
					};
					
					contextMenu.removeAll();
					UIUtil.constructMenu(contextMenu, menuScheme, this);
					contextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
		}
	}
	@Override	public void mouseClicked(MouseEvent arg0) {}
	@Override	public void mouseEntered(MouseEvent arg0) {}
	@Override	public void mouseExited(MouseEvent arg0) {}
	@Override	public void mouseReleased(MouseEvent arg0) {}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if( evt.getActionCommand().equals("close")) {
			ImageWorkspace workspace = panels.get(contextMenu.tabID).workspace;
			master.closeWorkspace(workspace);
		}		
	}


	// :::: MWorkspaceFileObserver
	@Override
	public void fileChanged( FileChangeEvent evt) {
		for( int i = 0; i < panels.size(); ++i) {
			if( panels.get(i).workspace == evt.getWorkspace()) {
				setTitleAt(i,  evt.getWorkspace().getFileName() + (evt.hasChanged() ? "*" : ""));
			}
		}
	}
	
	
}
