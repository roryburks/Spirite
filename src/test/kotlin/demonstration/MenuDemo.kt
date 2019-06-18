package demonstration

import sguiSwing.components.SwMenuBar
import spirite.base.brains.MasterControl
import spirite.base.brains.commands.GlobalCommands
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.pc.menus.SwContextMenus
import javax.swing.JFrame

fun main( args: Array<String>) {
    DemoLauncher.launch(BlankFrameWithMenuFrame(MasterControl()))
}

class BlankFrameWithMenuFrame(val master: MasterControl) : JFrame() {
    init /* Menu */ {
        val scheme = listOf(
                MenuItem("&File"),
                MenuItem(".&Ping", GlobalCommands.SaveWorkspace),
                MenuItem(".P&ong", customAction = { println("Pong")})
        )

        val bar = SwMenuBar()
        SwContextMenus(master.commandExecutor).constructMenu(bar, scheme)
        jMenuBar = bar
    }
}