package spirite.base.graphics

data class RenderProperties(
        val visible: Boolean = true,
        val alpha: Float = 1.0f,
        val method: RenderMethod = RenderMethod()
)

data class RenderMethod(
        val methodType: RenderMethodType = RenderMethodType.DEFAULT,
        val renderValue: Int = 0
)


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