package spirite.hybrid.tools.properties;

import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;
import javax.swing.JComponent;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.pc.ui.components.SliderPanel;

public class SliderProperty extends SwingToolProperty {
	private float value;
	private final float min;
	private final float max;

	public SliderProperty( String id, String hrName, float defaultValue) {
		this( id, hrName, defaultValue, 0, 1, 0);
	}
	public SliderProperty( String id, String hrName, float defaultValue, int mask) {
		this( id, hrName, defaultValue, 0, 1, mask);
	}
	public SliderProperty( String id, String hrName, float defaultValue, float min, float max) {
		this( id, hrName, defaultValue, min, max, 0);
	}
	public SliderProperty( String id, String hrName, float defaultValue, float min, float max, int mask) {
		this.value = defaultValue;
		this.hrName = hrName;
		this.id = id;
		this.mask = mask;
		this.min = min;
		this.max = max;
	}

	@Override public Float getValue() { return value; }
	@Override protected void setValue( Object newValue) { this.value = (float)newValue;}
	

	@Override
	public List<JComponent> buildComponent(DataBinding binding, Group horizontal, Group vertical,
			GroupLayout layout, ToolSettings settings) 
	{
		SliderPanel slider = new SliderPanel(min, max) {
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
			@Override public void doUIChanged(Object newValue) {
				settings.setValue( id, newValue);
			}

			@Override
			public void doDataChanged(Object newValue) {
				slider.setValue((float)newValue);
			}
		});
		
		horizontal.addComponent(slider);
		vertical.addComponent(slider, 24,24,24);
		
		return Arrays.asList(new JComponent[] {slider});
	}
}