
package spirite.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JColorChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import spirite.Globals;
import spirite.MDebug;
import spirite.brains.MasterControl;
import spirite.dialogs.NewImagePanel;
import spirite.panel_anim.AnimPanel;
import spirite.panel_toolset.PalettePanel;
import spirite.panel_toolset.ToolsPanel;
import spirite.panel_work.WorkPanel;

/**
 * While the MasterControl is "home base" for all the internals of the program, the root
 * frame is home base for the UI.  In addition to containing the menu bar and whatever
 * panels are attached to it, it also is the delegator for all of the Hotkeys.
 */
public class RootFrame extends javax.swing.JFrame
        implements KeyEventDispatcher, WindowFocusListener, ActionListener
{
    private AnimPanel animPanel;
    private PalettePanel palettePanel;
    private ToolsPanel toolsPanel;
    private WorkPanel workPanel;
    
    private MasterControl master;


    public RootFrame( MasterControl master) {
        this.master =  master;
        initComponents();
        

        this.addWindowFocusListener( this);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);

        master.newImage(128,128,new java.awt.Color(0,0,0,0));
    }

    private void initComponents() {
    	workPanel = new WorkPanel( master);
    	toolsPanel = new ToolsPanel( master);
    	animPanel = new AnimPanel( master);
    	palettePanel = new PalettePanel( master);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        workPanel.setMinimumSize(new java.awt.Dimension(300, 300));

        toolsPanel.setPreferredSize(new java.awt.Dimension(140, 140));

        initMenu();
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(workPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 535, Short.MAX_VALUE)
                    .addComponent(animPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 501, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(toolsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(palettePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(toolsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(palettePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(workPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 340, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(animPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        pack();
    }                   

    
    public static final int MAX_LEVEL = 10;
    private int _imCountLevel( String s){
    	int r = 0;
    	while( s.charAt(r) == '.')
    		r++;
    	return Math.min(r, MAX_LEVEL);
    }
    private void initMenu() {

    	
    	Object[][] menu_scheme = Globals.getMenuSchem();
    	

    	JMenuBar jMenuBar = new JMenuBar();
		JMenuItem new_node;
    	
    	JMenuItem[] active_root_tree = new JMenuItem[MAX_LEVEL];
    	
    	// Atempt to construct menu from parsed data in menu_scheme
		// !!!! TODO: note, there are very few sanity checks in here for now
    	int active_level = 0;
    	for( int i = 0; i < menu_scheme.length; ++i) {
    		int level =_imCountLevel((String)menu_scheme[i][0]);
    		menu_scheme[i][0] = ((String)menu_scheme[i][0]).substring(level);
    		
    		// Determine if it needs to be a Menu (which contains other options nested in it)
    		//	or a plain MenuItem (which doesn't)
    		if( level != 0 && (i+1 == menu_scheme.length || _imCountLevel((String)menu_scheme[i+1][0]) <= level)) {
    			new_node = new JMenuItem( (String) menu_scheme[i][0]);
    		}
    		else {
    			new_node = new JMenu( (String) menu_scheme[i][0]);
    		}
    		new_node.setMnemonic((int)menu_scheme[i][1]);

    		if( menu_scheme[i][2] != null) {
    			new_node.setActionCommand((String)menu_scheme[i][2]);
    			new_node.addActionListener(  this);
    		}
    		
    		// Add the MenuItem into the appropriate context
    		if( level == 0) {
    			jMenuBar.add( new_node);
    		}
    		else {
    			active_root_tree[level-1].add(new_node);
    		}
    		active_root_tree[ level] = new_node;
    	}
    	

        setJMenuBar(jMenuBar);
    }
    
    // :::: Menu Actions
    
    /***
     * Prompts for a new image dialog and then perofms it
     */
    private void promptNewImage() {     
        NewImagePanel panel = new NewImagePanel(master);

        int response = JOptionPane.showConfirmDialog(this,
                panel,
                "New Image",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            master.newImage(panel.getValueWidth(), panel.getValueHeight(), panel.getValueColor());
        }
    }                                            

    /***
     * 
     */
    private void promptDebugColor() {  
        // TODO DEBUG
        JColorChooser jcp = new JColorChooser();
        int response = JOptionPane.showConfirmDialog(this, jcp, "Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            master.getPaletteManager().setActiveColor(0, jcp.getColor());
        }

    }                                             



    /***
     * Performs the given hotkey command string or deligates the command to the
     * appropriate component
     */
    public void performCommand( String command) {
        if( command != null) {
            if( command.startsWith("global."))
                globalHotkeyCommand(command.substring("global.".length()));
            else if( command.startsWith("toolset."))
                master.getToolsetManager().setTool(command.substring("toolset.".length()));
            else if( command.startsWith("palette."))
            	master.getPaletteManager().performCommand(command.substring("palette.".length()));
            else if( command.startsWith("frame."))
            	master.getFrameManager().performCommand(command.substring("frame.".length()));
            else {
            	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown Command String prefix: " + command);
            }
        }
    }
    
    /***
     * Performs the given hotkey command string (should be of "global." focus)
     */
    private void globalHotkeyCommand( String command) {
        if( command.equals("zoom_in")) {
            int zl = workPanel.getZoomLevel();
            if( zl >= 11)
                workPanel.zoom(((zl+1)/4)*4 + 3);   // Arithmetic's a little unintuitive because of zoom_level's off by 1
            else if( zl >= 3)
                workPanel.zoom(((zl+1)/2)*2 + 1);
            else
                workPanel.zoom(zl+1);
        }
        else if( command.equals("zoom_out")) {
            int zl = workPanel.getZoomLevel();
            if( zl > 11)
                workPanel.zoom((zl/4)*4-1);
            else if( zl > 3)
                workPanel.zoom((zl/2)*2-1);
            else
                workPanel.zoom(zl-1);
        }
        else if( command.equals("zoom_in_slow"))
            workPanel.zoom(workPanel.getZoomLevel()+1);
        else if( command.equals("zoom_out_slow"))
            workPanel.zoom(workPanel.getZoomLevel()-1);
        else if( command.equals("zoom_0"))
            workPanel.zoom(0);
        else if( command.equals("new_image"))
        	promptNewImage();
        else if( command.equals("debug_color"))
        	promptDebugColor();
        else {
        	MDebug.handleWarning( MDebug.WarningType.REFERENCE, this, "Unknown global command: global." + command);
        }
    }
    
    // :::: KeyEventDispatcher
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int mod = e.getModifiersEx() & (KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

        // Hotkeys
        if( e.getID() == KeyEvent.KEY_PRESSED) {
            int key = e.getKeyCode();
            int modifier = e.getModifiersEx();

            String command = master.getHotekyManager().getCommand( key, modifier);

            performCommand(command);

        }
        
        return false;
    }

    // :::: WindowFocusListener
	@Override
	public void windowGainedFocus(WindowEvent evt) {
		// !!!! TODO : This causes issues when you move a window to a different Desktop/Workspace
		//	but solving this problem in pure Java will either be hacky or impossible.
		
		if( evt.getOppositeWindow() == null) {
			master.getFrameManager().showAllFrames();
			this.toFront();
		}
	}

	@Override	public void windowLostFocus(WindowEvent arg0) {	}

	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		performCommand(evt.getActionCommand());
	}
}
