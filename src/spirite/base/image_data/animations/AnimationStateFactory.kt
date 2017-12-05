package spirite.base.image_data.animations

import spirite.base.image_data.Animation
import spirite.base.image_data.AnimationManager
import spirite.base.image_data.AnimationState
import spirite.base.image_data.animations.ffa.FFAAnimationState
import spirite.base.image_data.animations.ffa.FixedFrameAnimation


fun createStateFromAnimation( animation: Animation, context: AnimationManager) : AnimationState {
    return when( animation) {
        is FixedFrameAnimation -> FFAAnimationState(context, animation)
        else -> throw Exception("Bad")
    }
}