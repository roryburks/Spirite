// Rory Burks

package spirite.pc.ui.panel_toolset;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import spirite.base.brains.HotkeyManager;
import spirite.base.brains.HotkeyManager.Hotkey;
import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.MToolsetObserver;
import spirite.base.brains.ToolsetManager.Property;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.ImageWorkspace.MNodeSelectionObserver;
import spirite.base.image_data.mediums.drawer.IImageDrawer;
import spirite.hybrid.Globals;
import spirite.hybrid.tools.ToolsetIcons;
import spirite.pc.ui.components.BoxList;

/**
 * 
 * 
 * Note: there is some redundant code between the way this loads its
 * "IconSheet" for buttons and the Global.getIcon method, but this
 * Component efficiently uses a single BufferedImage without splitting
 * it up so for now I'm keeping it.
 * 
 * @author Rory Burks
 *
 */
public class ToolsPanel extends JPanel
        implements ComponentListener, MToolsetObserver, MWorkspaceObserver, MNodeSelectionObserver, MFlashObserver
{
	// ToolsPanel needs access to the ToolsetManager and the HotkeyManager
    private final ToolsetManager toolsetManager;
    private final HotkeyManager hotkeyManager;
    private ImageWorkspace workspace;
    
	private static final long serialVersionUID = 1L;
	private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 24;

    private int tool_len;


    JPanel container;

    //ToolButton buttons[];
    BoxList<Tool> boxList = new BoxList<>(null, BUTTON_WIDTH, BUTTON_HEIGHT);


    public ToolsPanel( MasterControl master) {
    	this.toolsetManager = master.getToolsetManager();
    	this.hotkeyManager = master.getHotekyManager();

    	toolsetManager.addToolsetObserver(this);
    	master.addWorkspaceObserver(this);
    	master.addTrackingObserver(MFlashObserver.class, this);
    	
    	this.currentWorkspaceChanged(master.getCurrentWorkspace(), null);

        initComponents();
        
        this.setOpaque(false);
        
        // Make sure it's created with the proper selected tool
        toolsetChanged(toolsetManager.getSelectedTool());
    }


    private void initComponents() {
        // The toolset panel is simply a JPanel with a Grid Layout inside
        //  a JPanel with a Box Layout (so that the grid's height can be
        //  manipulated directly)
        this.setLayout( new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.addComponentListener(this);
        this.setPreferredSize(new java.awt.Dimension(140, 140));

        
        boxList.setRenderer((t, index, selected) -> {
        	ToolButton btn = new ToolButton(t);
        	btn.setSelected(selected);
        	return btn;
        });
        boxList.setFocusable(false);
        
        //container = new JPanel();
        add(boxList);
    }
    
    private List<Tool> currentTools;
    private void Build( List<Tool> tools) {
    	currentTools = tools;
    	
    	boxList.resetEntries(tools, -1);
    }

    // :::: Component Listener
    @Override
    public void componentResized(ComponentEvent e) {
        //calibrateGrid();
    }
    @Override
    public void componentMoved(ComponentEvent e) {}
    @Override
    public void componentShown(ComponentEvent e) {}
    @Override
    public void componentHidden(ComponentEvent e) {}

    // :::: MToolsetObserver
    @Override public void toolsetPropertyChanged(Tool tool, Property property) {}
    @Override
    public void toolsetChanged( Tool newTool) {
    	if( currentTools != null)
    		boxList.setSelectedIndex( currentTools.indexOf(newTool));
    }


    /**
     * Tool Button
     */
    class ToolButton extends JToggleButton
            implements ActionListener, MouseListener
    {
		private static final long serialVersionUID = 1L;
        private Tool tool;

        boolean hover = false;

        ToolButton( Tool tool) {
            this.tool = tool;
            this.addActionListener(this);
            this.addMouseListener(this);
            this.setBorder(null);


            Hotkey key = hotkeyManager.getHotkey("toolset." + tool.name());
            
            this.setToolTipText("<html>" + tool.description + " <b>" + ((key==null)?"":key.toString()) + "</b></html>" );

            // Because the component can be semi-transparent at times, this is
            //  needed to redraw the background when the component is redrawn
            this.setOpaque(false);
        }

        @Override
        public void paintComponent( Graphics g) {
        	// I'm not sure if this is the best way to go about calling super.paintComponent
        	//	without it actually drawing anything.
        	Rectangle r = g.getClipBounds();
        	g.setClip(0, 0, 0, 0);
        	super.paintComponent(g);
        	g.setClip(r.x, r.y, r.width, r.height);
        	
            int w = this.getWidth();
            int h = this.getHeight();
            int ew = w - BUTTON_WIDTH;


            if( isSelected()) {
                g.setColor( Globals.getColor("toolbutton.selected.background"));
                g.fillRect(0, 0, w, h);
            }

            Graphics2D g2 = (Graphics2D)g;
            g2.translate(ew/2, 0);
            ToolsetIcons.drawIcon(g2, tool);
            g2.translate(-ew/2, 0);
            

            if( hover) {
                Shape shape = new java.awt.geom.RoundRectangle2D.Float(0, 0, this.getWidth()-1, this.getHeight()-1, 5, 5);

                GradientPaint grad = new GradientPaint(w, 0, new Color(0,0,0,0), w, h, new Color(255,255,255,128));
                g2.setPaint(grad);
                g2.fill( shape);

                g2.setColor(Color.black);
                g2.draw(shape);
            }

        }

        // :: On Button Click
        @Override
        public void actionPerformed(ActionEvent e) {
        	toolsetManager.setSelectedTool(((ToolButton)e.getSource()).tool);
        }

        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mousePressed(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {
            hover = true;
            this.repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hover = false;
            this.repaint();
        }
    }


	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( workspace != null) {
			workspace.removeSelectionObserver(this);
		}
		this.workspace = selected;
		if( workspace != null) {
			workspace.addSelectionObserver(this);
		}
		selectionChanged((workspace == null) ? null : workspace.getSelectedNode());
	}
	@Override public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override public void removeWorkspace(ImageWorkspace newWorkspace) {}


	@Override
	public void selectionChanged(Node newSelection) {
		if( workspace == null)
			return;
		Build( toolsetManager.getToolsForDrawer(workspace.getDrawerFromNode(newSelection)));
	}


	Class<?> currentDrawerType = null;
	@Override
	public void flash() {
		IImageDrawer drawer = workspace.getActiveDrawer();
		if( drawer != null && currentDrawerType != drawer.getClass() ) {
			currentDrawerType = drawer.getClass();
			List<Tool> tools = toolsetManager.getToolsForDrawer(drawer);
			if(!tools.contains(toolsetManager.getSelectedTool()) && tools.size() != 0)
				toolsetManager.setSelectedTool(tools.get(0));
			Build( tools);
		}
	}

}
