package spirite.gui.components.dialogs

import spirite.base.imageData.mediums.IMedium.MediumType
import spirite.base.imageData.mediums.IMedium.MediumType.FLAT
import spirite.base.util.Color
import spirite.base.util.Colors
import spirite.gui.Orientation.HORIZONTAL
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid

private val MAX_DIM = 16000

class NewSimpleLayerPanel() : ICrossPanel by Hybrid.ui.CrossPanel()
{
    data class NewSimpleLayerReturn(
            val width: Int,
            val height: Int,
            val color: Color,
            val name: String,
            val mediumType: MediumType)

    private val comboBox = Hybrid.ui.ComboBox( MediumType.creatableTypes)
    private val widthField = Hybrid.ui.IntField(1, MAX_DIM, false)
    private val heightField = Hybrid.ui.IntField(1, MAX_DIM, false)
    private val nameField = Hybrid.ui.TextField()

    init {
        setLayout {
            rows += {
                add(Hybrid.ui.Label("Type: "))
                addGap(4)
                add(comboBox)
            }
            rows.addGap(2)
            rows.add( Hybrid.ui.Separator(HORIZONTAL), width = 300)
            rows.addGap(2)
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Name:"), 120)
                add(nameField, width = 120)
            }
            rows.addGap(2)
            rows.add( Hybrid.ui.Separator(HORIZONTAL), width = 300)
            rows.addGap(2)
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Width:"), 120)
                add(widthField, width = 120)
            }
            rows.addGap(2)
            rows += {
                addGap(20)
                add(Hybrid.ui.Label("Height:"), 120)
                add(heightField, width = 120)
            }
            rows.addGap(2)
            rows.add( Hybrid.ui.Separator(HORIZONTAL), width = 300)
        }
    }

    val result : NewSimpleLayerReturn get() =
        NewSimpleLayerReturn(
                widthField.width,
                heightField.height,
                Colors.BLUE,
                nameField.text,
                comboBox.selectedItem ?: FLAT)
}