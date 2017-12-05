package spirite.pc.ui.panel_layers;

import spirite.base.brains.MasterControl;
import spirite.base.brains.MasterControl.MWorkspaceObserver;
import spirite.base.graphics.IImage;
import spirite.base.graphics.RawImage;
import spirite.base.graphics.renderer.RenderEngine.RenderSettings;
import spirite.base.graphics.renderer.sources.LayerRenderSource;
import spirite.base.image_data.GroupTree.LayerNode;
import spirite.base.image_data.GroupTree.Node;
import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ReferenceManager;
import spirite.base.image_data.ReferenceManager.ImageReference;
import spirite.base.image_data.ReferenceManager.LayerReference;
import spirite.base.image_data.ReferenceManager.MReferenceObserver;
import spirite.base.image_data.ReferenceManager.Reference;
import spirite.base.image_data.layers.Layer;
import spirite.base.util.linear.Vec2;
import spirite.gui.hybrid.SButton;
import spirite.gui.hybrid.SPanel;
import spirite.hybrid.HybridUtil;
import spirite.pc.graphics.ImageBI;
import spirite.pc.ui.Transferables.NodeTransferable;
import spirite.pc.ui.components.SliderPanel;
import spirite.pc.ui.omni.OmniFrame.OmniComponent;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;

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
public class ReferenceSchemePanel extends SPanel
	implements OmniComponent, MWorkspaceObserver
{
	private final ReferenceListPanel referenceListPanel;
	private final SButton btnReset = new SButton();
	private final SButton btnLift = new SButton();
	final OpacitySlider opacitySlider = new OpacitySlider();
	
	private final MasterControl master;
	private ImageWorkspace workspace = null;
	private ReferenceManager refMan = null;
	
	
	public ReferenceSchemePanel(MasterControl master) {
		this.master = master;
		referenceListPanel = new ReferenceListPanel();
		
		initComponents();
		initLayout();
		initBindings();

		
		
		master.addWorkspaceObserver(this);
		
		workspace = master.getCurrentWorkspace();
		if( workspace != null) {
			refMan = master.getCurrentWorkspace().getReferenceManager();
			refMan.addReferenceObserve( referenceListPanel);
		}
	}
	
	private void initComponents() {
		opacitySlider.setValue(1.0f);
		btnReset.setToolTipText("Reset the reference transform.");
		btnLift.setToolTipText("Lift the current selection as a Reference.");
		
	}
	private void initBindings() {
		btnReset.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				master.executeCommandString("draw.reset_reference");
			}
		});
		btnLift.addActionListener( new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				master.executeCommandString("draw.lift_to_reference");
			}
		});
	}
	
	private void initLayout() {
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
							.addComponent(btnReset, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
							.addGap(1)
							.addComponent(btnLift, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE))
						.addComponent(referenceListPanel, GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE))
					.addGap(3))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
						.addGap(3)
					.addComponent(opacitySlider, 20, 20, 20)
					.addGap(0)
					.addComponent(referenceListPanel, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(btnReset, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnLift, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
					.addGap(16))
		);
		setLayout(groupLayout);
	}
	
	
	
	

	enum DragState {
		NOT_DRAGGING, OVER, UNDER, OUT
	};
	/** The OpacitySlider Swing Component */
	class OpacitySlider extends SliderPanel {
		OpacitySlider() {
			setMin(0.0f);
			setMax(1.0f);
			setLabel("Opacity: ");
		}
		
		public void refresh() {
			if( refMan != null)
				refMan.getRefAlpha();
		}
		
		@Override
		public void onValueChanged(float newValue) {
			if( refMan != null)
				refMan.setRefAlpha(getValue());
			super.onValueChanged(newValue);
		}
	}
	
	public class ReferenceListPanel extends SPanel 
		implements  ListCellRenderer<Reference>, MReferenceObserver
	{
	
		private final Color bgColor = new Color(64,64,64);
		private final Color bgColorActive = new Color(120,120,160);
		private final Color helperColor = new Color( 90,90,64);
		private final Color addOutlineColor = Color.black;//new Color( 160,160,111);
		
		private final JLabel helperLabel = new JLabel();
		private final JList<Reference> referenceList = new JList<>();
		private final DefaultListModel<Reference> model = new DefaultListModel<>();
		private final RLPDnDManager dndMan = new RLPDnDManager();
	
		public ReferenceListPanel() {
			setBackground(bgColor);
			
			initComponents();
			
			referenceList.setModel(model);
			referenceList.setCellRenderer( this);
			refreshList();
			
			// Init Drag-andDrop behavior
			this.setDropTarget(dndMan);
//			DragGestureRecognizer dgr = new 
		}

		private void initComponents() {
			GroupLayout layout = new GroupLayout(this);
			
			helperLabel.setText("<html><h2>Drag Layers here to add them to the reference section, drag out to remove.");
			helperLabel.setForeground(helperColor);
	
			layout.setVerticalGroup( layout.createParallelGroup()
					.addComponent(referenceList)
				.addGroup(layout.createParallelGroup(Alignment.CENTER)
					.addComponent(helperLabel, 0, 0, Short.MAX_VALUE)
				)
			);
			layout.setHorizontalGroup( layout.createParallelGroup()
					.addComponent(referenceList, 0, 0, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup(Alignment.CENTER)
						.addGap(0, 0, Short.MAX_VALUE)
						.addComponent(helperLabel, 100,100,100)
					)
			);
			this.setLayout(layout);
		}

		@Override
		public void paintComponent(Graphics g) {
			// It's generally not a good idea to Override paint instead of
			//	paintComponent, but this is the laziest way to get the lines 
			//	to appear over other components
			super.paintComponent(g);
			
			
			switch( dndMan.dragState) {
			case NOT_DRAGGING:
				break;
			case OUT:
				break;
			case OVER: {
				Rectangle rect = referenceList.getCellBounds(dndMan.dragToIndex, dndMan.dragToIndex);
				if( rect != null) {
					int dy = rect.y;
					g.setColor(addOutlineColor);
					g.drawLine(0, dy+1, getWidth(), dy+1);
					g.drawLine(0, dy, getWidth(), dy);
				}
				break;}
			case UNDER:{
				Rectangle rect = referenceList.getCellBounds(dndMan.dragToIndex, dndMan.dragToIndex);
				if( rect != null) {
					int dy = rect.y + rect.height - 1;
					g.setColor(addOutlineColor);
					g.drawLine(0, dy-1, getWidth(), dy-1);
					g.drawLine(0, dy, getWidth(), dy);
				}
				break;}
			default:
				break;
			
			}
		}
		
		/** List Cell Component Panel */
		class RLPCellPanel extends SPanel {
			final JLabel label;
			final SPanel thumbnail;
			Reference reference;
			
			RLPCellPanel() {
				GroupLayout layout = new GroupLayout(this);
				
				// TODO: Meh
				final Vec2 v2 = master.getSettingsManager().getThumbnailSize();
				final Dimension d = new Dimension((int)v2.x, (int)v2.y);

				label = new JLabel();
				thumbnail = new SPanel() {
					
					@Override
					protected void paintComponent(Graphics g) {
						super.paintComponent(g);
						
						if( workspace != null && reference != null ) {
							if( reference instanceof LayerReference) {
								RenderSettings settings = new RenderSettings(
										new LayerRenderSource(workspace,((LayerReference)reference).layer));
								settings.width = d.width;
								settings.height = d.height;
							
								RawImage bi = master.getRenderEngine().renderImage(settings);
								
								// TODO: MARK
								
								g.drawImage(((ImageBI)HybridUtil.convert(bi, ImageBI.class)).img, 0, 0, d.width, d.height, null);
							}
							else if( reference instanceof ImageReference){
								IImage raw = ((ImageReference) reference).image;
								
								BufferedImage bi = ((ImageBI)HybridUtil.convert(raw, ImageBI.class)).img;
								g.drawImage(bi, 
										0, 0, bi.getWidth(), bi.getHeight(),
										0, 0, d.width, d.height, null);
							}
						}
					}
				};
				thumbnail.setBorder( BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				this.setBorder( BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				
	
				layout.setVerticalGroup( layout.createParallelGroup()
					.addGroup( layout.createSequentialGroup()
						.addGap(2)
						.addComponent(thumbnail, d.height, d.height, d.height)
						.addGap(2)
					)
					.addComponent(label)
				);
				layout.setHorizontalGroup( layout.createSequentialGroup()
					.addGap(3)
					.addComponent(thumbnail, d.width, d.width, d.width)
					.addGap(3)
					.addComponent(label)
					.addGap(0, 0, Short.MAX_VALUE)
				);
				this.setLayout(layout);
			}
		}
		private final RLPCellPanel renderComponent = new RLPCellPanel();
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends Reference> list, Reference value, int index,
				boolean isSelected, boolean cellHasFocus) 
		{
			renderComponent.reference = value;
			if( value == null) {
				renderComponent.label.setText("Base Image");
			}
			else if( value instanceof LayerReference) {
				renderComponent.label.setText("Layer: " + index);
			}
			else if( value instanceof ImageReference){
				renderComponent.label.setText("Floating Selection: " + index);
			}
			return renderComponent;
		}

		/** Drag-and-drop target Manager. */
		private class RLPDnDManager extends DropTarget 
			implements DragGestureListener, DragSourceListener 
		{
			private final DragSource dragSource = DragSource.getDefaultDragSource();
//			private final DragGestureRecognizer dragGestureRecognizer;
			private int draggingIndex = -1;
			int dragToIndex;
			DragState dragState = DragState.NOT_DRAGGING;
			
			
			RLPDnDManager() {
				//dragGestureRecognizer = 
				dragSource.createDefaultDragGestureRecognizer(
						referenceList, DnDConstants.ACTION_COPY_OR_MOVE, this);
			}
			
			// :::: Internal
			private void setDragState( DragState newState, int newIndex) {
				if( dragState != newState || dragToIndex != newIndex) {
					dragState = newState;
					dragToIndex = newIndex;
					repaint();
				}
			}
			
			
			// :::: DropTargetListener inherited from DropTarget
			@Override
			public synchronized void dragOver(DropTargetDragEvent evt) {
				super.dragOver(evt);
				
				if( !evt.isDataFlavorSupported(NodeTransferable.FLAVOR) && 
					!evt.isDataFlavorSupported(REF_FLAVOR))
					return;
				
				if( ReferenceListPanel.this.contains(evt.getLocation())) {
					int index = referenceList.locationToIndex(evt.getLocation());
					Rectangle rect = referenceList.getCellBounds(index, index);
					
					if( evt.getLocation().y < rect.y + rect.height/2)
						setDragState(DragState.OVER,index);
					else
						setDragState(DragState.UNDER,index);
							
				}
				else {
					setDragState(DragState.OUT, 0);
				}
			}
			@Override
			public synchronized void dragExit(DropTargetEvent dte) {
				if( draggingIndex == -1)	{
					// Lazy way to determing if it's an internal drag.  Proper
					//	way would be using the transferable
					setDragState(DragState.NOT_DRAGGING,0);
				}
				else
					setDragState(DragState.OUT, 0);
				super.dragExit(dte);
			}
			
			
			// :::: DropTarget
			@Override
			public synchronized void drop(DropTargetDropEvent dtde) {
				if( refMan != null && dtde.isDataFlavorSupported(NodeTransferable.FLAVOR)) {
					try {
						Node node = ((NodeTransferable)dtde.getTransferable().getTransferData(NodeTransferable.FLAVOR)).node;
						
						if( node instanceof LayerNode) {
							Layer layer = ((LayerNode) node).getLayer();
							
							switch( dragState) {
							case OVER:
								refMan.addReference(layer, dragToIndex);
								break;
							case UNDER:
								refMan.addReference(layer, dragToIndex+1);
								break;
							default:
							}
						}
						
					} catch (UnsupportedFlavorException | IOException e) {}
				}
				if( dtde.isDataFlavorSupported(REF_FLAVOR)) {
					if( dragState == DragState.OVER) {
						if( refMan!=null && draggingIndex != dragToIndex)
							refMan.moveReference(draggingIndex, dragToIndex);
					}
					else if( dragState == DragState.UNDER){
						if( refMan!=null && draggingIndex != dragToIndex)
							refMan.moveReference(draggingIndex, dragToIndex+1);
					}
				}
				

				setDragState(DragState.NOT_DRAGGING,0);
				draggingIndex = -1;
				super.drop(dtde);
			}
			@Override
			public synchronized void dragEnter(DropTargetDragEvent evt) {
				super.dragEnter(evt);
			}
			
			// :::: DragGestureRecognizer
			@Override
			public void dragGestureRecognized(DragGestureEvent evt) {
				int index = referenceList.locationToIndex(evt.getDragOrigin());
				if( index == -1) return;
				
				draggingIndex = index;
				dragSource.startDrag(
						evt, 
						DragSource.DefaultMoveDrop, 
						new RefTransferable(), 
						this);
			}

			//  :::: DragSourceListener
			@Override
			public void dragDropEnd(DragSourceDropEvent evt) {
				if( dragState == DragState.OUT) {
					if( refMan!=null && model.getElementAt(draggingIndex) != null)
						refMan.removeReferenceAt(draggingIndex);
				}
				
				setDragState(DragState.NOT_DRAGGING,0);
				draggingIndex = -1;
			}

			@Override public void dragEnter(DragSourceDragEvent arg0) {}
			@Override public void dragExit(DragSourceEvent arg0) {}
			@Override public void dragOver(DragSourceDragEvent arg0) {}
			@Override public void dropActionChanged(DragSourceDragEvent arg0) {}
		}

		/** Empty Transferable that just makes sure RLPDnDManager only accepts drags
		 * from itself.  The data of what's being dragged is stored by RLPDnDManager;
		 * there's no need to bother encapsulating and unwrapping the data in a 
		 * transferable since it's all self-to-self; */
		public class RefTransferable implements Transferable {
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if( flavor.equals(REF_FLAVOR))	return this;
				else throw new UnsupportedFlavorException(flavor);
			}	
			@Override public DataFlavor[] getTransferDataFlavors() {return flavors;}
			@Override public boolean isDataFlavorSupported(DataFlavor flavor) {return flavor.equals(REF_FLAVOR);}
		}
		final DataFlavor REF_FLAVOR = new DataFlavor( RefTransferable.class, "Local RefList Move");
		final DataFlavor flavors[] = {REF_FLAVOR};
		
		
		/** Re-builds the list from Refrence List data*/
		void refreshList() {
			model.clear();
			if( refMan != null) {
				for( Reference ref : refMan.getList(true)) {
					model.addElement(ref);
				}
				model.addElement(null);
				for( Reference ref : refMan.getList(false)) {
					model.addElement(ref);
				}
			}
		}

		// MReferenceObserver
		@Override
		public void referenceStructureChanged(boolean hard) {
			if( hard) 
				refreshList();
			opacitySlider.refresh();
		}

		@Override
		public void toggleReference(boolean referenceMode) {
			setBackground( referenceMode ? bgColorActive : bgColor);
		}
	}


	// MWorkspaceObserver
	@Override
	public void currentWorkspaceChanged(ImageWorkspace selected, ImageWorkspace previous) {
		if( workspace != null) {
			refMan.removeReferenceObserve(referenceListPanel);
		}
		workspace = selected;
		if( workspace != null) {
			refMan = workspace.getReferenceManager();
			refMan.addReferenceObserve(referenceListPanel);
		}
		else 
			refMan = null;
		
		opacitySlider.refresh();
		referenceListPanel.refreshList();
	}

	@Override	public void newWorkspace(ImageWorkspace newWorkspace) {}
	@Override	public void removeWorkspace(ImageWorkspace newWorkspace) {}
	
	// :::: OmniComponent
	@Override
	public void onCleanup() {
//		referenceTreePanel.cleanup();
	}
	@Override public JComponent getComponent() {
		return this;
	}
}
