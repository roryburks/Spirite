package spirite.gui.views.animation.animationSpaceView

import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFAAnimationSpace
import spirite.base.util.linear.Vec2i
import spirite.gui.components.advanced.ITreeViewNonUI.DropDirection.*
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.jcomponent
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent

class FFAAnimationSpaceView(
        val animationSpace: FFAAnimationSpace,
        val imp : ICrossPanel = Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    init {
        rebuild()
    }

    fun rebuild()
    {
        imp.setLayout {
            rows += {
                add(Hybrid.ui.CrossPanel(), 300,300)
            }

            animationSpace.animations.forEach {ffa->
                val location = animationSpace.stateView.logicalSpace[ffa] ?: Vec2i.Zero
                rows.addFlatGroup(location.y) {
                    addGap(location.x)
                    add(FFABlock(ffa))
                }
            }
        }
    }

    private inner class FFABlock(
            val animation: FixedFrameAnimation,
            val imp : ICrossPanel = Hybrid.ui.CrossPanel())
        : IComponent by imp
    {
        init {
            imp.setLayout {
                (animation.start until animation.end).forEach {
                    cols.add(Hybrid.ui.Button(it.toString()), 24, 24)
                }
            }
        }
    }

    private val __listener =animationSpace.stateView.internalStateObservable.addObserver{
        rebuild()
    }

    init {
        imp.jcomponent.dropTarget = DndManager()
    }
    private inner class DndManager : DropTarget() {

        override fun drop(evt: DropTargetDropEvent) {
            println("drop")
//            try {
//                val draggingRelativeTo = draggingRelativeTo
//                if (draggingRelativeTo == dragging && dragging != null) return
//
//                val interpreter = (if (draggingRelativeTo == null) treeRootInterpreter else draggingRelativeTo.attributes)
//                        ?: return
//                if (interpreter.canImport(evt.transferable) && draggingRelativeTo != null)
//                    interpreter.interpretDrop(evt.transferable, draggingRelativeTo, draggingDirection)
//            }finally {
//                dragging = null
//            }
        }

        override fun dragOver(evt: DropTargetDragEvent) {
            println("dragOVer")
            evt.acceptDrag(DnDConstants.ACTION_COPY)
//            val oldNode = draggingRelativeTo
//            val oldDir = draggingDirection
//
//            val e_y = evt.location.y
//            val node =getNodeFromY(e_y)
//            draggingRelativeTo = node
//            draggingDirection = when {
//                node == null && e_y < 0 -> ABOVE
//                node == null -> BELOW
//                else -> {
//                    val n_y = node.component?.y ?: return
//                    val n_h = node.component?.height ?: return
//                    when {
//                        !node.attributes.isLeaf &&
//                                e_y > n_y + n_h/4 &&
//                                e_y < n_y + (n_h*3)/4 -> INTO
//                        e_y < n_y + n_h/2 -> ABOVE
//                        else -> BELOW
//                    }
//                }
//            }
//
//            val binding = node?.attributes ?: treeRootInterpreter
//            if( binding?.canImport(evt.transferable) == true)
//                evt.acceptDrag( DnDConstants.ACTION_COPY)
//            else
//                evt.rejectDrag()
//
//
//            if( oldDir != draggingDirection || oldNode != draggingRelativeTo)
//                redraw()
        }
    }
}