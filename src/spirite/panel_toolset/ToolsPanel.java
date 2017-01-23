// Rory Burks

package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import spirite.Globals;
import spirite.MDebug;
import spirite.MDebug.ErrorType;
import spirite.brains.HotkeyManager;
import spirite.brains.HotkeyManager.Hotkey;
import spirite.brains.MasterControl;
import spirite.brains.ToolsetManager;
import spirite.brains.ToolsetManager.MToolsetObserver;
import spirite.brains.ToolsetManager.Tool;

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
        implements ComponentListener, MToolsetObserver
{
	// ToolsPanel needs access to the ToolsetManager and the HotkeyManager
    private final ToolsetManager toolsetManager;
    private final HotkeyManager hotkeyManager;
    
	private static final long serialVersionUID = 1L;
	private static final int BUTTON_WIDTH = 24;
    private static final int BUTTON_HEIGHT = 24;

    private int tool_len;

    BufferedImage icon_sheet = null;
    int is_width, is_height;

    JPanel container;

    ToolButton buttons[];


    public ToolsPanel( MasterControl master) {
    	this.toolsetManager = master.getToolsetManager();
    	this.hotkeyManager = master.getHotekyManager();

    	toolsetManager.addToolsetObserver(this);

        prepareIconSheet();
        initComponents();
        
        this.setOpaque(false);
        
        // Make sure it's created with the proper selected tool
        toolsetChanged(toolsetManager.getSelectedTool());
    }

    // Loads the icon sheet from icons.resources
    private void prepareIconSheet() {
        icon_sheet = null;
        try {
            BufferedImage buff = ImageIO.read ( getClass().getClassLoader().getResource("icons.png").openStream());
            icon_sheet = new BufferedImage( buff.getWidth(), buff.getHeight(), BufferedImage.TYPE_INT_ARGB);
            
            Graphics g = icon_sheet.getGraphics();
            g.drawImage(buff, 0, 0, null);
            g.dispose();

            is_width = icon_sheet.getWidth() / 25;
            is_height = icon_sheet.getHeight() / 25;
        } catch (IOException e) {
        	MDebug.handleError( ErrorType.RESOURCE, this, "Failed to prepare Toolset Icon Sheet");
        }

        // Turns all pixels the same color as the top-left pixel into transparent
        //  pixels
        if( icon_sheet != null) {
            int base = icon_sheet.getRGB(0, 0);

            for(int x = 0; x < icon_sheet.getWidth(); ++x) {
                for( int y = 0; y < icon_sheet.getHeight(); ++y)
                    if( base == icon_sheet.getRGB(x, y))
                        icon_sheet.setRGB(x, y, 0);
            }
        }
    }

    private void initComponents() {
        // The toolset panel is simply a JPanel with a Grid Layout inside
        //  a JPanel with a Box Layout (so that the grid's height can be
        //  manipulated directly)
        this.setLayout( new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.addComponentListener(this);
        this.setPreferredSize(new java.awt.Dimension(140, 140));

        container = new JPanel();
        add(container);

        // :: Add Toolset Buttons
        int x = 0;
        int y = 0;

        tool_len = toolsetManager.getToolCount();

        buttons = new ToolButton[tool_len];
        for( int i = 0; i < tool_len; ++i) {
            buttons[i] = new ToolButton( toolsetManager.getNthTool(i));
            container.add( buttons[i]);
            x += 1;
            if( x >= is_width) {
                x = 0;
                y = (y + 1) % is_height;
            }
        }

        // Calibrate the grid
        // Note, this is invoked later so that getWidth works correctly
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                calibrateGrid();
            }
        });

    }

    /**
     * Calibrates the Grid such that each button is as close to BUTTON_WIDTH
     * wide and is exactly BUTTON_HEIGHT tall (unless it needs to squash to fit)
     */
    private void calibrateGrid() {
        GridLayout layout = new GridLayout();

        int cols = Math.max(1, getWidth() / BUTTON_WIDTH);
        int rows = (int) Math.ceil( tool_len / (float)cols);
        layout.setColumns( cols);
        layout.setRows(0);
        layout.setHgap(0);
        layout.setVgap(0);
        container.setLayout(layout);

        container.setMaximumSize(new Dimension( 9999, rows*BUTTON_HEIGHT));
    }

    // :::: Component Listener
    @Override
    public void componentResized(ComponentEvent e) {
        calibrateGrid();
    }
    @Override
    public void componentMoved(ComponentEvent e) {}
    @Override
    public void componentShown(ComponentEvent e) {}
    @Override
    public void componentHidden(ComponentEvent e) {}

    @Override
    public void toolsetChanged( Tool newTool) {
        for( ToolButton button : buttons) {
            if( button.tool.equals(newTool)) {
                if( !button.isSelected()) {
                    button.setSelected(true);
                    button.repaint();
                }
            }
            else if( button.isSelected()) {
                button.setSelected(false);
                button.repaint();
            }
        }
    }


    /**
     * Tool Button
     */
    class ToolButton extends JToggleButton
            implements ActionListener, MouseListener
    {
		private static final long serialVersionUID = 1L;
		private int ix, iy;
        private Tool tool;

        boolean hover = false;

        ToolButton( Tool tool) {
            this.tool = tool;
            this.ix = toolsetManager.getToolix(tool);
            this.iy = toolsetManager.getTooliy(tool);
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

            
            g.drawImage( icon_sheet, ew / 2, 0, ew/2 + 24, 24,
                    ix*25, iy*25, ix*25+24, iy*25+24, null);

            if( hover) {
                Graphics2D g2 = (Graphics2D)g;
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

}
