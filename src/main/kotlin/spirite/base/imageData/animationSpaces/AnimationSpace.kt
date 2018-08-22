package spirite.base.imageData.animationSpaces

import spirite.base.brains.Bindable
import spirite.base.brains.IObservable
import spirite.base.imageData.IImageWorkspace

interface IAnimationSpaceView
{
    interface InternalAnimationSpaceObserver {
        fun animationSpaceChanged(structureChange: Boolean)
    }
    val animationSpaceObservable : IObservable<InternalAnimationSpaceObserver>
}

abstract class AnimationSpace(
        name: String,
        val workspace: IImageWorkspace)
{
    var nameBind = Bindable(name)
    var name by nameBind

    abstract val stateView : IAnimationSpaceView
}