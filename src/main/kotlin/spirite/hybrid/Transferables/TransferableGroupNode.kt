package spirite.hybrid.Transferables

import spirite.base.graphics.IImage
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.GroupNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.Layer
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


val SpiriteGNodeFlavor = DataFlavor(IImage::class.java, "SpiriteInternalImage")


class TransferableSpiriteNode(node: Node) : Transferable {
    private val _node = node

    override fun getTransferData(flavor: DataFlavor?): Any {
        return when( flavor) {
            SpiriteGNodeFlavor -> _node♥
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = _dataFlavors.contains(flavor)
    override fun getTransferDataFlavors(): Array<DataFlavor> = _dataFlavors

    private val _dataFlavors = arrayOf( SpiriteGNodeFlavor)
}