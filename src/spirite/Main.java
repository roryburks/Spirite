// Rory Burks

package spirite;

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

