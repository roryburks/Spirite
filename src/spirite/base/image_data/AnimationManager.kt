package spirite.base.image_data

import spirite.base.image_data.GroupTree.Node
import spirite.base.image_data.ImageWorkspace.*
import spirite.base.image_data.UndoEngine.NullAction
import spirite.base.image_data.animations.NodeLinkedAnimation
import spirite.base.image_data.animations.createStateFromAnimation
import spirite.base.util.ObserverHandler
import spirite.hybrid.MDebug
import spirite.hybrid.MDebug.ErrorType

class AnimationManager(
        private val context: ImageWorkspace
) : MImageObserver, MNodeSelectionObserver
{
    private val _animations = ArrayList<Animation>()
    val animations : List<Animation> get() = _animations.toList()
    var  selectedAnimation : Animation? = null
        set(value) {
            if( field != value) {
                val previous = field
                field = value
                triggerAnimationSelectionChange(previous)
            }
        }
    var pseudoSelectedAnimation : Animation? = null; private set
    private val stateMap = HashMap<Animation, AnimationState>()

    init {
        context.addSelectionObserver( this )
        context.addImageObserver( this)
    }

    fun getAnimationState( anim: Animation): AnimationState {
        return stateMap[anim]!!
    }


    // region Add/Remove Animation
    fun addAnimation( newAnimation: Animation) {
        val previousSelected = selectedAnimation
        val animationState = createStateFromAnimation(newAnimation, this)
        newAnimation.context = this.context

        context.undoEngine.performAndStore( object: NullAction() {
            override fun performAction() {
                _addAnimation(newAnimation, animationState)
                selectedAnimation = newAnimation
            }
            override fun undoAction() {
                _removeAnimation( newAnimation)
                if( selectedAnimation == newAnimation)
                    selectedAnimation = previousSelected
            }
            override fun getDescription(): String {
                return "Added Animation"
            }
        })
    }
    fun removeAnimation( toRemove: Animation) {
        if (!_animations.contains(toRemove)) {
            MDebug.handleError(ErrorType.STRUCTURAL_MINOR, null, "Attempted to remove Animation that isn't tracked.");
            return;
        }

        val wasSelected = (toRemove == selectedAnimation)
        val oldIndex = _animations.indexOf(toRemove)
        val state = stateMap[toRemove]!!

        context.undoEngine.performAndStore( object: NullAction(){
            override fun performAction() {
                if( wasSelected)
                    selectedAnimation = null
                _removeAnimation(toRemove)
            }

            override fun undoAction() {
                _addAnimation(toRemove, state, oldIndex)
                if( wasSelected)
                    selectedAnimation = toRemove
            }
        })
    }
    private fun _addAnimation(animation: Animation, animationState: AnimationState, index : Int? = null) {
        _animations.add( index ?: _animations.size, animation)
        stateMap.put( animation, animationState)
        triggerNewAnimation( animation)
    }
    private fun _removeAnimation( animation: Animation) {
        _animations.remove(animation)
        stateMap.remove(animation)
        triggerRemoveAnimation(animation)
    }
    // endregion

    // Goes through all the animations and purges them of orphaned data
    fun purge() {}

    // region Observers
    private val animationStructureObs = ObserverHandler<MAnimationStructureObserver>()
    fun addAnimationStructureObserver(obs: MAnimationStructureObserver) {animationStructureObs.addObserver(obs)}
    fun removeAnimationStructureObserver(obs: MAnimationStructureObserver) {animationStructureObs.removeObserver(obs)}
    interface MAnimationStructureObserver {
        fun animationAdded(evt: AnimationStructureEvent)
        fun animationRemoved(evt: AnimationStructureEvent)
        fun animationChanged(evt: AnimationStructureEvent)
    }
    enum class StructureChangeType {
        ADD, REMOVE, CHANGE
    };
    class AnimationStructureEvent(
        val animation: Animation? = null,
        val type: StructureChangeType? = null
    ) {
    }

    private fun triggerNewAnimation(anim: Animation) {
        val evt = AnimationStructureEvent( anim, StructureChangeType.ADD)
        animationStructureObs.trigger({ obs: MAnimationStructureObserver -> obs.animationAdded(evt) })
    }

    private fun triggerRemoveAnimation(anim: Animation) {
        val evt = AnimationStructureEvent( anim, StructureChangeType.REMOVE)
        animationStructureObs.trigger({ obs: MAnimationStructureObserver -> obs.animationRemoved(evt) })
    }

    // Non-private so Animations can trigger it
    internal fun triggerChangeAnimation(anim: Animation) {
        val evt = AnimationStructureEvent( anim, StructureChangeType.CHANGE)
        animationStructureObs.trigger({ obs: MAnimationStructureObserver -> obs.animationChanged(evt) })
    }


    private val animationStateObs = ObserverHandler<MAnimationStateObserver>()
    fun addAnimationStateObserver( obs: MAnimationStateObserver) { animationStateObs.addObserver( obs) }
    fun removeAnimationStateObserver( obs: MAnimationStateObserver) {animationStateObs.removeObserver(obs)}

    interface MAnimationStateObserver {
        fun selectedAnimationChanged(evt: MAnimationStateEvent)
        fun animationFrameChanged(evt: MAnimationStateEvent)
        fun viewStateChanged( evt: MAnimationStateEvent)
    }

    data class MAnimationStateEvent (
            val selected: Animation?,
            val previous: Animation? = null,
            val state : AnimationState?
    ){}

    private fun triggerAnimationSelectionChange( previous: Animation?) {
        val evt = MAnimationStateEvent( selectedAnimation, previous, null)
        animationStateObs.trigger { it.selectedAnimationChanged(evt) }
    }
    internal fun triggerFrameChanged(anim: Animation) {
        val evt = MAnimationStateEvent( anim, null, stateMap[anim])
        animationStateObs.trigger { it.animationFrameChanged(evt) }
    }
    internal fun triggerInnerStateChange( anim: Animation) {
        pseudoSelectedAnimation = anim
        val evt = MAnimationStateEvent( anim, null, stateMap[anim])
        animationStateObs.trigger { it.viewStateChanged(evt) }

        context.triggerFlash()
    }

    // endregion

    // region Observer Implementations
    // :: MImageObserver
    override fun imageChanged(evt: ImageChangeEvent?) {}
    override fun structureChanged(evt: StructureChangeEvent?) {
        val changed = evt?.change?.changedNodes ?: return

        for( animation in animations) {
            if( animation is NodeLinkedAnimation)
                animation.nodesChanged(changed)
        }
    }

    // :::MNodeSelectionObserver
    override fun selectionChanged(newSelection: Node?) {
        // TODO
    }
    // endregion



    val view = AnimationView( context, this)
}