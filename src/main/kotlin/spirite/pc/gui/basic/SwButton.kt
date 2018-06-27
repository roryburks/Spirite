package spirite.pc.gui.basic

import spirite.gui.components.basic.IButton
import spirite.gui.resources.IIcon
import spirite.gui.resources.Skin
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.border.BevelBorder


class SwButton
private constructor( val imp: SwButtonImp)
    : IButton, ISwComponent by SwComponent(imp)
{
    constructor(str: String? = null) : this(SwButtonImp(str))

    override fun setIcon(icon: IIcon) {imp.icon = icon.icon}

    override var action: (() -> Unit)?
        get() = imp.action
        set(value) { imp.action = value}

    private class SwButtonImp( str: String? = null) : JButton() {
        var action: (() -> Unit)? = null

        init {
            text = str
            background = Skin.Global.BgDark.jcolor
            foreground = Skin.Global.Text.jcolor
            border = BorderFactory.createBevelBorder(
                    BevelBorder.RAISED, Skin.BevelBorder.Med.jcolor, Skin.BevelBorder.Dark.jcolor)

            addActionListener { action?.invoke() }
        }
    }
}