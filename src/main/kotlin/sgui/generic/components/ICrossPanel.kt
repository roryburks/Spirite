package sgui.generic.components

import sgui.generic.components.initializers.CrossInitializer

interface ICrossPanel : IComponent {
    fun setLayout(constructor: CrossInitializer.()->Unit)
    fun clearLayout()
}