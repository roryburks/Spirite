package spirite.panel_layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.border.EtchedBorder;

import spirite.brains.MasterControl;
import spirite.brains.MasterControl.MWorkspaceObserver;
import spirite.brains.RenderEngine.LayerRenderSource;
import spirite.brains.RenderEngine.RenderSettings;
import spirite.image_data.GroupTree.LayerNode;
import spirite.image_data.GroupTree.Node;
import spirite.image_data.ImageWorkspace;
import spirite.image_data.ReferenceManager;
import spirite.image_data.ReferenceManager.MReferenceObserver;
import spirite.image_data.layers.Layer;
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
public class ReferenceSchemePanel extends OmniComponent 
	implements MWorkspaceObserver
{
	private final ReferenceListPanel referenceListPanel;
	private final JButton btn1 = new JButton();
	private final JButton btn2 = new JButton();
	final OpacitySlider opacitySlider = new OpacitySlider();
	
	private final MasterControl master;
	private ImageWorkspace workspace = null;
	private ReferenceManager refMan = null;
	
	
	public ReferenceSchemePanel(MasterControl master) {
		this.master = master;
		referenceListPanel = new ReferenceListPanel();
		initComponents();
		opacitySlider.setValue(1.0f);

		
		
		master.addWorkspaceObserver(this);
		
		workspace = master.getCurrentWorkspace();
		if( workspace != null) {
			refMan = master.getCurrentWorkspace().getReferenceManager();
			refMan.addReferenceObserve( referenceListPanel);
		}
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
						.addComponent(btn1, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
						.addComponent(btn2, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE))
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
	
	public class ReferenceListPanel extends JPanel 
		implements  ListCellRenderer<Layer>, MReferenceObserver
	{
	
		private final Color bgColor = new Color(64,64,64);
		private final Color bgColorActive = new Color(120,120,160);
		private final Color helperColor = new Color( 90,90,64);
		private final Color addOutlineColor = Color.black;//new Color( 160,160,111);
		
		private final JLabel helperLabel = new JLabel();
		private final JList<Layer> referenceList = new JList<>();
		private final DefaultListModel<Layer> model = new DefaultListModel<>();
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
		class RLPCellPanel extends JPanel {
			final JLabel label;
			final JPanel thumbnail;
			Layer layer;
			
			RLPCellPanel() {
				GroupLayout layout = new GroupLayout(this);
				final Dimension d = master.getSettingsManager().getThumbnailSize();

				label = new JLabel();
				thumbnail = new JPanel() {
					
					@Override
					protected void paintComponent(Graphics g) {
						super.paintComponent(g);
						
						if( workspace != null && layer != null) {
							RenderSettings settings = new RenderSettings(
									new LayerRenderSource(workspace,layer));
							settings.width = d.width;
							settings.height = d.height;
						
							BufferedImage bi = master.getRenderEngine().renderImage(settings);
							
							g.drawImage(bi, 0, 0, d.width, d.height, null);
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
				JList<? extends Layer> list, Layer value, int index,
				boolean isSelected, boolean cellHasFocus) 
		{
			renderComponent.layer = value;
			if( value == null) {
				renderComponent.label.setText("Base Image");
			}
			else  {
				renderComponent.label.setText("Layer: " + index);
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
				
				if( !evt.isDataFlavorSupported(LayerTreePanel.FLAVOR) && 
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
				if( refMan != null && dtde.isDataFlavorSupported(LayerTreePanel.FLAVOR)) {
					try {
						Node node = ((LayerTreePanel.NodeTransferable)dtde.getTransferable().getTransferData(LayerTreePanel.FLAVOR)).node;
						
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
				for( Layer layer : refMan.getFrontList()) {
					model.addElement(layer);
				}
				model.addElement(null);
				for( Layer layer : refMan.getBackList()) {
					model.addElement(layer);
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
	
	@Override
	public void onCleanup() {
//		referenceTreePanel.cleanup();
	}
}
