package spirite.pc.ui.panel_anim;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import spirite.base.brains.MasterControl;
import spirite.pc.ui.OmniFrame.OmniComponent;

public class AnimationSchemePanel extends OmniComponent {
	private static final long serialVersionUID = 1L;
	private MasterControl master;

	public AnimationSchemePanel( MasterControl master) {
		this.master = master;
		initComponents();
	}
	
	/**
	 * Create the panel.
	 */
	public void initComponents() {
		
		animationSchemeTreePanel = new AnimationSchemeTreePanel(master);
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(animationSchemeTreePanel, GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(animationSchemeTreePanel, 0, 448, Integer.MAX_VALUE)
					.addContainerGap(77, Short.MAX_VALUE))
		);
		setLayout(groupLayout);
		
	}
	AnimationSchemeTreePanel animationSchemeTreePanel;
	
	// :::: OmniComponent
	@Override
	public void onCleanup() {
		super.onCleanup();
	}
}