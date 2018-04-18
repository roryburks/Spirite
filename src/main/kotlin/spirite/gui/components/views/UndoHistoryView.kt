package spirite.gui.components.views

import spirite.base.graphics.gl.GLImageTracker
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.components.basic.ITextArea
import spirite.hybrid.Hybrid

class UndoHistoryView
private constructor( val imp : ICrossPanel)
    :IComponent by imp
{
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

        Hybrid.timing.createTimer({
            glArea.text = "a"//${GLImageTracker.images.size} : ${GLImageTracker.bytesUsed}"
        }, 100, true)
    }
}