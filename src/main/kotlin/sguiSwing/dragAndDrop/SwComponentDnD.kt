package sguiSwing.dragAndDrop

import rb.vectrix.mathUtil.MathUtil
import rb.vectrix.mathUtil.f
import sguiSwing.components.SwComponent
import java.awt.Component
import java.awt.Point
import java.awt.dnd.*
import java.awt.event.InputEvent
import java.awt.event.MouseEvent

fun SwComponent.setDragTarget(listener: DropTargetListener) {
    this.component.dropTarget = DropTarget(this.component, listener)
}

fun SwComponent.addDragSource(
        gestureListener: DragGestureListener,
        distanceThreshold: Float = 5f ) {
    var startX: Int? = null
    var startY: Int? = null

    val recognizer = SwCompDhr(component,  DnDConstants.ACTION_COPY_OR_MOVE, gestureListener)

    onMousePress += {
        startX = it.point.x
        startY = it.point.y
    }
    onMouseDrag += {
        val sx = startX
        val sy = startY
        if( sx!= null && sy != null && MathUtil.distance(it.point.x.f, it.point.y.f, sx.f, sy.f) > distanceThreshold)
        {
            startX = null
            startY = null
            recognizer.fire(it.point.x, it.point.y)
        }
    }
    onMouseRelease += {
        startX = null
        startY = null
    }
}

private class SwCompDhr( comp: Component, i:Int, dgl: DragGestureListener, source:DragSource = DragSource.getDefaultDragSource())
    :DragGestureRecognizer(source, comp, i, dgl)
{
    override fun unregisterListeners() {}
    override fun registerListeners() {}

    fun fire( x: Int, y: Int) {
        events = ArrayList<InputEvent>(1)
        events.add(MouseEvent(component, 0, 0, 0, 0, 0, 0,  false))
        fireDragGestureRecognized(DnDConstants.ACTION_MOVE, Point(x,y))
    }
}