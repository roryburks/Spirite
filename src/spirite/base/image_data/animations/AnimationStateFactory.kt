package spirite.base.image_data.animations

import com.sun.javaws.exceptions.InvalidArgumentException
import spirite.base.image_data.Animation
import spirite.base.image_data.animations.ffa.FFAAnimationState
import spirite.base.image_data.animations.ffa.FixedFrameAnimation


fun createStateFromAnimation( animation: Animation) : AnimationState {
    return when( animation) {
        is FixedFrameAnimation -> FFAAnimationState(animation)
        else -> throw Exception("Bad")
    }
}