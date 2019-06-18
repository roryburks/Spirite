package sgui.components

import rb.owl.bindable.Bindable

interface ISlider : IComponent {
    var min : Int
    var max : Int

    val valueBind : Bindable<Int>
    var value : Int

    var tickSpacing: Int
    var snapsToTick: Boolean

    fun setLabels(labels: Map<Int,String>)
}

