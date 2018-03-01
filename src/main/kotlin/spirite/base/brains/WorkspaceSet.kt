package spirite.base.brains

import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.imageData.IImageWorkspace

interface IWorkspaceSet {

    interface WorkspaceObserver {
        fun workspaceCreated( newWorkspace: IImageWorkspace)
        fun workspaceRemoved( removedWorkspace: IImageWorkspace)
        fun workspaceChanged(selectedWorkspace: IImageWorkspace, previousSelected: IImageWorkspace)
    }
    val workspaceObserver : IObservable<WorkspaceObserver>
    val workspaces: List<IImageWorkspace>
}

class WorkspaceSet : IWorkspaceSet{


    // region Workspace Management
    override val workspaces = mutableListOf<IImageWorkspace>()

    override val workspaceObserver = Observable<WorkspaceObserver>()
    // endregion
}