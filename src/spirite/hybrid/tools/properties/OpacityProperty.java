package spirite.hybrid.tools.properties;

import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.GroupLayout.Group;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.hybrid.tools.properties.SwingToolProperty;
import spirite.pc.ui.components.SliderPanel;

public class OpacityProperty extends SwingToolProperty {
	private float value;

	public OpacityProperty( String id, String hrName, float defaultValue) {
		this( id, hrName, defaultValue, 0);
	}
	public OpacityProperty( String id, String hrName, float defaultValue, int mask) {
		this.value = defaultValue;
		this.hrName = hrName;
		this.id = id;
		this.mask = mask;
	}

	@Override public Float getValue() { return value; }
	@Override protected void setValue( Object newValue) { this.value = (float)newValue;}
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		SliderPanel slider = new SliderPanel() {
			@Override
			public void onValueChanged(float newValue) {
				binding.triggerUIChanged(newValue);
				super.onValueChanged(newValue);
			}
			@Override
			protected String valueAsString(float value) {
				return super.valueAsString(value*100);
			}
		};
		
		slider.setValue(value);
		slider.setLabel(hrName + " : ");

		binding.setLink( new ChangeExecuter() {
			@Override public void doUIChange(Object newValue) {
				settings.setValue( id, newValue);
			}

			@Override
			public void doDataChange(Object newValue) {
				slider.setValue((float)newValue);
			}
		});
		
		horizontal.addComponent(slider);
		vertical.addComponent(slider, 24,24,24);
		
		return Arrays.asList(new JComponent[] {slider});
	}
}