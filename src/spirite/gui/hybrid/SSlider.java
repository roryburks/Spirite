package spirite.gui.hybrid;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JSlider;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

import spirite.hybrid.Globals;

public class SSlider extends JSlider {
	public SSlider() {
		this.setBackground(Globals.getColor("bg"));
		this.setForeground(Globals.getColor("fg"));
		
		this.setUI(new UI(this));
	}
	
	private class UI extends BasicSliderUI {

		private UI(JSlider b) {
			super(b);
		}
		
		@Override
		public void paintTrack(Graphics g) {
			int yc = trackRect.y + trackRect.height/2;
			
			int x1 = trackRect.x;
			int x2 = trackRect.x + trackRect.width;
			int y1 = yc-2;
			int y2 = yc+2;

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setColor(Globals.getColor("bevelBorderMed"));
			g2.fillRect(x1, y1, x2-x1-1, y2-y1-1);

			g2.setColor(Globals.getColor("bevelBorderLight"));
			g2.drawLine(x1, y1, x2, y1);
			g2.drawLine(x1, y1, x1, y2);
			
			g2.setColor(Globals.getColor("bevelBorderDarker"));
			g2.drawLine(x1+1, y1+1, x2-1, y1+1);
			g2.drawLine(x1+1, y1+1, x1+1, y2-1);

			g2.setColor(Globals.getColor("bevelBorderDark"));
			g2.drawLine(x1, y2, x2, y2);
			g2.drawLine(x2, y1, x2, y2);
			
			g2.dispose();
		}
		
		@Override
		public void paintThumb(Graphics g) {
			int cx = thumbRect.x + thumbRect.width/2;

			Graphics2D g2 = (Graphics2D) g.create();
			
			
			Rectangle r;
			if( getHeight()-1 < thumbRect.height) 
				r =new Rectangle(thumbRect.x, 0, thumbRect.width, getHeight()-1);
			else
				r = thumbRect;
			
			int w = 2;
			int x1l = r.x;
			int x2l = r.x + r.width/2-2;
			int x1r = r.x + r.width - (r.width/2-2)-1;
			int x2r = r.x + r.width - 1;
			int y1 = r.y;
			int y2 = r.y + r.height;

			int leftx[] = {
				x1l, x2l, x2l, x1l+w, x1l+w, x2l, x2l, x1l
			};
			int lefty[] = {
				y1, y1, y1+w, y1+w, y2-w, y2-w, y2, y2
			};
			int rightx[] = {
				x1r, x2r, x2r, x1r, x1r, x2r-w, x2r-w, x1r
			};
			int righty[] = {
				y1, y1, y2, y2, y2-w, y2-w, y1+w, y1+w, y1
			};
			
			// Main Part
			Color fgD = Globals.getColor("bevelBorderMed");
			Color fgL = Globals.getColor("fgL");
			Color fg = Globals.getColor("fg");
			g2.setColor(fg);
			g2.fillPolygon(leftx, lefty, leftx.length);
			g2.fillPolygon(rightx, righty, rightx.length);
			
			// Highlights
			g2.setColor(fgL);
			g2.drawLine(x1l, y1, x2l, y1);
			g2.drawLine(x1l, y1, x1l, y2);
			g2.drawLine(x1r, y1, x1r, y1+w);
			g2.drawLine(x1r, y1, x2r, y1);
			g2.drawLine(x1r, y2, x2r, y2);
			g2.drawLine(x1r, y2, x1r, y2-w);

			// Shadows
			g2.setColor(fgD);
			g2.drawLine(x2l, y1, x2l, y1+w);
			g2.drawLine(x1l+w, y1+w, x2l, y1+w);
			g2.drawLine(x1l, y2, x2l, y2);
			g2.drawLine(x2l, y2, x2l, y2-w);
			g2.drawLine(x1r, y2, x2r, y2);
			g2.drawLine(x2r, y1, x2r, y2);

			// Center Line
			g2.setColor(Color.RED);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g2.drawLine(cx, thumbRect.y, cx, thumbRect.y + thumbRect.height);
			g2.dispose();
			
			//super.paintThumb(g);
		}
		
	}
}
