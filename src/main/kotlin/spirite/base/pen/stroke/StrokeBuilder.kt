package spirite.base.pen.stroke

import spirite.base.imageData.mediums.ArrangedMediumData
import spirite.base.imageData.selection.SelectionMask
import spirite.base.pen.PenState

class StrokeBuilder(
        val drawer: IStrokeDrawer,
        val params: StrokeParams,
        val arranged: ArrangedMediumData,
        val selection: SelectionMask?
) {
    val states : List<PenState> get() = _states
    private val _states  = mutableListOf<PenState>()

    fun start( start: PenState) {
        _states.add( start)
    }

    //fun startStroke( )
}

