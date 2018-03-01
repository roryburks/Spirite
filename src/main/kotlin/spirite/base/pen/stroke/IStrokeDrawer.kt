package spirite.base.pen.stroke

interface IStrokeDrawer {
    fun startStroke( )
    fun stepStroke()
    fun endStroke()
    fun batchStroke()
}