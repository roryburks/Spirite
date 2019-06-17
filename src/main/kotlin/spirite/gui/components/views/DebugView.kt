package spirite.gui.components.views

import sgui.generic.components.IComponent
import sgui.generic.components.ITextArea
import sgui.generic.components.crossContainer.ICrossPanel
import sgui.swing.SwIcon
import spirite.base.graphics.gl.GLImageTracker
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.hybrid.Hybrid

class DebugView
private constructor( val imp : ICrossPanel)
    :IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = null
    override val name: String get() = "Debug"

    constructor() : this( Hybrid.ui.CrossPanel())

    private val glArea : ITextArea = Hybrid.ui.TextArea()
    private val dbArea : ITextArea = Hybrid.ui.TextArea()

    init {
        glArea.enabled = false
        dbArea.enabled = false

        imp.setLayout {
            rows.add(dbArea, flex = 100f )
            rows.addGap(2)
            rows.add(glArea, flex = 100f )
            rows.padding = 4
        }

        Hybrid.timing.createTimer(100, true){
            val sb = StringBuilder()

            sb.appendln("${GLImageTracker.images.size} : ${GLImageTracker.bytesUsed}")
            GLImageTracker.images.forEach {
                sb.appendln("  [${it.width} xi ${it.height}]")
            }

            glArea.text = sb.toString()
        }
    }
}