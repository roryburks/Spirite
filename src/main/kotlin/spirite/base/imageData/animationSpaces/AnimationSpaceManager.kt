package spirite.base.imageData.animationSpaces

import spirite.base.brains.Bindable
import spirite.base.brains.IBindable
import spirite.base.imageData.IImageWorkspace

interface  IAnimationSpaceManager
{
    val workspace: IImageWorkspace

    val currentAnimationSpace : IAnimationSpace?
    val currentAnimationSpaceBind: IBindable<IAnimationSpace?>
}

class AnimationSpaceManager(override val workspace: IImageWorkspace) : IAnimationSpaceManager
{
    override val currentAnimationSpaceBind = Bindable<IAnimationSpace?>(null)
    override val currentAnimationSpace: IAnimationSpace? by currentAnimationSpaceBind

}