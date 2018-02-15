package spirite.base.graphics.rendering

import spirite.base.graphics.RenderProperties
import spirite.base.imageData.MediumHandle
import spirite.base.util.linear.MutableTransform

data class TransformedHandle(
        var medium: MediumHandle,
        var alpha: Float = 1.0f,
        var transform: MutableTransform = MutableTransform.IdentityMatrix(),
        var renderProperties: RenderProperties = RenderProperties(),
        var depth: Int = 0)