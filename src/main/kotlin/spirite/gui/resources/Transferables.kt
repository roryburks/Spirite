package spirite.gui.resources

import spirite.base.imageData.groupTree.GroupTree.Node
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

object Transferables {
    class NodeTransferable(val node: Node) : Transferable {
        override fun getTransferData(flavor: DataFlavor) =  when(flavor) {
            FLAVOR -> node
            else -> throw UnsupportedFlavorException(flavor)
        }

        override fun getTransferDataFlavors() = flavors
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.equals(FLAVOR)

        companion object {
            val FLAVOR = DataFlavor(NodeTransferable::class.java, "Group Tree Node")
            val flavors = arrayOf(FLAVOR)
        }
    }

}