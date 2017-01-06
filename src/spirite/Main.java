// Rory Burks

package spirite;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import spirite.brains.MasterControl;
import spirite.ui.FrameManager;

/**
 * Entry Point
 */
public class Main{
    
    public static MasterControl master;

    public static void main(String[] args) {
        try {
        	LinkedList<Integer> pll = new LinkedList<>();
        	pll.add( new Integer(1));
        	pll.add( new Integer(2));
        	pll.add( new Integer(3));
        	pll.add( new Integer(4));
        	pll.add( new Integer(5));
        	pll.add( new Integer(6));
        	pll.add( new Integer(7));
        	pll.add( new Integer(8));
        	pll.add( new Integer(9));
        	
        	Iterator<Integer> it = pll.descendingIterator();
        	int i = it.next();
        	
        	Iterator<Integer> it2 = pll.descendingIterator();
        	while( it2.hasNext()) {
        		int i2 = it2.next();
        		if( i == i2) 
        			break;
        		it2.remove();
        	}
        
        	for( Integer i3 : pll) {
        		System.out.print(i3);
        	}
        	
            master = new MasterControl();

            setLookAndFeel();

            SwingUtilities.invokeLater( new Runnable() {
                public void run() { createUI();}
            });
        } catch( Exception e) {
            MDebug.handleError(0, e);
        }
    }

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName());
        }
        catch (Exception e) {
            MDebug.handleError( 5, "Unable to set Look and Feel", e);
        }
    }

    public static void createUI() {
    	FrameManager frame_manager = master.getFrameManager();
    	
    	frame_manager.packMainFrame();
    	
    	frame_manager.addFrame( FrameManager.FrameType.LAYER);
    	//frame_manager.addFrame( new LayersPanel( master));
    }
}

