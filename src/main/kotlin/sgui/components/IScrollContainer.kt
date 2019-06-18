package sgui.components

import rb.vectrix.shapes.RectI


interface IScrollContainer : IComponent {
    fun makeAreaVisible( area: RectI)

    val horizontalBar: IScrollBar
    val verticalBar: IScrollBar
}

