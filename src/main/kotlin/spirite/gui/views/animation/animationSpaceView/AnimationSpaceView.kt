package spirite.gui.views.animation.animationSpaceView

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.AnimationSpace
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.gui.resources.Transferables.AnimationTransferable
import spirite.hybrid.Hybrid
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.WarningType.UNSUPPORTED
import spirite.pc.gui.basic.jcomponent
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent

class AnimationSpaceView(private val master: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = null
    override val name: String get() = "Animation Space View"

    private val imp = Hybrid.ui.CrossPanel()
    private val subPanel = Hybrid.ui.CrossPanel()
    private val spaceDropdown = Hybrid.ui.ComboBox<AnimationSpace>(emptyArray())
    private val removeButton = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Rig_Remove) }
    private val newButton = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Rig_New) }

    val currentWorkspace get() = master.workspaceSet.currentWorkspace

    init /*Layout*/ {
        imp.ref = this
        imp.setLayout {
            rows += {
                add(subPanel, 300, 300)
            }
            rows += {
                add(spaceDropdown, height = 20, flex = 100f)
                addGap(2)
                add(removeButton, height = 20, width = 20)
                addGap(1)
                add(newButton, height = 20, width = 20)
            }
        }
    }

    init {
        spaceDropdown.selectedItemBind.addRootListener { new, _ ->
            when( new) {
                is FFAAnimationSpace -> {
                    subPanel.setLayout {
                        rows.add(Hybrid.ui.ScrollContainer(FFAAnimationSpaceView(new)), flex = 100f)
                        rows.flex = 100f
                    }
                }
            }
        }
    }

    val __worspaceListener = master.workspaceSet.currentWorkspaceBind.addWeakListener { new, _ ->
        when( new) {
            null -> spaceDropdown.setValues(emptyList())
            else -> spaceDropdown.setValues(new.animationSpaceManager.animationSpaces, new.animationSpaceManager.currentAnimationSpace)
        }
    }
    val __animBindListener = master.centralObservatory.currentAnimationSpaceBind.addWeakListener { new, _ ->
        new?.also { spaceDropdown.selectedItem = it}
    }

    // region Dnd
    init {
        imp.jcomponent.dropTarget = DndManager()
    }
    private inner class DndManager : DropTarget() {

        override fun drop(evt: DropTargetDropEvent) {
            val workspace = currentWorkspace ?: return

            if( evt.transferable.isDataFlavorSupported(AnimationTransferable.FLAVOR)) {
                val droppedAnimation = evt.transferable.getTransferData(AnimationTransferable.FLAVOR) as? Animation ?: return

                if( droppedAnimation.workspace != workspace)
                {
                    MDebug.handleWarning(UNSUPPORTED, "Cannot import animation into different Workspace")
                    return
                }

                when(droppedAnimation) {
                    is FixedFrameAnimation -> {
                        val selected = spaceDropdown.selectedItem
                        when( selected) {
                            is FFAAnimationSpace -> selected.addAnimation(droppedAnimation)
                            else -> {
                                val newSpace = FFAAnimationSpace("$droppedAnimation space", workspace)
                                newSpace.addAnimation(droppedAnimation)
                                workspace.animationSpaceManager.addAnimationSpace(newSpace, true)
                            }
                        }
                    }
                }
            }
        }

        override fun dragOver(evt: DropTargetDragEvent) {
            if( evt.isDataFlavorSupported(AnimationTransferable.FLAVOR))
                evt.acceptDrag(DnDConstants.ACTION_COPY)
            else evt.rejectDrag()
        }
    }
    // endregion

    init /*Visuals*/ {
//        spaceDropdown.renderer = {value: AnimationSpace, index: Int, isSelected: Boolean, hasFocus: Boolean ->
//            Hybrid.ui.Label(value.name)
//        }
    }
}