package spirite.base.brains

import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.imageData.IImageWorkspace
import kotlin.math.max

interface IWorkspaceSet {

    interface WorkspaceObserver {
        fun workspaceCreated( newWorkspace: IImageWorkspace)
        fun workspaceRemoved( removedWorkspace: IImageWorkspace)
        fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?)
    }
    val workspaceObserver : IObservable<WorkspaceObserver>
    val workspaces: List<IImageWorkspace>

    val currentWorkspaceBind : Bindable<IImageWorkspace?>
    var currentWorkspace : IImageWorkspace?
}
interface MWorkspaceSet : IWorkspaceSet {
    fun addWorkspace( workspace: IImageWorkspace, select: Boolean = true)
    fun removeWorkspace( workspace: IImageWorkspace)
}

class WorkspaceSet : MWorkspaceSet{
    override fun addWorkspace(workspace: IImageWorkspace, select: Boolean) {
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

    override val currentWorkspaceBind = Bindable<IImageWorkspace?>(null, {new, old ->
        workspaceObserver.trigger { it.workspaceChanged(new, old) }
    })
    override var currentWorkspace: IImageWorkspace? by currentWorkspaceBind


    override val workspaces = mutableListOf<IImageWorkspace>()

    override val workspaceObserver = Observable<WorkspaceObserver>()
}