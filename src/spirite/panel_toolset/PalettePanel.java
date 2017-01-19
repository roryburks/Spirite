// Rory Burks

package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.brains.PaletteManager;
import spirite.brains.PaletteManager.MPaletteObserver;
import spirite.dialogs.Dialogs;
import spirite.ui.UIUtil;

public class PalettePanel extends JPanel 
        implements MouseListener, MPaletteObserver, ActionListener
{
	// PalettePanel only needs access to PaletteManager
    private final PaletteManager paletteManager;
    
	private static final long serialVersionUID = 1L;
	private final static int BIG_SIZE = 20;
    private final static int SMALL_SIZE = 12;

    private ColorPicker main, sub;
    private JScrollPane container;
    private PaletteSubpanel palette;
    private JButton btnSavePalette;
    private JButton btnLoadPalette;
    private JButton btnAddColor;

    public PalettePanel( MasterControl master) {
        paletteManager = master.getPaletteManager();

        initComponents();
        palette.addMouseListener(this);
        sub.addMouseListener(this);

        paletteManager.addPaletteObserver(this);
    }

    // Set up the GUI
    void initComponents() {
                GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        main = new ColorPicker( 0);
        main.addMouseListener(this);

        sub = new ColorPicker( 1);

        palette = new PaletteSubpanel();

        container = new JScrollPane(palette, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        container.getHorizontalScrollBar().setPreferredSize(new Dimension(5, 0));

        btnSavePalette = new JButton();
        btnLoadPalette = new JButton();
        btnAddColor = new JButton();
        btnSavePalette.setToolTipText("Save Palette");
        btnLoadPalette.setToolTipText("Load Palette");
        btnAddColor.setToolTipText("Add Color");
        
        btnSavePalette.addActionListener( this);
        btnLoadPalette.addActionListener( this);
        btnAddColor.addActionListener( this);

        btnSavePalette.setIcon(Globals.getIcon("palSavePalette"));
        btnLoadPalette.setIcon(Globals.getIcon("palLoadPalette"));
        btnAddColor.setIcon(Globals.getIcon("palNewColor"));

        btnSavePalette.setBackground( new Color( 170,170,220));
        btnLoadPalette.setBackground( new Color( 170,170,220));
        btnAddColor.setBackground( new Color( 170,170,220));
        
        Dimension bsize = new Dimension(24, 12);
        

        layout.setHorizontalGroup(layout.createParallelGroup( GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10)
                .addComponent(main, BIG_SIZE,BIG_SIZE,BIG_SIZE)
            )
            .addGroup(layout.createSequentialGroup()
                .addGap(20)
                .addComponent(sub, BIG_SIZE,BIG_SIZE,BIG_SIZE)
            )
            .addGroup( layout.createSequentialGroup()
                .addGap(20 + BIG_SIZE + 5)
                .addGroup(layout.createParallelGroup( GroupLayout.Alignment.TRAILING)
	            	.addGroup(layout.createSequentialGroup()
	                    .addComponent(container)
	                )
	            	.addGroup(layout.createSequentialGroup()
	            		.addComponent(btnSavePalette, bsize.width, bsize.width, bsize.width)
	            		.addGap(1)
	            		.addComponent(btnLoadPalette, bsize.width, bsize.width, bsize.width)
	            		.addGap(1)
	            		.addComponent(btnAddColor, bsize.width, bsize.width, bsize.width)
	            	)
	            )
            )

        );
        layout.setVerticalGroup(layout.createParallelGroup( GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(10)
                        .addComponent(main, BIG_SIZE,BIG_SIZE,BIG_SIZE)
                )
                .addGroup(layout.createSequentialGroup()
                        .addGap(20)
                        .addComponent(sub, BIG_SIZE,BIG_SIZE,BIG_SIZE)
                )
                .addGroup( layout.createSequentialGroup()
               		.addComponent(container)
               		.addGroup( layout.createParallelGroup()
               			.addComponent(btnSavePalette, bsize.height, bsize.height, bsize.height)
               			.addComponent(btnLoadPalette, bsize.height, bsize.height, bsize.height)
               			.addComponent(btnAddColor, bsize.height, bsize.height, bsize.height)
               		)
                )
        );
    }

    // :: MPaletteObserver
    @Override
    public void colorChanged() {
        main.setBackground( paletteManager.getActiveColor(0));
        sub.setBackground( paletteManager.getActiveColor(1));
        
        repaint();
        palette.repaint();
    }

    // :: MouseEventListener
    int startX, startY;
    int currentlyOver = -1;
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override    public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {
        startX = e.getX();
        startY = e.getY();
    }
    @Override 
    public void mouseReleased(MouseEvent e) {
    	Component draggedFrom = e.getComponent();
    	if( draggedFrom.contains(e.getPoint())) {
    		if( draggedFrom instanceof ColorPicker) {
    			Color c = Dialogs.pickColor();
    			
    			if( c != null) {
    				paletteManager.setActiveColor(((ColorPicker)draggedFrom).index, c);
    				if( !paletteManager.getColors().contains(c))
    					paletteManager.addPaletteColor(c);
    			}
    		}
    		else if( draggedFrom == palette) {
    			// A mouse drag from inside the PaletteSubpanel to itself can either
    			// start and end on 
    			int startIndex = palette.getIndexAt(startX, startY);
    			int endIndex = palette.getIndexAt(e.getX(), e.getY());

    			Color startC = paletteManager.getPaletteColor(startIndex);
    			
    			if( startIndex == -1 || endIndex == -1){}
    			else if( startIndex == endIndex) {
    				if( e.getClickCount() == 2 || startC == null) {
    					// Color Pick new Palette Color
    					Color c = Dialogs.pickColor( startC);
    					if( c != null)  {
    						paletteManager.setPaletteColor(startIndex, c);
        					paletteManager.setActiveColor(e.getButton()/2, c);
    					}
    				}
    				else
    					paletteManager.setActiveColor(e.getButton()/2, paletteManager.getPaletteColor(startIndex));
    			}
    			else {
    				// Move color from one place to another
    				paletteManager.setPaletteColor(endIndex, startC);
    			}
    		}
    	}
    	else {
    		// You have Dragged from one component to another.
    		Component draggedTo = null;
    		if( main.contains(SwingUtilities.convertPoint(draggedFrom, e.getPoint(), main)))
    			draggedTo = main;
    		else if(sub.contains(SwingUtilities.convertPoint(draggedFrom, e.getPoint(), sub)))
    			draggedTo = sub;
    		else if (palette.contains(SwingUtilities.convertPoint(draggedFrom, e.getPoint(), palette)))
    			draggedTo = palette;
    		
    		if( draggedTo instanceof ColorPicker) {
    			// Drag from palette into the Foreground/Background: set FG/BG color as dragged color
    			if( draggedFrom == palette) {
        			int index = palette.getIndexAt(startX, startY);
        			Color c = paletteManager.getPaletteColor(index);
        			
        			if( c != null)
        				paletteManager.setActiveColor( ((ColorPicker)draggedTo).index, c);
    			}
    		}
    		else if( draggedTo == palette) {
    			// Drag from ColorPicker to palette: copy the color
    			Point converted = SwingUtilities.convertPoint( draggedFrom, e.getPoint(), draggedTo);
    			int index = palette.getIndexAt(converted.x, converted.y);
    			Color c = ((ColorPicker)draggedFrom).getColor();
    			if( index == -1)
    				paletteManager.addPaletteColor(c);
    			else
    				paletteManager.setPaletteColor( index, c);
    		}
    		else {
    			// Drag from X to out of the context
    			if( draggedFrom == palette) {
    				// Drag a Palette color out of context: remove it
        			int index = palette.getIndexAt(startX, startY);
    				if( index != -1)
    					paletteManager.removePaletteColor(index);
    			}
    		}
    	}
    }

    /** Panels for the Foreground and Background colors */
    class ColorPicker extends JPanel {
		private static final long serialVersionUID = 1L;
		private final int index;

        ColorPicker( int index) {
            this.index = index;
            this.setBorder( new EtchedBorder(EtchedBorder.LOWERED));
            this.setPreferredSize( new Dimension( 24,24));
            this.setBackground( paletteManager.getActiveColor(index));
        }
        
        public Color getColor() {
        	return getBackground();
        }
    }

    /** Panel that draws and handles all other Palette colors. */
    class PaletteSubpanel extends JPanel {
		private static final long serialVersionUID = 1L;

		public PaletteSubpanel() {
            this.setOpaque(false);
        }

        public void repaintIndex( int index) {
            this.repaint( getBoundsOfIndex(index));
        }

        public int getIndexAt( int x, int y) {
            int w = Math.max(1, this.getWidth() / SMALL_SIZE);

            if( x > SMALL_SIZE * w) return -1;
            return x/SMALL_SIZE + (y/SMALL_SIZE)*w;
        }
        
        public Rectangle getBoundsOfIndex( int i) {
        	if( i < 0)
        		return null;
        	
        	int w = Math.max(1, this.getWidth() / SMALL_SIZE);
        	int x = SMALL_SIZE * (i % w);
        	int y = SMALL_SIZE * (i / w);
        	
        	return new Rectangle( x, y, SMALL_SIZE, SMALL_SIZE);
        }

        @Override
        public void paintComponent( Graphics g) {
        	super.paintComponent(g);
            Color selected1 = paletteManager.getActiveColor(0);
            Color selected2 = paletteManager.getActiveColor(1);
            
            for( Entry<Integer, Color> entry : paletteManager.getPalette()) {
            	Color c = entry.getValue();
            	Rectangle rect = getBoundsOfIndex( entry.getKey());
            	
            	g.setColor(c);;
            	g.fillRect( rect.x, rect.y, rect.width, rect.height);
            	

                if( c == selected2) {
                    g.setColor( Color.gray);
                	g.drawRect( rect.x, rect.y, rect.width, rect.height);
                }
                if( c == selected1) {
                    g.setColor( Color.black);
                	g.drawRect( rect.x, rect.y, rect.width, rect.height);
                }
            }
        }
    }

	@Override
	public void actionPerformed(ActionEvent evt) {
		if( evt.getSource() == btnAddColor) {
			paletteManager.addPaletteColor( paletteManager.getActiveColor(0));
		}
		else if( evt.getSource() == btnLoadPalette) {
			List<String> palettes = paletteManager.getStoredPaletteNames();
			
			String[][] menuScheme = new String[palettes.size()+1][];
			menuScheme[0] = new String[] {"Load Default Palette", "loaddefault"};
			for( int i=0; i<palettes.size(); ++i) {
				menuScheme[i+1] = new String[2];
				menuScheme[i+1][0] = palettes.get(i);
				menuScheme[i+1][1] = "load." + palettes.get(i);
			}
			
			contextMenu.removeAll();
			UIUtil.constructMenu(contextMenu, menuScheme, this);
			contextMenu.show(this, btnLoadPalette.getX() + 24, btnLoadPalette.getY() - 10);
		}
		else if( evt.getSource() == btnSavePalette) {

			List<String> palettes = paletteManager.getStoredPaletteNames();
			
			String[][] menuScheme = new String[palettes.size()+2][];
			for( int i=0; i<palettes.size(); ++i) {
				menuScheme[i] = new String[2];
				menuScheme[i][0] = palettes.get(i);
				menuScheme[i][1] = "save." + palettes.get(i);
			}
			menuScheme[palettes.size()] = new String[]{"-"};
			menuScheme[palettes.size()+1] = new String[]{"Save as New Palette", "savenew"};
			
			contextMenu.removeAll();
			UIUtil.constructMenu(contextMenu, menuScheme, this);
			contextMenu.show(this, btnLoadPalette.getX() + 24, btnLoadPalette.getY() - 10);
		}
		else if(evt.getActionCommand() != null) {
			String cmd = evt.getActionCommand();
			if( cmd.startsWith("load.")) {
				paletteManager.loadPalette(cmd.substring("load.".length()));
			}
			if( cmd.startsWith("save.")) {
				String name = cmd.substring("save.".length());
				if( JOptionPane.showConfirmDialog(null, "Overwrite " + name +"?") == JOptionPane.YES_OPTION) {
					paletteManager.savePalette(name);
				}
			}
			if( cmd.equals("savenew")) {
				String name = JOptionPane.showInputDialog("Save Palette As:");
				
				if( name == null)
					JOptionPane.showMessageDialog(null, "Cannot save as null name.");
				else {
					List<String> names = paletteManager.getStoredPaletteNames();
					
					if( !names.contains(name)
						|| JOptionPane.showConfirmDialog(null, "Overwrite " + name +"?") == JOptionPane.YES_OPTION) 
						paletteManager.savePalette(name);
				}
			}
			if( cmd.equals("loaddefault")) {
				paletteManager.loadDefaultPalette();
			}
		}
	}
	
	private final JPopupMenu contextMenu = new JPopupMenu();
	
}
