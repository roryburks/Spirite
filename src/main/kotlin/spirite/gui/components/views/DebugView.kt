package spirite.gui.components.views

import spirite.base.graphics.gl.GLImageTracker
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.ITextArea
import spirite.gui.resources.IIcon
import spirite.hybrid.Hybrid

class DebugView
private constructor( val imp : ICrossPanel)
    :IOmniComponent
{
    override val component: IComponent get() = imp
    override val icon: IIcon? get() = null

    constructor() : this( Hybrid.ui.CrossPanel())

    val glArea : ITextArea = Hybrid.ui.TextArea()
    val dbArea : ITextArea = Hybrid.ui.TextArea()

    init {
        glArea.enabled = false
        dbArea.enabled = false

        imp.setLayout {
            rows.add(dbArea, flex = 100f )
            rows.addGap(2)
            rows.add(glArea, flex = 100f )
            rows.padding = 4
        }

        Hybrid.timing.createTimer(100, true) {
            val sb = StringBuilder()

            sb.appendln("${GLImageTracker.images.size} : ${GLImageTracker.bytesUsed}")
            GLImageTracker.images.forEach {
                sb.appendln("  [${it.width} x ${it.height}]")
            }

            glArea.text = sb.toString()
        }
    }
}