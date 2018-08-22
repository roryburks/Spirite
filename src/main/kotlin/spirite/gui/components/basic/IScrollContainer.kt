package spirite.gui.components.basic

import spirite.base.util.linear.Rect

interface IScrollContainer : IComponent {
    fun makeAreaVisible( area: Rect)

    val horizontalBar: IScrollBar
    val verticalBar: IScrollBar
}

