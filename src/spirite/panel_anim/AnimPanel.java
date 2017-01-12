package spirite.panel_anim;

import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MCurrentImageObserver;
import spirite.image_data.animation_data.AbstractAnimation;
import spirite.image_data.animation_data.SimpleAnimation;
import spirite.ui.UIUtil;
import spirite.ui.UIUtil.MTextFieldNumber;

import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Hashtable;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JLabel;



public class AnimPanel extends javax.swing.JPanel 
        implements MCurrentImageObserver, ActionListener, WindowListener, DocumentListener
{
	private static final long serialVersionUID = 1L;
	MasterControl master;
	Timer timer;
	boolean isPlaying = false;
	
	
    Hashtable<Integer, JLabel> sliderDoc = new Hashtable<>();
	
    AbstractAnimation animation = null;
    
	// Metronome settings
	float start = 0.0f;
	float end = 2.0f;
	float tps = 0.2f;
	float met = 0.0f;

    /**
     * Creates new form PreviewPanel
     */
//    public AnimPanel() { initComponents(); }
    public AnimPanel( MasterControl master) {
        this.master = master;
        
        initComponents();
        
        timer = new Timer(16, this);
        timer.setRepeats(true);
        timer.start();
        
        updateSlider();
        
        // Add Listeners
        tfFPS.getDocument().addDocumentListener( this);
        master.addCurrentImageObserver(this);
        buttonBack.addActionListener(this);
        buttonPlay.addActionListener(this);
        buttonForward.addActionListener(this);
        
        // I hate doing things like this, but since the Timer keeps <this> alive and 
        //	the Swing system keeps the timer alive, <this> will never get GC'd and
        //	the timer will never stop unless you stop it manually.
        final AnimPanel _this = this;
        SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				((java.awt.Window)SwingUtilities.getRoot(_this)).addWindowListener(_this);
			}
		});
        
        constructFromAnimation( new SimpleAnimation( master.getCurrentWorkspace().getRootNode()));
    }
    
    private void updateSlider() {
        tfFPS.setText(Float.toString(tps));
    	sliderDoc.get(0).setText(Float.toString(start));
    	sliderDoc.get(1000).setText(Float.toString(end));
    }

    @Override
    public void imageRefresh() {
    	this.repaint();
    }
	@Override
	public void imageStructureRefresh() {
		this.repaint();
	}
	
	private void constructFromAnimation( AbstractAnimation anim) {
		animation = anim;
		start = anim.getStartFrame();
		end = anim.getEndFrame();
		slider.setValue(0);
		
		updateSlider();
	}
	

    private DisplayPanel previewPanel;
    private MTextFieldNumber tfFPS;
    private JButton buttonBack;
    private JToggleButton buttonPlay;
    private JButton buttonForward;
    private JSlider slider;
    
    private class DisplayPanel extends JPanel {
    	@Override
    	public void paint(Graphics g) {
    		super.paint(g);
    		
    		if( animation != null) {
    			animation.drawFrame(g, met);
    		}
    	}
    }
                          
    private void initComponents() {
        Dimension size = new Dimension(24,24);

        previewPanel = new DisplayPanel();
        
        buttonBack = new JButton();
        buttonPlay = new JToggleButton();
        buttonForward = new JButton();
        
        // Init Slider Properties
        slider = new JSlider();
        slider.setEnabled(false);
		slider.setMinimum(0);
		slider.setMaximum(1000);
		slider.setValue(0);
		sliderDoc.put(0, new JLabel("0.0"));
		sliderDoc.put(1000, new JLabel("1.0"));
        slider.setLabelTable( sliderDoc);
        slider.setPaintLabels(true);
		
        
        tfFPS = new UIUtil.MTextFieldNumber( true, true);
        tfFPS.setColumns(8);
        
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
            			.addComponent(tfFPS, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
            					.addComponent(tfFPS, 24, 24, 24)
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

    
    // :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();
		if( source == timer) {
			if( isPlaying) {
				met += 16.0f * tps / 1000.0f;
				met = MUtil.cycle(start, end, met);
				slider.setValue( Math.round(1000* (met - start) / (end - start)));
//				slider.repaint();
				
				repaint();
			}
		}else if( source == buttonPlay) {
			isPlaying = buttonPlay.isSelected();
		}
	}


	// :::: WindowListener
	@Override
	public void windowClosed(WindowEvent e) {
		timer.stop();
	}
	@Override	public void windowClosing(WindowEvent e) {}
	@Override	public void windowDeactivated(WindowEvent e) {}
	@Override	public void windowDeiconified(WindowEvent e) {}
	@Override	public void windowIconified(WindowEvent e) {}
	@Override	public void windowOpened(WindowEvent e) {}
	@Override	public void windowActivated(WindowEvent e) {}

	// :::: DocumentListener
	@Override
	public void changedUpdate(DocumentEvent arg0) {
		updateTPS();
	}
	@Override
	public void insertUpdate(DocumentEvent arg0) {
		updateTPS();
	}
	@Override
	public void removeUpdate(DocumentEvent arg0) {
		updateTPS();
	}
	
	private void updateTPS() {
		String str = tfFPS.getText();
		
		try {
			tps = Float.parseFloat(str);
		}catch( NumberFormatException e) {
			
		}
	}
	
	
}
