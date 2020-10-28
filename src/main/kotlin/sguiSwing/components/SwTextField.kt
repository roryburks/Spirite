package sguiSwing.components

import rbJvm.glow.SColor
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import sgui.components.*
import sgui.systems.KeypressSystem
import sguiSwing.jcolor
import sguiSwing.mouseSystem.adaptMouseSystem
import sguiSwing.skin.Skin.BevelBorder.Dark
import sguiSwing.skin.Skin.BevelBorder.Light
import sguiSwing.skin.Skin.TextField.Background
import sguiSwing.skin.Skin.TextField.InvalidBg
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.BorderFactory
import javax.swing.JTextField
import javax.swing.border.BevelBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument

class SwTextField
private constructor(private val imp : SwTextFieldImp) : ITextField, ISwComponent by SwComponent(imp)
{
    constructor() : this(SwTextFieldImp())

    override val textBind = Bindable("")
    override var text by textBind

    init {
        var swInducedLock = false
        var bindInducedLock = false
        imp.document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                swInducedLock = true
                if(!bindInducedLock)text = imp.text
                swInducedLock = false
            }
            override fun insertUpdate(e: DocumentEvent?) {
                swInducedLock = true
                if(!bindInducedLock)text = imp.text
                swInducedLock = false
            }
            override fun removeUpdate(e: DocumentEvent?) {
                swInducedLock = true
                if(!bindInducedLock || imp.text != "")text = imp.text
                swInducedLock = false
            }
        })
        textBind.addObserver { new, _ ->
            bindInducedLock = true
            if(!swInducedLock) imp.text = new
            bindInducedLock = false
        }


        imp.addFocusListener(object : FocusListener {
            override fun focusLost(e: FocusEvent) {
                KeypressSystem.hotkeysEnabled = true
            }
            override fun focusGained(e: FocusEvent?) {
                KeypressSystem.hotkeysEnabled = false
            }
        })
    }

    private class SwTextFieldImp() : JTextField()
    {
        init {
            adaptMouseSystem()
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
    override var validBg: SColor = Background.scolor
        set(value) {
            field = value
            checkIfOob()
        }
    override var invalidBg: SColor = InvalidBg.scolor
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
            true -> invalidBg.jcolor
            false -> validBg.jcolor
        }
    }

    init {
        imp.document = SwNFDocument()

        var swInducedLock = false
        var bindInducedLock = false
        imp.document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) {
                swInducedLock = true
                if( !bindInducedLock)text = imp.text
                swInducedLock = false
                checkIfOob()
            }
            override fun insertUpdate(e: DocumentEvent?) {
                swInducedLock = true
                if( !bindInducedLock)text = imp.text
                swInducedLock = false
                checkIfOob()
            }
            override fun removeUpdate(e: DocumentEvent?) {
                swInducedLock = true
                if( !bindInducedLock && imp.text != "")text = imp.text
                swInducedLock = false
                checkIfOob()}
        })
        textBind.addObserver { new, _->
            bindInducedLock = true
            if(!swInducedLock)
                imp.text = new
            bindInducedLock = false
        }
    }

    private class SwNumberFieldImp() : JTextField()
    {
        init {
            adaptMouseSystem()
            background = Background.jcolor
            border = BorderFactory.createBevelBorder(BevelBorder.LOWERED, Light.jcolor, Dark.jcolor)
        }
    }

    inner class SwNFDocument : PlainDocument() {
        override fun insertString(offs: Int, str: String, a: AttributeSet?) {
            if( !str.matches("""^-?[0-9]*\.?[0-9]*$""".toRegex())
                    || (str.startsWith('-') && (offs != 0 || !allowsNegatives))
                    || (str.contains('.') && (getText(0, length).contains('.') || !allowsFloats)))
            {
                java.awt.Toolkit.getDefaultToolkit().beep()
            }
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
        textBind.addObserver { new, _ -> value = new.toIntOrNull(10) ?: 0 }
        valueBind.addObserver { new, _ -> text = new.toString() }
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
        textBind.addObserver { new, _ -> value = new.toFloatOrNull() ?: 0f }
        valueBind.addObserver { new, _ -> text = new.toString() }
    }
}