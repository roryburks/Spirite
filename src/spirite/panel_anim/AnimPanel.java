package spirite.panel_anim;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MCurrentImageObserver;



public class AnimPanel extends javax.swing.JPanel 
        implements MCurrentImageObserver
{
	private static final long serialVersionUID = 1L;
	MasterControl master;

    /**
     * Creates new form PreviewPanel
     */
    public AnimPanel() { initComponents(); }
    public AnimPanel( MasterControl master) {
        this.master = master;
        
        master.addCurrentImageObserver(this);
    }

    @Override
    public void imageRefresh() {
    	this.repaint();
    }
	@Override
	public void imageStructureRefresh() {
		this.repaint();
	}

                          
    private void initComponents() {

        previewPanel = new PreviewPanel( master);

        javax.swing.GroupLayout previewPanel1Layout = new javax.swing.GroupLayout(previewPanel);
        previewPanel.setLayout(previewPanel1Layout);
        previewPanel1Layout.setHorizontalGroup(
            previewPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        previewPanel1Layout.setVerticalGroup(
            previewPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(previewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(405, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(previewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
    }               

             
    private PreviewPanel previewPanel;

}
