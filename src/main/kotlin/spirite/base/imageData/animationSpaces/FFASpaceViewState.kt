package spirite.base.imageData.animationSpaces

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.IAnimationSpaceView.InternalAnimationSpaceObserver
import spirite.base.util.linear.Vec2i

class FFASpaceViewState(val space: FFAAnimationSpace)
    :IAnimationSpaceView
{
    val logicalSpace : Map<FixedFrameAnimation,Vec2i> get() = _logicalSpace
    private val _logicalSpace = mutableMapOf<FixedFrameAnimation,Vec2i>()

    private val frameSize = 24
    private val gap = 8

    fun setLogicalSpace(ffa: FixedFrameAnimation, point: Vec2i)
    {
        if(space.animations.contains(ffa))
        {
            _logicalSpace[ffa] = point
            triggerOtherChange()
        }
    }

    internal fun triggerAnimationRemoved(animation: FixedFrameAnimation)
    {
        _logicalSpace.remove(animation)
        _animationSpaceObservable.trigger { it.animationSpaceChanged(true) }
    }
    internal fun triggerAnimationAdded(animation: FixedFrameAnimation)
    {
        val lowest = _logicalSpace.values.map { it.y }.max()
        _logicalSpace[animation] = Vec2i(0, if(lowest == null) 0 else (lowest + frameSize +  gap))
        _animationSpaceObservable.trigger { it.animationSpaceChanged(true) }

    }
    internal fun triggerOtherChange(structural: Boolean = false)
    {
        _animationSpaceObservable.trigger { it.animationSpaceChanged(structural) }
    }

    override val animationSpaceObservable : IObservable<InternalAnimationSpaceObserver> get() = _animationSpaceObservable
    private val _animationSpaceObservable= Observable<InternalAnimationSpaceObserver>()

    private val __animationStructureObs = space.workspace.animationManager.animationStructureChangeObservable.addObserver(object : AnimationStructureChangeObserver {
        override fun animationStructureChanged(animation: Animation) {
            if( space.animations.contains(animation))
                triggerOtherChange(true)
        }
    })
}