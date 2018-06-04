package spirite.pc.gui.basic

import spirite.base.brains.Bindable
import spirite.gui.components.basic.ITextArea
import spirite.gui.resources.Skin.BevelBorder.Dark
import spirite.gui.resources.Skin.BevelBorder.Light
import spirite.gui.resources.Skin.TextField.Background
import javax.swing.BorderFactory
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.border.BevelBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class SwTextArea
private constructor(val imp : SwTextAreaImp) : ITextArea, ISwComponent by SwComponent(imp)
{
    constructor() : this(SwTextAreaImp())

    override val textBind = Bindable("")
    override var text
            get() = textBind.field
            set(value) {
                if( textToBeSetToDocument == null) {
                    textToBeSetToDocument = value
                    textBind.field = value
                }
            }

    private var textToBeSetToDocument : String? = null
    val lock = object {}

    fun update() {
        synchronized(lock) {
            text = imp.textArea.text
        }
    }

    init {
        imp.textArea.document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {update()}
            override fun insertUpdate(e: DocumentEvent?) {update()}
            override fun removeUpdate(e: DocumentEvent?) {update()}
        })
        textBind.addListener { new, old ->
            imp.textArea.text = new
        }
    }


    private class SwTextAreaImp(val textArea :JTextArea = JTextArea()) : JScrollPane(textArea)
    {

        init {
            textArea.background = Background.jcolor
            textArea.border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Light.jcolor, Dark.jcolor)

        }
    }
}