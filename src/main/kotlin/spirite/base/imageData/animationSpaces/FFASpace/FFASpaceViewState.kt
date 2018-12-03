package spirite.base.imageData.animationSpaces.FFASpace

import spirite.base.util.binding.Bindable
import spirite.base.brains.IObservable
import spirite.base.brains.Observable
import spirite.base.imageData.animation.Animation
import spirite.base.imageData.animation.IAnimationManager.AnimationStructureChangeObserver
import spirite.base.imageData.animation.ffa.FixedFrameAnimation
import spirite.base.imageData.animationSpaces.IAnimationSpaceView
import spirite.base.imageData.animationSpaces.IAnimationSpaceView.InternalAnimationPlayObserver
import spirite.base.imageData.animationSpaces.IAnimationSpaceView.InternalAnimationSpaceObserver
import rb.vectrix.linear.Vec2i

class FFASpaceViewState(val space: FFAAnimationSpace)
    : IAnimationSpaceView
{
    // region Playback / timing
    var zoomLevel = 0

    var fpsBind = Bindable(8f)
    var fps by fpsBind
    var met = 0f
        set(value) {field = value; triggerPlayChange(animation, met)}
    var animation : FixedFrameAnimation? = null
        set(value) {field = value; triggerPlayChange(animation, met)}

    var playback: IFFAPlayback = FFANormalPlayback(space)
    fun advance( miliseconds: Float) = playback.advance(miliseconds)
    // endregion

    // region Logical Space
    val logicalSpace : Map<FixedFrameAnimation, Vec2i> get() = _logicalSpace
    private val _logicalSpace = mutableMapOf<FixedFrameAnimation, Vec2i>()

    private val frameSize = 24
    private val gap = 8

    fun setLogicalSpace(ffa: FixedFrameAnimation, point: Vec2i)
    {
        if(space.animations.contains(ffa))
        {
            if( point.xi < 0 || point.yi < 0)
            {
                val shiftX = if(point.xi < 0) -point.xi else 0
                val shiftY = if(point.yi < 0) -point.yi else 0

                _logicalSpace.replaceAll { key, value ->
                    if( key == ffa)
                        Vec2i(point.xi + shiftX, point.yi + shiftY)
                    else
                        Vec2i(value.xi + shiftX, value.yi + shiftY)
                }

            }
            else _logicalSpace[ffa] = point
            triggerOtherChange()
        }
    }
    // endregion

    // region CharBinds
    val charbinds : Map<FixedFrameAnimation,Char> get() = _charbinds
    private val _charbinds = mutableMapOf<FixedFrameAnimation,Char>()

    fun setCharBind(ffa: FixedFrameAnimation, char: Char?)
    {
        if( char == null)
            _charbinds.remove(ffa)
        else {
            _charbinds.values.remove(char)
            _charbinds[ffa] = char
        }
        triggerOtherChange()
    }
    // endregion

    internal fun triggerAnimationRemoved(animation: FixedFrameAnimation)
    {
        _logicalSpace.remove(animation)
        _animationSpaceObservable.trigger { it.animationSpaceChanged(true) }
    }
    internal fun triggerAnimationAdded(animation: FixedFrameAnimation)
    {
        val lowest = _logicalSpace.values.map { it.yi }.max()
        _logicalSpace[animation] = Vec2i(0, if (lowest == null) 0 else (lowest + frameSize + gap))
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

    override val animationPlayObservable: IObservable<InternalAnimationPlayObserver> get() = _animationPlayObservable
    private val _animationPlayObservable = Observable<InternalAnimationPlayObserver>()
    private fun triggerPlayChange( anim: Animation?, frame: Float) {
        _animationPlayObservable.trigger { it.playStateChanged(anim, frame) }
    }
}