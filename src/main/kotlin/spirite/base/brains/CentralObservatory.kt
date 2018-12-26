package spirite.base.brains

import rb.owl.IContract
import rb.owl.IObservable
import rb.owl.IObserver
import rb.owl.bindable.IBindObserver
import rb.owl.bindable.IBindable
import rb.owl.bindable.OnChangeEvent
import rb.owl.bindable.addObserver
import rb.owl.observer
import spirite.base.brains.IWorkspaceSet.WorkspaceObserver
import spirite.base.imageData.IImageObservatory.ImageObserver
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MediumHandle
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationObserver
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animationSpaces.AnimationSpace
import spirite.base.imageData.groupTree.GroupTree.Node
import spirite.base.imageData.groupTree.GroupTree.TreeObserver
import spirite.base.imageData.undo.IUndoEngine.UndoHistoryChangeEvent
import spirite.base.util.binding.ICruddyOldBindable
import spirite.base.util.binding.ICruddyBoundListener
import spirite.base.util.binding.CruddyOldOnChangeEvent
import java.lang.ref.WeakReference

/** The CentralObservatory is a place where things (primarily GUI components) which need to watch for certain changes
 * regardless of which Workspace is active should get their Observables from.  It automatically adds and removes ovservers
 * as the currentWorkspace is changed.*/
interface ICentralObservatory {
    val omniImageObserver : ICruddyOldObservable<ImageObserver>

    val trackingUndoHistoryObserver : ICruddyOldObservable<(UndoHistoryChangeEvent)->Any?>
    val trackingImageObserver : ICruddyOldObservable<ImageObserver>
    val trackingPrimaryTreeObserver : ICruddyOldObservable<TreeObserver>
    val trackingAnimationObservable : ICruddyOldObservable<AnimationObserver>
    val trackingAnimationStateObserver: ICruddyOldObservable<AnimationStructureChangeObserver>

    val activeDataBind : IBindable<MediumHandle?>
    val selectedNode : IBindable<Node?>
    val currentAnimationBind : ICruddyOldBindable<Animation?>
    val currentAnimationSpaceBind : ICruddyOldBindable<AnimationSpace?>
}

class CentralObservatory(private val workspaceSet : IWorkspaceSet)
    : ICentralObservatory
{
    private val trackingObservers  = mutableListOf<TrackingObserver<*>>()
    private val omniObserver  = mutableListOf<OmniObserver<*>>()

    override val omniImageObserver: ICruddyOldObservable<ImageObserver> = OmniObserver { it.imageObservatory.imageObservable }

    override val trackingUndoHistoryObserver: ICruddyOldObservable<(UndoHistoryChangeEvent) -> Any?> = TrackingObserver { it.undoEngine.undoHistoryObserver }
    override val trackingImageObserver = TrackingObserver {it.imageObservatory.imageObservable}
    override val trackingPrimaryTreeObserver: ICruddyOldObservable<TreeObserver> = TrackingObserver { it.groupTree.treeObservable }
    override val trackingAnimationObservable: ICruddyOldObservable<AnimationObserver> = TrackingObserver { it.animationManager.animationObservable }
    override val trackingAnimationStateObserver: ICruddyOldObservable<AnimationStructureChangeObserver> = TrackingObserver { it.animationManager.animationStructureChangeObservable }

    override val activeDataBind: IBindable<MediumHandle?> = TrackingBinder { it.activeMediumBind }
    override val selectedNode : IBindable<Node?> = TrackingBinder { it.groupTree.selectedNodeBind }
    override val currentAnimationBind : ICruddyOldBindable<Animation?> = CruddyOldTrackingBinder { it.animationManager.currentAnimationBind}
    override val currentAnimationSpaceBind: ICruddyOldBindable<AnimationSpace?> = CruddyOldTrackingBinder { it.animationSpaceManager.currentAnimationSpaceBind }

    init {
        // Note: In order to cut down on code which could easily be forgotten/broken, TrackingObservers automatically
        //  add themselves to an internal list which is then linked to the workspace observer automaticlaly.  But this
        //  means order is important.  In particular
        //      1) [trackingObservers' Initialization]
        //      2) [each individual TrackingObserver's initialization]
        //      3) [this init block]
        trackingObservers.forEach { workspaceSet.workspaceObserver.addObserver(it.observer()) }
    }

    private inner class TrackingBinder<T>(val finder: (IImageWorkspace) -> IBindable<T>) : IBindable<T?>
    {
        override val field: T? get() = workspaceSet.currentWorkspace?.run(finder)?.field
        var currentContract: IContract? = null

        override fun addObserver(observer: IObserver<OnChangeEvent<T?>>, trigger: Boolean): IContract = ObserverContract(observer)
        private val binds = mutableListOf<ObserverContract>()
        private inner class ObserverContract( val observer: IBindObserver<T?>) : IContract {
            init {binds.add(this)}
            override fun void() { binds.remove(this)}
        }

        private val workspaceObsContract = workspaceSet.currentWorkspaceBind.addObserver { new, old ->
            currentContract?.void()
            val oldF = old?.run(finder)?.field
            when(new) {
                null -> {
                    currentContract = null
                    binds.removeIf { it.observer.trigger?.invoke(null, oldF) == null }
                }
                else -> {
                    val newBind = finder(new)
                    val newF = newBind.field
                    currentContract = newBind.addObserver(newF != oldF){ newT: T, oldT: T ->
                        binds.removeIf { it.observer.trigger?.invoke(newT, oldT) == null }
                    }
                }
            }
        }
    }

    // region CruddyOldImplementation
    inner class CruddyOldTrackingBinder<T>(val finder: (IImageWorkspace) -> ICruddyOldBindable<T>) : ICruddyOldBindable<T?>
    {
        private val listeners = mutableListOf<CruddyOldOnChangeEvent<T?>>()
        private val weakListeners = mutableListOf<WeakReference<CruddyOldOnChangeEvent<T?>>>()
        override val field: T? get() = workspaceSet.currentWorkspace?.run { finder.invoke(this).field }

        override fun addListener(listener: CruddyOldOnChangeEvent<T?>) : ICruddyBoundListener<T?> {
            return TrackingBound(listener.apply { listeners.add( this) })
        }

        override fun addWeakListener(listener: CruddyOldOnChangeEvent<T?>) : ICruddyBoundListener<T?> {
            return TrackingBound(listener.apply { weakListeners.add( WeakReference(this)) })
        }

        init {
            var currentBind : ICruddyBoundListener<T>? = null
            val onChange : (T,T)->Unit = {new, old ->
                listeners.forEach { it.invoke(new, old) }
                weakListeners.removeIf { it.get()?.invoke(new,old) == null }
            }

            workspaceSet.currentWorkspaceBind.addObserver { selectedWS, previousSelected ->
                currentBind?.unbind()
                currentBind = selectedWS?.run { finder.invoke(this).addListener(onChange) }

                val new = selectedWS?.run(finder)?.field
                val old = previousSelected?.run(finder)?.field
                listeners.forEach { it.invoke(new, old) }
                weakListeners.removeIf { it.get()?.invoke(new, old) == null }

            }
        }

        // Todo: this could potentially do weird things if someone binds the same oce multiple times
        inner class TrackingBound<T>( private val oce: CruddyOldOnChangeEvent<T?>) : ICruddyBoundListener<T> {
            override fun unbind() {
                listeners.removeIf { it == oce }
                weakListeners.removeIf { (it.get()?: oce) == oce }
            }
        }
    }

    inner class OmniObserver<T>(
            val observerFinder : (IImageWorkspace) -> ICruddyOldObservable<T>)
        : ICruddyOldObservable<T>, WorkspaceObserver
    {
        init {
            omniObserver.add(this)
        }
        private val observers = mutableListOf<WeakReference<T>>()

        override fun addObserver(toAdd: T) : T {
            observers.add( WeakReference(toAdd))
            return toAdd
        }
        override fun removeObserver(toRemove: T) {
            observers.removeIf { (it.get() ?: toRemove) == toRemove}
        }

        override fun workspaceCreated(newWorkspace: IImageWorkspace) {
            val observable = observerFinder.invoke(newWorkspace)
            observers.removeIf { it.get()?.apply { observable.addObserver(this) } == null }
        }
        override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {
            val observable = observerFinder.invoke(removedWorkspace)
            observers.removeIf { it.get()?.apply { observable.removeObserver(this) } == null }
        }
        override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {}
    }

    inner class TrackingObserver<T>(
            val observerFinder : (IImageWorkspace) -> ICruddyOldObservable<T>)
        : ICruddyOldObservable<T>, WorkspaceObserver
    {
        init {
            trackingObservers.add(this)
        }
        private val observers = mutableListOf<WeakReference<T>>()

        override fun addObserver(toAdd: T) : T{
            val workspace = workspaceSet.currentWorkspace
            if( workspace != null) observerFinder.invoke(workspace).addObserver(toAdd)
            observers.add( WeakReference(toAdd))
            return toAdd
        }
        override fun removeObserver(toRemove: T) {
            val workspace = workspaceSet.currentWorkspace
            if( workspace != null) observerFinder.invoke(workspace).removeObserver(toRemove)
            observers.removeIf { (it.get() ?: toRemove) == toRemove}
        }

        override fun workspaceCreated(newWorkspace: IImageWorkspace) {}
        override fun workspaceRemoved(removedWorkspace: IImageWorkspace) {}
        override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) {
            if( previousSelected != null) {
                val observable = observerFinder.invoke(previousSelected)
                observers.removeIf { it.get()?.apply { observable.removeObserver(this) } == null }
            }
            if( selectedWorkspace != null) {
                val observable = observerFinder.invoke(selectedWorkspace)
                observers.removeIf { it.get()?.apply { observable.addObserver(this) } == null }
            }
        }
    }
    // endregion
}
