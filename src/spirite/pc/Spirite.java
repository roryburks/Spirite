// Rory Burks

package spirite.pc;

import spirite.base.brains.MasterControl;
import spirite.hybrid.MDebug;
import spirite.hybrid.MDebug.ErrorType;
import spirite.pc.ui.omni.FrameManager;

import javax.swing.*;



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

            // So that it's run on the UI thread.
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
    		//UIManager.getLookAndFeelDefaults().put( "background" , new ColorUIResource(new Color(152, 166, 173) ));
    		//UIManager.getLookAndFeelDefaults().put( "Panel.background" , new ColorUIResource(new Color(152, 166, 173) ));
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

