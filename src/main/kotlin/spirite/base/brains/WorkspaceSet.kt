package spirite.base.brains

import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.imageData.IImageWorkspace

interface IWorkspaceSet {

    interface WorkspaceObserver {
        fun workspaceCreated( newWorkspace: IImageWorkspace)
        fun workspaceRemoved( removedWorkspace: IImageWorkspace)
        fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?)
    }
    val workspaceObserver : IObservable<WorkspaceObserver>
    val workspaces: List<IImageWorkspace>
    var currentWorkspace : IImageWorkspace?
}

class WorkspaceSet : IWorkspaceSet{
    override var currentWorkspace: IImageWorkspace? = null
        set(value) {
            val previous = field
            field = value
            workspaceObserver.trigger { it.workspaceChanged(value, previous) }
        }


    override val workspaces = mutableListOf<IImageWorkspace>()

    override val workspaceObserver = Observable<WorkspaceObserver>()
}