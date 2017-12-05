package spirite.base.image_data

import spirite.base.image_data.Animation
import spirite.base.image_data.AnimationManager


abstract class AnimationState( private val context: AnimationManager, private val anim: Animation) {
    var met: Float = 0f
    var expanded = true

    protected fun triggerChange() {
        context.triggerInnerStateChange(anim)
    }
}