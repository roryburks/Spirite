// Rory Burks

package spirite;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import spirite.MDebug.ErrorType;
import spirite.brains.MasterControl;
import spirite.ui.FrameManager;



/**
 * Entry Point
 * 
 * TODO: Decide best way to have all Observers working on the AWT-EventQueue thread
 * no matter what thread triggers it.  (Copy-paste "Swing.InvokeLater 100 times or
 * implement some sort of class hierarchy).
 * 
 * TODO: Make all Observers weak-referenced.
 */
public class Spirite{
    
    public static MasterControl master;

    public static void main(String[] args) {
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
    	
//    	frame_manager.addFrame( FrameManager.FrameType.LAYER);
    	//frame_manager.addFrame( new LayersPanel( master));
    }
}

