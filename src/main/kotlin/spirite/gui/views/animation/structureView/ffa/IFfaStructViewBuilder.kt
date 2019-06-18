package spirite.gui.views.animation.structureView.ffa

import rb.IContract
import sgui.components.IComponent
import sgui.components.events.MouseEvent
import spirite.base.imageData.animation.ffa.IFfaFrame
import spirite.base.imageData.animation.ffa.IFfaLayer
import spirite.gui.views.animation.structureView.AnimFFAStructPanel
import java.awt.Graphics2D


interface IAnimDragBehavior {
    fun interpretMouseEvent(evt: MouseEvent, contract: IContract)
    fun draw( gc: Graphics2D) {}
}


interface IAnimDragBrain {
    fun interpretMouseEvent(evt: MouseEvent, context : AnimFFAStructPanel) : IAnimDragBehavior?
}
class AnimDragBrain(val lambda: (evt: MouseEvent, context : AnimFFAStructPanel) -> IAnimDragBehavior?) : IAnimDragBrain
{
    override fun interpretMouseEvent(evt: MouseEvent, context: AnimFFAStructPanel) = lambda(evt, context)
}

interface IFFAStructView {
    val component : IComponent
    val height: Int
    val dragBrain : IAnimDragBrain?
}

interface IFfaStructViewBuilder {
    fun buildNameComponent(layer: IFfaLayer) : IFFAStructView
    fun buildFrameComponent(layer: IFfaLayer, frame: IFfaFrame) : IFFAStructView
}