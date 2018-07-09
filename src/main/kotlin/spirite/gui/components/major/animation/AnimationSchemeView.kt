package spirite.gui.components.major.animation

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.FakeAnimation
import spirite.base.imageData.animation.IAnimationManager.AnimationObserver
import spirite.gui.components.advanced.ITreeViewNonUI.TreeNodeAttributes
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent.MouseButton.RIGHT
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.resources.IIcon
import spirite.gui.resources.SwIcons
import spirite.hybrid.Hybrid
import java.awt.event.MouseEvent

class AnimationSchemeView(val master: IMasterControl) : IOmniComponent {
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
        override fun makeComponent(t: Animation): IComponent {
            return Hybrid.ui.Label(t.name).also {it.onMouseRelease = rightclick}
        }
    }
    private val detailAttributes = object : TreeNodeAttributes<Animation> {
        override fun makeComponent(t: Animation): IComponent {
            return Hybrid.ui.Button(t.name)
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
                if( animation != null) {
                    master.contextMenus.LaunchContextMenu(evt.point, listOf(
                            MenuItem("Duplicate Animation", customAction = {animationManager.addAnimation(animation.dupe())}),
                            MenuItem("Delete Animation", customAction = {animationManager.removeAnimation(animation)})
                    ))
                }
            }
    }

    private val _animObs = object : AnimationObserver {
        override fun animationCreated(animation: Animation) = rebuild()
        override fun animationRemoved(animation: Animation) = rebuild()
    }.also { master.centralObservatory.omniAnimationObservable.addObserver(it)}

    private val _wsObs = master.workspaceSet.currentWorkspaceBind.addListener { _, _ ->  rebuild()}

    override fun close() {
        master.centralObservatory.omniAnimationObservable.removeObserver(_animObs)
        _wsObs.unbind()
    }

    init {
        rebuild()

        master.centralObservatory.currentAnimationBind.addWeakListener { new, old ->  list.selected = new}
        list.selectedBind.addListener { new, old ->  workspace?.animationManager?.currentAnimation = new }

        list.onMouseRelease = rightclick
    }
}