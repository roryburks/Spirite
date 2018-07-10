package spirite.gui.components.basic

import jspirite.gui.SScrollPane
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.basic.jcomponent
import java.awt.Component

interface IScrollContainer : IComponent {
}

class SwScrollContainer
private constructor( private val imp: SwScrollContainerImp)
    : IScrollContainer, IComponent by SwComponent(imp)
{
    constructor(component: IComponent) : this(SwScrollContainerImp(component.jcomponent))

    class SwScrollContainerImp( val component: Component) : SScrollPane(component) {}
}