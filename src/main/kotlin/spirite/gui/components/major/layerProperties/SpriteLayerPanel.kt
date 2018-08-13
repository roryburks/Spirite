package spirite.gui.components.major.layerProperties

import spirite.base.brains.Bindable
import spirite.base.brains.IMasterControl
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.Colors
import spirite.gui.components.basic.IBoxList.IBoxComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_RAISED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.jcolor
import java.awt.image.BufferedImage

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

    private val onPartChange = {
        val linked = linkedSprite
        when(linked) {
            null -> boxList.clear()
            else -> boxList.resetAllWithSelected(linked.parts, linked.activePart)
        }
    }

    private val activePartBind = Bindable<SpritePart?>(null)
    private var activePart: SpritePart? by activePartBind

    private val boxList = Hybrid.ui.BoxList<SpritePart>(32, 32)
    private val tfTransX = Hybrid.ui.FloatField()
    private val tfTransY = Hybrid.ui.FloatField()
    private val tfScaleX =  Hybrid.ui.FloatField()
    private val tfScaleY = Hybrid.ui.FloatField()
    private val tfRot = Hybrid.ui.FloatField()
    private val tfDepth = Hybrid.ui.IntField(allowsNegative = true)
    private val tfType = Hybrid.ui.TextField()
    private val opacitySlider = Hybrid.ui.GradientSlider(0f,1f, "Opacity")


    private val btnNewPart = Hybrid.ui.Button()
    private val btnRemovePart = Hybrid.ui.Button().also { it.action = {activePart?.also {  linkedSprite?.removePart( it)} }}
    private val btnVisibility = Hybrid.ui.ToggleButton()

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
                    get() = Hybrid.ui.CrossPanel {
                        cols += {
                            width = 32
                            addFlatGroup(22) {
                                add(Hybrid.ui.Label(part.partName).also {it.textSize = 10;it.textColor= Colors.BLACK.jcolor}, height = 8)
                            }
                            this += {
                                val thumbnail = Hybrid.ui.ImageBox()
                                thumbnail.checkeredBackground = true
                                thumbnail.ref = master.workspaceSet.currentWorkspace?.run {
                                    master.nativeThumbnailStore.contractThumbnail(part, this) {
                                        thumbnail.setImage(it)
                                    }
                                }
                                add( thumbnail)
                                height = 32
                            }
                        }
                    }.also {
                        it.onMouseClick ={ boxList.selected = part}
                        if( boxList.selected == part) it.setBasicBorder(BEVELED_RAISED)
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