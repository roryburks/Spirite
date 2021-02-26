package spirite.base.brains

import rb.global.IContract
import rb.extendo.extensions.toHashMap
import rb.owl.IObservable
import rb.owl.IObserver
import rb.owl.bindable.IBindObserver
import rb.owl.bindable.IBindable
import rb.owl.bindable.OnChangeEvent
import rb.owl.bindable.addObserver
import rb.owl.observer
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

/** The CentralObservatory is a place where things (primarily GUI components) which need to watch for certain changes
 * regardless of which Workspace is active should get their Observables from.  It automatically adds and removes ovservers
 * as the currentWorkspace is changed.*/
interface ICentralObservatory {
    val omniImageObserver : IObservable<ImageObserver>

    val trackingUndoHistoryObserver : IObservable<(UndoHistoryChangeEvent)->Any?>
    val trackingImageObserver : IObservable<ImageObserver>
    val trackingPrimaryTreeObserver : IObservable<TreeObserver>
    val trackingAnimationObservable : IObservable<AnimationObserver>
    val trackingAnimationStateObserver: IObservable<AnimationStructureChangeObserver>
    val trackingActiveDrawerObserver : IObservable<()->Unit>

    val activeDataBind : IBindable<MediumHandle?>
    val selectedNode : IBindable<Node?>
    val currentAnimationBind : IBindable<Animation?>
    val currentAnimationSpaceBind : IBindable<AnimationSpace?>
    val currentViewNum : IBindable<Int?>
}

class CentralObservatory(private val workspaceSet : IWorkspaceSet)
    : ICentralObservatory
{
    // OmniObservers (trigger any time something happens on any Workspace regardless of it it's currently active)
    override val omniImageObserver: IObservable<ImageObserver>
            = OmniObserver { it.imageObservatory.imageObservable }

    // Tracking Observers (triggers only for active workspace)
    override val trackingUndoHistoryObserver: IObservable<(UndoHistoryChangeEvent) -> Any?>
            = TrackingObserver ({ it.undoEngine.undoHistoryObserver })
    override val trackingImageObserver : IObservable<ImageObserver>
            = TrackingObserver ({it.imageObservatory.imageObservable})
    override val trackingPrimaryTreeObserver: IObservable<TreeObserver>
            = TrackingObserver ({ it.groupTree.treeObservable })
    override val trackingAnimationObservable: IObservable<AnimationObserver>
            = TrackingObserver ({ it.animationManager.animationObservable })
    override val trackingAnimationStateObserver: IObservable<AnimationStructureChangeObserver>
            = TrackingObserver ({ it.animationManager.animationStructureChangeObservable })
    override val trackingActiveDrawerObserver: IObservable<() -> Unit>
            = TrackingObserver ({ it.activeDrawerObs }, {it.triggerActiveDrawerChange()})

    // Tracking Binders
    override val activeDataBind: IBindable<MediumHandle?>
            = TrackingBinder { it.activeMediumBind }
    override val selectedNode : IBindable<Node?>
            = TrackingBinder { it.groupTree.selectedNodeBind }
    override val currentAnimationBind : IBindable<Animation?>
            = TrackingBinder { it.animationManager.currentAnimationBind}
    override val currentAnimationSpaceBind: IBindable<AnimationSpace?>
            = TrackingBinder { it.animationSpaceManager.currentAnimationSpaceBind }
    override val currentViewNum: IBindable<Int?>
            = TrackingBinder { it.viewSystem.viewBind }

    private inner  class OmniObserver<T>(val finder: (IImageWorkspace) -> IObservable<T>) : IObservable<T> {
        override fun addObserver(observer: IObserver<T>, trigger: Boolean): IContract = ObserverContract(observer)

        private val _binds = mutableListOf<ObserverContract>()
        private inner class ObserverContract(val observer: IObserver<T>): IContract {
            init{ _binds.add(this)}
            override fun void() { _binds.remove(this) }
       }

        private val _pipingObs = object : IObserver<T> {
            // TODO: Clear out empty Triggers (or maybe rethink that entire relationship)
            override val triggers get() = _binds.asSequence()
                        .mapNotNull { it.observer.triggers }
                        .flatten()
        }

        private val _wsKs = workspaceSet.workspaces
                .toHashMap({it}, {finder(it).addObserver(_pipingObs)})

        private val workspacK = workspaceSet.workspaceObserver.addObserver(object : IWorkspaceSet.WorkspaceObserver{
            override fun workspaceRemoved(removedWorkspace: IImageWorkspace) { _wsKs.remove(removedWorkspace)?.void() }
            override fun workspaceChanged(selectedWorkspace: IImageWorkspace?, previousSelected: IImageWorkspace?) { }
            override fun workspaceCreated(newWorkspace: IImageWorkspace) {
                // Shouldn't have to check for map re-population of same node
                _wsKs[newWorkspace] = finder(newWorkspace).addObserver(_pipingObs)
            }
        }.observer())
    }

    private inner class TrackingBinder<T>(val finder: (IImageWorkspace) -> IBindable<T>) : IBindable<T?>
    {
        override val field: T? get() = workspaceSet.currentWorkspace?.run(finder)?.field
        override fun addObserver(observer: IObserver<OnChangeEvent<T?>>, trigger: Boolean): IContract = ObserverContract(observer)

        private var currentContract: IContract? = null
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
                    binds.removeIf { it.observer.triggers?.forEach { it(null, oldF) } == null }
                }
                else -> {
                    val newBind = finder(new)
                    val newF = newBind.field
                    currentContract = newBind.addObserver(newF != oldF){ newT: T, oldT: T ->
                        binds.removeIf { it.observer.triggers?.forEach { it(newT, oldT) } == null }
                    }
                }
            }
        }
    }

    private inner class TrackingObserver<T>(
            val finder : (IImageWorkspace) -> IObservable<T>,
            val onChangeTrigger: ((IImageWorkspace)->Unit)? = null)
        : IObservable<T>
    {
        override fun addObserver(observer: IObserver<T>, trigger: Boolean) : IContract {
            current?.also { currentContracts.add(it.addObserver(observer, trigger)) }
            return ObserverContract(observer)
        }

        private var currentContracts = mutableListOf<IContract>()
        private val binds = mutableListOf<ObserverContract>()
        private inner class ObserverContract( val observer: IObserver<T>) : IContract {
            init { binds.add(this)}
            override fun void() {binds.remove(this)}
        }
        private var current: IObservable<T>? = null

        private val workspaceObsContract = workspaceSet.currentWorkspaceBind.addObserver { new, old ->
            currentContracts.forEach{it.void()}
            if( new != null) {
                val newT = finder(new)
                current = newT
                binds.asSequence()
                        .map { it.observer }
                        .forEach { newT.addObserver(it, true) }    // TODO Should trigger be saved, defaulted to false, or removed?
                onChangeTrigger?.invoke(new)
            }
            else current = null
        }

    }
}
