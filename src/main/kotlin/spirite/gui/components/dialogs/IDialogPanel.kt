package spirite.gui.components.dialogs

import spirite.gui.components.basic.IComponent

interface IDialogPanel<T> : IComponent {

    val result : T
}