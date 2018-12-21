package spirite.base.imageData.animationSpaces

import spirite.base.util.binding.CruddyBindable
import spirite.base.brains.ICruddyOldObservable
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.animation.Animation

interface IAnimationSpaceView
{
    interface InternalAnimationSpaceObserver {
        fun animationSpaceChanged(structureChange: Boolean)
    }
    interface InternalAnimationPlayObserver {
        fun playStateChanged( animation: Animation?, frame: Float)
    }
    val animationSpaceObservable : ICruddyOldObservable<InternalAnimationSpaceObserver>
    val animationPlayObservable : ICruddyOldObservable<InternalAnimationPlayObserver>
}

abstract class AnimationSpace(
        name: String,
        val workspace: IImageWorkspace)
{
    var nameBind = CruddyBindable(name)
    var name by nameBind

    abstract val stateView : IAnimationSpaceView
}