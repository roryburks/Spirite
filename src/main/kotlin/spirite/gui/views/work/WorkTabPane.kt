package spirite.gui.views.work

import rb.owl.bindable.addObserver
import rbJvm.owl.addWeakObserver
import sgui.components.IComponent
import sgui.core.components.ITabbedPane
import sgui.core.components.crossContainer.ICrossPanel
import sgui.core.components.events.MouseEvent
import sgui.swing.SwIcon
import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.brains.commands.GlobalCommands
import spirite.base.imageData.IImageWorkspace
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.menus.MenuItem
import spirite.sguiHybrid.Hybrid


class WorkTabPane
constructor(val master: IMasterControl, private val tabPane: ITabbedPane)
    : IOmniComponent
{
    override val component: IComponent get() = tabPane
    override val icon: SwIcon? get() = null
    override val name: String get() = "Work Area"

    constructor(master: IMasterControl) : this( master, Hybrid.ui.TabbedPane())

    val workSection = WorkSection(master)
    private val workspaces = mutableListOf<IImageWorkspace>()
    private val containers = mutableListOf<ICrossPanel>()

    init {
        tabPane.selectedIndexBind.addObserver { new, old ->
            if( old != -1) {
                master.workspaceSet.currentWorkspace = workspaces.getOrNull(new)
            }
            containers.forEach { it.clearLayout() }
            containers.getOrNull(new)?.setLayout { rows.add(workSection) }
        }

        tabPane.setMouseListener { tab, mouseEvent ->
            if( mouseEvent.button == MouseEvent.MouseButton.RIGHT && mouseEvent.type == MouseEvent.MouseEventType.CLICKED) {
                val ws = master.workspaceSet.workspaces.getOrNull(tab) ?: return@setMouseListener
                val scheme = listOf<MenuItem>(
                    MenuItem("Close Workspace", GlobalCommands.CloseWorkspace)
                )
                master.contextMenus.LaunchContextMenu(mouseEvent.point, scheme, ws)
            }
        }
    }

    var i = 0

    val workspaceObserverContract = master.workspaceSet.workspaceObserver.addWeakObserver(
            object : WorkspaceObserver {
                override fun workspaceCreated(newWorkspace: IImageWorkspace) {
                    val title = "TODO${i++}"

                    workspaces.add(newWorkspace)
                    val newContainer = Hybrid.ui.CrossPanel()
                    containers.add(newContainer)
                    tabPane.addTab(title, newContainer)

                    // TODO/Note: These are never-unbound strong-reference listeners, but WorkTabPane is probably a
                    //  1-1 with the Proc.
                    newWorkspace.displayedFilenameBind.addObserver { new, _ ->
                        val ind = tabPane.components.indexOf(newContainer)
                        tabPane.setTitleAt(ind, new)
                    }
                }

                override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {
                    val index = workspaces.indexOf(removedWorkspace)
                    if( index != -1) {
                        tabPane.removeTabAt(index)
                        containers.removeAt(index)
                        workspaces.removeAt(index)
                    }
                }

                override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {
                    val newIndex = workspaces.indexOf(selectedWorkspace)
                    tabPane.selectedIndex = newIndex
                }
            }
    )
}