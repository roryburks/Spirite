package sguiSwing.transfer

import sgui.transfer.ITransferObject
import sgui.transfer.StringTransferObject
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

object SwTransferObjectConverter {
    fun convert( transferable: Transferable) : ITransferObject = when(transferable) {
        is ConvertedITransferObject -> transferable.transObject
        else -> ConvertedTransferable(transferable)
    }

    fun convert(transObject: ITransferObject) : Transferable = when( transObject) {
        is ConvertedTransferable -> transObject.transferable
        else -> ConvertedITransferObject(transObject)
    }

    private class ConvertedITransferObject(val transObject: ITransferObject) : Transferable {
        override fun getTransferData(flavor: DataFlavor?): Any? {
            val key = map(flavor) ?: return null

            return transObject.getData(key)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
            val key = map(flavor) ?: return false

            return transObject.dataTypes.contains(key)
        }

        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return transObject.dataTypes
                    .map { map(it) }
                    .toTypedArray()
        }
    }

    private class ConvertedTransferable(val transferable : Transferable) : ITransferObject {
        override val dataTypes: Set<String>
            get() = transferable.transferDataFlavors
                    .mapNotNull { map(it) }
                    .toSet()

        override fun getData(type: String): Any? {
            val dataFlavor = map(type)
            return transferable.getTransferData(dataFlavor)
        }
    }


    private fun map( key: String) : DataFlavor {
        return when( key) {
            StringTransferObject.Key -> DataFlavor.stringFlavor
            else -> DataFlavor(SwTransferObjectConverter::class.java,"$Prefix$key")
        }
    }

    private fun map( flavor: DataFlavor?) : String? = when {
        flavor == DataFlavor.stringFlavor -> StringTransferObject.Key
        flavor?.humanPresentableName?.startsWith(Prefix) == true -> flavor.humanPresentableName.substring(Prefix.length)
        else -> null
    }

    private const val Prefix = "SGuiInternal:"
}




