package spirite.gui.components.major.tool

import spirite.base.brains.Bindable
import spirite.base.brains.IMasterControl
import spirite.base.brains.toolset.*
import spirite.base.util.Colors
import spirite.base.util.InvertibleFunction
import spirite.base.util.linear.Vec2
import spirite.gui.components.advanced.RadioButtonCluster
import spirite.gui.components.basic.IComponent
import spirite.gui.components.basic.IComponent.BasicBorder.BEVELED_LOWERED
import spirite.gui.components.basic.ICrossPanel
import spirite.gui.resources.Skin
import spirite.hybrid.Hybrid
import spirite.pc.gui.basic.SwComponent
import spirite.pc.gui.jcolor


fun <T> DropDownProperty<T>.getComponent() = Hybrid.ui.CrossPanel {
    rows.add(Hybrid.ui.Label("$hrName: "))
    rows.add(Hybrid.ui.ComboBox(values).apply {
        selectedItemBind.bindWeakly(valueBind)
    }, height = 24)
}



fun <T> RadioButtonProperty<T>.getComponent() :IComponent {
    val cluster = RadioButtonCluster(value, values.asList())
    cluster.valueBind.bindWeakly(valueBind)

    return Hybrid.ui.CrossPanel {
        cluster.radioButtons.forEach {
            rows.add(it)
        }
    }
}

class DualFloatBindind( default: Vec2) {
    val vecBind = Bindable(default, {new,old-> x = new.x; y = new.y})
    var vec by vecBind

    val xBind = Bindable(default.x, {new,old-> })
    var x by xBind

    val yBind = Bindable(default.x)
    var y by yBind
}

fun componentFromToolProperty( master: IMasterControl, toolProperty: ToolProperty<*>) = when( toolProperty) {
    is SliderProperty -> Hybrid.ui.CrossPanel {
        rows.add(Hybrid.ui.GradientSlider(toolProperty.min, toolProperty.max, toolProperty.hrName).apply {
            valueBind.bindWeakly(toolProperty.valueBind)
        }, height = 24)
    }
    is SizeProperty ->  Hybrid.ui.CrossPanel {
        rows.add(Hybrid.ui.GradientSlider(0f, 1000f, toolProperty.hrName).apply {
            valueBind.bindWeakly(toolProperty.valueBind)
            mutatorPositionToValue = object : InvertibleFunction<Float>{
                override fun perform(x: Float): Float = when {
                    x < 0.25f   -> x * 10f * 4f
                    x < 0.5f    -> (x - 0.25f) * 90f * 4f + 10f
                    x < 0.75    -> (x - 0.5f) * 400f * 4f + 100
                    else        -> (x - 0.75f) * 500f * 4f + 500f
                }

                override fun invert(x: Float): Float = when {
                    x < 10f     -> 0.25f * x / 10f
                    x < 100f    -> 0.25f + 0.25f * (x - 10f) / 90f
                    x < 500f    -> 0.5f + (x - 100f) / 400f
                    else        -> 0.75f + 0.25f * (x - 500f) / 500f
                }
            }
        }, height = 24)
    }
    is CheckBoxProperty -> Hybrid.ui.CrossPanel {
        rows += {
            add(Hybrid.ui.Label(toolProperty.hrName + ": "))
            add(Hybrid.ui.CheckBox().apply {
                checkBind.bindWeakly(toolProperty.valueBind)
            })
        }
    }
    is DropDownProperty -> toolProperty.getComponent()
    is ButtonProperty -> Hybrid.ui.Button(toolProperty.hrName).apply {
        action = {master.commandExecuter.executeCommand(toolProperty.command.commandString, toolProperty.value)}
    }
    is RadioButtonProperty -> toolProperty.getComponent()
    is FloatBoxProperty -> Hybrid.ui.CrossPanel{
        rows += {
            add(Hybrid.ui.Label(toolProperty.hrName + ": "))
            add(Hybrid.ui.FloatField().apply {
                valueBind.bindWeakly(toolProperty.valueBind)
            }, height = 24)
        }
    }
    is DualFloatBoxProperty -> Hybrid.ui.CrossPanel{
        val bind = DualFloatBindind(toolProperty.value)
        bind.vecBind.bindWeakly(toolProperty.valueBind)

        rows.add(Hybrid.ui.Label(toolProperty.hrName + ": "))
        rows += {
            add(Hybrid.ui.Label(toolProperty.label1))
            add(Hybrid.ui.FloatField().apply { valueBind.bind(bind.xBind) }, height = 24, flex = 50f)
            add(Hybrid.ui.Label(toolProperty.label2))
            add(Hybrid.ui.FloatField().apply { valueBind.bind(bind.yBind) }, height = 24, flex = 50f)
        }
    }

}

class ToolSettingsSection
private constructor(val master : IMasterControl, val imp : ICrossPanel)
    :IComponent by imp
{
    constructor(master: IMasterControl) : this(master, Hybrid.ui.CrossPanel())

    val toolLabel = Hybrid.ui.Label().apply { textColor = Colors.BLACK.jcolor }
    val toolPanel = Hybrid.ui.CrossPanel().apply { setBasicBorder(BEVELED_LOWERED) }

    init {
        imp.background = Skin.Global.Fg.scolor
        imp.setLayout {
            rows.addGap(2)
            rows += {
                addGap(2)
                add(toolLabel)
            }
            rows.addGap(2)
            rows += {
                addGap(4)
                add(toolPanel, flex = 100f)
                addGap(4)
                flex = 100f
            }
            rows.addGap(2)
            rows.flex = 100f
        }
    }

    fun constructFromTool( tool: Tool) {
        toolPanel.setLayout {
            tool.properties.forEach {
                rows.add(componentFromToolProperty(master, it))
            }
        }
    }

    private val ashdksja = master.toolsetManager.selectedToolBinding.addWeakListener { new, old ->
        toolLabel.label = new.description
        constructFromTool(new)
    }
}