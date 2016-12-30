
package spirite;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
        implements KeyEventDispatcher, WindowFocusListener
{
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem menuNewImage;
    
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
    	
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        menuNewImage = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        workPanel.setMinimumSize(new java.awt.Dimension(300, 300));

        toolsPanel.setPreferredSize(new java.awt.Dimension(140, 140));

        jMenu1.setText("File");

        menuNewImage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        menuNewImage.setText("New Image");
        menuNewImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuNewImageActionPerformed(evt);
            }
        });
        jMenu1.add(menuNewImage);

        jMenuItem1.setText("jMenuItem1");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

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

    private void menuNewImageActionPerformed(java.awt.event.ActionEvent evt) {                                             
    	System.out.println("New");
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

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {                                           
        // TODO add your handling code here:
        JColorChooser jcp = new JColorChooser();
        int response = JOptionPane.showConfirmDialog(this, jcp, "Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if( response == JOptionPane.OK_OPTION) {
            master.getPaletteManager().setActiveColor(0, jcp.getColor());
        }

    }                                             



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
    }
    
    // :::: KeyEventDispatcher
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int mod = e.getModifiersEx() & (KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK);

        // Hotkeys
        if( e.getID() == KeyEvent.KEY_RELEASED) {
            int key = e.getKeyCode();
            int modifier = e.getModifiersEx();

            String command = master.getHotekyManager().getCommand( key, modifier);

            // Depending on command domain, RootFrame either performs the command
            //	itself or pass the command onto the appropriate context.
            if( command != null) {
                if( command.startsWith("global."))
                    globalHotkeyCommand(command.substring("global.".length()));
                else if( command.startsWith("toolset."))
                    master.getToolsetManager().setTool(command.substring("toolset.".length()));
                else if( command.startsWith("palette."))
                	master.getPaletteManager().performCommand(command.substring("palette.".length()));

            }

        }
        
        return false;
    }

    // :::: WindowFocusListener
	@Override
	public void windowGainedFocus(WindowEvent evt) {
		master.getFrameManager().showAllFrames();
	}

	@Override	public void windowLostFocus(WindowEvent arg0) {	}
}
