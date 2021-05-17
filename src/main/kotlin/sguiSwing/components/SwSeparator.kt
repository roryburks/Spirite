package sguiSwing.components

import sgui.core.components.ISeparator
import sgui.core.Orientation
import sguiSwing.mouseSystem.adaptMouseSystem
import javax.swing.JSeparator
import javax.swing.SwingConstants

class SwSeparator
private constructor(private val imp : SwSeparatorImp) : ISeparator, ISwComponent by SwComponent(imp)
{
    constructor(orientation: Orientation) : this(SwSeparatorImp(orientation))

    private class SwSeparatorImp(orientation: Orientation): JSeparator(when( orientation) {
        Orientation.HORIZONTAL -> SwingConstants.HORIZONTAL
        Orientation.VERTICAL -> SwingConstants.VERTICAL
    })
    {
        init {adaptMouseSystem()}
    }
}