package spirite.pc.ui.panel_layers.layer_properties;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import spirite.base.brains.MasterControl;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.layers.puppet.Puppet;
import spirite.base.image_data.layers.puppet.PuppetLayer;
import spirite.hybrid.Globals;
import spirite.pc.ui.components.BoxList;

public class PuppetLayerPanel extends JPanel 
	implements ActionListener
{
	private final MasterControl master;
	private ImageWorkspace workspace;
	PuppetLayer puppet;
	
	PuppetLayerPanel(MasterControl master) {
		this.master = master;
		
		initLayout();
		refreshProperties();
	}
	
	public void setPuppet( PuppetLayer puppet, ImageWorkspace imageWorkspace) {
		this.puppet = puppet;
		this.workspace = imageWorkspace;
		refreshProperties();
	}
	
	private final String bpp_NORMAL = "Norm";
	private final String bpp_BASE = "Base";
	
	
	private JToggleButton btnSkeleton = new JToggleButton();
	private JToggleButton btnBase = new JToggleButton();
	private BoxList<Puppet.Part> boxPuppetParts = new BoxList<>(null, 24, 24);
	private void initLayout() {
		btnSkeleton.setToolTipText("Toggle Skeleton Visibility");
		btnSkeleton.setActionCommand("toggleSkeleton");
		btnSkeleton.addActionListener(this);
		btnSkeleton.setIcon( Globals.getIcon("icon.puppet.skeleton"));
		btnSkeleton.setMargin(new Insets(0, 0, 0, 0));
		
		btnBase.setToolTipText("Toggle Base View");
		btnBase.setActionCommand("toggleBase");
		btnBase.addActionListener(this);
		btnBase.setMargin(new Insets(0, 0, 0, 0));
		
		int btnHeight = 16;
		int btnSkelWidth = 24;
		int btnBaseWidth = 48;
		
		GroupLayout layout = new GroupLayout(this);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(0,0,Short.MAX_VALUE)
					.addComponent(btnSkeleton, btnSkelWidth, btnSkelWidth, btnSkelWidth)
					.addGap(3)
					.addComponent(btnBase, btnBaseWidth, btnBaseWidth, btnBaseWidth))
				.addComponent(boxPuppetParts))
			.addGap(3));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup(layout.createParallelGroup()
				.addComponent(btnBase, btnHeight, btnHeight, btnHeight)
				.addComponent(btnSkeleton, btnHeight, btnHeight, btnHeight))
			.addGap(3)
			.addComponent(boxPuppetParts)
			.addGap(3));
		
		this.setLayout(layout);
	}
	
	private void refreshProperties() {
		btnBase.setSelected( puppet != null && puppet.isUsingBase());
		btnBase.setText( btnBase.isSelected() ? bpp_BASE : bpp_NORMAL);
		btnBase.setEnabled( puppet != null && !puppet.isBaseOnly());
		
		btnSkeleton.setEnabled( puppet != null);
		btnSkeleton.setSelected( puppet != null && puppet.isSkeletonVisible());
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		switch( e.getActionCommand()) {
		case "toggleSkeleton":
			puppet.setSkeletonVisible(btnSkeleton.isSelected());
			refreshProperties();
			break;
		case "toggleBase":
			puppet.setUsingBase( btnBase.isSelected());
			refreshProperties();
			break;
		}
	}
}
