package sgui.generic.components

import sgui.generic.advancedComponents.crossContainer.CrossInitializer

interface ICrossPanel : IComponent {
    fun setLayout(constructor: CrossInitializer.()->Unit)
    fun clearLayout()
}