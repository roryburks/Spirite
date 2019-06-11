package spirite.gui.components.dialogs

import spirite.base.brains.IMasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.mediums.MediumType
import spirite.base.imageData.mediums.MediumType.FLAT
import rb.glow.color.Color
import rb.glow.color.Colors
import sgui.generic.Orientation.HORIZONTAL
import sgui.generic.Orientation.VERTICAL
import sgui.generic.components.ICrossPanel
import spirite.gui.components.dialogs.NewSimpleLayerPanel.NewSimpleLayerReturn
import spirite.hybrid.Hybrid

class NewSimpleLayerPanel(
        private val master: IMasterControl,
        workspace: IImageWorkspace) : ICrossPanel by Hybrid.ui.CrossPanel(), IDialogPanel<NewSimpleLayerReturn>
{
    data class NewSimpleLayerReturn(
            val width: Int,
            val height: Int,
            val color: Color,
            val name: String,
            val mediumType: MediumType)

    private val MAX_DIM get() = master.settingsManager.MaxDimension

    private val comboBox = Hybrid.ui.ComboBox( MediumType.creatableTypes)
    private val widthField = Hybrid.ui.IntField(1, MAX_DIM, false)
    private val heightField = Hybrid.ui.IntField(1, MAX_DIM, false)
    private val nameField = Hybrid.ui.TextField()

    private var activeColorField = 1
    private val cfields = arrayOf(
            Hybrid.ui.ColorSquare(master.paletteManager.activeBelt.getColor(0)),
            Hybrid.ui.ColorSquare(master.paletteManager.activeBelt.getColor(1)),
            Hybrid.ui.ColorSquare(Colors.TRANSPARENT),
            Hybrid.ui.ColorSquare(Colors.BLACK))

    fun repaintColors(acf: Int) {
        activeColorField = acf
        //cfields.forEach { redraw() }
        cfields[activeColorField].redraw()
        cfields[3].color = cfields[activeColorField].color
    }

    init {
        widthField.value = workspace.width
        heightField.value = workspace.height
        nameField.text = "Layer"

        cfields.forEach { it.active = false }
        cfields.forEachIndexed { ind, cfiedl -> cfiedl.onMouseClick += {repaintColors(ind)} }
        cfields[3].active = true

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
            rows.addGap(2)
            rows.add(Hybrid.ui.Label("Select Background Color:"))
            rows.addGap(2)
            rows += {
                this += {
                    addGap(2)
                    add(Hybrid.ui.Label("Palette Colors:"), 120)
                    addGap(2)
                    addFlatGroup (20) {
                        addGap(20)
                        add(cfields[2], width = 20, height =  20)
                    }
                    addFlatGroup (10) {
                        addGap(10)
                        add(cfields[1], width = 20, height =  20)
                    }
                    addFlatGroup {
                        addGap(0)
                        add(cfields[0], width = 20, height =  20)
                    }
                    flex = 150f
                }
                add(Hybrid.ui.Separator(VERTICAL))
                this += {
                    addGap(2)
                    add(Hybrid.ui.Label("Custom Color:"), 120)
                    addGap(2)
                    this += {
                        addGap(40)
                        add(cfields[3], width = 20, height =  20)
                    }
                    flex = 150f
                }
            }
        }
    }

    override val result : NewSimpleLayerReturn get() =
        NewSimpleLayerReturn(
                widthField.value,
                heightField.value,
                cfields[activeColorField].color,
                nameField.text,
                comboBox.selectedItem ?: FLAT)
}