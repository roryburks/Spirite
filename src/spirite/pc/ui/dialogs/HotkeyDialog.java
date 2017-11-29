package spirite.pc.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import spirite.base.brains.HotkeyManager;
import spirite.base.brains.HotkeyManager.Hotkey;
import spirite.base.brains.MasterControl;
import spirite.gui.hybrid.SButton;
import spirite.gui.hybrid.SPanel;
import spirite.pc.ui.components.SimpleTree;

/**
 * HotkeyDialog provides a user-interface for adding/changing hotkeys for any 
 * of the available CommandStrings.
 * 
 * @author Rory Burks
 */
public class HotkeyDialog extends JDialog {
	private final String HOTKEY_CHANGE = "Double-Click on Hotkey to Change.";
	private final String HOTKEY_CHANGING = ": Press Key Combo to change hotkey.";
	
	// External Components
	private final HotkeyManager hotkeyManager;
	
	// Components
	private final SPanel contentPanel = new SPanel();
	private JScrollPane scrollPane = new JScrollPane();
	private final SimpleTree tree = new SimpleTree();
	private final List<HKTable> tables = new ArrayList<>();
	private final JLabel lblStat = new JLabel(HOTKEY_CHANGE);
	
	/**
	 * Create the dialog.
	 */
	public HotkeyDialog( MasterControl master) {
		this.hotkeyManager = master.getHotekyManager();
		initComponents();
		
		List<String> commands = master.getAllValidCommands();
		
		// Go through all commands them to the list, and add the command and
		//	corresponding Hotkey to they 
		for( String command : commands) {
			String domain = command.substring(0, command.indexOf('.'));
			
			HKTable tableIn = null;
			for( HKTable table : tables) {
				if( table.domain.equals(domain)) {
					tableIn = table;
					break;
				}
			}
			if( tableIn == null) {
				tableIn = new HKTable(domain);
				tables.add(tableIn);
			}
			tableIn.entries.add( new HKEntry( command, hotkeyManager.getHotkey(command)));
			
		}
		for( HKTable table : tables){
			table.construct();
			
			tree.addTab( 
					new JLabel(table.domain), 
					table, 
					null, false);
		}
	}
	
	private void initComponents() {
		setBounds(100, 100, 450, 500);
		scrollPane.setViewportView(tree);
		
		// Construct External Layout
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.TRAILING)
				.addGroup(Alignment.LEADING, gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 404, Short.MAX_VALUE)
					.addContainerGap())
				.addGroup(Alignment.CENTER, gl_contentPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(lblStat)
						.addContainerGap())
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addContainerGap()
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
					.addGap(3)
					.addComponent(lblStat)
					.addContainerGap())
		);
		contentPanel.setLayout(gl_contentPanel);
		{
			SPanel buttonPane = new SPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				SButton okButton = new SButton("OK");
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				okButton.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
					}
				});
				getRootPane().setDefaultButton(okButton);
			}
		}
	}
	
	// ====================
	// ==== State Management
	String editingCommand = null;
	
	public void setEditingCommand( String command) {
		editingCommand = command;
		if( command == null) {
			lblStat.setText(HOTKEY_CHANGE);
		}
		else {
			lblStat.setText(command + HOTKEY_CHANGING);
		}
	}
	
	// =========
	// ==== etc
	private void commandChanged( String command) {
		String domain = command.substring(0, command.indexOf('.'));
		Hotkey key = hotkeyManager.getHotkey(command);
		
		for( HKTable table : tables) {
			if( table.domain.equals(domain)) {
				for( int i=0; i< table.model.getRowCount(); ++i) {
					if( table.entries.get(i).command.equals(command)) {
						table.model.setValueAt( key, i, 1);
					}
				}
			}
		}
	}

	// ====================
	// ==== Custom Components
	private class HKTableModel extends DefaultTableModel {
		HKTableModel( Object[][] data) {
			super( data, new Object[]{"command","hotkey"});
		}
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	}

	static class HKEntry {
		HKEntry( String command, Hotkey hotkey) {
			this.command = command;
			this.hotkey = hotkey;
		}
		String command;
		Hotkey hotkey;
	}
	private class HKTable extends JTable 
		implements MouseListener, KeyListener 
	{
		private final String domain;
		private final List<HKEntry> entries = new ArrayList<>();
		private HKTableModel model = null;
		HKTable( String domain) {
			this.domain = domain;
			this.setBorder( BorderFactory.createLineBorder(Color.BLACK));
			this.addKeyListener(this);
		}
		
		void construct() {
			Object[][] data = new Object[entries.size()][];
			
			for( int i=0; i<entries.size(); ++i) {
				data[i] = new Object[]{ entries.get(i).command, entries.get(i).hotkey};
			}
			
			this.model = new HKTableModel(data);
			setModel( model);
			
			this.addMouseListener(this);
		}

		// :::: MouseListener
		@Override public void mouseClicked(MouseEvent arg0) {}
		@Override public void mouseReleased(MouseEvent arg0) {}
		@Override public void mouseEntered(MouseEvent arg0) {}
		@Override public void mouseExited(MouseEvent arg0) {}
		@Override
		public void mousePressed(MouseEvent evt) {
			if( evt.getClickCount() >= 2) {
				int row = rowAtPoint(evt.getPoint());
				int col = columnAtPoint(evt.getPoint());
				setEditingCommand( entries.get(row).command);
			}
			else setEditingCommand(null);
		}

		// :::: KeyListener
		@Override public void keyReleased(KeyEvent arg0) {}
		@Override public void keyTyped(KeyEvent arg0) {}
		@Override
		public void keyPressed(KeyEvent evt) {
			if( editingCommand != null && !HotkeyManager.isModifier(evt.getKeyCode())) {
				int key = evt.getKeyCode();
				int mod = evt.getModifiersEx();
				
				String cmd = hotkeyManager.getCommand(key, mod);
				hotkeyManager.setCommand(key, mod, editingCommand);
				if( cmd != null) {
					commandChanged( cmd);
				}
				commandChanged( editingCommand);
				
				setEditingCommand(null);
			}
		}
	}
	
}
