package sguiSwing.components

import sgui.components.ISeparator
import sguiSwing.mouseSystem.adaptMouseSystem
import javax.swing.JSeparator
import javax.swing.SwingConstants

class SwSeparator
private constructor(private val imp : SwSeparatorImp) : ISeparator, ISwComponent by SwComponent(imp)
{
    constructor(orientation: sgui.Orientation) : this(SwSeparatorImp(orientation))

    private class SwSeparatorImp(orientation: sgui.Orientation): JSeparator(when( orientation) {
        sgui.Orientation.HORIZONTAL -> SwingConstants.HORIZONTAL
        sgui.Orientation.VERTICAL -> SwingConstants.VERTICAL
    })
    {
        init {adaptMouseSystem()}
    }
}