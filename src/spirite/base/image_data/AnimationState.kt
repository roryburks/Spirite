package spirite.base.image_data

import spirite.base.graphics.renderer.RenderEngine.TransformedHandle


abstract class AnimationState( private val context: AnimationManager, private val anim: Animation) {
    var met: Float = 0f
        set(value) {
            field = value
            context.triggerFrameChanged(anim)
        }
    var expanded = true

    protected fun triggerChange() {
        context.triggerInnerStateChange(anim)
    }

    abstract fun buildDrawTable() : List<List<TransformedHandle>>
}