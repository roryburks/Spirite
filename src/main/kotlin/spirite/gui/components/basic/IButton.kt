package spirite.gui.components.basic

interface IButton : IComponent {
    var action: (()->Unit)?
}
