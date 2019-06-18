package spirite.gui.components.dialogs

import sgui.components.IComponent

interface IDialogPanel<T> : IComponent {

    val result : T
}