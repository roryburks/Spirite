package spirite.pc.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import spirite.base.image_data.GroupTree.Node;

public class Transferables {

	
	public static class NodeTransferable implements Transferable {
		public final Node node;
		public NodeTransferable( Node node) {
			this.node = node;
		}
		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if( flavor.equals(FLAVOR))	return this;
			else throw new UnsupportedFlavorException(flavor);
		}	
		@Override public DataFlavor[] getTransferDataFlavors() {return flavors;}
		@Override public boolean isDataFlavorSupported(DataFlavor flavor) {return flavor.equals(FLAVOR);}

		public static final DataFlavor FLAVOR = new DataFlavor( NodeTransferable.class, "Group Tree Node");
		public static final DataFlavor flavors[] = {FLAVOR};
	}
}
