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
                field = value
                old?.layerChangeObserver?.removeObserver(onPartChange)
                activePartBind.unbind()

                tfType.textBind.unbind()
                tfDepth.valueBind.unbind()
                tfTransX.valueBind.unbind()
                tfTransY.valueBind.unbind()
                tfScaleX.valueBind.unbind()
                tfScaleY.valueBind.unbind()
                tfRot.valueBind.unbind()
                opacitySlider.valueBind.unbind()
                btnVisibility.checkBind.unbind()

                if( value != null) {
                    value.activePartBind.bindWeakly(activePartBind)
                    boxList.resetAllWithSelected(value.parts, value.activePart)
                    value.layerChangeObserver.addObserver(onPartChange)

                    value.cPartNameBind.bindWeakly(tfType.textBind)
                    value.cDepthBind.bindWeakly(tfDepth.valueBind)
                    value.cAlphaBind.bindWeakly(opacitySlider.valueBind)
                    value.cTransXBind.bindWeakly(tfTransX.valueBind)
                    value.cTransYBind.bindWeakly(tfTransY.valueBind)
                    value.cScaleXBind.bindWeakly(tfScaleX.valueBind)
                    value.cScaleYBind.bindWeakly(tfScaleY.valueBind)
                    value.cRotBind.bindWeakly(tfRot.valueBind)
                    value.cVisibleBind.bindWeakly(btnVisibility.checkBind)
                }
                else boxList.clear()
            }
        }

    val onPartChange = {
        val linked = linkedSprite
        when(linked) {
            null -> boxList.clear()
            else -> boxList.resetAllWithSelected(linked.parts, linked.activePart)
        }
    }

    val activePartBind = Bindable<SpritePart?>(null) { new, old ->
    }
    var activePart: SpritePart? by activePartBind

    val boxList = Hybrid.ui.BoxList<SpritePart>(24, 24)
    val tfTransX = Hybrid.ui.FloatField()
    val tfTransY = Hybrid.ui.FloatField()
    val tfScaleX =  Hybrid.ui.FloatField()
    val tfScaleY = Hybrid.ui.FloatField()
    val tfRot = Hybrid.ui.FloatField()
    val tfDepth = Hybrid.ui.IntField(allowsNegative = true)
    val tfType = Hybrid.ui.TextField()
    val opacitySlider = Hybrid.ui.GradientSlider(0f,1f, "Opacity")

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
    val btnRemovePart = Hybrid.ui.Button().also { it.action = {activePart?.also {  linkedSprite?.removePart( it)} }}
    val btnVisibility = Hybrid.ui.ToggleButton()

    init {
        btnVisibility.setOnIcon(SwIcons.SmallIcons.Rig_VisibileOn)
        btnVisibility.setOffIcon(SwIcons.SmallIcons.Rig_VisibleOff)
        btnNewPart.setIcon(SwIcons.SmallIcons.Rig_New)
        btnRemovePart.setIcon(SwIcons.SmallIcons.Rig_Remove)

        btnNewPart.action = {
            linkedSprite?.addPart("new")
        }



        boxList.renderer = {part ->
            object : IBoxComponent {
                override val component: IComponent
                    get() = Hybrid.ui.Button(part.partName).also {
                        it.action ={ boxList.selected = part}
                    }

                override fun setSelected(selected: Boolean) {
                    if( selected)
                        activePart = part
                }
            }
        }


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