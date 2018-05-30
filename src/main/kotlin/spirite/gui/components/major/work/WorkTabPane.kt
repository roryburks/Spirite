package spirite.gui.components.major.work

import spirite.base.brains.IMasterControl
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.brains.MasterControl
import spirite.base.imageData.IImageWorkspace
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.ITabbedPane
import spirite.gui.resources.IIcon
import spirite.hybrid.Hybrid
import javax.swing.SwingUtilities


class WorkTabPane
constructor(val master: IMasterControl, private val tabPane: ITabbedPane)
    : IOmniComponent
{
    override val component: IComponent get() = tabPane
    override val icon: IIcon? get() = null

    constructor(master: IMasterControl) : this( master, Hybrid.ui.TabbedPane())

    val workSection = WorkSection(master)
    private val workspaces = mutableListOf<IImageWorkspace>()
    private val containers = mutableListOf<ICrossPanel>()

    init {
        tabPane.selectedIndexBind.addListener {new, old ->
            if( old != -1) {
                master.workspaceSet.currentWorkspace = workspaces.getOrNull(new)
            }
            containers.forEach { it.clearLayout() }
            containers.getOrNull(new)?.setLayout { rows.add(workSection) }
        }
    }

    var i = 0

    val workspaceObserver = object : WorkspaceObserver {
        override fun workspaceCreated(newWorkspace: IImageWorkspace) {
            val title = "TODO${i++}"

            workspaces.add(newWorkspace)
            val newContainer = Hybrid.ui.CrossPanel()
            containers.add(newContainer)
            tabPane.addTab(title, newContainer)
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
    }.apply { master.workspaceSet.workspaceObserver.addObserver(this) }
}