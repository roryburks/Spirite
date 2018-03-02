package spirite.base.pen.stroke

interface IStrokeDrawerProvider {
    fun getStrokeDrawer( strokeParams: StrokeParams) : IStrokeDrawer
}