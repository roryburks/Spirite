package spirite.pc.ui.panel_anim;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.image_data.Animation;
import spirite.base.image_data.AnimationManager;
import spirite.base.image_data.AnimationManager.AnimationStructureEvent;
import spirite.base.image_data.AnimationManager.MAnimationStateEvent;
import spirite.base.image_data.AnimationManager.MAnimationStateObserver;
import spirite.base.image_data.AnimationManager.MAnimationStructureObserver;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.ImageChangeEvent;
import spirite.base.image_data.ImageWorkspace.MImageObserver;
import spirite.base.image_data.ImageWorkspace.StructureChangeEvent;
import spirite.base.image_data.animations.ffa.FixedFrameAnimation;
import spirite.base.util.MUtil;
import spirite.gui.hybrid.SButton;
import spirite.gui.hybrid.SPanel;
import spirite.gui.hybrid.SSlider;
import spirite.gui.hybrid.SToggleButton;
import spirite.hybrid.Globals;
import spirite.pc.graphics.awt.AWTContext;
import spirite.pc.ui.components.MTextFieldNumber;
import spirite.pc.ui.omni.OmniFrame.OmniComponent;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;



public class AnimationPreviewPanel extends SPanel
        implements OmniComponent,
        	MImageObserver, ActionListener, 
        	DocumentListener, MWorkspaceObserver, MAnimationStructureObserver, 
        	ChangeListener, MAnimationStateObserver
{
	private static final long serialVersionUID = 1L;
	private final Timer timer;
	private boolean isPlaying = false;
	
	
    private final Hashtable<Integer, JLabel> sliderDoc = new Hashtable<>();
	
	// Display Settings
	private float start = 0.0f;
	private float end = 2.0f;
	private float tps = 8.0f;
	private int zoom_level = 1;
	private boolean isFixedFrame = true;
	

	private final MasterControl master;
	private ImageWorkspace workspace;
	private AnimationManager animationManager;
	

	// Start Design
    private final JLabel lbLeft = new JLabel();
    private final JLabel lbRight = new JLabel();
    private final DisplayPanel previewPanel = new DisplayPanel();
    private MTextFieldNumber tfFPS;
    private final SButton buttonBack = new SButton();
    private final SToggleButton buttonPlay = new SToggleButton();
    private final SButton buttonForward = new SButton();
    private final SButton buttonExport = new SButton();
    private final SSlider slider = new SSlider();
    private final SliderLimiter sliderLimiter = new SliderLimiter();
    private final JSpinner spinner = new JSpinner();
    private final SButton buttonColor = new SButton();
    
	
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
        master.addGlobalImageObserver(this);
    	
        // Initialize UI
        initComponents();
        initBindings();
        
        timer = new Timer(16, this);
        timer.setRepeats(true);
        timer.start();
        
        updateSlider();

        // Add Listeners
        slider.addChangeListener(this);
        tfFPS.getDocument().addDocumentListener( this);
        buttonBack.addActionListener(this);
        buttonPlay.addActionListener(this);
        buttonForward.addActionListener(this);
        buttonExport.addActionListener(this);
        

        if( animationManager != null) {
        	reconstructFromSelectedAnimation();
        }
    }
    private void initComponents() {
        Dimension size = new Dimension(24,24);
        
        // Init ComponentProperties
        slider.setEnabled(true);
        
        tfFPS = new MTextFieldNumber( true, true);
        tfFPS.setColumns(8);
        
        buttonBack.setIcon(Globals.getIcon("icon.anim.stepB"));
        buttonPlay.setIcon(Globals.getIcon("icon.anim.play"));
        buttonForward.setIcon(Globals.getIcon("icon.anim.stepF"));
        buttonExport.setIcon(Globals.getIcon("icon.anim.export"));
        buttonExport.setToolTipText("Export Animation");
        
        JLabel lblFps = new JLabel("FPS");
        JLabel lbZoom = new JLabel("Zoom:");
        
        // Init Layout
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
            			.addGap(5)
            			.addComponent(buttonColor, size.width, size.width, size.width)
            			.addContainerGap(0, Short.MAX_VALUE)
            			.addComponent(lbZoom)
            			.addComponent(spinner, 40,40,40)
            			.addGap(5)
            			.addComponent(buttonExport, size.width, size.width, size.width)
            			.addGap(10)
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
            				.addComponent(buttonForward, size.height, size.height, size.height)
                			.addComponent(spinner, size.height, size.height, size.height)
                			.addComponent(lbZoom, size.height, size.height, size.height)
            				.addComponent(buttonExport, size.height, size.height, size.height)
            				.addComponent(buttonColor, size.height, size.height, size.height))
            		)
        		)
    			.addGap(5)
    			.addComponent(slider)
    			.addComponent(sliderLimiter, 16, 16, 16)
    			.addGap(5)
        );

        this.setLayout(layout);
    }
    
    private void initBindings() {
        spinner.setModel( new AbstractSpinnerModel() {
			@Override
			public void setValue(Object value) {
				if( value instanceof Integer) {
					zoom_level = (Integer)value;
					this.fireStateChanged();
				}
			}
			@Override
			public Object getValue() {
				if( zoom_level >= 0) {
					return Integer.toString(zoom_level+1);
				}
				else
					return "1/"+Integer.toString((-zoom_level)+1);
			}
			@Override
			public Object getPreviousValue() {
				return zoom_level-1;
			}
			@Override
			public Object getNextValue() {
				return zoom_level+1;
			}
		});
        spinner.addChangeListener( (e) -> {
			previewPanel.repaint();
		});
        
        buttonColor.addActionListener((evt) -> {
        	previewPanel.setBackground(
        			master.getDialogs().pickColor(previewPanel.getBackground()));
        });
    }
    // End Design
    
    

    class SliderLimiter extends SPanel {
    	
    	@Override
    	public void paintComponent(Graphics g) {
    		
    	}
    }
    
    private float getZoom() {
    	return (zoom_level >= 0)
    			?( zoom_level+1)
    			: (1.0f / (float)(-zoom_level+1));
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
    	slider.setSnapToTicks(isFixedFrame);
    	slider.setMajorTickSpacing(1);
    }

    @Override
    public void imageChanged(ImageChangeEvent evt) {
    	this.repaint();
    }
	@Override
	public void structureChanged(StructureChangeEvent evt) {
		this.repaint();
	}
	
	private void reconstructFromSelectedAnimation() {
		if( animationManager == null) {
			start = 0;
			end = 1;
			slider.setValue(0);
			isFixedFrame = true;
			updateSlider();
		}
		else {
			Animation animation = animationManager.getSelectedAnimation();
			
			if( animation != null) {
			
				start = animation.getStartFrame();
				end = animation.getEndFrame();
				slider.setValue(0);
				isFixedFrame = animation.isFixedFrame();
		
				updateSlider();
			}
		}
	}
	

    
    private class DisplayPanel extends SPanel {
    	@Override
    	public void paintComponent(Graphics g) {
    		super.paintComponent(g);
    		if( animationManager == null) return;
    		Animation animation = animationManager.getSelectedAnimation();
    		
    		if( animation != null) {
    			Graphics2D g2 = (Graphics2D)g;
    			g2.scale(getZoom(), getZoom());
    			animation.drawFrame(new AWTContext(g, getWidth(), getHeight()), 
    					animationManager.getAnimationState(animation).getMet());
    		}
    	}
    }
    
    
    // :::: ActionListener for Timer
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object source = evt.getSource();
		if( animationManager == null) return;
		Animation animation = animationManager.getSelectedAnimation();
		if( animation == null) return;
		
		if( source == timer) {
			if( isPlaying) {
				float met = animationManager.getAnimationState(animation).getMet() +  16.0f * tps / 1000.0f;
				met = MUtil.cycle(animation.getStartFrame(), animation.getEndFrame(), met);
				animationManager.getAnimationState(animation).setMet(met);
				
				slider.setValue((int) Math.floor(met));
			}
		}else if( source == buttonPlay) {
			isPlaying = buttonPlay.isSelected();
		}else if( source == buttonForward) {
			buttonPlay.setSelected(false);
			isPlaying = false;

			float met = animationManager.getAnimationState(animation).getMet() +  1.0f;
			met = MUtil.cycle(animation.getStartFrame(), animation.getEndFrame(), met);
			animationManager.getAnimationState(animation).setMet(met);
			slider.setValue((int) Math.floor(met));
//			((FixedFrameAnimation)animation).save();
		}else if( source == buttonBack) {
			buttonPlay.setSelected(false);
			isPlaying = false;

			float met = animationManager.getAnimationState(animation).getMet() -  1.0f;
			met = MUtil.cycle(animation.getStartFrame(), animation.getEndFrame(), met);
			animationManager.getAnimationState(animation).setMet(met);
			slider.setValue((int) Math.floor(met));
		}else if( source == buttonExport) {
			if( animation instanceof FixedFrameAnimation) {
				try {
					File f = master.getDialogs().pickAAFExport();
					if( f != null) {
						//AnimIO.exportFFAnim((FixedFrameAnimation) animation, f);
						throw new IOException("meh");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
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
	
	// :::: MAnimationStateObserver
	@Override	public void viewStateChanged(MAnimationStateEvent evt) {}
	@Override
	public void selectedAnimationChanged(MAnimationStateEvent evt) {
		refresh();
	}
	@Override
	public void animationFrameChanged(MAnimationStateEvent evt) {
		repaint();
	}
	
	// :::: Omnicomponent
	@Override
	public void onCleanup() {
		timer.stop();
	}
	@Override public JComponent getComponent() {
		return this;
	}
	
	
	
}
