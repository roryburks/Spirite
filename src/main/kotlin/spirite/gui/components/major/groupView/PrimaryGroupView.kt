package spirite.gui.components.major.groupView

import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.hybrid.Hybrid

class PrimaryGroupView
private constructor( val panel : ICrossPanel)
    : IComponent by panel
{
    constructor() : this(panel = Hybrid.ui.CrossPanel())
}
