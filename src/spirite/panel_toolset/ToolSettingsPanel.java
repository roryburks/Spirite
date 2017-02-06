package spirite.panel_toolset;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.brains.ToolsetManager;
import spirite.brains.ToolsetManager.MToolsetObserver;
import spirite.brains.ToolsetManager.PropertySchemeNode;
import spirite.brains.ToolsetManager.Tool;
import spirite.brains.ToolsetManager.ToolSettings;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.SelectionEngine.MSelectionEngineObserver;
import spirite.image_data.SelectionEngine.SelectionEvent;
import spirite.panel_toolset.PropertyPanels.SizeSlider;
import spirite.ui.OmniFrame.OmniComponent;
import spirite.ui.components.SliderPanel;

public class ToolSettingsPanel extends OmniComponent
	implements MToolsetObserver, MWorkspaceObserver, MSelectionEngineObserver, ActionListener
{
	private final MasterControl master;
	private final ToolsetManager manager;
	private ToolSettings settings;
	private final Map<JComponent,PropertySchemeNode> activeMap = new HashMap<>();
	private ImageWorkspace workspace = null;

	
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
	
	JPanel constructFromScheme( PropertySchemeNode[] scheme) {
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
	
	void createNode( PropertySchemeNode node, Group horizontal, Group vertical, GroupLayout layout) {
		switch( node.type) {
		case SIZE:{
			SizeSlider slider = new SizeSlider() {
				@Override
				public void onValueChanged(float newValue) {
					settings.setValue( node.id, newValue);
					super.onValueChanged(newValue);
				}
			};
			activeMap.put(slider, node);
			slider.setValue( (float)node.value);
			slider.setLabel( node.hiName + " : ");
			horizontal.addComponent(slider).addGap(30);
			vertical.addComponent(slider, 24,24,24);
			break;}
		case OPACITY: {
			SliderPanel slider = new SliderPanel() {
				@Override
				public void onValueChanged(float newValue) {
					settings.setValue(node.id, newValue);
					super.onValueChanged(newValue);
				}
				@Override
				protected String valueAsString(float value) {
					return super.valueAsString(value*100);
				}
			};
			activeMap.put(slider, node);
			slider.setValue((float)node.value);
			slider.setLabel(node.hiName + " : ");
			horizontal.addComponent(slider);
			vertical.addComponent(slider, 24,24,24);
			break;}
		case BUTTON: {
			JButton button = new JButton(node.hiName);
			JPanel panel = new JPanel();
			activeMap.put(button, node);
			
			button.setActionCommand( (String)node.value);
			button.addActionListener(this);

			horizontal.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(button).addComponent(panel));
			vertical.addComponent(button, 20,20,20).addComponent(panel,0,0,0);
			break;}
		case CHECK_BOX: {
			JCheckBox checkbox = new JCheckBox(node.hiName);
			activeMap.put(checkbox, node);
			checkbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					settings.setValue(node.id, checkbox.isSelected());
				}
			});
			checkbox.setFont(new Font("Tahoma",Font.PLAIN, 10));
			
			horizontal.addGap(5).addComponent(checkbox);
			vertical.addComponent(checkbox);
			break;}
		}
	}
	
	private class ToolPropertyComponent extends Component {
		
	}
	
	private void checkStatus( ) {
		for( Entry<JComponent,PropertySchemeNode> entry : activeMap.entrySet()) {
			int mask = entry.getValue().attributeMask;
			
			if( (mask & ToolsetManager.DISABLE_ON_NO_SELECTION) != 0) {
				if( workspace == null || workspace.getSelectionEngine().getSelection() == null)  {
					entry.getKey().setEnabled(false);
				}
				else
					entry.getKey().setEnabled(true);
			}
		}
	}

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
