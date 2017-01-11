// Rory Burks

package spirite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import spirite.MDebug.ErrorType;
import spirite.brains.MasterControl;
import spirite.ui.FrameManager;

/**
 * Entry Point
 */
public class Main{
    
    public static MasterControl master;

    public static void main(String[] args) {
    	LinkedList<Integer> sorted = new LinkedList<>();
    	
    	ListIterator<Integer> it = sorted.listIterator();
    	int intArr[] = new int[10];
    	int i = 0;
    	
    	while( it.hasNext()) {
    		if( intArr[i] > it.next()) {
    			it.previous();
    			it.add(i);
    			it.next();
    		}
    	}

        HashMap<Integer,Boolean> map = new HashMap<>();
        
        
        try {
            master = new MasterControl();

            setLookAndFeel();

            SwingUtilities.invokeLater( new Runnable() {
                public void run() { createUI();}
            });
        } catch( Exception e) {
        	MDebug.handleError( ErrorType.FATAL, e, "Core:" + e.getMessage());
        }
    }

    public static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName());
        }
        catch (Exception e) {
        	MDebug.handleError( ErrorType.RESOURCE, e, "Invalid Look and Feel.");
        }
    }

    public static void createUI() {
    	FrameManager frame_manager = master.getFrameManager();
    	
    	frame_manager.packMainFrame();
    	
    	frame_manager.addFrame( FrameManager.FrameType.LAYER);
    	//frame_manager.addFrame( new LayersPanel( master));
    }
}

