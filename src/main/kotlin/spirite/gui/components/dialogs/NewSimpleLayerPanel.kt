package spirite.gui.components.dialogs

import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.Color
import spirite.base.util.Colors
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid

class NewSimpleLayerPanel() : ICrossPanel by Hybrid.ui.CrossPanel()
{
    data class NewSimpleLayerReturn(
            val width: Int,
            val height: Int,
            val color: Color,
            val name: String,
            val mediumType: MediumType)

    private val comboBox = Hybrid.ui.ComboBox( MediumType.creatableTypes)

    init {
        setLayout {
            rows += {
                add(Hybrid.ui.Label("Type: "))
                addGap(4)
                add(comboBox)
            }
        }
    }

    val result : NewSimpleLayerReturn get() =
        NewSimpleLayerReturn(0, 0, Colors.BLUE, "0", comboBox.selectedItem ?: FLAT)
}