package spirite.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;

import spirite.brains.MasterControl;
import spirite.panel_layers.LayersPanel;
import spirite.panel_toolset.ToolsPanel;
import spirite.ui.FrameManager.FrameType;

public class OmniFrame extends JDialog{
	List<OmniPanel> panels = new ArrayList<OmniPanel>();
	MasterControl master;
	
	public OmniFrame( MasterControl master, FrameType type) {
		this.master = master;

		OmniPanel panel = null;
		switch( type) {
		case LAYER:
			panel = new LayersPanel( master);
			break;
		case TOOLS:
			panel = new ToolsPanel(master);
			break;
		}
		
		if( panel != null) {
			this.add(panel);
			panels.add(panel);
		}
		
	}
	
	public List<FrameType> getContainedFrameTypes() {
		List<FrameType> list = new ArrayList<FrameType>();

		for( OmniPanel panel : panels) {
			list.add( panel.getFrameType());
		}
		
		return list;
	}
	
	public boolean containsFrameType( FrameType type) {
		for( OmniPanel panel : panels) {
			if( panel.getFrameType() == type)
				return true;
		}
		
		return false;
	}
	
	/***
	 * An omnipanel is just a JPanel that has a special identifier that tells WHAT
	 * kind of panel it is.
	 */
	public static class OmniPanel extends JPanel {
		public FrameType getFrameType() {return FrameType.BAD;}
	}
}
