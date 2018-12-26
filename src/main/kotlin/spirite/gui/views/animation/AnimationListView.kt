package spirite.gui.views.animation

import rb.jvm.owl.addWeakObserver
import rb.owl.bindable.addObserver
import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.DeleteAnimationCommand
import spirite.base.brains.commands.DuplicateAnimationCommand
import spirite.base.brains.commands.ExportAafCommand
import spirite.base.brains.commands.RenameAnimationCommand
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.gui.components.advanced.ITreeViewNonUI.*
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.gui.resources.Transferables.AnimationTransferable
import spirite.hybrid.Hybrid
import spirite.pc.graphics.ImageBI
import spirite.pc.gui.basic.jcomponent
import java.awt.Point
import java.awt.dnd.*

class AnimationListView(val master: IMasterControl) : IOmniComponent {
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = SwIcons.BigIcons.Frame_AnimationScheme
    override val name: String get() = "Anims"

    private val workspace get() = master.workspaceSet.currentWorkspace

    private val list = Hybrid.ui.TreeView<Animation>()

    private val imp = Hybrid.ui.CrossPanel {
        rows.add(list, flex = 1f)
        rows.flex = 1f
    }

    private val attributes = object : TreeNodeAttributes<Animation> {
        override fun makeComponent(t: Animation): ITreeComponent {
            return object : ITreeComponent {
                override val component = Hybrid.ui.Label(t.name).also {
                    t.nameBind.addObserver { new, _ -> it.text = new }
                    it.onMouseRelease += rightclick
                    dnd.addListener(it, t)
                }
            }
        }
    }
    private val detailAttributes = object : TreeNodeAttributes<Animation> {
        override fun makeComponent(t: Animation): ITreeComponent {
            return object : ITreeComponent {
                override val component: IComponent = Hybrid.ui.Button(t.name).also{dnd.addListener(it, t)}
            }
        }
    }

    private fun rebuild() {
        list.clearRoots()
        list.constructTree {
            val workspace = workspace
            workspace?.animationManager?.animations?.
                    forEach { Branch(it, attributes, false) {Node(it, detailAttributes)} }
        }

        workspace?.animationManager?.animations
    }

    private val rightclick = { evt : spirite.gui.components.basic.events.MouseEvent->
        if( evt.button == RIGHT )
            workspace?.apply {
                val animation = list.getNodeFromY(evt.point.y)?.value

                val menuItems = mutableListOf(
                        MenuItem("Create New Fixed Frame Animation", customAction = {animationManager.addAnimation(FixedFrameAnimation("FixedFrameAnimation", this))})
                )


                if( animation != null) {
                    menuItems.add(MenuItem("Rename Animation", RenameAnimationCommand))
                    menuItems.add(MenuItem("Duplicate Animation", DuplicateAnimationCommand))
                    menuItems.add(MenuItem("Delete Animation", DeleteAnimationCommand))
                }
                if( animation is FixedFrameAnimation) {
                    menuItems.add(MenuItem("Export Animation To Aaf", ExportAafCommand))
                }

                master.contextMenus.LaunchContextMenu(evt.point, menuItems, animation)
            }
    }

    private val _animObs = object : AnimationObserver {
        override fun animationCreated(animation: Animation) = rebuild()
        override fun animationRemoved(animation: Animation) = rebuild()
    }.also { master.centralObservatory.trackingAnimationObservable.addObserver(it)}

    private val _wsObsK = master.workspaceSet.currentWorkspaceBind.addWeakObserver {  _, _ ->  rebuild()}
    private val _curAnimK = master.centralObservatory.currentAnimationBind.addWeakObserver { new, old ->  list.selected = new}

    override fun close() {
        master.centralObservatory.trackingAnimationObservable.removeObserver(_animObs)
        _wsObsK.void()
        _curAnimK.void()
    }

    init {
        rebuild()

        // Note: Since life of list is same as AnimationListView, no need to be weak
        list.selectedBind.addObserver { new, old ->  workspace?.animationManager?.currentAnimation = new }

        list.onMouseRelease += rightclick
    }


    //region Dnd
    private val dnd = DndManager()
    private inner class DndManager {
        val dragSource = DragSource.getDefaultDragSource()

        val rootDragListener = DragGestureListener { evt ->
            val animation = list.getNodeFromY(evt.dragOrigin.y )?.value ?: return@DragGestureListener
            startDragAnimation(animation, evt)
        }.also { dragSource.createDefaultDragGestureRecognizer(list.jcomponent,DnDConstants.ACTION_COPY_OR_MOVE, it ) }

        fun addListener(component: IComponent, animation: Animation)
        {
            dragSource.createDefaultDragGestureRecognizer(component.jcomponent,DnDConstants.ACTION_COPY_OR_MOVE)
                { evt -> startDragAnimation(animation, evt)}
        }

        fun startDragAnimation( animation: Animation, evt: DragGestureEvent) {
            val cursor = DragSource.DefaultMoveDrop
            //val cursorImage = Hybrid.imageConverter.convertOrNull<ImageBI>(node.attributes.makeCursor(node.value))?.bi
            dragSource.startDrag(
                    evt,
                    cursor,
                    null,
                    Point(10,10),
                    AnimationTransferable(animation),
                    null)
        }
    }
    //endregion
}