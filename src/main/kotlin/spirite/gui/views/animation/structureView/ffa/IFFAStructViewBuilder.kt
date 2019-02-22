package spirite.gui.views.animation.structureView.ffa

import rb.owl.IContract
import spirite.base.imageData.animation.ffa.IFFAFrame
import spirite.base.imageData.animation.ffa.IFFALayer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.events.MouseEvent

interface IAnimDragBehavior {
    fun intepretMouseEvent(evt: MouseEvent, terminationContrace : IContract)
}

interface IAnimDragBrain {
    fun interpretMouseEvent(evtMouseEvent: MouseEvent) : IAnimDragBehavior?
}

interface IFFAStructView {
    val component :IComponent
    val height: Int
    val dragBrain : IAnimDragBrain?
}

interface IFFAStructViewBuilder {
    fun buildMenuComponent(layer: IFFALayer) : IFFAStructView
    fun buildFrameComponent(layer: IFFALayer, frame: IFFAFrame) : IFFAStructView
}