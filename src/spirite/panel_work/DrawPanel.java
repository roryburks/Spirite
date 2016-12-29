//Rory Burks

package spirite.panel_work;


import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;
//import jpen.event.PenListener;

import jpen.owner.multiAwt.AwtPenToolkit;
import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.brains.ToolsetManager.MToolsetObserver;
import spirite.draw_engine.DrawEngine;
import spirite.image_data.ImageWorkspace;

/**
 * DrawPanel is the 
 *
 */
public class DrawPanel extends JPanel
     implements MToolsetObserver
{
	WorkPanel context;
	MasterControl master;
	Penner penner;


	public DrawPanel() {}
	public DrawPanel(WorkPanel context) {
		this.context = context;
		this.master = context.master;
		this.setBackground(new Color(0, 0, 0, 0));

		penner = new Penner( this);

	//	this.addMouseListener(mouser);
//		this.addMouseMotionListener(mouser);
		
		AwtPenToolkit.addPenListener(this, penner);
	}
 

    // :::: Paint
    @Override
    public void paint( Graphics g) {
        super.paint(g);

        if( master == null) return; // Needed so the IDE doesn't freak out

        // Draw Line
        Graphics2D g2 = (Graphics2D) g;

        // Draw Image
        ImageWorkspace workspace = master.getImageManager();
        if( workspace != null) {

        	List<BufferedImage> drawing_queue = workspace.getDrawingQueue();

        	for( BufferedImage img : drawing_queue) {
	            g.drawImage(img, context.itsX(0), context.itsY(0),
	            context.itsX(img.getWidth()), context.itsY(img.getHeight()),
	            0, 0, img.getWidth(), img.getHeight(), null);
        	}

            // Draw Border
            Stroke old_stroke = g2.getStroke();
            Stroke new_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4,2}, 0);
            g2.setStroke(new_stroke);
            g2.setColor(Globals.getColor("drawpanel.image.border"));
            g2.drawRect( context.itsX(0)-1,
            context.itsY(0)-1,
            (int)Math.round(workspace.getWidth()*context.zoom)+1,
            (int)Math.round(workspace.getHeight()*context.zoom)+1);
            g2.setStroke(old_stroke);

            if( context.zoom >= 4) {
                for( int i = 0; i < workspace.getWidth(); ++i) {
                    g2.drawLine(context.itsX(i), 0, context.itsX(i), this.getHeight());
                }
                for( int i = 0; i < workspace.getHeight(); ++i) {
                    g2.drawLine(0, context.itsY(i), this.getWidth(), context.itsY(i));
                }
            }
        }

    }


	@Override
	public void toolsetChanged(String new_tool) {}
 
}
