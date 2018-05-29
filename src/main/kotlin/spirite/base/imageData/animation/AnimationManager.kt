package spirite.base.imageData.animation

import spirite.base.brains.Bindable

interface IAnimationManager {
    val animations : List<IAnimation>
    var selectedAnimation: Bindable<IAnimation?>
}

class AnimationManager : IAnimationManager {
    override val animations = mutableListOf<IAnimation>()
    override var selectedAnimation = Bindable<IAnimation?>(null)

}