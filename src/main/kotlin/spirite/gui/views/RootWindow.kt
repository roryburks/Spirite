package spirite.gui.views

import rbJvm.owl.addWeakObserver
import sguiSwing.components.SwMenuBar
import sguiSwing.components.jcomponent
import spirite.base.brains.Hotkey
import spirite.base.brains.IMasterControl
import spirite.base.brains.commands.DebugCommands
import spirite.base.brains.commands.DrawCommandExecutor.DrawCommand.*
import spirite.base.brains.commands.FrameCommandExecutor.FrameCommand.*
import spirite.base.brains.commands.GlobalCommands
import spirite.base.brains.commands.SelectCommand.All
import spirite.base.brains.commands.SelectCommand.Invert
import spirite.base.brains.commands.SelectCommand.None
import spirite.base.brains.commands.WorkspaceCommands
import spirite.gui.components.advanced.omniContainer.OmniContainer
import spirite.gui.components.advanced.omniContainer.OmniSegment
import spirite.gui.components.advanced.omniContainer.OmniTab
import spirite.gui.components.advanced.omniContainer.SubContainer
import spirite.gui.implementations.topLevelFeedback.TopLevelPopupView
import spirite.gui.menus.MenuItem
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
                MenuItem(".&Import"),
                MenuItem("..Import &AAF", GlobalCommands.ImportAaf),
                MenuItem(".-"),
                MenuItem(".&Save Workspace", GlobalCommands.SaveWorkspace),
                MenuItem(".Save Workspace &As...", GlobalCommands.SaveWorkspaceAs),
                MenuItem(".-"),
                MenuItem(".Export Image", GlobalCommands.Export),
                MenuItem(".Export Image As...", GlobalCommands.ExportAs),

                MenuItem("&Edit"),
                MenuItem(".&Undo", UNDO),
                MenuItem(".&Redo", REDO),


                MenuItem("&Layer"),
                MenuItem(".Auto&crop Layer", AUTO_CROP),
                MenuItem(".Layer to &Image Size"),

                MenuItem("&Select"),
                MenuItem(".&All", All),
                MenuItem(".&None", None),
                MenuItem(".&Invert Selection", Invert),

                MenuItem("&Image"),
                MenuItem(".&Invert", INVERT),
                MenuItem(".&To Color"),
                MenuItem(".&Resize Workspace", WorkspaceCommands.ResizeWorkspace),

                MenuItem("&Window"),
                MenuItem(".&Dialogs"),
                MenuItem("..&Layers"),
                MenuItem("..&Tools"),
                MenuItem("..-"),
                MenuItem("..Animation &Scheme"),
                MenuItem("..Undo &History", UNDO_HISTORY),
                MenuItem("..&Reference Scheme"),

                MenuItem(".&Animation View", ANIMATION),
                MenuItem(".Animation &State View", ANIMATION_STATE),

                MenuItem("&Settings"),
                MenuItem(".Manage &Hotkeys"),
                MenuItem(".&Tablet Settings"),
                MenuItem(".&Debug Stats", DEBUG),
                MenuItem(".Toggle &GL Mode"),
                MenuItem(".Toggle GL Panel"),
                MenuItem(".&__DB_GL"),

                MenuItem("&Debug"),
                MenuItem(".Commands To Clipboard", DebugCommands.CommandHistoryToClipboard),
                MenuItem(".Brk", DebugCommands.Breakpoint),
                MenuItem(".Cycle Sprite", DebugCommands.CycleSpriteParts)

        )

        val bar = SwMenuBar()
        SwContextMenus(master.commandExecutor).constructMenu(bar, scheme)
        jMenuBar = bar
    }

    // Structure and Reference Region:
    // Note: when I have the Omni Components and containers more fleshed out, getting these will be more
    // the job of the FrameManager than the Root View
    val groupView = GroupView(master)
    val topLevelView = TopLevelPopupView()
    val toolSection = ToolSection(master)
    val animStrutView = AnimationStructureView(master)
    val toolSettingSection = ToolSettingsSection(master)
    val paletteSection = PaletteSection(master)
    val refView = ReferenceView()
    val animListView = AnimationListView(master)
    val layerPropertiesView = LayerPropertiesPanel(master)

    private val omni = OmniContainer {
        left += OmniSegment(groupView, 100, 300)
        center = SubContainer(200,200) {
            center = OmniSegment(workTabPane, 200)
            bottom += OmniSegment(animStrutView, 100, 200, false)
        }
        right += SubContainer(100, 120) {
            top += OmniSegment(toolSection, 64, 100)
            top += OmniSegment(toolSettingSection, 200, 200)

            center = OmniSegment( paletteSection , 100)
        }
        right += SubContainer(100,120) {
            center = OmniTab(listOf(refView, animListView), 100)
            bottom += OmniSegment( layerPropertiesView, 200)
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