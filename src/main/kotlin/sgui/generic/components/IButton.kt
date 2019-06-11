package sgui.generic.components

import sgui.generic.IIcon

interface IButton : IComponent {
    data class ButtonActionEvent(
            val pressingShift: Boolean,
            val pressingAlt: Boolean,
            val pressingCtrl: Boolean)

    var action: ((ButtonActionEvent)->Unit)?

    fun setIcon( icon: IIcon)
}
