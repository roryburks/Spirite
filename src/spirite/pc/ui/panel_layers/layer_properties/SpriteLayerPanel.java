package spirite.pc.ui.panel_layers.layer_properties;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.GraphicsContext;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.base.image_data.layers.SpriteLayer.PartStructure;
import spirite.base.image_data.layers.SpriteLayer.RigStructureObserver;
import spirite.base.util.DataBinding;
import spirite.base.util.DataBinding.ChangeExecuter;
import spirite.hybrid.Globals;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.HybridUtil;
import spirite.hybrid.MDebug;
import spirite.pc.graphics.ImageBI;
import spirite.pc.ui.UIUtil;
import spirite.pc.ui.components.BoxList;
import spirite.pc.ui.components.MTextFieldNumber;
import spirite.pc.ui.components.SliderPanel;

public class SpriteLayerPanel extends JPanel 
	implements 
		ActionListener,
		RigStructureObserver, 
		DocumentListener 
{
	private final MTextFieldNumber tfTransX = new MTextFieldNumber(true,true);
	private final MTextFieldNumber tfTransY = new MTextFieldNumber(true,true);
	private final MTextFieldNumber tfScaleX = new MTextFieldNumber(true,true);
	private final MTextFieldNumber tfScaleY = new MTextFieldNumber(true,true);
	private final MTextFieldNumber tfRot = new MTextFieldNumber(true,true);
	private final MTextFieldNumber tfDepth = new MTextFieldNumber(true,false);
	private final JTextField tfName= new JTextField();
	private final JButton bNewPart = new JButton();
	private final JButton bRemovePart = new JButton();
	private final JToggleButton bNodeVisiblity = new JToggleButton();
	private final OpacitySlider opacitySlider = new OpacitySlider();
	private final BoxList<Part> boxList;
	
	private final MasterControl master;
	private ImageWorkspace workspace;
	private SpriteLayer rig;
	
	DataBinding<Integer> boxListBinding = new DataBinding<>();
	
	public SpriteLayerPanel( MasterControl master) {
		this.master = master;
		
		boxList = new BoxList<Part>(null, 24,24) {
			@Override
			protected boolean attemptMove(int from, int to) {
				rig.movePart(from, to);
				
				// TODO : Bad
				SwingUtilities.invokeLater(() -> {
					rig.setActivePart(rig.getParts().get(to));
					refresh();
				});
				return true;
			}
		};

		initComponents();
		initBindings();
		
		boxListBinding.setLink(new ChangeExecuter<Integer>() {
			@Override
			public void doUIChanged(Integer newValue) {
				if( newValue != -1) {
					rig.setActivePart(rig.getParts().get(newValue));
					refresh();
				}
			}
			@Override
			public void doDataChanged(Integer newValue) {
				boxList.setSelectedIndex(newValue);
			}
		});
		
		boxList.setRenderer( (part, index, selected) -> {
			return new PartButton(part, selected);
		});
		boxList.setSelectionAction((index) -> {
			boxListBinding.triggerUIChanged(index);
		});
		
		refresh();
	}
	

	private void initComponents() {
		GroupLayout layout = new GroupLayout(this);

		JLabel lTrans = new SubLabel("Translation");
		JLabel lScale = new SubLabel("Scale: ");
		JLabel lRot = new SubLabel("Rotation:");
		JLabel lTX = new SubLabel("x");
		JLabel lTY = new SubLabel("y");
		JLabel lSX = new SubLabel("x");
		JLabel lSY = new SubLabel("y");
		JLabel lDepth = new SubLabel("Depth:");
		JLabel Ttype = new SubLabel("Type:");
		//JScrollPane jscroll = new JScrollPane(listPanel);

		bNewPart.setToolTipText("Create New Part");
		bRemovePart.setToolTipText("Remove Selected Part");
		bNodeVisiblity.setToolTipText("Toggle Node Visibility");
		bNewPart.setActionCommand("newPart");
		bRemovePart.setActionCommand("remPart");
		bNodeVisiblity.setActionCommand("toggleVis");
		bNewPart.addActionListener(this);
		bRemovePart.addActionListener(this);
		bNodeVisiblity.addActionListener(this);

		bNewPart.setIcon(Globals.getIcon("icon.rig.new"));
		bRemovePart.setIcon(Globals.getIcon("icon.rig.rem"));
		bNodeVisiblity.setSelectedIcon(Globals.getIcon("icon.rig.visOn"));
		bNodeVisiblity.setIcon(Globals.getIcon("icon.rig.visOff"));

		tfTransX.getDocument().addDocumentListener(this);
		tfTransY.getDocument().addDocumentListener(this);
		tfScaleX.getDocument().addDocumentListener(this);
		tfScaleY.getDocument().addDocumentListener(this);
		tfRot.getDocument().addDocumentListener(this);
		tfDepth.getDocument().addDocumentListener(this);
		tfName.getDocument().addDocumentListener(this);
		
		Dimension bSize = new Dimension( 24,16);
		
		layout.setVerticalGroup( layout.createSequentialGroup()
			.addContainerGap()
			//.addComponent(jscroll, 0, 200, Short.MAX_VALUE)
			.addComponent(boxList, 0, 200, Short.MAX_VALUE)
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				.addComponent(bNodeVisiblity, bSize.height, bSize.height, bSize.height)
				.addComponent(bNewPart, bSize.height, bSize.height, bSize.height)
				.addComponent(bRemovePart, bSize.height, bSize.height, bSize.height)
				.addComponent(opacitySlider, bSize.height, bSize.height, bSize.height)
			)
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				.addComponent(Ttype)
				.addComponent(tfName)
			)
			.addGap(3)
			.addComponent(lTrans)
			.addGroup( layout.createParallelGroup()
				.addComponent(lTX)
				.addComponent(lTY)
				.addComponent(tfTransX)
				.addComponent(tfTransY)
			)
			.addComponent(lScale)
			.addGroup( layout.createParallelGroup()
				.addComponent(lSX)
				.addComponent(lSY)
				.addComponent(tfScaleX)
				.addComponent(tfScaleY)
			)
			.addGroup( layout.createParallelGroup()
				.addComponent(lRot)
				.addComponent(tfRot)
			)
			.addGroup( layout.createParallelGroup()
				.addComponent(lDepth)
				.addComponent(tfDepth)
			)
			.addContainerGap()
		);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				//.addComponent(jscroll, 0, 200, Short.MAX_VALUE)
				.addComponent(boxList, 0, 200, Short.MAX_VALUE)
				.addComponent(lTrans)
				.addComponent(lScale)
				.addGroup(layout.createSequentialGroup()
					.addComponent(Ttype)
					.addGap(3)
					.addComponent(tfName)
				)
				.addGroup( layout.createSequentialGroup()
					.addGroup( layout.createParallelGroup()
						.addComponent(tfTransX)
						.addComponent(tfScaleX)
					)
					.addGap(3)
					.addGroup( layout.createParallelGroup()
						.addComponent(lTX)
						.addComponent(lSX)
					)
					.addGap(3)
					.addGroup( layout.createParallelGroup()
						.addComponent(tfTransY)
						.addComponent(tfScaleY)
					)
					.addGap(3)
					.addGroup( layout.createParallelGroup()
						.addComponent(lTY)
						.addComponent(lSY)
					)
				)
				.addGroup( layout.createSequentialGroup()
					.addComponent(lRot)
					.addComponent(tfRot)
				)
				.addGroup( layout.createSequentialGroup()
					.addComponent(lDepth)
					.addComponent(tfDepth)
				)
//				.addGroup(layout.createSequentialGroup()
//					.addComponent(labelT)
//					.addGap(10)
//					.addComponent(tfName, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
//				)
				.addGroup(layout.createSequentialGroup()
					.addGap(3)
					.addComponent(opacitySlider)
					.addGap(3)
					.addComponent(bNodeVisiblity, bSize.width, bSize.width, bSize.width)
					.addGap(3)
					.addComponent(bNewPart, bSize.width, bSize.width, bSize.width)
					.addGap(3)
					.addComponent(bRemovePart, bSize.width, bSize.width, bSize.width)
					.addGap(3)
				)
			)
			.addGap(3)
		);
		
		if( rig != null)
			boxListBinding.triggerDataChanged(rig.getActivePartIndex());
//		layout.linkSize( tfTransX, tfTransY, tfScaleX, tfTransY);
		this.setLayout(layout);
	}
	
	private void initBindings() {
		Map<KeyStroke,Action> actionMap = new HashMap<>();

		actionMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), UIUtil.buildAction((evt) -> {
			System.out.println("TEST" + evt.getSource());
		}));
		
		UIUtil.buildActionMap(boxList, actionMap);
	}
	
	
	
	void setRig( SpriteLayer setTo, ImageWorkspace ws) {
		if( rig != null) 
			rig.removeRigObserver(this);
		
		rig = setTo;
		workspace = ws;
		
		if( rig != null)
			rig.addRigObserver(this);

		refresh();
	}

	private final Color cEnabled=  new Color(255,255,255);
	private final Color cDisabled=  new Color(238,238,238);
	private boolean building = false;
	private void refresh() {
		// Listeners and Observers makes the order of things extremely tricky
		// since it's difficult or impossible to differentiate between a
		//	document change update which was caused automatically when 
		//	constructing a document and one that was caused by user input
		//	of some form.
		//
		// In general, the order:
		//	1: StartBuilding (lock out all on-change listeners)
		//  2: Adjust all the values of UI components
		//	3: Set the selection to the appropriate component
		//	4: EndBuilding (unlock the listeners)
		//Is the ordering you want, remembering to lock out SelectionListeners
		//	as well as document ones.
		if( building) return;
		
		building = true;

		
		if( rig != null) {
			bNewPart.setEnabled(true);
			bRemovePart.setEnabled(true);
			
			boxList.resetEntries(rig.getParts(), rig.getParts().indexOf(rig.getActivePart()));

			Part part = rig.getActivePart();
			
			if( part != null) {
				tfDepth.setText(""+part.getDepth());
				tfName.setText(part.getTypeName());
				tfTransX.setText(""+part.getTranslationX());
				tfTransY.setText(""+part.getTranslationY());
				tfScaleX.setText(""+part.getScaleX());
				tfScaleY.setText(""+part.getScaleY());
				tfRot.setText(""+part.getRotation());
				bNodeVisiblity.setSelected(part.isVisible());
				opacitySlider.setValue(part.getAlpha());
			}
		}

		bNewPart.setEnabled(!(rig==null));
		bRemovePart.setEnabled(!(rig==null));
		bNodeVisiblity.setEnabled(!(rig==null));
		tfDepth.setEnabled(!(rig==null));
		tfName.setEnabled(!(rig==null));
		tfTransX.setEnabled(!(rig==null));
		tfTransY.setEnabled(!(rig==null));
		tfScaleX.setEnabled(!(rig==null));
		tfScaleY.setEnabled(!(rig==null));
		tfRot.setEnabled(!(rig==null));
		//listPanel.setEnabled(!(rig==null));
		opacitySlider.setEnabled(!(rig==null));
		
		Color c = (rig == null) ? cDisabled : cEnabled;

		//listPanel.setBackground(c);
		tfDepth.setBackground(c);
		tfName.setBackground(c);
		tfTransX.setBackground(c);
		tfTransY.setBackground(c);
		tfScaleX.setBackground(c);
		tfScaleY.setBackground(c);
		tfRot.setBackground(c);
		
		

		
		building = false;

	}

	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		switch( evt.getActionCommand()) {
		case "newPart":
			if( rig != null) {
				RawImage img = HybridHelper.createImage(1, 1);
				
				rig.addPart(img, "");
			}
			break;
		case "remPart":
			if( rig != null && rig.getActivePart() != null) {
				rig.removePart(rig.getActivePart());
			}
			break;
		case "toggleVis":
			changePartAttributes();
			break;
		default:
			MDebug.log(evt.getActionCommand());
		}
		
		boxList.requestFocus();
	}

	// :::: RigStructureObserver
	@Override
	public void rigStructureChanged() {
		refresh();
	}

	// :::: DocumentLisstener
	@Override
	public void changedUpdate(DocumentEvent evt) {
		if( building) return;
		changePartAttributes();
	}

	@Override
	public void insertUpdate(DocumentEvent evt) {
		if(  building) return;

		changePartAttributes();
	}

	@Override
	public void removeUpdate(DocumentEvent evt) {
		if(  building) return;
		
		changePartAttributes();
		
	}
	
	private void changePartAttributes() {
		building = true;
		
		if( rig != null && rig.getActivePart() != null) {
			Part part = rig.getActivePart();
			float rot = (float) ((float)tfRot.getFloat()/180*Math.PI);
			
			if( part.getTranslationX() != tfTransX.getFloat() ||
				part.getTranslationY() != tfTransY.getFloat() ||
				part.getScaleX() != tfScaleX.getFloat() ||
				part.getScaleY() != tfScaleY.getFloat() ||
				part.getRotation() != rot ||
				part.getDepth() != tfDepth.getInt() ||
				!part.getTypeName().equals(tfName.getText()) ||
				part.isVisible() != bNodeVisiblity.isSelected() ||
				part.getAlpha() != opacitySlider.getValue()) 
			{
				PartStructure ps = part.getStructure();
				ps.transX = tfTransX.getFloat();
				ps.transY = tfTransY.getFloat();
				ps.scaleX = tfScaleX.getFloat();
				ps.scaleY = tfScaleY.getFloat();
				ps.rot = rot;
				ps.depth = tfDepth.getInt();
				ps.partName = tfName.getText();
				ps.visible = bNodeVisiblity.isSelected();
				ps.alpha = opacitySlider.getValue();
				
				rig.modifyPart( part,ps);
			}
		}
		building = false;
	}
	

	private class PartButton extends JPanel {
		private final Part part;
		
		PartButton( Part part, boolean selected) {
			this.part = part;
			this.setBorder(BorderFactory.createLineBorder((selected)?Color.blue:Color.black));
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			//IImage img = part.getImageHandle().deepAccess();
			
			float sx = 24f/(float)part.getImageHandle().getWidth();
			float sy = 24f/(float)part.getImageHandle().getHeight();
			
			float scale = Math.min( sx, sy);
			
			RawImage img2 = HybridHelper.createImage(24, 24);
			GraphicsContext gc = img2.getGraphics();
			gc.scale(scale, scale);
			gc.drawHandle(part.getImageHandle(), 0, 0);
			
			
			g.drawImage( ((ImageBI)HybridUtil.convert(img2, ImageBI.class)).img, 
					0, 0, null);
		}
	}
	

	private static final Font f = new Font("Tahoma", 0, 10);
	private class SubLabel extends JLabel { 
		SubLabel(String txt) {
			super(txt);
			this.setFont(f);
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
			changePartAttributes();
			super.onValueChanged(newValue);
		}
	}
	
	@Override
	public void requestFocus() {
		boxList.requestFocus();
	}
}