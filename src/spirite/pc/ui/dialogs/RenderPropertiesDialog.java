package spirite.pc.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import spirite.base.brains.MasterControl;
import spirite.base.brains.RenderEngine.RenderMethod;
import spirite.base.graphics.RenderProperties;
import spirite.pc.ui.components.RenderOptionsCombo;
import spirite.pc.ui.components.SliderPanel;
import spirite.pc.ui.panel_layers.LayersPanel.RenderOptionCellRenderer;
import spirite.pc.ui.panel_layers.LayersPanel.RenderTuple;

public class RenderPropertiesDialog extends JPanel {
	private SliderPanel slider;
	
	private final RenderProperties properties;
	private final RenderOptionsCombo renderCombo;
//
	
	public RenderPropertiesDialog(RenderProperties properties, MasterControl master) {
		this.properties = new RenderProperties(properties);
		renderCombo = new RenderOptionsCombo(master);

		initLayout();
		initBindings();
	}
	
	private void initLayout( ) {
		setBounds(0, 0, 250, 100);
		
//		getContentPane().setLayout( new BorderLayout());
//		content.setBorder( new EmptyBorder( 5,5,5,5));
//		content.setPreferredSize( new Dimension(300, 200));
//		getContentPane().add( content, BorderLayout.CENTER);
		
		GroupLayout layout = new GroupLayout(this);
		
		slider = new SliderPanel() {
			@Override
			public void onValueChanged(float newValue) {
				properties.alpha = newValue;
				super.onValueChanged(newValue);
			}
		};
		slider.setMin(0);
		slider.setMax(1);
		slider.setValue(properties.alpha);
		slider.setLabel("Alpha:");

		layout.setHorizontalGroup( layout.createSequentialGroup()
				.addGap(3)
				.addGroup( layout.createParallelGroup()
					.addComponent(slider)
					.addComponent(renderCombo))
				.addGap(3));
		layout.setVerticalGroup( layout.createSequentialGroup()
				.addGap(3)
				.addComponent(slider, 24, 24, 24)
				.addGap(3)
				.addComponent(renderCombo)
				.addGap( 3, 3, Short.MAX_VALUE));
		this.setLayout(layout);
	}
	
	private void initBindings() {
	}
	
	public RenderProperties getResult() {
		properties.method = renderCombo.getMethod();
		properties.renderValue = renderCombo.getRenderValue();
		return properties;
	}
}
