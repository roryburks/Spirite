package spirite.gui.components.basic

import spirite.gui.resources.IIcon

interface IButton : IComponent {
    var action: (()->Unit)?

    fun setIcon( icon: IIcon)
}
