package spirite.panel_work.awt;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.GroupLayout;

import spirite.Globals;
import spirite.brains.MasterControl;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.panel_work.WorkArea;
import spirite.panel_work.WorkPanel;
import spirite.panel_work.WorkPanel.View;
import spirite.ui.UIUtil;

/**
 *WorkSplicePanel is a simple container for the DrawPanel (which displays the 
 *	working images and controls input) and the ReferencePanel (which displays 
 *	reference images) so that they both appear on top of each other.
 *
 * @author Rory Burks
 */
public class WorkSplicePanel extends javax.swing.JPanel 
	implements MReferenceObserver, MouseWheelListener, WorkArea
{
	private static final long serialVersionUID = 1L;
	final WorkPanel context;
	View zoomer;
    int offsetx, offsety;
    
    ImageWorkspace workspace = null;

    public final DrawPanel drawPanel;
    private final ReferencePanel previewPanel;
    private final ReferencePanel previewPanelBack;
    
    Color normalBG = Globals.getColor("workArea.normalBG");
    Color referenceBG = Globals.getColor("workArea.referenceBG");
    /**
     * Creates new form WorkSplicePanel
     */
    public WorkSplicePanel( WorkPanel context, MasterControl master) {
    	this.context = context;
        this.zoomer = context.getCurrentView();
        previewPanel = new ReferencePanel(context, master, true);
        drawPanel = new DrawPanel( context, master);
        previewPanelBack = new ReferencePanel(context, master, false);
        initComponents();
        
        
        previewPanelBack.addMouseWheelListener(this);
        drawPanel.addMouseWheelListener(this);
        

        offsetx = 0;
        offsety = 0;
    }
 
    private void initComponents() {


        setPreferredSize(new java.awt.Dimension(420, 322));

        GroupLayout layout = new javax.swing.GroupLayout(this);

        layout.setHorizontalGroup( layout.createParallelGroup()
        		.addComponent(previewPanel)
        		.addComponent(drawPanel)
        		.addComponent(previewPanelBack));
        layout.setVerticalGroup( layout.createParallelGroup()
        		.addComponent(previewPanel)
        		.addComponent(drawPanel)
        		.addComponent(previewPanelBack));
        this.setLayout(layout);
    }                      
    

    @Override
    protected void paintComponent(Graphics g) {
    	super.paintComponent(g);
        // Draw Transparency BG
    	if( zoomer != null) {
	        int dx = (zoomer.itsX(0) >= 0)?0:-zoomer.itsX(0);
	        int dy = (zoomer.itsY(0) >= 0)?0:-zoomer.itsY(0);
	        UIUtil.drawTransparencyBG(g, new Rectangle( 
	        		Math.max(0, zoomer.itsX(0)),
	        		Math.max(0, zoomer.itsY(0)),
	        		Math.min(getWidth(), (int)Math.round(workspace.getWidth()*zoomer.getZoom())-2-dx),
	        		Math.min(getHeight(), (int)Math.round(workspace.getHeight()*zoomer.getZoom())-2-dy)),
	        		8);
    	}
    }

              
    // :::: MReferenceObserver
	@Override	public void referenceStructureChanged(boolean hard) {}
	@Override
	public void toggleReference(boolean referenceMode) {
		setBackground( referenceMode ? referenceBG : normalBG);
		
	}

	// :::: MouseWheelListener
	@Override
	public void mouseWheelMoved(MouseWheelEvent evt) {
		if( evt.getWheelRotation() < 0)
			zoomer.zoomIn();
		else if( evt.getWheelRotation() > 0)
			zoomer.zoomOut();
	}

	
	// :::: WorkArea
	@Override
	public void changeWorkspace(ImageWorkspace ws, View view) {
		if( workspace != null)
			workspace.getReferenceManager().removeReferenceObserve(this);
		workspace = ws;
		if( workspace != null)
			workspace.getReferenceManager().addReferenceObserve(this);

        this.zoomer = view;
		drawPanel.changeWorkspace( ws, view);
		previewPanel.changeWorkspace( ws, view);
		previewPanelBack.changeWorkspace(ws, view);
	}

	@Override
	public Component getComponent() {
		return this;
	}        

}
