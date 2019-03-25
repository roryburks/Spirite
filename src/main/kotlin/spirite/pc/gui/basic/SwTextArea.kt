package spirite.pc.gui.basic

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import spirite.gui.components.basic.ITextArea
import spirite.gui.resources.Skin.BevelBorder.Dark
import spirite.gui.resources.Skin.BevelBorder.Light
import spirite.gui.resources.Skin.TextField.Background
import spirite.pc.gui.adaptMouseSystem
import javax.swing.BorderFactory
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.border.BevelBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class SwTextArea
private constructor(private val imp : SwTextAreaImp) : ITextArea, ISwComponent by SwComponent(imp)
{
    // TODO: Re-implement TextBind -> UI binding
    constructor() : this(SwTextAreaImp())

    override val textBind = Bindable("")
    override var text by textBind

    private var textToBeSetToDocument : String? = null

    fun update() {
        text = imp.textArea.text
    }

    init {
        imp.textArea.document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {update()}
            override fun insertUpdate(e: DocumentEvent?) {update()}
            override fun removeUpdate(e: DocumentEvent?) {update()}
        })
        textBind.addObserver { _, _ ->
                //imp.textArea.text = new
        }
    }


    private class SwTextAreaImp(val textArea :JTextArea = JTextArea()) : JScrollPane(textArea)
    {
        init {
            adaptMouseSystem()
            textArea.background = Background.jcolor
            textArea.border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Light.jcolor, Dark.jcolor)
        }
    }
}