package spirite.hybrid.tools.properties;

import java.util.Arrays;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.GroupLayout.Group;

import spirite.base.brains.ToolsetManager.ToolSettings;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.DBSub;
import spirite.hybrid.tools.properties.SwingToolProperty;
import spirite.pc.ui.panel_toolset.PropertyPanels.SizeSlider;

public class SizeProperty extends SwingToolProperty {
	private float value;

	public SizeProperty( String id, String hrName, float defaultValue) {
		this( id, hrName, defaultValue, 0);
	}
	public SizeProperty( String id, String hrName, float defaultValue, int mask) {
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
		SizeSlider slider = new SizeSlider() {
			@Override
			public void onValueChanged(float newValue) {
				binding.uiChange(newValue);
				super.onValueChanged(newValue);
			}
		};
		slider.setValue( (float)value);
		slider.setLabel( hrName + " : ");
		
		binding.setLink( new DBSub() {
			@Override public void doUIChange(Object newValue) {
				settings.setValue( id, newValue);
			}

			@Override
			public void doDataChange(Object newValue) {
				slider.setValue((float)newValue);
			}
		});
		
		horizontal.addComponent(slider).addGap(30);
		vertical.addComponent(slider, 24,24,24);
		
		return Arrays.asList(new JComponent[] {slider});
	}
}