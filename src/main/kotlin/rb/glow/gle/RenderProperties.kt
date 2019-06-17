package rb.glow.gle

import rb.extendo.dataStructures.SinglyList
import rb.glow.gle.RenderMethodType.DEFAULT
import rb.vectrix.linear.ITransformF
import rb.vectrix.linear.ImmutableTransformF


data class RenderRubric constructor(
        val transform: ITransformF,
        val alpha: Float,
        val methods: List<RenderMethod>)
{
    constructor(
            transform: ITransformF = ImmutableTransformF.Identity,
            alpha: Float = 1f,
            method: RenderMethod? = null)
            : this(transform, alpha, if( method == null) emptyList() else SinglyList(method))

    fun stack(top: RenderRubric) = RenderRubric(
            top.transform * transform,
            top.alpha * alpha,
            methods + top.methods)

    fun stack( transform: ITransformF) = RenderRubric(
            transform * this.transform,
            alpha,
            methods)
}

/** RenderMethods is a MethodType along with a scroll (if applicable) */
data class RenderMethod(
        val methodType: RenderMethodType = DEFAULT,
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