package spirite.gui.components.dialogs

import sgui.generic.components.IComponent

interface IDialogPanel<T> : IComponent {

    val result : T
}