package jwin.test;

import java.util.ArrayList;

import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Font;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.geom.Line2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;

import jwinpointer.JWinPointerReader;
import jwinpointer.JWinPointerReader.PointerEventListener;

class PointerInfo {
    public int deviceType, pointerID;
    public boolean inverted;
    public int x, y, pressure;
}

// Multiple fingers can touch the screen simultaneously, so we use an array to store information about the fingers.
// We may as well also store stylus and mouse information in the same array.
// That's what this class is for.
class PointerInfoArray {
    public ArrayList< PointerInfo > array = new ArrayList< PointerInfo >();

    // returns -1 if none found
    private int getIndex( int deviceType, int pointerID ) {
        for ( int i = 0; i < array.size(); ++i ) {
            if ( array.get(i).deviceType == deviceType && array.get(i).pointerID == pointerID )
                return i;
        }
        return -1;
    }

    // Finds and updates existing pointer info, or creates a pointer info instance if no existing one is found.
    public void updatePointer( int deviceType, int pointerID, boolean inverted, int x, int y, int pressure ) {
        int i = getIndex( deviceType, pointerID );
        PointerInfo pi;
        if ( i == -1 ) {
            pi = new PointerInfo();
            pi.deviceType = deviceType;
            pi.pointerID = pointerID;
            array.add(pi);
        }
        else pi = array.get(i);

        pi.inverted = inverted;
        pi.x = x;
        pi.y = y;
        pi.pressure = pressure;
    }

    public void removePointer( int deviceType, int pointerID ) {
        int i = getIndex( deviceType, pointerID );
        if ( i != -1 ) {
            array.remove(i);
        }
    }
}

class EventLogItem {
    public String description;
    public int count;

    public EventLogItem( String d ) {
        description = d;
        count = 1;
    }
}

// Used to store an array of debug strings, with automatic collapsing of duplicates.
class EventLogger {
    private static final int MAX_NUM_ITEMS = 25;

    ArrayList< EventLogItem > items = new ArrayList< EventLogItem >();

    public void log( String message ) {
        int N = items.size();
        if ( N>0 && message.equals( items.get(N-1).description ) ) {
            items.get(N-1).count ++;
        }
        else {
            items.add( new EventLogItem( message ) );
            while ( items.size() > MAX_NUM_ITEMS )
                items.remove(0);
        }
    }

    public void draw( Graphics2D g2, int fontHeight, int viewportHeight ) {
        for ( int row = 0; row < items.size(); ++row ) {
            String d = items.get(row).description;
            int c = items.get(row).count;
            if ( c > 1 )
                d = d + "(x"+c+")";
            g2.drawString( d, 10, viewportHeight-10-(items.size()-1-row)*fontHeight /*for debugging*/ - fontHeight);
        }
    }
}


class MyCanvas extends JPanel implements PointerEventListener, MouseListener, MouseMotionListener {

    private Line2D line2D = new Line2D.Float();
    private Ellipse2D.Float ellipse2D = new Ellipse2D.Float();
    private int fontHeight = 20;
    private Font font = new Font( "Sans-serif", Font.BOLD, fontHeight );

    private Component rootComponent = null; // used to convert between coordinate systems
    private PointerInfoArray pointerInfoArray = new PointerInfoArray();
    private EventLogger logger = new EventLogger();

    public MyCanvas(
            // The coordinates we receive for pointer events are with respect to this component.
            // We use this component to convert to the coordinate system of the canvas.
            Component root
    ) {
        rootComponent = root;
        setBorder( BorderFactory.createLineBorder( Color.black ) );
        setBackground( Color.white );

        // These mouse event handlers are not necessary for retrieving pointer events.
        // They are only here for debugging, to allow for comparison of the mouse coordinates
        // and pointer coordinates.
        final boolean useMouseEventHandlers = false;
        if ( useMouseEventHandlers ) {
            addMouseListener( this );
            addMouseMotionListener( this );
        }
    }
    public Dimension getPreferredSize() {
        return new Dimension( 512, 512 );
    }
    public void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Graphics2D g2 = (Graphics2D)g;
        g2.setFont(font);

        g2.setColor( Color.GRAY );
        logger.draw(g2, fontHeight, getHeight());

        for ( int i = 0; i < pointerInfoArray.array.size(); ++i ) {
            int d = fontHeight;
            PointerInfo pi = pointerInfoArray.array.get(i);

            if ( pi.x < 0 || pi.x >= getWidth() || pi.y < 0 || pi.y >= getHeight() ) {
                // The pointer is outside the bounds of the canvas
                g2.setColor( Color.RED );
            }
            else g2.setColor( Color.BLUE );

            ellipse2D.setFrame( pi.x-d, pi.y-d, 2*d, 2*d );
            g2.draw( ellipse2D );

            line2D.setLine( pi.x, pi.y, pi.x+d, pi.y+2*d );
            g2.draw( line2D );

            String s1 = "dev,id,inv = " + pi.deviceType + "," + pi.pointerID + "," + pi.inverted;
            String s2 = "x,y,pressure = " + pi.x + "," + pi.y + "," + pi.pressure;
            g2.drawString(s1,pi.x+d,pi.y+2*d);
            g2.drawString(s2,pi.x+d,pi.y+3*d);
        }
    }

    // These mouse event handlers are not necessary for retrieving pointer events.
    // They are only here for debugging, to allow for comparison of the mouse coordinates
    // and pointer coordinates.
    public void mouseClicked( MouseEvent e ) { }
    public void mouseEntered( MouseEvent e ) { }
    public void mouseExited( MouseEvent e ) { }
    public void mousePressed( MouseEvent e ) { }
    public void mouseReleased( MouseEvent e ) { }
    public void mouseMoved( MouseEvent e ) { System.out.println("Mouse coordinates  : "+e.getX()+","+e.getY()); }
    public void mouseDragged( MouseEvent e ) { }


    private static final int EVENT_TYPE_DRAG = 1;
    private static final int EVENT_TYPE_HOVER = 2;
    private static final int EVENT_TYPE_DOWN = 3;
    private static final int EVENT_TYPE_UP = 4;
    private static final int EVENT_TYPE_BUTTON_DOWN = 5;
    private static final int EVENT_TYPE_BUTTON_UP = 6;
    private static final int EVENT_TYPE_IN_RANGE = 7;
    private static final int EVENT_TYPE_OUT_OF_RANGE = 8;

    public void pointerXYEvent(int deviceType, int pointerID, int eventType, boolean inverted, int x, int y, int pressure) {
        //System.out.println("Pointer coordinates before conversion: "+x+","+y);
        Point p = SwingUtilities.convertPoint(rootComponent, x, y, this);
        x = p.x;
        y = p.y;
        System.out.println("Pointer coordinates: "+x+","+y);
        pointerInfoArray.updatePointer( deviceType, pointerID, inverted, x, y, pressure );
        logger.log(generateDescription(deviceType,pointerID,eventType));
        repaint();
    }

    public void pointerButtonEvent(int deviceType, int pointerID, int eventType, boolean inverted, int buttonIndex) {
        logger.log(generateDescription(deviceType,pointerID,eventType));
        repaint();
    }
    public void pointerEvent(int deviceType, int pointerID, int eventType, boolean inverted) {
        logger.log(generateDescription(deviceType,pointerID,eventType));
        repaint();
    }

    private String generateDescription( int deviceType, int pointerID, int eventType ) {
        String s = "";
        switch ( eventType ) {
            case EVENT_TYPE_DRAG :
                s = "drag";
                break;
            case EVENT_TYPE_HOVER :
                s = "hover";
                break;
            case EVENT_TYPE_DOWN :
                s = "down";
                break;
            case EVENT_TYPE_UP :
                s = "up";
                break;
            case EVENT_TYPE_BUTTON_DOWN :
                s = "button down";
                break;
            case EVENT_TYPE_BUTTON_UP :
                s = "button up";
                break;
            case EVENT_TYPE_IN_RANGE :
                s = "in range";
                break;
            case EVENT_TYPE_OUT_OF_RANGE :
                s = "out of range";
                break;
            default:
                s = "?";
                break;
        }
        s = "dev,id = " + deviceType + "," + pointerID + "; " + s;
        return s;
    }
}

public class MyPointerTestApp implements ActionListener {

    private static JWinPointerReader jWinPointerReader;
    private static String applicationName = "Pointer Test App";
    JFrame frame;
    Container toolPanel;
    MyCanvas canvas;

    JMenuItem quitMenuItem, aboutMenuItem;
    JCheckBoxMenuItem toolsMenuItem;

    JButton button1, button2;

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if ( source == quitMenuItem ) {
            int response = JOptionPane.showConfirmDialog(
                    frame,
                    "Really quit?",
                    "Confirm Quit",
                    JOptionPane.YES_NO_OPTION
            );

            if (response == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        }
        else if ( source == toolsMenuItem ) {
            Container pane = frame.getContentPane();
            if ( toolsMenuItem.isSelected() ) {
                pane.removeAll();
                pane.add( toolPanel );
                pane.add( canvas );
            }
            else {
                pane.removeAll();
                pane.add( canvas );
            }
            frame.invalidate();
            frame.validate();
        }
        else if ( source == aboutMenuItem ) {
            JOptionPane.showMessageDialog(
                    frame,
                    "'" + applicationName + "' sample program\n"
                            + "written Dec 2015\n",
                    "About",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
        else if ( source == button1 ) {
            System.out.println("button1");
        }
        else if ( source == button2 ) {
            System.out.println("button2");
        }
    }

    // For thread safety, this should be invoked
    // from the event-dispatching thread.
    //
    private void createUserInterface() {
        if ( ! SwingUtilities.isEventDispatchThread() ) {
            System.out.println(
                    "Warning: UI is not being created in the Event Dispatch Thread!");
            assert false;
        }

        frame = new JFrame( applicationName );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        quitMenuItem = new JMenuItem("Quit");
        quitMenuItem.addActionListener(this);
        menu.add(quitMenuItem);
        menuBar.add(menu);
        menu = new JMenu("View");
        toolsMenuItem = new JCheckBoxMenuItem("Show Tools");
        toolsMenuItem.setSelected( true );
        toolsMenuItem.addActionListener(this);
        menu.add(toolsMenuItem);
        menuBar.add(menu);
        menu = new JMenu("Help");
        aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.addActionListener(this);
        menu.add(aboutMenuItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        toolPanel = new JPanel();
        toolPanel.setLayout( new BoxLayout( toolPanel, BoxLayout.Y_AXIS ) );

        canvas = new MyCanvas(
                // For some reason, passing in the frame here doesn't work
                menuBar
        );

        Container pane = frame.getContentPane();
        pane.setLayout( new BoxLayout( pane, BoxLayout.X_AXIS ) );
        pane.add( toolPanel );
        pane.add( canvas );

        button1 = new JButton( "Test Button 1" );
        button1.setAlignmentX( Component.LEFT_ALIGNMENT );
        button1.addActionListener(this);
        toolPanel.add( button1 );

        button2 = new JButton( "Test Button 2" );
        button2.setAlignmentX( Component.LEFT_ALIGNMENT );
        button2.addActionListener(this);
        toolPanel.add( button2 );

        frame.pack();
        frame.setVisible( true );

        jWinPointerReader = new JWinPointerReader(frame);
        jWinPointerReader.addPointerEventListener(canvas);
    }

    public static void main( String[] args ) {
        // Schedule the creation of the UI for the event-dispatching thread.
        javax.swing.SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        MyPointerTestApp pointerTest = new MyPointerTestApp();
                        pointerTest.createUserInterface();
                    }
                }
        );
    }
}