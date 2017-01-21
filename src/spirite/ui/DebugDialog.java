package spirite.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import spirite.brains.CacheManager.CacheDomain;
import spirite.brains.MasterControl;

public class DebugDialog extends JDialog implements ActionListener {

	private final JPanel contentPanel = new JPanel();

	Timer t = new Timer( 100, this);
	MasterControl master;
	
	JTextPane textPane;
	/**
	 * Create the dialog.
	 */
	public DebugDialog(MasterControl master) {
		this.master = master;
		t.setRepeats(true);
		t.start();
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		textPane = new JTextPane();
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(textPane, GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE))
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(textPane, GroupLayout.PREFERRED_SIZE, 167, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(40, Short.MAX_VALUE))
		);
		contentPanel.setLayout(gl_contentPanel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		DecimalFormat df = new DecimalFormat("#.##");

		String str = "Cache Size:"+ df.format(master.getCacheManager().getCacheSize()/(1024.0*1024.0))+"MB\n";
		
		Map< Object, CacheDomain> map = master.getCacheManager()._debugGetMap();
		
		for( Map.Entry< Object, CacheDomain> set : map.entrySet()) {
			
			str += set.getKey().toString() + " :: ";
			
			str += df.format(set.getValue().getSize() /(1024.0*1024.0)) + "MB\n";
		}
		
		textPane.setText(str);
	}
}
