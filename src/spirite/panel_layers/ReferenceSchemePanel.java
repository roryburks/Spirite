package spirite.panel_layers;

import javax.swing.GroupLayout;

import spirite.brains.MasterControl;
import spirite.ui.OmniFrame.OmniComponent;

public class ReferenceSchemePanel extends OmniComponent{
	private final ReferenceTreePanel referenceTreePanel;
	
	
	public ReferenceSchemePanel(MasterControl master) {
		referenceTreePanel = new ReferenceTreePanel(master);
		initComponents();
		System.out.println("TEST");
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
}
