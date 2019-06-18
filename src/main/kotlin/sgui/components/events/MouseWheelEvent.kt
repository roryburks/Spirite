package sgui.components.events

import sgui.UIPoint

data class MouseWheelEvent(
        val point: UIPoint,
        val moveAmount : Int)