package spirite.panel_toolset.settings_panels;

import javax.swing.JPanel;

import spirite.brains.ToolsetManager.PixelSettings;
import spirite.ui.UIUtil.SliderPanel;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

public class PixelSettingsPanel extends JPanel {
	private final PixelSettings settings;
	
	private SizeSlider slider;
	/**
	 * Create the panel.
	 */
	public PixelSettingsPanel() {
		this(new PixelSettings());
	}
	public PixelSettingsPanel(PixelSettings settings) {
		this.settings = settings;
		if( this.settings == null) settings = new PixelSettings();
		slider = new SizeSlider();
		slider.setValue(settings.getWidth());
		
		
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(slider, 0, 201, Short.MAX_VALUE)
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(slider, 24,24,24)
					.addContainerGap(252, Short.MAX_VALUE))
		);
		setLayout(groupLayout);
	}
	
	public class SizeSlider extends SliderPanel {
		public SizeSlider() {
			setMin(0);
			setMax(1000);
			setLabel("Size: ");
		}
		
		@Override
		public void onValueChanged(float newValue) {
			settings.setWidth( newValue);
			super.onValueChanged(newValue);
		}
		
		@Override
		protected float valueToWidth(float value) {
			if( value < 10)
				return 0.25f * value/10;
			if( value < 100)
				return 0.25f + 0.25f * (value - 10) / 90;
			if( value < 500) 
				return 0.5f + 0.25f * (value - 100) / 400;
			return 0.75f  + 0.25f * (value - 500) / 500;
		}
		
		@Override
		protected float widthToValue(float portion) {
			if( portion < 0.25)
				return portion * 10 * 4;
			if( portion < 0.5)
				return (portion - 0.25f) * 90 * 4 + 10;
			if( portion < 0.75)
				return (portion - 0.5f) * 400 * 4 + 100;
			return (portion - 0.75f) * 500 * 4 + 500;
		}
	}
}
