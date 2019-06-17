package spirite.gui.views.groupView

import rb.owl.bindable.addObserver
import spirite.base.brains.IMasterControl
import spirite.base.graphics.rendering.IThumbnailStore.IThumbnailAccessContract
import spirite.base.graphics.isolation.IIsolationManager.IsolationState
import spirite.base.imageData.groupTree.GroupTree.LayerNode
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import rb.glow.color.Colors
import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicBorder.BASIC
import sgui.generic.components.crossContainer.ICrossPanel
import sgui.generic.components.IToggleButton
import sgui.generic.components.events.MouseEvent.MouseButton.RIGHT
import sgui.swing.skin.Skin.AnimSchemePanel.ActiveNodeBg
import spirite.gui.resources.SpiriteIcons.SmallIcons.*
import spirite.hybrid.Hybrid

class SpriteLayerNodePanel
private constructor(
        val node: Node,
        val sprite: SpriteLayer,
        val master: IMasterControl,
        private val imp: ICrossPanel) : IComponent by imp
{
    constructor(node: Node, sprite: SpriteLayer, master: IMasterControl)
            : this(node, sprite, master,  Hybrid.ui.CrossPanel())

    val thumbnail = Hybrid.ui.ImageBox()
    val toggleButton = Hybrid.ui.ToggleButton(false)
    val editableLabel = Hybrid.ui.EditableLabel(node.name)
    val thumbnailContract = master.workspaceSet.currentWorkspace?.run {
        master.nativeThumbnailStore.contractThumbnail(node, this) {img ->thumbnail.setImage(img)}
    }

    init {
        imp.ref = this
        opaque = false
        background = Colors.TRANSPARENT

        toggleButton.plainStyle = true
        toggleButton.setOffIcon(Rig_New);
        toggleButton.setOnIcon(Rig_Remove);
        toggleButton.checkBind.addObserver { _, _ ->  setLayout()}

        //editableLabel.opaque = false
        editableLabel.textBind.addObserver { new, _-> node.name = new }

        setLayout()

        markAsPassThrough()
    }

    var partContracts: List<IThumbnailAccessContract>? = null

    fun setLayout()
    {
        partContracts?.forEach { it.release() }
        partContracts = null

        imp.setLayout {
            rows.addFlatGroup { add(toggleButton) }
            rows += {
                add(thumbnail, 32, 32)
                addGap(2)
                add(editableLabel, height = 16)
                height = 32
            }

            if (toggleButton.checked) {
                rows.addFlatGroup {
                    sprite.parts.forEach {part ->
                        add(SpriteLayerDisplayButton(part, master),12,12)
                        addGap(20)
                    }
                }
                rows.addFlatGroup(22) {
                    sprite.parts.forEach {part ->
                        val label = Hybrid.ui.Label(part.partName)
                        label.textSize = 10
                        label.textColor = Colors.BLACK
                        add(label,32,8)
                    }
                }
                rows += {
                    partContracts = sprite.parts.mapNotNull { part ->
                        val partThumb = Hybrid.ui.ImageBox()
                        partThumb.checkeredBackground = true
                        add(partThumb, 32, 32)
                        master.workspaceSet.currentWorkspace?.run {
                            master.nativeThumbnailStore.contractThumbnail(part, this) {
                                partThumb.setImage(it)
                            }
                        }
                    }
                }
            }
        }
    }

    class SpriteLayerDisplayButton(
            val part: SpritePart,
            val master: IMasterControl) : IToggleButton by Hybrid.ui.ToggleButton(true)
    {
        init {
            //btn.plainStyle = true
            background = ActiveNodeBg.scolor
            setBasicBorder(BASIC)
            setOnIcon(Rig_VisibileOn)
            setOffIcon(Rig_VisibleOff)
            onMouseClick += {evt->
                if( evt.button == RIGHT) {
                    val res = master.dialog.invokeDisplayOptions("Display for all Parts of kind [${part.partName}] in group.")
                    if( res != null) {
                        val workspace = part.context.workspace
                        val layer = part.context
                        val node = workspace.groupTree.root.getAllNodesSuchThat({(it as? LayerNode)?.layer == layer}).firstOrNull()
                        val parent = node?.parent
                        if( parent != null) {
                            if( evt.holdingAlt)
                                part.context.workspace.isolationManager.setIsolationStateForAllSpriteKindsBut(parent, part.partName, true, IsolationState(res.isVisible, res.alpha))
                            else
                                part.context.workspace.isolationManager.setIsolationStateForSpritePartKind(parent, part.partName, true, IsolationState(res.isVisible, res.alpha))
                        }
                    }
                }
            }
        }
    }
}