package spirite.gui.views.animation.animationSpaceView

import rb.jvm.owl.addWeakObserver
import rb.jvm.owl.bindWeaklyTo
import rb.owl.bindable.addObserver
import spirite.base.brains.IMasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFASpace.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.AnimationSpace
import spirite.base.imageData.animationSpaces.IAnimationSpaceManager.AnimationSpaceObserver
import spirite.gui.Orientation.HORIZONTAL
import spirite.gui.components.advanced.ResizeContainerPanel
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

    private val rightImp = Hybrid.ui.CrossPanel()
    private val leftImp = Hybrid.ui.CrossPanel()
    private val split = ResizeContainerPanel(rightImp, HORIZONTAL, 300)
    private val imp = Hybrid.ui.CrossPanel()
    private val subRightPanel = Hybrid.ui.CrossPanel()
    private val spaceDropdown = Hybrid.ui.ComboBox<AnimationSpace>(emptyArray())
    private val removeButton = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Rig_Remove) }
    private val newButton = Hybrid.ui.Button().also { it.setIcon(SwIcons.SmallIcons.Rig_New) }

    val currentWorkspace get() = master.workspaceSet.currentWorkspace

    init /*Visuals*/ {
        spaceDropdown.renderer = {value: AnimationSpace?, index: Int, isSelected: Boolean, hasFocus: Boolean ->
            when(value) {
                null -> Hybrid.ui.Label("---")
                else -> Hybrid.ui.EditableLabel(value.name).also { it.textBind.bindWeaklyTo(value.nameBind)}
            }
        }
    }

    init /*Layout*/ {
        imp.setLayout {
            cols.add(split, flex = 600f)
            cols.flex = 300f
        }

        split.addPanel(leftImp, 100, 300, 1)

        rightImp.ref = this
        rightImp.setLayout {
            rows += {
                add(subRightPanel, flex=300f)
                flex = 300f
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
        spaceDropdown.selectedItemBind.addObserver { new, _ ->
            when( new) {
                is FFAAnimationSpace -> {
                    subRightPanel.setLayout {
                        rows.add(Hybrid.ui.ScrollContainer(FFAAnimationSpaceView(new)), flex = 100f)
                        rows.flex = 100f
                    }
                    leftImp.setLayout {
                        rows.add(FFAPlayView(master, new))
                    }
                }
                else -> {subRightPanel.setLayout {  }}
            }
        }
    }

    fun rebuildDropDown(workspace : IImageWorkspace?)
    {
        when( workspace) {
            null -> spaceDropdown.setValues(emptyList())
            else -> spaceDropdown.setValues(workspace.animationSpaceManager.animationSpaces, workspace.animationSpaceManager.currentAnimationSpace)
        }
    }

    // region Observers
    val animationSpaceObserver = object : AnimationSpaceObserver {
        override fun spaceAdded(space: AnimationSpace) {
            if( space.workspace == currentWorkspace)
            rebuildDropDown(currentWorkspace)
        }

        override fun spaceRemoved(space: AnimationSpace) {
            if( space.workspace == currentWorkspace)
            rebuildDropDown(currentWorkspace)
        }

    }
    val __worspaceListenerK = master.workspaceSet.currentWorkspaceBind.addWeakObserver { new, old ->
        old?.animationSpaceManager?.animationSpaceObservable?.removeObserver(animationSpaceObserver)
        new?.animationSpaceManager?.animationSpaceObservable?.addObserver(animationSpaceObserver)
        rebuildDropDown(new)
    }
    val _curAnimK = master.centralObservatory.currentAnimationSpaceBind.addWeakObserver { new, _ ->
        new?.also { spaceDropdown.selectedItem = it}
    }
    //endregion

    // region Dnd
    init {
        rightImp.jcomponent.dropTarget = DndManager()
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
                                val newSpace = FFAAnimationSpace("${droppedAnimation.name} space", workspace)
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

}