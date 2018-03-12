package spirite.gui.basic

import spirite.gui.Skin
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.border.BevelBorder

interface IButton : IComponent {
    var action: (()->Unit)?
}

class SButton
private constructor( val imp: SButtonImp)
    : IButton, ISComponent by SComponentDirect(imp)
{
    constructor(str: String? = null) : this(SButtonImp(str))

    override var action: (() -> Unit)?
        get() = imp.action
        set(value) { imp.action = value}

    private class SButtonImp( str: String? = null) : JButton() {
        var action: (() -> Unit)? = null

        init {
            text = str
            background = Skin.Global.BgDark.color
            foreground = Skin.Global.Text.color
            border = BorderFactory.createBevelBorder(
                    BevelBorder.RAISED, Skin.BevelBorder.Med.color, Skin.BevelBorder.Dark.color)

            addActionListener { action?.invoke() }
        }
    }
}