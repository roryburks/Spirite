package spirite.panel_work;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.GroupLayout;

import spirite.brains.MasterControl;
import spirite.image_data.ImageWorkspace.MReferenceObserver;
import spirite.panel_work.WorkPanel.Zoomer;
import spirite.ui.UIUtil;

/**
 *WorkSplicePanel is a simple container for the DrawPanel (which displays the 
 *	working images and controls input) and the ReferencePanel (which displays 
 *	reference images) so that they both appear on top of each other.
 *
 * @author Rory Burks
 */
public class WorkSplicePanel extends javax.swing.JPanel implements MReferenceObserver 
{
	private static final long serialVersionUID = 1L;
	final WorkPanel context;
	final Zoomer zoomer;
    int offsetx, offsety;

    Color normalBG = new Color(238,238,238);
    Color referenceBG = new Color( 210,210,242);
    /**
     * Creates new form WorkSplicePanel
     */
    public WorkSplicePanel( WorkPanel context, MasterControl master) {
    	this.context = context;
        this.zoomer = context.zoomer;
        drawPanel = new DrawPanel( context, master);
        previewPanel = new ReferencePanel(context, master);
        initComponents();
        
        context.workspace.addReferenceObserve(this);
        

        offsetx = 0;
        offsety = 0;
    }
 
    private void initComponents() {


        setPreferredSize(new java.awt.Dimension(420, 322));

        GroupLayout layout = new javax.swing.GroupLayout(this);

        layout.setHorizontalGroup( layout.createParallelGroup()
        		.addComponent(drawPanel)
        		.addComponent(previewPanel));
        layout.setVerticalGroup( layout.createParallelGroup()
        		.addComponent(drawPanel)
        		.addComponent(previewPanel));
        this.setLayout(layout);
    }                      
    

    @Override
    protected void paintComponent(Graphics g) {
    	super.paintComponent(g);
        // Draw Transparency BG
        int dx = (zoomer.itsX(0) >= 0)?0:-zoomer.itsX(0);
        int dy = (zoomer.itsY(0) >= 0)?0:-zoomer.itsY(0);
        UIUtil.drawTransparencyBG(g, new Rectangle( 
        		Math.max(0, zoomer.itsX(0)),
        		Math.max(0, zoomer.itsY(0)),
        		Math.min(getWidth(), (int)Math.round(context.workspace.getWidth()*zoomer.getZoom())-2-dx),
        		Math.min(getHeight(), (int)Math.round(context.workspace.getHeight()*zoomer.getZoom())-2-dy)),
        		8);
    }

              
    DrawPanel drawPanel;
    ReferencePanel previewPanel;

	@Override	public void referenceStructureChanged() {}
	@Override
	public void toggleReference(boolean referenceMode) {
		setBackground( referenceMode ? referenceBG : normalBG);
		
	}        

}
