package spirite.gui.basic

import spirite.gui.Skin
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.border.BevelBorder

interface IButton : IComponent {
    var action: (()->Unit)?
}
