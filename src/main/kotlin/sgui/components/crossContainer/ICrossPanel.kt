package sgui.components.crossContainer

import sgui.components.IComponent

interface ICrossPanel : IComponent {
    fun setLayout(constructor: CrossInitializer.()->Unit)
    fun clearLayout()
}