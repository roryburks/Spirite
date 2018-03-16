package spirite.gui.components.major

import spirite.base.brains.MasterControl
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.pc.gui.basic.SwMenuBar
import spirite.pc.gui.menus.SwContextMenus
import javax.swing.JFrame

class RootWindow( val master: MasterControl) : JFrame() {
    init /* Menu */ {
        val scheme = listOf(
                MenuItem("&File"),
                MenuItem(".&New Image", GlobalCommand.PING),
                MenuItem(".-"),
                MenuItem(".&Open"),
                MenuItem(".-"),
                MenuItem(".&Save Workspace"),
                MenuItem(".Save Workspace &As..."),
                MenuItem(".-"),
                MenuItem(".Export Image"),
                MenuItem(".Export Image As..."),

                MenuItem("&Edit"),
                MenuItem(".&Undo"),
                MenuItem(".&Redo"),


                MenuItem("&Layer"),
                MenuItem(".Auto&crop Layer"),
                MenuItem(".Layer to &Image Size"),

                MenuItem("&Select"),
                MenuItem(".&All"),
                MenuItem(".&None"),
                MenuItem(".&Invert Selection (unimplemented)"),

                MenuItem("&Image"),
                MenuItem(".&Invert"),
                MenuItem(".&To Color"),
                MenuItem(".&Resize Workspace"),

                MenuItem("&Window"),
                MenuItem(".&Dialogs"),
                MenuItem("..&Layers"),
                MenuItem("..&Tools"),
                MenuItem("..-"),
                MenuItem("..Animation &Scheme"),
                MenuItem("..Undo &History"),
                MenuItem("..&Reference Scheme"),

                MenuItem(".&Animation View"),

                MenuItem("&Settings"),
                MenuItem(".Manage &Hotkeys"),
                MenuItem(".&Tablet Settings"),
                MenuItem(".&Debug Stats"),
                MenuItem(".Toggle &GL Mode"),
                MenuItem(".Toggle GL Panel"),
                MenuItem(".&__DB_GL")
        )

        val bar = SwMenuBar()
        SwContextMenus(master.commandExecuter).constructMenu(bar, scheme)
        jMenuBar = bar
    }

}