package spirite.panel_toolset;

import spirite.ui.components.SliderPanel;

public class PropertyPanels {

	public static class SizeSlider extends SliderPanel {
		public SizeSlider() {
			setMin(0);
			setMax(1000);
			setLabel("Size: ");
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
