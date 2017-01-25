package spirite.panel_layers;

import javax.swing.GroupLayout;

import spirite.brains.MasterControl;
import spirite.ui.OmniFrame.OmniComponent;

/**
 * 
 * The Reference System works like this: you can drag any Nodes
 * you want into the ReferenceSchemePanel and those nodes are 
 * drawn either above or bellow the image using various adjustable
 * render settings.  It also possesses different zoom.
 * 
 * The visibility, structure and orientation of the reference
 * section will not change the original image and are not saved
 * by the UndoEngine, but changes to the actual image data are.
 * 
 * 
 * @author Rory Burks
 *
 */
public class ReferenceSchemePanel extends OmniComponent{
	private final ReferenceTreePanel referenceTreePanel;
	
	
	public ReferenceSchemePanel(MasterControl master) {
		referenceTreePanel = new ReferenceTreePanel(master);
		initComponents();
	}
	
	private void initComponents() {
		GroupLayout layout = new GroupLayout(this);
		
		layout.setHorizontalGroup( layout.createSequentialGroup()
			.addGap(3)
			.addComponent(referenceTreePanel,0,160,Short.MAX_VALUE)
			.addGap(3)
		);
		
		layout.setVerticalGroup( layout.createSequentialGroup()
			.addGap(3)
			.addComponent(referenceTreePanel,0,500,Short.MAX_VALUE)
			.addGap(3)
		);
		
		this.setLayout(layout);
	}
	
	@Override
	public void onCleanup() {
		referenceTreePanel.cleanup();
	}
}
