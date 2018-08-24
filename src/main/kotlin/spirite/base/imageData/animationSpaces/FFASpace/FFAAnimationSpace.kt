package spirite.base.imageData.animationSpaces.FFASpace

import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationObserver
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.AnimationSpace

class FFAAnimationSpace(
        name: String,
        workspace: IImageWorkspace)
    : AnimationSpace(name, workspace)
{

    val animationStructs : List<FFASpaceStruct> get() = _animations
    val animations : List<FixedFrameAnimation> get() = _animations.map { it.animation }
    val links : List<SpacialLink> get() = _links
    override val stateView = FFASpaceViewState(this)

    data class FFASpaceStruct(
            val animation: FixedFrameAnimation,
            var onEndLink: Pair<FixedFrameAnimation,Int>? = null)
    private val _animations = mutableListOf<FFASpaceStruct>()
    private val _links = mutableListOf<SpacialLink>()

    data class SpacialLink(
            val origin: FixedFrameAnimation,
            val originFrame: Int,
            val destination: FixedFrameAnimation,
            val destinationFrame: Int)

    fun removeAnimation(animation: FixedFrameAnimation)
    {
        _animations.removeIf { it.animation == animation }
        _links.removeIf { it.origin == animation || it.destination == animation }
        _animations.forEach { if( it.onEndLink?.first == animation) it.onEndLink = null }
        stateView.triggerAnimationRemoved(animation)
    }

    fun addAnimation( animation: FixedFrameAnimation)
    {
        if( _animations.any {it.animation == animation} ) return
        _animations.add(FFASpaceStruct(animation))
        stateView.triggerAnimationAdded(animation)
    }

    fun setOnEndBehavior( animation: FixedFrameAnimation, onEndLink: Pair<FixedFrameAnimation, Int>?)
    {
        _animations.firstOrNull { it.animation == animation }?.onEndLink = onEndLink
    }

    fun addLink( link: SpacialLink)
    {
        if(!animations.contains(link.destination) || !animations.contains(link.origin) || links.contains(link))
            return

        _links.add(link)
    }
    fun removeLink(link: SpacialLink)
    {
        _links.remove(link)
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
            if( animations.contains(animation))
            {

            }
        }
    })
}