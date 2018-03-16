package spirite.gui.components.basic

import spirite.gui.components.advanced.crossContainer.CrossInitializer

interface ICrossPanel : IComponent {
    fun setLayout(constructor: CrossInitializer.()->Unit)
    fun clearLayout()
}