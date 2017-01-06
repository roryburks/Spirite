package spirite.panel_anim;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import spirite.draw_engine.RenderEngine.RenderSettings;
import spirite.brains.MasterControl;

public class PreviewPanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	MasterControl master;

    public PreviewPanel() {}
    public PreviewPanel(MasterControl master) {
        this.master = master;
    }


    @Override
    public void paint(Graphics g) {
        if( master == null) {
            g.drawArc(0, 0, 64, 64, 15, 300);
            return;
        }

        RenderSettings settings = new RenderSettings();
        settings.workspace = master.getCurrentWorkspace();
        
        BufferedImage image = master.getRenderEngine().renderImage(settings);
        g.drawImage(image, 0, 0, null);
    }

}
