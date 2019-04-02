package spirite.gui.views.animation.structureView.ffa

import org.jetbrains.annotations.Contract
import rb.owl.IContract
import spirite.base.imageData.animation.ffa.IFFAFrame
import spirite.base.imageData.animation.ffa.IFFALayer
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

interface IFFAStructView {
    val component :IComponent
    val height: Int
    val dragBrain : IAnimDragBrain?
}

interface IFFAStructViewBuilder {
    fun buildNameComponent(layer: IFFALayer) : IFFAStructView
    fun buildFrameComponent(layer: IFFALayer, frame: IFFAFrame) : IFFAStructView
}