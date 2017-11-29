package spirite.pc.ui.panel_toolset;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.brains.ToolsetManager;
import spirite.base.brains.ToolsetManager.MToolsetObserver;
import spirite.base.brains.ToolsetManager.Property;
import spirite.base.brains.ToolsetManager.Tool;
import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.brains.tools.ToolSchemes;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.selection.SelectionEngine.MSelectionEngineObserver;
import spirite.base.image_data.selection.SelectionEngine.SelectionEvent;
import spirite.base.util.DataBinding;
import spirite.gui.hybrid.SPanel;
import spirite.hybrid.tools.properties.SwingToolProperty;
import spirite.pc.ui.omni.OmniFrame.OmniComponent;

public class ToolSettingsPanel extends SPanel
	implements OmniComponent,
		MToolsetObserver, MWorkspaceObserver, MSelectionEngineObserver, ActionListener
{
	// External Components
	private final MasterControl master;
	private final ToolsetManager manager;
	private ToolSettings settings;
	private ImageWorkspace workspace = null;
	
	private SPanel settingsPanel = null;
	
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
		settingsPanel = null;
		
		label.setText(tool.description);
		
		container.getViewport().removeAll();
		if( settings != null) {
			settingsPanel = constructFromScheme(settings.getPropertyScheme());
			container.getViewport().add( settingsPanel);
		}
	}
	
	SPanel constructFromScheme( Property[] scheme) {
		bindingMap.clear();
		activeMap.clear();
		
		if( scheme == null) return null;
		SPanel panel = new SPanel();
		
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
	
	private final Map<String,DataBinding> bindingMap = new HashMap<>();
	
	void createNode( Property node, Group horizontal, Group vertical, GroupLayout layout) {
		DataBinding binding = new DataBinding();
		bindingMap.put(node.getId(), binding);
		
		if( node instanceof SwingToolProperty) 
		{
			List<JComponent> toLink = ((SwingToolProperty)node).buildComponent(binding, horizontal, vertical, layout, settings);
			
			if( toLink != null) {
				for( JComponent comp : toLink)
					activeMap.put( comp, node);
			}
		}
	}
	
	private void checkStatus( ) {
		for( Entry<JComponent,Property> entry : activeMap.entrySet()) {
			int mask = entry.getValue().getMask();
			
			if( (mask & ToolSchemes.DISABLE_ON_NO_SELECTION) != 0) {
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
	@Override
	public void toolsetPropertyChanged(Tool tool, Property property) {
		//if( tool == master.getToolsetManager().getSelectedTool()) {
			DataBinding binding = bindingMap.get(property.getId());
			
			if( binding != null) {
				binding.triggerDataChanged(property.getValue());
			}
		//}
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

	// :::: OmniComponent
	@Override public JComponent getComponent() {
		return this;
	}

}
