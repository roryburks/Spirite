package spirite.base.brains

import rb.owl.IObservable
import rb.owl.Observable
import rb.owl.bindable.Bindable
import rb.owl.bindable.IBindable
import rb.owl.bindable.addObserver
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MImageWorkspace
import spirite.base.util.binding.CruddyBindable
import kotlin.math.max

interface IWorkspaceSet {

    interface WorkspaceObserver {
        fun workspaceCreated( newWorkspace: IImageWorkspace)
        fun workspaceRemoved( removedWorkspace: IImageWorkspace)
        fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?)
    }
    val workspaceObserver : IObservable<WorkspaceObserver>
    val workspaces: List<IImageWorkspace>

    val currentWorkspaceBind : IBindable<IImageWorkspace?>
    var currentWorkspace : IImageWorkspace?
}
interface MWorkspaceSet : IWorkspaceSet {
    val currentMWorkspace : MImageWorkspace?
    fun addWorkspace(workspace: MImageWorkspace, select: Boolean = true)
    fun removeWorkspace( workspace: IImageWorkspace)
}

class WorkspaceSet : MWorkspaceSet{
    override fun addWorkspace(workspace: MImageWorkspace, select: Boolean) {
        workspaces.add(workspace)
        workspaceObserver.trigger { it.workspaceCreated(workspace)}

        if(select || currentWorkspace == null) {
            currentWorkspace = workspace
        }
    }

    override fun removeWorkspace(workspace: IImageWorkspace) {
        val indexOf = workspaces.indexOf(workspace)
        workspaceObserver.trigger { it.workspaceRemoved(workspace) }

        if(workspaces.remove(workspace) && currentWorkspace == workspace) {
            currentWorkspace = workspaces.getOrNull( max(0, indexOf-1))
        }
    }

    override val currentWorkspaceBind = Bindable<IImageWorkspace?>(null)
            .also { bind -> bind.addObserver(false) { new, old ->  workspaceObserver.trigger { it.workspaceChanged(new, old) }} }

    override var currentWorkspace: IImageWorkspace? by currentWorkspaceBind

    override val currentMWorkspace: MImageWorkspace? get() = currentWorkspaceBind.field as? MImageWorkspace // Badish

    override val workspaces = mutableListOf<MImageWorkspace>()

    override val workspaceObserver = Observable<WorkspaceObserver>()
}