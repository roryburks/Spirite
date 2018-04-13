package spirite.hybrid.Transferables

import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException


class TransferableImage(val image: Image) : Transferable {
    override fun getTransferData(flavor: DataFlavor?): Any {
        if( flavor == DataFlavor.imageFlavor)
            return image
        else
            throw UnsupportedFlavorException(flavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = flavor == DataFlavor.imageFlavor
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
}