// Rory Burks

package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
	private static final long serialVersionUID = 1L;
	final static int BIG_SIZE = 20;
    final static int SMALL_SIZE = 12;

    MasterControl master;
    PaletteManager palette_manager;

    ColorPicker main, sub;
    JScrollPane container;
    Palette palette;

    public PalettePanel(){}
    public PalettePanel( MasterControl master) {
        this.master = master;
        palette_manager = master.getPaletteManager();

        initComponents();

        master.getPaletteManager().addPaletteObserver(this);

  //      this.add( new JButton());
    }

    // Set up the GUI
    void initComponents() {
                GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);

        main = new ColorPicker( 0);
        main.addMouseListener(this);

        sub = new ColorPicker( 1);
        sub.addMouseListener(this);

        palette = new Palette();
        palette.addMouseListener(this);

        container = new JScrollPane(palette, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        container.getHorizontalScrollBar().setPreferredSize(new Dimension(5, 0));
        

//        palette.setPreferredSize( new Dimension( 50,999));
//        palette.setBackground(Color.red);

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
        main.setBackground( palette_manager.getActiveColor(0));
        sub.setBackground( palette_manager.getActiveColor(1));
        repaint();
/*        if( index == 0) {
            main.setBackground( master.getColor(index));
            main.repaint();
        }
        if( index == 1) {
            sub.setBackground( master.getColor(index));
            sub.repaint();
            // Needed so that the foreground color appears in front of the background
            main.repaint();
        }
        else {
            palette.repaintIndex(index);
        }*/
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
                palette_manager.setPaletteColor(((ColorPicker)source).index, c);
        }
        if( source == palette) {
            int count = master.getPaletteManager().getPaletteColorCount();
            int index = palette.getIndexAt(e.getX(), e.getY());

            if( index >= 0 && index < count) {
                if( e.getClickCount() == 1) {
                	palette_manager.setActiveColor(dest_color, palette_manager.getPaletteColor(index));
                }
                else if( e.getClickCount() == 2){
                    Color c = Dialogs.pickColor();

                    if( c != null) {
                    	palette_manager.setPaletteColor(index,  c);
                    	palette_manager.setActiveColor(0, c);
                    }
                }
            }
        }
    }
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}


    class ColorPicker extends JPanel {
		private static final long serialVersionUID = 1L;
		int index;

        ColorPicker( int index) {
            this.index = index;
            this.setBorder( new EtchedBorder(EtchedBorder.LOWERED));
            this.setPreferredSize( new Dimension( 24,24));
            this.setBackground( palette_manager.getActiveColor(index));

        }
    }

    class Palette extends JPanel {
		private static final long serialVersionUID = 1L;

		public Palette() {
            this.setOpaque(false);
        }

        public void repaintIndex( int index) {
            // Could be replaced with a smaller repaint area corresponding
            //  only to the index redrawn
            this.repaint();
        }

        public int getIndexAt( int x, int y) {
            int w = Math.max(1, this.getWidth() / SMALL_SIZE);

            if( x > SMALL_SIZE * w) return -1;
            return x/SMALL_SIZE + (y/SMALL_SIZE)*w;
        }

        @Override
        public void paint( Graphics g) {
            Color selected1 = palette_manager.getActiveColor(0);
            Color selected2 = palette_manager.getActiveColor(1);

            int count = palette_manager.getPaletteColorCount();
            int w = Math.max(1, this.getWidth() / SMALL_SIZE);
            int ix = 0;
            int iy = 0;

            for( int i = 0; i < count; ++i) {
                Color c = palette_manager.getPaletteColor(i);
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
