package spirite.panel_layers;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.security.acl.Group;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ImageWorkspace.MSelectionObserver;
import spirite.image_data.layers.RigLayer;
import spirite.image_data.layers.RigLayer.Part;
import spirite.ui.OmniFrame.OmniComponent;
import spirite.ui.components.MTextFieldNumber;

public class RigPanel extends OmniComponent 
	implements MWorkspaceObserver, MSelectionObserver, ActionListener 
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
		
		refresh();
	}
	
	private void initComponents() {
		GroupLayout layout = new GroupLayout(this);
		
		JLabel jOffsetX = new JLabel("X:");
		JLabel jOffsetY = new JLabel("Y:");
		JLabel jOffsetD = new JLabel("Depth:");
		JLabel jOffsetT = new JLabel("Type:");

		bNewPart.setToolTipText("Create New Part");
		bRemovePart.setToolTipText("Remove Selected Part");
		bNewPart.setActionCommand("newPart");
		bRemovePart.setActionCommand("remPart");
		bNewPart.addActionListener(this);
		bRemovePart.addActionListener(this);
		
		Dimension bSize = new Dimension( 24,16);
		
		layout.setVerticalGroup( layout.createSequentialGroup()
				.addContainerGap()
			.addComponent(listPanel, 0, 200, Short.MAX_VALUE)
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
				.addComponent(listPanel, 0, 200, Short.MAX_VALUE)
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
		rig = setTo;
		
		
		refresh();
	}
	private void refresh() {
		model.clear();
		
		if( rig == null) {
//			bNewPart.setEnabled(false);
	//		bRemovePart.setEnabled(false);
		}
		else {
			bNewPart.setEnabled(true);
			bRemovePart.setEnabled(true);
			
			for( Part part : rig.getParts()) {
				model.addElement(part);
			}
		}
		
		
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
		rig = null;
		if( workspace != null)
			workspace.removeSelectionObserver(this);
		workspace = selected;
		if( workspace != null)
			workspace.addSelectionObserver(this);
	}	

	// :::: MWorkspaceObserver
	@Override
	public void selectionChanged(Node newSelection) {

		System.out.println(":");
		if( newSelection instanceof LayerNode) {
			LayerNode ln = (LayerNode)newSelection;

			System.out.println("::");
			if( ln.getLayer() instanceof RigLayer) {
				setRig( (RigLayer)ln.getLayer());
			}
		}
	}

	// :::: ActionListener
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		switch( evt.getActionCommand()) {
		case "newPart":
			if( rig != null) {
				BufferedImage bi = new BufferedImage( 100,100, BufferedImage.TYPE_INT_ARGB);
				
				workspace.getUndoEngine().performAndStore(
						rig.createAddPartAction(bi, 0, 0, 0));
			}
			break;
		case "remPart":
			break;
		default:
			System.out.println(evt.getActionCommand());
		}
		
	}
}