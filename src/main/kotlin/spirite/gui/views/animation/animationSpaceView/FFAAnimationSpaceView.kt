package spirite.gui.views.animation.animationSpaceView

import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.FFAAnimationSpace
import spirite.base.imageData.animationSpaces.IAnimationSpaceView.InternalAnimationSpaceObserver
import spirite.base.util.linear.Vec2i
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid

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

            animationSpace.animations.forEach {ffa->
                val location = animationSpace.stateView.logicalSpace[ffa] ?: Vec2i.Zero
                rows.addFlatGroup(location.y) {
                    addGap(location.x)
                    add(FFABlock(ffa))
                }
            }
            rows += {
                add(Hybrid.ui.CrossPanel())
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

    private val __listener =animationSpace.stateView.animationSpaceObservable.addObserver( object : InternalAnimationSpaceObserver {
        override fun animationSpaceChanged(structureChange: Boolean) {
            rebuild()
        }
    })
//
//    init {
//        imp.jcomponent.dropTarget = DndManager()
//    }
//    private inner class DndManager : DropTarget() {
//
//        override fun drop(evt: DropTargetDropEvent) {
//
//            println("drop")
////            try {
////                val draggingRelativeTo = draggingRelativeTo
////                if (draggingRelativeTo == dragging && dragging != null) return
////
////                val interpreter = (if (draggingRelativeTo == null) treeRootInterpreter else draggingRelativeTo.attributes)
////                        ?: return
////                if (interpreter.canImport(evt.transferable) && draggingRelativeTo != null)
////                    interpreter.interpretDrop(evt.transferable, draggingRelativeTo, draggingDirection)
////            }finally {
////                dragging = null
////            }
//        }
//
//        override fun dragOver(evt: DropTargetDragEvent) {
//        }
//    }
}