package cwShared.dialogSystem

import sgui.components.IComponent

interface IDialogPanel<T> : IComponent {

    val result : T
}