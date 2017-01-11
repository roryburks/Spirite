package spirite.panel_anim;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MCurrentImageObserver;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;

import java.awt.Dimension;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JTextField;
import javax.swing.JLabel;



public class AnimPanel extends javax.swing.JPanel 
        implements MCurrentImageObserver
{
	private static final long serialVersionUID = 1L;
	MasterControl master;
	
	

    /**
     * Creates new form PreviewPanel
     */
//    public AnimPanel() { initComponents(); }
    public AnimPanel( MasterControl master) {
        this.master = master;
        
        initComponents();
        
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
	

    private PreviewPanel previewPanel;
    private JTextField textField;
    private JButton buttonBack;
    private JButton buttonPlay;
    private JButton buttonForward;
    private JSlider slider;
                          
    private void initComponents() {

        previewPanel = new PreviewPanel( master);
        
        buttonBack = new JButton();
        buttonPlay = new JButton();
        buttonForward = new JButton();
        slider = new JSlider();
        Dimension size = new Dimension(24,24);
        
        textField = new JTextField();
        textField.setColumns(8);
        
        JLabel lblFps = new JLabel("FPS");
        
        GroupLayout layout = new GroupLayout(this);
        
        Dimension previewSize = new Dimension( master.getCurrentWorkspace().getWidth(), master.getCurrentWorkspace().getHeight());
        
        layout.setHorizontalGroup( layout.createSequentialGroup()
    		.addGap(5)
    		.addGroup(layout.createParallelGroup()
            	.addComponent( previewPanel, 0, previewSize.width, Short.MAX_VALUE)
            	.addGroup( layout.createParallelGroup(Alignment.LEADING)
                	.addGroup(layout.createSequentialGroup()
                		.addGap(5)
                		.addComponent(buttonBack, 24, 24, 24)
                		.addGap(5)
                		.addComponent(buttonPlay, 24, 24, 24)
            			.addGap(5)
            			.addComponent(buttonForward, 24, 24, 24)
            			.addPreferredGap(ComponentPlacement.UNRELATED)
            			.addComponent(textField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            			.addPreferredGap(ComponentPlacement.RELATED)
            			.addComponent(lblFps)
            			.addContainerGap(0, Short.MAX_VALUE)
                	)
                )
                .addGroup( layout.createParallelGroup(Alignment.LEADING)
                	.addGroup( layout.createSequentialGroup()
                		.addGap(5)
                		.addComponent(slider, 0, 186, 186)
            			.addContainerGap(0, Short.MAX_VALUE)
                	)
                )
                .addGap(5)
    		)
        );
        layout.setVerticalGroup( 
        	layout.createSequentialGroup()
        		.addGap(5)
        		.addComponent(previewPanel, 0, previewSize.height, Short.MAX_VALUE)
        		.addGap(5)
        		.addGroup(
    	        	layout.createParallelGroup(Alignment.LEADING)
            		.addGroup(layout.createSequentialGroup()
            			.addGap(5)
            			.addGroup(layout.createParallelGroup(Alignment.LEADING)
            				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
            					.addComponent(textField, 24, 24, 24)
            					.addComponent(lblFps))
            				.addComponent(buttonBack, 24, 24, 24)
            				.addComponent(buttonPlay, 24, 24, 24)
            				.addComponent(buttonForward, 24, 24, 24))
            		)
        		)
    			.addGap(5)
    			.addComponent(slider)
    			.addGap(5)
        );

        this.setLayout(layout);
    }               
}
