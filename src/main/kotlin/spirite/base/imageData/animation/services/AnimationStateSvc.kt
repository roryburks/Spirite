package spirite.base.imageData.animation.services

import spirite.base.imageData.animation.Animation

interface IAnimationStateSvc {
    var numStateDomains : Int
    var currentStateDomain : Int

    fun getState(anim: Animation) : AnimationStateBind
}

class AnimationStateSvc : IAnimationStateSvc{
    override var numStateDomains: Int
        get() = TODO("Not yet implemented")
        set(value) {}
    override var currentStateDomain: Int
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun getState(anim: Animation): AnimationStateBind {
        TODO("Not yet implemented")
    }
}