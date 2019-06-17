package sgui.generic.components.crossContainer

import sgui.generic.components.IComponent

interface ICrossPanel : IComponent {
    fun setLayout(constructor: CrossInitializer.()->Unit)
    fun clearLayout()
}