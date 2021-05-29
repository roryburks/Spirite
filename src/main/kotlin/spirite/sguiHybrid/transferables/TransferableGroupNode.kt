package spirite.sguiHybrid.transferables

import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.MovableGroupTree
import spirite.base.imageData.groupTree.duplicateInto
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


val SpiriteGroupNodeFlavor = DataFlavor(GroupNode::class.java, "SpiriteInternalImage")

interface INodeBuilder {
    fun buildInto(groupTree: MovableGroupTree)
    val width: Int
    val height: Int
}

class TransferableGroupNode(node: GroupNode) : Transferable {
    private val _node = node

    val builder get() = object : INodeBuilder {
        override fun buildInto(groupTree: MovableGroupTree) = groupTree.duplicateInto(_node)
        override val width: Int get() = 800  // TODO: Whatever.  Magic numbers.  who cares
        override val height: Int get() = 600
    }

    override fun getTransferData(flavor: DataFlavor?): Any {
        return when( flavor) {
            SpiriteGroupNodeFlavor -> builder
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = _dataFlavors.contains(flavor)
    override fun getTransferDataFlavors(): Array<DataFlavor> = _dataFlavors

    private val _dataFlavors = arrayOf( SpiriteGroupNodeFlavor)
}

