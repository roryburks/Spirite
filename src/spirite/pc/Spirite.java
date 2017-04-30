// Rory Burks

package spirite.pc;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import spirite.base.brains.MasterControl;
import spirite.pc.MDebug.ErrorType;
import spirite.pc.ui.FrameManager;



/**
 * Entry Point
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
    }
}

