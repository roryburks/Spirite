// Rory Burks

package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;

import spirite.brains.MasterControl;
import spirite.brains.PaletteManager;
import spirite.brains.PaletteManager.MPaletteObserver;
import spirite.dialogs.Dialogs;

public class PalettePanel extends JPanel 
        implements MouseListener, MPaletteObserver
{
	// PalettePanel only needs access to PaletteManager
    private final PaletteManager paletteManager;
    
	private static final long serialVersionUID = 1L;
	private final static int BIG_SIZE = 20;
    private final static int SMALL_SIZE = 12;

    private ColorPicker main, sub;
    private JScrollPane container;
    private PaletteSubpanel palette;

    public PalettePanel( MasterControl master) {
        paletteManager = master.getPaletteManager();

        initComponents();

        paletteManager.addPaletteObserver(this);
    }

    // Set up the GUI
    void initComponents() {
                GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        main = new ColorPicker( 0);
        main.addMouseListener(this);

        sub = new ColorPicker( 1);
        sub.addMouseListener(this);

        palette = new PaletteSubpanel();
        palette.addMouseListener(this);

        container = new JScrollPane(palette, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        container.getHorizontalScrollBar().setPreferredSize(new Dimension(5, 0));
        

        layout.setHorizontalGroup(layout.createParallelGroup( GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(10)
                        .addComponent(main, BIG_SIZE,BIG_SIZE,BIG_SIZE)
                )
                .addGroup(layout.createSequentialGroup()
                        .addGap(20)
                        .addComponent(sub, BIG_SIZE,BIG_SIZE,BIG_SIZE)
                )
                .addGroup(layout.createSequentialGroup()
                        .addGap(20 + BIG_SIZE + 5)
                        .addComponent(container)
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
                .addComponent(container)
        );
    }

    // :: MPaletteObserver
    @Override
    public void colorChanged() {
        main.setBackground( paletteManager.getActiveColor(0));
        sub.setBackground( paletteManager.getActiveColor(1));
        repaint();
    }

    // :: MouseEventListener
    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override public void mousePressed(MouseEvent e) {
        Object source = e.getSource();


        int dest_color = (e.getButton() == MouseEvent.BUTTON1) ? 0 : 1;

        if( source == main || source == sub) {
            Color c = Dialogs.pickColor();

            if( c != null)
                paletteManager.setPaletteColor(((ColorPicker)source).index, c);
        }
        if( source == palette) {
            int count = paletteManager.getPaletteColorCount();
            int index = palette.getIndexAt(e.getX(), e.getY());

            if( index >= 0 && index < count) {
                if( e.getClickCount() == 1) {
                	paletteManager.setActiveColor(dest_color, paletteManager.getPaletteColor(index));
                }
                else if( e.getClickCount() == 2){
                    Color c = Dialogs.pickColor();

                    if( c != null) {
                    	paletteManager.setPaletteColor(index,  c);
                    	paletteManager.setActiveColor(0, c);
                    }
                }
            }
        }
    }
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

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
        	if( i < 0 || i > paletteManager.getPaletteColorCount())
        		return null;
        	
        	int w = Math.max(1, this.getWidth() / SMALL_SIZE);
        	int x = SMALL_SIZE * (i % w);
        	int y = SMALL_SIZE * (i / w);
        	
        	return new Rectangle( x, y, SMALL_SIZE, SMALL_SIZE);
        }

        @Override
        public void paintComponent( Graphics g) {
            Color selected1 = paletteManager.getActiveColor(0);
            Color selected2 = paletteManager.getActiveColor(1);

            int count = paletteManager.getPaletteColorCount();
            int w = Math.max(1, this.getWidth() / SMALL_SIZE);
            int ix = 0;
            int iy = 0;

            for( int i = 0; i < count; ++i) {
                Color c = paletteManager.getPaletteColor(i);
                g.setColor(c);
                g.fillRect(ix*SMALL_SIZE + 1, iy*SMALL_SIZE + 1, SMALL_SIZE-1, SMALL_SIZE-1);

                if( c == selected2) {
                    g.setColor( Color.gray);
                    g.drawRect(ix*SMALL_SIZE, iy*SMALL_SIZE, SMALL_SIZE, SMALL_SIZE);
                }
                if( c == selected1) {
                    g.setColor( Color.black);
                    g.drawRect(ix*SMALL_SIZE, iy*SMALL_SIZE, SMALL_SIZE, SMALL_SIZE);
                }

                ix += 1;
                if( ix >= w) {
                    ix = 0;
                    iy += 1;
                }
            }
        }
    }
}
