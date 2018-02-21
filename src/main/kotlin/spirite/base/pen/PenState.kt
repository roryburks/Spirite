package spirite.base.pen

data class PenState(
        val x: Float,
        val y: Float,
        val pressure: Float
)

enum class ButtonType {
    LEFT, RIGHT, CENTER
}