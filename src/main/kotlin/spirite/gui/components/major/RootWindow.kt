package spirite.gui.components.major

import spirite.base.brains.Hotkey
import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.FrameCommandExecuter.FrameCommand
import spirite.base.brains.commands.GlobalCommandExecuter.GlobalCommand
import spirite.base.brains.commands.SelectionCommandExecuter.SelectCommand
import spirite.gui.components.advanced.omniContainer.OmniContainer
import spirite.gui.components.advanced.omniContainer.OmniSegment
import spirite.gui.components.advanced.omniContainer.OmniTab
import spirite.gui.components.advanced.omniContainer.SubContainer
import spirite.gui.components.major.animation.AnimationSchemeView
import spirite.gui.components.major.animation.AnimationStructureView
import spirite.gui.components.major.groupView.GroupView
import spirite.gui.components.major.groupView.ReferenceView
import spirite.gui.components.major.layerProperties.LayerPropertiesPanel
import spirite.gui.components.major.tool.PaletteSection
import spirite.gui.components.major.tool.ToolSection
import spirite.gui.components.major.tool.ToolSettingsSection
import spirite.gui.components.major.work.WorkTabPane
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.pc.gui.basic.SwMenuBar
import spirite.pc.gui.basic.jcomponent
import spirite.pc.gui.menus.SwContextMenus
import java.awt.Dimension
import java.awt.GridLayout
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
                MenuItem(".&New Image", GlobalCommand.NEW_WORKSPACE),
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
                MenuItem(".&All", SelectCommand.ALL),
                MenuItem(".&None", SelectCommand.NONE),
                MenuItem(".&Invert Selection", SelectCommand.INVERT),

                MenuItem("&Image"),
                MenuItem(".&Invert", DrawCommand.INVERT),
                MenuItem(".&To Color"),
                MenuItem(".&Resize Workspace"),

                MenuItem("&Window"),
                MenuItem(".&Dialogs"),
                MenuItem("..&Layers"),
                MenuItem("..&Tools"),
                MenuItem("..-"),
                MenuItem("..Animation &Scheme"),
                MenuItem("..Undo &History", FrameCommand.UNDO_HISTORY),
                MenuItem("..&Reference Scheme"),

                MenuItem(".&Animation View", FrameCommand.ANIMATION),

                MenuItem("&Settings"),
                MenuItem(".Manage &Hotkeys"),
                MenuItem(".&Tablet Settings"),
                MenuItem(".&Debug Stats", FrameCommand.DEBUG),
                MenuItem(".Toggle &GL Mode"),
                MenuItem(".Toggle GL Panel"),
                MenuItem(".&__DB_GL")
        )

        val bar = SwMenuBar()
        SwContextMenus(master.commandExecuter).constructMenu(bar, scheme)
        jMenuBar = bar
    }

    private val omni = OmniContainer {
        left += OmniSegment(GroupView(master), 100, 150)
        center = SubContainer(200,200) {
            center = OmniSegment(workTabPane, 200)
            bottom += OmniSegment(AnimationStructureView(master), 100, 200, false)
        }
        right += SubContainer(100, 120) {
            top += OmniSegment(ToolSection(master), 64, 100)
            top += OmniSegment(ToolSettingsSection(master), 200, 200)

            center = OmniSegment( PaletteSection(master), 100)
        }
        right += SubContainer(100,120) {
            center = OmniTab(listOf(ReferenceView(), AnimationSchemeView(master)), 100)
            bottom += OmniSegment( LayerPropertiesPanel(master), 200)
        }
    }

    init /* Layout */ {
        this.layout = GridLayout()

        this.title = "Spirite"

        this.add( omni.jcomponent)

        SwingUtilities.invokeLater {this.size = Dimension(800,600) }
    }

    init /* Bindings */ {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { evt ->
            when(evt.id) {
                KeyEvent.KEY_PRESSED -> {
                    if( evt.keyCode == KeyEvent.VK_SPACE) workTabPane.workSection.penner.holdingSpace = true
                    val key = evt.keyCode
                    val modifier = evt.modifiersEx

                    val command = master.hotkeyManager.getCommand(Hotkey(key,modifier))
                    command?.apply { master.commandExecuter.executeCommand(this.commandString, this.objectCreator?.invoke(master)) }
                }
                KeyEvent.KEY_RELEASED -> {
                    if( evt.keyCode == KeyEvent.VK_SPACE) workTabPane.workSection.penner.holdingSpace = false
                }
            }

            false
        }

        master.centralObservatory.currentAnimationBind.addListener { new, old ->
            if( new == null && old != null) {
                omni
            }

        }
    }
}