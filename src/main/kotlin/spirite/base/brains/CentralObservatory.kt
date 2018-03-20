package spirite.base.brains

import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.groupTree.GroupTree.TreeObserver
import java.lang.ref.WeakReference

/** The CentralObservatory is a place where things (primarily GUI components) which need to watch for certain changes
 * regardless of which Workspace is active should get their Observables from.  It automatically adds and removes ovservers
 * as the currentWorkspace is changed.*/
interface ICentralObservatory {
    val trackingImageObserver : IObservable<ImageObserver>
    val omniImageObserver : IObservable<ImageObserver>
    val trackingPrimaryTreeObserver : IObservable<TreeObserver>
}

class CentralObservatory(private val workspaceSet : IWorkspaceSet)
    : ICentralObservatory
{
    private val trackingObservers  = mutableListOf<TrackingObserver<*>>()
    private val omniObserver  = mutableListOf<OmniObserver<*>>()

    override val trackingImageObserver = TrackingObserver {it.imageObservatory.imageObservers}
    override val omniImageObserver: IObservable<ImageObserver> = OmniObserver { it.imageObservatory.imageObservers }
    override val trackingPrimaryTreeObserver: IObservable<TreeObserver> = TrackingObserver { it.groupTree.treeObs }

    init {
        // Note: In order to cut down on code which could easily be forgotten/broken, TrackingObservers automatically
        //  add themselves to an internal list which is then linked to the workspace observer automaticlaly.  But this
        //  means order is important.  In particular
        //      1) [trackingObservers' Initialization]
        //      2) [each individual TrackingObserver's initialization]
        //      3) [this init block]
        trackingObservers.forEach { workspaceSet.workspaceObserver.addObserver(it) }
    }

    inner class OmniObserver<T>(
            val observerFinder : (IImageWorkspace) -> IObservable<T>)
        : IObservable<T>, WorkspaceObserver
    {
        init {
            omniObserver.add(this)
        }
        private val observers = mutableListOf<WeakReference<T>>()

        override fun addObserver(toAdd: T) {observers.add( WeakReference(toAdd))}
        override fun removeObserver(toRemove: T) {
            observers.removeIf {
                val obs = it.get()
                obs == null || obs == toRemove
            }
        }

        override fun workspaceCreated(newWorkspace: IImageWorkspace) {
            val observable = observerFinder.invoke(newWorkspace)
            observers.removeIf {
                val obs = it.get()
                if( obs != null)
                    observable.addObserver(obs)
                obs == null
            }
        }
        override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {
            val observable = observerFinder.invoke(removedWorkspace)
            observers.removeIf {
                val obs = it.get()
                if( obs != null)
                    observable.removeObserver(obs)
                obs == null
            }
        }
        override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {}
    }

    inner class TrackingObserver<T>(
            val observerFinder : (IImageWorkspace) -> IObservable<T>)
        : IObservable<T>, WorkspaceObserver
    {
        init {
            trackingObservers.add(this)
        }
        private val observers = mutableListOf<WeakReference<T>>()

        override fun addObserver(toAdd: T) {
            val workspace = workspaceSet.currentWorkspace
            if( workspace != null) observerFinder.invoke(workspace).addObserver(toAdd)
            observers.add( WeakReference(toAdd))
        }
        override fun removeObserver(toRemove: T) {
            val workspace = workspaceSet.currentWorkspace
            if( workspace != null) observerFinder.invoke(workspace).removeObserver(toRemove)
            observers.removeIf {
                val obs = it.get()
                obs == null || obs == toRemove
            }
        }

        override fun workspaceCreated(newWorkspace: IImageWorkspace) {}
        override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {}
        override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {
            if( previousSelected != null) {
                val observable = observerFinder.invoke(previousSelected)
                observers.removeIf {
                    val obs = it.get()
                    if( obs != null)
                        observable.removeObserver(obs)
                    obs == null
                }
            }
            if( selectedWorkspace != null) {
                val observable = observerFinder.invoke(selectedWorkspace)
                observers.removeIf {
                    val obs = it.get()
                    if( obs != null)
                        observable.addObserver(obs)
                    obs == null
                }
            }
        }
    }
}
