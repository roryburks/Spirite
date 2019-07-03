package spirite.gui.menus.dialogs

import sgui.components.IComponent

interface IDialogPanel<T> : IComponent {

    val result : T
}