package spirite.gui.hybrid;

import spirite.hybrid.Globals;

import javax.swing.*;
import java.awt.*;

public class SMenuBar extends JMenuBar {
	public SMenuBar() {
    	setBackground(Globals.getColor("fg"));
    	setForeground(Globals.getColor("textDark"));
    	setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Globals.getColor("bevelBorderDark")));
    	
    	setOpaque(false);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		
		Color highlight = Globals.getColor("fgL");
		Color bg = Globals.getColor("fg");
		
		int w = getWidth();
		int h = getHeight();

        Graphics2D g2 = (Graphics2D)g.create();
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        
        g2.setPaint(new GradientPaint(
                new Point(0, h/3-h/5), 
                bg, 
                new Point(0, h/3), 
                highlight));
        g2.fillRect(0, h/5, getWidth(), h/5);
        g2.setPaint(new GradientPaint(
                new Point(0, h/3), 
                highlight, 
                new Point(0, h/3+h/3), 
                bg));
        g2.fillRect(0, h/3, getWidth(), h/3);
        g2.dispose();
		
		super.paintComponent(g);
	}
}
