package spirite.pc.gui.basic

import spirite.gui.resources.Skin
import spirite.gui.components.basic.IButton
import spirite.gui.resources.IIcon
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
            background = Skin.Global.BgDark.color
            foreground = Skin.Global.Text.color
            border = BorderFactory.createBevelBorder(
                    BevelBorder.RAISED, Skin.BevelBorder.Med.color, Skin.BevelBorder.Dark.color)

            addActionListener { action?.invoke() }
        }
    }
}