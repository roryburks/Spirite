package sgui.swing.components

import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import sgui.generic.components.ITextArea
import sgui.skin.Skin.BevelBorder.Dark
import sgui.skin.Skin.BevelBorder.Light
import sgui.skin.Skin.TextField.Background
import spirite.hybrid.SwHybrid
import sgui.swing.adaptMouseSystem
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
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

        imp.textArea.addFocusListener(object : FocusListener {
            override fun focusLost(e: FocusEvent) {
                SwHybrid.keypressSystem.hotkeysEnabled = true
            }
            override fun focusGained(e: FocusEvent?) {
                SwHybrid.keypressSystem.hotkeysEnabled = false
            }
        })
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