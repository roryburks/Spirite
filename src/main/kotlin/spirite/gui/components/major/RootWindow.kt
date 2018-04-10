package spirite.gui.components.major

import spirite.base.brains.Hotkey
import spirite.base.brains.IMasterControl
import spirite.base.brains.MasterControl
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand
import spirite.gui.components.advanced.omniContainer.OmniContainer
import spirite.gui.components.advanced.omniContainer.OmniSegment
import spirite.gui.components.advanced.omniContainer.SubContainer
import spirite.gui.components.major.groupView.GroupView
import spirite.gui.components.major.tool.PaletteSection
import spirite.gui.components.major.tool.ToolSection
import spirite.gui.components.major.tool.ToolSettingsSection
import spirite.gui.components.major.work.WorkTabPane
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwMenuBar
import spirite.pc.gui.basic.jcomponent
import spirite.pc.gui.menus.SwContextMenus
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities

class RootWindow( val master: IMasterControl) : JFrame() {
    val view get() = workTabPane.workSection.currentView
    private val workTabPane =  WorkTabPane(master)

    init /* Menu */ {
        val scheme = listOf(
                MenuItem("&File"),
                MenuItem(".&New Image", GlobalCommand.PING),
                MenuItem(".-"),
                MenuItem(".&Open", GlobalCommand.OPEN),
                MenuItem(".-"),
                MenuItem(".&Save Workspace", GlobalCommand.SAVE_WORKSPACE),
                MenuItem(".Save Workspace &As...", GlobalCommand.SAVE_WORKSPACE_AS),
                MenuItem(".-"),
                MenuItem(".Export Image", GlobalCommand.EXPORT),
                MenuItem(".Export Image As...", GlobalCommand.EXPORT_AS),

                MenuItem("&Edit"),
                MenuItem(".&Undo", DrawCommand.UNDO),
                MenuItem(".&Redo", DrawCommand.REDO),


                MenuItem("&Layer"),
                MenuItem(".Auto&crop Layer", DrawCommand.AUTO_CROP),
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

    init /* Layout */ {
        this.layout = GridLayout()

        this.title = "Spirite"

        val omni = OmniContainer {
            left += OmniSegment(GroupView(master), 100, 150)
            center = OmniSegment( workTabPane, 200)
            right += SubContainer( {
                top += OmniSegment(ToolSection(master), 100, 200)
                top += OmniSegment(ToolSettingsSection(master), 100, 100)

                center = OmniSegment( PaletteSection(master), 100)
            }, 100, 120)
            right += SubContainer( {
                center = OmniSegment( Hybrid.ui.Label("Reference"), 100)
                bottom += OmniSegment( Hybrid.ui.Label("LayerInfo"), 100)
            }, 100, 120)
        }

        this.add( omni.jcomponent)

        SwingUtilities.invokeLater {this.size = Dimension(800,600) }
    }

    init /* Bindings */ {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { evt ->
            if( evt.id == KeyEvent.KEY_PRESSED) {
                val key = evt.keyCode
                val modifier = evt.modifiersEx

                val command = master.hotkeyManager.getCommand(Hotkey(key,modifier))

                command?.apply { master.commandExecuter.executeCommand(this, null) }
            }

            false
        }
    }
}