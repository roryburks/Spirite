package sgui

import sgui.components.IComponent

abstract class UIPoint(
        val x : Int,
        val y: Int)
{
    abstract fun convert( other: IComponent) : UIPoint
}

