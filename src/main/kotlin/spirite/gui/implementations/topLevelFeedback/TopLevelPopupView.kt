package spirite.gui.implementations.topLevelFeedback

import rb.extendo.extensions.removeFirst
import rb.vectrix.mathUtil.i
import sgui.components.IComponent
import sgui.components.crossContainer.ICrossPanel
import spirite.hybrid.Hybrid

class TopLevelPopupView(private  val imp: ICrossPanel = Hybrid.ui.CrossPanel())
    : IComponent by imp
{
    private val messages = mutableListOf<String>()

    fun pushMessage(string: String)
    {
        messages.add(string)
        update()
    }

    fun popMessage( string: String)
    {
        messages.removeFirst { it == string }
        update()
    }

    private fun update()
    {
        imp.setLayout {
            cols.addGap(0, 0, Short.MAX_VALUE.i)
            cols += {
                addGap(0, 0, Short.MAX_VALUE.i)
                for ( message in messages) {
                    add(Hybrid.ui.Label(message).also { it.opaque = true }, width = 200)
                    addGap(4)
                }
                addGap(16)
                width = 200
            }
        }
    }

    init {
        opaque = false
    }
}