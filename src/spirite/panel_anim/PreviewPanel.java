package spirite.panel_anim;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import spirite.brains.MasterControl.MImageObserver;
import spirite.brains.MasterControl;

public class PreviewPanel extends JPanel
{
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

        BufferedImage image = master.getImageManager().getActivePart().getData();
        g.drawImage(image, 0, 0, null);
    }

}
