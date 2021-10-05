package spirite.gui.views.groupView

import rb.glow.Color
import rb.glow.Colors
import rb.owl.bindable.addObserver
import sgui.components.IComponent
import sgui.core.components.crossContainer.ICrossPanel
import spirite.base.brains.IMasterControl
import spirite.base.imageData.groupTree.Node
import spirite.sguiHybrid.Hybrid

class NodeLayerPanel
private constructor(
    val node: Node,
    val master: IMasterControl,
    private val _imp: ICrossPanel
) : IComponent by _imp
{
    constructor(node: Node, master: IMasterControl)
            : this(node,  master, Hybrid.ui.CrossPanel())

    private val thumbnail = Hybrid.ui.ImageBox()
    private val thumbnailContract = master.workspaceSet.currentWorkspace?.run {
        master.nativeThumbnailStore.contractThumbnail(node, this) {img ->thumbnail.setImage(img)}
    }
    private val editableLabel =  Hybrid.ui.EditableLabel(node.name)
    private val littleLabel = Hybrid.ui.Label()


    init {

        _imp.ref = this
        opaque = false
        background = Colors.TRANSPARENT

        //editableLabel.opaque = false
        editableLabel.textBind.addObserver { new, _ -> node.name = new }
        littleLabel.textSize = 8
        littleLabel.textColor = Color.Make(239,228,175)

        // Determine littleLabel Text
        littleLabel.text = GroupViewHelper.determineLittleLabelText(master, node)

        _imp.setLayout {
            rows += {
                add(thumbnail, 32, 32)
                addGap(2)
                this += {
                    add(editableLabel, height = 16)
                    add(littleLabel, height = 10)
                }
                height = 32
            }
        }
    }

    fun triggerRename() { editableLabel.startEditing()}
}