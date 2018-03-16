package demonstration

import spirite.base.brains.MasterControl
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.PING
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.pc.gui.basic.SwMenuBar
import spirite.pc.gui.menus.SwContextMenus
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(BlankFrameWithMenuFrame(MasterControl()))
}

class BlankFrameWithMenuFrame(val master: MasterControl) : JFrame() {
    init /* Menu */ {
        val scheme = listOf(
                MenuItem("&File"),
                MenuItem(".&Ping", PING),
                MenuItem(".P&ong", customAction = { println("Pong")})
        )

        val bar = SwMenuBar()
        SwContextMenus(master.commandExecuter).constructMenu(bar, scheme)
        jMenuBar = bar
    }
}