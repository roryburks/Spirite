package spirite.panel_layers;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;

import spirite.brains.MasterControl;
import spirite.ui.OmniFrame.OmniComponent;
import spirite.ui.components.SliderPanel;

/**
 * 
 * The Reference System works like this: you can drag any Nodes
 * you want into the ReferenceSchemePanel and those nodes are 
 * drawn either above or bellow the image using various adjustable
 * render settings.  It also possesses different zoom.
 * 
 * The visibility, structure and orientation of the reference
 * section will not change the original image and are not saved
 * by the UndoEngine, but changes to the actual image data are.
 * 
 * 
 * @author Rory Burks
 *
 */
public class ReferenceSchemePanel extends OmniComponent{
	private final ReferenceListPanel referenceTreePanel;
	private final JButton btn1 = new JButton();
	private final JButton btn2 = new JButton();
	private final OpacitySlider opacitySlider = new OpacitySlider();
	
	
	public ReferenceSchemePanel(MasterControl master) {
		referenceTreePanel = new ReferenceListPanel(master, this);
		initComponents();
	}
	
	private void initComponents() {
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(3)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(opacitySlider)
						)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btn1, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
							.addGap(1)
							.addComponent(btn2, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE))
						.addComponent(referenceTreePanel, GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
					.addGap(3))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
						.addGap(3)
					.addComponent(opacitySlider, 20, 20, 20)
					.addGap(0)
					.addComponent(referenceTreePanel, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btn1, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
						.addComponent(btn2, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
					.addGap(16))
		);
		setLayout(groupLayout);
	}
	
	

	/** The OpacitySlider Swing Component */
	class OpacitySlider extends SliderPanel {
		OpacitySlider() {
			setMin(0.0f);
			setMax(1.0f);
			setLabel("Opacity: ");
		}
		
		public void refresh() {

//			referenceTreePanel.workspace.getRefAlpha();
		}
		
		@Override
		public void onValueChanged(float newValue) {
//			referenceTreePanel.workspace.setRefAlpha(getValue());
			super.onValueChanged(newValue);
		}
	}
	
	@Override
	public void onCleanup() {
//		referenceTreePanel.cleanup();
	}
}
