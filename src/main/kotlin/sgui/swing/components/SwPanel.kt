package sgui.swing.components

import CrossLayout
import sgui.generic.advancedComponents.crossContainer.CrossInitializer
import sgui.generic.components.IComponent
import sgui.generic.components.ICrossPanel
import sgui.skin.Skin.Global.Bg
import sgui.swing.adaptMouseSystem
import java.awt.Graphics
import javax.swing.JPanel


open class SwPanel
private constructor(private val imp: SPanelImp)
    : ICrossPanel, ISwComponent by SwComponent(imp)
{

    constructor( drawer: (JPanel.(Graphics) -> Unit)? = null) : this(SPanelImp(drawer))

    // The only reason this exists is so that references to the IComponents will always exist and they don't fall out
    //  of weak memory.
    private lateinit var components : List<IComponent>

    override fun setLayout(constructor: CrossInitializer.()->Unit) {
        imp.removeAll()
        val list = mutableListOf<IComponent>()
        imp.layout = CrossLayout.buildCrossLayout(imp, list, constructor)
        components = list
        imp.validate()
    }
    override fun clearLayout() {
        imp.removeAll()
    }

    private class SPanelImp(val drawer: (JPanel.(Graphics) -> Unit)? = null) : JPanel(){
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            drawer?.invoke(this, g)
        }

        init {
            adaptMouseSystem()
            background = Bg.jcolor;
        }
    }
}