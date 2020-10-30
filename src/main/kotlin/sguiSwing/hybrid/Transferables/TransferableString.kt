package sguiSwing.hybrid.Transferables

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

//val StringDataFlavor = DataFlavor(String::class.java, "string")


class TransferableString(val str: String) : Transferable {
    override fun getTransferData(flavor: DataFlavor?): Any {
        return when( flavor) {
            DataFlavor.stringFlavor -> str
            else -> throw UnsupportedFlavorException(flavor)
        }
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?) = dataFlavors.contains(flavor)
    override fun getTransferDataFlavors(): Array<DataFlavor> = dataFlavors

    val dataFlavors = arrayOf( DataFlavor.stringFlavor)
}
