package spirite.panel_layers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import spirite.MDebug;
import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.UndoEngine.UndoableAction;
import spirite.image_data.layers.RigLayer;
import spirite.image_data.layers.RigLayer.Part;
import spirite.image_data.layers.RigLayer.RigStructureObserver;
import spirite.ui.OmniFrame.OmniComponent;
import spirite.ui.components.MTextFieldNumber;

public class RigPanel extends OmniComponent 
	implements MWorkspaceObserver, MSelectionObserver, ActionListener,
		ListSelectionListener, RigStructureObserver, DocumentListener 
{
	private final JList<Part> listPanel = new JList<Part>();
	private final MTextFieldNumber tfOffsetX = new MTextFieldNumber(true,false);
	private final MTextFieldNumber tfOffsetY = new MTextFieldNumber(true,false);
	private final MTextFieldNumber tfDepth = new MTextFieldNumber(true,false);
	private final JTextField tfName= new JTextField();
	private final JButton bNewPart = new JButton();
	private final JButton bRemovePart = new JButton();
	private final DefaultListModel<Part> model = new DefaultListModel<>();
	
	private final MasterControl master;
	private ImageWorkspace workspace;
	private RigLayer rig;
	
	public RigPanel( MasterControl master) {
		this.master = master;
		initComponents();
		
		listPanel.setModel(model);
		master.addWorkspaceObserver(this);
		
		workspace = master.getCurrentWorkspace();
		if( workspace != null)
			workspace.addSelectionObserver(this);
		
		listPanel.addListSelectionListener(this);
		
		refresh();
	}
	
	private void initComponents() {
		GroupLayout layout = new GroupLayout(this);
		
		JLabel jOffsetX = new JLabel("X:");
		JLabel jOffsetY = new JLabel("Y:");
		JLabel jOffsetD = new JLabel("Depth:");
		JLabel jOffsetT = new JLabel("Type:");
		JScrollPane jscroll = new JScrollPane(listPanel);

		bNewPart.setToolTipText("Create New Part");
		bRemovePart.setToolTipText("Remove Selected Part");
		bNewPart.setActionCommand("newPart");
		bRemovePart.setActionCommand("remPart");
		bNewPart.addActionListener(this);
		bRemovePart.addActionListener(this);

		tfOffsetX.getDocument().addDocumentListener(this);
		tfOffsetY.getDocument().addDocumentListener(this);
		tfDepth.getDocument().addDocumentListener(this);
		tfName.getDocument().addDocumentListener(this);
		
		Dimension bSize = new Dimension( 24,16);
		
		layout.setVerticalGroup( layout.createSequentialGroup()
				.addContainerGap()
			.addComponent(jscroll, 0, 200, Short.MAX_VALUE)
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				.addComponent(bNewPart, bSize.height, bSize.height, bSize.height)
				.addComponent(bRemovePart, bSize.height, bSize.height, bSize.height)
			)
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				.addComponent(jOffsetT)
				.addComponent(tfName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addGroup( layout.createParallelGroup()
				.addComponent(jOffsetX)
				.addComponent(tfOffsetX, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addGroup( layout.createParallelGroup()
				.addComponent(jOffsetY)
				.addComponent(tfOffsetY, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addGroup( layout.createParallelGroup()
				.addComponent(jOffsetD)
				.addComponent(tfDepth, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			)
			.addContainerGap()
		);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addGap(3)
			.addGroup( layout.createParallelGroup()
				.addComponent(jscroll, 0, 200, Short.MAX_VALUE)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup()
						.addComponent(jOffsetX)
						.addComponent(jOffsetY)
						.addComponent(jOffsetD)
					)
					.addGap(10)
					.addGroup(layout.createParallelGroup()
						.addComponent(tfOffsetX, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
						.addComponent(tfOffsetY, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
						.addComponent(tfDepth, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
					)
					.addContainerGap()
				)
				.addGroup(layout.createSequentialGroup()
					.addComponent(jOffsetT)
					.addGap(10)
					.addComponent(tfName, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
				)
				.addGroup(layout.createSequentialGroup()
					.addGap(0,0,Short.MAX_VALUE)
					.addComponent(bNewPart, bSize.width, bSize.width, bSize.width)
					.addGap(3)
					.addComponent(bRemovePart, bSize.width, bSize.width, bSize.width)
					.addGap(3)
				)
			)
			.addGap(3)
		);
		this.setLayout(layout);
		
	}
	
	
	private void setRig( RigLayer setTo) {
		if( rig != null) 
			rig.removeRigObserver(this);
		
		rig = setTo;
		
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
				tfOffsetX.setText(""+part.getOffsetX());
				tfOffsetY.setText(""+part.getOffsetY());
			}
		}

		bNewPart.setEnabled(!(rig==null));
		bRemovePart.setEnabled(!(rig==null));
		tfDepth.setEnabled(!(rig==null));
		tfName.setEnabled(!(rig==null));
		tfOffsetX.setEnabled(!(rig==null));
		tfOffsetY.setEnabled(!(rig==null));
		listPanel.setEnabled(!(rig==null));
		
		Color c = (rig == null) ? cDisabled : cEnabled;

		listPanel.setBackground(c);
		tfDepth.setBackground(c);
		tfName.setBackground(c);
		tfOffsetX.setBackground(c);
		tfOffsetY.setBackground(c);
		
		

		if( rig != null)
			listPanel.setSelectedIndex(model.indexOf(rig.getActivePart()));
		
		building = false;

	}

	// :::: OmniComponent
	@Override
	public void onCleanup() {
		master.removeWorkspaceObserver(this);
	}

	// :::: MWorkspaceObserver
	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		setRig(null);
		if( workspace != null) {
			workspace.removeSelectionObserver(this);
		}
		workspace = selected;
		if( workspace != null) {
			workspace.addSelectionObserver(this);
		}
	}	

	// :::: MWorkspaceObserver
	@Override
	public void selectionChanged(Node newSelection) {
		if( newSelection instanceof LayerNode) {
			LayerNode ln = (LayerNode)newSelection;

			if( ln.getLayer() instanceof RigLayer) {
				setRig( (RigLayer)ln.getLayer());
				return;
			}
		}
		setRig(null);
	}

	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		switch( evt.getActionCommand()) {
		case "newPart":
			if( rig != null) {
				BufferedImage bi = new BufferedImage( 100,100, BufferedImage.TYPE_INT_ARGB);
				
				workspace.getUndoEngine().performAndStore(
						rig.createAddPartAction(bi, 0, 0, 0,""));
			}
			break;
		case "remPart":
			if( rig != null && rig.getActivePart() != null) {
				workspace.getUndoEngine().performAndStore(
						rig.createRemovePartAction(rig.getActivePart()));
			}
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
				building = true;
				if( rig == null) return;
				ListSelectionModel model =  listPanel.getSelectionModel();
				int i = model.getMinSelectionIndex();
				Rectangle rect = listPanel.getCellBounds(i,i);
				if( rect != null)
					listPanel.scrollRectToVisible( rect);
				
				Part part = RigPanel.this.model.getElementAt(i);
				if( rig.getActivePart() != part) {
					rig.setActivePart(part);
				}

				tfDepth.setText(""+part.getDepth());
				tfName.setText(part.getTypeName());
				tfOffsetX.setText(""+part.getOffsetX());
				tfOffsetY.setText(""+part.getOffsetY());
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
			if( part.getOffsetX() != tfOffsetX.getNumber() ||
				part.getOffsetY() != tfOffsetY.getNumber() ||
				part.getDepth() != tfDepth.getNumber() ||
				!part.getTypeName().equals(tfName.getText())) 
			{
				
				UndoableAction action = rig.createModifyPartAction(
						part, tfOffsetX.getNumber(), tfOffsetY.getNumber(), 
						tfDepth.getNumber(), tfName.getText());
				
				if( action != null)
					workspace.getUndoEngine().performAndStore(action);
			}
		}
		building = false;
		
	}
}