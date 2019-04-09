package spirite.gui.views.animation.structureView.ffa

import rb.owl.IContract
import spirite.base.imageData.animation.ffa.FFALayer.FFAFrame
import spirite.gui.components.basic.IComponent.BasicCursor.DEFAULT
import spirite.gui.components.basic.IComponent.BasicCursor.E_RESIZE
import spirite.gui.components.basic.events.MouseEvent
import spirite.gui.components.basic.events.MouseEvent.MouseButton.LEFT
import spirite.gui.components.basic.events.MouseEvent.MouseEventType.*
import spirite.gui.views.animation.structureView.AnimFFAStructPanel
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.max

abstract class ResizeableBrain(
        val frame: FFAFrame)
    :IAnimDragBrain
{
    override fun interpretMouseEvent(evt: MouseEvent, context: AnimFFAStructPanel): IAnimDragBehavior? {
        val rect = context.viewspace.rectForRangeInLayer(frame.layer, IntRange(frame.start, frame.end))
        val isDragEdge = evt.point.x > rect.x2 - 3

        if( evt.type == MOVED)
            context.setBasicCursor(if( isDragEdge) E_RESIZE else DEFAULT)
        if( evt.type == PRESSED && evt.button == LEFT && isDragEdge)
        {
            return ResizeBehavior(context)
        }

        return null
    }

    private inner class ResizeBehavior(val context: AnimFFAStructPanel) : IAnimDragBehavior {
        val start = frame.start
        var toLen = frame.length
        val viewspace get() = context.viewspace

        override fun interpretMouseEvent(evt: MouseEvent, contract: IContract) {
            val pt = evt.point.convert(context)
            when(evt.type) {
                DRAGGED -> {
                    toLen =  max(0, (pt.x - viewspace.leftJustification + viewspace.tickWidth/2) / viewspace.tickWidth - start)
                    val endBox = viewspace.rectForRangeInLayer(frame.layer, IntRange(start+toLen-1, start+toLen))
                    context.stretchWidth = endBox.x2
                    context.scrollContext.makeAreaVisible(endBox)
                    context.redraw()
                }
                RELEASED -> {
                    frame.length = toLen
                    contract.void()
                }
            }
        }

        override fun draw(gc: Graphics2D) {
            val range = viewspace.layerHeights[frame.layer]?: return
            val x = (start + toLen)* viewspace.tickWidth + viewspace.leftJustification
            gc.stroke = BasicStroke(2f)
            gc.color = Color.BLACK
            gc.drawLine(x, range.first, x, range.last)
        }
    }
}