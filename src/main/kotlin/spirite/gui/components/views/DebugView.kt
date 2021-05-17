package spirite.gui.components.views

import sgui.components.IComponent
import sgui.core.components.ITextArea
import sgui.core.components.crossContainer.ICrossPanel
import sgui.swing.SwIcon
import sgui.swing.hybrid.Hybrid
import spirite.gui.components.advanced.omniContainer.IOmniComponent

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

            sb.appendln("${Hybrid.gl.tracker?.images?.size} : ${Hybrid.gl.tracker?.bytesUsed}")
            Hybrid.gl.tracker?.images?.forEach {
                sb.appendln("  [${it.width} xi ${it.height}]")
            }

            glArea.text = sb.toString()
        }
    }
}