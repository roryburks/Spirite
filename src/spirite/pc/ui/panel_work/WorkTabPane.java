package spirite.pc.ui.panel_work;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.FileChangeEvent;
import spirite.base.image_data.ImageWorkspace.MWorkspaceFileObserver;
import spirite.base.pen.Penner;
import spirite.gui.hybrid.STabbedPane;
import spirite.pc.ui.ContextMenus;
import spirite.pc.ui.panel_work.WorkPanel.View;

/**
 * WorkTabPane is the top-level UI Component for the Work/Draw area.  In addition 
 * to managing the active Work Area based on tabs, it also handles any interactions 
 * that outside components might need to have with the work panel
 * 
 * 
 * @author Rory Burks
 *
 */
public class WorkTabPane extends STabbedPane 
	implements MWorkspaceObserver,
		MouseListener, ActionListener, MWorkspaceFileObserver
{
	private final MasterControl master;
	
	public final WorkPanel workPanel;
	
	// Panels should maintain a 1:1 assosciation with tabs
	private final List<ImageWorkspace> workspaces= new ArrayList<>();
	
	
	public WorkTabPane( MasterControl master) {
		this.master = master;
		workPanel =  new WorkPanel(master);
		
		master.addWorkspaceObserver(this);
		this.addChangeListener( (evt) -> {
			if( getSelectedIndex() == -1)
				return;
			
			ImageWorkspace selected = workspaces.get( getSelectedIndex());
			
			if( selected != master.getCurrentWorkspace()) {
				master.setCurrentWorkpace(selected);
			}
		});
		this.addMouseListener(this);
	}
	
	
	public View getZoomerForWorkspace( ImageWorkspace ws) 
		{return workPanel.getView(ws);}
	public Penner getPenner() 
		{return workPanel.getPenner();}
	
	// :::: MWorkspaceObserver
	@Override
	public void currentWorkspaceChanged( ImageWorkspace selected, ImageWorkspace previous) {
		for( int i = 0; i < workspaces.size(); ++i) {
			if( workspaces.get(i) == selected) {
				setSelectedIndex(i);
				return;
			}
		}
	}
	
	@Override
	public void newWorkspace( ImageWorkspace newWorkspace) {
		String title = (newWorkspace.getFile() == null)
				? "Untitled Image"
				: newWorkspace.getFile().getName();
		
		newWorkspace.addWorkspaceFileObserve(this);
		
		workspaces.add(newWorkspace);
		
		
		if( this.getTabCount() == 0)
			this.addTab(title, workPanel);
		else
			this.addTab(title, workPanel);
	}
	
	@Override
	public void removeWorkspace( ImageWorkspace workspace) {
		for( int i = 0; i < workspaces.size(); ++i) {
			if( workspaces.get(i) == workspace) {
				this.removeTabAt(i);
				workspaces.remove(i);
				
				if( i == 0 && this.getTabCount() != 0) {
					this.setComponentAt(0, workPanel);
				}
				break;
			}
		}
	}

	// :::: ChangeListener


	// :::: MouseListener, ActionListener for ContextMenu behavior
	private final WTPContextMenu contextMenu = new WTPContextMenu();
	private class WTPContextMenu extends JPopupMenu {
		int tabID;
	}
	@Override	public void mouseClicked(MouseEvent arg0) {}
	@Override	public void mouseEntered(MouseEvent arg0) {}
	@Override	public void mouseExited(MouseEvent arg0) {}
	@Override	public void mouseReleased(MouseEvent arg0) {}
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
					ContextMenus.constructMenu(contextMenu, menuScheme, this);
					contextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		if( evt.getActionCommand().equals("close")) {
			master.closeWorkspace(workspaces.get(contextMenu.tabID));
		}		
	}


	// :::: MWorkspaceFileObserver
	@Override
	public void fileChanged( FileChangeEvent evt) {
		for( int i = 0; i < workspaces.size(); ++i) {
			if( workspaces.get(i) == evt.getWorkspace()) {
				setTitleAt(i,  evt.getWorkspace().getFileName() + (evt.hasChanged() ? "*" : ""));
			}
		}
	}
	
	
}
