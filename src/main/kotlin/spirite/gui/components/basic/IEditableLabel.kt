package spirite.gui.components.basic

import spirite.base.util.binding.CruddyBindable
import spirite.gui.resources.Skin
import spirite.pc.gui.SColor
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.jcolor
import java.awt.KeyboardFocusManager
import java.awt.event.*
import javax.swing.*

interface IEditableLabel : ITextField
{
    fun startEditing()
}

class SwEditableLabel
private constructor(
        defaultText: String,
        private val imp : SwELabelImp)
    : IEditableLabel, IComponent by SwComponent(imp)
{

    override var foreground: SColor =Skin.Global.Text.scolor
        set(value) {
            field = value
            textArea?.foreground = value.jcolor
            label?.foreground = value.jcolor
        }

    constructor(defaultText: String) : this(defaultText, SwELabelImp())

    override val textBind: CruddyBindable<String> = CruddyBindable(defaultText) { new, _ ->
        label?.text = new
        textArea?.text = new
    }
    override var text by textBind

    private var label : SwELabel? = null
    private var textArea : SwETextArea? = null

    override fun startEditing() {
        val h = label?.height ?: 20
        label = null
        textArea = SwETextArea(text)
        imp.removeAll()
        SwingUtilities.invokeLater{
            textArea?.select(0, text.length)
            textArea?.requestFocus()
        }

        imp.layout = GroupLayout(imp).also {
            it.setHorizontalGroup(it.createSequentialGroup().addComponent(textArea))
            it.setVerticalGroup(it.createSequentialGroup().addComponent(textArea, h+2, h+2, h+2))
            it.setVerticalGroup(it.createSequentialGroup().addComponent(textArea, h+2, h+2, h+2))
        }
    }

    fun doneEditing() {
        text = textArea?.text ?: text

        textArea = null
        label = SwELabel(text)
        imp.removeAll()
        //imp.requestFocus()


        imp.layout = GroupLayout(imp).also {
            it.setHorizontalGroup(it.createSequentialGroup().addComponent(label))
            it.setVerticalGroup(it.createSequentialGroup().addComponent(label))
        }
    }


    init {
        val _label = SwELabel(defaultText)
        label = _label

        imp.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename")
        imp.actionMap.put("rename", object : AbstractAction(){
            override fun actionPerformed(e: ActionEvent?) {
                startEditing()
            }
        })

        imp.addMouseListener( object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                requestFocus()
            }
        })

        imp.isOpaque = false

        imp.layout = GroupLayout(imp).also {
            it.setHorizontalGroup(it.createSequentialGroup().addComponent(label))
            it.setVerticalGroup(it.createSequentialGroup().addComponent(label))
        }

    }

    private inner class SwELabel(text: String) : JLabel(text) {
        init {
            foreground = Skin.Global.Text.jcolor

            addMouseListener(object: MouseListener {
                override fun mouseReleased(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mouseEntered(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mouseClicked(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mouseExited(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mousePressed(e: MouseEvent?) {parent.dispatchEvent(e)}
            })


            addMouseMotionListener( object : MouseMotionListener {
                override fun mouseMoved(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mouseDragged(e: MouseEvent?) = parent.dispatchEvent(e)
            })
            addMouseListener( object : MouseListener {
                override fun mouseReleased(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mouseEntered(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mouseClicked(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mouseExited(e: MouseEvent?) = parent.dispatchEvent(e)
                override fun mousePressed(e: MouseEvent?) = parent.dispatchEvent(e)
            })
        }
    }
    private inner class SwETextArea(text: String) : JTextArea(text) {
        init {
            foreground = Skin.Global.Text.jcolor
            border = null
            background = null
            isOpaque = false
            requestFocus()

            KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener {
                if( it.oldValue == this && it.newValue != this) doneEditing()
            }

            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter")
            actionMap.put("enter", object : AbstractAction(){
                override fun actionPerformed(e: ActionEvent?) {
                    doneEditing()
                }
            })
        }
    }

    private class SwELabelImp : JPanel() {
        init {
            background = Skin.Global.Bg.jcolor
        }
    }
}