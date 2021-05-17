package spirite.gui.resources

import sgui.core.transfer.ITransferObject
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.groupTree.GroupTree.Node
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

object Transferables {
    class NodeTransferable(val node: Node) : Transferable {
        override fun getTransferData(flavor: DataFlavor) =  when(flavor) {
            FLAVOR -> node
            else -> throw UnsupportedFlavorException(flavor)
        }

        override fun getTransferDataFlavors() = flavors
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.equals(FLAVOR)

        companion object {
            val FLAVOR = DataFlavor(NodeTransferable::class.java, "Group Tree GroupNode")
            val flavors = arrayOf(FLAVOR)
        }
    }

    class NodeTransferObject(val node: Node) : ITransferObject {
        override val dataTypes: Set<String> get() = setOf(Key)

        override fun getData(type: String) = when(type) {
            Key -> node
            else -> null
        }

        companion object {
            const val Key = "SpiriteNode"
        }
    }

    class AnimationTransferable(val anim: Animation) : Transferable {
        override fun getTransferData(flavor: DataFlavor) =  when(flavor) {
            FLAVOR -> anim
            else -> throw UnsupportedFlavorException(flavor)
        }

        override fun getTransferDataFlavors() = flavors
        override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.equals(FLAVOR)

        companion object {
            val FLAVOR = DataFlavor(AnimationTransferable::class.java, "Spirite Animation")
            val flavors = arrayOf(FLAVOR)
        }
    }
}