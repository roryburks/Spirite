package sgui.core.components

import rb.owl.bindable.Bindable
import sgui.components.IComponent

interface ISlider : IComponent {
    var min : Int
    var max : Int

    val valueBind : Bindable<Int>
    var value : Int

    var tickSpacing: Int
    var snapsToTick: Boolean

    fun setLabels(labels: Map<Int,String>)
}

