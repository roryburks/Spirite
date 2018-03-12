package spirite.gui.basic

import spirite.gui.Skin.Global.Bg
import spirite.gui.advanced.crossContainer.CrossInitializer
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JPanel

open class SPanel
private constructor(invokable: Invokable<JComponent>)
    : JPanel(), IComponent,
        ISComponent by SComponent(invokable)
{
    init {invokable.invoker = {this}}
    constructor() : this(Invokable())

    init {
        background = Bg.color;
    }
}

open class SPanel2
private constructor(val imp: SPanelImp)
    :ISComponent by SComponentDirect(imp)
{
    constructor( drawer: (JPanel.(Graphics) -> Unit)? = null) : this( SPanelImp(drawer))

    val width: Int get() = imp.width
    val height: Int get() = imp.height

    fun setLayout(constructor: CrossInitializer.()->Unit) {
        imp.removeAll()
        imp.layout = CrossLayout.buildCrossLayout(imp, constructor)
        imp.validate()
    }

    private class SPanelImp(val drawer: (JPanel.(Graphics) -> Unit)? = null) : JPanel(){
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            drawer?.invoke(this, g)
        }

        init {
            background = Bg.color;
        }
    }
}