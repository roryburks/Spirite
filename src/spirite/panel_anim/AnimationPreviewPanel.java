package spirite.panel_anim;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import spirite.Globals;
import spirite.MUtil;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MCurrentImageObserver;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.Animation;
import spirite.image_data.AnimationManager;
import spirite.image_data.AnimationManager.AnimationStructureEvent;
import spirite.image_data.AnimationManager.MAnimationStateEvent;
import spirite.image_data.AnimationManager.MAnimationStateObserver;
import spirite.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.animation_data.FixedFrameAnimation;
import spirite.ui.OmniFrame.OmniComponent;
import spirite.ui.components.MTextFieldNumber;



public class AnimationPreviewPanel extends OmniComponent
        implements MCurrentImageObserver, ActionListener, 
        	DocumentListener, MWorkspaceObserver, MAnimationStructureObserver, 
        	ChangeListener, MAnimationStateObserver
{
	private static final long serialVersionUID = 1L;
	private final Timer timer;
	private boolean isPlaying = false;
	
	
    private final Hashtable<Integer, JLabel> sliderDoc = new Hashtable<>();
    JLabel lbLeft = new JLabel();
    JLabel lbRight = new JLabel();
	
//    private Animation animation = null;
    
	// Metronome settings
	private float start = 0.0f;
	private float end = 2.0f;
	private float tps = 0.2f;
//	private float met = 0.0f;
	

	private final MasterControl master;
	private ImageWorkspace workspace;
	private AnimationManager animationManager;
	

	// Start Design
    private DisplayPanel previewPanel;
    private MTextFieldNumber tfFPS;
    private JButton buttonBack;
    private JToggleButton buttonPlay;
    private JButton buttonForward;
    private final JSlider slider = new JSlider();
    private final SliderLimiter sliderLimiter = new SliderLimiter();
    
	
    /**
     * Creates new form PreviewPanel
     */
    public AnimationPreviewPanel( MasterControl master) {
        this.master = master;
        
        // Link Observers
        master.addWorkspaceObserver(this);
    	workspace = master.getCurrentWorkspace();
    	if( workspace != null) {
    		animationManager = workspace.getAnimationManager();
    		animationManager.addAnimationStructureObserver(this);
    		animationManager.addAnimationStateObserver(this);
    	}
    	else
    		animationManager = null;
    	
        
        initComponents();
        
        timer = new Timer(16, this);
        timer.setRepeats(true);
        timer.start();
        
        
        updateSlider();

        slider.addChangeListener(this);
        
        // Add Listeners
        tfFPS.getDocument().addDocumentListener( this);
        master.addCurrentImageObserver(this);
        buttonBack.addActionListener(this);
        buttonPlay.addActionListener(this);
        buttonForward.addActionListener(this);
        
        buttonBack.setIcon(Globals.getIcon("icon.anim.stepB"));
        buttonPlay.setIcon(Globals.getIcon("icon.anim.play"));
        buttonForward.setIcon(Globals.getIcon("icon.anim.stepF"));
        

        if( animationManager != null) {
        	reconstructFromSelectedAnimation();
        }
    }
    private void initComponents() {
        Dimension size = new Dimension(24,24);

        previewPanel = new DisplayPanel();
        
        buttonBack = new JButton();
        buttonPlay = new JToggleButton();
        buttonForward = new JButton();
        
        // Init Slider Properties
        slider.setEnabled(true);
		
        
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
                		.addGroup( layout.createParallelGroup()
                			.addComponent(slider, 0, 186, 186)
                			.addComponent(sliderLimiter)
                		)
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
    			.addComponent(sliderLimiter, 16, 16, 16)
    			.addGap(5)
        );

        this.setLayout(layout);
    }
    // End Design

    class SliderLimiter extends JPanel {
    	
    	@Override
    	public void paintComponent(Graphics g) {
    		
    	}
    }
    

    
    private void updateSlider() {
        tfFPS.setText(Float.toString(tps));

    	slider.setMinimum((int) start);
    	slider.setMaximum((int) end-1);
		slider.setValue((int)start);
        
    	sliderDoc.clear();
    	lbLeft.setText(Float.toString(start));
    	lbRight.setText(Float.toString(end-1));
    	sliderDoc.put((int)start, lbLeft);
    	sliderDoc.put((int)(end-1), lbRight);
        slider.setLabelTable( sliderDoc);

        slider.setPaintLabels(true);
    	slider.setPaintTicks(true);
    	slider.setSnapToTicks(true);
    	slider.setMajorTickSpacing(1);
    }

    @Override
    public void imageRefresh() {
    	this.repaint();
    }
	@Override
	public void imageStructureRefresh() {
		this.repaint();
	}
	
	private void reconstructFromSelectedAnimation() {
		if( animationManager == null) {
			start = 0;
			end = 1;
			slider.setValue(0);
			updateSlider();
		}
		else {
			Animation animation = animationManager.getSelectedAnimation();
			
			if( animation != null) {
			
				start = animation.getStartFrame();
				end = animation.getEndFrame();
				slider.setValue(0);
		
				updateSlider();
			}
		}
	}
	

    
    private class DisplayPanel extends JPanel {
    	@Override
    	public void paintComponent(Graphics g) {
    		super.paintComponent(g);
    		if( animationManager == null) return;
    		Animation animation = animationManager.getSelectedAnimation();
    		
    		if( animation != null) {
    			Graphics2D g2 = (Graphics2D)g;
    			g2.scale(2.0, 2.0);
    			animation.drawFrame(g, animationManager.getAnimationState(animation).getMetronom());
    		}
    	}
    }
    
    
    // :::: ActionListener for Timper
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();
		if( animationManager == null) return;
		Animation animation = animationManager.getSelectedAnimation();
		if( animation == null) return;
		
		if( source == timer) {
			if( isPlaying) {
				
				float met = animationManager.getAnimationState(animation).getMetronom() +  16.0f * tps / 1000.0f;
				met = MUtil.cycle(animation.getStartFrame(), animation.getEndFrame(), met);
				animationManager.getAnimationState(animation).setMetronome(met);
				slider.setValue((int) Math.floor(met));
				
			}
		}else if( source == buttonPlay) {
			isPlaying = buttonPlay.isSelected();
		}else if( source == buttonForward) {		
			((FixedFrameAnimation)animation).save();
		}
	}

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
		}catch( NumberFormatException e) {}
	}

	// :::: MWorkspaceObserver
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( workspace != null) {
			animationManager.removeAnimationStateObserver(this);
			animationManager.removeAnimationStructureObserver(this);
		}
    	workspace = master.getCurrentWorkspace();
    	if( workspace != null) {
    		animationManager = workspace.getAnimationManager();
    		animationManager.addAnimationStructureObserver(this);
    		animationManager.addAnimationStateObserver(this);
    	}
    	else
    		animationManager = null;
    	reconstructFromSelectedAnimation();
	}
	@Override
	public void stateChanged(ChangeEvent arg0) {

		repaint();
	}


	// :::: MAnimationStructureObserver
	@Override public void animationAdded(AnimationStructureEvent evt) {
		refresh();
	}
	@Override public void animationRemoved(AnimationStructureEvent evt) {
		refresh();
	}
	@Override public void animationChanged(AnimationStructureEvent evt) {
		refresh();
	}
	private void refresh() {	
		// Lazy, but effective
		try{
		reconstructFromSelectedAnimation();
		} catch( NullPointerException e) {}
	}
	
	// MAnimationStateObserver
		@Override
		public void selectedAnimationChanged(MAnimationStateEvent evt) {
			refresh();
		}
		@Override
		public void animationFrameChanged(MAnimationStateEvent evt) {
			repaint();
		}
	
	// :::: Inherited from OmniContainer
	@Override
	public void onCleanup() {
		timer.stop();
	}
	
	
	
}
