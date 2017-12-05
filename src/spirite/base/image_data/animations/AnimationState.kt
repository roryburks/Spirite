package spirite.base.image_data.animations

import spirite.base.image_data.Animation


abstract class AnimationState() {
    var met: Float = 0f
    var expanded = true
    var selectMet: Float = 0f
}