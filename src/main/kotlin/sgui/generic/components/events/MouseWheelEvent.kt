package sgui.generic.components.events

import sgui.generic.UIPoint

data class MouseWheelEvent(
        val point: UIPoint,
        val moveAmount : Int)