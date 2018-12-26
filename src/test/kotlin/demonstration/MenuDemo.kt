package demonstration

import spirite.base.brains.MasterControl
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand.SAVE_WORKSPACE
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
                MenuItem(".&Ping", SAVE_WORKSPACE),
                MenuItem(".P&ong", customAction = { println("Pong")})
        )

        val bar = SwMenuBar()
        SwContextMenus(master.commandExecutor).constructMenu(bar, scheme)
        jMenuBar = bar
    }
}