package spirite.base.imageData.animationSpaces

import spirite.base.brains.Bindable
import spirite.base.imageData.IImageWorkspace

abstract class AnimationSpace(
        name: String,
        val workspace: IImageWorkspace)
{
    var nameBind = Bindable(name)
    var name by nameBind
}