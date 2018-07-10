package spirite.pc.gui.basic

import CrossLayout
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.Skin.Global.Bg
import java.awt.Graphics
import javax.swing.JPanel


open class SwPanel
private constructor(val imp: SPanelImp)
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
            background = Bg.jcolor;
        }
    }
}