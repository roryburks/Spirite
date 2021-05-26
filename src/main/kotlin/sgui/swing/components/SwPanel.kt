package sgui.swing.components

import sgui.components.IComponent
import sgui.core.components.crossContainer.CrossInitializer
import sgui.core.components.crossContainer.ICrossPanel
import sgui.swing.advancedComponents.CrossContainer.CrossLayout
import sgui.swing.systems.mouseSystem.adaptMouseSystem
import sgui.swing.skin.Skin.Global.Bg
import sguiSwing.components.ISwComponent
import sguiSwing.components.SwComponent
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