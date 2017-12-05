package spirite.pc.ui.dialogs.root;

import spirite.base.image_data.ImageWorkspace;
import spirite.gui.hybrid.SPanel;
import spirite.pc.ui.components.MTextFieldNumber;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

public class JResizeWorkspace extends SPanel {
	private final ImageWorkspace workspace;
	
	MTextFieldNumber tfWidth = new MTextFieldNumber(false, false);
	MTextFieldNumber tfHeight = new MTextFieldNumber(false, false);
	JLabel lblWidth = new JLabel("Width");
	JLabel lblHeight = new JLabel("Height");
	
	public JResizeWorkspace(ImageWorkspace workspace) {
		this.workspace = workspace;
		initComponents();
	}
	
	public int getValueWidth() {return tfWidth.getInt();}
	public int getValueHeight() {return tfHeight.getInt();}
	
	private void initComponents() {
		tfWidth.setInt(workspace.getWidth());
		tfHeight.setInt(workspace.getHeight());
		
		// Layout
		GroupLayout layout = new GroupLayout(this);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addComponent(tfWidth, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addGap(3)
					.addComponent(lblWidth)
					.addGap(3)
					.addComponent(tfHeight, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addGap(3)
					.addComponent(lblHeight)))
			.addGap(3));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
				.addComponent(tfHeight)
				.addComponent(tfWidth)
				.addComponent(lblHeight)
				.addComponent(lblWidth))
			.addGap(3));

		layout.linkSize(SwingUtilities.HORIZONTAL, lblWidth, lblHeight);
		//layout.linkSize(SwingUtilities.HORIZONTAL, tfWidth, tfHeight);
		
		//this.setSize(new Dimension(280, 164));
		
		this.setLayout(layout);
	}
}
