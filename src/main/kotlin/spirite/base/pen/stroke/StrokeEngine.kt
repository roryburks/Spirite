package spirite.base.pen.stroke

import spirite.base.imageData.mediums.BuiltMediumData
import spirite.base.imageData.selection.SelectionMask
import spirite.base.pen.PenState

class StrokeEngine(
        val drawer: IStrokeDrawer
) {
    fun startStroke(
            firstPS: PenState,
            params: StrokeParams,
            built: BuiltMediumData,
            selection: SelectionMask?
    ) {

    }

    //fun startStroke( )
}

