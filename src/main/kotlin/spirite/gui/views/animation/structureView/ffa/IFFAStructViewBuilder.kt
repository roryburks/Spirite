package spirite.gui.views.animation.structureView.ffa

import rb.owl.IContract
import spirite.base.imageData.animation.ffa.IFFAFrame
import spirite.base.imageData.animation.ffa.IFfaLayer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent
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
    val component :IComponent
    val height: Int
    val dragBrain : IAnimDragBrain?
}

interface IFFAStructViewBuilder {
    fun buildNameComponent(layer: IFfaLayer) : IFFAStructView
    fun buildFrameComponent(layer: IFfaLayer, frame: IFFAFrame) : IFFAStructView
}