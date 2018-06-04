package spirite.pc.gui.basic

import spirite.base.brains.Bindable
import spirite.gui.components.basic.*
import spirite.gui.resources.Skin.BevelBorder.Dark
import spirite.gui.resources.Skin.BevelBorder.Light
import spirite.gui.resources.Skin.TextField.Background
import spirite.gui.resources.Skin.TextField.InvalidBg
import spirite.hybrid.Hybrid
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JTextField
import javax.swing.border.BevelBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument

class SwTextField
private constructor(val imp : SwTextFieldImp) : ITextField, ISwComponent by SwComponent(imp)
{
    constructor() : this(SwTextFieldImp())

    override val textBind = Bindable("")
    override var text by textBind

    init {
        var locked = false
        imp.document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false}
            override fun insertUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false}
            override fun removeUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false}
        })
        textBind.addRootListener { new, old -> if(!locked)imp.text = new }
    }

    private class SwTextFieldImp() : JTextField()
    {
        init {
            background = Background.jcolor
            border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Light.jcolor, Dark.jcolor)
        }
    }
}

sealed class SwNumberField
private constructor(
        val allowsNegatives: Boolean,
        val allowsFloats: Boolean,
        val imp : SwNumberFieldImp)
    : ISwComponent by SwComponent(imp), INumberFieldUI
{
    override var validBg: Color = Background.jcolor
        set(value) {
            field = value
            checkIfOob()
        }
    override var invalidBg: Color = InvalidBg.jcolor
        set(value) {
            field = value
            checkIfOob()
        }

    constructor( allowsNegatives: Boolean = true, allowsFloats: Boolean = false) :this(allowsNegatives, allowsFloats, SwNumberFieldImp())

    val textBind = Bindable("")
    var text by textBind

    abstract fun isOob(str: String) : Boolean
    private fun checkIfOob() {
        imp.background = when( isOob(text)) {
            true -> invalidBg
            false -> validBg
        }
    }

    init {
        imp.document = SwNFDocument()

        var locked = false
        imp.document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false;checkIfOob()}
            override fun insertUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false;checkIfOob()}
            override fun removeUpdate(e: DocumentEvent?) {locked = true; text = imp.text; locked = false;checkIfOob()}
        })
        textBind.addListener {new, old ->
            if(!locked)
                imp.text = new
        }
    }

    private class SwNumberFieldImp() : JTextField()
    {
        init {
            background = Background.jcolor
            border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Light.jcolor, Dark.jcolor)
        }
    }

    inner class SwNFDocument : PlainDocument() {
        override fun insertString(offs: Int, str: String, a: AttributeSet?) {
            if( !str.matches("""^-?[0-9]*\.?[0-9]*$""".toRegex())
                    || (str.startsWith('-') && (offs != 0 || !allowsNegatives))
                    || (str.contains('.') && (getText(0, length).contains('.') || !allowsFloats)))
                Hybrid.beep()
            else
                super.insertString(offs, str, a)
        }
    }
}

class SwIntField(min: Int, max: Int, allowsNegatives: Boolean = true) : SwNumberField(allowsNegatives, false),
        IIntField, IIntFieldNonUI by IntFieldNonUI(min, max)
{
    override fun isOob(str: String): Boolean {
        val num = str.toIntOrNull(10) ?: 0
        return num < min || num > max
    }

    init {
        textBind.addRootListener { new, old ->value = new.toIntOrNull(10) ?: 0 }
        valueBind.addRootListener {new, old -> text = new.toString() }
    }
}

class SwFloatField(min: Float, max: Float, allowsNegatives: Boolean = true) : SwNumberField(allowsNegatives, true),
        IFloatField, IFloatFieldNonUI by FloatFieldNonUI(min, max)
{
    override fun isOob(str: String): Boolean {
        val num = str.toFloatOrNull() ?: 0f
        return num < min || num > max
    }

    init {
        textBind.addRootListener { new, old -> value = new.toFloatOrNull() ?: 0f }
        valueBind.addRootListener { new, old -> text = new.toString() }
    }
}