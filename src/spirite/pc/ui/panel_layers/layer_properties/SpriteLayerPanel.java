package spirite.pc.ui.panel_layers.layer_properties;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import spirite.base.brains.MasterControl;
import spirite.base.graphics.RawImage;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.layers.SpriteLayer;
import spirite.base.image_data.layers.SpriteLayer.Part;
import spirite.base.image_data.layers.SpriteLayer.PartStructure;
import spirite.base.image_data.layers.SpriteLayer.RigStructureObserver;
import spirite.hybrid.Globals;
import spirite.hybrid.HybridHelper;
import spirite.hybrid.MDebug;
import spirite.pc.ui.components.MTextFieldNumber;
import spirite.pc.ui.components.SliderPanel;

public class SpriteLayerPanel extends JPanel 
	implements 
		ActionListener,
		ListSelectionListener, 
		RigStructureObserver, 
		DocumentListener 
{
	private final JList<Part> listPanel = new JList<Part>();
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
	private final DefaultListModel<Part> model = new DefaultListModel<>();
	private final OpacitySlider opacitySlider = new OpacitySlider();
	
	private final MasterControl master;
	private ImageWorkspace workspace;
	private SpriteLayer rig;
	
	public SpriteLayerPanel( MasterControl master) {
		this.master = master;
		initComponents();
		
		listPanel.setModel(model);
		
		listPanel.addListSelectionListener(this);
		listPanel.setCellRenderer(renderer);
		
		refresh();
	}
	
	
	private final ListCellRenderer<Part> renderer = new RigCellRender();
			
	class RigCellRender implements ListCellRenderer<SpriteLayer.Part> {
		private JPanel renderPanel = new JPanel();
		private JLabel label = new JLabel();
		private Color bgColor = new Color( 255,255,255);
		private Color selColor = new Color( 90,90,160);
		
		
		RigCellRender() {
			GroupLayout layout = new GroupLayout(renderPanel);
			
			layout.setVerticalGroup( layout.createSequentialGroup()
				.addComponent(label,16,16,16)
			);
			layout.setHorizontalGroup( layout.createSequentialGroup()
				.addComponent(label)
			);
			
			renderPanel.setLayout(layout);
			
		}
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends Part> list, 
				Part value, 
				int index, 
				boolean isSelected,
				boolean cellHasFocus)
		{
			label.setText(value.getTypeName());
			renderPanel.setBackground(isSelected?selColor:bgColor);
			return renderPanel;
		}
	};
	
	private static final Font f = new Font("Tahoma", 0, 10);
	private class SubLabel extends JLabel { SubLabel(String txt) {super(txt);this.setFont(f);}}
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
		JScrollPane jscroll = new JScrollPane(listPanel);

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
			.addComponent(jscroll, 0, 200, Short.MAX_VALUE)
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
				.addComponent(jscroll, 0, 200, Short.MAX_VALUE)
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
		
//		layout.linkSize( tfTransX, tfTransY, tfScaleX, tfTransY);
		this.setLayout(layout);
		
	}
	
	class OpacitySlider extends SliderPanel {
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
		model.clear();
		
		if( rig != null) {
			bNewPart.setEnabled(true);
			bRemovePart.setEnabled(true);
			
			for( Part part : rig.getParts()) {
				model.addElement(part);
			}
			

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
		listPanel.setEnabled(!(rig==null));
		opacitySlider.setEnabled(!(rig==null));
		
		Color c = (rig == null) ? cDisabled : cEnabled;

		listPanel.setBackground(c);
		tfDepth.setBackground(c);
		tfName.setBackground(c);
		tfTransX.setBackground(c);
		tfTransY.setBackground(c);
		tfScaleX.setBackground(c);
		tfScaleY.setBackground(c);
		tfRot.setBackground(c);
		
		

		if( rig != null)
			listPanel.setSelectedIndex(model.indexOf(rig.getActivePart()));
		
		building = false;

	}

	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		switch( evt.getActionCommand()) {
		case "newPart":
			if( rig != null) {
				RawImage img = HybridHelper.createImage(1, 1);
				
				workspace.getUndoEngine().performAndStore(
						rig.createAddPartAction(img, 0, 0, 0,""));
			}
			break;
		case "remPart":
			if( rig != null && rig.getActivePart() != null) {
				workspace.getUndoEngine().performAndStore(
						rig.createRemovePartAction(rig.getActivePart()));
			}
			break;
		case "toggleVis":
			changePartAttributes();
			break;
		default:
			MDebug.log(evt.getActionCommand());
		}
		
	}


	// ::: ListSelectionListener
	@Override
	public void valueChanged(ListSelectionEvent evt) {
		if( building ) return;
		
		// Compared to other Listeners, ListSelectionListeners seem really loosey goosey
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				if( rig == null) return;
				building = true;
				ListSelectionModel model =  listPanel.getSelectionModel();
				int i = model.getMinSelectionIndex();
				Rectangle rect = listPanel.getCellBounds(i,i);
				if( rect != null)
					listPanel.scrollRectToVisible( rect);
				
					Part part = SpriteLayerPanel.this.model.getElementAt(i);
				if( rig.getActivePart() != part) {
					rig.setActivePart(part);
				}

				tfDepth.setText(""+part.getDepth());
				tfName.setText(part.getTypeName());
				tfTransX.setText(""+part.getTranslationX());
				tfTransY.setText(""+part.getTranslationY());
				tfScaleX.setText(""+part.getScaleX());
				tfScaleY.setText(""+part.getScaleY());
				tfRot.setText(""+part.getRotation());
				building = false;
			}
		});
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
				
				UndoableAction action = rig.createModifyPartAction( part,ps);
				
				if( action != null)
					workspace.getUndoEngine().performAndStore(action);
			}
		}
		building = false;
		
	}
}