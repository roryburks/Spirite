package spirite.panel_work;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.ImageWorkspace;
import spirite.ui.UIUtil;

public class WorkTabPane extends JTabbedPane 
	implements MWorkspaceObserver, ChangeListener,
		MouseListener, ActionListener
{
	MasterControl master;
	List<WorkPanel> panels = new ArrayList<>();
	
	
	
	public WorkTabPane( MasterControl master) {
		this.master = master;
		
		master.addWorkspaceObserver(this);
		
		this.addChangeListener(this);
		this.addMouseListener(this);
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
		ImageWorkspace selected = panels.get( getSelectedIndex()).workspace;
		
		if( selected != master.getCurrentWorkspace()) {
			master.setCurrentWorkpace(selected);
		}
	}


	// MouseListener
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
					
					String [][] menuScheme =  {
						{"&Close File", "close", null},
					};
					
					JPopupMenu jpm = new JPopupMenu();
					UIUtil.constructMenu(jpm, menuScheme, this);
					jpm.show(evt.getComponent(), evt.getX(), evt.getY());
				}
			}
		}
	}


	// ActionListener
	@Override
	public void actionPerformed(ActionEvent arg0) {
		System.out.println(arg0.getActionCommand());
		
	}
	
	
}
