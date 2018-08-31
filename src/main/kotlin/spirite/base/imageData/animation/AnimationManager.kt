package spirite.base.imageData.animation

import spirite.base.brains.Bindable
import spirite.base.brains.IBindable
import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.imageData.MImageWorkspace
import spirite.base.imageData.animation.IAnimationManager.AnimationObserver
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.groupTree.GroupTree.*
import spirite.base.imageData.undo.NullAction
import spirite.base.util.groupExtensions.mapAggregated


interface IAnimationManager {
    val animations : List<Animation>

    val currentAnimationBind : IBindable<Animation?>
    var currentAnimation : Animation?

    fun addAnimation( animation: Animation, select: Boolean = true)
    fun removeAnimation( animation: Animation)

    interface AnimationObserver {
        fun animationCreated( animation: Animation)
        fun animationRemoved( animation: Animation)
    }
    val animationObservable : IObservable<AnimationObserver>

    interface AnimationStructureChangeObserver {
        fun animationStructureChanged( animation: Animation)
    }
    val animationStructureChangeObservable : IObservable<AnimationStructureChangeObserver>
    fun triggerStructureChange(animation: Animation)
}


class AnimationManager(val workspace : MImageWorkspace) : IAnimationManager {
    private val _animations = mutableListOf<Animation>()
    override val animations: List<Animation> get() = _animations

    override val currentAnimationBind = Bindable<Animation?>(FakeAnimation(workspace))
    override var currentAnimation by currentAnimationBind


    // region Create / Delete
    override fun addAnimation(animation: Animation, select: Boolean) {
        workspace.undoEngine.performAndStore(object : NullAction() {
            override val description: String get() = "Added Animation"
            override fun performAction() {_addAnimation(animation, select)}
            override fun undoAction() {_removeAnimation(animation)}
        })
    }

    override fun removeAnimation(animation: Animation) {
        val selected = animation == currentAnimation
        workspace.undoEngine.performAndStore(object : NullAction() {
            override val description: String get() = "Removed Animation"
            override fun performAction() {_removeAnimation(animation)}
            override fun undoAction() {_addAnimation(animation, selected)}
        })
    }

    private fun _addAnimation( animation: Animation, select: Boolean) {
        _animations.add(animation)
        if( select || currentAnimation == null)
            currentAnimation = animation

        animationObservable.trigger { it.animationCreated(animation) }
    }

    private fun _removeAnimation( animation: Animation) {
        _animations.remove(animation)
        if( currentAnimation == animation)
            currentAnimation = null

        animationObservable.trigger { it.animationRemoved(animation) }
    }
    // endregion

    override val animationObservable = Observable<AnimationObserver>()
    override val animationStructureChangeObservable = Observable<AnimationStructureChangeObserver>()

    override fun triggerStructureChange(animation: Animation) {
        animationStructureChangeObservable.trigger { it.animationStructureChanged(animation)}
    }

    private val __treeObserver = workspace.groupTree.treeObservable.addObserver(object : TreeObserver {
        override fun treeStructureChanged(evt: TreeChangeEvent) {
            val fixedFrameAnimations = _animations.filterIsInstance<FixedFrameAnimation>()

            if(!fixedFrameAnimations.any())
                return;

            val changedWithAncestors = mutableSetOf<Node>()
            val allAncestors = evt.changedNodes.mapAggregated { it.ancestors }
            changedWithAncestors.addAll(evt.changedNodes)
            changedWithAncestors.addAll(allAncestors)

            fixedFrameAnimations.forEach { it.treeChanged(changedWithAncestors) }
        }

        override fun nodePropertiesChanged(node: Node, renderChanged: Boolean) {}
    })
}