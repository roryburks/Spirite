package spirite.base.imageData.animationSpaces

import rb.owl.IObservable
import rb.owl.bindable.Bindable
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
    val animationSpaceObservable : IObservable<InternalAnimationSpaceObserver>
    val animationPlayObservable : IObservable<InternalAnimationPlayObserver>
}

abstract class AnimationSpace(
        name: String,
        val workspace: IImageWorkspace)
{
    var nameBind = Bindable(name)
    var name by nameBind

    abstract val stateView : IAnimationSpaceView
}