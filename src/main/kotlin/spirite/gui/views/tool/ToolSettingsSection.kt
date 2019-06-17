package spirite.gui.views.tool

import rb.IContract
import rb.glow.color.Colors
import rb.owl.bindable.Bindable
import rb.owl.bindable.addObserver
import rb.owl.interprettedBindings.bindToX
import rb.owl.interprettedBindings.bindToY
import rb.vectrix.functions.InvertibleFunction
import rbJvm.owl.addWeakObserver
import rbJvm.owl.bindWeaklyTo
import sgui.generic.components.IComboBox
import sgui.generic.components.IComponent
import sgui.generic.components.IComponent.BasicBorder.BEVELED_LOWERED
import sgui.generic.components.crossContainer.ICrossPanel
import sgui.swing.SwIcon
import sgui.swing.skin.Skin
import spirite.base.brains.IMasterControl
import spirite.base.brains.toolset.*
import spirite.gui.components.advanced.RadioButtonCluster
import spirite.gui.components.advanced.omniContainer.IOmniComponent
import spirite.hybrid.Hybrid


fun <T> DropDownProperty<T>.getComponent() = Hybrid.ui.CrossPanel {
    rows.add(Hybrid.ui.Label("$hrName: "))
    rows.add( DDPAdapter(values, valueBind), height = 24)
}
private class DDPAdapter<T>(values: Array<T>, bind: Bindable<T>, imp : IComboBox<T> = Hybrid.ui.ComboBox(values))
    : IComponent by imp
{
    init {
        imp.selectedItem = bind.field

        imp.ref = this
        imp.selectedItemBind.addObserver { new, _ -> if(new != null) bind.field = new }
    }
    val _valueK = bind.addWeakObserver { new, _ -> imp.selectedItem = new }
}




fun <T> RadioButtonProperty<T>.getComponent() : IComponent {
    val cluster = RadioButtonCluster(value, values.asList())
    val valueK = cluster.valueBind.bindWeaklyTo(valueBind)

    return CompContractUnion(Hybrid.ui.CrossPanel {
        cluster.radioButtons.forEach {
            rows.add(it)
        }
    }, valueK)
}
private class CompContractUnion(imp: IComponent, private val cont: IContract) : IComponent by imp

fun componentFromToolProperty( master: IMasterControl, toolProperty: ToolProperty<*>, contractList: MutableList<IContract>) = when( toolProperty) {
    is SliderProperty -> Hybrid.ui.CrossPanel {
        rows.add(Hybrid.ui.GradientSlider(toolProperty.min, toolProperty.max, toolProperty.hrName).apply {
            contractList.add(valueBind.bindTo(toolProperty.valueBind))
        }, height = 24)
    }
    is SizeProperty ->  Hybrid.ui.CrossPanel {
        rows.add(Hybrid.ui.GradientSlider(0f, 1000f, toolProperty.hrName).apply {
            contractList.add(valueBind.bindTo(toolProperty.valueBind))
            mutatorPositionToValue = object : InvertibleFunction<Float> {
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
                contractList.add(checkBind.bindTo(toolProperty.valueBind))
            })
        }
    }
    is DropDownProperty -> toolProperty.getComponent()
    is ButtonProperty -> Hybrid.ui.Button(toolProperty.hrName).apply {
        action = {
            Hybrid.gle.runInGLContext {
                master.commandExecutor.executeCommand(toolProperty.command.commandString, toolProperty.value)
            }
            toolProperty.value = !toolProperty.value
        }
    }
    is RadioButtonProperty -> toolProperty.getComponent()
    is FloatBoxProperty -> Hybrid.ui.CrossPanel{
        rows += {
            add(Hybrid.ui.Label(toolProperty.hrName + ": "))
            add(Hybrid.ui.FloatField().apply {
                contractList.add(valueBind.bindTo(toolProperty.valueBind))
            }, height = 24)
        }
    }
    is DualFloatBoxProperty -> Hybrid.ui.CrossPanel{
        rows.add(Hybrid.ui.Label(toolProperty.hrName + ": "))
        rows += {
            add(Hybrid.ui.Label(toolProperty.label1))
            add(Hybrid.ui.FloatField().also {
                contractList.add(it.valueBind.bindToX(toolProperty.valueBind))
            }, height = 24, flex = 50f)
            add(Hybrid.ui.Label(toolProperty.label2))
            add(Hybrid.ui.FloatField().also {
                contractList.add(it.valueBind.bindToY(toolProperty.valueBind))
            },  height = 24, flex = 50f)
        }
    }

}

class ToolSettingsSection
private constructor(val master : IMasterControl, val imp : ICrossPanel)
    : IOmniComponent
{
    init { imp.ref = this}
    override val component: IComponent get() = imp
    override val icon: SwIcon? get() = null
    override val name: String get() = "Tool Settings"

    constructor(master: IMasterControl) : this(master, Hybrid.ui.CrossPanel())

    val toolLabel = Hybrid.ui.Label().apply { textColor = Colors.BLACK }
    val toolPanel = Hybrid.ui.CrossPanel().apply { setBasicBorder(BEVELED_LOWERED) }

    private val contractList = mutableListOf<IContract>()

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

    private fun constructFromTool(tool: Tool) {
        contractList.forEach { it.void() }
        contractList.clear()

        toolPanel.setLayout {
            tool.properties.forEach {
                rows.add(componentFromToolProperty(master, it, contractList))
            }
        }
    }

    private val _selectedToolK = master.toolsetManager.selectedToolBinding.addWeakObserver { new, old ->
        toolLabel.text = new.description
        constructFromTool(new)
    }

    override fun close() {
        contractList.forEach { it.void() }
        _selectedToolK.void()
    }
}