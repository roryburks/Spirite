package spirite.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import spirite.MDebug;
import spirite.MDebug.DebugObserver;
import spirite.brains.CacheManager.CacheDomain;
import spirite.graphics.gl.engine.GLEngine;
import spirite.brains.MasterControl;

public class DebugDialog extends JDialog 
	implements ActionListener, DebugObserver, WindowListener 
{

	private final JPanel contentPanel = new JPanel();

	Timer t = new Timer( 100, this);
	MasterControl master;
	
	JTextPane textResources;
	JTextPane textDebug = new JTextPane();
	private JPanel buttonPane;
	private JScrollPane scrollPane;
	JButton btnReset = new JButton("Reset Debug Log");
	JLabel lblUsedResources = new JLabel("Used Resources:");
	JLabel lblDebugLog = new JLabel("Debug Log:");
	JScrollPane scrollResources = new JScrollPane();
	/**
	 * Create the dialog.
	 */
	public DebugDialog(MasterControl master) {
		this.master = master;
		
		
		
		MDebug.addLogObserver(this);
		
		t.setRepeats(true);
		t.start();
		setBounds(100, 100, 636, 536);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		
		scrollPane = new JScrollPane();
		
		btnReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MDebug.clearDebugLog();
			}
		});
		
		initLayout();
		
		textResources = new JTextPane();
		scrollResources.setViewportView(textResources);
		textResources.setEditable(false);
		
		this.addWindowListener(this);
		
		updateLog();
	}
	
	private void initLayout() {

		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(lblUsedResources)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addContainerGap()
							.addComponent(scrollResources, GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE))
						.addComponent(lblDebugLog)
						.addGroup(gl_contentPanel.createSequentialGroup()
							.addContainerGap()
							.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 584, GroupLayout.PREFERRED_SIZE)))
					.addGap(16))
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(btnReset)
					.addContainerGap(511, Short.MAX_VALUE))
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addComponent(lblUsedResources)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(scrollResources, GroupLayout.PREFERRED_SIZE, 169, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(lblDebugLog)
					.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(scrollPane, 0, 230, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(btnReset)
					.addContainerGap())
		);
		contentPanel.setLayout(gl_contentPanel);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		final int fix_y = scrollResources.getVerticalScrollBar().getValue();
		
		DecimalFormat df = new DecimalFormat("#.##");
		String str = "Cache Size:"+ df.format(master.getCacheManager().getCacheSize()/(1024.0*1024.0))+"MB\n";
		
		Map< Object, CacheDomain> map = master.getCacheManager()._debugGetMap();
		
		for( Map.Entry< Object, CacheDomain> set : map.entrySet()) {
			str += set.getKey().toString() + " :: ";
			str += df.format(set.getValue().getSize() /(1024.0*1024.0)) + "MB\n";
		}
		if(master.getSettingsManager().glMode()) {
			str +=  "\nGL Resources: ["+df.format(master.getGLCache().getCacheSize()/(1024.0*1024.0))+"MB in cache]\n" + GLEngine.getInstance().dispResourcesUsed();
		}
		textResources.setText(str);
		
		
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				scrollResources.getVerticalScrollBar().setValue(fix_y);
			}
		});
//		textResources.setText(GLEngine.getInstance().dispResourcesUsed());
	}

	// :::: DebugObserver
	@Override
	public void logChanged() {
		updateLog();
	}
	private void updateLog() {
		String str = "";
		
		for( String s : MDebug.getLog()) {
			str += s + "\n";
		}
		scrollPane.setViewportView(textDebug);
		
		textDebug.setText(str);
	}

	// :::: WindowListener
	@Override public void windowActivated(WindowEvent arg0) {}
	@Override public void windowClosed(WindowEvent arg0) {}
	@Override public void windowDeactivated(WindowEvent arg0) {}
	@Override public void windowDeiconified(WindowEvent arg0) {}
	@Override public void windowIconified(WindowEvent arg0) {}
	@Override public void windowOpened(WindowEvent arg0) {}
	@Override
	public void windowClosing(WindowEvent arg0) {
		t.stop();
	}

}
