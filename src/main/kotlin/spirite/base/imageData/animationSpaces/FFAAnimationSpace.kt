package spirite.base.imageData.animationSpaces

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationObserver
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation

class FFAAnimationSpace(
        name: String,
        workspace: IImageWorkspace)
    : AnimationSpace(name, workspace)
{
    val animations : List<FixedFrameAnimation> get() = _animations
    val links : List<SpacialLink> get() = _links
    override val stateView = FFASpaceViewState(this)

    private val _animations = mutableListOf<FixedFrameAnimation>()
    private val _links = mutableListOf<SpacialLink>()

    data class SpacialLink(
            val origin: FixedFrameAnimation,
            val originFrame: Int,
            val destination: FixedFrameAnimation,
            val destinationFrame: Int)

    fun removeAnimation(animation: FixedFrameAnimation)
    {
        _animations.remove(animation)
        _links.removeIf { it.origin == animation || it.destination == animation }
        stateView.triggerAnimationRemoved(animation)
    }

    fun addAnimation( animation: FixedFrameAnimation)
    {
        if( _animations.contains(animation)) return
        _animations.add(animation)
        stateView.triggerAnimationAdded(animation)
    }

    fun addLink( link: SpacialLink)
    {
        if(!_animations.contains(link.destination) || !_animations.contains(link.origin) || links.contains(link))
            return

        _links.add(link)
    }

    private val animationObserver = object: AnimationObserver {
        override fun animationCreated(animation: Animation) {}
        override fun animationRemoved(animation: Animation) {
            animation as? FixedFrameAnimation ?: return
            removeAnimation(animation)
        }
    }.also { workspace.animationManager.animationObservable.addObserver(it)}

    private val __animationStrucuteObserver = workspace.animationManager.animationStructureChangeObservable.addObserver( object : AnimationStructureChangeObserver {
        override fun animationStructureChanged(animation: Animation) {
            if( _animations.contains(animation))
            {

            }
        }
    })
}