package spirite.gui.views.layerProperties

import rb.jvm.owl.addWeakObserver
import rb.jvm.owl.bindWeaklyTo
import rb.owl.IContract
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import spirite.base.brains.IMasterControl
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.base.util.Colors
import spirite.gui.components.basic.IBoxList.IBoxComponent
import spirite.gui.components.basic.IBoxList.IMovementContract
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_RAISED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import spirite.pc.gui.jcolor

class SpriteLayerPanel(master: IMasterControl) : ICrossPanel by Hybrid.ui.CrossPanel()
{
    private var _activePartK : IContract? = null
    private var _layerChangeObsK : IContract? = null

    private var _opacitySliderK : IContract? = null
    private var _btnVisibilityK : IContract? = null
    private var _tfTypeK : IContract? = null
    private var _tfDepthK : IContract? = null
    private var _tfTransXK : IContract? = null
    private var _tfTransYK : IContract? = null
    private var _tfScaleXK : IContract? = null
    private var _tfScaleYK : IContract? = null
    private var _tfRotK : IContract? = null

    var linkedSprite : SpriteLayer? = null
        set(value) {
            val old = field
            if( value != old) {
                field = value

                _layerChangeObsK?.void()
                _activePartK?.void()
                _tfTypeK?.void()
                _tfDepthK?.void()
                _tfTransXK?.void()
                _tfTransYK?.void()
                _tfScaleXK?.void()
                _tfScaleYK?.void()
                _tfRotK?.void()
                _opacitySliderK?.void()
                _btnVisibilityK?.void()

                if( value != null) {
                    _activePartK = activePartBind.bindWeaklyTo(value.activePartBind)
                    boxList.resetAllWithSelected(value.parts, value.activePart)
                    _layerChangeObsK = value.layerChangeObserver.addWeakObserver(onPartChange)

                    _tfTypeK = tfType.textBind.bindTo(value.cPartNameBind)
                    _tfDepthK = tfDepth.valueBind.bindTo(value.cDepthBind)
                    _tfTransXK = tfTransX.valueBind.bindTo(value.cTransXBind)
                    _tfTransYK = tfTransY.valueBind.bindTo(value.cTransYBind)
                    _tfScaleXK = tfScaleX.valueBind.bindTo(value.cScaleXBind)
                    _tfScaleYK = tfScaleY.valueBind.bindTo(value.cScaleYBind)
                    _tfRotK = tfRot.valueBind.bindTo(value.cRotBind)
                    _opacitySliderK = opacitySlider.valueBind.bindTo(value.cAlphaBind)
                    _btnVisibilityK = btnVisibility.checkBind.bindTo(value.cVisibleBind)
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

        btnNewPart.action = {evt ->
            if( evt.pressingShift)
                linkedSprite?.addPartLinked("new")
            else
                linkedSprite?.addPart("new")
        }

        boxList.movementContract = object : IMovementContract {
            override fun canMove(from: Int, to: Int): Boolean = true
            override fun doMove(from: Int, to: Int) {
                linkedSprite?.movePart(from, to)
            }
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
                    }.apply {
                        onMouseClick +={ boxList.selected = part ; boxList.requestFocus()}
                        if( boxList.selected == part) setBasicBorder(BEVELED_RAISED)
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
                add(Hybrid.ui.Label("xi"), width = 30, height = 16)
                add(tfTransX, height = 16, flex = 1f)
                addGap(4)
                add(Hybrid.ui.Label("yi"), width = 30, height = 16)
                add(tfTransY, height = 16, flex = 1f)
            }
            rows.add(Hybrid.ui.Label("Scale").also { it.textSize = 9; it.bold = false }, height = 10)
            rows += {
                add(Hybrid.ui.Label("xi"), width = 30, height = 16)
                add(tfScaleX, height = 16, flex = 1f)
                addGap(4)
                add(Hybrid.ui.Label("yi"), width = 30, height = 16)
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