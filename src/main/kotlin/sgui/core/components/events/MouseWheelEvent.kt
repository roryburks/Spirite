package sgui.core.components.events

import sgui.core.UIPoint

data class MouseWheelEvent(
    val point: UIPoint,
    val moveAmount : Int)