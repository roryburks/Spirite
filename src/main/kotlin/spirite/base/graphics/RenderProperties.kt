package spirite.base.graphics


/** RenderProperties is carried by individual objects, like nodes, Sprite Parts, etc */
data class RenderProperties(
        var visible: Boolean = true,
        var alpha: Float = 1.0f,
        var method: RenderMethod = RenderMethod())
{
    val isVisible: Boolean = visible && alpha > 0f
}

/** RenderRhubric is what's conglomerated by TransformedHandles and passed to GC.renderImage */
data class RenderRhubric(val methods : List<RenderMethod> = emptyList())

/** RenderMethods is a MethodType along with a value (if applicable) */
data class RenderMethod(
        val methodType: RenderMethodType = RenderMethodType.DEFAULT,
        val renderValue: Int = methodType.defaultValue)


enum class RenderMethodType constructor(val description: String, val defaultValue: Int) {
    DEFAULT("Normal", 0),
    COLOR_CHANGE_HUE("As Color", 0xFF0000),
    COLOR_CHANGE_FULL("As Color (fully)", 0xFF0000),

    DISOLVE("Disolve", 57343),
    //DISOLVE("Disolve", 0b01110010_00101111),

    LIGHTEN("Lighten", 0),
    SUBTRACT("Subtract", 0),
    MULTIPLY("Multiply", 0),
    SCREEN("Screen", 0),
    OVERLAY("Overlay", 0)
}