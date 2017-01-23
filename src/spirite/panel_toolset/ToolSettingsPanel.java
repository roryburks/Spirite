package spirite.panel_toolset;

import java.awt.Color;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import spirite.brains.ToolsetManager;
import spirite.brains.ToolsetManager.MToolsetObserver;
import spirite.brains.ToolsetManager.PropertySchemeNode;
import spirite.brains.ToolsetManager.Tool;
import spirite.brains.ToolsetManager.ToolSettings;
import spirite.panel_toolset.PropertyPanels.SizeSlider;
import spirite.ui.UIUtil.SliderPanel;

public class ToolSettingsPanel extends JPanel 
	implements MToolsetObserver
{
	private final ToolsetManager manager;
	private ToolSettings settings;

	
	private final JLabel label;
	private final JScrollPane container;
	public ToolSettingsPanel( ToolsetManager manager) {
		this.manager = manager;
		
		label = new JLabel();
		container = new JScrollPane();
		
		initLayout();
		updatePanel();

		manager.addToolsetObserver(this);
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
		if( scheme == null) return null;
		JPanel panel = new JPanel();
		
		GroupLayout layout = new GroupLayout( panel);
		
		Group horizontal = layout.createParallelGroup();
		Group vertical = layout.createSequentialGroup();
		vertical.addGap(2);
		
		for( int i=0; i < scheme.length; ++i) {
			createNode( scheme[i], horizontal, vertical);
			vertical.addGap(2);
		}
		vertical.addGap(2);
		
		layout.setHorizontalGroup( horizontal);
		layout.setVerticalGroup(vertical);
		panel.setLayout(layout);
		return panel;
	}
	
	void createNode( PropertySchemeNode node, Group horizontal, Group vertical) {
		
		switch( node.type) {
		case SIZE:{
			SizeSlider slider = new SizeSlider() {
				@Override
				public void onValueChanged(float newValue) {
					settings.setValue( node.id, newValue);
					super.onValueChanged(newValue);
				}
			};
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
			slider.setValue((float)node.value);
			slider.setLabel(node.hiName + " : ");
			horizontal.addComponent(slider);
			vertical.addComponent(slider, 24,24,24);
			break;}
		}
	}

	@Override
	public void toolsetChanged(Tool newTool) {
		updatePanel();
		repaint();
	}
	

}
