package sguiSwing.skin

import rb.glow.color.SColor
import sguiSwing.scolor
import java.awt.Color

object Skin {
    private val baseDDD = Color(56, 72, 100)
    private val baseDD = Color(56, 90, 125)
    private val baseD = Color(56, 90, 125)
    private val base = Color(56, 118, 155)
    private val fgD = Color(56, 150, 203)
    private val fg = Color(56, 168, 223)
    private val fgL = Color(56, 190, 240)

    // This seems like a weird way to do it, but it could easily be replaced to read jcolor
    //  values from settings instead of hard-coded.
    interface ColorMarker {
        val jcolor : Color
        val scolor : SColor get() = jcolor.scolor
    }
    enum class DrawPanel(override val jcolor: Color) : ColorMarker {
        ImageBorder(Color(190, 190, 190)),
        BidBorder(Color(16,16,16)),
        LayerBorder(Color(190,190,120))
    }
    enum class Toolbutton(override val jcolor: Color) : ColorMarker {
        SelectedBackground(Color(128, 128, 128))
    }
    enum class ContentTree(override val jcolor: Color) : ColorMarker {
        SelectedBgDragging( Color( 160,160,196)),
        SelectedBackground(Color( 128,128,156)),
        Background(Color( 96,96,96))
    }
    enum class UndoPanel(override val jcolor: Color) : ColorMarker {
        SelectedBackground(Color( 160,160,196)),
        Background(Color( 238,238,238))
    }
    enum class AnimSchemePanel(override val jcolor: Color) : ColorMarker {
        ActiveNodeBg(fgD),
        TickBg(fg)
    }
    enum class WorkArea(override val jcolor: Color) : ColorMarker {
        NormalBg( Color(238,238,238)),
        ReferenceBg(Color( 210,210,242) )
    }
    enum class FFAAnimation(override val jcolor: Color) : ColorMarker {
        Arrow( Color(238,30,30)),
    }
    enum class ResizePanel(override val jcolor: Color) : ColorMarker {
        BarLineColor(baseDDD)
    }
    enum class TextField(override val jcolor: Color) : ColorMarker {
        Background(fg),
        InvalidBg(Color.RED)
    }
    enum class TabbedPane(override val jcolor: Color) : ColorMarker {
        SelectedBg(fg),
        UnselectedBg(base),
        TabBorder(baseD),
        TabText( Color(255,255,255))
    }
    enum class BevelBorder(override val jcolor: Color) : ColorMarker {
        Light(fgD),
        Med(baseD),
        Dark(baseDD),
        Darker(baseDDD)
    }
    enum class Global(override val jcolor: Color) : ColorMarker {
        BgDark(baseDD),
        Bg(base),
        Fg(fg),
        FgLight(fgL),
        Text( Color(190,205,220)),
        TextDark( Color( 57, 62, 66))
    }
    enum class GradientSlider( override val jcolor: Color) : ColorMarker {
        BgGradLeft(Color(64, 64, 64)),
        BgGradRight(Color(128,128,128)),
        FgGradLeft(Color(120,120,190)),
        FgGradRight(Color(90,90,160)),
        DisabledGradLeft(Color(120,120,120)),
        DisabledGradRight(Color(160,160,160))
    }
}