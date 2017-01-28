package spirite.panel_anim;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Hashtable;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MCurrentImageObserver;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.Animation;
import spirite.image_data.AnimationManager.AnimationStructureEvent;
import spirite.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.animation_data.FixedFrameAnimation;
import spirite.ui.OmniFrame.OmniComponent;
import spirite.ui.components.MTextFieldNumber;



public class AnimPanel extends OmniComponent
        implements MCurrentImageObserver, ActionListener, WindowListener, 
        	DocumentListener, MWorkspaceObserver, MAnimationStructureObserver
{
	private static final long serialVersionUID = 1L;
	private final Timer timer;
	private boolean isPlaying = false;
	
	
    private final Hashtable<Integer, JLabel> sliderDoc = new Hashtable<>();
	
    private Animation animation = null;
    
	// Metronome settings
	private float start = 0.0f;
	private float end = 2.0f;
	private float tps = 0.2f;
	private float met = 0.0f;
	

	private final MasterControl master;
	private ImageWorkspace workspace;
	
    /**
     * Creates new form PreviewPanel
     */
    public AnimPanel( MasterControl master) {
        this.master = master;
        
        
    	master.addWorkspaceObserver(this);
    	workspace = master.getCurrentWorkspace();
    	if( workspace != null) {
    		workspace.getAnimationManager().addStructureObserver(this);
    	}
    	
        
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
        

        if( master.getCurrentWorkspace() != null) {
	        List<Animation> list = master.getCurrentWorkspace().getAnimationManager().getAnimations();
	        if( !list.isEmpty()) {
	        	constructFromAnimation( list.get(0));
	        }
        }
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
	
	private void constructFromAnimation( Animation anim) {
		
		animation = anim;
		start = anim.getStartFrame();
		end = anim.getEndFrame();
		slider.setValue(0);

		updateSlider();
	}
	

	// Start Design
    private DisplayPanel previewPanel;
    private MTextFieldNumber tfFPS;
    private JButton buttonBack;
    private JToggleButton buttonPlay;
    private JButton buttonForward;
    private JSlider slider;
    
    private class DisplayPanel extends JPanel {
    	@Override
    	public void paintComponent(Graphics g) {
    		super.paintComponent(g);
    		
    		if( animation != null) {
    			Graphics2D g2 = (Graphics2D)g;
    			g2.scale(2.0, 2.0);
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
		
        
        tfFPS = new MTextFieldNumber( true, true);
        tfFPS.setColumns(8);
        
        JLabel lblFps = new JLabel("FPS");
        
        GroupLayout layout = new GroupLayout(this);
        
        
        Dimension previewSize = (master.getCurrentWorkspace() == null)
        		? new Dimension( 128,128)
        		:new Dimension( master.getCurrentWorkspace().getWidth()*2, master.getCurrentWorkspace().getHeight()*2);
        		
        		previewSize = new Dimension(400,400);
        
        layout.setHorizontalGroup( layout.createSequentialGroup()
    		.addGap(5)
    		.addGroup(layout.createParallelGroup()
            	.addComponent( previewPanel, 0, previewSize.width, Short.MAX_VALUE)
            	.addGroup( layout.createParallelGroup(Alignment.LEADING)
                	.addGroup(layout.createSequentialGroup()
                		.addGap(5)
                		.addComponent(buttonBack, size.width, size.width, size.width)
                		.addGap(5)
                		.addComponent(buttonPlay, size.width, size.width, size.width)
            			.addGap(5)
            			.addComponent(buttonForward, size.width, size.width, size.width)
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
            					.addComponent(tfFPS, size.height, size.height, size.height)
            					.addComponent(lblFps))
            				.addComponent(buttonBack, size.height, size.height, size.height)
            				.addComponent(buttonPlay, size.height, size.height, size.height)
            				.addComponent(buttonForward, size.height, size.height, size.height))
            		)
        		)
    			.addGap(5)
    			.addComponent(slider)
    			.addGap(5)
        );

        this.setLayout(layout);
    }
    // End Design

    
    // :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();
		if( source == timer) {
			if( isPlaying) {
				met += 16.0f * tps / 1000.0f;
				met = MUtil.cycle(start, end, met);
				slider.setValue( Math.round(1000* (met - start) / (end - start)));
				
				repaint();
			}
		}else if( source == buttonPlay) {
			isPlaying = buttonPlay.isSelected();
		}else if( source == buttonForward) {		
			((FixedFrameAnimation)this.animation).save();
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

	// :::: MWorkspaceObserver
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		// TODO Auto-generated method stub
		
	}


	// :::: MAnimationStructureObserver
	@Override
	public void animationStructureChanged(AnimationStructureEvent evt) {
		try{
		constructFromAnimation( workspace.getAnimationManager().getAnimations().get(0));
		} catch( NullPointerException e) {
			
		}
		
	}
	
	
}
