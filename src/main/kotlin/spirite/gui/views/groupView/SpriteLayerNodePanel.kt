package spirite.gui.views.groupView

import rb.glow.Color
import rb.glow.Colors
import rb.owl.bindable.addObserver
import sgui.components.IComponent
import sgui.components.IComponent.BasicBorder.BASIC
import sgui.core.components.IToggleButton
import sgui.core.components.crossContainer.ICrossPanel
import sgui.core.components.events.MouseEvent.MouseButton.RIGHT
import sgui.swing.skin.Skin.AnimSchemePanel.ActiveNodeBg
import spirite.base.brains.IMasterControl
import spirite.base.graphics.isolation.IIsolationManager.IsolationState
import spirite.base.graphics.rendering.IThumbnailStore.IThumbnailAccessContract
import spirite.base.imageData.groupTree.LayerNode
import spirite.base.imageData.groupTree.Node
import spirite.base.imageData.layers.sprite.SpriteLayer
import spirite.base.imageData.layers.sprite.SpriteLayer.SpritePart
import spirite.gui.resources.SpiriteIcons.SmallIcons.*
import spirite.sguiHybrid.Hybrid

class SpriteLayerNodePanel
private constructor(
    val node: Node,
    val sprite: SpriteLayer,
    val master: IMasterControl,
    private val _imp: ICrossPanel) : IComponent by _imp
{
    constructor(node: Node, sprite: SpriteLayer, master: IMasterControl)
            : this(node, sprite, master,  Hybrid.ui.CrossPanel())

    val thumbnail = Hybrid.ui.ImageBox()
    val toggleButton = Hybrid.ui.ToggleButton(false)
    val editableLabel = Hybrid.ui.EditableLabel(node.name)
    private val littleLabel = Hybrid.ui.Label()
    val thumbnailContract = master.workspaceSet.currentWorkspace?.run {
        master.nativeThumbnailStore.contractThumbnail(node, this) {img ->thumbnail.setImage(img)}
    }

    init {
        _imp.ref = this
        opaque = false
        background = Colors.TRANSPARENT

        toggleButton.plainStyle = true
        toggleButton.setOffIcon(Rig_New);
        toggleButton.setOnIcon(Rig_Remove);
        toggleButton.checkBind.addObserver { _, _ ->  setLayout()}

        //editableLabel.opaque = false
        editableLabel.textBind.addObserver { new, _-> node.name = new }
        littleLabel.textSize = 8
        littleLabel.textColor = Color.Make(239,228,175)

        // Determine littleLabel Text
        littleLabel.text = GroupViewHelper.determineLittleLabelText(master, node)

        setLayout()

        markAsPassThrough()
    }

    var partContracts: List<IThumbnailAccessContract>? = null

    fun setLayout()
    {
        partContracts?.forEach { it.release() }
        partContracts = null

        _imp.setLayout {
            rows.addFlatGroup { add(toggleButton) }
            rows += {
                add(thumbnail, 32, 32)
                addGap(2)
                this += {
                    add(editableLabel, height = 16)
                    add(littleLabel, height = 10)
                }
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
            setOnIcon(Rig_VisibleOn)
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