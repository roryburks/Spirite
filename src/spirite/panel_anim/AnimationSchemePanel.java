package spirite.panel_anim;

import javax.swing.JPanel;

import spirite.brains.MasterControl;
import spirite.ui.FrameManager.FrameType;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

public class AnimationSchemePanel extends JPanel {
	private MasterControl master;

	public AnimationSchemePanel( MasterControl master) {
		this.master = master;
		initComponents();
	}
	
	/**
	 * Create the panel.
	 */
	public void initComponents() {
		
		AnimationSchemeTreePanel animationSchemeTreePanel = new AnimationSchemeTreePanel();
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
					.addComponent(animationSchemeTreePanel, GroupLayout.PREFERRED_SIZE, 448, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(77, Short.MAX_VALUE))
		);
		setLayout(groupLayout);
		
	}
}
