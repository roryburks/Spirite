package spirite.pc.ui.panel_toolset;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.MToolsetObserver;
import spirite.base.brains.ToolsetManager.Property;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.SelectionEngine.MSelectionEngineObserver;
import spirite.base.image_data.SelectionEngine.SelectionEvent;
import spirite.pc.ui.OmniFrame.OmniComponent;
import spirite.pc.ui.components.SliderPanel;
import spirite.pc.ui.panel_toolset.PropertyPanels.SizeSlider;

public class ToolSettingsPanel extends OmniComponent
	implements MToolsetObserver, MWorkspaceObserver, MSelectionEngineObserver, ActionListener
{
	// External Components
	private final MasterControl master;
	private final ToolsetManager manager;
	private ToolSettings settings;
	private ImageWorkspace workspace = null;
	
	/**
	 * The ActiveMap links components to a property such that they can be 
	 * enabled/disabled/changed based on watched changes depending on their behavior
	 * options.
	 * 
	 * (For most Properties this is unnecessary, but it's good to add them to the
	 * map for future compatibility sake in case they are given behavior that allows
	 * them to be disabled/changed externally.)
	 */
	private final Map<JComponent,Property> activeMap = new HashMap<>();

	
	private final JLabel label;
	private final JScrollPane container;
	public ToolSettingsPanel( MasterControl master) {
		this.master = master;
		this.manager = master.getToolsetManager();
		this.workspace = master.getCurrentWorkspace();
		
		
		
		label = new JLabel();
		container = new JScrollPane();
		
		initLayout();
		updatePanel();
		
		master.addWorkspaceObserver(this);
		manager.addToolsetObserver(this);
		if( workspace != null) {
			workspace.getSelectionEngine().addSelectionObserver( this);
		}

	}
	
	void initLayout() {
		setBackground( Color.WHITE);
		GroupLayout layout = new GroupLayout(this);
		
		layout.setHorizontalGroup( layout.createSequentialGroup()
			.addGap(2)
			.addGroup( layout.createParallelGroup( GroupLayout.Alignment.LEADING)
				.addComponent(label)
				.addComponent(container, 0, 200, Short.MAX_VALUE)
			)
			.addGap(2)
		);
		
		layout.setVerticalGroup( layout.createSequentialGroup()
			.addGap(2)
			.addComponent(label)
			.addGap(2)
			.addComponent(container)
			.addGap(2)
		);
		
		setLayout(layout);
	}
	
	void updatePanel() {
		Tool tool = manager.getSelectedTool();
		settings = manager.getToolSettings(tool);
		
		label.setText(tool.description);
		
		container.getViewport().removeAll();
		if( settings != null) {
			container.getViewport().add(constructFromScheme(settings.getPropertyScheme()));
		}
	}
	
	JPanel constructFromScheme( Property[] scheme) {
		activeMap.clear();
		if( scheme == null) return null;
		JPanel panel = new JPanel();
		
		GroupLayout layout = new GroupLayout( panel);
		
		Group horizontal = layout.createParallelGroup().addGap( 0, 0, Short.MAX_VALUE);
		Group vertical = layout.createSequentialGroup();
		vertical.addGap(2);
		
		for( int i=0; i < scheme.length; ++i) {
			createNode( scheme[i], horizontal, vertical, layout);
			vertical.addGap(2);
		}
		vertical.addGap(2);
		
		layout.setHorizontalGroup( horizontal);
		layout.setVerticalGroup(vertical);
		panel.setLayout(layout);
		checkStatus();
		return panel;
	}
	
	void createNode( Property node, Group horizontal, Group vertical, GroupLayout layout) {
		switch( node.getType()) {
		case SIZE:{
			SizeSlider slider = new SizeSlider() {
				@Override
				public void onValueChanged(float newValue) {
					settings.setValue( node.getId(), newValue);
					super.onValueChanged(newValue);
				}
			};
			activeMap.put(slider, node);
			slider.setValue( (float)node.getValue());
			slider.setLabel( node.getName() + " : ");
			horizontal.addComponent(slider).addGap(30);
			vertical.addComponent(slider, 24,24,24);
			break;}
		case OPACITY: {
			SliderPanel slider = new SliderPanel() {
				@Override
				public void onValueChanged(float newValue) {
					settings.setValue(node.getId(), newValue);
					super.onValueChanged(newValue);
				}
				@Override
				protected String valueAsString(float value) {
					return super.valueAsString(value*100);
				}
			};
			activeMap.put(slider, node);
			slider.setValue((float)node.getValue());
			slider.setLabel(node.getName() + " : ");
			horizontal.addComponent(slider);
			vertical.addComponent(slider, 24,24,24);
			break;}
		case BUTTON: {
			JButton button = new JButton(node.getName());
			JPanel panel = new JPanel();
			activeMap.put(button, node);
			
			button.setActionCommand( (String)node.getValue());
			button.addActionListener(this);

			horizontal.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(button).addComponent(panel));
			vertical.addComponent(button, 20,20,20).addComponent(panel,0,0,0);
			break;}
		case CHECK_BOX: {
			JCheckBox checkbox = new JCheckBox(node.getName());
			activeMap.put(checkbox, node);
			checkbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					settings.setValue(node.getId(), checkbox.isSelected());
				}
			});
			checkbox.setFont(new Font("Tahoma",Font.PLAIN, 10));
			checkbox.setSelected((Boolean)node.getValue());
			
			horizontal.addGap(5).addComponent(checkbox);
			vertical.addComponent(checkbox);
			break;}
		case DROP_DOWN: {
			JComboBox<String> comboBox = new JComboBox<>();
			activeMap.put( comboBox, node);
			JLabel label = new JLabel(node.getName() + ":");
			
			String options[] = (String[])node.getExtra();
			
			// Init Components
			for( int i=0; i<options.length; ++i) {
				comboBox.addItem(options[i]);
			}
			comboBox.setFont(new Font("Tahoma",Font.PLAIN, 10));
			comboBox.setSelectedIndex((Integer)node.getValue());
			comboBox.addItemListener( new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if( e.getStateChange() == ItemEvent.SELECTED)
						settings.setValue(node.getId(), comboBox.getSelectedIndex());					
				}
			});
			
			// Add Components to Layout
			horizontal.addGroup(layout.createParallelGroup()
					.addComponent(label)
					.addComponent(comboBox, 0, 0, Short.MAX_VALUE));
			vertical.addComponent(label)
					.addComponent(comboBox, 20, 20, 20);
			
			break;}
		case RADIO_BUTTON:{
			String options[] = (String[])node.getExtra();
			
			// Create Components and their settings
			JRadioButton[] radioButtons = new JRadioButton[ options.length];
			for( int i=0; i<options.length; ++i) {
				radioButtons[i] = new JRadioButton(options[i]);
				activeMap.put( radioButtons[i], node);
				radioButtons[i].setFont(new Font("Tahoma",Font.PLAIN, 10));
			}
			
			radioButtons[(Integer)node.getValue()].setSelected(true);
			
			// Link actions of the Components, and add them to the layout
			Group horSub = layout.createParallelGroup();
			for( int i=0; i<options.length; ++i) {
				final int index = i;
				radioButtons[i].addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						for( int i=0; i<options.length; ++i) {
							radioButtons[i].setSelected(i == index);
						}
						settings.setValue(node.getId(), index);
					}
				});
				
				horSub.addComponent(radioButtons[i]);
				vertical.addComponent(radioButtons[i]);
			}
			horizontal.addGroup(horSub);
			break;}
		}
	}
	
	private void checkStatus( ) {
		for( Entry<JComponent,Property> entry : activeMap.entrySet()) {
			int mask = entry.getValue().getMask();
			
			if( (mask & ToolsetManager.DISABLE_ON_NO_SELECTION) != 0) {
				if( workspace == null || workspace.getSelectionEngine().getSelection() == null)  {
					entry.getKey().setEnabled(false);
				}
				else
					entry.getKey().setEnabled(true);
			}
		}
	}

	// :::: MToolsetObserver
	@Override
	public void toolsetChanged(Tool newTool) {
		updatePanel();
		repaint();
	}
	
	
	// :::: OmniComponent
	@Override
	public void onCleanup() {
		manager.removeToolsetObserver(this);
		master.removeWorkspaceObserver(this);
	}

	// :::: MWorkspaceObserver
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( workspace != null) {
			workspace.getSelectionEngine().removeSelectionObserver(this);
		}
		workspace = selected;
		if( workspace != null) {
			workspace.getSelectionEngine().addSelectionObserver(this);
		}
	}

	// :::: MSelectionEngineObserver
	@Override	public void buildingSelection(SelectionEvent evt) {}
	@Override
	public void selectionBuilt(SelectionEvent evt) {
		checkStatus();
	}

	// :::: ACtionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		master.executeCommandString(evt.getActionCommand());
	}

}