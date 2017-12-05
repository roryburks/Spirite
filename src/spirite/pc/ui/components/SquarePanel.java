package spirite.pc.ui.components;

import spirite.gui.hybrid.SPanel;

import java.awt.*;



public class SquarePanel extends SPanel {
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Container c = getParent();
        if (c != null) {
            d = c.getSize();
        } else {
            return new Dimension(10, 10);
        }
        int w = (int) d.getWidth();
        int h = (int) d.getHeight();
        int s = (w < h ? w : h);
        return new Dimension(s, s);
    }
}