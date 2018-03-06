package spirite.gui.crossLayout

import spirite.gui.Basic.SComponent

class CrossLayout (
        constructor: (CrossLayoutInitializer) -> Unit)
{
    init {


    }


    class CrossLayoutInitializer
    internal constructor() {
        val rows = CrossLayoutGroup()

        var padding : Int? = null
    }


    // region RowTypes
    interface RowLayoutRow {}

    class GapRow(
            val preferedSize : Int,
            val flex : Float = 0f) : RowLayoutRow


    // endregion

    class RowLayoutParameters
    internal constructor() {

    }

    companion object {
        infix fun SComponent.layout(layer : (RowLayoutParameters) -> Unit ) {


        }
    }
}