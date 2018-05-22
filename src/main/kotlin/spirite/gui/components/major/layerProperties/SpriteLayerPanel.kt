package spirite.gui.components.major.layerProperties

import spirite.base.brains.Bindable
import spirite.base.brains.IMasterControl
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.gui.components.basic.IBoxList.IBoxComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid

class SpriteLayerPanel(master: IMasterControl) : ICrossPanel by Hybrid.ui.CrossPanel()
{
    var linkedSprite : SpriteLayer? = null
        set(value) {
            val old = field
            if( value != old) {
                old?.partChangeObserver?.removeObserver(onPartChange)
                activePartBind.unbind()
                activePart = null
                boxList.clear()

                if( value != null) {
                    value.activePartBind.bindWeakly(activePartBind)
                    boxList.addAll( value.parts)
                    value.partChangeObserver.addObserver(onPartChange)
                }
            }
        }

    val onPartChange = {
        doLocked {
            val part = activePart
            tfTransX.value = part?.transX ?: 0f
            tfTransY.value = part?.transY ?: 0f
            tfScaleX.value = part?.scaleX ?: 1f
            tfScaleY.value = part?.scaleY ?: 1f
            tfRot.value = part?.rot ?: 0f
            println(part?.rot)
        }
    }
    private fun update

    val activePartBind = Bindable<SpritePart?>(null) { new, old ->
        println("do : $new")
        this.onPartChange.invoke()
    }
    var activePart: SpritePart? by activePartBind

    val boxList = Hybrid.ui.BoxList<SpritePart>(24, 24)
    val tfTransX = Hybrid.ui.FloatField().also { it.valueBind.addListener { new, old ->  doLocked { activePart?.transX = new}} }
    val tfTransY = Hybrid.ui.FloatField().also { it.valueBind.addListener { new, old ->  doLocked { activePart?.transY = new}} }
    val tfScaleX =  Hybrid.ui.FloatField().also { it.valueBind.addListener { new, old ->  doLocked { activePart?.scaleX = new}} }
    val tfScaleY = Hybrid.ui.FloatField().also { it.valueBind.addListener { new, old ->  doLocked { activePart?.scaleY = new} }}
    val tfRot = Hybrid.ui.FloatField().also { it.valueBind.addListener { new, old ->  doLocked { activePart?.rot = new} }}
    val tfDepth = Hybrid.ui.IntField(allowsNegative = true).also { it.valueBind.addListener { new, old ->  doLocked { activePart?.depth = new} }}
    val tfType = Hybrid.ui.TextField().also { it.textBind.addListener { new, old ->  doLocked { activePart?.partName = new} }}

    var lock = false
    inline fun <T> T.doLocked(block: (T) -> Unit): T {
        if(!lock) {
            lock = true
            block(this)
            lock = false
        }
        return this
    }


    val btnNewPart = Hybrid.ui.Button()
    val btnRemovePart = Hybrid.ui.Button()
    val btnVisibility = Hybrid.ui.ToggleButton()

    val opacitySlider = Hybrid.ui.GradientSlider(0f,1f, "Opacity")

    init {
        boxList.renderer = {part ->
            object : IBoxComponent {
                override val component: IComponent
                    get() = Hybrid.ui.Label(part.partName)

                override fun setSelected(selected: Boolean) {
                    if( selected)
                        activePart = part
                }
            }
        }

        btnVisibility.setOnIcon(SwIcons.SmallIcons.Rig_VisibileOn)
        btnVisibility.setOffIcon(SwIcons.SmallIcons.Rig_VisibleOff)

        setLayout {
            rows += {
                add(boxList, flex = 100f)
                flex = 100f
            }
            rows.addGap(1)
            rows += {
                add(opacitySlider,height = 16, flex= 100f)
                add(btnVisibility, width = 24, height = 16)
                add(btnNewPart, width = 24, height = 16)
                add(btnRemovePart, width = 24, height = 16)
            }
            rows.addGap(1)
            rows += {
                add(Hybrid.ui.Label("Type: "), width = 60, height = 16)
                add(tfType, height = 16, flex = 1f)
            }
            rows.add(Hybrid.ui.Label("Translation").also { it.textSize = 9; it.bold = false }, height = 10)
                rows += {
                add(Hybrid.ui.Label("x"), width = 30, height = 16)
                add(tfTransX, height = 16, flex = 1f)
                addGap(4)
                add(Hybrid.ui.Label("y"), width = 30, height = 16)
                add(tfTransY, height = 16, flex = 1f)
            }
            rows.add(Hybrid.ui.Label("Scale").also { it.textSize = 9; it.bold = false }, height = 10)
            rows += {
                add(Hybrid.ui.Label("x"), width = 30, height = 16)
                add(tfScaleX, height = 16, flex = 1f)
                addGap(4)
                add(Hybrid.ui.Label("y"), width = 30, height = 16)
                add(tfScaleY, height = 16, flex = 1f)
            }
            rows.addGap(1)
            rows += {
                add(Hybrid.ui.Label("Rotation: "), width = 60, height = 16)
                add(tfRot, height = 16, flex = 1f)
            }
            rows.addGap(1)
            rows += {
                add(Hybrid.ui.Label("Depth: "), width = 60, height = 16)
                add(tfDepth, height = 16, flex = 1f)
            }
        }
    }
}