package spirite.gui.basic

import spirite.gui.advanced.crossContainer.CrossInitializer

interface ICrossPanel : IComponent {
    fun setLayout(constructor: CrossInitializer.()->Unit)
}