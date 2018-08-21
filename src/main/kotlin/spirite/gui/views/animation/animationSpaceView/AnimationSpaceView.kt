package spirite.gui.views.animation.animationSpaceView

import spirite.base.brains.IMasterControl
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFAAnimationSpace
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.resources.IIcon
import spirite.gui.resources.Transferables.AnimationTransferable
import spirite.hybrid.Hybrid
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

    init {
        imp.ref = this
        imp.setLayout {
            rows += {
                add(Hybrid.ui.CrossPanel(), 300, 300)
            }
        }
    }


    val __listener = master.centralObservatory.currentAnimationSpaceBind.addWeakListener { new, old ->
        val ffaspace = new as? FFAAnimationSpace
        if(ffaspace != null) {
            imp.setLayout {
                rows.add(Hybrid.ui.ScrollContainer(FFAAnimationSpaceView(ffaspace)), flex = 100f)
                rows.flex = 100f
            }
        }
    }


    init {
        imp.jcomponent.dropTarget = DndManager()
    }
    private inner class DndManager : DropTarget() {

        override fun drop(evt: DropTargetDropEvent) {
            if( evt.transferable.isDataFlavorSupported(AnimationTransferable.FLAVOR)) {
                val x = evt.transferable.getTransferData(AnimationTransferable.FLAVOR) as? Animation ?: return
                println(x is FixedFrameAnimation)
            }
        }

        override fun dragOver(evt: DropTargetDragEvent) {
            if( evt.isDataFlavorSupported(AnimationTransferable.FLAVOR))
                evt.acceptDrag(DnDConstants.ACTION_COPY)
            else evt.rejectDrag()
        }
    }
}