package spirite.gui.basic.events

import spirite.gui.UIPoint

data class MouseWheelEvent(
        val point: UIPoint,
        val moveAmount : Int)