package spirite.pc.ui.panel_layers.layer_properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.SwingUtilities;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.MFlashObserver;
import spirite.base.image_data.layers.puppet.BasePuppet.BasePart;
import spirite.base.image_data.layers.puppet.PuppetLayer;
import spirite.gui.hybrid.SButton;
import spirite.gui.hybrid.SPanel;
import spirite.gui.hybrid.SToggleButton;
import spirite.hybrid.Globals;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.pc.graphics.ImageBI;
import spirite.pc.ui.components.BoxList;
import spirite.pc.ui.components.SliderPanel;

public class PuppetLayerPanel extends SPanel 
	implements ActionListener, MFlashObserver
{
	private final MasterControl master;
	private ImageWorkspace workspace;
	PuppetLayer puppet;
	
	PuppetLayerPanel(MasterControl master) {
		this.master = master;
		
		master.addTrackingObserver(MFlashObserver.class, this);
		
		initLayout();
		initBindings();
		refreshProperties();
	}
	
	public void setPuppet( PuppetLayer puppet, ImageWorkspace imageWorkspace) {
		this.puppet = puppet;
		this.workspace = imageWorkspace;
		refreshProperties();
	}
	
	private final String bpp_NORMAL = "Norm";
	private final String bpp_BASE = "Base";
	
	
	private SToggleButton btnSkeleton = new SToggleButton();
	private SToggleButton btnBase = new SToggleButton();
	
	private final int BUTTON_SIZE = 36;
	private BoxList<BasePart> boxPuppetParts = new BoxList<BasePart>(null, BUTTON_SIZE, BUTTON_SIZE) {
		protected boolean attemptMove(int from, int to) {
			puppet.movePart(from,to);
			puppet.setSelectedIndex(to);
			
			return true;
		};
	};
	private final SButton bNewPart = new SButton();
	private final SButton bRemovePart = new SButton();
	private final SToggleButton bNodeVisiblity = new SToggleButton();
	private final OpacitySlider opacitySlider = new OpacitySlider();
	
	private void initLayout() {
		btnSkeleton.setToolTipText("Toggle Skeleton Visibility");
		btnSkeleton.setActionCommand("toggleSkeleton");
		btnSkeleton.addActionListener(this);
		btnSkeleton.setIcon( Globals.getIcon("icon.puppet.skeleton"));
		btnSkeleton.setMargin(new Insets(0, 0, 0, 0));
		
		btnBase.setToolTipText("Toggle Base View");
		btnBase.setActionCommand("toggleBase");
		btnBase.addActionListener(this);
		btnBase.setMargin(new Insets(0, 0, 0, 0));
		
		bNewPart.setIcon(Globals.getIcon("icon.rig.new"));
		bRemovePart.setIcon(Globals.getIcon("icon.rig.rem"));
		
		int btnHeight = 16;
		int btnSkelWidth = 24;
		int btnBaseWidth = 48;

		Dimension btnSize = new Dimension( 24,16);
		
		GroupLayout layout = new GroupLayout(this);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup(layout.createParallelGroup()
				.addGroup(layout.createSequentialGroup()
					.addGap(0,0,Short.MAX_VALUE)
					.addComponent(btnSkeleton, btnSkelWidth, btnSkelWidth, btnSkelWidth)
					.addGap(3)
					.addComponent(btnBase, btnBaseWidth, btnBaseWidth, btnBaseWidth))
				.addComponent(boxPuppetParts)
				.addGroup(layout.createSequentialGroup()
						.addGap(3)
						.addComponent(opacitySlider)
						.addGap(3)
						.addComponent(bNodeVisiblity, btnSize.width, btnSize.width, btnSize.width)
						.addGap(3)
						.addComponent(bNewPart, btnSize.width, btnSize.width, btnSize.width)
						.addGap(3)
						.addComponent(bRemovePart, btnSize.width, btnSize.width, btnSize.width)
						.addGap(3)))
			.addGap(3));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup(layout.createParallelGroup()
				.addComponent(btnBase, btnHeight, btnHeight, btnHeight)
				.addComponent(btnSkeleton, btnHeight, btnHeight, btnHeight))
			.addGap(3)
			.addComponent(boxPuppetParts)
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				.addComponent(bNodeVisiblity, btnSize.height, btnSize.height, btnSize.height)
				.addComponent(bNewPart, btnSize.height, btnSize.height, btnSize.height)
				.addComponent(bRemovePart, btnSize.height, btnSize.height, btnSize.height)
				.addComponent(opacitySlider, btnSize.height, btnSize.height, btnSize.height)
			)
			.addGap(3));
		
		this.setLayout(layout);
	}
	private void initBindings() {
		bNewPart.addActionListener((evt) -> {
			puppet.addNewPart();
		});
		
		boxPuppetParts.setRenderer((t, ind, selected) -> new PartPanel(t, selected)); 
		
		boxPuppetParts.setSelectionAction((i)-> { 
			if( puppet!= null)
				puppet.setSelectedIndex(i);
		});
	}
	
	private class PartPanel extends SPanel{
		final BasePart part;
		PartPanel(BasePart part, boolean selected) {
			this.part = part;
			
			setBorder(BorderFactory.createLineBorder((selected)? Color.BLUE : Color.BLACK));
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			//IImage img = part.getImageHandle().deepAccess();
			
			float sx = BUTTON_SIZE/(float)part.handle.getWidth();
			float sy = BUTTON_SIZE/(float)part.handle.getHeight();
			
			float scale = Math.min( sx, sy);
			
			RawImage img2 = HybridHelper.createImage(BUTTON_SIZE, BUTTON_SIZE);
			GraphicsContext gc = img2.getGraphics();
			gc.scale(scale, scale);
			gc.drawHandle(part.handle, 0, 0);
			
			
			g.drawImage( ((ImageBI)HybridUtil.convert(img2, ImageBI.class)).img, 
					0, 0, null);
		}
	}
	
	private class OpacitySlider extends SliderPanel {
		OpacitySlider() {
			setMin(0.0f);
			setMax(1.0f);
			setLabel("Opacity: ");
		}
		
		@Override
		public void onValueChanged(float newValue) {
			// TODO
			//changePartAttributes();
			super.onValueChanged(newValue);
		}
	}
	
	private void refreshProperties() {
		btnBase.setSelected( puppet != null && puppet.isUsingBase());
		btnBase.setText( btnBase.isSelected() ? bpp_BASE : bpp_NORMAL);
		btnBase.setEnabled( puppet != null && !puppet.isBaseOnly());
		
		btnSkeleton.setEnabled( puppet != null);
		btnSkeleton.setSelected( puppet != null && puppet.isSkeletonVisible());
		
		if( puppet != null)
			boxPuppetParts.resetEntries(puppet.getBase().getParts(), puppet.getSelectedIndex());
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		switch( e.getActionCommand()) {
		case "toggleSkeleton":
			puppet.setSkeletonVisible(btnSkeleton.isSelected());
			refreshProperties();
			break;
		case "toggleBase":
			puppet.setUsingBase( btnBase.isSelected());
			refreshProperties();
			break;
		}
	}

	@Override public void flash() { 
		SwingUtilities.invokeLater(() -> refreshProperties());
	}
}
