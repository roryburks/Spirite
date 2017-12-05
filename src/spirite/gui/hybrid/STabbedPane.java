package spirite.gui.hybrid;

import spirite.hybrid.Globals;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class STabbedPane extends JTabbedPane {
	public STabbedPane() {
		this.setUI(new STPUI());
		
		this.setBorder(null);
	}
	
	private class STPUI extends BasicTabbedPaneUI {
		@Override
		protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect,
				Rectangle textRect) 
		{
			String title = getTitleAt(tabIndex);
			Graphics2D g2 = (Graphics2D)g.create();
			
			g2.setColor(Globals.getColor(getSelectedIndex() == tabIndex ? "tabbedPane.SelectedBG":"tabbedPane.UnselectedBG"));
			
			int x1 = rects[tabIndex].x;
			int y1 = rects[tabIndex].y;
			int x2 = rects[tabIndex].x + rects[tabIndex].width;
			int y2 = rects[tabIndex].y + rects[tabIndex].height;
			int[] x = new int[] {
				x1, x1, x1+5, x2-5, x2
			};
			int[] y = new int[] {
				y2, y1+5, y1, y1, y2
			};
			g2.fillPolygon( x ,y, 5);
			

			g2.setColor(Globals.getColor("tabbedPane.TabBorder"));
			g2.drawPolyline(x ,y, 5);
			
			g2.setColor(Globals.getColor("tabbedPane.TabText"));
			g2.drawString(title, x1 + 5, y2 - 5);
			
			g2.dispose();
		}
		
		@Override
		protected Insets getContentBorderInsets(int tabPlacement) {
			return new Insets(2, 2,4,4);
		}
		
		@Override
		protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y,
				int w, int h) 
		{
			g.setColor(Globals.getColor("bevelBorderMed"));
			g.fillRect(x, y, w, 2);
			g.fillRect(x, y, 2, h);
	
			g.setColor(Globals.getColor("bevelBorderDark"));
			g.fillRect(x+w-4, y, 4, h);
			g.fillRect(x, y+h-4, w, 4);
			

			g.setColor(Globals.getColor("bevelBorderLight"));
			g.drawLine(x+1, y+1, x+w-2, y+1);
			g.drawLine(x+1, y+1, x+1, y+h-2);
			g.drawLine(x+1, y+h-2, x+w-2, y+h-2);
			g.drawLine(x+w-2, y+1, x+w-2, y+h-2);

			g.setColor(Globals.getColor("bevelBorderDarker"));
			g.drawLine(x, y+h-1, x+w-1, y+h-1);
			g.drawLine(x+w-1, y, x+w-1, y+h-1);
		}
		protected void paintContentBorderLeftEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {};
		protected void paintContentBorderRightEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {}
		protected void paintContentBorderTopEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {};
	}
}
