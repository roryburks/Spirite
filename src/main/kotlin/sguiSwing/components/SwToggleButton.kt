package sguiSwing.components

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import sgui.IIcon
import sgui.components.IComponent
import sgui.components.IComponent.BasicBorder.BEVELED_RAISED
import sgui.components.IToggleButton
import sguiSwing.SwIcon
import sguiSwing.mouseSystem.adaptMouseSystem
import sguiSwing.skin.Skin
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JToggleButton


open class SwToggleButton
protected constructor(startChecked: Boolean, private val imp: JToggleButton )
    : IToggleButton,
        IComponent,
        ISwComponent by SwComponent(imp)
{
    override val checkBind = Bindable(startChecked)
    override var checked by checkBind

    override fun setOnIcon(icon: IIcon) {imp.selectedIcon = (icon as? SwIcon)?.icon ?: return}
    override fun setOffIcon(icon: IIcon) {imp.icon = (icon as? SwIcon)?.icon ?: return}

    override fun setOnIconOver(icon: IIcon) {imp.rolloverSelectedIcon = (icon as? SwIcon)?.icon ?: return}
    override fun setOffIconOver(icon: IIcon) {imp.rolloverIcon = (icon as? SwIcon)?.icon ?: return }

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