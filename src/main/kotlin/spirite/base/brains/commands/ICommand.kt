package spirite.base.brains.commands

import spirite.base.brains.KeyCommand

interface ICommand {
    val commandString : String
    val keyCommand: KeyCommand
}

