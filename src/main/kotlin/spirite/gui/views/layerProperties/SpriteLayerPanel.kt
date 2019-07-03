package spirite.gui.views.layerProperties

import rb.IContract
import rb.glow.color.Colors
import rb.owl.Observer
import rb.owl.bindable.Bindable
import rbJvm.owl.addWeakObserver
import rbJvm.owl.bindWeaklyTo
import sgui.components.IBoxList.IBoxComponent
import sgui.components.IBoxList.IMovementContract
import sgui.components.IComponent
import sgui.components.IComponent.BasicBorder.BEVELED_RAISED
import sgui.components.crossContainer.ICrossPanel
import sgui.components.events.MouseEvent
import sgui.components.events.MouseEvent.MouseButton.RIGHT
import spirite.base.brains.IMasterControl
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.gui.menus.SpriteLayerContextMenus
import spirite.gui.resources.SpiriteIcons
import spirite.hybrid.Hybrid

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
                    boxList.data.resetAllWithSelection(value.parts,  selectionSetForSprite(value) , value.activePart)
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
                else boxList.data.clear()
            }
        }

    private fun selectionSetForSprite(sprite: SpriteLayer) = when(val set = sprite.multiSelect) {
        null -> when( val part = sprite.activePart) {
            null -> setOf()
            else -> setOf(part)
        }
        else -> set
    }

    private val onPartChange = {
        val linked = linkedSprite
        when(linked) {
            null -> boxList.data.clear()
            else -> {
                boxList.data.resetAllWithSelection(linked.parts, selectionSetForSprite(linked), linked.activePart)
            }
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
    private val btnRemovePart = Hybrid.ui.Button()
    private val btnVisibility = Hybrid.ui.ToggleButton()

    init {
        btnVisibility.setOnIcon(SpiriteIcons.SmallIcons.Rig_VisibileOn)
        btnVisibility.setOffIcon(SpiriteIcons.SmallIcons.Rig_VisibleOff)
        btnNewPart.setIcon(SpiriteIcons.SmallIcons.Rig_New)
        btnRemovePart.setIcon(SpiriteIcons.SmallIcons.Rig_Remove)

        btnNewPart.action = {evt ->
            linkedSprite?.addPart("new", linked = evt.pressingShift)
        }
        btnRemovePart.action = { evt ->
            activePart?.also { activePart ->
                linkedSprite?.removePart(activePart, evt.pressingShift)
            }
        }

        boxList.movementContract = object : IMovementContract {
            override fun canMove(from: Int, to: Int): Boolean = true
            override fun doMove(from: Int, to: Int) {
                linkedSprite?.movePart(from, to)
            }
        }

        // Box List
        boxList.data.multiSelectEnabled = true

        boxList.renderer = {part ->
            object : IBoxComponent {
                override val component: IComponent = Hybrid.ui.CrossPanel {
                        cols += {
                            width = 32
                            addFlatGroup(22) {
                                add(Hybrid.ui.Label(part.partName).also {it.textSize = 10;it.textColor= Colors.BLACK}, height = 8)
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
                        onMouseClick +={boxList.requestFocus()}
                        if( boxList.data.selected == part) setBasicBorder(BEVELED_RAISED)
                    }

                override fun setSelected(selected: Boolean) {
                    if( selected) {
                        //activePart = part
                        component.setBasicBorder(BEVELED_RAISED)
                    }
                    else
                        component.setBasicBorder(null)
                }

                override fun onClick(mouseEvent: MouseEvent) {
                    if( mouseEvent.button == RIGHT) {
                        master.contextMenus.LaunchContextMenu(
                                mouseEvent.point,
                                SpriteLayerContextMenus.schemeForSprite(part),
                                part)
                    }
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

    private val _primarySelectK = boxList.data.selectedIndexBind.addWeakObserver { new, old -> linkedSprite?.activePart = boxList.data.selected }

    private val _otherSelectK = boxList.data.selectionObs.addObserver(Observer{
        if(boxList.data.currentSelectedSet.size <= 1) linkedSprite?.multiSelect = null
        else linkedSprite?.multiSelect = boxList.data.currentSelectedSet.toSet()
    })
}