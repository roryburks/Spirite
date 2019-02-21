package spirite.pc.gui.basic

import spirite.gui.Orientation
import spirite.gui.components.basic.ISeparator
import spirite.pc.gui.adaptMouseSystem
import javax.swing.JSeparator
import javax.swing.SwingConstants

class SwSeparator
private constructor(val imp : SwSeparatorImp) : ISeparator, ISwComponent by SwComponent(imp)
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