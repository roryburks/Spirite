package spirite.base.imageData.animation

import spirite.base.brains.Bindable
import spirite.base.brains.IBindable
import spirite.base.imageData.IImageWorkspace
import spirite.base.imageData.MImageWorkspace


interface IAnimationManager {
    val animations : List<Animation>

    val currentAnimationBind : IBindable<Animation?>
    var currentAnimation : Animation?
}

class AnimationManager(workspace : MImageWorkspace) : IAnimationManager{
    private val _animations = mutableListOf<Animation>()
    override val animations: List<Animation> get() = _animations

    override val currentAnimationBind = Bindable<Animation?>(null)
    override var currentAnimation by currentAnimationBind

}