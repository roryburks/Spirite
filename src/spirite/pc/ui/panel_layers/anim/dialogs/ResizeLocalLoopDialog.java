package spirite.pc.ui.panel_layers.anim.dialogs;

import java.awt.Dimension;
import java.awt.event.WindowEvent;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import spirite.pc.ui.components.MTextFieldNumber;

public class ResizeLocalLoopDialog extends JDialog {
	public boolean success = false;
	public int length = 1;
	public boolean inLoops = false;
	
	public ResizeLocalLoopDialog(int length, boolean inLoops) {
		super( new JFrame(), true);
		//setUndecorated(true);
		
		this.length = length;
		this.inLoops = inLoops;
		

		JLabel mainLabel = new JLabel("Resize Local Loop");
		MTextFieldNumber tfLength = new MTextFieldNumber(false, false);
		JComboBox<String> cbType = new JComboBox<>(new String[] {
			"Length",
			"Loop X Times"
		});
		JButton btnOK = new JButton("OK");
		JButton btnCancel = new JButton("Cancel");

		cbType.setSelectedIndex((inLoops)?1:0);
		tfLength.setText(Integer.toString(length));
		
		GroupLayout layout = new GroupLayout(this.getContentPane());
		
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(mainLabel)
			.addGroup(layout.createSequentialGroup()
				.addComponent(tfLength)
				.addGap(3)
				.addComponent(cbType))
			.addGroup(layout.createSequentialGroup()
				.addGap(0,0,Short.MAX_VALUE)
				.addComponent(btnOK)
				.addGap(3)
				.addComponent(btnCancel)));
		
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addComponent(mainLabel)
			.addGap(3)
			.addGroup(layout.createParallelGroup()
				.addComponent(tfLength, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(cbType, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
			.addGap(3,3,Short.MAX_VALUE)
			.addGroup(layout.createParallelGroup()
						.addComponent(btnOK)
						.addComponent(btnCancel))
			.addGap(3));
		
		this.getContentPane().setLayout(layout);
		this.setSize(240, 120);
		
		btnCancel.addActionListener((evt) -> {
			success = false;
            setVisible(false);
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		});
		btnOK.addActionListener((evt) -> {
			success = true;
			this.length = tfLength.getInt();
			this.inLoops = cbType.getSelectedIndex() == 1;
			
            setVisible(false);
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		});
	}
}
