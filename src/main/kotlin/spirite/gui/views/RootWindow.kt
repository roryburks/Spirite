package spirite.gui.views

import rbJvm.owl.addWeakObserver
import sgui.swing.components.SwMenuBar
import sgui.swing.components.jcomponent
import spirite.base.brains.Hotkey
import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand
import spirite.base.brains.commands.FrameCommandExecutor.FrameCommand
import spirite.base.brains.commands.GlobalCommands
import spirite.base.brains.commands.SelectCommand
import spirite.base.brains.commands.WorkspaceCommands
import spirite.gui.components.advanced.omniContainer.OmniContainer
import spirite.gui.components.advanced.omniContainer.OmniSegment
import spirite.gui.components.advanced.omniContainer.OmniTab
import spirite.gui.components.advanced.omniContainer.SubContainer
import spirite.gui.implementations.topLevelFeedback.TopLevelPopupView
import spirite.gui.menus.ContextMenus.MenuItem
import spirite.gui.views.animation.AnimationListView
import spirite.gui.views.animation.structureView.AnimationStructureView
import spirite.gui.views.groupView.GroupView
import spirite.gui.views.groupView.ReferenceView
import spirite.gui.views.layerProperties.LayerPropertiesPanel
import spirite.gui.views.tool.PaletteSection
import spirite.gui.views.tool.ToolSection
import spirite.gui.views.tool.ToolSettingsSection
import spirite.gui.views.work.WorkTabPane
import spirite.hybrid.Hybrid
import spirite.hybrid.SwHybrid
import spirite.pc.menus.SwContextMenus
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
                MenuItem(".&New Image", GlobalCommands.NewWorkspace),
                MenuItem(".-"),
                MenuItem(".&Open", GlobalCommands.Open),
                MenuItem(".-"),
                MenuItem(".&Save Workspace", GlobalCommands.SaveWorkspace),
                MenuItem(".Save Workspace &As...", GlobalCommands.SaveWorkspaceAs),
                MenuItem(".-"),
                MenuItem(".Export Image", GlobalCommands.Export),
                MenuItem(".Export Image As...", GlobalCommands.ExportAs),

                MenuItem("&Edit"),
                MenuItem(".&Undo", DrawCommand.UNDO),
                MenuItem(".&Redo", DrawCommand.REDO),


                MenuItem("&Layer"),
                MenuItem(".Auto&crop Layer", DrawCommand.AUTO_CROP),
                MenuItem(".Layer to &Image Size"),

                MenuItem("&Select"),
                MenuItem(".&All", SelectCommand.All),
                MenuItem(".&None", SelectCommand.None),
                MenuItem(".&Invert Selection", SelectCommand.Invert),

                MenuItem("&Image"),
                MenuItem(".&Invert", DrawCommand.INVERT),
                MenuItem(".&To Color"),
                MenuItem(".&Resize Workspace", WorkspaceCommands.ResizeWorkspace),

                MenuItem("&Window"),
                MenuItem(".&Dialogs"),
                MenuItem("..&Layers"),
                MenuItem("..&Tools"),
                MenuItem("..-"),
                MenuItem("..Animation &Scheme"),
                MenuItem("..Undo &History", FrameCommand.UNDO_HISTORY),
                MenuItem("..&Reference Scheme"),

                MenuItem(".&Animation View", FrameCommand.ANIMATION),
                MenuItem(".Animation &State View", FrameCommand.ANIMATION_STATE),

                MenuItem("&Settings"),
                MenuItem(".Manage &Hotkeys"),
                MenuItem(".&Tablet Settings"),
                MenuItem(".&Debug Stats", FrameCommand.DEBUG),
                MenuItem(".Toggle &GL Mode"),
                MenuItem(".Toggle GL Panel"),
                MenuItem(".&__DB_GL")
        )

        val bar = SwMenuBar()
        SwContextMenus(master.commandExecutor).constructMenu(bar, scheme)
        jMenuBar = bar
    }

    val groupView = GroupView(master)
    val topLevelView = TopLevelPopupView()

    private val omni = OmniContainer {
        left += OmniSegment(groupView, 100, 300)
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
            center = OmniTab(listOf(ReferenceView(), AnimationListView(master)), 100)
            bottom += OmniSegment( LayerPropertiesPanel(master), 200)
        }
    }

    init /* Layout */ {
        this.layout = GridLayout()

        this.title = "Spirite"

        val multiLevel = Hybrid.ui.CrossPanel {
            // TODO: Fix with mouse input
//            rows.addFlatGroup {
//                add(topLevelView, flex = 1f)
//                flex = 1f
//            }
            rows.addFlatGroup {
                add(omni, flex = 1f)
                flex = 1f
            }
        }
        this.add( multiLevel.jcomponent)

        SwingUtilities.invokeLater {this.size = Dimension(800,600) }
        SwingUtilities.invokeLater {groupView.component.jcomponent.requestFocus() }
    }

    init /* Bindings */ {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { evt ->
            if(focusOwner == null || !Hybrid.keypressSystem.hotkeysEnabled)
                return@addKeyEventDispatcher false

            Hybrid.gle.runInGLContext {
                when (evt.id) {
                    KeyEvent.KEY_PRESSED -> {
                        if (evt.keyCode == KeyEvent.VK_SPACE)
                            SwHybrid.keypressSystem.holdingSpace = true
                        val key = evt.keyCode
                        val modifier = evt.modifiersEx

                        val command = master.hotkeyManager.getCommand(Hotkey(key, modifier))
                        command?.apply { master.commandExecutor.executeCommand(this.commandString, this.objectCreator?.invoke(master)) }
                    }
                    KeyEvent.KEY_RELEASED -> {
                        if (evt.keyCode == KeyEvent.VK_SPACE)
                            SwHybrid.keypressSystem.holdingSpace = false
                    }
                }
            }

            false
        }
    }

    private val currentAnimationK = master.centralObservatory.currentAnimationBind.addWeakObserver { new, old ->
        // TODO: Was clearly incomplete before, not sure what is/was supposed to go here.
        if( new == null && old != null) {
            omni
        }
    }
}