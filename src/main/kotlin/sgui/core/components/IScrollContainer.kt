package sgui.core.components

import rb.vectrix.shapes.RectI
import sgui.components.IComponent


interface IScrollContainer : IComponent {
    fun makeAreaVisible( area: RectI)

    val horizontalBar: IScrollBar
    val verticalBar: IScrollBar
}

