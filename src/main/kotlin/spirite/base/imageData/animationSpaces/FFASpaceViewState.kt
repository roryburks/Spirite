package spirite.base.imageData.animationSpaces

import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.util.linear.Vec2i

class FFASpaceViewState(val space: FFAAnimationSpace)
{
    val logicalSpace : Map<FixedFrameAnimation,Vec2i> get() = _logicalSpace
    private val _logicalSpace = mutableMapOf<FixedFrameAnimation,Vec2i>()

    private val frameSize = 24
    private val gap = 8

    internal fun triggerAnimationRemoved(animation: FixedFrameAnimation)
    {
        _logicalSpace.remove(animation)
        _internalStateObservable.trigger { it() }
    }
    internal fun triggerAnimationAdded(animation: FixedFrameAnimation)
    {
        val lowest = _logicalSpace.values.map { it.y }.max()
        _logicalSpace[animation] = Vec2i(0, if(lowest == null) 0 else (lowest + frameSize +  gap))
        _internalStateObservable.trigger { it() }

    }
    internal fun triggerOtherChange()
    {
        _internalStateObservable.trigger { it() }

    }

    val internalStateObservable : IObservable<()->Unit> get() = _internalStateObservable
    private val _internalStateObservable = Observable<()->Unit>()
}