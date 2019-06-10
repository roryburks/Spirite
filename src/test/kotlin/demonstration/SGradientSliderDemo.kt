package demonstration

import spirite.base.util.InvertibleFunction
import sgui.generic.Orientation.HORIZONTAL
import spirite.gui.components.advanced.ResizeContainerPanel
import sgui.generic.components.IButton
import spirite.hybrid.Hybrid
import sgui.swing.components.jcomponent
import java.awt.Color
import java.awt.GridLayout
import javax.swing.JFrame


fun main( args: Array<String>) {
    DemoLauncher.launch(SGradientSliderDemo())
}

class SGradientSliderDemo : JFrame() {
    init {
        layout = GridLayout()

        val sliderThing = Hybrid.ui.GradientSlider(label = "Thing: ")

        val buttonReset : IButton = Hybrid.ui.Button("SetThing")
        buttonReset.action = { sliderThing.value = 0.5f }


        val sliderBound1 = Hybrid.ui.GradientSlider(label = "Bound 1 (xi^2):")
        val sliderBound2 = Hybrid.ui.GradientSlider(label = "Bound 2:")
        sliderBound1.mutatorPositionToValue = object: InvertibleFunction<Float> {
            override fun perform(x: Float): Float = Math.pow(x.toDouble(), 2.0).toFloat()
            override fun invert(x: Float): Float= Math.pow(x.toDouble(), 1.0/2.0).toFloat()
        }
        sliderBound2.valueBind.bindTo(sliderBound1.valueBind)
        sliderBound1.fgGradLeft = Color(30, 160, 30)
        sliderBound1.fgGradRight = Color.BLACK

        val disabledSlider = Hybrid.ui.GradientSlider(label = "Disabled")
        disabledSlider.value = 0.4f
        //disabledSlider.imp.isEnabled = false

        val resize = ResizeContainerPanel(sliderThing, HORIZONTAL)
        resize.minStretch = 100
        resize.addPanel(buttonReset, 100,100,-999)
        resize.addPanel(disabledSlider, 100,100,-999)
        resize.addPanel(sliderBound1, 100,100,999)
        resize.addPanel(sliderBound2, 100,100,999)

        add( resize.jcomponent)
    }
}