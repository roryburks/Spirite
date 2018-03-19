package spirite.base.graphics

import spirite.base.util.groupExtensions.SinglyList
import spirite.base.util.linear.Transform


/** RenderProperties is carried by individual objects, like nodes, Sprite Parts, etc */
data class RenderProperties(
        var visible: Boolean = true,
        var alpha: Float = 1.0f,
        var method: RenderMethod = RenderMethod())
{
    val isVisible: Boolean = visible && alpha > 0f
}


data class RenderRubric constructor(
        val transform: Transform,
        val alpha: Float,
        val methods: List<RenderMethod>)
{
    constructor(
            transform: Transform = Transform.IdentityMatrix,
            alpha: Float = 1f,
            method: RenderMethod? = null)
            : this(transform, alpha, if( method == null) emptyList() else SinglyList(method))

    fun stack(top: RenderRubric) : RenderRubric {
        return RenderRubric(
                top.transform * transform,
                top.alpha * alpha,
                methods + top.methods)
    }
}

/** RenderMethods is a MethodType along with a scroll (if applicable) */
data class RenderMethod(
        val methodType: RenderMethodType = RenderMethodType.DEFAULT,
        val renderValue: Int = methodType.defaultValue)


enum class RenderMethodType constructor(val description: String, val defaultValue: Int) {
    DEFAULT("Normal", 0),
    COLOR_CHANGE_HUE("As Color", 0xFF0000),
    COLOR_CHANGE_FULL("As Color (fully)", 0xFF0000),

    DISOLVE("Disolve", 57343),
    //DISSOLVE("Disolve", 0b01110010_00101111),

    LIGHTEN("Lighten", 0),
    SUBTRACT("Subtract", 0),
    MULTIPLY("Multiply", 0),
    SCREEN("Screen", 0),;
    //OVERLAY("Overlay", 0)

    override fun toString() = description
}