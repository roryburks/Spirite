package sgui.swing.components

import rb.owl.bindable.addObserver
import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicBorder.BEVELED_RAISED
import sgui.generic.components.IToggleButton
import sgui.generic.components.IToggleButtonNonUI
import sgui.generic.components.ToggleButtonNonUI
import spirite.gui.resources.IIcon
import sgui.skin.Skin
import sgui.swing.adaptMouseSystem
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JToggleButton


open class SwToggleButton
protected constructor(startChecked: Boolean, private val imp: JToggleButton )
    : IToggleButton,
        IToggleButtonNonUI by ToggleButtonNonUI(startChecked), IComponent,
        ISwComponent by SwComponent(imp)
{

    override fun setOnIcon(icon: IIcon) {imp.selectedIcon = icon.icon}
    override fun setOffIcon(icon: IIcon) {imp.icon = icon.icon}

    override fun setOnIconOver(icon: IIcon) {imp.rolloverSelectedIcon = icon.icon}
    override fun setOffIconOver(icon: IIcon) {imp.rolloverIcon = icon.icon }

    constructor(startChecked: Boolean = false) : this(startChecked, SwToggleButtonImp())

    override var plainStyle: Boolean = false
        set(value) {
            if( value != field) {
                field = value
                imp.isBorderPainted = !value
                imp.isContentAreaFilled = !value
                imp.isFocusPainted = !value
                imp.isOpaque = !value
            }
        }



    init {
        checkBind.addObserver { new, _ -> imp.isSelected = new }
        setBasicBorder(BEVELED_RAISED)
        background = Skin.Global.BgDark.scolor


        imp.addMouseListener(object : MouseListener {
            override fun mouseReleased(e: MouseEvent?) { if( enabled && e?.button == MouseEvent.BUTTON1)
                checked = !checked
            }
            override fun mouseEntered(e: MouseEvent?) {}
            override fun mouseClicked(e: MouseEvent?) {}
            override fun mouseExited(e: MouseEvent?) {}
            override fun mousePressed(e: MouseEvent?) {}
        })
    }

    private class SwToggleButtonImp : JToggleButton()
    {
        init {
            mouseListeners.forEach { removeMouseListener(it)}
            adaptMouseSystem()
        }
    }
}