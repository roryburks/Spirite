package spirite.pc.gui.basic

import spirite.base.brains.Bindable
import spirite.gui.components.basic.ITextArea
import spirite.gui.resources.Skin.BevelBorder.Dark
import spirite.gui.resources.Skin.BevelBorder.Light
import spirite.gui.resources.Skin.TextField.Background
import javax.swing.BorderFactory
import javax.swing.JTextArea
import javax.swing.border.BevelBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class SwTextArea
private constructor(val imp : SwTextAreaImp) : ITextArea, ISwComponent by SwComponent(imp)
{
    constructor() : this(SwTextAreaImp())

    override val textBind = Bindable("")
    override var text by textBind

    init {
        var locked = false
        imp.document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false}
            override fun insertUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false}
            override fun removeUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false}
        })
        textBind.addListener { new, old -> if(!locked)imp.text = new }
    }

    private class SwTextAreaImp() : JTextArea()
    {
        init {
            background = Background.color
            border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Light.color, Dark.color)
        }
    }
}