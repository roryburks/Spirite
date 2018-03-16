package spirite.pc.gui.basic

import spirite.gui.resources.Skin.Global.Bg
import spirite.gui.components.advanced.crossContainer.CrossInitializer
import spirite.gui.components.basic.ICrossPanel
import java.awt.Graphics
import javax.swing.JPanel


open class SwPanel
private constructor(val imp: SPanelImp)
    : ICrossPanel, ISwComponent by SwComponent(imp)
{

    constructor( drawer: (JPanel.(Graphics) -> Unit)? = null) : this(SPanelImp(drawer))

    override fun setLayout(constructor: CrossInitializer.()->Unit) {
        imp.removeAll()
        imp.layout = CrossLayout.buildCrossLayout(imp, constructor)
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
            background = Bg.color;
        }
    }
}